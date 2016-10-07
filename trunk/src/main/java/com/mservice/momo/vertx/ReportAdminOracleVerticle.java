package com.mservice.momo.vertx;

import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by concu on 5/28/14.
 */
public class ReportAdminOracleVerticle extends Verticle {

//    public static String ADDRESS ="REPORT_ADMIN.OracleVerticle";
    private Logger logger;
    private static String Driver;
    private static String Url;
    private static String Username;
    private static String Password;

    private DBProcess dbProcess;

    public void start() {
        logger = getContainer().logger();

        JsonObject db_cfg = getContainer().config();
        Driver  = db_cfg.getString("driver");
        Url = db_cfg.getString("url");
        Username = db_cfg.getString("username");
        Password = db_cfg.getString("password");

        //==> must be start after MongoDB
        dbProcess = new DBProcess(Driver,Url,Username,Password, AppConstant.ReportAdminOracleVerticle_ADDRESS, AppConstant.ReportAdminOracleVerticle_ADDRESS,logger);
        EventBus eb = vertx.eventBus();

        /*Handler<CoreMessage<JsonObject>> myHandler = new Handler<CoreMessage<JsonObject>>() {
            public void handle(CoreMessage<JsonObject> message) {
                JsonObject jsonObject = message.body();
                String type = jsonObject.getString(BANKNET_COLS.TYPE);
                switch (type){

                    default:
                        logger.warn("ReportAdminOracleVerticle NOT SUPPORT COMMAND " + type);
                        break;
                }
            }
        };

        eb.registerHandler(ADDRESS, myHandler);*/
    }

}
