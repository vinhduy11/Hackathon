package com.mservice.momo.web.internal.webadmin.controller;

import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 9/19/14.
 */
public class ResourceController {
    private Vertx vertx;
    private Container container;

    public ResourceController(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
    }
//
//    public void redirect(String page, Handler<Object> callback) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("$cmd", "redirect");
//        map.put("$target", page);
//        callback.handle(map);
//    }
//
//    public void responseFile(String page, Handler<Object> callback) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("$template", page);
//        callback.handle(map);
//    }
//
//    @Action(path = "/html/storeComments.html", roles = {Role.SUPER, Role.STORE_COMMENT})
//    public void index(HttpRequestContext context, Handler<Object> callback) {
//        responseFile("/html/storeComments.html", callback);
//    }
//
//    @Action(path = "/default.html")
//    public void defaultPage(HttpRequestContext context, Handler<Object> callback) {
//        String userId = context.getSession().getString("userId");
//        if (userId == null || userId.isEmpty()) {
//            redirect("/login.html", callback);
//            return;
//        }
//        responseFile("/html/main.html", callback);
//    }
//
//    @Action(path = "/html/main.html")
//    public void main(HttpRequestContext context, Handler<Object> callback) {
//        String userId = context.getSession().getString("userId");
//        if (userId == null || userId.isEmpty()) {
//            redirect("/login.html", callback);
//            return;
//        }
//        responseFile("/html/main.html", callback);
//    }
}
