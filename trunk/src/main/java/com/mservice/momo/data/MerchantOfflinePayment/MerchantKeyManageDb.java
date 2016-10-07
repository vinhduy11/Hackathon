package com.mservice.momo.data.MerchantOfflinePayment;

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
 * Created by concu on 10/5/15.
 */
public class MerchantKeyManageDb {
    private Vertx vertx;
    private Logger logger;

    public MerchantKeyManageDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.MerchantKeyManageCols.TABLE)
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

    public void updatePartial(String merchant_id
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.MerchantKeyManageCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.MerchantKeyManageCols.MERCHANT_ID, merchant_id);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("status", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void upsertPartial(int merchant_id
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.MerchantKeyManageCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.MerchantKeyManageCols.MERCHANT_ID, merchant_id);
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
        query.putString(MongoKeyWords.COLLECTION, colName.MerchantKeyManageCols.TABLE);

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

    public void findOne(String merchant_id, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.MerchantKeyManageCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.MerchantKeyManageCols.MERCHANT_ID, merchant_id);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj merchantObj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    merchantObj = new Obj(joResult);
                }

                callback.handle(merchantObj);
            }
        });
    }

    public static class Obj {

        public String merchant_id = "";
        public String merchant_name = "";
        public String merchant_number = "";
        public String dev_pub_key = "";
        public String dev_pri_key = "";
        public String pro_pub_key = "";
        public String pro_pri_key = "";
        public String merchant_infos = "";
        public String agent_name = "";
        public String service_id = "";
        public String service_type = "";
        public String busname = "";
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            merchant_id = jo.getString(colName.MerchantKeyManageCols.MERCHANT_ID, "").trim();
            merchant_name = jo.getString(colName.MerchantKeyManageCols.MERCHANT_NAME, "").trim();
            merchant_number = jo.getString(colName.MerchantKeyManageCols.MERCHANT_NUMBER, "").trim();
            dev_pub_key = jo.getString(colName.MerchantKeyManageCols.DEV_PUBLIC_KEY, "");
            dev_pri_key = jo.getString(colName.MerchantKeyManageCols.DEV_PRIVATE_KEY, "");
            pro_pub_key = jo.getString(colName.MerchantKeyManageCols.PRODUCT_PUBLIC_KEY, "");
            pro_pri_key = jo.getString(colName.MerchantKeyManageCols.PRODUCT_PRIVATE_KEY, "");
            merchant_infos = jo.getString(colName.MerchantKeyManageCols.MERCHANT_INFOS, "");
            agent_name = jo.getString(colName.MerchantKeyManageCols.AGENT_NAME, "");
            service_id = jo.getString(colName.MerchantKeyManageCols.SERVICE_ID, "");
            service_type = jo.getString(colName.MerchantKeyManageCols.SERVICE_TYPE, "");
            busname = jo.getString(colName.MerchantKeyManageCols.BUSNAME, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.MerchantKeyManageCols.MERCHANT_ID, merchant_id.trim());
            jo.putString(colName.MerchantKeyManageCols.MERCHANT_NAME, merchant_name.trim());
            jo.putString(colName.MerchantKeyManageCols.MERCHANT_NUMBER, merchant_number.trim());
            jo.putString(colName.MerchantKeyManageCols.DEV_PUBLIC_KEY, dev_pub_key);
            jo.putString(colName.MerchantKeyManageCols.DEV_PRIVATE_KEY, dev_pri_key);
            jo.putString(colName.MerchantKeyManageCols.PRODUCT_PUBLIC_KEY, pro_pub_key);
            jo.putString(colName.MerchantKeyManageCols.PRODUCT_PRIVATE_KEY, pro_pri_key);
            jo.putString(colName.MerchantKeyManageCols.MERCHANT_INFOS, merchant_infos);
            jo.putString(colName.MerchantKeyManageCols.AGENT_NAME, agent_name);
            jo.putString(colName.MerchantKeyManageCols.SERVICE_ID, service_id);
            jo.putString(colName.MerchantKeyManageCols.SERVICE_TYPE, service_type);
            jo.putString(colName.MerchantKeyManageCols.BUSNAME, busname);
            return jo;
        }
    }
}
