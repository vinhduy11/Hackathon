package com.mservice.momo.data.codeclaim;

import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.zalo.ZaloNotiObj;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.referral.ReferralProcess;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 2/23/16.
 */
public class ClaimCodePromotionVerticle extends Verticle {


    private final String TAG_MONEY = "%money";
    private final String TAG_GIFT_VALUE = "%giftvalue";
    //    private final String TAG_LAST_TID = "%lasttid";
//    private final String TAG_SERVICE = "%service";
    private final String TAG_END_GIFT_TIME = "%endgifttime";
    private final String TAG_END_MONEY_TIME = "%endmoneytime";
    long time20days = 1000L * 60 * 60 * 24 * 20;
    ArrayList<ZaloNotiObj> arrNotiList = null;
    ArrayList<Integer> time = new ArrayList<>();
    JsonArray claimCodePromotionJArr = new JsonArray();
    ClaimCodePromotionDb claimCodePromotionDb;
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
    private ControlOnClickActivityDb controlOnClickActivityDb;
    //Config for zalo promotion
    private ClaimCode_CodeCheckDb claimCode_codeCheckDb;
    private ClaimCode_PhoneCheckDb claimCode_phoneCheckDb;
    private ClaimCode_AllCheckDb claimCode_allCheckDb;
    private  ClaimCode_FecreditPromotionDb  claimCode_FecreditPromotionDb;
    private String url = "";
    private ReferralProcess referralProcess;
    ClaimCode_FecreditPromotionDb.Obj claimCodeFeCreditPromtionObj = new ClaimCode_FecreditPromotionDb.Obj();
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
       // jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());

        common = new Common(vertx, logger, container.config());

        controlOnClickActivityDb = new ControlOnClickActivityDb(vertx);
        claimCodePromotionDb = new ClaimCodePromotionDb(vertx, logger);
        claimCode_allCheckDb = new ClaimCode_AllCheckDb(vertx, logger);
        claimCode_codeCheckDb = new ClaimCode_CodeCheckDb(vertx, logger);
        claimCode_phoneCheckDb = new ClaimCode_PhoneCheckDb(vertx, logger);
        claimCode_FecreditPromotionDb = new ClaimCode_FecreditPromotionDb(vertx,logger);
        referralProcess = new ReferralProcess(vertx, logger, glbCfg);
        loadClaimCodePromotion();
       // url = jsonZaloPromo.getString(StringConstUtil.ZaloPromo.URL, "");
       // agent = jsonZaloPromo.getString(StringConstUtil.ZaloPromo.AGENT, "");
       // time_for_money = jsonZaloPromo.getInteger(StringConstUtil.ZaloPromo.TIME_FOR_MONEY, 0);
       // time_for_gift = jsonZaloPromo.getInteger(StringConstUtil.ZaloPromo.TIME_FOR_GIFT, 0);
       // value_of_money = jsonZaloPromo.getLong(StringConstUtil.ZaloPromo.VALUE_OF_MONEY, 0);
       // value_of_gift = jsonZaloPromo.getLong(StringConstUtil.ZaloPromo.VALUE_OF_GIFT, 0);
       // amount_bonus_maximum = jsonZaloPromo.getLong(StringConstUtil.ZaloPromo.AMOUNT_BONUS_MAXIMUM, 70000);
       // arrNotiList = new ArrayList<>();

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
        final AtomicInteger numberOfPromotion = new AtomicInteger(claimCodePromotionJArr.size());
        vertx.setTimer(timeDis, new Handler<Long>() {
            @Override
            public void handle(Long timerSet) {

                logger.info("CLAIMED CODE setTimer");
                vertx.setPeriodic(24 * 60 * 60 * 1000L, new Handler<Long>() { // 1 ngay quet lai 1 lan
                    //                vertx.setPeriodic(2 * 60 * 1000L, new Handler<Long>() {
                    @Override
                    public void handle(Long timePerDay) {
                        // Quet voucher 1
                        vertx.setPeriodic(1000L * 60 * 15, new Handler<Long>() {
                            @Override
                            public void handle(Long timePerEvent) {
                                if(numberOfPromotion.decrementAndGet() < 0)
                                {
                                    logger.info("DONE SCAN CLAIMED PROMOTION TIME EXPIRED");
                                    vertx.cancelTimer(timePerEvent);
                                }
                                else {
                                    int itemPosition = numberOfPromotion.intValue();
                                    logger.info("BEGIN TO SCAN CLAIMED PROMOTION TIME EXPIRED");
                                    Common.BuildLog log = new Common.BuildLog(logger);
                                    ClaimCodePromotionDb.Obj claimPromotionObj = new ClaimCodePromotionDb.Obj((JsonObject)claimCodePromotionJArr.get(itemPosition));
//                                    log.setPhoneNumber(claimPromotionObj.promotion_id);
//                                    log.add("getBackMoney", claimPromotionObj.getBackMoney);
                                    if(claimPromotionObj.getBackMoney && claimPromotionObj.activePromo)
                                    {
                                        scanExpiredLuckyMoney(log, claimPromotionObj);
                                    }
//                                    log.writeLog();
                                }
                            }
                        });
                    }
                });
            }
        });

        //chuong trinh nhap ma khuyen mai nhan qua
        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final ClaimCodePromotionObj claimCodePromotionObj = new ClaimCodePromotionObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(claimCodePromotionObj.phoneNumber);
                log.add("phoneNumber", claimCodePromotionObj.phoneNumber);
                log.add("code", claimCodePromotionObj.claimed_code);
                log.add("imei", claimCodePromotionObj.last_imei);
                log.add("source", claimCodePromotionObj.source);
                log.add("amount", claimCodePromotionObj.amount);
                log.add("serviceId", claimCodePromotionObj.serviceId);
                log.add("TID", claimCodePromotionObj.tid);
                log.add("promotion time / created date jupviec", claimCodePromotionObj.promotionTime);

