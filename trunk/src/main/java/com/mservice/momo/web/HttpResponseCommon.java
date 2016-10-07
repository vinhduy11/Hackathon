package com.mservice.momo.web;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 3/5/15.
 */
public class HttpResponseCommon {
    public static void response(HttpServerRequest request, JsonObject jsonObject) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");
        request.response().write(jsonObject.toString());

        request.response().end();
    }
}
