package com.mservice.momo.clientform;

import com.mservice.momo.data.BillInfoService;
import com.mservice.momo.data.CDHHErr;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.gateway.internal.connectorproxy.ConnectorCommon;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.CodeUtil;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoObj;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.proxy.entity.ProxyRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by concu on 11/4/14.
 */
public class SubmitForm {
    private static String billId2CheckInfor;
    public long TRANWITHPOINT_MIN_POINT = 0;
    public long TRANWITHPOINT_MIN_AMOUNT = 0;
    private Logger logger;
    private Vertx vertx;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private boolean isStoreApp = false;
    private PhonesDb phonesDb;
    public SubmitForm(Logger logger, Vertx vertx, JsonObject glbCfg) {
        this.logger = logger;
        this.vertx = vertx;
        this.glbCfg = glbCfg;

        JsonObject pointConfig = glbCfg.getObject("point", new JsonObject());
        TRANWITHPOINT_MIN_POINT = pointConfig.getLong("minPoint", 0);
        TRANWITHPOINT_MIN_AMOUNT = pointConfig.getLong("mintAmount", 0);
        giftManager = new GiftManager(vertx, logger, glbCfg);

        isStoreApp = glbCfg.getBoolean(StringConstUtil.CHECK_STORE_APP, false);

        phonesDb = new PhonesDb(vertx.eventBus(), logger);

    }

    private static ProxyRequest setValueSurveytoReques(Map<String, String> hashMap, ProxyRequest proxyRequest) {
        if (!hashMap.isEmpty()) {
            for (Map.Entry<String, String> question : hashMap.entrySet()) {
                if (question.getKey().substring(0, 2).equalsIgnoreCase("qu")) {
                    proxyRequest.addExtraValue(question.getKey(), question.getValue());
                }
            }
        }
        proxyRequest.setBillId(proxyRequest.getDebitor());
        return proxyRequest;
    }

