package com.mservice.momo.data.lotte;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by tunguyen on 10/05/2016.
 */
public class LottePromoCodeDb {
    private Vertx vertx;
    private Logger logger;
    public static final int NEW = 0;
    public static final int ACTIVE = 1;
    public static final int USED = 2;
    public static final int EXPIRE = 3;

    public LottePromoCodeDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final LottePromoCodeDb.Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.LottePromoCodeCol.TABLE)
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

    public void findByCode(String promoCode, final Handler<LottePromoCodeDb.Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.LottePromoCodeCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.LottePromoCodeCol.PROMO_CODE, promoCode);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                LottePromoCodeDb.Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new LottePromoCodeDb.Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public void upSert(String promoCode
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.LottePromoCodeCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.LottePromoCodeCol.PROMO_CODE, promoCode);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("status", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public static class Obj {
        public String id = "";
        public String promotionCode = "";
        public String owner = "";
        public String eventId = "";
        public long amount = 0;
        public long startDate = 0;
        public long endDate = 0;
        public long modifyDate = 0;
        public int status = 0;
        public String creator = "";
        public int promoType = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {
            id = jo.getString(colName.BillPayPromoError.ID, "");
            promotionCode = jo.getString(colName.LottePromoCodeCol.PROMO_CODE, "").trim();
            owner = jo.getString(colName.LottePromoCodeCol.OWNER, "").trim();
            eventId = jo.getString(colName.LottePromoCodeCol.EVENT_ID, "").trim();
            amount = jo.getLong(colName.LottePromoCodeCol.AMOUNT, 0);
            startDate = jo.getLong(colName.LottePromoCodeCol.START_DATE, 0);
            endDate = jo.getLong(colName.LottePromoCodeCol.END_DATE, 0);
            modifyDate = jo.getLong(colName.LottePromoCodeCol.MODIFY_DATE, 0);
            status = jo.getInteger(colName.LottePromoCodeCol.STATUS, 0);
            creator = jo.getString(colName.LottePromoCodeCol.CREATOR, "").trim();
            promoType = jo.getInteger(colName.LottePromoCodeCol.PROMO_TYPE, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.LottePromoCodeCol.ID, id.trim());
            jo.putString(colName.LottePromoCodeCol.PROMO_CODE, promotionCode);
            jo.putString(colName.LottePromoCodeCol.OWNER, owner);
            jo.putString(colName.LottePromoCodeCol.EVENT_ID, eventId);
            jo.putNumber(colName.LottePromoCodeCol.AMOUNT, amount);
            jo.putNumber(colName.LottePromoCodeCol.START_DATE, startDate);
            jo.putNumber(colName.LottePromoCodeCol.END_DATE, endDate);
            jo.putNumber(colName.LottePromoCodeCol.MODIFY_DATE, modifyDate);
            jo.putNumber(colName.LottePromoCodeCol.STATUS, status);
            jo.putString(colName.LottePromoCodeCol.CREATOR, creator.trim());
            jo.putNumber(colName.LottePromoCodeCol.PROMO_TYPE, promoType);
            return jo;
        }
    }
}
