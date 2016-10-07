package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.*;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.util.PhoneNumberUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 5/5/14.
 */
public class Mua123Handler extends CommandHandler {
    private Common mCom;
    public Mua123Handler(MainDb mainDb, Vertx vertx, Container container, JsonObject config) {
        super(mainDb, vertx, container, config);
        this.mCom = new Common(vertx,logger, container.config());
    }

    public void payOrder(final CommandContext context) {
        final CmdModels.Pay123MuaOrder cmd = (CmdModels.Pay123MuaOrder) context.getCommand().getBody();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber()) || !PhoneNumberUtil.isValidPhoneNumber(cmd.getReceiverPhoneNumber())) {
            context.replyError(-1, "Phone number is invalid!");
            return;
        }
        if (cmd.getAmount() < 0) {
            context.replyError(-1, "Amount must be greater than zero!");
            return;
        }



        //doing business
        Buffer signInBuf = MomoMessage.buildBuffer(
                SoapProto.MsgType.LOG_IN_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.LogIn.newBuilder()
                        .setNumber(cmd.getPhoneNumber())
                        .setMpin(cmd.getPin())
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, signInBuf, new Handler<Message<Integer>>() {
            @Override
            public void handle(Message<Integer> result) {
                CmdModels.IsPinCorrectReply.ResultCode resultCode;
                CmdModels.IsPinCorrectReply replyBody;
                MomoCommand replyCommand;

                if (result.body() != 0) {
                    context.replyError(result.body(), "");
                    return;
                }


                Buffer mua123Adjustment = MomoMessage.buildBuffer(
                        SoapProto.MsgType.MUA_123_ADJUSTMENT_VALUE,
                        0,
                        cmd.getPhoneNumber(),
                        SoapProto.commonAdjust.newBuilder()
                                .setSource("0" + cmd.getPhoneNumber())
                                .setTarget("0" + cmd.getReceiverPhoneNumber())
                                .setAmount(cmd.getAmount())
                                .setPhoneNumber("0" + cmd.getPhoneNumber())
                                .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                        .setKey("service_name")
                                        .setValue("123mua"))
                                .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                        .setKey("bill_id")
                                        .setValue(cmd.getOrderId()))
                                .build()
                                .toByteArray()
                );

                final int rcvPhone = cmd.getReceiverPhoneNumber();

                vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, mua123Adjustment, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        try {
                            MomoProto.TranHisV1. Builder builder = MomoProto.TranHisV1. newBuilder();
                            builder.setPartnerId("0" + cmd.getReceiverPhoneNumber());

                            builder.setAmount(cmd.getAmount());
                            builder.setComment(cmd.getComment());
                            builder.setTranType(MomoProto.TranHisV1. TranType.M2M_VALUE);
                            builder.setClientTime(System.currentTimeMillis());
                            builder.setCategory(0);

                            final MomoProto.TranHisV1 tranHis = builder.build();

                            Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REQUEST_VALUE
                                    , System.currentTimeMillis()
                                    , cmd.getPhoneNumber()
                                    , tranHis.toByteArray());

                            final MomoMessage msg = MomoMessage.fromBuffer(buffer);

                            mCom.sendTransReply(vertx,result.body(), System.currentTimeMillis(), msg, null, null, null);
                        } finally {
                            int err = result.body().getInteger("error");

                            if (err == SoapError.ACCESS_DENIED) {
                                CmdModels.Pay123MuaOrderReply.Builder cmdBody = CmdModels.Pay123MuaOrderReply.newBuilder()
                                        .setPhoneNumber(cmd.getPhoneNumber())
                                        .setResult(CmdModels.Pay123MuaOrderReply.Result.ACCESS_DENIED);
                                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.PAY_123MUA_ORDER_REPLY, cmdBody.build());
                                context.reply(replyCommand);
                                return;
                            }
                            if (err != 0) {
                                context.replyError(err, String.valueOf(result.body()));
                                return;
                            }

                            Long tranId = result.body().getLong("tranId", -1);
                            CmdModels.Pay123MuaOrderReply.Builder cmdBody = CmdModels.Pay123MuaOrderReply.newBuilder()
                                    .setPhoneNumber(cmd.getPhoneNumber())
                                    .setResult(CmdModels.Pay123MuaOrderReply.Result.SUCCESS)
                                    .setTransactionId(String.valueOf(tranId));
                            MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.PAY_123MUA_ORDER_REPLY, cmdBody.build());
                            context.reply(replyCommand);

                            // notify receiver
                            BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
                            helper.setType(SoapProto.Broadcast.MsgType.MONEY_RECV_VALUE);
                            helper.setSenderNumber(cmd.getPhoneNumber());
                            helper.setReceivers("0" + rcvPhone);
                            helper.setRequest(
                                    String.valueOf(result.body().getLong(colName.TranDBCols.AMOUNT)).getBytes()
                            );
                            logger.info("SEND REQUEST TO BROADCAST HANDLER sender,reciever " + cmd.getPhoneNumber() + "," + rcvPhone);
                            vertx.eventBus().send(Misc.getNumberBus(rcvPhone), helper.getJsonObject());
                        }
                    }
                });

            }
        });
    }
}
