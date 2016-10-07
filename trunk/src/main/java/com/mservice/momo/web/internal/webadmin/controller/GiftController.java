package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.gift.GiftTypeDb;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Date;
import java.util.List;

/**
 * Created by nam on 10/6/14.
 */
public class GiftController {
    private Vertx vertx;
    private Container container;
    private GiftTypeDb giftTypeDb;

    private GiftManager giftManager;

    public GiftController(final Vertx vertx,final Container container, JsonObject globalConfig) {
        this.vertx = vertx;
        this.container = container;
        giftTypeDb = new GiftTypeDb(vertx, container.logger());
        giftManager = new GiftManager(vertx, container.logger(), globalConfig);

//        vertx.setTimer(2000, new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                giftManager.createGift(987568815, 20000, "TYPE01", new Handler<JsonObject>() {
//                    @Override
//                    public void handle(JsonObject event) {
//                        System.err.println(event);
//                    }
//                });
//            }
//        });
    }

    @Action(path = "/service/getAvailableTranType")
    public void getAvailableTranType(HttpRequestContext context, final Handler<Object> callback) {
        giftManager.getAllTranType(new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray arr) {
                callback.handle(arr);
            }
        });
    }

    @Action(path = "/service/gift/type/getAll")
    public void getAll(HttpRequestContext context, final Handler<Object> callback) {

        giftTypeDb.find(null, 10000, new Handler<List<GiftType>>() {
            @Override
            public void handle(List<GiftType> types) {
                JsonArray array = new JsonArray();
                for (GiftType type : types) {
                    array.add(type.toJsonObject());
                }

                callback.handle(array);
            }
        });
    }

    @Action(path = "/service/gift/type/create")
    public void create(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().formAttributes();
        GiftType giftType = new GiftType();
        try {
            giftType.setModelId(params.get("_id"));
            giftType.serviceId = params.get("serviceId");
            giftType.name = params.get("name");
            giftType.desc = params.get("desc");
            giftType.icon = params.get("icon");
            giftType.image = params.get("image");
            giftType.transfer = "true".equalsIgnoreCase(params.get("transfer"));
            giftType.status = Integer.parseInt(params.get("status"));
            giftType.modifyDate = new Date();
            giftType.isNew = "true".equalsIgnoreCase(params.get("isNew"));
            giftType.visible = "true".equalsIgnoreCase(params.get("visible"));
            giftType.price = new JsonArray();
            giftType.policy = params.get("policy");

            String[] arr = params.get("price").split(",");
            for (String str : arr) {
                try {
                    giftType.price.add(Long.parseLong(str));
                } catch (NumberFormatException e) {
                }
            }
        } catch (Exception e) {
            callback.handle(new JsonObject().putNumber("error", 2).putString("desc", e.getMessage()));
            return;
        }
        if (!giftType.isInvalid()) {
            callback.handle(new JsonObject().putNumber("error", 1).putString("desc", "invalid param values"));
            return;
        }
//        if (giftType.getModelId() == null || giftType.getModelId().isEmpty()) {
//            giftTypeDb.save(giftType, new Handler<String>() {
//                @Override
//                public void handle(String event) {
//                    callback.handle(new JsonObject().putNumber("error", 0).putString("desc", "created"));
//                }
//            });
//            return;
//        }
        giftTypeDb.update(giftType, true, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                callback.handle(new JsonObject().putNumber("error", 0));
            }
        });
    }

}
