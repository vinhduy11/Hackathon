package com.mservice.momo.gateway.internal.visamaster;

import com.mservice.momo.data.FeeDb;
import com.mservice.momo.gateway.internal.connectorproxy.ConnectorCommon;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.visa.entity.CardInfo;
import com.mservice.visa.entity.VisaRequest;
import com.mservice.visa.entity.VisaRequestFactory;
import com.mservice.visa.entity.VisaResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

/**
 * Created by concu on 3/18/15.
 */
public class VMRequest {

//    public static void getCardList(Vertx vertx
//            ,String busAddress
//            ,String phoneNumber
//            ,String channel
//            ,final Common.BuildLog log
//            ,final Handler<List<CardInfo>> callback) {
//
//        log.add("func", "getCardList");
//        log.add("number", phoneNumber);
//
//        VisaRequest visaRequest = VisaRequestFactory.getCardListRequest(phoneNumber
//                , System.currentTimeMillis());
//
//
//        //keyvaluepair.start
//        visaRequest.addExtraValue("client","backend"); // bankend gui yeu cau
//        visaRequest.addExtraValue("chanel",channel); // phan biet kenh web/app
//        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms// khong can nhan sms
//        //billpay  cashin
//
//        vertx.eventBus().sendWithTimeout(busAddress
//                , visaRequest.getJsonObject()
//                , 450000, new Handler<AsyncResult<Message<JsonObject>>>() {
//            @Override
//            public void handle(AsyncResult<Message<JsonObject>> asyncResult) {
//
//                VisaResponse visaResponse;
//                //thanh cong
//                if (asyncResult.succeeded()) {
//                    JsonObject joRes = asyncResult.result().body();
//                    visaResponse = new VisaResponse(joRes);
//                } else if (asyncResult.failed()) {
//                    //that bai --> default timeout
//                    visaResponse = null;
//                    log.add("desc", "timeout");
//                } else {
//                    //that bai khac
//                    visaResponse = null;
//                    log.add("desc", "that bai khac");
//                }
//
//                if (visaResponse == null) {
//                    callback.handle(null);
//                    return;
//                }
//                callback.handle(visaResponse.getCardInfos());
//            }
//        });
//    }

    public static void getCardList(Vertx vertx
            ,String busAddress
            ,String phoneNumber
            ,String channel
            ,final Common.BuildLog log
            ,final Handler<List<CardInfo>> callback) {

        log.add("func", "getCardList");
        log.add("number", phoneNumber);

        VisaRequest visaRequest = VisaRequestFactory.getCardListRequest(phoneNumber
                , System.currentTimeMillis());


        //keyvaluepair.start
        visaRequest.addExtraValue("client","backend"); // bankend gui yeu cau
        visaRequest.addExtraValue("chanel",channel); // phan biet kenh web/app
        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms// khong can nhan sms
        //billpay  cashin

        ConnectorCommon.requestVisaToConnectorViaHTTP(vertx, phoneNumber, visaRequest.getJsonObject(), "visa", log, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joResponse) {
                VisaResponse visaResponse;
                int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                String des = joResponse.getString(StringConstUtil.DESCRIPTION, "");
                log.add("desc err getCardList Visa", err);
                log.add("desc visa", des );
                if(err != 0)
                {
                    visaResponse = null;
                    log.add("desc", "timeout");
                }
                else {
                    visaResponse = new VisaResponse(joResponse.getObject(BankHelperVerticle.DATA, new JsonObject()));
                }

                if(visaResponse == null)
                {
                    callback.handle(null);
                }
                else {
                    callback.handle(visaResponse.getCardInfos());
                }
            }
        });
    }

    //BEGIN: A.KHOA
    //Insert delete VM Card function.

