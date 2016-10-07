package com.mservice.momo.vertx.redis;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
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
 * Created by concu on 7/19/16.
 */
public class BillPayRedisVerticle extends Verticle {


    private Logger logger;
    private JsonObject glbCfg;
    private boolean isStoreApp;

    private HttpClient httpClientRedisOne;
    private HttpClient httpClientRedisTwo;
    private static int PORT_REDIS_1;
    private static int PORT_REDIS_2;
    private String HOST_REDIS_1;
    private String HOST_REDIS_2;
    private String PATH_REDIS_1;
    private String PATH_REDIS_2;

    private JsonObject merchantKeyManageObject;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();

        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        merchantKeyManageObject = glbCfg.getObject(StringConstUtil.MerchantKeyManage.JSON_OBJECT, new JsonObject());

        // jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());
        PORT_REDIS_1 = merchantKeyManageObject.getInteger("portRedis1", 3456);
        PORT_REDIS_2 = merchantKeyManageObject.getInteger("portRedis2", 3456);
        HOST_REDIS_1 = merchantKeyManageObject.getString("hostRedis1", "172.16.43.14");
        HOST_REDIS_2 = merchantKeyManageObject.getString("hostRedis2", "172.16.43.14");
        PATH_REDIS_1 = merchantKeyManageObject.getString("pathRedis1", "/redis/core");
        PATH_REDIS_2 = merchantKeyManageObject.getString("pathRedis2", "/redis/core");

        httpClientRedisOne = vertx.createHttpClient()
                .setHost(HOST_REDIS_1)
                .setPort(PORT_REDIS_1)
                .setMaxPoolSize(20)
                .setConnectTimeout(120000) // 2 phut
                .setKeepAlive(false);

