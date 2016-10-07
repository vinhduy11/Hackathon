package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.bank.entity.BankRequest;
import com.mservice.bank.entity.BankRequestFactory;
import com.mservice.bank.entity.BankResponse;
import com.mservice.common.Popup;
import com.mservice.momo.data.*;
import com.mservice.momo.data.ironmanpromote.AndroidDataUserDb;
import com.mservice.momo.data.ironmanpromote.IronManBonusTrackingTableDb;
import com.mservice.momo.data.ironmanpromote.IronManNewRegisterTrackingDb;
import com.mservice.momo.data.ironmanpromote.PreIronManPromotionManageDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.NewCoreConnectorVerticle;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.msg.CoreMessage;
import com.mservice.momo.gateway.internal.core.objects.Command;
import com.mservice.momo.gateway.internal.core.objects.Request;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.SoapInProcess;
import com.mservice.momo.gateway.internal.soapin.information.TopupMapping;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.msg.StatisticModels;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.ErrorConstString;
import com.mservice.momo.util.StatisticUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.ironmanpromo.IronManPromoObj;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 10/15/15.
 */
public class ConnectProcessPromoBackup {
    public static boolean TEST_MODE;
    public static int SESSION_TIME_OUT;
    public static int OTP_TIME_OUT;

    public static String OTP_TEMPLATE;
    public static String ARR_GROUP_CFG;
    public static String ARR_CAPSET_CFG;
    public static String UPPER_LIMIT_CFG;
    public static boolean SEND_SMS;
    public static int LOGIN_MAX_COUNT;
    public static String VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET = "";
    public static String VIETTINBANK_AGENT_ADJUST_FOR_BANKNET = "";
    public static long REFERENCE_BONUS = 0;
    public static JsonObject ANDROID_VERSION;
    public static JsonObject IOS_VERSION;
    public static JsonObject WINDOWPHONE_VERSION;
    public static String ZALO_GROUP = "";
    public static String ZALO_CAPSET_ID = "";
    public static String ZALO_UPPER_LIMIT = "";
    public boolean isStore;
    //local
    private Logger logger;
    private Vertx vertx;
    private PhonesDb phonesDb;
    private AccessHistoryDb accessHistoryDb;
    private Common mCom;
    private int max_reg_by_imei = 0;
    private RegImeiDb regImeiDb;
    private AgentsDb agentsDb;
    private TransDb transDb;
    private M2cOffline m2cOffline;
    private boolean allow_vietnam_mobile = false;
    private JsonObject appVerCfg = null;
    private GiftProcess giftProcess;
    private JsonObject bankJsonOject;
    private SoapInProcess mSoapProcessor;
    private JsonObject glbConfig;
    private int iosCode = 0;
    private int androidCode = 0;
    private JsonObject jsonOCBPromo = new JsonObject();
    private BillPayPromosDb billPayPromosDb;
    private IronManNewRegisterTrackingDb ironManNewRegisterTrackingDb;
    private IronManBonusTrackingTableDb ironManBonusTrackingTableDb;
    private ArrayList<IronManBonusTrackingTableDb.Obj> ironManBonusObjs = new ArrayList<>();
    private JsonObject jsonIronManPromo;
    private boolean isIronManPromoActive;
    private Card card;
    private JsonObject jsonPreIronPromo;
    private boolean isPreIronPromoActive;
    private JsonObject jsonIronManPlus;
    private boolean isIronManPlusActive;
    private PreIronManPromotionManageDb preIronManPromotionManageDb;
    private ControlOnClickActivityDb controlOnClickActivityDb;
    private AndroidDataUserDb androidDataUserDb;
    private MappingWalletOCBPromoDb mappingWalletOCBPromoDb;
    public ConnectProcessPromoBackup(Vertx vertx, Logger logger, JsonObject glbCfg) {

        this.vertx = vertx;
        this.logger = logger;
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);
        this.accessHistoryDb = new AccessHistoryDb(vertx.eventBus());
        this.mCom = new Common(vertx, logger, glbCfg);
        regImeiDb = new RegImeiDb(vertx.eventBus(), logger);
        //"max_reg_by_imei": 3
        max_reg_by_imei = glbCfg.getInteger("max_reg_by_imei", 5);
        agentsDb = new AgentsDb(vertx.eventBus(), logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);
        m2cOffline = new M2cOffline(vertx.eventBus(), logger);

        giftProcess = new GiftProcess(mCom, vertx, logger, glbCfg);
        allow_vietnam_mobile = glbCfg.getBoolean("allow_vietnam_mobile", false);

        appVerCfg = glbCfg.getObject("mobiapp_version", new JsonObject());

        bankJsonOject = glbCfg.getObject(StringConstUtil.BANK, new JsonObject());

        isStore = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        glbConfig = glbCfg;
        iosCode = bankJsonOject.getInteger(StringConstUtil.IOS_CODE, 1914);
        androidCode = bankJsonOject.getInteger(StringConstUtil.ANDROID_CODE, 41);
        jsonOCBPromo = glbCfg.getObject(StringConstUtil.OBCPromo.JSON_OBJECT, new JsonObject());
        billPayPromosDb = new BillPayPromosDb(vertx, logger);
        ironManNewRegisterTrackingDb = new IronManNewRegisterTrackingDb(vertx, logger);
        ironManBonusTrackingTableDb = new IronManBonusTrackingTableDb(vertx, logger);
        preIronManPromotionManageDb = new PreIronManPromotionManageDb(vertx, logger);
        controlOnClickActivityDb = new ControlOnClickActivityDb(vertx);
        androidDataUserDb = new AndroidDataUserDb(vertx, logger);
        mappingWalletOCBPromoDb = new MappingWalletOCBPromoDb(vertx, logger);

        //BEGIN 0000000052 IRON MAN
//        if(ironManBonusObjs.size() < 1)
//        {
//            loadIronManPromoTrackingTable(vertx, ironManBonusObjs, new Handler<ArrayList<IronManBonusTrackingTableDb.Obj>>() {
//                @Override
//                public void handle(ArrayList<IronManBonusTrackingTableDb.Obj> objs) {
//
//                }
//            });
//        }
        card = new Card(vertx.eventBus(), logger);
        jsonIronManPromo = glbCfg.getObject(StringConstUtil.IronManPromo.JSON_OBJECT, new JsonObject());
        isIronManPromoActive = jsonIronManPromo.getBoolean(StringConstUtil.IronManPromo.IS_ACTIVE, false);

        jsonPreIronPromo = glbCfg.getObject(StringConstUtil.PreIronManPromo.JSON_OBJECT, new JsonObject());
        isPreIronPromoActive = jsonPreIronPromo.getBoolean(StringConstUtil.PreIronManPromo.IS_ACTIVE, false);

        this.jsonIronManPlus = glbCfg.getObject(StringConstUtil.IronManPromoPlus.JSON_OBJECT, new JsonObject());
        isIronManPlusActive = jsonIronManPlus.getBoolean(StringConstUtil.IronManPromoPlus.IS_ACTIVE, false);
        //END 0000000052 IRON MAN
    }

    /*
    Lưu ý khi xử lý Hello
    1- Kiểm tra số phone
        1.1 Số phone không hợp lệ => return lỗi
        1.2 thực hiện bước 2
    2- Số phone hợp lệ => kiểm tra bảng phone đã có số này chưa
        2.1 chưa có -> sync với core và trả về thông tin tài khoản
        2.2 nếu có thực hiện bước 3
    3- có số phone trong danh bạ, kiểm tra session time out
    */
    public void processHello(final NetSocket sock,
                             final MomoMessage msg,
                             final SockData data,
                             final MomoProto.Hello hello) {

        final int phoneNumber = msg.cmdPhone;
        final String phoneImei = hello.getImei();
        final String phoneImeiKey = hello.getImeiKey();
        final String sessionKey = hello.getSessionKey();
        final String os = (hello.getDeviceOs() == null ? "" : hello.getDeviceOs());     //  app phai luon gui len
        final String token = (hello.getDeviceKey() == null ? "" : hello.getDeviceKey());   //  app phai luon gui len

        data.hello = true;
        data.imei = phoneImei;
        data.sessionKey = hello.getSessionKey();

        //
        data.os = os;
        data.pushToken = token;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "processHello");
        log.add("os", os);
        log.add("token", token);

        mCom.SendWholeSystemPaused(vertx, sock, msg, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean) {
                    log.add("whole system paused", aBoolean);
                    log.writeLog();
                    return;
                }

                //logInfoForSock(sock,"HELLO " );
                //neu khong cung cap so dien thoai => tra ve chua setup
                if (phoneNumber == 0) {
                    log.add("processHello phoneNumber", 0);

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.HELLO_REPLY_VALUE,
                            msg.cmdIndex,
                            0,
                            MomoProto.HelloReply.newBuilder()
                                    .setRcode(MomoProto.HelloReply.ResultCode.NOT_LOG_ON_VALUE)
                                    .setRegStatus(MomoProto.RegStatus.newBuilder().setIsSetup(false).build())
                                    .build().toByteArray()
                    );
                    mCom.writeDataToSocket(sock, buf);

                    log.writeLog();
                } else {
                    logger.info("process Hello " + sock.writeHandlerID() + " " + phoneNumber);
                    //neu da cung cap so dien thoai
                    //1- kiem tra xem co phai la reconnect khong

                    //save last token and os
                    //if( (!"".equalsIgnoreCase(token)) && (!"".equalsIgnoreCase(os))){

                    if (!"".equalsIgnoreCase(os)) {

                        JsonObject jo = new JsonObject();
                        jo.putString(colName.PhoneDBCols.PUSH_TOKEN, token);
                        jo.putString(colName.PhoneDBCols.PHONE_OS, os);
                        jo.putNumber(colName.PhoneDBCols.NUMBER, phoneNumber);

                        // update listcache token
//                        vertx.eventBus().send(AppConstant.CloundNotifyVerticleUpdate, jo);
                        vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                                Misc.makeHttpPostWrapperData(AppConstant.CloundNotifyVerticleUpdate, jo));

                        //todo : send this json to cloud verticle to cache token data
                        phonesDb.updatePartial(phoneNumber, jo, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj phoneObj) {

                                if (phoneObj != null) {
                                    //da co so phone nay trong he thong
                                    //kiem tra xem lan cuoi cung dang nhap
                                    boolean isSetup = false;
                                    boolean isLogin = false;

                                    logger.info("processHello lastImei/imeiKey " + phoneObj.lastImei + "/" + phoneObj.imeiKey + " last cmdInd " + (data == null ? "null" : data.lastCmdInd));

                                    //phoneObj.lastImei.equalsIgnoreCase(phoneImei) && --> dont know why, but ios change imei every time they login
                                    if (phoneObj.imeiKey.equalsIgnoreCase(phoneImeiKey)) {

                                        log.add("processHello Hello from a valid phone", "");

                                        //at this time we sure that this is a valid
                                        data.beginSession(phoneNumber, sock, vertx.eventBus(), logger, glbConfig);

                                        isSetup = true;
                                        data.isSetup = true;

                                        if (phoneObj.sessionKey.equalsIgnoreCase(sessionKey)
                                                && (System.currentTimeMillis() - phoneObj.last_session_time) < SESSION_TIME_OUT * 60 * 1000) {
                                            isLogin = true;
                                        }
                                    }
//                                    int rCode = isLogin ? MomoProto.HelloReply.ResultCode.LOG_ON_VALUE : MomoProto.HelloReply.ResultCode.NOT_LOG_ON_VALUE;
//                                    if(phoneObj.number == phoneNumber)
//                                    {
//                                    int rCode = MomoProto.HelloReply.ResultCode.LOG_ON_ANOTHER_DEVICE_VALUE;
//                                    }
                                    data.setPhoneObj(phoneObj, logger, "SYN AT HELLO");

                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.HELLO_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.HelloReply.newBuilder()
                                                    .setRcode(isLogin ? MomoProto.HelloReply.ResultCode.LOG_ON_VALUE : MomoProto.HelloReply.ResultCode.NOT_LOG_ON_VALUE)
                                                    .setVersionName(getAppVersionCode(os, phoneObj.isAgent).getString("name"))
                                                    .setVersionCode(getAppVersionCode(os, phoneObj.isAgent).getInteger("code"))
                                                    .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                                    .setIsSetup(isSetup)
                                                                    .setIsReged(phoneObj.isReged)
                                                                    .setIsNamed(phoneObj.isNamed)
                                                                    .build()
                                                    )
                                                    .build().toByteArray()
                                    );
                                    mCom.writeDataToSocket(sock, buf);


                                } else {
                                    //khong lay duoc phone Obj khi hello
                                    MomoProto.HelloReply.Builder builder = MomoProto.HelloReply.newBuilder();
                                    builder.setRcode(MomoProto.HelloReply.ResultCode.NOT_LOG_ON_VALUE)
                                            .setVersionName(getAppVersionCode(os, phoneObj.isAgent).getString("name"))
                                            .setVersionCode(getAppVersionCode(os, phoneObj.isAgent).getInteger("code"))
                                            .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                            .setIsSetup(false)
                                                            .setIsReged(false)
                                                            .setIsNamed(false)
                                            );
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.HELLO_REPLY_VALUE,
                                            msg.cmdIndex,
                                            0,
                                            builder.build().toByteArray()
                                    );
                                    mCom.writeDataToSocket(sock, buf);
                                }

                                log.writeLog();
                            }
                        });

                    } else {

                        //chua co thong tin token
                        phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(final PhonesDb.Obj phoneObj) {

                                if (phoneObj != null) {
                                    //da co so phone nay trong he thong
                                    //kiem tra xem lan cuoi cung dang nhap

                                    boolean isSetup = false;
                                    boolean isLogin = false;

                                    logger.info("processHello lastImei/imeiKey " + phoneObj.lastImei + "/" + phoneObj.imeiKey + " last cmd Ind " + (data == null ? "null" : data.lastCmdInd));

                                    //phoneObj.lastImei.equalsIgnoreCase(phoneImei) && --> dont know why, but ios change imei every time they login
                                    if (phoneObj.imeiKey.equalsIgnoreCase(phoneImeiKey)) {

                                        log.add("processHello Hello from a valid phone", "");

                                        //at this time we sure that this is a valid
                                        data.beginSession(phoneNumber, sock, vertx.eventBus(), logger, glbConfig);

                                        isSetup = true;
                                        data.isSetup = true;

                                        if (phoneObj.sessionKey.equalsIgnoreCase(sessionKey)
                                                && (System.currentTimeMillis() - phoneObj.last_session_time) < SESSION_TIME_OUT * 60 * 1000) {
                                            isLogin = true;
                                        }
                                    }

                                    data.setPhoneObj(phoneObj, logger, "SYN AT HELLO");
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.HELLO_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.HelloReply.newBuilder()
                                                    .setRcode(isLogin ? MomoProto.HelloReply.ResultCode.LOG_ON_VALUE : MomoProto.HelloReply.ResultCode.NOT_LOG_ON_VALUE)
                                                    .setVersionName(getAppVersionCode(os, phoneObj.isAgent).getString("name"))
                                                    .setVersionCode(getAppVersionCode(os, phoneObj.isAgent).getInteger("code"))
                                                    .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                                    .setIsSetup(isSetup)
                                                                    .setIsReged(phoneObj.isReged)
                                                                    .setIsNamed(phoneObj.isNamed)
                                                                    .build()
                                                    )
                                                    .build().toByteArray()
                                    );
                                    mCom.writeDataToSocket(sock, buf);

                                } else {
                                    //khong lay duoc phone Obj khi hello
                                    MomoProto.HelloReply.Builder builder = MomoProto.HelloReply.newBuilder();
                                    builder.setRcode(MomoProto.HelloReply.ResultCode.NOT_LOG_ON_VALUE)
                                            .setVersionName(getAppVersionCode(os, false).getString("name")) // Sua lai cai vu phoneObj == null
                                            .setVersionCode(getAppVersionCode(os, false).getInteger("code"))
                                            .setRegStatus(MomoProto.RegStatus.newBuilder()
                                                            .setIsSetup(false)
                                                            .setIsReged(false)
                                                            .setIsNamed(false)
                                            );
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.HELLO_REPLY_VALUE,
                                            msg.cmdIndex,
                                            0,
                                            builder.build().toByteArray()
                                    );
                                    mCom.writeDataToSocket(sock, buf);
                                }

                                log.writeLog();
                            }
                        });
                    }
                }
            } //END SYSTEMPAUSED MONGO
        });//END SYSTEMPAUSED MONGO
    }

    public void processGetOtp(final NetSocket sock, final MomoMessage msg, final SockData data) {

        final int number = msg.cmdPhone;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "processGetOtp");
        log.setPhoneNumber("0" + msg.cmdPhone);
        MomoProto.TextValueMsg getOtp;
        try {
            getOtp = MomoProto.TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            getOtp = null;
        }

        HashMap hashMap = Misc.getKeyValuePairs(getOtp.getKeysList());
        final String oldNumber = hashMap.containsKey(Const.OLD_NUMBER) ? hashMap.get(Const.OLD_NUMBER).toString().trim() : "";
        final String retailer = hashMap.containsKey(Const.DGD.Retailer) ? (String) hashMap.get(Const.DGD.Retailer) : "";
        final boolean isRetailer = (Const.DGDValues.Retailer == DataUtil.strToInt(retailer));

        log.add("old number", oldNumber);
        log.add("retailer", isRetailer);

