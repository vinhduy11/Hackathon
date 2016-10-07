package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.vertx.gift.models.GiftMessage;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 10/15/14.
 */
public class GiftMessageDb extends MongoModelController<GiftMessage> {

    public GiftMessageDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(GiftMessage model) {
        return "giftMessage";
    }

    @Override
    public GiftMessage newModelInstance() {
        return new GiftMessage();
    }
}
