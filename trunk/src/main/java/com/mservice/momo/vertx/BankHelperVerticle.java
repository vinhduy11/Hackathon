package com.mservice.momo.vertx;

import com.mservice.bank.entity.BankRequest;
import com.mservice.card.entity.CardRequest;
import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.net.ConnectException;
import java.rmi.RemoteException;

/**
 * Created by duyhuynh on 21/12/2015.
 */
public class BankHelperVerticle extends Verticle {

    // command
    public static final int BANK_IN_OUT = 1;
    public static final int ATM_IN_OUT = 2;
    public static final String TIME = "time";
    public static final String COMMAND = "type";
    public static final String CONTENT_TYPE_DEFAULT = "application/json";

    // response
    public static final String ERROR = "error";
    public static final String DESCRIPTION = "desc";
    public static final String DATA = "dataReq";
    public static final String PHONE = "cmdPhone";

    private static String host;
    private static int port;
    private static String path;
    private HttpClient httpClient;

    private Logger logger;
    private JsonObject glbCfg = new JsonObject();

    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    public void start() {
        logger = container.logger();
        glbCfg = container.config();

        JsonObject db_cfg = glbCfg.getObject("bank");
        host = db_cfg.getString("restHost", "0.0.0.0");
        port = db_cfg.getInteger("restPort", 19090);
        path = db_cfg.getString("restPath", "servicebank");
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        // open connect to connector
        httpClient = vertx.createHttpClient()
                .setHost(host)
                .setPort(port)
                .setMaxPoolSize(20)
                .setConnectTimeout(300000) // 5 phut
                .setKeepAlive(false);
        httpClient.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                httpClient = vertx.createHttpClient()
                        .setHost(host)
                        .setPort(port)
                        .setMaxPoolSize(20)
                        .setConnectTimeout(300000) // 5 phut
                        .setKeepAlive(false);
            }
        });


        // register local bus handler
        EventBus eb = vertx.eventBus();
        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                JsonObject jsonObject = message.body();

                int type = jsonObject.getInteger(COMMAND);
                try {
                    switch (type) {
                        case BANK_IN_OUT:
                            bankInOut(message);
                            break;
                        case ATM_IN_OUT:
                            cashInOutATMBank(message);
                            break;
                        default:
                            logger.warn("BankHelperVerticle NOT SUPPORT COMMAND " + type);
                            break;
                    }
                } catch (ConnectException ce) {
                    jsonObject.putNumber(ERROR, 5008); // timeout
                    jsonObject.putString(DESCRIPTION, "Request timeout + proxy_bank");
                    message.reply(jsonObject);

                } catch (RemoteException re) {
                    jsonObject.putNumber(ERROR, 5008); // timeout
                    jsonObject.putString(DESCRIPTION, "Request timeout + proxy_bank");
                    message.reply(jsonObject);
                } catch (Exception e) {
                    jsonObject.putNumber(ERROR, 9999); // loi he thong
                    jsonObject.putString(DESCRIPTION, e.getMessage());
                    message.reply(jsonObject);
                }
            }
        };
        eb.registerLocalHandler(AppConstant.BankHelperVerticle_ADDRESS, myHandler);
    }

    private void bankInOut(final Message<JsonObject> msg) throws RemoteException, ConnectException {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(msg.body().getString(PHONE));
        log.setTime(System.currentTimeMillis());
        final JsonObject joBankReq = msg.body().getObject(DATA);
        BankRequest bankRequest = new BankRequest(joBankReq);
        if(bankRequest != null)
        {
            connectorHTTPPostPathDb.findOne(bankRequest.getCoreBankCode(), new Handler<ConnectorHTTPPostPathDb.Obj>() {
                @Override
                public void handle(ConnectorHTTPPostPathDb.Obj connectorPath) {
                    HttpClient httpLocalClient = null;
                    if(connectorPath == null)
                    {
                        log.add("Create client", " to " + host + ", port " + port);
                        httpLocalClient = httpClient;
                        log.add("Connector Host global", host);
                        log.add("Connector Port global", port);
                        log.add("Connector Path global", path);
                    }
                    else {
                        log.add("Create client", " to " + connectorPath.host + ", port " + connectorPath.port);
                        httpLocalClient = vertx.createHttpClient()
                                .setHost(connectorPath.host)
                                .setPort(connectorPath.port)
                                .setMaxPoolSize(20)
                                .setConnectTimeout(300000) // 5 phut
                                .setKeepAlive(false);
                        log.add("Connector Host", connectorPath.host);
                        log.add("Connector Port", connectorPath.port);
                        log.add("Connector Path", connectorPath.path);
                    }
                    final String finalPath = connectorPath == null ? path : connectorPath.path;

                    log.add("Connector finalPath", finalPath);
                    HttpClientRequest httpReq = httpLocalClient.post(finalPath, new Handler<HttpClientResponse>() {
                        @Override
                        public void handle(HttpClientResponse httpClientResponse) {
                            int statusCode = httpClientResponse.statusCode();

                            if (statusCode != 200) {

                                log.add(DESCRIPTION, "Proxy bank is offline or internal error. Http status code = " + statusCode);
                                log.add(ERROR, statusCode);
                                log.writeLog();
                                msg.reply(new JsonObject().putNumber(ERROR, (statusCode + 10000)));
                                return;
                            }
                            final Buffer totalBuffer = new Buffer();
                            httpClientResponse.dataHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer buffer) {
                                    if (buffer != null) {
                                        totalBuffer.appendBuffer(buffer);
                                    }
                                }
                            });
                            httpClientResponse.endHandler(new Handler<Void>() {
                                @Override
                                public void handle(Void aVoid) {
                                    JsonObject jsonRpl = new JsonObject();

                                    try {
//                            log.add("Rcv buffer", totalBuffer.toString());
                                        JsonObject result = new JsonObject(totalBuffer.toString());
//                            log.add("Rcv json", result.toString());

                                        jsonRpl.putNumber(ERROR, 0);
                                        jsonRpl.putObject(DATA, result);
                                        msg.reply(jsonRpl);
                                    } catch (DecodeException e) {

                                        jsonRpl.putNumber(ERROR, 9999);
                                        jsonRpl.putString(DESCRIPTION, "Unexpected result.");
                                        msg.reply(jsonRpl);
                                    }
//                        log.add("json res", jsonRpl);
                                    log.writeLog();
                                    return;
                                }
                            });

                            httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable throwable) {
                                    JsonObject jsonRpl = new JsonObject();
                                    jsonRpl.putNumber(ERROR, 9999);
                                    jsonRpl.putString(DESCRIPTION, "Unexpected result.");
                                    msg.reply(jsonRpl);
                                    log.add("json res", jsonRpl);
                                    log.writeLog();
                                    return;
                                }
                            });
                        }
                    });
                    httpReq.putHeader("Content-Type", CONTENT_TYPE_DEFAULT);
                    log.add("Add Content-Type", CONTENT_TYPE_DEFAULT);
                    Buffer buffer = new Buffer(joBankReq.toString());
                    httpReq.putHeader("Content-Length", buffer.length() + "");
                    httpReq.end(buffer);
                    log.add("Add Json data", joBankReq);

                }
            });
        }
        else{
            log.add(DESCRIPTION, "Bank request is null");
            log.writeLog();
            msg.reply(new JsonObject().putNumber(ERROR, (1000 + 10000)));
            log.add(ERROR, 1000);
            return;
        }

    }

    private void cashInOutATMBank(final Message<JsonObject> msg) throws RemoteException, ConnectException {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(msg.body().getString(PHONE));
        log.setTime(System.currentTimeMillis());
        final JsonObject joBankReq = msg.body().getObject(DATA);
        CardRequest cardRequest = new CardRequest(joBankReq);
        if(cardRequest != null)
        {
            logger.info("CARD REQUEST " + cardRequest.getCardInfo());
            connectorHTTPPostPathDb.findOne(cardRequest.getPartnerCode().toLowerCase(), new Handler<ConnectorHTTPPostPathDb.Obj>() {
                @Override
                public void handle(ConnectorHTTPPostPathDb.Obj connectorPath) {
                    HttpClient httpLocalClient = null;
                    if(connectorPath == null)
                    {
                        log.add("Create client", " to " + host + ", port " + port);
                        httpLocalClient = httpClient;
                        log.add("Connector Host global", host);
                        log.add("Connector Port global", port);
                        log.add("Connector Path global", path);
                    }
                    else {
                        log.add("Create client", " to " + connectorPath.host + ", port " + connectorPath.port);
                        httpLocalClient = vertx.createHttpClient()
                                .setHost(connectorPath.host)
                                .setPort(connectorPath.port)
                                .setMaxPoolSize(20)
                                .setConnectTimeout(300000) // 5 phut
                                .setKeepAlive(false);
                        log.add("Connector Host", connectorPath.host);
                        log.add("Connector Port", connectorPath.port);
                        log.add("Connector Path", connectorPath.path);
                    }
                    final String finalPath = connectorPath == null ? path : connectorPath.path;

                    log.add("Connector finalPath", finalPath);
                    HttpClientRequest httpReq = httpLocalClient.post(finalPath, new Handler<HttpClientResponse>() {
                        @Override
                        public void handle(HttpClientResponse httpClientResponse) {
                            int statusCode = httpClientResponse.statusCode();

                            if (statusCode != 200) {

                                log.add(DESCRIPTION, "Proxy bank is offline or internal error. Http status code = " + statusCode);
                                log.writeLog();
                                msg.reply(new JsonObject().putNumber(ERROR, (statusCode + 10000)));
                                log.add(ERROR, statusCode);
                                return;
                            }
                            final Buffer totalBuffer = new Buffer();
                            httpClientResponse.dataHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer buffer) {
                                    if (buffer != null) {
                                        totalBuffer.appendBuffer(buffer);
                                    }
                                }
                            });
                            httpClientResponse.endHandler(new Handler<Void>() {
                                @Override
                                public void handle(Void aVoid) {
                                    JsonObject jsonRpl = new JsonObject();

                                    try {
//                            log.add("Rcv buffer", totalBuffer.toString());
                                        JsonObject result = new JsonObject(totalBuffer.toString());
//                            log.add("Rcv json", result.toString());

                                        jsonRpl.putNumber(ERROR, 0);
                                        jsonRpl.putObject(DATA, result);
                                        msg.reply(jsonRpl);
                                    } catch (DecodeException e) {

                                        jsonRpl.putNumber(ERROR, 9999);
                                        jsonRpl.putString(DESCRIPTION, "Unexpected result.");
                                        msg.reply(jsonRpl);
                                    }
//                        log.add("json res", jsonRpl);
                                    log.writeLog();
                                    return;
                                }
                            });

                            httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable throwable) {
                                    JsonObject jsonRpl = new JsonObject();
                                    jsonRpl.putNumber(ERROR, 9999);
                                    jsonRpl.putString(DESCRIPTION, "Unexpected result.");
                                    msg.reply(jsonRpl);
                                    log.add("json res", jsonRpl);
                                    log.writeLog();
                                    return;
                                }
                            });
                        }
                    });
                    httpReq.putHeader("Content-Type", CONTENT_TYPE_DEFAULT);
                    log.add("Add Content-Type", CONTENT_TYPE_DEFAULT);
                    Buffer buffer = new Buffer(joBankReq.toString());
                    httpReq.putHeader("Content-Length", buffer.length() + "");
                    httpReq.end(buffer);
                    log.add("Add Json data", joBankReq);

                }
            });
        }
        else{
            log.add(DESCRIPTION, "Bank request is null");
            log.writeLog();
            msg.reply(new JsonObject().putNumber(ERROR, (1000 + 10000)));
            log.add(ERROR, 1000);
            return;
        }

    }
}
