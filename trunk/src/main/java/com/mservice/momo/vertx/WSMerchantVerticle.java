package com.mservice.momo.vertx;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.MerchantOfflinePayment.MerchantKeyManageDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.permission.Cryption;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.*;
import com.mservice.momo.vertx.processor.transferbranch.PayOneBill;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.platform.Verticle;

/**
 * Created by tumegame on 07/01/2016.
 */
public class WSMerchantVerticle extends Verticle {
    private static int PORT = 8084;
    private static String HOST_ADDRESS = "1.1.1.1";
    private static int POS_TIMEOUT = 180000;
    private static String PUBLIC_KEY;
    private static String PRIVATE_KEY;
    private PhonesDb phonesDb;
    private MerchantKeyManageDb merchantKeyManageDb;
    private JsonObject merchantWebObject;
    private boolean isTestEnvironment;
    private TransferProcess transferProcess;
    private TransDb transDb;
    private TransferCommon transferCommon;
    private String staticResourceDir;
    private ConnectProcess connectProcess;
    private TransProcess transProcess;
    private PayOneBill payBillFactory;

    @Override
    public void start() {
        final Logger logger = container.logger();
        final JsonObject globalConfig = container.config();
        merchantWebObject = globalConfig.getObject(StringConstUtil.MerchantWeb.JSON_OBJECT, new JsonObject());
        transferCommon = new TransferCommon(vertx, logger, globalConfig);
        this.PORT = merchantWebObject.getInteger(StringConstUtil.MerchantWeb.PORT, 8084);
        this.HOST_ADDRESS = merchantWebObject.getString(StringConstUtil.MerchantWeb.HOST, "1.1.1.1");
        this.POS_TIMEOUT = merchantWebObject.getInteger(StringConstUtil.MerchantWeb.TIMEOUT, 180000);
        this.PUBLIC_KEY = merchantWebObject.getString(StringConstUtil.MerchantWeb.PUBLIC_KEY, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkhIycpu9lATGF6zpZjRzpJfGjKqXi+fn\nPfkbJay++VkMuR2Ny8sH1By91OQJupMbOixboqMkQh3mU+3ufNKwoOLC7BZG8ebiJWD1iyz/d2k8\nRFAWdi1kfTfCT9aFCWq3a/dkshM/nO+dyZL+7D+SqrpUkbfgd11tvSOmxtuIMGzto6LE1P64RlYk\n9kH02n42/c6DnloLd303gCcglzSnVwim6rpDEEb6YIk76WQFxefZ+KelXZbFz/7QIcVydT/xk8uV\nkwSy2QPWBTVEIWNd3Mc03u8tgWRWUqA72GLjLr2MovrkoHlCAFHxYJU03YpW7jyntHvJX9O48c24\nM5RHKwIDAQAB");
        this.PRIVATE_KEY = merchantWebObject.getString(StringConstUtil.MerchantWeb.PRIVATE_KEY, "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCSEjJym72UBMYXrOlmNHOkl8aM\nqpeL5+c9+RslrL75WQy5HY3LywfUHL3U5Am6kxs6LFuioyRCHeZT7e580rCg4sLsFkbx5uIlYPWL\nLP93aTxEUBZ2LWR9N8JP1oUJardr92SyEz+c753Jkv7sP5KqulSRt+B3XW29I6bG24gwbO2josTU\n/rhGViT2QfTafjb9zoOeWgt3fTeAJyCXNKdXCKbqukMQRvpgiTvpZAXF59n4p6VdlsXP/tAhxXJ1\nP/GTy5WTBLLZA9YFNUQhY13cxzTe7y2BZFZSoDvYYuMuvYyi+uSgeUIAUfFglTTdilbuPKe0e8lf\n07jxzbgzlEcrAgMBAAECggEACKgjbT9loNwXkVeiDXiWUe7cyYFisdvwjG+y+CygtM5ePqpNuQIz\nWJLLfU52dSQ6vNvcImtgvrpe6CJ1u9gQt7g4rR003yk7xdNVOgZUrZMDC1lju2U9S15+mZSxFbnJ\nXRCwWw1g+8AHreaUTRQHcS7RzEEFgI1873SpcaeZDqWOfJ5DMKvjvMG6eSkq4IHVE8c63tyogLMP\nbiGsEnDxlGpIp/iO0m/Eg0eTDrST4OkxRAzFhiA7Us8Mw1b7elsxdrwr4Ra+y9U0nxbiD06Bg4q0\n1tpAfpZnxbPrPwt+f9Uza4YnGoyqCf6FTWStR2aFLeuimquxFVVRZk5g2PZHAQKBgQDymJejiI8/\nKuSGhBh9sPUBrQbvT5IO4oLzQ4DCojOSt+GJ1jrflTr1QiFwfu4N++mNkPfWNNa2TK6bfiHBIjP4\nqSWUjmhYeCHlzKIcmTbKY0oHyvY1myuld+BTewsAQtxa+jJht4MCOR7vnyZJrOQUKnYwKYfXRRgQ\nbfvnld/PywKBgQCaJE3YVsc9ewz0/rV1eFG8/KgH6QuFlpaZlF8JlTvfRpOqPt1TduvsqZrJrwhi\nd6pQzhxT2Hx4xVT6vkjQFS13mmRykPYACquY71BgVcfPL+rT3O4zsQmbOnkwJQGSjcckbeL3ZSeD\nGKD+VXjC2N5hDhHVxqYvut8R/3vsFCO6IQKBgQCJZvHkFtGDZojew2yXrCVo2JZn7rp8IcEnhSEl\nm/b375wXlLXtsrkc9mK4M7wjQX4Lx4MH2Q+PWyk+OpdlqziiazM0y9+/0/LnFBrxdbn5sXjZpxQC\nUqvK4XW18qfNcxEZmkH05JqYJMAn2g02h2z7Gv2r06nzvFef3pthlJqaNwKBgHecxGukq2eiHSPg\no59MhuFIjnvU9APuH19+K2GcVKGmeuAZeFZUai90TZFEKhV/FsMtrf3CeJSfTJpnHsmaJXYycNAW\nfU76+L3st0qKRksYu/k77/xc3T9/2JrrnJTFQEucmguwi0DH0+aJHPTWpXpbGKvzQvK26HNm9hr0\nZeUBAoGBAMeIX4UWRHIgsUPpnPBbJ7N/OGIqcMpncmCmKfPaC9yhyzunLr98z+MDH+rBMm448PMr\nMWC3+k5bPgt0B0WHlTq3ZfcS48blVNYSQUx0FEQznOMOM3GmDA0yFZYbtW+pWtIXTdrjvfIfGcwu\nhaPlQ0KqH7JGSG7G1L6UtIaZ35UY");

        staticResourceDir = globalConfig.getObject("userResourceVerticle").getString("staticResourceDir", "/tmp");
        if (!staticResourceDir.endsWith("/"))
            staticResourceDir = staticResourceDir + "/";

        phonesDb = new PhonesDb(vertx.eventBus(), container.logger());
        merchantKeyManageDb = new MerchantKeyManageDb(vertx, container.logger());
        connectProcess = new ConnectProcess(vertx, logger, globalConfig);
        transferProcess = new TransferProcess(vertx, logger, globalConfig, ServerVerticle.MapTranRunning);
        transProcess = new TransProcess(vertx, logger, globalConfig);
        payBillFactory = new PayOneBill(vertx, logger, globalConfig);
        final ConcurrentSharedMap<String, JsonObject> sessions = vertx.sharedData().getMap(AppConstant.WebAdminVerticle_WEB_ADMIN_SESSION_MAP);

        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        connectProcess = new ConnectProcess(vertx, container.logger(), globalConfig);
        isTestEnvironment = merchantWebObject.getBoolean(StringConstUtil.MerchantWeb.IS_TEST_ENVIRONMENT, false);
        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest request) {
                        logger.info("[WebServiceMerchant] [" + request.method() + "] " + " uri: " + request.uri() + " path: " + request.path());
                        final JsonObject joReply = new JsonObject();
                        final String path = request.path();
                        final Cryption cryption = new Cryption(globalConfig, logger);
                        for (int i = 0; i < request.headers().size(); i++) {
                            logger.info("key " + request.headers().entries().get(i).getKey() + " value " + request.headers().entries().get(i).getValue());
                        }
                        if (request.method().equalsIgnoreCase("POST")) {
                            // test webservice
                            if (path.equalsIgnoreCase("/hello") || "/hello".equalsIgnoreCase(request.uri())) {
                                request.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        logger.info("hello" + buffer.getBytes());
                                        logger.info("hello" + buffer.toString());
                                        response(request,"{\"Hello success\":\"" + buffer.toString() + "\"}");
                                        return;
                                    }
                                });
                            }
                            //create public key and private key
                            else if (path.equalsIgnoreCase("/createkey") || "/createkey".equalsIgnoreCase(request.uri())) {
                                request.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        logger.info("createkey" + buffer.getBytes());
                                        logger.info("createkey" + buffer.toString());
                                        JsonObject joReceive = new JsonObject(buffer.toString());
                                        final int keySize = joReceive.getInteger(StringConstUtil.MerchantWeb.KEY_SIZE, 2048);
                                        String result = cryption.initKeyPair(keySize);
                                        response(request,result);
                                        return;
                                    }
                                });
                            }
                            //check public key and private key
                            else if (path.equalsIgnoreCase("/checkkey") || "/checkkey".equalsIgnoreCase(request.uri())) {
                                request.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        try {
                                            logger.info("checkkey=" + buffer.getBytes());
                                            logger.info("checkkey=" + buffer.toString());
                                            JsonObject joReceive = new JsonObject(buffer.toString());
                                            final String publicKey = joReceive.getString(StringConstUtil.MerchantWeb.PUBLIC_KEY, "");
                                            //String testString = "abcxyz";
                                            //byte[] testByte = testString.getBytes();
                                            //String hash = cryption.encrypt(testByte,publicKey);
                                            //logger.info("hash=" + hash);
                                            final String hash = joReceive.getString(StringConstUtil.MerchantWeb.HASH, "");
                                            boolean result = cryption.resultVerifyData(hash, publicKey, PRIVATE_KEY);
                                            logger.info("result=" + result);
                                            final String dataDecrypt = cryption.decrypt(hash.trim(), PRIVATE_KEY.trim());
                                            logger.info("dataDecrypt=" + dataDecrypt);
                                            response(request,"{\"result\": " + result + ", \"dataDecrypt\":\"" + dataDecrypt + "\"}");

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            response(request, "{\"result\": \"false\"}");
                                            return;
                                        }
                                    }
                                });
                            }
                            // user login
                            else if (path.equalsIgnoreCase("/login") || "/login".equalsIgnoreCase(request.uri())) {
                                logger.info("login");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                login(request, log, logger, cryption);
                            }
                            // create new merchant
                            else if (path.equalsIgnoreCase("/createmerchant") || "/createmerchant".equalsIgnoreCase(request.uri())) {
                                logger.info("createMerchant");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                createMerchant(request, log, logger, cryption);
                                // create new merchant
                            } else if (path.equalsIgnoreCase("/modifymerchant") || "/modifymerchant".equalsIgnoreCase(request.uri())) {
                                logger.info("modifyMerchant");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                modifyMerchant(request, log);
                                // topup phone
                            } else if (path.equalsIgnoreCase("/topup") || "/topup".equalsIgnoreCase(request.uri())) {
                                logger.info("topup");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                topup(request, log, logger, cryption);
                            } else if (path.equalsIgnoreCase("/checkinfo") || "/checkinfo".equalsIgnoreCase(request.uri())) {
                                logger.info("checkinfo");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                payBillCheckInfo(request, log, logger, cryption);
                            } else if (path.equalsIgnoreCase("/paybill") || "/paybill".equalsIgnoreCase(request.uri())) {
                                logger.info("paybill");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                payBill(request, log, logger, cryption);
                            } else if (path.equalsIgnoreCase("/m2m") || "/m2m".equalsIgnoreCase(request.uri())) {
                                logger.info("m2m");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                m2m(request, log, logger, cryption);
                            } else if (path.equalsIgnoreCase("/checkagent") || "/checkagent".equalsIgnoreCase(request.uri())) {
                                logger.info("checkagent");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                checkagent(request, log, logger, cryption);
                            }
                            else {
                                logger.info("Wrong path or path not exist");
                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Wrong path or path not exist");
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1008);
                                response(request,joReply.toString());
                                return;
                            }
                        } else {
                            response(request, "not support not POST menthod");
                        }
                    }
                }).listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
            @Override
            public void handle(AsyncResult<HttpServer> event) {
                if (event.succeeded()) {
                    logger.info("WSMerchantVerticle's listening on " + HOST_ADDRESS + ":" + PORT);
                }
            }
        });
    }


    /* login user, param request:
     customerNumber 0982123456
     pin DSFKSFSDF453DFSPOF
     */
    private void login(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());
                    log.setPhoneNumber("excutePos");

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String customerNumber = joReceive.getString(StringConstUtil.MerchantWeb.CUSTOMER_NUMBER, "");
                    final String pinEncrypt = joReceive.getString(StringConstUtil.MerchantWeb.PIN, "");

                    if ("".equalsIgnoreCase(pinEncrypt) || "".equalsIgnoreCase(customerNumber) || pinEncrypt.length() <= 6) {
                        log.add("desc", "Wrong request");
                        joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                        joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1020);
                        log.writeLog();
                        response(request, joReply);
                        return;
                    } else {

                        final String pin = cryption.decrypt(pinEncrypt.trim(), PRIVATE_KEY.trim());
                        final int phoneNumber = DataUtil.strToInt(customerNumber);
                        log.add("phoneNumber", customerNumber);

                        phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(final PhonesDb.Obj phoneObj) {

                                if (phoneObj == null) {
                                    log.add("desc", "Agent not found");
                                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Agent not found");
                                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 3);
                                    log.writeLog();
                                    response(request, joReply);
                                    return;
                                }

                                final SockData sockData = new SockData(vertx, logger, container.config());
                                sockData.setPhoneObj(phoneObj, logger, "Merchant web set phone object at begin request login");
                                sockData.isSetup = true;

                                MomoMessage msg = new MomoMessage(MomoProto.MsgType.LOGIN_VALUE
                                        , System.currentTimeMillis()
                                        , phoneNumber,
                                        MomoProto.LogIn.newBuilder()
                                                .setMpin(pin)
                                                .setDeviceModel("")
                                                .build().toByteArray()
                                );

                                connectProcess.processLogIn(null, msg, sockData, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject loginResult) {
                                        int error = loginResult.getInteger("error", 100);
                                        log.add("status", error);

                                        if (error == 0) {
                                            log.add("desc", "Agent not found");
                                            joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, SoapError.CoreErrorMap_VN.get(0));
                                            joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 0);
                                            log.writeLog();
                                            response(request, joReply);
                                            return;
                                        } else {
                                            log.add("desc", "Agent not found");
                                            joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, SoapError.CoreErrorMap_VN.get(error));
                                            joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, error);
                                            log.writeLog();
                                            response(request, joReply);
                                            return;
                                        }

                                    }
                                });
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.add("desc", "Exception " + e.getMessage());
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    log.writeLog();
                    response(request, joReply);
                    return;
                }
            }
        });
    }

    /* create new merchant, param request:
     merchantId WEB001
     agentName 0982123456
     serviceId abc
     */
    private void createMerchant(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                log.add("data get byte ", buffer.getBytes());
                log.add("data to string ", buffer.toString());

                JsonObject joReceive = new JsonObject(buffer.toString());
                final String merchantId = joReceive.getString(StringConstUtil.MerchantWeb.MERCHANT_ID, "");
                final String merchantName = joReceive.getString(StringConstUtil.MerchantWeb.MERCHANT_NAME, "");
                final String merchantNumber = joReceive.getString(StringConstUtil.MerchantWeb.MERCHANT_NUMBER, "");
                final String agentName = joReceive.getString(StringConstUtil.MerchantWeb.AGENT_NAME, "");
                final String serviceId = joReceive.getString(StringConstUtil.MerchantWeb.SERVICE_ID, "");

                log.setPhoneNumber(agentName);

                    if ("".equalsIgnoreCase(merchantId) || "".equalsIgnoreCase(agentName) || "".equalsIgnoreCase(serviceId)) {
                    log.add("desc", "Request miss param");
                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Request miss param");
                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        log.writeLog();
                        response(request, joReply);
                    return;
                } else {
                        JsonObject merchantObj = new JsonObject();
                        merchantObj.putString(colName.MerchantKeyManageCols.MERCHANT_ID, merchantId);
                        merchantObj.putString(colName.MerchantKeyManageCols.MERCHANT_NAME, merchantName);
                        merchantObj.putString(colName.MerchantKeyManageCols.MERCHANT_NUMBER, merchantNumber);
                        merchantObj.putString(colName.MerchantKeyManageCols.DEV_PUBLIC_KEY, "");
                        merchantObj.putString(colName.MerchantKeyManageCols.DEV_PRIVATE_KEY, "");
                        merchantObj.putString(colName.MerchantKeyManageCols.PRODUCT_PUBLIC_KEY, "");
                        merchantObj.putString(colName.MerchantKeyManageCols.PRODUCT_PRIVATE_KEY, "");
                        merchantObj.putString(colName.MerchantKeyManageCols.MERCHANT_INFOS, "0");
                        merchantObj.putString(colName.MerchantKeyManageCols.AGENT_NAME, agentName);
                        merchantObj.putString(colName.MerchantKeyManageCols.SERVICE_ID, serviceId);
                        MerchantKeyManageDb.Obj joInsert = new MerchantKeyManageDb.Obj(merchantObj);
                    log.add("merchantId", merchantId);
                    log.add("merchantName", merchantName);
                    log.add("merchantNumber", merchantNumber);
                    log.add("agentName", agentName);
                    log.add("serviceId", serviceId);

                    merchantKeyManageDb.insert(joInsert, new Handler<Integer>() {
                        @Override
                        public void handle(final Integer reply) {

                            if (reply == 0) {
                                joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Create merchant success");
                                joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 0);
                                log.writeLog();
                                response(request, joReply);
                            } else if (reply == 11000) {
                                joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "merchantId exist");
                                joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, reply);
                                log.writeLog();
                                response(request, joReply);
                            } else {
                                joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Create merchant fail");
                                joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, reply);
                                log.writeLog();
                                response(request, joReply);
                            }
                        }
                    });

                }
                } catch (Exception e) {
                    e.printStackTrace();
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    log.writeLog();
                    response(request, joReply);
                }
            }
        });
    }

    /* modify merchant, param request:
     merchantId WEB001
     */
    private void modifyMerchant(final HttpServerRequest request, final Common.BuildLog log) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String merchantId = joReceive.getString(StringConstUtil.MerchantWeb.MERCHANT_ID, "");
                    final String merchantName = joReceive.getString(StringConstUtil.MerchantWeb.MERCHANT_NAME, "");
                    final String merchantNumber = joReceive.getString(StringConstUtil.MerchantWeb.MERCHANT_NUMBER, "");
                    final String devPublicKey = joReceive.getString("devPublicKey", "");
                    final String devPrivateKey = joReceive.getString("devPrivateKey", "");
                    final String proPublicKey = joReceive.getString("proPublicKey", "");
                    final String proPrivateKey = joReceive.getString("proPrivateKey", "");
                    final String merchantInfo = joReceive.getString(StringConstUtil.MerchantWeb.MERCHANT_NUMBER, "");
                    final String agentName = joReceive.getString(StringConstUtil.MerchantWeb.AGENT_NAME, "");
                    final String serviceId = joReceive.getString(StringConstUtil.MerchantWeb.SERVICE_ID, "");

                    log.setPhoneNumber(agentName);

                    if ("".equalsIgnoreCase(merchantId)) {
                        log.add("desc", "Request miss param");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Request miss param");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        log.writeLog();
                        response(request, joReply);
                        return;
                    } else {
                        JsonObject merchantObj = new JsonObject();
                        merchantObj.putString(colName.MerchantKeyManageCols.MERCHANT_NAME, merchantName);
                        merchantObj.putString(colName.MerchantKeyManageCols.MERCHANT_NUMBER, merchantNumber);
                        merchantObj.putString(colName.MerchantKeyManageCols.DEV_PUBLIC_KEY, devPublicKey);
                        merchantObj.putString(colName.MerchantKeyManageCols.DEV_PRIVATE_KEY, devPrivateKey);
                        merchantObj.putString(colName.MerchantKeyManageCols.PRODUCT_PUBLIC_KEY, proPublicKey);
                        merchantObj.putString(colName.MerchantKeyManageCols.PRODUCT_PRIVATE_KEY, proPrivateKey);
                        merchantObj.putString(colName.MerchantKeyManageCols.MERCHANT_INFOS, merchantInfo);
                        merchantObj.putString(colName.MerchantKeyManageCols.AGENT_NAME, agentName);
                        merchantObj.putString(colName.MerchantKeyManageCols.SERVICE_ID, serviceId);
                        log.add("merchantId", merchantId);
                        log.add("merchantName", merchantName);
                        log.add("merchantNumber", merchantNumber);
                        log.add("devPublicKey", devPublicKey);
                        log.add("devPrivateKey", devPrivateKey);
                        log.add("proPublicKey", proPublicKey);
                        log.add("proPrivateKey", proPrivateKey);
                        log.add("merchantInfo", merchantInfo);
                        log.add("agentName", agentName);
                        log.add("serviceId", serviceId);

                        merchantKeyManageDb.updatePartial(merchantId, merchantObj, new Handler<Boolean>() {
                            @Override
                            public void handle(final Boolean reply) {

                                if (reply) {
                                    log.add("modify success ", merchantId);
                                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Modify merchant success");
                                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 0);
                                    log.writeLog();
                                    response(request, joReply);
                                } else {
                                    log.add("Modify fail ", merchantId);
                                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Modify merchant fail");
                                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, -1);
                                    log.writeLog();
                                    response(request, joReply);
                                }
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.add("Modify exception", e.getMessage());
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    log.writeLog();
                    response(request, joReply);
                }
            }
        });
    }

    /* topup phone, param request:
     fromNumber 0982xxxxxx
     pin xxxxxxxxxxxx
     amount 50000
     toNumber 0903xxxxxx
     sourcefrom 1 momo
     */
    private void topup(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String fromNumberRequest = joReceive.getString(StringConstUtil.MerchantWeb.FROM_NUMBER, "");
                    final String pinRequest = joReceive.getString(StringConstUtil.MerchantWeb.PIN, "");
                    final String toNumberRequest = joReceive.getString(StringConstUtil.MerchantWeb.TO_NUMBER, "");
                    final long amount = joReceive.getLong(StringConstUtil.MerchantWeb.AMOUNT, 0L);
                    final int sourceFrom = joReceive.getInteger(StringConstUtil.MerchantWeb.SOURCE_FROM, 1);

                    log.setPhoneNumber(String.valueOf(fromNumberRequest));

                    if ("".equalsIgnoreCase(fromNumberRequest) || "".equalsIgnoreCase(pinRequest) || "".equalsIgnoreCase(toNumberRequest) || amount <= 0) {
                        log.add("desc", "Request wrong param");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Request wrong param");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }

                    /*byte[] testByte = pinRequest.trim().getBytes();
                    String pinTestEncrypt = cryption.encrypt(testByte, PUBLIC_KEY);*/
                    boolean result = cryption.resultVerifyData(pinRequest.trim(), PUBLIC_KEY, PRIVATE_KEY);
                    if (!result) {
                        //Du lieu khong dung, tra loi tiep
                        log.add("error", "Check encrypt fail");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Check encrypt fail");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }

                    final String pin = cryption.decrypt(pinRequest.trim(), PRIVATE_KEY.trim());
                    //final String pin = pinRequest;
                    final int fromNumber = DataUtil.strToInt(fromNumberRequest);
                    final int toNumber = DataUtil.strToInt(toNumberRequest);
                    log.add("fromNumber", fromNumber);

                    /*// validation
                    if (!PhoneNumberUtil.isValidPhoneNumber(fromNumber) || !PhoneNumberUtil.isValidPhoneNumber(toNumber)) {
                        log.add("error", "Phone number is invalid");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Phone number is invalid");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }*/

                    SockData sockData = new SockData(vertx, logger, container.config());

                    MomoProto.TranHisV1 tranHis = MomoProto.TranHisV1.newBuilder()
                            .setTranType(MomoProto.TranHisV1.TranType.TOP_UP_VALUE)
                            .setPartnerCode("0" + toNumber)
                            .setPartnerName("0" + toNumber)
                            .setPartnerId("0" + toNumber)
                            .setAmount(amount)
                            .build();
                    MomoMessage msg = new MomoMessage(MomoProto.MsgType.TRANS_REQUEST_VALUE, 0, fromNumber, tranHis.toByteArray());

                    final long ackTime = System.currentTimeMillis();
                    final String channel = Const.CHANNEL_WEB;

                    final Common.BuildLog log = new Common.BuildLog(logger);
                    log.setPhoneNumber("0" + fromNumber);
                    log.add("func", "processTopUp");
                    log.add("amount", amount);


                    log.add("to number", toNumber);
                    log.add("channel", channel);

                    final SoapProto.TopUp.Builder builder = SoapProto.TopUp.newBuilder();
                    builder.setFromNumber(fromNumber)
                            .setMpin(pin)
                            .setChannel(channel)
                            .setAmount(amount)
                            .setToNumber(toNumber);

                    builder.addKeyValuePairs(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));
                    builder.addKeyValuePairs(Misc.buildKeyValuePairForSoap("wallettype", String.valueOf(sourceFrom)));

                    log.add("topup", "via soapin");
                    vertx.eventBus().send(
                            AppConstant.SoapVerticle_ADDRESS,
                            MomoMessage.buildBuffer(
                                    SoapProto.MsgType.TOP_UP_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    builder.build()
                                            .toByteArray()
                            ),
                            new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> result) {

                                    JsonObject tranReply = result.body();
                                    int err = tranReply.getInteger("error");
                                    long tranId = tranReply.getLong("tranId");
                                    if (err == 0) {
                                        log.add("error", "Topup success");
                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(0));
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                        log.writeLog();
                                        response(request, joReply);
                                        return;
                                    } else {
                                        log.add("error", "Topup fail= " + SoapError.CoreErrorMap_VN.get(err));
                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Topup fail= " + SoapError.CoreErrorMap_VN.get(err));
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, err);
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                        log.writeLog();
                                        response(request, joReply);
                                        return;
                                    }
                                }
                            });


                } catch (Exception e) {
                    e.printStackTrace();
                    log.add("Modify exception", e.getMessage());
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                    log.writeLog();
                    response(request, joReply);
                }
            }
        });
    }

    /* bill pay check info , param request:
     fromNumber 0982xxxxxx
     pin xxxxxxxxxxxx
     billId pe15000268498
     serviceId dien
     */
    private void payBillCheckInfo(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("billPay", "CheckInfo");
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String fromNumberRequest = joReceive.getString(StringConstUtil.MerchantWeb.FROM_NUMBER, "");
                    final String pinRequest = joReceive.getString(StringConstUtil.MerchantWeb.PIN, "");
                    final String billId = joReceive.getString(StringConstUtil.MerchantWeb.BILL_ID, "");
                    final String serviceId = joReceive.getString(StringConstUtil.MerchantWeb.SERVICE_ID, "");


                    log.setPhoneNumber(String.valueOf(fromNumberRequest));

                    if ("".equalsIgnoreCase(fromNumberRequest) || "".equalsIgnoreCase(pinRequest) || "".equalsIgnoreCase(billId) || "".equalsIgnoreCase(serviceId)) {
                        log.add("desc", "Request wrong param");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Request wrong param");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        joReply.putString(StringConstUtil.MerchantKeyManage.DATA, "");
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }
                    //kiem pin sau ma hoa
                    if (pinRequest.length() == 6) {
                        byte[] testByte = pinRequest.trim().getBytes();
                        String pinTestEncrypt = cryption.encrypt(testByte, PUBLIC_KEY);
                        log.add("anhtu", pinTestEncrypt);
                    }
                    boolean result = cryption.resultVerifyData(pinRequest.trim(), PUBLIC_KEY, PRIVATE_KEY);
                    if (!result) {
                        //Du lieu khong dung, tra loi tiep
                        log.add("error", "Check encrypt fail");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Check encrypt fail");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                        joReply.putString(StringConstUtil.MerchantKeyManage.DATA, "");
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }

                    final String pin = cryption.decrypt(pinRequest.trim(), PRIVATE_KEY.trim());
                    //final String pin = pinRequest;
                    final int fromNumber = DataUtil.strToInt(fromNumberRequest);
                    log.add("fromNumber", fromNumber);
                    log.add("billId", billId);
                    log.add("serviceId", serviceId);


                    Buffer getBillBuf = MomoMessage.buildBuffer(
                            SoapProto.MsgType.GET_BILL_INFO_VALUE,
                            0,
                            fromNumber,
                            SoapProto.GetBillInfo.newBuilder()
                                    .setMpin(pin)
                                    .setBillId(billId)
                                    .setProviderId(serviceId)
                                    .build()
                                    .toByteArray()
                    );

                    log.add("payBillCheckInfo", "via soapin");
                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, getBillBuf, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> result) {

                            int rcode = result.body().getInteger("rcode", -1);
                            String billInfo = result.body().getString("billInfo", "");
                            if(Misc.isNullOrEmpty(billInfo)){
                                billInfo = result.body().getString("description", ""); // fix avg
                                log.add("description checkinfo", billInfo);
                            }
                            //JsonObject obj = BillHandler.billInfoToJsonObject(billInfo);

                            if (rcode == 0) {
                                log.add("error", "Check info success");
                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(0));
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                                joReply.putString(StringConstUtil.MerchantKeyManage.DATA, billInfo);
                                log.writeLog();
                                response(request, joReply);
                                return;
                            } else {
                                log.add("error", "Check info= " + SoapError.CoreErrorMap_VN.get(rcode));
                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Check info fail= " + SoapError.CoreErrorMap_VN.get(rcode));
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, rcode);
                                joReply.putString(StringConstUtil.MerchantKeyManage.DATA, billInfo);
                                log.writeLog();
                                response(request, joReply);
                                return;
                            }
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                    log.add("Modify exception", e.getMessage());
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    joReply.putString(StringConstUtil.MerchantKeyManage.DATA, "");
                    log.writeLog();
                    response(request, joReply);
                }
            }
        });
    }

    /* pay bill  , param request:
     fromNumber 0982xxxxxx
     pin xxxxxxxxxxxx
     billId pe15000268498
     serviceId dien
     amount 50000
     sourcefrom 1 momo, 2 mload
     */
    private void payBill(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("payBill", "payBill");
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String fromNumberRequest = joReceive.getString(StringConstUtil.MerchantWeb.FROM_NUMBER, "");
                    final String pinRequest = joReceive.getString(StringConstUtil.MerchantWeb.PIN, "");
                    final String billId = joReceive.getString(StringConstUtil.MerchantWeb.BILL_ID, "");
                    final String serviceId = joReceive.getString(StringConstUtil.MerchantWeb.SERVICE_ID, "");
                    final long amount = joReceive.getLong(StringConstUtil.MerchantWeb.AMOUNT, 0L);
                    final int sourcefrom = joReceive.getInteger(StringConstUtil.MerchantWeb.SOURCE_FROM, 1);


                    log.setPhoneNumber(String.valueOf(fromNumberRequest));

                    if ("".equalsIgnoreCase(fromNumberRequest) || "".equalsIgnoreCase(pinRequest) || "".equalsIgnoreCase(billId) || "".equalsIgnoreCase(serviceId)
                            || amount <= 0) {
                        log.add("desc", "Request wrong param");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Request wrong param");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        joReply.putString(StringConstUtil.MerchantKeyManage.TRANSID, "");
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }
                    //kiem pin sau ma hoa
                    if (pinRequest.length() == 6) {
                        byte[] testByte = pinRequest.trim().getBytes();
                        String pinTestEncrypt = cryption.encrypt(testByte, PUBLIC_KEY);
                        log.add("anhtu", pinTestEncrypt);
                    }
                    boolean result = cryption.resultVerifyData(pinRequest.trim(), PUBLIC_KEY, PRIVATE_KEY);
                    if (!result) {
                        //Du lieu khong dung, tra loi tiep
                        log.add("error", "Check encrypt fail");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Check encrypt fail");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                        joReply.putString(StringConstUtil.MerchantKeyManage.TRANSID, "");
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }

                    final String pin = cryption.decrypt(pinRequest.trim(), PRIVATE_KEY.trim());
                    final int fromNumber = DataUtil.strToInt(fromNumberRequest);
                    log.add("fromNumber", fromNumber);
                    log.add("billId", billId);
                    log.add("serviceId", serviceId);
                    log.add("amount", amount);

                    /*SockData sockData = new SockData(vertx, logger);
                    PhonesDb.Obj obj = new PhonesDb.Obj();
                    obj.pin = pin;
                    sockData.setPhoneObj(obj, logger, "");*/

                    SoapProto.PayOneBill.Builder builder = SoapProto.PayOneBill.newBuilder();
                    builder.setPin(pin)
                            .setProviderId(serviceId)
                            .setBillId(billId)
                            .setChannel(Const.CHANNEL_WEB)
                            .setAmount(amount);

                    builder.addKeyValuePairs(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));
                    builder.addKeyValuePairs(Misc.buildKeyValuePairForSoap("wallettype", String.valueOf(sourcefrom)));

                    Buffer buildBuffer = MomoMessage.buildBuffer(
                            SoapProto.MsgType.PAY_ONE_BILL_VALUE,
                            System.currentTimeMillis(),
                            fromNumber,
                            builder.build()
                                    .toByteArray()
                    );

                    log.add("begin", "soapin verticle");

                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buildBuffer, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> result) {
                            log.add("end", "soapin verticle");
                            JsonObject tranReply = result.body();
                            int error = tranReply.getInteger(colName.TranDBCols.ERROR, -100);
                            long tranId = tranReply.getLong(colName.TranDBCols.TRAN_ID, 0);
                            if (error == 0) {
                                log.add("error", "Paybill success");
                                log.add("error code", error);
                                log.add("transid", tranId);
                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(0));
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                log.writeLog();
                                response(request, joReply);
                                return;
                            } else {
                                log.add("error", "Paybill " + SoapError.CoreErrorMap_VN.get(error));
                                log.add("error code", error);
                                log.add("transid", tranId);
                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Paybill fail " + SoapError.CoreErrorMap_VN.get(error));
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, error);
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                log.writeLog();
                                response(request, joReply);
                                return;
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    log.add("Modify exception", e.getMessage());
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    joReply.putString(StringConstUtil.MerchantKeyManage.TRANSID, "");
                    log.writeLog();
                    response(request, joReply);
                }
            }
        });
    }

    public static void response(HttpServerRequest request, JsonObject jsonObject) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(jsonObject.toString());

        request.response().end();
    }

    public static void response(HttpServerRequest request, String result) {
        request.response().setChunked(true);
        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(result);

        request.response().end();
    }

    /* tranfer, param request:
     fromNumber 0982xxxxxx
     pin xxxxxxxxxxxx
     amount 50000
     toNumber 0903xxxxxx
     sourcefrom 1 momo
     */
    private void m2m(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("M2M data get byte ", buffer.getBytes());
                    log.add("M2M data to string ", buffer.toString());
                    final long now = System.currentTimeMillis();

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String fromNumberRequest = joReceive.getString(StringConstUtil.MerchantWeb.FROM_NUMBER, "");
                    final String pinRequest = joReceive.getString(StringConstUtil.MerchantWeb.PIN, "");
                    final String toNumberRequest = joReceive.getString(StringConstUtil.MerchantWeb.TO_NUMBER, "");
                    final long amount = joReceive.getLong(StringConstUtil.MerchantWeb.AMOUNT, 0L);
                    final int sourceFrom = joReceive.getInteger(StringConstUtil.MerchantWeb.SOURCE_FROM, 1);

                    log.setPhoneNumber(String.valueOf(fromNumberRequest));

                    if ("".equalsIgnoreCase(fromNumberRequest) || "".equalsIgnoreCase(pinRequest) || "".equalsIgnoreCase(toNumberRequest) || amount <= 0) {
                        log.add("desc", "Request wrong param");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Request wrong param");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }

                    //byte[] testByte = pinRequest.trim().getBytes();
                    //String pinTestEncrypt = cryption.encrypt(testByte, PUBLIC_KEY);
                    boolean result = cryption.resultVerifyData(pinRequest.trim(), PUBLIC_KEY, PRIVATE_KEY);
                    if (!result) {
                        //Du lieu khong dung, tra loi tiep
                        log.add("error", "Check encrypt fail");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Check encrypt fail");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        log.writeLog();
                        response(request, joReply);
                        return;
                    }

                    final String pin = cryption.decrypt(pinRequest.trim(), PRIVATE_KEY.trim());
                    final int fromNumber = DataUtil.strToInt(fromNumberRequest);
                    final int toNumber = DataUtil.strToInt(toNumberRequest);
                    log.add("fromNumber", fromNumber);

                    //build data to core
                    final String channel = Const.CHANNEL_WEB;
                    SoapProto.M2MTransfer.Builder builder = SoapProto.M2MTransfer.newBuilder();
                    builder.setAgent(fromNumberRequest)
                            .setMpin(pin)
                            .setPhone(toNumberRequest)
                            .setChannel(channel)
                            .setAmount(amount);

                    builder.addKvps(Misc.buildKeyValuePairForSoap("time", String.valueOf(log.getTime())));

                    final Buffer m2mTransfer = MomoMessage.buildBuffer(
                            SoapProto.MsgType.M2M_TRANSFER_VALUE,
                            System.currentTimeMillis(),
                            fromNumber,
                            builder.build().toByteArray()
                    );

                    log.add("m2m", "via soapin");
                    vertx.eventBus().send(
                            AppConstant.SoapVerticle_ADDRESS,
                            m2mTransfer,
                            new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> result) {

                                    JsonObject tranReply = result.body();
                                    int err = tranReply.getInteger("error");
                                    long tranId = tranReply.getLong("tranId");
                                    if(tranId == 0 || tranId == -1){
                                        tranId = now;
                                    }
                                    if (err == 0) {
                                        log.add("error", "M2M success");
                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(0));
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                        response(request, joReply);
                                        log.add("noti", "send noti");
                                        log.writeLog();
                                        //fromNumberSendNoti(toNumber, amount, tranId,fromNumberRequest, toNumberRequest);
                                        toUserSendNotiAndSMS(true, toNumber, amount, tranId,fromNumberRequest, toNumberRequest);
                                        sendTranHis(true, tranId, fromNumberRequest, amount, toNumber);
                                        return;
                                    } else {
                                        log.add("error", "M2M fail= " + SoapError.CoreErrorMap_VN.get(err));
                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "M2M fail= " + SoapError.CoreErrorMap_VN.get(err));
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, err);
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                        log.writeLog();
                                        response(request, joReply);
                                        toUserSendNotiAndSMS(false , toNumber, amount, tranId,fromNumberRequest, toNumberRequest);
                                        sendTranHis(false, tranId, fromNumberRequest, amount, toNumber);
                                        return;
                                    }
                                }
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                    log.add("Modify exception", e.getMessage());
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                    log.writeLog();
                    response(request, joReply);
                }
            }
        });
    }

    /*private void fromNumberSendNoti(int toUser, long amount, long tranId, String fromNumber, String toNumber){
        String comment = "";
        String tpl = "";
        String caption = "";
        String sms = "";
        tpl = "Quý khách đã chuyển thành công số tiền %s đến %s";
        comment = String.format(tpl
                , String.format("%,d", amount).replace(",", ".") + "đ"
                , toNumber);
        caption = "Chuyển tiền thành công";
        String smsTmp = "Chuc mung quy khach da chuyen thanh cong so tien %sd den %s luc: %s. TID:%s. Xin cam on";
        sms = String.format(smsTmp
                , String.format("%,d", amount).replace(",", ".")
                , toNumber
                , Misc.getDate(System.currentTimeMillis())
                , tranId
        );

        Notification noti = new Notification();
        noti.receiverNumber = toUser;
        noti.caption = caption;
        noti.body = comment;
        noti.sms = sms;
        noti.priority = 2;
        noti.time = System.currentTimeMillis();
        noti.tranId = tranId;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                    }
                });
    }*/

    private void toUserSendNotiAndSMS(boolean result, int toUser, long amount, long tranId, String fromNumber, String toNumber){
        String comment = "";
        String tpl = "";
        String caption = "";
        String sms = "";
        if(result){
            tpl = "Quý khách đã nhận thành công số tiền %s từ %s";
            caption = "Nhận tiền thành công!";

        }else{
            tpl = "Quý khách nhận không thành công số tiền %s từ %s";
            caption = "Nhận tiền không thành công!";
        }

        comment = String.format(tpl
                , String.format("%,d", amount).replace(",", ".") + "đ"
                , fromNumber);
        /*String smsTmp = "Chuc mung quy khach da nhan thanh cong so tien %sd tu %s luc: %s. TID:%s. Xin cam on";
        sms = String.format(smsTmp
               // , Misc.formatAmount(amount).replace(",", "")
                , String.format("%,d", amount).replace(",", ".")
                , fromNumber
                , Misc.getDate(System.currentTimeMillis())
                , tranId
        );*/

        Notification noti = new Notification();
        noti.receiverNumber = toUser;
        noti.caption = caption;
        noti.body = comment;
        noti.sms = sms;
        noti.priority = 2;
        noti.time = System.currentTimeMillis();
        noti.tranId = tranId;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                    }
                });
    }

    private void sendTranHis(boolean result,long tranId, String ownerNumber,long amount,int toUser){
        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, 7);
        if(result){
            jsonTrans.putString(colName.TranDBCols.COMMENT, "Quý khách đã nhận thành công từ " + ownerNumber + " Số tiền: " + String.format("%,d", amount) + "đ.");
            jsonTrans.putNumber(colName.TranDBCols.STATUS, 4);
        }else{
            jsonTrans.putString(colName.TranDBCols.COMMENT, "Quý khách nhận không thành công từ " + ownerNumber + " Số tiền: " + String.format("%,d", amount) + "đ.");
            jsonTrans.putNumber(colName.TranDBCols.STATUS, 5);
        }

        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amount);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, toUser);
        jsonTrans.putString(colName.TranDBCols.BILL_ID, "");
        jsonTrans.putNumber(colName.TranDBCols.IO, 1);
        Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTrans, new JsonObject());
    }


    /* checkagent user, param request:
     customerNumber 0982123456
     */
    private void checkagent(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());
                    log.setPhoneNumber("excutePos");

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String customerNumber = joReceive.getString(StringConstUtil.MerchantWeb.CUSTOMER_NUMBER, "");

                    if ( "".equalsIgnoreCase(customerNumber) ) {
                        log.add("desc", "Wrong request");
                        joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                        joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1020);
                        joReply.putString(StringConstUtil.MerchantWeb.DATA, "");
//                        log.writeLog();
                        response(request, joReply);
                        return;
                    } else {

                        final int phoneNumber = DataUtil.strToInt(customerNumber);
                        log.add("phoneNumber", customerNumber);

                        phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(final PhonesDb.Obj phoneObj) {

                                if (phoneObj == null) {
                                    log.add("desc", "Agent not found");
                                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Agent not found");
                                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 3);
                                    joReply.putString(StringConstUtil.MerchantWeb.DATA, "");
//                                    log.writeLog();
                                    response(request, joReply);
                                    return;
                                }else{
                                    log.add("desc", "Agent ok");
                                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Agent ok");
                                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 0);
                                    joReply.putObject(StringConstUtil.MerchantWeb.DATA, phoneObj.toJsonObject());
//                                    log.writeLog();
                                    response(request, joReply);
                                    return;
                                }


                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.add("desc", "Exception " + e.getMessage());
                    joReply.putString(StringConstUtil.MerchantWeb.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantWeb.STATUS, 1006);
                    joReply.putString(StringConstUtil.MerchantWeb.DATA, "");
                    log.writeLog();
                    response(request, joReply);
                    return;
                }
            }
        });
    }

}
