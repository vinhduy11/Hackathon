package com.mservice.momo.data;

import com.mservice.momo.vertx.models.TranStatusConfig;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 8/7/14.
 */
public class TranStatusConfigDb extends MongoModelController<TranStatusConfig> {

    public TranStatusConfigDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(TranStatusConfig model) {
        return "Config_TranStatus";
    }

    @Override
    public TranStatusConfig newModelInstance() {
        return new TranStatusConfig();
    }
}
