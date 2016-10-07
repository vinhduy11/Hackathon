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
public class DGD2MillionsPromotionMembersDb {
    private Vertx vertx;
    private Logger logger;

    public DGD2MillionsPromotionMembersDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionMembersCol.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionMembersCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.DGD2MillionsPromotionMembersCol.STORE_ID, storeId);
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
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionMembersCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.DGD2MillionsPromotionMembersCol.STORE_ID, storeId);
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
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionMembersCol.TABLE);

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
        query.putString(MongoKeyWords.COLLECTION, colName.DGD2MillionsPromotionMembersCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.DGD2MillionsPromotionMembersCol.STORE_ID, storeId);
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

        public String storeId = "";
        public String momo_phone = "";
        public String store_name = "";
        public long register_time = 0;
        public boolean is_actived = false;
        public long actived_time = 0;
        public long tid_fee = 0;
        public boolean end_bonus_3months = false;
        public boolean end_bonus_2mils = false;
        public int error_code = -1;
        public long register_end_time = 0;
        public long bonus_end_time = 0;
        public Obj() {
        }

        public Obj(JsonObject jo) {
            storeId = jo.getString(colName.DGD2MillionsPromotionMembersCol.STORE_ID, "");
            momo_phone = jo.getString(colName.DGD2MillionsPromotionMembersCol.MOMO_PHONE, "");
            store_name = jo.getString(colName.DGD2MillionsPromotionMembersCol.STORE_NAME, "");
            register_time = jo.getLong(colName.DGD2MillionsPromotionMembersCol.REGISTER_TIME, 0);
            is_actived = jo.getBoolean(colName.DGD2MillionsPromotionMembersCol.IS_ACTIVED, false);
            actived_time = jo.getLong(colName.DGD2MillionsPromotionMembersCol.ACTIVED_TIME, 0);
            tid_fee = jo.getLong(colName.DGD2MillionsPromotionMembersCol.TID_FEE, 0);
            end_bonus_3months = jo.getBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_3MONTHS, false);
            end_bonus_2mils = jo.getBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_2MIL, false);
            error_code = jo.getInteger(colName.DGD2MillionsPromotionMembersCol.ERROR_CODE, -1);
            register_end_time = jo.getLong(colName.DGD2MillionsPromotionMembersCol.REGISTER_END_TIME, 0);
            bonus_end_time = jo.getLong(colName.DGD2MillionsPromotionMembersCol.BONUS_END_TIME, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.DGD2MillionsPromotionMembersCol.STORE_ID, storeId);
            jo.putString(colName.DGD2MillionsPromotionMembersCol.MOMO_PHONE, momo_phone);
            jo.putString(colName.DGD2MillionsPromotionMembersCol.STORE_NAME, store_name);
            jo.putNumber(colName.DGD2MillionsPromotionMembersCol.REGISTER_TIME, register_time);
            jo.putBoolean(colName.DGD2MillionsPromotionMembersCol.IS_ACTIVED, is_actived);
            jo.putNumber(colName.DGD2MillionsPromotionMembersCol.ACTIVED_TIME, actived_time);
            jo.putNumber(colName.DGD2MillionsPromotionMembersCol.TID_FEE, tid_fee);
            jo.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_3MONTHS, end_bonus_3months);
            jo.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_2MIL, end_bonus_2mils);
            jo.putNumber(colName.DGD2MillionsPromotionMembersCol.ERROR_CODE, error_code);
            jo.putNumber(colName.DGD2MillionsPromotionMembersCol.REGISTER_END_TIME, register_end_time);
            jo.putNumber(colName.DGD2MillionsPromotionMembersCol.BONUS_END_TIME, bonus_end_time);
            return jo;
        }
    }
}
