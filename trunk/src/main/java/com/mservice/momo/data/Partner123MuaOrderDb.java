package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 4/16/14.
 */
public class Partner123MuaOrderDb {
    private static final String COLLECTION_NAME = "partner123mua";
    private EventBus eventBus;
    private Logger logger;

    public Partner123MuaOrderDb(EventBus eventBus, Logger logger) {
        this.eventBus = eventBus;
        this.logger = logger;
    }

    public static class Order {
        public static final int STATUS_PENDING = 0;
        public static final int STATUS_PAID = 1;

        public String id;
        public Long time;
        public Integer status;
        public Long amount;

        public Order(String id, Long time, Integer status, Long amount) {
            this.id = id;
            this.time = time;
            this.status = status;
            this.amount = amount;
        }

        public Order(JsonObject obj) {
            this.id = obj.getString("orderId", "");
            this.time = obj.getLong("time", 0L);
            this.status = obj.getInteger("status", -1);
            this.amount = obj.getLong("amount", 0);
        }

        public JsonObject toJsonObject() {
            JsonObject obj = new JsonObject();
            obj.putString("orderId", this.id);
            if (this.time != null)
                obj.putNumber("time", time);
            if (this.status != null)
                obj.putNumber("status", status);
            obj.putNumber("amount", amount);
            return obj;
        }
    }

    public void saveOrder(Order order, final Handler<String> callback) {
        logger.debug(String.format("[Partner123MuaOrderDb] saveOrder(%s)", order.toJsonObject()));
        JsonObject model = order.toJsonObject();

        JsonObject criteria = new JsonObject()
                .putString("orderId", order.id);

        JsonObject query = new JsonObject()
                .putString("action", "update")
                .putBoolean("upsert", true)
                .putString("collection", COLLECTION_NAME)
                .putObject("criteria", criteria)
                .putObject("objNew", model);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body().getString("_id"));
            }
        });
    }

    public void getOrder(String orderId, final Handler<Order> callback) {
        logger.debug(String.format("[Partner123MuaOrderDb] getOrder({orderId: %s})", orderId));
        JsonObject matcher = new JsonObject()
                .putString("orderId", orderId);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    callback.handle(null);
                    return;
                }
                JsonObject obj = event.body().getObject("result");

                Order order = new Order(obj);

                callback.handle(order);
            }
        });
    }
}
