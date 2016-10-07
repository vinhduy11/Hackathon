package com.mservice.momo.vertx.processor;

import com.mservice.bank.entity.BankResponse;
import com.mservice.common.BankInfo;
import com.mservice.momo.data.*;
import com.mservice.momo.data.binhtanpromotion.DeviceDataUserDb;
import com.mservice.momo.data.codeclaim.ClaimCodePromotionObj;
import com.mservice.momo.data.discount50percent.RollBack50PerPromoDb;
import com.mservice.momo.data.discount50percent.RollBack50PerPromoObj;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.octoberpromo.OctoberPromoManageDb;
import com.mservice.momo.data.octoberpromo.OctoberPromoObj;
import com.mservice.momo.data.octoberpromo.OctoberPromoUserManageDb;
import com.mservice.momo.data.promotion.*;
import com.mservice.momo.data.referral.ReferralV1CodeInputDb;
import com.mservice.momo.data.zalo.ZaloTetPromotionObj;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.models.QueuedGift;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.promotion.AcquireBinhTanUserProcess;
import com.mservice.momo.vertx.processor.promotion.StandardCharterBankPromotionProcess;
import com.mservice.momo.vertx.processor.referral.ReferralProcess;
import com.mservice.momo.vertx.promotion_server.PromotionObj;
import com.mservice.visa.entity.VisaResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 10/15/15.
 * This class used to control promotion program.
 */
public class PromotionProcess {

    protected Vertx vertx;
    protected Logger logger;
    protected JsonObject glbConfig;
    protected PhonesDb phonesDb;
    protected boolean isStoreApp;
    protected ErrorPromotionTrackingDb errorPromotionTrackingDb;
    protected Card card;
    protected CashBackDb cashBackDb;
    protected AcquireBinhTanUserProcess acquireBinhTanUserProcess;
    protected JsonArray listActivePromotions = null;
    protected MappingWalletBankDb mappingWalletBankDb;
    JsonObject jsonOctoberProgram;
    boolean isOctoberProgramActive;
    TransDb transDb;
    OctoberPromoManageDb octoberPromoManageDb;
    OctoberPromoUserManageDb octoberPromoUserManageDb;
    JsonArray jsonArrayMainNumber;
    JsonObject jsonRollBack50PercentPromo;
    RollBack50PerPromoDb rollBack50PerPromoDb;
    PhoneCheckDb phoneCheckDb;
    HashMap<String, String> hashMapData = null;
    Queue<String> numberQueue = new ArrayDeque<>();
    StandardCharterBankPromotionProcess standardCharterBankPromotionProcess;
    private DeviceDataUserDb deviceDataUserDb;
    private ReferralV1CodeInputDb referralV1CodeInputDb;
    private WomanNationalTableDb womanNationalTableDb;
    private PromotionDb promotionDb;
    private AutoNotiCountDb autoNotiCountDb;
    private boolean isUAT;
    public PromotionProcess(Vertx vertx, Logger logger, JsonObject glbCfg)
    {
        this.vertx = vertx;
        this.logger = logger;
        this.glbConfig = glbCfg;
        phonesDb = new PhonesDb(vertx.eventBus(), logger);

        jsonOctoberProgram = glbCfg.getObject(StringConstUtil.OctoberPromoProgram.JSON_OBJECT, new JsonObject());
        isOctoberProgramActive = jsonOctoberProgram.getBoolean(StringConstUtil.OctoberPromoProgram.IS_ACTIVE, false);
        jsonArrayMainNumber = jsonOctoberProgram.getArray(StringConstUtil.OctoberPromoProgram.LIST_MAIN_NUMBER, new JsonArray());
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbConfig);
        octoberPromoUserManageDb = new OctoberPromoUserManageDb(vertx, logger);
        octoberPromoManageDb = new OctoberPromoManageDb(vertx, logger);
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        jsonRollBack50PercentPromo = glbCfg.getObject(StringConstUtil.RollBack50Percent.JSON_OBJECT, new JsonObject());
        mappingWalletBankDb = new MappingWalletBankDb(vertx, logger);
        rollBack50PerPromoDb = new RollBack50PerPromoDb(vertx, logger);
        phoneCheckDb = new PhoneCheckDb(vertx, logger);
        hashMapData = new HashMap<>();
        card = new Card(vertx.eventBus(), logger);
        cashBackDb = new CashBackDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        deviceDataUserDb = new DeviceDataUserDb(vertx, logger);
        referralV1CodeInputDb = new ReferralV1CodeInputDb(vertx, logger);
        promotionDb = new PromotionDb(vertx.eventBus(), logger);

        womanNationalTableDb = new WomanNationalTableDb(vertx, logger);

