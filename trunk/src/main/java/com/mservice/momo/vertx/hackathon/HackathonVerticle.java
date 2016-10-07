package com.mservice.momo.vertx.hackathon;


import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;

public class HackathonVerticle extends Verticle {
    public void start() {
        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                req.response().end("<h1>Hello from my first " +
                        "Vert.x 3 application</h1>");
            }
        }).listen(9669);
    }
}