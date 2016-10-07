package com.mservice.momo.vertx.processor;

import banknetvn.md5.checkMD5;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.bank.entity.BankRequest;
import com.mservice.bank.entity.BankRequestFactory;
import com.mservice.card.entity.BankCardRequestFactory;
import com.mservice.card.entity.CardRequest;
import com.mservice.card.entity.CardResponse;
import com.mservice.conf.WalletType;
import com.mservice.momo.avatar.UserResourceVerticle;
import com.mservice.momo.data.*;
import com.mservice.momo.data.ironmanpromote.IronManPromoGiftDB;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.tracking.BanknetTransDb;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.data.web.ServiceDetailDb;
import com.mservice.momo.data.web.ServicePackageDb;
import com.mservice.momo.gateway.external.vng.VngUtil;
import com.mservice.momo.gateway.external.vng.vngClass;
import com.mservice.momo.gateway.internal.connectorproxy.ConnectorCommon;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.gateway.internal.core.CoreCommon;
import com.mservice.momo.gateway.internal.core.objects.Response;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.SoapVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.visamaster.VMCardType;
import com.mservice.momo.gateway.internal.visamaster.VMConst;
import com.mservice.momo.gateway.internal.visamaster.VMFeeType;
import com.mservice.momo.gateway.internal.visamaster.VMRequest;
import com.mservice.momo.gateway.internal.walletmapping.WalletMappingConst;
import com.mservice.momo.gateway.internal.walletmapping.WalletMappingError;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.MomoProto.*;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.*;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.form.Command;
import com.mservice.momo.vertx.form.FieldData;
import com.mservice.momo.vertx.form.ReplyObj;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.models.DynamicConfig;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.models.UserSetting;
import com.mservice.momo.vertx.models.smartlink.SmartLinkRequest;
import com.mservice.momo.vertx.periodic.TimerLocSync;
import com.mservice.momo.vertx.periodic.TimerNotiSync;
import com.mservice.momo.vertx.periodic.TimerService;
import com.mservice.momo.vertx.periodic.TimerTranSync;
import com.mservice.momo.vertx.processor.transferbranch.Vinagame123Phim;
import com.mservice.momo.vertx.retailer.FeeObj;
import com.mservice.momo.vertx.vcb.ReqObj;
import com.mservice.momo.vertx.vcb.VcbCommon;
import com.mservice.momo.vertx.vcb.VcbNoti;
import com.mservice.momo.vertx.visampointpromo.VMCardIdCardNumberDB;
import com.mservice.proxy.entity.ProxyRequest;
import com.mservice.visa.entity.CardInfo;
import com.mservice.visa.entity.VisaResponse;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by User on 3/18/14.
 */
public class InfoProcess {
    public static int REQUEST_MONEY_MAX_PER_DAY = 0;
    public static int NONAME_MAX_TRAN_COUNT_PER_DAY = 0;
    public static long NONAME_MAX_TRAN_VALUE_PER_DAY = 0;
    public static long DEFAULT_CORE_TIMEOUT = 7 * 60 * 1000L;
    public static int NUMBER_COUNT_CMND = 3;
    private static String Driver;
    private static String Url;
    private static String Username;
    private static String Password;
    private static checkMD5 md5 = new checkMD5();
    public boolean isStoreApp = false;
    protected com.mservice.momo.vertx.processor.Common mCommon;
    private Logger logger;
    private Vertx vertx;
    private BillsDb mBillsDb;
    private AgentsDb agentsDb;
    private DeviceInfoDb deviceInfoDb;
    private AccessHistoryDb accessHistoryDb;
    private PhonesDb phonesDb;
    private TransDb transDb;
    private Card card;
    private ReqMoney reqMoney;
    private NotificationDb notificationDb;
    private FeeDb feeDb;
    private DynamicConfigDb dynamicConfigDb;
    private PromotionDb promotionDb;
    private PromotedDb promotedDb;
    private JsonObject glbCfg;
    private UserSettingDb userSettingDb;
    private String ftp_url = "";
    private String ftp_usrname = "";
    private String ftp_password = "";
    private int ftp_min_img = 0;
    private int retailer_named_value = 0;
    private CDHH cdhh;
    private int cdhh_min_code = 0;
    private int cdhh_max_code = 0;
    private int cdhh_max_sms = 0;
    private int cdhh_min_val = 0;
    private int cdhh_max_val;
    private Promo123PhimGlxDb glxDb;
    private MntDb mntDb;
    private CDHHErr cdhhErr;
    private DBProcess dbProcess;
    private long TRANWITHPOINT_MIN_POINT = 0;
    private long TRANWITHPOINT_MIN_AMOUNT = 0;
    private GiftManager giftManager;
    private int pgCodeMin = 6100;
    private int pgCodeMax = 6140;
    //code for amway promotion
    private int amwayCodeMin = 7000;
    private int amwayCodeMax = 7009;
    //for otp client
    private OtpClientDb otpClientDb;
    private CardTypeDb cardTypeDb;
    private VMCardIdCardNumberDB vmCardIdCardNumberDB;
    //for cybersource
    private boolean sbsEnable = false;
    private long sbsMinValueDirect = 0;
    private long sbsMinValueRedirect = 0;
    private String sbsBussAddress = "";
    private JsonObject sbsCfg;
    private TransProcess transProcess = null;
    private GroupManageDb groupManageDb = null;
    private JsonObject bankCfg;
    private BillRuleManageDb billRuleManageDb;
    //BEGIN 0000000007 GET FEE FROM BANK
    private String BANKID = "bankid";
    private String SRCFROM = "srcfrom";
    private String TRANSTYPE = "transtype";
    private String AMOUNT = "amount";
    private String TOTAL_AMOUNT = "totalamount";
    private String VERIFY_CASHIN_OUT_RES = "VERIFY_CASHIN_OUT_RES";
    private JsonObject sync_store_app_json_obj;
    private boolean activeServiceFee;
    private IronManPromoGiftDB ironManPromoGiftDB;
    private JsonObject jsonBillRuleManagement;
    private NotificationToolDb notificationToolDb;
    private JsonObject joMoMoTool;
    //BEGIN 0000000019 Kiem tra chung minh nhan dan user
    //    private boolean checkCMND(String cmnd)
//    {
//        boolean isCMND = false;
//        try{
//            long cmndNumber = Long.parseLong(cmnd);
//            if(cmnd.startsWith("0") && cmndNumber > 10000000 && (cmnd.length() == 9 || cmnd.length() == 12))
//            {
//                isCMND = true;
//            }
//            else if(cmndNumber > 100000000 && (cmnd.length() == 9 || cmnd.length() == 12))
//            {
//                isCMND = true;
//            }
//        }catch(NumberFormatException ex)
//        {
//            logger.info("cmnd is error");
//            isCMND = false;
//        }
////        if(cmnd.length() == 9 || cmnd.length() == 12)
////        {
////        String regex = "^[0-9]{9,12}";
////        Pattern pattern = Pattern.compile(regex);
////        Matcher matcher = pattern.matcher(cmnd);
////            if(matcher.find())
////            {
////               isCMND = true;
////            }
////        }
////        else{
////            isCMND = false;
////        }
//
//
//        return isCMND;
//
//    }
    private String NUMBERRECEIVEM2M = "numberReceiveM2M";
    //END 0000000019 Kiem tra chung minh nhan dan user
    private String TOTALFEE = "totalfee";
    private String TRANSFEE = "transfee";
    private String MOMOFEE = "momofee";
    private String CHANNEL = "channel";
    private String SERVICE_FEE = "servicefee";
    private String HINT_OTP = "hintOtp";
    private String REQUIRE_OTP = "requireOTP";

    /*public void processGetFtpInfo(final NetSocket sock, final MomoMessage msg) {

        FtpReply.Builder builder = FtpReply.newBuilder();
        builder.setUrl(ftp_url)
                .setUsername(ftp_usrname)
                .setPassword(ftp_password);

        Buffer buffer = MomoMessage.buildBuffer(
                MsgType.FTP_INFO_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build().toByteArray()
        );
        mCommon.writeDataToSocket(sock, buffer);
    }*/
    private String IOCITY = "ioCity";
    private String SERVICE_ID = "serviceid";
    private PromotionProcess promotionProcess;
    private String VISA_GROUP = "";
    private String VISA_CAPSET_ID = "";
    private String VISA_UPPER_LIMIT = "";
    private SpecialGroupDb specialGroupDb;

    private BanknetTransDb banknetTransDb;


    public InfoProcess(Vertx vertx, final Logger logger, JsonObject glbCfg) {
        this.vertx = vertx;
        this.logger = logger;
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);
        this.transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);
        this.accessHistoryDb = new AccessHistoryDb(vertx.eventBus());
        this.deviceInfoDb = new DeviceInfoDb(vertx.eventBus());
        this.agentsDb = new AgentsDb(vertx.eventBus(), logger);
        this.mBillsDb = new BillsDb(vertx.eventBus());
        this.card = new Card(vertx.eventBus(), logger);
        this.reqMoney = new ReqMoney(vertx.eventBus(), logger);
        this.notificationDb = DBFactory.createNotiDb(vertx, logger, glbCfg);
        this.feeDb = new FeeDb(vertx, logger);
        this.promotionDb = new PromotionDb(vertx.eventBus(), logger);
        this.promotedDb = new PromotedDb(vertx.eventBus(), logger);
        this.groupManageDb = new GroupManageDb(vertx, logger);
        this.mCommon = new com.mservice.momo.vertx.processor.Common(vertx, logger, glbCfg);
        this.userSettingDb = new UserSettingDb(vertx, logger);
        this.glbCfg = glbCfg;
        this.transProcess = new TransProcess(vertx, logger, glbCfg);
        ftp_url = glbCfg.getObject("retailer").getObject("ftp_upload").getString("url", "");
        ftp_usrname = glbCfg.getObject("retailer").getObject("ftp_upload").getString("username", "");
        ftp_password = glbCfg.getObject("retailer").getObject("ftp_upload").getString("password", "");
        ftp_min_img = glbCfg.getObject("retailer").getInteger("min_img", 3);
        retailer_named_value = glbCfg.getObject("retailer").getInteger("named_value", 20000);

        cdhh = new CDHH(vertx, logger);
        cdhhErr = new CDHHErr(vertx.eventBus(), logger);
        specialGroupDb = new SpecialGroupDb(vertx, logger);
        //todo get config
        cdhh_min_code = glbCfg.getObject("capdoihoanhao").getInteger("min_code", 1);
        cdhh_max_code = glbCfg.getObject("capdoihoanhao").getInteger("max_code", 7);
        cdhh_max_sms = glbCfg.getObject("capdoihoanhao").getInteger("max_sms_per_day", 30);
        cdhh_min_val = glbCfg.getObject("capdoihoanhao").getInteger("max_code", 1000);
        cdhh_max_val = glbCfg.getObject("capdoihoanhao").getInteger("max_val", 80000);

        JsonObject pointConfig = glbCfg.getObject("point", new JsonObject());
        TRANWITHPOINT_MIN_POINT = pointConfig.getLong("minPoint", 0);
        TRANWITHPOINT_MIN_AMOUNT = pointConfig.getLong("mintAmount", 0);
        giftManager = new GiftManager(vertx, logger, glbCfg);
        jsonBillRuleManagement = glbCfg.getObject(StringConstUtil.BillRuleManagement.JSON_OBJECT, new JsonObject());
        notificationToolDb = new NotificationToolDb(vertx, logger);
        //BEGIN 0000000015
        vmCardIdCardNumberDB = new VMCardIdCardNumberDB(vertx, logger);
        boolean checkStoreApp = MoMoInfoProcess.checkStoreApp();
        logger.info("checkStoreApp ------>" + checkStoreApp);
        //END   0000000015
        JsonObject pgGalaxy = glbCfg.getObject("galaxy", null);
        if (pgGalaxy != null) {
            JsonArray pgCode = pgGalaxy.getArray("pgcode");
            pgCodeMin = ((JsonObject) pgCode.get(0)).getInteger("min", 6100);
            pgCodeMax = ((JsonObject) pgCode.get(1)).getInteger("max", 6200);
        }

        JsonObject amwayPromo = glbCfg.getObject("amwaypromo", null);
        if (amwayPromo != null) {
            amwayCodeMin = amwayPromo.getInteger("codemin", 7000);
            amwayCodeMax = amwayPromo.getInteger("codemax", 7009);
        }

        glxDb = new Promo123PhimGlxDb(vertx.eventBus(), logger);
        mntDb = new MntDb(vertx.eventBus(), logger);
        sbsCfg = glbCfg.getObject("cybersource", null);
        if (sbsCfg != null) {
            sbsEnable = sbsCfg.getBoolean("enable", false);
//            sbsMinValueDirect = sbsCfg.getLong("min_value_direct", 50000);
            sbsBussAddress = sbsCfg.getString("proxyBusAddress", "cyberSourceVerticle");
//            sbsMinValueRedirect = sbsCfg.getLong("min_value_redirect", 50000);
        }
        isStoreApp = glbCfg.getBoolean(StringConstUtil.CHECK_STORE_APP, false);
        activeServiceFee = glbCfg.getBoolean(StringConstUtil.ACTIVE_SERVICE_FEE, false);
        // Load config for CungMua BEGIN 0000000025 Cung mua
