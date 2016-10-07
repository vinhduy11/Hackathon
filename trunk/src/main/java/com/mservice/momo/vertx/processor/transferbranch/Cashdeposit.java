package com.mservice.momo.vertx.processor.transferbranch;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.DepositInPlace;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.TransProcess;
import com.mservice.momo.vertx.processor.TransferCommon;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

/**
 * Created by concu on 4/19/14.
 */
public class Cashdeposit {
    TransProcess transProcess;
    Logger logger;
    Vertx vertx;
    DepositInPlace depositInPlace;
    PhonesDb phonesDb;
    FeeDb feeDb;
    private String AGENT_ADJUST_FOR_BANK_MANUAL ="";
    private String AGENT_ADJUST_FOR_BANK_MANUAL_FEE="";
    private Common mCom;
    private TransferCommon transferCommon;

    public Cashdeposit(Vertx vertx, Logger logger,JsonObject glbCfg){
        this.logger=logger;
        this.vertx =vertx;
        transProcess = new TransProcess(vertx,logger,glbCfg);
        depositInPlace =new DepositInPlace(vertx.eventBus(),logger);
        phonesDb = new PhonesDb(vertx.eventBus(),logger);
        feeDb=new FeeDb(vertx,logger);
        mCom = new Common(vertx,logger, glbCfg);
        transferCommon = new TransferCommon(vertx,logger,glbCfg);
        /*
        "bank_manual":{
        "agent_adjust_manual" :"bankoutmanual",
        "agent_adjust_manual_fee" :"bankoutmanualfee"
    },*
         */
        AGENT_ADJUST_FOR_BANK_MANUAL = glbCfg.getObject("bank_manual").getString("agent_adjust_manual","");
        AGENT_ADJUST_FOR_BANK_MANUAL_FEE = glbCfg.getObject("bank_manual").getString("agent_adjust_manual_fee", "");
    }
    public void doCashDeposit(final NetSocket sock
                                ,final MomoMessage msg
                                ,final SockData data,final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null ) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        switch (request.getSourceFrom()){
            case MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE:
                transProcess.processBankIn(sock, msg, data, callback);
                break;

            case MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE:
                doDepositFromBanknet(sock, msg, data, false, callback);
                break;

            default:
                logger.info("Transfer Cash deposit not supported SourceFrom "
                        + MomoProto.TranHisV1.SourceFrom.valueOf(request.getSourceFrom()).name());
                break;
        }
    }

    public void doSaveDepositOrWithdraw(final NetSocket sock
                                        , final MomoMessage msg
                                        , final SockData data
                                        , final Handler<JsonObject> callback){
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        if(request== null || !request.hasAmount()) return;

        final MomoProto.TranHisV1 frequest = request;

        /*address	-->	partner_ref
        maTinh	-->	partnerId
        maHuyen	-->	partnerCode
        */

        String tmpAddress = request.getPartnerRef() == null ? "" : request.getPartnerRef();
        String tmpMaTinh = request.getPartnerId() == null ? "" : request.getPartnerId();
        String tmpMaHuyen = request.getPartnerCode() == null ? "" : request.getPartnerCode();

        final String address = tmpAddress
                                + "; " + tmpMaTinh
                                + "; " + tmpMaHuyen;

        final int tranType = request.getTranType();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function","doSaveDepositOrWithdraw");
        log.add("amount",request.getAmount());
        log.add("maTinh", tmpMaTinh);
        log.add("maHuyen", tmpMaHuyen);

        log.add("fullAddress",address);

        //tam ngung dich vu giao hang nhanh
        int control = 1;
        if(control == 1){
            JsonObject tranRpl = Misc.getJsonObjRpl(100,0,request.getAmount(),0);
            mCom.sendTransReply(vertx,tranRpl, System.currentTimeMillis(), msg, sock,data, callback);
            return;
        }

        phonesDb.getPhoneObjInfo(msg.cmdPhone,new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                JsonObject tranRpl;

                final  long ackTime = System.currentTimeMillis();
                int service_type = Const.ServiceType.WITHDRAW.ordinal();

                if(tranType == MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE){
                    service_type = Const.ServiceType.DEPOSIT.ordinal();
                }
                log.add("dich vu",MomoProto.TranHisV1.TranType.valueOf(tranType).name());

                if(obj == null){
                    log.add("phoneObj","null");
                    //khong tinh gia tri cua giao dich nay
                    int io = 0;
                    tranRpl = Misc.getJsonObjRpl(100, 0, frequest.getAmount(), io);
                    tranRpl.putNumber(colName.TranDBCols.STATUS, TranObj.STATUS_FAIL);
                    mCom.sendTransReply(vertx,tranRpl, ackTime,msg,sock,data,callback);
                    log.writeLog();
                    return;
                }

                int named = (obj.isNamed == true ? 1 : 0);
                String full_name = (obj.name == null ? "" : obj.name);
                log.add("isnamed",named);
                log.add("name",full_name);

                JsonObject json = new JsonObject();
                json.putNumber("type", LStandbyOracleVerticle.DEPOSIT_WITHDRAW);
                json.putNumber(colName.DepositInPlaceDBCols.IS_NAMED, named);
                json.putString(colName.DepositInPlaceDBCols.FULL_NAME, full_name);
                json.putString(colName.DepositInPlaceDBCols.ADDRESS,address);
                json.putNumber(colName.DepositInPlaceDBCols.SERVICE_TYPE,service_type);
                json.putNumber(colName.DepositInPlaceDBCols.NUMBER,msg.cmdPhone);
                json.putNumber(colName.DepositInPlaceDBCols.AMOUNT,frequest.getAmount());

                if(sock == null)
                    json.putNumber(colName.DepositInPlaceDBCols.FROM_SOURCE, Const.FromSource.WEB.ordinal());
                else
                    json.putNumber(colName.DepositInPlaceDBCols.FROM_SOURCE, Const.FromSource.APP_MOBI.ordinal());

                vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS,json,new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> reply) {

                        JsonObject json = reply.body();
                        boolean result = json.getBoolean(colName.DepositInPlaceDBCols.RESULT,false);
                        int error_code = (result == true? 0:100);
                        log.add("oracle result", result);

                        //khong tinh gia tri cua giao dich nay
                        JsonObject  tranRpl = Misc.getJsonObjRpl(error_code, 0, frequest.getAmount(), 0);
                        tranRpl.putNumber(colName.TranDBCols.STATUS,(result ? TranObj.STATUS_OK : TranObj.STATUS_FAIL));

                        mCom.sendTransReply(vertx,tranRpl, ackTime, msg, sock,data, callback);
                        log.writeLog();
                    }
                });
            }
        });
    }

    private void doDepositFromBanknet(final NetSocket sock
            ,final MomoMessage msg
            ,final SockData data
            ,final boolean nextstep, final Handler<JsonObject> callback){

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
        otp	-->	partner_id
        */
        if(request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1()
                || !request.hasAmount() || !request.hasPartnerId()){
            mCom.writeErrorToSocket(sock);
            return;
        }

        final TransferCommon.BankNetObj bnObj = new TransferCommon.BankNetObj();
        bnObj.merchant_trans_id = request.getPartnerRef();
        bnObj.trans_id = request.getPartnerExtra1();
        bnObj.amount = request.getAmount();
        bnObj.otp = request.getPartnerId();
        bnObj.tranType= request.getTranType();

        Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE,bnObj.amount,"", "",new Handler<FeeDb.Obj>() {
            @Override
            public void handle(FeeDb.Obj obj) {
                int m2mFee = obj == null ? 1000: obj.STATIC_FEE;
                //do co phi M2M = 1000
                bnObj.amount = bnObj.amount + m2mFee;

                transferCommon.doVerifyBanknet(sock
                        ,msg
                        ,data
                        ,bnObj,nextstep, new Handler<FromSource.Obj>() {
                    @Override
                    public void handle(FromSource.Obj b) {
                        logger.info("doVerifyBanknet, nextstep, result " + nextstep + "," + b);
                    }
                }, callback);
            }
        });
    }

    public void doBankManual(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> callback){
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        if(request== null || !request.hasAmount()) return;

        final MomoProto.TranHisV1 frequest = request;

        /*bankId	    -->	partner_id
        holder_number	-->	partner_code
        amount	        -->	amount
        holder_name	    -->	partner_name
        bank name	    -->	partner_ref
        comment	        -->	comment
        inoutcity       --> billId -- trong/ngoai thanh pho
        bank_branch --> partner_extra_1
        */

        final  long ackTime = System.currentTimeMillis();

        final String bankId = frequest.getPartnerId() == null ? "" : frequest.getPartnerId();
        final int inout_city =Integer.parseInt(frequest.getBillId() == null ? "1" :frequest.getBillId());
        final String bank_branch = frequest.getPartnerExtra1() == null ? "" : frequest.getPartnerExtra1();
        final int feeType = MomoProto.FeeType.MOMO_VALUE;
        final int trantype = frequest.getTranType();
        final long amount = frequest.getAmount();
        final int channel = MomoProto.CardItem.Channel.MANUAL_VALUE;
        int tsource = 0; // nguon tu App
        if(sock== null){
            tsource = 1; // nguon tu Web
        }
        final int source = tsource;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("doBankManual","");
        log.add("bankid",bankId);
        log.add("channel",channel);
        log.add("tran type", MomoProto.TranHisV1.TranType.valueOf(trantype).name());
        log.add("inout city",inout_city);
        log.add("fee type", MomoProto.FeeType.valueOf(feeType).name());
        log.add("amount",amount);

        feeDb.getFee(bankId
                ,channel
                ,trantype
                ,inout_city
                ,feeType
                ,amount,new Handler<FeeDb.Obj>() {
            @Override
            public void handle(FeeDb.Obj obj) {
                int staticfee;
                double dynamicfee;

                if(obj==null){
                    staticfee=1100;
                    dynamicfee =1.2;
                }else {
                    staticfee=obj.STATIC_FEE;
                    dynamicfee=obj.DYNAMIC_FEE;
                }

                final int fStaticFee = staticfee;
                final double fDynamicFee = dynamicfee;

                final Misc.Cash cash = Misc.calculateAmount(frequest.getAmount()
                        , staticfee
                        , dynamicfee
                        , MomoProto.CardItem.LockedType.FULL_VALUE, trantype, log);

                //kiem tra co du so du khong
                JsonObject jo = new JsonObject();
                jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
                jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER, msg.cmdPhone);

                vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> jsonRpl) {
                        long curBalance = jsonRpl.body().getLong(UMarketOracleVerticle.fieldNames.BALANCE, 0);

                        JsonObject notEnoughCashJo = Misc.getJsonObjRpl(SoapError.INSUFFICIENT_FUNDS
                                , System.currentTimeMillis()
                                , cash.coreAmountAdjust
                                , -1);

                        log.add("current balance", curBalance);

                        // khong du tien
                        if(curBalance < cash.bankNetAmountLocked){
                            mCom.sendTransReply(vertx, notEnoughCashJo, ackTime,msg,sock,data, callback);
                            log.add("widthraw amount", cash.coreAmountAdjust);
                            log.add("fee amount", (cash.bankNetAmountLocked - cash.coreAmountAdjust));
                            log.add("total payed amount",cash.bankNetAmountLocked);
                            log.add("insufficient funds","");
                            log.writeLog();
                            return;
                        }

                        //thuc hien chuyen tien bankout manual

                        log.add("static fee", fStaticFee);
                        log.add("dynamic fee", fDynamicFee);
                        log.add("recieved amount", cash.coreAmountAdjust);
                        log.add("fee amount", (cash.bankNetAmountLocked - cash.coreAmountAdjust));

                        log.add("adjust 1","");
                        log.add("source","0" + msg.cmdPhone);
                        log.add("target",AGENT_ADJUST_FOR_BANK_MANUAL);
                        log.add("amount",cash.coreAmountAdjust);

                        //gui lock tien bang bankoutmanual
                        Buffer buffer = MomoMessage.buildBuffer(
                                SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                                , msg.cmdIndex
                                , msg.cmdPhone
                                , SoapProto.commonAdjust.newBuilder()
                                        .setSource("0" + msg.cmdPhone)
                                        .setTarget(AGENT_ADJUST_FOR_BANK_MANUAL)
                                        .setAmount(cash.coreAmountAdjust) // tru thang tien cua khach hang luon
                                        .setPhoneNumber("0" + msg.cmdPhone)
                                        .build()
                                        .toByteArray()
                        );

                        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> result) {

                                final JsonObject tranRpl = result.body();
                                int error = tranRpl.getInteger(colName.TranDBCols.ERROR, -1);
                                final long p_pending_tran = tranRpl.getLong(colName.TranDBCols.TRAN_ID,0);

                                log.add("result",error);
                                log.add("errordesc", SoapError.getDesc(error));

                                final long p_fee = cash.bankNetAmountLocked - cash.coreAmountAdjust;
                                //cat tien goc khong thanh cong
                                if(error >0){

                                    tranRpl.putNumber(colName.TranDBCols.IO,-1);
                                    mCom.sendTransReply(vertx, result.body(), ackTime,msg,sock,data, callback);
                                    log.writeLog();

                                }else{

                                    log.add("adjust 2","");
                                    log.add("source","0"+msg.cmdPhone);
                                    log.add("target",AGENT_ADJUST_FOR_BANK_MANUAL_FEE);
                                    log.add("amount",p_fee);

                                    //khong co phi
                                    if(p_fee == 0){

                                        insert2Accounting(msg
                                                ,frequest
                                                ,cash
                                                ,p_pending_tran
                                                ,p_fee
                                                ,inout_city
                                                ,bank_branch, source
                                                ,log, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {

                                                int status = TranObj.STATUS_OK;
                                                int error = 0;
                                                if(!aBoolean){
                                                    status =TranObj.STATUS_FAIL;
                                                    error = -1;
                                                }

                                                tranRpl.putNumber(colName.TranDBCols.STATUS,status);
                                                tranRpl.putNumber(colName.TranDBCols.IO,-1);
                                                tranRpl.putNumber(colName.TranDBCols.ERROR,error);
                                                tranRpl.putNumber(colName.TranDBCols.AMOUNT,cash.coreAmountAdjust);
                                                mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, data, callback);
                                            }
                                        });
                                    }else{

                                        //thuc hien cat phi bang bankoutmanualfee
                                        Buffer buffer = MomoMessage.buildBuffer(
                                                SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                                                ,msg.cmdIndex
                                                ,msg.cmdPhone
                                                ,SoapProto.commonAdjust.newBuilder()
                                                        .setSource("0" + msg.cmdPhone)
                                                        .setTarget(AGENT_ADJUST_FOR_BANK_MANUAL_FEE)
                                                        .setAmount(p_fee)
                                                        .setPhoneNumber("0" + msg.cmdPhone)
                                                        .build()
                                                        .toByteArray()
                                        );

                                        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> jsonResult) {

                                                int error = jsonResult.body().getInteger(colName.TranDBCols.ERROR, -1);
                                                log.add("result", error);
                                                log.add("errordesc", SoapError.getDesc(error));

                                                //cat phi khong thanh cong rollback so tien goc cho khach
                                                if (error != 0) {

                                                    tranRpl.putNumber(colName.TranDBCols.STATUS, TranObj.STATUS_FAIL);
                                                    tranRpl.putNumber(colName.TranDBCols.IO, -1);
                                                    tranRpl.putNumber(colName.TranDBCols.ERROR, error);
                                                    tranRpl.putNumber(colName.TranDBCols.AMOUNT, cash.coreAmountAdjust);
                                                    mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, data, callback);

                                                    rollBackAdjustment(msg
                                                            , cash.coreAmountAdjust
                                                            , AGENT_ADJUST_FOR_BANK_MANUAL
                                                            , "0" + msg.cmdPhone, log);

                                                    log.writeLog();
                                                } else {

                                                    //them du lieu xuong ke toan
                                                    insert2Accounting(msg
                                                            , frequest
                                                            , cash
                                                            , p_pending_tran
                                                            , p_fee, inout_city
                                                            , bank_branch,source
                                                            , log, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean aBoolean) {
                                                            int status = TranObj.STATUS_OK;
                                                            int error = 0;
                                                            if(!aBoolean){
                                                                status =TranObj.STATUS_FAIL;
                                                                error =-1;
                                                            }
                                                            tranRpl.putNumber(colName.TranDBCols.STATUS,status);
                                                            tranRpl.putNumber(colName.TranDBCols.IO,-1);
                                                            tranRpl.putNumber(colName.TranDBCols.ERROR,error);
                                                            tranRpl.putNumber(colName.TranDBCols.AMOUNT,cash.coreAmountAdjust);
                                                            mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, data, callback);
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void insert2Accounting(final MomoMessage msg
                                   ,final MomoProto.TranHisV1 frequest
                                    ,final Misc.Cash cash
                                    ,long p_pending_tran
                                    ,final long p_fee
                                    ,int inout_city
                                    ,String bank_branch
                                    ,int source
                                    ,final Common.BuildLog log
                                    ,final Handler<Boolean> callback) {

        String p_agent = "0" + msg.cmdPhone;
        String p_bank_name = frequest.getPartnerRef()== null ? "" : frequest.getPartnerRef();
        String p_cardname = frequest.getPartnerName() == null ? ""  : frequest.getPartnerName();
        String p_cardnumber =frequest.getPartnerCode()== null ? "" : frequest.getPartnerCode();
        String p_comment = frequest.getComment() == null ? "" : frequest.getComment();

        JsonObject jo = new JsonObject();
        jo.putNumber("type", UMarketOracleVerticle.BANK_OUT_MANUAL);
        jo.putString(UMarketOracleVerticle.CashDepotOrWithdraw.AGENT,p_agent);
        //Fix bank name co dau.
        jo.putString(UMarketOracleVerticle.CashDepotOrWithdraw.BANK_NAME, Misc.removeAccent(p_bank_name));
        jo.putNumber(UMarketOracleVerticle.CashDepotOrWithdraw.PENDING_TRAN, p_pending_tran);
        jo.putString(UMarketOracleVerticle.CashDepotOrWithdraw.CARDNAME,Misc.removeAccent(p_cardname));
        jo.putString(UMarketOracleVerticle.CashDepotOrWithdraw.CARDNUMBER ,p_cardnumber);
        jo.putNumber(UMarketOracleVerticle.CashDepotOrWithdraw.AMOUNT, cash.bankNetAmountLocked);
        jo.putNumber(UMarketOracleVerticle.CashDepotOrWithdraw.RECEIVE_AMOUNT, cash.coreAmountAdjust);
        jo.putNumber(UMarketOracleVerticle.CashDepotOrWithdraw.FEE, p_fee);
        jo.putString(UMarketOracleVerticle.CashDepotOrWithdraw.COMMENT,p_comment);
        jo.putNumber(UMarketOracleVerticle.CashDepotOrWithdraw.INOUT_CITY,inout_city);
        jo.putString(UMarketOracleVerticle.CashDepotOrWithdraw.BANK_BRANCH,bank_branch);
        jo.putNumber(UMarketOracleVerticle.CashDepotOrWithdraw.SOURCE,source);

        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jResult) {

                JsonObject jo =  jResult.body();
                boolean result = jo.getBoolean(UMarketOracleVerticle.CashDepotOrWithdraw.RESULT);
                log.add("Ke toan result", result);

                //chen xuong db ke toan khong thanh cong -->rollback toan bo tien cho khach
                if (!result){
                    //rollback fee
                    if(p_fee >0){
                        log.add("rollback fee amout",p_fee);
                        rollBackAdjustment(msg
                                ,p_fee
                                ,AGENT_ADJUST_FOR_BANK_MANUAL_FEE
                                ,"0" + msg.cmdPhone,log);
                    }

                    //rollback tien goc
                    log.add("rollback recieved amout",cash.coreAmountAdjust);
                    rollBackAdjustment(msg
                            ,cash.coreAmountAdjust
                            ,AGENT_ADJUST_FOR_BANK_MANUAL
                            ,"0" + msg.cmdPhone,log);
                }

                callback.handle(result);

                log.writeLog();
            }
        });
    }

    private void rollBackAdjustment(final MomoMessage msg
                                    ,final long amount
                                    ,final String fromAccount
                                    ,final String toAccount
                                    ,final Common.BuildLog log){
        //gui qua soap de lock tien
        Buffer buffer = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , SoapProto.commonAdjust.newBuilder()
                        .setSource(fromAccount)
                        .setTarget(toAccount)
                        .setAmount(amount)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS
                                        ,buffer
                                        ,new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jResult) {

                log.add("rollback result", jResult.body());
                log.add("rollback source",fromAccount);
                log.add("rollback target",toAccount);
                log.add("rollback amount",amount);

            }
        });
    }
}
