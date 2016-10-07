
package com.mservice.momo.connector;

import com.mservice.conf.ResultCode;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.gateway.internal.connectorproxy.ConnectorCommon;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.TransferProcess;
import com.mservice.proxy.entity.ProxyRequest;
import com.mservice.proxy.entity.ProxyResponse;
import com.mservice.shopping.entity.PaymentResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;

import javax.xml.bind.DatatypeConverter;

/**
 * @author vu
 */
public class ServiceHelper {

    private static void createTransactionHistory(Vertx vertx,
                                                 long cmdIndex, int cmdPhone, ProxyResponse response, MomoProto.TranHisV1 tranHisV1,
                                                 Common common, NetSocket netSocket) {

        TranObj tranObjWithEncrypt = createEncryptTransactionObject(response, tranHisV1, cmdIndex);
//        TransDb.TranObj tranObj = createTransactionObject(response, tranHisV1, cmdIndex);
        insertTransactionHistory(vertx, cmdPhone, tranObjWithEncrypt.getJSON(), new NoActionHander());

        TranObj tranObj = createTransactionObject(response, tranHisV1, cmdIndex);
        Buffer buffer = createClientInfo(tranObj, cmdIndex, cmdPhone);
        if(netSocket != null)
        {
            common.writeDataToSocket(netSocket, buffer);
        }

    }

    private static TranObj createEncryptTransactionObject(ProxyResponse response, MomoProto.TranHisV1 tranHisV1,
                                                           long cmdIndex) {
        TranObj tranObj = new TranObj();
        int proxyResponseCode = response.getProxyResponseCode();
        ProxyRequest proxyRequest = response.getRequest();
        tranObj.tranId = proxyRequest.getCoreTransId();
        tranObj.clientTime = tranHisV1.getClientTime();
        tranObj.ackTime = System.currentTimeMillis();
        tranObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
        tranObj.amount = proxyRequest.getAmount();
        tranObj.status = (proxyResponseCode == ResultCode.SUCCESS.getCode() ? TranObj.STATUS_OK : TranObj.STATUS_FAIL);
        tranObj.error = proxyResponseCode;
        tranObj.cmdId = cmdIndex;
        tranObj.billId = isEmpty(response.getCustomerName())
                ? nullToEmpty(tranHisV1.getBillId()) : (response.getCustomerName() + (isEmpty(
                tranHisV1.getBillId()) ? "" : (" - " + tranHisV1.getBillId())));
        tranObj.tranType = tranHisV1.getTranType();
        tranObj.partnerName = DataUtil.nullToEmpty(tranHisV1.getPartnerName());
        tranObj.parterCode = DataUtil.nullToEmpty(tranHisV1.getPartnerCode());
        tranObj.partnerRef = DataUtil.nullToEmpty(tranHisV1.getPartnerRef());
        tranObj.partnerId = DataUtil.nullToEmpty(tranHisV1.getPartnerId());
        tranObj.io = -1;
        tranObj.comment = response.getTransactionContent();
        tranObj.source_from = tranHisV1.getSourceFrom();
        tranObj.desc = DataUtil.nullToEmpty(response.getProxyResponseMessage());
        //{"otp":""}
        if(Misc.isValidJsonArray(tranHisV1.getShare()) || tranHisV1.getShare() == null || "".equalsIgnoreCase(tranHisV1.getShare()) || tranHisV1.getShare().isEmpty())
        {
            tranObj.share = createExtraEncryptData(response, tranHisV1, tranHisV1.getShare());
        }
        else {
            try{
                JsonObject joShare = new JsonObject(tranHisV1.getShare());
                JsonArray jrShare = new JsonArray();
                jrShare.add(joShare);
                tranObj.share = createExtraEncryptData(response, tranHisV1, jrShare.toString());

            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                ex.toString();
            }
        }

//        else if(!"".equalsIgnoreCase(tranHisV1.getShare())){
//            JsonArray jsonArray = new JsonArray();
//            jsonArray.add(tranHisV1.getShare());
//            tranObj.share = jsonArray;
//        }

//        if (tranObj.error == 17) {
//            tranObj.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
//            tranObj.desc = "Số tiền thanh toán không hợp lệ.";
//        }

        return tranObj;
    }

