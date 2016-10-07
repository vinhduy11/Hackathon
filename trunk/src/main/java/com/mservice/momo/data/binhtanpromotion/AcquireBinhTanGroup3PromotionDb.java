package com.mservice.momo.data.binhtanpromotion;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 5/5/16.
 */
public class AcquireBinhTanGroup3PromotionDb {
    private Vertx vertx;
    private Logger logger;

    public AcquireBinhTanGroup3PromotionDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanGroup3PromotionCol.TABLE)
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

    public void upsertPartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanGroup3PromotionCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, phoneNumber);
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
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanGroup3PromotionCol.TABLE);

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

    public void findOne(String phoneNumber, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanGroup3PromotionCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, phoneNumber);
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

    public void findAndModifyUsedVoucher(String phoneNumber, String giftId, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanGroup3PromotionCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.AcquireBinhTanGroup3PromotionCol.GIFT_ID, giftId);
        match.putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject objNew = new JsonObject();
        objNew.putBoolean(colName.AcquireBinhTanGroup3PromotionCol.IS_USED, true);


        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);

        //update
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.MULTI, true);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyUsedVoucher: " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joBinhTan = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyUsedVoucher json is not null " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  joBinhTan);
                    if(joBinhTan != null)
                    {
                        Obj referralObj = new Obj(joBinhTan);
                        callback.handle(referralObj);
                    }
                    else {
                        callback.handle(null);
                    }
                }
                else {
                    callback.handle(null);
                }
            }
        });
    }

    public static class Obj {
        public String phoneNumber = "";
        public long tid_billpay = 0;
        public long amount_billpay = 0;
        public long tid_cashin = 0;
        public long time_cashin = 0;
        public long amount_cashin = 0;
        public long tid_bonus = 0;
        public long amount_bonus = 0;
        public long time_bonus = 0;
        public String giftId = "";
        public boolean isUsed = false;
        public boolean extra_bonus = false;
        public Obj() {
        }

        public Obj(JsonObject jo) {

            phoneNumber = jo.getString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, "");
            tid_billpay = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.TID_BILLPAY, 0);
            amount_billpay = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.AMOUNT_BILLPAY, 0);
            tid_cashin = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.TID_CASHIN, 0);
            time_cashin = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.TIME_CASHIN, 0);
            amount_cashin = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.AMOUNT_CASHIN, 0);
            tid_bonus = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.TID_BONUS, 0);
            amount_bonus = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.AMOUNT_BONUS, 0);
            time_bonus = jo.getLong(colName.AcquireBinhTanGroup3PromotionCol.TIME_BONUS, 0);
            isUsed = jo.getBoolean(colName.AcquireBinhTanGroup3PromotionCol.IS_USED, false);
            giftId = jo.getString(colName.AcquireBinhTanGroup3PromotionCol.GIFT_ID, "");
            extra_bonus = jo.getBoolean(colName.AcquireBinhTanGroup3PromotionCol.EXTRA_BONUS, false);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, phoneNumber);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.TID_BILLPAY, tid_billpay);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.AMOUNT_BILLPAY, amount_billpay);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.TID_CASHIN, tid_cashin);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.TIME_CASHIN, time_cashin);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.AMOUNT_CASHIN, amount_cashin);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.TID_BONUS, tid_bonus);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.AMOUNT_BONUS, amount_bonus);
            jo.putNumber(colName.AcquireBinhTanGroup3PromotionCol.TIME_BONUS, time_bonus);
            jo.putBoolean(colName.AcquireBinhTanGroup3PromotionCol.IS_USED, isUsed);
            jo.putString(colName.AcquireBinhTanGroup3PromotionCol.GIFT_ID, giftId);
            jo.putBoolean(colName.AcquireBinhTanGroup3PromotionCol.EXTRA_BONUS, extra_bonus);
            return jo;
        }
    }
}
