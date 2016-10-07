package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.conf.WalletType;
import com.mservice.momo.connector.ServiceHelper;
import com.mservice.momo.data.*;
import com.mservice.momo.data.MerchantOfflinePayment.MerchantKeyManageDb;
import com.mservice.momo.data.ironmanpromote.IronManPromoGiftDB;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.connectorproxy.ConnectorCommon;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.visamaster.VMRequest;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.NotificationUtils;
import com.mservice.momo.util.StatisticUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.HTTPMerchantWebSiteVerticle;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.GiftTran;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.transferbranch.*;
import com.mservice.proxy.entity.ProxyRequest;
import com.mservice.proxy.entity.ProxyResponse;
import com.mservice.proxy.entity.v2.ProxyRequestFactory;
import com.mservice.proxy.entity.v2.ProxyRequestType;
import com.mservice.shopping.entity.PaymentResponse;
import com.mservice.shopping.entity.ShoppingRequest;
import com.mservice.shopping.entity.ShoppingRequestFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.*;

/**
 * Created by concu on 4/14/14.
 */
public class TransferProcess extends TransProcess {
    public static boolean BANK_NET_TEST_MODE;
    public static TransDb transDb;
    private static String Driver;
    private static String Url;
    private static String Username;
    private static String Password;
    private static int PORT_REDIS_1;
    private static int PORT_REDIS_2;
    private String VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET;
    private String VIETTINBANK_AGENT_ADJUST_FOR_BANKNET;
    private Logger logger;
    private TransProcess transProcess;
    private M2m transferM2M;
    private M2Merchant m2Merchant;
    private TopUp transferTopUp;
    private Cashdeposit transferCashDeposit;
    private PayOneBill transferPOB;
    private TopUpGame transferTUG;
    private PayAirLineTicket payAirLineTicket;
    private PayCinemaTicket payCinemaTicket;
    private HashMap<Integer,Boolean> mapTranRunning;
    private Vinagame123Phim vinagame123Phim;
    private PayAVGBill payAVGBill;
    private PayNuocCLBill payNuocCLBill;
    private Common mCom;
    private StatisticUtils statisticUtils;
    private GiftProcess giftProcess;
    private PhonesDb phonesDb;
    private Vertx vertx;
    private boolean isStoreApp;
    private JsonObject jsonShopping;
    private GiftManager giftManager;
    private PayOneSaleOffBill transferPayOneSaleOffBill;
    private IronManPromoGiftDB ironManPromoGiftDB;
    private BillsDb billsDb;
    private JsonObject globalConfig;
    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    private DBProcess dbProcess;
    private HttpClient httpClientRedisOne;
    private HttpClient httpClientRedisTwo;
    private String HOST_REDIS_1;
    private String HOST_REDIS_2;
    private String PATH_REDIS_1;
    private String PATH_REDIS_2;
    private JsonObject merchantKeyManageObject;
    private Hashtable<String, String> hashtableNumber;
    public TransferProcess(Vertx vertx
                            ,Logger logger
                            ,JsonObject glbCfg
                            ,HashMap<Integer,Boolean> mapTranRunning){
        super(vertx, logger, glbCfg);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);
        statisticUtils = new StatisticUtils(vertx, logger, glbCfg);
        this.vertx = vertx;

        this.VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET =glbCfg.getObject("server").getString("vietcombank_adjust_for_banknet","");
        this.VIETTINBANK_AGENT_ADJUST_FOR_BANKNET =glbCfg.getObject("server").getString("vietcombank_adjust_for_banknet","");

        this.mapTranRunning =mapTranRunning;
        this.logger =logger;
        this.globalConfig = glbCfg;
        this.transProcess =new TransProcess(vertx,logger,glbCfg);
        transferM2M = new M2m(vertx,logger,glbCfg);
        transferTopUp = new TopUp(vertx,logger,glbCfg);
        transferCashDeposit =new Cashdeposit(vertx,logger,glbCfg);
        transferPOB = new PayOneBill(vertx,logger,glbCfg);
        transferTUG = new TopUpGame(vertx,logger,glbCfg);
        payAirLineTicket = new PayAirLineTicket(vertx,logger,glbCfg);
        payCinemaTicket = new PayCinemaTicket(vertx,logger,glbCfg);

        vinagame123Phim = new Vinagame123Phim(vertx,logger, glbCfg);

        payNuocCLBill = new PayNuocCLBill(vertx,logger,glbCfg);
        payAVGBill = new PayAVGBill(vertx,logger,glbCfg);
        mCom = new Common(vertx,logger, glbCfg);

        giftProcess = new GiftProcess(mCom, vertx, logger, glbCfg);
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        jsonShopping = glbCfg.getObject(StringConstUtil.Shopping.ServiceType, new JsonObject());
        giftManager = new GiftManager(vertx, logger, glbCfg);
        transferPayOneSaleOffBill = new PayOneSaleOffBill(vertx, logger, glbCfg);
        ironManPromoGiftDB = new IronManPromoGiftDB(vertx, logger);
        billsDb = new BillsDb(vertx.eventBus());
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        merchantKeyManageObject = globalConfig.getObject(StringConstUtil.MerchantKeyManage.JSON_OBJECT, new JsonObject());
        m2Merchant = new M2Merchant(vertx, logger, glbCfg);
        this.PORT_REDIS_1 = merchantKeyManageObject.getInteger("portRedis1", 3456);
        this.PORT_REDIS_2 = merchantKeyManageObject.getInteger("portRedis2", 3456);
        this.HOST_REDIS_1 = merchantKeyManageObject.getString("hostRedis1", "172.16.43.14");
        this.HOST_REDIS_2 = merchantKeyManageObject.getString("hostRedis2", "172.16.43.14");
        this.PATH_REDIS_1 = merchantKeyManageObject.getString("pathRedis1", "/redis/core");
        this.PATH_REDIS_2 = merchantKeyManageObject.getString("pathRedis2", "/redis/core");
        //setting for TransferCommon
        /*TransferCommon.BANK_NET_TEST_MODE = BANK_NET_TEST_MODE;
        TransferCommon.VIETTINBANK_AGENT_ADJUST_FOR_BANKNET = vietcombank_adjust_account;
        TransferCommon.VIETTINBANK_AGENT_ADJUST_FOR_BANKNET = viettinbank_adjust_account;*/

        /* "refund":{
            "banknet_agent":"vtb2vcb",
                    "pay123_agent_internal":"vtb2vcb",
                    "pay123_agent_credit":"vtb2vcb"
        }*/

        /*TransferCommon.REFUND_BANKNET_AGENT =  glbCfg.getObject("refund").getString("banknet_agent","");
        TransferCommon.REFUND_123PAY_AGENT_INTERNAL=   glbCfg.getObject("refund").getString("pay123_agent_internal","");
        TransferCommon.REFUND_123PAY_AGENT_CREDIT_CARD=   glbCfg.getObject("refund").getString("pay123_agent_credit","");

        TransferCommon.REFUND_DATE_FROM = CoreCommon.getDateAsLong(glbCfg.getObject("refund").getString("date_from",""),"yyyy-MM-dd HH:mm:ss",logger,"Ngày bắt đầu hoàn phí");
        TransferCommon.REFUND_DATE_TO = CoreCommon.getDateAsLong(glbCfg.getObject("refund").getString("date_to",""),"yyyy-MM-dd HH:mm:ss",logger,"Ngày kết thúc hoàn phí");


        TransferCommon.logger =logger;
        TransferCommon.vertx =vertx;*/

        phonesDb = new PhonesDb(vertx.eventBus(),logger);

        JsonObject db_cfg = glbCfg.getObject("lstandby_database");
        Driver = db_cfg.getString("driver");
        Url = db_cfg.getString("url");
        Username = db_cfg.getString("username");
        Password = db_cfg.getString("password");
        dbProcess = new DBProcess(Driver
                , Url
                , Username
                , Password
                , AppConstant.LStandbyOracleVerticle_ADDRESS + "TransferProcess"
                , AppConstant.LStandbyOracleVerticle_ADDRESS + "TransferProcess"
                , logger);
        httpClientRedisOne = vertx.createHttpClient()
                .setHost(HOST_REDIS_1)
                .setPort(PORT_REDIS_1)
                .setMaxPoolSize(20)
                .setConnectTimeout(120000) // 2 phut
                .setKeepAlive(false);

        httpClientRedisTwo = vertx.createHttpClient()
                .setHost(HOST_REDIS_2)
                .setPort(PORT_REDIS_2)
                .setMaxPoolSize(20)
                .setConnectTimeout(120000) // 2 phut
                .setKeepAlive(false);