//    public static void deleteVMCard(Vertx vertx
//            , String busAddress
//            , String phoneNumber
//            , String cardId
//            , String channel
//            , final Common.BuildLog log
//            , final Handler<VisaResponse> callback) {
//
//        log.add("func: ", "deleteVMCard");
//        log.add("Phone number: ", phoneNumber);
//
//        //Set TransID again after get full code.
//        VisaRequest visaRequest = VisaRequestFactory.getDeleteTokenRequest(phoneNumber,
//                System.currentTimeMillis(), cardId);
//
//
//        //keyvaluepair.start
//        //keyvaluepair.start
//        visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
//        visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
//        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms// khong can nhan sms
//        //billpay  cashin
//        //keyvaluepair.end
//
//
//        // Assign trans ID
//        vertx.eventBus().sendWithTimeout(busAddress
//                , visaRequest.getJsonObject()
//                , 450000, new Handler<AsyncResult<Message<JsonObject>>>() {
//            @Override
//            public void handle(AsyncResult<Message<JsonObject>> asyncResult) {
//
//                VisaResponse visaResponse;
//                if (asyncResult.succeeded()) {
//                    JsonObject json = asyncResult.result().body();
//                    visaResponse = new VisaResponse(json);
//                    log.add("desc", "success");
//                } else if (asyncResult.failed()) {
//                    visaResponse = null;
//                    log.add("desc", "timeout");
//                } else {
//                    visaResponse = null;
//                    log.add("desc", "failed");
//                }
//                //thanh cong
//                callback.handle(visaResponse);
//            }
//        });
//    }

    public static void deleteVMCard(Vertx vertx
            , String busAddress
            , String phoneNumber
            , String cardId
            , String channel
            , final Common.BuildLog log
            , final Handler<VisaResponse> callback) {

        log.add("func: ", "deleteVMCard");
        log.add("Phone number: ", phoneNumber);

        //Set TransID again after get full code.
        VisaRequest visaRequest = VisaRequestFactory.getDeleteTokenRequest(phoneNumber,
                System.currentTimeMillis(), cardId);


        //keyvaluepair.start
        //keyvaluepair.start
        visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
        visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms// khong can nhan sms
        //billpay  cashin
        //keyvaluepair.end


        // Assign trans ID
        ConnectorCommon.requestVisaToConnectorViaHTTP(vertx, phoneNumber, visaRequest.getJsonObject(), "visa", log, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joResponse) {

                VisaResponse visaResponse;
                int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                String des = joResponse.getString(StringConstUtil.DESCRIPTION, "");
                log.add("desc err deleteVMCard Visa", err);
                log.add("desc visa", des );
                if(err != 0)
                {
                    visaResponse = null;
                    log.add("desc", "timeout");
                }
                else {
                    visaResponse = new VisaResponse(joResponse.getObject(BankHelperVerticle.DATA, new JsonObject()));
                }
                //thanh cong
                callback.handle(visaResponse);
            }
        });
    }
    //Insert Cash In VM Card function.
    /**
     *
     * @param vertx
     * @param busAddress
     * @param phoneNumber
     * @param cardId
     * @param tranAmount
     * @param feeType
     * @param callback
     */
