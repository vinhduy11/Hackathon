package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.SettingsDb;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.ConfigVerticle;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Map;

/**
 * Created by nam on 8/7/14.
 */
public class TranConfigController {

    private Vertx vertx;
    private Container container;

    public TranConfigController(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
    }

    @Action(path = "/service/tranConfig/getAll")
    public void getAll(HttpRequestContext context, final Handler<Object> callback) {

        JsonObject request = new JsonObject()
                .putString("cmd", ConfigVerticle.CMD_GET_ALL_TRANSFER_CONFIG);
        vertx.eventBus().send(AppConstant.ConfigVerticle, request, new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                callback.handle(message.body());
            }
        });
    }

    @Action(path = "/service/tranConfig/set")
    public void set(HttpRequestContext context, final Handler<Object> callback) {

        MultiMap params = context.getRequest().params();

        JsonObject json = new JsonObject();
        for(Map.Entry<String, String> entry: params.entries()) {
            json.putString(entry.getKey(), entry.getValue());
        }

        JsonObject request = new JsonObject()
                .putString("cmd", ConfigVerticle.CMD_SET_ALL_TRANSFER_CONFIG)
                .putObject("values", json);
        vertx.eventBus().send(AppConstant.ConfigVerticle, request, new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                callback.handle(message.body());
            }
        });
    }

    @Action(path = "/cdhh/on")
    public void cdhhOn(HttpRequestContext context, final Handler<Object> callback) {

        //eventBus.registerHandler(AppConstant.ConfigVerticleService_Update, updateServiceHandler);
        SettingsDb settingsDb = new SettingsDb(vertx.eventBus(),container.logger());
        settingsDb.setLong("CDHH",1, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if(aBoolean){
                    Common.ServiceReq serviceReq = new Common.ServiceReq();
                    serviceReq.Command =Common.ServiceReq.COMMAND.UPDATE_CDHH;
                    vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
                }
            }
        });
        callback.handle("Service is on!");
    }

    @Action(path = "/cdhh/off")
    public void cdhhOff(HttpRequestContext context, final Handler<Object> callback) {

        SettingsDb settingsDb = new SettingsDb(vertx.eventBus(),container.logger());
        settingsDb.setLong("CDHH",0, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if(aBoolean){
                    Common.ServiceReq serviceReq = new Common.ServiceReq();
                    serviceReq.Command =Common.ServiceReq.COMMAND.UPDATE_CDHH;
                    vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
                }
            }
        });
        callback.handle("Service is off!");
    }
}