        hashtableNumber = new Hashtable<>();

    }

    public static boolean checkNewAPI(String serviceId, int version) {
        if (21 == version) {
            return true;
        }
        return false;
    }

    public void execTransfer(final NetSocket sock
                                            ,MomoMessage momoOrgMsg
                                            ,final SockData data
                                            ,final Handler<JsonObject> webcallback){

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(momoOrgMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoOrgMsg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasPartnerCode() || !request.hasAmount()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        String str = "logcmd phone: " + "0" + momoOrgMsg.cmdPhone
                        + " tranType: " + MomoProto.TranHisV1.TranType.valueOf(request.getTranType()).name()
                        + " cmdInd: " + momoOrgMsg.cmdIndex;

        for(int i =0;i<request.getKvpCount();i ++ ){
            if("mobiVer".equalsIgnoreCase(request.getKvp(i).getText())){
                str = str + " mobiVer: " + request.getKvp(i).getValue();
                break;
            }
        }
        str = str + " current Index/ last Index: " + momoOrgMsg.cmdIndex + "/ " + (data == null ? "socdata null" : data.lastCmdInd);
        if(data == null){
            str = str + " desc: socket data null";
            mCom.writeErrorToSocket(sock);
            logger.info(str);
            return;
        }

        if ((momoOrgMsg.cmdIndex  == data.lastCmdInd)  && (data.lastCmdInd > 0)){
            str = str + " desc: trung command index roi";
            mCom.writeErrorToSocket(sock);

            JsonObject joTranReply = Misc.getJsonObjRpl(SoapError.SYSTEM_ERROR,System.currentTimeMillis(),0,-1);
            mCom.sendTransReply(vertx
                                ,joTranReply
                                ,System.currentTimeMillis()
                                ,momoOrgMsg
                                ,sock
                                ,data
                                ,null);
            logger.info(str);
            return;
        }

        logger.info(str);

        data.lastCmdInd = momoOrgMsg.cmdIndex;

        JsonObject joUp = new JsonObject();
        joUp.putNumber(colName.PhoneDBCols.LAST_CMD_IND, momoOrgMsg.cmdIndex);
        phonesDb.updatePartial(momoOrgMsg.cmdPhone,joUp,new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
            }
        });

        //gui thong bao da nhan yeu cau thanh cong
        mCom.sendACK(sock,momoOrgMsg);

        List<MomoProto.TextValue> kvp = request.getKvpList();
        for (MomoProto.TextValue text : kvp) {
            if (text.getText().equals("refId")) {
                String refId = text.getValue();
                if (refId != null && !refId.isEmpty())
                    statisticUtils.broadcastNotiTran(refId);
            }
        }

        int tranType = request.getTranType();
        if(tranType == MomoProto.TranHisV1.TranType.M2C_VALUE){
            tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
        }

        switch (tranType){

//            case MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE:
//                transProcess.processVisaMasterCashIn(sock,momoOrgMsg,data,false,null,null,null);
//                break;
            case MomoProto.TranHisV1.TranType.C2C_RECEIVE_VALUE:
                transProcess.processC2CReceive(sock,momoOrgMsg,data,null);
                break;

            case MomoProto.TranHisV1.TranType.C2C_VALUE:
                    transProcess.processC2C(sock,momoOrgMsg,data,null);
                break;

            //nap tien dien thoai
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:

                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)){
                    transferTopUp.doTopUp(sock,momoOrgMsg,data, webcallback);
                }
                break;
            //nap tien tu bank vcb/vtb --> mm
            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferCashDeposit.doCashDeposit(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferM2M.doM2M(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.M2C_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferM2M.doM2M(sock, momoOrgMsg, data, webcallback);
                }
                //transferM2Number.doM2Number(sock, momoOrgMsg, data, null);
                break;
            //giao dich rut tien tai diem giao dich
            case MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transProcess.processTransferMoneyToPlace(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.BANK_OUT_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transProcess.processBankOut(sock, momoOrgMsg, data, webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferPOB.doPayOneBill(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferTUG.doTopUpGame(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    payAirLineTicket.doPayAirlineTicket(sock, momoOrgMsg, data, webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.BILL_PAY_CINEMA_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    payCinemaTicket.doPayCinemaTicket(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferCashDeposit.doSaveDepositOrWithdraw(sock, momoOrgMsg
                            , data
                            , webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferCashDeposit.doSaveDepositOrWithdraw(sock, momoOrgMsg
                            , data
                            , webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    transferCashDeposit.doBankManual(sock, momoOrgMsg, data, webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    vinagame123Phim.do123Phim(sock,momoOrgMsg,data,webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.PAY_AVG_BILL_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    payAVGBill.doPayAVGBill(sock,momoOrgMsg,data,webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.PAY_NUOCCL_BILL_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    payNuocCLBill.doPayAVGBill(sock,momoOrgMsg,data,webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.BUY_GIFT_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    giftProcess.buyGift(sock,momoOrgMsg,data,webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.SEND_GIFT_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    giftProcess.sendGift(sock,momoOrgMsg,data,webcallback);
                }
                break;

            case MomoProto.TranHisV1.TranType.GIFT_TO_MPOINT_VALUE:
                if (mCom.serviceIsRunning(sock, momoOrgMsg, mapTranRunning, webcallback)) {
                    giftProcess.giftToPoint(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE:
                transferPayOneSaleOffBill.doPayOneSaleOffBill(sock, momoOrgMsg, data, webcallback);
                break;
            case MomoProto.TranHisV1.TranType.TRANSFER_ONE_BILL_VALUE:
                if(mCom.serviceIsRunning(sock,momoOrgMsg,mapTranRunning,webcallback)) {
                    m2Merchant.doM2Merchant(sock, momoOrgMsg, data, webcallback);
                }
                break;
            case MomoProto.TranHisV1.TranType.ATM_CASH_IN_VALUE:
                transProcess.processATMBankIn(sock, momoOrgMsg, data, null);
                break;
            case MomoProto.TranHisV1.TranType.ATM_CASH_OUT_VALUE:
                transProcess.processATMBankOut(sock, momoOrgMsg, data, null);
                break;
            default:
                logger.info(momoOrgMsg + "Transfer type has not supported yet " + request.getTranType());
                break;
        }
    }

    // Ham nay dung de thanh toan cac loai dich vu moi, goi qua connector nhu cungmua, nhommua
    public void processPayOneSaleOffBill(final NetSocket sock
            , final MomoMessage msg
            , final SockData _data
            , final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        final MomoProto.TranHisV1 fRequest = request;

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processPayOneBill", logger, sock, callback)) {
            return;
        }

        final String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processPayOneSaleOffBill");
        //lay thong tin referal
//        SoapProto.keyValuePair.Builder referalBuilder = null;
//        if (_data != null
//                && _data.getPhoneObj() != null
//                && _data.getPhoneObj().referenceNumber > 0
//                && _data.getPhoneObj().createdDate >= date_affected
//                && (_data.getPhoneObj().createdDate + day_duration * 24 * 60 * 60 * 1000L) > System.currentTimeMillis()) {
//
//            referalBuilder = SoapProto.keyValuePair.newBuilder();
//            referalBuilder.setKey(Const.REFERAL)
//                    .setValue("0" + _data.getPhoneObj().referenceNumber);
//        }

        HashMap<String, String> hashMap = Misc.getKeyValuePairs(request.getKvpList());
        //HashMap<String, String> hashMap1 = Misc.buildKeyValueHashMap(request.getKvpList());
        //HashMap<String, String> hashMap2 = Misc.buildKeyValueHashMap(request.getShare());
        String tmpServiceId = (request.getPartnerId() == null || request.getPartnerId().isEmpty() ? "" : request.getPartnerId());

        tmpServiceId = "".equalsIgnoreCase(tmpServiceId) ?
                (hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : tmpServiceId)
                : tmpServiceId;

        final String serviceId = tmpServiceId;

        final int walletType = hashMap.containsKey(Const.AppClient.SourceFrom) ? DataUtil.strToInt(hashMap.get(Const.AppClient.SourceFrom)) : 1;
        final long cashinTransId = hashMap.containsKey("parentId") ? DataUtil.stringToUNumber(hashMap.get("parentId")) : 0;
        final int typeSource = hashMap.containsKey("tys") ? DataUtil.strToInt(hashMap.get("tys")) : 0;
        log.add("typeSource billpay", typeSource);
        log.add("parentId billpay", cashinTransId);

        log.add("hashmap before", hashMap.toString());

        log.add("share", request.getShare());
        final JsonObject jsonShare = new JsonObject(request.getShare());
        jsonShare.putString("debitorPin", _data.pin);
        final String billId = jsonShare.getString(StringConstUtil.Shopping.BillId, "");
        final String fAmount = jsonShare.getString(Const.AppClient.Amount, "0");
        final long f1Amount = jsonShare.getLong("amount", 0);
        log.add("billId", billId);
        log.add("amt", fAmount);
        log.add("amount", f1Amount);
        log.add("serviceId", serviceId);

        log.add("hashmap after", hashMap.toString());
        log.add("call to", "TransferWithGiftContext");
//        log.writeLog();
        hashtableNumber.put("" + msg.cmdPhone, serviceId);

        final long finalAmount = f1Amount == 0 ? DataUtil.strToLong(fAmount) : f1Amount;
        TransferWithGiftContext.build(msg.cmdPhone, serviceId, billId, finalAmount, vertx, giftManager, _data,
                TRANWITHPOINT_MIN_POINT, TRANWITHPOINT_MIN_AMOUNT, logger,
                new Handler<TransferWithGiftContext>() {
                    @Override
                    public void handle(final TransferWithGiftContext context) {
                        if(!hashtableNumber.containsKey("" + msg.cmdPhone))
                        {
                            logger.info("HASH TABLE NUMBER DOES NOT CONTAIN NUMBER " + msg.cmdPhone);
                            return;
                        }
                        hashtableNumber.remove("" + msg.cmdPhone);
                        context.writeLog(logger);
                        ShoppingRequest paymentRequest = null;
                        log.add("func", "ServiceHelper.doPayment");
                        final int voucherPointType = Misc.getVoucherPointType(context);
                        final long useVoucher = context.voucher;
                        log.add("useVoucher", useVoucher);
                        log.add("remind voucher", context.remainVoucher);
                        jsonShare.putNumber("voucher", context.voucher);
                        jsonShare.putNumber("mpoint", context.point);
                        final HashMap<String, String> hashMap_temp = DataUtil.convertJsonToHashMap(jsonShare);
                        try {
                            paymentRequest = ShoppingRequestFactory.createPaymentBackendRequest(hashMap_temp, "0" + msg.cmdPhone, _data.pin, walletType);
                            if (isStoreApp) {
                                paymentRequest = ShoppingRequestFactory.createPaymentBackendAgencyRequest(hashMap_temp, "0" + msg.cmdPhone, _data.pin, walletType);

//                                paymentRequest.getData().putString("agentType", "3");
                            }
                            paymentRequest.setTransId(Long.parseLong(String.format("%s%s", System.currentTimeMillis(), RandomStringUtils.randomNumeric(4))));
                            log.add("payment request", paymentRequest.getJsonObject());
                            //Kiem tra xem co du tien thanh toan hay khong
                        } catch (Exception ex) {
                            paymentRequest = null;
                        }

                        Common.ServiceReq serviceReq = new Common.ServiceReq();
                        serviceReq.ServiceId = serviceId;
                        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
                        final ShoppingRequest paymentRequest_final = paymentRequest;
                        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
                            @Override
                            public void handle(JsonArray objects) {
                                final JsonObject serviceInfo = objects.size() == 0 ? new JsonObject() : (JsonObject) objects.get(0);
                                connectorHTTPPostPathDb.findOne(serviceId.toLowerCase(), new Handler<ConnectorHTTPPostPathDb.Obj>() {
                                    @Override
                                    public void handle(final ConnectorHTTPPostPathDb.Obj connectorPostObj) {
                                        if (connectorPostObj == null) {
                                            //Goi qua bus
                                            log.add("desc", "thong tin shopping qua bus");
                                            processPaySaleOffBillViaBus(context, paymentRequest_final, serviceId, msg, log, typeSource, cashinTransId, channel, fRequest, sock, _data, connectorPostObj.version);
                                            return;
                                        }

                                        log.add("desc", "thong tin thanh toan shopping qua http post");
                                        final int version = connectorPostObj.toJson().getInteger("version", 0);
                                        if (version == 2 || version == 21) {
                                            if (isStoreApp) {
                                                jsonShare.putNumber("source", 5);
                                            } else {
                                                jsonShare.putNumber("source", 2);
                                            }
                                        }
                                        log.add("version", version);
                                        fillProxyReqDataV2(log, sock, msg, _data, jsonShare, serviceId, version, serviceInfo, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(final JsonObject fillShare) {
                                                //Get Info Store
                                                getStoreInfo("0" + msg.cmdPhone, new Handler<AgentsDb.StoreInfo>() {
                                                    @Override
                                                    public void handle(AgentsDb.StoreInfo storeInfo) {
                                                        JsonObject joTransfer = paymentRequest_final == null ? fillShare : (2 == version || 21 == version) ? fillShare : paymentRequest_final.getJsonObject();
                                                        //BEGIN Update 05102016 -- NO VOUCHER WITH NO TAXI GROUP
                                                        if (2 == version || 21 == version || paymentRequest_final == null) {
                                                            //Nhom TAXI
                                                            com.mservice.proxy.entity.v2.ProxyRequest proxyTaxi = new com.mservice.proxy.entity.v2.ProxyRequest(joTransfer);
                                                            JsonObject joExtra = proxyTaxi.getExtraValue();
                                                            if ("nogroup".equalsIgnoreCase(joExtra.getString("coreGroupId", ""))) {
                                                                context.voucher = 0;
                                                            }
                                                        }
                                                        //END 05102016 -- NO VOUCHER WITH NO TAXI GROUP
                                                        log.add("joTransfer", joTransfer);
                                                        if(storeInfo != null)
                                                        {
                                                           joTransfer.putNumber("agentType", storeInfo.agent_type);
                                                        }
                                                        Misc.getDataForPaymentFromConnector("0" + msg.cmdPhone, connectorPostObj.host, connectorPostObj.port, globalConfig, connectorPostObj.path, vertx, log, serviceId, joTransfer, new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject joReplyFromConnector) {
                                                                int error = joReplyFromConnector.getInteger(StringConstUtil.ERROR, 1000);
                                                                if (error != 0) {
                                                                    //payment response is null
                                                                    log.add("error", "Flow goi Http Post bi loi");
                                                                    // Khong cho thanh toan hoa don, tra ve thong bao
                                                                    MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
                                                                    builderError.setStatus(5); // Tra ve loi cho app
                                                                    builderError.setError(1000000); // EXCEPTION_BILL_PAY
                                                                    builderError.setDesc(StringConstUtil.SHOPPING_FAILING_EXECUTE);
                                                                    Buffer buf = MomoMessage.buildBuffer(
                                                                            MomoProto.MsgType.TRANS_REPLY_VALUE,
                                                                            msg.cmdIndex,
                                                                            msg.cmdPhone,
                                                                            builderError.build()
                                                                                    .toByteArray()
                                                                    );
                                                                    Common mCommon = new Common(vertx, logger, glbCfg);
                                                                    mCommon.writeDataToSocket(sock, buf);
                                                                    log.writeLog();
                                                                    return;
                                                                }

                                                                JsonObject joData = joReplyFromConnector.getObject(BankHelperVerticle.DATA, null);
                                                                if (joData == null) {
                                                                    //payment response is null
                                                                    log.add("error", "joData is null");
                                                                    // Khong cho thanh toan hoa don, tra ve thong bao
                                                                    MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
                                                                    builderError.setStatus(5); // Tra ve loi cho app
                                                                    builderError.setError(1000000); // EXCEPTION_BILL_PAY
                                                                    builderError.setDesc(StringConstUtil.SHOPPING_FAILING_EXECUTE);
                                                                    Buffer buf = MomoMessage.buildBuffer(
                                                                            MomoProto.MsgType.TRANS_REPLY_VALUE,
                                                                            msg.cmdIndex,
                                                                            msg.cmdPhone,
                                                                            builderError.build()
                                                                                    .toByteArray()
                                                                    );
                                                                    Common mCommon = new Common(vertx, logger, glbCfg);
                                                                    mCommon.writeDataToSocket(sock, buf);
                                                                    log.writeLog();
                                                                    return;
                                                                }
                                                                log.add("desc", "thanh toan qua http post");
                                                                processPayBill(joData, typeSource, context, msg, cashinTransId, serviceId, channel, log, fRequest, sock, _data, version);
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        //lay thong tin referal
                    }
                }
        );
    }

    private void fillProxyReqDataV2(final Common.BuildLog log, final NetSocket sock, final MomoMessage msg,
                                          final SockData _data, final JsonObject share, final String serviceId, int version,
                                          final JsonObject serviceInfo,
                                          final Handler<JsonObject> callback) {
        if (21 == version) {
            if ("taxi".equalsIgnoreCase(serviceId)) {
                /*{
                    "serviceCode" : "taxi",
                    "amount": 100000, // so tien
                    "driverNumber": "0123456789",// so dt lai xe
                    "driverName": "XXX", // ten lai xe
                    "tip": 0,// tien tip
                    "voucher": 0,// voucher
                    "mpoint": 0,// point
                    "source": 1 // nguon tien
                    "fee": 0 // phi,
                    //"coreGroupId": "66122"
                }*/
                final String serviceCode = share.getString("serviceCode", "");
                final long amount = share.getLong("amount", 0L); // so tien
                final long voucher = share.getLong("voucher", 0L); // voucher
                final long mpoint = share.getLong("mpoint", 0L); // point
                final int source = share.getInteger("source", 0); // nguon tien
                final long fee = share.getLong("fee", 0L); // phi
                final String driverNumber = share.getString("driverNumber", ""); // so dt lai xe
                final String driverName = share.getString("driverName", ""); // ten lai xe
                final long tip = share.getLong("tip", 0L); // tien tip
                final String messageDriver = share.getString("messageDriver", "");
                final JsonObject json = new JsonObject();
                json.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.GET_GROUP_AGENT);
                json.putString("mmphone", driverNumber);
                vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS, json, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        final List<String> coreGroupIds = Arrays.asList(message.body().getString("result", "").split(","));

                        com.mservice.proxy.entity.v2.ProxyRequest proxyRequest = ProxyRequestFactory.createBackendRequest(ProxyRequestType.PAYBILL);
                        // common info
                        proxyRequest.setDebitor("0" + msg.cmdPhone);
                        proxyRequest.setDebitorPin(_data.pin);
                        proxyRequest.setAmount(amount + tip); // amount + tip
                        proxyRequest.setCashSource(MomoProto.TranHisV1.SourceFrom.valueOf(source).getNumber());
                        proxyRequest.setDebitorWalletType(WalletType.MOMO.getCode());
                        proxyRequest.setRequestId(Long.parseLong(String.format("%s%s", System.currentTimeMillis(), RandomStringUtils.randomNumeric(4))) + "");
                        proxyRequest.setServiceCode(serviceCode);
                        proxyRequest.setVoucher(voucher);
                        proxyRequest.setMpoint(mpoint);
                        proxyRequest.setFee(fee);
                        // vic info
                        proxyRequest.setReference1(driverNumber); // so dt lai xe
                        proxyRequest.setReference2(driverName); // ten lai xe
                        proxyRequest.addExtraValue("tip", tip + "");

                        JsonObject billCfg = new JsonObject(serviceInfo.getString(colName.ServiceCols.BILLPAY, "{}"));
                        JsonArray taxiBrands = billCfg.getArray("brand", new JsonArray());
                        List<String> noGroups = Arrays.asList(billCfg.getString("brandNoGroupId", "").split(","));
                        boolean isTaxi = false;
                        boolean isEU = false;

                        for(Object brand: taxiBrands) {
                            JsonObject brandJson = (JsonObject)brand;
                            int brandG = brandJson.getInteger("coreGroupId", 0);
                            for (String g : coreGroupIds) {
                                if (noGroups.contains(g)) {
                                    isEU = true;
                                    continue;
                                }
                                if (!"".equalsIgnoreCase(g) && Integer.parseInt(g) == brandG) {
                                    isTaxi = true;
                                    proxyRequest.addExtraValue("coreGroupId", g);
                                    String agent = brandJson.getString("agent", "");
                                    proxyRequest.setCreditor(agent); // agent nhan tien
                                    proxyRequest.addExtraValue("brandName", brandJson.getString("brandName", ""));
                                    log.add("taxi agent", agent);
                                    break;
                                }
                            }
                        }

                        if (!isTaxi && isEU) {
                            proxyRequest.addExtraValue("coreGroupId", "nogroup");
                            proxyRequest.setVoucher(0); //Update 05102016 -- NO VOUCHER WITH NO TAXI GROUP
                            String agent = billCfg.getString("agentNoGroup");
                            proxyRequest.setCreditor(agent); // agent nhan tien
                            String brandNameNoGroup = billCfg.getString("brandNameNoGroupNoti");
                            proxyRequest.addExtraValue("brandName", brandNameNoGroup);
                            log.add("taxi agent", agent);
                        }
                        proxyRequest.addExtraValue("messageDriver", messageDriver);
                        callback.handle(proxyRequest.getJsonObject());
                        return;
                    }
                });

            } else {
                callback.handle(share);
                return;
            }
        } else {
            callback.handle(share);
            return;
        }
    }

    private void getStoreInfo(String momoPhone, final Handler<AgentsDb.StoreInfo> callBack)
    {
        if(isStoreApp)
        {
            agentsDb.getOneAgent(momoPhone, "getStoreInfo | TransferProcess", new Handler<AgentsDb.StoreInfo>() {
                @Override
                public void handle(AgentsDb.StoreInfo storeInfo) {
                    callBack.handle(storeInfo);
                }
            });
        }
        else {
            callBack.handle(null);
        }
    }

    private void processPaySaleOffBillViaBus(final TransferWithGiftContext context, final ShoppingRequest paymentRequest_final, final String serviceId, final MomoMessage msg, final Common.BuildLog log, final int typeSource, final long cashinTransId, final String channel, final MomoProto.TranHisV1 fRequest, final NetSocket sock, final SockData _data, final int version) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SHOPPING_VERTICLE_BY_SERVICE_ID;
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject jsonVerticle = message.body();

                ViaConnectorObj viaConnectorObj = new ViaConnectorObj(jsonVerticle);
                if(viaConnectorObj.IsViaConnectorVerticle)
                {
                    logger.info(msg.cmdPhone + " --->  " + paymentRequest_final.getJsonObject());
//                    log.writeLog();

                    vertx.eventBus().sendWithTimeout(viaConnectorObj.BusName, paymentRequest_final.getJsonObject(), 60 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {

                            if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null && messageAsyncResult.result().body() != null) {
                                log.add("error", "get info from connector");
                                log.add("body String", messageAsyncResult.result().body().toString());
                                log.add("body", messageAsyncResult.result().body());
                                processPayBill(messageAsyncResult.result().body(), typeSource, context, msg, cashinTransId, serviceId, channel, log, fRequest, sock, _data, version);
                            } else {
                                //Time out
                                log.add("error", "timeout shopping from connector");
                                // Khong cho thanh toan hoa don, tra ve thong bao
                                MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
                                builderError.setStatus(5); // Tra ve loi cho app
                                builderError.setError(1000000); // EXCEPTION_BILL_PAY
                                builderError.setDesc(StringConstUtil.SHOPPING_WAITING_EXECUTE);
                                Buffer buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        builderError.build()
                                                .toByteArray()
                                );
                                Common mCommon = new Common(vertx, logger, glbCfg);
                                mCommon.writeDataToSocket(sock, buf);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }
                else
                {
                    log.add("desc", "Thieu du lieu trong file momo.json");
                    log.writeLog();

                    return;
                }
            }
        });
    }

    private void processPayBill(final JsonObject joInfo, int typeSource, final TransferWithGiftContext context, final MomoMessage msg, long cashinTransId, final String serviceId, String channel, final Common.BuildLog log, final MomoProto.TranHisV1 fRequest, final NetSocket sock, final SockData _data, final int version) {
        if (checkNewAPI(serviceId, version)) {
            processPayBillV2(joInfo, typeSource, context, msg, cashinTransId, serviceId, channel, log, fRequest, sock, _data, version);
            return;
        }
        final PaymentResponse paymentResponse = new PaymentResponse(joInfo);

        if (paymentResponse != null) {
            // sent result to connector with source money is creadit card
            if(typeSource != 0){
                VMRequest.sentResultConnectorSourceVisa(vertx, sbsGetBusAddress(), context.tranId, "0" + msg.cmdPhone, cashinTransId, serviceId, paymentResponse.getResultCode() == 0, channel, log, context.tranType, serviceId);
            }
            log.add("paymentResponse", paymentResponse.getJsonObject().toString());
            log.add("error", "payment response from connector is good, ngon lanh");
            if (paymentResponse.getResultCode() == 0) {
                log.add("error", "yeah giao dich thanh cong");
            } else {
                log.add("error", "oi, giao dich that bai roi");
                MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
                builderError.setStatus(5); // Tra ve loi cho app
                builderError.setError(paymentResponse.getResultCode()); // EXCEPTION_BILL_PAY
                builderError.setDesc(StringConstUtil.SHOPPING_FAILING_EXECUTE);
                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builderError.build()
                                .toByteArray()
                );
//                Common mCommon = new Common(vertx, logger);
//                mCommon.writeDataToSocket(sock, buf);
                ServiceHelper.createTransactionShoppingHistory(vertx, msg.cmdIndex, msg.cmdPhone, joInfo, fRequest, mCom, sock, log, serviceId, version);
                long tranId = paymentResponse.getRequest() != null ? paymentResponse.getRequest().getCoreTransId() : 0;
                Misc.buildPayOneBillNotiAndSend(paymentResponse.getNotificationHeader(),
                        paymentResponse.getNotificationContent(), tranId, msg.cmdPhone, msg.cmdIndex, vertx);
                log.writeLog();
                return;
            }
            log.add("payment response", paymentResponse.getJsonObject());
            log.add("comment", paymentResponse.getTransactionComment());
            String pr = paymentResponse.getRequest() != null ? paymentResponse.getRequest().toString() : "null";
            final long amount = paymentResponse.getRequest() != null && paymentResponse.getRequest().getAmount() > 0 ? paymentResponse.getRequest().getAmount() : 0;
            log.add("request", pr);
            log.add("amount", amount);
            final int timer = bankCfg.getInteger(StringConstUtil.TIMER, 5);
            log.add("TIMER", timer);
            log.add("======>0" + msg.cmdPhone, paymentResponse.getResultCode());
            logger.info(("======>0" + msg.cmdPhone + " " + paymentResponse.getResultCode()));
            final long tranId = paymentResponse.getRequest() != null ? paymentResponse.getRequest().getCoreTransId() == -1 ? paymentResponse.getJsonObject().getLong("coreTransId", System.currentTimeMillis()) : paymentResponse.getRequest().getCoreTransId() : 0;
            vertx.setTimer(timer * 1000L, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                    mCom.sendCurrentAgentInfoWithCallback(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            ServiceHelper.createTransactionShoppingHistory(vertx, msg.cmdIndex, msg.cmdPhone, joInfo, fRequest, mCom, sock, log, serviceId, version);

                            Misc.buildPayOneBillNotiAndSend(paymentResponse.getNotificationHeader(),
                                    paymentResponse.getNotificationContent(), tranId, msg.cmdPhone, msg.cmdIndex, vertx);

                        }
                    });
                }
            });
            log.add("0" + msg.cmdPhone, paymentResponse.getResultCode());
            if (paymentResponse.getResultCode() == 0) {
                log.add("0" + msg.cmdPhone, "BEGIN PROMOTION PAY_SALE_OFF_BILL");
                long fee = paymentResponse.getRequest() != null && paymentResponse.getRequest().getFee() > 0 ? paymentResponse.getRequest().getFee() : 0;
                promotionProcess.executePromotion(paymentResponse.getRequest().getBillId(), MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE, "",
                        msg.cmdPhone, tranId, amount, _data, serviceId, context, fee, fRequest.getSourceFrom(), log, new JsonObject(), new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject event) {

                            }
                        });

                processPromotion(sock, paymentResponse.getRequest().getBillId(), MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE, "",
                        msg, tranId, amount, _data, serviceId, context, fee, fRequest.getSourceFrom(), log, new JsonObject());

            }
            context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject result) {
                    if (result.getInteger("error", -1000) != 0) {
                        log.add("use gift", "use gift error");
                        log.add("error", result.getInteger("error"));
                        log.add("desc", SoapError.getDesc(result.getInteger("error")));
                    }

                    //Kiem tra ket qua xoa trong queuedgift
//                    if (result.getInteger("error", -1000) == 0) {
//                        //BEGIN 0000000052 Iron MAN
//                        JsonArray giftArray = result.getArray(StringConstUtil.IronManPromo.GIFT_ID_ARRAY, new JsonArray());
//                        if (giftArray.size() > 0) {
//                            updateIronManVoucher(msg, log, context, giftArray);
//                            promotionProcess.updateOctoberPromoVoucherStatus(log, giftArray, "0" + msg.cmdPhone);
//                        }
//                        //END 0000000052 IRON MAN
//                    }
                    //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
                    context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject result) {
                            if (result.getInteger("error", -1000) != 0) {
                                logger.error("returnMomo e  rror!");
                            }

                            if (context.error == 0) {
                                //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

//                                                            requestVisaPoint(msg, log, context, sock, _data);
                                //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                //force to update current amount
                                mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);


                            }

                            giftTranDb.save(new GiftTran(context), null);
                        }
                    });
                    log.writeLog();
                }
            });
//            log.writeLog();
        } else {
            //payment response is null
            log.add("error", "payment response from connector is null hehehe, lam lai di nhe");
            // Khong cho thanh toan hoa don, tra ve thong bao
            MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
            builderError.setStatus(5); // Tra ve loi cho app
            builderError.setError(1000000); // EXCEPTION_BILL_PAY
            builderError.setDesc(StringConstUtil.SHOPPING_FAILING_EXECUTE);
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.TRANS_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builderError.build()
                            .toByteArray()
            );
            Common mCommon = new Common(vertx, logger, glbCfg);
            mCommon.writeDataToSocket(sock, buf);
            log.writeLog();
            return;
        }
    }

    private void processPayBillV2(final JsonObject joInfo, int typeSource, final TransferWithGiftContext context, final MomoMessage msg, long cashinTransId, final String serviceId, String channel, final Common.BuildLog log, final MomoProto.TranHisV1 fRequest, final NetSocket sock, final SockData _data, final int version) {
        final com.mservice.proxy.entity.v2.ProxyResponse paymentResponse = new com.mservice.proxy.entity.v2.ProxyResponse(joInfo);

        if (paymentResponse != null) {
            final String comment = paymentResponse.getPushInfo() != null && paymentResponse.getPushInfo().getHistory() != null ?
                    paymentResponse.getPushInfo().getHistory().getTransactionContent() : "";
            final String notiHeader = paymentResponse.getPushInfo() != null && paymentResponse.getPushInfo().getNotification() != null ?
                    paymentResponse.getPushInfo().getNotification().getHeader() : "";
            final String notiContent = paymentResponse.getPushInfo() != null && paymentResponse.getPushInfo().getNotification() != null ?
                    paymentResponse.getPushInfo().getNotification().getContent() : "";
            // sent result to connector with source money is creadit card
            if(typeSource != 0){
                VMRequest.sentResultConnectorSourceVisa(vertx, sbsGetBusAddress(), context.tranId, "0" + msg.cmdPhone, cashinTransId, serviceId, paymentResponse.getResultCode() == 0, channel, log, context.tranType, serviceId);
            }
            log.add("paymentResponse", paymentResponse.getJsonObject().toString());
            log.add("error", "payment response from connector is good, ngon lanh");
            if (paymentResponse.getResultCode() == 0) {
                log.add("error", "yeah giao dich thanh cong");
            } else {
                log.add("error", "oi, giao dich that bai roi");
                MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
                builderError.setStatus(5); // Tra ve loi cho app
                builderError.setError(paymentResponse.getResultCode()); // EXCEPTION_BILL_PAY
                builderError.setDesc(StringConstUtil.SHOPPING_FAILING_EXECUTE);
                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builderError.build()
                                .toByteArray()
                );
//                Common mCommon = new Common(vertx, logger);
//                mCommon.writeDataToSocket(sock, buf);
                ServiceHelper.createTransactionShoppingHistory(vertx, msg.cmdIndex, msg.cmdPhone, joInfo, fRequest, mCom, sock, log, serviceId, version);
                long tranId = paymentResponse.getRequest() != null ? paymentResponse.getRequest().getCoreTransId() : 0;
                Misc.buildPayOneBillNotiAndSend(notiHeader, notiContent, tranId, msg.cmdPhone, msg.cmdIndex, vertx);
                log.writeLog();
                return;
            }
            log.add("payment response", paymentResponse.getJsonObject());
            log.add("comment", comment);
            String pr = paymentResponse.getRequest() != null ? paymentResponse.getRequest().toString() : "null";
            final long amount = paymentResponse.getRequest() != null && paymentResponse.getRequest().getAmount() > 0 ? paymentResponse.getRequest().getAmount() : 0;
            log.add("request", pr);
            log.add("amount", amount);
            final int timer = bankCfg.getInteger(StringConstUtil.TIMER, 5);
            log.add("TIMER", timer);
            final long tranId = paymentResponse.getRequest() != null ? paymentResponse.getRequest().getCoreTransId() == -1 ? paymentResponse.getJsonObject().getLong("coreTransId", System.currentTimeMillis()) : paymentResponse.getRequest().getCoreTransId() : 0;
            vertx.setTimer(timer * 1000L, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                    mCom.sendCurrentAgentInfoWithCallback(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            ServiceHelper.createTransactionShoppingHistory(vertx, msg.cmdIndex, msg.cmdPhone, joInfo, fRequest, mCom, sock, log, serviceId, version);

                            Misc.buildPayOneBillNotiAndSend(notiHeader, notiContent, tranId, msg.cmdPhone, msg.cmdIndex, vertx);

                        }
                    });
                }
            });

            if (paymentResponse.getResultCode() == 0) {
                long fee = paymentResponse.getRequest().getFee();
                if (paymentResponse.getBillList() != null && paymentResponse.getBillList().size() > 0) {
                    promotionProcess.executePromotion(paymentResponse.getBillList().get(0).getBillID(), MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE, "",
                            msg.cmdPhone, tranId, amount, _data, serviceId, context, fee, fRequest.getSourceFrom(), log, new JsonObject(), new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject event) {

                                }
                            });
                    processPromotion(sock, paymentResponse.getBillList().get(0).getBillID(), MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE,
                            "", msg, tranId, amount, _data, serviceId, context, fee, fRequest.getSourceFrom(), log, new JsonObject());
                }
                else {
                    promotionProcess.executePromotion(paymentResponse.getRequest().getReference1(), MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE, "",
                            msg.cmdPhone, tranId, amount, _data, serviceId, context, fee, fRequest.getSourceFrom(), log, new JsonObject(), new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject event) {

                                }
                            });
                }
            }
            context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject result) {
                    if (result.getInteger("error", -1000) != 0) {
                        log.add("use gift", "use gift error");
                        log.add("error", result.getInteger("error"));
                        log.add("desc", SoapError.getDesc(result.getInteger("error")));
                    }

                    //Kiem tra ket qua xoa trong queuedgift
//                    if (result.getInteger("error", -1000) == 0) {
//                        //BEGIN 0000000052 Iron MAN
//                        JsonArray giftArray = result.getArray(StringConstUtil.IronManPromo.GIFT_ID_ARRAY, new JsonArray());
//                        if (giftArray.size() > 0) {
//                            updateIronManVoucher(msg, log, context, giftArray);
//                            promotionProcess.updateOctoberPromoVoucherStatus(log, giftArray, "0" + msg.cmdPhone);
//                        }
//                        //END 0000000052 IRON MAN
//                    }
                    //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
                    context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject result) {
                            if (result.getInteger("error", -1000) != 0) {
                                logger.error("returnMomo error!");
                            }

                            if (context.error == 0) {
                                //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

//                                                            requestVisaPoint(msg, log, context, sock, _data);
                                //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                //force to update current amount
                                mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);


                            }

                            giftTranDb.save(new GiftTran(context), null);
                        }
                    });
                    log.writeLog();
                }
            });
