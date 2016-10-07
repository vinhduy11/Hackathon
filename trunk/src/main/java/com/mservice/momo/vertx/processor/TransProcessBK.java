package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.bank.entity.BankRequest;
import com.mservice.bank.entity.BankRequestFactory;
import com.mservice.bank.entity.BankResponse;
import com.mservice.momo.connector.ServiceHelper;
import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.gift.GiftTranDb;
import com.mservice.momo.data.ironmanpromote.IronManPromoGiftDB;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.Promo.PromoReqObj;
import com.mservice.momo.data.model.Promo.PromoType;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.gateway.external.vng.FilmInfo;
import com.mservice.momo.gateway.external.vng.VngUtil;
import com.mservice.momo.gateway.external.vng.vngClass;
import com.mservice.momo.gateway.internal.connectorproxy.ConnectorCommon;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.gateway.internal.core.CoreCommon;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.objects.*;
import com.mservice.momo.gateway.internal.db.mongo.MongoBase;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.SoapVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.TopupMapping;
import com.mservice.momo.gateway.internal.visamaster.VMFeeType;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.msg.TranTypeExt;
import com.mservice.momo.util.*;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoObj;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.event.NewProcessEvent;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftTran;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.ironmanpromo.IronManPromoObj;
import com.mservice.momo.vertx.m2number.Data;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.vcb.ReqObj;
import com.mservice.momo.vertx.vcb.VcbCommon;
import com.mservice.momo.vertx.visampointpromo.VMCardIdCardNumberDB;
import com.mservice.momo.vertx.visampointpromo.VisaMpointErrorDb;
import com.mservice.proxy.entity.ProxyRequest;
import com.mservice.proxy.entity.ProxyResponse;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.*;

//import com.mservice.momo.vertx.visampointpromo.VMCardIdCardNumberDB;

/**
 * Created by User on 3/18/14.
 */
public class TransProcessBK {
    public static long DEFAULT_CORE_TIMEOUT = 7 * 60 * 1000L;
    static
    JsonObject JsonVietnam2Step = null;
    private static HashSet<Integer> AGENT_NOT_FOUNDS = new HashSet<>();
    public long TRANWITHPOINT_MIN_POINT = 0;
    public long TRANWITHPOINT_MIN_AMOUNT = 0;
    public String GIFT_MOMO_AGENT;
    public JsonObject bankCfg;
    protected GiftManager giftManager;
    CDHH cdhh = null;
    CDHHSumary cdhhSumary = null;
    CDHHErr cdhhErr = null;
    int cdhh_min_code = 0;
    int cdhh_max_code = 0;
    int cdhh_max_sms = 0;
    int cdhh_min_val = 0;
    int cdhh_max_val = 0;
    String cdhhPrefix = "cdhh";
    boolean vote_via_core = false;
    CDHHPayBack cdhhPayBack = null;
    //fsc
    FSCSetting fscSetting = null;
    FSCRecords fscRecords = null;
    HashMap<String, String> hashMapAccountCDHH = new HashMap<>();
    HashMap<String, String> hashMapCoupleCDHH = new HashMap<>();
    //topup 2 step
    JsonObject JsonViettel2Step = null;
    JsonObject JsonVina2Step = null;
    JsonObject JsonMobi2Step = null;
    JsonObject JsonEvn2Step = null;
    JsonObject JsonBeeline2Step = null;
    JsonObject JsonSfone2Step = null;
    GiftTranDb giftTranDb;
    //promotion girl
    long promo_girl_buyfilm = 5000;
    long promo_girl_paybill = 10000;
    long promo_girl_bankin = 20000;
    int pgCodeMin = 6100;
    int pgCodeMax = 6140;
    private ArrayList<String> serviceIdEvent = new ArrayList<>();
    private BillsDb billsDb;
    private VMCardIdCardNumberDB vmCardIdCardNumberDB;
    // CBS variables
    private JsonObject sbsCfg = null;
    private HcPruVoucherManagerDb hcPruVoucherManagerDb;
    private BillRuleManageDb billRuleManageDb;

    private IronManPromoGiftDB ironManPromoGiftDB;
    private GiftDb giftDb;
    private boolean activeServiceFee;

    protected PromotionProcess promotionProcess;
    static {
        /*
        agent not found
        agent not registered
        agent inactive
        agent suspended
        access denied
        target not found
        target not registered
        target inactive
        target suspended
        */

        /*
         AGENT_NOT_FOUNDS.add(SoapError.AGENT_NOT_FOUND);
        AGENT_NOT_FOUNDS.add(SoapError.AGENT_NOT_REGISTERED);
        AGENT_NOT_FOUNDS.add(SoapError.AGENT_INACTIVE);
        AGENT_NOT_FOUNDS.add(SoapError.AGENT_SUSPENDED);
        */

        AGENT_NOT_FOUNDS.add(SoapError.TARGET_NOT_FOUND);
        AGENT_NOT_FOUNDS.add(SoapError.TARGET_NOT_REGISTERED);
        AGENT_NOT_FOUNDS.add(SoapError.TARGET_INACTIVE);
        AGENT_NOT_FOUNDS.add(SoapError.TARGET_SUSPENDED);

    }

    private Logger logger;
    private Vertx vertx;
    private String VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET = "";
    private String VIETTINBANK_AGENT_ADJUST_FOR_BANKNET = "";
    private String PHIM123_RECEIVER_CASH_ACCOUNT = "";
    private M2cOffline m2cOffline;
    private TransDb transDb;
    private PhonesDb phonesDb;
    private Promo123PhimGlxDb phim123Glx;
    private boolean TEST_MODE;
    private com.mservice.momo.vertx.processor.Common mCom;
    private NewProcessEvent processEvent;
    private CodeUtil codeUtilGLX = new CodeUtil(4, 5);
    private long promoPhim123DateFrom = 0;
    private long promoPhim123DateTo = 0;
    private String refund_agent_banknet = "";
    //shared
    private long date_affected = 0;
    private int day_duration = 0;
    //for topup
    private long topup_max_value_per_month = 0;
    private int topup_percent = 0;
    private long topup_static_value = 0;
    //for topup game
    private long topup_game_max_value_per_month = 0;
    private int topup_game_percent = 0;
    private long topup_game_static_value = 0;
    private JsonObject glbCfg = null;
    private long phim123_pharse2_from_date = 0;
    private long phim123_pharse2_to_date = 0;
    private int phim123_pharse2_max_ticket_count_promo = 0;
    private String phim123_pharse2_adjust_account = "";
    private int phim123_pharse2_percent = 0;
    private int phim123_pharse2_max_time_promo = 0;
    private String m2nworkingaccount = "";
    private String escapeAcc = "";
    private double escapPercent = 0;
    private long escapThresHold = 0;
    private GiftProcess giftProcess;
    public JsonObject billpay_cfg = null;
    private MntDb mntDb;
    //code for amway promotion
    private int amwayCodeMin = 7000;
    private int amwayCodeMax = 7009;
    private HashMap<Integer, JsonObject> hmRetailer;
    private AgentsDb agentsDb;

    private EventContentDb eventContentDb;
    private VisaMpointErrorDb visaMpointErrorDb;
    private boolean isStoreApp = false;

    public TransProcessBK(Vertx vertx
            , final Logger logger
            , JsonObject glbCfg
    ) {
        this.vertx = vertx;
        this.logger = logger;
        processEvent = new NewProcessEvent(glbCfg, vertx, logger);
        //serviceIdEvent.add("vnidol");
        // Load service event id list
//        if(serviceIdEvent.size() < 1)
//        {
//            loadServiceEvents(vertx, serviceIdEvent);
//        }
        this.eventContentDb = new EventContentDb(vertx, logger);

        billsDb = new BillsDb(vertx.eventBus());
        this.hcPruVoucherManagerDb = new HcPruVoucherManagerDb(vertx, logger);
        this.VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET = glbCfg.getObject("server").getString("vietcombank_adjust_for_banknet", "");
        this.VIETTINBANK_AGENT_ADJUST_FOR_BANKNET = glbCfg.getObject("server").getString("vietcombank_adjust_for_banknet", "");
        this.PHIM123_RECEIVER_CASH_ACCOUNT = glbCfg.getObject("vng_123phim").getString("reciever_account", "");
        TEST_MODE = glbCfg.getObject("vng_123phim").getBoolean("test_mode", false);

        m2cOffline = new M2cOffline(vertx.eventBus(), logger);

        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);

        //0000000015
        vmCardIdCardNumberDB = new VMCardIdCardNumberDB(vertx, logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        phim123Glx = new Promo123PhimGlxDb(vertx.eventBus(), logger);
        mCom = new com.mservice.momo.vertx.processor.Common(vertx, logger, glbCfg);

        String strDateFrom = glbCfg.getObject("vng_123phim").getObject("promotion").getString("date_from", "2014-09-15 00:00:00").trim();
        String strDateTo = glbCfg.getObject("vng_123phim").getObject("promotion").getString("date_to", "2014-10-31 23:59:59").trim();
        promoPhim123DateFrom = Misc.getDateAsLong(strDateFrom, "yyyy-MM-dd HH:mm:ss", logger, "Phim123 get config tu ngay loi");
        promoPhim123DateTo = Misc.getDateAsLong(strDateTo, "yyyy-MM-dd HH:mm:ss", logger, "Phim123 get config den ngay loi");

        refund_agent_banknet = glbCfg.getObject("refund").getString("banknet_agent", "");

        day_duration = glbCfg.getObject("remain_for_referal").getInteger("day", 300);
        String str_date_affect = glbCfg.getObject("remain_for_referal").getString("date_affected");
        date_affected = Misc.getDateAsLong(str_date_affect, "yyyy-MM-dd HH:mm:ss", logger, "");

        //for topup
        topup_max_value_per_month = glbCfg.getObject("remain_for_referal").getObject("topup").getLong("max_value_per_month", 500000);
        topup_percent = glbCfg.getObject("remain_for_referal").getObject("topup").getInteger("percent", 5);
        topup_static_value = glbCfg.getObject("remain_for_referal").getObject("topup").getLong("static", 0);

        topup_game_max_value_per_month = glbCfg.getObject("remain_for_referal").getObject("topup_game").getLong("max_value_per_month", 500000);
        topup_game_percent = glbCfg.getObject("remain_for_referal").getObject("topup_game").getInteger("percent", 5);
        topup_game_static_value = glbCfg.getObject("remain_for_referal").getObject("topup_game").getLong("static", 0);

        giftManager = new GiftManager(vertx, logger, glbCfg);
        giftProcess = new GiftProcess(mCom, vertx, logger, glbCfg);
        giftTranDb = new GiftTranDb(vertx, logger);

        DEFAULT_CORE_TIMEOUT = glbCfg.getLong("coreTimeOut", 7 * 60 * 1000L);

        String strPhim123Pharse2FromDate = glbCfg.getObject("vng_123phim").getObject("promotion_pharse2").getString("date_from");
        String strPhim123Pharse2ToDate = glbCfg.getObject("vng_123phim").getObject("promotion_pharse2").getString("date_to");
        phim123_pharse2_from_date = Misc.getDateAsLong(strPhim123Pharse2FromDate, "yyyy-MM-dd HH:mm:ss", logger, "");
        phim123_pharse2_to_date = Misc.getDateAsLong(strPhim123Pharse2ToDate, "yyyy-MM-dd HH:mm:ss", logger, "");
        phim123_pharse2_max_ticket_count_promo = glbCfg.getObject("vng_123phim").getObject("promotion_pharse2").getInteger("max_ticket_count_promo", 2);
        phim123_pharse2_adjust_account = glbCfg.getObject("vng_123phim").getObject("promotion_pharse2").getString("adjust_account", "");
        phim123_pharse2_percent = glbCfg.getObject("vng_123phim").getObject("promotion_pharse2").getInteger("percent", 0);
        phim123_pharse2_max_time_promo = glbCfg.getObject("vng_123phim").getObject("promotion_pharse2").getInteger("max_time_promo", 0);

        m2nworkingaccount = glbCfg.getObject("sms_for_m2number").getString("m2nworkingaccount", "");

        m2nworkingaccount = glbCfg.getObject("sms_for_m2number").getString("m2nworkingaccount", "");

        escapeAcc = glbCfg.getObject("escape").getString("account", "");
        escapPercent = (double) glbCfg.getObject("escape").getNumber("percent", 0);

        escapeAcc = glbCfg.getObject("escape").getString("account", "");
        escapPercent = (double) glbCfg.getObject("escape").getNumber("percent", 0);
        escapThresHold = glbCfg.getObject("escape").getLong("threshold", 0);
        this.glbCfg = glbCfg;

        cdhh = new CDHH(vertx, logger);

        cdhhSumary = new CDHHSumary(vertx.eventBus(), logger);
        cdhhErr = new CDHHErr(vertx.eventBus(), logger);
        cdhhPayBack = new CDHHPayBack(vertx.eventBus(), logger);

        //fsc
        fscSetting = new FSCSetting(vertx.eventBus(), logger);
        fscRecords = new FSCRecords(vertx.eventBus(), logger);
        agentsDb = new AgentsDb(vertx.eventBus(), logger);
        this.visaMpointErrorDb = new VisaMpointErrorDb(vertx, logger);
        // todo get config
        cdhh_min_code = glbCfg.getObject("capdoihoanhao").getInteger("min_code", 1);
        cdhh_max_code = glbCfg.getObject("capdoihoanhao").getInteger("max_code", 7);
        cdhh_max_sms = glbCfg.getObject("capdoihoanhao").getInteger("max_sms_per_day", 30);
        cdhh_min_val = glbCfg.getObject("capdoihoanhao").getInteger("min_val", 1000);
        cdhh_max_val = glbCfg.getObject("capdoihoanhao").getInteger("max_val", 80000);
        vote_via_core = glbCfg.getObject("capdoihoanhao").getBoolean("vote_via_core", false);
        JsonArray array = glbCfg.getObject("capdoihoanhao").getArray("account", null);
        if (array != null && array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                JsonObject o = array.get(i);
                for (String s : o.getFieldNames()) {
                    hashMapAccountCDHH.put(s, o.getString(s));
                    break;
                }
            }
        }

        JsonArray arrayCouple = glbCfg.getObject("capdoihoanhao").getArray("couple", null);
        if (arrayCouple != null && arrayCouple.size() > 0) {
            for (int i = 0; i < arrayCouple.size(); i++) {
                JsonObject o = arrayCouple.get(i);
                for (String s : o.getFieldNames()) {
                    hashMapCoupleCDHH.put(s, o.getString(s));
                    break;
                }
            }
        }

        //TOP UP 2 STEP

        JsonViettel2Step = glbCfg.getObject("topup_2_step").getObject("viettel", null);
        JsonVina2Step = glbCfg.getObject("topup_2_step").getObject("vina", null);
        JsonMobi2Step = glbCfg.getObject("topup_2_step").getObject("mobi", null);
        JsonEvn2Step = glbCfg.getObject("topup_2_step").getObject("evn", null);
        JsonBeeline2Step = glbCfg.getObject("topup_2_step").getObject("beeline", null);
        JsonVietnam2Step = glbCfg.getObject("topup_2_step").getObject("vietnam", null);
        JsonSfone2Step = glbCfg.getObject("topup_2_step").getObject("sfone", null);

        JsonObject pointConfig = glbCfg.getObject("point", new JsonObject());
        TRANWITHPOINT_MIN_POINT = pointConfig.getLong("minPoint", 0);
        TRANWITHPOINT_MIN_AMOUNT = pointConfig.getLong("mintAmount", 0);

        billpay_cfg = glbCfg.getObject("soap").getObject("billpay_cfg", null);
        isStoreApp = glbCfg.getBoolean(StringConstUtil.CHECK_STORE_APP, false);
        JsonObject giftConfig = glbCfg.getObject("gift", new JsonObject());
        GIFT_MOMO_AGENT = giftConfig.getString("momoAgent", null);
        if (GIFT_MOMO_AGENT == null) {
            throw new IllegalArgumentException("config.gift.momoAgent is missing!");
        }

        JsonObject joGalaxyObj = glbCfg.getObject("galaxy", null);
        if (joGalaxyObj != null) {
            JsonArray joPGPromo = joGalaxyObj.getArray("pgpromo", null);
            promo_girl_buyfilm = ((JsonObject) joPGPromo.get(0)).getLong("buyfilm", 5000);
            promo_girl_paybill = ((JsonObject) joPGPromo.get(1)).getLong("paybill", 10000);
            promo_girl_bankin = ((JsonObject) joPGPromo.get(2)).getLong("bankin", 20000);

            JsonArray joPGCode = joGalaxyObj.getArray("pgcode", null);

            pgCodeMin = ((JsonObject) joPGCode.get(0)).getInteger("min");
            pgCodeMax = ((JsonObject) joPGCode.get(1)).getInteger("max");
        }

        //amway config
        JsonObject amwayPromo = glbCfg.getObject("amwaypromo", null);
        if (amwayPromo != null) {
            amwayCodeMin = amwayPromo.getInteger("codemin", 7000);
            amwayCodeMax = amwayPromo.getInteger("codemax", 7009);
        }

        mntDb = new MntDb(vertx.eventBus(), logger);

        //khoi tao retailer chua thong tin sms, body, cap cho app diem giao dich
        hmRetailer = new HashMap<>();
        Misc.readCfg("notification/retailter/noti.json", hmRetailer, logger);

        // Get value from config file for cyber source.
        sbsCfg = glbCfg.getObject("cybersource", null);
        bankCfg = glbCfg.getObject(StringConstUtil.BANK, new JsonObject());
        billRuleManageDb = new BillRuleManageDb(vertx, logger);

        ironManPromoGiftDB = new IronManPromoGiftDB(vertx, logger);
        giftDb = new GiftDb(vertx, logger);
        activeServiceFee = glbCfg.getBoolean(StringConstUtil.ACTIVE_SERVICE_FEE, false);

        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);

//        HashMap<String, String> hash = new HashMap<>();
//        hash.put("data", "{\"Quantity\":\"2\",\"AccountId\":\"1788485\",\"cashSource\":\"Ví MoMo\",\"amt\":\"338000\",\"amount\":338000,\"MobileNumber\":\"01665530139\",\"billId\":\"1788485\",\"Email\":\"momo@mail.com\",\"Sku\":62070,\"target\":\"popup_confirm\"}");
//        ShoppingRequest shoppingRequest = ShoppingRequestFactory.createPopupConFirmBackendRequest(hash, 0,
//                0, "", "", 1, 0, 0);
//        logger.info(shoppingRequest.getJsonObject());
//        System.out.print("");
//        vertx.eventBus().send("shoppingVerticle",shoppingRequest.getJsonObject(), new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> message) {
//                if(message.body() != null){
//                    logger.info("");
//                }
//            }
//        });0980159354
//        MerchantKeyManageDb.Obj merchantObj = new MerchantKeyManageDb.Obj();
//        merchantObj.merchant_number = "0934123456";
//        merchantObj.merchant_id = "bebongbong1";
//        merchantObj.merchant_name = "hang ngon level 1";
//        merchantObj.dev_pub_key = Cryption.publicKey_tmp;
//        merchantObj.dev_pri_key = Cryption.privateKey_tmp;
//        MerchantKeyManageDb merchantKeyManageDb = new MerchantKeyManageDb(vertx, logger);
//        merchantKeyManageDb.insert(merchantObj, new Handler<Integer>() {
//            @Override
//            public void handle(Integer integer) {
//                logger.info(integer);
//            }
//        });

    }

    public void processC2C(final NetSocket sock
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

        final long ackTime = System.currentTimeMillis();
        final String agentPin = _data == null ? "" : _data.pin;
        //khong parse duoc du lieu
        if (request == null || "".equalsIgnoreCase(agentPin)) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //het session
        if (mCom.isSessionExpiredOneMore(msg, _data, "processC2C", logger, sock, callback)) {
            return;
        }

        String channel = Const.CHANNEL_MOBI;
        if (sock == null) {
            channel = Const.CHANNEL_WEB;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processC2C");

        String shareData = request.getShare() == null ? "" : request.getShare();
        final JsonArray requestInfo = "".equalsIgnoreCase(shareData) ? new JsonArray() : new JsonArray(shareData);

        //nguoi nhan
        final String rcvName = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverName, requestInfo));
        final String rcvCardId = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverCardId, requestInfo));
        final String rcvPhone = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverPhone, requestInfo));

        //nguoi gui
        final String sndName = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderName, requestInfo));
        final String sndCardId = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderCardId, requestInfo));
        final String sndPhone = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderPhone, requestInfo));
        final long amount = request.getAmount();

        //dai ly nhan tien
        final String retailerAddr = Misc.getInfoByKey(Const.C2C.retailerAddress, requestInfo);
        final String retailerMomoPhone = Misc.getInfoByKey(Const.C2C.retailerPhone, requestInfo);

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(request.getKvpList());
        final String retailer = hashMap.containsKey(Const.DGD.Retailer) ? hashMap.get(Const.DGD.Retailer) : "0";
        final boolean isRetailer = "1".equalsIgnoreCase(retailer) ? true : false;

        final int tranType = request.getTranType();

        log.add("receiver", "----");
        log.add("ho ten", rcvName);
        log.add("cmnd", rcvCardId);
        log.add("sdt", rcvPhone);

        log.add("sender", "----");
        log.add("ho ten", sndName);
        log.add("cmnd", sndName);
        log.add("sdt", sndPhone);

        log.add("cmdInd", msg.cmdIndex);
        log.add("channel", channel);
        log.add("amount", amount);
        log.add("retailer", isRetailer);


        //String comment = "will later";
        final String comment = "";

        final MomoProto.TranHisV1 fRequest = request;

//        //BEGIN 0000000010 check c2c rule.
        final JsonObject jsonRequest = new JsonObject();
        jsonRequest.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.CHECK_PRO_C2C_RULE);
        jsonRequest.putArray("array_request", requestInfo);
        jsonRequest.putNumber("amount", amount);
        log.add("request", jsonRequest);
        long time = 60000;
        log.writeLog();
        vertx.eventBus().sendWithTimeout(AppConstant.LStandbyOracleVerticle_ADDRESS
                , jsonRequest, time
                , new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                log.add("message c2c", messageAsyncResult.succeeded());
                //JsonObject jsonRepl = new JsonObject();
                //goi lenh chuyen tien c2c vao core
                if (messageAsyncResult != null && messageAsyncResult.succeeded()) {
                    if (messageAsyncResult.result() == null) {
                        log.add("messageAsyncResult.result()", "null");
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRANS_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.TranHisV1.newBuilder()
                                        .setCommandInd(msg.cmdIndex)
                                        .setError(33)
                                        .setDesc("Qua han muc cho phep")
                                        .build().toByteArray()
                        );
                        log.writeLog();
                        mCom.writeDataToSocket(sock, buf);
                        return;
                    }

                    JsonObject jsonResponse = messageAsyncResult.result().body();

                    if (jsonResponse.containsField("error") && jsonResponse.getInteger("error") == 1000) {
                        log.add("desc", "jsonResponse.getInteger(error) == 1000");
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRANS_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.TranHisV1.newBuilder()
                                        .setCommandInd(msg.cmdIndex)
                                        .setError(33)
                                        .setDesc("Qua han muc cho phep")
                                        .build().toByteArray()
                        );
                        log.writeLog();
                        mCom.writeDataToSocket(sock, buf);
                        return;
                    }

                    int result_amount = jsonResponse.getInteger("p_result_amt", 1);
                    int result_count = jsonResponse.getInteger("p_result_cnt", 1);
                    int result_limit = jsonResponse.getInteger("p_result_out_limit", 1);
                    String result_type = jsonResponse.getString("p_result_type", "c2c");
                    int count_cus = jsonResponse.getInteger("p_result_cnt_cus", 1);
                    log.add("jsonResponse", jsonResponse);
                    log.writeLog();
                    if (result_amount != 0) {
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRANS_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.TranHisV1.newBuilder()
                                        .setCommandInd(msg.cmdIndex)
                                        .setError(33)
                                        .setDesc("Qua han muc cho phep")
                                        .build().toByteArray()
                        );
                        log.writeLog();
                        mCom.writeDataToSocket(sock, buf);
                        return;
                    }
                    if (result_count != 0) {
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRANS_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.TranHisV1.newBuilder()
                                        .setCommandInd(msg.cmdIndex)
                                        .setError(33)
                                        .setDesc("Qua so lan giao dich cho phep")
                                        .build().toByteArray()
                        );
                        log.writeLog();
                        mCom.writeDataToSocket(sock, buf);
                        return;
                    }
                    if (result_limit != 0) {
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRANS_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.TranHisV1.newBuilder()
                                        .setCommandInd(msg.cmdIndex)
                                        .setError(33)
                                        .setDesc("Qua so lan gioi han cho phep")
                                        .build().toByteArray()
                        );
                        log.writeLog();
                        mCom.writeDataToSocket(sock, buf);
                        return;
                    }
                    if (count_cus != 0) {
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRANS_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.TranHisV1.newBuilder()
                                        .setCommandInd(msg.cmdIndex)
                                        .setError(33)
                                        .setDesc("Qua so lan gioi han cho phep")
                                        .build().toByteArray()
                        );
                        log.writeLog();
                        mCom.writeDataToSocket(sock, buf);
                        return;
                    }
                    if (result_amount == 0 && result_count == 0 && result_limit == 0 && count_cus == 0) {
                        log.add("fuc", "doC2C");
                        CoreCommon.doC2C(vertx
                                , log
                                , "0" + msg.cmdPhone
                                , agentPin
                                , sndName.replaceAll(" ", "").trim()
                                , sndPhone
                                , sndCardId.replaceAll(" ", "").trim()
                                , rcvName.replaceAll(" ", "").trim()
                                , rcvPhone
                                , rcvCardId.replaceAll(" ", "").trim()
                                , amount
                                , DEFAULT_CORE_TIMEOUT
                                , comment
                                , retailerAddr
                                , retailerMomoPhone
                                , null
                                , new Handler<Response>() {
                            @Override
                            public void handle(final Response requestObj) {
                                agentsDb.getOneAgent("0" + msg.cmdPhone, "doC2C", new Handler<AgentsDb.StoreInfo>() {
                                    @Override
                                    public void handle(AgentsDb.StoreInfo storeInfo) {
                                        log.add("error", requestObj.Error);
                                        log.add("desc", requestObj.Description);
                                        log.add("soapdesc", SoapError.getDesc(requestObj.Error));

                                        InformObj iobj = Misc.getContentFromCfg(hmRetailer, tranType, requestObj.Error, logger);

                                        //noti c2c
                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.sms = String.format(iobj.sms, Misc.formatAmount(amount).replace(",", "."), rcvName, rcvPhone);
                                        noti.tranId = requestObj.Tid;
                                        noti.priority = 2;
                                        noti.time = System.currentTimeMillis();
                                        noti.category = 0;
                                        noti.caption = iobj.cap;
                                        noti.body = String.format(iobj.body, Misc.formatAmount(amount).replace(",", "."), rcvName, rcvPhone);
                                        noti.htmlBody = noti.body;
                                        noti.status = Notification.STATUS_DETAIL;
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        Misc.sendNoti(vertx, noti);

                                        //tran c2c
                                        final TranObj tran = new TranObj();
                                        tran.cmdId = msg.cmdIndex;
                                        tran.clientTime = ackTime;
                                        tran.ackTime = ackTime;
                                        tran.finishTime = System.currentTimeMillis();
                                        tran.tranType = tranType;
                                        tran.tranId = requestObj.Tid;
                                        tran.error = requestObj.Error;
                                        tran.status = requestObj.Error == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL;
                                        tran.io = -1;
                                        tran.category = 0;
                                        tran.parterCode = fRequest.getPartnerCode();
                                        tran.amount = amount;
                                        tran.partnerId = fRequest.getPartnerId();
                                        tran.partnerName = fRequest.getPartnerName();
                                        tran.partnerRef = fRequest.getPartnerRef();
                                        tran.comment = fRequest.getComment();
                                        tran.billId = fRequest.getBillId();
                                        tran.cmdId = msg.cmdIndex;
                                        tran.desc = "";
                                        tran.share = new JsonArray(fRequest.getShare());

                                        //neu thanh cong thi them phan phi
                                        if (requestObj.Error == 0) {
                                            Misc.getC2CFee(vertx, fRequest.getTranType(), fRequest.getAmount(), new Handler<FeeDb.Obj>() {
                                                @Override
                                                public void handle(FeeDb.Obj obj) {
                                                    int c2cFee = obj == null ? 30000 : obj.STATIC_FEE;
                                                    //them phan phi
                                                    JsonObject joFee = new JsonObject();
                                                    joFee.putString(Const.AppClient.Fee, c2cFee + "");
                                                    tran.share.add(joFee);
                                                    tran.kvp = new JsonObject().putString(Const.DGD.Retailer, "1");
                                                    mCom.sendTransReplyByTran(msg, tran, transDb, sock);
                                                }
                                            });

                                        } else {
                                            //core timeout
                                            if (requestObj.Error == -1) {
                                                tran.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
                                                tran.desc = "Không tạo được giao dịch";
                                                log.add("core timeout", requestObj.Error);
                                            } else if (requestObj.Error == 7 && storeInfo != null && storeInfo.agent_type == 3
                                                    && storeInfo.status != 2) {
                                                tran.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
                                                tran.desc = StringConstUtil.IS_NOT_DCNTT_FUNCTION;
                                                log.add("core timeout", requestObj.Error);
                                            }
                                            tran.kvp = new JsonObject().putString(Const.DGD.Retailer, "1");
                                            mCom.sendTransReplyByTran(msg, tran, transDb, sock);
                                        }
                                        //send noti inform will have c2c with this agent
                                        String cap = "Sắp có giao dịch nhận C2C";
                                        String tmp = "Người nhận tiền (%s) muốn đến chỗ bạn nhận tiền, số tiền là %sđ, bạn hãy chuẩn bị những ngân sách cần thiết nhé.";
                                        if (requestObj.Error == 0
                                                && DataUtil.strToInt(retailerMomoPhone) > 0) {

                                            Notification notiAgentDest = new Notification();
                                            notiAgentDest.receiverNumber = DataUtil.strToInt(retailerMomoPhone);
                                            notiAgentDest.caption = cap;
                                            notiAgentDest.body = String.format(tmp, rcvPhone, Misc.formatAmount(amount).replaceAll(",", "."));
                                            notiAgentDest.bodyIOS = notiAgentDest.body;
                                            notiAgentDest.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                            notiAgentDest.status = Notification.STATUS_DETAIL;
                                            Misc.sendNoti(vertx, notiAgentDest);
                                        }

                                        //cap nhat session time
                                        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());

                                        //lay couponid theo duong khac vi khong tra ra duoc theo lenh  vuong
                                        if (requestObj.Error == 0) {

                                            //cap nhat so du tien
                                            mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);

                                            //lat couponid tu core ra
                                            JsonObject jo = new JsonObject();
                                            jo.putNumber("type", UMarketOracleVerticle.GET_COUPON_ID);
                                            jo.putNumber("tranid", requestObj.Tid);
                                            jo.putNumber("number", msg.cmdPhone);

                                            vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> jResult) {

                                                    JsonObject jo = jResult.body();
                                                    String couponId = jo.getString("couponid", "");
                                                    MntDb.Obj mntObj = new MntDb.Obj();
                                                    mntObj.code = couponId;
                                                    mntObj.shared = requestInfo;
                                                    mntObj.tranid = requestObj.Tid;
                                                    mntObj.agentnumber = "0" + msg.cmdPhone;
                                                    mntObj.amount = amount;
                                                    mntObj.time = System.currentTimeMillis();

                                                    //cache tren mongo
                                                    mntDb.save(mntObj, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean aBoolean) {

                                                        }
                                                    });
                                                    log.writeLog();
                                                }
                                            });
                                            return;
                                        }
                                        log.writeLog();
                                    }
                                });//end call core
                            }
                        });//END CHECK STORE DB
                    }
                }//end succeed
                else {
                    log.add("desc", "messageAsyncResult.failed()");
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.TRANS_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.TranHisV1.newBuilder()
                                    .setCommandInd(msg.cmdIndex)
                                    .setError(33)
                                    .build().toByteArray()
                    );
                    log.writeLog();
                    mCom.writeDataToSocket(sock, buf);
                    return;
                }
            }
        });

        //goi lenh chuyen tien c2c vao core