        autoNotiCountDb = new AutoNotiCountDb(vertx, logger);
        isUAT = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
    }

    // TODO: 29/06/2016

    /**
     *
     * @param keys
     * @param values
     * @param request
     */
    public void prepareGetUserInfoToCheckPromoProgram(List<String> keys, JsonObject values, HttpServerRequest request) {
        if(keys.size() < 9) {
            request.response().end();
            return;
        }
        //final String senderNumber, final String phoneNumber, final PhonesDb.Obj phoneObj,
        // final long tranId, final int tranType, final long amount, final String program, final SockData data
        // final JsonObject jsonExtra

        String phoneNumber = values.getString("phoneNumber","");
        int tranType = values.getInteger("tranType",0);
        JsonObject data = values.getObject("sockData");
        SockData sockData = null;
        if(data != null) {
            sockData = new SockData(vertx, logger, glbConfig);
            sockData.fromJson(data);
        }
        JsonObject jsonExtra = values.getObject("jsonExtra",new JsonObject());

        String senderNumber = jsonExtra.getString("senderNumber", "");
        PhonesDb.Obj phoneObj = jsonExtra.getObject("phoneObj") == null? null:new PhonesDb.Obj(jsonExtra.getObject("phoneObj"));
        long tranId = jsonExtra.getLong("tranId",0);
        long amount = jsonExtra.getLong("amount",0);
        String program = jsonExtra.getString("program","");
        getUserInfoToCheckPromoProgram(senderNumber, phoneNumber, phoneObj, tranId, tranType, amount, program, sockData, jsonExtra);
        responseJson(request.response(), new JsonObject());
    }
    /*****************************************************************************/

    /**
     *
     * @param senderNumber
     * @param phoneNumber
     * @param phoneObj
     * @param tranId
     * @param tranType
     * @param amount
     * @param program
     * @param data
     * @param jsonExtra
     */
    public void getUserInfoToCheckPromoProgram(final String senderNumber, final String phoneNumber, final PhonesDb.Obj phoneObj, final long tranId, final int tranType, final long amount, final String program, final SockData data
            , final JsonObject jsonExtra)
    {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(phoneNumber);
        log.add("class", "PromotionProcess");
        log.add("func", "getUserInfoToCheckPromoProgram");
        log.add("program", program);
        final String serviceId = jsonExtra.getString(StringConstUtil.RollBack50Percent.SERVICE_ID, "");
        final String cardCheckSum = jsonExtra.getString(StringConstUtil.RollBack50Percent.CARD_CHECK_SUM, "");
        log.add("serviceId", serviceId);
        hashMapData.put(phoneNumber, "PromotionProcess");
        numberQueue.add(phoneNumber);
        if("".equalsIgnoreCase(phoneNumber) && phoneObj == null)
        {
            log.add("desc", "So dien thoai khong co");
            log.writeLog();
            return;
        }
        //Kiem tra co phai nguon bank lien ket khong
        final JsonArray jsonArrayBank = jsonRollBack50PercentPromo.getArray(StringConstUtil.RollBack50Percent.BANK_CODES, new JsonArray());
        final JsonArray jsonArrayBonusBank = jsonRollBack50PercentPromo.getArray(StringConstUtil.RollBack50Percent.BONUS_BANK_CODES, new JsonArray());
        final long vcb_start_time = jsonRollBack50PercentPromo.getLong(StringConstUtil.RollBack50Percent.VCB_START_TIME, 0);

        if(phoneObj == null)
        {
            phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(final PhonesDb.Obj phObj) {
                    //Kiem tra chuong trinh thang 10
                    if(phObj == null)
                    {
                        log.add("desc", "Khong co thong tin user");
                        log.writeLog();
                        return;
                    }
                    if(StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO.equalsIgnoreCase(program))
                    {
                        checkOctoberPromotionProgram(senderNumber, phoneNumber, phObj, tranId, tranType, amount, program, log);
                    }
                    else if(StringConstUtil.RollBack50Percent.ROLLBACK_PROMO.equalsIgnoreCase(program))
                    {

                        if(jsonArrayBank.contains(data.bank_code))
                        {
                            //Co nam trong nhom ngan hang cho phep tra thuong
                            log.add("bankcode", data.bank_code);
                            checkRollback50PercentPromotionProgram(phoneNumber, tranId, phObj, amount, data, serviceId, log, phObj.bankPersonalId);
                            return;
                        }
                        else if(jsonArrayBonusBank.contains(data.bank_code))
                        {
                            checkWalletMapping(phObj, cardCheckSum, data, vcb_start_time, log, phoneNumber, tranId, amount, serviceId);
                            return;
                        }
                        log.add("bankcode", data.bank_code);
                        log.add("desc", "Khong co bank code trong chuong trinh tra thuong");
                        log.writeLog();
                        return;
                    }
                    else if(StringConstUtil.WomanNationalField.PROGRAM.equalsIgnoreCase(program))
                    {
                        // Chay chuong trinh 8/3
                        log.add("desc "  + StringConstUtil.WomanNationalField.PROGRAM, "requestWomanNational2016Promo");
                        log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "requestWomanNational2016Promo");
                        vertx.setTimer(1000L, new Handler<Long>() {
                            @Override
                            public void handle(Long event) {
                                log.add("desc hashmap " + StringConstUtil.WomanNationalField.PROGRAM, hashMapData.toString());
                                String number = numberQueue.poll();
                                if(hashMapData.containsKey(number))
                                {
                                    hashMapData.remove(phoneNumber);
                                    log.add("desc hashmap " + StringConstUtil.WomanNationalField.PROGRAM,"requestWomanNational2016Promo");
                                    jsonExtra.putNumber(StringConstUtil.TRANDB_TRAN_TYPE, tranType);
                                    jsonExtra.putNumber(StringConstUtil.AMOUNT, amount);
                                    requestWomanNational2016Promo(phoneNumber, program, cardCheckSum, phObj, tranId, data, jsonExtra);
                                }
                                log.writeLog();
                            }
                        });
//                        log.writeLog();
                    } else if ("ECHO".equalsIgnoreCase(program)) {
                        //App gui lenh echo => update vo Binh Tan.
                        excuteAcquireBinhTanUserPromotion(phoneNumber, log, null, data, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.ECHO, jsonExtra);
                    }
                    else if (StringConstUtil.GET_OTP.equalsIgnoreCase(program)) {
                        //App gui lenh echo => update vo Binh Tan.
                        log.add("desc" + StringConstUtil.BinhTanPromotion.PROGRAM, "GET OTP");
                        excuteAcquireBinhTanUserPromotion(phoneNumber, log, null, data, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.GET_OTP, jsonExtra);
                    }
                }
            });
            return;
        }

        //Kiem tra chuong trinh khuyen mai thang 10
        if(StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO.equalsIgnoreCase(program))
        {
            checkOctoberPromotionProgram(senderNumber, phoneNumber, phoneObj, tranId, tranType, amount, program, log);
            return;
        }
        else if(StringConstUtil.RollBack50Percent.ROLLBACK_PROMO.equalsIgnoreCase(program))
        {
            //Kiem tra co phai nguon bank lien ket khong
            if(jsonArrayBank.contains(data.bank_code))
            {
                //Co nam trong nhom ngan hang cho phep tra thuong
                log.add("bankcode", data.bank_code);
                log.add("desc", "nam trong nhom ngan hang duoc tra thuong");
                checkRollback50PercentPromotionProgram(phoneNumber, tranId, phoneObj, amount, data, serviceId, log, phoneObj.bankPersonalId);
                return;
            }
            else if(jsonArrayBonusBank.contains(data.bank_code))
            {
                //Kiem tra co map vi trong thoi gian khuyen mai khong va do co phai la lan dau tien khong.
                checkWalletMapping(phoneObj, cardCheckSum, data, vcb_start_time, log, phoneNumber, tranId, amount, serviceId);
                return;
            }
            log.add("bankcode", data.bank_code);
            log.add("desc", "Khong co bank code trong chuong trinh tra thuong");
            log.writeLog();
            return;

        }
        else if(StringConstUtil.WomanNationalField.PROGRAM.equalsIgnoreCase(program) && !isStoreApp)
        {
            // Chay chuong trinh 8/3
            log.add("desc", "requestWomanNational2016Promo");
            log.add("program", program);
            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "requestWomanNational2016Promo");
            vertx.setTimer(1500L, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    log.add("desc hashmap " + StringConstUtil.WomanNationalField.PROGRAM, hashMapData.toString());
                    if(hashMapData.containsKey(phoneNumber))
                    {
                        hashMapData.remove(phoneNumber);
                        log.add("desc hashmap " + StringConstUtil.WomanNationalField.PROGRAM,"requestWomanNational2016Promo");
                        jsonExtra.putNumber(StringConstUtil.TRANDB_TRAN_TYPE, tranType);
                        jsonExtra.putNumber(StringConstUtil.AMOUNT, amount);
                        requestWomanNational2016Promo(phoneNumber, program, cardCheckSum, phoneObj, tranId, data, jsonExtra);
                    }
                    log.writeLog();

                }
            });
            log.writeLog();
            return;
        } else if ("ECHO".equalsIgnoreCase(program)) {

        }
    }

    private void requestWomanNational2016Promo(final String phoneNumber, String program, final String cardCheckSum, final PhonesDb.Obj phoneObj, final long tranId
            , final SockData data,final JsonObject joExtra) {
//        phoneCheckDb.findOne(phoneNumber, program, new Handler<PhoneCheckDb.Obj>() {
//            @Override
//            public void handle(PhoneCheckDb.Obj phoneCheckObj) {
//                if(phoneCheckObj != null)
//                {
//                    //Tra thuong 8.3
//                    String cardId = "".equalsIgnoreCase(cardCheckSum) ? phoneObj.bankPersonalId : cardCheckSum;
//                    WomanNational2016Obj.requestWomanNational2016Promo(vertx, phoneNumber, tranId, cardId, phoneObj.bank_code, new Handler<JsonObject>() {
//                            @Override
//                            public void handle(JsonObject jsonResponse) {
//
//                            }
//                        });
//                }
//                else
//                {
//                    logger.info("Khong co thong tin so dien thoai trong bang check phone, so dien thoai khong duoc tra thuong");
//                }
//            }
//        });
        //Tra thuong 8.3
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneObj.number);
        final String cardId = "".equalsIgnoreCase(cardCheckSum) ? phoneObj.bankPersonalId : cardCheckSum;
        joExtra.putObject(StringConstUtil.WomanNationalField.PHONE_OBJ, phoneObj.toJsonObject());
        referralV1CodeInputDb.findOne("0" + phoneObj.number, new Handler<ReferralV1CodeInputDb.Obj>() {
            @Override
            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                if(referralObj == null)
                {
                    WomanNational2016Obj.requestWomanNational2016Promo(vertx, phoneNumber, tranId, cardId, phoneObj.bank_code, joExtra, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonResponse) {
                            int err = jsonResponse.getInteger(StringConstUtil.ERROR, -1);
                            String desc = jsonResponse.getString(StringConstUtil.DESCRIPTION, "ERROR");
                            String deviceInfo = jsonResponse.getString(StringConstUtil.DEVICE_IMEI, "");
                            if(err != 0)
                            {
                                log.add("err " + StringConstUtil.WomanNationalField.PROGRAM, err);
                                log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, desc);
                                log.add("deviceInfo " + StringConstUtil.WomanNationalField.PROGRAM, deviceInfo);
                                log.writeLog();
                                if("".equalsIgnoreCase(deviceInfo))
                                {
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, err, desc);
                                }
                                else
                                {
                                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, deviceInfo).putString(StringConstUtil.DESCRIPTION, desc);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.WomanNationalField.PROGRAM, err, joDesc.toString());
                                }
                            }
                        }
                    });
                }
                else {
                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "So dien thoai " + phoneNumber + " da tham gia gioi thieu ban be");
                    log.writeLog();
                }
            }
        });

    }

    // TODO: 28/06/2016

    /**
     * chuan bi truoc du lieu truoc khi xu ly promotion
     *
     * @param keys
     * @param values
     * @param request
     */
    public void prepareGetUserInfoToCheckPromoProgramWithCallback(List<String> keys, JsonObject values, HttpServerRequest request) {

        String phoneNumber = values.getString("phoneNumber", "");
        int tranType = values.getInteger("tranType", 0);
        SockData sockData = new SockData(vertx, logger, glbConfig);
        sockData.fromJson(values.getObject("sockData", new JsonObject()));
        JsonObject jsonExtra = values.getObject("jsonExtra", new JsonObject());
        String senderNumber = jsonExtra.getString("senderNumber", "");
        PhonesDb.Obj phoneObj = jsonExtra.getObject("phoneObj") == null? null:new PhonesDb.Obj(jsonExtra.getObject("phoneObj"));
        long tranId = jsonExtra.getLong("tranId", 0);
        long amount = jsonExtra.getLong("amount", 0);
        String program = jsonExtra.getString("program", "");
        //call target method
        getUserInfoToCheckPromoProgramWithCallback(senderNumber, phoneNumber, phoneObj, tranId, tranType,
                amount, program, sockData, jsonExtra, request);

    }

    // TODO: 28/06/2016
    /**
     * Kiem tra chuong trinh khuyen mai co phu hop voi code user nhap vao hay khong
     *
     * @param senderNumber
     * @param phoneNumber
     * @param phoneObj
     * @param tranId
     * @param tranType
     * @param amount
     * @param program
     * @param jsonExtra
     */
    public void getUserInfoToCheckPromoProgramWithCallback(final String senderNumber, final String phoneNumber, final PhonesDb.Obj phoneObj,
                                                           final long tranId, final int tranType, final long amount, final String program,
                                                           final SockData data, final JsonObject jsonExtra, final HttpServerRequest request) {
        final JsonObject joReply = new JsonObject();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(phoneNumber);
        log.add("class", "PromotionProcess");
        log.add("func", "getUserInfoToCheckPromoProgram");
        final String serviceId = jsonExtra.getString(StringConstUtil.RollBack50Percent.SERVICE_ID, "");
        final String cardCheckSum = jsonExtra.getString(StringConstUtil.RollBack50Percent.CARD_CHECK_SUM, "");
        log.add("serviceId", serviceId);


        if("".equalsIgnoreCase(phoneNumber) && phoneObj == null)
        {
            log.add("desc", "So dien thoai khong co");
            log.writeLog();
            return;
        }
        //Kiem tra co phai nguon bank lien ket khong
        final JsonArray jsonArrayBank = jsonRollBack50PercentPromo.getArray(StringConstUtil.RollBack50Percent.BANK_CODES, new JsonArray());
        final JsonArray jsonArrayBonusBank = jsonRollBack50PercentPromo.getArray(StringConstUtil.RollBack50Percent.BONUS_BANK_CODES, new JsonArray());
        final long vcb_start_time = jsonRollBack50PercentPromo.getLong(StringConstUtil.RollBack50Percent.VCB_START_TIME, 0);

        if(phoneObj == null)
        {
            phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(final PhonesDb.Obj phObj) {
                    //Kiem tra chuong trinh thang 10
                    if(phObj == null)
                    {
                        log.add("desc", "Khong co thong tin user");
                        log.writeLog();
                        return;
                    }
                    if(StringConstUtil.ZaloPromo.ZALO_PROGRAM.equalsIgnoreCase(program))
                    {
                        log.add("desc", "zalo promotion flow");
                        requestZaloPromotion(phoneNumber, jsonExtra, log, request);
                        return;
                    }
                    else if(StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM.equalsIgnoreCase(program)){
                        requestClaimCodePromotion(phObj, phoneNumber, jsonExtra, log, request);
                        return;
                    }
                    else if(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM.equalsIgnoreCase(program))
                    {
                        log.add("desc", "So dien thoai " + phObj.number + " tham gia chuong trinh Referral");
                        requestReferralV1Promotion(phObj, phoneNumber, jsonExtra, log, request);
                        return;
                    }
                    else {
                        log.add("desc" , "So dien thoai " + phObj.number + " khong duoc tham gia chuong trinh nao");
                        joReply.putNumber(StringConstUtil.ERROR, -1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                        responseJson(request.response(), joReply);
                        return;
                    }

                }
            });
            return;
        }

        if(StringConstUtil.ZaloPromo.ZALO_PROGRAM.equalsIgnoreCase(program))
        {
            log.add("desc", "check zalo promo program");
            requestZaloPromotion(phoneNumber, jsonExtra, log, request);
            return;
        }
        else if(StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM.equalsIgnoreCase(program)){
            requestClaimCodePromotion(phoneObj, phoneNumber, jsonExtra, log, request);
            return;
        }
        else if(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM.equalsIgnoreCase(program))
        {
            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai " + phoneObj.number + " tham gia chuong trinh Referral");
            requestReferralV1Promotion(phoneObj, phoneNumber, jsonExtra, log, request);
            return;
        }
        else {
            log.add("desc", "So dien thoai " + phoneObj.number + " khong duoc tham gia chuong trinh nao");
            joReply.putNumber(StringConstUtil.ERROR, -1000);
            joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
            responseJson(request.response(), joReply);
            return;
        }
    }
    /***********************************************************************************/

    /**
     * (old version khong su dung nua)
     * Kiem tra chuong trinh khuyen mai co phu hop voi code user nhap vao hay khong
     *
     * @param senderNumber
     * @param phoneNumber
     * @param phoneObj
     * @param tranId
     * @param tranType
     * @param amount
     * @param program
     * @param data
     * @param jsonExtra
     * @param callback
     */
    public void getUserInfoToCheckPromoProgramWithCallback(final String senderNumber, final String phoneNumber, final PhonesDb.Obj phoneObj, final long tranId, final int tranType, final long amount, final String program, final SockData data
            , final JsonObject jsonExtra,final Handler<JsonObject> callback)
    {
        final JsonObject joReply = new JsonObject();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(phoneNumber);
        log.add("class", "PromotionProcess");
        log.add("func", "getUserInfoToCheckPromoProgram");
        final String serviceId = jsonExtra.getString(StringConstUtil.RollBack50Percent.SERVICE_ID, "");
        final String cardCheckSum = jsonExtra.getString(StringConstUtil.RollBack50Percent.CARD_CHECK_SUM, "");
        log.add("serviceId", serviceId);


        if("".equalsIgnoreCase(phoneNumber) && phoneObj == null)
        {
            log.add("desc", "So dien thoai khong co");
            log.writeLog();
            return;
        }
        //Kiem tra co phai nguon bank lien ket khong
        final JsonArray jsonArrayBank = jsonRollBack50PercentPromo.getArray(StringConstUtil.RollBack50Percent.BANK_CODES, new JsonArray());
        final JsonArray jsonArrayBonusBank = jsonRollBack50PercentPromo.getArray(StringConstUtil.RollBack50Percent.BONUS_BANK_CODES, new JsonArray());
        final long vcb_start_time = jsonRollBack50PercentPromo.getLong(StringConstUtil.RollBack50Percent.VCB_START_TIME, 0);

        if(phoneObj == null)
        {
            phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(final PhonesDb.Obj phObj) {
                    //Kiem tra chuong trinh thang 10
                    if(phObj == null)
                    {
                        log.add("desc", "Khong co thong tin user");
                        log.writeLog();
                        return;
                    }
                    if(StringConstUtil.ZaloPromo.ZALO_PROGRAM.equalsIgnoreCase(program))
                    {
                        log.add("desc", "zalo promotion flow");
                        requestZaloPromotion(phoneNumber, jsonExtra, log, callback);
                        return;
                    }
                    else if(StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM.equalsIgnoreCase(program)){
                        requestClaimCodePromotion(phObj, phoneNumber, jsonExtra, log, callback);
                        return;
                    }
                    else if(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM.equalsIgnoreCase(program))
                    {
                        log.add("desc", "So dien thoai " + phObj.number + " tham gia chuong trinh Referral");
                        requestReferralV1Promotion(phObj, phoneNumber, jsonExtra, log, callback);
                        return;
                    }
                    else {
                        log.add("desc" , "So dien thoai " + phObj.number + " khong duoc tham gia chuong trinh nao");
                        joReply.putNumber(StringConstUtil.ERROR, -1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                        callback.handle(joReply);
                        return;
                    }

                }
            });
            return;
        }

        if(StringConstUtil.ZaloPromo.ZALO_PROGRAM.equalsIgnoreCase(program))
        {
            log.add("desc", "check zalo promo program");
            requestZaloPromotion(phoneNumber, jsonExtra, log, callback);
            return;
        }
        else if(StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM.equalsIgnoreCase(program)){
            requestClaimCodePromotion(phoneObj, phoneNumber, jsonExtra, log, callback);
            return;
        }
        else if(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM.equalsIgnoreCase(program))
        {
            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai " + phoneObj.number + " tham gia chuong trinh Referral");
            requestReferralV1Promotion(phoneObj, phoneNumber, jsonExtra, log, callback);
            return;
        }
        else {
            log.add("desc", "So dien thoai " + phoneObj.number + " khong duoc tham gia chuong trinh nao");
            joReply.putNumber(StringConstUtil.ERROR, -1000);
            joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
            callback.handle(joReply);
            return;
        }
    }

    // TODO: 28/06/2016
    private void requestZaloPromotion(String phoneNumber, JsonObject jsonExtra, Common.BuildLog log, final HttpServerRequest request) {
        String zaloCode = jsonExtra.getString(colName.ZaloTetPromotionCol.ZALO_CODE, "");
        String imei = jsonExtra.getString(colName.ZaloTetPromotionCol.DEVICE_IMEI, "");
        boolean isValidZaloCode = checkZaloCode(zaloCode, log);

        JsonObject joReply = new JsonObject();
        if(!isValidZaloCode)
        {
            log.add("desc", "Ma code nhap bay ba");
            joReply.putNumber(StringConstUtil.ERROR, -1000);
            //callback.handle(joReply);
            responseJson(request.response(), joReply);
            log.writeLog();
            return;
        }

        ZaloTetPromotionObj.requestZaloPromo(vertx, phoneNumber, zaloCode, StringConstUtil.ZaloPromo.ZALO_PROGRAM, imei, 0, 0, "", 0, 0, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joRes) {
                //callback.handle(joRes);
                responseJson(request.response(), joRes);
            }
        });
    }
    /******************************************************

     /**
     *
     * @param phoneNumber
     * @param jsonExtra
     * @param log
     * @param callback
     */
    private void requestZaloPromotion(String phoneNumber, JsonObject jsonExtra, Common.BuildLog log, final Handler<JsonObject> callback) {
        String zaloCode = jsonExtra.getString(colName.ZaloTetPromotionCol.ZALO_CODE, "");
        String imei = jsonExtra.getString(colName.ZaloTetPromotionCol.DEVICE_IMEI, "");
        boolean isValidZaloCode = checkZaloCode(zaloCode, log);

        JsonObject joReply = new JsonObject();
        if(!isValidZaloCode)
        {
            log.add("desc", "Ma code nhap bay ba");
            joReply.putNumber(StringConstUtil.ERROR, -1000);
            callback.handle(joReply);
            log.writeLog();
            return;
        }

        ZaloTetPromotionObj.requestZaloPromo(vertx, phoneNumber, zaloCode, StringConstUtil.ZaloPromo.ZALO_PROGRAM, imei, 0, 0, "", 0, 0, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joRes) {
                callback.handle(joRes);
            }
        });
    }

    // TODO: 28/06/2016
    /**
     * Chuong trinh nhan thuong bang cach nhap ma code
     *
     * @param phObj
     * @param phoneNumber
     * @param jsonExtra
     * @param log
     * @param request
     */
    private void requestClaimCodePromotion(PhonesDb.Obj phObj, String phoneNumber, JsonObject jsonExtra, Common.BuildLog log, final HttpServerRequest request) {
        String claimedCode = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.CODE, "");
        String imei = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.DEVICE_IMEI, "");
        boolean isValidCode = checkClaimedCode(claimedCode, log);

        JsonObject joReply = new JsonObject();
        if(!isValidCode)
        {
            log.add("desc", "Ma code nhap bay ba");
            joReply.putNumber(StringConstUtil.ERROR, -1000);
            responseJson(request.response(), joReply);
            log.writeLog();
            return;
        }
        ClaimCodePromotionObj.requestClaimedCodePromo(vertx, phoneNumber, claimedCode, StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM, imei, 0, 0, "", phObj.createdDate, new JsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joRes) {
                responseJson(request.response(), joRes);
            }
        });

    }
    /************************************************************/

    /**
     * Chuong trinh nhan thuong bang cach nhap ma code
     *
     * @param phObj
     * @param phoneNumber
     * @param jsonExtra
     * @param log
     * @param callback
     */
    private void requestClaimCodePromotion(PhonesDb.Obj phObj, String phoneNumber, JsonObject jsonExtra, Common.BuildLog log, final Handler<JsonObject> callback) {
        String claimedCode = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.CODE, "");
        String imei = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.DEVICE_IMEI, "");
        boolean isValidCode = checkClaimedCode(claimedCode, log);

        JsonObject joReply = new JsonObject();
        if(!isValidCode)
        {
            log.add("desc", "Ma code nhap bay ba");
            joReply.putNumber(StringConstUtil.ERROR, -1000);
            callback.handle(joReply);
            log.writeLog();
            return;
        }
        ClaimCodePromotionObj.requestClaimedCodePromo(vertx, phoneNumber, claimedCode, StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM, imei, 0, 0, "", phObj.createdDate, new JsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joRes) {
                callback.handle(joRes);
            }
        });

    }

    // TODO: 28/06/2016
    private void requestReferralV1Promotion(final PhonesDb.Obj phObj, final String phoneNumber, final JsonObject jsonExtra, final Common.BuildLog log, final HttpServerRequest request)
    {
        final String claimedCode = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.CODE, "");
        final String imei = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.DEVICE_IMEI, "");
        log.add("code"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, claimedCode);
        log.add("imei"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, imei);
        JsonObject joExtra = new JsonObject();
        if(phObj != null)
        {
            joExtra.putObject(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ, phObj.toJsonObject());
        }
        womanNationalTableDb.findOne(phoneNumber, new Handler<WomanNationalTableDb.Obj>() {
            @Override
            public void handle(WomanNationalTableDb.Obj lktkObj) {
                if (lktkObj == null) {
                    ClaimCodePromotionObj.requestClaimedCodePromo(vertx, phoneNumber, claimedCode, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, imei, 0, 0, "", phObj.createdDate, jsonExtra, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joRes) {
                            responseJson(request.response(), joRes);
                        }
                    });
                } else {
                    log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "khong duoc tham gia gioi thieu ban be vi da tham gia lien ket tai khoan");
                    log.writeLog();
                    responseJson(request.response(), new JsonObject().putNumber(StringConstUtil.ERROR, -1000).putString(StringConstUtil.DESCRIPTION, "Bạn đã tham gia chương trình khuyến mãi \"Liên kết tài khoản\" nên không được ghi nhận để tham gia chương trình này. Vui lòng gọi 1900 5454 41 để được hỗ trợ."));
                }
            }
        });
    }

    /***************************************************************************/

    /**
     *
     * @param phObj
     * @param phoneNumber
     * @param jsonExtra
     * @param log
     * @param callback
     */
    private void requestReferralV1Promotion(final PhonesDb.Obj phObj, final String phoneNumber, final JsonObject jsonExtra, final Common.BuildLog log, final Handler<JsonObject> callback) {
        final String claimedCode = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.CODE, "");
        final String imei = jsonExtra.getString(StringConstUtil.ClaimCodePromotion.DEVICE_IMEI, "");
        log.add("code" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, claimedCode);
        log.add("imei" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, imei);
        if (phObj != null) {
            jsonExtra.putObject(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ, phObj.toJsonObject());
        }
        womanNationalTableDb.findOne(phoneNumber, new Handler<WomanNationalTableDb.Obj>() {
            @Override
            public void handle(WomanNationalTableDb.Obj lktkObj) {
                if (lktkObj == null) {
                    ClaimCodePromotionObj.requestClaimedCodePromo(vertx, phoneNumber, claimedCode, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, imei, 0, 0, "", phObj.createdDate, jsonExtra, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joRes) {
                            callback.handle(joRes);
                        }
                    });
                } else {
                    log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "khong duoc tham gia gioi thieu ban be vi da tham gia lien ket tai khoan");
                    log.writeLog();
                    callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, -1000).putString(StringConstUtil.DESCRIPTION, "Bạn đã tham gia chương trình khuyến mãi \"Liên kết tài khoản\" nên không được ghi nhận để tham gia chương trình này. Vui lòng gọi 1900 5454 41 để được hỗ trợ."));
                }
            }
        });
    }

    private boolean checkZaloCode(String zaloCode, Common.BuildLog log)
    {
        log.add("desc", "check zalo code method");
        int divided = 0;
        int sum = 0;
        if(zaloCode.length() < 7)
        {
            return false;
        }
        for(int i = 1; i < zaloCode.length() - 1; i++)
        {
            if(i == 1 || i == 2)
            {
                divided = divided + Integer.parseInt(zaloCode.charAt(i) + "");
            }
            sum = sum + Integer.parseInt(zaloCode.charAt(i) + "");
        }
        divided = divided > 9 ? 9 : divided;
        int endNumber = Integer.parseInt(zaloCode.charAt(zaloCode.length() - 1) + "");
        log.add("endnumber", endNumber);
        log.add("zalo code", zaloCode);
        log.add("sum", sum);
        log.add("divided", divided);

        if(sum % divided == endNumber)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean checkClaimedCode(String claimedCode, Common.BuildLog log)
    {
        log.add("desc", "check zalo code method");
        int divided = 0;
        int sum = 0;
        int valueOfCode = DataUtil.strToInt(claimedCode);

        if(valueOfCode > 0 && claimedCode.length() < 7)
        {
            return false;
        }
        else if(valueOfCode == 0)
        {
            return true;
        }
        for(int i = 2; i < claimedCode.length() - 1; i++)
        {
            if(i == 2 || i == 3)
            {
                divided = divided + DataUtil.strToInt(claimedCode.charAt(i) + "");
            }
            sum = sum + DataUtil.strToInt(claimedCode.charAt(i) + "");
        }
        divided = divided > 9 ? 9 : divided;
        int endNumber = DataUtil.strToInt(claimedCode.charAt(claimedCode.length() - 1) + "");
        log.add("endnumber", endNumber);
        log.add("claimed code", claimedCode);
        log.add("sum", sum);
        log.add("divided", divided);
        if(divided == 0)
        {
            return false;
        }
        else if(sum % divided == endNumber)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private void checkWalletMapping(final PhonesDb.Obj phObj, String cardCheckSum, final SockData data, final long vcb_start_time, final Common.BuildLog log, final String phoneNumber, final long tranId, final long amount, final String serviceId) {
        //todo Thay doi du lieu ham
        //Kiem tra co map vi trong thoi gian khuyen mai khong va do co phai la lan dau tien khong.
//        final String personalNumber = "".equalsIgnoreCase(cardCheckSum) ? phObj.bankPersonalId : cardCheckSum;
//        String id = personalNumber + data.bank_code;
//        log.add("personalNumber", id);
//        mappingWalletBankDb.findOne(id, new Handler<MappingWalletBankDb.Obj>() {
//            @Override
//            public void handle(MappingWalletBankDb.Obj mappingWalletObj) {
//                if(mappingWalletObj != null && mappingWalletObj.number_of_mapping == 1 && mappingWalletObj.mapping_time > vcb_start_time && !"sbs".equalsIgnoreCase(mappingWalletObj.bank_name))
//                {
//                    log.add("bankcode", data.bank_code);
//                    log.add("desc", "Thoa dieu kien map vi, tra thuong cho em no");
//                    checkRollback50PercentPromotionProgram(phoneNumber, tranId, phObj, amount, data, serviceId, log, personalNumber);
//                    return;
//                }
//                else if(mappingWalletObj != null && mappingWalletObj.number_of_mapping == 0 && mappingWalletObj.mapping_time > vcb_start_time && "sbs".equalsIgnoreCase(mappingWalletObj.bank_name))
//                {
//                    log.add("bankcode", data.bank_code);
//                    log.add("desc", "Thoa dieu kien map vi visa, tra thuong cho em no");
//                    checkRollback50PercentPromotionProgram(phoneNumber, tranId, phObj, amount, data, serviceId, log, personalNumber);
//                    return;
//                }
//                else
//                {
//                    //Kiem tra xem no da duoc nhan qua chua, neu co roi thi cho tiep
//                    rollBack50PerPromoDb.searchPersonalDataList(personalNumber, phoneNumber, new Handler<ArrayList<RollBack50PerPromoDb.Obj>>() {
//                        @Override
//                        public void handle(ArrayList<RollBack50PerPromoDb.Obj> arrayList) {
//                              if(arrayList.size() > 0)
//                              {
//                                  checkRollback50PercentPromotionProgram(phoneNumber, tranId, phObj, amount, data, serviceId, log, personalNumber);
//                              }
//                              else {
//                                log.add("bankcode", data.bank_code);
//                                log.add("desc", "Khong co thong tin map vi nhe hoac so lan map vi khong thoa dieu kien cho phep");
//                                log.writeLog();
//                              }
//                        }
//                    });
//                    return;
//                }
//
//            }
//        });
    }

    //Bat dau kiem tra chuong trinh hoan tra 50%
    private void checkRollback50PercentPromotionProgram(final String phoneNumber, final long tranId, final PhonesDb.Obj phoneObj, final long tran_amount ,final SockData data, final String serviceId, final Common.BuildLog log, final String cmnd)
    {
        boolean isActive = jsonRollBack50PercentPromo.getBoolean(StringConstUtil.RollBack50Percent.IS_ACTIVE, false);
        if(isStoreApp) {
            log.add("desc", "La diem giao dich nen khong cho choi");
            log.writeLog();
            return;
        }
        if(!isActive)
        {
            log.add("desc", "chuong trinh chua bat dau, khong cho tham gia");
            log.writeLog();
            return;
        }

        //Kiem tra thoi gian chuong trinh co dang chay khong
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                log.add("phoneNumber", phoneNumber);

                long rollback_promo_start_date = 0;
                long rollback_promo_end_date = 0;

                long currentTime = System.currentTimeMillis();
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj october_promo = null;
                    for (Object o : array) {
                        october_promo = new PromotionDb.Obj((JsonObject) o);
                        if (october_promo.NAME.equalsIgnoreCase(StringConstUtil.RollBack50Percent.ROLLBACK_PROMO)) {
                            rollback_promo_start_date = october_promo.DATE_FROM;
                            rollback_promo_end_date = october_promo.DATE_TO;

                        } //End if get october promo info
                    } //End for get october_promo

                    //Sau khi lay duoc thong tin tu bang promo
                    log.add("rollback_start", rollback_promo_start_date);
                    log.add("rollback_end", rollback_promo_end_date);
                    log.add("current", currentTime);

                    if(currentTime < rollback_promo_end_date && currentTime > rollback_promo_start_date)
                    {
                        log.add("func", "Dang dien ra chuong trinh khuyen mai hoan tien cho user");

                        log.add("desc", "Vi " + phoneNumber + " dang ki trong thoi gian cho phep");
                        //Kiem tra thong tin khach hang co thuc hien giao dich nao chua
                        long amount = data.bank_amount;
                        long tid = data.bank_tid;
                        String bankcode = data.bank_code;

                        data.bank_amount = 0;
                        data.bank_tid = 0;
                        data.bank_code = "";
                        RollBack50PerPromoObj.requestRollBack50PerPromo(vertx, phoneNumber, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE, tranId, StringConstUtil.RollBack50Percent.ROLLBACK_PROMO, cmnd,
                                tran_amount, bankcode, amount, tid, serviceId, data.appCode, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {

                                    }
                                });
                        return;

                    }//End compare currentTime
                    else
                    {
                        log.add("func", "Chua dien ra chuong trinh khuyen mai rollback nay");
                        log.writeLog();
                        return;
                    }

                } // End if check array promo
                else{

                    log.add("func", "Khong co du lieu gi trong bang khuyen mai");
                    log.writeLog();
                    return;

                }//End else check array promo

            } //End handle requestPromoRecord

        });//End requestPromoRecord


    }

    //Bat dau kiem tra khuyen mai thang 10
    private void checkOctoberPromotionProgram(final String senderNumber, final String phoneNumber,final PhonesDb.Obj phoneObj, final long tranId, final int tranType, final long amount, final String program, final Common.BuildLog log)
    {
        log.add("func", "checkOctoberPromotionProgram");

        //Xem da active chuong trinh chua.
        if (!isOctoberProgramActive)
        {
            log.add("desc", "Chuong trinh chua duoc active");
            log.writeLog();
            return;
        }

        //Kiem tra thoi gian chuong trinh co dang chay khong
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                log.add("phoneNumber", phoneNumber);

                long october_promo_start_date = 0;
                long october_promo_end_date = 0;
                boolean enablePhase = false;

                long october_student_promo_start_date = 0;
                long october_student_promo_end_date = 0;
                long currentTime = System.currentTimeMillis();
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj october_promo = null;
                    for (Object o : array) {
                        october_promo = new PromotionDb.Obj((JsonObject) o);
                        if (october_promo.NAME.equalsIgnoreCase(StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO)) {
                            october_promo_start_date = october_promo.DATE_FROM;
                            october_promo_end_date = october_promo.DATE_TO;
                            enablePhase = october_promo.ENABLE_PHASE2;
                        } //End if get october promo info
                        else if(october_promo.NAME.equalsIgnoreCase(StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO_STUDENT)){
                            october_student_promo_start_date = october_promo.DATE_FROM;
                            october_student_promo_end_date = october_promo.DATE_TO;
                        }
                    } //End for get october_promo

                    //Sau khi lay duoc thong tin tu bang promo
                    long create_date_limit_start = jsonOctoberProgram.getLong(StringConstUtil.OctoberPromoProgram.CREATE_DATE_LIMIT_START, 0);
                    long create_date_limit_end = jsonOctoberProgram.getLong(StringConstUtil.OctoberPromoProgram.CREATE_DATE_LIMIT_END, 0);

                    log.add("october_start", october_promo_start_date);
                    log.add("october_end", october_promo_end_date);
                    log.add("current", currentTime);
                    log.add("create_date_limit_start", create_date_limit_start);
                    log.add("create_date_limit_end", create_date_limit_end);


                    //Kiem tra chuong trinh sinh vien thang 10 truoc
                    boolean checkOctoberPromoStudent = checkOctoberPromoStudent(senderNumber, log);

                    if(!"".equalsIgnoreCase(senderNumber) && checkOctoberPromoStudent && currentTime > october_student_promo_start_date && currentTime < october_student_promo_end_date
                            && phoneObj.createdDate < october_student_promo_end_date && phoneObj.createdDate > october_student_promo_start_date && amount > 49999)
                    {
                        log.add("desc", "Vi " + phoneNumber + " duoc tang khuyen mai tu chuong trinh October student tu so " + senderNumber);
                        //Kiem tra thong tin khach hang co thuc hien giao dich nao chua
                        processNotingOctoberPromotionProgramUser(phoneNumber, phoneObj, false, tranId, tranType, amount, program, log);
                        return;
                    }
                    //Kiem tra tiep xem co dang trong chuong trinh khuyen mai khong.
                    else if(currentTime < october_promo_end_date && currentTime > october_promo_start_date)
                    {
                        log.add("func", "Dang dien ra chuong trinh khuyen mai");
                        //Kiem tra ngay khoi tao cua khach hang co truoc 1/10 va tren 1/1 khong
                        if(phoneObj.createdDate > create_date_limit_start && phoneObj.createdDate < create_date_limit_end && amount > 49999)
                        {
                            log.add("desc", "Vi " + phoneNumber + " dang ki trong thoi gian cho phep");
                            //Kiem tra thong tin khach hang co thuc hien giao dich nao chua
                            processNotingOctoberPromotionProgramUser(phoneNumber, phoneObj, enablePhase, tranId, tranType, amount, program, log);
                            return;
                        } //End if compare createdDate
//                        else if(phoneObj.createdDate > create_date_limit_start && phoneObj.createdDate < create_date_limit_end && amount > 499999 && enablePhase)
//                        {
//                            log.add("desc", "Vi " + phoneNumber + " dang ki trong thoi gian cho phep");
//                            //Kiem tra thong tin khach hang co thuc hien giao dich nao chua
//                            processNotingOctoberPromotionProgramUser(phoneNumber, phoneObj, enablePhase, tranId, tranType, amount, program);
//                            return;
//                        } //End if compare createdDate
                        else{
                            log.add("desc", "Vi " + phoneNumber + " dang ki ngoai thoi gian cho phep");
                            log.writeLog();
                            return;
                        }

                    }//End compare currentTime
                    else
                    {
                        log.add("func", "Chua dien ra chuong trinh khuyen mai thang 10 nay");
                        log.writeLog();
                        return;
                    }

                } // End if check array promo
                else{

                    log.add("func", "Khong co du lieu gi trong bang khuyen mai");
                    log.writeLog();
                    return;

                }//End else check array promo

            } //End handle requestPromoRecord

        });//End requestPromoRecord
    } //End CheckOctoberProgram

    private boolean checkOctoberPromoStudent(String senderNumber, final Common.BuildLog log)
    {
        boolean isBonus = false;
        log.add("list main number", jsonArrayMainNumber.toString());
        log.add("info sender", senderNumber);
        if(!"".equalsIgnoreCase(senderNumber))
        {
            int sizeMainNumberList = jsonArrayMainNumber.size();
            log.add("size of main number list", sizeMainNumberList);
            for(int i = 0; i < sizeMainNumberList; i++)
            {
                if (senderNumber.equalsIgnoreCase(jsonArrayMainNumber.get(i).toString()))
                {
                    isBonus = true;
                    break;
                }
            }

        }

        return isBonus;
    }
    //Ham nay de kiem tra nhung khach hang thoa dieu kien khi khuyen mai da mo
    private void processNotingOctoberPromotionProgramUser(final String phoneNumber, final PhonesDb.Obj phoneObj, final boolean enableLuckyProgram, final long tranId, final int tranType, final long amount, final String program, final Common.BuildLog log)
    {
        JsonObject jsonFilter = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(3);
        jsonArray.add(38);
        jsonArray.add(29);
        jsonArray.add(45);
        JsonObject jsonNIN = new JsonObject();
        jsonNIN.putArray(MongoKeyWords.NOT_IN, jsonArray);
        jsonFilter.putObject(colName.TranDBCols.TRAN_TYPE, jsonNIN);
        transDb.searchWithFilter(DataUtil.strToInt(phoneNumber), jsonFilter, new Handler<ArrayList<TranObj>>() {
            @Override
            public void handle(ArrayList<TranObj> tranObjs) {
                if(tranObjs.size() > 0 && tranId == 0 && tranType == 0)
                {
                    log.add("desc", "Vi " + phoneNumber + " da thuc hien giao dich nap tien roi nen khong duoc khuyen mai");
                    log.writeLog();
                    return;
                }
                else if(tranObjs.size() > 0 && tranType == MomoProto.TranHisV1.TranType.BANK_IN_VALUE)
                {
                    log.add("desc", "Vi " + phoneNumber + " da thuc hien " + tranObjs.size() + " giao dich nap tien roi nen khong duoc khuyen mai");
                    log.writeLog();
                    return;
                }
                else if(tranObjs.size() > 1)
                {
                    log.add("desc", "Vi " + phoneNumber + " da thuc hien " + tranObjs.size() + " giao dich nap tien roi nen khong duoc khuyen mai");
                    log.writeLog();
                    return;
                }

                log.add("desc",  "Vi " + phoneNumber + " chua thuc hien 1 giao dich nap tien nao het nen cho khuyen mai");

                //Kiem tra xem co cho choi may man khong
                boolean isLucky = false;
                long min_amount = jsonOctoberProgram.getLong(StringConstUtil.OctoberPromoProgram.MIN_LUCKY_AMOUNT, 500000);

                final int number_of_lucky = jsonOctoberProgram.getInteger(StringConstUtil.OctoberPromoProgram.LUCKY_NUMBER, 2000);

                long currentTime = System.currentTimeMillis();
                if(enableLuckyProgram && amount >= min_amount)
                {
                    long luckyNumber = currentTime % 2;
                    if(luckyNumber == 1)
                    {
                        isLucky = true;
                    }

                }
                final OctoberPromoUserManageDb.Obj octoberPromoUserObj = new OctoberPromoUserManageDb.Obj();
                octoberPromoUserObj.is_lucky_man = isLucky;
                octoberPromoUserObj.phone_number = phoneNumber;
                octoberPromoUserObj.time = currentTime;
                log.add("octoberPromoUserObj", octoberPromoUserObj.toJson());
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 1);
                long startTime = calendar.getTimeInMillis();

                calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                long endTime = calendar.getTimeInMillis();

                log.add("startTime", startTime);
                log.add("endTime", endTime);
                log.add("lucky number", number_of_lucky);
                if(!isLucky)
                {
                    insertIntoOctoberPromoUser(octoberPromoUserObj, tranId, tranType, program, amount, phoneNumber, log);
                    return;
                }
                else
                {
                    octoberPromoUserManageDb.countUserRangeTime(startTime, endTime, new Handler<JsonArray>() {
                        @Override
                        public void handle(JsonArray jsonArray) {
                            if (jsonArray != null && jsonArray.size() > 0) {
                                boolean isLuckyMan = false;
                                int count = 0;
                                int lucky_man = 0;
                                int notReceivedGift = 0;
                                for (Object o : jsonArray) {
                                    isLuckyMan = ((JsonObject) o).getBoolean("_id");
                                    count = ((JsonObject) o).getInteger("count", 0);
                                    if (isLuckyMan) {
                                        lucky_man = count;
                                    }
                                }
                                if(lucky_man < number_of_lucky)
                                {
                                    insertIntoOctoberPromoUser(octoberPromoUserObj, tranId, tranType, program, amount, phoneNumber, log);
                                    return;
                                }
                                else
                                {

                                    log.add("desc", "Het luot roi kakakaka, chuyen tu may man sang khong may man");
                                    octoberPromoUserObj.is_lucky_man = false;
                                    insertIntoOctoberPromoUser(octoberPromoUserObj, tranId, tranType, program, amount, phoneNumber, log);
                                    return;
                                }

                            }
                            else{
                                log.add("desc", "New table OctoberUserPromo");
                                insertIntoOctoberPromoUser(octoberPromoUserObj, tranId, tranType, program, amount, phoneNumber, log);
                                return;
                            }
                        }
                    });
                }
            }
        });

    }

    private void insertIntoOctoberPromoUser(OctoberPromoUserManageDb.Obj octoberPromoUserObj, final long tranId, final int tranType, final String program, final long amount, final String phoneNumber, final Common.BuildLog log) {
        octoberPromoUserManageDb.insert(octoberPromoUserObj, new Handler<Integer>() {
            @Override
            public void handle(Integer result) {
                log.add("tranId", tranId);
                log.add("tranType", tranType);
                log.add("program", program);
                log.add("amount", amount);
                if(result == 0 && tranId == 0 && tranType == 0)
                {
                    log.add("desc", "Da them thong tin khach hang " + phoneNumber + " thanh cong.");
                    log.writeLog();
                    return;
                }
                else if(result == 0 && tranId != 0 && tranType != 0 && amount > 49999)
                {
                    //Khach hang thuc hien nap tien vao vi
                    log.add("tranId", tranId);
                    log.add("tranType", tranType);
                    log.add("program", program);
                    log.add("amount", amount);
                    //Thuc hien tra qua cho em no :)
                    //Tao verticle roi goi qua
                    OctoberPromoObj.requestOctoberPromo(vertx, phoneNumber, tranType, tranId, StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, "", "", new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            log.add("desc", jsonObject.toString());
                        }
                    });
                    return;
                }
                else{
                    //Thong tin khach hang da co
                    if(tranId != 0 && tranType != 0 && amount > 49999)
                    {
                        //Khach hang thuc hien nap tien vao vi
                        log.add("tranId", tranId);
                        log.add("tranType", tranType);
                        log.add("program", program);
                        log.add("amount", amount);
                        //Thuc hien tra qua cho em no :)
                        //Tao verticle roi goi qua
                        OctoberPromoObj.requestOctoberPromo(vertx, phoneNumber, tranType, tranId, StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, "", "", new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject jsonObject) {
                                log.add("desc", jsonObject.toString());
                            }
                        });
                        return;
                    }
                    log.add("desc", "duplicate key, tinh hack ha con");
                    log.writeLog();
                    return;

                }
            }
        });
    }

    // TODO: 29/06/2016

    /**
     * prepare data before call to updateOctoberPromoVoucherStatus
     *
     * @param keys
     * @param values
     * @param request
     */
    public void prepareUpdateOctoberPromoVoucherStatus(List<String> keys, JsonObject values, HttpServerRequest request) {
//        if(keys.size() < 2 || keys.size() > 2) {
//            responseError(400, request);
//            return;
//        }
        JsonArray giftArray = values.getArray("giftArray", new JsonArray());
        String phoneNumber = values.getString("phoneNumber", "");
        updateOctoberPromoVoucherStatus(giftArray, phoneNumber);
        request.response().setStatusCode(200).end();
    }
    /*******************************End************************************/

    /**
     * New method for change type request from send bus to http request
     *
     * @param giftArray
     * @param phoneNumber
     */
    public void updateOctoberPromoVoucherStatus(final JsonArray giftArray, String phoneNumber)
    {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "updateOctoberPromoVoucherStatus");
        JsonObject joFilter = new JsonObject();
        joFilter.putString(colName.OctoberPromoManageCols.PHONE_NUMBER, phoneNumber);
        joFilter.putNumber(colName.OctoberPromoManageCols.STATUS, 0);
        final JsonObject joUpdate = new JsonObject();
        octoberPromoManageDb.searchWithFilter(joFilter, new Handler<ArrayList<OctoberPromoManageDb.Obj>>() {
            @Override
            public void handle(ArrayList<OctoberPromoManageDb.Obj> objs) {
                if(objs.size() > 0)
                {
                    final OctoberPromoManageDb.Obj octoberPromoManagerObj = objs.get(0);
                    for(int i = 0; i < giftArray.size(); i++)
                    {
                        if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_1))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_1, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_2))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_2, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_3))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_3, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_4))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_4, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_5))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_5, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_6))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_6, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_7))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_7, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_8))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_8, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_9))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_9, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_10))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_10, true);
                        }
                    }
                    octoberPromoManageDb.updatePartial(octoberPromoManagerObj.id, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            log.add("voucher of " + octoberPromoManagerObj.phone_number + " is upsert", aBoolean);
                        }
                    });
                }
            }
        });
    }

    public void updateOctoberPromoVoucherStatus(final Common.BuildLog log,final JsonArray giftArray, String phoneNumber)
    {
        log.add("func", "updateOctoberPromoVoucherStatus");
        JsonObject joFilter = new JsonObject();
        joFilter.putString(colName.OctoberPromoManageCols.PHONE_NUMBER, phoneNumber);
        joFilter.putNumber(colName.OctoberPromoManageCols.STATUS, 0);
        final JsonObject joUpdate = new JsonObject();
        octoberPromoManageDb.searchWithFilter(joFilter, new Handler<ArrayList<OctoberPromoManageDb.Obj>>() {
            @Override
            public void handle(ArrayList<OctoberPromoManageDb.Obj> objs) {
                if(objs.size() > 0)
                {
                    final OctoberPromoManageDb.Obj octoberPromoManagerObj = objs.get(0);
                    for(int i = 0; i < giftArray.size(); i++)
                    {
                        if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_1))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_1, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_2))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_2, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_3))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_3, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_4))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_4, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_5))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_5, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_6))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_6, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_7))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_7, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_8))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_8, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_9))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_9, true);
                        }
                        else if(giftArray.get(i).toString().equalsIgnoreCase(octoberPromoManagerObj.gift_id_10))
                        {
                            joUpdate.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_10, true);
                        }
                    }
                    octoberPromoManageDb.updatePartial(octoberPromoManagerObj.id, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            log.add("voucher of " + octoberPromoManagerObj.phone_number + " is upsert", aBoolean);
                        }
                    });
                }
            }
        });
    }

    //Kiem tra chuong tirnh khuyen mai danh cho map vi lan dau tien voi tai khoan ngan hang
