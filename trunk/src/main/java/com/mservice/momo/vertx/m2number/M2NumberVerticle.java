package com.mservice.momo.vertx.m2number;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.M2cOffline;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.msg.CoreMessage;
import com.mservice.momo.gateway.internal.core.objects.Command;
import com.mservice.momo.gateway.internal.core.objects.Request;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by concu on 5/31/14.
 */
public class M2NumberVerticle extends Verticle {

    private long timerID;
    private Logger logger;
    private M2cOffline m2cOffline;
    private TransDb transDb;
    private PhonesDb phonesDb;
    private long interval;

    //private long deltaSecs;
    private String sms2;
    private String sms3;
    private String sms4;

    private long h24Secs = 24 * 60 * 60; //24h by seconds
    private long h36Secs = 36 * 60 * 60; //36h by seconds
    private long h48Secs = 48 * 60 * 60; //48h by seconds

    private boolean allow_m2number = false;
    private boolean remindByNoti = false;

    public void start() {
        //time to scan

        //todo production
        interval = 10 * 60 * 1000;//10 phut quet 1 lan
        //todo test
        //interval = 60*1000;//10 phut quet 1 lan
        logger = container.logger();
        m2cOffline = new M2cOffline(vertx.eventBus(), logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        phonesDb = new PhonesDb(vertx.eventBus(), logger);

        JsonObject m2number_cfg = container.config().getObject("sms_for_m2number");

        /*"sms_for_m2number":{
            "sms1":"Ban nhan duoc %sd tu 0&d. Truy cap http://app.momo.vn/%s de nhan tien trong 48 gio hoac goi 0839917199 de duoc ho tro.",
                    "sms2":"Ban con 24 gio de nhan so tien %sd tu sdt %s. Truy cap http://app.momo.vn/%s de nhan tien. Hoac goi 0839917199 de duoc ho tro. Cam on.",
                    "sms3":"Ban con 12 gio de nhan so tien %sd tu sdt %s. Truy cap http://app.momo.vn/%s de nhan tien. Hoac goi 0839917199 de duoc ho tro. Cam on.",
                    "sms4":"Giao dich da huy so tien %sd se duoc tra lai cho nguoi gui. Cam on.",
                    "after1time":24, // hour
                    "after2time":36, //hour
                    "after3time":48 //hour
        },*/

        //sms for each time that we send sms to receiver money
//        sms2 = container.config().getString("sms2");
//        sms3 = container.config().getString("sms3");
//        sms4 = container.config().getString("sms4");
        sms2 = m2number_cfg.getString("sms2");
        sms3 = m2number_cfg.getString("sms3");
        sms4 = m2number_cfg.getString("sms4");

        //todo this is production
//        h24Secs = container.config().getInteger("after1time")*60*60; // tinh theo giay
//        h36Secs = container.config().getInteger("after2time")*60*60; // tinh theo giay
//        h48Secs = container.config().getInteger("after3time")*60*60; // tinh theo giay

        h24Secs = m2number_cfg.getInteger("after1time") * 60 * 60; // tinh theo giay
        h36Secs = m2number_cfg.getInteger("after2time") * 60 * 60; // tinh theo giay
        h48Secs = m2number_cfg.getInteger("after3time") * 60 * 60; // tinh theo giay

        //todo only test
        /*interval = 7*1000; // 1 phut
        h24Secs = 25; // tinh theo giay
        h36Secs = 40; // tinh theo giay
        h48Secs = 60; // tinh theo giay*/

//        allow_m2number = container.config().getBoolean("allow_m2number",false);
//
//        remindByNoti = container.config().getBoolean("remind_by_noti", false);

        allow_m2number = m2number_cfg.getBoolean("allow_m2number", false);

        remindByNoti = m2number_cfg.getBoolean("remind_by_noti", false);

        logger.info("CHO PHEP CHAY TIMER M2NUMBER " + allow_m2number);

        if (allow_m2number) {
            if (remindByNoti) {

                logger.info("Timer for m2number by send out notification");
                timerID = vertx.setPeriodic(interval, new Handler<Long>() {
                    public void handle(Long tId) {
                        doScanWaitingByNoti(tId);
                    }
                });
                doScanWaitingByNoti(0L);
            } else {

                logger.info("Timer for m2number by send out sms");
                timerID = vertx.setPeriodic(interval, new Handler<Long>() {
                    public void handle(Long tId) {
                        doScanWaitingBySms(tId);
                    }
                });
                doScanWaitingBySms(0L);
            }
        }
    }

    private void doScanWaitingByNoti(Long tid) {

        final Common.BuildLog ulog = new Common.BuildLog(logger);
        ulog.setPhoneNumber("doScanWaiting");
        ulog.add("send out", "by notification");
        ulog.add("run at", Misc.dateVNFormatWithTime(System.currentTimeMillis()));

        //lay tat ca cac giao dich chua thuc hien
        m2cOffline.getWaitingObjs(new Handler<ArrayList<M2cOffline.Obj>>() {
            @Override
            public void handle(ArrayList<M2cOffline.Obj> objs) {

                if (objs == null || objs.size() == 0) {
                    ulog.add("m2number", "no waiting orders");
                    ulog.writeLog();
                    return;
                }

                ulog.add("total m2number will be processed", objs.size());
//                ulog.writeLog();

                for (int i = 0; i < objs.size(); i++) {

                    ulog.add("item " + (i + 1), "*****************");
                    ulog.add("at time", Misc.dateVNFormatWithTime(System.currentTimeMillis()));

                    M2cOffline.Obj o = objs.get(i);
                    String newStatus = colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW);

                    Date createTime = new Date(o.CREATE_TIME);
                    Date curDate = new Date();
                    long diffBySecs = (curDate.getTime() - createTime.getTime()) / 1000;

                    //het han
                    if ((diffBySecs >= h48Secs) && o.SMS_SENDED == 3 && o.STATUS.equalsIgnoreCase(newStatus)) {
                        ulog.add("function", "chay doExpired m2number, tra tien lai cho nguoi gui 0" + o.SOURCE_PHONE);
                        doExpiredByNoti(o);
                        continue;
                    }

                    //nhac lan 3
                    if ((diffBySecs >= h36Secs) && o.SMS_SENDED == 2 && o.STATUS.equalsIgnoreCase(newStatus)) {
                        ulog.add("function", "chay doRemain3 m2number, gui noti nhac lan 3 cho vi 0" + o.SOURCE_PHONE);
                        doRemain3ByNoti(o);
                        continue;
                    }

                    //nhac lan 2
                    if ((diffBySecs >= h24Secs) && o.SMS_SENDED == 1 && o.STATUS.equalsIgnoreCase(newStatus)) {
                        ulog.add("function", "chay doRemain2 m2number, gui noti nhac lan 2 cho vi 0" + o.SOURCE_PHONE);
                        doRemain2ByNoti(o);
                    }
                }
                ulog.writeLog();
            }
        });
    }


