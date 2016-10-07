package com.mservice.momo.gateway.internal.db.oracle;

import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.SettingsDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStore;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStoreNearestRequest;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.JSONUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.rate.CoreAgentRating;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by concu on 5/28/14.
 */
public class LStandbyOracleVerticle extends Verticle {

    //share all
    public static final int DEPOSIT_WITHDRAW = 1;
    public static final int UPDATE_NAPRUT_TANNOI = 2;
    public static final int STORE_RATING = 3;
    public static final int GET_M2M_TYPE_AND_CAPSET = 4;
    public static final int DO_NAMED = 5;
    public static final int GET_LIQUIDITY = 6; // get noi dung canh bao mat thanh khoan
    public static final int CHECK_PRO_C2C_RULE = 7;
    public static final int CHECK_MOMO_AGENT = 8;
    public static final int GET_C2C_INFO = 9;
    public static final int GET_GROUP_AGENT = 10;
    public static final int GET_INFO_BANK_USER = 11;
    public static final String TIME = "time";
    public static final String COMMAND = "type";
    private static String Driver;
    private static String Url;
    private static String Username;
    private static String Password;
    private static double LNG_MAX = 0;
    private static double LNG_MIN = 0;
    private static double LAT_MAX = 0;
    private static double LAT_MIN = 0;
    private static HashMap<String, String> bankMap;
    private static String sepDeg = "°";
    private static String sepMin = "\'";
    private static String sepSec = "\"";
    private static String sepComma = ",";
    private static String replaced = ";";
    private static long time_sync = 0;
    private static ArrayList<String> NotAllowTranType = new ArrayList<>();
    private static List<String> specialListAgent;

    static {
        bankMap = new HashMap<>();
        bankMap.put("vietinbank.bank", "102");
        bankMap.put("vcb.bank", "12345");
        bankMap.put("vpb.bank", "103");
        bankMap.put("ocb.bank", "104");
        bankMap.put("acb.bank", "106");
        bankMap.put("exb.bank", "107");
    }

    static {
        specialListAgent = new ArrayList<>();
        specialListAgent.add("vcb.bank");
        specialListAgent.add("vpb.bank");
        specialListAgent.add("ocb.bank");
        specialListAgent.add("exb.bank");
        specialListAgent.add("bal123pay");
        specialListAgent.add("balsmartlink_vcb");
        specialListAgent.add("balbanknet_vcb");
        specialListAgent.add("vietinbank.bank");
    }

    static {

        //TODO : neu muon khong thuc hien khuyen mai cho dich vu nao do thi them vao day
        NotAllowTranType.add("adjustment");
        NotAllowTranType.add("bonus");
        NotAllowTranType.add("fee");

    }

    private final Object syncLock = new Object();
    private PromotionProcess promotionProcess;
    //    public static String ADDRESS ="LStandbyOracleVerticle";
    private Logger logger;
    private DBProcess dbProcess;
    private SettingsDb mSettingsDb;
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

    public static void sendHumanATMRequest() {

    }

    public void start() {
        logger = container.logger();
        glbCfg = container.config();

        agentsDb = new AgentsDb(vertx.eventBus(), logger);

        JsonObject db_cfg = glbCfg.getObject("lstandby_database");
        Driver = db_cfg.getString("driver");
        Url = db_cfg.getString("url");
        Username = db_cfg.getString("username");
        Password = db_cfg.getString("password");

        JsonObject map_cfg = glbCfg.getObject("map");
        LNG_MAX = (double) map_cfg.getNumber("lng_max", 0);
        LNG_MIN = (double) map_cfg.getNumber("lng_min", 0);
        LAT_MAX = (double) map_cfg.getNumber("lat_max", 0);
        LAT_MIN = (double) map_cfg.getNumber("lat_min", 0);
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

        logger.info(AppConstant.LStandbyOracleVerticle_ADDRESS + " bankoutmanual : " + bankoutmanual);
        logger.info(AppConstant.LStandbyOracleVerticle_ADDRESS + " bankoutmanualfee : " + bankoutmanualfee);

        joTurnOffNoti = glbCfg.getObject(StringConstUtil.TurningOffNotification.JSON_OBJECT, new JsonObject());
        //==> must be start after MongoDB
        mSettingsDb = new SettingsDb(vertx.eventBus(), logger);
        mPhonesDb = new PhonesDb(vertx.eventBus(), logger);
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);
        dbProcess = new DBProcess(Driver
                , Url
                , Username
                , Password
                , AppConstant.LStandbyOracleVerticle_ADDRESS
                , AppConstant.LStandbyOracleVerticle_ADDRESS
                , logger);

        /*Common.BuildLog log = new Common.BuildLog(logger);
        JsonObject jo = dbProcess.PRO_GET_CAPTION_SMS_LIQUIDITY("0979754034",log);*/

//        if (allow_sync_data) {
//            mSyncPool = new ConnectDB(Driver
//                    , Url
//                    , Username
//                    , Password
//                    , "LStandbySyncPool"
//                    , "LStandbySyncPool"
//                    , logger);
//        }

        EventBus eb = vertx.eventBus();

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                JsonObject jsonObject = message.body();

                int type = jsonObject.getInteger(COMMAND);
                switch (type) {
                    case DEPOSIT_WITHDRAW:
                        doDepositWithdraw(message);
                        break;

                    case UPDATE_NAPRUT_TANNOI:
                        doUpdateNapRutTanNoi(message);
                        break;

                    case STORE_RATING:
                        doStoreRating(message);
                        break;
                    case GET_M2M_TYPE_AND_CAPSET:
                        doM2mTypeAndCapset(message);
                        break;
                    case DO_NAMED:
                        doUdateNamed(message);
                        break;

                    case GET_LIQUIDITY:
                        doGetLiquidity(message);
                        break;
                    case GET_C2C_INFO:
                        doGetC2CInfo(message);
                        break;
                    case CHECK_PRO_C2C_RULE:
                        checkProC2cRule(message);
                        break;
                    case CHECK_MOMO_AGENT:
                        checkMomoAgent(message);
                        break;
                    case GET_GROUP_AGENT:
                        getGroupListAgent(message);
                        break;
                    case GET_INFO_BANK_USER:
                        getInfoBankUser(message);
                        break;
                    default:
                        logger.warn("LStandbyOracleVerticle NOT SUPPORT COMMAND " + type);
                        break;
                }
            }
        };

        eb.registerLocalHandler(AppConstant.LStandbyOracleVerticle_ADDRESS, myHandler);

        //cap nhat tat ca diem giao dich thanh dinh danh
        //updateAllMMTAgent2Named();

        //cho phep sync du lieu
        if (allow_sync_data) {
            logger.info("Sync data is started");
            //sync location
            vertx.setPeriodic(15 * 60 * 1000, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    doStoresSync(event);
                }
            });

            //cho phep syc thong tin diem giao dich tren phone
            doStoresSync(0L);
//            vertx.setPeriodic(15 * 60 * 1000, new Handler<Long>() {
//                @Override
//                public void handle(Long timer) {
//                    checkSyncTrans(timer);
//                }
//            });
            //sync trans outside
//            vertx.setPeriodic(120 * 1000L, new Handler<Long>() {
//                @Override
//                public void handle(Long event) {
//                    synchronized (syncLock) {
//                        try {
//                            syncTrans();
//                        } catch (Exception e) {
//                            logger.error("Sync tran fail", e);
//                        }
//                    }
//                }
//            });

            //todo tam bo sync tran ngay khi start server
            /*try {
                syncTrans();
            }
            catch (Exception e){
                logger.error("Sync tran fail", e);
            }*/

            //sync agent info
            //start timer for sync agent info
            vertx.setPeriodic(10 * 60 * 1000, new Handler<Long>() {
                @Override
                public void handle(Long timerId) {
                    doSyncAgents();
                }
            });


            //todo tam bo sync thong tin vi khi start server
            doSyncAgents();

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