//    public static void doVisaMasterCashIn(final Vertx vertx,
//                                        final String busAddress,
//                                        final SockData data,
//                                        final String phoneNumber,
//                                        final String cardId,
//                                        final String cardLbl,
//                                        final String cardType,
//                                        final long tranAmount,
//                                        final int feeType,
//                                        final long serviceFee,
//                                        final String channel,
//                                        final String emailDesc,
//                                        final Common.BuildLog log,
//                                        final String descriptionService,
//                                        final int tranType,
//                                        final String serviceId,
//                                        final Handler<VisaResponse> callback) {
//
//        log.add("func", "doVisaMasterCashIn");
//        log.add("phone number: ", phoneNumber);
//        log.add("cardId: ", cardId);
//
//        final String bankId = VMFeeType.getBankId(feeType);
//        final Common.ServiceReq serviceReq = new Common.ServiceReq();
//        serviceReq.Command      = Common.ServiceReq.COMMAND.GET_FEE;
//        serviceReq.bankId       = bankId;
//        serviceReq.channel = 0;
//        serviceReq.inoutCity = 0;
//        serviceReq.tranType = 0;
//
//        log.add("bankId: ", bankId);
//
//        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> message) {
//                FeeDb.Obj obj = null;
//                if (message.body() != null) {
//                    obj = new FeeDb.Obj(message.body());
//                }
//                int static_fee;
//                double dynamic_fee;
//
//                if (obj == null) {
//                    static_fee = 3200;
//                    dynamic_fee = 2.4;
//                } else {
//                    static_fee = obj.STATIC_FEE;
//                    dynamic_fee = obj.DYNAMIC_FEE;
//                }
//
//                log.add("static_fee: ", static_fee);
//                log.add("dynamic_fee: ", dynamic_fee);
//                log.add("amount: ", tranAmount);
//
//                long fee = VMFeeType.calculateFeeMethod(static_fee, dynamic_fee, tranAmount);
//                log.add("fee", fee);
//                String ip = data.ip;
//                //BEGIN 0000000005
//                if(data.ip.length() > 1){ip = data.ip.replace("\\", "").replace("/","");}
//                //END 0000000005
//                final VisaRequest visaRequest = VisaRequestFactory.getCashinRequest(phoneNumber
//                        , System.currentTimeMillis()
//                        , cardId
//                        , cardLbl
//                        , tranAmount
//                        , fee
//                        ,emailDesc,cardType, serviceFee, descriptionService, ip);
//                //keyvaluepair.start
//                String type = feeType == 0 ? "cashin" : "billpay";
//                visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
//                visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
//                visaRequest.addExtraValue("issms", "no"); // khong can nhan sms
//                visaRequest.addExtraValue("type", type); // khong can nhan sms
//                visaRequest.addExtraValue("tranType",String.valueOf(tranType));
//                visaRequest.addExtraValue("serviceId", serviceId);
//                log.add("request content", visaRequest);
//                //billpay  cashin
//                //keyvaluepair.end
//
//                vertx.eventBus().sendWithTimeout(busAddress, visaRequest.getJsonObject(), 45000,
//                        new AsyncResultHandler<Message<JsonObject>>() {
//                            @Override
//                            public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
//                                VisaResponse visaResponse;
//                                if (messageAsyncResult.succeeded()) {
//                                    JsonObject jsonObject = messageAsyncResult.result().body();
//                                    visaResponse = new VisaResponse(jsonObject);
//                                    log.add("desc: ", "success");
//                                } else if (messageAsyncResult.failed()) {
//                                    visaResponse = null;
//                                    log.add("desc", "timeout");
//                                } else {
//                                    visaResponse = null;
//                                    log.add("desc", "failed");
//                                }
//                                //thanh cong
//                                callback.handle(visaResponse);
//                            }
//                        }
//                );
//            }
//        });
//    }

    public static void doVisaMasterCashIn(final Vertx vertx,
                                          final String busAddress,
                                          final SockData data,
                                          final String phoneNumber,
                                          final String cardId,
                                          final String cardLbl,
                                          final String cardType,
                                          final long tranAmount,
                                          final int feeType,
                                          final long serviceFee,
                                          final String channel,
                                          final String emailDesc,
                                          final Common.BuildLog log,
                                          final String descriptionService,
                                          final int tranType,
                                          final String serviceId,
                                          final Handler<VisaResponse> callback) {

        log.add("func", "doVisaMasterCashIn");
        log.add("phone number: ", phoneNumber);
        log.add("cardId: ", cardId);

        final String bankId = VMFeeType.getBankId(feeType);
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command      = Common.ServiceReq.COMMAND.GET_FEE;
        serviceReq.bankId       = bankId;
        serviceReq.channel = 0;
        serviceReq.inoutCity = 0;
        serviceReq.tranType = 0;

        log.add("bankId: ", bankId);

        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                FeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new FeeDb.Obj(message.body());
                }
                int static_fee;
                double dynamic_fee;

                if (obj == null) {
                    static_fee = 3200;
                    dynamic_fee = 2.4;
                } else {
                    static_fee = obj.STATIC_FEE;
                    dynamic_fee = obj.DYNAMIC_FEE;
                }

                log.add("static_fee: ", static_fee);
                log.add("dynamic_fee: ", dynamic_fee);
                log.add("amount: ", tranAmount);

                long fee = VMFeeType.calculateFeeMethod(static_fee, dynamic_fee, tranAmount);
                log.add("fee", fee);
                String ip = data.ip;
                //BEGIN 0000000005
                if(data.ip.length() > 1){ip = data.ip.replace("\\", "").replace("/","");}
                //END 0000000005
                final VisaRequest visaRequest = VisaRequestFactory.getCashinRequest(phoneNumber
                        , System.currentTimeMillis()
                        , cardId
                        , cardLbl
                        , tranAmount
                        , fee
                        ,emailDesc,cardType, serviceFee, descriptionService, ip);
                //keyvaluepair.start
                String type = feeType == 0 ? "cashin" : "billpay";
                visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
                visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
                visaRequest.addExtraValue("issms", "no"); // khong can nhan sms
                visaRequest.addExtraValue("type", type); // khong can nhan sms
                visaRequest.addExtraValue("tranType",String.valueOf(tranType));
                visaRequest.addExtraValue("serviceId", serviceId);
                log.add("request content", visaRequest);
                //billpay  cashin
                //keyvaluepair.end

                ConnectorCommon.requestVisaToConnectorViaHTTP(vertx, phoneNumber, visaRequest.getJsonObject(), "visa", log, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject joResponse) {
                                VisaResponse visaResponse;
                                int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                                String des = joResponse.getString(StringConstUtil.DESCRIPTION, "");
                                log.add("desc err doVisaMasterCashIn Visa", err);
                                log.add("desc visa", des );
                                if(err != 0)
                                {
                                    visaResponse = null;
                                    log.add("desc", "timeout");
                                }
                                else {
                                    visaResponse = new VisaResponse(joResponse.getObject(BankHelperVerticle.DATA, new JsonObject()));
                                }
                                //thanh cong
                                callback.handle(visaResponse);
                            }
                        }
                );
            }
        });
    }

