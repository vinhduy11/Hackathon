package com.mservice.momo.gateway.internal.connectorproxy;

import com.mservice.momo.data.BillInfoService;
import com.mservice.momo.data.CacheBillPayDb;
import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.proxy.entity.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectorCommon {

    private static final String TYPE_BILLPAY = "billpay";
    private static final String TYPE_TOPUP = "topup";

    public static int AccountNotFound = 48;
    public static int Payed = 9002;
    public static int Success = 0;
    public static int Expired = 9004;

    public static String jsonkey = "connector_proxy_busname";
    //    public static void insertInfoCacheBill(Vertx vertx, final BillInfoService billInfoService, final ProxyResponse response, final CacheBillPayDb cacheBillPayDb, final Common.BuildLog log, final int duration) {
//        Common.ServiceReq serviceReq = new Common.ServiceReq();
//        serviceReq.ServiceId = response.getRequest().getServiceCode();
//        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
//
//        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
//            @Override
//            public void handle(final JsonArray objects) {
//                log.add("size", objects.size());
//                if (objects != null && objects.size() > 0) {
//                    boolean isCachedBill = ((JsonObject) objects.get(0)).getBoolean(colName.ServiceCols.HAS_CACHED_BILL, false);
//                    if (isCachedBill) {
//                        JsonObject jsonBill = billInfoService.toJsonObject();
//                        CacheBillPayDb.Obj cacheObj = new CacheBillPayDb.Obj();
//                        log.add("func", "insertInfoCacheBill");
//                        cacheObj.billId = response.getRequest().getBillId();
//                        cacheObj.total_amount = jsonBill.getLong(colName.CacheBillPay.TOTAL_AMOUNT, 0);
//                        cacheObj.extra_info = jsonBill.getArray(colName.CacheBillPay.EXTRA_INFO, new JsonArray());
//                        cacheObj.customer_info = jsonBill.getArray(colName.CacheBillPay.CUSTOMER_INFO, new JsonArray());
//                        cacheObj.array_price = jsonBill.getArray(colName.CacheBillPay.ARRAY_PRICE, new JsonArray());
//                        cacheObj.count = 0;
//                        cacheObj.checkedTime = System.currentTimeMillis();
//                        cacheObj.againCheckTime = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * duration;
//                        cacheObj.service_id = response.getRequest().getServiceCode();
//
//                        log.add("cacheObj", cacheObj);
//                        log.writeLog();
//                        cacheBillPayDb.updatePartial(cacheObj.billId, cacheObj.toJson(), new Handler<Boolean>() {
//                            @Override
//                            public void handle(Boolean aBoolean) {
//                                log.add("cacheBillPayDb.updatePartial", aBoolean);
//                                log.writeLog();
//                            }
//                        });
//
//                    }
//                }
//            }
//        });
//    }
    static ConnectorHTTPPostPathDb connectorHTTPPostPathDb;

    public static void RequestCheckBill(final Vertx _vertx, final Logger logger, final int phoneNumber, final JsonObject glbConfig, final ProxyRequest proxyRequest, final String busName, final com.mservice.momo.vertx.processor.Common.BuildLog log, final Handler<BillInfoService> callback) {

        // Lay thong tin cached bill.
        JsonObject cacheBillInfo = glbConfig != null ? glbConfig.getObject(StringConstUtil.CACHE_BILL_INFO, new JsonObject()) : new JsonObject();
        if(connectorHTTPPostPathDb == null)
        {
            connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(_vertx);
        }
        final int count = cacheBillInfo.getInteger(StringConstUtil.COUNT, 3);
        final int duration = cacheBillInfo.getInteger(StringConstUtil.DURATION, 1);

        log.add("function", "Request");
        log.add("json request to connector", proxyRequest.toString());
//        final CacheBillPayDb cacheBillPayDb = new CacheBillPayDb(_vertx, logger);
//        cacheBillPayDb.findOne("0" + phoneNumber, new Handler<CacheBillPayDb.Obj>() {
//            @Override
//            public void handle(CacheBillPayDb.Obj obj) {
//                log.add("cacheBillPay.findOne", obj);
//                if (obj != null && System.currentTimeMillis() < obj.againCheckTime) {
////                    log.add("count", obj.count);
////                    obj.count = obj.count + 1;
////                    obj.checkedTime = System.currentTimeMillis();
////                    log.add("count again", obj.count);
////                    cacheBillPayDb.updatePartial(proxyRequest.getBillId(), obj.toJson(), new Handler<Boolean>() {
////                        @Override
////                        public void handle(Boolean aBoolean) {
////                            String result = aBoolean ? "Update" : "Insert";
////                            log.add("update result", result);
////                            log.writeLog();
////                        }
////                    });
////                    BillInfoService billInfoService = new BillInfoService(obj.toJson());
//                    log.add("cacheBillPay -> BillInfoService(obj.toJson())", "Check lien tuc nen khong cho check");
//                    log.writeLog();
////                    callback.handle(billInfoService);
//                    //Neu co thong tin bill, thi tra ra luon
//                    return;
//                } COMMENT CACHE BILL, DONT USE
        connectorHTTPPostPathDb.findOne(proxyRequest.getServiceCode().toLowerCase(), new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj connectorPostObj) {
                if(connectorPostObj == null)
                {
                    log.add("desc", "Check bill via bus");
                    requestCheckBillFromConnectorViaBus(_vertx, proxyRequest, busName, log, callback);
                    return;
                }
                log.add("desc", "check bill via http post");
                requestCheckBillFromConnectorViaHttpPost(connectorPostObj, glbConfig, _vertx, log, proxyRequest, busName, callback);
                return;
            }
        });

