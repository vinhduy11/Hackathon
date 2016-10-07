package com.mservice.momo.vertx.napas;

import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;


/**
 * Created by KhoaNguyen on 9/22/2016.
 */
/*
This server used to get request from Napas.
 */
public class NapasServiceVerticle extends Verticle {

    JsonObject glbConfig;
    Logger logger;
    String HOST = "";
    int PORT = 0;
    ConnectorHTTPPostPathDb connectorHTTPPostPathDb;

    @Override
    public void start() {
        glbConfig = container.config();
        logger = container.logger();
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        loadNapasService(new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj connectorPathObj) {
                HOST = connectorPathObj == null ? "0.0.0.0" : connectorPathObj.host;
                PORT = connectorPathObj == null ? 8100 : connectorPathObj.port;
                createServer();
            }
        });

    }

    private void createServer() {
        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest request) {
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(this.getClass().getSimpleName());
                if ("".equalsIgnoreCase(request.path())) {
                    request.bodyHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer bufferData) {
                            log.add("Buffer data from NAPAS ", bufferData.toString());
                            //todo execute napas data.
                            log.writeLog();
                        }
                    });
                } else if ("".equalsIgnoreCase(request.path())) {
                    request.bodyHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer bufferData) {
                            log.add("Buffer data from NAPAS ", bufferData.toString());
                            //todo execute napas data.
                            log.writeLog();
                        }
                    });
                } else {
                    log.add("DESC " + this.getClass().getSimpleName(), "ERROR PATH");
                    log.writeLog();

                }
            }
        }).listen(PORT, HOST);
    }

    private void loadNapasService(Handler<ConnectorHTTPPostPathDb.Obj> callback) {
        connectorHTTPPostPathDb.findOne("napas", new Handler<ConnectorHTTPPostPathDb.Obj>() {
                    @Override
                    public void handle(ConnectorHTTPPostPathDb.Obj connectorObj) {

                    }
                }
        );
    }
}
