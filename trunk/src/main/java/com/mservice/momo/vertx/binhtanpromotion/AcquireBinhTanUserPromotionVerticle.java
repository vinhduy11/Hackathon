package com.mservice.momo.vertx.binhtanpromotion;

import com.mservice.momo.data.*;
import com.mservice.momo.data.binhtanpromotion.AcquireBinhTanGroup2PromotionDb;
import com.mservice.momo.data.binhtanpromotion.AcquireBinhTanGroup3PromotionDb;
import com.mservice.momo.data.binhtanpromotion.AcquireBinhTanUserPromotionDb;
import com.mservice.momo.data.binhtanpromotion.BinhTanDgdTransTrackingDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.CheatInfoDb;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.promotion.PhoneCheckDb;
import com.mservice.momo.data.promotion.PromotionCountTrackingDb;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by khoanguyen on 03/05/2016.
 */
public class AcquireBinhTanUserPromotionVerticle extends Verticle {

    HashMap<String, String> hashMap;
    private Logger logger;
    private JsonObject glbCfg;
    private TransDb tranDb;
    private boolean isStoreApp;
    private boolean remindNoti;
    private boolean isUAT;
    private GiftManager giftManager;
    private AcquireBinhTanGroup2PromotionDb acquireBinhTanGroup2PromotionDb;
    private AcquireBinhTanUserPromotionDb acquireBinhTanUserPromotionDb;
    private AcquireBinhTanGroup3PromotionDb acquireBinhTanGroup3PromotionDb;
    private PhoneCheckDb phoneCheckDb;
    private ErrorPromotionTrackingDb errorPromotionTrackingDb;
    private PhonesDb phonesDb;
    private ControlOnClickActivityDb controlOnClickActivityDb;
    private BinhTanDgdTransTrackingDb binhTanDgdTransTrackingDb;
    private PromotionCountTrackingDb promotionCountTrackingDb;
    private CheatInfoDb cheatInfoDb;
    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        isUAT = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
        remindNoti = glbCfg.getBoolean(StringConstUtil.SEND_REMIND_NOTI, false);
        giftManager = new GiftManager(vertx, logger, glbCfg);
        acquireBinhTanGroup2PromotionDb = new AcquireBinhTanGroup2PromotionDb(vertx, logger);
        acquireBinhTanUserPromotionDb = new AcquireBinhTanUserPromotionDb(vertx, logger);
        acquireBinhTanGroup3PromotionDb = new AcquireBinhTanGroup3PromotionDb(vertx, logger);
        phoneCheckDb = new PhoneCheckDb(vertx, logger);
        final long checkInfoDgdTime = isUAT ? 2 : 24 * 60; //
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        hashMap = new HashMap<>();
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        controlOnClickActivityDb = new ControlOnClickActivityDb(vertx);
        binhTanDgdTransTrackingDb = new BinhTanDgdTransTrackingDb(vertx, logger);
        promotionCountTrackingDb = new PromotionCountTrackingDb(vertx, logger);
        cheatInfoDb = new CheatInfoDb(vertx, logger);
        //Cho Tien user 10k
        if(remindNoti)
        {
            remindAndGiveBonus(checkInfoDgdTime);
        }

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final Common.BuildLog log = new Common.BuildLog(logger);
                final BinhTanPromotionObj binhTanPromotionObj = new BinhTanPromotionObj(reqJson);
                log.setPhoneNumber(binhTanPromotionObj.phoneNumber);
                log.add("phoneNumber " + StringConstUtil.BinhTanPromotion.PROGRAM, binhTanPromotionObj.phoneNumber);
                log.add("program " + StringConstUtil.BinhTanPromotion.PROGRAM, binhTanPromotionObj.program);
                log.add("joExtra " + StringConstUtil.BinhTanPromotion.PROGRAM, binhTanPromotionObj.joExtra.toString());
                hashMap.put(binhTanPromotionObj.phoneNumber, binhTanPromotionObj.program);
                final JsonObject jsonReply = new JsonObject();
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;

