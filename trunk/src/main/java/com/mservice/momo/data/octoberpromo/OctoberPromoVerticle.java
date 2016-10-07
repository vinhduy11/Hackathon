package com.mservice.momo.data.octoberpromo;

import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
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
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 10/15/15.
 */
public class OctoberPromoVerticle extends Verticle {

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
    private JsonObject jsonOctoberPromo;
    private HashMap<String, String> mappingService = new HashMap<>();

    private HashMap<String, String> mappingGift = new HashMap<>();
    private Common common;
    Common.BuildLog log;
    boolean isOctoberPromoActive = false;
    OctoberPromoUserManageDb octoberPromoUserManageDb;
    OctoberPromoManageDb octoberPromoManageDb;
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
        jsonOctoberPromo = glbCfg.getObject(StringConstUtil.OctoberPromoProgram.JSON_OBJECT, new JsonObject());

        //Them thong tin mapping dich vu
        mappingService.put("octv1", "Vé xem phim");
        mappingService.put("octv2", "Mua sắm");
        mappingService.put("octv3", "Dịch vụ phim online");
        mappingService.put("octv4", "Vé máy bay Jetstar");
        mappingService.put("octv5", "Điện lực");
        mappingService.put("octv6", "Cấp nước");
        mappingService.put("octv7", "Internet/điện thoại cố định");
        mappingService.put("octv8", "Vay tiêu dùng");
        mappingService.put("octv9", "Truyền hình cáp");
        mappingService.put("octv10", "Mã thẻ");
        mappingService.put("octv11", "Voucher khuyến mãi 50.000đ");

        mappingGift.put("0", "octv1");
        mappingGift.put("1", "octv2");
        mappingGift.put("2", "octv3");
        mappingGift.put("3", "octv4");
        mappingGift.put("4", "octv5");
        mappingGift.put("5", "octv6");
        mappingGift.put("6", "octv7");
        mappingGift.put("7", "octv8");
        mappingGift.put("8", "octv9");
        mappingGift.put("9", "octv10");
        mappingGift.put("10", "octv11");


        common = new Common(vertx, logger, container.config());
        isOctoberPromoActive = jsonOctoberPromo.getBoolean(StringConstUtil.OctoberPromoProgram.IS_ACTIVE, false);

        final long timeScanExpired = jsonOctoberPromo.getLong(StringConstUtil.OctoberPromoProgram.TIME_SCAN, 30);
        final long timeGivingGift = jsonOctoberPromo.getLong(StringConstUtil.OctoberPromoProgram.TIME_SCAN_GIVING_GIFT, 1440);
        octoberPromoUserManageDb = new OctoberPromoUserManageDb(vertx, logger);
        octoberPromoManageDb = new OctoberPromoManageDb(vertx, logger);
        log = new Common.BuildLog(logger);


        boolean scanExpiredGift = jsonOctoberPromo.getBoolean(StringConstUtil.OctoberPromoProgram.SCAN_EXPIRED_GIFT, false);
        if(scanExpiredGift)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 58);
            calendar.set(Calendar.SECOND, 50);
