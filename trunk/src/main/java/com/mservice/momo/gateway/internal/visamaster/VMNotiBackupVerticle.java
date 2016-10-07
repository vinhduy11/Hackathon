package com.mservice.momo.gateway.internal.visamaster;

import com.mservice.momo.data.*;
import com.mservice.momo.data.ironmanpromote.IronManPromoGiftDB;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.permission.Cryption;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import com.mservice.momo.vertx.visampointpromo.VMCardIdCardNumberDB;
import com.mservice.visa.entity.CardInfo;
import com.mservice.visa.entity.VisaRequest;
import com.mservice.visa.entity.VisaRequestType;
import com.mservice.visa.entity.VisaResponse;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by khoanguyen on 09/05/2016.
 */
public class VMNotiBackupVerticle extends Verticle{

    private static final String SuccessTemplate = "Quý khách đã nạp thành công số tiền %sđ từ thẻ %s.";
    private static final String FailedTemplate = "Quý khách đã nạp không thành công số tiền %sđ từ thẻ %s.";
    public String VISA_GROUP = "";
    public String VISA_CAPSET_ID = "";
    public String VISA_UPPER_LIMIT = "";
    private boolean isStoreApp;
    private Logger logger;
    private JsonObject glbCfg;
    private EventBus eventBus;
    private Card card;
    private String sbsBusAddress ="";
    private TransDb tranDb;
    private PhonesDb phonesDb;
    private VMCardIdCardNumberDB vmCardIdCardNumberDB;
    private GroupManageDb groupManageDb;
    private IronManPromoGiftDB ironManPromoGiftDB;
    private boolean isActiveIronManPromo;
    private JsonObject jsonIronPromo;

    private JsonObject jsonIronManPlus;
    private boolean isActiveIronManPlusPromo;
    private PromotionProcess promotionProcess;
    private String HOST_ADDRESS = "";
    private int PORT = 0;
    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    @Override
    public void start() {

        this.eventBus = vertx.eventBus();
        this.logger = container.logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);
        this.vmCardIdCardNumberDB = new VMCardIdCardNumberDB(vertx, logger);
        this.groupManageDb = new GroupManageDb(vertx, logger);

        JsonObject sbsCfg = glbCfg.getObject("cybersource", null);
        if (sbsCfg == null) {
            logger.info("VISA MASTER SYBERSOURCE LISTEN NOTIFY NOT CONFIGURED ON THIS MACHINE");
            return;
        }
        VISA_GROUP = sbsCfg.getString("visa_group");
        VISA_CAPSET_ID = sbsCfg.getString("visa_capset_id");
        VISA_UPPER_LIMIT = sbsCfg.getString("visa_upper_limit");
        boolean enable = sbsCfg.getBoolean("enable", false);
        if (!enable) {
            logger.info("VISA MASTER SYBERSOURCE LISTEN NOTIFY NOT ENABLE ON THIS MACHINE");
            return;
        }
        card = new Card(vertx.eventBus(), logger);
        ironManPromoGiftDB = new IronManPromoGiftDB(vertx, logger);
        jsonIronManPlus = glbCfg.getObject(StringConstUtil.IronManPromoPlus.JSON_OBJECT, new JsonObject());
        isActiveIronManPlusPromo = jsonIronManPlus.getBoolean(StringConstUtil.IronManPromoPlus.IS_ACTIVE, false);
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);
        /*"cybersource":{
            "enable":true,
                    "min_value":0,
                    "proxyBusAddress":"cyberSourceVerticle"
        }*/

        sbsBusAddress = sbsCfg.getString("proxyBusAddress", "proxyBusAddress");
        jsonIronPromo = glbCfg.getObject(StringConstUtil.IronManPromo.JSON_OBJECT, new JsonObject());
        isActiveIronManPromo = jsonIronPromo.getBoolean(StringConstUtil.IronManPromo.IS_ACTIVE, false);
        logger.info("VISA MASTER SYBERSOURCE LISTEN NOTIFY STARTED ON THIS MACHINE");
        this.logger = getContainer().logger();
        this.glbCfg = container.config();