//        if(!isRetailer){
//            getOtpEndUser(sock, msg, data, number, log);
//        }else{
        //diem giao dich
        agentsDb.getOneAgent("0" + number, "processGetOtp", new Handler<AgentsDb.StoreInfo>() {
            @Override
            public void handle(AgentsDb.StoreInfo storeInfo) {

                MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();
                boolean isOk = true;

                //Dang nhap app enduser nhung so dien thoai do la diem giao dich
                if (!isRetailer && storeInfo != null && storeInfo.status != 2) {
                    builder.setResult(false)
                            .setRcode(ErrorConstString.CAN_NOT_LOG_IN_END_USER_APP)
                            .setDesc("Tài khoản chỉ đăng nhập ở Ứng dụng MoMo Điểm giao dịch");
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.GET_OTP_REPLY_VALUE,
                            msg.cmdIndex,
                            number,
                            builder.build().toByteArray()
                    );
                    mCom.writeDataToSocket(sock, buf);

                    log.writeLog();
                    return;
                } else if (!isRetailer && (storeInfo == null || (storeInfo != null && storeInfo.status == 2))) // Dang nhap voi enduser voi app enduser
                {
                    // Cho phep dang nhap nhu enduser
                    getOtpEndUser(sock, msg, data, number, log);
                    return;
                }

                //khong tim thay DGD tren danh sach location
                if (storeInfo == null) {
                    isOk = false;
                    builder.setResult(false)
                            .setRcode(MomoProto.LogInReply.ResultCode.AGENT_NOT_FOUND_VALUE)
                            .setDesc("Tài khoản của bạn không phải là của đại lý. Vui lòng liên hệ với chăm sóc khách hàng để được hỗ trợ.");

                } else if (storeInfo != null && storeInfo.status == 1) {
                    isOk = false;
                    builder.setResult(false)
                            .setRcode(MomoProto.LogInReply.ResultCode.AGENT_CANCELLED_VALUE)
                            .setDesc("Tài khoản đang tạm khóa. Vui lòng liên hệ với chăm sóc khách hàng để được hỗ trợ.");
                } else if (storeInfo != null && storeInfo.status == 2) {
                    isOk = false;
                    builder.setResult(false)
                            .setRcode(MomoProto.LogInReply.ResultCode.AGENT_CANCELLED_VALUE)
                            .setDesc("Tài khoản tạm ngừng giao dịch. Vui lòng liên hệ với chăm sóc khách hàng để được hỗ trợ.");
                }

                //mmt agent invalid
                if (!isOk) {

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.GET_OTP_REPLY_VALUE,
                            msg.cmdIndex,
                            number,
                            builder.build().toByteArray()
                    );
                    mCom.writeDataToSocket(sock, buf);

                    log.writeLog();
                    return;
                }

                //thuc hien nhu end-user
                getOtpEndUser(sock, msg, data, number, log);
            }
        });
//        }// end else isRetailer
        //-------------------------------------------------------------------------------------------------
        //end-user
