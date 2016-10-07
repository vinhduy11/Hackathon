package com.mservice.momo.vertx.processor.transferbranch;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.TransProcess;
import com.mservice.momo.vertx.processor.TransferCommon;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.ArrayList;

/**
 * Created by concu on 4/19/14.
 */
public class M2Merchant {
    TransProcess transProcess;
    Logger logger;
    Vertx vertx;
    Common mCom;
    TransferCommon transferCommon;
    AgentsDb agentsDb;
    PhonesDb phonesDb;

    public M2Merchant(Vertx vertx, Logger logger, JsonObject glbCfg) {
        this.logger = logger;
        this.vertx = vertx;
        transProcess = new TransProcess(vertx, logger, glbCfg);
        mCom = new Common(vertx, logger, glbCfg);
        transferCommon = new TransferCommon(vertx, logger, glbCfg);
        agentsDb = new AgentsDb(vertx.eventBus(), logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
    }


    public void doM2Merchant(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {

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
                transProcess.processM2MerchantTransferViaRedis(sock, msg, data, null, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                doM2MerchantFromBanknet(sock, msg, data, true, callback);
                break;
            //nap tien tan noi
            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                doM2MerchantFromBankLinked(sock, msg, data, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.PAY123_VALUE:
                transProcess.processM2MerchantTransferViaRedis(sock, msg, data, null, callback);
                break;
            case MomoProto.TranHisV1.SourceFrom.MASTERCARD_VALUE:
                transProcess.processM2MerchantTransferViaRedis(sock, msg, data, null, callback);
//                doM2MFromVisaMaster(sock, msg, data, true, callback);
//                transProcess.doSthFromVisaMaster(vertx, msg, sock, data, logger, true, request, callback);
                break;

            default:
                logger.info("Transfer M2m not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(request.getSourceFrom()).name());
                break;
        }
    }

    private void doM2MerchantFromBanknet(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
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

                transferCommon.doVerifyBanknet(sock, msg, data
                        , bnObj, nextstep, new Handler<FromSource.Obj>() {
                    @Override
                    public void handle(FromSource.Obj fsObj) {

                        if (fsObj.Result) {
                            ArrayList<SoapProto.keyValuePair> keyValuePairs = Misc.addKeyValuePair(null, Const.SERVICE_NAME, fsObj.ServiceSource);
                            transProcess.processM2MerchantTransferViaRedis(sock, msg, data, keyValuePairs, callback);
                        }
                    }
                }, callback);

            }
        });

    }

    private void doM2MerchantFromBankLinked(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final boolean nextstep
            , final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        /*BankIn
        from_bank	-->	partner_ref
	    bank_code	-->	partner_code
	    amount	-->	amount
        */

        if (request == null || !request.hasAmount() || !request.hasPartnerId()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        final TransferCommon.BankLinkedObj blObj = new TransferCommon.BankLinkedObj();
        String bankCode = (request.getPartnerCode() == null || request.getPartnerId().isEmpty() ? "" : request.getPartnerCode());
        bankCode = "".equalsIgnoreCase(bankCode) ? ((request.getPartnerExtra1() == null || request.getPartnerExtra1().isEmpty()) ? bankCode : request.getPartnerExtra1()) : bankCode;

        blObj.amount = request.getAmount();
        blObj.bank_code = bankCode;
        blObj.from_bank = (request.getPartnerRef() == null ? "" : request.getPartnerRef());
        final MomoProto.TranHisV1 frequest = request;
        final String receiverNumber = request.getPartnerId();
        logger.info("receiver number is " + request.getPartnerId());
        phonesDb.getPhoneObjInfo(DataUtil.strToInt(request.getPartnerId()), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj phoneObj) {
                if (phoneObj != null) {
                    Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE, blObj.amount, "0" + msg.cmdPhone, receiverNumber, new Handler<FeeDb.Obj>() {
                        @Override
                        public void handle(FeeDb.Obj obj) {
                            int m2mFee = obj != null ? obj.STATIC_FEE : 1000;
                            blObj.amount = blObj.amount + m2mFee;

                            transferCommon.doBankIn(frequest, sock, msg, data, blObj, nextstep, new Handler<FromSource.Obj>() {
                                @Override
                                public void handle(FromSource.Obj fsObj) {

                                    if (fsObj.Result) {
                                        ArrayList<SoapProto.keyValuePair> keyValuePairs = Misc.addKeyValuePair(null, Const.SERVICE_NAME, fsObj.ServiceSource);
                                        transProcess.processM2MerchantTransferViaRedis(sock, msg, data, keyValuePairs, callback);
                                    }
                                }
                            }, callback);
                        }
                    });
                    return;
                }
                transferCommon.doBankIn(frequest, sock, msg, data, blObj, nextstep, new Handler<FromSource.Obj>() {
                    @Override
                    public void handle(FromSource.Obj fsObj) {

                        if (fsObj.Result) {
                            ArrayList<SoapProto.keyValuePair> keyValuePairs = Misc.addKeyValuePair(null, Const.SERVICE_NAME, fsObj.ServiceSource);
                            transProcess.processM2MerchantTransferViaRedis(sock, msg, data, keyValuePairs, callback);
                        }
                    }
                }, callback);

                return;
            }
        });
    }


}
