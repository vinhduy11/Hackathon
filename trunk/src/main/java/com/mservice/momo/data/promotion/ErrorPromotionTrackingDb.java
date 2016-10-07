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
 * Created by concu on 4/28/16.
 */
public class ErrorPromotionTrackingDb {
    private Vertx vertx;
    private Logger logger;


    public ErrorPromotionTrackingDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(String program, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, program + "_ErrorCheck")
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
        query.putString(MongoKeyWords.COLLECTION, program + "_ErrorCheck");
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ErrorPromotionTrackingCol.PHONE_NUMBER, phone);
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
        query.putString(MongoKeyWords.COLLECTION, program + "_ErrorCheck");
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ErrorPromotionTrackingCol.PHONE_NUMBER, phone);
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
        query.putString(MongoKeyWords.COLLECTION, program + "_ErrorCheck");

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

    public static class Obj {

        public String phone = "";
        public String program = "";
        public long time = 0;
        public int error_code = 0;
        public String desc = "";
        public String deviceInfo = "";
        public Obj() {
        }

        public Obj(JsonObject jo) {
            phone = jo.getString(colName.ErrorPromotionTrackingCol.PHONE_NUMBER, "");
            program = jo.getString(colName.ErrorPromotionTrackingCol.PROGRAM, "");
            time = jo.getLong(colName.ErrorPromotionTrackingCol.TIME, 0);
            error_code = jo.getInteger(colName.ErrorPromotionTrackingCol.ERROR_CODE, 0);
            desc = jo.getString(colName.ErrorPromotionTrackingCol.DESC, "");
            deviceInfo = jo.getString(colName.ErrorPromotionTrackingCol.DEVICE_INFO, "");

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ErrorPromotionTrackingCol.PHONE_NUMBER, phone);
            jo.putString(colName.ErrorPromotionTrackingCol.PROGRAM, program);
            jo.putNumber(colName.ErrorPromotionTrackingCol.TIME, time);
            jo.putNumber(colName.ErrorPromotionTrackingCol.ERROR_CODE, error_code);
            jo.putString(colName.ErrorPromotionTrackingCol.DESC, desc);
            jo.putString(colName.ErrorPromotionTrackingCol.DEVICE_INFO, deviceInfo);
            return jo;
        }
    }
}
