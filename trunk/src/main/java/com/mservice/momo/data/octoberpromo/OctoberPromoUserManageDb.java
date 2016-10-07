package com.mservice.momo.data.octoberpromo;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 10/14/15.
 */
public class OctoberPromoUserManageDb {

    private Vertx vertx;
    private Logger logger;

    public OctoberPromoUserManageDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.OctoberPromoUserManageCols.TABLE)
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

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoUserManageCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.OctoberPromoUserManageCols.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void update(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoUserManageCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.OctoberPromoUserManageCols.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoUserManageCols.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        Obj obj = new Obj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void findOne(String phoneNumber, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoUserManageCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.OctoberPromoUserManageCols.PHONE_NUMBER, phoneNumber);
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

    public void countUserRangeTime(long startTime, long endTime, final Handler<JsonArray> callback){

        JsonObject sumNumber = new JsonObject()
                .putNumber("$sum", 1);

        JsonObject grouper = new JsonObject()
                .putString("_id", "$is_lucky_man")
                .putObject("count", sumNumber);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoUserManageCols.TABLE);
        query.putString("action", "aggregate");

        query.putObject(MongoKeyWords.GROUPER, grouper);

        JsonObject jsonLessTime = new JsonObject();
        jsonLessTime.putNumber(MongoKeyWords.LESS_THAN, endTime);

        JsonObject jsonLess = new JsonObject();
        jsonLess.putObject(colName.OctoberPromoUserManageCols.TIME, jsonLessTime);

        JsonObject jsonGreaterTime = new JsonObject();
        jsonGreaterTime.putNumber(MongoKeyWords.GREATER, startTime);

        JsonObject jsonGreater = new JsonObject();
        jsonGreater.putObject(colName.OctoberPromoUserManageCols.TIME, jsonGreaterTime);

        JsonArray jsonArrayAnd = new JsonArray();
        jsonArrayAnd.add(jsonLess);
        jsonArrayAnd.add(jsonGreater);

        JsonObject jsonAnd = new JsonObject();
        jsonAnd.putArray(MongoKeyWords.AND_$, jsonArrayAnd);

        query.putObject(MongoKeyWords.MATCHER, jsonAnd);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if(event.body() != null)
                {
                    JsonArray result = event.body().getArray("result");
                    callback.handle(result);
                }
                else {
                    callback.handle(new JsonArray());
                }
            }
        });
    }

    public static class Obj {

        public String phone_number = "";
        public long time = 0;
        public boolean is_lucky_man = false;

        public Obj() {
        }

        public Obj(JsonObject jo) {
            phone_number = jo.getString(colName.OctoberPromoUserManageCols.PHONE_NUMBER, "");
            time = jo.getLong(colName.OctoberPromoUserManageCols.TIME, 0);
            is_lucky_man = jo.getBoolean(colName.OctoberPromoUserManageCols.IS_LUCKY_MAN, false);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.OctoberPromoUserManageCols.PHONE_NUMBER, phone_number);
            jo.putNumber(colName.OctoberPromoUserManageCols.TIME, time);
            jo.putBoolean(colName.OctoberPromoUserManageCols.IS_LUCKY_MAN, is_lucky_man);
            return jo;
        }
    }


}