//        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        // jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());
        loadConfig(connectorHTTPPostPathDb, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                createServer();
            }
        });


    }

    private void createServer() {
        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                                    @Override
                                    public void handle(final HttpServerRequest request) {
                                        logger.info("[WebService] [" + request.method() + "] " + " uri: " + request.uri() + " path: " + request.path());
                                        final JsonObject joReply = new JsonObject();
                                        final String path = request.path();
                                        final Cryption cryption = new Cryption(glbCfg, logger);
                                        for (int i = 0; i < request.headers().size(); i++) {
                                            logger.info("key " + request.headers().entries().get(i).getKey() + " value " + request.headers().entries().get(i).getValue());
                                        }
                                        if (request.method().equalsIgnoreCase("POST")) {
                                            if ("/visanoti".equalsIgnoreCase(path)) {
                                                request.bodyHandler(new Handler<Buffer>() {
                                                                        @Override
                                                                        public void handle(final Buffer buffer) {
                                                                            logger.info(buffer.getBytes());
                                                                            logger.info(buffer.toString());
                                                                            final Common.BuildLog log = new Common.BuildLog(logger);
                                                                            try {
                                                                                if (Misc.isValidJsonObject(buffer.toString())) {
                                                                                    JsonObject proxyJson = new JsonObject(buffer.toString());
                                                                                    VisaResponse visaResponse = new VisaResponse(proxyJson);
                                                                                    VisaRequestType visaRequestType = visaResponse.getVisaRequest().getRequestType();

                                                                                    switch (visaRequestType) {
                                                                                        case CASH_IN:
                                                                                            doNotifyCashInVM(visaResponse);
                                                                                            break;
                                                                                        case CREATE_TOKEN:
                                                                                            sendVMNotiCreateToken(proxyJson, request);
                                                                                            break;
                                                                                        default:
                                                                                            break;
                                                                                    }
                                                                                    request.response().end(new JsonObject().putString("desc", "Success").toString());
                                                                                } //END data.length > 4
                                                                                else {
                                                                                    logger.info("Loi du lieu gui sang thieu hoac sai format");
                                                                                    logger.info("Thong tin nhan tu app sai");
                                                                                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                                                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                                                                    request.response().end(joReply.toString());
                                                                                }
                                                                            } catch (Exception e) {
                                                                                e.printStackTrace();
                                                                                //Error tu app, response ve luon
                                                                                //JsonObject joReply = new JsonObject();
                                                                                logger.info("Thong tin nhan tu app sai");
                                                                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                                                                request.response().end(joReply.toString());
                                                                            }
                                                                        }
                                                                    }
                                                );
                                            }
                                        } else {
                                            request.response().end("got request");
                                        }
                                    }
                                }
                ).listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
                            @Override
                            public void handle(AsyncResult<HttpServer> event) {
                                if (event.succeeded()) {
                                    logger.info("VMNotiBackupVerticle's listening on " + HOST_ADDRESS + ":" + PORT);
                                }
                                else {
                                    logger.info("VMNotiBackupVerticle CAN NOT listen on " + HOST_ADDRESS + ":" + PORT);
                                }
                            }
                        }
                );
    }

    private void loadConfig(ConnectorHTTPPostPathDb connectorHTTPPostPathDb, final Logger logger, final Handler<JsonObject> callback)
    {
        logger.info("FUNCTION " + "loadConfig VMNotiBackupVerticle");
        if(connectorHTTPPostPathDb == null)
        {
            connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        }

        connectorHTTPPostPathDb.findOne("visanoti", new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj pathObj) {
                if(pathObj == null)
                {
                    logger.info("pathObj = null");
                    HOST_ADDRESS = "172.16.43.22";
                    PORT = 8092;
                    JsonObject joCallBack = new JsonObject().putString("HOST_ADDRESS", HOST_ADDRESS).putNumber("PORT", PORT);
                    callback.handle(joCallBack);
                }
                else {
                    HOST_ADDRESS = pathObj.host;
                    PORT = pathObj.port;
                    JsonObject joCallBack = new JsonObject().putString("HOST_ADDRESS", HOST_ADDRESS).putNumber("PORT", PORT);
                    callback.handle(joCallBack);
                }

            }
        });
    }

    private void sendVMNotiCreateToken(JsonObject json, HttpServerRequest request) {

        VisaResponse visaResponse = null;
        if (json == null) {
            visaResponse = new VisaResponse();
            visaResponse.setResultCode(100);
            visaResponse.setDescription("Không lấy được dữ liệu");
            request.response().end(visaResponse.getJsonObject().toString());
            return;
        }

        visaResponse = new VisaResponse(json);
        final long phoneNumber = DataUtil.stringToVnPhoneNumber(visaResponse.getVisaRequest().getPhoneNumber());
        if (phoneNumber <= 0) {
            visaResponse.setResultCode(100);
            visaResponse.setDescription("Số điện thoại không hợp lệ : " + visaResponse.getVisaRequest().getPhoneNumber());
            request.response().end(visaResponse.getJsonObject().toString());
        }
        else if ("".equalsIgnoreCase(visaResponse.getHeader())) {
            visaResponse.setResultCode(100);
            visaResponse.setDescription("Header is empty");
            request.response().end(visaResponse.getJsonObject().toString());
        }
        else if ("".equalsIgnoreCase(visaResponse.getContent())) {
            visaResponse.setResultCode(100);
            visaResponse.setDescription("Content is empty");
            request.response().end(visaResponse.getJsonObject().toString());
        }
        else
        {
            final com.mservice.momo.vertx.models.Notification noti = new com.mservice.momo.vertx.models.Notification();
            long curTime = System.currentTimeMillis();
            noti.receiverNumber = DataUtil.strToInt("" + phoneNumber);
            noti.caption = visaResponse.getHeader();
            noti.body = visaResponse.getContent();
            noti.bodyIOS = noti.body;
            noti.priority = 2;
            noti.tranId = curTime;
            noti.time = curTime;
            noti.cmdId = curTime;
            noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
            noti.status = com.mservice.momo.vertx.models.Notification.STATUS_DISPLAY;
            noti.sms = "";
            //write log
            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(phoneNumber + "vm");
            log.add("func", "sendVMNotiCreateToken");
            //define the value for client to show popup when receive the notification
            JsonObject jo = new JsonObject();
            jo.putString(VMConst.partnerCode, "sbs");
            List<CardInfo> cardInfos = visaResponse.getCardInfos();
            String cardId = "";
            if (cardInfos.size() > 0) {
                cardId = cardInfos.get(0).getCardId();
            }

            //BEGIN 0000000002 INSERT MORE INFO FOR VISAMASTER
            if (visaResponse.getVisaRequest() != null) {
                jo.putString(VMConst.cardId, cardId);
                jo.putString(VMConst.cardNumber, visaResponse.getVisaRequest().getCardNumber());
                jo.putString(VMConst.cardType, VMCardType.getCodeCardType(visaResponse.getVisaRequest().getCardType()));
                jo.putString(VMConst.cardHolerName, visaResponse.getVisaRequest().getCardHolder());
                log.add("vmResultCode", visaResponse.getResultCode());
                if (visaResponse.getResultCode() == 0) {
                    //Kiem tra khuyen mai map the
                    log.add("desc noti visa " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Kiem tra tra thuong visa gioi thieu ban be");
                    log.add("visa response " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, visaResponse.getJsonObject());
//                    promotionProcess.executeReferralPromotion(StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.BANK_MAPPING, null, visaResponse, null, null, log, new JsonObject());
                    phonesDb.getPhoneObjInfo(DataUtil.strToInt(visaResponse.getVisaRequest().getPhoneNumber()), new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(PhonesDb.Obj obj) {
                            if (obj != null && !obj.isNamed) {
                                //todo xem nhu thanh cong --> set nhom visa cho thang nay
                                log.add("func", "setGroupVisa");
                                try {
                                    setGroupVisa("0" + phoneNumber);
                                } catch (Exception ex) {
                                    log.add("desc", "set group null " + ex.getMessage());
                                }

                            }
                        }
                    });
                }
            }
            //END 0000000002 INSERT MORE INFO FOR VISAMASTER
            noti.extra = jo.toString();
            //send noti

            vertx.eventBus().send(AppConstant.NotificationVerticle_VISA_ADDRESS_SEND_NOTIFICATION, noti.toFullJsonObject(), new Handler<Message<Boolean>>() {
                @Override
                public void handle(Message<Boolean> visaMessRep) {
                    if(visaMessRep != null && !visaMessRep.body())
                    {
                        Misc.sendNoti(vertx, noti);
                    }
                    else if(visaMessRep == null) {
                        Misc.sendNoti(vertx, noti);
                    }
                }
            });
//            Misc.sendNoti(vertx, noti);

            //reply to client
            visaResponse.setResultCode(0);
            request.response().end(visaResponse.getJsonObject().toString());

            log.add("header", visaResponse.getHeader());
            log.add("content", visaResponse.getContent());
            final int resultCode = visaResponse.getResultCode();
            final String deviceImei = visaResponse.getVisaRequest().getExtraValue().getString(StringConstUtil.DEVICE_IMEI, "");
            final VisaResponse visaResponse_tmp = visaResponse;
            AtomicInteger count = new AtomicInteger(0);
            if(resultCode == 0)
            {
                getCardList(count, phoneNumber, log, visaResponse.getCardInfos().get(0).getCardNumber(), new Handler<String>() {
                    @Override
                    public void handle(String cardHash) {
                        log.add("cardHash", cardHash);
                        log.add("result code", resultCode);
                        if(!"".equalsIgnoreCase(cardHash) && cardHash != null && resultCode == 0)
                        {
                            log.add("promotionProcess", "executeSCBPromotionProcess");
                            // TODO: 29/06/2016 change thư way to call promotion process
                            promotionProcess.executeSCBPromotionProcess("0" + phoneNumber, cardHash, visaResponse_tmp.getCardInfos().get(0).getCardNumber(), deviceImei, new JsonObject(), StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION.BANK_MAPPING, log);
//                            JsonObject values = new JsonObject();
//                            values.putString("phoneNumber", "0" + phoneNumber);
//                            values.putString("cardInfo", cardHash);
//                            values.putString("bankAcc", visaResponse_tmp.getCardInfos().get(0).getCardNumber());
//                            values.putString("imei", deviceImei);
//                            values.putObject("jsonExtra", new JsonObject());
//                            values.putString("msgType", StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION.BANK_MAPPING.toString());
//
//                            HttpClient client = vertx.createHttpClient();
//
//                            Handler<HttpClientResponse> handle = new Handler<HttpClientResponse>() {
//                                @Override
//                                public void handle(HttpClientResponse response) {
//                                    log.add("Response received",response.statusCode());
//                                }
//                            };
//                            String path = "/promotion?" + Promo.PromoReq.COMMAND + "=executeSCB";
//                            HttpClientRequest httpClientRequest = client.setHost("10.10.10.143").setPort(8449).post(path, handle);
//                            buildParams(httpClientRequest, values, log);
                        }
                        else {
                            log.writeLog();
                        }
                    }
                });

            }
        }

    }

    private void doNotifyCashInVM(VisaResponse visaResponse){

        final int phoneNumber = DataUtil.strToInt(visaResponse.getVisaRequest().getPhoneNumber());

        int resultCode = visaResponse.getResultCode();
        long tranId = visaResponse.getVisaRequest().getCoreTransId();

        //noti client
        final com.mservice.momo.vertx.models.Notification noti = new com.mservice.momo.vertx.models.Notification();
        long curTime = System.currentTimeMillis();
        noti.receiverNumber = DataUtil.strToInt("" + phoneNumber) ;
        noti.caption= visaResponse.getHeader();
        noti.body = visaResponse.getContent();
        noti.bodyIOS =noti.body;
        noti.priority = 2;
        noti.tranId = tranId;
        noti.time = curTime;
        noti.cmdId = curTime;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.status = com.mservice.momo.vertx.models.Notification.STATUS_DISPLAY;
        noti.sms = "";

        //define the value for client to show popup when receive the notification
        JsonObject jo = new JsonObject();
        jo.putString(VMConst.partnerCode,"sbs");
        String success = resultCode == 0 ? "true" : "false";
        jo.putString("result",success);
        noti.extra = jo.toString();

        //tran
//        final String cardId = visaResponse.getVisaRequest().getCardId();
        final String bankAcc = visaResponse.getVisaRequest().getCardNumber();
//        String s = "";
//        if(bankAcc != null && !bankAcc.equalsIgnoreCase(""))
//        {
//            s = bankAcc.substring(bankAcc.length()-4);
//        }
//        else
//        {
//            s = cardId.substring(cardId.length()-4);
//        }
        String comment;
        if(resultCode == 0){
            comment = String.format(SuccessTemplate
                    ,Misc.formatAmount(visaResponse.getVisaRequest().getAmount()).replace(",",".")
                    ,bankAcc)
                    + "\n" + "Phí dịch vụ: " + Misc.formatAmount(visaResponse.getVisaRequest().getFee()).replaceAll(",", ".") + "đ";;
        }else{
            comment = String.format(FailedTemplate
                    ,Misc.formatAmount(visaResponse.getVisaRequest().getAmount()).replace(",",".")
                    ,bankAcc);
        }

        VisaRequest visaRequest = visaResponse.getVisaRequest();
        long lastTime_tmp = 0;
        long lastTranId_tmp = 0;
        String tranType = "";
        String serviceId = "";
        if(visaRequest != null)
        {
            JsonObject jsonExtra = visaRequest.getExtraValue();
            tranType = jsonExtra.getString("tranType", "");
            serviceId = jsonExtra.getString("serviceId", "");
            int M2M_VALUE = 6;
            int TRANSFER_MONEY_TO_PLACE_VALUE = 13;
            int BANK_IN_VALUE = 1;
            int BANK_NET_VERIFY_OTP_VALUE = 11;
            int MOMO_TO_BANK_MANUAL_VALUE = 26;
            int BANK_OUT_VALUE = 2;

            if (DataUtil.strToInt(tranType) != M2M_VALUE || DataUtil.strToInt(tranType) != TRANSFER_MONEY_TO_PLACE_VALUE
                    || DataUtil.strToInt(tranType) != BANK_IN_VALUE || DataUtil.strToInt(tranType) != BANK_NET_VERIFY_OTP_VALUE
                    || DataUtil.strToInt(tranType) != MOMO_TO_BANK_MANUAL_VALUE || DataUtil.strToInt(tranType) != BANK_OUT_VALUE
                    )
            {
                lastTime_tmp = curTime;
                lastTranId_tmp = tranId;
            }
            else{
                tranType = "-1";
            }
        }
        final long lastTime = lastTime_tmp;
        final long lastTranId = lastTranId_tmp;
        final String cardCheckSum = visaResponse.getCardChecksum();
        final VisaResponse visaResponse1 = visaResponse;
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("checkSum", cardCheckSum);
        log.writeLog();

        final JsonArray jsonArray = new JsonArray();
        JsonObject jsonShare = new JsonObject();
        jsonShare.putString("cardlbl", bankAcc);
        jsonShare.putString("cardtype", VMCardType.getCodeCardType(visaResponse.getVisaRequest().getCardType()));
        jsonShare.putString("cardnumbervisa", cardCheckSum);
        jsonShare.putString("tranType", tranType);

        jsonArray.add(jsonShare);

        JsonObject kvpObject = new JsonObject();
        kvpObject.putString("serviceId", serviceId);
        String ownerName = visaResponse.getVisaRequest().getCardNumber() == null ? "" :visaResponse.getVisaRequest().getCardNumber();
        final TranObj mainObj = new TranObj();
        mainObj.owner_number = phoneNumber;
        mainObj.tranId = tranId;
        mainObj.clientTime = curTime;
        mainObj.ackTime = curTime;
        mainObj.finishTime = curTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = visaResponse.getVisaRequest().getAmount();
        mainObj.status = resultCode == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL;
        mainObj.error = resultCode;
        mainObj.cmdId = curTime;
        mainObj.billId = "";
        mainObj.io = 1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MASTERCARD_VALUE;
        mainObj.tranType    = MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE;
        mainObj.owner_name  = ownerName;
        mainObj.comment     = comment;
        mainObj.category    = 8;
        mainObj.share       = jsonArray;
        mainObj.kvp         = kvpObject;
        tranDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if(result){
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
//                    Misc.sendNoti(vertx, noti);
                    vertx.eventBus().send(AppConstant.NotificationVerticle_VISA_ADDRESS_SEND_NOTIFICATION, noti.toFullJsonObject(), new Handler<Message<Boolean>>() {
                        @Override
                        public void handle(Message<Boolean> visaMessRep) {
                            if(visaMessRep != null && !visaMessRep.body())
                            {
                                Misc.sendNoti(vertx, noti);
                            }
                            else if(visaMessRep == null) {
                                Misc.sendNoti(vertx, noti);
                            }
                        }
                    });
                }
            }
        });

        if(resultCode == 0){

            log.add("func", "Kiem tra khuyen mai");
            promotionProcess.executePromotion("", MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE, "", mainObj.owner_number,
                    tranId, mainObj.amount , null, "sbs", null, 0, 1, log, new JsonObject(), new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {

                        }
                    });
