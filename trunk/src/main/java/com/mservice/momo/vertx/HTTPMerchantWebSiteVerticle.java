package com.mservice.momo.vertx;

import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.MerchantOfflinePayment.IPOSTransactionInfoDb;
import com.mservice.momo.data.MerchantOfflinePayment.MerchantKeyManageDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.ipos.IPosCashLimitDb;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.security.CryptLib;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.permission.Cryption;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.QueuedGiftResult;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.*;
import com.mservice.momo.web.external.services.pay123.Controller123Pay;
import com.mservice.momo.web.internal.services.ControllerBankOutManual;
import com.mservice.momo.web.internal.services.ControllerVcbMapWallet;
import com.mservice.momo.web.internal.services.ControllerWarningRetailer;
import com.mservice.momo.web.internal.webadmin.handler.ControllerMapper;
import com.mservice.momo.web.internal.webadmin.handler.RenderHandler;
import com.mservice.payment.token.PaymentCodeClient;
import com.mservice.payment.token.PaymentCodeServer;
import com.mservice.security.otpas.Base32;
import org.apache.commons.lang3.RandomStringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.*;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.platform.Verticle;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by concu on 8/27/15.
 */
public class HTTPMerchantWebSiteVerticle extends Verticle {

    public static final String SERVICE_NAME = "ipos";
    public static final String SERVICE_NAME_GALAXY = "galaxySDK";
    public static final String SERVICE_NAME_PTI = "pti";

    //    private static final String MOMO_KEYSTORE_PATH = "localhost.jks";
    private static final String MOMO_KEYSTORE_PATH = "momokeystore.jks";
    private static final String MOMO_KEYSTORE_PASSWORD = "MomoApp";
    private static final String SEPERATE_POS = ";";
    private static int PORT = 18080;
    private static String HOST_ADDRESS = "1.1.1.1";
    private static int POS_TIMEOUT = 180000;
    private PhonesDb phonesDb;
    private String staticResourceDir;
    private ConnectProcess connectProcess;
    private MerchantKeyManageDb merchantKeyManageDb;
    private JsonObject merchantKeyManageObject;
    private boolean isTestEnvironment;
    private TransferProcess transferProcess;
    private TransProcess transProcess;
    private TransDb transDb;
    private TransferCommon transferCommon;
    private GiftManager giftManager;
    private GiftProcess giftProcess;
    private com.mservice.momo.vertx.processor.Common mCom;
    private MainDb mainDb;
    private IPosCashLimitDb iPosCashLimitDb;
    private long LIMIT_PAY_OFFLINE_NOT_NAME = 2000000;
    private long LIMIT_PAY_OFFLINE_MAX = 20000000;
    private long LIMIT_PAY_OFFLINE_MIN = 10000;
    private HttpClient httpClientGoit;
    private ConcurrentHashMap<String, String> concurrentHashMapCodeOTP;
    private IPOSTransactionInfoDb iposTransactionInfoDb;
    private ConcurrentHashMap<String, String> concurrentHashMapIpos;

