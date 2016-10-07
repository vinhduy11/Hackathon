package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.BillInfoService;
import com.mservice.momo.data.BillsDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.*;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.InfoProcess;
import com.mservice.momo.vertx.processor.transferbranch.PayAirLineTicket;
import com.mservice.momo.vertx.processor.transferbranch.PayOneBill;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by ntunam on 3/25/14.
 */
public class BillHandler extends CommandHandler {
    private InfoProcess infoFactory;
    private PayOneBill payBillFactory;
    private PayAirLineTicket airLineTicketFactory;

    public BillHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject config, InfoProcess infoFactory, PayAirLineTicket airLineTicketFactory, PayOneBill payBillFactory) {
        super(mainDb, vertx, container, config);
        this.infoFactory = infoFactory;
        this.payBillFactory = payBillFactory;
        this.airLineTicketFactory = airLineTicketFactory;
    }

    /**
     * result json fields:
     * {
     * agentName : "",
     * address : "",
     * phoneNumber : "",
     * bills : [
     * {billId : "",
     * amount : "",
     * startDate : "",
     * endDate : "",
     * }, ..
     * ]
     * <p/>
     * }
     *
     * @param info
     * @return
     */
    private static JsonObject billInfoToJsonObject(String info) {
        JsonObject obj = new JsonObject();
        JsonArray bills = new JsonArray();
        int i, j;

        String part[] = info.split("~");

        i = part[0].indexOf(",");
        String agentName = part[0].substring(0, i);

        j = part[0].indexOf(",", i + 1);
        String address = part[0].substring(i + 1, j);
        i = j;

        String phoneNumber = part[0].substring(i + 1, part[0].length());

        if (part.length > 1) {

            String billParts[] = part[1].split("#");
            for (String bill : billParts) {

                i = bill.indexOf(",");
                String billId = bill.substring(0, i);

                j = bill.indexOf(",", i + 1);
                long amount = 0;
                try {
                    amount = Long.parseLong(bill.substring(i + 1, j));
                } catch (NumberFormatException e) {
                }
                i = j;

                j = bill.indexOf(",", i + 1);
                long startDate = 0;
                try {
                    startDate = new SimpleDateFormat("yyyy-MM-dd").parse(bill.substring(i + 1, j)).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                i = j;

                long endDay = 0;
                try {
                    endDay = new SimpleDateFormat("yyyy-MM-dd").parse(bill.substring(i + 1, bill.length())).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

//            System.err.println("billId " + billId);
//            System.err.println("amount " + amount);
//            System.err.println("startDate " + startDate);
                JsonObject b = new JsonObject();
                bills.add(new JsonObject()
                        .putString("billId", billId)
                        .putNumber("amount", amount)
                        .putNumber("startDate", startDate)
                        .putNumber("endDate", endDay));
            }
        }

        obj.putString("agentName", agentName);
        obj.putString("address", address);
        obj.putString("phoneNumber", phoneNumber);
        obj.putArray("bills", bills);
//        System.out.println(agentName);
//        System.out.println(address);
//        System.out.println(phoneNumber);
        return obj;
    }

    public static void main(String args[]) {
        String x = "NGUYEN MINH KHUE,TO 42 KP2 Phuong Tan Chanh Hiep Quan 12,-1~~";
        System.out.println(billInfoToJsonObject(x));
    }

    public void getBillInfo(final CommandContext context) {
        final CmdModels.GetBillInfo cmd = (CmdModels.GetBillInfo) context.getCommand().getBody();
/*
        MomoProto.GetBillInfo body = MomoProto.GetBillInfo.newBuilder()
                .setBillId(cmd.getBillId())
                .setProviderId(cmd.getProviderId())
                .build();

        MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.GET_BILL_INFO_VALUE, 0, cmd.getPhoneNumber(), body.toByteArray());

        SockData data = new SockData();
        data.pin = cmd.getPin();

        infoFactory.processGetBillInfo(null, momoMessage, data, new Handler<String>() {
            @Override
            public void handle(String billInfo) {
                CmdModels.GetBillInfoReply body = null;
                if (billInfo == null) {
                    body = CmdModels.GetBillInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.GetBillInfoReply.Result.BILL_NOT_FOUND)
                            .build();
                    context.reply(new MomoCommand(CmdModels.CommandType.GET_BILL_INFO_REPLY, body));
                    return;
                }
                JsonObject obj = billInfoToJsonObject(billInfo);
                JsonArray bills = obj.getArray("bills");

                final String agentNameOnBill = obj.getString("agentName", "");
                final String agentAddressOnBill = obj.getString("address", "");
                final String agentPhoneOnBill = obj.getString("phoneNumber", "");


                if (bills.size() <= 0) {
                    body = CmdModels.GetBillInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.GetBillInfoReply.Result.BILL_NOT_FOUND)
                            .build();
                } else {

                    CmdModels.GetBillInfoReply.Builder builder = CmdModels.GetBillInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.GetBillInfoReply.Result.SUCESS)
                            .setAgentName(agentNameOnBill)
                            .setAgentAddress(agentAddressOnBill)
                            .setAgentPhone(agentPhoneOnBill);

                    for (int i = 0; i < bills.size(); i++) {
                        JsonObject detail = bills.get(i);
                        builder.addBillDetails(CmdModels.BillDetail.newBuilder()
                                        .setBillId(detail.getString("billId", ""))
                                        .setAmount(detail.getLong("amount", 0))
                                        .setStartDate(detail.getLong("startDate", 0))
                                        .setEndDate(detail.getLong("endDate", 0))
                        );
                    }
                    body = builder.build();
                }
                context.reply(new MomoCommand(CmdModels.CommandType.GET_BILL_INFO_REPLY, body));
            }
        });*/


        Buffer getBillBuf = MomoMessage.buildBuffer(
                SoapProto.MsgType.GET_BILL_INFO_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.GetBillInfo.newBuilder()
                        .setMpin(cmd.getPin())
                        .setBillId(cmd.getBillId())
                        .setProviderId(cmd.getProviderId())
                        .build()
                        .toByteArray()
        );


        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, getBillBuf, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                CmdModels.GetBillInfoReply body;
                int rcode = result.body().getInteger("rcode");
                String billInfo = result.body().getString("billInfo");
//                billInfo = "NGUYEN MINH KHUE,TO 42 KP2 Phuong Tan Chanh Hiep Quan 12,-1~~";

//                logger.debug("Billinfo: " + billInfo);

                if (rcode == SoapError.AGENT_NOT_FOUND) {
                    CmdModels.GetBillInfoReply.Builder builder = CmdModels.GetBillInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.GetBillInfoReply.Result.AGENT_NOT_FOUND);
                    context.reply(new MomoCommand(CmdModels.CommandType.GET_BILL_INFO_REPLY, builder.build()));
                }
                if (rcode != 0) {
                    context.replyError(rcode, "Error result.");
                    return;
                }
                JsonObject obj = billInfoToJsonObject(billInfo);
                JsonArray bills = obj.getArray("bills");

                final String agentNameOnBill = obj.getString("agentName", "");
                final String agentAddressOnBill = obj.getString("address", "");
                final String agentPhoneOnBill = obj.getString("phoneNumber", "");


                if (agentNameOnBill.isEmpty() && agentAddressOnBill.isEmpty() && agentPhoneOnBill.isEmpty()) {
                    body = CmdModels.GetBillInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.GetBillInfoReply.Result.BILL_NOT_FOUND)
                            .build();
                } else {

                    CmdModels.GetBillInfoReply.Builder builder = CmdModels.GetBillInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.GetBillInfoReply.Result.SUCESS)
                            .setAgentName(agentNameOnBill)
                            .setAgentAddress(agentAddressOnBill)
                            .setAgentPhone(agentPhoneOnBill);

                    for (int i = 0; i < bills.size(); i++) {
                        JsonObject detail = bills.get(i);
                        builder.addBillDetails(CmdModels.BillDetail.newBuilder()
                                        .setBillId(detail.getString("billId", ""))
                                        .setAmount(detail.getLong("amount", 0))
                                        .setStartDate(detail.getLong("startDate", 0))
                                        .setEndDate(detail.getLong("endDate", 0))
                        );
                    }
                    body = builder.build();
                }
                context.reply(new MomoCommand(CmdModels.CommandType.GET_BILL_INFO_REPLY, body));
            }
        });
    }

    public void payBill(final CommandContext context) {
        final CmdModels.PayBill cmd = (CmdModels.PayBill) context.getCommand().getBody();
/**
 partnerId	ProviderId	-->	partner_id
 amount	amount	-->	amount
 billId	billId	-->	billId
 */

        MomoProto.TranHisV1 tranHisV1 = MomoProto.TranHisV1.newBuilder()
                .setTranType(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE)
                .setPartnerId(cmd.getProviderId())
                .setAmount(cmd.getAmount())
                .setBillId(cmd.getBillId())
                .setSourceFrom(MomoProto.TranHisV1.SourceFrom.MOMO_VALUE)
                .build();

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }

        MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.TRANSFER_REQUEST_VALUE, 0, cmd.getPhoneNumber(), tranHisV1.toByteArray());

        payBillFactory.doPayOneBill(null, momoMessage, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jo) {
                int error = jo.getInteger(colName.TranDBCols.ERROR, 100);
                if (error > 0) {
                    context.replyError(error, String.valueOf(jo));
                    return;
                }

                CmdModels.PayBillReply.Builder cmdBody = CmdModels.PayBillReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(CmdModels.PayBillReply.Result.SUCCESS)
                        .setTransactionId(String.valueOf(jo.getLong(colName.TranDBCols.TRAN_ID, 0)));
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.PAY_BILL_REPLY, cmdBody.build());
                context.reply(replyCommand);
            }
        });