//    public void getFirstTimeBankMappingPromotion(SockData data, VisaResponse visaResponse, BankInfo bankInfo, Common.BuildLog log)
//    {
//        final String phoneNumber = visaResponse != null ? visaResponse.getVisaRequest().getPhoneNumber()
//                : bankInfo.getPhoneNumber();
//        final String bankCode = visaResponse != null ? "sbs" : bankInfo.getCoreBankCode();
//        final String cardInfo = visaResponse != null ? visaResponse.getCardChecksum() : bankInfo.getCustomerId();
//        log.setPhoneNumber(phoneNumber);
//        log.add("method", "getFirstTimeBankMappingPromotion");
//        if("".equalsIgnoreCase(bankCode))
//        {
//            log.add("desc", "Khong co thong tin CMND, khong tra thuong " + phoneNumber);
//            log.writeLog();
//            return;
//        }
//        else if(isStoreApp)
//        {
//            log.add("desc", "So diem giao dich, khong cho tham gia " + phoneNumber);
//            log.writeLog();
//            return;
//        }
//        log.add("desc", "Kiem tra tra thuong bonus referral v1");
//        referralProcess.checkBonusReferralV1Promotion(data, visaResponse, bankInfo, log);
//    }
//
//    public void goBankMappingPromotion(VisaResponse visaResponse, SockData data, BankResponse bankResponse, Common.BuildLog log)
//    {
//        String phoneNumber = visaResponse != null ? visaResponse.getVisaRequest().getPhoneNumber() : bankResponse.getRequest().getInitiator();
//        if(isStoreApp)
//        {
//            log.add("desc", "So diem giao dich, khong cho tham gia " + phoneNumber);
//            log.writeLog();
//            return;
//        }
//        referralProcess.checkAndBonusReferralV1Promotion(visaResponse, data, bankResponse, log);
//    }

    public void prepareExecuteReferralPromotion(List<String> keys, JsonObject values, HttpServerRequest request) {

        String phoneNumber = values.getString("phoneNumber", "");
        JsonObject joSockData = values.getObject("sockData");
        SockData sockData = null;
        if(joSockData != null) {
            sockData = new SockData(vertx, logger, glbConfig);
            sockData.fromJson(joSockData);
        }
        JsonObject jsonExtra = values.getObject("jsonExtra", new JsonObject());
        Common.BuildLog log = new Common.BuildLog(logger);
        StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL msgType =
                StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.getType(jsonExtra.getString("msgType",""));
        BankInfo bankInfo = new BankInfo(jsonExtra.getObject("bankInfo", new JsonObject()));
        VisaResponse visaResponse = new VisaResponse(jsonExtra.getObject("visaResponse", new JsonObject()));
        BankResponse bankResponse = new BankResponse(jsonExtra.getObject("bankResponse", new JsonObject()));

        executeReferralPromotion(phoneNumber,msgType, bankInfo, visaResponse, sockData, bankResponse, log, jsonExtra);
    }

    public void executeReferralPromotion(String phoneNumber, StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL msgType, BankInfo bankInfo, VisaResponse visaResponse,final SockData data,final BankResponse bankResponse,final Common.BuildLog log, JsonObject joExtra)
    {
        final ReferralProcess referralProcess = new ReferralProcess(vertx, logger, glbConfig);
        String serviceId = "";
        long tranId = 0;
        long amount = 0;
        if(isStoreApp)
        {
            log.add("desc", "So diem giao dich, khong cho tham gia ");
            log.writeLog();
            return;
        }
        switch (msgType)
        {
            case FIRST_TIME_BANK_MAPPING:
                phoneNumber = visaResponse != null ? visaResponse.getVisaRequest().getPhoneNumber()
                        : bankInfo.getPhoneNumber();
                String bankCode = visaResponse != null ? "sbs" : bankInfo.getCoreBankCode();
                String cardInfo = visaResponse != null ? visaResponse.getCardChecksum() : bankInfo.getCustomerId();
                log.setPhoneNumber(phoneNumber);
                log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "getFirstTimeBankMappingPromotion FIRST_TIME_BANK_MAPPING");
                if("".equalsIgnoreCase(bankCode))
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co thong tin CMND/the visa, khong tra thuong " + phoneNumber);
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Khong co thong tin CMND/the visa, khong tra thuong " + phoneNumber);
                    return;
                }
                referralProcess.checkBonusReferralV1Promotion(data, visaResponse, bankInfo, log);
                break;
            case BANK_MAPPING:
