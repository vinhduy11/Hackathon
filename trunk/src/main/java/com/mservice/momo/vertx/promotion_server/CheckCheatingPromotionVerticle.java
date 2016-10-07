package com.mservice.momo.vertx.promotion_server;

import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by concu on 9/20/16.
 */
public class CheckCheatingPromotionVerticle extends Verticle {

    public JsonObject glbConfig;
    public Logger logger;
    ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    private boolean isStoreApp;
    private HttpClient httpClient;
    private Set<String> phoneSet;
    private HashMap hashMap;
    private boolean isUAT;

    @Override
    public void start() {

        glbConfig = container.config();
        logger = container.logger();
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        isStoreApp = glbConfig.getBoolean(StringConstUtil.IS_STORE_APP, false);
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        final Common.BuildLog log = new Common.BuildLog(logger);
        final JsonObject joReply = new JsonObject();
        phoneSet = new HashSet<>();
        hashMap = new HashMap();
        isUAT = glbConfig.getBoolean(StringConstUtil.IS_UAT, false);
        loadClientPath(new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj connectorObj) {
                logger.info("host " + connectorObj.host);
                logger.info("port " + connectorObj.port);
                httpClient = vertx.createHttpClient().setHost(connectorObj.host).setPort(connectorObj.port).setKeepAlive(false).setMaxPoolSize(50).setConnectTimeout(60000);
                Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> message) {
                        final JsonObject jsonRequest = message.body();
                        log.setPhoneNumber("check_cheat");
                        if (isUAT) {
                            log.add("data jsonrequest", jsonRequest);
                        }
                        String promotionPath = jsonRequest.getString(StringConstUtil.PATH, "");
                        requestHttpPostToCheatServer(promotionPath, log, jsonRequest, joReply, message);
                    }
                };
                vertx.eventBus().registerLocalHandler(AppConstant.CHECK_CHEATING_PROMOTION_BUS_ADDRESS, myHandler);
            }
        });

    }

    private void requestHttpPostToCheatServer(final String path, final Common.BuildLog log, final JsonObject jsonInfo, final JsonObject joReply, final Message message) {
        final Buffer bufferData = new Buffer();

        final HttpClientRequest httpClientRequest = httpClient.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {

                int statusCode = httpClientResponse != null ? httpClientResponse.statusCode() : 1000;

                if (statusCode != 200) {
                    message.reply(joReply.putNumber(StringConstUtil.ERROR, (statusCode + 10000)));
                    log.add(StringConstUtil.ERROR, statusCode);
                    log.writeLog();
                    return;
                }

                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer bodyBuffer) {
                        try {
                            String data = "".equalsIgnoreCase(bodyBuffer.toString()) ? bufferData.toString() : bodyBuffer.toString();
                            if(!"".equalsIgnoreCase(data.toString()) && Misc.isValidJsonObject(data.toString()))
                            {
                                JsonObject result = new JsonObject(data.trim());
//                                log.add("Rcv json", data.toString());
                                joReply.putNumber(StringConstUtil.PromotionField.ERROR, 0);
                                joReply.putObject(StringConstUtil.PromotionField.RESULT, result);
                                joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Success");
                                message.reply(joReply);
                            }
                            else
                            {
                                joReply.putNumber(StringConstUtil.PromotionField.ERROR, 9998);
                                joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Unexpected result 9998. ");
                                message.reply(joReply);
                            }
                        } catch (DecodeException e) {
                            log.add("ex", e.fillInStackTrace());
                            joReply.putNumber(StringConstUtil.PromotionField.ERROR, 9999);
                            joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Unexpected result 9999. ");
                            message.reply(joReply);
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
                        bufferData.appendBuffer(dataBuffer);
                    }
                });

                httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        joReply.putNumber(StringConstUtil.PromotionField.ERROR, 9999);
                        joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Unexpected result. ");
                        message.reply(joReply);
                        log.writeLog();
                        //todo check
                        return;
                    }
                });
            }
        });
        Buffer buffer = new Buffer(jsonInfo.toString());
        httpClientRequest.putHeader("Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
        httpClientRequest.setTimeout(300000L);
        httpClientRequest.putHeader("Content-Length", buffer.length() + "");
        httpClientRequest.write(buffer).end();
    }

    public void loadClientPath(final Handler<ConnectorHTTPPostPathDb.Obj> callback)
    {
        connectorHTTPPostPathDb.findOne("check_cheat_" + AppConstant.PREFIX, new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj connectorHttpObj) {
                if(connectorHttpObj == null)
                {
                    logger.info("CONNECTOR HTTP PROMOTION CLIENT VERTICLE IS NULL");
                    ConnectorHTTPPostPathDb.Obj connectorHttp = new ConnectorHTTPPostPathDb.Obj();
                    connectorHttp.serviceId = "promotion_server";
                    connectorHttp.host = "1.1.1.1";
                    connectorHttp.port = 0;
                    connectorHttp.path = "/checkScore";
                    callback.handle(connectorHttp);
                }
                else {
                    logger.info("DATA promotion_server" + connectorHttpObj.toJson());
                    callback.handle(connectorHttpObj);
                }
            }
        });
    }
}
