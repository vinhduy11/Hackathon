package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.*;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.util.PhoneNumberUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.TransProcess;
import com.mservice.momo.vertx.processor.transferbranch.Cashdeposit;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 3/24/14.
 */
public class TransferHandler extends CommandHandler {
    private TransProcess transFactory;
    private Common mCom;
    Cashdeposit cashdeposit;

    public TransferHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject globalConfig, JsonObject config, TransProcess transFactory) {
        super(mainDb, vertx, container, config);
        this.transFactory = transFactory;
        cashdeposit = new Cashdeposit(vertx, logger, globalConfig);
        mCom = new Common(vertx,logger, globalConfig);
    }

    public void m2m(final CommandContext context) {
        final CmdModels.TransferM2m cmd = (CmdModels.TransferM2m) context.getCommand().getBody();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber()) || !PhoneNumberUtil.isValidPhoneNumber(cmd.getReceiverPhoneNumber())) {
            context.replyError(-1, "Phone number is invalid!");
            return;
        }
        if (cmd.getAmount() < 0) {
            context.replyError(-1, "Amount must be greater than zero!");
            return;
        }


        final SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }

        mainDb.mPhonesDb.getPhoneObjInfo(cmd.getReceiverPhoneNumber(), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                String partnerName = "";
                if (obj != null) {
                    partnerName = obj.name;
                }
                MomoProto.TranHisV1 tranHis = MomoProto.TranHisV1.newBuilder()
                        .setTranType(MomoProto.TranHisV1.TranType.M2M_VALUE)
                        .setPartnerId("0" + cmd.getReceiverPhoneNumber())
                        .setAmount(cmd.getAmount())
                        .setComment(cmd.getComment())
                        .setPartnerName(partnerName)
                        .build();

                final MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.TRANSFER_REQUEST_VALUE, 0, cmd.getPhoneNumber(), tranHis.toByteArray());


                transFactory.processM2MTransfer(null, momoMessage, sockData, null, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        Integer error = result.getInteger("error", -1);
                        Long tranId = result.getLong("tranId", 0L);
                        if (error == 0) {
                            CmdModels.TransferM2mReply.Builder cmdBody = CmdModels.TransferM2mReply.newBuilder()
                                    .setPhoneNumber(cmd.getPhoneNumber())
                                    .setResult(CmdModels.TransferM2mReply.Result.SUCCESS)
                                    .setTransactionId(String.valueOf(tranId));
                            MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2M_REPLY, cmdBody.build());
                            context.reply(replyCommand);
                            return;
                        }
                        if (error == SoapError.ACCESS_DENIED) {
                            CmdModels.TransferM2mReply.Builder cmdBody = CmdModels.TransferM2mReply.newBuilder()
                                    .setPhoneNumber(cmd.getPhoneNumber())
                                    .setResult(CmdModels.TransferM2mReply.Result.ACCESS_DENIED);
                            MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2M_REPLY, cmdBody.build());
                            context.reply(replyCommand);
                            return;
                        }
                        if (error != 0) {
                            context.replyError(error, String.valueOf(result));
                            return;
                        }
                    }
                });
            }
        });

/*        mainDb.mPhonesDb.getPhoneObjInfo(cmd.getPhoneNumber(), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj)
                //tren mongo Db khong co thong tin nay
                if (obj == null) {
                    logger.debug(cmd.getPhoneNumber() + " Can't get agent info from mongodb. Pull it out from core.");
                    mainDb.mPhonesDb.forceSyncWithCore(cmd.getPhoneNumber(), new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(PhonesDb.Obj obj) {
                            if(obj == null){
                                logger.debug(cmd.getPhoneNumber() + "Can't get agent info from core.");
                                return;
                            }
                            obj.pin = cmd.getPin();
                            doM2M(obj, momoMessage, cmd, context);
                        }
                    });

                } else {
                    obj.pin = cmd.getPin();
                    doM2M(obj, momoMessage, cmd, context);
                }
            }
        });*/

