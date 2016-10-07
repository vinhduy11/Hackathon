package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.vertx.gift.models.GiftTran;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 11/4/14.
 */
public class GiftTranDb extends MongoModelController<GiftTran>{

    public GiftTranDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(GiftTran model) {
        return "giftTran";
    }

    @Override
    public GiftTran newModelInstance() {
        return new GiftTran();
    }
}
