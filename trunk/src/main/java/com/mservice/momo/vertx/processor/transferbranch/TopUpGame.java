package com.mservice.momo.vertx.processor.transferbranch;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
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

/**
 * Created by concu on 4/24/14.
 */
public class TopUpGame {

    TransProcess transProcess;
    Logger logger;
    Vertx vertx;
    Common mCom;
    TransferCommon transferCommon;

    public TopUpGame(Vertx vertx, Logger logger, JsonObject glbCfg) {
        this.logger = logger;
        this.vertx = vertx;
        transProcess = new TransProcess(vertx, logger, glbCfg);
        mCom = new Common(vertx, logger, glbCfg);
        transferCommon = new TransferCommon(vertx, logger, glbCfg);
    }

    public void doTopUpGame(final NetSocket sock, final MomoMessage momoOrgMsg, final SockData data, final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(momoOrgMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        switch (request.getSourceFrom()) {
            case MomoProto.TranHisV1.SourceFrom.MOMO_VALUE:
                transProcess.processTopUpGame(sock, momoOrgMsg, data, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.PAY123_VALUE:
                transProcess.processTopUpGame(sock, momoOrgMsg, data, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                doTopUpGameFromBanknet(sock, momoOrgMsg, data, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                doTopUpGameFromBankLinked(sock, momoOrgMsg, data, true, callback);
                break;

            default:
                logger.info("Transfer TopUpGame not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(request.getSourceFrom()).name());
                break;
        }
    }

    private void doTopUpGameFromBanknet(final NetSocket sock
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
	    otp	-->	partner_name

        */
        if (request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1()
                || !request.hasAmount() || !request.hasPartnerName()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        final TransferCommon.BankNetObj bnObj = new TransferCommon.BankNetObj();
        bnObj.merchant_trans_id = request.getPartnerRef();
        bnObj.trans_id = request.getPartnerExtra1();
        bnObj.amount = request.getAmount();
        bnObj.otp = request.getPartnerName();
        bnObj.tranType = request.getTranType();

        Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE, bnObj.amount, "", "",new Handler<FeeDb.Obj>() {
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
                            transProcess.processTopUpGame(sock, momoOrgMsg, data, callback);
                        }
                    }
                }, callback);
            }
        });
    }

    private void doTopUpGameFromBankLinked(final NetSocket sock
            , final MomoMessage momoOrgMsg, final SockData data
            , final boolean nextstep, final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(momoOrgMsg.cmdBody);
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

        TransferCommon.BankLinkedObj blObj = new TransferCommon.BankLinkedObj();
        blObj.amount = request.getAmount();
        blObj.bank_code = request.getPartnerExtra1();
        blObj.from_bank = request.getPartnerRef() == null ? "" : request.getPartnerRef();

        transferCommon.doBankIn(request, sock
                , momoOrgMsg
                , data
                , blObj, nextstep, new Handler<FromSource.Obj>() {
            @Override
            public void handle(FromSource.Obj b) {
                if (b.Result) {
                    transProcess.processTopUpGame(sock, momoOrgMsg, data, callback);
                }
            }
        }, callback);
    }
}