//        CoreCommon.doC2C(vertx
//                , log
//                , "0" + msg.cmdPhone
//                , agentPin
//                , sndName.replaceAll(" ", "").trim()
//                , sndPhone
//                , sndCardId.replaceAll(" ", "").trim()
//                , rcvName.replaceAll(" ", "").trim()
//                , rcvPhone
//                , rcvCardId.replaceAll(" ", "").trim()
//                , amount
//                , DEFAULT_CORE_TIMEOUT
//                , comment
//                , retailerAddr
//                , retailerMomoPhone
//                , null
//                , new Handler<Response>() {
//            @Override
//            public void handle(final Response requestObj) {
//
//                log.add("error", requestObj.Error);
//                log.add("desc", requestObj.Description);
//                log.add("soapdesc", SoapError.getDesc(requestObj.Error));
//
//                InformObj iobj = Misc.getContentFromCfg(hmRetailer, tranType, requestObj.Error, logger);
//
//                //noti c2c
//                Notification noti = new Notification();
//                noti.receiverNumber = msg.cmdPhone;
//                noti.sms = String.format(iobj.sms, Misc.formatAmount(amount).replace(",", "."), rcvName, rcvPhone);
//                noti.tranId = requestObj.Tid;
//                noti.priority = 2;
//                noti.time = System.currentTimeMillis();
//                noti.category = 0;
//                noti.caption = iobj.cap;
//                noti.body = String.format(iobj.body, Misc.formatAmount(amount).replace(",", "."), rcvName, rcvPhone);
//                noti.htmlBody = noti.body;
//                noti.status = Notification.STATUS_DETAIL;
//                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
//                Misc.sendNoti(vertx, noti);
//
//                //tran c2c
//                final TranObj tran = new TranObj();
//                tran.cmdId = msg.cmdIndex;
//                tran.clientTime = ackTime;
//                tran.ackTime = ackTime;
//                tran.finishTime = System.currentTimeMillis();
//                tran.tranType = tranType;
//                tran.tranId = requestObj.Tid;
//                tran.error = requestObj.Error;
//                tran.status = requestObj.Error == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL;
//                tran.io = -1;
//                tran.category = 0;
//                tran.parterCode = fRequest.getPartnerCode();
//                tran.amount = amount;
//                tran.partnerId = fRequest.getPartnerId();
//                tran.partnerName = fRequest.getPartnerName();
//                tran.partnerRef = fRequest.getPartnerRef();
//                tran.comment = fRequest.getComment();
//                tran.billId = fRequest.getBillId();
//                tran.cmdId = msg.cmdIndex;
//                tran.desc = "";
//                tran.share = new JsonArray(fRequest.getShare());
//
//                //neu thanh cong thi them phan phi
//                if (requestObj.Error == 0) {
//                    Misc.getC2CFee(vertx, fRequest.getTranType(), fRequest.getAmount(), new Handler<FeeDb.Obj>() {
//                        @Override
//                        public void handle(FeeDb.Obj obj) {
//                            int c2cFee = obj == null ? 30000 : obj.STATIC_FEE;
//                            //them phan phi
//                            JsonObject joFee = new JsonObject();
//                            joFee.putString(Const.AppClient.Fee, c2cFee + "");
//                            tran.share.add(joFee);
//                            tran.kvp = new JsonObject().putString(Const.DGD.Retailer, "1");
//                            mCom.sendTransReplyByTran(msg, tran, transDb, sock);
//                        }
//                    });
//
//                } else {
//                    //core timeout
//                    if (requestObj.Error == -1) {
//                        tran.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
//                        tran.desc = "Không tạo được giao dịch";
//                        log.add("core timeout", requestObj.Error);
//                    }
//
//                    tran.kvp = new JsonObject().putString(Const.DGD.Retailer, "1");
//                    mCom.sendTransReplyByTran(msg, tran, transDb, sock);
//                }
//
//                //send noti inform will have c2c with this agent
//                String cap = "Sắp có giao dịch nhận C2C";
//                String tmp = "Người nhận tiền (%s) muốn đến chỗ bạn nhận tiền, số tiền là %sđ, bạn hãy chuẩn bị những ngân sách cần thiết nhé.";
//                if (requestObj.Error == 0
//                        && DataUtil.strToInt(retailerMomoPhone) > 0) {
//
//                    Notification notiAgentDest = new Notification();
//                    notiAgentDest.receiverNumber = DataUtil.strToInt(retailerMomoPhone);
//                    notiAgentDest.caption = cap;
//                    notiAgentDest.body = String.format(tmp, rcvPhone, Misc.formatAmount(amount).replaceAll(",", "."));
//                    notiAgentDest.bodyIOS = notiAgentDest.body;
//                    notiAgentDest.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
//                    notiAgentDest.status = Notification.STATUS_DETAIL;
//                    Misc.sendNoti(vertx, notiAgentDest);
//                }
//
//                //cap nhat session time
//                mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
//
//                //lay couponid theo duong khac vi khong tra ra duoc theo lenh  vuong
//                if (requestObj.Error == 0) {
//
//                    //cap nhat so du tien
//                    mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
//
//                    //lat couponid tu core ra
//                    JsonObject jo = new JsonObject();
//                    jo.putNumber("type", UMarketOracleVerticle.GET_COUPON_ID);
//                    jo.putNumber("tranid", requestObj.Tid);
//                    jo.putNumber("number", msg.cmdPhone);
//
//                    vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
//                        @Override
//                        public void handle(Message<JsonObject> jResult) {
//
//                            JsonObject jo = jResult.body();
//                            String couponId = jo.getString("couponid", "");
//                            MntDb.Obj mntObj = new MntDb.Obj();
//                            mntObj.code = couponId;
//                            mntObj.shared = requestInfo;
//                            mntObj.tranid = requestObj.Tid;
//                            mntObj.agentnumber = "0" + msg.cmdPhone;
//                            mntObj.amount = amount;
//
//                            //cache tren mongo
//                            mntDb.save(mntObj, new Handler<Boolean>() {
//                                @Override
//                                public void handle(Boolean aBoolean) {
//
//                                }
//                            });
//                            log.writeLog();
//                        }
//                    });
//                    return;
//                }
//                log.writeLog();
//            }
//        });
    }

    public void processBanknetVerifyOtp(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> callback) {
        MomoProto.BanketVerifyOtp request;
        try {
            request = MomoProto.BanketVerifyOtp.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasFeeAmount() || !request.hasFullAmount()
                || !request.hasMerchantTransId() || !request.hasTransId() || !request.hasOtp()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("otp", request.getOtp() == null ? "" : request.getOtp());
        log.add("transId", request.getTransId() == null ? "" : request.getTransId());
        log.add("merchantTransId", request.getMerchantTransId() == null ? "" : request.getMerchantTransId());
        log.add("fullamount", request.getFullAmount());
        final long ackTime = System.currentTimeMillis();

        SoapProto.BankNetVerifyOtp.Builder builder = SoapProto.BankNetVerifyOtp.newBuilder();
        builder.setAmount(request.getFullAmount())
                .setOtp(request.getOtp())
                .setMerchantTransId(request.getMerchantTransId())
                .setTransId(request.getTransId());
        builder.addKvps(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));

        final long corAdjustAmt = request.getFullAmount() - request.getFeeAmount();
        final long fee = request.getFeeAmount();

        Buffer buffer = MomoMessage.buildBuffer(SoapProto.MsgType.BANK_NET_VERIFY_OTP_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , SoapProto.BankNetVerifyOtp.newBuilder()
                        .setAmount(request.getFullAmount())
                        .setOtp(request.getOtp())
                        .setMerchantTransId(request.getMerchantTransId())
                        .setTransId(request.getTransId())
                        .build().toByteArray()
        );

        log.add("begin", "banknet verticle");
        final MomoProto.BanketVerifyOtp f_request = request;
        vertx.eventBus().send(AppConstant.BanknetVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> json) {

                log.add("end", "banknet verticle");
                log.add("banknet result", json.body().encodePrettily());

                //verify otp on bank-net side success
                if (json.body() != null && json.body().getBoolean(MongoKeyWords.RESULT)) {

                    //chon tai khoan ke qua qua vietcombank hoac viettinbank
                    String adjustAcc = VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET;
                    if (data != null && data.getPhoneObj() != null
                            && "102".equalsIgnoreCase(data.getPhoneObj().bank_code)) {

                        adjustAcc = VIETTINBANK_AGENT_ADJUST_FOR_BANKNET;
                    }

                    Buffer buffer = MomoMessage.buildBuffer(
                            SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , SoapProto.commonAdjust.newBuilder()
                                    .setSource(adjustAcc)
                                    .setTarget("0" + msg.cmdPhone)
                                    .setAmount(corAdjustAmt)
                                    .setPhoneNumber("0" + msg.cmdPhone)
                                    .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                            .setKey(Const.REFUND_AMOUNT).setValue(String.valueOf(fee)))
                                    .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                            .setKey(Const.REFUND_AGENT).setValue(refund_agent_banknet))
                                    .build()
                                    .toByteArray()
                    );

                    log.add("begin", "soapin verticle");
                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> result) {

                            int rcode = result.body().getInteger(colName.TranDBCols.ERROR);

                            log.add("adjust at momo side result", rcode);
                            log.add("error", rcode);
                            log.add("desc", SoapError.getDesc(rcode));
                            Buffer buf = MomoMessage.buildBuffer(MomoProto.MsgType.BANKNET_VERIFY_OTP_REPLY_VALUE
                                    , msg.cmdIndex
                                    , msg.cmdPhone
                                    , MomoProto.StandardReply.newBuilder()
                                    .setResult(rcode == 0 ? true : false)
                                    .setRcode(rcode)
                                    .build().toByteArray());

                            log.writeLog();
                            mCom.writeDataToSocket(sock, buf);

                            if(rcode == 0)
                            {
                                //Kiem tra thuc hien tra thuong chuong trinh thang 10
                                log.add("func", "Kiem tra khuyen mai thang 10");
                                long tid = result.body().getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());
                                promotionProcess.getUserInfoToCheckPromoProgram("", "0" + msg.cmdPhone, null, tid, MomoProto.TranHisV1.TranType.BANK_NET_VERIFY_OTP_VALUE, f_request.getFullAmount(), StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, data,
                                        new JsonObject());
                            }
                        }
                    });
                } else {
                    //verify on bank-net side failed
                    Buffer buf = MomoMessage.buildBuffer(MomoProto.MsgType.BANKNET_VERIFY_OTP_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , MomoProto.StandardReply.newBuilder()
                            .setResult(json.body().getBoolean("result"))
                            .setRcode(json.body().getInteger("error"))
                            .build().toByteArray());

                    mCom.writeDataToSocket(sock, buf);
                }
            }
        });
    }

    public void processBankOut(final NetSocket sock
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

        if (request == null || !request.hasAmount() || !request.hasPartnerCode()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processBankOut", logger, sock, callback)) {
            return;
        }

        final long ackTime = System.currentTimeMillis();
        String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processBankOut");
        log.add("cmdInd", msg.cmdIndex);
        log.add("channel", channel);
        log.add("amount", request.getAmount());
        log.add("bankcode", request.getPartnerCode() == null ? "" : request.getPartnerCode());

        final String bankCode = request.getPartnerCode();
        SoapProto.BankOut.Builder builder = SoapProto.BankOut.newBuilder().setMpin(_data.pin)
                .setAmount(request.getAmount())
                .setChannel(channel)
                .setBankCode(request.getPartnerCode());
        builder.addKvps(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));

        final Common common = new Common(vertx, logger, glbCfg);
        Buffer bankout = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_OUT_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build()
                        .toByteArray()
        );

        log.add("begin", "soapin verticle");
        boolean allow_bank_via_connector = bankCfg.getBoolean(StringConstUtil.ALLOW_BANK_VIA_CONNECTOR, false);
        final int timer = bankCfg.getInteger(StringConstUtil.TIMER, 5);
        log.add("TIMER", timer);
        log.add("ALLOW_BANK_VIA_CONNECTOR", allow_bank_via_connector);
        String otp = "";
//        JsonArray share = request.getShare().equalsIgnoreCase("") ? new JsonArray() : new JsonArray(request.getShare());
//        if (share.size() > 0) {
//            for (Object o : share) {
//                otp = ((JsonObject) o).getString("otp", "");
//                if (!otp.equalsIgnoreCase("")) break;
//            }
//        }
        try {
            JsonArray share = request.getShare().equalsIgnoreCase("") ? new JsonArray() : new JsonArray(request.getShare());
            if (share.size() > 0) {
                for (Object o : share) {
                    otp = ((JsonObject) o).getString("otp", "");
                    if (!otp.equalsIgnoreCase("")) break;
                }
            }
        } catch (Exception ex) {
            JsonObject jsonShare = request.getShare().equalsIgnoreCase("") ? new JsonObject() : new JsonObject(request.getShare());
            otp = jsonShare.getString("otp", "");
        }
        String bankMethod = "";
        if (request.getKvpList().size() > 0) {
            for (MomoProto.TextValue textValue : request.getKvpList()) {
                if (textValue.getText().equalsIgnoreCase("bankmethod")) {
                    bankMethod = textValue.getValue();
                    break;
                }
            }
        }
        log.add("bankMethod", bankMethod);
        //Kiem tra neu la luon nhan OTP thi goi qua connector
        final String bankVerticle = bankCfg.getString(StringConstUtil.BANK_CONNECTOR_VERTICLE, "bankVerticle");
        log.add("bank verticle ", bankVerticle);
        final MomoProto.TranHisV1 f_request = request;
        final int tranType = request.getTranType();
//        KeyValuePair client = new KeyValuePair();
//        client.setKey("client");
//        client.setValue("backend");
//
//        KeyValuePair chanel = new KeyValuePair();
//        chanel.setKey("chanel");
//        chanel.setValue(channel);
//
//        com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
//        kvIsSMS.setKey("issms");
//        kvIsSMS.setValue("no");
        final Map<String, String> kvps = new HashMap<>();
        kvps.put("client", "backend");
        kvps.put("chanel", "mobi");
        kvps.put("issms", "no");
        final String phoneStr = String.valueOf(msg.cmdPhone).substring(String.valueOf(msg.cmdPhone).length() - 1 - 5, String.valueOf(msg.cmdPhone).length() - 1);
        final String curPhoneStr = String.valueOf(System.currentTimeMillis()) + phoneStr;
        final long curPhoneLong = Long.parseLong(curPhoneStr);
        log.add("phoneStr ", phoneStr);
        log.add("curPhoneStr ", curPhoneStr);
        log.add("curPhoneLong ", curPhoneLong);
        if (allow_bank_via_connector) {
            final String otp_tmp = otp;
            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj obj) {
                    String bnk_tmp = bankCode;
                    if ((DataUtil.strToInt(bankCode) == 0) && obj != null) {
                        bnk_tmp = obj.bank_code.equalsIgnoreCase("") || obj.bank_code.equalsIgnoreCase("0") ? "0" : obj.bank_code;
                    } else if (DataUtil.strToInt(bankCode) == 0) {
                        bnk_tmp = "0";
                    }
                    log.add("bank code again", bnk_tmp);
                    BankRequest bankRequest = BankRequestFactory.cashoutRequest(bankCode, "0" + msg.cmdPhone, _data.pin, curPhoneLong, f_request.getAmount(), otp_tmp, kvps);
                    vertx.eventBus().sendWithTimeout(bankVerticle, bankRequest.getJsonObject(), 60 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                            JsonObject jsonRep = new JsonObject();

                            final MomoProto.TranHisV1.Builder builder = f_request.toBuilder();
                            if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                                mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                                final BankResponse bankResponse = new BankResponse(messageAsyncResult.result().body());
                                log.add("result", "Co ket qua tra ve tu connector");
                                log.add("bankResponse", bankResponse);

                                builder.setTranType(tranType);

                                agentsDb.getOneAgent("0" + msg.cmdPhone, "bankVerticle", new Handler<AgentsDb.StoreInfo>() {
                                    @Override
                                    public void handle(AgentsDb.StoreInfo storeInfo) {
                                        Buffer buf;
                                        if (bankResponse != null && bankResponse.getResultCode() == 0) {

                                            //Tra ket qua thanh cong ve cho client
                                            log.add("resultCode", bankResponse.getResultCode());
                                            builder.setError(bankResponse.getResultCode())
                                                    .setDesc(bankResponse.getDescription()).setStatus(4);
                                            //build buffer --> soap verticle
                                            buf = MomoMessage.buildBuffer(
                                                    MomoProto.MsgType.TRANS_REPLY_VALUE,
                                                    msg.cmdIndex,
                                                    msg.cmdPhone,
                                                    builder.build().toByteArray()
                                            );
                                        } else {
                                            if (bankResponse != null && bankResponse.getResultCode() != 0 && storeInfo != null && storeInfo.agent_type == 3 && storeInfo.status != 2) {
                                                builder.setError(1000000)
                                                        .setDesc(StringConstUtil.IS_NOT_DCNTT_FUNCTION).setStatus(5);

                                                buf = MomoMessage.buildBuffer(
                                                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                                                        msg.cmdIndex,
                                                        msg.cmdPhone,
                                                        builder.build().toByteArray()
                                                );
                                            } else {
                                                int errorCode = bankResponse == null ? 1000 : bankResponse.getResultCode();
                                                String desc = bankResponse == null ? "Khong nhan duoc ket qua tu core" : bankResponse.getDescription();
                                                log.add("errorCode", errorCode);
                                                log.add("desc", desc);

                                                builder.setError(errorCode)
                                                        .setDesc(desc).setStatus(5);

                                                buf = MomoMessage.buildBuffer(
                                                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                                                        msg.cmdIndex,
                                                        msg.cmdPhone,
                                                        builder.build().toByteArray()
                                                );
                                            }
                                        }
                                        common.writeDataToSocket(sock, buf);
                                        log.writeLog();
                                        return;
                                    }
                                });
                            } else {
                                //Tra loi time out, khong ket noi duoc voi connector
                                Buffer buf;
                                builder.setError(1000)
                                        .setDesc("Time out").setStatus(5);
                                buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        builder.build().toByteArray());
                                common.writeDataToSocket(sock, buf);
                                mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }
            });
        } else {
            vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, bankout, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> result) {
                    log.add("end", "soapin verticle");
                    log.writeLog();

                    final JsonObject tranRpl = result.body();

                    //test.start
                /*tranRpl.putNumber(colName.TranDBCols.ERROR,0);
                tranRpl.putNumber(colName.TranDBCols.STATUS,TranObj.STATUS_OK);*/
                    //test.end
                    Buffer buf;
                    MomoProto.TranHisV1.Builder builder = f_request.toBuilder();
                    log.add("tranRpl", tranRpl);
                    //giao dich loi
                    agentsDb.getOneAgent("0" + msg.cmdPhone, "soapin verticle", new Handler<AgentsDb.StoreInfo>() {
                        @Override
                        public void handle(AgentsDb.StoreInfo storeInfo) {
                            if (tranRpl.getInteger(colName.TranDBCols.STATUS, -1) == 5 && storeInfo != null && storeInfo.agent_type == 3
                                    && storeInfo.status != 2) {
                                Buffer buf;
                                MomoProto.TranHisV1.Builder builder = f_request.toBuilder();

                                builder.setError(1000000)
                                        .setDesc(StringConstUtil.IS_NOT_DCNTT_FUNCTION).setStatus(5);
                                buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        builder.build().toByteArray());
                                if (mCom != null) {
                                    mCom.writeDataToSocket(sock, buf);
                                }
                                log.writeLog();
                                return;
                            }
                            mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, callback);
                        }
                    });

                }
            });
        }
        //cap nhat session time
        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
    }

    public void processTopUp(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null
                || !request.hasPartnerCode()
                || !request.hasAmount()) {

            if (sock != null) {
                mCom.writeErrorToSocket(sock);
            }

            if (callback != null) {
                callback.handle(
                        new JsonObject()
                                .putNumber("error", -1)
                                .putString("description", "")
                );
            }
            return;
        }

        final MomoProto.TranHisV1 fRequest = request;

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, data, "processTopUp", logger, sock, callback)) {
            return;
        }

        final long ackTime = System.currentTimeMillis();
        final String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        final long tranAmount = request.getAmount();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processTopUp");
        log.add("amount", tranAmount);

        final int cusNum = DataUtil.strToInt(request.getPartnerCode());
        log.add("to number", cusNum);
        log.add("channel", channel);

        final SoapProto.TopUp.Builder builder = SoapProto.TopUp.newBuilder();
        builder.setFromNumber(msg.cmdPhone)
                .setMpin(data.pin)
                .setChannel(channel)
                .setAmount(tranAmount)
                .setToNumber(cusNum);

        builder.addKeyValuePairs(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));

        //dgd.start
        ArrayList<SoapProto.keyValuePair.Builder> reKeyValues = Misc.buildKeyValuesForSoap(request.getKvpList());
        for (int i = 0; i < reKeyValues.size(); i++) {
            builder.addKeyValuePairs(reKeyValues.get(i));
        }

        HashMap<String, String> hashMap = Misc.getKeyValuePairs(request.getKvpList());
        final String retailer = hashMap.containsKey(Const.DGD.Retailer) ? hashMap.get(Const.DGD.Retailer) : "";
        final String cusnum = hashMap.containsKey(Const.DGD.CusNumber) ? hashMap.get(Const.DGD.CusNumber) : "";
        final boolean isRetailer = Const.DGDValues.Retailer == DataUtil.strToInt(retailer) ? true : false;

        log.add("retailer", isRetailer);
        //dgd.end

        //tinh toan gia tri bonus topup cho referal
        if (willGetBonus(data)) {
            //tinh toan gia tri can lay

            ArrayList<Integer> listTranType = new ArrayList<>();
            listTranType.add(MomoProto.TranHisV1.TranType.TOP_UP_VALUE);
            transDb.sumTranInCurrentMonth(msg.cmdPhone, listTranType, new Handler<Long>() {
                @Override
                public void handle(Long totalValue) {
                    //bonus for referal
                    long bonusValue = Misc.calBonusValue(totalValue
                            , tranAmount
                            , topup_max_value_per_month
                            , topup_percent
                            , topup_static_value
                            , log);

                    SoapProto.keyValuePair.Builder bonusBuilder = SoapProto.keyValuePair.newBuilder();
                    bonusBuilder.setKey(Const.BONUSFORREFERAL);
                    bonusBuilder.setValue(String.valueOf(bonusValue));

                    //referal
                    SoapProto.keyValuePair.Builder referalBuilder = SoapProto.keyValuePair.newBuilder();
                    referalBuilder.setKey(Const.REFERAL);
                    referalBuilder.setValue("0" + data.getPhoneObj().referenceNumber);

                    builder.addKeyValuePairs(bonusBuilder);
                    builder.addKeyValuePairs(referalBuilder);

                    //todo lam sau topup 2 buoc qua
                    String vtelTarget = TopupMapping.getTargetTopup("0" + cusNum, "0" + msg.cmdPhone, log);
                    boolean enable = getTopup2StepEnable(JsonViettel2Step);
                    String busAddress = getTopup2StepBusAddress(JsonViettel2Step);

                    if (enable
                            && ("viettelv1.airtime".equalsIgnoreCase(vtelTarget)
                            || "viettelv2.airtime".equalsIgnoreCase(vtelTarget))) {

                        log.add("begin topup 2 step", "---------");
                        log.add("enable", enable);
                        log.add("busAddress", busAddress);

                        topUp2Step(msg
                                , data
                                , tranAmount
                                , cusNum
                                , ackTime
                                , sock
                                , isRetailer
                                , log
                                , fRequest
                                , busAddress
                                , channel
                                , new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject jsonObject) {
                                logger.info("topup 2 steps json reply " + jsonObject);
                                if (callback != null) {
                                    callback.handle(jsonObject);
                                }
                            }
                        });

                        return;
                    }

                    TransferWithGiftContext.build(msg.cmdPhone,
                            "topup",
                            "0" + cusNum,
                            tranAmount,
                            vertx,
                            giftManager,
                            data,
                            TRANWITHPOINT_MIN_POINT,
                            TRANWITHPOINT_MIN_AMOUNT,
                            logger,
                            new Handler<TransferWithGiftContext>() {
                                @Override
                                public void handle(final TransferWithGiftContext context) {
                                    context.writeLog(logger);

                                    int vcPointType = Misc.getVoucherPointType(context);
                                    log.add("voucher and point type", vcPointType);
                                    log.add("begin", "TransferWithGiftContext");
                                    log.add("topup", "with point and voucher");
                                    if (vcPointType != 0) {
                                        CoreCommon.tranWithPointAndVoucher(vertx
                                                , log
                                                , context.phone
                                                , context.pin
                                                , context.point
                                                , context.voucher
                                                , context.amount
                                                , Const.CoreProcessType.OneStep
                                                , Const.CoreTranType.Topup
                                                , ""
                                                , channel
                                                , "0" + cusNum
                                                , vcPointType
                                                , DEFAULT_CORE_TIMEOUT,
                                                new Handler<Response>() {
                                                    @Override
                                                    public void handle(final Response coreReply) {
                                                        context.error = coreReply.Error;
                                                        context.tranId = coreReply.Tid;
                                                        context.tranType = MomoProto.TranHisV1.TranType.TOP_UP_VALUE;
                                                        context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject result) {

                                                                if (result.getInteger("error", -1000) != 0) {
                                                                    log.add("use gift", "use gift error");
                                                                    log.add("error", result.getInteger("error"));
                                                                    log.add("desc", SoapError.getDesc(result.getInteger("error")));
                                                                }

                                                                context.returnMomo(vertx
                                                                        , logger
                                                                        , GIFT_MOMO_AGENT
                                                                        , coreReply.Error, new Handler<JsonObject>() {
                                                                    @Override
                                                                    public void handle(JsonObject results) {
                                                                        if (results.getInteger("error", -1000) != 0) {
                                                                            logger.error("returnMomo error!");
                                                                        }

                                                                        JsonObject joReply = Misc.getJsonObjRpl(coreReply.Error
                                                                                , coreReply.Tid
                                                                                , context.amount, -1);
                                                                        joReply.putNumber("point", context.curPoint);
                                                                        log.add("curPoint ------------------->", context.curPoint);
                                                                        Misc.addCustomNumber(joReply, "0" + cusNum);
                                                                        mCom.sendTransReply(vertx, joReply
                                                                                , System.currentTimeMillis()
                                                                                , msg
                                                                                , sock
                                                                                , data
                                                                                , callback);

                                                                        giftTranDb.save(new GiftTran(context), null);

                                                                        if (context.error == 0) {
                                                                            //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

                                                                            requestVisaPoint(msg, log, context, sock, data);
                                                                            //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                                            //BEGIN 0000000052 IRON MAN
                                                                            //requestIronManPromo(msg, log, context);
                                                                            updateIronManVoucher(msg, log, new JsonArray());
                                                                            //END 0000000052 IRON MAN
                                                                        }

                                                                    }
                                                                });
                                                                JsonObject jsonRep = new JsonObject();
                                                                jsonRep.putNumber("error", context.error);
                                                                jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                                jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                                                jsonRep.putString(StringConstUtil.SERVICE, "topup");
                                                                if (callback != null) {
                                                                    callback.handle(jsonRep);
                                                                }
                                                                log.writeLog();
                                                            }
                                                        });
                                                    }
                                                }
                                        );
                                        return;
                                    }

                                    //send this to soap
                                    log.add("topup", "via soapin");
                                    vertx.eventBus().send(
                                            AppConstant.SoapVerticle_ADDRESS,
                                            MomoMessage.buildBuffer(
                                                    SoapProto.MsgType.TOP_UP_VALUE,
                                                    msg.cmdIndex,
                                                    msg.cmdPhone,
                                                    builder.build()
                                                            .toByteArray()
                                            ),
                                            new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> result) {

                                                    JsonObject tranReply = result.body();
                                                    //Khoa: Truyen them point
                                                    tranReply.putNumber("point", context.curPoint);
                                                    log.add("curPoint ------------------->", context.curPoint);

                                                    //dgd --> gui thong bao tc ve cho khach hang
                                                    int rcode = result.body().getInteger(colName.TranDBCols.ERROR, -1);
                                                    long tranId = result.body().getLong(colName.TranDBCols.TRAN_ID, -100);
                                                    log.add("rcode", rcode);
                                                    log.add("tranId", tranId);
                                                    log.add("result.body", result.body());


                                                    Misc.addCustomNumber(tranReply, "0" + cusNum);
                                                    mCom.sendTransReply(vertx
                                                            , tranReply
                                                            , ackTime
                                                            , msg, sock, data, callback);

                                                    if (isRetailer && rcode == 0) {
                                                        sendSmsTopupRetailer(tranId, tranAmount, msg.cmdPhone, cusNum);
                                                    }
                                                    if (rcode == 0) {
                                                        //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master
                                                        context.tranId = tranId;
                                                        requestVisaPoint(msg, log, context, sock, data);
                                                        //END 0000000015 Kiem tra luon tra khuyen mai visa master
                                                        updateIronManVoucher(msg, log, new JsonArray());
                                                    }
                                                    JsonObject jsonRep = new JsonObject();
                                                    jsonRep.putNumber("error", rcode);
                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                                    jsonRep.putString(StringConstUtil.SERVICE, "topup");
                                                    if (callback != null) {
                                                        callback.handle(jsonRep);
                                                    }
                                                    log.writeLog();
                                                }
                                            }
                                    );

                                }
                            }
                    );
                }
            });

        } // END IF willGetBonus
        else {

            //top up binh thuong
            //todo lam sau topup 2 buoc qua
            String vtelTarget = TopupMapping.getTargetTopup("0" + cusNum, "0" + msg.cmdPhone, log);
            boolean enable = getTopup2StepEnable(JsonViettel2Step);
            String busAddress = getTopup2StepBusAddress(JsonViettel2Step);

            if (enable
                    && ("viettelv1.airtime".equalsIgnoreCase(vtelTarget)
                    || "viettelv2.airtime".equalsIgnoreCase(vtelTarget))) {

                log.add("enable", enable);
                log.add("busAddress", busAddress);
                log.add("begin topup 2 step", "---------");

                topUp2Step(msg
                        , data
                        , tranAmount
                        , cusNum
                        , ackTime
                        , sock
                        , isRetailer
                        , log
                        , fRequest
                        , busAddress
                        , channel
                        , new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                        logger.info("topup 2steps reply jsonObj --->" + jsonObject);
                        if (callback != null) {
                            callback.handle(jsonObject);
                        }
                    }
                });

                return;
            }

            //top up with point and voucher
            TransferWithGiftContext.build(msg.cmdPhone,
                    "topup",
                    "0" + cusNum,
                    tranAmount,
                    vertx,
                    giftManager,
                    data,
                    TRANWITHPOINT_MIN_POINT,
                    TRANWITHPOINT_MIN_AMOUNT,
                    logger,
                    new Handler<TransferWithGiftContext>() {
                        @Override
                        public void handle(final TransferWithGiftContext context) {
                            context.writeLog(logger);

                            int vcPointType = Misc.getVoucherPointType(context);
                            log.add("voucher and point type", vcPointType);
                            if (vcPointType != 0) {
                                log.add("begin", "TransferWithGiftContext");
                                log.add("topup", "with point and voucher");
                                CoreCommon.tranWithPointAndVoucher(vertx
                                        , log
                                        , context.phone
                                        , context.pin
                                        , context.point
                                        , context.voucher
                                        , context.amount
                                        , Const.CoreProcessType.OneStep
                                        , Const.CoreTranType.Topup
                                        , ""
                                        , channel
                                        , "0" + cusNum
                                        , vcPointType
                                        , DEFAULT_CORE_TIMEOUT,
                                        new Handler<Response>() {
                                            @Override
                                            public void handle(final Response coreReply) {
                                                context.error = coreReply.Error;
                                                context.tranId = coreReply.Tid;
                                                context.tranType = MomoProto.TranHisV1.TranType.TOP_UP_VALUE;
                                                context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject result) {

                                                        if (result.getInteger("error", -1000) != 0) {
                                                            log.add("use gift", "use gift error");
                                                            log.add("error", result.getInteger("error"));
                                                            log.add("desc", SoapError.getDesc(result.getInteger("error")));
                                                        }

                                                        context.returnMomo(vertx
                                                                , logger
                                                                , GIFT_MOMO_AGENT
                                                                , coreReply.Error, new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject result) {
                                                                if (result.getInteger("error", -1000) != 0) {
                                                                    logger.error("returnMomo error!");
                                                                }

                                                                JsonObject joReply = Misc.getJsonObjRpl(coreReply.Error
                                                                        , coreReply.Tid
                                                                        , context.amount, -1);
                                                                joReply.putNumber("point", context.curPoint);
                                                                log.add("curPoint ------------------->", context.curPoint);
                                                                Misc.addCustomNumber(joReply, "0" + cusNum);
                                                                //Khoa: Truyen them point
                                                                mCom.sendTransReply(vertx, joReply
                                                                        , System.currentTimeMillis()
                                                                        , msg
                                                                        , sock
                                                                        , data
                                                                        , callback);

                                                                giftTranDb.save(new GiftTran(context), null);

                                                            }
                                                        });
                                                        if (context.error == 0) {
                                                            //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

                                                            requestVisaPoint(msg, log, context, sock, data);
                                                            //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                            //BEGIN 0000000052 IRON MAN
                                                            //requestIronManPromo(msg, log, context);
                                                            updateIronManVoucher(msg, log, new JsonArray());
                                                            //END 0000000052 IRON MAN
                                                        }
                                                        JsonObject jsonRep = new JsonObject();
                                                        jsonRep.putNumber("error", context.error);
                                                        jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                        jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                                        jsonRep.putString(StringConstUtil.SERVICE, "topup");
                                                        if (callback != null) {
                                                            callback.handle(jsonRep);
                                                        }
                                                        log.writeLog();
                                                    }
                                                });
                                            }
                                        }
                                );
                                return;
                            }

                            //send this to soap
                            log.add("topup", "via soapin");
                            vertx.eventBus().send(
                                    AppConstant.SoapVerticle_ADDRESS,
                                    MomoMessage.buildBuffer(
                                            SoapProto.MsgType.TOP_UP_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.build()
                                                    .toByteArray()
                                    ),
                                    new Handler<Message<JsonObject>>() {
                                        @Override
                                        public void handle(Message<JsonObject> result) {

                                            JsonObject tranReply = result.body();
                                            tranReply.putNumber("point", context.curPoint);
                                            log.add("curPoint ------------------->", context.curPoint);
                                            //dgd --> gui thong bao tc ve cho khach hang
                                            int rcode = result.body().getInteger(colName.TranDBCols.ERROR, -1);

                                            Misc.addCustomNumber(tranReply, "0" + cusNum);
//                                            mCom.sendTransReply(vertx
//                                                    , result.body()
//                                                    , ackTime
//                                                    , msg, sock, data, callback);
                                            //Khoa: Truyen them point
                                            mCom.sendTransReply(vertx
                                                    , tranReply
                                                    , ackTime
                                                    , msg, sock, data, callback);

                                            //dgd --> gui thong bao tc ve cho khach hang
                                            //int rcode = result.body().getInteger(colName.TranDBCols.ERROR, -1);
                                            long tranId = result.body().getLong(colName.TranDBCols.TRAN_ID, -100);

                                            if (isRetailer && rcode == 0) {
                                                sendSmsTopupRetailer(tranId, tranAmount, msg.cmdPhone, cusNum);
                                            }
                                            if (rcode == 0) {
                                                context.tranId = tranId;
                                                //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master
                                                requestVisaPoint(msg, log, context, sock, data);
                                                updateIronManVoucher(msg, log, new JsonArray());
                                                //END 0000000015 Kiem tra luon tra khuyen mai visa master
                                            }
                                            JsonObject jsonRep = new JsonObject();
                                            jsonRep.putNumber("error", rcode);
                                            jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                            jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                            jsonRep.putString(StringConstUtil.SERVICE, "topup");
                                            if (callback != null) {
                                                callback.handle(jsonRep);
                                            }
                                            log.writeLog();
                                        }
                                    }
                            );
                        }
                    }
            );
        }

        //cap nhat session time
        if (sock != null)
            mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());

    }

    private void sendSmsTopupRetailer(long tranId, long tranAmount, int retailerPhone, int cusNum) {
        String smsFormat = NotificationUtils.getSmsFormat(Const.SmsKey.Topup);
        //"sms":"Chuc mung, tai khoan di dong cua quy khach da duoc nap ${amount}d tu DGD (${retailer}) luc ${vntime}. TID: ${tranId}. Xin cam on."
        String sms = DataUtil.stringFormat(smsFormat)
                .put(Const.SmsField.Amount, Misc.formatAmount(tranAmount).replace(",", "."))
                .put(Const.SmsField.Retailer, "0" + retailerPhone)
                .put(Const.SmsField.VnTime, Misc.getDate(System.currentTimeMillis()))
                .put(Const.SmsField.TranId, tranId)
                .toString();

        Misc.sendSms(vertx, cusNum, sms);
    }

    private void topUp2Step(final MomoMessage msg
            , final SockData data
            , final long tranAmount
            , final int cusNum
            , final long ackTime
            , final NetSocket sock
            , final boolean isRetailer
            , final Common.BuildLog log
            , final MomoProto.TranHisV1 request
            , final String busAddress
            , final String channel
            , final Handler<JsonObject> callback) {

        TransferWithGiftContext.build(msg.cmdPhone,
                "topup",
                "0" + cusNum,
                tranAmount,
                vertx,
                giftManager,
                data,
                TRANWITHPOINT_MIN_POINT,
                TRANWITHPOINT_MIN_AMOUNT,
                logger,
                new Handler<TransferWithGiftContext>() {
                    @Override
                    public void handle(final TransferWithGiftContext context) {
                        context.writeLog(logger);
                        //always call topup 2 step include point and voucher

                        ProxyRequest topupRequest = ConnectorCommon.createTopupRequest("0" + msg.cmdPhone
                                , data.pin
                                , "topup"
                                , "0" + cusNum
                                , "0" + cusNum
                                , tranAmount
                                , context.voucher
                                , context.point
                                , 1, channel, "", "", "", "", "");
                        ServiceHelper.doPayment(sock
                                , msg
                                , vertx
                                , topupRequest
                                , mCom
                                , request
                                , log
                                , glbCfg
                                , busAddress, new Handler<ProxyResponse>() {
                            @Override
                            public void handle(ProxyResponse proxyResponse) {

                                context.tranId = proxyResponse.getRequest().getCoreTransId();
                                context.tranType = MomoProto.TranHisV1.TranType.TOP_UP_VALUE;
                                context.error = proxyResponse.getProxyResponseCode();

                                //gui tin nhan cho end-user khi la diem giao dich thuc hien cho
                                if (isRetailer && context.error == 0) {
                                    sendSmsTopupRetailer(context.tranId, tranAmount, msg.cmdPhone, cusNum);
                                }

                                context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject result) {
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

                                                if (context.error == 0) {
                                                    //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master
                                                    requestVisaPoint(msg, log, context, sock, data);

                                                    //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                    //BEGIN 0000000052 IRON MAN
                                                    //requestIronManPromo(msg, log, context);
                                                    updateIronManVoucher(msg, log, new JsonArray());
                                                    //END 0000000052 IRON MAN

                                                    mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);


                                                }
                                                giftTranDb.save(new GiftTran(context), null);
                                            }
                                        });
                                        log.writeLog();
                                        JsonObject jsonReply = new JsonObject();
                                        jsonReply.putNumber("error", context.error);
                                        jsonReply.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                        jsonReply.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                        jsonReply.putString(StringConstUtil.SERVICE, "topup");
                                        if (callback != null) {
                                            callback.handle(jsonReply);
                                        }
                                    }
                                });

                            }
                        });

                        /*int vcPointType = Misc.getVoucherPointType(context);
                        log.add("voucher and point type", vcPointType);
                        if (vcPointType != 0){

                        }*/
                    }
                }
        );





        /*log.add("func","topUp2Step");
        log.add("retailer",isRetailer);
        log.add("call to","CoreCommon.topUp");
        //goi qua vu
        CoreCommon.topUp(vertx
                , msg.cmdPhone
                , data.pin
                , tranAmount
                , WalletType.MOMO
                , cusNum
                , "", log
                , new Handler<Response>() {
            @Override
            public void handle(Response requestObj) {

                JsonObject joReply = Misc.getJsonObjRpl(requestObj.Error, requestObj.Tid, tranAmount, -1);
                Misc.addCustomNumber(joReply, "0" + cusNum);
                mCom.sendTransReply(vertx, joReply, ackTime, msg, sock, data, callback);

                log.writeLog();

                //dgd --> gui thong bao tc ve cho khach hang
                if (isRetailer && requestObj.Error == 0) {

                    sendSmsTopupRetailer(requestObj.Tid, tranAmount, msg.cmdPhone, cusNum);
                }

            }
        });*/
    }

    public void processBankIn(final NetSocket sock
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

        if (request == null || !request.hasAmount() || !request.hasPartnerCode()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processBankIn", logger, sock, callback)) {
            return;
        }
        final Common common = new Common(vertx, logger, glbCfg);
        String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processBankIn");
        log.add("cmdInd", msg.cmdIndex);
        log.add("channel", channel);
        log.add("amount", request.getAmount());
        final String bankCode = request.getPartnerCode() == null ? "" : request.getPartnerCode();
        log.add("bankcode", bankCode);

        String bankMethod = "";
        if (request.getKvpList().size() > 0) {
            for (MomoProto.TextValue textValue : request.getKvpList()) {
                if (textValue.getText().equalsIgnoreCase("bankmethod")) {
                    bankMethod = textValue.getValue();
                    break;
                }
            }
        }
        log.add("bankMethod", bankMethod);

        final long ackTime = System.currentTimeMillis();
        SoapProto.BankIn.Builder builder = SoapProto.BankIn.newBuilder();
        builder.setMpin(_data.pin)
                .setChannel(channel)
                .setAmount(request.getAmount())
                .setBankCode(request.getPartnerCode());
        builder.addKvps(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));

        Buffer bankin = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_IN_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build()
                        .toByteArray()
        );

        log.add("begin", "soapverticle");
        String otp = "";
        final int tranType = request.getTranType();
