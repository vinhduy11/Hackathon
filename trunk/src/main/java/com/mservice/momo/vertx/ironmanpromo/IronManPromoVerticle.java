package com.mservice.momo.vertx.ironmanpromo;

import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.ironmanpromote.IronManBonusTrackingTableDb;
import com.mservice.momo.data.ironmanpromote.IronManNewRegisterTrackingDb;
import com.mservice.momo.data.ironmanpromote.IronManPromoGiftDB;
import com.mservice.momo.data.ironmanpromote.IronManRandomGiftManageDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
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
* Created by concu on 9/7/15.
*/
public class IronManPromoVerticle extends Verticle {


    long time20days = 1000L * 60 * 60 * 24 * 20;
    JsonObject sbsCfg;
    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private TransDb tranDb;
    private AgentsDb agentsDb;
    private DBProcess dbProcess;
    private GiftDb giftDb;
    private PhonesDb phonesDb;
    private Card card;
    private JsonObject jsonOcbPromo;
    private MappingWalletBankDb mappingWalletBankDb;
    private boolean isStoreApp;
    private JsonObject jsonIronmanPromo;
    private IronManPromoGiftDB ironManPromoGiftDB;
    private HashMap<String, String> mappingService = new HashMap<>();
    private IronManNewRegisterTrackingDb ironManNewRegisterTrackingDb;
    private IronManBonusTrackingTableDb ironManBonusTrackingTableDb;
    private JsonObject ironManPlus;
    private Common common;
    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.sbsCfg = glbCfg.getObject("cybersource", null);
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);
        this.agentsDb = new AgentsDb(vertx.eventBus(), logger);
        this.giftManager = new GiftManager(vertx, logger, glbCfg);
        this.giftDb = new GiftDb(vertx, logger);
        this.card = new Card(vertx.eventBus(), logger);
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);
        jsonOcbPromo = glbCfg.getObject(StringConstUtil.OBCPromo.JSON_OBJECT, new JsonObject());
        mappingWalletBankDb = new MappingWalletBankDb(vertx, logger);
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        jsonIronmanPromo = glbCfg.getObject(StringConstUtil.IronManPromo.JSON_OBJECT, new JsonObject());
        ironManPromoGiftDB = new IronManPromoGiftDB(vertx, logger);
        ironManNewRegisterTrackingDb = new IronManNewRegisterTrackingDb(vertx,logger);
        ironManBonusTrackingTableDb = new IronManBonusTrackingTableDb(vertx, logger);
        this.ironManPlus = glbCfg.getObject(StringConstUtil.IronManPromoPlus.JSON_OBJECT, new JsonObject());
        //Them thong tin mapping dich vu
        mappingService.put("ironmanv2", "Qùa tặng Vé xem phim");
        mappingService.put("ironmanv3", "Qùa tặng eVoucher");
        mappingService.put("ironmanv4", "Qùa tặng Vay tiêu dùng cá nhân");
        mappingService.put("ironmanv5", "Qùa tặng eMovie card");
        mappingService.put("ironmanv6", "Qùa tặng Vé máy bay Jetstar");
        mappingService.put("ironmanv7", "Qùa tặng Internet");
        mappingService.put("ironmanv8", "Qùa tặng Điện lực Hồ Chí Minh");
        mappingService.put("ironmanv9", "Qùa tặng Cấp Nước");
        mappingService.put("ironmanv10", "Qùa tặng Vé máy bay Jetstar");
        mappingService.put("ironmanv11", "Qùa tặng Truyền hình cáp");
        mappingService.put("ironmanv12", "Qùa tặng SCJ Life On");
        mappingService.put("ironmanv13", "Qùa tặng Nạp tài khoản Fshare");
        mappingService.put("ironmanv14", "Qùa tặng Thẻ diệt Virus");
        common = new Common(vertx, logger, glbCfg);

        //String vinaProxyVerticle = "vinaphoneProxyVerticle";
        //Khong load duoc agent tra thuong

        long timeScanExpired = jsonIronmanPromo.getLong(StringConstUtil.IronManPromo.TIME_SCAN_EXPIRED_GIFT, 30);
        long timeScanMapping = jsonIronmanPromo.getLong(StringConstUtil.IronManPromo.TIME_SCAN_MAPPING, 24 * 60);

        long timeScanPlusRemind = ironManPlus.getLong(StringConstUtil.IronManPromoPlus.TIME_SCAN_PERIOD, 10);
        //QUet dich vu Iron man de nhac nho user su dung qua

        vertx.setPeriodic(timeScanExpired * 60 * 1000L, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                // Quet voucher 1
                logger.info("BEGIN TO SCAN TIME EXPIRED");
                Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("timeScanExpired");
                scanVoucherOneGroup(log);
                scanVoucherTwoGroup(log);
                scanVoucherThreeGroup(log);
                log.writeLog();
                //Quet cac voucher con lai
            }
        });

        vertx.setPeriodic(timeScanPlusRemind * 60 * 1000L, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                // Quet voucher 1
                logger.info("BEGIN TO SCAN TIME REMIND FOR PLUS");
                Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("timeScanPlusRemind");
                scanVoucherFourGroup(log);
                rollbackVoucherFourGroup(log);
                log.writeLog();
                //Quet cac voucher con lai
            }
        });

//        vertx.setPeriodic(timeScanMapping * 60 * 1000L, new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                logger.info("BEGIN TO SCAN TIME MAPPING");
//                Common.BuildLog log = new Common.BuildLog(logger);
//                log.setPhoneNumber("timeScanMapping");
//                scanVisaMapping(log);
//                log.writeLog();
//            }
//        });