//        cungmuaCfg = glbCfg.getObject("cungmua", null);
//        if (cungmuaCfg != null) {
//            cungmuaEnable = cungmuaCfg.getBoolean("enable", false);
//            cungmuaBussAddress = cungmuaCfg.getString("proxyBusAddress", "cungmuaVerticle");
//        }
        //END 0000000025 Cung mua
        //retailer for opt client
        otpClientDb = new OtpClientDb(vertx.eventBus(), logger);

        cardTypeDb = new CardTypeDb(vertx, logger);

        this.bankCfg = glbCfg.getObject(StringConstUtil.BANK_CONNECTOR_VERTICLE, new JsonObject());

        this.billRuleManageDb = new BillRuleManageDb(vertx, logger);

        this.ironManPromoGiftDB = new IronManPromoGiftDB(vertx, logger);

        this.sync_store_app_json_obj = glbCfg.getObject(StringConstUtil.SyncStoreApp.JSON_OBJECT, new JsonObject());

        JsonObject db_cfg = glbCfg.getObject("lstandby_database");
        Driver = db_cfg.getString("driver");
        Url = db_cfg.getString("url");
        Username = db_cfg.getString("username");
        Password = db_cfg.getString("password");

        dbProcess = new DBProcess(Driver
                , Url
                , Username
                , Password
                , AppConstant.LStandbyOracleVerticle_ADDRESS + "InfoProcess"
                , AppConstant.LStandbyOracleVerticle_ADDRESS + "InfoProcess"
                , logger);
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);

        VISA_GROUP = sbsCfg.getString("visa_group");
        VISA_CAPSET_ID = sbsCfg.getString("visa_capset_id");
        VISA_UPPER_LIMIT = sbsCfg.getString("visa_upper_limit");
        groupManageDb = new GroupManageDb(vertx, logger);

        banknetTransDb = new BanknetTransDb(vertx, logger);
        joMoMoTool = glbCfg.getObject(StringConstUtil.MOMO_TOOL.JSON_MOMO_TOOL, new JsonObject());

    }

    private void buildDataForC2CGetInfo(JsonArray arInfo, TextValueMsg.Builder builder) {

        String sndName = Misc.getInfoByKey(Const.C2C.senderName, arInfo);
        String sndPhone = Misc.getInfoByKey(Const.C2C.senderPhone, arInfo);
        String sndCardId = Misc.getInfoByKey(Const.C2C.senderCardId, arInfo);

        String rcvName = Misc.getInfoByKey(Const.C2C.receiverName, arInfo);
        String rcvPhone = Misc.getInfoByKey(Const.C2C.receiverPhone, arInfo);
        String rcvCardId = Misc.getInfoByKey(Const.C2C.receiverCardId, arInfo);

        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.C2C.senderName).setValue(sndName));
        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.C2C.senderPhone).setValue(sndPhone));
        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.C2C.senderCardId).setValue(sndCardId));

        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.C2C.receiverName).setValue(rcvName));
        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.C2C.receiverPhone).setValue(rcvPhone));
        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.C2C.receiverCardId).setValue(rcvCardId));
    }

    private void addTextValue(TextValueMsg.Builder builder, String key, String value) {
        builder.addKeys(MomoProto.TextValue.newBuilder().setText(key).setValue(value));
    }

    public void processGetOtpForClient(final NetSocket sock
            , final MomoMessage msg
            , final SockData data) {

        TextValueMsg textValueMsg;

        try {
            textValueMsg = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            textValueMsg = null;
        }

        final StandardReply.Builder builder = StandardReply.newBuilder();
        HashMap<String, String> hashMap = Misc.getKeyValuePairs(textValueMsg.getKeysList());

        String clientPhone = hashMap.containsKey(Const.DGD.CusNumber) ? hashMap.get(Const.DGD.CusNumber) : "";
        final String clientName = hashMap.containsKey(Const.DGD.CusName) ? hashMap.get(Const.DGD.CusName) : "";
        final String clientCardId = hashMap.containsKey(Const.DGD.CusCardId) ? hashMap.get(Const.DGD.CusCardId) : "";

        //app phai gui them 2 key
        //key cusname : ten vi can dinh danh
        //key cuscardid  : cmnd vi can dinh danh

        final int phoneNumer = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(clientPhone) + "");

        boolean checkCMND = checkCMND_user(clientCardId);

        if (textValueMsg == null || phoneNumer <= 0) {
            Buffer buffer = MomoMessage.buildBuffer(
                    MsgType.GET_OTP_FOR_CLIENT_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , builder.setResult(false)
                            .setRcode(1)
                            .setDesc("Dữ liệu không hợp lệ. Vui lòng kiểm tra lại")
                            .build().toByteArray()
            );
            mCommon.writeDataToSocket(sock, buffer);
            return;
        } else if (!checkCMND) {
            Buffer buffer = MomoMessage.buildBuffer(
                    MsgType.GET_OTP_FOR_CLIENT_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , builder.setResult(false)
                            .setRcode(1)
                            .setDesc("Dữ liệu CMND không hợp lệ. Vui lòng kiểm tra lại")
                            .build().toByteArray()
            );
            mCommon.writeDataToSocket(sock, buffer);
            return;
        }

        //CMND only use 3 time
        int countCMND = countCMNDOfUser(clientCardId, clientPhone);
        logger.debug("CMND " + clientCardId + " phone " + clientPhone + " indentify " + countCMND + " time");
        if (countCMND >= NUMBER_COUNT_CMND) {
            logger.debug("CMND " + clientCardId + " indentify fail");
            Buffer buffer = MomoMessage.buildBuffer(
                    MsgType.GET_OTP_FOR_CLIENT_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , builder.setResult(false)
                            .setRcode(1)
                            .setDesc("CMND đã được đăng ký 3 Ví")
                            .build().toByteArray()
            );
            mCommon.writeDataToSocket(sock, buffer);
            return;
        }
        logger.debug("CMND " + clientCardId + " indentify sucesss");
        otpClientDb.findOne(msg.cmdPhone, phoneNumer, new Handler<OtpClientDb.Obj>() {
            @Override
            public void handle(OtpClientDb.Obj obj) {

                //not exist otp yet
                if (obj == null) {
                    //save db
                    final String otp = DataUtil.getOtp();
                    OtpClientDb.Obj newObj = new OtpClientDb.Obj();
                    newObj.retailer = msg.cmdPhone;
                    newObj.customer_number = phoneNumer;
                    newObj.opt = otp;

                    otpClientDb.upsert(newObj, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {

                            //send sms to customer phone, reply to client
                            execGetOtpForClient(phoneNumer
                                    , otp
                                    , msg
                                    , builder
                                    , clientName
                                    , clientCardId
                                    , true
                                    , sock);
                        }
                    });
                } else {

                    long curTime = System.currentTimeMillis();
                    boolean isSendSms = (curTime >= (obj.time + 3 * 60 * 1000L) ? true : false);

                    //send sms to customer phone, reply to client
                    execGetOtpForClient(phoneNumer
                            , obj.opt
                            , msg
                            , builder
                            , clientName
                            , clientCardId
                            , isSendSms
                            , sock);

                    //update time for otp client
                    if (isSendSms) {
                        JsonObject joUp = new JsonObject();
                        joUp.putNumber(colName.RetailerOtpClient.time, System.currentTimeMillis());
                        otpClientDb.updatePartial(obj.id, joUp, null);
                    }
                }
            }
        });
    }

    private boolean checkCMND_user(String cmnd) {
        boolean isCMND = false;
        if ("".equalsIgnoreCase(cmnd) && !isStoreApp)
        {
            isCMND = true;
        }
        else if (cmnd.length() == 9 || cmnd.length() == 12) {
            String regex = "^[0-9]{9,12}";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(cmnd);
            if (matcher.matches()) {
                isCMND = true;
            }
        } else {
            isCMND = false;
        }

        return isCMND;

    }

    public int countCMNDOfUser(String CMND, String phone) {
        int countCMND = 0;
        try {
            countCMND = dbProcess.PRO_COUNT_ACCCOUNT(CMND, phone);
            return countCMND;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("CMND new exception " + e.getMessage());
            return 100;
        }
    }

    public int checkCMND3time(String CMND, String phone) {
        int check = 0;
        try {
            check = dbProcess.PRO_CHECK_ACCCOUNT(CMND, phone);
            return check;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Check CMND 3 time exception " + e.getMessage());
            return 0;
        }
    }

    /**
     * @param customerNumer the client phone number
     * @param otp           otp will be sent to phoneNumber
     * @param msg           the message content otp will be sent to custmomer phone number
     * @param builder       the build used to build message and send to client
     * @param sock          the current socket
     */

    private void execGetOtpForClient(final int customerNumer
            , final String otp
            , final MomoMessage msg
            , final StandardReply.Builder builder
            , final String customerName
            , final String customerCardId
            , boolean isSendSms
            , final NetSocket sock) {
        //send otp
        if (isSendSms) {

            final String cusName = Misc.removeAccent(customerName);
            final String tplStr_1 = "Ban dang duoc DGD %s dinh danh thong tin: Ten: %s, CMND %s. Gui ma %s cho DGD de dinh danh.";
//            final String tplStr = "Ban dang duoc %s(%s) dinh danh thong tin: Ten(%s), CMND(%s). Gui ma nay %s cho DGD de dinh danh";

            agentsDb.getOneAgent("0" + msg.cmdPhone, "execGetOptForClient", new Handler<AgentsDb.StoreInfo>() {
                @Override
                public void handle(AgentsDb.StoreInfo storeInfo) {
                    String storeName = storeInfo == null ? "" : storeInfo.storeName;
                    storeName = "".equalsIgnoreCase(storeName) ? "" : Misc.removeAccent(storeName);

                    String sms = String.format(tplStr_1
                            , "0" + msg.cmdPhone
                            , cusName
                            , customerCardId
                            , otp);
                    Misc.sendSms(vertx
                            , customerNumer
                            , sms);

                }
            });
        }

        //send result to client
        Buffer buffer = MomoMessage.buildBuffer(
                MsgType.GET_OTP_FOR_CLIENT_REPLY_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , builder.setResult(true)
                        .setRcode(0)
                        .setDesc("")
                        .build().toByteArray()
        );
        mCommon.writeDataToSocket(sock, buffer);
    }

    public void processGetC2cInfo(final NetSocket sock, final MomoMessage msg, final SockData data) {

        TextValueMsg getC2cInfo;
        try {
            getC2cInfo = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            getC2cInfo = null;
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);

        HashMap<String, String> hm = Misc.buildKeyValueHashMap(getC2cInfo.getKeysList());
        final String rcvCashCode = hm.containsKey("rcvcashcode") ? hm.get("rcvcashcode").trim() : "";
        final long amount = hm.containsKey(Const.AppClient.Amount) ? DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount).trim()) : 0;
        log.add("rcvCashCode", rcvCashCode);

        if (getC2cInfo == null || "".equalsIgnoreCase(rcvCashCode)) {
            mCommon.writeErrorToSocket(sock);
            log.writeLog();
            return;
        }

        final String pin = data == null ? "" : data.pin;
        if ("".equalsIgnoreCase(pin)) {
            mCommon.writeErrorToSocket(sock);
            log.add("pin in sockdata", pin);
            log.writeLog();
            return;
        }

        final TextValueMsg.Builder builder = TextValueMsg.newBuilder();

        mntDb.findOne(rcvCashCode, new Handler<MntDb.Obj>() {
            @Override
            public void handle(final MntDb.Obj obj) {

                //co thong tin trong cache memory
                if (obj != null && obj.checkfone == msg.cmdPhone && obj.time > (System.currentTimeMillis() - DEFAULT_CORE_TIMEOUT)) {

                    buildDataForC2CGetInfo(obj.shared, builder);

                    //khong dung so tien can nhan
                    if (amount != obj.amount) {
                        addTextValue(builder, Const.AppClient.Desciption, "Không đúng số tiền cần nhận");
                    } else {
                        //all ok and have cache
                        addTextValue(builder, Const.AppClient.Amount, String.valueOf(amount));
                        addTextValue(builder, Const.AppClient.Fee, "0");
                        addTextValue(builder, Const.AppClient.Desciption, "");
                    }

                    Buffer buffer = MomoMessage.buildBuffer(
                            MsgType.GET_C2C_INFO_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.build().toByteArray());

                    mCommon.writeDataToSocket(sock, buffer);
                    return;
                }
                //vao core check
                CoreCommon.doGetC2CInfo(vertx
                        , log
                        , "0" + msg.cmdPhone
                        , pin
                        , amount
                        , rcvCashCode
                        , DEFAULT_CORE_TIMEOUT, null, new Handler<Response>() {
                    @Override
                    public void handle(Response requestObj) {
                        long tid = requestObj != null ? requestObj.Tid : -1;
                        logger.info("C2C TID GET INFO " + tid );
                        if (requestObj.Error == 0) {

                            JsonObject joReq = new JsonObject();
                            joReq.putNumber("type", UMarketOracleVerticle.GET_C2C_INFO);
                            joReq.putString(UMarketOracleVerticle.fieldNames.MTCN, rcvCashCode);
                            joReq.putNumber("number", msg.cmdPhone);

//                            vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS, joReq, new Handler<Message<JsonArray>>() {
//                                @Override
//                                public void handle(Message<JsonArray> arrResult) { //FIX C2C tu L Sang U
                          vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, joReq, new Handler<Message<JsonArray>>() {
                                    @Override
                                    public void handle(Message<JsonArray> arrResult) {
                                    JsonArray arrInfo = arrResult.body();
                                    long amount = 0;
                                    String sndPhone = "";
                                    String rcvPhone = "";
                                    int status = -1;
                                    String description = "";
                                    for (int i = 0; i < arrInfo.size(); i++) {
                                        JsonObject jo = arrInfo.get(i);
                                        for (String s : jo.getFieldNames()) {
                                            //lay amount
                                            if (Const.AppClient.Amount.equalsIgnoreCase(s)) {
                                                amount = jo.getLong(s, 0);
                                            }

                                            //lay nguoi gui
                                            if (Const.C2C.senderPhone.equalsIgnoreCase(s)) {
                                                sndPhone = jo.getString(s, "");
                                            }

                                            //lay nguoi nhan
                                            if (Const.C2C.receiverPhone.equalsIgnoreCase(s)) {
                                                rcvPhone = jo.getString(s, "");
                                            }

                                            //Lay thong tin status tu ben MIS
                                            if (StringConstUtil.STATUS.equalsIgnoreCase(s)) {
                                                status = jo.getInteger(s, -1);
                                            }

                                            if (StringConstUtil.DESCRIPTION.equalsIgnoreCase(s)) {
                                                description = jo.getString(s, "");
                                            }
                                            break;
                                        }
                                    }

                                    if(status != 4 && !"".equalsIgnoreCase(description)) //MIS tra loi khong tra tien cho chau nay.
                                    {
                                        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.AppClient.Desciption)
                                                .setValue("GD nhận tiền đang tạm treo theo yêu cầu của Người gửi. Chi tiết: KH liên hệ Người gửi."));
                                        Buffer buffer = MomoMessage.buildBuffer(
                                                MsgType.GET_C2C_INFO_REPLY_VALUE,
                                                msg.cmdIndex,
                                                msg.cmdPhone,
                                                builder.build().toByteArray());

                                        mCommon.writeDataToSocket(sock, buffer);
                                        return;
                                    }

                                    if (!"".equalsIgnoreCase(sndPhone) && !"".equalsIgnoreCase(rcvPhone)) {
                                        buildDataForC2CGetInfo(arrInfo, builder);
                                        addTextValue(builder, Const.AppClient.Amount, String.valueOf(amount));
                                        addTextValue(builder, Const.AppClient.Fee, "0");
                                        addTextValue(builder, Const.AppClient.Desciption, "");

                                        //cap nhat so phone check
                                        JsonObject joUp = new JsonObject();
                                        joUp.putNumber(colName.MntCols.checkfone, msg.cmdPhone);

                                        MntDb.Obj mntObj = new MntDb.Obj();
                                        mntObj.checkfone = msg.cmdPhone;
                                        mntObj.code = rcvCashCode;
                                        mntObj.shared = arrInfo;
                                        mntObj.checkfone = msg.cmdPhone;
                                        mntObj.amount = amount;
                                        mntObj.time = System.currentTimeMillis();

                                        mntDb.save(mntObj, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {
                                            }
                                        });
                                    } else {
                                        builder.addKeys(MomoProto.TextValue.newBuilder()
                                                .setText(Const.AppClient.Desciption)
                                                .setValue("Không lấy được thông tin giao dịch."));
                                    }

                                    Buffer buffer = MomoMessage.buildBuffer(
                                            MsgType.GET_C2C_INFO_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.build().toByteArray());

                                    mCommon.writeDataToSocket(sock, buffer);
                                }
                            });
                            return;
                        }

                        String errDesc = "";
                        if (requestObj.Error == SoapError.AMOUNT_TOO_BIG
                                || requestObj.Error == SoapError.AMOUNT_TOO_SMALL
                                || requestObj.Error == SoapError.AMOUNT_OUT_OF_RANGE
                                || requestObj.Error == SoapError.INVALID_AMOUNT
                                ) {
                            errDesc = "Số tiền không chính xác. Vui lòng kiểm tra lại";
                        } else {
                            errDesc = SoapError.getDesc(requestObj.Error);
                        }

                        builder.addKeys(MomoProto.TextValue.newBuilder().setText(Const.AppClient.Desciption)
                                .setValue(errDesc));
                        Buffer buffer = MomoMessage.buildBuffer(
                                MsgType.GET_C2C_INFO_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.build().toByteArray());

                        mCommon.writeDataToSocket(sock, buffer);
                    }
                });
            }
        });
    }

    public void translateForm(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {

        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        if (fields == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }
        final HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());
        final String serviceId = hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : "";
        final String source = hashMap.containsKey(Const.AppClient.Source) ? hashMap.get(Const.AppClient.Source) : "";
        final String sourceFrom = hashMap.containsKey(Const.AppClient.SourceFrom) ? hashMap.get(Const.AppClient.SourceFrom) : source;
        final String serviceType = hashMap.containsKey(Const.AppClient.ServiceType) ? hashMap.get(Const.AppClient.ServiceType) : "";
        final String bookingType = hashMap.containsKey("bookingType") ? hashMap.get("bookingType") : "";
        final String appFee = hashMap.containsKey("vietjetfee") ? hashMap.get("vietjetfee") : "0";
        final String driverName = hashMap.containsKey("driverName") ? hashMap.get("driverName") : "";
        final long tip = DataUtil.stringToUNumber(hashMap.containsKey("tip") ? hashMap.get("tip") : "0");
//        hashMap.put("requestSource", String.valueOf(dbProcess.checkDCNTT("0" + msg.cmdPhone, logger, glbCfg.getObject("htpp_admin_database").getString("dcntt_group_ids", "58123, 59124"))));
//        if(serviceType.equalsIgnoreCase(StringConstUtil.Shopping.ServiceType))
//        {
//            MoMoInfoProcess moMoInfoProcess = new MoMoInfoProcess(vertx, logger, glbCfg);
//            moMoInfoProcess.loadJsonObjectForm(hashMap, msg, data, sock, mCommon, logger);
//            return;
//        }
        Misc.getTranslatedInfo(vertx, hashMap, serviceId, msg.cmdPhone, isStoreApp, new Handler<ArrayList<BillInfoService.TextValue>>() {
            @Override
            public void handle(final ArrayList<BillInfoService.TextValue> arrayList) {
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("0" + msg.cmdPhone);
                log.add("function", "processTransLateForm");

                for (int i = 0; i < arrayList.size(); i++) {
                    log.add(arrayList.get(i).text, arrayList.get(i).value);
                }
                log.add("listTranslateForm", arrayList.size());
                long amt = 0;
                if (hashMap.containsKey(Const.AppClient.Amount)) {
                    amt = DataUtil.stringToUNumber(hashMap.get(Const.AppClient.Amount));
                }

//                long amount_key = 0;
//                if (hashMap.containsKey(StringConstUtil.AMOUNT)) {
//                    amount_key = DataUtil.stringToUNumber(hashMap.get(StringConstUtil.AMOUNT));
//                }

                String dgd = "";
                if (hashMap.containsKey(Const.AppClient.Stores)) {
                    dgd = hashMap.get(Const.AppClient.Stores);
                }

                long amountKey = 0;
                if (hashMap.containsKey(StringConstUtil.AMOUNT)) {
                    amountKey = DataUtil.stringToUNumber(hashMap.get(StringConstUtil.AMOUNT));
                }
                final int qty = hashMap.containsKey(Const.AppClient.Quantity) ? DataUtil.strToInt(hashMap.get(Const.AppClient.Quantity)) : 1;

                final String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
                final boolean checkStoreApp = glbCfg.getBoolean(StringConstUtil.CHECK_STORE_APP, false);

                final String store = dgd;
                log.add("dgd", dgd);
                log.add("store", store);
                log.add("serviceType", serviceType);
                final long amt0fee = amountKey == 0 ? amt : amountKey;
                final long fAmt = arrayList.size() > 0 ? amt0fee * qty + tip : amt0fee  + tip;
//                log.writeLog();
                getServiceFee(serviceId, fAmt, log, new Handler<Long>() {
                            @Override
                            public void handle(final Long serviceFee) {

                                final long amount = fAmt + serviceFee;

                                TransferWithGiftContext.build(msg.cmdPhone, serviceId, billId, amount, vertx, giftManager, data,
                                        TRANWITHPOINT_MIN_POINT, TRANWITHPOINT_MIN_AMOUNT, logger,
                                        new Handler<TransferWithGiftContext>() {
                                            @Override
                                            public void handle(final TransferWithGiftContext context) {
                                                context.writeLog(logger);

                                                //long freeAmt = context.voucher + context.point;
                                                //context.voucher;
                                                //context.point;
                                                //context.momorcvPhone
                                                log.add("desc ", "TransferWithGift");
                                                final long momo = (context.momo < 0 ? 0 : context.momo);
                                                final Common.ServiceReq serviceReq = new Common.ServiceReq();
                                                serviceReq.ServiceId = serviceId;
                                                serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
                                                log.add("desc", "getServiceInfo");
//                                                log.writeLog();
                                                Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
                                                    @Override
                                                    public void handle(JsonArray listServices) {
                                                        String nameService = "";
                                                        for(int i = 0; i < listServices.size(); i++)
                                                        {
                                                            String serId = ((JsonObject) listServices.get(i)).getString(colName.ServiceCols.SERVICE_ID, "");
                                                            if(serviceId.equalsIgnoreCase(serId))
                                                            {
                                                                nameService = ((JsonObject) listServices.get(i)).getString(colName.ServiceCols.SERVICE_NAME, serId);
                                                                break;
                                                            }
                                                            else {
                                                                nameService = "";
                                                            }
                                                        }
                                                        log.add("Service Name", nameService);
                                                        final String serviceName = nameService.equalsIgnoreCase("") ? serviceId : nameService;
                                                        log.add("method", "getFee");
//                                                        log.writeLog();mo.
                                                        // lay phi tu banknet
                                                        final JsonObject bankFeeRequest = buildFeeFromBankReq("0" + msg.cmdPhone, hashMap, momo);
                                                        getFeeFromBankNew(bankFeeRequest, log, new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject bankFeeRes) {
                                                                final long banknetFee = bankFeeRes.getLong("totalfee", 0L);
                                                                // fee visa
                                                                String bankId = bankFeeRequest.getString(BANKID, "");
                                                                getFee(sourceFrom, bankId, momo, log, new Handler<Long>() {
                                                                    @Override
                                                                    public void handle(final Long visaFee) {
                                                                        boolean isNewForm = false;
                                                                        long totalFee = visaFee + banknetFee + serviceFee + Long.parseLong(appFee);
                                                                        if ("vietjetairNew".equals(serviceId)) {
                                                                            arrayList.add(Misc.buildTextValue("Nhà cung cấp", serviceName));
                                                                            arrayList.add(Misc.buildTextValue("Loại vé", bookingType));
                                                                            arrayList.add(Misc.buildTextValue("Số lượng", qty + ""));
                                                                            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt0fee).replaceAll(",", ".") + "đ" + ""));
                                                                            // khong bao gom appFee
                                                                            arrayList.add(Misc.buildTextValue(StringConstUtil.PHI_GIAO_DICH_FROM_CARD, Misc.formatAmount(Math.max(visaFee, banknetFee)).replaceAll(",", ".") + "đ"));
                                                                            isNewForm = true;
                                                                        } else if ("taxi".equals(serviceId)) {
                                                                            arrayList.add(Misc.buildTextValue("Họ tên lái xe", driverName));
                                                                            arrayList.add(Misc.buildTextValue("Cước Taxi", Misc.formatAmount(amt0fee).replaceAll(",", ".") + "đ" + ""));
                                                                            arrayList.add(Misc.buildTextValue("Tip", Misc.formatAmount(tip).replaceAll(",", ".") + "đ" + ""));
                                                                            arrayList.add(Misc.buildTextValue(StringConstUtil.PHI_GIAO_DICH, Misc.formatAmount(totalFee).replaceAll(",", ".") + "đ"));
                                                                        } else {
                                                                            arrayList.add(Misc.buildTextValue(StringConstUtil.PHI_GIAO_DICH_FROM_CARD, Misc.formatAmount(Math.max(visaFee, banknetFee)).replaceAll(",", ".") + "đ"));
                                                                        }

                                                                        if (!store.equalsIgnoreCase("1") || !checkStoreApp) {
                                                                            if (!"taxi".equalsIgnoreCase(serviceId)) {
                                                                                arrayList.add(Misc.buildTextValue("Quà tặng", Misc.formatAmount(context.voucher).replaceAll(",", ".") + "đ"));
                                                                            } else {
                                                                                arrayList.add(Misc.buildTextValue("Thẻ quà tặng", Misc.formatAmount(context.voucher).replaceAll(",", ".") + "đ"));
                                                                            }
//                                                            arrayList.add(Misc.buildTextValue("Tài khoản khuyến mãi", Misc.formatAmount(context.point).replaceAll(",", ".") + "đ"));
                                                                        }

                                                                        if (!isNewForm) {
                                                                            arrayList.add(Misc.buildTextValue(StringConstUtil.TONG_CHI_PHI, Misc.formatAmount(fAmt + totalFee).replaceAll(",", ".") + "đ"));
                                                                        }
//                                                                else {
//                                                            arrayList.add(Misc.buildTextValue(StringConstUtil.TONG_CHI_PHI, Misc.formatAmount(amt0fee + totalFee).replaceAll(",", ".") + "đ"));
//                                                                }

//                                    arrayList.add(Misc.buildTextValue("Tài khoản MoMo", Misc.formatAmount(momo).replaceAll(",", ".") + "đ"));
//                                    arrayList.add(Misc.buildTextValue("Phí giao dịch", Misc.formatAmount(fee).replaceAll(",", ".") + "đ"));
//                                    arrayList.add(Misc.buildTextValue("Tổng tiền", Misc.formatAmount(fAmt + fee).replaceAll(",", ".") + "đ"));
                                                                        //arrayList.add(Misc.buildTextValue(Const.AppClient.TotalMomo,String.valueOf(momo)));
                                                                        boolean isValidOS = checkOS(data);
                                                                        log.add("isValidOs", isValidOS);
                                                                        log.add("app code", data.appCode);
                                                                        log.add("app version", data.appVersion);
                                                                        log.add("change money", context.remainVoucher);
                                                                        log.add("app os", data.os);
                                                                        if (context.remainVoucher > 0 && isValidOS && context.voucher > 0 && !isNewForm) {
                                                                            arrayList.add(Misc.buildTextValue(StringConstUtil.GIFT_ALERT_KEY, String.format(StringConstUtil.GIFT_ALERT_VALUE, Misc.formatAmount(context.voucher + context.remainVoucher).replaceAll(",", "."),
                                                                                    Misc.formatAmount(fAmt + totalFee).replaceAll(",", "."), Misc.formatAmount(context.remainVoucher).replaceAll(",", ".")) + ""));
                                                                        }
                                                                        else if (context.remainVoucher > 0 && isValidOS && context.voucher > 0 && isNewForm) {
                                                                            arrayList.add(Misc.buildTextValue(StringConstUtil.GIFT_ALERT_KEY, String.format(StringConstUtil.GIFT_ALERT_VALUE, Misc.formatAmount(context.voucher + context.remainVoucher).replaceAll(",", "."),
                                                                                    Misc.formatAmount(amt0fee + Math.max(visaFee, banknetFee)).replaceAll(",", "."), Misc.formatAmount(context.remainVoucher).replaceAll(",", ".")) + ""));
                                                                        }
                                                                        if (serviceType.equalsIgnoreCase(StringConstUtil.Shopping.ServiceType)) {
                                                                            log.add("serviceType ", "shopping");
                                                                            MoMoInfoProcess moMoInfoProcess = new MoMoInfoProcess(vertx, logger, glbCfg);
                                                                            hashMap.put("serviceFee", serviceFee + "");
                                                                            hashMap.put("cashSourceFee", Math.max(visaFee, banknetFee) + "");
                                                                            moMoInfoProcess.loadJsonObjectForm(hashMap, msg, data, sock, mCommon, logger, context.voucher, context.point, totalFee, fAmt + totalFee, checkStoreApp, context.remainVoucher);
                                                                            //log.writeLog();
                                                                            return;
                                                                        } else {
                                                                            log.add("reply to client ", "0" + msg.cmdPhone);
                                                                            Misc.sendTranslateToClient(msg, sock, arrayList, mCommon, log);
                                                                        }
                                                                        log.writeLog();
                                                                    } // end service fee
                                                                });
                                                                //end fee
                                                            }
                                                        });

                                                    }

                                                });

                                            }

                                        }
                                );
                            }
                        }
                );
            }
        });
    }

    private JsonObject buildFeeFromBankReq(String phoneNumber, final HashMap<String, String> hashMap, long newAmount) {
        final String bankId = hashMap.containsKey(BANKID) ? hashMap.get(BANKID) : "";
        final String src = hashMap.containsKey(Const.AppClient.Source) ? hashMap.get(Const.AppClient.Source) : "";
        final String srcFrom = hashMap.containsKey(SRCFROM) ? hashMap.get(SRCFROM) : src;
        int transType = hashMap.containsKey(TRANSTYPE) ? DataUtil.strToInt(hashMap.get(TRANSTYPE)) : DataUtil.strToInt("0");
        long amount = newAmount;
        String numberReceiveM2M = hashMap.containsKey(NUMBERRECEIVEM2M) ? hashMap.get(NUMBERRECEIVEM2M) : "";

        int channel = hashMap.containsKey(CHANNEL) ? DataUtil.strToInt(hashMap.get(CHANNEL)) : 0;
        int iocity = hashMap.containsKey(IOCITY) ? DataUtil.strToInt(hashMap.get(IOCITY)) : 0;
        final JsonObject jsonInfo = new JsonObject();

        /*
        {
            "bankid": "",
            "srcfrom": "",
            "transtype": 1,
            "amount": 100000,
            "numberReceiveM2M": "",
            "channel": 0,
            "ioCity": 0
        }
         */
        jsonInfo.putString(BANKID, bankId);
        jsonInfo.putString(SRCFROM, srcFrom);
        jsonInfo.putNumber(TRANSTYPE, transType);
        jsonInfo.putNumber(AMOUNT, amount);
        jsonInfo.putString(NUMBERRECEIVEM2M, numberReceiveM2M);
        jsonInfo.putNumber(CHANNEL, channel);
        jsonInfo.putNumber(IOCITY, iocity);
        jsonInfo.putString(StringConstUtil.NUMBER, phoneNumber);
        return jsonInfo;
    }

    private boolean checkOS(SockData data)
    {
        boolean isNewApp = false;
        if(StringConstUtil.ANDROID_OS.equalsIgnoreCase(data.os) && data.appCode > 52 )
        {
            isNewApp = true;
        }
        else if(StringConstUtil.IOS_OS.equalsIgnoreCase(data.os) && data.appCode > 1918)
        {
            isNewApp = true;
        }
        return  isNewApp;
    }

    private void getFee(String sourceFrom, String bank, final long cashInAmount, final Common.BuildLog log, final Handler<Long> callback) {

        log.add("func", "getFee");
        log.add("sourceFrom", sourceFrom);
        log.add("cashInAmount", cashInAmount);
        String bankId = VMFeeType.getBankId(VMFeeType.TRANSFER_FEE_TYPE);
        if("".equalsIgnoreCase(bank) && DataUtil.strToInt(sourceFrom) == 3)
        {
            log.add("-------", "Nguon visa nhung app gui thieu thong tin bankId");
        }
        else if("".equalsIgnoreCase(bank) && DataUtil.strToInt(sourceFrom) == 5)
        {
            log.add("-------", "Nguon banknet nhung app gui thieu thong tin bankId");
            bankId = "999999";
        }
        else if (!(("sbs".equalsIgnoreCase(bank) || "sbstransfer".equalsIgnoreCase(bank)) && "3".equalsIgnoreCase(sourceFrom))) {
            callback.handle(0L);
            log.add("-------", "khong phai nguon visa master");
            log.writeLog();
            return;
        }
        log.add("-------", "nguon visa master");

        if (0 == cashInAmount) {
            callback.handle(0L);
            log.add("-------", "cashInAmount = 0 => visa fee = 0");
            log.writeLog();
            return;
        }


        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_FEE;
        serviceReq.bankId = bankId;
        serviceReq.channel = 0;
        serviceReq.inoutCity = 0;
        serviceReq.tranType = 0;
        log.add("bankId", bankId);
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                FeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new FeeDb.Obj(message.body());
                }
                int static_fee;
                double dynamic_fee;

                if (obj == null) {
                    static_fee = 3200;
                    dynamic_fee = 2.4;
                } else {
                    static_fee = obj.STATIC_FEE;
                    dynamic_fee = obj.DYNAMIC_FEE;
                }

                log.add("static_fee: ", static_fee);
                log.add("dynamic_fee: ", dynamic_fee);
                log.add("amount: ", cashInAmount);
                long fee = 0;
                if (cashInAmount != 0) {
                    fee = VMFeeType.calculateFeeMethod(static_fee, dynamic_fee, cashInAmount);
                }
                log.add("fee", fee);
                callback.handle(fee);
            }
        });
    }

    public void processGetBillInfo(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<String> callback) {
        GetBillInfo request;
        try {
            request = GetBillInfo.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasProviderId() || !request.hasBillId()) {
            if (sock != null)
                mCommon.writeErrorToSocket(sock);
            return;
        }

        if("".equalsIgnoreCase(request.getProviderId()) && "".equalsIgnoreCase(request.getBillId()) && isStoreApp)
        {
            if (sock != null)
                mCommon.writeErrorToSocket(sock);
            return;
        }

        if (request.getGetBillInfoAction() == GetBillInfoAction.GET_ONE_VALUE) {
            // todo: get bill info as usual

            //????
            getOneBill(request, sock, msg, data, GetBillInfoAction.GET_ONE, callback);
        } else if (request.getGetBillInfoAction() == GetBillInfoAction.GET_SAVED_VALUE) {
            getSavedBills(request, sock, msg, data, callback);
        } else if (request.getGetBillInfoAction() == GetBillInfoAction.SYNC_VALUE) {
            syncBills(request, sock, msg, data, callback);
        }

    }

    public void processGetBalance(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<String> callback) {
        TextValueMsg request;
        try {
            request = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        /*//key     value
        //momo   tienmomo
        //vc      tien voucher
        //point   tien point
        //mload   tien mload*/
        //HashMap<String,String> map = Misc.buildKeyValueHashMap(request.getKeysList());

        final TextValueMsg.Builder builder = TextValueMsg.newBuilder();

        if (request.getKeysCount() == 0) {
            builder.addKeys(Misc.getTextValueBuilder("momo", "0"));
            builder.addKeys(Misc.getTextValueBuilder("vc", "0"));
            builder.addKeys(Misc.getTextValueBuilder("point", "0"));
            builder.addKeys(Misc.getTextValueBuilder("mload", "0"));

            Buffer buf = MomoMessage.buildBuffer(
                    MsgType.GET_BALANCE_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build()
                            .toByteArray()
            );

            mCommon.writeDataToSocket(sock, buf);
            return;
        }

        Queue<String> queueServiceId = new LinkedList<>();
        for (int i = 0; i < request.getKeysCount(); i++) {
            queueServiceId.add(request.getKeys(i).getValue());
        }

        TransferWithGiftContext.build(msg.cmdPhone
                , queueServiceId
                , ""
                , 100
                , vertx
                , giftManager
                , data
                , TRANWITHPOINT_MIN_POINT
                , TRANWITHPOINT_MIN_AMOUNT, logger, new Handler<TransferWithGiftContext>() {
            @Override
            public void handle(TransferWithGiftContext context) {

                builder.addKeys(Misc.getTextValueBuilder("momo", "0"));
                builder.addKeys(Misc.getTextValueBuilder("vc", String.valueOf(context.voucher + context.remainVoucher)));
                builder.addKeys(Misc.getTextValueBuilder("point", String.valueOf(context.point)));
                builder.addKeys(Misc.getTextValueBuilder("mload", "0"));
                if (context.error != 0) {
                    logger.info("BALANCE IS ERROR " + context.error);
                    return;
                }
                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.GET_BALANCE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build()
                                .toByteArray()
                );
                mCommon.writeDataToSocket(sock, buf);
//                JsonArray jarrBalance = new JsonArray().add(new JsonObject().putNumber(StringConstUtil.BALANCE_TOTAL.MOMO, context.curMomo))
//                        .add(new JsonObject().putNumber(StringConstUtil.BALANCE_TOTAL.GIFT, context.curGift))
//                        .add(new JsonObject().putNumber(StringConstUtil.BALANCE_TOTAL.POINT, context.curPoint));
//                JsonObject joUpdate = new JsonObject().putString(colName.PhoneDBCols.BALANCE_TOTAL, jarrBalance.toString());
//                phonesDb.updatePartial(msg.cmdPhone, joUpdate, new Handler<PhonesDb.Obj>() {
//                    @Override
//                    public void handle(PhonesDb.Obj event) {
//
//                    }
//                });
            }
        });

    }

    public void getOneBill(final GetBillInfo request
            , final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final GetBillInfoAction action
            , final Handler<String> callback) {

        Buffer getBillBuf = MomoMessage.buildBuffer(
                SoapProto.MsgType.GET_BILL_INFO_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                SoapProto.GetBillInfo.newBuilder()
                        .setMpin(data.pin)
                        .setBillId(request.getBillId())
                        .setProviderId(request.getProviderId())
                        .build()
                        .toByteArray()
        );

        final String provideId = request.getProviderId();
        final String billId = Misc.refineBillId(provideId, request.getBillId(), "pe");

        logger.debug("processGetBillInfo : " + request.getProviderId() + "," + request.getBillId());

        //send to soap
        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, getBillBuf, new Handler<Message<JsonObject>>() {
            @Override

            /*JsonObject obj = new JsonObject();
            obj.putString("billInfo",billInfoStr);
            obj.putNumber("rcode",result_code);
            message.reply(obj);*/

            public void handle(Message<JsonObject> result) {
                boolean isOk = false;
                int rcode = GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE;
                String billInfoStr = "";

                if (result.body() != null) {
                    rcode = result.body().getInteger("rcode");
                    billInfoStr = result.body().getString("billInfo");
                }

                if (rcode == GetBillInfoReply.ResultCode.ALL_OK_VALUE) {
                    isOk = true;
                }

                GetBillInfoReply.Builder builder = GetBillInfoReply.newBuilder();
                builder.setProviderId(request.getProviderId());
                builder.setBillId(billId);
                builder.setAction(action);

                Buffer buf = null;
                JsonObject jsonBillInfo = new JsonObject();

                if (isOk) {
                    //Pham Thanh Duc,,~HNG1798612,379500,28/02/2014,28/02/2014~
                    String[] billItems = billInfoStr.split("~");
                    if (billItems.length >= 1 && billItems[0].trim().length() > 0) {
                        //thong tin khach hang
                        String[] cusInfo = billItems[0].split(",");
                        //thông tin khách hàng=họ tên,địa chỉ,số đt
                        if (cusInfo.length >= 1) {
                            builder.setName(cusInfo[0]);
                        }
                        if (cusInfo.length >= 2) {
                            builder.setAddress(cusInfo[1]);
                        }
                        if (cusInfo.length >= 3) {
                            builder.setPhone(cusInfo[2]);
                        }

                        if (billItems.length >= 2 && billItems[1].trim().length() > 0) {
                            String billInfo = billItems[1];
                            String[] bills = billInfo.split("#");
                            long amount = 0;
                            for (String b : bills) {
                                //HNG1798612,379500,28/02/2014,28/02/2014
                                if (b.trim().length() > 0) {
                                    //todo we have one valid bill here
                                    String[] ar = b.split(",");
                                    int arLen = ar.length;
                                    builder.addBills(
                                            BillDetail.newBuilder()
                                                    .setBillId(arLen > 0 ? ar[0] : "")
                                                    .setAmount(arLen > 1 ? DataUtil.stringToUNumber(ar[1]) : 0)
                                                    .setFromDate(arLen > 2 ? ar[2] : "")
                                                    .setToDate(arLen > 3 ? ar[3] : "")
                                    );

                                    //thông tin hóa đơn=mã hóa đơn,số tiền,từ ngày,đến ngày.
                                    amount += (arLen > 1 ? DataUtil.stringToUNumber(ar[1]) : 0);
                                }
                            }

                            final long famount = amount;

                            jsonBillInfo.putString(colName.BillDBCols.PROVIDER_ID, provideId);
                            jsonBillInfo.putString(colName.BillDBCols.BILL_ID, billId);
                            jsonBillInfo.putNumber(colName.BillDBCols.TOTAL_AMOUNT, famount);

                            mBillsDb.setBillAmount(msg.cmdPhone
                                    , jsonBillInfo
                                    , new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {
                                            logger.debug("Set billAount = Total amount =" + famount + " from DB, providerId :"
                                                    + provideId
                                                    + " billId tong " + billId);
                                        }
                                    }
                            );
                            //todo we do nothing for extra info atm
                        }
                    } else {
                        isOk = false;
                    }

                    buf = MomoMessage.buildBuffer(
                            MsgType.GET_BILL_INFO_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.setResult(isOk)
                                    .setRcode(rcode)
                                    .build()
                                    .toByteArray()
                    );

                    if (callback != null)
                        callback.handle(billInfoStr);

                } else {
                    buf = MomoMessage.buildBuffer(
                            MsgType.GET_BILL_INFO_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.setResult(false)
                                    .setRcode(rcode)
                                    .build()
                                    .toByteArray()
                    );
                    if (callback != null)
                        callback.handle(null);
                }

                if (sock != null)
                    mCommon.writeDataToSocket(sock, buf);
            }
        });
    }

    public void getSavedBills(GetBillInfo request
            , final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<String> callback) {

        //todo: Get bills from mongo and compare it with soapin.
        BillsDb.Obj obj = new BillsDb.Obj();
        obj.payChanel = 0;
        mBillsDb.getAllBills(msg.cmdPhone, obj, new Handler<ArrayList<BillsDb.Obj>>() {
            @Override
            public void handle(final ArrayList<BillsDb.Obj> savedBills) {
                if (savedBills.size() == 0) {
                    logger.debug(String.format("%d request getSavedBills but empty.", msg.cmdPhone));
                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.NO_MORE_BILLS_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            null
                    );
                    if (sock != null)
                        mCommon.writeDataToSocket(sock, buf);

                    return;
                }
                long createdTimerId = vertx.setPeriodic(1000, new Handler<Long>() {
                    @Override
                    public void handle(Long timerId) {
                        if (savedBills.size() > 0) {
                            logger.debug("bill list size " + savedBills.size());
                            BillsDb.Obj obj = savedBills.remove(0);
                            logger.debug("bill id " + obj.billId);

                            //TODO dont know why we need that
                            BillDetail.Builder billDetail = BillDetail.newBuilder();
                            billDetail.setAmount(obj.totalAmount <= 0 ? 0 : obj.totalAmount);

                            GetBillInfoReply.Builder replyBody = GetBillInfoReply.newBuilder();
                            replyBody.setAction(GetBillInfoAction.GET_SAVED);

                            replyBody.setAddress(obj.ownerAddress);
                            replyBody.setName(obj.ownerName);
                            replyBody.setPhone(obj.ownerPhone);
                            replyBody.setProviderId(obj.providerId);
                            replyBody.setBillId(obj.billId);

                            replyBody.addBills(billDetail);

                            Buffer buf = MomoMessage.buildBuffer(
                                    MsgType.GET_BILL_INFO_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    replyBody.setResult(true)
                                            .setRcode(GetBillInfoReply.ResultCode.ALL_OK_VALUE)
                                            .build()
                                            .toByteArray()
                            );
                            if (sock != null)
                                mCommon.writeDataToSocket(sock, buf);
                            logger.debug(String.format("Reply %d saved bill {providerId: %s, billId: %s}.", msg.cmdPhone, obj.providerId, obj.billId));
                        } else {
                            vertx.cancelTimer(timerId);
                            Buffer buf = MomoMessage.buildBuffer(
                                    MsgType.NO_MORE_BILLS_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    null
                            );
                            if (sock != null)
                                mCommon.writeDataToSocket(sock, buf);

                            logger.debug(String.format("Cancel getSavedBill timer(%d) for %d.", timerId, msg.cmdPhone));
                        }
                    }
                });
                logger.debug(String.format("Created timer{id: %d} to reply savedBills to %d", createdTimerId, msg.cmdPhone));
            }
        });

    }

    public void syncBills(final GetBillInfo request, final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<String> callback) {

        BillsDb.Obj obj = new BillsDb.Obj();
        obj.payChanel = 0;
        mBillsDb.getAllBills(msg.cmdPhone, obj, new Handler<ArrayList<BillsDb.Obj>>() {
            @Override
            public void handle(final ArrayList<BillsDb.Obj> savedBills) {
                if (savedBills == null || savedBills.size() == 0) {
                    logger.debug(String.format("%d request getSavedBills but empty.", msg.cmdPhone));
                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.NO_MORE_BILLS_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            null
                    );
                    if (sock != null)
                        mCommon.writeDataToSocket(sock, buf);
                    return;
                }
                long createdTimerId = vertx.setPeriodic(1000, new Handler<Long>() {

                    boolean sendNextBill = true;

                    @Override
                    public void handle(Long timerId) {
                        if (savedBills.size() > 0) {
                            if (sendNextBill) {// if the last bill wasn't sent, skip this round.
                                BillsDb.Obj obj = savedBills.remove(0);

                                GetBillInfo requestBody = GetBillInfo.newBuilder()
                                        .setGetBillInfoAction(GetBillInfoAction.GET_ONE_VALUE)
                                        .setProviderId(obj.providerId)
                                        .setBillId(obj.billId)
                                        .build();

                                sendNextBill = false;
                                getOneBill(requestBody, sock, msg, data, GetBillInfoAction.SYNC, new Handler<String>() {
                                    @Override
                                    public void handle(String event) {
                                        sendNextBill = true;
                                    }
                                });
                            }
                        } else {

                            Buffer buf = MomoMessage.buildBuffer(
                                    MsgType.NO_MORE_BILLS_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    null
                            );
                            if (sock != null)
                                mCommon.writeDataToSocket(sock, buf);

                            logger.debug(String.format("Cancel synBills timer(%d) for %d.", timerId, msg.cmdPhone));
                            vertx.cancelTimer(timerId);
                        }
                    }
                });
                logger.debug(String.format("Created timer{id: %d} to reply synBills to %d", createdTimerId, msg.cmdPhone));
            }
        });
    }

    //
    public void validateBill(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> callback) {

        TextValueMsg checkBill = null;
        try {
            checkBill = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            checkBill = null;
        }

        if (checkBill == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        HashMap<String, String> hashMapTVs = Misc.getKeyValuePairs(checkBill.getKeysList());

        final StandardReply.Builder builder = StandardReply.newBuilder();
        builder.setResult(false);
        builder.setRcode(TranHisV1.ResultCode.CUSTOM_ERROR_VALUE);

        final JsonObject json = new JsonObject();
        json.putNumber("error", TranHisV1.ResultCode.CUSTOM_ERROR_VALUE);
        json.putBoolean("result", false);

        final String serviceId = hashMapTVs.containsKey(Const.ValidBill.ServiceId) ? hashMapTVs.get(Const.ValidBill.ServiceId) : "";
        final String billId = hashMapTVs.containsKey(Const.ValidBill.BillId) ? hashMapTVs.get(Const.ValidBill.BillId) : "";
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("serviceId", serviceId);
        log.add("billId", billId);

        if ("".equalsIgnoreCase(serviceId)) {
            builder.setDesc("Mã dịch vụ không hợp lệ.");
            json.putString("desc", "Mã dịch vụ không hợp lệ.");
            Buffer buf = MomoMessage.buildBuffer(
                    MsgType.CHECK_BILL_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build()
                            .toByteArray()
            );

            mCommon.writeDataToSocket(sock, buf);

            if (callback != null) {
                callback.handle(json);
            }

            log.writeLog();

            saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.InvalidServiceId, CDHHErr.Error.InvalidServiceId, 0);
            return;
        }

        if ("capdoihoanhao".equalsIgnoreCase(serviceId)) {

            int code = DataUtil.strToInt(billId);
            boolean codeInValid = (code > cdhh_max_code || code < cdhh_min_code);

            if (codeInValid) {

                builder.setDesc("Sai SBD bình chọn.");
                json.putString("desc", "Sai SBD bình chọn.");

                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.CHECK_BILL_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build()
                                .toByteArray()
                );

                mCommon.writeDataToSocket(sock, buf);

                if (callback != null) {
                    callback.handle(json);
                }

                log.writeLog();

                saveTrackDDHHErr(msg.cmdPhone, CDHHErr.Desc.InvalidCode, CDHHErr.Error.InvalidCode, 0);
                return;
            }

            com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
            serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_CDHH_ON_OFF;
            vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<Boolean>>() {
                @Override
                public void handle(Message<Boolean> message) {
                    //todo he thong chua mo cua
                    if (message.body() == false) {

                        builder.setDesc("Hệ thống đóng cửa.");
                        json.putString("desc", "Hệ thống đóng cửa.");
                        Buffer buf = MomoMessage.buildBuffer(
                                MsgType.CHECK_BILL_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.build()
                                        .toByteArray()
                        );

                        mCommon.writeDataToSocket(sock, buf);

                        if (callback != null) {
                            callback.handle(json);
                        }
                        return;
                    }

                    builder.setRcode(0);
                    builder.setResult(true);
                    builder.setDesc("");

                    json.putBoolean("result", true);
                    json.putNumber("error", 0);
                    json.putString("desc", "");

                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.CHECK_BILL_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.build()
                                    .toByteArray()
                    );

                    mCommon.writeDataToSocket(sock, buf);

                    if (callback != null) {
                        callback.handle(json);
                    }
                    log.writeLog();

                }
            });

            return;
        }


        builder.setRcode(0);
        builder.setResult(true);
        builder.setDesc("");

        json.putBoolean("result", true);
        json.putNumber("error", 0);
        json.putString("desc", "");

        Buffer buf = MomoMessage.buildBuffer(
                MsgType.CHECK_BILL_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build()
                        .toByteArray()
        );

        mCommon.writeDataToSocket(sock, buf);

        if (callback != null) {
            callback.handle(json);
        }
        log.writeLog();
    }

    /*public void processBillSync(final NetSocket sock, final MomoMessage msg) {
        MomoProto.BillSync request;
        try {
            request = MomoProto.BillSync.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasPayChanel()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        BillsDb.Obj obj = createObj(request);

        switch (request.getType()) {
            case MomoProto.BillSync.SyncType.INS_VALUE:
                mBillsDb.addBill(msg.cmdPhone, obj, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.BILL_SYNC_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.BillSyncReply.newBuilder()
                                        .setResult(aBoolean)
                                        .build().toByteArray()
                        );
                        mCommon.writeDataToSocket(sock, buf);
                    }
                });
                break;

            case MomoProto.BillSync.SyncType.DEL_VALUE:
                mBillsDb.deleteBill(msg.cmdPhone, obj, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.BILL_SYNC_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.BillSyncReply.newBuilder()
                                        .setResult(aBoolean)
                                        .build().toByteArray()
                        );
                        mCommon.writeDataToSocket(sock, buf);
                    }
                });
                break;
            case MomoProto.BillSync.SyncType.GET_VALUE:
                mBillsDb.getAllBills(msg.cmdPhone, obj, new Handler<ArrayList<BillsDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<BillsDb.Obj> jarray) {

                        MomoProto.BillSyncReply.Builder builder = MomoProto.BillSyncReply.newBuilder();

                        if (jarray != null) {
                            for (int i = 0; i < jarray.size(); i++) {
                                BillsDb.Obj o = jarray.get(i);
                                builder.addBills(MomoProto.BillSync.newBuilder()
                                                .setBillId(o.billId)
                                                .setProviderId(o.providerId)
                                                .setBillDetail(o.billDetail)
                                                .setStatus(o.status)
                                                .setOwnerName(o.ownerName)
                                                .setOwnerAddress(o.ownerAddress)
                                                .setOwnerPhone(o.ownerPhone)
                                                .setTotalAmount(o.totalAmount)
                                                .setPayType(o.payType)
                                                .setTransferType(o.tranferType)
                                                .setDueDate(o.dueDate)
                                                .setPayScheduler(o.payScheduler)
                                                .setPayChanel(o.payChanel)
                                );
                            }
                        }

                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.BILL_SYNC_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.setResult(true)
                                        .build()
                                        .toByteArray()
                        );
                        mCommon.writeDataToSocket(sock, buf);
                    }
                });
                break;
            default:
                break;
        }
    }*/

    private void saveTrackDDHHErr(int number, String desc, String error, int voteAmount) {

        CDHHErr.Obj cdhhErrObj = new CDHHErr.Obj();
        cdhhErrObj.desc = desc;
        cdhhErrObj.error = error;
        cdhhErrObj.number = number;
        cdhhErrObj.voteAmount = voteAmount;
        cdhhErr.save(cdhhErrObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });
    }

    public void processCheckPhone(final NetSocket sock, final MomoMessage msg) {

        PhoneExist request;
        logger.info("method processCheckPhone ");
        try {
            logger.info("try request PhoneExist ");
            request = PhoneExist.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.info("try request PhoneExist ");
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }
        logger.info("request.hasNumber() " + request.hasNumber());
        if (request == null || !request.hasNumber()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final int phoneNumber = DataUtil.strToInt(request.getNumber());
        logger.info("phoneNumber " + phoneNumber);
        final AgentInfo.Builder replyBuilder = AgentInfo.newBuilder();

        agentsDb.getOneAgent("0" + phoneNumber, "processCheckPhone", new Handler<AgentsDb.StoreInfo>() {
            @Override
            public void handle(final AgentsDb.StoreInfo storeInfo) {
                final String dgd = (((storeInfo != null) && (storeInfo.deleted == false)) ? "1" : "0");

                phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj obj) {
                        if (obj != null) {
                            logger.info("phone obj check exist not null");
                            // Get group of agent
                            Set<Integer> groupList = dbProcess.getAgentGroupId("0" + phoneNumber, logger);
                            logger.info(groupList.size());
                            for(int i = 0; i < groupList.size(); i++)
                            {
                                logger.info("group id: " + groupList.toArray()[i].toString());
                            }
                            replyBuilder.setResult(true)
                                    .setName(storeInfo != null ? storeInfo.storeName : obj.name)
                                    .setCardId(obj.cardId)
                                    .addListKeyValue(TextValue.newBuilder().setText(Const.DGD.Retailer).setValue(dgd))
                                    .addListKeyValue(TextValue.newBuilder().setText(Const.DGD.GroupId).setValue(StringUtils.join(groupList, ",")))
                                    .setRegStatus(RegStatus.newBuilder()
                                                    .setIsReged(obj.isReged)
                                                    .setIsNamed(obj.isNamed)
                                                    .setIsActive(obj.isActived)
                                                    .setIsSetup(obj.isSetup)
                                                    .setIsStopped(false)
                                                    .setIsSuppend(false)
                                                    .setIsFrozen(false)
                                    );
                            Buffer buffer = MomoMessage.buildBuffer(
                                    MsgType.PHONE_EXIST_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    replyBuilder.build().toByteArray()
                            );
                            mCommon.writeDataToSocket(sock, buffer);
                        }
                        // BEGIN 05/05/2016 Off DCNTT
                        else if(isStoreApp && obj == null)
                        {
                            logger.info("phone obj check exist not null");
                            // Get group of agent
                            Set<Integer> groupList = dbProcess.getAgentGroupId("0" + phoneNumber, logger);
                            replyBuilder.setResult(true)
                                    .addListKeyValue(TextValue.newBuilder().setText(Const.DGD.Retailer).setValue(dgd))
                                    .addListKeyValue(TextValue.newBuilder().setText(Const.DGD.GroupId).setValue(StringUtils.join(groupList, ",")))
                                    .setRegStatus(RegStatus.newBuilder()
                                                    .setIsReged(true)
                                    );
                            Buffer buffer = MomoMessage.buildBuffer(
                                    MsgType.PHONE_EXIST_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    replyBuilder.build().toByteArray()
                            );
                            mCommon.writeDataToSocket(sock, buffer);
                        }
                        // END 05/05/2016 Off DCNTT
                        else {
                            logger.info("phone obj check exist not null");
                            replyBuilder.setResult(false)
                                    .addListKeyValue(TextValue.newBuilder().setText(Const.DGD.Retailer).setValue(dgd))
                                    .setRegStatus(RegStatus.newBuilder()
                                                    .setIsReged(false)
                                    );
                            Buffer buffer = MomoMessage.buildBuffer(
                                    MsgType.PHONE_EXIST_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    replyBuilder.build().toByteArray()
                            );
                            mCommon.writeDataToSocket(sock, buffer);
                        }
                    }
                });
            }
        });
    }

    public void processGetAgentInfo(final NetSocket sock, final MomoMessage msg) {

        PhoneExist request;
        try {
            request = PhoneExist.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasNumber()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final int customPhone = DataUtil.strToInt(request.getNumber());
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("customPhone", "0" + customPhone);

        //1. lay trang thai
        //linhlamtiep
        agentsDb.getOneAgent("0" + msg.cmdPhone, "processGetAgentInfo", new Handler<AgentsDb.StoreInfo>() {
            @Override
            public void handle(AgentsDb.StoreInfo storeInfo) {

                if (storeInfo != null && storeInfo.agent_type == 3 && storeInfo.status != 2) {
                    log.add("check is store", storeInfo);
                    log.add("check is DCNTT", storeInfo.agent_type);
                    //String desc = storeInfo.isAgent ? StringConstUtil.IS_NAMED_DGD : StringConstUtil.IS_NAMED_USER;
                    final AgentInfo.Builder builder = AgentInfo.newBuilder();
                    builder.setExist(false);
                    final TextValue txtValue = TextValue.getDefaultInstance();
                    builder.setAddress(storeInfo.address)
                            .setName(storeInfo.name)
                            .setCardId("")
                            .setEmail("")
                            .setRegStatus(RegStatus.newBuilder()
                                    .setIsReged(true)
                                    .setIsNamed(true)
                                    .setIsActive(true)
                                    .setIsSetup(true)
                                    .setIsStopped(false)
                                    .setIsSuppend(false)
                                    .setIsFrozen(false))
                            .setResult(true)
                            .setDateOfBirth("")
                            .setBankCode("")
                            .setBankName("")
                            .addListKeyValue(txtValue.toBuilder().setText(StringConstUtil.IS_AGENT).setValue(storeInfo.storeName + "").
                                    setText(StringConstUtil.DESCRIPTION).setValue(StringConstUtil.IS_NOT_DCNTT_FUNCTION));

                    Buffer buffer = MomoMessage.buildBuffer(
                            MsgType.GET_AGENT_INFO_RELY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.build().toByteArray()
                    );
                    mCommon.writeDataToSocket(sock, buffer);

                    return;
                }

                Misc.getAgentStatus(vertx, customPhone, log, phonesDb, new Handler<SoapVerticle.ObjCoreStatus>() {
                    @Override
                    public void handle(SoapVerticle.ObjCoreStatus objCoreStatus) {

                        final AgentInfo.Builder builder = AgentInfo.newBuilder();
                        builder.setExist(objCoreStatus.isReged);
                        final TextValue txtValue = TextValue.getDefaultInstance();

                        if (objCoreStatus.isReged) {
                            phonesDb.getPhoneObjInfo(customPhone, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(PhonesDb.Obj obj) {
                                    if (obj == null) {
                                        builder.setExist(false);
                                    } else {

                                        String desc = obj.isAgent ? StringConstUtil.IS_NAMED_DGD : StringConstUtil.IS_NAMED_USER;
                                        builder.setAddress(obj.address)
                                                .setName(obj.name)
                                                .setCardId(obj.cardId)
                                                .setEmail(obj.email)
                                                .setRegStatus(RegStatus.newBuilder()
                                                        .setIsReged(obj.isReged)
                                                        .setIsNamed(obj.isNamed)
                                                        .setIsActive(obj.isActived)
                                                        .setIsSetup(obj.isSetup)
                                                        .setIsStopped(false)
                                                        .setIsSuppend(false)
                                                        .setIsFrozen(false))
                                                .setResult(true)
                                                .setDateOfBirth(obj.dateOfBirth)
                                                .setBankCode(obj.bank_code)
                                                .setBankName(obj.bank_name)
                                                .addListKeyValue(txtValue.toBuilder().setText(StringConstUtil.IS_AGENT).setValue(obj.isAgent + "").
                                                        setText(StringConstUtil.DESCRIPTION).setValue(desc));

                                    }

                                    Buffer buffer = MomoMessage.buildBuffer(
                                            MsgType.GET_AGENT_INFO_RELY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.build().toByteArray()
                                    );
                                    mCommon.writeDataToSocket(sock, buffer);
                                }
                            });
                        } else {
                            Buffer buffer = MomoMessage.buildBuffer(
                                    MsgType.GET_AGENT_INFO_RELY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    builder.build().toByteArray()
                            );
                            mCommon.writeDataToSocket(sock, buffer);
                        }
                    }
                });
                //End Misc
            }
        });
    }

    public void processAgentModify(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<Boolean> callback) {

        Register request;
        try {
            request = Register.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            if (sock != null)
                mCommon.writeErrorToSocket(sock);
            else
                callback.handle(false);
            return;
        }

        /*optional string name = 1;
        optional string card_id = 2;
        optional string address = 3;
        optional string email = 4;*/

        final String name = request.getName() == null ? "" : request.getName().trim();
        final String card_id = request.getCardId() == null ? "" : request.getCardId().trim();
        final String address = request.getAddress() == null ? "" : request.getAddress().trim();
        final String email = request.getEmail() == null ? "" : request.getEmail().trim();
        final String dob = request.getDob() == null ? "" : request.getDob().trim();

        final SoapProto.AgentInfoModify.Builder builder = SoapProto.AgentInfoModify.newBuilder()
                .setName(name)
                .setCardId(card_id)
                .setAddress(address)
                .setEmail(email)
                .setDob(dob);

        //lay kvp for soap from client request
        final ArrayList<SoapProto.keyValuePair.Builder> arrayKVs
                = Misc.buildKeyValuesForSoap(request.getKeyValueList());

//        arrayKVs.add(SoapProto.keyValuePair.newBuilder().setKey("bank_code").setValue("0"));
        /*//district        :maquanhuyen BELL ten quanhuyen
        //city         :matp BELL ten thanh pho
        //image        :path img1 BELL img2.....*/
        HashMap hashMap = Misc.buildKeyValueHashMap(request.getKeyValueList());
        final String imgs = hashMap.containsKey(Const.DGD.Img) ? (String) hashMap.get(Const.DGD.Img) : "";
        String disctInfo = hashMap.containsKey(Const.DGD.District) ? (String) hashMap.get(Const.DGD.District) : "";

        String[] disArr = disctInfo.split(MomoMessage.BELL);
        final int disId = DataUtil.strToInt(disArr[0]);
        final String disName = disArr.length > 1 ? disArr[1] : "";


        String cityInfo = hashMap.containsKey(Const.DGD.City) ? (String) hashMap.get(Const.DGD.City) : "";

        String[] ciArr = cityInfo.split(MomoMessage.BELL);
        final int ciId = DataUtil.strToInt(ciArr[0]);
        final String ciName = ciArr.length > 1 ? ciArr[1] : "";

        String retailer = hashMap.containsKey(Const.DGD.Retailer) ? (String) hashMap.get(Const.DGD.Retailer) : "";
        String tCusNumber = hashMap.containsKey(Const.DGD.CusNumber) ? (String) hashMap.get(Const.DGD.CusNumber) : "";
        String isNamed = hashMap.containsKey(Const.DGD.IsNamed) ? (String) hashMap.get(Const.DGD.IsNamed) : "";
        final String cusPersonalType = "Đã đăng ký";
        final String cusWard = "";
        final int cusNumber = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(tCusNumber) + "");
        final String otp = hashMap.containsKey("otp") ? (String) hashMap.get("otp") : "";

        // 0000000020 Check waitReg => check otp => dinh danh diem giao dich chap nhan thanh toan
        final String waitingReg = hashMap.containsKey("waitingReg") ? (String) hashMap.get("waitingReg") : "";


        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processAgentModify");
        log.add("dgd", retailer);
        log.add("isnamed", isNamed);
        log.add("cusnum", cusNumber);
        log.add("name", name);
        log.add("card_id", card_id);
        log.add("address", address);
        log.add("email", email);
        log.add("dob", dob);
        log.add("otp", otp);
        boolean checkCMND = checkCMND_user(card_id);

        if (!checkCMND) {
            Buffer buffer = MomoMessage.buildBuffer(
                    MsgType.AGENT_MODIFY_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    StandardReply.newBuilder()
                            .setResult(false)
                            .setRcode(SoapError.ACCESS_DENIED)
                            .setDesc("Ví đã có CMND không hợp lệ, Vui lòng kiểm tra lại")
                            .build().toByteArray()
            );
            mCommon.writeDataToSocket(sock, buffer);
            return;
        }
        //CMND only use 3 time
        String phoneCount = "0" + cusNumber;
        if (cusNumber == -1) {
            phoneCount = "0" + msg.cmdPhone;
        }
        if (!"".equalsIgnoreCase(card_id)) {
            int countCMND = countCMNDOfUser(card_id, phoneCount);
            logger.debug("CMND " + card_id + " phone " + phoneCount + " indentify " + countCMND + " time");
            if (countCMND >= NUMBER_COUNT_CMND) {
                logger.debug("CMND " + card_id + " indentify fail");
                Buffer buffer = MomoMessage.buildBuffer(
                        MsgType.AGENT_MODIFY_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        StandardReply.newBuilder()
                                .setResult(false)
                                .setRcode(SoapError.ACCESS_DENIED)
                                .setDesc("CMND đã được đăng ký 3 Ví")
                                .build().toByteArray()
                );
                mCommon.writeDataToSocket(sock, buffer);
                return;
            }
            logger.debug("CMND " + card_id + " indentify sucesss");
        }
        logger.debug("CMND " + card_id + " indentify sucesss");
        final boolean isRetailer = (Const.DGDValues.Retailer == DataUtil.strToInt(retailer)
                && Const.DGDValues.IsNamed == DataUtil.strToInt(isNamed)
                && cusNumber > 0);

        log.add("isRetailer", isRetailer);
        //       log.add("getOneAgent", "Kiem tra co phai diem chap nhan thanh toan di dinh danh ko");
        namingIsValid(sock, msg, arrayKVs, imgs, cusNumber, otp, log, isRetailer, new Handler<Boolean>() {
            @Override
            public void handle(Boolean valid) {

                //thong tin khach hang khong hop le
                if (!valid) {
                    return;
                }
                JsonObject jsonShare = new JsonObject();
                jsonShare.putString(Const.DGD.Img, imgs);
                final JsonArray jsonArrayShare = new JsonArray();
                jsonArrayShare.add(jsonShare);
                if (waitingReg.equalsIgnoreCase("1")) {
                    if (sock != null) {
//                        JsonObject joUpdate = new JsonObject();
//                        joUpdate.putBoolean(colName.PhoneDBCols.WAITING_REG, true);
//                        phonesDb.updatePhoneWithOutUpsert(cusNumber, joUpdate, new Handler<Boolean>() {
//                            @Override
//                            public void handle(Boolean aBoolean) {
//                                if (aBoolean) {
                        final Buffer buffer = MomoMessage.buildBuffer(
                                MsgType.AGENT_MODIFY_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                StandardReply.newBuilder()
                                        .setResult(false)
                                        .setRcode(0)
                                        .setDesc("OTP is OK")
                                        .build().toByteArray()
                        );
                        mCommon.writeDataToSocket(sock, buffer);
                        long curTime = System.currentTimeMillis();
                        //noti
                        Notification noti = new Notification();
                        noti.receiverNumber = msg.cmdPhone;
                        noti.caption = StringConstUtil.PAYMENT_STORE_TITLE;
                        noti.body = StringConstUtil.PAYMENT_STORE_BODY;
                        noti.bodyIOS = noti.body;
                        noti.priority = 1;
                        noti.tranId = curTime;
                        noti.time = curTime;
                        noti.cmdId = msg.cmdIndex;
                        noti.type = NotificationType.NOTI_TRANSACTION_VALUE;
                        noti.status = Notification.STATUS_DETAIL;
                        noti.sms = "";

                        String content = name + MomoMessage.BELL
                                + "0" + cusNumber + MomoMessage.BELL
                                + dob + MomoMessage.BELL
                                + card_id + MomoMessage.BELL
                                + address;
                        //tran
                        TranObj tran = new TranObj();
                        tran.tranId = curTime;
                        tran.clientTime = curTime;
                        tran.ackTime = curTime;
                        tran.finishTime = curTime;
                        tran.tranType = TranHisV1.TranType.NAMED_VALUE;
                        tran.io = 1;
                        tran.category = 0;
                        tran.partnerId = "0" + cusNumber;
                        tran.parterCode = "0" + cusNumber;
                        tran.partnerName = name;
                        tran.partnerRef = "";
                        tran.billId = "0";
                        tran.amount = retailer_named_value;
                        tran.comment = content;
                        tran.status = TranObj.STATUS_OK;
                        tran.error = 0;
                        tran.cmdId = curTime;
                        tran.share = jsonArrayShare;
                        mCommon.sendTransReplyByTran(msg, tran, transDb, sock);

                        Misc.sendNoti(vertx, noti);
//                                } else {
//                                    final Buffer buffer = MomoMessage.buildBuffer(
//                                            MsgType.AGENT_MODIFY_REPLY_VALUE,
//                                            msg.cmdIndex,
//                                            msg.cmdPhone,
//                                            StandardReply.newBuilder()
//                                                    .setResult(false)
//                                                    .setRcode(1000)
//                                                    .setDesc("Số điện thoại này không có, vui lòng kiểm tra lại")
//                                                    .build().toByteArray()
//                                    );
//                                    mCommon.writeDataToSocket(sock, buffer);
//                                }
//
//                            }
//                        });//End update

                    } else {
                        callback.handle(valid);
                    }
                    return;
                }

                //request agent modify via core
                for (int i = 0; i < arrayKVs.size(); i++) {
                    builder.addKvps(arrayKVs.get(i));
                }

                Buffer agentInfo = MomoMessage.buildBuffer(
                        SoapProto.MsgType.AGENT_INFO_MODIFY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build().toByteArray()
                );

                log.writeLog();

                //yeu cau core dinh danh cho vi nay
                vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, agentInfo, new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> result) {

                        //core tra ket qua dinh danh
                        final boolean isOk = result.body();

                        final Buffer buffer = MomoMessage.buildBuffer(
                                MsgType.AGENT_MODIFY_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                StandardReply.newBuilder()
                                        .setResult(isOk)
                                        .setRcode(SoapError.DB_NO_RECORD)
                                        .setDesc("Ví " + "0" + cusNumber + " đã định danh, Vui lòng kiểm tra lại")
                                        .build().toByteArray()
                        );


                        if (sock != null) {
                            mCommon.writeDataToSocket(sock, buffer);
                        } else {
                            callback.handle(isOk);
                        }

                        //sua thong tin thanh cong -->cap nhat mongo
                        final int fCusNumber = isRetailer ? cusNumber : msg.cmdPhone;

                        //neu dinh danh thanh cong
                        if (isOk) {

                            //dong bo du lieu dinh danh vi voi core
                            JsonObject jo = new JsonObject();

                            if (!"".equalsIgnoreCase(name)) {
                                jo.putString(colName.PhoneDBCols.NAME, name);
                            }

                            if (!"".equalsIgnoreCase(card_id)) {
                                jo.putString(colName.PhoneDBCols.CARD_ID, card_id);
                            }

                            if (!"".equalsIgnoreCase(address)) {
                                jo.putString(colName.PhoneDBCols.ADDRESS, address);
                            }

                            if (!"".equalsIgnoreCase(email)) {
                                jo.putString(colName.PhoneDBCols.EMAIL, email);
                            }

                            if (!"".equalsIgnoreCase(dob)) {
                                jo.putString(colName.PhoneDBCols.DATE_OF_BIRTH, dob);
                            }

                            //la giao dich dinh danh cua diem giao dich
                            if (isRetailer) {
                                jo.putBoolean(colName.PhoneDBCols.IS_NAMED, isRetailer);
                            }

                            phonesDb.updatePartial(fCusNumber, jo, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(PhonesDb.Obj obj) {
                                    //cap nhat lai cache cho end-user
                                    if (!isRetailer && data != null) {
                                        data.setPhoneObj(obj, logger, "");
                                    }
                                }
                            });

                            //diem giao dich dinh danh vi cho khach hang
                            if (cusNumber > 0 && isStoreApp) {

                                //cap nhat du lieu co he thong phan phoi HTPP
                                JsonObject json = new JsonObject();
                                json.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.DO_NAMED);
                                json.putString("cusPhone", "0" + cusNumber);
                                json.putString("cusName", name);
                                json.putString("cusDob", dob);
                                json.putString("cusId", card_id);
                                json.putString("cusIdType", cusPersonalType);
                                json.putString("cusEmail", email);
                                json.putString("cusAddress", address);
                                json.putString("cusWard", cusWard);
                                json.putNumber("cusDisId", disId);
                                json.putString("cusDisName", disName);
                                json.putNumber("cusCiId", ciId);
                                json.putString("cusCiName", ciName);
                                json.putNumber("cusStatus", 1);

                                vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS
                                        , json
                                        , new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> result) {
                                    }
                                });

                                long curTime = System.currentTimeMillis();
                                //noti
                                Notification noti = new Notification();
                                noti.receiverNumber = msg.cmdPhone;
                                noti.caption = (isOk ? "Định danh thành công" : "Định danh không thành công");
                                noti.body = (isOk ? "Chúc mừng, Bạn đã định danh thành công cho " + name + "(0" + cusNumber + ")" : "Bạn đã định danh không thành công cho " + name + "(0" + cusNumber + ")");
                                noti.bodyIOS = noti.body;
                                noti.priority = 1;
                                noti.tranId = curTime;
                                noti.time = curTime;
                                noti.cmdId = msg.cmdIndex;
                                noti.type = NotificationType.NOTI_TRANSACTION_VALUE;
                                noti.status = Notification.STATUS_DISPLAY;
                                noti.sms = "";

                                String content = name + MomoMessage.BELL
                                        + "0" + cusNumber + MomoMessage.BELL
                                        + dob + MomoMessage.BELL
                                        + card_id + MomoMessage.BELL
                                        + address;
                                //tran
                                TranObj tran = new TranObj();
                                tran.tranId = curTime;
                                tran.clientTime = curTime;
                                tran.ackTime = curTime;
                                tran.finishTime = curTime;
                                tran.tranType = TranHisV1.TranType.NAMED_VALUE;
                                tran.io = 1;
                                tran.category = 0;
                                tran.partnerId = "0" + cusNumber;
                                tran.parterCode = "0" + cusNumber;
                                tran.partnerName = name;
                                tran.partnerRef = "";
                                tran.billId = "0";
                                tran.amount = retailer_named_value;
                                tran.comment = content;
                                tran.status = (isOk ? TranObj.STATUS_OK : TranObj.STATUS_FAIL);
                                tran.error = (isOk ? 0 : SoapError.SYSTEM_ERROR);
                                tran.cmdId = curTime;

                                mCommon.sendTransReplyByTran(msg, tran, transDb, sock);

                                Misc.sendNoti(vertx, noti);

                            }
                        }
                    }
                });

            }
        });
    }


    public void processAgentModifyNew(final JsonObject info, final Handler<JsonObject> callback) {

        final long cmdIndex = System.currentTimeMillis();

        final JsonObject joReply = new JsonObject();
        final int phone = info.getInteger("phone", 0);
        final String name = info.getString("name", "").trim();
        final String card_id = info.getString("card_id", "").trim();
        final String address = info.getString("address", "").trim();
        final String email = info.getString("email", "").trim();
        final String dob = info.getString("dob", "").trim();

        final int disId = info.getInteger("disId", 0);
        final String disName = info.getString("disName", "").trim();

        final int ciId = info.getInteger("ciId", 0);
        final String ciName = info.getString("ciName", "").trim();

        final String cusPersonalType = "Đã đăng ký";
        final String cusWard = "";

        final SoapProto.AgentInfoModify.Builder builder = SoapProto.AgentInfoModify.newBuilder()
                .setName(name)
                .setCardId(card_id)
                .setAddress(address)
                .setEmail(email)
                .setDob(dob);

        String retailer = "1";
        String tCusNumber = "0" + phone;
        String isNamed = "1";
        final int cusNumber = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(tCusNumber) + "");
        final boolean isRetailer = (Const.DGDValues.Retailer == DataUtil.strToInt(retailer)
                && Const.DGDValues.IsNamed == DataUtil.strToInt(isNamed)
                && cusNumber > 0);

        SoapProto.keyValuePair retailerKVP = com.mservice.momo.msg.SoapProto.keyValuePair.newBuilder().setKey(Const.DGD.Retailer).setValue(retailer).build();
        SoapProto.keyValuePair cusNumberKVP = com.mservice.momo.msg.SoapProto.keyValuePair.newBuilder().setKey(Const.DGD.CusNumber).setValue(tCusNumber).build();
        SoapProto.keyValuePair isNamedKVP = com.mservice.momo.msg.SoapProto.keyValuePair.newBuilder().setKey(Const.DGD.IsNamed).setValue(isNamed).build();
        builder.addKvps(retailerKVP);
        builder.addKvps(cusNumberKVP);
        builder.addKvps(isNamedKVP);

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phone);
        log.add("function", "processAgentModifyNew");
        log.add("retailer", retailer);
        log.add("cusNumber", cusNumber);
        log.add("isNamed", isNamed);
        log.add("phone", phone);
        log.add("name", name);
        log.add("card_id", card_id);
        log.add("address", address);
        log.add("email", email);
        log.add("dob", dob);
        log.add("disId", disId);
        log.add("disName", disName);
        log.add("ciId", ciId);
        log.add("ciName", ciName);

        boolean checkCMND = checkCMND_user(card_id);
        if (!checkCMND) {
            joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, SoapError.ACCESS_DENIED);
            joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "Ví đã có CMND không hợp lệ, Vui lòng kiểm tra lại");
            callback.handle(joReply);
            return;
        }
        //CMND only use 3 time
        String phoneCount = "0" + phone;
        if (phone == -1) {
            phoneCount = "0" + phone;
        }
        if (!"".equalsIgnoreCase(card_id)) {
            int countCMND = countCMNDOfUser(card_id, phoneCount);
            logger.debug("CMND " + card_id + " phone " + phoneCount + " indentify " + countCMND + " time");
            if (countCMND >= NUMBER_COUNT_CMND) {
                logger.debug("CMND " + card_id + " indentify fail");
                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, SoapError.ACCESS_DENIED);
                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, "CMND đã được đăng ký 3 Ví");
                callback.handle(joReply);
                return;
            }
            logger.debug("CMND " + card_id + " indentify sucesss");
        }

        Buffer agentInfo = MomoMessage.buildBuffer(
                SoapProto.MsgType.AGENT_INFO_MODIFY_VALUE,
                cmdIndex,
                phone,
                builder.build().toByteArray()
        );
        log.writeLog();

        //yeu cau core dinh danh cho vi nay
        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, agentInfo, new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> result) {

                //core tra ket qua dinh danh
                final boolean isOk = result.body();

                int err = isOk ? SoapError.SUCCESS : SoapError.SYSTEM_ERROR;
                joReply.putNumber(StringConstUtil.MerchantKeyManage.STATUS, err);
                joReply.putString(StringConstUtil.MerchantKeyManage.MESSAGE, SoapError.getDesc(err));
                callback.handle(joReply);

                //sua thong tin thanh cong -->cap nhat mongo
                final int fCusNumber = phone;

                //neu dinh danh thanh cong
                if (isOk) {

                    //dong bo du lieu dinh danh vi voi core
                    JsonObject jo = new JsonObject();

                    if (!"".equalsIgnoreCase(name)) {
                        jo.putString(colName.PhoneDBCols.NAME, name);
                    }

                    if (!"".equalsIgnoreCase(card_id)) {
                        jo.putString(colName.PhoneDBCols.CARD_ID, card_id);
                    }

                    if (!"".equalsIgnoreCase(address)) {
                        jo.putString(colName.PhoneDBCols.ADDRESS, address);
                    }

                    if (!"".equalsIgnoreCase(email)) {
                        jo.putString(colName.PhoneDBCols.EMAIL, email);
                    }

                    if (!"".equalsIgnoreCase(dob)) {
                        jo.putString(colName.PhoneDBCols.DATE_OF_BIRTH, dob);
                    }

                    //la giao dich dinh danh cua diem giao dich
                    if (isRetailer) {
                        jo.putBoolean(colName.PhoneDBCols.IS_NAMED, isRetailer);
                    }

                    phonesDb.updatePartial(fCusNumber, jo, new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(PhonesDb.Obj obj) {
                        }
                    });

                    //diem giao dich dinh danh vi cho khach hang
                    if (cusNumber > 0 && isStoreApp) {

                        //cap nhat du lieu co he thong phan phoi HTPP
                        JsonObject json = new JsonObject();
                        json.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.DO_NAMED);
                        json.putString("cusPhone", "0" + cusNumber);
                        json.putString("cusName", name);
                        json.putString("cusDob", dob);
                        json.putString("cusId", card_id);
                        json.putString("cusIdType", cusPersonalType);
                        json.putString("cusEmail", email);
                        json.putString("cusAddress", address);
                        json.putString("cusWard", cusWard);
                        json.putNumber("cusDisId", disId);
                        json.putString("cusDisName", disName);
                        json.putNumber("cusCiId", ciId);
                        json.putString("cusCiName", ciName);
                        json.putNumber("cusStatus", 1);

                        vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS
                                , json
                                , new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> result) {
                                    }
                                });

                        long curTime = System.currentTimeMillis();
                        //noti
                        Notification noti = new Notification();
                        noti.receiverNumber = phone;
                        noti.caption = (isOk ? "Định danh thành công" : "Định danh không thành công");
                        noti.body = (isOk ? "Chúc mừng, Bạn đã định danh thành công cho " + name + "(0" + cusNumber + ")" : "Bạn đã định danh không thành công cho " + name + "(0" + cusNumber + ")");
                        noti.bodyIOS = noti.body;
                        noti.priority = 1;
                        noti.tranId = curTime;
                        noti.time = curTime;
                        noti.cmdId = cmdIndex;
                        noti.type = NotificationType.NOTI_TRANSACTION_VALUE;
                        noti.status = Notification.STATUS_DISPLAY;
                        noti.sms = "";

                        String content = name + MomoMessage.BELL
                                + "0" + cusNumber + MomoMessage.BELL
                                + dob + MomoMessage.BELL
                                + card_id + MomoMessage.BELL
                                + address;
                        //tran
                        TranObj tran = new TranObj();
                        tran.tranId = curTime;
                        tran.clientTime = curTime;
                        tran.ackTime = curTime;
                        tran.finishTime = curTime;
                        tran.tranType = TranHisV1.TranType.NAMED_VALUE;
                        tran.io = 1;
                        tran.category = 0;
                        tran.partnerId = "0" + cusNumber;
                        tran.parterCode = "0" + cusNumber;
                        tran.partnerName = name;
                        tran.partnerRef = "";
                        tran.billId = "0";
                        tran.amount = retailer_named_value;
                        tran.comment = content;
                        tran.status = (isOk ? TranObj.STATUS_OK : TranObj.STATUS_FAIL);
                        tran.error = (isOk ? 0 : SoapError.SYSTEM_ERROR);
                        tran.cmdId = curTime;

                        transDb.upsertTran(phone, tran.getJSON(), new Handler<TranObj>() {
                            @Override
                            public void handle(TranObj tranObj) {
                            }
                        });

                        Misc.sendNoti(vertx, noti);

                    }
                }
            }
        });
    }

    /**
     * @param sock       current socket
     * @param msg        current request message
     * @param arrayKVs   list of key-value will be sent to soap
     * @param imgs       list of images as string array
     * @param cusNumber  the wallet will be named
     * @param otp        the opt to verify information
     * @param log        the log
     * @param isRetailer true : request named agent from retailer, other end-user request
     * @param callback   handle the result after validate information true: valid --> continue process, false : stop at this time
     */
    private void namingIsValid(final NetSocket sock
            , final MomoMessage msg
            , final ArrayList<SoapProto.keyValuePair.Builder> arrayKVs
            , String imgs
            , int cusNumber
            , final String otp
            , final Common.BuildLog log
            , boolean isRetailer
            , final Handler<Boolean> callback) {

        boolean valid = true;

        StandardReply.Builder builder = StandardReply.newBuilder();

        if (isRetailer) {
            //lay danh sach hinh
            if (!"".equalsIgnoreCase(imgs)) {
                String[] arImgs = imgs.split(MomoMessage.BELL);
                log.add("imgs", arImgs.length);

                //khong du so hinh quy dinh
                if (arImgs == null || arImgs.length < ftp_min_img) {
                    valid = false;
                } else {
                    //dua danh sach hinh vao keyvalue pair de store vao trong core
                    if (arImgs != null) {
                        String imgPreFix = "img_";
                        for (int i = 0; i < arImgs.length; i++) {
                            Misc.addKeyValueForSoap(arrayKVs, imgPreFix + (i + 1), arImgs[i]);
                            log.add(imgPreFix + (i + 1), arImgs[i]);
                        }
                    }
                }
            } else {
                valid = false;
                log.add("loi", "chua co thong tin up hinh");
            }

            //so hinh upload len server khong hop le
            if (!valid) {

                final Buffer buffer = MomoMessage.buildBuffer(
                        MsgType.AGENT_MODIFY_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.setResult(false)
                                .setRcode(SoapError.SYSTEM_ERROR)
                                .setDesc("Số lượng hình upload tối thiểu " + ftp_min_img)
                                .build().toByteArray()
                );

                mCommon.writeDataToSocket(sock, buffer);
                callback.handle(false);
                log.writeLog();
                return;
            }

            //check otp for naming wallet
            if ("".equalsIgnoreCase(otp)) {
                final Buffer buffer = MomoMessage.buildBuffer(
                        MsgType.AGENT_MODIFY_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        StandardReply.newBuilder()
                                .setResult(false)
                                .setRcode(SoapError.SYSTEM_ERROR)
                                .setDesc("Vui lòng nhập OTP để định danh")
                                .build().toByteArray()
                );

                mCommon.writeDataToSocket(sock, buffer);
                log.writeLog();
                //not valid
                callback.handle(false);
                return;
            }

            otpClientDb.findOne(msg.cmdPhone, cusNumber, new Handler<OtpClientDb.Obj>() {
                @Override
                public void handle(OtpClientDb.Obj obj) {
                    String savedOtp = obj == null ? "" : obj.opt;

                    // khong khop otp
                    if (!savedOtp.equalsIgnoreCase(otp)) {

                        final Buffer buffer = MomoMessage.buildBuffer(
                                MsgType.AGENT_MODIFY_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                StandardReply.newBuilder()
                                        .setResult(false)
                                        .setRcode(0)
                                        .setDesc("OTP bạn nhập không đúng, vui lòng nhập lại")
                                        .build().toByteArray()
                        );

                        mCommon.writeDataToSocket(sock, buffer);
                        log.writeLog();
                        callback.handle(false);
                        return;
                    }

                    //remove the record from db
                    otpClientDb.remove(obj.id, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                        }
                    });

                    callback.handle(true);
                }
            });
            return;
        }

        //khong phai la diem giao dich --> xem nhu hop le
        callback.handle(valid);

    }

    public void processSyncStoreLocation(final NetSocket sock, final MomoMessage msg) {

        StandardSync req;
        try {
            req = StandardSync.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            req = null;
        }

        if (req == null || !req.hasLastUpdateTime()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("client last sync time location", req.getLastUpdateTime());
        log.add("time vn", Misc.dateVNFormatWithTime(req.getLastUpdateTime()));
        log.add("function", "agentsDb.getStores");
        int syncStoreForApp = 10;
        final StandardSync req_tmp = req;

        long iosTimeLastUpdate = sync_store_app_json_obj.getLong(StringConstUtil.SyncStoreApp.TIME_IOS_DEFAULT, Long.parseLong("1428896510613"));
        long androidTimeLastUpdate = sync_store_app_json_obj.getLong(StringConstUtil.SyncStoreApp.TIME_ANDROID_DEFAULT, Long.parseLong("1414808348774"));
        if(isStoreApp)
        {
            SyncStoreLocationReply.Builder builder = SyncStoreLocationReply.newBuilder();
            Buffer buff = MomoMessage.buildBuffer(MsgType.STORE_LOCATION_SYNC_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , builder.setResult(false).build()
                    .toByteArray());
            mCommon.writeDataToSocket(sock, buff);
        }
        else if(req.getLastUpdateTime() == iosTimeLastUpdate || req.getLastUpdateTime() == androidTimeLastUpdate)
        {
            vertx.setTimer(10 * 60 * 1000L, new Handler<Long>() {
                @Override
                public void handle(final Long event) {
                    agentsDb.getActivesStores(req_tmp.getLastUpdateTime(), new Handler<ArrayList<AgentsDb.StoreInfo>>() {
                        @Override
                        public void handle(ArrayList<AgentsDb.StoreInfo> objs) {
//                            vertx.cancelTimer(event);
                            log.add("location size", objs == null ? 0 : objs.size());

                            SyncStoreLocationReply.Builder builder = SyncStoreLocationReply.newBuilder();

                            //chay for tra ve, limited 1024
                            if (objs != null && objs.size() > 0) {
                                TimerLocSync tLocSync = new TimerLocSync(vertx, logger, sock, msg, objs, glbCfg);
                                tLocSync.start();
                                log.add("sync timer started " + msg.cmdPhone, "");
                            } else {
                                Buffer buff = MomoMessage.buildBuffer(MsgType.STORE_LOCATION_SYNC_REPLY_VALUE
                                        , msg.cmdIndex
                                        , msg.cmdPhone
                                        , builder.setResult(false).build()
                                        .toByteArray());
                                mCommon.writeDataToSocket(sock, buff);
                            }
                            vertx.cancelTimer(event);
                            log.writeLog();
                        }
                    });
                }
            });
            return;
        }
        else{
            agentsDb.getStores(req.getLastUpdateTime(), new Handler<ArrayList<AgentsDb.StoreInfo>>() {
                @Override
                public void handle(ArrayList<AgentsDb.StoreInfo> objs) {

                    log.add("location size", objs == null ? 0 : objs.size());

                    SyncStoreLocationReply.Builder builder = SyncStoreLocationReply.newBuilder();

                    //chay for tra ve, limited 1024
                    if (objs != null && objs.size() > 0) {
                        TimerLocSync tLocSync = new TimerLocSync(vertx, logger, sock, msg, objs, glbCfg);
                        tLocSync.start();
                        log.add("sync timer started", "");
                    } else {
                        Buffer buff = MomoMessage.buildBuffer(MsgType.STORE_LOCATION_SYNC_REPLY_VALUE
                                , msg.cmdIndex
                                , msg.cmdPhone
                                , builder.setResult(false).build()
                                .toByteArray());
                        mCommon.writeDataToSocket(sock, buff);
                    }
                    log.writeLog();
                }
            });
        }
    }

    public void processAvatarUpload(final NetSocket sock, final MomoMessage msg) {

        JsonObject queryCmd = new JsonObject()
                .putString("phoneNumber", "0" + msg.cmdPhone);
        vertx.eventBus().send(UserResourceVerticle.CMD_GET_TOKEN, queryCmd, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                String token = event.body().getString("token", "");

                Buffer buffer = MomoMessage.buildBuffer(
                        MsgType.AVATAR_UPLOAD_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , AvatarUploadReply.newBuilder()
                                .setToken(token)
                                .build()
                                .toByteArray()
                );
                mCommon.writeDataToSocket(sock, buffer);
            }
        });
    }

    public void processSaveDeviceInfo(final NetSocket sock
            , final MomoMessage msg
            , final SockData _data) {

        DeviceInfo dvcInfo = null;
        try {
            dvcInfo = DeviceInfo.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            dvcInfo = null;
        }

        if (dvcInfo == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        String token = (dvcInfo.getToken() == null ? "" : dvcInfo.getToken());
        String os = (dvcInfo.getOs() == null ? "" : dvcInfo.getOs());

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("token", token);
        log.add("os", os);

        if ((!"".equalsIgnoreCase(token)) && (!"".equalsIgnoreCase(os))) {

            log.add("call setToken", "");

            phonesDb.setToken(msg.cmdPhone, dvcInfo.getToken(), dvcInfo.getOs(), new Handler<Boolean>() {
                @Override
                public void handle(Boolean result) {
                    log.add("set token result", result);
                    log.writeLog();
                }
            });

        } else {
            log.writeLog();
        }

        deviceInfoDb.saveDeviceInfo(msg.cmdPhone
                , dvcInfo.getDeviceName() == null ? "" : dvcInfo.getDeviceName()
                , dvcInfo.getDeviceVersion() == null ? "" : dvcInfo.getDeviceVersion()
                , dvcInfo.getDeviceModel() == null ? "" : dvcInfo.getDeviceModel()
                , dvcInfo.getDeviceManufacturer() == null ? "" : dvcInfo.getDeviceManufacturer()
                , dvcInfo.getAppVersion() == null ? "" : dvcInfo.getAppVersion()
                , dvcInfo.getDeviceSWidth() == null ? "" : dvcInfo.getDeviceSWidth()
                , dvcInfo.getDeviceSHeight() == null ? "" : dvcInfo.getDeviceSHeight()
                , dvcInfo.getDevicePrimaryEmail() == null ? "" : dvcInfo.getDevicePrimaryEmail()
                , dvcInfo.getOs() == null ? "" : dvcInfo.getOs()
                , dvcInfo.getToken() == null ? "" : dvcInfo.getToken()
                , new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                logger.info("SAVE DEVICE INFO " + aBoolean);
            }
        });


        Buffer buffer = MomoMessage.buildBuffer(
                MsgType.DEVICE_INFO_REPLY_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , StandardReply.newBuilder()
                        .setResult(true)
                        .build()
                        .toByteArray()
        );

        mCommon.writeDataToSocket(sock, buffer);
    }

    public void processGetAccessHistory(final NetSocket sock, final MomoMessage msg) {
        GetAccessHistory request;
        try {
            request = GetAccessHistory.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasPageNum() || !request.hasPageNum()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        accessHistoryDb.getLatest(msg.cmdPhone, request.getPageNum(), request.getPageSize(), new Handler<ArrayList<AccessHistoryDb.AccessHistoryObj>>() {
            @Override
            public void handle(ArrayList<AccessHistoryDb.AccessHistoryObj> results) {

                GetAccessHistoryReply.Builder builder = GetAccessHistoryReply.newBuilder();

                if (results != null && results.size() > 0) {

                    for (int i = 0; i < results.size(); i++) {

                        builder.addAccessDetail(AccessHistoryDetail.newBuilder()
                                        .setIp(results.get(i).ip)
                                        .setTimeIn(results.get(i).start_time_access)
                                        .setTimeOut(results.get(i).end_time_access)
                                        .setDeviceModel(results.get(i).device_model)
                        );
                    }
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.GET_ACCESS_HISTORY_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.setResult(true)
                                .build().toByteArray()
                );
                mCommon.writeDataToSocket(sock, buf);
            }
        });

    }

    public void processMoneyRequest(final NetSocket sock, final MomoMessage msg, final SockData data) {
        MoneyRequest request = null;
        try {
            request = MoneyRequest.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }

        if (request == null || !request.hasAmount()
                || !request.hasToNumber()
                || !request.hasContent()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        if (data == null || data.getPhoneObj() == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }
        Common.BuildLog log = new Common.BuildLog(logger);
        final MoneyRequest frequest = request;
        log.add("existsMMPhone", "processMoneyRequest");
        log.writeLog();
        agentsDb.getOneAgent("0" + msg.cmdPhone, "processMoneyRequest", new Handler<AgentsDb.StoreInfo>() {
            @Override
            public void handle(AgentsDb.StoreInfo storeInfo) {
                //retailer
                if (storeInfo != null) {
                    int fromPhone = msg.cmdPhone;
                    String fromUserName = (data == null || data.getPhoneObj() == null) ? "" : data.getPhoneObj().name;
                    String storeName = (storeInfo.storeName == null) ? "" : storeInfo.storeName;
                    String comment = frequest.getContent() == null ? "" : frequest.getContent();

                    JsonObject extra = new JsonObject();
                    extra.putNumber("type", 2);
                    extra.putNumber("phoneNumber", frequest.getToNumber());
                    extra.putString("fromUserName", fromUserName);
                    extra.putNumber("fromPhone", fromPhone);
                    extra.putNumber("amount", frequest.getAmount());
                    extra.putString("comment", comment);
                    extra.putNumber("cmdId", msg.cmdIndex);
                    extra.putString(Const.DGD.Retailer, "1");

                    Notification noti = new Notification();
                    noti.type = MomoProto.NotificationType.NOTI_MONEY_REQUEST_VALUE;
                    noti.receiverNumber = frequest.getToNumber();
                    noti.sender = fromPhone;
                    noti.priority = 2;
                    noti.cmdId = msg.cmdIndex;
                    noti.caption = "Yêu cầu rút tiền"
                            + MomoMessage.BELL + fromUserName
                            + MomoMessage.BELL + frequest.getAmount()
                            + MomoMessage.BELL + comment;
                    noti.body = "Bạn nhận được 1 yêu cầu rút tiền từ số 0" + fromPhone + "(" + fromUserName + ")";
                    noti.bodyIOS = "Bạn nhận được 1 yêu cầu rút tiền từ số 0" + fromPhone + "(" + fromUserName + ")";
                    if (isStoreApp) {
                        noti.caption = "Yêu cầu rút tiền"
                                + MomoMessage.BELL + storeName
                                + MomoMessage.BELL + frequest.getAmount()
                                + MomoMessage.BELL + comment;
                        noti.body = "Bạn nhận được yêu cầu rút tiền từ điểm giao dịch " + storeName + "(" + fromPhone + ")";
                        noti.bodyIOS = "Bạn nhận được yêu cầu rút tiền từ điểm giao dịch " + storeName + "(" + fromPhone + ")";
                    }
                    noti.time = System.currentTimeMillis();
                    noti.category = 0;
                    noti.extra = extra.toString();

                    Misc.sendNoti(vertx, noti);

                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.MONEY_REQUEST_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , StandardReply.newBuilder()
                                    .setResult(true)
                                    .setRcode(0).build()
                                    .toByteArray()
                    );
                    mCommon.writeDataToSocket(sock, buf);
                    return;
                }

                //end user
                reqMoney.execRequestMoney(msg.cmdPhone, new Handler<Integer>() {
                    @Override
                    public void handle(final Integer requestNumber) {
                        Buffer buffer;

                        if (requestNumber <= REQUEST_MONEY_MAX_PER_DAY) {

                            int fromPhone = msg.cmdPhone;
                            String fromUserName = (data == null || data.getPhoneObj() == null) ? "" : data.getPhoneObj().name;
                            String comment = frequest.getContent() == null ? "" : frequest.getContent();

                            JsonObject extra = new JsonObject();
                            extra.putNumber("type", 2);
                            extra.putNumber("phoneNumber", frequest.getToNumber());
                            extra.putString("fromUserName", fromUserName);
                            extra.putNumber("fromPhone", fromPhone);
                            extra.putNumber("amount", frequest.getAmount());
                            extra.putString("comment", comment);
                            extra.putNumber("cmdId", msg.cmdIndex);

                            Notification noti = new Notification();
                            noti.type = MomoProto.NotificationType.NOTI_MONEY_REQUEST_VALUE;
                            noti.receiverNumber = frequest.getToNumber();
                            noti.sender = fromPhone;
                            noti.priority = 2;
                            noti.cmdId = msg.cmdIndex;
                            noti.caption = "Yêu cầu mượn tiền" + MomoMessage.BELL + fromUserName + MomoMessage.BELL + frequest.getAmount() + MomoMessage.BELL + comment;
                            noti.body = "Bạn nhận được 1 yêu cầu mượn tiền từ số 0" + fromPhone + "(" + fromUserName + ")";
                            noti.bodyIOS = "Bạn nhận được 1 yêu cầu mượn tiền từ số 0" + fromPhone + "(" + fromUserName + ")";
                            if (isStoreApp) {
                                noti.caption = "Yêu cầu rút tiền" + MomoMessage.BELL + fromUserName + MomoMessage.BELL + frequest.getAmount() + MomoMessage.BELL + comment;
                                noti.body = "Bạn nhận được yêu cầu rút tiền từ điểm giao dịch " + fromUserName + "(0" + fromPhone + ")";
                                noti.bodyIOS = "Bạn nhận được yêu cầu rút tiền từ điểm giao dịch " + fromUserName + "(0" + fromPhone + ")";
                            }
                            noti.time = System.currentTimeMillis();
                            noti.category = 0;
                            noti.extra = extra.toString();

                            Misc.sendNoti(vertx, noti);

                            Buffer buf = MomoMessage.buildBuffer(
                                    MsgType.MONEY_REQUEST_REPLY_VALUE
                                    , msg.cmdIndex
                                    , msg.cmdPhone
                                    , StandardReply.newBuilder()
                                            .setResult(true)
                                            .setRcode(requestNumber).build()
                                            .toByteArray()
                            );
                            mCommon.writeDataToSocket(sock, buf);

                        } else {
                            buffer = MomoMessage.buildBuffer(
                                    MsgType.MONEY_REQUEST_REPLY_VALUE
                                    , msg.cmdIndex
                                    , msg.cmdPhone
                                    , StandardReply.newBuilder()
                                            .setResult(false)
                                            .build()
                                            .toByteArray()
                            );

                            mCommon.writeDataToSocket(sock, buffer);
                        }
                    }
                });
            }
        });
    }

    public void processWhoIsMomoer(final NetSocket sock, final MomoMessage msg) {
        WhoIsMomoer request;
        try {
            request = WhoIsMomoer.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        if (!isStoreApp) {
            phonesDb.checkWhoIsMomoer(request.getNumbersList(), new Handler<List<Integer>>() {
                @Override
                public void handle(List<Integer> s) {
                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.WHO_IS_MOMOER_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            WhoIsMomoer.newBuilder()
                                    .addAllNumbers(s)
                                    .build().toByteArray()
                    );
                    mCommon.writeDataToSocket(sock, buf);
                }
            });

            JsonArray jarrListPhones = new JsonArray();
            for(Integer number : request.getNumbersList())
            {
                jarrListPhones.add(number);
            }
            JsonObject joCheckCheat = new JsonObject()
                    .putString(StringConstUtil.MOMO_TOOL.PHONE_NUMBER, "0" + msg.cmdPhone)
                    .putString(StringConstUtil.PATH, "/sendContacts")
                    .putArray(StringConstUtil.MOMO_TOOL.PHONE_LIST, jarrListPhones);
            vertx.eventBus().send(AppConstant.CHECK_CHEATING_PROMOTION_BUS_ADDRESS, joCheckCheat, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {

                }
            });
            return;
        }
        Buffer buf = MomoMessage.buildBuffer(
                MsgType.WHO_IS_MOMOER_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                WhoIsMomoer.newBuilder()
                        .addAllNumbers(new ArrayList<Integer>())
                        .build().toByteArray()
        );
        mCommon.writeDataToSocket(sock, buf);
        return;
    }

    public void processBanknetCheckInfo(final NetSocket sock
            , final MomoMessage msg
            , final SockData _data
            , final Handler<Buffer> webCallback) {
        MomoProto.CardItem request;
        try {
            request = MomoProto.CardItem.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }

        if (request == null
                || !request.hasCardHolderName()
                || !request.hasCardHolderNumber()
                || !request.hasCardHolderMonth()
                || !request.hasCardHolderYear()
                || !request.hasBankId()
                || !request.hasAmount()) {

            mCommon.writeErrorToSocket(sock);
            return;
        }

        final MomoProto.CardItem frequest = request;

        /*
            card_holder_year	-->	bill_Id
            card_holder_month	-->	partner_ref
            card_holder_number	-->	partner_code
            card_holder_name	-->	partner_name
            bankId	-->	partner_id
        */

        //store card info
        if (_data != null) {
            _data.card_holder_name = request.getCardHolderName() == null ? "" : request.getCardHolderName();
            _data.card_holder_number = request.getCardHolderNumber() == null ? "" : request.getCardHolderNumber();
            _data.card_holder_month = request.getCardHolderMonth() == null ? "" : request.getCardHolderMonth();
            _data.card_holder_year = request.getCardHolderYear() == null ? "" : request.getCardHolderYear();
            _data.bank_net_bank_code = request.getBankId() == null ? "" : request.getBankId();
            //set bank-net info
        }

        //refine.start
        String card_holder_month = request.getCardHolderMonth();
        String card_holder_year = request.getCardHolderYear();

        if (card_holder_month.length() == 1) {
            card_holder_month = "0" + card_holder_month;
        }

        if (card_holder_year.length() == 4) {
            card_holder_year = card_holder_year.substring(2, 4);
        } else if (card_holder_year.length() == 3) {
            card_holder_year = card_holder_year.substring(1, 3);
        } else if (card_holder_year.length() == 1) {
            card_holder_year = "0" + card_holder_year;
        }

        final String card_month = card_holder_month;
        final String card_year = card_holder_year;

        final String card_holder_name = Misc.removeAccent(frequest.getCardHolderName());
        final String card_holder_number = Misc.removeAccent(frequest.getCardHolderNumber());

        //refine.end

        final String bankId = frequest.getBankId() == null ? "" : frequest.getBankId();
        final int inout_city = 1;
        final int trantype = TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE;
        final int channel = MomoProto.CardItem.Channel.BANKNET_VALUE;

        final int feetype = FeeType.MOMO_VALUE;
        final long amount = 0;

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processBanknetCheckInfo");
        log.add("bankid", bankId);
        log.add("channel", channel);
        log.add("tran type", TranHisV1.TranType.valueOf(trantype).name());
        log.add("IO city", inout_city);
        log.add("fee type", FeeType.valueOf(feetype).name());
        log.add("client amount", request.getAmount());

        log.add("bank type", request.getLockedType());
        String bankVerticle = bankCfg.getString(StringConstUtil.BANK, "bankVerticle");
        log.add("bank verticle ", bankVerticle);
        BankRequest bankRequest = BankRequestFactory.otpRequest(request.getBankId(), "0" + msg.cmdPhone, System.currentTimeMillis(), request.getAmount());
        final MomoProto.CardItem finalRequest = request;
        isBankLinkCashIn(bankId, log, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                int requestTranType = finalRequest.getTranType();
                String tranId = "noOTP";
                if (TranHisV1.TranType.BANK_IN_VALUE == requestTranType && aBoolean) {
                    tranId = "hasOtp";
                }
                log.add(tranId + " => requestTranType", requestTranType);
                log.add(tranId + " => is bankLink source", aBoolean);

                if (finalRequest != null && finalRequest.getLockedType() == BankType.BANK_LINKED_TO_MOMO) {

                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.BANK_NET_TO_MOMO_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , BankNetToMomoRely.newBuilder()
                                    .setResult(true)
                                    .setRcode(0) // request timeout
                                    .setMerchantTransId("Success")
                                    .setTransId(tranId)
                                    .build()
                                    .toByteArray()
                    );
                    log.writeLog();
                    if (sock != null) {
                        mCommon.writeDataToSocket(sock, buf);
                    }

                } else {
                    Misc.getM2MFee(vertx, TranHisV1.TranType.M2M_VALUE, amount, "", "", new Handler<FeeDb.Obj>() {
                        @Override
                        public void handle(FeeDb.Obj obj) {
                            final int m2mFee = obj == null ? 1000 : obj.STATIC_FEE;
                            log.add("m2mfee", m2mFee);

                            feeDb.getFee(bankId, channel, trantype, inout_city, feetype, amount, new Handler<FeeDb.Obj>() {
                                @Override
                                public void handle(FeeDb.Obj obj) {

                                    long staticfee;
                                    double dynamicfee;

                                    if (obj == null) {
                                        staticfee = 1100;
                                        dynamicfee = 1.2;
                                        _data.bank_net_bank_name = "";
                                    } else {
                                        staticfee = obj.STATIC_FEE;
                                        dynamicfee = obj.DYNAMIC_FEE;
                                        _data.bank_net_bank_name = obj.BANK_NAME;
                                    }

                                    final Misc.Cash cash = Misc.calculateAmount(frequest.getAmount() + m2mFee
                                            , staticfee
                                            , dynamicfee
                                            , frequest.getLockedType()
                                            , trantype
                                            , log);

                                    log.add("before send to banknet", "");
                                    log.add("amount will sent to banknet", cash.bankNetAmountLocked);
                                    log.add("amount will adjust at MoMo", cash.coreAmountAdjust);
                                    log.add("send to banknet verticle", "");
                                    //build buffer send to soap
                                    Buffer buffer = MomoMessage.buildBuffer(
                                            SoapProto.MsgType.BANK_NET_TO_MOMO_VALUE
                                            , msg.cmdIndex
                                            , msg.cmdPhone
                                            , SoapProto.BankNetToMomo.newBuilder()
                                                    .setAmount(cash.bankNetAmountLocked)
                                                    .setBankId(frequest.getBankId())
                                                    .setCardHolderName(card_holder_name)
                                                    .setCardHolderNumber(card_holder_number)
                                                    .setCardHolderMonth(card_month)
                                                    .setCardHolderYear(card_year)
                                                    .build()
                                                    .toByteArray()
                                    );

                                    vertx.eventBus().sendWithTimeout(AppConstant.BanknetVerticle_ADDRESS
                                            , buffer
                                            , 40000, new Handler<AsyncResult<Message<JsonObject>>>() {
                                                @Override
                                                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {

                                                    Buffer buf;

                                                    //timeout
                                                    if (messageAsyncResult.failed()) {
                                                        int rCode = 8; // for mobile app.
                                                        if (webCallback != null && sock == null) {
                                                            rCode += 5000;
                                                        }

                                                        buf = MomoMessage.buildBuffer(
                                                                MsgType.BANK_NET_TO_MOMO_REPLY_VALUE
                                                                , msg.cmdIndex
                                                                , msg.cmdPhone
                                                                , BankNetToMomoRely.newBuilder()
                                                                        .setResult(false)
                                                                        .setRcode(rCode) // request timeout
                                                                        .build()
                                                                        .toByteArray()
                                                        );

                                                    } else if (messageAsyncResult.succeeded()) {
                                                        JsonObject json = messageAsyncResult.result().body();

                                                        if (json != null) {

                                                            String result = (json == null ? "json NULL" : json.getBoolean("result").toString());
                                                            int rcode = (json == null ? -1 : json.getInteger("rcode"));
                                                            String merchant_trans_id = (json == null ? "" : json.getString("merchant_trans_id"));
                                                            String trans_id = (json == null ? "" : json.getString("trans_id"));

                                                            log.add("error", rcode);
                                                            log.add("result", result);
                                                            log.add("errordesc", BanknetErrors.getDesc((rcode == 0 ? 0 : rcode - 5000)));
                                                            log.add("merchant_trans_id", merchant_trans_id);
                                                            log.add("trans_id", trans_id);
                                                            log.add("json reply", json);

                                                            buf = MomoMessage.buildBuffer(
                                                                    MsgType.BANK_NET_TO_MOMO_REPLY_VALUE
                                                                    , msg.cmdIndex
                                                                    , msg.cmdPhone
                                                                    , BankNetToMomoRely.newBuilder()
                                                                            .setResult(json.getBoolean("result"))
                                                                            .setRcode(json.getInteger("rcode"))
                                                                            .setMerchantTransId(json.getString("merchant_trans_id"))
                                                                            .setTransId(json.getString("trans_id"))
                                                                            .build()
                                                                            .toByteArray()
                                                            );
                                                        } else {
                                                            buf = MomoMessage.buildBuffer(
                                                                    MsgType.BANK_NET_TO_MOMO_REPLY_VALUE
                                                                    , msg.cmdIndex
                                                                    , msg.cmdPhone
                                                                    , BankNetToMomoRely.newBuilder()
                                                                            .setResult(false)
                                                                            .setRcode(-1)
                                                                            .build()
                                                                            .toByteArray()
                                                            );
                                                        }

                                                    } else {
                                                        buf = MomoMessage.buildBuffer(
                                                                MsgType.BANK_NET_TO_MOMO_REPLY_VALUE
                                                                , msg.cmdIndex
                                                                , msg.cmdPhone
                                                                , BankNetToMomoRely.newBuilder()
                                                                        .setResult(false)
                                                                        .setRcode(-1) // system error
                                                                        .build()
                                                                        .toByteArray()
                                                        );
                                                    }

                                                    //for socket
                                                    if (sock != null) {
                                                        mCommon.writeDataToSocket(sock, buf);
                                                    }

                                                    //for web
                                                    if (webCallback != null) {

                                                        webCallback.handle(buf);
                                                    }

                                                    log.writeLog();
                                                }
                                            });
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    public void processAddOrUpdate(final NetSocket sock, final MomoMessage msg) {
        CardAddOrUpdate request;
        try {
            request = CardAddOrUpdate.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        ArrayList<JsonObject> arrayList = new ArrayList<>();

        for (int i = 0; i < request.getCardListCount(); i++) {

            JsonObject jo = Card.buildCardJsonObj(request.getCardList(i));
            if (jo != null) {
                arrayList.add(jo);
            }
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processAddOrUpdate");
        log.add("received size", arrayList.size());


        card.addOrUpdate(msg.cmdPhone, arrayList, new Handler<ArrayList<MomoProto.CardItem>>() {
            @Override
            public void handle(ArrayList<MomoProto.CardItem> arr) {

                log.add("processed size", (arr != null ? arr.size() : 0));
                CardAddOrUpdateReply.Builder builder = CardAddOrUpdateReply.newBuilder();

                for (MomoProto.CardItem item : arr) {
                    builder.addBankNetToMomoList(item);
                }

                Buffer buffer = MomoMessage.buildBuffer(
                        MsgType.CARD_ADD_OR_UPDATE_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray()
                );

                mCommon.writeDataToSocket(sock, buffer);

                log.writeLog();
            }
        });
    }

    public void processCardSyncAtFirstTime(final NetSocket sock, final MomoMessage msg) {

        card.findAll(msg.cmdPhone, new Handler<ArrayList<Card.Obj>>() {
            @Override
            public void handle(ArrayList<Card.Obj> arr) {
                CardSyncFirstTimeReply.Builder builder = CardSyncFirstTimeReply.newBuilder();
                String bankId = "";
                String bankName = "";
                String cardTypeId = "";
                for (int i = 0; i < arr.size(); i++) {

                    bankId = arr.get(i).bankid;
                    cardTypeId = arr.get(i).cardType;
                    if ("sbs".equalsIgnoreCase(bankId)) {
                        bankName = VMCardType.convertBankNameFromCardTypeId(cardTypeId);
                    } else {
                        bankName = arr.get(i).bank_name;
                    }
                    builder.addCardList(MomoProto.CardItem.newBuilder()
                                    .setCardHolderName(arr.get(i).card_holder_name)
                                    .setCardHolderNumber(arr.get(i).card_holder_number)
                                    .setBankId(arr.get(i).bankid)
                                    .setBankType(arr.get(i).bank_type)
                                    .setRowId(arr.get(i).row_id)
                                    .setLastSyncTime(arr.get(i).last_sync_time)
                                    .setCardHolderMonth(arr.get(i).card_holder_month)
                                    .setCardHolderYear(arr.get(i).card_holder_year)
                                    .setBankName(bankName)
                                    .setStatus(arr.get(i).status)
                                    .setCardType(arr.get(i).cardType)
                                    .setCardId(arr.get(i).cardId)
                                    .setLastSyncTime(arr.get(i).last_sync_time)
                    );
                }
                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.CARD_SYNC_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buf);
            }
        });
    }

    public void processTranSync(final NetSocket sock, final MomoMessage msg) {
        StandardSync request;
        try {
            request = StandardSync.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasLastUpdateTime()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }
        int last_sync_time = glbCfg.getObject("sync_time").getInteger("last_sync_time", 1);
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("client last sync time tran", request.getLastUpdateTime());
        log.add("time vn", Misc.dateVNFormatWithTime(request.getLastUpdateTime()));
        log.add("function", "transDb.find");
        log.add("last_sync_time", last_sync_time);
        long pastXMonths = 1000L * 60 * 60 * 24 * 30 * last_sync_time;
        long requestTime = request.getLastUpdateTime() == 0 ? System.currentTimeMillis() - pastXMonths : request.getLastUpdateTime();

        transDb.find(msg.cmdPhone, requestTime, isStoreApp, new Handler<ArrayList<TranObj>>() {
            @Override
            public void handle(ArrayList<TranObj> arr) {

                log.add("sync tran with size", (arr != null ? arr.size() : 0));

                TranHisSyncReply.Builder builder = TranHisSyncReply.newBuilder();
                Buffer buf;
                if (arr != null && arr.size() > 0) {

                    TimerTranSync tTranSync = new TimerTranSync(vertx, logger, sock, msg, arr, glbCfg);
                    tTranSync.start();
                    log.add("timer for sync tran is started", "");
                } else {

                    buf = MomoMessage.buildBuffer(
                            MsgType.TRAN_SYNC_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder
                                    .setResult(false)
                                    .build()
                                    .toByteArray()
                    );
                    mCommon.writeDataToSocket(sock, buf);

                    // Send TRAN_SYNC_FINISH
                    MomoMessage momoMessage = new MomoMessage(MsgType.TRAN_SYNC_FINISH_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , "".getBytes());
                    mCommon.writeDataToSocket(sock, momoMessage.toBuffer());
                }
                log.writeLog();
            }
        });
    }

    public void processTranStatistic(final NetSocket sock, final MomoMessage msg, final SockData data) {

        phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if (data != null)
                    data.setPhoneObj(obj, logger, "");

                if (obj != null && obj.isNamed == true) {
                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.TRAN_STATISTIC_PER_DAY_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , TranStatisticPerDayReply.newBuilder()
                                    .setMaxAmount(NONAME_MAX_TRAN_VALUE_PER_DAY)
                                    .setTotalAmount(0)
                                    .setTotalCount(NONAME_MAX_TRAN_COUNT_PER_DAY)
                                    .setRemainCount(NONAME_MAX_TRAN_COUNT_PER_DAY)
                                    .build().toByteArray()
                    );
                    mCommon.writeDataToSocket(sock, buf);
                    return;
                } else {
                    //Check trong bang visa.
                    groupManageDb.findOne("0" + msg.cmdPhone, new Handler<GroupManageDb.Obj>() {
                        @Override
                        public void handle(GroupManageDb.Obj obj) {
                            if (obj != null) {
                                Buffer buf = MomoMessage.buildBuffer(
                                        MsgType.TRAN_STATISTIC_PER_DAY_REPLY_VALUE
                                        , msg.cmdIndex
                                        , msg.cmdPhone
                                        , TranStatisticPerDayReply.newBuilder()
                                                .setMaxAmount(NONAME_MAX_TRAN_VALUE_PER_DAY)
                                                .setTotalAmount(0)
                                                .setTotalCount(NONAME_MAX_TRAN_COUNT_PER_DAY)
                                                .setRemainCount(NONAME_MAX_TRAN_COUNT_PER_DAY)
                                                .build().toByteArray()
                                );
                                mCommon.writeDataToSocket(sock, buf);
                                return;
                            } else {
                                transDb.getStatisticTranPerDay(msg.cmdPhone, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject json) {
                                        long totalValue = json.getLong("value");
                                        int tranCount = json.getInteger("count");

                                        Buffer buf = MomoMessage.buildBuffer(
                                                MsgType.TRAN_STATISTIC_PER_DAY_REPLY_VALUE
                                                , msg.cmdIndex
                                                , msg.cmdPhone
                                                , TranStatisticPerDayReply.newBuilder()
                                                        .setMaxAmount(NONAME_MAX_TRAN_VALUE_PER_DAY)
                                                        .setTotalAmount(totalValue)
                                                        .setTotalCount(NONAME_MAX_TRAN_COUNT_PER_DAY)
                                                        .setRemainCount(NONAME_MAX_TRAN_COUNT_PER_DAY - tranCount)
                                                        .build().toByteArray()
                                        );

                                        mCommon.writeDataToSocket(sock, buf);
                                    }
                                });
                            }
                        }
                    });
                }
            }

        });

//        transDb.getStatisticTranPerDay(msg.cmdPhone, new Handler<JsonObject>() {
//            @Override
//            public void handle(JsonObject json) {
//                long totalValue = json.getLong("value");
//                int tranCount = json.getInteger("count");
//
//                Buffer buf = MomoMessage.buildBuffer(
//                        MsgType.TRAN_STATISTIC_PER_DAY_REPLY_VALUE
//                        , msg.cmdIndex
//                        , msg.cmdPhone
//                        , TranStatisticPerDayReply.newBuilder()
//                                .setMaxAmount(NONAME_MAX_TRAN_VALUE_PER_DAY)
//                                .setTotalAmount(totalValue)
//                                .setTotalCount(NONAME_MAX_TRAN_COUNT_PER_DAY)
//                                .setRemainCount(NONAME_MAX_TRAN_COUNT_PER_DAY - tranCount)
//                                .build().toByteArray()
//                );
//
//                mCommon.writeDataToSocket(sock, buf);
//            }
//        });
    }

    public void processGetFee(final NetSocket sock, final MomoMessage msg) {

        GetFee request;
        try {
            request = GetFee.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }
        if (request == null || !request.hasBankId() || !request.hasChannel() || !request.hasTranType()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final String bankId = request.getBankId();
        final int channel = request.getChannel();
        final int trantype = request.getTranType();
        final int inout_city = request.getIoCity();

        //todo lay du lieu tu request
        int feetype = FeeType.MOMO_VALUE;
        if (trantype == TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE) {
            feetype = FeeType.CASH_VALUE;
        }
        final int fFeeType = feetype;
        final long amount = 0;

        logger.info("processGetFee, "
                + "phone: 0" + msg.cmdPhone
                + " bankid: " + bankId
                + " channel: " + channel
                + " trantype: " + trantype
                + " inout_city: " + inout_city
                + " feetype: " + feetype
                + " amount: " + amount);
        final Common.BuildLog log = new Common.BuildLog(logger);
        final GetFeeReply.Builder builder = GetFeeReply.newBuilder();

        //la giao dich chuyen tien
        if ("m2m".equalsIgnoreCase(bankId)) {
            log.add("existsMMPhone", "processGetFee");
            log.writeLog();
            agentsDb.existsMMPhone("0" + msg.cmdPhone, "processGetFee", new Handler<Boolean>() {
                @Override
                public void handle(Boolean exists) {

                    //retailer does m2m
                    if (exists) {

                        Buffer buf = MomoMessage.buildBuffer(
                                MsgType.GET_FEE_REPLY_VALUE
                                , msg.cmdIndex
                                , msg.cmdPhone
                                , builder.setStaticFee(0)
                                        .setDymanicFee(0)
                                        .setBankId(bankId)
                                        .setChannel(channel)
                                        .setTranType(trantype)
                                        .setIoCity(inout_city)
                                        .setFeeType(fFeeType)
                                        .build().toByteArray()
                        );

                        mCommon.writeDataToSocket(sock, buf);
                        return;
                    }

                    //end-use does m2m
                    feeDb.getFee(bankId
                            , channel
                            , trantype
                            , inout_city
                            , fFeeType
                            , amount
                            , new Handler<FeeDb.Obj>() {
                        @Override
                        public void handle(FeeDb.Obj obj) {

                            int static_fee;
                            double dynamic_fee;

                            if (obj == null) {
                                static_fee = 1100;
                                dynamic_fee = 1.2;
                            } else {
                                static_fee = obj.STATIC_FEE;
                                dynamic_fee = obj.DYNAMIC_FEE;
                            }

                            Buffer buf = MomoMessage.buildBuffer(
                                    MsgType.GET_FEE_REPLY_VALUE
                                    , msg.cmdIndex
                                    , msg.cmdPhone
                                    , builder
                                            .setStaticFee(static_fee)
                                            .setDymanicFee(dynamic_fee)
                                            .setBankId(bankId)
                                            .setChannel(channel)
                                            .setTranType(trantype)
                                            .setIoCity(inout_city)
                                            .setFeeType(fFeeType)
                                            .build().toByteArray()
                            );

                            mCommon.writeDataToSocket(sock, buf);
                        }
                    });
                }
            });
        } else {

            //not m2m transaction
            feeDb.getFee(bankId
                    , channel
                    , trantype
                    , inout_city
                    , fFeeType
                    , amount
                    , new Handler<FeeDb.Obj>() {
                @Override
                public void handle(FeeDb.Obj obj) {

                    int static_fee;
                    double dynamic_fee;

                    if (obj == null) {
                        static_fee = 1100;
                        dynamic_fee = 1.2;
                    } else {
                        static_fee = obj.STATIC_FEE;
                        dynamic_fee = obj.DYNAMIC_FEE;
                    }

                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.GET_FEE_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , builder.setStaticFee(static_fee)
                                    .setDymanicFee(dynamic_fee)
                                    .setBankId(bankId)
                                    .setChannel(channel)
                                    .setTranType(trantype)
                                    .setIoCity(inout_city)
                                    .setFeeType(fFeeType)
                                    .build().toByteArray()
                    );

                    mCommon.writeDataToSocket(sock, buf);
                }
            });
        }
    }

    public void processGetMinMaxTran(final NetSocket sock, final MomoMessage msg, final Handler<JsonArray> webcallback) {

        com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processGetMinMaxTran");

        //todo : lay du lieu tu cache memory
        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_MINMAXTRAN;

        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                JsonArray jsonArray = message.body();

                MinMaxReply.Builder builder = MinMaxReply.newBuilder();

                if (jsonArray != null && jsonArray.size() > 0) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject jo = jsonArray.get(i);
                        MinMaxTranDb.Obj o = new MinMaxTranDb.Obj(jo);
                        builder.addMinMaxList(MinMax.newBuilder()
                                .setIsNamed(o.isnamed)
                                .setMaxValue(o.maxvalue)
                                .setMinValue(o.minvalue)
                                .setTranType(o.trantype));
                    }
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.GET_MIN_MAX_TRAN_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray()
                );

                if (sock != null) {
                    mCommon.writeDataToSocket(sock, buf);
                }

                if (webcallback != null) {
                    webcallback.handle(jsonArray);
                }
            }
        });

        // todo: a Linh review nếu ok xóa xử lý củ ở dưới.