//            calendar.set(Calendar.HOUR_OF_DAY, 21);
//            calendar.set(Calendar.MINUTE, 20);
//            calendar.set(Calendar.SECOND, 00);
            long finalTime = calendar.getTimeInMillis();
            long timeDis = finalTime - System.currentTimeMillis();
            logger.info("FINAL TIME " + finalTime);
            logger.info("TIME DIS " + timeDis);
            vertx.setTimer(timeDis, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {

                    logger.info("OCTOBER setTimer");
                    vertx.setPeriodic(timeScanExpired * 60 * 1000L, new Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            // Quet voucher 1
                            logger.info("BEGIN TO SCAN OCTOBER PROMOTION TIME EXPIRED");
                            Common.BuildLog log = new Common.BuildLog(logger);
                            log.setPhoneNumber("timeScanExpired");
                            scanExpiredLuckyVoucher(log);
                            scanExpiredUnLuckyVoucher(log);
                            log.writeLog();
                        }
                    });

                    vertx.setPeriodic(timeGivingGift * 60 * 1000L, new Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            // Quet voucher 1
                            logger.info("BEGIN TO SCAN OCTOBER PROMOTION GIVING GIFT");
                            Common.BuildLog log = new Common.BuildLog(logger);
                            log.setPhoneNumber("timeGivingGift");
                            scanGivingOctoberGift(log);
                            log.writeLog();
                        }
                    });
                }
            });

        }

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final OctoberPromoObj octoberPromoObj = new OctoberPromoObj(reqJson);
                log.setPhoneNumber(octoberPromoObj.phoneNumber);
                log.add("phoneNumber", octoberPromoObj.phoneNumber);
                log.add("trantype", octoberPromoObj.tranType);
                log.add("tranid", octoberPromoObj.tranId);
                log.add("source", octoberPromoObj.source);

                final JsonObject jsonReply = new JsonObject();


                //Neu diem giao dich chuoi lot vo day thi khong tra thuong
