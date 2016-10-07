package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.data.web.ServiceDetailDb;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.msg.*;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.NotificationUtils;
import com.mservice.momo.util.StatisticUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.*;

/**
 * Created by User on 3/18/14.
 */
public class Common {

    private TransDb transDb;
    private PhonesDb phonesDb;
    private Logger logger;
    private AgentsDb agentsDb;
    private Vertx vertx;

    public Common(Vertx vertx, Logger logger, JsonObject glbCfg) {
        this.vertx = vertx;
        this.logger = logger;
        agentsDb = new AgentsDb(vertx.eventBus(), logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);

    }

    public void SendWholeSystemPaused(Vertx _vertx
            , final NetSocket sock
            , final MomoMessage msg
            , final Handler<Boolean> callback) {

        ServiceReq serviceReq = new ServiceReq();
        serviceReq.Command = ServiceReq.COMMAND.GET_SERVER_ONOFF;

        _vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonObject jRpl = message.body();
                boolean isPaused = jRpl.getBoolean("ispaused", false);
                String caption = jRpl.getString("caption", "Thông báo");
                String body = jRpl.getString("body", "Hệ thống đang tạm dừng để nâng cấp dịch vụ, quý khách vui lòng thử lại sau");

                if (isPaused) {
                    //he thong dang tam dung
                    Buffer paused = MomoMessage.buildBuffer(
                            MomoProto.MsgType.WHOLE_SYSTEM_PAUSED_VALUE,
                            msg.cmdIndex,
                            0,
                            MomoProto.WholeSystemPaused.newBuilder()
                                    .setCaption(caption)
                                    .setBody(body)
                                    .build().toByteArray()
                    );
                    writeDataToSocket(sock, paused);

                    callback.handle(true);
                    return;
                }
                callback.handle(false);
            }
        });
    }

    public void writeEchoReply(final NetSocket sock, MomoMessage msg) {
        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.ECHO_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                null
        );
        writeDataToSocket(sock, buf);
    }

    public void writeDataToSocket(final NetSocket sock, Buffer buf) {

        MomoProto.MsgType type = MomoProto.MsgType.valueOf(MomoMessage.getType(buf));
        final MomoMessage msg = MomoMessage.fromBuffer(buf);


        if(msg != null && ServerVerticle.hashDataUsers.containsKey("" + MomoMessage.getIndex(buf)))
        {
            String index = "" + MomoMessage.getIndex(buf);
            long inputTime = ServerVerticle.hashDataUsers.get(index) != null ? ServerVerticle.hashDataUsers.get(index) : 0;
            long timeLost = System.currentTimeMillis() - inputTime;
            logger.info("TIME RESPONSE TO NUMBER " + msg.cmdPhone + " INDEX " + index + " MSECOND LOST " + timeLost + "ms" + " MSG_TYPE " + type + "_" + msg.cmdType);
            ServerVerticle.hashDataUsers.remove(index);
            logger.info("SIZE HASH DATA USERS REMOVE " + ServerVerticle.hashDataUsers.size());
        }
        else if(msg == null) {
            logger.info("MSG IS NULL " + sock.writeHandlerID());
        }
        else {
            logger.info("HASH DATA USERS NOT INDEX");
        }

        if (sock == null) {
            logger.info("socket is null - can not write data");
            return;
        }

        if (sock.writeQueueFull()) {
            logger.info("CPU AND RAM ARE OVERLOADING, PLEASE RESTART SERVER. " + (msg != null ? msg.cmdPhone + " MSG TYPE " + type + "_" + msg.cmdType : "MSG IS NULL"));
        }
        sock.write(buf);
        //for debug only
//        MomoProto.MsgType type = MomoProto.MsgType.valueOf(MomoMessage.getType(buf));
        if (type != null) {
            logger.info(sock.writeHandlerID() + " >> snd msg type " + type.name() + " " + buf.length() + " bytes of data");
        } else {
            logger.info(sock.writeHandlerID() + " >> snd unknown msg type " + buf.length() + " bytes of data");
        }


//        else {
//            logger.info(sock.writeHandlerID() + "can not send message sock.writeQueueFull()");
//            sock.pause();
//            sock.drainHandler(new VoidHandler() {
//                public void handle() {
//                    sock.resume();
//                }
//            });
//        }
    }

    public void writeDataToSocketAndClose(final NetSocket sock, Buffer buf) {
        writeDataToSocket(sock, buf);
        if (sock != null) {
            sock.close();
        }
        /*vertx.setTimer(1000, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                if(sock != null){
                    sock.close();
                }
            }
        });*/
    }

    public void writeErrorToSocket(final NetSocket sock, int errCode, String errDesc) {

        MomoProto.StandardReply err = MomoProto.StandardReply.newBuilder()
                .setRcode(errCode)
                .setDesc(errDesc)
                .build();
        Buffer errBuf = MomoMessage.buildBuffer(MomoProto.MsgType.ERROR_VALUE, 0, 0, err.toByteArray());
        writeDataToSocket(sock, errBuf);

    }

    public void writeErrorToSocket(final NetSocket sock) {
        writeErrorToSocket(sock
                , MomoProto.SystemError.MSG_FORMAT_NOT_CORRECT_VALUE
                , MomoProto.SystemError.MSG_FORMAT_NOT_CORRECT.name());
    }

    //tra ket qua ve
    public synchronized void sendTransReply(final Vertx vertx, final JsonObject tranRplFromSoap
            , final long ackTime
            , final MomoMessage msg
            , final NetSocket sock
            , final SockData data
            , final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            logger.debug("Parse TranRequest message in sendTransReply got error " + ex.getMessage());
            request = null;
        }

        //sync du lieu voi core va tra ve cho client cap nhat
        if (tranRplFromSoap != null && request != null) {
            final MomoProto.TranHisV1 finalRequest = request;

            tranRplFromSoap.putNumber(colName.TranDBCols.COMMAND_INDEX, msg.cmdIndex);
            tranRplFromSoap.putNumber(colName.TranDBCols.CLIENT_TIME, request.getClientTime());
            tranRplFromSoap.putNumber(colName.TranDBCols.ACK_TIME, ackTime);

            int tranType = request.getTranType();
            if (tranType == MomoProto.TranHisV1.TranType.M2C_VALUE) {
                tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
            }

            String ticketcode = tranRplFromSoap.getString("ticketcode", "");

            tranRplFromSoap.putNumber(colName.TranDBCols.TRAN_TYPE, tranType);
            tranRplFromSoap.putNumber(colName.TranDBCols.CATEGORY, request.getCategory());
            tranRplFromSoap.putBoolean(colName.TranDBCols.DELETED, false);
            tranRplFromSoap.putNumber(colName.TranDBCols.FROM_SOURCE, request.getSourceFrom());
            tranRplFromSoap.putString(colName.TranDBCols.PARTNER_ID, request.getPartnerId());
            tranRplFromSoap.putString(colName.TranDBCols.PARTNER_CODE, request.getPartnerCode());
            tranRplFromSoap.putString(colName.TranDBCols.PARTNER_NAME, request.getPartnerName());
            tranRplFromSoap.putString(colName.TranDBCols.PARTNER_REF, request.getPartnerRef());

            if ("".equalsIgnoreCase(ticketcode)) {
                tranRplFromSoap.putString(colName.TranDBCols.COMMENT, request.getComment());
            } else {
                String orgText = request.getComment();
                orgText = orgText.replaceAll(". Mã đặt vé: .", ". Mã đặt vé: " + ticketcode + " .");
                tranRplFromSoap.putString(colName.TranDBCols.COMMENT, orgText);
            }

            //force comment by new comment
            String newComment = tranRplFromSoap.getString(Const.AppClient.NewComment, "");
            if (!"".equalsIgnoreCase(newComment)) {
                tranRplFromSoap.putString(colName.TranDBCols.COMMENT, newComment);
            }

            //refine billid dien.start
            String billId = request.getBillId() == null ? "" : request.getBillId();
            String serviceId = request.getPartnerId() == null ? "" : request.getPartnerId();
            billId = Misc.refineBillId(serviceId, billId, "pe");
            tranRplFromSoap.putString(colName.TranDBCols.BILL_ID, billId);
            //refine billid dien.end

            //new
            tranRplFromSoap.putNumber(colName.TranDBCols.PARRENT_TRAN_TYPE, MomoProto.MsgType.TRANSFER_REQUEST_VALUE);
            tranRplFromSoap.putNumber(colName.TranDBCols.OWNER_NUMBER, msg.cmdPhone);

            final boolean balChanged = (tranType != MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE
                    && tranType != MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE);

            final boolean tranSuccessed = tranRplFromSoap.getInteger(colName.TranDBCols.ERROR, -1) == 0;

            //send result to sender
            tranRplFromSoap.putString(colName.TranDBCols.OWNER_NAME, (data != null && data.getPhoneObj() != null) ? data.getPhoneObj().name : "");


            final BuildLog log = new BuildLog(logger);
            log.setPhoneNumber("0" + msg);

            //build shared data
            String share = (request.getShare() == null || request.getShare().isEmpty() ? "" : request.getShare());
            String html = tranRplFromSoap.getString(Const.AppClient.Html, "");

            String qrcode = tranRplFromSoap.getString(Const.AppClient.Qrcode, "");

            boolean isJsonArrayShare = Misc.isValidJsonArray(share);
            JsonArray arrayShare = "".equalsIgnoreCase(share) || !isJsonArrayShare ? new JsonArray() : new JsonArray(share);

            if (!"".equalsIgnoreCase(html)) {
                arrayShare.add(new JsonObject().putString(Const.AppClient.Html, html));
            }

            if (!"".equalsIgnoreCase(qrcode)) {
                arrayShare.add(new JsonObject().putString(Const.AppClient.Qrcode, qrcode));
            }

            if (tranRplFromSoap.containsField(Const.AppClient.Fee)) {
                int fee = tranRplFromSoap.getInteger(Const.AppClient.Fee, 0);
                arrayShare.add(new JsonObject().putValue(Const.AppClient.Fee, fee));
                tranRplFromSoap.removeField(Const.AppClient.Fee);
            }

            if (tranRplFromSoap.containsField(Const.AppClient.OldAmount)) {
                long oldAmount = tranRplFromSoap.getInteger(Const.AppClient.OldAmount, 0);
                arrayShare.add(new JsonObject().putValue(Const.AppClient.OldAmount, oldAmount));
            }
            if (tranRplFromSoap.containsField("point")) {
                long point = tranRplFromSoap.getInteger("point", 0);
                log.add("Check point ------------------->", "point");
                log.add("point", point);
                arrayShare.add(new JsonObject().putValue("point", point));
            }

            //dua them key cusnum vao tat ca cac giao dich cua retailer
            String customerPhone = "";
            if (tranRplFromSoap.containsField(Const.DGD.CusNumber)) {
                String customerNumber = tranRplFromSoap.getString(Const.DGD.CusNumber, "");
                customerPhone = customerNumber;
                if (!"".equalsIgnoreCase(customerNumber) && !arrayShare.contains(Const.DGD.CusNumber)) {
                    arrayShare.add(new JsonObject().putValue(Const.DGD.CusNumber, customerNumber));
                }
                tranRplFromSoap.removeField(Const.DGD.CusNumber);
            }

            tranRplFromSoap.putArray(colName.TranDBCols.SHARE, arrayShare);
            if(!tranRplFromSoap.containsField(colName.TranDBCols.FINISH_TIME))
            {
                tranRplFromSoap.putNumber(colName.TranDBCols.FINISH_TIME, System.currentTimeMillis());
            }
            int status = tranRplFromSoap.getInteger(colName.TranDBCols.STATUS) == null ? tranRplFromSoap.getInteger(colName.TranDBCols.ERROR, -1) == 0 ? 4 : 5 : tranRplFromSoap.getInteger(colName.TranDBCols.STATUS);
            tranRplFromSoap.putNumber(colName.TranDBCols.STATUS, status);
            Buffer buffer = Misc.buildTranHisReply(msg, finalRequest, tranRplFromSoap, log);
            log.add("function", "writeDataToSocket");
            if(sock != null)
            {
                writeDataToSocket(sock, buffer);
            }

            final TranObj tranObj = new TranObj(tranRplFromSoap);

            //save tranhis of sender
            //Ham saveTranDb co thay doi noi dung cua tranRplFromSoap nen tao 1 instance khac.

            //JsonObject jCloneTran =  tranRplFromSoap.copy();
            JsonObject jCloneTran = tranObj.getJSON();

            if (jCloneTran.getInteger(colName.TranDBCols.TRAN_TYPE, 0) == TranTypeExt.Escape
                    || jCloneTran.getInteger(colName.TranDBCols.TRAN_TYPE, 0) == TranTypeExt.CapDoiHoanHao
                    || jCloneTran.getInteger(colName.TranDBCols.TRAN_TYPE, 0) == TranTypeExt.Fsc2014
                    ) {
                jCloneTran.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
            }
            log.add("jCloneTran", jCloneTran.toString());
            saveTranDb(msg.cmdPhone, jCloneTran);

            //giao dich duoc thuc hien khong qua app -->cap nhat cho app neu online
            if (sock == null) {
                BroadcastHandler.sendOutSideTransSync(vertx, tranObj);
            }

            //gui thong tin khuyen mai cho giao dich thanh cong
            if (tranSuccessed && balChanged) {
                if (sock != null) {
                    log.add("function", "sendCurrentAgentInfo");
                    sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);
                }
            }

            //xu ly notification cho cac truong hop dac biet
            //1. QRCODE
            if ("qrcode".equalsIgnoreCase(request.getBillId()) && tranType == MomoProto.TranHisV1.TranType.M2M_VALUE) {

                /*
                    Thanh toán QR Code thành công
                    Chuyển tiền thành công
                    Quý khách đã chuyển thành công số tiền [xxx] tới [ tên] (số điện thoại) qua mã QR.
                        Thanh toán QR Code thất bại
                    Chuyển tiền không thành công
                    Quý khách đã chuyển không thành công số tiền [xxx] tới [ tên] (số điện thoại) qua mã QR.
                */

                long tranId = tranRplFromSoap.getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());
                String partnerName = (request.getPartnerName() == null ? "" : request.getPartnerName());
                String partnerPhone = (request.getPartnerId() == null ? "" : request.getPartnerId());
                int tmpPhone = DataUtil.strToInt(partnerPhone);
                partnerPhone = (tmpPhone > 0 ? "0" + tmpPhone : "0");

                String comment = "";
                String tpl = "";
                String caption = "";
                String sms = "";

                if (tranSuccessed) {
                    tpl = "Quý khách đã chuyển thành công số tiền %s tới %s (%s) qua mã QR.";
                    comment = String.format(tpl
                            , Misc.formatAmount(request.getAmount()).replace(",", "") + "đ"
                            , partnerName
                            , partnerPhone);
                    caption = "Chuyển tiền thành công";
                    String smsTmp = "Chuc mung quy khach da chuyen thanh cong so tien %sd den %s (%s) luc: %s. TID:%s. Xin cam on";
                    sms = String.format(smsTmp
                            , Misc.formatAmount(request.getAmount()).replace(",", "")
                            , partnerName
                            , partnerPhone
                            , Misc.getDate(System.currentTimeMillis())
                            , tranId
                    );

                } else {
                    tpl = "Quý khách đã chuyển không thành công số tiền %s tới %s (%s) qua mã QR.";
                    comment = String.format(tpl
                            , Misc.formatAmount(request.getAmount()).replace(",", "") + "đ"
                            , partnerName
                            , partnerPhone);
                    caption = "Chuyển tiền không thành công";
                }

                Notification noti = new Notification();
                noti.receiverNumber = msg.cmdPhone;
                noti.caption = caption;
                noti.body = comment;//"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";
                noti.sms = sms;     //"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.";
                noti.priority = 1;
                noti.time = System.currentTimeMillis();
                noti.tranId = tranId;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                        , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                    }
                });

            } else if (tranObj.tranType == MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE) {

                //xu ly phan loai thanh toan cho hoa don hay dich vu
                final TranObj clonedTran = Misc.cloneTran(tranObj);

                //thuc hien check thong tin xem la hoa don hay dich vu
                ServiceReq serviceReq = new ServiceReq();
                serviceReq.Command = ServiceReq.COMMAND.GET_SERVICE_TYPE;
                serviceReq.ServiceId = clonedTran.partnerId;

                Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
                    @Override
                    public void handle(JsonArray array) {
                        ServiceDb.Obj obj = new ServiceDb.Obj((JsonObject) array.get(0));

                        if ("service".equalsIgnoreCase(obj.serviceType)) {
                            clonedTran.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE_VALUE;
                        }
                        log.add("createRequestPushNotification ----------------------> ", "two");
                        //notification cho client
//                        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
//                                , NotificationUtils.createRequestPushNotification(clonedTran.owner_number, 1, clonedTran));
                        //Chan SMS
                        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                , NotificationUtils.createRequestPushNotification(clonedTran.owner_number, 2, clonedTran));

                    }
                });

                // giao dich voi doi tac Escap
            } else {
                log.add("createRequestPushNotification ----------------------> ", "one");
                try {
//                    vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
//                            , NotificationUtils.createRequestPushNotification(tranObj.owner_number, 1, tranObj));
                    //Chan SMS
                    vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                            , NotificationUtils.createRequestPushNotification(tranObj.owner_number, 2, tranObj));
                } catch (Exception ex) {
                    log.add("exception noti", ex);
                }
                //cac trung hop con lai --> notification cho client

            }

            //ghi log o day
            log.writeLog();

            //cac giao dich thanh cong va co ben nhan tien
            if (data != null && tranSuccessed
                    && (tranRplFromSoap.getBoolean(colName.TranDBCols.IS_M2NUMBER, false) == false)
                    && (tranType == MomoProto.TranHisV1.TranType.M2M_VALUE ||
                    tranType == MomoProto.TranHisV1.TranType.M2C_VALUE ||
                    tranType == MomoProto.TranHisV1.TranType.PAY_ONE_BILL_OTHER_VALUE ||
                    tranType == MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE
                        /* todo hungtt sua ngay 12/02, 123 phim khong can nhan noti nay
                        || tranType == MomoProto.TranHisV1.TranType.PHIM123_VALUE
                        */
            )) {

                int rcvNumber = DataUtil.strToInt(tranRplFromSoap.getString(colName.TranDBCols.PARTNER_ID, "0"));

                //lay thong tin goc cho ben nhan tien
                long oldAmount = tranRplFromSoap.getLong(Const.AppClient.OldAmount, 0);
                String oldComment = tranRplFromSoap.getString(Const.AppClient.OldComment, "");

                //lay so tien goc
                if (oldAmount > 0) {
                    tranRplFromSoap.putNumber(colName.TranDBCols.AMOUNT, oldAmount);
                    tranRplFromSoap.removeField(Const.AppClient.OldAmount);
                }

                //lay comment goc
                tranRplFromSoap.putString(colName.TranDBCols.COMMENT, oldComment);
                tranRplFromSoap.removeField(Const.AppClient.OldComment);

                //gui thong tin cho nguoi nhan tien
                sendMoneyRecv(rcvNumber
                        , tranRplFromSoap
                        , msg.cmdPhone
                        , ((data != null && data.getPhoneObj() != null) ? data.getPhoneObj().name : "")
                        , ((data != null && data.getPhoneObj() != null) ? data.getPhoneObj().cardId : "")
                        , 1);
            }

            if (sock == null) {
                StatisticUtils.fireTrans(vertx.eventBus(), tranObj.owner_number, StatisticModels.Action.Channel.WEB);
            } else {
                StatisticUtils.fireTrans(vertx.eventBus(), tranObj.owner_number, StatisticModels.Action.Channel.MOBILE);
            }

            //Calls from web
            if (callback != null) {
                callback.handle(tranRplFromSoap);
            }
        }
    }

    public void sendTransReplyByTran(
            final MomoMessage msg
            , final TranObj tran
            , final TransDb transDb
            , final NetSocket sock
    ) {

        transDb.upsertTran(msg.cmdPhone, tran.getJSON(), new Handler<TranObj>() {
            @Override
            public void handle(TranObj tranObj) {
            }
        });

        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.TRANS_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                MomoProto.TranHisV1.newBuilder()
                        .setTranId(tran.tranId)
                        .setClientTime(tran.clientTime)
                        .setAckTime(tran.ackTime)
                        .setFinishTime(tran.finishTime)
                        .setTranType(tran.tranType)
                        .setIo(tran.io)
                        .setCategory(tran.category)
                        .setPartnerId(tran.partnerId == null ? "" : tran.partnerId)
                        .setPartnerCode(tran.parterCode == null ? "" : tran.parterCode)
                        .setPartnerName(tran.partnerName == null ? "" : tran.partnerName)
                        .setPartnerRef(tran.partnerRef == null ? "" : tran.partnerRef)
                        .setBillId(tran.billId == null ? "" : tran.billId)
                        .setAmount(tran.amount)
                        .setComment(tran.comment == null ? "" : tran.comment)
                        .setCommandInd(msg.cmdIndex)
                        .setStatus(tran.status)
                        .setError(tran.error)
                        .setSourceFrom(tran.source_from)
                        .setDesc(tran.desc == null ? "" : tran.desc)
                        .setShare(tran.share == null ? "[]" : tran.share.toString())
                        .addAllKvp(getKvp(tran))
                        .build().toByteArray()
        );

        writeDataToSocket(sock, buf);

    }

    private List<MomoProto.TextValue> getKvp(TranObj tran) {
        List<MomoProto.TextValue> kvp = new ArrayList<>();
        if (tran.kvp == null)
            return kvp;
        for (String field : tran.kvp.getFieldNames()) {
            String value = tran.kvp.getString(field);
            if (value != null)
                kvp.add(MomoProto.TextValue.newBuilder().setText(field).setValue(value).build());
        }
        return kvp;
    }


    public void sendMoneyRecv(final int recvNumber
            , final JsonObject senderTranRpl
            , final int senderPhone
            , final String senderName
            , final String senderId
            , final int notiPriority) {

        final JsonObject rcv = senderTranRpl.copy();

        rcv.putNumber(colName.TranDBCols.IO, 1);
        //sender name
        rcv.putString(colName.TranDBCols.PARTNER_NAME, senderName);
        //sender number
        rcv.putString(colName.TranDBCols.PARTNER_ID, "0" + senderPhone);
        //sender cardId
        rcv.putString(colName.TranDBCols.PARTNER_CODE, senderId);

        rcv.putNumber(colName.TranDBCols.AMOUNT, senderTranRpl.getLong(colName.TranDBCols.AMOUNT, 0));

        long amount = senderTranRpl.getLong(colName.TranDBCols.AMOUNT, 0);
        rcv.putNumber(colName.TranDBCols.AMOUNT, amount);
        rcv.putArray(colName.TranDBCols.SHARE, new JsonArray());

        //save tranhis of receiver
        saveTranDb(recvNumber, rcv);

        BroadcastHandler.LocalMsgHelper helperTran = new BroadcastHandler.LocalMsgHelper();
        helperTran.setType(SoapProto.Broadcast.MsgType.MONEY_RECV_VALUE);
        helperTran.setSenderNumber(senderPhone);
        helperTran.setReceivers("0" + recvNumber);
        helperTran.setExtra(rcv);

        vertx.eventBus().publish(Misc.getNumberBus(recvNumber), helperTran.getJsonObject());

        TranObj tranRcvObj = new TranObj(rcv);
        tranRcvObj.owner_number = recvNumber;
        tranRcvObj.owner_name = "0" + recvNumber;

        //kiem tra la giao dich QRcode
        String payFor = rcv.getString(colName.TranDBCols.BILL_ID, "");

        long tranId = senderTranRpl.getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());

        if ("qrcode".equalsIgnoreCase(payFor)) {
            String tpl = "Quý khách đã nhận thành công số tiền %sđ từ %s (%s).";
            String comment = String.format(tpl, Misc.formatAmount(amount).replace(",", "."), senderName, "0" + senderPhone);
            rcv.putString(colName.TranDBCols.COMMENT, comment);

            String smsTmp = "Chuc mung quy khach da nhan duoc %sd tu %s(%s) luc %s. TID: %s. Xin cam on.";

            String sms = String.format(smsTmp
                    , Misc.formatAmount(amount).replace(",", ".")
                    , senderName
                    , "0" + senderPhone
                    , Misc.getDate(System.currentTimeMillis())
                    , tranId + ""
            );

            Notification noti = new Notification();
            noti.receiverNumber = recvNumber;
            noti.caption = "Nhận tiền thành công";
            noti.body = comment;
            noti.sms = sms;
            noti.priority = notiPriority;
            noti.time = System.currentTimeMillis();
            noti.tranId = tranId;
            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

            vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                    , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                }
            });

        } else {
            BuildLog log = new BuildLog(logger);
            log.add("createRequestPushNotification ----------------------> ", "three");
            vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                    , NotificationUtils.createRequestPushNotification(recvNumber, notiPriority, tranRcvObj));
        }
    }

    public void sendMoneyPopupRecv(final int recvNumber
            , final JsonObject senderTranRpl
            , final int senderPhone
            , final String senderName
            , final String senderId
            , final int notiPriority
            , final String title
            , final String body) {

        final JsonObject rcv = senderTranRpl.copy();

        rcv.putNumber(colName.TranDBCols.IO, 1);
        //sender name
        rcv.putString(colName.TranDBCols.PARTNER_NAME, senderName);
        //sender number
        rcv.putString(colName.TranDBCols.PARTNER_ID, "0" + senderPhone);
        //sender cardId
        rcv.putString(colName.TranDBCols.PARTNER_CODE, senderId);

        rcv.putNumber(colName.TranDBCols.AMOUNT, senderTranRpl.getLong(colName.TranDBCols.AMOUNT, 0));

        long amount = senderTranRpl.getLong(colName.TranDBCols.AMOUNT, 0);
        rcv.putNumber(colName.TranDBCols.AMOUNT, amount);
        rcv.putArray(colName.TranDBCols.SHARE, new JsonArray());

        //save tranhis of receiver
        saveTranDb(recvNumber, rcv);

        BroadcastHandler.LocalMsgHelper helperTran = new BroadcastHandler.LocalMsgHelper();
        helperTran.setType(SoapProto.Broadcast.MsgType.MONEY_RECV_VALUE);
        helperTran.setSenderNumber(senderPhone);
        helperTran.setReceivers("0" + recvNumber);
        helperTran.setExtra(rcv);

        vertx.eventBus().publish(Misc.getNumberBus(recvNumber), helperTran.getJsonObject());

        TranObj tranRcvObj = new TranObj(rcv);
        tranRcvObj.owner_number = recvNumber;
        tranRcvObj.owner_name = "0" + recvNumber;

        //kiem tra la giao dich QRcode
//        String payFor = rcv.getString(colName.TranDBCols.BILL_ID, "");

        long tranId = senderTranRpl.getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());