//                phoneNumber =  visaResponse != null ? visaResponse.getVisaRequest().getPhoneNumber() : bankResponse.getRequest().getInitiator();
//                log.setPhoneNumber(phoneNumber);
//                log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "checkAndBonusReferralV1Promotion + BANK_MAPPING");
//                log.add("phonenumber " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneNumber);
//                referralProcess.checkAndBonusReferralV1PromotionCashIn(data,, log);
                break;
//            case BACKEND_BANK_MAPPING:
//                phoneNumber =  joExtra.getString(StringConstUtil.ReferralVOnePromoField.PHONE_NUMBER, "");
//                cardInfo = joExtra.getString(StringConstUtil.ReferralVOnePromoField.CARD_INFO, "");
//                String bankAcc = joExtra.getString(StringConstUtil.ReferralVOnePromoField.BANK_ACC, "");
//                String imei = joExtra.getString(StringConstUtil.ReferralVOnePromoField.DEVICE_IMEI, "");
//                log.setPhoneNumber(phoneNumber);
//                log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "checkAndBonusReferralV1Promotion BACKEND_BANK_MAPPING");
//                log.add("phonenumber " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneNumber);
//                referralProcess.checkAndBonusReferralV1PromotionFromBackend(phoneNumber, cardInfo, imei, bankAcc, log);
//                break;
            case CASH_BACK:
//                serviceId = joExtra.getString(StringConstUtil.ReferralVOnePromoField.SERVICE_ID, "");
//                phoneNumber = joExtra.getString(StringConstUtil.ReferralVOnePromoField.PHONE_NUMBER, "");
//                amount = joExtra.getLong(StringConstUtil.ReferralVOnePromoField.AMOUNT, 0);
//                tranId = joExtra.getLong(StringConstUtil.ReferralVOnePromoField.TRAN_ID, 0);
//                log.setPhoneNumber(phoneNumber);
//                log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "executeCashBackReferralV1Promotion CASH_BACK");
//                referralProcess.executeCashBackReferralV1Promotion(data, phoneNumber, tranId, amount, serviceId, log);
                break;
            case CASH_IN:
                phoneNumber =  visaResponse != null ? visaResponse.getVisaRequest().getPhoneNumber() : bankResponse.getRequest().getInitiator();
                log.setPhoneNumber(phoneNumber);
                log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "checkAndBonusReferralV1Promotion + BANK_MAPPING");
                log.add("phonenumber " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneNumber);
                final String phoneNumberFinal = phoneNumber;
                phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj phoneObj) {
                        if(phoneObj != null)
                        {
                            referralProcess.checkAndBonusReferralV1PromotionCashIn(data, phoneObj, log);
                        }
                        else {
                            log.add("phonenumber " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Bang phone is null " + phoneNumberFinal);
                            log.writeLog();
                        }
                    }
                });
                break;
            default:
                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong tham gia cashback do truyen sai msgtype");
                log.writeLog();
                break;
        }

    }

    // TODO: 29/06/2016 create method prepare of ExecuteSCBPromotionProcess
    /**
     * prepare data before call to executeSCBPromotionProcess
     *
     * @param keys
     * @param values
     * @param request
     */
    public void prepareExecuteSCBPromotionProcess(List<String> keys, JsonObject values, HttpServerRequest request) {

        String phoneNumber = values.getString("phoneNumber", "");
        JsonObject jsonExtra = values.getObject("jsonExtra", new JsonObject());
        String msgTypeS = values.getString("msgType", "");
        StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION msgType = StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION.getType(msgTypeS);
        Common.BuildLog log = new Common.BuildLog(logger);

        String cardInfo = jsonExtra.getString("cardInfo", "");
        String bankAcc = jsonExtra.getString("bankAcc", "");
        String imei = jsonExtra.getString("imei", "");

        executeSCBPromotionProcess(phoneNumber, cardInfo, bankAcc, imei, jsonExtra, msgType, log);

        // response to client
        responseJson(request.response(), new JsonObject());
    }

    /**
     *
     * @param phoneNumber
     * @param cardInfo
     * @param bankAcc
     * @param imei
     * @param joExtra
     * @param msgType
     * @param log
     */
    public void executeSCBPromotionProcess(String phoneNumber, String cardInfo, String bankAcc, String imei, JsonObject joExtra, StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION msgType, Common.BuildLog log)
    {
        if(standardCharterBankPromotionProcess == null)
        {
            standardCharterBankPromotionProcess = new StandardCharterBankPromotionProcess(vertx, logger, glbConfig);
        }
        switch(msgType)
        {
            case BANK_MAPPING:
                log.add("" + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "BANK_MAPPING");
                log.add("" + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "checkAndSaveSCBCardInfo");
                standardCharterBankPromotionProcess.checkAndSaveSCBCardInfo(phoneNumber, cardInfo, bankAcc, imei, joExtra, log);
                break;
            case CASH_BACK:
                log.add("" + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "CASH_BACK");
                log.add("" + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "executeScbPromotionCashBackProcess");
                standardCharterBankPromotionProcess.executeScbPromotionCashBackProcess(phoneNumber, log, joExtra);
                break;
            default:
                log.add("" + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "DONT HAVE SUITABLE MSG TYPE");
                log.writeLog();
                break;
        }
    }

    public void prepareExcuteAcquireBinhTanUserPromotion(List<String> keys, JsonObject values, HttpServerRequest request) {
        String phoneNumber = values.getString("phoneNumber", "");
        MomoMessage message = MomoMessage.fromBuffer(new Buffer(values.getString("message", "")));
        JsonObject joSockData = values.getObject("sockData");
        SockData sockData = null;
        if(joSockData != null) {
            sockData = new SockData(vertx, logger, glbConfig);
            sockData.fromJson(joSockData);
        }
        JsonObject jsonExtra = values.getObject("jsonExtra", new JsonObject());
        StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION msg =
                StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.getType(jsonExtra.getString("msg_type_binhtan_promotion", ""));
        Common.BuildLog log = new Common.BuildLog(logger);
        excuteAcquireBinhTanUserPromotion(phoneNumber, log, message, sockData, msg, jsonExtra);
    }

    public void excuteAcquireBinhTanUserPromotion(final String phoneNumber,final Common.BuildLog log, final MomoMessage message, final SockData sockData, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION msg_type_binhtan_promotion, final JsonObject joExtra)
    {
        if(acquireBinhTanUserProcess == null)
        {
            acquireBinhTanUserProcess = new AcquireBinhTanUserProcess(vertx, logger, glbConfig);
        }
        //msg_type_binhtan_promotion = StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.NONE;
        switch (msg_type_binhtan_promotion)
        {
            case REGISTER:
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "REGISTER METHOD");
                acquireBinhTanUserProcess.executeRegisterUser(phoneNumber, log, sockData, joExtra);
                break;
            case LOGIN:
                vertx.setTimer(2000L, new Handler<Long>() {
                    @Override
                    public void handle(Long timer) {
                        vertx.cancelTimer(timer);
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "LOGIN METHOD");
                        acquireBinhTanUserProcess.executeLoginUser(message, phoneNumber, log, sockData, joExtra);
                    }
                });
                break;
            case CASH_IN:
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "CASH_IN METHOD");
                acquireBinhTanUserProcess.getInfoBankForBinhTan(phoneNumber, log, sockData, joExtra);
                break;
            case BILL_PAY:
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "BILL_PAY METHOD");
                acquireBinhTanUserProcess.executeBillPayUser(message, phoneNumber, log, sockData, joExtra);
                break;
            case UPDATE_BILL_PAY:
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "UPDATE_BILL_PAY METHOD");
                acquireBinhTanUserProcess.updateInfoBillPayUser(phoneNumber, log, sockData, joExtra);
                break;
            case ECHO:
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "ECHO METHOD");
                //msg is null
                acquireBinhTanUserProcess.getEchoCommand(phoneNumber, log, sockData, joExtra);
                break;
            case GET_OTP:
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "GET_OTP METHOD");
                //msg is null
                acquireBinhTanUserProcess.getOTPCommand(phoneNumber, log, sockData, joExtra);
                break;
            default:
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong co msg type phu hop");
                break;
        }
    }

    // TODO: 04/07/2016 create prepare method to receive http request from client

    /**
     * prepareStartProcessPromotion
     *
     * @param keys
     * @param values
     * @param request
     */
