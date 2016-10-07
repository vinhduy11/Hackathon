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
 * Created by concu on 9/30/15.
 */
public class MappingWalletOCBPromoDb {

    private Vertx vertx;
    private Logger logger;

    public MappingWalletOCBPromoDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.MappingWalletOCBPromo.TABLE)
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

    public void updatePartial(String customer_id
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.MappingWalletOCBPromo.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.MappingWalletOCBPromo.CUSTOMER_ID, customer_id);
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

    public void upsertWalletBank(String id
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.MappingWalletOCBPromo.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.MappingWalletOCBPromo.CUSTOMER_ID, id);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        //"update": { "$inc": { "seq": 1 } }
//        JsonObject jsonInc = new JsonObject();
//        jsonInc.putNumber(colName.MappingWalletBank.NUMBER_OF_UPDATE, 1);
////        JsonObject jsonInc = new JsonObject();
////        jsonInc.putObject(MongoKeyWords.INCREMENT, jsonValue);
//        joUpdate.putObject(MongoKeyWords.INCREMENT, jsonInc);

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
        query.putString(MongoKeyWords.COLLECTION, colName.MappingWalletOCBPromo.TABLE);

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

    public void findOne(String id, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.MappingWalletOCBPromo.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.MappingWalletOCBPromo.CUSTOMER_ID, id);
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

        public String id = "";
        public String number = ""; // wallet will be get voucher
        public String bank_code = "";
        public long mapping_time = 0;
        public String bank_name = "";
        public String customer_name = "";
        public String customer_id = "";


        public Obj() {
        }

        public Obj(JsonObject jo) {

            id = jo.getString(colName.MappingWalletOCBPromo.CUSTOMER_ID, "");
            number = jo.getString(colName.MappingWalletOCBPromo.NUMBER, "");
            bank_code = jo.getString(colName.MappingWalletOCBPromo.BANK_CODE, "");
            bank_name = jo.getString(colName.MappingWalletOCBPromo.BANK_NAME, "");
            mapping_time = jo.getLong(colName.MappingWalletOCBPromo.MAPPING_TIME, 0);
            customer_name = jo.getString(colName.MappingWalletOCBPromo.CUSTOMER_NAME, "");
            customer_id = jo.getString(colName.MappingWalletOCBPromo.CUSTOMER_ID, "");

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.MappingWalletOCBPromo.CUSTOMER_ID, id);

            jo.putString(colName.MappingWalletOCBPromo.NUMBER, number);
            jo.putString(colName.MappingWalletOCBPromo.BANK_CODE, bank_code);
            jo.putString(colName.MappingWalletOCBPromo.BANK_NAME, bank_name);
            jo.putNumber(colName.MappingWalletOCBPromo.MAPPING_TIME, mapping_time);
            jo.putString(colName.MappingWalletOCBPromo.CUSTOMER_NAME, customer_name);
            jo.putString(colName.MappingWalletOCBPromo.CUSTOMER_ID, customer_id);

            return jo;
        }
    }
}