//        vertx.setPeriodic(50L, new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                saveInfoIronManDb()
//            }
//        });


        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final IronManPromoObj ironManPromoObj = new IronManPromoObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(ironManPromoObj.phoneNumber);
                log.add("phoneNumber", ironManPromoObj.phoneNumber);
                log.add("trantype", ironManPromoObj.tranType);
                log.add("tranid", ironManPromoObj.tranId);
                log.add("source", ironManPromoObj.source);
                log.add("serviceId", ironManPromoObj.serviceId);

                final JsonObject jsonReply = new JsonObject();


                //Neu diem giao dich chuoi lot vo day thi khong tra thuong
                if(isStoreApp)
                {
                    log.add("desc", "app diem giao dich khong cho choi ironman promo");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }

                if (ironManPromoObj.phoneNumber.equalsIgnoreCase("") || DataUtil.strToLong(ironManPromoObj.phoneNumber) <= 0) {
                    log.add("desc", "So dien thoai la so dien thoai khong co that.");
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }

                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        log.add("phoneNumber", ironManPromoObj.phoneNumber);
                        log.add("serviceId", ironManPromoObj.serviceId);
                        log.add("source", ironManPromoObj.source);
                        log.add("tranType", ironManPromoObj.tranType);
                        log.add("tranId", ironManPromoObj.tranId);

                        long iron_man_promo_start_date = 0;
                        long iron_man_promo_end_date = 0;

                        long iron_man_promo_start_date_later = 0;
                        long iron_man_promo_end_date_later = 0;

                        long currentTime = System.currentTimeMillis();
                        if (array != null && array.size() > 0) {
                            ArrayList<PromotionDb.Obj> objs = new ArrayList<>();
                            PromotionDb.Obj iron_promo = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
//                                        objs.add(new PromotionDb.Obj((JsonObject) o));
                                iron_promo = new PromotionDb.Obj((JsonObject) o);
                                if (iron_promo.NAME.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO)) {
                                    iron_man_promo_start_date = iron_promo.DATE_FROM;
                                    iron_man_promo_end_date = iron_promo.DATE_TO;
                                }
                                else if (iron_promo.NAME.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_LATER)) {
                                    iron_man_promo_start_date_later = iron_promo.DATE_FROM;
                                    iron_man_promo_end_date_later = iron_promo.DATE_TO;
                                }
                            }


                            //Kiem tra thoi gian
                            if(currentTime < iron_man_promo_start_date_later && currentTime > iron_man_promo_end_date_later && ironManPromoObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_4))
                            {
                                //Chua den thoi gian tra thuong cho chau zai nay
                                log.add("func", "Chua mo cua tham gia lv2 nhoa");
                                log.writeLog();
                                message.reply(jsonReply);
                                return;
                            }
                            else if(currentTime >= iron_man_promo_start_date_later && currentTime <= iron_man_promo_end_date_later && ironManPromoObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_4))
                            {
                                log.add("func", "chu duoc tham gia level nay nhe");
                                checkTypeOfIronManSource(ironManPromoObj, log, message);

                            }
                            else if(currentTime < iron_man_promo_start_date && currentTime > iron_man_promo_end_date)
                            {
                                //Chua den thoi gian tra thuong cho chau zai nay
                                log.add("func", "Chua mo cua tham gia nhoa");
                                log.writeLog();
                                message.reply(jsonReply);
                                return;
                            }
                            //trong thoi gian tham gia
                            else if (currentTime >= iron_man_promo_start_date && currentTime <= iron_man_promo_end_date)
                            {
                                checkTypeOfIronManSource(ironManPromoObj, log, message);
                            }
                            else
                            {
                                log.add("desc", "Eo biet vi sao vo day luon");
                                log.writeLog();
                                return;
                            }
                        }
                    }
                });
            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.IRON_MAN_PROMO_BUSS_ADDRESS, myHandler);
    }

    private void checkTypeOfIronManSource(final IronManPromoObj ironManPromoObj, final Common.BuildLog log, final Message<JsonObject> message) {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_RANDOM_GIFT;
//                                final ArrayList<IronManRandomGiftManageDb.Obj> ironRandomGift = new ArrayList();
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                if (jsonArray != null && jsonArray.size() > 0) {
                    JsonObject jo;
                    IronManRandomGiftManageDb.Obj randomGift = null;
                    for (Object o : jsonArray) {
                        jo = (JsonObject) o;
                        randomGift = new IronManRandomGiftManageDb.Obj(jo);
                        if (randomGift.group.equalsIgnoreCase(ironManPromoObj.source)) {
                            break;
                        }
//                                                ironRandomGift.add(randomGift);
                    }

                    log.add("func", "Chuong trinh iron man dang chay .... ngon");
                    //Kiem tra xem tra qua 1 phai khong
                    final JsonObject jsonReply = new JsonObject();
                    IronManPromoGiftDB.Obj obj = new IronManPromoGiftDB.Obj();
//                                            IronManRandomGiftManageDb.Obj randomGift_tmp = randomGift;
                    if (ironManPromoObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_4)) {
                        log.add("desc", "tra qua 4 cho thang be " + ironManPromoObj.phoneNumber);
                        doIronPromoForVoucherLater(message, jsonReply, ironManPromoObj, StringConstUtil.IronManPromo.IRON_PROMO_4, log);
                        return;
                    }
                    else if (randomGift == null) {
                        log.add("desc", "khong co thong tin random gift " + ironManPromoObj.phoneNumber);
                        log.writeLog();
                        message.reply(jsonReply);
                        return;
                    }
                    else if (ironManPromoObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_1)) {
                        log.add("desc", "tra qua 1 cho thang ku " + ironManPromoObj.phoneNumber);
                        final List<String> listVoucher = listVoucher(randomGift);
                        log.add("desc", "so luong tra qua 1 " + listVoucher.size());
                        if (listVoucher.size() > 0) {
                            doIronPromoForVoucher(message, jsonReply, ironManPromoObj, listVoucher, StringConstUtil.IronManPromo.IRON_PROMO_1, log);
                        }
                        log.writeLog();
                        return;

                    } else if (ironManPromoObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_2)) {
                        //Tra qua 2 cho thang ku
                        log.add("desc", "tra qua 2 cho thang ku " + ironManPromoObj.phoneNumber);
                        List<String> listVoucher = listVoucher(randomGift);
                        log.add("desc", "so luong tra qua 2 " + listVoucher.size());
                        doIronPromoForVoucher(message, jsonReply, ironManPromoObj, listVoucher, StringConstUtil.IronManPromo.IRON_PROMO_2, log);
                        log.writeLog();
                        return;
                    } else if (ironManPromoObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_3)) {
                        //Tra qua 3 cho thang ku
                        log.add("desc", "tra qua 3 cho thang ku " + ironManPromoObj.phoneNumber);
                        List<String> listVoucher = listVoucher(randomGift);
                        log.add("desc", "so luong tra qua 3 " + listVoucher.size());
                        doIronPromoForVoucher(message, jsonReply, ironManPromoObj, listVoucher, StringConstUtil.IronManPromo.IRON_PROMO_3, log);
                        log.writeLog();
                        return;
                    }
                    else {
                        //
                        log.add("desc", "chui vo ma khong thuoc the loai nao ca, khong ra gi roi " + ironManPromoObj.phoneNumber);
                        log.writeLog();
                        return;
                    }
                }//End If array random gift
                else {
                    log.add("desc", "chua cau hinh random gift " + ironManPromoObj.phoneNumber);
                    log.writeLog();
                    return;
                }
            }
        });
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void doIronPromoForVoucher(final Message<JsonObject> message
            , final JsonObject joReply
            , final IronManPromoObj reqObj
            , final List<String> listVoucher
            , final String group
            , final Common.BuildLog log) {
        ironManPromoGiftDB.findOne(reqObj.phoneNumber, new Handler<IronManPromoGiftDB.Obj>() {
            @Override
            public void handle(final IronManPromoGiftDB.Obj ironManPromoGiftDBObj) {

                // Trả khuyến mãi
                // Them thong tin service id va so voucher vao core
                ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                keyValues.add(new Misc.KeyValue("program", "ironmanpromo"));


                keyValues.add(new Misc.KeyValue("group", group));

                final String agentName = jsonIronmanPromo.getString("agent", "cskh");
                final int timeForGift_1 = jsonIronmanPromo.getInteger("timeforgift_1", 1);
                final int timeForGift_2 = jsonIronmanPromo.getInteger("timeforgift_2", 7);
                final int timeForGift_3 = jsonIronmanPromo.getInteger("timeforgift_3", 10);
                final long giftValue_2 = jsonIronmanPromo.getLong("valueofgift", 50000);
                final long giftValue_1 = jsonIronmanPromo.getLong("valueofgift_1", 10000);
                final int timeForActive = jsonIronmanPromo.getInteger("timeforactive", 20);
                //final String giftTypeId = reqObj.serviceId;
                final long giftValue = group.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_1) ? giftValue_1 : giftValue_2;

                final long totalGiftValue = listVoucher.size() * giftValue;

                log.add("TOTAL GIFT", listVoucher.size());
                log.add("TOTAL VALUE", totalGiftValue);

                //Chan vu tra thuong
                if (ironManPromoGiftDBObj != null && ironManPromoGiftDBObj.promo_count > 10) {
                    log.add("desc", "het duoc tra thuong nha cu");
                    log.writeLog();
                    message.reply(joReply);
                    return;
                } else if (ironManPromoGiftDBObj != null && ironManPromoGiftDBObj.has_voucher_group_2 && ironManPromoGiftDBObj.has_voucher_group_1 && ironManPromoGiftDBObj.has_voucher_group_3) {
                    log.add("desc", "het duoc tra thuong nha cu");
                    log.writeLog();
                    message.reply(joReply);
                    return;
                } else if (ironManPromoGiftDBObj != null && ironManPromoGiftDBObj.has_voucher_group_2 && reqObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_2)) {
                    log.add("desc", "het duoc tra thuong nha cu");
                    log.writeLog();
                    message.reply(joReply);
                    return;
                } else if (ironManPromoGiftDBObj != null && ironManPromoGiftDBObj.has_voucher_group_1 && reqObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_1)) {
                    log.add("desc", "het duoc tra thuong nha cu");
                    log.writeLog();
                    message.reply(joReply);
                    return;
                } else if (ironManPromoGiftDBObj != null && ironManPromoGiftDBObj.has_voucher_group_3 && reqObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_3)) {
                    log.add("desc", "het duoc tra thuong nha cu");
                    log.writeLog();
                    message.reply(joReply);
                    return;
                }


                int timeForGift = 0;
                if (group.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_1)) {
                    timeForGift = timeForGift_1;
                } else if (group.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_2)) {
                    timeForGift = timeForGift_2;
                } else if (group.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_3)) {
                    timeForGift = timeForGift_3;
                }
                //Tra thuong trong core
                final int endGiftTime = timeForGift;
                giftManager.adjustGiftValue(agentName
                        , reqObj.phoneNumber
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
                            final String note = reqObj.source;

                            //for (int i = 0; i < listVoucher.size(); i++) {
                            keyValues.clear();

                            kv.Key = "group";
                            kv.Value = reqObj.source + "_" + group;
                            keyValues.add(kv);
                            final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                            log.add("so luong voucher", atomicInteger);

                            vertx.setPeriodic(250L, new Handler<Long>() {
                                @Override
                                public void handle(Long aPeriodicLong) {
                                    if (atomicInteger.decrementAndGet() < 0) {
                                        log.add("func", "out of range for number " + reqObj.phoneNumber);
                                        log.writeLog();
                                        vertx.cancelTimer(aPeriodicLong);
                                        return;
                                    } else {
                                        final int itemPosition = atomicInteger.intValue();
                                        log.add("itemPosition", itemPosition);
                                        final boolean fireNoti = itemPosition == 0;
                                        giftType.setModelId(listVoucher.get(itemPosition).trim());
                                        final String serviceId = listVoucher.get(itemPosition).trim();
                                        giftManager.createLocalGiftForBillPayPromoWithDetailGift(reqObj.phoneNumber
                                                , giftValue
                                                , giftType
                                                , promotedTranId
                                                , agentName
                                                , modifyDate
                                                , endGiftTime
                                                , keyValues, note, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject jsonObject) {
                                                int err = jsonObject.getInteger("error", -1);
                                                final long tranId = jsonObject.getInteger("tranId", -1);
                                                final Gift gift = new Gift(jsonObject.getObject("gift"));
                                                final String giftId = gift.getModelId().trim();
                                                log.add("desc", "tra thuong chuong trinh ironman bang gift");
                                                log.add("err", err);

                                                //------------tat ca thanh cong roi
                                                if (err == 0) {
                                                    giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject jsonObject) {
                                                            gift.status = 3;
                                                            giveIronManVoucher(listVoucher, message, joReply, group, reqObj, gift, tranId, promotedTranId, giftValue, endGiftTime, log, fireNoti, itemPosition, ironManPromoGiftDBObj, giftId, serviceId);
                                                        }
                                                    });
                                                    return;
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

                            log.writeLog();
                            return;
                        }
                    }
                });
            }
        });
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    public void giveIronManVoucher(final List<String> listVoucher, final Message<JsonObject> message
            , final JsonObject joReply
            , final String group
            , final IronManPromoObj reqObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final Common.BuildLog log
            , final boolean fireNoti
            , final int itemPosition
            , final IronManPromoGiftDB.Obj ironPromoGiftDbObj
            , final String giftId
            , final String serviceId) {
//-------cap nhat mongo db ghi nhan da tra thuong
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

//        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
//            @Override
//            public void handle(final JsonArray objects) {
        final long activatedTime = System.currentTimeMillis();

        final JsonObject joUpdate = new JsonObject();

        if (group.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_1)) {
            if (ironPromoGiftDbObj != null) {
                log.add("desc", "da nhan qua 1");
                log.writeLog();
                message.reply(joReply);
                return;
            }
            joUpdate.putString(colName.IronManPromoGift.SERVICE_V1, "topup");
            joUpdate.putString(colName.IronManPromoGift.PHONE_NUMBER, reqObj.phoneNumber);
            joUpdate.putNumber(colName.IronManPromoGift.START_TIME_GROUP_1, System.currentTimeMillis());
            joUpdate.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_1, true);
            joUpdate.putNumber(colName.IronManPromoGift.END_TIME_GROUP_1, System.currentTimeMillis() +  1000L * 60 * 60 * 24 * timeForGift);
            joUpdate.putNumber(colName.IronManPromoGift.PROMO_COUNT, 1);
            joUpdate.putString(colName.IronManPromoGift.GIFT_ID_1, giftId);

            //Insert them
            joUpdate.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_1, 0);
            joUpdate.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_2, 0);
            joUpdate.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_3, 0);

            joUpdate.putString(colName.IronManPromoGift.CMND, "");
            joUpdate.putString(colName.IronManPromoGift.VISA_CARD, "");

            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_1, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_2, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_3, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_4, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_5, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_6, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_7, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_8, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_9, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_10, false);
            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_11, false);

        } else if (group.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_2) && ironPromoGiftDbObj != null && ironPromoGiftDbObj.used_voucher_1) {
            joUpdate.putNumber(colName.IronManPromoGift.START_TIME_GROUP_2, System.currentTimeMillis());
            joUpdate.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_2, true);
            joUpdate.putNumber(colName.IronManPromoGift.END_TIME_GROUP_2, System.currentTimeMillis() + 1000L * 60 * 60 * 24 * timeForGift);
            joUpdate.putNumber(colName.IronManPromoGift.PROMO_COUNT, ironPromoGiftDbObj.promo_count + 4);
            if (itemPosition == 0) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V2, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_2, giftId);
            } else if (itemPosition == 1) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V3, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_3, giftId);
            } else if (itemPosition == 2) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V4, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_4, giftId);
            } else if (itemPosition == 3) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V5, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_5, giftId);
            }
        } else if (group.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_3)) {

            log.add("desc", "Khong co thong tin card check sum va cmnd nao truoc do, tra thuong lan 3 cho em no");
            joUpdate.putNumber(colName.IronManPromoGift.START_TIME_GROUP_3, System.currentTimeMillis());
            joUpdate.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_3, true);
            joUpdate.putNumber(colName.IronManPromoGift.END_TIME_GROUP_3, System.currentTimeMillis() + 1000L * 60 * 60 * 24 * timeForGift);
            joUpdate.putNumber(colName.IronManPromoGift.PROMO_COUNT, ironPromoGiftDbObj.promo_count + 6);
            joUpdate.putString(colName.IronManPromoGift.CMND, reqObj.cmnd);
            joUpdate.putString(colName.IronManPromoGift.VISA_CARD, reqObj.cardCheckSum);

            if (itemPosition == 0) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V6, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_6, giftId);
            } else if (itemPosition == 1) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V7, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_7, giftId);
            } else if (itemPosition == 2) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V8, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_8, giftId);
            } else if (itemPosition == 3) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V9, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_9, giftId);
            } else if (itemPosition == 4) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V10, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_10, giftId);
            } else if (itemPosition == 5) {
                joUpdate.putString(colName.IronManPromoGift.SERVICE_V11, serviceId);
                joUpdate.putString(colName.IronManPromoGift.GIFT_ID_11, giftId);
            }
        }
        ironManPromoGiftDB.update(reqObj.phoneNumber, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (fireNoti) {
                    String giftMessage = "giftMessage";
                    String notiCaption = "notiCaption";
                    //String serviceName = "serviceName";
                    String tranComment = "tranComment";
                    String notiBody = "notiBody";

                    long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                    long endTime = activatedTime + timeforgift;
                    String date = Misc.dateVNFormatWithDot(endTime);
                    log.add("group", reqObj.source);
                    long value = giftValue;
                    if (reqObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_1)) {
                        giftMessage = PromoContentNotification.IRON_MAN_GIFT_RECEIVING_POP_UP_CONTENT;
                        tranComment = PromoContentNotification.IRON_MAN_GIFT_RECEIVING_POP_UP_CONTENT;
                        notiCaption = "Nhận thẻ quà tặng";
                        notiBody = PromoContentNotification.IRON_MAN_GIFT_RECEIVING_POP_UP_CONTENT;

                    }
                    else if(reqObj.source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_2)){
                        long endtime = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * timeForGift;
                        notiCaption = "Nhận thẻ quà tặng";
                        giftMessage = String.format(PromoContentNotification.IRON_MAN_GIFT_RECEIVING_POP_UP_200_CONTENT, Misc.dateVNFormat(endtime));
                        tranComment = String.format(PromoContentNotification.IRON_MAN_GIFT_RECEIVING_POP_UP_200_CONTENT, Misc.dateVNFormat(endtime));
                        notiBody = String.format(PromoContentNotification.IRON_MAN_GIFT_RECEIVING_POP_UP_200_CONTENT, Misc.dateVNFormat(endtime));
                        value = listVoucher.size() * giftValue;
                    }
                    else
                    {
                        notiCaption = "Nhận thẻ quà tặng";
                        giftMessage = PromoContentNotification.IRON_MAN_GIFT_SUCCESSFUL_MAPPING_CONTENT;
                        tranComment = PromoContentNotification.IRON_MAN_GIFT_SUCCESSFUL_MAPPING_CONTENT;
                        notiBody = PromoContentNotification.IRON_MAN_GIFT_SUCCESSFUL_MAPPING_CONTENT;
                        value = listVoucher.size() * giftValue;
                    }