//    public static void createToken(Vertx vertx
//            , String busAddress
//            , SockData data
//            , JsonObject jsonCardInfo
//            , String phoneNumber
//            , final Common.BuildLog log
//            , final String channel
//            , final Handler<VisaResponse> callback) {
//
//        String cardType = jsonCardInfo.getString(VMConst.cardType, "001");
//        String cardNumber = jsonCardInfo.getString(VMConst.cardNumber, "");
//        String cardHolderName = jsonCardInfo.getString(VMConst.cardHolerName, "");
//        String email = jsonCardInfo.getString(VMConst.email,"");
//        String appId = jsonCardInfo.getString(VMConst.appId, "");
//        log.add("func: ", "createToken");
//        log.add("Phone number: ", phoneNumber);
//        log.add("busAddress: ", busAddress);
//        log.writeLog();
//        //Set TransID again after get full code.
//        String ip = data.ip;
//        if(data.ip.length() > 1){ip = data.ip.replace("\\", "").replace("/","");}
//        VisaRequest visaRequest = VisaRequestFactory.getCreateTokenRequest(phoneNumber,
//                System.currentTimeMillis(), cardHolderName, VMCardType.getCardType(cardType), cardNumber, email, ip);
//        // Assign trans ID
//        //keyvaluepair.start
//        visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
//        visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
//        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms
//        visaRequest.addExtraValue(VMConst.partnerCode,jsonCardInfo.getString(VMConst.partnerCode,""));
//        visaRequest.addExtraValue(StringConstUtil.DEVICE_IMEI, data.imei);
//        //billpay  cashin
//        vertx.eventBus().sendWithTimeout(busAddress
//                , visaRequest.getJsonObject()
//                , 120000, new Handler<AsyncResult<Message<JsonObject>>>() {
//            @Override
//            public void handle(AsyncResult<Message<JsonObject>> asyncResult) {
//
//                VisaResponse visaResponse;
//                if (asyncResult.succeeded()) {
//                    JsonObject json = asyncResult.result().body();
//                    visaResponse = new VisaResponse(json);
//                    log.add("desc", "success");
//                } else if (asyncResult.failed()) {
//                    visaResponse = null;
//                    log.add("desc", "timeout");
//                } else {
//                    visaResponse = null;
//                    log.add("desc", "failed");
//                }
//                //thanh cong
//                callback.handle(visaResponse);
//
//            }
//        });
//    }

    public static void createToken(Vertx vertx
            , String busAddress
            , SockData data
            , JsonObject jsonCardInfo
            , String phoneNumber
            , final Common.BuildLog log
            , final String channel
            , final Handler<VisaResponse> callback) {

        String cardType = jsonCardInfo.getString(VMConst.cardType, "001");
        String cardNumber = jsonCardInfo.getString(VMConst.cardNumber, "");
        String cardHolderName = jsonCardInfo.getString(VMConst.cardHolerName, "");
        String email = jsonCardInfo.getString(VMConst.email,"");
        String appId = jsonCardInfo.getString(VMConst.appId, "");
        log.add("func: ", "createToken");
        log.add("Phone number: ", phoneNumber);
        log.add("busAddress: ", busAddress);
        log.writeLog();
        //Set TransID again after get full code.
        String ip = data.ip;
        if(data.ip.length() > 1){ip = data.ip.replace("\\", "").replace("/","");}
        VisaRequest visaRequest = VisaRequestFactory.getCreateTokenRequest(phoneNumber,
                System.currentTimeMillis(), cardHolderName, VMCardType.getCardType(cardType), cardNumber, email, ip);
        // Assign trans ID
        //keyvaluepair.start
        visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
        visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms
        visaRequest.addExtraValue(VMConst.partnerCode,jsonCardInfo.getString(VMConst.partnerCode,""));
        visaRequest.addExtraValue(StringConstUtil.DEVICE_IMEI, data.imei);
        //billpay  cashin
        ConnectorCommon.requestVisaToConnectorViaHTTP(vertx, phoneNumber, visaRequest.getJsonObject(), "visa", log, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joResponse) {

                VisaResponse visaResponse;
                int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                String des = joResponse.getString(StringConstUtil.DESCRIPTION, "");
                log.add("desc err createToken Visa", err);
                log.add("desc visa", des );
                if(err != 0)
                {
                    visaResponse = null;
                    log.add("desc", "timeout");
                }
                else {
                    visaResponse = new VisaResponse(joResponse.getObject(BankHelperVerticle.DATA, new JsonObject()));
                }
                //thanh cong
                callback.handle(visaResponse);

            }
        });
    }

