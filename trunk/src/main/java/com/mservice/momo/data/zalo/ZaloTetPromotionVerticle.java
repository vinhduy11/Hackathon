package com.mservice.momo.data.zalo;

import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.NotificationUtils;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 1/22/16.
 */
public class ZaloTetPromotionVerticle extends Verticle {


    long time20days = 1000L * 60 * 60 * 24 * 20;
    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private TransDb tranDb;
    private AgentsDb agentsDb;
    private DBProcess dbProcess;
    private GiftDb giftDb;
    private PhonesDb phonesDb;
    private Card card;
    private boolean isStoreApp;
    private JsonObject jsonZaloPromo;

    private Common common;

    boolean isZaloPromoActive = false;

    ZaloTetPromotionDb zaloTetPromotionDb;
    ZaloNotiGroupDb zaloNotiGroupDb;
    private ControlOnClickActivityDb controlOnClickActivityDb;
    //Config for zalo promotion
    private String agent = "";
    private int time_for_money = 7;
    private int time_for_gift = 45;

    private long value_of_money = 10000;
    private long value_of_gift =  150000;
    private long amount_bonus_maximum = 70000;
    private ZaloTetCashBackPromotionDb zaloTetCashBackPromotionDb;
    private String url = "";
    private String agentCashback = "";
    ArrayList<ZaloNotiObj> arrNotiList = null;
    ArrayList<Integer> time = new ArrayList<>();
    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        this.agentsDb = new AgentsDb(vertx.eventBus(), logger);
        this.giftManager = new GiftManager(vertx, logger, glbCfg);
        this.giftDb = new GiftDb(vertx, logger);
        this.card = new Card(vertx.eventBus(), logger);
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);

        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());
        zaloNotiGroupDb = new ZaloNotiGroupDb(vertx, logger);
        zaloTetPromotionDb = new ZaloTetPromotionDb(vertx, logger);
        zaloTetCashBackPromotionDb = new ZaloTetCashBackPromotionDb(vertx, logger);

        common = new Common(vertx, logger, container.config());

        time.add(1);
        time.add(2);
        time.add(3);
        time.add(4);
        time.add(5);
        time.add(6);
        time.add(14);
        time.add(21);
        time.add(28);
        time.add(35);
        time.add(42);
        time.add(45);
        time.add(50);
        time.add(70);
        time.add(80);
        time.add(90);
        controlOnClickActivityDb = new ControlOnClickActivityDb(vertx);
        url = jsonZaloPromo.getString(StringConstUtil.ZaloPromo.URL, "");
        agent = jsonZaloPromo.getString(StringConstUtil.ZaloPromo.AGENT, "");
        agentCashback = jsonZaloPromo.getString(StringConstUtil.ZaloPromo.AGENT_CASHBACK, "");
        time_for_money = jsonZaloPromo.getInteger(StringConstUtil.ZaloPromo.TIME_FOR_MONEY, 0);
        time_for_gift = jsonZaloPromo.getInteger(StringConstUtil.ZaloPromo.TIME_FOR_GIFT, 0);
        value_of_money = jsonZaloPromo.getLong(StringConstUtil.ZaloPromo.VALUE_OF_MONEY, 0);
        value_of_gift = jsonZaloPromo.getLong(StringConstUtil.ZaloPromo.VALUE_OF_GIFT, 0);
        amount_bonus_maximum = jsonZaloPromo.getLong(StringConstUtil.ZaloPromo.AMOUNT_BONUS_MAXIMUM, 70000);
        arrNotiList = new ArrayList<>();
        boolean scanExpiredMoney = jsonZaloPromo.getBoolean(StringConstUtil.ZaloPromo.SCAN_EXPIRED_GIFT, false);
        if(scanExpiredMoney)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 56);
            calendar.set(Calendar.SECOND, 50);
