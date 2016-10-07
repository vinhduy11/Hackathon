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
 * Created by concu on 2/4/16.
 */
public class LixiManageDb {

    private Vertx vertx;
    private Logger logger;

    public LixiManageDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.LixiManageCol.TABLE)
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

        public String phone_number = "";
        public long time = 0;
        public long money_value = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.LixiManageCol.PHONE_NUMBER, "").trim();
            time = jo.getLong(colName.LixiManageCol.TIME, 0);
            money_value = jo.getLong(colName.LixiManageCol.AMOUNT, 0);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.LixiManageCol.PHONE_NUMBER, phone_number.trim());
            jo.putNumber(colName.LixiManageCol.TIME, time);
            jo.putNumber(colName.LixiManageCol.AMOUNT, money_value);
            return jo;
        }
    }

}