                if(isStoreApp && !StringConstUtil.BinhTanPromotion.CASHIN_SOURCE.equalsIgnoreCase(binhTanPromotionObj.source))
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong danh cho app DGD");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Khong danh cho app DGD");
                    log.writeLog();
                    message.reply(jsonReply);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong danh cho app DGD " + binhTanPromotionObj.joExtra.toString());
                    return;
                }
                else if("".equalsIgnoreCase(binhTanPromotionObj.phoneNumber) || "".equalsIgnoreCase(binhTanPromotionObj.program)
                        || "".equalsIgnoreCase(binhTanPromotionObj.joExtra.toString()))
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Du lieu thieu sot nghiem trong");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Du lieu thieu sot nghiem trong");
                    log.writeLog();
                    message.reply(jsonReply);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Thieu sot du lieu " + binhTanPromotionObj.joExtra.toString());
                    return;
                }

                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        long promo_start_date = 0;
                        long promo_end_date = 0;
                        long currentTime = System.currentTimeMillis();
                        String agent = "";
                        long total_amount = 0;
                        long perTranAmount = 0;
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj promoObj = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
                                promoObj = new PromotionDb.Obj((JsonObject) o);
                                if (promoObj.NAME.equalsIgnoreCase(binhTanPromotionObj.program)) {
                                    promo_start_date = promoObj.DATE_FROM;
                                    promo_end_date = promoObj.DATE_TO;
                                    agent = promoObj.ADJUST_ACCOUNT;
                                    total_amount = promoObj.TRAN_MIN_VALUE;
                                    perTranAmount = promoObj.PER_TRAN_VALUE;
                                    break;
                                }
                            }
                            final PromotionDb.Obj finalPromoObj = promo_start_date > 0 ? promoObj : null;
                            //Check lan nua do dai chuoi ki tu
                            if ("".equalsIgnoreCase(agent) || finalPromoObj == null) {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                JsonObject jsonReply = new JsonObject();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                message.reply(jsonReply);
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh " + binhTanPromotionObj.joExtra.toString());
                                log.writeLog();
                                return;
                            } else if (binhTanPromotionObj.program.equalsIgnoreCase(StringConstUtil.BinhTanPromotion.PROGRAM)
                                    && (currentTime < promo_start_date || currentTime > promo_end_date)) {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Chua bat dau chay chuong trinh " + finalPromoObj.NAME);
                                log.writeLog();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Chua bat dau chay chuong trinh.");
                                message.reply(jsonReply);
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Chua bat dau chay chuong trinh. " + binhTanPromotionObj.joExtra.toString());
                                return;
                            } else if ("".equalsIgnoreCase(binhTanPromotionObj.phoneNumber) || DataUtil.strToLong(binhTanPromotionObj.phoneNumber) <= 0 || !Misc.checkNumber(DataUtil.strToInt(binhTanPromotionObj.phoneNumber))) {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "So dien thoai la so dien thoai khong co that.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "So dien thoai khong hop le");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if (StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4.equalsIgnoreCase(binhTanPromotionObj.program))
                            {
                                final String storeNumber = binhTanPromotionObj.joExtra.getString(StringConstUtil.STORE_NUMBER, "");
                                log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, "Chuong trinh Binh Tan danh cho DGD Binh Tan");
                                log.add("DESC" + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, "KIem tra thoi gian giao dich co hop le ko");
                                phoneCheckDb.findOne(storeNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, new Handler<PhoneCheckDb.Obj>() {
                                    @Override
                                    public void handle(final PhoneCheckDb.Obj agentObj) {
                                        if(agentObj == null) {
                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Diem giao dich khong thuoc danh sach activation");
                                            return;
                                        }
                                        long startDateTime = convertToStartTimeToday(finalPromoObj.OFF_TIME_FROM);
                                        long endDateTime = convertToEndTimeToday(finalPromoObj.OFF_TIME_TO);

                                        if(agentObj.off_time_from != null && agentObj.off_time_from.equalsIgnoreCase(StringConstUtil.EMPTY)) {
                                            startDateTime = DataUtil.strToLong(agentObj.off_time_from);
                                        }

                                        if(agentObj.off_time_to != null && agentObj.off_time_to.equalsIgnoreCase(StringConstUtil.EMPTY)) {
                                            endDateTime = DataUtil.strToLong(agentObj.off_time_to);
                                        }
                                        final long fStartDateTime = startDateTime;
                                        final long fEndDateTime = endDateTime;
                                        if (System.currentTimeMillis() > startDateTime && System.currentTimeMillis() < endDateTime) {
                                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, "Trong thoi gian tra thuong");
                                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, "Kiem tra giao dich va ghi nhan");
                                            logger.info("Time của chuong trinh G4: from " + startDateTime + " to " + endDateTime);
                                            //send noti cho user nhac nho nap tien dien thoai truoc ngay T+14
                                            /*********************************************************************************/
                                            acquireBinhTanUserPromotionDb.findOne(binhTanPromotionObj.phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                                                @Override
                                                public void handle(AcquireBinhTanUserPromotionDb.Obj obj) {
                                                    if((obj == null || obj.time_group_3 == 0 || obj.noti_times == 0) && finalPromoObj.STATUS) {
                                                        logger.info("Send noti thong bao nap tien truoc ngay T G4");
                                                        JsonObject joObject = createJsonNotification(binhTanPromotionObj.phoneNumber,
                                                                PromoContentNotification.NOTI_BINH_TAN_CASHIN_T_DAY.replace("%date%", Misc.dateVNFormat(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000)),
                                                                PromoContentNotification.NOTI_BINH_TAN_CASHIN_T_DAY_TITLE);
                                                        Misc.sendStandardNoti(vertx, joObject);
                                                        updateNotiTime(binhTanPromotionObj.phoneNumber, 1);
                                                    } else if((obj == null || obj.time_group_3 == 0) && !finalPromoObj.STATUS) {
                                                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Khong cho nhan thuong tu dien gia dich ngoai activation !!!! " + binhTanPromotionObj.phoneNumber);
                                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong cho nhan thuong tu dien gia dich ngoai activation !!!! " + binhTanPromotionObj.phoneNumber);
                                                        log.writeLog();
                                                        return;
                                                    }
                                                    checkDGDInfoAndRecordTrans(fStartDateTime, fEndDateTime, finalPromoObj, binhTanPromotionObj, log, storeNumber, agentObj);
                                                }
                                            });
                                            /*********************************************************************************/

                                        } else {
                                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, "Ngoai thoi gian tra thuong Binh Tan");
                                            log.writeLog();
                                            //todo SendNotification to DGD.
                                            JsonObject joObject = createJsonNotification(storeNumber, PromoContentNotification.NOTI_BINH_TAN_OVER_TIME, PromoContentNotification.NOTI_BINH_TAN_TITLE);
                                            Misc.sendStandardNoti(vertx, joObject);
                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "So dien thoai " + binhTanPromotionObj.phoneNumber + " KHONG duoc tham gia khuyen mai binh tan "
                                                    + " vi tham gia nap tien ngoai thoi gian quy dinh tu DGD " + storeNumber);
                                        }
                                    }
                                });
                                return;
                            }
                            else if (StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5.equalsIgnoreCase(binhTanPromotionObj.program))
                            {
                                final String storeNumber = binhTanPromotionObj.joExtra.getString(StringConstUtil.STORE_NUMBER, "");
                                log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5, "Chuong trinh Binh Tan danh cho DGD Binh Tan");
                                log.add("DESC" + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5, "KIem tra thoi gian giao dich co hop le ko");
                                phoneCheckDb.findOne(storeNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, new Handler<PhoneCheckDb.Obj>() {
                                    @Override
                                    public void handle(final PhoneCheckDb.Obj agentObj) {
                                        if(agentObj == null) {
                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Diem giao dich khong thuoc danh sach activation");
                                            return;
                                        }
                                        long startDateTime = convertToStartTimeToday(finalPromoObj.OFF_TIME_FROM);
                                        long endDateTime = convertToEndTimeToday(finalPromoObj.OFF_TIME_TO);

                                        if(agentObj.off_time_from != null && agentObj.off_time_from.equalsIgnoreCase(StringConstUtil.EMPTY)) {
                                            startDateTime = DataUtil.strToLong(agentObj.off_time_from);
                                        }

                                        if(agentObj.off_time_to != null && agentObj.off_time_to.equalsIgnoreCase(StringConstUtil.EMPTY)) {
                                            endDateTime = DataUtil.strToLong(agentObj.off_time_to);
                                        }

                                        if (System.currentTimeMillis() > startDateTime && System.currentTimeMillis() < endDateTime) {
                                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5, "Trong thoi gian tra thuong");
                                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5, "Kiem tra giao dich va ghi nhan");
                                            //send noti cho user nhac nho nap tien dien thoai truoc ngay T+14
                                            /*********************************************************************************/
                                            acquireBinhTanUserPromotionDb.findOne(binhTanPromotionObj.phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                                                @Override
                                                public void handle(AcquireBinhTanUserPromotionDb.Obj obj) {
                                                    if((obj == null || obj.time_group_3 == 0) || obj.noti_times == 0) {
                                                        logger.info("Send noti thong bao nap tien truoc ngay T G5");
                                                        JsonObject joObject = createJsonNotification(binhTanPromotionObj.phoneNumber,
                                                                PromoContentNotification.NOTI_BINH_TAN_CASHIN_T_DAY.replace("%date%", Misc.dateVNFormat(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000)),
                                                                PromoContentNotification.NOTI_BINH_TAN_CASHIN_T_DAY_TITLE);
                                                        Misc.sendStandardNoti(vertx, joObject);

                                                        updateNotiTime(binhTanPromotionObj.phoneNumber, 1);
                                                    }
                                                }
                                            });
                                            /*********************************************************************************/
                                            checkDGDInfoAndRecordTrans(startDateTime, endDateTime, finalPromoObj, binhTanPromotionObj, log, storeNumber, agentObj);
                                        } else {
                                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5, "Ngoai thoi gian tra thuong Binh Tan");
                                            log.writeLog();
                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "So dien thoai " + binhTanPromotionObj.phoneNumber + " KHONG duoc tham gia khuyen mai binh tan "
                                                    + " vi tham gia nap tien ngoai thoi gian quy dinh tu DGD " + storeNumber);
                                        }
                                    }
                                });
                                return;
                            } else {
                                //Kiem tra xem da tra thuong cho em nay chua
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Kiem tra xem login hay billpay");
                                if(StringConstUtil.BinhTanPromotion.PROGRAM.equalsIgnoreCase(binhTanPromotionObj.program))
                                {
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thuc thi group 1");
                                    if(finalPromoObj.ENABLE_PHASE2)
                                    {
                                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Chay all user tham gia khuyen mai");
                                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "recordRegisteredUser");
                                        recordRegisteredUser(message, binhTanPromotionObj.joExtra, binhTanPromotionObj.phoneNumber, log);
                                        log.writeLog();
                                        return;
                                    }

                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Kiem tra xem user nay duoc tham gia khuyen mai ko");
                                    checkRegisterdUser(binhTanPromotionObj.joExtra, binhTanPromotionObj.phoneNumber, binhTanPromotionObj.program, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean resultResponse) {
                                            if(resultResponse)
                                            {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "So dien thoai " + binhTanPromotionObj.phoneNumber + " duoc tham gia khuyen mai binh tan");
                                                recordRegisteredUser(message, binhTanPromotionObj.joExtra, binhTanPromotionObj.phoneNumber, log);
                                                log.writeLog();
                                                return;
                                            }
                                            else {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "So dien thoai " + binhTanPromotionObj.phoneNumber + " KHONG duoc tham gia khuyen mai binh tan");
                                                log.writeLog();
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "So dien thoai " + binhTanPromotionObj.phoneNumber + " KHONG duoc tham gia khuyen mai binh tan" + binhTanPromotionObj.joExtra.toString());
                                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000));
                                                return;
                                            }
                                        }
                                    });
                                } else if(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2.equalsIgnoreCase(binhTanPromotionObj.program)) {
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thuc thi tra thuong group 2");

                                    vertx.setTimer(1000L, new Handler<Long>() {
                                        @Override
                                        public void handle(Long timer) {
                                            vertx.cancelTimer(timer);
                                            if(binhTanPromotionObj.program.equalsIgnoreCase(hashMap.remove(binhTanPromotionObj.phoneNumber)))
                                            {
                                                long time = getRandomTime(finalPromoObj.MIN_TIMES, finalPromoObj.MAX_TIMES);
                                                time = time < 1 ? 1 : time;
                                                vertx.setTimer(time * 1000L, new Handler<Long>() {
                                                    @Override
                                                    public void handle(final Long randomTimer) {
                                                        phonesDb.getPhoneObjInfo(DataUtil.strToInt(binhTanPromotionObj.phoneNumber), new Handler<PhonesDb.Obj>() {
                                                            @Override
                                                            public void handle(final PhonesDb.Obj phoneObj) {
                                                                if(phoneObj == null)
                                                                {
                                                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "phoneObj luc tra group 2 = null");
                                                                    log.writeLog();
                                                                    message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000));
                                                                    return;
                                                                }
                                                                acquireBinhTanUserPromotionDb.findOne(binhTanPromotionObj.phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                                                                    @Override
                                                                    public void handle(final AcquireBinhTanUserPromotionDb.Obj acquireBinhTanObj) {
                                                                        if(acquireBinhTanObj == null)
                                                                        {
                                                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "phoneObj luc tra group 2 = null");
                                                                            log.writeLog();
                                                                            message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000));
                                                                            return;
                                                                        }
                                                                        else if(!acquireBinhTanObj.imei.equalsIgnoreCase(phoneObj.lastImei))
                                                                        {
                                                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "thông tin imei ghi nhận với thông tin imei hiện tại của máy đang khác nhau, trảm luôn " + acquireBinhTanObj.imei + " vs " + phoneObj.lastImei);
                                                                            log.writeLog();
                                                                            JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                                                                            acquireBinhTanUserPromotionDb.updatePartial(acquireBinhTanObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                                @Override
                                                                                public void handle(Boolean event) {
                                                                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "thông tin imei ghi nhận với thông tin imei hiện tại của máy đang khác nhau, trảm luôn " + acquireBinhTanObj.imei + " vs " + phoneObj.lastImei);
                                                                                }
                                                                            });
                                                                            message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000));
                                                                            return;
                                                                        }
                                                                        else if(acquireBinhTanObj.numberOfOtp > 1)
                                                                        {
                                                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Số điện thoại này đã bị lock vì get OTP 2 lần");
                                                                            log.writeLog();
                                                                            JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                                                                            acquireBinhTanUserPromotionDb.updatePartial(acquireBinhTanObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                                @Override
                                                                                public void handle(Boolean event) {
                                                                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Số điện thoại này đã bị lock vì get OTP 2 lần");
                                                                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000, "Thiết bị này đã nhận mã xác thực (OTP) trên 1 lần nên không được tham gia tiếp chương trình khuyến mãi");
                                                                                }
                                                                            });
                                                                            message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000));
                                                                            return;
                                                                        }
                                                                        else if(!phoneObj.isAgent && phoneObj.appCode < 76 && StringConstUtil.ANDROID_OS.equalsIgnoreCase(phoneObj.phoneOs))
                                                                        {
                                                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Số điện thoại này đã login vào app android cũ => không trả thưởng nữa => nghi vấn cheat.");
                                                                                log.writeLog();
                                                                                JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                                                                                acquireBinhTanUserPromotionDb.updatePartial(acquireBinhTanObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                                @Override
                                                                                public void handle(Boolean event) {
                                                                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Số điện thoại này đã login vào app android cũ => không trả thưởng nữa => nghi vấn cheat");
                                                                                    }
                                                                                });
                                                                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000));
                                                                                return;
                                                                        }
                                                                        else if(!acquireBinhTanObj.hasBonus)
                                                                        {
                                                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Số điện thoại này random khong duoc tra thuong.");
                                                                            log.writeLog();
                                                                            JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                                                                            acquireBinhTanUserPromotionDb.updatePartial(acquireBinhTanObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                                @Override
                                                                                public void handle(Boolean event) {
                                                                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Số điện thoại này random khong duoc tra thuong.");
                                                                                }
                                                                            });
                                                                            message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000));
                                                                            return;
                                                                        }
                                                                        else {
                                                                            executeGroup2Promotion(message, binhTanPromotionObj.phoneNumber, binhTanPromotionObj.program, binhTanPromotionObj.joExtra, finalPromoObj, log);
                                                                            vertx.cancelTimer(randomTimer);
                                                                            return;
                                                                        }
                                                                    }
                                                                });

                                                            }
                                                        });

                                                    }
                                                });
                                                return;
                                            }
                                            else {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "hashmap is not equal program");
                                                log.writeLog();
                                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                jsonReply.putString(StringConstUtil.DESCRIPTION, "hashmap is not equal program");
                                                message.reply(jsonReply);
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "So dien thoai " + binhTanPromotionObj.phoneNumber + " gui nhieu lenh thuc thi nen khong tra khuyen mai " + binhTanPromotionObj.joExtra.toString());
                                                return;
                                            }
                                        }
                                    });
                                   return;
                                }
                                else if(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3.equalsIgnoreCase(binhTanPromotionObj.program))
                                {
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thuc thi tra thuong group 3");
                                    vertx.setTimer(1000L, new Handler<Long>() {
                                        @Override
                                        public void handle(Long timer) {
                                            vertx.cancelTimer(timer);
                                            if(binhTanPromotionObj.program.equalsIgnoreCase(hashMap.remove(binhTanPromotionObj.phoneNumber)))
                                            {
                                                //todo check cheating server.
                                                final double maxScoreServer = DataUtil.stringToUNumber(finalPromoObj.ADJUST_PIN, 1);
                                                log.add("maxScoreServer", maxScoreServer);
                                                if(maxScoreServer > 0.0)
                                                {
                                                    log.add("GREAT!!! desc", "maxScoreServer is greater than 0");
                                                    JsonObject joCheckCheat = new JsonObject()
                                                            .putString(StringConstUtil.MOMO_TOOL.PHONE_NUMBER, binhTanPromotionObj.phoneNumber)
                                                            .putString(StringConstUtil.PATH, "/checkScore");
                                                    vertx.eventBus().send(AppConstant.CHECK_CHEATING_PROMOTION_BUS_ADDRESS, joCheckCheat, new Handler<Message<JsonObject>>() {
                                                        @Override
                                                        public void handle(Message<JsonObject> msgResponse) {
                                                            JsonObject joResponse = msgResponse.body();
                                                            JsonObject joData = joResponse.getObject(StringConstUtil.PromotionField.RESULT, new JsonObject());
                                                            Number maxScore = joData.getNumber("max_score");
                                                            final double checkedMaxScore = getMaxScore(joData, binhTanPromotionObj.phoneNumber, maxScoreServer);
                                                            final JsonObject joObjDup = getMaxScoreJsonObject(joData, binhTanPromotionObj.phoneNumber, maxScoreServer);
                                                            if(maxScore != null)
                                                            {
                                                                log.add("desc", "maxScore is not null");
                                                                if (checkedMaxScore > maxScoreServer)
                                                                {
                                                                    log.add("WARNING desc", "maxscore greater than score server");
                                                                    log.writeLog();
                                                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000,
                                                                            "thiet bi cua sdt " + binhTanPromotionObj.phoneNumber + " da duoc tra thuong cho so dien thoai khac " + joObjDup.getString("number", ""));
                                                                }
                                                                else {
                                                                    log.add("desc", "less than maxScoreServer");
                                                                    executeGroup3Promotion(binhTanPromotionObj, message, binhTanPromotionObj.phoneNumber, binhTanPromotionObj.program, binhTanPromotionObj.joExtra, finalPromoObj, log);
                                                                }
                                                            }
                                                            else {
                                                                executeGroup3Promotion(binhTanPromotionObj, message, binhTanPromotionObj.phoneNumber, binhTanPromotionObj.program, binhTanPromotionObj.joExtra, finalPromoObj, log);
                                                            }
                                                            CheatInfoDb.Obj cheatInfoObj = new CheatInfoDb.Obj();
                                                            cheatInfoObj.phoneNumber = binhTanPromotionObj.phoneNumber;
                                                            cheatInfoObj.time = System.currentTimeMillis();
                                                            cheatInfoObj.info = joResponse.toString();
                                                            cheatInfoDb.insert(cheatInfoObj, new Handler<Integer>() {
                                                                @Override
                                                                public void handle(Integer event) {

                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                                else{
                                                    log.add("WARNING desc", "MAX SCORE CONFIG IS NOT GREATER THAN 0");
                                                    executeGroup3Promotion(binhTanPromotionObj, message, binhTanPromotionObj.phoneNumber, binhTanPromotionObj.program, binhTanPromotionObj.joExtra, finalPromoObj, log);
                                                }
                                                return;
                                            }
                                            else {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "hashmap is not equal program");
                                                log.writeLog();
                                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                jsonReply.putString(StringConstUtil.DESCRIPTION, "hashmap is not equal program");
                                                message.reply(jsonReply);
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "So dien thoai " + binhTanPromotionObj.phoneNumber + " gui nhieu lenh thuc thi nen khong tra khuyen mai " + binhTanPromotionObj.joExtra.toString());
                                                return;
                                            }
                                        }
                                    });
                                    return;
                                }
                                else {
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong thuoc nhom nao het .... " + binhTanPromotionObj.phoneNumber);
                                    log.writeLog();
                                    return;
                                }
                            }
                        }
                    }
                });
            }
        };

        vertx.eventBus().registerLocalHandler(AppConstant.BINHTAN_PROMOTION_BUS_ADDRESS, myHandler);
    }

    private double getMaxScore(JsonObject joData, String number, double defaultValue) {
        double maxScore = 0.0;

        JsonArray jarrData = joData.getArray("data", new JsonArray());
        if (jarrData.size() > 0) {
            for (int i = 0; i < jarrData.size(); i++) {
                JsonObject joReceiveData = (JsonObject) jarrData.get(i);
                if (!number.equalsIgnoreCase(joReceiveData.getString("number", "")) && joReceiveData.getNumber("score") != null && joReceiveData.getNumber("score").doubleValue() > defaultValue) {
                    maxScore = joReceiveData.getNumber("score").doubleValue();
                    break;
                }
            }
        }

        return maxScore;
    }

    private JsonObject getMaxScoreJsonObject(JsonObject joData, String number, double defaultValue) {
        double maxScore = 0.0;
        JsonObject joDupObj = new JsonObject();
        JsonArray jarrData = joData.getArray("data", new JsonArray());
        if (jarrData.size() > 0) {
            for (int i = 0; i < jarrData.size(); i++) {
                JsonObject joReceiveData = (JsonObject) jarrData.get(i);
                if (!number.equalsIgnoreCase(joReceiveData.getString("number", "")) && joReceiveData.getNumber("score") != null && joReceiveData.getNumber("score").doubleValue() > defaultValue) {
                    maxScore = joReceiveData.getNumber("score").doubleValue();
                    joDupObj.putNumber("score", maxScore);
                    joDupObj.putString("number", joReceiveData.getString("number", ""));
                    break;
                }
            }
        }

        return joDupObj;
    }

    private void remindAndGiveBonus(final long checkInfoDgdTime) {
        final Calendar calendar = Calendar.getInstance();

        if(!isUAT)
        {
            calendar.set(Calendar.HOUR_OF_DAY, 21);
            calendar.set(Calendar.MINUTE, 00);
            calendar.set(Calendar.SECOND, 00);

        }
        long delayBonusTime = calendar.getTimeInMillis() - System.currentTimeMillis() < 1 ? 1 : calendar.getTimeInMillis() - System.currentTimeMillis();
        final long timeGivingBonus = isUAT ? 1000L * 60 * 3 : 1000L * 60 * 60 * 24;
        vertx.setTimer(delayBonusTime, new Handler<Long>() {
            @Override
            public void handle(Long delayBNTime) {
                //Kiem tra thong tin diem giao dich da duoc cho phep hoat dong hay chua
                logger.info("AUTO GIVING BONUS 10k " + StringConstUtil.BinhTanPromotion.PROGRAM + " time: " + System.currentTimeMillis());
                vertx.setPeriodic(timeGivingBonus, new Handler<Long>() {
                    @Override
                    public void handle(final Long checkInfoTime) {
                        logger.info("AUTO GIVING BONUS 10k " + StringConstUtil.BinhTanPromotion.PROGRAM + " time: " + System.currentTimeMillis());
                        giveBonusForCustomerAuto();
                    }
                });
                giveBonusForCustomerAuto();
                vertx.cancelTimer(delayBNTime);
            }
        });

        final Calendar calendar_rollback = Calendar.getInstance();
        if(!isUAT)
        {
            calendar_rollback.set(Calendar.HOUR_OF_DAY, 23);
            calendar_rollback.set(Calendar.MINUTE, 00);
            calendar_rollback.set(Calendar.SECOND, 00);
        }
        final long timeRollback = isUAT ? 1000L * 60 * 5 : 1000L * 60 * 60 * 24;
        final long delayRollBackTime = calendar_rollback.getTimeInMillis() - System.currentTimeMillis() < 1 ? 1 : calendar_rollback.getTimeInMillis() - System.currentTimeMillis();
        vertx.setTimer(delayRollBackTime, new Handler<Long>() {
            @Override
            public void handle(Long delayRBTime) {
                //Kiem tra thong tin thu hoi nhom 2
                logger.info("AUTO ROLLBACK BONUS 10k " + StringConstUtil.BinhTanPromotion.PROGRAM + " time: " + System.currentTimeMillis());
                vertx.setPeriodic(timeRollback, new Handler<Long>() {
                    @Override
                    public void handle(final Long checkInfoTime) {
                        logger.info("AUTO ROLLBACK BONUS 10k " + StringConstUtil.BinhTanPromotion.PROGRAM + " time: " + System.currentTimeMillis());
                        rollBackCustomerMoneyAuto();
                    }
                });
                rollBackCustomerMoneyAuto();
                vertx.cancelTimer(delayRBTime);
            }
        });


        final Calendar calendar_remind = Calendar.getInstance();
        if(!isUAT)
        {
            calendar_remind.set(Calendar.HOUR_OF_DAY, 20);
            calendar_remind.set(Calendar.MINUTE, 00);
            calendar_remind.set(Calendar.SECOND, 00);
        }
        final long timeRemind = isUAT ? 1000L * 60 * 7 : 1000L * 60 * 60 * 24;
        final long delayRemindTime = calendar_remind.getTimeInMillis() - System.currentTimeMillis() < 1 ? 1 : calendar_remind.getTimeInMillis() - System.currentTimeMillis();
        vertx.setTimer(delayRemindTime, new Handler<Long>() {
            @Override
            public void handle(Long RMTime) {
                //Kiem tra thong tin thu hoi nhom 2
                vertx.setPeriodic(timeRemind, new Handler<Long>() {
                    @Override
                    public void handle(final Long checkInfoTime) {
                        notiRemindGroup3();
                    }
                });
                notiRemindGroup3();
                vertx.cancelTimer(RMTime);
            }
        });

        vertx.setPeriodic(1000L * 60 * 60, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                logger.info("giveBackMoneyForUser");
                giveBackMoneyForUser();
            }
        });
        logger.info("giveBackMoneyForUser lan 1 sau restart");
        giveBackMoneyForUser();