    private void doScanWaitingBySms(Long tid) {

        final Common.BuildLog ulog = new Common.BuildLog(logger);
        ulog.setPhoneNumber("doScanWaiting");
        ulog.add("send out", "by sms");
        ulog.add("run at", Misc.dateVNFormatWithTime(System.currentTimeMillis()));

        //lay tat ca cac giao dich chua thuc hien
        m2cOffline.getWaitingObjs(new Handler<ArrayList<M2cOffline.Obj>>() {
            @Override
            public void handle(ArrayList<M2cOffline.Obj> objs) {

                if (objs == null || objs.size() == 0) {
                    ulog.add("m2number", "no waiting orders");
                    ulog.writeLog();
                    return;
                }

                ulog.add("total m2number will be processed", objs.size());
//                ulog.writeLog();

                for (int i = 0; i < objs.size(); i++) {

                    ulog.add("item " + (i + 1), "*****************");
                    ulog.add("at time", Misc.dateVNFormatWithTime(System.currentTimeMillis()));

                    M2cOffline.Obj o = objs.get(i);
                    String newStatus = colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW);

                    Date createTime = new Date(o.CREATE_TIME);
                    Date curDate = new Date();
                    long diffBySecs = (curDate.getTime() - createTime.getTime()) / 1000;

                    //het han
                    if ((diffBySecs >= h48Secs) && o.SMS_SENDED == 3 && o.STATUS.equalsIgnoreCase(newStatus)) {
                        ulog.add("function", "chay doExpired m2number, tra tien lai cho nguoi gui 0" + o.SOURCE_PHONE);
                        doExpiredBySms(o);
                        continue;
                    }

                    //nhac lan 3
                    if ((diffBySecs >= h36Secs) && o.SMS_SENDED == 2 && o.STATUS.equalsIgnoreCase(newStatus)) {
                        ulog.add("function", "chay doRemain3 m2number, gui noti nhac lan 3 cho vi 0" + o.SOURCE_PHONE);
                        doRemain3BySms(o);
                        continue;
                    }

                    //nhac lan 2
                    if ((diffBySecs >= h24Secs) && o.SMS_SENDED == 1 && o.STATUS.equalsIgnoreCase(newStatus)) {
                        ulog.add("function", "chay doRemain2 m2number, gui noti nhac lan 2 cho vi 0" + o.SOURCE_PHONE);
                        doRemain2BySms(o);
                    }
                }
                ulog.writeLog();
            }
        });
    }


    /*"sms2":"Ban con 24 gio de nhan so tien %sd tu sdt %s. Truy cap http://app.momo.vn/%s de nhan tien. Hoac goi 0839917199 de duoc ho tro. Cam on.",
    "sms3":"Ban con 12 gio de nhan so tien %sd tu sdt %s. Truy cap http://app.momo.vn/%s de nhan tien. Hoac goi 0839917199 de duoc ho tro. Cam on.",
    "sms4":"Giao dich da huy so tien %sd se duoc tra lai cho nguoi gui. Cam on.",*/


    private void doExpiredByNoti(final M2cOffline.Obj o) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + o.SOURCE_PHONE);
        log.add("function", "doExpired Waiting M2number");
        log.add("receiver_number", "0" + o.DESTINATION_PHONE);
        log.add("tranid", o.TRAN_ID);

        Request rbObj = new Request();
        rbObj.TYPE = Command.ROLLBACK;
        rbObj.TRAN_ID = o.TRAN_ID;
        rbObj.WALLET = WalletType.MOMO;
        rbObj.PHONE_NUMBER = log.getPhoneNumber();
        rbObj.TIME = log.getTime();

        log.add("timer M2number request rollback", rbObj.toJsonObject());

        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS
                , rbObj.toJsonObject()
                , new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {

                Buffer buf = message.body();

                final CoreMessage reply = CoreMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(reply.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                log.add("rollback result", (rpl == null ? "rpl == null" : rpl.getErrorCode()));
                log.add("rollback result tid", (rpl == null ? "rpl == null" : rpl.getTid()));

                if (rpl != null && (rpl.getErrorCode() == 0 || rpl.getErrorCode() == 400 || rpl.getErrorCode() == 103)) {

                    m2cOffline.updateAndGetObjectByTranId(o.TRAN_ID
                            , colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.EXPIRED)
                            , "timer"
                            , new Handler<Boolean>() {

                        @Override
                        public void handle(Boolean result) {

                            log.add("function", "updateAndGetObjectByTranId");
                            log.add("desc", "update status of m2cOffline");

                            if (result) {
                                log.add("result", colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.EXPIRED));

                                log.add("function", "transDb.updateTranStatusNew");

                                transDb.updateTranStatusNew(o.SOURCE_PHONE, o.TRAN_ID, TranObj.CANCELLED, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean result) {

                                        if (result == true) {

                                            log.add("transDb status", TranObj.CANCELLED);

                                            String sms = String.format(sms4, String.format("%,d", o.AMOUNT));

                                            log.add("sms sent out", sms);

                                            //todo ban noti cho thang gui
                                            String body = String.format(Data.Body.expired
                                                    , "0" + o.DESTINATION_PHONE
                                                    , Misc.formatAmount(o.AMOUNT).replaceAll(",", "."));

                                            final Notification noti = new Notification();
                                            noti.receiverNumber = o.SOURCE_PHONE;
                                            noti.caption = Data.Cap.expired;
                                            noti.body = body;
                                            noti.bodyIOS = body;
                                            noti.sms = "";
                                            noti.tranId = o.TRAN_ID; // ban tren toan he thong
                                            noti.status = Notification.STATUS_DETAIL;
                                            noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                            noti.priority = 2;
                                            noti.time = System.currentTimeMillis();
                                            noti.category = 0;
                                            noti.extra = (new JsonObject().putString("msg", String.format(Data.share, Misc.formatAmount(o.AMOUNT), o.SHORT_LINK))
                                                    .putString("cskh", "0839917199")
                                                    .putString("link", Data.link + o.SHORT_LINK).toString());

                                            Misc.sendNoti(vertx, noti);

                                            log.add("noti", noti.toJsonObject().encodePrettily());

                                            phonesDb.getPhoneObjInfoLocal(o.SOURCE_PHONE, new Handler<PhonesDb.Obj>() {
                                                @Override
                                                public void handle(PhonesDb.Obj obj) {
                                                    if (obj != null) {
                                                        Misc.forceUpdateAgentInfo(vertx, obj);
                                                    }
                                                }
                                            });


                                            //todo : xoa tren core nhu the nao ??????
                                            //xoa tai khoan nguoi nhan
                                            phonesDb.removePhoneObj(o.DESTINATION_PHONE, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean aBoolean) {
                                                }
                                            });

                                        } else {
                                            log.add("tranObj after update with cancelled is null", "");
                                        }
                                        log.writeLog();
                                    }
                                });
                            } else {
                                log.add("m2cOfflineObj after update with cancelled status is null", "");
                            }

