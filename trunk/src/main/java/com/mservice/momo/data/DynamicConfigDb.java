package com.mservice.momo.data;

import com.mservice.momo.vertx.models.DynamicConfig;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 6/2/14.
 */
public class DynamicConfigDb extends MongoModelController<DynamicConfig> {

    public DynamicConfigDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(DynamicConfig model) {
        return "dynamicConfig";
    }

    @Override
    public DynamicConfig newModelInstance() {
        return new DynamicConfig();
    }
}