//            }
//        }); //END CHECK CACHED BILL SERVICE

    }

    private static void requestCheckBillFromConnectorViaHttpPost(ConnectorHTTPPostPathDb.Obj connectorPostObj, JsonObject glbConfig, Vertx _vertx, final Common.BuildLog log, ProxyRequest proxyRequest, final String busName, final Handler<BillInfoService> callback) {
        Misc.getDataFromConnector(proxyRequest.getInitiator(), connectorPostObj.host, connectorPostObj.port, connectorPostObj.path, _vertx, log, proxyRequest.getServiceCode(), proxyRequest.getJsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joReplyFromConnector) {
                BillInfoService billInfoService = new BillInfoService();
                //thanh cong
                int error = joReplyFromConnector.getInteger(StringConstUtil.ERROR, 1000);
                if (error != 0) {
                    //xem nhu timeout
                    billInfoService.total_amount = -100;
                    log.add("error", -100);
                    log.add("desc", "Request timeout " + busName.toString());
                    callback.handle(billInfoService);
                    return;
                }

                JsonObject joData = joReplyFromConnector.getObject(BankHelperVerticle.DATA, null);
                if (joData == null) {
                    //xem nhu timeout
                    billInfoService.total_amount = -100;
                    log.add("error", -100);
                    log.add("desc", "Request timeout " + busName.toString());
                    callback.handle(billInfoService);
                    return;
                }
                ProxyResponse response = new ProxyResponse(joData);
                //build info service
                billInfoService = buildBillInfoService(response, log);
                callback.handle(billInfoService);
                return;
            }
        });
    }

    public static void requestVisaToConnectorViaHTTP(final Vertx vertx, final String phoneNumber, final JsonObject joData, final String serviceId, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        if(connectorHTTPPostPathDb == null)
        {
            connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        }
        connectorHTTPPostPathDb.findOne(serviceId, new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj visaObj) {
                if(visaObj == null)
                {
                    JsonObject joReply = new JsonObject();
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Khong co thong tin http visa");
                    callback.handle(joReply);
                    return;
                }
                Misc.getDataFromConnector(phoneNumber, visaObj.host, visaObj.port, visaObj.path, vertx, log, serviceId, joData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject joResponse) {
                        callback.handle(joResponse);
                    }
                });
                return;
            }
        });
    }

    private static void requestCheckBillFromConnectorViaBus(Vertx _vertx, ProxyRequest proxyRequest, final String busName, final Common.BuildLog log, final Handler<BillInfoService> callback) {
        _vertx.eventBus().sendWithTimeout(busName, proxyRequest.getJsonObject(), 90 * 1000, new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {

                BillInfoService billInfoService = new BillInfoService();
                //thanh cong
                if (messageAsyncResult.succeeded()) {
                    JsonObject jo = messageAsyncResult.result().body();
                    ProxyResponse response = new ProxyResponse(jo);

                    //build info service
                    billInfoService = buildBillInfoService(response, log);

                    //BEGIN 0000000030 Cache Bill
//                            insertInfoCacheBill(_vertx, billInfoService, response, cacheBillPayDb, log, duration);
                    //END 0000000030 Cache Bill.

                    //BEGIN 0000000051 Luu thoi gian checkbill cua so dien thoai XXX.
                    //controlTimeToCheckBill(_vertx, billInfoService, phoneNumber, response, cacheBillPayDb, log, duration);
                    //END 0000000051

                    callback.handle(billInfoService);
                } else {
                    //xem nhu timeout
                    billInfoService.total_amount = -100;
                    log.add("error", -100);
                    log.add("desc", "Request timeout " + busName.toString());
                    callback.handle(billInfoService);
                }
            }
        });
    }

    public static void controlTimeToCheckBill(Vertx vertx, final BillInfoService billInfoService, final int phoneNumber, final ProxyResponse response, final CacheBillPayDb cacheBillPayDb, final Common.BuildLog log, final int duration) {
        JsonObject jsonBill = billInfoService.toJsonObject();
        CacheBillPayDb.Obj cacheObj = new CacheBillPayDb.Obj();
        log.add("func", "controlTimeToCheckBill");
        cacheObj.billId = "0" + phoneNumber;
//        cacheObj.total_amount = jsonBill.getLong(colName.CacheBillPay.TOTAL_AMOUNT, 0);
//        cacheObj.extra_info = jsonBill.getArray(colName.CacheBillPay.EXTRA_INFO, new JsonArray());
//        cacheObj.customer_info = jsonBill.getArray(colName.CacheBillPay.CUSTOMER_INFO, new JsonArray());
//        cacheObj.array_price = jsonBill.getArray(colName.CacheBillPay.ARRAY_PRICE, new JsonArray());
        cacheObj.count = 0;
        cacheObj.checkedTime = System.currentTimeMillis();
        cacheObj.againCheckTime = System.currentTimeMillis() + 1000L * duration;
        cacheObj.service_id = response.getRequest().getServiceCode();

        log.add("cacheObj", cacheObj);
        cacheBillPayDb.updatePartial(cacheObj.billId, cacheObj.toJson(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                log.add("cacheBillPayDb.updatePartial", aBoolean);
                log.writeLog();
            }
        });
    }

    public static void requestPayment(final Vertx _vertx,
                                      final ProxyRequest proxyRequest,
                                      final String busName,
                                      final com.mservice.momo.vertx.processor.Common.BuildLog log,
                                      final JsonObject globalConfig,
                                      final Handler<ProxyResponse> callback) {

        log.add("function", "Request");
        log.add("json request to connector", proxyRequest.toString());
        final AtomicInteger atomicCount = new AtomicInteger();
        if(connectorHTTPPostPathDb == null)
        {
            connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(_vertx);
        }
        connectorHTTPPostPathDb.findOne(proxyRequest.getServiceCode().toLowerCase(), new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(final ConnectorHTTPPostPathDb.Obj connectorPathObj) {
                if(connectorPathObj == null)
                {
                    //Di theo luong thanh toan Verticle
                    log.add("desc", "Di theo luong Verticle");
                    sendRequestToConnector(_vertx, proxyRequest, busName, log, atomicCount, callback);
                    return;
                }
                else {
                    //Di theo luong HTTP POST
                    log.add("desc", "HTTP POST");
                    sendHttpPostPaymentRequestToConnector(connectorPathObj, globalConfig, _vertx, log, proxyRequest, callback);
                    return;
                }
            }
        });

    }

    private static void sendHttpPostPaymentRequestToConnector(final ConnectorHTTPPostPathDb.Obj connectorPathObj, JsonObject globalConfig, Vertx _vertx, final Common.BuildLog log, final ProxyRequest proxyRequest, final Handler<ProxyResponse> callback) {
        Misc.getDataForPaymentFromConnector(proxyRequest.getInitiator(), connectorPathObj.host, connectorPathObj.port, globalConfig, connectorPathObj.path, _vertx, log, connectorPathObj.serviceId, proxyRequest.getJsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joReply) {
                int error = joReply.getInteger(StringConstUtil.ERROR, 1000);
                ProxyResponse response;
                if (error != 0) {
                    response = new ProxyResponse(proxyRequest);
                    response.setProxyResponseCode(-100);
                    response.setPartnerResponseMessage("Time out");
                    callback.handle(response);
                    log.add("desc", "Thanh toan dich vu " + connectorPathObj.serviceId + " that bai tu connector");
                    log.writeLog();
                    return;
                }
                JsonObject jsonData = joReply.getObject(BankHelperVerticle.DATA, null);
                if (jsonData == null) {
                    response = new ProxyResponse(proxyRequest);
                    response.setProxyResponseCode(-100);
                    response.setPartnerResponseMessage("Time out");
                    callback.handle(response);
                    log.add("desc", "Thanh toan dich vu " + connectorPathObj.serviceId + " that bai tu do data null");
                    log.writeLog();
                    return;
                }
                log.add("desc", "Thanh toan dich vu " + connectorPathObj.serviceId + " thanh cong tu connector");
                response = new ProxyResponse(jsonData);
                callback.handle(response);
            }
        });
    }

    private static void sendRequestToConnector(final Vertx _vertx, final ProxyRequest proxyRequest, final String busName, final Common.BuildLog log, final AtomicInteger atomicCount, final Handler<ProxyResponse> callback) {
        final long time_request = System.currentTimeMillis();
        try
        {
            _vertx.eventBus().sendWithTimeout(busName,
                    proxyRequest.getJsonObject(),
                    900 * 1000,
                    new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                            ProxyResponse response;
                            //thanh cong
                            if (messageAsyncResult.succeeded()) {
                                JsonObject jo = messageAsyncResult.result().body();
                                response = new ProxyResponse(jo);
                                callback.handle(response);
                            } else {
                                atomicCount.incrementAndGet();
                                long time_response = System.currentTimeMillis();
                                long distance = time_response - time_request;
                                log.add("error", -100);
                                log.add("desc", "Request timeout " + busName);
                                log.add("desc", "time " + distance);
                                log.add("desc", "count " + atomicCount.intValue());
//                                log.writeLog();
                                if(distance < 5000L && atomicCount.intValue() < 4)
                                {
                                    sendRequestToConnector(_vertx, proxyRequest, busName, log, atomicCount, callback);
                                }
                                else {
                                    response = new ProxyResponse(proxyRequest);
                                    response.setProxyResponseCode(-100);
                                    response.setPartnerResponseMessage("Time out");
                                    callback.handle(response);
                                }
                            }

                        }
                    }
            );
        }
        catch(Exception ex){
            atomicCount.incrementAndGet();
            long time_response = System.currentTimeMillis();
            long distance = time_response - time_request;
            log.add("error", -100);
            log.add("desc", "Request timeout " + busName);
            log.add("desc", "time " + distance);
            log.add("desc", "count " + atomicCount.intValue());
//            log.writeLog();
            if(distance < 5000L && atomicCount.intValue() < 4)
            {
                sendRequestToConnector(_vertx, proxyRequest, busName, log, atomicCount, callback);
            }
            else {
                ProxyResponse response = new ProxyResponse(proxyRequest);
                response.setProxyResponseCode(-100);
                response.setPartnerResponseMessage("Time out");
                callback.handle(response);
            }

        }

    }

    private static BillInfoService buildBillInfoService(ProxyResponse response, com.mservice.momo.vertx.processor.Common.BuildLog log) {
        BillInfoService billInfoService = new BillInfoService();
        //todo build cai cai chuan cua response

		/*ProxyResponse
         {
		 “billList”: 		[{
		 "billId": "12345",
		 “userId": "12432",
		 "amount": "20000",
		 "cycle": "8/2014",
		 "fromDate":"",
		 “toDate":""
		 },
		 {
		 "billId": "12345",
		 “userId": "12432",
		 "amount": "20000",
		 "cycle": "8/2014",
		 "fromDate":"",
		 “toDate":""


		 }],
		 "proxyRequest": {
		 “type”: “billpay”,
		 “requestSource”: 1,
		 “requestType”: 1,
		 “amount”: 348000,
		 “transId”: 3456789,
		 “debitor”: “0917823475”,
		 “debitorPin”: 123456,
		 “serviceCode”: “EVN”,
		 “billerId”: “PE00004324554”,
		 “creditor”: “billpaydienhcm”
		 }
		 "proxyResponseCode": 100,
		 "proxyResponseMessage": “System error”,
		 "totalAmount": 80000,
		 "partnerTransId": “43254656”
		 "customerId": “223344”
		 "customerName": “Nguyen Van A”
		 "customerInfo": “”
		 "customerPhone": “0913882173”,
		 "customerAddress": “12 Ngo tat tô”,
		 "serialNumber": “123i97-4556-5654”, // ban key
		 "secretNumber": “5435vgfdg-5435”, // ban key
		 }*/
        int proxyResponseCode = response.getProxyResponseCode();
        String proxyResponseMessage = (response.getProxyResponseMessage() == null ? "" : response.getProxyResponseMessage());
        long totalAmount = response.getTotalAmount();
        String partnerTransId = (response.getPartnerTransId() == null ? "" : response.getPartnerTransId());
        String customerId = (response.getCustomerId() == null ? "" : response.getCustomerId());
        String customerName = (response.getCustomerName() == null ? "" : response.getCustomerName());
        String customerInfo = (response.getCustomerInfo() == null ? "" : response.getCustomerInfo());
        String customerPhone = (response.getCustomerPhone() == null ? "" : response.getCustomerPhone());
        String customerAddress = (response.getCustomerAddress() == null ? "" : response.getCustomerAddress());
        String codeItem = (response.getRequest().getBillId() == null ? "" : response.getRequest().getBillId());
        String htmlContent = response.getHtmlContent();

        log.add("proxyResponseCode", proxyResponseCode);
        log.add("proxyResponseMessage", proxyResponseMessage);
        log.add("totalAmount", totalAmount);
        log.add("partnerTransId", partnerTransId);
        log.add("customerId", customerId);
        log.add("customerName", customerName);

        log.add("customerInfo", customerInfo);
        log.add("customerPhone", customerPhone);
        log.add("customerAddress", customerAddress);
        log.add("codeItem", codeItem);

        //BEGIN 0000000030 CheckBill


        //END 0000000030 CheckBill
        if ("prudential".equalsIgnoreCase(response.getRequest().getServiceCode())
                || "railway".equalsIgnoreCase(response.getRequest().getServiceCode())
                || "thienhoa".equalsIgnoreCase(response.getRequest().getServiceCode())) {
            //todo xu ly rieng cho prudential, railway

            if (proxyResponseCode == Success) {
                billInfoService.total_amount = 0;
            } else if (proxyResponseCode == Payed) {
                billInfoService.total_amount = -1;
            } else if (proxyResponseCode == Expired) {
                billInfoService.total_amount = -2;
            } else {
                billInfoService.total_amount = -100;
            }
        } else {

            if (proxyResponseCode == Success) {
                //lay thong tin khach hang thanh cong
                billInfoService.total_amount = totalAmount;
            } else if (proxyResponseCode == Payed) {
                //da thanh toan
                billInfoService.total_amount = 0;
            } else if (proxyResponseCode == AccountNotFound) {
                //khong tim thay thong tin khach hang
                billInfoService.total_amount = -100;
            } else {
                //cac loi con lai --> xem nhu khong lay duoc thong tin hoa don
                billInfoService.total_amount = -100;
            }

        }

        if (customerName != null && !"".equalsIgnoreCase(customerName)) {
            BillInfoService.TextValue cusName = new BillInfoService.TextValue();
            cusName.text = Const.AppClient.FullName;
            cusName.value = customerName;
            billInfoService.addCustomInfo(cusName);
        }

        if (customerId != null && !"".equalsIgnoreCase(customerId)) {
            BillInfoService.TextValue cusId = new BillInfoService.TextValue();
            cusId.text = Const.AppClient.Account;
            cusId.value = customerId;
            billInfoService.addCustomInfo(cusId);
        }

        if (customerPhone != null && !"".equalsIgnoreCase(customerPhone)) {
            BillInfoService.TextValue cusInfo = new BillInfoService.TextValue();
            cusInfo.text = Const.AppClient.Phone;
            cusInfo.value = customerPhone;
            billInfoService.addCustomInfo(cusInfo);
        }

        if (customerAddress != null && !"".equalsIgnoreCase(customerAddress)) {
            BillInfoService.TextValue cusAddress = new BillInfoService.TextValue();
            cusAddress.text = Const.AppClient.Address;
            cusAddress.value = customerAddress;
            billInfoService.addCustomInfo(cusAddress);
        }

        if (customerInfo != null && !"".equalsIgnoreCase(customerInfo)) {
            BillInfoService.TextValue cusInfo = new BillInfoService.TextValue();
            cusInfo.text = "inf";
            cusInfo.value = customerInfo;
            billInfoService.addCustomInfo(cusInfo);
        }
        //get total money from reponse
        if (totalAmount != 0) {
            BillInfoService.TextValue tAmount = new BillInfoService.TextValue();
            tAmount.text = "money";
            tAmount.value = String.valueOf(totalAmount);
            billInfoService.addCustomInfo(tAmount);
        }
        //Get bill Id from request
        if (codeItem != null && !"".equalsIgnoreCase(codeItem)) {
            BillInfoService.TextValue codeI = new BillInfoService.TextValue();
            codeI.text = "item";
            codeI.value = codeItem;
            billInfoService.addCustomInfo(codeI);
        }
        if (htmlContent != null && !"".equalsIgnoreCase(htmlContent)) {
            BillInfoService.TextValue html = new BillInfoService.TextValue();
            html.text = Const.AppClient.Html;
            html.value = htmlContent;
            billInfoService.addCustomInfo(html);
        }
        if (proxyResponseMessage != null && !"".equalsIgnoreCase(proxyResponseMessage)) {
            BillInfoService.TextValue responseMessage = new BillInfoService.TextValue();
            responseMessage.text = "responseMessage";
            responseMessage.value = proxyResponseMessage;
            billInfoService.addCustomInfo(responseMessage);
        }


        BillInfoService.TextValue responseCode = new BillInfoService.TextValue();
        responseCode.text = "responseCode";
        responseCode.value = String.valueOf(proxyResponseCode);
        billInfoService.addCustomInfo(responseCode);


		/* cusInfo = new BillInfoService.TextValue();
         cusInfo.text = "dc";
		 cusInfo.value = listTempCusInfo[2];
		 billInfoService.addCustomInfo(cusInfo);*/

		/*if (mapRes.containsKey("cusInfo")) {
         String tempCusInfo = mapRes.get("cusInfo");
		 String[] listTempCusInfo = tempCusInfo.split("#");

		 //lay so dien thoai
		 if(listTempCusInfo != null
		 && (listTempCusInfo.length >=1)
		 && (!listTempCusInfo[0].equalsIgnoreCase(""))){

		 //neu co so dien thoai
		 if(DataUtil.strToInt(listTempCusInfo[0]) > 0){
		 cusInfo = new BillInfoService.TextValue();
		 cusInfo.text = "sdt";
		 cusInfo.value = listTempCusInfo[0];
		 billInfoService.addCustomInfo(cusInfo);
		 }

		 }

		 //lay goi dia chi
		 if((listTempCusInfo != null)
		 && (listTempCusInfo.length>=3)
		 && (!listTempCusInfo[2].equalsIgnoreCase(""))){
		 cusInfo = new BillInfoService.TextValue();
		 cusInfo.text = "dc";
		 cusInfo.value = listTempCusInfo[2];
		 billInfoService.addCustomInfo(cusInfo);
		 }

		 //lay goi dich vu
		 if((listTempCusInfo != null)
		 && (listTempCusInfo.length>=2)
		 && (!listTempCusInfo[1].equalsIgnoreCase(""))){
		 cusInfo = new BillInfoService.TextValue();
		 cusInfo.text = "sn";
		 cusInfo.value = listTempCusInfo[1];
		 billInfoService.addCustomInfo(cusInfo);
		 }
		 }*/

		/*{
         "billId": "12345", // ma hoa don con
		 “userId": "12432", //
		 "amount": "20000", // so tien con
		 "cycle": "8/2014", // ky
		 "fromDate":"",
		 “toDate":""
		 }*/
        //chi tiet cac hoa don con
        List<BillerInfo> billerInfos = (response.getBillList() == null ? new ArrayList<BillerInfo>() : response.getBillList());

        for (int i = 0; i < billerInfos.size(); i++) {
            BillInfoService.ExtraInfo extraInfo = new BillInfoService.ExtraInfo();

            extraInfo.bill_detail_id = (billerInfos.get(i).getBillID() == null ? "" : billerInfos.get(i).getBillID());

            extraInfo.amount = (billerInfos.get(i).getAmount() == 0 ? "0" : billerInfos.get(i).getAmount() + "");

            //uu tien lay theo ky thanh toan truoc
            if (billerInfos.get(i).getCycle() != null && !"".equalsIgnoreCase(billerInfos.get(i).getCycle())) {

                extraInfo.from_date = "Kỳ " + billerInfos.get(i).getCycle();

                extraInfo.to_date = "Kỳ " + billerInfos.get(i).getCycle();
            } else {
                //lay theo ngay thanh toan
                extraInfo.from_date = billerInfos.get(i).getFromDate() == null ? "" : billerInfos.get(i).getFromDate();
                extraInfo.to_date = billerInfos.get(i).getToDate() == null ? "" : billerInfos.get(i).getToDate();

            }

			/*

			 }else if(billerInfos.get(i).getFromDate()!= null &&  !"".equalsIgnoreCase(billerInfos.get(i).getFromDate())){

			 extraInfo.from_date = billerInfos.get(i).getFromDate();

			 }else if(billerInfos.get(i).getToDate()!= null &&  !"".equalsIgnoreCase(billerInfos.get(i).getToDate())){

			 extraInfo.to_date = billerInfos.get(i).getToDate();

			 }*/
            billInfoService.addExtraInfo(extraInfo);

            log.add("--------------", "item " + (i + 1));
            log.add("billId", billerInfos.get(i).getBillID() == null ? "null" : billerInfos.get(i).getBillID());
            log.add("userId", billerInfos.get(i).getUserID() == null ? "null" : billerInfos.get(i).getUserID());
            log.add("amount", billerInfos.get(i).getAmount() == 0 ? "0" : billerInfos.get(i).getAmount() + "");
            log.add("cycle", billerInfos.get(i).getCycle() == null ? "null" : billerInfos.get(i).getCycle());
            log.add("fromDate", billerInfos.get(i).getFromDate() == null ? "null" : billerInfos.get(i).getFromDate());
            log.add("toDate", billerInfos.get(i).getToDate() == null ? "null" : billerInfos.get(i).getToDate());
            log.add("serialNumber", billerInfos.get(i).getSerialNumber() == null ? "null" : billerInfos.get(i).getSerialNumber());
            log.add("serialSecret", billerInfos.get(i).getSecretNumber() == null ? "null" : billerInfos.get(i).getSecretNumber());
        }

        return billInfoService;
    }

    public static ProxyRequest createCheckInforRequest(String debitor, String pin,
                                                       String serviceCode, String creditor, String billId) {
        return getProxyRequestObj(debitor, pin, serviceCode, creditor, billId,
                0, 0, 0, 1, "", "", "", "", "",
                ProxyRequestType.INFORMATION, Core.WalletType.MOMO_VALUE, TYPE_BILLPAY, 0, new JsonObject());
    }

    /**
     * processSubmitFrmTopupCheckInfor
     * @param debitor
     * @param pin
     * @param serviceCode
     * @param creditor
     * @param billId
     * @return
     */
    public static ProxyRequest createSubmitFrmTopupCheckInforRequest(String debitor, String pin,
                                                       String serviceCode, String creditor, String billId, Map<String, String> hashDatas) {
        String quantity = hashDatas.containsKey(Const.AppClient.Quantity) ? hashDatas.get(Const.AppClient.Quantity) : "1";
        String amount = hashDatas.containsKey(Const.AppClient.Amount) ? hashDatas.get(Const.AppClient.Amount) : "0";
        String customerName = hashDatas.containsKey(Const.AppClient.FullName) ? hashDatas.get(Const.AppClient.FullName) : "0";

        return getProxyRequestObj(debitor, pin, serviceCode, creditor, billId,
                DataUtil.strToLong(amount), 0, 0, DataUtil.strToInt(quantity), customerName, "", "", "", "",
                ProxyRequestType.INFORMATION, Core.WalletType.MOMO_VALUE, TYPE_BILLPAY, 0, new JsonObject());
    }


    public static ProxyRequest createPaymentRequest(String debitor, String pin,
                                                    String serviceCode, String creditor, String billId,
                                                    long amount, long voucher, long mpoint, int quantity,
                                                    String customerName, String customerInfo, String customerPhone,
                                                    String customerAddress, String customerEmail, long serviceFee, JsonObject joExtra) {
        return getProxyRequestObj(debitor, pin, serviceCode, creditor, billId,
                amount, voucher, mpoint, quantity, customerName, customerInfo,
                customerPhone, customerAddress, customerEmail,
                ProxyRequestType.PAYMENT, Core.WalletType.MOMO_VALUE, TYPE_BILLPAY, serviceFee, joExtra);
    }

    public static ProxyRequest cancelPaymentRequest(String debitor, String pin,
                                                    String serviceCode, String creditor, String billId,
                                                    long amount, long voucher, long mpoint, int quantity,
                                                    String customerName, String customerInfo, String customerPhone,
                                                    String customerAddress, String customerEmail, long serviceFee) {
        return getProxyRequestObj(debitor, pin, serviceCode, creditor, billId,
                amount, voucher, mpoint, quantity, customerName, customerInfo,
                customerPhone, customerAddress, customerEmail,
                ProxyRequestType.CANCEL_PAYMENT, Core.WalletType.MOMO_VALUE, TYPE_BILLPAY, serviceFee, new JsonObject());
    }

    public static ProxyRequest createTopupRequest(String debitor
            , String pin,
                                                  String serviceCode, String creditor, String billId,
                                                  long amount, long voucher, long mpoint, int quantity, String channel,
                                                  String customerName, String customerInfo, String customerPhone,
                                                  String customerAddress, String customerEmail) {
        ProxyRequest proxyRequest =
                getProxyRequestObj(debitor, pin, serviceCode, creditor, billId,
                        amount, voucher, mpoint, quantity, customerName, customerInfo,
                        customerPhone, customerAddress, customerEmail,
                        ProxyRequestType.TOPUP, Core.WalletType.MOMO_VALUE, TYPE_TOPUP, 0, new JsonObject());

        proxyRequest.addExtraValue("issms", "no");
        proxyRequest.addExtraValue("client", "backend");
        proxyRequest.addExtraValue("chanel", channel);

        return proxyRequest;
    }

    private static ProxyRequest getProxyRequestObj(String debitor,
                                                   String pin, String serviceCode, String creditor, String billId,
                                                   long amount, long voucher, long mpoint, int quantity,
                                                   String customerName, String customerInfo, String customerPhone,
                                                   String customerAddress, String customerEmail,
                                                   RequestType requestType, int walletType, String type, long serviceFee, JsonObject joExtra) {

        ProxyRequest proxyRequest = ProxyRequestFactory.createBackendRequest(requestType);

        //proxyRequest.setType(type);
        proxyRequest.setAmount(amount);
        proxyRequest.setVoucher(voucher);
        proxyRequest.setMpoint(mpoint);
        proxyRequest.setQuantity(quantity);
        proxyRequest.setTransId(Long.parseLong(String.format("%s%s", System.currentTimeMillis(), RandomStringUtils.randomNumeric(2))));
        proxyRequest.setDebitor(debitor);
        proxyRequest.setDebitorPin(pin);
        proxyRequest.setServiceCode(serviceCode.toUpperCase());
        proxyRequest.setBillId(billId);
        proxyRequest.setCreditor(creditor);
        proxyRequest.setDebitorWalletType(walletType);
        proxyRequest.setCustomerName(customerName);
        proxyRequest.setCustomerInfo(customerInfo);
        proxyRequest.setCustomerPhone(customerPhone);
        proxyRequest.setCustomerAddress(customerAddress);
        proxyRequest.setCustomerEmail(customerEmail);
        proxyRequest.setFee(serviceFee);
        proxyRequest.setVersion(joExtra.getInteger(colName.PhoneDBCols.APPCODE, 100));
        if(joExtra.getString(colName.PhoneDBCols.PHONE_OS, "").equalsIgnoreCase(StringConstUtil.ANDROID_OS))
        {
            proxyRequest.setOperation(Operation.ANDROID.getCode());
        }else if(joExtra.getString(colName.PhoneDBCols.PHONE_OS, "").equalsIgnoreCase(StringConstUtil.IOS_OS))
        {
            proxyRequest.setOperation(Operation.IOS.getCode());
        }
        else {
            proxyRequest.setOperation(Operation.WINDOWS_PHONE.getCode());
        }

        return proxyRequest;
    }


    //BEGIN 0000000030
    public static ProxyRequest createPaymentBackEndAgencyRequest(String debitor, String pin,
                                                                 String serviceCode, String creditor, String billId,
                                                                 long amount, long voucher, long mpoint, int quantity,
                                                                 String customerName, String customerInfo, String customerPhone,
                                                                 String customerAddress, String customerEmail, long serviceFee, JsonObject joExtra) {
        return getProxyRequestBackEndAgencyObj(debitor, pin, serviceCode, creditor, billId,
                amount, voucher, mpoint, quantity, customerName, customerInfo,
                customerPhone, customerAddress, customerEmail,
                ProxyRequestType.PAYMENT, Core.WalletType.MOMO_VALUE, TYPE_BILLPAY, serviceFee, joExtra);

    }

    private static ProxyRequest getProxyRequestBackEndAgencyObj(String debitor,
                                                                String pin, String serviceCode, String creditor, String billId,
                                                                long amount, long voucher, long mpoint, int quantity,
                                                                String customerName, String customerInfo, String customerPhone,
                                                                String customerAddress, String customerEmail,
                                                                RequestType requestType, int walletType, String type, long serviceFee, JsonObject joExtra) {

        ProxyRequest proxyRequest = ProxyRequestFactory.createBackendAgencyRequest(requestType);

        //proxyRequest.setType(type);
        proxyRequest.setAmount(amount);
        proxyRequest.setVoucher(voucher);
        proxyRequest.setMpoint(mpoint);
        proxyRequest.setQuantity(quantity);
        proxyRequest.setTransId(Long.parseLong(String.format("%s%s", System.currentTimeMillis(), RandomStringUtils.randomNumeric(2))));
        proxyRequest.setDebitor(debitor);
        proxyRequest.setDebitorPin(pin);
        proxyRequest.setServiceCode(serviceCode.toUpperCase());
        proxyRequest.setBillId(billId);
        proxyRequest.setCreditor(creditor);
        proxyRequest.setDebitorWalletType(walletType);
        proxyRequest.setCustomerName(customerName);
        proxyRequest.setCustomerInfo(customerInfo);
        proxyRequest.setCustomerPhone(customerPhone);
        proxyRequest.setCustomerAddress(customerAddress);
        proxyRequest.setCustomerEmail(customerEmail);
        proxyRequest.setFee(serviceFee);
        proxyRequest.setVersion(joExtra.getInteger(colName.PhoneDBCols.APPCODE, 100));
        if(joExtra.getString(colName.PhoneDBCols.PHONE_OS, "").equalsIgnoreCase(StringConstUtil.ANDROID_OS))
        {
            proxyRequest.setOperation(Operation.ANDROID.getCode());
        }else if(joExtra.getString(colName.PhoneDBCols.PHONE_OS, "").equalsIgnoreCase(StringConstUtil.IOS_OS))
        {
            proxyRequest.setOperation(Operation.IOS.getCode());
        }
        else {
            proxyRequest.setOperation(Operation.WINDOWS_PHONE.getCode());
        }
        return proxyRequest;
    }
    //END 0000000030

}
