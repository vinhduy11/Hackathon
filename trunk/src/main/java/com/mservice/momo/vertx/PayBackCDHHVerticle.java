package com.mservice.momo.vertx;

import com.mservice.momo.data.CDHHPayBack;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;

/**
 * Created by User on 3/29/14.
 */
public class PayBackCDHHVerticle extends Verticle {

    CDHHPayBack cdhhPayBack = null;
    private Logger logger;
    private TransDb transDb;
    //todo lam lai cai nay

    /*"pay_back_account":"0974540385", // new
    "pay_back_max":3, // new
    "delay_pay_back_time" :1, //phut*/
    private JsonObject glbCfg;

    public void start() {
        glbCfg = getContainer().config();
        logger = getContainer().logger();
        EventBus eb = vertx.eventBus();
        cdhhPayBack = new CDHHPayBack(vertx.eventBus(), logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());

        Handler<Message> myHandler = new Handler<Message>() {
            public void handle(Message message) {
                JsonObject requestJO = (JsonObject) message.body();
                final String phoneNumber = "0" + DataUtil.strToInt(requestJO.getString("number"));
                final int voteAmount = requestJO.getInteger("amount");
                final long tranId = requestJO.getLong("tranid");
                final String paybackaccount = requestJO.getString("pbacc");
                final int delaytime = requestJO.getInteger("dtime");
                final int paybackmax = requestJO.getInteger("pbmax");
                final String serviceId = requestJO.getString("sid");
                final JsonObject localConfig = requestJO.getObject("glbconfig", glbCfg);

                /*jsonPayBackReq.putString("pbacc", paybackaccount);
                jsonPayBackReq.putNumber("dtime",delaytime);
                jsonPayBackReq.putNumber("pbmax",paybackmax);*/
                //vertx.setTimer(delay_pay_back_time*60*1000, new Handler<Long>() {

                beginDoPayBack(phoneNumber
                        , voteAmount
                        , tranId
                        , paybackmax
                        , paybackaccount
                        , delaytime
                        , serviceId
                        , localConfig
                        , message);
            }
        };

        eb.registerLocalHandler(AppConstant.PayBackCDHHVerticle_ADDRESS, myHandler);

    }

    private int getMinValue(String serviceId) {
        return glbCfg.getObject(serviceId).getInteger("min_val");
    }

