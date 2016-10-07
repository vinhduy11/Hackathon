package com.mservice.momo.vertx.ironmanpromo;

import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.ironmanpromote.CountPreIronManManageDb;
import com.mservice.momo.data.ironmanpromote.IronManNewRegisterTrackingDb;
import com.mservice.momo.data.ironmanpromote.PreIronManPromotionManageDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
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
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by concu on 9/19/15.
 */
public class PreIronManPromoVerticle extends Verticle {


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
    private JsonObject jsonPreIronmanPromo;
    private PreIronManPromotionManageDb preIronManPromotionManageDb;
    private HashMap<String, String> mappingService = new HashMap<>();
    private IronManNewRegisterTrackingDb ironManNewRegisterTrackingDb;
    private CountPreIronManManageDb countPreIronManManageDb;
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
        jsonPreIronmanPromo = glbCfg.getObject(StringConstUtil.PreIronManPromo.JSON_OBJECT, new JsonObject());
        countPreIronManManageDb = new CountPreIronManManageDb(vertx, logger);
        preIronManPromotionManageDb = new PreIronManPromotionManageDb(vertx, logger);

        //String vinaProxyVerticle = "vinaphoneProxyVerticle";
        //Khong load duoc agent tra thuong

        final int number_of_random_gift = jsonPreIronmanPromo.getInteger(StringConstUtil.PreIronManPromo.NUMBER_OF_RANDOM_VOUCHER, 2000);
        final int number_of_given_gift = jsonPreIronmanPromo.getInteger(StringConstUtil.PreIronManPromo.NUMBER_OF_GIVEN_VOUCHER, 2000);
        //QUet dich vu Iron man de nhac nho user su dung qua

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final PreIronManPromoObj preIronManPromoObj = new PreIronManPromoObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(preIronManPromoObj.phoneNumber);
                log.add("phoneNumber", preIronManPromoObj.phoneNumber);
                log.add("source", preIronManPromoObj.source);

                final JsonObject jsonReply = new JsonObject();

                //Neu diem giao dich chuoi lot vo day thi khong tra thuong
                if (isStoreApp) {
                    log.add("desc", "app diem giao dich khong cho choi ironman promo");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }

                if (preIronManPromoObj.phoneNumber.equalsIgnoreCase("") || DataUtil.strToLong(preIronManPromoObj.phoneNumber) <= 0) {
                    log.add("desc", "So dien thoai la so dien thoai khong co that.");
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }

