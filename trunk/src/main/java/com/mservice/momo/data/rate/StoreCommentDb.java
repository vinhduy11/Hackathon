package com.mservice.momo.data.rate;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.rate.StoreComment;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 9/12/14.
 *
 * TODO:
 *  *Highly recommended index:
 *      storeId;
 *      commenter;
 *      modifyDate;
 *  *Recommended index
 *      status;
 */
public class StoreCommentDb extends MongoModelController<StoreComment>{
    public StoreCommentDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(StoreComment model) {
        return "storeComment";
    }

    @Override
    public StoreComment newModelInstance() {
        return new StoreComment();
    }

    public void getCommentPage(int pageSize, Integer storeId, Integer commenter, long lastTime, List status, final Handler<List<StoreComment>> callback) {
        JsonObject matcher = new JsonObject();
        if (storeId != null)
            matcher.putNumber("storeId", storeId);
        if (commenter != null && commenter != 0)
            matcher.putNumber("commenter", commenter);
        matcher.putObject("modifyDate",
                new JsonObject()
                        .putNumber(MongoKeyWords.LESS_THAN, lastTime)
        );


        if (status != null) {
            JsonObject in = new JsonObject()
                    .putArray("$in", new JsonArray(status));
            matcher.putObject("status", in);
        }

        JsonObject sortBy = new JsonObject().putNumber("modifyDate", -1);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putNumber(MongoKeyWords.LIMIT, pageSize);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putObject(MongoKeyWords.SORT, sortBy);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<StoreComment> mList = new ArrayList<StoreComment>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            StoreComment m = newModelInstance();
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
