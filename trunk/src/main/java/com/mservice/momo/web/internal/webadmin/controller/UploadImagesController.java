package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by locnguyen on 22/07/2014.
 */
public class UploadImagesController {
    public static final int DEFAULT_PRIORITY = 10;

    public static String IMAGE_DIR_PATH = "/tmp";
    public static String IMAGE_STORAGE_HOST = "localhost";


    private Vertx vertx;
    private Logger logger;
    private String STATIC_FILE_DIRECTORY = "";

    public UploadImagesController(Vertx vertx, Container container, String STATIC_FILE_DIRECTORY) {
        this.vertx = vertx;
        logger = container.logger();
        this.STATIC_FILE_DIRECTORY = STATIC_FILE_DIRECTORY;

    }

    @Action(path = "/uploadimage")
    public void sendImageUpload(HttpRequestContext context, final Handler<JsonObject> callback){
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }
    private void response(HttpServerRequest request, JsonObject jo) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(jo.toString());
        request.response().end();
    }

    private void response(HttpServerRequest request, String strResult) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "text/plain ; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(strResult);
        request.response().end();
    }
}