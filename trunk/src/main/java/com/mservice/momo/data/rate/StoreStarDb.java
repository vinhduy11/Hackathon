package com.mservice.momo.data.rate;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.rate.StoreStar;
import com.mservice.momo.vertx.processor.Common;
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
 */
public class StoreStarDb extends MongoModelController<StoreStar> {
    public StoreStarDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(StoreStar model) {
        return "storeStar";
    }

    @Override
    public StoreStar newModelInstance() {
        return new StoreStar();
    }

//    public void movePoint(Integer storeId, Integer oldStar, Integer newStar, Handler<JsonObject> callback) {
//        final CoreCommon.BuildLog log = new CoreCommon.BuildLog(logger);
//        log.add("action", "MovePoint");
//        log.add("storeId", storeId);
//        log.add("oldStar", oldStar);
//        log.add("newStar", newStar);
//
//        log.add("result", "???");
//        log.writeLog();
//    }

    public void find(List storeIds, final Handler<List<StoreStar>> callback) {
        JsonObject matcher = new JsonObject();
        JsonObject in = new JsonObject()
                .putArray("$in", new JsonArray(storeIds));

        matcher.putObject("storeId", in);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<StoreStar> mList = new ArrayList<StoreStar>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            StoreStar m = newModelInstance();
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
