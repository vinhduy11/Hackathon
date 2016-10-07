package com.mservice.momo.vertx;

import com.mservice.momo.data.ControlOnClickActivityDb;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.MerchantOfflinePayment.MerchantKeyManageDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.gift.GiftTranDb;
import com.mservice.momo.data.ipos.IPosCashLimitDb;
import com.mservice.momo.data.lotte.LottePromoCodeDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.db.oracle.HTPPOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.permission.Cryption;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.MomoJackJsonPack;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.GiftTran;
import com.mservice.momo.vertx.merchant.lotte.entity.*;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.GiftProcess;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import com.mservice.payment.token.PaymentCodeServer;
import com.mservice.security.otpas.Base32;
import org.apache.commons.lang3.RandomStringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.*;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

/**
 * Created by ios-001 on 2/25/16.
 */
public class LotteVerticle extends Verticle {

    private static final String MOMO_KEYSTORE_PATH = "momokeystore.jks";
    private static final String MOMO_KEYSTORE_PASSWORD = "MomoApp";
    private static final boolean IS_SSL = false;
    private static int PORT = 8090;
    private static String HOST_ADDRESS = "0.0.0.0";
    Hashtable<String, Integer> hashPayMent;
    private HttpClient httpClientMIS;
    private HttpServer httpServer;
    private HttpClient httpClientConnector;
    private MerchantKeyManageDb merchantKeyManageDb;
    private LottePromoCodeDb lottePromoCodeDb;
    private IPosCashLimitDb iPosCashLimitDb;
    private long MIN_AMOUNT = 10000;
    private long MAX_AMOUNT = 5000000;
    private PhonesDb phonesDb;
    private TransDb transDb;
    private String SERVICE_ID = "";
    private String SERVICE_NAME = "";
    private String agentCreditor;
    private String publicKey;
    private String privateKey;
    private long CASH_LIMIT = 5000000;
    private GiftProcess giftProcess;
    private GiftTranDb giftTranDb;
    private String GIFT_MOMO_AGENT = "";
    private GiftManager giftManager;
    private PromotionProcess promotionProcess;
    private ControlOnClickActivityDb controlOnClickActivityDb;

