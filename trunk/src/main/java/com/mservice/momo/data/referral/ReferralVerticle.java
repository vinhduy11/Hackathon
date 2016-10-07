package com.mservice.momo.data.referral;

import com.mservice.momo.data.*;
import com.mservice.momo.data.binhtanpromotion.DeviceDataUserDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
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
import com.mservice.momo.vertx.models.Notification;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Created by concu on 4/6/16.
 */
public class ReferralVerticle extends Verticle {
    private Logger logger;
    private JsonObject glbCfg;
    private TransDb tranDb;
    private GiftManager giftManager;
    private boolean isStoreApp;
    private ReferralV1CodeInputDb referralV1CodeInputDb;
    private ReferralV1TransactionsTrackingDb referralV1TransactionsTrackingDb;
    private ErrorPromotionTrackingDb errorPromotionTrackingDb;
    private DeviceDataUserDb deviceDataUserDb;
    private PromotionCountTrackingDb promotionCountTrackingDb;
    private MappingWalletBankDb mappingWalletBankDb;
    private PhonesDb phonesDb;

    private boolean remindNoti;
    private boolean isUAT;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        giftManager = new GiftManager(vertx, logger, glbCfg);
        referralV1CodeInputDb = new ReferralV1CodeInputDb(vertx, logger);
        referralV1TransactionsTrackingDb = new ReferralV1TransactionsTrackingDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        deviceDataUserDb = new DeviceDataUserDb(vertx, logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        promotionCountTrackingDb = new PromotionCountTrackingDb(vertx, logger);
        mappingWalletBankDb = new MappingWalletBankDb(vertx, logger);
        isUAT = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
        remindNoti = glbCfg.getBoolean(StringConstUtil.SEND_REMIND_NOTI, false);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);

        if(remindNoti)
        {
            remindUserCashInPeriodic();
        }

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final ReferralPromotionObj referralPromotionObj = new ReferralPromotionObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(referralPromotionObj.phoneNumber);
                log.add("phoneNumber" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralPromotionObj.phoneNumber);
                log.add("tranId"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralPromotionObj.tid);
                log.add("serviceID"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralPromotionObj.serviceId);
                log.add("group"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralPromotionObj.source);
                log.add("amount"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralPromotionObj.amount);
                log.add("extra"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralPromotionObj.joExtra.toString());
                final String bankType = referralPromotionObj.joExtra.getString(StringConstUtil.TYPE, "");
                JsonObject joReferral = referralPromotionObj.joExtra.getObject(StringConstUtil.ReferralVOnePromoField.REFERRAL_OBJ, new JsonObject());
                final ReferralV1CodeInputDb.Obj referralField = new ReferralV1CodeInputDb.Obj(joReferral);
                log.add("info joReferral " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, joReferral.toString());
                final JsonObject jsonReply = new JsonObject();
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                final long mappingTimeUser = referralPromotionObj.joExtra.getLong(colName.MappingWalletBank.MAPPING_TIME, 0);
                if(isStoreApp)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong danh co app DGD");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Khong danh cho app EU");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }

                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        boolean enableGiftGiving = false;