//        minMaxTranDb.getMinMaxForTran(new Handler<ArrayList<MinMaxTranDb.Obj>>() {
//            @Override
//            public void handle(ArrayList<MinMaxTranDb.Obj> arr) {
//
//                MomoProto.MinMaxReply.Builder builder = MomoProto.MinMaxReply.newBuilder();
//                JsonArray jsonArray = new JsonArray();
//
//                if (arr != null && arr.size() > 0) {
//                    for (MinMaxTranDb.Obj o : arr) {
//                        builder.addMinMaxList(MomoProto.MinMax.newBuilder()
//                                .setIsNamed(o.isnamed)
//                                .setMaxValue(o.maxvalue)
//                                .setMinValue(o.minvalue)
//                                .setTranType(o.trantype));
//
//                        jsonArray.add(new JsonObject().putBoolean(colName.MinMaxForTranCols.IS_NAMED, o.isnamed)
//                                .putNumber(colName.MinMaxForTranCols.MAX_VALUE, o.maxvalue)
//                                .putNumber(colName.MinMaxForTranCols.MIN_VALUE, o.minvalue)
//                                .putNumber(colName.MinMaxForTranCols.TRAN_TYPE, o.trantype));
//
//                    }
//                }
//
//                Buffer buf = MomoMessage.buildBuffer(
//                        MomoProto.MsgType.GET_MIN_MAX_TRAN_REPLY_VALUE
//                        , msg.cmdIndex
//                        , msg.cmdPhone
//                        , builder.build().toByteArray()
//                );
//                if (sock != null) {
//                    mCommon.writeDataToSocket(sock, buf);
//                }
//
//                if (webcallback != null) {
//                    webcallback.handle(jsonArray);
//                }
//            }
//        });
    }

    public void processRemoveSavedBill(final NetSocket sock, final MomoMessage msg) {
        RemoveSavedBill rawRequest;
        try {
            rawRequest = RemoveSavedBill.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            rawRequest = null;
        }
        if (rawRequest == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        for (Bill bill : rawRequest.getBillsList()) {
            final BillsDb.Obj obj = new BillsDb.Obj();
            obj.billId = bill.getBillId();
            obj.providerId = bill.getProviderId();
            obj.payChanel = 0;

            mBillsDb.deleteBill(msg.cmdPhone, obj, new Handler<Boolean>() {
                @Override
                public void handle(Boolean result) {
                    logger.debug(String.format("%s deleted bill:  providerId: %s, billId: %s", msg.cmdPhone, obj.providerId, obj.billId));
                }
            });
        }

        Buffer buf = MomoMessage.buildBuffer(
                MsgType.REMOVE_SAVED_BILL_REPLY_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , RemoveSavedBillReply.newBuilder()
                        .setResult(RemoveSavedBillReply.Result.SUCCESS)
                        .build().toByteArray()
        );
        mCommon.writeDataToSocket(sock, buf);
    }

    public void syncNotification(final NetSocket sock, SockData data, final MomoMessage msg) {

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);

        StandardSync rawRequest;
        try {
            rawRequest = StandardSync.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            rawRequest = null;
        }
        if (rawRequest == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final StandardSync request = rawRequest;

        log.add("client last sync time noti", request.getLastUpdateTime());
        log.add("time vn", Misc.dateVNFormatWithTime(request.getLastUpdateTime()));

        final List<Integer> status = Notification.getAllStatusWithoutDeleted();
        notificationToolDb.getAllNotification(msg.cmdPhone, request.getLastUpdateTime(), status, new Handler<List<Notification>>() {
            @Override
            public void handle(final List<Notification> listNotiFromTool) {
                notificationDb.getAllNotificationForSync(msg.cmdPhone, request.getLastUpdateTime(), status, new Handler<List<Notification>>() {

                    @Override
                    public void handle(List<Notification> notifications) {

                        for(Notification notiTmp : listNotiFromTool)
                        {
                            notifications.add(notiTmp);
                        }
                        log.add("notis size", notifications == null ? 0 : notifications.size());

                        if (notifications == null || notifications.size() == 0) {
                            log.add("send finish sync", "ok");
                            responseNotificationSyncFinish(sock, msg);
                            log.writeLog();
                            return;
                        }

                        ArrayList<Notification> notis = new ArrayList<>();
                        for (Notification noti : notifications) {
                            notis.add(noti);
                        }

                        log.add("timer sync noti is started", "");
                        TimerNotiSync timerNotiSync = new TimerNotiSync(vertx, logger, sock, msg, notis, glbCfg);
                        timerNotiSync.start();
                        log.writeLog();
                    }
                });
            }
        });


        /*notificationDb.count(msg.cmdPhone, request.getLastUpdateTime(), status, new Handler<Long>() {
            @Override
            public void handle(final Long count) {
                final int PAGE_SIZE = 5;
                int lastPage = (int) (count / PAGE_SIZE);
                if (count % PAGE_SIZE > 0)
                    lastPage++;

                final int lastPageNumber = lastPage;

                if (count <= 0) {
                    logger.debug(String.format("%d has no notifications.", msg.cmdPhone));
                    responseNotificationSyncFinish(sock, msg);
                    return;
                }

                // response notification pages to clients.
                long timerId = vertx.setPeriodic(300, new Handler<Long>() {
                    int sendingPage = 0;
                    boolean responseNextNotification = true;

                    @Override
                    public void handle(Long timerId) {
                        if (!responseNextNotification) {
                            return;
                        }
                        if (sendingPage < lastPageNumber) {
                            sendingPage++;
                            notificationDb.getAllNotificationPage(msg.cmdPhone, PAGE_SIZE, sendingPage, request.getLastUpdateTime(), status, new Handler<List<Notification>>() {
                                @Override
                                public void handle(List<Notification> models) {
                                    MomoProto.NotificationSyncReply.Builder responseBody = MomoProto.NotificationSyncReply.newBuilder();
                                    for (Notification model : models) {
                                        responseBody.addNotifications(model.toMomoProto());
                                    }
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.NOTIFICATION_SYNC_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            responseBody.build().toByteArray()
                                    );
                                    logger.debug(String.format("Response %d notifications to %d.", models.size(), msg.cmdPhone));
                                    mCommon.writeDataToSocket(sock, buf);
                                }
                            });
                        } else { // no more pages to be sent.
                            logger.debug(String.format("Timer(id:%d) sending notifications to %d has been canceled.", timerId, msg.cmdPhone));
                            responseNotificationSyncFinish(sock, msg);
                            vertx.cancelTimer(timerId);
                        }
                    }
                });
                logger.debug(String.format("Create timer(id:%d) for sending notifications to %d.", timerId, msg.cmdPhone));
            }
        });*/

    }

    private void responseNotificationSyncFinish(final NetSocket sock, MomoMessage msg) {
        Buffer buf = MomoMessage.buildBuffer(
                MsgType.NOTIFICATION_SYNC_FINISH_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                "".getBytes()
        );
        mCommon.writeDataToSocket(sock, buf);
    }

    public void processDeleteTrans(final NetSocket sock, final MomoMessage msg, final Handler<JsonObject> callback) {

        IdList request;
        try {
            request = IdList.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        if (request.getIdsCount() == 0) {
            Buffer buffer = MomoMessage.buildBuffer(MsgType.TRAN_DELETE_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , IdList.newBuilder()
                    .build().toByteArray());
            mCommon.writeDataToSocket(sock, buffer);
            return;
        }

        //get tranIds
        final ArrayList<Long> arrTrans = new ArrayList<>();

        transDb.delRows(msg.cmdPhone, arrTrans, new Handler<Boolean>() {
            @Override
            public void handle(Boolean b) {
                IdList.Builder builder = IdList.newBuilder();
                if (b) {
                    for (long item : arrTrans) {
                        builder.addIds(item);
                    }
                }

                Buffer buffer = MomoMessage.buildBuffer(MsgType.TRAN_DELETE_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buffer);
            }
        });

    }

    public void checkTranStatus(final NetSocket sock, final MomoMessage msg) {

        IdList request;
        try {
            request = IdList.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        if (request.getIdsCount() == 0) {
            Buffer buffer = MomoMessage.buildBuffer(MsgType.CHECK_TRAN_STATUS_REPLY_VALUE
                    , msg.cmdIndex
                    , msg.cmdPhone
                    , IdList.newBuilder()
                    .build().toByteArray());
            mCommon.writeDataToSocket(sock, buffer);
            return;
        }

        //danh sach cac tranId tu client gui xuong
        final ArrayList<Long> cIds = new ArrayList<>();

        for (int i = 0; i < request.getIdsCount(); i++) {
            cIds.add(request.getIds(i));
        }

        //lay danh sach cac tranId co tren server
        transDb.getTranExistOnServer(msg.cmdPhone, cIds, new Handler<ArrayList<Long>>() {
            @Override
            public void handle(ArrayList<Long> exitList) {

                //khong co tren server
                ArrayList<Long> notExitList = new ArrayList<Long>();

                //co tren server
                ArrayList<Long> remainList = new ArrayList<Long>();

                if (exitList == null || exitList.size() == 0) {
                    notExitList = cIds;
                } else {

                    for (int i = 0; i < cIds.size(); i++) {
                        if (!exitList.contains(cIds.get(i))) {
                            notExitList.add(cIds.get(i));
                        } else {
                            remainList.add(cIds.get(i));
                        }
                    }
                }

                //gui danh sach nhung tran khong ton tai tren server
                IdList.Builder builder = IdList.newBuilder();
                builder.addAllIds(notExitList);

                Buffer buffer = MomoMessage.buildBuffer(MsgType.CHECK_TRAN_STATUS_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());
                mCommon.writeDataToSocket(sock, buffer);

                if (remainList == null || remainList.size() == 0) {
                    return;
                }

                transDb.getTransInList(msg.cmdPhone, remainList, new Handler<ArrayList<TranObj>>() {
                    @Override
                    public void handle(ArrayList<TranObj> arr) {

                        //co tranhis thi tra ve cho client, khong thi thoi
                        if (arr != null && arr.size() > 0) {
                            TimerTranSync tTranSync = new TimerTranSync(vertx, logger, sock, msg, arr, glbCfg);
                            tTranSync.start();
                        }
                    }
                });
            }
        });
    }

    public void setDynamicConfigDb(DynamicConfigDb dynamicConfigDb) {
        this.dynamicConfigDb = dynamicConfigDb;
    }

    public void getDynamicConfig(final NetSocket sock, final MomoMessage msg) {
        GetDynamicConfig rawRequest;
        try {
            rawRequest = GetDynamicConfig.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            rawRequest = null;
        }

        if (rawRequest == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }
        final GetDynamicConfig request = rawRequest;

        String key = request.getName();
        DynamicConfig filter = new DynamicConfig();
        filter.setModelId(key);
        filter.setName(key);
        dynamicConfigDb.findOne(filter, new Handler<DynamicConfig>() {
            @Override
            public void handle(DynamicConfig result) {
                GetDynamicConfigReply.Builder builder = GetDynamicConfigReply.newBuilder();
                builder.setName(request.getName());
                if (result == null) {
                    builder.setValue("");
                } else {
                    builder.setValue(result.getValue());
                }

                Buffer buffer = MomoMessage.buildBuffer(MsgType.GET_DYNAMIC_CONFIG_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());
                mCommon.writeDataToSocket(sock, buffer);
            }
        });
    }

    public void createOrder123Phim(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> webcallback) {
        CreateOrder123Phim req;
        try {
            req = CreateOrder123Phim.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            req = null;
        }

        if (req == null || !req.hasSessionId() || !req.hasDeviceId()) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        /*optional uint64 session_id = 1; // suat chieu phim
        repeated string seat_list = 2; // danh sach ghe dat
        optional string device_id = 3; //android/ios
        optional string phone_number=4;
        optional string email =5;*/


        String session_id = req.getSessionId() + "";
        String device_id = ("android".equalsIgnoreCase(req.getDeviceId()) ? "22" : "21");

        //lay session time va phone
        String tmpData = (req.getPhoneNumber() == null ? "" : req.getPhoneNumber());

        String[] arrData = tmpData.split(MomoMessage.BELL);

        String phone_number = ((arrData != null && arrData.length > 0) ? "0" + DataUtil.strToInt(arrData[0]) : "");
        String session_time = ((arrData != null && arrData.length > 1) ? arrData[1].trim() : "");
        String film_name = ((arrData != null && arrData.length > 2) ? arrData[2].trim() : "");
        String galaxy_info = ((arrData != null && arrData.length > 3) ? arrData[3].trim() : "");

        String email = (req.getEmail() == null ? "" : req.getEmail());

        final VngUtil.ObjCreate reqObj = new VngUtil.ObjCreate();
        reqObj.session_id = session_id;
        reqObj.customer_phone = phone_number;
        reqObj.customer_email = email;
        reqObj.list_seat = req.getSeatListList();
        reqObj.device_id = device_id;
        reqObj.session_time = session_time;
        reqObj.film_name = film_name;
        reqObj.galaxy_info = galaxy_info;

        if (data != null) {
            data.Cinema = "glx";
            if ("".equalsIgnoreCase(reqObj.galaxy_info)) {
                data.Cinema = "bhd";
            }
        }

        if (data != null && data.getPhoneObj() != null) {

            reqObj.customer_name = data.getPhoneObj().name;


            //huy lenh dat truoc do
            if (!"".equalsIgnoreCase(data.lastCinemaInvoiceNo)) {
                Vinagame123Phim.cancel123Phim(vertx
                        , data.lastCinemaInvoiceNo
                        , logger, msg.cmdPhone, new Handler<VngUtil.ObjReply>() {
                    @Override
                    public void handle(VngUtil.ObjReply objReply) {
                        logger.info("cancel123Phim error " + objReply.error);
                    }
                });
            }

            doCreateOrder(sock, msg, reqObj, data, webcallback);

        } else {
            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj pObj) {

                    if (pObj != null) {

                        reqObj.customer_name = pObj.name;
                    }
                    data.setPhoneObj(pObj, logger, "");

                    doCreateOrder(sock, msg, reqObj, data, webcallback);
                }
            });
        }
    }

    private void doCreateOrder(final NetSocket socket
            , final MomoMessage msg
            , final VngUtil.ObjCreate vngCreateOrder
            , final SockData data
            , final Handler<JsonObject> webcallback) {

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "doCreateOrder");
        log.add("order info", "---------");
        log.add("sessionid", vngCreateOrder.session_id);
        log.add("cusName", vngCreateOrder.customer_name);
        log.add("cusNumber", vngCreateOrder.phone_number);
        log.add("cusEmail", vngCreateOrder.customer_email);
        log.add("film name", vngCreateOrder.film_name);
        log.add("list seat", vngCreateOrder.list_seat);
        log.add("galaxy info", vngCreateOrder.galaxy_info);

        if (vngCreateOrder.customer_name == null || "".equalsIgnoreCase(vngCreateOrder.customer_name)) {
            vngCreateOrder.customer_name = "noname";
        }

        JsonObject jsonReq = VngUtil.getJsonCreateOrder(vngCreateOrder);

        jsonReq.putString(vngClass.PHONE_NUMBER, "0" + msg.cmdPhone);
        jsonReq.putNumber(vngClass.TIME, log.getTime());
        jsonReq.putNumber(StringConstUtil.APP_CODE, data.appCode);
        jsonReq.putString(StringConstUtil.APP_OS, data.os);
        vertx.eventBus().sendWithTimeout(AppConstant.VinaGameCinemaVerticle_ADDRESS
                , jsonReq
                , 60000, new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> jRpl) {

                CreateOrder123PhimReply.Builder builder = CreateOrder123PhimReply.newBuilder();
                JsonObject jsonReturn = new JsonObject();
                VngUtil.ObjReply objReply;
                if (jRpl.succeeded()) {

                    jsonReturn = jRpl.result().body();
                    objReply = new VngUtil.ObjReply(jsonReturn);

                    builder.setRcode(objReply.error);

                    log.add("invoice_no", objReply.invoice_no);
                    log.add("ticket_code", objReply.ticket_code);
                    log.add("price_before", objReply.price_before);
                    log.add("price_after", objReply.price_after);
                    log.add("list_price", jsonReturn.getObject(vngClass.Res.list_price, new JsonObject()));
                    log.add("reply session_id", objReply.session_id);
                    log.add("reply session_time", objReply.session_time);
                    log.add("reply film name", objReply.film_name);

                    log.add("error", objReply.error);
                    log.add("errordesc", Phim123Errors.getDesc(objReply.error));

                    if (objReply.error == 0) {

                        data.lastCinemaInvoiceNo = objReply.invoice_no;

                        builder.setInvoiceNo(objReply.invoice_no)
                                .setTicketCode(objReply.ticket_code)
                                .setTotalAmount(objReply.price_after);

                        if (objReply.list_price != null && objReply.list_price.size() > 0) {

                            for (VngUtil.Price price : objReply.list_price) {
                                builder.addSeatList(Seat.newBuilder()
                                        .setSeat(price.key)
                                        .setPrice(price.value));
                            }
                        }
                    }
                } else {

                    log.add("result", "REQUEST_TIMEOUT_VALUE");
                    log.add("errdesc", "request createOrder timeout");
                    builder.setRcode(5008); // request timeout = banknet error
                    jsonReturn.putNumber(vngClass.Res.error, 5008);
                    jsonReturn.putString(vngClass.Res.desc, "request createOrder timeout");
                }

                log.writeLog();

                if (socket != null) {
                    Buffer buffer = MomoMessage.buildBuffer(MsgType.CREATE_ORDER_123PHIM_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , builder.build().toByteArray());

                    mCommon.writeDataToSocket(socket, buffer);
                }

                if (webcallback != null) {
                    webcallback.handle(jsonReturn);
                }
            }
        });
    }

    public void getPromotionList(final NetSocket sock, final MomoMessage msg) {

        //todo gui xuong bus promotion

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;

        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);

                GetPromotionReply.Builder builder = GetPromotionReply.newBuilder();

                if (array != null && array.size() > 0) {
                    ArrayList<PromotionDb.Obj> objs = new ArrayList<>();

                    for (Object o : array) {
                        objs.add(new PromotionDb.Obj((JsonObject) o));
                    }

                    for (int i = 0; i < objs.size(); i++) {
                        /*optional string id=1;
                        optional uint64 from_date =2;
                        optional uint64 to_date =3;
                        optional bool is_active =4;
                        optional string promotion_name=6;*/

                        builder.addPromotionList(Promotion.newBuilder()
                                        .setId(objs.get(i).ID)
                                        .setFromDate(objs.get(i).DATE_FROM)
                                        .setToDate(objs.get(i).DATE_TO)
                                        .setIsActive(objs.get(i).ACTIVE)
                                        .setPromotionName(objs.get(i).NAME)
                        );
                    }

                }

                Buffer buf = MomoMessage.buildBuffer(MsgType.GET_PROMOTION_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buf);
            }
        });

        /*promotionDb.getPromotions(new Handler<ArrayList<PromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<PromotionDb.Obj> objs) {
                MomoProto.GetPromotionReply.Builder builder = MomoProto.GetPromotionReply.newBuilder();
                if (objs != null && objs.size() > 0) {
                    for (int i = 0; i < objs.size(); i++) {
                        *//*optional string id=1;
                        optional uint64 from_date =2;
                        optional uint64 to_date =3;
                        optional bool is_active =4;
                        optional string promotion_name=6;*//*

                        builder.addPromotionList(MomoProto.Promotion.newBuilder()
                                        .setId(objs.get(i).ID)
                                        .setFromDate(objs.get(i).DATE_FROM)
                                        .setToDate(objs.get(i).DATE_TO)
                                        .setIsActive(objs.get(i).ACTIVE)
                                        .setPromotionName(objs.get(i).NAME)
                        );
                    }
                }

                Buffer buf = MomoMessage.buildBuffer(MomoProto.MsgType.GET_PROMOTION_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buf);

            }
        });*/
    }

    public void getPromotionDetail(final NetSocket sock, final MomoMessage msg) {
        GetPromotionDetail data = null;
        try {
            data = GetPromotionDetail.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
        }
        if (data == null || (!data.hasId() && !"".equalsIgnoreCase(data.getId()))) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_DETAIL_BY_PROMO_ID;
        promoReqObj.PROMO_ID = data.getId();

        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                GetPromotionDetailReply.Builder builder = GetPromotionDetailReply.newBuilder();

                //todo phai build lai noi dung ben duoi nhe

                /*Giới thiệu cho bạn bè và người thân của bạn cùng sử dụng MoMo để chuyển tiền và thanh toán tiện lợi. Cả hai đều được %sđ vào tài khoản nghe gọi, Khi người được giới thiệu thanh toán dịch vụ bất kỳ có giá trị tối thiểu %sđ trong vòng %s ngày*/


                if (jsonObject != null) {
                    PromotionDb.Obj obj = new PromotionDb.Obj(jsonObject);
                    builder.setIntroData(String.format(obj.INTRO_DATA
                                    , Misc.formatAmount(obj.PER_TRAN_VALUE).replace(",", ".")
                                    , Misc.formatAmount(obj.TRAN_MIN_VALUE).replace(",", ".")
                                    , obj.DURATION_TRAN + "")
                    );
                    builder.setIntroSms(String.format(obj.INTRO_SMS
                            , (obj.PER_TRAN_VALUE / 1000) + ""));
                    builder.setDuration(obj.DURATION_TRAN);

                    com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
                    log.setPhoneNumber("0" + msg.cmdPhone);
                    log.add("NAME", obj.NAME);
                    log.add("PER_TRAN_VALUE", obj.PER_TRAN_VALUE);
                    log.add("ACTIVE", obj.ACTIVE);
                    log.add("MIN_TIMES", obj.MIN_TIMES);
                    log.add("MAX_TIMES", obj.MAX_TIMES);
                    log.add("DATE_FROM", obj.DATE_FROM);
                    log.add("DATE_TO", obj.DATE_TO);
                    log.add("DURATION", obj.DURATION);
                    log.add("DURATION_TRAN", obj.DURATION_TRAN);
                    log.add("INTRO_DATA", obj.INTRO_DATA);
                    log.add("INTRO_SMS", obj.INTRO_SMS);
                    log.writeLog();

                }

                Buffer buf = MomoMessage.buildBuffer(MsgType.GET_PROMOTION_DETAIL_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buf);
            }
        });

        promotionDb.getPromotionById(data.getId(), new Handler<PromotionDb.Obj>() {
            @Override
            public void handle(PromotionDb.Obj obj) {
                GetPromotionDetailReply.Builder builder = GetPromotionDetailReply.newBuilder();
                if (obj != null) {

                    /*
                    "introdata" : "Giới thiệu cho bạn bè và người thân của bạn cùng sử dụng MoMo để chuyển tiền và thanh toán tiện lợi. Cả hai đều được 10.000đ vào tài khoản, Khi người được giới thiệu thanh toán dịch vụ bất kỳ có giá trị tối thiểu 20.000đ trong vòng 30 ngày",
                    "introsms" : "Su dung MoMo de chuyen nhan tien va thanh toan tien loi, nhan ngay 10k khi thanh toan. Dung so dt cua minh lam Ma gioi thieu  khi dang ky nhe. Tai ung dung tai: http://momo.vn/download",*/

                    builder.setIntroData(String.format(obj.INTRO_DATA
                                    , Misc.formatAmount(obj.PER_TRAN_VALUE).replace(",", ".")
                                    , Misc.formatAmount(obj.TRAN_MIN_VALUE).replace(",", ".")
                                    , obj.DURATION + "")
                    );
                    builder.setIntroSms(String.format(obj.INTRO_SMS
                            , (obj.PER_TRAN_VALUE / 1000) + ""));
                    builder.setDuration(obj.DURATION);

                }

                Buffer buf = MomoMessage.buildBuffer(MsgType.GET_PROMOTION_DETAIL_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buf);
            }
        });

    }

    public void getSmartlinkUrl(NetSocket sock, MomoMessage msg) {
        GetSmartLinkUrl data = null;
        try {
            data = GetSmartLinkUrl.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
        }
        if (data == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        SmartLinkRequest x = new SmartLinkRequest();
        x.url = "https://paymentcert.smartlink.com.vn:8181/vpcpay.do"; //https://paymentcert.smartlink.com.vn:8181/vpcpay.do
        x.secureHashSecret = "198BE3F2E8C75A53F38C1C4A5B6DBA27"; //198BE3F2E8C75A53F38C1C4A5B6DBA27
        x.vpc_Merchant = "SMLTEST"; //SMLTEST
        x.vpc_AccessCode = "ECAFAB"; //ECAFAB
        x.vpc_MerchTxnRef = UUID.randomUUID().toString();
        x.vpc_Amount = data.getAmount() + "00";
        x.vpc_ReturnURL = "http://recharge.momo.vn:28080/MSRecharge/ms_return.jsp";
        x.vpc_OrderInfo = String.valueOf(msg.cmdPhone);
        x.vpc_TicketNo = "Nap tien MoMo su dung smartlink";
        x.vpc_BackURL = "http://momo.vn/";

        String generatedUrl = x.buildUrl();
        logger.info("SmartLink url:" + generatedUrl);
        GetSmartLinkUrlReply.Builder builder = GetSmartLinkUrlReply.newBuilder();
        builder.setUrl(generatedUrl);
        Buffer buf = MomoMessage.buildBuffer(MsgType.GET_SMARTLINK_URL_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone, builder.build().toByteArray());
        mCommon.writeDataToSocket(sock, buf);
    }

    public void getInviteeStatistic(final NetSocket sock, final MomoMessage msg) {

        promotedDb.getAllByInviter(msg.cmdPhone, new Handler<ArrayList<PromotedDb.Obj>>() {
            @Override
            public void handle(ArrayList<PromotedDb.Obj> objs) {
                InviteStatisticReply.Builder builder = InviteStatisticReply.newBuilder();

                if (objs != null && objs.size() > 0) {
                    for (PromotedDb.Obj o : objs) {
                        builder.addInviteeList(Invitee.newBuilder()
                                        .setName(o.INVITEE_NAME)
                                        .setPhone("0" + o.INVITEE_NUMBER)
                                        .setTime(o.PROMOTED_TIME)
                        );
                    }
                }

                Buffer buf = MomoMessage.buildBuffer(MsgType.INVITE_STATISTIC_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());
                mCommon.writeDataToSocket(sock, buf);
            }
        });
    }

    /**
     * lay thong tin cac loai dich vu theo phan loai
     *
     * @param sock
     * @param msg
     */
    public void getServices(final NetSocket sock, final MomoMessage msg, final Handler<ArrayList<ServiceDb.Obj>> webCallback) {

        Service service = null;
        try {
            service = Service.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
        }

        if (service == null) {
            mCommon.writeErrorToSocket(sock);

            return;
        }

        if (service.getServiceType() == null) {
            mCommon.writeErrorToSocket(sock);

            return;
        }

        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE;

        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {

                ArrayList<ServiceDb.Obj> objs = mCommon.getServiceList(message.body());

                if (webCallback != null) {
                    webCallback.handle(objs);
                    return;
                }

                ServiceReply.Builder builder = ServiceReply.newBuilder();

                if (objs != null && objs.size() > 0) {

                    TimerService tService = new TimerService(vertx, logger, sock, msg, objs, glbCfg);
                    tService.start();


                } else {

                    //khong co du lieu

                    Buffer buf = MomoMessage.buildBuffer(MsgType.GET_SERVICE_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , builder.build().toByteArray());
                    mCommon.writeDataToSocket(sock, buf);
                }

            }
        });
    }

    public void getServicesByLastTime(final NetSocket sock, final MomoMessage msg, final Handler<ArrayList<ServiceDb.Obj>> webCallback) {

        GetServiceByLastTime service = null;
        try {
            service = GetServiceByLastTime.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
        }

        if (service == null) {
            mCommon.writeErrorToSocket(sock);

            return;
        }

        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_BY_LAST_TIME;
        serviceReq.lastTime = service.getLastTime();

        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {

                ServiceReply.Builder builder = ServiceReply.newBuilder();

                ArrayList<ServiceDb.Obj> objs = mCommon.getServiceList(message.body());

                if (webCallback != null) {
                    webCallback.handle(objs);
                    return;
                }

                if (objs != null && objs.size() > 0) {

                    TimerService tService = new TimerService(vertx, logger, sock, msg, objs, glbCfg);
                    tService.start();

                } else {

                    //khong co du lieu
                    Buffer buf = MomoMessage.buildBuffer(MsgType.GET_SERVICE_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , builder.build().toByteArray());
                    mCommon.writeDataToSocket(sock, buf);
                }

            }
        });
    }

    public void getServicesByServiceId(final NetSocket sock, final MomoMessage msg, final Handler<ServiceDb.Obj> webCallback) {

        GetServiceByServiceId service = null;
        try {
            service = GetServiceByServiceId.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "getServicesByServiceId");

        if (service == null) {
            mCommon.writeErrorToSocket(sock);

            return;
        }

        if (service.getServiceId() == null) {
            return;
        }

        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
        serviceReq.ServiceId = service.getServiceId();

        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {

                ServiceItem.Builder builder = ServiceItem.newBuilder();
                ServiceDb.Obj o = null;
                if (message.body() != null && message.body().size() > 0) {

                    JsonObject jo = message.body().get(0);
                    o = new ServiceDb.Obj(jo);

                    builder.setServiceType(o.serviceType);
                    builder.setPartnerCode(o.partnerCode);
                    builder.setServiceId(o.serviceID);
                    builder.setServiceName(o.serviceName);
                    builder.setPartnerSite(o.partnerSite);
                    builder.setIconUrl(o.iconUrl);
                    builder.setStatus(o.status);
                    builder.setTextPopup(o.textPopup);
                    builder.setHasCheckDebit(o.hasCheckDebit);
                    builder.setTitleDialog(o.titleDialog);
                    builder.setLastUpdate(o.lastUpdateTime);
                    builder.setBillidType(o.billType);
                    builder.setStar(o.star);
                    builder.setTotalForm(o.totalForm);
                    builder.setCategoryName(o.cateName);
                    builder.setCategoryId(o.cateId);

                }

                if (webCallback != null) {

                    webCallback.handle(o);

                    return;
                }

                Buffer buf = MomoMessage.buildBuffer(MsgType.GET_SERVICE_BY_SERVICE_ID_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buf);

            }
        });
    }

    public void processInviter(final NetSocket sock, final MomoMessage msg
            , final SockData data
            , GetServiceLayout fsl, final com.mservice.momo.vertx.processor.Common.BuildLog log) {

        final Notification noti = new Notification();
        noti.receiverNumber = msg.cmdPhone;
        noti.cmdId = System.currentTimeMillis();
        noti.tranId = System.currentTimeMillis();
        noti.time = System.currentTimeMillis();
        noti.priority = 2;
        noti.status = Notification.STATUS_DISPLAY;
        noti.type = NotificationType.NOTI_DETAIL_VALUE;
        noti.sms = "";

        final Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.DO_PROMO_BY_CODE;
        promoReqObj.CREATOR = "0" + msg.cmdPhone;
        promoReqObj.PROMO_CODE = (fsl.getBillId() == null ? "" : fsl.getBillId());
        promoReqObj.PROMO_NAME = "InviteFriend".toLowerCase();


        if ("".equalsIgnoreCase(fsl.getServiceId()) || !DataUtil.isValidCode(promoReqObj.PROMO_CODE, "MOMO")) {
            noti.caption = "Mã khuyến mãi không hợp lệ.";
            noti.body = "Mã khuyến mãi không hợp lệ.";
            noti.bodyIOS = "Mã khuyến mãi không hợp lệ.";

            int delayTime = 60; // ios time
            if (data != null && data.getPhoneObj() != null && "android".equals(data.getPhoneObj().phoneOs)) {
                delayTime = 120;
            }

            vertx.setTimer(delayTime * 1000, new Handler<Long>() {
                @Override
                public void handle(Long aLong) {
                    Misc.sendNoti(vertx, noti);
                }
            });
            return;
        }


        //con thoi gian lock
        if (data != null && data.getPhoneObj() != null && System.currentTimeMillis() < data.inviteLockTill) {
            Buffer buf = MomoMessage.buildBuffer(
                    MsgType.HELLO_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    HelloReply.newBuilder()
                            .setRcode(HelloReply.ResultCode.NOT_LOG_ON_VALUE)
                            .setVersionName("")
                            .setVersionCode(1)
                            .setRegStatus(RegStatus.newBuilder()
                                            .setIsSetup(true)
                                            .setIsReged(data.getPhoneObj().isReged)
                                            .setIsNamed(data.getPhoneObj().isNamed)
                                            .build()
                            )
                            .build().toByteArray()
            );
            mCommon.writeDataToSocket(sock, buf);
            return;

        } else if (data != null && data.getPhoneObj() != null && data.inviteTryCount < 3) {

            data.inviteTryCount++;

        } else if (data != null && data.getPhoneObj() != null && data.inviteTryCount >= 3) {
            //lock tai khoan 5 phut
            data.inviteLockTill = System.currentTimeMillis() + 5 * 60 * 1000L;

            Buffer buf = MomoMessage.buildBuffer(
                    MsgType.HELLO_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    HelloReply.newBuilder()
                            .setRcode(HelloReply.ResultCode.NOT_LOG_ON_VALUE)
                            .setVersionName("")
                            .setVersionCode(1)
                            .setRegStatus(RegStatus.newBuilder()
                                            .setIsSetup(true)
                                            .setIsReged(data.getPhoneObj().isReged)
                                            .setIsNamed(data.getPhoneObj().isNamed)
                                            .build()
                            )
                            .build().toByteArray()
            );
            mCommon.writeDataToSocket(sock, buf);
            return;
        } else if (data != null) {
            //reset lai
            data.inviteTryCount = 0;
        }

        /*GET_PROMO_STUDENT = 1142; // body GetPromoStudent
        GET_PROMO_STUDENT_REPLY = 1143; // body standardReply*/

        if (!"".equalsIgnoreCase(fsl.getServiceId())) {
            //gui xuong bus thuc hien khuyen mai

            Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    //todo here
                    Promo.PromoResObj promoResObj = new Promo.PromoResObj(jsonObject);

                    log.add("execute promo result", "-----------");
                    log.add("result", promoResObj.RESULT);
                    log.add("error", promoResObj.ERROR);
                    log.add("desc", promoResObj.DESCRIPTION);

                    /*"priority" : 1,
                            "type" : 1,
                            "caption" : "Nạp game thành công!",
                            "body" : "Quý khách đã nạp thành công số tiền 10.000đ cho tài khoản vco, nhà cung cấp concu.",
                            "sms" : "Chuc mung quy khach da nap thanh cong 10.000d cho tai khoan vco, nha cung cap , luc 10:38 12/06/2014. TID: 442191481. Xin cam on.",
                            "tranId" : NumberLong(442191481),
                            "cmdId" : NumberLong(1402544306257),
                            "time" : NumberLong(1402544310922),
                            "status" : 2
                    */

                    //todo gui noi dung thong bao va tin nhan nhu the nao

                    if (promoResObj.RESULT == true) {

                        //reset lock count
                        data.inviteTryCount = 0;

                        noti.caption = "Nhận khuyến mãi thành công";
                        noti.body = String.format("Bạn đã nhận được %sđ từ chương trình khuyến mãi vào tài khoản nghe gọi %s"
                                , Misc.formatAmount(promoResObj.PROMO_AMOUNT).replace(",", ".")
                                , "0" + msg.cmdPhone);
                        noti.bodyIOS = String.format("Bạn đã nhận được %sđ từ chương trình khuyến mãi vào tài khoản nghe gọi %s"
                                , Misc.formatAmount(promoResObj.PROMO_AMOUNT).replace(",", ".")
                                , "0" + msg.cmdPhone);
                    } else {
                        noti.caption = "Nhận khuyến mãi không thành công";
                        noti.body = promoResObj.DESCRIPTION;
                        noti.bodyIOS = promoResObj.DESCRIPTION;
                    }

                    log.writeLog();

                    int delayTime = 60; // ios time
                    if (data != null
                            && data.getPhoneObj() != null
                            && "android".equals(data.getPhoneObj().phoneOs)) {
                        delayTime = 120;
                    }

                    vertx.setTimer(delayTime * 1000, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            Misc.sendNoti(vertx, noti);
                        }
                    });
                }
            });

        } else {
            log.add("khong nhap ma code ma doi khuyen mai ha cu", "");
            log.writeLog();
        }
        return;
    }

    /**
     * lay thong tin hoa don + thong tin cau hinh form tuong ung voi dich vu service_id
     *
     * @param sock
     * @param msg
     */

    public void getServiceLayout(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> callback) {

        GetServiceLayout serviceLayout = null;
        try {
            serviceLayout = GetServiceLayout.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
        }

        if (serviceLayout == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }
        /*
        optional string service_id = 1; // dung de check no + lay danh sach cac fielditem
        optional string bill_id     = 2; // dung de check no
        optional bool has_check_bill =3; // co check no hay khong
        */

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("service id", serviceLayout.getServiceId());
        log.add("bill id", serviceLayout.getBillId());
        log.add("service type", serviceLayout.getServiceType());
        log.add("has check debit", serviceLayout.getHasCheckBill());
        log.add("is promo", serviceLayout.getIsPromo());

        /*message GetServiceLayoutReply{
            //bat buoc phai co
            optional uint64 total_amount   =1;
            repeated FieldItem list_field  =2;

            //optional
            repeated TextValue array_price      =3; // danh sach cac menh gia co the tra
            repeated TextValue customer_info    =4; // chua thong tin khach hang
            repeated ExtraInfo extra_info       =5; // thong tin ho tro nguoi dung, vi du : chi tiet tung bill con cua dien
        }*/

        final GetServiceLayout fsl = serviceLayout;

        final GetServiceLayoutReply.Builder builder = GetServiceLayoutReply.newBuilder();

        //lay thong tin dich vu
        final com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_DETAIL_BY_SERVICE_ID;
        serviceReq.ServiceId = fsl.getServiceId();

        log.add("request json", serviceReq.toJSON());
        log.writeLog();

        //todo: dang lam theo cach cu, tam vay thoi
        if ("invitefriend".equalsIgnoreCase(fsl.getServiceId()) && (fsl.getIsPromo() == false)) {
            processInviter(sock, msg, data, fsl, log);
            return;
        }

        //lay cac fields de ve form khuyen mai
        if (fsl.getIsPromo()) {

            Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
                @Override
                public void handle(JsonArray array) {
                    ArrayList<ServiceDetailDb.Obj> detailObjs = mCommon.getServiceDetailList(array);

                    if (detailObjs != null && detailObjs.size() > 0) {

                        for (int i = 0; i < detailObjs.size(); i++) {
                            builder.addListField(MomoProto.FieldItem.newBuilder()
                                            .setFieldLabel(detailObjs.get(i).fieldLabel)
                                            .setFieldType(detailObjs.get(i).fieldType)
                                            .setIsAmount(detailObjs.get(i).isAmount)
                                            .setIsBillId(detailObjs.get(i).isBillId)
                                            .setRequire((detailObjs.get(i).required ? 1 : 0))
                                            .setKey(detailObjs.get(i).key)
                                            .setHasChild(detailObjs.get(i).hasChild)
                                            .setLine(detailObjs.get(i).line)
                            );

                            log.add("item " + (i + 1), "");
                            log.add("fieldLabel ", detailObjs.get(i).fieldLabel);
                            log.add("fieldType ", detailObjs.get(i).fieldType);
                            log.add("isAmount ", detailObjs.get(i).isAmount);
                            log.add("isBillId ", detailObjs.get(i).isBillId);
                            log.add("Required ", detailObjs.get(i).required);
                            log.add("key ", detailObjs.get(i).key);
                            log.add("has child ", detailObjs.get(i).hasChild);
                            log.add("line ", detailObjs.get(i).line);
                        }
                    }

                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.GET_SERVICE_LAYOUT_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.build()
                                    .toByteArray()
                    );

                    mCommon.writeDataToSocket(sock, buf);
                    log.writeLog();
                }
            });
            return;
        }

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {

                vertx.eventBus().send(AppConstant.ConfigVerticleService
                        , serviceReq.toJSON()
                        , new Handler<Message<JsonArray>>() {
                    @Override
                    public void handle(Message<JsonArray> message) {

                        ArrayList<ServiceDetailDb.Obj> objs = mCommon.getServiceDetailList(message.body());

                        //su dung de tra ket qua ve cho web client

                        final BillInfoService webBis = new BillInfoService();

                        //lay danh sach cac field de client ve man hinh
                        if (objs != null && objs.size() > 0) {

                            for (int i = 0; i < objs.size(); i++) {
                                builder.addListField(MomoProto.FieldItem.newBuilder()
                                                .setFieldLabel(objs.get(i).fieldLabel)
                                                .setFieldType(objs.get(i).fieldType)
                                                .setIsAmount(objs.get(i).isAmount)
                                                .setIsBillId(objs.get(i).isBillId)
                                                .setRequire((objs.get(i).required ? 1 : 0))
                                                .setKey(objs.get(i).key)
                                                .setHasChild(objs.get(i).hasChild)
                                                .setHasChild(objs.get(i).line)
                                );

                                log.add("item " + (i + 1), "");
                                log.add("fieldLabel ", objs.get(i).fieldLabel);
                                log.add("fieldType ", objs.get(i).fieldType);
                                log.add("isAmount ", objs.get(i).isAmount);
                                log.add("isBillId ", objs.get(i).isBillId);
                                log.add("Required ", objs.get(i).required);
                                log.add("key ", objs.get(i).key);
                                log.add("hasChild ", objs.get(i).hasChild);

                            }
                        }

                        //neu co check no --> goi service check no
                        executeGetServiceLayout(webBis
                                , fsl
                                , msg
                                , data
                                , log
                                , builder
                                , sock
                                , callback);
                    }
                });
            }
        });
    }

    private void executeGetServiceLayout(final BillInfoService webBis
            , final GetServiceLayout fsl
            , final MomoMessage msg
            , final SockData data
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final GetServiceLayoutReply.Builder builder
            , final NetSocket sock
            , final Handler<JsonObject> callback) {

        final String serviceId = fsl.getServiceId();
        final String serviceCode = serviceId.toUpperCase();
        final String billId = Misc.refineBillId(serviceId, fsl.getBillId(), "pe");
        boolean tmpHasCheckDebit = fsl.getHasCheckBill();

        if ("vivoo".equalsIgnoreCase(serviceId)) {
            tmpHasCheckDebit = true;
        }
        final boolean hasCheckDebit = tmpHasCheckDebit;

        Misc.getViaCoreService(vertx, serviceId, isStoreApp, new Handler<ViaConnectorObj>() {
            @Override
            public void handle(ViaConnectorObj viaConnectorObj) {

                if (hasCheckDebit) {

                    log.add("serviceCode", serviceCode);
                    //di theo duong moi
                    if (viaConnectorObj.IsViaConnectorVerticle) {

                        String pin = (data == null ? "" : data.pin);
                        ProxyRequest proxyRequest = ConnectorCommon.createCheckInforRequest(
                                "0" + msg.cmdPhone, pin,
                                serviceCode,
                                viaConnectorObj.BillPay,
                                billId);
                        log.add("request content", proxyRequest.toString());


                        //BEGIN 0000000030 CheckBill via CORE
                        ConnectorCommon.RequestCheckBill(vertx, logger, msg.cmdPhone
                                , glbCfg
                                , proxyRequest
                                , viaConnectorObj.BusName
                                , log, new Handler<BillInfoService>() {
                            @Override
                            public void handle(BillInfoService billInfoService) {

                                setBillInfReplyClient(billInfoService
                                        , builder
                                        , fsl
                                        , webBis
                                        , log
                                        , sock
                                        , msg
                                        , callback);
                                log.writeLog();
                            }
                        });
                        return;
                    }

                    //qua duong keyvaluepairs
                    Buffer getBillBuf = MomoMessage.buildBuffer(
                            SoapProto.MsgType.GET_BILL_INFO_BY_SERVICE_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            SoapProto.GetBillInfo.newBuilder()
                                    .setMpin(data.pin)
                                    .setBillId(billId)
                                    .setProviderId(serviceId)
                                    .build()
                                    .toByteArray()
                    );

                    log.add("service id", serviceId);
                    log.add("bill id", billId);

                    //send to soap
                    //BEGIN 0000000030 CheckBill via SOAP
                    vertx.eventBus().sendWithTimeout(AppConstant.SoapVerticle_ADDRESS, getBillBuf
                            , 20000
                            , new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                            if (messageAsyncResult.succeeded()) {

                                log.add("request getBillInfo succsessed", "");

                                Message<JsonObject> result = messageAsyncResult.result();

                                log.add("get bill info json result", result.body().toString());

                                int rcode = (result != null ? result.body().getInteger("rcode", 3) : 3); // loi he thong

                                //test.start
                                //rcode = 0;
                                //test.end

                                //lay thong tin bill bi loi
                                if (rcode != 0) {

                                    log.add("khong lay duoc thong tin bill", "");

                                    builder.setTotalAmount(-100); // khong lay duoc thong tin bill
                                    webBis.total_amount = -100;

                                    if (sock != null) {
                                        Buffer buf = MomoMessage.buildBuffer(
                                                MsgType.GET_SERVICE_LAYOUT_REPLY_VALUE,
                                                msg.cmdIndex,
                                                msg.cmdPhone,
                                                builder.build()
                                                        .toByteArray()
                                        );

                                        mCommon.writeDataToSocket(sock, buf);
                                    } else {
                                        log.add("sock is null at this time", "");
                                    }

                                    if (callback != null) {
                                        log.add("return web", "tra ket qua ve cho web");
                                        callback.handle(webBis.toJsonObject());
                                    }

                                    log.writeLog();
                                    return;
                                }

                                JsonObject jsonBill = result.body().getObject("json_result", null);
                                final BillInfoService bis = new BillInfoService(jsonBill);

                                //test.start
                                //bis.total_amount = 154200;
                                //test.end


                                //khong parse duoc thong tin bill
                                if (bis == null) {
                                    log.add("Khong parse duoc thong tin bill", "");

                                    builder.setTotalAmount(-100); // khong lay duoc thong tin bill
                                    if (sock != null) {
                                        Buffer buf = MomoMessage.buildBuffer(
                                                MsgType.GET_SERVICE_LAYOUT_REPLY_VALUE,
                                                msg.cmdIndex,
                                                msg.cmdPhone,
                                                builder.build()
                                                        .toByteArray()
                                        );

                                        mCommon.writeDataToSocket(sock, buf);

                                    } else {
                                        log.add("sock is null at this time", "");
                                    }

                                    //tra ket qua ve cho web
                                    if (callback != null) {
                                        webBis.total_amount = -100; // khong lay duoc thong tin bill
                                        callback.handle(webBis.toJsonObject());
                                    }

                                    log.writeLog();
                                    return;
                                }
                                setBillInfReplyClient(bis
                                        , builder
                                        , fsl
                                        , webBis
                                        , log
                                        , sock
                                        , msg
                                        , callback);
                                log.writeLog();

                            } else {
                                //xem nhu time out
                                log.add("request get billinfo timeout", "");
                                builder.setTotalAmount(-100);// khong lay duoc bill info
                                if (sock != null) {

                                    Buffer buf = MomoMessage.buildBuffer(
                                            MsgType.GET_SERVICE_LAYOUT_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.build()
                                                    .toByteArray()
                                    );

                                    mCommon.writeDataToSocket(sock, buf);

                                    log.add("tra ket qua ve cho client", "done");
                                } else {
                                    log.add("sock is null at this time", "");
                                }

                                //gui ket qua ve cho web
                                if (callback != null) {
                                    webBis.total_amount = -100;
                                    callback.handle(webBis.toJsonObject());
                                }

                            }
                            log.writeLog();
                        }
                    });
                } else {

                    log.add("call getListPriceFromLocal", "");

                    //khong check bill --> lay thong tin menh gia tu local
                    getListPriceFromLocal(fsl.getServiceId()
                            , "", log
                            , new Handler<ArrayList<ServicePackageDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<ServicePackageDb.Obj> objs) {

                            log.add("package size from local", (objs != null ? objs.size() : 0));

                            if (objs != null && objs.size() > 0) {

                                for (int i = 0; i < objs.size(); i++) {

                                    builder.addArrayPrice(TextValue.newBuilder()
                                                    .setText(objs.get(i).packageName)
                                                    .setValue(objs.get(i).packageValue)
                                                    .setId(objs.get(i).parentid)
                                    );

                                    log.add("price " + (i + 1), "");
                                    log.add("text", objs.get(i).packageName);
                                    log.add("value", objs.get(i).packageValue);
                                    log.add("parentid", objs.get(i).parentid);

                                    //build ket qua tra ve cho web
                                    BillInfoService.TextValue tv = new BillInfoService.TextValue();
                                    tv.text = objs.get(i).packageName;
                                    tv.value = objs.get(i).packageValue;
                                    tv.parentid = objs.get(i).parentid;
                                    webBis.array_price.add(tv);
                                }
                            }

                            if (sock != null) {
                                Buffer buffer = MomoMessage.buildBuffer(MsgType.GET_SERVICE_LAYOUT_REPLY_VALUE
                                        , msg.cmdIndex
                                        , msg.cmdPhone
                                        , builder.build().toByteArray());

                                mCommon.writeDataToSocket(sock, buffer);
                            }

                            //tra ket qua ve cho web
                            if (callback != null) {
                                callback.handle(webBis.toJsonObject());
                            }

                            log.writeLog();
                        }
                    });
                }
            }
        });
    }

    private void setBillInfReplyClient(BillInfoService bis
            , final GetServiceLayoutReply.Builder builder
            , final GetServiceLayout fsl
            , final BillInfoService webBis
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final NetSocket sock
            , final MomoMessage msg, final Handler<JsonObject> callback) {

        //khong tra theo danh sach menh gia
        log.add("total amount", bis.total_amount);
        builder.setTotalAmount(bis.total_amount);

        if ("vivoo".equalsIgnoreCase(fsl.getServiceId()) && bis.total_amount == 0) {

            getListPriceFromLocal(fsl.getServiceId()
                    , "", log
                    , new Handler<ArrayList<ServicePackageDb.Obj>>() {
                @Override
                public void handle(ArrayList<ServicePackageDb.Obj> objs) {

                    Common.BuildLog log1 = new Common.BuildLog(logger);
                    log1.setPhoneNumber(log.getPhoneNumber());
                    log1.setTime(log.getTime());

                    log1.add("package size from local", (objs != null ? objs.size() : 0));

                    if (objs != null && objs.size() > 0) {

                        for (int i = 0; i < objs.size(); i++) {

                            builder.addArrayPrice(TextValue.newBuilder()
                                            .setText(objs.get(i).packageName)
                                            .setValue(objs.get(i).packageValue)
                                            .setId(objs.get(i).parentid)
                            );

                            log1.add("price " + (i + 1), "********************");
                            log1.add("text", objs.get(i).packageName);
                            log1.add("value", objs.get(i).packageValue);
                            log1.add("parentid", objs.get(i).parentid);

                            //build du lieu tra ve cho web
                            BillInfoService.TextValue tv = new BillInfoService.TextValue();
                            tv.text = objs.get(i).packageName;
                            tv.value = objs.get(i).packageValue;
                            tv.parentid = objs.get(i).parentid;
                            webBis.array_price.add(tv);
                        }
                    }

                    if (sock != null) {

                        Buffer buf = MomoMessage.buildBuffer(
                                MsgType.GET_SERVICE_LAYOUT_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.build()
                                        .toByteArray()
                        );

                        mCommon.writeDataToSocket(sock, buf);

                        log.add("tra ket qua ve cho client", "done");
                    } else {
                        log.add("sock is null at this time", "");
                    }

                    if (callback != null) {
                        callback.handle(webBis.toJsonObject());
                    }
                    log1.writeLog();
                }
            });
            return;
        }

        //tra theo 1 cuc tong
        if ((bis.array_price == null || bis.array_price.size() == 0) && fsl.getHasCheckBill()) {

            builder.addArrayPrice(TextValue.newBuilder()
                    .setText(String.valueOf(bis.total_amount))
                    .setValue(String.valueOf(bis.total_amount)));

            webBis.total_amount = bis.total_amount;

        } else {

            for (int j = 0; j < bis.array_price.size(); j++) {

                builder.addArrayPrice(TextValue.newBuilder()
                                .setText(bis.array_price.get(j).text)
                                .setValue(bis.array_price.get(j).value)
                );

                log.add("price " + (j + 1), "");
                log.add("text", bis.array_price.get(j).text);
                log.add("value", bis.array_price.get(j).value);

                //build du lieu tra ve cho web
                BillInfoService.TextValue tv = new BillInfoService.TextValue();
                tv.text = bis.array_price.get(j).text;
                tv.value = bis.array_price.get(j).value;
                webBis.array_price.add(tv);
            }
        }

        //lay thong tin chi tiet hoa don
        if ((bis.extra_info != null) && (bis.extra_info.size() > 0)) {
            for (int k = 0; k < bis.extra_info.size(); k++) {

                builder.addExtraInfo(ExtraInfo.newBuilder()
                                .setBillDetailId(bis.extra_info.get(k).bill_detail_id)
                                .setAmount(bis.extra_info.get(k).amount)
                                .setFromDate(bis.extra_info.get(k).from_date)
                                .setToDate(bis.extra_info.get(k).to_date)
                );

                log.add("bill detail " + (k + 1), "");
                log.add("bill_detail_id", bis.extra_info.get(k).bill_detail_id);
                log.add("amount", bis.extra_info.get(k).amount);
                log.add("from_date", bis.extra_info.get(k).from_date);
                log.add("to_date", bis.extra_info.get(k).to_date);

                //build ket qua tra ve cho web
                BillInfoService.ExtraInfo ex = new BillInfoService.ExtraInfo();
                ex.bill_detail_id = bis.extra_info.get(k).bill_detail_id;
                ex.amount = bis.extra_info.get(k).amount;
                ex.from_date = bis.extra_info.get(k).from_date;
                ex.to_date = bis.extra_info.get(k).to_date;

                webBis.extra_info.add(ex);

            }
        }

        //lay thong tin khach hang
        if ((bis.customer_info != null) && (bis.customer_info.size() > 0)) {

            for (int l = 0; l < bis.customer_info.size(); l++) {

                builder.addCustomerInfo(TextValue.newBuilder()
                                .setText(bis.customer_info.get(l).text)
                                .setValue(bis.customer_info.get(l).value)
                );

                log.add("Customer info " + (l + 1), "");
                log.add(bis.customer_info.get(l).text, bis.customer_info.get(l).value);

                //build thong tin khach hang
                BillInfoService.TextValue tv = new BillInfoService.TextValue();
                tv.text = bis.customer_info.get(l).text;
                tv.value = bis.customer_info.get(l).value;
                webBis.customer_info.add(tv);
            }
        }

        //tra ket qua va cho client truoc
        if (sock != null) {

            Buffer buf = MomoMessage.buildBuffer(
                    MsgType.GET_SERVICE_LAYOUT_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build()
                            .toByteArray()
            );

            mCommon.writeDataToSocket(sock, buf);

            log.add("tra ket qua ve cho client", "done");
        } else {
            log.add("sock is null at this time", "");
        }

        //tra ket qua ve cho web
        if (callback != null) {
            callback.handle(webBis.toJsonObject());
        }

        //todo : save bill
        JsonObject jsonBillInfo = new JsonObject();
        jsonBillInfo.putString(colName.BillDBCols.PROVIDER_ID, fsl.getServiceId());
        jsonBillInfo.putString(colName.BillDBCols.BILL_ID, Misc.refineBillId(fsl.getServiceId(), fsl.getBillId(), "pe"));
        jsonBillInfo.putNumber(colName.BillDBCols.TOTAL_AMOUNT, bis.total_amount);

        log.add("save bill with info", jsonBillInfo.toString());

        //theo yeu cau cua APP, backend chi luu nhung bill
        // khong lay thong tin dich vu
        if (bis != null
                && bis.total_amount > 0
                && !"service".equalsIgnoreCase(fsl.getServiceType())) {
            mBillsDb.setBillAmount(msg.cmdPhone
                    , jsonBillInfo
                    , new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            log.add("save bill result", aBoolean);
                            log.writeLog();
                        }
                    }
            );
        }
    }

    private void getListPriceFromLocal(String serviceId
            , String packageType
            , com.mservice.momo.vertx.processor.Common.BuildLog log
            , final Handler<ArrayList<ServicePackageDb.Obj>> callback) {

        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_PACKAGE_LIST;
        serviceReq.ServiceId = serviceId;
        serviceReq.PackageType = packageType;

        log.add("function", "getListPriceFromLocal");
        log.add("service id", serviceId);
        log.add("package type", packageType);
        log.add("request json", serviceReq.toJSON());

        vertx.eventBus().send(AppConstant.ConfigVerticleService
                , serviceReq.toJSON()
                , new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                ArrayList<ServicePackageDb.Obj> arrayList = Misc.getPackageList(message.body());
                callback.handle(arrayList);
            }
        });
    }

    //promo area.end

    public void getNotification(final NetSocket sock, final MomoMessage msg, SockData data, Object o) {
        GetNotification request;
        try {
            request = GetNotification.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        notificationDb.find(msg.cmdPhone, request.getNotiId(), new Handler<Notification>() {
            @Override
            public void handle(Notification notification) {

                GetNotificationReply.Builder builder = GetNotificationReply.newBuilder();

                if (notification != null) {
                    builder.setNoti(notification.toMomoProto());
                }

                Buffer buffer = MomoMessage.buildBuffer(MsgType.GET_NOTIFICATION_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , builder.build().toByteArray());

                mCommon.writeDataToSocket(sock, buffer);
            }
        });
    }

    public void getUserSetting(final NetSocket sock, final MomoMessage msg, SockData data, final Handler<JsonObject> callback) {

        data.getUserSetting(new Handler<UserSetting>() {
            @Override
            public void handle(UserSetting userSetting) {
                Buffer buffer = MomoMessage.buildBuffer(MsgType.GET_USER_SETTTING_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , userSetting.toMomoProto().build().toByteArray());

                mCommon.writeDataToSocket(sock, buffer);
            }
        });
    }

    public void setUserSetting(final NetSocket sock
            , final MomoMessage msg
            , final SockData sockData
            , Handler<JsonObject> callback) {
        MomoProto.UserSetting request;
        try {
            request = MomoProto.UserSetting.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final UserSetting userSetting = new UserSetting(request);
        userSetting.setModelId("0" + msg.cmdPhone);

        userSettingDb.update(userSetting, true, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                sockData.userSetting = userSetting;
                Buffer buffer = MomoMessage.buildBuffer(MsgType.SET_USER_SETTING_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , StandardReply.newBuilder()
                        .setResult(true)
                        .setRcode(0)
                        .setDesc("success")
                        .build().toByteArray());

                mCommon.writeDataToSocket(sock, buffer);
            }
        });
    }

    public void clearAllNoti(final NetSocket sock, final MomoMessage msg) {
        notificationDb.clearAllNoti(msg.cmdPhone, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {

                int error = result.getInteger("error", -100);

                Buffer buffer = MomoMessage.buildBuffer(MsgType.CLEAR_ALL_NOTI_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , StandardReply.newBuilder()
                        .setResult(true)
                        .setRcode(error)
                        .build().toByteArray());

                mCommon.writeDataToSocket(sock, buffer);
            }
        });
    }

    public void addFields(final NetSocket sock
            , final MomoMessage msg
            , final SockData data, final Handler<JsonObject> callback) {

        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        final PhonesDb.Obj phoneObj = (data == null || data.getPhoneObj() == null ? null : data.getPhoneObj());

        if (fields == null || phoneObj == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());

        final String referal = (hashMap.containsKey(Const.REFERAL) ? hashMap.get(Const.REFERAL).trim() : "");

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.add("phone A", referal);

        final StandardReply.Builder builder = StandardReply.newBuilder();

        log.add("referal", referal);
        log.add("time", Misc.dateVNFormatWithTime(System.currentTimeMillis()));

        //vnpthn tra thuong chuong trinh uu dai vnpt ha noi
        if ("vnpthn".equalsIgnoreCase(referal) && "".equalsIgnoreCase(phoneObj.inviter)) {
            doVnptHnPromo(sock, msg, referal, builder, phoneObj);
            return;
        }

        //1. momo
        if ("momo".equalsIgnoreCase(referal)
                && "".equalsIgnoreCase(phoneObj.inviter)) {
            //todo momo
            doMomoPromo(sock, msg, builder, phoneObj);
            return;
        }

        //2. pg galaxy
        if (DataUtil.strToInt(referal) >= pgCodeMin
                && DataUtil.strToInt(referal) <= pgCodeMax
                && "".equalsIgnoreCase(phoneObj.inviter)
                ) {

            //tra thuong PG galaxy
            doPGGalaxyPromo(sock
                    , msg
                    , DataUtil.strToInt(referal)
                    , builder
                    , phoneObj);


            //tra thuong cho end-user nhap ma gioi thieu la PG
            VcbCommon.requestGiftMomoForB(vertx
                    , msg.cmdPhone
                    , String.valueOf(DataUtil.strToInt(referal))
                    , 0
                    , 0
                    , 0, phoneObj.bankPersonalId
                    , phoneObj.bank_code
                    , false);
            return;
        }

        //3. chuong trinh khuyen mai vietcombank voucher 100K amway
        if (DataUtil.strToInt(referal) >= amwayCodeMin
                && DataUtil.strToInt(referal) <= amwayCodeMax
                && "".equalsIgnoreCase(phoneObj.inviter)
                ) {

            doAmwayPromo(sock, msg, DataUtil.strToInt(referal), builder, phoneObj);
            return;
        }

        //4. vcb
        if (DataUtil.stringToVnPhoneNumber(referal) > 0 && "".equalsIgnoreCase(phoneObj.inviter)) {
            doVcbPromo(msg.cmdPhone
                    , DataUtil.strToInt(referal)
                    , sock
                    , msg
                    , builder
                    , phoneObj
                    , log);
            return;
        }

        //da nhap ma tham du chuong trinh
        if (!"".equalsIgnoreCase(phoneObj.inviter)) {

            //send result to client
            Misc.sendStandardReply(sock
                    , msg
                    , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                    , "Mã tham dự chương trình hiện tại của bạn là : " + phoneObj.inviter
                    , 1
                    , mCommon);
            return;
        }

        //send result to client
        Misc.sendStandardReply(sock
                , msg
                , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                , "Cảm ơn bạn đã gởi thông tin. Xem chi tiết trong thông báo bạn nhận."
                , 1
                , mCommon);

        //send noti
        final Notification noti = new Notification();
        noti.receiverNumber = msg.cmdPhone;
        noti.sms = "";
        noti.tranId = System.currentTimeMillis();
        noti.priority = 2;
        noti.time = System.currentTimeMillis();
        noti.category = 0;
        noti.caption = VcbNoti.InputA.Cap.InvalidCode;
        noti.body = String.format(VcbNoti.InputA.Body.InvalidCode, referal);
        noti.htmlBody = String.format(VcbNoti.InputA.Body.InvalidCode, referal);
        noti.status = Notification.STATUS_DETAIL;
        noti.type = NotificationType.NOTI_DETAIL_VALUE;
        Misc.sendNoti(vertx, noti);
    }

    //promo area.start
    private void doVcbPromo(final int phoneNumberB
            , final int phoneNumberA
            , final NetSocket sock
            , final MomoMessage msg
            , final StandardReply.Builder builder
            , final PhonesDb.Obj phoneObj
            , final com.mservice.momo.vertx.processor.Common.BuildLog log) {

        final Notification noti = new Notification();
        noti.receiverNumber = phoneNumberB;
        noti.sms = "";
        noti.tranId = System.currentTimeMillis();
        noti.priority = 2;
        noti.time = System.currentTimeMillis();
        noti.category = 0;

        final Promo.PromoReqObj vcbReqPromoRec = new Promo.PromoReqObj();
        vcbReqPromoRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
        vcbReqPromoRec.PROMO_NAME = "vcbpromo";
        log.add("promo name", vcbReqPromoRec.PROMO_NAME);

        //lay chuong trinh promotion cua VCB hien tai
        Misc.requestPromoRecord(vertx, vcbReqPromoRec, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                final PromotionDb.Obj promoRec = new PromotionDb.Obj(jsonObject);

                if (promoRec == null || promoRec.DATE_FROM == 0 || promoRec.DATE_TO == 0) {
                    log.add("********", "vcb khong co chuong trinh khuyen mai nao");

                    //send result to client
                    Misc.sendStandardReply(sock
                            , msg
                            , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                            , "Cảm ơn bạn đã gởi thông tin."
                            , 1
                            , mCommon);

                    return;
                }

                long curTime = System.currentTimeMillis();
                if (curTime < promoRec.DATE_FROM || curTime > promoRec.DATE_TO) {
                    log.add("********", "vcb het thoi gian khuyen mai");

                    //send result to client
                    Misc.sendStandardReply(sock
                            , msg
                            , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                            , "Cảm ơn bạn đã gởi thông tin."
                            , 1
                            , mCommon);

                    return;
                }

                //todo check phone A
                phonesDb.getPhoneObjInfo(phoneNumberA, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(final PhonesDb.Obj phoneObjA) {
                        if (phoneObjA == null) {
                            log.add("********", "vcb khong tim thay nguoi gioi thieu tren he thong");

                            noti.caption = VcbNoti.InputA.Cap.ANotHasWallet;
                            noti.body = String.format(VcbNoti.InputA.Body.ANotHasWallet, phoneNumberA);
                            noti.htmlBody = String.format(VcbNoti.InputA.Body.ANotHasWallet, phoneNumberA);
                            noti.status = Notification.STATUS_DETAIL;
                            noti.type = NotificationType.NOTI_DETAIL_VALUE;
                            Misc.sendNoti(vertx, noti);

                            //send result to client
                            Misc.sendStandardReply(sock
                                    , msg
                                    , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                    , "Ví 0" + phoneNumberA + " không tồn tại trên hệ thống. Vui lòng kiểm tra lại"
                                    , 1
                                    , mCommon);
                            return;
                        }

                        if (!"12345".equalsIgnoreCase(phoneObjA.bank_code)) {
                            log.add("********", "vcb nguoi gioi thieu khong map vi voi VCB");

                            noti.caption = VcbNoti.InputA.Cap.ANotMapWallet;
                            noti.body = String.format(VcbNoti.InputA.Body.ANotMapWallet, "0" + phoneObjA.number, "0" + phoneObjA.number);
                            noti.htmlBody = String.format(VcbNoti.InputA.Body.ANotMapWallet, "0" + phoneObjA.number, "0" + phoneObjA.number);
                            noti.status = Notification.STATUS_DETAIL;
                            noti.type = NotificationType.NOTI_DETAIL_VALUE;
                            Misc.sendNoti(vertx, noti);

                            //send result to client
                            Misc.sendStandardReply(sock
                                    , msg
                                    , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                    , "Ví 0" + phoneNumberA + " chưa liên kết với ngân hàng Vietcombank"
                                    , 1
                                    , mCommon);
                            return;
                        }

                        if (DataUtil.strToInt(phoneObjA.inviter) == msg.cmdPhone) {

                            //send result to client
                            Misc.sendStandardReply(sock
                                    , msg
                                    , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                    , "Bạn không được nhập số người giới thiệu đã giới thiệu bạn"
                                    , 1
                                    , mCommon);

                            log.add("********", "vcb nhap so nguoi gioi thieu da gioi thieu minh phone A: " + phoneNumberA + " phoneB: " + phoneNumberB);
                            return;
                        }

                        if (phoneNumberA == msg.cmdPhone) {

                            //send result to client
                            Misc.sendStandardReply(sock
                                    , msg
                                    , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                    , "Bạn không được tự giới thiệu cho chính mình"
                                    , 1
                                    , mCommon);

                            log.add("********", "vcb tu gioi thieu cho chinh minh phoneA :" + phoneNumberA + " phoneB: " + phoneNumberB);
                            return;

                        }

                        if (!"".equalsIgnoreCase(phoneObj.inviter)) {

                            //send result to client
                            Misc.sendStandardReply(sock
                                    , msg
                                    , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                    , "Bạn đã đăng ký người giới thiệu thành công trước đó"
                                    , 1
                                    , mCommon);

                            noti.caption = VcbNoti.InputA.Cap.AIsPartnerOfB;
                            noti.body = VcbNoti.InputA.Body.AIsPartnerOfB;
                            noti.htmlBody = VcbNoti.InputA.Body.AIsPartnerOfB;
                            noti.status = Notification.STATUS_DETAIL;
                            noti.type = NotificationType.NOTI_DETAIL_VALUE;
                            noti.btnTitle = "Map ví VCB";
                            Misc.sendNoti(vertx, noti);

                        } else {
                            //cap nhat lai DB so nguoi gioi thieu
                            final JsonObject upDate = new JsonObject();

                            final long inviteTime = System.currentTimeMillis();
                            upDate.putString(colName.PhoneDBCols.INVITER, "0" + phoneNumberA);
                            upDate.putNumber(colName.PhoneDBCols.INVITE_TIME, System.currentTimeMillis());

                            phonesDb.updatePartialNoReturnObj(msg.cmdPhone, upDate, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {

                                    phoneObj.inviter = "0" + phoneNumberA;
                                    log.add("vcb updated json", upDate.encodePrettily());

                                    //update cache
                                    phoneObj.inviteTime = inviteTime;
                                    phoneObj.inviter = "0" + phoneNumberA;

                                    noti.caption = VcbNoti.InputA.Cap.AValid;
                                    noti.body = VcbNoti.InputA.Body.AValid;
                                    noti.htmlBody = VcbNoti.InputA.Body.AValid;
                                    noti.status = Notification.STATUS_DETAIL;
                                    noti.type = NotificationType.NOTI_MAP_WALLET_VALUE;
                                    noti.btnTitle = "Map ví VCB";
                                    Misc.sendNoti(vertx, noti);

                                    Misc.modifyAgent(vertx, phoneObj, logger, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {
                                            logger.info("A");
                                        }
                                    });

                                    log.add("*******", "vcb begin VcbCommon.requestGiftForB");

                                    //B chua map vi vcb
                                    if (!"12345".equalsIgnoreCase(phoneObj.bank_code)) {

                                        //send result to client
                                        Misc.sendStandardReply(sock
                                                , msg
                                                , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                                , "Ví 0" + msg.cmdPhone + " chưa liên kết với Vietcombank"
                                                , 1
                                                , mCommon);

                                        noti.caption = VcbNoti.InputA.Cap.BNotMap;
                                        noti.body = VcbNoti.InputA.Body.BNotMap;
                                        noti.htmlBody = VcbNoti.InputA.Body.BNotMap;
                                        noti.status = Notification.STATUS_DETAIL;
                                        noti.type = MomoProto.NotificationType.NOTI_MAP_WALLET_VALUE;
                                        noti.btnTitle = "Map vi VCB";
                                        Misc.sendNoti(vertx, noti);
                                        log.add("*********", "vcb " + phoneNumberB + " chua map vi");
                                        log.writeLog();
                                        return;
                                    }

                                    //kiem tra giao dich bank-in
                                    transDb.findOneVcbTran(phoneNumberB, phoneObj.bank_code
                                            , promoRec.DATE_FROM
                                            , promoRec.DATE_TO, new Handler<TranObj>() {
                                        @Override
                                        public void handle(TranObj vcbTranObjB) {

                                            //chua co giao dich nap tien tu VCB hoac giao dich that bai
                                            if (vcbTranObjB == null || vcbTranObjB.error > 0) {

                                                //send result to client
                                                Misc.sendStandardReply(sock
                                                        , msg
                                                        , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                                        , "Ví 0" + msg.cmdPhone + " chưa nạp tiền thành công từ Vietcombank"
                                                        , 1
                                                        , mCommon);

                                                noti.receiverNumber = phoneNumberB;
                                                noti.caption = VcbNoti.InputA.Cap.BNotTranVcb;
                                                noti.body = VcbNoti.InputA.Body.BNotTranVcb;
                                                noti.htmlBody = VcbNoti.InputA.Body.BNotTranVcb;
                                                noti.status = Notification.STATUS_DETAIL;
                                                noti.type = MomoProto.NotificationType.NOTI_CASH_MONEY_VALUE;
                                                noti.btnTitle = "Nạp tiền từ VCB";
                                                Misc.sendNoti(vertx, noti);

                                                log.add("*********", "vcb 0" + phoneNumberB + " chua co giao dich bank-in tu VCB");
                                                log.writeLog();
                                                return;
                                            }

                                            //send result to client
                                            Misc.sendStandardReply(sock
                                                    , msg
                                                    , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                                                    , "Bạn đã nhập mã người giới thiệu thành công"
                                                    , 0
                                                    , mCommon);

                                            //tao gift cho B
                                            log.add("*********", "vcb request khuyen mai cho 0" + phoneNumberB);
                                            log.writeLog();
                                            VcbCommon.requestGiftForB(vertx
                                                    , msg.cmdPhone
                                                    , "0" + phoneNumberA
                                                    , 0
                                                    , 0
                                                    , 0);

                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void doMomoPromo(final NetSocket sock
            , final MomoMessage msg
            , final StandardReply.Builder builder, final PhonesDb.Obj phoneObj) {

        JsonObject joUp = new JsonObject();
        joUp.putString(colName.PhoneDBCols.INVITER, "momo");

        final long inviteTime = System.currentTimeMillis();

        joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, inviteTime);

        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, joUp, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                phoneObj.inviter = "momo";

                Misc.modifyAgent(vertx, phoneObj, logger, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        logger.debug("A");
                    }
                });

                phoneObj.inviteTime = inviteTime;
                phoneObj.inviter = "momo";

                VcbCommon.requestGiftMomoForB(vertx
                        , msg.cmdPhone
                        , "momo"
                        , 0
                        , 0
                        , 0, phoneObj.bankPersonalId
                        , phoneObj.bank_code
                        , false);

                //send result to socket
                Misc.sendStandardReply(sock
                        , msg
                        , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                        , "Bạn đã nhập mã MOMO thành công"
                        , 0
                        , mCommon);
            }
        });
    }

    private void doAmwayPromo(final NetSocket sock
            , final MomoMessage msg
            , final int amwayReferal
            , final StandardReply.Builder builder, final PhonesDb.Obj phoneObj) {

        JsonObject joUp = new JsonObject();
        joUp.putString(colName.PhoneDBCols.INVITER, amwayReferal + "");

        final long inviteTime = System.currentTimeMillis();

        joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, inviteTime);

        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, joUp, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                phoneObj.inviter = amwayReferal + "";

                Misc.modifyAgent(vertx, phoneObj, logger, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        logger.debug("A");
                    }
                });

                phoneObj.inviteTime = inviteTime;
                phoneObj.inviter = amwayReferal + "";

                VcbCommon.requestGiftMomoForAmwayPromo(vertx
                        , msg.cmdPhone
                        , amwayReferal + ""
                        , 0
                        , 0
                        , 0, phoneObj.bankPersonalId
                        , phoneObj.bank_code
                        , false);

                //send result to socket
                Misc.sendStandardReply(sock
                        , msg
                        , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                        , "Bạn đã nhập mã giới thiệu thành công"
                        , 0
                        , mCommon);
            }
        });
    }

    private void doVnptHnPromo(final NetSocket sock
            , final MomoMessage msg
            , final String referal
            , final StandardReply.Builder builder, final PhonesDb.Obj phoneObj) {

        JsonObject joUp = new JsonObject();
        joUp.putString(colName.PhoneDBCols.INVITER, referal.toLowerCase().trim());

        final long inviteTime = System.currentTimeMillis();

        joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, inviteTime);

        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, joUp, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                phoneObj.inviter = referal.toLowerCase().trim();
                phoneObj.inviteTime = inviteTime;

                Misc.modifyAgent(vertx, phoneObj, logger, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        logger.debug("A");
                    }
                });

                VcbCommon.requestGiftMomoViettinbank(vertx
                        , msg.cmdPhone
                        , "vtbmomo"
                        , 0
                        , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
                        , 0
                        , phoneObj.bankPersonalId
                        , phoneObj.bank_code
                        , false
                        , 0
                        , "vnpthn"
                        , "vtbpromo"
                        , false
                        , ReqObj.online, "vnpthn", new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                    }
                });

                Misc.sendStandardReply(sock
                        , msg
                        , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                        , "Bạn đã nhập mã giới thiệu thành công"
                        , 0
                        , mCommon);
            }
        });
    }

    private void doPGGalaxyPromo(final NetSocket sock
            , final MomoMessage msg
            , final int pgCode
            , final StandardReply.Builder builder, final PhonesDb.Obj phonObj) {

        JsonObject joUp = new JsonObject();
        final long inviteTime = System.currentTimeMillis();
        joUp.putString(colName.PhoneDBCols.INVITER, String.valueOf(pgCode));
        joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, inviteTime);

        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, joUp, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                phonObj.inviter = String.valueOf(pgCode);

                Misc.modifyAgent(vertx, phonObj, logger, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {

                    }
                });

                //update cache
                phonObj.inviteTime = inviteTime;
                phonObj.inviter = String.valueOf(pgCode);

                //send result to socket
                Misc.sendStandardReply(sock
                        , msg
                        , MomoProto.MsgType.ADD_FIELD_REPLY_VALUE
                        , "Bạn đã nhập số người giới thiệu " + pgCode + " thành công"
                        , 0
                        , mCommon);

                glxDb.findOne(msg.cmdPhone, new Handler<Promo123PhimGlxDb.Obj>() {
                    @Override
                    public void handle(Promo123PhimGlxDb.Obj obj) {
                        if (obj != null && !"".equalsIgnoreCase(obj.ID)) {
                            VcbCommon.requestPromoForPG(vertx
                                    , msg.cmdPhone
                                    , pgCode
                                    , obj.AMOUNT
                                    , TranHisV1.TranType.PHIM123_VALUE
                                    , 0
                                    , 5000, "123phim");
                        }
                    }
                });

            }
        });
    }

    public void getFormFields(final NetSocket sock, final MomoMessage msg, final Handler<JsonObject> callback) {

        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        if (fields == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);

        final FormField.Builder builder = FormField.newBuilder();
        final HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());

        final String serviceId = hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : "";
        final int nextForm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;
        log.add("serviceid", serviceId);
        log.add("nextform", nextForm);

        if ("".equalsIgnoreCase(serviceId)) {

            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.GET_FORM_FIELDS_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build()
                            .toByteArray()
            );

            mCommon.writeDataToSocket(sock, buf);

            log.add("desc", "Khong lay duoc service id tu client");
            log.writeLog();
            return;
        }