    public void processSubmitFrmZingxu(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalFrm) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);

        //simulate here
        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;

        log.add("function", "processSubmitFrmZingxu");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);
        log.add("amount", strAmt);

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        if (nextFrm == (totalFrm - 1)) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Caption, "Xác nhận mua"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Button, "Đồng ý"));

            for (String s : hashMap.keySet()) {
                arrayList.add(new BillInfoService.TextValue(s, hashMap.get(s)));
            }

        } else {
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
        }

        message.reply(Misc.getJsonArray(arrayList));
        log.writeLog();
    }

    public void processSubmitFrmJetStar(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalFrm) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        final MomoProto.FormReply.Builder builder = MomoProto.FormReply.newBuilder();


        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;

        log.add("function", "processSubmitFrmJetStar");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);
        log.add("amount", strAmt);


        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        if (nextFrm == (totalFrm - 1)) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));// dung
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Caption, "Xác nhận mua"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Button, "Đồng ý"));

            for (String s : hashMap.keySet()) {
                arrayList.add(new BillInfoService.TextValue(s, hashMap.get(s)));
            }

        } else {
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
        }

        message.reply(Misc.getJsonArray(arrayList));
        log.writeLog();

    }

    public void processSubmitFrmBac(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalFrm) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        final MomoProto.FormReply.Builder builder = MomoProto.FormReply.newBuilder();

        //simulate here
        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;

        log.add("function", "processSubmitFrmBac");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);
        log.add("amount", strAmt);

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        if (nextFrm == (totalFrm - 1)) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Caption, "Xác nhận mua"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Button, "Đồng ý"));

            for (String s : hashMap.keySet()) {
                arrayList.add(new BillInfoService.TextValue(s, hashMap.get(s)));
            }

        } else {
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
        }

        message.reply(Misc.getJsonArray(arrayList));
        log.writeLog();

    }

    public void processSubmitFrmOnc(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalFrm) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        final MomoProto.FormReply.Builder builder = MomoProto.FormReply.newBuilder();

        //simulate here
        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;

        log.add("function", "processSubmitFrmOnc");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);
        log.add("amount", strAmt);

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        if (nextFrm == (totalFrm - 1)) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Caption, "Xác nhận thanh toán"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Button, "Đồng ý"));

            for (String s : hashMap.keySet()) {
                arrayList.add(new BillInfoService.TextValue(s, hashMap.get(s)));
            }

        } else {
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
        }

        message.reply(Misc.getJsonArray(arrayList));
        log.writeLog();

    }

    public void processSubmitFrmAvg(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalForm
            , final Map<String, BillInfoService> hashMapBIS
    ) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        final MomoProto.FormReply.Builder builder = MomoProto.FormReply.newBuilder();

        //simulate here
        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;
        log.add("function", "processSubmitFrmAvg");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);

        final ArrayList<BillInfoService.TextValue> textValues = new ArrayList<>();

        if (nextFrm == 0) {

            if (hashMapBIS.containsKey(serviceId + phoneNumber)) {
                log.add("desc", "hashMapBis remove serviceId + phoneNumber");
                hashMapBIS.remove(serviceId + phoneNumber);
            }

            Misc.getBillInfo(vertx, billId, serviceId, new Handler<BillInfoService>() {
                @Override
                public void handle(BillInfoService billInfoService) {
                    log.add("billInfoService", billInfoService.toJsonObject());
                    if (billInfoService != null && billInfoService.array_price.size() > 0) {
                        log.add("desc", "Save infos into hashMapBIS " + serviceId + phoneNumber);
                        hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
                    } else {
                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Không lấy được thông tin khách hàng."));
                    }
                    message.reply(Misc.getJsonArray(textValues));
                    log.writeLog();
                    return;
                }
            });
        } else {

            if (nextFrm == (totalForm - 1)) {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
            } else {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
            }

            message.reply(Misc.getJsonArray(textValues));
            log.writeLog();
        }
    }

    public void processSubmitFrmInviteFriend(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmInviteFriend");
        log.add("service id", serviceId);

        final String promoCode = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "1";

        log.add("promocode", promoCode);
        log.add("nextFrm", nextFrm);

        CodeUtil codeUtil = new CodeUtil(5, 6);

        if (!codeUtil.isCodeValid(promoCode, "MOMO")) {

            ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<BillInfoService.TextValue>();
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Mã lì xì không hợp lệ, Vui lòng thử lại"));

            message.reply(Misc.getJsonArray(arrayList));
            log.writeLog();
            return;
        }

        //default la chuong trinh gioi thieu ban be
        String promoName = "invitefriend";

        final Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.DO_PROMO_BY_CODE;
        promoReqObj.CREATOR = "0" + phoneNumber;
        promoReqObj.PROMO_CODE = promoCode.trim().toUpperCase();
        promoReqObj.PROMO_NAME = promoName;

        log.add("request claim code json", promoReqObj.toJsonObject());
//        log.writeLog();

        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                Promo.PromoResObj promoResObj = new Promo.PromoResObj(jsonObject);

                log.add("execute promo result", "-----------");
                log.add("result", promoResObj.RESULT);
                log.add("error", promoResObj.ERROR);
                log.add("desc", promoResObj.DESCRIPTION);

                ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<BillInfoService.TextValue>();
                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, promoResObj.DESCRIPTION));

                message.reply(Misc.getJsonArray(arrayList));
                log.writeLog();
                return;
            }
        });
    }

    public void processSubmitFrmCoffeeShop(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmCoffeeShop");

        final String promoCode = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "1";

        final Promo.PromoReqObj cfPromoReqObj = new Promo.PromoReqObj();
        cfPromoReqObj.COMMAND = Promo.PromoType.PROMO_M2M_CLAIM_CODE;
        cfPromoReqObj.CREATOR = "0" + phoneNumber;
        cfPromoReqObj.PROMO_CODE = promoCode.trim().toUpperCase();
        cfPromoReqObj.PROMO_NAME = "";

        log.add("serviceid", serviceId);
        log.add("command", cfPromoReqObj.COMMAND);
        log.add("creator", cfPromoReqObj.CREATOR);
        log.add("promo code", cfPromoReqObj.PROMO_CODE);
        log.add("nextform", nextFrm);

        Misc.requestPromoRecord(vertx, cfPromoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                Promo.PromoResObj promoResObj = new Promo.PromoResObj(jsonObject);

                log.add("execute promo result", "-----------");
                log.add("result", promoResObj.RESULT);
                log.add("error", promoResObj.ERROR);
                log.add("desc", promoResObj.DESCRIPTION);

                ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, promoResObj.DESCRIPTION));
                message.reply(Misc.getJsonArray(arrayList));
                log.writeLog();
            }
        });

    }
    //END 0000000004

    //BEGIN 0000000004
    public void processSubmitFrmBillPayPromo(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmBillPayPromo");

        final String promoCode = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "1";

        BillPayPromoObj.requestBillPayPromo(vertx, "0" + phoneNumber, 0, 0, promoCode, BillPayPromoConst.MUON_GIO_DONG_PROMO,
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                        if (jsonObject != null) {
                            int error = jsonObject.getInteger("error", 1000);
                            String desc = jsonObject.getString("desc", "Bạn đã nhận được thẻ quà tặng này rồi");
                            if (error == 0) {
                                //Ban da nhan duoc 1 the qua tang
                                ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
                                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Bạn đã nhận được thẻ quà tặng "));
                                message.reply(Misc.getJsonArray(arrayList));
                                log.writeLog();
                                return;
                            } else {
                                log.add("error", 1000);
                                log.add("desc", "So xui nen khong nhan duoc qua roi");
                                ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
                                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, desc));
                                message.reply(Misc.getJsonArray(arrayList));
                                log.writeLog();
                                return;
                            }
                        }
                    }
                }
        );
//        final Promo.PromoReqObj cfPromoReqObj = new Promo.PromoReqObj();
//        cfPromoReqObj.COMMAND = Promo.PromoType.PROMO_M2M_CLAIM_CODE;
//        cfPromoReqObj.CREATOR = "0" + phoneNumber;
//        cfPromoReqObj.PROMO_CODE = promoCode.trim().toUpperCase();
//        cfPromoReqObj.PROMO_NAME = "";
//
//        log.add("serviceid",serviceId);
//        log.add("command", cfPromoReqObj.COMMAND);
//        log.add("creator", cfPromoReqObj.CREATOR);
//        log.add("promo code", cfPromoReqObj.PROMO_CODE);
//        log.add("nextform", nextFrm);


