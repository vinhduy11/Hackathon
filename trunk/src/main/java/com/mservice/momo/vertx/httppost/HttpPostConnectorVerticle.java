package com.mservice.momo.vertx.httppost;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.processor.Common;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.*;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by concu on 2/17/16.
 */
public class HttpPostConnectorVerticle extends Verticle {

    Logger logger;
    private JsonObject httpConfig;
    private HttpClient httpDBClient = null;

    @Override
    public void start() {
        logger = getContainer().logger();

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final Common.BuildLog log = new Common.BuildLog(logger);
                String phoneNumber = reqJson.getString(StringConstUtil.NUMBER, "");
                String host = reqJson.getString(StringConstUtil.ConnectorNotification.HOST, "");
                int port = reqJson.getInteger(StringConstUtil.ConnectorNotification.PORT, 0);
                String serviceId = reqJson.getString(StringConstUtil.SERVICE_ID, "");
                String path = reqJson.getString(StringConstUtil.ConnectorNotification.PATH, "");
                JsonObject jsonInfo = reqJson.getObject(StringConstUtil.ConnectorNotification.JSON_INFO, new JsonObject());
                HttpClient httpClient = vertx.createHttpClient().setKeepAlive(false).setMaxPoolSize(20).setPort(port).setHost(host);
                log.setPhoneNumber(phoneNumber);
                log.add("phoneNumber", phoneNumber);
                log.add("host", host);
                log.add("port", port);
                log.add("serviceId", serviceId);
                log.add("path", path);
                log.add("jsonInfo", jsonInfo.toString());
                JsonObject joReply = new JsonObject();
                requestHttpPostToConnector(httpClient, path, log, serviceId, jsonInfo, joReply, message);

            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.HTTP_POST_CONNECTOR_BUS_ADDRESS, myHandler);

