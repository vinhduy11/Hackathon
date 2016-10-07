package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 11/19/15.
 */
public class PushNotificationInfoManagerDb {

    private Vertx vertx;
    private Logger logger;

    public PushNotificationInfoManagerDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.PushNotificationInfoManager.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if (!event.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(event.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }

    public static class Obj {

        public static String PUSH_INFO_ID = "_id";
        public static String NUMBER = "number";
        public static String TIME = "time";
        public static String TABLE = "PushNotificationInfoManagerDb";

        public String push_info_id = "";
        public String number = "";
        public long time = 0;


        public Obj() {
        }

        public Obj(JsonObject jo) {
            push_info_id = jo.getString(colName.PushNotificationInfoManager.PUSH_INFO_ID, "");
            number = jo.getString(colName.PushNotificationInfoManager.NUMBER, "");
            time = jo.getLong(colName.PushNotificationInfoManager.TIME, 0);



        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.PushNotificationInfoManager.PUSH_INFO_ID, push_info_id);
            jo.putString(colName.PushNotificationInfoManager.NUMBER, number);
            jo.putNumber(colName.PushNotificationInfoManager.TIME, time);

            return jo;
        }
    }
}