//                    serviceName = ((JsonObject) objects.get(0)).getString("ser_name", reqObj.serviceId.toUpperCase()).toString();
//                    giftMessage = String.format(PromoContentNotification.BILL_PAY_PROMO_GIFT_MESSAGE_ONE, serviceName, date);
//                    tranComment = String.format(PromoContentNotification.BILL_PAY_PROMO_TRAN_COMMENT_ONE, serviceName, date);
//                    notiBody = String.format(PromoContentNotification.BILL_PAY_PROMO_DETAIL_NOTI_ONE, serviceName, date);


                    Misc.sendTranHisAndNotiBillPayGift(vertx
                            , DataUtil.strToInt(reqObj.phoneNumber)
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
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
            }
        });
//            }
//        });
    }

    private void saveInfoIronManDb(final int program_number, final IronManBonusTrackingTableDb.Obj ironManBonusTracking)
    {
        ironManNewRegisterTrackingDb.countUserStatus(program_number, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                if (jsonArray != null && jsonArray.size() > 0) {
                    boolean isBonus = false;
                    int count = 0;
                    int receivedGift = 0;
                    int notReceivedGift = 0;
                    for (Object o : jsonArray) {
                        isBonus = ((JsonObject) o).getBoolean("_id");
                        count = ((JsonObject) o).getInteger("count", 0);
                        if (isBonus) {
                            receivedGift = count;
                        }
                        else{
                            notReceivedGift = count;
                        }
                    }
                    JsonObject jsonBonusTracking = ironManBonusTracking.toJson();
                    jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER);
                    jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN);

                    jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, receivedGift + notReceivedGift);
                    jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, receivedGift);

                    ironManBonusTrackingTableDb.updatePartial(program_number, jsonBonusTracking, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    });