//    public void prepareStartProcessPromotion(List<String> keys, JsonObject values, HttpServerRequest request) {
//        String phoneNumber = values.getString("phoneNumber", "");
//        MomoMessage message = MomoMessage.fromBuffer(new Buffer(values.getString("message", "")));
//        JsonObject joSockData = values.getObject("sockData");
//        SockData sockData = null;
//        if(joSockData != null) {
//            sockData = new SockData(vertx, logger, glbConfig);
//            sockData.fromJson(joSockData);
//        }
//        int tranType = values.getInteger("tranType", 0);
//        JsonObject jsonExtra = values.getObject("jsonExtra", new JsonObject());
//        Common.BuildLog log = new Common.BuildLog(logger);
//        startProcessPromotion(phoneNumber, log, message, sockData, tranType, jsonExtra, request);
//    }

    /**
     * startProcessPromotion
     *
     * @param phoneNumber
     * @param log
     * @param message
     * @param sockData
     * @param tranType
     * @param joExtra
     * @param request
     */
//    private void startProcessPromotion(final String phoneNumber, final Common.BuildLog log, final MomoMessage message, final SockData sockData, final int tranType, final JsonObject joExtra, final HttpServerRequest request)
//    {
//        //Get active promotion.
//        //final List<PromotionDb.Obj> listPromoObjs = new ArrayList<>();
//        final Queue<PromotionDb.Obj> queuePromoObjs = new ArrayDeque<>();
//        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
//        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_ACTIVE_LIST;
//        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
//            @Override
//            public void handle(JsonObject json) {
//                JsonArray array = json.getArray("array", null);
//                if (array != null && array.size() > 0) {
//                    PromotionDb.Obj promoObj = null;
//                    for (Object o : array) {
//                        promoObj = new PromotionDb.Obj((JsonObject) o);
//                        if(promoObj != null)
//                        {
////                            listPromoObjs.add(promoObj);
//                            //check tran type
//                            if(checkTranTypeInProcess(promoObj.TRAN_TYPE, tranType)) {
//                                queuePromoObjs.add(promoObj);
//                            }
//                        }
//                    }
//                    executePromotionQueue(queuePromoObjs, phoneNumber, log, message, sockData, tranType, joExtra, request);
//                }
//                else {
//                    log.add("desc", "Khong co chuong trinh khuyen mai nao dang chay trong khoang thoi gian nay");
//                    log.writeLog();
//                    return;
//                }
//            }
//        });
//    }

    /**
     * Kiem tra chuong tinh khuyen mai cho kieu thanh toan nao
     *
     * @param promotionTranType
     * @param tranType
     * @return
     */
    private boolean checkTranTypeInProcess(int promotionTranType, int tranType) {
        boolean res = false;
        switch (promotionTranType) {
            case 0://cash in
                if(tranType == MomoProto.TranHisV1.TranType.BANK_IN_VALUE) {
                    res = true;
                } else {
                    res = false;
                }
                break;
            case 1://Claim Code
                if(tranType == MomoProto.TranHisV1.TranType.GIFT_CLAIM_VALUE) {
                    res = true;
                } else {
                    res = false;
                }
                break;
            case 2://Register
                res = true;
                break;
            case 3://Pay 1 Bill
                if(tranType == MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.PAY_AVG_BILL_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.PAY_NUOCCL_BILL_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.PAY_ONE_BILL_OTHER_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.BILL_PAY_CINEMA_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.BILL_PAY_INSURANCE_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.BILL_PAY_INTERNET_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_TRAIN_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.BILL_PAY_TELEPHONE_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.BILL_PAY_OTHER_VALUE
                        || tranType == MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE) {
                    res = true;
                } else {
                    res = false;
                }
                break;
            default:
                res = false;
                break;
        }
        return res;
    }


