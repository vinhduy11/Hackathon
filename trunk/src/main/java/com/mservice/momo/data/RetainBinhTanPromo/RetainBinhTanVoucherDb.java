package com.mservice.momo.data.RetainBinhTanPromo;

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
 * Created by congnguyenit on 9/26/16.
 */
public class RetainBinhTanVoucherDb {

    private Vertx vertx;
    private Logger logger;

    public RetainBinhTanVoucherDb(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void findByPhone(String phoneNumber, final Handler<Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.RetainBinhTanVoucherDbCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.RetainBinhTanVoucherDbCols.NUMBER, phoneNumber);
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

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.RetainBinhTanVoucherDbCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> joObject) {

                int result = 0;
                if (!joObject.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(joObject.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.RetainBinhTanVoucherDbCols.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<>();

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

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.RetainBinhTanVoucherDbCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.RetainBinhTanVoucherDbCols.NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString(MongoKeyWords.STATUS, "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }
    public static class Obj {
        public String   NUMBER;
        public long     CASHIN_TIME;
        public long     CASHIN_TID;
        public long     VOUCHER_TIME;
        public long     VOUCHER_AMOUNT;
        public String   GIFT_ID;
        public boolean  IS_USED;
        public int      TIME_OF_VOUCHER;
        public boolean  BONUS_EXTRA;

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.RetainBinhTanVoucherDbCols.NUMBER, NUMBER);
            jo.putNumber(colName.RetainBinhTanVoucherDbCols.CASHIN_AMOUNT, CASHIN_TID);
            jo.putNumber(colName.RetainBinhTanVoucherDbCols.TIME_CASHIN, CASHIN_TIME);
            jo.putNumber(colName.RetainBinhTanVoucherDbCols.VOUCHER_TIME, VOUCHER_TIME);
            jo.putNumber(colName.RetainBinhTanVoucherDbCols.VOUCHER_AMOUNT, VOUCHER_AMOUNT);
            jo.putString(colName.RetainBinhTanVoucherDbCols.GIFT_ID, GIFT_ID);
            jo.putBoolean(colName.RetainBinhTanVoucherDbCols.IS_USED, IS_USED);
            jo.putNumber(colName.RetainBinhTanVoucherDbCols.TIME_OF_VOUCHER, TIME_OF_VOUCHER);
            jo.putBoolean(colName.RetainBinhTanVoucherDbCols.BONUS_EXTRA, BONUS_EXTRA);
            return jo;

        }

        public Obj(JsonObject jo){
            NUMBER          = jo.getString(colName.RetainBinhTanVoucherDbCols.NUMBER, "");
            CASHIN_TID   = jo.getLong(colName.RetainBinhTanVoucherDbCols.CASHIN_AMOUNT, 0);
            CASHIN_TIME = jo.getLong(colName.RetainBinhTanVoucherDbCols.TIME_CASHIN, 0);
            VOUCHER_TIME = jo.getInteger(colName.RetainBinhTanVoucherDbCols.VOUCHER_TIME, 0);
            TIME_OF_VOUCHER = jo.getInteger(colName.RetainBinhTanVoucherDbCols.TIME_OF_VOUCHER, 0);
            VOUCHER_AMOUNT  = jo.getInteger(colName.RetainBinhTanVoucherDbCols.VOUCHER_AMOUNT, 0);
            GIFT_ID         = jo.getString(colName.RetainBinhTanVoucherDbCols.GIFT_ID, "");
            IS_USED         = jo.getBoolean(colName.RetainBinhTanVoucherDbCols.IS_USED, false);
            BONUS_EXTRA     = jo.getBoolean(colName.RetainBinhTanVoucherDbCols.BONUS_EXTRA, false);
        }

        public Obj(){

        }
    }
}