//        if ("qrcode".equalsIgnoreCase(payFor)) {
//            String tpl = "Quý khách đã nhận thành công số tiền %sđ từ %s (%s).";
//            String comment = String.format(tpl, Misc.formatAmount(amount).replace(",", "."), senderName, "0" + senderPhone);
                String comment = body;
                rcv.putString(colName.TranDBCols.COMMENT, comment);

//            String smsTmp = "Chuc mung quy khach da nhan duoc %sd tu %s(%s) luc %s. TID: %s. Xin cam on.";
//
//            String sms = String.format(smsTmp
//                    , Misc.formatAmount(amount).replace(",", ".")
//                    , senderName
//                    , "0" + senderPhone
//                    , Misc.getDate(System.currentTimeMillis())
//                    , tranId + ""
//            );
            String sms = Misc.removeAccent(body);
            Notification noti = new Notification();
            noti.receiverNumber = recvNumber;
            noti.caption = title;
            noti.body = comment;
            noti.sms = sms;
            noti.priority = notiPriority;
            noti.time = System.currentTimeMillis();
            noti.tranId = tranId;
            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

            vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                    , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                }
            });

//        } else {
//            BuildLog log = new BuildLog(logger);
//            log.add("createRequestPushNotification ----------------------> ", "three");
//            vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
//                    , NotificationUtils.createRequestPushNotification(recvNumber, notiPriority, tranRcvObj));
//        }
    }

    //    public void sendCurrentAgentInfo(Vertx vertx, final NetSocket sock,
