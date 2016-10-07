package com.mservice.momo.data.tracking;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 6/9/16.
 */
public class BanknetTransDb {
    private Vertx vertx;
    private Logger logger;

    public BanknetTransDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {
        logger.info("insert BanknetTransDb");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.BanknetTransCol.TABLE)
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

    public void updatePartial(String merchantTransId
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        logger.info("updatePartial BanknetTransDb");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BanknetTransCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.BanknetTransCol.MERCHANT_TRAN_ID, merchantTransId);
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

    public void findOne(String merchantTranId, final Handler<Obj> callback) {
        logger.info("findOne BanknetTransDb");
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.BanknetTransCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.BanknetTransCol.MERCHANT_TRAN_ID, merchantTranId);
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

        public String merchant_tran_id = "";
        public String phoneNumber = "";
        public String tranId = "";
        public String bank_acc = "";
        public String bank_name = "";
        public String bank_id = "";
        public long time_get_otp = 0;
        public int result_get_otp = 0;
        public long time_verify = 0;
        public int result_verify = 0;
        public JsonObject joExtra = new JsonObject();
        public Obj() {
        }

        public Obj(JsonObject jo) {
            merchant_tran_id = jo.getString(colName.BanknetTransCol.MERCHANT_TRAN_ID, "");
            phoneNumber = jo.getString(colName.BanknetTransCol.PHONE_NUMBER, "");
            tranId = jo.getString(colName.BanknetTransCol.TRAN_ID, "");
            bank_acc = jo.getString(colName.BanknetTransCol.BANK_ACC, "");
            bank_name = jo.getString(colName.BanknetTransCol.BANK_NAME, "");
            bank_id= jo.getString(colName.BanknetTransCol.BANK_ID, "");
            time_get_otp = jo.getLong(colName.BanknetTransCol.TIME_GET_OTP, 0);
            result_get_otp = jo.getInteger(colName.BanknetTransCol.RESULT_GET_OTP, 0);
            time_verify = jo.getLong(colName.BanknetTransCol.TIME_VERIFY, 0);
            result_verify = jo.getInteger(colName.BanknetTransCol.RESULT_VERIFY, 0);
            joExtra = jo.getObject(colName.BanknetTransCol.EXTRA, new JsonObject());
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
             jo.putString(colName.BanknetTransCol.MERCHANT_TRAN_ID, merchant_tran_id);
             jo.putString(colName.BanknetTransCol.PHONE_NUMBER, phoneNumber);
             jo.putString(colName.BanknetTransCol.TRAN_ID, tranId);
             jo.putString(colName.BanknetTransCol.BANK_ACC, bank_acc);
             jo.putString(colName.BanknetTransCol.BANK_NAME, bank_name);
             jo.putString(colName.BanknetTransCol.BANK_ID, bank_id);
             jo.putNumber(colName.BanknetTransCol.TIME_GET_OTP, time_get_otp);
             jo.putNumber(colName.BanknetTransCol.RESULT_GET_OTP, result_get_otp);
             jo.putNumber(colName.BanknetTransCol.TIME_VERIFY, time_verify);
             jo.putNumber(colName.BanknetTransCol.RESULT_VERIFY, result_verify);
             jo.putObject(colName.BanknetTransCol.EXTRA,joExtra);
            return jo;
        }
    }
}