//        JsonArray share = request.getShare().equalsIgnoreCase("") ? new JsonArray() : new JsonArray(request.getShare());
//
//        if (share.size() > 0) {
//            for (Object o : share) {
//                otp = ((JsonObject) o).getString("otp", "");
//                if (!otp.equalsIgnoreCase("")) break;
//            }
//        }
        log.add("share", request.getShare());
        log.writeLog();
        try {
            JsonArray share = request.getShare().equalsIgnoreCase("") ? new JsonArray() : new JsonArray(request.getShare());
            if (share.size() > 0) {
                for (Object o : share) {
                    otp = ((JsonObject) o).getString("otp", "");
                    if (!otp.equalsIgnoreCase("")) break;
                }
            }
        } catch (Exception ex) {
            JsonObject jsonShare = request.getShare().equalsIgnoreCase("") ? new JsonObject() : new JsonObject(request.getShare());
            otp = jsonShare.getString("otp", "");
        }
        //Kiem tra neu la luon nhan OTP thi goi qua connector
        final String bankVerticle = bankCfg.getString(StringConstUtil.BANK, "bankVerticle");
        boolean allow_bank_via_connector = bankCfg.getBoolean(StringConstUtil.ALLOW_BANK_VIA_CONNECTOR, false);
        final int timer = bankCfg.getInteger(StringConstUtil.TIMER, 5);
        log.add("TIMER", timer);
        log.add("allow_bank_via_connector", allow_bank_via_connector);
        log.add("bank verticle ", bankVerticle);
//        KeyValuePair client = new KeyValuePair();
//        client.setKey("client");
//        client.setValue("backend");
//
//        KeyValuePair chanel = new KeyValuePair();
//        chanel.setKey("chanel");
//        chanel.setValue(channel);
//
//        com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
//        kvIsSMS.setKey("issms");
//        kvIsSMS.setValue("no");
        final MomoProto.TranHisV1 f_request = request;
        final Map<String, String> kvps = new HashMap<>();
        kvps.put("client", "backend");
        kvps.put("chanel", "mobi");
        kvps.put("issms", "no");
        //String curTime = String.valueOf(System.currentTimeMillis());
        //String phoneStr_tmp = String.valueOf(msg.cmdPhone);
        final String phoneStr = String.valueOf(msg.cmdPhone).substring(String.valueOf(msg.cmdPhone).length() - 1 - 5, String.valueOf(msg.cmdPhone).length() - 1);
        final String curPhoneStr = String.valueOf(System.currentTimeMillis()) + phoneStr;
        final long curPhoneLong = Long.parseLong(curPhoneStr);
        log.add("phoneStr ", phoneStr);
        log.add("curPhoneStr ", curPhoneStr);
        log.add("curPhoneLong ", curPhoneLong);

        if (allow_bank_via_connector) {
            final String otp_tmp = otp;
            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(final PhonesDb.Obj obj) {
                    String bnk_tmp = bankCode;
                    if ((DataUtil.strToInt(bankCode) == 0) && obj != null) {
                        bnk_tmp = obj.bank_code.equalsIgnoreCase("") || obj.bank_code.equalsIgnoreCase("0") ? "0" : obj.bank_code;
                    } else if (DataUtil.strToInt(bankCode) == 0) {
                        bnk_tmp = "0";
                    }
                    log.add("bank code again", bnk_tmp);
                    BankRequest bankRequest = BankRequestFactory.cashinRequest(bnk_tmp, "0" + msg.cmdPhone, _data.pin, curPhoneLong, f_request.getAmount(), otp_tmp, kvps);
                    log.add("bank request", bankRequest.getJsonObject());
                    log.writeLog();
//                    vertx.createHttpClient().post("", new Handler<HttpClientResponse>() {
//                        @Override
//                        public void handle(HttpClientResponse httpClientResponse) {
//
//                        }
//                    });
                    vertx.eventBus().sendWithTimeout(bankVerticle, bankRequest.getJsonObject(), 60 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                            JsonObject jsonRep = new JsonObject();
                            Buffer buf;
                            MomoProto.TranHisV1.Builder builder = f_request.toBuilder();
                            if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                                BankResponse bankResponse = new BankResponse(messageAsyncResult.result().body());
                                log.add("result", "Co ket qua tra ve tu connector");
                                log.add("bankResponse", bankResponse);
                                builder.setTranType(tranType);

                                if (bankResponse != null && bankResponse.getResultCode() == 0) {
                                    //Tra ket qua thanh cong ve cho client
                                    log.add("resultCode", bankResponse.getResultCode());
                                    updateIronManVoucher(msg, log, new JsonArray());
                                    builder.setStatus(4)
                                            .setError(bankResponse.getResultCode())
                                            .setDesc(bankResponse.getDescription());
                                    //build buffer --> soap verticle
//                            buf = MomoMessage.buildBuffer(
//                                MomoProto.MsgType.TRANS_REPLY_VALUE,
//                                msg.cmdIndex,
//                                msg.cmdPhone,
//                                MomoProto.TextValueMsg.newBuilder().
//                                        addKeys(MomoProto.TextValue.newBuilder().setText(StringConstUtil.ERROR).setValue("" + bankResponse.getResultCode())
//                                        .setText(StringConstUtil.DESCRIPTION).setValue(bankResponse.getDescription())).build().toByteArray()
//                            );
                                    buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.TRANS_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.build().toByteArray()
                                    );
                                } else {
                                    int errorCode = bankResponse == null ? 1000 : bankResponse.getResultCode();
                                    String desc = bankResponse == null ? "Khong nhan duoc ket qua tu core" : bankResponse.getDescription();
                                    log.add("errorCode", errorCode);
                                    log.add("desc", desc);

                                    builder.setStatus(5)
                                            .setError(errorCode)
                                            .setDesc(desc);

                                    buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.TRANS_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.build().toByteArray()
                                    );
                                }
                                final Buffer buf_tmp = buf;
                                vertx.setTimer(timer * 1000L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long aLong) {
                                        mCom.sendCurrentAgentInfoWithCallback(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject jsonObject) {
                                                common.writeDataToSocket(sock, buf_tmp);

                                            }
                                        });
                                    }
                                });


//                        mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                                if(bankResponse.getResultCode() == 0)
                                {
                                    //Chay chuong trinh khuyen mai thang 10
                                    log.add("func", "Kiem tra khuyen mai thang 10");
                                    promotionProcess.getUserInfoToCheckPromoProgram("", "0" + msg.cmdPhone, obj, bankResponse.getRequest().getCoreTransId(), MomoProto.TranHisV1.TranType.BANK_IN_VALUE, f_request.getAmount(),
                                            StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, _data, new JsonObject());
                                }
                                log.writeLog();
                                return;
                            } else {
                                //Tra loi time out, khong ket noi duoc voi connector
                                builder.setStatus(5)
                                        .setError(1000)
                                        .setDesc("Time out");

                                buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.TRANS_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        builder.build().toByteArray()
                                );

                                common.writeDataToSocket(sock, buf);
                                mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }
            });
        } else {
            //Luon banklinked cu, khong nhan BankMethod
            vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, bankin, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> result) {
                    final JsonObject tranRpl = result.body();

                    log.add("end", "soapverticle");
                    log.writeLog();

                /*tranRpl.putNumber(colName.TranDBCols.ERROR,0);
                tranRpl.putNumber(colName.TranDBCols.STATUS,TranObj.STATUS_OK);*/
                    Buffer buf;
                    MomoProto.TranHisV1.Builder builder = f_request.toBuilder();
                    log.add("tranRpl", tranRpl);
//                    agentsDb.getOneAgent("0" + msg.cmdPhone, "", new Handler<AgentsDb.StoreInfo>() {
//                        @Override
//                        public void handle(AgentsDb.StoreInfo storeInfo) {
//                            if (tranRpl.getInteger(colName.TranDBCols.STATUS, -1) == 5 && storeInfo != null && storeInfo.agent_type == 3
//                                    && !storeInfo.deleted) {
//                                Buffer buf;
//                                MomoProto.TranHisV1.Builder builder = f_request.toBuilder();
//
//                                builder.setError(1000000)
//                                        .setDesc(StringConstUtil.IS_NOT_DCNTT_FUNCTION).setStatus(5);
//                                buf = MomoMessage.buildBuffer(
//                                        MomoProto.MsgType.TRANS_REPLY_VALUE,
//                                        msg.cmdIndex,
//                                        msg.cmdPhone,
//                                        builder.build().toByteArray());
//                                if (mCom != null) {
//                                    mCom.writeDataToSocket(sock, buf);
//                                }
//                                log.writeLog();
//                                return;
//                            }

                    mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, callback);

                    int error = tranRpl.getInteger(colName.TranDBCols.ERROR, -1);
                    long tranId = tranRpl.getLong(colName.TranDBCols.TRAN_ID, -1);
                    long amount = tranRpl.getLong(colName.TranDBCols.AMOUNT, 0);

                    if (error == 0) {

                        PhonesDb.Obj phoneObj = ((_data == null || _data.getPhoneObj() == null) ? null : _data.getPhoneObj());

                        String inviter = (phoneObj == null || phoneObj.inviter == null) ? "" : phoneObj.inviter;
                        if (phoneObj == null) return;

                        //chay chuong trinh uu dai map vi viettinbank vnpt-ha noi.
                        if ("102".equalsIgnoreCase(bankCode)) {
                            VcbCommon.requestGiftMomoViettinbank(vertx
                                    , msg.cmdPhone
                                    , "vtbmomo"
                                    , amount
                                    , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
                                    , tranId
                                    , phoneObj.bankPersonalId
                                    , bankCode
                                    , false
                                    , 0
                                    , "vnpthn"
                                    , "vtbpromo"
                                    , false
                                    , ReqObj.online, inviter, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jsonObject) {
                                }
                            });
                            return;
                        }

                        //vcb theo ma momo
                        if ("momo".equalsIgnoreCase(phoneObj.inviter.trim())) {
                            VcbCommon.requestGiftMomoForB(vertx
                                    , msg.cmdPhone
                                    , phoneObj.inviter.trim()
                                    , amount
                                    , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
                                    , tranId
                                    , phoneObj.bankPersonalId
                                    , phoneObj.bank_code, true);
                            return;
                        }

                        //tra khuyen mai cho chuong trinh promotion girl va end-user
                        if (DataUtil.strToInt(phoneObj.inviter) >= pgCodeMin && DataUtil.strToInt(phoneObj.inviter) <= pgCodeMax) {

                            //tra thuong cho end-user nhap ma gioi thieu la PG
                            VcbCommon.requestGiftMomoForB(vertx
                                    , msg.cmdPhone
                                    , String.valueOf(DataUtil.strToInt(phoneObj.inviter))
                                    , 0
                                    , 0
                                    , 0, phoneObj.bankPersonalId
                                    , phoneObj.bank_code
                                    , true);

                            return;
                        }

                        // tra khuyen mai theo amway
                        if (DataUtil.strToInt(phoneObj.inviter) >= amwayCodeMin && DataUtil.strToInt(phoneObj.inviter) <= amwayCodeMax) {

                            //tra thuong cho end-user nhap ma gioi thieu la PG
                            VcbCommon.requestGiftMomoForAmwayPromo(vertx
                                    , msg.cmdPhone
                                    , String.valueOf(DataUtil.strToInt(phoneObj.inviter))
                                    , 0
                                    , 0
                                    , 0, phoneObj.bankPersonalId
                                    , phoneObj.bank_code
                                    , true);

                            return;
                        }

                        //vcb theo so dien thoai nguoi gioi thieu
                        if (DataUtil.strToInt(phoneObj.inviter) > 0) {
                            VcbCommon.requestGiftForB(vertx
                                    , msg.cmdPhone
                                    , phoneObj.inviter
                                    , amount
                                    , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
                                    , tranId);

                            return;
                        }
                    }
//                        }
//                    });
                }
            });
        }
        //cap nhat session time
        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
    }

    public void processM2CTransfer(final NetSocket sock
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

        //phone|name|cmnd|amount|notice

        if (request == null || !request.hasAmount()
                || !request.hasPartnerId()
                || !request.hasPartnerName()
                || !request.hasPartnerCode()
                || !request.hasAmount()
                || !request.hasComment()) {


            if (sock != null) {
                mCom.writeErrorToSocket(sock);
            }
            if (callback != null) {
                callback.handle(new JsonObject().putNumber("error", -1).putString("description", ""));
            }
            return;
        }


        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processM2CTransfer", logger, sock, callback)) {
            return;
        }

        String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        //build buffer --> soap verticle
        Buffer m2cTransfer = MomoMessage.buildBuffer(
                SoapProto.MsgType.M2C_TRANSFER_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                SoapProto.M2CTransfer.newBuilder()
                        .setAgent("0" + String.valueOf(msg.cmdPhone))
                        .setMpin(_data.pin)
                        .setPhone(request.getPartnerId())
                        .setName(request.getPartnerName())
                        .setCardId(request.getPartnerCode())
                        .setChannel(channel)
                        .setAmount(request.getAmount())
                        .setNotice(request.getComment())
                        .build()
                        .toByteArray()
        );

        final long ackTime = System.currentTimeMillis();

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, m2cTransfer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                JsonObject json = result.body();
                mCom.sendTransReply(vertx, json, ackTime, msg, sock, _data, callback);
            }
        });
        //cap nhat session time
        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
    }

    public void processC2CReceive(final NetSocket sock
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

        if (request == null) {

            if (sock != null) {
                mCom.writeErrorToSocket(sock);
            }
            if (callback != null) {
                callback.handle(new JsonObject().putNumber("error", -1).putString("description", ""));
            }
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processC2CReceive", logger, sock, callback)) {
            return;
        }

        String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processC2CReceive");


        String pin = _data == null ? "" : _data.pin;
        final long amount = request.getAmount();
        final long ackTime = System.currentTimeMillis();

        if ("".equalsIgnoreCase(pin)) {
            JsonObject tranRpl = Misc.getJsonObjRpl(SoapError.SYSTEM_ERROR, System.currentTimeMillis(), request.getAmount(), 1);
            mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, null);
            return;
        }

        String shareData = request.getShare() == null ? "" : request.getShare();
        final JsonArray requestInfo = "".equalsIgnoreCase(shareData) ? new JsonArray() : new JsonArray(shareData);


        //nguoi nhan
        final String rcvName = Misc.getInfoByKey(Const.C2C.receiverName, requestInfo);
        final String rcvCardId = Misc.getInfoByKey(Const.C2C.receiverCardId, requestInfo);
        final String rcvPhone = Misc.getInfoByKey(Const.C2C.receiverPhone, requestInfo);

        //nguoi gui
        final String sndName = Misc.getInfoByKey(Const.C2C.senderName, requestInfo);
        final String sndCardId = Misc.getInfoByKey(Const.C2C.senderCardId, requestInfo);
        final String sndPhone = Misc.getInfoByKey(Const.C2C.senderPhone, requestInfo);

        //dai ly nhan tien
        final String retailerAddr = Misc.getInfoByKey(Const.C2C.retailerAddress, requestInfo);
        final String retailerMomoPhone = Misc.getInfoByKey(Const.C2C.retailerPhone, requestInfo);

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(request.getKvpList());
        final String retailer = hashMap.containsKey(Const.DGD.Retailer) ? hashMap.get(Const.DGD.Retailer) : "0";
        final boolean isRetailer = "1".equalsIgnoreCase(retailer) ? true : false;

        final int tranType = request.getTranType();

        final String rcvCashCode = request.getBillId() == null ? "" : request.getBillId();
        log.add("mtcn", rcvCashCode);
        log.add("amount", amount);
        log.add("channel", channel);
        log.add("retailer", isRetailer);
        log.add("call", "doC2cCommit");
        final MomoProto.TranHisV1 fRequest = request;

        CoreCommon.doC2cCommit(vertx
                , log
                , "0" + msg.cmdPhone
                , pin
                , rcvCashCode
                , DEFAULT_CORE_TIMEOUT
                , null, new Handler<Response>() {
            @Override
            public void handle(final Response requestObj) {
                agentsDb.getOneAgent("0" + msg.cmdPhone, "doC2cCommit", new Handler<AgentsDb.StoreInfo>() {
                    @Override
                    public void handle(AgentsDb.StoreInfo storeInfo) {
                        log.add("core result", requestObj.Error);
                        log.add("core desc", requestObj.Description);
                        log.add("soap desc", SoapError.getDesc(requestObj.Error));
                        log.writeLog();
                        if (requestObj.Error == 0) {
                            //xoa DB
                            mntDb.remove(rcvCashCode, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                }
                            });

                            mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);

                        }

                        InformObj iobj = Misc.getContentFromCfg(hmRetailer, fRequest.getTranType(), requestObj.Error, logger);
                        //noti c2c
                        Notification noti = new Notification();
                        noti.receiverNumber = msg.cmdPhone;
                        noti.sms = String.format(iobj.sms, Misc.formatAmount(amount).replace(",", "."), rcvName, rcvPhone);
                        noti.tranId = requestObj.Tid;
                        noti.priority = 2;
                        noti.time = System.currentTimeMillis();
                        noti.category = 0;
                        noti.caption = iobj.cap;
                        noti.body = String.format(iobj.body, Misc.formatAmount(amount).replace(",", "."), rcvName, rcvPhone);
                        noti.htmlBody = noti.body;
                        noti.status = Notification.STATUS_DETAIL;
                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                        Misc.sendNoti(vertx, noti);

                        //tran c2c
                        final TranObj tran = new TranObj();
                        tran.cmdId = msg.cmdIndex;
                        tran.clientTime = ackTime;
                        tran.ackTime = ackTime;
                        tran.finishTime = System.currentTimeMillis();
                        tran.tranType = tranType;
                        tran.tranId = requestObj.Tid;
                        tran.error = requestObj.Error;
                        tran.status = requestObj.Error == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL;
                        tran.io = 1;
                        tran.category = 0;
                        tran.parterCode = fRequest.getPartnerCode();
                        tran.amount = amount;
                        tran.partnerId = fRequest.getPartnerId();
                        tran.partnerName = fRequest.getPartnerName();
                        tran.partnerRef = fRequest.getPartnerRef();
                        tran.comment = fRequest.getComment();
                        tran.billId = fRequest.getBillId();
                        tran.cmdId = msg.cmdIndex;
                        tran.desc = "";
                        tran.share = new JsonArray(fRequest.getShare());
                        tran.kvp = new JsonObject().putString(Const.DGD.Retailer, "1");
                        if (requestObj.Error == 7 && storeInfo != null && storeInfo.agent_type == 3
                                && storeInfo.status != 2) {
                            tran.desc = StringConstUtil.IS_NOT_DCNTT_FUNCTION;
                            tran.error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
                        }
                        mCom.sendTransReplyByTran(msg, tran, transDb, sock);

                        //cap nhat session time
                        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
                    }
                });
            }
        });
    }

    public void processM2MTransfer(final NetSocket sock
            , final MomoMessage msg
            , final SockData _data
            , final List<SoapProto.keyValuePair> kvpList
            , final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasPartnerId()
                || !request.hasAmount()
                || !request.hasComment()) {

            if (sock != null) {
                mCom.writeErrorToSocket(sock);
            }

            if (callback != null) {
                callback.handle(new JsonObject().putNumber("error", -1).putString("description", ""));
            }
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processM2MTransfer", logger, sock, callback)) {
            return;
        }

        String agent = "0" + String.valueOf(msg.cmdPhone);
        String mpin = _data.pin;

        String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        //request tu client
        //key: service_name
        //value:
        //thanh toan QRCode: QRCode
        //vay tien tu ban be : muon_tien
        String value = (request.getBillId() == null ? "" : request.getBillId());
        final long amount = request.getAmount();

        final String customNumber = (request.getPartnerId() == null || "".equalsIgnoreCase(request.getPartnerId())) ? "" : request.getPartnerId();

        SoapProto.M2MTransfer.Builder builder = SoapProto.M2MTransfer.newBuilder();
        builder.setAgent(agent)
                .setMpin(mpin)
                .setPhone(request.getPartnerId())
                .setChannel(channel)
                .setAmount(amount)
                .setNotice(request.getComment());

        //neu co key thi them vao
        if (value != null && !"".equalsIgnoreCase(value)) {

            //dua vao service mode
            if ("qrcode".equalsIgnoreCase(value)) {
                builder.addKvps(SoapProto.keyValuePair.newBuilder()
                        .setKey(Const.SERCICE_MODE)
                        .setValue(value));
            } else {
                //dua vao service name
                builder.addKvps(SoapProto.keyValuePair.newBuilder()
                        .setKey(Const.SERVICE_NAME)
                        .setValue(value));
            }
        }

        //
        if (kvpList != null && kvpList.size() > 0) {
            for (int i = 0; i < kvpList.size(); i++) {
                builder.addKvps(SoapProto.keyValuePair.newBuilder()
                                .setKey(kvpList.get(i).getKey())
                                .setValue(kvpList.get(i).getValue())
                );
            }
        }

        //dgd.start
        HashMap<String, String> hashMapM2M = Misc.buildKeyValueHashMap(request.getKvpList());
        String retailer = (hashMapM2M.containsKey(Const.DGD.Retailer) ? hashMapM2M.get(Const.DGD.Retailer) : "");
        final String cusnum = "".equalsIgnoreCase(customNumber) ?
                (hashMapM2M.containsKey(Const.DGD.CusNumber) ? hashMapM2M.get(Const.DGD.CusNumber) : "") :
                customNumber;
        final boolean isRetailer = (Const.DGDValues.Retailer == DataUtil.strToInt(retailer)
                && !"".equalsIgnoreCase(cusnum));
        //dgd.end

        final long ackTime = System.currentTimeMillis();
        final int rcvPhone = Integer.valueOf(request.getPartnerId());
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processM2MTransfer");
        log.add("rcv number", "0" + rcvPhone);
        log.add("amount", request.getAmount());
        log.add("start getAgentStatus", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
        log.add("isRetailer", isRetailer);
        log.add("test log", "check m2m");
        final String orgComment = request.getComment();
        builder.addKvps(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));
        //build buffer --> soap verticle
        final Buffer m2mTransfer = MomoMessage.buildBuffer(
                SoapProto.MsgType.M2M_TRANSFER_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build().toByteArray()
        );

        //1 get agentstatus
        final MomoProto.TranHisV1 f_request = request;
        Misc.getAgentStatus(vertx, rcvPhone, log, phonesDb, new Handler<SoapVerticle.ObjCoreStatus>() {
            @Override
            public void handle(SoapVerticle.ObjCoreStatus objCoreStatus) {

                log.add("end getAgentStatus", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                log.add("isReged", objCoreStatus.isReged);
                log.add("isActivated", objCoreStatus.isActivated);
                log.add("isSuspended", objCoreStatus.isSuspended);
                log.add("isFrozen", objCoreStatus.isFrozen);
                log.add("isStopped", objCoreStatus.isStopped);

                //registered already
                if (objCoreStatus.isReged) {
                    log.add("do", "M2M");
                    //todo lam theo luong M2M
                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS
                            , m2mTransfer
                            , new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> result) {
                            final JsonObject tranRpl = result.body();
                            Misc.addCustomNumber(tranRpl, customNumber);
                            int error = tranRpl.getInteger(colName.TranDBCols.ERROR, 0);
                            log.add("tranRpl", tranRpl);
                            log.add("error", error);

                            if (error == 0) {
                                final long tranId = tranRpl.getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());
                                log.add("tranId", tranId);
                                //retailer do m2m
                                if (isRetailer) {

                                    mCom.sendTransReply(vertx
                                            , tranRpl
                                            , ackTime
                                            , msg
                                            , sock
                                            , _data
                                            , callback);
                                    log.add("desc", "bat dau kiem tra khuyen mai");
                                    //todo xoa log
                                    log.writeLog();
                                    promotionProcess.getUserInfoToCheckPromoProgram("0" + msg.cmdPhone, "0" + rcvPhone, null, tranId, MomoProto.TranHisV1.TranType.M2M_VALUE, amount, StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, _data,
                                            new JsonObject());
                                    return;
                                }
                                log.add("existsMMPhone", "processM2MTransfer");
                                //check receiver is retailer
                                agentsDb.existsMMPhone("0" + rcvPhone, "processM2MTransfer", new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean exist) {
                                        if (exist) {
                                            mCom.sendTransReply(vertx
                                                    , tranRpl
                                                    , ackTime
                                                    , msg
                                                    , sock
                                                    , _data
                                                    , callback);
                                        } else {
                                            Misc.getM2MFee(vertx, MomoProto.TranHisV1.TranType.M2M_VALUE, amount,"0" + msg.cmdPhone, "0" + rcvPhone, new Handler<FeeDb.Obj>() {
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

                                                    mCom.sendTransReply(vertx
                                                            , tranRpl
                                                            , ackTime
                                                            , msg
                                                            , sock
                                                            , _data
                                                            , callback);
                                                    //Kiem tra khuyen mai thang 10
                                                    log.add("func", "Kiem tra khuyen mai thang 10");
                                                    log.writeLog();
                                                    promotionProcess.getUserInfoToCheckPromoProgram("0" + msg.cmdPhone, "0" + rcvPhone, null, tranId, MomoProto.TranHisV1.TranType.M2M_VALUE, amount, StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, _data
                                                            , new JsonObject());
                                                }
                                            });
                                        }
                                    }
                                });
                            } else {
                                //giao dich loi
                                log.add("desc", "Giao dich loi te le");
                                agentsDb.getOneAgent("0" + msg.cmdPhone, "", new Handler<AgentsDb.StoreInfo>() {
                                    @Override
                                    public void handle(AgentsDb.StoreInfo storeInfo) {
                                        if (storeInfo != null && storeInfo.agent_type == 3
                                                && storeInfo.status != 2) {
                                            Buffer buf;
                                            MomoProto.TranHisV1.Builder builder = f_request.toBuilder();

                                            builder.setError(1000000)
                                                    .setDesc(StringConstUtil.IS_NOT_DCNTT_FUNCTION).setStatus(5);

                                            //Them builder cho m2m
                                            MomoProto.TextValue textValue = MomoProto.TextValue.getDefaultInstance();
                                            builder.addKvp(textValue.toBuilder().setText("htmlpopup").setValue(StringConstUtil.M2M_HTML_FAIL));

                                            buf = MomoMessage.buildBuffer(
                                                    MomoProto.MsgType.TRANS_REPLY_VALUE,
                                                    msg.cmdIndex,
                                                    msg.cmdPhone,
                                                    builder.build().toByteArray());

                                            if (mCom != null) {
                                                mCom.writeDataToSocket(sock, buf);
                                            }
                                            log.writeLog();
                                            return;
                                        }
                                        Misc.addCustomNumber(tranRpl, customNumber);
                                        mCom.sendTransReply(vertx
                                                , tranRpl
                                                , ackTime
                                                , msg
                                                , sock
                                                , _data
                                                , callback);
                                    }
                                });
                            }
                        }
                    });
                } else {
                    //todo lam theo M2N moi
                    log.add("do", "M2N");

                    //khong phai DGD
                    if (!isRetailer) {
                        log.add("func", "saveM2Number");
                        saveM2Number(sock, msg, _data, callback);

                    } else {
                        long tranIdErr = -2000;
                        final JsonObject failReply = Misc.getJsonObjRpl(SoapError.TARGET_NOT_FOUND, tranIdErr, amount, -1);
                        agentsDb.getOneAgent("0" + msg.cmdPhone, "", new Handler<AgentsDb.StoreInfo>() {
                            @Override
                            public void handle(AgentsDb.StoreInfo storeInfo) {
                                if (storeInfo != null && storeInfo.agent_type == 3
                                        && storeInfo.status != 2) {
                                    Buffer buf;
                                    MomoProto.TranHisV1.Builder builder = f_request.toBuilder();

                                    builder.setError(1000000)
                                            .setDesc(StringConstUtil.IS_NOT_DCNTT_FUNCTION).setStatus(5);
                                    buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.TRANS_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.build().toByteArray());
                                    if (mCom != null) {
                                        mCom.writeDataToSocket(sock, buf);
                                    }
                                    log.writeLog();
                                    return;
                                }
                                log.add("retailer", "diem giao dich khong chuyen M2N duoc");

                                mCom.sendTransReply(vertx
                                        , failReply
                                        , ackTime
                                        , msg
                                        , sock
                                        , _data
                                        , callback);
                            }
                        });

                    }
                    log.writeLog();
                }

                mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
            }
        });
    }

    private void sendSmsM2mRetailer(long tranId, long amount, int retailerPhone, String cusnum) {

        String smsFormat = NotificationUtils.getSmsFormat(Const.SmsKey.M2m);
        //"sms":"Chuc mung quy khach da duoc nap ${amount}d tu DGD (${retailer}) luc ${vntime}. TID: ${tranId}. Xin cam on."
        String sms = DataUtil.stringFormat(smsFormat)
                .put(Const.SmsField.Amount, Misc.formatAmount(amount).replace(",", "."))
                .put(Const.SmsField.Retailer, "0" + retailerPhone)
                .put(Const.SmsField.VnTime, Misc.getDate(System.currentTimeMillis()))
                .put(Const.SmsField.TranId, tranId)
                .toString();

        Misc.sendSms(vertx, DataUtil.strToInt(cusnum), sms);
    }

    private void commit123Phim(final Response rplObj,
                               final com.mservice.momo.vertx.processor.Common.BuildLog log,
                               final long amount,
                               final MomoMessage msg,
                               final NetSocket sock,
                               final SockData _data,
                               final FilmInfo filmInfo,
                               final String customNumber,
                               final Handler<JsonObject> callback,
                               final Handler<VngUtil.ObjReply> cbConfirm) {

        log.add("func", "commit123Phim");
        final long ackTime = System.currentTimeMillis();
        if (rplObj.Error != 0) {

            log.add("error", rplObj.Error);
            log.add("tid", rplObj.Tid);
            log.add("description", rplObj.Description);

            JsonObject tranErr = Misc.getJsonObjRpl(rplObj.Error
                    , rplObj.Tid
                    , amount
                    , -1);
            Misc.addCustomNumber(tranErr, customNumber);
            tranErr.putNumber(colName.TranDBCols.STATUS, TranObj.STATUS_FAIL);
            tranErr.putNumber(colName.TranDBCols.ERROR, rplObj.Error);

            //lock tien khong thanh cong --> tra ket qua ve cho client
            mCom.sendTransReply(vertx, tranErr, ackTime, msg, sock, _data, null);

            //gui lenh huy qua 123phim
            VngUtil.ObjConfirmOrCancel cancel = new VngUtil.ObjConfirmOrCancel();
            cancel.invoice_no = filmInfo.invoiceNo;
            JsonObject jsoncancelReq = VngUtil.getJsonCancel(cancel);

            jsoncancelReq.putString(vngClass.PHONE_NUMBER, log.getPhoneNumber());
            jsoncancelReq.putNumber(vngClass.TIME, log.getTime());

            log.add("call cancel to", "cinema verticle");
            vertx.eventBus().sendWithTimeout(AppConstant.VinaGameCinemaVerticle_ADDRESS
                    , jsoncancelReq
                    , 60000
                    , new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> jcancelrpl) {

                    VngUtil.ObjReply canObjRpl;

                    final JsonObject jsonCancel = new JsonObject();

                    if (jcancelrpl.succeeded()) {

                        JsonObject json = jcancelrpl.result().body();
                        canObjRpl = new VngUtil.ObjReply(json);
                        log.add("date_cancel", canObjRpl.date_cancel);
                        log.add("error", canObjRpl.error);
                        log.add("desc", canObjRpl.desc);
                        log.add("errdesc", Phim123Errors.getDesc(canObjRpl.error));
                    } else {
                        jsonCancel.putNumber(vngClass.Res.error, 5008);
                        jsonCancel.putString(vngClass.Res.desc, "123phim orderCancel with timeout");
                        canObjRpl = new VngUtil.ObjReply(jsonCancel);
                        log.add("error", canObjRpl.error);
                        log.add("desc", canObjRpl.desc);
                        log.add("errdesc", "123phim orderCancel with timeout");
                    }
                }
            });

            //tra ket qua ve de xu ly xu dung point and voucher
            VngUtil.ObjReply commitObjReply = new VngUtil.ObjReply();
            commitObjReply.error = rplObj.Error;
            cbConfirm.handle(commitObjReply);
            return;
        }

        final long lockedTranId = rplObj.Tid;
        final JsonObject tranRpl = Misc.getJsonObjRpl(rplObj.Error
                , lockedTranId
                , amount
                , -1);

        Misc.addCustomNumber(tranRpl, customNumber);

        log.add("tid", rplObj.Tid);
        log.add("error", rplObj.Error);
        log.add("desc", rplObj.Description);
        log.add("step 2", "confirm123Phim");

        log.add("invoice_no", filmInfo.invoiceNo);
        log.add("payment_code", lockedTranId);

        VngUtil.ObjConfirmOrCancel confirm = new VngUtil.ObjConfirmOrCancel();
        confirm.invoice_no = filmInfo.invoiceNo;
        confirm.payment_code = lockedTranId;
        confirm.phone_number = log.getPhoneNumber();
        confirm.time = log.getTime();
        JsonObject jsoncfmReq = VngUtil.getJsonConfirm(confirm);

        log.add("call confirm to", "cinema verticle");
