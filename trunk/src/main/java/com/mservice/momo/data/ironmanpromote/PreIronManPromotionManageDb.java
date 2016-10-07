package com.mservice.momo.data.ironmanpromote;

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
 * Created by concu on 9/19/15.
 */
public class PreIronManPromotionManageDb {

    private Vertx vertx;
    private Logger logger;

    public PreIronManPromotionManageDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.PreIronManPromotionManage.TABLE)
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

    public void updatePartial(int phone_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PreIronManPromotionManage.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PreIronManPromotionManage.PHONE_NUMBER, phone_number);
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

    public void upsertPartial(int phone_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PreIronManPromotionManage.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PreIronManPromotionManage.PHONE_NUMBER, phone_number);
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
        query.putString(MongoKeyWords.COLLECTION, colName.PreIronManPromotionManage.TABLE);

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

    public void findOne(String number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PreIronManPromotionManage.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.PreIronManPromotionManage.PHONE_NUMBER, number);
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

        public String phone_number = "";
        public String gift_id = "";
        public long start_time = 0;
        public long end_time = 0;
        public boolean is_used = false;
        public boolean is_new_register = false;
        public boolean is_login_user = false;

        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.PreIronManPromotionManage.PHONE_NUMBER, "").trim();
            gift_id = jo.getString(colName.PreIronManPromotionManage.GIFT_ID, "").trim();
            start_time = jo.getLong(colName.PreIronManPromotionManage.START_TIME, 0);
            end_time = jo.getLong(colName.PreIronManPromotionManage.END_TIME, 0);
            is_used = jo.getBoolean(colName.PreIronManPromotionManage.IS_USED, false);
            is_new_register = jo.getBoolean(colName.PreIronManPromotionManage.IS_NEW_REGISTER, false);
            is_login_user = jo.getBoolean(colName.PreIronManPromotionManage.IS_LOGIN_USER, false);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.PreIronManPromotionManage.PHONE_NUMBER, phone_number.trim());
            jo.putString(colName.PreIronManPromotionManage.GIFT_ID, gift_id.trim());
            jo.putNumber(colName.PreIronManPromotionManage.START_TIME, start_time);
            jo.putNumber(colName.PreIronManPromotionManage.END_TIME, end_time);
            jo.putBoolean(colName.PreIronManPromotionManage.IS_USED, is_used);
            jo.putBoolean(colName.PreIronManPromotionManage.IS_NEW_REGISTER, is_new_register);
            jo.putBoolean(colName.PreIronManPromotionManage.IS_LOGIN_USER, is_login_user);
            return jo;
        }
    }


}
