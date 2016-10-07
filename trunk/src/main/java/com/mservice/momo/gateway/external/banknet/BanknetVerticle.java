package com.mservice.momo.gateway.external.banknet;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.tracking.TrackingBanknetDb;
import com.mservice.momo.gateway.external.banknet.obj.BanknetFunction;
import com.mservice.momo.gateway.external.banknet.obj.BanknetResponse;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by User on 3/12/14.
 */
public class BanknetVerticle extends Verticle {
    //    public static String ADDRESS = "BANKNET_WEBSERVICE";
    //public static String BANKNET_WS_URL;
    public static JsonObject bank_net_partner_cfg;

    private Logger logger;
    private BanknetFunction mBanknetFunction;
    private TrackingBanknetDb trackingBanknetDb;
    public static void LoadCfg(JsonObject banknetCfg) {
        bank_net_partner_cfg = banknetCfg;
    }

    public void start() {
        logger = getContainer().logger();

        JsonObject bank_net = container.config().getObject("bank_net");
        LoadCfg(bank_net);
//        LoadCfg(container.config());

        mBanknetFunction = new BanknetFunction(vertx.eventBus(), bank_net_partner_cfg, logger);
        trackingBanknetDb = new TrackingBanknetDb(vertx, logger);
        EventBus eb = vertx.eventBus();

        Handler<Message> myHandler = new Handler<Message>() {
            public void handle(Message message) {

                MomoMessage momoMessage = MomoMessage.fromBuffer((Buffer) message.body());
                switch (momoMessage.cmdType) {
                    case SoapProto.MsgType.BANK_NET_TO_MOMO_VALUE:
                        doBankNetToMomo(message);
                        break;
                    case SoapProto.MsgType.BANK_NET_VERIFY_OTP_VALUE:
                        doBankNetVerifyOtp(message);
                        break;
                    case SoapProto.MsgType.BANK_NET_CONFIRM_VALUE:
                        doBankNetConfirm(message);
                        break;

                    default:
                        logger.error("Call Soap in with invalid message type: ".concat(String.valueOf(momoMessage.cmdType)));
                        break;
                }
            }
        };

        eb.registerLocalHandler(AppConstant.BanknetVerticle_ADDRESS, myHandler);
        //then if client have request, then we process here
    }

    public void doBankNetToMomo(final Message message) {

        final MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.BankNetToMomo bankNetToMoMo;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE;

        try {
            bankNetToMoMo = SoapProto.BankNetToMomo.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            bankNetToMoMo = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            String amount = bankNetToMoMo.getAmount() + "";
            final SoapProto.BankNetToMomo bankNetToMoMoFinal = bankNetToMoMo;
            mBanknetFunction.doBankNet(amount
                    , bankNetToMoMo.getBankId()
                    , bankNetToMoMo.getCardHolderName()
                    , bankNetToMoMo.getCardHolderNumber()
                    , bankNetToMoMo.getCardHolderMonth()
                    , bankNetToMoMo.getCardHolderYear(), log, new Handler<BanknetResponse>() {
                @Override
                public void handle(BanknetResponse banknetResponse) {
                    int result;
                    if (banknetResponse == null) {
                        result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
                    } else {
                        result = banknetResponse.reponsecode;
                    }
                    boolean isOk = false;
                    if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {
                        isOk = true;
                    }

                    JsonObject replyObj = new JsonObject();
                    replyObj.putBoolean("result", isOk);
                    replyObj.putNumber("rcode", (result == 0 ? result : 5000 + result));
                    replyObj.putString("merchant_trans_id", banknetResponse.merchant_trans_id);
                    replyObj.putString("trans_id", banknetResponse.trans_id);

                    log.add("banknet error code", result);
                    log.add("banknet to client error code", (result == 0 ? result : 5000 + result));

                    log.writeLog();

                    message.reply(replyObj);
                    JsonObject joExtra = new JsonObject();
                    joExtra.putNumber(StringConstUtil.AMOUNT, bankNetToMoMoFinal.getAmount());
                    joExtra.putObject(StringConstUtil.RESULT, replyObj);
                    joExtra.putString(StringConstUtil.BANK_CODE, bankNetToMoMoFinal.getBankId());
                    keepTrackInfoTran(banknetResponse.merchant_trans_id, "0" + momoMsg.cmdPhone, banknetResponse.trans_id, joExtra);
                }
            });
        } else {
            JsonObject replyObj = new JsonObject();
            replyObj.putBoolean("result", false);
            replyObj.putNumber("rcode", MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE);
            replyObj.putString("merchant_trans_id", "");
            replyObj.putString("trans_id", "");

            log.add("result", false);
            log.writeLog();

            message.reply(replyObj);
            JsonObject joExtra = new JsonObject();
            joExtra.putNumber(StringConstUtil.AMOUNT, 0);
            joExtra.putObject(StringConstUtil.RESULT, replyObj);
            joExtra.putString(StringConstUtil.BANK_CODE, "");
            keepTrackInfoTran("x", "0" + momoMsg.cmdPhone, System.currentTimeMillis() + "", joExtra);
        }

    }

    public void doBankNetVerifyOtp(Message message) {

        MomoMessage msg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.BankNetVerifyOtp bankNetVerifyOtp;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE;

        try {
            bankNetVerifyOtp = SoapProto.BankNetVerifyOtp.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            bankNetVerifyOtp = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        BanknetResponse response;

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            response = mBanknetFunction.VerifyOTP(bankNetVerifyOtp.getOtp()
                    , bankNetVerifyOtp.getMerchantTransId()
                    , bankNetVerifyOtp.getTransId(), log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.reponsecode;
            }
        }

        JsonObject replyObj = new JsonObject();
        boolean isOk = true;
        if (result != 0) {
            isOk = false;
        }

        replyObj.putBoolean("result", isOk);

        if (result != 0) {
            result += 5000;
        }

        replyObj.putNumber("error", result);

        log.writeLog();

        message.reply(replyObj);
        JsonObject joExtra = new JsonObject();
        joExtra.putNumber(StringConstUtil.AMOUNT, 0);
        joExtra.putObject(StringConstUtil.RESULT, replyObj);
        joExtra.putString("otp", bankNetVerifyOtp.getOtp());
        keepTrackInfoTran(bankNetVerifyOtp.getMerchantTransId(), "0" + msg.cmdPhone, bankNetVerifyOtp.getTransId(), joExtra);
    }

    public void doBankNetConfirm(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.BankNetConfirm bankNetConfirm;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE;

        try {
            bankNetConfirm = SoapProto.BankNetConfirm.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            bankNetConfirm = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        BanknetResponse response;

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            response = mBanknetFunction.ConfirmTransactionResult(momoMsg.cmdPhone
                    , bankNetConfirm.getMerchantTransId()
                    , bankNetConfirm.getTransId()
                    , bankNetConfirm.getAdjustResult());

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.reponsecode;
            }
        }

        JsonObject replyObj = new JsonObject();
        boolean isOk = false;
        if (result == 0) {
            isOk = true;
        }
        replyObj.putBoolean("result", isOk);
        replyObj.putNumber("error", (result + 5000));
        message.reply(replyObj);
        JsonObject joExtra = new JsonObject();
        joExtra.putNumber(StringConstUtil.AMOUNT, 0);
        joExtra.putObject(StringConstUtil.RESULT, replyObj);
        joExtra.putString("adjust result", bankNetConfirm.getAdjustResult() + "");
        keepTrackInfoTran(bankNetConfirm.getMerchantTransId(), "0" + momoMsg.cmdPhone, bankNetConfirm.getTransId() + "", joExtra);
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
