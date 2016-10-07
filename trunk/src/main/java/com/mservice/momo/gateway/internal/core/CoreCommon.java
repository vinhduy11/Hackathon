package com.mservice.momo.gateway.internal.core;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.objects.*;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 9/26/14.
 */
public class CoreCommon {

    /*required uint64 tid = 1;
    required int32 errorCode = 2;
    optional string description =3;
    repeated KeyValuePair params =4;*/

    //busAddress : srv86.CoreConnectorVerticle

    public static void doAction(final Vertx _vertx
            , final String busAddress
            , final Request reqObj
            , final Handler<Response> callback, final Logger _logger) {

        //AppConstant.CoreConnectorVerticle_ADDRESS
        _logger.info("request to core connector with json : " + reqObj.toJsonObject().encodePrettily());

        _vertx.eventBus().send(busAddress, reqObj.toJsonObject(), new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {

                Response replyObj = new Response();


                final MomoMessage reply = MomoMessage.fromBuffer(message.body());
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(reply.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                //lock tien khong thanh cong
                if ((rpl == null)) {
                    replyObj.Error = -100;
                    replyObj.Description = "Lỗi hệ thống";

                } else {

                    replyObj.Error = rpl.getErrorCode();
                    replyObj.Description = rpl.getDescription();
                    replyObj.Tid = rpl.getTid();
                    if (rpl.getParamsCount() > 0) {
                        replyObj.KeyValueList = new ArrayList<>();

                        for (int i = 0; i < rpl.getParamsCount(); i++) {

                            KeyValue kv = new KeyValue();
                            kv.Key = rpl.getParams(i).getKey();
                            kv.Value = rpl.getParams(i).getValue();
                            replyObj.KeyValueList.add(kv);
                        }
                    }
                }

                callback.handle(replyObj);
            }
        });
    }

    public static void getBalanceNew(Vertx _vertx
            , final String number
            , final String pin
            , final Logger _logger
            , final Handler<Response> callback) {

        Request loginObj = new Request();
        loginObj.TYPE = Command.BALANCE;
        loginObj.SENDER_NUM = number;
        loginObj.SENDER_PIN = pin;

        JsonObject loginJo = loginObj.toJsonObject();

        _vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, loginJo, new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                MomoMessage momoMessage = MomoMessage.fromBuffer(message.body());
                Core.StandardReply reply;
                Response replyObj = new Response();

                try {
                    reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
                } catch (Exception ex) {
                    reply = null;
                }

                if (reply == null) {
                    replyObj.Error = -100; //loi he thong
                } else {
                    replyObj.Error = reply.getErrorCode();
                    replyObj.Description = reply.getDescription() == null ? "" : reply.getDescription();

                    if (reply.getParamsCount() > 0) {
                        for (int i = 0; i < reply.getParamsCount(); i++) {
                            replyObj.KeyValueList.add(new KeyValue(reply.getParams(i).getKey()
                                    , reply.getParams(i).getValue()));
                        }
                    } else {

                        replyObj.KeyValueList.add(new KeyValue("momo"
                                , String.valueOf(reply.getErrorCode())));
                    }
                }

                callback.handle(replyObj);

            }
        });
    }

    public static void register(Vertx _vertx
            , final int number
            , final String name
            , final String idCard
            , final String email
            , final String pin
            , final Logger _logger
            , final Handler<Integer> callback) {

        JsonObject jo = new JsonObject();
        jo.putString(Structure.TYPE, Command.REGISTER);
        jo.putString(Structure.AGENT_NAME, name.replace(" ", "_"));
        jo.putString(Structure.AGENT_ID_CARD, idCard.replace(" ", "_"));
        jo.putString(Structure.AGENT_EMAIL, email.replace(" ", "_"));
        jo.putString(Structure.AGENT_PIN, pin.replace(" ", "_"));
        jo.putString(Structure.AGENT_NUMBER, "0" + number);

        _vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, jo, new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                MomoMessage momoMessage = MomoMessage.fromBuffer(message.body());
                Core.StandardReply reply;
                try {
                    reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
                } catch (Exception ex) {
                    reply = null;
                }

                if (reply == null) {
                    _logger.info("register : can not parse reveived buffer from core, number : " + number);
                    callback.handle(-1);
                    return;
                }

                //final long rplTranId = reply.getTid();
                _logger.info("register reply, ecode: " + reply.getTid() + "," + reply.getErrorCode());
                callback.handle(reply.getErrorCode());

            }
        });
    }
