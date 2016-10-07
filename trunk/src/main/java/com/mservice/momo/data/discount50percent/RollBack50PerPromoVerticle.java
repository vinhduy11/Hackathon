package com.mservice.momo.data.discount50percent;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.visampointpromo.VisaMpointPromoConst;
import com.mservice.momo.vertx.visampointpromo.VisaMpointPromoDb;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by concu on 11/3/15.
 */
public class RollBack50PerPromoVerticle extends Verticle {

    long time20days = 1000L * 60 * 60 * 24 * 20;
    RollBack50PerPromoDb rollBack50PerPromoDb;
    TransDb transDb;
    VisaMpointPromoDb visaMpointPromoDb;
    private Logger logger;
    private JsonObject glbCfg;
    private boolean isStoreApp;
    private JsonObject jsonRollbackPromo;
    private Common common;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        jsonRollbackPromo = glbCfg.getObject(StringConstUtil.RollBack50Percent.JSON_OBJECT, new JsonObject());
        final boolean isRollbackPromoActive = jsonRollbackPromo.getBoolean(StringConstUtil.RollBack50Percent.IS_ACTIVE, false);
        //Them thong tin mapping dich vu

        rollBack50PerPromoDb = new RollBack50PerPromoDb(vertx, logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        common = new Common(vertx, logger, container.config());
        visaMpointPromoDb = new VisaMpointPromoDb(vertx, logger);

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final Common.BuildLog log = new Common.BuildLog(logger);
                final JsonObject reqJson = message.body();
                final RollBack50PerPromoObj rollBack50PerPromoObj = new RollBack50PerPromoObj(reqJson);
                log.setPhoneNumber(rollBack50PerPromoObj.phoneNumber);
                log.add("phoneNumber", rollBack50PerPromoObj.phoneNumber);
                log.add("trantype", rollBack50PerPromoObj.tranType);
                log.add("tranid", rollBack50PerPromoObj.tranId);
                log.add("source", rollBack50PerPromoObj.source);
                log.add("cmnd", rollBack50PerPromoObj.cmnd);
                log.add("appCode", rollBack50PerPromoObj.appCode);
                final JsonObject jsonReply = new JsonObject();
                final long limit_amount = jsonRollbackPromo.getLong(StringConstUtil.RollBack50Percent.LIMIT_AMOUNT, 200000);
                log.add("limit_amount", limit_amount);

                //Neu diem giao dich chuoi lot vo day thi khong tra thuong
                if (isStoreApp) {
                    log.add("desc", "app diem giao dich khong cho choi october promo");
                    log.writeLog();
                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "app diem giao dich khong cho choi october promo");
                    message.reply(jsonReply);
                    return;
                } else if (!isRollbackPromoActive) {
                    log.add("desc", "Chua active chuong trinh");
                    log.writeLog();
                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Chua active chuong trinh");
                    message.reply(jsonReply);
                    return;
                } else if (rollBack50PerPromoObj.phoneNumber.equalsIgnoreCase("") || DataUtil.strToLong(rollBack50PerPromoObj.phoneNumber) < 1) {
                    log.add("desc", "So dien thoai la so dien thoai khong co that.");
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                } else if (!StringConstUtil.RollBack50Percent.ROLLBACK_PROMO.equalsIgnoreCase(rollBack50PerPromoObj.source)) {
                    log.add("desc", "Sai chuong trinh.");
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                } else if (rollBack50PerPromoObj.tranId == 0 || rollBack50PerPromoObj.tranType == 0 || "".equalsIgnoreCase(rollBack50PerPromoObj.cmnd)) {
                    log.add("desc", "Khong co thong tin giao dich");
                    log.writeLog();
                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    return;

                } else {
                    //Thoat het cac lop, bat dau tra khuyen mai
                    log.add("desc", "tra khuyen mai");
                    rollBack50PerPromoDb.searchPersonalDataList(rollBack50PerPromoObj.cmnd, rollBack50PerPromoObj.phoneNumber, new Handler<ArrayList<RollBack50PerPromoDb.Obj>>() {
                        @Override
                        public void handle(final ArrayList<RollBack50PerPromoDb.Obj> objs) {
                            if (objs.size() > 0) {
                                final long totalAmount = checkCurrentTotalAmount(objs);
                                if (!objs.get(0).cmnd.equalsIgnoreCase(rollBack50PerPromoObj.cmnd)) {
                                    //Khong trung CMND, khong tra thuong.
                                    //Chua co duoc tra thuong, tra thuong lan dau
                                    log.add("desc", "Thong tin chung minh nhan thuong khac voi thong tin dang yeu cau, khong tra thuong");
                                    log.writeLog();
                                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Thong tin chung minh nhan thuong khac voi thong tin dang yeu cau, khong tra thuong");
                                    message.reply(jsonReply);
                                    return;
                                } else if (!objs.get(0).phone_number.equalsIgnoreCase(rollBack50PerPromoObj.phoneNumber)) {
                                    log.add("desc", "Thong tin so dien thoai da nhan thuong khac voi thong tin dang yeu cau, khong tra thuong");
                                    log.writeLog();
                                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Thong tin so dien thoai da nhan thuong khac voi thong tin dang yeu cau, khong tra thuong");
                                    message.reply(jsonReply);
                                    return;
                                }
                                else if(!objs.get(0).bank_code.equalsIgnoreCase(rollBack50PerPromoObj.bank_code))
                                {
                                    log.add("desc", "Bank code khong dung, khong tra thuong");
                                    log.writeLog();
                                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Thong tin ngan hang da nhan thuong khac voi thong tin dang yeu cau, khong tra thuong");
                                    message.reply(jsonReply);
                                    return;
                                }
                                else if (objs.size() > 9) {
                                    log.add("desc", "Da nhan thuong 10 lan, khong tra thuong nua");
                                    log.writeLog();
                                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Da nhan thuong 10 lan, khong tra thuong nua");
                                    message.reply(jsonReply);
                                    return;
                                } else if (totalAmount >= limit_amount) {
                                    log.add("desc", "Da nhan thuong du so tien cho phep, khong tra thuong nua");
                                    log.writeLog();
                                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Da nhan thuong du so tien cho phep, khong tra thuong nua");
                                    message.reply(jsonReply);
                                    return;
                                } else {
                                    //log.add("desc", "Thong tin mapping hoan hao, tra thuong");
                                    visaMpointPromoDb.searchPersonalDataList(rollBack50PerPromoObj.cmnd, rollBack50PerPromoObj.phoneNumber, new Handler<ArrayList<VisaMpointPromoDb.Obj>>() {
                                        @Override
                                        public void handle(ArrayList<VisaMpointPromoDb.Obj> visaObjs) {
                                            if(visaObjs.size() == 0)
                                            {
                                                log.add("desc", "Thong tin mapping hoan hao, tra thuong");
                                                rollbackMoneyToCustomer(message, rollBack50PerPromoObj, log, objs, totalAmount);
                                            }
                                            else
                                            {
                                                log.add("desc", "Da nhan visa 10%");
                                            }
                                            log.writeLog();
                                        }
                                    });

                                    return;
                                }
                            }

                            visaMpointPromoDb.searchPersonalDataList(rollBack50PerPromoObj.cmnd, rollBack50PerPromoObj.phoneNumber, new Handler<ArrayList<VisaMpointPromoDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<VisaMpointPromoDb.Obj> visaObjs) {
                                    if(visaObjs.size() == 0)
                                    {
                                        //Chua co duoc tra thuong, tra thuong lan dau
                                        log.add("desc", "Chua duoc tra thuong, tra thuong lan 1");
                                        log.add("desc", "tra thuong");
                                        rollbackMoneyToCustomer(message, rollBack50PerPromoObj, log, objs, 0);
                                    }
                                    else {
                                        //Chua co duoc tra thuong, tra thuong lan dau
                                        log.add("desc", "Da nhan visa 10%");
                                    }
                                    log.writeLog();
                                }
                            });
                            return;
                        }
                    });
                }
            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.ROLLBACK_50PERCENT_PROMO_BUSS_ADDRESS, myHandler);
    }

    private long checkCurrentTotalAmount(ArrayList<RollBack50PerPromoDb.Obj> arrayListRollbackList) {
        long totalAmount = 0;

        for (int i = 0; i < arrayListRollbackList.size(); i++) {
            totalAmount = totalAmount + arrayListRollbackList.get(i).bonus_amount;
        }
        return totalAmount;
    }

    private void rollbackMoneyToCustomer(final Message message, final RollBack50PerPromoObj rollBack50PerPromoObj, final Common.BuildLog log, final ArrayList<RollBack50PerPromoDb.Obj> objs, long totalAmount) {
        int percent = jsonRollbackPromo.getInteger(StringConstUtil.RollBack50Percent.PERCENT, 50);
        int visa_percent = jsonRollbackPromo.getInteger(StringConstUtil.RollBack50Percent.VISA_PERCENT, 20);
        long limitAmout = jsonRollbackPromo.getLong(StringConstUtil.RollBack50Percent.LIMIT_AMOUNT, 200000);
        String agent = jsonRollbackPromo.getString(StringConstUtil.RollBack50Percent.AGENT, "cskh");
        log.add("percent tra thuong", percent);
        log.add("limitAmout tra thuong", limitAmout);
        String[] bank = rollBack50PerPromoObj.bank_code.split("_");
        int final_percent = "sbs".equalsIgnoreCase(bank[0].toString()) ? visa_percent : percent;
        final long bonusAmount_tmp = rollBack50PerPromoObj.bank_amount * final_percent / 100;
        log.add("so tien phai tra thuong", bonusAmount_tmp);

        final long bonusAmount_tmp1 = limitAmout - totalAmount;
        log.add("So tien con lai duoc tra ", bonusAmount_tmp1);

        final long bonusAmount = Math.min(bonusAmount_tmp, bonusAmount_tmp1);

        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = StringConstUtil.RollBack50Percent.ROLLBACK_PROMO;
        Misc.adjustment(vertx, agent, rollBack50PerPromoObj.phoneNumber, bonusAmount, Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
            @Override
            public void handle(Common.SoapObjReply soapObjReply) {
                if (soapObjReply != null && soapObjReply.error != 0) {
                    log.add("core tra loi ", soapObjReply.error);
                    log.add("status ", soapObjReply.status);
                    log.add("tid", soapObjReply.tranId);
                    log.add("desc", "core tra loi");
                    log.writeLog();
                    JsonObject jsonReply = new JsonObject();
                    jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                    jsonReply.putString(StringConstUtil.DESCRIPTION,  "core tra loi");
                    message.reply(jsonReply);
                    return;
                }

                log.add("core tra loi ", soapObjReply.error);
                log.add("status ", soapObjReply.status);
                log.add("tid", soapObjReply.tranId);
                log.add("desc", "thanh cong nhe");

                RollBack50PerPromoDb.Obj rollBackDbObj = new RollBack50PerPromoDb.Obj();
                rollBackDbObj.phone_number = rollBack50PerPromoObj.phoneNumber;
                rollBackDbObj.promo_count = objs.size();
                rollBackDbObj.tran_amount = rollBack50PerPromoObj.tran_amount;
                rollBackDbObj.bank_amount = rollBack50PerPromoObj.bank_amount;
                rollBackDbObj.bonus_amount = bonusAmount;
                rollBackDbObj.bank_code = rollBack50PerPromoObj.bank_code;
                rollBackDbObj.cmnd = rollBack50PerPromoObj.cmnd;
                rollBackDbObj.tran_id = rollBack50PerPromoObj.tranId;
                rollBackDbObj.bank_tran_id = rollBack50PerPromoObj.bank_tid;
                rollBackDbObj.bonus_time = System.currentTimeMillis();
                rollBackDbObj.service_id = rollBack50PerPromoObj.serviceId;
                rollBackDbObj.promo_tran_id = soapObjReply.tranId;
                rollBack50PerPromoDb.insert(rollBackDbObj, new Handler<Integer>() {
                    @Override
                    public void handle(Integer integer) {
                        logger.info("them thong tin vao bang tra thuong rollback la " + integer);
                    }
                });
                sendTranHisPromo(rollBackDbObj, soapObjReply, rollBack50PerPromoObj.appCode);
                JsonObject joRepLy = new JsonObject();
                joRepLy.putNumber("error", soapObjReply.error);
                joRepLy.putString(StringConstUtil.DESCRIPTION,  "NGON");
                joRepLy.putNumber(VisaMpointPromoConst.COUNT,  objs.size() + 1);
                message.reply(joRepLy);
                log.writeLog();
                return;
            }
        });
    }



    private void firePromoNoti(RollBack50PerPromoDb.Obj rollBackDbObj, Common.SoapObjReply soapObjReply) {
        String notiBody = "";
        String[] bank = rollBackDbObj.bank_code.split("_");
        if ("12345".equalsIgnoreCase(bank[0].toString()))
        {
            notiBody = "Bạn vừa được hoàn lại " + Misc.formatAmount(rollBackDbObj.bonus_amount) +
                    "đ tiền khuyến mãi vào Tài khoản MoMo khi thực hiện thanh toán theo chương trình “Hoàn 50% cho chủ thẻ VCB”. Chi tiết liên hệ 08 39917199.";
        }
        else if("sbs".equalsIgnoreCase(bank[0].toString()))
        {
            notiBody = "Bạn vừa được hoàn lại " + Misc.formatAmount(rollBackDbObj.bonus_amount) +
                    "đ tiền khuyến mãi vào Tài khoản MoMo khi thực hiện thanh toán theo chương trình “Hoàn 20% cho chủ thẻ Visa/Master”. Chi tiết liên hệ 08 39917199.";
        }
        else
        {
            notiBody = "Quý khách vừa được hoàn lại " + Misc.formatAmount(rollBackDbObj.bonus_amount) +
                    "đ tiền khuyến mãi vào Tài khoản MoMo khi thực hiện thanh toán " +
                    "theo chương trình “Hoàn 50% giá trị thanh toán”. \n" +
                    "Chi tiết liên hệ 08 39917199.";
        }

        String notiCaption = "Nhận tiền khuyến mãi";

        final Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
        noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = soapObjReply.tranId;
        noti.time = new Date().getTime();

        noti.receiverNumber = DataUtil.strToInt(rollBackDbObj.phone_number);
        Misc.sendNoti(vertx, noti);

//                common.sendCurrentAgentInfo(vertx, sock, 0, msg.cmdPhone, data);
        return;
    }

    public void sendTranHisPromo(final RollBack50PerPromoDb.Obj rollBackDbObj, final Common.SoapObjReply soapObjReply, int appCode) {

        //Send tranhis
        String comment = "";
        String[] bank = rollBackDbObj.bank_code.split("_");
        if ("12345".equalsIgnoreCase(bank[0].toString()))
        {
            comment = "Bạn vừa được hoàn lại " + Misc.formatAmount(rollBackDbObj.bonus_amount) +
                    "đ tiền khuyến mãi vào Tài khoản MoMo khi thực hiện thanh toán theo chương trình “Hoàn 50% cho chủ thẻ VCB”. Chi tiết liên hệ 08 39917199.";
        }
        else if("sbs".equalsIgnoreCase(bank[0].toString()))
        {
            comment = "Bạn vừa được hoàn lại " + Misc.formatAmount(rollBackDbObj.bonus_amount) +
                    "đ tiền khuyến mãi vào Tài khoản MoMo khi thực hiện thanh toán theo chương trình “Hoàn 20% cho chủ thẻ Visa/Master”. Chi tiết liên hệ 08 39917199.";
        }
        else
        {
            comment = "Quý khách vừa được hoàn lại " + Misc.formatAmount(rollBackDbObj.bonus_amount) +
                    "đ tiền khuyến mãi vào Tài khoản MoMo khi thực hiện thanh toán " +
                    "theo chương trình “Hoàn 50% giá trị thanh toán”. \n" +
                    "Chi tiết liên hệ 08 39917199.";
        }
        int tranType = MomoProto.TranHisV1.TranType.PROMOTION_VALUE;
        if(appCode > 100)
        {
            tranType = MomoProto.TranHisV1.TranType.MPOINT_CLAIM_VALUE;
        }
        final TranObj mainObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        mainObj.tranType = tranType;
        mainObj.comment = comment;
        mainObj.tranId = soapObjReply.tranId;
        mainObj.clientTime = currentTime;
        mainObj.ackTime = currentTime;
        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = rollBackDbObj.bonus_amount;
        mainObj.status = TranObj.STATUS_OK;
        mainObj.error = 0;
        mainObj.io = 1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.owner_number = DataUtil.strToInt(rollBackDbObj.phone_number);
        mainObj.partnerName = "M_Service";
        mainObj.partnerId = "";
        mainObj.partnerRef = mainObj.comment;
        transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                    firePromoNoti(rollBackDbObj, soapObjReply);
                }
            }
        });
    }
}
