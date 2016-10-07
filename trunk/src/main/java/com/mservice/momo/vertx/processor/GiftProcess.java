package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.common.Popup;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.gift.*;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.util.claimcode.ClaimCodeUtils;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.ClaimHistory;
import com.mservice.momo.vertx.gift.GiftError;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.*;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nam on 10/2/14.
 */
public class GiftProcess {
    public static int SYN_GIFT_SIZE = 5;
    public String G2N_MESSAGE;
    public String LINK;
    public long TRANWITHPOINT_MIN_POINT = 0;
    public long TRANWITHPOINT_MIN_AMOUNT = 0;
    private Vertx vertx;
    private GiftManager giftManager;
    private Common common;
    private Logger logger;
    private GiftDb giftDb;
    private GiftTypeDb giftTypeDb;
    private TransDb tranDb;
    private ClaimHistoryDb claimHistoryDb;
    private GiftHistoryDb giftHistoryDb;
    private QueuedGiftDb queuedGiftDb;
    private GiftTranDb giftTranDb;
    private PhonesDb phonesDb;
    private PromotionProcess promotionProcess;
    private JsonObject jsonClaimPoint;
    public GiftProcess(Common mCom, Vertx mVertx, final Logger logger, JsonObject globalConfig) {
        this.common = mCom;
        this.vertx = mVertx;
        this.logger = logger;
        giftManager = new GiftManager(vertx, logger, globalConfig);
        giftTypeDb = new GiftTypeDb(vertx, logger);
        tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, globalConfig);

        giftDb = new GiftDb(vertx, logger);
        claimHistoryDb = new ClaimHistoryDb(vertx, logger);
        giftHistoryDb = new GiftHistoryDb(vertx, logger);

        JsonObject gift = globalConfig.getObject("gift", new JsonObject());
        queuedGiftDb = new QueuedGiftDb(vertx, logger);

        giftTranDb = new GiftTranDb(vertx, logger);

//        G2N_MESSAGE = gift.getString("g2nMessage", "Hey, mình có một món quà dành tặng bạn. Nhận quà tặng tại: <a href='http://app.momo.vn/${token}'>http://app.momo.vn/${token}</a>");
        G2N_MESSAGE = "Hey, mình có một món quà dành tặng bạn. Nhận quà tặng tại: http://app.momo.vn:8081/${token}";
        LINK = gift.getString("link", "http://app.momo.vn:8081/${token}");

        JsonObject pointConfig = globalConfig.getObject("point", new JsonObject());
        TRANWITHPOINT_MIN_POINT = pointConfig.getLong("minPoint", 0);
        TRANWITHPOINT_MIN_AMOUNT = pointConfig.getLong("mintAmount", 0);

        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        promotionProcess = new PromotionProcess(vertx, logger, globalConfig);
        jsonClaimPoint = globalConfig.getObject(StringConstUtil.ClaimPointFunction.JSON_OBJECT, new JsonObject());
    }

    public void giftClaim(final NetSocket sock, final MomoMessage msg, final SockData data, Handler<JsonObject> callback) {

        //todo for production
//        Buffer buf = MomoMessage.buildBuffer(
//                MomoProto.MsgType.GIFT_CLAIM_REPLY_VALUE,
//                msg.cmdIndex,
//                msg.cmdPhone,
//                MomoProto.GiftClaimReply.newBuilder()
//                        .setError(1)
//                        .setPoint(0)
//                        .build().toByteArray()
//        );
//        common.writeDataToSocket(sock, buf);

        //todo for test
        MomoProto.GiftClaim rawRequest;
        try {
            rawRequest = MomoProto.GiftClaim.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            rawRequest = null;
        }
        if (rawRequest == null) {
            common.writeErrorToSocket(sock);
            return;
        }
        final boolean isActive = jsonClaimPoint.getBoolean(StringConstUtil.ClaimPointFunction.IS_ACTIVE, false);
        if(!isActive)
        {
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.GIFT_CLAIM_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.GiftClaimReply.newBuilder()
                            .setError(-1)
                            .setPoint(0)
                            .build().toByteArray()
            );
            common.writeDataToSocket(sock, buf);
            return;
        }
        final String code = rawRequest.getCode().trim();
        Common.BuildLog log = new Common.BuildLog(logger);
        final long finalAmount = jsonClaimPoint.getLong(StringConstUtil.ClaimPointFunction.CLAIM_AMOUNT, 5000);
        final String agent = jsonClaimPoint.getString(StringConstUtil.ClaimPointFunction.AGENT, "cskh");
        JsonObject joExtra = new JsonObject();
        joExtra.putString(StringConstUtil.ClaimCodePromotion.CODE, code);
        joExtra.putString(StringConstUtil.ClaimCodePromotion.DEVICE_IMEI, data.imei);

        if("".equalsIgnoreCase(code.trim()))
        {
            String body = "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ";
            showInformationPopup(body ,msg, new Common.BuildLog(logger));
            return;
        }
        log.add("Claim code", "claim code");

        promotionProcess.executePromotion("", MomoProto.TranHisV1.TranType.GIFT_CLAIM_VALUE, "", msg.cmdPhone, 0,
                0, data, "giftClaim", null, 0, 1, log, new JsonObject().putString(StringConstUtil.PromotionField.CODE, code), new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {

                    }
                }
        );
        String program = ClaimCodeUtils.checkClaimCodeProgram(code);
        promotionProcess.getUserInfoToCheckPromoProgramWithCallback("", "0" + msg.cmdPhone, null, 0, MomoProto.MsgType.GIFT_CLAIM_VALUE, 0, program
                , data, joExtra, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonResponse) {
                final int err = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                String body = jsonResponse.getString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                String url = jsonResponse.getString(StringConstUtil.URL, "https://momo.vn/chiasemomo/the-le.html");
                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.GIFT_CLAIM_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        MomoProto.GiftClaimReply.newBuilder()
                                .setError(1000)
                                .setPoint(0)
                                .build().toByteArray()
                );
                //change popup
                logger.info("ERROR POPUP GIFT CLAIM REPLY " + err);
                if(err != 0 && err == 2001 ) {
                    logger.info("sendPopupReferral");
                    Misc.sendPopupReferral(vertx, "0" + msg.cmdPhone, body, PromoContentNotification.CHIA_SE_MOMO_TITLE, url);
                } else if(err != 0 && err != -3000)
                {
                    logger.info("showInformationPopup");
                    showInformationPopup(body ,msg, new Common.BuildLog(logger));
                }

