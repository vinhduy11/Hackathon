package com.mservice.momo.data.referral;

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
 * Created by concu on 4/6/16.
 */
public class ReferralV1TransactionsTrackingDb {
    private Vertx vertx;
    private Logger logger;

    public ReferralV1TransactionsTrackingDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.ReferralV1TransactionsTrackingCol.TABLE)
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

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1TransactionsTrackingCol.TABLE);

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


    public static class Obj {

        public String inviteeNumber = "";
        public String inviterNumber = "";
        public long tran_amount = 0;
        public long tran_id = 0;
        public long bonus_time = 0;
        public long bonus_tid = 0;
        public long bonus_amount = 0;
        public String bank_code = "";
        public String cardInfo = "";

        public Obj() {
        }

        public Obj(JsonObject jo) {
            inviteeNumber = jo.getString(colName.ReferralV1TransactionsTrackingCol.INVITEE_NUMBER, "");
            inviterNumber = jo.getString(colName.ReferralV1TransactionsTrackingCol.INVITER_NUMBER, "");
            tran_amount = jo.getLong(colName.ReferralV1TransactionsTrackingCol.TRAN_AMOUNT, 0);
            tran_id = jo.getLong(colName.ReferralV1TransactionsTrackingCol.TRAN_ID, 0);
            bonus_time = jo.getLong(colName.ReferralV1TransactionsTrackingCol.BONUS_TIME, 0);
            bonus_tid = jo.getLong(colName.ReferralV1TransactionsTrackingCol.BONUS_TID, 0);
            bonus_amount = jo.getLong(colName.ReferralV1TransactionsTrackingCol.BONUS_AMOUNT, 0);
            bank_code = jo.getString(colName.ReferralV1TransactionsTrackingCol.BANK_CODE, "");
            cardInfo = jo.getString(colName.ReferralV1TransactionsTrackingCol.CARD_INFO, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ReferralV1TransactionsTrackingCol.INVITEE_NUMBER, inviteeNumber);
            jo.putString(colName.ReferralV1TransactionsTrackingCol.INVITER_NUMBER, inviterNumber);
            jo.putNumber(colName.ReferralV1TransactionsTrackingCol.TRAN_AMOUNT, tran_amount);
            jo.putNumber(colName.ReferralV1TransactionsTrackingCol.TRAN_ID, tran_id);
            jo.putNumber(colName.ReferralV1TransactionsTrackingCol.BONUS_TIME, bonus_time);
            jo.putNumber(colName.ReferralV1TransactionsTrackingCol.BONUS_TID, bonus_tid);
            jo.putNumber(colName.ReferralV1TransactionsTrackingCol.BONUS_AMOUNT, bonus_amount);
            jo.putString(colName.ReferralV1TransactionsTrackingCol.BANK_CODE, bank_code);
            jo.putString(colName.ReferralV1TransactionsTrackingCol.CARD_INFO, cardInfo);
            return jo;
        }
    }
}