//            promotionProcess.getUserInfoToCheckPromoProgram("", "0" + phoneNumber, null, tranId, MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE, visaResponse.getVisaRequest().getAmount(), StringConstUtil.WomanNationalField.PROGRAM, null
//                    , new JsonObject().putString(StringConstUtil.RollBack50Percent.CARD_CHECK_SUM,cardCheckSum));
//            JsonObject joExtra = new JsonObject();
//            joExtra.putNumber(StringConstUtil.AMOUNT, visaResponse.getVisaRequest().getAmount());
//            joExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, tranId);
//            joExtra.putString(StringConstUtil.BANK_CODE, cardCheckSum);
//            promotionProcess.excuteAcquireBinhTanUserPromotion("0" + phoneNumber, log, null, null, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joExtra);

        }
    }

    //BEGIN IRON_PLUS 0000000056
    private void updateIronManVoucher(final String phoneNumber, final Common.BuildLog log,final JsonArray giftArray)
    {
        log.add("func", "updateIronManVoucher");
        ironManPromoGiftDB.findOne(phoneNumber, new Handler<IronManPromoGiftDB.Obj>() {
            @Override
            public void handle(IronManPromoGiftDB.Obj ironmanGiftObj) {
                log.add("ironmanGiftObj", ironmanGiftObj);
                if(ironmanGiftObj != null)
                {
                    JsonObject joUpdate = new JsonObject();
                    for(int i = 0; i < giftArray.size(); i++)
                    {
                        if(ironmanGiftObj.gift_id_2.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_2, true);
                        }
                        else if(ironmanGiftObj.gift_id_3.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_3, true);
                        }
                        else if(ironmanGiftObj.gift_id_4.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_4, true);
                        }
                        else if(ironmanGiftObj.gift_id_5.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_5, true);
                        }
                        else if(ironmanGiftObj.gift_id_6.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_6, true);
                        }
                        else if(ironmanGiftObj.gift_id_7.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_7, true);
                        }
                        else if(ironmanGiftObj.gift_id_8.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_8, true);
                        }
                        else if(ironmanGiftObj.gift_id_9.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_9, true);
                        }
                        else if(ironmanGiftObj.gift_id_10.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_10, true);
                        }
                        else if(ironmanGiftObj.gift_id_11.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_11, true);
                        }
                    }
                    joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_12, true);
                    ironManPromoGiftDB.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    });
                }
            }
        });
    }
    //END 0000000056 IronMan_PLUS
    private void setGroupVisa(final String phoneNumber)
    {
        final Buffer buffer = MomoMessage.buildBuffer(SoapProto.MsgType.MAP_AGENT_TO_VISA_GROUP_VALUE
                , 0
                , DataUtil.strToInt(phoneNumber)
                , SoapProto.ZaloGroup.newBuilder()
                .setZaloGroup(VISA_GROUP)
                .setZaloCapsetId(VISA_CAPSET_ID)
                .setZaloUpperLimit(VISA_UPPER_LIMIT)
                .build().toByteArray());
        GroupManageDb.Obj groupObj = new GroupManageDb.Obj();
        groupObj.number = phoneNumber;
        groupObj.groupid = VISA_GROUP;
        groupManageDb.insert(groupObj, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {
                vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> reply) {
                        logger.info(DataUtil.strToInt(phoneNumber) + ",set group visa " + reply.body());
                    }
                });
            }
        });

    }
    private JsonObject buildCardInfo(CardInfo ci) {

        JsonObject o = new JsonObject();
        o.putString(colName.CardDBCols.CARD_HOLDER_NAME
                , ci.getCardHolder() == null ? "" : ci.getCardHolder());
        o.putString(colName.CardDBCols.CARD_HOLDER_NUMBER
                , ci.getCardNumber() == null ? "" : ci.getCardNumber());

        o.putString(colName.CardDBCols.CARD_HOLDER_YEAR
                , ci.getExpiredDate() == null ? "" : ci.getExpiredDate());

        o.putString(colName.CardDBCols.CARD_HOLDER_MONTH,"");
        o.putString(colName.CardDBCols.BANK_NAME,VMCardType.convertBankNameFromCardTypeId(VMCardType.getCodeCardType(ci.getType())));
        o.putString(colName.CardDBCols.BANKID,"sbs");
        o.putBoolean(colName.CardDBCols.DELETED, false);
        o.putNumber(colName.CardDBCols.LAST_SYNC_TIME, System.currentTimeMillis());
        o.putString(colName.CardDBCols.CARD_TYPE, VMCardType.getCodeCardType(ci.getType()));
        o.putString(colName.CardDBCols.CARD_ID,ci.getCardId() == null ? "" : ci.getCardId());
        o.putNumber(colName.CardDBCols.ROW_ID,System.currentTimeMillis());
        o.putString(colName.CardDBCols.CARD_CHECKSUM, ci.getHashString());
        return o;
    }


    public void getCardList(final AtomicInteger count, final long phoneNumber,final Common.BuildLog log,final String bankAcc, final Handler<String> callback)
    {
        if(count.intValue() < 3)
        {
            VMRequest.getCardList(vertx
                    , sbsBusAddress
                    , "0" +phoneNumber, "web", log, new Handler<List<CardInfo>>() {
                @Override
                public void handle(List<CardInfo> cardInfos) {
                    if (cardInfos != null && cardInfos.size() > 0) {
                        boolean isCallback = false;
                        ArrayList<JsonObject> jsonArray = new ArrayList<JsonObject>();
                        for (int i = 0; i < cardInfos.size(); i++) {

                                /*
                                optional string card_holder_year    =1; // cardExpired
                                optional string card_holder_number=3; // cardNumber
                                optional string card_holder_name    =4; // cardHolder
                                optional string bankId                =5; // sbs
                                optional string cardId           = 16; id cua dong
                                optional string type             = 17;
                                */
                            CardInfo ci = cardInfos.get(i);
                            JsonObject joCardInfo = buildCardInfo(ci);
                            jsonArray.add(joCardInfo);
                            log.add("bankAcc", bankAcc.trim());
                            log.add("ci.getCardNumber", ci.getCardNumber().trim());
                            if(bankAcc.trim().equalsIgnoreCase(ci.getCardNumber().trim()))
                            {
                                log.add("isCallback", ci.getHashString());
                                isCallback = true;
                                callback.handle(ci.getHashString());
                            }
                        }
                        if(!isCallback)
                        {
                            callback.handle("");
                        }
                        log.add("cardInfo", jsonArray);
                        for (Object o : jsonArray) {
                            card.upsertVisaMaster(DataUtil.strToInt(phoneNumber + ""), (JsonObject) o, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                    log.add("upsertVisaMaster ----------------------------", aBoolean);
                                    log.writeLog();
                                }
                            });
                        }

                    }
                    else
                    {
                        count.incrementAndGet();
                        vertx.setTimer(5000L, new Handler<Long>() {
                            @Override
                            public void handle(Long event) {
                                getCardList(count, phoneNumber, log, bankAcc, callback);
                            }
                        });

                    }
                }
            });
        }
        else
        {
            log.add("thong tin the", "Khong lay duoc thong tin the.");
            log.writeLog();
            callback.handle("");
        }
    }

    /**
     *
     * @param req
     * @param jo
     * @param log
     */
    public void buildParams(HttpClientRequest req, JsonObject jo, Common.BuildLog log) {

        Buffer buffer = null;
        buffer = new Buffer(jo.toString());
        req.putHeader("Content-Length", buffer.length() + "");
        req.end(buffer);

    }
}
