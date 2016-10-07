package com.mservice.momo.gateway.internal.db.oracle;

import com.mservice.momo.data.*;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.NotificationUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.*;
import oracle.jdbc.OracleTypes;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import javax.xml.bind.DatatypeConverter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 5/28/14.
 */
public class UMarketOracleVerticle extends Verticle {
    //command type
    public static final int GET_BALANCE = 1;
    public static final int BANK_OUT_MANUAL = 2;
    public static final int BANKNET = 3;
    public static final int GET_COUPON_ID = 4;
    public static final int GET_C2C_INFO = 5;
    public static final int CHECK_PRO_C2C_RULE = 6;
    public static final int GET_M2M_TYPE_AND_CAPSET = 7;
    public static final int CHECK_PRO_C2C_RECEIVE_RULE = 8;
    public static final int LOGIN_WITHOUT_CORE = 9;
    public static String ADDRESS = AppConstant.PREFIX + "UMarketOracleVerticle";
    private static String Driver;
    private static String Url;
    private static String Username;
    private static String Password;
    private static DBProcess dbProcess;
    private static List<String> specialListAgent;
    private static ArrayList<String> NotAllowTranType = new ArrayList<>();
    private static HashMap<String, String> bankMap;
    private static double LNG_MAX = 0;
    private static double LNG_MIN = 0;
    private static double LAT_MAX = 0;
    private static double LAT_MIN = 0;
    private static String sepDeg = "°";
    private static String sepMin = "\'";
    private static String sepSec = "\"";
    private static String sepComma = ",";
    private static String replaced = ";";
    private static long time_sync = 0;

    static {
        bankMap = new HashMap<>();
        bankMap.put("vietinbank.bank", "102");
        bankMap.put("vcb.bank", "12345");
        bankMap.put("vpb.bank", "103");
        bankMap.put("ocb.bank", "104");
        bankMap.put("acb.bank", "106");
        bankMap.put("sacom.bank", "109");
        bankMap.put("exim.bank", "107");
        bankMap.put("tpb.bank", "105");
    }

    static {
        specialListAgent = new ArrayList<>();
        specialListAgent.add("vcb.bank");
        specialListAgent.add("vpb.bank");
        specialListAgent.add("ocb.bank");
        specialListAgent.add("sacom.bank");
        specialListAgent.add("exim.bank");
        specialListAgent.add("bal123pay");
        specialListAgent.add("balsmartlink_vcb");
        specialListAgent.add("balbanknet_vcb");
        specialListAgent.add("vietinbank.bank");
        specialListAgent.add("tpb.bank");
    }

    static {

        //TODO : neu muon khong thuc hien khuyen mai cho dich vu nao do thi them vao day
        NotAllowTranType.add("adjustment");
        NotAllowTranType.add("bonus");
        NotAllowTranType.add("fee");

    }

    private final Object syncLock = new Object();
    private Logger logger;
    private SettingsDb mSettingsDb;
    private PromotionProcess promotionProcess;
    //    public static String ADDRESS ="LStandbyOracleVerticle";
    private ConnectDB mSyncPool;
    private PhonesDb mPhonesDb;
    private boolean allow_sync_data = false;
    private String bankoutmanual;
    private String bankoutmanualfee;
    //bo nhung giao dich co ben nhan tien nam trong day
    private ArrayList<String> ExcludeCREDITOR = new ArrayList<>();
    //bo nhung giao dich co ben nhan tien nam trong day
    private ArrayList<String> ExcludeDEBITOR = new ArrayList<>();
    private AgentsDb agentsDb;
    private AtomicBoolean mIsProcess = new AtomicBoolean(false);
    private boolean isSyncMMT = false;
    private Object lckMMT = new Object();
    private JsonObject glbCfg = new JsonObject();
    private PhonesDb phonesDb;
    private JsonObject joTurnOffNoti;
    private boolean allowScanSync;


    private static String getBankCode(String agent) {
        String code = bankMap.get(agent);
        if (code != null) {
            return code;
        } else {
            return agent;
        }
    }

    public void start() {
        logger = getContainer().logger();
        glbCfg = container.config();
        logger.info("deploy Uv1");
//        JsonObject umarket_database = globalCfg.getObject("umarket_database");
//        JsonObject umarket_database_cfg = new JsonObject();
//        umarket_database_cfg.putObject("umarket_database", umarket_database);

        JsonObject cfg = getContainer().config().getObject("umarket_database");
        Driver = cfg.getString("driver");
        Url = cfg.getString("url");
        Username = cfg.getString("username");
        Password = cfg.getString("password");
        dbProcess = new DBProcess(Driver, Url, Username, Password, ADDRESS, ADDRESS, logger);
        mSettingsDb = new SettingsDb(vertx.eventBus(), logger);
        EventBus eb = vertx.eventBus();


        agentsDb = new AgentsDb(vertx.eventBus(), logger);

        JsonObject map_cfg = glbCfg.getObject("map");

        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        allow_sync_data = glbCfg.getBoolean("allow_sync_data", false);
        allowScanSync = glbCfg.getBoolean(StringConstUtil.SCAN_SYNC, false);
        ExcludeCREDITOR.add(glbCfg.getObject("vng_123phim").getString("reciever_account", ""));
        ExcludeCREDITOR.add("mmoneytovoucher");
        ExcludeCREDITOR.add("vouchertommoney");

        ExcludeCREDITOR.add("c2cfeein"); // phi cua giao dich c2c

        ExcludeDEBITOR.add(glbCfg.getObject("pay123").getString("account", ""));
        ExcludeDEBITOR.add("mmoneytovoucher");
        ExcludeDEBITOR.add("vouchertommoney");

        bankoutmanual = glbCfg.getObject("bank_manual").getString("agent_adjust_manual", "");
        bankoutmanualfee = glbCfg.getObject("bank_manual").getString("agent_adjust_manual_fee", "");

        joTurnOffNoti = glbCfg.getObject(StringConstUtil.TurningOffNotification.JSON_OBJECT, new JsonObject());
        //==> must be start after MongoDB
        mPhonesDb = new PhonesDb(vertx.eventBus(), logger);
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);