//        final Calendar calendar_updateGroup3 = Calendar.getInstance();
//        if(!isUAT)
//        {
//            calendar_remind.set(Calendar.HOUR_OF_DAY, 22);
//            calendar_remind.set(Calendar.MINUTE, 00);
//            calendar_remind.set(Calendar.SECOND, 00);
//        }
//        final long timeUpdate = isUAT ? 1000L * 60 * 10 : 1000L * 60 * 60 * 24;
//        final long delayUpdateTime = calendar_updateGroup3.getTimeInMillis() - System.currentTimeMillis() < 1 ? 1 : calendar_updateGroup3.getTimeInMillis() - System.currentTimeMillis();
//        vertx.setTimer(delayUpdateTime, new Handler<Long>() {
//            @Override
//            public void handle(Long UDTime) {
//                //Kiem tra thong tin thu hoi nhom 2
//                vertx.setPeriodic(timeUpdate, new Handler<Long>() {
//                    @Override
//                    public void handle(final Long checkInfoTime) {
//                        updateEndGroup3();
//                    }
//                });
//                updateEndGroup3();
//                vertx.cancelTimer(UDTime);
//            }
//        });
    }

    private void giveBackMoneyForUser() {
        JsonObject joFilter =new JsonObject();
        JsonObject joGroup = new JsonObject().putNumber(colName.AcquireBinhTanUserPromotionCol.GROUP, 1);
        JsonObject time = new JsonObject().putObject(colName.AcquireBinhTanUserPromotionCol.TIME, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - 1000L * 60 * 60 * 16));
        JsonObject extraKey = new JsonObject().putObject(colName.AcquireBinhTanUserPromotionCol.EXTRA_KEY, new JsonObject().putString(MongoKeyWords.NOT_EQUAL, ""));
        JsonObject lockStatus = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, true);
        JsonObject numberOfOtp = new JsonObject().putNumber(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_OTP, 1);
        JsonObject isLocked = new JsonObject().putObject(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, new JsonObject().putBoolean(MongoKeyWords.NOT_EQUAL, true));
        JsonArray jarrAnd = new JsonArray();
        jarrAnd.add(joGroup);
        jarrAnd.add(time);
        jarrAnd.add(extraKey);
        jarrAnd.add(lockStatus);
        jarrAnd.add(numberOfOtp);
        jarrAnd.add(isLocked);
        joFilter.putArray(MongoKeyWords.AND_$, jarrAnd);
        logger.info("log search tra bu truong hop user bi lock qua lau group 1 " +  joFilter.toString());
        acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<AcquireBinhTanUserPromotionDb.Obj> listAcquireObjs) {
                giveBonusForCustomerGroup2Auto(listAcquireObjs);
            }
        });
    }

    private void giveBonusForCustomerAuto() {
        //Kiem tra chuong trinh khuyen mai con dang chay khong
        getPromotion(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonResponse) {
                int error = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                final JsonObject joPromo = jsonResponse.getObject(StringConstUtil.PROMOTION, new JsonObject());
                if(error == 0)
                {
                    //todo lay thog tin de tra qua 10k tu dong cho user.
                    JsonObject joFilter = new JsonObject();
                    joFilter.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, false);
                    joFilter.putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, false);
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 1);
                    long startTime = calendar.getTimeInMillis();
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 999);
                    long endTime = calendar.getTimeInMillis();
                    JsonArray jarrAnd = new JsonArray();
                    JsonObject joStartTime = new JsonObject();
                    joStartTime.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime);

                    JsonObject jStartTime = new JsonObject();
                    jStartTime.putObject(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_BONUS, joStartTime);

                    jarrAnd.add(jStartTime);

                    JsonObject joEndTime = new JsonObject();
                    joEndTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime);

                    JsonObject jEndTime = new JsonObject();
                    jEndTime.putObject(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_BONUS, joEndTime);

                    jarrAnd.add(jEndTime);
                    joFilter.putArray(MongoKeyWords.AND_$, jarrAnd);
                    logger.info("query tra 10k cho user " + joFilter.toString());
                    acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
                        @Override
                        public void handle(final ArrayList<AcquireBinhTanUserPromotionDb.Obj> listAcquireObjs) {
                            final AtomicInteger listItems = new AtomicInteger(listAcquireObjs.size());
                            final Common.BuildLog log = new Common.BuildLog(logger);
                            final PromotionDb.Obj promoProgram = new PromotionDb.Obj(joPromo);
                            vertx.setPeriodic(250L, new Handler<Long>() {
                                @Override
                                public void handle(Long timer) {
                                    int position = listItems.decrementAndGet();
                                    if(position < 0)
                                    {
                                        logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " da tra 10k xong");
                                        vertx.cancelTimer(timer);
                                    }
                                    else {
                                        logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " tra thuong cho so " + listAcquireObjs.get(position).phoneNumber);
                                        JsonObject joExtra = new JsonObject();
                                        log.setPhoneNumber(listAcquireObjs.get(position).phoneNumber);
                                        joExtra.putObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, listAcquireObjs.get(position).toJson());
                                        executeGroup2Promotion(null, listAcquireObjs.get(position).phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2
                                                , joExtra, promoProgram, log);
                                    }
                                }
                            });

                        }
                    });
                }
                else {
                    logger.info(jsonResponse.getString(StringConstUtil.DESCRIPTION, ""));
                }
            }
        });
    }

    private void giveBonusForCustomerGroup2Auto(final ArrayList<AcquireBinhTanUserPromotionDb.Obj> listAcquireObjs) {
        //Kiem tra chuong trinh khuyen mai con dang chay khong
        getPromotion(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonResponse) {
                int error = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                final JsonObject joPromo = jsonResponse.getObject(StringConstUtil.PROMOTION, new JsonObject());
                if (error == 0) {
                    //todo lay thog tin de tra qua 10k tu dong cho user.

                    final AtomicInteger listItems = new AtomicInteger(listAcquireObjs.size());
                    final Common.BuildLog log = new Common.BuildLog(logger);
                    final PromotionDb.Obj promoProgram = new PromotionDb.Obj(joPromo);
                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long timer) {
                            int position = listItems.decrementAndGet();
                            if (position < 0) {
                                logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " da tra 10k xong");
                                vertx.cancelTimer(timer);
                            } else {
                                logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " tra thuong cho so " + listAcquireObjs.get(position).phoneNumber);
                                JsonObject joExtra = new JsonObject();
                                log.setPhoneNumber(listAcquireObjs.get(position).phoneNumber);
                                joExtra.putObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, listAcquireObjs.get(position).toJson());
                                executeGroup2Promotion(null, listAcquireObjs.get(position).phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2
                                        , joExtra, promoProgram, log);
                            }
                        }
                    });
                } else {
                    logger.info(jsonResponse.getString(StringConstUtil.DESCRIPTION, ""));
                }
            }
        });
    }

    private void rollBackCustomerMoneyAuto() {
        //Kiem tra chuong trinh khuyen mai con dang chay khong
        getPromotion(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonResponse) {
                int error = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                final JsonObject joPromo = jsonResponse.getObject(StringConstUtil.PROMOTION, new JsonObject());
                if(error == 0)
                {
                    //todo lay thog tin de tra qua 10k tu dong cho user.
                    JsonObject joFilter = new JsonObject();
                    Calendar calendar_rollback = Calendar.getInstance();
                    calendar_rollback.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_rollback.set(Calendar.MINUTE, 0);
                    calendar_rollback.set(Calendar.SECOND, 0);
                    calendar_rollback.set(Calendar.MILLISECOND, 1);
                    long startTime = calendar_rollback.getTimeInMillis() - 1000L * 60 * 60 * 24;
                    calendar_rollback.set(Calendar.HOUR_OF_DAY, 23);
                    calendar_rollback.set(Calendar.MINUTE, 59);
                    calendar_rollback.set(Calendar.SECOND, 59);
                    calendar_rollback.set(Calendar.MILLISECOND, 999);
                    long endTime = calendar_rollback.getTimeInMillis();
                    JsonArray jarrAnd = new JsonArray();
                    JsonObject joStartTime = new JsonObject();
                    joStartTime.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime);

                    JsonObject jStartTime = new JsonObject();
                    jStartTime.putObject(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_ROLLBACK, joStartTime);

                    jarrAnd.add(jStartTime);

                    JsonObject joEndTime = new JsonObject();
                    joEndTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime);

                    JsonObject jEndTime = new JsonObject();
                    jEndTime.putObject(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_ROLLBACK, joEndTime);

                    jarrAnd.add(jEndTime);

                    JsonObject group3 = new JsonObject();
                    group3.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, 0);
                    jarrAnd.add(group3);

                    joFilter.putArray(MongoKeyWords.AND_$, jarrAnd);
                    logger.info("query tra 10k cho user " + joFilter.toString());
                    acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
                        @Override
                        public void handle(final ArrayList<AcquireBinhTanUserPromotionDb.Obj> listAcquireObjs) {
                            final AtomicInteger listItems = new AtomicInteger(listAcquireObjs.size());
                            final Common.BuildLog log = new Common.BuildLog(logger);
                            final PromotionDb.Obj promoProgram = new PromotionDb.Obj(joPromo);
                            vertx.setPeriodic(500L, new Handler<Long>() {
                                @Override
                                public void handle(Long timer) {
                                    final int position = listItems.decrementAndGet();
                                    if(position < 0)
                                    {
                                        logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " da tra 10k xong");
                                        vertx.cancelTimer(timer);
                                    }
                                    else {
//                                                        JsonObject joFilter2 = new JsonObject();
//                                                        joFilter2.putString(colName.AcquireBinhTanGroup2PromotionCol.PHONE_NUMBER, listAcquireObjs.get(position).phoneNumber);
//                                                        joFilter2.putNumber(colName.AcquireBinhTanGroup2PromotionCol.IS_USED, 0);

                                        acquireBinhTanGroup2PromotionDb.findAndModifyUsedVoucher(listAcquireObjs.get(position).phoneNumber, 0, 2, new Handler<AcquireBinhTanGroup2PromotionDb.Obj>() {
                                            @Override
                                            public void handle(AcquireBinhTanGroup2PromotionDb.Obj acquireObj) {
                                                if(acquireObj != null)
                                                {
                                                    logger.info("ROLL BACK USER MONEY " + acquireObj.toJson());
                                                    rollbackMoneyUser(promoProgram.ADJUST_ACCOUNT, promoProgram.PER_TRAN_VALUE, listAcquireObjs.get(position).phoneNumber, log, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject joRep) {
                                                            int err = joRep.getInteger(StringConstUtil.ERROR, -1);
                                                            final long tranId = joRep.getInteger(StringConstUtil.TRANDB_TRAN_ID, -1);
                                                            int valueUpdate = err == 0 ? 2 : 0;
                                                            acquireBinhTanGroup2PromotionDb.findAndModifyUsedVoucher(listAcquireObjs.get(position).phoneNumber, 2, valueUpdate, new Handler<AcquireBinhTanGroup2PromotionDb.Obj>() {
                                                                @Override
                                                                public void handle(AcquireBinhTanGroup2PromotionDb.Obj info) {
                                                                    logger.info("BAN NOTI BANG BANG ");
                                                                    long amount = promoProgram.PER_TRAN_VALUE;
                                                                    String titleNoti=  promoProgram.NOTI_SMS_INVITEE;
                                                                    String bodyNoti = promoProgram.NOTI_BODY_INVITEE;
                                                                    String partnerName = promoProgram.INTRO_DATA;
                                                                    bodyNoti = bodyNoti.replaceAll("%enddate", Misc.dateVNFormatWithDot(info.time_bonus));
                                                                    fireMoneyNotiAndSendTranHist(listAcquireObjs.get(position).phoneNumber, amount, tranId, log, titleNoti, bodyNoti, bodyNoti, partnerName, -1, MomoProto.TranHisV1.TranType.M2M_VALUE);
                                                                }
                                                            });
                                                            return;
                                                        }
                                                    });
                                                }
                                            }
                                        });


//                                                        acquireBinhTanGroup2PromotionDb.searchWithFilter(joFilter2, new Handler<ArrayList<AcquireBinhTanGroup2PromotionDb.Obj>>() {
//                                                            @Override
//                                                            public void handle(ArrayList<AcquireBinhTanGroup2PromotionDb.Obj> listBonusG2) {
//                                                                if(listBonusG2.size() > 0)
//                                                                {
//                                                                    logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " tra thuong cho so " + listAcquireObjs.get(position).phoneNumber);
//                                                                    JsonObject joExtra = new JsonObject();
//                                                                    log.setPhoneNumber(listAcquireObjs.get(position).phoneNumber);
//                                                                    joExtra.putObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, listAcquireObjs.get(position).toJson());
//
//                                                                    rollbackMoneyUser(promoProgram.ADJUST_ACCOUNT, promoProgram.PER_TRAN_VALUE, listAcquireObjs.get(position).phoneNumber, log, new Handler<JsonObject>() {
//                                                                        @Override
//                                                                        public void handle(JsonObject event) {
//
//                                                                        }
//                                                                    });
//                                                                }
//                                                            }
//                                                        });

                                    }
                                }
                            });

                        }
                    });
                }
                else {
                    logger.info(jsonResponse.getString(StringConstUtil.DESCRIPTION, ""));
                }
            }
        });
    }

    /**
     * Auto Noti cho chương trình bình tân 3
     */
    private void notiRemindGroup3() {
        //Kiem tra chuong trinh khuyen mai con dang chay khong
        getPromotion(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonResponse) {
                logger.info("REMIND " + StringConstUtil.BinhTanPromotion.PROGRAM + " noti group 3");
                int error = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                final JsonObject joPromo = jsonResponse.getObject(StringConstUtil.PROMOTION, new JsonObject());
                if(error == 0)
                {
                    JsonArray jArrOR = new JsonArray();
                    JsonArray jArrBigAND = new JsonArray();
                    //todo lay thog tin de tra qua 10k tu dong cho user.
                    JsonObject joFilter = new JsonObject();
                    final Calendar calendar_remind = Calendar.getInstance();

                    /**is_locked**/
                    JsonObject jLockNull = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, null);
                    JsonObject jLockFalse = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, false);
                    JsonArray jLockArr = new JsonArray().add(jLockNull).add(jLockFalse);
                    JsonObject jLockOr = new JsonObject().putArray(MongoKeyWords.OR, jLockArr);

                    /************************************7 Day Left**********************************************/
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_remind.set(Calendar.MINUTE, 0);
                    calendar_remind.set(Calendar.SECOND, 0);
                    final long startTime7 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 7;
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 23);
                    calendar_remind.set(Calendar.MINUTE, 59);
                    calendar_remind.set(Calendar.SECOND, 59);
                    final long endTime7 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 7;
                    //Time 1
                    JsonArray jarrAnd7 = new JsonArray();
                    JsonObject joStartTime7 = new JsonObject();
                    joStartTime7.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime7);

                    JsonObject jStartTime7 = new JsonObject();
                    jStartTime7.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joStartTime7);

                    jarrAnd7.add(jStartTime7);

                    JsonObject joEndTime7 = new JsonObject();
                    joEndTime7.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime7);

                    JsonObject jEndTime7 = new JsonObject();
                    jEndTime7.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joEndTime7);

                    jarrAnd7.add(jEndTime7);

                    JsonObject joOr7D = new JsonObject().putArray(MongoKeyWords.OR, new JsonArray()
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_7_DAY, null))
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_7_DAY, false)));

                    jarrAnd7.add(joOr7D);

                    JsonObject joAnd7 = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd7);

                    /************************************14 Day Left**********************************************/
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_remind.set(Calendar.MINUTE, 0);
                    calendar_remind.set(Calendar.SECOND, 0);
                    final long startTime14 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 14;
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 23);
                    calendar_remind.set(Calendar.MINUTE, 59);
                    calendar_remind.set(Calendar.SECOND, 59);
                    final long endTime14 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 14;
                    //Time 1
                    JsonArray jarrAnd14 = new JsonArray();
                    JsonObject joStartTime = new JsonObject();
                    joStartTime.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime14);

                    JsonObject jStartTime14 = new JsonObject();
                    jStartTime14.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joStartTime);

                    jarrAnd14.add(jStartTime14);

                    JsonObject joEndTime14 = new JsonObject();
                    joEndTime14.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime14);

                    JsonObject jEndTime14 = new JsonObject();
                    jEndTime14.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joEndTime14);

                    jarrAnd14.add(jEndTime14);

                    JsonObject joOr14D = new JsonObject().putArray(MongoKeyWords.OR, new JsonArray()
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_14_DAY, null))
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_14_DAY, false)));

                    jarrAnd14.add(joOr14D);

                    JsonObject joAnd14 = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd14);

                    /************************************21 Day Left**********************************************/
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_remind.set(Calendar.MINUTE, 0);
                    calendar_remind.set(Calendar.SECOND, 0);
                    final long startTime21 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 21;
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 23);
                    calendar_remind.set(Calendar.MINUTE, 59);
                    calendar_remind.set(Calendar.SECOND, 59);
                    final long endTime21 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 21;
                    //Time 1
                    JsonArray jarrAnd21 = new JsonArray();
                    JsonObject joStartTime21 = new JsonObject();
                    joStartTime21.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime21);

                    JsonObject jStartTime21 = new JsonObject();
                    jStartTime21.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joStartTime21);

                    jarrAnd21.add(jStartTime21);

                    JsonObject joEndTime21 = new JsonObject();
                    joEndTime21.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime21);

                    JsonObject joCashINTime21 = new JsonObject();
                    joCashINTime21.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime14);
                    JsonObject jCashINTime21 = new JsonObject();
                    jCashINTime21.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_CASHIN, joCashINTime21);

                    jarrAnd21.add(jCashINTime21);

                    JsonObject jEndTime21 = new JsonObject();
                    jEndTime21.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joEndTime21);

                    jarrAnd21.add(jEndTime21);

                    JsonObject joOr21D = new JsonObject().putArray(MongoKeyWords.OR, new JsonArray()
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_21_DAY, null))
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_21_DAY, false)));

                    jarrAnd21.add(joOr21D);

                    JsonObject joAnd21 = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd21);
                    /********************************28 Day Left**************************************************/
                    //Time after 28 day
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_remind.set(Calendar.MINUTE, 0);
                    calendar_remind.set(Calendar.SECOND, 0);
                    calendar_remind.set(Calendar.MILLISECOND, 1);
                    final long startTime28 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 28;
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 23);
                    calendar_remind.set(Calendar.MINUTE, 59);
                    calendar_remind.set(Calendar.SECOND, 59);
                    calendar_remind.set(Calendar.MILLISECOND, 999);
                    final long endTime28 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 28;
                    JsonArray jarrAnd28 = new JsonArray();
                    JsonObject joStartTime28 = new JsonObject();
                    joStartTime28.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime28);

                    JsonObject jStartTime28 = new JsonObject();
                    jStartTime28.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joStartTime28);

                    jarrAnd28.add(jStartTime28);

                    JsonObject joEndTime28 = new JsonObject();
                    joEndTime28.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime28);

                    JsonObject jEndTime2 = new JsonObject();
                    jEndTime2.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joEndTime28);

                    jarrAnd28.add(jEndTime2);

                    JsonObject joOr28D = new JsonObject().putArray(MongoKeyWords.OR, new JsonArray()
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_28_DAY, null))
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_28_DAY, false)));

                    jarrAnd28.add(joOr28D);

                    JsonObject joAnd28 = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd28);

                    /************************************35 Day Left**********************************************/
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 0);
                    calendar_remind.set(Calendar.MINUTE, 0);
                    calendar_remind.set(Calendar.SECOND, 0);
                    final long startTime35 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 35;
                    calendar_remind.set(Calendar.HOUR_OF_DAY, 23);
                    calendar_remind.set(Calendar.MINUTE, 59);
                    calendar_remind.set(Calendar.SECOND, 59);
                    final long endTime35 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 35;
                    //Time 1
                    JsonArray jarrAnd35 = new JsonArray();
                    JsonObject joStartTime35 = new JsonObject();
                    joStartTime35.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime35);

                    JsonObject jStartTime35 = new JsonObject();
                    jStartTime35.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joStartTime35);

                    jarrAnd35.add(jStartTime35);

                    JsonObject joEndTime35 = new JsonObject();
                    joEndTime35.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime35);

                    JsonObject jEndTime35 = new JsonObject();
                    jEndTime35.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, joEndTime35);

                    jarrAnd35.add(jEndTime35);

                    JsonObject joCashINTime35 = new JsonObject();
                    joCashINTime35.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime28);
                    JsonObject jCashINTime35 = new JsonObject();
                    jCashINTime35.putObject(colName.AcquireBinhTanUserPromotionCol.TIME_CASHIN, joCashINTime35);

                    jarrAnd35.add(jCashINTime35);

                    JsonObject joOr35D = new JsonObject().putArray(MongoKeyWords.OR, new JsonArray()
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_35_DAY, null))
                            .add(new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_35_DAY, false)));

                    jarrAnd35.add(joOr35D);

                    JsonObject joAnd35 = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd35);
                    /************************************End check day**********************************************/

                    jArrOR.add(joAnd7);
                    jArrOR.add(joAnd14);
                    jArrOR.add(joAnd21);
                    jArrOR.add(joAnd28);
                    jArrOR.add(joAnd35);

                    JsonObject jsonOr = new JsonObject();
                    jsonOr.putArray(MongoKeyWords.OR, jArrOR);

                    jArrBigAND.add(jsonOr).add(jLockOr);

                    JsonObject joEndGroup3 = new JsonObject();
                    joEndGroup3.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, false);

                    JsonObject joEndGroup2 = new JsonObject();
                    joEndGroup3.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, true);
                    jArrBigAND.add(joEndGroup3);
                    jArrBigAND.add(joEndGroup2);

                    joFilter.putArray(MongoKeyWords.AND_$, jArrBigAND);
                    logger.info("query remind cashin " + joFilter.toString());
                    acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
                        @Override
                        public void handle(final ArrayList<AcquireBinhTanUserPromotionDb.Obj> listAcquireObjs) {
                            final AtomicInteger listItems = new AtomicInteger(listAcquireObjs.size());
                            final Common.BuildLog log = new Common.BuildLog(logger);
                            final PromotionDb.Obj promoProgram = new PromotionDb.Obj(joPromo);
                            vertx.setPeriodic(500L, new Handler<Long>() {
                                @Override
                                public void handle(Long timer) {
                                    final int position = listItems.decrementAndGet();
                                    if(position < 0)
                                    {
                                        logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " done noti remind");
                                        vertx.cancelTimer(timer);
                                    }
                                    else {
                                        final AcquireBinhTanUserPromotionDb.Obj checkedPromotion = listAcquireObjs.get(position);
                                        JsonObject joNumber = new JsonObject().putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, listAcquireObjs.get(position).phoneNumber);
                                        acquireBinhTanGroup3PromotionDb.searchWithFilter(joNumber, new Handler<ArrayList<AcquireBinhTanGroup3PromotionDb.Obj>>() {
                                            @Override
                                            public void handle(ArrayList<AcquireBinhTanGroup3PromotionDb.Obj> list) {
                                                if(checkedPromotion.time_group_3 > startTime7 && checkedPromotion.time_group_3 < endTime7 && list.size() < 1) {
                                                    logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " noti remind 7");

                                                    long remindEndTime = checkedPromotion.time_group_3 + 1000L * 60 * 60 * 24 * 13;

                                                    JsonObject joNoti = new JsonObject();
                                                    String titleNoti = PromoContentNotification.NOTI_BINH_TAN_UNTOPUP_T7_DAY_TITLE;
                                                    String bodyNoti = PromoContentNotification.NOTI_BINH_TAN_UNTOPUP_T7_DAY.replaceAll("%todate%", Misc.dateVNFormatWithDot(remindEndTime));
                                                    String phoneNumber = listAcquireObjs.get(position).phoneNumber;
                                                    joNoti.putString(StringConstUtil.StandardNoti.CAPTION, titleNoti);
                                                    joNoti.putString(StringConstUtil.StandardNoti.BODY, bodyNoti);
                                                    joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phoneNumber);
                                                    joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, 0L);
                                                    Misc.sendStandardNoti(vertx, joNoti);
                                                    updateNotiTime(phoneNumber, 7);
                                                }

                                                if(checkedPromotion.time_group_3 > startTime14 && checkedPromotion.time_group_3 < endTime14 && list.size() < 2) {
                                                    long cashinTime = checkedPromotion.time_cashin;
                                                    if(cashinTime > startTime14) {
                                                        logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " noti remind 14");
                                                        String titleNoti = PromoContentNotification.NOTI_BINH_TAN_PROMO_TITLE_30;
                                                        String bodyNoti = promoProgram.NOTI_BODY_INVITER;
                                                        String phoneNumber = listAcquireObjs.get(position).phoneNumber;
                                                        JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/nap10tang50/htmlpage/CachNhanThuong.html");
                                                        Misc.sendRedirectNoti(vertx, joNoti);
                                                        updateNotiTime(phoneNumber, 14);
                                                    }
                                                }

                                                if(checkedPromotion.time_group_3 > startTime21 && checkedPromotion.time_group_3 < endTime21 && list.size() < 2) {
                                                    long remindEndTime = checkedPromotion.time_group_3 + 1000L * 60 * 60 * 24 * 27;

                                                    String titleNoti = PromoContentNotification.NOTI_BINH_TAN_UNCASHIN_T2135_DAY_TITLE;
                                                    String bodyNoti = PromoContentNotification.NOTI_BINH_TAN_UNCASHIN_T2135_DAY.replaceAll("%todate%", Misc.dateVNFormatWithDot(remindEndTime));
                                                    String phoneNumber = listAcquireObjs.get(position).phoneNumber;
                                                    JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/nap10tang50/htmlpage/CachNhanThuong.html");
                                                    Misc.sendRedirectNoti(vertx, joNoti);
                                                    updateNotiTime(phoneNumber, 21);
                                                }

                                                if(checkedPromotion.time_group_3 > startTime28 && checkedPromotion.time_group_3 < endTime28  && list.size() < 3) {
                                                    long cashinTime = checkedPromotion.time_cashin;
                                                    if(cashinTime > startTime28) {
                                                        logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " noti remind 28");
                                                        String titleNoti = PromoContentNotification.NOTI_BINH_TAN_PROMO_TITLE_30;
                                                        String bodyNoti = promoProgram.NOTI_BODY_INVITER;
                                                        String phoneNumber = listAcquireObjs.get(position).phoneNumber;
                                                        JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/nap10tang50/htmlpage/CachNhanThuong.html");
                                                        Misc.sendRedirectNoti(vertx, joNoti);
                                                        updateNotiTime(phoneNumber, 28);
                                                    }
                                                }

                                                if(checkedPromotion.time_group_3 > startTime35 && checkedPromotion.time_group_3 < endTime35  && list.size() < 3) {
                                                    long remindEndTime = checkedPromotion.time_group_3 + 1000L * 60 * 60 * 24 * 41;

                                                    logger.info("desc " + StringConstUtil.BinhTanPromotion.PROGRAM + " " + " noti remind 35");
                                                    String titleNoti = PromoContentNotification.NOTI_BINH_TAN_UNCASHIN_T2135_DAY_TITLE;
                                                    String bodyNoti = PromoContentNotification.NOTI_BINH_TAN_UNCASHIN_T2135_DAY.replaceAll("%todate%", Misc.dateVNFormatWithDot(remindEndTime));
                                                    String phoneNumber = listAcquireObjs.get(position).phoneNumber;
                                                    JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/nap10tang50/htmlpage/CachNhanThuong.html");
                                                    Misc.sendRedirectNoti(vertx, joNoti);
                                                    updateNotiTime(phoneNumber, 35);
                                                }
                                            }
                                        });
                                    }
                                }
                            });

                        }
                    });

                } else {
                    logger.info(jsonResponse.getString(StringConstUtil.DESCRIPTION, ""));
                }
            }
        });
    }

    private void updateEndGroup3() {
        //Kiem tra chuong trinh khuyen mai con dang chay khong
        getPromotion(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonResponse) {
                logger.info("Update End Group 3 " + StringConstUtil.BinhTanPromotion.PROGRAM + " updateEndGroup3");
                int error = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                final JsonObject joPromo = jsonResponse.getObject(StringConstUtil.PROMOTION, new JsonObject());
                final PromotionDb.Obj promoObj = new PromotionDb.Obj(joPromo);
                if(error == 0)
                {
                    Calendar endDate = Calendar.getInstance();
                    endDate.set(Calendar.HOUR_OF_DAY, 23);
                    endDate.set(Calendar.MINUTE, 59);
                    endDate.set(Calendar.SECOND, 59);
                    long endDateL = endDate.getTimeInMillis();

                    final JsonObject joFilter = new JsonObject();

                    JsonArray jAndArray = new JsonArray();

                    JsonObject joLessTimeGroup3 = new JsonObject().putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, endDateL - 1000L * 60 * 60 * 24 * 2 * promoObj.DURATION_TRAN));
                    JsonObject joGreaterTimeGroup3 = new JsonObject().putObject(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, new JsonObject().putNumber(MongoKeyWords.GREATER, 0));
                    JsonObject joEndGroup3 = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, false);
                    JsonObject joEndGroup2 = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, true);
                    jAndArray.add(joLessTimeGroup3);
                    jAndArray.add(joEndGroup3);
                    jAndArray.add(joEndGroup2);
                    jAndArray.add(joGreaterTimeGroup3);

                    joFilter.putArray(MongoKeyWords.AND_$, jAndArray);
                    logger.info("query update endgroup 3 " + joFilter.toString());

                    JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, true);
                    acquireBinhTanUserPromotionDb.updateAllField(joFilter, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {
                            logger.info("update endgroup 3 success");
                        }
                    });
                }
                else {
                    logger.info(jsonResponse.getString(StringConstUtil.DESCRIPTION, ""));
                }
            }
        });
    }



    private long getRandomTime(int minTime, int maxTime)
    {
        long randomTime = 0;
        if(maxTime != 0)
        {
            randomTime = System.currentTimeMillis() % maxTime;
            if(randomTime < minTime)
            {
                randomTime = Math.round( (minTime + maxTime + randomTime)/3 );
            }
        }
        return randomTime;
    }
    private void executeGroup2Promotion(final Message message, final String phoneNumber,final String program, JsonObject joExtra, final PromotionDb.Obj promoProgram, final Common.BuildLog log)
    {
        log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "executeGroup2Promotion");
        final AcquireBinhTanUserPromotionDb.Obj acquireObj = new AcquireBinhTanUserPromotionDb.Obj(joExtra.getObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, new JsonObject()));
        log.add("acquireObj " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, acquireObj.toJson());
        if(acquireObj.numberOfOtp > 1)
        {
            //todo Insert ERROR_CHECK Table
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Tai khoan da bi lock do get OTP 2 lan " + phoneNumber);
            log.writeLog();
            if(message != null)
            {
                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã nhận mã xác thực (OTP) trên 1 lần nên không được tham gia chương trình khuyến mãi. "));
            }
            return;
        }
        else if(acquireObj.end_group_2)
        {
            //todo Insert ERROR_CHECK Table
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Khong duoc tra group 2 nua, tra du roi " + phoneNumber);
            log.writeLog();
            if(message != null)
            {
                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "Khong duoc tra group 2 nua, tra du roi " + phoneNumber));
            }
            return;
        }
        else if(acquireObj.isLocked)
        {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Số điện thoại này đã bị lock rồi " + phoneNumber);
            log.writeLog();
            if(message != null)
            {
                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "Số điện thoại này đã bị lock rồi " + phoneNumber));
            }
            return;
        }
        else if(acquireObj.tid_cashin > 0 || acquireObj.time_cashin > 0 || acquireObj.amount_cashin > 0)
        {
            //todo Insert ERROR_CHECK Table
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Da chuyen so dien thoai nay qua group 3 roi " + phoneNumber);
            log.writeLog();
            if(message != null)
            {
                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "Da chuyen so dien thoai nay qua group 3 roi " + phoneNumber));
            }

            return;
        }
        final JsonObject joFilter = new JsonObject();
        joFilter.putString(colName.AcquireBinhTanGroup2PromotionCol.PHONE_NUMBER, phoneNumber);
        final String id = phoneNumber + "_" + program;
        promotionCountTrackingDb.findAndIncCountUser(phoneNumber, program, id, new Handler<PromotionCountTrackingDb.Obj>() {
            @Override
            public void handle(PromotionCountTrackingDb.Obj promoCountTrackingObj) {
                if(promoCountTrackingObj == null)
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Du lieu count bi null, kiem tra lai code " + phoneNumber + "_" + program + "_" + id);
                    //todo Update endgroup = true
                    if(message != null)
                    {
                        message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "Du lieu count bi null, kiem tra lai code " + phoneNumber + "_" + program + "_" + id));
                    }
                    return;
                }
                else if(promoCountTrackingObj != null && promoCountTrackingObj.count > 2)
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Da tra 3 lan roi, khong tra nua");
                    //todo Update endgroup = true
                    updateEndGroup2(phoneNumber, log);
                    if(message != null)
                    {
                        message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "Da tra 3 lan roi, khong tra nua"));
                    }
                    return;
                }
                acquireBinhTanGroup2PromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanGroup2PromotionDb.Obj>>() {
                    @Override
                    public void handle(final ArrayList<AcquireBinhTanGroup2PromotionDb.Obj> listGroup2Promos) {
                        if(listGroup2Promos.size() > 2)
                        {
                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Da tra 3 lan roi, khong tra nua");
                            //todo Update endgroup = true
                            updateEndGroup2(phoneNumber, log);
                            if(message != null)
                            {
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "Da tra 3 lan roi, khong tra nua"));
                            }
                            return;
                        }
                        giveBonusMoneyForUser(promoProgram.ADJUST_ACCOUNT, promoProgram.PER_TRAN_VALUE, phoneNumber, log, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject joResponse) {
                                final int error = joResponse.getInteger(StringConstUtil.ERROR, -1000);
                                final long tranId = joResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                                if(error != 0)
                                {
                                    //todo Insert ERROR_CHECK Table
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "So dien thoai " + phoneNumber + " bi core tra loi ");
                                    log.writeLog();
                                    if(message != null)
                                    {
                                        message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 1000).putString(StringConstUtil.DESCRIPTION, "So dien thoai " + phoneNumber + " bi core tra loi "));
                                    }
                                    return;
                                }
                                else {
                                    Gift gift = new Gift();
                                    gift.setModelId("topup");
                                    gift.status = 3;
                                    gift.typeId = "topup";
                                    long endgift = promoProgram.DURATION * 24 * 60 * 60 * 1000L + System.currentTimeMillis();
                                    log.add("endgift " + StringConstUtil.BinhTanPromotion.PROGRAM, endgift + "");

                                    String body = listGroup2Promos.size() == 0 ? promoProgram.NOTI_COMMENT.replaceAll("%endgift", Misc.dateVNFormatWithDot(endgift)).replaceAll("%endpromo", Misc.dateVNFormatWithDot(promoProgram.DATE_TO)) : promoProgram.NOTI_BODY_INVITER;
                                    String caption = listGroup2Promos.size() == 0 ? promoProgram.NOTI_CAPTION : promoProgram.NOTI_SMS_INVITER;
                                    log.add("body " + StringConstUtil.BinhTanPromotion.PROGRAM, body + "");
                                    log.add("caption " + StringConstUtil.BinhTanPromotion.PROGRAM, caption + "");
                                    sendNotiAndTranHis(promoProgram.PER_TRAN_VALUE, caption, body, body, promoProgram, phoneNumber, tranId, gift);
                                    if(message != null)
                                    {
                                        message.reply(joResponse);
                                    }
//                            Misc.sendTranHisAndNotiGiftForBillPay(vertx, DataUtil.strToInt(phoneNumber), promoProgram.NOTI_COMMENT, tranId, promoProgram.PER_TRAN_VALUE, gift,
//                                    promoProgram.NOTI_CAPTION, promoProgram.NOTI_COMMENT, promoProgram.NOTI_COMMENT, tranDb);
                                }
                                final AcquireBinhTanGroup2PromotionDb.Obj acquireObj2 = new AcquireBinhTanGroup2PromotionDb.Obj();
                                acquireObj2.phoneNumber = phoneNumber;
                                acquireObj2.amount_bonus = promoProgram.PER_TRAN_VALUE;
                                acquireObj2.tid_bonus = tranId;
                                acquireObj2.time_bonus = System.currentTimeMillis();
                                acquireObj2.times = listGroup2Promos.size() + 1;
                                acquireObj2.isUsed = 0;
                                acquireBinhTanGroup2PromotionDb.insert(acquireObj2, new Handler<Integer>() {
                                    @Override
                                    public void handle(Integer event) {
                                        if(acquireObj2.times == 3)
                                        {
                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Update du lieu ket thuc group 2");
                                            updateEndGroup2(phoneNumber, log);
                                            return;
                                        }
                                        else {
                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "Update lan tra thuong ke tiep cho group 2");
                                            updateNextTimeGroup2(phoneNumber, log, promoProgram);
                                            return;
                                        }
                                    }
                                });

                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, "executeGroup2Promotion " + phoneNumber + " success");
                                log.writeLog();
                                return;
                            }
                        });
                    }
                });
            }
        });

    }

    private void executeGroup3Promotion(final BinhTanPromotionObj binhTanPromotionObj, final Message message, final String phoneNumber, final String program, final JsonObject joExtra, final PromotionDb.Obj promoProgram, final Common.BuildLog log) {
        final AcquireBinhTanUserPromotionDb.Obj acquireObj = new AcquireBinhTanUserPromotionDb.Obj(joExtra.getObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, new JsonObject()));
        final int group = groupPayGift(promoProgram.DURATION_TRAN, acquireObj.time_group_3, log);
        if (acquireObj.time_group_3 == 0) {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Chua cashin ma topup !!!! " + acquireObj.phoneNumber + " TIME " + acquireObj.time_group_3);
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Chua cashin ma topup !!!! " + acquireObj.phoneNumber + " TIME " + acquireObj.time_group_3);
            log.writeLog();
            return;
        } else if (group < 1 || group > 3) {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Qua thoi gian tham gia chuong trinh nay roi !!!! " + acquireObj.phoneNumber);
            JsonObject joUpdate = new JsonObject();
            joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, true);
            joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.GROUP, 3);
            acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                @Override
                public void handle(Boolean event) {

                }
            });
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Qua thoi gian tham gia chuong trinh nay roi !!!! " + acquireObj.phoneNumber);
            log.writeLog();
            return;
        } else if (acquireObj.isLocked) {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Số điện thoại này đã bị lock " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis());
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Số điện thoại này đã bị lock " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis());
            log.writeLog();
            return;
        } else if (acquireObj.numberOfOtp > 1) {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Số điện thoại này đã bị lock vì get OTP > 1 lan " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis());
            log.writeLog();
            JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
            acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                @Override
                public void handle(Boolean event) {
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Số điện thoại này đã bị lock vì get OTP > 1 lan " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis());
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000, "Thiết bị này đã nhận mã xác thực (OTP) trên 1 lần nên không được tham gia tiếp chương trình khuyến mãi");
                }
            });
            return;
        }
        boolean isOk = checkOk(promoProgram.DURATION_TRAN, acquireObj.time_cashin, acquireObj.time_group_3, log);
        if (!isOk) {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Cashin qua lau roi, cashin lai roi billpay tiep nhe !!!!");
            log.writeLog();
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Cashin qua lau roi, cashin lai roi billpay tiep nhe !!!!");
            return;
        }
        log.add("duration tran " + promoProgram.NAME , promoProgram.DURATION_TRAN);
        log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, group);
        List<String> listVc = getListVoucher(promoProgram.INTRO_SMS);
        String voucherInfo = getVoucherInfo(listVc, group);
        if ("".equalsIgnoreCase(voucherInfo)) {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Khong co thong tin the qua tang !!!! " + acquireObj.phoneNumber);
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong co thong tin the qua tang !!!! " + acquireObj.phoneNumber);
            log.writeLog();
            return;
        }
        final String[] vc = voucherInfo.split(":");
        final long amount_vc = getAmountVC(DataUtil.strToLong(vc[1].toString().trim()), promoProgram, joExtra.getLong(StringConstUtil.AMOUNT, 0));
        final List<String> listVoucher = new ArrayList<>();
        listVoucher.add(vc[0].toString().trim());
        long startTime = getStartTimeGroup3(promoProgram.DURATION_TRAN, acquireObj.time_group_3, log);
        long endTime = getEndTimeGroup3(promoProgram.DURATION_TRAN, acquireObj.time_group_3, log);
        log.add("starttime group 3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, startTime);
        log.add("endTime group 3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endTime);
        final JsonObject joFilter = new JsonObject();
        JsonArray jarrAnd = new JsonArray();

        JsonObject joNumber = new JsonObject().putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, phoneNumber);
        JsonObject joStartTime = new JsonObject().putObject(colName.AcquireBinhTanGroup3PromotionCol.TIME_BONUS, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime));
        JsonObject joEndTime = new JsonObject().putObject(colName.AcquireBinhTanGroup3PromotionCol.TIME_BONUS, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime));
        jarrAnd.add(joNumber);
        jarrAnd.add(joStartTime);
        jarrAnd.add(joEndTime);
        joFilter.putArray(MongoKeyWords.AND_$, jarrAnd);
        logger.info("query tra qua " + joFilter.toString());
        final String id = phoneNumber + "_" + program + "_" + group;
