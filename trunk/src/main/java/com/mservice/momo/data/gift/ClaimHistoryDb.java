package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.vertx.gift.ClaimHistory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 10/21/14.
 */
public class ClaimHistoryDb extends MongoModelController<ClaimHistory>{
    public ClaimHistoryDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(ClaimHistory model) {
        return "claimHistory";
    }

    @Override
    public ClaimHistory newModelInstance() {
        return new ClaimHistory();
    }
}