        if (allow_sync_data) {
            mSyncPool = new ConnectDB(Driver
                    , Url
                    , Username
                    , Password
                    , "UMarketSyncPool"
                    , "UMarketSyncPool"
                    , logger);
            logger.info("create pool success Uv1");
        }

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {

                JsonObject json = message.body();
                int type = json.getInteger(fieldNames.TYPE);
                switch (type) {
                    case GET_C2C_INFO:
                        doGetC2CInfo(message);
                        break;
                    case GET_COUPON_ID:
                        doGetCouponId(message);
                        break;
                    case GET_BALANCE:
                        getBalance(message);
                        break;
                    case BANK_OUT_MANUAL:
                        doBankOutManual(message);
                        break;
                    case BANKNET:
                        doBanknetTrackInfo(message);
                        break;
                    case CHECK_PRO_C2C_RULE:
                        checkProC2cRule(message);
                        break;
                    case GET_M2M_TYPE_AND_CAPSET:
                        doM2mTypeAndCapset(message);
                        break;
                    case CHECK_PRO_C2C_RECEIVE_RULE:
                        checkProC2cReceiveRule(message);
                        break;
                    case LOGIN_WITHOUT_CORE:
                        login(message);
                    default:
                        logger.warn("UMarketOracleVerticle NOT SUPPORT COMMAND " + type);
                        break;
                }
            }
        };
        eb.registerLocalHandler(ADDRESS, myHandler);

        if (allow_sync_data) {
            logger.info("Sync data is started");

            //cho phep syc thong tin diem giao dich tren phone
            vertx.setPeriodic(15 * 60 * 1000, new Handler<Long>() {
                @Override
                public void handle(Long timer) {
                    checkSyncTrans(timer);
                }
            });
            //sync trans outside
            vertx.setPeriodic(30 * 1000L, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    synchronized (syncLock) {
                        try {
                            logger.info("begin sync");
                            syncTrans();
                        } catch (Exception e) {
                            logger.error("Sync tran fail", e);
                        }
                    }
                }
            });
        }
        else if(allowScanSync)
        {
            logger.info("Check Sync data is started");
            vertx.setPeriodic(1000L * 60 * 15, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    mSettingsDb.getLong("LAST_TRANS_SYNC_TIME", new Handler<Long>() {
                        @Override
                        public void handle(Long longSyncTrans) {
                            logger.info("TIME SYNC TRAN " + longSyncTrans);
                            logger.info("TIME CURRENT " + System.currentTimeMillis());
                            if(System.currentTimeMillis() - 1000L * 60 * 15 > longSyncTrans)
                            {
                                logger.info("NOT SYNC TRANSACTIONS");
                            }
                            else {
                                logger.info("SYNC TRANSACTIONS");
                            }
                        }
                    });
                }
            });
        }
        else {
            logger.info("Sync data is not started");
        }
    }

    private void doM2mTypeAndCapset(Message<JsonObject> message) {
        JsonObject json = message.body();
        String fromNumber = json.getString("fromNumber");
        String toNumber = json.getString("toNumber");
        Long amount = json.getLong("amount");

        Common.BuildLog log = new Common.BuildLog(logger, fromNumber);
        String m2mtype = "";

        boolean isCapsetOk = dbProcess.PRO_M2M_CAPSET(fromNumber, amount, log);
        log.add("isCapsetOk", isCapsetOk);
        log.add("end", "PRO_M2M_CAPSET");

        if (isCapsetOk) {
            log.add("begin", "PRO_M2M_TYPE");
//            m2mtype = dbProcess.PRO_M2M_TYPE(fromNumber, toNumber, log);
            AtomicInteger count = new AtomicInteger(0);
            m2mtype = callProM2MType(fromNumber, toNumber, log, count);
            log.add("from number", fromNumber);
            log.add("to number", toNumber);
            log.add("m2m type", m2mtype);
            log.add("end", "PRO_M2M_TYPE");
        }


        JsonObject jo = new JsonObject();

        jo.putBoolean("isCapsetOk", isCapsetOk);
        jo.putString("m2mtype", m2mtype);

        message.reply(jo);
        log.writeLog();
    }

    private String callProM2MType(String fromNumber, String toNumber, Common.BuildLog log, AtomicInteger count) {
        String m2mType = "";
        if (count.intValue() == 2) {
            m2mType = "".equalsIgnoreCase(m2mType) ? "u2u" : m2mType;
            return m2mType;
        } else {
            m2mType = dbProcess.PRO_M2M_TYPE(fromNumber, toNumber, log);
            if ("".equalsIgnoreCase(m2mType)) {
                count.incrementAndGet();
                callProM2MType(fromNumber, toNumber, log, count);
            }
            return m2mType;
        }

    }

    //BEGIN 0000000019: check pro c2c rule
    private void checkProC2cRule(Message<JsonObject> message) {
        JsonObject jsonMessage = message.body();
        Common.BuildLog log = new Common.BuildLog(logger);
        JsonObject jsonRequest = jsonMessage.getObject("request", null);
        JsonArray requestInfoArray = jsonMessage.getArray("array_request", null);
        JsonObject jsonReply = new JsonObject();
        log.add("func", "checkProC2cRule");
        log.add("requestInfo", requestInfoArray);
        log.writeLog();
        if (requestInfoArray != null) {
            //nguoi nhan
            final String rcvName = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverName, requestInfoArray));
            final String rcvCardId = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverCardId, requestInfoArray));
            final String rcvPhone = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverPhone, requestInfoArray));

            //nguoi gui
            final String sndName = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderName, requestInfoArray));
            final String sndCardId = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderCardId, requestInfoArray));
            final String sndPhone = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderPhone, requestInfoArray));
            final long amount = jsonMessage.getLong("amount", 0);

            //dai ly nhan tien
            final String retailerAddr = Misc.getInfoByKey(Const.C2C.retailerAddress, requestInfoArray);
            final String retailerMomoPhone = Misc.getInfoByKey(Const.C2C.retailerPhone, requestInfoArray);

            jsonReply = dbProcess.checkC2cRule(retailerMomoPhone, sndCardId, "voucherin", amount, log);
            message.reply(jsonReply);
            return;
        }
        jsonReply.putNumber("error", 1000);
        jsonReply.putString("desc", "requestInfo is null");
        message.reply(jsonReply);
        return;
    }

    //BEGIN 0000000019: check pro c2c rule
    private void checkProC2cReceiveRule(Message<JsonObject> message) {
        JsonObject jsonMessage = message.body();
        Common.BuildLog log = new Common.BuildLog(logger);
        JsonObject jsonRequest = jsonMessage.getObject("request", null);
        JsonArray requestInfoArray = jsonMessage.getArray("array_request", null);
        JsonObject jsonReply = new JsonObject();
        log.add("func", "checkProC2cReceiveRule");
        log.add("requestInfo", requestInfoArray);
        log.writeLog();
        if (requestInfoArray != null) {
            //nguoi nhan
            final String rcvName = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverName, requestInfoArray));
            final String rcvCardId = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverCardId, requestInfoArray));
            final String rcvPhone = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.receiverPhone, requestInfoArray));

            //nguoi gui
            final String sndName = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderName, requestInfoArray));
            final String sndCardId = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderCardId, requestInfoArray));
            final String sndPhone = Misc.removeAccent(Misc.getInfoByKey(Const.C2C.senderPhone, requestInfoArray));
            final long amount = jsonMessage.getLong("amount", 0);

            //dai ly nhan tien
            final String retailerAddr = Misc.getInfoByKey(Const.C2C.retailerAddress, requestInfoArray);
            final String retailerMomoPhone = Misc.getInfoByKey(Const.C2C.retailerPhone, requestInfoArray);

            jsonReply = dbProcess.checkC2cRule(retailerMomoPhone, rcvCardId, "voucherout", amount, log);
            message.reply(jsonReply);
            return;
        }
        jsonReply.putNumber("error", 1000);
        jsonReply.putString("desc", "requestInfo is null");
        message.reply(jsonReply);
        return;
    }


    private void doBanknetTrackInfo(Message<JsonObject> message) {

        JsonObject jo = message.body();
        long p_start_time = jo.getLong(BANKNET_COLS.START_TIME);
        long p_end_time = jo.getLong(BANKNET_COLS.END_TIME);
        String p_customer_account = jo.getString(BANKNET_COLS.CUSTOMER_ACCOUNT);
        long p_amount = jo.getLong(BANKNET_COLS.AMOUNT);
        String p_partner_trans_id = jo.getString(BANKNET_COLS.PARTNER_TRANS_ID);
        int p_partner_cfm_code = jo.getInteger(BANKNET_COLS.PARTNER_CODE_CONFIRM);
        int p_internal_error = jo.getInteger(BANKNET_COLS.INTERNAL_ERROR);
        int p_external_error = jo.getInteger(BANKNET_COLS.EXTERNAL_ERROR);
        int p_source = jo.getInteger(BANKNET_COLS.SOURCE, 0);

        boolean result = dbProcess.M_SERVICE_BANKNET_UPSERT(p_start_time
                , p_end_time
                , p_customer_account
                , p_amount
                , p_partner_trans_id
                , p_partner_cfm_code
                , p_internal_error
                , p_external_error
                , p_source
                , logger);

        JsonObject jrpl = new JsonObject();
        jrpl.putBoolean(BANKNET_COLS.RESULT, result);
        message.reply(jrpl);
    }

    private void getBalance(Message<JsonObject> message) {
        JsonObject json = message.body();
        JsonObject jRpl = json.copy();

        int number = json.getInteger(fieldNames.NUMBER, 0);

        if (number <= 0) {
            jRpl.putNumber(fieldNames.BALANCE, 0);
            message.reply(jRpl);
            return;
        }

        Common.BuildLog log = new Common.BuildLog(container.logger(), number);
        JsonObject result = dbProcess.getAgentBalanceExtend("0" + number, log);
        JsonObject joUpdate = new JsonObject().putString(colName.PhoneDBCols.BALANCE_TOTAL, result.toString());
        phonesDb.updatePartial(number, joUpdate, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj event) {
            }
        });
        log.add("balance", result);
        log.writeLog();
        message.reply(result);
    }

    private void doGetC2CInfo(Message<JsonObject> message) {
        JsonObject json = message.body();

        String mtcn = json.getString(fieldNames.MTCN, "");
        int phoneNumber = json.getInteger(fieldNames.NUMBER, 0);

        Common.BuildLog log = new Common.BuildLog(container.logger());
        log.setPhoneNumber("0" + phoneNumber);
        log.add("func", "doGetC2CInfo");
        log.add("mtcn", mtcn);

        JsonArray jsonArray = dbProcess.getC2CInfo(mtcn, log);
        message.reply(jsonArray);

        log.writeLog();
    }

    private void doGetCouponId(Message<JsonObject> message) {
        JsonObject json = message.body();
        JsonObject jRpl = json.copy();

        long tranid = json.getLong(fieldNames.TRAN_ID, 0);
        int phoneNumber = json.getInteger(fieldNames.NUMBER, 0);

        Common.BuildLog log = new Common.BuildLog(container.logger());
        log.setPhoneNumber("0" + phoneNumber);
        log.add("func", "doGetCouponId");
        log.add("tranid", tranid);
        if (tranid == 0) {
            jRpl.putString(fieldNames.COUPON_ID, "");
            message.reply(jRpl);
            log.writeLog();
            return;
        }

        String couponId = dbProcess.getCouponId(tranid, log);
        log.add("couponId", couponId);
        log.writeLog();

        JsonObject jsonRpl = new JsonObject();
        jsonRpl.putString("couponid", couponId);
        message.reply(jsonRpl);
    }

    private void doBankOutManual(Message<JsonObject> message) {

        JsonObject j = message.body();

        String p_agent = j.getString(CashDepotOrWithdraw.AGENT);
        String p_bank_name = j.getString(CashDepotOrWithdraw.BANK_NAME);
        long p_pending_tran = j.getLong(CashDepotOrWithdraw.PENDING_TRAN);
        String p_cardname = j.getString(CashDepotOrWithdraw.CARDNAME);
        String p_cardnumber = j.getString(CashDepotOrWithdraw.CARDNUMBER);
        long p_amount = j.getLong(CashDepotOrWithdraw.AMOUNT);
        long p_received_amount = j.getLong(CashDepotOrWithdraw.RECEIVE_AMOUNT);
        long p_fee = j.getLong(CashDepotOrWithdraw.FEE);
        String p_comment = j.getString(CashDepotOrWithdraw.COMMENT);
        int inout_city = j.getInteger(CashDepotOrWithdraw.INOUT_CITY);
        String p_bank_branch = j.getString(CashDepotOrWithdraw.BANK_BRANCH);
        int p_source = j.getInteger(CashDepotOrWithdraw.SOURCE, 0);

        boolean res = dbProcess.BANKPOUT_PENDING_UPSERT(p_agent
                , p_bank_name
                , p_pending_tran
                , p_cardname
                , p_cardnumber
                , p_amount
                , p_fee
                , p_received_amount
                , p_comment
                , inout_city
                , p_bank_branch
                , p_source
                , logger);

        JsonObject jrpl = new JsonObject();
        jrpl.putBoolean(colName.BankManualCols.RESULT, res);
        message.reply(jrpl);
    }

    private void syncTrans() throws Exception {

        if (mIsProcess.compareAndSet(false, true)) {
            //get the last time we sync
            mSettingsDb.getLong("LAST_TRANS_SYNC_TIME", new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                    long last_sync_time = 0;
                    if (aLong != null) {
                        last_sync_time = aLong;
                    }

                    if (last_sync_time == 0) {
                        last_sync_time = System.currentTimeMillis() - 30 * 60 * 1000; //15*24*60*60*1000;
                    }

                    final Common.BuildLog log = new Common.BuildLog(logger);
                    log.setPhoneNumber("synctran");

                    Connection conn = mSyncPool.getConnect();
                    CallableStatement cs;
                    final boolean checkStoreApp = glbCfg.getBoolean(StringConstUtil.CHECK_STORE_APP, false);


                    final ResultSet res;
                    if (conn != null) {

                        final ArrayList<String> GHNList = new ArrayList<String>();// dbProcess.GetGHNPersonList(log);

                        try {
                            Timestamp date = new Timestamp(last_sync_time);
                            log.add("Begin sync from core for", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
//                            log.add("call store procedure", "{call UMARKET2BACKEND(?,?)}");

                            cs = conn.prepareCall("{call UMARKET2BACKEND(?,?)}");

                            cs.setTimestamp(1, date);
                            cs.registerOutParameter(2, OracleTypes.CURSOR);
                            cs.execute();
                            res = (ResultSet) cs.getObject(2);

                            while (res.next()) {
                                final Long ID = nullToDefault(res.getLong("ID"), 0L);                                 // TID
                                final String TYPE = nullToDefault(res.getString("TYPE"), "");                         // TYPE
                                final String INITIATOR = nullToDefault(res.getString("INITIATOR"), "");               // SO DIEN THOAI NGUOI TAO
                                final String CREDITOR = nullToDefault(res.getString("CREDITOR"), "").trim();          // SO DIEN THOAI NGUOI DUOC TANG TIEN
                                final String CREDITOR_NAME = nullToDefault(res.getString("CREDITORNAME"), CREDITOR);  // TEN NGUOI DUOC TANG TIEN
                                final String DEBITOR = nullToDefault(res.getString("DEBITOR"), "").trim();            // SO DIEN THOAI NGUOI BI GIAM TIEN
                                final String DEBITOR_NAME = nullToDefault(res.getString("DEBITORNAME"), DEBITOR);     // TEN NGUOI BI GIAM TIEN
                                final long AMOUNT = nullToDefault(res.getLong("AMOUNT"), 0L);                         // AMOUNT
                                final String RECIPIENT = nullToDefault(res.getString("RECIPIENT"), "");               // SO DIEN THOAI DUOC TOPUP
                                final long LAST_MODIFIED = nullToDefault(res.getTimestamp("LAST_MODIFIED"), 0L);      // LAST_MODIFIED
                                final int WALLETTYPE = nullToDefault(res.getInt("wallettype"), 0);                    // WALLETTYPE
                                final String PARENTTYPE = nullToDefault(res.getString("parentType"), "");
                                final String IS_AGENT = nullToDefault(res.getString("isAgent"), ""); // 1: agent, 0: enduser
                                final String PARENT_ID = nullToDefault(res.getString("parentId"), "");
                                String ADJ_DESC_TMP = "";
                                String NOTI_CONTENT_TMP = "";
                                String NOTI_CAPTION_TMP = "";
                                String ALIAS_NAME_TMP = "";
                                String HIS_CONTENT_TMP = "";
                                try {
                                    NOTI_CONTENT_TMP = nullToDefault(res.getString("noti_content"), "");
                                    NOTI_CAPTION_TMP = nullToDefault(res.getString("noti_caption"), "");
                                    ALIAS_NAME_TMP = nullToDefault(res.getString("alias_name"), "");
                                    HIS_CONTENT_TMP = nullToDefault(res.getString("his_content"), "");
                                    ADJ_DESC_TMP = nullToDefault(res.getString("adj_desc"), "");
                                } catch (Exception e) {
                                    ADJ_DESC_TMP = "";
                                }
                                time_sync = System.currentTimeMillis();
//                                log.add("noti content", NOTI_CONTENT_TMP);
//                                log.add("noti_caption", NOTI_CAPTION_TMP);
                                logger.info("phone :" + DEBITOR
                                        + ", TYPE : " + TYPE
                                        + ", PARENTTYPE : " + PARENTTYPE);
                                final String ALIAS_NAME = ALIAS_NAME_TMP;
                                //final String HIS_CONTENT = "".equalsIgnoreCase(ADJ_DESC_TMP) ? HIS_CONTENT_TMP : new String(DatatypeConverter.parseBase64Binary(HIS_CONTENT_TMP));
                                final String HIS_CONTENT = "".equalsIgnoreCase(HIS_CONTENT_TMP) ? HIS_CONTENT_TMP : new String(DatatypeConverter.parseBase64Binary(HIS_CONTENT_TMP));
                                //nam trong danh sach loai bo
                                if (ExcludeCREDITOR.contains(CREDITOR)) {
                                    log.add("exclude creditor", CREDITOR);
                                    continue;
                                }

                                //nam trong danh sach loai bo
                                if (ExcludeDEBITOR.contains(DEBITOR)) {
                                    log.add("exclude debitor", DEBITOR);
                                    continue;
                                }

                                if (specialListAgent.contains(DEBITOR)) {
//                                    JsonObject joInfoExtra = new JsonObject();
//                                    joInfoExtra.putString(StringConstUtil.NUMBER, CREDITOR);
//                                    joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, ID);
//                                    joInfoExtra.putNumber(StringConstUtil.AMOUNT, AMOUNT);
//                                    joInfoExtra.putString(StringConstUtil.SERVICE_ID, "trabu");
//                                    joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_TYPE, MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
//                                    promotionProcess.excuteAcquireBinhTanUserPromotion(CREDITOR, log, null, null, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joInfoExtra);
                                }


                                log.add("begin syn for tid : " + ID + " type : " + TYPE + " amount : " + AMOUNT + " creditor : " + CREDITOR + " debitor : " + DEBITOR + " wallettype " + WALLETTYPE,
                                        "noti content" + NOTI_CONTENT_TMP);

                                last_sync_time = Math.max(last_sync_time, LAST_MODIFIED);

                                final TranObj mainObj = new TranObj();
                                mainObj.tranId = ID;
                                mainObj.clientTime = LAST_MODIFIED;
                                mainObj.ackTime = LAST_MODIFIED;
                                mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
                                mainObj.amount = AMOUNT;
                                mainObj.status = TranObj.STATUS_OK;
                                mainObj.error = 0;
                                mainObj.cmdId = -1;
                                mainObj.billId = "-1";
                                mainObj.io = -1;
                                mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                boolean isMainValid = false;

                                final TranObj rcvObj = new TranObj();
                                rcvObj.tranId = ID;
                                rcvObj.clientTime = LAST_MODIFIED;
                                rcvObj.ackTime = LAST_MODIFIED;
                                rcvObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
                                rcvObj.amount = AMOUNT;
                                rcvObj.status = TranObj.STATUS_OK;
                                rcvObj.error = 0;
                                rcvObj.cmdId = -1;
                                rcvObj.billId = "-1";
                                rcvObj.io = 1;
                                rcvObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                boolean isRecvValid = false;

//                                log.add("tran type", TYPE);
//                                log.add("wallet type", WALLETTYPE);
//                                log.add("isAgent", IS_AGENT);
                                log.add("ParentId", PARENT_ID);
//                                log.add("his content", HIS_CONTENT);
                                //todo build trans object than, upsert --> topup
                                if ("buy".equalsIgnoreCase(TYPE)) {

                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
                                    if (mainObj.owner_number > 0) {

                                        log.add("debitor number", "0" + mainObj.owner_number);
//                                        log.add("debitor name", DEBITOR_NAME);

                                        mainObj.owner_name = DEBITOR_NAME;
                                        mainObj.io = -1;
                                        mainObj.category = -1;
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.TOP_UP_VALUE;

                                        if (RECIPIENT != null) {
                                            mainObj.partnerId = DataUtil.getPhoneNumberProviderInVN(RECIPIENT);
                                            mainObj.parterCode = RECIPIENT;
                                            mainObj.partnerName = "";
                                        }

                                        isMainValid = true;
                                    }
                                }   //END TYPE = buy
                                else if ("transfer".equalsIgnoreCase(TYPE)) {

                                    log.add("CREDITOR/DEBITOR", CREDITOR + "/" + DEBITOR);

                                    if (GHNList.contains(CREDITOR)) {

                                        log.add("GHNList contains CREDITOR", CREDITOR);

                                        dbProcess.PRO_UPDATE_NAPRUTTANNOI(CREDITOR, DEBITOR, LAST_MODIFIED, ID, log);

                                    } else if (GHNList.contains(DEBITOR)) {

                                        log.add("GHNList contains DEBITOR", DEBITOR);

                                        dbProcess.PRO_UPDATE_NAPRUTTANNOI(DEBITOR, CREDITOR, LAST_MODIFIED, ID, log);
                                    } else {
//                                        log.add("GHNList contains: khong chua CREDITOR: " + CREDITOR + " va DEBITOR " + DEBITOR, "");

                                    }
                                    //chuyen tien M2M

                                    //nguoi gui
                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
                                    if (mainObj.owner_number > 0) {

                                        log.add("debitor number", "0" + mainObj.owner_number);
//                                        log.add("debitor name", DEBITOR_NAME);

                                        mainObj.io = -1;
                                        mainObj.owner_name = DEBITOR_NAME;
                                        mainObj.billId = "-1";
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                                        mainObj.comment = "Chuyển tiền từ tài khoản MoMo đến tài khoản MoMo";
                                        mainObj.partnerName = CREDITOR_NAME;
                                        mainObj.category = 8;
                                        mainObj.partnerId = CREDITOR;

                                        isMainValid = true;
                                    }

                                    //nguoi nhan
                                    rcvObj.owner_number = DataUtil.strToInt(CREDITOR);
                                    if (rcvObj.owner_number > 0) {
                                        log.add("creditor number", "0" + rcvObj.owner_number);
//                                        log.add("creditor name", CREDITOR);
//                                        log.add("Alias name", ALIAS_NAME);
//                                        log.add("his content", HIS_CONTENT);

                                        rcvObj.owner_name = CREDITOR_NAME;
                                        rcvObj.io = 1;
                                        rcvObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                                        rcvObj.billId = "-1";
                                        if (!"".equalsIgnoreCase(HIS_CONTENT)) {
                                            rcvObj.comment = HIS_CONTENT;
                                        }

                                        if ("123pay_promo".equalsIgnoreCase(DEBITOR)) {
                                            rcvObj.partnerName = "M_Service";
                                            rcvObj.partnerId = "Hoàn phí";
                                        } else {
                                            rcvObj.partnerName = "".equalsIgnoreCase(ALIAS_NAME) ? DEBITOR_NAME : ALIAS_NAME;
                                            rcvObj.partnerId = DEBITOR;
                                        }

                                        isRecvValid = true;
                                    }

                                } //END TYPE = transfer
                                else if ("billpay".equalsIgnoreCase(TYPE)) {

                                    //thanh toan hoa don
                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
                                    if (mainObj.owner_number > 0) {

                                        log.add("debitor number", "0" + mainObj.owner_number);
//                                        log.add("debitor name", DEBITOR_NAME);

                                        mainObj.io = -1;
                                        mainObj.owner_name = DEBITOR_NAME;
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                                        mainObj.partnerId = CREDITOR; // todo
                                        mainObj.partnerName = CREDITOR_NAME; // todo
                                        mainObj.parterCode = ""; // todo ten chu hoa don
                                        mainObj.billId = RECIPIENT; // todo

                                        isMainValid = true;
                                    }

                                } //END Type = billpay
                                else if ("bankcashin".equalsIgnoreCase(TYPE)) {

                                    //nap tien tu ngan hang lien ket
                                    mainObj.owner_number = DataUtil.strToInt(CREDITOR);
                                    if (mainObj.owner_number > 0) {

                                        log.add("creditor number", "0" + mainObj.owner_number);
//                                        log.add("creditor name", DEBITOR_NAME);

                                        mainObj.owner_name = CREDITOR_NAME;
                                        mainObj.io = 1;
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BANK_IN_VALUE;
                                        mainObj.partnerId = "Ngân hàng liên kết";
                                        mainObj.parterCode = getBankCode(DEBITOR);
                                        mainObj.partnerName = DEBITOR_NAME;
                                        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE;
                                        isMainValid = true;
                                    }
                                }  //END TYPE = BANKIN
                                else if ("bankcashout".equalsIgnoreCase(TYPE)) {

                                    //chuyen tien ra ngan hang
                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
                                    if (mainObj.owner_number > 0) {

                                        log.add("debitor number", "0" + mainObj.owner_number);
//                                        log.add("debitor name", DEBITOR_NAME);

                                        mainObj.owner_name = DEBITOR_NAME;
                                        mainObj.io = -1;
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BANK_OUT_VALUE;
                                        mainObj.partnerId = "Ngân hàng liên kết";
                                        mainObj.parterCode = getBankCode(CREDITOR);
                                        mainObj.partnerName = CREDITOR_NAME;
                                        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE;
                                        isMainValid = true;
                                    }
                                }   //END TYPE = bankout
                                else if ("adjustment".equalsIgnoreCase(TYPE)) {

                                    //nguoi gui
                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
                                    if (mainObj.owner_number > 0) {

                                        log.add("debitor number", "0" + mainObj.owner_number);
//                                        log.add("debitor name", DEBITOR_NAME);
                                        String pid = CREDITOR;
                                        if (bankMap.containsKey(CREDITOR)) {
                                            pid = bankMap.get(CREDITOR);
                                        }
                                        mainObj.owner_name = DEBITOR_NAME;
                                        mainObj.billId = "-1";
                                        mainObj.io = -1;
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                                        mainObj.comment = "Chuyển tiền từ tài khoản MoMo đến tài khoản MoMo";
                                        mainObj.partnerName = CREDITOR_NAME;
                                        mainObj.category = 8;
                                        //mainObj.partnerId = CREDITOR;
                                        mainObj.partnerId = pid;
                                        isMainValid = true;
                                    }

                                    //nguoi nhan
                                    rcvObj.owner_number = DataUtil.strToInt(CREDITOR);
                                    if (rcvObj.owner_number > 0) {
                                        if (!"".equalsIgnoreCase(HIS_CONTENT)) {
                                            rcvObj.comment = HIS_CONTENT;
                                        }
                                        log.add("creditor number", "0" + rcvObj.owner_number);
//                                        log.add("creditor name", CREDITOR_NAME);
                                        String rid = DEBITOR;
                                        if (bankMap.containsKey(DEBITOR)) {
                                            rid = bankMap.get(DEBITOR);
                                        }
                                        rcvObj.owner_name = CREDITOR_NAME;
                                        rcvObj.io = 1;
                                        rcvObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                                        rcvObj.billId = "-1";
                                        rcvObj.partnerName = DEBITOR_NAME;
                                        //rcvObj.partnerId = DEBITOR;
                                        rcvObj.partnerId = rid;

                                        //giao dich hoan tien goc bankoutmanual
                                        if (bankoutmanual.equalsIgnoreCase(DEBITOR.trim())) {
                                            rcvObj.comment = "Hoàn tiền rút tiền về ngân hàng";
                                        }

                                        //hoan phi chuyen tien bankout manual
                                        if (bankoutmanualfee.equalsIgnoreCase(DEBITOR)) {
                                            rcvObj.comment = "Hoàn phí rút tiền về ngân hàng";
                                        }

                                        isRecvValid = true;

                                        /*//khong cap nhat cac giao dich hoan tra tien tu bankout manual va bankout manual fee
                                        if(bankoutmanual.equalsIgnoreCase(DEBITOR) || bankoutmanualfee.equalsIgnoreCase(DEBITOR)){
                                                isRecvValid =false;
                                        }else{
                                            isRecvValid = true;
                                        }*/
                                    }

                                    //wallettype: 1 momo: 2 mload, 3: point, 4:voucher
                                }   //END TYPE = adjust
                                else if (("bonus".equalsIgnoreCase(TYPE))) {
                                    mainObj.owner_number = DataUtil.strToInt(CREDITOR);
                                    log.add("creditor number", "0" + mainObj.owner_number);
//                                    log.add("creditor name", CREDITOR_NAME);

                                    mainObj.owner_name = CREDITOR_NAME;
                                    //mainObj.owner_number = Integer.parseInt(CREDITOR);
                                    mainObj.io = 1;
                                    mainObj.tranType = MomoProto.TranHisV1.TranType.BONUS_VALUE;
                                    if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BONUS_DGD_VALUE;
                                    }
//                                    log.add("checkStoreApp --->", IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE));
//                                    log.add("mainObj.tranType --->", mainObj.tranType);

                                    //tien hoa hong tra theo point
//                                    //neu la dien giao dich tra theo momo
                                    JsonObject note = new JsonObject();
                                    if ((WALLETTYPE == WalletType.MOMO || WALLETTYPE == WalletType.POINT)
                                            && mainObj.owner_number > 0) {
//                                        mainObj.comment = "Bạn nhận được " + AMOUNT + "đ tiền hoa hồng của dịch vụ.";
                                        if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
                                            mainObj.comment = "Bạn nhận được " + Misc.formatAmount(AMOUNT).replaceAll(",", ".") + "đ tiền thù lao của dịch vụ.";
                                            if ("".equalsIgnoreCase(mainObj.partnerId)) {
                                                mainObj.partnerId = "Tiền thù lao";
                                            }
                                        } else {
                                            if ("promotion".equalsIgnoreCase(DEBITOR) && mainObj.owner_number > 0) {
                                                mainObj.comment = StringConstUtil.BONUS_300_CONTENT;
                                                note.putBoolean(StringConstUtil.KEY, true);
                                            } else {
                                                mainObj.comment = "Bạn nhận được " + Misc.formatAmount(AMOUNT).replaceAll(",", ".") + "đ tiền hoa hồng của dịch vụ.";
                                                note.putBoolean(StringConstUtil.KEY, false);
                                            }
                                            JsonArray jsonArray = new JsonArray();
                                            jsonArray.add(note);
                                            mainObj.share = jsonArray;

                                        }
                                        isMainValid = true;
                                    }

                                    //hoa hong gui tien
                                    if ("c2ccomin".equalsIgnoreCase(DEBITOR) && mainObj.owner_number > 0) {
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.C2C_BONUS_SEND_VALUE;
//                                        mainObj.comment = "Bạn nhận được "
//                                                    + Misc.formatAmount(AMOUNT).replaceAll(",",".")
//                                                    + "đ hoa hồng gửi tiền.";
                                        if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
                                            mainObj.comment = "Bạn nhận được "
                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
                                                    + "đ thù lao gửi tiền.";
                                        } else {
                                            mainObj.comment = "Bạn nhận được "
                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
                                                    + "đ hoa hồng gửi tiền.";
                                        }

                                        isMainValid = true;

                                    }

                                    //hoa hong nhan tien
                                    if ("c2ccomout".equalsIgnoreCase(DEBITOR) && mainObj.owner_number > 0) {

                                        //reset tran type
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.C2C_BONUS_RECEIVE_VALUE;
//                                        mainObj.comment = "Bạn nhận được "
//                                                    + Misc.formatAmount(AMOUNT).replaceAll(",",".")
//                                                    + "đ hoa hồng nhận tiền.";
                                        if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
                                            mainObj.comment = "Bạn nhận được "
                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
                                                    + "đ thù lao nhận tiền.";

                                        } else {
                                            mainObj.comment = "Bạn nhận được "
                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
                                                    + "đ hoa hồng nhận tiền.";

                                        }

                                        isMainValid = true;

                                    }

                                } // END BONUS
                                else if ("fee".equalsIgnoreCase(TYPE)) {
                                    //khuyen mai
                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
                                    if (mainObj.owner_number > 0) {

                                        log.add("debitor number", "0" + mainObj.owner_number);
                                        log.add("debitor name", DEBITOR_NAME);

                                        mainObj.owner_name = DEBITOR_NAME;
                                        mainObj.io = -1;
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.FEE_VALUE;
                                        mainObj.amount = AMOUNT;

                                        //parentType = nap,rut,gui,nhan,u2u,ag2ag
                                        //fee rut tien tai diem giao dich
                                        if ("rut".equalsIgnoreCase(PARENTTYPE)) {
                                            mainObj.comment = "Bạn đã bị trừ "
                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
                                                    + "đ - phí rút tiền tại điểm giao dịch.";
                                            isMainValid = true;
                                        } else {
                                            if (AMOUNT == 1000) {
                                                mainObj.comment = "Phí dịch vụ chuyển tiền";
                                                isMainValid = false;
                                            } else {
                                                isMainValid = true;
                                                mainObj.comment = "Bạn đã bị trừ "
                                                        + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
                                                        + "đ tiền phí dịch vụ.";
                                            }
                                        }
                                    }
                                } //END TYPE = fee

                                TransDb tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());

                                final long fAmount = AMOUNT;
                                final String fType = TYPE;
                                final String fDebitor = DEBITOR;
                                final String NOTI_CONTENT = "".equalsIgnoreCase(NOTI_CONTENT_TMP) ? NOTI_CONTENT_TMP : new String(DatatypeConverter.parseBase64Binary(NOTI_CONTENT_TMP));
                                final String NOTI_CAPTION = "".equalsIgnoreCase(NOTI_CAPTION_TMP) ? NOTI_CAPTION_TMP : new String(DatatypeConverter.parseBase64Binary(NOTI_CAPTION_TMP));
                                ;
                                log.add("NOTI_CONTENT DECODE", NOTI_CONTENT);
                                log.add("NOTI_CAPTION DECODE", NOTI_CAPTION);
                                //Check dk noti
                                final JsonArray jsonListTurnOffNotiNumber = joTurnOffNoti.getArray(StringConstUtil.TurningOffNotification.LIST_NUMBER, new JsonArray());
                                final JsonArray jsonListTurnOffAgent = joTurnOffNoti.getArray(StringConstUtil.TurningOffNotification.LIST_AGENT, new JsonArray());
                                final boolean turnOffNoti = joTurnOffNoti.getBoolean(StringConstUtil.TurningOffNotification.IS_ACTIVE, false);
                                //BEGIN isMainValid
                                if (isMainValid && turnOffNoti && jsonListTurnOffAgent.contains(CREDITOR)) {
                                    log.add("desc isMainValid 1", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
//                                    log.writeLog();
                                }
                                else if (isMainValid && turnOffNoti && jsonListTurnOffAgent.contains(DEBITOR)) {
                                    log.add("desc isMainValid 2", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
//                                    log.writeLog();
                                }
                                else if (isMainValid) {

                                    final Common.BuildLog tranSndLog = new Common.BuildLog(logger);
                                    tranSndLog.setPhoneNumber("0" + mainObj.owner_number);
                                    tranSndLog.add("Res", res);
                                    tranSndLog.add("this is tran for sender", "");
                                    tranSndLog.add("isMainValid", isMainValid);
                                    tranSndLog.add("Json tran will be upsert", mainObj.getJSON());
//                                    NOTI_CONTENT_TMP = nullToDefault(res.getString("noti_content"), "");
//                                    NOTI_CAPTION_TMP = nullToDefault(res.getString("noti_caption"), "");
//                                    ALIAS_NAME = nullToDefault(res.getString("alias_name"), "");
//                                    HIS_CONTENT = nullToDefault(res.getString("his_content"), "");
//                                    ADJ_DESC_TMP = nullToDefault(res.getString("adj_desc"), "");
                                    if (!"".equalsIgnoreCase(HIS_CONTENT)) {
                                        mainObj.comment = HIS_CONTENT;
                                    }

                                    if (!"".equalsIgnoreCase(ADJ_DESC_TMP) && "fee".equalsIgnoreCase(ADJ_DESC_TMP)) {
                                        log.add("desc", "phi dich vu tu ben connector");
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.FEE_VALUE;
                                    } else if (!"".equalsIgnoreCase(ADJ_DESC_TMP) && "bonus".equalsIgnoreCase(ADJ_DESC_TMP)) {
                                        log.add("desc", "bonus tu ben connector");
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BONUS_VALUE;
                                    } else if (!"".equalsIgnoreCase(ADJ_DESC_TMP) && "payment".equalsIgnoreCase(ADJ_DESC_TMP)) {
                                        log.add("desc", "thanh toan tu ben connector");
                                        mainObj.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                                        mainObj.billId = "";
                                    }
                                    time_sync = System.currentTimeMillis();
                                    tranDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean result) {

                                            tranSndLog.add("isUpdated", result);
                                            String cap = "";
                                            String body = "";

                                            if (!result) {
                                                //phi giao dich m2m
                                                if (fAmount == 1000 && "fee".equalsIgnoreCase(fType)) {
                                                    cap = "Phí dịch vụ";
                                                    body = "Số dư tài khoản của bạn thay đổi: -1.000đ phí dịch vụ chuyển tiền.";
                                                }

                                                //c2c hoa hong gui tien
                                                if ("c2ccomin".equalsIgnoreCase(fDebitor)) {
                                                    cap = "Thù lao gửi tiền";
                                                    body = "Bạn nhận được " + Misc.formatAmount(fAmount).replaceAll(",", ".") + "đ thù lao gửi tiền C2C";
                                                }

                                                //c2c hoa hong nhan tien
                                                if ("c2ccomout".equalsIgnoreCase(fDebitor)) {
                                                    cap = "Thù lao nhận tiền";
                                                    body = "Bạn nhận được " + Misc.formatAmount(fAmount).replaceAll(",", ".") + "đ thù lao nhận tiền C2C";
                                                }

                                                if ("rut".equalsIgnoreCase(PARENTTYPE)) {
                                                    cap = "Phí rút tiền";
                                                    body = mainObj.comment;
                                                }

                                                BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                                                tranSndLog.add("cap --------------->", cap);

                                                //ban notification
                                                if (!"".equalsIgnoreCase(cap)) {
                                                    final Notification noti = new Notification();
                                                    noti.receiverNumber = mainObj.owner_number;
                                                    noti.caption = cap;
                                                    noti.body = body;
                                                    noti.bodyIOS = body;
                                                    noti.sms = "";
                                                    noti.tranId = mainObj.tranId;
                                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                                    noti.priority = 2;
                                                    noti.status = Notification.STATUS_DETAIL;
                                                    noti.time = System.currentTimeMillis();
                                                    Misc.sendNoti(vertx, noti);
                                                } else if (!"".equalsIgnoreCase(NOTI_CAPTION)) {
                                                    final Notification noti = new Notification();
                                                    noti.receiverNumber = mainObj.owner_number;
                                                    noti.caption = NOTI_CAPTION;
                                                    noti.body = NOTI_CONTENT;
                                                    noti.bodyIOS = NOTI_CONTENT;
                                                    noti.sms = "";
                                                    noti.tranId = mainObj.tranId;
                                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                                    noti.priority = 2;
                                                    noti.status = Notification.STATUS_DETAIL;
                                                    noti.time = System.currentTimeMillis();
                                                    Misc.sendNoti(vertx, noti);

                                                } else {
                                                    if ((turnOffNoti && jsonListTurnOffAgent.contains(CREDITOR) && jsonListTurnOffNotiNumber.contains(DEBITOR)) && ("adjustment".equalsIgnoreCase(TYPE))) {
                                                        tranSndLog.add("desc", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
                                                    } else {
                                                        //Check dk noti
                                                        tranSndLog.add("createRequestPushNotification ----------------------> ", "five");
                                                        vertx.eventBus().send(
                                                                AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                                                , NotificationUtils.createRequestPushNotification(mainObj.owner_number
                                                                        , 2
                                                                        , mainObj)
                                                        );
                                                    }

                                                }
                                            }
                                            tranSndLog.writeLog();
                                        }
                                    });
                                }
                                //END isMainValid


                                //Begin isRecvValid
                                if (isRecvValid) {
                                    time_sync = System.currentTimeMillis();
                                    if (turnOffNoti && jsonListTurnOffAgent.contains(DEBITOR)) {
                                        log.add("desc isRecvValid", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
                                    }
                                    else {
                                        final Common.BuildLog tranRcvLog = new Common.BuildLog(logger);
                                        tranRcvLog.setPhoneNumber("0" + rcvObj.owner_number);
                                        tranRcvLog.add("this is tran for receiver", "");
                                        tranRcvLog.add("isRecvValid", isRecvValid);
                                        tranRcvLog.add("Json tran will be upsert", rcvObj.getJSON());
                                        final boolean isRecvValid_tmp = isRecvValid;
                                        tranDb.upsertTranOutSideNew(rcvObj.owner_number, rcvObj.getJSON(), new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean result) {
                                                tranRcvLog.add("isUpdated", result);

                                                //neu khong phai la cap nhat --> tao moi
                                                if (!result) {
                                                    if (turnOffNoti && jsonListTurnOffAgent.contains(DEBITOR)) {
                                                        final Notification noti = new Notification();
                                                        noti.receiverNumber = rcvObj.owner_number;
                                                        noti.caption = "Chuyển tiền từ TK Khuyến mãi sang TK MoMo";
                                                        noti.body = "Tiền từ TK Khuyến mãi đã được chuyển vào TK MoMo của quý khách.";
                                                        noti.bodyIOS = "Tiền từ TK Khuyến mãi đã được chuyển vào TK MoMo của quý khách.";
                                                        noti.sms = "";
                                                        noti.tranId = rcvObj.tranId;
                                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                                        noti.priority = 2;
                                                        noti.status = Notification.STATUS_DETAIL;
                                                        noti.time = System.currentTimeMillis();
                                                        Misc.sendNoti(vertx, noti);
                                                    } else if (!"".equalsIgnoreCase(NOTI_CONTENT) && !"".equalsIgnoreCase(NOTI_CAPTION)) {
                                                        String content = NOTI_CONTENT.replaceAll("<<transid>>", rcvObj.tranId + "");
                                                        log.add("NOTI_CONTENT", "true");
                                                        log.add("NOTI_CONTENT BEFORE", NOTI_CONTENT);
                                                        log.add("NOTI_CONTENT AFTER", content);
                                                        final Notification noti = new Notification();
                                                        noti.receiverNumber = rcvObj.owner_number;
                                                        noti.caption = "".equalsIgnoreCase(NOTI_CAPTION) ? "Nhận tiền thành công!" : NOTI_CAPTION;
                                                        noti.body = content;
                                                        noti.bodyIOS = content;
                                                        noti.sms = "";
                                                        noti.tranId = rcvObj.tranId;
                                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                                        noti.priority = 2;
                                                        noti.status = Notification.STATUS_DETAIL;
                                                        noti.time = System.currentTimeMillis();
                                                        Misc.sendNoti(vertx, noti);
                                                    } else {
                                                        if ((turnOffNoti && jsonListTurnOffAgent.contains(CREDITOR) && jsonListTurnOffNotiNumber.contains(DEBITOR)) && ("adjustment".equalsIgnoreCase(TYPE))) {
                                                            tranRcvLog.add("desc", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
                                                        }
                                                        else {
                                                            log.add("createRequestPushNotification ----------------------> ", "four");
                                                            BroadcastHandler.sendOutSideTransSync(vertx, rcvObj);
                                                            vertx.eventBus().send(
                                                                    AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                                                    , NotificationUtils.createRequestPushNotification(
                                                                            rcvObj.owner_number,
                                                                            2,
                                                                            rcvObj)
                                                            );
                                                        }
                                                    }
                                                }
                                                tranRcvLog.writeLog();
                                            }
                                        });
                                    }
                                } //END isRecvValid
                            } // End while

                            log.add("last sync", last_sync_time);
                            log.add("last time", Misc.dateVNFormatWithTime(last_sync_time));

                            mSettingsDb.setLong("LAST_TRANS_SYNC_TIME", last_sync_time, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean event) {
                                    //do nothing here
                                    log.add("save last sync time result", event);
                                    log.writeLog();
                                }
                            });

                            if (conn != null && !conn.isClosed()) {
                                conn.close();
                            }

                        } catch (SQLException e) {

                            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
                            e.printStackTrace();
                            logger.error("co loi tai call UMARKET2BACKEND(?,?) " + errorDesc);

                            log.add("Execute statement failed", errorDesc);
                            log.writeLog();
                        } finally {
                            mIsProcess.set(false);
                        } // End try
                    }
                }
            });
        }
    }

    public long nullToDefault(Long val, long def) {
        return (val != null ? val : def);
    }

    public String nullToDefault(String val, String def) {
        return (val != null ? val : def);
    }

    public long nullToDefault(Timestamp val, long def) {
        return (val != null ? val.getTime() : def);
    }

    public int nullToDefault(Integer val, int def) {
        return (val != null ? val.intValue() : def);
    }

    private void checkSyncTrans(long time)
    {
        long currentTime = System.currentTimeMillis();

        if(currentTime - 1000L * 60 * 15 > time_sync)
        {
            logger.info("NOT SYNC TRANSACTIONS");
        }
        else {
            logger.info("SYNC TRANSACTIONS");
        }
    }

    private void login(Message<JsonObject> message) {

        JsonObject joRequest = message.body();
        String phoneNumber = joRequest.getString(StringConstUtil.NUMBER, "");
        String pass = joRequest.getString(StringConstUtil.PIN, "");
        String encode = DataCrypt.encrypt("auth_token", phoneNumber, "pin", pass);
        boolean result = dbProcess.login(phoneNumber, encode);
        JsonObject joReply = new JsonObject();
        joReply.putBoolean(StringConstUtil.RESULT, result);
        message.reply(joReply);
    }

    public static class fieldNames {
        public static String NUMBER = "number";
        public static String TYPE = "type";
        public static String BALANCE = "balance";
        public static String COUPON_ID = "couponid";
        public static String TRAN_ID = "tranid";
        public static String MTCN = "mtcn";
    }

    public static class CashDepotOrWithdraw {
        public static String TYPE = "type";
        public static String AGENT = "agent";
        public static String BANK_NAME = "bankname";
        public static String PENDING_TRAN = "pendingtran";
        public static String CARDNAME = "cardname";
        public static String CARDNUMBER = "cardnumber";
        public static String AMOUNT = "amount";
        public static String RECEIVE_AMOUNT = "recieveamount";
        public static String FEE = "fee";
        public static String COMMENT = "comment";
        public static String INOUT_CITY = "inoutcity";
        public static String BANK_BRANCH = "bankbranch";
        public static String SOURCE = "source";
        public static String RESULT = "result";

    }

    public static class BANKNET_COLS {
        public static String TYPE = "type";
        public static String START_TIME = "start_time";
        public static String END_TIME = "end_time";
        public static String CUSTOMER_ACCOUNT = "customer_account";
        public static String AMOUNT = "amount";
        public static String PARTNER_TRANS_ID = "trans_id";
        public static String PARTNER_CODE_CONFIRM = "partner_cfm_code";
        public static String INTERNAL_ERROR = "internal_error";
        public static String EXTERNAL_ERROR = "external_error";
        public static String SOURCE = "source";
        public static String RESULT = "result";
    }


}
