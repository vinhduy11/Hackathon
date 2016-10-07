package com.mservice.momo.data.servicegiftrule;

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
 * Created by concu on 2/18/16.
 */
public class CheckServiceGiftRuleDb {

    private Vertx vertx;
    private Logger logger;

    public CheckServiceGiftRuleDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.CheckServiceGiftRuleCol.TABLE)
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

    /**
     * update or insert new document with criteria billId and providerId
     * @param obj the object that will insert or update to DB
     * @param callback the result of this operation
     */
    public void upsert(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CheckServiceGiftRuleCol.TABLE);

        JsonObject joUp = obj.toJson();
        JsonObject criteria   = new JsonObject();

        //criteria.putNumber(colName.BillInfo.number,obj.number);
        criteria.putString(colName.CheckServiceGiftRuleCol.SERVICE_ID, obj.serviceId);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUp);
        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject obj = message.body();

                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    public void updateAllField(JsonObject joUpdate, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CheckServiceGiftRuleCol.TABLE);


        JsonObject criteria = new JsonObject();
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);
        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.MULTI, true);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject obj = message.body();

                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CheckServiceGiftRuleCol.TABLE);

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

        public String serviceId = "";
        public long min_amount = 0;
        public boolean check_service = false;
        public boolean check_all = false;
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            serviceId = jo.getString(colName.CheckServiceGiftRuleCol.SERVICE_ID, "").trim();
            min_amount = jo.getLong(colName.CheckServiceGiftRuleCol.MIN_AMOUNT, 0);
            check_service = jo.getBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED, false);
            check_all  = jo.getBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED_ALL, false);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.CheckServiceGiftRuleCol.SERVICE_ID, serviceId.trim());
            jo.putNumber(colName.CheckServiceGiftRuleCol.MIN_AMOUNT, min_amount);
            jo.putBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED, check_service);
            jo.putBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED_ALL, check_all);
            return jo;
        }
    }
}
