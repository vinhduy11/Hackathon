package com.mservice.momo.gateway.internal.db.mysql;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.model.DBMsg;
import com.mservice.momo.util.JacksonJSONUtils;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by duyhuynh on 18/07/2016.
 */
public class MySQLVerticle extends Verticle {

    Logger logger;
    private HttpClient httpDBClient = null;

    @Override
    public void start() {
        logger = getContainer().logger();
        final JsonObject globalConfig = container.config();

        JsonObject moduleInfo = globalConfig.getObject(DBFactory.Source.MYSQL_MODULE_INFO, new JsonObject());
        String host = moduleInfo.getString(DBFactory.Source.HOST, "0.0.0.0");
        int port = moduleInfo.getInteger(DBFactory.Source.PORT, 13306);
        httpDBClient = vertx.createHttpClient()
                .setHost(host)
                .setPort(port)
                .setMaxPoolSize(5)
                .setConnectTimeout(60000)
                .setKeepAlive(false);
        logger.info("Create DB client to " + host + " in port " + port);

        vertx.eventBus().registerHandler(AppConstant.NOTI_DB_BUS, new Handler<Message>() {
            @Override
            public void handle(Message message) {
                makeDBRequest(httpDBClient, DBFactory.Source.NOTI_PATH, message);
            }
        }, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> voidAsyncResult) {
                logger.info("Register handler " + AppConstant.NOTI_DB_BUS);
            }
        });
        vertx.eventBus().registerHandler(AppConstant.TRAN_DB_BUS, new Handler<Message>() {
            @Override
            public void handle(Message message) {
                makeDBRequest(httpDBClient, DBFactory.Source.TRAN_PATH, message);
            }
        }, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> voidAsyncResult) {
                logger.info("Register handler " + AppConstant.TRAN_DB_BUS);
            }
        });
    }

    private void makeDBRequest(final HttpClient httpDBClient, final String path, final Message message) {

        final JsonObject json = (JsonObject) message.body();
        final DBMsg dbMsg = JacksonJSONUtils.jsonToObj(json.encode(), DBMsg.class);

        HttpClientRequest request = httpDBClient.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse httpClientResponse) {

                int statusCode = httpClientResponse.statusCode();
                if (statusCode != 200) {
                    dbMsg.err = statusCode;
                    dbMsg.des = "Status code " + statusCode;
                    message.reply(JacksonJSONUtils.objToJsonObj(dbMsg));
                    logger.info("db response status code " + statusCode);
                    return;
                }

                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            String data = buffer.toString();
                            DBMsg resMsg = JacksonJSONUtils.jsonToObj(data, DBMsg.class);
                            if (resMsg == null) {
                                dbMsg.err = 402;
                                dbMsg.des = "Invalid reponse " + data;
                                message.reply(JacksonJSONUtils.objToJsonObj(dbMsg));
                                logger.info("db Invalid response");
                                return;
                            }
                            message.reply(JacksonJSONUtils.objToJsonObj(resMsg));
                        } catch (Exception e) {
                            dbMsg.err = 402;
                            dbMsg.des = "Invalid reponse " + e.getMessage();
                            message.reply(JacksonJSONUtils.objToJsonObj(dbMsg));
                            logger.info("db Invalid response " + e.getMessage());
                        }
                    }
                });

                httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        dbMsg.err = 401;
                        dbMsg.des = "Error reponse " + throwable.getMessage();
                        message.reply(JacksonJSONUtils.objToJsonObj(dbMsg));
                        logger.info("db Invalid response " + throwable.getMessage());
                    }
                });
            }
        });

        httpDBClient.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                dbMsg.err = 400;
                dbMsg.des = "Unexpected error " + throwable.getMessage();
                message.reply(JacksonJSONUtils.objToJsonObj(dbMsg));
                logger.info("db request error " + throwable.getMessage());
            }
        });

        Buffer buffer = new Buffer(JacksonJSONUtils.objToJsonObj(dbMsg).encode());
        request.putHeader("Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
        request.putHeader("Content-Length", buffer.length() + "");
        request.write(buffer).end();
    }
}
