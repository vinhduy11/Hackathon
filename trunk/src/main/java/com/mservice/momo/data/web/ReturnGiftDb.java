package com.mservice.momo.data.web;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by congnguyenit on 8/11/16.
 */
public class ReturnGiftDb {
    private Logger logger;
    private EventBus eventBus;

    public ReturnGiftDb(EventBus eventBus, Logger logger) {
        this.logger = logger;
        this.eventBus = eventBus;
    }

    public void insertDetail(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.ReturnGiftDetailDbCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJsonObject());


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
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

    public void insert(final LogDbObj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.ReturnGiftDbCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJsonObject());


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
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

    public void getAllDetail(final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ReturnGiftDetailDbCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ReturnGiftDetailDbCols.TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }
                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getAll(final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ReturnGiftDbCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ReturnGiftDbCols.TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Obj> arrayList = null;
                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }
                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public static class Obj {
        public String rid;
        public String ruser;
        public String phone_number;
        public String promotion_name;
        public Long time;
        public Boolean result;

        public Obj(JsonObject jo) {
            rid = jo.getString(colName.ReturnGiftDetailDbCols.RID, "");
            ruser = jo.getString(colName.ReturnGiftDetailDbCols.RUSER, "");
            phone_number = jo.getString(colName.ReturnGiftDetailDbCols.PHONE_NUMBER, "");
            promotion_name = jo.getString(colName.ReturnGiftDetailDbCols.PROMOTION_NAME, "");
            time = jo.getLong(colName.ReturnGiftDetailDbCols.TIME, 0L);
            result = jo.getBoolean(colName.ReturnGiftDetailDbCols.RESULT, false);
        }

        public JsonObject toJsonObject() {
            JsonObject output = new JsonObject();
            output.putString(colName.ReturnGiftDetailDbCols.RID, rid);
            output.putString(colName.ReturnGiftDetailDbCols.RUSER, ruser);
            output.putString(colName.ReturnGiftDetailDbCols.PHONE_NUMBER, phone_number);
            output.putString(colName.ReturnGiftDetailDbCols.PROMOTION_NAME, promotion_name);
            output.putNumber(colName.ReturnGiftDetailDbCols.TIME, time);
            output.putBoolean(colName.ReturnGiftDetailDbCols.RESULT, result);
            return output;
        }
    }

    public static class LogDbObj {
        public String rid;
        public String promotion_name;
        public Long time;

        public LogDbObj(JsonObject jo) {
            rid = jo.getString(colName.ReturnGiftDetailDbCols.RID, "");
            promotion_name = jo.getString(colName.ReturnGiftDetailDbCols.PROMOTION_NAME, "");
            time = jo.getLong(colName.ReturnGiftDetailDbCols.TIME, 0L);
        }

        public JsonObject toJsonObject() {
            JsonObject output = new JsonObject();
            output.putString(colName.ReturnGiftDetailDbCols.RID, rid);
            output.putString(colName.ReturnGiftDetailDbCols.PROMOTION_NAME, promotion_name);
            output.putNumber(colName.ReturnGiftDetailDbCols.TIME, time);
            return output;
        }
    }
}
