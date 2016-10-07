package com.mservice.momo.vertx;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.lotte.LottePromoDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.CheatInfoDb;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.promotion_server.MainPromotionVerticle;
import com.mservice.momo.vertx.promotion_server.PromotionObj;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by manhly on 01/08/2016.
 */
public class MLotteVerticle extends MainPromotionVerticle {
    Logger logger;
    JsonObject glbCfg;
    ErrorPromotionTrackingDb errorPromotionTrackingDb;
    CheatInfoDb cheatInfoDb;
    private boolean remindNoti;
    private boolean isUAT;
    private TransDb tranDb;
    private LottePromoDb lottePromoDb;
    private PhonesDb phonesDb;

    public void start() {
        super.start();
        logger = container.logger();
        glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        isUAT = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
        remindNoti = glbCfg.getBoolean(StringConstUtil.SEND_REMIND_PROMO, false);
        lottePromoDb = new LottePromoDb(vertx, logger);       //n
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        cheatInfoDb = new CheatInfoDb(vertx, logger);
        if (remindNoti) {
            remind14DaysNotUsedGiftUser();
            remind22DaysNotUsedGiftUser();
        }

        vertx.eventBus().registerHandler(AppConstant.LOTTE_PROMOTION_BUS_ADDRESS, new Handler<Message<JsonObject>>() {
            public void handle(final Message<JsonObject> message) {
                final Common.BuildLog log = new Common.BuildLog(logger);
                JsonObject joReceive = message.body();
                final PromotionObj promotionObj = new PromotionObj(joReceive);
                log.setPhoneNumber(promotionObj.phoneNumber);
//                final PhonesDb.Obj userPhoneObj = new PhonesDb.Obj(promotionObj.joPhone);
                if (promotionObj.tranType != MomoProto.TranHisV1.TranType.PAY_IPOS_BILL_VALUE) {
                    //2000 ~ 2030
                    callBack(2000, "", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                    return;
                }

                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj promoObj = null;
                            long voucherAmount = 100000;
                            String agent = ""; // Tai khoang tra thuong
                            for (Object o : array) {
                                if (((JsonObject) o).getString(colName.PromoCols.NAME, "").equalsIgnoreCase("lotte_promo")) {
                                    promoObj = new PromotionDb.Obj((JsonObject) o);
                                    agent = promoObj.ADJUST_ACCOUNT; //Tai khoang tra thuong
                                    voucherAmount = promoObj.PER_TRAN_VALUE;
                                    break;
                                }
                            }

                            if (promoObj == null) {
                                callBack(5052, "Chua cau hinh chuong trinh tra thuong", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber), "CHUA CAU HINH TRA THUONG!", 5052, "", log, this.getClass().getName());
                                return;
                            }

                            final String fAgent = agent;
                            final long fVoucherAmount = voucherAmount;
                            final PromotionDb.Obj fPromoObj = promoObj;
                            final PhonesDb.Obj phoneObj = new PhonesDb.Obj(promotionObj.joPhone);
                            /*******************************************************/
                            lottePromoDb.findByPhone(promotionObj.phoneNumber, new Handler<LottePromoDb.Obj>() {
                                @Override
                                public void handle(LottePromoDb.Obj obj) {
                                    if (obj == null || obj.bonusTime == 0L) {
                                        long curr = System.currentTimeMillis();
                                        if (fPromoObj.DATE_FROM > curr || fPromoObj.DATE_TO < curr) {
                                            callBack(5052, "Het thoi gian khuyen mai!", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                            saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber), "Het thoi gian khuyen mai!", 5052, "", log, this.getClass().getName());
                                            return;
                                        }
                                        if (phoneObj == null) {
                                            callBack(5052, "So dt chua thay ghi nhan bang phones, kiem tra lai nhe!", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                            saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber), "CHUA THAY GHI NHAN TRONG BANG PHONES => CHUA REG NICK, KIEM TRA DU LIEU CO GI DO SAI ROI!", 5052, "", log, this.getClass().getName());
                                            return;
                                        }

                                        if (phoneObj.createdDate < fPromoObj.DATE_FROM || phoneObj.createdDate > fPromoObj.DATE_TO) {
                                            callBack(5052, "Tham gia ngoai thoi gian khuyen mai!", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                            saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber), "THAM GIA NGOAI THOI GIAN KHUYEN MAI!", 5052, "", log, this.getClass().getName());
                                            return;
                                        }

                                        checkDevice(phoneObj.deviceInfo, phoneObj.phoneOs, phoneObj.appCode, "lotte", DataUtil.strToInt(promotionObj.phoneNumber), phoneObj.lastImei, true, false, this.getClass().getSimpleName(), log, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject joResult) {
                                                int err = joResult.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                                                String desc = joResult.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                                                if (err != 0) {
                                                    addLog("info", desc, this.getClass().getSimpleName(), log);
                                                    callBack(5058, "check divice false", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber), joResult.getString(StringConstUtil.PromotionField.DESCRIPTION, "Loi check device!"), 5052, "", log, this.getClass().getName());
                                                    return;
                                                }

                                                final LottePromoDb.Obj lottePromotionObj = new LottePromoDb.Obj();
                                                lottePromotionObj.phoneNumber = promotionObj.phoneNumber;
                                                lottePromotionObj.registerTime = phoneObj.createdDate;
                                                lottePromotionObj.updateTime = System.currentTimeMillis();
                                                lottePromoDb.insert(lottePromotionObj, new Handler<Integer>() {
                                                    @Override
                                                    public void handle(Integer err) {
                                                        if (err == 0) {
                                                            addLog("info", "insert success " + promotionObj.phoneNumber, this.getClass().getSimpleName(), log);
                                                            //tra thuong
                                                            //todo check cheating server.
                                                            final double maxScoreServer = DataUtil.stringToUNumber(fPromoObj.ADJUST_PIN, 1);
                                                            log.add("maxScoreServer", maxScoreServer);
                                                            if (maxScoreServer > 0.0) {
                                                                log.add("GREAT!!! desc", "maxScoreServer is greater than 0");
                                                                JsonObject joCheckCheat = new JsonObject()
                                                                        .putString(StringConstUtil.MOMO_TOOL.PHONE_NUMBER, lottePromotionObj.phoneNumber)
                                                                        .putString(StringConstUtil.PATH, "/checkScore");
                                                                vertx.eventBus().send(AppConstant.CHECK_CHEATING_PROMOTION_BUS_ADDRESS, joCheckCheat, new Handler<Message<JsonObject>>() {
                                                                    @Override
                                                                    public void handle(Message<JsonObject> msgResponse) {
                                                                        JsonObject joResponse = msgResponse.body();
                                                                        JsonObject joData = joResponse.getObject(StringConstUtil.PromotionField.RESULT, new JsonObject());
                                                                        Number maxScore = joData.getNumber("max_score");
                                                                        final double checkedMaxScore = getMaxScore(joData, promotionObj.phoneNumber, maxScoreServer);
                                                                        final JsonObject joObjDup = getMaxScoreJsonObject(joData, promotionObj.phoneNumber, maxScoreServer);
                                                                        if (maxScore != null) {
                                                                            log.add("desc", "maxScore is not null");
                                                                            if (checkedMaxScore > maxScoreServer) {
                                                                                log.add("WARNING desc", "maxscore greater than score server");
                                                                                log.writeLog();
                                                                                callBack(5058, "Vuong rule chan device", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                                                saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber)
                                                                                        , "thiet bi cua sdt " + promotionObj.phoneNumber + " da duoc tra thuong cho so dien thoai khac " + joObjDup.getString("number", "")
                                                                                        , 5058, "", log, this.getClass().getName());
                                                                            } else {
                                                                                log.add("desc", "less than maxScoreServer");
                                                                                executeGiveVoucher(message, fVoucherAmount, fAgent, promotionObj, log);
                                                                            }
                                                                        } else {
                                                                            executeGiveVoucher(message, fVoucherAmount, fAgent, promotionObj, log);
                                                                        }
                                                                        CheatInfoDb.Obj cheatInfoObj = new CheatInfoDb.Obj();
                                                                        cheatInfoObj.phoneNumber = lottePromotionObj.phoneNumber;
                                                                        cheatInfoObj.time = System.currentTimeMillis();
                                                                        cheatInfoObj.info = joResponse.toString();

                                                                        cheatInfoDb.insert(cheatInfoObj, new Handler<Integer>() {
                                                                            @Override
                                                                            public void handle(Integer event) {

                                                                            }
                                                                        });
                                                                    }
                                                                });
                                                            } else {
                                                                log.add("WARNING desc", "MAX SCORE CONFIG IS NOT GREATER THAN 0");
                                                                executeGiveVoucher(message, fVoucherAmount, fAgent, promotionObj, log);
                                                            }
                                                            return;
                                                        } else {
                                                            addLog("info", "insert fail " + promotionObj.phoneNumber, this.getClass().getSimpleName(), log);
                                                            callBack(err, "", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                            saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber), "INSERT VAO DB MONGO FAIL!", err, "", log, this.getClass().getName());
                                                        }

                                                        return;
                                                    }
                                                });
                                            }
                                        });
                                    } else {
                                        //User da duoc nhan thuong.
                                        callBack(0, "OK", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                        JsonObject joUpdate = new JsonObject();
                                        joUpdate.putBoolean(colName.LottePromoCol.USED_GIFT, true);
                                        joUpdate.putNumber(colName.LottePromoCol.UPDATE_TIME, System.currentTimeMillis());
                                        lottePromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {

                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            callBack(5052, "Chua cau hinh chuong trinh tra thuong", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                            saveErrorPromotionDescription("lotte_promo", DataUtil.strToInt(promotionObj.phoneNumber), "CHUA CAU HINH CHUONG TRINH!", 5052, "", log, this.getClass().getName());
                        }

                    }
                });
            }
        });
    }

    private double getMaxScore(JsonObject joData, String number, double defaultValue) {
        double maxScore = 0.0;

        JsonArray jarrData = joData.getArray("data", new JsonArray());
        if (jarrData.size() > 0) {
            for (int i = 0; i < jarrData.size(); i++) {
                JsonObject joReceiveData = new JsonObject(jarrData.get(i).toString());
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

    private void executeGiveVoucher(final Message message, final long fVoucherAmount, final String fAgent, final PromotionObj promotionObj, final Common.BuildLog log) {
        List<String> listVoucher = new ArrayList<String>() {{
            add("lotte_gift");
        }};
        giveVoucher(fVoucherAmount, 30, fAgent, promotionObj.phoneNumber, "lotte_promo", listVoucher, log, "MLotteVerticle", true, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject joResponse) {
                int error = joResponse.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                String desc = joResponse.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                final long giftTranId = joResponse.getLong(StringConstUtil.PromotionField.GIFT_TID, System.currentTimeMillis());
                //todo if success
                JsonObject joUpdate = new JsonObject();
                joUpdate.putNumber(colName.LottePromoCol.TRAN_ID, promotionObj.tranId);
                joUpdate.putNumber(colName.LottePromoCol.BONUS_AMOUNT, fVoucherAmount);
                joUpdate.putNumber(colName.LottePromoCol.BONUS_TIME, System.currentTimeMillis());
                joUpdate.putNumber(colName.LottePromoCol.TRAN_AMOUNT, promotionObj.amount);
                joUpdate.putNumber(colName.LottePromoCol.UPDATE_TIME, System.currentTimeMillis());
                joUpdate.putNumber(colName.LottePromoCol.ERROR, error);
                lottePromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
                        Gift gift = new Gift(joResponse.getObject(StringConstUtil.PromotionField.GIFT, new JsonObject()));
                        Long temp = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30);
                        String expireVoucherDate = Misc.dateVNFormat(temp);
                        String notibody = StringConstUtil.lotteNoti.BODY.replace("%s", expireVoucherDate);
                        Notification notification = buildPopupGiftNotification(StringConstUtil.lotteNoti.CAPTIONNOTI, notibody, 3, 16, giftTranId, "lotte", gift, 100000, promotionObj.phoneNumber, StringConstUtil.lotteNoti.CAPTION);
                        //{"screenId":16,"serviceId":"","type":3,"button_title_1":"xem chi tiet","button_title_2":"","button_title_x":true}
                        //notification.type = 28;
                        //notification.tranId = Long.valueOf(0);
                        //notification.extra = "{\"screenId\":16,\"serviceId\":\"\",\"type\":3,\"button_title_1\":\"xem chi tiet\",\"button_title_2\":\"\",\"button_title_x\":true}";
                        saveSuccessPromotionTransaction(notibody, giftTranId, 100000, promotionObj.phoneNumber, "Thẻ quà tặng LotteMart", "", "momo", notibody, "", new JsonObject());
                        callBack(0, "success", notification, log, message, this.getClass().getSimpleName().toString(), new JsonObject());

                    }
                });
            }
        });
    }

    private void remind14DaysNotUsedGiftUser() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime);
        //long waitTime = calendarTime - currentTime > 0 ? calendarTime - currentTime :1;
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait14daysTimerSet) {
                vertx.cancelTimer(wait14daysTimerSet);
                vertx.setPeriodic(1000L * 60 * 60 * 24, new Handler<Long>() {
                    //vertx.setPeriodic(1000L * 60 * 2, new Handler<Long>() {
                    @Override
                    public void handle(Long wait14daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();

                        JsonArray jarrAnd = new JsonArray();

                        JsonObject joGreaterTime = new JsonObject().putObject(colName.LottePromoCol.BONUS_TIME, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 15)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.LottePromoCol.BONUS_TIME, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 14)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);

                        JsonObject joUsedGift = new JsonObject().putBoolean(colName.LottePromoCol.USED_GIFT, false);
                        jarrAnd.add(joUsedGift);

                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        lottePromoDb.searchWithFilter(joSearch, new Handler<ArrayList<LottePromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<LottePromoDb.Obj> listLottePromo1) {
                                final AtomicInteger count = new AtomicInteger(listLottePromo1.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        int position = count.decrementAndGet();
//                                        listLottePromo1.get(position).phoneNumber
                                        //build noti and send
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }
                                        //Build noti and send
                                        JsonObject joNoti = new JsonObject();
                                        Long temp = listLottePromo1.get(position).bonusTime + (1000L * 60 * 60 * 24 * 30);
                                        String expireVoucherDate = Misc.dateVNFormat(temp);
                                        String notibody = StringConstUtil.lotteNoti.BODY14.replace("%s", expireVoucherDate);
                                        String bodyNoti = notibody;
                                        String titleNoti = StringConstUtil.lotteNoti.CAPTION14;
                                        sendNotiViaCloud(titleNoti, bodyNoti, listLottePromo1.get(position).phoneNumber);

                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void remind22DaysNotUsedGiftUser() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime);
        //long waitTime = calendarTime - currentTime > 0 ? calendarTime - currentTime : 1;
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait22daysTimerSet) {
                vertx.cancelTimer(wait22daysTimerSet);
                vertx.setPeriodic(1000L * 60 * 60 * 24, new Handler<Long>() {
                    @Override
                    public void handle(Long wait22daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();
                        JsonArray jarrAnd = new JsonArray();
                        JsonObject joGreaterTime = new JsonObject().putObject(colName.LottePromoCol.BONUS_TIME, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 23)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.LottePromoCol.BONUS_TIME, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 22)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);

                        JsonObject joUsedGift = new JsonObject().putBoolean(colName.LottePromoCol.USED_GIFT, false);
                        jarrAnd.add(joUsedGift);

                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        lottePromoDb.searchWithFilter(joSearch, new Handler<ArrayList<LottePromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<LottePromoDb.Obj> listLottePromo1) {
                                final AtomicInteger count = new AtomicInteger(listLottePromo1.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        int position = count.decrementAndGet();
//                                        listLottePromo1.get(position).phoneNumber
                                        //build noti and send
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }
                                        //Build noti and send
                                        JsonObject joNoti = new JsonObject();
                                        Long temp = listLottePromo1.get(position).bonusTime + (1000L * 60 * 60 * 24 * 30);
                                        String expireVoucherDate = Misc.dateVNFormat(temp);
                                        String notibody = StringConstUtil.lotteNoti.BODY22.replace("%s", expireVoucherDate);
                                        String bodyNoti = notibody;
                                        String titleNoti = StringConstUtil.lotteNoti.CAPTION22;
                                        sendNotiViaCloud(titleNoti, bodyNoti, listLottePromo1.get(position).phoneNumber);

                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }
}