//        vertx.eventBus().sendWithTimeout(AppConstant.VinaGameCinemaVerticle_ADDRESS
//                , jsoncfmReq
//                , 600000
//                , new Handler<AsyncResult<Message<JsonObject>>>()
        vertx.eventBus().send(AppConstant.VinaGameCinemaVerticle_ADDRESS
                , jsoncfmReq
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsoncfmRpl) {

                final VngUtil.ObjReply objRpl;

                //thanh cong
//                if (jsoncfmRpl.succeeded()) {
//                    JsonObject json = jsoncfmRpl.body();
//                    objRpl = new VngUtil.ObjReply(json);
//
//                    //reset lastCinemaInvoiceNo
//                    if (objRpl.error == 0 && _data != null) {
//                        _data.lastCinemaInvoiceNo = "";
//                    }
//
//                    log.add("date_confirm", objRpl.date_confirm);
//                    log.add("error", objRpl.error);
//                    log.add("desc", objRpl.desc);
//                    log.add("errdesc", Phim123Errors.getDesc(objRpl.error));
//
//                } else {
//                    //timeout
//                    final JsonObject jsonConfirm = new JsonObject();
//
//                    if (jsoncfmRpl.failed()) {
//                        jsonConfirm.putNumber(vngClass.Res.error, 5008);
//                        jsonConfirm.putString(vngClass.Res.desc, "123phim orderConfirm with timeout");
//
//                    } else {
//                        jsonConfirm.putNumber(vngClass.Res.error, 5008);
//                        jsonConfirm.putString(vngClass.Res.desc, "123phim orderConfirm with unfinited error");
//                    }
//
//                    objRpl = new VngUtil.ObjReply(jsonConfirm);
//                    log.add("error", objRpl.error);
//                    log.add("desc", objRpl.desc);
//                    log.add("errdesc", Phim123Errors.getDesc(objRpl.error));
//                }

                //todo test
                //objRpl.error = 0;
                JsonObject json = jsoncfmRpl.body();
                objRpl = new VngUtil.ObjReply(json);

                //reset lastCinemaInvoiceNo
                if (objRpl.error == 0 && _data != null) {
                    _data.lastCinemaInvoiceNo = "";
                }

                log.add("date_confirm", objRpl.date_confirm);
                log.add("error", objRpl.error);
                log.add("desc", objRpl.desc);
                log.add("errdesc", Phim123Errors.getDesc(objRpl.error));

                tranRpl.putNumber(colName.TranDBCols.ERROR, objRpl.error);
                tranRpl.putNumber(colName.TranDBCols.STATUS, (objRpl.error == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL));

                tranRpl.putString(colName.TranDBCols.PARTNER_INVOICE_NO, filmInfo.invoiceNo);
                tranRpl.putString(colName.TranDBCols.PARTNER_TICKET_CODE, objRpl.ticket_code);
                tranRpl.putNumber(colName.TranDBCols.PARTNER_ERROR, objRpl.error);
                tranRpl.putString(colName.TranDBCols.PARTNER_DESCRIPTION, objRpl.desc);
                tranRpl.putString(colName.TranDBCols.PARTNER_ACTION, "confirm");

                //refine ticket code for galaxy cinema because of when confirm success, they will give a ticket code via confirm
                if ("".equalsIgnoreCase(filmInfo.ticketCode)) {
                    tranRpl.putString("ticketcode", objRpl.ticket_code);
                }

                if ("".equalsIgnoreCase(filmInfo.ticketCode)) {
                    filmInfo.ticketCode = objRpl.ticket_code;
                }

                filmInfo.tranId = lockedTranId;

                //khuyen mai galaxy
                if (objRpl.error == 0) {

                    //khuyen mai chuong trinh galaxy : 1.voucher 2: combo bap nuoc
                    doPromoGalaxy(msg
                            , sock
                            , filmInfo);

                    //tinh hoa hong cho PG Galaxy
                    PhonesDb.Obj phoneOb = ((_data == null || _data.getPhoneObj() == null) ? null : _data.getPhoneObj());
                    if (phoneOb != null) {
                        String pgCode = phoneOb.inviter;
                        long createDate = phoneOb.createdDate;
                        if (DataUtil.strToInt(pgCode) >= pgCodeMin
                                && DataUtil.strToInt(pgCode) <= pgCodeMax
                                && createDate >= 1422032400000L
                                ) {

                            //tra thuong cho PG
                            requestPromotionForPG(tranRpl
                                    , _data
                                    , msg
                                    , amount
                                    , "phim123", MomoProto.TranHisV1.TranType.PHIM123_VALUE
                                    , promo_girl_buyfilm);

                        }
                    }
                }


                mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, callback);
                /*try {
                    mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, callback);
                } catch (Exception e) {
                    logger.error("mCom.sendTransReply", e);
                }*/

                log.add("result", objRpl.error);
                log.add("desc", objRpl.desc);
                log.add("errdesc", Phim123Errors.getDesc(objRpl.error));

                //confirm at 123phim side failed
                if (objRpl.error > 0) {

                    //tra ket qua ve de xu ly xu dung point and voucher
                    VngUtil.ObjReply commitObjReply = new VngUtil.ObjReply();
                    commitObjReply.error = objRpl.error;
                    cbConfirm.handle(commitObjReply);

                    log.add("step 3", "rollbackTran");
                    log.add("rollback with tranid", lockedTranId);

                    Request rbObj = new Request();
                    rbObj.TIME = log.getTime();
                    rbObj.PHONE_NUMBER = log.getPhoneNumber();
                    rbObj.TYPE = Command.ROLLBACK;
                    rbObj.TRAN_ID = lockedTranId;
                    JsonObject jrb = rbObj.toJsonObject();

                    vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, jrb, new Handler<Message<Buffer>>() {
                        @Override
                        public void handle(Message<Buffer> message) {

                            MomoMessage momoMessage = MomoMessage.fromBuffer(message.body());
                            Core.StandardReply reply;
                            try {
                                reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
                            } catch (Exception ex) {
                                reply = null;
                            }

                            if (reply == null) {
                                log.add("rollback result", "Core.StandardReply null");
                            } else {
                                log.add("rollback result", reply.getErrorCode());
                                log.add("tid", reply.getTid());
                                log.add("error", (reply.getErrorCode() == 0 || reply.getErrorCode() == 103) ? 0 : reply.getErrorCode());
                            }

                            //gui lenh huy qua 123phim
                            VngUtil.ObjConfirmOrCancel cancel = new VngUtil.ObjConfirmOrCancel();
                            cancel.invoice_no = filmInfo.invoiceNo;
                            JsonObject jsoncancelReq = VngUtil.getJsonCancel(cancel);

                            jsoncancelReq.putString(vngClass.PHONE_NUMBER, log.getPhoneNumber());
                            jsoncancelReq.putNumber(vngClass.TIME, log.getTime());

                            log.add("call cancel to", "cinema verticle");
                            vertx.eventBus().sendWithTimeout(AppConstant.VinaGameCinemaVerticle_ADDRESS
                                    , jsoncancelReq
                                    , 60000
                                    , new Handler<AsyncResult<Message<JsonObject>>>() {
                                @Override
                                public void handle(AsyncResult<Message<JsonObject>> jcancelrpl) {

                                    VngUtil.ObjReply canObjRpl = null;

                                    final JsonObject jsonCancel = new JsonObject();

                                    if (jcancelrpl.succeeded()) {

                                        JsonObject json = jcancelrpl.result().body();
                                        canObjRpl = new VngUtil.ObjReply(json);
                                        log.add("date_cancel", canObjRpl.date_cancel);
                                        log.add("error", canObjRpl.error);
                                        log.add("desc", canObjRpl.desc);
                                        log.add("errdesc", Phim123Errors.getDesc(canObjRpl.error));
                                    } else {
                                        jsonCancel.putNumber(vngClass.Res.error, 5008);
                                        jsonCancel.putString(vngClass.Res.desc, "123phim orderCancel with timeout");
                                        canObjRpl = new VngUtil.ObjReply(jsonCancel);
                                        log.add("error", canObjRpl.error);
                                        log.add("desc", canObjRpl.desc);
                                        log.add("errdesc", "123phim orderCancel with timeout");
                                    }

                                    log.writeLog();
                                }
                            });
                        }
                    });
                } else {

                    //confirm at 123phim side success --> commit this transaction
                    log.add("step 3", "commitTran");
                    Request commitObj = new Request();
                    commitObj.TYPE = Command.COMMIT;
                    commitObj.TRAN_ID = lockedTranId;
                    commitObj.PHONE_NUMBER = log.getPhoneNumber();
                    commitObj.TIME = log.getTime();

                    JsonObject commitJo = commitObj.toJsonObject();

                    log.add("call commit to", "core connector verticle");
                    vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, commitJo, new Handler<Message<Buffer>>() {
                        @Override
                        public void handle(Message<Buffer> message) {
                            MomoMessage momoMessage = MomoMessage.fromBuffer(message.body());
                            Core.StandardReply reply;
                            try {
                                reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
                            } catch (Exception ex) {
                                reply = null;
                            }

                            if (reply == null) {
                                log.add("commit result", "Core.StandardReply null");
                            } else {
                                log.add("error", reply.getErrorCode());
                                log.add("desc", reply.getDescription());
                                log.add("tid", reply.getTid());
                            }

                            //tra ket qua ve de xu ly xu dung point and voucher
                            VngUtil.ObjReply commitObjReply = new VngUtil.ObjReply();
                            commitObjReply.error = reply.getErrorCode();
                            cbConfirm.handle(commitObjReply);
                            log.writeLog();
                        }
                    });
                }
            }
        });
    }

    private void doPromoGalaxy(final MomoMessage msg
            , final NetSocket sock
            , final FilmInfo filmInfo) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("cinemaShortname", filmInfo.cinemaShortName);
        log.add("func", "doPromoGalaxy");

        final Promo.PromoReqObj reqGlxRec = new Promo.PromoReqObj();
        reqGlxRec.COMMAND = PromoType.GET_PROMOTION_REC;
        reqGlxRec.PROMO_NAME = "galaxy";

        Misc.requestPromoRecord(vertx, reqGlxRec, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                final PromotionDb.Obj glxRec = new PromotionDb.Obj(jsonObject);

                //khong co chuog trinh
                if (glxRec == null || glxRec.DATE_TO == 0 || glxRec.DATE_FROM == 0) {
                    log.add("galaxydesc", "Khong co chuong trinh khuyen mai nao");
                    log.writeLog();
                    return;
                }

                long curTime = System.currentTimeMillis();

                //het thoi gian khuyen mai
                if (curTime < glxRec.DATE_FROM || glxRec.DATE_TO < curTime) {
                    log.add("galaxydesc", "het thoi gian khuyen mai");
                    log.writeLog();
                    return;
                }

                //khong phai rap galaxy
                if (!"glx".equalsIgnoreCase(filmInfo.cinemaShortName)) {
                    sendNotiInformChance(glxRec, msg);

                    log.add("galaxydesc", "Khong phai dat ve xem phim rap Galaxy");
                    log.writeLog();
                    return;
                }

                phim123Glx.findOne(msg.cmdPhone, new Handler<Promo123PhimGlxDb.Obj>() {
                    @Override
                    public void handle(final Promo123PhimGlxDb.Obj glxObj) {

                        //het so lan khuyen mai
                        if (glxObj.PROMO_COUNT >= glxRec.MAX_TIMES) {
                            log.add("galaxydesc", "het so lan khuyen mai");
                            log.writeLog();
                            return;
                        }

                        transDb.findOneByTranType(msg.cmdPhone
                                , MomoProto.TranHisV1.TranType.PHIM123_VALUE
                                , glxRec.DATE_FROM, new Handler<Integer>() {
                            @Override
                            public void handle(Integer count) {
                                //khong tra thuong -- da thuc hien giao dich mua ve xem phim truoc do
                                if (count > 0) {
                                    //tracking cho cham soc khach hang o day
                                    Promo123PhimGlxDb.Obj bhdObj = new Promo123PhimGlxDb.Obj();
                                    bhdObj.ID = msg.cmdPhone + "";
                                    bhdObj.TICKET_CODE = filmInfo.ticketCode;
                                    bhdObj.INVOICE_NO = filmInfo.invoiceNo;
                                    bhdObj.BUY_TIME = filmInfo.buyDate;
                                    bhdObj.DISPLAY_TIME = filmInfo.displayTime;
                                    bhdObj.NUMBER_OF_SEAT = filmInfo.seatAmount;
                                    bhdObj.SEAT_LIST = filmInfo.seatList;
                                    bhdObj.FILM_NAME = filmInfo.filmName;
                                    bhdObj.RAP = filmInfo.cinemaFullName;
                                    bhdObj.SUBRAP = filmInfo.cinemaSubName;
                                    bhdObj.STATUS = Promo123PhimGlxDb.STATUS_NEW;
                                    bhdObj.DESC = "Đã mua vé xem phim trước đợt khuyến mãi nên không trả thưởng";
                                    bhdObj.PROMO_COUNT = 50;
                                    bhdObj.PROMO_TIMEVN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
                                    bhdObj.TIME = System.currentTimeMillis();
                                    bhdObj.TIMEVN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
                                    bhdObj.AMOUNT = filmInfo.amount;

                                    phim123Glx.save(bhdObj, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {
                                        }
                                    });

                                    log.add("galaxydesc", "Da co giao dich dat ve xem phim truoc day --> khong tra VC/Combo");
                                    log.writeLog();
                                    return;
                                }

                                //thuc hien khuyen mai tai day
                                //chua tra thuong lan nao
                                log.add("showTime", filmInfo.displayTime);
                                log.add("showTimeAsLong", filmInfo.displayTimeAsLong);
                                if (glxObj.PROMO_COUNT == 0) {

                                    //chay phase 2, chi tra combo bap nuoc khi khach hang da mua ve xem phim lan dau
                                    log.add("enable phase2", glxRec.ENABLE_PHASE2);
                                    if (glxRec.ENABLE_PHASE2) {
                                        savedBookedTime01Ok(msg, filmInfo, "Đã mua lần 1");

                                        //gui thong bao co hoi nhan qua khi mua ve lan thu 2
                                        sendNotiInformChance(glxRec, msg);

                                        log.add("desc-phase 2", "only track booked first time successfully");
                                        log.writeLog();
                                        return;
                                    }
                                    //---------------------------------------------------------------------

                                    if (filmInfo.amount < glxRec.TRAN_MIN_VALUE) {
                                        log.add("galaxydesc", "Khong tra thuong vi : " + filmInfo.amount + " < " + glxRec.PER_TRAN_VALUE);
                                        return;
                                    }

                                    log.add("galaxydesc", "tra voucher 30000d");
                                    //tang vc
                                    ArrayList<Misc.KeyValue> listKeyValue = new ArrayList<>();
                                    listKeyValue.add(new Misc.KeyValue("promo", "galaxy"));
                                    listKeyValue.add(new Misc.KeyValue("cinema", filmInfo.cinemaShortName));
                                    giftManager.adjustGiftValue(glxRec.ADJUST_ACCOUNT
                                            , "0" + msg.cmdPhone
                                            , glxRec.PER_TRAN_VALUE
                                            , listKeyValue
                                            , new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            int error = jsonObject.getInteger("error", -1);
                                            long tranId = jsonObject.getLong("tranId", -1);
                                            log.add("error", error);
                                            log.add("galaxydesc", SoapError.getDesc(error));

                                            if (error == 0) {

                                                final String giftTypeId = "galaxy";
                                                GiftType giftType = new GiftType();
                                                giftType.setModelId(giftTypeId);

                                                ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                                Misc.KeyValue kv = new Misc.KeyValue();
                                                kv.Key = "cinema";
                                                kv.Value = "glx";
                                                keyValues.add(kv);

                                                giftManager.createLocalGift("0" + msg.cmdPhone
                                                        , glxRec.PER_TRAN_VALUE
                                                        , giftType
                                                        , tranId
                                                        , glxRec.ADJUST_ACCOUNT
                                                        , glxRec.DURATION
                                                        , keyValues, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject jsonObject) {
                                                        int err = jsonObject.getInteger("error", -1);
                                                        long tranId = jsonObject.getInteger("tranId", -1);

                                                        log.add("galaxydesc", "tra thuong galaxy bang gift");
                                                        log.add("err", err);

                                                        if (err == 0) {

                                                            Gift gift = new Gift(jsonObject.getObject("gift"));
                                                            String tranComment = "Vui xem phim Galaxy cùng Ví MoMo.";
                                                            String notiCaption = "Nhận thưởng thẻ quà tặng!";
                                                            //String notiBody="Chúc mừng Bạn vừa nhận Thẻ quà tặng từ chương trình “Có MoMo không lo hết vé”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi” và kích hoạt thẻ quà tặng Bạn vừa nhận! Thẻ quà tặng có hiệu lực sau 2 tiếng kể từ khi nhận được. LH: (08) 399 171 99.";
                                                            String notiBody = "Chúc mừng Bạn vừa nhận Thẻ quà tặng 30.000đ từ chương trình “Vui xem phim Galaxy cùng Ví MoMo”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi” và kích hoạt thẻ quà tặng Bạn vừa nhận! Thẻ quà tặng có hiệu lực sau 2 tiếng kể từ khi nhận được. LH: (08) 399 171 99.";

                                                            String giftMessage = tranComment;//"Có MoMo không lo hết vé và hơn thế nữa";
                                                            Misc.sendTranHisAndNotiGift(vertx
                                                                    , msg.cmdPhone
                                                                    , tranComment
                                                                    , tranId
                                                                    , glxRec.PER_TRAN_VALUE
                                                                    , gift
                                                                    , notiCaption
                                                                    , notiBody
                                                                    , giftMessage
                                                                    , transDb);

                                                            savedBookedTime01Ok(msg, filmInfo, "Trả thưởng bằng voucher");

                                                            log.writeLog();

                                                        } else {
                                                            log.writeLog();
                                                        }
                                                    }
                                                });
                                            } else {
                                                log.add("galaxydesc", "Tra qua galaxy loi");
                                                log.writeLog();
                                            }
                                        }
                                    });
                                    return;
                                }

                                //tang combo bap nuoc
                                if (glxObj.PROMO_COUNT == 1) {

                                    //gia tri khong du tien de nhan qua
                                    if (filmInfo.amount < glxRec.TRAN_MIN_VALUE) {
                                        log.add("glx", "gia tri giao dich nho hon gia tri toi thieu " + filmInfo.amount + " < " + glxRec.TRAN_MIN_VALUE);
                                        log.writeLog();
                                        sendNotiInformChance(glxRec, msg);
                                        return;
                                    }

                                    /*else{
                                        //du tien nhan qua nhung chua du 2h sau khi mua lan 1
                                        if(glxObj.TIME + 2*60*60*1000 > System.currentTimeMillis()){

                                            log.add("glx","chua du sau 2h - khong tra tra combo nhan bap nuoc");
                                            log.writeLog();
                                            sendNotiInformChance(glxRec,msg);
                                            return;
                                        }
                                    }*/

                                    long curTime = System.currentTimeMillis();

                                    //khong co thoi gian off , thoi gian chieu khong nam trong chuong trinh khuyen mai
                                    if (filmInfo.displayTimeAsLong < glxRec.DATE_FROM
                                            || glxRec.DATE_TO < filmInfo.displayTimeAsLong) {

                                        log.add("galaxydesc", "Thoi gian chieu khong nam trong thoi gian khuyen mai, khong co offtime");
                                        log.writeLog();
                                        return;
                                    }

                                    //sau 2h moi set tang tiep combo
                                    if ((glxObj.TIME + 2 * 60 * 60 * 1000L) > curTime) {
                                        sendNotiInformChance(glxRec, msg);
                                        log.add("galaxydesc", "Sau 2h moi duoc xet tang combo nhap bap nuoc");
                                        log.writeLog();
                                        return;
                                    }

                                    log.add("glx", "bat dau tra combo bap nuoc");
                                    //tang code
                                    final String code = "MOMO" + codeUtilGLX.getNextCode();
                                    JsonObject joUp = new JsonObject();
                                    joUp.putString(colName.Phim123PromoGlxCols.PROMO_CODE, code);
                                    joUp.putNumber(colName.Phim123PromoGlxCols.AMOUNT, filmInfo.amount);
                                    joUp.putString(colName.Phim123PromoGlxCols.STATUS, Promo123PhimGlxDb.STATUS_NEW);
                                    joUp.putString(colName.Phim123PromoGlxCols.DESC, Promo123PhimGlxDb.DESC_NEW);
                                    joUp.putNumber(colName.Phim123PromoGlxCols.PROMO_COUNT, 2);
                                    joUp.putString(colName.Phim123PromoGlxCols.DISPLAY_TIME, filmInfo.displayTime);
                                    joUp.putString(colName.Phim123PromoGlxCols.BUY_TIME, filmInfo.buyDate);
                                    joUp.putString(colName.Phim123PromoGlxCols.SEAT_LIST, filmInfo.seatList);
                                    joUp.putNumber(colName.Phim123PromoGlxCols.NUMBER_OF_SEAT, filmInfo.seatAmount);
                                    joUp.putString(colName.Phim123PromoGlxCols.RAP, filmInfo.cinemaFullName);
                                    joUp.putString(colName.Phim123PromoGlxCols.FILM_NAME, filmInfo.filmName);
                                    joUp.putString(colName.Phim123PromoGlxCols.INVOICE_NO, filmInfo.invoiceNo);
                                    joUp.putString(colName.Phim123PromoGlxCols.TICKET_CODE, filmInfo.ticketCode);
                                    joUp.putString(colName.Phim123PromoGlxCols.SUBRAP, filmInfo.cinemaSubName);
                                    joUp.putString(colName.Phim123PromoGlxCols.PROMO_TIMEVN, Misc.dateVNFormatWithTime(System.currentTimeMillis()));

                                    //cap nhat so lan khuyen mai len 1
                                    phim123Glx.update(msg.cmdPhone, joUp, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {

                                            //we want delay sometime to make sure client recieved tranhis
                                            vertx.setTimer(500, new Handler<Long>() {
                                                @Override
                                                public void handle(Long aLong) {

                                                    String bodyTmplate = "Mã nhận COMBO: %s; Phim %s; Rạp %s; Suất chiếu %s .Vui lòng xuất trình mã nhận Combo và vé xem phim theo quy định để nhận quà. LH: (08) 399 171 99.";
                                                    String notiBody = String.format(bodyTmplate
                                                            , code
                                                            , filmInfo.filmName
                                                            , filmInfo.cinemaFullName
                                                            , filmInfo.displayTime);

                                                    TranObj tranObj = new TranObj();
                                                    tranObj.owner_number = msg.cmdPhone;
                                                    tranObj.error = 0;
                                                    tranObj.status = TranObj.STATUS_OK;
                                                    tranObj.tranId = filmInfo.tranId;
                                                    tranObj.tranType = MomoProto.TranHisV1.TranType.PHIM123_VALUE;
                                                    tranObj.clientTime = System.currentTimeMillis();
                                                    tranObj.ackTime = System.currentTimeMillis();
                                                    tranObj.finishTime = System.currentTimeMillis();
                                                    tranObj.comment = notiBody;
                                                    tranObj.amount = filmInfo.amount;
                                                    tranObj.io = -1;
                                                    tranObj.category = 0;
                                                    tranObj.cmdId = msg.cmdIndex;

                                                    Misc.sendTranAsSyn(msg, sock, transDb, tranObj, mCom);

                                                    //send noti
                                                    String notiCap = "Nhận thưởng phiếu COMBO khuyến mãi!";
                                                    Notification noti = new Notification();
                                                    noti.receiverNumber = msg.cmdPhone;
                                                    noti.sms = "";
                                                    noti.tranId = filmInfo.tranId;
                                                    noti.priority = 2;
                                                    noti.time = System.currentTimeMillis();
                                                    noti.category = 0;
                                                    noti.caption = notiCap;
                                                    noti.body = notiBody;
                                                    noti.status = Notification.STATUS_DETAIL;
                                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                                    Misc.sendNoti(vertx, noti);
                                                }
                                            });
                                        }
                                    });
                                    log.writeLog();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    //ghi nhan da tra khuyen mai lan 1 roi

    /**
     * tracking the first time we paid promotion by voucher for this wallet
     *
     * @param msg
     * @param filmInfo
     * @param desc
     */
    private void savedBookedTime01Ok(MomoMessage msg
            , FilmInfo filmInfo
            , String desc) {
        //cap nhat so lan khuyen mai len 1
        Promo123PhimGlxDb.Obj saveObj = new Promo123PhimGlxDb.Obj();
        saveObj.ID = msg.cmdPhone + "";
        saveObj.TICKET_CODE = filmInfo.ticketCode;
        saveObj.INVOICE_NO = filmInfo.invoiceNo;
        saveObj.BUY_TIME = filmInfo.buyDate;
        saveObj.DISPLAY_TIME = filmInfo.displayTime;
        saveObj.NUMBER_OF_SEAT = filmInfo.seatAmount;
        saveObj.SEAT_LIST = filmInfo.seatList;
        saveObj.FILM_NAME = filmInfo.filmName;
        saveObj.RAP = filmInfo.cinemaFullName;
        saveObj.SUBRAP = filmInfo.cinemaSubName;
        saveObj.STATUS = Promo123PhimGlxDb.STATUS_NEW;
        saveObj.DESC = desc;
        saveObj.PROMO_COUNT = 1;
        saveObj.PROMO_TIMEVN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        saveObj.TIME = System.currentTimeMillis();
        saveObj.TIMEVN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        saveObj.AMOUNT = filmInfo.amount;

        phim123Glx.save(saveObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {

            }
        });
    }

    private void sendNotiInformChance(PromotionDb.Obj glxRec, MomoMessage msg) {

        //khong co chuong trinh khuyen mai nao
        if (glxRec == null || glxRec.DATE_FROM == 0 || glxRec.DATE_TO == 0) {
            return;
        }

        //het thoi gian khuyen mai
        long curTime = System.currentTimeMillis();
        if ((curTime < glxRec.DATE_FROM) || (glxRec.DATE_TO < curTime)) {
            return;
        }

        String cap = "Galaxy Khuyến mãi";
        String tmp = "Từ %s - %s, Khi mua 2 vé xem phim Galaxy lần thứ hai qua Ví MoMo, KH được tặng ngay 1 combo (bắp nước) trị giá 61.000đ. Lưu ý: Giao dịch mua vé lần thứ nhất phải cách giao dịch lần thứ 2 ít nhất là 2h. LH: 08 399 171 99.";

        String body = String.format(tmp
                , Misc.dateFormatWithParten(glxRec.DATE_FROM, "dd/MM")
                , Misc.dateFormatWithParten(glxRec.DATE_TO, "dd/MM/yyyy")
        );

        //send noti
        Notification noti = new Notification();
        noti.receiverNumber = msg.cmdPhone;
        noti.sms = "";
        noti.tranId = System.currentTimeMillis();
        noti.priority = 2;
        noti.time = System.currentTimeMillis();
        noti.category = 0;
        noti.caption = cap;
        noti.body = body;
        noti.status = Notification.STATUS_DETAIL;
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        Misc.sendNoti(vertx, noti);
    }

    public void processPayment123Phim(final NetSocket sock
            , final MomoMessage msg
            , final SockData _data
            , final String invoice_no
            , final String ticket_code
            , final Handler<JsonObject> callback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasAmount()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "processPayment123Phim", logger, sock, callback)) {
            return;
        }

        final String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "processPayment123Phim");

        //1. lock tien
        final String sndNumber = "0" + msg.cmdPhone;
        final String sndPin = _data.pin;
        final long amount = request.getAmount();

        final String rcvNumber = PHIM123_RECEIVER_CASH_ACCOUNT;

        final String serviceId = "123phim";
        final String billerId = "123phim";
        final String specialAgent = PHIM123_RECEIVER_CASH_ACCOUNT;

        final FilmInfo filmInfo = new FilmInfo(request.getPartnerName(), invoice_no, ticket_code, amount);

        final HashMap<String, String> hashMap = Misc.getKeyValuePairs(request.getKvpList());
        final String customNumber = hashMap.containsKey(Const.DGD.CusNumber) ? (String) hashMap.get(Const.DGD.CusNumber) : "";

        String retailer = hashMap.containsKey(Const.DGD.Retailer) ? (String) hashMap.get(Const.DGD.Retailer) : "";
        final boolean isRetailer = Const.DGDValues.Retailer == DataUtil.strToInt(retailer) ? true : false;

        log.add("isRetailer", isRetailer);
        //xu ly theo luong co su dung point and voucher

        ArrayList<String> excludeExtraVals = new ArrayList<>();
        String exclueKey = "";

        //khong lay vc chi ap dung cho galaxy
        if (!"glx".equalsIgnoreCase(filmInfo.cinemaShortName)) {
            excludeExtraVals.add("glx");
            exclueKey = "cinema";
        }
        //BEGIN 0000000004
        final MomoProto.TranHisV1 fRequest = request;

        final String bank_code = fRequest.getPartnerExtra1();
        final String from_bank = fRequest.getPartnerRef() == null ? "" : fRequest.getPartnerRef();
        //END   0000000004
        TransferWithGiftContext.buildWithExclude(msg.cmdPhone, serviceId
                , billerId
                , amount
                , vertx
                , giftManager
                , _data
                , TRANWITHPOINT_MIN_POINT
                , TRANWITHPOINT_MIN_AMOUNT
                , excludeExtraVals
                , exclueKey, logger, new Handler<TransferWithGiftContext>() {
            @Override
            public void handle(final TransferWithGiftContext context) {

                context.writeLog(logger);
                int voucherPointType = Misc.getVoucherPointType(context);
                log.add("voucher point type", voucherPointType);

                if (voucherPointType != 0) {

                    log.add("func", "CoreCommon.tranWithPointAndVoucher");
                    CoreCommon.tranWithPointAndVoucher(vertx
                            , log
                            , msg.cmdPhone
                            , _data.pin
                            , context.point
                            , context.voucher
                            , amount
                            , Const.CoreProcessType.TwoStep
                            , Const.CoreTranType.Billpay
                            , specialAgent
                            , channel
                            , filmInfo.cinemaShortName //"0" + msg.cmdPhone
                            , voucherPointType
                            , DEFAULT_CORE_TIMEOUT, new Handler<Response>() {
                        @Override
                        public void handle(final Response coreReply) {

                            long confirmid = 0;
                            for (KeyValue s : coreReply.KeyValueList) {
                                if ("confirmid".equalsIgnoreCase(s.Key)) {
                                    confirmid = DataUtil.stringToUNumber(s.Value);
                                    break;
                                }
                            }

                            log.add("tranWithPointAndVoucher result", "err : " + coreReply.Error + " desc " + coreReply.Description);
                            coreReply.Tid = confirmid;

                            commit123Phim(coreReply
                                    , log
                                    , amount
                                    , msg
                                    , sock
                                    , _data
                                    , filmInfo, customNumber
                                    , callback, new Handler<VngUtil.ObjReply>() {
                                @Override
                                public void handle(final VngUtil.ObjReply objReply) {
                                    //
                                    context.tranId = coreReply.Tid;
                                    context.error = coreReply.Error;
                                    context.tranType = MomoProto.TranHisV1.TranType.PHIM123_VALUE;
                                    if (objReply.error == 0) {
//                                        JsonObject jsonGift = new JsonObject();
//                                        jsonGift.putString(colName.HC_PRU_VoucherManage.NUMBER, "0" + msg.cmdPhone);
//                                        jsonGift.putString(colName.HC_PRU_VoucherManage.BILL_ID, billerId);
//                                        jsonGift.putString(colName.HC_PRU_VoucherManage.SERVICE_ID, serviceId);
//                                        jsonGift.putString(colName.HC_PRU_VoucherManage.GIFT_ID, context.giftId);
//                                        insertPayOneHcPruBill(jsonGift);

                                        //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

                                        requestVisaPoint(msg, log, context, sock, _data);
                                        //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                        context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject result) {
                                                if (result.getInteger("error", -1000) != 0) {
                                                    log.add("use", "gift error");
                                                    log.add("error", result.getInteger("error"));
                                                    log.add("desc", SoapError.getDesc(result.getInteger("error")));
                                                }

                                                //BEGIN 0000000004
                                                CharSequence common = ",";
                                                CharSequence subtract = "-";
                                                log.add("seatAmount", filmInfo.seatAmount);
                                                log.add("filmInfo.seatList", filmInfo.seatList);
                                                log.add("index", msg.cmdIndex);
                                                long minValue = 100000;
                                                if (filmInfo.seatAmount > 1 || filmInfo.seatList.contains(common) || filmInfo.seatList.contains(subtract)
                                                        || filmInfo.amount > minValue) {
                                                    log.add("func", "requestBillPayPromo");
                                                    BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, context.tranType,
                                                            context.tranId, serviceId, "", new Handler<JsonObject>() {
                                                                @Override
                                                                public void handle(JsonObject jsonObject) {
                                                                    log.add("requestBillPayPromo", jsonObject);
                                                                }
                                                            }
                                                    );

                                                }
                                                //END 0000000004

                                                //Kiem tra ket qua xoa trong queuedgift
                                                if(result.getInteger("error", -1000) == 0)
                                                {
                                                    //BEGIN 0000000052 Iron MAN
                                                    JsonArray giftArray = result.getArray(StringConstUtil.IronManPromo.GIFT_ID_ARRAY, new JsonArray());
                                                    updateIronManVoucher(msg, log, giftArray);
                                                    promotionProcess.updateOctoberPromoVoucherStatus(log, giftArray, "0" + msg.cmdPhone);
                                                    //END 0000000052 IRON MAN
                                                }

                                                //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
                                                context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject result) {
                                                        if (result.getInteger("error", -1000) != 0) {
                                                            logger.error("returnMomo error!");
                                                        }

                                                        if (context.error == 0) {
                                                            mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                                                        }

                                                        //luu transaction infor --> giftTran for tracking
                                                        giftTranDb.save(new GiftTran(context), null);
                                                        log.writeLog();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                    //BEGIN 0000000050
                                    JsonObject jsonRep = new JsonObject();
                                    jsonRep.putNumber("error", objReply.error);
                                    jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                    jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                    jsonRep.putString(StringConstUtil.SERVICE, serviceId);
                                    if (callback != null) {
                                        callback.handle(jsonRep);
                                    }
                                    //END 0000000050
                                }
                            });

                        }
                    });
                } else {

                    log.add("sndNumber", sndNumber);
                    log.add("sndPinLen", (sndPin == null ? "null" : sndPin.length()));
                    log.add("amount", amount);
                    log.add("rcvNumber", rcvNumber);
                    log.add("channel", channel);
                    log.add("step 1", "transferWithLock");

                    ArrayList<KeyValue> listKeyValue = new ArrayList<KeyValue>();
                    listKeyValue.add(new KeyValue("cinetype", filmInfo.cinemaShortName));

                    CoreCommon.transferWithLock(vertx,
                            log,
                            WalletType.MOMO,
                            "0" + msg.cmdPhone,
                            sndPin,
                            rcvNumber,
                            amount,
                            rcvNumber,
                            "123phim"
                            , listKeyValue,
                            new Handler<Response>() {
                                @Override
                                public void handle(Response requestObj) {
                                    commit123Phim(requestObj
                                            , log
                                            , amount
                                            , msg
                                            , sock
                                            , _data
                                            , filmInfo, customNumber
                                            , callback, new Handler<VngUtil.ObjReply>() {
                                        @Override
                                        public void handle(VngUtil.ObjReply objReply) {
                                            if (objReply.error == 0) {
                                                CharSequence common = ",";
                                                CharSequence subtract = "-";
                                                //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

                                                requestVisaPoint(msg, log, context, sock, _data);
                                                //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                //BEGIN 0000000004
                                                log.add("index", msg.cmdIndex);
                                                if (filmInfo.seatAmount > 1 || filmInfo.seatList.contains(common) || filmInfo.seatList.contains(subtract)) {
                                                    BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, context.tranType,
                                                            context.tranId, serviceId, "", new Handler<JsonObject>() {
                                                                @Override
                                                                public void handle(JsonObject jsonObject) {
                                                                    log.add("requestBillPayPromo", jsonObject);
                                                                    log.writeLog();
                                                                }
                                                            }
                                                    );

                                                }
                                                //END 0000000004

                                            }
                                            //BEGIN 0000000050
                                            JsonObject jsonRep = new JsonObject();
                                            jsonRep.putNumber("error", objReply.error);
                                            jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                            jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                            jsonRep.putString(StringConstUtil.SERVICE, serviceId);
                                            if (callback != null) {
                                                callback.handle(jsonRep);
                                            }
                                            //END 0000000050
                                        }
                                    });
                                }
                            }
                    );
                }
            }
        });

        //cap nhat session time
        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
    }

    private void saveTrackDDHHErr(int number, String desc, String error, String code, long voteAmount) {

        CDHHErr.Obj cdhhErrObj = new CDHHErr.Obj();
        cdhhErrObj.desc = desc;
        cdhhErrObj.error = error;
        cdhhErrObj.number = number;
        cdhhErrObj.voteAmount = voteAmount;
        cdhhErrObj.code = code;
        cdhhErr.save(cdhhErrObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });
    }

    private void loadServiceEvents(Vertx _vertVertx, final ArrayList<String> sid) {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_EVENT;

        Misc.getServiceInfo(_vertVertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                if (jsonArray != null) {
                    for (Object o : jsonArray) {
                        JsonObject jo = (JsonObject) o;
                        sid.add(jo.getString("ser_id"));
                    }
                }
            }
        });
    }

    public void processPayOneBill(final NetSocket sock
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
        log.add("service event", serviceIdEvent);
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
        String tmpServiceId = (request.getPartnerId() == null || request.getPartnerId().isEmpty() ? "" : request.getPartnerId());

        tmpServiceId = "".equalsIgnoreCase(tmpServiceId) ?
                (hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : tmpServiceId)
                : tmpServiceId;

        final String serviceId = tmpServiceId;

//        loadServiceEvents(vertx, serviceIdEvent);
        if (serviceIdEvent.size() == 0) {
            loadServiceEvents(vertx, serviceIdEvent);
        }
        if (serviceIdEvent.contains(serviceId)) {

            final MomoProto.TranHisV1 request1 = request;
            JsonObject jsonSearch = new JsonObject();
            jsonSearch.putString(colName.eventContent.ID, serviceId);
            eventContentDb.findOne(serviceId, new Handler<EventContentDb.Obj>() {
                @Override
                public void handle(EventContentDb.Obj obj) {
                    if (obj != null) {
                        processEvent.doEvent(msg, sock, _data, obj, request1);
                    }
                }
            });

            return;
        }
        //BEGIN 0000000004
        final String bank_code = fRequest.getPartnerExtra1();
        final String from_bank = fRequest.getPartnerRef() == null ? "" : fRequest.getPartnerRef();
        //END   0000000004

        String tmpBillId = (request.getBillId() == null || request.getBillId().isEmpty() ? "" : request.getBillId());
        tmpBillId = "".equalsIgnoreCase(tmpBillId) ?
                (hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : tmpBillId)
                : tmpBillId;
        final String billId = tmpBillId;

        //dgd.start

        final String retailer = hashMap.containsKey(Const.DGD.Retailer) ? hashMap.get(Const.DGD.Retailer) : "";
        final String cusnum = hashMap.containsKey(Const.DGD.CusNumber) ? hashMap.get(Const.DGD.CusNumber) : "";
        final boolean isRetailer = (Const.DGDValues.Retailer == DataUtil.strToInt(retailer)
                && DataUtil.strToInt(cusnum) > 0);
        //dgd.end

        long tmpAmount = hashMap.containsKey(Const.AppClient.Amount) ? DataUtil.stringToUNumber(hashMap.get(Const.AppClient.Amount)) : 0;
        final int qty = hashMap.containsKey(Const.AppClient.Quantity) ? DataUtil.strToInt(hashMap.get(Const.AppClient.Quantity)) : 1;
        final String cusName = hashMap.containsKey(Const.AppClient.FullName) ? hashMap.get(Const.AppClient.FullName) : "";
        final String cusInfo = hashMap.containsKey("inf") ? hashMap.get("inf") : "";
        final String cusPhone = hashMap.containsKey(Const.AppClient.Phone) ? hashMap.get(Const.AppClient.Phone) : "";
        final String cusAddress = hashMap.containsKey(Const.AppClient.Address) ? hashMap.get(Const.AppClient.Address) : "";
        final String cusEmail = hashMap.containsKey("email") ? hashMap.get("email") : "";

        tmpAmount = (tmpAmount > 0 ? tmpAmount : request.getAmount());
        final long fAmount = tmpAmount * qty;


        String share = request.getShare();
        log.add("share", share);
        boolean isJsonArrayShare = Misc.isValidJsonArray(share);
        JsonArray jsonArrayShare = new JsonArray();
        JsonObject jsonObjectShare = new JsonObject();
        String phoneToSms = "";
        String confirmpopup = "";
        String moneypopup = "";
        if(isJsonArrayShare && !"".equalsIgnoreCase(share))
        {
            jsonArrayShare = share.equalsIgnoreCase("") ? new JsonArray() : new JsonArray(share);
            if (jsonArrayShare.size() > 0) {
                for (Object o : jsonArrayShare) {
                    phoneToSms = ((JsonObject) o).getString(Const.DGD.CusNumber, "");
                    if (!phoneToSms.equalsIgnoreCase(""))
                        break;
                }
            }
        }
        else if(!"".equalsIgnoreCase(share))
        {
            jsonObjectShare = new JsonObject(share);
            confirmpopup = jsonObjectShare.getString("confirmpopup", "");
            moneypopup = jsonObjectShare.getString("moneypopup", "");
        }



        final String smsPhone = phoneToSms.equalsIgnoreCase("") ? cusnum : phoneToSms;
        log.add("smsPhone", smsPhone);
        log.add("phoneToSms", phoneToSms);
        log.add("confirmpopup", confirmpopup);
        log.add("moneypopup", moneypopup);
        // Chay nhu cu nhe.
        final String confirmPopup_tmp = confirmpopup;
        final String moneypopup_tmp = moneypopup;
        SoapProto.PayOneBill.Builder builder = SoapProto.PayOneBill.newBuilder();
        builder.setPin(_data.pin)
                .setProviderId(serviceId)
                .setBillId(billId)
                .setChannel(channel)
                .setAmount(fAmount);

        builder.addKeyValuePairs(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));

        Buffer tmpPOB;

        log.add("isRetailer", isRetailer);

        if (referalBuilder != null) {
            //them key
            builder.addKeyValuePairs(referalBuilder);

            tmpPOB = MomoMessage.buildBuffer(
                    SoapProto.MsgType.PAY_ONE_BILL_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build()
                            .toByteArray()
            );
        } else {
            tmpPOB = MomoMessage.buildBuffer(
                    SoapProto.MsgType.PAY_ONE_BILL_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build()
                            .toByteArray()
            );
        }

        final Buffer payOneBill = tmpPOB;

        billRuleManageDb.findOne(billId, new Handler<BillRuleManageDb.Obj>() {
            @Override
            public void handle(final BillRuleManageDb.Obj obj) {
                long currentTime = System.currentTimeMillis();

                if (obj != null && currentTime < obj.endTime) {
                    // Khong cho thanh toan hoa don, tra ve thong bao
                    MomoProto.TranHisV1.Builder builderError = fRequest.toBuilder();
                    builderError.setStatus(5); // Tra ve loi cho app
                    builderError.setError(1000000); // EXCEPTION_BILL_PAY
                    builderError.setDesc(ErrorConstString.HC_ONE_DAY_ONE_BILL);
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
                //todo xoa writelog
                log.writeLog();
                Misc.getViaCoreService(vertx, serviceId.toLowerCase(), isStoreApp, new Handler<ViaConnectorObj>() {
                    @Override
                    public void handle(final ViaConnectorObj viaConnectorObj) {
                        log.add("via core connector", viaConnectorObj.IsViaConnectorVerticle);
                        log.add("busname", viaConnectorObj.BusName);
                        log.add("billpay", viaConnectorObj.BillPay);
                        //todo xoa writelog
                        log.writeLog();
                        if (viaConnectorObj.IsViaConnectorVerticle) {

                            log.add("call to", "TransferWithGiftContext");
                            getServiceFee(serviceId, fAmount, log, new Handler<Long>() {
                                @Override
                                public void handle(final Long serviceFee) {
                                    final long totalAmount = fAmount + serviceFee;
                                    TransferWithGiftContext.build(msg.cmdPhone, serviceId, billId, totalAmount, vertx, giftManager, _data,
                                            TRANWITHPOINT_MIN_POINT, TRANWITHPOINT_MIN_AMOUNT, logger,
                                            new Handler<TransferWithGiftContext>() {
                                                @Override
                                                public void handle(final TransferWithGiftContext context) {
                                                    //todo xoa writelog
                                                    log.writeLog();
                                                    context.writeLog(logger);
                                                    log.add("func", "ServiceHelper.doPayment");
                                                    final int voucherPointType = Misc.getVoucherPointType(context);
                                                    final long useVoucher = context.voucher;
                                                    log.add("useVoucer", useVoucher);
                                                    log.add("amount", context.amount);
                                                    log.add("voucher", context.voucher);
                                                    log.add("point", context.point);
                                                    ProxyRequest paymentRequest;
                                                    log.writeLog();
                                                    if(DataUtil.strToInt(confirmPopup_tmp) == 0 && DataUtil.strToInt(moneypopup_tmp) == 1)
                                                    {
                                                        // Cancel
                                                        paymentRequest = ConnectorCommon.cancelPaymentRequest(
                                                                "0" + msg.cmdPhone
                                                                , _data.pin
                                                                , fRequest.getPartnerId()
                                                                , viaConnectorObj.BillPay
                                                                , fRequest.getBillId()
                                                                , fAmount//context.amount
                                                                , context.voucher
                                                                , context.point
                                                                , qty,
                                                                cusName, cusInfo, cusPhone, cusAddress, cusEmail, serviceFee);
                                                    }
                                                    else
                                                    {
                                                        paymentRequest = ConnectorCommon.createPaymentRequest(
                                                                "0" + msg.cmdPhone
                                                                , _data.pin
                                                                , fRequest.getPartnerId()
                                                                , viaConnectorObj.BillPay
                                                                , fRequest.getBillId()
                                                                , fAmount//context.amount
                                                                , context.voucher
                                                                , context.point
                                                                , qty,
                                                                cusName, cusInfo, cusPhone, cusAddress, cusEmail, serviceFee, new JsonObject());
                                                    }


                                                    log.add("isStoreApp ---->", isStoreApp);

                                                    if (isStoreApp) {
                                                        paymentRequest = ConnectorCommon.createPaymentBackEndAgencyRequest(
                                                                "0" + msg.cmdPhone
                                                                , _data.pin
                                                                , fRequest.getPartnerId()
                                                                , viaConnectorObj.BillPay
                                                                , fRequest.getBillId()
                                                                , fAmount//context.amount
                                                                , context.voucher
                                                                , context.point
                                                                , qty,
                                                                cusName, cusInfo, smsPhone, cusAddress, cusEmail, serviceFee, new JsonObject());
                                                    }
                                                    log.add("cusnum ---->", cusnum);
                                                    log.add("cusPhone ---->", cusPhone);
                                                    log.add("payment request content", paymentRequest.toString());
                                                    ServiceHelper.
                                                            doPayment(sock, msg, vertx,
                                                                    paymentRequest, mCom, fRequest, log, glbCfg,
                                                                    viaConnectorObj.BusName, new Handler<ProxyResponse>() {

                                                                        @Override
                                                                        public void handle(final ProxyResponse proxyResponse) {
                                                                            Misc.removeCacheFormData(vertx, serviceId, msg.cmdPhone, isStoreApp, new Handler<JsonObject>() {
                                                                                @Override
                                                                                public void handle(JsonObject jsonObject) {
                                                                                    log.add("desc", jsonObject.encodePrettily());
                                                                                }
                                                                            });
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
//                                                                            if (rcode == 0) {
//                                                                                BillRuleManageDb.Obj objBill = new BillRuleManageDb.Obj();
//                                                                                objBill.billId = billId;
//                                                                                objBill.tranId = context.tranId;
//                                                                                objBill.tranType = context.tranType;
//                                                                                objBill.serviceId = context.serviceId;
//                                                                                objBill.phoneNumber = "0" + msg.cmdPhone;
//                                                                                objBill.amount = context.amount;
//                                                                                saveBillInfoToCheckTimeToPay(objBill, log, viaConnectorObj.IsViaConnectorVerticle);
//
//                                                                            }
//                                                                            requestPromotionForPG(tranReply
//                                                                                    , _data
//                                                                                    , msg
//                                                                                    , fAmount
//                                                                                    , serviceId, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE
//                                                                                    , promo_girl_paybill);

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

                                                                                            if (context.error == 0) {
                                                                                                if (context.voucher != 0) {
                                                                                                    JsonObject jsonGift = new JsonObject();
                                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.NUMBER, "0" + msg.cmdPhone);
                                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.BILL_ID, billId);
                                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.SERVICE_ID, serviceId);
                                                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.GIFT_ID, context.giftId);
                                                                                                    insertPayOneHcPruBill(jsonGift);

                                                                                                }
                                                                                                //Kiem tra ket qua xoa trong queuedgift
                                                                                                if (results.getInteger("error", -1000) == 0) {
                                                                                                    //BEGIN 0000000052 Iron MAN
                                                                                                    JsonArray giftArray = result.getArray(StringConstUtil.IronManPromo.GIFT_ID_ARRAY, new JsonArray());

                                                                                                    updateIronManVoucher(msg, log, giftArray);
                                                                                                    promotionProcess.updateOctoberPromoVoucherStatus(log, giftArray, "0" + msg.cmdPhone);
                                                                                                    //END 0000000052 IRON MAN


                                                                                                }
//                                                                                                processPromotion(msg, proxyResponse.getRequest().getCoreTransId(), totalAmount, _data);
                                                                                                //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

                                                                                                //requestVisaPoint(msg, log, context, sock, _data);
                                                                                                //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                                                                //force to update current amount
                                                                                                mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);

                                                                                                //reset bill amount here
                                                                                                resetBillAmount(msg.cmdPhone, serviceId, billId);
                                                                                                //BEGIN 0000000004
                                                                                                log.add("index", msg.cmdIndex);
                                                                                                log.add("func", "Bat dau tra thuong billpay hehe");
                                                                                                log.add("tranId", context.tranId);
                                                                                                log.add("tranType", context.tranType);
                                                                                                log.add("phone", msg.cmdPhone);
                                                                                                log.add("serviceId", serviceId);
//                                                                                                BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, context.tranType,
//                                                                                                        context.tranId, serviceId, "", new Handler<JsonObject>() {
//                                                                                                            @Override
//                                                                                                            public void handle(JsonObject jsonObject) {
//                                                                                                                log.add("requestBillPayPromo", jsonObject);
//                                                                                                                log.writeLog();
//                                                                                                            }
//                                                                                                        }
//                                                                                                );
                                                                                                //END 0000000004


                                                                                            }

                                                                                            giftTranDb.save(new GiftTran(context), null);
                                                                                        }
                                                                                    });
                                                                                    //dgd
                                                                                    //BEGIN 0000000001 Send SMS to Customer.
//                                                            final ProxyResponse proxyResponseTmp = proxyResponse;
//                                                            if (isRetailer && rcode == 0) {
//                                                                sendSmsBillPayForCustomer(proxyResponseTmp, fAmount, context.tranId, serviceId, billId, cusPhone);
//                                                            }
                                                                                    //END 0000000001 Send SMS to Customer.
                                                                                    log.writeLog();
                                                                                }
                                                                            });
                                                                            //BEGIN 0000000050
                                                                            JsonObject jsonRep = new JsonObject();
                                                                            jsonRep.putNumber("error", rcode);
                                                                            jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                                            jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                                                            jsonRep.putString(StringConstUtil.SERVICE, serviceId);
                                                                            if (callback != null) {
                                                                                callback.handle(jsonRep);
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

                        final long ackTime = System.currentTimeMillis();
                        final MomoProto.TranHisV1 fTranHis = fRequest;

                        //
                        if ("capdoihoanhao".equalsIgnoreCase(serviceId)) {

                            Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
                                @Override
                                public void handle(final CdhhConfig cdhhConfig) {
                                    boolean isOpened = true;
                                    if (cdhhConfig == null) {
                                        isOpened = false;
                                    }
                                    long time = System.currentTimeMillis();

                                    if ((cdhhConfig.endTime == null
                                            || cdhhConfig.startTime == null) && isOpened) {
                                        isOpened = false;
                                    }

                                    if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                                            || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                                            && isOpened) {
                                        isOpened = false;
                                    }

                                    if (isOpened == false) {
                                        final long tid = System.currentTimeMillis();
                                        //todo chua mo cua
                                        String content = "Hệ thống chưa mở, bình chọn của bạn không hợp lệ. Bạn có thể tiếp tục bình chọn cho cặp thí sinh yêu thích vào lúc 23h30 đêm nay - 21h của đêm liveshow sắp tới để có cơ hội trúng 5 triệu đồng từ Ví MoMo. Xin cảm ơn.";
                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.caption = "Hệ thống chưa mở";
                                        noti.body = content;
                                        noti.bodyIOS = content;
                                        noti.sms = "";
                                        noti.tranId = tid; // tran id khi ban theo danh sach
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        noti.priority = 2;
                                        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                        noti.time = System.currentTimeMillis();

                                        //ban notification
                                        Misc.sendNoti(vertx, noti);

                                        MomoMessage fmsg3 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                                                , MomoProto.TranHisV1.newBuilder()
                                                .setComment(content)
                                                .setPartnerCode(fTranHis.getPartnerCode())
                                                .setAmount(fTranHis.getAmount())
                                                .setSourceFrom(fTranHis.getSourceFrom())
                                                .setPartnerId(fTranHis.getPartnerId())
                                                .setPartnerCode(fTranHis.getPartnerCode())
                                                .setPartnerName("Cặp đôi hoàn hảo")
                                                .setBillId(fTranHis.getBillId())
                                                .setPartnerRef(noti.caption)
                                                .setClientTime(fTranHis.getClientTime())
                                                .setTranType(TranTypeExt.CapDoiHoanHao)
                                                .setIo(fTranHis.getIo())
                                                .setCategory(fTranHis.getCategory())
                                                .setPartnerExtra1(fTranHis.getPartnerExtra1())
                                                .build().toByteArray()
                                        );

                                        JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, tid, 0, -1);
                                        Misc.addErrDescAndComment(tranErr, noti.caption, content);
                                        mCom.sendTransReply(vertx, tranErr, ackTime, fmsg3, sock, _data, callback);

                                        forceTranCDHHFailed(tid, fmsg3, fTranHis, ackTime, 0, _data, content, sock);
                                        return;

                                    }

                                    //todo lay so cuoi cung trong chuoi
                                    final String trimBill = billId.trim();
                                    final String code = "".equalsIgnoreCase(trimBill) ? "" : trimBill.substring(trimBill.length() - 1, trimBill.length());
                                    final ArrayList<Misc.KeyValue> valueArrayList = new ArrayList<>();
                                    valueArrayList.add(new Misc.KeyValue("cdhh", "cap_doi_hoan_hao"));

                                    final long willVoteAmt = (fAmount / cdhh_min_val);
                                    final String coupleName = hashMapCoupleCDHH.get(code);
                                    final String sttCap = "0" + DataUtil.strToInt(code);
                                    final String soLuong = "" + willVoteAmt;
                                    final String fServiceName = sttCap + MomoMessage.BELL + coupleName + MomoMessage.BELL + soLuong;
                                    final long curTime = System.currentTimeMillis();

                                    //todo sai so bao danh
                                    if (DataUtil.strToInt(code) > cdhh_max_code || DataUtil.strToInt(code) < cdhh_min_code) {
                                        //todo tra ve loi o day
                                        String content = "SBD bạn bình chọn không hợp lệ, vui lòng nhập lại. Bình chọn bằng Ví MoMo để có cơ hội trúng giải thưởng 10 triệu cho đêm liveshow, 5 triệu cho tuần. LH: 0839917199";
                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.caption = "Sai SBD bình chọn";
                                        noti.body = content;
                                        noti.bodyIOS = content;
                                        noti.sms = "";
                                        noti.tranId = curTime; // tran id khi ban theo danh sach
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        noti.priority = 2;
                                        noti.status = Notification.STATUS_DISPLAY; // cho phep hien thi khi sync du lieu
                                        noti.time = System.currentTimeMillis();

                                        //khong ban default noti --> cho dong ban ben ngoai
                                        Misc.sendNoti(vertx, noti);

                                        MomoMessage fmsg1 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                                                , MomoProto.TranHisV1.newBuilder()
                                                .setComment(content)
                                                .setPartnerCode(fTranHis.getPartnerCode())
                                                .setAmount(0)
                                                .setSourceFrom(fTranHis.getSourceFrom())
                                                .setPartnerId(fTranHis.getPartnerId())
                                                .setPartnerCode(fTranHis.getPartnerCode())
                                                .setPartnerName("Cặp đôi hoàn hảo")
                                                .setBillId(fServiceName)
                                                .setPartnerRef(noti.caption)
                                                .setClientTime(fTranHis.getClientTime())
                                                .setTranType(TranTypeExt.CapDoiHoanHao)
                                                .setIo(fTranHis.getIo())
                                                .setCategory(fTranHis.getCategory())
                                                .setPartnerExtra1(fTranHis.getPartnerExtra1())
                                                .build().toByteArray()
                                        );

                                        //todo tra ket qua loi ve
                                        JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, curTime, 0, -1);
                                        Misc.addErrDescAndComment(tranErr, "Sai SBD bình chọn", content);
                                        mCom.sendTransReply(vertx, tranErr, ackTime, fmsg1, sock, _data, callback);

                                        forceTranCDHHFailed(curTime, fmsg1, fTranHis, ackTime, 0, _data, content, sock);

                                        saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.InvalidCode, CDHHErr.Error.InvalidCode, trimBill, 0);
                                        return;
                                    }

                                    if (fAmount > cdhh_max_val || fAmount < cdhh_min_val) {
                                        //todo tra ve loi o day

                                        String content = "Số lượng tin nhắn không hợp lệ, vui lòng nhập lại. Bình chọn bằng Ví MoMo để có cơ hội trúng giải thưởng 10 triệu cho đêm liveshow, 5 triệu cho tuần. LH: 0839917199";
                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.caption = "Sai SBD bình chọn";
                                        noti.body = content;
                                        noti.bodyIOS = content;
                                        noti.sms = "";
                                        noti.tranId = curTime; // tran id khi ban theo danh sach
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        noti.priority = 2;
                                        noti.status = Notification.STATUS_DISPLAY; // cho phep hien thi khi sync du lieu
                                        noti.time = System.currentTimeMillis();

                                        //ban noti ben ngoai
                                        Misc.sendNoti(vertx, noti);

                                        MomoMessage fmsg2 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                                                , MomoProto.TranHisV1.newBuilder()
                                                .setComment(content)
                                                .setPartnerCode(fTranHis.getPartnerCode())
                                                .setAmount(fTranHis.getAmount())
                                                .setSourceFrom(fTranHis.getSourceFrom())
                                                .setPartnerId(fTranHis.getPartnerId())
                                                .setPartnerCode(fTranHis.getPartnerCode())
                                                .setPartnerName("Cặp đôi hoàn hảo")
                                                .setBillId(fServiceName)
                                                .setPartnerRef(noti.caption)
                                                .setClientTime(fTranHis.getClientTime())
                                                .setTranType(TranTypeExt.CapDoiHoanHao)
                                                .setIo(fTranHis.getIo())
                                                .setCategory(fTranHis.getCategory())
                                                .setPartnerExtra1(fTranHis.getPartnerExtra1())
                                                .build().toByteArray()
                                        );

                                        //todo tra ket qua loi ve
                                        JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, curTime, fAmount, -1);
                                        Misc.addErrDescAndComment(tranErr, "Số lượng tin nhắn không hợp lệ.", content);
                                        mCom.sendTransReply(vertx, tranErr, ackTime, fmsg2, sock, _data, callback);

                                        forceTranCDHHFailed(curTime, fmsg2, fTranHis, ackTime, fAmount, _data, content, sock);

                                        saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.OverAmount, CDHHErr.Error.OverAmount, trimBill, (fAmount / cdhh_min_val));
                                        return;
                                    }

                                    cdhh.findByNumber(msg.cmdPhone, serviceId, new Handler<Integer>() {
                                        @Override
                                        public void handle(Integer count) {

                                            //todo qua so luong binh chon
                                            if (count + willVoteAmt > cdhh_max_sms) {

                                                String content = "Bạn đã bình chọn quá 30 tin nhắn hôm nay. Vui lòng quay lại hôm sau để bình chọn cho thí sinh mình yêu thích. Bình chọn bằng Ví MoMo để có cơ hội nhận được 10 triệu đồng đêm liveshow, 5 triệu đồng tuần.";
                                                Notification noti = new Notification();
                                                noti.receiverNumber = msg.cmdPhone;
                                                noti.caption = "Quá số lượng bình chọn";
                                                noti.body = content;
                                                noti.bodyIOS = content;
                                                noti.sms = "";
                                                noti.tranId = curTime; // tran id khi ban theo danh sach
                                                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                                noti.priority = 2;
                                                noti.status = Notification.STATUS_DISPLAY;
                                                noti.time = System.currentTimeMillis();
                                                //ban notification
                                                Misc.sendNoti(vertx, noti);

                                                MomoMessage fmsg4 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                                                        , MomoProto.TranHisV1.newBuilder()
                                                        .setComment(content)
                                                        .setPartnerCode(fTranHis.getPartnerCode())
                                                        .setAmount(fTranHis.getAmount())
                                                        .setSourceFrom(fTranHis.getSourceFrom())
                                                        .setPartnerId(fTranHis.getPartnerId())
                                                        .setPartnerCode(fTranHis.getPartnerCode())
                                                        .setPartnerName("Cặp đôi hoàn hảo")
                                                        .setBillId(fServiceName)
                                                        .setPartnerRef(noti.caption)
                                                        .setClientTime(fTranHis.getClientTime())
                                                        .setTranType(TranTypeExt.CapDoiHoanHao)
                                                        .setIo(fTranHis.getIo())
                                                        .setCategory(fTranHis.getCategory())
                                                        .setPartnerExtra1(fTranHis.getPartnerExtra1())
                                                        .build().toByteArray()
                                                );

                                                //todo tra ket qua loi ve

                                                JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, curTime, 0, -1);
                                                Misc.addErrDescAndComment(tranErr, "Bạn đã bình chọn quá 30 tin nhắn hôm nay.", content);
                                                mCom.sendTransReply(vertx, tranErr, ackTime, fmsg4, sock, _data, callback);

                                                forceTranCDHHFailed(curTime, fmsg4, fTranHis, ackTime, 0, _data, content, sock);

                                                saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.OverAmount, CDHHErr.Error.OverAmount, trimBill, (fAmount / cdhh_min_val));
                                                return;

                                            }

                                            String key_acc = cdhhPrefix + DataUtil.strToInt(code);
                                            String cdhh_account = hashMapAccountCDHH.get(key_acc);

                                            MomoMessage fmsg5 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                                                    , MomoProto.TranHisV1.newBuilder()
                                                    .setComment("Ví MoMo của bạn không đủ tiền để bình chọn. Vui lòng nạp tiền để bình chọn & có cơ hội nhận được giải thưởng 10 triệu đồng cho đêm liveshow, 5 triệu đồng cho tuần. Nạp tiền Ví MoMo tại các điểm giao dịch của MoMo, từ 23 ngân hàng, thẻ Visa/Master có liên kết hoặc “mượn từ bạn bè” có MoMo. LH: 0839917199")
                                                    .setPartnerCode(fTranHis.getPartnerCode())
                                                    .setAmount(fTranHis.getAmount())
                                                    .setSourceFrom(fTranHis.getSourceFrom())
                                                    .setPartnerId(fTranHis.getPartnerId())
                                                    .setPartnerCode(fTranHis.getPartnerCode())
                                                    .setPartnerName("Cặp đôi hoàn hảo")
                                                    .setBillId(fServiceName)
                                                    .setPartnerRef("Bình chọn thành công")
                                                    .setClientTime(fTranHis.getClientTime())
                                                    .setTranType(TranTypeExt.CapDoiHoanHao)
                                                    .setIo(fTranHis.getIo())
                                                    .setCategory(fTranHis.getCategory())
                                                    .setPartnerExtra1(fTranHis.getPartnerExtra1())
                                                    .build().toByteArray()
                                            );

                                            if (vote_via_core) {
                                                voteViaCoreConnector(cdhh_account, msg, fAmount, log, sock, _data, callback, ackTime, fmsg5, fTranHis, code, soLuong, cdhhConfig, serviceId);
                                            } else {
                                                voteViaSoapIn(cdhh_account, msg, fAmount, valueArrayList, log, sock, _data, callback, ackTime, fmsg5, fTranHis, code, soLuong, cdhhConfig, serviceId);
                                            }
                                        }
                                    });
                                }
                            });
                            return;
                        } //END CAP DOI HOAN HAO

                        //cac giao dich thuc hien 1 buoc qua core
                        getServiceFee(serviceId, fAmount, log, new Handler<Long>() {
                            @Override
                            public void handle(Long servicefee) {

                                final long totalAmount = fAmount;

                                TransferWithGiftContext.build(msg.cmdPhone
                                        , serviceId
                                        , billId
                                        , totalAmount
                                        , vertx
                                        , giftManager
                                        , _data
                                        , TRANWITHPOINT_MIN_POINT
                                        , TRANWITHPOINT_MIN_AMOUNT, logger, new Handler<TransferWithGiftContext>() {
                                    @Override
                                    public void handle(final TransferWithGiftContext context) {
                                        int voucherPointType = Misc.getVoucherPointType(context);

                                        log.add("voucher and point type", voucherPointType);
                                        final long useVoucher = context.voucher;
                                        //qua core 1 buoc
                                        if (voucherPointType != 0) {

                                            String specialAgent = Misc.getSpecialAgent(serviceId, billpay_cfg);
                                            log.add("special agent", specialAgent);

                                            if ("".equalsIgnoreCase(specialAgent)) {
                                                log.add("vanhanh", "check them tai sao khong lai duoc bill pay cua dich vu " + serviceId + " billpay_cfg");
                                                log.writeLog();

                                                JsonObject joTranReply = Misc.getJsonObjRpl(SoapError.SYSTEM_ERROR, 100, totalAmount, -1);
                                                Misc.addCustomNumber(joTranReply, cusnum);
                                                mCom.sendTransReply(vertx, joTranReply, ackTime, msg, sock, _data, callback);
                                                return;
                                            }

                                            String tBillId = billId;
                                            if ("".equalsIgnoreCase(billId)) {
                                                tBillId = "0" + msg.cmdPhone;
                                            }

                                            log.add("func", "tranWithPointAndVoucher");
                                            CoreCommon.tranWithPointAndVoucher(vertx
                                                    , log
                                                    , msg.cmdPhone
                                                    , _data.pin
                                                    , context.point
                                                    , context.voucher
                                                    , totalAmount
                                                    , Const.CoreProcessType.OneStep
                                                    , Const.CoreTranType.Billpay
                                                    , specialAgent
                                                    , channel
                                                    //,"0" + msg.cmdPhone
                                                    , tBillId
                                                    , voucherPointType
                                                    , DEFAULT_CORE_TIMEOUT, new Handler<Response>() {
                                                @Override
                                                public void handle(final Response requestObj) {

                                                    context.tranId = requestObj.Tid;
                                                    context.error = requestObj.Error;
                                                    context.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                                                    context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject result) {
                                                            if (result.getInteger("error", -1000) != 0) {
                                                                log.add("gift", "use gift error");
                                                                log.add("error", result.getInteger("error"));
                                                                log.add("desc", SoapError.getDesc(result.getInteger("error")));
                                                            }
                                                            if (context.error == 0) {
//                                                                log.add("func", "checkbillonday");
//                                                                BillRuleManageDb.Obj objBill = new BillRuleManageDb.Obj();
//                                                                objBill.billId = billId;
//                                                                objBill.tranId = context.tranId;
//                                                                objBill.tranType = context.tranType;
//                                                                objBill.serviceId = context.serviceId;
//                                                                objBill.phoneNumber = "0" + msg.cmdPhone;
//                                                                objBill.amount = context.amount;
//                                                                saveBillInfoToCheckTimeToPay(objBill, log, viaConnectorObj.IsViaConnectorVerticle);

                                                                //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

                                                                requestVisaPoint(msg, log, context, sock, _data);
                                                                //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                                //BEGIN 0000000001 Send SMS to Customer
                                                                if (isRetailer || isStoreApp) {
                                                                    sendSmsBillPayForCustomer(totalAmount, context.tranId, serviceId, billId, smsPhone);
                                                                }
                                                                //END 0000000001
                                                            }
                                                            //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
                                                            context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
                                                                @Override
                                                                public void handle(JsonObject result) {
                                                                    if (result.getInteger("error", -1000) != 0) {
                                                                        log.add("return momo", "error");
                                                                    }

                                                                    final JsonObject joTranReply = Misc.getJsonObjRpl(requestObj.Error, requestObj.Tid, totalAmount, -1);
                                                                    Misc.addCustomNumber(joTranReply, cusnum);

                                                                    mCom.sendTransReply(vertx, joTranReply, ackTime, msg, sock, _data, callback);

                                                                    //reset bill amount here
                                                                    if (context.error == 0) {
//                                                                if (context.voucher == 0) {
//                                                                    JsonObject jsonGift = new JsonObject();
//                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.NUMBER, "0" + msg.cmdPhone);
//                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.BILL_ID, billId);
//                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.SERVICE_ID, serviceId);
//                                                                    jsonGift.putString(colName.HC_PRU_VoucherManage.GIFT_ID, context.giftId);
//                                                                    insertPayOneHcPruBill(jsonGift);
//                                                                }
                                                                        //Kiem tra ket qua xoa trong queuedgift
                                                                        if(result.getInteger("error", -1000) == 0)
                                                                        {
                                                                            //BEGIN 0000000052 Iron MAN
                                                                            JsonArray giftArray = result.getArray(StringConstUtil.IronManPromo.GIFT_ID_ARRAY, new JsonArray());
                                                                            updateIronManVoucher(msg, log, giftArray);
                                                                            promotionProcess.updateOctoberPromoVoucherStatus(log, giftArray, "0" + msg.cmdPhone);
                                                                            //END 0000000052 IRON MAN
                                                                        }


                                                                        resetBillAmount(msg.cmdPhone, serviceId, billId);
                                                                        //BEGIN 0000000004
                                                                        log.add("index", msg.cmdIndex);
                                                                        BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, context.tranType,
                                                                                context.tranId, serviceId, "", new Handler<JsonObject>() {
                                                                                    @Override
                                                                                    public void handle(JsonObject jsonObject) {
                                                                                        log.add("requestBillPayPromo", jsonObject);
                                                                                        log.writeLog();
                                                                                    }
                                                                                }
                                                                        );
                                                                        //END 0000000004
                                                                    }


                                                                    //tra thuong cho promotion girl
//                                                                    requestPromotionForPG(joTranReply
//                                                                            , _data
//                                                                            , msg
//                                                                            , fAmount
//                                                                            , serviceId, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE
//                                                                            , promo_girl_paybill);

                                                                    giftTranDb.save(new GiftTran(context), null);

                                                                    log.writeLog();
                                                                }
                                                            }); //END Lay phan du cua GIFT bo vao tui MOMO
                                                        }
                                                    }); // END TRANFER GIFT WITH NEED
                                                    //BEGIN 0000000050
                                                    JsonObject jsonRep = new JsonObject();
                                                    jsonRep.putNumber("error", context.error);
                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                                    jsonRep.putString(StringConstUtil.SERVICE, serviceId);
                                                    if (callback != null) {
                                                        callback.handle(jsonRep);
                                                    }
                                                    //END 0000000050
                                                }
                                            }); //END TRANSWITH POINT AND VOUCHER
                                        } else {

                                            log.add("begin", "soapin verticle");
                                            //chay cac luong cu thanh toan qua soapin
                                            vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, payOneBill, new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> result) {

                                                    JsonObject joReply = result.body();

                                                    log.add("end", "soapin verticle");
                                                    log.writeLog();

                                                    Misc.addCustomNumber(joReply, cusnum);
                                                    mCom.sendTransReply(vertx
                                                            , joReply
                                                            , ackTime
                                                            , msg
                                                            , sock
                                                            , _data
                                                            , callback);

                                                    int error = joReply.getInteger(colName.TranDBCols.ERROR, -100);
                                                    long tid = joReply.getLong(colName.TranDBCols.TRAN_ID, 0);
                                                    //reset amount by provider and bill
                                                    if (error == 0) {
                                                        //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

//                                                        requestVisaPoint(msg, log, context, sock, _data);
                                                        //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                        resetBillAmount(msg.cmdPhone, serviceId, billId);
                                                        //BEGIN 0000000004
                                                        log.add("index", msg.cmdIndex);
                                                        BillPayPromoObj.requestBillPayPromo(vertx, "0" + msg.cmdPhone, context.tranType,
                                                                context.tranId, serviceId, "", new Handler<JsonObject>() {
                                                                    @Override
                                                                    public void handle(JsonObject jsonObject) {
                                                                        log.add("requestBillPayPromo", jsonObject);
                                                                        log.writeLog();
                                                                    }
                                                                }
                                                        );

//                                                        processPromotion(msg, tid, totalAmount, _data);
                                                        //END 0000000004

                                                        //BEGIN 0000000001 Send SMS to Customer
                                                        if (isRetailer || isStoreApp) {
                                                            sendSmsBillPayForCustomer(totalAmount, tid, serviceId, billId, smsPhone);
                                                        }
                                                        //END 0000000001

                                                    }

//                                                    requestPromotionForPG(joReply
//                                                            , _data
//                                                            , msg
//                                                            , fAmount
//                                                            , serviceId, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE
//                                                            , promo_girl_paybill);

                                                    //BEGIN 0000000050
                                                    JsonObject jsonRep = new JsonObject();
                                                    jsonRep.putNumber("error", error);
                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_ID, context.tranId);
                                                    jsonRep.putNumber(colName.TranDBCols.TRAN_TYPE, context.tranType);
                                                    jsonRep.putString(StringConstUtil.SERVICE, serviceId);
                                                    if (callback != null) {
                                                        callback.handle(jsonRep);
                                                    }
                                                    //END 0000000050
                                                }
                                            });
                                        }
                                    }
                                }); // END With voucher and point
                            } // end handler getServiceFee
                        }); //END getServiceFee
                        //cap nhat session time
                        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());

                    }
                }); // END getViaCoreService
            }
        }); //END BILLRULE
    }

