package com.mservice.momo.vertx.gift;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 10/16/14.
 */
public class GiftToNumberManager {

    private String G2N_ACCOUNT = "0979754034";

    private GiftManager giftManager;

    public GiftToNumberManager(Vertx vertx, Logger logger, JsonObject globalConfig) {
        JsonObject giftConfig = globalConfig.getObject("gift", new JsonObject());
        G2N_ACCOUNT = giftConfig.getString("g2nAccount", "mservice");
        giftManager = new GiftManager(vertx, logger, globalConfig);
    }

}
