package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.PhoneNumberUtil;
import com.mservice.momo.util.ValidationUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.TransProcess;
import com.mservice.momo.vertx.processor.transferbranch.Cashdeposit;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by ntunam on 3/20/14.
 */
public class BankAccountHandler extends CommandHandler {

    private TransProcess transFactory;
    private Cashdeposit cashdeposit;

    public BankAccountHandler(MainDb mainDb, Vertx vertx, Container container, TransProcess transFactory, JsonObject config) {
        super(mainDb, vertx, container, config);
        this.transFactory = transFactory;
        cashdeposit = new Cashdeposit(vertx, container.logger(), container.config());
    }

    public void modifyBankAccount(final CommandContext context) {
        CmdModels.ModifyBankAccount cmd = (CmdModels.ModifyBankAccount) context.getCommand().getBody();
        final CmdModels.BankAccount account = cmd.getAccount();

        //validation
        if (account != null) {
            if (!PhoneNumberUtil.isValidPhoneNumber(account.getPhoneNumber())) {
                context.replyError(-1, "Phone number is invalid.");
                return;
            }
            if (ValidationUtil.isEmpty(account.getAccountId())) {
                context.replyError(-1, "AccountId is invalid.");
                return;
            }
        }
        //doing business
        switch (cmd.getCommand()) {
            case ADD:
                mainDb.bankAccountDb.save(account.getPhoneNumber(), account.getAccountId(), account.getBankName(), account.getOwnerName(), account.getCreatedDate(), account.getBankId(), new Handler<String>() {
                    @Override
                    public void handle(String objId) {
                        CmdModels.ModifyBankAccountReply replyBody = CmdModels.ModifyBankAccountReply.newBuilder()
                                .setResult(CmdModels.ModifyBankAccountReply.ResultCode.SUCCESS)
                                .build();
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.MODIFY_BANK_ACCOUNT_REPLY, replyBody);
                        context.reply(replyCommand);
                    }
                });
                break;
            case UPDATE:
                mainDb.bankAccountDb.update(account.getPhoneNumber(), account.getAccountId(), account.getBankName(), account.getOwnerName(), account.getCreatedDate(), account.getBankId(), new Handler<String>() {
                    @Override
                    public void handle(String objId) {
                        CmdModels.ModifyBankAccountReply replyBody = CmdModels.ModifyBankAccountReply.newBuilder()
                                .setResult(CmdModels.ModifyBankAccountReply.ResultCode.SUCCESS)
                                .build();
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.MODIFY_BANK_ACCOUNT_REPLY, replyBody);
                        context.reply(replyCommand);
                    }
                });
                break;
            case DELETE:
                mainDb.bankAccountDb.delete(account.getPhoneNumber(), account.getAccountId(), new Handler<String>() {
                    @Override
                    public void handle(String objId) {
                        CmdModels.ModifyBankAccountReply replyBody = CmdModels.ModifyBankAccountReply.newBuilder()
                                .setResult(CmdModels.ModifyBankAccountReply.ResultCode.SUCCESS)
                                .build();
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.MODIFY_BANK_ACCOUNT_REPLY, replyBody);
                        context.reply(replyCommand);
                    }
                });
                break;
            default:
                throw new IllegalAccessError();
        }
    }

    public void getAgentBankAccounts(final CommandContext context) {
        CmdModels.GetAgentBankAccounts cmd = (CmdModels.GetAgentBankAccounts) context.getCommand().getBody();
        mainDb.bankAccountDb.getUserBankAccounts(cmd.getPhoneNumber(), new Handler<ArrayList<CmdModels.BankAccount>>() {
            @Override
            public void handle(ArrayList<CmdModels.BankAccount> accounts) {
                CmdModels.GetAgentBankAccountsReply.Builder builder = CmdModels.GetAgentBankAccountsReply.newBuilder();
                if (accounts != null) {
                    for (CmdModels.BankAccount account : accounts) {
                        builder.addAccounts(
                                CmdModels.BankAccount.newBuilder()
                                        .setPhoneNumber(account.getPhoneNumber())
                                        .setAccountId(account.getAccountId())
                                        .setBankName(account.getBankName())
                                        .setOwnerName(account.getOwnerName())
                                        .setCreatedDate(account.getCreatedDate())
                                        .setBankId(account.getBankId() == null ? "" : account.getBankId())
                                        .build()
                        );
                    }
                }
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_AGENT_BANK_ACCOUNTS_REPLY, builder.build());
                context.reply(replyCommand);
            }
        });
    }

    public void bankIn(final CommandContext context) {
        final CmdModels.BankIn cmd = (CmdModels.BankIn) context.getCommand().getBody();

        String bankName = "Vietcombank";
        if (cmd.getBankCode().trim().equalsIgnoreCase("102")) {
            bankName = "Viettinbank";
        }

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }
        MomoMessage msg = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, 0, cmd.getPhoneNumber(),
                MomoProto.TranHisV1.newBuilder()
                        .setTranType(MomoProto.TranHisV1.TranType.BANK_IN_VALUE)
                        .setPartnerName(bankName)
                        .setPartnerCode(cmd.getBankCode())
                        .setAmount(cmd.getAmount())
                        .build().toByteArray()
                );
        transFactory.processBankIn(null, msg, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int err = result.getInteger("error");
                if (err != 0) {
                    context.replyError(err, String.valueOf(result));
                    return;
                }

                Long tranId = result.getLong("tranId", -1);
                CmdModels.BankInReply.Builder cmdBody = CmdModels.BankInReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(CmdModels.BankInReply.Result.SUCCESS)
                        .setTransactionId(String.valueOf(tranId));
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.BANK_IN_REPLY, cmdBody.build());
                context.reply(replyCommand);
            }
        });

