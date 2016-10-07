package com.mservice.momo.cmd;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 3/14/14.
 */
public class CommandHandler {

    protected final MainDb mainDb;
    protected final Vertx vertx;
    protected final Container container;
    protected final Logger logger;
    protected final JsonObject config;


    public CommandHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject config) {
        this.mainDb = mainDb;
        this.vertx = vertx;
        this.container = container;
        this.logger = container.logger();
        this.config = config;
    }

}