//    private void processPromotion(MomoMessage msg, long tranId, long fAmount, SockData _data) {
//
//        promotionProcess.getUserInfoToCheckPromoProgram("", "0" + msg.cmdPhone,
//                null, tranId, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE,
//                fAmount, StringConstUtil.RollBack50Percent.ROLLBACK_PROMO, _data, new JsonObject());
//    }

    private void requestPromotionForPG(JsonObject joReply
            , SockData _data
            , final MomoMessage msg
            , final long fAmount
            , final String serviceId
            , final int tranType
            , final long payAmount) {

        int error = joReply.getInteger(colName.TranDBCols.ERROR, -1);
        final long tranId = joReply.getLong(colName.TranDBCols.TRAN_ID, 0);

        //tra thuong chuong trinh PG
        PhonesDb.Obj phoneOb = (_data == null || _data.getPhoneObj() == null) ? null : _data.getPhoneObj();
        final String pgCode = phoneOb == null ? "" : phoneOb.inviter;
        if (error == 0 && phoneOb != null
                && !"".equalsIgnoreCase(pgCode)
                && DataUtil.strToInt(pgCode) >= pgCodeMin
                && DataUtil.strToInt(pgCode) <= pgCodeMax
                ) {

            final PromoReqObj reqPromoRec = new PromoReqObj();
            reqPromoRec.COMMAND = PromoType.GET_PROMOTION_REC;
            reqPromoRec.PROMO_NAME = "promogirlgalaxy";
            Misc.requestPromoRecord(vertx, reqPromoRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    final PromotionDb.Obj resPromoRec = new PromotionDb.Obj(jsonObject);

                    if (resPromoRec == null || resPromoRec.DATE_FROM == 0 || resPromoRec.DATE_TO == 0) {
                        return;
                    }

                    long curTime = System.currentTimeMillis();
                    if (curTime < resPromoRec.DATE_FROM || resPromoRec.DATE_TO < curTime) {
                        return;
                    }

                    //khong chua dich vu duoc quy dinh tra thuong cho promotion girl
                    if ("".equalsIgnoreCase(resPromoRec.NOTI_COMMENT) || resPromoRec.NOTI_COMMENT.contains(serviceId)) {
                        return;
                    }

                    VcbCommon.requestPromoForPG(vertx
                            , msg.cmdPhone
                            , DataUtil.strToInt(pgCode)
                            , fAmount
                            , tranType
                            , tranId
                            , payAmount
                            , serviceId);

                }
            });
        }
    }

    private void saveEventInfo(final String code
            , int phoneNumber
            , final long amount
            , long tranid
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final String name
            , final String serviceId
            , final CdhhConfig cdhhConfig) {
        //todo save

        final CDHH.Obj obj = new CDHH.Obj();
        obj.code = code;
        obj.number = phoneNumber;
        obj.time = System.currentTimeMillis();
        obj.value = amount;
        obj.day_vn = Misc.dateVNFormat(System.currentTimeMillis());
        obj.voteAmount = amount / cdhh_min_val;
        obj.time_vn = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        obj.tranid = tranid;
        obj.name = name;
        obj.serviceid = serviceId;

        log.add("save json", obj.toJson().encodePrettily());
        cdhh.save(obj, cdhhConfig, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });

        cdhhSumary.increase("0" + DataUtil.strToInt(code + "")
                , (amount / cdhh_min_val), cdhhConfig, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });
    }

    private void voteViaSoapIn(final String cdhh_account
            , final MomoMessage msg
            , final long amount
            , final ArrayList<Misc.KeyValue> valueArrayList
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final NetSocket sock
            , final SockData _data
            , final Handler<JsonObject> callback
            , final long ackTime
            , final MomoMessage fmsg
            , final MomoProto.TranHisV1 fTranHis
            , final String code
            , final String soLuong, final CdhhConfig cdhhConfig, final String serviceId) {


        Misc.adjustment(vertx, "0" + msg.cmdPhone
                , cdhh_account
                , amount
                , WalletType.MOMO
                , valueArrayList, log, new Handler<com.mservice.momo.vertx.processor.Common.SoapObjReply>() {
            @Override
            public void handle(final com.mservice.momo.vertx.processor.Common.SoapObjReply soapObjReply) {
                //todo gia lap loi
                if (soapObjReply.error != 0) {

                    //todo khong du tien de binh chon
                    long curTime = System.currentTimeMillis();
                    String content = "Ví MoMo của bạn không đủ tiền để bình chọn. Vui lòng nạp tiền để bình chọn & có cơ hội nhận được giải thưởng 10 triệu đồng cho đêm liveshow, 5 triệu đồng cho tuần. Nạp tiền Ví MoMo tại các điểm giao dịch của MoMo, từ 23 ngân hàng, thẻ Visa/Master có liên kết hoặc “mượn từ bạn bè” có MoMo. LH: 0839917199";
                    Notification noti = new Notification();
                    noti.receiverNumber = msg.cmdPhone;
                    noti.caption = "Không đủ tiền bình chọn";
                    noti.body = content;
                    noti.bodyIOS = content;
                    noti.sms = "";
                    noti.tranId = curTime; // tran id khi ban theo danh sach
                    noti.type = MomoProto.NotificationType.NOTI_CASH_MONEY_VALUE;
                    noti.priority = 2;
                    noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                    noti.time = System.currentTimeMillis();
                    //ban notification
                    Misc.sendNoti(vertx, noti);

                    JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, curTime, 0, -1);
                    Misc.addErrDescAndComment(tranErr, "Ví MoMo của bạn không đủ tiền để bình chọn.", content);

                    MomoMessage fmsg6 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                            , MomoProto.TranHisV1.newBuilder()
                            .setComment(content)
                            .setPartnerCode(fTranHis.getPartnerCode())
                            .setAmount(0)
                            .setSourceFrom(fTranHis.getSourceFrom())
                            .setPartnerId(fTranHis.getPartnerId())
                            .setPartnerCode(fTranHis.getPartnerCode())
                            .setPartnerName("Cặp đôi hoàn hảo")
                            .setBillId(fTranHis.getBillId())
                            .setPartnerRef("Bình chọn không thành công")
                            .setClientTime(fTranHis.getClientTime())
                            .setTranType(TranTypeExt.CapDoiHoanHao)
                            .setIo(fTranHis.getIo())
                            .setCategory(fTranHis.getCategory())
                            .setPartnerExtra1(fTranHis.getPartnerExtra1())
                            .build().toByteArray()
                    );

                    mCom.sendTransReply(vertx, tranErr, ackTime, fmsg6, sock, _data, callback);

                    forceTranCDHHFailed(curTime, fmsg6, fTranHis, ackTime, 0, _data, content, sock);

                    saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.NotEnoughCash, CDHHErr.Error.NotEnoughCash, code, (amount / cdhh_min_val));

                } else {

                    //todo thanh cong
                    final String successVoteContent = "Bạn đã bình chọn thành công cho SBD0" + DataUtil.strToInt(code) + "  với số lượng: " + soLuong + " tin nhắn. Bình chọn bằng Ví MoMo bạn có cơ hội nhận được giải thưởng là 10 triệu cho đêm liveshow, 5 triệu cho tuần do Ví MoMo tặng. LH: 0839917199";
                    JsonObject tranReply = Misc.getJsonObjRpl(soapObjReply.error, soapObjReply.tranId, amount, -1);
                    Misc.addErrDescAndComment(tranReply, null, successVoteContent);

                    mCom.sendTransReply(vertx, tranReply, ackTime, fmsg, sock, _data, callback);

                    vertx.setTimer(300, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            //todo send tranout side

                            Notification noti = new Notification();
                            noti.receiverNumber = msg.cmdPhone;
                            noti.caption = "Bình chọn thành công";
                            noti.body = successVoteContent;
                            noti.bodyIOS = successVoteContent;
                            noti.sms = "";
                            noti.tranId = soapObjReply.tranId;
                            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                            noti.priority = 2;
                            noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                            noti.time = System.currentTimeMillis();
                            noti.cmdId = msg.cmdIndex;

                            //ban notification
                            Misc.sendNoti(vertx, noti);

                            final TranObj tran = new TranObj();
                            tran.owner_number = msg.cmdPhone;
                            tran.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                            tran.status = TranObj.STATUS_OK;
                            tran.io = -1;
                            tran.comment = successVoteContent;
                            tran.tranId = soapObjReply.tranId;
                            tran.cmdId = msg.cmdIndex;
                            tran.error = 0;
                            tran.billId = fTranHis.getPartnerId();
                            tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                            tran.clientTime = fTranHis.getClientTime();
                            tran.ackTime = ackTime;
                            tran.finishTime = System.currentTimeMillis();
                            tran.amount = amount;
                            tran.owner_name = ((_data != null && _data.getPhoneObj() != null) ? _data.getPhoneObj().name : "");
                            tran.category = 0;
                            tran.deleted = false;
                            tran.partnerId = fTranHis.getPartnerId();
                            tran.parterCode = "";
                            tran.partnerName = "Cặp đôi hoàn hảo";
                            tran.cmdId = msg.cmdIndex;
                            transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean result) {

                                    MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                                    builder.addTranList(MomoProto.TranHisV1.newBuilder()
                                                    .setTranId(tran.tranId)
                                                    .setClientTime(tran.clientTime)
                                                    .setAckTime(tran.ackTime)
                                                    .setFinishTime(tran.finishTime)
                                                    .setTranType(tran.tranType)
                                                    .setIo(tran.io)
                                                    .setCategory(tran.category)
                                                    .setPartnerId(tran.partnerId)
                                                    .setPartnerCode(tran.parterCode)
                                                    .setPartnerName(tran.partnerName)
                                                    .setPartnerRef(tran.partnerRef)
                                                    .setBillId(tran.billId)
                                                    .setAmount(tran.amount)
                                                    .setComment(tran.comment)
                                                    .setStatus(tran.status)
                                                    .setError(tran.error)
                                                    .setCommandInd(tran.cmdId)
                                    );

                                    Buffer buff = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.setResult(true)
                                                    .build()
                                                    .toByteArray()
                                    );

                                    mCom.writeDataToSocket(sock, buff);
                                }
                            });

                        }
                    });

                    //todo save du lieu
                    String name = (_data != null && _data.getPhoneObj() != null ? _data.getPhoneObj().name : "");
                    saveEventInfo(code, msg.cmdPhone, amount, soapObjReply.tranId, log, name, serviceId, cdhhConfig);

                    Misc.getPayBackCDHHSetting(vertx, serviceId, new Handler<CDHHPayBackSetting.Obj>() {
                        @Override
                        public void handle(final CDHHPayBackSetting.Obj pbObj) {

                            cdhhPayBack.getVotedAmount("0" + msg.cmdPhone, serviceId, new Handler<Integer>() {
                                @Override
                                public void handle(Integer votedAmout) {
                                    if (votedAmout < 3) {
                                        //ban noti hoan tien
                                        sendNotiHoanTienCDHH(msg.cmdPhone);
                                    }
                                }
                            });

                            if ((pbObj != null) && (pbObj.status == true)) {
                                requestCDHHPayBack(soapObjReply.tranId
                                        , msg.cmdPhone
                                        , amount
                                        , pbObj.paybackaccount
                                        , pbObj.delaytime
                                        , pbObj.paybackmax, serviceId);
                            }
                        }
                    });
                }
            }
        });
    }

    private void requestCDHHPayBack(long tranId
            , int number
            , long amount
            , String paybackaccount
            , int delaytime
            , int paybackmax, String serviceId) {
        JsonObject jsonPayBackReq = new JsonObject();
        jsonPayBackReq.putString("number", "0" + number);
        jsonPayBackReq.putNumber("amount", (amount / cdhh_min_val));
        jsonPayBackReq.putNumber("tranid", tranId);
        jsonPayBackReq.putString("pbacc", paybackaccount);
        jsonPayBackReq.putNumber("dtime", delaytime);
        jsonPayBackReq.putNumber("pbmax", paybackmax);
        jsonPayBackReq.putString("sid", serviceId);

        vertx.eventBus().send(AppConstant.PayBackCDHHVerticle_ADDRESS, jsonPayBackReq, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

            }
        });
    }

    private void voteViaCoreConnectorLoadTest(
            final String cdhh_account
            , final MomoMessage msg
            , final long amount
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final NetSocket sock
            , final SockData _data
            , final Handler<JsonObject> callback
            , final long ackTime
            , final MomoMessage fmsg
            , final MomoProto.TranHisV1 fTranHis
            , final String code
            , final String soLuong
            , final CdhhConfig cdhhConfig, final String serviceId) {

        Misc.getPayBackCDHHSetting(vertx, serviceId, new Handler<CDHHPayBackSetting.Obj>() {
            @Override
            public void handle(final CDHHPayBackSetting.Obj pbObj) {

                CoreCommon.voteVent(vertx, "0" + msg.cmdPhone, "", amount, cdhh_account, log, new Handler<Response>() {
                    @Override
                    public void handle(final Response requestObj) {

                        if (requestObj.Error != 0) {

                            //todo khong du tien de binh chon
                            String content = "Ví MoMo của bạn không đủ tiền để bình chọn. Vui lòng nạp tiền để bình chọn & có cơ hội nhận được giải thưởng 10 triệu đồng cho đêm liveshow, 5 triệu đồng cho tuần. Nạp tiền Ví MoMo tại các điểm giao dịch của MoMo, từ 23 ngân hàng, thẻ Visa/Master có liên kết hoặc “mượn từ bạn bè” có MoMo. LH: 0839917199";
                            Notification noti = new Notification();
                            noti.receiverNumber = msg.cmdPhone;
                            noti.caption = "Không đủ tiền bình chọn";
                            noti.body = content;
                            noti.bodyIOS = content;
                            noti.sms = "";
                            noti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                            noti.type = MomoProto.NotificationType.NOTI_CASH_MONEY_VALUE;
                            noti.priority = 2;
                            noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                            noti.time = System.currentTimeMillis();
                            //ban notification
                            Misc.sendNoti(vertx, noti);

                            long tranId = System.currentTimeMillis();
                            JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, tranId, 0, -1);
                            Misc.addErrDescAndComment(tranErr, "Ví MoMo của bạn không đủ tiền để bình chọn.", content);
                            MomoMessage fmsg6 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                                    , MomoProto.TranHisV1.newBuilder()
                                    .setComment(content)
                                    .setPartnerCode(fTranHis.getPartnerCode())
                                    .setAmount(0)
                                    .setSourceFrom(fTranHis.getSourceFrom())
                                    .setPartnerId(fTranHis.getPartnerId())
                                    .setPartnerCode(fTranHis.getPartnerCode())
                                    .setPartnerName("Cặp đôi hoàn hảo")
                                    .setBillId(fTranHis.getBillId())
                                    .setPartnerRef("Bình chọn không thành công")
                                    .setClientTime(fTranHis.getClientTime())
                                    .setTranType(TranTypeExt.CapDoiHoanHao)
                                    .setIo(fTranHis.getIo())
                                    .setCategory(fTranHis.getCategory())
                                    .setPartnerExtra1(fTranHis.getPartnerExtra1())
                                    .build().toByteArray()
                            );

                            mCom.sendTransReply(vertx, tranErr, ackTime, fmsg6, sock, _data, callback);

                            forceTranCDHHFailed(tranId, fmsg6, fTranHis, ackTime, 0, _data, content, sock);

                            saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.NotEnoughCash, CDHHErr.Error.NotEnoughCash, code, (amount / cdhh_min_val));

                        } else {

                            //todo thanh cong
                            final String successVoteContent = "Bạn đã bình chọn thành công cho SBD0" + DataUtil.strToInt(code) + "  với số lượng: " + soLuong + " tin nhắn. Bình chọn bằng Ví MoMo bạn có cơ hội nhận được giải thưởng là 10 triệu cho đêm liveshow, 5 triệu cho tuần do Ví MoMo tặng. LH: 0839917199";
                            final JsonObject tranReply = Misc.getJsonObjRpl(requestObj.Error, requestObj.Tid, amount, -1);
                            Misc.addErrDescAndComment(tranReply, "", successVoteContent);
                            mCom.sendTransReply(vertx, tranReply, ackTime, fmsg, sock, _data, callback);

                            vertx.setTimer(300, new Handler<Long>() {
                                @Override
                                public void handle(Long aLong) {
                                    //todo send tranout side
                                    String successVoteContent = "Bạn đã bình chọn thành công cho SBD0" + DataUtil.strToInt(code) + "  với số lượng: " + soLuong + " tin nhắn. Bình chọn bằng Ví MoMo bạn có cơ hội nhận được giải thưởng là 10 triệu cho đêm liveshow, 5 triệu cho tuần do Ví MoMo tặng. LH: 0839917199";

                                    Notification noti = new Notification();
                                    noti.receiverNumber = msg.cmdPhone;
                                    noti.caption = "Bình chọn thành công";
                                    noti.body = successVoteContent;
                                    noti.bodyIOS = successVoteContent;
                                    noti.sms = "";
                                    noti.tranId = requestObj.Tid;
                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                    noti.priority = 2;
                                    noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                    noti.time = System.currentTimeMillis();
                                    noti.cmdId = msg.cmdIndex;

                                    //ban notification
                                    Misc.sendNoti(vertx, noti);

                                    final TranObj tran = new TranObj();
                                    tran.owner_number = msg.cmdPhone;
                                    tran.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                                    tran.status = TranObj.STATUS_OK;
                                    tran.io = -1;
                                    tran.comment = successVoteContent;
                                    tran.tranId = requestObj.Tid;
                                    tran.cmdId = msg.cmdIndex;
                                    tran.error = 0;
                                    tran.billId = fTranHis.getPartnerId();
                                    tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                    tran.clientTime = fTranHis.getClientTime();
                                    tran.ackTime = ackTime;
                                    tran.finishTime = System.currentTimeMillis();
                                    tran.amount = amount;
                                    tran.owner_name = ((_data != null && _data.getPhoneObj() != null) ? _data.getPhoneObj().name : "");
                                    tran.category = 0;
                                    tran.deleted = false;
                                    tran.partnerId = fTranHis.getPartnerId();
                                    tran.parterCode = "";
                                    tran.partnerName = "Cặp đôi hoàn hảo";
                                    tran.cmdId = msg.cmdIndex;
                                    transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean result) {

                                            MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                                            builder.addTranList(MomoProto.TranHisV1.newBuilder()
                                                            .setTranId(tran.tranId)
                                                            .setClientTime(tran.clientTime)
                                                            .setAckTime(tran.ackTime)
                                                            .setFinishTime(tran.finishTime)
                                                            .setTranType(tran.tranType)
                                                            .setIo(tran.io)
                                                            .setCategory(tran.category)
                                                            .setPartnerId(tran.partnerId)
                                                            .setPartnerCode(tran.parterCode)
                                                            .setPartnerName(tran.partnerName)
                                                            .setPartnerRef(tran.partnerRef)
                                                            .setBillId(tran.billId)
                                                            .setAmount(tran.amount)
                                                            .setComment(tran.comment)
                                                            .setStatus(tran.status)
                                                            .setError(tran.error)
                                                            .setCommandInd(tran.cmdId)
                                            );

                                            Buffer buff = MomoMessage.buildBuffer(
                                                    MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                                                    msg.cmdIndex,
                                                    msg.cmdPhone,
                                                    builder.setResult(true)
                                                            .build()
                                                            .toByteArray()
                                            );

                                            mCom.writeDataToSocket(sock, buff);
                                        }
                                    });
                                }
                            });

                            String name = (_data != null && _data.getPhoneObj() != null ? _data.getPhoneObj().name : "");
                            saveEventInfo(code, msg.cmdPhone, amount, requestObj.Tid, log, name, serviceId, cdhhConfig);

                            if (pbObj.status == false) {
                                sendNotiHoanTienCDHH(msg.cmdPhone);
                            } else {
                                //hoan tien
                                requestCDHHPayBack(requestObj.Tid
                                        , msg.cmdPhone
                                        , amount
                                        , pbObj.paybackaccount
                                        , pbObj.delaytime
                                        , pbObj.paybackmax, serviceId);
                            }
                        }
                    }
                });
            }
        });
    }


    private void voteViaCoreConnector(
            final String cdhh_account
            , final MomoMessage msg
            , final long amount
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final NetSocket sock
            , final SockData _data
            , final Handler<JsonObject> callback
            , final long ackTime
            , final MomoMessage fmsg
            , final MomoProto.TranHisV1 fTranHis
            , final String code
            , final String soLuong
            , final CdhhConfig cdhhConfig, final String serviceId) {

        CoreCommon.voteVent(vertx, "0" + msg.cmdPhone, "", amount, cdhh_account, log, new Handler<Response>() {
            @Override
            public void handle(final Response requestObj) {

                if (requestObj.Error != 0) {

                    //todo khong du tien de binh chon
                    String content = "Ví MoMo của bạn không đủ tiền để bình chọn. Vui lòng nạp tiền để bình chọn & có cơ hội nhận được giải thưởng 10 triệu đồng cho đêm liveshow, 5 triệu đồng cho tuần. Nạp tiền Ví MoMo tại các điểm giao dịch của MoMo, từ 23 ngân hàng, thẻ Visa/Master có liên kết hoặc “mượn từ bạn bè” có MoMo. LH: 0839917199";
                    Notification noti = new Notification();
                    noti.receiverNumber = msg.cmdPhone;
                    noti.caption = "Không đủ tiền bình chọn";
                    noti.body = content;
                    noti.bodyIOS = content;
                    noti.sms = "";
                    noti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                    noti.type = MomoProto.NotificationType.NOTI_CASH_MONEY_VALUE;
                    noti.priority = 2;
                    noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                    noti.time = System.currentTimeMillis();
                    //ban notification
                    Misc.sendNoti(vertx, noti);

                    long tranId = System.currentTimeMillis();
                    JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, tranId, 0, -1);
                    Misc.addErrDescAndComment(tranErr, "Ví MoMo của bạn không đủ tiền để bình chọn.", content);

                    MomoMessage fmsg6 = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                            , MomoProto.TranHisV1.newBuilder()
                            .setComment(content)
                            .setPartnerCode(fTranHis.getPartnerCode())
                            .setAmount(0)
                            .setSourceFrom(fTranHis.getSourceFrom())
                            .setPartnerId(fTranHis.getPartnerId())
                            .setPartnerCode(fTranHis.getPartnerCode())
                            .setPartnerName("Cặp đôi hoàn hảo")
                            .setBillId(fTranHis.getBillId())
                            .setPartnerRef("Bình chọn không thành công")
                            .setClientTime(fTranHis.getClientTime())
                            .setTranType(TranTypeExt.CapDoiHoanHao)
                            .setIo(fTranHis.getIo())
                            .setCategory(fTranHis.getCategory())
                            .setPartnerExtra1(fTranHis.getPartnerExtra1())
                            .build().toByteArray()
                    );

                    mCom.sendTransReply(vertx, tranErr, ackTime, fmsg6, sock, _data, callback);

                    forceTranCDHHFailed(tranId, fmsg6, fTranHis, ackTime, 0, _data, content, sock);

                    saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.NotEnoughCash, CDHHErr.Error.NotEnoughCash, code, (amount / cdhh_min_val));

                } else {

                    //todo thanh cong
                    final String successVoteContent = "Bạn đã bình chọn thành công cho SBD0" + DataUtil.strToInt(code) + "  với số lượng: " + soLuong + " tin nhắn. Bình chọn bằng Ví MoMo bạn có cơ hội nhận được giải thưởng là 10 triệu cho đêm liveshow, 5 triệu cho tuần do Ví MoMo tặng. LH: 0839917199";
                    final JsonObject tranReply = Misc.getJsonObjRpl(requestObj.Error, requestObj.Tid, amount, -1);
                    Misc.addErrDescAndComment(tranReply, "", successVoteContent);
                    mCom.sendTransReply(vertx, tranReply, ackTime, fmsg, sock, _data, callback);

                    vertx.setTimer(300, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            //todo send tranout side
                            String successVoteContent = "Bạn đã bình chọn thành công cho SBD0" + DataUtil.strToInt(code) + "  với số lượng: " + soLuong + " tin nhắn. Bình chọn bằng Ví MoMo bạn có cơ hội nhận được giải thưởng là 10 triệu cho đêm liveshow, 5 triệu cho tuần do Ví MoMo tặng. LH: 0839917199";

                            Notification noti = new Notification();
                            noti.receiverNumber = msg.cmdPhone;
                            noti.caption = "Bình chọn thành công";
                            noti.body = successVoteContent;
                            noti.bodyIOS = successVoteContent;
                            noti.sms = "";
                            noti.tranId = requestObj.Tid;
                            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                            noti.priority = 2;
                            noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                            noti.time = System.currentTimeMillis();
                            noti.cmdId = msg.cmdIndex;

                            //ban notification
                            Misc.sendNoti(vertx, noti);

                            final TranObj tran = new TranObj();
                            tran.owner_number = msg.cmdPhone;
                            tran.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                            tran.status = TranObj.STATUS_OK;
                            tran.io = -1;
                            tran.comment = successVoteContent;
                            tran.tranId = requestObj.Tid;
                            tran.cmdId = msg.cmdIndex;
                            tran.error = 0;
                            tran.billId = fTranHis.getPartnerId();
                            tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                            tran.clientTime = fTranHis.getClientTime();
                            tran.ackTime = ackTime;
                            tran.finishTime = System.currentTimeMillis();
                            tran.amount = amount;
                            tran.owner_name = ((_data != null && _data.getPhoneObj() != null) ? _data.getPhoneObj().name : "");
                            tran.category = 0;
                            tran.deleted = false;
                            tran.partnerId = fTranHis.getPartnerId();
                            tran.parterCode = "";
                            tran.partnerName = "Cặp đôi hoàn hảo";
                            tran.cmdId = msg.cmdIndex;
                            transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean result) {

                                    MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                                    builder.addTranList(MomoProto.TranHisV1.newBuilder()
                                                    .setTranId(tran.tranId)
                                                    .setClientTime(tran.clientTime)
                                                    .setAckTime(tran.ackTime)
                                                    .setFinishTime(tran.finishTime)
                                                    .setTranType(tran.tranType)
                                                    .setIo(tran.io)
                                                    .setCategory(tran.category)
                                                    .setPartnerId(tran.partnerId)
                                                    .setPartnerCode(tran.parterCode)
                                                    .setPartnerName(tran.partnerName)
                                                    .setPartnerRef(tran.partnerRef)
                                                    .setBillId(tran.billId)
                                                    .setAmount(tran.amount)
                                                    .setComment(tran.comment)
                                                    .setStatus(tran.status)
                                                    .setError(tran.error)
                                                    .setCommandInd(tran.cmdId)
                                    );

                                    Buffer buff = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.setResult(true)
                                                    .build()
                                                    .toByteArray()
                                    );

                                    mCom.writeDataToSocket(sock, buff);
                                }
                            });
                        }
                    });

                    String name = (_data != null && _data.getPhoneObj() != null ? _data.getPhoneObj().name : "");
                    saveEventInfo(code, msg.cmdPhone, amount, requestObj.Tid, log, name, serviceId, cdhhConfig);

                    Misc.getPayBackCDHHSetting(vertx, serviceId, new Handler<CDHHPayBackSetting.Obj>() {
                        @Override
                        public void handle(final CDHHPayBackSetting.Obj pbObj) {

                            cdhhPayBack.getVotedAmount("0" + msg.cmdPhone, serviceId, new Handler<Integer>() {
                                @Override
                                public void handle(Integer votedAmout) {
                                    if (votedAmout < 3) {
                                        //ban noti hoan tien
                                        sendNotiHoanTienCDHH(msg.cmdPhone);
                                    }
                                }
                            });

                            if ((pbObj != null) && (pbObj.status == true)) {
                                requestCDHHPayBack(requestObj.Tid
                                        , msg.cmdPhone
                                        , amount
                                        , pbObj.paybackaccount
                                        , pbObj.delaytime
                                        , pbObj.paybackmax, serviceId);
                            }
                        }
                    });
                }
            }
        });
    }

    private void sendNotiHoanTienCDHH(int rcvNumber) {
        String content = "Mỗi Ví MoMo sẽ được miễn phí 3 tin nhắn đầu tiên và sẽ được hoàn tiền trong vòng 24 giờ kể từ khi bình chọn. Cảm ơn bạn đã sử dụng Ví MoMo để bình chọn cho thí sinh yêu thích.";
        Notification noti = new Notification();
        noti.receiverNumber = rcvNumber;
        noti.caption = "Hoàn tiền sau 24 giờ";
        noti.body = content;
        noti.bodyIOS = content;
        noti.sms = "";
        noti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
        noti.type = MomoProto.NotificationType.NOTI_GENERIC_VALUE;
        noti.priority = 2;
        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
        noti.time = System.currentTimeMillis();
        //ban notification
        Misc.sendNoti(vertx, noti);
    }

    private void forceTranCDHHFailed(final long tranId
            , final MomoMessage msg
            , final MomoProto.TranHisV1 fTranHis
            , final long ackTime
            , final long amount
            , final SockData _data
            , final String content
            , final NetSocket sock) {
        vertx.setTimer(300, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {

                //todo send tranout side
                final TranObj tran = new TranObj();
                tran.owner_number = msg.cmdPhone;
                tran.tranType = fTranHis.getTranType();
                tran.status = TranObj.STATUS_FAIL;
                tran.io = -1;
                tran.comment = content;//
                tran.tranId = tranId;
                tran.cmdId = msg.cmdIndex;
                tran.error = 100;
                tran.billId = fTranHis.getPartnerId();
                tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                tran.clientTime = fTranHis.getClientTime();
                tran.ackTime = ackTime;
                tran.finishTime = System.currentTimeMillis();
                tran.amount = amount;
                tran.owner_name = ((_data != null && _data.getPhoneObj() != null) ? _data.getPhoneObj().name : "");
                tran.category = 0;
                tran.deleted = false;
                tran.partnerId = fTranHis.getPartnerId();
                tran.parterCode = "";
                tran.partnerName = "Cặp đôi hoàn hảo";
                tran.cmdId = msg.cmdIndex;
                transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {

                        MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                        builder.addTranList(MomoProto.TranHisV1.newBuilder()
                                        .setTranId(tran.tranId)
                                        .setClientTime(tran.clientTime)
                                        .setAckTime(tran.ackTime)
                                        .setFinishTime(tran.finishTime)
                                        .setTranType(tran.tranType)
                                        .setIo(tran.io)
                                        .setCategory(tran.category)
                                        .setPartnerId(tran.partnerId)
                                        .setPartnerCode(tran.parterCode)
                                        .setPartnerName(tran.partnerName)
                                        .setPartnerRef(tran.partnerRef)
                                        .setBillId(tran.billId)
                                        .setAmount(tran.amount)
                                        .setComment(tran.comment)
                                        .setStatus(tran.status)
                                        .setError(tran.error)
                                        .setCommandInd(tran.cmdId)
                        );

                        Buffer buff = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.setResult(true)
                                        .build()
                                        .toByteArray()
                        );

                        mCom.writeDataToSocket(sock, buff);
                    }
                });
            }
        });
    }

    private void processPaymentViaCore(final NetSocket sock, final MomoMessage msg, final SockData _data, final Handler<JsonObject> callback,
                                       final MomoProto.TranHisV1 request, final com.mservice.momo.vertx.processor.Common.BuildLog log, final String serviceId, final String billId, final String cusnum,
                                       final boolean isRetailer, ViaConnectorObj viaConnectorObj) {

        ProxyRequest proxyRequest_temp = ConnectorCommon.createPaymentRequest(
                "0" + msg.cmdPhone,
                _data.pin,
                request.getPartnerId(),
                viaConnectorObj.BillPay,
                request.getBillId(),
                request.getAmount(),
                0, 0, 1, "", "", "", "", "", 0, new JsonObject());
        if (isStoreApp) {
            proxyRequest_temp = ConnectorCommon.createPaymentBackEndAgencyRequest(
                    "0" + msg.cmdPhone,
                    _data.pin,
                    request.getPartnerId(),
                    viaConnectorObj.BillPay,
                    request.getBillId(),
                    request.getAmount(),
                    0, 0, 1, "", "", "", "", "", 0, new JsonObject());
        }

        final ProxyRequest proxyRequest = proxyRequest_temp;
        log.add("paymentRequest content", proxyRequest.toString());

        ConnectorCommon.requestPayment(vertx
                , proxyRequest
                , viaConnectorObj.BusName
                , log
                , glbCfg
                , new Handler<ProxyResponse>() {
            @Override
            public void handle(ProxyResponse proxyResponse) {

                final long amount = proxyRequest.getAmount();
                int rcode = proxyResponse.getProxyResponseCode();
                final long tranId = proxyResponse.getRequest().getCoreTransId();
                long ackTime = System.currentTimeMillis();

                JsonObject jsonReply = Misc.getJsonObjRpl(rcode, tranId, amount, 1);
                mCom.sendTransReply(vertx, jsonReply, ackTime, msg, sock, _data, callback);

                //todo tao json tran reply cho client + notification
                if (rcode == 0 && amount >= 50000) {
                    PromoReqObj promoReqObj = new PromoReqObj();
                    promoReqObj.COMMAND = PromoType.INVITE_FRIEND_GEN_CODE;
                    promoReqObj.CREATOR = "0" + msg.cmdPhone;
                    promoReqObj.TRAN_TYPE = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                    promoReqObj.TRAN_AMOUNT = amount;
                    log.add("function", "requestPromo");
                    log.add("request promo invitefriend", promoReqObj.toJsonObject());

                    Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {

                        }
                    });
                }

                //dgd
                if (isRetailer && rcode == 0) {
                    sendSmsBillPayForRetailer(amount, tranId, serviceId, billId, cusnum);
                }

                log.add("error", rcode);
                log.add("errorDesc", SoapError.getDesc(rcode));
                log.writeLog();
            }
        });
    }


    private void sendSmsBillPayForRetailer(final long amount, final long tranId, String serviceId, final String billId, final String cusnum) {
        //thuc hien check thong tin xem la hoa don hay dich vu
        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_TYPE;
        serviceReq.ServiceId = serviceId;

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray array) {
                ServiceDb.Obj obj = new ServiceDb.Obj((JsonObject) array.get(0));
                String smsFormat = "";
                String sms = "";
                if (StringConstUtil.SERVICE.equalsIgnoreCase(obj.serviceType)) {
                    smsFormat = NotificationUtils.getSmsFormatRetailer(Const.SmsKey.Service);
                    // Chuc mung quy khach da thanh toan dich vu ${billId} THANH CONG, so tien ${amount}, ma giao dich ${tranId}.Xin cam on.
                    sms = DataUtil.stringFormat(smsFormat)
                            .put(Const.SmsField.BillId, billId)
                            .put(Const.SmsField.Amount, Misc.formatAmount(amount).replace(",", "."))
                            .put(Const.SmsField.TranId, tranId)
                            .toString();
                } else {
                    smsFormat = NotificationUtils.getSmsFormatRetailer(Const.SmsKey.Invoice);
                    // Chuc mung quy khach da thanh toan hoa don ${billId} THANH CONG, so tien ${amount}, ma giao dich ${tranId}. Xin cam on.
                    sms = DataUtil.stringFormat(smsFormat)
                            .put(Const.SmsField.BillId, billId)
                            .put(Const.SmsField.Amount, Misc.formatAmount(amount).replace(",", "."))
                            .put(Const.SmsField.TranId, tranId)
                            .toString();
                }
                Misc.sendSms(vertx, DataUtil.strToInt(cusnum), sms);
            }
        });
    }

    private void payBackEscape(final MomoMessage msg, final NetSocket sock, final long tranAmount, final com.mservice.momo.vertx.processor.Common.BuildLog log, final SockData _data) {
        //khong giam gia
        if (tranAmount < escapThresHold) return;

        final long payBackAmt = (long) (tranAmount * escapPercent);
        log.add("payBackAmt", payBackAmt);

        Misc.adjustment(vertx
                , escapeAcc
                , "0" + msg.cmdPhone
                , payBackAmt
                , WalletType.MOMO
                , null
                , log, new Handler<com.mservice.momo.vertx.processor.Common.SoapObjReply>() {
            @Override
            public void handle(final com.mservice.momo.vertx.processor.Common.SoapObjReply soReply) {
                log.add("error", soReply.error);
                log.add("desc", SoapError.getDesc(soReply.error));
                if (soReply.error != 0) {
                    log.writeLog();
                    return;
                }

                //todo noti + tran
                final long curTime = System.currentTimeMillis();

                final TranObj tran = new TranObj();
                tran.owner_number = msg.cmdPhone;
                tran.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                tran.status = TranObj.STATUS_OK;
                tran.io = 1;
                tran.comment = "Quý khách đã nhận " + Misc.formatAmount(payBackAmt).replace(",", ".") + " đồng là tiền hoàn 15% vé Escape Halloween";
                tran.tranId = soReply.tranId;
                tran.cmdId = curTime;
                tran.error = 0;
                tran.billId = "-1";
                tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                tran.clientTime = curTime;
                tran.ackTime = curTime;
                tran.finishTime = curTime;
                tran.amount = payBackAmt;
                tran.owner_name = ((_data != null && _data.getPhoneObj() != null) ? _data.getPhoneObj().name : "");
                tran.category = 0;
                tran.deleted = false;
                tran.partnerId = "M_Service";
                tran.parterCode = "";
                tran.partnerName = "Escape Halloween";

                transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {

                        //neu khong phai la cap nhat --> tao moi

                        BroadcastHandler.sendOutSideTransSync(vertx, tran);

                        Notification noti = new Notification();
                        noti.receiverNumber = msg.cmdPhone;
                        noti.caption = "Hoàn 15% vé Escape Halloween"; // todo
                        noti.body = "Chúc mừng quý khách đã nhận " + Misc.formatAmount(payBackAmt).replace(",", ".") + " đồng là tiền hoàn 15% vé Escape Halloween. Cảm ơn quý khách đã sử dụng Ví MoMo. Chúc quý khách có một bữa tiệc thú vị.";
                        noti.bodyIOS = noti.body;
                        noti.priority = 1;
                        noti.tranId = soReply.tranId;
                        noti.time = curTime;
                        noti.cmdId = msg.cmdIndex;
                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                        noti.status = Notification.STATUS_DISPLAY;
                        noti.sms = "";

                        log.writeLog();

                        Misc.sendNoti(vertx, noti);

                        mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                    }
                });
            }
        });
    }

    public void processTopUpGame(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasAmount() || !request.hasPartnerCode() || !request.hasPartnerId()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, data, "processTopUpGame", logger, sock, callback)) {
            return;
        }

        int amount = Integer.valueOf(String.valueOf(request.getAmount()));
        final long tranAmount = amount;

        String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;

        final long ackTime = System.currentTimeMillis();

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processTopUpGame");


        final SoapProto.TopUpGame.Builder builder = SoapProto.TopUpGame.newBuilder();
        builder.setMpin(data.pin)
                .setProviderId(request.getPartnerId())
                .setChannel(channel)
                .setAmount(amount)
                .setGameAccount(request.getPartnerCode());

        if (willGetBonus(data)) {

            //tinh toan gia tri can lay
            //????
            ArrayList<Integer> listTranType = new ArrayList<>();
            listTranType.add(MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE);
            transDb.sumTranInCurrentMonth(msg.cmdPhone, listTranType, new Handler<Long>() {
                @Override
                public void handle(Long totalValue) {

                    //bonus for referal
                    long bonusValue = Misc.calBonusValue(totalValue
                            , tranAmount
                            , topup_game_max_value_per_month
                            , topup_game_percent
                            , topup_game_static_value
                            , log);

                    SoapProto.keyValuePair.Builder bonusBuilder = SoapProto.keyValuePair.newBuilder();
                    bonusBuilder.setKey(Const.BONUSFORREFERAL);
                    bonusBuilder.setValue(String.valueOf(bonusValue));

                    //referal
                    SoapProto.keyValuePair.Builder referalBuilder = SoapProto.keyValuePair.newBuilder();
                    referalBuilder.setKey(Const.REFERAL);
                    referalBuilder.setValue("0" + data.getPhoneObj().referenceNumber);

                    builder.addKeyValuePairs(bonusBuilder);
                    builder.addKeyValuePairs(referalBuilder);

                    log.writeLog();

                    vertx.eventBus().send(
                            AppConstant.SoapVerticle_ADDRESS,
                            MomoMessage.buildBuffer(
                                    SoapProto.MsgType.TOPUP_GAME_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    builder.build()
                                            .toByteArray()
                            ),
                            new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> result) {

                                    mCom.sendTransReply(vertx, result.body(), ackTime, msg, sock, data, callback);
                                }
                            }
                    );

                }
            });

        } else {
            //top up game binh thuong
            //send this to soap
            log.add("referal number", "not found");
            log.writeLog();
            vertx.eventBus().send(
                    AppConstant.SoapVerticle_ADDRESS,
                    MomoMessage.buildBuffer(
                            SoapProto.MsgType.TOPUP_GAME_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.build()
                                    .toByteArray()
                    ),
                    new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> result) {
                            mCom.sendTransReply(vertx, result.body(), ackTime, msg, sock, data, callback);

                        }
                    }
            );
        }

        //cap nhat session time
        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
    }

    public void processTransferMoneyToPlace(final NetSocket sock
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

        if (request == null || !request.hasPartnerId()
                || !request.hasAmount() || !request.hasComment()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg
                , _data
                , "processTransferMoneyToPlace"
                , logger
                , sock
                , callback)) {
            return;
        }

        String agent = "0" + String.valueOf(msg.cmdPhone);
        String mpin = _data.pin;

        String value = (request.getBillId() == null ? "" : request.getBillId());

        SoapProto.TransferMoney2Place.Builder builder = SoapProto.TransferMoney2Place.newBuilder();
        builder.setAgent(agent)
                .setMpin(mpin)
                .setPhone(request.getPartnerId())
                .setAmount(request.getAmount())
                .setNotice(request.getComment());


        if (value != null && !"".equalsIgnoreCase(value)) {
            SoapProto.keyValuePair.Builder kvp = SoapProto.keyValuePair.newBuilder();
            kvp.setKey("service_name");
            kvp.setValue(value);
            builder.addKvps(kvp);
        }

        //build buffer --> soap verticle
        Buffer transferMoney2Place = MomoMessage.buildBuffer(
                SoapProto.MsgType.TRANSFER_MONEY_TO_PLACE_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build().toByteArray());

        final long ackTime = System.currentTimeMillis();
        //final int rcvPhone = Integer.valueOf(request.getPartnerId());

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, transferMoney2Place, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {

                mCom.sendTransReply(vertx, result.body(), ackTime, msg, sock, _data, callback);
            }
        });
        //cap nhat session time
        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
    }

    private void saveM2Number(final NetSocket sock
            , final MomoMessage msg
            , final SockData _data
            , final Handler<JsonObject> webcallback) {

        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        /*phone	-->	partner_id
        amount	-->	amount
        */

        if (request == null || !request.hasPartnerId() || !request.hasAmount()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        //kiem tra thong tin cache data is not valid
        if (mCom.isSessionExpiredOneMore(msg, _data, "saveM2Number", logger, sock, null)) {
            return;
        }

        final MomoProto.TranHisV1 fRequest = request;

        //todo treo tien len tai khoan trung gian
        final int rcvNumber = Integer.parseInt(request.getPartnerId());
        final long amount = request.getAmount();
        final long ackTime = System.currentTimeMillis();

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "saveM2Number");
        log.add("receiver_num", "0" + rcvNumber);
        Request lockObj = new Request();

        lockObj.TYPE = Command.TRANSFER_WITH_LOCK;
        lockObj.SENDER_NUM = "0" + msg.cmdPhone;
        lockObj.SENDER_PIN = _data.pin;
        lockObj.TRAN_AMOUNT = amount;
        lockObj.RECVER_NUM = "0" + rcvNumber;
        lockObj.TARGET = m2nworkingaccount; // treo tien den tai khoan nay
        lockObj.WALLET = WalletType.MOMO;
        lockObj.KeyValueList = new ArrayList<>();
        lockObj.KeyValueList.add(new KeyValue("m2mtype", "m2n"));
        lockObj.KeyValueList.add(new KeyValue(Const.CoreVC.Recipient, "0" + rcvNumber));

        //lockObj.KeyValueList.add(Misc.getClientBackendKeyValue());
        //lockObj.KeyValueList.add(Misc.getIsSmsKeyValue());

        lockObj.PHONE_NUMBER = log.getPhoneNumber();
        lockObj.TIME = log.getTime();
        final JsonObject lockJo = lockObj.toJsonObject();

        log.add("request json transfer", lockJo);

        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, lockJo, new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                Buffer buf = message.body();

                final MomoMessage reply = MomoMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(reply.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                //lock tien khong thanh cong
                if (rpl != null && rpl.getErrorCode() != 0) {

                    log.add("errorCode", rpl.getErrorCode());
                    log.add("errDesc", SoapError.getDesc(rpl.getErrorCode()));

                    JsonObject tranRpl = Misc.getJsonObjRpl(TranObj.STATUS_FAIL, rpl.getTid(), amount, -1);
                    tranRpl.putNumber(colName.TranDBCols.STATUS, TranObj.STATUS_FAIL);
                    tranRpl.putNumber(colName.TranDBCols.ERROR, rpl.getErrorCode());

                    mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, null);
                    log.writeLog();
                    return;
                }

                final Core.StandardReply fRpl = rpl;

                CodeUtil codeUtil = new CodeUtil();
                final String short_link = codeUtil.getNextCode();

                //final String sms = String.format(sms0, String.format("%,d", amount), msg.cmdPhone, short_link);

                //luu xuong mongo db
                m2cOffline.insert(msg.cmdPhone
                        , rcvNumber
                        , _data.getPhoneObj().name
                        , _data.getPhoneObj().cardId
                        , amount
                        , ""
                        , fRpl.getTid()
                        , short_link
                        , 0
                        , new Handler<Integer>() {
                    @Override
                    public void handle(Integer result) {
                        final int rcode = result;

                        /*//send resutl to client
                        final JsonObject tranRpl = Misc.getJsonObjRpl(rcode, fRpl.getTid(), amount, -1);
                        tranRpl.putNumber(colName.TranDBCols.STATUS, (result == 0 ? TranObj.STATUS_PROCESS : TranObj.STATUS_FAIL));
                        tranRpl.putBoolean(colName.TranDBCols.IS_M2NUMBER, true);
                        tranRpl.putObject("kvp", new JsonObject()
                                        .putString("link", "http://app.momo.vn/" + short_link)
                                        .putString("msg", sms)
                        );*/

                        //note : luong cu gui sms thi gui ket qua ve cho client --> chuyen tu sms --> data notification
                        //mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, webcallback);

                        log.add("save to m2cOffline table result", result);

                        if (result == MongoBase.DUPLICATE_CODE_ERROR) {
                            vertx.setTimer(1000, new Handler<Long>() {
                                @Override
                                public void handle(Long aLong) {
                                    saveM2Number(sock
                                            , msg
                                            , _data
                                            , webcallback);
                                }
                            });
                            return;
                        } else if (result != 0) {

                            log.add("rollback tranid", fRpl.getTid());

                            //send resutl to client
                            final JsonObject tranRpl = Misc.getJsonObjRpl(rcode, fRpl.getTid(), amount, -1);
                            tranRpl.putNumber(colName.TranDBCols.STATUS, (result == 0 ? TranObj.STATUS_PROCESS : TranObj.STATUS_FAIL));
                            tranRpl.putBoolean(colName.TranDBCols.IS_M2NUMBER, true);
                            tranRpl.putObject("kvp", new JsonObject()
                                            .putString("link", "http://app.momo.vn/" + short_link)
                                            .putString("cskh", "")
                                            .putString("msg", String.format(Data.share, Misc.formatAmount(amount), short_link))
                            );

                            //gui ket qua ve binh thuong nhu 1 giao dich loi
                            mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, webcallback);

                            Request rbObj = new Request();
                            rbObj.TYPE = Command.ROLLBACK;
                            rbObj.TRAN_ID = fRpl.getTid();
                            rbObj.PHONE_NUMBER = log.getPhoneNumber();
                            rbObj.TIME = log.getTime();

                            log.add("request json rollback", rbObj.toJsonObject());

                            vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS
                                    , rbObj.toJsonObject()
                                    , new Handler<Message<Buffer>>() {
                                @Override
                                public void handle(Message<Buffer> message) {

                                    Buffer buf = message.body();

                                    final MomoMessage reply = MomoMessage.fromBuffer(buf);
                                    Core.StandardReply rpl;
                                    try {
                                        rpl = Core.StandardReply.parseFrom(reply.cmdBody);
                                    } catch (Exception ex) {
                                        rpl = null;
                                    }

                                    log.add("rollback result", (rpl == null ? "rpl == null" : rpl.getErrorCode()));
                                    log.add("rollback result tid", (rpl == null ? "rpl == null" : rpl.getTid()));

                                    if (rpl != null && (rpl.getErrorCode() == 0 || rpl.getErrorCode() == 400 || rpl.getErrorCode() == 103)) {

                                        log.add("update m2cOffline with status", colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.DELETED));

                                        m2cOffline.updateAndGetObjectByTranId(fRpl.getTid()
                                                , colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.DELETED)
                                                , "backend"
                                                , new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean result) {
                                                //cap nhat rollback tren bang offline thanh cong
                                                if (result) {
                                                    log.add("update transDb with status", "cancelled");
                                                    transDb.updateTranStatusNew(msg.cmdPhone
                                                            , fRpl.getTid()
                                                            , TranObj.CANCELLED, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean tranObj) {

                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }

                                    log.writeLog();
                                }
                            });

                        } else {
                            //build sms and send to receiver number
                            //Ban nhan duoc %sd tu 0%d. Truy cap http://app.momo.vn/%s de nhan tien trong 48 gio hoac goi 0839917199 de duoc ho tro.

                            //log.add("send sms", sms);

                            /*//String sms = "Chuc mung ban da nhan duoc " + String.format("%,d", amount) + " tu " + _data.getPhoneObj().name + "(" + momoTranHisMsg.cmdPhone + "). Truy cap http://app.momo.vn/" + short_link + " de nhan tien trong vong 48h. Xin cam on";
                            SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                                    .setSmsId(0)
                                    .setContent(sms)
                                    .setToNumber(rcvNumber)
                                    .build();

                            vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());*/

                            //1. gui 1 tran ve client
                            String body = "Quí khách đã chuyển thành công số tiền "
                                    + Misc.formatAmount(amount).replaceAll(",", ".")
                                    + "đ cho số điện thoại 0" + rcvNumber;

                            JsonObject kvpJo = new JsonObject();

                            kvpJo.putString("msg", String.format(Data.share, Misc.formatAmount(amount), short_link))
                                    .putString("cskh", "")
                                    .putString("link", "http://app.momo.vn/" + short_link);

                            Misc.buildTranHisAndSend(msg
                                    , amount
                                    , rcode
                                    , fRpl.getTid()
                                    , fRequest.getComment()
                                    , fRequest, transDb, mCom, kvpJo, "", sock);

                            //2. gui noti ve client

                            final Notification noti = new Notification();
                            noti.receiverNumber = msg.cmdPhone;
                            noti.caption = "Chuyển tiền thành công";
                            noti.body = body;
                            noti.bodyIOS = body;
                            noti.sms = "";
                            noti.tranId = fRpl.getTid(); // ban tren toan he thong
                            noti.status = Notification.STATUS_DETAIL;
                            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                            noti.priority = 2;
                            noti.time = System.currentTimeMillis();
                            noti.category = 0;
                            noti.extra = kvpJo.toString();

                            Misc.sendNoti(vertx, noti);
                            log.add("amount before sendCurrentAgentInfo ------->", amount);
                            mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);

                            log.writeLog();
                        }
                    }
                });

            }
        });
    }

    public void processGetPromo(final NetSocket sock, final MomoMessage msg, final Handler<Integer> webCallback) {

        MomoProto.TextValueMsg promoStudent = null;
        try {
            promoStudent = MomoProto.TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processGetPromotionStudent");

        if (promoStudent == null) {
            mCom.writeErrorToSocket(sock);
            log.writeLog();
            return;
        }

        HashMap<String, String> hashMap = null;

        if (promoStudent.getKeysCount() > 0) {
            hashMap = Misc.getKeyValuePairs(promoStudent.getKeysList());
        }

        String promoCode = "";
        if (hashMap != null
                && hashMap.size() > 0
                && hashMap.containsKey(Const.PROMO_CODE)) {
            promoCode = hashMap.get(Const.PROMO_CODE);
        }

        final MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();

        if ("".equalsIgnoreCase(promoCode)) {
            Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.GET_PROMO_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , builder.setResult(false)
                    .setRcode(-100)
                    .setDesc("Vui lòng nhập mã khuyến mãi")
                    .build().toByteArray());
            mCom.writeDataToSocket(sock, buffer);
            log.add("promotion code", "");
            log.writeLog();
            return;
        }

        //MOMOABCDBDE
        //cafe mien phi
        if (promoCode.length() == 11) {
            log.add("begin process for coffee shop", "");

            PromoReqObj cfPromoReqObj = new PromoReqObj();
            cfPromoReqObj.COMMAND = PromoType.PROMO_M2M_CLAIM_CODE;
            cfPromoReqObj.CREATOR = "0" + msg.cmdPhone;
            cfPromoReqObj.PROMO_CODE = promoCode;
            cfPromoReqObj.PROMO_NAME = "";

            log.add("command", cfPromoReqObj.COMMAND);
            log.add("creator", cfPromoReqObj.CREATOR);
            log.add("promo code", cfPromoReqObj.PROMO_CODE);

            Misc.requestPromoRecord(vertx, cfPromoReqObj, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    Promo.PromoResObj promoResObj = new Promo.PromoResObj(jsonObject);

                    log.add("execute promo result", "-----------");
                    log.add("result", promoResObj.RESULT);
                    log.add("error", promoResObj.ERROR);
                    log.add("desc", promoResObj.DESCRIPTION);

                    Buffer buf = MomoMessage.buildBuffer(MomoProto.MsgType.GET_PROMO_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , MomoProto.StandardReply.newBuilder()
                            .setRcode(promoResObj.ERROR)
                            .setResult(promoResObj.RESULT)
                            .setDesc(promoResObj.DESCRIPTION)
                            .build().toByteArray());
                    mCom.writeDataToSocket(sock, buf);
                    log.writeLog();
                }
            });

            return;
        }

        //gioi thieu ban be
        //check sum

        CodeUtil codeUtil = new CodeUtil(5, 6);
        if (!codeUtil.isCodeValid(promoCode, "MOMO")) {

            Buffer buffer = MomoMessage.buildBuffer(MomoProto.MsgType.GET_PROMO_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , builder.setResult(false)
                    .setRcode(-100)
                    .setDesc("Mã khuyến mãi không hợp lệ")
                    .build().toByteArray());
            mCom.writeDataToSocket(sock, buffer);
            log.add("check sum", "-------");
            log.add("ma khong hop le", promoCode);
            return;
        }

        //default la chuong trinh gioi thieu ban be
        String promoName = "InviteFriend";

        if (hashMap != null && hashMap.size() > 0 && hashMap.containsKey(Const.SERVICE_ID)) {
            promoName = hashMap.get(Const.SERVICE_ID);
        }

        final PromoReqObj promoReqObj = new PromoReqObj();
        promoReqObj.COMMAND = PromoType.DO_PROMO_BY_CODE;
        promoReqObj.CREATOR = "0" + msg.cmdPhone;
        promoReqObj.PROMO_CODE = promoCode;

        // todo app phai truyen xuong, chinh la service id cua dich vu la chuong trinh khuyen mai
        promoReqObj.PROMO_NAME = promoName;

        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                Promo.PromoResObj promoResObj = new Promo.PromoResObj(jsonObject);

                log.add("execute promo result", "-----------");
                log.add("result", promoResObj.RESULT);
                log.add("error", promoResObj.ERROR);
                log.add("desc", promoResObj.DESCRIPTION);

                Buffer buf = MomoMessage.buildBuffer(MomoProto.MsgType.GET_PROMO_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , MomoProto.StandardReply.newBuilder()
                        .setRcode(promoResObj.ERROR)
                        .setResult(promoResObj.RESULT)
                        .setDesc(promoResObj.DESCRIPTION)
                        .build().toByteArray());
                mCom.writeDataToSocket(sock, buf);
                log.writeLog();
            }
        });
    }

    private boolean willGetBonus(SockData data) {
        //return false;
        boolean result = false;
        if (data != null
                && data.getPhoneObj() != null
                && data.getPhoneObj().referenceNumber > 0
                && data.getPhoneObj().createdDate >= date_affected
                && (data.getPhoneObj().createdDate + day_duration * 24 * 60 * 60 * 1000L) >= System.currentTimeMillis()) {
            result = true;
        }
        return false;
    }

    private boolean getTopup2StepEnable(JsonObject joCfg) {
        if (joCfg == null) return false;
        return joCfg.getBoolean("enable", false);
    }

    private String getTopup2StepBusAddress(JsonObject joCfg) {
        if (joCfg == null) return "";
        return joCfg.getString("busaddress", "");
    }