/*
        Buffer getBillBuf = MomoMessage.buildBuffer(
                SoapProto.MsgType.GET_BILL_INFO_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.GetBillInfo.newBuilder()
                        .setMpin(cmd.getPin())
                        .setBillId(cmd.getBillId())
                        .setProviderId(cmd.getProviderId())
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(SoapVerticle.ADDRESS, getBillBuf, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> result) {
                CmdModels.GetBillInfoReply body;
                int rcode = result.body().getInteger("rcode");
                String billInfo = result.body().getString("billInfo");

                if (rcode == SoapError.AGENT_NOT_FOUND) {
                    CmdModels.PayBillReply.Builder builder = CmdModels.PayBillReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.PayBillReply.Result.AGENT_NOT_FOUND);
                    context.reply(new MomoCommand(CmdModels.CommandType.PAY_BILL_REPLY, builder.build()));
                }
                if (rcode != 0) {
                    context.replyError(rcode, String.valueOf(result.body()));
                    return;
                }
                JsonObject obj = billInfoToJsonObject(billInfo);
                JsonArray bills = obj.getArray("bills");

                final String agentNameOnBill = obj.getString("agentName", "");
                final String agentAddressOnBill = obj.getString("address", "");
                final String agentPhoneOnBill = obj.getString("phoneNumber", "");


                Buffer payOneBill = MomoMessage.buildBuffer(
                        SoapProto.MsgType.PAY_ONE_BILL_VALUE,
                        0,
                        cmd.getPhoneNumber(),
                        SoapProto.PayOneBill.newBuilder()
                                .setPin(cmd.getPin())
                                .setProviderId(cmd.getProviderId())
                                .setBillId(cmd.getBillId())
                                .setChannel(Const.CHANNEL_WEB)
                                .setAmount(cmd.getAmount())
                                .build()
                                .toByteArray()
                );

                vertx.eventBus().send(SoapVerticle.ADDRESS, payOneBill, new Handler<CoreMessage<JsonObject>>() {
                    @Override
                    public void handle(CoreMessage<JsonObject> result) {
                        try {
                            MomoProto.TranHisV1. Builder builder = MomoProto.TranHisV1. newBuilder();
                            builder.setTranType(MomoProto.TranHisV1. TranType.PAY_ONE_BILL_VALUE);
                            builder.setClientTime(System.currentTimeMillis());
                            builder.setCategory(0);
                            builder.setPartnerId(cmd.getProviderId());
                            builder.setBillId(cmd.getBillId());
                            builder.setAmount(cmd.getAmount());
                            builder.setPartnerName(agentNameOnBill);
                            builder.setPartnerCode(agentPhoneOnBill);
                            builder.setPartnerRef(agentAddressOnBill);

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

                            CmdModels.PayBillReply.Builder cmdBody = CmdModels.PayBillReply.newBuilder()
                                    .setPhoneNumber(cmd.getPhoneNumber())
                                    .setResult(CmdModels.PayBillReply.Result.SUCCESS)
                                    .setTransactionId(result.body().getNumber("tranId", 0).toString());
                            MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.PAY_BILL_REPLY, cmdBody.build());
                            context.reply(replyCommand);
                        }
                    }
                });
            }
        });*/


    }

    public void saveBill(final CommandContext context) {
        final CmdModels.SaveBill cmd = (CmdModels.SaveBill) context.getCommand().getBody();


        BillsDb.Obj objBillDetail = new BillsDb.Obj();
//        objBillDetail.ownerAddress = agentAddressOnBill;
//        objBillDetail.ownerName = agentNameOnBill;
//        objBillDetail.ownerPhone = agentPhoneOnBill;

        objBillDetail.providerId = cmd.getProviderId();
        objBillDetail.billId = cmd.getBillId();

        mainDb.mBillsDb.addBill(cmd.getPhoneNumber(), objBillDetail, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                if (event) {
                    CmdModels.SaveBillReply.Builder cmdBody = CmdModels.SaveBillReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.SaveBillReply.Result.SUCCESS);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.SAVE_BILL_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                } else {
                    CmdModels.SaveBillReply.Builder cmdBody = CmdModels.SaveBillReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.SaveBillReply.Result.FAIL);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.SAVE_BILL_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                }
            }
        });