//                            log.writeLog();
                        }
                    });
                }

//                log.writeLog();
            }
        });
    }


    private void doExpiredBySms(final M2cOffline.Obj o) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + o.SOURCE_PHONE);
        log.add("function", "doExpired Waiting M2number");
        log.add("receiver_number", "0" + o.DESTINATION_PHONE);
        log.add("tranid", o.TRAN_ID);

        Request rbObj = new Request();
        rbObj.TYPE = Command.ROLLBACK;
        rbObj.TRAN_ID = o.TRAN_ID;
        rbObj.WALLET = WalletType.MOMO;
        rbObj.PHONE_NUMBER = log.getPhoneNumber();
        rbObj.TIME = log.getTime();

        log.add("timer M2number request rollback", rbObj.toJsonObject());

        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS
                , rbObj.toJsonObject()
                , new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {

                Buffer buf = message.body();

                final CoreMessage reply = CoreMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(reply.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                log.add("rollback result", (rpl == null ? "rpl == null" : rpl.getErrorCode()));
                log.add("rollback result tid", (rpl == null ? "rpl == null" : rpl.getTid()));

                if (rpl != null && (rpl.getErrorCode() == 0 || rpl.getErrorCode() == 400 || rpl.getErrorCode() == 103)) {

                    m2cOffline.updateAndGetObjectByTranId(o.TRAN_ID
                            , colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.EXPIRED)
                            , "timer"
                            , new Handler<Boolean>() {

                        @Override
                        public void handle(Boolean result) {

                            log.add("function", "updateAndGetObjectByTranId");
                            log.add("desc", "update status of m2cOffline");

                            if (result) {
                                log.add("result", colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.EXPIRED));

                                log.add("function", "transDb.updateTranStatusNew");

                                transDb.updateTranStatusNew(o.SOURCE_PHONE, o.TRAN_ID, TranObj.CANCELLED, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean result) {

                                        if (result == true) {

                                            log.add("transDb status", TranObj.CANCELLED);

                                            String sms = String.format(sms4, String.format("%,d", o.AMOUNT));

                                            log.add("sms sent out", sms);

                                            //todo gui sms
                                            //gui sms cho nguoi nhan tien --> het thoi gian nhan tien roi
                                            SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                                                    .setSmsId(0)
                                                    .setContent(sms)
                                                    .setToNumber(o.DESTINATION_PHONE)
                                                    .build();
                                            vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());

                                            //1. gui sms cho nguoi chuyen tien khi khong thanh cong

                                            //Ban nhan lai [số tiền] tu giao dich hoan tra. Ly do: Giao dich chuyen tien bi huy do nguoi nhan [so dien thoai nguoi nhan] khong nhan tien. Xin cam on!
                                            String smsSenderTmp = "Ban nhan lai %sd tu giao dich hoan tra. Ly do: Giao dich chuyen tien bi huy do nguoi nhan %s khong nhan tien. Xin cam on!";
                                            String smsSender = String.format(smsSenderTmp
                                                    , Misc.formatAmount(o.AMOUNT).replace(",", ".")
                                                    , "0" + o.DESTINATION_PHONE
                                            );

                                            SoapProto.SendSms senderSendSms = SoapProto.SendSms.newBuilder()
                                                    .setSmsId(0)
                                                    .setContent(smsSender)
                                                    .setToNumber(o.SOURCE_PHONE)
                                                    .build();
                                            vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, senderSendSms.toByteArray());

                                            phonesDb.getPhoneObjInfoLocal(o.SOURCE_PHONE, new Handler<PhonesDb.Obj>() {
                                                @Override
                                                public void handle(PhonesDb.Obj obj) {
                                                    if (obj != null) {
                                                        Misc.forceUpdateAgentInfo(vertx, obj);
                                                    }
                                                }
                                            });


                                            //todo : xoa tren core nhu the nao ??????
                                            //xoa tai khoan nguoi nhan
                                            phonesDb.removePhoneObj(o.DESTINATION_PHONE, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean aBoolean) {
                                                }
                                            });

                                        } else {
                                            log.add("tranObj after update with cancelled is null", "");
                                        }
                                        log.writeLog();
                                    }
                                });
                            } else {
                                log.add("m2cOfflineObj after update with cancelled status is null", "");
                            }