//                    final Common.ServiceReq serviceReq = new Common.ServiceReq();
//
//                    serviceReq.Command = Common.ServiceReq.COMMAND.UPDATE_IRON_MAN_USER;
//                    serviceReq.PackageId = jsonBonusTracking.toString();
//                    Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
//                        @Override
//                        public void handle(JsonArray jsonArray) {
//
//                        }
//                    });
                }
            }
        });
    }

    public ArrayList<String> listVoucher(IronManRandomGiftManageDb.Obj randomObj)
    {
        ArrayList<String> listVoucher = new ArrayList<>();
        if(!randomObj.fixed_gifts.equalsIgnoreCase(""))
        {
            String[] fixs = randomObj.fixed_gifts.split(",");
            if(fixs.length > 0)
            {
                for(int i = 0; i < fixs.length; i++)
                {
                    listVoucher.add(fixs[i].toString().trim());
                }
            }
        }

        //int receivedVoucher = listVoucher.size();
        if(!randomObj.random_gifts.equalsIgnoreCase("") && listVoucher.size() < randomObj.number_of_gift)
        {
            String[] randomGifts = randomObj.random_gifts.split(",");
            List<String> randomGifts_tmp = new ArrayList<String>(Arrays.asList(randomGifts));
            if(listVoucher.size() < randomObj.number_of_gift)
            {
                int numberOfRandomGift = 0;
                int mod = 0;
                int totalGiftRandom = randomObj.number_of_gift - listVoucher.size();
                for(int i = 0; i < totalGiftRandom; i++)
                {
                    numberOfRandomGift = randomGifts_tmp.size();
                    mod = UUID.randomUUID().hashCode() % numberOfRandomGift;
                    if(mod < 0)
                    {
                        mod = mod * -1;
                    }
                    listVoucher.add(randomGifts_tmp.get(mod));
                    randomGifts_tmp.remove(mod);
                }
            }
        }

        return listVoucher;
    }

    //END 0000000050


    //Quet voucher 1

    private void scanVoucherOneGroup(final Common.BuildLog log)
    {
        log.add("func", "scanVoucherOneGroup");

        long time = 12;
        final long next12hour = System.currentTimeMillis() + 12 * 60 * 60 * 1000L;
        final long next6hour = System.currentTimeMillis() + 6 * 60 * 60 * 1000L;
        final long next3hour = System.currentTimeMillis() + 3 * 60 * 60 * 1000L;
        JsonObject joFilter = new JsonObject();
        joFilter.putBoolean(colName.IronManPromoGift.USED_VOUCHER_1, false);

        ironManPromoGiftDB.findExpiredVoucherGroupOne(time, joFilter, new Handler<ArrayList<IronManPromoGiftDB.Obj>>() {
            @Override
            public void handle(ArrayList<IronManPromoGiftDB.Obj> ironManObjs) {
                log.add("size gift voucher 1", ironManObjs);
                if (ironManObjs.size() > 0) {
                    log.add("desc", "co vai be chua dung voucher 1 va sap het han");
                    boolean fireNoti = false;
                    Notification notification = null;
                    final ArrayList<IronManPromoGiftDB.Obj> firedNotiList = new ArrayList<IronManPromoGiftDB.Obj>();
                    for (IronManPromoGiftDB.Obj ironObj : ironManObjs) {
                        //Nhung dua chua dung voucher 1 va co time < 12h
                        fireNoti = false;
                        notification = new Notification();
                        if (!ironObj.used_voucher_1 && ironObj.end_time_group_1 <= next12hour && ironObj.number_of_noti_1 < 1
                                && !ironObj.has_voucher_group_4) {
                            log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 12h, ban noti");
                            fireNoti = true;
                            ironObj.number_of_noti_1 = 1;
                            firedNotiList.add(ironObj);
                            notification = createNotiGroupOne(ironObj.phone_number);
                        } else if (!ironObj.used_voucher_1 && ironObj.end_time_group_1 <= next6hour && ironObj.number_of_noti_1 < 2
                                && !ironObj.has_voucher_group_4) {
                            log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 6h, ban noti");
                            fireNoti = true;
                            ironObj.number_of_noti_1 = 2;
                            firedNotiList.add(ironObj);
                            notification = createNotiGroupOne(ironObj.phone_number);
                        } else if (!ironObj.used_voucher_1 && ironObj.end_time_group_1 <= next3hour && ironObj.number_of_noti_1 < 3
                                && !ironObj.has_voucher_group_4) {
                            log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 3h, ban noti");
                            fireNoti = true;
                            ironObj.number_of_noti_1 = 3;
                            firedNotiList.add(ironObj);
                            notification = createNotiGroupOne(ironObj.phone_number);
                        }
                        if (fireNoti) {
                            Misc.sendNoti(vertx, notification);
                        }
                    }

                    //Update database da ban noti
//                    vertx.setTimer(30 * 1000L, new Handler<Long>() {
//                        @Override
//                        public void handle(Long timerTime) {
                            final AtomicInteger listIronUser = new AtomicInteger(firedNotiList.size());

                            vertx.setPeriodic(50L, new Handler<Long>() {
                                @Override
                                public void handle(Long periTime) {
                                    int userPosition = listIronUser.decrementAndGet();
                                    if (userPosition < 0) {
                                        log.add("desc", "Da update het thong tin ban noti 1");
                                        vertx.cancelTimer(periTime);
                                        return;
                                    } else {
                                        JsonObject joUpdate = new JsonObject();
                                        joUpdate.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_1, firedNotiList.get(userPosition).number_of_noti_1);
                                        ironManPromoGiftDB.updatePartial(firedNotiList.get(userPosition).phone_number, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {

                                            }
                                        });
                                    }
                                }
                            });
//                        }
//                    });


                } else {
                    log.add("desc", "Chua co qua nao sap het han ca");
                }
                log.writeLog();
            }
        });
    }

    //Quet qua va ban noti lan 2
    private void scanVoucherTwoGroup(final Common.BuildLog log)
    {
        log.add("func", "scanVoucherTwoGroup");

        long time = 3;
        final long next3days = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L;
        final long next1days = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000L;

        JsonObject joFilter = new JsonObject();

        ironManPromoGiftDB.findExpiredVoucherGroupTwo(time, joFilter, new Handler<ArrayList<IronManPromoGiftDB.Obj>>() {
            @Override
            public void handle(ArrayList<IronManPromoGiftDB.Obj> ironManObjs) {
                log.add("size gift voucher group 2", ironManObjs);
                if (ironManObjs.size() > 0) {
                    log.add("desc", "co vai be chua dung voucher group 2 va sap het han");
                    boolean fireNoti = false;
                    Notification notification = null;
                    final ArrayList<IronManPromoGiftDB.Obj> firedNotiList = new ArrayList<IronManPromoGiftDB.Obj>();
                    List<String> listVoucher = null;
                    for (IronManPromoGiftDB.Obj ironObj : ironManObjs) {
                        //Nhung dua chua dung voucher 1 va co time < 12h
                        fireNoti = false;
                        notification = new Notification();
                        listVoucher = new ArrayList<String>();
                        if (!ironObj.used_voucher_2) {
                            listVoucher.add(ironObj.service_v2);
                        }
                        if (!ironObj.used_voucher_3) {
                            listVoucher.add(ironObj.service_v3);
                        }
                        if (!ironObj.used_voucher_4) {
                            listVoucher.add(ironObj.service_v4);
                        }
                        if (!ironObj.used_voucher_5) {
                            listVoucher.add(ironObj.service_v5);
                        }

                        if(listVoucher.size() > 0)
                        {
                            if (ironObj.end_time_group_2 <= next3days && ironObj.number_of_noti_2 < 1
                                    && !ironObj.has_voucher_group_4) {
                                log.add("desc", "Kiem tra so " + ironObj.phone_number);
                                log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 12h, ban noti");
                                fireNoti = true;
                                ironObj.number_of_noti_2 = 1;
                                firedNotiList.add(ironObj);
                                notification = createNotiGroupTwoThree(ironObj.phone_number, listVoucher, ironObj.number_of_noti_2);
                            } else if (ironObj.end_time_group_2 <= next1days && ironObj.number_of_noti_2 < 2
                                    && !ironObj.has_voucher_group_4) {
                                log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 6h, ban noti");
                                fireNoti = true;
                                ironObj.number_of_noti_2 = 2;
                                firedNotiList.add(ironObj);
                                notification = createNotiGroupTwoThree(ironObj.phone_number, listVoucher, ironObj.number_of_noti_2);
                            }
                        }
                        if (fireNoti) {
                            Misc.sendNoti(vertx, notification);
                        }
                    }

//                    //Update database da ban noti
//                    vertx.setTimer(30 * 1000L, new Handler<Long>() {
//                        @Override
//                        public void handle(Long timerTime) {
                            final AtomicInteger listIronUser = new AtomicInteger(firedNotiList.size());

                            vertx.setPeriodic(50L, new Handler<Long>() {
                                @Override
                                public void handle(Long periTime) {
                                    int userPosition = listIronUser.decrementAndGet();
                                    if (userPosition < 0) {
                                        log.add("desc", "Da update het thong tin ban noti 2");
                                        vertx.cancelTimer(periTime);
                                        return;
                                    } else {
                                        JsonObject joUpdate = new JsonObject();
                                        joUpdate.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_2, firedNotiList.get(userPosition).number_of_noti_2);
                                        ironManPromoGiftDB.updatePartial(firedNotiList.get(userPosition).phone_number, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {

                                            }
                                        });
                                    }
                                }
                            });
//                        }
//                    });
                } else {
                    log.add("desc", "Chua co qua nao sap het han ca");
                }
                log.writeLog();
            }
        });
    }

    //Quet qua iron man va ban noti lan 3
    //Quet qua va ban noti lan 2
    private void scanVoucherThreeGroup(final Common.BuildLog log)
    {
        log.add("func", "scanVoucherThreeGroup");

        long time = 3;
        final long next3days = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L;
        final long next1days = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000L;

        JsonObject joFilter = new JsonObject();

        ironManPromoGiftDB.findExpiredVoucherGroupThree(time, joFilter, new Handler<ArrayList<IronManPromoGiftDB.Obj>>() {
            @Override
            public void handle(ArrayList<IronManPromoGiftDB.Obj> ironManObjs) {
                log.add("size gift voucher group 3", ironManObjs);
                if (ironManObjs.size() > 0) {
                    log.add("desc", "co vai be chua dung voucher group 3 va sap het han");
                    boolean fireNoti = false;
                    Notification notification = null;
                    final ArrayList<IronManPromoGiftDB.Obj> firedNotiList = new ArrayList<IronManPromoGiftDB.Obj>();
                    List<String> listVoucher = null;
                    for (IronManPromoGiftDB.Obj ironObj : ironManObjs) {
                        //Nhung dua chua dung voucher 1 va co time < 12h
                        fireNoti = false;
                        notification = new Notification();
                        listVoucher = new ArrayList<String>();
                        if (!ironObj.used_voucher_6) {
                            listVoucher.add(ironObj.service_v6);
                        }
                        if (!ironObj.used_voucher_7) {
                            listVoucher.add(ironObj.service_v7);
                        }
                        if (!ironObj.used_voucher_8) {
                            listVoucher.add(ironObj.service_v8);
                        }
                        if (!ironObj.used_voucher_9) {
                            listVoucher.add(ironObj.service_v9);
                        }
                        if (!ironObj.used_voucher_10) {
                            listVoucher.add(ironObj.service_v10);
                        }
                        if (!ironObj.used_voucher_11) {
                            listVoucher.add(ironObj.service_v11);
                        }

                        if (listVoucher.size() > 0) {
                            if (ironObj.end_time_group_3 <= next3days && ironObj.number_of_noti_3 < 1
                                    && !ironObj.has_voucher_group_4) {
                                log.add("desc", "Kiem tra so " + ironObj.phone_number);
                                log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 12h, ban noti");
                                fireNoti = true;
                                ironObj.number_of_noti_3 = 1;
                                firedNotiList.add(ironObj);
                                notification = createNotiGroupTwoThree(ironObj.phone_number, listVoucher, ironObj.number_of_noti_3);
                            } else if (ironObj.end_time_group_3 <= next1days && ironObj.number_of_noti_3 < 2
                                    && !ironObj.has_voucher_group_4) {
                                log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 6h, ban noti");
                                fireNoti = true;
                                ironObj.number_of_noti_3 = 2;
                                firedNotiList.add(ironObj);
                                notification = createNotiGroupTwoThree(ironObj.phone_number, listVoucher, ironObj.number_of_noti_3);
                            }
                        }
                        if (fireNoti) {
                            Misc.sendNoti(vertx, notification);
                        }
                    }

                    //Update database da ban noti
//                    vertx.setTimer(30 * 1000L, new Handler<Long>() {
//                        @Override
//                        public void handle(Long timerTime) {
                            final AtomicInteger listIronUser = new AtomicInteger(firedNotiList.size());

                            vertx.setPeriodic(200L, new Handler<Long>() {
                                @Override
                                public void handle(Long periTime) {
                                    int userPosition = listIronUser.decrementAndGet();
                                    if (userPosition < 0) {
                                        log.add("desc", "Da update het thong tin ban noti 3");
                                        vertx.cancelTimer(periTime);
                                        return;
                                    } else {
                                        JsonObject joUpdate = new JsonObject();
                                        joUpdate.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_3, firedNotiList.get(userPosition).number_of_noti_3);
                                        ironManPromoGiftDB.updatePartial(firedNotiList.get(userPosition).phone_number, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {

                                            }
                                        });
                                    }
                                }
                            });
//                        }
//                    });
                } else {
                    log.add("desc", "Chua co qua nao sap het han ca");
                }
                log.writeLog();
            }
        });
    }


    //Quet qua iron man va ban noti lan 3
    //Quet qua va ban noti lan 2
    private void rollbackVoucherFourGroup(final Common.BuildLog log)
    {
        log.add("func", "scanVoucherFourGroup");
        final long value_of_gift = ironManPlus.getLong(StringConstUtil.IronManPromoPlus.VALUE_OF_GIFT, 10000);
        final String agent = ironManPlus.getString(StringConstUtil.IronManPromoPlus.AGENT, "ironman_promo");
        final int timeForGift = ironManPlus.getInteger(StringConstUtil.IronManPromoPlus.TIME_FOR_GIFT, 2);
        ironManPromoGiftDB.findExpiredVoucherGroupFour(new Handler<ArrayList<IronManPromoGiftDB.Obj>>() {
            @Override
            public void handle(final ArrayList<IronManPromoGiftDB.Obj> ironManObjs) {
                log.add("size gift voucher group 4", ironManObjs);
                if (ironManObjs.size() > 0) {
                    log.add("desc", "Co vai em chua xai tien hehe");

                    final AtomicInteger listIronUser = new AtomicInteger(ironManObjs.size());

                    vertx.setPeriodic(200L, new Handler<Long>() {
                        @Override
                        public void handle(Long periTime) {
                            final int userPosition = listIronUser.decrementAndGet();

                            if(userPosition < 0)
                            {
                                log.add("desc", "Het qui trinh");
                                vertx.cancelTimer(periTime);
                                log.writeLog();
                                return;
                            }
                            else{
                                final String phoneNumber = ironManObjs.get(userPosition).phone_number;
                                ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                keyValues.add(new Misc.KeyValue("program", "ironmanpromo_plus"));

                                keyValues.add(new Misc.KeyValue("group", "thu hoi"));
                                Misc.adjustment(vertx, phoneNumber, agent, value_of_gift, WalletType.MOMO, keyValues, log, new Handler<Common.SoapObjReply>() {
                                    @Override
                                    public void handle(Common.SoapObjReply soapObjReply) {
                                        if(soapObjReply.error == 0)
                                        {
                                            BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                                            msg.setType(SoapProto.Broadcast.MsgType.FORCE_UPDATE_AGENT_INFO_VALUE);
                                            msg.setSenderNumber(DataUtil.strToInt(phoneNumber));

                                            vertx.eventBus().publish(Misc.getNumberBus(DataUtil.strToInt(phoneNumber)), msg.getJsonObject());
                                            //Lay lai tien thanh cong
                                            log.add("desc", "thu hoi thanh cong roi nhe");
                                            //Ban noti thu hoi thanh cong.
                                            //Chia buon cung bac ....
                                            createTranHisRollBackGroupFour(ironManObjs.get(userPosition).phone_number, soapObjReply.tranId, soapObjReply.amount);
                                            Notification noti = createNotiRollBackGroupFour(ironManObjs.get(userPosition).phone_number);
                                            Misc.sendNoti(vertx, noti);


                                        }
                                        //Lay khong thanh cong
                                        JsonObject joUpdate = new JsonObject();
                                        joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_12, true);
                                        ironManPromoGiftDB.update(phoneNumber, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {

                                            }
                                        });
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

    private void scanVoucherFourGroup(final Common.BuildLog log)
    {
        log.add("func", "scanVoucherFourGroup");

        long time = 24;
        final long next24Hours = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
        final long next1Hours = System.currentTimeMillis() + 60 * 60 * 1000L;

        JsonObject joFilter = new JsonObject();

        ironManPromoGiftDB.findOneRemindedVoucherGroupFour(time, joFilter, new Handler<ArrayList<IronManPromoGiftDB.Obj>>() {
            @Override
            public void handle(ArrayList<IronManPromoGiftDB.Obj> ironManObjs) {
                log.add("size gift voucher group 4", ironManObjs);
                if (ironManObjs.size() > 0) {
                    log.add("desc", "co vai be chua dung voucher group 4 va sap het han");
                    boolean fireNoti = false;
                    Notification notification = null;
                    final ArrayList<IronManPromoGiftDB.Obj> firedNotiList = new ArrayList<IronManPromoGiftDB.Obj>();
                    for (IronManPromoGiftDB.Obj ironObj : ironManObjs) {
                        //Nhung dua chua dung voucher 1 va co time < 12h
                        notification = new Notification();

                        if (ironObj.end_time_group_4 <= next24Hours && ironObj.number_of_noti_4 < 1) {
                            log.add("desc", "Kiem tra so " + ironObj.phone_number);
                            log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 24h, ban noti");
                            fireNoti = true;
                            ironObj.number_of_noti_4 = 1;
                            firedNotiList.add(ironObj);
                            notification = createNotiGroupFour(ironObj.phone_number, PromoContentNotification.IRON_MAN_PLUS_24H_REMIND_TITLE, PromoContentNotification.IRON_MAN_PLUS_24H_REMIND_BODY);
                        } else if (ironObj.end_time_group_4 <= next1Hours && ironObj.number_of_noti_4 < 2) {
                            log.add("desc", "so " + ironObj.phone_number + " co voucher chua dung trong 1h, ban noti");
                            fireNoti = true;
                            ironObj.end_time_group_4 = 2;
                            firedNotiList.add(ironObj);
                            notification = createNotiGroupFour(ironObj.phone_number, PromoContentNotification.IRON_MAN_PLUS_1H_REMIND_TITLE, PromoContentNotification.IRON_MAN_PLUS_1H_REMIND_BODY);
                        }
                        if (fireNoti) {
                            Misc.sendNoti(vertx, notification);
                        }

                    }

                    //Update database da ban noti
//                    vertx.setTimer(30 * 1000L, new Handler<Long>() {
//                        @Override
//                        public void handle(Long timerTime) {
                    final AtomicInteger listIronUser = new AtomicInteger(firedNotiList.size());

                    vertx.setPeriodic(200L, new Handler<Long>() {
                        @Override
                        public void handle(Long periTime) {
                            int userPosition = listIronUser.decrementAndGet();
                            if (userPosition < 0) {
                                log.add("desc", "Da update het thong tin ban noti 3");
                                vertx.cancelTimer(periTime);
                                return;
                            } else {
                                JsonObject joUpdate = new JsonObject();
                                joUpdate.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_4, firedNotiList.get(userPosition).number_of_noti_4);
                                ironManPromoGiftDB.updatePartial(firedNotiList.get(userPosition).phone_number, joUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean event) {

                                    }
                                });
                            }
                        }
                    });
//                        }
//                    });
                } else {
                    log.add("desc", "Chua co qua nao sap het han ca");
                }
                log.writeLog();
            }
        });
    }


    private Notification createNotiGroupOne(String phoneNumber)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TOPUP_VALUE;
        noti.caption = PromoContentNotification.IRON_MAN_GROUP_VOUCHER_ONE_NOTIFICATION_CAPTION;
        noti.body = PromoContentNotification.IRON_MAN_GROUP_VOUCHER_ONE_NOTIFICATION_BODY;
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        return noti;
    }

    private Notification createNotiGroupTwoThree(String phoneNumber, List<String> listVoucher, int number_of_noti)
    {
        String caption = "";
        String body = "";
        String service = "";
        for(int i = 0; i < listVoucher.size(); i++)
        {
            if(i == 0)
            {
                service = service + mappingService.get(listVoucher.get(i).toString());
            }
            else if (i <= 4)
            {
                service = service + ", " + mappingService.get(listVoucher.get(i).toString());
            }
            else{
                break;
            }
        }
        if(number_of_noti == 1)
        {
            caption = PromoContentNotification.IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_CAPTION_THREE_DAY;
            body = String.format(PromoContentNotification.IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_BODY_THREE_DAY, service);
        }
        else {
            caption = PromoContentNotification.IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_CAPTION_ONE_DAY;
            body = String.format(PromoContentNotification.IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_BODY_ONE_DAY, service);
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

    private void scanVisaMapping(final Common.BuildLog log)
    {
        log.add("func", "scanVisaMapping");
        JsonObject jsonFilter = new JsonObject();

        jsonFilter.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_3, false);
        jsonFilter.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_2, true);

        ironManPromoGiftDB.searchWithFilter(jsonFilter, new Handler<ArrayList<IronManPromoGiftDB.Obj>>() {
            @Override
            public void handle(ArrayList<IronManPromoGiftDB.Obj> ironManPromoObjs) {
                if(ironManPromoObjs.size() > 0)
                {
                    for(int i = 0; i < ironManPromoObjs.size(); i++)
                    {
                        fireNotiScanMapping(ironManPromoObjs.get(i), log);
                    }
                }
            }
        });
        log.writeLog();
    }

    private void fireNotiScanMapping(IronManPromoGiftDB.Obj ironManPromoObj, Common.BuildLog log)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_LIXI_CREATE_VALUE;
        noti.caption = PromoContentNotification.IRON_MAN_GROUP_MAPPING_CAPTION;
        noti.body = PromoContentNotification.IRON_MAN_GROUP_MAPPING_BODY;
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(ironManPromoObj.phone_number);
        Misc.sendNoti(vertx, noti);
    }

    private void doIronPromoForVoucherLater(final Message message,final JsonObject jsonReply,final IronManPromoObj ironManPromoObj,final String program, final Common.BuildLog log)
    {
        final long value_of_gift = ironManPlus.getLong(StringConstUtil.IronManPromoPlus.VALUE_OF_GIFT, 10000);
        final String agent = ironManPlus.getString(StringConstUtil.IronManPromoPlus.AGENT, "ironman_promo");
        final int timeForGift = ironManPlus.getInteger(StringConstUtil.IronManPromoPlus.TIME_FOR_GIFT, 2);
        ironManPromoGiftDB.findOne(ironManPromoObj.phoneNumber, new Handler<IronManPromoGiftDB.Obj>() {
            @Override
            public void handle(final IronManPromoGiftDB.Obj ironManPromoGiftDBObj) {
                if(ironManPromoGiftDBObj == null)
                {
                    log.add("func", "doIronPromoForVoucherLater");
                    ArrayList<Misc.KeyValue> listKeyValues = new ArrayList<>();
                    listKeyValues.add(new Misc.KeyValue("program", "ironmanpromo_plus"));


                    listKeyValues.add(new Misc.KeyValue("group", program));
                    Misc.adjustment(vertx, agent, ironManPromoObj.phoneNumber, value_of_gift, WalletType.MOMO, listKeyValues, log, new Handler<Common.SoapObjReply>() {
                        @Override
                        public void handle(final Common.SoapObjReply result) {
                            if(result.error == 0)
                            {
                                //Core tra 10k thanh cong cho thang nay
                                final JsonObject joRep = new JsonObject();
                                joRep.putNumber("error", 0);
                                joRep.putString("desc", "success");
                                message.reply(joRep);
                                log.add("result", "success");
                                IronManPromoGiftDB.Obj obj = new IronManPromoGiftDB.Obj();
                                obj.has_voucher_group_4 = true;
                                obj.service_v12 = StringConstUtil.IronManPromo.IRON_PROMO_LATER;
                                obj.start_time_group_4 = System.currentTimeMillis();
                                obj.end_time_group_4 = System.currentTimeMillis() + timeForGift * 24 * 60 * 60 * 1000L;
                                obj.phone_number = ironManPromoObj.phoneNumber;
                                obj.promo_count = 1;
                                obj.number_of_noti_4 = 0;
                                ironManPromoGiftDB.insert(obj, new Handler<Integer>() {
                                    @Override
                                    public void handle(Integer insertResult) {
                                        if (insertResult == 0) {
//                                            createTranHisGroupFour(ironManPromoObj.phoneNumber, result.tranId, result.amount);
//                                            Notification noti = createNotiGroupFour(ironManPromoObj.phoneNumber, PromoContentNotification.IRON_MAN_PLUS_TITLE,
//                                                    PromoContentNotification.IRON_MAN_PLUS_BODY);
//                                            Misc.sendNoti(vertx, noti);
                                            log.add("group", ironManPromoObj.source);
                                            long value = value_of_gift;

                                            String notiCaption = PromoContentNotification.IRON_MAN_PLUS_TITLE;
                                            Gift gift = new Gift();
                                            gift.setModelId("topup");
                                            gift.status = 3;
                                            gift.typeId = "topup";
                                            String giftMessage = PromoContentNotification.IRON_MAN_PLUS_BODY;
                                            String tranComment = PromoContentNotification.IRON_MAN_PLUS_BODY;
                                            String notiBody = PromoContentNotification.IRON_MAN_PLUS_BODY;
                                            Misc.sendTranHisAndNotiBillPayGift(vertx
                                                    , DataUtil.strToInt(ironManPromoObj.phoneNumber)
                                                    , tranComment
                                                    , result.tranId
                                                    , value
                                                    , gift
                                                    , notiCaption
                                                    , notiBody
                                                    , giftMessage
                                                    , tranDb);
                                            //------------thong bao nhan qua neu la nhom 1
                                            log.add("insertResult", insertResult);

                                            log.writeLog();
                                            return;


                                        }
                                    }
                                });

                            }
                        }
                    });
                    log.writeLog();
                }
            }
        });

    }

    private Notification createNotiRollBackGroupFour(String phoneNumber)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.caption = PromoContentNotification.IRON_MAN_PLUS_ROLLBACK_TITLE;
        noti.body = PromoContentNotification.IRON_MAN_PLUS_ROLLBACK_BODY;
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        return noti;
    }

    private Notification createNotiGroupFour(String phoneNumber, String title, String body)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TOPUP_VALUE;
        noti.caption = title;
        noti.body = body;
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        return noti;
    }

    private void createTranHisGroupFour(final String phoneNumber, long tranId, long tranAmount)
    {
        String fullTranComment =  PromoContentNotification.IRON_MAN_PLUS_BODY; //+ sufixTranComment;
        JsonObject jsonHideLink = new JsonObject();
        final TranObj tran = new TranObj();
        long currentTime = System.currentTimeMillis();
        tran.tranType = MomoProto.TranHisV1.TranType.MPOINT_CLAIM_VALUE;
        tran.comment = fullTranComment;
        tran.tranId = tranId;
        tran.clientTime = currentTime;
        tran.ackTime = currentTime;
        tran.finishTime = currentTime;          //=> this must be the time we sync, or user will not sync this to device
        tran.partnerName = "MoMo"; //
        tran.partnerId = PromoContentNotification.IRON_MAN_PLUS_TITLE;
        tran.partnerRef = "";
        tran.amount = tranAmount;
        tran.status = TranObj.STATUS_OK;
        tran.error = 0;
        tran.cmdId = System.currentTimeMillis();
        tran.parterCode = "M_SERVICE";
        tran.owner_number = DataUtil.strToInt(phoneNumber);
        tran.io = 1;
//        jsonShare.putString("html", PromoContentNotification.HTMLSTR);
        jsonHideLink.putString("giftdetaillink", "false");
//        tran.share.addObject(jsonShare);
        tran.share.addObject(jsonHideLink);
        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                    //Ban noti va lich su giao dich cho so dien thoai do
            }
        });

    }

    private void createTranHisRollBackGroupFour(final String phoneNumber, long tranId, long tranAmount)
    {
        String fullTranComment =  PromoContentNotification.IRON_MAN_PLUS_ROLLBACK_BODY; //+ sufixTranComment;
        JsonObject jsonHideLink = new JsonObject();
        final TranObj tran = new TranObj();
        long currentTime = System.currentTimeMillis();
        tran.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
        tran.comment = fullTranComment;
        tran.tranId = tranId;
        tran.clientTime = currentTime;
        tran.ackTime = currentTime;
        tran.finishTime = currentTime;          //=> this must be the time we sync, or user will not sync this to device
        tran.partnerName = "MoMo"; //
        tran.partnerId = PromoContentNotification.IRON_MAN_PLUS_ROLLBACK_TITLE;
        tran.partnerRef = "";
        tran.amount = tranAmount;
        tran.status = TranObj.STATUS_OK;
        tran.error = 0;
        tran.cmdId = System.currentTimeMillis();
        tran.parterCode = "M_SERVICE";
        tran.owner_number = DataUtil.strToInt(phoneNumber);
        tran.io = -1;
//        jsonShare.putString("html", PromoContentNotification.HTMLSTR);
        jsonHideLink.putString("giftdetaillink", "false");
//        tran.share.addObject(jsonShare);
        tran.share.addObject(jsonHideLink);
        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                //Ban noti va lich su giao dich cho so dien thoai do
            }
        });
    }

}