//                final AtomicInteger count = new AtomicInteger(3);
//                vertx.setPeriodic(5000L, new Handler<Long>() {
//                    @Override
//                    public void handle(Long event) {
//                        if(count.decrementAndGet() < 0)
//                        {
//                            vertx.cancelTimer(event);
//                            return;
//                        }
//                        common.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);
//                    }
//                });
                common.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);
                common.writeDataToSocket(sock, buf);
                return;
            }
        });
    }

    private void showInformationPopup(final String body, final MomoMessage msg, final Common.BuildLog log) {
        //Ban popup thanh khoan cho diem giao dich
        log.add("desc", "Co popup thanh khoan " + msg.cmdPhone);
        log.writeLog();
        Popup popup = new Popup(Popup.Type.CONFIRM);
        popup.setHeader("Thông báo");
        popup.setContent(body);
        popup.setInitiator("0" + msg.cmdPhone);
        popup.setEnabledClose(false);
        popup.setOkButtonLabel("Xác nhận");
        popup.setCancelButtonLabel("Hủy");
        sendPopUpInformation(popup, msg, false, System.currentTimeMillis(), StringConstUtil.LIQUID_POPUP);
    }

    private void sendPopUpInformation(Popup popup, MomoMessage msg, boolean isConfirm, long tid, String partner) {
        JsonObject jsonExtra = new JsonObject();
        int type = isConfirm ? 1 : 0;

        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        if (isConfirm) {
            String button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }
        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, partner);
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = 0L;
        notification.cmdId = msg.cmdIndex;
        notification.time = new Date().getTime();
        notification.receiverNumber = msg.cmdPhone;
        notification.extra = jsonExtra.toString();

        if(popup != null)
        {
            Misc.sendPromotionPopupNoti(vertx, notification);
        }
    }
    // Uses in gift claim process
    public void successClaimGift(final NetSocket sock, final MomoMessage msg, final SockData data,
                                 final String fromAgent, final String pin, final String aName, final String aPhone, final String aAvatar,
                                 final long giftAmount, final String giftType, final String code, final String comment, final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone, "successClaimGift");
        log.add("giftAmount", giftAmount);
        log.add("aName", aName);
        log.add("giftType", giftType);
        log.add("code", code);

        giftManager.createGift(fromAgent, pin, giftAmount, giftType, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error");
                long tranId = result.getLong("tranId", 0);

                log.add("error", error);
                log.add("tranId", tranId);
                log.writeLog();

                final Gift gift = new Gift(result.getObject("gift"));
                final GiftType giftType = new GiftType(result.getObject("giftType"));
                if (error == 0) {
                    gift.getExtra().putString("creator", "sys");
                    giftDb.update(gift, false, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {
                            giftManager.adjustGift(fromAgent, "0" + msg.cmdPhone, gift.getModelId(), null, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject result) {
                                    int error = result.getInteger("error");
                                    long tranId = result.getLong("tranId", 0);

                                    ClaimHistory history = new ClaimHistory(fromAgent, msg.cmdPhone, error, giftAmount, tranId, gift.getModelId(), code);
                                    history.code = "adjustGift";
                                    claimHistoryDb.save(history, null);

                                    if (error == 0) {

                                        final TranObj mainObj = new TranObj();
                                        long currentTime = System.currentTimeMillis();
                                        //TODO: Build mainObj
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.GIFT_CLAIM_VALUE;
                                        mainObj.comment = DataUtil.stringFormat(comment)
                                                .put("giftName", giftType.name)
                                                .toString();
                                        mainObj.tranId = tranId;
                                        mainObj.clientTime = currentTime;
                                        mainObj.ackTime = currentTime;
                                        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
                                        mainObj.partnerName = aName;
                                        mainObj.partnerId = aPhone;
                                        mainObj.partnerRef = aAvatar;
                                        mainObj.amount = giftAmount;
                                        mainObj.status = TranObj.STATUS_OK;
                                        mainObj.error = 0;
                                        mainObj.cmdId = msg.cmdIndex;
                                        mainObj.parterCode = gift.typeId;
                                        mainObj.billId = gift.getModelId();

                                        //                mainObj.billId = gift.getModelId();
                                        mainObj.io = 1;
                                        //                mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                        //                mainObj.owner_number = receiver;

//                                tranDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
//                                    @Override
//                                    public void handle(Boolean result) {
//                                        if (!result) {
//                                            BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
//                                        }
//                                    }
//                                });
                                        Misc.sendTranAsSyn(msg, sock, tranDb, mainObj, common);
                                    }

                                    callback.handle(result);
                                }
                            });
                        }
                    });
                    return;
                }
                ClaimHistory history = new ClaimHistory(fromAgent, msg.cmdPhone, error, giftAmount, tranId, gift.getModelId(), code);
                history.code = "createGift";
                claimHistoryDb.save(history, null);
                callback.handle(result);
            }
        });
    }

    public void successClaimPoint(final NetSocket sock, final MomoMessage msg, final SockData data,
                                  final String fromAgent, final String aName, final String aPhone, final String aAvatar,
                                  final long pointAmount, final String comment, final String code, final Handler<JsonObject> callback) {
        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone, "successClaimPoint");
        log.add("fromAgent", fromAgent);
        log.add("pointAmount", pointAmount);
        log.add("code", code);

        Misc.adjustment(vertx, fromAgent, aPhone, pointAmount, Core.WalletType.POINT_VALUE, null, new Common.BuildLog(logger), new Handler<Common.SoapObjReply>() {
            @Override
            public void handle(Common.SoapObjReply coreReply) {
                ClaimHistory history = new ClaimHistory(fromAgent, msg.cmdPhone, coreReply.error, pointAmount, coreReply.tranId, null, code);
                history.comment = "transferPoint";
                claimHistoryDb.save(history, null);

                log.add("error", coreReply.error);
                log.add("tranId", coreReply.tranId);
                log.writeLog();

                if (coreReply.error != 0) {
                    callback.handle(new JsonObject()
                            .putNumber("error", coreReply.error));
                    return;
                }

                final TranObj mainObj = new TranObj();
                long currentTime = System.currentTimeMillis();
                //TODO: Build mainObj
                mainObj.tranType = MomoProto.TranHisV1.TranType.MPOINT_CLAIM_VALUE;
                mainObj.comment = comment;
                mainObj.tranId = coreReply.tranId;
                mainObj.clientTime = currentTime;
                mainObj.ackTime = currentTime;
                mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
                mainObj.amount = pointAmount;
                mainObj.status = TranObj.STATUS_OK;
                mainObj.error = 0;
                mainObj.cmdId = msg.cmdIndex;
                mainObj.partnerName = aName;
                mainObj.partnerId = code;
                mainObj.partnerRef = aAvatar;
//                mainObj.billId = gift.getModelId();
                mainObj.io = 1;
//                mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                mainObj.owner_number = DataUtil.strToInt(aPhone);
                MomoMessage momoMessage = msg;
                momoMessage.cmdPhone = DataUtil.strToInt(aPhone);
                Misc.sendTranAsSyn(momoMessage, sock, tranDb, mainObj, common);
//                tranDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
//                    @Override
//                    public void handle(Boolean result) {
//                        if (!result) {
//                            BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
//                        }
//                    }
//                });

                Notification notification = new Notification();
                notification.priority = 2;
                notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                notification.caption = PromoContentNotification.GIFT_CLAIM_RECEIVE_TITLE;// "Nhận thưởng quà khuyến mãi";
                notification.body = String.format(PromoContentNotification.GIFT_CLAIM_RECEIVE_BODY, pointAmount);//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                notification.tranId = coreReply.tranId;
                notification.time = new Date().getTime();
                notification.receiverNumber = DataUtil.strToInt(aPhone);
                Misc.sendNoti(vertx, notification);

                if(sock != null)
                logger.info("send current agent info for " + msg.cmdPhone);
                common.sendCurrentAgentInfo(vertx, sock, 0, msg.cmdPhone, data);

                callback.handle(new JsonObject()
                        .putNumber("error", coreReply.error));
            }
        });


    }

    public void getGiftType(final NetSocket sock, final MomoMessage msg, SockData data, Handler<JsonObject> callback) {
        MomoProto.GetGiftType request;
        try {
            request = MomoProto.GetGiftType.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }

        String giftTypeId = request.getGiftTypeId();
        if (giftTypeId == null || giftTypeId.isEmpty()) {
            giftManager.getGiftType(request.getTime(), new Handler<List<GiftType>>() {
                @Override
                public void handle(final List<GiftType> types) {
                    //Lay 5 gift type tra ve app
                    final AtomicInteger atomicInteger = new AtomicInteger(types.size());

                    vertx.setPeriodic(1000L, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            //neu so luong het thi tra ve cho app luon
                            final MomoProto.GetGiftTypeReply.Builder builder = MomoProto.GetGiftTypeReply.newBuilder();
                            if (atomicInteger.intValue() < 1) {
                                vertx.cancelTimer(aLong);
                                return;
                            }
                            //Neu co gift type thi tra ve app 5 qua 1 lan
                            else{
                               int number = Math.min(atomicInteger.intValue(), 5);
                               int position = 0;
                               for(int i = 0; i < number; i++)
                               {
                                   position = atomicInteger.decrementAndGet();
                                   builder.addGiftType(types.get(position).toMomoProto());
                               }
                                Buffer buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.GET_GIFT_TYPE_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        builder.build().toByteArray()
                                );
                                common.writeDataToSocket(sock, buf);

                            }

                        }
                    });
//                    for (GiftType giftType : types) {
//                        builder.addGiftType(giftType.toMomoProto());
//                    }
//                    Buffer buf = MomoMessage.buildBuffer(
//                            MomoProto.MsgType.GET_GIFT_TYPE_REPLY_VALUE,
//                            msg.cmdIndex,
//                            msg.cmdPhone,
//                            builder.build().toByteArray()
//                    );
//                    common.writeDataToSocket(sock, buf);
                }
            });
            return;
        }
        giftManager.getGetGiftType(giftTypeId, new Handler<GiftType>() {
            @Override
            public void handle(GiftType giftType) {
                MomoProto.GetGiftTypeReply.Builder builder = MomoProto.GetGiftTypeReply.newBuilder();
                if (giftType != null) {
                    builder.addGiftType(giftType.toMomoProto());
                }
                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.GET_GIFT_TYPE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build().toByteArray()
                );
                common.writeDataToSocket(sock, buf);
            }
        });
    }

    public void buyGift(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> webcallback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone, "buyGift");

        final String mTarget = request.getPartnerId(); // Nguoi nhan
        final String mGiftTypeId = request.getBillId();   //giftTypeId
