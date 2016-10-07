package com.mservice.momo.vertx.promotion_server;

import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by congnguyen on 27/06/2016.
 *
 * prepare for new logic : tập trung thành nơi nhận request xử lý cho promotion
 */
public class PromotionServerVerticle extends Verticle {

    private Logger logger;

    private JsonObject glbCfg;
    private String HOST_ADDRESS = "";
    private int PORT = 0;
    private PromotionLoader promotionLoader;
    ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    @Override
    public void start() {
        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
//        joServerConfig = glbCfg.getObject("promotion_server", new JsonObject());
        promotionLoader = new PromotionLoader(vertx, logger, glbCfg);
        loadConfig(connectorHTTPPostPathDb, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                createServer();
            }
        });
    }

    private void createServer() {
        HttpServer server = vertx.createHttpServer();

        Handler<HttpServerRequest> promotionHandler = new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest request) {
                String path = request.path();

                if(path.equalsIgnoreCase("/promotion") && request.method().equalsIgnoreCase("POST")) {
                    //params to execute Promotion process
                    request.bodyHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer buffer) {
                            logger.info("Data receive from client promotion " + buffer.toString());
                            if(Misc.isValidJsonObject(buffer.toString()))
                            {
                                JsonObject joData = new JsonObject(buffer.toString());
                                promotionLoader.executePromotionVerticle(joData, new Handler<JsonArray>() {
                                    @Override
                                    public void handle(JsonArray listPromotionResults) {
                                        logger.info("send buffer " + listPromotionResults.toString());
                                        request.response().end(listPromotionResults.toString());
                                    }
                                });
                            }
                            else {
                                logger.info("data receive from client promotion is not json object " + buffer.toString());
                                responseError(5000, PromoContentNotification.PromotionErrorMap.get(5000), request);
                            }
                        }
                    });
                } else {
                    responseError(5001, PromoContentNotification.PromotionErrorMap.get(5001), request);
                }
            }
        };

        // set handle to server
        server.requestHandler(promotionHandler);
        server.listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
            @Override
            public void handle(AsyncResult<HttpServer> event) {
                logger.info("start promotion 2 verticle completed with port: " + PORT + " and host: " + HOST_ADDRESS);
                if (event.failed()) {
                    event.cause().printStackTrace();
                }
            }
        });
    }

    private void loadConfig(ConnectorHTTPPostPathDb connectorHTTPPostPathDb, final Logger logger, final Handler<JsonObject> callback)
    {
        logger.info("FUNCTION " + "loadConfig PROMOTION SERVER");
        if(connectorHTTPPostPathDb == null)
        {
            connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        }

        connectorHTTPPostPathDb.findOne("promotion_server", new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj pathObj) {
                if(pathObj == null)
                {
                    logger.info("pathObj = null");
                    HOST_ADDRESS = AppConstant.HOST_SERVER;
                    PORT = 0;
                    JsonObject joCallBack = new JsonObject().putString("HOST_ADDRESS", HOST_ADDRESS).putNumber("PORT", PORT);
                    callback.handle(joCallBack);
                }
                else {
                    String portServer = "".equalsIgnoreCase(AppConstant.HOST_SERVER) ? pathObj.path : AppConstant.HOST_SERVER;
                    logger.info("HOST_ADDRESS " + portServer);
                    HOST_ADDRESS = portServer;
                    PORT = pathObj.port;
                    JsonObject joCallBack = new JsonObject().putString("HOST_ADDRESS", HOST_ADDRESS).putNumber("PORT", PORT);
                    callback.handle(joCallBack);
                }
            }
        });
    }

    /**
     *
     * @param code
     * @param request
     */
    private void responseError(int code, String desc, HttpServerRequest request) {
        JsonObject joReply = new JsonObject();
        joReply.putNumber(StringConstUtil.PromotionField.ERROR, code);
        joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, desc);
        request.response().end(joReply.toString());
    }

    //    private void createServer() {