/*        Buffer bankin = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_IN_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.BankIn.newBuilder()
                        .setMpin(cmd.getPin())
                        .setChannel(Const.CHANNEL_WEB)
                        .setAmount(cmd.getAmount())
                        .setBankCode(cmd.getBankCode())
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(SoapVerticle.ADDRESS, bankin, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> result) {
                try {
                    MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
                    builder.setPartnerName("");
                    builder.setPartnerCode(cmd.getBankCode());
                    builder.setAmount(cmd.getAmount());
                    builder.setTranType(MomoProto.TranHisV1.TranType.BANK_IN_VALUE);

                    final MomoProto.TranHisV1 tranHis = builder.build();
                    Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REQUEST_VALUE, System.currentTimeMillis(), cmd.getPhoneNumber(), tranHis.toByteArray());

                    final MomoMessage msg = MomoMessage.fromBuffer(buffer);
                    CoreCommon.sendTransReply(result.body(), System.currentTimeMillis(), msg, null, null, null);
                } finally {
                    int err = result.body().getInteger("error");
                    if (err != 0) {
                        context.replyError(err, String.valueOf(result.body()));
                        return;
                    }

                    Long tranId = result.body().getLong("tranId", -1);
                    CmdModels.BankInReply.Builder cmdBody = CmdModels.BankInReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.BankInReply.Result.SUCCESS)
                            .setTransactionId(String.valueOf(tranId));
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.BANK_IN_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                }

            }
        });*/
    }

    public void bankOut(final CommandContext context) {
        final CmdModels.BankOut cmd = (CmdModels.BankOut) context.getCommand().getBody();

        String bankName = "Vietcombank";
        if (cmd.getBankCode().trim().equalsIgnoreCase("102")) {
            bankName = "Viettinbank";
        }

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }
        MomoMessage msg = new MomoMessage(MomoProto.TranHisV1.TranType.BANK_OUT_VALUE, 0, cmd.getPhoneNumber(),
                MomoProto.TranHisV1.newBuilder()
                        .setTranType(MomoProto.TranHisV1.TranType.BANK_OUT_VALUE)
                        .setPartnerName(bankName)
                        .setPartnerCode(cmd.getBankCode())
                        .setAmount(cmd.getAmount())
                        .build().toByteArray()
        );

        transFactory.processBankOut(null, msg, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int err = result.getInteger("error");
                if (err != 0) {
                    context.replyError(err, String.valueOf(result));
                    return;
                }

                Long tranId = result.getLong("tranId", -1);
                CmdModels.BankOutReply.Builder cmdBody = CmdModels.BankOutReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(CmdModels.BankOutReply.Result.SUCCESS)
                        .setTransactionId(String.valueOf(tranId));
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.BANK_OUT_REPLY, cmdBody.build());
                context.reply(replyCommand);
            }
        });

 /*       Buffer bankout = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_OUT_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.BankIn.newBuilder()
                        .setMpin(cmd.getPin())
                        .setChannel(Const.CHANNEL_WEB)
                        .setAmount(cmd.getAmount())
                        .setBankCode(cmd.getBankCode())
                        .build()
                        .toByteArray()
        );


        MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
        builder.setPartnerName("");
        builder.setPartnerCode(cmd.getBankCode());
        builder.setAmount(cmd.getAmount());
        builder.setTranType(MomoProto.TranHisV1.TranType.BANK_OUT_VALUE);

        final MomoProto.TranHisV1 tranHis = builder.build();
        Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REQUEST_VALUE, System.currentTimeMillis(), cmd.getPhoneNumber(), tranHis.toByteArray());

        final MomoMessage msg = MomoMessage.fromBuffer(buffer);

        vertx.eventBus().send(SoapVerticle.ADDRESS, bankout, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> result) {
                CoreCommon.sendTransReply(result.body(), System.currentTimeMillis(), msg, null, null, null);

                int err = result.body().getInteger("error");
                if (err != 0) {
                    context.replyError(err, String.valueOf(result.body()));
                    return;
                }

                Long tranId = result.body().getLong("tranId", -1);
                CmdModels.BankOutReply.Builder cmdBody = CmdModels.BankOutReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(CmdModels.BankOutReply.Result.SUCCESS)
                        .setTransactionId(String.valueOf(tranId));
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.BANK_OUT_REPLY, cmdBody.build());
                context.reply(replyCommand);
            }
        });*/
    }

    public void bankOutManual(final CommandContext context) {
        final CmdModels.BankOutManual cmd = (CmdModels.BankOutManual) context.getCommand().getBody();
        SockData sockData = context.getSockData(cmd.getPhoneNumber());

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + cmd.getPhoneNumber());
        log.add("call from web","");
        log.add("function", "bankOutManual");

        if (sockData == null) {
            context.replyError(-1, "Login required!");
            log.add("sockData", "null");
            log.writeLog();
            return;
        }

        log.add("bankId",cmd.getBankId());
        log.add("holder number",cmd.getHolderNumber());
        log.add("amount", cmd.getAmount());
        log.add("holder name",cmd.getHolderName());
        log.add("bank name",cmd.getBankName());
        log.add("comment",cmd.getComment());
        log.add("inout city",cmd.getInOutCity()); // 0 : ngoai tp : 1 trong tp
        log.add("bank branch",cmd.getBankBranch());

        /*bankId	    -->	partner_id
        holder_number	-->	partner_code
        amount	        -->	amount
        holder_name	    -->	partner_name
        bank name	    -->	partner_ref
        comment	        -->	comment
        inoutcity       --> billId -- trong/ngoai thanh pho
        bank_branch --> partner_extra_1
        */

        MomoMessage msg = new MomoMessage(MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE
                ,0
                ,cmd.getPhoneNumber(),
                MomoProto.TranHisV1.newBuilder()
                        .setTranType(MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE)
                        .setPartnerId(cmd.getBankId())
                        .setPartnerCode(cmd.getHolderNumber())
                        .setAmount(cmd.getAmount())
                        .setPartnerName(cmd.getHolderName())
                        .setPartnerRef(cmd.getBankName())
                        .setComment(cmd.getComment())
                        .setBillId(String.valueOf(cmd.getInOutCity()))
                        .setPartnerExtra1(cmd.getBankBranch())
                        .setSourceFrom(MomoProto.TranHisV1.SourceFrom.MOMO_VALUE)
                        .build().toByteArray()
        );

        cashdeposit.doBankManual(null, msg, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger(colName.TranDBCols.ERROR, -1000);

                Long tranId = result.getLong(colName.TranDBCols.TRAN_ID, -1);
                CmdModels.BankOutManualReply.Builder cmdBody = CmdModels.BankOutManualReply.newBuilder()
                                                                        .setError(error)
                                                                        .setTranId(tranId);

                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.BANK_OUT_MANUAL_REPLY
                        , cmdBody.build());
                log.add("json result", result.toString());
                log.writeLog();

                context.reply(replyCommand);
            }
        });
    }
}