//        if(group == 1 && !promoProgram.STATUS) {
//            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Khong cho nhan thuong tu dien gia dich ngoai activation !!!! " + acquireObj.phoneNumber);
//            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong cho nhan thuong tu dien gia dich ngoai activation !!!! " + acquireObj.phoneNumber);
//            log.writeLog();
//            return;
//        }
        acquireBinhTanGroup3PromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanGroup3PromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<AcquireBinhTanGroup3PromotionDb.Obj> listItems) {
                log.add("listItem size " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, listItems.size());
                log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, group);
                if (listItems.size() > 0) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Da nhan duoc qua tang cua dot nay roi, doi 14 ngay sau nhe !!!! " + acquireObj.phoneNumber);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Da nhan duoc qua tang cua dot nay roi, doi 14 ngay sau nhe !!!! " + acquireObj.phoneNumber);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000, "Trong đợt này, số " + acquireObj.phoneNumber + " đã nhận quà tặng vào ngày " + Misc.dateVNFormatWithDot(listItems.get(0).time_bonus) + " vui lòng thực hiện lại sau 14 ngày.");
                    log.writeLog();
                    return;
                }
                promotionCountTrackingDb.findAndIncCountUser(phoneNumber, program, id, new Handler<PromotionCountTrackingDb.Obj>() {
                    @Override
                    public void handle(PromotionCountTrackingDb.Obj promoCountTrackingObj) {
                        if (promoCountTrackingObj == null) {
                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "thong tin count bi null, review lai code !!!! " + acquireObj.phoneNumber);
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "thong tin count bi null, review lai code " + acquireObj.phoneNumber);
                            log.writeLog();
                            return;
                        } else if (promoCountTrackingObj.count > 1) {
                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Da nhan duoc qua tang cua dot nay roi, doi 14 ngay sau nhe vi count > 1!!!! " + acquireObj.phoneNumber);
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Da nhan duoc qua tang cua dot nay roi, doi 14 ngay sau nhe vi count > 1 !!!! " + acquireObj.phoneNumber);
                            log.writeLog();
                            return;
                        }
                        final long timer = promoProgram.MIN_TIMES == 0 ? 1000L * 60 * 15 : promoProgram.MIN_TIMES * 1000L;
                        ControlOnClickActivityDb.Obj controlActiObj = new ControlOnClickActivityDb.Obj();
                        controlActiObj.key = phoneNumber + "_" + program + "_" + group;
                        controlActiObj.program = program;
                        controlActiObj.number = phoneNumber;
                        controlActiObj.service = "";
                        controlOnClickActivityDb.insert(controlActiObj, new Handler<Integer>() {
                            @Override
                            public void handle(Integer result) {
                                if (result == 0) {
                                    vertx.setTimer(timer, new Handler<Long>() {
                                        @Override
                                        public void handle(final Long giftTimer) {
                                            acquireBinhTanUserPromotionDb.findOne(phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                                                @Override
                                                public void handle(AcquireBinhTanUserPromotionDb.Obj acquireObjAfter) {
                                                    if (acquireObjAfter == null) {
                                                        log.add("acquireObjAfter " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, " is null");
                                                        log.writeLog();
                                                        vertx.cancelTimer(giftTimer);
                                                        return;
                                                    } else if (acquireObjAfter.numberOfOtp > 1) {
                                                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Số điện thoại này không nhận được quà voucher do đã bị lock vì get OTP > 1 lan " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis() + " OTP " + acquireObjAfter.numberOfOtp);
                                                        log.writeLog();
                                                        JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                                                        acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean event) {
                                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Số điện thoại này không nhận được quà voucher do đã bị lock vì get OTP > 1 lan " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis());
                                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000, "Thiết bị này đã nhận mã xác thực (OTP) trên 1 lần nên không được tham gia tiếp chương trình khuyến mãi");
                                                            }
                                                        });
                                                        vertx.cancelTimer(giftTimer);
                                                        return;
                                                    }
                                                    JsonObject joFilter = new JsonObject().putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, phoneNumber);
                                                    acquireBinhTanGroup3PromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanGroup3PromotionDb.Obj>>() {
                                                        @Override
                                                        public void handle(ArrayList<AcquireBinhTanGroup3PromotionDb.Obj> listPromoTransG3) {
                                                            if (listPromoTransG3.size() >= group) {
                                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "Số điện thoại này không nhận đủ quà voucher cho lần này rồi " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis() + " GROUP " + group);
                                                                log.writeLog();
                                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Số điện thoại này không nhận đủ quà voucher cho lần này rồi " + acquireObj.phoneNumber + " TIME " + System.currentTimeMillis() + " GROUP " + group);
                                                                vertx.cancelTimer(giftTimer);
                                                                return;
                                                            }
                                                            giveVoucherForUser(message, amount_vc, promoProgram.DURATION, new JsonObject(), promoProgram.ADJUST_ACCOUNT,
                                                                    binhTanPromotionObj, promoProgram, listVoucher, log, new Handler<JsonObject>() {
                                                                        @Override
                                                                        public void handle(JsonObject joResponse) {
                                                                            //todo tao db group 3 va insert.
                                                                            int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                                                                            long tranId = joResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                                                            String giftId = joResponse.getString(StringConstUtil.GIFT_ID, "");
                                                                            if (err == 0) {
                                                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tra qua thanh cong " + phoneNumber);
                                                                                AcquireBinhTanGroup3PromotionDb.Obj acquireObj3 = new AcquireBinhTanGroup3PromotionDb.Obj();
                                                                                acquireObj3.phoneNumber = phoneNumber;
                                                                                acquireObj3.tid_billpay = joExtra.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                                                                acquireObj3.amount_billpay = joExtra.getLong(StringConstUtil.AMOUNT, 0);
                                                                                acquireObj3.tid_cashin = acquireObj.tid_cashin;
                                                                                acquireObj3.time_cashin = acquireObj.time_cashin;
                                                                                acquireObj3.amount_cashin = acquireObj.amount_cashin;
                                                                                acquireObj3.tid_bonus = tranId;
                                                                                acquireObj3.amount_bonus = amount_vc;
                                                                                acquireObj3.time_bonus = System.currentTimeMillis();
                                                                                acquireObj3.giftId = giftId;

                                                                                acquireBinhTanGroup3PromotionDb.insert(acquireObj3, new Handler<Integer>() {
                                                                                    @Override
                                                                                    public void handle(Integer result) {
                                                                                        if (group == 3) {
                                                                                            JsonObject joUpdate = new JsonObject();
                                                                                            joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, true);
                                                                                            acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                                                @Override
                                                                                                public void handle(Boolean event) {

                                                                                                }
                                                                                            });
                                                                                        }
                                                                                    }
                                                                                });
                                                                                if (message != null) {
                                                                                    message.reply(joResponse);
                                                                                }
                                                                            } else {
                                                                                //todo luu lai bang loi
                                                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Loi core khong tra qua user " + err);
                                                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Loi core khong tra qua user " + acquireObj.phoneNumber);
                                                                                log.writeLog();
                                                                                return;
                                                                            }
                                                                            vertx.cancelTimer(giftTimer);
                                                                        }
                                                                    }
                                                            );
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    //todo luu lai bang loi
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khach hang giao dich trong thoi gian xem xet truoc khi co qua " + phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khach hang giao dich trong thoi gian xem xet truoc khi co qua " + phoneNumber);
                                    log.writeLog();
                                    return;
                                }
                            }
                        });
                    }
                });
            }
        });

    }

    private long getStartTimeGroup3(int duration, long startTimeInG3, Common.BuildLog log)
    {
        log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "getStartTimeGroup3");
        log.add("getStartTimeGroup3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, startTimeInG3);
        long time = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTimeInG3);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        long endStartTimeG3 = isUAT ? startTimeInG3 : calendar.getTimeInMillis();
        log.add("endStartTimeG3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endStartTimeG3);
        long durationMillis = 0;
        if(isUAT)
        {
            durationMillis = duration * 1000L * 60;
        }
        else {
            durationMillis = duration * 1000L * 60 * 60 * 24;
        }

        log.add("durationMillis " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, durationMillis);
        if(endStartTimeG3 + durationMillis >= System.currentTimeMillis() && startTimeInG3 <= System.currentTimeMillis())
        {
            time = startTimeInG3;
            log.add("time start g3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, time);
        }
        else if(endStartTimeG3 + 2 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + durationMillis <= System.currentTimeMillis())
        {
            time = endStartTimeG3 + durationMillis;
            log.add("time start g3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, time);
        }
        else if(endStartTimeG3 + 3 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + 2 * durationMillis <= System.currentTimeMillis())
        {
            time = endStartTimeG3 + 2 * durationMillis;
            log.add("time start g3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, time);
        }
        return time;
    }

    private long getEndTimeGroup3(int duration, long startTimeInG3, Common.BuildLog log)
    {
        log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "getEndTimeGroup3");
        log.add("getEndTimeGroup3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, startTimeInG3);
        long time = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTimeInG3);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        long endStartTimeG3 = isUAT ? startTimeInG3 : calendar.getTimeInMillis();
        log.add("endStartTimeG3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endStartTimeG3);
        long durationMillis = 0;
        if(isUAT)
        {
            durationMillis = duration * 1000L * 60;
        }
        else {
            durationMillis = duration * 1000L * 60 * 60 * 24;
        }
        log.add("durationMillis " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, durationMillis);
        if(endStartTimeG3 + durationMillis >= System.currentTimeMillis() && startTimeInG3 <= System.currentTimeMillis())
        {
            time = endStartTimeG3 + durationMillis;
            log.add("time end g3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, time);
        }
        else if(endStartTimeG3 + 2 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + durationMillis <= System.currentTimeMillis())
        {
            time = endStartTimeG3 + 2 * durationMillis;
            log.add("time end g3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, time);
        }
        else if(endStartTimeG3 + 3 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + 2 * durationMillis <= System.currentTimeMillis())
        {
            time = endStartTimeG3 + 3 * durationMillis;
            log.add("time end g3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, time);
        }
        return time;
    }

    private long getAmountVC(long standardAmount, PromotionDb.Obj promoProgram, long billPayAmount)
    {
        long amount = standardAmount;

        if(promoProgram.ENABLE_PHASE2)
        {
          amount = standardAmount;
        }
        else {
          amount = billPayAmount > standardAmount ? standardAmount : billPayAmount;
        }

        return amount;
    }
    private String getVoucherInfo(List<String> listVc, int group)
    {
        int position = listVc.size() >= group ? group - 1 : 0;
//        String[] vcData = listVc.get(position).split(":");
//        long amount = vcData.length == 2 ? DataUtil.strToLong(vcData[1].trim()) : 0;

        return listVc.size() > 0 ? listVc.get(position).toString().trim() : "";
    }

    private List<String> getListVoucher(String voucherInfo)
    {
        String[] listGift = voucherInfo.split(";");
        List<String> listVoucher = new ArrayList<>();
        String []gift = {};
        for(String giftInfo : listGift)
        {
            gift = giftInfo.trim().split(":");
            if(gift.length == 2)
            {
                listVoucher.add(giftInfo.trim());
            }
        }

        return listVoucher;

    }

    private int groupPayGift(int duration, long startTimeInG3, Common.BuildLog log)
    {
        log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "groupPayGift");
        log.add("startTimeInG3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, startTimeInG3);
        int group = -1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTimeInG3);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        long endStartTimeG3 = isUAT ? startTimeInG3 : calendar.getTimeInMillis();
        log.add("endStartTimeG3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endStartTimeG3);
        long durationMillis = 0;
        if(isUAT)
        {
            durationMillis = duration * 1000L * 60;
        }
        else {
            durationMillis = duration * 1000L * 60 * 60 * 24;
        }
        log.add("durationMillis " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, durationMillis);
        if(startTimeInG3 > System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "0");
            group = 0;
        }
        else if(endStartTimeG3 + durationMillis >= System.currentTimeMillis() && startTimeInG3 <= System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "1");
            group = 1;
        }
        else if(endStartTimeG3 + 2 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + durationMillis <= System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "2");
            group = 2;
        }
        else if(endStartTimeG3 + 3 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + 2 * durationMillis <= System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "3");
            group = 3;
        }
        else if(endStartTimeG3 + 3 * durationMillis < System.currentTimeMillis()) {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "4");
            group = 4;
        }
        return group;
    }

    /*
    This method used to check cashin time is in the period when paying bill
     */
    private boolean checkOk(int duration, long cashinTime, long group3Time, Common.BuildLog log)
    {
        log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "checkOk");
        log.add("cashinTime " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, cashinTime);
        log.add("group3Time " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, group3Time);
        log.add("duration " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, duration);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(cashinTime);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long milliDuration = 0;
        long endCashInTime = isUAT ? cashinTime : calendar.getTimeInMillis();
        calendar.setTimeInMillis(group3Time);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long endGroup3Time = isUAT ? group3Time : calendar.getTimeInMillis();

        log.add("duration " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, duration);
        log.add("cashinTime " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, cashinTime);
        log.add("group3Time " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, group3Time);
        log.add("endCashInTime " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endCashInTime);
        log.add("endGroup3Time " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time);
        if(isUAT)
        {
            milliDuration = duration * 1000L * 60;
        }
        else {
            milliDuration = duration * 1000L * 60 * 60 * 24;
        }
        log.add("milliDuration " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, milliDuration);
//        log.add("Test endGroup3Time + milliDuration >= System.currentTimeMillis() :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + milliDuration +" >= " + System.currentTimeMillis());
//        log.add("Test group3Time <= System.currentTimeMillis() :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, group3Time +" <= "+ System.currentTimeMillis());
//        log.add("Test endGroup3Time + milliDuration >= endCashInTime:  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + milliDuration +" >= "+ endCashInTime);
//        log.add("Test group3Time <= endCashInTime :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, group3Time +" <= "+ endCashInTime);
//
//        log.add("Test endGroup3Time + 2 * milliDuration >= System.currentTimeMillis() :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + 2 * milliDuration +" >= "+ System.currentTimeMillis());
//        log.add("Test endGroup3Time + milliDuration <= System.currentTimeMillis() :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + milliDuration +" <= "+ System.currentTimeMillis());
//        log.add("Test endGroup3Time + 2 * milliDuration >= endCashInTime:  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + 2 * milliDuration +" >= "+ endCashInTime);
//        log.add("Test endGroup3Time + milliDuration <= endCashInTime :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + milliDuration + " <= " + endCashInTime);
//
//        log.add("Test endGroup3Time + 3 * milliDuration >= System.currentTimeMillis() :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + 3 * milliDuration +" >= " + System.currentTimeMillis());
//        log.add("Test endGroup3Time + 2 * milliDuration <= System.currentTimeMillis() :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + 2 * milliDuration +" <= "+ System.currentTimeMillis());
//        log.add("Test endGroup3Time + 3 * milliDuration >= endCashInTime:  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + 3 * milliDuration +" >= "+ endCashInTime);
//        log.add("Test endGroup3Time + 2 * milliDuration <= endCashInTime :  " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endGroup3Time + 2 * milliDuration +" <= " + endCashInTime);

        if(endGroup3Time + milliDuration >= System.currentTimeMillis() && group3Time <= System.currentTimeMillis()
                && endGroup3Time + milliDuration >= endCashInTime && group3Time <= endCashInTime)
        {
            log.add("group checkOk " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, 1);
            return true;
        }
        else if(endGroup3Time + 2 * milliDuration >= System.currentTimeMillis() && endGroup3Time + milliDuration <= System.currentTimeMillis()
                && endGroup3Time + 2 * milliDuration >= endCashInTime && endGroup3Time + milliDuration <= endCashInTime)
        {
            log.add("group checkOk " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, 2);
            return true;
        }
        else if(endGroup3Time + 3 * milliDuration >= System.currentTimeMillis() && endGroup3Time + 2 * milliDuration <= System.currentTimeMillis()
                && endGroup3Time + 3 * milliDuration >= endCashInTime && endGroup3Time + 2 * milliDuration <= endCashInTime)
        {
            log.add("group checkOk " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, 3);
            return true;
        }
        return false;

    }

    private void updateNextTimeGroup2(String phoneNumber, Common.BuildLog log, PromotionDb.Obj promoProgram) {
        JsonObject joUpdate = new JsonObject();
        long nextTime = promoProgram.DURATION_TRAN * 24 * 60 * 60 * 1000L + System.currentTimeMillis();
        joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_BONUS, nextTime);
        long rollbackTime = promoProgram.DURATION * 24 * 60 * 60 * 1000L + System.currentTimeMillis();
        joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_ROLLBACK, rollbackTime);
        joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.GROUP, 2);
        joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, false);
        acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {

            }
        });
        log.writeLog();
    }

    private void updateEndGroup2(String phoneNumber, Common.BuildLog log) {
        JsonObject joUpdate = new JsonObject();
        joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, true);
        acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {

            }
        });
        log.writeLog();
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void giveVoucherForUser(final Message<JsonObject> message
            , final long value_of_gift
            , final int time_for_gift
            , final JsonObject joReply
            , final String agent
            , final BinhTanPromotionObj binhTanPromotionObj
            , final PromotionDb.Obj acquirePromoProgram
            , final List<String> listVoucher
            , final Common.BuildLog log
            , final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", binhTanPromotionObj.source));

        keyValues.add(new Misc.KeyValue("group", binhTanPromotionObj.source));



        log.add("TOTAL GIFT " + StringConstUtil.BinhTanPromotion.PROGRAM, listVoucher.size());
        log.add("TOTAL VALUE " + StringConstUtil.BinhTanPromotion.PROGRAM, value_of_gift);

        int timeForGift = time_for_gift;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , binhTanPromotionObj.phoneNumber
                , value_of_gift
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error" + StringConstUtil.BinhTanPromotion.PROGRAM, error);
                log.add("desc" + StringConstUtil.BinhTanPromotion.PROGRAM, SoapError.getDesc(error));

                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    //tao gift tren backend
                    final GiftType giftType = new GiftType();
                    final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    final Misc.KeyValue kv = new Misc.KeyValue();

                    final long modifyDate = System.currentTimeMillis();
                    final String note = binhTanPromotionObj.source;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = binhTanPromotionObj.source;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                    log.add("so luong voucher " + StringConstUtil.BinhTanPromotion.PROGRAM, atomicInteger);

                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {
                            if (atomicInteger.decrementAndGet() < 0) {
                                log.add("func " + StringConstUtil.BinhTanPromotion.PROGRAM, "out of range for number " + binhTanPromotionObj.phoneNumber);
                                log.writeLog();
                                vertx.cancelTimer(aPeriodicLong);
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                log.add("itemPosition " + StringConstUtil.BinhTanPromotion.PROGRAM, itemPosition);
                                giftType.setModelId(listVoucher.get(itemPosition).trim());
                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(binhTanPromotionObj.phoneNumber
                                        , value_of_gift
                                        , giftType
                                        , promotedTranId
                                        , agent
                                        , modifyDate
                                        , endGiftTime
                                        , keyValues, note, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {
                                        int err = jsonObject.getInteger("error", -1);
                                        final long tranId = jsonObject.getInteger("tranId", -1);
                                        final Gift gift = new Gift(jsonObject.getObject("gift"));
                                        final String giftId = gift.getModelId().trim();
                                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "tra thuong chuong trinh zalo bang gift");
                                        log.add("err " + StringConstUtil.BinhTanPromotion.PROGRAM, err);

                                        //------------tat ca thanh cong roi
                                        if (err == 0) {
                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tao qua local referral thanh cong");
                                            giftManager.useGift(binhTanPromotionObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {
//                                                    Misc.sendTranHisAndNotiGiftForBillPay(vertx, DataUtil.strToInt(binhTanPromotionObj.phoneNumber), acquirePromoProgram.NOTI_COMMENT, tranId, value_of_gift, gift,
//                                                            acquirePromoProgram.NOTI_CAPTION, acquirePromoProgram.NOTI_COMMENT, acquirePromoProgram.NOTI_COMMENT, tranDb);
                                                    String body = acquirePromoProgram.NOTI_COMMENT.replaceAll("%money", Misc.formatAmount(value_of_gift));
                                                    sendNotiAndTranHis(value_of_gift, acquirePromoProgram.NOTI_CAPTION, body, body, acquirePromoProgram, binhTanPromotionObj.phoneNumber, tranId, gift);
                                                    joReply.putNumber(StringConstUtil.ERROR, 0);
                                                    joReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId);
                                                    joReply.putString(StringConstUtil.GIFT_ID, giftId);
                                                    callback.handle(joReply);
                                                    return;
                                                }
                                            });
                                        } else {
                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tao qua local binhtan fail");
                                            joReply.putNumber("error", 1000);
                                            joReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId);
                                            callback.handle(joReply);
                                            return;
                                        }
                                    }
                                });
                            }
                        }
                    });
                    return;
                } else {
                    //tra thuong trong core khong thanh cong
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Core loi");
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Lỗi " + SoapError.getDesc(error));
                    log.add("Exception " + StringConstUtil.BinhTanPromotion.PROGRAM, "Exception " + SoapError.getDesc(error));
                    callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, error).putString(StringConstUtil.DESCRIPTION, "core tra loi khi tra the qua tang " + error)
                            .putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId));
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, error, "Loi core");
                    return;
                }
            }
        });
    }

    private void getPromotion(final String program, final Handler<JsonObject> callback)
    {
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        //Moi lan chay la kiem tra thoi gian promo.
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonObject joReply = new JsonObject();
                JsonArray array = json.getArray("array", null);
                long promo_start_date = 0;
                long promo_end_date = 0;
                long currentTime = System.currentTimeMillis();
                String agent = "";
                long amount = 0;
                String titleNoti = "";
                String bodyNoti = "";
                String bodyTrans = "";
                String partnerName = "";
                int duration = 0;
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(program)) {
                            promo_start_date = promoObj.DATE_FROM;
                            promo_end_date = promoObj.DATE_TO;
                            agent = promoObj.ADJUST_ACCOUNT;
                            amount = promoObj.PER_TRAN_VALUE;
                            titleNoti = promoObj.NOTI_CAPTION;
                            bodyNoti = promoObj.NOTI_COMMENT;
                            bodyTrans = promoObj.NOTI_COMMENT;
                            partnerName = promoObj.INTRO_DATA;
                            duration = promoObj.DURATION;
                            break;
                        }
                    }
                    //Kiem tra xem con thoi gian khuyen mai ko
                    if("".equalsIgnoreCase(agent))
                    {
                        logger.info("program binh tan " + StringConstUtil.BinhTanPromotion.PROGRAM);
                        logger.info("thong tin agent1 && agent 2 " + agent + " " + StringConstUtil.BinhTanPromotion.PROGRAM);
                        logger.info("thong tin start date " + promo_start_date + " " + StringConstUtil.BinhTanPromotion.PROGRAM);
                        logger.info("thong tin end date " + promo_end_date + " " + StringConstUtil.BinhTanPromotion.PROGRAM);
                        logger.info("thong tin amount " + amount + " " + StringConstUtil.BinhTanPromotion.PROGRAM);
                        logger.info("Ngoai thoi gian khuyen mai, khong ghi nhan thong tin" + " " + StringConstUtil.BinhTanPromotion.PROGRAM);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Chua cau hinh chuong trinh de quet tu dong " + " " + StringConstUtil.BinhTanPromotion.PROGRAM);
                        joReply.putNumber(StringConstUtil.ERROR, -1);
                        callback.handle(joReply);
                    }
                    else {
                        //Tim thong tin diem giao dich
                        joReply.putObject(StringConstUtil.PROMOTION, promoObj.toJsonObject());
                        joReply.putNumber(StringConstUtil.ERROR, 0);
                        callback.handle(joReply);
                    }
                }
                else
                {
                    logger.info("Khong load duoc thong tin du lieu khuyen mai");
                    joReply.putString(StringConstUtil.DESCRIPTION, "Khong co thong tin");
                    joReply.putNumber(StringConstUtil.ERROR, -2);
                    callback.handle(joReply);
                }
            }
        });
    }

    private void giveBonusMoneyForUser(final String agent, final long value_of_money, final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.setPhoneNumber(phoneNumber);
        log.add("desc", "executeGroup2Promotion");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = StringConstUtil.BinhTanPromotion.PROGRAM;
        Misc.adjustment(vertx, agent, phoneNumber, value_of_money,
                Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("desc", "core tra ket qua");
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            log.add("core tra loi ", soapObjReply.error);
                            log.add("status ", soapObjReply.status);
                            log.add("tid", soapObjReply.tranId);
                            log.add("desc", "core tra loi");
                            log.writeLog();
                            JsonObject jsonReply = new JsonObject();
                            jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "Khong thu duoc tien khach hang, core tra loi " + soapObjReply.error);
                            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                            callback.handle(jsonReply);
                            return;
                        }
                        //Yeu cau xong tien mat
                        //Luu qua trong Db va ban noti thong bao
