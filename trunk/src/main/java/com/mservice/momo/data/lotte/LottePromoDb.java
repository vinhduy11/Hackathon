package com.mservice.momo.data.lotte;

import com.mservice.momo.data.binhtanpromotion.AcquireBinhTanUserPromotionDb;
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
 * Created by manhly on 03/08/2016.
 */
public class LottePromoDb {
    private Vertx vertx;
    private Logger logger;

    public LottePromoDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;

    }
    public void insert(final LottePromoDb.Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.LottePromoCol.TABLE)
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

    public void findByPhone(String phoneNumber, final Handler<LottePromoDb.Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.LottePromoCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.LottePromoCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                LottePromoDb.Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new LottePromoDb.Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }
    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<LottePromoDb.Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.LottePromoCol.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<LottePromoDb.Obj> arrayList = new ArrayList<LottePromoDb.Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        LottePromoDb.Obj obj = new LottePromoDb.Obj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }


    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.LottePromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.LottePromoCol.PHONE_NUMBER, phoneNumber);
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

    public static class Obj {
        public String phoneNumber = "";
        public long registerTime = 0;
        public int tranType = 0;
        public long tranId = 0;
        public long bonusTime = 0;
        public long tranAmount = 0;
        public long bonusAmount = 0;
        public int error = 0;
        public boolean usedGift = false;
        public long updateTime = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {

            phoneNumber = jo.getString(colName.LottePromoCol.PHONE_NUMBER, "");
            registerTime = jo.getLong(colName.LottePromoCol.REGISTER_TIME, 0);
            tranType = jo.getInteger(colName.LottePromoCol.TRAN_TYPE, 0);
            tranId = jo.getLong(colName.LottePromoCol.TRAN_ID, 0);
            bonusTime = jo.getLong(colName.LottePromoCol.BONUS_TIME, 0);
            tranAmount = jo.getLong(colName.LottePromoCol.TRAN_AMOUNT, 0);
            bonusAmount = jo.getLong(colName.LottePromoCol.BONUS_AMOUNT, 0);
            error = jo.getInteger(colName.LottePromoCol.ERROR, 0);
            usedGift = jo.getBoolean(colName.LottePromoCol.USED_GIFT, false);
            updateTime = jo.getLong(colName.LottePromoCol.UPDATE_TIME, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.LottePromoCol.PHONE_NUMBER, phoneNumber);
            jo.putNumber(colName.LottePromoCol.REGISTER_TIME, registerTime);
            jo.putNumber(colName.LottePromoCol.TRAN_TYPE, tranType);
            jo.putNumber(colName.LottePromoCol.TRAN_ID, tranId);
            jo.putNumber(colName.LottePromoCol.BONUS_TIME, bonusTime);
            jo.putNumber(colName.LottePromoCol.TRAN_AMOUNT, tranAmount);
            jo.putNumber(colName.LottePromoCol.BONUS_AMOUNT, bonusAmount);
            jo.putNumber(colName.LottePromoCol.ERROR, error);
            jo.putBoolean(colName.LottePromoCol.USED_GIFT, usedGift);
            jo.putNumber(colName.LottePromoCol.UPDATE_TIME, updateTime);
            return jo;
        }
    }
}
