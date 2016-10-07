package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.models.GiftToNumber;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 10/16/14.
 */
public class GiftToNumberDb extends MongoModelController<GiftToNumber> {

    public GiftToNumberDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(GiftToNumber model) {
        return "giftToNumber";
    }

    @Override
    public GiftToNumber newModelInstance() {
        return new GiftToNumber();
    }

    public void find(long time, int status, final Handler<List<GiftToNumber>> callback) {
        JsonObject matcher = new JsonObject()
                .putNumber("status", GiftToNumber.STATUS_NEW)
                .putObject("startDate", new JsonObject()    
                                .putNumber("$lt", time)
                );

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<GiftToNumber> mList = new ArrayList<GiftToNumber>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            GiftToNumber m = newModelInstance();
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