    @Override
    public void start() {
        final Logger logger = container.logger();
        final JsonObject globalConfig = container.config();
        JsonObject selfConfig = globalConfig.getObject("LotteVerticle", new JsonObject());
        PORT = selfConfig.getInteger("port", 8090);
        HOST_ADDRESS = selfConfig.getString("host", "0.0.0.0");
        agentCreditor = selfConfig.getString("agentCreditor", "payment_scbvn");
        SERVICE_ID = selfConfig.getString("serviceId", "lotte");
        JsonObject giftConfig = globalConfig.getObject("gift", new JsonObject());
        GIFT_MOMO_AGENT = giftConfig.getString("momoAgent", null);
        giftManager = new GiftManager(vertx, logger, globalConfig);
        SERVICE_NAME = selfConfig.getString("serviceName", "LOTTE");
        publicKey = selfConfig.getString("publicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh7U/P2vDywftQ34us2hYEcCvPIIwGACd\n6yR0PoH5IdSps9O3Vj+50Dc/jh+ZJDCcDyWCc9pHXaMrg8mMQXrescL/azg+fjlF+sb4syd/Y45h\nBs+XfJjR025PIf44NMt6wsekL1PaxWQMkyrZDHldqr30wzkWIAxjzVaKZHaB7H2TLiar399lwn6S\nJQAtlj2yUbYVEGFrbP8bO/0gqbc88uYNYDtZ/HxIT54FIs1zLrik+NaAiFaKkMlvNowBGqilJDZg\nDXybt2H/HbIE3QG75WmKkGNl5r7gF73yOxt+2/iIomfTFXQuzNCSqTk0vXRGXrO0zzzXhs51PrA2\nITt+ewIDAQAB");
        privateKey = selfConfig.getString("privateKey", "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCHtT8/a8PLB+1Dfi6zaFgRwK88\ngjAYAJ3rJHQ+gfkh1Kmz07dWP7nQNz+OH5kkMJwPJYJz2kddoyuDyYxBet6xwv9rOD5+OUX6xviz\nJ39jjmEGz5d8mNHTbk8h/jg0y3rCx6QvU9rFZAyTKtkMeV2qvfTDORYgDGPNVopkdoHsfZMuJqvf\n32XCfpIlAC2WPbJRthUQYWts/xs7/SCptzzy5g1gO1n8fEhPngUizXMuuKT41oCIVoqQyW82jAEa\nqKUkNmANfJu3Yf8dsgTdAbvlaYqQY2XmvuAXvfI7G37b+IiiZ9MVdC7M0JKpOTS9dEZes7TPPNeG\nznU+sDYhO357AgMBAAECggEACGcmpWys0POKs3Uqux0o5uCBeUOuaq4PGTJGqGAv5vJeF65yWbrU\nbJofK1O1jdIct6tg6n+Hj7q8xQpDzwImq+chHah75iDxvld3qtA1SRrV0zAjdymXRHo+GCK/pDU9\nQrZN6khM8ZACX1J14hnZvOrKLxRn1FAwFdTX+OSFovkEXgzAvwziEYWdZx+xHNzAfLFGI2q5I1/Y\n+Fo1Iqv30T/eNquav6eRqhZObcOwk7ubqTH4+7LikKTtHD7Mc/w3lsdRK7JDhhuU3ZVrG6DaGYSx\n4u10C133kiCirU7hx9VrfmNU/K+QT6I+QURp9iGVTAXxiktK+E0MYW2DaCRkUQKBgQDPwPQSam+I\nBtISdQ0MyG3YN+Xy4xS7Vv2u53sTbjrnBMAjwpf70Av9n8SmQcwsyOvN1fdh/k7RYiCqm+wYIXK5\nkOsdWTngcWfsofuDpmZ0xIJue4GLWgpnwStliFcZNQ+RmqXAoN1v0BojCJ3EUEe1Hon+1siEay5/\nG07lZi79pwKBgQCnOSUMieZlV0MWimhzqbxWKVjZldcwJJRDkH2rBCdEEDvmbXPWlkaSf+f5TqPC\n1pW9+kSMlKMFzCb0OPlycyzc4HQET91W8cBZ0oAU5JkmbU/eaHLjdLtZrDqvJXvoETIvsWfF/rX5\nM9BvJ1CrMAKWs2eP2h70IItaSZ3TFigbDQKBgQCy7CDIA3aOliEx2DEnAy7m+i1GI5/lQCQ3EBEF\nSPfdok+//IVcT28kPQblkOFA6MF8gnwNnzFOTs6HYMXSemwNuOtkWUXpMIIqX873MMYoZl1WvXNy\nClfPx6OSS/uHMBV6ds+tuF09QOJhrbgCLIm1SNnb6irDMHPY8DHgi0KJHQKBgEaH6cKCH3U6+wb0\n1d2DB/bndZxAgQTDSO2+ceLB27XvivRD0gn+VEHSRQt4ScYSMBJzDpqkzqXRV9TGex/0yEVZPlXb\nQaY8TT8VdARb7uSwUnGiaGLbh3HpHM9m5f4Z0qsfDoAKUMKNQiq/0FyD4XOis9mOzGN3no80Yab4\nv225AoGBAJ59Qp/BdkSO8IEHWQGJiDDa2ICg1znluY9MNpAzbha9QtGzwvTgMaEYhBFu5VtbI2Aq\nsNlLKMY6yOfS2/HGyF9YU6i4Lde5LRmo/bnoU2tgVfTtGzxEooNfAp2r1F2QpsiAaeJe4ZuxneaR\njPPIbUbkyTupd7XFQ97rPykF45nw");
        httpClientMIS = getVertx().createHttpClient();
        httpClientMIS.setHost(selfConfig.getObject("mis_merchant").getString("host", "172.16.23.152"));
        httpClientMIS.setPort(selfConfig.getObject("mis_merchant").getInteger("port", 8080));
        httpClientMIS.setMaxPoolSize(20);
        httpClientMIS.setConnectTimeout(50000);
        httpClientMIS.setKeepAlive(false);
        httpClientConnector = getVertx().createHttpClient();
        httpClientConnector.setHost(selfConfig.getObject("connector_merchant").getString("host", "172.16.44.179"));
        httpClientConnector.setPort(selfConfig.getObject("connector_merchant").getInteger("port", 8092));
        httpClientConnector.setMaxPoolSize(20);
        httpClientConnector.setConnectTimeout(50000);
        httpClientConnector.setKeepAlive(false);
        merchantKeyManageDb = new MerchantKeyManageDb(vertx, container.logger());
        phonesDb = new PhonesDb(vertx.eventBus(), container.logger());
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        lottePromoCodeDb = new LottePromoCodeDb(vertx, logger);
        iPosCashLimitDb = new IPosCashLimitDb(vertx, logger);
        hashPayMent = new Hashtable<>();
        controlOnClickActivityDb = new ControlOnClickActivityDb(vertx);
        Common mcom = new Common(vertx, logger, globalConfig);
        giftProcess = new GiftProcess(mcom, vertx, logger, globalConfig);
        giftTranDb = new GiftTranDb(vertx, logger);
        promotionProcess = new PromotionProcess(vertx, logger, globalConfig);
        httpServer = vertx.createHttpServer();
        if (IS_SSL) {
            httpServer.setSSL(true)
                    .setKeyStorePath(MOMO_KEYSTORE_PATH)
                    .setKeyStorePassword(MOMO_KEYSTORE_PASSWORD)
                    .setClientAuthRequired(false);
        }
        httpServer.requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest request) {
                final HttpServerResponse response = request.response();

                logger.info("[WebService] [" + request.method() + "] " + " uri: " + request.uri() + " path: " + request.path());
                final JsonObject joReply = new JsonObject();
                final Cryption cryption = new Cryption(globalConfig, logger);
                final String path = request.path();
                for (int i = 0; i < request.headers().size(); i++) {
                    logger.info("key " + request.headers().entries().get(i).getKey() + " value " + request.headers().entries().get(i).getValue());
                }
                if (request.method().equalsIgnoreCase("POST")) {
                    if ("/user/login".equalsIgnoreCase(path)) {
                        request.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                final MerLogin loginMsg = parseMsg(buffer, MerLogin.class);
                                if (loginMsg == null) {
                                    MerMsg errorMsg = new MerMsg();
                                    errorMsg.setErrorCode(MerErrorCode.WrongUserPass);
                                    sendDataToClient(response, errorMsg);
                                    return;
                                }
                                doLogin(cryption, loginMsg, response);
                            }
                        });
                    } else if ("/user/reset_pin".equalsIgnoreCase(path)) {
                        request.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                final MerChangePass merChangePass = parseMsg(buffer, MerChangePass.class);
                                if (merChangePass == null) {
                                    MerMsg errorMsg = new MerMsg();
                                    errorMsg.setErrorCode(MerErrorCode.WrongRequest);
                                    sendDataToClient(response, errorMsg);
                                    return;
                                }
                                doChangePass(merChangePass, response, cryption);
                            }
                        });
                    } else if ("/bill/check".equalsIgnoreCase(path)) {
                        request.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                final MerPayment msg = parseMsg(buffer, MerPayment.class);
                                if (msg == null) {
                                    MerMsg errorMsg = new MerMsg();
                                    errorMsg.setErrorCode(MerErrorCode.WrongRequest);
                                    sendDataToClient(response, errorMsg);
                                    return;
                                }
                                checkPaymentCode(cryption, msg, response);
                            }
                        });
                    } else if ("/bill/his".equalsIgnoreCase(path)) {
                        request.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                final MerTranhis msg = parseMsg(buffer, MerTranhis.class);
                                if (msg == null) {
                                    MerMsg errorMsg = new MerMsg();
                                    errorMsg.setErrorCode(MerErrorCode.WrongRequest);
                                    sendDataToClient(response, errorMsg);
                                    return;
                                }
                                doGetTranHis(msg, response, cryption);
                            }
                        });
                    } else if ("/bill/pay".equalsIgnoreCase(path)) {
                        request.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                final MerPayment msg = parseMsg(buffer, MerPayment.class);
                                if (msg == null) {
                                    MerMsg errorMsg = new MerMsg();
                                    errorMsg.setErrorCode(MerErrorCode.WrongRequest);
                                    sendDataToClient(response, errorMsg);
                                    return;
                                }
                                doPayment(cryption, msg, response);
                            }
                        });
                    } else {
                        MerMsg errorMsg = new MerMsg();
                        errorMsg.setErrorCode(MerErrorCode.WrongRequest);
                        sendDataToClient(response, errorMsg);
                        return;
                    }
                }
            }
        }).listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
            @Override
            public void handle(AsyncResult<HttpServer> httpServerAsyncResult) {
                if (httpServerAsyncResult.succeeded()) {
                    logger.info("LotteVerticle's listening on " + HOST_ADDRESS + ":" + PORT);
                }
            }
        });
    }

    private void doGetTranHis(final MerTranhis msg, final HttpServerResponse response, final Cryption cryption) {
        final Logger logger = container.logger();

        checkSessionUser(cryption, msg, new Handler<Boolean>() {
            @Override
            public void handle(Boolean check) {
                if (check) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.putObject("data", new JsonObject(MomoJackJsonPack.objToString(msg)));
                    jsonObject.putNumber(HTPPOracleVerticle.COMMAND, HTPPOracleVerticle.GET_MERCHANT_TRANHIS);

                    vertx.eventBus().send(AppConstant.HTPPOracleVerticle_ADDRESS, jsonObject, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            if (message.body() != null) {
                                sendDataToClient(response, MomoJackJsonPack.jsonToObj(message.body().encode(), MerTranhis.class));
                                logger.info("Get tranhis of merchant " + msg.merchantId + " is OK " + message.body());
                                return;
                            } else {
                                msg.setErrorCode(MerErrorCode.InternalError);
                                sendDataToClient(response, msg);
                                logger.error("Get tranhis of merchant is FAIL");
                                return;
                            }
                        }
                    });
                } else {
                    msg.setErrorCode(MerErrorCode.LoginAnotherDevice);
                    sendDataToClient(response, msg);
                    logger.error(msg.errorDesc);
                    return;
                }
            }
        });
    }

    private void doInsertTran(final MerPayment msg, final String cashier, final long now, final Handler<Boolean> callback) {
        final Logger logger = container.logger();
        MerTranInfo merTranInfo = new MerTranInfo();
        if (!Misc.isNullOrEmpty(cashier)) {
            JsonObject cashierObj = new JsonObject(cashier);
            merTranInfo.STORE_ID = cashierObj.getObject("data").getObject("userProfile").getInteger("store_ID", 0);
            merTranInfo.STAFF_ID = cashierObj.getObject("data").getInteger("id", 0);
        }
        merTranInfo.ID = UUID.randomUUID().toString();
        merTranInfo.CREATED_DATE = now;
        merTranInfo.TID = msg.TID;
        merTranInfo.PHONE_NUMBER = msg.phone;
        merTranInfo.AMOUNT = (long) msg.oriAmount;
        merTranInfo.FEE = 0;
        merTranInfo.RECEIVED_AMOUNT = (long) msg.amount;
        merTranInfo.STATUS = msg.errorCode;
        merTranInfo.REFCODE = msg.idRef;
        merTranInfo.ERROR_CODE = String.valueOf(msg.errorCode);
        if (checkShowError(msg.errorCode)) {
            merTranInfo.DESCRIPTION = "Giao dịch gặp lỗi. Vui lòng hướng dẫn KH cập nhật lại mã thanh toán và thực hiện lại giao dịch.";
        } else {
            merTranInfo.DESCRIPTION = msg.errorDesc;
        }
        merTranInfo.PAYMENTCODE = msg.paymentCode;
        merTranInfo.VOUCHERCODE = msg.voucherCode;
        JsonObject jsonObject = new JsonObject();
        jsonObject.putObject("data", new JsonObject(MomoJackJsonPack.objToString(merTranInfo)));
        jsonObject.putNumber(HTPPOracleVerticle.COMMAND, HTPPOracleVerticle.INSERT_MERCHANT_TRAN);

        vertx.eventBus().send(AppConstant.HTPPOracleVerticle_ADDRESS, jsonObject, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                if (message.body() != null) {
                    logger.info("Insert tranhis is " + (message.body().getBoolean("result") ? "OK" : "FAIL"));
                    callback.handle(message.body().getBoolean("result"));
                } else {
                    callback.handle(false);
                    logger.error("Insert tranhis is FAIL");
                }
            }
        });
    }

    private void doChangePass(final MerChangePass merChangePass, final HttpServerResponse response, final Cryption cryption) {
        final Logger logger = container.logger();
        final String password;
        final String newPassword;
        try {
            boolean resultPin = cryption.resultVerifyData(merChangePass.merchantPIN.trim(), publicKey, privateKey);
            boolean resultNewPin = cryption.resultVerifyData(merChangePass.newPIN.trim(), publicKey, privateKey);
            if (!resultPin || !resultNewPin) {
                merChangePass.setErrorCode(MerErrorCode.PinHashError);
                sendDataToClient(response, merChangePass);
                logger.error("Login error: wrong hash password");
                return;
            }
            password = cryption.decrypt(merChangePass.merchantPIN.trim(), privateKey.trim());
            newPassword = cryption.decrypt(merChangePass.newPIN.trim(), privateKey.trim());
            logger.error("Decrypt pass success old:" + password + " new:" + newPassword);
        } catch (Exception e) {
            e.printStackTrace();
            merChangePass.setErrorCode(MerErrorCode.PinHashError);
            sendDataToClient(response, merChangePass);
            logger.error("unexpected error", e);
            return;
        }

        checkSessionUser(cryption, merChangePass, new Handler<Boolean>() {
            @Override
            public void handle(Boolean check) {
                if (check) {
                    final String path = "/lottemart/api/changePass?username="
                            + merChangePass.merchantId + "&oldpassword=" + password
                            + "&newpassword=" + newPassword + "&retypepassword=" + newPassword;

                    doPostMISMerchant(path, null, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject resultJSON) {
                            if (!resultJSON.getBoolean("result")) {
                                merChangePass.setErrorCode(MerErrorCode.InternalError);
                                sendDataToClient(response, merChangePass);
                                logger.error("Change pass error: code=" + resultJSON.getInteger("errCode") + ", desc=" + resultJSON.getInteger("errDesc"));
                                return;
                            } else {
                                if (resultJSON.getObject("data") != null) {
                                    /**
                                     * {"resultCode":0,"message":"Mật khẩu cũ không đúng","data":null,"data1":null,"data2":null,"Ext":null}
                                     * -4: Username/Mật khẩu rỗng;
                                     * -3: Mật khẩu mới không phức tạp;
                                     * -2: Mật khẩu nhập lại <> mật khẩu mới;
                                     * -1: User không tồn tại;
                                     * 0: Mật khẩu cũ không đúng;
                                     * 1: đổi mật khẩu thành công;
                                     * 2: Something wrong happen
                                     */
                                    JsonObject data = resultJSON.getObject("data");
                                    if (1 != data.getInteger("resultCode")) {
                                        merChangePass.setErrorCode(MerErrorCode.InternalError);
                                        merChangePass.appendErrorDesc(": " + data.getString("message"));
                                        sendDataToClient(response, merChangePass);
                                        logger.error("Change pass error: MIS ErrCode=" + data.getInteger("resultCode") + ", MIS message=" + data.getString("message"));
                                        return;
                                    } else {
                                        sendDataToClient(response, merChangePass);
                                        return;
                                    }
                                } else {
                                    merChangePass.setErrorCode(MerErrorCode.InternalError);
                                    sendDataToClient(response, merChangePass);
                                    logger.error("Change pass error, result json is null");
                                    return;
                                }
                            }
                        }
                    });
                } else {
                    merChangePass.setErrorCode(MerErrorCode.LoginAnotherDevice);
                    sendDataToClient(response, merChangePass);
                    logger.error(merChangePass.errorDesc);
                    return;
                }
            }
        });


    }

    protected void sendDataToClient(HttpServerResponse res, MerMsg input) {
        final Logger logger = container.logger();

        res.putHeader("Content-Type", "application/json; charset=utf-8");
        res.setChunked(true);
        final String json = MomoJackJsonPack.objToString(input);
        res.write(new Buffer(json, "UTF-8"));
        logger.info("Merchant response: " + new JsonObject(json).encodePrettily());
        res.end();
        res.close();
    }

    private void doLogin(final Cryption cryption, final MerLogin loginMsg, final HttpServerResponse response) {
        final Logger logger = container.logger();
        final String password;
        try {
            boolean result = cryption.resultVerifyData(loginMsg.merchantPIN.trim(), publicKey, privateKey);
            if (!result) {
                loginMsg.setErrorCode(MerErrorCode.PinHashError);
                sendDataToClient(response, loginMsg);
                logger.error("Login error: wrong hash password");
                return;
            }
            password = cryption.decrypt(loginMsg.merchantPIN.trim(), privateKey);
            logger.error("Decrypt pass success " + password);
        } catch (Exception e) {
            e.printStackTrace();
            loginMsg.setErrorCode(MerErrorCode.PinHashError);
            sendDataToClient(response, loginMsg);
            logger.error("unexpected error", e);
            return;
        }

        JsonObject loginJSON = new JsonObject();
        loginJSON.putString("username", loginMsg.merchantId);
        loginJSON.putString("password", password);

        doPostMISMerchant("/lottemart/api/login", loginJSON.encodePrettily(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject outData) {
                if (!outData.getBoolean("result")) {
                    loginMsg.setErrorCode(MerErrorCode.WrongUserPass);
                    sendDataToClient(response, loginMsg);
                    logger.error("Login error: code=" + outData.getInteger("errCode") + ", desc=" + outData.getString("errDesc"));
                    return;
                } else {
                    JsonObject jsonObject = outData.getObject("data");
                    if (jsonObject.getObject("data") == null) {
                        loginMsg.setErrorCode(MerErrorCode.WrongUserPass);
                        sendDataToClient(response, loginMsg);
                        return;
                    }
                    String storeId = String.valueOf(jsonObject.getObject("data").getObject("userProfile").getInteger("store_ID", 0));
                    if (0 == jsonObject.getInteger("resultCode") && !storeId.equals(loginMsg.storeId)) {
                        loginMsg.setErrorCode(MerErrorCode.WrongStoreId);
                        sendDataToClient(response, loginMsg);
                        return;
                    } else if (0 == jsonObject.getInteger("resultCode") && storeId.equals(loginMsg.storeId)) {
                        try {
                            JsonArray array = jsonObject.getObject("data").getArray("authorities");
                            for (Object object : array) {
                                JsonObject auth = (JsonObject) object;
                                if ("ROLE_SUB".equalsIgnoreCase(auth.getString("name"))) {
                                    JsonArray privileges = auth.getArray("privileges");
                                    for (Object privilege : privileges) {
                                        loginMsg.authorities.add(MomoJackJsonPack.jsonToObj(((JsonObject) privilege).encode(), MerAuthority.class));
                                    }
                                }
                            }
                            final String agentInfo = jsonObject.encode();
                            final String agentName = jsonObject.getObject("data").getObject("userProfile").getString("agent");
                            loginMsg.sessionKey = UUID.randomUUID().toString();
                            logger.info("I generate new session key [" + loginMsg.sessionKey + "] for merchant [" + loginMsg.merchantId + "]");

                            // check
                            merchantKeyManageDb.findOne(loginMsg.merchantId, new Handler<MerchantKeyManageDb.Obj>() {
                                @Override
                                public void handle(MerchantKeyManageDb.Obj obj) {
                                    if (obj != null) {
                                        // update
                                        obj.dev_pri_key = loginMsg.sessionKey;
                                        obj.merchant_infos = agentInfo;
                                        merchantKeyManageDb.updatePartial(loginMsg.merchantId, obj.toJson(), new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {
                                                if (!aBoolean) {
                                                    logger.error("Can not add update merchant", null);
                                                    loginMsg.setErrorCode(MerErrorCode.WrongUserPass);
                                                }
                                                sendDataToClient(response, loginMsg);
                                                return;
                                            }
                                        });
                                    } else {
                                        MerchantKeyManageDb.Obj newMer = new MerchantKeyManageDb.Obj();
                                        newMer.merchant_id = loginMsg.merchantId;
                                        newMer.agent_name = agentName;
                                        newMer.merchant_infos = agentInfo;
                                        newMer.dev_pri_key = loginMsg.sessionKey;
                                        newMer.service_id = SERVICE_ID;

                                        merchantKeyManageDb.insert(newMer, new Handler<Integer>() {
                                            @Override
                                            public void handle(Integer integer) {
                                                if (integer != 0) {
                                                    logger.error("Can not add new merchant: " + integer, null);
                                                    loginMsg.setErrorCode(MerErrorCode.WrongUserPass);
                                                }
                                                sendDataToClient(response, loginMsg);
                                                return;
                                            }
                                        });
                                    }
                                }
                            });
                        } catch (Exception e) {
                            loginMsg.setErrorCode(MerErrorCode.WrongUserPass);
                            sendDataToClient(response, loginMsg);
                            logger.error("unexpected error", e);
                            return;
                        }
                    } else {
                        loginMsg.setErrorCode(MerErrorCode.WrongUserPass);
                        sendDataToClient(response, loginMsg);
                        return;
                    }
                }
            }
        });
    }

    protected <T> T parseMsg(Buffer buffer, Class<T> type) {
        final Logger logger = container.logger();
        try {
            String json = new String(buffer.getBytes(), "UTF-8");
            logger.info("Merchant request: " + json);
            return (T) MomoJackJsonPack.jsonToObj(json, type);
        } catch (Exception e) {
            logger.error("Merchant parse request fail: " + e.getMessage(), e);
        }
        return null;
    }

    private void doPostMISMerchant(String path, String data, final Handler<JsonObject> callback) {
        final Logger logger = container.logger();
        logger.info("I post MIS path " + path);

        final JsonObject result = new JsonObject();

        HttpClientRequest request = httpClientMIS.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {

                if (response.statusCode() != 200) {
                    result.putBoolean("result", false);
                    result.putNumber("errCode", 10000 + response.statusCode());
                    result.putString("errDesc", "Send to Merchant error: statusCode=" + response.statusCode() + ", statusMessage=" + response.statusMessage());
                    callback.handle(result);
                    return;
                }

                response.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        result.putBoolean("result", true);
                        result.putObject("data", new JsonObject(buffer.toString()));
                        logger.info("MIS response data " + buffer.toString());
                        callback.handle(result);
                        return;
                    }
                });
                response.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        result.putBoolean("result", false);
                        result.putNumber("errCode", 9999);
                        result.putString("errDesc", "Unexpected error: " + throwable.getMessage());
                        callback.handle(result);
                        return;
                    }
                });
            }
        });
        request.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                result.putBoolean("result", false);
                result.putNumber("errCode", 9999);
                result.putString("errDesc", "Unexpected error: " + throwable.getMessage());
                callback.handle(result);
                return;
            }
        });
        if (data != null) {
            request.putHeader("Content-Type", "application/json; charset=utf-8");
            logger.info("MIS request data " + data);
            request.end(new Buffer(data, "UTF-8"));
        } else {
            request.end();
        }
    }

    private void doPostConnector(String path, String data, final Handler<JsonObject> callback) {
        final Logger logger = container.logger();
        logger.info("I post connector path " + path);
        final long startTime = System.currentTimeMillis();
        final JsonObject result = new JsonObject();

        HttpClientRequest request = httpClientConnector.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {

                if (response.statusCode() != 200) {
                    result.putBoolean("result", false);
                    result.putNumber("errCode", 10000 + response.statusCode());
                    result.putString("errDesc", "Send to connector error: statusCode=" + response.statusCode() + ", statusMessage=" + response.statusMessage());
                    callback.handle(result);
                    return;
                }

                response.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        result.putBoolean("result", true);
                        result.putObject("data", new JsonObject(buffer.toString()));
                        final long time = (System.currentTimeMillis() - startTime) / 1000;
                        logger.info("Connector response time " + time + "s data " + buffer.toString());
                        callback.handle(result);
                        return;
                    }
                });
                response.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        result.putBoolean("result", false);
                        result.putNumber("errCode", 9999);
                        result.putString("errDesc", "Hệ thống lỗi, không kết nối được máy chủ");