//                if(isStoreApp)
//                {
//                    log.add("desc", "app diem giao dich khong cho choi october promo");
//                    log.writeLog();
//                    message.reply(jsonReply);
//                    return;
//                }
//                else
                if(!isOctoberPromoActive)
                {
                    log.add("desc", "Chua active chuong trinh");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }
                else if (octoberPromoObj.phoneNumber.equalsIgnoreCase("") || DataUtil.strToLong(octoberPromoObj.phoneNumber) <= 0) {
                    log.add("desc", "So dien thoai la so dien thoai khong co that.");
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
                else if(!StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO.equalsIgnoreCase(octoberPromoObj.source))
                {
                    log.add("desc", "Sai chuong trinh.");
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
                else if(octoberPromoObj.tranId == 0 || octoberPromoObj.tranType == 0)
                {
                    log.add("desc", "Khong co thong tin giao dich");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }
                else {
                    //Thoat het cac lop, bat dau tra khuyen mai
                    log.add("desc", "tra khuyen mai");
                    octoberPromoUserManageDb.findOne(octoberPromoObj.phoneNumber, new Handler<OctoberPromoUserManageDb.Obj>() {
                        @Override
                        public void handle(OctoberPromoUserManageDb.Obj octoberPromoUserManageObj) {
                            if(octoberPromoUserManageObj != null)
                            {
                                log.add("desc", "Co thong tin khach hang trong bang luu tru");
                                processCheckOctoberPromoProgram(octoberPromoObj, octoberPromoUserManageObj, false, message);
                                return;
                            }
                            else
                            {
                                log.add("desc", "Khong co thong tin khach hang trong bang luu tru");
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }




            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.OCTOBER_PROMO_BUSS_ADDRESS, myHandler);
    }


    private void processCheckOctoberPromoProgram(final OctoberPromoObj octoberPromoObj,final OctoberPromoUserManageDb.Obj octoberPromoUserManageObj, final boolean auto, final Message message)
    {
        final JsonObject jsonReply = new JsonObject();
        if(!auto)
        {
            log.add("desc", "Khong chay auto, khach hang nap tien vao vi");
            octoberPromoManageDb.findOne(octoberPromoObj.phoneNumber, new Handler<OctoberPromoManageDb.Obj>() {
                @Override
                public void handle(OctoberPromoManageDb.Obj octoberPromoManageObj) {
                    if(octoberPromoManageObj == null)
                    {
                        //Khach hang chua nhan qua
                        log.add("desc", "Khach hang chua nhan qua nao, tra qua cho khach");
                        giveOctoberPromoGift(message, octoberPromoObj, octoberPromoUserManageObj, jsonReply, 1);
                        return;
                    }
                    else
                    {
                        log.add("desc", "Khach hang da nhan qua, khong tra qua");
                        log.writeLog();
                        jsonReply.putNumber("error", 1000);
                        jsonReply.putString("desc", "Khach hang da nhan qua, khong tra qua");
                        message.reply(jsonReply);
                        return;
                    }


                }
            });
            return;
        }


        log.add("desc", "He thong chay auto tra qua theo thang");
        //todo: Hien thuc tra qua tu dong (10 thang)
        return;
    }


    private void giveOctoberPromoGift(final Message message, final OctoberPromoObj octoberPromoObj, final OctoberPromoUserManageDb.Obj octoberPromoUserManageObj, final JsonObject joReply, final int group) {
        log.add("func", "giveOctoberPromoGift");
        final String agentName = jsonOctoberPromo.getString(StringConstUtil.OctoberPromoProgram.AGENT, "cskh");
        final int timeForGift = jsonOctoberPromo.getInteger(StringConstUtil.OctoberPromoProgram.TIME_FOR_GIFT, 15);
        final long giftValue = jsonOctoberPromo.getLong(StringConstUtil.OctoberPromoProgram.VALUE_OF_GIFT, 50000);
        int number_of_lucky_gift = jsonOctoberPromo.getInteger(StringConstUtil.OctoberPromoProgram.NUMBER_OF_LUCKY_GIFT, 10);
        long totalGiftValue = giftValue;
        final int numberOfVoucher = octoberPromoUserManageObj.is_lucky_man ? number_of_lucky_gift : 1;
        if (octoberPromoUserManageObj.is_lucky_man) {
            totalGiftValue = number_of_lucky_gift * giftValue;
        }
        final List<String> listVoucher = loadListVoucher(numberOfVoucher);
        log.add("list voucher", listVoucher.size() + " " + listVoucher.toString());
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO));

        giftManager.adjustGiftValue(agentName
                , octoberPromoObj.phoneNumber
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
                    final String note = octoberPromoObj.source;

                    //for (int i = 0; i < listVoucher.size(); i++) {
                    keyValues.clear();
                    kv.Key = "group";
                    kv.Value = octoberPromoObj.source + "_" + group;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(numberOfVoucher);
                    log.add("so luong voucher", atomicInteger);

                    vertx.setPeriodic(500L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {

                            if (atomicInteger.decrementAndGet() < 0) {
                                log.add("func", "out of range for number " + octoberPromoObj.phoneNumber);
                                log.writeLog();
                                vertx.cancelTimer(aPeriodicLong);
                                return;
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                log.add("itemPosition", itemPosition);
                                giftType.setModelId(listVoucher.get(itemPosition).trim());
                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(octoberPromoObj.phoneNumber
                                        , giftValue
                                        , giftType
                                        , promotedTranId
                                        , agentName
                                        , modifyDate
                                        , timeForGift
                                        , keyValues, note, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {
                                        int err = jsonObject.getInteger("error", -1);
                                        final long tranId = jsonObject.getInteger("tranId", -1);
                                        final Gift gift = new Gift(jsonObject.getObject("gift"));
                                        final String giftId = gift.getModelId().trim();
                                        log.add("desc", "tra thuong chuong trinh october bang gift");
                                        log.add("giftId", giftId);
                                        log.add("err", err);

                                        //------------tat ca thanh cong roi
                                        if (err == 0) {
                                            insertOctoberPromoInfo(message, group, octoberPromoObj, octoberPromoUserManageObj, timeForGift, giftId, itemPosition + 1, listVoucher, gift, tranId);
                                            return;
                                        } else {
                                            log.add("desc", "loi khi tao local gift");
                                            joReply.putNumber("error", 1000);
                                            joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                            //message.reply(joReply);
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
                    log.writeLog();
                    return;
                }
            }
        });

    }

    private void insertOctoberPromoInfo(final Message message, int group,final OctoberPromoObj octoberObj,final OctoberPromoUserManageDb.Obj octoberPromoUserManageObj, final int duration, String giftId,final int position, final List<String> listVoucher, final Gift gift, final long tranId)
    {
        long endTimeGift = 1000L * 60 * 60 * 24 * duration;
        long nextMonthDuration = 1000L * 60 * 60 * 24 * 30;
        final OctoberPromoManageDb.Obj octoberPromoManageObj = new OctoberPromoManageDb.Obj();
        octoberPromoManageObj.tran_id = octoberObj.tranId;
        octoberPromoManageObj.tran_type = octoberObj.tranType;
        octoberPromoManageObj.is_lucky_man = octoberPromoUserManageObj.is_lucky_man;
        octoberPromoManageObj.card_check_sum = octoberObj.cardCheckSum;
        octoberPromoManageObj.cmnd = octoberObj.cmnd;
        octoberPromoManageObj.phone_number = octoberObj.phoneNumber;
        octoberPromoManageObj.start_time_gift_received = System.currentTimeMillis();
        octoberPromoManageObj.end_time_gift_received= System.currentTimeMillis() + endTimeGift;
        octoberPromoManageObj.next_time_to_receive_gift = System.currentTimeMillis();
        octoberPromoManageObj.auto_time_to_receive_gift = System.currentTimeMillis();
        String id = octoberObj.phoneNumber + "_" + group;
        octoberPromoManageObj.id = id;
        if(octoberPromoUserManageObj.is_lucky_man)
        {
            octoberPromoManageObj.next_time_to_receive_gift = System.currentTimeMillis() + nextMonthDuration;
        }
        octoberPromoManageObj.promo_count = group;
        octoberPromoManageObj.status = 0;
        JsonObject joUpdate = octoberPromoManageObj.toJsonWithNotGiftId();

        if(position == 1)
        {
            octoberPromoManageObj.gift_id_1 = giftId;
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_1, giftId);
        }
        else if(position == 2)
        {
            octoberPromoManageObj.gift_id_2 = giftId;
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_2, giftId);
        }
        else if(position == 3)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_3, giftId);
            octoberPromoManageObj.gift_id_3 = giftId;
        }
        else if(position == 4)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_4, giftId);
            octoberPromoManageObj.gift_id_4 = giftId;
        }
        else if(position == 5)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_5, giftId);
            octoberPromoManageObj.gift_id_5 = giftId;
        }
        else if(position == 6)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_6, giftId);
            octoberPromoManageObj.gift_id_6 = giftId;
        }
        else if(position == 7)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_7, giftId);
            octoberPromoManageObj.gift_id_7 = giftId;
        }
        else if(position == 8)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_8, giftId);
            octoberPromoManageObj.gift_id_8 = giftId;
        }
        else if(position == 9)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_9, giftId);
            octoberPromoManageObj.gift_id_9 = giftId;
        }
        else if(position == 10)
        {
            joUpdate.putString(colName.OctoberPromoManageCols.GIFT_ID_10, giftId);
            octoberPromoManageObj.gift_id_10 = giftId;
        }




        if(octoberPromoManageObj.is_lucky_man && position != 10)
        {
            joUpdate.removeField(colName.OctoberPromoManageCols.ID);
        }
        octoberPromoManageDb.update(id, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                fireNotiAndCreateTranHis(message, position, octoberPromoManageObj, listVoucher, gift, duration, tranId);
            }
        });
    }

    private void fireNotiAndCreateTranHis(Message message, int position, OctoberPromoManageDb.Obj octoberPromoManageObj, List<String> listVoucher, Gift gift, int duration, long tranId)
    {
        JsonObject joReply = new JsonObject();
        if (position == 1) {
            String giftMessage = "giftMessage";
            String notiCaption = "notiCaption";
            String tranComment = "tranComment";
            String notiBody = "notiBody";

            long timeforgift = 60 * 1000L * 60 * 24 * duration;
            long endTime = System.currentTimeMillis() + timeforgift;
            String date = Misc.dateVNFormatWithDot(endTime);
            long value = gift.amount;
            notiCaption = "Nhận thẻ quà tặng";
            if (octoberPromoManageObj.is_lucky_man) {
                notiCaption = "Nhận thưởng ưu đãi 5 triệu đồng";
                if(octoberPromoManageObj.promo_count == 1)
                {
                    giftMessage =PromoContentNotification.OCTOBER_PROMO_LUCKY;
                    tranComment = PromoContentNotification.OCTOBER_PROMO_LUCKY;
                    notiBody = PromoContentNotification.OCTOBER_PROMO_LUCKY;
                }
                else
                {
                    giftMessage = String.format(PromoContentNotification.OCTOBER_PROMO_LUCKY_N, octoberPromoManageObj.promo_count);
                    tranComment = String.format(PromoContentNotification.OCTOBER_PROMO_LUCKY_N, octoberPromoManageObj.promo_count);
                    notiBody = String.format(PromoContentNotification.OCTOBER_PROMO_LUCKY_N, octoberPromoManageObj.promo_count);
                }
            }
            else {
                notiCaption = "Nhận thưởng ưu đãi 50.000đ";
                giftMessage = PromoContentNotification.OCTOBER_PROMO_UNLUCKY;
                tranComment = PromoContentNotification.OCTOBER_PROMO_UNLUCKY;
                notiBody = PromoContentNotification.OCTOBER_PROMO_UNLUCKY;
                value = listVoucher.size() * gift.amount;
            }

            Misc.sendTranHisAndNotiBillPayGift(vertx
                    , DataUtil.strToInt(octoberPromoManageObj.phone_number)
                    , tranComment
                    , tranId
                    , value
                    , gift
                    , notiCaption
                    , notiBody
                    , giftMessage
                    , tranDb);
            //------------thong bao nhan qua neu la nhom 1
            joReply.putNumber("error", 0);
            joReply.putString("desc", "Thành công");
            //message.reply(joReply);
            log.writeLog();
            return;
        }
    }

    private List<String> loadListVoucher(int numberOfVoucher) {
        List<String> listVoucher = new ArrayList<>();
        if (numberOfVoucher == 1) {
            listVoucher.add(mappingGift.get("10"));
        } else if (numberOfVoucher > 1) {
            for (int i = 0; i < numberOfVoucher; i++) {
                listVoucher.add(mappingGift.get(i + ""));
            }
        }
        return listVoucher;
    }


    //Quet qua iron man va ban noti lan 3
    //Quet qua va ban noti lan 2
    private void scanExpiredLuckyVoucher(final Common.BuildLog log)
    {
        log.add("func", "scanExpiredLuckyVoucher");

        long time = 3;
        final long next10days = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L;
        final long next5days = System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000L;
        final long next1days = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000L;

        octoberPromoManageDb.findExpiredLuckyVoucher(time, new Handler<ArrayList<OctoberPromoManageDb.Obj>>() {
            @Override
            public void handle(ArrayList<OctoberPromoManageDb.Obj> octoberObjs) {
                log.add("size gift voucher october", octoberObjs);
                if (octoberObjs.size() > 0) {
                    log.add("desc", "co vai be chua dung voucher october va sap het han");
                    boolean fireNoti = false;
                    Notification notification = null;
                    final ArrayList<OctoberPromoManageDb.Obj> firedNotiList = new ArrayList<OctoberPromoManageDb.Obj>();
                    List<String> listVoucher = null;
                    for (OctoberPromoManageDb.Obj octoberObj : octoberObjs) {
                        //Nhung dua chua dung voucher 1 va co time < 12h
                        fireNoti = false;
                        notification = new Notification();
                        listVoucher = new ArrayList<String>();
                        if (!octoberObj.used_voucher_1) {
                            listVoucher.add(mappingService.get("octv1"));
                        }
                        if (!octoberObj.used_voucher_2) {
                            listVoucher.add(mappingService.get("octv2"));
                        }
                        if (!octoberObj.used_voucher_3) {
                            listVoucher.add(mappingService.get("octv3"));
                        }
                        if (!octoberObj.used_voucher_4) {
                            listVoucher.add(mappingService.get("octv4"));
                        }
                        if (!octoberObj.used_voucher_5) {
                            listVoucher.add(mappingService.get("octv5"));
                        }
                        if (!octoberObj.used_voucher_6) {
                            listVoucher.add(mappingService.get("octv6"));
                        }
                        if (!octoberObj.used_voucher_7) {
                            listVoucher.add(mappingService.get("octv7"));
                        }
                        if (!octoberObj.used_voucher_8) {
                            listVoucher.add(mappingService.get("octv8"));
                        }
                        if (!octoberObj.used_voucher_9) {
                            listVoucher.add(mappingService.get("octv9"));
                        }
                        if (!octoberObj.used_voucher_10) {
                            listVoucher.add(mappingService.get("octv10"));
                        }

                        if (listVoucher.size() > 0) {
                            if (octoberObj.end_time_gift_received <= next10days && octoberObj.number_of_noti < 1) {
                                log.add("desc", "Kiem tra so " + octoberObj.phone_number);
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 5 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 1;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            } else if (octoberObj.end_time_gift_received <= next5days && octoberObj.number_of_noti < 2) {
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 10 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 2;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            } else if (octoberObj.end_time_gift_received <= next1days && octoberObj.number_of_noti < 3) {
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 14 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 3;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            } else if (octoberObj.end_time_gift_received <= System.currentTimeMillis() && octoberObj.number_of_noti < 4) {
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 15 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 4;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            }
                        }
                        if (fireNoti) {
                            Misc.sendNoti(vertx, notification);
                        }
                    }

                    final AtomicInteger listOctoberGift = new AtomicInteger(firedNotiList.size());

                    vertx.setPeriodic(500L, new Handler<Long>() {
                        @Override
                        public void handle(Long periTime) {
                            int userPosition = listOctoberGift.decrementAndGet();
                            if (userPosition < 0) {
                                log.add("desc", "Da update het thong tin ban noti october");
                                vertx.cancelTimer(periTime);
                                return;
                            } else {
                                JsonObject joUpdate = new JsonObject();
                                joUpdate.putNumber(colName.OctoberPromoManageCols.NUMBER_OF_NOTI, firedNotiList.get(userPosition).number_of_noti);
                                if (firedNotiList.get(userPosition).number_of_noti == 4) {
                                    joUpdate.putNumber(colName.OctoberPromoManageCols.STATUS, 1);
                                }
                                octoberPromoManageDb.updatePartial(firedNotiList.get(userPosition).id, joUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean event) {

                                    }
                                });
                            }
                        }
                    });
                } else {
                    log.add("desc", "Chua co qua nao sap het han ca");
                }
                log.writeLog();
            }
        });
    }

    private void scanExpiredUnLuckyVoucher(final Common.BuildLog log)
    {
        log.add("func", "scanExpiredUnLuckyVoucher");

        long time = 3;
        final long next10days = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L;
        final long next5days = System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000L;
        final long next1days = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000L;

        octoberPromoManageDb.findExpiredUnLuckyVoucher(time, new Handler<ArrayList<OctoberPromoManageDb.Obj>>() {
            @Override
            public void handle(ArrayList<OctoberPromoManageDb.Obj> octoberObjs) {
                log.add("size gift voucher group unlucky October", octoberObjs);
                if (octoberObjs.size() > 0) {
                    log.add("desc", "co vai be chua dung voucher group unlucky October va sap het han");
                    boolean fireNoti = false;
                    Notification notification = null;
                    final ArrayList<OctoberPromoManageDb.Obj> firedNotiList = new ArrayList<OctoberPromoManageDb.Obj>();
                    List<String> listVoucher = null;
                    for (OctoberPromoManageDb.Obj octoberObj : octoberObjs) {
                        //Nhung dua chua dung voucher 1 va co time < 12h
                        fireNoti = false;
                        notification = new Notification();
                        listVoucher = new ArrayList<String>();
                        if (!octoberObj.used_voucher_1) {
                            listVoucher.add(mappingService.get("octv11"));
                        }

                        if (listVoucher.size() > 0) {
                            if (octoberObj.end_time_gift_received <= next10days && octoberObj.number_of_noti < 1) {
                                log.add("desc", "Kiem tra so " + octoberObj.phone_number);
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 5 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 1;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredUnLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            } else if (octoberObj.end_time_gift_received <= next5days && octoberObj.number_of_noti < 2) {
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 10 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 2;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredUnLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            } else if (octoberObj.end_time_gift_received <= next1days && octoberObj.number_of_noti < 3) {
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 14 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 3;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredUnLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            } else if (octoberObj.end_time_gift_received <= System.currentTimeMillis() && octoberObj.number_of_noti < 4) {
                                log.add("desc", "so " + octoberObj.phone_number + " co voucher chua dung trong 15 ngay, ban noti");
                                fireNoti = true;
                                octoberObj.number_of_noti = 4;
                                firedNotiList.add(octoberObj);
                                notification = createExpiredUnLuckyVoucherNoti(octoberObj.phone_number, listVoucher, octoberObj.number_of_noti);
                            }
                        }
                        if (fireNoti) {
                            Misc.sendNoti(vertx, notification);
                        }
                    }

                    final AtomicInteger listOctoberGift = new AtomicInteger(firedNotiList.size());

                    vertx.setPeriodic(500L, new Handler<Long>() {
                        @Override
                        public void handle(Long periTime) {
                            int userPosition = listOctoberGift.decrementAndGet();
                            if (userPosition < 0) {
                                log.add("desc", "Da update het thong tin ban noti 3");
                                vertx.cancelTimer(periTime);
                                return;
                            } else {
                                JsonObject joUpdate = new JsonObject();
                                joUpdate.putNumber(colName.OctoberPromoManageCols.NUMBER_OF_NOTI, firedNotiList.get(userPosition).number_of_noti);
                                if (firedNotiList.get(userPosition).number_of_noti == 4) {
                                    joUpdate.putNumber(colName.OctoberPromoManageCols.STATUS, 1);
                                }
                                octoberPromoManageDb.updatePartial(firedNotiList.get(userPosition).id, joUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean event) {

                                    }
                                });
                            }
                        }
                    });
                } else {
                    log.add("desc", "Chua co qua nao sap het han ca");
                }
                log.writeLog();
            }
        });
    }

    private void scanGivingOctoberGift(final Common.BuildLog log)
    {
        log.add("func", "scanGivingOctoberGift");

        long time = 15;

        octoberPromoManageDb.findLuckyNumber(time, new Handler<ArrayList<OctoberPromoManageDb.Obj>>() {
            @Override
            public void handle(ArrayList<OctoberPromoManageDb.Obj> octoberObjs) {
                log.add("size of list lucky number", octoberObjs);
                if (octoberObjs.size() > 0) {
                    log.add("desc", "co vai be duoc tra thuong tu dong hom nay");
                    final ArrayList<OctoberPromoManageDb.Obj> listOctoberObj = new ArrayList<OctoberPromoManageDb.Obj>();
                    for (OctoberPromoManageDb.Obj octoberObj : octoberObjs) {
                        //Nhung dua chua dung voucher 1 va co time < 12h
                        listOctoberObj.add(octoberObj);
                    }

                    final AtomicInteger listOctoberGift = new AtomicInteger(listOctoberObj.size());

                    vertx.setPeriodic(5000L, new Handler<Long>() {
                        @Override
                        public void handle(Long periTime) {
                            final int userPosition = listOctoberGift.decrementAndGet();
                            if (userPosition < 0) {
                                log.add("desc", "Da update het thong tin ban noti october");
                                vertx.cancelTimer(periTime);
                                return;
                            } else {
                                final OctoberPromoObj octoberPromoObj = new OctoberPromoObj();
                                octoberPromoObj.phoneNumber = listOctoberObj.get(userPosition).phone_number;
                                octoberPromoObj.source = StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO;

                                final OctoberPromoUserManageDb.Obj octoberPromoUser = new OctoberPromoUserManageDb.Obj();
                                octoberPromoUser.is_lucky_man = listOctoberObj.get(userPosition).is_lucky_man;
                                octoberPromoUser.phone_number = listOctoberObj.get(userPosition).phone_number;

                                JsonObject jsonUpdate = new JsonObject();
                                jsonUpdate.putNumber(colName.OctoberPromoManageCols.STATUS, 2);
                                octoberPromoManageDb.updatePartial(listOctoberObj.get(userPosition).id, jsonUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {
                                        giveOctoberPromoGift(null, octoberPromoObj, octoberPromoUser, new JsonObject(), listOctoberObj.get(userPosition).promo_count + 1);
                                    }
                                });

                            }
                        }
                    });
                } else {
                    log.add("desc", "Chua co qua nao sap het han ca");
                }
                log.writeLog();
            }
        });
    }

    private Notification createExpiredLuckyVoucherNoti(String phoneNumber, List<String> listVoucher, int number_of_noti)
    {
        String caption = "";
        String body = "";
        String service = "";
        for(int i = 0; i < listVoucher.size(); i++)
        {
            if(i == 0)
            {
                service = service + listVoucher.get(i).toString();
            }
            else if (i <= 3)
            {
                service = service + ", " + listVoucher.get(i).toString();
            }
            else{
                break;
            }
        }
        if(number_of_noti == 1)
        {
            caption = PromoContentNotification.OCTOBER_PROMO_10DAYS_LUCKY_CAPTION;
            body = String.format(PromoContentNotification.OCTOBER_PROMO_10DAYS_LUCKY_BODY, listVoucher.size() + "");
        }
        else if(number_of_noti == 2){
            caption = PromoContentNotification.OCTOBER_PROMO_5DAYS_LUCKY_CAPTION;
            body = String.format(PromoContentNotification.OCTOBER_PROMO_5DAYS_LUCKY_BODY, listVoucher.size() + "");
        }
        else if(number_of_noti == 3){
            caption = PromoContentNotification.OCTOBER_PROMO_1DAYS_LUCKY_CAPTION;
            body = String.format(PromoContentNotification.OCTOBER_PROMO_1DAYS_LUCKY_BODY, listVoucher.size() + "");
        }
        else if(number_of_noti == 4){
            caption = PromoContentNotification.OCTOBER_PROMO_0DAYS_LUCKY_CAPTION;
            body = PromoContentNotification.OCTOBER_PROMO_0DAYS_LUCKY_BODY;
        }
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_VOUCHER_VIEW_VALUE;
        noti.caption = caption;
        noti.body = body;
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        return noti;
    }

    private Notification createExpiredUnLuckyVoucherNoti(String phoneNumber, List<String> listVoucher, int number_of_noti)
    {
        String caption = "";
        String body = "";
        String service = "";
        for(int i = 0; i < listVoucher.size(); i++)
        {
            if(i == 0)
            {
                service = service + listVoucher.get(i).toString();
            }
            else if (i <= 3)
            {
                service = service + ", " + listVoucher.get(i).toString();
            }
            else{
                break;
            }
        }
        if(number_of_noti == 1)
        {
            caption = PromoContentNotification.OCTOBER_PROMO_10DAYS_UNLUCKY_CAPTION;
            body = String.format(PromoContentNotification.OCTOBER_PROMO_10DAYS_UNLUCKY_BODY);
        }
        else if(number_of_noti == 2){
            caption = PromoContentNotification.OCTOBER_PROMO_5DAYS_UNLUCKY_CAPTION;
            body = String.format(PromoContentNotification.OCTOBER_PROMO_5DAYS_UNLUCKY_BODY);
        }
        else if(number_of_noti == 3){
            caption = PromoContentNotification.OCTOBER_PROMO_1DAYS_UNLUCKY_CAPTION;
            body = String.format(PromoContentNotification.OCTOBER_PROMO_1DAYS_UNLUCKY_BODY);
        }
        else if(number_of_noti == 4){
            caption = PromoContentNotification.OCTOBER_PROMO_0DAYS_UNLUCKY_CAPTION;
            body = String.format(PromoContentNotification.OCTOBER_PROMO_0DAYS_UNLUCKY_BODY);
        }
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_VOUCHER_VIEW_VALUE;
        noti.caption = caption;
        noti.body = body;
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        return noti;
    }
}