//                        giveMoney(message, zaloTetPromotionObj, log, soapObjReply, jsonReply);
                        jsonReply.putNumber(StringConstUtil.ERROR, 0);
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        callback.handle(jsonReply);
                    }
                });
    }

    private void fireMoneyNotiAndSendTranHist(final String phoneNumber, final long amount
            , final long tranId
            , final Common.BuildLog log
            , final String titleNoti
            , final String bodyNoti
            , final String bodyTrans
            , final String partnerName
            , final int io)
    {
        log.add("desc", "fireMoneyNotiAndSendTranHist");

        JsonObject joTranHis = new JsonObject();
        joTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.FEE_VALUE);
        joTranHis.putString(colName.TranDBCols.COMMENT, bodyTrans);
        joTranHis.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        joTranHis.putNumber(colName.TranDBCols.AMOUNT, amount);
        joTranHis.putNumber(colName.TranDBCols.STATUS, 4);
        joTranHis.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        joTranHis.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
        joTranHis.putString(colName.TranDBCols.BILL_ID, "");
        joTranHis.putString(StringConstUtil.HTML, "");
        joTranHis.putNumber(colName.TranDBCols.IO, io);
        Misc.sendingStandardTransHisFromJson(vertx, tranDb, joTranHis, new JsonObject());

        JsonObject joNoti = new JsonObject();
        joNoti.putString(StringConstUtil.StandardNoti.CAPTION, titleNoti);
        joNoti.putString(StringConstUtil.StandardNoti.BODY, bodyNoti);
        joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phoneNumber);
        joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tranId);
        Misc.sendStandardNoti(vertx, joNoti);
    }

    private void sendNotiAndTranHis(long amount, String notiCaption, String notiBody, String tranComment, PromotionDb.Obj promoObj, String phoneNumber, long tranId, Gift gift) {
//        String notiCaption = promoObj.NOTI_CAPTION;
//        String giftMessage = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
//        String tranComment = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
//        String notiBody = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
        String partnerName = promoObj.INTRO_DATA;
        String serviceId = "topup";

        Misc.sendTranHisAndNotiZaloMoney(vertx
                , DataUtil.strToInt(phoneNumber)
                , tranComment
                , tranId
                , amount
                , gift
                , notiCaption
                , notiBody
                , tranComment
                , partnerName
                , serviceId
                , tranDb);
    }

    private void rollbackMoneyUser(final String agent, final long value_of_money, final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.setPhoneNumber(phoneNumber);
        log.add("desc", "rollbackMoneyUser");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = StringConstUtil.BinhTanPromotion.PROGRAM;
        Misc.adjustment(vertx, phoneNumber, agent, value_of_money,
                Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("desc", "core tra ket qua");
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            log.add("core tra loi ", soapObjReply.error);
                            log.add("status ", soapObjReply.status);
                            log.add("tid", soapObjReply.tranId);
                            log.add("desc", "core tra loi");
                            log.writeLog();
                            JsonObject jsonReply = new JsonObject();
                            jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "Khong thu duoc tien khach hang, core tra loi " + soapObjReply.error);
                            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                            callback.handle(jsonReply);
                            return;
                        }
                        //Yeu cau xong tien mat
                        //Luu qua trong Db va ban noti thong bao