/*

        //build buffer --> soap verticle
        final Buffer m2mTransfer = MomoMessage.buildBuffer(
                SoapProto.MsgType.M2M_TRANSFER_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.M2MTransfer.newBuilder()
                        .setAgent("0" + cmd.getPhoneNumber())
                        .setMpin(cmd.getPin())
                        .setPhone("0" + cmd.getReceiverPhoneNumber())
                        .setChannel(Const.CHANNEL_WEB)
                        .setAmount(cmd.getAmount())
                        .setNotice(cmd.getComment())
                        .build()
                        .toByteArray()
        );

        final int rcvPhone = cmd.getReceiverPhoneNumber();

        vertx.eventBus().send(SoapVerticle.ADDRESS, m2mTransfer, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> result) {
                try {
                    MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
                    builder.setPartnerId("0" + cmd.getReceiverPhoneNumber());
                    builder.setPartnerName("0" + cmd.getReceiverPhoneNumber()); //todo xem lai cho nay
                    builder.setAmount(cmd.getAmount());
                    builder.setComment(cmd.getComment());
                    builder.setTranType(MomoProto.TranHisV1.TranType.M2M_VALUE);
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

                    if (err == SoapError.ACCESS_DENIED) {
                        CmdModels.TransferM2mReply.Builder cmdBody = CmdModels.TransferM2mReply.newBuilder()
                                .setPhoneNumber(cmd.getPhoneNumber())
                                .setResult(CmdModels.TransferM2mReply.Result.ACCESS_DENIED);
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2M_REPLY, cmdBody.build());
                        context.reply(replyCommand);
                        return;
                    }
                    if (err != 0) {
                        context.replyError(err, String.valueOf(result.body()));
                        return;
                    }

                    Long tranId = result.body().getLong("tranId", -1);
                    CmdModels.TransferM2mReply.Builder cmdBody = CmdModels.TransferM2mReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.TransferM2mReply.Result.SUCCESS)
                            .setTransactionId(String.valueOf(tranId));
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2M_REPLY, cmdBody.build());
                    context.reply(replyCommand);

                    /* notify receiver
                    BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
                    helper.setType(MomoProto.Broadcast.MsgType.UPDATE_INFO_VALUE);
                    helper.setSenderNumber(cmd.getPhoneNumber());
                    helper.setReceivers("0" + rcvPhone);
                    helper.setAmount(result.body().getLong(colName.TranDBCols.AMOUNT));

                    logger.info("SEND REQUEST TO BROADCAST HANDLER sender,reciever " + cmd.getPhoneNumber() + "," + rcvPhone);
                    vertx.eventBus().send(CoreCommon.getNumberBus(rcvPhone), helper.getJsonObject());
                }
            }
        });*/




    }