//    private void checkSyncTrans(long time)
//    {
//        long currentTime = System.currentTimeMillis();
//
//        if(currentTime - 1000L * 60 * 15 > time_sync)
//        {
//            logger.info("NOT SYNC TRANSACTIONS");
//        }
//        else {
//            logger.info("SYNC TRANSACTIONS");
//        }
//    }
    /**
     * get C2C Info from U => L
     * @param message
     */
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

    //BEGIN 0000000009 Check Momo Agent
    private void checkMomoAgent(Message<JsonObject> message) {
        JsonObject json = message.body();
        String mmphone = json.getString("mmphone", "0");
        int result = 1000;
        result = dbProcess.checkMMTAgent(mmphone, logger);
        JsonObject responseResult = new JsonObject();

        responseResult.putNumber("result", result);
        message.reply(responseResult);
        return;

    }
    //END 0000000019: check pro c2c rule


    private void getGroupListAgent(Message<JsonObject> message) {
        JsonObject json = message.body();
        String mmphone = json.getString("mmphone", "0");
        Set<Integer> result = dbProcess.getAgentGroupId(mmphone, logger);
        JsonObject responseResult = new JsonObject();
        responseResult.putString("result", StringUtils.join(result, ","));
        message.reply(responseResult);
        return;
    }

    private void getInfoBankUser(Message<JsonObject> message) {
        JsonObject json = message.body();
        String mmphone = json.getString(colName.WomanNationalCols.PHONE_NUMBER, "0");
        String bankCardId = json.getString(colName.WomanNationalCols.CARD_ID, "0");
        JsonArray jarrResult= dbProcess.getInfoBankUser(mmphone, bankCardId, logger);
        JsonObject responseResult = new JsonObject();
        responseResult.putArray(StringConstUtil.RESULT, jarrResult);
        message.reply(responseResult);
        return;
    }

    private void checkAgentStatus(Message<JsonObject> message) {
        JsonObject json = message.body();
        String mmphone = json.getString("aPhone", "0");
        MStore store = dbProcess.checkStoreStatus(mmphone, logger);

        JsonObject res = JSONUtil.fromObjToJsonObj(store);
        message.reply(res);
        return;
    }

    private void searchStoreNearest(Message<JsonObject> message) {
        MStoreNearestRequest request = JSONUtil.fromJsonObjToObj(message.body().getObject("nearest"), MStoreNearestRequest.class);
        dbProcess.selectStoreNearest(request, glbCfg.getObject("map"), logger);
        JsonObject res = JSONUtil.fromObjToJsonObj(request);
        message.reply(res);
        return;
    }

    private void searchStore(Message<JsonObject> message) {
        MStoreNearestRequest request = JSONUtil.fromJsonObjToObj(message.body().getObject("nearest"), MStoreNearestRequest.class);
        dbProcess.selectStore(request, glbCfg.getObject("map"), logger);
        JsonObject res = JSONUtil.fromObjToJsonObj(request);
        message.reply(res);
        return;
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

//    private void replyProM2mType(String isCapset, String m2mType, String fromNumber, String toNumber, Message message)
//    {
//        JsonObject joFrom = new JsonObject().putNumber(colName.PhoneDBCols.NUMBER, DataUtil.strToInt(fromNumber));
//        JsonObject joTo = new JsonObject().putNumber(colName.PhoneDBCols.NUMBER, DataUtil.strToInt(toNumber));
//        JsonArray jarr = new JsonArray().add(joFrom).add(joTo);
//
//        phonesDb.getPhoneInList(jarr, new Handler<ArrayList<PhonesDb.Obj>>() {
//            @Override
//            public void handle(ArrayList<PhonesDb.Obj> listPhoneObjs) {
//                 if()
//            }
//        });
//    }

    private String callProM2MType(String fromNumber, String toNumber, Common.BuildLog log, AtomicInteger count)
    {
        String m2mType = "";
        if(count.intValue() == 2)
        {
            m2mType = "".equalsIgnoreCase(m2mType) ? "u2u" : m2mType;
            return m2mType;
        }
        else {
            m2mType = dbProcess.PRO_M2M_TYPE(fromNumber, toNumber, log);
            if("".equalsIgnoreCase(m2mType))
            {
               count.incrementAndGet();
               callProM2MType(fromNumber, toNumber, log, count);
            }
            return m2mType;
        }

    }
    private void doStoreRating(Message<JsonObject> message) {
        CoreAgentRating r = new CoreAgentRating();
        JsonObject model = message.body().getObject("model");
        if (model == null) {
            container.logger().error("Missing param (model)");
        }
        r.setValues(model);
        Common.BuildLog log = new Common.BuildLog(container.logger(), r.user);
        int error = dbProcess.PRO_AGENT_RATING(r.agent, r.rating, r.user, r.content, r.type, log);

        log.add("insert core error", error);
        log.writeLog();
    }

    private void doSyncAgents() {

        mSettingsDb.getLong("LAST_AGENT_SYNC_TIME", new Handler<Long>() {
            @Override
            public void handle(Long aLong) {

                long last_changed_time = 0;
                if (aLong != null) {
                    last_changed_time = aLong;
                }
                if (last_changed_time == 0) {
                    last_changed_time = System.currentTimeMillis() - 90 * 24 * 60 * 60 * 1000; //15*24*60*60*1000;
                }

                ArrayList<JsonObject> arrayList = dbProcess.getAgentsData(last_changed_time, logger);
                if (arrayList == null || arrayList.size() == 0) return;

                final Queue<JsonObject> queue = new ArrayDeque<>();
                for (JsonObject o : arrayList) {
                    queue.add(o);
                }

                Fuck(queue, last_changed_time);

            }
        });
    }

    private void Fuck(final Queue<JsonObject> queue, final long last_changed_time) {

        if (queue == null || queue.size() == 0) {

            mSettingsDb.setLong("LAST_AGENT_SYNC_TIME", last_changed_time, new Handler<Boolean>() {
                @Override
                public void handle(Boolean abool) {

                    Common.BuildLog log = new Common.BuildLog(logger);
                    log.setPhoneNumber("agent_last_sync_time");
                    log.add("last time", last_changed_time);
                    log.add("last time VN", Misc.dateVNFormatWithTime(last_changed_time));
                    log.add("update result", abool);
                    log.writeLog();
                }
            });

            return;
        }
        final JsonObject jo = queue.poll();

        if (jo == null) {
            Fuck(queue, last_changed_time);
        }

        final int number = jo.getInteger(colName.PhoneDBCols.NUMBER);
        logger.info("0" + number + "  " + jo.encodePrettily());

        final boolean deleted = jo.getBoolean(colName.PhoneDBCols.DELETED, false);

        if (deleted) {
            mPhonesDb.removePhoneObj(number, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    Fuck(queue, last_changed_time);
                }
            });
        } else {
            mPhonesDb.update(number, jo, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {

                    long tmp = jo.getLong(colName.PhoneDBCols.LAST_UPDATE_TIME, 0);

                    long maxtime = Math.max(tmp, last_changed_time);
                    Fuck(queue, maxtime);

                    BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                    msg.setType(SoapProto.Broadcast.MsgType.FORCE_UPDATE_AGENT_INFO_VALUE);
                    msg.setSenderNumber(number);
                    msg.setExtra(jo);

                    vertx.eventBus().publish(Misc.getNumberBus(number), msg.getJsonObject());
                }
            });
        }
    }

    private void upsertPhoneInfo(final JsonObject o, final Common.BuildLog log) {



        /*mPhonesDb.updatePartial(o.getInteger(colName.PhoneDBCols.NUMBER), o, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {

                boolean result =(obj == null ? false : true);
                log.add("update result",result);
                log.writeLog();

                // gui lai thong tin cap nhat cho user vi luc nay da co thay doi thong tin cua user tren he thong
                if (obj != null) {
                    BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                    msg.setType(SoapProto.Broadcast.MsgType.FORCE_UPDATE_AGENT_INFO_VALUE);
                    msg.setSenderNumber(obj.number);
                    msg.setExtra(obj.toJsonObject());

                    vertx.eventBus().publish(Misc.getNumberBus(obj.number), msg.getJsonObject());
                }
            }
        });*/
    }

    private void doUpdateNapRutTanNoi(Message<JsonObject> message) {

        JsonObject json = message.body();

        String from_phone = "0" + DataUtil.strToInt(json.getString(NapRutTanNoi.FROM_NUMBER, "0"));
        String to_phone = "0" + DataUtil.strToInt(json.getString(NapRutTanNoi.TO_NUMBER, "0"));
        long amount = json.getLong(NapRutTanNoi.AMOUNT, 0);
        String m2m_type = json.getString(NapRutTanNoi.M2M_TYPE, "u2u");
        long tid = json.getLong(NapRutTanNoi.TID, 0);

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(from_phone);
        log.add("function", "doUpdateNapRutTanNoi");
        log.add("from number", from_phone);
        log.add("to number", to_phone);
        log.add("amount", amount);
        log.add("tid", tid);
        log.add("m2m type", m2m_type);

        List<String> GHN = dbProcess.GetGHNPersonList(log);
        if (GHN.size() == 0) {
            log.add("Khong lay duoc thong tin giao hang nhanh", "");
            log.writeLog();
            return;
        }

        String debitor = from_phone;   // thang giam tien
        String creditor = to_phone;     // thang tang tien

        //giao dich nap tien cho khach hang
        if (GHN.contains(debitor)) {
            dbProcess.PRO_UPDATE_NAPRUTTANNOI(debitor, creditor, System.currentTimeMillis(), tid, log);
        }
        //giao dich rut tien cho khach hang
        else if (GHN.contains(creditor)) {
            dbProcess.PRO_UPDATE_NAPRUTTANNOI(creditor, debitor, System.currentTimeMillis(), tid, log);
        } else {
            log.add("Khong thuoc nhom giao hang nhanh", "");
        }

        log.writeLog();

        json.putBoolean(colName.DepositInPlaceDBCols.RESULT, true);
        message.reply(json);
    }

    /*
        cap nhat thong tin dinh danh cho vi do diem giao dich thuc hien
     */
    private void doUdateNamed(Message<JsonObject> message) {

        JsonObject json = message.body();

        Common.BuildLog log = new Common.BuildLog(logger);

        String p_cusPhone = json.getString("cusPhone", "");
        String p_fullname = json.getString("cusName", "");
        String p_dob = json.getString("cusDob", "");
        String p_personalid = json.getString("cusId", "");
        String p_personaltype = json.getString("cusIdType", "");
        String p_email = json.getString("cusEmail", "");
        String p_address = json.getString("cusAddress", "");
        String p_ward = json.getString("cusWard", "");
        int p_districtid = json.getInteger("cusDisId", -1);
        String p_districtname = json.getString("cusDisName", "");
        int p_cityid = json.getInteger("cusCiId", -1);
        String p_cityname = json.getString("cusCiName", "");
        int p_status = json.getInteger("cusStatus", -1);

        //cap nhat du lieu co he thong phan phoi HTPP
        dbProcess.PROC_APPS_UPDATE_AI_DATA(p_cusPhone,
                p_fullname,
                p_dob,
                p_personalid,
                p_personaltype,
                p_email,
                p_address,
                p_ward,
                p_districtid,
                p_districtname,
                p_cityid,
                p_cityname,
                p_status,
                log);

        log.writeLog();

        json.putBoolean(colName.DepositInPlaceDBCols.RESULT, true);
        message.reply(json);
    }

    private void doGetLiquidity(Message<JsonObject> message) {

        Common.BuildLog log = new Common.BuildLog(logger);
        JsonObject json = message.body();
        JsonObject joReply = new JsonObject();
        String retailerNumber = json.getString("retailer", "");

        log.add("func", "doGetLiquidity");
        log.setPhoneNumber(retailerNumber);
        log.add("retailerNumber", retailerNumber);
        if ("".equalsIgnoreCase(retailerNumber)) {
            joReply.putNumber("error", -100);
            joReply.putString("caption", "");
            joReply.putString("content", "");
            message.reply(joReply);
            log.writeLog();
            return;
        }

        joReply = dbProcess.PRO_GET_CAPTION_SMS_LIQUIDITY(retailerNumber, log);
        message.reply(joReply);
        log.writeLog();
    }

    private void updateAllMMTAgent2Named() {
        ArrayList<String> arrayList = dbProcess.getAllMMTAgent(logger);

        for (String s : arrayList) {
            JsonObject json = new JsonObject();
            json.putBoolean(colName.PhoneDBCols.IS_NAMED, true);
            final int phoneNumber = DataUtil.strToInt(s);
            json.putNumber(colName.PhoneDBCols.NUMBER, phoneNumber);
            json.putBoolean(colName.PhoneDBCols.DELETED, false);

            logger.info(json.toString());

            mPhonesDb.updatePartialNoReturnObj(phoneNumber, json, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    logger.info("update phone object result: " + aBoolean);
                }
            });

        }
    }

    private void doDepositWithdraw(Message<JsonObject> message) {

        JsonObject json = message.body();

        int named = json.getInteger(colName.DepositInPlaceDBCols.IS_NAMED);
        String full_name = json.getString(colName.DepositInPlaceDBCols.FULL_NAME);
        String address = json.getString(colName.DepositInPlaceDBCols.ADDRESS);
        int service_type = json.getInteger(colName.DepositInPlaceDBCols.SERVICE_TYPE);
        int number = json.getInteger(colName.DepositInPlaceDBCols.NUMBER);
        long amount = json.getLong(colName.DepositInPlaceDBCols.AMOUNT);
        int from_source = json.getInteger(colName.DepositInPlaceDBCols.FROM_SOURCE);// Const.FromSource.APP_MOBI.ordinal();

        boolean result = dbProcess.MT_NAP_RUT_TAN_NOI_UPSERT(address
                , service_type
                , named
                , full_name
                , "0" + number
                , amount
                , from_source, "",
                logger);

        json.putBoolean(colName.DepositInPlaceDBCols.RESULT, result);
        message.reply(json);
    }

    private double ToDegree(String src) {

        if (src == null || src.equalsIgnoreCase("null")) return 0;
        double r;
        try {
            r = Double.valueOf(src);
        } catch (Exception e) {
            r = 0;
        }

        if (r > 0) {

            return r;
        }
        //106°53'49.45"E
        src = src.replace(sepDeg, replaced);
        src = src.replace(sepComma, replaced);
        src = src.replace(sepMin, replaced);
        src = src.replace(sepSec, replaced);
        String[] ar = src.split(replaced);

        String hour = ar.length >= 1 ? ar[0].trim() : "0";
        String min = ar.length >= 2 ? ar[1].trim() : "0";
        String sec = ar.length >= 3 ? ar[2].trim() : "0";
        try {
            r = Double.valueOf(hour) + (Double.valueOf(min) * 60 + Double.valueOf(sec)) / 3600;
        } catch (Exception ex) {
            r = 0;
        }
        return r;
    }

    public void  doStoresSync(Long tid) {
        logger.debug("LocSyncWithHTPP start at time " + Misc.dateVNFormat(new Date()));

        mSettingsDb.getLong("LAST_STORE_SYNC_TIME", new Handler<Long>() {
            public void handle(Long aLong) {
                //todo for init data only
                //aLong = 0L;

                long lastSyncTime = aLong;
                logger.debug("START SYNC STORE FROM " + lastSyncTime);

                //dang sync o luong truoc do
                if (isSyncMMT) {
                    return;
                }

                ArrayList<JsonObject> arrJ = dbProcess.getStoresData(aLong, logger);
                logger.info("getStoresData" + arrJ.size());
                //location list will be apply to mongo
                final ArrayList<JsonObject> arr = new ArrayList<>();

                //location list will be write to file
                final ArrayList<JsonObject> arrFlat = new ArrayList<>();

                //refined
                double lng;
                double lat;

                String sWrong = "";
                for (JsonObject o : arrJ) {
                    logger.info("Info object" + o);
                    boolean deleted = false;

                    if (o.getString(colName.AgentDBCols.DELETED).equalsIgnoreCase("1")) {
                        deleted = true;
                    }

                    boolean isValid;
                    lastSyncTime = Math.max(lastSyncTime, o.getLong(colName.AgentDBCols.LAST_UPDATE_TIME, 0));

                    lng = ToDegree(o.getString(colName.AgentDBCols.LONGITUDE));
                    lat = ToDegree(o.getString(colName.AgentDBCols.LATITUDE));
                    //---------------
                    AgentsDb.doUpdateNormalize(o);
                    //---------------

                    //kiem tra toa do
                    //dung lng &lat binh thuong
                    if (lat >= LAT_MIN
                            && lat <= LAT_MAX
                            && lng >= LNG_MIN
                            && lng <= LNG_MAX) {

                        //o.putNumber(colName.AgentDBCols.LONGITUDE, lng);
                        //o.putNumber(colName.AgentDBCols.LATITUDE, lat);

                        JsonObject jo = buildJO(o, lng, lat, deleted);
                        //jo.putArray(colName.AgentDBCols.KEY, key);
                        if (jo != null) {
                            arr.add(jo);
                        }
                        logger.info("valid store" + jo);
                        logger.info("size arr" + arr.size());
                        if (o.getString(colName.AgentDBCols.DELETED, "0").equalsIgnoreCase("0")) {
                            arrFlat.add(o);
                        }
                        logger.info("size arrFlat" + arrFlat.size());
                        isValid = true;

                    } else if (lng >= LAT_MIN
                            && lng <= LAT_MAX
                            && lat >= LNG_MIN
                            && lat <= LNG_MAX) { //
                        //isValid = false;
                        isValid = true;

                        //reset delete
                        deleted = false;

                        //o.putNumber(colName.AgentDBCols.LATITUDE, lng);
                        //o.putNumber(colName.AgentDBCols.LONGITUDE, lat);
                        if (o.getString(colName.AgentDBCols.DELETED).equalsIgnoreCase("1")) {
                            deleted = true;
                        }

                        JsonObject jo = buildJO(o, lat, lng, deleted);
                        if (jo != null) {
                            arr.add(jo);
                        }

                        if (o.getString(colName.AgentDBCols.DELETED, "0").equalsIgnoreCase("0")) {
                            arrFlat.add(o);
                        }

                    } else {
                        //lng and lat are invalid

                        isValid = true;
                        deleted = true;
                        JsonObject jo = buildJO(o, lng, lat, deleted);
                        //jo.putArray(colName.AgentDBCols.KEY, key);
                        if (jo != null) {
                            arr.add(jo);
                        }
                        logger.info("invalid store" + jo);
                        logger.info("size arr" + arr.size());
                        if (o.getString(colName.AgentDBCols.DELETED, "0").equalsIgnoreCase("0")) {
                            arrFlat.add(o);
                        }
                        logger.info("size arrFlat" + arrFlat.size());
                    }

                    //kiem tra momo
                    String momophone = o.getString(colName.AgentDBCols.MOMO_PHONE, "");
                    logger.info("momophone" + momophone);
                    if (momophone == null || "null".equalsIgnoreCase(momophone) || "".equalsIgnoreCase(momophone)) {
                        isValid = false;
                    }

                    if (!isValid) {
                        logger.debug("lng,lat " + lng + "," + lat);
                        sWrong += "TEN DAI DIEN: " + o.getString(colName.AgentDBCols.NAME) + ";"
                                + " SDT: " + o.getString(colName.AgentDBCols.PHONE) + ";"
                                + " TEN CUA HANG: " + o.getString(colName.AgentDBCols.STORE_NAME) + ";"
                                + " LONGITUDE: " + lng + ";"
                                + " LATITUDE: " + lat + ";"
                                + " MOMOPHONE: " + momophone + ";"
                                + " ADDRESS: " + o.getString(colName.AgentDBCols.ADDRESS) + ";"
                                + " STREET: " + o.getString(colName.AgentDBCols.STREET) + ";"
                                + " WARD: " + o.getString(colName.AgentDBCols.WARD) + ";"
                                + " DISTRICTID: " + o.getString(colName.AgentDBCols.DISTRICT_ID) + ";"
                                + " CITYID: " + o.getString(colName.AgentDBCols.CITY_ID)
                                + "\n";

                    }
                }
                String s = "";
                for (int f = 0; f < arrFlat.size(); f++) {
                    logger.debug("Momophone: " + arrFlat.get(f).getString(colName.AgentDBCols.MOMO_PHONE)
                                    + " lng: " + DataUtil.getDouble(arrFlat.get(f).getString(colName.AgentDBCols.LONGITUDE))
                                    + " lat: " + DataUtil.getDouble(arrFlat.get(f).getString(colName.AgentDBCols.LATITUDE))
                    );

                    s += getString(arrFlat.get(f)) + "\n";
                }

                //todo : doan nay luu file de xuat cho app
                /*BufferedWriter writer = null;
                try {

                    //create a temporary file
                    java.io.File logFile = new java.io.File("/home/concu/wrong_store.txt");
                    writer = new BufferedWriter(new FileWriter(logFile));
                    writer.write(sWrong);
                    writer.flush();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        // Close the writer regardless of what happens...
                        writer.close();
                    } catch (Exception e) {
                    }
                }*/

                Queue<JsonObject> queueAgents = new ArrayDeque<>();
                if (arr != null && arr.size() > 0) {
                    for (int i = 0; i < arr.size(); i++) {
                        queueAgents.add(arr.get(i));
                        logger.info(arr.get(i));
                    }
                }
                logger.info(queueAgents);
                if (queueAgents != null && queueAgents.size() > 0) {
                    synchronized (lckMMT) {
                        isSyncMMT = true;
                    }
                    logger.info("lastSyncTime" + lastSyncTime);
                    upSertMMTAgent(queueAgents, lastSyncTime);
                }
            }
        });
    }

    private void upSertMMTAgent(final Queue<JsonObject> queueAgents, final long lastSyncTime) {

        if (queueAgents == null || queueAgents.size() == 0) {
            synchronized (lckMMT) {
                isSyncMMT = false;
            }

            //cap nhat DB last sync time
            mSettingsDb.setLong("LAST_STORE_SYNC_TIME", lastSyncTime, new Handler<Boolean>() {
                @Override
                public void handle(Boolean event) {
                    logger.info("SYNC STORE DONE " + lastSyncTime);
                }
            });
            return;
        }

        final JsonObject item = queueAgents.poll();
        if (item == null) {
            upSertMMTAgent(queueAgents, lastSyncTime);
        } else {

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(item.getString(colName.AgentDBCols.MOMO_PHONE));
            log.add("last rowid", item.getInteger(colName.AgentDBCols.ROW_CORE_ID, 0));
            log.add("store name", item.getString(colName.AgentDBCols.STORE_NAME, ""));
            log.add("name", item.getString(colName.AgentDBCols.NAME, ""));
            log.add("phone", item.getString(colName.AgentDBCols.PHONE, ""));
            log.add("agentType", item.getInteger(colName.AgentDBCols.AGENT_TYPE, 0));
            log.add("json upsert", "...");
            final int status = item.getInteger(colName.AgentDBCols.STATUS, -1);
            final boolean del = item.getBoolean(colName.AgentDBCols.DELETED, false);
            log.add("del", del);
            log.add("status", status);

            final long lastTimeItem = item.getLong(colName.AgentDBCols.LAST_UPDATE_TIME, 0);

            agentsDb.upsertLocation(item, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {

                    long lastTimeGreater = Math.max(lastSyncTime, lastTimeItem);

                    long phone = DataUtil.stringToVnPhoneNumber(item.getString(colName.AgentDBCols.MOMO_PHONE));

                    if (phone > 0) {
                        int phoneNumber = DataUtil.strToInt(phone + "");
                        //cap nhat isAgent tren bang phones
                        JsonObject upPhone = new JsonObject();
                        upPhone.putNumber(colName.PhoneDBCols.NUMBER, phoneNumber);
                        boolean isAgent = (status == 2 || status == 1) ? false : true;
                        upPhone.putBoolean(colName.PhoneDBCols.IS_AGENT, isAgent);
                        //boolean isDel = del;
                        upPhone.putBoolean(colName.PhoneDBCols.DELETED, false);

                        mPhonesDb.updatePartial(phoneNumber, upPhone, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj obj) {

                            }
                        });

                    }
                    upSertMMTAgent(queueAgents, lastTimeGreater);
                    log.add("isUpdated", aBoolean);
                    log.writeLog();
                }
            });
        }
    }

    public String getString(JsonObject o) {
        return
                "INSERT INTO tSTORES(" +
                        "OWNER," +
                        "PHONE," +
                        "ADDRESS," +
                        "STREET," +
                        "STREET_NORM," +
                        "WARD," +
                        "WARD_NORM," +
                        "NAME," +
                        "NAME_NORM," +
                        "LNG," +
                        "LAT," +
                        "CITY_ID," +
                        "DISTRICT_ID," +
                        "AREA_ID," +
                        "CID," +
                        "LAST_UPDATE," +
                        "MOMO_PHONE," +
                        "DELETED) VALUES ('" +
                        o.getString(colName.AgentDBCols.NAME) + "',"
                        + o.getString(colName.AgentDBCols.PHONE) + ",'"
                        + o.getString(colName.AgentDBCols.ADDRESS) + "','"
                        + o.getString(colName.AgentDBCols.STREET) + "','"
                        + Misc.removeAccent(o.getString(colName.AgentDBCols.STREET)).toLowerCase() + "','"
                        + o.getString(colName.AgentDBCols.WARD) + "','"
                        + Misc.removeAccent(o.getString(colName.AgentDBCols.WARD)).toLowerCase().replace(":", "") + "','"
                        + o.getString(colName.AgentDBCols.STORE_NAME) + "','"
                        + Misc.removeAccent(o.getString(colName.AgentDBCols.STORE_NAME)).toLowerCase() + "',"
                        + o.getString(colName.AgentDBCols.LONGITUDE) + ","
                        + o.getString(colName.AgentDBCols.LATITUDE) + ","
                        + o.getString(colName.AgentDBCols.CITY_ID) + ","
                        + o.getString(colName.AgentDBCols.DISTRICT_ID) + ","
                        + o.getString(colName.AgentDBCols.AREA_ID) + ","
                        + o.getString(colName.AgentDBCols.ROW_CORE_ID) + ","
                        + o.getLong(colName.AgentDBCols.LAST_UPDATE_TIME) + ","
                        + "'" + o.getString(colName.AgentDBCols.MOMO_PHONE) + "',"
                        + o.getString(colName.AgentDBCols.DELETED) + ");";
    }

    private JsonObject buildJO(JsonObject jo, double lng, double lat, boolean deleted) {
        JsonObject j = null;
        try {
            j = new JsonObject();
            j.putString(colName.AgentDBCols.NAME, jo.getString(colName.AgentDBCols.NAME, ""));
            j.putString(colName.AgentDBCols.PHONE, jo.getString(colName.AgentDBCols.PHONE, ""));
            j.putString(colName.AgentDBCols.STORE_NAME, jo.getString(colName.AgentDBCols.STORE_NAME, ""));
            j.putNumber(colName.AgentDBCols.ROW_CORE_ID, Integer.parseInt(jo.getString(colName.AgentDBCols.ROW_CORE_ID, "0")));

            JsonArray pntArr = new JsonArray();
            pntArr.add(lng)
                    .add(lat);

            JsonObject cor = new JsonObject();

            cor.putString("type", "Point");
            cor.putArray("coordinates", pntArr);

            j.putObject(colName.AgentDBCols.LOCATION, cor);
            j.putString(colName.AgentDBCols.ADDRESS, jo.getString(colName.AgentDBCols.ADDRESS, ""));
            j.putString(colName.AgentDBCols.STREET, jo.getString(colName.AgentDBCols.STREET, ""));
            j.putString(colName.AgentDBCols.WARD, jo.getString(colName.AgentDBCols.WARD, ""));
            j.putNumber(colName.AgentDBCols.DISTRICT_ID, Integer.parseInt(jo.getString(colName.AgentDBCols.DISTRICT_ID, "0")));
            j.putNumber(colName.AgentDBCols.CITY_ID, Integer.parseInt(jo.getString(colName.AgentDBCols.CITY_ID, "0")));
            j.putNumber(colName.AgentDBCols.AREA_ID, Integer.parseInt(jo.getString(colName.AgentDBCols.AREA_ID, "0")));
            j.putNumber(colName.AgentDBCols.ROW_ID, 0);
            j.putNumber(colName.AgentDBCols.LAST_UPDATE_TIME, jo.getLong(colName.AgentDBCols.LAST_UPDATE_TIME, 0));
            j.putBoolean(colName.AgentDBCols.DELETED, deleted);
            j.putString(colName.AgentDBCols.MOMO_PHONE, jo.getString(colName.AgentDBCols.MOMO_PHONE, ""));
            j.putNumber(colName.AgentDBCols.STATUS, jo.getInteger(colName.AgentDBCols.STATUS, 0));

            j.putString(colName.AgentDBCols.WARD_NAME, jo.getString(colName.AgentDBCols.WARD_NAME, ""));
            j.putString(colName.AgentDBCols.DISTRICT_NAME, jo.getString(colName.AgentDBCols.DISTRICT_NAME, ""));
            j.putString(colName.AgentDBCols.CITY_NAME, jo.getString(colName.AgentDBCols.CITY_NAME, ""));
            j.putNumber(colName.AgentDBCols.AGENT_TYPE, jo.getInteger(colName.AgentDBCols.AGENT_TYPE, 0));
            j.putBoolean(colName.AgentDBCols.TDL, jo.getString(colName.AgentDBCols.TDL, "0").equalsIgnoreCase("1"));
            j.putNumber(colName.AgentDBCols.ACTIVE_DATE, jo.getLong(colName.AgentDBCols.ACTIVE_DATE, 0));
        } catch (Exception ex) {

            logger.info(jo.toString());
        }

        return j;
    }

