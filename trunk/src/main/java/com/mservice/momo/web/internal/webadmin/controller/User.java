package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;

/**
 * Created by ntunam on 4/7/14.
 */
public class User {

    @Action(path = "/user/list")
    public void list(HttpRequestContext context, Handler<Object> callback) {
        callback.handle("Hi there");
    }
}
