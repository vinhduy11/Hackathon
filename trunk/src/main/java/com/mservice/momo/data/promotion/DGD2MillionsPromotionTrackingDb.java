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
 * Created by concu on 3/11/16.
 */
public class DGD2MillionsPromotionTrackingDb {
    private Vertx vertx;
    private Logger logger;

    public DGD2MillionsPromotionTrackingDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionTrackingCol.TABLE)
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

    public void updatePartial(String storeId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionTrackingCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.DGD2MillionsPromotionTrackingCol.STORE_ID, storeId);
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

    public void upSert(String storeId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionTrackingCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.DGD2MillionsPromotionTrackingCol.STORE_ID, storeId);
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
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionTrackingCol.TABLE);

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


    public void findOne(String storeId, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionTrackingCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.DGD2MillionsPromotionTrackingCol.STORE_ID, storeId);
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

        public String storeId = "";
        public String momo_phone = "";
        public long tid_billpay = 0;
        public String serviceId = "";
        public long amount = 0;
        public long tid_cashback = 0;
        public int error_code = 0;
        public int group = 0;
        public long time = 0;
        public String billId = "";
        public Obj() {
        }

        public Obj(JsonObject jo) {
            storeId = jo.getString(colName.DGD2MillionsPromotionTrackingCol.STORE_ID, "");;
            momo_phone = jo.getString(colName.DGD2MillionsPromotionTrackingCol.MOMO_PHONE, "");
            tid_billpay = jo.getLong(colName.DGD2MillionsPromotionTrackingCol.TID_BILLPAY, 0);
            serviceId = jo.getString(colName.DGD2MillionsPromotionTrackingCol.SERVICE_ID, "");
            amount = jo.getLong(colName.DGD2MillionsPromotionTrackingCol.AMOUNT, 0);
            tid_cashback = jo.getLong(colName.DGD2MillionsPromotionTrackingCol.TID_CASHBACK, 0);
            error_code = jo.getInteger(colName.DGD2MillionsPromotionTrackingCol.ERROR_CODE, 0);
            group = jo.getInteger(colName.DGD2MillionsPromotionTrackingCol.GROUP, 0);
            time = jo.getLong(colName.DGD2MillionsPromotionTrackingCol.TIME, 0);
            billId = jo.getString(colName.DGD2MillionsPromotionTrackingCol.BILL_ID, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.DGD2MillionsPromotionTrackingCol.STORE_ID, storeId);
            jo.putString(colName.DGD2MillionsPromotionTrackingCol.MOMO_PHONE, momo_phone);
            jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.TID_BILLPAY, tid_billpay);
            jo.putString(colName.DGD2MillionsPromotionTrackingCol.SERVICE_ID, serviceId);
            jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.AMOUNT, amount);
            jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.TID_CASHBACK, tid_cashback);
            jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.ERROR_CODE, error_code);
            jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.GROUP, group);
            jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.TIME, time);
            jo.putString(colName.DGD2MillionsPromotionTrackingCol.BILL_ID, billId);
            return jo;
        }
    }
}