//                        giveMoney(message, zaloTetPromotionObj, log, soapObjReply, jsonReply);
                        jsonReply.putNumber(StringConstUtil.ERROR, 0);
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        callback.handle(jsonReply);
                    }
                });
    }

    private void recordRegisteredUser(final Message message, JsonObject joExtra, String phoneNumber, Common.BuildLog log)
    {
        String imei = joExtra.getString(StringConstUtil.DEVICE_IMEI, "");
        String extraKey = joExtra.getString(StringConstUtil.BinhTanPromotion.EXTRA_KEY, "");
        AcquireBinhTanUserPromotionDb.Obj acquireObj = new AcquireBinhTanUserPromotionDb.Obj();
        acquireObj.phoneNumber = phoneNumber;
        acquireObj.imei = imei;
        acquireObj.extra_key = extraKey;
        acquireObj.lock = false;
        acquireObj.time = System.currentTimeMillis();
        acquireObj.group = 1;
        acquireObj.numberOfOtp = 1;
        acquireObj.echo = 1;
        acquireObj.hasBonus = joExtra.getBoolean(colName.AcquireBinhTanUserPromotionCol.HAS_BONUS, false);
        log.add("json insert" + StringConstUtil.BinhTanPromotion.PROGRAM, acquireObj.toJson());

        acquireBinhTanUserPromotionDb.insert(acquireObj, new Handler<Integer>() {
            @Override
            public void handle(Integer result) {
                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
            }
        });
    }

    private void checkRegisterdUser(JsonObject joExtra, String phoneNumber, String program, final Handler<Boolean> callback)
    {
        phoneCheckDb.findOne(phoneNumber, program, new Handler<PhoneCheckDb.Obj>() {
            @Override
            public void handle(PhoneCheckDb.Obj phoneCheckObj) {
                if(phoneCheckObj == null)
                {
                    callback.handle(false);
                }
                else {
                    callback.handle(true);
                }
            }
        } );
    }


    private void fireMoneyNotiAndSendTranHist(final String phoneNumber, final long amount
            , final long tranId
            , final Common.BuildLog log
            , final String titleNoti
            , final String bodyNoti
            , final String bodyTrans
            , final String partnerName
            , final int io
            , final int tranType)
    {
        log.add("desc", "fireMoneyNotiAndSendTranHist");

        JsonObject joTranHis = new JsonObject();
        joTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, tranType);
        joTranHis.putString(colName.TranDBCols.COMMENT, bodyTrans);
        joTranHis.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        joTranHis.putNumber(colName.TranDBCols.AMOUNT, amount);
        joTranHis.putNumber(colName.TranDBCols.STATUS, 4);
        joTranHis.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        joTranHis.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
        joTranHis.putString(colName.TranDBCols.BILL_ID, "");
        joTranHis.putString(StringConstUtil.HTML, "");
        joTranHis.putNumber(colName.TranDBCols.IO, io);
        Misc.sendingStandardTransHisFromJson(vertx, tranDb, joTranHis, new JsonObject());

        JsonObject joNoti = new JsonObject();
        joNoti.putString(StringConstUtil.StandardNoti.CAPTION, titleNoti);
        joNoti.putString(StringConstUtil.StandardNoti.BODY, bodyNoti);
        joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phoneNumber);
        joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tranId);
        Misc.sendStandardNoti(vertx, joNoti);

    }

    private long convertToStartTimeToday(String startTime)
    {
        long time = 0;

        Calendar calendar = Calendar.getInstance();
        String[] startTimes = startTime.split(":");
        int hourOfDate = startTimes.length > 2 ? DataUtil.strToInt(startTimes[0]) : 8;
        int minute = startTimes.length > 2 ? DataUtil.strToInt(startTimes[1]) : 0;
        int second = startTimes.length > 2 ? DataUtil.strToInt(startTimes[2]) : 0;
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDate);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        time = calendar.getTimeInMillis();
        return time;
    }


    private long convertToEndTimeToday(String endTime)
    {
        long time = 0;

        Calendar calendar = Calendar.getInstance();
        String[] startTimes = endTime.split(":");
        int hourOfDate = startTimes.length > 2 ? DataUtil.strToInt(startTimes[0]) : 18;
        int minute = startTimes.length > 2 ? DataUtil.strToInt(startTimes[1]) : 0;
        int second = startTimes.length > 2 ? DataUtil.strToInt(startTimes[2]) : 0;
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDate);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        time = calendar.getTimeInMillis();
        return time;
    }

    private void checkDGDInfoAndRecordTrans(final long startDateTime, long endDateTime, final PromotionDb.Obj finalPromoObj, final BinhTanPromotionObj binhTanPromotionObj,final Common.BuildLog log, final String storeNumber, final PhoneCheckDb.Obj agentObj)
    {
        JsonObject joFilter = new JsonObject();
        JsonObject joStoreNumber = new JsonObject().putString(colName.BinhTanDgdTransTracking.PHONE_NUMBER, storeNumber);
        JsonObject joStartTime = new JsonObject().putObject(colName.BinhTanDgdTransTracking.TIME, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, startDateTime));
        JsonObject joEndTime = new JsonObject().putObject(colName.BinhTanDgdTransTracking.TIME, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, endDateTime));
        JsonArray jArrAnd = new JsonArray();
        jArrAnd.add(joStoreNumber);
        jArrAnd.add(joStartTime);
        jArrAnd.add(joEndTime);
        joFilter.putArray(MongoKeyWords.AND_$, jArrAnd);
        binhTanDgdTransTrackingDb.searchWithFilter(joFilter, new Handler<ArrayList<BinhTanDgdTransTrackingDb.Obj>>() {
                    @Override
                    public void handle(final ArrayList<BinhTanDgdTransTrackingDb.Obj> listTrans) {
//                        phoneCheckDb.findOne(storeNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, new Handler<PhoneCheckDb.Obj>() {
//                            @Override
//                            public void handle(final PhoneCheckDb.Obj agentObj) {
                                int trans_per_day = agentObj.trans_per_day < 0 ? 0 : agentObj.trans_per_day;
                                int trans_per_hour = agentObj.trans_per_hour < 0 ? 0 : agentObj.trans_per_hour;
                                long min_between_trans = agentObj.min_between_trans < 0? 0 : agentObj.min_between_trans;
                                if(listTrans.size() == trans_per_day - 1)
                                {
                                    JsonObject joObject = createJsonNotification(storeNumber, String.format(PromoContentNotification.NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_DAY, trans_per_day), String.format(PromoContentNotification.NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_DAY_TITLE, trans_per_day));
                                    Misc.sendStandardNoti(vertx, joObject);
                                }
                                else if(listTrans.size() >= trans_per_day)
                                {
                                    //todo luu lai bang loi
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Điểm giao dịch " + storeNumber + " đã giao dịch quá số lần quy định trong 1 ngày là " + trans_per_day + " cho EU " + binhTanPromotionObj.phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Điểm giao dịch " + storeNumber + " đã giao dịch quá số lần quy định trong 1 ngày là " + trans_per_day + " cho EU " + binhTanPromotionObj.phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000,  "Điểm giao dịch " + storeNumber + " đã giao dịch quá số lần quy định trong 1 ngày là " + trans_per_day + " cho EU " + binhTanPromotionObj.phoneNumber + ". Vui lòng nạp MoMo và thực hiện lại giao dịch để được nhận thưởng voucher.");
                                    log.writeLog();

                                    JsonObject joObject = createJsonNotification(storeNumber, String.format(PromoContentNotification.NOTI_BINH_TAN_OVER_NUMBER_OF_TRAN_PER_DAY, trans_per_day), PromoContentNotification.NOTI_BINH_TAN_TITLE);
                                    Misc.sendStandardNoti(vertx, joObject);
                                    return;
                                }
//                        long endHour = startDateTime + (1000L * 60 * 60 * getHours(System.currentTimeMillis() - startDateTime));
                                long endHour = System.currentTimeMillis();
                                long startHour = endHour - (1000L * 60 * 60);
                                int totalTransInHour = 0;

                                for(BinhTanDgdTransTrackingDb.Obj dgdTran : listTrans)
                                {
                                    if(dgdTran.time > startHour && dgdTran.time < endHour)
                                    {
                                        totalTransInHour++;
                                    }
                                }
                                if(totalTransInHour == trans_per_hour - 1)
                                {
                                    JsonObject joObject = createJsonNotification(storeNumber, String.format(PromoContentNotification.NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_HOUR, trans_per_hour), String.format(PromoContentNotification.NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_HOUR_TITLE, trans_per_hour));
                                    Misc.sendStandardNoti(vertx, joObject);
                                }
                                else if(totalTransInHour >= trans_per_hour)
                                {
                                    //todo luu lai bang loi
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Điểm giao dịch " + storeNumber + " đã giao dịch quá số lần quy định trong 1 giờ là " + trans_per_hour + " cho EU " + binhTanPromotionObj.phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Điểm giao dịch " + storeNumber + " đã giao dịch quá số lần quy định trong 1 giờ là " + trans_per_hour + " cho EU " + binhTanPromotionObj.phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000,  "Điểm giao dịch " + storeNumber + " đã giao dịch quá số lần quy định trong 1 giờ là " + trans_per_hour + " cho EU " + binhTanPromotionObj.phoneNumber + ". Vui lòng nạp MoMo và thực hiện lại giao dịch để được nhận thưởng voucher.");
                                    log.writeLog();
                                    //todo SendNotification to DGD.

                                    JsonObject joObject = createJsonNotification(storeNumber, String.format(PromoContentNotification.NOTI_BINH_TAN_OVER_NUMBER_OF_TRAN_PER_HOUR, trans_per_hour), PromoContentNotification.NOTI_BINH_TAN_TITLE);
                                    Misc.sendStandardNoti(vertx, joObject);
                                    return;
                                }
                                long finalTransTime = 0;
                                if(listTrans.size() > 0)
                                {
                                    finalTransTime = listTrans.get(0).time;
                                }

                                if(System.currentTimeMillis() - finalTransTime < 1000L * 60 * min_between_trans)
                                {
                                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Điểm giao dịch " + storeNumber + " đã giao dịch chưa quá " + min_between_trans + " phút cho 2 giao dịch liên tiếp cho EU " + binhTanPromotionObj.phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000,  "Điểm giao dịch " + storeNumber + " đã giao dịch chưa quá " + min_between_trans + " phút cho 2 giao dịch liên tiếp cho EU " + binhTanPromotionObj.phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000,  "Điểm giao dịch " + storeNumber + " đã giao dịch chưa quá " + min_between_trans + " phút cho 2 giao dịch liên tiếp cho EU " + binhTanPromotionObj.phoneNumber + ". Vui lòng nạp MoMo và thực hiện lại giao dịch để được nhận thưởng voucher.");
                                    log.writeLog();
                                    //todo SendNotification to DGD.
                                    JsonObject joObject = createJsonNotification(storeNumber, String.format(PromoContentNotification.NOTI_BINH_TAN_OVER_TIME_BETWEEN_TWO_TRANS, min_between_trans), PromoContentNotification.NOTI_BINH_TAN_TITLE);
                                    Misc.sendStandardNoti(vertx, joObject);
                                    return;
                                }
                                phonesDb.getPhoneObjInfo(DataUtil.strToInt(binhTanPromotionObj.phoneNumber), new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(PhonesDb.Obj phoneObj) {
                                        if(phoneObj == null)
                                        {
                                            //todo chua tao vi moi, he thong chua co thong tin vi
                                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "SDT " + binhTanPromotionObj.phoneNumber + " chua tao vi moi");
                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "SDT " + binhTanPromotionObj.phoneNumber + " khong co thong tin vi tren he thong BE" );
                                            log.writeLog();
                                            //todo SendNotification to DGD.
                                            return;
                                        }
                                        else {
                                            String customerPhoneOS = null == phoneObj.phoneOs || "".equalsIgnoreCase(phoneObj.phoneOs) ? checkOS(phoneObj.deviceInfo) : phoneObj.phoneOs;
                                            int numberOfIOS = countIOSUserOfAgent(storeNumber, listTrans, log);
                                            log.add("info", "number of tran IOS " + storeNumber + " is " + numberOfIOS);
                                            if(numberOfIOS >= agentObj.totalIOS && StringConstUtil.IOS_OS.equalsIgnoreCase(customerPhoneOS))
                                            {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Điểm giao dịch " + storeNumber + " đã giao dịch quá " + agentObj.totalIOS + " thiet bi IOS trong ngay ");
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000,  "Điểm giao dịch " + storeNumber + " đã giao dịch quá " + agentObj.totalIOS + " thiet bi IOS trong ngay ");
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, binhTanPromotionObj.phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000,  "Điểm giao dịch " + storeNumber + " đã giao dịch quá " + agentObj.totalIOS + " thiet bi IOS trong ngay ");
                                                log.writeLog();
                                                //todo SendNotification to DGD.
                                                return;
                                            }
                                            BinhTanDgdTransTrackingDb.Obj binhtanObj = new BinhTanDgdTransTrackingDb.Obj();
                                            binhtanObj.phoneNumber = storeNumber;
                                            binhtanObj.customerPhone = binhTanPromotionObj.phoneNumber;
                                            binhtanObj.tid = binhTanPromotionObj.joExtra.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                            binhtanObj.amount = binhTanPromotionObj.joExtra.getLong(StringConstUtil.AMOUNT, 0);
                                            binhtanObj.time = System.currentTimeMillis();
                                            binhtanObj.os = customerPhoneOS;
                                            binhTanDgdTransTrackingDb.insert(binhtanObj, new Handler<Integer>() {
                                                @Override
                                                public void handle(Integer event) {
                                                    JsonObject joUpdate = new JsonObject();
                                                    joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.AMOUNT_CASHIN, binhTanPromotionObj.joExtra.getLong(StringConstUtil.AMOUNT, 0));
                                                    joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TID_CASHIN, binhTanPromotionObj.joExtra.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis()));
                                                    joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_CASHIN, System.currentTimeMillis());
                                                    joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, true);
                                                    joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.GROUP, 3);
                                                    joUpdate.putString(colName.AcquireBinhTanUserPromotionCol.BANK_CARD_ID, binhTanPromotionObj.joExtra.getString(StringConstUtil.BinhTanPromotion.BANK_ID, ""));
                                                    AcquireBinhTanUserPromotionDb.Obj acquireObj = new AcquireBinhTanUserPromotionDb.Obj(binhTanPromotionObj.joExtra.getObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, new JsonObject()));
                                                    if (acquireObj.time_group_3 == 0) {
                                                        joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, System.currentTimeMillis());
                                                    }
                                                    acquireBinhTanUserPromotionDb.updatePartial(binhTanPromotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean event) {

                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    }
                                });