//        int mSource = request.getSourceFrom();      // nguon tien
        final long amount = request.getAmount();        // Số tiền gift sẽ mua
        final long ackTime = System.currentTimeMillis();

        log.add("target[patherId]", mTarget);
        log.add("giftTypeId[billId]", mGiftTypeId);
        log.add("amount[Amount]", amount);
        log.writeLog();


        final int target = DataUtil.toInteger(mTarget, 0);

        giftManager.createGift("0" + msg.cmdPhone, data.pin, amount, mGiftTypeId, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error", Integer.MIN_VALUE);
                int tranId = result.getInteger("tranId", Integer.MIN_VALUE);
                int io = -1;

                log.add("error", error);
                log.add("tranId", tranId);
                log.writeLog();

                JsonObject reply = Misc.getJsonObjRpl(error, tranId, amount, io);

                //TODO: add giftId into response tranhisV1 kvp
                JsonObject giftJson = result.getObject("gift");
                if (error == 0 && giftJson != null) {
                    Gift gift = new Gift(giftJson);
                    JsonObject kvp = new JsonObject()
                            .putString("giftId", gift.getModelId());
                    reply.putObject("kvp", kvp);
                }

                if (error < 0) {
                    Misc.addErrDescAndComment(reply, GiftError.getDesc(error), null);
                }
                common.sendTransReply(vertx, reply, ackTime, msg, sock, data, webcallback);
                if (error == 0) {
                    common.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);

                }
            }
        });
    }

    public ArrayList<MomoProto.GetGiftReply.Builder> toSynBlocks(List<Gift> giftList, int size) {
        ArrayList<MomoProto.GetGiftReply.Builder> messages = new ArrayList<MomoProto.GetGiftReply.Builder>();
        MomoProto.GetGiftReply.Builder builder = MomoProto.GetGiftReply.newBuilder();
        for (int i = 0; i < giftList.size(); i++) {
            builder.addGift(giftList.get(i).toMomoProto());
            if (builder.getGiftCount() == size) {
                messages.add(builder);
                builder = MomoProto.GetGiftReply.newBuilder();
            }
        }
        if (builder.getGiftCount() > 0) {
            messages.add(builder);
        }
        return messages;
    }

    public void getGift(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> webcallback) {

        MomoProto.GetGift request;
        try {
            request = MomoProto.GetGift.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("giftId", (request.getGiftId() == null ? "get all gift" : "get by giftId"));
        log.add("giftId is ", (request.getGiftId() == null ? "get all gift" : "get by giftId " + request.getGiftId()));
        final String giftId = request.getGiftId();
        if (giftId != null && !giftId.isEmpty()) {
            giftManager.getAgentGift("0" + msg.cmdPhone, giftId, new Handler<Gift>() {
                @Override
                public void handle(Gift gift) {
                    if (gift == null || !gift.owner.equals("0" + msg.cmdPhone)) {
                        //TODO: reply empty
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.GET_GIFT_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.GetGiftReply.newBuilder()
                                        .setIndex(0)
                                        .build().toByteArray()
                        );
                        common.writeDataToSocket(sock, buf);
                        log.writeLog();
                        return;
                    }
                    log.add("gift info", gift.toJsonObject());
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.GET_GIFT_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.GetGiftReply.newBuilder()
                                    .addGift(gift.toMomoProto())
                                    .setIndex(0)
                                    .build().toByteArray()
                    );
                    common.writeDataToSocket(sock, buf);
                    log.writeLog();
                }
            });
            return;
        }

        //TODO: return all gifts
        giftDb.getStableGift("0" + msg.cmdPhone, new Handler<List<Gift>>() {
            @Override
            public void handle(List<Gift> giftList) {
                final ArrayList<MomoProto.GetGiftReply.Builder> messages = toSynBlocks(giftList, SYN_GIFT_SIZE);

                final JsonObject joIndex = new JsonObject();

                final int maxSize = messages.size();

                if (maxSize == 0) {
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.GET_GIFT_FINISH_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            null
                    );
                    common.writeDataToSocket(sock, buf);
                    log.writeLog();
                    return;
                }

                joIndex.putNumber("pos", 0);

                vertx.setPeriodic(500, new Handler<Long>() {
                    @Override
                    public void handle(Long timerId) {
                        int i = joIndex.getInteger("pos");
                        if (i < maxSize) {
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.GET_GIFT_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    messages.get(i).setIndex(i)
                                            .build().toByteArray()
                            );
                            common.writeDataToSocket(sock, buf);
                            log.writeLog();
                            i++;
                            joIndex.putNumber("pos", i);
                            logger.info("current index gift: " + i);
                        } else {
                            logger.info("last index gift: " + joIndex.getInteger("pos"));
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.GET_GIFT_FINISH_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    "f".getBytes()
                            );
                            common.writeDataToSocket(sock, buf);
                            vertx.cancelTimer(timerId);
                            log.writeLog();
                        }
                    }
                });
            }
        });
    }

    public void notifyReceiver(int sender, String senderName, int receiver, Gift gift, GiftType giftType, String message, long tranId) {
        final TranObj mainObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        //TODO: Build mainObj
        mainObj.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        mainObj.comment = DataUtil.stringFormat("Quý khách đã nhận thẻ quà tặng của dịch vụ ${giftName} trị giá ${giftAmount}đ. \nLời nhắn: ${message}")
                .put("giftName", giftType.name)
                .put("giftAmount", String.format("%,d", gift.amount).replace(",", "."))
                .put("message", message)
                .toString();
        mainObj.tranId = tranId;
        mainObj.clientTime = currentTime;
        mainObj.ackTime = currentTime;
        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.partnerName = senderName;
        mainObj.partnerId = "0" + sender;
        mainObj.amount = gift.amount;
        mainObj.status = TranObj.STATUS_OK;
        mainObj.error = 0;
        mainObj.cmdId = -1;
        mainObj.billId = gift.getModelId();
        mainObj.io = 1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.owner_number = receiver;
        mainObj.partnerRef = message;

        //TODO: Build noti
        final Notification notification = new Notification();
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
        notification.caption = "Nhận được quà";
        notification.body = DataUtil.stringFormat("Bạn vừa nhận được một gói quà từ ${sender}(${senderName}). Nội dung: ${message}")
                .put("sender", "0" + sender)
                .put("senderName", senderName)
                .put("message", message)
                .toString();

        notification.tranId = tranId;
        notification.sender = sender;

        notification.time = new Date().getTime();
        notification.extra = new JsonObject()
                .putString("giftId", gift.getModelId())
                .putString("giftTypeId", gift.typeId)
                .putString("msg", message)
                .putString("sender", "0" + sender)
                .putString("senderName", senderName)
                .putString("amount", String.valueOf(gift.amount))
                .toString();
        notification.receiverNumber = receiver;

        tranDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {

                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                }
            }
        });

        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                , notification.toFullJsonObject());
    }

    public void sendGift(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> webcallback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone, "sendGift");

        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }

        int mTarget;
        try {
            mTarget = Integer.parseInt(request.getPartnerId()); // Nguoi nhan
        } catch (Exception e) {
            mTarget = 0;
        }
        final String mMessage = request.getPartnerCode();// message
        final String giftId = request.getBillId();       //giftTypeId

        //get name of sender
        final String agentName = (request.getPartnerExtra1() == null || "".equalsIgnoreCase(request.getPartnerExtra1())) ?
                (
                        (data != null && data.getPhoneObj() != null) ? data.getPhoneObj().name : ""
                ) : request.getPartnerExtra1();

        final int target = mTarget;

        final long actionTime = DataUtil.stringToUNumber(request.getPartnerRef());  // Time that the gift will be sent.

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 10);
        final long currentTime = cal.getTimeInMillis();
        log.add("actionTime", actionTime);
        log.add("currentTime", currentTime);
        log.add("giftId", giftId);
        log.add("agentName", agentName);
        phonesDb.getPhoneObjInfo(target, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj phoneObj) {
                if(phoneObj != null && phoneObj.isAgent)
                {
                    //Khong tra qua
                    String desc = "Quà tặng đã được gửi tới tài khoản MoMo của người nhận.";
                    log.add("desc", "So nay la diem giao dich nen khong duoc phep tang qua");
                    log.writeLog();
                    MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder()
                            .setTranId(System.currentTimeMillis())
                            .setStatus(5)
                            .setError(-1)
                            .setDesc(desc);
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.TRANS_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.build().toByteArray());
                    common.writeDataToSocket(sock, buf);
                    return;
                }
                else
                {
                    if (actionTime < currentTime) {
                        log.add("sendGift", "now");
                        log.writeLog();
                        sendGiftNow(giftId, target, mMessage, sock, msg, data, webcallback);
                    } else {

                        log.add("sendGift", Misc.dateFormatWithParten(actionTime, "HH:mm:ss dd/MM/yyyy"));
                        log.writeLog();
                        String pin = (data == null ? "" : data.pin);

                        if ("".equalsIgnoreCase(pin)) {
                            phonesDb.getPhoneObjInfoLocal(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(PhonesDb.Obj obj) {
                                    if (obj == null) {
                                        log.add("getPhoneObjInfoLocal", "Khong tim thay phone tren bang phone");
                                        log.add("???", "why not get the phone Object");
                                    }
                                    setGiftTimed(actionTime
                                            , giftId
                                            , target
                                            , agentName
                                            , mMessage
                                            , sock
                                            , msg
                                            , obj.pin
                                            , webcallback);
                                }
                            });
                        } else {
                            setGiftTimed(actionTime
                                    , giftId
                                    , target
                                    , agentName
                                    , mMessage
                                    , sock
                                    , msg
                                    , pin
                                    , webcallback);
                        }
                    }
                }
            }
        });
    }

    private void setGiftTimed(final long time
            , final String giftId
            , final int target
            , final String agentName
            , final String mMessage
            , final NetSocket sock
            , final MomoMessage msg
            , final String pin, final Handler<JsonObject> webcallback) {

        giftManager.setGiftTimed("0" + msg.cmdPhone
                , agentName
                , pin
                , "0" + target
                , giftId
                , mMessage
                , time, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error", -1000);
                JsonObject giftJson = result.getObject("gift");
                JsonObject timedJson = result.getObject("timedGift", null);

                //get timedGift Object from result
                TimedGift timedGift = null;
                if (timedJson != null) {
                    timedGift = new TimedGift(timedJson);
                }

                Gift gift = null;
                if (giftJson != null)
                    gift = new Gift(giftJson);
                final TranObj mainObj = new TranObj();
                long currentTime = System.currentTimeMillis();

                mainObj.tranType = MomoProto.TranHisV1.TranType.SEND_GIFT_VALUE;
                mainObj.comment = "Tặng quà thành công!";
                mainObj.tranId = (timedGift == null ? 0 : timedGift.preTranId);
                mainObj.clientTime = currentTime;
                mainObj.ackTime = currentTime;
                mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
                mainObj.amount = 0;
                if (gift != null) {
                    mainObj.amount = gift.amount;
                }
                mainObj.status = TranObj.STATUS_PROCESS;
                mainObj.error = error;
                mainObj.cmdId = -1;
                mainObj.billId = gift.getModelId();
                mainObj.io = -1;
                mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                mainObj.desc = DataUtil.stringFormat("Món quà của bạn sẽ được gửi đi vào lúc ${sendTime}")
                        .put("sendTime", Misc.dateFormatWithParten(time, "HH:mm dd/MM/yyyy"))
                        .toString();

                mainObj.kvp = new JsonObject()
                        .putString("msg", mainObj.desc);

                common.sendTransReplyByTran(msg, mainObj, tranDb, sock);
            }
        });
    }

    private void sendGiftNow(final String giftId, final int target, final String mMessage, final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> webcallback) {

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone, "sendGift");
        log.add("giftId", giftId);
        log.add("target", target);
        log.writeLog();
        final long ackTime = System.currentTimeMillis();

        giftManager.transferGifDirectOrG2n("0" + msg.cmdPhone
                , data.getPhoneObj().name
                , data.pin
                , giftId
                , target
                , mMessage, null, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                String tranType = result.getString("tranType");
                int error = result.getInteger("error", Integer.MIN_VALUE);
                int tranId = result.getInteger("tranId", Integer.MIN_VALUE);
                int io = -1;
                long amount;

                log.add("error", error);
                log.add("tranId", tranId);
                log.writeLog();

                Gift gift = new Gift(result.getObject("gift"));
                GiftType giftType = new GiftType(result.getObject("giftType"));

                amount = gift.amount;

                JsonObject reply = Misc.getJsonObjRpl(error, tranId, amount, io);
                String desc = "Quà tặng đã được gửi tới tài khoản MoMo của người nhận";
                reply.putString("desc", desc);

                //Transfer directly
                if (GiftManager.TRANTYPE_TRANSFER.equals(tranType)) {
                    if (error == 0) {
                        notifyReceiver(msg.cmdPhone, data.getPhoneObj().name, target, gift, giftType, mMessage, tranId);
                        promotionProcess.updateOctoberPromoVoucherStatus(log, new JsonArray().add(giftId), "0" + msg.cmdPhone);
                    }
                } else if (GiftManager.TRANTYPE_G2N.equals(tranType)) {
                    reply = Misc.getJsonObjRplProcessing(error, tranId, amount, io);
                    GiftToNumber g2n = new GiftToNumber(result.getObject("giftToNumber"));
                    JsonObject kvp = new JsonObject()
                            .putString("msg", DataUtil.stringFormat(G2N_MESSAGE).put("token", g2n.link).toString())
                            .putString("link", DataUtil.stringFormat(LINK).put("token", g2n.link).toString());

                    reply.putObject("kvp", kvp);

                }
                common.sendTransReply(vertx, reply, ackTime, msg, sock, data, webcallback);
            }
        });
    }

    public void setGiftStatus(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> webcallback) {
        MomoProto.SetGiftStatus request;
        try {
            request = MomoProto.SetGiftStatus.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }
        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }

        String giftId = request.getGiftId();
        giftManager.getGift(giftId, new Handler<Gift>() {
            @Override
            public void handle(final Gift gift) {
                if (gift != null) {
                    if (gift.owner.equals("0" + msg.cmdPhone)) {
                        if (gift.status != Gift.STATUS_NEW)
                            return;
                        gift.status = Gift.STATUS_VIEWED;
                        giftDb.update(gift, false, null);

//                        // Notify sender
                        final GiftHistory filter = new GiftHistory();
                        filter.giftId = gift.getModelId();
                        filter.to = "0" + msg.cmdPhone;
                        //find sender
                        giftHistoryDb.findOne(filter, new Handler<GiftHistory>() {
                            @Override
                            public void handle(GiftHistory history) {
                                if (history == null)
                                    return;
                                notifySender(data, history, gift);
                            }
                        });
                    }
                }
            }
        });
    }

    private void notifySender(final SockData data, GiftHistory history, Gift gift) {
        int phone = DataUtil.strToInt(history.from);
        if (phone == 0)
            return;
        //TODO: Build noti
        String senderName = "";
        if (data.getPhoneObj() != null)
            senderName = data.getPhoneObj().name;

        final Notification notification = new Notification();
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        notification.caption = "Xem quà";
        notification.body = DataUtil.stringFormat("${name}(${phone}) đã xem quà tặng của bạn vào lúc ${time}")
                .put("name", senderName)
                .put("phone", "0" + data.getNumber())
                .put("time", Misc.dateFormatWithParten(System.currentTimeMillis(), "HH:mm - dd/MM/yyyy"))
                .toString();
//        notification.sms = "";
        notification.tranId = history.tranId;
//        notification.sender = msg.cmdPhone;

        notification.time = new Date().getTime();
        notification.receiverNumber = phone;


        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                , notification.toFullJsonObject(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

            }
        });
    }

    public void giftToPoint(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> webcallback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }

        final long ackTime = System.currentTimeMillis();
        final String giftId = request.getPartnerCode();

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone, "giftToPoint");
        log.add("giftId", giftId);

        //todo why not to exchange gift to point ?????????
        //todo who request this rule. det
        JsonObject reply = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.NOT_TRANSFERABLE_VALUE, 0, 0, 0);
        Misc.addErrDescAndComment(reply, "Qùa tặng không được quy đổi thành tiền trong tài khoản khuyến mãi", null);
        common.sendTransReply(vertx, reply, ackTime, msg, sock, data, webcallback);

        /*
        giftManager.transferGift("0"+msg.cmdPhone, giftId, EXPIRE_GIFT_MOMO, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error", -1000);
                long tranId = result.getLong("tranId", 0);
                Gift gift = new Gift(result.getObject("gift"));
                GiftType giftType = new GiftType(result.getObject("giftType"));

                JsonObject reply = Misc.getJsonObjRpl(error, tranId, amount, io);
                common.sendTransReply(vertx, reply, ackTime, msg, sock, data, webcallback);
            }
        });*/

        /*giftManager.giftToPoint("0" + msg.cmdPhone, data.pin, giftId, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error", Integer.MIN_VALUE);
                int tranId = result.getInteger("tranId", Integer.MIN_VALUE);
                int io = +1;
                long amount = 0;

                log.add("error", error);
                log.add("tranId", tranId);
                log.writeLog();

                if (error == 0) {
                    Gift gift = new Gift(result.getObject("gift"));

                    amount = gift.amount;

                }

                JsonObject reply = Misc.getJsonObjRpl(error, tranId, amount, io);
                common.sendTransReply(vertx, reply, ackTime, msg, sock, data, webcallback);
            }
        });*/
    }

    public void sendGiftMessage(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> webcallback) {
        MomoProto.SendMessage request;
        try {
            request = MomoProto.SendMessage.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            request = null;
            logger.error("InvalidProtocolBufferException", e);
        }

        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }
        final MomoProto.SendMessage fRequest = request;

        final String message = request.getMsg();
        final String senderName = request.getSenderName();
        final int toPhone = request.getToPhone();
        final long tranId = request.getTranId();

        //gui loi cam on khi nhan gift hay nhan tien
        final int type = request.getType() == 0 ? MomoProto.SendMessage.Type.GIFT_THANKS_VALUE : request.getType();

        final GiftTran tran = new GiftTran(String.valueOf(tranId));
        giftTranDb.findOne(tran, new Handler<GiftTran>() {
            @Override
            public void handle(GiftTran result) {
                if (result != null && result.thank) {

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.SEND_GIFT_MESSAGE_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.StandardReply.newBuilder()
                                    .setRcode(1)
                                    .setResult(false)
                                    .setDesc("Bạn chỉ được gửi lời cảm ơn một lần duy nhất.")
                                    .build().toByteArray()
                    );
                    common.writeDataToSocket(sock, buf);
                    return;
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.SEND_GIFT_MESSAGE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        MomoProto.StandardReply.newBuilder()
                                .setRcode(0)
                                .setResult(true)
                                .build().toByteArray()
                );
                common.writeDataToSocket(sock, buf);

                tran.thank = true;
                giftTranDb.update(tran, true, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {
                        //TODO: Build noti
                        final Notification notification = new Notification();
                        notification.priority = 2;
                        notification.type = MomoProto.NotificationType.NOTI_GIFT_REPLY_VALUE;
                        notification.caption = "Lời cảm ơn";
                        if (type == MomoProto.SendMessage.Type.GIFT_THANKS_VALUE) {

                            notification.body = DataUtil.stringFormat("${receiverName}(${receiverPhone}) gửi lời cảm ơn cho món quà bạn tặng: \"${message}\"")
                                    .put("receiverName", data.getPhoneObj() == null ? "" : data.getPhoneObj().name)
                                    .put("receiverPhone", "0" + msg.cmdPhone)
                                    .put("message", message)
                                    .toString();
                        } else {
                            notification.body = DataUtil.stringFormat("${receiverName}(${receiverPhone}) gửi lời cảm ơn đến bạn: \"${message}\"")
                                    .put("receiverName", data.getPhoneObj() == null ? "" : data.getPhoneObj().name)
                                    .put("receiverPhone", "0" + msg.cmdPhone)
                                    .put("message", message)
                                    .toString();
                        }
                        notification.extra = new JsonObject().putString("senderName", senderName).putString("msg", message).toString();

                        notification.tranId = tranId;
                        notification.sender = msg.cmdPhone;

                        notification.time = new Date().getTime();
                        notification.receiverNumber = toPhone;

                        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                , notification.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {

                            }
                        });
                    }
                });

                tranDb.getTranById(toPhone, fRequest.getTranId(), new Handler<TranObj>() {
                    @Override
                    public void handle(TranObj tranObj) {
                        if (tranObj == null) {
                            logger.warn("TranId doesn't exists");
                            return;
                        }
                        tranObj.comment += "\n " + senderName + ": " + message;
                        tranDb.upsertTranOutSideNew(toPhone, tranObj.getJSON(), new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean event) {

                            }
                        });
                    }
                });
            }
        });

    }

    public void receiveGift(final int phone) {
        giftManager.commitG2nTransaction("0" + phone, new Handler<List<GiftToNumber>>() {
            @Override
            public void handle(List<GiftToNumber> result) {
                for (GiftToNumber giftToNumber : result) {
                    final GiftToNumber g2n = giftToNumber;

                    if (g2n.tranError != 0) {
                        continue;
                    }

                    final int fromPhone = DataUtil.strToInt(g2n.fromAgent);

                    Gift filter = new Gift(g2n.giftId);
                    giftDb.findOne(filter, new Handler<Gift>() {
                        @Override
                        public void handle(final Gift gift) {
                            GiftType filter = new GiftType(gift.typeId);
                            giftTypeDb.findOne(filter, new Handler<GiftType>() {
                                @Override
                                public void handle(final GiftType giftType) {
                                    notifyReceiver(fromPhone, g2n.senderName, phone, gift, giftType, g2n.comment, g2n.tranId);
                                }
                            });
                        }
                    });


                    tranDb.updateTranStatusNew(fromPhone, g2n.tranId, TranObj.STATUS_OK, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {
                            tranDb.getTranById(fromPhone, g2n.tranId, new Handler<TranObj>() {
                                @Override
                                public void handle(TranObj tran) {
                                    BroadcastHandler.sendOutSideTransSync(vertx, tran);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    public void useGift(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> webcallback) {
        MomoProto.GiftRequest request;
        try {
            request = MomoProto.GiftRequest.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        final String giftId = request.getGiftId();

        if (request == null || giftId == null) {
            common.writeErrorToSocket(sock);
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone, "useGift");
        log.add("giftId", giftId);

        giftManager.getGift(giftId, new Handler<Gift>() {
            @Override
            public void handle(final Gift gift) {

                // kich hoat qua cu VCB promotion
                if (gift != null && ("VCBPROMO_B".equalsIgnoreCase(gift.typeId) || "VCBPROMO_A".equalsIgnoreCase(gift.typeId))) {
                    activeGiftVcbPromo(gift, log, msg, giftId, sock);
                    return;
                }

                // kich hoat qua cu galaxy promotion
                if (gift != null && ("galaxy".equalsIgnoreCase(gift.typeId))) {
                    activeGiftGalaxyPromo(gift, log, msg, giftId, sock);
                    return;
                }

                //other gifts
                giftManager.useGift("0" + msg.cmdPhone, giftId, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        int error = result.getInteger("error", -1000);

                        log.add("giftId", giftId);
                        log.add("error", error);
                        log.writeLog();
                        if (error == GiftError.ACTIVATED_TIME_ERROR) {
                            String desc = result.getString("desc", GiftError.getDesc(error));
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.USE_GIFT_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    MomoProto.StandardReply.newBuilder()
                                            .setRcode(error)
                                            .setResult(error == 0)
                                            .setDesc(desc)
                                            .build().toByteArray()
                            );
                            common.writeDataToSocket(sock, buf);
                        } else {
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.USE_GIFT_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    MomoProto.StandardReply.newBuilder()
                                            .setRcode(error)
                                            .setResult(error == 0)
                                            .setDesc(GiftError.getDesc(error))
                                            .build().toByteArray()
                            );
                            common.writeDataToSocket(sock, buf);
                        }
                    }
                });
            }
        });

    }

    public void canUseGift(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> webcallback) {
        MomoProto.GiftRequest request;
        try {
            request = MomoProto.GiftRequest.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }
        final Common.BuildLog log = new Common.BuildLog(logger);
        if(request == null)
        {
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.CAN_USE_GIFT_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.StandardReply.newBuilder()
                            .setRcode(0)
                            .setResult(0 == 0)
                            .setDesc(GiftError.getDesc(0))
                            .build().toByteArray()
            );
            common.writeDataToSocket(sock, buf);
            return;
        }
        final MomoProto.GiftRequest frequest = request;
        logger.info("giftId " + msg.cmdPhone + " " + request.getGiftId());
        logger.info("serviceId " + msg.cmdPhone + " " + request.getServiceId());
        if(request.getServiceId().equalsIgnoreCase("[\"taxi\"]") || request.getServiceId().equalsIgnoreCase("[\"topup\"]"))
        {
            promotionProcess.executePromotion("", MomoProto.MsgType.CAN_USE_GIFT_VALUE, "", msg.cmdPhone, 0,
                    0, data, request.getServiceId(), null, 0, 1, log, new JsonObject().putString(StringConstUtil.PromotionField.GIFT_ID, request.getGiftId()), new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonResponse) {
                            JsonArray jarrData = jsonResponse.getArray(StringConstUtil.PromotionField.DATA, new JsonArray());
                            log.add("jarrData", jarrData.toString());

                            int error = -1;
                            JsonObject joData = null;
                            JsonObject joExtraData = new JsonObject();
                            for(Object o : jarrData)
                            {
                                joExtraData = ((JsonObject)o);
                                if(frequest.getServiceId().equalsIgnoreCase(joExtraData.getString(StringConstUtil.PromotionField.SERVICE_ID, "")))
                                {
                                    joData = joExtraData;
                                }
                            }
                            Buffer buf;
                            if(joData == null)
                            {
                                buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.CAN_USE_GIFT_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        MomoProto.StandardReply.newBuilder()
                                                .setRcode(0)
                                                .setResult(0 == 0)
                                                .setDesc(GiftError.getDesc(0))
                                                .build().toByteArray()
                                );
                            }
                            else {
                                int err = joData.getInteger(StringConstUtil.PromotionField.ERROR, 0);
                                String desc = joData.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                                buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.CAN_USE_GIFT_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        MomoProto.StandardReply.newBuilder()
                                                .setRcode(err)
                                                .setResult(err == 0)
                                                .setDesc(desc)
                                                .build().toByteArray()
                                );
                            }
                            log.writeLog();
                            common.writeDataToSocket(sock, buf);
                        }
                    }
            );
        }
        else
        {
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.CAN_USE_GIFT_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.StandardReply.newBuilder()
                            .setRcode(0)
                            .setResult(0 == 0)
                            .setDesc(GiftError.getDesc(0))
                            .build().toByteArray()
            );
            log.writeLog();
            common.writeDataToSocket(sock, buf);
        }

    }

    public void setTranWithGift(final TransferWithGiftContext context, final Handler<JsonObject> callback) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + context.phone);
        log.add("func", "setTranWithGift");
        log.add("serviceId", context.serviceId);
        log.add("tranId", context.tranId);

        if (context.queuedGiftResult == null || context.queuedGiftResult.gifts == null || context.queuedGiftResult.gifts.isEmpty()) {
            callback.handle(new JsonObject().putNumber("error", 0));
            log.add("gift count", 0);
            log.writeLog();
            return;
        }

        final int max = (context.queuedGiftResult.queuedGifts == null ? 0 : context.queuedGiftResult.queuedGifts.size());

        //not has any gift in queue
        if (max == 0) {
            callback.handle(new JsonObject().putNumber("error", 0));
            log.add("gift count", 0);
            log.writeLog();
            return;
        }
        final JsonArray arrayGiftId = new JsonArray();
        final JsonObject joPos = new JsonObject();
        joPos.putNumber("p", 0);

        vertx.setPeriodic(400, new Handler<Long>() {
            @Override
            public void handle(Long timerId) {

                int pos = joPos.getInteger("p");

                if (pos >= max) {
                    //return result to client

                    callback.handle(new JsonObject().putNumber("error", 0).putArray(StringConstUtil.IronManPromo.GIFT_ID_ARRAY, arrayGiftId));

                    log.writeLog();

                    //end timer
                    vertx.cancelTimer(timerId);
                    return;
                }

                //begin process for each gift
                final QueuedGift queuedGift = context.queuedGiftResult.queuedGifts.get(pos);
                Gift gift = context.queuedGiftResult.gifts.get(queuedGift.giftId);

                log.add("gift item", "--" + (pos + 1) + "--");

                if (gift == null) {
                    log.add("gift", "null");
                    log.add("????", "need to check why gift is null from queued gift id");
                } else {
                    giftManager.changeGiftOwner(gift.getModelId()
                            , "0" + context.phone
                            , context.serviceId
                            , context.tranId, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject result) {
                            queuedGiftDb.remove(queuedGift, null);
                        }
                    });

                }

                pos++;
                log.add("giftId", gift.getModelId() == null ? "null" : gift.getModelId());
                arrayGiftId.add(queuedGift.giftId);
                joPos.putNumber("p", pos);
            }
        });
    }

    public void getTranConfirmInfo(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }
        if (request == null) {
            common.writeErrorToSocket(sock);
            return;
        }
        final String serviceId = request.getBillId();
        final Long amount = request.getAmount();

        if ("123phim".equalsIgnoreCase(serviceId) && data != null && "bhd".equalsIgnoreCase(data.Cinema)) {

            final Promo.PromoReqObj reqGlxRec = new Promo.PromoReqObj();
            reqGlxRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            reqGlxRec.PROMO_NAME = "galaxy";

            Misc.requestPromoRecord(vertx, reqGlxRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {


                    final PromotionDb.Obj glxRec = new PromotionDb.Obj(jsonObject);
                    long curTime = System.currentTimeMillis();

                    boolean runNormal = false;
                    if (glxRec == null || glxRec.DATE_FROM == 0 || glxRec.DATE_TO == 0) {
                        runNormal = true;
                    } else {
                        if ((curTime < glxRec.DATE_FROM) || (curTime > glxRec.DATE_TO)) {
                            runNormal = true;
                        }
                    }
                    //tinh gift, point theo cach binh thuong
                    if (runNormal) {
                        TransferWithGiftContext.build(msg.cmdPhone, serviceId, "", amount, vertx, giftManager, data,
                                TRANWITHPOINT_MIN_POINT, TRANWITHPOINT_MIN_AMOUNT, logger,
                                new Handler<TransferWithGiftContext>() {
                                    @Override
                                    public void handle(TransferWithGiftContext context) {
                                        context.writeLog(logger);
                                        MomoProto.GetTranConfirmReply.Builder builder = MomoProto.GetTranConfirmReply.newBuilder();
                                        builder.setGift(context.voucher);
                                        builder.setPoint(context.point);
                                        if (context.momo > 0)
                                            builder.setMomo(context.momo);
                                        else

                                            builder.setMomo(0);

                                        Buffer buf = MomoMessage.buildBuffer(
                                                MomoProto.MsgType.GET_TRAN_CONFIRM_INFO_REPLY_VALUE,
                                                msg.cmdIndex,
                                                msg.cmdPhone,
                                                builder.build().toByteArray()
                                        );
                                        common.writeDataToSocket(sock, buf);
                                    }
                                }
                        );
                        return;
                    } else {

                        //tin gift point ngoai tru galaxy
                        ArrayList<String> excludeVals = new ArrayList<String>();
                        excludeVals.add("glx");
                        String excludeKey = "cinema";
                        TransferWithGiftContext.buildWithExclude(msg.cmdPhone, serviceId, "", amount, vertx, giftManager, data,
                                TRANWITHPOINT_MIN_POINT
                                , TRANWITHPOINT_MIN_AMOUNT
                                , excludeVals
                                , excludeKey
                                , logger,
                                new Handler<TransferWithGiftContext>() {
                                    @Override
                                    public void handle(TransferWithGiftContext context) {
                                        context.writeLog(logger);
                                        MomoProto.GetTranConfirmReply.Builder builder = MomoProto.GetTranConfirmReply.newBuilder();
                                        builder.setGift(context.voucher);
                                        builder.setPoint(context.point);
                                        if (context.momo > 0)
                                            builder.setMomo(context.momo);
                                        else

                                            builder.setMomo(0);

                                        Buffer buf = MomoMessage.buildBuffer(
                                                MomoProto.MsgType.GET_TRAN_CONFIRM_INFO_REPLY_VALUE,
                                                msg.cmdIndex,
                                                msg.cmdPhone,
                                                builder.build().toByteArray()
                                        );
                                        common.writeDataToSocket(sock, buf);
                                    }
                                }
                        );
                    }
                }
            });

        } else {

            TransferWithGiftContext.build(msg.cmdPhone, serviceId, "", amount, vertx, giftManager, data,
                    TRANWITHPOINT_MIN_POINT, TRANWITHPOINT_MIN_AMOUNT, logger,
                    new Handler<TransferWithGiftContext>() {
                        @Override
                        public void handle(TransferWithGiftContext context) {
                            context.writeLog(logger);
                            MomoProto.GetTranConfirmReply.Builder builder = MomoProto.GetTranConfirmReply.newBuilder();
                            builder.setGift(context.voucher);
                            builder.setPoint(context.point);
                            if (context.momo > 0)
                                builder.setMomo(context.momo);
                            else
                                builder.setMomo(0);
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.GET_TRAN_CONFIRM_INFO_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    builder.build().toByteArray()
                            );
                            common.writeDataToSocket(sock, buf);
                        }
                    }
            );
            return;
        }
    }

    //gift promotion area.start

    private void activeGiftVcbPromo(final Gift gift
            , final Common.BuildLog log
            , final MomoMessage msg
            , final String giftId, final NetSocket sock) {
        final Promo.PromoReqObj vcbReqPromoRec = new Promo.PromoReqObj();
        vcbReqPromoRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
        vcbReqPromoRec.PROMO_NAME = "vcbpromo";
        log.add("promo name", vcbReqPromoRec.PROMO_NAME);

        Misc.requestPromoRecord(vertx, vcbReqPromoRec, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                final PromotionDb.Obj promoRec = new PromotionDb.Obj(jsonObject);
                long curTime = System.currentTimeMillis();

                if (promoRec == null || promoRec.DATE_FROM > curTime || curTime > promoRec.DATE_TO) {
                    log.add("***", "run active gift normally");
                    giftManager.useGift("0" + msg.cmdPhone, giftId, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject result) {
                            int error = result.getInteger("error", -1000);

                            log.add("giftId", giftId);
                            log.add("error", error);
                            log.writeLog();

                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.USE_GIFT_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    MomoProto.StandardReply.newBuilder()
                                            .setRcode(error)
                                            .setResult(error == 0)
                                            .setDesc(GiftError.getDesc(error))
                                            .build().toByteArray()
                            );
                            common.writeDataToSocket(sock, buf);
                        }
                    });

                    return;
                }

                //in vcb promotion
                if (curTime > gift.endDate.getTime()) {

                    log.add("end date", Misc.dateVNFormatWithTime(gift.endDate.getTime()));
                    log.add("current date", Misc.dateVNFormatWithTime(curTime));
                    log.add("****", "over time to active this gift");
                    log.writeLog();

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.USE_GIFT_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.StandardReply.newBuilder()
                                    .setRcode(GiftError.OVERTIME_TO_USE)
                                    .setResult(false)
                                    .setDesc(String.format(GiftError.getDesc(GiftError.OVERTIME_TO_USE)
                                            , String.valueOf(promoRec.DURATION)))
                                    .build().toByteArray()
                    );
                    common.writeDataToSocket(sock, buf);
                    return;
                }

                giftManager.useGift("0" + msg.cmdPhone, giftId, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        int error = result.getInteger("error", -1000);

                        log.add("giftId", giftId);
                        log.add("error", error);

                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.USE_GIFT_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.StandardReply.newBuilder()
                                        .setRcode(error)
                                        .setResult(error == 0)
                                        .setDesc(GiftError.getDesc(error))
                                        .build().toByteArray()
                        );
                        common.writeDataToSocket(sock, buf);

                        //reset time --> no limit
                        if (error == 0) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.add(Calendar.DAY_OF_MONTH, 365 * 10); //thoi han su dung 10 years
                            gift.endDate = calendar.getTime();
                            gift.status = Gift.STATUS_USED;

                            log.add("***", "update endDate of gift to no limit");
                            giftManager.updateGift(gift, false, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                    log.add("result", aBoolean);
                                    log.writeLog();
                                }
                            });
                            return;
                        }

                        log.writeLog();
                    }
                });
            }
        });
    }

    private void activeGiftGalaxyPromo(final Gift gift
            , final Common.BuildLog log
            , final MomoMessage msg
            , final String giftId, final NetSocket sock) {

        log.add("func", "activeGiftGalaxyPromo");

        final MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();
        long curTime = System.currentTimeMillis();

        long basedTime = gift.startDate.getTime() + 2 * 60 * 60 * 1000L;

        //chua den gio active
        if (basedTime > curTime) {
            log.add("desc", "vui long kich hoat gift sau 2h ke tu khi nhan duoc qua");
            builder.setRcode(GiftError.GIFT_NOT_QUEUED)
                    .setResult(false)
                    .setDesc("Bạn vui lòng kích hoạt thẻ quà tặng sau 2h kể từ khi nhận!");

            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.USE_GIFT_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build().toByteArray()
            );
            common.writeDataToSocket(sock, buf);
            log.writeLog();

        } else {

            giftManager.useGift("0" + msg.cmdPhone, giftId, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject result) {
                    int error = result.getInteger("error", -1000);

                    log.add("giftId", giftId);
                    log.add("error", error);

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.USE_GIFT_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.StandardReply.newBuilder()
                                    .setRcode(error)
                                    .setResult(error == 0)
                                    .setDesc(GiftError.getDesc(error))
                                    .build().toByteArray()
                    );
                    common.writeDataToSocket(sock, buf);
                    log.writeLog();
                }
            });
        }
    }
    //gift promotion area.end

}