//            log.writeLog();
            return;
        } else {
            //payment response is null
            log.add("error", "payment response from connector is null hehehe, lam lai di nhe");
            // Khong cho thanh toan hoa don, tra ve thong bao
            MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
            builderError.setStatus(5); // Tra ve loi cho app
            builderError.setError(1000000); // EXCEPTION_BILL_PAY
            builderError.setDesc(StringConstUtil.SHOPPING_FAILING_EXECUTE);
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.TRANS_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builderError.build()
                            .toByteArray()
            );
            Common mCommon = new Common(vertx, logger, glbCfg);
            mCommon.writeDataToSocket(sock, buf);
            log.writeLog();
            return;
        }
    }

    //BEGIN 0000000052 IRON MAN
    private void updateIronManVoucher(final MomoMessage msg, final Common.BuildLog log, final TransferWithGiftContext context,final JsonArray giftArray)
    {
        log.add("func", "updateIronManVoucher");
        ironManPromoGiftDB.findOne("0" + msg.cmdPhone, new Handler<IronManPromoGiftDB.Obj>() {
            @Override
            public void handle(IronManPromoGiftDB.Obj ironmanGiftObj) {
                log.add("ironmanGiftObj", ironmanGiftObj);
                if(ironmanGiftObj != null)
                {
                    JsonObject joUpdate = new JsonObject();
                    for(int i = 0; i < giftArray.size(); i++)
                    {
                        if(ironmanGiftObj.gift_id_2.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_2, true);
                        }
                        else if(ironmanGiftObj.gift_id_3.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_3, true);
                        }
                        else if(ironmanGiftObj.gift_id_4.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_4, true);
                        }
                        else if(ironmanGiftObj.gift_id_5.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_5, true);
                        }
                        else if(ironmanGiftObj.gift_id_6.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_6, true);
                        }
                        else if(ironmanGiftObj.gift_id_7.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_7, true);
                        }
                        else if(ironmanGiftObj.gift_id_8.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_8, true);
                        }
                        else if(ironmanGiftObj.gift_id_9.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_9, true);
                        }
                        else if(ironmanGiftObj.gift_id_10.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_10, true);
                        }
                        else if(ironmanGiftObj.gift_id_11.equalsIgnoreCase(giftArray.get(i).toString()))
                        {
                            joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_11, true);
                        }
                    }
                    ironManPromoGiftDB.updatePartial("0" + msg.cmdPhone, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    });
                }
            }
        });
    }
    //END 0000000052 IRON MAN