//                            }
//                        });
                    }
                }
        );
    }

    private int countIOSUserOfAgent(final String agentNumber, final ArrayList<BinhTanDgdTransTrackingDb.Obj> listTrans, Common.BuildLog log)
    {
        int count = 0;
        log.add("func", "countIOSUserOfAgent");
        log.add("size of trans", listTrans.size());
        for(BinhTanDgdTransTrackingDb.Obj binhTanTrans : listTrans)
        {
            if(StringConstUtil.IOS_OS.equalsIgnoreCase(binhTanTrans.os))
            {
                count++;
            }
        }
        log.add("total IOS of DGD ", agentNumber + " is " + count);
        return count;
    }
    private String checkOS(String extraKey)
    {
        String os = extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL).length > 1 ? StringConstUtil.ANDROID_OS : StringConstUtil.IOS_OS;
        return os;
    }

    private JsonObject createJsonNotification(String storeNumber, String content, String title)
    {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.StandardNoti.CAPTION, title);
        jo.putString(StringConstUtil.StandardNoti.BODY, content);
        jo.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, storeNumber);
        jo.putNumber(StringConstUtil.StandardNoti.TRAN_ID, System.currentTimeMillis());
        return jo;
    }

    private long getHours(long timeRange)
    {
        double ratio = (double)timeRange/(1000L * 60 * 60);
        long time = (long)Math.ceil(ratio);
        return time;
    }

    /**
     *
     * @param storeNumber
     * @param content
     * @param title
     * @param url
     * @return
     */
    private JsonObject createJsonNotificationRedirect(String storeNumber, String content, String title, String url)
    {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.RedirectNoti.CAPTION, title);
        jo.putString(StringConstUtil.RedirectNoti.BODY, content);
        jo.putString(StringConstUtil.RedirectNoti.RECEIVER_NUMBER, storeNumber);
        jo.putNumber(StringConstUtil.RedirectNoti.TRAN_ID, 0L);
        jo.putString(StringConstUtil.RedirectNoti.URL, url);
        return jo;
    }

    private void updateNotiTime(String phoneNumber, int time) {

        JsonObject joUpdate = new JsonObject();
        joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.NOTI_TIMES, time);
        joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_OF_NOTI_FIRE, System.currentTimeMillis());

        switch (time) {
            case 7:
                joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_7_DAY, true);
                break;
            case 14:
                joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_14_DAY, true);
                break;
            case 21:
                joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_21_DAY, true);
                break;
            case 28:
                joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_28_DAY, true);
                break;
            case 35:
                joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.NOTI_35_DAY, true);
                break;
        }

        acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {

            }
        });
    }

}
