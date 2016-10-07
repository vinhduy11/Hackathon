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
public class Partner123MuaRequestFailDb {
    private static final String COLLECTION_NAME = "partner123mua_fail";
    private EventBus eventBus;
    private Logger logger;

    public Partner123MuaRequestFailDb(EventBus eventBus, Logger logger) {
        this.eventBus = eventBus;
        this.logger = logger;
    }

    public static class PaidOrder {

        public String id;
        public String tranId;
        public String buyerPhone;
        public String shopPhone;
        public String payPhone;
        public Long amount;

        public String comment;

        public Long time;

        public PaidOrder(String id, String tranId, String buyerPhone, String shopPhone, String payPhone, Long amount, String comment, Long time) {
            this.id = id;
            this.tranId = tranId;
            this.buyerPhone = buyerPhone;
            this.shopPhone = shopPhone;
            this.payPhone = payPhone;
            this.amount = amount;
            this.comment = comment;
            this.time = time;
        }

        public PaidOrder(JsonObject obj) {
            this.id = obj.getString("orderId", "");

            this.tranId = obj.getString("tranId", "");
            this.buyerPhone = obj.getString("buyerPhone", "");
            this.shopPhone = obj.getString("shopPhone", "");
            this.payPhone = obj.getString("payPhone", "");
            this.amount = obj.getLong("amount", 0);

            this.comment = obj.getString("comment", "");

            this.time = obj.getLong("time", 0L);
        }

        public JsonObject toJsonObject() {
            JsonObject obj = new JsonObject();
            obj.putString("orderId", this.id);
            obj.putString("tranId", this.tranId);
            obj.putString("buyerPhone", this.buyerPhone);
            obj.putString("shopPhone", this.shopPhone);
            obj.putString("payPhone", this.payPhone);
            obj.putString("tranId", this.tranId);
            obj.putNumber("amount", this.amount);
            obj.putString("comment", this.comment);
            obj.putNumber("time", this.time);

            return obj;
        }
    }

    public void saveOrder(PaidOrder order, final Handler<String> callback) {
        logger.debug(String.format("[Partner123MuaRequestFailDb] saveOrder(%s)", order.toJsonObject()));
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

    public void getOrder(String orderId, final Handler<PaidOrder> callback) {
        logger.debug(String.format("[Partner123MuaRequestFailDb] getOrder({orderId: %s})", orderId));
        JsonObject matcher = new JsonObject()
                .putString("orderId", orderId);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        logger.debug(String.format("[Partner123MuaRequestFailDb].[getOrder] > [MongoVerticle]"));
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                logger.debug(String.format("[Partner123MuaRequestFailDb].[getOrder] > [MongoVerticle]"  + event));
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    callback.handle(null);
                    return;
                }
                JsonObject obj = event.body().getObject("result");

                PaidOrder order = new PaidOrder(obj);

                callback.handle(order);
            }
        });
    }
}
