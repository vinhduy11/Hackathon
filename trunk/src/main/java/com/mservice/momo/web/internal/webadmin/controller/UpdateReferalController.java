package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by locnguyen on 26/07/2014.
 */
public class UpdateReferalController {
    private Vertx vertx;
    private Logger logger;
    private JsonObject glbCfg;
    public UpdateReferalController(Vertx vertx, Container container, JsonObject glbCfg) {
        this.vertx = vertx;
        logger = container.logger();
        this.glbCfg = glbCfg;
    }

    @Action(path = "/update_referal")

    public void upsert(HttpRequestContext context, final Handler<JsonObject> callback) {

        PhonesDb phonesDb = new PhonesDb(vertx.eventBus(),logger);
        phonesDb.getPhoneHasReferal(0, new Handler<ArrayList<PhonesDb.Obj>>() {
            @Override
            public void handle(final ArrayList<PhonesDb.Obj> phoneList) {

                callback.handle(new JsonObject().putNumber("totalAgent",phoneList.size()));

                final JsonObject jo = new JsonObject();
                jo.putNumber("ind",0);

                vertx.setPeriodic(5000, new Handler<Long>() {
                    @Override
                    public void handle(final Long aLong) {
                        final int ind = jo.getInteger("ind",0);

                        PhonesDb.Obj pObj = phoneList.get(ind);

                        Misc.modifyAgent(vertx,pObj,logger, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                jo.putNumber("ind", ind + 1);
                                logger.info("current index " + ind);
                                if(ind == phoneList.size() - 1){
                                    vertx.cancelTimer(aLong);
                                }
                            }
                        });
                    }
                });
            }
        });
    }
}
