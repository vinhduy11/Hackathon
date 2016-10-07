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
 * Created by congnguyenit on 8/30/16.
 */
public class GrabPromoDb {
    private Vertx vertx;
    private Logger logger;

    public GrabPromoDb(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void findByPhone(String phoneNumber, final Handler<Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.GrabPromoDbCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.GrabPromoDbCols.NUMBER, phoneNumber);
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
                .putString(MongoKeyWords.COLLECTION, colName.GrabPromoDbCols.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.GrabPromoDbCols.TABLE);

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
        query.putString(MongoKeyWords.COLLECTION, colName.GrabPromoDbCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.GrabPromoDbCols.NUMBER, phoneNumber);
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

        public String  NUMBER;
        public String  CODE;
        public long    TIME_INPUT;
        public String  TRAN_ID;
        public String  CARD_ID;
        public int     TIME_OF_BONUS;
        public boolean IS_LOCK;
        public boolean END_PROMO;
        public long    TIME_REGISTER;
        public long    TIME_UPDATE;

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.GrabPromoDbCols.NUMBER, NUMBER);
            jo.putString(colName.GrabPromoDbCols.CODE, CODE);
            jo.putNumber(colName.GrabPromoDbCols.TIME_INPUT, TIME_INPUT);
            jo.putString(colName.GrabPromoDbCols.TID, TRAN_ID);
            jo.putNumber(colName.GrabPromoDbCols.TIME_OF_BONUS, TIME_OF_BONUS);
            jo.putBoolean(colName.GrabPromoDbCols.IS_LOCK, IS_LOCK);
            jo.putBoolean(colName.GrabPromoDbCols.END_PROMO, END_PROMO);
            jo.putNumber(colName.GrabPromoDbCols.TIME_REGISTER, TIME_REGISTER);
            jo.putNumber(colName.GrabPromoDbCols.TIME_UPDATE, TIME_UPDATE);
            jo.putString(colName.GrabPromoDbCols.CARDID, CARD_ID);
            return jo;

        }

        public Obj(JsonObject jo){
            NUMBER        = jo.getString(colName.GrabPromoDbCols.NUMBER, "");
            CODE          = jo.getString(colName.GrabPromoDbCols.CODE, "");
            TIME_INPUT    = jo.getLong(colName.GrabPromoDbCols.TIME_INPUT, 0);
            TRAN_ID       = jo.getString(colName.GrabPromoDbCols.TID, "");
            TIME_OF_BONUS = jo.getInteger(colName.GrabPromoDbCols.TIME_OF_BONUS, 0);
            IS_LOCK       = jo.getBoolean(colName.GrabPromoDbCols.IS_LOCK, false);
            END_PROMO     = jo.getBoolean(colName.GrabPromoDbCols.END_PROMO, false);
            TIME_REGISTER = jo.getLong(colName.GrabPromoDbCols.TIME_REGISTER, 0);
            TIME_UPDATE = jo.getLong(colName.GrabPromoDbCols.TIME_UPDATE, 0);
            CARD_ID       = jo.getString(colName.GrabPromoDbCols.CARDID, "");
        }

        public Obj(){

        }
    }
}