//                        long promo_start_date = 0;
//                        long promo_end_date = 0;
                        long currentTime = System.currentTimeMillis();
                        String agent = "";
                        long giftAmount = 0;
                        long moneyAmount = 0;
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj promoObj = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
                                promoObj = new PromotionDb.Obj((JsonObject) o);

                                if (promoObj.NAME.equalsIgnoreCase(referralPromotionObj.source)) {
//                                    promo_start_date = promoObj.DATE_FROM;
//                                    promo_end_date = promoObj.DATE_TO;
                                    log.add("Thong tin promo : " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, promoObj.toJsonObject());
                                    agent = promoObj.ADJUST_ACCOUNT;
                                    giftAmount = promoObj.TRAN_MIN_VALUE;
                                    moneyAmount = promoObj.PER_TRAN_VALUE;
                                    break;
                                }
                            }
                            final PromotionDb.Obj referralProgram = "".equalsIgnoreCase(agent) ? null : promoObj;
                            //Check lan nua do dai chuoi ki tu
                            if ("".equalsIgnoreCase(agent) || referralProgram == null) {
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                JsonObject jsonReply = new JsonObject();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else if ("".equalsIgnoreCase(referralPromotionObj.phoneNumber) || DataUtil.strToLong(referralPromotionObj.phoneNumber) <= 0) {
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai la so dien thoai khong co that.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                //Cong Nguyen change BA 01/08/2016
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if(mappingTimeUser < referralProgram.DATE_FROM && mappingTimeUser > 0)
                            {
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Map ví trước thời gian khuyến mãi nên không trả thưởng ");
                                JsonObject jsonReply = new JsonObject();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Map ví trước thời gian khuyến mãi nên không trả thưởng " + mappingTimeUser);
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if(referralPromotionObj.source.equalsIgnoreCase(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM)){
                                if(currentTime < referralProgram.DATE_FROM || currentTime > referralProgram.DATE_TO)
                                {
                                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                    //Cong Nguyen change BA 01/08/2016
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Đã hết thời gian triển khai chương trình " + referralProgram.NOTI_CAPTION + ". Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h");
                                    message.reply(jsonReply);
                                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Ngoai thoi gian khuyen mai, eo tra thuong.");
                                    log.writeLog();
                                    return;
                                }
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra thuong cho user.");
                                if (checkAbstractVisaCard(referralProgram, referralPromotionObj, log))
                                {
                                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Vuong the ao, khong tra qua.");
                                    message.reply(jsonReply);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "The ao nen khong tra thuong " + referralPromotionObj.joExtra.toString());
                                    return;
                                }
                                if(StringConstUtil.PHONES_BANKID_SBS.equalsIgnoreCase(bankType) && referralProgram.ENABLE_PHASE2)
                                {
                                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Dang kich hoat chan visa khong tra thuong");
                                    message.reply(jsonReply);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Hien tai khong cho phep tham gia chuong trinh visa, vui long thuc hien lai sau thoi gian nghi le ");
                                    return;
                                }
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Lay thong tin phone .");
                                final PhonesDb.Obj phoneObj = new PhonesDb.Obj(referralPromotionObj.joExtra.getObject(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ, new JsonObject()));
                                if(phoneObj == null || phoneObj.number == 0)
                                {
                                    return;
                                }
                                else {
                                    checkBankIsAccepted(phoneObj.bank_code, referralProgram, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean res) {
                                            if(!res) {
                                                log.add("desc "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So DGT map ngan hang ngoai danh sach accept");
                                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Tài khoản " + phoneObj.number + " map với ngân hàng không có trong danh sách được chấp nhận, bank id: " + phoneObj.bank_code);
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Ví map với ngân hàng không nằm trong danh sách được chấp nhận bank_code: " + phoneObj.bank_code );
                                                message.reply(jsonReply);
                                                log.writeLog();
                                                return;
                                            } else {
                                                //configure check sim
                                                boolean isCheckSim = referralProgram.EXTRA.getBoolean("is_check_sim",false);
                                                getExtraKeyFromApp(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralPromotionObj.phoneNumber, phoneObj.deviceInfo, isCheckSim, log, phoneObj.phoneOs, referralPromotionObj.last_imei, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject joResponse) {
                                                        int err = joResponse.getInteger(StringConstUtil.ERROR, 1000);
                                                        String desc = joResponse.getString(StringConstUtil.DESCRIPTION, "ERROR");
                                                        if(err == 0)
                                                        {
                                                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, desc);
                                                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "bonusForUser");
                                                            promotionCountTrackingDb.findAndIncCountUser(referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralField.inviteeCardInfo, new Handler<PromotionCountTrackingDb.Obj>() {
                                                                @Override
                                                                public void handle(PromotionCountTrackingDb.Obj promoCountTracking) {
                                                                    if(promoCountTracking.count == 1)
                                                                    {
                                                                        bonusForUser(referralProgram.ADJUST_ACCOUNT, referralProgram, referralProgram.PER_TRAN_VALUE, referralProgram.TRAN_MIN_VALUE, referralProgram.ADJUST_ACCOUNT, referralField, log, jsonReply, message, referralPromotionObj);
                                                                    }
                                                                    else {
                                                                        log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tài khoản này đã được nhận thưởng.");
                                                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Tài khoản này đã được nhận thưởng. " + referralPromotionObj.phoneNumber);
                                                                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Tài khoản này đã được nhận thưởng. " + referralPromotionObj.phoneNumber);
                                                                        message.reply(jsonReply);
                                                                        log.writeLog();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else {
                                                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, desc);
                                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, desc + " " + referralPromotionObj.phoneNumber);
                                                            log.writeLog();
                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, desc);
                                                            message.reply(jsonReply);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                                return;
                            } else if(referralPromotionObj.source.equalsIgnoreCase(StringConstUtil.ReferralVOnePromoField.REFERRAL_CASHBACK_PROGRAM)) {
                                //Khong tra thuong cho user khi cashback.
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "cashback referral cho user.");
//                                String currentCardInfo = referralPromotionObj.joExtra.getString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, "");
//                                executeReferralV1CashbackPromotion(currentTime, referralProgram, log, referralPromotionObj, jsonReply, message, referralField, currentCardInfo);
                                return;
                            } else {
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong ton tai chuong trinh.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                //Cong Nguyen change BA 01/08/2016
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                        }
                    }
                });
            }
        };

        vertx.eventBus().registerLocalHandler(AppConstant.REFERRAL_PROMOTION_BUS_ADDRESS, myHandler);
    }

    private void remindUserCashInPeriodic() {
        logger.info("Join create timer");
        final Calendar calendar = Calendar.getInstance();
        if(!isUAT)
        {
            logger.info("khong phai moi truong test ----> run luc 9h sang");
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 00);
            calendar.set(Calendar.SECOND, 00);

        }

        //se chay luc 9h, neu start BE sau 9h se chay vao 9h ngay hom
        long delayTime = calendar.getTimeInMillis() - System.currentTimeMillis() < 1 ? (calendar.getTimeInMillis() + (24 * 60 * 60 * 1000)) - System.currentTimeMillis(): calendar.getTimeInMillis() - System.currentTimeMillis();
        if(isUAT) {
            delayTime = 1;
        }
        final long timeToFire = isUAT ? 1000L * 60 * 5 : 1000L * 60 * 60 * 24;

        logger.info("timeToFire ----> " + timeToFire + " delayTime ---->" + delayTime);

        vertx.setTimer(delayTime, new Handler<Long>() {
            @Override
            public void handle(Long delayTime1) {
                logger.info("Join run kiem tra chuong trinh referral");

                vertx.setPeriodic(timeToFire, new Handler<Long>() {
                    @Override
                    public void handle(final Long checkInfoTime) {
                        logger.info("Start run kiem tra chuowng trinh referral");
                        remindUserCashIn();
                    }
                });
                remindUserCashIn();

                logger.info("cancel timer kiem tra chuong trinh referral");
                vertx.cancelTimer(delayTime1);
            }
        });
    }

    public void remindUserCashIn() {
        //Cong Nguyen 11/07/2016 Notify automatic cho nguoi gioi thieu
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        //get list promotion
//        logger.info("Get list promotion");
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                final JsonArray array = json.getArray("array", null);
                if (array != null && array.size() > 0) {
//                    PromotionDb.Obj promoObj = null;
                    final AtomicInteger countPromo = new AtomicInteger(array.size());
                    long proId = vertx.setPeriodic(200L, new Handler<Long>() {
                        @Override
                        public void handle(Long countTimer) {
                            int position = countPromo.decrementAndGet();
//                            logger.info("REMIND .... ");
                            if (position < 0) {
//                                logger.info("Khong co bat ky khuyen mai nao");
                                vertx.cancelTimer(countTimer);
                            } else {
//                                logger.info("Kiem tra khuyen mai");
                                JsonObject o = array.get(position);
                                final PromotionDb.Obj promoObj = new PromotionDb.Obj(o);
                                final String url = "https://momo.vn/chiasemomo/huong-dan-lien-ket.html";
                                if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM)) {

                                    long current = System.currentTimeMillis();
                                    if(current< promoObj.DATE_FROM || current > promoObj.DATE_TO)
                                        return;
//                                    logger.info("Chay kiem tra cho chuowng trinh gioi thieu ban be");
                                    //notify cho app cua nguoi gioi thieu
                                    //case 3 ngày chưa map
                                    long dateTime = promoObj.DATE_TO;
                                    String dateString = DataUtil.convertTime(dateTime);
                                    final String dateStringPlus = Misc.dateVNFormatWithDot(dateTime + 24 * 60 * 60 * 1000);

                                    JsonObject joFilter3D = new JsonObject();
                                    //kiem tra ngay tranh truong hop noti nhieu lan
                                    long timeToCheckSTart3D = current - (3 * 1000 * 60 * 60 * 24);
                                    long timeToCheckEnd3D = current - (4 * 1000 * 60 * 60 * 24);
                                    logger.info("Check time 3D: " + timeToCheckSTart3D + " - " + timeToCheckEnd3D);
                                    JsonArray jarrAnd3D = new JsonArray();

                                    JsonObject joTimeStart3D = new JsonObject();
                                    joTimeStart3D.putNumber(MongoKeyWords.LESS_OR_EQUAL, timeToCheckSTart3D);

                                    JsonObject jTimeStart3D = new JsonObject();
                                    jTimeStart3D.putObject(colName.ReferralV1CodeInputCol.INPUT_TIME, joTimeStart3D);

                                    JsonObject joTimeEnd3D = new JsonObject();
                                    joTimeEnd3D.putNumber(MongoKeyWords.GREATER_OR_EQUAL, timeToCheckEnd3D);

                                    JsonObject jTimeEnd3D = new JsonObject();
                                    jTimeEnd3D.putObject(colName.ReferralV1CodeInputCol.INPUT_TIME, joTimeEnd3D);

                                    JsonObject jCardInfo = new JsonObject();
                                    jCardInfo.putString(colName.ReferralV1CodeInputCol.IS_MAPPED, null);

                                    JsonObject jLock = new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.LOCK, null);

                                    JsonObject jMap = new JsonObject();
                                    jMap.putString(colName.ReferralV1CodeInputCol.MAPPING_TIME, null);

                                    jarrAnd3D.add(jTimeStart3D);
                                    jarrAnd3D.add(jTimeEnd3D);
                                    jarrAnd3D.add(jCardInfo);
                                    jarrAnd3D.add(jLock);
                                    jarrAnd3D.add(jMap);
                                    joFilter3D.putArray(MongoKeyWords.AND_$, jarrAnd3D);

//                                    logger.info("Check log send noti sau 3 ngay: " + joFilter3D);

                                    referralV1CodeInputDb.searchWithFilter(joFilter3D, new Handler<ArrayList<ReferralV1CodeInputDb.Obj>>() {
                                        @Override
                                        public void handle(final ArrayList<ReferralV1CodeInputDb.Obj> res) {
                                            final AtomicInteger count = new AtomicInteger(res.size());

                                            vertx.setPeriodic(200L, new Handler<Long>() {
                                                @Override
                                                public void handle(Long countTimer) {
                                                    int position = count.decrementAndGet();
                                                    if (position < 0) {
//                                                        logger.info("REMIND .... no user");
                                                        vertx.cancelTimer(countTimer);
                                                        return;
                                                    } else {
                                                        ReferralV1CodeInputDb.Obj obj = res.get(position);
                                                        String bodyMessage = PromoContentNotification.NOTI_AUTO_REFFERAL_UA_3D_BODY.replaceAll("%contact%", obj.inviteeNumber);
                                                        Misc.sendRedirectNoti(vertx, createJsonNotification(obj.inviterNumber, bodyMessage, PromoContentNotification.CHIA_SE_MOMO_TITLE, url));
                                                        referralV1CodeInputDb.findAndUpdateInfoUser(obj.inviteeNumber, new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.NOTI3DAY, true), new Handler<ReferralV1CodeInputDb.Obj>() {
                                                            @Override
                                                            public void handle(ReferralV1CodeInputDb.Obj event) {
//                                                                logger.info("add field NOTI3DAY to true, send noti once time");
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                        }
                                    });

                                    /*****************************************5 ngay chua map****************************************************/
                                    JsonObject joFilter5DLink = new JsonObject();
                                    //kiem tra ngay tranh truong hop noti nhieu lan
                                    long timeToCheckSTart5D = current - (5 * 1000 * 60 * 60 * 24);
                                    long timeToCheckEnd5D = current - (6 * 1000 * 60 * 60 * 24);

//                                    logger.info("Check time 5D: " + timeToCheckSTart5D + " - " + timeToCheckEnd5D);
                                    JsonArray jarrAnd5DLink = new JsonArray();


                                    JsonObject jSmsNull = new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.SMS, null);
                                    JsonObject jSmsFalse = new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.SMS, false);
                                    JsonArray JSmsOrArr = new JsonArray().add(jSmsNull).add(jSmsFalse);
                                    JsonObject jSmsOrObj = new JsonObject().putArray(MongoKeyWords.OR, JSmsOrArr);

                                    JsonObject joTimeStart5D = new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, timeToCheckSTart5D);
                                    JsonObject jTimeStart5D = new JsonObject().putObject(colName.ReferralV1CodeInputCol.INPUT_TIME, joTimeStart5D);
                                    JsonObject joTimeEnd5D = new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, timeToCheckEnd5D);
                                    JsonObject jTimeEnd5D = new JsonObject().putObject(colName.ReferralV1CodeInputCol.INPUT_TIME, joTimeEnd5D);

                                    JsonObject jSmsMappedNull = new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.SMS_MAPPED, null);
                                    JsonObject jSmsMappedFalse = new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.SMS_MAPPED, false);
                                    JsonArray JSmsMappedOrArr = new JsonArray().add(jSmsMappedNull).add(jSmsMappedFalse);
                                    JsonObject jSmsMappedOrObj = new JsonObject().putArray(MongoKeyWords.OR, JSmsMappedOrArr);

                                    JsonObject joTimeStart5DMAP = new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, timeToCheckSTart5D);
                                    JsonObject jTimeStart5DMAP = new JsonObject().putObject(colName.ReferralV1CodeInputCol.MAPPING_TIME, joTimeStart5DMAP);
                                    JsonObject joTimeEnd5DMAP = new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, timeToCheckEnd5D);
                                    JsonObject jTimeEnd5DMAP = new JsonObject().putObject(colName.ReferralV1CodeInputCol.MAPPING_TIME, joTimeEnd5DMAP);

                                    JsonObject jo5DayMapped = new JsonObject().putNumber(colName.ReferralV1CodeInputCol.MAPPING_TIME, null);
                                    JsonArray jArrAnd5DayCheckInputTime = new JsonArray().add(jTimeStart5D).add(jTimeEnd5D).add(jo5DayMapped).add(jSmsOrObj);
                                    JsonObject joAnd5DayCheckInputTime = new JsonObject().putArray(MongoKeyWords.AND_$, jArrAnd5DayCheckInputTime);

                                    /************/
                                    JsonArray jArrAnd5DayCheckMappingTime = new JsonArray().add(jTimeStart5DMAP).add(jTimeEnd5DMAP).add(jSmsMappedOrObj);
                                    JsonObject joAnd5DayCheckMappingTime = new JsonObject().putArray(MongoKeyWords.AND_$, jArrAnd5DayCheckMappingTime);

                                    JsonArray jArrOr5DayCheckingTime = new JsonArray().add(joAnd5DayCheckInputTime).add(joAnd5DayCheckMappingTime);
                                    JsonObject joOr5DayCheckingTime = new JsonObject().putArray(MongoKeyWords.OR, jArrOr5DayCheckingTime);

                                    JsonObject jCardInfo5DLink = new JsonObject().putString(colName.ReferralV1CodeInputCol.IS_MAPPED, null);

                                    jarrAnd5DLink.add(joOr5DayCheckingTime).add(jCardInfo5DLink).add(jLock);
                                    joFilter5DLink.putArray(MongoKeyWords.AND_$, jarrAnd5DLink);

//                                    logger.info("Check log send noti sau 5 ngay chua link: " + joFilter5DLink);

                                    referralV1CodeInputDb.searchWithFilter(joFilter5DLink, new Handler<ArrayList<ReferralV1CodeInputDb.Obj>>() {
                                        @Override
                                        public void handle(final ArrayList<ReferralV1CodeInputDb.Obj> res) {
                                            final AtomicInteger count = new AtomicInteger(res.size());
//                                            logger.info("Referral Check size array user chua map ngan hang sau 5 ngay: " + res.size());
                                            //get them info tu bang khac de kiem tra la chua maping hay chua cashin
                                            //add list invitee_number to fletch data from phonesdb
                                            JsonArray array = new JsonArray();
                                            final ArrayList<Integer> listPhone = new ArrayList<Integer>();
                                            for (ReferralV1CodeInputDb.Obj obj: res) {
//                                                logger.info("REMIND 5 >>>>>>>>>>>> " + obj.inviteeNumber);
                                                int num = Integer.parseInt(obj.inviteeNumber);
                                                array.addNumber(num);
                                                listPhone.add(num);
                                            }

                                            phonesDb.getPhoneListFull(listPhone, null, new Handler<ArrayList<PhonesDb.Obj>>() {
                                                @Override
                                                public void handle(final ArrayList<PhonesDb.Obj> array) {
                                                    final AtomicInteger count = new AtomicInteger(array.size());

//                                                    logger.info("Arr>>>>>>>>>>>>>>>>> " + array.size());
                                                    vertx.setPeriodic(200L, new Handler<Long>() {
                                                        @Override
                                                        public void handle(Long countTimer) {
                                                            int position = count.decrementAndGet();
//                                                            logger.info(">>>>>>>>>>>>>>>>> " + position);
                                                            if (position < 0) {
//                                                                logger.info("REMIND .... no user");
                                                                vertx.cancelTimer(countTimer);
                                                            } else {
//                                                                logger.info(">>>>>>>>>>>>>>>>> " + position);
                                                                PhonesDb.Obj obj = array.get(position);
                                                                if((obj.bank_code == null || obj.bank_code.equalsIgnoreCase("") || obj.bank_code.equalsIgnoreCase("0") || obj.bank_code == "")) {
//                                                                    logger.info("chua mapping voi ngan hang lien ket sau 5 ngay " + obj.toJsonObject());
                                                                    String bodyMessage = PromoContentNotification.NOTI_AUTO_REFFERAL_UA_5D_LINK_BODY;
                                                                    Misc.sendSms(vertx, obj.number, bodyMessage);
                                                                    referralV1CodeInputDb.findAndUpdateInfoUser("0" + obj.number, new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.SMS, true), new Handler<ReferralV1CodeInputDb.Obj>() {
                                                                        @Override
                                                                        public void handle(ReferralV1CodeInputDb.Obj event) {
//                                                                            logger.info("add field sms to true, send sms once time");
                                                                        }
                                                                    });
                                                                } else {
//                                                                    logger.info("chua cash in tu ngan hang lien ket sau 5 ngay " + obj.toJsonObject());
                                                                    String bodyMessage = PromoContentNotification.NOTI_AUTO_REFFERAL_UA_5D_CASHIN_BODY;
                                                                    Misc.sendSms(vertx, obj.number, bodyMessage);
                                                                    referralV1CodeInputDb.findAndUpdateInfoUser("0" + obj.number, new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.SMS_MAPPED, true), new Handler<ReferralV1CodeInputDb.Obj>() {
                                                                        @Override
                                                                        public void handle(ReferralV1CodeInputDb.Obj event) {
//                                                                            logger.info("add field sms to true, send sms mapped once time");
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });


                                    /*****************************************1 ngay****************************************************/
                                    JsonObject joFilter1DLink = new JsonObject();
                                    //kiem tra ngay tranh truong hop noti nhieu lan
                                    long timeToCheckSTart1D = current - (1 * 1000 * 60 * 60 * 24);
                                    long timeToCheckEnd1D = current - (2 * 1000 * 60 * 60 * 24);

//                                    logger.info("Check time 1D: " + timeToCheckSTart1D + " - " + timeToCheckEnd1D);

                                    JsonArray jarrAnd1DLink = new JsonArray();

                                    JsonObject joTimeStart1D = new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, timeToCheckSTart1D);
//                                    logger.info("show joTimeStart1D time check object: " + joTimeStart1D);
                                    JsonObject jTimeStart1D = new JsonObject().putObject(colName.ReferralV1CodeInputCol.INPUT_TIME, joTimeStart1D);
//                                    logger.info("show joTimeStart1D time check object: " + joTimeStart1D);
                                    JsonObject joTimeEnd1D = new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, timeToCheckEnd1D);
//                                    logger.info("show timeToCheckEnd1D time check object: " + timeToCheckEnd1D);
                                    JsonObject jTimeEnd1D = new JsonObject().putObject(colName.ReferralV1CodeInputCol.INPUT_TIME, joTimeEnd1D);
//                                    logger.info("show joTimeEnd1D time check object: " + joTimeEnd1D);


                                    JsonObject joTimeStart1DMAP = new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, timeToCheckSTart1D);
                                    JsonObject jTimeStart1DMAP = new JsonObject().putObject(colName.ReferralV1CodeInputCol.MAPPING_TIME, joTimeStart1DMAP);
                                    JsonObject joTimeEnd1DMAP = new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, timeToCheckEnd1D);
                                    JsonObject jTimeEnd1DMAP = new JsonObject().putObject(colName.ReferralV1CodeInputCol.MAPPING_TIME, joTimeEnd1DMAP);

                                    JsonObject jo1DayMapped = new JsonObject().putNumber(colName.ReferralV1CodeInputCol.MAPPING_TIME, null);
                                    JsonArray jArrAnd1DayCheckInputTime = new JsonArray().add(jTimeStart1D).add(jTimeEnd1D).add(jo1DayMapped);
                                    JsonObject joAnd1DayCheckInputTime = new JsonObject().putArray(MongoKeyWords.AND_$, jArrAnd1DayCheckInputTime);
                                    JsonArray jArrAnd1DayCheckMappingTime = new JsonArray().add(jTimeStart1DMAP).add(jTimeEnd1DMAP);
                                    JsonObject joAnd1DayCheckMappingTime = new JsonObject().putArray(MongoKeyWords.AND_$, jArrAnd1DayCheckMappingTime);

                                    JsonArray jArrOr1DayCheckingTime = new JsonArray().add(joAnd1DayCheckInputTime).add(joAnd1DayCheckMappingTime);
                                    JsonObject joOr1DayCheckingTime = new JsonObject().putArray(MongoKeyWords.OR, jArrOr1DayCheckingTime);

                                    JsonObject jCardInfo1DLink = new JsonObject();
                                    jCardInfo1DLink.putString(colName.ReferralV1CodeInputCol.IS_MAPPED, null);

                                    jarrAnd1DLink.add(joOr1DayCheckingTime).add(jCardInfo1DLink).add(jLock);
                                    joFilter1DLink.putArray(MongoKeyWords.AND_$, jarrAnd1DLink);
//                                    logger.info("Check log send noti sau 1 ngay chua link: " + joFilter1DLink);

                                    referralV1CodeInputDb.searchWithFilter(joFilter1DLink, new Handler<ArrayList<ReferralV1CodeInputDb.Obj>>() {
                                        @Override
                                        public void handle(final ArrayList<ReferralV1CodeInputDb.Obj> res) {

                                            JsonArray array = new JsonArray();
                                            ArrayList<Integer> listPhone = new ArrayList<Integer>();
                                            for (ReferralV1CodeInputDb.Obj obj: res) {
//                                                logger.info("REMIND 1 >>>>>>>>>>>> " + obj.inviteeNumber);
                                                int num = Integer.parseInt(obj.inviteeNumber);
                                                array.addNumber(num);
                                                listPhone.add(num);
                                            }

                                            JsonObject query = new JsonObject().putObject("",new JsonObject().putArray(MongoKeyWords.IN_$,array));

                                            phonesDb.getPhoneListFull(listPhone, null, new Handler<ArrayList<PhonesDb.Obj>>() {
                                                @Override
                                                public void handle(final ArrayList<PhonesDb.Obj> array) {
                                                    final AtomicInteger count = new AtomicInteger(array.size());

                                                    vertx.setPeriodic(200L, new Handler<Long>() {
                                                        @Override
                                                        public void handle(Long countTimer) {
                                                            int position = count.decrementAndGet();
                                                            if (position < 0) {
//                                                                logger.info("REMIND .... no user");
                                                                vertx.cancelTimer(countTimer);
                                                            } else {
                                                                PhonesDb.Obj obj = array.get(position);
                                                                if(obj.bank_code == null || obj.bank_code.equalsIgnoreCase("") || obj.bank_code.equalsIgnoreCase("0") || obj.bank_code == "") {
//                                                                    logger.info("chua mapping voi ngan hang lien ket sau 1 ngay: " + obj.toJsonObject());
                                                                    String bodyMessage = PromoContentNotification.NOTI_AUTO_REFFERAL_UA_1D_LINK_BODY;
                                                                    Misc.sendRedirectNoti(vertx, createJsonNotification("0" + obj.number, bodyMessage, PromoContentNotification.CHIA_SE_MOMO_TITLE, url));
                                                                } else {
//                                                                    logger.info("chua cash in tu ngan hang lien ket sau 1 ngay: " + obj.toJsonObject());
                                                                    String bodyMessage = PromoContentNotification.NOTI_AUTO_REFFERAL_UA_1D_CASHIN_BODY;
                                                                    Misc.sendRedirectNoti(vertx, createJsonNotification("0" + obj.number, bodyMessage, PromoContentNotification.THEM_BAN_THEM_QUA_TITLE, url));
                                                                }
                                                                referralV1CodeInputDb.findAndUpdateInfoUser("0" + obj.number, new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.NOTI1DAY, true), new Handler<ReferralV1CodeInputDb.Obj>() {
                                                                    @Override
                                                                    public void handle(ReferralV1CodeInputDb.Obj event) {
//                                                                        logger.info("add field NOTI1DAY to true, send noti once time");
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        });

    }

    private void executeReferralV1CashbackPromotion(long currentTime, final PromotionDb.Obj referralProgram, final Common.BuildLog log, final ReferralPromotionObj referralPromotionObj, final JsonObject jsonReply, final Message<JsonObject> message, final ReferralV1CodeInputDb.Obj referralField, final String currentCardInfo) {
        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "cashback 20% cho inviter");
        log.add("referral program " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralProgram.toJsonObject());
//        final ReferralV1CodeInputDb.Obj referralObj = new ReferralV1CodeInputDb.Obj(referralPromotionObj.joExtra);

        long cashbackEndTime = referralField.bonus_time + 1000L * 60 * 60 * 24 * referralProgram.DURATION;
        if(!referralField.isMapped || "".equalsIgnoreCase(referralField.inviterNumber) || "".equalsIgnoreCase(referralField.inviteeNumber) || "".equalsIgnoreCase(referralField.imei_code))
        {
            log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "data referral is not right");
            log.writeLog();
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thieu thong tin cashback " + referralPromotionObj.phoneNumber);
            return;
        }
        else if(cashbackEndTime < currentTime)
        {
            log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "END TIME CASH BACK REFERRAL");
            log.writeLog();
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralField.inviterNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "het thoi gian cashback");
            return;
        }

        //Count so lan nhan thuong cua user
        JsonObject joFilter = new JsonObject();
        joFilter.putString(colName.ReferralV1TransactionsTrackingCol.INVITER_NUMBER, referralField.inviterNumber);
        joFilter.putString(colName.ReferralV1TransactionsTrackingCol.INVITEE_NUMBER, referralField.inviteeNumber);
        logger.info("data filter" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " + joFilter.toString());
        referralV1TransactionsTrackingDb.searchWithFilter(joFilter, new Handler<ArrayList<ReferralV1TransactionsTrackingDb.Obj>>() {
            @Override
            public void handle(ArrayList<ReferralV1TransactionsTrackingDb.Obj> listCashBacksTrans) {
                   final long cashBachMoney = getCashBackMoney(listCashBacksTrans, referralProgram, referralPromotionObj.amount, log);
                    if(cashBachMoney <= 0)
                    {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "OVERLOAD MAX VALUE BONUS");
                        log.writeLog();
                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralField.inviterNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Hoan tra tien day du, khong tra cashback nua");
                        return;
                    }
                    giveBonusMoneyForInviter(referralProgram.ADJUST_ACCOUNT, cashBachMoney, referralField.inviterNumber, log, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joResponse) {
                            final int err = joResponse.getInteger(StringConstUtil.ERROR, -1);

                            if(err != 0)
                            {
                                log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra tien 100k tu core khong thanh cong.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Tra tien 100k tu core khong thanh cong.");
                                message.reply(jsonReply);
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralField.inviterNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Tra tien 100k tu core cho " + referralField.inviterNumber + " khong thanh cong");
                                return;
                            }
                            final long moneyTranId = joResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                            log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thanh toan xong inviter " + referralField.inviterNumber);
                            ReferralV1TransactionsTrackingDb.Obj referraTrackingObj = new ReferralV1TransactionsTrackingDb.Obj();
                            referraTrackingObj.inviterNumber = referralField.inviterNumber;
                            referraTrackingObj.bonus_amount = cashBachMoney;
                            referraTrackingObj.bonus_tid = moneyTranId;
                            referraTrackingObj.cardInfo = currentCardInfo;
                            referraTrackingObj.bonus_time = System.currentTimeMillis();
                            referraTrackingObj.inviteeNumber = referralField.inviteeNumber;
                            referraTrackingObj.tran_id = referralPromotionObj.tid;
                            referraTrackingObj.tran_amount = referralPromotionObj.amount;
                            referraTrackingObj.bank_code = referralPromotionObj.joExtra.getString(colName.ReferralV1CodeInputCol.INVITEE_BANK_CODE, "");
                            referralV1TransactionsTrackingDb.insert(referraTrackingObj, new Handler<Integer>() {
                                @Override
                                public void handle(Integer event) {
                                    String notiBody = String.format(referralProgram.NOTI_BODY_INVITER, Misc.formatAmount(cashBachMoney), referralField.inviteeNumber);
                                    fireMoneyNotiAndSendTranHist(referralField.inviterNumber,cashBachMoney, moneyTranId, log, referralProgram.NOTI_CAPTION, notiBody, notiBody
                                            , referralProgram.INTRO_DATA, 1);
                                }
                            });

                        }
                    });
            }
        });
        return;
    }

    private boolean checkAbstractVisaCard(PromotionDb.Obj referralProgram, ReferralPromotionObj referralPromotionObj, Common.BuildLog log) {
        String[] pins = referralProgram.ADJUST_PIN.split(";");
        String bankAcc = referralPromotionObj.joExtra.getString(StringConstUtil.ReferralVOnePromoField.BANK_ACC, "");
        if(!"".equalsIgnoreCase(bankAcc))
        {
            for(int i = 0; i < pins.length; i++)
            {
                if(!"".equalsIgnoreCase(pins[i].toString().trim()) && bankAcc.contains(pins[i].toString().trim()))
                {
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Vuong thong tin the ao, khong tra thuong cho user.");
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Vuong thong tin the ao roi, khong tra thuong");
                    return true;
                }
            }
        }
        return false;
    }

    private long getCashBackMoney(ArrayList<ReferralV1TransactionsTrackingDb.Obj> listCashBacksTrans, PromotionDb.Obj referralProgram, long amount, Common.BuildLog log)
    {
        long cashBackMoney = 0;
        long total = 0;
        for(ReferralV1TransactionsTrackingDb.Obj cashBackTran: listCashBacksTrans)
        {
            total = total + cashBackTran.bonus_amount;
        }
        log.add("total cashback " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, total);
        cashBackMoney = referralProgram.TRAN_MIN_VALUE -  total;
        log.add("cashbackMoney cashback " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, cashBackMoney);
        log.add("amount cashback " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, amount);
        log.add("pertran cashback " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, referralProgram.PER_TRAN_VALUE);
        long cashback_tmp = amount * referralProgram.PER_TRAN_VALUE / 100;
        log.add("cashback_tmp cashback " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, cashback_tmp);
        if(cashBackMoney > 0)
        {
            cashBackMoney = cashBackMoney > cashback_tmp ? cashback_tmp : cashBackMoney;
        }
        log.add("cashBackMoney cashback again " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, cashBackMoney);
        return cashBackMoney;
    }

    private void bonusForUser(String agent, final PromotionDb.Obj referralProgram, final long bonus_money, final long bonus_gift, final String agent_final, final ReferralV1CodeInputDb.Obj referralField, final Common.BuildLog log, final JsonObject jsonReply, final Message<JsonObject> message, final ReferralPromotionObj referralPromotionObj) {
        giveBonusMoneyForInviter(agent, bonus_money, referralField.inviterNumber, log, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joMoneyResponse) {
                final int err = joMoneyResponse.getInteger(StringConstUtil.ERROR, -1);

                if(err != 0)
                {
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra tien 100k tu core khong thanh cong.");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Tra tien 100k tu core khong thanh cong.");
                    message.reply(jsonReply);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, err, "Tra tien 100k tu core khong thanh cong");
                    return;
                }
                final long moneyTranId = joMoneyResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thanh toan xong inviter " + referralField.inviterNumber);

                //Tra qua cho invitee
                log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra qua cho invitee " + referralField.inviteeNumber);
                List<String> listVoucher = new ArrayList<>();
                listVoucher.add(referralProgram.INTRO_SMS);
                giveVoucherForInvitee(message, bonus_gift, 30, jsonReply, agent_final, referralPromotionObj, referralProgram, listVoucher, log, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonGiftResponse) {
                        int error = jsonGiftResponse.getInteger(StringConstUtil.ERROR, -1);
                        long giftTid = jsonGiftResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);

                        if(error != 0)
                        {
                            log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra qua 100k tu core khong thanh cong.");
                            log.add("error" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, error);
                            log.add("gift tid" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, giftTid);
                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                            jsonReply.putString(StringConstUtil.DESCRIPTION, "Tra tien 100k tu core khong thanh cong.");
                            message.reply(jsonReply);
                        }
                        else {
                            logger.info("Tra qua thanh cong");
                            log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra qua tu core cho referral thanh cong " + referralField.inviteeNumber);
                            String endDate = Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * referralProgram.DURATION);
                            String notiBodyInviter = referralProgram.NOTI_BODY_INVITER.replace( "%contact%",referralField.inviteeNumber);
                            fireMoneyNotiAndSendTranHist(referralField.inviterNumber,bonus_money, moneyTranId, log, referralProgram.NOTI_CAPTION, notiBodyInviter, notiBodyInviter
                                    , referralProgram.INTRO_DATA, 1);
                            jsonReply.putNumber(StringConstUtil.ERROR, 0);
                            jsonReply.putString(StringConstUtil.DESCRIPTION, "Good.");
                            message.reply(jsonReply);
                        }
                        log.writeLog();
                        JsonObject joUpdate = new JsonObject();
                        if(!"".equalsIgnoreCase(referralPromotionObj.last_imei))
                        {
                            joUpdate.putString(colName.ReferralV1CodeInputCol.IMEI_CODE, referralPromotionObj.last_imei);
                        }
                        joUpdate.putNumber(colName.ReferralV1CodeInputCol.BONUS_TIME, System.currentTimeMillis());
                        joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITEE_BONUS_AMOUNT, bonus_gift);
                        joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITER_BONUS_AMOUNT, bonus_money);
                        joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITEE_BONUS_TID, giftTid);
                        joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITER_BONUS_TID, moneyTranId);
                        joUpdate.putBoolean(colName.ReferralV1CodeInputCol.IS_MAPPED, true);
                        referralV1CodeInputDb.updatePartial(referralField.inviteeNumber, joUpdate, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean event) {
                                logger.info("UPDATE DATA CHUONG TRINH REFERRAL THANH CONG " + referralField.inviteeNumber);
                                if(err != 0)
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, err, "Loi core");
                            }
                        });
                        return;
                    }
                });
            }
        });
    }

    private void giveBonusMoneyForInviter(final String agent, final long value_of_money, final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.setPhoneNumber(phoneNumber);
        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "getFeeFromStore");
        log.add("phone " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneNumber);
        log.add("agent " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, agent);
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM;
        Misc.adjustment(vertx, agent, phoneNumber, value_of_money,
                Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "core tra ket qua");
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            log.add("core tra loi " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, soapObjReply.error);
                            log.add("status " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, soapObjReply.status);
                            log.add("tid " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, soapObjReply.tranId);
                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "core tra loi");
