package com.mservice.momo.data.grabpromo;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by congnguyenit on 8/30/16.
 */
class GrabCodePromoDb {

    private Vertx vertx;
    private Logger logger;

    public GrabCodePromoDb(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void findByCode(String code, final Handler<Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.GrabCodePromoDbCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.GrabCodePromoDbCols.CODE, code);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }

    public void removeByCode(String code, final Handler<Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.GrabCodePromoDbCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.GrabCodePromoDbCols.CODE, code);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.GrabCodePromoDbCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> joObject) {

                int result = 0;
                if (!joObject.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(joObject.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }


    public static class Obj {

        public String  NUMBER;
        public String  CODE;

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.GrabCodePromoDbCols.CODE, CODE);
            jo.putString(colName.GrabCodePromoDbCols.NUMBER, NUMBER);
            return jo;

        }

        public Obj(JsonObject jo){
            NUMBER        = jo.getString(colName.GrabCodePromoDbCols.NUMBER, "");
            CODE          = jo.getString(colName.GrabCodePromoDbCols.CODE, "");
        }
    }
}