//    public void processPayOneBill(final SockData _data
//            , final MomoMessage msg
//            , final Handler<JsonObject> callback) {
//
//
//        final NetSocket sock = null;
//        //SockData _data = new SockData(vertx, logger);
//
//        final String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;
//
//        final Common.BuildLog log = new Common.BuildLog(logger);
//        log.setPhoneNumber("0" + msg.cmdPhone);
//        log.add("func", "processPayOneBill");
//
//        //lay thong tin referal
//        SoapProto.keyValuePair.Builder referalBuilder = null;
//
//        referalBuilder = SoapProto.keyValuePair.newBuilder();
//        referalBuilder.setKey(Const.REFERAL)
//                .setValue("0" + _data.getPhoneObj().referenceNumber);
//
//
//        //HashMap<String, String> hashMap = Misc.getKeyValuePairs(request.getKvpList());
//        String tmpServiceId = (request.getPartnerId() == null || request.getPartnerId().isEmpty() ? "" : request.getPartnerId());
//
//        tmpServiceId = "".equalsIgnoreCase(tmpServiceId) ?
//                (hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : tmpServiceId)
//                : tmpServiceId;
//
//        final String serviceId = tmpServiceId;
//
//
//        String tmpBillId = (request.getBillId() == null || request.getBillId().isEmpty() ? "" : request.getBillId());
//        tmpBillId = "".equalsIgnoreCase(tmpBillId) ?
//                (hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : tmpBillId)
//                : tmpBillId;
//        final String billId = tmpBillId;
//
//        //dgd.start
//
//        final String retailer = hashMap.containsKey(Const.DGD.Retailer) ? hashMap.get(Const.DGD.Retailer) : "";
//        final String cusnum = hashMap.containsKey(Const.DGD.CusNumber) ? hashMap.get(Const.DGD.CusNumber) : "";
//        final boolean isRetailer = (Const.DGDValues.Retailer == DataUtil.strToInt(retailer)
//                && DataUtil.strToInt(cusnum) > 0);
//        //dgd.end
//
//        long tmpAmount = hashMap.containsKey(Const.AppClient.Amount) ? DataUtil.stringToUNumber(hashMap.get(Const.AppClient.Amount)) : 0;
//        final int qty = hashMap.containsKey(Const.AppClient.Quantity) ? DataUtil.strToInt(hashMap.get(Const.AppClient.Quantity)) : 1;
//        final String cusName = hashMap.containsKey(Const.AppClient.FullName) ? hashMap.get(Const.AppClient.FullName) : "";
//        final String cusInfo = hashMap.containsKey("inf") ? hashMap.get("inf") : "";
//        final String cusPhone = hashMap.containsKey(Const.AppClient.Phone) ? hashMap.get(Const.AppClient.Phone) : "";
//        final String cusAddress = hashMap.containsKey(Const.AppClient.Address) ? hashMap.get(Const.AppClient.Address) : "";
//        final String cusEmail = hashMap.containsKey("email") ? hashMap.get("email") : "";
//
//        tmpAmount = (tmpAmount > 0 ? tmpAmount : request.getAmount());
//        final long fAmount = tmpAmount * qty;
//
//        String share = request.getShare();
//        log.add("share", share);
//        JsonArray jsonArrayShare = share.equalsIgnoreCase("") ? new JsonArray() : new JsonArray(share);
//        String phoneToSms = "";
//        if (jsonArrayShare.size() > 0) {
//            for (Object o : jsonArrayShare) {
//                phoneToSms = ((JsonObject) o).getString(Const.DGD.CusNumber, "");
//                if (!phoneToSms.equalsIgnoreCase(""))
//                    break;
//            }
//        }
//        final String smsPhone = phoneToSms.equalsIgnoreCase("") ? cusnum : phoneToSms;
//        log.add("smsPhone", smsPhone);
//        log.add("phoneToSms", phoneToSms);
//        SoapProto.PayOneBill.Builder builder = SoapProto.PayOneBill.newBuilder();
//        builder.setPin(_data.pin)
//                .setProviderId(serviceId)
//                .setBillId(billId)
//                .setChannel(channel)
//                .setAmount(fAmount);
//
//        builder.addKeyValuePairs(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));
//
//        Buffer tmpPOB;
//
//        log.add("isRetailer", isRetailer);
//
//        if (referalBuilder != null) {
//            //them key
//            builder.addKeyValuePairs(referalBuilder);
//
//            tmpPOB = MomoMessage.buildBuffer(
//                    SoapProto.MsgType.PAY_ONE_BILL_VALUE,
//                    msg.cmdIndex,
//                    msg.cmdPhone,
//                    builder.build()
//                            .toByteArray()
//            );
//        } else {
//            tmpPOB = MomoMessage.buildBuffer(
//                    SoapProto.MsgType.PAY_ONE_BILL_VALUE,
//                    msg.cmdIndex,
//                    msg.cmdPhone,
//                    builder.build()
//                            .toByteArray()
//            );
//        }
//
//        final Buffer payOneBill = tmpPOB;
//
//        // Chay nhu cu nhe.
//        Misc.getViaCoreService(vertx, serviceId, isStoreApp, new Handler<ViaConnectorObj>() {
//            @Override
//            public void handle(final ViaConnectorObj viaConnectorObj) {
//                log.add("via core connector", viaConnectorObj.IsViaConnectorVerticle);
//                log.add("busname", viaConnectorObj.BusName);
//                log.add("billpay", viaConnectorObj.BillPay);
//
//                if (viaConnectorObj.IsViaConnectorVerticle) {
//
//                    log.add("call to", "TransferWithGiftContext");
//                    TransferWithGiftContext.build(msg.cmdPhone, serviceId, billId, fAmount, vertx, giftManager, _data,
//                            TRANWITHPOINT_MIN_POINT, TRANWITHPOINT_MIN_AMOUNT, logger,
//                            new Handler<TransferWithGiftContext>() {
//                                @Override
//                                public void handle(final TransferWithGiftContext context) {
//                                    context.writeLog(logger);
//                                    log.add("func", "ServiceHelper.doPayment");
//                                    final int voucherPointType = Misc.getVoucherPointType(context);
//                                    final long useVoucher = context.voucher;
//                                    log.add("useVoucer", useVoucher);
//                                    ProxyRequest paymentRequest = ConnectorCommon.createPaymentRequest(
//                                            "0" + msg.cmdPhone
//                                            , _data.pin
//                                            , fRequest.getPartnerId()
//                                            , viaConnectorObj.BillPay
//                                            , fRequest.getBillId()
//                                            , context.amount
//                                            , context.voucher
//                                            , context.point
//                                            , qty,
//                                            cusName, cusInfo, cusPhone, cusAddress, cusEmail);
//
//                                    log.add("isStoreApp ---->", isStoreApp);
//
//                                    if (isStoreApp) {
//                                        paymentRequest = ConnectorCommon.createPaymentBackEndAgencyRequest(
//                                                "0" + msg.cmdPhone
//                                                , _data.pin
//                                                , fRequest.getPartnerId()
//                                                , viaConnectorObj.BillPay
//                                                , fRequest.getBillId()
//                                                , context.amount
//                                                , context.voucher
//                                                , context.point
//                                                , qty,
//                                                cusName, cusInfo, smsPhone, cusAddress, cusEmail);
//                                    }
//                                    log.add("cusnum ---->", cusnum);
//                                    log.add("cusPhone ---->", cusPhone);
//                                    log.add("payment request content", paymentRequest.toString());
//                                    ServiceHelper.
//                                            doPayment(sock, msg, vertx,
//                                                    paymentRequest, mCom, fRequest, log,
//                                                    viaConnectorObj.BusName, new Handler<ProxyResponse>() {
//
//                                                        @Override
//                                                        public void handle(final ProxyResponse proxyResponse) {
//                                                            Misc.removeCacheFormData(vertx, serviceId, msg.cmdPhone, isStoreApp, new Handler<JsonObject>() {
//                                                                @Override
//                                                                public void handle(JsonObject jsonObject) {
//                                                                    log.add("desc", jsonObject.encodePrettily());
//                                                                }
//                                                            });
//                                                            final int rcode = proxyResponse.getProxyResponseCode();
//                                                            context.tranId = proxyResponse.getRequest().getCoreTransId();
//                                                            context.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
//                                                            context.error = proxyResponse.getProxyResponseCode();
//
//                                                            //tra thuong cho promotion girl
//                                                            JsonObject tranReply = new JsonObject();
//                                                            tranReply.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
//                                                            tranReply.putNumber(colName.TranDBCols.ERROR, context.error);
//                                                            log.add("rcode", rcode);
//                                                            log.add("func", "Save bill to check on day");
//
//                                                            context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
//                                                                @Override
//                                                                public void handle(final JsonObject result) {
//                                                                    if (result.getInteger("error", -1000) != 0) {
//                                                                        log.add("use gift", "use gift error");
//                                                                        log.add("error", result.getInteger("error"));
//                                                                        log.add("desc", SoapError.getDesc(result.getInteger("error")));
//                                                                    }
//
//                                                                    //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
//                                                                    context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
//                                                                        @Override
//                                                                        public void handle(JsonObject results) {
//                                                                            if (results.getInteger("error", -1000) != 0) {
//                                                                                logger.error("returnMomo error!");
//                                                                            }
//
//                                                                            if (context.error == 0) {
//                                                                                if (context.voucher != 0) {
//                                                                                    JsonObject jsonGift = new JsonObject();
//                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.NUMBER, "0" + msg.cmdPhone);
//                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.BILL_ID, billId);
//                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.SERVICE_ID, serviceId);
//                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.GIFT_ID, context.giftId);
//                                                                                    insertPayOneHcPruBill(jsonGift);
//
//                                                                                }
//
//                                                                                //force to update current amount
//
//                                                                                //reset bill amount here
//                                                                                resetOfflineBillAmount(msg.cmdPhone, serviceId, billId);
//                                                                                //BEGIN 0000000004
//                                                                                log.add("index", msg.cmdIndex);
//                                                                                log.add("func", "Bat dau tra thuong billpay hehe");
//                                                                                log.add("tranId", context.tranId);
//                                                                                log.add("tranType", context.tranType);
//                                                                                log.add("phone", msg.cmdPhone);
//                                                                                log.add("serviceId", serviceId);
//                                                                                BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, context.tranType,
//                                                                                        context.tranId, serviceId, "", new Handler<JsonObject>() {
//                                                                                            @Override
//                                                                                            public void handle(JsonObject jsonObject) {
//                                                                                                log.add("requestBillPayPromo", jsonObject);
//                                                                                                log.writeLog();
//                                                                                            }
//                                                                                        }
//                                                                                );
//                                                                                //END 0000000004
//
//
//                                                                            }
//
//                                                                            giftTranDb.save(new GiftTran(context), null);
//                                                                        }
//                                                                    });
//                                                                    //dgd
//                                                                    //BEGIN 0000000001 Send SMS to Customer.
////                                                            final ProxyResponse proxyResponseTmp = proxyResponse;
////                                                            if (isRetailer && rcode == 0) {
////                                                                sendSmsBillPayForCustomer(proxyResponseTmp, fAmount, context.tranId, serviceId, billId, cusPhone);
////                                                            }
//                                                                    //END 0000000001 Send SMS to Customer.
//                                                                    log.writeLog();
//                                                                }
//                                                            });
//                                                            //BEGIN 0000000050
//                                                            JsonObject jsonRep = new JsonObject();
//                                                            jsonRep.putNumber("error", rcode);
//                                                            jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
//                                                            jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
//                                                            jsonRep.putString(StringConstUtil.SERVICE, serviceId);
//                                                            if (callback != null) {
//                                                                callback.handle(jsonRep);
//                                                            }
//                                                            //END 0000000050
//
//
//                                                        }
//                                                    }
//                                            );
//                                }
//                            }
//                    );
//
//                    return;
//                } // END (viaConnectorObj.IsViaConnectorVerticle)
//
//                final long ackTime = System.currentTimeMillis();
//
//                //cac giao dich thuc hien 1 buoc qua core
//                TransferWithGiftContext.build(msg.cmdPhone
//                        , serviceId
//                        , billId
//                        , fAmount
//                        , vertx
//                        , giftManager
//                        , _data
//                        , TRANWITHPOINT_MIN_POINT
//                        , TRANWITHPOINT_MIN_AMOUNT, logger, new Handler<TransferWithGiftContext>() {
//                    @Override
//                    public void handle(final TransferWithGiftContext context) {
//                        int voucherPointType = Misc.getVoucherPointType(context);
//
//                        log.add("voucher and point type", voucherPointType);
//                        final long useVoucher = context.voucher;
//                        //qua core 1 buoc
//                        if (voucherPointType != 0) {
//
//                            String specialAgent = Misc.getSpecialAgent(serviceId, billpay_cfg);
//                            log.add("special agent", specialAgent);
//
//                            if ("".equalsIgnoreCase(specialAgent)) {
//                                log.add("vanhanh", "check them tai sao khong lai duoc bill pay cua dich vu " + serviceId + " billpay_cfg");
//                                log.writeLog();
//
//                                JsonObject joTranReply = Misc.getJsonObjRpl(SoapError.SYSTEM_ERROR, 100, fAmount, -1);
//                                Misc.addCustomNumber(joTranReply, cusnum);
//                                mCom.sendTransReply(vertx, joTranReply, ackTime, msg, sock, _data, callback);
//                                return;
//                            }
//
//                            String tBillId = billId;
//                            if ("".equalsIgnoreCase(billId)) {
//                                tBillId = "0" + msg.cmdPhone;
//                            }
//
//                            log.add("func", "tranWithPointAndVoucher");
//                            CoreCommon.tranWithPointAndVoucher(vertx
//                                    , log
//                                    , msg.cmdPhone
//                                    , _data.pin
//                                    , context.point
//                                    , context.voucher
//                                    , fAmount
//                                    , Const.CoreProcessType.OneStep
//                                    , Const.CoreTranType.Billpay
//                                    , specialAgent
//                                    , channel
//                                    //,"0" + msg.cmdPhone
//                                    , tBillId
//                                    , voucherPointType
//                                    , DEFAULT_CORE_TIMEOUT, new Handler<Response>() {
//                                @Override
//                                public void handle(final Response requestObj) {
//
//                                    context.tranId = requestObj.Tid;
//                                    context.error = requestObj.Error;
//                                    context.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
//                                    context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
//                                        @Override
//                                        public void handle(JsonObject result) {
//                                            if (result.getInteger("error", -1000) != 0) {
//                                                log.add("gift", "use gift error");
//                                                log.add("error", result.getInteger("error"));
//                                                log.add("desc", SoapError.getDesc(result.getInteger("error")));
//                                            }
//
//                                            //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
//                                            context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
//                                                @Override
//                                                public void handle(JsonObject result) {
//                                                    if (result.getInteger("error", -1000) != 0) {
//                                                        log.add("return momo", "error");
//                                                    }
//
//                                                    final JsonObject joTranReply = Misc.getJsonObjRpl(requestObj.Error, requestObj.Tid, fAmount, -1);
//                                                    Misc.addCustomNumber(joTranReply, cusnum);
//
//                                                    mCom.sendTransReply(vertx, joTranReply, ackTime, msg, sock, _data, callback);
//
//                                                    //reset bill amount here
//                                                    if (context.error == 0) {
//                                                        resetOfflineBillAmount(msg.cmdPhone, serviceId, billId);
//                                                    }
//
//
//                                                    giftTranDb.save(new GiftTran(context), null);
//
//                                                    log.writeLog();
//                                                }
//                                            }); //END Lay phan du cua GIFT bo vao tui MOMO
//                                        }
//                                    }); // END TRANFER GIFT WITH NEED
//                                    //BEGIN 0000000050
//                                    JsonObject jsonRep = new JsonObject();
//                                    jsonRep.putNumber("error", context.error);
//                                    jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
//                                    jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
//                                    jsonRep.putString(StringConstUtil.SERVICE, serviceId);
//                                    if (callback != null) {
//                                        callback.handle(jsonRep);
//                                    }
//                                    //END 0000000050
//                                }
//                            }); //END TRANSWITH POINT AND VOUCHER
//                        } else {
//
//                            log.add("begin", "soapin verticle");
//                            //chay cac luong cu thanh toan qua soapin
//                            vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, payOneBill, new Handler<Message<JsonObject>>() {
//                                @Override
//                                public void handle(Message<JsonObject> result) {
//
//                                    JsonObject joReply = result.body();
//
//                                    log.add("end", "soapin verticle");
//                                    log.writeLog();
//
//                                    Misc.addCustomNumber(joReply, cusnum);
//                                    mCom.sendTransReply(vertx
//                                            , joReply
//                                            , ackTime
//                                            , msg
//                                            , sock
//                                            , _data
//                                            , callback);
//
//                                    int error = joReply.getInteger(colName.TranDBCols.ERROR, -100);
//                                    //reset amount by provider and bill
//                                    if (error == 0) {
//
//
//                                        resetOfflineBillAmount(msg.cmdPhone, serviceId, billId);
//                                        //BEGIN 0000000004
//                                        log.add("index", msg.cmdIndex);
//                                        BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, context.tranType,
//                                                context.tranId, serviceId, "", new Handler<JsonObject>() {
//                                                    @Override
//                                                    public void handle(JsonObject jsonObject) {
//                                                        log.add("requestBillPayPromo", jsonObject);
//                                                        log.writeLog();
//                                                    }
//                                                }
//                                        );
//                                        //END 0000000004
//
//
//                                    }
//
//
//                                    //BEGIN 0000000050
//                                    JsonObject jsonRep = new JsonObject();
//                                    jsonRep.putNumber("error", error);
//                                    jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
//                                    jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
//                                    jsonRep.putString(StringConstUtil.SERVICE, serviceId);
//                                    if (callback != null) {
//                                        callback.handle(jsonRep);
//                                    }
//                                    //END 0000000050
//                                }
//                            });
//                        }
//                    }
//                }); // END With voucher and point
//
//                //cap nhat session time
//                mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
//
//            }
//        }); // END getViaCoreService
//    }
//    /**
//     * after paying successfull, we want to reset the total amount of (provider,billid)
//     *
//     * @param providerId the serviceId or provider id
//     * @param billId     the bill that has been paied
//     * @param number     the wallet we want reset the total amount of bill.
//     */
//    private void resetOfflineBillAmount(int number, String providerId, String billId) {
//        billsDb.resetBillAmount(number, providerId, billId, new Handler<Boolean>() {
//            @Override
//            public void handle(Boolean aBoolean) {
//            }
//        });
//    }

    public void processM2MerchantTransfer(final String pin, final SockData _data
            , final String billId
            , final MerchantKeyManageDb.Obj meObj
            , final String serviceId
            , long customer_amount
            , final String receiverNumber
            , final String senderNumber
            , final String description, final JsonObject jsonDataClient,final Handler<JsonObject> callback) {

        final MomoMessage msg = _data != null ? new MomoMessage(0, 0, _data.getNumber(), new byte[1])
                                 : new MomoMessage(0, 0, DataUtil.strToInt(receiverNumber), new byte[1]);
        final NetSocket sock = null;
        final String agent = senderNumber;
        String mpin = pin;

        final String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        //request tu client
        //key: service_name
        //value:
        //thanh toan QRCode: QRCode
        //vay tien tu ban be : muon_tien
        String value_billId = (billId == null ? "" : billId);
        final long amount = customer_amount;

        final String customNumber = (receiverNumber == null || "".equalsIgnoreCase(receiverNumber)) ? "" : receiverNumber;

        SoapProto.M2MTransfer.Builder builder = SoapProto.M2MTransfer.newBuilder();
        builder.setAgent(agent) //Thong tin thang chuyen tien
                .setMpin(mpin)
                .setPhone(receiverNumber) //Thong tin so dien thoai nguoi nhan
                .setChannel(channel)
                .setAmount(amount)
                .setNotice(description); //Description


        //neu co key thi them vao
        if (value_billId != null && !"".equalsIgnoreCase(value_billId)) {

            //dua vao service mode
            if ("qrcode".equalsIgnoreCase(value_billId)) {
                builder.addKvps(SoapProto.keyValuePair.newBuilder()
                        .setKey(Const.SERCICE_MODE)
                        .setValue(value_billId));
            } else {
                //dua vao service name
                builder.addKvps(SoapProto.keyValuePair.newBuilder()
                        .setKey(Const.SERVICE_NAME)
                        .setValue(value_billId));

            }
        }


//        long client_amount = jsonDataClient.getLong(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
//        String client_merchantCode = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.MERCHANT_CODE, "");
        String client_username = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.USER_NAME, "");
        String client_billId = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.BILL_ID, "");