    private void beginDoPayBack(final String phoneNumber
            , final int willVoteAmount
            , final long tranId
            , final int max_pay_back_count
            , final String pay_back_account
            , final int delaytime
            , final String serviceId
            , final JsonObject localCfg
            , final Message message) {

        vertx.setTimer(delaytime * 1000, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                glbCfg = localCfg.containsField("event_noti_resource") ? localCfg : glbCfg;
                doPayBack(phoneNumber
                        , willVoteAmount
                        , tranId
                        , max_pay_back_count
                        , pay_back_account
                        , serviceId
                        , message);
            }
        });
    }

    private void doPayBack(final String phoneNumber
            , final int willVoteAmount
            , final long tranId
            , final int max_pay_back_count
            , final String pay_back_account
            , final String serviceId
            , final Message message
    ) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("" + tranId);
        log.add("serviceid", serviceId);

        cdhhPayBack.getVotedAmount(phoneNumber, serviceId, new Handler<Integer>() {
            @Override
            public void handle(Integer votedAmt) {

                final JsonObject joReply = new JsonObject();
                if (votedAmt >= max_pay_back_count) {
                    joReply.putString("desc", "Số lượng tin nhắn đã hoàn tiền : " + votedAmt);
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }

                int willPayBackAmt = 0;
                if ((votedAmt + willVoteAmount) < max_pay_back_count) {
                    willPayBackAmt = willVoteAmount;
                } else {
                    willPayBackAmt = max_pay_back_count - votedAmt;
                }
                final long paybackVal = willPayBackAmt * getMinValue(serviceId);

                final int incPackbackCnt = willPayBackAmt;

                //khong payback tien
                if (paybackVal == 0) {
                    log.add("cdhhdesc", "Da hoan tien du so luong tin nhan(3)");

                    joReply.putString("desc", "Số lượng tin nhắn đã hoàn tiền : " + votedAmt);
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }

                ArrayList<Misc.KeyValue> arrayList = new ArrayList<>();
                arrayList.add(new Misc.KeyValue("payback", serviceId));

                Misc.adjustment(vertx
                        , pay_back_account
                        , phoneNumber
                        , paybackVal
                        , WalletType.MOMO
                        , arrayList
                        , log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("error", soapObjReply.error);
                        log.add("desc", SoapError.getDesc(soapObjReply.error));
//                        log.writeLog();

                        if (soapObjReply.error == 0) {

                            joReply.putString("desc", "Số tiền đã được hoàn là : " + paybackVal);
                            message.reply(joReply);

                            String comment = "";
                            String caption = "";
                            caption = "Hoàn tiền bình chọn";
                            comment = String.format("Chúc mừng bạn đã nhận %s đồng từ chương trình miễn phí 3 tin nhắn bình chọn từ Ứng dụng MoMo. Cảm ơn bạn đã dùng Ứng dụng MoMo để bình chọn."
                                    , Misc.formatAmount(paybackVal).replace(",", ".")
                                    , String.valueOf(3));
                            JsonObject jsonEvent_Noti = glbCfg.getObject("event_noti_resource");
                            if (jsonEvent_Noti != null) {

//                                JsonObject noti = jsonEvent_Noti.getObject(serviceId);
//                                if(noti != null)
//                                {
                                JsonObject noti_cap = jsonEvent_Noti.getObject("cap");
                                if (noti_cap != null) {
                                    caption = noti_cap.getString("PayBackComepleted", caption);

                                }
                                JsonObject noti_body = jsonEvent_Noti.getObject("body");
                                if (noti_body != null) {
                                    String commentTmp = noti_body.getString("PayBackComepleted", "");
                                    if (!commentTmp.equalsIgnoreCase("")) {
                                        comment = String.format(commentTmp
                                                , Misc.formatAmount(paybackVal).replace(",", ".")
                                                , String.valueOf(3));

                                    }
                                }

                                //}

                            }

//                            if("capdoihoanhao".equalsIgnoreCase(serviceId)){
//                                caption = "Hoàn tiền bình chọn";
//                                comment = "Chúc mừng bạn đã nhận "+ Misc.formatAmount(paybackVal).replace(",",".") +" đồng từ chương trình miễn phí 3 tin nhắn bình chọn của Cặp đôi Hoàn hảo 2014. Cảm ơn bạn đã dùng Ví MoMo để bình chọn.";
//                            }else if("buocnhayhoanvu".equalsIgnoreCase(serviceId)){
//                                caption = Noti.Cap.PayBackComepleted;
//                                comment = String.format(Noti.Body.PayBackComepleted
//                                        , Misc.formatAmount(paybackVal).replace(",",".")
//                                        ,String.valueOf(3));
//                            }else if("remix".equalsIgnoreCase(serviceId)){
//                                caption = Noti.Cap.PayBackComepleted;
//                                comment = String.format(Noti.Remix.Body.PayBackComepleted
//                                        , Misc.formatAmount(paybackVal).replace(",",".")
//                                        ,String.valueOf(3));
//                            }else if("vnidol".equalsIgnoreCase(serviceId)){
//                                caption = Noti.Cap.PayBackComepleted;
//                                comment = String.format(Noti.VnIdol.Body.PayBackComepleted
//                                        , Misc.formatAmount(paybackVal).replace(",",".")
//                                        ,String.valueOf(3));
//                            }


                            //todo cap nhat so luong hoan tien
                            cdhhPayBack.incVotedAmount(phoneNumber
                                    , serviceId
                                    , incPackbackCnt
                                    , new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean o) {
                                }
                            });

                            //todo send tranout side
                            if ("".equalsIgnoreCase(comment)) {
                                return;
                            }

                            long curTime = System.currentTimeMillis();
                            final Notification noti = new Notification();
                            noti.receiverNumber = DataUtil.strToInt(phoneNumber);
                            noti.caption = caption;
                            noti.body = comment;
                            noti.bodyIOS = comment;
                            noti.sms = "";
                            noti.tranId = soapObjReply.tranId;
                            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                            noti.priority = 2;
                            noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                            noti.time = System.currentTimeMillis();
                            noti.cmdId = curTime;

                            final TranObj tran = new TranObj();
                            tran.owner_number = DataUtil.strToInt(phoneNumber);
                            tran.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                            tran.status = TranObj.STATUS_OK;
                            tran.io = 1;
                            tran.comment = comment;
                            tran.tranId = soapObjReply.tranId;
                            tran.cmdId = curTime;
                            tran.error = 0;
                            tran.billId = "";
                            tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                            tran.clientTime = curTime;
                            tran.ackTime = curTime;
                            tran.finishTime = curTime;
                            tran.amount = paybackVal;
                            tran.owner_name = "";
                            tran.category = 0;
                            tran.deleted = false;
                            tran.partnerId = caption;
                            tran.parterCode = "M_Service";
                            tran.partnerName = "M_Service";
                            tran.cmdId = curTime;
                            transDb.upsertTranOutSideNew(tran.owner_number
                                    , tran.getJSON(), new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean result) {

                                    BroadcastHandler.sendOutSideTransSync(vertx, tran);

                                    //ban notification
                                    Misc.sendNoti(vertx, noti);

                                    //force update so du
                                    PhonesDb.Obj obj = new PhonesDb.Obj();
                                    obj.number = DataUtil.strToInt(phoneNumber);
                                    Misc.forceUpdateAgentInfo(vertx, obj);
                                }
                            });

                            log.writeLog();

                        } else {
                            String errdesc = "Soap error : " + SoapError.getDesc(soapObjReply.error);
                            joReply.putString("desc", errdesc);
                            message.reply(joReply);
                            log.add("error", soapObjReply.error);
                            log.add("errdesc", errdesc);
                            log.writeLog();
                        }
                    }
                });
            }
        });
    }
}
