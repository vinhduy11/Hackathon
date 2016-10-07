package com.mservice.momo.data.customercaregiftgroup;

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
 * Created by concu on 11/24/15.
 */
public class DollarHeartCustomerCareGiftGroupDb {

    private Vertx vertx;
    private Logger logger;

    public DollarHeartCustomerCareGiftGroupDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.DollarHeartCustomerCareGiftGroup.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.DollarHeartCustomerCareGiftGroup.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.DollarHeartCustomerCareGiftGroup.NUMBER, phone_number);
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
        query.putString(MongoKeyWords.COLLECTION, colName.DollarHeartCustomerCareGiftGroup.TABLE);

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
        query.putString(MongoKeyWords.COLLECTION, colName.DollarHeartCustomerCareGiftGroup.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.DollarHeartCustomerCareGiftGroup.NUMBER, number);
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

    public void deleteRows(String number, final Handler<Boolean> callback)
    {
        JsonObject query    = new JsonObject();
        JsonObject matcher   = new JsonObject();
        matcher.putString(colName.DollarHeartCustomerCareGiftGroup.NUMBER, number);
        query.putString( MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.DollarHeartCustomerCareGiftGroup.TABLE);
        query.putBoolean(MongoKeyWords.MULTI, true);
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

        public String phone_number = "";
        public String gift_id = "";
        public long time = 0;
        public long money_value = 0;
        public long gift_amount = 0;
        public boolean is_received_noti = false;
        public String program = "";
        public long tranId = 0;
        public String group = "";
        public int duration = 0;
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.DollarHeartCustomerCareGiftGroup.NUMBER, "").trim();
            gift_id = jo.getString(colName.DollarHeartCustomerCareGiftGroup.GIFT_ID, "").trim();
            time = jo.getLong(colName.DollarHeartCustomerCareGiftGroup.TIME, 0);
            money_value = jo.getLong(colName.DollarHeartCustomerCareGiftGroup.MONEY_VALUE, 0);
            gift_amount = jo.getLong(colName.DollarHeartCustomerCareGiftGroup.GIFT_AMOUNT, 0);
            is_received_noti = jo.getBoolean(colName.DollarHeartCustomerCareGiftGroup.IS_RECEIVED_NOTIFICATION, false);
            program = jo.getString(colName.DollarHeartCustomerCareGiftGroup.PROGRAM, "");
            tranId = jo.getLong(colName.DollarHeartCustomerCareGiftGroup.TRAN_ID, 0);
            group = jo.getString(colName.DollarHeartCustomerCareGiftGroup.GROUP, "");
            duration = jo.getInteger(colName.DollarHeartCustomerCareGiftGroup.DURATION, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.DollarHeartCustomerCareGiftGroup.NUMBER, phone_number.trim());
            jo.putString(colName.DollarHeartCustomerCareGiftGroup.GIFT_ID, gift_id.trim());
            jo.putNumber(colName.DollarHeartCustomerCareGiftGroup.TIME, time);
            jo.putNumber(colName.DollarHeartCustomerCareGiftGroup.MONEY_VALUE, money_value);
            jo.putNumber(colName.DollarHeartCustomerCareGiftGroup.GIFT_AMOUNT, gift_amount);
            jo.putBoolean(colName.DollarHeartCustomerCareGiftGroup.IS_RECEIVED_NOTIFICATION, is_received_noti);
            jo.putString(colName.DollarHeartCustomerCareGiftGroup.PROGRAM, program);
            jo.putNumber(colName.DollarHeartCustomerCareGiftGroup.TRAN_ID, tranId);
            jo.putString(colName.DollarHeartCustomerCareGiftGroup.GROUP, group);
            jo.putNumber(colName.DollarHeartCustomerCareGiftGroup.DURATION, duration);
            return jo;
        }
    }


}