//        String client_phoneNumber = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.PHONENUMBER, "");
        final String tranId = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.TRANSID, "");

        builder.addKvps(SoapProto.keyValuePair.newBuilder()
                .setKey("merchant_offline")
                .setValue("m2m"));
        builder.addKvps(SoapProto.keyValuePair.newBuilder()
                .setKey("billId").setValue(client_billId));
        builder.addKvps(SoapProto.keyValuePair.newBuilder()
                .setKey("tranId").setValue(tranId));
        builder.addKvps(SoapProto.keyValuePair.newBuilder()
                .setKey("username").setValue(client_username));

        final JsonObject jsonKvp = new JsonObject();
        jsonKvp.putString("merchant_offline","m2m");
        jsonKvp.putString("billId",client_billId);
        jsonKvp.putString("customer_tranId",tranId);
        jsonKvp.putString("username",client_username);
        final long ackTime = System.currentTimeMillis();
        //final int rcvPhone = Integer.valueOf(receiverNumber);
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processM2MerchantTransfer");
        log.add("rcv number", "0" + receiverNumber);
        log.add("amount", amount);
        log.add("start getMerchantStatus", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
        final String orgComment = description;
        builder.addKvps(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));
        //build buffer --> soap verticle
        final Buffer m2mTransfer = MomoMessage.buildBuffer(
                SoapProto.MsgType.M2MERCHANT_TRANSFER_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build().toByteArray()
        );

        log.add("do", "M2Merchant");
        //todo lam theo luong M2M
        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS
                , m2mTransfer
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                final JsonObject tranRpl = result.body();
                Misc.addCustomNumber(tranRpl, customNumber);
                final int error = tranRpl.getInteger(colName.TranDBCols.ERROR, 0);
                log.add("tranRpl", tranRpl);
