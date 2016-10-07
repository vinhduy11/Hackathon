package com.mservice.momo.vertx.promotion_verticle;

import com.mservice.momo.data.*;
import com.mservice.momo.data.binhtanpromotion.DeviceDataUserDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.promotion.PromotionCountTrackingDb;
import com.mservice.momo.data.promotion.WomanNational2016Obj;
import com.mservice.momo.data.promotion.WomanNationalTableDb;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.AsyncResult;
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
 * Created by concu on 3/3/16.
 */
public class WomanNational2016Verticle extends Verticle {
    DeviceDataUserDb deviceDataUserDb;
    WomanNationalTableDb womanNationalTableDb;
    ErrorPromotionTrackingDb errorPromotionTrackingDb;
    MappingWalletBankDb mappingWalletBankDb;
    PromotionCountTrackingDb promotionCountTrackingDb;
    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private TransDb tranDb;
    private boolean isStoreApp;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());

        this.giftManager = new GiftManager(vertx, logger, glbCfg);

        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        // jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());
        promotionCountTrackingDb = new PromotionCountTrackingDb(vertx, logger);
        womanNationalTableDb = new WomanNationalTableDb(vertx, logger);
        deviceDataUserDb = new DeviceDataUserDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        mappingWalletBankDb = new MappingWalletBankDb(vertx, logger);
        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final WomanNational2016Obj womanNational2016Obj = new WomanNational2016Obj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(womanNational2016Obj.phoneNumber);
                log.add("phoneNumber " + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.phoneNumber);
                log.add("cashinTid " + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.cashinTid);
                log.add("card_id " + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.card_id);
                log.add("bank_code " + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.bank_code);
                log.add("joExtra " + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.joExtra);
                log.add("tranType "  + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.joExtra.getLong(StringConstUtil.TRANDB_TRAN_TYPE, 0));
                final JsonObject jsonReply = new JsonObject();
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                final PhonesDb.Obj phoneObj = new PhonesDb.Obj(womanNational2016Obj.joExtra.getObject(StringConstUtil.WomanNationalField.PHONE_OBJ, new JsonObject()));
                if ("".equalsIgnoreCase(womanNational2016Obj.phoneNumber)) {
                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Du lieu thieu sot nghiem trong");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Du lieu thieu sot nghiem trong");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }
                else if(("".equalsIgnoreCase(womanNational2016Obj.bank_code)
                        || "".equalsIgnoreCase(womanNational2016Obj.card_id)) && womanNational2016Obj.joExtra.getInteger(StringConstUtil.TRANDB_TRAN_TYPE, 0) != StringConstUtil.TranTypeExtra.FIRST_WALLET_MAPPING)
                {
                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Không có thông tin CMND");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Không có thông tin CMND của ngân hàng " + womanNational2016Obj.bank_code + " được ghi nhận nên tạm thời không trả thưởng, sẽ kiểm tra và trả bù lại sau");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }
                else if (isStoreApp) {
                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Diem giao dich khong duoc choi tro choi nay");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Điểm giao dịch không được tham gia chương trình này");
                    log.writeLog();
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
                        long gift_amount = 0;
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj promoObj = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
                                promoObj = new PromotionDb.Obj((JsonObject) o);
                                if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.WomanNationalField.PROGRAM)) {
                                    promo_start_date = promoObj.DATE_FROM;
                                    promo_end_date = promoObj.DATE_TO;
                                    agent = promoObj.ADJUST_ACCOUNT;
                                    break;
                                }
                            }
                            final PromotionDb.Obj womanPromoObj = promo_start_date > 0 ? promoObj : null;
                            //Check lan nua do dai chuoi ki tu
                            if ("".equalsIgnoreCase(agent) || womanPromoObj == null) {
                                log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                JsonObject jsonReply = new JsonObject();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else if (currentTime < promo_start_date || currentTime > promo_end_date) {
                                log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Chua bat dau chay chuong trinh " + womanPromoObj.NAME);
                                log.writeLog();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Chua bat dau chay chuong trinh.");
                                message.reply(jsonReply);
                                return;
                            } else if ("".equalsIgnoreCase(womanNational2016Obj.phoneNumber) || DataUtil.strToLong(womanNational2016Obj.phoneNumber) <= 0) {
                                log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "So dien thoai la so dien thoai khong co that.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            }
                            else if(womanNational2016Obj.joExtra.getInteger(StringConstUtil.TRANDB_TRAN_TYPE, 0) == StringConstUtil.TranTypeExtra.FIRST_WALLET_MAPPING)
                            {
                                log.add("Desc " + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.phoneNumber + " lan dau map vi ne");
                                WomanNationalTableDb.Obj womanObj = new WomanNationalTableDb.Obj();
                                womanObj.phoneNumber = womanNational2016Obj.phoneNumber;
                                womanNationalTableDb.insert(womanObj, new Handler<Integer>() {
                                    @Override
                                    public void handle(Integer event) {
                                        log.add("Desc " + StringConstUtil.WomanNationalField.PROGRAM, womanNational2016Obj.phoneNumber + " da insert thanh cong");
                                        log.writeLog();
                                    }
                                });
                                return;
                            }
//                            else if (phoneObj.createdDate < womanPromoObj.DATE_FROM || phoneObj.createdDate > womanPromoObj.DATE_TO) {
//                                log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Thoi gian tao vi ngoai thoi gian khuyen mai.");
//                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
//                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
//                                message.reply(jsonReply);
//                                log.writeLog();
//                            }
                            else {
                                if(womanNational2016Obj.joExtra.getLong(StringConstUtil.AMOUNT) < womanPromoObj.TRAN_MIN_VALUE)
                                {
                                    log.add("desc ", "Cashin nho hon gia tri cho phep " + womanPromoObj.TRAN_MIN_VALUE);
                                    log.writeLog();
                                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Cashin nhỏ hơn giá trị cho phép để được nhận thưởng là " + womanPromoObj.TRAN_MIN_VALUE);
                                    message.reply(jsonReply);
                                    return;
                                }
                                //Kiem tra xem da tra thuong cho em nay chua
                                JsonObject joFilter = new JsonObject();
                                final JsonObject joCardId = new JsonObject();
                                JsonObject joPhoneNumber = new JsonObject();
                                joPhoneNumber.putString(colName.WomanNationalCols.PHONE_NUMBER, womanNational2016Obj.phoneNumber);
                                joCardId.putString(colName.WomanNationalCols.CARD_ID, womanNational2016Obj.card_id);
                                JsonArray jOr = new JsonArray();
                                jOr.add(joCardId);
                                jOr.add(joPhoneNumber);
                                joFilter.putArray(MongoKeyWords.OR, jOr);
                                womanNationalTableDb.searchWithFilter(joFilter, new Handler<ArrayList<WomanNationalTableDb.Obj>>() {
                                    @Override
                                    public void handle(ArrayList<WomanNationalTableDb.Obj> listData) {
                                        if (listData.size() == 1 && listData.get(0).cashInTime == 0 && "".equalsIgnoreCase(listData.get(0).bankCode)
                                                && "".equalsIgnoreCase(listData.get(0).cardId)) {
                                            boolean isCheckSim = womanPromoObj.EXTRA.getBoolean("is_check_sim", true);
                                            getExtraKeyFromApp("0" + phoneObj.number, StringConstUtil.WomanNationalField.PROGRAM, phoneObj.deviceInfo, isCheckSim, log, phoneObj.phoneOs, phoneObj.lastImei, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject joResponse) {
                                                    int err = joResponse.getInteger(StringConstUtil.ERROR, 1000);
                                                    String desc = joResponse.getString(StringConstUtil.DESCRIPTION, "ERROR");
                                                    log.add("err " + StringConstUtil.WomanNationalField.PROGRAM, err);
                                                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, desc);
                                                    if (err == 0) {
                                                        //Tra thuong cho em no.
                                                        List<String> listVoucher = getListVoucher(womanPromoObj.INTRO_SMS);
                                                        giveListVouchersForUser(message, womanPromoObj.PER_TRAN_VALUE, womanPromoObj.DURATION, new JsonObject(), womanPromoObj.ADJUST_ACCOUNT, womanNational2016Obj
                                                                , womanPromoObj, listVoucher, log);
                                                    } else {
                                                        log.add("desc ", "Thiet bi nay da nhan thuong, khong tra thuong nua nhe");
//                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
//                                            jsonReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã được nhận thưởng rồi nè, không trả thưởng nữa");
                                                        message.reply(joResponse);
                                                        log.writeLog();
                                                    }
                                                }
                                            });
                                        }
                                        else if(listData.size() == 0)
                                        {
                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "chua ghi nhan thong tin bank ve mongo");
                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Lay thong tin BANK tu Connector");
                                            JsonObject joQuery = new JsonObject()
                                                    .putString(colName.WomanNationalCols.PHONE_NUMBER, womanNational2016Obj.phoneNumber)
                                                    .putString(colName.WomanNationalCols.CARD_ID, womanNational2016Obj.card_id)
                                                    .putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.GET_INFO_BANK_USER);
                                            vertx.eventBus().sendWithTimeout(AppConstant.LStandbyOracleVerticle_ADDRESS, joQuery, 60000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                                                @Override
                                                public void handle(AsyncResult<Message<JsonObject>> dataRespond) {
                                                    if (dataRespond != null && dataRespond.result() != null && dataRespond.succeeded()) {
                                                        JsonObject joResponse = dataRespond.result().body();
                                                        JsonArray jarrData = joResponse.getArray(StringConstUtil.RESULT, new JsonArray());
                                                        if (jarrData.size() < 1 || jarrData.size() > 1) {
                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " So dien thoai da map vi nhieu lan nen khong tra thuong " + womanNational2016Obj.phoneNumber);
                                                            log.writeLog();
                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, " So dien thoai da map vi nhieu lan nen khong tra thuong " + womanNational2016Obj.phoneNumber);
                                                            message.reply(jsonReply);
                                                            return;
                                                        }
                                                        JsonObject joData = jarrData.get(0);
                                                        final String bankCode = joData.getString(colName.PhoneDBCols.BANK_CODE, "");
                                                        final String bankName = joData.getString(colName.PhoneDBCols.BANK_NAME, "");
                                                        final String phoneNumber = joData.getString(colName.PhoneDBCols.NUMBER, "");
                                                        final String bankCardId = joData.getString(colName.PhoneDBCols.BANK_PERSONAL_ID, "");
                                                        final long mappingTime = joData.getLong(colName.MappingWalletBank.MAPPING_TIME, 0);
                                                        final long unmappingTime = joData.getLong(colName.MappingWalletBank.UNMAPPING_TIME, 0);
                                                        Calendar calendar = Calendar.getInstance();
                                                        if (unmappingTime != 0) {
                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " So dien thoai da unmap vi nen khong tra thuong " + womanNational2016Obj.phoneNumber);
                                                            log.writeLog();
                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, " So dien thoai da unmap vi nen khong tra thuong " + womanNational2016Obj.phoneNumber);
                                                            message.reply(jsonReply);
                                                            return;
                                                        }
                                                        else if ("".equalsIgnoreCase(bankCode) || "".equalsIgnoreCase(bankName) || "".equalsIgnoreCase(bankCardId)) {
                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " Thiếu thông tin trả thưởng từ hệ thống core nên kiểm tra và trả bù lại sau " + womanNational2016Obj.phoneNumber);
                                                            log.writeLog();
                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, " Thiếu thông tin trả thưởng từ hệ thống core nên kiểm tra và trả bù lại sau " + womanNational2016Obj.phoneNumber);
                                                            message.reply(jsonReply);
                                                            return;
                                                        }
