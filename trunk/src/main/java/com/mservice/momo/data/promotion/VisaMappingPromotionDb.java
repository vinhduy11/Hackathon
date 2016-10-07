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
 * Created by concu on 4/25/16.
 */
public class VisaMappingPromotionDb {
    private Vertx vertx;
    private Logger logger;

    public VisaMappingPromotionDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final String program, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.VisaMappingPromotionCol.TABLE + "_" + program)
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

    public void updatePartial(final String program,String phone_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMappingPromotionCol.TABLE + "_" + program);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.VisaMappingPromotionCol.PHONE_NUMBER, phone_number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.MULTI, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void updateWithInsert(final String program,String phone_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMappingPromotionCol.TABLE + "_" + program);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.VisaMappingPromotionCol.PHONE_NUMBER, phone_number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.MULTI, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(final String program,JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMappingPromotionCol.TABLE + "_" + program);

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

    public void findOne(final String program, String number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMappingPromotionCol.TABLE + "_" + program);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.VisaMappingPromotionCol.PHONE_NUMBER, number);
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

        public String phone_number = "";
        public String program = "";
        public long time = 0;
        public String bankAcc = "";
        public String cardInfo = "";
        public String device_imei = "";
        public JsonObject joExtra = new JsonObject();
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.VisaMappingPromotionCol.PHONE_NUMBER, "").trim();
            time = jo.getLong(colName.VisaMappingPromotionCol.TIME_MAPPING, 0);
            program = jo.getString(colName.VisaMappingPromotionCol.PROGRAM, "");
            bankAcc = jo.getString(colName.VisaMappingPromotionCol.BANK_ACC, "");
            cardInfo = jo.getString(colName.VisaMappingPromotionCol.CARD_INFO, "");
            device_imei = jo.getString(colName.VisaMappingPromotionCol.DEVICE_IMEI, "");
            joExtra = jo.getObject(colName.VisaMappingPromotionCol.EXTRA, new JsonObject());
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.VisaMappingPromotionCol.PHONE_NUMBER, phone_number.trim());
            jo.putNumber(colName.VisaMappingPromotionCol.TIME_MAPPING, time);
            jo.putString(colName.VisaMappingPromotionCol.PROGRAM, program);
            jo.putString(colName.VisaMappingPromotionCol.BANK_ACC, bankAcc);
            jo.putString(colName.VisaMappingPromotionCol.CARD_INFO, cardInfo);
            jo.putString(colName.VisaMappingPromotionCol.DEVICE_IMEI, device_imei);
            jo.putObject(colName.VisaMappingPromotionCol.EXTRA, joExtra);
            return jo;
        }
    }
}
