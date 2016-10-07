package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 3/13/14.
 */
public class OtpHandler extends CommandHandler {

    private final boolean sendSmsMessage;
    private final String otpSmsMessageTemplate;
    private final int maxLoginAttempt;

    public OtpHandler(MainDb mainDb, Vertx vertx, Container container, int maxLoginAttempt, JsonObject config) {
        super(mainDb, vertx, container, config);

        this.maxLoginAttempt = maxLoginAttempt;
        this.sendSmsMessage = config.getBoolean("sendSmsMessage", false);
        this.otpSmsMessageTemplate = config.getString("otpSmsMessageTemplate", "Your OTP number is %s. Don't reply this message please.");
    }


    public void sendOtp(final CommandContext context) {
        CmdModels.SendOtp cmd = (CmdModels.SendOtp) context.getCommand().getBody();

        final String generatedOtp = DataUtil.getOtp();

        logger.info("Generated OTP for [" + cmd.getPhoneNumber() + "]: " + generatedOtp);

        mainDb.mPhonesDb.setOtp(cmd.getPhoneNumber(), generatedOtp, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                logger.debug("SET OTP-TIME WHEN DID GET OTP " + result);
            }
        });

        if (sendSmsMessage) {
            SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                    .setSmsId(0)
                    .setToNumber(cmd.getPhoneNumber())
                    .setContent(String.format(otpSmsMessageTemplate, generatedOtp))
                    .build();
            vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
            logger.debug(String.format("Send SMS to %s: " + otpSmsMessageTemplate, cmd.getPhoneNumber(), generatedOtp));
        }

        CmdModels.SendOtpReply replyBody = CmdModels.SendOtpReply.newBuilder()
                .setPhoneNumber(cmd.getPhoneNumber())
                .setOtp(generatedOtp)
                .build();
        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.SEND_OTP_REPLY, replyBody);

        context.reply(replyCommand);
    }

    public void verifyOtp(final CommandContext context) {
        final CmdModels.VerifyOtp cmd = (CmdModels.VerifyOtp) context.getCommand().getBody();

        mainDb.mPhonesDb.getPhoneInfo(cmd.getPhoneNumber(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                final PhonesDb.Obj phoneObj = new PhonesDb.Obj(event.body().getObject(MongoKeyWords.RESULT,null));
                logger.info("GET OTP-TIME FROM PHONE DB : " + phoneObj);
                if (phoneObj != null) {
                    //check thoi gian xac thuc OTP khong qua 5 phut

                    if (System.currentTimeMillis() - phoneObj.otp_time > 5 * 60 * 1000) {
                        CmdModels.VerifyOtpReply replyBody = CmdModels.VerifyOtpReply.newBuilder()
                                .setPhoneNumber(cmd.getPhoneNumber())
                                .setResult(CmdModels.VerifyOtpReply.ResultCode.OUT_OF_TIME)
                                .build();

                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_OTP_REPLY, replyBody);
                        context.getRequest().reply(MomoCommand.toBuffer(replyCommand).getBytes());
                        return;
                    }

                    //check OTP khong dung
                    if (!cmd.getOtp().equalsIgnoreCase(phoneObj.otp)) {
                        CmdModels.VerifyOtpReply replyBody = CmdModels.VerifyOtpReply.newBuilder()
                                .setPhoneNumber(cmd.getPhoneNumber())
                                .setResult(CmdModels.VerifyOtpReply.ResultCode.NOT_MATCH)
                                .build();

                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_OTP_REPLY, replyBody);
                        context.getRequest().reply(MomoCommand.toBuffer(replyCommand).getBytes());
                        return;
                    }

                    CmdModels.VerifyOtpReply replyBody = CmdModels.VerifyOtpReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.VerifyOtpReply.ResultCode.SUCCESS)
                            .build();

                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_OTP_REPLY, replyBody);
                    context.reply(replyCommand);
                } else {
                    //khong tin thay phone info
                    //chuyen nay khong the xay ra dc
                    logger.fatal("Loi nghiem trong, MongoKeyWords db khong luu & load dc du lieu - ServerVerticle");
                }
            }
        });
//
//        if(1==1)return;
//        mainDb.mPhonesDb.forceSyncWithCore(cmd.getPhoneNumber(), new Handler<PhonesDb.Obj>() {
//            @Override
//            public void handle(PhonesDb.Obj obj) {
//                final PhonesDb.Obj phoneObj = obj;
//                logger.info("GET OTP-TIME FROM PHONE DB : " + phoneObj);
//                if (phoneObj != null) {
//                    //check thoi gian xac thuc OTP khong qua 5 phut
//
//                    if (System.currentTimeMillis() - phoneObj.otp_time > 5 * 60 * 1000) {
//                        CmdModels.VerifyOtpReply replyBody = CmdModels.VerifyOtpReply.newBuilder()
//                                .setPhoneNumber(cmd.getPhoneNumber())
//                                .setResult(CmdModels.VerifyOtpReply.ResultCode.OUT_OF_TIME)
//                                .build();
//
//                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_OTP_REPLY, replyBody);
//                        context.getRequest().reply(MomoCommand.toBuffer(replyCommand).getBytes());
//                        return;
//                    }
//
//                    //check OTP khong dung
//                    if (!cmd.getOtp().equalsIgnoreCase(phoneObj.otp)) {
//                        CmdModels.VerifyOtpReply replyBody = CmdModels.VerifyOtpReply.newBuilder()
//                                .setPhoneNumber(cmd.getPhoneNumber())
//                                .setResult(CmdModels.VerifyOtpReply.ResultCode.NOT_MATCH)
//                                .build();
//
//                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_OTP_REPLY, replyBody);
//                        context.getRequest().reply(MomoCommand.toBuffer(replyCommand).getBytes());
//                        return;
//                    }
//
//                    CmdModels.VerifyOtpReply replyBody = CmdModels.VerifyOtpReply.newBuilder()
//                            .setPhoneNumber(cmd.getPhoneNumber())
//                            .setResult(CmdModels.VerifyOtpReply.ResultCode.SUCCESS)
//                            .build();
//
//                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.VERIFY_OTP_REPLY, replyBody);
//                    context.reply(replyCommand);
//                } else {
//                    //khong tin thay phone info
//                    //chuyen nay khong the xay ra dc
//                    logger.fatal("Loi nghiem trong, MongoKeyWords db khong luu & load dc du lieu - ServerVerticle");
//                }
//            }
//        });
    }

    public void sendSms(CommandContext context) {
        CmdModels.SendSms cmd = (CmdModels.SendSms) context.getCommand().getBody();

        SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                .setSmsId(0)
                .setToNumber(cmd.getPhoneNumber())
                .setContent(cmd.getContent())
                .build();
        vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
        logger.debug(String.format("Send SMS to %s: " + cmd.getContent(), cmd.getPhoneNumber()));

        CmdModels.SendSmsReply replyBody = CmdModels.SendSmsReply.newBuilder()
                .build();

        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.SEND_SMS_REPLY, replyBody);
        context.reply(replyCommand);
    }
}
