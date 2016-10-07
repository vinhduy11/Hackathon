package com.mservice.momo.data;

import com.mservice.momo.data.wc.WcPlayer;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 6/11/14.
 */
public class WcPlayerDb extends MongoModelController<WcPlayer> {
    public WcPlayerDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(WcPlayer model) {
        return "wc_players";
    }

    @Override
    public WcPlayer newModelInstance() {
        return new WcPlayer();
    }
}
