package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.models.TimedGift;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 10/31/14.
 */
public class TimedGiftDb extends MongoModelController<TimedGift> {

    public TimedGiftDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(TimedGift model) {
        return "timedGift";
    }

    @Override
    public TimedGift newModelInstance() {
        return new TimedGift();
    }

    public void upsert(final TimedGift timedGift, final Handler<TimedGift> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);

        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));

        JsonObject newJsonObj = timedGift.getPersisFields();

        JsonObject match = new JsonObject();
        match.putString("giftId",timedGift.giftId);
        query.putObject(MongoKeyWords.MATCHER, match);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                callback.handle(timedGift);
            }
        });
    }

    public void find(long time, final Handler<List<TimedGift>> callback) {
        JsonObject matcher = new JsonObject();

        JsonObject lte = new JsonObject()
                .putNumber("$lte", time);
        matcher.putObject("time", lte);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<TimedGift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            TimedGift m = newModelInstance();
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
