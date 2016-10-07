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
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 5/23/14.
 */
public abstract class MongoModelController<M extends MongoModel> {
    protected Vertx vertx;
    protected Logger logger;

    protected MongoModelController(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.logger = container.logger();
    }

    protected MongoModelController(Vertx vertx, Logger logger) {
        this.vertx = vertx;
        this.logger = logger;
    }

    public abstract String getCollectionName(M model);

    public abstract M newModelInstance();

    /**
     * If the saving process successfully, The input model will be set Id field by the id give by the mongo database.
     *
     * @param model
     * @param savedModel
     */
    public void save(final M model, final Handler<String> savedModel) {
        if (model == null)
            throw new IllegalArgumentException("Model can't be null");

        JsonObject json = model.getPersisFields();
        if (model.getModelId() != null)
            json.putString("_id", model.getModelId());

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

    public void update(M model, boolean upsert, final Handler<Boolean> callback) {
        if (model == null)
            throw new IllegalArgumentException("Model can't be null");

        JsonObject updateValues = model.getPersisFields();

        if (model.getModelId() == null || (model.getModelId() != null && model.getModelId().isEmpty())) {
            throw new IllegalArgumentException("model.modelId is null or empty!");
        }
        updateValues.removeField("_id");

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, getCollectionName(model));
        JsonObject criteria = new JsonObject()
                .putString("_id", model.getModelId());
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, updateValues);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, upsert);

        logger.trace(String.format("Updated model %s{id:%s}: %s"
                , model.getClass().getSimpleName()
                , model.getModelId()
                , model.getPersisFields()));

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (callback != null)
                    callback.handle(true);
            }
        });
    }
    public void updateOld(M model, boolean upsert, final Handler<Boolean> callback) {
        if (model == null)
            throw new IllegalArgumentException("Model can't be null");

        JsonObject updateValues = model.getPersisFields();

        if (model.getModelId() == null || (model.getModelId() != null && model.getModelId().isEmpty())) {
            throw new IllegalArgumentException("model.modelId is null or empty!");
        }
        updateValues.removeField("_id");
//        if (updateValues.containsField("_id")) {
//            throw new IllegalArgumentException("model.toJsonObject() returns an JsonObject contains _id field!");
//        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);

        query.putString(MongoKeyWords.COLLECTION, getCollectionName(model));
        JsonObject matcher = new JsonObject()
                .putString("_id", model.getModelId());
        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, updateValues);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, upsert);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.trace(String.format("Updated model %s{id:%s}: %s"
                , model.getClass().getSimpleName()
                , model.getModelId()
                , model.getPersisFields()));

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (callback != null)
                    callback.handle(true);
            }
        });
    }

    public void updateMulti(M filter, M newValue, final Handler<Long> callback) {
        if (filter == null)
            throw new IllegalArgumentException("Filter can't be null");
        if (newValue == null)
            throw new IllegalArgumentException("newValue can't be null");

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
        JsonObject matcher = filter.getPersisFields();
        if (filter.getModelId() != null) {
            matcher.putString("_id", filter.getModelId());
        }

        query.putObject(MongoKeyWords.CRITERIA, matcher);

        JsonObject nValue = newValue.getPersisFields();
        nValue.removeField("_id");

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, nValue);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.MULTI, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (callback != null)
                    callback.handle(0L);
            }
        });
    }

    public void remove(final M containIdModel, final Handler<Boolean> callback) {
        if (containIdModel == null)
            throw new IllegalArgumentException("Model can't be null");

        if (containIdModel.getModelId() == null || (containIdModel.getModelId() != null && containIdModel.getModelId().isEmpty())) {
            throw new IllegalArgumentException("model.modelId is null or empty!");
        }
        JsonObject criteria = new JsonObject()
                .putString("_id", containIdModel.getModelId());

        JsonObject query = new JsonObject()
                .putString("action", "delete")
                .putString("collection", getCollectionName(containIdModel))
                .putObject("matcher", criteria);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (callback == null)
                    return;
                if (event.body().getString("_id") != null)
                    callback.handle(true);
                callback.handle(false);
            }
        });
    }

    /**
     * Set filter = null to find all.
     *
     * @param filter
     * @param callback
     */
    public void findOne(final M filter, final Handler<M> callback) {
        JsonObject matcher = filter.getPersisFields();
        if (filter.getModelId()!=null && !filter.getModelId().isEmpty()) {
            matcher.putString("_id", filter.getModelId());
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
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
                    M m = newModelInstance();
                    m.setModelId(result.getString("_id"));
                    m.setValues(result);
                    callback.handle(m);
                }
            }
        });
    }

    /*public void findOne(final String giftId, final Handler<M> callback) {

        JsonObject matcher = new JsonObject();
        matcher.putString("giftId",giftId);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, "queuedGift");
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> event) {
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
    }*/

    public void find(final M filter, int limit, final Handler<List<M>> callback) {
        JsonObject matcher = null;
        if (filter != null) {
            matcher = filter.getPersisFields();
            if (filter.getModelId() != null && !filter.getModelId().isEmpty()) {
                matcher.putString("_id", filter.getModelId());
            }
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        if (limit > 0)
            query.putNumber(MongoKeyWords.LIMIT, limit);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
        if (matcher != null)
            query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<M> mList = new ArrayList<M>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            M m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(filter) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

    public void increase(M filter, M value, final Handler<Long> callback) {
        JsonObject matcher = filter.getPersisFields();
        if (filter.getModelId()!=null && !filter.getModelId().isEmpty()) {
            matcher.putString("_id", filter.getModelId());
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
        query.putObject(MongoKeyWords.CRITERIA, matcher);

        JsonObject nValue = new JsonObject()
                .putObject("$inc", value.getPersisFields()
                );

//        JsonObject update = new JsonObject();
//        update.putObject(MongoKeyWords.INCREMENT, nValue);
        query.putObject(MongoKeyWords.OBJ_NEW, nValue);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.MULTI, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (callback != null)
                    callback.handle(0L);
            }
        });

    }

    public void increase(M filter, M value, boolean upsert, final Handler<Long> callback) {
        JsonObject matcher = filter.getPersisFields();
        if (filter.getModelId()!=null && !filter.getModelId().isEmpty()) {
            matcher.putString("_id", filter.getModelId());
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
        query.putObject(MongoKeyWords.CRITERIA, matcher);

        JsonObject nValue = new JsonObject()
                .putObject("$inc", value.getPersisFields()
                );

//        JsonObject update = new JsonObject();
//        update.putObject(MongoKeyWords.INCREMENT, nValue);
        query.putObject(MongoKeyWords.OBJ_NEW, nValue);
        query.putBoolean(MongoKeyWords.UPSERT, upsert);
        query.putBoolean(MongoKeyWords.MULTI, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (callback != null)
                    callback.handle(0L);
            }
        });

    }

    public void count(M filter, final Handler<Long> callback) {
        JsonObject matcher = null;
        if (filter != null) {
            matcher = filter.getPersisFields();
            if (filter.getModelId() != null && !filter.getModelId().isEmpty()) {
                matcher.putString("_id", filter.getModelId());
            }
        }

        JsonObject query = new JsonObject()
                .putString("action", "findWithFilter")
                .putString("collection", getCollectionName(filter))
                .putObject("matcher", matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (callback == null)
                    return;
                callback.handle(event.body().getLong("count", 0));
            }
        });
    }

    public void getPage(int pageSize, int pageNumber,final M filter, final Handler<List<M>> callback) {
        JsonObject matcher = null;
        if (filter != null) {
            matcher = filter.getPersisFields();
            if (filter.getModelId() != null && !filter.getModelId().isEmpty()) {
                matcher.putString("_id", filter.getModelId());
            }
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        if (pageSize > 0)
            query.putNumber(MongoKeyWords.LIMIT, pageSize);
        query.putNumber(MongoKeyWords.SKIP, pageSize * pageNumber);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
        if (matcher != null)
            query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<M> mList = new ArrayList<M>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            M m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(filter) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

}