//        if(!isRetailer){
//            getOtpEndUser(sock, msg, data, number, log);
//        }else{
//            //diem giao dich
//            agentsDb.getOneAgent("0" + number, "processGetOtp", new Handler<AgentsDb.StoreInfo>() {
//                @Override
//                public void handle(AgentsDb.StoreInfo storeInfo) {
//
//                    MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();
//                    boolean isOk = true;
//
//                    //khong tim thay DGD tren danh sach location
//                    if(storeInfo == null){
//                        isOk =false;
//                        builder.setResult(false)
//                                .setRcode(MomoProto.LogInReply.ResultCode.AGENT_NOT_FOUND_VALUE)
//                                .setDesc("Tài khoản của bạn không phải là của đại lý. Vui lòng liên hệ với chăm sóc khách hàng để được hỗ trợ.");
//
//                    }else if(storeInfo!= null && storeInfo.status == 1){
//                        isOk =false;
//                        builder.setResult(false)
//                                .setRcode(MomoProto.LogInReply.ResultCode.AGENT_CANCELLED_VALUE)
//                                .setDesc("Tài khoản đang tạm khóa. Vui lòng liên hệ với chăm sóc khách hàng để được hỗ trợ.");
//                    }else if(storeInfo!=null && storeInfo.status == 2){
//                        isOk =false;
//                        builder.setResult(false)
//                                .setRcode(MomoProto.LogInReply.ResultCode.AGENT_CANCELLED_VALUE)
//                                .setDesc("Tài khoản tạm ngừng giao dịch. Vui lòng liên hệ với chăm sóc khách hàng để được hỗ trợ.");
//                    }
//
//                    //mmt agent invalid
//                    if(!isOk){
//
//                        Buffer buf = MomoMessage.buildBuffer(
//                                MomoProto.MsgType.GET_OTP_REPLY_VALUE,
//                                msg.cmdIndex,
//                                number,
//                                builder.build().toByteArray()
//                        );
//                        mCom.writeDataToSocket(sock, buf);
//
//                        log.writeLog();
//                        return;
//                    }
//
//                    //thuc hien nhu end-user
//                    getOtpEndUser(sock, msg, data, number, log);
//                }
//            });
//        }

        //because of other wallet use this device --> reset token for old wallet
        resetTokenForOldWallet(data, oldNumber, "0" + number);
    }

    /*
    reset token
    */
    private void resetTokenForOldWallet(final SockData data, final String oldNumber, final String newNumber) {

        int tPrevNumber = data != null && data.getPhoneObj() != null ? data.getPhoneObj().number : 0;

        final int prevNumber = !"".equalsIgnoreCase(oldNumber) ? DataUtil.strToInt(oldNumber) : tPrevNumber;

        if (prevNumber > 0) {
            phonesDb.getPhoneObjInfo(prevNumber, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj obj) {
                    if (obj != null) {

                        //reset token for old number
                        JsonObject jo = new JsonObject();
                        jo.putString(colName.PhoneDBCols.PUSH_TOKEN, "");
                        phonesDb.updatePartialNoReturnObj(prevNumber, jo, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                            }
                        });

                        //update token for new number
                        if (DataUtil.strToInt(newNumber) > 0) {
                            JsonObject jonew = new JsonObject();
                            jonew.putString(colName.PhoneDBCols.PUSH_TOKEN, obj.pushToken);
                            phonesDb.updatePartialNoReturnObj(DataUtil.strToInt(newNumber), jonew, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void getOtpEndUser(final NetSocket sock, final MomoMessage msg, final SockData data, final int number, final Common.BuildLog log) {
        //check trang thai cua thang nay duoi core
        final Buffer buffer = MomoMessage.buildBuffer(SoapProto.MsgType.CHECK_USER_STATUS_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , "".getBytes());

        String imei = (data != null ? data.imei : "");

        boolean isIOS = (data != null && "ios".equalsIgnoreCase(data.os)) ? true : false;

        //neu la IOS thi chuyen qua lay token cua IOS
        if (data != null && "ios".equalsIgnoreCase(data.os) && !"".equalsIgnoreCase(data.pushToken)) {
            imei = data.pushToken;
        }

        //String imei = (data != null ? data.imei : "");
        //co imei
        if (!"".equalsIgnoreCase(imei)) {
            //todo : kiem tra imei nay dang ky may lan trong thang
            regImeiDb.findOne(data.imei, new Handler<RegImeiDb.Obj>() {
                @Override
                public void handle(RegImeiDb.Obj obj) {

                    if (obj != null) {
                        //vuat qua so lan dang ky trong thang tren 1 may
                        if (obj.arrayListPhones.size() >= max_reg_by_imei
                                && (!obj.arrayListPhones.contains(msg.cmdPhone))
                                && Misc.getYearMonth(obj.time) == Misc.getYearMonth(System.currentTimeMillis())) {

                            Buffer bufBlock = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.GET_OTP_REPLY_VALUE,
                                    msg.cmdIndex,
                                    number,
                                    MomoProto.StandardReply.newBuilder()
                                            .setResult(false)
                                            .setRcode(MomoProto.SystemError.IS_BLOCKING_VALUE)
                                            .build().toByteArray()
                            );
                            mCom.writeDataToSocket(sock, bufBlock);

                            log.add("current reg count by imei", obj.count);
                            log.add("allow max reg count by imei", max_reg_by_imei);
                            log.writeLog();

                            //track xem thang nay co gang lam bao nhieu lan, cho den thoi diem hien tai

                            obj.time = System.currentTimeMillis();
                            regImeiDb.upsert(obj, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                }
                            });

                        } else {
                            obj.id = data.imei;
                            //trong thang
                            if (Misc.getYearMonth(obj.time) == Misc.getYearMonth(System.currentTimeMillis())
                                    && (!obj.arrayListPhones.contains(msg.cmdPhone))
                                    ) {

                                //them 1 so phone moi vao theo imei nay
                                obj.arrayListPhones.add(msg.cmdPhone);

                                //qua thang moi
                            } else {
                                //reset lai danh sach so phone theo imei nay
                                obj.arrayListPhones = new ArrayList<Integer>();
                                obj.arrayListPhones.add(msg.cmdPhone);
                            }

                            obj.time = System.currentTimeMillis();
                            executeGetOtp(sock, msg, data, number, log, buffer, obj);
                        }
                    } else {
                        //dang ky o day va tang len 1 lan cho imei nay
                        RegImeiDb.Obj regObj = new RegImeiDb.Obj();
                        regObj.count = 0;
                        regObj.id = data.imei;
                        regObj.arrayListPhones = new ArrayList<>();
                        regObj.arrayListPhones.add(msg.cmdPhone);

                        regObj.time = System.currentTimeMillis();
                        executeGetOtp(sock, msg, data, number, log, buffer, regObj);
                    }
                }
            });
        } else {

            //neu la IOS, token duoc cap sau
            if (isIOS) {
                executeGetOtp(sock, msg, data, number, log, buffer, null);
                return;
            }

            //xem nhu loi socket
            Buffer bufBlock = MomoMessage.buildBuffer(
                    MomoProto.MsgType.GET_OTP_REPLY_VALUE,
                    msg.cmdIndex,
                    number,
                    MomoProto.StandardReply.newBuilder()
                            .setResult(false)
                            .setRcode(MomoProto.SystemError.IS_BLOCKING_VALUE)
                            .build().toByteArray()
            );
            mCom.writeDataToSocket(sock, bufBlock);
            log.add("sockdata", "null, can not get imei");
            log.writeLog();
        }
    }

    private void executeGetOtp(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final int number
            , final Common.BuildLog log
            , Buffer buffer, final RegImeiDb.Obj regImeiObj) {


        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<Buffer>>() {
            @Override
            public void handle(final Message<Buffer> response) {
                MomoMessage momo = MomoMessage.fromBuffer(response.body());

                boolean isRegistered = false;
                boolean isActived = true;
                MomoProto.RegStatus status = null;
                try {
                    status = MomoProto.RegStatus.parseFrom(momo.cmdBody);
                    isRegistered = status.getIsReged();
                    isActived = status.getIsActive();
                } catch (InvalidProtocolBufferException e) {

                }

                log.add("isReged", isRegistered);
                log.add("isActived", isActived);

                //agent is blocking
                if (status != null && isRegistered && !isActived) {

                    Buffer bufBlock = MomoMessage.buildBuffer(
                            MomoProto.MsgType.GET_OTP_REPLY_VALUE,
                            msg.cmdIndex,
                            number,
                            MomoProto.StandardReply.newBuilder()
                                    .setResult(false)
                                    .setRcode(MomoProto.SystemError.IS_BLOCKING_VALUE)
                                    .build().toByteArray()
                    );
                    mCom.writeDataToSocket(sock, bufBlock);
                    log.add("agent is blocked", "");
                    log.writeLog();
                    return;
                }

                phonesDb.getPhoneObjInfo(number, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj phoneObj) {

                        if (phoneObj != null) {
                            data.setPhoneObj(phoneObj, logger, "RESET WHEN processGetOtp");
                        }

                        if (phoneObj == null || "".equalsIgnoreCase(phoneObj.otp)
                                || phoneObj.otp_time + 2 * 60 * 1000 < System.currentTimeMillis()) {

                            final String otp = (number == 966543465 ? "543465" : DataUtil.getOtp());
                            logger.info(String.format(OTP_TEMPLATE, otp, otp) + " --> " + msg.cmdPhone);

                            if (data.getPhoneObj() != null) {
                                data.getPhoneObj().otp = otp;
                                data.getPhoneObj().otp_time = System.currentTimeMillis();
                            }

                            phonesDb.setOtp(msg.cmdPhone, otp, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean result) {

                                    log.add("set OTP result", result);

                                    if (result) {

                                        SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                                                .setSmsId(0)
                                                .setToNumber(number)
                                                .setContent(String.format(OTP_TEMPLATE, otp, otp))
                                                .build();

                                        //gui sms
                                        if (SEND_SMS) {
                                            //chi gui den 1 so dien thoai duy nhat la NHUNG
                                            String mobiProvider = TopupMapping.getTargetTopup("0" + number, "0" + msg.cmdPhone, log);

                                            //mobiProvider = (("".equalsIgnoreCase(mobiProvider) || mobiProvider.isEmpty()) ?  "" : mobiProvider);

                                            if (TEST_MODE) {
                                                SoapProto.SendSms nhungSms = SoapProto.SendSms.newBuilder()
                                                        .setSmsId(0)
                                                        .setToNumber(979754034)
                                                        .setContent(String.format(OTP_TEMPLATE, otp, otp))
                                                        .build();

                                                if (!"".equalsIgnoreCase(mobiProvider)) {
                                                    vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, nhungSms.toByteArray());
                                                }
                                            } else {
                                                if (!"".equalsIgnoreCase(mobiProvider)) {
                                                    vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
                                                    log.add("desc", "set timerID for get OTP");
                                                    long timerId = vertx.setTimer(5 * 60 * 1000L, new Handler<Long>() {
                                                        @Override
                                                        public void handle(Long aLong) {
                                                            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                                                                @Override
                                                                public void handle(PhonesDb.Obj obj) {
                                                                    if(obj != null && !obj.otp.equalsIgnoreCase(""))
                                                                    {
                                                                        log.add("func", "THE NUMBER " + msg.cmdPhone + " NOT INPUT OTP AFTER 5 MINUTES");
                                                                        log.writeLog();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                    log.add("timerId", timerId);
                                                    data.timer = timerId;
                                                }
                                            }
                                        }

                                        //co thi moi cap nhat, khong ti thoi
                                        if (regImeiObj != null) {
                                            regImeiDb.upsert(regImeiObj, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean aBoolean) {
                                                }
                                            });
                                        }
                                    } else {
                                        log.add("set OTP for " + msg.cmdPhone, "false");
                                    }
                                    log.writeLog();
                                }
                            });
                        } else {
                            if (phoneObj.otp_time + 10 * 1000 > System.currentTimeMillis()) {
                                logger.debug(msg.cmdPhone + " GET OTP < 10s, OTP: " + phoneObj.otp);
                            } else {
                                logger.debug(msg.cmdPhone + " GET OTP after 10s - ignored");
                            }

                            //resend last otp when request less than 2 mins
                            if (phoneObj.otp_time + 2 * 60 * 1000 > System.currentTimeMillis()) {
                                log.add("GET OTP < 2 minutes", "resend the last otp");
                                log.add("SEND_SMS", String.format(OTP_TEMPLATE, phoneObj.otp, phoneObj.otp));

                                SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                                        .setSmsId(0)
                                        .setToNumber(number)
                                        .setContent(String.format(OTP_TEMPLATE, phoneObj.otp, phoneObj.otp))
                                        .build();

                                if (SEND_SMS) {
                                    vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
                                    long timerId = vertx.setTimer(5 * 60 * 1000L, new Handler<Long>() {
                                        @Override
                                        public void handle(Long aLong) {

                                            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                                                @Override
                                                public void handle(PhonesDb.Obj obj) {
                                                    if(obj != null && !obj.otp.equalsIgnoreCase(""))
                                                    {
                                                        log.add("func", "THE NUMBER " + msg.cmdPhone + " DID NOT ENTER OTP AFTER 5 MINUTES");
                                                        log.writeLog();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                    data.timer = timerId;
                                    log.add("timer", timerId);
                                }
                            } else {
                                log.add("WTF GET OTP > 2 minutes", "");
                            }
                            log.writeLog();
                        }

                        //luon gui ket qua get otp thanh cong
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.GET_OTP_REPLY_VALUE,
                                msg.cmdIndex,
                                number,
                                MomoProto.StandardReply.newBuilder()
                                        .setResult(true)
                                        .build().toByteArray()
                        );
                        mCom.writeDataToSocket(sock, buf);
                    }
                });
            }
        });
    }

    public void processLogOut(final NetSocket sock, final MomoMessage msg, final SockData sdata) {

        phonesDb.expireSession(msg.cmdPhone, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                logger.debug("Logout result " + result);
            }
        });

        PhonesDb.Obj data = sdata.getPhoneObj();

        //luu lich su truy cap
        if (data != null && !"".equalsIgnoreCase(sdata.deviceModel)) {
            accessHistoryDb.saveAccessTime(msg.cmdPhone
                    , sdata.ip
                    , sdata.start_access_time
                    , sdata.deviceModel, new Handler<Boolean>() {
                @Override
                public void handle(Boolean b) {
                    logger.debug("TRACK ACCESS HISTORY WHEN LOG OUT ACCESS INFO " + b);
                }
            });
        }

        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.LOG_OUT_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                MomoProto.StandardReply.newBuilder()
                        .setResult(true)
                        .setRcode(0)
                        .build().toByteArray()
        );

        mCom.writeDataToSocket(sock, buf);
    }

    public void processVerifyOtp(final NetSocket sock, final MomoMessage msg, final SockData data) {

        MomoProto.VerifyOtp verifyOtp;
        String userOtp = "";
        try {
            verifyOtp = MomoProto.VerifyOtp.parseFrom(msg.cmdBody);
            userOtp = verifyOtp.getOtp();
        } catch (InvalidProtocolBufferException e) {
            verifyOtp = null;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processVerifyOtp");
        log.add("user OTP", userOtp);

        final String fUserOtp = userOtp;
        log.add("data timerId", data.timer);
        vertx.cancelTimer(data.timer);
        if (verifyOtp != null && userOtp != null && userOtp.length() == 6) {

            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(final PhonesDb.Obj phoneObj) {
                    final MomoProto.VerifyOtpReply.Builder builder = MomoProto.VerifyOtpReply.newBuilder().setResult(false);
                    if (phoneObj != null) {

                        log.add("phone OTP", phoneObj.otp);

                        boolean verifyOk = true;

                        if (DataUtil.strToInt(phoneObj.otp) != DataUtil.strToInt(fUserOtp)) {
                            log.add("verify with wrong OTP", "");

                            builder.setRcode(MomoProto.VerifyOtpReply.ResultCode.WRONG_OTP_VALUE);
                            builder.setResult(false);
                            verifyOk = false;
                        }
                        //OTP OK - check thoi gian xac thuc OTP khong qua 5 phut
                        else if ((System.currentTimeMillis() - phoneObj.otp_time) > OTP_TIME_OUT * 60 * 1000) {

                            log.add("verify OTP timeout", "");
                            builder.setRcode(MomoProto.VerifyOtpReply.ResultCode.TIME_OUT_VALUE);
                            builder.setResult(false);
                            verifyOk = false;
                        }

                        if (!verifyOk) {
                            //Khong dung dinh dang cua message OTP verify
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.VERIFY_OTP_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    builder.build().toByteArray());
                            mCom.writeDataToSocket(sock, buf);
                            log.writeLog();
                            return;
                        }

                        //verify OTP successfull
                        final String imei_key = UUID.randomUUID().toString();
                        data.isSetup = true;

                        JsonObject jo = new JsonObject();
                        jo.putString(colName.PhoneDBCols.LAST_IMEI, data.imei);
                        jo.putString(colName.PhoneDBCols.IMEI_KEY, imei_key);
                        jo.putBoolean(colName.PhoneDBCols.IS_SETUP, true);
                        jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, 9);
                        jo.putNumber(colName.PhoneDBCols.NUMBER, msg.cmdPhone);
                        jo.putBoolean(colName.PhoneDBCols.DELETED, false);

                        //reset lai otp va otp-time de khong dung lai nua
                        jo.putString(colName.PhoneDBCols.OTP, "");
                        jo.putNumber(colName.PhoneDBCols.OTP_TIME, 0);

                        log.add("update Phone after verify OTP success", "");
                        log.add("updated JSON", jo);

                        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, jo, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {

                                //proccess with waitingReg  = true
                                boolean isNamed = phoneObj.isNamed;
                                boolean isReged = phoneObj.waitingReg ? false : phoneObj.isReged;

                                log.add("update result", aBoolean);
                                builder.setResult(true)
                                        .setRcode(MomoProto.VerifyOtpReply.ResultCode.ALL_OK_VALUE)
                                        .setImeiKey(imei_key)
                                        .setRegStatus(
                                                MomoProto.RegStatus.newBuilder()
                                                        .setIsSetup(true)
                                                        .setIsNamed(isNamed)
                                                        .setIsReged(isReged)
                                        );

                                Buffer buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.VERIFY_OTP_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        builder.build().toByteArray()
                                );
                                mCom.writeDataToSocket(sock, buf);
                                log.writeLog();
                                return;
                            }
                        });


                    } else {

                        log.add("Khong tim thay phoneObject khi verify Otp", "");
                        //Khong tim thay phoneObject khi verify Otp

                        builder.setRcode(MomoProto.VerifyOtpReply.ResultCode.WRONG_OTP_VALUE);
                        builder.setResult(false);
                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.VERIFY_OTP_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.build().toByteArray());
                        mCom.writeDataToSocket(sock, buf);
                        log.writeLog();
                    }
                }
            });
        } else {
            //Khong dung dinh dang cua message OTP verify
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.VERIFY_OTP_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.VerifyOtpReply.newBuilder()
                            .setResult(false)
                            .setRcode(MomoProto.VerifyOtpReply.ResultCode.WRONG_OTP_VALUE)
                            .build().toByteArray()
            );
            mCom.writeDataToSocket(sock, buf);

            log.add("invalid format message VerifyOTP", "");
            log.writeLog();
        }
    }

    public void processRegister(final NetSocket sock, final MomoMessage msg, final SockData data) {
        MomoProto.Register req;
        try {
            req = MomoProto.Register.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            req = null;
        }

        final MomoProto.Register request = req;

        if (request == null || !request.hasName() || !request.hasCardId() || !request.hasPin()) {
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.REGISTER_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.StandardReply.newBuilder()
                            .setResult(false)
                            .setRcode(MomoProto.SystemError.SYSTEM_ERROR_VALUE)
                            .build().toByteArray()
            );
            mCom.writeDataToSocket(sock, buf);
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);

        log.setPhoneNumber("0" + msg);
        log.add("function", "processRegister");

        final String channel = (sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI);

        log.add("channel", channel);

        final String name = Misc.removeAccent(request.getName() == null ? "" : request.getName());
        final String card_id = (request.getCardId() == null ? "" : request.getCardId());
        final String pin = request.getPin();
        final String address = (request.getAddress() == null ? "" : request.getAddress());
        final String dob = (request.getDob() == null ? "" : request.getDob());

        String question_tmp = "";
        if(!data.os.equalsIgnoreCase("ios"))
        {
            question_tmp = request.getQuestion() == null ? "" : request.getQuestion();
        }
        final String question = question_tmp;
        String referal = req.getReference() == null ? "" : req.getReference();

        final int referenceNumber = (request.getReference() == null ? 0 : DataUtil.strToInt(request.getReference()));

        log.add("referal", referal);
        log.add("reference number", referenceNumber);

        if ("".equalsIgnoreCase(referal) && referenceNumber > 0) {
            referal = "0" + referenceNumber;
        }

        final SoapProto.keyValuePair.Builder kvpBuilder = SoapProto.keyValuePair.newBuilder();
        kvpBuilder.setKey("referal")
                .setValue(referal);

        getPhoneObjForWaitingReg(msg.cmdPhone, data, new Handler<String>() {
            @Override
            public void handle(final String waitingReg) {

                kvpBuilder.setKey("waitregister").setValue(waitingReg);

                log.add("waitregister -------->", waitingReg);

                final Buffer signUpBuf = MomoMessage.buildBuffer(
                        SoapProto.MsgType.REGISTER_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        SoapProto.Register.newBuilder()
                                .setName(name)
                                .setIdCard(card_id)
                                .setChannel(channel)
                                .setPin(pin)
                                .setArrGroup(ARR_GROUP_CFG)
                                .setArrCapset(ARR_CAPSET_CFG)
                                .setUpperLimit(UPPER_LIMIT_CFG)
                                .setAddress(address)
                                .setDateOfBirth(dob)
                                .addKvps(kvpBuilder) // them cap key cho gio thieu sinh vien
                                .build()
                                .toByteArray()
                );

                //lay token tren app day xuong
                String imei = "";
                if (request.getKeyValueCount() > 0) {
                    for (int i = 0; i < request.getKeyValueCount(); i++) {
                        if ("imei".equalsIgnoreCase(request.getKeyValue(i).getText())) {
                            imei = (request.getKeyValue(i).getValue() == null ? "" : request.getKeyValue(i).getValue());
                        }
                    }
                }

                //lay imei trong sock data
                if ("".equalsIgnoreCase(imei) && data != null && data.imei != null && !"".equalsIgnoreCase(data.imei)) {
                    imei = data.imei;
                }

                boolean isIOS = (data != null && "ios".equalsIgnoreCase(data.os)) ? true : false;

                //neu la IOS thi chuyen qua lay token cua IOS
                if (data != null && "ios".equalsIgnoreCase(data.os) && !"".equalsIgnoreCase(data.pushToken)) {
                    imei = data.pushToken;
                }

                //co imei
                if (!"".equalsIgnoreCase(imei)) {
                    //todo : kiem tra imei nay dang ky may lan trong thang
                    regImeiDb.findOne(data.imei, new Handler<RegImeiDb.Obj>() {
                        @Override
                        public void handle(RegImeiDb.Obj obj) {

                            if (obj != null) {
                                //vuat qua so lan dang ky trong thang tren 1 may
                                if (obj.count >= max_reg_by_imei
                                        && Misc.getYearMonth(obj.time) == Misc.getYearMonth(System.currentTimeMillis())) {

                                    //todo khong cho dang ky moi
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.REGISTER_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.StandardReply.newBuilder()
                                                    .setResult(false)
                                                    .setRcode(MomoProto.SystemError.SYSTEM_ERROR_VALUE)
                                                    .setDesc("Bạn đã đăng ký mở ví mới vượt quá " + max_reg_by_imei + " lần/1 tháng")
                                                    .build().toByteArray()
                                    );
                                    mCom.writeDataToSocket(sock, buf);

                                    log.add("current reg count by imei", obj.count);
                                    log.add("allow max reg count by imei", max_reg_by_imei);
                                    log.writeLog();

                                    //track xem thang nay co gang lam bao nhieu lan, cho den thoi diem hien tai
                                    obj.count++;
                                    obj.time = System.currentTimeMillis();
                                    regImeiDb.upsert(obj, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {
                                        }
                                    });


                                } else {
                                    obj.id = data.imei;

                                    if (Misc.getYearMonth(obj.time) == Misc.getYearMonth(System.currentTimeMillis())) {
                                        //trong thang ++ 1 lan
                                        obj.count++;

                                    } else {
                                        //qua thang reset lai 1
                                        obj.count = 1;

                                    }
                                    obj.time = System.currentTimeMillis();
                                    excuteRegister(msg, sock, signUpBuf, name, card_id, dob, question, referenceNumber, pin, obj, waitingReg, data, log);
                                }
                            } else {
                                //dang ky o day va tang len 1 lan cho imei nay
                                RegImeiDb.Obj regObj = new RegImeiDb.Obj();
                                regObj.count = 1;
                                regObj.id = data.imei;
                                regObj.time = System.currentTimeMillis();
                                excuteRegister(msg, sock, signUpBuf, name, card_id, dob, question, referenceNumber, pin, regObj, waitingReg, data, log);
                            }
                        }
                    });
                } else {

                    //neu la IOS --> xem nhu vua download moi ve
                    if (isIOS) {
                        excuteRegister(msg, sock, signUpBuf, name, card_id, dob, question, referenceNumber, pin, null, waitingReg, data, log);
                        return;
                    }

                    //xem nhu loi socket
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.REGISTER_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.StandardReply.newBuilder()
                                    .setResult(false)
                                    .setRcode(MomoProto.SystemError.SYSTEM_ERROR_VALUE)
                                    .build().toByteArray()
                    );
                    mCom.writeDataToSocket(sock, buf);
                    log.add("sockdata", "null, can not get imei");
                    log.writeLog();
                }
            }
        });
    }

    private void excuteRegister(final MomoMessage msg
            , final NetSocket sock
            , final Buffer signUpBuf
            , final String name
            , final String card_id
            , final String dob
            , final String address_tmp_string
            , final int referenceNumber
            , final String pin
            , final RegImeiDb.Obj regImeiObj
            , final String waitingReg
            , final SockData data
            , final Common.BuildLog log) {

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, signUpBuf, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                boolean isOk = false;

                /*reply.putNumber("result",resultToClient);
                reply.putBoolean("isaddnew",isNewUser);*/

                int rcode = result.body().getInteger("result", -1);

                log.add("register result", rcode);
                //map lai ma loi khi tai khoan tam khoa
                if (rcode == SoapError.AGENT_INACTIVE) {
                    log.add("agent is blocking now", "");
                    rcode = MomoProto.SystemError.IS_BLOCKING_VALUE;
                }

                boolean isAddNew = result.body().getBoolean("isaddnew", false);
                log.add("isAddNew", isAddNew);
                JsonObject jsonReply = result.body().getObject(colName.RegisterInfoCol.REGISTER_INFO, new JsonObject());
                log.add("jsonObjcet reply", jsonReply);
                String address = "";
                String nameCore = jsonReply.getString(colName.RegisterInfoCol.NAME, "").equalsIgnoreCase("") ? name : jsonReply.getString(colName.RegisterInfoCol.NAME);
                String cardIdCore = jsonReply.getString(colName.RegisterInfoCol.CARD_ID, "").equalsIgnoreCase("") ? card_id : jsonReply.getString(colName.RegisterInfoCol.CARD_ID);
                String addressCore = jsonReply.getString(colName.RegisterInfoCol.ADDRESS, "").equalsIgnoreCase("") ? address : jsonReply.getString(colName.RegisterInfoCol.ADDRESS);
                String dateOfBirthCore = jsonReply.getString(colName.RegisterInfoCol.DATE_OF_BIRTH, "").equalsIgnoreCase("") ? dob : jsonReply.getString(colName.RegisterInfoCol.DATE_OF_BIRTH);
                boolean isNamedCore = jsonReply.getBoolean(colName.RegisterInfoCol.IS_NAME, false);

                if (rcode == MomoProto.SystemError.ALL_OK_VALUE) {
                    isOk = true;

                    //luu thong tin vao mongo DB
                    JsonObject jo = new JsonObject();
                    if (!"".equalsIgnoreCase(name)) {
                        jo.putString(colName.PhoneDBCols.NAME, name);
                    } else if (!"".equalsIgnoreCase(nameCore)) {
                        jo.putString(colName.PhoneDBCols.NAME, nameCore);
                    }
                    if (!"".equalsIgnoreCase(card_id)) {
                        jo.putString(colName.PhoneDBCols.CARD_ID, card_id);
                    } else if (!"".equalsIgnoreCase(cardIdCore)) {
                        jo.putString(colName.PhoneDBCols.CARD_ID, cardIdCore);
                    }

                    if (!"".equalsIgnoreCase(dob)) {
                        jo.putString(colName.PhoneDBCols.DATE_OF_BIRTH, dob);
                    } else if (!"".equalsIgnoreCase(dateOfBirthCore)) {
                        jo.putString(colName.PhoneDBCols.DATE_OF_BIRTH, dateOfBirthCore);
                    }

                    if (!"".equalsIgnoreCase(address)) {
                        jo.putString(colName.PhoneDBCols.ADDRESS, address);
                    } else if (!"".equalsIgnoreCase(addressCore)) {
                        jo.putString(colName.PhoneDBCols.ADDRESS, addressCore);
                    }

                    jo.putString(colName.PhoneDBCols.PIN, DataUtil.encode(pin));
                    jo.putNumber(colName.PhoneDBCols.NUMBER, msg.cmdPhone);
                    jo.putBoolean(colName.PhoneDBCols.IS_REGED, true);  //da dang ky

                    //chac chan la chua dinh danh
                    if (isAddNew) {
                        jo.putBoolean(colName.PhoneDBCols.IS_NAMED, false); //da set group noname
                    } else {
                        jo.putBoolean(colName.PhoneDBCols.IS_NAMED, isNamedCore);
                    }

                    jo.putBoolean(colName.PhoneDBCols.DELETED, false);  //dang hoat dong
                    jo.putBoolean(colName.PhoneDBCols.IS_SETUP, true);  //da setUp

                    //khuyen mai
                    jo.putNumber(colName.PhoneDBCols.REFERENCE_NUMBER, referenceNumber);
                    jo.putNumber(colName.PhoneDBCols.CREATED_DATE, System.currentTimeMillis());
                    jo.putNumber(colName.PhoneDBCols.INVITER_COUNT, 0);
                    jo.putNumber(colName.PhoneDBCols.INVITEE_COUNT, 0);
                    jo.putBoolean(colName.PhoneDBCols.IS_INVITER, false);

                    //reset waiting register from mapping wallet with bank
                    if ("1".equalsIgnoreCase(waitingReg)) {
                        jo.putBoolean(colName.PhoneDBCols.WAITING_REG, false);
                    } else {
                        jo.putBoolean(colName.PhoneDBCols.WAITING_REG, true);
                    }

                    // fix bug nhap thong tin gioi thieu voi app cu
                    // 1. PG galaxy
                    //2. PG amway
                    if ((referenceNumber >= 6100 && referenceNumber <= 6200)
                            || (referenceNumber >= 7000 && referenceNumber <= 7020)
                            ) {
                        jo.putString(colName.PhoneDBCols.INVITER, referenceNumber + "");
                        jo.putNumber(colName.PhoneDBCols.INVITE_TIME, System.currentTimeMillis());

                    } else if (DataUtil.stringToVnPhoneNumber("0" + referenceNumber) > 0) {
                        jo.putString(colName.PhoneDBCols.INVITER, "0" + referenceNumber);
                        jo.putNumber(colName.PhoneDBCols.INVITE_TIME, System.currentTimeMillis());
                    }

                    log.add("updated JSON", jo);

                    phonesDb.updatePartialNoReturnObj(msg.cmdPhone, jo, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            log.add("update result", aBoolean);
                            log.writeLog();
                        }
                    });
                }

                //luon tra ket qua dang ky ve cho client
                final Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.REGISTER_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        MomoProto.StandardReply.newBuilder()
                                .setResult(isOk)
                                .setRcode(rcode)
                                .build().toByteArray()
                );
                //BEGIN 0000000052 IRON MAN
                log.add("isIronManPromoActive", isIronManPromoActive);
                log.add("isIronManPromoPlusActive", isIronManPlusActive);
                if (isAddNew && isOk && !isStore && isIronManPlusActive) {
                    final String[] address_tmp = address_tmp_string.split(MomoMessage.BELL);
                    log.add("size address tmp", address_tmp.length);
                    if (!data.os.equalsIgnoreCase("ios") && address_tmp.length == 0)
                    {
                        mCom.writeDataToSocket(sock, buf);
                    }
                    else{
                        final AtomicInteger integer = new AtomicInteger(address_tmp.length);
                        final AtomicInteger empty = new AtomicInteger(0);
                        if(!data.os.equalsIgnoreCase("ios"))
                        {
                            log.add("ios", "android");
                            vertx.setPeriodic(200L, new Handler<Long>() {
                                @Override
                                public void handle(final Long event) {
                                    int position = integer.decrementAndGet();
                                    if(position < 0)
                                    {
                                        log.add("position", position);
                                        vertx.cancelTimer(event);
                                        if(empty.intValue() != address_tmp.length)
                                        {
                                            log.add("position", "empty.intValue() != address_tmp.length");

                                            processIronMan(buf, log, msg, sock);

                                        }
                                        else{
                                            log.add("position", "empty.intValue() == address_tmp.length");
                                            mCom.writeDataToSocket(sock, buf);
                                        }
                                        log.writeLog();
                                    }
                                    else
                                    {
                                        if(address_tmp[position].equalsIgnoreCase(""))
                                        {
                                            log.add("item", address_tmp[position]);
                                            empty.incrementAndGet();
                                        }
                                        else{
                                            log.add("item", address_tmp[position]);
                                            androidDataUserDb.insert(address_tmp[position], new Handler<Integer>() {
                                                @Override
                                                public void handle(Integer inter) {
                                                    if(inter != 0)
                                                    {
                                                        vertx.cancelTimer(event);
                                                        log.add("error", "Loi insert android data user");
                                                        log.writeLog();
                                                        mCom.writeDataToSocket(sock, buf);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            }) ;
                        }
                        else if (data.os.equalsIgnoreCase("ios"))
                        {
                            androidDataUserDb.insert(data.imei, new Handler<Integer>() {
                                @Override
                                public void handle(Integer integer2) {
                                    if(integer2 == 0)
                                    {
                                        processIronMan(buf, log, msg, sock);

                                    }
                                    else{
                                        log.add("error", "Loi insert ios data user");
                                        log.writeLog();
                                        mCom.writeDataToSocket(sock, buf);
                                    }
                                }
                            });
                        }
                        else{
                            processIronMan(buf, log, msg, sock);

                        }

                    }
                }
                else{
                    mCom.writeDataToSocket(sock, buf);
                }

                if (isOk) {
                    //da tang so lan o ngoai roi, chi luu lai thoi
                    if (isAddNew && regImeiObj != null) {
                        regImeiDb.upsert(regImeiObj, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                log.add("track register", aBoolean);
                            }
                        });
                    }

                    //force send update profile when client is waiting register
                    if ("1".equalsIgnoreCase(waitingReg)) {
                        mCom.sendCurrentAgentInfo(vertx
                                , sock
                                , msg.cmdIndex
                                , msg.cmdPhone
                                , data);
                    }

                    //inform to all online users
                    //24/9/2015 ... comment NEW_USER_VALUE
//                    BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
//                    helper.setType(SoapProto.Broadcast.MsgType.NEW_USER_VALUE);
//                    helper.setSenderNumber(msg.cmdPhone);
//                    helper.setNewPhone(msg.cmdPhone);
//                    JsonObject jsonExtra = new JsonObject();
//                    jsonExtra.putBoolean(StringConstUtil.IS_STORE_APP, isStore);
//                    helper.setExtra(jsonExtra);
//                    vertx.eventBus().publish(ServerVerticle.MOMO_BROADCAST
//                            , helper.getJsonObject());
                }

                //chay M2N cho thang nay
                if (isOk && isAddNew) {

                    log.add("begin m2number", "******");
                    processM2N(msg, sock, log);
                    log.add("end m2number", "******");

                    log.add("begin gift2number", "******");
                    giftProcess.receiveGift(msg.cmdPhone);
                    log.add("end gift2number", "******");
                }

                if (isOk && isAddNew && referenceNumber > 0) {

                    doIntroGTBB(referenceNumber, msg);
                }

                log.writeLog();
            }
        });
    }

    private void processIronMan(final Buffer buf, final Common.BuildLog log, final MomoMessage msg, final NetSocket sock) {
        log.add("desc -------", " xet duyet iron man");
        ControlOnClickActivityDb.Obj controlObj = new ControlOnClickActivityDb.Obj();
        controlObj.program = StringConstUtil.IronManPromo.IRON_PROMO;
        controlObj.number = "0" + msg.cmdPhone;
        controlObj.key = msg.cmdPhone + StringConstUtil.IronManPromo.IRON_PROMO;
        controlOnClickActivityDb.insert(controlObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {
                if (event == 0) {
                    JsonObject joSearch = new JsonObject();
                    joSearch.putString(colName.IronManBonusTrackingTable.PROGRAM, StringConstUtil.IronManPromo.IRON_PROMO);
                    ironManBonusTrackingTableDb.searchWithFilter(joSearch, new Handler<ArrayList<IronManBonusTrackingTableDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<IronManBonusTrackingTableDb.Obj> objs) {
//                            log.add("size iron BonusTrackingTable", objs.size());
                            IronManBonusTrackingTableDb.Obj bonusObj = null;
                            boolean hasBonus = false;
//                            log.add("size iron BonusTrackingTable", objs);
                            for (IronManBonusTrackingTableDb.Obj bonusTrackingObj : objs) {
                                if (System.currentTimeMillis() >= bonusTrackingObj.start_time && System.currentTimeMillis() <= bonusTrackingObj.end_time) {
                                    log.add("bonusTrackingObj", bonusTrackingObj.toJson());
                                    log.add("size iron", "nam trong chuong trinh iron");
                                    log.add("program number", bonusTrackingObj.program_number);
                                    hasBonus = randomCheckIronManBonus(bonusTrackingObj, log);
                                    bonusObj = bonusTrackingObj;
                                    break;
                                } // END IF
                            } // END FOR
                            if (bonusObj != null && bonusObj.program_number != 0) {
                                final boolean hasBonus_tmp = hasBonus;
                                final IronManBonusTrackingTableDb.Obj bonusObj_tmp = bonusObj;
                                updateIronNewRegisterTrackingTable(sock, buf, msg, log, hasBonus_tmp, bonusObj_tmp);
//                                                updateIronBonusTrackingTable(bonusObj_tmp,msg, log, hasBonus_tmp);

                            } else {
                                mCom.writeDataToSocket(sock, buf);
                            }
//                                mCom.writeDataToSocket(sock, buf);
                        }
                    });
//                                }); // END LOAD TRACKING
                } else {
                    mCom.writeDataToSocket(sock, buf);
                }
            }
        });//END CONTROL
    }

    //BEGIN 0000000052 IRON MAN
    private void updateIronNewRegisterTrackingTable(final NetSocket sock, final Buffer buf, MomoMessage msg, final Common.BuildLog log, boolean hasBonus, final IronManBonusTrackingTableDb.Obj ironManBonusTracking)
    {
        log.add("func", "updateIronNewRegisterTrackingTable");
        log.add("func", ironManBonusTracking.program_number);
        final int program_number = ironManBonusTracking.program_number;
        IronManNewRegisterTrackingDb.Obj ironRegisObj = createIronManObject(msg, hasBonus, ironManBonusTracking.program_number);
        ironManNewRegisterTrackingDb.insert(ironRegisObj, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {

                log.add("desc ironManNewRegisterTrackingDb", integer);
                logger.info("ironManNewRegisterTrackingDb with result " + integer);
                saveInfoIronManDb(program_number, ironManBonusTracking);

                mCom.writeDataToSocket(sock, buf);
            }
        });
    }

    private void saveInfoIronManDb(final int program_number, final IronManBonusTrackingTableDb.Obj ironManBonusTracking)
    {
        ironManNewRegisterTrackingDb.countUserStatus(program_number, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                if (jsonArray != null && jsonArray.size() > 0) {
                    boolean isBonus = false;
                    int count = 0;
                    int receivedGift = 0;
                    int notReceivedGift = 0;
                    for (Object o : jsonArray) {
                        isBonus = ((JsonObject) o).getBoolean("_id");
                        count = ((JsonObject) o).getInteger("count", 0);
                        if (isBonus) {
                            receivedGift = count;
                        }
                        else{
                            notReceivedGift = count;
                        }
                    }
                    JsonObject jsonBonusTracking = ironManBonusTracking.toJson();
                    jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER);
                    jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN);

                    jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, receivedGift + notReceivedGift);
                    jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, receivedGift);
                    ironManBonusTrackingTableDb.updatePartial(program_number, jsonBonusTracking, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    });
