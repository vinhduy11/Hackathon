package com.mservice.momo.vertx.processor.transferbranch;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.external.vng.VngUtil;
import com.mservice.momo.gateway.external.vng.vngClass;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.Phim123Errors;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.TransProcess;
import com.mservice.momo.vertx.processor.TransferCommon;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

/**
 * Created by concu on 6/24/14.
 */
public class Vinagame123Phim {
    TransProcess transProcess;
    JsonObject jsonOcbPromo;
    private Logger logger;
    private Vertx vertx;
    private Common mCom;
    private TransferCommon transferCommon;
    // used with point and voucher
    private long TRANWITHPOINT_MIN_POINT = 0;
    private long TRANWITHPOINT_MIN_AMOUNT = 0;
    private GiftManager giftManager;
    private int vietCombankMin = 0;
    private int vietTinBankMin = 0;

    public Vinagame123Phim(Vertx vertx, Logger logger, JsonObject glbCfg) {
        this.logger = logger;
        this.vertx = vertx;
        this.transProcess = new TransProcess(vertx, logger, glbCfg);
        this.mCom = new Common(vertx, logger, glbCfg);
        this.transferCommon = new TransferCommon(vertx, logger, glbCfg);

        JsonObject pointConfig = glbCfg.getObject("point", new JsonObject());
        TRANWITHPOINT_MIN_POINT = pointConfig.getLong("minPoint", 0);
        TRANWITHPOINT_MIN_AMOUNT = pointConfig.getLong("mintAmount", 0);
        giftManager = new GiftManager(vertx, logger, glbCfg);

        JsonObject bankCashInJson = glbCfg.getObject("bankcashin", null);
        if (bankCashInJson != null) {
            vietCombankMin = bankCashInJson.getInteger("vietcombankmin", 0);
            vietTinBankMin = bankCashInJson.getInteger("viettinbankmin", 0);
        }
        jsonOcbPromo = glbCfg.getObject(StringConstUtil.OBCPromo.JSON_OBJECT, new JsonObject());
    }

    public static void cancel123Phim(final Vertx _vertx
            , final String invoice_no
            , final Logger logger
            , final int phoneNumber
            , final Handler<VngUtil.ObjReply> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "cancel123Phim");
        log.add("invoice_no", invoice_no);

        VngUtil.ObjConfirmOrCancel cancel = new VngUtil.ObjConfirmOrCancel();
        cancel.invoice_no = invoice_no;
        JsonObject jsoncancelReq = VngUtil.getJsonCancel(cancel);

        jsoncancelReq.putString(vngClass.PHONE_NUMBER, "0" + phoneNumber);
        jsoncancelReq.putNumber(vngClass.TIME, log.getTime());

