package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.wc.DuDoan;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by nam on 6/8/14.
 */
public class DuDoanDb extends MongoModelController<DuDoan>{

    public DuDoanDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(DuDoan model) {
        return "wc_duDoan_" + model.getMatchId();
    }

    @Override
    public DuDoan newModelInstance() {
        return new DuDoan();
    }

    public void getOneWinner(final String matchId, final Handler<DuDoan> callback){
        DuDoan duDoan = new DuDoan();
        duDoan.setMatchId(matchId);

        JsonObject matcher = new JsonObject();
        matcher.putNumber("tranError", -1);
        matcher.putObject("money", new JsonObject().putNumber("$gte", 1));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(duDoan));
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
                String value = event.body().getObject("result").getString("value", "");
                if (callback != null) {
                    DuDoan m = newModelInstance();
                    m.setModelId(result.getString("_id"));
                    m.setValues(result);
                    m.setMatchId(matchId);
                    callback.handle(m);
                }
            }
        });
    }

    public void getWinnerBatch(final String matchId, final int batch_size, final Handler<ConcurrentLinkedQueue<DuDoan>> callback){
        DuDoan duDoan = new DuDoan();
        duDoan.setMatchId(matchId);

        JsonObject matcher = new JsonObject();
        matcher.putNumber("tranError", -1);
        matcher.putObject("money", new JsonObject().putNumber("$gte", 1));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(duDoan));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putNumber(MongoKeyWords.BATCH_SIZE, batch_size);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ConcurrentLinkedQueue<DuDoan> concurrentQueue = new ConcurrentLinkedQueue<DuDoan>();

                JsonArray jsonArray = event.body().getArray(MongoKeyWords.RESULT_ARRAY);

                for(int i = 0 ; i<jsonArray.size() ; i++){
                    JsonObject o = jsonArray.get(i);
                    DuDoan m = newModelInstance();
                    m.setModelId(o.getString("_id"));
                    m.setValues(o);
                    m.setMatchId(matchId);
                    concurrentQueue.add(m);
                }

                callback.handle(concurrentQueue);
            }
        });
    }

    public void upsertWithZaloTimeChecking(final DuDoan duDoan, final Handler<Boolean> callback) {

        JsonObject matcher = new JsonObject();
        matcher.putString("_id", duDoan.getModelId());
        matcher.putObject("zaloId",
                new JsonObject()
                        .putNumber("$lt", duDoan.getZaloId())
        );

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "upsertDuDoan");
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(duDoan));
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putObject(MongoKeyWords.CRITERIA, matcher);
        query.putObject(MongoKeyWords.OBJ_NEW, duDoan.getPersisFields().putString("_id", duDoan.getModelId()));

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Integer error = event.body().getInteger("error");
                if (error != 0) {
                    //response String error.
                    callback.handle(false);
                } else {
                    callback.handle(true);
                }
            }
        });
    }


    public void nextDuDoanHasMessage(final String matchId, final Handler<DuDoan> callback) {
        DuDoan duDoan = new DuDoan();
        duDoan.setMatchId(matchId);

        JsonArray array = new JsonArray();
        array.add(
                new JsonObject()
                        .putObject("sentZaloMessage",
                                new JsonObject()
                                        .putBoolean("$exists", false)
                        )
        );
        array.add(
                new JsonObject()
                .putBoolean("sentZaloMessage", false)
        );

        JsonObject matcher = new JsonObject();
        matcher.putArray("$or", array);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(duDoan));
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
                String value = event.body().getObject("result").getString("value", "");
                if (callback != null) {
                    DuDoan m = newModelInstance();
                    m.setModelId(result.getString("_id"));
                    m.setValues(result);
                    m.setMatchId(matchId);
                    callback.handle(m);
                }
            }
        });
    }


    public void buildResendList(DuDoan filter, DuDoan newValue, final Handler<Long> callback) {
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


}
