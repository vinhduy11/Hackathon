package com.mservice.momo.web.external.services.pay123;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.Track123PayNotifyDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.Constant123Pay;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.HttpResponseCommon;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.Map;

/**
 * Created by locnguyen on 30/07/2014.
 */
public class Controller123Pay {

    private Vertx vertx;
    private Logger logger;
    private TransDb transDb;
    private Track123PayNotifyDb track123PayNotify;

    //not used at this time
    //private Long TIMEOUT_123PAY = 30000L;

    //tai khoan dung de adjustment
    private String pay123_account  = "";
    private String pay123_account_visa_master = "";

    //tai khoan dung de refund
    private String refund_agent_for_internal ="";
    private String refund_agent_for_visa_master ="";

    private long refund_date_from =0;
    private long refund_date_to = 0;

    private String pay123_key ="";
    private long pay123_expire_time = 0;

    private Common mCom;
    private PromotionProcess promotionProcess;
    public Controller123Pay(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        track123PayNotify = new Track123PayNotifyDb(vertx.eventBus(),logger);

        JsonObject glbCfg = container.config();
        JsonObject pay123Cfg = glbCfg.getObject("pay123");

        pay123_account = pay123Cfg.getString("account","");
        pay123_account_visa_master = pay123Cfg.getString("account_visa_master","");
        pay123_key = pay123Cfg.getString("key","");
        pay123_expire_time = pay123Cfg.getLong("expire_time", 5);

        refund_agent_for_internal = glbCfg.getObject("refund").getString("pay123_agent_internal","");
        refund_agent_for_visa_master =glbCfg.getObject("refund").getString("pay123_agent_credit","");

        refund_date_from = Misc.getDateAsLong(glbCfg.getObject("refund").getString("date_from", ""), "yyyy-MM-dd HH:mm:ss", logger, "Ngày bắt đầu hoàn phí");
        refund_date_to = Misc.getDateAsLong(glbCfg.getObject("refund").getString("date_to", ""), "yyyy-MM-dd HH:mm:ss", logger, "Ngày kết thúc hoàn phí");

        mCom = new Common(vertx,logger, container.config());
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);
    }

    @Action(path = "/123pay/notify")
    public void notify_123pay(final HttpRequestContext context
                                ,final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function","/123pay/notify");
        log.add("params", context.postData);

        //khong chap nhat phuong thuc GET
        if(context.getRequest().method().equalsIgnoreCase("GET")){
            return123Pay(context,"MethodGETIsInvalid",-3,log);
            log.add("phuong thuc GET la khong hop le","---------");
            log.writeLog();
            return;
        }

        Map<String, String> params = context.getPostParams();
        String bankCode = "";
        String transactionStatus = "";
        long ts = 0L;
        String checksum = "";
        long amount = 0L;

        // get params
        String tranIDTemp = "";
        if (params.containsKey("mTransactionID")) {
            tranIDTemp = params.get("mTransactionID");
            log.add("mTransactionID",tranIDTemp);
        }

        final String mTransactionID = tranIDTemp;

        if (params.containsKey("bankCode")) {
            bankCode = params.get("bankCode");
            log.add("bankCode",bankCode);
        }


        if (params.containsKey("transactionStatus")) {
            transactionStatus = params.get("transactionStatus");
            log.add("transactionStatus",transactionStatus);
        }

        String eventlog ="notifyfrom123pay: ";

        //trang thai don hang
        final int orderStatus = DataUtil.strToInt(transactionStatus);
        log.add("errorDesc",Constant123Pay.getDesc(orderStatus));

        eventlog +=" error - " + orderStatus;
        eventlog +=" local desc - " + Constant123Pay.getDesc(orderStatus);

        if (params.containsKey("ts")) {
            ts = DataUtil.stringToUNumber(params.get("ts"));
            log.add("ts",ts);
        }

        String description ="";
        if(params.containsKey("description")){
            description = params.get("description");
        }

        eventlog+=" 123pay desc - " + description;

        if (params.containsKey("checksum")) {
            checksum = params.get("checksum");
            log.add("checksum",checksum);
        }

        String[] tempList = mTransactionID.split("m");
        log.add("array after split size", (tempList != null ? tempList.length : "null"));

        if(tempList!=null && tempList.length!=4){
            return123Pay(context,"InvalidData",-3,log);

            eventlog+=" khong phai yeu cau cua mservice gui qua";
            if(orderStatus == 1){
                eventlog= log.getTime() + "|thanhcong" + eventlog;
            }else{
                eventlog= log.getTime() + "|thatbai" + eventlog;
            }
            logger.info(eventlog);

            log.add("Du lieu o dau do truyen ve ta, khong phai MService request roi","");
            log.writeLog();
            return;
        }

        if(orderStatus == 1){
            eventlog= log.getTime() + "|thanhcong" + eventlog;
        }else{
            eventlog= log.getTime() + "|thatbai" + eventlog;
        }

        logger.info(eventlog);

        int tempPhone = 0;
        String device = "";

        if (tempList.length>3) {
            tempPhone =  DataUtil.strToInt(tempList[1]);
            amount = DataUtil.stringToUNumber(tempList[2]);
            device = tempList[3];
            log.add("phone number", tempPhone);
            log.add("amount",amount);
            log.add("device", device);
        }

        if(amount <=0 || tempPhone <=0){
            return123Pay(context,"InvalidMTransactionID",-3,log);
            log.add("Du lieu o dau do truyen ve ta, khong phai MService request roi","");
            log.writeLog();
            return;
        }

        long fee = 0;

        //khong hoan phi
        int vmStatic = 0;
        double vmDynamic = 0;

        int otherStatic = 1100;
        double otherDynamic = 1.2;

        String tmpAdjustAccount = "";
        String tmpRefundAgent = "";
        if(bankCode != null
                && bankCode.toUpperCase().contains("123PCC")
                && !"".equalsIgnoreCase(pay123_account_visa_master)){

            tmpAdjustAccount = pay123_account_visa_master;
            fee = Misc.get123PayFee(amount, vmDynamic, vmStatic);
            tmpRefundAgent = refund_agent_for_visa_master;

        }else{

            tmpAdjustAccount = pay123_account;
            fee = Misc.get123PayFee(amount, otherDynamic, otherStatic);
            tmpRefundAgent = refund_agent_for_internal;
        }

        if("".equalsIgnoreCase(pay123_account_visa_master)){
            pay123_account_visa_master = pay123_account;
        }

        final String finalRefundAgent = tmpRefundAgent;
        log.add("refund agent",finalRefundAgent);
        final String finalAdjustAccount = tmpAdjustAccount;
        log.add("123pay adjust account", finalAdjustAccount);

        final Long finalFee = Misc.getRefundFee(refund_date_from
                , refund_date_to
                , fee
                , tempPhone
                , logger);
        log.add("fee",finalFee);

        //track 123pay notify
        final Track123PayNotifyDb.Obj trckNoti = new Track123PayNotifyDb.Obj();
        trckNoti.MTRANSACTIONID = mTransactionID;
        trckNoti.BANK_CODE = bankCode;

        trckNoti.TIMESTAMP = ts + "";
        trckNoti.DESCRIPTION =description;
        trckNoti.CHECK_SUM = checksum;
        trckNoti.TRAN_STATUS = transactionStatus;
        trckNoti.AMOUNT = amount;
        trckNoti.PHONE_NUMBER = tempPhone;
        trckNoti.ERROR_DESC = Constant123Pay.getDesc(orderStatus);

        final int phoneNumber = tempPhone;
        final String fBankCode = bankCode;
        final String fTranStatus = transactionStatus;
        final long fTs = ts;
        final String fCheckSum = checksum;
        final long fAmount = amount;

        //check sum invalid
        String owner = mTransactionID
                + fBankCode
                + fTranStatus
                + fTs
                + pay123_key ;

        String ownerCheckSum = DataUtil.getHMACSHA1WithoutKey(owner);

        if (!ownerCheckSum.equalsIgnoreCase(fCheckSum)) {

            return123Pay(context, mTransactionID, -1, log);
            log.add("errorDesc",Constant123Pay.getDesc(-1));
            log.add("checksum of 123pay is valid failed","");
            log.writeLog();
            return;
        }

        //kiem tra so dien thoai, cash bankcode khong hop le
        if (phoneNumber == 0 ||  fAmount == 0 || fBankCode.isEmpty()) {
            return123Pay(context, mTransactionID, -1, log);
            log.add("phone number, amount or bankcode is invalid","");
            log.add("errorDesc",Constant123Pay.getDesc(-1));
            log.writeLog();
            return;
        }

        track123PayNotify.isMTranProcessed(mTransactionID, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {

                log.add("function","track123PayNotify.isMTranProcessed");

                if(aBoolean) {
                    log.add("da xu ly cho mtransactionid nay roi","");
                    return123Pay(context,mTransactionID,2,log);
                    log.writeLog();
                    return;
                }

                track123PayNotify.save(trckNoti,new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        log.add("track 123pay", aBoolean);
                    }
                });

                log.setPhoneNumber("0" + phoneNumber);

                int returnCode = -3;// cap nhat trang thai don hang doi tac that bai

                final String orgBankCode = (fBankCode.startsWith("123P") ? fBankCode.replace("123P","") : fBankCode);

                //don hang khong thanh cong
                if(orderStatus != 1){

                    log.add("errorDesc", Constant123Pay.getDesc(orderStatus));

                    //tra ket qua ve 123pay
                    return123Pay(context, mTransactionID, returnCode, log);

                    //tao giao dich khong thanh cong
                    long ctime = System.currentTimeMillis();

                    final TranObj tranError = new TranObj();
                    tranError.owner_number = phoneNumber;
                    tranError.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                    tranError.tranId = ctime;
                    tranError.clientTime = ctime;
                    tranError.ackTime = ctime;
                    tranError.finishTime = ctime;//=> this must be the time we sync, or user will not sync this to device
                    tranError.amount = fAmount;
                    tranError.status = TranObj.STATUS_FAIL;
                    tranError.error = orderStatus + 15000; // ma loi duoc + them 15k
                    tranError.cmdId = ctime;
                    tranError.billId = mTransactionID; // phuc vu cho web truy van
                    tranError.category = 0;
                    tranError.io = 1;
                    tranError.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                    tranError.parterCode = "123pay";

                    tranError.owner_number = phoneNumber;

                    //todo thay doi 2 cai nay
                    //todo caption : Nạp tiền không thành công
                    //String tpl= "Quý khách đã nạp không thành công số tiền [xxx] từ ngân hàng [xxx] qua thẻ ATM.";

                    String tpl= "Quý khách đã nạp không thành công số tiền %s từ %s.";
                    String fullCmt = String.format(tpl
                                            , Misc.formatAmount(fAmount).replace(",",".")
                                            , Constant123Pay.getShortBankName(orgBankCode)
                    );

                    tranError.comment= fullCmt;
                    tranError.partnerId=orgBankCode;
                    tranError.partnerName = Constant123Pay.getShortBankName(orgBankCode);

                    log.add("tran JSON", tranError.getJSON());
                    log.add("function", "upsertTranOutSideNew");
                    transDb.upsertTranOutSideNew(tranError.owner_number, tranError.getJSON(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {

                            log.add("isUpdated", result);
                            if(!result){
                                BroadcastHandler.sendOutSideTransSync(vertx, tranError);

                                Notification noti = new Notification();
                                noti.receiverNumber = tranError.owner_number;
                                noti.caption = "Nạp tiền không thành công";
                                noti.body = tranError.comment ;
                                noti.sms =  "";
                                noti.priority = 1;
                                noti.time = System.currentTimeMillis();
                                noti.tranId = tranError.tranId;
                                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

                                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                        , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> message) {
                                    }
                                });
                            }

                            log.writeLog();
                        }
                    });

                    return;
                }else{
                    // don hang thanh cong
                    // TODO: xay dung keyvaluepair day xuong
                    SoapProto.keyValuePair.Builder kvpSrvName = SoapProto.keyValuePair.newBuilder()
                            .setKey(Const.SERVICE_NAME)
                            .setValue("123pay");

                    SoapProto.keyValuePair.Builder kvpPartnerId = SoapProto.keyValuePair.newBuilder()
                            .setKey(Const.PARTNER_ID)
                            .setValue(mTransactionID);

                    SoapProto.keyValuePair.Builder kvpRefundAmt = SoapProto.keyValuePair.newBuilder()
                            .setKey(Const.REFUND_AMOUNT)
                            .setValue(String.valueOf(finalFee));

                    SoapProto.keyValuePair.Builder kvpRefundAgent = SoapProto.keyValuePair.newBuilder()
                            .setKey(Const.REFUND_AGENT)
                            .setValue(finalRefundAgent);

                    SoapProto.keyValuePair.Builder kvpBankCode = SoapProto.keyValuePair.newBuilder()
                            .setKey("bankcode")
                            .setValue(fBankCode);


                    //builder
                    SoapProto.commonAdjust.Builder builder = SoapProto.commonAdjust.newBuilder();
                    builder.setSource(finalAdjustAccount)
                            .setTarget("0" + phoneNumber)
                            .setWalletType(1)
                            .setAmount(fAmount)
                            .setPhoneNumber("0" + phoneNumber)
                            .setTime(log.getTime())
                            .setDescription("Nap_tien_123pay")
                            .addExtraMap(kvpSrvName)
                            .addExtraMap(kvpPartnerId)
                            .addExtraMap(kvpRefundAmt)
                            .addExtraMap(kvpRefundAgent)
                            .addExtraMap(kvpBankCode);

                    //buffer adjust
                    Buffer buffer = MomoMessage.buildBuffer(
                            SoapProto.MsgType.ADJUSTMENT_VALUE
                            , System.currentTimeMillis()
                            , phoneNumber
                            , builder.build().toByteArray());

                    //day qua soap verticle

                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS,buffer,new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> msgReply) {

                            Common.SoapObjReply soapObjReply = new Common.SoapObjReply(msgReply.body());

                            String success = (soapObjReply.error == 0 ? " thành công" : " không thành công");

                            log.add("soap result code", soapObjReply.error);
                            log.add("123pay adjust", success);

                            String strCmt = "";
                            String caption ="";
                            String sms = "";

                            //do tien tu soap khong thanh cong
                           if(soapObjReply.error != 0){
                               caption = "Nạp tiền không thành công";
                               String tpl= "Quý khách đã nạp không thành công số tiền %sđ từ %s.";
                               strCmt = String.format(tpl
                                       , Misc.formatAmount(fAmount).replace(",",".")
                                       , Constant123Pay.getShortBankName(orgBankCode)
                               );


                           }else{
                               caption = "Nạp tiền thành công";
                               String tpl ="Quý khách đã nạp thành công số tiền %sđ từ %s.";

                               strCmt= String.format(tpl
                                       , Misc.formatAmount(fAmount).replace(",",".")
                                       ,Constant123Pay.getShortBankName(orgBankCode));

                               String smsTmp = "Chuc mung quy khach da nap thanh cong so tien %sd tu %s. TID: %s. Xin cam on!";
                               sms = String.format(smsTmp, Misc.formatAmount(fAmount).replace(",",".")
                                                        ,Constant123Pay.getShortBankName(orgBankCode)
                                                        ,String.valueOf(soapObjReply.tranId));

                           }

                            final String fCaption = caption;
                            final String fSms = sms;


                            //todo : caption nap tien thanh cong
                            //String tpl ="Quý khách đã nạp thành công số tiền [xxx] từ ngân hàng [xxx] qua thẻ ATM.";

                            //1. TAO 1 GIAO DICH GUI VE CHO CLIENT
                            final TranObj tran = new TranObj();
                            tran.owner_number = phoneNumber;
                            tran.tranType=MomoProto.TranHisV1.TranType.M2M_VALUE;
                            tran.tranId = soapObjReply.tranId;
                            tran.clientTime = soapObjReply.finishTime;
                            tran.ackTime = soapObjReply.finishTime;
                            tran.finishTime = soapObjReply.finishTime;//=> this must be the time we sync, or user will not sync this to device
                            tran.amount = soapObjReply.amount;
                            tran.status = soapObjReply.status;
                            tran.error = soapObjReply.error;// lay ma loi cua core tra ve cho client o day
                            tran.cmdId = soapObjReply.finishTime;
                            tran.billId = mTransactionID;
                            tran.category = 0;
                            tran.io = soapObjReply.io;
                            tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                            tran.parterCode = "123pay";

                            tran.comment= strCmt;

                            tran.partnerId=orgBankCode;
                            tran.partnerName = Constant123Pay.getShortBankName(orgBankCode);

                            int returnCode123 = 1; // default thanh cong

                            if (soapObjReply.error != 0) {
                                returnCode123 = -3;
                                log.add("Thuc hien adjust tien vao vi cho khach khong thanh cong","");
                            }

                            log.add("error", soapObjReply.error);
                            log.add("desc", SoapError.getDesc(soapObjReply.error));

                            //3. SEND BACK 123PAY
                            return123Pay(context, mTransactionID, returnCode123, log);

                            log.add("create new tran for 123pay","");
                            log.add("tran 123pay json", tran.getJSON());

                            //Khuyen mai 123pay
                            if(soapObjReply.error == 0)
                            {
                                JsonObject joExtra = new JsonObject();
                                joExtra.putNumber(StringConstUtil.AMOUNT, soapObjReply.amount);
                                joExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                                joExtra.putString(StringConstUtil.BANK_CODE, "123pay");
//                                promotionProcess.excuteAcquireBinhTanUserPromotion("0" + phoneNumber, log, null, null, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joExtra);
//                                log.writeLog();
                            }
                            transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean result) {

                                    log.add("isUpdated", result);

                                    if(!result){
                                        BroadcastHandler.sendOutSideTransSync(vertx, tran);

                                        Notification noti = new Notification();
                                        noti.receiverNumber = tran.owner_number;
                                        noti.caption = fCaption;   //"Ngày MoMo";
                                        noti.body = tran.comment ; //"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";
                                        noti.sms =  fSms;          //"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.";
                                        noti.priority = 1;
                                        noti.time = System.currentTimeMillis();
                                        noti.tranId = tran.tranId;
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

                                        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                                , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> message) {
                                            }
                                        });

                                    }

                                    log.writeLog();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void return123Pay(HttpRequestContext context
                                                ,String tranID
                                                ,int returnCode
                                                ,Common.BuildLog log){


        String ts = System.currentTimeMillis() + "";
        final JsonObject result = new JsonObject();
        result.putString("mTransactionID",tranID);
        result.putString("ts",ts);
        result.putString("returnCode", returnCode + "");

        String hash = tranID
                        + String.valueOf(returnCode)
                        + String.valueOf(ts)
                        + pay123_key;

        String hmacsha1 = DataUtil.getHMACSHA1WithoutKey(hash);
        result.putString("checksum", hmacsha1);

        log.add("response to 123pay checksum", hmacsha1);

        HttpResponseCommon.response(context.getRequest(), result);
    }
}
