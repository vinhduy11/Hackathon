package com.mservice.momo.gateway.internal.db.oracle;

import com.mservice.momo.gateway.internal.soapin.information.obj.MStore;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStoreNearestRequest;
import com.mservice.momo.util.JSONUtil;
import com.mservice.momo.util.MomoJackJsonPack;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.merchant.lotte.entity.MerTranInfo;
import com.mservice.momo.vertx.merchant.lotte.entity.MerTranhis;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by duyhv on 12/15/2015.
 */
public class HTPPOracleVerticle extends Verticle {

    //share all
    public static final int CHECK_AGENT_STATUS = 9;
    public static final int SEARCH_STORE_NEAREST = 10;
    public static final int SEARCH_STORE = 11;
    public static final int INSERT_MERCHANT_TRAN = 12;
    public static final int GET_MERCHANT_TRANHIS = 13;
    public static final String TIME = "time";
    public static final String COMMAND = "type";
    private static String Driver;
    private static String Url;
    private static String Username;
    private static String Password;

    private Logger logger;
    private DBProcess dbProcess;
    private JsonObject glbCfg = new JsonObject();

    public void start() {
        logger = container.logger();
        glbCfg = container.config();

        JsonObject db_cfg = glbCfg.getObject("htpp_admin_database");
        Driver = db_cfg.getString("driver");
        Url = db_cfg.getString("url");
        Username = db_cfg.getString("username");
        Password = db_cfg.getString("password");

        dbProcess = new DBProcess(Driver
                , Url
                , Username
                , Password
                , AppConstant.HTPPOracleVerticle_ADDRESS
                , AppConstant.HTPPOracleVerticle_ADDRESS
                , logger);

        EventBus eb = vertx.eventBus();

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                JsonObject jsonObject = message.body();

                int type = jsonObject.getInteger(COMMAND);
                switch (type) {
                    case CHECK_AGENT_STATUS:
                        checkAgentStatus(message);
                        break;
                    case SEARCH_STORE_NEAREST:
                        searchStoreNearest(message);
                        break;
                    case SEARCH_STORE:
                        searchStore(message);
                        break;
                    case INSERT_MERCHANT_TRAN:
                        insertMerchantTran(message);
                        break;
                    case GET_MERCHANT_TRANHIS:
                        getMerchantTran(message);
                        break;
                    default:
                        logger.warn("HTPPOracleVerticle NOT SUPPORT COMMAND " + type);
                        break;
                }
            }
        };

        eb.registerLocalHandler(AppConstant.HTPPOracleVerticle_ADDRESS, myHandler);
    }

    private void getMerchantTran(Message<JsonObject> message) {
        JsonObject json = message.body().getObject("data");
        MerTranhis request = MomoJackJsonPack.jsonToObj(json.encode(), MerTranhis.class);
        dbProcess.getMerchantTran(request, logger);
        JsonObject res = new JsonObject(MomoJackJsonPack.objToString(request));
        message.reply(res);
        return;
    }

    private void insertMerchantTran(Message<JsonObject> message) {
        JsonObject json = message.body().getObject("data");
        MerTranInfo request = MomoJackJsonPack.jsonToObj(json.encode(), MerTranInfo.class);
        boolean result = dbProcess.insertMerchantTran(request, logger);
        json.putBoolean("result", result);
        message.reply(json);
        return;
    }

    /**
     * Kiem tra DGD hoac DCNTT
     * @param message
     */
    private void checkAgentStatus(Message<JsonObject> message) {
        JsonObject json = message.body();
        String mmphone = json.getString("aPhone", "0");
        MStore store = dbProcess.checkStoreStatus(mmphone, logger);
        if ((store == null || store.ID == -1)
                && dbProcess.checkDCNTT(mmphone, logger, glbCfg.getObject("htpp_admin_database").getString("dcntt_group_ids", "58123, 59124"))) {
            store = new MStore();
            store.ID = -2; // fix for DCNTT
            store.STATUS = 0; // fix for DCNTT
        }

        JsonObject res = JSONUtil.fromObjToJsonObj(store);
        message.reply(res);
        return;
    }

    private void searchStoreNearest(Message<JsonObject> message) {
        MStoreNearestRequest request = JSONUtil.fromJsonObjToObj(message.body().getObject("nearest"), MStoreNearestRequest.class);
        dbProcess.selectStoreNearest_new(request, glbCfg.getObject("map"), logger);
        JsonObject res = JSONUtil.fromObjToJsonObj(request);
        message.reply(res);
        return;
    }

    private void searchStore(Message<JsonObject> message) {
        MStoreNearestRequest request = JSONUtil.fromJsonObjToObj(message.body().getObject("nearest"), MStoreNearestRequest.class);
        dbProcess.selectStore_new(request, glbCfg.getObject("map"), logger);
        JsonObject res = JSONUtil.fromObjToJsonObj(request);
        message.reply(res);
        return;
    }
}