//    public void processVisaMasterCashIn(final NetSocket sock
//            , final MomoMessage msg
//            , final SockData _data
//            , final boolean doNextStep
//            , final JsonObject paraInfo
//            , final Handler<FromSource.Obj> callback
//            , final Handler<JsonObject> webcallback) {
//
//        MomoProto.TranHisV1 request;
//
//        request = TranHisUtils.getRequest(msg, logger);
//        if (request == null || !request.hasAmount() || !request.hasPartnerCode()) {
//            mCom.writeErrorToSocket(sock);
//            return;
//        }
//
//        //kiem tra thong tin cache data is not valid
//        if (mCom.isSessionExpiredOneMore(msg, _data, "processVisaMasterCashIn", logger, sock, webcallback)) {
//            return;
//        }
//        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(request.getKvpList());
//        final String cardId = hashMap.containsKey(VMConst.cardId) ? hashMap.get(VMConst.cardId) : "";
//
//        //not used at this time --> will use later
//        final String partnerCode = hashMap.containsKey(VMConst.partnerCode) ? hashMap.get(VMConst.partnerCode) : "";
//        String channel = TranHisUtils.getChannel(sock);
//
//        int tranType = request.getTranType();
//        String emailDesc = "";
//        if(tranType == MomoProto.TranHisV1.TranType.M2M_VALUE){
//            emailDesc = "Chuyển tiền";
//        }else{
//            //giao dich gian tiep
//            if(doNextStep){
//                emailDesc = "Thanh toán hóa đơn";
//            }else{
//                //giao dich chuyen tien truc tiep
//                emailDesc = "Nạp tiền vào ví điện tử";
//            }
//        }
//
//        final long transAmount = paraInfo != null && paraInfo.containsField("amount")
//                ? paraInfo.getLong("amount")
//                : request.getAmount();
//
//        final Common.BuildLog log = new Common.BuildLog(logger);
//        log.setPhoneNumber("0" + msg.cmdPhone);
//        log.add("func", "processVisaMasterCashIn");
//        log.add("cmdInd", msg.cmdIndex);
//        log.add("channel", channel);
//        log.add("amount", transAmount);
//        log.add("cardID", cardId);
//
//        //tinh fee
//        int feeType = doNextStep ? VMFeeType.TRANSFER_FEE_TYPE : VMFeeType.DEPOSIT_FEE_TYPE;
//        log.add("fee type", feeType == VMFeeType.DEPOSIT_FEE_TYPE ? "DEPOSIT_FEE_TYPE" : "TRANSFER_FEE_TYPE");
//        final long ackTime = System.currentTimeMillis();
//
//        //kiem tra giao dich nap tien truc tiep
//        //gia tri giao dich toi thieu phai la minValue (50K) thi moi cho nap
//        long minValue = Math.max(sbsGetMinValue(feeType),0);
//        if(feeType == VMFeeType.DEPOSIT_FEE_TYPE && minValue > transAmount){
//            long tranId = System.currentTimeMillis();
//            JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE,tranId,transAmount,-1);
//
//            String errDesc ="Giá trị giao dịch tối thiểu phải là: " + Misc.formatAmount(minValue).replace(",",".") + "đ";
//            Misc.addErrDescAndComment(tranErr,errDesc,"Giao dịch không thành công. " + errDesc);
//            mCom.sendTransReply(vertx,tranErr,ackTime,msg,sock,_data,null);
//            log.writeLog();
//            return;
//        }
//
//        long actualAmount = Math.max(sbsGetMinValue(feeType),transAmount);
//        log.add("actual amount", actualAmount);
//
//        VMRequest.doVisaMasterCashIn(vertx
//                                    , sbsGetBusAddress()
//                                    , "0" + msg.cmdPhone
//                                    , cardId
//                                    , actualAmount
//                                    , feeType
//                                    , channel
//                                    , emailDesc
//                                    , log
//                                    , new Handler<VisaResponse>() {
//            @Override
//            public void handle(VisaResponse visaResponse) {
//
//                int error = visaResponse == null ? SoapError.SYSTEM_ERROR : visaResponse.getResultCode();
//                long tranId = visaResponse == null ? 0 : visaResponse.getVisaRequest().getCoreTransId();
//                JsonObject tranRpl = Misc.getJsonObjRpl(error, tranId, transAmount, 1);
//
//                log.add("error", visaResponse == null ? "-100" : visaResponse.getResultCode());
//                log.add("desc",visaResponse == null ? "Core timeout" : visaResponse.getDescription() == null ? "" : visaResponse.getDescription());
//                log.add("tranId: ", tranId);
//
//                //chuyen tien truc tiep tu visa-master ve vi momo
//                if (!doNextStep) {
//                    mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, webcallback);
//                    log.writeLog();
//                    return;
//                }
//
//                //chuyen tien ve --> thuc hien 1 giao dich dich khac
//                TranHisUtils.addMoreTransferInfoTranRpl(tranRpl,
//                        ackTime,
//                        MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE,
//                        StringConstUtil.IO_GET_MONEY_STATE,
//                        StringConstUtil.DEFAULT_CATEGORY,
//                        "Visa-Master",
//                        "Visa-Master",
//                        "Will-later",
//                        transAmount,
//                        MomoProto.TranHisV1.SourceFrom.MASTERCARD_VALUE);
//
//                //tao giao dich trung gian --> tra ve client
//                mCom.saveAndSendTempTran(vertx, msg, tranRpl, sock, _data);
//
//                //set result for callback
//                FromSource.Obj frmSrcObj = new FromSource.Obj();
//
//                //giao dich cash-in visa-master thanh cong
//                log.add("error",error);
//                log.add("visa-master desc", SoapError.getDesc(error));
//                if (error == 0) {
//                    frmSrcObj.Result = true;
//                } else {
//                    //giao dich cash-in visa-master khong thanh cong --> giao dich chinh --> khong thanh cong
//                    tranRpl.putNumber(colName.TranDBCols.IO, -1);
//                    mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, _data, webcallback);
//                    frmSrcObj.Result = false;
//                }
//
//                callback.handle(frmSrcObj);
//                log.writeLog();
//            }
//        });
//
//        //cap nhat session time
//        mCom.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
//    }

    //Do Something from Visa Master Card.
