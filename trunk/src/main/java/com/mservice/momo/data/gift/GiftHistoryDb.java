package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftHistory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 10/7/14.
 */
public class GiftHistoryDb extends MongoModelController<GiftHistory> {

    public GiftHistoryDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(GiftHistory model) {
        return "giftHistory";
    }

    @Override
    public GiftHistory newModelInstance() {
        return new GiftHistory();
    }

    public void findOne(GiftHistory filter, final Handler<GiftHistory> callback) {
        JsonObject matcher = filter.getPersisFields();
        if (filter.getModelId()!=null && !filter.getModelId().isEmpty()) {
            matcher.putString("_id", filter.getModelId());
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putObject(MongoKeyWords.SORT, new JsonObject().putNumber("time", -1));

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }
                String value = event.body().getObject("result").getString("value", "");
                if (callback != null) {
                    GiftHistory m = newModelInstance();
                    m.setModelId(result.getString("_id"));
                    m.setValues(result);
                    callback.handle(m);
                }
            }
        });
    }
}