//    private void syncTrans() throws Exception {
//
//        if (mIsProcess.compareAndSet(false, true)) {
//            //get the last time we sync
//            mSettingsDb.getLong("LAST_TRANS_SYNC_TIME", new Handler<Long>() {
//                @Override
//                public void handle(Long aLong) {
//                    long last_sync_time = 0;
//                    if (aLong != null) {
//                        last_sync_time = aLong;
//                    }
//
//                    if (last_sync_time == 0) {
//                        last_sync_time = System.currentTimeMillis() - 30 * 60 * 1000; //15*24*60*60*1000;
//                    }
//
//                    final Common.BuildLog log = new Common.BuildLog(logger);
//                    log.setPhoneNumber("synctran");
//
//                    Connection conn = mSyncPool.getConnect();
//                    CallableStatement cs;
//                    final boolean checkStoreApp = glbCfg.getBoolean(StringConstUtil.CHECK_STORE_APP, false);
//
//
//                    final ResultSet res;
//                    if (conn != null) {
//
//                        final ArrayList<String> GHNList = new ArrayList<String>();// dbProcess.GetGHNPersonList(log);
//
//                        try {
//                            Timestamp date = new Timestamp(last_sync_time);
//                            log.add("Begin sync from core for", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
////                            log.add("call store procedure", "{call UMARKET2BACKEND(?,?)}");
//
//                            cs = conn.prepareCall("{call UMARKET2BACKEND(?,?)}");
//
//                            cs.setTimestamp(1, date);
//                            cs.registerOutParameter(2, OracleTypes.CURSOR);
//                            cs.execute();
//                            res = (ResultSet) cs.getObject(2);
//
//                            while (res.next()) {
//                                final Long ID = nullToDefault(res.getLong("ID"), 0L);                                 // TID
//                                final String TYPE = nullToDefault(res.getString("TYPE"), "");                         // TYPE
//                                final String INITIATOR = nullToDefault(res.getString("INITIATOR"), "");               // SO DIEN THOAI NGUOI TAO
//                                final String CREDITOR = nullToDefault(res.getString("CREDITOR"), "").trim();          // SO DIEN THOAI NGUOI DUOC TANG TIEN
//                                final String CREDITOR_NAME = nullToDefault(res.getString("CREDITORNAME"), CREDITOR);  // TEN NGUOI DUOC TANG TIEN
//                                final String DEBITOR = nullToDefault(res.getString("DEBITOR"), "").trim();            // SO DIEN THOAI NGUOI BI GIAM TIEN
//                                final String DEBITOR_NAME = nullToDefault(res.getString("DEBITORNAME"), DEBITOR);     // TEN NGUOI BI GIAM TIEN
//                                final long AMOUNT = nullToDefault(res.getLong("AMOUNT"), 0L);                         // AMOUNT
//                                final String RECIPIENT = nullToDefault(res.getString("RECIPIENT"), "");               // SO DIEN THOAI DUOC TOPUP
//                                final long LAST_MODIFIED = nullToDefault(res.getTimestamp("LAST_MODIFIED"), 0L);      // LAST_MODIFIED
//                                final int WALLETTYPE = nullToDefault(res.getInt("wallettype"), 0);                    // WALLETTYPE
//                                final String PARENTTYPE = nullToDefault(res.getString("parentType"), "");
//                                final String IS_AGENT = nullToDefault(res.getString("isAgent"), ""); // 1: agent, 0: enduser
//                                final String PARENT_ID = nullToDefault(res.getString("parentId"), "");
//                                String ADJ_DESC_TMP = "";
//                                String NOTI_CONTENT_TMP = "";
//                                String NOTI_CAPTION_TMP = "";
//                                String ALIAS_NAME_TMP = "";
//                                String HIS_CONTENT_TMP = "";
//                                try{
//                                    NOTI_CONTENT_TMP = nullToDefault(res.getString("noti_content"), "");
//                                    NOTI_CAPTION_TMP = nullToDefault(res.getString("noti_caption"), "");
//                                    ALIAS_NAME_TMP = nullToDefault(res.getString("alias_name"), "");
//                                    HIS_CONTENT_TMP = nullToDefault(res.getString("his_content"), "");
//                                    ADJ_DESC_TMP = nullToDefault(res.getString("adj_desc"), "");
//                                }catch (Exception e)
//                                {
//                                    ADJ_DESC_TMP = "";
//                                }
//                                time_sync = System.currentTimeMillis();
////                                log.add("noti content", NOTI_CONTENT_TMP);
////                                log.add("noti_caption", NOTI_CAPTION_TMP);
//                                logger.info("phone :" + DEBITOR
//                                        + ", TYPE : " + TYPE
//                                        + ", PARENTTYPE : " + PARENTTYPE);
//                                final String ALIAS_NAME = ALIAS_NAME_TMP;
//                                //final String HIS_CONTENT = "".equalsIgnoreCase(ADJ_DESC_TMP) ? HIS_CONTENT_TMP : new String(DatatypeConverter.parseBase64Binary(HIS_CONTENT_TMP));
//                                final String HIS_CONTENT = "".equalsIgnoreCase(HIS_CONTENT_TMP) ? HIS_CONTENT_TMP : new String(DatatypeConverter.parseBase64Binary(HIS_CONTENT_TMP));
//                                //nam trong danh sach loai bo
//                                if (ExcludeCREDITOR.contains(CREDITOR)) {
//                                    log.add("exclude creditor", CREDITOR);
//                                    continue;
//                                }
//
//                                //nam trong danh sach loai bo
//                                if (ExcludeDEBITOR.contains(DEBITOR)) {
//                                    log.add("exclude debitor", DEBITOR);
//                                    continue;
//                                }
//
//                                if(specialListAgent.contains(DEBITOR)) {
//                                    JsonObject joInfoExtra = new JsonObject();
//                                    joInfoExtra.putString(StringConstUtil.NUMBER, CREDITOR);
//                                    joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, ID);
//                                    joInfoExtra.putNumber(StringConstUtil.AMOUNT, AMOUNT);
//                                    joInfoExtra.putString(StringConstUtil.SERVICE_ID, "trabu");
//                                    joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_TYPE, MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
//                                    promotionProcess.excuteAcquireBinhTanUserPromotion(CREDITOR, log, null, null, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joInfoExtra);
//                                }
//
//
//                                log.add("begin syn for tid : " + ID + " type : " + TYPE + " amount : " + AMOUNT + " creditor : " + CREDITOR + " debitor : " + DEBITOR + " wallettype " + WALLETTYPE,
//                                "noti content" + NOTI_CONTENT_TMP);
//
//                                last_sync_time = Math.max(last_sync_time, LAST_MODIFIED);
//
//                                final TransDb.TranObj mainObj = new TransDb.TranObj();
//                                mainObj.tranId = ID;
//                                mainObj.clientTime = LAST_MODIFIED;
//                                mainObj.ackTime = LAST_MODIFIED;
//                                mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
//                                mainObj.amount = AMOUNT;
//                                mainObj.status = TranObj.STATUS_OK;
//                                mainObj.error = 0;
//                                mainObj.cmdId = -1;
//                                mainObj.billId = "-1";
//                                mainObj.io = -1;
//                                mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
//                                boolean isMainValid = false;
//
//                                final TransDb.TranObj rcvObj = new TransDb.TranObj();
//                                rcvObj.tranId = ID;
//                                rcvObj.clientTime = LAST_MODIFIED;
//                                rcvObj.ackTime = LAST_MODIFIED;
//                                rcvObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
//                                rcvObj.amount = AMOUNT;
//                                rcvObj.status = TranObj.STATUS_OK;
//                                rcvObj.error = 0;
//                                rcvObj.cmdId = -1;
//                                rcvObj.billId = "-1";
//                                rcvObj.io = 1;
//                                rcvObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
//                                boolean isRecvValid = false;
//
////                                log.add("tran type", TYPE);
////                                log.add("wallet type", WALLETTYPE);
////                                log.add("isAgent", IS_AGENT);
//                                log.add("ParentId", PARENT_ID);
////                                log.add("his content", HIS_CONTENT);
//                                //todo build trans object than, upsert --> topup
//                                if ("buy".equalsIgnoreCase(TYPE)) {
//
//                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
//                                    if (mainObj.owner_number > 0) {
//
//                                        log.add("debitor number", "0" + mainObj.owner_number);
////                                        log.add("debitor name", DEBITOR_NAME);
//
//                                        mainObj.owner_name = DEBITOR_NAME;
//                                        mainObj.io = -1;
//                                        mainObj.category = -1;
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.TOP_UP_VALUE;
//
//                                        if (RECIPIENT != null) {
//                                            mainObj.partnerId = DataUtil.getPhoneNumberProviderInVN(RECIPIENT);
//                                            mainObj.parterCode = RECIPIENT;
//                                            mainObj.partnerName = "";
//                                        }
//
//                                        isMainValid = true;
//                                    }
//                                }   //END TYPE = buy
//                                else if ("transfer".equalsIgnoreCase(TYPE)) {
//
//                                    log.add("CREDITOR/DEBITOR", CREDITOR + "/" + DEBITOR);
//
//                                    if (GHNList.contains(CREDITOR)) {
//
//                                        log.add("GHNList contains CREDITOR", CREDITOR);
//
//                                        dbProcess.PRO_UPDATE_NAPRUTTANNOI(CREDITOR, DEBITOR, LAST_MODIFIED, ID, log);
//
//                                    } else if (GHNList.contains(DEBITOR)) {
//
//                                        log.add("GHNList contains DEBITOR", DEBITOR);
//
//                                        dbProcess.PRO_UPDATE_NAPRUTTANNOI(DEBITOR, CREDITOR, LAST_MODIFIED, ID, log);
//                                    } else {
////                                        log.add("GHNList contains: khong chua CREDITOR: " + CREDITOR + " va DEBITOR " + DEBITOR, "");
//
//                                    }
//                                    //chuyen tien M2M
//
//                                    //nguoi gui
//                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
//                                    if (mainObj.owner_number > 0) {
//
//                                        log.add("debitor number", "0" + mainObj.owner_number);
////                                        log.add("debitor name", DEBITOR_NAME);
//
//                                        mainObj.io = -1;
//                                        mainObj.owner_name = DEBITOR_NAME;
//                                        mainObj.billId = "-1";
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
//                                        mainObj.comment = "Chuyển tiền từ tài khoản MoMo đến tài khoản MoMo";
//                                        mainObj.partnerName = CREDITOR_NAME;
//                                        mainObj.category = 8;
//                                        mainObj.partnerId = CREDITOR;
//
//                                        isMainValid = true;
//                                    }
//
//                                    //nguoi nhan
//                                    rcvObj.owner_number = DataUtil.strToInt(CREDITOR);
//                                    if (rcvObj.owner_number > 0) {
//                                        log.add("creditor number", "0" + rcvObj.owner_number);
////                                        log.add("creditor name", CREDITOR);
////                                        log.add("Alias name", ALIAS_NAME);
////                                        log.add("his content", HIS_CONTENT);
//
//                                        rcvObj.owner_name = CREDITOR_NAME;
//                                        rcvObj.io = 1;
//                                        rcvObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
//                                        rcvObj.billId = "-1";
//                                        if(!"".equalsIgnoreCase(HIS_CONTENT)){
//                                            rcvObj.comment = HIS_CONTENT;
//                                        }
//
//                                        if ("123pay_promo".equalsIgnoreCase(DEBITOR)) {
//                                            rcvObj.partnerName = "M_Service";
//                                            rcvObj.partnerId = "Hoàn phí";
//                                        } else {
//                                            rcvObj.partnerName = "".equalsIgnoreCase(ALIAS_NAME) ? DEBITOR_NAME : ALIAS_NAME;
//                                            rcvObj.partnerId = DEBITOR;
//                                        }
//
//                                        isRecvValid = true;
//                                    }
//
//                                } //END TYPE = transfer
//                                else if ("billpay".equalsIgnoreCase(TYPE)) {
//
//                                    //thanh toan hoa don
//                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
//                                    if (mainObj.owner_number > 0) {
//
//                                        log.add("debitor number", "0" + mainObj.owner_number);
////                                        log.add("debitor name", DEBITOR_NAME);
//
//                                        mainObj.io = -1;
//                                        mainObj.owner_name = DEBITOR_NAME;
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
//                                        mainObj.partnerId = CREDITOR; // todo
//                                        mainObj.partnerName = CREDITOR_NAME; // todo
//                                        mainObj.parterCode = ""; // todo ten chu hoa don
//                                        mainObj.billId = RECIPIENT; // todo
//
//                                        isMainValid = true;
//                                    }
//
//                                } //END Type = billpay
//                                else if ("bankcashin".equalsIgnoreCase(TYPE)) {
//
//                                    //nap tien tu ngan hang lien ket
//                                    mainObj.owner_number = DataUtil.strToInt(CREDITOR);
//                                    if (mainObj.owner_number > 0) {
//
//                                        log.add("creditor number", "0" + mainObj.owner_number);
////                                        log.add("creditor name", DEBITOR_NAME);
//
//                                        mainObj.owner_name = CREDITOR_NAME;
//                                        mainObj.io = 1;
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BANK_IN_VALUE;
//                                        mainObj.partnerId = "Ngân hàng liên kết";
//                                        mainObj.parterCode = getBankCode(DEBITOR);
//                                        mainObj.partnerName = DEBITOR_NAME;
//                                        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE;
//                                        isMainValid = true;
//                                    }
//                                }  //END TYPE = BANKIN
//                                else if ("bankcashout".equalsIgnoreCase(TYPE)) {
//
//                                    //chuyen tien ra ngan hang
//                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
//                                    if (mainObj.owner_number > 0) {
//
//                                        log.add("debitor number", "0" + mainObj.owner_number);
////                                        log.add("debitor name", DEBITOR_NAME);
//
//                                        mainObj.owner_name = DEBITOR_NAME;
//                                        mainObj.io = -1;
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BANK_OUT_VALUE;
//                                        mainObj.partnerId = "Ngân hàng liên kết";
//                                        mainObj.parterCode = getBankCode(CREDITOR);
//                                        mainObj.partnerName = CREDITOR_NAME;
//                                        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE;
//                                        isMainValid = true;
//                                    }
//                                }   //END TYPE = bankout
//                                else if ("adjustment".equalsIgnoreCase(TYPE)) {
//
//                                    //nguoi gui
//                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
//                                    if (mainObj.owner_number > 0) {
//
//                                        log.add("debitor number", "0" + mainObj.owner_number);
////                                        log.add("debitor name", DEBITOR_NAME);
//                                        String pid = CREDITOR;
//                                        if(bankMap.containsKey(CREDITOR))
//                                        {
//                                            pid = bankMap.get(CREDITOR);
//                                        }
//                                        mainObj.owner_name = DEBITOR_NAME;
//                                        mainObj.billId = "-1";
//                                        mainObj.io = -1;
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
//                                        mainObj.comment = "Chuyển tiền từ tài khoản MoMo đến tài khoản MoMo";
//                                        mainObj.partnerName = CREDITOR_NAME;
//                                        mainObj.category = 8;
//                                        //mainObj.partnerId = CREDITOR;
//                                        mainObj.partnerId = pid;
//                                        isMainValid = true;
//                                    }
//
//                                    //nguoi nhan
//                                    rcvObj.owner_number = DataUtil.strToInt(CREDITOR);
//                                    if (rcvObj.owner_number > 0) {
//                                        if(!"".equalsIgnoreCase(HIS_CONTENT)){
//                                            rcvObj.comment = HIS_CONTENT;
//                                        }
//                                        log.add("creditor number", "0" + rcvObj.owner_number);
////                                        log.add("creditor name", CREDITOR_NAME);
//                                        String rid = DEBITOR;
//                                        if(bankMap.containsKey(DEBITOR))
//                                        {
//                                            rid = bankMap.get(DEBITOR);
//                                        }
//                                        rcvObj.owner_name = CREDITOR_NAME;
//                                        rcvObj.io = 1;
//                                        rcvObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
//                                        rcvObj.billId = "-1";
//                                        rcvObj.partnerName = DEBITOR_NAME;
//                                        //rcvObj.partnerId = DEBITOR;
//                                        rcvObj.partnerId = rid;
//
//                                        //giao dich hoan tien goc bankoutmanual
//                                        if (bankoutmanual.equalsIgnoreCase(DEBITOR.trim())) {
//                                            rcvObj.comment = "Hoàn tiền rút tiền về ngân hàng";
//                                        }
//
//                                        //hoan phi chuyen tien bankout manual
//                                        if (bankoutmanualfee.equalsIgnoreCase(DEBITOR)) {
//                                            rcvObj.comment = "Hoàn phí rút tiền về ngân hàng";
//                                        }
//
//                                        isRecvValid = true;
//
//                                        /*//khong cap nhat cac giao dich hoan tra tien tu bankout manual va bankout manual fee
//                                        if(bankoutmanual.equalsIgnoreCase(DEBITOR) || bankoutmanualfee.equalsIgnoreCase(DEBITOR)){
//                                                isRecvValid =false;
//                                        }else{
//                                            isRecvValid = true;
//                                        }*/
//                                    }
//
//                                    //wallettype: 1 momo: 2 mload, 3: point, 4:voucher
//                                }   //END TYPE = adjust
//                                else if (("bonus".equalsIgnoreCase(TYPE))) {
//                                    mainObj.owner_number = DataUtil.strToInt(CREDITOR);
//                                    log.add("creditor number", "0" + mainObj.owner_number);
////                                    log.add("creditor name", CREDITOR_NAME);
//
//                                    mainObj.owner_name = CREDITOR_NAME;
//                                    //mainObj.owner_number = Integer.parseInt(CREDITOR);
//                                    mainObj.io = 1;
//                                    mainObj.tranType = MomoProto.TranHisV1.TranType.BONUS_VALUE;
//                                    if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BONUS_DGD_VALUE;
//                                    }
////                                    log.add("checkStoreApp --->", IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE));
////                                    log.add("mainObj.tranType --->", mainObj.tranType);
//
//                                    //tien hoa hong tra theo point
////                                    //neu la dien giao dich tra theo momo
//                                    JsonObject note = new JsonObject();
//                                    if ((WALLETTYPE == WalletType.MOMO || WALLETTYPE == WalletType.POINT)
//                                            && mainObj.owner_number > 0) {
////                                        mainObj.comment = "Bạn nhận được " + AMOUNT + "đ tiền hoa hồng của dịch vụ.";
//                                        if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
//                                            mainObj.comment = "Bạn nhận được " + Misc.formatAmount(AMOUNT).replaceAll(",", ".") + "đ tiền thù lao của dịch vụ.";
//                                            if ("".equalsIgnoreCase(mainObj.partnerId)) {
//                                                mainObj.partnerId = "Tiền thù lao";
//                                            }
//                                        } else {
//                                            if ("promotion".equalsIgnoreCase(DEBITOR) && mainObj.owner_number > 0) {
//                                                mainObj.comment = StringConstUtil.BONUS_300_CONTENT;
//                                                note.putBoolean(StringConstUtil.KEY, true);
//                                            } else {
//                                                mainObj.comment = "Bạn nhận được " + Misc.formatAmount(AMOUNT).replaceAll(",", ".") + "đ tiền hoa hồng của dịch vụ.";
//                                                note.putBoolean(StringConstUtil.KEY, false);
//                                            }
//                                            JsonArray jsonArray = new JsonArray();
//                                            jsonArray.add(note);
//                                            mainObj.share = jsonArray;
//
//                                        }
//                                        isMainValid = true;
//                                    }
//
//                                    //hoa hong gui tien
//                                    if ("c2ccomin".equalsIgnoreCase(DEBITOR) && mainObj.owner_number > 0) {
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.C2C_BONUS_SEND_VALUE;
////                                        mainObj.comment = "Bạn nhận được "
////                                                    + Misc.formatAmount(AMOUNT).replaceAll(",",".")
////                                                    + "đ hoa hồng gửi tiền.";
//                                        if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
//                                            mainObj.comment = "Bạn nhận được "
//                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
//                                                    + "đ thù lao gửi tiền.";
//                                        } else {
//                                            mainObj.comment = "Bạn nhận được "
//                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
//                                                    + "đ hoa hồng gửi tiền.";
//                                        }
//
//                                        isMainValid = true;
//
//                                    }
//
//                                    //hoa hong nhan tien
//                                    if ("c2ccomout".equalsIgnoreCase(DEBITOR) && mainObj.owner_number > 0) {
//
//                                        //reset tran type
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.C2C_BONUS_RECEIVE_VALUE;
////                                        mainObj.comment = "Bạn nhận được "
////                                                    + Misc.formatAmount(AMOUNT).replaceAll(",",".")
////                                                    + "đ hoa hồng nhận tiền.";
//                                        if (IS_AGENT.equalsIgnoreCase(StringConstUtil.CORE_STORE_VALUE)) {
//                                            mainObj.comment = "Bạn nhận được "
//                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
//                                                    + "đ thù lao nhận tiền.";
//
//                                        } else {
//                                            mainObj.comment = "Bạn nhận được "
//                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
//                                                    + "đ hoa hồng nhận tiền.";
//
//                                        }
//
//                                        isMainValid = true;
//
//                                    }
//
//                                } // END BONUS
//                                else if ("fee".equalsIgnoreCase(TYPE)) {
//                                    //khuyen mai
//                                    mainObj.owner_number = DataUtil.strToInt(DEBITOR);
//                                    if (mainObj.owner_number > 0) {
//
//                                        log.add("debitor number", "0" + mainObj.owner_number);
//                                        log.add("debitor name", DEBITOR_NAME);
//
//                                        mainObj.owner_name = DEBITOR_NAME;
//                                        mainObj.io = -1;
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.FEE_VALUE;
//                                        mainObj.amount = AMOUNT;
//
//                                        //parentType = nap,rut,gui,nhan,u2u,ag2ag
//                                        //fee rut tien tai diem giao dich
//                                        if ("rut".equalsIgnoreCase(PARENTTYPE)) {
//                                            mainObj.comment = "Bạn đã bị trừ "
//                                                    + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
//                                                    + "đ - phí rút tiền tại điểm giao dịch.";
//                                            isMainValid = true;
//                                        } else {
//                                            if (AMOUNT == 1000) {
//                                                mainObj.comment = "Phí dịch vụ chuyển tiền";
//                                                isMainValid = false;
//                                            } else {
//                                                isMainValid = true;
//                                                mainObj.comment = "Bạn đã bị trừ "
//                                                        + Misc.formatAmount(AMOUNT).replaceAll(",", ".")
//                                                        + "đ tiền phí dịch vụ.";
//                                            }
//                                        }
//                                    }
//                                } //END TYPE = fee
//
//                                TransDb tranDb = new TransDb(vertx, vertx.eventBus(), logger);
//
//                                final long fAmount = AMOUNT;
//                                final String fType = TYPE;
//                                final String fDebitor = DEBITOR;
//                                final String NOTI_CONTENT = "".equalsIgnoreCase(NOTI_CONTENT_TMP) ? NOTI_CONTENT_TMP : new String(DatatypeConverter.parseBase64Binary(NOTI_CONTENT_TMP));
//                                final String NOTI_CAPTION = "".equalsIgnoreCase(NOTI_CAPTION_TMP) ? NOTI_CAPTION_TMP : new String(DatatypeConverter.parseBase64Binary(NOTI_CAPTION_TMP));;
//                                log.add("NOTI_CONTENT DECODE", NOTI_CONTENT);
//                                log.add("NOTI_CAPTION DECODE", NOTI_CAPTION);
//                                //Check dk noti
//                                final JsonArray jsonListTurnOffNotiNumber = joTurnOffNoti.getArray(StringConstUtil.TurningOffNotification.LIST_NUMBER, new JsonArray());
//                                final JsonArray jsonListTurnOffAgent = joTurnOffNoti.getArray(StringConstUtil.TurningOffNotification.LIST_AGENT, new JsonArray());
//                                final boolean turnOffNoti = joTurnOffNoti.getBoolean(StringConstUtil.TurningOffNotification.IS_ACTIVE, false);
//                                //BEGIN isMainValid
//                                if (isMainValid && turnOffNoti && jsonListTurnOffAgent.contains(CREDITOR))
//                                {
//                                    log.add("desc", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
////                                    log.writeLog();
//                                }
//                                else if (isMainValid) {
//
//                                    final Common.BuildLog tranSndLog = new Common.BuildLog(logger);
//                                    tranSndLog.setPhoneNumber("0" + mainObj.owner_number);
//                                    tranSndLog.add("Res", res);
//                                    tranSndLog.add("this is tran for sender", "");
//                                    tranSndLog.add("isMainValid", isMainValid);
//                                    tranSndLog.add("Json tran will be upsert", mainObj.getJSON());
////                                    NOTI_CONTENT_TMP = nullToDefault(res.getString("noti_content"), "");
////                                    NOTI_CAPTION_TMP = nullToDefault(res.getString("noti_caption"), "");
////                                    ALIAS_NAME = nullToDefault(res.getString("alias_name"), "");
////                                    HIS_CONTENT = nullToDefault(res.getString("his_content"), "");
////                                    ADJ_DESC_TMP = nullToDefault(res.getString("adj_desc"), "");
//                                    if(!"".equalsIgnoreCase(HIS_CONTENT))
//                                    {
//                                        mainObj.comment = HIS_CONTENT;
//                                    }
//
//                                    if(!"".equalsIgnoreCase(ADJ_DESC_TMP) && "fee".equalsIgnoreCase(ADJ_DESC_TMP))
//                                    {
//                                        log.add("desc", "phi dich vu tu ben connector");
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.FEE_VALUE;
//                                    }
//                                    else if(!"".equalsIgnoreCase(ADJ_DESC_TMP) && "bonus".equalsIgnoreCase(ADJ_DESC_TMP))
//                                    {
//                                        log.add("desc", "bonus tu ben connector");
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.BONUS_VALUE;
//                                    }
//                                    else if(!"".equalsIgnoreCase(ADJ_DESC_TMP) && "payment".equalsIgnoreCase(ADJ_DESC_TMP))
//                                    {
//                                        log.add("desc", "thanh toan tu ben connector");
//                                        mainObj.tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
//                                        mainObj.billId = "";
//                                    }
//                                    time_sync = System.currentTimeMillis();
//                                    tranDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
//                                        @Override
//                                        public void handle(Boolean result) {
//
//                                            tranSndLog.add("isUpdated", result);
//                                            String cap = "";
//                                            String body = "";
//
//                                            if (!result) {
//                                                //phi giao dich m2m
//                                                if (fAmount == 1000 && "fee".equalsIgnoreCase(fType)) {
//                                                    cap = "Phí dịch vụ";
//                                                    body = "Số dư tài khoản của bạn thay đổi: -1.000đ phí dịch vụ chuyển tiền.";
//                                                }
//
//                                                //c2c hoa hong gui tien
//                                                if ("c2ccomin".equalsIgnoreCase(fDebitor)) {
//                                                    cap = "Thù lao gửi tiền";
//                                                    body = "Bạn nhận được " + Misc.formatAmount(fAmount).replaceAll(",", ".") + "đ thù lao gửi tiền C2C";
//                                                }
//
//                                                //c2c hoa hong nhan tien
//                                                if ("c2ccomout".equalsIgnoreCase(fDebitor)) {
//                                                    cap = "Thù lao nhận tiền";
//                                                    body = "Bạn nhận được " + Misc.formatAmount(fAmount).replaceAll(",", ".") + "đ thù lao nhận tiền C2C";
//                                                }
//
//                                                if ("rut".equalsIgnoreCase(PARENTTYPE)) {
//                                                    cap = "Phí rút tiền";
//                                                    body = mainObj.comment;
//                                                }
//
//                                                BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
//                                                tranSndLog.add("cap --------------->", cap);
//
//                                                //ban notification
//                                                if (!"".equalsIgnoreCase(cap)) {
//                                                    Notification noti = new Notification();
//                                                    noti.receiverNumber = mainObj.owner_number;
//                                                    noti.caption = cap;
//                                                    noti.body = body;
//                                                    noti.bodyIOS = body;
//                                                    noti.sms = "";
//                                                    noti.tranId = mainObj.tranId;
//                                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
//                                                    noti.priority = 2;
//                                                    noti.status = Notification.STATUS_DETAIL;
//                                                    noti.time = System.currentTimeMillis();
//                                                    Misc.sendNoti(vertx, noti);
//
//                                                }
//                                                else if(!"".equalsIgnoreCase(NOTI_CAPTION))
//                                                {
//                                                    Notification noti = new Notification();
//                                                    noti.receiverNumber = mainObj.owner_number;
//                                                    noti.caption = NOTI_CAPTION;
//                                                    noti.body = NOTI_CONTENT;
//                                                    noti.bodyIOS = NOTI_CONTENT;
//                                                    noti.sms = "";
//                                                    noti.tranId = mainObj.tranId;
//                                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
//                                                    noti.priority = 2;
//                                                    noti.status = Notification.STATUS_DETAIL;
//                                                    noti.time = System.currentTimeMillis();
//                                                    Misc.sendNoti(vertx, noti);
//                                                }
//                                                else {
//                                                    if((turnOffNoti && jsonListTurnOffAgent.contains(CREDITOR) && jsonListTurnOffNotiNumber.contains(DEBITOR)) && ("adjustment".equalsIgnoreCase(TYPE)))
//                                                    {
//                                                        tranSndLog.add("desc", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
//                                                    }
//                                                    else {
//                                                        //Check dk noti
//                                                        tranSndLog.add("createRequestPushNotification ----------------------> ", "five");
//                                                        vertx.eventBus().send(
//                                                                AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
//                                                                , NotificationUtils.createRequestPushNotification(mainObj.owner_number
//                                                                        , 2
//                                                                        , mainObj)
//                                                        );
//                                                    }
//
//                                                }
//                                            }
//                                            tranSndLog.writeLog();
//                                        }
//                                    });
//                                }
//                                //END isMainValid
//
//
//                                //Begin isRecvValid
//                                if (isRecvValid) {
//                                    time_sync = System.currentTimeMillis();
//                                    if(turnOffNoti && jsonListTurnOffAgent.contains(DEBITOR))
//                                    {
//                                        rcvObj.desc = "Tiền từ TK Khuyến mãi đã được chuyển vào TK MoMo của quý khách.";
//                                        rcvObj.comment = "Tiền từ TK Khuyến mãi đã được chuyển vào TK MoMo của quý khách.";
//                                        rcvObj.partnerName = "CHUYỂN TIỀN KHUYẾN MÃI";
//                                        rcvObj.partnerId = "";
//                                    }
//                                    final Common.BuildLog tranRcvLog = new Common.BuildLog(logger);
//                                    tranRcvLog.setPhoneNumber("0" + rcvObj.owner_number);
//                                    tranRcvLog.add("this is tran for receiver", "");
//                                    tranRcvLog.add("isRecvValid", isRecvValid);
//                                    tranRcvLog.add("Json tran will be upsert", rcvObj.getJSON());
//                                    final boolean isRecvValid_tmp = isRecvValid;
//                                    tranDb.upsertTranOutSideNew(rcvObj.owner_number, rcvObj.getJSON(), new Handler<Boolean>() {
//                                        @Override
//                                        public void handle(Boolean result) {
//                                            tranRcvLog.add("isUpdated", result);
//
//                                            //neu khong phai la cap nhat --> tao moi
//                                            if (!result) {
//                                                if(turnOffNoti && jsonListTurnOffAgent.contains(DEBITOR))
//                                                {
//                                                    Notification noti = new Notification();
//                                                    noti.receiverNumber = rcvObj.owner_number;
//                                                    noti.caption = "Chuyển tiền từ TK Khuyến mãi sang TK MoMo";
//                                                    noti.body = "Tiền từ TK Khuyến mãi đã được chuyển vào TK MoMo của quý khách.";
//                                                    noti.bodyIOS = "Tiền từ TK Khuyến mãi đã được chuyển vào TK MoMo của quý khách.";
//                                                    noti.sms = "";
//                                                    noti.tranId = rcvObj.tranId;
//                                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
//                                                    noti.priority = 2;
//                                                    noti.status = Notification.STATUS_DETAIL;
//                                                    noti.time = System.currentTimeMillis();
//                                                    Misc.sendNoti(vertx, noti);
//                                                }
//                                                else if(!"".equalsIgnoreCase(NOTI_CONTENT) && !"".equalsIgnoreCase(NOTI_CAPTION))
//                                                {
//                                                    String content = NOTI_CONTENT.replaceAll("<<transid>>", rcvObj.tranId + "");
//                                                    log.add("NOTI_CONTENT", "true");
//                                                    log.add("NOTI_CONTENT BEFORE", NOTI_CONTENT);
//                                                    log.add("NOTI_CONTENT AFTER", content);
//                                                    Notification noti = new Notification();
//                                                    noti.receiverNumber = rcvObj.owner_number;
//                                                    noti.caption = "".equalsIgnoreCase(NOTI_CAPTION) ? "Nhận tiền thành công!" : NOTI_CAPTION;
//                                                    noti.body = content;
//                                                    noti.bodyIOS = content;
//                                                    noti.sms = "";
//                                                    noti.tranId = rcvObj.tranId;
//                                                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
//                                                    noti.priority = 2;
//                                                    noti.status = Notification.STATUS_DETAIL;
//                                                    noti.time = System.currentTimeMillis();
//                                                    Misc.sendNoti(vertx, noti);
//                                                }
//                                                else{
//                                                    if((turnOffNoti && jsonListTurnOffAgent.contains(CREDITOR) && jsonListTurnOffNotiNumber.contains(DEBITOR)) && ("adjustment".equalsIgnoreCase(TYPE)))
//                                                    {
//                                                        tranRcvLog.add("desc", "khong ban noti cho sdt " + DEBITOR + " vs agent " + CREDITOR);
//                                                    }
//                                                    else {
//                                                        log.add("createRequestPushNotification ----------------------> ", "four");
//                                                        BroadcastHandler.sendOutSideTransSync(vertx, rcvObj);
//                                                        vertx.eventBus().send(
//                                                                AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
//                                                                , NotificationUtils.createRequestPushNotification(
//                                                                        rcvObj.owner_number,
//                                                                        2,
//                                                                        rcvObj)
//                                                        );
//                                                    }
//                                                }
//                                            }
//                                            tranRcvLog.writeLog();
//                                        }
//                                    });
//                                } //END isRecvValid
//                            } // End while
//
//                            log.add("last sync", last_sync_time);
//                            log.add("last time", Misc.dateVNFormatWithTime(last_sync_time));
//
//                            mSettingsDb.setLong("LAST_TRANS_SYNC_TIME", last_sync_time, new Handler<Boolean>() {
//                                @Override
//                                public void handle(Boolean event) {
//                                    //do nothing here
//                                    log.add("save last sync time result", event);
//                                    log.writeLog();
//                                }
//                            });
//
//                            if (conn != null && !conn.isClosed()) {
//                                conn.close();
//                            }
//
//                        } catch (SQLException e) {
//
//                            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
//                            e.printStackTrace();
//                            logger.error("co loi tai call UMARKET2BACKEND(?,?) " + errorDesc);
//
//                            log.add("Execute statement failed", errorDesc);
//                            log.writeLog();
//                        } finally {
//                            mIsProcess.set(false);
//                        } // End try
//                    }
//                }
//            });
//        }
//    }

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

    public static class fieldNames {
        public static String NUMBER = "number";
        public static String TYPE = "type";
        public static String BALANCE = "balance";
        public static String COUPON_ID = "couponid";
        public static String TRAN_ID = "tranid";
        public static String MTCN = "mtcn";
    }

    //define fields in Json communication
    public static class NapRutTanNoi {
        public static String FROM_NUMBER = "from_number";
        public static String TO_NUMBER = "to_number";
        public static String AMOUNT = "amount";
        public static String M2M_TYPE = "m2mtype";
        public static String TID = "tid";

    }