//
//    public static void adjustment(
//            final  Vertx _vertx
//            ,final String senderAgent
//            ,final long amount
//            ,final String recvAgent
//            ,final String type
//            ,long timeout
//            ,final Logger mLogger
//            ,final Handler<Response> callback ){
//
//        JsonObject jo = new JsonObject();
//        jo.putString(Structure.TYPE, Command.ADJUST);
//        jo.putString(Structure.SENDER_NUM, senderAgent);
//        jo.putString(Structure.RECVER_NUM, recvAgent);
//        jo.putNumber(Structure.TRAN_AMOUNT, amount);
//        jo.putString(Structure.ADJUST_TYP, type);
//
//        mLogger.info("adjustment senderAgent : " + senderAgent);
//
//        _vertx.eventBus().sendWithTimeout(AppConstant.CoreConnectorVerticle_ADDRESS, jo, timeout, new Handler<AsyncResult<CoreMessage<Buffer>>>() {
//            @Override
//            public void handle(AsyncResult<CoreMessage<Buffer>> result) {
//
//                Response reply;
//                if (result.succeeded()) {
//                    MomoMessage momoMessage = MomoMessage.fromBuffer(result.result().body());
//                    try {
//                        reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
//                    } catch (InvalidProtocolBufferException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    reply = new Response();
//                    reply.Error = SoapError.IN_TIMEOUT;
//                    reply.Tid = System.currentTimeMillis();
//                    reply.Description = SoapError.getDesc(SoapError.IN_TIMEOUT);
//                }
//                MomoMessage momoMessage = MomoMessage.fromBuffer(result.result().body());
//                Core.StandardReply reply;
//                try {
//                    reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
//                } catch (Exception ex) {
//                    reply = null;
//                }
//
//                JsonObject joResult = new JsonObject();
//                if (reply == null) {
//                    mLogger.info("adjustment : can not parse reveived buffer from core , senderAgent : " + senderAgent);
//                    joResult.putNumber("ERROR", -1);
//                    joResult.putNumber("TID", -1);
//                } else {
//                    joResult.putNumber("ERROR", reply.getErrorCode());
//                    joResult.putNumber("TID", reply.getTid());
//                }
//
//                //final long rplTranId = reply.getTid();
//                mLogger.info("adjustment, ecode: " + reply.getTid() + "," + reply.getErrorCode());
//                callback.handle(joResult);
//            }
//        });
//    }

    public static void tranWithPointAndVoucher(final Vertx _vertx
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , int phone
            , String pin
            , long pointAmt
            , long voucherAmt
            , long tranAmount
            , int coreProcessType //ma thanh toan -->dien .... serviceId
            , String tranType
            , String specialAgent // tai khoan nhan tien
            , String channel // kenh thuc hien
            , String recipient // hoac la ma hoa don(billerId)
            , int voucherOrPointType // loai thuc hien dung VC or point/ VC / Point
            , long timeOut
            , final Handler<Response> callback) {
        JsonObject jo = new JsonObject();
        log.add("function", "tranWithPointAndVoucher");
        log.add("pointAmt", pointAmt);
        log.add("voucherAmt", voucherAmt);
        log.add("coreProcessType", coreProcessType);
        log.add("tranType", tranType);
        log.add("pin len", pin.length());
        log.add("phone", "0" + phone);
        /*
                vp_type =  1: dùng voucher và point
                           2: chỉ dùng vouher + momo (nếu có)
                           3: chỉ point và momo (nêu có)
        */
        //TRANWVP pin vp_type amountvoucher amountpoint amounttrans transtype(buy|billpay) recipient billerid sepecialagent channal")
        //sepecialagent : topup : airtime ; agent dac biet

        String sms = "";
        if (tranType.equalsIgnoreCase(Const.CoreTranType.Topup)) {
            specialAgent = "airtime";
        }

        log.add("specialAgent", specialAgent);

        sms = "TRANWVP " + pin
                + " " + voucherOrPointType
                + " " + voucherAmt
                + " " + pointAmt
                + " " + tranAmount
                + " " + tranType  // transtype(buy|billpay)
                + " " + recipient
                + " " + coreProcessType
                + " " + specialAgent
                + " " + channel;

        //TRANWVP 111111 2 85000 0 85000 billpay bhd 2 01634240762 mobi

        jo.putString(Structure.TYPE, Command.TRAN_WITH_VOUCHER_AND_POINT);
        jo.putString(Structure.INITIATOR, "0" + phone);
        jo.putString(Structure.SMS, sms);
        log.add("Sms to core", sms);
        log.writeLog();

        callCore(_vertx, log, timeOut, jo, callback);
    }

    /*
    Gửi tiền: VOUCHERIN pin recipientname recipient providername provider amount
        VOUCHERIN : ma lenh
        recipientname: ten nguoi nhan
        recipient : so dien thoai nguoi nhan
        providername: ten nguoi gui
        provider:  so dien thoai nguoi gui
        keyvaluepair tu add
        MTCN == couponid
    Kiểm tra thông tin giao dịch :  VOUCHEROUTINIT pin MTCN amount
    Nhận tiền : VOUCHEROUTCNFRM pin
    */

    public static void doC2C(final Vertx _vertx
            , final Common.BuildLog log
            , String agentPhone
            , final String agentPin
            , String sndName
            , String sndPhone
            , String sndCardId
            , String rcvName
            , String rcvPhone
            , String rcvCardId
            , long tranAmount
            , long timeOut
            , String comment
            , String retailerAddr
            , String retailerMomoPhone
            , ArrayList<Misc.KeyValue> keyValues
            , final Handler<Response> callback) {
        JsonObject jo = new JsonObject();
        log.add("func", "depositCash");
        log.add("agentPin len", agentPin.length());
        log.add("sndName", sndName);
        log.add("sndPhone", sndPhone);

        log.add("rcvName", rcvName);
        log.add("rcvPhone", rcvPhone);
        log.add("amount", tranAmount);

        JsonArray array = new JsonArray();
        array.add(new JsonObject().putString("c2ctype", "c2c"));
        array.add(new JsonObject().putString("message", comment));

        //recipientname,recipient,providername,provider,amount,recipientid,providerid,c2ctype,message

        array.add(new JsonObject().putString("recipientname", rcvName));
        array.add(new JsonObject().putString("recipient", rcvPhone));
        array.add(new JsonObject().putString("recipientid", rcvCardId));

        array.add(new JsonObject().putString("providername", sndName));
        array.add(new JsonObject().putString("provider", sndPhone));
        array.add(new JsonObject().putString("providerid", sndCardId));
        array.add(new JsonObject().putString("amount", String.valueOf(tranAmount)));

        array.add(new JsonObject().putString("client", "backend"));
        array.add(new JsonObject().putString("chanel", "mobi"));
        array.add(new JsonObject().putString("issms", "no"));

        if (keyValues != null && keyValues.size() > 0) {

            log.add("begin keyvaluepairs", "---------");
            for (int i = 0; i < keyValues.size(); i++) {

                Misc.KeyValue kv = keyValues.get(i);
                array.add(new JsonObject().putString(kv.Key, kv.Value));
                log.add(kv.Key, kv.Value);
            }
        }

        /*VOUCHERIN pin recipientname recipient providername provider amount
        VOUCHERIN : ma lenh
        recipientname: ten nguoi nhan
        recipient : so dien thoai nguoi nhan
        providername: ten nguoi gui
        provider:  so dien thoai nguoi gui
        keyvaluepair tu add
        MTCN == couponid*/
        String sms = "";
        sms = "VOUCHERIN " + agentPin;

        jo.putString(Structure.TYPE, Command.RETAILER_TRANSFER_CASH);
        jo.putString(Structure.INITIATOR, agentPhone);
        jo.putString(Structure.SMS, sms);
        jo.putArray(Structure.KEY_VALUE_PAIR_ARR, array);
        jo.putString(Structure.PHONE_NUMBER, agentPhone);
        log.add("Sms to core", sms);
        log.writeLog();

        callCore(_vertx, log, timeOut, jo, callback);
    }

    public static void doGetC2CInfo(final Vertx _vertx
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , String agentPhone
            , final String agentPin
            , long tranAmount
            , String rcvCashCode
            , long timeOut
            , ArrayList<Misc.KeyValue> keyValues
            , final Handler<Response> callback) {
        JsonObject jo = new JsonObject();
        log.add("func", "depositCash");
        log.add("agentPin len", agentPin.length());
        log.add("amount", tranAmount);

        JsonArray array = new JsonArray();

        if (keyValues != null && keyValues.size() > 0) {
            log.add("begin keyvaluepairs", "---------");

            for (int i = 0; i < keyValues.size(); i++) {
                Misc.KeyValue kv = keyValues.get(i);
                array.add(new JsonObject().putString(kv.Key, kv.Value));
                log.add(kv.Key, kv.Value);
            }
        }

        Request coreReq = new Request();
        coreReq.TYPE = Command.RETAILER_TRANSFER_CASH_RECOMMIT;
        coreReq.TARGET = rcvCashCode;
        coreReq.TRAN_AMOUNT = tranAmount;
        coreReq.SENDER_NUM = agentPhone;
        coreReq.SENDER_PIN = agentPin;
        coreReq.WALLET = Core.WalletType.MOMO_VALUE;

        callCore(_vertx, log, timeOut, coreReq.toJsonObject(), callback);
    }

    public static void doC2cCommit(final Vertx _vertx
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , String agentPhone
            , final String agentPin
            , final String rcvCashCode
            , long timeOut
            , ArrayList<Misc.KeyValue> keyValues
            , final Handler<Response> callback) {
        JsonObject jo = new JsonObject();
        log.add("func", "doC2cCommit");
        log.add("agentPin len", agentPin.length());

        JsonArray array = new JsonArray();

        if (keyValues != null && keyValues.size() > 0) {

            log.add("begin keyvaluepairs", "---------");

            for (int i = 0; i < keyValues.size(); i++) {
                Misc.KeyValue kv = keyValues.get(i);
                array.add(new JsonObject().putString(kv.Key, kv.Value));
                log.add(kv.Key, kv.Value);
            }
        }

        array.add(new JsonObject().putString("client", "backend"));
        array.add(new JsonObject().putString("chanel", "mobi"));
        array.add(new JsonObject().putString("issms", "no"));

        /*Nhận tiền : VOUCHEROUTCNFRM pin*/
        String sms = "";
        sms = "VOUCHEROUTCNFRM " + agentPin + " " + rcvCashCode;

        jo.putString(Structure.TYPE, Command.RETAILER_TRANSFER_CASH_COMMIT);
        jo.putString(Structure.INITIATOR, agentPhone);
        jo.putString(Structure.SMS, sms);
        jo.putArray(Structure.KEY_VALUE_PAIR_ARR, array);
        jo.putString(Structure.PHONE_NUMBER, agentPhone);
        log.add("Sms to core", sms);
        log.writeLog();
        log.add("Func", "callCore");
        callCore(_vertx, log, timeOut, jo, callback);
    }

    private static void callCore(Vertx _vertx
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , long timeOut
            , JsonObject jo
            , final Handler<Response> callback) {
        log.add("callCore", jo.toString());
        _vertx.eventBus().sendWithTimeout(AppConstant.CoreConnectorVerticle_ADDRESS, jo, timeOut, new Handler<AsyncResult<Message<Buffer>>>() {
            @Override
            public void handle(AsyncResult<Message<Buffer>> result) {
                Response reply;
                log.add("result after callCore", result.succeeded());
                if (result.failed()) {
                    reply = new Response();
                    reply.Error = SoapError.IN_TIMEOUT;
                    reply.Tid = System.currentTimeMillis();
                    reply.Description = SoapError.getDesc(SoapError.IN_TIMEOUT);
                    log.add("error", reply.Error + "-" + SoapError.getDesc(reply.Error));
//                    log.writeLog();
                    callback.handle(reply);
                    return;
                }

                Buffer buf = result.result().body();
                final MomoMessage replyMessage = MomoMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(replyMessage.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                Response requestObj = new Response();

                if (rpl != null) {
                    requestObj.Tid = rpl.getTid();
                    requestObj.Error = rpl.getErrorCode();
                    requestObj.Description = (rpl.getDescription() == null ? "null" : rpl.getDescription());
                    if (rpl.getParamsCount() > 0) {
                        for (int i = 0; i < rpl.getParamsCount(); i++) {
                            KeyValue kv = new KeyValue();
                            kv.Key = rpl.getParams(i).getKey();
                            kv.Value = rpl.getParams(i).getValue();
                            requestObj.KeyValueList.add(kv);
                        }
                    }
                }
                log.add("result from core here", "************************");
                log.add("tranid", requestObj.Tid);
                log.add("error", requestObj.Error);
                log.add("errorDesc", SoapError.getDesc(requestObj.Error));
                log.add("core desc", requestObj.Description);

                callback.handle(requestObj);
                return;

            }
        });
    }

    public static void momoToVoucher(final Vertx vertx
            , Logger logger
            , String phone
            , String pin
            , long amount
            , long timeOut
            , final Handler<Response> callback) {

        JsonObject json = new JsonObject();
        json.putString(Structure.TYPE, Command.MOMO_MONEY_TO_VOUCHER);
        json.putString(Structure.INITIATOR, phone);
        json.putString(Structure.AGENT_PIN, pin);
        json.putNumber(Structure.TRAN_AMOUNT, amount);

        final Common.BuildLog log = new Common.BuildLog(logger, phone, "momoToVoucher");

        vertx.eventBus().sendWithTimeout(AppConstant.CoreConnectorVerticle_ADDRESS, json, timeOut, new Handler<AsyncResult<Message<Buffer>>>() {
            @Override
            public void handle(AsyncResult<Message<Buffer>> result) {
                Response reply;
                if (result.succeeded()) {
                    reply = Response.parse(result.result().body(), log);
                } else {
                    reply = new Response();
                    reply.Error = SoapError.IN_TIMEOUT;
                    reply.Tid = System.currentTimeMillis();
                    reply.Description = SoapError.getDesc(SoapError.IN_TIMEOUT);
                }
                log.add("error", reply.Error + "-" + SoapError.getDesc(reply.Error));
                log.writeLog();
                callback.handle(reply);
            }
        });
    }

    public static void voucherToPoint(final Vertx vertx, Logger logger, String phone, String pin, long amount, long timeOut, final Handler<Response> callback) {
        JsonObject json = new JsonObject();
        json.putString(Structure.TYPE, Command.VOUCHER_TOPOINT);
        json.putString(Structure.INITIATOR, phone);
        json.putString(Structure.AGENT_PIN, pin);
        json.putNumber(Structure.TRAN_AMOUNT, amount);

        final Common.BuildLog log = new Common.BuildLog(logger, phone, "voucherToPoint");
        log.add("call", "core");
//        log.writeLog();

        vertx.eventBus().sendWithTimeout(AppConstant.CoreConnectorVerticle_ADDRESS, json, timeOut, new Handler<AsyncResult<Message<Buffer>>>() {
            @Override
            public void handle(AsyncResult<Message<Buffer>> result) {
                Response reply;
                if (result.succeeded()) {
                    reply = Response.parse(result.result().body(), log);
                } else {
                    reply = new Response();
                    reply.Error = SoapError.IN_TIMEOUT;
                    reply.Tid = System.currentTimeMillis();
                    reply.Description = SoapError.getDesc(SoapError.IN_TIMEOUT);
                }
                log.add("error", reply.Error + "-" + SoapError.getDesc(reply.Error));
                log.writeLog();
                callback.handle(reply);
            }
        });
    }

//    public static void topUpWithVoucher(Vertx vertx, int phone, long voucher, long point, long tranAmount, int topUpPhone, final CoreCommon.BuildLog logger, final Handler<Response> callback) {
////        JsonObject jo = new JsonObject();
////        jo.putString(Structure.TYPE, Command.TOPUP_WITH_VOUHER);
////        Core.GenericRquest request = Core.GenericRquest.newBuilder()
////                .setInitiator("0" + phone)
////                .setSms("TRANWVC vnp " + voucher + " " + point + " " + tranAmount + " buy 0" + topUpPhone)
////                .build();
//
//        JsonObject jo = new JsonObject();
////        String sms = "TRANWP " + pin + " vnp " + point + " " + point + " " + tranAmount + " buy 0" + topUpPhone + " target";
//        String sms = "TRANWVC vnp " + voucher + " " + point + " " + tranAmount + " buy 0" + topUpPhone;
//
//        jo.putString(Structure.TYPE, Command.TOPUP_WITH_POINT);
//        jo.putString(Structure.INDICATOR, "0" + phone);
//        jo.putString(Structure.SMS, sms);
//
//        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, jo, new Handler<CoreMessage<Buffer>>() {
//            @Override
//            public void handle(CoreMessage<Buffer> event) {
//                Response reply = Response.parse(event.body(), logger);
//                callback.handle(reply);
//            }
//        });
//    }


    public static void transferWithLock(final Vertx vertx
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final int walletType
            , final String fromPhone
            , final String pin
            , final String toPhone
            , final long amount
            , final String middleAgent
            , final String m2mType
            , final ArrayList<KeyValue> listKeyValues
            , final Handler<Response> callback) {
        Request lockObj = new Request();

        lockObj.TYPE = Command.TRANSFER_WITH_LOCK;
        lockObj.SENDER_PIN = pin;
        lockObj.TRAN_AMOUNT = amount;
        lockObj.SENDER_NUM = fromPhone;
        lockObj.RECVER_NUM = toPhone;
        lockObj.TARGET = middleAgent;
        lockObj.WALLET = walletType;
        lockObj.KeyValueList = new ArrayList<>();
        lockObj.KeyValueList.add(new KeyValue(Const.CoreVC.M2mTpype, m2mType));
        lockObj.KeyValueList.add(new KeyValue(Const.CoreVC.Recipient, toPhone));
        lockObj.KeyValueList.add(Misc.getIsSmsKeyValue());
        lockObj.KeyValueList.add(Misc.getClientBackendKeyValue());
        lockObj.PHONE_NUMBER = fromPhone;
        lockObj.TIME = System.currentTimeMillis();

        if (listKeyValues != null && listKeyValues.size() > 0) {
            for (int i = 0; i < listKeyValues.size(); i++) {
                lockObj.KeyValueList.add(listKeyValues.get(i));
            }
        }

        final JsonObject lockJo = lockObj.toJsonObject();

        log.add("function", "CoreCommom.transferWithLock");
        log.add("json request", lockJo.encodePrettily());

        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, lockJo, new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                Buffer buf = message.body();
                final MomoMessage replyMessage = MomoMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(replyMessage.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                Response requestObj = new Response();

                if (rpl != null) {
                    requestObj.Tid = rpl.getTid();
                    requestObj.Error = rpl.getErrorCode();
                    requestObj.Description = (rpl.getDescription() == null ? "null" : rpl.getDescription());
                    if (rpl.getParamsCount() > 0) {
                        for (int i = 0; i < rpl.getParamsCount(); i++) {
                            KeyValue kv = new KeyValue();
                            kv.Key = rpl.getParams(i).getKey();
                            kv.Value = rpl.getParams(i).getValue();
                            requestObj.KeyValueList.add(kv);
                        }
                    }
                }
                log.add("result from core here", "************************");
                log.add("tranid", requestObj.Tid);
                log.add("error", requestObj.Error);
                log.add("errorDesc", SoapError.getDesc(requestObj.Error));
                log.add("core desc", requestObj.Description);

                callback.handle(requestObj);
            }
        });
    }

    public static void topUp(final Vertx _vertx
            , final int senderNumber
            , final String senderPin
            , final long amount
            , final int wallet
            , final int recieNumber
            , final String target
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final Handler<Response> callback) {

        Request reqTopup = new Request();
        reqTopup.TYPE = Command.TOPUP;
        reqTopup.SENDER_NUM = "0" + senderNumber;
        reqTopup.SENDER_PIN = senderPin;
        reqTopup.RECVER_NUM = "0" + recieNumber;
        reqTopup.TRAN_AMOUNT = amount;
        reqTopup.WALLET = wallet;
        reqTopup.TARGET = target;
        reqTopup.KeyValueList = new ArrayList<>();
        reqTopup.KeyValueList.add(new KeyValue(Const.CoreVC.Account, "0" + recieNumber));
        reqTopup.KeyValueList.add(new KeyValue(Const.CoreVC.Recipient, "0" + recieNumber));
        reqTopup.KeyValueList.add(Misc.getIsSmsKeyValue());
        reqTopup.KeyValueList.add(Misc.getClientBackendKeyValue());

        log.add("func", "CoreCommon.topup");
        log.add("request json", reqTopup.toJsonObject().encodePrettily());

        _vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, reqTopup.toJsonObject(), new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                Buffer buf = message.body();
                final MomoMessage replyMessage = MomoMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(replyMessage.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                Response requestObj = new Response();

                if (rpl != null) {
                    requestObj.Tid = rpl.getTid();
                    requestObj.Error = rpl.getErrorCode();
                    requestObj.Description = (rpl.getDescription() == null ? "null" : rpl.getDescription());
                    if (rpl.getParamsCount() > 0) {
                        for (int i = 0; i < rpl.getParamsCount(); i++) {
                            KeyValue kv = new KeyValue();
                            kv.Key = rpl.getParams(i).getKey();
                            kv.Value = rpl.getParams(i).getValue();
                            requestObj.KeyValueList.add(kv);
                        }
                    }
                }
                callback.handle(requestObj);
                log.add("result from core here", "************************");
                log.add("tranid", requestObj.Tid);
                log.add("error", requestObj.Error);
                log.add("errorDesc", SoapError.getDesc(requestObj.Error));
                log.add("core desc", requestObj.Description);
            }
        });
    }

    /*_type.equalsIgnoreCase(Command.VOTE)){
        String sender_num   = reqObj.SENDER_NUM;
        String sender_pin   = reqObj.SENDER_PIN;
        String recver_num   = reqObj.RECVER_NUM;//so dien thoai hoac ma hoa don
        long tran_amount    = reqObj.TRAN_AMOUNT;//gia tri giao dich

        msgType = Core.MsgType.GENERIC_REQUEST_VALUE;
        msgBody = Core.GenericRquest.newBuilder()
                .setInitiator(INITIATOR_ACC)
                .setSms("MOMOADJUST " + INITIATOR_PIN + " 0" + sender_num +" 0" + recver_num + " " + tran_amount + " cdhh voting")
                .build().toByteArray();*//*

        */
    public static void voteVent(final Vertx _vertx
            , final String senderNumber
            , final String senderPin
            , final long amount
            , final String recieNumber
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final Handler<Response> callback) {

        Request reqTopup = new Request();
        reqTopup.TYPE = Command.VOTE;
        reqTopup.SENDER_NUM = senderNumber;
        reqTopup.SENDER_PIN = senderPin;
        reqTopup.RECVER_NUM = recieNumber;
        reqTopup.TRAN_AMOUNT = amount;

        log.add("function", "CoreCommon.voteVent");
        log.add("request json", reqTopup.toJsonObject().encodePrettily());

        _vertx.eventBus().send(AppConstant.CoreConnectorCDHHVerticle_ADDRESS, reqTopup.toJsonObject(), new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                Buffer buf = message.body();
                final MomoMessage replyMessage = MomoMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(replyMessage.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                Response requestObj = new Response();

                if (rpl != null) {
                    requestObj.Tid = rpl.getTid();
                    requestObj.Error = rpl.getErrorCode();
                    requestObj.Description = (rpl.getDescription() == null ? "null" : rpl.getDescription());
                    if (rpl.getParamsCount() > 0) {
                        for (int i = 0; i < rpl.getParamsCount(); i++) {
                            KeyValue kv = new KeyValue();
                            kv.Key = rpl.getParams(i).getKey();
                            kv.Value = rpl.getParams(i).getValue();
                            requestObj.KeyValueList.add(kv);
                        }
                    }
                }
                callback.handle(requestObj);
                log.add("result from core here", "************************");
                log.add("tranid", requestObj.Tid);
                log.add("error", requestObj.Error);
                log.add("errorDesc", SoapError.getDesc(requestObj.Error));
                log.add("core desc", requestObj.Description);
            }
        });
    }


