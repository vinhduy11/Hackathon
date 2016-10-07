package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 8/5/14.
 */
public abstract class MQueue<M extends MongoModel> extends MongoModelController<M> {

    public static final String ACCESS_TIME_FIELD = "__at";

    protected MQueue(Vertx vertx, Logger logger) {
        super(vertx, logger);
        this.vertx = vertx;
    }

    /**
     * Override this to get your right time in multi clients case.
     */
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getRevivalTime() {
        return 60000; // 1 phút
    }

    public void peek(final Handler<M> callback) {

        long currentTime = getCurrentTimeMillis();
        JsonArray constrains = new JsonArray();
        constrains.add(
                new JsonObject()
                        .putObject(ACCESS_TIME_FIELD, new JsonObject().putBoolean("$exists", false))
        );
        constrains.add(
                new JsonObject().putObject(ACCESS_TIME_FIELD, new JsonObject().putNumber("$lte", currentTime - getRevivalTime()))
        );

        JsonObject matcher = new JsonObject().putArray("$or", constrains);

        JsonObject sort = new JsonObject();
        sort.putNumber(ACCESS_TIME_FIELD, 1);

        JsonObject update = new JsonObject();
        update.putObject("$set", new JsonObject().putNumber(ACCESS_TIME_FIELD, currentTime));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putObject(MongoKeyWords.SORT, sort);
        query.putBoolean(MongoKeyWords.NEW, true);
        query.putBoolean(MongoKeyWords.UPSERT, false);

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
                    M m = newModelInstance();
                    m.setModelId(result.getString("_id"));
                    m.setValues(result);
                    callback.handle(m);
                }
            }
        });
    }

    /**
     * If the saving process successfully, The input model will be set Id field by the id give by the mongo database.
     *
     * @param model
     * @param savedModel
     */
    public void push(final M model, final Handler<String> savedModel) {
        if (model == null)
            throw new IllegalArgumentException("Model can't be null");

        JsonObject json = model.getPersisFields();
        if (model.getModelId() != null)
            json.putString("_id", model.getModelId());
        json.putNumber(ACCESS_TIME_FIELD, getCurrentTimeMillis());

        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection",getCollectionName(model))
                .putObject("document", json);

        logger.debug("MongoModelController request Event Address : " + AppConstant.MongoVerticle_ADDRESS);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if (createdId != null) {
                        if (savedModel != null)
                            savedModel.handle(createdId);
                        logger.trace(String.format("Saved model %s{id:%s}: %s", model.getClass().getSimpleName(), createdId, model.getPersisFields()));
                        return;
                    }
                }
                logger.trace(String.format("Can't save model %s: %s", model.getClass().getSimpleName(), model.getPersisFields()));
                if (savedModel != null)
                    savedModel.handle(null);
            }
        });
    }

    @Override
    @Deprecated
    public void save(M model, Handler<String> savedModel) {
        super.save(model, savedModel);
    }
}