        // Create internal http server
        final JsonObject globalConfig = container.config();
        httpConfig = globalConfig.getObject(Const.INTERNAL_HTTP_POST_CFG.CONFIG_NAME, new JsonObject());
        logger.info("httpConfig: " + httpConfig.toString());
        if (httpConfig.getBoolean(Const.INTERNAL_HTTP_POST_CFG.IS_SERVER, false)) {
            createGlbHttpServer(vertx, logger,
                    httpConfig.getInteger(Const.INTERNAL_HTTP_POST_CFG.PORT, 20000),
                    httpConfig.getString(Const.INTERNAL_HTTP_POST_CFG.HOST, "1.1.1.1"));
        }
        vertx.eventBus().registerLocalHandler(AppConstant.HTTP_POST_BUS_ADDRESS, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject request = message.body();
                logger.info("[HttpPostInternal]Send via http post");
                doRequest(request, new Handler<Object>() {
                    @Override
                    public void handle(final Object obj) {

                        JsonObject resJson = null;
                        try {
                            resJson = new JsonObject(String.valueOf(obj));
                        } catch (Exception e) {
                        }
                        if (resJson != null && resJson.getInteger(StringConstUtil.ERROR, -1) == 0) {
                            String resDataString = resJson.getString(StringConstUtil.DESCRIPTION);
                            if (isJsonArray(resDataString)) {
                                message.reply(new JsonArray(resDataString));
                            } else if (isJsonObject(resDataString)) {
                                message.reply(new JsonObject(resDataString));
                            } else if (NumberUtils.isNumber(resDataString)) {
                                message.reply(new Integer(resDataString));
                            }
                        } else {
                            logger.info("[HttpPostInternal]Try send via cluster");
                            final String bus = request.getString(Const.INTERNAL_HTTP_POST_CFG.BUS);
                            final JsonObject data = request.getObject(Const.INTERNAL_HTTP_POST_CFG.DATA);
                            // Try send via cluster
                            vertx.eventBus().send(bus, data, new Handler<Message<Object>>() {
                                @Override
                                public void handle(Message<Object> clusterRes) {
                                    message.reply(clusterRes.body());
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private boolean isJsonArray(String s) {
        JsonArray resJson = null;
        try {
            resJson = new JsonArray(s);
        } catch (Exception e) {
        }
        return resJson != null;
    }

    private boolean isJsonObject(String s) {
        JsonObject resJson = null;
        try {
            resJson = new JsonObject(s);
        } catch (Exception e) {
        }
        return resJson != null;
    }

    private void requestHttpPostToConnector(final HttpClient httpClient, final String path, final Common.BuildLog log, final String serviceId, final JsonObject jsonInfo, final JsonObject joReply, final Message message) {
        final Buffer bufferData = new Buffer();

        final HttpClientRequest httpClientRequest = httpClient.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {

                int statusCode = httpClientResponse != null ? httpClientResponse.statusCode() : 1000;

                if (statusCode != 200) {

                    log.add(StringConstUtil.DESCRIPTION, "Proxy " + serviceId + " is offline or internal error. Http status code = " + statusCode);
//                    log.writeLog();
                    message.reply(joReply.putNumber(StringConstUtil.ERROR, (statusCode + 10000)));
                    log.add(StringConstUtil.ERROR, statusCode);
                    httpClient.close();
                    log.writeLog();
                    return;
                }

                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer bodyBuffer) {
                        try {
                            String data = "".equalsIgnoreCase(bodyBuffer.toString()) ? bufferData.toString() : bodyBuffer.toString();
//                            log.add("Rcv buffer", data.toString());
                            JsonObject result = new JsonObject(data);
//                            log.add("Rcv json", data.toString());
                            joReply.putNumber(StringConstUtil.ERROR, 0);
                            joReply.putObject(BankHelperVerticle.DATA, result);
                            message.reply(joReply);
                            httpClient.close();
                        } catch (DecodeException e) {
                            log.add("ex", e.fillInStackTrace());
                            joReply.putNumber(StringConstUtil.ERROR, 9999);
                            joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
                            message.reply(joReply);
                            httpClient.close();
                        }
//                        log.add("json res", bodyBuffer.toString());
                        log.writeLog();
                        //todo check
                        return;
                    }
                });

                httpClientResponse.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer dataBuffer) {
                        log.add("data", dataBuffer.length());
                        bufferData.appendBuffer(dataBuffer);
                    }
                });

                httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        joReply.putNumber(StringConstUtil.ERROR, 9999);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
                        message.reply(joReply);
//                        log.add("json res", joReply);
                        httpClient.close();
                        log.writeLog();
                        //todo check
                        return;
                    }
                });
            }
        });
        Buffer buffer = new Buffer(jsonInfo.toString());
        httpClientRequest.putHeader("Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
        log.add("Add Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
        httpClientRequest.setTimeout(120000L);
        httpClientRequest.putHeader("Content-Length", buffer.length() + "");
        httpClientRequest.write(buffer).end();
        log.add("Add Json data", jsonInfo);
    }

    public void doRequest(JsonObject requestData, final Handler<Object> callback) {
        String host = httpConfig.getString(Const.INTERNAL_HTTP_POST_CFG.HOST, "1.1.1.1");
        int port = httpConfig.getInteger(Const.INTERNAL_HTTP_POST_CFG.PORT, 20000);
        String formEUhost = httpConfig.getString(Const.INTERNAL_HTTP_POST_CFG.FORM_EU_HOST, "1.1.1.1");
        int formEUport = httpConfig.getInteger(Const.INTERNAL_HTTP_POST_CFG.FORM_EU_PORT, 20000);
        String formDGDhost = httpConfig.getString(Const.INTERNAL_HTTP_POST_CFG.FORM_DGD_HOST, "1.1.1.1");
        int formDGDport = httpConfig.getInteger(Const.INTERNAL_HTTP_POST_CFG.FORM_DGD_PORT, 20000);

        final String bus = requestData.getString(Const.INTERNAL_HTTP_POST_CFG.BUS, "");
        final JsonObject data = requestData.getObject(Const.INTERNAL_HTTP_POST_CFG.DATA, new JsonObject());

        HttpClient newhttpClient = vertx.createHttpClient()
                .setKeepAlive(false)
                .setPort(port)
                .setHost(host);
        if (AppConstant.DGD_SubmitForm_Address.equalsIgnoreCase(bus)) {
            newhttpClient = vertx.createHttpClient()
                    .setKeepAlive(false)
                    .setPort(formDGDport)
                    .setHost(formDGDhost);
            logger.info("[HttpPostInternalServer]Create http client to DGD_SubmitForm " + formDGDhost + " port " + formDGDport);
        } else if (AppConstant.SubmitForm_Address.equalsIgnoreCase(bus)) {
            newhttpClient = vertx.createHttpClient()
                    .setKeepAlive(false)
                    .setPort(formEUport)
                    .setHost(formEUhost);
            logger.info("[HttpPostInternalServer]Create http client to SubmitForm " + formEUhost + " port " + formEUport);
        } else {
            logger.info("[HttpPostInternalServer]Create http client to " + host + " port " + port);
        }
        final HttpClient newhttpClientFinal = newhttpClient;

        final HttpClientRequest httpClientRequest = newhttpClientFinal.post("", new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {
                int statusCode = httpClientResponse != null ? httpClientResponse.statusCode() : 1000;
                if (statusCode != 200) {
                    logger.error("[HttpPostInternalClient]" + StringConstUtil.DESCRIPTION + ": Http Server is offline or internal error. Http status code = " + statusCode);
                    logger.info("[HttpPostInternalClient]" + StringConstUtil.ERROR + ": " + statusCode);
                    callback.handle(new JsonObject()
                            .putNumber(StringConstUtil.ERROR, (statusCode + 10000)));
                    newhttpClientFinal.close();
                    return;
                }

                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer bodyBuffer) {
                        try {
                            String data = "".equalsIgnoreCase(bodyBuffer.toString()) ? bodyBuffer.toString() : bodyBuffer.toString();
                            callback.handle(new JsonObject()
                                    .putNumber(StringConstUtil.ERROR, 0)
                                    .putString(StringConstUtil.DESCRIPTION, data));
                            newhttpClientFinal.close();
                        } catch (DecodeException e) {
                            logger.error("ex: " + e.fillInStackTrace());
                            callback.handle(new JsonObject()
                                    .putNumber(StringConstUtil.ERROR, 9999)
                                    .putString(StringConstUtil.DESCRIPTION, "Unexpected result."));
                            newhttpClientFinal.close();
                        }
                        return;
                    }
                });

                httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        logger.error("[HttpPostInternalClient]ex: " + throwable.fillInStackTrace());
                        callback.handle(new JsonObject()
                                .putNumber(StringConstUtil.ERROR, 9999)
                                .putString(StringConstUtil.DESCRIPTION, "Unexpected result."));
                        newhttpClientFinal.close();
                        return;
                    }
                });
            }
        });
        httpClientRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                logger.debug("[HttpPostInternalClient]ex: " + throwable.fillInStackTrace());
                callback.handle(new JsonObject()
                        .putNumber(StringConstUtil.ERROR, 9999)
                        .putString(StringConstUtil.DESCRIPTION, "Unexpected result."));
                newhttpClientFinal.close();
                return;
            }
        });

        Buffer buffer = new Buffer(requestData.toString());
        httpClientRequest.putHeader("Content-Type", "application/json;charset=utf-8");
        httpClientRequest.setTimeout(25000L);
        httpClientRequest.putHeader("Content-Length", buffer.length() + "");
        httpClientRequest.end(buffer);
    }

    public void createGlbHttpServer(final Vertx vertx, final Logger logger, final int port, final String host) {
        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest httpRequest) {
                final HttpServerResponse response = httpRequest.response();
                response.putHeader("Content-Type", "application/json;charset=utf-8");
                response.putHeader("Access-Control-Allow-Origin", "*");

                httpRequest.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        logger.info("[HttpPostInternalServer] receive " + (buffer != null ? buffer.toString() : ""));
                        if (buffer == null || !StringUtils.isNotEmpty(buffer.toString())) {
                            logger.error("[HttpPostInternalServer] Invalid message");
                            response.setStatusCode(500).end("Invalid message");
                        } else {
                            JsonObject requestJson = new JsonObject(buffer.toString());
                            String bus = requestJson.getString(Const.INTERNAL_HTTP_POST_CFG.BUS, "");
                            JsonObject data = requestJson.getObject(Const.INTERNAL_HTTP_POST_CFG.DATA, new JsonObject());
                            logger.error("[HttpPostInternalServer] Send to bus " + bus);
                            vertx.eventBus().send(bus, data, new Handler<Message<Object>>() {
                                @Override
                                public void handle(Message<Object> message) {
                                    String resData = message.body().toString();
                                    logger.info("[HttpPostInternalServer] response " + resData);
                                    response.end(resData);
                                }
                            });
                        }
                    }
                });
                httpRequest.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        response.setStatusCode(500).end(throwable.getMessage());
                    }
                });
            }
        }).listen(port, host, new Handler<AsyncResult<HttpServer>>() {
            @Override
            public void handle(AsyncResult<HttpServer> httpServerAsyncResult) {
                if (httpServerAsyncResult.succeeded()) {
                    logger.info("HttpPostInternalServer begin listening on " + host + ":" + port);
                } else {
                    logger.error("HttpPostInternalServer can not binding on " + host + ":" + port + ", error" + httpServerAsyncResult.cause().getMessage());
                }
            }
        });
    }
}
