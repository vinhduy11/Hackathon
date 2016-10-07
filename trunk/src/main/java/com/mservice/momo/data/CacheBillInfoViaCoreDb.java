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
 * Created by concu on 7/8/15.
 */
public class CacheBillInfoViaCoreDb {

    private Vertx vertx;
    private Logger logger;

    public CacheBillInfoViaCoreDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.CacheBillInfoViaCore.TABLE)
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

    public void updatePartial(String billId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CacheBillInfoViaCore.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CacheBillInfoViaCore.BILL_ID, billId);
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
        query.putString(MongoKeyWords.COLLECTION, colName.CacheBillInfoViaCore.TABLE);

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

    public void findOne(String billId, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.CacheBillInfoViaCore.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.CacheBillInfoViaCore.BILL_ID, billId);
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

        //        public int proxyResponseCode = 0;
//
//        public String proxyResponseMessage = "";
//
//        public long totalAmount = 0;
//
//        public String parentTranId = "";
//
//        public String customerId = "";
//
//        public String customerName = "";
//
//        public String customerInfo = "";
//
//        public String customerPhone = "";
//
//        public String customerAddress = "";
//
//        public String codeItem = ""; // billId
//
//        public String htmlContent = "";
//
//        public String serviceCode = "";
//
//        public JsonArray bill_infos = new JsonArray();
        public String billId = "";

        public String billInfo = "";

        public String number = "";

        public String providerId = "";

        public int count = 0;

        public int rcode = 0;

        public long checkedTime = 0;

        public long againCheckTime = 0; // Thoi gian de check lai.

        public JsonObject jsonResult = new JsonObject();

        public Obj() {
        }

        public Obj(JsonObject jo) {

            billId = jo.getString(colName.CacheBillInfoViaCore.BILL_ID, "");

            billInfo = jo.getString(colName.CacheBillInfoViaCore.BILL_INFO, "");
            number = jo.getString(colName.CacheBillInfoViaCore.NUMBER, "");
            providerId = jo.getString(colName.CacheBillInfoViaCore.PROVIDER_ID, "");

            rcode = jo.getInteger(colName.CacheBillInfoViaCore.RCODE, 0);

            count = jo.getInteger(colName.CacheBillInfoViaCore.COUNT, 0);
            checkedTime = jo.getLong(colName.CacheBillInfoViaCore.CHECKED_TIME, 0);
            againCheckTime = jo.getLong(colName.CacheBillInfoViaCore.END_CHECKED_TIME, 0);

            jsonResult = jo.getObject(colName.CacheBillInfoViaCore.JSON_RESULT, new JsonObject());

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();


            jo.putString(colName.CacheBillInfoViaCore.BILL_ID, billId);

            jo.putString(colName.CacheBillInfoViaCore.BILL_INFO, billInfo);
            jo.putString(colName.CacheBillInfoViaCore.NUMBER, number);
            jo.putString(colName.CacheBillInfoViaCore.PROVIDER_ID, providerId);
            jo.putNumber(colName.CacheBillInfoViaCore.RCODE, rcode);
            jo.putNumber(colName.CacheBillInfoViaCore.COUNT, count);
            jo.putNumber(colName.CacheBillInfoViaCore.CHECKED_TIME, checkedTime);
            jo.putNumber(colName.CacheBillInfoViaCore.END_CHECKED_TIME, againCheckTime);

            jo.putObject(colName.CacheBillInfoViaCore.JSON_RESULT, jsonResult);

            return jo;
        }
    }
}
