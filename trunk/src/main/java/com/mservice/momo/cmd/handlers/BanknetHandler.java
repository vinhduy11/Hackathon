package com.mservice.momo.cmd.handlers;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BanknetProcess;
import com.mservice.momo.vertx.processor.InfoProcess;
import com.mservice.momo.vertx.processor.TransferCommon;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 3/26/14.
 */
public class BanknetHandler extends CommandHandler {
    private double BANK_NET_DINAMIC_FEE;
    private int BANK_NET_STATIC_FEE;
    private String MOMO_PHONE;

    private BanknetProcess banknetProcess;

    private InfoProcess infoFactory;
    private TransferCommon transferCommon;

    public BanknetHandler(MainDb mainDb, Vertx vertx, Container container, double banknetDinamicFee, int banknetStaticFee, String momoPhone, BanknetProcess banknetProcess, JsonObject config, InfoProcess infoFactory) {
        super(mainDb, vertx, container, config);
        this.BANK_NET_DINAMIC_FEE = banknetDinamicFee;
        this.BANK_NET_STATIC_FEE = banknetStaticFee;
        this.MOMO_PHONE = momoPhone;
        this.banknetProcess = banknetProcess;
        this.infoFactory =infoFactory;
        this.transferCommon = new TransferCommon(vertx
                                                ,logger
                                                ,config);
    }

    //todo : nam check more
    public void banknetToMomo(final CommandContext context) {

        final CmdModels.BanknetToMomo cmd = (CmdModels.BanknetToMomo)context.getCommand().getBody();

        SockData data = context.getSockData(cmd.getPhoneNumber());
        if (data == null) {
            context.replyError(-1, "Login required!");
            return;
        }


        String cardHolderMonth = String.valueOf(cmd.getCardHolderMonth());
        if (cardHolderMonth != null && cardHolderMonth.length()==1)
            cardHolderMonth = "0" + cardHolderMonth;

        String cardHolderYear = String.valueOf(cmd.getCardHolderYear());
        if (cardHolderYear != null && cardHolderYear.length() == 1)
            cardHolderYear = "0" + cardHolderYear;

        MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE,
                0,
                cmd.getPhoneNumber(),
                MomoProto.CardItem.newBuilder()
                        .setCardHolderName(cmd.getCardHolderName())
                        .setCardHolderNumber(cmd.getCardHolderNumber())
                        .setCardHolderMonth(cardHolderMonth)
                        .setCardHolderYear(cardHolderYear)
                        .setBankId(cmd.getBankId())
                        .setAmount(cmd.getAmount())
                        .build().toByteArray()
        );

        infoFactory.processBanknetCheckInfo(null, momoMessage, data, new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    MomoProto.BankNetToMomoRely result =MomoProto.BankNetToMomoRely.parseFrom(MomoMessage.fromBuffer(buffer).cmdBody);

                    Object replyBody;
                    if (result.getRcode() == 0) {
                        //build and send result back to client
                        replyBody = CmdModels.BanknetToMomoReply.newBuilder()
                                .setPhoneNumber(cmd.getPhoneNumber())
                                .setResult(CmdModels.BanknetToMomoReply.Result.SUCCESS)
                                .setMerchantTransactionId(result.getMerchantTransId())
                                .setTransactionId(result.getTransId())
                                .setBanketError(result.getRcode())
                                .build();
                    } else {
                        replyBody = CmdModels.BanknetToMomoReply.newBuilder()
                                .setPhoneNumber(cmd.getPhoneNumber())
                                .setResult(CmdModels.BanknetToMomoReply.Result.FAILED)
                                .setMerchantTransactionId(result.getMerchantTransId())
                                .setTransactionId(result.getTransId())
                                .setBanketError(result.getRcode())
                                .build();
                    }

                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.BANK_NET_TO_MOMO_REPLY, replyBody);
                    context.reply(replyCommand);

                } catch (InvalidProtocolBufferException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });


//        banknetProcess.processBanknetToMoMo(null,momoMessage,data,new Handler<Buffer>() {
//            @Override
//            public void handle(Buffer buffer) {
//                MomoMessage mmMsg = MomoMessage.fromBuffer(buffer);
//                MomoProto.BankNetToMomoRely reply;
//                try {
//                    reply = MomoProto.BankNetToMomoRely.parseFrom(mmMsg.cmdBody);
//                }catch (InvalidProtocolBufferException ex){
//                    reply = null;
//                }
//
//                //todo : rely.getRcode(); thong bao loi cu the cho nguoi dung neu can
//
//                //parse result
//                CmdModels.BanknetToMomoReply.Result result;
//                if(reply == null){
//                    result = CmdModels.BanknetToMomoReply.Result.FAILED;
//                }else{
//                    result = reply.getResult() == true
//                            ? CmdModels.BanknetToMomoReply.Result.SUCCESS
//                            : CmdModels.BanknetToMomoReply.Result.FAILED;
//
//                }
//
//                //build and send result back to client
//                Object replyBody = CmdModels.BanknetToMomoReply.newBuilder()
//                        .setPhoneNumber(cmd.getPhoneNumber())
//                        .setResult(result)
//                        .setMerchantTransactionId(reply.getMerchantTransId())
//                        .setTransactionId(reply.getTransId())
//                        .setBanketError(reply.getRcode())
//                        .build();
//
//                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.BANK_NET_TO_MOMO_REPLY, replyBody);
//                context.reply(replyCommand);
//            }
//        });
    }

    //todo : nam check more
    public void verifyBankNetOtp(final CommandContext context) {
        final CmdModels.VerifyBanknetOtp cmd = (CmdModels.VerifyBanknetOtp) context.getCommand().getBody();

        /*MomoProto.TranHisV1 request;
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

            CoreCommon.writeDataToSocket(sock, logger);
            return;
        }*/

/*
        merchant_trans_id	-->	partner_name
        trans_id	-->	partner_code
        amount	-->	amount
        otp	-->	partner_id
        bankId	-->	billId
*/

        SockData data = context.getSockData(cmd.getPhoneNumber());
        if (data == null) {
            context.replyError(-1, "Login required!");
            return;
        }

        MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE
                ,0
                ,cmd.getPhoneNumber()
                ,MomoProto.TranHisV1. newBuilder()
                        .setClientTime(System.currentTimeMillis())
                        .setIo(1)
                        .setTranType(MomoProto.TranHisV1. TranType.BANK_NET_VERIFY_OTP_VALUE)
                        .setPartnerName(cmd.getMerchantTransactionId())
                        .setPartnerCode(cmd.getTransactionId())
                        .setAmount(cmd.getAmount())
                        .setPartnerId(cmd.getOtp())
                        .build().toByteArray());
        data.bank_net_bank_code = cmd.getBankId();

        TransferCommon.BankNetObj bankNetObj = new TransferCommon.BankNetObj();
        bankNetObj.amount = cmd.getAmount();
        bankNetObj.bankId = cmd.getBankId();
        bankNetObj.otp = cmd.getOtp();
        bankNetObj.trans_id = cmd.getTransactionId();;
        bankNetObj.merchant_trans_id = cmd.getMerchantTransactionId();

        transferCommon.doVerifyBanknet(null, momoMessage, data, bankNetObj, false, new Handler<FromSource.Obj>() {
            @Override
            public void handle(FromSource.Obj fsObj) {

            }
        }, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject tranRpl) {

                int error = tranRpl.getInteger(colName.TranDBCols.ERROR, TranObj.STATUS_FAIL);

                CmdModels.VerifyBanknetOtpReply.Builder cmdBody = CmdModels.VerifyBanknetOtpReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(error);
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_BANKNET_OTP_REPLY, cmdBody.build());
                context.reply(replyCommand);
            }
        });

//        banknetProcess.processBanknetVerifyOtp(null, momoMessage,data,new Handler<JsonObject>() {
//            @Override
//            public void handle(JsonObject tranRpl) {
//
//                int error = tranRpl.getInteger(colName.TranDBCols.ERROR, TranObj.STATUS_FAIL);
//
//                CmdModels.VerifyBanknetOtpReply.Result result = (error == 0
//                                        ? CmdModels.VerifyBanknetOtpReply.Result.SUCCESS
//                                        : CmdModels.VerifyBanknetOtpReply.Result.FAILED);
//
//                CmdModels.VerifyBanknetOtpReply.Builder cmdBody = CmdModels.VerifyBanknetOtpReply.newBuilder()
//                        .setPhoneNumber(cmd.getPhoneNumber())
//                        .setResult(result);
//                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_BANKNET_OTP_REPLY, cmdBody.build());
//                context.reply(replyCommand);
//            }
//        });
    }
}