    private static TranObj createTransactionObject(ProxyResponse response, MomoProto.TranHisV1 tranHisV1,
                                                           long cmdIndex) {
        TranObj tranObj = new TranObj();
        int proxyResponseCode = response.getProxyResponseCode();
        ProxyRequest proxyRequest = response.getRequest();
        tranObj.tranId = proxyRequest.getCoreTransId();
        tranObj.clientTime = tranHisV1.getClientTime();
        tranObj.ackTime = System.currentTimeMillis();
        tranObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
        tranObj.amount = proxyRequest.getAmount();
        tranObj.status = (proxyResponseCode == ResultCode.SUCCESS.getCode() ? TranObj.STATUS_OK : TranObj.STATUS_FAIL);
        tranObj.error = proxyResponseCode;
        tranObj.cmdId = cmdIndex;
        tranObj.billId = isEmpty(response.getCustomerName())
                ? nullToEmpty(tranHisV1.getBillId()) : (response.getCustomerName() + (isEmpty(
                tranHisV1.getBillId()) ? "" : (" - " + tranHisV1.getBillId())));
        tranObj.tranType = tranHisV1.getTranType();
        tranObj.partnerName = DataUtil.nullToEmpty(tranHisV1.getPartnerName());
        tranObj.parterCode = DataUtil.nullToEmpty(tranHisV1.getPartnerCode());
        tranObj.partnerRef = DataUtil.nullToEmpty(tranHisV1.getPartnerRef());
        tranObj.partnerId = DataUtil.nullToEmpty(tranHisV1.getPartnerId());
        tranObj.io = -1;
        tranObj.comment = response.getTransactionContent();
        tranObj.source_from = tranHisV1.getSourceFrom();
        tranObj.desc = DataUtil.nullToEmpty(response.getProxyResponseMessage());
        if(Misc.isValidJsonArray(tranHisV1.getShare()) || tranHisV1.getShare() == null || "".equalsIgnoreCase(tranHisV1.getShare()) || tranHisV1.getShare().isEmpty())
        {
            tranObj.share = createExtraData(response, tranHisV1);
        }
//        else if(!"".equalsIgnoreCase(tranHisV1.getShare())){
//            JsonArray jsonArray = new JsonArray();
//            jsonArray.add(tranHisV1.getShare());
//            tranObj.share = jsonArray;
//        }

//        if (tranObj.error == 17) {
//            tranObj.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
//            tranObj.desc = "Số tiền thanh toán không hợp lệ.";
//        }

        return tranObj;
    }

    private static JsonArray createExtraData(ProxyResponse response, MomoProto.TranHisV1 tranHisV1) {

        JsonArray arrayShared = tranHisV1.getShare() == null || tranHisV1.getShare().isEmpty() ? new JsonArray() : new JsonArray(tranHisV1.getShare());

        String html = response.getHtmlContent();
        String qrCode = response.getQrCode();
        if (!"".equalsIgnoreCase(html)) {
           arrayShared.add(new JsonObject().putString(Const.AppClient.Html, html));

        }
        if (!"".equalsIgnoreCase(qrCode)) {
            arrayShared.add(new JsonObject().putString(Const.AppClient.Qrcode, qrCode));
        }

        if (!arrayShared.contains(Const.DGD.CusNumber)) {
            String creditor = response.getRequest().getJsonObject().getString("creditor", "");

            if (!"".equalsIgnoreCase(creditor)) {
                arrayShared.add(new JsonObject().putString(Const.DGD.CusNumber, creditor));
            }
        }

        return arrayShared;
    }

    private static JsonArray createExtraEncryptData(ProxyResponse response, MomoProto.TranHisV1 tranHisV1, String share) {

        String html = response.getHtmlContent();
        JsonArray arrayShared = "".equalsIgnoreCase(share) || tranHisV1.getShare() == null || tranHisV1.getShare().isEmpty() ? new JsonArray() : new JsonArray(share);

        String html_encr;
        html_encr = "".equalsIgnoreCase(html) ? html : DatatypeConverter.printBase64Binary(html.getBytes());
        String qrCode = response.getQrCode();
        if (!"".equalsIgnoreCase(html_encr)) {
//            arrayShared.add(new JsonObject().putString(Const.AppClient.Html, html));
              arrayShared.add(new JsonObject().putString(Const.AppClient.Html, html_encr));
        }
        if (!"".equalsIgnoreCase(qrCode)) {
            arrayShared.add(new JsonObject().putString(Const.AppClient.Qrcode, qrCode));
        }

        if (!arrayShared.contains(Const.DGD.CusNumber)) {
            String creditor = response.getRequest().getJsonObject().getString("creditor", "");

            if (!"".equalsIgnoreCase(creditor)) {
                arrayShared.add(new JsonObject().putString(Const.DGD.CusNumber, creditor));
            }
        }

        return arrayShared;
    }