//                log.writeLog();

                if (error == 0) {
                    log.add("existsMMPhone", "processM2MerchantTransfer");
                    log.writeLog();
                    Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE, amount, "0" + msg.cmdPhone, "0" + receiverNumber, new Handler<FeeDb.Obj>() {
                        @Override
                        public void handle(FeeDb.Obj obj) {
                            long fee = obj == null ? 1000 : obj.STATIC_FEE;

                                                    /*//dua phi vao key fee cua shared
                                                    tranRpl.putNumber(Const.AppClient.Fee, fee);*/

                            String newComment = "Số tiền đã chuyển: " + Misc.formatAmount(amount).replaceAll(",", ".") + "đ"
                                    + "\n" + "Phí dịch vụ: " + Misc.formatAmount(fee).replaceAll(",", ".") + "đ"
                                    + "\n" + "Lời nhắn: " + orgComment;
                            long newAmount = amount + fee;

                            //tien goc
                            tranRpl.putNumber(Const.AppClient.OldAmount, amount);
                            //comment goc
                            tranRpl.putString(Const.AppClient.OldComment, orgComment);

                            //tao comment moi
                            tranRpl.putString(Const.AppClient.NewComment, newComment);
                            //tao so tien moi
                            tranRpl.putNumber(colName.TranDBCols.AMOUNT, newAmount);

                            Misc.addCustomNumber(tranRpl, customNumber);
                            if(meObj != null)
                            {
                                buildTranHisAndSendNotiForM2M(meObj, jsonKvp, tranRpl, serviceId, senderNumber, receiverNumber, error, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {
                                    }
                                });
                            }
                            else if("lixi".equalsIgnoreCase(description)) {
                                String titleNoti = jsonDataClient.getString("title", "");
                                String bodyNoti = jsonDataClient.getString("body", "");
                                String code = jsonDataClient.getString("code", "");
                                String name = jsonDataClient.getString("name", "");
                                sendPopupForReceiver(receiverNumber, tranRpl, code, name, titleNoti, bodyNoti);
                                sendNotiLixi(log, tranRpl.getLong("tranId", 0), receiverNumber, amount, titleNoti, bodyNoti);
                            }
                            callback.handle(tranRpl);

                        }
                    });
                }
                else if(error == 14)
                {
                    final long tid = tranRpl.getLong("tranId", 0);
                    saveM2NForTool(channel, tid, 0, amount, receiverNumber, agent, pin, callback);
                }
                else {
                    Misc.addCustomNumber(tranRpl, customNumber);
                        if(meObj != null)
                        {
                            buildTranHisAndSendNotiForM2M(meObj, jsonKvp, tranRpl, serviceId, senderNumber, receiverNumber, error, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jsonObject) {

                                }
                            });
                        }
                        else if("lixi".equalsIgnoreCase(description)) {
                             String titleNoti = jsonDataClient.getString("title", "");
                             String bodyNoti = jsonDataClient.getString("body", "");
                            String code = jsonDataClient.getString("code", "");
                            String name = jsonDataClient.getString("name", "");
                            sendPopupForReceiver(receiverNumber, tranRpl, code, name, titleNoti, bodyNoti);
                            sendNotiLixi(log, tranRpl.getLong("tranId", 0), receiverNumber, amount, titleNoti, bodyNoti);
                        }
                    callback.handle(tranRpl);
                }
            }
        });
        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis()); //So dien thoai