//    public void doSthFromVisaMaster(Vertx vertx, MomoMessage msg, NetSocket sock, SockData data,
//                                    Logger logger, final boolean nextStep, MomoProto.TranHisV1 request,
//                                    final Handler<JsonObject> webCallback) {
//
//
//        Common mCommon = new Common(vertx, logger);
//
//
//        final Common.BuildLog log = new Common.BuildLog(logger);
//        log.add("func", "doSthFromVisaMaster");
//
//
//        if (request == null || !request.hasPartnerRef() || !request.hasPartnerExtra1()
//                || !request.hasAmount() || !request.hasPartnerId()) {
//            mCommon.writeErrorToSocket(sock);
//            return;
//        }
//
//        String ticket_code = "";
//        String invoice_no = "";
//
//        if (request.getTranType() == MomoProto.TranHisV1.TranType.PHIM123_VALUE) {
//            ticket_code = getTicketCode(request);
//            invoice_no = request.getBillId().trim();
//        }
//
//        String serviceId = getServiceId(request.getTranType());
//        if (serviceId.equals("")) {
//            serviceId = getTmpServiceId(request);
//        }
//
//        log.add("TransId", request.getTranId());
//        log.add("TransType", request.getTranType());
//        log.add("cmdInd", msg.cmdIndex);
//        log.add("phone", msg.cmdPhone);
//        log.add("type", msg.cmdType);
//        log.add("invoice_no: ", invoice_no);
//        log.add("ticket_code: ", ticket_code);
//        log.add("serviceId: ", serviceId);
//
//        int transType = request.getTranType();
//
//        JsonObject jsonContainerObject = new JsonObject();
//
//        jsonContainerObject.putString("serviceId", serviceId);
//        jsonContainerObject.putString("invoice_no", invoice_no);
//        jsonContainerObject.putString("ticket_code", ticket_code);
//        jsonContainerObject.putNumber("transType", transType);
//
//
//        if (request.getTranType() == MomoProto.TranHisV1.TranType.M2M_VALUE) {
//            executeProcessVisaMasterWithoutPointAndVoucher(request, vertx, sock, data
//                    , msg, jsonContainerObject
//                    , nextStep, log, webCallback);
//        } else {
//            executeProcessVisaMasterWithPointAndVoucher(request, vertx, sock,
//                    data, msg, jsonContainerObject, logger
//                    , nextStep, webCallback);
//        }
//    }
//
//    public String getServiceId(int transType) {
//        String serviceId = "";
//
//        switch (transType) {
//            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
//                serviceId = StringConstUtil.TOP_UP;
//            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
//                serviceId = StringConstUtil.ONE_TWO_THREE_FILM;
//            default:
//                serviceId = "";
//        }
//
//        return serviceId;
//    }
//
//    public String getTmpServiceId(MomoProto.TranHisV1 request) {
//        String tmpServiceId = "";
//        HashMap<String, String> hashMap = Misc.getKeyValuePairs(request.getKvpList());
//        tmpServiceId = (request.getPartnerId() == null || request.getPartnerId().isEmpty() ? "" : request.getPartnerId());
//
//        tmpServiceId = "".equalsIgnoreCase(tmpServiceId) ?
//                (hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : tmpServiceId)
//                : tmpServiceId;
//        return tmpServiceId;
//    }
//
//    public String getTicketCode(MomoProto.TranHisV1 request) {
//        String ticket = "";
//        if (request.getSourceFrom() == MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE) {
//            String[] ar = request.getPartnerName().split(MomoMessage.BELL);
//            ticket = (ar.length > 0 ? ar[ar.length - 1] : "null");
//        } else {
//            ticket = (request.getPartnerCode() == null ? "null" : request.getPartnerCode());
//        }
//
//        return ticket;
//    }
//
//
//    private void executeProcessVisaMasterWithPointAndVoucher(MomoProto.TranHisV1 request, Vertx vertx,
//                                                             final NetSocket sock,
//                                                             final SockData data,
//                                                             final MomoMessage msg,
//                                                             final JsonObject jsonObject,
//                                                             Logger logger, final boolean nextstep,
//                                                             final Handler<JsonObject> webCallback) {
//        //tinh toan so luong voucher, point va tien momo su dung trong giao dich
//        final int transType = jsonObject.getInteger("transType");
//
//        final String serviceId = jsonObject.getString("serviceId");
//
//
//        final Common.BuildLog log = new Common.BuildLog(logger);
//        TransferWithGiftContext.build(msg.cmdPhone,
//                serviceId,
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
//                            executeProcess(transType, sock, msg, data, null, jsonObject, webCallback);
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
//                        processVisaMasterCashIn(sock, msg, data, nextstep, paraInfo, new Handler<FromSource.Obj>() {
//                            @Override
//                            public void handle(FromSource.Obj obj) {
//                                if (obj.Result) {
//                                    log.add("obj.Result", "success");
//                                    executeProcess(transType, sock, msg, data, obj, jsonObject, webCallback);
//                                    log.writeLog();
//                                }
//                            }
//                        }, webCallback);
//                    }
//                });
//    }
//
//    private void executeProcessVisaMasterWithoutPointAndVoucher(MomoProto.TranHisV1 request, Vertx vertx,
//                                                                final NetSocket sock,
//                                                                final SockData data,
//                                                                final MomoMessage msg,
//                                                                final JsonObject jsonObject,
//                                                                final boolean nextstep,
//                                                                final Common.BuildLog log,
//                                                                final Handler<JsonObject> webCallback
//    ) {
//        //long cashInAmt = Math.max(context.momo, vietCombankMin);
//        long cashInAmt = request.getAmount();
//        JsonObject paraInfo = new JsonObject();
//        paraInfo.putNumber("amount", cashInAmt);
//
//        final int transType = jsonObject.getInteger("transType");
//        this.processVisaMasterCashIn(sock, msg, data, nextstep, paraInfo, new Handler<FromSource.Obj>() {
//            @Override
//            public void handle(FromSource.Obj obj) {
//                if (obj.Result) {
//                    log.add("obj.Result", "success");
//                    executeProcess(transType, sock, msg, data, obj, jsonObject, webCallback);
//                    log.writeLog();
//                }
//            }
//        }, webCallback);
//    }
//
//    private void executeProcess(int transType, NetSocket sock, MomoMessage msg, SockData data,
//                                FromSource.Obj obj, JsonObject jsonContainerObject, final Handler<JsonObject> webCallback) {
//
//
//        switch (transType) {
//            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
//                processTopUp(sock, msg, data, webCallback);
//                break;
//            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
//                String invoice_no = jsonContainerObject.getString("invoice_no");
//                String ticket_code = jsonContainerObject.getString("ticket_code");
//                processPayment123Phim(sock
//                        , msg
//                        , data
//                        , invoice_no
//                        , ticket_code
//                        , webCallback);
//                break;
//            case MomoProto.TranHisV1.TranType.M2M_VALUE:
//                if (obj != null) {
//                    ArrayList<SoapProto.keyValuePair> keyValuePairs = Misc.addKeyValuePair(null, Const.SERVICE_NAME, obj.ServiceSource);
//                    processM2MTransfer(sock, msg, data, keyValuePairs, webCallback);
//                }
//                break;
//            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
//                processPayOneBill(sock, msg, data, webCallback);
//                break;
//            default:
//                break;
//        }
//    }

    /**
     * after paying successfull, we want to reset the total amount of (provider,billid)
     *
     * @param providerId the serviceId or provider id
     * @param billId     the bill that has been paied
     * @param number     the wallet we want reset the total amount of bill.
     */
    private void resetBillAmount(int number, String providerId, String billId) {
        billsDb.resetBillAmount(number, providerId, billId, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });
    }

    private String sbsGetBusAddress() {

        if (sbsCfg != null) {
            return sbsCfg.getString("proxyBusAddress", "cyberSourceVerticle");
        }
        return "cyberSourceVerticle";
    }

    private long sbsGetMinValue(int fee) {

        long minValue = 0;
        if (fee == VMFeeType.DEPOSIT_FEE_TYPE) {
            if (sbsCfg != null) {
                minValue = sbsCfg.getLong("min_value_direct", 50000);
            } else {
                minValue = 50000;
            }
        } else {
            if (sbsCfg != null) {
                minValue = sbsCfg.getLong("min_value_redirect", 10000);
            } else {
                minValue = 1000;
            }
        }
        return minValue;
    }

    //BEGIN 0000000001 Send SMS to Customer.
