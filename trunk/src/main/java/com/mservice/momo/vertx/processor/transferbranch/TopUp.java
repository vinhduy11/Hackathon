package com.mservice.momo.vertx.processor.transferbranch;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.TransProcess;
import com.mservice.momo.vertx.processor.TransferCommon;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

/**
 * Created by concu on 4/19/14.
 */
public class TopUp {
    TransProcess transProcess;
    Logger logger;
    Vertx vertx;
    Common mCom;
    TransferCommon transferCommon;

    // used with point and voucher
    long TRANWITHPOINT_MIN_POINT = 0;
    long TRANWITHPOINT_MIN_AMOUNT = 0;
    GiftManager giftManager;

    private int vietCombankMin = 0;
    private int vietTinBankMin = 0;
    private JsonObject jsonOcbPromo;

    public TopUp(Vertx vertx, Logger logger, JsonObject glbCfg) {
        this.vertx = vertx;
        this.logger = logger;
        transProcess = new TransProcess(vertx, logger, glbCfg);
        mCom = new Common(vertx, logger, glbCfg);
        transferCommon = new TransferCommon(vertx, logger, glbCfg);

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

    public void doTopUp(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasPartnerCode() || !request.hasAmount()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        switch (request.getSourceFrom()) {
            case MomoProto.TranHisV1.SourceFrom.MOMO_VALUE:
                transProcess.processTopUp(sock, msg, data, callback);
                break;
            case MomoProto.TranHisV1.SourceFrom.PAY123_VALUE:
                transProcess.processTopUp(sock, msg, data, callback);
                break;
            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                doTopUpFromBankLinked(sock, msg, data, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                doTopUpFromBanknet(sock, msg, data, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.MASTERCARD_VALUE:
                transProcess.processTopUp(sock, msg, data, callback);
//                doTopUpFromVisaMaster(sock, msg, data, true, callback);
//                transProcess.doSthFromVisaMaster(vertx, msg, sock, data, logger, true, request, callback);
                break;


            default:
                logger.info("Transfer TopUp not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(request.getSourceFrom()).name());
                break;
        }
    }

    private void doTopUpFromBankLinked(final NetSocket sock
            , final MomoMessage msg, final SockData data
            , final boolean nextstep, final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }
        /*from_bank	-->	partner_ref
        bank_code	-->	partner_id
        amount	-->	amount
        */

        if (request == null || !request.hasAmount() || !request.hasPartnerId()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        final MomoProto.TranHisV1 fRequest = request;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "doTopUpFromBankLinked");
        log.add("sid", "topup");

        TransferWithGiftContext.build(msg.cmdPhone,
                "topup",
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
                        log.add("momo", context.momo);

                        //no need to do bank cash in
                        if (context.momo <= 0) {
                            transProcess.processTopUp(sock, msg, data, callback);
                            log.add("note", "no use cash from momo, no need to do bank cash in");
                            log.writeLog();
                            return;
                        }

                        final long cashInAmt = Math.max(context.momo, vietCombankMin);

                        //need to do bank-in with momo amount not transaction amount
                        final TransferCommon.BankLinkedObj blObj = new TransferCommon.BankLinkedObj();
                        blObj.amount = cashInAmt;
                        blObj.bank_code = fRequest.getPartnerId();
                        blObj.from_bank = fRequest.getPartnerRef() == null ? "" : fRequest.getPartnerRef();

                        transferCommon.doBankIn(fRequest, sock
                                , msg
                                , data
                                , blObj, nextstep, new Handler<FromSource.Obj>() {
                            @Override
                            public void handle(FromSource.Obj b) {
                                if (b.Result) {
                                    //BEGIN 0000000050
//                                    transProcess.processTopUp(sock, msg, data, callback);
                                    JsonObject jsonRes = new JsonObject(b.ServiceSource);
                                    long tran_id = jsonRes.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                    data.bank_amount = blObj.amount;
                                    data.bank_code = blObj.bank_code;
                                    data.bank_tid = tran_id;
                                    transProcess.processTopUp(sock, msg, data, new Handler<JsonObject>() {
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
                                                    log.add("desc", "Kiem tra khuyen mai OCB");
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
                                    //END 0000000050
                                }
                            }
                        }, callback);
                    }
                }
        );
    }

    private void doTopUpFromBanknet(final NetSocket sock
            , final MomoMessage momoOrgMsg
            , final SockData data
            , final boolean nextstep, final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(momoOrgMsg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        /*
        BanknetVerifyOtp
        merchant_trans_id	-->	partner_ref
        trans_id	-->	PartnerExtra_1
        amount	-->	amount
        otp	-->	partner_id
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
        bnObj.otp = request.getPartnerId();
        bnObj.tranType = request.getTranType();

        Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE, bnObj.amount, "","",new Handler<FeeDb.Obj>() {
            @Override
            public void handle(FeeDb.Obj obj) {
                int m2mFee = obj == null ? 1000 : obj.STATIC_FEE;
                //do co phi M2M = 1000
                bnObj.amount = bnObj.amount + m2mFee;

                transferCommon.doVerifyBanknet(sock, momoOrgMsg, data
                        , bnObj, nextstep, new Handler<FromSource.Obj>() {
                    @Override
                    public void handle(FromSource.Obj b) {
                        if (b.Result) {
                            transProcess.processTopUp(sock, momoOrgMsg, data, callback);
                        }
                    }
                }, callback);
            }
        });
    }

//    private void doTopUpFromVisaMaster(final NetSocket sock
//            ,final MomoMessage msg
//            ,final SockData data
//            , final boolean nextstep, final Handler<JsonObject> webCallback) {
//
//
//        final Common.BuildLog log = new Common.BuildLog(logger);
//
//        MomoProto.TranHisV1 request;
//        request = TranHisUtils.getRequest(msg, logger);
//
//        if(request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1()
//                || !request.hasAmount() || !request.hasPartnerId()){
//            mCom.writeErrorToSocket(sock);
//            return;
//        }
//
//        log.add("func", "doTopUpFromVisaMaster");
//        log.add("cmdInd", msg.cmdIndex);
//        log.add("phone", msg.cmdPhone);
//        log.add("type", msg.cmdType);
//
//        //tinh toan so luong voucher, point va tien momo su dung trong giao dich
//        TransferWithGiftContext.build(msg.cmdPhone,
//                StringConstUtil.TOP_UP,
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
//                            transProcess.processTopUp(sock, msg, data, webCallback);
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
//                                    transProcess.processTopUp(sock, msg, data, webCallback);
//                                    log.writeLog();
//                                }
//                        }
//                        }, webCallback);
//                    }
//                });
//
//    }
}
