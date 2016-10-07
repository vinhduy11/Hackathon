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
 * Created by KhoaNguyen on 9/21/2016.
 */
public class IPOSTransactionInfoDb {
    private Vertx vertx;
    private Logger logger;

    public IPOSTransactionInfoDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.IPOSTransactionInfoCols.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.IPOSTransactionInfoCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.IPOSTransactionInfoCols.MERCHANT_ID, merchant_id);
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

    public void findOne(String paymentCode, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.IPOSTransactionInfoCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.IPOSTransactionInfoCols.PAYMENT_CODE, paymentCode);
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

        public String payment_code = "";
        public long time = 0;
        public String merchant_id = "";
        public String decrypted_payment_code = "";
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            merchant_id = jo.getString(colName.IPOSTransactionInfoCols.MERCHANT_ID, "").trim();
            payment_code = jo.getString(colName.IPOSTransactionInfoCols.PAYMENT_CODE, "").trim();
            time = jo.getLong(colName.IPOSTransactionInfoCols.TIME, 0);
            decrypted_payment_code = jo.getString(colName.IPOSTransactionInfoCols.DECRYPTED_PAYMENT_CODE, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.IPOSTransactionInfoCols.MERCHANT_ID, merchant_id.trim());
            jo.putString(colName.IPOSTransactionInfoCols.PAYMENT_CODE, payment_code.trim());
            jo.putNumber(colName.IPOSTransactionInfoCols.TIME, time);
            jo.putString(colName.IPOSTransactionInfoCols.DECRYPTED_PAYMENT_CODE, decrypted_payment_code);

            return jo;
        }
    }
}