//    private void executePromotionQueue(final Queue<PromotionDb.Obj> queuePromoObjs, final String phoneNumber, final Common.BuildLog log,
//                                       final MomoMessage message, final SockData sockData, int tranType, final JsonObject joExtra, HttpServerRequest request)
//    {
//        PromotionDb.Obj promoObj = queuePromoObjs.poll();
//
//        executePromotion(promoObj, phoneNumber, log, message, sockData, tranType, joExtra, request);
//
//        executePromotionQueue(queuePromoObjs, phoneNumber, log, message, sockData, tranType, joExtra, request);
//    }

//    /***
//     *
//     *
//     *
//     * @param promoObj
//     * @param phoneNumber
//     * @param log
//     * @param message
//     * @param sockData
//     * @param tranType
//     * @param joExtra
//     * @param request
//     */
//    private void executePromotion(PromotionDb.Obj promoObj, final String phoneNumber, final Common.BuildLog log,
//                                  final MomoMessage message, final SockData sockData, int tranType,
//                                  final JsonObject joExtra, final HttpServerRequest request) {
//
//        boolean isVerticle = promoObj.ISVERTICLE;
//        String addressName = promoObj.ADDRESSNAME;
//        if(isVerticle) {
//            vertx.eventBus().send(addressName, joExtra, new Handler<Message<JsonObject>>() {
//                @Override
//                public void handle(Message<JsonObject> res) {
//                    responseJson(request.response(), res.body());
//                }
//            });
//        } else {
//
//            if(addressName.equalsIgnoreCase("excuteAcquireBinhTanUserPromotion")) {
//
//                StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION msg =
//                        StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.getType(joExtra.getString("msg_type_binhtan_promotion", ""));
//                excuteAcquireBinhTanUserPromotion(phoneNumber, log, message, sockData, msg, joExtra);
//            }
//
//            if(addressName.equalsIgnoreCase("executeSCBPromotionProcess")) {
//
//                String cardInfo = joExtra.getString("cardInfo", "");
//                String bankAcc = joExtra.getString("bankAcc", "");
//                String imei = joExtra.getString("imei", "");
//                StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION msgType = StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION.getType(joExtra.getString("msgType", ""));
//                executeSCBPromotionProcess(phoneNumber, cardInfo, bankAcc, imei, joExtra, msgType, log);
//            }
//
//            if(addressName.equalsIgnoreCase("getUserInfoToCheckPromoProgram")) {
//
//                String senderNumber = joExtra.getString("senderNumber", "");
//                PhonesDb.Obj phoneObj = new PhonesDb.Obj(joExtra.getObject("phoneObj",new JsonObject()));
//                long tranId = joExtra.getLong("tranId",0);
//                long amount = joExtra.getLong("amount",0);
//                String program = joExtra.getString("program","");
//                getUserInfoToCheckPromoProgram(senderNumber, phoneNumber, phoneObj, tranId, tranType, amount, program, sockData, joExtra);
//
//            }
//
//            if(addressName.equalsIgnoreCase("getUserInfoToCheckPromoProgramWithCallback")) {
//                String senderNumber = joExtra.getString("senderNumber", "");
//                PhonesDb.Obj phoneObj = new PhonesDb.Obj(joExtra.getObject("phoneObj",new JsonObject()));
//                long tranId = joExtra.getLong("tranId", 0);
//                long amount = joExtra.getLong("amount", 0);
//                String program = joExtra.getString("program", "");
//                getUserInfoToCheckPromoProgramWithCallback(senderNumber, phoneNumber, phoneObj, tranId, tranType,
//                        amount, program, sockData, joExtra, request);
//            }
//
//            if(addressName.equalsIgnoreCase("executeReferralPromotion")) {
//                StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL msgType =
//                        StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.getType(joExtra.getString("msgType",""));
//                BankInfo bankInfo = new BankInfo(joExtra.getObject("bankInfo", new JsonObject()));
//                VisaResponse visaResponse = new VisaResponse(joExtra.getObject("visaResponse", new JsonObject()));
//                BankResponse bankResponse = new BankResponse(joExtra.getObject("bankResponse", new JsonObject()));
//
//                executeReferralPromotion(phoneNumber,msgType, bankInfo, visaResponse, sockData, bankResponse, log, joExtra);
//            }
//
//            if(addressName.equalsIgnoreCase("updateOctoberPromoVoucherStatus")) {
//                JsonArray giftArray = joExtra.getArray("giftArray", new JsonArray());
//                updateOctoberPromoVoucherStatus(giftArray, phoneNumber);
//            }
//        }
//
//    }

    /**
     *
     * @param code
     * @param request
     */
    private void responseError(int code, HttpServerRequest request) {
        if(code == 400) {
            responseJson(request.response().setStatusCode(code), new JsonObject()
                    .putNumber("result", 5)
                    .putString("description", "Bad Request!")
                    .putString("Path", request.path())
            );
        } else if(code == 404) {
            responseJson(request.response().setStatusCode(code), new JsonObject()
                    .putNumber("result", 5)
                    .putString("description", "Resource not found!")
                    .putString("Path", request.path())
            );
        } else if(code == 401) {
            responseJson(request.response().setStatusCode(code), new JsonObject()
                    .putNumber("result", 5)
                    .putString("description", "Not Authentication!")
                    .putString("Path", request.path())
            );
        } else {
            responseJson(request.response().setStatusCode(code), new JsonObject()
                    .putNumber("result", 5)
                    .putString("description", "Error unknow")
                    .putString("Path", request.path())
            );
        }
    }

    private void responseJson(HttpServerResponse response, JsonObject obj) {
        response.putHeader("Content-Type", "application/json");
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.end(obj.toString());
    }

    /**
     * Send noti automatic after user cash in successful to
     * @param phoneNumber
     * @param bankCode
     * @param program
     * @param log
     * @param joInfoExtra
     */
    //0" + msg.cmdPhone, blObj.bank_code, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,log, joInfoExtra
    public void executeRefferalNotify(final String phoneNumber, final String bankCode, final String program, final Common.BuildLog log, final JsonObject joInfoExtra) {
        logger.info("reject another user bank mapping");
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    JsonObject jsonTime = new JsonObject();
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM)) {
                            promoObj = new PromotionDb.Obj((JsonObject) o);
                            break;
                        }
                    }
                    if(promoObj == null) {
                        return;
                    }
                    final PromotionDb.Obj promoObjfn = promoObj;
                    phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(PhonesDb.Obj obj) {
                            checkBankIsAccepted(obj.bank_code, promoObjfn, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean res) {
                                    if (res) {
                                        vertx.setTimer(30000L, new Handler<Long>() {
                                            @Override
                                            public void handle(Long event) {
                                                autoNotiCountDb.findOneByType(phoneNumber, colName.ReferralV1CodeInputCol.TABLE, new Handler<AutoNotiCountDb.Obj>() {
                                                    @Override
                                                    public void handle(AutoNotiCountDb.Obj obj) {
                                                        boolean sendNoti = false;
                                                        if (obj == null) {
                                                            logger.info("chua co bat ky thong tin cash in");
                                                            JsonObject joInsert = new JsonObject()
                                                                    .putString(colName.AutoNotiCount.PHONE_NUMBER, phoneNumber)
                                                                    .putString(colName.AutoNotiCount.TYPE, colName.ReferralV1CodeInputCol.TABLE);
                                                            AutoNotiCountDb.Obj objInsert = new AutoNotiCountDb.Obj(joInsert);
                                                            autoNotiCountDb.insert(objInsert, new Handler<Integer>() {
                                                                @Override
                                                                public void handle(Integer event) {
                                                                }
                                                            });
                                                            sendNoti = true;
                                                        } else {
                                                            if (obj.count < 3) {
                                                                logger.info("cash in chua qua 3 lan: " + obj.count);
                                                                sendNoti = true;
                                                                autoNotiCountDb.findAndIncCount(phoneNumber, new Handler<AutoNotiCountDb.Obj>() {
                                                                    @Override
                                                                    public void handle(AutoNotiCountDb.Obj event) {
                                                                    }
                                                                });
                                                            } else {
                                                                logger.info("cash in hon 3 lan roi");
                                                                Calendar calendar = Calendar.getInstance();
                                                                calendar.set(Calendar.DAY_OF_MONTH, 1);
                                                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                                                calendar.set(Calendar.MINUTE, 0);
                                                                calendar.set(Calendar.SECOND, 0);

                                                                long time = calendar.getTimeInMillis();
                                                                logger.info("Thoi gian ngay dau thang: " + time + " , thoi gian cash in cuoi cung: " + obj.cashinTime);
                                                                if (obj != null && obj.cashinTime < time) {
                                                                    sendNoti = true;
                                                                }
                                                            }
                                                        }
                                                        if (sendNoti) {
                                                            logger.info("send noti to user");
                                                            sendNotiCashIn(program, phoneNumber);
                                                        }
                                                        autoNotiCountDb.updatePartial(phoneNumber
                                                                , new JsonObject().putNumber(colName.AutoNotiCount.CASHIN_TIME, System.currentTimeMillis())
                                                                , new Handler<Boolean>() {
                                                                    @Override
                                                                    public void handle(Boolean event) {

                                                                    }
                                                                });
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
            }
        });
    }

    /**
     *
     * @param program
     * @param phoneNumber
     */
    private void sendNotiCashIn(final String program, final String phoneNumber) {
        promotionDb.getPromotions(new Handler<ArrayList<PromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<PromotionDb.Obj> promotions) {
                for (PromotionDb.Obj promotion : promotions) {
                    if (promotion.NAME.equalsIgnoreCase(program)) {
                        long curent = System.currentTimeMillis();
                        logger.info("reject user join program before or after time of promotion");
                        if (promotion.DATE_FROM > curent || promotion.DATE_TO < curent)
                            return;

                        String body = PromoContentNotification.NOTI_AUTO_REFFERAL_BODY;
                        String title = PromoContentNotification.NOTI_AUTO_REFFERAL_TITLE;
                        Misc.sendPopupReferral(vertx, phoneNumber, body, title, "https://momo.vn/chiasemomo/index.html");
                        return;
                    }
                }
            }
        });
    }

    public void executePromotion(final String billID, final int tranType, final String cusNum, final int phoneNumber,final long tranId,final long fAmount,final SockData _data, final String serviceId, final TransferWithGiftContext context, final long fee, final int srcFrom, final Common.BuildLog log
            , final JsonObject joExtra, final Handler<JsonObject> callback)
    {
        final Common.BuildLog log1 = new Common.BuildLog(logger);
        log1.add("method", "executePromotion");
        log1.add("method", "tranTYPE " + tranType + " serviceId " + serviceId);
        getJsonPromotion(billID, tranType, cusNum, phoneNumber, tranId, fAmount, _data, serviceId, context, fee, srcFrom, log, joExtra, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonPromotion) {
                vertx.eventBus().send(AppConstant.PROMOTION_CLIENT_BUS_ADDRESS, jsonPromotion, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> dataCallBack) {
                        //todo getResponse listNoti and send NotiContent - Popup
                        JsonObject joResponse = dataCallBack.body();
                        int error = joResponse.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                        if (error == 0) {
                            //todo getResultArrayAndSendNoti
                            JsonArray jArrResult = joResponse.getArray(StringConstUtil.PromotionField.RESULT, new JsonArray());
                            if (isUAT) {
                                log1.add("jArrResult promotion", jArrResult.toString());
                                log1.add("size promotion", jArrResult.size());
                            }
                            if (jArrResult.size() > 0) {
                                sendPromotionNoti(jArrResult, log, tranType, callback);
                            } else {
                                log1.add("error promotion", error);
                                log1.add("desc promotion", joResponse.getString(StringConstUtil.PromotionField.DESCRIPTION, ""));
                                callback.handle(new JsonObject());
                            }

                        } else {
                            //Do nothing
                            log1.add("error promotion", error);
                            log1.add("desc promotion", joResponse.getString(StringConstUtil.PromotionField.DESCRIPTION, ""));
                            callback.handle(new JsonObject());
                        }
                        log1.writeAndClear();

                    }
                });
            }
        });
    }

    private void sendPromotionNoti(final JsonArray jArrPromotionNoti, final Common.BuildLog log, final int tranType, final Handler<JsonObject> callback)
    {
        final JsonArray jarrData = new JsonArray();
        final List<JsonObject> joNotiList = new ArrayList<>();
        for (Object jo:jArrPromotionNoti) {
            JsonObject jsonExtra = ((JsonObject)jo).getObject(StringConstUtil.PromotionField.JSON_EXTRA, null);
            if(jsonExtra != null && jsonExtra.getFieldNames().size() > 0)
            {
                jarrData.add(jsonExtra);
            }
            if(((JsonObject)jo).getObject(StringConstUtil.PromotionField.NOTIFICATION, null) != null) {
                joNotiList.add((JsonObject)jo);
            }
        }

        final AtomicInteger countNoti = new AtomicInteger(joNotiList.size());
        vertx.setPeriodic(5000L, new Handler<Long>() {
            @Override
            public void handle(Long notiTimer) {
                int position = countNoti.decrementAndGet();
                if(position < 0)
                {
                    JsonObject joData = new JsonObject().putArray(StringConstUtil.PromotionField.DATA, jarrData);
//                    log.writeLog();
                    vertx.cancelTimer(notiTimer);
                    return;
                }
                JsonObject joData = joNotiList.get(position);
                JsonObject joNoti = joData.getObject(StringConstUtil.PromotionField.NOTIFICATION, null);
                Notification noti = joNoti == null ? null : Notification.parse(joNoti);
                if((tranType == MomoProto.TranHisV1.TranType.PAY_IPOS_BILL_VALUE || tranType == MomoProto.TranHisV1.TranType.PAY_SDK_BILL_VALUE) && noti != null)
                {
                    Misc.sendNotiFromSDKServer(vertx, noti);
                }
                else if(noti != null){
                    Misc.sendNoti(vertx, noti);
                }

            }
        });
        JsonObject joData = new JsonObject().putArray(StringConstUtil.PromotionField.DATA, jarrData);
        callback.handle(joData);
//        vertx.setPeriodic(5000L, new Handler<Long>() {
//            @Override
//            public void handle(Long notiTimer) {
//                int position = countNoti.decrementAndGet();
//                if(position < 0)
//                {
//                    JsonObject joData = new JsonObject().putArray(StringConstUtil.PromotionField.DATA, jarrData);
////                    log.writeLog();
//                    callback.handle(joData);
//                    vertx.cancelTimer(notiTimer);
//                }
//                else {
//                    JsonObject joData = jArrPromotionNoti.get(position);
//                    JsonObject joNoti = joData.getObject(StringConstUtil.PromotionField.NOTIFICATION, null);
//                    Notification noti = joNoti == null ? null : Notification.parse(joNoti);
//                    JsonObject jsonExtra = joData.getObject(StringConstUtil.PromotionField.JSON_EXTRA, null);
//                    if((tranType == MomoProto.TranHisV1.TranType.PAY_IPOS_BILL_VALUE || tranType == MomoProto.TranHisV1.TranType.PAY_SDK_BILL_VALUE) && noti != null)
//                    {
//                        Misc.sendNotiFromSDKServer(vertx, noti);
//                    }
//                    else if(noti != null){
//                        Misc.sendNoti(vertx, noti);
//                    }
//                    if(jsonExtra != null && jsonExtra.getFieldNames().size() > 0)
//                    {
//                        jarrData.add(jsonExtra);
//                    }
//                }
//            }
//        });
    }

    private void getJsonPromotion(final String billID, final int tranType, final String cusNum,final int phoneNumber,final long tranId,final long fAmount,final SockData _data, final String serviceId, final TransferWithGiftContext context, final long fee, final int srcFrom, final Common.BuildLog log
            , final JsonObject joExtra,final Handler<JsonObject> callback)
    {
        log.add("method", "getJsonPromotion");
        final JsonArray jArrGift = new JsonArray();

        if(context != null && context.queuedGiftResult != null && context.queuedGiftResult.queuedGifts.size() > 0)
        {
            for(QueuedGift queuedGift : context.queuedGiftResult.queuedGifts)
            {
                jArrGift.add(queuedGift.giftId);
            }
        }
        final PromotionObj promotionObj = new PromotionObj();
        if(_data != null && _data.getPhoneObj() != null){
             promotionObj.billId = billID;
             promotionObj.amount = fAmount;
             promotionObj.customerNumber = cusNum;
             promotionObj.fee = fee;
             promotionObj.jarrGift = jArrGift;
             promotionObj.joExtra = joExtra;
             promotionObj.joPhone = _data.getPhoneObj().toJsonObject();
             promotionObj.phoneNumber = "0" + phoneNumber;
             promotionObj.serviceId = serviceId;
             promotionObj.srcFrom = srcFrom;
             promotionObj.tranId = tranId;
             promotionObj.tranType = tranType;
             promotionObj.isStoreApp = isStoreApp;
             callback.handle(promotionObj.toPromotionJsonObject());
        }
        else {
            phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj phoneObj) {
                    promotionObj.billId = billID;
                    promotionObj.amount = fAmount;
                    promotionObj.customerNumber = cusNum;
                    promotionObj.fee = fee;
                    promotionObj.jarrGift = jArrGift;
                    promotionObj.joExtra = joExtra;
                    promotionObj.phoneNumber = "0" + phoneNumber;
                    promotionObj.serviceId = serviceId;
                    promotionObj.srcFrom = srcFrom;
                    promotionObj.tranId = tranId;
                    promotionObj.tranType = tranType;
                    promotionObj.isStoreApp = isStoreApp;
                    if(phoneObj != null)
                    {
                        promotionObj.joPhone = phoneObj.toJsonObject();
                    }
                    else {
                        logger.info("PHONE OBJ IN PROMOTION PROCESS IS NULL " + "0" + phoneNumber);
                        promotionObj.joPhone = new JsonObject();
                    }
                    callback.handle(promotionObj.toPromotionJsonObject());
                }
            });
        }
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
