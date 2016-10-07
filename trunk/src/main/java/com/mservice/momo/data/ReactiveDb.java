package com.mservice.momo.data;

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
 * Created by concu on 1/15/16.
 */
public class ReactiveDb {

    private Vertx vertx;
    private Logger logger;

    public ReactiveDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final String Collection, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, Collection)
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

    public void countNumberOfRow(String Collection, JsonObject filter, final Handler<Integer> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, Collection);

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

                callback.handle(arrayList.size());
            }
        });
    }

    public void searchWithFilter(String Collection, JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, Collection);

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

        public String phone_number = "";
        public String giftType = "";
        public String gift_id = "";
        public long time = 0;
        public long gift_amount = 0;
        public long tranId = 0;
        public int duration = 0;
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.ReactiveDbCol.PHONE_NUMBER, "").trim();
            gift_id = jo.getString(colName.ReactiveDbCol.GIFT_ID, "").trim();
            time = jo.getLong(colName.ReactiveDbCol.TIME, 0);
            gift_amount = jo.getLong(colName.ReactiveDbCol.AMOUNT, 0);
            giftType = jo.getString(colName.ReactiveDbCol.GIFT_TYPE, "");
            tranId = jo.getLong(colName.ReactiveDbCol.TRAN_ID, 0);
            duration = jo.getInteger(colName.DollarHeartCustomerCareGiftGroup.DURATION, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ReactiveDbCol.PHONE_NUMBER, phone_number.trim());
            jo.putString(colName.ReactiveDbCol.GIFT_ID, gift_id.trim());
            jo.putNumber(colName.ReactiveDbCol.TIME, time);
            jo.putNumber(colName.ReactiveDbCol.AMOUNT, gift_amount);
            jo.putString(colName.ReactiveDbCol.GIFT_TYPE, giftType);
            jo.putNumber(colName.ReactiveDbCol.TRAN_ID, tranId);
            jo.putNumber(colName.ReactiveDbCol.DURATION, duration);
            return jo;
        }
    }


}
