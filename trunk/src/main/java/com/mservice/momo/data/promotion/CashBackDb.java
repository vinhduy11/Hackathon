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
 * Created by concu on 4/23/16.
 */
public class CashBackDb {
    private Vertx vertx;
    private Logger logger;

    public CashBackDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(String program, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, program + "_cashback")
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

    public void updatePartial(String program, String phone_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, program + "_cashback");
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CashBackCol.PHONE_NUMBER, phone_number);
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


    public void searchWithFilter(String program, JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, program + "_cashback");

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

    public void findOne(String program, String number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, program + "_cashback");

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.CashBackCol.PHONE_NUMBER, number);
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
        public long tranId = 0;
        public long tranIdBonus = 0;
        public long amount = 0;
        public long amountBonus = 0;
        public int rate = 0;
        public int tranType = 0;
        public String serviceId = "";
        public long time = 0;
        public String bankAcc = "";
        public String cardInfo = "";
        public String device_imei = "";
        public int error_code = -1;
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.CashBackCol.PHONE_NUMBER, "").trim();
            tranId = jo.getLong(colName.CashBackCol.TRAN_ID, 0);
            tranIdBonus = jo.getLong(colName.CashBackCol.BONUS_TRAN_ID, 0);
            time = jo.getLong(colName.CashBackCol.TIME, 0);
            amount = jo.getLong(colName.CashBackCol.AMOUNT, 0);
            amountBonus = jo.getLong(colName.CashBackCol.BONUS_AMOUNT, 0);
            serviceId = jo.getString(colName.CashBackCol.SERVICE_ID, "").trim();
            rate = jo.getInteger(colName.CashBackCol.RATE, 0);
            tranType = jo.getInteger(colName.CashBackCol.TRAN_TYPE, 0);
            program = jo.getString(colName.CashBackCol.PROGRAM, "");
            bankAcc = jo.getString(colName.CashBackCol.BANK_ACC, "");
            cardInfo = jo.getString(colName.CashBackCol.CARD_INFO, "");
            device_imei = jo.getString(colName.CashBackCol.DEVICE_IMEI, "");
            error_code = jo.getInteger(colName.CashBackCol.ERROR_CODE, -1);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.CashBackCol.PHONE_NUMBER, phone_number.trim());
            jo.putNumber(colName.CashBackCol.TIME, time);
            jo.putNumber(colName.CashBackCol.TRAN_ID, tranId);
            jo.putNumber(colName.CashBackCol.BONUS_TRAN_ID, tranIdBonus);
            jo.putString(colName.CashBackCol.SERVICE_ID, serviceId);
            jo.putNumber(colName.CashBackCol.AMOUNT, amount);
            jo.putNumber(colName.CashBackCol.BONUS_AMOUNT, amountBonus);
            jo.putString(colName.CashBackCol.PROGRAM, program);
            jo.putNumber(colName.CashBackCol.TRAN_TYPE, tranType);
            jo.putNumber(colName.CashBackCol.RATE, rate);
            jo.putString(colName.CashBackCol.BANK_ACC, bankAcc);
            jo.putString(colName.CashBackCol.CARD_INFO, cardInfo);
            jo.putString(colName.CashBackCol.DEVICE_IMEI, device_imei);
            jo.putNumber(colName.CashBackCol.ERROR_CODE, error_code);
            return jo;
        }
    }
}