    @Override
    public void start() {
        final Logger logger = container.logger();
        final JsonObject globalConfig = container.config();
        merchantKeyManageObject = globalConfig.getObject(StringConstUtil.MerchantKeyManage.JSON_OBJECT, new JsonObject());
        transferCommon = new TransferCommon(vertx, logger, globalConfig);
        this.PORT = merchantKeyManageObject.getInteger(StringConstUtil.MerchantKeyManage.PORT, 8082);
        this.HOST_ADDRESS = merchantKeyManageObject.getString(StringConstUtil.MerchantKeyManage.HOST, "1.1.1.1");
        this.POS_TIMEOUT = merchantKeyManageObject.getInteger(StringConstUtil.MerchantKeyManage.TIMEOUT, 180000);
        iPosCashLimitDb = new IPosCashLimitDb(vertx, logger);
        staticResourceDir = globalConfig.getObject("userResourceVerticle").getString("staticResourceDir", "/tmp");
        if (!staticResourceDir.endsWith("/"))
            staticResourceDir = staticResourceDir + "/";

        phonesDb = new PhonesDb(vertx.eventBus(), container.logger());
        mCom = new com.mservice.momo.vertx.processor.Common(vertx, logger, globalConfig);
        giftManager = new GiftManager(vertx, logger, globalConfig);
        giftProcess = new GiftProcess(mCom, vertx, logger, globalConfig);
        merchantKeyManageDb = new MerchantKeyManageDb(vertx, container.logger());
        connectProcess = new ConnectProcess(vertx, logger, globalConfig);
        transferProcess = new TransferProcess(vertx, logger, globalConfig, ServerVerticle.MapTranRunning);
        transProcess = new TransProcess(vertx, logger, globalConfig);
        final ConcurrentSharedMap<String, JsonObject> sessions = vertx.sharedData().getMap(AppConstant.WebAdminVerticle_WEB_ADMIN_SESSION_MAP);
        concurrentHashMapCodeOTP = new ConcurrentHashMap<>();
        final ControllerMapper controllerMapper = new ControllerMapper(vertx, container);

        controllerMapper.addController(new Controller123Pay(vertx, container));
        controllerMapper.addController(new ControllerBankOutManual(vertx, container));
        controllerMapper.addController(new ControllerVcbMapWallet(vertx, container));
        controllerMapper.addController(new ControllerWarningRetailer(vertx, container));

        final RenderHandler renderHandler = new RenderHandler(vertx, container);
        controllerMapper.setNextHandler(renderHandler);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        connectProcess = new ConnectProcess(vertx, container.logger(), globalConfig);
        isTestEnvironment = merchantKeyManageObject.getBoolean(StringConstUtil.MerchantKeyManage.IS_TEST_ENVIRONMENT, false);
        iposTransactionInfoDb = new IPOSTransactionInfoDb(vertx, logger);
        concurrentHashMapIpos = new ConcurrentHashMap<>();
        httpClientGoit = vertx.createHttpClient()
                .setHost("beta.gotit.vn")
                .setPort(443)
                .setMaxPoolSize(20)
                .setConnectTimeout(120000) // 2 phut
                .setKeepAlive(false)
                .setSSL(true)
                .setTrustAll(true)
                .setVerifyHost(false);

        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest request) {
                        logger.info("[WebService] [" + request.method() + "] " + " uri: " + request.uri() + " path: " + request.path());
                        final JsonObject joReply = new JsonObject();
                        final String path = request.path();
                        final Cryption cryption = new Cryption(globalConfig, logger);
                        for (int i = 0; i < request.headers().size(); i++) {
                            logger.info("key " + request.headers().entries().get(i).getKey() + " value " + request.headers().entries().get(i).getValue());
                        }
                        if (request.method().equalsIgnoreCase("POST")) {
                            if ("/paygamebill".equalsIgnoreCase(path) || "/paygamebill".equalsIgnoreCase(request.uri())) {
                                request.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(final Buffer buffer) {
                                        logger.info(buffer.getBytes());
                                        logger.info(buffer.toString());
                                        final Common.BuildLog log = new Common.BuildLog(logger);


                                        try {
                                            String bigData = buffer.toString().replaceAll("(\\r\\n|\\n)", "");
                                            logger.info(bigData);
                                            final String[] datas = buffer.toString().split(MomoMessage.BELL);
                                            logger.info(datas.length);
                                            if (datas.length > 4) {
                                                logger.info("Day du du lieu tu doi tac");
                                                //data[0] so dien thoai tham gia
                                                final String phone_number = datas[0].toString().trim();
                                                logger.info("phone_number is " + phone_number);
                                                log.setPhoneNumber(phone_number);
                                                //datas[4] merchant code
                                                final String merchant_code = datas[4].toString().trim();
                                                logger.info("Merchant code is " + merchant_code);
                                                //datas[1] data from app
                                                final String data_app = datas[1].toString().trim();
                                                //data[2] data from client
                                                final String data_client = datas[2].toString().trim();

                                                //data[3] ip
                                                String ip = datas[3].toString().trim();
                                                logger.info("IP is " + ip);
                                                logger.info("data_client " + data_client);
                                                //Lay thong tin key tu doi tac
                                                processPayMerchantBill(log, cryption, phone_number, merchant_code, data_app, data_client, logger, joReply, request);
                                            } //END data.length > 4
                                            else if (!"".equalsIgnoreCase(bigData) && Misc.isValidJsonObject(bigData)) {
                                                logger.info("Thong tin nhan tu app la json => di theo luong moi");
                                                JsonObject jsonReceive = new JsonObject(bigData);
                                                String phone_number = jsonReceive.getString(StringConstUtil.MerchantKeyManage.PHONENUMBER, "");
                                                String data_client = jsonReceive.getString(StringConstUtil.MerchantKeyManage.HASH, "").toString().trim().replaceAll("(\\r\\n|\\n)", "");
                                                String data_app = jsonReceive.getString(StringConstUtil.MerchantKeyManage.DATA, "").toString().trim().replaceAll("(\\r\\n|\\n)", "");
                                                logger.info("data app nhan tu doi tac " + data_app);
                                                data_app = data_app.split(MomoMessage.BELL).length > 1 ? data_app.split(MomoMessage.BELL)[1].toString().trim() : data_app;
                                                logger.info("data app nhan sau khi split BELL " + data_app);
                                                String merchant_code = jsonReceive.getString(StringConstUtil.MerchantKeyManage.MERCHANT_CODE, "");
                                                if ("".equalsIgnoreCase(phone_number) || "".equalsIgnoreCase(data_client) || "".equalsIgnoreCase(data_app) || "".equalsIgnoreCase(merchant_code)) {
                                                    logger.info("Loi du lieu gui sang thieu hoac sai format");
                                                    logger.info("Thong tin nhan tu app sai");
                                                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                                    request.response().end(joReply.toString());
                                                    return;
                                                } else {
                                                    logger.info("Thong tin gui qua day du");
                                                    processPayMerchantBill(log, cryption, phone_number, merchant_code, data_app, data_client, logger, joReply, request);
                                                }

                                            } else {
                                                logger.info("Loi du lieu gui sang thieu hoac sai format");
                                                logger.info("Thong tin nhan tu app sai");
                                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                                request.response().end(joReply.toString());
                                                return;
                                            }
                                        } catch (Exception e) {
//                                            e.printStackTrace();
                                            //Error tu app, response ve luon
                                            //JsonObject joReply = new JsonObject();
                                            logger.info("Thong tin nhan tu app sai");
                                            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                            request.response().end(joReply.toString());
                                            return;
                                        }
                                    }
                                });
                            } else if (path.equalsIgnoreCase("/register")) {
                                request.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        logger.info(buffer.getBytes());
                                        logger.info(buffer.toString());
                                        final Common.BuildLog log = new Common.BuildLog(logger);
                                        request.response().end("Hello nhan duoc roi");
                                        return;
                                    }
                                });
                            }
                            // de gia lap cho merchant test choi cho dzui ay ma
                            else if (path.equalsIgnoreCase("/testpayoffline")) {
                                logger.info("testpayoffline");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                testExecutePos(request, log, logger, cryption);
                            }
                            //thanh toan qua POS
                            else if (path.equalsIgnoreCase("/payoffline")) {
                                logger.info("payoffline");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                executePos(request, log, logger, cryption);
                            }
                            //Kiem tra thong tin IPOS
                            else if (path.equalsIgnoreCase("/checkPartnerInfo")) {
                                logger.info("/checkPartnerInfo");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                checkPartnerInfo(request, log, logger, cryption);
                            }
                            //gene code for POS
                            else if (path.equalsIgnoreCase("/createcode")) {
                                logger.info("createcode");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                createcode(request, log, logger, cryption);
                            } else if (path.equalsIgnoreCase("/cashlimit")) {
                                logger.info("cashlimit");
                                final Common.BuildLog log = new Common.BuildLog(logger);
                                getAndSetCashLimit(request, log, logger, cryption);
                            }
                            //test hash
                            else if (path.equalsIgnoreCase("/encrypthash")) {
                                logger.info("/encrypthash");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                encrypthash(request, log, logger, cryption);
                            }
                            //convert json for IOS
                            else if (path.equalsIgnoreCase("/cryptlib")) {
                                logger.info("cryptlib");
                                Common.BuildLog log = new Common.BuildLog(logger);
                                cryptlib(request, log, cryption);
                                //test cho gotit
                            } else if (path.equalsIgnoreCase("/testgotit")) {
                                logger.info("testgotit");
                                request.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(final Buffer buffer) {
                                        logger.info(buffer.getBytes());
                                        logger.info(buffer.toString());
                                        JsonObject obj = new JsonObject(buffer.toString());
                                        String billId = obj.getString("billId", "");
                                        String transId = obj.getString("billId", "");
                                        long amount = obj.getLong("amount", 0);
                                        Common.BuildLog log = new Common.BuildLog(logger);
                                        MerchantKeyManageDb.Obj merchantObj = new MerchantKeyManageDb.Obj();
                                        merchantObj.service_id = "gotit";

                                        sendNotify(logger, merchantObj, cryption, billId, transId, amount);
                                    }
                                });
                            } else {
                                logger.info("Sai duong dan, error path");
                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1008));
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1008);
                                request.response().end(joReply.toString());
                                return;
                            }
                        } else {
                            request.response().setStatusCode(403);
                            request.response().end();
//                            request.response().;
//                            request.response().end(new JsonObject()
//                            .putNumber("result", 403)
//                            .putString("description", "Forbidden").toString());
//                            request.response().end("got request");
//                            request.bodyHandler(new Handler<Buffer>() {
//                                @Override
//                                public void handle(Buffer postData) {
//                                    logger.info(postData.toString());
//                                    String cookieString = request.headers().get("Cookie");
//                                    if (cookieString == null || cookieString.trim().length() == 0) {
//                                        cookieString = "{}";
//                                    }
//                                    Cookie cookie;
//                                    try {
//                                        cookie = new Cookie(cookieString);
//                                    } catch (DecodeException e) {
//                                        cookie = new Cookie("{}");
//                                    }
//
//                                    Session session = null;
//                                    String sessionId = cookie.getString("sessionId");
//                                    if (sessionId != null) {
//                                        sessionId = sessionId.trim();
//                                        if (sessionId.length() > 0) {
//                                            JsonObject sessionJsonObject = sessions.get(sessionId);
//                                            if (sessionJsonObject != null) {
//                                                session = new Session(sessionId, sessionJsonObject);
//                                            }
//                                        }
//                                    }
//                                    if (session == null) {
//                                        sessionId = UUID.randomUUID().toString();
//                                        session = new Session(sessionId, "{}");
//                                    }
//
//                                    HttpRequestContext context = new HttpRequestContext(request, cookie, session);
//                                    context.setPostParams(postData.toString());
//
//                                    //Handling request
//                                    controllerMapper.handle(context);
//                                }
//                            });
                        }
                    }
                }).listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
            @Override
            public void handle(AsyncResult<HttpServer> event) {
                if (event.succeeded()) {
                    logger.info("HTTPMerchantWebSiteVerticle's listening on " + HOST_ADDRESS + ":" + PORT);
                }
            }
        });
    }

    private void processPayMerchantBill(final Common.BuildLog log, final Cryption cryption, final String phone_number, final String merchant_code, final String data_app, final String data_client, final Logger logger, final JsonObject joReply, final HttpServerRequest request) {
        merchantKeyManageDb.findOne(merchant_code, new Handler<MerchantKeyManageDb.Obj>() {
            @Override
            public void handle(final MerchantKeyManageDb.Obj merchantObj) {

                if (merchantObj == null) {
                    // Tra ket qua ve cho app
                    logger.info("Khong co thong tin cua merchant " + merchant_code);
                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Khong co thong tin cua nha cung cap");
                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, -1);
                    request.response().end(joReply.toString());
                    return;
                }

                //Truong hop co thong tin merchant
                //Kiem tra xem moi truong test hay product
                String pub_key = "";
                String pri_key = "";
                if (isTestEnvironment) {
                    pub_key = merchantObj.dev_pub_key;
                    pri_key = merchantObj.dev_pri_key;
                } else {
                    pub_key = merchantObj.pro_pub_key;
                    pri_key = merchantObj.pro_pri_key;
                }

                //Co thong tin key, bat dau ma hoa =))
                //Xem xet du lieu ma hoa dung yeu cau khong da
                try {
                    boolean result = cryption.resultVerifyData(data_client, pub_key, pri_key);
                    if (!result) {
                        //Du lieu khong dung, tra loi tiep
                        logger.info("Loi verify data is false");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Thong tin gui qua loi");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, -1);
                        request.response().end("got data " + joReply);
                        return;
                    }

                    logger.info("NGON, verify data thanh cong");

                    logger.info("Giai ma data client");
                    final String dataClient = cryption.decryptIOS(data_client.trim(), pri_key.trim());
                    logger.info(dataClient);
                    logger.info("decrypt thanh cong");

                    //Du lieu OK, ta di tiep
                    //GIai ma du lieu doi tuong de lay thong tin so dien thoai
//                                                            String data_client_decrypted = cryption.decrypt(data_client, pri_key);
//                                                            logger.info("Du lieu tu doi tac dua sang " + data_client_decrypted);
//
//                                                            JsonObject json_data_client_decrypted = new JsonObject(data_client_decrypted);

                    //Lay du lieu ben app gui qua lam viec nao
                    phonesDb.getPhoneObjInfo(DataUtil.strToInt(phone_number), new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(final PhonesDb.Obj phoneObj) {
                            if (phoneObj != null && !"".equalsIgnoreCase(phoneObj.sessionKey)) {
                                try {
                                    String sessionKey_tmp = phoneObj.sessionKey.replaceAll("\\-", "");
                                    String[] dataAppArr = data_app.split(MomoMessage.BELL);
                                    final String dataApp_tmp = dataAppArr.length > 1 ? dataAppArr[1] : dataAppArr[0];
                                    CryptLib cryptLib = new CryptLib();
                                    String iv = CryptLib.generateRandomIV(0); //16 bytes = 128 bit
                                    String dataApp = "";
                                    try {
                                        logger.info("DATA APP 1" + dataApp);
                                        logger.info("sessionKey_tmp " + sessionKey_tmp);
                                        dataApp = cryptLib.decrypt(dataApp_tmp, sessionKey_tmp, iv);
                                    } catch (Exception e) {
                                        log.add("error ", e.getMessage());
                                        log.writeLog();
                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Phiên làm việc hết hiệu lực");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1008);
                                        request.response().end(joReply.toString());
                                        return;
                                    }

                                    String[] listDataApp = dataApp.split(MomoMessage.BELL);
                                    dataApp = listDataApp.length > 1 ? listDataApp[listDataApp.length - 1] : dataApp;
//                                    try{
//                                        dataApp = cryptLib.decrypt(dataApp_tmp, sessionKey_tmp, iv);
//                                    }
//                                    catch (Exception ex)
//                                    {
//                                        dataApp = cryption.decryptSessionKeyData(dataApp_tmp, sessionKey_tmp);
//                                    }
                                    logger.info("DATA APP 2" + dataApp);
                                    if ("".equalsIgnoreCase(dataApp) || !Misc.isValidJsonObject(dataApp)) {
                                        log.add("sskey", sessionKey_tmp);
                                        log.add("desc", "loi decrypt");
                                        log.add("desc", "m2Merchant fail");
                                        log.writeLog();
                                        joReply.putNumber("status", -2);
                                        joReply.putString("message", "Nhan yeu cau thanh cong nhung co error " + -2);
                                        request.response().end(joReply.toString());
                                        return;
                                    }
                                    final JsonObject jsonDataApp = new JsonObject(dataApp);
                                    //Du lieu sau khi decrypted.
                                    String merchantCode = jsonDataApp.getString(StringConstUtil.MerchantKeyManage.MERCHANT_CODE, "");
                                    String merchantName = jsonDataApp.getString(StringConstUtil.MerchantKeyManage.MERCHANT_NAME, "");
                                    final String desc = jsonDataApp.getString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "");
                                    final long amount = jsonDataApp.getLong(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
                                    String[] sourceFromDatas = jsonDataApp.getString(StringConstUtil.MerchantKeyManage.SOURCE_FROM, "0").split(MomoMessage.BELL);
                                    int sourceFrom = DataUtil.strToInt(sourceFromDatas[0]);
                                    final String pin = jsonDataApp.getString(StringConstUtil.MerchantKeyManage.PIN, "");
                                    final String userName = jsonDataApp.getString(StringConstUtil.MerchantKeyManage.USER_NAME, "");
                                    log.add("merchantCode", merchantCode);
                                    log.add("merchantName", merchantName);
                                    log.add("desc", desc);
                                    log.add("amount", amount);
                                    log.add("sourceFrom", sourceFrom);
                                    log.add("pin", pin);
                                    log.add("userName", userName);
                                    log.add("SOURCE_FROM", jsonDataApp.getString(StringConstUtil.MerchantKeyManage.SOURCE_FROM, ""));
                                    if (merchant_code.equalsIgnoreCase(merchantCode) && sourceFrom == MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE) {
                                        //processM2Merchant(phoneObj, desc, amount, pin, logger, phone_number, merchantObj, log, joReply, request);
                                        TransferCommon.BankLinkedObj bankLinkedObj = new TransferCommon.BankLinkedObj();
                                        bankLinkedObj.bank_code = phoneObj.bank_code;
                                        bankLinkedObj.amount = amount;
                                        bankLinkedObj.from_bank = phoneObj.bank_name;
                                        SockData sockData = new SockData(vertx, logger, container.config());
                                        sockData.pin = pin;
                                        sockData.setPhoneObj(phoneObj, logger, "");
                                        transferCommon.doBankInViaSDK(phone_number, sockData, bankLinkedObj, new Handler<FromSource.Obj>() {
                                            @Override
                                            public void handle(FromSource.Obj fromSourceObj) {
                                                if (fromSourceObj.Result) {
                                                    processPayMerchantBill(phoneObj, userName, desc, amount, pin, logger, phone_number, merchantObj, log, joReply, request, dataClient, cryption);
                                                } else {
                                                    joReply.putNumber("status", -3);
                                                    joReply.putString("message", "Nạp tiền từ ngân hàng không thành công. Xin vui lòng thực hiện lại sau.");
                                                    request.response().end(joReply.toString());
                                                    return;
                                                }
                                            }
                                        });
                                        return;
                                    } else if (merchant_code.equalsIgnoreCase(merchantCode) && sourceFrom == MomoProto.TranHisV1.SourceFrom.MOMO_VALUE) {
                                        //processM2Merchant(phoneObj, desc, amount, pin, logger, phone_number, merchantObj, log, joReply, request);
                                        processPayMerchantBill(phoneObj, userName, desc, amount, pin, logger, phone_number, merchantObj, log, joReply, request, dataClient, cryption);
                                        return;
                                    } else {
                                        log.writeLog();
                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        request.response().end(joReply.toString());
                                        return;
                                    }
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                    log.add("error ", e.getMessage());
                                    log.writeLog();
                                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                    request.response().end(joReply.toString());
                                    return;
                                } catch (NoSuchPaddingException e) {
                                    e.printStackTrace();
                                    log.add("error ", e.getMessage());
                                    log.writeLog();
                                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                    request.response().end(joReply.toString());
                                    return;
                                }
                            } else {
                                logger.info("User not momo");
                                log.writeLog();
                                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Không có thông tin khách hàng");
                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 5);
                                request.response().end(joReply.toString());
                                return;
                            }
                        }
                    });

                } catch (Exception e) {
                    logger.info("Loi verify data");
                    logger.info("Thong tin nhan tu app sai");
                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                    request.response().end(joReply.toString());
                    e.printStackTrace();
                    return;
                }
            }
        });
    }

    private void executePos(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    final JsonObject joReply = new JsonObject();
                    final JsonObject messageReply = new JsonObject();
                    log.add("data byte", buffer.getBytes());
                    log.add("data string", buffer.toString());
                    final long now = System.currentTimeMillis();
                    JsonObject joReceive;
                    final HttpServerResponse httpServerResponse = request.response().putHeader("content-type", "application/json");
                    try {
                        joReceive = new JsonObject(buffer.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.info("Thông tin trong hash sai tham số: " + e.getMessage());
                        log.writeLog();
                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Thông tin sai tham số");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 163);
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                        httpServerResponse.end(joReply.toString());
                        return;
                    }

                    final String merchantId = joReceive.getString(StringConstUtil.MerchantKeyManage.MERCHANT_ID, "");
                    final String TID = joReceive.getString(StringConstUtil.MerchantKeyManage.TID, "");
                    final String voucherCode = joReceive.getString("voucherCode", "");
                    final long amount = joReceive.getLong(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
                    final String hash = joReceive.getString(StringConstUtil.MerchantKeyManage.HASH, "");
                    final String description = joReceive.getString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "");
                    log.setPhoneNumber(merchantId);

                    if ("".equalsIgnoreCase(merchantId) || "".equalsIgnoreCase(TID) || amount <= 0 ||
                            "".equalsIgnoreCase(hash)) {
                        log.add("desc", "Yêu cầu sai tham số");
                        log.writeLog();
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 150);
                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Yêu cầu sai tham số");
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                        httpServerResponse.end(joReply.toString());
                        return;
                    } else {

                        merchantKeyManageDb.findOne(merchantId, new Handler<MerchantKeyManageDb.Obj>() {
                            @Override
                            public void handle(final MerchantKeyManageDb.Obj merchantObj) {

                                if (merchantObj == null) {
                                    // Tra ket qua ve cho app
                                    log.add("desc", "Nhà cung cấp không tồn tại " + merchantId);
                                    log.writeLog();
                                    messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Nhà cung cấp không tồn tại");
                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 152);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                    joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                    httpServerResponse.end(joReply.toString());
                                    return;
                                }

                                try {
                                    String pub_key = "";
                                    String pri_key = "";
                                    if (isTestEnvironment) {
                                        pub_key = merchantObj.dev_pub_key;
                                        pri_key = merchantObj.dev_pri_key;
                                    } else {
                                        pub_key = merchantObj.pro_pub_key;
                                        pri_key = merchantObj.pro_pri_key;
                                    }
                                    // todo create key for test
                                                    /*String testString = "{\"Customer_number\":\"" + customerNumber + "\",\"Token\":\"" + token + "\",\"Amount\": " + amount + "}";
                                                    byte[] testByte = testString.getBytes();
                                                    final String testEn = cryption.encrypt(testByte, pub_key);
                                                    log.add("key hash=", testEn);*/
                                    //todo dung hash sau khi test
                                    boolean result = cryption.resultVerifyData(hash, pub_key, pri_key);
                                    //boolean result = cryption.resultVerifyData(testEn, pub_key, pri_key);
                                    if (!result) {
                                        //Du lieu khong dung, tra loi tiep
                                        logger.info("Giải mã hash thất bại");
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Giải mã hash thất bại");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }

                                    logger.info("Public key oki");
                                    // todo dung hash sau khi test
                                    String dataDecrypt = "";
                                    try {
                                        dataDecrypt = cryption.decrypt(hash.trim(), pri_key.trim());
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        logger.info("Giải mã hash thất bại: " + ex.getMessage());
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Giải mã hash thất bại");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }

                                    //final String dataDecrypt = cryption.decrypt(testEn.trim(), pri_key.trim());
                                    logger.info(dataDecrypt);
                                    logger.info("Giải mã hash thành công");
                                    if (dataDecrypt == null || "".equals(dataDecrypt)) {
                                        // Tra ket qua ve cho app
                                        log.add("desc", "hash is null" + merchantId);
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Hash không có thông tin");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 154);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }

                                    String merchantIdHash;
                                    final String TIDHash;
                                    long amountHash;
                                    String paymentCodeHash;
                                    try {
                                        JsonObject joHash = new JsonObject(dataDecrypt.toString());
                                        merchantIdHash = joHash.getString(StringConstUtil.MerchantKeyManage.MERCHANT_ID, "");
                                        TIDHash = joHash.getString(StringConstUtil.MerchantKeyManage.TID, "");
                                        amountHash = joHash.getLong(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
                                        paymentCodeHash = joHash.getString("paymentCode", "");
                                        log.add("merchantIdHash", merchantIdHash);
                                        log.add("TIDHash", TIDHash);
                                        log.add("amountHash", amountHash);
                                        log.add("paymentCode", paymentCodeHash);

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        logger.info("Thông tin trong hash sai tham số: " + ex.getMessage());
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Thông tin trong hash sai tham số");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 155);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }
                                    if (!merchantIdHash.equals(merchantId) || !TIDHash.equals(TID) || amountHash != amount || "".equals(paymentCodeHash)) {
                                        // check hash
                                        log.add("desc", "hash wrong with param request from m " + merchantId);
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Thông tin trong hash sai tham số");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 155);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }
                                    final String paymentCodeTmp = paymentCodeHash.replace(" ", "").trim();
                                    final String paymentCode = "MM".equalsIgnoreCase(paymentCodeTmp.substring(0, 2)) ? paymentCodeTmp.substring(2, paymentCodeTmp.length()) : paymentCodeTmp;
                                    logger.info("payment Code New IPOS " + paymentCode);
                                    concurrentHashMapIpos.put(paymentCode, TIDHash);
                                    //BEGIN DECRYPT PHONE AND PASS
                                    iposTransactionInfoDb.findOne(paymentCode, new Handler<IPOSTransactionInfoDb.Obj>() {
                                                @Override
                                                public void handle(IPOSTransactionInfoDb.Obj iposTransactionInfoObj) {
                                                    String[] decryptedDataBefore = new String[]{};
                                                    if (iposTransactionInfoObj != null && !"".equalsIgnoreCase(iposTransactionInfoObj.decrypted_payment_code)) {
                                                        decryptedDataBefore = iposTransactionInfoObj.decrypted_payment_code.split("_");
                                                    }
                                                    final String phoneDecryptBefore = decryptedDataBefore.length > 1 ? decryptedDataBefore[0] : "";
                                                    final String passDecryptBefore = decryptedDataBefore.length > 1 ? decryptedDataBefore[1] : "";
                                                    String phoneHash = "";
                                                    final boolean haveVersion = !"MM".equalsIgnoreCase(paymentCodeTmp.substring(0, 2)) ? true : false;
                                                    try {
                                                        phoneHash = "".equalsIgnoreCase(phoneDecryptBefore) ? PaymentCodeServer.decodePhone(paymentCode, haveVersion) : phoneDecryptBefore;
                                                        if (phoneHash == null || phoneHash.equals("")) {
                                                            logger.info("Giải mã phone thất bại");
                                                            log.writeLog();
                                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Giải mã phone thất bại");
                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                            httpServerResponse.end("got data " + joReply);
                                                            return;
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        logger.info("Mã thanh toán không hợp lệ. Vui lòng tạo mã thanh toán mới để thực hiện lại giao dịch.");
                                                        log.writeLog();
                                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Mã thanh toán không hợp lệ. Vui lòng tạo mã thanh toán mới để thực hiện lại giao dịch.");
                                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 162);
                                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                        httpServerResponse.end(joReply.toString());
                                                        return;
                                                    }

                                                    final String phonede = phoneHash;
                                                    final int phone = DataUtil.strToInt(phonede);
                                                    log.add("customerNumber", phone);
                                                    //Lay du lieu ben app gui qua lam viec nao

                                                    phonesDb.getPhoneObjInfo(phone, new Handler<PhonesDb.Obj>() {
                                                        @Override
                                                        public void handle(final PhonesDb.Obj phoneObj) {
                                                            if (phoneObj != null && !"".equalsIgnoreCase(phoneObj.sessionKey)) {

                                                                iPosCashLimitDb.findOne(phonede, new Handler<IPosCashLimitDb.Obj>() {
                                                                    @Override
                                                                    public void handle(IPosCashLimitDb.Obj cashLimitObj) {
                                                                    /*long cashLimit = 0;
                                                                    if(phoneObj.isNamed){
                                                                        cashLimit = cashLimitObj == null ? LIMIT_PAY_OFFLINE : cashLimitObj.money_value;
                                                                    }else{
                                                                        cashLimit = cashLimitObj == null ? LIMIT_PAY_OFFLINE_NOT_NAME : cashLimitObj.money_value;
                                                                    }*/
                                                                        long cashLimit = cashLimitObj == null ? LIMIT_PAY_OFFLINE_NOT_NAME : cashLimitObj.money_value;
                                                                        if (cashLimit < amount) {
                                                                            log.add("desc", "Vuong han muc roi " + cashLimit + " < " + amount);
                                                                            log.writeLog();
                                                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Vướng hạn mức, hạn mức đang là " + Misc.formatAmount(cashLimit).replace(",", ".") + "đ không thể thanh toán hóa đơn này.");
                                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 161);
                                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                            httpServerResponse.end(joReply.toString());
                                                                            toSendNotiAndSMS(161, phone, amount, -1, merchantObj.merchant_name, now);
                                                                            sendTranHis(161, -1, phonede, amount, paymentCode, merchantObj, now, description);
                                                                            return;
                                                                        }
                                                                        if (amount < LIMIT_PAY_OFFLINE_MIN || amount > LIMIT_PAY_OFFLINE_MAX) {
                                                                            log.add("desc", "Yêu cầu sai số tiền");
                                                                            log.writeLog();
                                                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Yêu cầu sai số tiền");
                                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 151);
                                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                            httpServerResponse.end(joReply.toString());
                                                                            toSendNotiAndSMS(151, phone, amount, -1, merchantObj.merchant_name, now);
                                                                            sendTranHis(151, -1, phonede, amount, paymentCode, merchantObj, now, description);
                                                                            return;
                                                                        }
                                                                        try {

                                                                            String sessionKey = phoneObj.sessionKey.replaceAll("-", "").trim();
                                                                            //todo test encrypt data from app
                                                                            //String tokenTest = encryptDataApp(sessionKey, phoneObj.pin, log);
                                                                            //log.add("tokenTest", tokenTest);
                                                                            log.add("sessionKey", sessionKey);
                                                                            String secretKey = Base32.encode(sessionKey.getBytes());
                                                                            log.add("secretKey", secretKey);
                                                                            //SERVER dung tu day
                                                                            String passde;

                                                                            if (paymentCode.length() == 18) {
                                                                                passde = "".equalsIgnoreCase(passDecryptBefore) ?
                                                                                        PaymentCodeServer.decodePassBase10(secretKey, paymentCode, haveVersion) : passDecryptBefore;
                                                                            } else {
                                                                                passde = "".equalsIgnoreCase(passDecryptBefore) ?
                                                                                        PaymentCodeServer.decodePass(secretKey, paymentCode, haveVersion) : passDecryptBefore;
                                                                            }
                                                                            final String pass = passde;

                                                                            if (concurrentHashMapIpos.remove(paymentCode, TIDHash)) {
                                                                                //Du lieu sau khi decrypted.

                                                                                int sourceFrom = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                                                                final String userName = phoneObj.name;

                                                                                log.add("sourceFrom", sourceFrom);
                                                                                log.add("userName", userName);

                                                                                SockData sockData = new SockData(vertx, logger, container.config());
                                                                                sockData.setPhoneObj(phoneObj, logger, "");
                                                                                sockData.pin = pass;
                                                                                sockData.setNumber(phone);

                                                                                JsonObject jsonDataClient = new JsonObject();
                                                                                jsonDataClient.putString(StringConstUtil.MerchantKeyManage.USER_NAME, merchantId);
                                                                                jsonDataClient.putString(StringConstUtil.MerchantKeyManage.BILL_ID, paymentCode);
                                                                                jsonDataClient.putString(StringConstUtil.MerchantKeyManage.TRANSID, TID);

                                                                                if (sourceFrom == MomoProto.TranHisV1.SourceFrom.MOMO_VALUE) {
                                                                                    processIPOSTransfer(logger, pass, sockData, TID, merchantObj, merchantObj.service_id, amount, merchantObj.agent_name, "0" + phone, description, phoneObj, jsonDataClient, new Handler<JsonObject>() {
                                                                                        @Override
                                                                                        public void handle(JsonObject jObjFromSoap) {
//
                                                                                            int error = jObjFromSoap.getInteger("status", -1);
                                                                                            long tranId = jObjFromSoap.getLong("transid", now);
                                                                                            if (error == 0) {
                                                                                                log.add("desc", "m2Merchant success");
                                                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                                                                                                messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, SoapError.CoreErrorMap_VN.get(0));
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.VOUCHER, joReply.getLong(StringConstUtil.MerchantKeyManage.VOUCHER, 0));
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.MOMO, joReply.getLong(StringConstUtil.MerchantKeyManage.MOMO, 0));
                                                                                                messageReply.putString(StringConstUtil.MerchantKeyManage.CUSTOMER_NUMBER_PARTNER, joReply.getString(StringConstUtil.MerchantKeyManage.CUSTOMER_NUMBER_PARTNER, ""));
                                                                                                joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                                            } else if (error == 100 || error == 1014) {
                                                                                                log.add("desc", "m2Merchant error " + error);
                                                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, error);
                                                                                                messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Phiên làm việc hết hiệu lực");
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                                                joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                                            } else {
                                                                                                log.add("desc", "m2Merchant error " + error);
                                                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, error);
                                                                                                messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, SoapError.CoreErrorMap_VN.get(error));
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tranId);
                                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                                                joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                                            }
                                                                                            try {
                                                                                                log.add("response payoffline", joReply.toString());
                                                                                                httpServerResponse.end(joReply.toString());
                                                                                                toSendNotiAndSMS(error, phone, amount, tranId, merchantObj.merchant_name, now);
                                                                                                sendTranHis(error, tranId, phonede, amount, paymentCode, merchantObj, now, description);
                                                                                            } catch (Exception ex) {
                                                                                                ex.printStackTrace();
                                                                                                log.add("exception ne", ex.toString());
                                                                                            } finally {
                                                                                                log.add("finish time payoffline milisecond", System.currentTimeMillis() - now);
                                                                                                log.writeLog();
                                                                                                return;
                                                                                            }
                                                                                        }
                                                                                    });
                                                                                } else {
                                                                                    log.writeLog();
                                                                                    messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Sai nguồn tiền");
                                                                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 156);
                                                                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                                    joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                                    httpServerResponse.end(joReply.toString());
                                                                                    return;
                                                                                }
                                                                                // token da duoc thanh toan
                                                                            } else {
                                                                                log.writeLog();
                                                                                messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Token đã được sử dụng");
                                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 157);
                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                                joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                                httpServerResponse.end(joReply.toString());
                                                                                toSendNotiAndSMS(157, phone, amount, -1, merchantObj.merchant_name, now);
                                                                                sendTranHis(157, -1, phonede, amount, paymentCode, merchantObj, now, description);
                                                                                return;
                                                                            }
                                                                        } catch (Exception e) {
                                                                            e.printStackTrace();
                                                                            logger.info("Exception: " + e.getMessage());
                                                                            log.writeLog();
                                                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Lỗi do exception");
                                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 159);
                                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                            httpServerResponse.end(joReply.toString());
                                                                            return;
                                                                        }
                                                                    }
                                                                });
                                                            } else {
                                                                logger.info("Info phone not exist");
                                                                log.writeLog();
                                                                messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Không có thông tin khách hàng");
                                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 158);
                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                httpServerResponse.end(joReply.toString());
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                    );
                                } catch (Exception e) {
                                    logger.info("Exception: " + e.getMessage());
                                    log.writeLog();
                                    messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Lỗi do exception");
                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 159);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                    joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                    httpServerResponse.end(joReply.toString());
                                    e.printStackTrace();
                                    return;
                                }
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.writeLog();
                    request.response().end("Get request but error");
                }

            }
        });

    }

    private void checkPartnerInfo(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    final JsonObject joReply = new JsonObject();
                    final JsonObject messageReply = new JsonObject();
                    log.add("data byte", buffer.getBytes());
                    log.add("data string", buffer.toString());
                    final long now = System.currentTimeMillis();
                    JsonObject joReceive;
                    final HttpServerResponse httpServerResponse = request.response().putHeader("content-type", "application/json");
                    try {
                        joReceive = new JsonObject(buffer.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.info("Thông tin trong hash sai tham số: " + e.getMessage());
                        log.writeLog();
                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Thông tin sai tham số");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 163);
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                        httpServerResponse.end(joReply.toString());
                        return;
                    }

                    final String merchantId = joReceive.getString(StringConstUtil.MerchantKeyManage.MERCHANT_ID, "");
                    final String TID = joReceive.getString(StringConstUtil.MerchantKeyManage.TID, "");
                    final long amount = joReceive.getLong(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
                    final String hash = joReceive.getString(StringConstUtil.MerchantKeyManage.HASH, "");
                    final String description = joReceive.getString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "");
                    log.setPhoneNumber(merchantId);

                    if ("".equalsIgnoreCase(merchantId) || "".equalsIgnoreCase(TID) || amount <= 0 ||
                            "".equalsIgnoreCase(hash)) {
                        log.add("desc", "Yêu cầu sai tham số");
                        log.writeLog();
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 150);
                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Yêu cầu sai tham số");
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                        httpServerResponse.end(joReply.toString());
                        return;
                    } else {
                        merchantKeyManageDb.findOne(merchantId, new Handler<MerchantKeyManageDb.Obj>() {
                            @Override
                            public void handle(final MerchantKeyManageDb.Obj merchantObj) {
                                if (merchantObj == null) {
                                    // Tra ket qua ve cho app
                                    log.add("desc", "Nhà cung cấp không tồn tại " + merchantId);
                                    log.writeLog();
                                    messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Nhà cung cấp không tồn tại");
                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 152);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                    joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                    httpServerResponse.end(joReply.toString());
                                    return;
                                }

                                try {
                                    String pub_key = "";
                                    String pri_key = "";
                                    if (isTestEnvironment) {
                                        pub_key = merchantObj.dev_pub_key;
                                        pri_key = merchantObj.dev_pri_key;
                                    } else {
                                        pub_key = merchantObj.pro_pub_key;
                                        pri_key = merchantObj.pro_pri_key;
                                    }
                                    // todo create key for test
                                                    /*String testString = "{\"Customer_number\":\"" + customerNumber + "\",\"Token\":\"" + token + "\",\"Amount\": " + amount + "}";
                                                    byte[] testByte = testString.getBytes();
                                                    final String testEn = cryption.encrypt(testByte, pub_key);
                                                    log.add("key hash=", testEn);*/
                                    //todo dung hash sau khi test
                                    boolean result = cryption.resultVerifyData(hash, pub_key, pri_key);
                                    //boolean result = cryption.resultVerifyData(testEn, pub_key, pri_key);
                                    if (!result) {
                                        //Du lieu khong dung, tra loi tiep
                                        logger.info("Giải mã hash thất bại");
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Giải mã hash thất bại");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }

                                    logger.info("Public key oki");
                                    // todo dung hash sau khi test
                                    String dataDecrypt = "";
                                    try {
                                        dataDecrypt = cryption.decrypt(hash.trim(), pri_key.trim());
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        logger.info("Giải mã hash thất bại: " + ex.getMessage());
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Giải mã hash thất bại");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }

                                    //final String dataDecrypt = cryption.decrypt(testEn.trim(), pri_key.trim());
                                    logger.info(dataDecrypt);
                                    logger.info("Giải mã hash thành công");
                                    if (dataDecrypt == null || "".equals(dataDecrypt)) {
                                        // Tra ket qua ve cho app
                                        log.add("desc", "hash is null" + merchantId);
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Hash không có thông tin");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 154);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }

                                    String merchantIdHash;
                                    String TIDHash;
                                    long amountHash;
                                    final String paymentCodeHash;
                                    try {
                                        JsonObject joHash = new JsonObject(dataDecrypt.toString());
                                        merchantIdHash = joHash.getString(StringConstUtil.MerchantKeyManage.MERCHANT_ID, "");
                                        TIDHash = joHash.getString(StringConstUtil.MerchantKeyManage.TID, "");
                                        amountHash = joHash.getLong(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
                                        paymentCodeHash = joHash.getString("paymentCode", "");
                                        concurrentHashMapCodeOTP.put(paymentCodeHash, TIDHash);
                                        log.add("merchantIdHash", merchantIdHash);
                                        log.add("TIDHash", TIDHash);
                                        log.add("amountHash", amountHash);
                                        log.add("paymentCode", paymentCodeHash);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        logger.info("Thông tin trong hash sai tham số: " + ex.getMessage());
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Thông tin trong hash sai tham số");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 155);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }
                                    if (!merchantIdHash.equals(merchantId) || !TIDHash.equals(TID) || amountHash != amount || "".equals(paymentCodeHash)) {
                                        // check hash
                                        log.add("desc", "hash wrong with param request from m " + merchantId);
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Thông tin trong hash sai tham số");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 155);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }
                                    String phoneHash = "";
                                    final String paymentCodeTmp = paymentCodeHash.replace(" ", "").trim();
                                    final String paymentCode = "MM".equalsIgnoreCase(paymentCodeTmp.substring(0, 2)) ? paymentCodeTmp.substring(2).trim() : paymentCodeTmp;
                                    logger.info("payment Code New IPOS " + paymentCode);
                                    final boolean haveVersion = !"MM".equalsIgnoreCase(paymentCodeTmp.substring(0, 2)) ? true : false;
                                    try {
                                        phoneHash = PaymentCodeServer.decodePhone(paymentCode, haveVersion);
                                        if (phoneHash == null || phoneHash.equals("")) {
                                            logger.info("Giải mã phone thất bại");
                                            log.writeLog();
                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Giải mã phone thất bại");
                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 153);
                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                            httpServerResponse.end("got data " + joReply);
                                            return;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        logger.info("Mã thanh toán không hợp lệ. Vui lòng tạo mã thanh toán mới để thực hiện lại giao dịch.");
                                        log.writeLog();
                                        messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Mã thanh toán không hợp lệ. Vui lòng tạo mã thanh toán mới để thực hiện lại giao dịch.");
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 162);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                        messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                        httpServerResponse.end(joReply.toString());
                                        return;
                                    }

                                    final String phonede = phoneHash;
                                    final int phone = DataUtil.strToInt(phonede);
                                    log.add("customerNumber", phone);
                                    //Lay du lieu ben app gui qua lam viec nao
                                    if (!TIDHash.equalsIgnoreCase(concurrentHashMapCodeOTP.remove(paymentCodeHash))) {
                                        logger.info("ERROR IPOS 2 lenh goi vao he thong");
                                        return;
                                    }
                                    phonesDb.getPhoneObjInfo(phone, new Handler<PhonesDb.Obj>() {
                                        @Override
                                        public void handle(final PhonesDb.Obj phoneObj) {
                                            if (phoneObj != null && !"".equalsIgnoreCase(phoneObj.sessionKey)) {
                                                iPosCashLimitDb.findOne(phonede, new Handler<IPosCashLimitDb.Obj>() {
                                                    @Override
                                                    public void handle(IPosCashLimitDb.Obj cashLimitObj) {
                                                                    /*long cashLimit = 0;
                                                                    if(phoneObj.isNamed){
                                                                        cashLimit = cashLimitObj == null ? LIMIT_PAY_OFFLINE : cashLimitObj.money_value;
                                                                    }else{
                                                                        cashLimit = cashLimitObj == null ? LIMIT_PAY_OFFLINE_NOT_NAME : cashLimitObj.money_value;
                                                                    }*/
                                                        long cashLimit = cashLimitObj == null ? LIMIT_PAY_OFFLINE_NOT_NAME : cashLimitObj.money_value;
                                                        if (cashLimit < amount) {
                                                            log.add("desc", "Vuong han muc roi " + cashLimit + " < " + amount);
                                                            log.writeLog();
                                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Vướng hạn mức, hạn mức đang là " + Misc.formatAmount(cashLimit).replace(",", ".") + "đ không thể thanh toán hóa đơn này.");
                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 161);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                            httpServerResponse.end(joReply.toString());
                                                            toSendNotiAndSMS(161, phone, amount, -1, merchantObj.merchant_name, now);
                                                            sendTranHis(161, -1, phonede, amount, paymentCode, merchantObj, now, description);
                                                            return;
                                                        }
                                                        if (amount < LIMIT_PAY_OFFLINE_MIN || amount > LIMIT_PAY_OFFLINE_MAX) {
                                                            log.add("desc", "Yêu cầu sai số tiền");
                                                            log.writeLog();
                                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Yêu cầu sai số tiền");
                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 151);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                            httpServerResponse.end(joReply.toString());
                                                            toSendNotiAndSMS(151, phone, amount, -1, merchantObj.merchant_name, now);
                                                            sendTranHis(151, -1, phonede, amount, paymentCode, merchantObj, now, description);
                                                            return;
                                                        }
                                                        try {
                                                            String sessionKey = phoneObj.sessionKey.replaceAll("-", "").trim();
                                                            //todo test encrypt data from app
                                                            //String tokenTest = encryptDataApp(sessionKey, phoneObj.pin, log);
                                                            //log.add("tokenTest", tokenTest);
                                                            log.add("sessionKey", sessionKey);
                                                            String secretKey = Base32.encode(sessionKey.getBytes());
                                                            log.add("secretKey", secretKey);
                                                            //SERVER dung tu day
                                                            final String passde;
                                                            if (paymentCode.length() == 18) {
                                                                passde = PaymentCodeServer.decodePassBase10(secretKey, paymentCode, haveVersion);
                                                            } else {
                                                                passde = PaymentCodeServer.decodePass(secretKey, paymentCode, haveVersion);
                                                            }
                                                            final String pass = passde;

                                                            //Du lieu sau khi decrypted.
                                                            //todo moc core => lay tien, lay tien voucher.
                                                            long curMoMo = 0;
                                                            long curPoint = 0;
                                                            long curGift = 0;
                                                            if ("".equalsIgnoreCase(phoneObj.balanceTotal) && Misc.isValidJsonObject(phoneObj.balanceTotal)) {
                                                                JsonObject joData = new JsonObject(phoneObj.balanceTotal);
                                                                curMoMo = joData.getLong(colName.CoreBalanceCols.BALANCE, 0);
                                                                curGift = joData.getLong(colName.CoreBalanceCols.VOUCHER, 0);
                                                                curPoint = joData.getLong(colName.CoreBalanceCols.POINT, 0);
                                                            }
                                                            final long currentUserMoMo = curMoMo;

                                                            giftManager.getQueuedGift("0" + phoneObj.number, merchantObj.service_id, new Handler<QueuedGiftResult>() {
                                                                @Override
                                                                public void handle(QueuedGiftResult queuedGiftResult) {
                                                                    long giftAmountCanUse = 0;
                                                                    if (queuedGiftResult != null && queuedGiftResult.gifts != null && queuedGiftResult.gifts.size() > 0) {
                                                                        for (Gift gift : queuedGiftResult.gifts.values()) {
                                                                            giftAmountCanUse += gift.amount;
                                                                        }
                                                                    }
                                                                    long moneyToSend = giftAmountCanUse + currentUserMoMo < amount ? 10000 : amount;
                                                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                                                                    messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, SoapError.CoreErrorMap_VN.get(0));
                                                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                                    messageReply.putString(StringConstUtil.MerchantKeyManage.CUSTOMER_NAME, phoneObj.name);
                                                                    messageReply.putString(StringConstUtil.MerchantKeyManage.CUSTOMER_EMAIL, phoneObj.email);
                                                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.CUSTOMER_AMOUNT, moneyToSend);
                                                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.CUSTOMER_PHONE, phoneObj.number);
                                                                    joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                                    IPOSTransactionInfoDb.Obj iposObj = new IPOSTransactionInfoDb.Obj();
                                                                    iposObj.payment_code = paymentCode;
                                                                    iposObj.merchant_id = merchantId;
                                                                    iposObj.time = System.currentTimeMillis();
                                                                    iposObj.decrypted_payment_code = phonede + "_" + passde;
                                                                    iposTransactionInfoDb.insert(iposObj, new Handler<Integer>() {
                                                                        @Override
                                                                        public void handle(Integer result) {
                                                                            httpServerResponse.end(joReply.toString());
                                                                        }
                                                                    });

                                                                }
                                                            });
                                                            //REPLY TO CLIENT
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            logger.info("Exception: " + e.getMessage());
                                                            log.writeLog();
                                                            messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Lỗi do exception");
                                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 159);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                            messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                            joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                            httpServerResponse.end(joReply.toString());
                                                            return;
                                                        }
                                                    }
                                                });
                                            } else {
                                                logger.info("Info phone not exist");
                                                log.writeLog();
                                                messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Không có thông tin khách hàng");
                                                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 158);
                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                                messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                                joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                                httpServerResponse.end(joReply.toString());
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    logger.info("Exception: " + e.getMessage());
                                    log.writeLog();
                                    messageReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, "Lỗi do exception");
                                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 159);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, -1);
                                    messageReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                    joReply.putObject(StringConstUtil.MerchantKeyManage.MESSAGE, messageReply);
                                    httpServerResponse.end(joReply.toString());
                                    e.printStackTrace();
                                    return;
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.writeLog();
                    request.response().end("Get request but error");
                }

            }
        });

    }

    private void processPayMerchantBill(final PhonesDb.Obj phoneObj, final String billId, final String desc, final long amount, final String pin, final Logger logger, final String phone_number, final MerchantKeyManageDb.Obj merchantObj, final Common.BuildLog log, final JsonObject joReply, final HttpServerRequest request
            , final String data_client, final Cryption cryption) {
        SockData sockData = new SockData(vertx, logger, container.config());
        sockData.setPhoneObj(phoneObj, logger, "");
        sockData.pin = pin;
        sockData.setNumber(DataUtil.strToInt(phone_number));
        transferProcess.payOneMerchantBill(log, merchantObj.service_id, amount, sockData, billId, pin, merchantObj.agent_name, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonConnectorResponse) {
                boolean isConnector = jsonConnectorResponse.getBoolean("isConnector", false);
                int error = jsonConnectorResponse.getInteger("error", -1);
                long tranId = jsonConnectorResponse.getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());
                log.add("isConnector", isConnector);
                log.add("error", error);
                if (isConnector) {
                    if (error == 0) {
                        log.add("desc", "m2Merchant success");
                        joReply.putNumber("status", 0);
                        joReply.putString("message", SoapError.CoreErrorMap_VN.get(error));
                    } else if (error == -1) {
                        log.add("desc", "m2Merchant fail");
                        joReply.putNumber("status", -1);
                        joReply.putString("message", SoapError.CoreErrorMap_VN.get(error));
                    } else if (error == 100) {
                        log.add("desc", "Phiên làm việc hết hiệu lực");
                        joReply.putNumber("status", 100);
                        joReply.putString("message", "Phiên làm việc hết hiệu lực");
                    } else {
                        log.add("desc", "m2Merchant fail");
                        joReply.putNumber("status", error);
                        joReply.putString("message", SoapError.CoreErrorMap_VN.get(error));
                    }
                    try {
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                        request.response().end(joReply.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        log.add("exception ne", ex.toString());
                    }
                    log.writeLog();
                } else {
                    processM2Merchant(phoneObj, desc, amount, pin, logger, phone_number, merchantObj, log, joReply, data_client, request, cryption);
                }

            }
        });
    }


    private void processM2Merchant(PhonesDb.Obj phoneObj, final String desc, final long amount, final String pin, final Logger logger, final String phone_number, final MerchantKeyManageDb.Obj merchantObj, final Common.BuildLog log, final JsonObject joReply, String data_client, final HttpServerRequest request, final Cryption cryption) {
        final SockData sockData = new SockData(vertx, logger, container.config());
        sockData.setPhoneObj(phoneObj, logger, "");
        sockData.pin = pin;
        sockData.setNumber(DataUtil.strToInt(phone_number));


        if ("".equalsIgnoreCase(data_client) || !Misc.isValidJsonObject(data_client)) {
            log.add("desc", "processM2Merchant_1 fail");
            log.add("desc", "Thong tin bi sai");
            log.writeLog();
            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1008));
            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1008);
            request.response().end(joReply.toString());
            return;
        }

        final JsonObject jsonDataClient = new JsonObject(data_client);
        final long client_amount;
        final String client_billId;
        try {
            client_amount = jsonDataClient.getLong(StringConstUtil.MerchantKeyManage.AMOUNT, 0);
            String client_merchantCode = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.MERCHANT_CODE, "");
            String client_username = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.USER_NAME, "");
            client_billId = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.BILL_ID, "");
            String client_phoneNumber = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.PHONENUMBER, "");
            final String tranId = jsonDataClient.getString(StringConstUtil.MerchantKeyManage.TRANSID, "");
            log.add("jsonDataClient", jsonDataClient.toString());
            if (!client_phoneNumber.equalsIgnoreCase(phone_number) || client_amount != amount || !client_merchantCode.equalsIgnoreCase(merchantObj.merchant_id) || "".equalsIgnoreCase(tranId)) {
                log.add("desc", "processM2Merchant_2 compare fail");
                log.add("desc", "Thong tin bi sai");
                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1008));
                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1008);
                request.response().end(joReply.toString());
                log.writeLog();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.add("desc", "Tham so khong hop le");
            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
            request.response().end(joReply.toString());
            log.writeLog();
            return;
        }

        transDb.getTranByPartnerInvNo(DataUtil.strToInt(phone_number), client_billId, new Handler<TranObj>() {
            @Override
            public void handle(final TranObj tranObj) {
                //trans not exist
                if (tranObj == null) {

                    Queue<String> queueServiceId = new LinkedList<>();
                    queueServiceId.add(merchantObj.service_id);

                    TransferWithGiftContext.build(DataUtil.strToInt(phone_number)
                            , queueServiceId
                            , client_billId
                            , client_amount
                            , vertx
                            , giftManager
                            , sockData
                            , 0
                            , 0, logger, new Handler<TransferWithGiftContext>() {
                        @Override
                        public void handle(final TransferWithGiftContext context) {
                            JsonObject jsonRequest = new JsonObject();
                            jsonRequest.putNumber("requestTime", System.currentTimeMillis());
                            jsonRequest.putString("coreCmd", "");
                            jsonRequest.putNumber("requestType", 0);
                            jsonRequest.putString("pass", "" + pin);
                            jsonRequest.putString("requestId", "" + Long.parseLong(String.format("%s%s", System.currentTimeMillis(), RandomStringUtils.randomNumeric(4))));
                            jsonRequest.putString("initiator", phone_number);
                            jsonRequest.putString("replyList", "");
                            JsonObject jsonExtras = new JsonObject();
                            jsonExtras.putString("account", client_billId);
                            if ((context.voucher + context.remainVoucher) != 0) {
                                jsonRequest.putString("sms", "TRANWVPNEW " + pin);
                                jsonExtras.putString("pin", "" + pin);
                                jsonExtras.putNumber("type_vptm", 2);
                                jsonExtras.putNumber("amountvoucher", context.voucher + context.remainVoucher);
                                jsonExtras.putNumber("amountpoint", 0);
                                jsonExtras.putNumber("amounttran", context.amount);
                                jsonExtras.putString("trantype", "billpay");
                                jsonExtras.putNumber("billerid", 2);
                                jsonExtras.putString("chanel", "backend");
                                jsonExtras.putString("specialagent", merchantObj.agent_name);
                            } else {
                                jsonRequest.putString("sms", "BILLPAYNEW1B " + pin);
                                jsonExtras.putString("client", "backend");
                                jsonExtras.putString("issms", "no");
                                jsonExtras.putNumber("amount", amount);
                                jsonExtras.putNumber("type", 1);
                                jsonExtras.putString("target", merchantObj.agent_name);
                            }
                            jsonRequest.putObject("extras", jsonExtras);

                            transferProcess.billPayToRedis(logger, jsonRequest, context, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jObjFromSoap) {
                                    int error = jObjFromSoap.getInteger("error", -1);
                                    long tid = jObjFromSoap.getLong("tranId", 0);
                                    if (error == 0) {
                                        log.add("desc", "m2Merchant success");
                                        joReply.putNumber("status", 0);
                                        joReply.putString("message", "Nhan yeu cau thanh cong");
                                        context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(final JsonObject result) {

                                            }
                                        });
                                        if (merchantObj.service_type != null && merchantObj.service_type.equals("1")) {
                                            sendNotify(logger, merchantObj, cryption, client_billId, String.valueOf(tid), client_amount);
                                        }
                                    } else if (error == -1) {
                                        log.add("desc", "m2Merchant fail");
                                        joReply.putNumber("status", 0);
                                        joReply.putString("message", "Nhan yeu cau thanh cong nhung co error (sdt chua dang ki)" + error);
                                    } else {
                                        log.add("desc", "m2Merchant fail");
                                        joReply.putNumber("status", error);
                                        joReply.putString("message", "Nhan yeu cau thanh cong nhung co error " + error);
                                    }
                                    try {
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                        joReply.putString(StringConstUtil.MerchantKeyManage.TRANSID, tid + "");
                                        request.response().end(joReply.toString());
                                        final JsonObject jsonKvp = new JsonObject();
                                        jsonKvp.putString("merchant", "billpay");
                                        jsonKvp.putString("billId", client_billId);
                                        transferProcess.buildTranHisAndSendNotiForM2M(merchantObj, jsonKvp, jObjFromSoap, merchantObj.service_id, phone_number, merchantObj.merchant_number, error, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject jsonObject) {
                                            }
                                        });
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        log.add("exception ne", ex.toString());
                                    }
                                    log.writeLog();
                                    return;
                                }
                            });
                        }
                    });
                } else {
                    log.add("desc", "Hóa đơn đã thanh toán");
                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Hóa đơn đã thanh toán");
                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1013);
                    request.response().end(joReply.toString());
                    log.writeLog();
                    return;
                }
            }
        });
    }

    //For test encrypt ipos service Base10
    private String encryptDataApp(String sessionKey, String phoneNumber, String pass, final Common.BuildLog log, int appcode) {
        int refreshCount = new Random().nextInt(Integer.MAX_VALUE);
        refreshCount++;
        if (refreshCount == Integer.MAX_VALUE) {
            refreshCount = refreshCount % 100000;
        }
        if (refreshCount < 0) {
            refreshCount = Math.abs(refreshCount);
        }
        String base10 = "";
        try {
            String secret = Base32.encode(sessionKey.getBytes());
            System.out.println("sessionKey app:" + secret);

            base10 = PaymentCodeClient.encodeBase10(secret, phoneNumber, pass, refreshCount, appcode < 1);

            System.out.println("Encode Base10 app:" + base10);

            ///Convert to Base30
            //String base30 = ConverterUtils.Base10toBase30(base10);
            //System.out.println("Encode Base30:" + base30);//ma 5 so base30
            return base10;
        } catch (Exception e) {
            e.printStackTrace();
            log.add("Exception ", e.toString());
        }

        return base10;
    }

    //for test merchant ipos service
    private void testExecutePos(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    final JsonObject joReply = new JsonObject();
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());
                    log.setPhoneNumber("excutePos");

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String merchantId = joReceive.getString("merchantId", "");
                    final String TID = joReceive.getString("TID", "");
                    final String description = joReceive.getString("description", "");
                    final String voucherCode = joReceive.getString("voucherCode", "");
                    final String paymentCode = joReceive.getString("paymentCode", "");
                    final long amount = joReceive.getLong("amount", 0);

                    if ("".equalsIgnoreCase(merchantId) || "".equalsIgnoreCase(TID) || "".equalsIgnoreCase(paymentCode)) {
                        log.add("desc", "Wrong request");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        request.response().end(joReply.toString());
                        return;
                    } else {
                        merchantKeyManageDb.findOne(merchantId, new Handler<MerchantKeyManageDb.Obj>() {
                                    @Override
                                    public void handle(MerchantKeyManageDb.Obj merchantKeyObj) {
                                        if (merchantKeyObj == null) {
                                            log.writeLog();
                                            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1006));
                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1006);
                                            request.response().end(joReply.toString());
                                            return;
                                        }
                                        try {
                                            String pub_key = merchantKeyObj.dev_pub_key;

                                            // todo create key for test
                                            String testString = "{\"merchantId\":\"" + merchantId + "\",\"TID\":\"" + TID + "\",\"paymentCode\":\"" + paymentCode + "\",\"amount\": " + amount + "}";
                                            byte[] testByte = testString.getBytes();
                                            final String hash = cryption.encrypt(testByte, pub_key);
                                            log.add("hash", hash);
                                            log.add("merchantId", merchantId);
                                            log.add("TID", TID);
                                            log.add("paymentCode", paymentCode);
                                            log.add("amount", amount);
                                            log.add("***testString", testString);

                                            joReply.putString(StringConstUtil.MerchantKeyManage.MERCHANT_ID, merchantId);
                                            joReply.putString(StringConstUtil.MerchantKeyManage.TID, TID);
                                            joReply.putString("voucherCode", voucherCode);
                                            joReply.putString(StringConstUtil.MerchantKeyManage.DESCRIPTION, description);
                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                                            joReply.putString(StringConstUtil.MerchantKeyManage.HASH, hash);
                                            log.writeLog();
                                            request.response().end(joReply.toString());

                                        } catch (Exception e) {
//                            e.printStackTrace();
                                            log.writeLog();
                                            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1006));
                                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1006);
                                            request.response().end(joReply.toString());
                                        }
                                    }
                                }
                        );


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    request.response().end("Get request but error ");
                }
            }
        });
    }

    private void cryptlib(final HttpServerRequest request, final Common.BuildLog log, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                final JsonObject joReply = new JsonObject();
                try {
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());
                    log.setPhoneNumber("excutePos");

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final JsonObject jsonStringObj = joReceive.getObject("jsonString", null);
                    final String SessionKey = joReceive.getString("SessionKey", "");

                    if (jsonStringObj == null || "".equalsIgnoreCase(SessionKey)) {
                        log.add("desc", "Wrong request");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        joReply.putString("encryptString", "");
                        log.writeLog();
                        request.response().end(joReply.toString());
                        return;
                    } else {
                        final String jsonString = jsonStringObj.toString();

                        String dataEncryp = cryption.encrypSessionKeyData(jsonString, SessionKey);
                        log.add("dataEncryp ", dataEncryp);
                        log.add("dataDecypt ", cryption.decryptSessionKeyData(dataEncryp, SessionKey));
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(0));
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                        joReply.putString("encryptString", dataEncryp);
                        log.writeLog();
                        request.response().end(joReply.toString());
                        return;
                    }
                } catch (Exception e) {

                    joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Exception " + e.getMessage());
                    joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1006);
                    joReply.putString("encryptString", "");
                    log.writeLog();
                    request.response().end(joReply.toString());
                    e.printStackTrace();
                }
            }
        });
    }

    //create code POS
    private void createcode(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    final JsonObject joReply = new JsonObject();
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());
                    log.setPhoneNumber("excutePos");

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String customerNumber = joReceive.getString("customerNumber", "");
                    final String pin = joReceive.getString("pin", "");
                    final int appCode = joReceive.getInteger("appcode", 0);


                    if ("".equalsIgnoreCase(customerNumber) || "".equalsIgnoreCase(pin)) {
                        log.add("desc", "Wrong request");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        request.response().end(joReply.toString());
                        return;
                    } else {
                        try {
                            phonesDb.getPhoneObjInfo(DataUtil.strToInt(customerNumber), new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(final PhonesDb.Obj phoneObj) {
                                    if (phoneObj != null && !"".equalsIgnoreCase(phoneObj.sessionKey)) {
                                        String sessionKey = phoneObj.sessionKey.replaceAll("-", "").trim();
                                        //todo test encrypt data from app
                                        String tokenTest = encryptDataApp(sessionKey, customerNumber, phoneObj.pin, log, appCode);
                                        log.add("tokenTest", tokenTest);
                                        log.add("sessionKey", sessionKey);

                                        joReply.putString(StringConstUtil.MerchantKeyManage.TOKEN, tokenTest);
                                        log.writeLog();
                                        request.response().end(joReply.toString());
                                    } else {
                                        log.add("desc", "Wrong request");
                                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                                        request.response().end(joReply.toString());
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.writeLog();
                            joReply.putString(StringConstUtil.MerchantKeyManage.TOKEN, "");
                            request.response().end(joReply.toString());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    request.response().end("Get request but error ");
                }
            }
        });
    }

    //create code POS
    private void getAndSetCashLimit(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    final JsonObject joReply = new JsonObject();
                    logger.info("data get byte " + buffer.getBytes());
                    logger.info("data to string " + buffer.toString());
                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String customerNumber = joReceive.getString("number", "");
                    final String sessionKey = joReceive.getString("sessionkey", "");
                    final long amount = joReceive.getLong("amount", -1);
                    log.setPhoneNumber("customerNumber + getAndSetCashLimit");

                    if ("".equalsIgnoreCase(customerNumber) || "".equalsIgnoreCase(sessionKey)) {
                        log.add("desc", "Wrong request");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1020));
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        request.response().end(joReply.toString());
                    } else {
                        try {
                            phonesDb.getPhoneObjInfo(DataUtil.strToInt(customerNumber), new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(final PhonesDb.Obj phoneObj) {
                                    if (phoneObj != null && !"".equalsIgnoreCase(phoneObj.sessionKey) && sessionKey.equalsIgnoreCase(phoneObj.sessionKey)) {
                                        //todo test encrypt data from app
                                        if (amount < 0) {
                                            //Get Cash Limit
                                            iPosCashLimitDb.findOne(customerNumber, new Handler<IPosCashLimitDb.Obj>() {
                                                @Override
                                                public void handle(IPosCashLimitDb.Obj IPosCustomerObj) {
                                                    if (IPosCustomerObj == null) {
                                                        log.add("desc", "Chua set hang muc cho so dien thoai nay.");
                                                        joReply.putNumber(StringConstUtil.ERROR, 0);
                                                        joReply.putString(StringConstUtil.DESCRIPTION, "");
                                                        joReply.putNumber(StringConstUtil.AMOUNT, merchantKeyManageObject.getLong(StringConstUtil.MerchantKeyManage.STANDARD_AMOUNT, 2000000));
                                                        joReply.putString(StringConstUtil.NUMBER, customerNumber);
                                                        log.writeLog();
                                                        request.response().end(joReply.toString());
                                                        return;
                                                    }
                                                    log.add("desc", "Tra hang muc.");
                                                    joReply.putNumber(StringConstUtil.ERROR, 0);
                                                    joReply.putString(StringConstUtil.DESCRIPTION, "");
                                                    joReply.putNumber(StringConstUtil.AMOUNT, IPosCustomerObj.money_value);
                                                    joReply.putString(StringConstUtil.NUMBER, customerNumber);
                                                    log.writeLog();
                                                    request.response().end(joReply.toString());
                                                    return;
                                                }
                                            });
                                            return;
                                        }
                                        //Luu hang muc
                                        IPosCashLimitDb.Obj iPosObj = new IPosCashLimitDb.Obj();
                                        iPosObj.money_value = amount;
                                        iPosObj.phone_number = customerNumber;
                                        iPosObj.time = System.currentTimeMillis();
                                        iPosCashLimitDb.upSert(customerNumber, iPosObj.toJson(), new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {
                                                log.add("desc", "Cap nhat hang muc thanh cong.");
                                                joReply.putNumber(StringConstUtil.ERROR, 0);
                                                joReply.putString(StringConstUtil.DESCRIPTION, "");
                                                joReply.putNumber(StringConstUtil.AMOUNT, amount);
                                                joReply.putString(StringConstUtil.NUMBER, customerNumber);
                                                log.writeLog();
                                                request.response().end(joReply.toString());
                                                return;
                                            }
                                        });
                                        return;
                                    } else {
                                        log.add("desc", "Không tồn tại thông tin số điện thoại");
                                        joReply.putNumber(StringConstUtil.ERROR, -1000);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "Không tồn tại thông tin số điện thoại");
                                        joReply.putNumber(StringConstUtil.AMOUNT, amount);
                                        joReply.putString(StringConstUtil.NUMBER, customerNumber);
                                        log.writeLog();
                                        request.response().end(joReply.toString());
                                        return;
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.writeLog();
                            joReply.putString(StringConstUtil.MerchantKeyManage.TOKEN, "");
                            request.response().end(joReply.toString());
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    request.response().end("Get request but error ");
                    return;
                }
            }
        });
    }

    //for test hash
    private void encrypthash(final HttpServerRequest request, final Common.BuildLog log, final Logger logger, final Cryption cryption) {
        request.bodyHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    final JsonObject joReply = new JsonObject();
                    log.add("data get byte ", buffer.getBytes());
                    log.add("data to string ", buffer.toString());
                    log.setPhoneNumber("excutePos");

                    JsonObject joReceive = new JsonObject(buffer.toString());
                    final String merchantId = joReceive.getString("merchantId", "");
                    final String TID = joReceive.getString("TID", "");
                    final String paymentCode = joReceive.getString("paymentCode", "");
                    final long amount = joReceive.getLong("amount", 0);

                    if ("".equalsIgnoreCase(merchantId) || "".equalsIgnoreCase(TID) || "".equalsIgnoreCase(paymentCode)) {
                        log.add("desc", "Wrong request");
                        joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Wrong request");
                        joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1020);
                        joReply.putString(StringConstUtil.MerchantKeyManage.HASH, "");
                        request.response().end(joReply.toString());
                        return;
                    } else {
                        try {

                            String pub_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlrTGcWKzpVponjdFfc6r3Vs/T9aYH+qRk9aLpLizrdKHNI5EF7qo0ENbJwwJPLO2ulzfFkOhr8YQU7WeADMSmluoaT5j5mWDEcG3OhP4szS17Vw4xAuzsKDNdDhFTQn5v/yHiF8Xo1lHTthlDNM7NojHBDJSFltVPyFXc7hbmMSsBfNJr2eoL30wHUWpSb3AEwSDLXO8aUs9tbtc7jmvdFLIJAZQWUCVBLgbRlfB2jN/Stqu12Y6o+YffIL4L1n5+5uZzusaohLVtBH2vYeWZhqU84UpD9VBNg1QYbgTTRe4+J6sgs2BBs21jZBZxA9LWVfjVFX8Vm2RtE2iySNQ3QIDAQAB";

                            // todo create key for test
                            String testString = "{\"merchantId\":\"" + merchantId + "\",\"TID\":\"" + TID + "\",\"paymentCode\":\"" + paymentCode + "\",\"amount\": " + amount + "}";
                            byte[] testByte = testString.getBytes();
                            final String hash = cryption.encrypt(testByte, pub_key);
                            log.add("hash", hash);
                            log.add("merchantId", merchantId);
                            log.add("TID", TID);
                            log.add("paymentCode", paymentCode);
                            log.add("amount", amount);
                            log.add("***testString", testString);
                            log.add("***hash", hash);
                            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(0));
                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 0);
                            joReply.putString(StringConstUtil.MerchantKeyManage.HASH, hash);
                            log.writeLog();
                            request.response().end(joReply.toString());

                        } catch (Exception e) {
                            e.printStackTrace();
                            log.writeLog();
                            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.CoreErrorMap_VN.get(1006));
                            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, 1006);
                            joReply.putString(StringConstUtil.MerchantKeyManage.HASH, "");
                            request.response().end(joReply.toString());
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    request.response().end("Get request but error ");
                }
            }
        });
    }

    public void processIPOSTransfer(final Logger logger, final String pin, final SockData _data
            , final String billId
            , final MerchantKeyManageDb.Obj merchantObj
            , final String serviceId
            , final long customer_amount
            , final String receiverNumber
            , final String senderNumber
            , final String description, PhonesDb.Obj phoneObj, final JsonObject jsonDataClient, final Handler<JsonObject> callback) {

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        final SockData sockData = new SockData(vertx, logger, container.config());
        sockData.setPhoneObj(phoneObj, logger, "");
        sockData.pin = pin;
        sockData.setNumber(DataUtil.strToInt(senderNumber));

        final long amount = customer_amount;
        Queue<String> queueServiceId = new LinkedList<>();
        queueServiceId.add(merchantObj.service_id);

        TransferWithGiftContext.build(DataUtil.strToInt(senderNumber)
                , queueServiceId
                , billId
                , amount
                , vertx
                , giftManager
                , sockData
                , 0
                , 0, logger, new Handler<TransferWithGiftContext>() {
            @Override
            public void handle(final TransferWithGiftContext context) {
                JsonObject jsonRequest = new JsonObject();
                jsonRequest.putNumber("requestTime", System.currentTimeMillis());
                jsonRequest.putString("coreCmd", "");
                jsonRequest.putNumber("requestType", 0);
                jsonRequest.putString("pass", "" + pin);
                jsonRequest.putString("requestId", "" + Long.parseLong(String.format("%s%s", System.currentTimeMillis(), RandomStringUtils.randomNumeric(4))));
                jsonRequest.putString("initiator", senderNumber);
                jsonRequest.putString("replyList", "");
                JsonObject jsonExtras = new JsonObject();
                jsonExtras.putString("account", billId);
                if ((context.voucher + context.remainVoucher) != 0) {
                    jsonRequest.putString("sms", "TRANWVPNEW " + pin);
                    jsonExtras.putString("pin", "" + pin);
                    jsonExtras.putNumber("type_vptm", 2);
                    jsonExtras.putNumber("amountvoucher", context.voucher + context.remainVoucher);
                    jsonExtras.putNumber("amountpoint", 0);
                    jsonExtras.putNumber("amounttran", context.amount);
                    jsonExtras.putString("trantype", "billpay");
                    jsonExtras.putNumber("billerid", 2);
                    jsonExtras.putString("chanel", "backend");
                    jsonExtras.putString("specialagent", merchantObj.agent_name);
                } else {
                    jsonRequest.putString("sms", "BILLPAYNEW1B " + pin);
                    jsonExtras.putString("client", "backend");
                    jsonExtras.putString("issms", "no");
                    jsonExtras.putNumber("amount", amount);
                    jsonExtras.putNumber("type", 1);
                    jsonExtras.putString("target", merchantObj.agent_name);
                }
                jsonRequest.putObject("extras", jsonExtras);

                transferProcess.billPayToRedis(logger, jsonRequest, context, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jObjFromSoap) {
                        final JsonObject joReply = new JsonObject();
                        int error = jObjFromSoap.getInteger("error", -1);
                        long tid = jObjFromSoap.getLong("tranId", 0);
                        if (error == 0) {
                            log.add("desc", "m2Merchant success");
                            joReply.putNumber("status", 0);
                            joReply.putString("message", "Nhan yeu cau thanh cong");
                            joReply.putNumber(StringConstUtil.MerchantKeyManage.VOUCHER, context.voucher);
                            joReply.putNumber(StringConstUtil.MerchantKeyManage.MOMO, customer_amount - context.voucher);
                            joReply.putString(StringConstUtil.MerchantKeyManage.CUSTOMER_NUMBER_PARTNER, senderNumber);
                                    /*context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(final JsonObject result) {

                                        }
                                    });*/
                        } else if (error == -1) {
                            log.add("desc", "m2Merchant fail");
                            joReply.putNumber("status", 0);
                            joReply.putString("message", "Nhan yeu cau thanh cong nhung co error (sdt chua dang ki)" + error);
                        } else {
                            log.add("desc", "m2Merchant fail");
                            joReply.putNumber("status", error);
                            joReply.putString("message", "Nhan yeu cau thanh cong nhung co error " + error);
                        }
                        try {
                            joReply.putNumber(StringConstUtil.MerchantKeyManage.AMOUNT, amount);
                            joReply.putNumber(StringConstUtil.MerchantKeyManage.TRANSID, tid);
                            callback.handle(joReply);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            log.add("exception ne", ex.toString());
                            callback.handle(joReply);
                        }
                        log.writeLog();
                        context.transferGiftIfNeeded(vertx, giftProcess, context, new Handler<JsonObject>() {
                            @Override
                            public void handle(final JsonObject result) {

                            }
                        });
                        return;
                    }
                });
            }
        });
    }

    private void toSendNotiAndSMS(int result, int toUser, long amount, long tranId, String name, long now) {
        if (tranId == 0 || tranId == -1) {
            tranId = now;
        }
        String comment = "";
        String tpl = "";
        String caption = "";
        String sms = "";
        if (result == 0) {
            tpl = "Quý khách đã thực hiện thanh toán thành công tại %s với số tiền %sđ.";
            caption = "Thanh toán thành công";
            String smsTmp = "Quy khach da thuc hien thanh toan thanh cong tai %s bang MoMo voi so tien %s VND. Ma giao dich la %s";
            sms = String.format(smsTmp
                    , name
                    , Misc.formatAmount(amount).replace(",", ".")
                    , tranId);
            comment = String.format(tpl
                    , name
                    , Misc.formatAmount(amount).replace(",", "."));
        } else if (result == 1001) {
            comment = "Số dư trong ví của quý khách hiện tại không đủ để thực hiện giao dịch, vui lòng nạp tiền thêm vào ví và thực hiện thanh toán lại. Liên hệ hỗ trợ: 1900 5454 41";
            caption = "Thanh toán không thành công";
        } else if (result == 161) {
            comment = "Số tiền quý khách cần thanh toán vượt quá hạn mức đã cài đặt, vui lòng kiểm tra và thực hiện thanh toán lại. Liên hệ hỗ trợ: 1900 5454 41";
            caption = "Thanh toán không thành công";
        } else if (result == 100) {
            comment = "Mã thanh toán hết hiệu lực, vui lòng đổi mã mới và thực hiện thanh toán lại. Liên hệ hỗ trợ: 1900 5454 41";
            caption = "Thanh toán không thành công";
        } else if (result == 157) {
            comment = "Mỗi mã thanh toán chỉ được dùng cho 1 hóa đơn, vui lòng đổi mã mới và thực hiện thanh toán lại. Liên hệ hỗ trợ: 1900 5454 41";
            caption = "Thanh toán không thành công";
        } else if (result == 1004) {
            comment = "Quý khách hết hạn mức giao dịch trong ngày. Vui lòng định danh để tăng hạn mức giao dịch lên tối đa. Liên hệ hỗ trợ: 1900 5454 41";
            caption = "Thanh toán không thành công";
        } else if (result == 151) {
            comment = "Số tiền thanh toán tối thiểu là 10.000đ và tối đa là 20.000.000đ, vui lòng thực hiện lại. LH MoMo 1900 5454 41";
            caption = "Thanh toán không thành công";
        } else {
            tpl = "Lỗi %s, quý khách vui lòng thực hiện thanh toán lại. Liên hệ hỗ trợ: 1900 5454 41";
            caption = "Thanh toán không thành công";
            comment = String.format(tpl
                    , String.valueOf(result));
        }

        Notification noti = new Notification();
        noti.receiverNumber = toUser;
        noti.caption = caption;
        noti.body = comment;
        noti.sms = sms;
        if (result == 0) {
            noti.priority = 1;
        } else {
            noti.priority = 2;
        }
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

    private void sendTranHis(int result, long tranId, String ownerNumber, long amount, String billId, MerchantKeyManageDb.Obj merchantObj, long now, String description) {
        if (tranId == 0 || tranId == -1) {
            tranId = now;
        }
        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, 7);
        if (result == 0) {
            jsonTrans.putString(colName.TranDBCols.COMMENT, "Thanh toán thành công tại " + merchantObj.merchant_name);
            if (merchantObj.service_type != null && merchantObj.service_type.equals("2")) {
                jsonTrans.putString(colName.TranDBCols.COMMENT, description);
            }
            jsonTrans.putNumber(colName.TranDBCols.STATUS, 4);
        } else {
            jsonTrans.putString(colName.TranDBCols.COMMENT, "Thanh toán không thành công tại " + merchantObj.merchant_name);
            jsonTrans.putNumber(colName.TranDBCols.STATUS, 5);
        }

        jsonTrans.putString(colName.TranDBCols.PARTNER_ID, merchantObj.service_id);
        jsonTrans.putString(colName.TranDBCols.PARTNER_NAME, merchantObj.merchant_name);
        jsonTrans.putString(colName.TranDBCols.BILL_ID, billId);
        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amount);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(ownerNumber));
        jsonTrans.putNumber(colName.TranDBCols.IO, -1);
        Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTrans, new JsonObject());
    }

    private void sendNotify(final Logger logger, final MerchantKeyManageDb.Obj merchantObj, final Cryption cryption, final String billId, final String transId, final long amount) {
        String path = "/payment.json";
        HttpClientRequest request = httpClientGoit.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse response) {
                int statusCode = response.statusCode();
                if (statusCode != 200) {

                    //411: length required
                    //401: unauthorized
                    logger.info("error notify " + statusCode);
                    logger.info("desc notify " + response.statusMessage());
                    return;
                }

                response.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            logger.info("Respone notify " + buffer.toString());
                            JsonObject result = new JsonObject(buffer.toString());
                            if (!Misc.isNullOrEmpty(buffer.toString())) {
                                logger.info("desc notify success");
                            } else {
                                logger.info("desc notify fail");
                            }

                        } catch (Exception e) {
//                            e.printStackTrace();
                            logger.info("error notify 1006");
                            logger.info("desc notify error exception " + e.getMessage());
                        }

                    }
                });
            }
        });
        JsonObject jsonRequest = buildParaNotify(cryption, billId, transId, amount);

        Buffer bufferRequest = new Buffer(jsonRequest.toString());
        request.putHeader("content-length", String.valueOf(bufferRequest.length()));
        request.putHeader("content-Type", "application/json");
        logger.info("Request notify " + bufferRequest.toString());
        request.end(bufferRequest);
    }

    private JsonObject buildParaNotify(final Cryption cryption, final String billId, final String transId, final long amount) {
        JsonObject jsonData = new JsonObject();
        try {
            jsonData.putString("billId", cryption.encryptGotIt(billId));
            jsonData.putString("transId", cryption.encryptGotIt(transId));
            jsonData.putString("amount", cryption.encryptGotIt(String.valueOf(amount)));
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String format = formatter.format(date);
            jsonData.putString("paymentDate", cryption.encryptGotIt(format));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonData;
    }

}
