package com.mservice.momo.vertx;

import banknetvn.md5.checkMD5;
import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.gateway.internal.db.oracle.HTPPOracleVerticle;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStore;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStoreNearestRequest;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.util.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by duyhv on 12/7/2015.
 */
public class DGDWSVerticle extends Verticle {

    private static int PORT = 16996;
    private static String HOST_ADDRESS = "0.0.0.0";
    private static String MD5_KEY;
    private static String SOURCE;
    private static checkMD5 md5 = new checkMD5();
    private AgentsDb agentDb;

    private static boolean validate(MStoreNearestRequest request) {
//        String calc = "";
//        try {
//            String s = request.searchValue + request.pageNum + request.pageSize + request.time + (long) request.lat + (long) request.lon + MD5_KEY;
//            calc = md5.getMD5Hash(s);
//            System.out.println("MD5 hash " + s + " = " + calc);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//        if (!calc.equalsIgnoreCase(request.checkSum)) {
//            return false;
//        }
        return true;
    }

    private static boolean validateByCode(JsonObject jo) {
        String calc = "";
        try {
            String s = "" + jo.getInteger("CId") + jo.getInteger("DId")+ jo.getInteger("pageNum") + jo.getInteger("pageSize") + jo.getLong("time") + MD5_KEY;
            calc = md5.getMD5Hash(s);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (!calc.equalsIgnoreCase(jo.getString("checkSum"))) {
            return false;
        }
        return true;
    }

    private static boolean checkTimeout(MStoreNearestRequest request) {
        /*long now = System.currentTimeMillis();
        if (!((now - 2 * 60 * 1000) <= request.time &&  request.time <= (now + 2 * 60 * 1000))) {
            System.out.println("request timeout. request time: " + request.time + ". current time: " + now);
            return false;
        }*/
        return true;
    }

    private void doUpdateNormalize() {
        this.agentDb.updateNormalize(new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray arr) {
                Queue<JsonObject> queueAgents = new ArrayDeque<>();
                if (arr != null && arr.size() > 0) {
                    for (int i = 0; i < arr.size(); i++) {
                        queueAgents.add((JsonObject) arr.get(i));
                    }
                }
                container.logger().debug("Need update " + queueAgents.size() + " to normalize");
                upSertMMTAgent(queueAgents);
            }
        });
    }

    private void upSertMMTAgent(final Queue<JsonObject> queueAgents) {
        if (queueAgents == null || queueAgents.size() == 0) {
            container.logger().debug("Update Stores to normalize is done");
            return;
        }
        final JsonObject item = queueAgents.poll();
        if (item != null) {
            this.agentDb.upsertLocation(item, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    upSertMMTAgent(queueAgents);
                }
            });
        }
    }

    @Override
    public void start() {
        final Logger logger = container.logger();
        this.agentDb = new AgentsDb(vertx.eventBus(), container.logger());
        final JsonObject globalConfig = container.config();
        JsonObject webServiceVerticleConfig = globalConfig.getObject("DGDVerticle", new JsonObject());
        this.PORT = webServiceVerticleConfig.getInteger("port", 8083);
        this.HOST_ADDRESS = webServiceVerticleConfig.getString("hostAddress", "0.0.0.0");
        this.MD5_KEY = webServiceVerticleConfig.getString("key", "Z8wh%Rj(DP");
        this.SOURCE = webServiceVerticleConfig.getString("source", "mongo");

        //if (!container.config().getBoolean("storeApp")) {
            doUpdateNormalize();
        //}

        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest httpRequest) {
                        logger.info("[DGDWSVerticle] [" + httpRequest.method() + "] " + " uri: " + httpRequest.uri() + " path: " + httpRequest.path());
                        final String path = httpRequest.path();
                        final JsonObject reply = new JsonObject();

                        if (httpRequest.method().equalsIgnoreCase("POST")) {
                            if ("/dgd/nearest".equalsIgnoreCase(path) || "/dgd/search".equalsIgnoreCase(path)) {
                                httpRequest.dataHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(final Buffer buffer) {
                                        logger.info("DGDWSVerticle: Search store, request=" + buffer.toString());
                                        final MStoreNearestRequest dataReq = JSONUtil.fromStrToObj(buffer.toString(), MStoreNearestRequest.class);
                                        if (dataReq == null) {
                                            logger.error("DGDWSVerticle: invalid request");
                                        }
                                        if (dataReq == null
                                                || !StringUtils.isNotEmpty(dataReq.checkSum)
                                                || ("/dgd/nearest".equalsIgnoreCase(path) && dataReq.lat == 0 && dataReq.lon == 0)
                                                || !validate(dataReq)) {
                                            MStoreNearestRequest failRes = new MStoreNearestRequest();
                                            failRes.resultCode = 10000;
                                            failRes.resultDesc = "invalid params";
                                            logger.info("DGDWSVerticle: response: invalid params");
                                            httpRequest.response().end(JSONUtil.fromObjToStr(failRes));
                                            return;
                                        }

                                        if (!checkTimeout(dataReq)) {
                                            MStoreNearestRequest failRes = new MStoreNearestRequest();
                                            failRes.resultCode = -3;
                                            failRes.resultDesc = "request timeout. Please check paramns that the time field is over 2 minutes";
                                            logger.info("DGDWSVerticle: response: " + failRes.resultDesc);
                                            httpRequest.response().end(JSONUtil.fromObjToStr(failRes));
                                            return;
                                        }
                                        if (dataReq.searchValue.matches("[^a-zA-Z0-9]+")) {
                                            MStoreNearestRequest failRes = new MStoreNearestRequest();
                                            failRes.resultCode = -1;
                                            failRes.resultDesc = "Not found";
                                            logger.info("DGDWSVerticle: response: " + failRes.resultDesc);
                                            httpRequest.response().end(JSONUtil.fromObjToStr(failRes));
                                            return;
                                        }

                                        if ("mis".equalsIgnoreCase(SOURCE)) {
                                            JsonObject json = new JsonObject();
                                            if ("/dgd/nearest".equalsIgnoreCase(path))
                                                json.putNumber(LStandbyOracleVerticle.COMMAND, HTPPOracleVerticle.SEARCH_STORE_NEAREST);
                                            else
                                                json.putNumber(LStandbyOracleVerticle.COMMAND, HTPPOracleVerticle.SEARCH_STORE);
                                            json.putObject("nearest", JSONUtil.fromObjToJsonObj(dataReq));

                                            vertx.eventBus().send(AppConstant.HTPPOracleVerticle_ADDRESS, json, new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> result) {

                                                    httpRequest.response().putHeader("Content-Type", "application/json");
                                                    httpRequest.response().putHeader("Access-Control-Allow-Origin", "*");
//                                                    logger.info("DGDWSVerticle: response: " + result.body().toString());
                                                    logger.info("DGDWSVerticle: response: ...");
                                                    httpRequest.response().end(result.body().toString());
                                                }
                                            });
                                        } else {
                                            agentDb.getStoresNew(dataReq.lat, dataReq.lon, dataReq.searchValue, dataReq.pageNum, dataReq.pageSize, new Handler<List<AgentsDb.StoreInfo>>() {
                                                @Override
                                                public void handle(List<AgentsDb.StoreInfo> storeInfos) {
                                                    httpRequest.response().putHeader("Content-Type", "application/json;charset=utf-8");
                                                    httpRequest.response().putHeader("Access-Control-Allow-Origin", "*");

                                                    if (storeInfos == null || storeInfos.size() == 0) {
                                                        dataReq.resultCode = -1;
                                                        dataReq.resultDesc = "Not found";
                                                        logger.info("DGDWSVerticle: response: " + dataReq.resultDesc);
                                                        httpRequest.response().end(JSONUtil.fromObjToStr(dataReq));
                                                        return;
                                                    } else {
                                                        dataReq.resultCode = 0;
                                                        dataReq.resultData = MStore.fromList(storeInfos);
                                                        for (MStore s : dataReq.resultData) {
                                                            s.DISTANCE = MStore.distFrom(dataReq.lat, dataReq.lon, s.LATITUDE, s.LONGITUDE);
                                                        }
                                                        String jsonRes = JSONUtil.fromObjToStr(dataReq);
                                                        logger.info("DGDWSVerticle: response: ...");
                                                        httpRequest.response().end(jsonRes);
                                                        return;
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            } else if ("/dgd/code".equalsIgnoreCase(path)) {
                                httpRequest.dataHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(final Buffer buffer) {
                                        final HttpServerResponse response = httpRequest.response();
                                        response.putHeader("Content-Type", "application/json;charset=utf-8");
                                        response.putHeader("Access-Control-Allow-Origin", "*");
                                        try {

                                            logger.info("DGDWSVerticle: Search store by code, request=" + buffer.toString());
                                            final JsonObject dataReq = new JsonObject(buffer.toString());
                                            if (dataReq == null || !StringUtils.isNotEmpty(dataReq.getString("checkSum"))
                                                    || !validateByCode(dataReq)) {
                                                JsonObject failRes = new JsonObject();
                                                failRes.putNumber("resultCode", 10000);
                                                failRes.putString("resultDesc", "invalid params");
                                                logger.info("DGDWSVerticle: Search store by code: response: invalid params");
                                                response.end(failRes.encode());
                                                return;
                                            }
                                            agentDb.countStoreByCode(dataReq.getInteger("CId"), dataReq.getInteger("DId"),
                                                    dataReq.getInteger("pageSize"), dataReq.getInteger("pageNum"), new Handler<Long>() {
                                                        @Override
                                                        public void handle(Long size) {
                                                            long count = size / dataReq.getInteger("pageSize");

                                                            if (size % dataReq.getInteger("pageSize") > 0)
                                                                count++;
                                                            final long pageCount = count;


                                                            agentDb.getStores(dataReq.getInteger("CId"), dataReq.getInteger("DId"),
                                                                    dataReq.getInteger("pageSize"), dataReq.getInteger("pageNum"), new Handler<List<AgentsDb.StoreInfo>>() {
                                                                        @Override
                                                                        public void handle(List<AgentsDb.StoreInfo> storeInfos) {

                                                                            if (storeInfos == null || storeInfos.size() == 0) {
                                                                                JsonObject failRes = new JsonObject();
                                                                                failRes.putNumber("resultCode", -1);
                                                                                failRes.putString("resultDesc", "Not found");
                                                                                response.end(failRes.encode());
                                                                                return;
                                                                            } else {
                                                                                JsonObject ok = new JsonObject();
                                                                                ok.putNumber("resultCode", 0);
                                                                                ok.putNumber("pageCount", pageCount);
                                                                                ok.putArray("resultData", new JsonArray(JSONUtil.fromObjToStr(storeInfos)));
                                                                                response.end(ok.encode());
                                                                                return;
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    });
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            JsonObject failRes = new JsonObject();
                                            failRes.putNumber("resultCode", -2);
                                            failRes.putString("resultDesc", "system error");
                                            logger.info("DGDWSVerticle: Search store by code: response: system error");
                                            response.end(failRes.encode());
                                            return;
                                        }
                                    }
                                });
                            } else {
                                logger.info("DGDWSVerticle: response: url not found");
                                httpRequest.response().setStatusCode(404).end();
                                return;
                            }
                        }
                    }
                }).listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
            @Override
            public void handle(AsyncResult<HttpServer> event) {
                if (event.succeeded()) {
                    logger.info("DGDWSVerticle's listening on " + HOST_ADDRESS + ":" + PORT);
                }
            }
        });
    }
}
