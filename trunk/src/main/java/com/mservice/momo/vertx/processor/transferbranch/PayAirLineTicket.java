package com.mservice.momo.vertx.processor.transferbranch;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
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
public class PayAirLineTicket {
    TransProcess transProcess;
    Logger logger;
    Vertx vertx;
    TransferCommon transferCommon;
    private Common mCommon;

    public PayAirLineTicket(Vertx vertx, Logger logger, JsonObject glbCfg) {
        this.logger = logger;
        this.vertx = vertx;
        transProcess = new TransProcess(vertx, logger, glbCfg);
        mCommon = new Common(vertx, logger, glbCfg);
        transferCommon = new TransferCommon(vertx, logger, glbCfg);
    }

    public void doPayAirlineTicket(final NetSocket sock, final MomoMessage momoOrgMsg, final SockData data, final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(momoOrgMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        switch (request.getSourceFrom()) {
            case MomoProto.TranHisV1.SourceFrom.MOMO_VALUE:
                doPayAirlineTicketFromMomo(sock, momoOrgMsg, data, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                doPayAirlineTicketFromBankNet(sock, momoOrgMsg, data, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                doPayAirlineTicketFromBankLinked(sock, momoOrgMsg, data, true, callback);
                break;
            case MomoProto.TranHisV1.SourceFrom.PAY123_VALUE:
                doPayAirlineTicketFromMomo(sock, momoOrgMsg, data, callback);
                break;

            default:
                logger.info("PayAirLineTicket not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(request.getSourceFrom()).name());
                break;
        }
    }

    private void doPayAirlineTicketFromMomo(final NetSocket sock
            , final MomoMessage momoOrgMsg
            , final SockData data
            , final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(momoOrgMsg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        final MomoProto.TranHisV1 req = request;

        /*ProviderId	-->	partner_id
        amount	-->	amount
        billId	-->	billId
        */

        if (request == null || !request.hasPartnerId() || !request.hasBillId()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        if (request.getAmount() > 0) {
            transProcess.processPayOneBill(sock, momoOrgMsg, data, callback);
            return;
        }

    }

    private void doPayAirlineTicketFromBankLinked(final NetSocket sock
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

        if (request == null || !request.hasPartnerExtra1()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final MomoProto.TranHisV1 req = request;

        final TransferCommon.BankLinkedObj blObj = new TransferCommon.BankLinkedObj();
        blObj.amount = request.getAmount();
        blObj.bank_code = request.getPartnerExtra1();
        blObj.from_bank = (request.getPartnerRef() == null ? "" : request.getPartnerRef());

        if (request.getAmount() == 0) {


            return;
        }

        transferCommon.doBankIn(request, sock
                , momoOrgMsg
                , data
                , blObj, nextstep, new Handler<FromSource.Obj>() {
            @Override
            public void handle(FromSource.Obj fsObj) {
                if (fsObj.Result) {
                    transProcess.processPayOneBill(sock, momoOrgMsg, data, callback);
                }
            }
        }, callback);
    }

    private void doPayAirlineTicketFromBankNet(final NetSocket sock
            , final MomoMessage momoOrgMsg, final SockData data
            , final boolean nextstep, final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(momoOrgMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        /*BanknetVerifyOtp
        merchant_trans_id	-->	partner_ref
	    trans_id	-->	PartnerExtra_1
	    amount	-->	amount
	    otp	-->	partner_code
        */

        if (request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1() || !request.hasPartnerCode()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final MomoProto.TranHisV1 req = request;

        final TransferCommon.BankNetObj bnObj = new TransferCommon.BankNetObj();

        bnObj.merchant_trans_id = request.getPartnerRef();
        bnObj.trans_id = request.getPartnerExtra1();
        bnObj.amount = request.getAmount();
        bnObj.otp = request.getPartnerCode();
        bnObj.tranType = request.getTranType();

        if (request.getAmount() == 0) {

            return;
        }

        transferCommon.doVerifyBanknet(sock
                , momoOrgMsg
                , data
                , bnObj, nextstep, new Handler<FromSource.Obj>() {
            @Override
            public void handle(FromSource.Obj fsObj) {
                if (fsObj.Result) {
                    transProcess.processPayOneBill(sock, momoOrgMsg, data, callback);
                }
            }
        }, callback);
    }
}
