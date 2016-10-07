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
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by concu on 7/20/16.
 */
public class PromotionClientVerticle extends Verticle {


    private Logger logger;
    private JsonObject glbCfg;
    private boolean isStoreApp;
    private HttpClient httpClient;
    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    private Set<String> phoneSet;
    private HashMap hashMap;
    private boolean isUAT;
    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        final Common.BuildLog log = new Common.BuildLog(logger);
        final JsonObject joReply = new JsonObject();
        phoneSet = new HashSet<>();
        hashMap = new HashMap();
        isUAT = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
        loadClientPath(new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj connectorObj) {
                logger.info("host " + connectorObj.host);
                logger.info("port " + connectorObj.port);
                final String promotionPath = connectorObj.path;
                logger.info("path " + connectorObj.path);
                logger.info("promotionPath " + promotionPath);
                httpClient = vertx.createHttpClient().setHost(connectorObj.host).setPort(connectorObj.port).setKeepAlive(false).setMaxPoolSize(50);
                Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> message) {
                        final JsonObject jsonRequest = message.body();
                        log.setPhoneNumber("promotion");
                        if(isUAT)
                        {
                            log.add("data jsonrequest", jsonRequest);
                        }
//                        log.add("promotion path", promotionPath);
                        final PromotionObj promotionObj = new PromotionObj(jsonRequest);

                        phoneSet.add(promotionObj.phoneNumber);

                        vertx.setTimer(2000, new Handler<Long>() {
                            @Override
                            public void handle(Long waitingTime) {
                                if(phoneSet.remove(promotionObj.phoneNumber))
                                {
                                    requestHttpPostToPromotionServer(promotionPath, log, jsonRequest, joReply, message);
                                }
                                vertx.cancelTimer(waitingTime);
                            }
                        });
                    }
                };
                vertx.eventBus().registerLocalHandler(AppConstant.PROMOTION_CLIENT_BUS_ADDRESS, myHandler);
            }
        });
    }

    private void requestHttpPostToPromotionServer(final String path, final Common.BuildLog log, final JsonObject jsonInfo, final JsonObject joReply, final Message message) {
        final Buffer bufferData = new Buffer();

        final HttpClientRequest httpClientRequest = httpClient.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {

                int statusCode = httpClientResponse != null ? httpClientResponse.statusCode() : 1000;

                if (statusCode != 200) {

//                    log.writeLog();
                    message.reply(joReply.putNumber(StringConstUtil.ERROR, (statusCode + 10000)));
                    log.add(StringConstUtil.ERROR, statusCode);
//                    httpClient.close();
                    log.writeAndClear();
                    return;
                }

                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer bodyBuffer) {
                        try {
                            String data = "".equalsIgnoreCase(bodyBuffer.toString()) ? bufferData.toString() : bodyBuffer.toString();
//                            log.add("Rcv buffer", data.toString());
                            if(!"".equalsIgnoreCase(data.toString()) && Misc.isValidJsonArray(data.toString()))
                            {
                                JsonArray result = new JsonArray(data);
//                                log.add("Rcv json", data.toString());
                                joReply.putNumber(StringConstUtil.PromotionField.ERROR, 0);
                                joReply.putArray(StringConstUtil.PromotionField.RESULT, result);
                                joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Success");
                                message.reply(joReply);
                            }
                            else
                            {
//                                log.add("Rcv json error", "promotion call back data is error");
                                joReply.putNumber(StringConstUtil.PromotionField.ERROR, 9998);
                                joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Unexpected result 9998. ");
                                message.reply(joReply);
                            }
//                            httpClient.close();
                        } catch (DecodeException e) {
                            log.add("ex", e.fillInStackTrace());
                            joReply.putNumber(StringConstUtil.PromotionField.ERROR, 9999);
                            joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Unexpected result 9999. ");
                            message.reply(joReply);
//                            httpClient.close();
                        }
                        if(isUAT)
                        {
                            log.add("json res", joReply.toString());

                            log.writeAndClear();
                        }
                        //todo check
                        return;
                    }
                });

                httpClientResponse.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer dataBuffer) {
//                        log.add("data", dataBuffer.length());
                        bufferData.appendBuffer(dataBuffer);
                    }
                });

                httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        joReply.putNumber(StringConstUtil.PromotionField.ERROR, 9999);
                        joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "Unexpected result. ");
                        message.reply(joReply);
//                        log.add("json res", joReply);
//                        httpClient.close();
                        log.writeAndClear();
                        //todo check
                        return;
                    }
                });
            }
        });
        Buffer buffer = new Buffer(jsonInfo.toString());
        httpClientRequest.putHeader("Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
//        log.add("Add Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
        httpClientRequest.setTimeout(300000L);
        httpClientRequest.putHeader("Content-Length", buffer.length() + "");
        httpClientRequest.write(buffer).end();
//        log.add("Add Json data", jsonInfo);
    }

    public void loadClientPath(final Handler<ConnectorHTTPPostPathDb.Obj> callback)
    {
        connectorHTTPPostPathDb.findOne("promotion_" + AppConstant.PREFIX, new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj connectorHttpObj) {
                if(connectorHttpObj == null)
                {
                    logger.info("CONNECTOR HTTP PROMOTION CLIENT VERTICLE IS NULL");
                    ConnectorHTTPPostPathDb.Obj connectorHttp = new ConnectorHTTPPostPathDb.Obj();
                    connectorHttp.serviceId = "promotion_server";
                    connectorHttp.host = "1.1.1.1";
                    connectorHttp.port = 0;
                    connectorHttp.path = "/promotion";
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
