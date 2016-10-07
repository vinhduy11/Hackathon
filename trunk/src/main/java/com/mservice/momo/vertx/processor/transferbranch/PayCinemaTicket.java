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
 * Created by concu on 4/25/14.
 */
public class PayCinemaTicket {

    TransProcess transProcess;
    Logger logger;
    Vertx vertx;
    Common mCom;
    TransferCommon transferCommon;

    public PayCinemaTicket(Vertx vertx, Logger logger, JsonObject glbCfg) {
        this.logger = logger;
        this.vertx = vertx;
        transProcess = new TransProcess(vertx, logger, glbCfg);
        mCom = new Common(vertx, logger, glbCfg);
        transferCommon = new TransferCommon(vertx, logger, glbCfg);
    }

    public void doPayCinemaTicket(final NetSocket sock, final MomoMessage momoOrgMsg, final SockData data, final Handler<JsonObject> callback) {

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
                doPayCinemaTicketFromMomo(sock, momoOrgMsg, data, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.PAY123_VALUE:
                doPayCinemaTicketFromMomo(sock, momoOrgMsg, data, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                doPayCinemaTicketFromBankNet(sock, momoOrgMsg, data, true, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                doPayCinemaTicketFromBankLinked(sock, momoOrgMsg, data, true, callback);
                break;

            default:
                logger.info("PayAirLineTicket not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(request.getSourceFrom()).name());
                break;
        }
    }

    private void doPayCinemaTicketFromMomo(final NetSocket sock
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
            mCom.writeErrorToSocket(sock);
            return;
        }

        if (request.getAmount() > 0) {
            transProcess.processPayOneBill(sock, momoOrgMsg, data, callback);
            return;
        }

        /*CoreCommon.getTicketInfo(sock,momoOrgMsg,data,new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jo) {
                if(jo == null){
                    jo= new JsonObject();
                    jo.putString(CoreCommon.TicketFields.PROVIDER_ID,req.getPartnerId());
                    jo.putString(CoreCommon.TicketFields.TICKET_ID,req.getBillId());
                    jo.putNumber(CoreCommon.TicketFields.RCODE,100);
                }

                if(jo.getInteger(CoreCommon.TicketFields.RCODE,0)>0){
                    CoreCommon.sendGetTicketInfoError(sock,momoOrgMsg,jo);
                    return;
                }

                transProcess.processPayOneBill(sock,momoOrgMsg,data, callback);
            }
        });*/
    }

    private void doPayCinemaTicketFromBankLinked(final NetSocket sock
            , final MomoMessage momoOrgMsg, final SockData data
            , final boolean nextstep
            , final Handler<JsonObject> callback) {
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
            mCom.writeErrorToSocket(sock);
            return;
        }

        final MomoProto.TranHisV1 req = request;

        final TransferCommon.BankLinkedObj blObj = new TransferCommon.BankLinkedObj();
        blObj.amount = request.getAmount();
        blObj.bank_code = request.getPartnerExtra1();
        blObj.from_bank = (request.getPartnerRef() == null ? "" : request.getPartnerRef());

        if (request.getAmount() == 0) {

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

            /*CoreCommon.getTicketInfo(sock,momoOrgMsg,data,new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jo) {
                    if(jo == null){
                        jo= new JsonObject();
                        jo.putString(CoreCommon.TicketFields.PROVIDER_ID,req.getPartnerId());
                        jo.putString(CoreCommon.TicketFields.TICKET_ID,req.getBillId());
                        jo.putNumber(CoreCommon.TicketFields.RCODE,100);
                    }

                    if(jo.getInteger(CoreCommon.TicketFields.RCODE,0)>0){
                        CoreCommon.sendGetTicketInfoError(sock,momoOrgMsg,jo);
                        return;
                    }


                }
            });*/

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

    private void doPayCinemaTicketFromBankNet(final NetSocket sock
            , final MomoMessage momoOrgMsg, final SockData data
            , final boolean nextstep
            , final Handler<JsonObject> callback) {
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
            mCom.writeErrorToSocket(sock);
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

            transferCommon.doVerifyBanknet(sock
                    , momoOrgMsg
                    , data
                    , bnObj
                    , nextstep, new Handler<FromSource.Obj>() {
                @Override
                public void handle(FromSource.Obj b) {
                    if (b.Result) {
                        transProcess.processPayOneBill(sock, momoOrgMsg, data, callback);
                    }
                }
            }, callback);
            /*CoreCommon.getTicketInfo(sock,momoOrgMsg,data,new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jo) {
                    if(jo == null){
                        jo= new JsonObject();
                        jo.putString(CoreCommon.TicketFields.PROVIDER_ID,req.getPartnerId());
                        jo.putString(CoreCommon.TicketFields.TICKET_ID,req.getBillId());
                        jo.putNumber(CoreCommon.TicketFields.RCODE,100);
                    }

                    if(jo.getInteger(CoreCommon.TicketFields.RCODE,0)>0){
                        CoreCommon.sendGetTicketInfoError(sock,momoOrgMsg,jo);
                        return;
                    }


                }
            });*/

            return;
        }
    }

}