//        Misc.requestPromoRecord(vertx, cfPromoReqObj, logger, new Handler<JsonObject>() {
//            @Override
//            public void handle(JsonObject jsonObject) {
//                Promo.PromoResObj promoResObj = new Promo.PromoResObj(jsonObject);
//
//                log.add("execute promo result", "-----------");
//                log.add("result", promoResObj.RESULT);
//                log.add("error", promoResObj.ERROR);
//                log.add("desc", promoResObj.DESCRIPTION);
//
//                ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
//                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
//                arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, promoResObj.DESCRIPTION));
//                message.reply(Misc.getJsonArray(arrayList));
//                log.writeLog();
//            }
//        });

    }

    public void processSubmitFrmCDHH(final Message message, final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        int min_code = glbCfg.getObject("capdoihoanhao").getInteger("min_code", 1);
        int max_code = glbCfg.getObject("capdoihoanhao").getInteger("max_code", 7);
        int max_sms = glbCfg.getObject("capdoihoanhao").getInteger("max_sms_per_day", 30);
        final int min_val = glbCfg.getObject("capdoihoanhao").getInteger("min_val", 1000);
        final int max_val = glbCfg.getObject("capdoihoanhao").getInteger("max_val", 80000);

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmCDHH");
        log.add("sid", serviceId);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "0";

        String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final long amount = DataUtil.strToInt(strAmt);
        final int code = DataUtil.strToInt(billId);

        log.add("nextform", nextFrm);
        log.add("amount", amount);
        log.add("code", code);

        boolean isValid = true;

        final ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();


        //kiem tra ma so binh chon
        if ((code > max_code || code < min_code) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "SBD không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("ma code khong hop le", "0" + code);
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidCode, CDHHErr.Error.InvalidCode, 0);
        }

        //kiem tra so luong tin nhan
        if ((amount < min_val || amount > max_val) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Số lượng tin nhắn bình chọn không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("So luong tin nhan binh chon khong hop le", (amount / min_val));
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidServiceId, CDHHErr.Error.InvalidServiceId, 0);

        }

        if (!isValid) {
            message.reply(Misc.getJsonArray(arrayList));
            log.writeLog();
            return;
        }

        //kiem tra co mo cua khong
        Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
            @Override
            public void handle(CdhhConfig cdhhConfig) {
                boolean isOpened = true;
                if (cdhhConfig == null) {
                    isOpened = false;
                }

                long time = System.currentTimeMillis();

                if ((cdhhConfig.endTime == null
                        || cdhhConfig.startTime == null) && isOpened) {
                    isOpened = false;
                }

                if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                        || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                        && isOpened) {
                    isOpened = false;
                }

                if (!isOpened) {

                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Hệ thống chưa mở cửa. Vui lòng thử lại sau"));

                    saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.NotOpen, CDHHErr.Error.NotOpen, 0);
                    message.reply(Misc.getJsonArray(arrayList));
                    return;
                }

                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                message.reply(Misc.getJsonArray(arrayList));
                log.writeLog();
            }
        });
    }

    public void processSubmitFrmBNHV(final Message message, final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        int min_code = glbCfg.getObject("capdoihoanhao").getInteger("min_code", 1);
        int max_code = glbCfg.getObject("capdoihoanhao").getInteger("max_code", 7);
        int max_sms = glbCfg.getObject("capdoihoanhao").getInteger("max_sms_per_day", 30);
        final int min_val = glbCfg.getObject("capdoihoanhao").getInteger("min_val", 1000);
        final int max_val = glbCfg.getObject("capdoihoanhao").getInteger("max_val", 80000);

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmCDHH");
        log.add("sid", serviceId);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "0";

        String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final long amount = DataUtil.strToInt(strAmt);
        final int code = DataUtil.strToInt(billId);

        log.add("nextform", nextFrm);
        log.add("amount", amount);
        log.add("code", code);

        boolean isValid = true;

        final ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
        message.reply(Misc.getJsonArray(arrayList));

        //kiem tra ma so binh chon
        if ((code > max_code || code < min_code) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "SBD không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("ma code khong hop le", "0" + code);
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidCode, CDHHErr.Error.InvalidCode, 0);
        }

        //kiem tra so luong tin nhan
        if ((amount < min_val || amount > max_val) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Số lượng tin nhắn bình chọn không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("So luong tin nhan binh chon khong hop le", (amount / min_val));
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidServiceId, CDHHErr.Error.InvalidServiceId, 0);

        }

        if (!isValid) {
            message.reply(Misc.getJsonArray(arrayList));
            log.writeLog();
            return;
        }

        //kiem tra co mo cua khong
        Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
            @Override
            public void handle(CdhhConfig cdhhConfig) {
                boolean isOpened = true;
                if (cdhhConfig == null) {
                    isOpened = false;
                }

                long time = System.currentTimeMillis();

                if ((cdhhConfig.endTime == null
                        || cdhhConfig.startTime == null) && isOpened) {
                    isOpened = false;
                }

                if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                        || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                        && isOpened) {
                    isOpened = false;
                }

                if (!isOpened) {

                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Hệ thống chưa mở cửa. Vui lòng thử lại sau"));

                    saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.NotOpen, CDHHErr.Error.NotOpen, 0);
                    message.reply(Misc.getJsonArray(arrayList));
                    return;
                }

                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                message.reply(Misc.getJsonArray(arrayList));
                log.writeLog();
            }
        });
    }

    public void processSubmitFrmREMIX(final Message message, final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmREMIX");
        log.add("sid", serviceId);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "0";

        String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final long amount = DataUtil.strToInt(strAmt);
        final int code = DataUtil.strToInt(billId);

        log.add("nextform", nextFrm);
        log.add("amount", amount);
        log.add("code", code);

        final ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        //kiem tra co mo cua khong
        Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
            @Override
            public void handle(CdhhConfig cdhhConfig) {
                boolean isOpened = true;
                if (cdhhConfig == null) {
                    isOpened = false;
                }

                long time = System.currentTimeMillis();

                if ((cdhhConfig.endTime == null
                        || cdhhConfig.startTime == null) && isOpened) {
                    isOpened = false;
                }

                if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                        || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                        && isOpened) {
                    isOpened = false;
                }

                if (!isOpened) {

                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Hệ thống chưa mở cửa. Vui lòng thử lại sau"));
                    message.reply(Misc.getJsonArray(arrayList));
                    return;
                }

                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                message.reply(Misc.getJsonArray(arrayList));
                log.writeLog();
            }
        });
    }

    public void processSubmitFrmVNIDOL(final Message message, final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        int min_code = glbCfg.getObject("vnidol").getInteger("min_code", 1);
        int max_code = glbCfg.getObject("vnidol").getInteger("max_code", 8);
        int max_sms = glbCfg.getObject("vnidol").getInteger("max_sms_per_day", 3);
        final int min_val = glbCfg.getObject("vnidol").getInteger("min_val", 3000);
        final int max_val = glbCfg.getObject("vnidol").getInteger("max_val", 9000);

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmVNIDOL");
        log.add("sid", serviceId);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "0";

        String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final long amount = DataUtil.strToInt(strAmt);
        final int code = DataUtil.strToInt(billId);

        log.add("nextform", nextFrm);
        log.add("amount", amount);
        log.add("code", code);

        boolean isValid = true;

        final ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
        message.reply(Misc.getJsonArray(arrayList));

        //kiem tra ma so binh chon
        if ((code > max_code || code < min_code) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Tên thí sinh không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("ma code khong hop le", "0" + code);
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidCode, CDHHErr.Error.InvalidCode, 0);
        }

        //kiem tra so luong tin nhan
        if ((amount < min_val || amount > max_val) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Số lượng tin nhắn bình chọn không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("So luong tin nhan binh chon khong hop le", (amount / min_val));
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidServiceId, CDHHErr.Error.InvalidServiceId, 0);

        }

        if (!isValid) {
            message.reply(Misc.getJsonArray(arrayList));
            log.writeLog();
            return;
        }

        //kiem tra co mo cua khong
        Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
            @Override
            public void handle(CdhhConfig cdhhConfig) {
                boolean isOpened = true;
                if (cdhhConfig == null) {
                    isOpened = false;
                }

                long time = System.currentTimeMillis();

                if ((cdhhConfig.endTime == null
                        || cdhhConfig.startTime == null) && isOpened) {
                    isOpened = false;
                }

                if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                        || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                        && isOpened) {
                    isOpened = false;
                }

                if (!isOpened) {

                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Hệ thống chưa mở cửa. Vui lòng thử lại sau"));

                    saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.NotOpen, CDHHErr.Error.NotOpen, 0);
                    message.reply(Misc.getJsonArray(arrayList));
                    return;
                }

                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                message.reply(Misc.getJsonArray(arrayList));
                log.writeLog();
            }
        });
    }

    public void processSubmitFrmEvent(final Message message, final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        int min_code = glbCfg.getObject(serviceId).getInteger("min_code", 1);
        int max_code = glbCfg.getObject(serviceId).getInteger("max_code", 8);
        int max_sms = glbCfg.getObject(serviceId).getInteger("max_sms_per_day", 3);
        final int min_val = glbCfg.getObject(serviceId).getInteger("min_val", 3000);
        final int max_val = glbCfg.getObject(serviceId).getInteger("max_val", 9000);

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processSubmitFrmEvent " + serviceId);
        log.add("sid", serviceId);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "0";

        String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final long amount = DataUtil.strToInt(strAmt);
        final int code = DataUtil.strToInt(billId);

        log.add("nextform", nextFrm);
        log.add("amount", amount);
        log.add("code", code);

        boolean isValid = true;

        final ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
        message.reply(Misc.getJsonArray(arrayList));

        //kiem tra ma so binh chon
        if ((code > max_code || code < min_code) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Tên thí sinh không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("ma code khong hop le", "0" + code);
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidCode, CDHHErr.Error.InvalidCode, 0);
        }

        //kiem tra so luong tin nhan
        if ((amount < min_val || amount > max_val) && isValid) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Số lượng tin nhắn bình chọn không hợp lệ. Vui lòng kiểm tra lại"));
            isValid = false;
            log.add("So luong tin nhan binh chon khong hop le", (amount / min_val));
            saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.InvalidServiceId, CDHHErr.Error.InvalidServiceId, 0);

        }

        if (!isValid) {
            message.reply(Misc.getJsonArray(arrayList));
            log.writeLog();
            return;
        }

        //kiem tra co mo cua khong
        Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
            @Override
            public void handle(CdhhConfig cdhhConfig) {
                boolean isOpened = true;
                if (cdhhConfig == null) {
                    isOpened = false;
                }

                long time = System.currentTimeMillis();

                if ((cdhhConfig.endTime == null
                        || cdhhConfig.startTime == null) && isOpened) {
                    isOpened = false;
                }

                if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                        || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                        && isOpened) {
                    isOpened = false;
                }

                if (!isOpened) {

                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                    arrayList.add(new BillInfoService.TextValue(Const.AppClient.Error, "Hệ thống chưa mở cửa. Vui lòng thử lại sau"));

                    saveTrackDDHHErr(phoneNumber, CDHHErr.Desc.NotOpen, CDHHErr.Error.NotOpen, 0);
                    message.reply(Misc.getJsonArray(arrayList));
                    return;
                }

                arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                message.reply(Misc.getJsonArray(arrayList));
                log.writeLog();
            }
        });
    }

    public void processDefault(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber) {

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("sid", serviceId);

        final MomoProto.FormReply.Builder builder = MomoProto.FormReply.newBuilder();
        String nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? hashMap.get(Const.AppClient.NextForm) : "0";

        //default : lay thong tin client tra ve cho client
        builder.setNext(DataUtil.strToInt(nextFrm));

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
        arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, nextFrm));

        message.reply(Misc.getJsonArray(arrayList));

        log.writeLog();
    }
    //support.end

    //suport.start
    private void saveTrackDDHHErr(int number, String desc, String error, int voteAmount) {

        CDHHErr cdhhErr = new CDHHErr(vertx.eventBus(), logger);
        CDHHErr.Obj cdhhErrObj = new CDHHErr.Obj();
        cdhhErrObj.desc = desc;
        cdhhErrObj.error = error;
        cdhhErrObj.number = number;
        cdhhErrObj.voteAmount = voteAmount;
        cdhhErr.save(cdhhErrObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });
    }

    public void processSubmitFrmTopup(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalFrm) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final String strAmt = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;
        final String qty = hashMap.containsKey(Const.AppClient.Quantity) ? hashMap.get(Const.AppClient.Quantity) : "1";
        final String email = hashMap.containsKey(Const.AppClient.Email) ? hashMap.get(Const.AppClient.Email) : "";

        log.add("function", "processSubmitFrmTopup");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);
        log.add("amount", strAmt);
        log.add("qty", qty);
        log.add("email", email);

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        if (nextFrm == (totalFrm - 1)) {

            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Caption, "Xác nhận thanh toán"));
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.Button, "Đồng ý"));

            for (String s : hashMap.keySet()) {
                arrayList.add(new BillInfoService.TextValue(s, hashMap.get(s)));
            }

        } else {
            arrayList.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
        }

        message.reply(Misc.getJsonArray(arrayList));
        log.writeLog();
    }

    public void processSubmitFrmTopupCheckInfor(final Message message
            , final Map<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalForm
            , final Map<String, BillInfoService> hashMapBIS
    ) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;

        String quantity = hashMap.containsKey(Const.AppClient.Quantity) ? hashMap.get(Const.AppClient.Quantity) : "";
        log.add("quantity", quantity);
        String amount = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "";
        String fullname = hashMap.containsKey(Const.AppClient.FullName) ? hashMap.get(Const.AppClient.FullName) : "";
        log.add("amount", amount);

        log.add("function", "processSubmitFrmTopupCheckInfor");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);
        log.add("fullname", fullname);
        final String serviceCode = serviceId.toUpperCase();

        final ArrayList<BillInfoService.TextValue> textValues = new ArrayList<>();

        if (nextFrm == 0) {

            Misc.getViaCoreService(vertx, serviceId, isStoreApp, new Handler<ViaConnectorObj>() {

                @Override
                public void handle(final ViaConnectorObj viaConnectorObj) {
                    if (viaConnectorObj.IsViaConnectorVerticle) {

                        ProxyRequest proxyRequest = ConnectorCommon.createSubmitFrmTopupCheckInforRequest(
                                "0" + phoneNumber, "", serviceCode, viaConnectorObj.BillPay, billId, hashMap);
                        if (proxyRequest.getServiceCode().equalsIgnoreCase("survey")) {
                            proxyRequest = setValueSurveytoReques(hashMap, proxyRequest);
                        }

                        ConnectorCommon.RequestCheckBill(vertx, logger, phoneNumber, glbCfg, proxyRequest, viaConnectorObj.BusName, log, new Handler<BillInfoService>() {
                            @Override
                            public void handle(final BillInfoService billInfoService) {

                                if ("prudential".equalsIgnoreCase(serviceId)) {

                                    if (billInfoService.total_amount == 0) {
                                        hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                                        log.add("Thông tin submitForm", billInfoService.toJsonObject().encodePrettily());
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
                                    } else if (billInfoService.total_amount == -1) {
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Số hợp đồng đã hết hiệu lực."));
                                    } else {
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Số hợp đồng bị sai. Vui lòng nhập lại hoặc liên hệ để hỗ trợ."));
                                    }

                                } else if ("railway".equalsIgnoreCase(serviceId) || "thienhoa".equalsIgnoreCase(serviceId)) {

                                    if (billInfoService.total_amount == 0) {
                                        hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                                        log.add("Thông tin submitForm", billInfoService.toJsonObject().encodePrettily());
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
                                    } else if (billInfoService.total_amount == -1) {
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Mã đã được thanh toán. Vui lòng nhập mã thanh toán khác."));
                                    } else if (billInfoService.total_amount == -2) {
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Mã thanh toán đã hết hiệu lực."));
                                    } else {
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Mã thanh toán bạn nhập chưa đúng. Vui lòng kiểm tra và nhập lại hoặc liên hệ (08) 39917199 để được hỗ trợ."));
                                    }

                                } else if ("vtvcab".equalsIgnoreCase(serviceId)) {

                                    if (billInfoService.total_amount == 0) {
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Mã đã được thanh toán."));
                                    } else if (billInfoService.total_amount == -100) {
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Mã thanh toán bạn nhập chưa đúng. Vui lòng kiểm tra và nhập lại."));
                                    } else {
                                        hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                                        log.add("Thông tin submitForm", billInfoService.toJsonObject().encodePrettily());
                                        textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
                                    }
                                }
                                // Neu dich vu la scb, kiem tra dinh danh.
                                else if("scb".equalsIgnoreCase(serviceId))
                                {
                                    addBillInfoSCBService(billInfoService, phoneNumber, hashMapBIS, serviceId, log, textValues, nextFrm, message);
                                    return;
                                }
                                else if(viaConnectorObj.isNamedChecked){
                                    addBillInfoIsNamedCheckedService(billInfoService, phoneNumber, hashMapBIS, serviceId, log, textValues, nextFrm, message);
                                    return;
                                }
                                else {
                                    addBillInfoOtherServices(billInfoService, hashMapBIS, serviceId, phoneNumber, log, textValues, nextFrm);
                                }
                                message.reply(Misc.getJsonArray(textValues));
                                log.writeLog();
                            }
                        });
                    }
                }
            });
        } else {

            if (nextFrm == (totalForm - 1)) {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
            } else {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
            }

            message.reply(Misc.getJsonArray(textValues));
            log.writeLog();
        }
    }

    private void addBillInfoSCBService(final BillInfoService billInfoService, final int phoneNumber, final Map<String, BillInfoService> hashMapBIS, final String serviceId, final Common.BuildLog log, final ArrayList<BillInfoService.TextValue> textValues, final int nextFrm, final Message message) {
        //Kiem tra thong tin dinh danh cua khach hang
        phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if(obj != null && obj.isNamed)
                {
                    addBillInfoOtherServices(billInfoService, hashMapBIS, serviceId, phoneNumber, log, textValues, nextFrm);
                }
                else if(billInfoService.total_amount != -100)
                {
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, StringConstUtil.SCB_NO_NAMED_POP_UP));
                }
                else{
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Không lấy được thông tin khách hàng."));
                }
                message.reply(Misc.getJsonArray(textValues));
                log.writeLog();
            }
        });
    }

    private void addBillInfoIsNamedCheckedService(final BillInfoService billInfoService, final int phoneNumber, final Map<String, BillInfoService> hashMapBIS, final String serviceId, final Common.BuildLog log, final ArrayList<BillInfoService.TextValue> textValues, final int nextFrm, final Message message) {
        //Kiem tra thong tin dinh danh cua khach hang
        phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if(obj != null && obj.isNamed)
                {
                    addBillInfoOtherServices(billInfoService, hashMapBIS, serviceId, phoneNumber, log, textValues, nextFrm);
                }
                else if(billInfoService.total_amount != -100)
                {
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, StringConstUtil.SCB_NO_NAMED_POP_UP));
                }
                else{
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Không lấy được thông tin khách hàng."));
                }
                message.reply(Misc.getJsonArray(textValues));
                log.writeLog();
            }
        });
    }

    private void addBillInfoOtherServices(BillInfoService billInfoService, Map<String, BillInfoService> hashMapBIS, String serviceId, int phoneNumber, Common.BuildLog log, ArrayList<BillInfoService.TextValue> textValues, int nextFrm) {
        if (billInfoService.total_amount != -100) { //test avg
            hashMapBIS.put(serviceId + phoneNumber, billInfoService);
            log.add("Thông tin submitForm", billInfoService.toJsonObject().encodePrettily());

            textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));

        } else {
            String responseMessage = "Không lấy được thông tin khách hàng.";
            if(billInfoService.customer_info.size() > 0)
            {
                for(BillInfoService.TextValue textValue : billInfoService.customer_info)
                {
                    if(("responseMessage").equalsIgnoreCase(textValue.text))
                    {
                        responseMessage = textValue.value;
                    }
                }
            }

            textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
            textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, responseMessage));
        }
    }

    public void processSubmitFrmTopupCheckInforUseProxyRespose(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalForm
            , final Map<String, BillInfoService> hashMapBIS
    ) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;
        log.add("function", "processSubmitFrmTopupCheckInforUseProxyRespose");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);

        final String serviceCode = serviceId.toUpperCase();

        final ArrayList<BillInfoService.TextValue> textValues = new ArrayList<>();

        if (nextFrm == 0) {

            Misc.getViaCoreService(vertx, serviceId, isStoreApp, new Handler<ViaConnectorObj>() {

                @Override
                public void handle(ViaConnectorObj viaConnectorObj) {
                    if (viaConnectorObj.IsViaConnectorVerticle) {

                        ProxyRequest proxyRequest = ConnectorCommon.createCheckInforRequest(
                                "0" + phoneNumber, "", serviceCode, viaConnectorObj.BillPay, billId);
                        log.add("request content", proxyRequest.toString());

                        ConnectorCommon.RequestCheckBill(vertx, logger, phoneNumber, glbCfg, proxyRequest, viaConnectorObj.BusName, log, new Handler<BillInfoService>() {
                            @Override
                            public void handle(BillInfoService billInfoService) {

                                HashMap<String, BillInfoService.TextValue> hash = Misc.convertArrayTextValue(billInfoService.customer_info);
                                String responseMessage = hash.containsKey("responseMessage") ? hash.get("responseMessage").value : "Không lấy được thông tin khách hàng.";
                                int responseCode = hash.containsKey("responseCode") ? Integer.parseInt(hash.get("responseCode").value) : 1006;

                                if (responseCode == 0) { //response success
                                    hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                                    log.add("Infor submitForm", billInfoService.toJsonObject().encodePrettily());
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));

                                } else { //response fail, show message error from proxy
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, responseMessage));

                                }
                                message.reply(Misc.getJsonArray(textValues));
                                log.writeLog();

                            }
                        });
                    }
                }
            });
        } else {

            if (nextFrm == (totalForm - 1)) {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
            } else {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
            }

            message.reply(Misc.getJsonArray(textValues));
            log.writeLog();
        }
    }

    public void processSubmitFrmTopupSecondCheckInfor(final Message message
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalForm
            , final Map<String, BillInfoService> hashMapBIS
    ) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        if (!billId.equals("")) {
            billId2CheckInfor = billId;
        }
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;

        log.add("function", "processSubmitFrmTopupCheckInforUseProxyRespose");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);

        final String serviceCode = serviceId.toUpperCase();

        final ArrayList<BillInfoService.TextValue> textValues = new ArrayList<>();

        if (nextFrm == 0) {

            Misc.getViaCoreService(vertx, serviceId, isStoreApp, new Handler<ViaConnectorObj>() {

                @Override
                public void handle(ViaConnectorObj viaConnectorObj) {
                    if (viaConnectorObj.IsViaConnectorVerticle) {

                        ProxyRequest proxyRequest = ConnectorCommon.createCheckInforRequest(
                                "0" + phoneNumber, "", serviceCode, viaConnectorObj.BillPay, billId);
                        proxyRequest.setCustomerInfo("1");
                        log.add("request content check 1", proxyRequest.toString());

                        ConnectorCommon.RequestCheckBill(vertx, logger, phoneNumber, glbCfg, proxyRequest, viaConnectorObj.BusName, log, new Handler<BillInfoService>() {
                            @Override
                            public void handle(BillInfoService billInfoService) {

                                HashMap<String, BillInfoService.TextValue> hash = Misc.convertArrayTextValue(billInfoService.customer_info);
                                String responseMessage = hash.containsKey("responseMessage") ? hash.get("responseMessage").value : "Không lấy được thông tin khách hàng.";
                                int responseCode = hash.containsKey("responseCode") ? Integer.parseInt(hash.get("responseCode").value) : 1006;

                                if (responseCode == 0) { //response success
                                    hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                                    log.add("Infor submitForm 1", billInfoService.toJsonObject().encodePrettily());
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));

                                } else { //response fail, show message error from proxy
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, responseMessage));

                                }
                                message.reply(Misc.getJsonArray(textValues));
                                log.writeLog();

                            }
                        });
                    }
                }
            });
        } else if (nextFrm == 1) {
            Misc.getViaCoreService(vertx, serviceId, isStoreApp, new Handler<ViaConnectorObj>() {

                @Override
                public void handle(ViaConnectorObj viaConnectorObj) {
                    if (viaConnectorObj.IsViaConnectorVerticle) {

                        ProxyRequest proxyRequest = ConnectorCommon.createCheckInforRequest(
                                "0" + phoneNumber, "", serviceCode, viaConnectorObj.BillPay, billId);
                        proxyRequest.setCustomerInfo("2");
                        final String acc = hashMap.containsKey(Const.AppClient.Account) ? hashMap.get(Const.AppClient.Account) : "";
                        final String ht = hashMap.containsKey(Const.AppClient.FullName) ? hashMap.get(Const.AppClient.FullName) : "";
                        proxyRequest.setCustomerName(acc);
                        proxyRequest.setCustomerPhone(ht);
                        proxyRequest.setBillId(billId2CheckInfor);
                        log.add("request content check 2", proxyRequest.toString());

                        ConnectorCommon.RequestCheckBill(vertx, logger, phoneNumber, glbCfg, proxyRequest, viaConnectorObj.BusName, log, new Handler<BillInfoService>() {
                            @Override
                            public void handle(BillInfoService billInfoService) {

                                HashMap<String, BillInfoService.TextValue> hash = Misc.convertArrayTextValue(billInfoService.customer_info);
                                String responseMessage = hash.containsKey("responseMessage") ? hash.get("responseMessage").value : "Không lấy được thông tin khách hàng.";
                                int responseCode = hash.containsKey("responseCode") ? Integer.parseInt(hash.get("responseCode").value) : 1006;

                                if (responseCode == 0) { //response success
                                    if (hashMapBIS.containsKey(serviceId + phoneNumber)) {
                                        hashMapBIS.remove(serviceId + phoneNumber);
                                        hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                                    }

                                    log.add("Infor submitForm 2", billInfoService.toJsonObject().encodePrettily());
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));

                                } else { //response fail, show message error from proxy
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, responseMessage));

                                }
                                message.reply(Misc.getJsonArray(textValues));
                                log.writeLog();

                            }
                        });
                    }
                }
            });
        } else {

            if (nextFrm == (totalForm - 1)) {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0")); // dung tai day
            } else {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
            }

            message.reply(Misc.getJsonArray(textValues));
            log.writeLog();
        }
    }

    public void processSubmitFrmTopupCheckInforOTP(final Message message
            , final Map<String, String> hashMap
            , final String serviceId
            , final int phoneNumber, final int totalForm
            , final Map<String, BillInfoService> hashMapBIS
    ) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);

        final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        final int nextFrm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;
        log.add("function", "processSubmitFrmTopupCheckInfor");
        log.add("serviceid", serviceId);
        log.add("billid", billId);
        log.add("nextFrm", nextFrm);

        final String serviceCode = serviceId.toUpperCase();

        final ArrayList<BillInfoService.TextValue> textValues = new ArrayList<>();

        if (nextFrm == 0) {

            Misc.getViaCoreService(vertx, serviceId, isStoreApp, new Handler<ViaConnectorObj>() {

                @Override
                public void handle(ViaConnectorObj viaConnectorObj) {
                    if (viaConnectorObj.IsViaConnectorVerticle) {

                        ProxyRequest proxyRequest = ConnectorCommon.createCheckInforRequest(
                                "0" + phoneNumber, "", serviceCode, viaConnectorObj.BillPay, billId);
                        if (proxyRequest.getServiceCode().equalsIgnoreCase("survey")) {
                            proxyRequest = setValueSurveytoReques(hashMap, proxyRequest);
                        }

                        ConnectorCommon.RequestCheckBill(vertx, logger, phoneNumber, glbCfg, proxyRequest, viaConnectorObj.BusName, log, new Handler<BillInfoService>() {
                            @Override
                            public void handle(BillInfoService billInfoService) {
                                HashMap<String, BillInfoService.TextValue> hash = Misc.convertArrayTextValue(billInfoService.customer_info);
                                String responseMessage = hash.containsKey("responseMessage") ? hash.get("responseMessage").value : "Không lấy được thông tin khách hàng.";
                                int responseCode = hash.containsKey("responseCode") ? Integer.parseInt(hash.get("responseCode").value) : 1006;
                                if (responseCode == 0) { //response success
                                    hashMapBIS.put(serviceId + phoneNumber, billInfoService);
                                    log.add("Infor submitForm ", billInfoService.toJsonObject().encodePrettily());
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));

                                } else { //response fail, show message error from proxy
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                                    textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, responseMessage));

                                }
                                message.reply(Misc.getJsonArray(textValues));
                                log.writeLog();

                            }
                        });
                    }
                }
            });
        } else {
            if (nextFrm == (totalForm - 1)) {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, "0"));
                textValues.add(new BillInfoService.TextValue(Const.AppClient.Error, "Bạn đã lấy mã OTP thành công!"));
            } else {
                textValues.add(new BillInfoService.TextValue(Const.AppClient.NextForm, String.valueOf(nextFrm + 1)));
                log.add("Infor nextFrm " + nextFrm, "");
            }

            message.reply(Misc.getJsonArray(textValues));
            log.writeLog();
        }
    }

}
