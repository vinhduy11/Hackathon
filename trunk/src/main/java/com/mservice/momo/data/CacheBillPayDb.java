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
 * Created by concu on 7/7/15.
 */
public class CacheBillPayDb {

    private Vertx vertx;
    private Logger logger;

    public CacheBillPayDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.CacheBillPay.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.CacheBillPay.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CacheBillPay.BILL_ID, billId);
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

    public void updatePartialWithOutInsert(String billId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CacheBillPay.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CacheBillPay.BILL_ID, billId);
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

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CacheBillPay.TABLE);

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
        query.putString(MongoKeyWords.COLLECTION, colName.CacheBillPay.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.CacheBillPay.BILL_ID, billId);
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

        public long total_amount = 0;

        public JsonArray customer_info = new JsonArray();

        public JsonArray array_price = new JsonArray();

        public JsonArray extra_info = new JsonArray();

        public int count = 0;

        public long checkedTime = 0;

        public long againCheckTime = 0; // Thoi gian de check lai.

        public String service_id = "";

        public Obj() {
        }

        public Obj(JsonObject jo) {

//            proxyResponseCode = jo.getInteger(colName.CacheBillPay.PROXY_RESPONSE_CODE, 0);
//            proxyResponseMessage = jo.getString(colName.CacheBillPay.PROXY_RESPONSE_MESSAGE, "");
//            totalAmount = jo.getLong(colName.CacheBillPay.TOTAL_AMOUNT, 0);
//            parentTranId = jo.getString(colName.CacheBillPay.PARENT_TRANS_ID, "");
//            customerId = jo.getString(colName.CacheBillPay.CUSTOMER_ID, "");
//            customerName = jo.getString(colName.CacheBillPay.CUSTOMER_NAME, "");
//            customerInfo = jo.getString(colName.CacheBillPay.CUSTOMER_INFO, "");
//            customerPhone = jo.getString(colName.CacheBillPay.CUSTOMER_PHONE, "");
//            customerAddress = jo.getString(colName.CacheBillPay.CUSTOMER_ADDRESS, "");
//            codeItem = jo.getString(colName.CacheBillPay.CODE_ITEM, "");
//            htmlContent = jo.getString(colName.CacheBillPay.HTML_CONTENT, "");
//            serviceCode = jo.getString(colName.CacheBillPay.SERVICE_CODE, "");
//            bill_infos = jo.getArray(colName.CacheBillPay.BILL_LIST, new JsonArray());


            billId = jo.getString(colName.CacheBillPay.BILL_ID, "");

            total_amount = jo.getLong(colName.CacheBillPay.TOTAL_AMOUNT, 0);
            customer_info = jo.getArray(colName.CacheBillPay.CUSTOMER_INFO, new JsonArray());
            array_price = jo.getArray(colName.CacheBillPay.ARRAY_PRICE, new JsonArray());
            extra_info = jo.getArray(colName.CacheBillPay.EXTRA_INFO, new JsonArray());


            count = jo.getInteger(colName.CacheBillPay.COUNT, 0);
            checkedTime = jo.getLong(colName.CacheBillPay.CHECK_TIME, 0);
            againCheckTime = jo.getLong(colName.CacheBillPay.CHECK_TIME_AGAIN, 0);

            service_id = jo.getString(colName.CacheBillPay.SERVICE_ID, "");

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

//            jo.putNumber(colName.CacheBillPay.PROXY_RESPONSE_CODE, proxyResponseCode);
//            jo.putString(colName.CacheBillPay.PROXY_RESPONSE_MESSAGE, proxyResponseMessage);
//            jo.putNumber(colName.CacheBillPay.TOTAL_AMOUNT, totalAmount);
//            jo.putString(colName.CacheBillPay.PARENT_TRANS_ID, parentTranId);
//            jo.putString(colName.CacheBillPay.CUSTOMER_ID, customerId);
//
//            jo.putString(colName.CacheBillPay.CUSTOMER_NAME, customerName);
//            jo.putString(colName.CacheBillPay.CUSTOMER_INFO, customerInfo);
//            jo.putString(colName.CacheBillPay.CUSTOMER_PHONE, customerPhone);
//            jo.putString(colName.CacheBillPay.CUSTOMER_ADDRESS, customerAddress);
//
//            jo.putString(colName.CacheBillPay.CODE_ITEM, codeItem);
//            jo.putString(colName.CacheBillPay.HTML_CONTENT, htmlContent);
//            jo.putString(colName.CacheBillPay.SERVICE_CODE, serviceCode);
//
//            jo.putArray(colName.CacheBillPay.BILL_LIST, bill_infos);

            jo.putString(colName.CacheBillPay.BILL_ID, billId);

            jo.putNumber(colName.CacheBillPay.TOTAL_AMOUNT, total_amount);
            jo.putArray(colName.CacheBillPay.CUSTOMER_INFO, customer_info);
            jo.putArray(colName.CacheBillPay.ARRAY_PRICE, array_price);
            jo.putArray(colName.CacheBillPay.EXTRA_INFO, extra_info);

            jo.putNumber(colName.CacheBillPay.COUNT, count);
            jo.putNumber(colName.CacheBillPay.CHECK_TIME, checkedTime);
            jo.putNumber(colName.CacheBillPay.CHECK_TIME_AGAIN, againCheckTime);
            jo.putString(colName.CacheBillPay.SERVICE_ID, service_id);

            return jo;
        }
    }
}
