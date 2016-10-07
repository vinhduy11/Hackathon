package com.mservice.momo.data.promotion;

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
 * Created by concu on 3/3/16.
 */
public class PhoneCheckDb {

    private Vertx vertx;
    private Logger logger;

    public PhoneCheckDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(String program, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, program)
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

    public void updatePartial(String phone, String program
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, program);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.PhoneChecking.PHONE_NUMBER, phone);
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

    public void upSert(String phone, String program
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, program);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.PhoneChecking.PHONE_NUMBER, phone);
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

    public void searchWithFilter(String program, JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, program);

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


    public void findOne(String phone, String program, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, program);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.PhoneChecking.PHONE_NUMBER, phone);
        matcher.putBoolean(colName.PhoneChecking.ENABLE, null);
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

    public static class Obj {

        public String phone = "";
        public String program = "";
        public int trans_per_day = 0;
        public int trans_per_hour = 0;
        public int min_between_trans = 0;
        public int totalIOS = 0;
        public int totalAndroid = 0;
        public String off_time_from = "";
        public String off_time_to = "";

        public Obj() {
        }

        public Obj(JsonObject jo) {
            phone = jo.getString(colName.PhoneChecking.PHONE_NUMBER, "");
            program = jo.getString(colName.PhoneChecking.PROGRAM, "");
            trans_per_day = jo.getInteger(colName.PhoneChecking.TRANS_PER_DAY, 0);
            trans_per_hour = jo.getInteger(colName.PhoneChecking.TRANS_PER_HOUR, 0);
            min_between_trans = jo.getInteger(colName.PhoneChecking.MIN_BETWEEN_TRANS, 0);
            totalIOS = jo.getInteger(colName.PhoneChecking.TOTAL_IOS, 0);
            totalAndroid = jo.getInteger(colName.PhoneChecking.TOTAL_ANDROID, 0);
            off_time_from = jo.getString(colName.PhoneChecking.OFF_TIME_FROM, "");
            off_time_to = jo.getString(colName.PhoneChecking.OFF_TIME_TO, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.PhoneChecking.PHONE_NUMBER, phone);
            jo.putString(colName.PhoneChecking.PROGRAM, program);
            jo.putNumber(colName.PhoneChecking.TRANS_PER_DAY, trans_per_day);
            jo.putNumber(colName.PhoneChecking.TRANS_PER_HOUR, trans_per_hour);
            jo.putNumber(colName.PhoneChecking.MIN_BETWEEN_TRANS, min_between_trans);
            jo.putNumber(colName.PhoneChecking.TOTAL_IOS, totalIOS);
            jo.putNumber(colName.PhoneChecking.TOTAL_ANDROID, totalAndroid);
            jo.putString(colName.PhoneChecking.OFF_TIME_FROM, off_time_from);
            jo.putString(colName.PhoneChecking.OFF_TIME_TO, off_time_to);
            return jo;
        }
    }
}