//                            log.writeLog();
                        }
                    });
                }

//                log.writeLog();
            }
        });
    }


    private void doRemain3ByNoti(final M2cOffline.Obj o) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + o.SOURCE_PHONE);
        log.add("function", "doRemain2");
        log.add("receiver phone", "0" + o.DESTINATION_PHONE);
        log.add("tranid", o.TRAN_ID);
        log.add("sms times", o.SMS_SENDED);
        log.add("amount", o.AMOUNT);

        m2cOffline.incSmsSent(o.TRAN_ID, new Handler<Boolean>() {
            @Override
            public void handle(Boolean b) {

                log.add("m2cOffline.incSmsSent result", b);

                //todo ban noti cho thang gui
                String body = String.format(Data.Body.remain12h
                        , "0" + o.DESTINATION_PHONE
                        , Misc.formatAmount(o.AMOUNT).replaceAll(",", ".")
                        , "0" + o.DESTINATION_PHONE);

                final Notification noti = new Notification();
                noti.receiverNumber = o.SOURCE_PHONE;
                noti.caption = Data.Cap.remain12h;
                noti.body = body;
                noti.bodyIOS = body;
                noti.sms = "";
                noti.tranId = o.TRAN_ID; // ban tren toan he thong
                noti.status = Notification.STATUS_DETAIL;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                noti.priority = 2;
                noti.time = System.currentTimeMillis();
                noti.category = 0;
                noti.extra = (new JsonObject().putString("msg", String.format(Data.share, Misc.formatAmount(o.AMOUNT), o.SHORT_LINK))
                        .putString("cskh", "")
                        .putString("link", Data.link + o.SHORT_LINK).toString());

                Misc.sendNoti(vertx, noti);
                log.add("noti", noti.toJsonObject().encodePrettily());
                log.writeLog();
            }
        });
    }


    private void doRemain3BySms(final M2cOffline.Obj o) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + o.SOURCE_PHONE);
        log.add("function", "doRemain2");
        log.add("receiver phone", "0" + o.DESTINATION_PHONE);
        log.add("tranid", o.TRAN_ID);
        log.add("sms times", o.SMS_SENDED);
        log.add("amount", o.AMOUNT);

        m2cOffline.incSmsSent(o.TRAN_ID, new Handler<Boolean>() {
            @Override
            public void handle(Boolean b) {

                log.add("m2cOffline.incSmsSent result", b);

                //todo send sms
                String sms = String.format(sms3
                        , String.format("%,d", o.AMOUNT)
                        , "0" + o.SOURCE_PHONE
                        , o.SHORT_LINK);

                SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                        .setSmsId(0)
                        .setContent(sms)
                        .setToNumber(o.DESTINATION_PHONE)
                        .build();

                vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
                log.writeLog();
            }
        });
    }


    private void doRemain2ByNoti(final M2cOffline.Obj o) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + o.SOURCE_PHONE);
        log.add("function", "doRemain2");
        log.add("receiver phone", "0" + o.DESTINATION_PHONE);
        log.add("tranid", o.TRAN_ID);
        log.add("sms times", o.SMS_SENDED);
        log.add("amount", o.AMOUNT);

        m2cOffline.incSmsSent(o.TRAN_ID, new Handler<Boolean>() {
            @Override
            public void handle(Boolean b) {
                log.add("m2cOffline.incSmsSent result", b);

                //todo ban noti cho thang gui
                String body = String.format(Data.Body.remain24h
                        , "0" + o.DESTINATION_PHONE
                        , Misc.formatAmount(o.AMOUNT).replaceAll(",", ".")
                        , "0" + o.DESTINATION_PHONE);

                final Notification noti = new Notification();
                noti.receiverNumber = o.SOURCE_PHONE;
                noti.caption = Data.Cap.remain24h;
                noti.body = body;
                noti.bodyIOS = body;
                noti.sms = "";
                noti.tranId = o.TRAN_ID; // ban tren toan he thong
                noti.status = Notification.STATUS_DETAIL;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                noti.priority = 2;
                noti.time = System.currentTimeMillis();
                noti.category = 0;
                noti.extra = (new JsonObject().putString("msg", String.format(Data.share, Misc.formatAmount(o.AMOUNT), o.SHORT_LINK))
                        .putString("cskh", "")
                        .putString("link", Data.link + o.SHORT_LINK).toString());

                Misc.sendNoti(vertx, noti);

                log.add("noti", noti.toJsonObject().encodePrettily());
                log.writeLog();

            }
        });

    }

    private void doRemain2BySms(final M2cOffline.Obj o) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + o.SOURCE_PHONE);
        log.add("function", "doRemain2");
        log.add("receiver phone", "0" + o.DESTINATION_PHONE);
        log.add("tranid", o.TRAN_ID);
        log.add("sms times", o.SMS_SENDED);
        log.add("amount", o.AMOUNT);

        m2cOffline.incSmsSent(o.TRAN_ID, new Handler<Boolean>() {
            @Override
            public void handle(Boolean b) {
                log.add("m2cOffline.incSmsSent result", b);

                //todo send sms
                String sms = String.format(sms2
                        , String.format("%,d", o.AMOUNT)
                        , "0" + o.SOURCE_PHONE
                        , o.SHORT_LINK);

                SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                        .setSmsId(0)
                        .setContent(sms)
                        .setToNumber(o.DESTINATION_PHONE)
                        .build();
                log.add("SEND_SMS M2NUMBER", sms);
//                log.writeLog();
                vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
                log.writeLog();

            }
        });


    }


}
