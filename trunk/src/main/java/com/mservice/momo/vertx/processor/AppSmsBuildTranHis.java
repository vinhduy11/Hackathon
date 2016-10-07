package com.mservice.momo.vertx.processor;

import com.mservice.momo.data.model.AppSmsConst;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.gateway.internal.soapin.information.SoapInProcess;
import com.mservice.momo.gateway.internal.soapin.information.obj.AgentInfo;
import com.mservice.momo.util.DataUtil;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by User on 4/3/14.
 */
public class AppSmsBuildTranHis {
    private Logger logger;
    private String sep =" ";
    private JsonObject glbCfg;
    public AppSmsBuildTranHis(Logger logger, JsonObject glbCfg){
        this.logger = logger;
        this.glbCfg = glbCfg;
    }

    public MomoProto.TranHisV1 TopUpTranHis(MomoMessage momoMessage, String orgContent, int tranType){

        // todo parse orgContent here to build exact tran his.
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>
        String[] arr = orgContent.split(sep);

        String to_number = arr.length > 4 ? arr[4] : "";
        long amount = DataUtil.stringToUNumber(arr.length > 5 ? arr[5] : "0");

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        optional uint64 tranId =1; // return from core doing transaction, cTID
        optional uint64 client_time = 2; // time at calling webservice
        optional uint64 ackTime = 3; // ack time from server to client
        optional uint64 finishTime=4; // the time that server response result.
        optional uint32 tranType = 5; //ex : MomoProto.MsgType.BANK_IN_VALUE
        optional uint32 io = 6; // direction of transaction.
        optional uint32 category=7; // type of transfer, chuyen tien ve que, cho vay, khac
        optional string partnerId = 8; // ban hang cua minh : providerId ..
        optional string partnerCode=9; // ma doi tac
        optional string partnerName=10; // ten doi tac
        optional string partner_ref =11;
        optional string billId=12; // for invoice, billID
        optional uint64 amount=13; // gia tri giao dich
        optional string comment=14; // ghi chu
        optional uint32 status=15; // trang thai giao dich
        optional uint32 error=16; // ma loi giao dich
        optional uint64 command_Ind = 17; // command index
        */

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerCode(to_number);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 TopUpGameTranHis(MomoMessage momoMessage, String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        ProviderId	-->	partner_id
        game_account	-->	partner_code
        amount	-->	amount
        */
        String providerId ="";
        String gameAccount ="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());
        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerCode(gameAccount);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 M2MTranHis(MomoMessage momoMessage, String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*M2M
        phone	-->	partner_id
        amount	-->	amount
        notice	-->	comment*/

        String phone ="";
        long amount = 0;
        String comment =""; // todo hình như phải replace . = " "

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(phone);
        tranBuilder.setAmount(amount);
        tranBuilder.setComment(comment);
        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 M2CTranHis(MomoMessage momoMessage, String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*M2C
        phone	-->	partner_id
        name	-->	partner_name
        cardId	-->	partner_code
        amount	-->	amount
        notice	-->	comment
        */

        String phone ="";
        String name =""; //todo hình như phải replace . = " "
        String cardId ="";
        long amount = 0;
        String comment =""; // todo hình như phải replace . = " "

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(phone);
        tranBuilder.setPartnerName(name);
        tranBuilder.setPartnerCode(cardId);
        tranBuilder.setAmount(amount);
        tranBuilder.setComment(comment);
        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BankInTranHis(MomoMessage momoMessage, String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*BankIn
        from_bank	-->	partner_name
	    bank_code	-->	partner_code
	    amount	-->	amount
        */
        SoapInProcess soapInProcess = new SoapInProcess(logger, glbCfg);
        AgentInfo agentInfo = soapInProcess.getAgentInfo(String.valueOf(momoMessage.cmdPhone));

        String from_bank = agentInfo == null ? "" : agentInfo.getBank_name();
        String bank_code = agentInfo == null ? "" : agentInfo.getBank_code();
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerName(from_bank);
        tranBuilder.setPartnerCode(bank_code);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BankOutTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*BankOut
        from_bank	-->	partner_name
	    bank_code	-->	partner_code
	    amount	-->	amount
        */
        SoapInProcess soapInProcess = new SoapInProcess(logger,glbCfg);
        AgentInfo agentInfo = soapInProcess.getAgentInfo(String.valueOf(momoMessage.cmdPhone));

        String from_bank = agentInfo == null ? "" : agentInfo.getBank_name();
        String bank_code = agentInfo == null ? "" : agentInfo.getBank_code();
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerName(from_bank);
        tranBuilder.setPartnerCode(bank_code);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 TransferMoney2PlaceTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*TransferMoney2Place
        phone	partner_id
        amount	amount
        notice	comment
        */

        String phone = "";
        String comment = ""; //todo replace all . = " "
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(phone);
        tranBuilder.setComment(comment);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BillPayTelephoneTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        partner_ref	-->	areaId
        billId	-->	phone
        amount	-->	amount
        */

        String providerId = "";
        String areaId = "";
        String phone="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setPartnerRef(areaId);
        tranBuilder.setBillId(phone);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BillPayTicketAirlineTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        billId	-->	ticketId
        amount	-->	amount
        */
        String providerId = "";
        String ticketId="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setBillId(ticketId);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BillPayInternetTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        BillPayInternet
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        partner_ref	-->	areaId
        billId	-->	customerAcc
        amount	-->	amount
        */
        String providerId = "";
        String customerAcc="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setBillId(customerAcc);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BillPayTicketTrainTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        BillPayTicketTrain
        partner_id --> ProviderId
        partner_ref	-->	areaId
        partnerName	-->	getProviderName(areaId)
        billId	-->	ticketId
        amount	-->	amount
        */
        String providerId ="";
        String areaId = "";
        String ticketId="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerRef(areaId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(areaId));
        tranBuilder.setBillId(ticketId);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BillPayInsuranceTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        BillPayInsurance
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        billId	-->	insuranceAcc
        amount	-->	amount
        */
        String providerId = "";
        String insuranceAcc="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setBillId(insuranceAcc);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BillPayOtherTranHis(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        BillPayOther
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        partner_ref	-->	areaId
        billId	-->	billerId
        amount	-->	amount
        */
        String providerId = "";
        String areaId ="";
        String billerId="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerRef(areaId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setBillId(billerId);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 DepositCashOther(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>
        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        DepositCashOther
        partnerId	-->	 providerId
        partnerName	-->	getProviderName(providerId)
        billId	-->	customerAcc
        amount	-->	amount
        */
        String providerId = "";
        String customerAcc="";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setBillId(customerAcc);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BuyMobilityCard(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        BuyMobilityCard
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        amount	-->	amount
        */
        String providerId = "";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BuyGameCard(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        BuyGameCard
        partnerId	-->	 providerId
        partnerName	-->	getProviderName(providerId)
        amount	-->	amount
        */
        String providerId = "";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BuyOther(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        BuyOther
        partnerId	-->	 providerId
        partnerName	-->	getProviderName(providerId)
        amount	    -->	amount
        */
        String providerId = "";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 BillPayElectric(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        PayOneBill --> BillPayElectric
        ProviderId	-->	partner_id
	    billId	-->	billId
	    amount	-->	amount
        */

        //todo must change providerid to dien if needed
        String providerId = "";
        String billId = "";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setBillId(billId);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

    public MomoProto.TranHisV1 mua123(MomoMessage momoMessage,String orgContent,int tranType){

        // todo parse orgContent here to build exact tran his
        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>

        String[] arr = orgContent.split(sep);

        MomoProto.TranHisV1.Builder tranBuilder = MomoProto.TranHisV1.newBuilder();
        /*
        PayOneBill --> BillPayElectric
        ProviderId	-->	partner_id
	    billId	-->	billId
	    amount	-->	amount
        */

        //todo must change providerid to dien if needed
        String providerId = "";
        String billId = "";
        long amount = 0;

        //set values
        tranBuilder.setCommandInd(momoMessage.cmdIndex);
        tranBuilder.setTranType(tranType);
        tranBuilder.setClientTime(System.currentTimeMillis());

        tranBuilder.setPartnerId(providerId);
        tranBuilder.setPartnerName(AppSmsConst.Partner.getProviderName(providerId));
        tranBuilder.setBillId(billId);
        tranBuilder.setAmount(amount);

        return tranBuilder.build() ;
    }

}