//                                                        Date simpleDateFormat = null;
//                                                        try {
//                                                            simpleDateFormat = new SimpleDateFormat("dd-MMM-yy hh.mm.ss.S a").parse(mappingTime);
//                                                        } catch (ParseException e) {
//                                                            e.printStackTrace();
//                                                        }
                                                        if(mappingTime == 0)
                                                        {
                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " Thiếu thông tin trả thưởng từ hệ thống core nên kiểm tra và trả bù lại sau " + womanNational2016Obj.phoneNumber);
                                                            log.writeLog();
                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, " Thiếu thông tin trả thưởng từ hệ thống core nên kiểm tra và trả bù lại sau " + womanNational2016Obj.phoneNumber);
                                                            message.reply(jsonReply);
                                                            return;
                                                        }
//                                                        calendar.setTime(simpleDateFormat);
//                                                        final long mappingTimeUser = calendar.getTimeInMillis();
                                                        if(mappingTime < womanPromoObj.DATE_FROM)
                                                        {
                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " Map ví trước thời gian diễn ra chương trình " + womanNational2016Obj.phoneNumber);
                                                            log.writeLog();
                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, " Map ví trước thời gian diễn ra chương trình " + womanNational2016Obj.phoneNumber);
                                                            message.reply(jsonReply);
                                                            return;
                                                        }
                                                        else if(mappingTime < System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 7)
                                                        {
                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " Kiem tra va se hoan tra neu thoa dieu kien nhan thuong " + womanNational2016Obj.phoneNumber);
                                                            log.writeLog();
                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, " Kiem tra va se hoan tra neu thoa dieu kien nhan thuong " + womanNational2016Obj.phoneNumber);
                                                            message.reply(jsonReply);
                                                            return;
                                                        }
                                                        JsonObject joFilter = new JsonObject();
                                                        JsonObject joNumber = new JsonObject().putString(colName.MappingWalletBank.NUMBER, womanNational2016Obj.phoneNumber);
                                                        JsonObject joCardInfo = new JsonObject().putString(colName.MappingWalletBank.CUSTOMER_ID, womanNational2016Obj.card_id);
                                                        JsonArray jarrOr = new JsonArray().add(joNumber).add(joCardInfo);
                                                        joFilter.putArray(MongoKeyWords.OR, jarrOr);
                                                        mappingWalletBankDb.searchWithFilter(joFilter, new Handler<ArrayList<MappingWalletBankDb.Obj>>() {
                                                            @Override
                                                            public void handle(ArrayList<MappingWalletBankDb.Obj> mappingWalletList) {
                                                                if(mappingWalletList.size() > 1)
                                                                {
                                                                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " Map vi LKTK > 1 " + womanNational2016Obj.phoneNumber);
                                                                    log.writeLog();
                                                                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                                    jsonReply.putString(StringConstUtil.DESCRIPTION, " Map vi LKTK > 1 " + womanNational2016Obj.phoneNumber);
                                                                    message.reply(jsonReply);
                                                                    return;
                                                                }
                                                                else if(mappingWalletList.size() == 1 && mappingWalletList.get(0).mapping_time < (System.currentTimeMillis() - 1000L * 60 * 60))
                                                                {
                                                                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " Map vi LKTK da lau => ko tra " + womanNational2016Obj.phoneNumber);
                                                                    log.writeLog();
                                                                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                                    jsonReply.putString(StringConstUtil.DESCRIPTION, " Map vi LKTK da lau => ko tra " + womanNational2016Obj.phoneNumber);
                                                                    message.reply(jsonReply);
                                                                    return;
                                                                }
                                                                else {
                                                                    boolean isCheckSim = womanPromoObj.EXTRA.getBoolean("is_check_sim", true);
                                                                    getExtraKeyFromApp("0" + phoneObj.number, StringConstUtil.WomanNationalField.PROGRAM, phoneObj.deviceInfo, isCheckSim, log, phoneObj.phoneOs, phoneObj.lastImei, new Handler<JsonObject>() {
                                                                        @Override
                                                                        public void handle(JsonObject joResponse) {
                                                                            int err = joResponse.getInteger(StringConstUtil.ERROR, 1000);
                                                                            String desc = joResponse.getString(StringConstUtil.DESCRIPTION, "ERROR");
                                                                            log.add("err " + StringConstUtil.WomanNationalField.PROGRAM, err);
                                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, desc);
                                                                            if (err == 0) {
                                                                                //Tra thuong cho em no.
                                                                                final List<String> listVoucher = getListVoucher(womanPromoObj.INTRO_SMS);
                                                                                promotionCountTrackingDb.findAndIncCountUser(phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, bankCardId, new Handler<PromotionCountTrackingDb.Obj>() {
                                                                                    @Override
                                                                                    public void handle(PromotionCountTrackingDb.Obj promoCountTrackingObj) {
                                                                                        if(promoCountTrackingObj != null && promoCountTrackingObj.count == 1)
                                                                                        {
                                                                                            giveListVouchersForUser(message, womanPromoObj.PER_TRAN_VALUE, womanPromoObj.DURATION, new JsonObject(), womanPromoObj.ADJUST_ACCOUNT, womanNational2016Obj
                                                                                                    , womanPromoObj, listVoucher, log);
                                                                                        }
                                                                                        else {
                                                                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " Số điện thoại này đã được trả thưởng " + womanNational2016Obj.phoneNumber);
                                                                                            log.writeLog();
                                                                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                                                            jsonReply.putString(StringConstUtil.DESCRIPTION, " Số điện thoại này đã được trả thưởng " + womanNational2016Obj.phoneNumber);
                                                                                            message.reply(jsonReply);
                                                                                            return;
                                                                                        }
                                                                                    }
                                                                                });
                                                                            } else {
                                                                                log.add("desc ", "Thiet bi nay da nhan thuong, khong tra thuong nua nhe");
//                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
//                                            jsonReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã được nhận thưởng rồi nè, không trả thưởng nữa");
                                                                                message.reply(joResponse);
                                                                                log.writeLog();
                                                                            }
                                                                            JsonObject joWalletUpdate = new JsonObject();
                                                                            String id = phoneNumber + bankCode + bankCardId;
                                                                            joWalletUpdate.putString(colName.MappingWalletBank.ID, id);
                                                                            joWalletUpdate.putString(colName.MappingWalletBank.NUMBER, phoneNumber);
                                                                            joWalletUpdate.putString(colName.MappingWalletBank.BANK_NAME, bankName);
                                                                            joWalletUpdate.putString(colName.MappingWalletBank.BANK_CODE, bankCode);
                                                                            joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_NAME, "");
                                                                            joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_ID, bankCardId);
                                                                            joWalletUpdate.putNumber(colName.MappingWalletBank.MAPPING_TIME, mappingTime);
                                                                            mappingWalletBankDb.upsertWalletBank(id, joWalletUpdate, new Handler<Boolean>() {
                                                                                @Override
                                                                                public void handle(Boolean aBoolean) {
                                                                                    logger.info("update mappingWalletBankDb for bankInfo.getPhoneNumber() is " + phoneNumber);
                                                                                }
                                                                            });
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                                    } else {
                                                        log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, " ghi nhan va tra bu cho user " + womanNational2016Obj.phoneNumber);
                                                        log.writeLog();
                                                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                                        jsonReply.putString(StringConstUtil.DESCRIPTION, " ghi nhan va tra bu cho user " + womanNational2016Obj.phoneNumber);
                                                        message.reply(jsonReply);
                                                    }
                                                }
                                            });
                                        }
                                        else {
                                            List<String> listPhoneCmnd = new ArrayList<String>();
                                            String phoneCmnd = "";
                                            for(int i = 0; i < listData.size(); i++)
                                            {
                                                if(!"".equalsIgnoreCase(listData.get(i).cardId))
                                                {
                                                    phoneCmnd = "SDT " + listData.get(i).phoneNumber + "co thong tin CMND la " + listData.get(i).cardId;
                                                    listPhoneCmnd.add(phoneCmnd);
                                                }
                                            }
                                            String desc = listData.size() == 0 ? "Số điện th " + womanNational2016Obj.phoneNumber + " đã map ví nên không được tham gia chương trình này" : "CMND " + womanNational2016Obj.card_id
                                            + " của số điện th " + womanNational2016Obj.phoneNumber + " đã được nhận thưởng. INFO: " + listPhoneCmnd.toString();
                                            log.add("desc ", desc);
                                            log.writeLog();
                                            jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                            jsonReply.putString(StringConstUtil.DESCRIPTION, desc );
                                            message.reply(jsonReply);
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            }
        };

        vertx.eventBus().registerLocalHandler(AppConstant.WOMAN_NATIONAL_2016_PROMOTION_BUS_ADDRESS, myHandler);
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
    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void giveVoucherForUser(final Message<JsonObject> message
            , final PromotionDb.Obj promoObj
            , final WomanNational2016Obj womanNational2016Obj
            , final Common.BuildLog log) {
        // Trả khuyến mãi
        // Them thong tin service id va so voucher vao core
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", StringConstUtil.WomanNationalField.PROGRAM));
        keyValues.add(new Misc.KeyValue("group", StringConstUtil.WomanNationalField.PROGRAM));
        final JsonObject joReply = new JsonObject();
        //final String giftTypeId = reqObj.serviceId;
        log.add("TOTAL VALUE "  + StringConstUtil.WomanNationalField.PROGRAM, promoObj.PER_TRAN_VALUE);

        final int endGiftTime = promoObj.DURATION;
        //Tra thuong trong core
        giftManager.adjustGiftValue(promoObj.ADJUST_ACCOUNT
                , womanNational2016Obj.phoneNumber
                , promoObj.PER_TRAN_VALUE
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error "  + StringConstUtil.WomanNationalField.PROGRAM, error);
                log.add("desc "  + StringConstUtil.WomanNationalField.PROGRAM, SoapError.getDesc(error));

                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    //tao gift tren backend
                    final GiftType giftType = new GiftType();
                    final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    final Misc.KeyValue kv = new Misc.KeyValue();

                    final long modifyDate = System.currentTimeMillis();
                    final String note = StringConstUtil.WomanNationalField.PROGRAM;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = StringConstUtil.WomanNationalField.PROGRAM;
                    keyValues.add(kv);

                    String giftModelId = promoObj.INTRO_SMS;
                    long giftValueFinal = promoObj.PER_TRAN_VALUE;
                    giftType.setModelId(giftModelId);
//
                    giftManager.createLocalGiftForBillPayPromoWithDetailGift(womanNational2016Obj.phoneNumber
                            , giftValueFinal
                            , giftType
                            , promotedTranId
                            , promoObj.ADJUST_ACCOUNT
                            , modifyDate
                            , endGiftTime
                            , keyValues, note, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            int err = jsonObject.getInteger("error", -1);
                            final long tranId = jsonObject.getInteger("tranId", -1);
                            final Gift gift = new Gift(jsonObject.getObject("gift"));
                            final String giftId = gift.getModelId().trim();
                            log.add("desc "  + StringConstUtil.WomanNationalField.PROGRAM, "tra thuong chuong trinh woman bang gift");
                            log.add("err "  + StringConstUtil.WomanNationalField.PROGRAM, err);

                            //------------tat ca thanh cong roi
                            if (err == 0) {
                                giftManager.useGift(womanNational2016Obj.phoneNumber, giftId, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {
                                        gift.status = 3;
                                        updateVoucherInfoForUser(giftId, womanNational2016Obj, log, tranId, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean result) {
                                                if(result)
                                                {
                                                    sendNotiAndTranHis(promoObj, womanNational2016Obj, tranId, gift);
                                                }
                                            }
                                        });
                                    }
                                });
//                                            return;
                            } else {
                                log.add("error "  + StringConstUtil.WomanNationalField.PROGRAM, -1000);
                                log.add("desc "  + StringConstUtil.WomanNationalField.PROGRAM, "Lỗi " + SoapError.getDesc(error));
                                joReply.putNumber("error", 1000);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Loi core");
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                    return;
                } else {
                    //tra thuong trong core khong thanh cong
                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Lỗi " + SoapError.getDesc(error));
                    log.add("Exception "  + StringConstUtil.WomanNationalField.PROGRAM, "Exception " + SoapError.getDesc(error));
                    message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, error).putString(StringConstUtil.DESCRIPTION, "Hệ thống tạm thời gián đoạn. Vui lòng thực hiện lại sau 30 phút"));
                    log.writeLog();
                    return;
                }
            }
        });
    }

    private void updateVoucherInfoForUser(final String giftId, final WomanNational2016Obj womanNational2016Obj, final Common.BuildLog log, final long tranId, final Handler<Boolean> callback)
    {
        WomanNationalTableDb.Obj womanObj = new WomanNationalTableDb.Obj();
        womanObj.cardId = womanNational2016Obj.card_id;
        womanObj.cashInTid = womanNational2016Obj.cashinTid;
        womanObj.phoneNumber = womanNational2016Obj.phoneNumber;
        womanObj.bankCode = womanNational2016Obj.bank_code;
        womanObj.giftId = giftId;
        womanObj.cashInTime = System.currentTimeMillis();

        womanNationalTableDb.upSert(womanNational2016Obj.phoneNumber, womanObj.toJson(), new Handler<Boolean>() {

            @Override
            public void handle(Boolean result) {
                callback.handle(result);
            }
        });
    }

    private void sendNotiAndTranHis(PromotionDb.Obj promoObj, WomanNational2016Obj womanNational2016Obj, long tranId, Gift gift) {
        String notiCaption = promoObj.NOTI_CAPTION;
        String giftMessage = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
        String tranComment = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
        String notiBody = String.format(promoObj.NOTI_COMMENT, Misc.dateVNFormatWithDot(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * promoObj.DURATION ));
        String partnerName = promoObj.INTRO_DATA;
        String serviceId = "topup";

        Misc.sendTranHisAndNotiZaloMoney(vertx
                , DataUtil.strToInt(womanNational2016Obj.phoneNumber)
                , tranComment
                , tranId
                , promoObj.PER_TRAN_VALUE
                , gift
                , notiCaption
                , notiBody
                , giftMessage
                , partnerName
                , serviceId
                , tranDb);
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void giveListVouchersForUser(final Message<JsonObject> message
            , final long value_of_gift
            , final int time_for_gift
            , final JsonObject joReply
            , final String agent
            , final WomanNational2016Obj womanNational2016Obj
            , final PromotionDb.Obj promoObj
            , final List<String> listVoucher
            , final Common.BuildLog log) {
        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", StringConstUtil.WomanNationalField.PROGRAM));

        keyValues.add(new Misc.KeyValue("group", StringConstUtil.WomanNationalField.PROGRAM));



        log.add("TOTAL GIFT " + StringConstUtil.WomanNationalField.PROGRAM, listVoucher.size());
        log.add("TOTAL VALUE " + StringConstUtil.WomanNationalField.PROGRAM, value_of_gift);

        int timeForGift = time_for_gift;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , womanNational2016Obj.phoneNumber
                , value_of_gift
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error" + StringConstUtil.WomanNationalField.PROGRAM, error);
                log.add("desc" + StringConstUtil.WomanNationalField.PROGRAM, SoapError.getDesc(error));

                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    //tao gift tren backend
                    final GiftType giftType = new GiftType();
                    final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    final Misc.KeyValue kv = new Misc.KeyValue();

                    final long modifyDate = System.currentTimeMillis();
                    final String note = StringConstUtil.WomanNationalField.PROGRAM;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = StringConstUtil.WomanNationalField.PROGRAM;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                    log.add("so luong voucher " + StringConstUtil.WomanNationalField.PROGRAM, atomicInteger);

                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {
                            if (atomicInteger.decrementAndGet() < 0) {
                                log.add("func " + StringConstUtil.WomanNationalField.PROGRAM, "out of range for number " + womanNational2016Obj.phoneNumber);
                                log.writeLog();
                                vertx.cancelTimer(aPeriodicLong);
                                message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                log.add("itemPosition " + StringConstUtil.WomanNationalField.PROGRAM, itemPosition);
                                String[] gift = listVoucher.get(itemPosition).split(":");
                                giftType.setModelId(gift[0].toString().trim());
                                long giftValue = DataUtil.strToLong(gift[1].toString().trim());
                                if(giftValue < 1)
                                {
                                    log.add("func " + StringConstUtil.WomanNationalField.PROGRAM, "Thong tin cau hinh qua khong chinh xac " + womanNational2016Obj.phoneNumber);
                                    log.writeLog();
                                    vertx.cancelTimer(aPeriodicLong);
                                    message.reply(new JsonObject().putNumber(StringConstUtil.ERROR, -1000).putString(StringConstUtil.DESCRIPTION, "Thông tin cấu hình quà không đúng, vận hành chỉnh lại webadmin"));
                                    return;
                                }

                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(womanNational2016Obj.phoneNumber
                                        , giftValue
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
                                        log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "tra thuong chuong trinh woman bang gift");
                                        log.add("err " + StringConstUtil.WomanNationalField.PROGRAM, err);

                                        //------------tat ca thanh cong roi
                                        if (err == 0) {
                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Tao qua local woman thanh cong");
                                            if(promoObj.ENABLE_PHASE2)
                                            {
                                                giftManager.useGift(womanNational2016Obj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject jsonObject) {
                                                        if(itemPosition == 0)
                                                        {
                                                            updateVoucherInfoForUser(giftId, womanNational2016Obj, log, tranId, new Handler<Boolean>() {
                                                                @Override
                                                                public void handle(Boolean result) {
//                                                                    if(result == 0)
//                                                                    {
                                                                        sendNotiAndTranHis(promoObj, womanNational2016Obj, tranId, gift);
//                                                                    }
                                                                }
                                                            });
                                                            return;
                                                        }
                                                    }
                                                });
                                                return;
                                            }
                                            else {
                                                if(itemPosition == 0)
                                                {
                                                    updateVoucherInfoForUser(giftId, womanNational2016Obj, log, tranId, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean result) {
//                                                            if(result == 0)
//                                                            {
                                                                sendNotiAndTranHis(promoObj, womanNational2016Obj, tranId, gift);
//                                                            }
                                                        }
                                                    });
                                                    return;
                                                }
                                                return;
                                            }

                                        } else {
                                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Tao qua woman fail");
                                            log.writeLog();
                                            joReply.putNumber(StringConstUtil.ERROR, err);
                                            joReply.putString(StringConstUtil.DESCRIPTION, "Lỗi backend => không thể tạo quà tặng cho khách hàng.");
                                            message.reply(joReply);
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
                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Core loi");
                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Lỗi " + SoapError.getDesc(error));
                    log.add("Exception " + StringConstUtil.WomanNationalField.PROGRAM, "Exception " + SoapError.getDesc(error));
                    joReply.putNumber(StringConstUtil.ERROR, error);
                    joReply.putString(StringConstUtil.DESCRIPTION, SoapError.getDesc(error));
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
            }
        });
    }

    public void getExtraKeyFromApp(final String phoneNumber, final String program, final String extraKey, boolean isCheckSim, final Common.BuildLog log,final String os, final String imei, final Handler<JsonObject> callback) {
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
//            if(!"".equalsIgnoreCase(extraKeyTemp))
//            {
//                int count = 0;
//                boolean isKilled = false;
//                String []extraKeyTempArr = extraKeyTemp.split(MomoMessage.BELL);
//                for(int i = 0; i < extraKeyTempArr.length; i++)
//                {
////                    if("0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))
////                    {
////                        isKilled = true;
////                        break;
////                    }
//                    if(/*!"XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) &&*/ /*!"-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) &&*/ !"".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())
//                            /*&& !"0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())*/  /*&& (!isCheckSim || !"XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))*/)
//                    {
//                        log.add("desc " + program, "Thiết bị này có sim");
//                        count = count + 1;
//                    }
////                    if("XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) || "-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))
////                    {
////                        joReply.putNumber(StringConstUtil.ERROR, 1000);
////                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
////                        log.add("error " + program, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
////                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn " + " " + phoneNumber);
////                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
////                        callback.handle(joReply);
////                        return;
////                    }
//                }
////                if(isKilled)
////                {
////                    joReply.putNumber(StringConstUtil.ERROR, 1000);
////                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này dùng máy ảo nên không được tham gia chương trình");
////                    log.add("error " + program, "Thiết bị này dùng máy ảo nên không được tham gia chương trình");
////                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này dùng máy ảo nên không được tham gia chương trình " + " " + phoneNumber);
////                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
////                    callback.handle(joReply);
////                    return;
////                }
//                /*else*/ if (count < 2) {
//                    joReply.putNumber(StringConstUtil.ERROR, 1000);
//                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
//                    log.add("error " + program, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
//                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn " + " " + phoneNumber);
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
//                    callback.handle(joReply);
//                    return;
//                }
//            }
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
            final AtomicInteger integer = new AtomicInteger(address_tmp.length > 3 ? 3 : address_tmp.length);
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
                    else if(address_tmp[position].equalsIgnoreCase("XXX")) {
//                        vertx.cancelTimer(event);
                        log.add("error " + program, "Loi insert android data user bi xxx");
                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này không truyền đủ thông tin, sẽ kiểm tra và trả bù nếu hợp lệ. Xin cám ơn " +  phoneNumber);
                        callback.handle(joReply);
                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này không truyền đủ thông tin, sẽ kiểm tra và trả bù nếu hợp lệ. Xin cám ơn " +  phoneNumber);
                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
//                        return;
                    }
                    else if(address_tmp[position].trim().equalsIgnoreCase("02:00:00:00:00:00"))
                    {
                        log.add("item " + phoneNumber + " " + program, address_tmp[position]);
                        log.add("item " + phoneNumber + " " + program, "ANDROID 6 nen bo qua imei");
                    }
                    else {
                        log.add("item " + program, address_tmp[position]);
                        final DeviceDataUserDb.Obj deviceDataUserObj = new DeviceDataUserDb.Obj();
                        deviceDataUserObj.id = address_tmp[position].toString().trim();
                        deviceDataUserObj.phoneNumber = phoneNumber;
                        deviceDataUserDb.insert(deviceDataUserObj, program, new Handler<Integer>() {
                            @Override
                            public void handle(Integer resultInsert) {
                                if (resultInsert != 0) {
                                    vertx.cancelTimer(event);
                                    log.add("error " + program, "Loi insert android data user");
                                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                                    joReply.putString(StringConstUtil.DESCRIPTION, "Du lieu da ton tai, khong cho so nay tham gia chuong trinh : " + deviceDataUserObj.id);
                                    callback.handle(joReply);
                                    //cong nguyen 05/08/2016 them thong tin cho cskh tra loi.
//                                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Du lieu da ton tai, khong cho so nay tham gia chuong trinh" + " " + phoneNumber);
//                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, program, 1000, joDesc.toString());
                                    JsonObject joFilter = new JsonObject().putString(colName.DeviceDataUser.ID, deviceDataUserObj.id);
                                    deviceDataUserDb.searchWithFilter(StringConstUtil.BinhTanPromotion.PROGRAM, joFilter, new Handler<ArrayList<DeviceDataUserDb.Obj>>() {
                                        @Override
                                        public void handle(ArrayList<DeviceDataUserDb.Obj> listDevices) {
                                            String phoneDup = "";
                                            if(listDevices.size() > 0)
                                            {
                                                phoneDup = listDevices.get(0).phoneNumber;
                                            }
                                            logger.info("Kiem tra so dien thoai da duoc nhan thuong truoc do bang thiet bi nay.");
                                            JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị ANDROID của số điện thoại " + phoneNumber + " đã được trả thưởng cho số điện thoại " + phoneDup);
                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, joDesc.toString());
                                        }
                                    });
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


//    private void getExtraKeyFromApp(final String phoneNumber, final String program, final String extraKey,final Common.BuildLog log, final String os, final String imei, final Handler<JsonObject> callback) {
//        final JsonObject joReply = new JsonObject();
//        final String[] address_tmp = extraKey.split(MomoMessage.BELL);
//        log.add("size address tmp " + StringConstUtil.WomanNationalField.PROGRAM, address_tmp.length);
//        if (!os.equalsIgnoreCase("ios") && address_tmp.length == 0) {
//            joReply.putNumber(StringConstUtil.ERROR, 1000);
//            joReply.putString(StringConstUtil.DESCRIPTION, "Thieu du lieu extra key");
////            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, 1000, "Thieu du lieu extra key" + " " + phoneNumber);
//            callback.handle(joReply);
//        }
//        else if (!os.equalsIgnoreCase(StringConstUtil.IOS_OS) && !os.equalsIgnoreCase(StringConstUtil.ANDROID_OS) ) {
//            joReply.putNumber(StringConstUtil.ERROR, 1000);
//            joReply.putString(StringConstUtil.DESCRIPTION, "Thiet bi WINDOW PHONES khong duoc tham gia chuong trinh nay.");
////            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, 1000, "Thieu du lieu extra key" + " " + phoneNumber);
//            callback.handle(joReply);
//        }
//        else {
//            final AtomicInteger integer = new AtomicInteger(address_tmp.length);
//            final AtomicInteger empty = new AtomicInteger(0);
//            if (!os.equalsIgnoreCase("ios")) {
//                log.add("os " + StringConstUtil.WomanNationalField.PROGRAM, "android");
//                takeNoteDeviceInfo(phoneNumber, log, callback, joReply, address_tmp, integer, empty, extraKey);
//            }
////            else if(data.os.equalsIgnoreCase("ios") && data.appCode >= 1923)
////            {
////                //Thuc hien luu tru moi
////                log.add("os " + StringConstUtil.BinhTanPromotion.PROGRAM, "ios");
////                takeNoteDeviceInfo(phoneNumber, log, callback, joReply, address_tmp, integer, empty);
////            }
//            else if (os.equalsIgnoreCase("ios")) {
//                DeviceDataUserDb.Obj deviceObj = new DeviceDataUserDb.Obj();
//                deviceObj.phoneNumber = phoneNumber;
//                deviceObj.id = imei;
//                deviceDataUserDb.insert(deviceObj, program, new Handler<Integer>() {
//                    @Override
//                    public void handle(Integer result) {
//                        if (result == 0) {
//                            joReply.putNumber(StringConstUtil.ERROR, 0);
//                            joReply.putString(StringConstUtil.DESCRIPTION, "GOOD");
//                        } else {
//                            joReply.putNumber(StringConstUtil.ERROR, 1000);
//                            joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã được sử dụng, không cho tham gia khuyến mãi nữa.");
//                            joReply.putString(StringConstUtil.DEVICE_IMEI, extraKey);
//                            log.add("error " + StringConstUtil.WomanNationalField.PROGRAM, "Loi insert ios data user");
////                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, 1000, "Du lieu thiet bi da ton tai, khong cho so nay tham gia khuyen mai LKTK nua" + " " + phoneNumber);
//                        }
//                        callback.handle(joReply);
//                    }
//                });
//                return;
//            } else {
////                processIronMan(buf, log, msg, sock);
//                log.add("desc" + StringConstUtil.WomanNationalField.PROGRAM, "Khong ton tai thiet bi nay");
//                joReply.putNumber(StringConstUtil.ERROR, 1000);
//                joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này không tồn tại");
////                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, 1000, "Khong ton tai thiet bi, khong cho tham gia chuong trinh" + " " + phoneNumber);
//                callback.handle(joReply);
//            }
//
//        }
//    }
//
//
//    private void takeNoteDeviceInfo(final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback, final JsonObject joReply, final String[] address_tmp, final AtomicInteger integer, final AtomicInteger empty, final String extraKey) {
//        vertx.setPeriodic(200L, new Handler<Long>() {
//            @Override
//            public void handle(final Long event) {
//                int position = integer.decrementAndGet();
//                if (position < 0) {
//                    log.add("position " + StringConstUtil.WomanNationalField.PROGRAM, position);
//                    vertx.cancelTimer(event);
//                    if (empty.intValue() != address_tmp.length) {
//                        log.add("position " + StringConstUtil.WomanNationalField.PROGRAM, "empty.intValue() != address_tmp.length");
//                        joReply.putNumber(StringConstUtil.ERROR, 0);
//                        joReply.putString(StringConstUtil.DESCRIPTION, "GOOD");
//                    } else {
//                        log.add("position " + StringConstUtil.WomanNationalField.PROGRAM, "empty.intValue() == address_tmp.length");
//                        joReply.putNumber(StringConstUtil.ERROR, 0);
//                        joReply.putString(StringConstUtil.DESCRIPTION, "GOOD");
//                    }
//                    callback.handle(joReply);
//                    return;
//                } else {
//                    if (address_tmp[position].equalsIgnoreCase("")) {
//                        log.add("item " + StringConstUtil.WomanNationalField.PROGRAM, address_tmp[position]);
//                        empty.incrementAndGet();
//                    } else {
//                        log.add("item " + StringConstUtil.WomanNationalField.PROGRAM, address_tmp[position]);
//                        DeviceDataUserDb.Obj deviceDataUserObj = new DeviceDataUserDb.Obj();
//                        deviceDataUserObj.id = address_tmp[position].toString().trim();
//                        deviceDataUserObj.phoneNumber = phoneNumber;
//                        deviceDataUserDb.insert(deviceDataUserObj, StringConstUtil.WomanNationalField.PROGRAM, new Handler<Integer>() {
//                            @Override
//                            public void handle(Integer resultInsert) {
//                                if (resultInsert != 0) {
//                                    vertx.cancelTimer(event);
//                                    log.add("error " + StringConstUtil.WomanNationalField.PROGRAM, "Loi insert android data user");
//                                    joReply.putNumber(StringConstUtil.ERROR, 1000);
//                                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã tham gia chương trình, không cho tham gia chương trình LKTK nữa.");
//                                    joReply.putString(StringConstUtil.DEVICE_IMEI, extraKey);
//                                    callback.handle(joReply);
////                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, 1000, "Du lieu da ton tai, khong cho so nay tham gia chuong trinh" + " " + phoneNumber);
//                                    return;
//                                }
//                            }
//                        });
//                    }
//                }
//            }
//        });
//        return;
//    }

}
