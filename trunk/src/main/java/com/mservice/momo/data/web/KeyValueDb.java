package com.mservice.momo.data.web;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by ntunam on 3/27/14.
 */
public class KeyValueDb {
    public static final String COLLECTION_NAME = "web_key_value";

    private EventBus eventBus;

    public KeyValueDb(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void get(String key, final Handler<String> handler) {

        JsonObject matcher = new JsonObject()
                .putString("key", key);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    handler.handle(null);
                    return;
                }
                String value = event.body().getObject("result").getString("value", "");
                handler.handle(value);
            }
        });
    }

    public void put(String key, String value, final Handler<String> handler) {
        JsonObject model = new JsonObject()
                .putString("key", key)
                .putString("value", value);

        JsonObject criteria = new JsonObject()
                .putString("key", key);

        JsonObject query = new JsonObject()
                .putString("action", "update")
                .putBoolean("upsert", true)
                .putString("collection", COLLECTION_NAME)
                .putObject("criteria", criteria)
                .putObject("objNew", model);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getString("_id"));
            }
        });
    }

}