        httpClientRedisTwo = vertx.createHttpClient()
                .setHost(HOST_REDIS_2)
                .setPort(PORT_REDIS_2)
                .setMaxPoolSize(20)
                .setConnectTimeout(120000) // 2 phut
                .setKeepAlive(false);


        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject jsonRequest = message.body();
                billPayToRedis(logger, jsonRequest, message);
            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.BILL_PAY_REDIS_BUS_ADDRESS, myHandler);
    }

    public void billPayToRedis(final Logger logger,final JsonObject jsonRequest,final Message<JsonObject> message){

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        final JsonObject tranReply = new JsonObject();
        long amountObj = jsonRequest.getObject("extras", new JsonObject()).getLong("amount", 0);
//        if(amountObj == 0){
//            amountObj = jsonRequest.getObject("extras", new JsonObject()).getLong("amounttran", 0);
//        }
        final long amount = amountObj;
        final String fromNumber = jsonRequest.getString("initiator", "");
        String toAgent = jsonRequest.getObject("extras").getString("target", "");
        if(Misc.isNullOrEmpty(toAgent)){
            toAgent = jsonRequest.getObject("extras").getString("specialagent", "");
        }
        final String pin = jsonRequest.getString("pass", "");
        final String billId = jsonRequest.getObject("extras").getString("account", "");
        if(amount == 0 || Misc.isNullOrEmpty(fromNumber) || Misc.isNullOrEmpty(toAgent) || Misc.isNullOrEmpty(pin)){
            tranReply.putNumber(colName.TranDBCols.ERROR, 1020);
            tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
            tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
            message.reply(tranReply);
            return;
        }
        if(System.currentTimeMillis() % 2 == 0){ //chia tải 2 server redis
            logger.info("billPayToRedis[1] host " + HOST_REDIS_1 + " port " + PORT_REDIS_1 + " path " + PATH_REDIS_1);
            HttpClientRequest requestOne = httpClientRedisOne.post(PATH_REDIS_1, new Handler<HttpClientResponse>() {
                @Override
                public void handle(final HttpClientResponse responseOne) {
                    int statusCode = responseOne.statusCode();
                    if (statusCode != 200) {

                        //411: length required
                        //401: unauthorized
                        logger.info("error Redis[1] " + statusCode);
                        logger.info("desc Redis[1] " + responseOne.statusMessage());
                        return;
                    }

                    responseOne.bodyHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer buffer) {
                            try {
                                logger.info("Respone Redis[1] " + buffer.toString());
                                //JsonObject result = new JsonObject(buffer.toString());
                                if(!Misc.isNullOrEmpty(buffer.toString())){
                                    JsonObject jsonResponse = new JsonObject(buffer.toString());
                                    int error = jsonResponse.getInteger("status", 1006);
                                    long tranId = jsonResponse.getLong("tid", -1);
                                    tranReply.putNumber(colName.TranDBCols.TRAN_ID, tranId);
                                    tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                    tranReply.putNumber(colName.TranDBCols.ERROR, error);
                                    if (error == 0) {
                                        logger.info("Redis[1] success");
                                        Misc.addCustomNumber(tranReply, fromNumber);

                                        message.reply(tranReply);
                                        log.writeLog();
                                    }
                                    else {
                                        logger.info("Redis[1] fail");
                                        Misc.addCustomNumber(tranReply, fromNumber);
                                        message.reply(tranReply);
                                        log.writeLog();
                                    }
                                }else{
                                    logger.info("Redis[1] null" );
                                    tranReply.putNumber(colName.TranDBCols.ERROR, 1008);
                                    tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                    tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                    message.reply(tranReply);
                                    log.writeLog();
                                    return;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.info("Redis[1] error exception " + e.getMessage());
                                tranReply.putNumber(colName.TranDBCols.ERROR, 1006);
                                tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                message.reply(tranReply);
                                log.writeLog();
                                return;
                            }

                        }
                    });
                }
            });

            Buffer bufferRequest = new Buffer(jsonRequest.toString());
            requestOne.putHeader("content-length", String.valueOf(bufferRequest.length()));
            requestOne.putHeader("content-Type", "application/json");
            logger.info("Request Redis[1] " + bufferRequest.toString());
            requestOne.end(bufferRequest);
        }else{
            logger.info("billPayToRedis[2] host " + HOST_REDIS_2 + " port " + PORT_REDIS_2 + " path " + PATH_REDIS_2);
            HttpClientRequest requestTwo = httpClientRedisTwo.post(PATH_REDIS_2, new Handler<HttpClientResponse>() {
                @Override
                public void handle(final HttpClientResponse responseTwo) {
                    int statusCode = responseTwo.statusCode();
                    if (statusCode != 200) {

                        //411: length required
                        //401: unauthorized
                        logger.info("error Redis[2] " + statusCode);
                        logger.info("desc Redis[2] " + responseTwo.statusMessage());
                        return;
                    }

                    responseTwo.bodyHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer buffer) {
                            try {
                                logger.info("Respone Redis[2] " + buffer.toString());
                                //JsonObject result = new JsonObject(buffer.toString());
                                if(!Misc.isNullOrEmpty(buffer.toString())){
                                    JsonObject jsonResponse = new JsonObject(buffer.toString());
                                    int error = jsonResponse.getInteger("status", 1006);
                                    long tranId = jsonResponse.getLong("tid", -1);
                                    tranReply.putNumber(colName.TranDBCols.TRAN_ID, tranId);
                                    tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                    tranReply.putNumber(colName.TranDBCols.ERROR, error);
                                    if (error == 0) {
                                        logger.info("Redis[2] success");
                                        Misc.addCustomNumber(tranReply, fromNumber);
                                        message.reply(tranReply);
                                        log.writeLog();
                                    }
                                    else {
                                        Misc.addCustomNumber(tranReply, fromNumber);
                                        message.reply(tranReply);
                                        log.writeLog();
                                    }
                                }else{
                                    logger.info("Redis[2] null" );
                                    tranReply.putNumber(colName.TranDBCols.ERROR, 1008);
                                    tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                    tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                    message.reply(tranReply);
                                    log.writeLog();
                                    return;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.info("Redis[2] error exception " + e.getMessage());
                                tranReply.putNumber(colName.TranDBCols.ERROR, 1006);
                                tranReply.putNumber(colName.TranDBCols.TRAN_ID, -1);
                                tranReply.putNumber(colName.TranDBCols.AMOUNT, amount);
                                message.reply(tranReply);
                                log.writeLog();
                                return;
                            }

                        }
                    });
                }
            });

            Buffer bufferRequest = new Buffer(jsonRequest.toString());
            requestTwo.putHeader("content-length", String.valueOf(bufferRequest.length()));
            requestTwo.putHeader("content-Type", "application/json");
            logger.info("Request Redis[2] " + bufferRequest.toString());
            requestTwo.end(bufferRequest);
        }
    }
}
