package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.models.GiftType;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 9/26/14.
 */
public class GiftTypeDb extends MongoModelController<GiftType>{

    public GiftTypeDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(GiftType model) {
        return "giftType";
    }

    @Override
    public GiftType newModelInstance() {
        return new GiftType();
    }

    public void findWithTime(long greaterThanTime, final Handler<List<GiftType>> callback) {
        JsonObject matcher = new JsonObject();

        JsonObject time = new JsonObject()
                .putNumber("$gt", greaterThanTime);

        matcher.putObject("modifyDate", time);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);


        JsonObject modifyTime = new JsonObject();
        modifyTime.putNumber("modifyDate", -1); //Lay tu cao toi thap
        //modifyTime.putNumber("modifyDate", 1); //Chinh sua 13/10, lay qua tu cao toi thap de get nguoc tu thap len cao
        query.putObject(MongoKeyWords.SORT, modifyTime);


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<GiftType> mList = new ArrayList<GiftType>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            GiftType m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

    public void findWithTimeWithoutDeletedGift(long greaterThanTime, final Handler<List<GiftType>> callback) {
        JsonObject matcher = new JsonObject();

        JsonObject time = new JsonObject()
                .putNumber("$gt", greaterThanTime);

        JsonObject compareStatus = new JsonObject().putNumber(MongoKeyWords.NOT_EQUAL, 3);
        JsonObject status = new JsonObject().putObject("status", compareStatus);

        JsonObject modifyDate = new JsonObject().putObject("modifyDate", time);

        JsonArray jsonArrayAnd = new JsonArray().add(status).add(modifyDate);

        //JsonObject jsonAnd = new JsonObject().putArray(MongoKeyWords.AND_$, jsonArrayAnd);

        matcher.putArray(MongoKeyWords.AND_$, jsonArrayAnd);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);


        JsonObject modifyTime = new JsonObject();
        modifyTime.putNumber("modifyDate", 1);
        query.putObject(MongoKeyWords.SORT, modifyTime);


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<GiftType> mList = new ArrayList<GiftType>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            GiftType m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }
}