//                            log.writeLog();
                            JsonObject jsonReply = new JsonObject();
                            jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "Khong tang duoc tien khach hang, core tra loi");
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
        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "fireMoneyNotiAndSendTranHist");

        JsonObject joTranHis = new JsonObject();
        joTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.PROMOTION_VALUE);
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

        // TODO: 12/07/2016  Cong Nguyen
        JsonObject joNoti = new JsonObject();
        joNoti.putString(StringConstUtil.StandardNoti.CAPTION, titleNoti);
        joNoti.putString(StringConstUtil.StandardNoti.BODY, bodyNoti);
        joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phoneNumber);
        joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tranId);
        //Cong Nguyen change 13/07/2016
        logger.info("send popup to user A khi user B cash in thanh cong");
        sendPopupReferral(phoneNumber, bodyNoti, titleNoti, tranId);

    }

    private void sendPopupReferral(String phoneNumber, String body, String title, long tranId) {
        JsonObject jsonExtra = new JsonObject();
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, false);
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, StringConstUtil.CONFIRM_BUTTON_TITLE);
        jsonExtra.putNumber(StringConstUtil.TYPE, 3);
        Notification notification = new Notification();
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = title;
        notification.body = body;
        notification.tranId = tranId;
        notification.cmdId = 0L;
        notification.time = System.currentTimeMillis();
        notification.receiverNumber = Integer.parseInt(phoneNumber);
        notification.extra = jsonExtra.toString();

        Misc.sendNoti(vertx, notification);
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void giveVoucherForInvitee(final Message<JsonObject> message
            , final long value_of_gift
            , final int time_for_gift
            , final JsonObject joReply
            , final String agent
            , final ReferralPromotionObj referralPromotionObj
            , final PromotionDb.Obj referralProgram
            , final List<String> listVoucher
            , final Common.BuildLog log
            , final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", referralPromotionObj.source));

        keyValues.add(new Misc.KeyValue("group", referralPromotionObj.source));



        log.add("TOTAL GIFT " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, listVoucher.size());
        log.add("TOTAL VALUE " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, value_of_gift);

        int timeForGift = time_for_gift;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , referralPromotionObj.phoneNumber
                , value_of_gift
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, error);
                log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, SoapError.getDesc(error));

                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    //tao gift tren backend
                    final GiftType giftType = new GiftType();
                    final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    final Misc.KeyValue kv = new Misc.KeyValue();

                    final long modifyDate = System.currentTimeMillis();
                    final String note = referralPromotionObj.source;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = referralPromotionObj.source;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                    log.add("so luong voucher " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, atomicInteger);

                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {
                            if (atomicInteger.decrementAndGet() < 0) {
                                log.add("func " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "out of range for invitee_number " + referralPromotionObj.phoneNumber);
                                log.writeLog();
                                vertx.cancelTimer(aPeriodicLong);
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                log.add("itemPosition " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, itemPosition);
                                giftType.setModelId(listVoucher.get(itemPosition).trim());
                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(referralPromotionObj.phoneNumber
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
                                        log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "tra thuong chuong trinh zalo bang gift");
                                        log.add("err " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, err);

                                        //------------tat ca thanh cong roi
                                        if (err == 0) {
                                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tao qua local referral thanh cong");
                                            giftManager.useGift(referralPromotionObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {
//                                                    Misc.sendTranHisAndNotiGiftForBillPay(vertx, DataUtil.strToInt(referralPromotionObj.phoneNumber), referralProgram.NOTI_BODY_INVITEE, tranId, value_of_gift, gift,
//                                                            referralProgram.NOTI_CAPTION, referralProgram.NOTI_BODY_INVITEE, referralProgram.NOTI_BODY_INVITEE, tranDb);
//                                                    String notiBody = String.format(referralProgram.NOTI_BODY_INVITEE, Misc.dateVNFormatWithDot(referralProgram.DATE_TO + 1000L * 60 * 60 * 24));
                                                    sendNotiAndTranHis(referralProgram.NOTI_CAPTION, referralProgram.NOTI_BODY_INVITEE, referralProgram.NOTI_BODY_INVITEE, referralProgram, referralPromotionObj, tranId, gift);
                                                    joReply.putNumber(StringConstUtil.ERROR, 0);
                                                    joReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId);
                                                    callback.handle(joReply);
                                                    return;
                                                }
                                            });
                                        } else {
                                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tao qua local referral fail");
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
                    log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Core loi");
                    log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Lỗi " + SoapError.getDesc(error));
                    log.add("Exception " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Exception " + SoapError.getDesc(error));
                    callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, error).putString(StringConstUtil.DESCRIPTION, "Hệ thống tạm thời gián đoạn. Vui lòng thực hiện lại sau 30 phút")
                            .putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId));
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, referralPromotionObj.phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, error, "Loi core");
                    return;
                }
            }
        });
    }

    private void sendNotiAndTranHis(String notiCaption, String notiBody, String tranComment, PromotionDb.Obj promoObj, ReferralPromotionObj referralPromotionObj, long tranId, Gift gift) {
//        String notiCaption = promoObj.NOTI_CAPTION;
//        String giftMessage = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
//        String tranComment = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
//        String notiBody = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
        String partnerName = promoObj.INTRO_DATA;
        String serviceId = "topup";

        Misc.sendTranHisAndNotiZaloMoney(vertx
                , DataUtil.strToInt(referralPromotionObj.phoneNumber)
                , tranComment
                , tranId
                , promoObj.PER_TRAN_VALUE
                , gift
                , notiCaption
                , notiBody
                , tranComment
                , partnerName
                , serviceId
                , tranDb);
    }

    public void getExtraKeyFromApp(final String program, final String phoneNumber, final String extraKey, boolean isCheckSim, final Common.BuildLog log,final String os, final String imei, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();
        log.add("extra key " + program, extraKey);

        String extraKeyAndroid = "";
        if(StringConstUtil.ANDROID_OS.equalsIgnoreCase(os))
        {
            final String[] extraKeyArr = extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL);
            log.add("extra keyArr length " + program, extraKeyArr.length);
            extraKeyAndroid = extraKeyArr.length > 1 ? extraKeyArr[0] : extraKey;
            log.add("extraKeyAndroid " + program, extraKeyAndroid);

            //KIEM TRA ANDROID moi
            String extraKeyTemp = extraKeyArr.length > 1 ? extraKeyArr[1] : "";
            log.add("extraKeyTemp " + program, extraKeyTemp);
            if(!"".equalsIgnoreCase(extraKeyTemp))
            {
                String []extraKeyTempArr = extraKeyTemp.split(MomoMessage.BELL);
                int count = 0;
                boolean isKilled = false;
                for(int i = 0; i < extraKeyTempArr.length; i++)
                {
                    if("0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))
                    {
                        isKilled = true;
                        break;
                    }
                    else if(/*!"XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) &&*/ !"-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) && !"".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())
                            && !"0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) && (!isCheckSim || !"XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())))
                    {
                        log.add("desc " + program, "Thiết bị này có sim");
                        count = count + 1;
                    }

//                    if("XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) || "-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))
//                    {
//                        joReply.putNumber(StringConstUtil.ERROR, 1000);
//                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
//                        log.add("error " + program, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
//                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn " + " " + phoneNumber);
//                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
//                        callback.handle(joReply);
//                        return;
//                    }
                }
                if(isKilled)
                {
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị là máy ảo, không trả thưởng, vui lòng dùng app thật nhé.");
                    log.add("error " + program, "Thiết bị là máy ảo, không trả thưởng, vui lòng dùng app thật nhé.");
                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị là máy ảo, không trả thưởng, vui lòng dùng app thật nhé. " + " " + phoneNumber);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
                    callback.handle(joReply);
                    return;
                }
                else if(count < 2)
                {
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
                    log.add("error " + program, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn " + " " + phoneNumber);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
                    callback.handle(joReply);
                    return;
                }
            }

        }

        String extraKeyFinal = "".equalsIgnoreCase(extraKeyAndroid) ? extraKey : extraKeyAndroid;
        final String[] address_tmp = extraKeyFinal.split(MomoMessage.BELL);
        log.add("size address tmp " + program, address_tmp.length);

        if (!os.equalsIgnoreCase("ios") && address_tmp.length == 0) {
            joReply.putNumber(StringConstUtil.ERROR, 1000);
            joReply.putString(StringConstUtil.DESCRIPTION, "Thieu du lieu extra key");
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, "Thieu du lieu extra key" + " " + phoneNumber);
            callback.handle(joReply);
        } else {
            final AtomicInteger integer = new AtomicInteger(address_tmp.length);
            final AtomicInteger empty = new AtomicInteger(0);
            if (!"".equalsIgnoreCase(os) && !os.equalsIgnoreCase("ios")) {
                log.add("os " + program, "android");
                takeNoteDeviceInfo(program, phoneNumber, log, callback, joReply, address_tmp, integer, empty, extraKeyFinal);
            }
//            else if(data.os.equalsIgnoreCase("ios") && data.appCode >= 1923)
//            {
//                //Thuc hien luu tru moi
//                log.add("os " + StringConstUtil.BinhTanPromotion.PROGRAM, "ios");
//                takeNoteDeviceInfo(phoneNumber, log, callback, joReply, address_tmp, integer, empty);
//            }
            else if (os.equalsIgnoreCase("ios")) {
                DeviceDataUserDb.Obj deviceObj = new DeviceDataUserDb.Obj();
                deviceObj.phoneNumber = phoneNumber;
                deviceObj.id = imei;
                deviceDataUserDb.insert(deviceObj, program, new Handler<Integer>() {
                    @Override
                    public void handle(Integer result) {
                        if (result == 0) {
                            joReply.putNumber(StringConstUtil.ERROR, 0);
                            joReply.putString(StringConstUtil.DESCRIPTION, "GOOD");
                        } else {
                            joReply.putNumber(StringConstUtil.ERROR, 1000);
                            joReply.putString(StringConstUtil.DESCRIPTION, "Du lieu thiet bi da ton tai, khong cho so nay tham gia khuyen mai binh tan nua");
                            log.add("error " + program, "Loi insert ios data user");
                            JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, imei).putString(StringConstUtil.DESCRIPTION, "Du lieu thiet bi da ton tai, khong cho so nay tham gia khuyen mai binh tan nua" + " " + phoneNumber);
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
                        }
                        callback.handle(joReply);
                    }
                });
                return;
            } else {
//                processIronMan(buf, log, msg, sock);
                log.add("desc" + program, "Khong ton tai thiet bi nay");
                joReply.putNumber(StringConstUtil.ERROR, 1000);
                joReply.putString(StringConstUtil.DESCRIPTION, "Khong ton tai thiet bi, khong cho tham gia chuong trinh");
                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, "Khong ton tai thiet bi, khong cho tham gia chuong trinh" + " " + phoneNumber);
                callback.handle(joReply);
            }

        }

    }

    private void takeNoteDeviceInfo(final String program, final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback, final JsonObject joReply, final String[] address_tmp, final AtomicInteger integer, final AtomicInteger empty, final String extraKey) {
        vertx.setPeriodic(200L, new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                int position = integer.decrementAndGet();
                if (position < 0) {
                    log.add("position " + program, position);
                    vertx.cancelTimer(event);
                    if (empty.intValue() > 1) {
                        log.add("position " + program, "empty.intValue() != address_tmp.length");
                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin gmail || imei || mac ghi nhan. + INFO: " + extraKey);
                    } else {
                        log.add("position " + program, "data is enough => GOOD");
                        joReply.putNumber(StringConstUtil.ERROR, 0);
                        joReply.putString(StringConstUtil.DESCRIPTION, "GOOD");
                    }
                    callback.handle(joReply);
                    return;
                }
                else {
                    if (address_tmp[position].equalsIgnoreCase("")) {
                        log.add("item " + program, address_tmp[position]);
                        empty.incrementAndGet();
                    }
                    //cong nguyen 05/08/2016 bo chan sim
//                    else if(address_tmp[position].equalsIgnoreCase("XXX")) {
//                        vertx.cancelTimer(event);
//                        log.add("error " + program, "Loi insert android data user bi xxx");
//                        joReply.putNumber(StringConstUtil.ERROR, 1000);
//                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này không truyền đủ thông tin, sẽ kiểm tra và trả bù nếu hợp lệ. Xin cám ơn " +  phoneNumber);
//                        callback.handle(joReply);
//                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này không truyền đủ thông tin, sẽ kiểm tra và trả bù nếu hợp lệ. Xin cám ơn " +  phoneNumber);
//                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
//                        return;
//                    }
                    else if(address_tmp[position].trim().equalsIgnoreCase("02:00:00:00:00:00")) {
                        log.add("item " + phoneNumber + " " + program, address_tmp[position]);
                        log.add("item " + phoneNumber + " " + program, "ANDROID 6 nen bo qua imei");
                    } else if(address_tmp[position].toString().trim().equalsIgnoreCase("xxx")) {
                        log.add("item " + phoneNumber + " " + program, address_tmp[position]);
                        log.add("item " + phoneNumber + " " + program, " xxx nen khong luu");
                    } else {
                        log.add("item " + program, address_tmp[position]);
                        DeviceDataUserDb.Obj deviceDataUserObj = new DeviceDataUserDb.Obj();
                        deviceDataUserObj.id = address_tmp[position].toString().trim();
                        deviceDataUserObj.phoneNumber = phoneNumber;
                        deviceDataUserDb.insert(deviceDataUserObj, program, new Handler<Integer>() {
                            @Override
                            public void handle(Integer resultInsert) {
                                if (resultInsert != 0) {
                                    vertx.cancelTimer(event);
                                    log.add("error " + program, "Loi insert android data user");
                                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                                    joReply.putString(StringConstUtil.DESCRIPTION, "Du lieu da ton tai, khong cho so nay tham gia chuong trinh");
                                    callback.handle(joReply);
                                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Du lieu da ton tai, khong cho so nay tham gia chuong trinh" + " " + phoneNumber);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
                                    return;
                                }
                            }
                        });
                    }
                }
            }
        });
        return;
    }

    private JsonObject createJsonNotification(String storeNumber, String content, String title, String url)
    {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.RedirectNoti.CAPTION, title);
        jo.putString(StringConstUtil.RedirectNoti.BODY, content);
        jo.putString(StringConstUtil.RedirectNoti.RECEIVER_NUMBER, storeNumber);
        jo.putNumber(StringConstUtil.RedirectNoti.TRAN_ID, System.currentTimeMillis());
        jo.putString(StringConstUtil.RedirectNoti.URL, url);
        logger.info("Noti content: " + jo.toString());
        return jo;
    }

    private void checkBankIsAccepted(String bankId, PromotionDb.Obj prompromotionObj, Handler<Boolean> callback) {
        String strListBank = prompromotionObj.ADJUST_PIN;
        String[] listBank = strListBank.split(";");
        //accept all of bank
        if(listBank.length == 0) {
            callback.handle(true);
            return;
        }

        for (String obj:listBank) {
            if(obj.equalsIgnoreCase(bankId)) {
                callback.handle(true);
                return;
            }
        }
        callback.handle(false);
    }
}