//        else if("proxy_confirm".equalsIgnoreCase(serviceId))
//        {
//            Buffer buf = MomoMessage.buildBuffer(
//                    MomoProto.MsgType.GET_FORM_FIELDS_REPLY_VALUE,
//                    msg.cmdIndex,
//                    msg.cmdPhone,
//                    builder.build()
//                            .toByteArray()
//            );
//
//            mCommon.writeDataToSocket(sock, buf);
//
//            log.add("desc", "Khong lay duoc service id tu client");
//            log.writeLog();
//            return;
//        }

        //cau hinh formdata o local
        Misc.requestFormFields(vertx, nextForm, serviceId, Command.get_form_fields, isStoreApp, "", msg.cmdPhone, new Handler<ReplyObj>() {
            @Override
            public void handle(ReplyObj replyObj) {

                ArrayList<com.mservice.momo.vertx.form.FieldItem> items = replyObj.items;

                //build fields
                for (int i = 0; i < items.size(); i++) {

                    builder.addListFieldItem(MomoProto.FieldItem.newBuilder()
                                    .setFieldLabel(items.get(i).fieldlabel)
                                    .setFieldType(items.get(i).fieldtype)
                                    .setKey(items.get(i).key)
                                    .setRequire(items.get(i).required ? 1 : 0)
                                    .setHasChild(items.get(i).haschild)
                                    .setLine(items.get(i).line)
                                    .setIsAmount(items.get(i).isamount)
                                    .setReadonly(items.get(i).readonly)
                                    .setValue(items.get(i).value)
                    );

                    log.add("item " + (i + 1), "");
                    log.add("fieldLabel", items.get(i).fieldlabel);
                    log.add("fieldType", items.get(i).fieldtype);
                    log.add("Key", items.get(i).key);
                    log.add("Required", items.get(i).required);
                    log.add("has Child", items.get(i).haschild);
                    log.add("Line", items.get(i).line);
                    log.add("isamount", items.get(i).isamount);
                    log.add("readonly", items.get(i).readonly);
                    log.add("value", items.get(i).value);
                }

                //build first data
                HashMap<String, ArrayList<FieldData>> hmFDs = Misc.getHasMapFieldData(replyObj.datas);

                if (hmFDs != null && hmFDs.size() > 0) {
                    for (String s : hmFDs.keySet()) {
                        MomoProto.ValueForDropBox.Builder vfd = MomoProto.ValueForDropBox.newBuilder();
                        vfd.setLinkToDropKey(s);

                        ArrayList<FieldData> pkgLstItem = hmFDs.get(s);

                        for (int i = 0; i < pkgLstItem.size(); i++) {

                            FieldData pkgItem = pkgLstItem.get(i);

                            vfd.addListValue(MomoProto.TextValue.newBuilder()
                                            .setText(pkgItem.text)
                                            .setValue(pkgItem.value)
                                            .setId(pkgItem.id)
                            );
                        }

                        builder.addListValue(vfd);
                    }
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.GET_FORM_FIELDS_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build()
                                .toByteArray()
                );

                mCommon.writeDataToSocket(sock, buf);
                log.writeLog();
            }
        });

    }

    public void processSubmitForm(final NetSocket sock, final MomoMessage msg, final SockData data, final Handler<JsonObject> callback) {
        /*
        SUBMIT_FORM = 1189;     //client gui toan bo thong tin collect tu user len server
                                //body Fields
        SUBMIT_FORM_REPLY = 1190;   //tra ket qua ve client
                                    //body FromReply*/

        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        if (fields == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);

        final FormReply.Builder builder = FormReply.newBuilder();

        final HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());
        for (String s : hashMap.keySet()) {
            log.add(s, hashMap.get(s));
        }

        String serviceId = hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : "";
        //email
        String email = hashMap.containsKey(Const.AppClient.Email) ? hashMap.get(Const.AppClient.Email) : "";
        log.add("serviceid", serviceId);
        log.add("email", email);
        String billId = hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "";
        log.add("billId", billId);
        String quantity = hashMap.containsKey(Const.AppClient.Quantity) ? hashMap.get(Const.AppClient.Quantity) : "";
        log.add("quantity", quantity);
        String amount = hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "";
        log.add("amount", amount);
        if ("".equalsIgnoreCase(serviceId)) {
            log.add("desc", "Khong lay duoc service id tu client");
//            log.writeLog();
            Buffer buf = MomoMessage.buildBuffer(
                    MsgType.SUBMIT_FORM_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build()
                            .toByteArray()
            );

            mCommon.writeDataToSocket(sock, buf);
            log.writeLog();
            return;
        }
        final int pay_times = jsonBillRuleManagement.getInteger(StringConstUtil.BillRuleManagement.PAY_TIMES, 2);
        billRuleManageDb.findOne(billId, new Handler<BillRuleManageDb.Obj>() {
            @Override
            public void handle(final BillRuleManageDb.Obj obj) {
                long currentTime = System.currentTimeMillis();

                if (obj != null && currentTime < obj.endTime && obj.count == pay_times) {
                    // Khong cho thanh toan hoa don, tra ve thong bao
                    TextValue textValue = TextValue.getDefaultInstance();
                    builder.addListInfo(0, textValue.toBuilder().setText("err").setValue(ErrorConstString.HC_ONE_DAY_ONE_BILL));
                    Buffer buf = MomoMessage.buildBuffer(
                            MsgType.SUBMIT_FORM_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.build()
                                    .toByteArray()
                    );

                    mCommon.writeDataToSocket(sock, buf);
                    log.writeLog();
                    return;
                }

                Misc.requestSubmitForm(vertx, hashMap, msg.cmdPhone, isStoreApp, new Handler<ArrayList<BillInfoService.TextValue>>() {
                    @Override
                    public void handle(ArrayList<BillInfoService.TextValue> textValues) {
                        HashMap<String, String> hmResult = Misc.convertArrayTextValueToHashMap(textValues);
                        int nextForm = hmResult.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hmResult.get(Const.AppClient.NextForm)) : 0;
                        builder.setNext(nextForm);
                        for (int i = 0; i < textValues.size(); i++) {
                            BillInfoService.TextValue tv = textValues.get(i);
                            builder.addListInfo(TextValue.newBuilder().setValue(tv.value).setText(tv.text));
                        }

                        Buffer buf = MomoMessage.buildBuffer(
                                MsgType.SUBMIT_FORM_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.build()
                                        .toByteArray()
                        );

                        mCommon.writeDataToSocket(sock, buf);
                        log.writeLog();
                    }
                });
            }
        });

    }