//    private void sendSmsBillPayForCustomer(final long amount, final long tranId, final String serviceId, final String billId, final String cusnum) {
//        //thuc hien check thong tin xem la hoa don hay dich vu
//        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
//        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_TYPE;
//        serviceReq.ServiceId = serviceId;
//        final Common.BuildLog log = new Common.BuildLog(logger);
//        log.add("func: ", "sendSmsBillPayForCustomer");
//        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
//            @Override
//            public void handle(JsonArray array) {
//                ServiceDb.Obj obj = new ServiceDb.Obj((JsonObject) array.get(0));
//                String smsFormat = "";
//                String sms = "";
//                //Get SMS and SMS Format
//                if(StringConstUtil.OPERATION_SMILE.equalsIgnoreCase(serviceId))
//                {
//                    String key = StringConstUtil.OPERATION_SMILE;
//                    log.add("key", key);
//                    smsFormat = NotificationUtils.getSmsFormat(key);
//                    //<sms>Ung ho %s thanh cong. So tien: %sd. TID: %s. Cam on quy khach da su dung dich vu MoMo.</sms>
//                    sms = String.format(smsFormat, obj.serviceName, amount + "", tranId + "");
//                }
//                else if(StringConstUtil.INVOICE.equalsIgnoreCase(obj.serviceType) && (StringConstUtil.VINAHCM.equalsIgnoreCase(serviceId)
//                        || StringConstUtil.MOBI.equalsIgnoreCase(serviceId)))
//                {
////                    <sms>Thue bao %s thanh toan thanh cong. So tien: %sd. TID: %s. Cam on quy khach da su dung dich vu MoMo.</sms>
//                    String key = StringConstUtil.INVOICE_MOBI;
//                    log.add("key", key);
//                    smsFormat = NotificationUtils.getSmsFormat(key);
//                    sms = String.format(smsFormat,cusnum, amount + "", tranId + "");
//                }
//                else if(StringConstUtil.INVOICE.equalsIgnoreCase(obj.serviceType))
//                {
////                    <sms>Hoa don %s thanh toan thanh cong, So tien: %sd. TID: %s. Cam on quy khach da su dung dich vu MoMo.</sms>
//                    String key = StringConstUtil.INVOICE;
//                    log.add("key", key);
//                    smsFormat = NotificationUtils.getSmsFormat(key);
//                    sms = String.format(smsFormat,billId, amount + "", tranId + "");
//                }
//                else if(StringConstUtil.SERVICE.equalsIgnoreCase(obj.serviceType) && StringConstUtil.SERVICE_TOPUP.equalsIgnoreCase(obj.cateId))
//                {
////                    <sms>Nap %s, TK:%s thanh cong, So tien: %sd. TID: %s. Cam on quy khach da su dung dich vu MoMo.</sms>
//                    String key = StringConstUtil.SERVICE_TOPUP;
//                    log.add("key", key);
//                    smsFormat = NotificationUtils.getSmsFormat(key);
//                    sms = String.format(smsFormat,obj.serviceName, "", amount + "", tranId + "");
//                }
//                else if(StringConstUtil.SERVICE.equalsIgnoreCase(obj.serviceType) && StringConstUtil.EVENT.equalsIgnoreCase(obj.cateId))
//                {
////                    <sms>Binh chon %s thanh cong. So tien: %sd. TID: %s. Cam on quy khach da su dung dich vu MoMo.</sms>
//                    String key = StringConstUtil.EVENT;
//                    log.add("key", key);
//                    smsFormat = NotificationUtils.getSmsFormat(key);
//                    sms = String.format(smsFormat,obj.serviceName, amount + "", tranId + "");
//                }
//                else
//                {
//                    sms = "Chuc mung quy khach da thanh toan dich vu thanh cong";
//                }
//
//                log.add("serviceId", obj.serviceID);
//                log.add("serviceType", obj.serviceType);
//                log.add("partnerCode", obj.partnerCode);
//                log.add("catId", obj.cateId);
//                log.add("cat name", obj.cateName);
//                log.add("sms format: ", smsFormat);
//                log.add("sms: ", sms);
//                log.writeLog();
//                Misc.sendSms(vertx, DataUtil.strToInt(cusnum), sms);
//
//            }
//        });
//    }
    private void sendSmsBillPayForCustomer(final long amount, final long tranId, final String serviceId, final String billId, final String cusnum) {
        //thuc hien check thong tin xem la hoa don hay dich vu
        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
        serviceReq.ServiceId = serviceId;
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func: ", "sendSmsBillPayForCustomer");
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray array) {

                String smsFormat = "";
                String sms = "";
                //Get SMS and SMS Format
                if (array.size() > 0) {
                    try {
                        ServiceDb.Obj obj = new ServiceDb.Obj((JsonObject) array.get(0));
                        String key = StringConstUtil.SERVICE;
                        log.add("key", key);
                        smsFormat = NotificationUtils.getSmsFormat(key);
                        //Quy khach thanh toan %sd cho %s thanh cong qua dich vu MoMo. HD: %s, TID: %s. Tim hieu them tai: https://momo.vn/.
                        sms = String.format(smsFormat, amount + "", Misc.removeAccent(obj.serviceName), billId + "", tranId);
                        log.add("serviceId", obj.serviceID);
                        log.add("serviceType", obj.serviceType);
                        log.add("partnerCode", obj.partnerCode);
                        log.add("catId", obj.cateId);
                        log.add("cat name", obj.cateName);
                        log.add("sms format: ", smsFormat);
                        log.add("sms: ", sms);
                    } catch (Exception ex) {
                        log.add("error", ex);
                        log.add("desc", "can not part data to ServiceDb.obj");
                        sms = "Chuc mung quy khach da thanh toan dich vu thanh cong";
                    }
                } else {
                    log.add("service id", "Khong co thong tin dich vu de gui SMS");
                    sms = "Chuc mung quy khach da thanh toan dich vu thanh cong";
                }
                log.writeLog();
                Misc.sendSms(vertx, DataUtil.strToInt(cusnum), sms);

            }
        });
    }
    //END 0000000001 Send SMS to Customer.


    //Request tra point visa

    public void requestVisaPoint(final MomoMessage msg, final Common.BuildLog log, final TransferWithGiftContext context, final NetSocket sock, final SockData data) {
//        log.add("fuc", "requestVisaPoint");
//        transDb.getTheTwoLastTranOfNumber(msg.cmdPhone, new Handler<JsonArray>() {
//            @Override
//            public void handle(JsonArray objects) {
//                if (objects == null) {
//                    log.add("error", 1000);
//                    log.add("desc", "Khong co giao dich gi tu truoc gio ca");
//                    return;
//                }
//                JsonObject kvpObject = null;
//                int cat = 0;
//                long tranIdVisa = 0;
//                long amount = 0;
//                String serviceId = "";
//                if (objects != null && objects.size() > 0) {
//                    for (Object tranObj : objects) {
//                        log.add("tranObj", tranObj);
//                        cat = ((JsonObject) tranObj).getInteger(colName.TranDBCols.CATEGORY, 0);
//                        JsonArray share = ((JsonObject) tranObj).getArray(colName.TranDBCols.SHARE, null);
////
//                        tranIdVisa = ((JsonObject) tranObj).getLong(colName.TranDBCols.TRAN_ID, 0);
//                        log.add("share", share);
//                        String cardNumberVisa = "";
//                        String tranType = "";
//                        String cardType = "";
//                        amount = ((JsonObject) tranObj).getLong(colName.TranDBCols.AMOUNT, 0);
//                        long cashinTime = ((JsonObject) tranObj).getLong(colName.TranDBCols.CLIENT_TIME, 0);
//
//                        int error = ((JsonObject) tranObj).getInteger(colName.TranDBCols.ERROR, -1);
//                        if (share != null && share.size() > 0) {
//                            cardNumberVisa = ((JsonObject) share.get(0)).getString("cardnumbervisa", "");
//                            tranType = ((JsonObject) share.get(0)).getString("tranType", "");
//                            cardType = ((JsonObject) share.get(0)).getString("cardtype", "");
//                            log.add("tranType", tranType);
//                            log.add("cardNum", cardNumberVisa);
//                            log.add("amount", amount);
//                            log.add("error", error);
//
//                        }
//
//                        kvpObject = ((JsonObject) tranObj).getObject(colName.TranDBCols.KVP, null);
//                        if (kvpObject != null) {
//                            serviceId = kvpObject.getString("serviceId", "");
//                        }
//
//                        if (cardType.equalsIgnoreCase("001") && cat == 8 && !tranType.equalsIgnoreCase("") && !tranType.equalsIgnoreCase("0") && !cardNumberVisa.equalsIgnoreCase("")
//                                && !tranType.equalsIgnoreCase("50") && error == 0) {
//
//                            log.add("cardNumberVisa", cardNumberVisa);
//                            log.writeLog();
//                            final String cardNum = cardNumberVisa;
//                            final long visaAmount = amount;
//                            VisaMpointPromoObj.requestVisaMpointPromo(vertx, "0" + msg.cmdPhone, context.tranType, context.tranId
//                                    , cardNumberVisa, amount, tranIdVisa, serviceId, context.amount, cashinTime, new Handler<JsonObject>() {
//                                @Override
//                                public void handle(JsonObject jsonObject) {
//                                    log.add("requestVisaMpointPromo", jsonObject);
//                                    if (jsonObject != null) {
//                                        JsonObject jsonRep = jsonObject;
//                                        int error = jsonRep.getInteger("error", -1);
//                                        String desc = jsonRep.getString("desc", "Loi");
//                                        if (error != 0) {
//                                            VisaMpointErrorDb.Obj vsErrorObj = new VisaMpointErrorDb.Obj();
//                                            vsErrorObj.cardnumber = cardNum;
//                                            vsErrorObj.number = "0" + msg.cmdPhone;
//                                            vsErrorObj.tranid = context.tranId;
//                                            vsErrorObj.trantype = context.tranType;
//                                            vsErrorObj.error = error;
//                                            vsErrorObj.desc_error = desc;
//                                            vsErrorObj.time = System.currentTimeMillis();
//                                            vsErrorObj.count = 1;
//                                            visaMpointErrorDb.insert(vsErrorObj, new Handler<Integer>() {
//                                                @Override
//                                                public void handle(Integer integer) {
//
//                                                }
//                                            });
//                                        } else {
//                                            long tranId = jsonRep.containsField(StringConstUtil.TRANDB_TRAN_ID) ? jsonRep.getLong(StringConstUtil.TRANDB_TRAN_ID) : 0;
//                                            log.add("tranId", tranId);
//                                            sendTranHisVisaPromo(tranId, msg, sock, calculateMpoint(visaAmount, 30000), msg.cmdPhone, mCom);
//                                        }
//                                    }
//                                    mCom.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);
//                                    log.writeLog();
//                                }
//                            });
//
//                            return;
//                        }
//                    }//end for
//                }
//
//                return;
//
//            }
//        });
    }

    public long calculateMpoint(long amount, long totalBalance) {

        long mpoint = 0;

        long mpoint_tmp = (long) (Math.ceil(10 * 0.01 * amount));

        if (mpoint_tmp > totalBalance) {
            mpoint = totalBalance;
        } else {
            mpoint = mpoint_tmp;
        }

        return mpoint;
    }

    public void sendTranHisVisaPromo(long tranId, MomoMessage msg, NetSocket sock, long amount, int number, Common common) {

        //Send tranhis
        final TranObj mainObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        mainObj.tranType = MomoProto.TranHisV1.TranType.PROMOTION_VALUE;
        mainObj.comment = "Bạn nhận được " + amount + "đ tiền khuyến mãi của dịch vụ thanh toán Visa.";
        mainObj.tranId = tranId;
        mainObj.clientTime = currentTime;
        mainObj.ackTime = currentTime;
        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = amount;
        mainObj.status = TranObj.STATUS_OK;
        mainObj.error = 0;
        mainObj.io = 1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.owner_number = number;
        mainObj.partnerName = "M_Service";
        mainObj.partnerId = "";
        mainObj.partnerRef = mainObj.comment;
        Misc.sendTranAsSyn(msg, sock, transDb, mainObj, common);
        transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                }
            }
        });
    }

    public void insertPayOneHcPruBill(JsonObject jsonGift) {

        String billerId = jsonGift.getString(colName.HC_PRU_VoucherManage.BILL_ID, "");
        jsonGift.putNumber(colName.HC_PRU_VoucherManage.TIME, System.currentTimeMillis());

        hcPruVoucherManagerDb.updatePartial(billerId, jsonGift, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aboolean) {

            }
        });
    }


//    private void saveBillInfoToCheckTimeToPay(final BillRuleManageDb.Obj obj, final Common.BuildLog log, final boolean isViaConnector) {
//        log.add("func", "saveBillInfoToCheckTimeToPay");
//        obj.startTime = System.currentTimeMillis();
//        log.add("BillInfo", obj.toJson());
//        Common.ServiceReq serviceReq = new Common.ServiceReq();
//        serviceReq.ServiceId = obj.serviceId;
//        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
//        log.add("ServiceId", obj.serviceId);
//        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
//            @Override
//            public void handle(final JsonArray objects) {
//
//                log.add("objects", objects);
//                if (objects != null && objects.size() > 0) {
//                    //Tim xem bill nay co luu trong catched khong, neu co va thanh toan thanh cong thi xoa cached.
////                    boolean hasCatchedBill = ((JsonObject) objects.get(0)).getBoolean(colName.ServiceCols.HAS_CACHED_BILL, false);
////                    if (hasCatchedBill) {
////                        if (isViaConnector) {
////                            CacheBillPayDb cacheBillPayDb = new CacheBillPayDb(vertx, logger);
////                            JsonObject joUpdate = new JsonObject();
////                            joUpdate.putNumber(colName.CacheBillPay.COUNT, 5);
////                            cacheBillPayDb.updatePartialWithOutInsert(obj.billId, joUpdate, new Handler<Boolean>() {
////                                @Override
////                                public void handle(Boolean aBoolean) {
////                                    log.add("isUpdate CacheBillPayDb", aBoolean);
////                                    log.writeLog();
////                                }
////                            });
////                            return;
////                        }
////
////                        CacheBillInfoViaCoreDb cacheBillInfoViaCoreDb = new CacheBillInfoViaCoreDb(vertx, logger);
////                        JsonObject joUpdate = new JsonObject();
////                        joUpdate.putNumber(colName.CacheBillInfoViaCore.COUNT, 5);
////                        cacheBillInfoViaCoreDb.updatePartial(obj.billId, joUpdate, new Handler<Boolean>() {
////                            @Override
////                            public void handle(Boolean aBoolean) {
////                                log.add("isUpdate CacheBillInfoViaCoreDb", aBoolean);
////                                log.writeLog();
////                            }
////                        });
////                        return;
////                    }
//
//                    boolean isCheckedTime = ((JsonObject) objects.get(0)).getBoolean(colName.ServiceCols.CHECK_TIME_ON_ONE_BILL, false);
//                    if (isCheckedTime) {
//                        Calendar calendar = Calendar.getInstance();
//                        calendar.set(Calendar.HOUR_OF_DAY, 0);
//                        calendar.set(Calendar.MINUTE, 0);
//                        calendar.set(Calendar.SECOND, 0);
//                        calendar.set(Calendar.MILLISECOND, 0);
//                        long endTime_current = calendar.getTimeInMillis();
//
//                        int timeToCheck = ((JsonObject) objects.get(0)).getInteger(colName.ServiceCols.PAY_TIME_ON_ONE_BILL, 0);
//                        long endTime = timeToCheck * 24 * 60 * 60 * 1000L;
//                        obj.endTime = endTime + endTime_current;
//                        billRuleManageDb.updatePartial(obj.billId, obj.toJson(), new Handler<Boolean>() {
//                            @Override
//                            public void handle(Boolean aBoolean) {
//                                log.add("isUpdate", aBoolean);
//                                log.writeLog();
//                            }
//                        });
//                    }
//                }
//            }
//        });
//
//    }




    //BEGIN 0000000052 IronMan
    public void requestIronManPromo(final MomoMessage msg, final Common.BuildLog log, final TransferWithGiftContext context) {

        //Kiem tra xem co thanh toan voucher 1 hay khong
        log.add("func", "requestIronManPromo");
        ironManPromoGiftDB.findOne("0" + msg.cmdPhone, new Handler<IronManPromoGiftDB.Obj>() {
            @Override
            public void handle(IronManPromoGiftDB.Obj obj) {
                if(obj != null)
                {
                    if(obj.has_voucher_group_1 && !obj.used_voucher_1) //Co voucher 1 va chua su dung voucher
                    {
                        giftDb.findOne(obj.gift_id_1, new Handler<Gift>() {
                            @Override
                            public void handle(Gift gift) {
                                log.add("data iron man gift", gift);
                                if(gift != null && gift.status == 6)
                                {
                                    log.add("status of gift", gift.status);
                                    log.add("desc", "da thanh toan v1, cap nhat db");
                                    JsonObject joUpdate = new JsonObject();
                                    joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_1, true);
                                    ironManPromoGiftDB.updatePartial("0" + msg.cmdPhone, joUpdate, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {
                                            log.add("desc", "da thanh toan v1, tra v3");
                                            IronManPromoObj.requestIronManPromo(vertx, "0" + msg.cmdPhone, context.tranType, context.tranId, "topup", StringConstUtil.IronManPromo.IRON_PROMO_2,
                                                    "", "", new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject jsonObject) {
                                                            log.writeLog();
                                                            return;
                                                        }
                                                    });
                                        }
                                    });
                                    log.writeLog();
                                    return;
                                }
                            }
                        });
                    }
                }
                else{
                    log.add("desc", "Khong co nam trong danh sach tra thuong ironMan");
                    log.writeLog();
                    return;
                }
            }
        });
    }


    private void updateIronManVoucher(final MomoMessage msg, final Common.BuildLog log,final JsonArray giftArray)
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
                    joUpdate.putBoolean(colName.IronManPromoGift.USED_VOUCHER_12, true);
                    ironManPromoGiftDB.updatePartial("0" + msg.cmdPhone, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    });
                }
            }
        });
    }
    //END 0000000052 IronMan

    private void getServiceFee(String serviceId, final long cashInAmount, final Common.BuildLog log, final Handler<Long> callback) {

        log.add("func", "getFee");
        log.add("serviceId", serviceId);
        log.add("cashInAmount", cashInAmount);


        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_FEE;
        serviceReq.ServiceId = serviceId;
        serviceReq.channel = 0;
        serviceReq.inoutCity = 0;
        serviceReq.tranType = 0;

        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ServiceFeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new ServiceFeeDb.Obj(message.body());
                }
                int static_fee;
                double dynamic_fee;

                if (obj == null) {
                    static_fee = 0;
                    dynamic_fee = 0.0;
                } else {
                    static_fee = obj.STATIC_FEE;
                    dynamic_fee = obj.DYNAMIC_FEE;
                }

                log.add("static_fee: ", static_fee);
                log.add("dynamic_fee: ", dynamic_fee);
                log.add("amount: ", cashInAmount);
                long fee = 0;
                if (cashInAmount != 0) {
                    fee = VMFeeType.calculateFeeMethod(static_fee, dynamic_fee, cashInAmount);
                }
                log.add("fee", fee);
                callback.handle(fee);
            }
        });
    }
}