//                        result.putString("errDesc", "Unexpected error: " + throwable.getMessage());
                        callback.handle(result);
                        return;
                    }
                });
            }
        });
        request.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                result.putBoolean("result", false);
                result.putNumber("errCode", 9999);
                result.putString("errDesc", "Hệ thống lỗi, không kết nối được máy chủ.");
//                result.putString("errDesc", "Unexpected error: " + throwable.getMessage());
                callback.handle(result);
                return;
            }
        });
        if (data != null) {
            request.putHeader("Content-Type", "application/json; charset=utf-8");
            logger.info("Connector request data " + data);
            request.end(new Buffer(data, "UTF-8"));
        } else {
            request.end();
        }
    }

    private void checkPaymentCode(final Cryption cryption, final MerPayment paymentMsg, final HttpServerResponse response) {
        final Logger logger = container.logger();
        logger.info("Request of merchant [" + MomoJackJsonPack.objToString(paymentMsg) + "]");

        //check request
        if (Misc.isNullOrEmpty(paymentMsg.merchantId)) {
            paymentMsg.setErrorCode(MerErrorCode.NotExistsMerchant);
            logger.error("App miss merchantId");
            sendDataToClient(response, paymentMsg);
            return;
        }
        if (Misc.isNullOrEmpty(paymentMsg.paymentCode)) {
            paymentMsg.setErrorCode(MerErrorCode.NotExistsMerchant);
            logger.error("App miss paymentCode");
            sendDataToClient(response, paymentMsg);
            return;
        }

        logger.error(MerErrorCode.Success);
        paymentMsg.setErrorCode(MerErrorCode.Success);
        sendDataToClient(response, paymentMsg);
        return;

        /*merchantKeyManageDb.findOne(paymentMsg.merchantId, new Handler<MerchantKeyManageDb.Obj>() {
                @Override
                public void handle(MerchantKeyManageDb.Obj obj) {
                    if (obj != null) {
                        final MerchantKeyManageDb.Obj merchantObj = obj;
                        if(!Misc.isNullOrEmpty(merchantObj.dev_pri_key)) {
                            logger.error("Session Key merchant " + merchantObj.dev_pri_key);
                            String phonede = "";
                            try {
                                phonede = PaymentCodeServer.decodePhone(paymentMsg.paymentCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.error(MerErrorCode.PaymentCodeWrong);
                                paymentMsg.setErrorCode(MerErrorCode.PaymentCodeWrong);
                                sendDataToClient(response, paymentMsg);
                                return;
                            }
                            if(!Misc.isNullOrEmpty(phonede)){
                                phonesDb.getPhoneObjInfo(DataUtil.strToInt(phonede), new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(PhonesDb.Obj objPhone) {
                                        if(objPhone != null){
                                            logger.error(MerErrorCode.Success);
                                            paymentMsg.setErrorCode(MerErrorCode.Success);
                                            sendDataToClient(response, paymentMsg);
                                            return;
                                        }else{
                                            logger.error(MerErrorCode.PhoneNumberNotFound);
                                            paymentMsg.setErrorCode(MerErrorCode.PhoneNumberNotFound);
                                            sendDataToClient(response, paymentMsg);
                                            return;
                                        }
                                    }
                                });
                            }else{
                                logger.error(MerErrorCode.PaymentCodeWrong);
                                paymentMsg.setErrorCode(MerErrorCode.PaymentCodeWrong);
                                sendDataToClient(response, paymentMsg);
                                return;
                            }
                        }else{
                            paymentMsg.setErrorCode(MerErrorCode.SessionKeyNotExits);
                            logger.error("Merchant " +paymentMsg.merchantId + " sessionKey is null or empty");
                            sendDataToClient(response, paymentMsg);
                            return;
                        }
                    }else{
                        paymentMsg.setErrorCode(MerErrorCode.NotExistsMerchant);
                        logger.error(paymentMsg.errorDesc +" " + paymentMsg.merchantId);
                        sendDataToClient(response, paymentMsg);
                        return;
                    }
                }
            });*/

    }

    private void doPayment(final Cryption cryption, final MerPayment paymentMsg, final HttpServerResponse response) {
        final Logger logger = container.logger();
        logger.info("Start time [" + System.currentTimeMillis() + "]");
        final long startTime = System.currentTimeMillis();

        //check request
        if (Misc.isNullOrEmpty(paymentMsg.merchantId)) {
            paymentMsg.setErrorCode(MerErrorCode.NotExistsMerchant);
            logger.error("App miss merchantId");
            responsePayment(response, paymentMsg, "", "", 0);
            return;
        }
        if (Misc.isNullOrEmpty(paymentMsg.hash)) {
            paymentMsg.setErrorCode(MerErrorCode.HashIsNull);
            logger.error(paymentMsg.errorDesc);
            responsePayment(response, paymentMsg, "", "", 0);
            return;
        }
        if (Misc.isNullOrEmpty(paymentMsg.TID)) {
            paymentMsg.setErrorCode(MerErrorCode.WrongRequest);
            logger.error(paymentMsg.errorDesc);
            responsePayment(response, paymentMsg, "", "", 0);
            return;
        }
        if (paymentMsg.amount < MIN_AMOUNT || paymentMsg.amount > MAX_AMOUNT) {
            paymentMsg.setErrorCode(MerErrorCode.WrongAmount);
            logger.error(paymentMsg.errorDesc);
            responsePayment(response, paymentMsg, "", "", 0);
            return;
        }
        paymentMsg.oriAmount = paymentMsg.amount;
        //check merchent id
        final long now = System.currentTimeMillis();
        merchantKeyManageDb.findOne(paymentMsg.merchantId, new Handler<MerchantKeyManageDb.Obj>() {
            @Override
            public void handle(MerchantKeyManageDb.Obj obj) {
                if (obj != null) {
                    final MerchantKeyManageDb.Obj merchantObj = obj;
                    if (!Misc.isNullOrEmpty(merchantObj.dev_pri_key)) {
                        logger.error("Session Key merchant " + merchantObj.dev_pri_key);
                        try {
                            String decryptData = "";
                            try {
                                decryptData = cryption.decryptIPOS(merchantObj.dev_pri_key, paymentMsg.hash.trim());
                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.error(MerErrorCode.LoginAnotherDevice);
                                paymentMsg.setErrorCode(MerErrorCode.LoginAnotherDevice);
                                sendDataToClient(response, paymentMsg);
                                return;
                            }

                            if (Misc.isNullOrEmpty(decryptData)) {
                                logger.error(MerErrorCode.HashNull);
                                paymentMsg.setErrorCode(MerErrorCode.HashNull);
                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, "", 0);
                                return;
                            }
                            JsonObject hashData = new JsonObject(decryptData);
                            logger.info("Decrypt hash data: " + hashData.encodePrettily());
                            String merchantIdHash = hashData.getString("merchantId", "");
                            String TIDHash = hashData.getString("TID", "");
                            final long amountHash = hashData.getLong("amount", 0);
                            final String paymentCodeTmp = hashData.getString("paymentCode", "");
                            final String paymentCode = "MM".equalsIgnoreCase(paymentCodeTmp.substring(0, 2)) ? paymentCodeTmp.substring(2, paymentCodeTmp.length()) : paymentCodeTmp;
                            logger.info("paymentCode new " + paymentCode);
                            if (!paymentMsg.merchantId.equals(merchantIdHash) || !paymentMsg.TID.equals(TIDHash)
                                    || paymentMsg.oriAmount != amountHash || Misc.isNullOrEmpty(paymentCode)) {
                                logger.error(MerErrorCode.CompareHashWrong);
                                paymentMsg.setErrorCode(MerErrorCode.CompareHashWrong);
                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, "", 0);
                                return;
                            }

                            paymentMsg.paymentCode = paymentCode;
                            paymentMsg.TID = String.valueOf(now);
                            String phonede = "";
                            final boolean haveVersion = !"MM".equalsIgnoreCase(paymentCodeTmp.substring(0, 2)) ? true : false;
                            try {
                                phonede = PaymentCodeServer.decodePhone(paymentCode, haveVersion);
                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.error("Decrypt paymentCode: unexpected error ", e);
                                paymentMsg.setErrorCode(MerErrorCode.PaymentCodeWrong);
                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
                            }
                            if (phonede == null || phonede.equals("")) {
                                logger.error(MerErrorCode.DecryptPhoneFail);
                                paymentMsg.setErrorCode(MerErrorCode.DecryptPhoneFail);
                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
                                return;
                            }
                            paymentMsg.phone = phonede;
                            final int phone = DataUtil.strToInt(phonede);
                            logger.info("customerNumber " + phonede);

                            phonesDb.getPhoneObjInfo(phone, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(final PhonesDb.Obj phoneObj) {
                                    if (phoneObj != null && !"".equalsIgnoreCase(phoneObj.sessionKey)) {
                                        try {
                                            String sessionKey = phoneObj.sessionKey.replaceAll("-", "").trim();
                                            logger.info("sessionKey " + sessionKey);
                                            String secretKey = Base32.encode(sessionKey.getBytes());
                                            logger.info("secretKey " + secretKey);

                                            String passde;

                                            if (paymentCode.length() == 18) {
                                                passde = PaymentCodeServer.decodePassBase10(secretKey, paymentCode, haveVersion);
                                            } else {
                                                passde = PaymentCodeServer.decodePass(secretKey, paymentCode, haveVersion);
                                            }
                                            final String pass = passde;
                                            logger.info("Decrypt pass success " + passde);
                                            //todo remove in production
                                            hashPayMent.put(paymentCode, phone);
                                            logger.info("hashPayment paycode phone size " + paymentCode + " " + phone + " " + hashPayMent.size());
                                            ControlOnClickActivityDb.Obj controlObj = new ControlOnClickActivityDb.Obj();
                                            controlObj.key = paymentCode + phone;
                                            controlObj.number = "0" + phone;
                                            controlObj.service = "lotte";
                                            controlObj.program = "lotte";
                                            controlOnClickActivityDb.insert(controlObj, new Handler<Integer>() {
                                                @Override
                                                public void handle(Integer result) {
                                                    if (result == 0) {
                                                        if(!hashPayMent.containsKey(paymentCode))
                                                        {
                                                            logger.info("DUPLICATE REQUEST FROM APP HASHPAYMENT NOT CONTAIN " + paymentCode + "|" + phone);
                                                            return;
                                                        }
                                                        hashPayMent.remove(paymentCode);
                                                        doExePay(paymentMsg, response, null, merchantObj, phone, pass, now, 0);
                                                    } else {
                                                        logger.info("DUPLICATE REQUEST FROM APP " + paymentCode + "|" + phone);
                                                    }
                                                }
                                            });


                                            //logger.info("@@@ Decrypt result phone 0" + phone + " pin " + pass);
//                                            transDb.getTranByBillId(phone, paymentCode, new Handler<TranObj>() {
//                                                @Override
//                                                public void handle(final TranObj tranObj) {
//                                                    //trans not exist
//                                                    if (tranObj == null) {
//                                                        if(Misc.isNullOrEmpty(paymentMsg.voucherCode)){
//                                                            logger.error("Not promo code");
//                                                            doExePay(paymentMsg,response, null, merchantObj, phone, pass, now, 0);
//                                                        }else{
//                                                            lottePromoCodeDb.findByCode(paymentMsg.voucherCode, new Handler<LottePromoCodeDb.Obj>() {
//                                                                @Override
//                                                                public void handle(LottePromoCodeDb.Obj promoCodeObj) {
//                                                                    if(promoCodeObj == null){
//                                                                        paymentMsg.setErrorCode(MerErrorCode.PromoCodeNotExit);
//                                                                        logger.error(paymentMsg.errorDesc);
//                                                                        responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
//                                                                        return;
//                                                                    }else{
//                                                                        if(promoCodeObj.status == LottePromoCodeDb.USED){
//                                                                            paymentMsg.setErrorCode(MerErrorCode.PromoCodeUsed);
//                                                                            logger.error(paymentMsg.errorDesc);
//                                                                            responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
//                                                                            return;
//                                                                        }else if(promoCodeObj.status == LottePromoCodeDb.EXPIRE || promoCodeObj.endDate < System.currentTimeMillis()){
//                                                                            paymentMsg.setErrorCode(MerErrorCode.PromoCodeExpired);
//                                                                            logger.error(paymentMsg.errorDesc);
//                                                                            responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
//                                                                            return;
//                                                                        }else{
//                                                                            paymentMsg.amount = paymentMsg.amount - promoCodeObj.amount;
//                                                                            if(paymentMsg.amount <= 0){ // promo > amount
//                                                                                paymentMsg.amount = 0;
//                                                                                if(!Misc.isNullOrEmpty(paymentMsg.TID) && paymentMsg.TID.length() > 6){
//                                                                                    paymentMsg.idRef = paymentMsg.TID.substring(paymentMsg.TID.length() - 6); //TID of current millisecond
//                                                                                }
//                                                                                paymentMsg.setErrorCode(MerErrorCode.Success);
//                                                                                logger.error(paymentMsg.errorDesc);
//                                                                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, promoCodeObj.amount);
//                                                                                updatePromoCode(paymentMsg, promoCodeObj);
//                                                                                return;
//                                                                            }else{
//                                                                                doExePay(paymentMsg,response, promoCodeObj, merchantObj, phone, pass, now, promoCodeObj.amount);
//                                                                            }
//                                                                        }
//                                                                    }
//                                                                }
//                                                            });
//                                                        }
//                                                    }else{
//                                                        logger.error( "PaymentCode " + paymentCode + " had been used ");
//                                                        paymentMsg.setErrorCode(MerErrorCode.AlreadyPay);
//                                                        responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
//                                                    }
//                                                }
//                                            });

                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            logger.error("Decrypt paymentCode: unexpected error ", ex);
                                            paymentMsg.setErrorCode(MerErrorCode.DecryptPaymentCodeException);
                                            responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
                                        }
                                    } else {
                                        logger.error(MerErrorCode.PhoneNumberNotFound);
                                        paymentMsg.setErrorCode(MerErrorCode.PhoneNumberNotFound);
                                        responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentCode, 0);
                                        return;
                                    }
                                }
                            });


                        } catch (Exception e) {
                            logger.error("Hash wrong : unexpected error ", e);
                            paymentMsg.setErrorCode(MerErrorCode.HashWrongSessionKey);
                            responsePayment(response, paymentMsg, merchantObj.merchant_infos, "", 0);
                        }
                    } else {
                        paymentMsg.setErrorCode(MerErrorCode.SessionKeyNotExits);
                        logger.error("Merchant " + paymentMsg.merchantId + " sessionKey is null or empty");
                        responsePayment(response, paymentMsg, merchantObj.merchant_infos, "", 0);
                        return;
                    }

                } else {
                    paymentMsg.setErrorCode(MerErrorCode.NotExistsMerchant);
                    logger.error(paymentMsg.errorDesc + " " + paymentMsg.merchantId);
                    responsePayment(response, paymentMsg, "", "", 0);
                    return;
                }
            }
        });
    }

    private void sendNotiAndSMS(int result, int toUser, long amount, long tranId, long now, String storeName) {
        final Logger logger = container.logger();
        if (tranId == 0 || tranId == -1) {
            tranId = now;
        }
        String comment = "";
        String tpl = "";
        String caption = "";
        String sms = "";
        if (result == 0) {
            tpl = "Quý khách đã thanh toán thành công %s đ tại siêu thị %s.";
            caption = "Thanh toán thành công";
            String smsTmp = "Quy khach vua thanh toan thanh cong so tien %sd cho dich vu LotteMart. TID %s LH Ho tro MoMo 1900545441";
            sms = String.format(smsTmp
                    , Misc.formatAmount(amount).replace(",", ".")
                    , tranId);
            comment = String.format(tpl
                    , Misc.formatAmount(amount).replace(",", ".")
                    , storeName);
        } else {
            tpl = "Quý khách đã thanh toán không thành công %s đ tại siêu thị %s";
            caption = "Thanh toán không thành công";
            comment = String.format(tpl
                    , Misc.formatAmount(amount).replace(",", ".")
                    , storeName);
        }

        Notification noti = new Notification();
        noti.receiverNumber = toUser;
        noti.caption = caption;
        noti.body = comment;
        noti.sms = sms;
        noti.priority = 2;
        noti.time = System.currentTimeMillis();
        noti.tranId = tranId;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        logger.error("Send noti : " + comment);
        logger.error("Send SMS : " + sms);
        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_SDK_SERVER
                , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                logger.error("Send noti and SMS success");
            }
        });
        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_SMS
                , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                logger.error("Send noti and SMS success");
            }
        });
    }

    private void sendTranHis(int result, long tranId, String ownerNumber, long amount, String billId, long now, long promoAmount, long oriAmount) {
        if (tranId == 0 || tranId == -1) {
            tranId = now;
        }
        String tranRef = String.valueOf(tranId);
        if (tranRef.length() > 6) {
            tranRef = tranRef.substring(tranRef.length() - 6);
        }
        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, 7);
        if (result == 0) {
            if (promoAmount == 0) {
                jsonTrans.putString(colName.TranDBCols.COMMENT, "Quý khách đã thanh toán thành công hóa đơn Lotte Mart trị giá " + Misc.formatAmount(amount).replace(",", ".") + "đ\n Mã tham chiếu " + tranRef);
            } else {
                jsonTrans.putString(colName.TranDBCols.COMMENT, "Quý khách đã thanh toán thành công hóa đơn Lotte Mart trị giá " + Misc.formatAmount(oriAmount).replace(",", ".") + "đ với voucher mệnh giá " + Misc.formatAmount(promoAmount).replace(",", ".") + "đ\n Mã tham chiếu " + tranRef);
            }
            jsonTrans.putNumber(colName.TranDBCols.STATUS, 4);
        } else {
            jsonTrans.putString(colName.TranDBCols.COMMENT, "Quý khách đã thanh toán không thành công hóa đơn Lotte Mart trị giá " + Misc.formatAmount(amount).replace(",", ".") + "đ");
            jsonTrans.putNumber(colName.TranDBCols.STATUS, 5);
        }

        jsonTrans.putString(colName.TranDBCols.PARTNER_ID, SERVICE_ID.toLowerCase());
        jsonTrans.putString(colName.TranDBCols.PARTNER_NAME, SERVICE_NAME);
        jsonTrans.putString(colName.TranDBCols.BILL_ID, "");
        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amount);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(ownerNumber));
        jsonTrans.putNumber(colName.TranDBCols.IO, -1);
        Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTrans, new JsonObject());
    }

    private void responsePayment(HttpServerResponse response, final MerPayment paymentMsg, final String cashier, String paymentCode, long promoAmount) {
        final Logger logger = container.logger();
        final long now = System.currentTimeMillis();
        long trandId = Misc.isNullOrEmpty(paymentMsg.TID) ? now : Long.valueOf(paymentMsg.TID);
        paymentMsg.description = paymentMsg.errorDesc;
        int storeId;
        String storeName = "";
        if (!Misc.isNullOrEmpty(cashier)) {
            JsonObject cashierObj = new JsonObject(cashier);
            storeId = cashierObj.getObject("data").getObject("userProfile").getInteger("store_ID", 0);
            storeName = cashierObj.getObject("data").getObject("userProfile").getString("store_NAME", "");
        }

        sendDataToClient(response, paymentMsg);
        sendNotiAndSMS(paymentMsg.errorCode, DataUtil.strToInt(paymentMsg.phone), (long) paymentMsg.amount, trandId, now, storeName);
        sendTranHis(paymentMsg.errorCode, trandId, paymentMsg.phone, (long) paymentMsg.amount, paymentCode, now, promoAmount, (long) paymentMsg.oriAmount);
        doInsertTran(paymentMsg, cashier, now, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                if (event) {
                    logger.error("Insert TID " + paymentMsg.TID + " in MIS success ");
                } else {
                    logger.error("Insert TID " + paymentMsg.TID + " in MIS fail ");
                }
            }
        });
    }

    private void updatePromoCode(final MerPayment paymentMsg, LottePromoCodeDb.Obj lottePromo) {
        lottePromo.status = 2;
        JsonObject lotteObj = lottePromo.toJson();
        final Logger logger = container.logger();
        lottePromoCodeDb.upSert(paymentMsg.voucherCode, lotteObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                if (event) {
                    logger.error("Update promo code " + paymentMsg.voucherCode + " success ");
                } else {
                    logger.error("Update promo code " + paymentMsg.voucherCode + " fail ");
                }
            }
        });
    }

    /*private void insertPromoCode(final LottePromoCodeDb.Obj lottePromo){

        final Logger logger = container.logger();
        lottePromoCodeDb.insert( lottePromo, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {
                if(event == 1){
                    logger.error( "Insert promo code " + lottePromo.promotionCode + " success ");
                }else{
                    logger.error( "Insert promo code " + lottePromo.promotionCode + " fail ");
                }
            }
        });
    }*/

    private void doExePay(final MerPayment paymentMsg, final HttpServerResponse response, final LottePromoCodeDb.Obj promoCode,
                          final MerchantKeyManageDb.Obj merchantObj, final int phone, final String pass, final long now, final long promoAmount) {
        final Logger logger = container.logger();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(paymentMsg.phone);
        final SockData sockData = new SockData(vertx, logger, container.config());
        sockData.pin = pass;
        iPosCashLimitDb.findOne("0" + phone, new Handler<IPosCashLimitDb.Obj>() {
            @Override
            public void handle(IPosCashLimitDb.Obj cashLimitObj) {
                long cashLimit = cashLimitObj == null ? CASH_LIMIT : cashLimitObj.money_value;
                if (cashLimit >= paymentMsg.amount) {
                    TransferWithGiftContext.build(phone, SERVICE_ID, "", Math.round(Math.ceil(paymentMsg.amount)), vertx, giftManager, sockData, 0, 0, logger, new Handler<TransferWithGiftContext>() {
                        @Override
                        public void handle(final TransferWithGiftContext context) {
                            JsonObject proxy = getProxyRequestObj(paymentMsg, "0" + phone, pass, now, merchantObj, context);
                            String requestData = proxy.toString();
//                        RedisFactory.createBillpayPointVoucherRequest(proxyRequest);
                            doPostConnector("/proxyservice", requestData, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject responseData) {
                                    try {
                                        if (!responseData.getBoolean("result")) {
                                            paymentMsg.errorCode = responseData.getInteger("errCode");
                                            paymentMsg.errorDesc = responseData.getString("errDesc");
                                            responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentMsg.paymentCode, context.voucher);
                                            logger.error("Connnector response error: code=" + responseData.getInteger("errCode") + ", desc=" + responseData.getString("errDesc"));
                                            return;
                                        } else {
                                            logger.error("Connnector response success ");
                                            JsonObject objResponse = new JsonObject(responseData.getObject("data").toString());
                                            int resultCode = objResponse.getInteger("resultCode", 1006);
                                            String resultMessage = SoapError.CoreErrorMap_VN.get(resultCode);
                                            long tranId = objResponse.getObject("request").getLong("coreTransId", now);
                                            if (resultCode == 0) {
                                                paymentMsg.TID = String.valueOf(tranId); //get 6 number last for referen n
                                                if (!Misc.isNullOrEmpty(paymentMsg.TID) && paymentMsg.TID.length() > 6) {
                                                    paymentMsg.idRef = paymentMsg.TID.substring(paymentMsg.TID.length() - 6);
                                                }
                                                paymentMsg.setErrorCode(MerErrorCode.Success);
                                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentMsg.paymentCode, context.voucher);
                                                if (promoCode != null) {
                                                    updatePromoCode(paymentMsg, promoCode);
                                                }
                                                logger.info("End time [" + System.currentTimeMillis() + "]");
                                                context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject result) {
                                                        if (result.getInteger("error", -1000) != 0) {
                                                            log.add("use gift", "use gift error");
                                                            log.add("error", result.getInteger("error"));
                                                            log.add("desc", SoapError.getDesc(result.getInteger("error")));
                                                        }

                                                        //Kiem tra ket qua xoa trong queuedgift
//                                                            if (result.getInteger("error", -1000) == 0) {
//                                                                //BEGIN 0000000052 Iron MAN
//                                                                JsonArray giftArray = result.getArray(StringConstUtil.IronManPromo.GIFT_ID_ARRAY, new JsonArray());
//                                                                if (giftArray.size() > 0) {
//                                                                    updateIronManVoucher(msg, log, context, giftArray);
//                                                                    promotionProcess.updateOctoberPromoVoucherStatus(log, giftArray, "0" + msg.cmdPhone);
//                                                                }
//                                                                //END 0000000052 IRON MAN
//                                                            }
                                                        //lay phan du gift cua khach hang bo vao tui cua MService hehehehe
                                                        context.returnMomo(vertx, logger, GIFT_MOMO_AGENT, context.error, new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject result) {
                                                                if (result.getInteger("error", -1000) != 0) {
                                                                    logger.error("returnMomo error!");
                                                                }

                                                                if (context.error == 0) {
                                                                    //BEGIN 0000000015 Kiem tra luon tra khuyen mai visa master

//                                                            requestVisaPoint(msg, log, context, sock, _data);
                                                                    //END 0000000015 Kiem tra luon tra khuyen mai visa master

                                                                    //force to update current amount
                                                                }
                                                                giftTranDb.save(new GiftTran(context), null);
                                                            }
                                                        });
                                                        log.writeLog();
                                                    }
                                                });
                                                //todo check promotion
                                                promotionProcess.executePromotion(paymentMsg.paymentCode, MomoProto.TranHisV1.TranType.PAY_IPOS_BILL_VALUE, "", Integer.parseInt(paymentMsg.phone),
                                                        tranId, Math.round(Math.ceil(paymentMsg.amount)), sockData, "lotte", context, 0, 1, log, new JsonObject(), new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject event) {}});
                                            } else if (resultCode == 1014) {
                                                logger.error(resultCode);
                                                paymentMsg.setErrorCode(MerErrorCode.PaymentCodeExpire);
                                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentMsg.paymentCode, context.voucher);
                                                logger.info("End time [" + System.currentTimeMillis() + "]");
                                            } else {
                                                logger.error(resultCode);
                                                paymentMsg.errorCode = resultCode;
                                                paymentMsg.errorDesc = resultMessage;
                                                responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentMsg.paymentCode, context.voucher);
                                                logger.info("End time [" + System.currentTimeMillis() + "]");
                                            }
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        logger.error("Connector unexpected error ", ex);
                                        paymentMsg.errorCode = -1;
                                        paymentMsg.errorDesc = "Dữ liệu lỗi do kết nối";
                                        responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentMsg.paymentCode, context.voucher);
                                    }
                                }
                            });
                        }
                    });
                } else {
                    paymentMsg.setErrorCode(MerErrorCode.AmountOverquota);
                    logger.error(paymentMsg.errorDesc);
                    responsePayment(response, paymentMsg, merchantObj.merchant_infos, paymentMsg.paymentCode, 0);
                    return;
                }
            }
        });
    }

    private JsonObject getProxyRequestObj(MerPayment paymentMsg, String user, String pass, long now, MerchantKeyManageDb.Obj merchantObj, TransferWithGiftContext context) {

        JsonObject merchantJson = new JsonObject(merchantObj.merchant_infos);
        int staffId = merchantJson.getObject("data").getInteger("id", 0);

        JsonObject requestJson = new JsonObject();
        requestJson.putObject("extras", new JsonObject());
        requestJson.putString("requestId", Long.parseLong(String.format("%s%s", System.currentTimeMillis(), RandomStringUtils.randomNumeric(4))) + "");
        requestJson.putNumber("source", 2);
        requestJson.putNumber("type", 3);
        requestJson.putString("typeMessage", "payBill");

        requestJson.putString("reference1", "33021" + paddingStoreId(paymentMsg.storeId) + "+QR"); //StoreID+QR, vd: 001+QR
        requestJson.putString("reference2", paymentMsg.merchantId); // Mã nhân viên+Mã giao dịch thành công : mã tham chiếu chưa có do core sinh ở connnector
        requestJson.putString("debitor", user);
        requestJson.putString("debitorPin", pass);
        requestJson.putString("creditor", agentCreditor);
        requestJson.putNumber("form", 1);
        requestJson.putString("creditorName", paymentMsg.storeId);
        requestJson.putNumber("amount", paymentMsg.amount);
        requestJson.putNumber("unitPrice", paymentMsg.amount);
        requestJson.putNumber("voucher", context.voucher);
        requestJson.putString("serviceCode", SERVICE_ID);
        requestJson.putNumber("debitorWalletType", 1);
        requestJson.putNumber("quantity", 1);

        return requestJson;
    }

    private String paddingStoreId(String storeId) {
        String paddingStoreId = storeId;

        if (storeId.length() < 3) {
            paddingStoreId = paddingStoreId.length() == 2 ? "0" + storeId : "00" + storeId;
        }

        return paddingStoreId;
    }

    private void checkSessionUser(final Cryption cryption, final MerMsg merMsg, final Handler<Boolean> callback) {
        final Logger logger = container.logger();

        merchantKeyManageDb.findOne(merMsg.merchantId, new Handler<MerchantKeyManageDb.Obj>() {
            @Override
            public void handle(MerchantKeyManageDb.Obj obj) {
                if (obj != null) {
                    String decryptData = "";
                    try {
                        decryptData = cryption.decryptIPOS(obj.dev_pri_key, merMsg.sessionHash.trim());
                    } catch (Exception e) {
                        callback.handle(false);
                        logger.error(MerErrorCode.LoginAnotherDevice);
                        return;
                    }
                    if (Misc.isNullOrEmpty(decryptData)) {
                        callback.handle(false);
                    } else {
                        callback.handle(true);
                    }
                }
            }
        });
    }

    private boolean checkShowError(int errorCode) {
        boolean result = false;
        List<Integer> listError = Arrays.asList(25, 47, 73, 1008, 1011, 1020, 1024, 1033, 1034, 1035, 1042, 1043, 9999);
        for (Integer error : listError) {
            if (error == errorCode) {
                return true;
            }
        }
        return result;
    }

}