//            }
//        });
    }
    //Thanh toan voi nguon offline

    public void sendNotiLixi(Common.BuildLog log, final long tid,final String phoneNumber, final long amount,final String title,final String body) {

        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.M2M_VALUE);
        jsonTrans.putString(colName.TranDBCols.COMMENT, body);
        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tid);
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amount);
        jsonTrans.putNumber(colName.TranDBCols.STATUS, 0);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        jsonTrans.putString(colName.TranDBCols.BILL_ID, "");
        jsonTrans.putNumber(colName.TranDBCols.IO, 1);
        jsonTrans.putNumber(colName.TranDBCols.STATUS, 4);
        Misc.sendingStandardTransHisFromJsonWithCallback(vertx, transDb, jsonTrans, new JsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                final Notification noti = new Notification();
                noti.priority = 1;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                noti.caption = title;// "Nhận thưởng quà khuyến mãi";
                noti.body = body;
                noti.tranId = tid;
                noti.time = new Date().getTime();
                noti.receiverNumber = DataUtil.strToInt(phoneNumber);
                Misc.sendNoti(vertx, noti);
            }
        });


        log.writeLog();

        logger.info("Nhan tien 100k momo Thanh cong");
        return;
    }


    private void buildTranHisAndSendNotiForPayBill(ProxyResponse proxyResponse, String serviceId, String phone_number, int error, Handler<JsonObject> callback)
    {
        //buildTranHisAndSendNotiForMerchant(proxyResponse, phone_number, merchantNumber, error, callback);
        buildTranHisAndSendNotiForCustomer(proxyResponse, serviceId, phone_number, error, callback);
    }

    public void buildTranHisAndSendNotiForM2M(MerchantKeyManageDb.Obj meObj, JsonObject jsonExtra, JsonObject jsonFromSoap, String serviceId, String phone_number, String merchantNumber, int error, Handler<JsonObject> callback)
    {
        //buildTranHisAndSendNotiForMerchant(proxyResponse, phone_number, merchantNumber, error, callback);
        buildTranHisAndSendNotiM2MForCustomer(meObj, jsonExtra, jsonFromSoap, serviceId, phone_number, merchantNumber, error, callback);
    }

    /**
     * Build tranhis and send noti for merchant
     * @param proxyResponse
     * @param phone_number
     * @param merchantNumber
     */