//    public String getServiceName(String serviceCode)
//    {
//        String serviceName = "";
//        CharSequence vms_airtime	="vms.airtime";
//        CharSequence vinaphone_airtime	="vinaphone.airtime";
//        CharSequence viettelv1_airtime	="viettelv1.airtime";
//        CharSequence viettelv2_airtime	="viettelv2.airtime";
//        CharSequence viettelv3_airtime	="viettelv3.airtime";
//        CharSequence viettelv4_airtime	="viettelv4.airtime";
//        CharSequence vnmobile_airtime	="vnmobile.airtime";
//        CharSequence beeline_airtime	="beeline.airtime";
//        CharSequence billpay6	="billpay6";
//        CharSequence billpayviettelpost	="billpayviettelpost";
//        CharSequence billpaymobifone	="billpaymobifone";
//        CharSequence vcb_bank	= "vcb.bank";
//        CharSequence vietinbank_bank	="vietinbank.bank";
//        CharSequence billpay1	="billpay1";
//        CharSequence billpay2	="billpay2";
//        CharSequence billpay3	="billpay3";
//        CharSequence billpay4	="billpay4";
//        CharSequence billpayminhchau	="billpayminhchau";
//        CharSequence billpay7	="billpay7";
//        CharSequence billpaynuoccholon	="billpaynuoccholon";
//        CharSequence billpayonepay	="billpayonepay";
//        CharSequence billpayvnpay	="billpayvnpay";
//        CharSequence billpay9	="billpay9";
//        CharSequence billpaymsd	="billpaymsd";
//        CharSequence billpayevnhcm	="billpayevnhcm";
//        CharSequence billpaytrendmicro	="billpaytrendmicro";
//        CharSequence mathe_epay	="mathe_epay";
//        CharSequence billpaynuocnhabe	="billpaynuocnhabe";
//        CharSequence billpaypingsky	="billpaypingsky";
//        CharSequence billpaygiatotviet	="billpaygiatotviet";
//        CharSequence billpayopersmile	="billpayopersmile";
//        CharSequence license_kaspersky	="license_kaspersky";
//        CharSequence license_lacviet	="license_lacviet";
//        CharSequence billpay11	="billpay11";
//        CharSequence billpayfimplus	="billpayfimplus";
//        CharSequence ongame_airtime	="ongame.airtime";
//        CharSequence billpayprufinance	="billpayprufinance";
//        CharSequence mathe_bitcard	="mathe_bitcard";
//        CharSequence billpayvivoo	="billpayvivoo";
//        CharSequence billpayfshare	="billpayfshare";
//        CharSequence license_bkav	="license_bkav";
//        CharSequence mathe_mservice	="mathe_mservice";
//        CharSequence billpayvtvcab	="billpayvtvcab";
//
//        if(serviceCode.contains(vms_airtime))
//        {
//            serviceName = "mua mã thẻ MOBIFONE " ;
//        }else if(serviceCode.contains(vinaphone_airtime)){serviceName = "mua mã thẻ VINA " ;}
//        else if(serviceCode.contains(viettelv1_airtime)){serviceName = "mua mã thẻ VIETTEL  " ;}
//        else if(serviceCode.contains(viettelv2_airtime)){serviceName = "mua mã thẻ VIETTEL  " ;}
//        else if(serviceCode.contains(viettelv3_airtime)){serviceName = "mua mã thẻ VIETTEL  " ;}
//        else if(serviceCode.contains(viettelv4_airtime)){serviceName = "mua mã thẻ VIETTEL  " ;}
//        else if(serviceCode.contains(vnmobile_airtime)){serviceName = "mua mã thẻ VIETNAMMOBILE  " ;}
//        else if(serviceCode.contains(beeline_airtime)){serviceName = "mua mã thẻ BEELINE " ;}
//        else if(serviceCode.contains(billpay6)){serviceName = "mua mã thẻ VINA " ;}
//        else if(serviceCode.contains(billpayviettelpost)){serviceName = "mua mã thẻ VIETTEL  " ;}
//        else if(serviceCode.contains(billpaymobifone)){serviceName = "mua mã thẻ MOBIFONE " ;}
//        else if(serviceCode.contains(vcb_bank)){serviceName = "mua mã thẻ VIETCOMBANK " ;}
//        else if(serviceCode.contains(vietinbank_bank)){serviceName = "mua mã thẻ VIETINBANK " ;}
//        else if(serviceCode.contains(billpay1)){serviceName = "mua mã thẻ GAME VTC " ;}
//        else if(serviceCode.contains(billpay2)){serviceName = "mua mã thẻ GAME BAC FPT " ;}
//        else if(serviceCode.contains(billpay3)){serviceName = "mua mã thẻ GAME ZINGXU " ;}
//        else if(serviceCode.contains(billpay4)){serviceName = "mua mã thẻ GAME ONCASH " ;}
//        else if(serviceCode.contains(billpayminhchau)){serviceName = "mua mã thẻ MINHCHAU(GAME) " ;}
//        else if(serviceCode.contains(billpay7)){serviceName = "mua mã thẻ VNPT_ADSL_HCM " ;}
//        else if(serviceCode.contains(billpaynuoccholon)){serviceName = "mua mã thẻ ONEPAY/AIRMEKONG " ;}
//        else if(serviceCode.contains(billpayonepay)){serviceName = "mua mã thẻ ONEPAY/AIRMEKONG " ;}
//        else if(serviceCode.contains(billpayvnpay)){serviceName = "mua mã thẻ VNPAY " ;}
//        else if(serviceCode.contains(billpay9)){serviceName = "mua mã thẻ FPT_ADSL " ;}
//        else if(serviceCode.contains(billpaymsd)){serviceName = "mua mã thẻ MSD(Chích Ngừa Trả Chậm) " ;}
//        else if(serviceCode.contains(billpayevnhcm)){serviceName = "mua mã thẻ EVEHCM " ;}
//        else if(serviceCode.contains(billpaytrendmicro)){serviceName = "mua mã thẻ TRENDMICRO " ;}
//        else if(serviceCode.contains(mathe_epay)){serviceName = "mua mã thẻ EPAY " ;}
//        else if(serviceCode.contains(billpaynuocnhabe)){serviceName = "mua mã thẻ NUOCNB(Nước Nhà Bè) " ;}
//        else if(serviceCode.contains(billpaypingsky)){serviceName = "mua mã thẻ PINGSKY " ;}
//        else if(serviceCode.contains(billpaygiatotviet)){serviceName = "mua mã thẻ GTV " ;}
//        else if(serviceCode.contains(billpayopersmile)){serviceName = "mua mã thẻ OPERATIONSMILE " ;}
//        else if(serviceCode.contains(license_kaspersky)){serviceName = "mua mã thẻ KASPERSKY " ;}
//        else if(serviceCode.contains(license_lacviet)){serviceName = "mua mã thẻ LACVIET " ;}
//        else if(serviceCode.contains(billpay11)){serviceName = "mua mã thẻ VTHN " ;}
//        else if(serviceCode.contains(billpayfimplus)){serviceName = "mua mã thẻ FIMPLUS " ;}
//        else if(serviceCode.contains(ongame_airtime)){serviceName = "mua mã thẻ ONGAME " ;}
//        else if(serviceCode.contains(billpayprufinance)){serviceName = "mua mã thẻ PRUF " ;}
//        else if(serviceCode.contains(mathe_bitcard)){serviceName = "mua mã thẻ BIT " ;}
//        else if(serviceCode.contains(billpayvivoo)){serviceName = "mua mã thẻ VIVOO " ;}
//        else if(serviceCode.contains(billpayfshare)){serviceName = "mua mã thẻ FSHARE " ;}
//        else if(serviceCode.contains(license_bkav)){serviceName = "mua mã thẻ BKAV " ;}
//        else if(serviceCode.contains(mathe_mservice)){serviceName = "mua mã thẻ SOFTPIN " ;}
//        else if(serviceCode.contains(billpayvtvcab)){serviceName = "mua mã thẻ VTVCAB " ;}
//
//
//        return serviceName;
//    }
}