//    public static void sentResultConnectorSourceVisa(final Vertx vertx,
//                                                     final String busAddress,
//                                                     final long transId,
//                                                     final String phoneNumber,
//                                                     final long cashinTransId,
//                                                     final String serviceName,
//                                                     final boolean isSuccess,
//                                                     final String channel,
//                                                     final Common.BuildLog log,
//                                                     final int tranType,
//                                                     final String serviceId){
//
//        final VisaRequest visaRequest = VisaRequestFactory.createSendEmailRequest(
//                phoneNumber
//                , transId
//                , cashinTransId
//                , serviceName
//                , isSuccess);
//        //keyvaluepair.start
//        visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
//        visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
//        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms
//        visaRequest.addExtraValue("tranType",String.valueOf(tranType));
//        visaRequest.addExtraValue("serviceId", serviceId);
//        log.add("request sentResultConnectorSourceVisa content", visaRequest);
//        //billpay  cashin
//        //keyvaluepair.end
//
//        vertx.eventBus().sendWithTimeout(busAddress, visaRequest.getJsonObject(), 45000,
//                new AsyncResultHandler<Message<JsonObject>>() {
//                    @Override
//                    public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
//                        if (messageAsyncResult.succeeded()) {
//                            log.add("desc: ", "success");
//                        } else if (messageAsyncResult.failed()) {
//                            log.add("desc", "timeout");
//                        } else {
//                            log.add("desc", "failed");
//                        }
//                        log.writeLog();
//                    }
//                }
//        );
//    }
        public static void sentResultConnectorSourceVisa(final Vertx vertx,
                                                     final String busAddress,
                                                     final long transId,
                                                     final String phoneNumber,
                                                     final long cashinTransId,
                                                     final String serviceName,
                                                     final boolean isSuccess,
                                                     final String channel,
                                                     final Common.BuildLog log,
                                                     final int tranType,
                                                     final String serviceId){

        final VisaRequest visaRequest = VisaRequestFactory.createSendEmailRequest(
                phoneNumber
                , transId
                , cashinTransId
                , serviceName
                , isSuccess);
        //keyvaluepair.start
        visaRequest.addExtraValue("client", "backend"); // bankend gui yeu cau
        visaRequest.addExtraValue("chanel", channel); // phan biet kenh web/app
        visaRequest.addExtraValue("issms", "no"); // khong can nhan sms
        visaRequest.addExtraValue("tranType",String.valueOf(tranType));
        visaRequest.addExtraValue("serviceId", serviceId);
        log.add("request sentResultConnectorSourceVisa content", visaRequest);
        //billpay  cashin
        //keyvaluepair.end

            ConnectorCommon.requestVisaToConnectorViaHTTP(vertx, phoneNumber, visaRequest.getJsonObject(), "visa", log, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joResponse) {
                            VisaResponse visaResponse;
                            int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                            String des = joResponse.getString(StringConstUtil.DESCRIPTION, "");
                            log.add("desc err sentResultConnectorSourceVisa Visa", err);
                            log.add("desc visa", des );
                            if(err != 0)
                            {
                                visaResponse = null;
                                log.add("desc", "timeout");
                            }
                            else {
                                visaResponse = new VisaResponse(joResponse.getObject(BankHelperVerticle.DATA, new JsonObject()));
                            }
                            //thanh cong
                            log.writeLog();
                    }
                }
        );
    }
}

    //END