//        JsonObject serverConfig = glbCfg.getObject("promotion_server");
//        final String host = serverConfig.getString("host", "0.0.0.0");
//        final int port = serverConfig.getInteger("port", 7778);
//        HttpServer server = vertx.createHttpServer();
//
//        Handler<HttpServerRequest> promotionHandler = new Handler<HttpServerRequest>() {
//            @Override
//            public void handle(final HttpServerRequest request) {
//                String path = request.path();
//
//                if(path.equalsIgnoreCase("/promotion") && request.method().equalsIgnoreCase("POST")) {
//                    final String cmd = request.params().get(Promo.PromoReq.COMMAND);
//
//                    //params to execute Promotion process
//                    request.bodyHandler(new Handler<Buffer>() {
//                        @Override
//                        public void handle(Buffer buffer) {
////                            List<String> mapParams = new ArrayList<String>();
////                            String[] paramSplits = buffer.toString().split("&");
////                            String valueSplits;
////                            if (paramSplits.length > 0)
////                            {
////                                for (String param : paramSplits)
////                                {
////                                    if (!param.isEmpty())
////                                    {
////                                        mapParams.add(param);
////                                    }
////                                }
////                            }
////
////                            JsonObject jValue = new JsonObject(mapParams.get(0));
////                            Set<String> jKeys = jValue.getFieldNames();
////                            List<String> keys = new ArrayList<>();
////                            keys.addAll(jKeys == null? new ArrayList<String>():jKeys);
////                            Collections.sort(keys);
////                            if(cmd == null) {
////                                promotionProcess.prepareStartProcessPromotion(keys, jValue, request);
////                                return;
////                            }
//
//                            // TODO: 04/07/2016 có thể chuyển tất cả qua promotionProcess.prepareStartProcessPromotion(keys, jValue, request);
//                            // xử lý bên dưới có thể bỏ qua
//
////                            switch (cmd) {
////                                case "getUserInfo":
////                                    promotionProcess.prepareGetUserInfoToCheckPromoProgram(keys, jValue, request);
////                                    break;
////                                case "getUserInfoCallback":
////                                    promotionProcess.prepareGetUserInfoToCheckPromoProgramWithCallback(keys, jValue, request);
////                                    break;
////                                case "executeReferral":
////                                    promotionProcess.prepareExecuteReferralPromotion(keys, jValue, request);
////                                    break;
////                                case "executeSCB":
////                                    promotionProcess.prepareExecuteSCBPromotionProcess(keys, jValue, request);
////                                    break;
////                                case "updateOctober":
////                                    promotionProcess.prepareUpdateOctoberPromoVoucherStatus(keys, jValue, request);
////                                    break;
////                                case "excuteAcquireBinhTan":
////                                    promotionProcess.prepareExcuteAcquireBinhTanUserPromotion(keys, jValue, request);
////                                    break;
////                                default:
////                                    promotionProcess.prepareStartProcessPromotion(keys, jValue, request);
////                                    break;
////                            }
//                        }
//                    });
//
//
//                } else {
//                    responseError(404, request);
//                }
//            }
//        };
//
//        // set handle to server
//        server.requestHandler(promotionHandler);
//        server.listen(port, host, new Handler<AsyncResult<HttpServer>>() {
//            @Override
//            public void handle(AsyncResult<HttpServer> event) {
//                logger.info("start promotion 2 verticle completed with port: " + port + " and host: " + host);
//                if (event.failed()) {
//                    event.cause().printStackTrace();
//                }
//            }
//        });
//    }


//    /**
//     *
//     * @param code
//     * @param request
//     */
//    private void responseError(int code, HttpServerRequest request) {
//        if(code == 400) {
//            responseJson(request.response().setStatusCode(code), new JsonObject()
//                            .putNumber("result", 5)
//                            .putString("description", "Bad Request!")
//                            .putString("Path", request.path())
//            );
//        } else if(code == 404) {
//            responseJson(request.response().setStatusCode(code), new JsonObject()
//                            .putNumber("result", 5)
//                            .putString("description", "Resource not found!")
//                            .putString("Path", request.path())
//            );
//        } else if(code == 401) {
//            responseJson(request.response().setStatusCode(code), new JsonObject()
//                            .putNumber("result", 5)
//                            .putString("description", "Not Authentication!")
//                            .putString("Path", request.path())
//            );
//        } else {
//            responseJson(request.response().setStatusCode(code), new JsonObject()
//                            .putNumber("result", 5)
//                            .putString("description", "Unknow Error")
//                            .putString("Path", request.path())
//            );
//        }
//    }
}
