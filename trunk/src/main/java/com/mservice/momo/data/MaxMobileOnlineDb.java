package com.mservice.momo.data;

import com.mservice.momo.vertx.models.MaxMobileOnline;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 5/24/14.
 */
public class MaxMobileOnlineDb extends MongoModelController<MaxMobileOnline> {
    public MaxMobileOnlineDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(MaxMobileOnline model) {
        return "statistic_MaxMobileOnline";
    }

    @Override
    public MaxMobileOnline newModelInstance() {
        return new MaxMobileOnline();
    }
}
