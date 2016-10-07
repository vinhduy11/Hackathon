package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 4/15/14.
 */
public class SystemInfo {

    @Action(path = "/service/system/getInfo")
    public void getTestInfo(HttpRequestContext context, Handler<Object> callback) {

        JsonObject obj = new JsonObject();

        obj.putNumber("cores", Runtime.getRuntime().availableProcessors());
        obj.putNumber("freeMemory", Runtime.getRuntime().freeMemory());
        obj.putNumber("totalMemory", Runtime.getRuntime().totalMemory());

        callback.handle(obj);
    }
}
