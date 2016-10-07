package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.vertx.models.wc.Invitee;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 6/25/14.
 */
public class InviteeDb extends MongoModelController<Invitee> {

    public InviteeDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(Invitee model) {
        return "wc_invitee";
    }

    @Override
    public Invitee newModelInstance() {
        return new Invitee();
    }
}