//        private void executeProcess(int transType, NetSocket sock, MomoMessage msg, SockData data,
//                                JsonObject jsonContainerObject) {
//
//
//        switch (transType) {
//            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
//                transProcess.processTopUp(sock, msg, data, null);
//                break;
//            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
//                String invoice_no = jsonContainerObject.getString(VMConst.invoice_no);
//                String ticket_code = jsonContainerObject.getString(VMConst.ticket_code);
//                transProcess.processPayment123Phim(sock
//                        , msg
//                        , data
//                        , invoice_no
//                        , ticket_code
//                        , null);
//                break;
//            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
//                transProcess.processPayOneBill(sock, msg, data, null);
//                break;
//            default:
//                break;
//        }
//    }

    public void getValueForDropByParentId(final NetSocket sock
            , final MomoMessage msg
            , final Handler<JsonObject> callback) {

        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        if (fields == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "getValueForDropByParentId");

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());
        String parentId = hashMap.containsKey(Const.AppClient.Id) ? hashMap.get(Const.AppClient.Id) : "";
        String serviceId = hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : "";
        int nextForm = hashMap.containsKey(Const.AppClient.NextForm) ? DataUtil.strToInt(hashMap.get(Const.AppClient.NextForm)) : 0;

        log.add("parentId", parentId);
        log.add("serviceid", serviceId);
        log.add("nextForm", nextForm);

        Misc.requestFormFields(vertx, nextForm, serviceId, Command.get_data_for_dropbox, isStoreApp, parentId, msg.cmdPhone, new Handler<ReplyObj>() {
            @Override
            public void handle(ReplyObj replyObj) {
                HashMap<String, ArrayList<FieldData>> hashMapPkg = Misc.getHasMapFieldData(replyObj.datas);

                ValueForDropBox.Builder vfd = ValueForDropBox.newBuilder();
                if (hashMapPkg != null && hashMapPkg.size() > 0) {
                    for (String s : hashMapPkg.keySet()) {

                        vfd.setLinkToDropKey(s);

                        ArrayList<FieldData> pkgLstItem = hashMapPkg.get(s);

                        for (int i = 0; i < pkgLstItem.size(); i++) {

                            FieldData pkgItem = pkgLstItem.get(i);

                            vfd.addListValue(TextValue.newBuilder()
                                            .setText(pkgItem.text)
                                            .setValue(pkgItem.value)
                                            .setId(pkgItem.id)
                            );

                            log.add("item", "----" + (i + 1) + "----");
                            log.add("pkname", pkgItem.text);
                            log.add("pkvalue", pkgItem.value);
                            log.add("id", pkgItem.id);
                        }
                    }
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.GET_VALUE_FOR_DROPBOX_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        vfd.build()
                                .toByteArray()
                );

                mCommon.writeDataToSocket(sock, buf);
                log.writeLog();

            }
        });
    }

    public void getCategory(final NetSocket sock
            , final MomoMessage msg
            , final Handler<JsonObject> callback) {

        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        if (fields == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());
        long lastTime = hashMap.containsKey("lasttime") ? DataUtil.stringToUNumber(hashMap.get("lasttime")) : 0;

        com.mservice.momo.vertx.processor.Common.ServiceReq serviceReq = new com.mservice.momo.vertx.processor.Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_CATEGORY_BY_LAST_TIME;
        serviceReq.lastTime = lastTime;

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                ArrayList<ServiceCategory.Obj> arrayList = Misc.getCategoryList(jsonArray);

                ServiceReply.Builder builder = ServiceReply.newBuilder();

                if (arrayList != null && arrayList.size() > 0) {
                    for (int i = 0; i < arrayList.size(); i++) {

                        ServiceCategory.Obj o = arrayList.get(i);

                        /*
                            category_id --> category_id;
                            category_name -->category_name;
                            desc ="desc" -->partner_site;
                            status ="stat" -->status;
                            lasttime ="ltime" -->last_update;
                            order ="ord"-->next_form;
                            iconurl ="iurl" --> icon_url;
                            star ="star" -->star
                        */

                        builder.addServiceList(ServiceItem.newBuilder()
                                        .setStatus("on".equalsIgnoreCase(o.status) ? 1 : 0)
                                        .setStar(o.star)
                                        .setCategoryId(o.id)
                                        .setCategoryName(o.name)
                                        .setLastUpdate(o.lasttime)
                                        .setPartnerSite(o.desc)
                                        .setTotalForm(o.order)
                                        .setIconUrl(o.iconurl)
                        );

                    }
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.GET_CATEGORY_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build()
                                .toByteArray()
                );

                mCommon.writeDataToSocket(sock, buf);

            }
        });
    }

    public void mustUpdateAgentInfo(final NetSocket sock, final MomoMessage msg, final SockData data, Handler<JsonObject> callback) {
        phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {

            @Override
            public void handle(PhonesDb.Obj obj) {
                boolean result = false;
                if (obj != null) {
                    if (obj.name == null)
                        result = true;
                    else if (obj.name.isEmpty())
                        result = true;
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.MUST_UPDATE_INFO_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        MomoProto.StandardReply.newBuilder()
                                .setRcode(0)
                                .setResult(result)
                                .build().toByteArray()
                );
                mCommon.writeDataToSocket(sock, buf);
            }
        });
    }

    public void getRetailerFee(final NetSocket sock
            , final MomoMessage msg
            , final Handler<JsonObject> callback) {

        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        if (fields == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "getRetailerFee");

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());

        //default M2M
        int tranType = hashMap.containsKey("trantype") ? DataUtil.strToInt(hashMap.get("trantype")) : 6;
        long amount = hashMap.containsKey(Const.AppClient.Amount) ? DataUtil.stringToUNumber(hashMap.get(Const.AppClient.Amount)) : 0;

        final TextValueMsg.Builder builder = TextValueMsg.newBuilder();

        Misc.getRetailerFee(vertx, tranType, amount, msg.cmdPhone, new Handler<FeeObj>() {
            @Override
            public void handle(FeeObj feeObj) {
                builder.addKeys(TextValue.newBuilder().setText("fee").setValue(String.valueOf(feeObj.fee)));
                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.DGD_GET_FEE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build()
                                .toByteArray()
                );

                mCommon.writeDataToSocket(sock, buf);
                log.writeLog();
            }
        });
    }

    /**
     * @param sock : the current socket
     * @param msg  the message client sent to server
     * @param data the cache data of this socket
     */
    public void getRetailerLiquidity(final NetSocket sock, final MomoMessage msg, final SockData data) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        String agentNumber = "0" + msg.cmdPhone;
        //gui lat thong tin

        JsonObject joReq = new JsonObject();
        joReq.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.GET_LIQUIDITY);
        joReq.putString("retailer", agentNumber);

        vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS
                , joReq
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                JsonObject joRes = result.body();
                String caption = joRes.getString("caption", "");
                String content = joRes.getString("content", "");

                TextValue.Builder tvCap = TextValue.newBuilder().setText("caption").setValue(caption);
                TextValue.Builder tvContent = TextValue.newBuilder().setText("content").setValue(content);

                Buffer buffer = MomoMessage.buildBuffer(MsgType.GET_LIQUIDITY_REPLY_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , TextValueMsg.newBuilder()
                        .addKeys(tvCap)
                        .addKeys(tvContent)
                        .build().toByteArray());
                mCommon.writeDataToSocket(sock, buffer);
            }
        });
    }

    /**
     * This method used to get Visa Master Card List.
     *
     * @param sock
     * @param msg
     * @param data
     */
    public void getVisaMasterCardList(final NetSocket sock, final MomoMessage msg, SockData data) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        String channel = TranHisUtils.getChannel(sock);
        VMRequest.getCardList(vertx, sbsBussAddress, "0" + msg.cmdPhone, channel, log, new Handler<List<CardInfo>>() {
            @Override
            public void handle(List<CardInfo> cardInfos) {
                CardAddOrUpdate.Builder builder = CardAddOrUpdate.newBuilder();
                Buffer buffer;
                JsonArray jsonArray = new JsonArray();
                if (cardInfos != null && cardInfos.size() > 0) {
                    for (CardInfo cif : cardInfos) {

                        log.add("item: ",
                                "Expired: " + cif.getExpiredDate().toString() +
                                        "Card Number: " + cif.getCardNumber().toString() +
                                        "Card Holder: " + cif.getCardHolder().toString() +
                                        "Card Id: " + cif.getCardId().toString() +
                                        "Card Type " + VMCardType.getCodeCardType(cif.getType()).toString()
                        );

                        builder.addCardList(CardItem.newBuilder()
                                        .setCardHolderYear(cif.getExpiredDate())
                                        .setCardHolderNumber(cif.getCardNumber())
                                        .setCardHolderName(cif.getCardHolder())
                                        .setCardId(cif.getCardId())
                                        .setCardType(VMCardType.getCodeCardType(cif.getType()))
                        );

                        JsonObject joCardInfo = buildCardInfo(cif);
                        jsonArray.add(joCardInfo);
                    }

                    buffer = MomoMessage.buildBuffer(MsgType.GET_CARD_LIST_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone, builder.build().toByteArray());
                    mCommon.writeDataToSocket(sock, buffer);
                    log.writeLog();
                    log.add("cardInfo", jsonArray);
                    for (Object o : jsonArray) {
                        card.upsertVisaMaster(msg.cmdPhone, (JsonObject) o, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                log.add("upsertVisaMaster ----------------------------", aBoolean);
                                log.writeLog();
                            }
                        });
                    }
                }
            }
        });
    }

    private JsonObject buildCardInfo(CardInfo ci) {

        JsonObject o = new JsonObject();
        o.putString(colName.CardDBCols.CARD_HOLDER_NAME
                , ci.getCardHolder() == null ? "" : ci.getCardHolder());
        o.putString(colName.CardDBCols.CARD_HOLDER_NUMBER
                , ci.getCardNumber() == null ? "" : ci.getCardNumber());

        o.putString(colName.CardDBCols.CARD_HOLDER_YEAR
                , ci.getExpiredDate() == null ? "" : ci.getExpiredDate());

        o.putString(colName.CardDBCols.CARD_HOLDER_MONTH, "");
        o.putString(colName.CardDBCols.BANK_NAME, VMCardType.convertBankNameFromCardTypeId(VMCardType.getCodeCardType(ci.getType())));
        o.putString(colName.CardDBCols.BANKID, "sbs");
        o.putBoolean(colName.CardDBCols.DELETED, false);
        o.putNumber(colName.CardDBCols.LAST_SYNC_TIME, System.currentTimeMillis());
        o.putString(colName.CardDBCols.CARD_TYPE, VMCardType.getCodeCardType(ci.getType()));
        o.putString(colName.CardDBCols.CARD_ID, ci.getCardId() == null ? "" : ci.getCardId());
        o.putNumber(colName.CardDBCols.ROW_ID, System.currentTimeMillis());
        o.putString(colName.CardDBCols.CARD_CHECKSUM, ci.getHashString());
        return o;
    }

    public void deleteVMCardToken(final NetSocket sock, final MomoMessage msg, SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("Func:", "deleteVMCardToken");
        String channel = TranHisUtils.getChannel(sock);

        MomoProto.TextValueMsg request;
        try {
            request = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(request.getKeysList());

        //not used at this time yet, maybe will use later
        String partnerCode = hashMap.containsKey("partner") ? hashMap.get("partner") : "";

        final String cardId = hashMap.containsKey("cardid") ? hashMap.get("cardid") : "";

        VMRequest.deleteVMCard(vertx
                , sbsBussAddress
                , "0" + msg.cmdPhone
                , cardId
                , channel
                , log
                , new Handler<VisaResponse>() {
            @Override
            public void handle(VisaResponse visaResponse) {

                TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                Buffer deleteTokenBuffer;
                if (visaResponse != null) {
                    log.add("Id", visaResponse.getResultCode());
                    log.add("Desc", visaResponse.getDescription());
                    textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setId(visaResponse.getResultCode() + ""));
                }

                //return StandardReply
                int error = visaResponse == null ? SoapError.SYSTEM_ERROR : visaResponse.getResultCode();
                String desc = (error == 0 ? "Hủy liên kết thẻ visa master thành công" : "Hủy liên kết thẻ visa master không thành công");
                Misc.sendStandardReply(sock, msg, MsgType.DELETE_TOKEN_REPLY_VALUE, desc, error, mCommon);


//                // xoa trong DB
                log.add("error", error);
                if (error == 0) {
                    card.deleteVMCard(msg.cmdPhone, visaResponse, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            if (jsonObject != null) {
                                log.add("deleteDB", "success");
                            } else {
                                log.add("deleteDB", "fail");
                            }
                        }
                    });

                }
                log.writeLog();

            }
        });
    }

    public void createVMCardToken(final NetSocket sock, final MomoMessage msg, final SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("Func: ", "createVMCardToken");
        final String channel = TranHisUtils.getChannel(sock);
        /*: TextValueMsg
        //key		value
        “partner”	“sbs”
        “username”	“ten tren the visa-master”
        “cardtype”	“loai card dang request token”

        "cardnumber"    “số card”
        "cardexpired"  "ngày hết hạn"
        "cvvnumber"   "số CVV của thẻ"
        "cardholdername" "tên chủ thẻ"

        */
        MomoProto.TextValueMsg request;
        try {
            request = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }
        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(request.getKeysList());
        JsonObject jsonCardInfo = getVmCardInfo(hashMap);
        String requestType = jsonCardInfo.getString(VMConst.requestType, "");
        final String cardId = jsonCardInfo.getString(VMConst.cardId, "");
        final String amountString = jsonCardInfo.getString(VMConst.amount, "0");
        final String tranTypeString = jsonCardInfo.getString(VMConst.tranType, "");
        final String feeTypeString = jsonCardInfo.getString(VMConst.feeType, "");
        final String cardTypeString = jsonCardInfo.getString(VMConst.cardType, "");
        final String numberReceiver = jsonCardInfo.getString(VMConst.numberreceiver, "");
        final String cardLbl = jsonCardInfo.getString(VMConst.cardLabel, "");
        final String providerName = jsonCardInfo.getString(VMConst.providername, "");
        final String serviceId = jsonCardInfo.getString(VMConst.partnerCode, "");
        final String billId = jsonCardInfo.getString(VMConst.billid, "");
        final String billIdIOS = jsonCardInfo.getString("app", "");
        final long amount = DataUtil.strToLong(amountString);
        final int tranType = DataUtil.strToInt(tranTypeString);
        final int feeType = DataUtil.strToInt(feeTypeString);
        final long ackTime = System.currentTimeMillis();
        final boolean check_visa_named = sbsCfg.getBoolean("check_named", false);
        //kiem tra giao dich nap tien truc tiep
        //gia tri giao dich toi thieu phai la minValue (50K) thi moi cho nap
        log.add("cardId--->", cardId);
        log.add("amountString--->", amountString);
        log.add("tranTypeString--->", tranTypeString);
        log.add("feeTypeString--->", feeTypeString);
        log.add("cardTypeString--->", cardTypeString);
        log.add("numberReceiver--->", numberReceiver);
        log.add("cardLbl--->", cardLbl);
        log.add("providerName--->", providerName);
        log.add("serviceId--->", serviceId);
        log.add("billId--->", billId);

        final String emailDesc = "Nạp tiền vào ví điện tử";
        if ("cashin".equalsIgnoreCase(requestType)) {

            log.add("func---->", "cashin");
            long minValue = Math.max(sbsGetMinValue(feeType), 0);
//            if(feeType == VMFeeType.DEPOSIT_FEE_TYPE && minValue > amount){
            if (minValue > amount) {
                log.add("Error: ---> ", minValue + ">" + amount);
                long tranId = System.currentTimeMillis();
                JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, tranId, amount, -1);
                TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                String errDesc = "Giá trị giao dịch tối thiểu phải là: " + Misc.formatAmount(minValue).replace(",", ".") + "đ";
                Misc.addErrDescAndComment(tranErr, errDesc, "Giao dịch không thành công. " + errDesc);
//            mCommon.sendTransReply(vertx,tranErr,ackTime,msg,sock, data,null);
                textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc")
                        .setValue(errDesc));
                Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.CREATE_TOKEN_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                        , textValueMsgBuilder.build().toByteArray());
                mCommon.writeDataToSocket(sock, createVMbuffer);
                log.writeLog();
                return;
            }
            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj senderPhoneObj) {
                    if(senderPhoneObj == null || (senderPhoneObj != null && !senderPhoneObj.isNamed && check_visa_named))
                    {
                        log.add("Error: ---> ", "Chua dinh danh vi");
                        log.add("Error: ---> ", "IS NOT NAMED");
                        long tranId = System.currentTimeMillis();
                        JsonObject tranErr = Misc.getJsonObjRpl(MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE, tranId, amount, -1);
                        TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                        String errDesc = "Tài khoản chưa định danh, vui lòng định danh để thực hiện giao dịch. Xin cám ơn.";
                        Misc.addErrDescAndComment(tranErr, errDesc, "Giao dịch không thành công. " + errDesc);
//            mCommon.sendTransReply(vertx,tranErr,ackTime,msg,sock, data,null);
                        textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc")
                                .setValue(errDesc));
                        Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.CREATE_TOKEN_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                                , textValueMsgBuilder.build().toByteArray());
                        mCommon.writeDataToSocket(sock, createVMbuffer);
                        log.writeLog();
                        return;
                    }
                    processCashInVisa(feeType, amount, log, tranType, numberReceiver, sock, data, msg, cardId, cardLbl, cardTypeString, channel, emailDesc, providerName, ackTime, serviceId, billId);
                }
            }); // END CHECK PHONE
        } else {
            log.add("method", "Tao the VisaMaster");
            VMRequest.createToken(vertx, sbsBussAddress, data, jsonCardInfo, "0" + msg.cmdPhone, log, channel, new Handler<VisaResponse>() {
                @Override
                public void handle(final VisaResponse visaResponse) {
                    TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                    Buffer createVMbuffer;

                    if (visaResponse == null) {
                        textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("error")
                                .setValue(1000 + "")).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue("Hệ thống hiện tại đang tạm lỗi, quý khách vui lòng thực hiện lại sau"));

                        log.add("phone--------------------->", "null");
                        log.add("error---------------------", "null");
                        log.add("desc", "null");
                    } else if (visaResponse != null && visaResponse.getResultCode() == 0) {
                        logger.info("visa Response " + visaResponse.getJsonObject());
                        String url = visaResponse.getUrl();
                        log.add("url", url);

                        JsonObject postInfo = visaResponse.getPostInfo();
                        postInfo.putString("url", url);
                        if (!"".equalsIgnoreCase(url)) {
                            textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("url")
                                    .setValue(url));
                        }

                        //Add the thanh cong. Luu lai bang map the
                        JsonObject joWalletUpdate = new JsonObject();
                        MappingWalletBankDb mappingWalletBankDb = new MappingWalletBankDb(vertx,logger);
                        String id = visaResponse.getCardChecksum() + "sbs_" + VMCardType.getCodeCardType(visaResponse.getVisaRequest().getCardType());
//                        joWalletUpdate.putString(colName.MappingWalletBank.ID, id);
                        joWalletUpdate.putString(colName.MappingWalletBank.NUMBER, "0" + msg.cmdPhone);
                        joWalletUpdate.putString(colName.MappingWalletBank.BANK_NAME, "sbs");
                        joWalletUpdate.putString(colName.MappingWalletBank.BANK_CODE, VMCardType.getCodeCardType(visaResponse.getVisaRequest().getCardType()));
                        joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_NAME, visaResponse.getVisaRequest().getCardHolder());
                        joWalletUpdate.putNumber(colName.MappingWalletBank.MAPPING_TIME, System.currentTimeMillis());
                        joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_ID, visaResponse.getCardChecksum());
                        joWalletUpdate.putNumber(colName.MappingWalletBank.NUMBER_OF_MAPPING, visaResponse.countRegister());
                        mappingWalletBankDb.upsertWalletBank(id, joWalletUpdate, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                logger.info("update mappingWalletBankDb for bankInfo.getPhoneNumber() is " + "0" + msg.cmdPhone);
                            }
                        });
                        //Lan dau map voi cai the nay
                        log.add("desc", "Kiem tra tra thuong visa lan dau tien " + msg.cmdPhone);
