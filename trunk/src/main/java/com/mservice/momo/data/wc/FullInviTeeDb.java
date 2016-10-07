package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.vertx.models.wc.FullInvitee;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 6/25/14.
 */
public class FullInviTeeDb extends MongoModelController<FullInvitee>{

    public FullInviTeeDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(FullInvitee model) {
        return "wc_fullInvitee";
    }

    @Override
    public FullInvitee newModelInstance() {
        return new FullInvitee();
    }

}