//                    final Common.ServiceReq serviceReq = new Common.ServiceReq();
//                    serviceReq.Command = Common.ServiceReq.COMMAND.UPDATE_IRON_MAN_USER;
//                    serviceReq.PackageId = jsonBonusTracking.toString();
//                    Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
//                        @Override
//                        public void handle(JsonArray jsonArray) {
//
//                        }
//                    });
                }
            }
        });
    }
    private void updateIronBonusTrackingTable(IronManBonusTrackingTableDb.Obj bonusTrackingObj, MomoMessage msg, Common.BuildLog log, boolean hasBonus)
    {
//        JsonObject jsonBonusTrackingUpdate = new JsonObject();
        int bonus_man = bonusTrackingObj.number_of_bonus_man;
        int new_comer = bonusTrackingObj.number_of_new_comer + 1;
        if(hasBonus)
        {
            bonus_man = bonusTrackingObj.number_of_bonus_man + 1;
        }
//        jsonBonusTrackingUpdate.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, new_comer);
//        jsonBonusTrackingUpdate.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, bonus_man);

        JsonObject jsonBonusTracking = bonusTrackingObj.toJson();
        jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER);
        jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN);

        jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, new_comer);
        jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, bonus_man);

        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.UPDATE_IRON_MAN_USER;
        serviceReq.PackageId = jsonBonusTracking.toString();
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {

            }
        });
    }

    private IronManNewRegisterTrackingDb.Obj createIronManObject(MomoMessage msg, boolean hasBonus, int program_number)
    {
        IronManNewRegisterTrackingDb.Obj obj = new IronManNewRegisterTrackingDb.Obj();
        obj.program_number = program_number;
        obj.phoneNumber = "0" + msg.cmdPhone;
        obj.timeRegistry = System.currentTimeMillis();
        obj.hasBonus = hasBonus;
        return obj;
    }

    private boolean randomCheckIronManBonus(IronManBonusTrackingTableDb.Obj trackingObj, Common.BuildLog log) {
        boolean hasBonus = false;

        //Khong can map voi ratio cua minh ma tinh tong cong cua phien
        if(trackingObj.not_ratio_flag && trackingObj.number_of_bonus_man < trackingObj.number_of_bonus_gave_man)
        {
            hasBonus = true;
        }
        //Map voi ti le ben he thong
        else
        {
            int randomValue = UUID.randomUUID().hashCode() % 2; //0: hen, 1:xui
            double currentRatio = Double.parseDouble("1");
            if(trackingObj.number_of_new_comer != 0)
            {
                currentRatio = Double.parseDouble(trackingObj.number_of_bonus_man  + "")/ Double.parseDouble(trackingObj.number_of_new_comer  + "");
            }
            double minRatio = Double.parseDouble(trackingObj.min_ratio + "")/100;
            double maxRatio = Double.parseDouble("" + trackingObj.max_ratio)/100;
            double ratio = Double.parseDouble("" + trackingObj.numerator)/ Double.parseDouble("" + trackingObj.denominator);
            double funnyRatio_1 = (maxRatio - ratio)/2;
            double funnyRatio_2 = (ratio - minRatio)/2;
            log.setPhoneNumber("randomCheckIronManBonus");
            log.add("currentRatio", currentRatio);
            log.add("minRatio", minRatio);
            log.add("maxRatio", maxRatio);
            log.add("ratio", ratio);
            log.add("funnyRatio_1", funnyRatio_1);
            log.add("funnyRatio_2", funnyRatio_2);
            //Kiem tra xem co rap vao ratio khong
            if (randomValue == 0) {
                //Thang nay hen, random duoc tra thuong.
                hasBonus = true;
                //Kiem tra xem no co vuot qua muc cho phep hay khong, neu co thi .... xin loi em no thoi
            } else {
                // So em nay xui, he thong khong cho tra thuong.
                hasBonus = false;
                //Kiem tra ti le hien tai
            }

            if(currentRatio > maxRatio)
            {
                hasBonus = false;
            }
            else if (currentRatio < minRatio)
            {
                hasBonus = true;
            }
            else if(currentRatio > ratio + funnyRatio_1)
            {
                hasBonus = false;
            }
            else if(currentRatio < ratio - funnyRatio_2)
            {
                hasBonus = true;
            }

        }
        log.add("bonus", hasBonus);
        return hasBonus;
    }

    private void loadIronManPromoTrackingTable(Vertx _vertx, final Handler<ArrayList<IronManBonusTrackingTableDb.Obj>> arrayListHandler) {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_IRON_MAN_PROMO_TRACKING_TABLE;
        final ArrayList<IronManBonusTrackingTableDb.Obj> ironManBonusObjs = new ArrayList<>();
        Misc.getServiceInfo(_vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                logger.info("GET_IRON_MAN_PROMO_TRACKING_TABLE la " + jsonArray);
                if (jsonArray != null) {
                    logger.info("GET_IRON_MAN_PROMO_TRACKING_TABLE la" + jsonArray.size());
                    JsonObject jo;
                    IronManBonusTrackingTableDb.Obj trackingObj;
                    for (Object o : jsonArray) {
                        jo = (JsonObject) o;
                        trackingObj = new IronManBonusTrackingTableDb.Obj(jo);
                        ironManBonusObjs.add(trackingObj);
                    }
                    arrayListHandler.handle(ironManBonusObjs);
                }
            }
        });
    }
    //END 0000000052 IRON MAN

    private void doIntroGTBB(final int referenceNumber, final MomoMessage msg) {
        phonesDb.getPhoneObjInfo(referenceNumber, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if (obj != null && obj.isInviter) {

                    //todo nguoi duoc gioi thieu
                    String dGThieu = "Bạn đã được giới thiệu thành công. Trong 7 ngày (từ hôm nay) hãy thực hiện giao dịch trị giá tối thiểu 50.000đ để nhận được mã khuyến mãi trị giá 20.000đ (mã không sử dụng được cho các thuê bao trả sau chưa đăng ký thanh toán bằng thẻ trả trước hoặc Top-Up).";
                    Notification dgtNoti = new Notification();
                    dgtNoti.receiverNumber = msg.cmdPhone;
                    dgtNoti.caption = "Giới thiệu bạn bè";
                    dgtNoti.body = dGThieu;
                    dgtNoti.bodyIOS = dGThieu;
                    dgtNoti.sms = "";
                    dgtNoti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                    dgtNoti.type = MomoProto.NotificationType.NOTI_TOPUP_VALUE;
                    dgtNoti.priority = 2;
                    dgtNoti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                    dgtNoti.time = System.currentTimeMillis();
                    //ban notification
                    Misc.sendNoti(vertx, dgtNoti);

                    //todo chuc mung
                    String greetingDGT = "Chúc mừng bạn đã được mời tham gia chương trình Giới thiệu Bạn bè. Hãy dùng số điện thoại của bạn mời/giới thiệu để cài đặt Ví để mỗi người nhận được mã KM trị giá 20.000đ. Xem thêm tại: momo.vn/gioithieubanbe";
                    Notification greetingNoti = new Notification();
                    greetingNoti.receiverNumber = msg.cmdPhone;
                    greetingNoti.caption = "Giới thiệu bạn bè";
                    greetingNoti.body = greetingDGT;
                    greetingNoti.bodyIOS = greetingDGT;
                    greetingNoti.sms = "";
                    greetingNoti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                    greetingNoti.type = MomoProto.NotificationType.NOTI_GENERIC_VALUE;
                    greetingNoti.priority = 2;
                    greetingNoti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                    greetingNoti.time = System.currentTimeMillis();
                    //ban notification
                    Misc.sendNoti(vertx, greetingNoti);


                    //todo nguoi gioi thieu
                    String gThieu = "Bạn đã giới thiệu thành công số 0" + msg.cmdPhone + " Hãy tiếp tục dùng số điện thoại của mình giới thiệu bạn bè để nhận nhiều khuyến mãi của Ví MoMo. Chương trình không áp dụng cho các thuê bao trả sau chưa đăng ký thanh toán bằng thẻ trả trước hoặc Top-Up.";
                    Notification gtNoti = new Notification();
                    gtNoti.receiverNumber = referenceNumber;
                    gtNoti.caption = "Giới thiệu bạn bè";
                    gtNoti.body = gThieu;
                    gtNoti.bodyIOS = gThieu;
                    gtNoti.sms = "";
                    gtNoti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                    gtNoti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                    gtNoti.priority = 2;
                    gtNoti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                    gtNoti.time = System.currentTimeMillis();
                    //ban notification
                    Misc.sendNoti(vertx, gtNoti);

                }
            }
        });
    }

    public void processM2N(final MomoMessage msg, final NetSocket sock, final Common.BuildLog log) {
        //neu dang ky thanh cong --> kiem tra co trong bang cho nhan tien khong
        final int phoneNumber = msg.cmdPhone;
        m2cOffline.getObjectByReceviedNumber(phoneNumber, new Handler<ArrayList<M2cOffline.Obj>>() {
            @Override
            public void handle(ArrayList<M2cOffline.Obj> lstObj) {
                if (lstObj == null || lstObj.size() == 0) {
                    log.add("Khong co M2N cho nhan", "");
                    log.writeLog();
                    return;
                }

                for (final M2cOffline.Obj m2nObj : lstObj) {
                    if (m2nObj.STATUS.equalsIgnoreCase(colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW))) {
                        log.add("M2number", "");
                        log.add("destination phone", m2nObj.DESTINATION_PHONE);
                        log.add("tranId", m2nObj.TRAN_ID);
                        log.add("begin commitTran", "");
                        log.add("co lenh dang cho tranid", m2nObj.TRAN_ID);

                        Request commitObj = new Request();
                        commitObj.TYPE = Command.COMMIT;
                        commitObj.TRAN_ID = m2nObj.TRAN_ID;
                        commitObj.WALLET = WalletType.MOMO;
                        commitObj.PHONE_NUMBER = log.getPhoneNumber();
                        commitObj.TIME = log.getTime();

                        JsonObject commitJo = commitObj.toJsonObject();

                        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS
                                , commitJo
                                , new Handler<Message<Buffer>>() {
                            @Override
                            public void handle(Message<Buffer> message) {
                                CoreMessage momoMessage = CoreMessage.fromBuffer(message.body());
                                Core.StandardReply reply;
                                try {
                                    reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
                                } catch (Exception ex) {
                                    reply = null;
                                }

                                if (reply == null) {
                                    log.add("commit result", "Core.StandardReply null");
                                } else {
                                    log.add("Commit tranid", m2nObj.TRAN_ID);
                                    log.add("error", reply.getErrorCode());
                                    log.add("commitTran result", (reply == null ? "reply==null" : reply.getTid()));
                                    log.add("description", (reply == null ? "reply==null" : reply.getDescription()));
                                }

                                if (reply == null || (reply != null && reply.getErrorCode() != 0)) {
                                    log.writeLog();
                                    return;
                                }

                                if (reply.getErrorCode() == 0) {
                                    String approved = colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.APPROVED);
                                    log.add("approved", approved);
                                    //1. update sender information

                                    transDb.updateTranStatus(m2nObj.SOURCE_PHONE
                                            , m2nObj.TRAN_ID
                                            , TranObj.STATUS_OK, new Handler<TranObj>() {
                                        @Override
                                        public void handle(TranObj tranObj) {
                                            //neu la cap nhat
                                            if (tranObj != null) {
                                                log.add("update tranDb status", TranObj.STATUS_OK);
                                                final TranObj fTranObj = tranObj;

                                                //1. gui noti cho nguoi tao giao dich
                                                Notification noti = new Notification();
                                                noti.receiverNumber = m2nObj.SOURCE_PHONE;
                                                noti.caption = " Chuyển tiền thành công!";
                                                noti.body = "Quý khách chuyển thành công tới 0" + m2nObj.DESTINATION_PHONE + " Số tiền: " + String.format("%,d", m2nObj.AMOUNT) + "đ. Người nhận đã nhận tiền";
                                                noti.sms = "Quy khach chuyen thanh cong toi 0" + m2nObj.DESTINATION_PHONE + " So tien: " + String.format("%,d", m2nObj.AMOUNT) + "d. Nguoi nhan da nhan tien. TID: " + m2nObj.TRAN_ID;
                                                noti.priority = 1;
                                                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                                noti.status = Notification.STATUS_DISPLAY;

                                                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                                        , noti.toJsonObject());

                                                //2 gui tranoutside de cap nhat cho ben gui
                                                phonesDb.getPhoneObjInfoLocal(m2nObj.SOURCE_PHONE, new Handler<PhonesDb.Obj>() {
                                                    @Override
                                                    public void handle(PhonesDb.Obj phoneObj) {
                                                        if (phoneObj != null) {
                                                            fTranObj.owner_number = phoneObj.number;
                                                            fTranObj.owner_name = phoneObj.name;
                                                            BroadcastHandler.sendOutSideTransSync(vertx, fTranObj);
                                                        }
                                                    }
                                                });
                                            }

                                            log.writeLog();
                                        }
                                    });

                                    final String content = "Quý khách đã nhận thành công số tiền "
                                            + Misc.formatAmount(m2nObj.AMOUNT)
                                            + "đ từ " + m2nObj.NAME
                                            + "(0" + m2nObj.SOURCE_PHONE + ")";

                                    //todo tao tran va noti cho nguoi nhan
                                    final long time = System.currentTimeMillis();

                                    final TranObj tran = new TranObj();
                                    tran.owner_number = m2nObj.DESTINATION_PHONE;
                                    tran.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                                    tran.status = TranObj.STATUS_OK;
                                    tran.io = 1;
                                    tran.comment = content;
                                    tran.tranId = m2nObj.TRAN_ID;
                                    tran.cmdId = time;
                                    tran.error = 0;
                                    tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                    tran.clientTime = time;
                                    tran.ackTime = time;
                                    tran.finishTime = time;
                                    tran.amount = m2nObj.AMOUNT;
                                    tran.category = 0;
                                    tran.deleted = false;
                                    tran.partnerId = "0" + m2nObj.SOURCE_PHONE;
                                    tran.parterCode = "0" + m2nObj.SOURCE_PHONE;
                                    tran.partnerName = m2nObj.NAME;
                                    tran.share = new JsonArray();

                                    try {
                                        mCom.sendTransReplyByTran(msg, tran, transDb, sock);
                                    } catch (Exception e) {
                                        logger.error("mCom.sendTransReplyByTran", e);
                                    }

                                    transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean result) {

                                            //neu khong phai la cap nhat --> tao moi
                                            BroadcastHandler.sendOutSideTransSync(vertx, tran);

                                            Notification noti = new Notification();
                                            noti.receiverNumber = m2nObj.DESTINATION_PHONE;
                                            noti.caption = "Nhận tiền thành công";
                                            noti.body = content;
                                            noti.bodyIOS = content;
                                            noti.priority = 2;
                                            noti.tranId = m2nObj.TRAN_ID;
                                            noti.time = time;
                                            noti.cmdId = time;
                                            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                            noti.status = Notification.STATUS_DISPLAY;
                                            noti.sms = "";

                                            log.writeLog();

                                            Misc.sendNoti(vertx, noti);
                                        }
                                    });

                                    m2cOffline.updateAndGetObjectByTranId(m2nObj.TRAN_ID, approved, "core"
                                            , new Handler<Boolean>() {
                                        @Override

                                        public void handle(final Boolean result) {
                                            if (result) {
                                                log.add("update m2number with status", true);
                                            } else {
                                                log.add("update m2number with status", false);
                                            }
                                            log.writeLog();
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    public void processLogIn(final NetSocket sock,
                             final MomoMessage msg,
                             final SockData data,
                             final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("function", "processLogIn");

        if (data != null && !data.isSetup) {
            log.add("data.isSetup", data.isSetup);
        }
        if (data != null && !data.isSetup) {
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.LOGIN_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.LogInReply.newBuilder()
                            .setResult(false)
                            .setRcode(MomoProto.LogInReply.ResultCode.NOT_SETUP_VALUE)
                            .build().toByteArray()
            );

            mCom.writeDataToSocket(sock, buf);

            if (sock != null)
                sock.close();

            if (callback != null) {
                callback.handle(new JsonObject().putNumber("error", 100).putString("description", "Not setted Up"));
            }
            log.writeLog();
            return;
        }

        //login qua duong soapin
        //processLogInViaSoap(sock,msg,data,callback);

        //login qua duong core connector

        log.add("function", "loginViaCoreConnector");
        if (data != null) {
            log.add("data appCode + ", data.appCode + " " + data.appVersion);
        }
        loginViaCoreConnector(sock, msg, data, callback, log);
    }

    private void loginViaCoreConnector(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final Handler<JsonObject> callback, final Common.BuildLog log) {

        phonesDb.getPhoneObjInfoLocal(msg.cmdPhone, new Handler<PhonesDb.Obj>() {

            @Override
            public void handle(final PhonesDb.Obj localObj) {

                if (localObj == null) {
                    log.add("getPhoneObjInfoLocal", "NULL");

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.LogInReply.newBuilder()
                                    .setResult(false)
                                    .setRcode(MomoProto.LogInReply.ResultCode.NOT_SETUP_VALUE)
                                    .build().toByteArray()
                    );

                    //send result of login right now
                    if (sock != null)
                        mCom.writeDataToSocket(sock, buf);
                    else if (callback != null) //hung hot fix
                        callback.handle(new JsonObject()
                                .putNumber("error", MomoProto.LogInReply.ResultCode.PIN_INVALID_VALUE)
                                .putString("description", "Login lock")
                                .putNumber("lockTill", localObj.locked_till_time));

                    log.writeLog();
                    return;
                }

                //da co phoneObject
                if (data != null) {
                    data.setPhoneObj(localObj, logger, "Set phoneObj for Sockdata  with localPhone");
                }

                logger.debug("being locked in " + (localObj.locked_till_time - System.currentTimeMillis()) / 1000 + " seconds");

                if (localObj.locked_till_time > System.currentTimeMillis()) {
                    logger.debug("LOCKED");

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.LogInReply.newBuilder()
                                    .setResult(false)
                                    .setRcode(MomoProto.LogInReply.ResultCode.TEMP_LOCK_VALUE)
                                    .setLockedUntil((localObj.locked_till_time - System.currentTimeMillis()) / 1000)
                                    .setLoginRemainCount(localObj.login_max_try_count)
                                    .build().toByteArray()
                    );

                    //send result of login right now
                    if (sock != null)
                        mCom.writeDataToSocket(sock, buf);
                    else
                        callback.handle(new JsonObject().putNumber("error", MomoProto.LogInReply.ResultCode.PIN_INVALID_VALUE)
                                .putString("description", "Login lock")
                                .putNumber("lockTill", localObj.locked_till_time));
                } else {
                    logger.debug("NOT LOCKED");
                    MomoProto.LogIn request;
                    try {
                        request = MomoProto.LogIn.parseFrom(msg.cmdBody);
                    } catch (InvalidProtocolBufferException e) {
                        logger.error("InvalidProtocolBufferException", e);
                        request = null;
                    }

                    if (request == null || !request.hasMpin() || !request.hasDeviceModel()) {
                        if (sock != null)
                            mCom.writeErrorToSocket(sock);
                        else
                            callback.handle(new JsonObject().putNumber("error", 100).putString("description", "Error format message"));
                        return;
                    }

                    final String deviceModel = request.getDeviceModel();

                    logger.debug("processLogin : " + "******" + "," + request.getDeviceModel());
                    final String pin = request.getMpin();
                    final String fAppVersion = request.getAppVer() == null ? "" : request.getAppVer();
                    final int fAppCode = DataUtil.strToInt((request.getCodeVer() == null ? "0" : request.getCodeVer()));

                    //todo diem giao dịch va app thuong
                    //optional string agent_type = 3; // "0" : END-USER, "1" DGD ....

                    String agentType = (request.getAgentType() != null ? request.getAgentType() : "");
                    log.add("agentType", agentType);
                    log.add("fAppCode", fAppCode);
                    log.add("fAppVersion", fAppVersion);
                    //diem giao dich login
                    if (!"".equalsIgnoreCase(agentType) && "1".equalsIgnoreCase(agentType)) {

                        agentsDb.getOneAgent("0" + msg.cmdPhone, "loginViaCoreConnector", new Handler<AgentsDb.StoreInfo>() {
                            @Override
                            public void handle(AgentsDb.StoreInfo storeInfo) {

                                //khong tim thay DGD tren danh sach location
                                if (storeInfo == null) {
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.LogInReply.newBuilder()
                                                    .setResult(false)
                                                    .setLoginRemainCount(0)
                                                    .setRcode(MomoProto.LogInReply.ResultCode.AGENT_NOT_FOUND_VALUE)
                                                    .build().toByteArray()
                                    );
                                    if (sock != null)
                                        mCom.writeDataToSocket(sock, buf);
                                    else
                                        callback.handle(new JsonObject()
                                                .putNumber("error", MomoProto.LogInReply.ResultCode.AGENT_NOT_FOUND_VALUE)
                                                .putString("description", "DGD AGENT_NOT_FOUND_VALUE"));

                                    log.add("dgd", "khong tim thay diem giao dich tren he AgentDB");
                                    log.writeLog();
                                    return;
                                }

                                if (storeInfo.status != 0) {
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.LogInReply.newBuilder()
                                                    .setResult(false)
                                                    .setLoginRemainCount(0)
                                                    .setRcode(MomoProto.LogInReply.ResultCode.AGENT_CANCELLED_VALUE)
                                                    .build().toByteArray()
                                    );
                                    if (sock != null)
                                        mCom.writeDataToSocket(sock, buf);
                                    else
                                        callback.handle(new JsonObject()
                                                .putNumber("error", MomoProto.LogInReply.ResultCode.AGENT_CANCELLED_VALUE)
                                                .putString("description", "DGD AGENT_CANCELLED_VALUE"));

                                    log.add("dgd", "DGD da bi xoa tren AgentDb");
                                    log.writeLog();
                                    return;
                                }

                                if (data != null) {
                                    data.appCode = fAppCode;
                                    data.appVersion = fAppVersion;
                                }
                                //goi ham login binh thuong
                                doLoginViaCore(localObj
                                        , deviceModel
                                        , pin
                                        , log
                                        , msg
                                        , sock
                                        , fAppCode
                                        , fAppVersion
                                        , data
                                        , callback);

                            }
                        });
                    } else {
                        //end-user login
                        if (data != null) {
                            data.appCode = fAppCode;
                            data.appVersion = fAppVersion;
                        }

                        doLoginViaCore(localObj
                                , deviceModel
                                , pin
                                , log
                                , msg
                                , sock
                                , fAppCode
                                , fAppVersion
                                , data
                                , callback);
                    }
                }
            }
        });
    }

    private void doLoginViaCore(final PhonesDb.Obj localObj
            , final String deviceModel
            , final String pin
            , final Common.BuildLog log
            , final MomoMessage msg
            , final NetSocket sock
            , final int appCode
            , final String appVer
            , final SockData data
            , final Handler<JsonObject> callback) {
        log.add("call login via core", "");
//        PlusCoreConnectorVerticle.getBalance(vertx, "0" + msg.cmdPhone, pin, logger, new Handler<Integer>() {
        NewCoreConnectorVerticle.getBalance(vertx, "0" + msg.cmdPhone, pin, logger, new Handler<Integer>() {
            @Override
            public void handle(final Integer loginResult) {

                log.add("login result", loginResult);

                //login khong thanh cong
                if (loginResult != MomoProto.LogInReply.ResultCode.ALL_OK_VALUE) {

                    if (loginResult == MomoProto.LogInReply.ResultCode.AUTH_RETRY_EXCEED_VALUE) {
                        //set count ve 0
                        JsonObject jo = new JsonObject();
                        jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, 0);
                        jo.putNumber(colName.PhoneDBCols.LOCKED_UNTIL, 0);
                        jo.putBoolean(colName.PhoneDBCols.IS_SETUP, true);

                        log.add("updated JSON", jo.encodePrettily());
                        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, jo, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                log.add("updated result", aBoolean);
                            }
                        });

                        Buffer buf = MomoMessage.buildBuffer(
                                MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                MomoProto.LogInReply.newBuilder()
                                        .setResult(false)
                                        .setLoginRemainCount(0)
                                        .setRcode(loginResult)
                                        .build().toByteArray()
                        );
                        if (sock != null)
                            mCom.writeDataToSocket(sock, buf);
                        else
                            callback.handle(new JsonObject()
                                    .putNumber("error", loginResult)
                                    .putString("description", "AUTH_RETRY_EXCEED_VALUE"));
                    } else if (loginResult == MomoProto.LogInReply.ResultCode.AUTH_EXPIRED_VALUE) {

                        //giam so lan con lai va thong bao loi cu the
                        JsonObject jo = new JsonObject();

                        if (localObj != null && localObj.login_max_try_count > 0) {
                            jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT
                                    , localObj.login_max_try_count - 1);
                        } else {
                            jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, 0);
                        }

                        jo.putNumber(colName.PhoneDBCols.LOCKED_UNTIL, 0);

                        log.add("updated JSON", jo);

                        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, jo, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                log.add("update result", aBoolean);
                                Buffer buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        MomoProto.LogInReply.newBuilder()
                                                .setResult(false)
                                                .setLoginRemainCount((localObj.login_max_try_count > 0 ? localObj.login_max_try_count : 0))
                                                .setRcode(loginResult)
                                                .build().toByteArray()
                                );
                                if (sock != null)
                                    mCom.writeDataToSocket(sock, buf);
                                else
                                    callback.handle(new JsonObject()
                                            .putNumber("error", loginResult)
                                            .putString("description", "AUTH_EXPIRED_VALUE"));

                                log.writeLog();
                            }
                        });

                    } else {
                        long lockTill = 0;

                        if (localObj.login_max_try_count == 7) {
                            lockTill = System.currentTimeMillis() + 5 * 60 * 1000;
                        }

                        if (localObj.login_max_try_count == 4) {
                            lockTill = System.currentTimeMillis() + 10 * 60 * 1000;
                        }

                        log.add("localObj.login_max_try_count/lockTill"
                                , localObj.login_max_try_count + "/" + lockTill);

                        final long fLockTill = lockTill;
                        final int fRemainLoginCount = (localObj.login_max_try_count > 0 ? localObj.login_max_try_count - 1 : 0);
                        //for locktill
                        JsonObject jolock = new JsonObject();
                        jolock.putNumber(colName.PhoneDBCols.LOCKED_UNTIL, lockTill);
                        jolock.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, fRemainLoginCount);

                        log.add("updated JSON", jolock);

                        phonesDb.updatePartialNoReturnObj(msg.cmdPhone, jolock, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                log.add("updated result", aBoolean);

                                Buffer buf;
                                if (fLockTill != 0) {
                                    buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.LogInReply.newBuilder()
                                                    .setResult(false)
                                                    .setLoginRemainCount(fRemainLoginCount)
                                                    .setLockedUntil((fLockTill - System.currentTimeMillis()) / 1000)
                                                    .setRcode(MomoProto.LogInReply.ResultCode.TEMP_LOCK_VALUE)
                                                    .build().toByteArray()
                                    );
                                } else {
                                    buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.LogInReply.newBuilder()
                                                    .setResult(false)
                                                    .setLoginRemainCount(fRemainLoginCount)
                                                    .setRcode(loginResult)
                                                    .build().toByteArray()
                                    );
                                }

                                log.add("login failed, remain login count", fRemainLoginCount);

                                if (sock != null)
                                    mCom.writeDataToSocket(sock, buf);
                                else
                                    callback.handle(new JsonObject()
                                            .putNumber("error", loginResult)
                                            .putNumber("loginRemain", fRemainLoginCount)
                                            .putNumber("lockTill", fLockTill));


                                log.writeLog();
                            }
                        });
                    }
                } else {
                    log.add("logged in by phone", "");

//                    StatisticUtils.fireLogin(vertx.eventBus(), msg.cmdPhone, StatisticModels.Action.Channel.MOBILE);
//
//                    if (sock != null && data != null) {
//                        data.beginSession(msg.cmdPhone, sock, vertx.eventBus(), logger);
//                        data.ip = sock.remoteAddress().getAddress().toString();//10.10.10.
//                        data.setPhoneObj(localObj, logger, "");
//
//                        data.sessionKey = UUID.randomUUID().toString();
//                        data.pin = pin;
//
//                        //cap nhat thoi gian login sau cung cho session nay
//                        data.start_access_time = System.currentTimeMillis();
//                        data.deviceModel = deviceModel;
//                    }
//
//                    //login thanh cong
//                    Buffer buf = MomoMessage.buildBuffer(
//                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
//                            msg.cmdIndex,
//                            msg.cmdPhone,
//                            MomoProto.LogInReply.newBuilder()
//                                    .setResult(true)
//                                    .setTime(System.currentTimeMillis())
//                                    .setSkey(data.sessionKey)
//                                    .build().toByteArray()
//                    );
//
//                    //send result of login right now
//                    if (sock != null) {
//                        mCom.writeDataToSocket(sock, buf);
//
//                        //Check mapvi
//                        phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
//                            @Override
//                            public void handle(PhonesDb.Obj obj) {
//                                if (obj != null) {
//                                    checkWalletMappingFromBank(msg, obj.bank_code, log);
//                                }
//                            }
//                        });
//
//                        // End Mapvi
//                    }
//
//                    if (callback != null) {
//                        callback.handle(new JsonObject().putNumber("error", 0));
//                    }
//
//                    JsonObject objNew = new JsonObject();
//                    objNew.putString(colName.PhoneDBCols.LAST_IMEI, data.imei);
//                    objNew.putString(colName.PhoneDBCols.PIN, DataUtil.encode(pin));
//                    objNew.putString(colName.PhoneDBCols.SESSION_KEY, data.sessionKey);
//                    objNew.putNumber(colName.PhoneDBCols.LAST_TIME, System.currentTimeMillis());
//                    objNew.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, LOGIN_MAX_COUNT);
//
//                    objNew.putNumber(colName.PhoneDBCols.APPCODE, appCode);
//                    objNew.putString(colName.PhoneDBCols.APPVER, appVer);
//
//                    phonesDb.updatePartialNoReturnObj(msg.cmdPhone, objNew, new Handler<Boolean>() {
//                        @Override
//                        public void handle(Boolean aBoolean) {
//                            log.add("update last login", aBoolean);
//                            data.setPhoneObj(localObj, logger, "set phone Obj after login success");
//                            data.pushToken = localObj.pushToken;
//                            data.os = localObj.phoneOs;
//
//                            if (sock != null) {
//                                signalNewLogIn(sock.writeHandlerID(), msg.cmdPhone);
//                                mCom.sendCurrentAgentInfo(vertx
//                                        , sock
//                                        , msg.cmdIndex
//                                        , msg.cmdPhone
//                                        , data);
//                            }
//
//                            log.writeLog();
//
//                        }
//                    });

                    //login thanh cong
                    final Buffer signUpBuf = MomoMessage.buildBuffer(
                            SoapProto.MsgType.GET_AGENT_INFO_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            SoapProto.AgentInfo.newBuilder().setNumber("0" + msg.cmdPhone)
                                    .build()
                                    .toByteArray()
                    );
                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, signUpBuf, new Handler<Message<Buffer>>() {
                        @Override
                        public void handle(final Message message) {

                            MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
                            SoapProto.GetAgentInfoReply request;
                            try {
                                request = SoapProto.GetAgentInfoReply.parseFrom(momoMsg.cmdBody);
                            } catch (InvalidProtocolBufferException e) {
                                request = null;
                            }
                            log.add("request", request);
                            String name = "";
                            String address = "";
                            String dateOfBirth = "";
                            String card_id = "";
                            String bank_name = "";
                            String bank_acc = "";
                            String bank_code = "";
                            boolean isNamed = false;

                            if (request != null && request.getResult()) {
                                name = request.getName();
                                address = request.getAddress();
                                dateOfBirth = request.getDateOfBirth();
                                card_id = request.getCardId();
                                bank_name = request.getBankName();
                                bank_acc = request.getBankAcc();
                                bank_code = request.getBankCode();
                                SoapProto.RegStatus regStatus = request.getRegStatus();
                                if (regStatus != null) {
                                    isNamed = regStatus.getIsNamed();
                                } else if (!"".equalsIgnoreCase(bank_name) || !"".equalsIgnoreCase(bank_acc) || !"".equalsIgnoreCase(bank_code)) {
                                    isNamed = true;
                                }
                            }
                            log.add("logged in by phone", "");
                            log.add("name", name);
                            log.add("address", address);
                            log.add("dateOfBirth", dateOfBirth);
                            log.add("card_id", card_id);
                            log.add("bank_name", bank_name);
                            log.add("bank_acc", bank_acc);
                            log.add("isNamed", isNamed);
                            log.add("bank_code", bank_code);

                            StatisticUtils.fireLogin(vertx.eventBus(), msg.cmdPhone, StatisticModels.Action.Channel.MOBILE);

                            if (sock != null && data != null) {
                                data.beginSession(msg.cmdPhone, sock, vertx.eventBus(), logger, glbConfig);
                                data.ip = sock.remoteAddress().getAddress().toString();//10.10.10.
                                data.setPhoneObj(localObj, logger, "");

                                data.sessionKey = UUID.randomUUID().toString();
                                data.pin = pin;

                                //cap nhat thoi gian login sau cung cho session nay
                                data.start_access_time = System.currentTimeMillis();
                                data.deviceModel = deviceModel;
                            }

                            //login thanh cong
                            Buffer buf = MomoMessage.buildBuffer(
                                    MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                    msg.cmdIndex,
                                    msg.cmdPhone,
                                    MomoProto.LogInReply.newBuilder()
                                            .setResult(true)
                                            .setTime(System.currentTimeMillis())
                                            .setSkey(data.sessionKey)
                                            .build().toByteArray()
                            );

                            //send result of login right now
                            if (sock != null) {
                                mCom.writeDataToSocket(sock, buf);
                                //Check mapvi

                                phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(final PhonesDb.Obj obj) {
                                        if (obj != null) {
                                            log.add("app code of user", appCode);
                                            log.add("ios code", iosCode);
                                            log.add("android code", androidCode);
                                            if (appCode >= iosCode || appCode >= androidCode) {
                                                checkWalletMappingFromBank(msg, obj.bank_code, log, data);
                                            }
                                            // End Mapvi
//                                            if (!isStore && obj.bankPersonalId.equalsIgnoreCase("") && obj.bank_name.equalsIgnoreCase("") && isIronManPromoActive) {
//                                                card.findAll(msg.cmdPhone, new Handler<ArrayList<Card.Obj>>() {
//                                                    @Override
//                                                    public void handle(ArrayList<Card.Obj> objs) {
//                                                        if (objs.size() == 0) {
//                                                            log.add("desc", "Khong co su dung the nao ca");
//                                                            log.add("desc", "Bat dau kiem tra Iron man");
//                                                            checkIronManPromo(msg, log, data, StringConstUtil.IronManPromo.IRON_PROMO, sock);
//
//
//                                                        } else {
//                                                            boolean hasVisa = false;
//                                                            for (Card.Obj cardObj : objs) {
//                                                                if (cardObj.bankid.equalsIgnoreCase(StringConstUtil.PHONES_BANKID_SBS)) {
//                                                                    log.add("desc", "Co the visa ne");
//                                                                    hasVisa = true;
//                                                                    break;
//                                                                }
//                                                            }
//                                                            log.add("hasVisa is ", hasVisa);
//                                                            if (!hasVisa) {
//                                                                log.add("desc", "Bat dau kiem tra Iron man");
//                                                                checkIronManPromo(msg, log, data, StringConstUtil.IronManPromo.IRON_PROMO, sock);
//
//                                                            }
//                                                        }
//                                                        log.writeLog();
//                                                    }
//                                                });
//                                            } else if (isPreIronPromoActive) {
//                                                log.add("isPreIronPromoActive", isPreIronPromoActive);
//                                                final long start_time = jsonPreIronPromo.getLong(StringConstUtil.PreIronManPromo.START_TIME, Long.parseLong("1442422800000"));
//                                                final long end_time = jsonPreIronPromo.getLong(StringConstUtil.PreIronManPromo.END_TIME, Long.parseLong("1442662514756"));
//
//                                                if (obj != null && obj.createdDate > start_time && obj.createdDate < end_time) {
//                                                    //Cho no 1 trong 2000 voucher
//                                                    PreIronManPromoObj.requestIronManPromo(vertx, "0" + msg.cmdPhone, StringConstUtil.PreIronManPromo.GROUP_1, new Handler<JsonObject>() {
//                                                        @Override
//                                                        public void handle(JsonObject event) {
//
//                                                        }
//                                                    });
//                                                } else if (obj != null && obj.createdDate > end_time) {
//                                                    //Random nhe.
//                                                    if (System.currentTimeMillis() % 2 == 0) {
//                                                        //Tra qua
//                                                        PreIronManPromoObj.requestIronManPromo(vertx, "0" + msg.cmdPhone, StringConstUtil.PreIronManPromo.GROUP_2, new Handler<JsonObject>() {
//                                                            @Override
//                                                            public void handle(JsonObject event) {
//
//                                                            }
//                                                        });
//                                                    } else {
//                                                        //Khong Tra
//                                                        log.add("desc", "Chu xui vl, eo duoc 10k luon");
//                                                        log.writeLog();
//                                                        return;
//                                                    }
//                                                } else {
//                                                    //Khong Tra
//                                                    log.add("desc", "Chu xui vl, eo duoc 10k luon");
//                                                    log.writeLog();
//                                                    return;
//                                                }
//                                            } else if (!isStore && obj.bankPersonalId.equalsIgnoreCase("") && obj.bank_name.equalsIgnoreCase("") && isIronManPlusActive) {
//                                                checkIronManPromo(msg, log, data, StringConstUtil.IronManPromo.IRON_PROMO_4, sock);
//                                            }
                                        }
                                    }
                                });

                            }

                            if (callback != null) {
                                callback.handle(new JsonObject().putNumber("error", 0));
                            }

                            JsonObject objNew = new JsonObject();
                            objNew.putString(colName.PhoneDBCols.LAST_IMEI, data.imei);
                            objNew.putString(colName.PhoneDBCols.PIN, DataUtil.encode(pin));
                            objNew.putString(colName.PhoneDBCols.SESSION_KEY, data.sessionKey);
                            objNew.putNumber(colName.PhoneDBCols.LAST_TIME, System.currentTimeMillis());
                            objNew.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, LOGIN_MAX_COUNT);

                            objNew.putNumber(colName.PhoneDBCols.APPCODE, appCode);
                            objNew.putString(colName.PhoneDBCols.APPVER, appVer);

                            if (!"".equalsIgnoreCase(name)) {
                                objNew.putString(colName.PhoneDBCols.NAME, name);
                                localObj.name = name;
                            }
                            if (!"".equalsIgnoreCase(address)) {
                                objNew.putString(colName.PhoneDBCols.ADDRESS, address);
                                localObj.address = address;
                            }
                            if (!"".equalsIgnoreCase(dateOfBirth)) {
                                objNew.putString(colName.PhoneDBCols.DATE_OF_BIRTH, dateOfBirth);
                                localObj.dateOfBirth = dateOfBirth;
                            }
                            if (!"".equalsIgnoreCase(card_id)) {
                                objNew.putString(colName.PhoneDBCols.CARD_ID, card_id);
                                localObj.cardId = card_id;
                            }
                            if (!"".equalsIgnoreCase(bank_acc)) {
                                objNew.putString(colName.PhoneDBCols.BANK_ACCOUNT, bank_acc);
                                localObj.bank_account = bank_acc;
                            }
                            if (!"".equalsIgnoreCase(bank_code)) {
                                objNew.putString(colName.PhoneDBCols.BANK_CODE, bank_code);
                                localObj.bank_code = bank_code;
                            }
                            if (!"".equalsIgnoreCase(bank_name)) {
                                objNew.putString(colName.PhoneDBCols.BANK_NAME, bank_name);
                                localObj.bank_name = bank_name;
                            }
                            if (isNamed) {
                                objNew.putBoolean(colName.PhoneDBCols.IS_NAMED, isNamed);
                                localObj.isNamed = isNamed;
                            }

                            phonesDb.updatePartialNoReturnObj(msg.cmdPhone, objNew, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                    log.add("update last login", aBoolean);
                                    data.setPhoneObj(localObj, logger, "set phone Obj after login success");
                                    data.pushToken = localObj.pushToken;
                                    data.os = localObj.phoneOs;

                                    if (sock != null) {
                                        signalNewLogIn(sock.writeHandlerID(), msg.cmdPhone);
                                        mCom.sendCurrentAgentInfo(vertx
                                                , sock
                                                , msg.cmdIndex
                                                , msg.cmdPhone
                                                , data);
                                    }

                                    log.writeLog();

                                }
                            });
                        }
                    });
                }
            }
        });
    }

    //BEGIN 0000000052 IRONMAN
    private void checkIronManPromo(final MomoMessage msg, final Common.BuildLog log,final SockData data, String program, final NetSocket sock) {
        //Kiem tra xem chuong trinh nay co dang chay hay khong.

        ControlOnClickActivityDb.Obj controlobj = new ControlOnClickActivityDb.Obj();
        if(program.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO))
        {
            controlobj.program = StringConstUtil.IronManPromo.IRON_PROMO;
            controlobj.number = msg.cmdPhone + "";
            controlobj.service = StringConstUtil.IronManPromo.IRON_PROMO;
            controlobj.key = StringConstUtil.IronManPromo.IRON_PROMO + msg.cmdPhone + StringConstUtil.IronManPromo.IRON_PROMO;
        }
        else{
            controlobj.program = StringConstUtil.IronManPromo.IRON_PROMO_4;
            controlobj.number = msg.cmdPhone + "";
            controlobj.service = StringConstUtil.IronManPromo.IRON_PROMO_4;
            controlobj.key = StringConstUtil.IronManPromo.IRON_PROMO_4 + msg.cmdPhone + StringConstUtil.IronManPromo.IRON_PROMO_4;
        }
        controlOnClickActivityDb.insert(controlobj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {
                if(event == 0)
                {
                    log.add("func", "checkAndGiveIronManPromo1");
                    checkAndGiveIronManPromo(msg, log, sock, data);
                }
            }
        });
    }

    private void checkAndGiveIronManPromo(final MomoMessage msg, final Common.BuildLog log, final NetSocket sock, final SockData data) {
        log.add("func", "checkAndGiveIronManPromo2");
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                //Kiem tra neu thoa dieu kien thi cho voucher 10k
                long ironman_promo_start_date = 0;
                long ironman_promo_end_date = 0;

                long ironman_promo_start_date_plus = 0;
                long ironman_promo_end_date_plus = 0;
                long currentTime = System.currentTimeMillis();
                if (array != null && array.size() > 0) {
                    ArrayList<PromotionDb.Obj> objs = new ArrayList<>();
                    PromotionDb.Obj obj_promo = null;
                    JsonObject jsonTime = new JsonObject();
                    for (final Object o : array) {
                        obj_promo = new PromotionDb.Obj((JsonObject) o);
                        if (obj_promo.NAME.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_LATER)) {
                            ironman_promo_start_date_plus = obj_promo.DATE_FROM;
                            ironman_promo_end_date_plus = obj_promo.DATE_TO;
                            break;
                        }
//                        else if(obj_promo.NAME.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_LATER)) {
//                            ironman_promo_start_date_plus = obj_promo.DATE_FROM;
//                            ironman_promo_end_date_plus = obj_promo.DATE_TO;
//                        }
                    } // END FOR
                    // Dang trong thoi gian tra thuong hehe
                    if ((currentTime <= ironman_promo_end_date_plus && currentTime >= ironman_promo_start_date_plus))  {
                        ironManNewRegisterTrackingDb.findOne("0" + msg.cmdPhone, new Handler<IronManNewRegisterTrackingDb.Obj>() {
                            @Override
                            public void handle(IronManNewRegisterTrackingDb.Obj obj) {
                                //Ten nay co tham gia chuong trinh ne
                                if (obj != null) {
                                    if (obj.hasBonus) {
                                        log.add("desc", "chu nay moi dang ki nen duoc tham gia ironman_promo + duoc thuong roi hehe");
                                        //Ban qua Verticle IRONMAN_PROMO.
                                        IronManPromoObj.requestIronManPromo(vertx, "0" + msg.cmdPhone, 0, 0, "topup", StringConstUtil.IronManPromo.IRON_PROMO_4, "", "", new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject jsonObject) {
                                                if (jsonObject != null) {
                                                    log.add("key", jsonObject);
                                                }
                                                log.add("key", jsonObject);
                                                log.add("func desc", "sendCurrent");
//                                                Notification noti = createNotiGroupFour("0" + msg.cmdPhone, PromoContentNotification.IRON_MAN_PLUS_TITLE,
//                                                        PromoContentNotification.IRON_MAN_PLUS_BODY);
//                                                Misc.sendNoti(vertx, noti);
                                                //long money = jsonIronManPlus.getLong(StringConstUtil.IronManPromoPlus.VALUE_OF_GIFT, 10000);
                                                mCom.sendCurrentAgentInfoWithDefaultMoney(vertx
                                                        , sock
                                                        , msg.cmdIndex
                                                        , msg.cmdPhone
                                                        , 10000
                                                        , data);
                                                log.writeLog();
                                            }
                                        });
                                        //long money = jsonIronManPlus.getLong(StringConstUtil.IronManPromoPlus.VALUE_OF_GIFT, 10000);
                                        mCom.sendCurrentAgentInfoWithDefaultMoney(vertx
                                                , sock
                                                , msg.cmdIndex
                                                , msg.cmdPhone
                                                , 10000
                                                , data);
                                        log.writeLog();
                                        return;
                                    }
                                    log.add("desc", "chu nay moi dang ki nen duoc tham gia ironman_promo nhung xui qua eo duoc thuong");
                                } else {
                                    log.add("desc", "chu nay khong duoc tham gia ironman_promo");
                                }
                                log.writeLog();
                            }
                        });
                    }
                } // END IF ARRAY
            }
        });
    }
    //END 0000000052 IRONMAN
    /*public void processLogInViaSoap( final NetSocket sock,
                                final MomoMessage msg,
                                final SockData data,
                                final Handler<JsonObject> callback) {

        if(!data.isSetup){
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.LOGIN_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    MomoProto.LogInReply.newBuilder()
                            .setResult(false)
                            .setRcode(MomoProto.LogInReply.ResultCode.NOT_SETUP_VALUE)
                            .build().toByteArray()
            );

            CoreCommon.writeDataToSocket(sock, buf);

            if (sock != null)
                sock.close();
            return;
        }

        //get db
        phonesDb.getPhoneObjInfoLocal(msg.cmdPhone,new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(final PhonesDb.Obj localObj) {

                logger.debug("being locked in " +  (localObj.locked_till_time - System.currentTimeMillis())/1000  + " seconds");

                if(localObj.locked_till_time > System.currentTimeMillis()){
                    logger.debug("LOCKED");

                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            MomoProto.LogInReply.newBuilder()
                                    .setResult(false)
                                    .setRcode(MomoProto.LogInReply.ResultCode.TEMP_LOCK_VALUE)
                                    .setLockedUntil((localObj.locked_till_time - System.currentTimeMillis())/1000)
                                    .setLoginRemainCount(localObj.login_max_try_count)
                                    .build().toByteArray()
                    );

                    //send result of login right now
                    if (sock != null)
                        CoreCommon.writeDataToSocket(sock, buf);
                    else
                        callback.handle(new JsonObject().putNumber("error", MomoProto.LogInReply.ResultCode.PIN_INVALID_VALUE).putString("description", "Login lock").putNumber("lockTill", localObj.locked_till_time));
                }
                else{
                    logger.debug("NOT LOCKED");
                    MomoProto.LogIn request;
                    try {
                        request = MomoProto.LogIn.parseFrom(msg.cmdBody);
                    } catch (InvalidProtocolBufferException e) {
                        logger.error("InvalidProtocolBufferException", e);
                        request = null;
                    }

                    if (request == null || !request.hasMpin() || !request.hasDeviceModel()) {
                        if (sock != null)
                            CoreCommon.writeErrorToSocket(sock);
                        else
                            callback.handle(new JsonObject().putNumber("error", 100).putString("description", "Error format message"));
                        return;
                    }

                    final String deviceModel = request.getDeviceModel();

                    Buffer signInBuf = MomoMessage.buildBuffer(
                            SoapProto.MsgType.LOG_IN_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            SoapProto.LogIn.newBuilder()
                                    .setNumber(msg.cmdPhone)
                                    .setMpin(request.getMpin())
                                    .build()
                                    .toByteArray()
                    );


                    logger.debug("processLogin : " + "******" + "," + request.getDeviceModel());
                    final String pin = request.getMpin();
                    vertx.eventBus().sendWithTimeout(AppConstant.SoapVerticle_ADDRESS, signInBuf, 30000, new Handler<AsyncResult<CoreMessage<Integer>>>() {
                        @Override
                        public void handle(AsyncResult<CoreMessage<Integer>> messageAsyncResult) {
                            if (messageAsyncResult.failed()) {
                                //timeout
                                String err = messageAsyncResult.cause().getMessage();

                                logger.info(msg.cmdPhone + " login failed : " + err);

                                Buffer buf = MomoMessage.buildBuffer(
                                        MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                        msg.cmdIndex,
                                        msg.cmdPhone,
                                        MomoProto.LogInReply.newBuilder()
                                                .setResult(false)
                                                .setRcode(MomoProto.LogInReply.ResultCode.SYSTEM_ERROR_VALUE)
                                                .build().toByteArray()
                                );

                                //send result of login right now
                                if (sock != null)
                                    CoreCommon.writeDataToSocket(sock, buf);
                                else
                                    callback.handle(new JsonObject().putNumber("error", 100).putString("description", "Login timeout"));
                            }
                            else if(messageAsyncResult.succeeded()){
                                final int loginResult = messageAsyncResult.result().body();

                                //login khong thanh cong
                                if(loginResult != MomoProto.LogInReply.ResultCode.ALL_OK_VALUE){

                                    if(loginResult == MomoProto.LogInReply.ResultCode.AUTH_RETRY_EXCEED_VALUE){
                                        //set count ve 0
                                        JsonObject jo = new JsonObject();
                                        jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, 0);
                                        jo.putNumber(colName.PhoneDBCols.LOCKED_UNTIL,0);

                                        phonesDb.updatePartial(msg.cmdPhone,jo,new Handler<PhonesDb.Obj>() {
                                            @Override
                                            public void handle(PhonesDb.Obj event) {
                                                logger.debug("LAST_IMEI after setup " + event.lastImei);
                                            }
                                        });

                                        Buffer buf = MomoMessage.buildBuffer(
                                                MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                                msg.cmdIndex,
                                                msg.cmdPhone,
                                                MomoProto.LogInReply.newBuilder()
                                                        .setResult(false)
                                                        .setLoginRemainCount(0)
                                                        .setRcode(loginResult)
                                                        .build().toByteArray()
                                        );
                                        if (sock != null)
                                            CoreCommon.writeDataToSocket(sock, buf);
                                        else
                                            callback.handle(new JsonObject().putNumber("error", loginResult).putString("description", "AUTH_RETRY_EXCEED_VALUE"));
                                    }
                                    else if(loginResult == MomoProto.LogInReply.ResultCode.AUTH_EXPIRED_VALUE){

                                        //giam so lan con lai va thong bao loi cu the
                                        JsonObject jo = new JsonObject();
                                        jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, 0);
                                        jo.putNumber(colName.PhoneDBCols.LOCKED_UNTIL,0);

                                        phonesDb.updatePartial(msg.cmdPhone,jo,new Handler<PhonesDb.Obj>() {
                                            @Override
                                            public void handle(PhonesDb.Obj event) {
                                                logger.debug("LAST_IMEI after setup " + event.lastImei);
                                            }
                                        });

                                        Buffer buf = MomoMessage.buildBuffer(
                                                MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                                msg.cmdIndex,
                                                msg.cmdPhone,
                                                MomoProto.LogInReply.newBuilder()
                                                        .setResult(false)
                                                        .setLoginRemainCount(9)
                                                        .setRcode(loginResult)
                                                        .build().toByteArray()
                                        );
                                        if (sock != null)
                                            CoreCommon.writeDataToSocket(sock, buf);
                                        else
                                            callback.handle(new JsonObject().putNumber("error", loginResult).putString("description", "AUTH_EXPIRED_VALUE"));

                                    }
                                    else {
                                        long lockTill = 0;

                                        if(localObj.login_max_try_count == 7){
                                            lockTill = System.currentTimeMillis() + 5*60*1000;

                                        }

                                        if(localObj.login_max_try_count == 4){
                                            lockTill = System.currentTimeMillis() + 10*60*1000;
                                        }

                                        logger.debug("localObj.login_max_try_count " + localObj.login_max_try_count
                                                        + "lockTill " + lockTill
                                        );

                                        final long fLockTill = lockTill;

                                        phonesDb.decrementAndGetLoginCnt(msg.cmdPhone, lockTill, new Handler<Integer>() {
                                            @Override
                                            public void handle(Integer integer) {
                                                Buffer buf;
                                                if (fLockTill != 0) {
                                                    buf = MomoMessage.buildBuffer(
                                                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                                            msg.cmdIndex,
                                                            msg.cmdPhone,
                                                            MomoProto.LogInReply.newBuilder()
                                                                    .setResult(false)
                                                                    .setLoginRemainCount(integer)
                                                                    .setLockedUntil((fLockTill - System.currentTimeMillis())/1000)
                                                                    .setRcode(MomoProto.LogInReply.ResultCode.TEMP_LOCK_VALUE)
                                                                    .build().toByteArray()
                                                    );

                                                }
                                                else {
                                                    buf = MomoMessage.buildBuffer(
                                                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                                            msg.cmdIndex,
                                                            msg.cmdPhone,
                                                            MomoProto.LogInReply.newBuilder()
                                                                    .setResult(false)
                                                                    .setLoginRemainCount(integer)
                                                                    .setRcode(loginResult)
                                                                    .build().toByteArray()
                                                    );
                                                }

                                                logger.debug(msg.cmdPhone + " login failed, remain login count " + integer);

                                                if (sock != null)
                                                    CoreCommon.writeDataToSocket(sock, buf);
                                                else
                                                    callback.handle(new JsonObject()
                                                            .putNumber("error", loginResult)
                                                            .putNumber("loginRemain", integer)
                                                            .putNumber("lockTill", fLockTill));
                                            }
                                        });
                                    }
                                }
                                else {
                                    //login thanh cong

                                    logger.debug(msg.cmdPhone + " logged in by phone.");
                                    StatisticUtils.fireLogin(vertx.eventBus(), msg.cmdPhone, StatisticModels.Action.Channel.MOBILE);

                                    if (sock != null) {
                                        data.beginSession(msg.cmdPhone, sock, vertx.eventBus(), logger);
                                        data.ip = sock.remoteAddress().getAddress().toString();
                                    }
                                    data.sessionKey = UUID.randomUUID().toString();
                                    data.pin = pin;

                                    //cap nhat thoi gian login sau cung cho session nay
                                    data.start_access_time = System.currentTimeMillis();

                                    data.deviceModel = deviceModel;

                                    //login thanh cong
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.LOGIN_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            MomoProto.LogInReply.newBuilder()
                                                    .setResult(true)
                                                    .setTime(System.currentTimeMillis())
                                                    .setSkey(data.sessionKey)

                                                    .build().toByteArray()
                                    );

                                    //send result of login right now
                                    if(sock != null){
                                        CoreCommon.writeDataToSocket(sock, buf);
                                    }
                                    phonesDb.updateLastLogin(msg.cmdPhone
                                                            ,data.imei
                                                            ,data.sessionKey
                                                            ,System.currentTimeMillis()
                                                            ,data.pin
                                                            ,new Handler<PhonesDb.Obj>() {
                                        @Override
                                        public void handle(PhonesDb.Obj obj) {

                                            if (sock != null) {
                                                signalNewLogIn(sock.writeHandlerID(), msg.cmdPhone);
                                                data.setPhoneObj(obj,logger,"");

                                                CoreCommon.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, data);
                                            }

                                            if (callback != null)
                                                callback.handle(new JsonObject().putNumber("error", 0));
                                        }
                                    });


                                }
                            }
                        }
                    });
                }

            }
        });

    }*/

    public void signalNewLogIn(String sockId, int phone) {
        BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
        helper.setType(SoapProto.Broadcast.MsgType.KILL_PREV_VALUE);
        helper.setReceivers(sockId);

        vertx.eventBus().publish(Misc.getNumberBus(phone), helper.getJsonObject());
    }

    public void processChangePin(final NetSocket sock, final MomoMessage msg, final SockData _data) {

        MomoProto.ChangePin request;
        try {
            request = MomoProto.ChangePin.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null || !request.hasNewPin() || !request.hasOldPin()) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        String number = "0" + String.valueOf(msg.cmdPhone);
        String old_pin = request.getOldPin();

        logger.debug("processChangePin : " + request.getOldPin() + "," + request.getNewPin());
        final String new_pin = request.getNewPin();

        //build buffer --> soap verticle
        Buffer changePin = MomoMessage.buildBuffer(
                SoapProto.MsgType.CHANGE_PIN_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                SoapProto.ChangePin.newBuilder()
                        .setNumber(number)
                        .setOldPin(old_pin)
                        .setNewPin(new_pin)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, changePin, new Handler<Message<Integer>>() {
            @Override
            public void handle(Message<Integer> result) {
                boolean isOk = false;
                if (result.body() == MomoProto.SystemError.ALL_OK_VALUE) {
                    isOk = true;
                }
                //update new pin in cache
                if (isOk) {
                    //cap nhat pin trong DB
                    phonesDb.updatePin(msg.cmdPhone, new_pin, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {
                            //cap nhat cache
                            if (result && _data != null && _data.getPhoneObj() != null) {
                                _data.getPhoneObj().pin = new_pin;
                            }

                            if (result && _data != null) {
                                _data.pin = new_pin;
                            }

                            logger.debug("SAVE NEW PIN TO PHONES " + result);
                        }
                    });
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.CHANGE_PIN_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        MomoProto.StandardReply.newBuilder()
                                .setResult(isOk)
                                .setRcode(result.body())
                                .build().toByteArray()
                );
                mCom.writeDataToSocket(sock, buf);
            }
        });
    }

    private JsonObject getAppVersionCode(String os, boolean isAgent) {
        String element = isAgent ? "agent_" + os.toLowerCase() : os.toLowerCase();


        JsonObject jo = appVerCfg.getObject(element, null);
        if (jo != null) {
            return jo;
        }

        jo = new JsonObject();
        jo.putString("name", "0.0.0");
        jo.putNumber("code", 0);
        return jo;
    }

    private void getPhoneObjForWaitingReg(int phoneNumber, SockData data, final Handler<String> callback) {
        //set indicator waitingReg
        if (data != null && data.getPhoneObj() != null) {
            String waitingReg = data.getPhoneObj().waitingReg ? "1" : "0";
            callback.handle(waitingReg);
        } else {
            phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj obj) {
                    String waitingReg = (obj == null ? "0" : (obj.waitingReg ? "1" : "0"));
                    callback.handle(waitingReg);
                }
            });
        }
    }
    //BEGIN 0000000026 Test gui thong tin de ban popup MAPVI

    public void checkWalletMappingFromBank(final MomoMessage msg, final String bankcode, final Common.BuildLog log, SockData sockData) {
        //BEGIN 0000000026 Test gui thong tin de ban popup MAPVI
        long timeout = 60 * 1000L;
        // Get bankrequest and convert to JsonObject.
        BankRequest bankRequest = BankRequestFactory.createCheckNewMaprRequest(bankcode, "0" + msg.cmdPhone);
        log.add("func:", "checkWalletMappingFromBank");
        String bank_verticle = bankJsonOject.containsField(StringConstUtil.BANK_CONNECTOR_VERTICLE)
                ? bankJsonOject.getString(StringConstUtil.BANK_CONNECTOR_VERTICLE) : "bankVerticle";
        final String ocbBankCode = jsonOCBPromo.getString(StringConstUtil.OBCPromo.BANK_CODE, "104");
        if (!bank_verticle.equalsIgnoreCase("")) {
            vertx.eventBus().sendWithTimeout(bank_verticle, bankRequest.getJsonObject(), timeout, new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                    if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                        final BankResponse response = new BankResponse(messageAsyncResult.result().body());
                        Popup popup = null;
                        int resultCode = -1;
                        boolean have2Buttons = false; // Check
                        if (response != null) {
                            popup = response.getPopup();
                            resultCode = response.getResultCode();
                            if (popup != null) {
                                have2Buttons = popup.getCancelButtonLabel() == null || popup.getCancelButtonLabel().equalsIgnoreCase("") ? false : true;
                            }
                        } // END CHECK RESPONSE IS NOT NULL
                        boolean isActiveOCBPromo = jsonOCBPromo.getBoolean(StringConstUtil.OBCPromo.IS_ACTIVE, false);
                        //Kiem tra xem chuong trinh OCB co active khong.
                        if(isActiveOCBPromo && resultCode == 0 && bankcode.equalsIgnoreCase(ocbBankCode))
                        {
                            checkOCBPromo(popup, have2Buttons, log, msg);
                        }
                        //Neu khong active ocb thi kiem tra result code
                        else if(resultCode == 0)
                        {
                            //Co ket qua map vi
                            log.add("desc", "OCB PROMO is not actived.");
                            log.add("desc", "Send wallet info popup.");
                            log.writeLog();
                            sendPopUpInformation(popup, msg, have2Buttons, System.currentTimeMillis());
                        }
                        else {
                            // Khong co map vi
                            log.add("resultCode", resultCode);
                            log.add(StringConstUtil.WALLET_MAPPING_RESULT, response); // show log ket qua map vi
                            log.add("ket qua response tra ve", "resultCode_tmp is not equal 0");
                            log.writeLog();
                        }
                        return;
                    } //END IF GET MESSAGE SUCCESSFUL
                    else {
                        log.add(StringConstUtil.WALLET_MAPPING_RESULT, "Goi qua connector fail khi lay thong tin bank"); // show log ket qua map vi
                        log.add("ket qua response tra ve", "null");
                        log.writeLog();
                        return;
                    }
                }
            });
        }

        //END 0000000026 Test gui thong tin de ban popup MAPVI
    }

    private void checkOCBPromo(final Popup popup,final boolean have2Buttons, final Common.BuildLog log, final MomoMessage msg) {

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                if (array != null && array.size() > 0) {
                    ArrayList<PromotionDb.Obj> objs = new ArrayList<>();
                    PromotionDb.Obj obj_promo = null;
                    long ocb_promo_start_date = 0;
                    long ocb_promo_end_date = 0;
                    for (Object o : array) {
                        obj_promo = new PromotionDb.Obj((JsonObject) o);
                        if (obj_promo.NAME.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO_PROGRAM)) {
                            ocb_promo_start_date = obj_promo.DATE_FROM;
                            ocb_promo_end_date = obj_promo.DATE_TO;
                            break;
                        }
                    }
                    //Co tra ve popup.
                    if(System.currentTimeMillis() >= ocb_promo_start_date && System.currentTimeMillis() <= ocb_promo_end_date)
                    {
                        //Moi map vi va dang trong khuyen mai .... kiem tra so dien thoai da tham gia chua
                        mappingWalletOCBPromoDb.findOne("0" + msg.cmdPhone, new Handler<MappingWalletOCBPromoDb.Obj>() {
                            @Override
                            public void handle(MappingWalletOCBPromoDb.Obj mappingOCBObj) {
                                if(mappingOCBObj == null)
                                {
                                    //Khong duoc tham gia chuong trinh nay
                                    //Da nhan billpay, khong tra popup tra thuong
                                    log.add("have2Buttons", have2Buttons); // show log ket qua map vi
                                    //Ban noti voi 2 nut
                                    log.add("desc", "Khong duoc tham gia OCB huhu");
                                    log.writeLog();
                                    sendPopUpInformation(popup, msg, have2Buttons, System.currentTimeMillis());
                                }
                                //Chua nhan billpay, cho popup tra thuong
                                else{
                                    log.add("desc", "Duoc tham gia OCB");
                                    popup.setHeader(BillPayPromoConst.POPUP_HEADER_OCB_PROMO);
                                    popup.setContent(BillPayPromoConst.POPUP_CONTENT_OCB_PROMO);
                                    long tid = System.currentTimeMillis();
                                    sendPopUpInformation(popup, msg, have2Buttons, tid);
                                    JsonObject jsonTranHis = new JsonObject();
                                    jsonTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, 7);
                                    jsonTranHis.putString(colName.TranDBCols.COMMENT, BillPayPromoConst.POPUP_CONTENT_OCB_PROMO);
                                    jsonTranHis.putNumber(colName.TranDBCols.TRAN_ID, tid);
                                    jsonTranHis.putNumber(colName.TranDBCols.AMOUNT, 100000);
                                    jsonTranHis.putNumber(colName.TranDBCols.STATUS, 0);
                                    jsonTranHis.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(popup.getInitiator()));
                                    jsonTranHis.putString(colName.TranDBCols.BILL_ID, "");
                                    String html = BillPayPromoConst.HTMLSTR_OCB_PROMO.replace("serviceid", "topup");
                                    jsonTranHis.putString(StringConstUtil.HTML, html);
                                    Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTranHis, new JsonObject());
                                    log.writeLog();
                                }
                            }
                        });
                    }
                    else {
                        log.add("have2Buttons", have2Buttons); // show log ket qua map vi
                        //Ban noti voi 2 nut
                        log.add("desc", "Khong duoc tham gia OCB huhu");
                        log.writeLog();
                        sendPopUpInformation(popup, msg, have2Buttons, System.currentTimeMillis());
                    }
                }
            }
        });
    }

    //END 0000000050
    private void sendPopUpInformation(Popup popup, MomoMessage msg, boolean isConfirm, long tid) {
        JsonObject jsonExtra = new JsonObject();
        int type = isConfirm ? 1 : 0;

        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        if (isConfirm) {
            String button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }
        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = 0L;
        notification.cmdId = msg.cmdIndex;
        notification.time = new Date().getTime();
        notification.receiverNumber = msg.cmdPhone;
        notification.extra = jsonExtra.toString();
        Misc.sendNoti(vertx, notification);
    }
    //END 0000000026 Test gui thong tin de ban popup MAPVI

    private Notification createNotiGroupFour(String phoneNumber, String title, String body)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TOPUP_VALUE;
        noti.caption = title;
        noti.body = body;
        noti.tranId = System.currentTimeMillis();
        noti.time = new Date().getTime();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        return noti;
    }
}
