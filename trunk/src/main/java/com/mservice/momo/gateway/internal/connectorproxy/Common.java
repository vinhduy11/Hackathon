package com.mservice.momo.gateway.internal.connectorproxy;

import com.mservice.momo.data.BillInfoService;
import com.mservice.proxy.entity.*;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.List;



public class Common {
    public static String jsonkey = "connector_proxy_busname";
    public static void RequestCommand(Vertx _vertx
            ,final ProxyRequest proxyRequest
            ,final String busName
            ,final com.mservice.momo.vertx.processor.Common.BuildLog log
            ,final Handler<BillInfoService> callback){


        log.add("function","Request");
        log.add("json request to connector", proxyRequest.getJsonObject());

        _vertx.eventBus().sendWithTimeout(busName
                                            ,proxyRequest.getJsonObject()
                                            ,90*1000
                                            ,new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {

                BillInfoService billInfoService = new BillInfoService();
                //thanh cong
                if(messageAsyncResult.succeeded()){
                    JsonObject jo =  messageAsyncResult.result().body();
                    ProxyResponse response = new ProxyResponse(jo);

                    //build info service
                    billInfoService = buildBillInfoService(response, log);

                    callback.handle(billInfoService);
                }else {
                    //xem nhu timeout
                    billInfoService.total_amount = -100;
                    log.add("error",-100);
                    log.add("desc", "Request timeout");
                    callback.handle(billInfoService);
                }
            }
        });
    }

    private static BillInfoService buildBillInfoService(ProxyResponse response
                                                        ,com.mservice.momo.vertx.processor.Common.BuildLog log){
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
        String proxyResponseMessage = response.getProxyResponseMessage();
        long totalAmount =  response.getTotalAmount();
        String partnerTransId = response.getPartnerTransId();
        String customerId = response.getCustomerId();
        String customerName = response.getCustomerName();
        String customerInfo = response.getCustomerInfo();
        String customerPhone = response.getCustomerPhone();
        String customerAddress = response.getCustomerAddress();
        /*String serialNumber = response.getSerialNumber();
        String secretNumber = response.getSecretNumber();*/

        log.add("proxyResponseCode",proxyResponseCode);
        log.add("proxyResponseMessage",proxyResponseMessage);
        log.add("totalAmount",totalAmount);
        log.add("partnerTransId",partnerTransId);
        log.add("customerId",customerId);
        log.add("customerName",customerName);

        log.add("customerInfo",customerInfo);
        log.add("customerPhone",customerPhone);
        log.add("customerAddress",customerAddress);
        /*log.add("serialNumber",serialNumber);
        log.add("secretNumber",secretNumber);*/

        //thong tin chung
        billInfoService.total_amount = totalAmount;


        /*if (mapRes.containsKey("cusName")) {
            cusInfo = new BillInfoService.TextValue();
            cusInfo.text = "ht";
            cusInfo.value = mapRes.get("cusName");
            if (cusInfo.value!=null && !"".equalsIgnoreCase(cusInfo.value))
                billInfoService.addCustomInfo(cusInfo);
        }*/

        if(customerName != null && !"".equalsIgnoreCase(customerName)){
            BillInfoService.TextValue cusName = new BillInfoService.TextValue();
            cusName.text = "ht";
            cusName.value = customerName;
            billInfoService.addCustomInfo(cusName);
        }

        if(customerPhone != null && !"".equalsIgnoreCase(customerPhone)){
            BillInfoService.TextValue cusInfo = new BillInfoService.TextValue();
            cusInfo.text = "sdt";
            cusInfo.value = customerPhone;
            billInfoService.addCustomInfo(cusInfo);
        }

        if(customerAddress != null && !"".equalsIgnoreCase(customerAddress)){
            BillInfoService.TextValue cusAddress = new BillInfoService.TextValue();
            cusAddress.text = "dc";
            cusAddress.value = customerAddress;
            billInfoService.addCustomInfo(cusAddress);
        }


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
        List<BillerInfo> billerInfos = null; /* response.getList();*/

        for(int i =0 ; i< billerInfos.size(); i++){
            BillInfoService.ExtraInfo extraInfo = new BillInfoService.ExtraInfo();

            extraInfo.bill_detail_id = (billerInfos.get(i).getBillID() == null ? "" : billerInfos.get(i).getBillID());

            extraInfo.amount = (billerInfos.get(i).getAmount() == 0 ?  "0" : billerInfos.get(i).getAmount() + "");

            //uu tien lay theo ky thanh toan truoc
            if(billerInfos.get(i).getCycle()!= null &&  !"".equalsIgnoreCase(billerInfos.get(i).getCycle())) {

                extraInfo.from_date = "Kỳ " + billerInfos.get(i).getCycle();

                extraInfo.to_date = "Kỳ " + billerInfos.get(i).getCycle();
            }else {
                //lay theo ngay thanh toan
                extraInfo.from_date = billerInfos.get(i).getFromDate() == null ? "" : billerInfos.get(i).getFromDate();
                extraInfo.to_date = billerInfos.get(i).getToDate() == null ? "" : billerInfos.get(i).getToDate() ;

            }

            /*

            }else if(billerInfos.get(i).getFromDate()!= null &&  !"".equalsIgnoreCase(billerInfos.get(i).getFromDate())){

                extraInfo.from_date = billerInfos.get(i).getFromDate();

            }else if(billerInfos.get(i).getToDate()!= null &&  !"".equalsIgnoreCase(billerInfos.get(i).getToDate())){

                extraInfo.to_date = billerInfos.get(i).getToDate();

            }*/

            billInfoService.addExtraInfo(extraInfo);

            log.add("--------------","item " + (i + 1));
            log.add("billId",billerInfos.get(i).getBillID() == null ? "null" : billerInfos.get(i).getBillID());
            log.add("userId",billerInfos.get(i).getUserID() == null ? "null" : billerInfos.get(i).getUserID());
            log.add("amount", billerInfos.get(i).getAmount() + "");
            log.add("cycle",billerInfos.get(i).getCycle() == null ? "null" : billerInfos.get(i).getCycle());
            log.add("fromDate",billerInfos.get(i).getFromDate() == null ? "null" : billerInfos.get(i).getFromDate());
            log.add("toDate",billerInfos.get(i).getToDate() == null ? "null" : billerInfos.get(i).getToDate());

        }

        return  billInfoService;
    }


    public static ViaConnectorObj getViaConnectorObj(JsonObject glbCfg, String serviceId, com.mservice.momo.vertx.processor.Common.BuildLog log){
      
        String busName = "";
        String billpay = "";
        boolean isViaConnectorVerticle = false;
        log.add("jsonkey",jsonkey);

        JsonObject jo =  glbCfg.getObject(com.mservice.momo.gateway.internal.connectorproxy.Common.jsonkey, null);
        if(jo != null){
            JsonObject segmentInfo = jo.getObject(serviceId, null);



            if(segmentInfo != null){
                busName = segmentInfo.getString("busname","");
                billpay = segmentInfo.getString("billpay","");

                if(!"".equalsIgnoreCase(busName) && !"".equalsIgnoreCase(billpay)){
                    isViaConnectorVerticle = true;
                }
            }else{
                log.add("get json by " + serviceId, "null");
            }
        }else{
            log.add("get json by " + jsonkey, "null");
        }

        log.add("busname", busName);
        log.add("billpay", billpay);
        log.add("is check bill via connector verticle", isViaConnectorVerticle);

        ViaConnectorObj viaObj = new ViaConnectorObj();
        viaObj.IsViaConnectorVerticle = isViaConnectorVerticle;
        viaObj.BillPay = billpay;
        viaObj.BusName = busName;
        return viaObj;
    }

    public static ProxyRequest getProxyRequestObj(String phoneNumber
                                                ,String pin
                                                ,String serviceCode
                                                ,String billPay
                                                ,String billId
                                                ,long amount
                                                ,RequestType requestType
                                                ,final com.mservice.momo.vertx.processor.Common.BuildLog log){

        //ProxyRequest proxyRequest = ProxyRequestFactory.createBackendRequest(RequestType.INFORMATION);
        ProxyRequest proxyRequest = ProxyRequestFactory.createBackendRequest(requestType);

        //chung cho thanh toan va check bill
        // thanh toan

        //RequestType.PAYMENT;
//        proxyRequest.setType("billpay"); // connector se dinh nghia them cai nay cho thong nhat ve sau
        proxyRequest.setAmount(amount);
        proxyRequest.setTransId(System.currentTimeMillis());
        proxyRequest.setDebitor(phoneNumber);
        proxyRequest.setDebitorPin(pin);
        proxyRequest.setServiceCode(serviceCode);
        proxyRequest.setBillId(billId);
        proxyRequest.setCreditor(billPay);

//        log.add("type", proxyRequest.getType());
        log.add("TransId", proxyRequest.getTransId());
        log.add("debitor", proxyRequest.getDebitor());
        log.add("debitor pin len", proxyRequest.getDebitorPin().length());
        log.add("service code", proxyRequest.getServiceCode());
        log.add("biller id", proxyRequest.getBillId());
        log.add("creditor", proxyRequest.getCreditor());

        return proxyRequest;
    }

}
