package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.msg.*;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.PhoneNumberUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.ModelMapper;
import com.mservice.momo.vertx.processor.transferbranch.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

//import com.mservice.momo.vertx.models.ModelMapper;

/**
 * Created by nam on 7/25/14.
 */
public class MakeTranHandler extends CommandHandler {

    private Logger logger;

    private M2m m2mHelper;
    private PayAirLineTicket payAirLineTicketHelper;
    private PayAVGBill payAVGBillHelper;
    private PayCinemaTicket payCinemaTicketHelper;
    private PayNuocCLBill payNuocCLBillHelper;
    private PayOneBill payOneBillHelper;
    private TopUp topupHelper;
    private TopUpGame topUpGameHelper;
    private Vinagame123Phim vinagame123PhimHelper;

    private final String otpSmsMessageTemplate;

    public MakeTranHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject globalConfig) {
        super(mainDb, vertx, container, globalConfig);
        logger = container.logger();

        m2mHelper = new M2m(vertx, logger, globalConfig);
        payAirLineTicketHelper = new PayAirLineTicket(vertx, logger, globalConfig);
        payAVGBillHelper = new PayAVGBill(vertx, logger, globalConfig);
        payCinemaTicketHelper = new PayCinemaTicket(vertx, logger, globalConfig);
        payNuocCLBillHelper = new PayNuocCLBill(vertx, logger, globalConfig);
        payOneBillHelper = new PayOneBill(vertx, logger, globalConfig);
        topupHelper = new TopUp(vertx, logger, globalConfig);
        topUpGameHelper = new TopUpGame(vertx, logger, globalConfig);
        vinagame123PhimHelper = new Vinagame123Phim(vertx, logger, globalConfig);


        this.otpSmsMessageTemplate = globalConfig.getObject("commandVerticle").getObject("otpHandler").getString("otpSmsMessageTemplate", "Your OTP number is %s. Don't reply this message please.");
    }

    public void makeTran(CommandContext context) {
        final MomoProto.TranHisV1 request = (MomoProto.TranHisV1) context.getCommand().getBody();
        final int phone = context.getCommand().getPhone();

        if (!PhoneNumberUtil.isValidPhoneNumber(phone)) {
            context.replyError(-1, "Invalid phone number!");
        }

        SockData sockData = context.getSockData(phone);
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }

        context.getTranMap().put(phone, request);

        final String generatedOtp = DataUtil.getOtp();

        logger.info("Generated OTP for [" + phone + "]: " + generatedOtp);

        mainDb.mPhonesDb.setOtp(phone, generatedOtp, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {

            }
        });

        SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                .setSmsId(0)
                .setToNumber(phone)
                .setContent(String.format(otpSmsMessageTemplate, generatedOtp))
                .build();
        vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
        logger.debug(String.format("Send SMS to %s: " + otpSmsMessageTemplate, phone, generatedOtp));

        CmdModels.MakeTranReply replyBody = CmdModels.MakeTranReply.newBuilder()
                .setError(0)
                .build();
        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.MAKE_TRAN_REPLY, replyBody);
        context.reply(replyCommand);
    }

    public void sendMakeTranReply(final CommandContext context, JsonObject result) {
        MomoProto.TranHisV1 tranHisV1 = ModelMapper.toTranHisV1(result);
        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.COMPLETE_TRAN_REPLY, tranHisV1);
        context.reply(replyCommand);
    }

    public void callTransactionHelper(final CommandContext context, SockData sockData, int phone, final MomoProto.TranHisV1 tranHisV1, final Handler<JsonObject> callback) {
        MomoMessage msg = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, context.getCommand().getId(), phone,
                tranHisV1.toByteArray());

        switch (tranHisV1.getTranType()) {
            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                m2mHelper.doM2M(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                payAirLineTicketHelper.doPayAirlineTicket(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.PAY_AVG_BILL_VALUE:
                payAVGBillHelper.doPayAVGBill(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_CINEMA_VALUE:
                payCinemaTicketHelper.doPayCinemaTicket(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.PAY_NUOCCL_BILL_VALUE:
                payNuocCLBillHelper.doPayAVGBill(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
                payOneBillHelper.doPayOneBill(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                topupHelper.doTopUp(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                topUpGameHelper.doTopUpGame(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
                vinagame123PhimHelper.do123Phim(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
                break;
        }
    }

    public void completeTran(final CommandContext context) {
        final CmdModels.CompeleteTran cmd = (CmdModels.CompeleteTran) context.getCommand().getBody();
        final int phone = context.getCommand().getPhone();

        if (!PhoneNumberUtil.isValidPhoneNumber(phone)) {
            context.replyError(-1, "Invalid phone number!");
        }

        final SockData sockData = context.getSockData(phone);
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }
        final MomoProto.TranHisV1 tranHisV1 = context.getTranMap().get(phone);
        if (tranHisV1 == null) {
            context.replyError(-2, "Call make transaction first.");
            return;
        }
        mainDb.mPhonesDb.getOtp(phone, new Handler<String>() {
            @Override
            public void handle(String generatedOtp) {
                if (cmd.getOtp() == null || !cmd.getOtp().equals(generatedOtp)) {
                    context.replyError(-3, "Otp does not match.");
                    return;
                }
                mainDb.mPhonesDb.setOtp(phone, "", new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {

                    }
                });

                //do transaction:

                callTransactionHelper(context, sockData, phone, tranHisV1, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        MomoProto.TranHisV1 tranHisV1 = ModelMapper.toTranHisV1(result);
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.COMPLETE_TRAN_REPLY, tranHisV1);
                        context.reply(replyCommand);
                    }
                });

            }
        });
    }

    public void doTran(final CommandContext context) {
        final MomoProto.TranHisV1 cmd = (MomoProto.TranHisV1) context.getCommand().getBody();
        final int phone = context.getCommand().getPhone();

        if (!PhoneNumberUtil.isValidPhoneNumber(phone)) {
            context.replyError(-1, "Invalid phone number!");
        }

        final SockData sockData = context.getSockData(phone);
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }

        callTransactionHelper(context, sockData, phone, cmd, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                MomoProto.TranHisV1 tranHisV1 = ModelMapper.toTranHisV1(result);
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.DO_TRAN_REPLY, tranHisV1);
                context.reply(replyCommand);
            }
        });
    }

}
