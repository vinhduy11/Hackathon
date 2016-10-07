package com.mservice.momo.data.promotion;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 9/20/16.
 */
public class CheatInfoDb {
    private Vertx vertx;
    private Logger logger;

    public CheatInfoDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.CheatInfoDbCols.TABLE)
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

        public String info = "";

        public String phoneNumber = "";

        public long time = 0;


        public Obj() {
        }

        public Obj(JsonObject jo) {

            phoneNumber = jo.getString(colName.CheatInfoDbCols.PHONE_NUMBER, "");
            time = jo.getLong(colName.CheatInfoDbCols.TIME, 0);
            info = jo.getString(colName.CheatInfoDbCols.INFO, "");

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.CheatInfoDbCols.PHONE_NUMBER,phoneNumber);
            jo.putNumber(colName.CheatInfoDbCols.TIME, time);
            jo.putString(colName.CheatInfoDbCols.INFO, info);

            return jo;
        }
    }
}
