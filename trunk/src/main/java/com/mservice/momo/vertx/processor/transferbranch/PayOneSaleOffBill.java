package com.mservice.momo.vertx.processor.transferbranch;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.processor.*;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.HashMap;

/**
 * Created by concu on 8/14/15.
 */
public class PayOneSaleOffBill {

    protected long TRANWITHPOINT_MIN_POINT = 0;
    protected long TRANWITHPOINT_MIN_AMOUNT = 0;
    protected int vietCombankMin = 0;
    protected GiftManager giftManager;
    TransProcess transProcess;
    Logger logger;
    Vertx vertx;
    Common mCom;
    TransferCommon transferCommon;
    AgentsDb agentsDb;
    PhonesDb phonesDb;
    TransferProcess transferProcess;
    private int vietTinBankMin = 0;
    private JsonObject jsonConfig;

    public PayOneSaleOffBill(Vertx vertx, Logger logger, JsonObject glbCfg) {

        this.logger = logger;
        this.vertx = vertx;
        //transProcess = new TransProcess(vertx, logger, glbCfg);
        mCom = new Common(vertx, logger, glbCfg);
        transferCommon = new TransferCommon(vertx, logger, glbCfg);
        agentsDb = new AgentsDb(vertx.eventBus(), logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        //transferProcess = new TransferProcess(vertx, logger, glbCfg, ServerVerticle.MapTranRunning);
        JsonObject pointConfig = glbCfg.getObject("point", new JsonObject());
        TRANWITHPOINT_MIN_POINT = pointConfig.getLong("minPoint", 0);
        TRANWITHPOINT_MIN_AMOUNT = pointConfig.getLong("mintAmount", 0);
        giftManager = new GiftManager(vertx, logger, glbCfg);
        JsonObject bankCashInJson = glbCfg.getObject("bankcashin", null);
        if (bankCashInJson != null) {
            vietCombankMin = bankCashInJson.getInteger("vietcombankmin", 0);
            vietTinBankMin = bankCashInJson.getInteger("viettinbankmin", 0);
        }
        jsonConfig = glbCfg;
    }


    public void doPayOneSaleOffBill(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {
        transferProcess = new TransferProcess(vertx, logger, jsonConfig, ServerVerticle.MapTranRunning);
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //build org tran his
        //final MomoMessage momoTranHisMsg = TransferCommon.buildTranHisFromV1(request, momoOrgMsg);

        switch (request.getSourceFrom()) {
            case MomoProto.TranHisV1.SourceFrom.MOMO_VALUE:
                transferProcess.processPayOneSaleOffBill(sock, msg, data, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                doPayOneSaleOffBillFromBanknet(sock, msg, data, true, callback);
                break;
            //nap tien tan noi
            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                doPayOneSaleOffBillFromBankLinked(sock, msg, data, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.PAY123_VALUE:
                transferProcess.processPayOneSaleOffBill(sock, msg, data, callback);
                break;
            case MomoProto.TranHisV1.SourceFrom.MASTERCARD_VALUE:
                transferProcess.processPayOneSaleOffBill(sock, msg, data, callback);
//                doM2MFromVisaMaster(sock, msg, data, true, callback);
//                transProcess.doSthFromVisaMaster(vertx, msg, sock, data, logger, true, request, callback);
                break;

            default:
                logger.info("Transfer M2m not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(request.getSourceFrom()).name());
                break;
        }
    }

    private void doPayOneSaleOffBillFromBankLinked(final NetSocket sock
            , final MomoMessage msg, final SockData data
            , final boolean nextstep, final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        /*BankIn
        from_bank	-->	partner_ref
	    bank_code	-->	PartnerExtra_1
	    amount	-->	amount

        */

        if (request == null || !request.hasAmount() || !request.hasPartnerExtra1()) {
            mCom.writeErrorToSocket(sock);
            return;
        }
        final MomoProto.TranHisV1 fRequest = request;

        HashMap<String, String> hashMap = Misc.getKeyValuePairs(request.getKvpList());
        String tmpServiceId = (request.getPartnerId() == null || request.getPartnerId().isEmpty() ? "" : request.getPartnerId());

        tmpServiceId = "".equalsIgnoreCase(tmpServiceId) ?
                (hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : tmpServiceId)
                : tmpServiceId;

        final String serviceId = tmpServiceId;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "doPayOneSaleOffBillFromBankLinked");
        log.add("sid", serviceId);

        TransferWithGiftContext.build(msg.cmdPhone,
                serviceId,
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
                        log.add("tran amount", context.amount);
                        log.add("momo", context.momo);

                        //no need to do bank cash in
                        if (context.momo <= 0) {
                            transferProcess.processPayOneSaleOffBill(sock, msg, data, callback);
                            log.add("note", "no use cash from momo, no need to do bank cash in");
                            log.writeLog();
                            return;
                        }

                        final long cashInAmt = Math.max(context.momo, vietCombankMin);

                        //need to do bank-in with momo amount not transaction amount
                        final TransferCommon.BankLinkedObj blObj = new TransferCommon.BankLinkedObj();
                        blObj.amount = cashInAmt;
                        blObj.bank_code = fRequest.getPartnerExtra1();
                        blObj.from_bank = (fRequest.getPartnerRef() == null ? "" : fRequest.getPartnerRef());

                        transferCommon.doBankIn(fRequest, sock
                                , msg
                                , data
                                , blObj, nextstep, new Handler<FromSource.Obj>() {
                            @Override
                            public void handle(FromSource.Obj b) {
                                if (b.Result) {
                                    JsonObject jsonRepBank = new JsonObject(b.ServiceSource);
                                    data.bank_code = blObj.bank_code;
                                    data.bank_tid = jsonRepBank.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                                    data.bank_amount = cashInAmt;
                                    transferProcess.processPayOneSaleOffBill(sock, msg, data, callback);
                                }
                                else {
                                    mCom.writeErrorToSocket(sock);
                                    return;
                                }
                            }
                        }, callback);
                    }
                }
        );
    }

    private void doPayOneSaleOffBillFromBanknet(final NetSocket sock
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
        otp	-->	partner_code
        */
        if (request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1()
                || !request.hasAmount() || !request.hasPartnerCode()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        final TransferCommon.BankNetObj bnObj = new TransferCommon.BankNetObj();
        bnObj.merchant_trans_id = request.getPartnerRef();
        bnObj.trans_id = request.getPartnerExtra1();
        bnObj.amount = request.getAmount();
        bnObj.otp = request.getPartnerCode();
        bnObj.tranType = request.getTranType();

        Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE, bnObj.amount, "", "", new Handler<FeeDb.Obj>() {
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
                            transferProcess.processPayOneSaleOffBill(sock, momoOrgMsg, data, callback);
                        }
                    }
                }, callback);
            }
        });
    }
}