/*    private void doM2M(PhonesDb.Obj obj, MomoMessage momoMessage, final CmdModels.TransferM2m cmd, final CommandContext context) {
        SockData sockData = new SockData();
        sockData.setPhoneObj(obj, logger, "");

        transFactory.processM2MTransfer(null, momoMessage, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                Integer error = result.getInteger("error", -1);
                Long tranId = result.getLong("tranId", 0L);
                if (error == 0) {
                    CmdModels.TransferM2mReply.Builder cmdBody = CmdModels.TransferM2mReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.TransferM2mReply.Result.SUCCESS)
                            .setTransactionId(String.valueOf(tranId));
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2M_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                    return;
                }
                if (error == SoapError.ACCESS_DENIED) {
                    CmdModels.TransferM2mReply.Builder cmdBody = CmdModels.TransferM2mReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.TransferM2mReply.Result.ACCESS_DENIED);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2M_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                    return;
                }
                if (error != 0) {
                    context.replyError(error, String.valueOf(result));
                    return;
                }
            }
        });
    }*/

    public void m2c(final CommandContext context) {
        final CmdModels.TransferM2c cmd = (CmdModels.TransferM2c) context.getCommand().getBody();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber()) || !PhoneNumberUtil.isValidPhoneNumber(cmd.getReceiverPhoneNumber())) {
            context.replyError(-1, "Phone number is invalid!");
            return;
        }
        if (cmd.getAmount() < 0) {
            context.replyError(-1, "Amount must be greater than zero!");
            return;
        }

        Buffer m2cTransfer = MomoMessage.buildBuffer(
                SoapProto.MsgType.M2C_TRANSFER_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.M2CTransfer.newBuilder()
                        .setAgent("0" + String.valueOf(cmd.getPhoneNumber()))
                        .setMpin(cmd.getPin())
                        .setPhone("0" + cmd.getReceiverPhoneNumber())
                        .setName(cmd.getReceiverName())
                        .setCardId(cmd.getReceiverCardId())
                        .setChannel(Const.CHANNEL_WEB)
                        .setAmount(cmd.getAmount())
                        .setNotice(cmd.getComment())
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, m2cTransfer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                try {
                    //building tran his
                    MomoProto.TranHisV1. Builder builder = MomoProto.TranHisV1. newBuilder();
                    builder.setPartnerId("0" + cmd.getReceiverPhoneNumber());
                    builder.setPartnerName(cmd.getReceiverName());
                    builder.setPartnerCode(cmd.getReceiverCardId());
                    builder.setAmount(cmd.getAmount());
                    builder.setComment(cmd.getComment());
                    builder.setTranType(MomoProto.TranHisV1. TranType.M2C_VALUE);

                    final MomoProto.TranHisV1 tranHis = builder.build();

                    Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REQUEST_VALUE, System.currentTimeMillis(), cmd.getPhoneNumber(), tranHis.toByteArray());

                    final MomoMessage msg = MomoMessage.fromBuffer(buffer);
                    mCom.sendTransReply(vertx, result.body(), System.currentTimeMillis(), msg, null, null, null);

                } finally {
                    int err = result.body().getInteger("error");

                    if (err == SoapError.ACCESS_DENIED) {
                        CmdModels.TransferM2cReply.Builder cmdBody = CmdModels.TransferM2cReply.newBuilder()
                                .setPhoneNumber(cmd.getPhoneNumber())
                                .setResult(CmdModels.TransferM2cReply.Result.ACCESS_DENIED);
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2C_REPLY, cmdBody.build());
                        context.reply(replyCommand);
                        return;
                    }
                    if (err != 0) {
                        context.replyError(err, String.valueOf(result.body()));
                        return;
                    }
                    CmdModels.TransferM2cReply.Builder cmdBody = CmdModels.TransferM2cReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.TransferM2cReply.Result.SUCCESS);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.TRANSFER_M2C_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                }
            }
        });
    }

    public void depositWithdrawAtPlace(final CommandContext context) {
        final CmdModels.DepositWithdrawAtPlace cmd = (CmdModels.DepositWithdrawAtPlace) context.getCommand().getBody();

        /*address	-->	partner_ref
        maTinh	-->	partnerId
        maHuyen	-->	partnerCode
        */

        MomoProto.TranHisV1.Builder requestBuilder = MomoProto.TranHisV1.newBuilder()
                .setAmount(cmd.getAmount())
                .setSourceFrom(MomoProto.TranHisV1.SourceFrom.MOMO_VALUE)
                .setPartnerRef(cmd.getAddress())
                .setPartnerId(cmd.getMaTinh())
                .setPartnerCode(cmd.getMaHuyen());


        requestBuilder.setTranType(MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE);
        if (cmd.getInOut() < 0) {
            requestBuilder.setTranType(MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE);
        }

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }
        MomoMessage message = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE
                ,0
                ,cmd.getPhoneNumber()
                ,requestBuilder.build().toByteArray());


        cashdeposit.doSaveDepositOrWithdraw(null, message, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                CmdModels.DepositWithdrawAtPlaceReply.Builder cmdBody;
                if (event.getInteger(colName.TranDBCols.ERROR) > 0) {
                    cmdBody = CmdModels.DepositWithdrawAtPlaceReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.DepositWithdrawAtPlaceReply.Result.FAIL);
                } else {
                    cmdBody = CmdModels.DepositWithdrawAtPlaceReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.DepositWithdrawAtPlaceReply.Result.SUCCESS);
                }
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.DEPOSIT_WITHDRAW_AT_PLACE_REPLY, cmdBody.build());

                context.reply(replyCommand);
            }
        });
//        cashdeposit.doCashDeposit(null, message, sockData, new Handler<JsonObject>() {
//            @Override
//            public void handle(JsonObject event) {
//            }
//        });

    }

    public void withdrawByAgent(final CommandContext context) {
        final CmdModels.WithdrawByAgent cmd = (CmdModels.WithdrawByAgent) context.getCommand().getBody();

        MomoProto.TranHisV1.Builder requestBuilder = MomoProto.TranHisV1.newBuilder()
                .setAmount(cmd.getAmount())
                .setTranType(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE)
                .setPartnerId("0" + cmd.getAgentPhone())
                .setAmount(cmd.getAmount())
                .setComment(cmd.getComment());



        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }
        MomoMessage message = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, 0, cmd.getPhoneNumber(), requestBuilder.build().toByteArray());

        transFactory.processTransferMoneyToPlace(null ,message, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                Integer error = result.getInteger("error", -1);
                Long tranId = result.getLong("tranId", 0L);
                if (error == 0) {
                    CmdModels.WithdrawByAgentReply.Builder cmdBody = CmdModels.WithdrawByAgentReply.newBuilder()
                            .setResult(CmdModels.WithdrawByAgentReply.Result.SUCCESS)
                            .setTransactionId(String.valueOf(tranId));
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.WITHDRAW_BY_AGENT_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                    return;
                }
                if (error != 0) {
                    CmdModels.WithdrawByAgentReply.Builder cmdBody = CmdModels.WithdrawByAgentReply.newBuilder()
                            .setResult(CmdModels.WithdrawByAgentReply.Result.FAIL)
                            .setTransactionId(String.valueOf(tranId));
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.WITHDRAW_BY_AGENT_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                    return;
                }
            }
        });
    }
}
