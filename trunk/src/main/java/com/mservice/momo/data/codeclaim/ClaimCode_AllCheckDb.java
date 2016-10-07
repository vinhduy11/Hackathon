package com.mservice.momo.data.codeclaim;

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
 * Created by concu on 2/23/16.
 */
public class ClaimCode_AllCheckDb {

    private Vertx vertx;
    private Logger logger;

    public ClaimCode_AllCheckDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(int giftNumber, String promotionId, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, promotionId + "_AllCheck")
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson(giftNumber));


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

    public void updatePartial(String code, String promotionId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, promotionId + "_AllCheck");
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ClaimCode_AllCheckCols.CODE, code);
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

    public void updatePartialViaPhone(String phone, String promotionId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, promotionId + "_AllCheck");
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ClaimCode_AllCheckCols.PHONE, phone);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.MULTI, true);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                logger.info("Thong tin khi update claim mongo "  + obj.toString());
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void upSert(String code, String promotionId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, promotionId + "_AllCheck");
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ClaimCode_AllCheckCols.CODE, code);
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

    public void searchWithFilter(String promotionId, JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, promotionId + "_AllCheck");

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


    public void findOne(String code, String promotionId, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, promotionId + "_AllCheck");

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.ClaimCode_AllCheckCols.CODE, code);
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

    public void findClaimedCodeUserInfo(String promotionId, String phoneNumber, String last_imei, String code, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, promotionId + "_AllCheck");

        JsonObject jsonPhone = new JsonObject();

        jsonPhone.putString(colName.ClaimCode_AllCheckCols.PHONE, phoneNumber);

        JsonObject jsonImei = new JsonObject();

        jsonImei.putString(colName.ClaimCode_AllCheckCols.DEVICE_IMEI, last_imei);

        JsonObject jsonCode = new JsonObject();

        jsonCode.putString(colName.ClaimCode_AllCheckCols.CODE, code);

        JsonArray orArray = new JsonArray();
        orArray.addObject(jsonPhone);
        orArray.addObject(jsonImei);
        orArray.addObject(jsonCode);

        JsonObject matcher = new JsonObject();
        matcher.putArray(MongoKeyWords.OR, orArray);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);
        query.putObject(MongoKeyWords.MATCHER, matcher);

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

        public String code = "";
        public String phone = "";
        public long time = 0;
        public JsonArray gift_id_arr = new JsonArray();
        public long gift_time = 0;
        public long gift_tid = 0;
        public long gift_amount = 0;
        public long money_amount = 0;
        public int money_status = 0;
        public long money_time = 0;
        public long money_tid = 0;
        public long money_rollback_tid = 0;
        public long trans_pay_bill_tid = 0;
        public String device_imei = "";
        public JsonObject joExtra = new JsonObject();
        public Obj() {
        }

        public Obj(JsonObject jo) {
            code = jo.getString(colName.ClaimCode_AllCheckCols.CODE, "");
            phone = jo.getString(colName.ClaimCode_AllCheckCols.PHONE, "");
            time = jo.getLong(colName.ClaimCode_AllCheckCols.TIME, 0);
            gift_id_arr = jo.getArray(colName.ClaimCode_AllCheckCols.GIFT_ID, new JsonArray());
            gift_time = jo.getLong(colName.ClaimCode_AllCheckCols.GIFT_TIME, 0);
            gift_tid = jo.getLong(colName.ClaimCode_AllCheckCols.GIFT_TID, 0);
            gift_amount = jo.getLong(colName.ClaimCode_AllCheckCols.GIFT_AMOUNT, 0);
            money_amount = jo.getLong(colName.ClaimCode_AllCheckCols.MOMO_MONEY_AMOUNT, 0);
            money_status = jo.getInteger(colName.ClaimCode_AllCheckCols.MONEY_STATUS, 0);
            money_time = jo.getLong(colName.ClaimCode_AllCheckCols.MONEY_TIME, 0);
            money_tid = jo.getLong(colName.ClaimCode_AllCheckCols.MONEY_TID, 0);
            money_rollback_tid =  jo.getLong(colName.ClaimCode_AllCheckCols.MONEY_ROLLBACK_TID, 0);
            device_imei = jo.getString(colName.ClaimCode_AllCheckCols.DEVICE_IMEI, "");
            trans_pay_bill_tid =  jo.getLong(colName.ClaimCode_AllCheckCols.TRANS_PAY_BILL_TID, 0);
            joExtra = jo.getObject(colName.ClaimCode_AllCheckCols.JSON_EXTRA, new JsonObject());
        }

        public JsonObject toJson(int number) {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ClaimCode_AllCheckCols.CODE, code);
            jo.putString(colName.ClaimCode_AllCheckCols.PHONE, phone);
            jo.putNumber(colName.ClaimCode_AllCheckCols.TIME, time);
            String col = colName.ClaimCode_AllCheckCols.GIFT_TID;
            if(gift_id_arr.size() == number)
            {
                for(int i = 0; i < number; i++)
                {
                    int numCol = number + 1;
                    col = colName.ClaimCode_AllCheckCols.GIFT_ID + "_" + numCol;
                    jo.putString(col, gift_id_arr.get(number).toString());
                }
            }
            else {
                for(int i = 0; i < number; i++)
                {
                    int numCol = number + 1;
                    col = colName.ClaimCode_AllCheckCols.GIFT_ID + "_" + numCol;
                    jo.putString(col, "");
                }
            }
            jo.putNumber(colName.ClaimCode_AllCheckCols.GIFT_TIME, gift_time);
            jo.putNumber(colName.ClaimCode_AllCheckCols.GIFT_TID, gift_tid);
            jo.putNumber(colName.ClaimCode_AllCheckCols.GIFT_AMOUNT, gift_amount);
            jo.putNumber(colName.ClaimCode_AllCheckCols.MOMO_MONEY_AMOUNT, money_amount);
            jo.putNumber(colName.ClaimCode_AllCheckCols.MONEY_STATUS, money_status);
            jo.putNumber(colName.ClaimCode_AllCheckCols.MONEY_TIME, money_time);
            jo.putNumber(colName.ClaimCode_AllCheckCols.MONEY_TID, money_tid);
            jo.putNumber(colName.ClaimCode_AllCheckCols.MONEY_ROLLBACK_TID, money_rollback_tid);
            jo.putString(colName.ClaimCode_AllCheckCols.DEVICE_IMEI, device_imei);
            jo.putNumber(colName.ClaimCode_AllCheckCols.TRANS_PAY_BILL_TID, trans_pay_bill_tid);
            jo.putObject(colName.ClaimCode_AllCheckCols.JSON_EXTRA, joExtra);
            return jo;
        }
    }


}