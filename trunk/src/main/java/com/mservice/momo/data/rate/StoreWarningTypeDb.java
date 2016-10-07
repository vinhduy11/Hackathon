package com.mservice.momo.data.rate;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.vertx.models.rate.StoreWarningType;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 9/13/14.
 */
public class StoreWarningTypeDb extends MongoModelController<StoreWarningType> {
    public StoreWarningTypeDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(StoreWarningType model) {
        return "storeWarningType";
    }

    @Override
    public StoreWarningType newModelInstance() {
        return new StoreWarningType();
    }
}