        _vertx.eventBus().sendWithTimeout(AppConstant.VinaGameCinemaVerticle_ADDRESS
                , jsoncancelReq, 40000, new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> jcancelrpl) {

                VngUtil.ObjReply objReply = null;

                final JsonObject jsonCancel = new JsonObject();

                if (jcancelrpl.succeeded()) {

                    JsonObject json = jcancelrpl.result().body();
                    objReply = new VngUtil.ObjReply(json);

                    log.add("date_cancel", objReply.date_cancel);
                    log.add("error", objReply.error);
                    log.add("desc", objReply.desc);
                    log.add("errdesc", Phim123Errors.getDesc(objReply.error));

                    callback.handle(objReply);
                    return;
                }

                if (jcancelrpl.failed()) {
                    jsonCancel.putNumber(vngClass.Res.error, 5008);
                    jsonCancel.putString(vngClass.Res.desc, "123phim orderCancel with timeout");

                } else {

                    jsonCancel.putNumber(vngClass.Res.error, 5008);
                    jsonCancel.putString(vngClass.Res.desc, "123phim orderCancel with unfinited error");

                }
                objReply = new VngUtil.ObjReply(jsonCancel);

                log.writeLog();

                callback.handle(objReply);

            }
        });
    }

    public void do123Phim(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasAmount()) {
            mCom.writeErrorToSocket(sock);
            return;
        }
        /*partnerId	enum phim123{CANCEL,CONFIRM}
        amount	amount
        billId	invoice_no
        */

        //neu nguon tu bank_net : ticket_code	 = lastIndex of partnerName, seperated by BELL
        //nguon tu momo va bank lien ket : ticket_code = partnerCode
        String tcode = "";
        if (request.getSourceFrom() == MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE) {
            String[] ar = request.getPartnerName().split(MomoMessage.BELL);
            tcode = (ar.length > 0 ? ar[ar.length - 1] : "null");
        } else {
            tcode = (request.getPartnerCode() == null ? "null" : request.getPartnerCode());
        }

        final String ticket_code = tcode;
        final String invoice_no = request.getBillId().trim();

        int action = DataUtil.strToInt(request.getPartnerId());
        if (action == MomoProto.phim123.CANCEL_VALUE) {
            cancel123Phim(vertx, invoice_no, logger, msg.cmdPhone, new Handler<VngUtil.ObjReply>() {
                @Override
                public void handle(VngUtil.ObjReply objReply) {
                    logger.info("cancel123Phim error " + objReply.error);
                }
            });
            return;
        }

        final MomoProto.TranHisV1 frequest = request;

        switch (frequest.getSourceFrom()) {
            case MomoProto.TranHisV1.SourceFrom.MOMO_VALUE:
                transProcess.processPayment123Phim(sock
                        , msg
                        , data
                        , invoice_no
                        , ticket_code
                        , callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.PAY123_VALUE:
                transProcess.processPayment123Phim(sock
                        , msg
                        , data
                        , invoice_no
                        , ticket_code
                        , callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                do123PhimFromBankLinked(sock
                        , msg
                        , data
                        , invoice_no
                        , ticket_code
                        , true
                        , callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                do123PhimFromBanknet(sock, msg, data, invoice_no, ticket_code, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.MASTERCARD_VALUE:
                transProcess.processPayment123Phim(sock
                        , msg
                        , data
                        , invoice_no
                        , ticket_code
                        , callback);
                break;
//                do123PhimFromVisaMaster(sock
//                        , msg
//                        , data
//                        , true
//                        , invoice_no
//                        , ticket_code
//                        , callback);
//                transProcess.doSthFromVisaMaster(vertx, msg, sock, data, logger, true, request, callback);
            default:
                logger.info("Transfer TopUp not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(frequest.getSourceFrom()).name());
                break;
        }
    }

    private void do123PhimFromBankLinked(final NetSocket sock
            , final MomoMessage msg, final SockData data
            , final String invoice_no
            , final String ticket_code
            , final boolean nextstep, final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }
        /*from_bank	-->	partner_ref
        bank_code	-->	PartnerExtra_1
        amount	-->	amount
        ticket_code	-->	partnerCode
        */


        if (request == null || !request.hasAmount() || !request.hasPartnerExtra1()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        final MomoProto.TranHisV1 fRequest = request;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "do123PhimFromBankLinked");
        log.add("sid", "123phim");

        TransferWithGiftContext.build(msg.cmdPhone,
                "123phim",
                "",
                request.getAmount(),
                vertx,
                giftManager,
                data,
                TRANWITHPOINT_MIN_POINT,
                TRANWITHPOINT_MIN_AMOUNT,
                logger, new Handler<TransferWithGiftContext>() {
                    @Override
                    public void handle(TransferWithGiftContext context) {

                        //not used momo value
                        log.add("point", context.point);
                        log.add("voucher", context.voucher);
                        log.add("tranAmount", context.amount);
                        log.add("momoAmout", context.momo);

                        //no need to do bank cash in
                        if (context.momo <= 0) {
                            transProcess.processPayment123Phim(sock
                                    , msg
                                    , data
                                    , invoice_no
                                    , ticket_code
                                    , callback);

                            log.add("note", "no use cash from momo, no need to do bank cash in");
                            log.writeLog();
                            return;
                        }

                        long cashInAmt = Math.max(context.momo, vietCombankMin);

                        //need to do bank-in with momo amount not transaction amount
                        final TransferCommon.BankLinkedObj blObj = new TransferCommon.BankLinkedObj();
                        blObj.amount = cashInAmt;
                        blObj.bank_code = fRequest.getPartnerExtra1();
                        blObj.from_bank = fRequest.getPartnerRef() == null ? "" : fRequest.getPartnerRef();

                        transferCommon.doBankIn(fRequest, sock
                                , msg
                                , data
                                , blObj, nextstep, new Handler<FromSource.Obj>() {
                            @Override
                            public void handle(FromSource.Obj b) {
                                if (b.Result) {
                                    JsonObject jsonRes = new JsonObject(b.ServiceSource);
                                    long tran_id = jsonRes.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                    data.bank_amount = blObj.amount;
                                    data.bank_code = blObj.bank_code;
                                    data.bank_tid = tran_id;
                                    transProcess.processPayment123Phim(sock
                                            , msg
                                            , data
                                            , invoice_no
                                            , ticket_code
                                            , new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
//
                                            log.add("JsonObject rep Topup", jsonObject);
                                            int error = jsonObject.getInteger("error", -1);

                                            if (error == 0) {
                                                // Kiem tra phai thanh toan tu OCB khong
                                                String bank_code = jsonOcbPromo.getString(StringConstUtil.OBCPromo.BANK_CODE, "104");
                                                if (blObj.bank_code.equalsIgnoreCase(bank_code)) {
                                                    //Kiem tra tra thuong OCB
                                                    int tranType = jsonObject.getInteger(colName.TranDBCols.TRAN_TYPE, 0);
                                                    long tranId = jsonObject.getLong(colName.TranDBCols.TRAN_ID, 0);
                                                    String serviceId = jsonObject.getString(StringConstUtil.SERVICE, "");
                                                    log.add("tranType", tranType);
                                                    log.add("tranId", tranId);
                                                    log.add("serviceId", serviceId);
//                                                    BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, tranType, tranId, serviceId, StringConstUtil.OBCPromo.OCB,
//                                                            new Handler<JsonObject>() {
//                                                                @Override
//                                                                public void handle(JsonObject jsonObject) {
//                                                                    log.add("result from bill pay promo", jsonObject);
//                                                                    log.writeLog();
//                                                                }
//                                                            }
//                                                    );

                                                }
                                            } else if (!jsonObject.containsField("error")) {
                                                log.add("desc", "Khong chua field error");
                                                callback.handle(jsonObject);
                                            } else {
                                                log.add("Khong duoc tra thuong roi ", msg.cmdPhone);
                                                log.add("error ", error);
                                            }
                                            log.writeLog();
                                        }
                                    });
                                }
                            }
                        }, callback);
                    }
                }
        );
    }

    private void do123PhimFromBanknet(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final String invoice_no
            , final String ticket_code
            , final boolean nextstep, final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        /*
        BanknetVerifyOtp
        merchant_trans_id	-->	partner_ref
        trans_id	-->	PartnerExtra_1
        amount	-->	amount
        otp	-->	partner_code
        */
        if (request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1()
                || !request.hasAmount() || !request.hasPartnerId()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        final TransferCommon.BankNetObj bnObj = new TransferCommon.BankNetObj();
        bnObj.merchant_trans_id = request.getPartnerRef();
        bnObj.trans_id = request.getPartnerExtra1();
        bnObj.amount = request.getAmount();
        bnObj.otp = request.getPartnerCode();
        bnObj.tranType = request.getTranType();

        Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE, bnObj.amount, "", "",new Handler<FeeDb.Obj>() {
            @Override
            public void handle(FeeDb.Obj obj) {
                int m2mFee = obj == null ? 1000 : obj.STATIC_FEE;
                //do co phi M2M = 1000
                bnObj.amount = bnObj.amount + m2mFee;

                transferCommon.doVerifyBanknet(sock, msg, data
                        , bnObj, nextstep, new Handler<FromSource.Obj>() {
                    @Override
                    public void handle(FromSource.Obj b) {
                        if (b.Result) {
                            transProcess.processPayment123Phim(sock, msg, data, invoice_no, ticket_code, callback);
                        }
                    }
                }, callback);
            }
        });
    }

//    private void do123PhimFromVisaMaster(final NetSocket sock
//            , final MomoMessage msg
//            , final SockData data
//            , final boolean nextstep
//            , final String invoice_no
//            , final String ticket_code
//            , final Handler<JsonObject> webCallback) {
//
//        final Common.BuildLog log = new Common.BuildLog(logger);
//
//        MomoProto.TranHisV1 request;
//        request = TranHisUtils.getRequest(msg, logger);
//
//        if (request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1()
//                || !request.hasAmount() || !request.hasPartnerId()) {
//            mCom.writeErrorToSocket(sock);
//            return;
//        }
//
//        log.add("func", "do123PhimFromVisaMaster");
//        log.add("cmdInd", msg.cmdIndex);
//        log.add("phone", msg.cmdPhone);
//        log.add("type", msg.cmdType);
//        log.add("invoice_no: ", invoice_no);
//        log.add("ticket_code: ", ticket_code);
//
//        //tinh toan so luong voucher, point va tien momo su dung trong giao dich
//        TransferWithGiftContext.build(msg.cmdPhone,
//                StringConstUtil.ONE_TWO_THREE_FILM,
//                "",
//                request.getAmount(),
//                vertx,
//                giftManager,
//                data,
//                TRANWITHPOINT_MIN_POINT,
//                TRANWITHPOINT_MIN_AMOUNT,
//                logger, new Handler<TransferWithGiftContext>() {
//                    @Override
//                    public void handle(TransferWithGiftContext context) {
//                        //not used momo value
//                        log.add("point", context.point); // so point su dung
//                        log.add("voucher", context.voucher); // so voucher su dung
//                        log.add("tranAmount", context.amount); // gia tri giao dich
//                        log.add("momo", context.momo); // tien momo thuc te sau khi tru voucher - point
//
//                        //no need to do cash in through visa-master
//                        if (context.momo <= 0) {
//                            transProcess.processPayment123Phim(sock, msg, data, invoice_no, ticket_code, webCallback);
//                            log.add("note", "no use cash from momo, no need to do bank cash in");
//                            log.writeLog();
//                            return;
//                        }
//
//                        //long cashInAmt = Math.max(context.momo, vietCombankMin);
//                        long cashInAmt = context.momo;
//                        JsonObject paraInfo = new JsonObject();
//                        paraInfo.putNumber("amount", cashInAmt);
//
//                        transProcess.processVisaMasterCashIn(sock, msg, data, nextstep, paraInfo, new Handler<FromSource.Obj>() {
//                            @Override
//                            public void handle(FromSource.Obj obj) {
//                                if (obj.Result) {
//                                    log.add("obj.Result", "success");
//                                    transProcess.processPayment123Phim(sock, msg, data, invoice_no, ticket_code, webCallback);
//                                    log.writeLog();
//                                }
//                            }
//                        }, webCallback);
//                    }
//                });
//
//    }
}