//                            promotionProcess.getFirstTimeBankMappingPromotion(data, visaResponse, null, log);
//                        promotionProcess.executeReferralPromotion(StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.FIRST_TIME_BANK_MAPPING, null, visaResponse, data, null, log, new JsonObject());
                        //BEGIN 0000000015 Visa master promo
//                        AtomicInteger atomicCheck = new AtomicInteger(0);
//                        checkVisaConnectorAgain(log, msg, atomicCheck, visaResponse.getCardChecksum(), data);


                    } else {
                        String description = visaResponse.getDescription() == null ? "" : visaResponse.getDescription();
                        logger.info("visa Response " + visaResponse.getJsonObject());
                        textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("error")
                                .setValue(visaResponse.getResultCode() + "")).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue(description));

                        log.add("phone--------------------->", visaResponse.getVisaRequest().getPhoneNumber());
                        log.add("error---------------------", visaResponse.getResultCode());
                        log.add("desc", "FAIL FROM CONNECTOR");
                    }

                    createVMbuffer = MomoMessage.buildBuffer(MsgType.CREATE_TOKEN_REPLY_VALUE
                            , msg.cmdIndex
                            , msg.cmdPhone
                            , textValueMsgBuilder.build().toByteArray());

                    log.writeLog();
                    mCommon.writeDataToSocket(sock, createVMbuffer);
                }
            });
        }
        mCommon.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());

    }

    public Card.Obj createATMCardInfo(CardResponse cardResponse, String bankCode, String bankName, Common.BuildLog log)
    {
        Card.Obj cardObj = new Card.Obj();
        cardObj.card_holder_number = cardResponse.getRequest().getCardInfo().getCardNumber();
        cardObj.bankid = bankCode;
        cardObj.cardType = cardResponse.getRequest().getPartnerCode();
        cardObj.card_holder_year = Calendar.getInstance().get(Calendar.YEAR) + "";
        cardObj.card_holder_month = Calendar.getInstance().get(Calendar.MONTH) + "";
        cardObj.bank_name = bankName;
        cardObj.deleted = false;
        cardObj.last_sync_time = System.currentTimeMillis();
        cardObj.cardId = cardResponse.getRequest().getCardInfo().getCardId();
        cardObj.row_id = UUID.randomUUID().hashCode();
        cardObj.cardCheckSum = cardResponse.getRequest().getCheckSum();
        cardObj.bank_type = 10;
        cardObj.status = 1;
        cardObj.card_holder_name = cardResponse.getRequest().getCardInfo().getHolderName();
//        cardObj.card_holder_name = cardResponse.getJsonObject().getObject("request", new JsonObject()).getObject("cardInfo", new JsonObject())
//                .getString("cardName", "ABC");
        log.add("cardResponse", cardResponse.getJsonObject());
        log.add("cardRequest", cardResponse.getJsonObject().getObject("request", new JsonObject()));
        log.add("cardInfo", cardResponse.getJsonObject().getObject("request", new JsonObject()).getObject("cardInfo", new JsonObject()));
        log.add("cardName", cardResponse.getJsonObject().getObject("request", new JsonObject()).getObject("cardInfo", new JsonObject()).getString("cardName", "ABC"));
        return cardObj;
    }
    public void mapWallet(final NetSocket sock, final MomoMessage msg, final SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("Func: ", "mapWallet");
        final String channel = TranHisUtils.getChannel(sock);
        /*: TextValueMsg
        //key		value
        “requestType”	“hanh dong: verify, confirm_map, unmap”
        “bankCode”	“ma ngan hang”
        “bankAccountNo”	“số card”
        "bankAccountName" “tên chủ thẻ”
        "personalId"  "CMND"
        "otp"   "otp"
        "cardCreateAt" "ngay mo the"
        */
        MomoProto.TextValueMsg request;
        try {
            request = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            request = null;
        }

        if (request == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }
        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(request.getKeysList());
        final JsonObject jsonCardInfo = getVmCardInfo(hashMap);

        final String requestType = jsonCardInfo.getString(WalletMappingConst.requestType, "");
        final String bankCode = jsonCardInfo.getString(WalletMappingConst.bankCode, "");
        final String bankAccountNo = jsonCardInfo.getString(WalletMappingConst.bankAccountNo, "");
        final String bankAccountName = jsonCardInfo.getString(WalletMappingConst.bankAccountName, "");
        final String personalId = jsonCardInfo.getString(WalletMappingConst.personalId, "");
        final String otp = jsonCardInfo.getString(WalletMappingConst.otp, "");
        final String cardCreateAt = jsonCardInfo.getString(WalletMappingConst.cardCreateAt, "0");
        final String cardId = jsonCardInfo.getString(WalletMappingConst.cardId, "");
        final String bankName = jsonCardInfo.getString(WalletMappingConst.bankName, "");
//        final String bankId = jsonCardInfo.getString(WalletMappingConst.bankId, "");
        log.add("requestType--->", requestType);
        log.add("bankCode--->", bankCode);
        log.add("bankAccountNo--->", bankAccountNo);
        log.add("bankAccountName--->", bankAccountName);
        log.add("personalId--->", personalId);
        log.add("otp--->", otp);
        log.add("cardCreateAt--->", cardCreateAt);
        log.add("cardId--->", cardId);

        Map<String, Object> kvps = new HashMap<>();
        kvps.put("client", "backend");
        kvps.put("chanel", "mobi");
        kvps.put("issms", "no");
        kvps.put("cardCreateAt", cardCreateAt);

        if (WalletMappingConst.ActionType.VERIRY.equalsIgnoreCase(requestType)) {

            log.add("func---->", "verify");
            int err = WalletMappingError.SUCCESS;
            if (StringUtils.isEmpty(bankCode)) {
                err = WalletMappingError.INVALID_BANK_CODE;
            }
            if (!bankAccountNo.matches("\\d{16,19}")) {
                err = WalletMappingError.INVALID_CARD_NUMBER;
            }
            String holderNameNormalize = Misc.removeAccent(bankAccountName);
            if (StringUtils.isEmpty(bankAccountName) || !bankAccountName.equalsIgnoreCase(holderNameNormalize)) {
                err = WalletMappingError.INVALID_CARD_HOLDER_NAME;
            }
            if (!personalId.matches("\\d{9}|\\d{12}")) {
                err = WalletMappingError.INVALID_PERSIONAL_ID;
            }
            long cardCreateAtTime = DataUtil.parseTime("MM/yyyy", cardCreateAt);
            if (cardCreateAtTime == -1 || cardCreateAtTime >= System.currentTimeMillis()) {
                err = WalletMappingError.INVALID_CARD_CREATE_TIME;
            }
            if (WalletMappingError.SUCCESS != err) {
                String desc = WalletMappingError.getDesc(err);
                log.add("errDesc", desc);
                TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("err")
                        .setValue("" + err)).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue(desc));
                Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.WALLET_MAP_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                        , textValueMsgBuilder.build().toByteArray());
                mCommon.writeDataToSocket(sock, createVMbuffer);
                log.writeLog();
                return;
            }
            // Send to connector
            log.add("mapWallet verify", "send request to connector");
            final CardRequest cardRequest = BankCardRequestFactory.enquiryMapRequest(bankCode.toLowerCase(), "0" + msg.cmdPhone, data.pin, "",  bankAccountNo,
                    personalId, bankAccountName, kvps, WalletType.MOMO);

            JsonObject joBankReq = new JsonObject();
            joBankReq.putNumber(BankHelperVerticle.COMMAND, BankHelperVerticle.ATM_IN_OUT);
            joBankReq.putString(BankHelperVerticle.PHONE, "0" + msg.cmdPhone);
            joBankReq.putObject(BankHelperVerticle.DATA, cardRequest.getJsonObject());
            vertx.eventBus().sendWithTimeout(AppConstant.BankHelperVerticle_ADDRESS, joBankReq, 80 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> bankAsyncResult) {
                    if (bankAsyncResult.succeeded() && bankAsyncResult.result() != null) {

                        JsonObject reponse = bankAsyncResult.result().body();
                        log.add("proxy_bank's response", reponse.toString());
                        if (reponse.getInteger(BankHelperVerticle.ERROR) != 0) {
                            log.add("errorCode", "time out");
                            createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, null, false, jsonCardInfo);
                            return;
                        }

                        CardResponse cardResponse = new CardResponse(reponse.getObject(BankHelperVerticle.DATA));
                        log.add("result", "Co ket qua tra ve tu connector");
                        log.add("cardResponse", cardResponse);

                        if (cardResponse != null && cardResponse.getResultCode() == 0) {
                            log.add("wallet mapping verify", "success");
                            log.add("transId", cardResponse.getRequest().getCoreTransId());
                            TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                            textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder()
                                    .setText("desc")
                                    .setValue(WalletMappingError.getDesc(WalletMappingError.SUCCESS)))
                            .addKeys(TextValue.getDefaultInstance().toBuilder()
                                    .setText("err")
                                    .setValue("" + WalletMappingError.SUCCESS));
                            Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.WALLET_MAP_REPLY_VALUE, msg.cmdIndex,
                                    msg.cmdPhone, textValueMsgBuilder.build().toByteArray());
                            mCommon.writeDataToSocket(sock, createVMbuffer);
                            log.writeLog();
                            return;
                        } else if (cardResponse != null && cardResponse.getResultCode() != 0) {
                            log.add("connector return", "error");
                            log.add("transId", cardResponse.getRequest().getCoreTransId());
                            createConnectorError(log, msg, sock, cardResponse.getResultCode(), cardResponse.getMessage(),
                                    false, jsonCardInfo);
                            return;
                        } else {
                            log.add("bankResponse", "is empty");
                            createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, null, false, jsonCardInfo);
                            return;
                        }
                    } else {
                        log.add("connector return", bankAsyncResult.cause().getMessage());
                        createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, null, false, jsonCardInfo);
                        return;
                    }
                }
            });
        } else if (WalletMappingConst.ActionType.MAP.equalsIgnoreCase(requestType))  {
            log.add("func---->", "confirm wallet map");
            int err = WalletMappingError.SUCCESS;
            if (StringUtils.isEmpty(otp)) {
                err = WalletMappingError.MISSING_OTP;
            }
            if (WalletMappingError.SUCCESS != err) {
                String desc = WalletMappingError.getDesc(err);
                log.add("errDesc", desc);
                TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("err")
                        .setValue("" + err)).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue(desc));
                Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.WALLET_MAP_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                        , textValueMsgBuilder.build().toByteArray());
                mCommon.writeDataToSocket(sock, createVMbuffer);
                log.writeLog();
                return;
            }

            log.add("mapWallet confirm map", "send request to connector");
            final CardRequest cardRequest = BankCardRequestFactory.confirmMapRequest(bankCode, "0" + msg.cmdPhone, data.pin, "", otp, bankAccountNo,
                    personalId, bankAccountName, kvps, WalletType.MOMO);

            JsonObject joBankReq = new JsonObject();
            joBankReq.putNumber(BankHelperVerticle.COMMAND, BankHelperVerticle.ATM_IN_OUT);
            joBankReq.putString(BankHelperVerticle.PHONE, "0" + msg.cmdPhone);
            joBankReq.putObject(BankHelperVerticle.DATA, cardRequest.getJsonObject());
            vertx.eventBus().sendWithTimeout(AppConstant.BankHelperVerticle_ADDRESS, joBankReq, 80 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> bankAsyncResult) {
                    if (bankAsyncResult.succeeded() && bankAsyncResult.result() != null) {

                        JsonObject reponse = bankAsyncResult.result().body();
                        log.add("proxy_bank's response", reponse.toString());
                        if (reponse.getInteger(BankHelperVerticle.ERROR) != 0) {
                            log.add("errorCode", "time out");
                            createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, null, true, jsonCardInfo);
                            return;
                        }

                        final CardResponse cardResponse = new CardResponse(reponse.getObject(BankHelperVerticle.DATA));
                        log.add("result", "Co ket qua tra ve tu connector");
                        log.add("cardResponse", cardResponse);

                        if (cardResponse != null && cardResponse.getResultCode() == 0) {
                            log.add("wallet confirm map", "success");
                            log.add("transId", cardResponse.getRequest().getCoreTransId());
                            //todo add cardinfo into CardTable
                            log.add("name ATM", cardResponse.getRequest().getCardInfo().getHolderName());
                            Card.Obj cardObj = createATMCardInfo(cardResponse, bankCode, bankName, log);
                            card.upsertATMCard(msg.cmdPhone, cardObj.toJson(), new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean result) {
                                    log.add("result upsert", result);
                                    createConnectorError(log, msg, sock, cardResponse.getResultCode(), cardResponse.getMessage(), true, jsonCardInfo);
                                    JsonObject joUpdate;
                                    if (cardResponse.getEnableIdentify()) {
                                        joUpdate = new JsonObject().putString(colName.PhoneDBCols.NAME, cardResponse.getRequest().getCardInfo().getHolderName())
                                                .putBoolean(colName.PhoneDBCols.IS_NAMED, true)
                                                .putString(colName.PhoneDBCols.CARD_ID, cardResponse.getRequest().getCardInfo().getPersonalId()).putNumber(colName.PhoneDBCols.STATUS_ATMCARD, 1);
                                    } else {
                                        joUpdate = new JsonObject().putNumber(colName.PhoneDBCols.STATUS_ATMCARD, 1);
                                    }
                                    phonesDb.updatePartial(msg.cmdPhone, joUpdate, new Handler<PhonesDb.Obj>() {
                                        @Override
                                        public void handle(PhonesDb.Obj event) {
                                            mCommon.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);
                                        }
                                    });
                                }
                            });
                        } else if (cardResponse != null && cardResponse.getResultCode() != 0) {
                            log.add("connector return", "error");
                            log.add("transId", cardResponse.getRequest().getCoreTransId());
                            createConnectorError(log, msg, sock, cardResponse.getResultCode(), cardResponse.getMessage(), true, jsonCardInfo);
                            return;
                        } else {
                            log.add("bankResponse", "is empty");
                            createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, "Giao dịch đang được xử lý", true, jsonCardInfo);
                            return;
                        }
                    } else {
                        log.add("connector return", bankAsyncResult.cause().getMessage());
                        createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, "Giao dịch đang được xử lý", true, jsonCardInfo);
                        return;
                    }
                }
            });
        } else if (WalletMappingConst.ActionType.UNMAP.equalsIgnoreCase(requestType))  {
            log.add("func---->", "unmap wallet");
            int err = WalletMappingError.SUCCESS;
            if (StringUtils.isEmpty(bankCode)) {
                err = WalletMappingError.INVALID_BANK_CODE;
            }
            if (WalletMappingError.SUCCESS != err) {
                String desc = WalletMappingError.getDesc(err);
                log.add("errDesc", desc);
                TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("err")
                        .setValue("" + err)).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue(desc));
                Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.WALLET_MAP_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                        , textValueMsgBuilder.build().toByteArray());
                mCommon.writeDataToSocket(sock, createVMbuffer);
                log.writeLog();
                return;
            }

            log.add("mapWallet unmap", "send request to connector");
            final CardRequest cardRequest = BankCardRequestFactory.confirmUnmapRequest(bankCode, "0" + msg.cmdPhone, data.pin, "", cardId, otp, kvps, WalletType.MOMO);

            JsonObject joBankReq = new JsonObject();
            joBankReq.putNumber(BankHelperVerticle.COMMAND, BankHelperVerticle.ATM_IN_OUT);
            joBankReq.putString(BankHelperVerticle.PHONE, "0" + msg.cmdPhone);
            joBankReq.putObject(BankHelperVerticle.DATA, cardRequest.getJsonObject());
            vertx.eventBus().sendWithTimeout(AppConstant.BankHelperVerticle_ADDRESS, joBankReq, 80 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> bankAsyncResult) {
                    if (bankAsyncResult.succeeded() && bankAsyncResult.result() != null) {

                        JsonObject reponse = bankAsyncResult.result().body();
                        log.add("proxy_bank's response", reponse.toString());
                        if (reponse.getInteger(BankHelperVerticle.ERROR) != 0) {
                            log.add("errorCode", "time out");
                            createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, null, true, jsonCardInfo);
                            return;
                        }

                        final CardResponse cardResponse = new CardResponse(reponse.getObject(BankHelperVerticle.DATA));
                        log.add("result", "Co ket qua tra ve tu connector");
                        log.add("cardResponse", cardResponse);

                        if (cardResponse != null && cardResponse.getResultCode() == 0) {
                            log.add("wallet unmap", "success");
                            log.add("transId", cardResponse.getRequest().getCoreTransId());

                            JsonObject joUpdate = new JsonObject();
                            joUpdate.putBoolean(colName.CardDBCols.DELETED, true);
                            joUpdate.putNumber(colName.CardDBCols.STATUS, 2);
                            joUpdate.putNumber(colName.CardDBCols.LAST_SYNC_TIME, System.currentTimeMillis());
                            log.add("cardId", cardId);
                            card.deleteATMCard(msg.cmdPhone, cardId, joUpdate, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean event) {
                                    createConnectorError(log, msg, sock, WalletMappingError.SUCCESS, cardResponse.getMessage(), true, jsonCardInfo);
                                    card.findAllActivedATMCard(msg.cmdPhone, new Handler<ArrayList<Card.Obj>>() {
                                        @Override
                                        public void handle(ArrayList<Card.Obj> listCard) {
                                            if (listCard.size() < 1) {
                                                JsonObject joUpdate = new JsonObject().putNumber(colName.PhoneDBCols.STATUS_ATMCARD, 2);
                                                phonesDb.updatePartial(msg.cmdPhone, joUpdate, new Handler<PhonesDb.Obj>() {
                                                    @Override
                                                    public void handle(PhonesDb.Obj event) {
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            });
                        } else if (cardResponse != null && cardResponse.getResultCode() != 0) {
                            log.add("connector return", "error");
                            log.add("transId", cardResponse.getRequest().getCoreTransId());
                            createConnectorError(log, msg, sock, cardResponse.getResultCode(), cardResponse.getMessage(), true, jsonCardInfo);
                            return;
                        } else {
                            log.add("bankResponse", "is empty");
                            createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, null, true, jsonCardInfo);
                            return;
                        }
                    } else {
                        log.add("connector return", bankAsyncResult.cause().getMessage());
                        createConnectorError(log, msg, sock, WalletMappingError.TIMEOUT, null, true, jsonCardInfo);
                        return;
                    }
                }
            });
        } else {
            int err = WalletMappingError.INVALID_PARAMETERS;
            String desc = WalletMappingError.getDesc(err);
            log.add("errDesc", desc);
            TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
            textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("err")
                    .setValue("" + err)).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue(desc));
            Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.WALLET_MAP_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                    , textValueMsgBuilder.build().toByteArray());
            mCommon.writeDataToSocket(sock, createVMbuffer);
            log.writeLog();
            return;
        }
        mCommon.updateSessionTime(msg.cmdPhone, System.currentTimeMillis());
    }

    private void addNewBankLinkCard(final Common.BuildLog log, final MomoMessage msg, final JsonObject bankRequest, final Handler<Boolean> callback) {
        final String bankCode = bankRequest.getString(WalletMappingConst.bankCode, "");
        final String bankAccountNo = bankRequest.getString(WalletMappingConst.bankAccountNo, "");
        final String bankAccountName = bankRequest.getString(WalletMappingConst.bankAccountName, "");
        final String cardCreateAt = bankRequest.getString(WalletMappingConst.cardCreateAt, "0");

        final JsonObject cardObj = new JsonObject();
        cardObj.putNumber(colName.CardDBCols.LAST_SYNC_TIME, System.currentTimeMillis());
        cardObj.putString(colName.CardDBCols.CARD_HOLDER_NAME, bankAccountName);
        char[] mashCard = new char[bankAccountNo.length() - 4];
        Arrays.fill(mashCard, '*');
        String cardNumberThin = String.valueOf(mashCard)  + bankAccountNo.substring(bankAccountNo.length() - 4);
        cardObj.putString(colName.CardDBCols.CARD_HOLDER_NUMBER, cardNumberThin);
        cardObj.putString(colName.CardDBCols.CARD_HOLDER_YEAR, cardCreateAt);
        cardObj.putString(colName.CardDBCols.BANK_NAME, StringConstUtil.bankLinkNames.get(bankCode));
        cardObj.putString(colName.CardDBCols.BANKID, bankCode);
        cardObj.putNumber(colName.CardDBCols.BANK_TYPE, 10);
        cardObj.putBoolean(colName.CardDBCols.DELETED, false);
        cardObj.putNumber(colName.CardDBCols.STATUS, 1);
        cardObj.putNumber(colName.CardDBCols.ROW_ID, (long)System.currentTimeMillis());
        String cardHash = "";
        try {
            cardHash = md5.getMD5Hash(bankAccountNo);
        } catch (Exception e) {
        }
        cardObj.putString(colName.CardDBCols.CARD_CHECKSUM, cardHash);

        card.deleteBanklinkCard(msg.cmdPhone, bankCode, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                card.insert(msg.cmdPhone, new Card.Obj(cardObj), new Handler<Integer>() {
                    @Override
                    public void handle(Integer res) {
                        log.add("Insert new banklink card " + bankCode, (res == 0 ? "ok" : "fail"));
                        log.writeLog();
                        callback.handle(res == 0);
                    }
                });
            }
        });
    }

    private void delNewBankLinkCard(final Common.BuildLog log, final MomoMessage msg, final JsonObject bankRequest, final Handler<Boolean> callback) {
        final String bankCode = bankRequest.getString(WalletMappingConst.bankCode, "");

        card.deleteBanklinkCard(msg.cmdPhone, bankCode, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                log.add("Del banklink card of bank " + bankCode, (jsonObject != null ? "ok" : "fail"));
                log.writeLog();
                callback.handle(jsonObject != null);
            }
        });
    }

    private void createConnectorError(Common.BuildLog log, MomoMessage msg, NetSocket sock, int err, String desc, boolean hasNoti, JsonObject bankRequest) {
        if (StringUtils.isEmpty(desc)) {
            desc = WalletMappingError.getDesc(err);
        }
        log.add("errCode", err);
        log.add("errDesc", desc);
        TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
        textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("err")
                .setValue("" + err)).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue(desc));
        Buffer createVMbuffer = MomoMessage.buildBuffer(MsgType.WALLET_MAP_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                , textValueMsgBuilder.build().toByteArray());