                countPreIronManManageDb.findOne(preIronManPromoObj.source, new Handler<CountPreIronManManageDb.Obj>() {
                    @Override
                    public void handle(CountPreIronManManageDb.Obj countObj) {
                        if(preIronManPromoObj.source.equalsIgnoreCase(StringConstUtil.PreIronManPromo.GROUP_1))
                        {
                            if(countObj != null && countObj.total > number_of_given_gift)
                            {
                                log.add("Desc", "Da qua so luong tang mien phi, xin loi chu em");
                                log.writeLog();
                                return;
                            }
                            //Tra thuong group 1
                            doPreIronPromoForVoucher(message, jsonReply, preIronManPromoObj, preIronManPromoObj.source, countObj, log);
                            return;
                        }
                        else if(preIronManPromoObj.source.equalsIgnoreCase(StringConstUtil.PreIronManPromo.GROUP_2))
                        {
                            if(countObj != null && countObj.total > number_of_random_gift)
                            {
                                log.add("Desc", "Da qua so luong tang mien phi, xin loi chu em");
                                log.writeLog();
                                return;
                            }
                            //Tra thuong group 2
                            doPreIronPromoForVoucher(message, jsonReply, preIronManPromoObj, preIronManPromoObj.source, countObj, log);
                            return;
                        }
                    }
                });


            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.PRE_IRON_MAN_PROMO_BUSS_ADDRESS, myHandler);
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void doPreIronPromoForVoucher(final Message<JsonObject> message
            , final JsonObject joReply
            , final PreIronManPromoObj reqObj
            , final String group
            , final CountPreIronManManageDb.Obj countObj
            , final Common.BuildLog log) {

        log.add("func", "doPreIronPromoForVoucher");
        preIronManPromotionManageDb.findOne(reqObj.phoneNumber, new Handler<PreIronManPromotionManageDb.Obj>() {
            @Override
            public void handle(final PreIronManPromotionManageDb.Obj preIronManPromoGiftDBObj) {

                // Trả khuyến mãi
                // Them thong tin service id va so voucher vao core
                ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                keyValues.add(new Misc.KeyValue("program", "pre_ironmanpromo"));


                keyValues.add(new Misc.KeyValue("group", group));

                final String agentName = jsonPreIronmanPromo.getString("agent", "cskh");
                final int timeForGift = jsonPreIronmanPromo.getInteger("timeforgift", 1);
                final long giftValue = jsonPreIronmanPromo.getLong("valueofgift", 10000);

                //Tra thuong trong core

                if(preIronManPromoGiftDBObj != null)
                {
                    log.add("Desc", "Da nhan qua roi, khong duoc nua nhe");
                    log.writeLog();
                    return;
                }

                final int endGiftTime = timeForGift;
                giftManager.adjustGiftValue(agentName
                        , reqObj.phoneNumber
                        , giftValue
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
                            giftType.setModelId("ironmanv1");
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
                                    log.add("desc", "tra thuong chuong trinh pre ironman bang gift");
                                    log.add("err", err);

                                    //------------tat ca thanh cong roi
                                    if (err == 0) {
                                        giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject jsonObject) {
                                                gift.status = 3;
                                                giveVoucherPreIronMan(message, reqObj, giftId, log, countObj, gift, giftValue, tranId);
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

    public void giveVoucherPreIronMan(final Message message, final PreIronManPromoObj preIronManPromoObj, String giftId, final Common.BuildLog log, final CountPreIronManManageDb.Obj countObj,
                                      final Gift gift, final long giftValue, final long tranId)
    {
        final PreIronManPromotionManageDb.Obj preObj = new PreIronManPromotionManageDb.Obj();
        preObj.gift_id = giftId;
        preObj.phone_number = preIronManPromoObj.phoneNumber;
        if(preIronManPromoObj.source.equalsIgnoreCase(StringConstUtil.PreIronManPromo.GROUP_1))
        {
            preObj.is_login_user = true;
            preObj.is_new_register = false;
        }
        else {
            preObj.is_login_user = false;
            preObj.is_new_register = true;
        }
        final JsonObject joReply = new JsonObject();
        preIronManPromotionManageDb.insert(preObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {
                if(countObj == null && event == 0)
                {
                    JsonObject joUpdate = new JsonObject();
                    joUpdate.putNumber(colName.CountPreIronManManage.TOTAL, 1);
                    countPreIronManManageDb.upsertPartial(preIronManPromoObj.source, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    } );
                }
                else if (event == 0)
                {
                    JsonObject joUpdate = new JsonObject();
                    joUpdate.putNumber(colName.CountPreIronManManage.TOTAL, countObj.total + 1);
                    countPreIronManManageDb.upsertPartial(preIronManPromoObj.source, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {
                            String giftMessage = "giftMessage";
                            String notiCaption = "notiCaption";
                            //String serviceName = "serviceName";
                            String tranComment = "tranComment";
                            String notiBody = "notiBody";

                            long timeforgift = 60 * 1000L * 60 * 24 * 1;
                            long endTime = 1 + timeforgift;


                            giftMessage = PromoContentNotification.PRE_PROMO_TOPUP;
                            tranComment = PromoContentNotification.PRE_PROMO_TOPUP;
                            notiCaption = "Nhận thẻ quà tặng";
                            notiBody = PromoContentNotification.PRE_PROMO_TOPUP;


                            Misc.sendTranHisAndNotiBillPayGift(vertx
                                    , DataUtil.strToInt(preIronManPromoObj.phoneNumber)
                                    , tranComment
                                    , tranId
                                    , giftValue
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
                        }
                    });
                }
            }
        });
    }




}