    private static Buffer createClientInfo(TranObj tranObj,
                                           long cmdIndex, int cmdPhone) {
        //send to client
        MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
        builder.setTranId(tranObj.tranId)
                .setClientTime(tranObj.clientTime)
                .setAckTime(tranObj.ackTime)
                .setFinishTime(tranObj.finishTime)
                .setTranType(tranObj.tranType)
                .setIo(tranObj.io)
                .setCategory(tranObj.category)
                .setPartnerId(tranObj.partnerId)
                .setPartnerCode(tranObj.parterCode)
                .setPartnerName(tranObj.partnerName)
                .setPartnerRef(tranObj.partnerRef)
                .setBillId(tranObj.billId)
                .setAmount(tranObj.amount)
                .setComment(tranObj.comment)
                .setCommandInd(tranObj.cmdId)
                .setStatus(tranObj.status)
                .setError(tranObj.error)
                .setSourceFrom(tranObj.source_from)
                .setDesc(DataUtil.nullToEmpty(tranObj.desc))
                .setShare(tranObj.share.toString());
        builder.addKvp(MomoProto.TextValue.newBuilder().setText("fcmt").setValue("1"));
        Buffer buf = MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REPLY_VALUE,
                cmdIndex, cmdPhone, builder.build().toByteArray());
        return buf;
    }

    public static void doPayment(final NetSocket netSocket, final MomoMessage msg,
                                 final Vertx vertx, final ProxyRequest proxyRequest,
                                 final Common common,
                                 final MomoProto.TranHisV1 request,
                                 final Common.BuildLog log, JsonObject globalConfig,
                                 String busAddress, final Handler<ProxyResponse> calback) {
//        if(proxyRequest.getServiceCode().equalsIgnoreCase("survey")){
//            proxyRequest = setValueSurveytoReques(request, proxyRequest);
//        }      
        ConnectorCommon.requestPayment(vertx, proxyRequest, busAddress, log, globalConfig, new Handler<ProxyResponse>() {
            @Override
            public void handle(ProxyResponse proxyResponse) {
                final long tranId = proxyResponse.getRequest().getCoreTransId();

                Misc.buildNotiAndSend(proxyResponse.getNotificationHeader(),
                        proxyResponse.getNotificationContent(), tranId, msg.cmdPhone, msg.cmdIndex, vertx);

                createTransactionHistory(vertx
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , proxyResponse
                        , request
                        , common
                        , netSocket);

//                log.writeLog();
                calback.handle(proxyResponse);
            }
        });
    }


    public static void doMerchantPayment(final Vertx vertx, final ProxyRequest proxyRequest,
                                 final Common.BuildLog log,
                                 String busAddress, JsonObject globalConfig, final Handler<ProxyResponse> calback) {
//        if(proxyRequest.getServiceCode().equalsIgnoreCase("survey")){
//            proxyRequest = setValueSurveytoReques(request, proxyRequest);
//        }

        ConnectorCommon.requestPayment(vertx, proxyRequest, busAddress, log, globalConfig, new Handler<ProxyResponse>() {
            @Override
            public void handle(ProxyResponse proxyResponse) {
                log.add("requestPayment", "tra ve proxy response");
//                log.writeLog();
                calback.handle(proxyResponse);
            }
        });
    }

    private static void insertTransactionHistory(final Vertx vertx, final int number, final JsonObject tranObjJson, final Handler<Object> callback) {

        TransferProcess.transDb.upsertTran(number, tranObjJson, new Handler<TranObj>() {
            @Override
            public void handle(TranObj tranObj) {
                callback.handle(tranObj);
            }
        });
    }

    static boolean isEmpty(Object object) {
        return object == null || object.toString().isEmpty();
    }

    static String nullToEmpty(Object object) {
        return object == null ? "" : object.toString();
    }

    public static void createTransactionShoppingHistory(Vertx vertx,
                                                        long cmdIndex, int cmdPhone, JsonObject response, MomoProto.TranHisV1 tranHisV1,
                                                        Common common, NetSocket netSocket, Common.BuildLog log,
                                                        String serviceId, int version) {
        if (TransferProcess.checkNewAPI(serviceId, version)) {
            final com.mservice.proxy.entity.v2.ProxyResponse paymentResponse = new com.mservice.proxy.entity.v2.ProxyResponse(response);
            TranObj tranObj = createTransactionShoppingObject(paymentResponse, tranHisV1, cmdIndex);
            log.add("createTransactionShoppingObject", tranObj.getJSON());
            insertTransactionHistory(vertx, cmdPhone, tranObj.getJSON(), new NoActionHander());
            Buffer buffer = createClientInfo(tranObj, cmdIndex, cmdPhone);
            common.writeDataToSocket(netSocket, buffer);
        } else {
            final PaymentResponse paymentResponse = new PaymentResponse(response);
            TranObj tranObj = createTransactionShoppingObject(paymentResponse, tranHisV1, cmdIndex);
            log.add("createTransactionShoppingObject", tranObj.getJSON());
            insertTransactionHistory(vertx, cmdPhone, tranObj.getJSON(), new NoActionHander());
            Buffer buffer = createClientInfo(tranObj, cmdIndex, cmdPhone);
            common.writeDataToSocket(netSocket, buffer);
        }
    }

    private static TranObj createTransactionShoppingObject(PaymentResponse response, MomoProto.TranHisV1 tranHisV1,
                                                                   long cmdIndex) {
        TranObj tranObj = new TranObj();
        int proxyResponseCode = response.getResultCode();
        //PaymentResponse paymentResponse = response.get;
        tranObj.tranId = response.getRequest() != null ? response.getRequest().getCoreTransId() : 0;
        tranObj.clientTime = tranHisV1.getClientTime();
        tranObj.ackTime = System.currentTimeMillis();
        tranObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
        tranObj.amount = response.getRequest() != null ? response.getRequest().getAmount() : 0;
        tranObj.status = (proxyResponseCode == ResultCode.SUCCESS.getCode() ? TranObj.STATUS_OK : TranObj.STATUS_FAIL);
        tranObj.error = proxyResponseCode;
        tranObj.cmdId = cmdIndex;
        tranObj.billId = response.getRequest() != null ? (response.getRequest().getBillId() != null ? response.getRequest().getBillId() : "") : "";
        tranObj.tranType = tranHisV1.getTranType();
        tranObj.partnerName = DataUtil.nullToEmpty(tranHisV1.getPartnerName());
        tranObj.parterCode = DataUtil.nullToEmpty(tranHisV1.getPartnerCode());
        tranObj.partnerRef = DataUtil.nullToEmpty(tranHisV1.getPartnerRef());
        tranObj.partnerId = DataUtil.nullToEmpty(tranHisV1.getPartnerId());
        tranObj.io = -1;
        tranObj.comment = response.getTransactionComment();
        tranObj.source_from = tranHisV1.getSourceFrom();
        tranObj.share = createExtraShoppingData(response, tranHisV1);
        tranObj.desc = DataUtil.nullToEmpty(response.getResultMessage());
//        if (tranObj.error == 17) {
//            tranObj.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
//            tranObj.desc = "Số tiền thanh toán không hợp lệ.";
//        }

        return tranObj;
    }

    private static TranObj createTransactionShoppingObject(com.mservice.proxy.entity.v2.ProxyResponse response, MomoProto.TranHisV1 tranHisV1,
                                                                   long cmdIndex) {
        TranObj tranObj = new TranObj();
        int proxyResponseCode = response.getResultCode();
        //PaymentResponse paymentResponse = response.get;
        tranObj.tranId = response.getRequest() != null ? response.getRequest().getCoreTransId() : 0;
        tranObj.clientTime = tranHisV1.getClientTime();
        tranObj.ackTime = System.currentTimeMillis();
        tranObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
        tranObj.amount = response.getRequest() != null ? response.getRequest().getAmount() : 0;
        tranObj.status = (proxyResponseCode == ResultCode.SUCCESS.getCode() ? TranObj.STATUS_OK : TranObj.STATUS_FAIL);
        tranObj.error = proxyResponseCode;
        tranObj.cmdId = cmdIndex;
        if (response.getBillList().size() > 0) {
            tranObj.billId = response.getBillList().size() > 0 ? response.getBillList().get(0).getBillID() : "";
        }
        tranObj.tranType = tranHisV1.getTranType();
        tranObj.partnerName = DataUtil.nullToEmpty(tranHisV1.getPartnerName());
        tranObj.parterCode = DataUtil.nullToEmpty(tranHisV1.getPartnerCode());
        tranObj.partnerRef = DataUtil.nullToEmpty(tranHisV1.getPartnerRef());
        tranObj.partnerId = DataUtil.nullToEmpty(tranHisV1.getPartnerId());
        tranObj.io = -1;
        tranObj.comment = response.getPushInfo() != null && response.getPushInfo().getHistory() != null ? response.getPushInfo().getHistory().getTransactionContent() : "";
        tranObj.source_from = tranHisV1.getSourceFrom();
        tranObj.share = createExtraShoppingData(response, tranHisV1);
        tranObj.desc = DataUtil.nullToEmpty(response.getResultMessage());
//        if (tranObj.error == 17) {
//            tranObj.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
//            tranObj.desc = "Số tiền thanh toán không hợp lệ.";
//        }

        return tranObj;
    }

    private static JsonArray createExtraShoppingData(PaymentResponse response, MomoProto.TranHisV1 tranHisV1) {

//        JsonArray arrayShared = tranHisV1.getShare() == null || tranHisV1.getShare().isEmpty() ? new JsonArray() : new JsonArray(tranHisV1.getShare());
        JsonArray arrayShared = new JsonArray();
        String html = response.getHtmlContent();
        JsonObject jsonHtml = new JsonObject();
        jsonHtml.putString(Const.AppClient.Html, html);
        //String qrCode = response.getQrCode();
        if (!"".equalsIgnoreCase(html)) {
            arrayShared.add(jsonHtml);
        }
//        if (!"".equalsIgnoreCase(qrCode)) {
//            arrayShared.add(new JsonObject().putString(Const.AppClient.Qrcode, qrCode));
//        }

//        if(!arrayShared.contains(Const.DGD.CusNumber)){
//            String creditor = response.getRequest().getJsonObject().getString("creditor","");
//
//            if(!"".equalsIgnoreCase(creditor)){
//                arrayShared.add(new JsonObject().putString(Const.DGD.CusNumber, creditor));
//            }
//        }

        return arrayShared;
    }

    private static JsonArray createExtraShoppingData(com.mservice.proxy.entity.v2.ProxyResponse response, MomoProto.TranHisV1 tranHisV1) {

//        JsonArray arrayShared = tranHisV1.getShare() == null || tranHisV1.getShare().isEmpty() ? new JsonArray() : new JsonArray(tranHisV1.getShare());
        JsonArray arrayShared = new JsonArray();
        String html = response.getPushInfo() != null && response.getPushInfo().getHistory() != null ? response.getPushInfo().getHistory().getHtmlContent() : "";
        JsonObject jsonHtml = new JsonObject();
        jsonHtml.putString(Const.AppClient.Html, html);
        //String qrCode = response.getQrCode();
        if (!"".equalsIgnoreCase(html)) {
            arrayShared.add(jsonHtml);
        }
        JsonObject requestExtra = response.getRequest().getExtraValue();
        String coreGroupId = requestExtra.getString("coreGroupId", "");
        if (!"".equalsIgnoreCase(coreGroupId)) {
            JsonObject groupIdJson = new JsonObject();
            groupIdJson.putString("coreGroupId", coreGroupId);
            arrayShared.add(groupIdJson);
        }

//        if (!"".equalsIgnoreCase(qrCode)) {
//            arrayShared.add(new JsonObject().putString(Const.AppClient.Qrcode, qrCode));
//        }

//        if(!arrayShared.contains(Const.DGD.CusNumber)){
//            String creditor = response.getRequest().getJsonObject().getString("creditor","");
//
//            if(!"".equalsIgnoreCase(creditor)){
//                arrayShared.add(new JsonObject().putString(Const.DGD.CusNumber, creditor));
//            }
//        }

        return arrayShared;
    }

    static class NoActionHander<T> implements Handler<T> {

        @Override
        public void handle(T event) {

        }
    }
}
