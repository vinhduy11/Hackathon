package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.models.QueuedGift;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 10/24/14.
 */
public class QueuedGiftDb extends MongoModelController<QueuedGift> {
    private Logger logger;
    private EventBus eventBus;

    public QueuedGiftDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
        this.eventBus = vertx.eventBus();
        this.logger = logger;
    }

    @Override
    public String getCollectionName(QueuedGift model) {
        return "queuedGift";
    }

    @Override
    public QueuedGift newModelInstance() {
        return new QueuedGift();
    }

    public void deleteQueuedGift(String id, final  Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));

        match.putString("_id", id);

        query.putObject(MongoKeyWords.MATCHER,match);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }
    ///------------------------------------------------------

    public void findBy(String owner, final Handler<List<QueuedGift>> callback) {
        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));

        if (!"".equalsIgnoreCase(owner)){
            matcher.putString("owner",owner);
            query.putObject(MongoKeyWords.MATCHER, matcher);
        }

        JsonObject fields = new JsonObject();
        //lay danh sach cac field trong bang QueuedGift
        fields.putNumber("_id", 1);
        fields.putNumber("owner", 1);
        fields.putNumber("gifTypeId", 1);
        fields.putNumber("service", 1);
        fields.putNumber("giftId", 1);

        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<QueuedGift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            QueuedGift m = newModelInstance();
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

    //xoa gift ra khoi queue dua vao giftId
    public void removeGift(final String giftId, final Handler<Integer> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION,"queuedGift");
        match.putString("giftId", giftId);

        query.putObject(MongoKeyWords.MATCHER,match);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count);
            }
        });
    }


    public void findOne(final String giftId, final Handler<QueuedGift> callback) {

        JsonObject matcher = new JsonObject();
        matcher.putString("giftId",giftId);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }

                if (callback != null) {
                    QueuedGift queuedGift = new QueuedGift();
                    queuedGift.setModelId(result.getString("_id"));
                    queuedGift.setValues(result);
                    callback.handle(queuedGift);
                }
            }
        });
    }

}
