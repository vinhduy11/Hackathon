package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.FeeCollection;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.tracking.TrackingBanknetDb;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

/**
 * Created by User on 3/18/14.
 */
public class BanknetProcess {

    public static  String BANK_NET_BANK_CODE_BANK_NAME_SUPPORT;
    public static  String VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET;
    public static  String VIETTINBANK_AGENT_ADJUST_FOR_BANKNET;
    public static boolean BANK_NET_TEST_MODE;

    private Vertx vertx;
    private Logger logger;
    private FeeDb feeDb;
    private Common mCom;
    private TrackingBanknetDb trackingBanknetDb;
    private PromotionProcess promotionProcess;
    public BanknetProcess(Vertx vertx, Logger logger, JsonObject glbCfg){
        this.vertx = vertx;
        this.logger =logger;
        feeDb =new FeeDb(vertx,logger);
        mCom = new Common(vertx,logger, glbCfg);
        trackingBanknetDb = new TrackingBanknetDb(vertx, logger);
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);
    }

    public void processBanknetVerifyOtp(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {

        if(BANK_NET_TEST_MODE) return;

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1. parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }

        if (request == null
                || !request.hasAmount()
                || !request.hasPartnerName()
                || !request.hasPartnerCode()
                || !request.hasPartnerId()) {

            mCom.writeErrorToSocket(sock);
            return;
        }

        final long org_amount = request.getAmount();

        //build buffer to verify OTP
        Buffer buffer = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_NET_VERIFY_OTP_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , SoapProto.BankNetVerifyOtp.newBuilder()
                .setAmount(org_amount)
                .setMerchantTransId(request.getPartnerName())
                .setTransId(request.getPartnerCode())
                .setOtp(request.getPartnerId())
                .build()
                .toByteArray()
        );


        final long ackTime = System.currentTimeMillis();
        //gui lenh xac thuc OTP den bank-net
        vertx.eventBus().send(AppConstant.BanknetVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> json) {

                //verify otp on bank-net side success
                if (json.body() != null && json.body().getBoolean("result")) {
                    String bankId = data.bank_net_bank_code;
                    int inout_city = 1; // xem nhu trong thanh pho
                    int channel = MomoProto.CardItem.Channel.BANKNET_VALUE;
                    int trantype = MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE;

                    FeeDb.Obj feeObj = FeeCollection.getInstance().findFeeBy(bankId, channel, trantype, inout_city);

                    //chon tai khoan ke qua qua vietcombank hoac viettinbank
                    String adjustAcc = VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET;
                    if (data != null && data.getPhoneObj() != null && data.getPhoneObj().bank_code.equalsIgnoreCase("102")) {
                        adjustAcc = VIETTINBANK_AGENT_ADJUST_FOR_BANKNET;
                    }

                    final long transferAmt = (long) (org_amount * (100 - feeObj.DYNAMIC_FEE) / 100) - feeObj.STATIC_FEE;

                    long bnFee = 0;
                    String freeBnAgent = "";

                    Buffer buffer = MomoMessage.buildBuffer(
                            SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , SoapProto.commonAdjust.newBuilder()
                                    .setSource(adjustAcc)
                                    .setTarget("0" + msg.cmdPhone)
                                    .setAmount(transferAmt)
                                    .setPhoneNumber("0" + msg.cmdPhone)
                                    .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                            .setKey("refundamount")
                                            .setValue(String.valueOf(bnFee)))
                                    .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                            .setKey("refundagent")
                                            .setValue(freeBnAgent))
                                    .build()
                                    .toByteArray()
                    );

                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> result) {
                            JsonObject tranReply = result.body();
                            keepTrackInfoTran(data.bank_net_bank_code, "0" + msg.cmdPhone, System.currentTimeMillis() + "", tranReply);
                            mCom.sendTransReply(vertx, tranReply, ackTime, msg, sock, data, callback);
                            final Common.BuildLog log = new Common.BuildLog(logger);
                            promotionProcess.executePromotion("", MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE, "", msg.cmdPhone,
                                    System.currentTimeMillis(), org_amount , null, "sbs", null, 0, 1, log, new JsonObject(), new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject event) {

                                        }
                                    });
                        }
                    });

                } else {
                    //verify on bank-net side failed
                    JsonObject tranReply = json.body();
                    keepTrackInfoTran(data.bank_net_bank_code, "0" + msg.cmdPhone, System.currentTimeMillis() + "", tranReply);
                    mCom.sendTransReply(vertx, tranReply, ackTime, msg, sock, data, callback);
                }
            }
        });
    }
    private void keepTrackInfoTran(String billId, String phoneNumber, String tranId, JsonObject joReply) {
        TrackingBanknetDb.Obj trackBanknetObj = new TrackingBanknetDb.Obj();
        trackBanknetObj.billId = billId;
        trackBanknetObj.phoneNumber = phoneNumber;
        trackBanknetObj.time = System.currentTimeMillis();
        trackBanknetObj.tranId = tranId;
        trackBanknetObj.joExtra = joReply;
        trackingBanknetDb.insert(trackBanknetObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {

            }
        });
    }

}
