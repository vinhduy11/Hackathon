package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 7/22/15.
 */
public class ControlOnClickActivityDb {
    private Vertx vertx;
//    private Logger logger;

    public ControlOnClickActivityDb(Vertx vertx) {
//        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.ControlOnClickActivity.TABLE)
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


    public void removeData(String code, final Handler<Boolean> callback)
    {
        JsonObject query = new JsonObject();
        JsonObject matcher   = new JsonObject();
        matcher.putString(colName.ControlOnClickActivity.KEY,code);
        query.putString( MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ControlOnClickActivity.TABLE);
        query.putObject(MongoKeyWords.MATCHER, matcher);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jObjMsg) {
                JsonObject obj = jObjMsg.body();
                if (obj.getString( MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public static class Obj {


        public String key = "";
        public String number = "";
        public String service = "";
        public String program = "";


        public Obj() {

        }

        public Obj(JsonObject jo) {
            key = jo.getString(colName.ControlOnClickActivity.KEY, "");
            number = jo.getString(colName.ControlOnClickActivity.NUMBER, "");
            service = jo.getString(colName.ControlOnClickActivity.SERVICE, "");
            program = jo.getString(colName.ControlOnClickActivity.PROGRAM, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ControlOnClickActivity.KEY, key);
            jo.putString(colName.ControlOnClickActivity.NUMBER, number);
            jo.putString(colName.ControlOnClickActivity.SERVICE, service);
            jo.putString(colName.ControlOnClickActivity.PROGRAM, program);
            return jo;
        }
    }
}
