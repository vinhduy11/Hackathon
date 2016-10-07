package com.mservice.momo.data;

import com.mservice.momo.vertx.models.NotiStatistic;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 9/18/14.
 */
public class NotiStatisticDb extends MongoModelController<NotiStatistic> {

    public NotiStatisticDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(NotiStatistic model) {
        return "notiStatistic";
    }

    @Override
    public NotiStatistic newModelInstance() {
        return new NotiStatistic();
    }
}