//                                            final long cmdIndex,
//                                            final int cmdPhone,
//                                            final SockData sockData) {
//
//        logger.debug("Send agent info with cmdIndex, cmdPhone " + cmdIndex  +"," + cmdPhone);
//
//        final BuildLog log = new BuildLog(logger);
//        log.setPhoneNumber("0" + cmdPhone);
//        log.add("function","sendCurrentAgentInfo");
//
//        JsonObject jo = new JsonObject();
//        jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
//        jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER,cmdPhone);
//
//        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> jsonRpl) {
//                final long balance = jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE,0);
//
//                final long point = jsonRpl.body().getLong(colName.CoreBalanceCols.POINT,0);
//
//                if(sockData == null || sockData.getPhoneObj() == null){
//                    phonesDb.getPhoneObjInfo(cmdPhone,new Handler<PhonesDb.Obj>() {
//                        @Override
//                        public void handle(PhonesDb.Obj phoneObj) {
//                            sockData.setPhoneObj(phoneObj,logger,"");
//                            String referal = phoneObj.referenceNumber > 0 ? "0" + phoneObj.referenceNumber : "";
//
//                            Buffer buf = MomoMessage.buildBuffer(
//                                    MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
//                                    , cmdIndex
//                                    , cmdPhone
//                                    , MomoProto.AgentInfo.newBuilder()
//                                            .setResult(true)
//                                            .setName(phoneObj.name)
//                                            .setCardId(phoneObj.cardId)
//                                            .setDateOfBirth(phoneObj.dateOfBirth)
//                                            .setNonameCount(phoneObj.noNameTranCount)
//                                            .setPoint(point)
//                                            .setMomo(balance)
//                                            .setMload(phoneObj.mload)
//                                            .setBankName(phoneObj.bank_name)
//                                            .setBankCode(phoneObj.bank_code)
//                                            .setAddress(phoneObj.address)
//                                            .setEmail(phoneObj.email)
//                                            .setBankAccount(phoneObj.bank_account)
//                                            .setRegStatus(MomoProto.RegStatus.newBuilder()
//                                                    .setIsSetup(true)
//                                                    .setIsNamed(phoneObj.isNamed)
//                                                    .setIsReged(phoneObj.isReged)
//                                                    .setIsActive(phoneObj.isActived)
//                                                    .build())
//                                            .addListKeyValue(MomoProto.TextValue.newBuilder()
//                                                            .setText(Const.AppClient.Referal)
//                                                            .setValue(referal))
//                                            .build()
//                                            .toByteArray()
//                            );
//
//                            writeDataToSocket(sock, buf);
//
//                            log.add("result",true);
//                            log.add("name",phoneObj.name);
//                            log.add("cardId",phoneObj.cardId);
//                            log.add("dateOfBirth",phoneObj.dateOfBirth);
//                            log.add("address",phoneObj.address);
//                            log.add("email",phoneObj.email);
//                            log.add("balance",balance);
//                            log.add("bank_name",phoneObj.bank_name);
//                            log.add("bank_code",phoneObj.bank_code);
//                            log.add("bank_account",phoneObj.bank_account);
//                            log.add("isReged",phoneObj.isReged);
//                            log.add("isNamed",phoneObj.isNamed);
//                            log.add("isActived",phoneObj.isActived);
//                            log.add("isSetup",phoneObj.isSetup);
//
//                            log.writeLog();
//                        }
//                    });
//                }else {
//                    PhonesDb.Obj coreResult = sockData.getPhoneObj();
//                    String referal = coreResult.referenceNumber > 0 ? "0" + coreResult.referenceNumber : "";
//                    Buffer buf = MomoMessage.buildBuffer(
//                            MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
//                            , cmdIndex
//                            , cmdPhone
//                            , MomoProto.AgentInfo.newBuilder()
//                                    .setResult(true)
//                                    .setName(coreResult.name)
//                                    .setCardId(coreResult.cardId)
//                                    .setDateOfBirth(coreResult.dateOfBirth)
//                                    .setNonameCount(coreResult.noNameTranCount)
//                                    .setPoint(point)
//                                    .setMomo(balance)
//                                    .setMload(coreResult.mload)
//                                    .setBankName(coreResult.bank_name)
//                                    .setBankCode(coreResult.bank_code)
//                                    .setAddress(coreResult.address)
//                                    .setEmail(coreResult.email)
//                                    .setBankAccount(coreResult.bank_account)
//                                    .setRegStatus(MomoProto.RegStatus.newBuilder()
//                                            .setIsSetup(true)
//                                            .setIsNamed(coreResult.isNamed)
//                                            .setIsReged(coreResult.isReged)
//                                            .setIsActive(coreResult.isActived)
//                                            .build())
//                                    .addListKeyValue(MomoProto.TextValue.newBuilder()
//                                            .setText(Const.AppClient.Referal)
//                                            .setValue(referal))
//                                    .build()
//                                    .toByteArray()
//                    );
//
//                    writeDataToSocket(sock, buf);
//
//                    log.add("result",true);
//                    log.add("name",coreResult.name);
//                    log.add("cardId",coreResult.cardId);
//                    log.add("dateOfBirth",coreResult.dateOfBirth);
//                    log.add("address",coreResult.address);
//                    log.add("email",coreResult.email);
//                    log.add("balance",balance);
//                    log.add("bank_name",coreResult.bank_name);
//                    log.add("bank_code",coreResult.bank_code);
//                    log.add("bank_account",coreResult.bank_account);
//                    log.add("isReged",coreResult.isReged);
//                    log.add("isNamed",coreResult.isNamed);
//                    log.add("isActived",coreResult.isActived);
//                    log.add("isSetup",coreResult.isSetup);
//                    log.writeLog();
//                }
//            }
//        });
//    }
    public void sendCurrentAgentInfo(Vertx vertx, final NetSocket sock,
                                     final long cmdIndex,
                                     final int cmdPhone,
                                     final SockData sockData) {

        logger.debug("Send agent info with cmdIndex, cmdPhone " + cmdIndex + "," + cmdPhone);

        final BuildLog log = new BuildLog(logger);
        log.setPhoneNumber("0" + cmdPhone);
        log.add("function", "sendCurrentAgentInfo");

        JsonObject jo = new JsonObject();
        jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
        jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER, cmdPhone);

        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonRpl) {
                final int error = jsonRpl.body().getInteger(StringConstUtil.ERROR, 0);
                if (error != 0) {
                    log.add("function", "money is 0 " + error + " " + jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0));
                    log.writeLog();
                    return;
                }
                final long balance = jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0);

                final long point = jsonRpl.body().getLong(colName.CoreBalanceCols.POINT, 0);

                agentsDb.getOneAgent("0" + cmdPhone, "sendCurrentAgentInfo", new Handler<AgentsDb.StoreInfo>() {
                    @Override
                    public void handle(final AgentsDb.StoreInfo storeInfo) {
//                        if (sockData != null && sockData.getPhoneObj() == null) {
                        if (sockData != null) {
                            phonesDb.getPhoneObjInfo(cmdPhone, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(PhonesDb.Obj phoneObj) {
                                    sockData.setPhoneObj(phoneObj, logger, "");
                                    String referal = phoneObj.referenceNumber > 0 ? "0" + phoneObj.referenceNumber : "";
                                    final String bankCode = phoneObj.bank_code;
                                    final String bankName = convertBankName(bankCode).equalsIgnoreCase("") ? phoneObj.bank_name : convertBankName(bankCode);
                                    String name = storeInfo != null ? storeInfo.storeName : phoneObj.name;
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
                                            , cmdIndex
                                            , cmdPhone
                                            , MomoProto.AgentInfo.newBuilder()
                                                    .setResult(true)
                                                    .setName(name)
                                                    .setCardId(phoneObj.cardId)
                                                    .setDateOfBirth(phoneObj.dateOfBirth)
                                                    .setNonameCount(phoneObj.noNameTranCount)
                                                    .setPoint(point)
                                                    .setMomo(balance)
                                                    .setMload(phoneObj.mload)
//                                                    .setBankName(phoneObj.bank_name)
                                                    .setBankName(bankName)
                                                    .setBankCode(bankCode)
                                                    .setAddress(phoneObj.address)
                                                    .setEmail(phoneObj.email)
                                                    .setBankAccount(phoneObj.bank_account)
                                                    .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                            .setIsSetup(true)
                                                            .setIsNamed(phoneObj.isNamed)
                                                            .setIsReged(phoneObj.isReged)
                                                            .setIsActive(phoneObj.isActived)
                                                            .build())
                                                    .addListKeyValue(MomoProto.TextValue.newBuilder()
                                                            .setText(Const.AppClient.Referal)
                                                            .setValue(referal))
                                                    .build()
                                                    .toByteArray()
                                    );
                                    if(sock != null)
                                    {
                                        writeDataToSocket(sock, buf);
                                    }


                                    log.add("result", true);
                                    log.add("name", phoneObj.name);
                                    log.add("cardId", phoneObj.cardId);
                                    log.add("dateOfBirth", phoneObj.dateOfBirth);
                                    log.add("address", phoneObj.address);
                                    log.add("email", phoneObj.email);
                                    log.add("balance", balance);
                                    log.add("bank_name", bankName);
                                    log.add("bank_code", bankCode);
                                    log.add("bank_account", phoneObj.bank_account);
                                    log.add("isReged", phoneObj.isReged);
                                    log.add("isNamed", phoneObj.isNamed);
                                    log.add("isActived", phoneObj.isActived);
                                    log.add("isSetup", phoneObj.isSetup);

                                    log.writeLog();
                                }
                            });
                        } else if(sockData != null){
                            PhonesDb.Obj coreResult = sockData.getPhoneObj();
                            String referal = coreResult.referenceNumber > 0 ? "0" + coreResult.referenceNumber : "";
                            String name = storeInfo != null ? storeInfo.storeName : coreResult.name;
                            final String bankCode = sockData.isNewMapUpdate ? sockData.bank_code : coreResult.bank_code;
                            final String bankName = sockData.isNewMapUpdate ? sockData.bank_name :
                                    (convertBankName(bankCode).equalsIgnoreCase("") ? coreResult.bank_name : convertBankName(bankCode));
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
                                    , cmdIndex
                                    , cmdPhone
                                    , MomoProto.AgentInfo.newBuilder()
                                            .setResult(true)
                                            .setName(name)
                                            .setCardId(coreResult.cardId)
                                            .setDateOfBirth(coreResult.dateOfBirth)
                                            .setNonameCount(coreResult.noNameTranCount)
                                            .setPoint(point)
                                            .setMomo(balance)
                                            .setMload(coreResult.mload)
//                                            .setBankName(coreResult.bank_name)
                                            .setBankName(bankName)
                                            .setBankCode(bankCode)
                                            .setAddress(coreResult.address)
                                            .setEmail(coreResult.email)
                                            .setBankAccount(coreResult.bank_account)
                                            .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                    .setIsSetup(true)
                                                    .setIsNamed(coreResult.isNamed)
                                                    .setIsReged(coreResult.isReged)
                                                    .setIsActive(coreResult.isActived)
                                                    .build())
                                            .addListKeyValue(MomoProto.TextValue.newBuilder()
                                                    .setText(Const.AppClient.Referal)
                                                    .setValue(referal))
                                            .build()
                                            .toByteArray()
                            );

                            if(sock != null)
                            {
                                writeDataToSocket(sock, buf);
                            }


                            log.add("result", true);
                            log.add("name", coreResult.name);
                            log.add("cardId", coreResult.cardId);
                            log.add("dateOfBirth", coreResult.dateOfBirth);
                            log.add("address", coreResult.address);
                            log.add("email", coreResult.email);
                            log.add("balance", balance);
                            log.add("bank_name", bankName);
                            log.add("bank_code", bankCode);
                            log.add("bank_account", coreResult.bank_account);
                            log.add("isReged", coreResult.isReged);
                            log.add("isNamed", coreResult.isNamed);
                            log.add("isActived", coreResult.isActived);
                            log.add("isSetup", coreResult.isSetup);
                            log.writeLog();
                        }
                    }
                });
            }
        });
    }

    public void sendCurrentAgentInfoWithDefaultMoney(Vertx vertx, final NetSocket sock,
                                     final long cmdIndex,
                                     final int cmdPhone,
                                     final long amount,
                                     final SockData sockData) {

        logger.debug("Send agent info with cmdIndex, cmdPhone " + cmdIndex + "," + cmdPhone);

        final BuildLog log = new BuildLog(logger);
        log.setPhoneNumber("0" + cmdPhone);
        log.add("function", "sendCurrentAgentInfo");

        JsonObject jo = new JsonObject();
        jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
        jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER, cmdPhone);

        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonRpl) {
                final int error = jsonRpl.body().getInteger(StringConstUtil.ERROR, 0);
                if (error != 0) {
                    log.add("function", "money is 0 " + error + " " + jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0));
                    log.writeLog();
                    return;
                }
                final long balance = jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0);
                final long final_balance = balance == 0 ? amount : balance;
                final long point = jsonRpl.body().getLong(colName.CoreBalanceCols.POINT, 0);

                agentsDb.getOneAgent("0" + cmdPhone, "sendCurrentAgentInfo", new Handler<AgentsDb.StoreInfo>() {
                    @Override
                    public void handle(final AgentsDb.StoreInfo storeInfo) {
                        if (sockData == null || sockData.getPhoneObj() == null) {
                            phonesDb.getPhoneObjInfo(cmdPhone, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(PhonesDb.Obj phoneObj) {
                                    sockData.setPhoneObj(phoneObj, logger, "");
                                    String referal = phoneObj.referenceNumber > 0 ? "0" + phoneObj.referenceNumber : "";
                                    final String bankName = convertBankName(phoneObj.bank_code).equalsIgnoreCase("") ? phoneObj.bank_name : convertBankName(phoneObj.bank_code);
                                    String name = storeInfo != null ? storeInfo.storeName : phoneObj.name;
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
                                            , cmdIndex
                                            , cmdPhone
                                            , MomoProto.AgentInfo.newBuilder()
                                                    .setResult(true)
                                                    .setName(name)
                                                    .setCardId(phoneObj.cardId)
                                                    .setDateOfBirth(phoneObj.dateOfBirth)
                                                    .setNonameCount(phoneObj.noNameTranCount)
                                                    .setPoint(point)
                                                    .setMomo(final_balance)
                                                    .setMload(phoneObj.mload)
//                                                    .setBankName(phoneObj.bank_name)
                                                    .setBankName(bankName)
                                                    .setBankCode(phoneObj.bank_code)
                                                    .setAddress(phoneObj.address)
                                                    .setEmail(phoneObj.email)
                                                    .setBankAccount(phoneObj.bank_account)
                                                    .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                            .setIsSetup(true)
                                                            .setIsNamed(phoneObj.isNamed)
                                                            .setIsReged(phoneObj.isReged)
                                                            .setIsActive(phoneObj.isActived)
                                                            .build())
                                                    .addListKeyValue(MomoProto.TextValue.newBuilder()
                                                            .setText(Const.AppClient.Referal)
                                                            .setValue(referal))
                                                    .build()
                                                    .toByteArray()
                                    );

                                    writeDataToSocket(sock, buf);

                                    log.add("result", true);
                                    log.add("name", phoneObj.name);
                                    log.add("cardId", phoneObj.cardId);
                                    log.add("dateOfBirth", phoneObj.dateOfBirth);
                                    log.add("address", phoneObj.address);
                                    log.add("email", phoneObj.email);
                                    log.add("balance", final_balance);
                                    log.add("bank_name", phoneObj.bank_name);
                                    log.add("bank_code", phoneObj.bank_code);
                                    log.add("bank_account", phoneObj.bank_account);
                                    log.add("isReged", phoneObj.isReged);
                                    log.add("isNamed", phoneObj.isNamed);
                                    log.add("isActived", phoneObj.isActived);
                                    log.add("isSetup", phoneObj.isSetup);

                                    log.writeLog();
                                }
                            });
                        } else {
                            PhonesDb.Obj coreResult = sockData.getPhoneObj();
                            String referal = coreResult.referenceNumber > 0 ? "0" + coreResult.referenceNumber : "";
                            String name = storeInfo != null ? storeInfo.storeName : coreResult.name;
                            final String bankName = convertBankName(coreResult.bank_code).equalsIgnoreCase("") ? coreResult.bank_name : convertBankName(coreResult.bank_code);
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
                                    , cmdIndex
                                    , cmdPhone
                                    , MomoProto.AgentInfo.newBuilder()
                                            .setResult(true)
                                            .setName(name)
                                            .setCardId(coreResult.cardId)
                                            .setDateOfBirth(coreResult.dateOfBirth)
                                            .setNonameCount(coreResult.noNameTranCount)
                                            .setPoint(point)
                                            .setMomo(final_balance)
                                            .setMload(coreResult.mload)
//                                            .setBankName(coreResult.bank_name)
                                            .setBankName(bankName)
                                            .setBankCode(coreResult.bank_code)
                                            .setAddress(coreResult.address)
                                            .setEmail(coreResult.email)
                                            .setBankAccount(coreResult.bank_account)
                                            .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                    .setIsSetup(true)
                                                    .setIsNamed(coreResult.isNamed)
                                                    .setIsReged(coreResult.isReged)
                                                    .setIsActive(coreResult.isActived)
                                                    .build())
                                            .addListKeyValue(MomoProto.TextValue.newBuilder()
                                                    .setText(Const.AppClient.Referal)
                                                    .setValue(referal))
                                            .build()
                                            .toByteArray()
                            );

                            writeDataToSocket(sock, buf);

                            log.add("result", true);
                            log.add("name", coreResult.name);
                            log.add("cardId", coreResult.cardId);
                            log.add("dateOfBirth", coreResult.dateOfBirth);
                            log.add("address", coreResult.address);
                            log.add("email", coreResult.email);
                            log.add("balance", final_balance);
                            log.add("bank_name", coreResult.bank_name);
                            log.add("bank_code", coreResult.bank_code);
                            log.add("bank_account", coreResult.bank_account);
                            log.add("isReged", coreResult.isReged);
                            log.add("isNamed", coreResult.isNamed);
                            log.add("isActived", coreResult.isActived);
                            log.add("isSetup", coreResult.isSetup);
                            log.writeLog();
                        }
                    }
                });
            }
        });
    }

    public String convertBankName(String bankCode) {
        String bankName = "";
        switch (bankCode) {
            case "569366":
            case "104":
                bankName = "NH Phương Đông";
                break;
        }
        if (StringUtils.isEmpty(bankName)) {
            bankName = StringConstUtil.bankLinkNames.containsKey(bankCode) ? StringConstUtil.bankLinkNames.get(bankCode) : "";
        }
        return bankName;
    }

    public void sendCurrentAgentInfoWithCallback(final Vertx vertx, final NetSocket sock,
                                                 final long cmdIndex,
                                                 final int cmdPhone,
                                                 final SockData sockData, final Handler<JsonObject> callback) {

        logger.debug("Send agent info with cmdIndex, cmdPhone " + cmdIndex + "," + cmdPhone);

        final BuildLog log = new BuildLog(logger);
        log.setPhoneNumber("0" + cmdPhone);
        log.add("function", "sendCurrentAgentInfo");

        final JsonObject jo = new JsonObject();
        jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
        jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER, cmdPhone);

        agentsDb.getOneAgent("0" + cmdPhone, "sendCurrentAgentInfo", new Handler<AgentsDb.StoreInfo>() {
            @Override
            public void handle(final AgentsDb.StoreInfo storeInfo) {

                vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> jsonRpl) {
                        final long balance = jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0);

                        final long point = jsonRpl.body().getLong(colName.CoreBalanceCols.POINT, 0);

                        if (sockData == null || sockData.getPhoneObj() == null) {
                            phonesDb.getPhoneObjInfo(cmdPhone, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(PhonesDb.Obj phoneObj) {
                                    sockData.setPhoneObj(phoneObj, logger, "");
                                    String referal = phoneObj.referenceNumber > 0 ? "0" + phoneObj.referenceNumber : "";

                                    String name = storeInfo != null ? storeInfo.storeName : phoneObj.name;
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
                                            , cmdIndex
                                            , cmdPhone
                                            , MomoProto.AgentInfo.newBuilder()
                                                    .setResult(true)
                                                    .setName(name)
                                                    .setCardId(phoneObj.cardId)
                                                    .setDateOfBirth(phoneObj.dateOfBirth)
                                                    .setNonameCount(phoneObj.noNameTranCount)
                                                    .setPoint(point)
                                                    .setMomo(balance)
                                                    .setMload(phoneObj.mload)
                                                    .setBankName(phoneObj.bank_name)
                                                    .setBankCode(phoneObj.bank_code)
                                                    .setAddress(phoneObj.address)
                                                    .setEmail(phoneObj.email)
                                                    .setBankAccount(phoneObj.bank_account)
                                                    .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                            .setIsSetup(true)
                                                            .setIsNamed(phoneObj.isNamed)
                                                            .setIsReged(phoneObj.isReged)
                                                            .setIsActive(phoneObj.isActived)
                                                            .build())
                                                    .addListKeyValue(MomoProto.TextValue.newBuilder()
                                                            .setText(Const.AppClient.Referal)
                                                            .setValue(referal))
                                                    .build()
                                                    .toByteArray()
                                    );

                                    writeDataToSocket(sock, buf);

                                    log.add("result", true);
                                    log.add("name", phoneObj.name);
                                    log.add("cardId", phoneObj.cardId);
                                    log.add("dateOfBirth", phoneObj.dateOfBirth);
                                    log.add("address", phoneObj.address);
                                    log.add("email", phoneObj.email);
                                    log.add("balance", balance);
                                    log.add("bank_name", phoneObj.bank_name);
                                    log.add("bank_code", phoneObj.bank_code);
                                    log.add("bank_account", phoneObj.bank_account);
                                    log.add("isReged", phoneObj.isReged);
                                    log.add("isNamed", phoneObj.isNamed);
                                    log.add("isActived", phoneObj.isActived);
                                    log.add("isSetup", phoneObj.isSetup);

                                    log.writeLog();
                                }
                            });
                        } else {
                            PhonesDb.Obj coreResult = sockData.getPhoneObj();
                            String referal = coreResult.referenceNumber > 0 ? "0" + coreResult.referenceNumber : "";
                            String name = storeInfo != null ? storeInfo.storeName : coreResult.name;
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE
                                    , cmdIndex
                                    , cmdPhone
                                    , MomoProto.AgentInfo.newBuilder()
                                            .setResult(true)
                                            .setName(name)
                                            .setCardId(coreResult.cardId)
                                            .setDateOfBirth(coreResult.dateOfBirth)
                                            .setNonameCount(coreResult.noNameTranCount)
                                            .setPoint(point)
                                            .setMomo(balance)
                                            .setMload(coreResult.mload)
                                            .setBankName(coreResult.bank_name)
                                            .setBankCode(coreResult.bank_code)
                                            .setAddress(coreResult.address)
                                            .setEmail(coreResult.email)
                                            .setBankAccount(coreResult.bank_account)
                                            .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                    .setIsSetup(true)
                                                    .setIsNamed(coreResult.isNamed)
                                                    .setIsReged(coreResult.isReged)
                                                    .setIsActive(coreResult.isActived)
                                                    .build())
                                            .addListKeyValue(MomoProto.TextValue.newBuilder()
                                                    .setText(Const.AppClient.Referal)
                                                    .setValue(referal))
                                            .build()
                                            .toByteArray()
                            );

                            writeDataToSocket(sock, buf);

                            log.add("result", true);
                            log.add("name", coreResult.name);
                            log.add("cardId", coreResult.cardId);
                            log.add("dateOfBirth", coreResult.dateOfBirth);
                            log.add("address", coreResult.address);
                            log.add("email", coreResult.email);
                            log.add("balance", balance);
                            log.add("bank_name", coreResult.bank_name);
                            log.add("bank_code", coreResult.bank_code);
                            log.add("bank_account", coreResult.bank_account);
                            log.add("isReged", coreResult.isReged);
                            log.add("isNamed", coreResult.isNamed);
                            log.add("isActived", coreResult.isActived);
                            log.add("isSetup", coreResult.isSetup);
                            log.writeLog();
                        }
                        JsonObject jsonRep = new JsonObject();
                        callback.handle(jsonRep);
                    }
                }); // END UMARKET
            }
        });
    }

    public void logInfoForSock(NetSocket sock, String info, SockData data) {
        if (sock != null) {
            logger.debug(sock.remoteAddress().toString() + (data != null ? " phone number : " + data.getNumber() : "") + " >> " + info);
        } else {
            logger.debug("logInfoForSock :map SockData is null for this sock, SOCKET HAS BEEN KILLED");
        }
    }

    public void sendACK(final NetSocket sock, final MomoMessage msg) {

        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.ACK_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , null);

        writeDataToSocket(sock, buf);
    }

    public void sendTranAck(final NetSocket sock, final MomoMessage msg) {
        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.TRAN_ACK_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , null);

        writeDataToSocket(sock, buf);
    }

    public void updateSessionTime(final int number, long curTime) {
        phonesDb.updateSessionTime(number, curTime, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                logger.debug("updateSessionTime result :" + aBoolean);
            }
        });
    }


    private void saveTranDb(final int phoneNumber, final JsonObject transReply) {

        transDb.upsertTran(phoneNumber, transReply, new Handler<TranObj>() {
            @Override
            public void handle(TranObj tranObj) {
                logger.debug("SAVE TRAN INTO DB WITH STATUS "
                        + transReply.getInteger(colName.TranDBCols.ERROR)
                        + ",Error : " + transReply.getInteger(colName.TranDBCols.ERROR));
            }
        });
    }

    //app sms.start


    //app sms.end

    /**
     * Check session expire one more time
     *
     * @param msg      momomessage request len server
     * @param data     cache data for each socket
     * @param funcName current function name is called from client
     * @param _logger  logger to write log
     * @param sock     NetSoket for each connection of client and server
     * @param callback this object will reply when is call from web appp
     * @return sock or callback
     */

    public boolean isSessionExpiredOneMore(MomoMessage msg
            , SockData data
            , final String funcName
            , final Logger _logger
            , final NetSocket sock
            , final Handler<JsonObject> callback) {

        //validate cache data
        BuildLog log = new BuildLog(_logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", funcName);

        boolean isExpired = false;

        if (data == null) {
            isExpired = true;
            log.add("sockdata is null", "");
        } else if (data.getPhoneObj() == null) {
            isExpired = true;
            log.add("sockdata phoneObj is null", "");
        } else if ("".equalsIgnoreCase(data.pin)) {
            isExpired = true;
            log.add("sockdata pin", "empty");
        }

        if (isExpired) {

            log.writeLog();

            if (sock != null) {
                Buffer buffer = Misc.buildSessionExpired(msg);
                writeDataToSocket(sock, buffer);
            }

            if (callback != null) {
                callback.handle(new JsonObject().putNumber("error"
                                , MomoProto.TranHisV1.ResultCode.SESSION_EXPIRED_VALUE)
                                .putString("desc", "sockdata or sockdata.pin = empty")
                );
            }
        }

        return isExpired;
    }

    public void saveAndSendTempTran(final Vertx vertx
            , final MomoMessage msg
            , final JsonObject tranObj
            , final NetSocket socket
            , final SockData data) {
        //todo luu tran xuong db
        final BuildLog log = new BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "saveAndSendTempTran");
        log.add("tran JSON", tranObj);

        //1 luu DB
        tranObj.putNumber(colName.TranDBCols.COMMAND_INDEX, 0);
        transDb.upsertTranOutSideNew(msg.cmdPhone, tranObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                log.add("function", "upsertTranOutSideNew");
                log.add("isUpdated", result);

                //2.send tran
                sendCurrentAgentInfo(vertx, socket, 0, msg.cmdPhone, data);
                TranObj obj = new TranObj(tranObj);

                MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                builder.addTranList(MomoProto.TranHisV1.newBuilder()
                                .setTranId(obj.tranId)
                                .setClientTime(obj.clientTime)
                                .setAckTime(obj.ackTime)
                                .setFinishTime(obj.finishTime)
                                .setTranType(obj.tranType)
                                .setIo(obj.io)
                                .setCategory(obj.category)
                                .setPartnerId(obj.partnerId)
                                .setPartnerCode(obj.parterCode)
                                .setPartnerName(obj.partnerName)
                                .setPartnerRef(obj.partnerRef)
                                .setBillId(obj.billId)
                                .setAmount(obj.amount)
                                .setComment(obj.comment)
                                .setStatus(obj.status)
                                .setError(obj.error)
                                .setCommandInd(obj.cmdId)
                );

                Buffer buff = MomoMessage.buildBuffer(
                        MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                        System.currentTimeMillis(),
                        msg.cmdPhone,
                        builder.setResult(true)
                                .build()
                                .toByteArray()
                );

                writeDataToSocket(socket, buff);

                log.writeLog();
            }
        });
    }

    public boolean serviceIsRunning(final NetSocket sock
            , final MomoMessage msg
            , final HashMap<Integer, Boolean> map
            , final Handler<JsonObject> webcallback

    ) {
        MomoProto.TranHisV1 tranHisV1;
        try {
            tranHisV1 = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            tranHisV1 = null;
        }

        //khong parse duoc --> vao trong xu ly
        if (tranHisV1 == null) return true;

        //khong chua dich vu nay --> xem nhu dang chay
        int tranType = tranHisV1.getTranType();
        if (!map.containsKey(tranType)) return true;

        boolean isRunning = map.get(tranType);

        //dich vu dang tam dung
        if (!isRunning) {
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.TRANS_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.TranHisV1.newBuilder()
                            .setTranId(0)
                            .setClientTime(tranHisV1.getClientTime())
                            .setAckTime(System.currentTimeMillis())
                            .setFinishTime(System.currentTimeMillis())
                            .setTranType(tranHisV1.getTranType())
                            .setIo(tranHisV1.getIo())
                            .setCategory(tranHisV1.getCategory())
                            .setPartnerId(tranHisV1.getPartnerId() == null ? "" : tranHisV1.getPartnerId())
                            .setPartnerCode(tranHisV1.getPartnerCode() == null ? "" : tranHisV1.getPartnerCode())
                            .setPartnerName(tranHisV1.getPartnerName() == null ? "" : tranHisV1.getPartnerName())
                            .setPartnerRef(tranHisV1.getPartnerRef() == null ? "" : tranHisV1.getPartnerRef())
                            .setBillId(tranHisV1.getBillId() == null ? "" : tranHisV1.getBillId())
                            .setAmount(tranHisV1.getAmount())
                            .setComment("")
                            .setCommandInd(msg.cmdIndex)
                            .setStatus(TranObj.STATUS_FAIL)
                            .setError(MomoProto.SystemError.SERVICE_IS_PAUSED_VALUE)
                            .setSourceFrom(tranHisV1.getSourceFrom())
                            .build().toByteArray()
            );

            if (sock != null) {
                writeDataToSocket(sock, buf);
            }

            if (webcallback != null) {
                JsonObject jo = new JsonObject();
                jo.putNumber(colName.TranDBCols.TRAN_ID, 0);
                jo.putNumber(colName.TranDBCols.CLIENT_TIME, tranHisV1.getClientTime());
                jo.putNumber(colName.TranDBCols.ACK_TIME, System.currentTimeMillis());
                jo.putNumber(colName.TranDBCols.FINISH_TIME, 0);
                jo.putNumber(colName.TranDBCols.TRAN_TYPE, tranHisV1.getTranType());
                jo.putNumber(colName.TranDBCols.IO, tranHisV1.getIo());
                jo.putNumber(colName.TranDBCols.CATEGORY, tranHisV1.getCategory());
                jo.putString(colName.TranDBCols.PARTNER_ID, tranHisV1.getPartnerId() == null ? "" : tranHisV1.getPartnerId());
                jo.putString(colName.TranDBCols.PARTNER_CODE, tranHisV1.getPartnerCode() == null ? "" : tranHisV1.getPartnerCode());
                jo.putString(colName.TranDBCols.PARTNER_NAME, tranHisV1.getPartnerName() == null ? "" : tranHisV1.getPartnerName());
                jo.putString(colName.TranDBCols.PARTNER_REF, tranHisV1.getPartnerRef() == null ? "" : tranHisV1.getPartnerRef());
                jo.putString(colName.TranDBCols.BILL_ID, tranHisV1.getBillId() == null ? "" : tranHisV1.getBillId());
                jo.putNumber(colName.TranDBCols.AMOUNT, tranHisV1.getAmount());
                jo.putNumber(colName.TranDBCols.COMMAND_INDEX, msg.cmdIndex);
                jo.putNumber(colName.TranDBCols.STATUS, TranObj.STATUS_FAIL);
                jo.putNumber(colName.TranDBCols.ERROR, MomoProto.SystemError.SERVICE_IS_PAUSED_VALUE);
                jo.putNumber(colName.TranDBCols.FROM_SOURCE, tranHisV1.getSourceFrom());

                webcallback.handle(jo);
            }

            return false;
        }
        return true;
    }

    public void logInfo(int phoneNumber, long cmdIndex, String info) {
        logger.info(String.format("%d|%d|%s", phoneNumber, cmdIndex, info));
    }

    public void logInfo(MomoMessage momoMessage, String info) {
        if (momoMessage != null) {
            logInfo(momoMessage.cmdPhone, momoMessage.cmdIndex, info);
        } else {
            logger.info(info);
        }
    }

    public long AddDate(int delta) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, delta);
        Date date = cal.getTime();
        return date.getTime();
    }

    public ArrayList<ServiceDb.Obj> getServiceList(JsonArray joArr) {


        ArrayList<ServiceDb.Obj> arrayList = null;

        if (joArr == null || joArr.size() == 0) return arrayList;
        arrayList = new ArrayList();

        for (Object o : joArr) {
            JsonObject jo = (JsonObject) o;
            arrayList.add(new ServiceDb.Obj(jo));
        }
        return arrayList;
    }

    public ArrayList<ServiceDetailDb.Obj> getServiceDetailList(JsonArray joArr) {

        ArrayList<ServiceDetailDb.Obj> arrayList = null;

        if (joArr == null || joArr.size() == 0) return arrayList;
        arrayList = new ArrayList<>();

        for (Object o : joArr) {
            JsonObject jo = (JsonObject) o;
            arrayList.add(new ServiceDetailDb.Obj(jo));
        }
        return arrayList;
    }

    public static class SoapObjReply {
        public int status = -1;
        public int error = -1;
        public long tranId = -1;
        public long amount = 0;
        public int io = 0;
        public long finishTime = 0;

        public SoapObjReply(JsonObject jo) {
            status = jo.getInteger(colName.TranDBCols.STATUS, TranObj.STATUS_FAIL);
            error = jo.getInteger(colName.TranDBCols.ERROR, -1);
            tranId = jo.getLong(colName.TranDBCols.TRAN_ID, -1);
            amount = jo.getLong(colName.TranDBCols.AMOUNT, 0);
            io = jo.getInteger(colName.TranDBCols.IO, 0);
            finishTime = jo.getLong(colName.TranDBCols.FINISH_TIME, System.currentTimeMillis());
        }

        public SoapObjReply() {
        }

        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.TranDBCols.STATUS, status);
            jo.putNumber(colName.TranDBCols.ERROR, error);
            jo.putNumber(colName.TranDBCols.TRAN_ID, tranId);
            jo.putNumber(colName.TranDBCols.AMOUNT, amount);
            jo.putNumber(colName.TranDBCols.IO, io);
            jo.putNumber(colName.TranDBCols.FINISH_TIME, finishTime);
            return jo;
        }
    }

    public static class BuildLog {
        public String flag;
        private ArrayList<String> keys = new ArrayList<>();
        private ArrayList<Object> values = new ArrayList<>();
        private Logger logger;
        private long time = 0;
        private String phoneNumber = "";

        public BuildLog(Logger logger) {
            this.logger = logger;
            time = System.currentTimeMillis();
        }

        public BuildLog(Logger logger, int phone) {
            this(logger, "0" + phone);
        }

        public BuildLog(Logger logger, int phone, String flag) {
            this(logger, "0" + phone, flag);
        }

        public BuildLog(Logger logger, String phone) {
            this(logger, phone, null);
        }

        public BuildLog(Logger logger, String phone, String flag) {
            this.logger = logger;
            time = System.currentTimeMillis();
            this.phoneNumber = phone;
            this.flag = flag;
        }

        public void add(String key, Object value) {
            if (key == null) {
                logger.info("key null mat roi");
            } else {
                if (value != null) {
                    keys.add(key);
                    values.add(value);
                } else {
                    logger.info("value null for key " + key);
                }
            }
        }

        public String getPhoneNumber() {
            return this.phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public void writeLog() {
            int i = 0;
            try {

                String prefix = time + "|" + phoneNumber;
                if (flag != null) {
                    prefix += "|" + flag;
                }

                for (i = 0; i < keys.size(); i++) {

                    /*if(keys.get(i) == null || values.get(i) == null){
                        continue;
                    }*/

                    logger.info(prefix + "|" + keys.get(i) + " -> " + values.get(i));

                    /*logger.info(prefix + " " + (keys.get(i) == null ? "" : keys.get(i)) + " -> " + (values.get(i) == null ?  "" : values.get(i)));


                    if (values.get(i) != null) {
                        if ("".equalsIgnoreCase(values.get(i).toString())) {
                            logger.info(prefix + " " + (keys.get(i) == null ? "" : keys.get(i)));
                        } else {
                            logger.info(prefix + " " + (keys.get(i) == null ? "" : keys.get(i)) + " -> " + (values.get(i) == null ?  "" : values.get(i)));
                        }
                    } else {
                        logger.info(prefix + " " + (keys.get(i) == null ? "" : keys.get(i)) + " -> null");
                    }*/
                }
            } catch (Exception ex) {
                logger.info("bi null o key thu " + i);
                logger.info("key" + (keys.get(i) == null ? "null" : keys.get(i)));
                logger.info("value" + (values.get(i) == null ? "null" : values.get(i)));
            }


        }

        public void writeAndClear() {
            writeLog();
            keys.clear();
            values.clear();
        }
    }

    //service.start
    public static class ServiceReq {

        public int Command = 0;
        //get service, service detail , package list
        public String PackageType = "";
        public String ServiceId = "";
        public String PackageId = "";
        public String WholeSystemPaused = "";
        //get bank fee
        public String bankId = "";
        public Integer tranType = -1;
        public Integer channel = -1;
        public Integer inoutCity = -1;
        public Integer feeType = -1;
        public long amount = -1;
        public String serviceType = "";
        public long lastTime = 0;
        private String cmd = "cmd";
        private String packagetype = "packagetype";
        private String serviceid = "serviceid";
        private String wholesystempaused = "wholesystempaused";
        private String BANKID = "bankid";
        private String TRANTYPE = "trantype";
        private String CHANNEL = "channel";
        private String INOUTCITY = "inoutcity";
        private String FEETYPE = "feetype";
        private String AMOUNT = "amount";
        private String SERVICE_TYPE = "servicetype";
        private String LAST_TIME = "lasttime";
        private String PACKAGE_ID = "packageid";

        public ServiceReq() {
        }

        public ServiceReq(JsonObject jo) {

            Command = jo.getInteger(cmd, 0);
            PackageType = jo.getString(packagetype, "");
            ServiceId = jo.getString(serviceid, "");
            WholeSystemPaused = jo.getString(wholesystempaused, "");

            bankId = jo.getString(BANKID, "");
            tranType = jo.getInteger(TRANTYPE, -1);
            channel = jo.getInteger(CHANNEL, -1);
            inoutCity = jo.getInteger(INOUTCITY, -1);
            feeType = jo.getInteger(FEETYPE, -1);
            amount = jo.getLong(AMOUNT, -1);
            serviceType = jo.getString(SERVICE_TYPE, "");
            lastTime = jo.getLong(LAST_TIME, 0);
            PackageId = jo.getString(PACKAGE_ID, "");
        }

        ;

        public JsonObject toJSON() {
            JsonObject jo = new JsonObject();
            jo.putNumber(cmd, Command);
            jo.putString(packagetype, PackageType);
            jo.putString(serviceid, ServiceId);
            jo.putString(PACKAGE_ID, PackageId);
            jo.putString(wholesystempaused, WholeSystemPaused);

            jo.putString(BANKID, bankId);
            jo.putNumber(TRANTYPE, tranType);
            jo.putNumber(CHANNEL, channel);
            jo.putNumber(INOUTCITY, inoutCity);
            jo.putNumber(FEETYPE, feeType);
            jo.putNumber(AMOUNT, amount);
            jo.putString(SERVICE_TYPE, serviceType);
            jo.putNumber(LAST_TIME, lastTime);
            return jo;
        }

        public static class COMMAND {
            public static final int GET_SERVICE = 1;
            public static final int GET_SERVICE_DETAIL_BY_SERVICE_ID = 2;
            public static final int GET_PACKAGE_LIST = 3;

            public static final int UPDATE_SERVICE = 4;
            public static final int UPDATE_SERVICE_DETAIL_BY_SERVICE_ID = 5;
            public static final int UPDATE_PACKAGE = 6;
            public static final int GET_SERVER_ONOFF = 7;
            public static final int UPDATE_SERVER_ONOFF = 8;
            public static final int GET_MINMAXTRAN = 9;
            public static final int UPDATE_MINMAXTRAN = 10;
            public static final int GET_FEE = 11;
            public static final int UPDATE_FEE = 12;

            //check is service or not
            public static final int GET_SERVICE_TYPE = 13;

            //lay record dich vu theo ma dich vu
            public static final int GET_SERVICE_BY_SERVICE_ID = 14;

            //lay danh sach cac dich vu co thay doi ke tu thoi diem last_time client request
            public static final int GET_SERVICE_BY_LAST_TIME = 15;

            public static final int GET_PACKAGE_BY_AMOUNT = 16;

            public static final int UPDATE_CDHH = 17;
            public static final int GET_CDHH_ON_OFF = 18;

            //for CDHH
            public static final int UPDATE_CDHH_PAYBACK = 19;
            public static final int GET_PAYBACK_EVENT = 20;

            public static final int UPDATE_CDHH_CONFIG_WEEK_OR_AQUATER = 23;
            public static final int GET_CDHH_CONFIG_WEEK_OR_AQUATER_ACTIVE = 24;
            public static final int GET_PACKAGE_LIST_BY_PARENT_ID = 25;

            public static final int GET_SERVICE_CATEGORY_BY_LAST_TIME = 26;
            public static final int UPDATE_SERVICE_CATEGORY = 27;
            public static final int UPDATE_SERVICE_FORM = 28;


            //Insert new static variable
            public static final int GET_ALL_CARD_TYPE = 29;

            public static final int UPDATE_CARD_TYPE = 30;

            public static final int FORCE_LOAD_ALL_CONFIG = 31;
            public static final int GET_SERVICE_EVENT = 32;

            public static final int REFRESH_CONFIG_DATA = 33;

            public static final int GET_IRON_MAN_PROMO_TRACKING_TABLE = 34;

            public static final int REFRESH_IRON_MAN_TRACKING_TABLE = 35;

            public static final int GET_RANDOM_GIFT = 36;

            public static final int REFRESH_RANDOM_GIFT = 37;

            public static final int UPDATE_IRON_MAN_USER = 38;

            public static final int UPDATE_SERVICE_FEE = 39;

            public static final int GET_SERVICE_FEE = 40;

            public static final int GET_SHOPPING_VERTICLE_BY_SERVICE_ID = 41;

            public static final int GET_CONNECTOR_SERVICE_BUS_NAME = 42;

            public static final int UPDATE_CONNECTOR_SERVICE_BUS_NAME = 43;

            public static final int RELOAD_CONNECTOR_SERVICE_BUS_NAME = 44;

            public static final int GET_SERVICE_GIFT_RULES = 45;

            public static final int UPDATE_SERVICE_GIFT_RULES = 46;

            public static final int RELOAD_CONNECTOR_SERVICE_GIFT_RULES = 47;
        }
    }

    //for get ticket info
    public class TicketFields {
        public String RCODE = "rcode";
        public String PROVIDER_ID = "providerId";
        public String TICKET_ID = "ticketId";
        public String AMOUNT = "amt";

    }


    //service.end


}