//            calendar.set(Calendar.HOUR_OF_DAY, 18);
//            calendar.set(Calendar.MINUTE, 52);
//            calendar.set(Calendar.SECOND, 00);
            long finalTime = calendar.getTimeInMillis();
            long timeDis = finalTime - System.currentTimeMillis();
            logger.info("FINAL TIME " + finalTime);
            logger.info("TIME DIS " + timeDis);
            vertx.setTimer(timeDis, new Handler<Long>() {
                @Override
                public void handle(Long timerSet) {

             logger.info("ZALO setTimer");
                    vertx.setPeriodic(24 * 60 * 60 * 1000L, new Handler<Long>() { // 1 ngay quet lai 1 lan
//                vertx.setPeriodic(2 * 60 * 1000L, new Handler<Long>() {
                        @Override
                        public void handle(Long timerPer) {
                            // Quet voucher 1
                            logger.info("BEGIN TO SCAN ZALO PROMOTION TIME EXPIRED");
                            Common.BuildLog log = new Common.BuildLog(logger);
                            log.setPhoneNumber("scanExpiredMoney");
                            scanExpiredLuckyMoney(log);
                            log.writeLog();
                        }
                    });
                }
            });
        }

        if(scanExpiredMoney)
        {
            Calendar calendar = Calendar.getInstance();
            int date = calendar.get(Calendar.DATE);
            calendar.set(Calendar.HOUR_OF_DAY, 4);
            calendar.set(Calendar.MINUTE, 00);
            calendar.set(Calendar.SECOND, 00);
            calendar.set(Calendar.DATE, date + 1);
//            calendar.set(Calendar.HOUR_OF_DAY, 9);
//            calendar.set(Calendar.MINUTE, 30);
//            calendar.set(Calendar.SECOND, 00);
            final long finalTime = calendar.getTimeInMillis();
            long timeDis = finalTime - System.currentTimeMillis();
            logger.info("FINAL TIME " + finalTime);
            logger.info("TIME DIS " + timeDis);
            vertx.setTimer(timeDis, new Handler<Long>() {
                @Override
                public void handle(Long timerSet) {

                    logger.info("ZALO setTimer scan Noti");
                    vertx.setPeriodic(24 * 60 * 60 * 1000L, new Handler<Long>() { // 1 ngay quet lai 1 lan
//                    vertx.setPeriodic(2 * 60 * 1000L, new Handler<Long>() {
                        @Override
                        public void handle(Long timerPer) {
                            // Quet voucher 1
                            arrNotiList.clear();
                            logger.info("scan group T1");

                            Queue<Integer> integerQueue = new ArrayDeque<Integer>();
                            for(int i = 0; i < time.size(); i++)
                            {
                                integerQueue.add(time.get(i));
                            }
                            scanNotiGroup(integerQueue, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean response) {
                                    if(response)
                                    {
                                        final AtomicInteger atomicNoti = new AtomicInteger(arrNotiList.size());
                                        vertx.setTimer(finalTime + 1000L*60*60*5, new Handler<Long>() {
//                                            vertx.setTimer(finalTime + 1000L * 60 * 5, new Handler<Long>() {
                                            @Override
                                            public void handle(final Long notiTime) {

                                                vertx.setPeriodic(1000L, new Handler<Long>() {
                                                    @Override
                                                    public void handle(Long notiTimePeri) {
                                                        if(atomicNoti.decrementAndGet() < 0)
                                                        {
                                                            vertx.cancelTimer(notiTime);
                                                            vertx.cancelTimer(notiTimePeri);
                                                            return;
                                                        }
                                                        int itemPosition = atomicNoti.intValue();
                                                        logger.info("item position " + itemPosition);
                                                        sendNotiViaGroup(arrNotiList.get(itemPosition).getPhoneNumber(), arrNotiList.get(itemPosition).getGroup(), arrNotiList.get(itemPosition).getJsonData());
                                                    }
                                                });
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
        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final ZaloTetPromotionObj zaloPromoObj = new ZaloTetPromotionObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);;
                log.setPhoneNumber(zaloPromoObj.phoneNumber);
                log.add("phoneNumber", zaloPromoObj.phoneNumber);
                log.add("zalo code", zaloPromoObj.zalo_code);
                log.add("imei", zaloPromoObj.last_imei);
                log.add("source", zaloPromoObj.source);
                log.add("amount", zaloPromoObj.amount);
                log.add("serviceId", zaloPromoObj.serviceId);
                log.add("TID", zaloPromoObj.tid);
                log.add("promotion time", zaloPromoObj.promotionTime);
                log.add("cash back time", zaloPromoObj.cashbackTime);

                if(StringConstUtil.ZaloPromo.ZALO_CASHBACK_PROGRAM.equalsIgnoreCase(zaloPromoObj.source))
                {
                    //Lay thoi gian hien tai
                    long startMonth = System.currentTimeMillis();
                    long endMonth = System.currentTimeMillis();
                    long cashBackTime = zaloPromoObj.cashbackTime == 0 ? System.currentTimeMillis() : zaloPromoObj.cashbackTime;
                    if(cashBackTime >= zaloPromoObj.promotionTime && cashBackTime <= zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 30)
                    {
                        startMonth = zaloPromoObj.promotionTime;
                        endMonth = zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 30;
                    }
                    else if(cashBackTime >= zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 30 && cashBackTime <= zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 60)
                    {
                        startMonth = zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 30;
                        endMonth = zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 60;
                    }
                    else if(cashBackTime >= zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 60 && cashBackTime <= zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 90)
                    {
                        startMonth = zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 60;
                        endMonth = zaloPromoObj.promotionTime + 1000L * 60 * 60 * 24 * 90;
                    }

                    JsonObject joFilter = new JsonObject();
                    joFilter.putString(colName.ZaloTetCashBackPromotionCol.PHONE_NUMBER, zaloPromoObj.phoneNumber);

                    JsonObject joGreater = new JsonObject();
                    joGreater.putNumber(MongoKeyWords.GREATER, startMonth);

                    JsonObject joTimeGreater = new JsonObject();
                    joTimeGreater.putObject(colName.ZaloTetCashBackPromotionCol.TIME, joGreater);


                    JsonObject joLess = new JsonObject();
                    joLess.putNumber(MongoKeyWords.LESS_THAN, endMonth);

                    JsonObject joTimeLess = new JsonObject();
                    joTimeLess.putObject(colName.ZaloTetCashBackPromotionCol.TIME, joLess);

                    JsonArray jsonAnd = new JsonArray();
                    jsonAnd.add(joTimeGreater);
                    jsonAnd.add(joTimeLess);
                    joFilter.putArray(MongoKeyWords.AND_$, jsonAnd);

                    zaloTetCashBackPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<ZaloTetCashBackPromotionDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<ZaloTetCashBackPromotionDb.Obj> listItem) {
                            log.add("so luong da duoc cash back trong thang", listItem.size());
                            long amount_bonus = 0;
                            for(int i = 0; i < listItem.size(); i++)
                            {
                                amount_bonus = amount_bonus + listItem.get(i).amount_bonus;
                            }
                            log.add("amount_bonus", amount_bonus);
                            if(amount_bonus >= amount_bonus_maximum)
                            {
                                log.add("desc", "Da nhan tien cashback nhieu hon cho phep roi nhe");
                                message.reply(new JsonObject().putString(StringConstUtil.DESCRIPTION,"Da nhan tien cashback nhieu hon cho phep roi nhe")
                                                    .putNumber(StringConstUtil.ERROR, 1000));
                                log.writeLog();
                                return;
                            }
                            final long cashbackMoney = 7 * zaloPromoObj.amount / 100;
                            log.add("cashbackMoney", cashbackMoney);
                            long final_cashBackMoney = amount_bonus_maximum - amount_bonus > cashbackMoney ? cashbackMoney : amount_bonus_maximum - amount_bonus;
                            //Neu la chuong trinh cashback thi cashback
                            log.add("final_cashBackMoney", final_cashBackMoney);
                            cashBackMoneyToUser(final_cashBackMoney, zaloPromoObj, log, message, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jsonResponseCashBack) {
                                    int err = jsonResponseCashBack.getInteger(StringConstUtil.ERROR, -1000);
                                    final long amountCashBack = jsonResponseCashBack.getLong(StringConstUtil.ZaloPromo.AMOUNT, 0);
                                    final long tidCashBack = jsonResponseCashBack.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                                    log.add("err", err);
                                    message.reply(jsonResponseCashBack);
                                    if(err == 0)
                                    {
                                        ZaloTetCashBackPromotionDb.Obj zaloCashBackObj = new ZaloTetCashBackPromotionDb.Obj();
                                        zaloCashBackObj.service_id = zaloPromoObj.serviceId;
                                        zaloCashBackObj.tran_id = zaloPromoObj.tid;
                                        zaloCashBackObj.amount = zaloPromoObj.amount;
                                        zaloCashBackObj.phone_number = zaloPromoObj.phoneNumber;
                                        zaloCashBackObj.tranIdBonus = tidCashBack;
                                        zaloCashBackObj.amount_bonus = amountCashBack;
                                        zaloCashBackObj.time = System.currentTimeMillis();
                                        zaloTetCashBackPromotionDb.insert(zaloCashBackObj, new Handler<Integer>() {
                                            @Override
                                            public void handle(Integer event) {
                                                sendNotiAndTranHisCashBack7Percent(log, tidCashBack, zaloPromoObj.phoneNumber, amountCashBack);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    return;
                }

                final JsonObject jsonReply = new JsonObject();
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        boolean enableGiftGiving = false;
                        long zalo_promo_start_date = 0;
                        long zalo_promo_end_date = 0;
                        long currentTime = System.currentTimeMillis();
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj zalo_promo = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
                                zalo_promo = new PromotionDb.Obj((JsonObject) o);
                                if (zalo_promo.NAME.equalsIgnoreCase(StringConstUtil.ZaloPromo.ZALO_PROGRAM)) {
                                    zalo_promo_start_date = zalo_promo.DATE_FROM;
                                    zalo_promo_end_date = zalo_promo.DATE_TO;
                                    enableGiftGiving = zalo_promo.ENABLE_PHASE2;
                                    agent = "".equalsIgnoreCase(zalo_promo.ADJUST_ACCOUNT) ? agent : zalo_promo.ADJUST_ACCOUNT;
                                }
                            }
                            //Check lan nua do dai chuoi ki tu
                            final boolean enableGift = enableGiftGiving;
                            if (zaloPromoObj.zalo_code.length() < 9) {
                                log.add("desc", "sai code");
                                jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã lì xì này không tồn tại trong hệ thống, vui lòng nhập mã khác");
                                log.writeLog();
                                message.reply(jsonReply);
                                return;
                            }
                            else if(currentTime < zalo_promo_start_date || currentTime > zalo_promo_end_date)
                            {
                                log.add("desc", "Chua bat dau chay chuong trinh zalo");
                                log.writeLog();
                                jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã lì xì đã hết hạn sử dụng. Mã lì xì chỉ có thời hạn trong 7 ngày");
                                message.reply(jsonReply);
                                return;
                            }
                            boolean isZaloPromoActive = jsonZaloPromo.getBoolean(StringConstUtil.ZaloPromo.IS_ACTIVE, false);
                            if (!isZaloPromoActive) {
                                log.add("desc", "Chua active chuong trinh zalo");
                                log.writeLog();
                                jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã lì xì đã hết hạn sử dụng. Mã lì xì chỉ có thời hạn trong 7 ngày");
                                message.reply(jsonReply);
                                return;
                            } else if (zaloPromoObj.phoneNumber.equalsIgnoreCase("") || DataUtil.strToLong(zaloPromoObj.phoneNumber) <= 0) {
                                log.add("desc", "So dien thoai la so dien thoai khong co that.");
                                jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã lì xì này không tồn tại trong hệ thống, vui lòng nhập mã khác");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else if (!StringConstUtil.ZaloPromo.ZALO_PROGRAM.equalsIgnoreCase(zaloPromoObj.source)) {
                                log.add("desc", "Sai chuong trinh.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã lì xì này không tồn tại trong hệ thống, vui lòng nhập mã khác");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else {

                                String group_number = zaloPromoObj.zalo_code.charAt(0) + "";
                                final String collection = getTableFromCode(group_number);

                                if ("".equalsIgnoreCase(collection)) {
                                    log.add("desc", "Sai ma code.");
                                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã lì xì này không tồn tại trong hệ thống, vui lòng nhập mã khác");
                                    message.reply(jsonReply);
                                    log.writeLog();
                                    return;
                                }
                                zaloTetPromotionDb.findZaloUserInfo(colName.ZaloTetPromotionCol.TABLE_ALL, zaloPromoObj.phoneNumber, zaloPromoObj.last_imei, zaloPromoObj.zalo_code, new Handler<ArrayList<ZaloTetPromotionDb.Obj>>() {
                                    @Override
                                    public void handle(ArrayList<ZaloTetPromotionDb.Obj> listZaloObj) {
                                        if (listZaloObj.size() > 0) {
                                            log.add("desc", "So dien thoai hoac imei da nhan thuong.");
                                            jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                            jsonReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này hoặc Mã lì xì này đã từng được sử dụng trước đó. Chương trình chỉ áp dụng 1 mã lì xì cho 1 thiết bị");
                                            message.reply(jsonReply);
                                            log.writeLog();
                                            return;
                                        }
                                        zaloTetPromotionDb.findOneZaloCode(collection, zaloPromoObj.zalo_code, new Handler<ZaloTetPromotionDb.Obj>() {
                                            @Override
                                            public void handle(ZaloTetPromotionDb.Obj zaloTetObj) {
                                                if (zaloTetObj == null) {
                                                    log.add("desc", "Sai ma code, ma code khong ton tai trong db. " + collection);
                                                    jsonReply.putNumber(StringConstUtil.ERROR, 1000);
                                                    jsonReply.putString("desc", "Mã lì xì này không tồn tại trong hệ thống, vui lòng nhập mã khác");
                                                    message.reply(jsonReply);
                                                    log.writeLog();
                                                    return;
                                                }
                                                log.add("desc", "tra khuyen mai zalo");
                                                //Tra thuong tien + qua cho em no.

                                                ControlOnClickActivityDb.Obj controlObj = new ControlOnClickActivityDb.Obj();
                                                controlObj.key = zaloPromoObj.zalo_code;
                                                controlObj.number = zaloPromoObj.phoneNumber;
                                                controlObj.program = zaloPromoObj.source;
                                                controlOnClickActivityDb.insert(controlObj, new Handler<Integer>() {
                                                    @Override
                                                    public void handle(Integer result) {
                                                        if(result == 0)
                                                        {
                                                            processGiveMoneyAndGiftForZaloUser(message, zaloPromoObj, log, enableGift);
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                                //Thoat het cac lop, bat dau tra khuyen mai
                            }
                        }
                    }
                });
        }
    };
        vertx.eventBus().registerLocalHandler(AppConstant.ZALO_PROMOTION_BUSS_ADDRESS, myHandler);
    }



    private void processGiveMoneyAndGiftForZaloUser(final Message message,final ZaloTetPromotionObj zaloTetPromotionObj,final Common.BuildLog log, final boolean enableGift)
    {
        giveMoneyToUser(zaloTetPromotionObj, log, message, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joResponse) {
                int err = joResponse.getInteger(StringConstUtil.ERROR, 1000);
                final long soapTid = joResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                if(err != 0)
                {
                    //Tra tien khong thanh cong, khong tra qua luon
                    log.add("desc", "tra tien khong thanh cong cho tid " + soapTid);
                    message.reply(joResponse);
                    return;
                }
                else if(!enableGift)
                {
                    log.add("desc", "Tra tien thanh cong, nhung khong tra qua");
                    log.add("desc", "Luu info, ban noti vs popup");
                    saveInfo(message, zaloTetPromotionObj, log, soapTid, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {
                            sendNotiAndTranHis(zaloTetPromotionObj, false, soapTid, null);
                            message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                        }
                    });
                    return;
                }
                else {
                    final List<String> listVoucher = new ArrayList<String>();
                    listVoucher.add("taoquan2016_dienhcm");
                    listVoucher.add("taoquan2016_123phim");
                    saveInfo(message, zaloTetPromotionObj, log, soapTid, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {
                            giveVoucherForUser(message, new JsonObject(), zaloTetPromotionObj, listVoucher, log);
                            log.add("desc", "Tra tien thanh cong, Tra qua");
                            //message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                        }
                    });
                    return;
                }
            }
        });
    }

    private void giveMoneyToUser(final ZaloTetPromotionObj zaloTetPromotionObj, final Common.BuildLog log, final Message message, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.add("desc", "giveMoneyToUser");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = zaloTetPromotionObj.source;
        Misc.adjustment(vertx, agent, zaloTetPromotionObj.phoneNumber, value_of_money,
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
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "Hệ thống tạm thời gián đoạn. Vui lòng thực hiện lại sau 30 phút");
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

    private void cashBackMoneyToUser(final long cashbackMoney, final ZaloTetPromotionObj zaloTetPromotionObj, final Common.BuildLog log, final Message message, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.add("desc", "cashBackMoneyToUser");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = zaloTetPromotionObj.source;
        Misc.adjustment(vertx, agentCashback, zaloTetPromotionObj.phoneNumber, cashbackMoney,
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
                            jsonReply.putString(StringConstUtil.DESCRIPTION, "core tra loi");
                            jsonReply.putNumber(StringConstUtil.ZaloPromo.AMOUNT, cashbackMoney);
                            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
//                            message.reply(jsonReply);
                            callback.handle(jsonReply);
                            return;
                        }
                        //Yeu cau xong tien mat
                        //Luu qua trong Db va ban noti thong bao
//                        giveMoney(message, zaloTetPromotionObj, log, soapObjReply, jsonReply);
                        jsonReply.putNumber(StringConstUtil.ERROR, 0);
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Thanh cong");
                        jsonReply.putNumber(StringConstUtil.ZaloPromo.AMOUNT, cashbackMoney);
                        callback.handle(jsonReply);
                    }
                });
    }


    private void saveInfo(final Message message, final ZaloTetPromotionObj zaloTetPromotionObj, final Common.BuildLog log, final long tranId, final Handler<JsonObject> callback)
    {
        log.add("desc", "giveMoney");
        ZaloTetPromotionDb.Obj zaloDbObj = new ZaloTetPromotionDb.Obj();
        zaloDbObj.device_imei = zaloTetPromotionObj.last_imei;
        zaloDbObj.end_time_gift = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * time_for_gift;
        zaloDbObj.end_time_money = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * time_for_money;
        zaloDbObj.hadMoney = true;
        zaloDbObj.phone_number = zaloTetPromotionObj.phoneNumber;
        zaloDbObj.time = System.currentTimeMillis();
        zaloDbObj.zalo_code = zaloTetPromotionObj.zalo_code;

        zaloTetPromotionDb.insert(colName.ZaloTetPromotionCol.TABLE_ALL, zaloDbObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {
                callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
            }
        });

    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void giveVoucherForUser(final Message<JsonObject> message
            , final JsonObject joReply
            , final ZaloTetPromotionObj zaloTetPromotionObj
            , final List<String> listVoucher
            , final Common.BuildLog log) {
        // Trả khuyến mãi
        // Them thong tin service id va so voucher vao core
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", "zalo_promo"));

        keyValues.add(new Misc.KeyValue("group", zaloTetPromotionObj.source));

        //final String giftTypeId = reqObj.serviceId;
        final long giftValue = value_of_gift;

        final long totalGiftValue = giftValue;

        log.add("TOTAL GIFT", listVoucher.size());
        log.add("TOTAL VALUE", totalGiftValue);

        int timeForGift = time_for_gift;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , zaloTetPromotionObj.phoneNumber
                , totalGiftValue
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error", error);
                log.add("desc", SoapError.getDesc(error));

                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    //tao gift tren backend
                    final GiftType giftType = new GiftType();
                    final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    final Misc.KeyValue kv = new Misc.KeyValue();

                    final long modifyDate = System.currentTimeMillis();
                    final String note = zaloTetPromotionObj.source;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = zaloTetPromotionObj.source;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                    log.add("so luong voucher", atomicInteger);

                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {
                            if (atomicInteger.decrementAndGet() < 0) {
                                log.add("func", "out of range for number " + zaloTetPromotionObj.phoneNumber);
                                log.writeLog();
                                vertx.cancelTimer(aPeriodicLong);
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
//                                return;
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                log.add("itemPosition", itemPosition);
                                final boolean fireNoti = itemPosition == 0;
                                giftType.setModelId(listVoucher.get(itemPosition).trim());
//                                final String serviceId = listVoucher.get(itemPosition).trim();
                                final long giftValueFinal = "taoquan2016_dienhcm".equalsIgnoreCase(listVoucher.get(itemPosition).trim()) ? 100000 : 50000;
                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(zaloTetPromotionObj.phoneNumber
                                        , giftValueFinal
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
                                        log.add("desc", "tra thuong chuong trinh zalo bang gift");
                                        log.add("err", err);

                                        //------------tat ca thanh cong roi
                                        if (err == 0) {
                                            giftManager.useGift(zaloTetPromotionObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {
                                                    gift.status = 3;
                                                    updateVoucherInfoForUser(message, giftId, zaloTetPromotionObj, log, gift.typeId, tranId, itemPosition, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean event) {
                                                            if(itemPosition == 0)
                                                            {
                                                                sendNotiAndTranHis(zaloTetPromotionObj, true, tranId, gift);
                                                                vertx.setTimer(1000L * 60 * 5, new Handler<Long>() {
                                                                    @Override
                                                                    public void handle(Long timer) {
                                                                        sendNotiViaGroup(zaloTetPromotionObj.phoneNumber, StringConstUtil.ZaloPromo.NOTI_GROUP_T, null);
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                                }
                                            });
//                                            return;
                                        } else {
                                            joReply.putNumber("error", 1000);
                                            joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                            message.reply(joReply);
                                            log.writeLog();
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
                    log.add("desc", "Lỗi " + SoapError.getDesc(error));
                    log.add("Exception", "Exception " + SoapError.getDesc(error));
                    message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, error).putString(StringConstUtil.DESCRIPTION, "Hệ thống tạm thời gián đoạn. Vui lòng thực hiện lại sau 30 phút"));
                    log.writeLog();
                    return;
                }
            }
        });
    }

    private void updateVoucherInfoForUser(final Message message, final String giftId, final ZaloTetPromotionObj zaloTetPromotionObj, final Common.BuildLog log, final String serviceId, final long tranId, final int count, final Handler<Boolean> callback)
    {
        JsonObject joUpdate = new JsonObject();
        if(count == 0)
        {
            joUpdate.putString(colName.ZaloTetPromotionCol.GIFT_ID_1, giftId);
            joUpdate.putString(colName.ZaloTetPromotionCol.GIFT_TYPE_1, serviceId);
            joUpdate.putBoolean(colName.ZaloTetPromotionCol.HAD_BONUS_7PERCENT, true);
            joUpdate.putNumber(colName.ZaloTetPromotionCol.END_BONUS_7PERCENT_TIME, System.currentTimeMillis() + 90 * 24 * 60 * 60 * 1000L);
        }
        else if(count == 1)
        {
            joUpdate.putString(colName.ZaloTetPromotionCol.GIFT_ID_2, giftId);
            joUpdate.putString(colName.ZaloTetPromotionCol.GIFT_TYPE_2, serviceId);
        }

        zaloTetPromotionDb.updatePartial(colName.ZaloTetPromotionCol.TABLE_ALL, zaloTetPromotionObj.zalo_code, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                callback.handle(event);
            }
        });
    }

    private void sendNotiAndTranHis(ZaloTetPromotionObj zaloTetPromotionObj, boolean enableGift, long tranId, Gift gift) {
        String notiCaption = enableGift ? PromoContentNotification.ZALO_GIFT_NOTI_TITLE : PromoContentNotification.PRE_ZALO_GIFT_NOTI_TITLE;
        String giftMessage = enableGift ? PromoContentNotification.ZALO_GIFT_GIFT_TRANHIS : PromoContentNotification.PRE_ZALO_GIFT_TRANHIS;
        String tranComment = enableGift ? PromoContentNotification.ZALO_GIFT_GIFT_TRANHIS : PromoContentNotification.PRE_ZALO_GIFT_TRANHIS;
        String notiBody = enableGift ? PromoContentNotification.ZALO_GIFT_NOTI_BODY : PromoContentNotification.PRE_ZALO_GIFT_NOTI_BODY;
        long total = value_of_gift + value_of_money;
        long value = enableGift ? total : value_of_money;
        String partnerName = enableGift ? "Lì Xì Táo Quân 2016" : "Chương trình Minigame cùng MoMo";
        String serviceId = "topup";
        gift = new Gift();
        if(!enableGift)
        {
            gift.setModelId("taoquan10k");
            gift.status = 3;
            gift.typeId = "taoquan10k";
        }
        else {
            gift.setModelId("taoquan2016");
            gift.status = 3;
            gift.typeId = "taoquan2016";
        }
        Misc.sendTranHisAndNotiZaloMoney(vertx
                , DataUtil.strToInt(zaloTetPromotionObj.phoneNumber)
                , tranComment
                , tranId
                , value
                , gift
                , notiCaption
                , notiBody
                , giftMessage
                , partnerName
                , serviceId
                , tranDb);
    }

    public void sendNotiViaGroup(final String phoneNumber, String group, JsonObject jsonObject) {

        String titleNoti = "";
        String bodyNoti = "";
        boolean usedVoucher1 = true;
        boolean usedVoucher2 = true;
        String amount = "50.000";
        String service = "điện";
        switch (group)
        {
            case "NOTI_GROUP_T":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_BODY;
                break;

            case "NOTI_GROUP_T_1_A":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_1_A_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_1_A_BODY;
                break;
            case "NOTI_GROUP_T_1_B":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_1_B_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_1_B_BODY;
                break;
            case "NOTI_GROUP_T_2":
                long time_2 = System.currentTimeMillis();
                if(jsonObject != null)
                {
                    time_2 = jsonObject.getLong(colName.ZaloTetPromotionCol.END_TIME_GIFT, System.currentTimeMillis());
                }
                titleNoti = PromoContentNotification.NOTI_GROUP_T_2_TITLE;
                bodyNoti = String.format(PromoContentNotification.NOTI_GROUP_T_2_BODY, Misc.dateVNFormatWithDot(time_2));
                break;
            case "NOTI_GROUP_T_3":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_3_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_3_BODY;
                break;
            case "NOTI_GROUP_T_4":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_4_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_4_BODY;
                break;

            case "NOTI_GROUP_T_5":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_5_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_5_BODY;
                break;

            case "NOTI_GROUP_T_6":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_6_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_6_BODY;
                break;
//            case "NOTI_GROUP_T_7":
//                titleNoti = PromoContentNotification.NOTI_GROUP_T_7_TITLE;
//                bodyNoti = PromoContentNotification.NOTI_GROUP_T_7_BODY;
//                break;
            case "NOTI_GROUP_T_14_A":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_14_A_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_14_A_BODY;
                break;
            case "NOTI_GROUP_T_14_B":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_14_B_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_14_B_BODY;
                break;
            case "NOTI_GROUP_T_21":
                long time_21 = System.currentTimeMillis();
                if(jsonObject != null)
                {
                    time_21 = jsonObject.getLong(colName.ZaloTetPromotionCol.END_TIME_GIFT, System.currentTimeMillis());
                }
                titleNoti = PromoContentNotification.NOTI_GROUP_T_21_TITLE;
                bodyNoti = String.format(PromoContentNotification.NOTI_GROUP_T_21_BODY, Misc.dateVNFormatWithDot(time_21));
                break;
            case "NOTI_GROUP_T_28":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_28_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_28_BODY;
                break;
            case "NOTI_GROUP_T_35":
                long time_35 = System.currentTimeMillis();
                if(jsonObject != null)
                {
                    time_35 = jsonObject.getLong(colName.ZaloTetPromotionCol.END_TIME_GIFT, System.currentTimeMillis());
                }
                titleNoti = PromoContentNotification.NOTI_GROUP_T_35_TITLE;
                bodyNoti = String.format(PromoContentNotification.NOTI_GROUP_T_35_BODY, Misc.dateVNFormatWithDot(time_35));
                break;
            case "NOTI_GROUP_T_42":
                usedVoucher1 = jsonObject.getBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, true);
                usedVoucher2 = jsonObject.getBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, true);
                if(usedVoucher1 && !usedVoucher2)
                {
                    amount = "50.000";
                    service = "123phim";
                }
                else if(!usedVoucher1 && usedVoucher2)
                {
                    amount = "50.000";
                    service = "điện";
                }
                else if(usedVoucher1 && usedVoucher2)
                {
                    amount = "150.000";
                    service = "điện, 123phim";
                }

                titleNoti = PromoContentNotification.NOTI_GROUP_T_42_TITLE;
                bodyNoti = String.format(PromoContentNotification.NOTI_GROUP_T_42_BODY, amount, service);
                break;
            case "NOTI_GROUP_T_45":
                usedVoucher1 = jsonObject.getBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, true);
                usedVoucher2 = jsonObject.getBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, true);

                if(usedVoucher1 && !usedVoucher2)
                {
                    amount = "50.000";
                    service = "123phim";
                }
                else if(!usedVoucher1 && usedVoucher2)
                {
                    amount = "50.000";
                    service = "điện";
                }
                else if(usedVoucher1 && usedVoucher2)
                {
                    amount = "150.000";
                    service = "điện, 123phim";
                }
                titleNoti = PromoContentNotification.NOTI_GROUP_T_45_TITLE;
                bodyNoti = String.format(PromoContentNotification.NOTI_GROUP_T_45_BODY, amount, service);
                break;
            case "NOTI_GROUP_T_50":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_50_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_50_BODY;
                break;
            case "NOTI_GROUP_T_70":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_70_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_70_BODY;
                break;
            case "NOTI_GROUP_T_80":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_80_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_80_BODY;
                break;
            case "NOTI_GROUP_T_90":
                titleNoti = PromoContentNotification.NOTI_GROUP_T_90_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_90_BODY;
                break;
            default:
                titleNoti = PromoContentNotification.NOTI_GROUP_T_TITLE;
                bodyNoti = PromoContentNotification.NOTI_GROUP_T_BODY;
                break;
        }
        final Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.caption = titleNoti;// "Nhận thưởng quà khuyến mãi";
        noti.body = bodyNoti;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        JsonObject joUrl = new JsonObject();
        joUrl.putString("url", url);
        noti.extra = joUrl.toString();
        Misc.sendNoti(vertx, noti);
        logger.info("Nhan tien 100k momo Thanh cong");
        return;
    }

    private String getTableFromCode(String groupNumber)
    {
        String collection = "";
        if(Integer.parseInt(groupNumber) == 1)
        {
            collection = "zalo_group_one";
        }
        else if(Integer.parseInt(groupNumber) == 2)
        {
            collection = "zalo_group_two";
        }
        else if(Integer.parseInt(groupNumber) == 3)
        {
            collection = "zalo_group_three";
        }
        else if(Integer.parseInt(groupNumber) == 4)
        {
            collection = "zalo_group_four";
        }
        else if(Integer.parseInt(groupNumber) == 5)
        {
            collection = "zalo_group_five";
        }
        else if(Integer.parseInt(groupNumber) == 6)
        {
            collection = "zalo_group_six";
        }
        else if(Integer.parseInt(groupNumber) == 7)
        {
            collection = "zalo_group_seven";
        }
        else if(Integer.parseInt(groupNumber) == 8)
        {
            collection = "zalo_group_eight";
        }
        else if(Integer.parseInt(groupNumber) == 9)
        {
            collection = "zalo_group_nine";
        }
        return collection;

    }

    private void scanExpiredLuckyMoney(final Common.BuildLog log)
    {
        JsonObject joFilter = new JsonObject();
        joFilter.putNumber(colName.ZaloTetPromotionCol.MONEY_STATUS, 0);

        JsonObject jsonLte = new JsonObject();
        jsonLte.putNumber(MongoKeyWords.LESS_THAN, System.currentTimeMillis());

        joFilter.putObject(colName.ZaloTetPromotionCol.END_TIME_MONEY, jsonLte);

        zaloTetPromotionDb.searchWithFilter(colName.ZaloTetPromotionCol.TABLE_ALL, joFilter, new Handler<ArrayList<ZaloTetPromotionDb.Obj>>() {
            @Override
            public void handle(final ArrayList<ZaloTetPromotionDb.Obj> listExpiredZalo) {
//                final JsonObject joReply = new JsonObject();
                if(listExpiredZalo.size() == 0)
                {
//                    joReply.putNumber(StringConstUtil.ERROR, 0);
//                    callback.handle(joReply);
                    return;
                }

                final AtomicInteger atomicInteger = new AtomicInteger(listExpiredZalo.size());
                vertx.setPeriodic(500L, new Handler<Long>() {
                    @Override
                    public void handle(Long timerPeriodic) {
                        final int itemPosition = atomicInteger.decrementAndGet();
                        if(itemPosition < 0)
                        {
                            vertx.cancelTimer(timerPeriodic);
                            return;
                        }
                        getBackLuckyMoney(listExpiredZalo.get(itemPosition), log, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject jsonCallback) {
                                int error = jsonCallback.getInteger(StringConstUtil.ERROR, -1);
                                long tranId = jsonCallback.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                if(error == 0)
                                {
                                    //Tru tien thanh cong, cap nhat DB
                                    updateZaloDb(listExpiredZalo.get(itemPosition).phone_number, log, tranId, listExpiredZalo.get(itemPosition).gift_id_1);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void getBackLuckyMoney(final ZaloTetPromotionDb.Obj zaloTetPromotionObj, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.add("desc", "getBackLuckyMoneyFromUser");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = StringConstUtil.ZaloPromo.ZALO_PROGRAM;
        Misc.adjustment(vertx, zaloTetPromotionObj.phone_number, agent, value_of_money,
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
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "core tra loi");
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
                        return;
                    }
                });
    }

    private void updateZaloDb(final String phoneNumber, final Common.BuildLog log, final long tranId, final String giftId) {
        JsonObject joUpdate = new JsonObject();
        joUpdate.putNumber(colName.ZaloTetPromotionCol.MONEY_STATUS, 2);
        zaloTetPromotionDb.updatePhoneNumberPartial(colName.ZaloTetPromotionCol.TABLE_ALL, phoneNumber, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                log.add("desc", "Da update zalo all voi sdt " + phoneNumber);
                sendNotiAndTranHisGetBackMoney(log, tranId, phoneNumber, value_of_money, giftId);
            }
        });
    }

    public void sendNotiAndTranHisGetBackMoney(Common.BuildLog log, final long tid,final String phoneNumber, long amount,final String giftId) {

        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.M2M_VALUE);
        jsonTrans.putString(colName.TranDBCols.COMMENT, PromoContentNotification.ZALO_GET_BACK_MONEY_TRANHIS);
        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tid);
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amount);
        jsonTrans.putNumber(colName.TranDBCols.STATUS, 4);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        jsonTrans.putString(colName.TranDBCols.BILL_ID, "");
        Misc.sendingStandardTransHisFromJsonWithCallback(vertx, tranDb, jsonTrans, new JsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                final Notification noti = new Notification();
                noti.priority = 2;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                noti.caption = "".equalsIgnoreCase(giftId) ? PromoContentNotification.ZALO_GET_BACK_PRE_MONEY_NOTI_TITLE : PromoContentNotification.ZALO_GET_BACK_MONEY_NOTI_TITLE;// "Nhận thưởng quà khuyến mãi";
                noti.body = "".equalsIgnoreCase(giftId) ? PromoContentNotification.ZALO_GET_BACK_PRE_MONEY_NOTI_BODY :PromoContentNotification.ZALO_GET_BACK_MONEY_NOTI_BODY;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                noti.tranId = tid;
                noti.time = new Date().getTime();

                noti.receiverNumber = DataUtil.strToInt(phoneNumber);
                Misc.sendNoti(vertx, noti);
            }
        });


        log.writeLog();

        logger.info("Nhan tien 100k momo Thanh cong");
        return;
    }

    public void sendNotiAndTranHisCashBack7Percent(Common.BuildLog log, final long tid,final String phoneNumber, final long amount) {

        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, 29);
        jsonTrans.putString(colName.TranDBCols.COMMENT, String.format(PromoContentNotification.ZALO_BONUS_7_PERCENT_TRANHIS, NotificationUtils.getAmount(amount)));
        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tid);
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amount);
        jsonTrans.putNumber(colName.TranDBCols.STATUS, 0);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        jsonTrans.putString(colName.TranDBCols.PARTNER_NAME, "Hoàn 10% GTGD");
        jsonTrans.putString(colName.TranDBCols.BILL_ID, "Hoàn 10% GTGD");
        jsonTrans.putNumber(colName.TranDBCols.IO, 1);
        jsonTrans.putNumber(colName.TranDBCols.STATUS, 4);
        Misc.sendingStandardTransHisFromJsonWithCallback(vertx, tranDb, jsonTrans, new JsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                final Notification noti = new Notification();
                noti.priority = 2;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                noti.caption = PromoContentNotification.ZALO_BONUS_7_PERCENT_NOTI_TITLE;// "Nhận thưởng quà khuyến mãi";
                noti.body = String.format(PromoContentNotification.ZALO_BONUS_7_PERCENT_NOTI_BODY, NotificationUtils.getAmount(amount));//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                noti.tranId = tid;
                noti.time = new Date().getTime();
                noti.receiverNumber = DataUtil.strToInt(phoneNumber);
                Misc.sendNoti(vertx, noti);
            }
        });


        log.writeLog();

        logger.info("Nhan tien 100k momo Thanh cong");
        return;
    }

    public void scanNotiGroup(final Queue<Integer> integerQueue,final Handler<Boolean> callback)
    {
        final int time = integerQueue.poll();
        if(time == 0)
        {
            logger.info("Da scan het item");
            return;
        }

        JsonObject joFilter = getConditionViaTime(time);

        zaloTetPromotionDb.searchWithFilter(colName.ZaloTetPromotionCol.TABLE_ALL, joFilter, new Handler<ArrayList<ZaloTetPromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<ZaloTetPromotionDb.Obj> listGroup) {
                ZaloNotiObj zaloNotiObj;
                for(int i = 0; i < listGroup.size(); i++)
                {
                    zaloNotiObj = new ZaloNotiObj();
                    zaloNotiObj.setGroup(getGroupViaTime(time));
                    zaloNotiObj.setPhoneNumber(listGroup.get(i).phone_number);
                    zaloNotiObj.setJsonData(listGroup.get(i).toJson());
                    arrNotiList.add(zaloNotiObj);
                }
                if(integerQueue.size() > 0)
                {
                    scanNotiGroup(integerQueue, callback);
                }
                else
                {
                    callback.handle(true);
                }
            }
        });

    }

    public JsonObject getConditionViaTime(int time)
    {
        long end_date = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * time;
        long start_date = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * (time - 1);
        JsonObject jsonGroup = new JsonObject();

        JsonObject joStartTime = new JsonObject();
        JsonObject joEndTime = new JsonObject();

        JsonObject joGreater = new JsonObject();
        JsonObject joLess = new JsonObject();

        joGreater.putNumber(MongoKeyWords.GREATER_OR_EQUAL, start_date);
        joLess.putNumber(MongoKeyWords.LESS_OR_EQUAL, end_date);

        joStartTime.putObject(colName.ZaloTetPromotionCol.TIME, joGreater);
        joEndTime.putObject(colName.ZaloTetPromotionCol.TIME, joLess);

        JsonArray jarrAnd = new JsonArray();
        jarrAnd.add(joStartTime);
        jarrAnd.add(joEndTime);

        JsonObject jsonAnd = new JsonObject();
        jsonAnd.putArray(MongoKeyWords.AND_$, jarrAnd);
        jsonGroup.putArray(colName.ZaloTetPromotionCol.TIME, jarrAnd);

        JsonObject jsonVoucher1 = new JsonObject();
        JsonObject jsonVoucher2 = new JsonObject();
        JsonArray jarrOr = new JsonArray();
        switch (time)
        {
            case 1:
                jsonGroup.putString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, "");
                break;
            case 2:
                jsonGroup.putString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, "");
                break;
            case 3:
                break;
            case 4:
                jsonGroup.putString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, "");
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, false);
                break;
            case 5:
                jsonGroup.putString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, "");
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, false);
                break;
            case 6:
                jsonGroup.putNumber(colName.ZaloTetPromotionCol.MONEY_STATUS, 0);
                break;
            case 14:
                break;
            case 21:
                jsonGroup.putString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, "");
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, false);
                break;
            case 28:
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
                break;
            case 35:
                jsonGroup.putString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, "");
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
                jsonGroup.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, false);
                break;
            case 42:
                jsonVoucher1.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
                jsonVoucher2.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, false);
                jarrOr.add(jsonVoucher1);
                jarrOr.add(jsonVoucher2);
                jsonGroup.putArray(MongoKeyWords.OR, jarrOr);
                break;
            case 45:
                jsonVoucher1.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
                jsonVoucher2.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, false);
                jarrOr.add(jsonVoucher1);
                jarrOr.add(jsonVoucher2);
                jsonGroup.putArray(MongoKeyWords.OR, jarrOr);
                break;
            case 50:
                break;
            case 70:
                break;
            case 80:
                break;
            case 90:
                break;
            default:
                break;
        }
        return jsonGroup;
    }

    public String getGroupViaTime(int time)
    {
        String group = "";
        switch (time)
        {
            case 1:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_1_B;
                break;
            case 2:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_2;
                break;
            case 3:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_3;
                break;
            case 4:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_4;
                break;
            case 5:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_5;
                break;
            case 6:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_6;
                break;
//            case 7:
//                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_7;
//                break;
            case 14:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_14_B;
                break;
            case 21:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_21;
                break;
            case 28:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_28;
                break;
            case 35:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_35;
                break;
            case 42:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_42;
                break;
            case 45:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_45;
                break;
            case 50:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_50;
                break;
            case 70:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_70;
                break;
            case 80:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_80;
                break;
            case 90:
                group = StringConstUtil.ZaloPromo.NOTI_GROUP_T_90;
                break;
            default:
                break;
        }
        return group;
    }
}
