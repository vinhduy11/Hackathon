package com.mservice.momo.data.tracking;

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
 * Created by concu on 4/13/16.
 */
public class Tracking123PhimDb {
    private Vertx vertx;
    private Logger logger;

    public Tracking123PhimDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {
        logger.info("insert Tracking123PhimDb");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.Tracking123PhimCol.TABLE)
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
        logger.info("updatePartial Tracking123PayDb");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Tracking123PhimCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.Tracking123PhimCol.BILL_ID, billId);
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

    public void upSert(String billId
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        logger.info("upSert Tracking123PayDb");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Tracking123PhimCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.Tracking123PhimCol.BILL_ID, billId);
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
        logger.info("search with filter Tracking123PayDb");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Tracking123PhimCol.TABLE);

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
        logger.info("findOne Tracking123PayDb");
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Tracking123PhimCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.Tracking123PhimCol.BILL_ID, billId);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    logger.info(joResult.toString());
                    obj = new Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public static class Obj {

        public String billId = "";
        public String phoneNumber = "";
        public long time = 0;
        public long tranId = 0;
        public JsonObject joExtra = new JsonObject();
        public Obj() {
        }

        public Obj(JsonObject jo) {
            billId = jo.getString(colName.Tracking123PhimCol.BILL_ID, "");
            phoneNumber = jo.getString(colName.Tracking123PhimCol.PHONE_NUMBER, "");
            time = jo.getLong(colName.Tracking123PhimCol.TIME, 0);
            tranId = jo.getLong(colName.Tracking123PhimCol.TRAN_ID, 0);
            joExtra = jo.getObject(colName.Tracking123PhimCol.EXTRA, new JsonObject());
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.Tracking123PhimCol.BILL_ID, billId);
            jo.putString(colName.Tracking123PhimCol.PHONE_NUMBER, phoneNumber);
            jo.putNumber(colName.Tracking123PhimCol.TIME, time);
            jo.putNumber(colName.Tracking123PhimCol.TRAN_ID, tranId);
            jo.putObject(colName.Tracking123PhimCol.EXTRA, joExtra);
            return jo;
        }
    }

}

