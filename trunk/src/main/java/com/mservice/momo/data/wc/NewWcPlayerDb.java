package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.vertx.models.wc.NewWcPlayer;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 6/27/14.
 */
public class NewWcPlayerDb extends MongoModelController<NewWcPlayer>{

    public NewWcPlayerDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    public NewWcPlayerDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(NewWcPlayer model) {
        return "wc_newWcPlayer";
    }

    @Override
    public NewWcPlayer newModelInstance() {
        return new NewWcPlayer();
    }
}
