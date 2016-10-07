package com.mservice.momo.data;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by duyhuynh on 05/07/2016.
 */
public class DBFactory {

    public static NotificationDb createNotiDb(Vertx vertx, Logger logger, JsonObject glbConfig) {
        String notiSource = glbConfig.getString(Source.NOTI_SOURCE, Source.MONGO);
        if (Source.MYSQL.equalsIgnoreCase(notiSource)) {
            return new NotificationMySQLDb(vertx, logger, glbConfig);
        } else if (Source.MONGO_MYSQL.equalsIgnoreCase(notiSource)) {
            return new NotificationMongoDb(vertx, logger, glbConfig, true);
        } else {
            return new NotificationMongoDb(vertx, logger, glbConfig, false);
        }
    }

    public static TransDb createTranDb(Vertx vertx, EventBus eb, Logger logger, JsonObject glbConfig) {
        String tranSource = glbConfig.getString(Source.TRAN_SOURCE, Source.MONGO);
        if (Source.MYSQL.equalsIgnoreCase(tranSource)) {
            return new TransMySQLDb(vertx, eb, logger, glbConfig);
        } else if (Source.MONGO_MYSQL.equalsIgnoreCase(tranSource)) {
            return new TransMongoDb(vertx, eb, logger, glbConfig, true);
        } else {
            return new TransMongoDb(vertx, eb, logger, glbConfig, false);
        }
    }

    public static class Source {
        public static final String MYSQL = "mysql";
        public static final String MONGO = "mongo";
        public static final String MONGO_MYSQL = "mongo_mysql";
        public static final String NOTI_SOURCE = "db_noti_source";
        public static final String TRAN_SOURCE = "db_tran_source";
        public static final String MYSQL_DELAY = "mysql_delay";
        public static final String MYSQL_MODULE_INFO = "db_module_info";
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String NOTI_PATH = "/noti";
        public static final String TRAN_PATH = "/tran";
    }
}