//        Buffer getBillBuf = MomoMessage.buildBuffer(
//                SoapProto.MsgType.GET_BILL_INFO_VALUE,
//                0,
//                cmd.getPhoneNumber(),
//                SoapProto.GetBillInfo.newBuilder()
//                        .setMpin(cmd.getPin())
//                        .setBillId(cmd.getBillId())
//                        .setProviderId(cmd.getProviderId())
//                        .build()
//                        .toByteArray()
//        );
//
//
//        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, getBillBuf, new Handler<CoreMessage<JsonObject>>() {
//            @Override
//            public void handle(CoreMessage<JsonObject> result) {
//                CmdModels.SaveBillReply body;
//                int rcode = result.body().getInteger("rcode");
//                String billInfo = result.body().getString("billInfo");
//
//                if (rcode != 0) {
//                    context.replyError(rcode, "");
//                    return;
//                }
//                JsonObject obj = billInfoToJsonObject(billInfo);
//                JsonArray bills = obj.getArray("bills");
//
//                final String agentNameOnBill = obj.getString("agentName", "");
//                final String agentAddressOnBill = obj.getString("address", "");
//                final String agentPhoneOnBill = obj.getString("phoneNumber", "");
//
//
//                if (bills.size() <= 0) {
//                    body = CmdModels.SaveBillReply.newBuilder()
//                            .setPhoneNumber(cmd.getPhoneNumber())
//                            .setResult(CmdModels.SaveBillReply.Result.FAIL)
//                            .build();
//                } else {
//                    BillsDb.Obj objBillDetail = new BillsDb.Obj();
//                    objBillDetail.ownerAddress = agentAddressOnBill;
//                    objBillDetail.ownerName = agentNameOnBill;
//                    objBillDetail.ownerPhone = agentPhoneOnBill;
//
//                    objBillDetail.providerId = cmd.getProviderId();
//                    objBillDetail.billId = cmd.getBillId();
//
//                    mainDb.mBillsDb.addBill(cmd.getPhoneNumber(),objBillDetail, new Handler<Boolean>() {
//                        @Override
//                        public void handle(Boolean event) {
//                            if (event) {
//                                CmdModels.SaveBillReply.Builder cmdBody = CmdModels.SaveBillReply.newBuilder()
//                                        .setPhoneNumber(cmd.getPhoneNumber())
//                                        .setResult(CmdModels.SaveBillReply.Result.SUCCESS);
//                                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.SAVE_BILL_REPLY, cmdBody.build());
//                                context.reply(replyCommand);
//                            } else {
//                                CmdModels.SaveBillReply.Builder cmdBody = CmdModels.SaveBillReply.newBuilder()
//                                        .setPhoneNumber(cmd.getPhoneNumber())
//                                        .setResult(CmdModels.SaveBillReply.Result.FAIL);
//                                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.SAVE_BILL_REPLY, cmdBody.build());
//                                context.reply(replyCommand);
//                            }
//                        }
//                    });
//
//                }
//            }
//        });

    }

    public void getSavedBill(final CommandContext context) {
        final CmdModels.GetSavedBill cmd = (CmdModels.GetSavedBill) context.getCommand().getBody();

        BillsDb.Obj obj = new BillsDb.Obj();
        obj.payChanel = 0;
        mainDb.mBillsDb.getAllBills(cmd.getPhoneNumber(), obj, new Handler<ArrayList<BillsDb.Obj>>() {
            @Override
            public void handle(ArrayList<BillsDb.Obj> result) {
                CmdModels.GetSavedBillReply.Builder cmdBody = CmdModels.GetSavedBillReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber());
                for (BillsDb.Obj obj : result) {
                    CmdModels.BillDetail bill = CmdModels.BillDetail.newBuilder()
                            .setBillId(obj.billId)
                            .setProviderId(obj.providerId)
                            .setAmount(obj.totalAmount)
                            .build();
                    cmdBody.addBillDetails(bill);
                }
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_SAVED_BILL_REPLY, cmdBody.build());
                context.reply(replyCommand);
            }
        });
    }

    public void removeSavedBill(final CommandContext context) {
        final CmdModels.RemoveSavedBill cmd = (CmdModels.RemoveSavedBill) context.getCommand().getBody();

        BillsDb.Obj objBillDetail = new BillsDb.Obj();
        objBillDetail.billId = cmd.getBillId();
        objBillDetail.providerId = cmd.getProviderId();
        objBillDetail.payChanel = 0;
        mainDb.mBillsDb.deleteBill(cmd.getPhoneNumber(), objBillDetail, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                if (event) {
                    CmdModels.RemoveSavedBillReply.Builder cmdBody = CmdModels.RemoveSavedBillReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.RemoveSavedBillReply.Result.SUCCESS);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.REMOVE_SAVED_BILL_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                } else {
                    CmdModels.RemoveSavedBillReply.Builder cmdBody = CmdModels.RemoveSavedBillReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.RemoveSavedBillReply.Result.FAIL);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.REMOVE_SAVED_BILL_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                }
            }
        });
    }

    public void payAirlineTicket(final CommandContext context) {
        final CmdModels.PayAirlineTicket cmd = (CmdModels.PayAirlineTicket) context.getCommand().getBody();

        /*ProviderId	-->	partner_id
        amount	-->	amount
        billId	-->	billId
        */

        MomoProto.TranHisV1 tranHisV1 = MomoProto.TranHisV1.newBuilder()
                .setTranType(MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE)
                .setPartnerId(cmd.getProviderId())
                .setAmount(cmd.getAmount())
                .setBillId(cmd.getBillId())
                .setSourceFrom(MomoProto.TranHisV1.SourceFrom.MOMO_VALUE)
                .build();

        MomoMessage msg = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, 0, cmd.getPhoneNumber(), tranHisV1.toByteArray());

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }

        payBillFactory.doPayOneBill(null, msg, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                int error = json.getInteger("error");
                final long tranId = json.getLong("tranId", 0);
                if (error != 0) {
                    context.replyError(error, String.valueOf(json));
                } else {
                    CmdModels.PayAirlineTicketReply.Builder cmdBody = CmdModels.PayAirlineTicketReply.newBuilder()
                            .setResult(CmdModels.PayAirlineTicketReply.Result.SUCCESS)
                            .setTransactionId(tranId);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.REMOVE_SAVED_BILL_REPLY, cmdBody.build());
                    context.reply(replyCommand);
                }
            }
        });
    }

    public void getServiceLayout(final CommandContext context) {
        final MomoProto.GetServiceLayout cmd = (MomoProto.GetServiceLayout) context.getCommand().getBody();

        SockData sockData = context.getSockData(context.getCommand().getPhone());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }

        MomoMessage msg = new MomoMessage(MomoProto.MsgType.GET_SERVICE_LAYOUT_VALUE, 0, context.getCommand().getPhone(), cmd.toByteArray());

        infoFactory.getServiceLayout(null, msg, sockData, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                if (result != null) {
                    BillInfoService bis = new BillInfoService(result);
                    MomoProto.GetServiceLayoutReply.Builder builder = MomoProto.GetServiceLayoutReply.newBuilder();

                    builder.setTotalAmount(bis.total_amount);

                    if (bis.customer_info != null && bis.customer_info.size() > 0) {
                        for (BillInfoService.TextValue tv : bis.customer_info) {
                            MomoProto.TextValue textValue = MomoProto.TextValue.newBuilder()
                                    .setText(tv.text)
                                    .setValue(tv.value)
                                    .build();
                            builder.addCustomerInfo(textValue);
                        }
                    }
                    if (bis.array_price != null && bis.array_price.size() > 0) {
                        for (BillInfoService.TextValue tv : bis.array_price) {
                            MomoProto.TextValue textValue = MomoProto.TextValue.newBuilder()
                                    .setText(tv.text)
                                    .setValue(tv.value)
                                    .build();
                            builder.addArrayPrice(textValue);
                        }
                    }

                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_SERVICE_LAYOUT_REPLY, builder.build());
                    context.reply(replyCommand);
                }
            }
        });
    }

    public void getService(final CommandContext context) {
        final MomoProto.Service cmd = (MomoProto.Service) context.getCommand().getBody();

        MomoMessage msg = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, 0, context.getCommand().getPhone(), cmd.toByteArray());
        infoFactory.getServices(null, msg, new Handler<ArrayList<ServiceDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDb.Obj> objs) {

                MomoProto.ServiceReply.Builder serviceListBuilder = MomoProto.ServiceReply.newBuilder();

                for (ServiceDb.Obj o : objs) {
                    MomoProto.ServiceItem.Builder builder = MomoProto.ServiceItem.newBuilder();

                    builder.setServiceType(o.serviceType);
                    builder.setPartnerCode(o.partnerCode);
                    builder.setServiceId(o.serviceID);
                    builder.setServiceName(o.serviceName);
                    builder.setPartnerSite(o.partnerSite);
                    builder.setIconUrl(o.iconUrl);
                    builder.setStatus(o.status);
                    builder.setTextPopup(o.textPopup);
                    builder.setHasCheckDebit(o.hasCheckDebit);
                    builder.setTitleDialog(o.titleDialog);
                    builder.setLastUpdate(o.lastUpdateTime);
                    builder.setBillidType(o.billType);
                    builder.setStar(o.star);
                    builder.setTotalForm(o.totalForm);
                    builder.setCategoryName(o.cateName);
                    builder.setCategoryId(o.cateId);

                    serviceListBuilder.addServiceList(builder.build());
                }

                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_SERVICE_REPLY, serviceListBuilder.build());
                context.reply(replyCommand);
            }
        });
    }


    //BEGIN 0000000032 Quan li thoi gian thanh toan 1 bill


}
