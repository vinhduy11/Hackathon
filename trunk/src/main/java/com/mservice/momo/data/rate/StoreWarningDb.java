package com.mservice.momo.data.rate;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.rate.StoreComment;
import com.mservice.momo.vertx.models.rate.StoreWarning;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 9/13/14.
 */
public class StoreWarningDb extends MongoModelController<StoreWarning> {
    public StoreWarningDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(StoreWarning model) {
        return "storeWarning";
    }

    @Override
    public StoreWarning newModelInstance() {
        return new StoreWarning();
    }

    public void find(StoreWarning filter, Long time, Integer pageSize, final Handler<List<StoreWarning>> callback) {
        JsonObject matcher = filter.getPersisFields();
        if(filter.getModelId()!=null)
            matcher.putString("_id", filter.getModelId());
        matcher.putObject("date",
                new JsonObject()
                        .putNumber(MongoKeyWords.LESS_THAN, time)
        );


        JsonObject sortBy = new JsonObject().putNumber("date", -1);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putNumber(MongoKeyWords.LIMIT, pageSize);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putObject(MongoKeyWords.SORT, sortBy);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<StoreWarning> mList = new ArrayList<StoreWarning>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            StoreWarning m = newModelInstance();
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