//        if (hasNoti) {
//            mapWalletNotify(log, err, msg.cmdPhone, bankRequest);
//        }
        mCommon.writeDataToSocket(sock, createVMbuffer);
        log.writeLog();
    }

    private void mapWalletNotify(Common.BuildLog log, int err, int toUser, JsonObject bankRequest) {
        Notification noti = new Notification();
        noti.receiverNumber = toUser;
        noti.priority = 1;
        noti.time = System.currentTimeMillis();
        noti.type = NotificationType.NOTI_GENERIC_VALUE;

        String requestType = bankRequest.getString(WalletMappingConst.requestType, "");
        String bankName = StringConstUtil.bankLinkNames.get(bankRequest.getString(WalletMappingConst.bankCode, ""));
        String bankCardNumber = bankRequest.getString(WalletMappingConst.bankAccountNo, "");
        String cardNumberThin = bankCardNumber.substring(bankCardNumber.length() - 4);
        if (err == 0) {
            if (WalletMappingConst.ActionType.MAP.equalsIgnoreCase(requestType)) {
                String caption1 = "Liên kết thành công";
                String body1 = "Quý khách đã liên kết thành công Ví MoMo với thẻ %s **** %s";
                noti.caption = caption1;
                noti.body = String.format(body1, bankName, cardNumberThin);
                logger.error("Send noti : " + body1);
                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                        , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                logger.error("Send noti success");
                            }
                        });

                String caption2 = "Thông báo";
                String body2 = "Quý khách đã liên kết thành công Ví điện tử với thẻ %s **** %s";
                noti.caption = caption2;
                noti.body = String.format(body2, bankName, cardNumberThin);
                ;
                logger.error("Send noti : " + body1);
                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                        , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                logger.error("Send noti success");
                            }
                        });
            } else if (WalletMappingConst.ActionType.UNMAP.equalsIgnoreCase(requestType)) {
                String caption1 = "Hủy liên kết thẻ thành công!";
                String body1 = "Quý khách đã hủy liên kết thẻ %s **** %s với Ví MoMo thành công";
                noti.caption = caption1;
                noti.body = String.format(body1, bankName, cardNumberThin);
                logger.error("Send noti : " + body1);
                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                        , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                logger.error("Send noti success");
                            }
                        });
            }
        } else {
            if (WalletMappingConst.ActionType.MAP.equalsIgnoreCase(requestType)) {
                String caption = "Liên kết thẻ không thành công";
                String body = "Quý khách đã liên kết không thành công Ví MoMo với thẻ %s**** %s";
                noti.caption = caption;
                noti.body = String.format(body, bankName, cardNumberThin);
                logger.error("Send noti : " + body);
                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                        , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                logger.error("Send noti success");
                            }
                        });
            } else if (WalletMappingConst.ActionType.UNMAP.equalsIgnoreCase(requestType)) {
                String caption1 = "Hủy liên kết thẻ không thành công !";
                String body1 = "Quý khách đã hủy liên kết thẻ %s**** %s với Ví MoMo không thành công";
                noti.caption = caption1;
                noti.body = String.format(body1, bankName, cardNumberThin);
                logger.error("Send noti : " + body1);
                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                        , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                logger.error("Send noti success");
                            }
                        });
            }
        }
    }

    private void checkVisaConnectorAgain(final Common.BuildLog log, final MomoMessage msg, final AtomicInteger atomicCheck,final String cardCheckSum, final SockData data) {
        //Kiem tra the visa xem co map thanh cong hay khong.
        log.add("desc", "Kiem tra xem co the map thanh cong khong");
        if(atomicCheck.intValue() == 2)
        {
            log.add("desc", "Recheck visa connector 2 times");
            log.writeLog();
            return;
        }
        vertx.setTimer(1000L * 90, new Handler<Long>() {
            @Override
            public void handle(final Long timerCheck) {
                log.add("desc", "Tim tat ca the visa");
                card.findActivedCardVisaWithCardCheckSum(cardCheckSum, msg.cmdPhone, new Handler<ArrayList<Card.Obj>>() {
                    @Override
                    public void handle(ArrayList<Card.Obj> listCards) {
                        if (listCards.size() == 0) {
                            log.add("desc", "Khong co the visa add thanh cong roi");
                            VMRequest.getCardList(vertx, sbsBussAddress, "0" + msg.cmdPhone, "mobi", log, new Handler<List<CardInfo>>() {
                                @Override
                                public void handle(List<CardInfo> listCardInfos) {
                                    if (listCardInfos == null || listCardInfos.size() == 0) {
                                        vertx.cancelTimer(timerCheck);
                                        atomicCheck.incrementAndGet();
                                        checkVisaConnectorAgain(log, msg, atomicCheck, cardCheckSum, data);
                                        return;
                                    }

                                    ArrayList<JsonObject> jsonArray = new ArrayList<JsonObject>();
                                    String cardCheckSumConnector = "";
                                    String bankAcc = "";
                                    for (int i = 0; i < listCardInfos.size(); i++) {
                                        CardInfo ci = listCardInfos.get(i);
                                        if(cardCheckSum.equalsIgnoreCase(ci.getHashString()))
                                        {
                                            cardCheckSumConnector = ci.getHashString();
                                            bankAcc = ci.getCardNumber();
                                        }
                                        JsonObject joCardInfo = buildCardInfo(ci);
                                        jsonArray.add(joCardInfo);
                                    }
                                    log.add("cardInfo", jsonArray);
                                    for (Object o : jsonArray) {
                                        card.upsertVisaMaster(msg.cmdPhone, (JsonObject) o, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {
                                                log.add("upsertVisaMaster ----------------------------", aBoolean);
//                                                log.writeLog();
                                            }
                                        });
                                    }
                                    if(!"".equalsIgnoreCase(cardCheckSumConnector))
                                    {
                                        //Co thong tin cua cai the hoi nay add ne.
                                        log.add("desc " + msg.cmdPhone, "co thong tin cua the add hoi nay ma bi time out " + cardCheckSumConnector);
                                        JsonObject joExtra = new JsonObject();
                                        joExtra.getString(StringConstUtil.ReferralVOnePromoField.PHONE_NUMBER, "0" + msg.cmdPhone);
                                        joExtra.getString(StringConstUtil.ReferralVOnePromoField.CARD_INFO, cardCheckSumConnector);
                                        joExtra.getString(StringConstUtil.ReferralVOnePromoField.BANK_ACC, bankAcc);
                                        joExtra.getString(StringConstUtil.ReferralVOnePromoField.DEVICE_IMEI, data.imei);
//                                        promotionProcess.executeReferralPromotion(StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.BACKEND_BANK_MAPPING, null, null, null, null, log, joExtra);
//                                        promotionProcess.executeSCBPromotionProcess("0" + msg.cmdPhone, cardCheckSumConnector, bankAcc, data.imei, new JsonObject(), StringConstUtil.StandardCharterBankPromotion.MSG_TYPE_SCB_PROMOTION.BANK_MAPPING, log);
                                        com.mservice.momo.vertx.models.Notification noti = new com.mservice.momo.vertx.models.Notification();
                                        long curTime = System.currentTimeMillis();
                                        noti.receiverNumber = DataUtil.strToInt("" + msg.cmdPhone);
                                        noti.caption = "Thêm thẻ thành công!";
                                        noti.body = "Chúc mừng Quý khách đã thực hiện thêm thẻ thành công với thẻ VISA - " + bankAcc;
                                        noti.bodyIOS = noti.body;
                                        noti.priority = 2;
                                        noti.tranId = curTime;
                                        noti.time = curTime;
                                        noti.cmdId = curTime;
                                        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                        noti.status = com.mservice.momo.vertx.models.Notification.STATUS_DISPLAY;
                                        noti.sms = "";
                                        Misc.sendNoti(vertx, noti);
                                    }
                                    phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                                        @Override
                                        public void handle(PhonesDb.Obj phoneObj) {
                                            if(phoneObj == null || phoneObj.isNamed)
                                            {
                                                logger.info("Tai khoan da dinh danh, khong add group visa " + msg.cmdPhone);
                                                return;
                                            }
                                            logger.info("SET GROUP VISA FOR " + msg.cmdPhone);
                                            try
                                            {
                                                Misc.setGroupVisa(VISA_GROUP, VISA_CAPSET_ID, VISA_UPPER_LIMIT, groupManageDb, vertx, logger, "0" + msg.cmdPhone);
                                            }
                                            catch (Exception ex)
                                            {
                                                logger.info("SET GROUP VISA FOR " + msg.cmdPhone + " ERROR " + ex.getMessage());
                                            }

                                        }
                                    });
                                    vertx.cancelTimer(timerCheck);
                                    return;
                                }
                            });
                            return;
                        }
                        vertx.cancelTimer(timerCheck);
                    }
                });
            }
        });
    }

    private void processCashInVisa(final int feeType, long amount, final Common.BuildLog log, final int tranType, final String numberReceiver, final NetSocket sock, final SockData data, final MomoMessage msg, final String cardId, final String cardLbl, final String cardTypeString, final String channel, final String emailDesc, final String providerName, final long ackTime, final String serviceId, String billId) {
        final long actualAmount = Math.max(sbsGetMinValue(feeType), amount);
        log.add("actual amount", actualAmount);

        // BEGIN M2M
        if (tranType == TranHisV1.TranType.M2M_VALUE || tranType == TranHisV1.TranType.M2C_VALUE) {
//                final String emailDesc = "Nạp tiền vào ví điện tử";
            log.add("Loai giao dich -->", "Kiem tra M2M thanh cong");
            //Check M2m Or M2N
            phonesDb.getPhoneObjInfo(DataUtil.strToInt(numberReceiver), new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj obj) {
                    if (obj == null || !obj.isReged) {
                        log.add("Loai giao dich -->", "M2N");
                        //M2N
                        doVisaMasterCashInForM2M(sock, data, msg, cardId, cardLbl, cardTypeString, actualAmount, feeType,
                                0, channel, emailDesc, log, providerName, actualAmount, ackTime, tranType, serviceId);
                    } else {
                        log.add("Loai giao dich -->", "Store_M2M");
                        agentsDb.getOneAgent(numberReceiver, "createVMCardToken", new Handler<AgentsDb.StoreInfo>() {
                            @Override
                            public void handle(AgentsDb.StoreInfo storeInfo) {
                                String storeName = storeInfo == null ? "" : storeInfo.storeName;
                                storeName = "".equalsIgnoreCase(storeName) ? "" : Misc.removeAccent(storeName);
                                //Kiem tra co phai diem giao dich khong
                                if (storeName != null && storeName != "" && storeInfo.status != 2) {
                                    Misc.getRetailerFee(vertx, tranType, actualAmount, msg.cmdPhone, new Handler<FeeObj>() {
                                        @Override
                                        public void handle(FeeObj feeObj) {
                                            long newFeeForM2m = 0;
                                            long newAmountForM2m = actualAmount;
                                            if (feeObj != null) {
                                                newFeeForM2m = feeObj.fee;
                                                newAmountForM2m = newAmountForM2m + newFeeForM2m;
                                            }
                                            doVisaMasterCashInForM2M(sock, data, msg, cardId, cardLbl, cardTypeString, newAmountForM2m, feeType,
                                                    newFeeForM2m, channel, emailDesc, log, providerName, actualAmount, ackTime, tranType, serviceId);
                                        }
                                    });
                                } else {
                                    log.add("Loai giao dich -->", "M2M");
                                    //M2M
                                    //Get fee for M2M
                                    Misc.getM2MFee(vertx, tranType, actualAmount, "0" + msg.cmdPhone ,numberReceiver, new Handler<FeeDb.Obj>() {
                                        @Override
                                        public void handle(FeeDb.Obj obj) {
                                            long newFeeForM2m = 0;
                                            long newAmountForM2m = actualAmount;
                                            if (obj != null) {
                                                newFeeForM2m = obj.STATIC_FEE;
                                                newAmountForM2m = newAmountForM2m + newFeeForM2m;
                                            }
                                            doVisaMasterCashInForM2M(sock, data, msg, cardId, cardLbl, cardTypeString, newAmountForM2m, feeType,
                                                    newFeeForM2m, channel, emailDesc, log, providerName, actualAmount, ackTime, tranType, serviceId);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            });
        } else if (tranType == TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE) {
            log.add("Loai giao dich -->", "VM_PROCESS_CASH_IN_VALUE");
            doVisaMasterCashInForM2M(sock, data, msg, cardId, cardLbl, cardTypeString, actualAmount, feeType,
                    0, channel, emailDesc, log, providerName, actualAmount, ackTime, tranType, serviceId);
        } else {
            String serviceIdTemp = serviceId;
            if (serviceId.equalsIgnoreCase("")) {
                if (tranType == TranHisV1.TranType.TOP_UP_VALUE) {
                    serviceIdTemp = "topup";
                } else if (tranType == TranHisV1.TranType.PHIM123_VALUE) {
                    serviceIdTemp = "123phim";
                } else {
                    serviceIdTemp = "noservice";
                }
            }

            log.add("method: ", "Voucher and gift");
            log.add("serviceId: ", serviceId);
            log.add("serviceIdTemp: ", serviceIdTemp);
            final String ticket_code = serviceId;
            final String invoice_no = billId.trim();

            final JsonObject jsonObject = new JsonObject();
            jsonObject.putString(VMConst.ticket_code, ticket_code);
            jsonObject.putString(VMConst.invoice_no, invoice_no);
            final String serviceId_visa = serviceIdTemp;
            TransferWithGiftContext.build(msg.cmdPhone,
                    serviceIdTemp,
                    billId,
                    actualAmount,
                    vertx,
                    giftManager,
                    data,
                    TRANWITHPOINT_MIN_POINT,
                    TRANWITHPOINT_MIN_AMOUNT,
                    logger, new Handler<TransferWithGiftContext>() {
                        @Override
                        public void handle(TransferWithGiftContext context) {
                            //not used momo value
                            log.add("point", context.point); // so point su dung
                            log.add("voucher", context.voucher); // so voucher su dung
                            log.add("tranAmount", context.amount); // gia tri giao dich
                            log.add("momo", context.momo); // tien momo thuc te sau khi tru voucher - point

                            //no need to do cash in through visa-master
                            if (context.momo <= 0) {
                                TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                                Buffer createVMbuffer;
                                textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("error")
                                        .setValue("-100")).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue("Thanh toan voucher va gift, khong thanh toan bang Visa Master"));


                                log.add("note", "no use cash from momo, no need to do bank cash in");
                                createVMbuffer = MomoMessage.buildBuffer(MsgType.CREATE_TOKEN_REPLY_VALUE
                                        , msg.cmdIndex
                                        , msg.cmdPhone
                                        , textValueMsgBuilder.build().toByteArray());
                                log.writeLog();
                                mCommon.writeDataToSocket(sock, createVMbuffer);
                                return;
                            }
                            final long cashInAmt = context.momo;
                            doVisaMasterCashInForM2M(sock, data, msg, cardId, cardLbl, cardTypeString, cashInAmt, feeType,
                                    0, channel, emailDesc, log, providerName, actualAmount, ackTime, tranType, serviceId_visa);
                        }
                    }
            );
        }
    }

    private long sbsGetMinValue(int fee) {

        long minValue = 0;
        if (fee == VMFeeType.DEPOSIT_FEE_TYPE) {
            if (sbsCfg != null) {
                minValue = sbsCfg.getLong("min_value_direct", 10000);
            } else {
                minValue = 10000;
            }
        } else {
            if (sbsCfg != null) {
                minValue = sbsCfg.getLong("min_value_redirect", 10000);
            } else {
                minValue = 10000;
            }
        }
        return minValue;
    }

    private JsonObject getVmCardInfo(HashMap<String, String> hashMap) {

        JsonObject jsonCardInfo = new JsonObject();
        for (String s : hashMap.keySet()) {
            jsonCardInfo.putString(s, hashMap.get(s).toString());
        }
       /* String partnerCode = hashMap.containsKey(VMConst.partnerCode) ? hashMap.get(VMConst.partnerCode) : "";
        String userName = hashMap.containsKey(VMConst.userName) ? hashMap.get(VMConst.userName) : "";
        String cardType = hashMap.containsKey(VMConst.cardType) ? hashMap.get(VMConst.cardType) : "";

        String cardNumber = hashMap.containsKey(VMConst.cardNumber) ? hashMap.get(VMConst.cardNumber) : "";
        String cardexpired = hashMap.containsKey(VMConst.cardExpired) ? hashMap.get(VMConst.cardExpired) : "";
        long cvvnumber = hashMap.containsKey(VMConst.cvnNumber) ? DataUtil.strToLong(hashMap.get(VMConst.cvnNumber)): 0;
        String cardholdername = hashMap.containsKey(VMConst.cardHolerName) ? hashMap.get(VMConst.cardHolerName) : "";
        String email = hashMap.containsKey(VMConst.email) ? hashMap.get(VMConst.email):"";

        String appId = hashMap.containsKey(VMConst.appId) ? hashMap.get(VMConst.appId) : "";
        jsonCardInfo.putString(VMConst.userName, userName);
        jsonCardInfo.putString(VMConst.cardType, cardType);
        jsonCardInfo.putString(VMConst.cardNumber, cardNumber);
        jsonCardInfo.putString(VMConst.cardExpired, cardexpired);
        jsonCardInfo.putNumber(VMConst.cvnNumber, cvvnumber);
        jsonCardInfo.putString(VMConst.cardHolerName, cardholdername);
        jsonCardInfo.putString(VMConst.email,email);
        jsonCardInfo.putString(VMConst.partnerCode,partnerCode);
        jsonCardInfo.putString(VMConst.appId, appId);*/
        return jsonCardInfo;
    }

    //This method ... used to get all card types on Database.
    public void getCardType(final NetSocket sock
            , final MomoMessage msg
            , final Handler<JsonObject> callback) {

/*
        GET_CARD_TYPE = 1219; // body: TextValueMsg
        //key           value
        //partnercode   sbs
        //lasttime      lastime, default 0*/

        /*GET_CARD_TYPE_REPLY = 1220; //body ServiceReply
                                *//*optional string service_type    = 1;
                                optional string partner_code    = 2;        --> partnercode
                                optional string service_id      = 3;        --> cardtype
                                optional string service_name    = 4;        --> desctiption
                                optional string icon_url        = 6;        --> icon cardtype
                                optional bool   has_check_debit    = 9;    --> enable
                                optional uint64 last_update        =11;    -->lasttime
                                */
        // TextValue is gotten from client.
        TextValueMsg fields;
        try {
            fields = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            fields = null;
        }

        if (fields == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(fields.getKeysList());
        long lastTime = hashMap.containsKey("lastime") ? DataUtil.stringToUNumber(hashMap.get("lastime")) : 0;
        String partnerCode = hashMap.containsKey("partnercode") ? (String) hashMap.get("partnercode") : "";

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("partnercode", partnerCode);
        log.add("lasttime", Misc.dateVNFormatWithTime(lastTime));

        //Truyen command get all card type
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_ALL_CARD_TYPE;
        serviceReq.lastTime = lastTime;

        //Truoc khi xuong database, goi xuong config de xem gia tri co luu lai hay khong
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonArray>>() {

            //Ket qua tra lai sau khi handle. Sau khi goi qua Config se lay ve 1 mang gia tri thay doi
            @Override
            public void handle(Message<JsonArray> message) {
                JsonArray array = message.body();
                ArrayList<CardTypeDb.Obj> objs = new ArrayList<CardTypeDb.Obj>(); // lay duoc nhung record thay doi tu ServiceConfVerticle.
                ServiceReply.Builder builder = ServiceReply.newBuilder();
                // lay cac doi tuong trong JsonArray bo vao ArrayList
                if (array != null && array.size() > 0) {
                    for (int i = 0; i < array.size(); i++) {
                        objs.add(new CardTypeDb.Obj((JsonObject) array.get(i)));
                    }
                }
                log.add("list size", objs == null ? 0 : objs.size());
                //Add tat ca cac doi tuong vao trong ServiceList(dua theo ket qua mapping) de tra ve cho Client.
                if (objs != null && objs.size() > 0) {
                    for (CardTypeDb.Obj o : objs) {
                        builder.addServiceList(ServiceItem.newBuilder()
                                        .setPartnerCode(o.partnerCode)
                                        .setServiceId(o.cardType)
                                        .setServiceName(o.desc)
                                        .setIconUrl(o.iconUrl)
                                        .setHasCheckDebit(o.enable)
                                        .setLastUpdate(o.lastTime)
                        );
                    }
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MsgType.GET_CARD_TYPE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build()
                                .toByteArray()
                );
                //Truyen nhung gia tri thay doi len cho Client.
                mCommon.writeDataToSocket(sock, buf);
                log.writeLog();

            }
        });
    }

    public void doVisaMasterCashInForM2M(final NetSocket sock, final SockData data, final MomoMessage msg, final String cardId, final String cardLbl, final String cardTypeString,
                                         final long newAmountForM2m, final int feeType, final long newFeeForM2m, String channel, final String emailDesc,
                                         final Common.BuildLog log, final String providerName, final long actualAmount, final long ackTime, final int trantype, final String serviceId) {

        VMRequest.doVisaMasterCashIn(vertx, sbsBussAddress, data
                , "0" + msg.cmdPhone
                , cardId
                , cardLbl
                , cardTypeString
                , newAmountForM2m
                , feeType
                , newFeeForM2m
                , channel
                , emailDesc
                , log, providerName, trantype, serviceId, new Handler<VisaResponse>() {
            @Override
            public void handle(VisaResponse visaResponse) {

                TextValueMsg.Builder textValueMsgBuilder = TextValueMsg.getDefaultInstance().toBuilder();
                Buffer createVMbuffer;
                if (visaResponse == null) {
                    textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("error")
                            .setValue(1000 + "")).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue("Visarespond is null"));

                }
                else if (visaResponse != null && visaResponse.getResultCode() == 0) {
                    String url = visaResponse.getUrl();
                    log.add("url", url);

                    JsonObject postInfo = visaResponse.getPostInfo();
                    postInfo.putString("url", url);
                    if (!"".equalsIgnoreCase(url)) {
                        textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("url")
                                .setValue(url));
                    }
                    final String cardCheckSum = visaResponse.getCardChecksum();
                    //BEGIN 0000000015 Visa master promo
//                                                final VisaResponse visaResponse1 = visaResponse;
//                                                if(!cardCheckSum.equalsIgnoreCase(""))
//                                                {
//                                                    vmCardIdCardNumberDB.findOne(cardCheckSum, new Handler<VMCardIdCardNumberDB.Obj>() {
//                                                        @Override
//                                                        public void handle(VMCardIdCardNumberDB.Obj obj) {
//                                                            if(obj != null && visaResponse1.getCardInfos() != null)
//                                                            {
//                                                                //Update lai table
//                                                                JsonObject jsonUpdate = new JsonObject();
//                                                                jsonUpdate.putString(colName.VMCardIdCardNumber.CARDID, visaResponse1.getCardInfos().get(0).getCardId());
//                                                                vmCardIdCardNumberDB.updatePartial(cardCheckSum, jsonUpdate, new Handler<Boolean>() {
//                                                                    @Override
//                                                                    public void handle(Boolean aBoolean) {
//
//                                                                    }
//                                                                });
//                                                            }
//                                                            else
//                                                            {
//                                                                VMCardIdCardNumberDB.Obj vmCardIdObj = new VMCardIdCardNumberDB.Obj();
//                                                                vmCardIdObj.cardNumer = cardCheckSum;
//                                                                vmCardIdObj.cardId = visaResponse1.getCardInfos().get(0).getCardId();
//                                                                vmCardIdCardNumberDB.insert(vmCardIdObj, new Handler<Integer>() {
//                                                                    @Override
//                                                                    public void handle(Integer integer) {
//
//                                                                    }
//                                                                });
//                                                            }
//                                                        }
//                                                    });
//                                                }
                } else {
                    textValueMsgBuilder.addKeys(TextValue.getDefaultInstance().toBuilder().setText("error")
                            .setValue(visaResponse.getResultCode() + "")).addKeys(TextValue.getDefaultInstance().toBuilder().setText("desc").setValue(visaResponse.getDescription()));
                }
                createVMbuffer = MomoMessage.buildBuffer(MsgType.CREATE_TOKEN_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone
                        , textValueMsgBuilder.build().toByteArray());
                long tranId = visaResponse == null ? 0 : visaResponse.getVisaRequest().getCoreTransId();
                int error = visaResponse == null ? SoapError.SYSTEM_ERROR : visaResponse.getResultCode();
                JsonObject tranRpl = Misc.getJsonObjRpl(error, tranId, actualAmount, 1);

                log.add("error", visaResponse == null ? "-100" : visaResponse.getResultCode());
                log.add("desc", visaResponse == null ? "Core timeout" : visaResponse.getDescription() == null ? "" : visaResponse.getDescription());
                log.add("tranId: ", tranId);

                //chuyen tien ve --> thuc hien 1 giao dich dich khac
                TranHisUtils.addMoreTransferInfoTranRpl(tranRpl,
                        ackTime,
                        MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE,
                        StringConstUtil.IO_GET_MONEY_STATE,
                        StringConstUtil.DEFAULT_CATEGORY,
                        "Visa-Master",
                        "Visa-Master",
                        "Will-later",
                        actualAmount,
                        MomoProto.TranHisV1.SourceFrom.MASTERCARD_VALUE);

                //tao giao dich trung gian --> tra ve client
                mCommon.saveAndSendTempTran(vertx, msg, tranRpl, sock, data);
                log.writeLog();
                mCommon.writeDataToSocket(sock, createVMbuffer);
            }
        });
    }

    private void getServiceFee(String serviceId, final long cashInAmount, final Common.BuildLog log, final Handler<Long> callback) {

        log.add("func", "getFee");
        log.add("serviceId", serviceId);
        log.add("cashInAmount", cashInAmount);


        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_FEE;
        serviceReq.ServiceId = serviceId;
        serviceReq.channel = 0;
        serviceReq.inoutCity = 0;
        serviceReq.tranType = 0;
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ServiceFeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new ServiceFeeDb.Obj(message.body());
                }
                int static_fee;
                double dynamic_fee;

                if (obj == null) {
                    static_fee = 0;
                    dynamic_fee = 0;
                } else {
                    static_fee = obj.STATIC_FEE;
                    dynamic_fee = obj.DYNAMIC_FEE;
                }

                log.add("static_fee: ", static_fee);
                log.add("dynamic_fee: ", dynamic_fee);
                log.add("amount: ", cashInAmount);
                long fee = 0;
                if (cashInAmount != 0) {
                    fee = VMFeeType.calculateFeeMethod(static_fee, dynamic_fee, cashInAmount);
                }
                log.add("fee", fee);
                callback.handle(fee);
            }
        });
    }

    public void processGetFeeFromBank(final NetSocket sock, final MomoMessage msg) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        TextValueMsg textValueMsg;
        try {
            textValueMsg = TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            textValueMsg = null;
        }
        if (textValueMsg == null) {
            mCommon.writeErrorToSocket(sock);
            return;
        }


        HashMap<String, String> hashMap = Misc.getKeyValuePairs(textValueMsg.getKeysList());


        final String bankId = hashMap.containsKey(BANKID) ? hashMap.get(BANKID) : "";
        final String srcFrom = hashMap.containsKey(SRCFROM) ? hashMap.get(SRCFROM) : "";
        int transType = hashMap.containsKey(TRANSTYPE) ? DataUtil.strToInt(hashMap.get(TRANSTYPE)) : DataUtil.strToInt("0");
        long amount = hashMap.containsKey(AMOUNT) ? DataUtil.stringToUNumber(hashMap.get(AMOUNT)) : DataUtil.stringToUNumber("0");
        final long total_amount = hashMap.containsKey(TOTAL_AMOUNT) ? DataUtil.stringToUNumber(hashMap.get(TOTAL_AMOUNT)) : DataUtil.stringToUNumber("0");
        String numberReceiveM2M = hashMap.containsKey(NUMBERRECEIVEM2M) ? hashMap.get(NUMBERRECEIVEM2M) : "";

        int channel = hashMap.containsKey(CHANNEL) ? DataUtil.strToInt(hashMap.get(CHANNEL)) : 0;
        int iocity = hashMap.containsKey(IOCITY) ? DataUtil.strToInt(hashMap.get(IOCITY)) : 0;
        final String serviceId = hashMap.containsKey(SERVICE_ID) ? hashMap.get(SERVICE_ID) : "";
        final JsonObject jsonInfo = new JsonObject();

        /*
        {
            "bankid": "",
            "srcfrom": "",
            "transtype": 1,
            "amount": 100000,
            "numberReceiveM2M": "",
            "channel": 0,
            "ioCity": 0
        }
         */
        jsonInfo.putString(BANKID, bankId);
        jsonInfo.putString(SRCFROM, srcFrom);
        jsonInfo.putNumber(TRANSTYPE, transType);
        jsonInfo.putNumber(AMOUNT, amount);
        jsonInfo.putString(NUMBERRECEIVEM2M, numberReceiveM2M);
        jsonInfo.putNumber(CHANNEL, channel);
        jsonInfo.putNumber(IOCITY, iocity);
        jsonInfo.putString(StringConstUtil.NUMBER, "0" + msg.cmdPhone);

        log.add("bankId", bankId);
        log.add("SRCFROM", srcFrom);
        log.add("TRANSTYPE", transType);
        log.add("AMOUNT", amount);
        log.add("NUMBERRECEIVEM2M", numberReceiveM2M);
        //todo lay du lieu tu request

        logger.info("processGetFee, "
                + "phone: 0" + msg.cmdPhone
                + " bankid: " + bankId
                + " srcFrom: " + srcFrom
                + " trantype: " + transType
                + " amount: " + amount
                + " numberReceiveM2M: " + numberReceiveM2M);

        final TextValueMsg.Builder builder = TextValueMsg.newBuilder();

        getServiceFee(serviceId, total_amount, log, new Handler<Long>() {
            @Override
            public void handle(final Long serviceFee) {
                //get fee from bank
                getFeeFromBank(jsonInfo, log, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {

//                getServiceFee(SERVICE_ID)
                        log.add("jsonObject_getFeeFromBank", jsonObject);
//                long fee = aLong - amount; // Tinh fee
                        Buffer buf;
                        if(DataUtil.strToInt(srcFrom)== 1)
                        {
                            builder.addKeys(Misc.getTextValueBuilder(TOTALFEE, "0"));
                            builder.addKeys(Misc.getTextValueBuilder(TRANSFEE, "0"));
                            builder.addKeys(Misc.getTextValueBuilder(SERVICE_FEE, serviceFee + ""));
                            buf = MomoMessage.buildBuffer(
                                    MsgType.GET_FEE_BANK_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    builder.build()
                                            .toByteArray()
                            );
                        }
                        else{
                            builder.addKeys(Misc.getTextValueBuilder(TOTALFEE, jsonObject.getNumber(TOTALFEE, 0).toString()));
                            builder.addKeys(Misc.getTextValueBuilder(TRANSFEE, jsonObject.getNumber(TRANSFEE, 0).toString()));
                            builder.addKeys(Misc.getTextValueBuilder(SERVICE_FEE, serviceFee + ""));
                            buf = MomoMessage.buildBuffer(
                                    MsgType.GET_FEE_BANK_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    builder.build()
                                            .toByteArray()
                            );
                        }

                        //Truyen nhung gia tri thay doi len cho Client.
                        mCommon.writeDataToSocket(sock, buf);
                        log.writeLog();
                    }
                });
            }
        });
    }

    private void isBankLinkCashIn(String bankId, final Common.BuildLog log,
                                          final Handler<Boolean> callback) {
        log.add("func", "isBankLinkCashIn");

        // The lien ket noi dia
        if (StringConstUtil.bankLinkVerify.containsKey(bankId) && StringConstUtil.bankLinkVerify.get(bankId)) {
            callback.handle(true);
            log.writeLog();
            return;
        } else {
            callback.handle(false);
            log.writeLog();
            return;
        }
    }

    private void getFeeFromBank(final JsonObject jsonInfo, final Common.BuildLog log, final Handler<JsonObject> callback) {

        final String numberReceiveM2M = jsonInfo.getString(NUMBERRECEIVEM2M, "");
        final String senderNumber = jsonInfo.getString(StringConstUtil.NUMBER, "");
        log.add("func", "getFeeFromBank");
        log.add("jsonInfo", jsonInfo);
        final int transType = jsonInfo.getInteger(TRANSTYPE, 0);
        final long amount = jsonInfo.getLong(AMOUNT, 0);
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        final int transTypeForGetFeeShowPopUp = 0;
        // Khong phai giao dich chuyen tien M2M
        log.add("TRANSTYPE", transType);
        log.add("AMOUNT", amount);
        log.add("NUMBERRECEIVEM2M", numberReceiveM2M);
        if (numberReceiveM2M.equalsIgnoreCase("")) {
            jsonInfo.putNumber(MOMOFEE, 0);
            calculateFee(jsonInfo, log, callback);
        }
        else if(!"".equalsIgnoreCase(numberReceiveM2M) && DataUtil.strToInt(numberReceiveM2M) == 0)
        {
            //todo getFee GrabTaxi OR service transfer without feeM2M.
            if("1".equalsIgnoreCase(jsonInfo.getString(SRCFROM, ""))) //Thuc hien tu nguon MOMO
            {
                JsonObject jsonReply = new JsonObject();
                jsonReply.putNumber("totalfee", 0);
                jsonReply.putNumber("transfee", 0);
                callback.handle(jsonReply);
            }
            else { //THUC HIEN TU NGUON KHAC MOMO.
                jsonInfo.putNumber(MOMOFEE, 0);
                calculateFee(jsonInfo, log, callback);
            }
        }
        else // Giao dich chuyen tien M2M
        {
            agentsDb.getOneAgent(numberReceiveM2M, "getFeeFromBank", new Handler<AgentsDb.StoreInfo>() {
                @Override
                public void handle(AgentsDb.StoreInfo storeInfo) {
                    if (storeInfo != null && storeInfo.status != 2) {
                        log.add("storeInfo", "!=null");
                        Misc.getRetailerFee(vertx, transTypeForGetFeeShowPopUp, amount, DataUtil.strToInt(numberReceiveM2M), new Handler<FeeObj>() {
                            @Override
                            public void handle(FeeObj feeObj) {
                                JsonObject json = new JsonObject();
                                long newFeeForM2m = 0;
                                long newAmountForM2m = amount;
                                if (feeObj != null) {
                                    newFeeForM2m = feeObj.fee;
                                    newAmountForM2m = newAmountForM2m + newFeeForM2m;
                                }
                                json = jsonInfo;
                                json.removeField(AMOUNT);
                                json.putNumber(AMOUNT, newAmountForM2m);
                                json.putNumber(MOMOFEE, newFeeForM2m);
                                log.add("json", json);
                                if (transType != TranHisV1.TranType.BANK_IN_VALUE && transType != TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE && transType != TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE) {
                                    log.add("func", "M2MStore");
                                    JsonObject jsonReply = new JsonObject();
                                    jsonReply.putNumber("totalfee", 0);
                                    jsonReply.putNumber("transfee", newFeeForM2m);
                                    callback.handle(jsonReply);
                                    log.writeLog();
                                } else {
                                    log.add("func", "calculateFee");
                                    calculateFee(json, log, callback);
                                }
                            }
                        });
                    } else {
                        log.add("func", "getM2MFee");
                        phonesDb.getPhoneObjInfo(DataUtil.strToInt(numberReceiveM2M), new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj phoneObj) {
                                //final PhonesDb.Obj phoneObj = new PhonesDb.Obj(message.body().getObject(MongoKeyWords.RESULT,null));
                                logger.info("GET OTP-TIME FROM PHONE DB : " + phoneObj);
                                if (phoneObj != null) {
                                    Misc.getM2MFee(vertx, transTypeForGetFeeShowPopUp, amount, senderNumber, numberReceiveM2M, new Handler<FeeDb.Obj>() {
                                        @Override
                                        public void handle(FeeDb.Obj obj) {
                                            JsonObject json = new JsonObject();
                                            long newFeeForM2m = 0;
                                            long newAmountForM2m = amount;
                                            if (obj == null) {
                                                newFeeForM2m = 1000;
                                                newAmountForM2m = amount + newFeeForM2m;
                                            } else {
                                                newFeeForM2m = obj.STATIC_FEE;
                                                newAmountForM2m = amount + newFeeForM2m;
                                            }
                                            json = jsonInfo;
                                            json.removeField(AMOUNT);
                                            json.putNumber(AMOUNT, newAmountForM2m);
                                            json.putNumber(MOMOFEE, newFeeForM2m);
                                            log.add("json", json);
                                            if (transType != TranHisV1.TranType.BANK_IN_VALUE && transType != TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE && transType != TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE) {
                                                JsonObject jsonReply = new JsonObject();
                                                jsonReply.putNumber("totalfee", 0);
                                                jsonReply.putNumber("transfee", newFeeForM2m);
                                                callback.handle(jsonReply);
                                                log.writeLog();
                                            } else {
                                                log.add("func", "calculateFee");
                                                calculateFee(json, log, callback);
                                            }
                                        }
                                    });
                                } else {
                                    log.add("func", "M2N");
                                    jsonInfo.putNumber(MOMOFEE, 0);
                                    calculateFee(jsonInfo, log, callback);

                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void getFeeFromBankNew(final JsonObject jsonInfo, final Common.BuildLog log, final Handler<JsonObject> callback) {

        String bankId = jsonInfo.getString(BANKID, "");
        final String srcFrom = jsonInfo.getString(SRCFROM, "");
        if (!(!StringUtils.isEmpty(bankId) && !"sbs".equalsIgnoreCase(bankId) && !"sbstransfer".equalsIgnoreCase(bankId) &&
                (DataUtil.strToInt(srcFrom) == 3 || DataUtil.strToInt(srcFrom) == 5 || DataUtil.strToInt(srcFrom) == 6))) {
            JsonObject jsonReply = new JsonObject();
            jsonReply.putNumber("totalfee", 0);
            jsonReply.putNumber("transfee", 0);
            callback.handle(jsonReply);
            return;
        }

        final String numberReceiveM2M = jsonInfo.getString(NUMBERRECEIVEM2M, "");
        final String senderNumber = jsonInfo.getString(StringConstUtil.NUMBER, "");
        log.add("func", "getFeeFromBank");
        log.add("jsonInfo", jsonInfo);
        final int transType = jsonInfo.getInteger(TRANSTYPE, 0);
        final long amount = jsonInfo.getLong(AMOUNT, 0);
        if (0 == amount) {
            JsonObject jsonReply = new JsonObject();
            jsonReply.putNumber("totalfee", 0);
            jsonReply.putNumber("transfee", 0);
            callback.handle(jsonReply);
            return;
        }
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        final int transTypeForGetFeeShowPopUp = 0;
        // Khong phai giao dich chuyen tien M2M
        log.add("TRANSTYPE", transType);
        log.add("AMOUNT", amount);
        log.add("NUMBERRECEIVEM2M", numberReceiveM2M);
        if (numberReceiveM2M.equalsIgnoreCase("")) {
            jsonInfo.putNumber(MOMOFEE, 0);
            calculateFee(jsonInfo, log, callback);
        } else // Giao dich chuyen tien M2M
        {
            agentsDb.getOneAgent(numberReceiveM2M, "getFeeFromBank", new Handler<AgentsDb.StoreInfo>() {
                @Override
                public void handle(AgentsDb.StoreInfo storeInfo) {
                    if (storeInfo != null && storeInfo.status != 2) {
                        log.add("storeInfo", "!=null");
                        Misc.getRetailerFee(vertx, transTypeForGetFeeShowPopUp, amount, DataUtil.strToInt(numberReceiveM2M), new Handler<FeeObj>() {
                            @Override
                            public void handle(FeeObj feeObj) {
                                JsonObject json = new JsonObject();
                                long newFeeForM2m = 0;
                                long newAmountForM2m = amount;
                                if (feeObj != null) {
                                    newFeeForM2m = feeObj.fee;
                                    newAmountForM2m = newAmountForM2m + newFeeForM2m;
                                }
                                json = jsonInfo;
                                json.removeField(AMOUNT);
                                json.putNumber(AMOUNT, newAmountForM2m);
                                json.putNumber(MOMOFEE, newFeeForM2m);
                                log.add("json", json);
                                if (transType != TranHisV1.TranType.BANK_IN_VALUE && transType != TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE && transType != TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE) {
                                    log.add("func", "M2MStore");
                                    JsonObject jsonReply = new JsonObject();
                                    jsonReply.putNumber("totalfee", 0);
                                    jsonReply.putNumber("transfee", newFeeForM2m);
                                    callback.handle(jsonReply);
                                    log.writeLog();
                                } else {
                                    log.add("func", "calculateFee");
                                    calculateFee(json, log, callback);
                                }
                            }
                        });
                    } else {
                        log.add("func", "getM2MFee");
                        phonesDb.getPhoneObjInfo(DataUtil.strToInt(numberReceiveM2M), new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj phoneObj) {
                                //final PhonesDb.Obj phoneObj = new PhonesDb.Obj(message.body().getObject(MongoKeyWords.RESULT,null));
                                logger.info("GET OTP-TIME FROM PHONE DB : " + phoneObj);
                                if (phoneObj != null) {
                                    Misc.getM2MFee(vertx, transTypeForGetFeeShowPopUp, amount, senderNumber, numberReceiveM2M, new Handler<FeeDb.Obj>() {
                                        @Override
                                        public void handle(FeeDb.Obj obj) {
                                            JsonObject json = new JsonObject();
                                            long newFeeForM2m = 0;
                                            long newAmountForM2m = amount;
                                            if (obj == null) {
                                                newFeeForM2m = 1000;
                                                newAmountForM2m = amount + newFeeForM2m;
                                            } else {
                                                newFeeForM2m = obj.STATIC_FEE;
                                                newAmountForM2m = amount + newFeeForM2m;
                                            }
                                            json = jsonInfo;
                                            json.removeField(AMOUNT);
                                            json.putNumber(AMOUNT, newAmountForM2m);
                                            json.putNumber(MOMOFEE, newFeeForM2m);
                                            log.add("json", json);
                                            if (transType != TranHisV1.TranType.BANK_IN_VALUE && transType != TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE && transType != TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE) {
                                                JsonObject jsonReply = new JsonObject();
                                                jsonReply.putNumber("totalfee", 0);
                                                jsonReply.putNumber("transfee", newFeeForM2m);
                                                callback.handle(jsonReply);
                                                log.writeLog();
                                            } else {
                                                log.add("func", "calculateFee");
                                                calculateFee(json, log, callback);
                                            }
                                        }
                                    });
                                } else {
                                    log.add("func", "M2N");
//                                    JsonObject jsonReply = new JsonObject();
//                                    long newFeeForM2m = 0;
                                    jsonInfo.putNumber(MOMOFEE, 0);
                                    calculateFee(jsonInfo, log, callback);
//                                    jsonReply.putNumber("totalfee", newFeeForM2m);
//                                    jsonReply.putNumber("transfee", newFeeForM2m);
//                                    callback.handle(jsonReply);
//                                    log.writeLog();
                                }
                            }
                        });
                    }
                }
            });
        }
        // NOTE: 3 giao dich dang dung chung 1 cong thuc ... nen gop chung ... neu khac se tach ra.
//        if(transType == TranHisV1.TranType.BANK_IN_VALUE)
//        {
//
//        }
//        else if(transType == TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE)
//        {
//
//        }
//        else if(transType == TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE)
//        {
//
//        }
//        else
//        {
//
//        }
    }

    public void calculateFee(final JsonObject jsonInfo, final Common.BuildLog log, final Handler<JsonObject> callback) {

        log.add("func", "calculateFee");
        final JsonObject jsonReply = new JsonObject();
        String bankId = jsonInfo.getString(BANKID, "");
        final long cashInAmount = jsonInfo.getLong(AMOUNT, 0);
        final int tranType = jsonInfo.getInteger(TRANSTYPE, 0);
        final int channel = jsonInfo.getInteger(CHANNEL, 0);
        final int iocity = jsonInfo.getInteger(IOCITY, 0);
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_FEE;
        serviceReq.bankId = bankId;
        serviceReq.channel = channel;
        serviceReq.inoutCity = iocity;
        serviceReq.tranType = tranType;
        serviceReq.amount = cashInAmount;
        log.add("calculateFee json", serviceReq.toJSON());
//        log.writeLog();
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                FeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new FeeDb.Obj(message.body());
                }
                int static_fee;
                double dynamic_fee;

                if (obj == null) {
                    static_fee = 1100;
                    dynamic_fee = 1.2;
                } else {
                    static_fee = obj.STATIC_FEE;
                    dynamic_fee = obj.DYNAMIC_FEE;
                }

                log.add("static_fee: ", static_fee);
                log.add("dynamic_fee: ", dynamic_fee);
                log.add("cashInAmount: ", cashInAmount);
                long fee = 0;
                Misc.Cash cash = Misc.calculateAmount(cashInAmount, static_fee, dynamic_fee, 1, tranType, log);

                fee = cash.bankNetAmountLocked - cashInAmount;
                long transfee = jsonInfo.getInteger(MOMOFEE, 0);
                jsonReply.putNumber("totalfee", fee);
                jsonReply.putNumber("transfee", transfee);
                callback.handle(jsonReply);
                log.writeLog();
            }
        });
    }


//    public static void calculateTestFee(final JsonObject jsonInfo, final Common.BuildLog log, final Handler<JsonObject> callback)
//    {
//
//        final JsonObject jsonReply = new JsonObject();
//        String bankId = jsonInfo.getString("bankid", "");
//        final long cashInAmount = jsonInfo.getLong("amount", 0);
//        final int tranType = jsonInfo.getInteger("tranType", 0);
//
//        final Common.ServiceReq serviceReq = new Common.ServiceReq();
//        serviceReq.Command      = Common.ServiceReq.COMMAND.GET_FEE;
//        serviceReq.bankId       = bankId;
//        serviceReq.channel = 0;
//        serviceReq.inoutCity = 0;
//        serviceReq.tranType = 0;
//        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> message) {
//                FeeDb.Obj obj = null;
//                if (message.body() != null) {
//                    obj = new FeeDb.Obj(message.body());
//                }
//                int static_fee;
//                double dynamic_fee;
//
//                if (obj == null) {
//                    static_fee = 1100;
//                    dynamic_fee = 1.2;
//                } else {
//                    static_fee = obj.STATIC_FEE;
//                    dynamic_fee = obj.DYNAMIC_FEE;
//                }
//
//                log.add("static_fee: ", static_fee);
//                log.add("dynamic_fee: ", dynamic_fee);
//                log.add("cashInAmount: ", cashInAmount);
//                long fee = 0;
//                Misc.Cash cash = Misc.calculateAmount(cashInAmount, static_fee, dynamic_fee, 1, tranType, log);
//
//                fee = cash.bankNetAmountLocked - cashInAmount;
//                long transfee = jsonInfo.getInteger("fee", 0);
//                jsonReply.putNumber("totalfee", fee);
//                jsonReply.putNumber("transfee", transfee);
//                callback.handle(jsonReply);
//                log.writeLog();
//            }
//        });
//    }
    //END 0000000007 GET FEE FROM BANK
}
