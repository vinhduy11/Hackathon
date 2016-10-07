package com.mservice.momo.data.binhtanpromotion;

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
 * Created by KhoaNguyen on 6/13/2016.
 */
public class BinhTanDgdTransTrackingDb {

    private Vertx vertx;
    private Logger logger;

    public BinhTanDgdTransTrackingDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.BinhTanDgdTransTracking.TABLE)
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

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BinhTanDgdTransTracking.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);
        query.putObject(MongoKeyWords.SORT, new JsonObject().putNumber(colName.BinhTanDgdTransTracking.TIME, -1));

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

    public void groupByNumberWallet(int program_number, final Handler<JsonArray> callback){

        JsonObject sumNumber = new JsonObject()
                .putNumber("$sum", 1);

        JsonObject grouper = new JsonObject()
                .putString("_id", "$has_bonus")
                .putObject("count", sumNumber);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.COLLECTION, colName.BinhTanDgdTransTracking.TABLE);
        query.putString("action", "aggregate");

        query.putObject(MongoKeyWords.GROUPER, grouper);
        query.putObject(MongoKeyWords.MATCHER, new JsonObject());
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

        public String phoneNumber = "";
        public long tid = 0;
        public long amount = 0;
        public long time = 0;
        public String customerPhone = "";
        public String os = "";
        public Obj() {
        }

        public Obj(JsonObject jo) {

            phoneNumber = jo.getString(colName.BinhTanDgdTransTracking.PHONE_NUMBER, "");
            tid = jo.getLong(colName.BinhTanDgdTransTracking.TRAN_ID, 0);
            amount= jo.getLong(colName.BinhTanDgdTransTracking.AMOUNT, 0);
            time = jo.getLong(colName.BinhTanDgdTransTracking.TIME, 0);
            customerPhone = jo.getString(colName.BinhTanDgdTransTracking.CUSTOMER_PHONE, "");
            os = jo.getString(colName.BinhTanDgdTransTracking.CUSTOMER_OS, "");

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.BinhTanDgdTransTracking.PHONE_NUMBER, phoneNumber);
            jo.putNumber(colName.BinhTanDgdTransTracking.TRAN_ID, tid);
            jo.putNumber(colName.BinhTanDgdTransTracking.AMOUNT, amount);
            jo.putNumber(colName.BinhTanDgdTransTracking.TIME, time);
            jo.putString(colName.BinhTanDgdTransTracking.CUSTOMER_PHONE, customerPhone);
            jo.putString(colName.BinhTanDgdTransTracking.CUSTOMER_OS, os);
            return jo;
        }
    }

}
