package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class MntDb {
    EventBus eventBus;
    Logger logger;

    public MntDb(EventBus eb, Logger log) {
        eventBus = eb;
        logger = log;
    }

    public void save(Obj obj, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.MntCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                boolean result = false;
                if (event.body() != null && event.body().getString(MongoKeyWords.UPSERTED_ID, null) != null) {
                    result = true;
                }
                callback.handle(result);
            }
        });
    }

    public void updatePartial(String code, JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.MntCols.table);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.MntCols.code, code);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    public void findOne(final String code, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.MntCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.MntCols.code, code);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj o = null;
                JsonObject jo = event.body().getObject(MongoKeyWords.RESULT, null);
                if (jo != null) {
                    o = new Obj(jo);
                }
                callback.handle(o);
            }
        });
    }

    public void remove(String code, final Handler<Boolean> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.MntCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.MntCols.code, code);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public static class Obj {
        public String code = "";
        public JsonArray shared = new JsonArray();
        public String timevn = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        public String agentnumber = "";
        public long tranid = 0;
        public int checkfone = 0;
        public long amount = 0;
        public long time = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {

            code = jo.getString(colName.MntCols.code, "");
            agentnumber = jo.getString(colName.MntCols.agentnumber, "");
            shared = jo.getArray(colName.MntCols.shared, new JsonArray());
            timevn = jo.getString(colName.MntCols.timevn, "");
            tranid = jo.getLong(colName.MntCols.tranid, 0);
            checkfone = jo.getInteger(colName.MntCols.checkfone, checkfone);
            amount = jo.getLong(colName.MntCols.amount, 0);
            time = jo.getLong(colName.MntCols.time, 0);
        }


        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.MntCols.code, code);
            jo.putString(colName.MntCols.agentnumber, agentnumber);
            jo.putArray(colName.MntCols.shared, shared);
            jo.putString(colName.MntCols.timevn, timevn);
            jo.putNumber(colName.MntCols.tranid, tranid);
            jo.putNumber(colName.MntCols.checkfone, checkfone);
            jo.putNumber(colName.MntCols.amount, amount);
            jo.putNumber(colName.MntCols.time, time);
            return jo;
        }
    }
}
