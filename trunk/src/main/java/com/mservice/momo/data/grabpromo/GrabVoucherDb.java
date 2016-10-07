package com.mservice.momo.data.grabpromo;

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
 * Created by congnguyenit on 9/5/16.
 */
public class GrabVoucherDb {

    private Vertx vertx;
    private Logger logger;

    public GrabVoucherDb(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void findByPhone(String phoneNumber, final Handler<Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.GrabVoucherDbCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.GrabVoucherDbCols.NUMBER, phoneNumber);
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
                .putString(MongoKeyWords.COLLECTION, colName.GrabVoucherDbCols.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.GrabVoucherDbCols.TABLE);

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

    public void findAndModifyUsedVoucher(String phoneNumber, String giftId, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.GrabVoucherDbCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.GrabVoucherDbCols.GIFT_ID, giftId);
        match.putString(colName.GrabVoucherDbCols.NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject objNew = new JsonObject();
        objNew.putBoolean(colName.GrabVoucherDbCols.IS_USED, true);
        objNew.putNumber(colName.GrabVoucherDbCols.VOUCHER_TIME, System.currentTimeMillis());

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
                logger.info("findAndModifyUsedVoucher " + "Grab_Promo" + " " +  json);
                if(json!= null){
                    JsonObject joRes = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyUsedVoucher json is not null " + "Grab_Promo" + " " +  joRes);
                    if(joRes != null)
                    {
                        Obj referralObj = new Obj(joRes);
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

        public String   NUMBER;
        public String   CODE;
        public long     CASHIN_TIME;
        public long     CASHIN_AMOUNT;
        public long     VOUCHER_TIME;
        public long     VOUCHER_AMOUNT;
        public String   GIFT_ID;
        public boolean  IS_USED;
        public int      TIME_OF_VOUCHER;
        public boolean  BONUS_EXTRA;

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.GrabVoucherDbCols.CODE, CODE);
            jo.putString(colName.GrabVoucherDbCols.NUMBER, NUMBER);
            jo.putNumber(colName.GrabVoucherDbCols.CASHIN_AMOUNT, CASHIN_AMOUNT);
            jo.putNumber(colName.GrabVoucherDbCols.TIME_CASHIN, CASHIN_TIME);
            jo.putNumber(colName.GrabVoucherDbCols.VOUCHER_TIME, VOUCHER_TIME);
            jo.putNumber(colName.GrabVoucherDbCols.VOUCHER_AMOUNT, VOUCHER_AMOUNT);
            jo.putString(colName.GrabVoucherDbCols.GIFT_ID, GIFT_ID);
            jo.putBoolean(colName.GrabVoucherDbCols.IS_USED, IS_USED);
            jo.putNumber(colName.GrabVoucherDbCols.TIME_OF_VOUCHER, TIME_OF_VOUCHER);
            jo.putBoolean(colName.GrabVoucherDbCols.BONUS_EXTRA, BONUS_EXTRA);
            return jo;

        }

        public Obj(JsonObject jo){
            NUMBER          = jo.getString(colName.GrabVoucherDbCols.NUMBER, "");
            CODE            = jo.getString(colName.GrabVoucherDbCols.CODE, "");
            CASHIN_AMOUNT   = jo.getLong(colName.GrabVoucherDbCols.CASHIN_AMOUNT, 0);
            CASHIN_TIME = jo.getLong(colName.GrabVoucherDbCols.TIME_CASHIN, 0);
            VOUCHER_TIME = jo.getInteger(colName.GrabVoucherDbCols.VOUCHER_TIME, 0);
            TIME_OF_VOUCHER = jo.getInteger(colName.GrabVoucherDbCols.TIME_OF_VOUCHER, 0);
            VOUCHER_AMOUNT  = jo.getInteger(colName.GrabVoucherDbCols.VOUCHER_AMOUNT, 0);
            GIFT_ID         = jo.getString(colName.GrabVoucherDbCols.GIFT_ID, "");
            IS_USED         = jo.getBoolean(colName.GrabVoucherDbCols.IS_USED, false);
            BONUS_EXTRA     = jo.getBoolean(colName.GrabVoucherDbCols.BONUS_EXTRA, false);
        }

        public Obj(){

        }
    }
}
