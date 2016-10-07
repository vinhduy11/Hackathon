package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.msg.*;
import com.mservice.momo.util.PhoneNumberUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.TransProcess;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 3/24/14.
 */
public class TopupHandler extends CommandHandler {

    private TransProcess transFactory;

    public TopupHandler(MainDb mainDb, Vertx vertx, Container container, TransProcess transFactory, JsonObject config) {
        super(mainDb, vertx, container, config);
        this.transFactory = transFactory;
    }

    public void topup(final CommandContext context) {
        final CmdModels.Topup cmd = (CmdModels.Topup) context.getCommand().getBody();

        // validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber()) || !PhoneNumberUtil.isValidPhoneNumber(cmd.getTopupPhoneNumber())) {
            context.replyError(-1, "Phone number is invalid!");
            return;
        }
        if (cmd.getAmount() <= 0) {
            context.replyError(-1, "Amount must be greater than zero!");
            return;
        }

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }
        MomoProto.TranHisV1 tranHis = MomoProto.TranHisV1.newBuilder()
                .setTranType(MomoProto.TranHisV1.TranType.TOP_UP_VALUE)
                .setPartnerCode("0" + cmd.getTopupPhoneNumber())
                .setPartnerName("0" + cmd.getTopupPhoneNumber())
                .setPartnerId("0" + cmd.getTopupPhoneNumber())
                .setAmount(cmd.getAmount())
                .build();
        MomoMessage msg = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, 0, cmd.getPhoneNumber(), tranHis.toByteArray());

        transFactory.processTopUp(null, msg, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int err = result.getInteger("error");
                if (err != 0) {
                    context.replyError(err, String.valueOf(result));
                    return;
                }

                CmdModels.TopupReply.Builder cmdBody = CmdModels.TopupReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(CmdModels.TopupReply.Result.SUCCESS)
                        .setTransactionId(String.valueOf(result.getNumber("tranId", 0)));
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TOPUP_REPLY, cmdBody.build());
                context.reply(replyCommand);
            }
        });

        // business process
/*        Buffer topupMessage = MomoMessage.buildBuffer(
                SoapProto.MsgType.TOP_UP_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.TopUp.newBuilder()
                        .setFromNumber(cmd.getPhoneNumber())
                        .setMpin(cmd.getPin())
                        .setChannel(Const.CHANNEL_WEB)
                        .setAmount(cmd.getAmount())
                        .setToNumber(cmd.getTopupPhoneNumber())
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(SoapVerticle.ADDRESS, topupMessage, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> result) {
                try {
                    MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
                    builder.setPartnerCode("0" + cmd.getTopupPhoneNumber());
                    builder.setAmount(cmd.getAmount());
                    builder.setTranType(MomoProto.TranHisV1.TranType.TOP_UP_VALUE);
                    builder.setPartnerName("0" + cmd.getTopupPhoneNumber());
                    builder.setClientTime(System.currentTimeMillis());
                    builder.setCategory(0);

                    final MomoProto.TranHisV1 tranHis = builder.build();

                    Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REQUEST_VALUE
                            , System.currentTimeMillis()
                            , cmd.getPhoneNumber()
                            , tranHis.toByteArray());

                    final MomoMessage msg = MomoMessage.fromBuffer(buffer);

                    CoreCommon.sendTransReply(result.body(), System.currentTimeMillis(), msg, null, null, null);
                } finally {
                    int err = result.body().getInteger("error");
                    if (err != 0) {
                        context.replyError(err, String.valueOf(result.body()));
                        return;
                    }

                    CmdModels.TopupReply.Builder cmdBody = CmdModels.TopupReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.TopupReply.Result.SUCCESS)
                            .setTransactionId(result.body().getNumber("tranId", 0).toString());
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TOPUP_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                }
            }
        });*/
    }


    public void topupGame(final CommandContext context) {
        final CmdModels.TopupGame cmd = (CmdModels.TopupGame) context.getCommand().getBody();

        // validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber())) {
            context.replyError(-1, "Phone number is invalid!");
            return;
        }
        if (cmd.getAmount() < 0) {
            context.replyError(-1, "Amount must be greater than zero!");
            return;
        }

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required");
            return;
        }
        MomoMessage msg = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, 0, cmd.getPhoneNumber(),
                MomoProto.TranHisV1.newBuilder()
                        .setTranType(MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE)
                        .setPartnerId(cmd.getProviderId())
                        .setPartnerCode(cmd.getGameAccount())
                        .setAmount(cmd.getAmount())
                        .build().toByteArray()
                );
        transFactory.processTopUpGame(null, msg, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {

                    int err = result.getInteger("error");
                    if (err != 0) {
                        context.replyError(err, String.valueOf(result));
                        return;
                    }

                    CmdModels.TopupGameReply.Builder cmdBody = CmdModels.TopupGameReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.TopupGameReply.TopupGameResult.SUCCESS)
                            .setTransactionId(result.getNumber("tranId", 0).toString());
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TOPUP_GAME_REPLY, cmdBody.build());
                    context.reply(replyCommand);
            }
        });

//        Buffer topUpGame = MomoMessage.buildBuffer(
//                SoapProto.MsgType.TOPUP_GAME_VALUE,
//                0,
//                cmd.getPhoneNumber(),
//                SoapProto.TopUpGame.newBuilder()
//                        .setMpin(cmd.getPin())
//                        .setProviderId(cmd.getProviderId())
//                        .setChannel(Const.CHANNEL_WEB)
//                        .setAmount(cmd.getAmount())
//                        .setGameAccount(cmd.getGameAccount())
//                        .build()
//                        .toByteArray()
//        );
//
//        vertx.eventBus().send(SoapVerticle.ADDRESS, topUpGame, new Handler<CoreMessage<JsonObject>>() {
//            @Override
//            public void handle(CoreMessage<JsonObject> result) {
//                try {
//                    MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
//                    builder.setAmount(cmd.getAmount());
//                    builder.setTranType(MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE);
//                    builder.setPartnerId(cmd.getProviderId());
//                    builder.setPartnerCode(cmd.getGameAccount());
//                    builder.setPartnerName(cmd.getProviderId());
//                    builder.setClientTime(System.currentTimeMillis());
//                    builder.setCategory(0);
//
//                    final MomoProto.TranHisV1 tranHis = builder.build();
//
//                    Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REQUEST_VALUE
//                            , System.currentTimeMillis()
//                            , cmd.getPhoneNumber()
//                            , tranHis.toByteArray());
//
//                    final MomoMessage msg = MomoMessage.fromBuffer(buffer);
//
//                    CoreCommon.sendTransReply(result.body(), System.currentTimeMillis(), msg, null, null, null);
//                } finally {
//                    int err = result.body().getInteger("error");
//                    if (err != 0) {
//                        context.replyError(err, String.valueOf(result.body()));
//                        return;
//                    }
//
//                    CmdModels.TopupGameReply.Builder cmdBody = CmdModels.TopupGameReply.newBuilder()
//                            .setPhoneNumber(cmd.getPhoneNumber())
//                            .setResult(CmdModels.TopupGameReply.TopupGameResult.SUCCESS)
//                            .setTransactionId(result.body().getNumber("tranId", 0).toString());
//                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TOPUP_GAME_REPLY, cmdBody.build());
//                    context.reply(replyCommand);
//                }
//            }
//        });
    }
}