                final JsonObject jsonReply = new JsonObject();
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                if("".equalsIgnoreCase(claimCodePromotionObj.claimed_code) && claimCodePromotionObj.tid > 0 && StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM.equalsIgnoreCase(claimCodePromotionObj.source)
                        && !"".equalsIgnoreCase(claimCodePromotionObj.phoneNumber))
                {
                    //UPDATE DATA su dung qua.
                    final AtomicInteger countPromo = new AtomicInteger(claimCodePromotionJArr.size());
                    vertx.setPeriodic(1000L, new Handler<Long>() {
                        @Override
                        public void handle(Long updateAllDbTime) {
                            if (countPromo.decrementAndGet() < 0) {
                                vertx.cancelTimer(updateAllDbTime);
                                log.add("desc", "DONE update");
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                            } else {
                                int itemPosition = countPromo.intValue();
                                final ClaimCodePromotionDb.Obj claimPromotionObj = new ClaimCodePromotionDb.Obj((JsonObject) claimCodePromotionJArr.get(itemPosition));
                                JsonObject joUpdate = new JsonObject();
                                joUpdate.putNumber(colName.ClaimCode_AllCheckCols.MONEY_STATUS, 1);
                                joUpdate.putNumber(colName.ClaimCode_AllCheckCols.TRANS_PAY_BILL_TID, claimCodePromotionObj.tid);
                                claimCode_allCheckDb.updatePartialViaPhone(claimCodePromotionObj.phoneNumber, claimPromotionObj.promotion_id, joUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean event) {
                                        logger.info("DONE UPDATE " + claimPromotionObj.promotion_id);
                                    }
                                });
                            }
                        }
                    });
                    return;
                }
                else if("".equalsIgnoreCase(claimCodePromotionObj.claimed_code) && claimCodePromotionObj.tid > 0
                        && !"".equalsIgnoreCase(claimCodePromotionObj.phoneNumber))
                {
                    //Thuc hien chuong trinh hau mai
                    log.add("desc", "executePostSaleService");
                    final ClaimCodePromotionDb.Obj claimCodeProgram = getProgramViaCode(claimCodePromotionObj.serviceId, log);
                    executePostSaleService(message, claimCodePromotionObj, claimCodeProgram, log);
                    return;
                }
                else if("".equalsIgnoreCase(claimCodePromotionObj.claimed_code))
                {
                    log.add("desc", "Khongn nhap code @_@");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }
                else if(isStoreApp)
                {
                    log.add("desc", "Diem giao dich khong duoc choi tro choi nay");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    //Cong Nguyen change BA 01/08/2016
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Chương trình khuyến mãi không áp dụng cho điểm giao dịch của MoMo. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }

                else if(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM.equalsIgnoreCase(claimCodePromotionObj.source))
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "claim code cho chuong trinh referral");
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "referralProcess");
                    referralProcess.checkReferralPromotion(message, log, claimCodePromotionObj, claimCodePromotionObj.joExtra);
                    return;
                }
                final ClaimCodePromotionDb.Obj claimCodeProgram = getProgramViaCode(claimCodePromotionObj.claimed_code, log);
                if(claimCodeProgram == null)
                {
                    log.add("desc", "sai code hoac la chuong trinh da off");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }
                else if(!claimCodeProgram.activePromo)
                {
                    log.add("desc", "chuong trinh claim code dang " + claimCodeProgram.activePromo + "");
                    log.writeLog();
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                    message.reply(jsonReply);
                    return;
                }

                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        boolean enableGiftGiving = false;
                        long promo_start_date = 0;
                        long promo_end_date = 0;
                        long currentTime = System.currentTimeMillis();
                        String agent = "";
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj claim_promo = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
                                claim_promo = new PromotionDb.Obj((JsonObject) o);
                                if (claim_promo.NAME.equalsIgnoreCase(claimCodeProgram.promotion_id)) {
                                    promo_start_date = claim_promo.DATE_FROM;
                                    promo_end_date = claim_promo.DATE_TO;
                                    agent = claim_promo.ADJUST_ACCOUNT;
                                    break;
                                }
                            }

                            //Check lan nua do dai chuoi ki tu
                            claimCodeProgram.agent = "".equalsIgnoreCase(claimCodeProgram.agent) ? agent : claimCodeProgram.agent;
                            final boolean enableGift = enableGiftGiving;
                            if("".equalsIgnoreCase(agent) && "".equalsIgnoreCase(claimCodeProgram.agent))
                            {
                                log.add("desc", "Thieu thong tin agent");
                                JsonObject jsonReply = new JsonObject();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION,  "Hệ thống đang bảo trì. Vui lòng thử lại sau 30 phút");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if (currentTime < promo_start_date || currentTime > promo_end_date) {
                                log.add("desc", "Chua bat dau chay chuong trinh " + claim_promo.NAME);
                                log.writeLog();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi đã hết thời hạn nhận thưởng. Hãy sử dụng MoMo để tham gia các chương trình khuyến mãi khác.");
                                message.reply(jsonReply);
                                return;
                            }
                            else if("jupviec".equalsIgnoreCase(claimCodePromotionObj.claimed_code) && claimCodePromotionObj.promotionTime < promo_start_date)
                            {
                                log.add("desc", "Tham gia jup viec nhung chua tao vi moi " + claim_promo.NAME);
                                log.writeLog();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Chương trình áp dụng cho khách hàng đăng ký ví MoMo từ 22/03 - 31/05/2016.");
                                message.reply(jsonReply);
                                return;
                            }
                            else if("fecredit".equalsIgnoreCase(claimCodePromotionObj.claimed_code)){
                                claimCode_FecreditPromotionDb.findOne(claimCodePromotionObj.phoneNumber, new Handler<ClaimCode_FecreditPromotionDb.Obj>()  {
                                    @Override
                                    public void handle(ClaimCode_FecreditPromotionDb.Obj phone_number) {
                                        if(phone_number != null && phone_number.phone_number.equals(claimCodePromotionObj.phoneNumber)){
                                            logger.info("Ban da nhan qua !!");
                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                            jsonReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã nhận khuyến mãi trên ví MoMo này. Chương trình chỉ áp dụng 1 lượt nhận quà khuyến mãi/ 1 ví. Cảm ơn bạn đã tham gia chương trình");
                                            message.reply(jsonReply);
                                        }else
                                        {
                                            List<String> listVoucher =  new ArrayList<String>(){{
                                                add("fecredit:100000");
                                                add("fecredit0:100000");
                                                add("fecredit1:100000");
                                            }};
                                              //String[] listGift = program.gift_list.split(";");
//                                            log.add("gift_list", program.gift_list);
//                                            log.add("list gift", listGift.length);
//                                            final List<String> listVoucher = getListVoucher(listGift);
//                                            final long giftMoney = getTotalMoney(listGift);
//                                            log.add("listVoucher", listVoucher.size());
//                                            log.add("gift money", giftMoney);
                                            giveVoucherForUser(message, 100000, claimCodeProgram, claimCodePromotionObj,listVoucher, log, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject event) {
                                                    claimCodeFeCreditPromtionObj.phone_number = claimCodePromotionObj.phoneNumber;
                                                    claimCodeFeCreditPromtionObj.div_no = claimCodePromotionObj.last_imei;
                                                    claimCodeFeCreditPromtionObj.promotion_name = claimCodePromotionObj.claimed_code;
                                                    claimCodeFeCreditPromtionObj.agent = claimCodeProgram.agent;
                                                    claimCodeFeCreditPromtionObj.prefix = claimCodeProgram.prefix;
                                                    claimCodeFeCreditPromtionObj.momo_money = 100000;
                                                    claimCodeFeCreditPromtionObj.gift_time = System.currentTimeMillis();
                                                    claimCode_FecreditPromotionDb.insert(claimCodeFeCreditPromtionObj, new Handler<Integer>() {
                                                        @Override
                                                        public void handle(Integer event) {

                                                        }
                                                    });
                                                    //todo sendNoti

                                                }
                                            });
                                        }

                                    }
                                });
                                return;
                            }
                            else if (claimCodePromotionObj.claimed_code.length() < 3) {
                                log.add("desc", "sai code");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                                log.writeLog();
                                message.reply(jsonReply);
                                return;
                            }

                            if (claimCodePromotionObj.phoneNumber.equalsIgnoreCase("") || DataUtil.strToLong(claimCodePromotionObj.phoneNumber) <= 0) {
                                log.add("desc", "So dien thoai la so dien thoai khong co that.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else if (!StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM.equalsIgnoreCase(claimCodePromotionObj.source)) {
                                log.add("desc", "Sai chuong trinh.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else {
                                log.add("desc", "Kiem tra code trong DB");
                                final AtomicInteger countInput = new AtomicInteger(0);
                                vertx.setTimer(1500L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        countInput.incrementAndGet();
                                        log.add("countInput", countInput);
                                        if(countInput.intValue() == 1)
                                        {
                                            //Kiem tra chuong trinh mot code
                                            if(claimCodeProgram.group == 1)
                                            {
                                                checkOneClaimedCodeIntoDb(claimCodePromotionObj, claimCodeProgram, log, jsonReply, message);
                                            }
                                            else {
                                                checkClaimedCodeIntoDb(claimCodePromotionObj, claimCodeProgram, log, jsonReply, message);
                                            }
                                        }
                                    }
                                });
                            }
                            //CreditPromotionVerticlecode here

                        }
                    }
                });
            }
        };

        Handler<Message<JsonObject>> loadHander = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final ClaimCodePromotionObj claimCodePromotionObj = new ClaimCodePromotionObj(reqJson);
                switch (claimCodePromotionObj.command)
                {
                    case 1:
                        message.reply(claimCodePromotionJArr);
                        break;
                    case 3:
                        reloadClaimCodePromotion(message);
                        break;
                    default:
                        break;
                }


            }
        };

        Handler<Message<JsonObject>> updateHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final ClaimCodePromotionObj claimCodePromotionObj = new ClaimCodePromotionObj(reqJson);
                switch (claimCodePromotionObj.command)
                {
                    case 2:
                        reloadClaimCodePromotion(message);
                        break;
                    default:
                        break;
                }
            }
        };

        vertx.eventBus().registerLocalHandler(AppConstant.CLAIMED_CODE_PROMOTION_BUSS_ADDRESS, myHandler);
        vertx.eventBus().registerLocalHandler(AppConstant.LOAD_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS, loadHander);
        vertx.eventBus().registerHandler(AppConstant.UPDATE_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS, updateHandler);
    }

    private void executePostSaleService(final Message<JsonObject> message, final ClaimCodePromotionObj claimCodePromotionObj, final ClaimCodePromotionDb.Obj claimCodeProgram, final Common.BuildLog log)
    {
        log.add("method", "executePostSaleService");
        if(StringConstUtil.ClaimCodePromotion.POST_SALE_JUPVIEC_SERVICE.equalsIgnoreCase(claimCodePromotionObj.source) && "jupviec".equalsIgnoreCase(claimCodePromotionObj.serviceId))
        {
            log.add("desc", "Hau mai giup viec ne");
            //Kiem tra xem co claim code hay khong
            JsonObject joFilter = new JsonObject();
            joFilter.putString(colName.ClaimCode_AllCheckCols.PHONE, claimCodePromotionObj.phoneNumber);
            claimCode_allCheckDb.searchWithFilter(claimCodeProgram.promotion_id, joFilter, new Handler<ArrayList<ClaimCode_AllCheckDb.Obj>>() {
                @Override
                public void handle(ArrayList<ClaimCode_AllCheckDb.Obj> claimJupViecCodeObjs) {
                        if(claimJupViecCodeObjs.size() == 0 || claimJupViecCodeObjs.size() > 1)
                        {
                            log.add("desc", "thanh toan giup viec nhung chua claim code");
                            log.writeLog();
                            return;
                        }

                        //Neu da claim thi kiem tra xem da nhan khuyen mai chua
                        JsonObject joExtra = claimJupViecCodeObjs.get(0).joExtra;
                        if(joExtra.getFieldNames().size() != 0)
                        {
                            //Da co khuyen mai, khong tra khuyen mai
                            log.add("desc", "Da tra khuyen mai giup viec roi nhe");
                            log.writeLog();
                            return;
                        }
                        //Chua tra gi, tra cho em no
                    //program.gift_list => aaa:50000; bbb:10000
                    String[] listGift = claimCodeProgram.gift_list.split(";");
                    log.add("gift_list", claimCodeProgram.gift_list);
                    log.add("list gift", listGift.length);
                    final List<String> listVoucher = getListVoucher(listGift);
                    final long giftMoney = getTotalMoney(listGift);
                    log.add("listVoucher", listVoucher.size());
                    givePostSaleServiceVoucherForJupViecUser(message, giftMoney, claimCodeProgram, claimCodePromotionObj, listVoucher, log);
                }
            });
            return;
        }


    }

    private void checkClaimedCodeIntoDb(final ClaimCodePromotionObj claimCodePromotionObj, final ClaimCodePromotionDb.Obj claimCodeProgram, final Common.BuildLog log, final JsonObject jsonReply, final Message<JsonObject> message) {
        //Kiem tra xem code co ton tai ko
        claimCode_codeCheckDb.findOne(claimCodePromotionObj.claimed_code.toUpperCase(), claimCodeProgram.promotion_id, new Handler<ClaimCode_CodeCheckDb.Obj>() {
            @Override
            public void handle(ClaimCode_CodeCheckDb.Obj claimedCode) {
                if (claimedCode == null && claimCodeProgram.isMoMoPromotion) {
                    //Khong ton tai code, tra loi
                    log.add("desc", "Sai ma code, ma code khong ton tai trong " + claimCodeProgram.promotion_id);
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString("desc", claimCodeProgram.notiRollbackBody);
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
                else if (claimedCode == null) {
                    //Khong ton tai code, tra loi
                    log.add("desc", "Sai ma code, ma code khong ton tai trong " + claimCodeProgram.promotion_id);
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString("desc", "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
                else if(!claimedCode.enabled)
                {
                    //Khong ton tai code, tra loi
                    log.add("desc", "Mã code chưa được kích hoạt " + claimCodeProgram.promotion_id);
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString("desc", "Mã code chưa được kích hoạt");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
                // Ton tai code, tra thuong cho user
                //Tra thuong
                //Kiem tra xem so nay da claim code chua
                claimCode_allCheckDb.findClaimedCodeUserInfo(claimCodeProgram.promotion_id, claimCodePromotionObj.phoneNumber
                        , claimCodePromotionObj.last_imei, claimCodePromotionObj.claimed_code, new Handler<ArrayList<ClaimCode_AllCheckDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<ClaimCode_AllCheckDb.Obj> listClaimCode) {
                        if (listClaimCode.size() > 0) {
                            //Khong duoc tra thuong nhe
                            //Tim xem tai sao lai co du lieu claim code
                            log.add("desc", "So dien thoai hoac imei da nhan thuong.");
                            int result = checkClaimCodeRule(listClaimCode, claimCodePromotionObj, claimCodeProgram);
                            if(result == 1  && !claimCodeProgram.uncheckDevice)
                            {
                                //DT
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã nhận khuyến mãi trên ví MoMo này. Chương trình chỉ áp dụng 1 lượt nhận quà khuyến mãi/ 1 ví. Cảm ơn bạn đã tham gia chương trình");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if (result == 2 && !claimCodeProgram.uncheckDevice)
                            {
                                //DEVICE
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã nhận khuyến mãi trên thiết bị này. Chương trình chỉ áp dụng 1 lượt nhận quà khuyến mãi/ 1 thiết bị di động. Cảm ơn bạn đã tham gia chương trình");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if(!claimCodeProgram.isMoMoPromotion)
                            {
                                //MA CODE
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi này đã được sử dụng,  vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if(claimCodeProgram.isMoMoPromotion && result == 3)
                            {
                                //MA CODE
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, claimCodeProgram.notiRollbackBody);
                                message.reply(jsonReply);
                                log.writeLog();
                                return;

                            }
                        }
                        //Tra thuong cho no
                        //Kiem tra so dien thoai
                        if (claimCodeProgram.check_phone) {
                            claimCode_phoneCheckDb.findOne(claimCodePromotionObj.phoneNumber, claimCodeProgram.promotion_id, new Handler<ClaimCode_PhoneCheckDb.Obj>() {
                                @Override
                                public void handle(ClaimCode_PhoneCheckDb.Obj phoneCode) {
                                    if (phoneCode == null) {
                                        //So nay khong duoc tra thuong
                                        log.add("desc", "So dien thoai hoac imei da nhan thuong.");
                                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                        jsonReply.putString(StringConstUtil.DESCRIPTION, "So dien thoai nay khong duoc tham gia chuong trinh nay");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }
                                    //Tra thuong cho em no
                                    log.add("desc", "tra khuyen mai claim code");
                                    processGiveMoneyAndGiftForUser(message, claimCodeProgram, claimCodePromotionObj, log);
                                }
                            });
                            return;
                        }
                        //Tra thuong cho em no
                        log.add("desc", "tra khuyen mai claim code");
                        processGiveMoneyAndGiftForUser(message, claimCodeProgram, claimCodePromotionObj, log);
                    }
                });
            }
        });
    }

    /**
     * Chương trình nhập code với duy nhất 1 code cho toàn bộ promtion
     *
     * @param claimCodePromotionObj
     * @param claimCodeProgram
     * @param log
     * @param jsonReply
     * @param message
     */
    private void checkOneClaimedCodeIntoDb(final ClaimCodePromotionObj claimCodePromotionObj, final ClaimCodePromotionDb.Obj claimCodeProgram, final Common.BuildLog log, final JsonObject jsonReply, final Message<JsonObject> message) {
        //Kiem tra xem code co ton tai ko
        final String code = claimCodePromotionObj.claimed_code + "_" + claimCodePromotionObj.phoneNumber;
        // Ton tai code, tra thuong cho user
        //Tra thuong
        //Kiem tra xem so nay da claim code chua
        claimCode_allCheckDb.findClaimedCodeUserInfo(claimCodeProgram.promotion_id, claimCodePromotionObj.phoneNumber
                , claimCodePromotionObj.last_imei, code, new Handler<ArrayList<ClaimCode_AllCheckDb.Obj>>() {
            @Override
            public void handle(ArrayList<ClaimCode_AllCheckDb.Obj> listClaimCode) {
                if (listClaimCode.size() > 0) {
                    //Khong duoc tra thuong nhe
                    //Tim xem tai sao lai co du lieu claim code
                    log.add("desc", "So dien thoai hoac imei da nhan thuong.");
                    int result = checkClaimCodeRule(listClaimCode, claimCodePromotionObj, claimCodeProgram);
                    if (result == 1) {
                        //DT
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã nhận khuyến mãi trên ví MoMo này. Chương trình chỉ áp dụng 1 lượt nhận quà khuyến mãi/ 1 ví. Cảm ơn bạn đã tham gia chương trình");
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    } else if (result == 2) {
                        //DEVICE
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã nhận khuyến mãi trên thiết bị này. Chương trình chỉ áp dụng 1 lượt nhận quà khuyến mãi/ 1 thiết bị di động. Cảm ơn bạn đã tham gia chương trình");
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    } else {
                        //MA CODE
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi này đã được sử dụng,  vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    }
                }
                //Tra thuong cho no
                //Kiem tra so dien thoai
                if (claimCodeProgram.check_phone) {
                    claimCode_phoneCheckDb.findOne(claimCodePromotionObj.phoneNumber, claimCodeProgram.promotion_id, new Handler<ClaimCode_PhoneCheckDb.Obj>() {
                        @Override
                        public void handle(ClaimCode_PhoneCheckDb.Obj phoneCode) {
                            if (phoneCode == null) {
                                //So nay khong duoc tra thuong
                                log.add("desc", "So dien thoai hoac imei da nhan thuong.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "So dien thoai nay khong duoc tham gia chuong trinh nay");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            //Tra thuong cho em no
                            log.add("desc", "tra khuyen mai claim code");
                            processGiveMoneyAndGiftForUser(message, claimCodeProgram, claimCodePromotionObj, log);
                        }
                    });
                    return;
                }
                //Tra thuong cho em no
                log.add("desc", "tra khuyen mai claim code");
                processGiveMoneyAndGiftForUser(message, claimCodeProgram, claimCodePromotionObj, log);
            }
        });
    }

    private ClaimCodePromotionDb.Obj getProgramViaCode(String code, Common.BuildLog log)
    {
        //Kiem tra xem code co ton tai ko, neu khong thi cat du lieu va tim prefix
        logger.info("aaaaaaaaaaaaaaaaaaaaaaaa"+code);
        String prefix_tmp =  code.length() > 2 ? code.substring(0, 2) : "xxx";
        log.add("prefix_tmp", prefix_tmp);
        log.add("size claim code promotion", claimCodePromotionJArr.size());
        ClaimCodePromotionDb.Obj claimObj = null;
        int flag = 0;
        for(int i = 0; i < claimCodePromotionJArr.size(); i++)
        {
            claimObj = new ClaimCodePromotionDb.Obj(((JsonObject)claimCodePromotionJArr.get(i)));
            //Nhom su dung 1 code
            if(code.trim().equalsIgnoreCase(claimObj.prefix) && claimObj.group == 1)
            {
                flag = 1;
                log.add("data ", claimObj.prefix);
                break;
            }
            //Nhom su dung nhieu code
            else if(prefix_tmp.equalsIgnoreCase(claimObj.prefix) && claimObj.group == 2)
            {
                flag = 1;
                log.add("data ", claimObj.prefix);
                break;
            }
        }
        claimObj = flag == 1 ? claimObj : null;
     // log.add("promotion Id", claimObj.promotion_id);
        return claimObj;
    }

    /**
     * Kiểm tra điều kiện nhận khuyến mãi của user
     *
     * @param listClaimCode
     * @param claimCodePromotionObj
     * @return
     */
    private int checkClaimCodeRule(ArrayList<ClaimCode_AllCheckDb.Obj> listClaimCode, ClaimCodePromotionObj claimCodePromotionObj, final ClaimCodePromotionDb.Obj claimCodeProgram)
    {
        int result = 0;

        for(int i = 0; i < listClaimCode.size(); i ++)
        {
            if(claimCodePromotionObj.phoneNumber.equalsIgnoreCase(listClaimCode.get(i).phone) && !claimCodeProgram.uncheckDevice)
            {
                result = 1;
                break;
            }
            else if(claimCodePromotionObj.last_imei.equalsIgnoreCase(listClaimCode.get(i).device_imei) && !claimCodeProgram.uncheckDevice)
            {
                result = 2;
                break;
            }
            else if(claimCodePromotionObj.claimed_code.equalsIgnoreCase(listClaimCode.get(i).code))
            {
                result = 3;
                break;
            }
        }

        return result;
    }

    private void processGiveMoneyAndGiftForUser(final Message message,final ClaimCodePromotionDb.Obj program, final ClaimCodePromotionObj claimCodePromotionObj,final Common.BuildLog log)
    {
        giveMoneyToUser(claimCodePromotionObj, program, log, message, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joResponse) {
                int err = joResponse.getInteger(StringConstUtil.ERROR, -1000);
                final long soapTid = joResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                if(err != 0)
                {
                    //Tra tien khong thanh cong, khong tra qua luon
                    log.add("desc", "tra tien khong thanh cong cho tid " + soapTid);
                    message.reply(joResponse);
                    return;
                }
                else {
                    //program.gift_list => aaa:50000; bbb:10000
                    String[] listGift = program.gift_list.split(";");
                    log.add("gift_list", program.gift_list);
                    log.add("list gift", listGift.length);
                    final List<String> listVoucher = getListVoucher(listGift);
                    final long giftMoney = getTotalMoney(listGift);
                    log.add("listVoucher", listVoucher.size());
                    log.add("gift money", giftMoney);
                    saveInfo(message, listVoucher.size(), claimCodePromotionObj, program, log, soapTid, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject response) {
                                int error = response.getInteger(StringConstUtil.ERROR, -1000);
                                if (error == 0 && giftMoney > 0) {
                                    log.add("desc", "Tra tien thanh cong, Tra qua");
                                    giveVoucherForUser(message, giftMoney, program, claimCodePromotionObj, listVoucher, log, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject event) {
                                            //todo sendNoti
                                        }
                                    });
                                }
                                else if(error == 0 && giftMoney == 0)
                                {
                                    Gift gift = new Gift();
                                    gift.setModelId(program.serviceId);
                                    gift.status = 3;
                                    gift.typeId = program.serviceId;
                                    sendNotiAndTranHis(program.momo_money, program, claimCodePromotionObj, soapTid, gift);
                                    //callback from here to notify back to caller
                                    message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                                    return;
                                }
                                else {
                                    JsonObject jsonReply = new JsonObject();
                                    log.add("desc", "So dien thoai hoac imei da nhan thuong.");
                                    jsonReply.putNumber(StringConstUtil.ERROR, -3000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Hệ thống đang bảo trì. Vui lòng thử lại sau 30 phút");
                                    message.reply(jsonReply);
                                    log.writeLog();
                                    return;
                                }
                                //message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                            }
                        });
                        return;

                }
            }
        });
    }

    private List<String> getListVoucher(String[] listGift)
    {
        List<String> listVoucher = new ArrayList<>();
        String []gift = {};
        for(String giftInfo : listGift)
        {
            gift = giftInfo.trim().split(":");
            if(gift.length == 2)
            {
                listVoucher.add(giftInfo);
            }
        }

        return listVoucher;

    }

    private long getTotalMoney(String[] listGift)
    {
        long money = 0;
        String []gift = {};
        for(String giftInfo : listGift)
        {
            gift = giftInfo.trim().split(":");
            if(gift.length == 2)
            {

                money = money + Long.parseLong(gift[1].trim());
            }
        }
        return money;
    }
    private void giveMoneyToUser(final ClaimCodePromotionObj claimCodePromotionObj, ClaimCodePromotionDb.Obj program, final Common.BuildLog log, final Message message, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.add("desc", "giveMoneyToUser");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = program.promotion_id;
        if(program.momo_money <= 0)
        {
            log.add("desc", "Khong tra tien cho EU");
            jsonReply.putNumber(StringConstUtil.ERROR, 0);
            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, 0);
            callback.handle(jsonReply);
            return;
        }
        else {
            Misc.adjustment(vertx, program.agent, claimCodePromotionObj.phoneNumber, program.momo_money,
                    Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                        @Override
                        public void handle(Common.SoapObjReply soapObjReply) {
                            log.add("desc", "core tra ket qua");
                            if (soapObjReply != null && soapObjReply.error != 0) {
                                log.add("core tra loi ", soapObjReply.error);
                                log.add("status ", soapObjReply.status);
                                log.add("tid", soapObjReply.tranId);
                                log.add("desc", "core tra loi");
//                                log.writeLog();
                                JsonObject jsonReply = new JsonObject();
                                jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                                jsonReply.putString(StringConstUtil.DESCRIPTION,  "Hệ thống đang bảo trì. Vui lòng thử lại sau 30 phút");
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
    }

    private void saveInfo(final Message message, final int numberVoucher, final ClaimCodePromotionObj claimCodePromotionObj, ClaimCodePromotionDb.Obj program, final Common.BuildLog log, final long tranId, final Handler<JsonObject> callback)
    {
        log.add("desc", "giveMoney");
        ClaimCode_AllCheckDb.Obj claimCodeAllCheckObj = new ClaimCode_AllCheckDb.Obj();

        claimCodeAllCheckObj.device_imei = claimCodePromotionObj.last_imei;
        if(program.group == 1)
        {
            claimCodeAllCheckObj.code = claimCodePromotionObj.claimed_code + "_" + claimCodePromotionObj.phoneNumber;
        }
        else {
            claimCodeAllCheckObj.code = claimCodePromotionObj.claimed_code;
        }
        claimCodeAllCheckObj.money_amount = program.momo_money;
        claimCodeAllCheckObj.phone = claimCodePromotionObj.phoneNumber;
        claimCodeAllCheckObj.money_time = System.currentTimeMillis() + program.money_time * 1000L * 60 * 60 * 24;
        claimCodeAllCheckObj.money_tid = tranId;
        claimCodeAllCheckObj.time = System.currentTimeMillis();
        claimCodeAllCheckObj.money_status = 0;

        claimCode_allCheckDb.insert(numberVoucher, program.promotion_id, claimCodeAllCheckObj, new Handler<Integer>() {
            @Override
            public void handle(Integer result) {
                callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, result));
            }
        } );
    }


    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void giveVoucherForUser(final Message<JsonObject> message
            , final long totalGiftValue
            , final ClaimCodePromotionDb.Obj program
            , final ClaimCodePromotionObj claimCodePromotionObj
            , final List<String> listVoucher
            , final Common.BuildLog log, final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        // Them thong tin service id va so voucher vao core
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", program.promotion_id));
        keyValues.add(new Misc.KeyValue("group", claimCodePromotionObj.source));
        final JsonObject joReply = new JsonObject();
        //final String giftTypeId = reqObj.serviceId;
        log.add("TOTAL GIFT", listVoucher.size());
        log.add("TOTAL VALUE", totalGiftValue);

        int timeForGift = program.gift_time;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;

        giftManager.adjustGiftValue(program.agent
                , claimCodePromotionObj.phoneNumber
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
                    final String note = claimCodePromotionObj.source + program.promotion_id;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = claimCodePromotionObj.source;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                    log.add("so luong voucher", atomicInteger);

                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {
                            if (atomicInteger.decrementAndGet() < 0) {
                                log.add("func", "out of range for number " + claimCodePromotionObj.phoneNumber);
                                log.writeLog();
                                vertx.cancelTimer(aPeriodicLong);
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
//                                return;
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                log.add("itemPosition", itemPosition);
                                final boolean fireNoti = itemPosition == 0;
                                String giftModelId = listVoucher.get(itemPosition).trim().split(":")[0].trim();
                                long giftValueFinal = DataUtil.strToLong(listVoucher.get(itemPosition).trim().split(":")[1].trim());
                                giftType.setModelId(giftModelId);
//
                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(claimCodePromotionObj.phoneNumber
                                        , giftValueFinal
                                        , giftType
                                        , promotedTranId
                                        , program.agent
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
                                            giftManager.useGift(claimCodePromotionObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {
                                                    gift.status = 3;
                                                    updateVoucherInfoForUser(giftId, program, totalGiftValue, claimCodePromotionObj, log, tranId, itemPosition, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean event) {
                                                            if(itemPosition == 0)
                                                            {
                                                                sendNotiAndTranHis(totalGiftValue + program.momo_money, program, claimCodePromotionObj, tranId, gift);
                                                                //callback from here to notify back to caller
                                                                callback.handle(new JsonObject());
                                                            }
                                                        }
                                                    });
                                                }
                                            });
//                                            return;
                                        } else {
                                            log.add("error", -1000);
                                            log.add("desc", "Lỗi " + SoapError.getDesc(error));
                                            joReply.putNumber("error", 1000);
                                            joReply.putString(StringConstUtil.DESCRIPTION, "Hệ thống đang bảo trì. Vui lòng thử lại sau 30 phút");
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

    private void updateVoucherInfoForUser(final String giftId, ClaimCodePromotionDb.Obj program, long totalGiftAmount, final ClaimCodePromotionObj claimCodePromotionObj, final Common.BuildLog log, final long tranId, final int count, final Handler<Boolean> callback)
    {
        JsonObject joUpdate = new JsonObject();
        int numCol = count + 1;
        String giftCol = colName.ClaimCode_AllCheckCols.GIFT_ID + "_" + numCol;
        log.add("giftCol", giftCol);
        log.add("numCol", numCol);
        if(count == 0)
        {
            joUpdate.putString(giftCol, giftId);
            joUpdate.putNumber(colName.ClaimCode_AllCheckCols.GIFT_TID, tranId);
            joUpdate.putNumber(colName.ClaimCode_AllCheckCols.GIFT_TIME, System.currentTimeMillis());
            joUpdate.putNumber(colName.ClaimCode_AllCheckCols.GIFT_AMOUNT, totalGiftAmount);
        }
        else {
            joUpdate.putString(giftCol, giftId);
        }
        String code = "";
        if(program.group == 1)
        {
            code = claimCodePromotionObj.claimed_code + "_" + claimCodePromotionObj.phoneNumber;
        }
        else {
            code = claimCodePromotionObj.claimed_code;
        }

        claimCode_allCheckDb.updatePartial(code, program.promotion_id, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(result);
            }
        });
    }

    private void sendNotiAndTranHisForJupViec(long totalGiftValue, ClaimCodePromotionDb.Obj program, ClaimCodePromotionObj claimCodePromotionObj, long tranId, Gift gift) {
        String giftendtime = Misc.dateVNFormatWithDot(System.currentTimeMillis() + program.gift_time * 1000L * 60 * 60 * 24);
        String notiCaption = "<Dọn nhà hôm nay – Nhận ngay quà tặng>";

        String giftMessage = String.format("Tặng bạn ưu đãi 50.000đ áp dụng giảm giá khi thanh toán Jupviec trên ứng dụng MoMo. Ưu đãi áp dụng đến %s.", giftendtime);
        String tranComment = String.format("Tặng bạn ưu đãi 50.000đ áp dụng giảm giá khi thanh toán Jupviec trên ứng dụng MoMo. Ưu đãi áp dụng đến %s.", giftendtime);
        String notiBody = String.format("Tặng bạn ưu đãi 50.000đ áp dụng giảm giá khi thanh toán Jupviec trên ứng dụng MoMo. Ưu đãi áp dụng đến %s.", giftendtime);
        String partnerName = program.partnerName;
        String serviceId = program.serviceId;

        Misc.sendTranHisAndNotiZaloMoney(vertx
                , DataUtil.strToInt(claimCodePromotionObj.phoneNumber)
                , tranComment
                , tranId
                , totalGiftValue
                , gift
                , notiCaption
                , notiBody
                , giftMessage
                , partnerName
                , serviceId
                , tranDb);
    }
    private void sendNotiAndTranHis(long totalGiftValue, ClaimCodePromotionDb.Obj program, ClaimCodePromotionObj claimCodePromotionObj, long tranId, Gift gift) {
        String giftendtime = Misc.dateVNFormatWithDot(System.currentTimeMillis() + program.gift_time * 1000L * 60 * 60 * 24);
        String moneyendtime = Misc.dateVNFormatWithDot(System.currentTimeMillis() + program.money_time * 1000L * 60 * 60 * 24);
        String notiCaption = program.notiTitle;
        String giftMessage = program.transBody.replaceAll(TAG_END_GIFT_TIME, giftendtime).replaceAll(TAG_END_MONEY_TIME, moneyendtime)
                .replaceAll(TAG_MONEY, program.momo_money + "").replaceAll(TAG_GIFT_VALUE, totalGiftValue + "");
        String tranComment = program.transBody.replaceAll(TAG_END_GIFT_TIME, giftendtime).replaceAll(TAG_END_MONEY_TIME, moneyendtime)
                .replaceAll(TAG_MONEY, program.momo_money + "").replaceAll(TAG_GIFT_VALUE, totalGiftValue + "");
        String notiBody = program.notiBody.replaceAll(TAG_END_GIFT_TIME, giftendtime).replaceAll(TAG_END_MONEY_TIME, moneyendtime)
                .replaceAll(TAG_MONEY, program.momo_money + "").replaceAll(TAG_GIFT_VALUE, totalGiftValue + "");
        String partnerName = program.partnerName;
        String serviceId = program.serviceId;

        Misc.sendTranHisAndNotiZaloMoney(vertx
                , DataUtil.strToInt(claimCodePromotionObj.phoneNumber)
                , tranComment
                , tranId
                , totalGiftValue
                , gift
                , notiCaption
                , notiBody
                , giftMessage
                , partnerName
                , serviceId
                , tranDb);
    }

    private void scanExpiredLuckyMoney(final Common.BuildLog log, final ClaimCodePromotionDb.Obj program)
    {
        JsonObject joFilter = new JsonObject();
        joFilter.putNumber(colName.ClaimCode_AllCheckCols.MONEY_STATUS, 0);

        JsonObject jsonLte = new JsonObject();
        jsonLte.putNumber(MongoKeyWords.LESS_THAN, System.currentTimeMillis());

        joFilter.putObject(colName.ClaimCode_AllCheckCols.MONEY_TIME, jsonLte);
        claimCode_allCheckDb.searchWithFilter(program.promotion_id, joFilter, new Handler<ArrayList<ClaimCode_AllCheckDb.Obj>>() {
                    @Override
                    public void handle(final ArrayList<ClaimCode_AllCheckDb.Obj> listRows) {
                        if (listRows.size() == 0) {
                            return;
                        }

                        final AtomicInteger atomicInteger = new AtomicInteger(listRows.size());
                        vertx.setPeriodic(100L, new Handler<Long>() {
                            @Override
                            public void handle(Long timerPeriodic) {
                                final int itemPosition = atomicInteger.decrementAndGet();
                                if (itemPosition < 0) {
                                    vertx.cancelTimer(timerPeriodic);
                                    return;
                                }
                                getBackLuckyMoney(program,listRows.get(itemPosition).phone, log, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonCallback) {
                                        int error = jsonCallback.getInteger(StringConstUtil.ERROR, -1);
                                        long tranId = jsonCallback.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                        if (error == 0) {
                                            //Tru tien thanh cong, cap nhat DB
                                            updateAllDb(listRows.get(itemPosition), program,log, tranId);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
    }

    private void getBackLuckyMoney(final ClaimCodePromotionDb.Obj program, String phone, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.add("desc", "getBackLuckyMoneyFromUser");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = StringConstUtil.ZaloPromo.ZALO_PROGRAM;
        Misc.adjustment(vertx, phone, program.agent, program.momo_money,
                Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("desc", "core tra ket qua");
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            log.add("core tra loi ", soapObjReply.error);
                            log.add("status ", soapObjReply.status);
                            log.add("tid", soapObjReply.tranId);
                            log.add("desc", "core tra loi");
//                            log.writeLog();
                            JsonObject jsonReply = new JsonObject();
                            jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "core tra loi");
                            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                            callback.handle(jsonReply);
                            return;
                        }
                        jsonReply.putNumber(StringConstUtil.ERROR, 0);
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        callback.handle(jsonReply);
                        return;
                    }
                });
    }

    private void updateAllDb(final ClaimCode_AllCheckDb.Obj claimAllCodeObj, final ClaimCodePromotionDb.Obj program, final Common.BuildLog log, final long tranId) {
        JsonObject joUpdate = new JsonObject();
        joUpdate.putNumber(colName.ZaloTetPromotionCol.MONEY_STATUS, 2);
        claimCode_allCheckDb.updatePartial(claimAllCodeObj.code, program.promotion_id, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                log.add("desc", "Da update voi sdt " + claimAllCodeObj.phone);
                sendNotiAndTranHisGetBackMoney(log, program, tranId, claimAllCodeObj.phone, program.momo_money);
            }
        });
    }

    public void sendNotiAndTranHisGetBackMoney(Common.BuildLog log, final ClaimCodePromotionDb.Obj program, final long tid,final String phoneNumber, long amount) {

        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.M2M_VALUE);
        jsonTrans.putString(colName.TranDBCols.COMMENT, program.transRollbackBody);
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
                noti.caption = program.notiRollbackTitle;
                noti.body = program.notiRollbackBody;
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

    private void loadClaimCodePromotion()
    {
        JsonObject joFilter = new JsonObject();
//        joFilter.putBoolean(colName.ClaimCodePromotionCols.ACTIVE_PROMO, true);
        claimCodePromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<ClaimCodePromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<ClaimCodePromotionDb.Obj> listClaimCodePromotion) {
                for(int i = 0; i < listClaimCodePromotion.size(); i++)
                {
                    claimCodePromotionJArr.add(listClaimCodePromotion.get(i).toJson());
                }
            }
        });
    }

    private void reloadClaimCodePromotion(final Message message)
    {
        JsonObject joFilter = new JsonObject();
//        joFilter.putBoolean(colName.ClaimCodePromotionCols.ACTIVE_PROMO, true);
        claimCodePromotionJArr = new JsonArray();
        claimCodePromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<ClaimCodePromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<ClaimCodePromotionDb.Obj> listClaimCodePromotion) {
                for(int i = 0; i < listClaimCodePromotion.size(); i++)
                {
                    claimCodePromotionJArr.add(listClaimCodePromotion.get(i).toJson());
                }
                message.reply(true);
            }
        });
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void givePostSaleServiceVoucherForJupViecUser(final Message<JsonObject> message
            , final long totalGiftValue
            , final ClaimCodePromotionDb.Obj program
            , final ClaimCodePromotionObj claimCodePromotionObj
            , final List<String> listVoucher
            , final Common.BuildLog log) {
        // Trả khuyến mãi
        // Them thong tin service id va so voucher vao core
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", program.promotion_id));
        keyValues.add(new Misc.KeyValue("group", claimCodePromotionObj.source));
        final JsonObject joReply = new JsonObject();
        //final String giftTypeId = reqObj.serviceId;
        log.add("TOTAL GIFT", listVoucher.size());
        log.add("TOTAL VALUE", totalGiftValue);

        int timeForGift = program.gift_time;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(program.agent
                , claimCodePromotionObj.phoneNumber
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
                    final String note = claimCodePromotionObj.source + program.promotion_id;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = claimCodePromotionObj.source;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                    log.add("so luong voucher", atomicInteger);

                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {
                            if (atomicInteger.decrementAndGet() < 0) {
                                log.add("func", "out of range for number " + claimCodePromotionObj.phoneNumber);
                                log.writeLog();
                                vertx.cancelTimer(aPeriodicLong);
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
//                                return;
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                log.add("itemPosition", itemPosition);
                                final boolean fireNoti = itemPosition == 0;
                                String giftModelId = listVoucher.get(itemPosition).trim().split(":")[0].trim();
                                long giftValueFinal = DataUtil.strToLong(listVoucher.get(itemPosition).trim().split(":")[1].trim());
                                giftType.setModelId(giftModelId);
//
                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(claimCodePromotionObj.phoneNumber
                                        , giftValueFinal
                                        , giftType
                                        , promotedTranId
                                        , program.agent
                                        , modifyDate
                                        , endGiftTime
                                        , keyValues, note, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {
                                        int err = jsonObject.getInteger("error", -1);
                                        final long tranId = jsonObject.getInteger("tranId", -1);
                                        final Gift gift = new Gift(jsonObject.getObject("gift"));
                                        final String giftId = gift.getModelId().trim();
                                        log.add("desc", "tra thuong chuong trinh claim code bang gift");
                                        log.add("err", err);

                                        //------------tat ca thanh cong roi
                                        if (err == 0) {
                                            giftManager.useGift(claimCodePromotionObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {
                                                    gift.status = 3;
                                                    JsonObject joUpdate = new JsonObject();
                                                    JsonObject joExtra = new JsonObject();
                                                    joExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, claimCodePromotionObj.tid);
                                                    joExtra.putString(StringConstUtil.ClaimCodePromotion.POST_SALE_JUPVIEC_SERVICE, claimCodePromotionObj.serviceId);
                                                    joExtra.putNumber(StringConstUtil.ClaimCodePromotion.AMOUNT, claimCodePromotionObj.amount);
                                                    joExtra.putNumber(StringConstUtil.ClaimCodePromotion.PROMOTION_TIME, System.currentTimeMillis());
                                                    joExtra.putString("vn_promotion_time", Misc.dateVNFormatWithDot(System.currentTimeMillis()));
                                                    joUpdate.putObject(colName.ClaimCode_AllCheckCols.JSON_EXTRA, joExtra);
                                                    claimCode_allCheckDb.updatePartialViaPhone(claimCodePromotionObj.phoneNumber, program.promotion_id, joUpdate, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean result) {
                                                            log.add("result update claim jupviec", result);
                                                            log.writeLog();
                                                            sendNotiAndTranHisForJupViec(totalGiftValue, program, claimCodePromotionObj, tranId, gift);
                                                        }
                                                    });
                                                }
                                            });
//                                            return;
                                        } else {
                                            log.add("error", -1000);
                                            log.add("desc", "Lỗi " + SoapError.getDesc(error));
                                            joReply.putNumber("error", 1000);
                                            joReply.putString(StringConstUtil.DESCRIPTION, "Hệ thống đang bảo trì. Vui lòng thử lại sau 30 phút");
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
}