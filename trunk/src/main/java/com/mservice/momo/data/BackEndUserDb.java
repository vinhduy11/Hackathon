package com.mservice.momo.data;

//import com.mservice.momo.vertx.models.com.mservice.momo.web.internal.webadmin.BackEndUser;

import com.mservice.momo.vertx.models.webadmin.BackEndUser;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 9/19/14.
 */
public class BackEndUserDb extends MongoModelController<BackEndUser> {

    public BackEndUserDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(BackEndUser model) {
        return "backendUser";
    }

    @Override
    public BackEndUser newModelInstance() {
        return new BackEndUser();
    }
}
