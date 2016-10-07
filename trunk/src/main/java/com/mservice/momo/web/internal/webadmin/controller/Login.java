package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.BackEndUserDb;
import com.mservice.momo.vertx.models.webadmin.BackEndUser;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import com.mservice.momo.web.internal.webadmin.handler.Role;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ntunam on 4/7/14.
 */
public class Login {



    private Vertx vertx;
    private Container container;
    private BackEndUserDb backEndUserDb;

    public Login(Vertx vertx, final Container container) {
        this.vertx = vertx;
        this.container = container;

        this.backEndUserDb = new BackEndUserDb(vertx, container.logger());

        final BackEndUser supper = new BackEndUser();
        backEndUserDb.findOne(supper, new Handler<BackEndUser>() {
            @Override
            public void handle(BackEndUser backEndUser) {
                if(backEndUser==null) {
                    supper.setModelId("admin");
                    supper.password = "admin";
                    supper.addRole(Role.SUPER.toString());
                    backEndUserDb.save(supper, new Handler<String>() {
                        @Override
                        public void handle(String s) {
                            container.logger().info("Create new ADMIN user with default password.");
                        }
                    });
                }
            }
        });

    }

    public void redirect(String page, Handler<Object> callback) {
        Map<String, Object> map = new HashMap<>();
        map.put("$cmd", "redirect");
        map.put("$target", page);
        callback.handle(map);
    }

    @Action(path = "/login")
    public void login(final HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String userId = params.get("userId");
        final String password = params.get("password");

        if(userId==null || password==null) {
            JsonObject json = new JsonObject()
                    .putNumber("error", 2)
                    .putString("desc", "Missing params");
            callback.handle(json);
            return;
        }

        BackEndUser filter = new BackEndUser();
        filter.setModelId(userId);

        backEndUserDb.findOne(filter, new Handler<BackEndUser>() {
            @Override
            public void handle(BackEndUser backEndUser) {
                if (backEndUser == null || !password.equals(backEndUser.password)) {
                    JsonObject json = new JsonObject()
                            .putNumber("error", 1)
                            .putString("desc", "Login fail");
                    callback.handle(json);
                    return;
                }
                JsonObject json = new JsonObject()
                        .putNumber("error", 0)
                        .putString("desc", "Logged in!");


                context.getCookie().putString("sessionId", context.getSession().getString("id"));
                context.getSession().putBoolean("loggedIn", true);
                context.getSession().putString("userId", backEndUser.getModelId());
                context.getSession().putArray("roles", backEndUser.roles);

                callback.handle(json);
            }
        });

//        JsonObject obj = new JsonObject();
//        if ("admin".equals(userId) && "admin".equals(password)) {
//            obj.putNumber("error", 0);cookieString
//
//        } else {
//
//            obj.putNumber("error", 1);
//        }
//
//        callback.handle(obj);
    }

    @Action(path = "/session/logout")
    public void logout(HttpRequestContext context, Handler<Object> callback) {
        context.getSession().removeField("loggedIn");
        context.getSession().removeField("roles");
        context.getSession().removeField("userId");

        for(String fieldName: context.getCookie().getFieldNames()) {
            context.getCookie().removeField(fieldName);
        }

        redirect("/login.html", callback);
    }

    @Action(path = "/session/sessionInfo")
    public void sessionInfo(HttpRequestContext context, final Handler<Object> callback) {
        String userId = context.getSession().getString("userId", "");

        JsonObject obj = new JsonObject();
        obj.putString("sessionId", context.getSession().getString("id"));
        obj.putString("userId", userId);
        obj.putArray("roles", context.getSession().getArray("roles"));

        callback.handle(obj);
    }
}
