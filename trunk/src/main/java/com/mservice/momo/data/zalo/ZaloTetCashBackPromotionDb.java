package com.mservice.momo.data.zalo;

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
 * Created by concu on 1/28/16.
 */
public class ZaloTetCashBackPromotionDb {


    private Vertx vertx;
    private Logger logger;

    public ZaloTetCashBackPromotionDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.ZaloTetCashBackPromotionCol.TABLE)
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

    public void updatePartial(String phone_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ZaloTetCashBackPromotionCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ZaloTetCashBackPromotionCol.PHONE_NUMBER, phone_number);
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


    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ZaloTetCashBackPromotionCol.TABLE);

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

    public void findOne(String number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.ZaloTetCashBackPromotionCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.ZaloTetCashBackPromotionCol.PHONE_NUMBER, number);
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
        public long tran_id = 0;
        public long tranIdBonus = 0;
        public long amount = 0;
        public long amount_bonus = 0;
        public String service_id = "";
        public long time = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.ZaloTetCashBackPromotionCol.PHONE_NUMBER, "").trim();
            tran_id = jo.getLong(colName.ZaloTetCashBackPromotionCol.TRAN_ID, 0);
            tranIdBonus = jo.getLong(colName.ZaloTetCashBackPromotionCol.TRAN_ID_BONUS, 0);
            time = jo.getLong(colName.ZaloTetCashBackPromotionCol.TIME, 0);
            amount = jo.getLong(colName.ZaloTetCashBackPromotionCol.AMOUNT, 0);
            amount_bonus = jo.getLong(colName.ZaloTetCashBackPromotionCol.AMOUNT_BONUS, 0);
            service_id = jo.getString(colName.ZaloTetCashBackPromotionCol.SERVICE_ID, "").trim();
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ZaloTetCashBackPromotionCol.PHONE_NUMBER, phone_number.trim());
            jo.putNumber(colName.ZaloTetCashBackPromotionCol.TIME, time);
            jo.putNumber(colName.ZaloTetCashBackPromotionCol.TRAN_ID, tran_id);
            jo.putNumber(colName.ZaloTetCashBackPromotionCol.TRAN_ID_BONUS, tranIdBonus);
            jo.putString(colName.ZaloTetCashBackPromotionCol.SERVICE_ID, service_id);
            jo.putNumber(colName.ZaloTetCashBackPromotionCol.AMOUNT, amount);
            jo.putNumber(colName.ZaloTetCashBackPromotionCol.AMOUNT_BONUS, amount_bonus);
            return jo;
        }
    }



}
