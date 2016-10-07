package com.mservice.momo.vertx;

import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.gateway.internal.core.NewCoreConnectorVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.SoapInProcess;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.InfoProcess;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by concu on 4/26/16.
 */
public class LoginVerticle extends Verticle {

    public static final String NAMED_ACTION = "named";
    public static final String UNNAMED_ACTION = "unnamed";
    private Logger logger;
    private JsonObject glbCfg;
    private boolean isStoreApp;

    private String HOST_ADDRESS = "";
    private int PORT = 0;
    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    private HttpServer httpServer;
    private SoapInProcess mSoapProcessor;
    private InfoProcess infoProcess;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        mSoapProcessor = new SoapInProcess(logger, glbCfg);

        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        // jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());
        loadConfig(connectorHTTPPostPathDb, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                createServer();
            }
        });
        infoProcess = new InfoProcess(vertx, logger, glbCfg);
    }

    private void createServer() {
        httpServer = vertx.createHttpServer();
//        httpServer.setSSL(true);
        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                                    @Override
                                    public void handle(final HttpServerRequest request) {

                                        final HttpServerResponse response = request.response();
                                        logger.info("[WebService] [" + request.method() + "] " + " uri: " + request.uri() + " path: " + request.path());
                                        final JsonObject joReply = new JsonObject();
                                        final String path = request.path();
                                        for (int i = 0; i < request.headers().size(); i++) {
                                            logger.info("key " + request.headers().entries().get(i).getKey() + " value " + request.headers().entries().get(i).getValue());
                                        }
                                        if (request.method().equalsIgnoreCase("POST")) {
                                            if ("/login".equalsIgnoreCase(path)) {
                                                request.bodyHandler(new Handler<Buffer>() {
                                                                        @Override
                                                                        public void handle(final Buffer buffer) {
                                                                            logger.info(buffer.getBytes());
                                                                            logger.info(buffer.toString());
                                                                            final Common.BuildLog log = new Common.BuildLog(logger);
                                                                            try {
                                                                                final String[] datas = buffer.toString().split(MomoMessage.BELL);
                                                                                logger.info(datas.length);
                                                                                if (Misc.isValidJsonObject(buffer.toString())) {
                                                                                    JsonObject joData = new JsonObject(buffer.toString());
                                                                                    String phoneNumber = joData.getString("phone", "");
                                                                                    String pin = joData.getString("pin", "");
                                                                                    logger.info("PHONE NUNBER LOGIN " + phoneNumber);
                                                                                    logger.info("PIN LOGIN " + pin);
                                                                                    NewCoreConnectorVerticle.getBalance(vertx, phoneNumber, pin, logger, new Handler<Integer>() {
                                                                                                @Override
                                                                                                public void handle(Integer loginResult) {
                                                                                                    if (loginResult != MomoProto.LogInReply.ResultCode.ALL_OK_VALUE) {
                                                                                                        request.response().end("false");
                                                                                                    } else {
                                                                                                        request.response().end("true");
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                    );
                                                                                } //END data.length > 4
                                                                                else {
                                                                                    logger.info("Loi du lieu gui sang thieu hoac sai format");
                                                                                    logger.info("Thong tin nhan tu app sai");
                                                                                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                                                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                                                                    request.response().end(joReply.toString());
                                                                                    return;
                                                                                }
                                                                            } catch (Exception e) {
                                                                                e.printStackTrace();
                                                                                //Error tu app, response ve luon
                                                                                //JsonObject joReply = new JsonObject();
                                                                                logger.info("Thong tin nhan tu app sai");
                                                                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                                                                request.response().end(joReply.toString());
                                                                                return;
                                                                            }
                                                                        }
                                                                    }
                                                );
                                            } else if (("/" + NAMED_ACTION).equalsIgnoreCase(path) || ("/" + UNNAMED_ACTION).equalsIgnoreCase(path)) {
                                                response.putHeader("Content-Type", "application/json;charset=utf-8");
                                                response.putHeader("Access-Control-Allow-Origin", "*");

                                                request.bodyHandler(new Handler<Buffer>() {
                                                    @Override
                                                    public void handle(Buffer buffer) {
                                                        logger.info(buffer.toString());
                                                        try {
                                                            if (Misc.isValidJsonObject(buffer.toString())) {
                                                                JsonObject joData = new JsonObject(buffer.toString());
                                                                final int phoneNumber = joData.getInteger("phone", 0);
                                                                logger.info("PHONE NUNBER NEED DO NAMED " + phoneNumber);
                                                                final String agent = "0" + phoneNumber;
                                                                logger.info("path: " + path);

                                                                if (("/" + UNNAMED_ACTION).equalsIgnoreCase(path)) {
                                                                    doUnnamed(joData, response);
                                                                } else {
                                                                    infoProcess.processAgentModifyNew(joData, new Handler<JsonObject>() {
                                                                        @Override
                                                                        public void handle(JsonObject joReply) {
                                                                            response.end(joReply.toString());
                                                                        }
                                                                    });
                                                                }
                                                            } else {
                                                                logger.warn("Loi du lieu gui sang thieu hoac sai format");
                                                                logger.warn("Thong tin nhan tu app sai");
                                                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(SoapError.PARAM_INVALID_REQUIRED));
                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, SoapError.PARAM_INVALID_REQUIRED);
                                                                response.end(joReply.toString());
                                                                return;
                                                            }
                                                        } catch (Exception e) {
                                                            logger.warn("Thong tin nhan tu app sai", e);
                                                            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(SoapError.PARAM_INVALID_REQUIRED));
                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, SoapError.PARAM_INVALID_REQUIRED);
                                                            response.end(joReply.toString());
                                                            return;
                                                        }
                                                    }
                                                });
                                                request.exceptionHandler(new Handler<Throwable>() {
                                                    @Override
                                                    public void handle(Throwable throwable) {
                                                        logger.warn("Thong tin nhan tu app sai", throwable.getCause());
                                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(SoapError.PARAM_INVALID_REQUIRED));
                                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, SoapError.PARAM_INVALID_REQUIRED);
                                                        response.end(joReply.toString());
                                                    }
                                                });
                                            } else {
                                                response.setStatusCode(404).end();
                                                return;
                                            }
                                        } else {
                                            request.response().end("got request");
                                            return;
                                        }
                                    }
                                }
                ).

                listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
                                @Override
                                public void handle(AsyncResult<HttpServer> event) {
                                    if (event.succeeded()) {
                                        logger.info("LoginVerticle's listening on " + HOST_ADDRESS + ":" + PORT);
                                    } else {
                                        logger.error("LoginVerticle can not binding on " + HOST_ADDRESS + ":" + PORT, event.cause());
                                    }
                                }
                            }
                    );
    }

    private void doUnnamed(final JsonObject joData, final HttpServerResponse response) {

        final JsonObject joReply = new JsonObject();

        final int agent = joData.getInteger("phone", 0);

        final Buffer soapBuffer = MomoMessage.buildBuffer(SoapProto.MsgType.SET_AGENT_NAMED_VALUE
                , 0
                , agent
                , SoapProto.ZaloGroup.newBuilder()
                        .setZaloGroup(UNNAMED_ACTION)
                        .setZaloCapsetId("")
                        .setZaloUpperLimit("")
                        .build().toByteArray());

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, soapBuffer, new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> reply) {
                logger.info(agent + ", set group momo " + reply.body());
                int rcode = reply.body() ? SoapError.SUCCESS : SoapError.SYSTEM_ERROR;
                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(rcode));
                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, rcode);
                response.end(joReply.toString());
                return;
            }
        });
    }

    private void loadConfig(ConnectorHTTPPostPathDb connectorHTTPPostPathDb, final Logger logger, final Handler<JsonObject> callback)
    {
        logger.info("FUNCTION " + "loadConfig LoginVerticle");
        if(connectorHTTPPostPathDb == null)
        {
            connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        }

        connectorHTTPPostPathDb.findOne("apex", new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj pathObj) {
                if(pathObj == null)
                {
                    logger.info("pathObj = null");
                    HOST_ADDRESS = "172.16.43.22";
                    PORT = 8091;
                    JsonObject joCallBack = new JsonObject().putString("HOST_ADDRESS", HOST_ADDRESS).putNumber("PORT", PORT);
                    callback.handle(joCallBack);
                }
                else {
                    HOST_ADDRESS = pathObj.host;
                    PORT = pathObj.port;
                    JsonObject joCallBack = new JsonObject().putString("HOST_ADDRESS", HOST_ADDRESS).putNumber("PORT", PORT);
                    callback.handle(joCallBack);
                }

            }
        });
    }
    }
