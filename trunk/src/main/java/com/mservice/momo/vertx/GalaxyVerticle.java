package com.mservice.momo.vertx;

import com.mservice.momo.web.Cookie;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.controller.GalaxyPromoController;
import com.mservice.momo.web.internal.webadmin.handler.ControllerMapper;
import com.mservice.momo.web.internal.webadmin.handler.RenderHandler;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class GalaxyVerticle extends Verticle {

    private static int PORT = 38080;
    private static String HOST_ADDRESS = "0.0.0.0";

    public static boolean isAvatarStorage;

    @Override
    public void start() {

        final Logger logger = container.logger();
        JsonObject globalConfig = container.config();
        JsonObject glxConfig = globalConfig.getObject("galaxyVerticle", null);

        if(glxConfig == null){
            logger.info("WEB GALAXY KHONG START TREN SERVER NAY !!!");
            return;
        }

        if(!glxConfig.getBoolean("enable", false)) {
            logger.info("WEB GALAXY IS DISABLE!!!");
            return;
        }

        this.PORT = glxConfig.getInteger("port", 38080);
        this.HOST_ADDRESS = glxConfig.getString("hostAddress", "0.0.0.0");

        final Map<String, JsonObject> sessions = new HashMap<>();

        final ControllerMapper controllerMapper = new ControllerMapper(vertx, container);

        controllerMapper.addController(new GalaxyPromoController(vertx, container));

        final RenderHandler renderHandler = new RenderHandler(vertx, container);

        controllerMapper.setNextHandler(renderHandler);

        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest request) {

                        request.expectMultiPart(true);

                        request.endHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                System.out.println("end");
                            }
                        });

                        request.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer postData) {

                                String cookieString = request.headers().get("Cookie");
                                System.out.println("Cookie: " + cookieString);
                                if (cookieString == null || cookieString.trim().length() == 0) {
                                    cookieString = "{}";
                                }
                                Cookie cookie;
                                try {
                                    cookie = new Cookie(cookieString);
                                } catch (DecodeException e) {
                                    cookie = new Cookie("{}");
                                    cookie.putNumber("time", System.currentTimeMillis());
                                }

                                JsonObject session = null;
                                String sessionId = cookie.getString("sessionId");
                                if (sessionId != null) {
                                    sessionId = sessionId.trim();
                                    if (sessionId.length() > 0) {
                                        session = sessions.get(sessionId);
                                    }
                                }
                                if (session == null) {
                                    sessionId = UUID.randomUUID().toString();
                                    session = new JsonObject()
                                            .putString("id", sessionId);
                                    sessions.put(sessionId, session);
                                }

                                HttpRequestContext context = new HttpRequestContext(request, cookie, session);
                                controllerMapper.handle(context);
                            }
                        });
                    }
                })

                .listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
                    @Override
                    public void handle(AsyncResult<HttpServer> event) {
                        if (event.failed()) {
                            logger.error("GalaxyVerticle can't listen on " + HOST_ADDRESS + ":" + PORT);
                            event.cause().printStackTrace();
                            return;
                        }
                        logger.info("GalaxyVerticle is listening on " + HOST_ADDRESS + ":" + PORT);
                    }
                });
    }

}