//    private void buildTranHisAndSendNotiForMerchant(ProxyResponse proxyResponse, final String phone_number,final String merchantNumber, int error,final Handler<JsonObject> callback)
//    {
//        //{"status":5,"error":1001,"tranId":17796333,"amt":100000,"io":-1,"ftime":1447471353200,"cusnum":"0934123456"}
//        final long amt = proxyResponse.getTotalAmount();
//        final long tid = proxyResponse.getRequest().getCoreTransId();
//
//        if(error == 0)
//        {
//            int status = error == 0 ? 4 : 5;
//            JsonObject jsonTrans = new JsonObject();
//            jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.M2M_VALUE);
//            jsonTrans.putString(colName.TranDBCols.COMMENT, String.format(StringConstUtil.MerchantContent.BODY_MERCHANT_NOTI, Misc.formatAmount(amt), phone_number));
//            jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tid);
//            jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amt);
//            jsonTrans.putNumber(colName.TranDBCols.STATUS, status);
//            jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(merchantNumber));
//            jsonTrans.putString(colName.TranDBCols.BILL_ID, "");
//            jsonTrans.putString(StringConstUtil.HTML, "");
//
//            Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTrans);
//
//            vertx.setTimer(2000L, new Handler<Long>() {
//                @Override
//                public void handle(Long aLong) {
//                    JsonObject joNoti = new JsonObject();
//                    joNoti.putString(StringConstUtil.StandardNoti.CAPTION, StringConstUtil.MerchantContent.CAPTION_MERCHANT_NOTI);
//                    joNoti.putString(StringConstUtil.StandardNoti.BODY, String.format(StringConstUtil.MerchantContent.BODY_MERCHANT_NOTI, Misc.formatAmount(amt), phone_number));
//                    joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, merchantNumber);
//                    joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tid);
//
//                    Misc.sendStandardNoti(vertx, joNoti);
//                    callback.handle(joNoti);
//                    vertx.cancelTimer(aLong);
//                }
//            });
//        }
//    }

    /**
     * Send noti and build transaction history for customer
     * @param proxyResponse
     * @param phone_number
     */
    private void buildTranHisAndSendNotiForCustomer(final ProxyResponse proxyResponse, final String serviceId, final String phone_number, final int error, final Handler<JsonObject> callback)
    {
        //{"status":5,"error":1001,"tranId":17796333,"amt":100000,"io":-1,"ftime":1447471353200,"cusnum":"0934123456"}

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArrayService) {
                String partnerName = jsonArrayService.size() == 0 ? serviceId : ((JsonObject)jsonArrayService.get(0)).getString(colName.ServiceCols.SERVICE_NAME, serviceId.toUpperCase()).toString();
                final long amt = proxyResponse.getRequest().getAmount();
                final long tid = proxyResponse.getRequest().getCoreTransId();
                int status = error == 0 ? 4 : 5;
                final String tranComment = error == 0 ? String.format(StringConstUtil.MerchantContent.SUCCESS_BODY_CUSTOMER_TRANHIS, Misc.formatAmount(amt), partnerName) : String.format(StringConstUtil.MerchantContent.FAIL_BODY_CUSTOMER_TRANHIS, Misc.formatAmount(amt), partnerName);
                final String notiCaption = error == 0 ? StringConstUtil.MerchantContent.SUCCESS_CAPTION_CUSTOMER_NOTI : StringConstUtil.MerchantContent.FAIL_CAPTION_CUSTOMER_NOTI;
                final String notiBody = error == 0 ? String.format(StringConstUtil.MerchantContent.SUCCESS_BODY_CUSTOMER_NOTI, Misc.formatAmount(amt), partnerName) : String.format(StringConstUtil.MerchantContent.FAIL_BODY_CUSTOMER_NOTI, Misc.formatAmount(amt), partnerName);

                JsonObject jsonTrans = new JsonObject();
                jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
                jsonTrans.putString(colName.TranDBCols.COMMENT, tranComment);
                jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tid);
                jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amt);
                jsonTrans.putNumber(colName.TranDBCols.STATUS, status);
                jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phone_number));
                jsonTrans.putString(colName.TranDBCols.BILL_ID, proxyResponse.getRequest().getBillId());
                jsonTrans.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
                jsonTrans.putString(colName.TranDBCols.PARTNER_ID, serviceId);
                jsonTrans.putString(StringConstUtil.HTML, "");

                Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTrans, new JsonObject());

                vertx.setTimer(2000L, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {
                        JsonObject joNoti = new JsonObject();
                        joNoti.putString(StringConstUtil.StandardNoti.CAPTION, notiCaption);
                        joNoti.putString(StringConstUtil.StandardNoti.BODY, notiBody);
                        joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phone_number);
                        joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tid);

                        Misc.sendStandardNoti(vertx, joNoti);
                        callback.handle(joNoti);
                        vertx.cancelTimer(aLong);
                    }
                });
            }
        });
    }

    /**
     * Send noti and build transaction history for customer
     * @param jsonSoap
     * @param phone_number
     * @param merchantNumber
     */
    private void buildTranHisAndSendNotiM2MForCustomer(final MerchantKeyManageDb.Obj meObj, final JsonObject jsonExtra, final JsonObject jsonSoap, final String serviceId, final String phone_number,final String merchantNumber, final int error, final Handler<JsonObject> callback)
    {
        //{"status":5,"error":1001,"tranId":17796333,"amt":100000,"io":-1,"ftime":1447471353200,"cusnum":"0934123456"}

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

        final long amt = jsonSoap.getLong("amt", 0);
        final long tid = jsonSoap.getLong("tranId", 0);
        int status = error == 0 ? 4 : 5;
        final String tranComment = error == 0 ? String.format(StringConstUtil.MerchantContent.SUCCESS_BODY_CUSTOMER_TRANHIS, NotificationUtils.getAmount(amt), meObj.merchant_name) : String.format(StringConstUtil.MerchantContent.FAIL_BODY_CUSTOMER_TRANHIS, NotificationUtils.getAmount(amt), meObj.merchant_name, meObj.merchant_name, meObj.merchant_number);
        final String notiCaption = error == 0 ? StringConstUtil.MerchantContent.SUCCESS_CAPTION_CUSTOMER_NOTI : StringConstUtil.MerchantContent.FAIL_CAPTION_CUSTOMER_NOTI;
        final String notiBody = error == 0 ? String.format(StringConstUtil.MerchantContent.SUCCESS_BODY_CUSTOMER_NOTI, NotificationUtils.getAmount(amt), meObj.merchant_name) : String.format(StringConstUtil.MerchantContent.FAIL_BODY_CUSTOMER_NOTI, NotificationUtils.getAmount(amt), meObj.merchant_name, meObj.merchant_name, meObj.merchant_number);

        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
        jsonTrans.putString(colName.TranDBCols.COMMENT, tranComment);
        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tid);
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amt);
        jsonTrans.putNumber(colName.TranDBCols.STATUS, status);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phone_number));
        // pos service save billid
        if (serviceId.equals(HTTPMerchantWebSiteVerticle.SERVICE_NAME)) {
            String billId = jsonExtra.getString("billId") != null ? jsonExtra.getString("billId") : phone_number;
            jsonTrans.putString(colName.TranDBCols.BILL_ID, billId);
            // check for GalaxySDK
        }else if (meObj.service_id.equals(HTTPMerchantWebSiteVerticle.SERVICE_NAME_GALAXY)) {
            String billId = jsonExtra.getString("billId") != null ? jsonExtra.getString("billId") : "";
            jsonTrans.putString(colName.TranDBCols.DESCRIPTION, billId);
            jsonTrans.putString(colName.TranDBCols.BILL_ID, phone_number);
        } else {
            jsonTrans.putString(colName.TranDBCols.BILL_ID, phone_number);
        }
        jsonTrans.putString(colName.TranDBCols.PARTNER_NAME, meObj.merchant_name);
        jsonTrans.putString(colName.TranDBCols.PARTNER_ID, serviceId);

        jsonTrans.putString(colName.TranDBCols.PARTNER_CODE, serviceId);
        jsonTrans.putString(StringConstUtil.HTML, "");

        Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTrans, jsonExtra);

        vertx.setTimer(2000L, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                JsonObject joNoti = new JsonObject();
                joNoti.putString(StringConstUtil.StandardNoti.CAPTION, notiCaption);
                joNoti.putString(StringConstUtil.StandardNoti.BODY, notiBody);
                joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phone_number);
                joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tid);

                Misc.sendStandardNoti(vertx, joNoti);
                callback.handle(joNoti);
                vertx.cancelTimer(aLong);
            }
        });
    }

    public void payOneMerchantBill(final Common.BuildLog log,final String serviceId, final long fAmount,final SockData _data,final String billId, final String pin, final String agentInfo, final Handler<JsonObject> callback)
    {
        Misc.getViaCoreService(vertx, serviceId.toLowerCase(), isStoreApp, new Handler<ViaConnectorObj>() {
            @Override
            public void handle(final ViaConnectorObj viaConnectorObj) {
                log.add("via core connector", viaConnectorObj.IsViaConnectorVerticle);
                log.add("busname", viaConnectorObj.BusName);
                log.add("billpay", viaConnectorObj.BillPay);
                //todo xoa writelog
//                log.writeLog();
                if (viaConnectorObj.IsViaConnectorVerticle) {

                    log.add("call to", "TransferWithGiftContext");
                    getServiceFee(serviceId, fAmount, log, new Handler<Long>() {
                        @Override
                        public void handle(final Long serviceFee) {
                            final long totalAmount = fAmount + serviceFee;
                            TransferWithGiftContext.build(_data.getNumber(), serviceId, billId, totalAmount, vertx, giftManager, _data,
                                    TRANWITHPOINT_MIN_POINT, TRANWITHPOINT_MIN_AMOUNT, logger,
                                    new Handler<TransferWithGiftContext>() {
                                        @Override
                                        public void handle(final TransferWithGiftContext context) {
                                            //todo xoa writelog
//                                            log.writeLog();
                                            context.writeLog(logger);
                                            log.add("func", "ServiceHelper.doPayment");
                                            final int voucherPointType = Misc.getVoucherPointType(context);
                                            final long useVoucher = context.voucher;
                                            log.add("useVoucer", useVoucher);
                                            log.add("amount", context.amount);
                                            log.add("voucher", context.voucher);
                                            log.add("point", context.point);
                                            ProxyRequest paymentRequest;
//                                            log.writeLog();

                                            paymentRequest = ConnectorCommon.createPaymentRequest(
                                                    "0" + _data.getNumber()
                                                    , pin
                                                    , serviceId
                                                    , agentInfo
                                                    , billId
                                                    , fAmount//context.amount
                                                    , context.voucher
                                                    , context.point
                                                    , 1,
                                                    "", "", "", "", "", serviceFee, new JsonObject());
                                            paymentRequest.addExtraValue("type", "sdk");
                                            log.add("isStoreApp ---->", isStoreApp);
                                            log.add("payment request content", paymentRequest.toString());
                                            ServiceHelper.
                                                    doMerchantPayment(vertx,
                                                            paymentRequest, log,
                                                            viaConnectorObj.BusName, globalConfig,  new Handler<ProxyResponse>() {

                                                                @Override
                                                                public void handle(final ProxyResponse proxyResponse) {
                                                                    Misc.removeCacheFormData(vertx, serviceId, _data.getNumber(), isStoreApp, new Handler<JsonObject>() {
                                                                        @Override
                                                                        public void handle(JsonObject jsonObject) {
                                                                            log.add("desc", jsonObject.encodePrettily());
                                                                        }
                                                                    });
                                                                    final JsonObject jsonRep = new JsonObject();
                                                                    if(proxyResponse == null)
                                                                    {
                                                                        jsonRep.putNumber("error", -1);
                                                                        callback.handle(jsonRep);
                                                                        return;
                                                                    }

                                                                    final int rcode = proxyResponse.getProxyResponseCode();
                                                                    context.tranId = proxyResponse.getRequest().getCoreTransId();
                                                                    context.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                                                                    context.error = proxyResponse.getProxyResponseCode();

                                                                    //tra thuong cho promotion girl
                                                                    JsonObject tranReply = new JsonObject();
                                                                    tranReply.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                                    tranReply.putNumber(colName.TranDBCols.ERROR, context.error);
                                                                    log.add("rcode", rcode);
                                                                    log.add("func", "Save bill to check on day");

                                                                    context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                                                        @Override
                                                                        public void handle(final JsonObject result) {
                                                                            if (result.getInteger("error", -1000) != 0) {
                                                                                log.add("use gift", "use gift error");
                                                                                log.add("error", result.getInteger("error"));
                                                                                log.add("desc", SoapError.getDesc(result.getInteger("error")));
                                                                            }

                                                                            //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
                                                                            context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
                                                                                @Override
                                                                                public void handle(JsonObject results) {
                                                                                    if (results.getInteger("error", -1000) != 0) {
                                                                                        logger.error("returnMomo error!");
                                                                                    }

//                                                                                    if (context.error == 0) {
//
//                                                                                        //reset bill amount here
//                                                                                        //resetBillAmount(_data.getNumber(), serviceId, billId);
//                                                                                        //BEGIN 0000000004
//
//                                                                                    }

                                                                                    giftTranDb.save(new GiftTran(context), null);
                                                                                }
                                                                            });
                                                                            log.writeLog();
                                                                        }
                                                                    });
                                                                    //BEGIN 0000000050

                                                                    jsonRep.putBoolean("isConnector", true);
                                                                    jsonRep.putNumber("error", rcode);
                                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                                                    jsonRep.putString(StringConstUtil.SERVICE, serviceId);
                                                                    if (callback != null) {
                                                                        buildTranHisAndSendNotiForPayBill(proxyResponse, serviceId, "0" + _data.getNumber(), rcode, new Handler<JsonObject>() {
                                                                            @Override
                                                                            public void handle(JsonObject jsonObject) {
                                                                                callback.handle(jsonRep);
                                                                            }
                                                                        });
                                                                       // callback.handle(jsonRep);
                                                                    }
                                                                    //END 0000000050
                                                                }
                                                            }
                                                    );
                                        }
                                    }
                            ); //END TRANSFERWITHGIFT
                        } //END Handle fee
                    }); //END GetServiceFee

                    return;
                } // END (viaConnectorObj.IsViaConnectorVerticle)
                else
                {
                    final JsonObject jsonRep = new JsonObject();
                    jsonRep.putBoolean("isConnector", false);
                    jsonRep.putNumber("error", -2000);
                    jsonRep.putString("desc", "khong goi qua connector");
                    callback.handle(jsonRep);
                }
            }
        }); // END getViaCoreService
    }

    public void sendPopupForReceiver(String receiverNumber, JsonObject tranRplFromSoap, String code, String name, String title, String body) {
        //cac giao dich thanh cong va co ben nhan tien

            int rcvNumber = DataUtil.strToInt(receiverNumber);

            //lay thong tin goc cho ben nhan tien
            long oldAmount = tranRplFromSoap.getLong(Const.AppClient.OldAmount, 0);
            String oldComment = tranRplFromSoap.getString(Const.AppClient.OldComment, "");

            //lay so tien goc
            if (oldAmount > 0) {
                tranRplFromSoap.putNumber(colName.TranDBCols.AMOUNT, oldAmount);
                tranRplFromSoap.removeField(Const.AppClient.OldAmount);
            }

            //lay comment goc
            tranRplFromSoap.putString(colName.TranDBCols.COMMENT, oldComment);
            tranRplFromSoap.removeField(Const.AppClient.OldComment);

            //gui thong tin cho nguoi nhan tien
            int senderPhone = DataUtil.strToInt(code) > 0 ? DataUtil.strToInt(code) : 100000;
            mCom.sendMoneyPopupRecv(rcvNumber
                    , tranRplFromSoap
                    , senderPhone
                    , name
                    , name
                    , 1
                    , title
                    , body);

    }

    public void billPayToRedis(final Logger logger,final JsonObject jsonRequest, TransferWithGiftContext context, final Handler<JsonObject> callback){

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        final JsonObject tranReply = new JsonObject();
        long amountObj = jsonRequest.getObject("extras").getLong("amount", 0);
        if(amountObj == 0){
            amountObj = jsonRequest.getObject("extras").getLong("amounttran", 0);
        }
        final long amount = amountObj;
        final String fromNumber = jsonRequest.getString("initiator", "");
        String toAgent = jsonRequest.getObject("extras").getString("target", "");
        if(Misc.isNullOrEmpty(toAgent)){
            toAgent = jsonRequest.getObject("extras").getString("specialagent", "");
        }
        final String pin = jsonRequest.getString("pass", "");
        final String billId = jsonRequest.getObject("extras").getString("account", "");
        if(amount == 0 || Misc.isNullOrEmpty(fromNumber) || Misc.isNullOrEmpty(toAgent) || Misc.isNullOrEmpty(pin)){
            tranReply.putNumber(colName.TranDBCols.ERROR, 1020);
            tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
            tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
            callback.handle(tranReply);
            return;
        }
                    if(System.currentTimeMillis() % 2 == 0){ //chia tải 2 server redis
                        logger.info("billPayToRedis[1] host " + HOST_REDIS_1 + " port " + PORT_REDIS_1 + " path " + PATH_REDIS_1);
                        HttpClientRequest requestOne = httpClientRedisOne.post(PATH_REDIS_1, new Handler<HttpClientResponse>() {
                            @Override
                            public void handle(final HttpClientResponse responseOne) {
                                int statusCode = responseOne.statusCode();
                                if (statusCode != 200) {

                                    //411: length required
                                    //401: unauthorized
                                    logger.info("error Redis[1] " + statusCode);
                                    logger.info("desc Redis[1] " + responseOne.statusMessage());
                                    return;
                                }

                                responseOne.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        try {
                                            logger.info("Respone Redis[1] " + buffer.toString());
                                            //JsonObject result = new JsonObject(buffer.toString());
                                            if(!Misc.isNullOrEmpty(buffer.toString())){
                                                JsonObject jsonResponse = new JsonObject(buffer.toString());
                                                int error = jsonResponse.getInteger("status", 1006);
                                                long tranId = jsonResponse.getLong("tid", -1);
                                                tranReply.putNumber(colName.TranDBCols.TRAN_ID, tranId);
                                                tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                                tranReply.putNumber(colName.TranDBCols.ERROR, error);
                                                if (error == 0) {
                                                    logger.info("Redis[1] success");
                                                    Misc.addCustomNumber(tranReply, fromNumber);

                                                    callback.handle(tranReply);
                                                    log.writeLog();
                                                }
                                                else {
                                                    logger.info("Redis[1] fail");
                                                    Misc.addCustomNumber(tranReply, fromNumber);
                                                    callback.handle(tranReply);
                                                    log.writeLog();
                                                }
                                            }else{
                                                logger.info("Redis[1] null" );
                                                tranReply.putNumber(colName.TranDBCols.ERROR, 1008);
                                                tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                                tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                                callback.handle(tranReply);
                                                log.writeLog();
                                                return;
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            logger.info("Redis[1] error exception " + e.getMessage());
                                            tranReply.putNumber(colName.TranDBCols.ERROR, 1006);
                                            tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                            tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                            callback.handle(tranReply);
                                            log.writeLog();
                                            return;
                                        }

                                    }
                                });
                            }
                        });

                        Buffer bufferRequest = new Buffer(jsonRequest.toString());
                        requestOne.putHeader("content-length", String.valueOf(bufferRequest.length()));
                        requestOne.putHeader("content-Type", "application/json");
                        logger.info("Request Redis[1] " + bufferRequest.toString());
                        requestOne.end(bufferRequest);
                    }else{
                        logger.info("billPayToRedis[2] host " + HOST_REDIS_2 + " port " + PORT_REDIS_2 + " path " + PATH_REDIS_2);
                        HttpClientRequest requestTwo = httpClientRedisTwo.post(PATH_REDIS_2, new Handler<HttpClientResponse>() {
                            @Override
                            public void handle(final HttpClientResponse responseTwo) {
                                int statusCode = responseTwo.statusCode();
                                if (statusCode != 200) {

                                    //411: length required
                                    //401: unauthorized
                                    logger.info("error Redis[2] " + statusCode);
                                    logger.info("desc Redis[2] " + responseTwo.statusMessage());
                                    return;
                                }

                                responseTwo.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        try {
                                            logger.info("Respone Redis[2] " + buffer.toString());
                                            //JsonObject result = new JsonObject(buffer.toString());
                                            if(!Misc.isNullOrEmpty(buffer.toString())){
                                                JsonObject jsonResponse = new JsonObject(buffer.toString());
                                                int error = jsonResponse.getInteger("status", 1006);
                                                long tranId = jsonResponse.getLong("tid", -1);
                                                tranReply.putNumber(colName.TranDBCols.TRAN_ID, tranId);
                                                tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                                tranReply.putNumber(colName.TranDBCols.ERROR, error);
                                                if (error == 0) {
                                                    logger.info("Redis[2] success");
                                                    Misc.addCustomNumber(tranReply, fromNumber);
                                                    callback.handle(tranReply);
                                                    log.writeLog();
                                                }
                                                else {
                                                    Misc.addCustomNumber(tranReply, fromNumber);
                                                    callback.handle(tranReply);
                                                    log.writeLog();
                                                }
                                            }else{
                                                logger.info("Redis[2] null" );
                                                tranReply.putNumber(colName.TranDBCols.ERROR, 1008);
                                                tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                                tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                                callback.handle(tranReply);
                                                log.writeLog();
                                                return;
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            logger.info("Redis[2] error exception " + e.getMessage());
                                            tranReply.putNumber(colName.TranDBCols.ERROR, 1006);
                                            tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                            tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                            callback.handle(tranReply);
                                            log.writeLog();
                                            return;
                                        }

                                    }
                                });
                            }
                        });

                        Buffer bufferRequest = new Buffer(jsonRequest.toString());
                        requestTwo.putHeader("content-length", String.valueOf(bufferRequest.length()));
                        requestTwo.putHeader("content-Type", "application/json");
                        logger.info("Request Redis[2] " + bufferRequest.toString());
                        requestTwo.end(bufferRequest);
                    }
    }

    public void processTransferOneBill(final NetSocket sock
            , final MomoMessage msg
            , final SockData _data
            , final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        final MomoProto.TranHisV1 fRequest = request;

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processPayOneBill", logger, sock, callback)) {
            return;
        }

        final String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processPayOneBill");
        //lay thong tin referal
        SoapProto.keyValuePair.Builder referalBuilder = null;
        if (_data != null
                && _data.getPhoneObj() != null
                && _data.getPhoneObj().referenceNumber > 0
                && _data.getPhoneObj().createdDate >= date_affected
                && (_data.getPhoneObj().createdDate + day_duration * 24 * 60 * 60 * 1000L) > System.currentTimeMillis()) {

            referalBuilder = SoapProto.keyValuePair.newBuilder();
            referalBuilder.setKey(Const.REFERAL)
                    .setValue("0" + _data.getPhoneObj().referenceNumber);
        }

        HashMap<String, String> hashMap = Misc.getKeyValuePairs(request.getKvpList());

    }

}