/*


    /*Core.GenericRquest gReq = Core.GenericRquest.newBuilder()
            .setInitiator(initiator)
            .setSms("MMONEYTOMMONEY "+pin+" "+target+" "+amount + " cdhh")
            .build();*/

    /*public static void transferCDHH(final Vertx _vertx
            , final CoreCommon.BuildLog log
            , int phone
            , String pin
            , long amount
            , String target
            , final Handler<Response> callback) {
        JsonObject jo = new JsonObject();
        log.add("function","transferCDHH");
        log.add("amount", amount);
        log.add("target", target);
        log.add("pin len",pin.length());
        log.add("phone","0" + phone);

        String sms = "MMONEYTOMMONEY "+pin+" "+target+" "+amount + " cdhh";

        jo.putString(Structure.TYPE, Command.TRAN_WITH_VOUCHER_AND_POINT);
        jo.putString(Structure.INITIATOR, "0" + phone);
        jo.putString(Structure.SMS, sms);
        log.add("json request", jo.encodePrettily());

        _vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, jo, new Handler<CoreMessage<Buffer>>() {
            @Override
            public void handle(CoreMessage<Buffer> event) {
                Buffer buf = event.body();
                final MomoMessage replyMessage = MomoMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(replyMessage.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                Response coreReplyObj = new Response();

                if(rpl!= null){
                    coreReplyObj.Tid = rpl.getTid();
                    coreReplyObj.Error = rpl.getErrorCode();
                    coreReplyObj.Description = (rpl.getDescription() == null ? "null" : rpl.getDescription());
                    if(rpl.getParamsCount() > 0){
                        for (int i=0;i< rpl.getParamsCount();i ++){
                            KeyValue kv = new KeyValue();
                            kv.Key = rpl.getParams(i).getKey();
                            kv.Value = rpl.getParams(i).getValue();
                            coreReplyObj.KeyValueList.add(kv);
                        }
                    }
                }
                log.add("result from core here","************************");
                log.add("tranid", coreReplyObj.Tid);
                log.add("error", coreReplyObj.Error);
                log.add("errorDesc", SoapError.getDesc(coreReplyObj.Error));
                log.add("core desc", coreReplyObj.Description);

                callback.handle(coreReplyObj);
            }
        });
        //"TRANWP vnp " + point + " " + point + " " + tranAmount + " " + tranType + " " + topupPhone
    }*/


}
