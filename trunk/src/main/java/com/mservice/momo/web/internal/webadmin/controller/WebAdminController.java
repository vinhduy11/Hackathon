package com.mservice.momo.web.internal.webadmin.controller;

import com.mongodb.*;
import com.mservice.momo.data.*;
import com.mservice.momo.data.codeclaim.ClaimCodePromotionDb;
import com.mservice.momo.data.codeclaim.ClaimCodePromotionObj;
import com.mservice.momo.data.codeclaim.ClaimCode_AllCheckDb;
import com.mservice.momo.data.codeclaim.ClaimCode_CodeCheckDb;
import com.mservice.momo.data.connector.ConnectorProxyBusNameDb;
import com.mservice.momo.data.customercaregiftgroup.DollarHeartCustomerCareGiftGroupDb;
import com.mservice.momo.data.customercaregiftgroup.DollarHeartCustomerCareGiftGroupObj;
import com.mservice.momo.data.discount50percent.RollBack50PerPromoObj;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.gift.QueuedGiftDb;
import com.mservice.momo.data.ironmanpromote.IronManBonusTrackingTableDb;
import com.mservice.momo.data.ironmanpromote.IronManRandomGiftManageDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.popup.EmailPopupDb;
import com.mservice.momo.data.servicegiftrule.CheckServiceGiftRuleDb;
import com.mservice.momo.data.zalo.ZaloTetPromotionDb;
import com.mservice.momo.data.zalo.ZaloTetPromotionObj;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.gateway.external.vng.FilmInfo;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.SoapInProcess;
import com.mservice.momo.gateway.internal.visamaster.VMCardType;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.CodeUtil;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.ServiceUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoObj;
import com.mservice.momo.vertx.customercare.CustomCareObj;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.gift.models.QueuedGift;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.ConnectProcess;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.TransferProcess;
import com.mservice.momo.vertx.vcb.ReqObj;
import com.mservice.momo.vertx.vcb.VcbCommon;
import com.mservice.momo.vertx.visampointpromo.VisaMpointPromoConst;
import com.mservice.momo.vertx.visampointpromo.VisaMpointPromoObj;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import com.mservice.momo.web.internal.webadmin.objs.*;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by locnguyen on 22/07/2014.
 */
public class WebAdminController {

    public static String IMAGE_DIR_PATH = "/tmp";
    public static String IMAGE_STORAGE_HOST = "localhost";
    public static HashMap<Long, UserWebAdminDb.Obj> listUser = new HashMap<Long, UserWebAdminDb.Obj>();
    private static long TIME_OUT = 10 * 60 * 1000; // ms
    public JsonObject joMongo;
    public boolean isUAT;
    protected CodeUtil codeUtilGLX = new CodeUtil(4, 5);
    protected CodeUtil glxUtil = new CodeUtil(4, 5);
    protected Vertx vertx;
    protected Logger logger;
    protected UserWebAdminDb userDb;
    protected WholeSystemPauseDb wholeSystemPauseDb;
    protected PhonesDb phonesDb;
    protected PromoTrackDb promoTrackDb;
    protected Promo123PhimDb promo123PhimDb;
    protected Promo123PhimGlxDb promo123PhimGlxDb;
    protected TransDb transDb;
    protected CdhhConfigDb cdhhConfigDb;
    protected String STATIC_FILE_DIRECTORY = "";
    protected CDHH cdhh;
    protected Promo123PhimGlxDb phim123Glx;
    protected GiftManager giftManager;
    protected VcbCmndRecs vcbCmndRecs;
    protected CardTypeDb cardTypeDb;
    protected AgentsDb agentsDb;
    protected boolean isRunning = false;
    GiftDb giftDb = null;
    QueuedGiftDb queuedGiftDb = null;
    TransferProcess transferProcess;
    EmailPopupDb emailPopupDb;
    ControlOnClickActivityDb controlOnClickActivityDb;
    //    @Action(path = "/readCustomerCareDollarHeartGroupFile")
//    public void readCustomerCareDollarHeartGroupFile(HttpRequestContext context, final Handler<JsonArray> callback) {
//        MultiMap params = context.getRequest().params();
//        final Common.BuildLog log = new Common.BuildLog(logger);
//        logger.info("address: " + context.getRequest().remoteAddress().getAddress());
//
//        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
//        log.add("hFile", hFile);
//
//        String[] ar = hFile.split("\\\\");
//        hFile = ar[ar.length - 1];
//
//        String filePath = STATIC_FILE_DIRECTORY + hFile;
//
//        log.add("filePath", filePath);
//        logger.info("filePath: " + filePath);
//        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
//
//        final Queue<ReturnGiftRow> returnGiftRowQueue = new ArrayDeque<>();
//        for (Object o : arrayList) {
//            returnGiftRowQueue.add(new ReturnGiftRow(o.toString()));
//        }
//
//        final JsonArray arrayResult = new JsonArray();
//
//        if (returnGiftRowQueue.size() > 1) {
//            ReturnGiftRow returnGiftRow = returnGiftRowQueue.poll();
//        }
//
//        //start to process send gift as promotion
//        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
//        int totalRows = returnGiftRowQueue.size();
//        doReturnCustomerCareDollarHeartGroupGift(returnGiftRowQueue, arrayResult, totalRows, callback);
//        log.writeLog();
//    }
    JsonArray arrayResult;
    NotificationDb notificationDb;
    String agent = "";
    String password = "";
    String code = "";
    private CustomCareDb customerCareDb;
    private BillPayPromoErrorDb billPayPromoErrorDb;
    private JsonObject glbConfig;
    private IronManBonusTrackingTableDb ironManBonusTrackingTableDb;
    private IronManRandomGiftManageDb ironManRandomGiftManageDb;
    private boolean sendByZalo;
    private ZaloTetPromotionDb zaloTetPromotionDb;
    private String zaloSaveMongoTable;
    private String zaloSaveMongoHost;
    private DollarHeartCustomerCareGiftGroupDb dollarHeartCustomerCareGiftGroupDb;
    private LixiManageDb lixiManageDb;
    private ConnectorProxyBusNameDb connectorProxyBusNameDb;
    private CheckServiceGiftRuleDb checkServiceGiftRuleDb;
    private ClaimCodePromotionDb claimCodePromotionDb;

    ///////////////////////////////////////////////////////////////

    //viettinbank.start
    private ClaimCode_AllCheckDb claimCode_allCheckDb;
    private ClaimCode_CodeCheckDb claimCode_codeCheckDb;

    public WebAdminController(Vertx vertx, Container container, String STATIC_FILE_DIRECTORY) {
        this.vertx = vertx;
        this.glbConfig = container.config();
        logger = container.logger();
        userDb = new UserWebAdminDb(vertx.eventBus(), logger);
        wholeSystemPauseDb = new WholeSystemPauseDb(vertx.eventBus(), logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        promoTrackDb = new PromoTrackDb(vertx.eventBus(), logger);
        promo123PhimDb = new Promo123PhimDb(vertx.eventBus(), logger);
        promo123PhimGlxDb = new Promo123PhimGlxDb(vertx.eventBus(), logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbConfig);
        cdhhConfigDb = new CdhhConfigDb(vertx, logger);
        this.STATIC_FILE_DIRECTORY = STATIC_FILE_DIRECTORY;
        giftDb = new GiftDb(vertx, logger);
        queuedGiftDb = new QueuedGiftDb(vertx, logger);
        cdhh = new CDHH(vertx, logger);
        phim123Glx = new Promo123PhimGlxDb(vertx.eventBus(), logger);
        giftManager = new GiftManager(vertx, logger, container.config());
        vcbCmndRecs = new VcbCmndRecs(vertx.eventBus(), logger);
        cardTypeDb = new CardTypeDb(vertx, logger);
        customerCareDb = new CustomCareDb(vertx, logger);
        billPayPromoErrorDb = new BillPayPromoErrorDb(vertx, logger);
        this.agentsDb = new AgentsDb(vertx.eventBus(), logger);
        ironManBonusTrackingTableDb = new IronManBonusTrackingTableDb(vertx, logger);
        ironManRandomGiftManageDb = new IronManRandomGiftManageDb(vertx,logger);
        sendByZalo = glbConfig.getObject(StringConstUtil.ZaloSMS.JSON_OBJECT, new JsonObject()).getBoolean(StringConstUtil.ZaloSMS.SEND_BY_ZALO, false);
        zaloTetPromotionDb = new ZaloTetPromotionDb(vertx, logger);
        zaloSaveMongoTable = glbConfig.getObject("zaloSaveMongo", new JsonObject()).getString("table", "zalo_group_two");
        zaloSaveMongoHost = glbConfig.getObject("zaloSaveMongo", new JsonObject()).getString("host", "172.16.14.35");
        transferProcess = new TransferProcess(vertx, logger, glbConfig, ServerVerticle.MapTranRunning);
        dollarHeartCustomerCareGiftGroupDb = new DollarHeartCustomerCareGiftGroupDb(vertx, logger);
        lixiManageDb = new LixiManageDb(vertx, logger);
        connectorProxyBusNameDb = new ConnectorProxyBusNameDb(vertx, logger);
        checkServiceGiftRuleDb = new CheckServiceGiftRuleDb(vertx, logger);
        claimCodePromotionDb = new ClaimCodePromotionDb(vertx, logger);
        claimCode_allCheckDb = new ClaimCode_AllCheckDb(vertx, logger);
        claimCode_codeCheckDb =  new ClaimCode_CodeCheckDb(vertx, logger);
        notificationDb = DBFactory.createNotiDb(vertx, logger, glbConfig);
        emailPopupDb = new EmailPopupDb(vertx, logger);
        joMongo = glbConfig.getObject("mongo", new JsonObject());
        isUAT = glbConfig.getBoolean(StringConstUtil.IS_UAT, false);
        controlOnClickActivityDb = new ControlOnClickActivityDb(vertx);
    }

    public static String checkSession(HttpRequestContext context
            , final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        long id = DataUtil.stringToUNumber(params.get("id"));

        if (id < 1 || !listUser.containsKey(id)
                || listUser.get(id).lastActTime + TIME_OUT < System.currentTimeMillis()) {
            if (callback != null) {
                callback.handle(
                        new JsonObject()
                                .putString("error", "-1")
                                .putString("desc", "Must login")
                );
            }
            return "";
        }
        listUser.get(id).lastActTime = System.currentTimeMillis();
        return listUser.get(id).username;

    }

    public void notifyUpdateCdhhConfig() {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.UPDATE_CDHH_CONFIG_WEEK_OR_AQUATER;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
    }

    @Action(path = "/uploadVtbOnline")
    public void replyVtbUploadVtpOnline(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/readVtbOnline")
    public void readVtbOnline(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<VtbRow> vtbRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            vtbRowQueue.add(new VtbRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (vtbRowQueue.size() > 1) {
            VtbRow vtbRow = vtbRowQueue.poll();
            arrayResult.add(vtbRow.toJson());
        }

        //start to process send gift as promotion
        doVtbSaveCmnd(vtbRowQueue, arrayResult, callback);
        log.writeLog();
    }

    @Action(path = "/readFile")
    public void readerFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<VtbRow> vtbRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            vtbRowQueue.add(new VtbRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (vtbRowQueue.size() > 1) {
            VtbRow vtbRow = vtbRowQueue.poll();
            arrayResult.add(vtbRow.toJson());
        }

        //start to process send gift as promotion
        doVtbPromotion(vtbRowQueue, arrayResult, callback);

        log.writeLog();
    }

    private void doVtbSaveCmnd(final Queue<VtbRow> vtbRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {
        if (vtbRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }
        final VtbRow rec = vtbRowQueue.poll();
        if (rec == null) {
            doVtbSaveCmnd(vtbRowQueue, arrayResult, callback);
        } else {
            final int phoneNumber = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.number) + "");

            if (phoneNumber > 0) {
                vcbCmndRecs.findOne(rec.cmnd, new Handler<VcbCmndRecs.Obj>() {
                    @Override
                    public void handle(VcbCmndRecs.Obj obj) {

                        //chua co trong CMND t
                        if (obj == null) {
                            VcbCmndRecs.Obj vcbObj = new VcbCmndRecs.Obj();
                            vcbObj.number = rec.number;
                            vcbObj.bankcode = rec.bankcode;
                            vcbObj.cardid = rec.cmnd;
                            vcbObj.promocount = 0;
                            vcbObj.timevn = Misc.dateVNFormatWithTime(System.currentTimeMillis());

                            vcbCmndRecs.insert(vcbObj, new Handler<Integer>() {
                                @Override
                                public void handle(Integer error) {

                                    //save not cmnd table successfully
                                    if (error != 0) {
                                        rec.error = "101";
                                        rec.desc = "Không thêm được CMND";
                                        arrayResult.add(rec.toJson());
                                        doVtbSaveCmnd(vtbRowQueue, arrayResult, callback);
                                    } else {

                                        //save to cmnd table successfully
                                        //cap nhat thong tin khuyen mai
                                        JsonObject joUp = new JsonObject();
                                        joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, rec.cmnd);
                                        joUp.putString(colName.PhoneDBCols.BANK_CODE, rec.bankcode);
                                        joUp.putString(colName.PhoneDBCols.BANK_NAME, rec.bankname);
                                        phonesDb.updatePartial(DataUtil.strToInt(rec.number)
                                                , joUp
                                                , new Handler<PhonesDb.Obj>() {
                                            @Override
                                            public void handle(PhonesDb.Obj obj) {
                                                rec.error = "0";
                                                rec.desc = "thành công";
                                                arrayResult.add(rec.toJson());
                                                doVtbSaveCmnd(vtbRowQueue, arrayResult, callback);
                                            }
                                        });
                                    }
                                }
                            });

                        } else {

                            //not promoted for this card id
                            if (obj.promocount == 0) {
                                //cap nhat thong tin khuyen mai
                                JsonObject joUp = new JsonObject();
                                joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, rec.cmnd);
                                joUp.putString(colName.PhoneDBCols.BANK_CODE, rec.bankcode);
                                joUp.putString(colName.PhoneDBCols.BANK_NAME, rec.bankname);
                                phonesDb.updatePartial(DataUtil.strToInt(rec.number)
                                        , joUp
                                        , new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(PhonesDb.Obj obj) {
                                        rec.error = "0";
                                        rec.desc = "thành công";
                                        arrayResult.add(rec.toJson());
                                        doVtbSaveCmnd(vtbRowQueue, arrayResult, callback);
                                    }
                                });
                            } else {

                                //promoted already
                                rec.error = "1000";
                                rec.desc = "Đã khuyến mãi cho số điện thoại " + obj.number;
                                arrayResult.add(rec.toJson());
                                doVtbSaveCmnd(vtbRowQueue, arrayResult, callback);
                            }
                        }
                    }
                });

            } else {
                //cap nhat ket qua so dien thoai khong hop le
                rec.error = "1000";
                rec.desc = "SDT không hợp lệ";
                arrayResult.add(rec.toJson());
                doVtbSaveCmnd(vtbRowQueue, arrayResult, callback);
            }
        }
    }

    private void doVtbPromotion(final Queue<VtbRow> vtbRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {

        if (vtbRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }
        //lay ra doi tuong VtbRow dau tien trong queue (co nghia la doi tuong dau tien se khong con o trong queue nua)
        final VtbRow rec = vtbRowQueue.poll();

        if (rec == null) {
            doVtbPromotion(vtbRowQueue, arrayResult, callback);
        } else {

            final int phoneNumber = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.number) + "");

            if (phoneNumber > 0) {

                JsonObject fields = new JsonObject();
                fields.putNumber("_id", 0);
                fields.putNumber(colName.PhoneDBCols.NUMBER, 1);
                fields.putNumber(colName.PhoneDBCols.INVITER, 1);
                phonesDb.getPhoneObjWithFields(phoneNumber, fields, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(final PhonesDb.Obj phoneObj) {

                        //not exist on phones table
                        if (phoneObj == null) {

                            rec.error = "213";
                            rec.desc = "Không có ví trên bảng phone";
                            arrayResult.add(rec.toJson());
                            doVtbPromotion(vtbRowQueue, arrayResult, callback);

                            // da co nguoi gioi thieu
                        } else if (!"".equalsIgnoreCase(phoneObj.inviter)) {
                            rec.error = "214";
                            rec.desc = "NGT hiện tại " + phoneObj.inviter;
                            arrayResult.add(rec.toJson());
                            doVtbPromotion(vtbRowQueue, arrayResult, callback);

                        } else {

                            //goi khuyen mai cho viettinbank
                            //1. save bang cmnd
                            final VcbCmndRecs.Obj cmndObj = new VcbCmndRecs.Obj();
                            cmndObj.cardid = rec.cmnd;
                            cmndObj.promocount = 0;
                            cmndObj.number = rec.number;
                            cmndObj.bankcode = rec.bankcode;
                            cmndObj.timevn = Misc.dateVNFormatWithTime(System.currentTimeMillis());

                            vcbCmndRecs.findOne(rec.cmnd, new Handler<VcbCmndRecs.Obj>() {
                                @Override
                                public void handle(VcbCmndRecs.Obj obj) {
                                    //chua co record tren bang CMND
                                    if (obj == null) {
                                        vcbCmndRecs.insert(cmndObj, new Handler<Integer>() {
                                            @Override
                                            public void handle(Integer integer) {
                                                if (integer == 0) {
                                                    //goi tra thuong
                                                    VcbCommon.requestGiftMomoViettinbank(vertx
                                                            , DataUtil.strToInt(rec.number)
                                                            , "vtbmomo"
                                                            , 0
                                                            , 1
                                                            , 0
                                                            , rec.cmnd
                                                            , rec.bankcode
                                                            , true
                                                            , 0
                                                            , "vnpthn"
                                                            , "vtbpromo", true, ReqObj.offline, "vnpthn", new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject result) {
                                                            rec.error = result.getInteger("error", 0) + "";
                                                            rec.desc = result.getString("desc", result.getString("desc", "not set"));
                                                            arrayResult.add(rec.toJson());

                                                            //tra thuong thanh cong
                                                            if (result.getInteger("error", -1) == 0) {
                                                                //cap nhat thong tin uu dai
                                                                JsonObject joUp = new JsonObject();
                                                                joUp.putNumber(colName.PhoneDBCols.NUMBER, phoneNumber);
                                                                joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, rec.cmnd);
                                                                joUp.putString(colName.PhoneDBCols.INVITER, "vnpthn");
                                                                joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, System.currentTimeMillis());

                                                                //thong tin mapvi viettinbank
                                                                joUp.putString(colName.PhoneDBCols.BANK_NAME, "Viettinbank");
                                                                joUp.putString(colName.PhoneDBCols.BANK_CODE, "102");

                                                                phonesDb.updatePartial(phoneNumber, joUp, new Handler<PhonesDb.Obj>() {
                                                                    @Override
                                                                    public void handle(PhonesDb.Obj obj) {
                                                                        doVtbPromotion(vtbRowQueue, arrayResult, callback);
                                                                    }
                                                                });
                                                            } else {
                                                                doVtbPromotion(vtbRowQueue, arrayResult, callback);
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    rec.error = integer + "";
                                                    rec.desc = "Thêm dữ liệu vào bảng CMND lỗi";
                                                    arrayResult.add(rec.toJson());
                                                    doVtbPromotion(vtbRowQueue, arrayResult, callback);
                                                }
                                            }
                                        });
                                    } else {
                                        //da tra thuong cho cmnd nay
                                        if (obj.promocount >= 1) {
                                            //cap nhat ket qua da tra thuong roi
                                            rec.error = "210";
                                            rec.desc = "Đã trả thưởng cho CMND: " + rec.cmnd;
                                            arrayResult.add(rec.toJson());
                                            doVtbPromotion(vtbRowQueue, arrayResult, callback);

                                        } else {
                                            //goi tra thuong
                                            VcbCommon.requestGiftMomoViettinbank(vertx
                                                    , DataUtil.strToInt(rec.number)
                                                    , "vtbmomo"
                                                    , 0
                                                    , 1
                                                    , 0
                                                    , rec.cmnd
                                                    , rec.bankcode
                                                    , true
                                                    , 0
                                                    , "vnpthn"
                                                    , "vtbpromo", true, ReqObj.offline, "vnpthn", new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject result) {
                                                    rec.error = result.getInteger("error", 0) + "";
                                                    rec.desc = result.getString("desc", result.getString("desc", "not set"));
                                                    arrayResult.add(rec.toJson());

                                                    //tra thuong thanh cong
                                                    if (result.getInteger("error", -1) == 0) {
                                                        //cap nhat thong tin uu dai
                                                        JsonObject joUp = new JsonObject();
                                                        joUp.putNumber(colName.PhoneDBCols.NUMBER, phoneNumber);
                                                        joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, rec.cmnd);
                                                        joUp.putString(colName.PhoneDBCols.INVITER, "vnpthn");
                                                        joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, System.currentTimeMillis());

                                                        //thong tin mapvi viettinbank
                                                        joUp.putString(colName.PhoneDBCols.BANK_NAME, "Viettinbank");
                                                        joUp.putString(colName.PhoneDBCols.BANK_CODE, "102");

                                                        phonesDb.updatePartial(phoneNumber, joUp, new Handler<PhonesDb.Obj>() {
                                                            @Override
                                                            public void handle(PhonesDb.Obj obj) {
                                                                doVtbPromotion(vtbRowQueue, arrayResult, callback);
                                                            }
                                                        });
                                                    } else {
                                                        doVtbPromotion(vtbRowQueue, arrayResult, callback);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });

                        }
                    }
                });
            } else {

                //cap nhat ket qua so dien thoai khong hop le
                rec.error = "1000";
                rec.desc = "SDT không hợp lệ";
                arrayResult.add(rec.toJson());
                doVtbPromotion(vtbRowQueue, arrayResult, callback);
            }
        }
    }

    @Action(path = "/service/cdhh/getAll")
    public void getAll(HttpRequestContext context, final Handler<Object> callback) {
        CdhhConfig filter = new CdhhConfig();
        cdhhConfigDb.find(filter, 10000, new Handler<List<CdhhConfig>>() {
            @Override
            public void handle(List<CdhhConfig> result) {
                JsonArray arr = new JsonArray();
                for (CdhhConfig config : result) {
                    arr.add(config.toJsonObject());
                }
                callback.handle(arr);
            }
        });
    }

    @Action(path = "/service/cdhh/update")
    public void updateCdhh(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String cdhhConfigId = params.get("cdhhConfigId");
        if (cdhhConfigId == null || cdhhConfigId.trim().isEmpty())
            cdhhConfigId = null;

        String serviceId = params.get("serviceId");
        String periodName = params.get("periodName");
        String collName = params.get("collName");
        String startTime = params.get("startTime");
        String endTime = params.get("endTime");
        String active = params.get("active");

        String report = params.get("report");

        int codeMin = params.get("codeMin") != null ? DataUtil.strToInt(params.get("codeMin")) : 0;
        int codeMax = params.get("codeMax") != null ? DataUtil.strToInt(params.get("codeMax")) : 0;


        CdhhConfig cdhhConfig = new CdhhConfig();
        try {
            cdhhConfig.setModelId(cdhhConfigId);
            cdhhConfig.serviceId = serviceId;
            cdhhConfig.periodName = periodName;
            cdhhConfig.collName = collName;
            cdhhConfig.startTime = Long.parseLong(startTime);
            cdhhConfig.endTime = Long.parseLong(endTime);
            cdhhConfig.active = "true".equalsIgnoreCase(active);
            cdhhConfig.minCode = codeMin;
            cdhhConfig.maxCode = codeMax;
            cdhhConfig.report = "true".equalsIgnoreCase(report);
        } catch (Exception e) {
            callback.handle(new JsonObject().putNumber("error", 1).putString("desc", e.getMessage()));
            return;
        }

        if (cdhhConfigId == null) {
            cdhhConfigDb.save(cdhhConfig, new Handler<String>() {
                @Override
                public void handle(String event) {
                    callback.handle(new JsonObject().putNumber("error", 0));
                }
            });
            return;
        }
        cdhhConfigDb.update(cdhhConfig, false, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(new JsonObject().putNumber("error", 0));
                notifyUpdateCdhhConfig();
            }
        });

    }

    //BEGIN 0000000052
    @Action(path = "/service/ironman/getAll")
    public void getIronManTrackingTableData(HttpRequestContext context, final Handler<Object> callback) {
        JsonObject filter = new JsonObject();
        ironManBonusTrackingTableDb.searchWithFilter(filter, new Handler<ArrayList<IronManBonusTrackingTableDb.Obj>>() {
            @Override
            public void handle(ArrayList<IronManBonusTrackingTableDb.Obj> event) {
                JsonArray arr = new JsonArray();
                for (IronManBonusTrackingTableDb.Obj obj : event) {
                    arr.add(obj.toJson());
                }
                callback.handle(arr);
            }
        });

    }

    @Action(path = "/service/ironman/update")
    public void updateIronMan(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
//        String ironmanId = params.get("ironmanId");
//        if (ironmanId == null || ironmanId.trim().isEmpty())
//            ironmanId = null;

        String program = params.get("program");
        int programNumber = params.get("program_number") != null ? DataUtil.strToInt(params.get("program_number")) : 0;
        int numerator = params.get("numerator") != null ? DataUtil.strToInt(params.get("numerator")) : 0;
        int denominator = params.get("denominator")  != null ? DataUtil.strToInt(params.get("denominator")) : 0;
        String start_time = params.get("start_time");
        String end_time = params.get("end_time");


        String chkNotRatio = params.get("chkNotRatio");

        int minRatio = params.get("minRatio") != null ? DataUtil.strToInt(params.get("minRatio")) : 0;
        int maxRatio = params.get("maxRatio") != null ? DataUtil.strToInt(params.get("maxRatio")) : 0;
        int givenman = params.get("givenman") != null ? DataUtil.strToInt(params.get("givenman")) : 0;


        IronManBonusTrackingTableDb.Obj ironManBonusTrackingObj = new IronManBonusTrackingTableDb.Obj();

        try {
            ironManBonusTrackingObj.program = program;
            ironManBonusTrackingObj.numerator = numerator;
            ironManBonusTrackingObj.denominator = denominator;
            ironManBonusTrackingObj.start_time = Long.parseLong(start_time);
            ironManBonusTrackingObj.end_time = Long.parseLong(end_time);
            ironManBonusTrackingObj.not_ratio_flag = chkNotRatio.equalsIgnoreCase("true");
            ironManBonusTrackingObj.min_ratio = minRatio;
            ironManBonusTrackingObj.max_ratio = maxRatio;
            ironManBonusTrackingObj.number_of_bonus_gave_man = givenman;

        } catch (Exception e) {
            callback.handle(new JsonObject().putNumber("error", 1).putString("desc", e.getMessage()));
            return;
        }

        JsonObject joUpdate = ironManBonusTrackingObj.toJson();
        joUpdate.removeField(colName.IronManBonusTrackingTable.PROGRAM_NUMBER);
        joUpdate.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN);
        joUpdate.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER);
        ironManBonusTrackingTableDb.upsertPartial(programNumber, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                callback.handle(new JsonObject().putNumber("error", 0));
                notifyUpdateIronManConfig();
            }
        });

    }

    public void notifyUpdateIronManConfig() {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.REFRESH_IRON_MAN_TRACKING_TABLE;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_PrefixUpdate, serviceReq.toJSON());
    }

    //END 0000000052

    @Action(path = "/service/ironman/refreshconfig")
    public void getConfigIronMan(HttpRequestContext context, final Handler<Object> callback) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.REFRESH_IRON_MAN_TRACKING_TABLE;
        vertx.eventBus().send(AppConstant.ConfigVerticleService_PrefixUpdate, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if(event.body().containsField("error"))
                {
                    callback.handle(event.body());
                }
                else{
                    JsonObject reply = new JsonObject().putNumber("error", -1);
                    callback.handle(reply);
                }
            }
        });
    }

    @Action(path = "/service/ironman/refreshgift")
    public void getGiftIronMan(HttpRequestContext context, final Handler<Object> callback) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.REFRESH_RANDOM_GIFT;
        vertx.eventBus().send(AppConstant.ConfigVerticleService_PrefixUpdate, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if(event.body().containsField("error"))
                {
                    callback.handle(event.body());
                }
                else{
                    JsonObject reply = new JsonObject().putNumber("error", -1);
                    callback.handle(reply);
                }
            }
        });
    }

    @Action(path = "/service/ironman/updaterandomgift")
    public void getRandomIronManGift(HttpRequestContext context, final Handler<Object> callback) {
        context.getRequest();
        MultiMap params = context.getRequest().params();

        /*public static String id ="_id";
        public static String name ="name";
        public static String desc ="desc";
        public static String status ="stat";
        public static String lasttime ="ltime";*/

        final JsonObject joReply = new JsonObject();

        String group = params.get("group");
        String fixedGift = params.get("fixed_gifts");
        String randomGift = params.get("random_gifts");
        int numberOfGift = params.get("number_of_gift") != null ? DataUtil.strToInt(params.get("number_of_gift")) : 0;;

        if("".equalsIgnoreCase(group)){
            joReply.putNumber("error",1);
            joReply.putString("desc","Vui lòng nhập nhập Group");
            callback.handle(joReply);
            return;
        }

        IronManRandomGiftManageDb.Obj randomGiftObj = new IronManRandomGiftManageDb.Obj();
        randomGiftObj.group = group;
        randomGiftObj.fixed_gifts = fixedGift;
        randomGiftObj.random_gifts = randomGift;
        randomGiftObj.number_of_gift = numberOfGift;
        ironManRandomGiftManageDb.upsertRandomGift(group, randomGiftObj.toJson(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                callback.handle(new JsonObject().putNumber("error", 0));
            }
        });
    }

    @Action(path = "/service/ironman/getrandomgift")
    public void getAllRandomGift(HttpRequestContext context, final Handler<Object> callback) {

        JsonObject jsonFilter = new JsonObject();
        ironManRandomGiftManageDb.searchWithFilter(jsonFilter, new Handler<ArrayList<IronManRandomGiftManageDb.Obj>>() {
            @Override
            public void handle(ArrayList<IronManRandomGiftManageDb.Obj> ironmanRandomGiftObj) {
                JsonArray jsonArray = new JsonArray();
                for (int i = 0; i < ironmanRandomGiftObj.size(); i++) {
                    jsonArray.add(ironmanRandomGiftObj.get(i).toJson());
                }
                callback.handle(jsonArray);
            }
        });
    }

    @Action(path = "/service/cdhh/setInactive")
    public void setCdhhInactive(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String cdhhConfigId = params.get("cdhhConfigId");
        String active = params.get("cdhhConfigId");
        if (cdhhConfigId == null || cdhhConfigId.isEmpty()) {
            callback.handle(new JsonObject().putNumber("error", 1).putString("desc", "cdhhConfigId can't be null!"));
            return;
        }
        CdhhConfig config = new CdhhConfig();
        config.setModelId(cdhhConfigId);
        config.active = false;
        cdhhConfigDb.update(config, false, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                callback.handle(new JsonObject().putNumber("error", 0));

                //linh. thieu doan nay
                notifyUpdateCdhhConfig();
            }
        });
    }

    @Action(path = "/service/cdhh/setActive")
    public void setCdhhActive(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String cdhhConfigId = params.get("cdhhConfigId");
        if (cdhhConfigId == null || cdhhConfigId.isEmpty()) {
            callback.handle(new JsonObject().putNumber("error", 1).putString("desc", "cdhhConfigId can't be null!"));
            return;
        }

        CdhhConfig config = new CdhhConfig();
        config.setModelId(cdhhConfigId);

        cdhhConfigDb.findOne(config, new Handler<CdhhConfig>() {
            @Override
            public void handle(final CdhhConfig config) {
                if (config == null) {
                    callback.handle(new JsonObject().putNumber("error", 2));
                    return;
                }

                if (!config.collName.contains("_15")) {

                    cdhhConfigDb.setActive(config.getModelId(), config.serviceId, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {
                            callback.handle(event);
                            notifyUpdateCdhhConfig();
                        }
                    });
                    return;
                }

                config.active = true;
                /*config.startTime = System.currentTimeMillis();
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, 15);
                config.endTime = calendar.getTimeInMillis();*/

                cdhhConfigDb.setActive(config.getModelId(), config.serviceId, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject result) {
                        cdhhConfigDb.update(config, false, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean r) {
                                callback.handle(result);
                                notifyUpdateCdhhConfig();
                            }
                        });
                    }
                });
            }
        });
    }

    @Action(path = "/server/getOTP")
    public void getOTP(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        final int phoneNumber = DataUtil.strToInt(params.get("phone"));
        if (phoneNumber > 0) {
            if(isUAT)
            {
                phonesDb.getOtp(phoneNumber, new Handler<String>() {
                    @Override
                    public void handle(String s) {
                        JsonObject jo = new JsonObject();
                        jo.putNumber("phone", phoneNumber);
                        jo.putString("OTP", s);

                        callback.handle(jo);
                    }
                });
            }
            else {
                JsonObject jo = new JsonObject();
                jo.putNumber("phone", phoneNumber);
                jo.putString("OTP", "BI MAT KHONG DUOC BAT MI ... CHEAT HA MAI");
                callback.handle(jo);
            }
        }
    }

    @Action(path = "/server/update")
    public void serviceUpdate(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "upsert service detail");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        WholeSystemPauseDb.Obj obj = new WholeSystemPauseDb.Obj();
        obj.ID = params.get(colName.WhoSystemPauseCols.ID);
        obj.CAPTION = params.get(colName.WhoSystemPauseCols.CAPTION);
        obj.BODY = params.get(colName.WhoSystemPauseCols.BODY);
        obj.ACTIVED = Boolean.parseBoolean(params.get(colName.WhoSystemPauseCols.ACTIVED));
        obj.LAST_CHANGED = System.currentTimeMillis();
        obj.CHANGED_BY = username;

        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        wholeSystemPauseDb.updateID(obj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "upsert failed");

                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }
                log.writeLog();

                update(Common.ServiceReq.COMMAND.UPDATE_SERVER_ONOFF);

                callback.handle(result);
                return;
            }
        });

    }

    /////////////////////////check status code invite friend///////////////////////////

    @Action(path = "/server/getall")
    public void serverOnOff(HttpRequestContext context, final Handler<JsonObject> callback) {
        wholeSystemPauseDb.getlist(new Handler<ArrayList<WholeSystemPauseDb.Obj>>() {
            @Override
            public void handle(ArrayList<WholeSystemPauseDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTable(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);

                callback.handle(result);
            }
        });

    }

    public String buildTable(ArrayList<WholeSystemPauseDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Caption</th>" +
                "  <th>Body</th>" +
                "  <th>Stopped</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += "<tr>\n" +
                        "<td>" +
                        "<button id= 'edit' _id = '" + objs.get(i).ID + "'" + "val ='" + i + "'>Edit</button>" +
                        "</td>" +
                        "  <td rid='" + i + "'>" + objs.get(i).CAPTION + "</td>" +
                        "  <td rid='" + i + "'>" + objs.get(i).BODY + "</td>" +

                        "  <td rid='" + i + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
                if (objs.get(i).ACTIVED)
                    result += " checked ";
                result += ">" + "</td>"
                        + "</tr>";
            }
        }
        result += "</table>";
        return result;
    }

    @Action(path = "/user/login")
    public void login(final HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        final String username = params.get("username") == null ? "" : params.get("username");
        String password = params.get("password") == null ? "" : params.get("password");

        if ("".equalsIgnoreCase(username) || "".equalsIgnoreCase(password)) {
            callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "Login failed")
            );
            return;
        }

        userDb.getUserInfo(username, password, 1, 10, new Handler<ArrayList<UserWebAdminDb.Obj>>() {
            @Override
            public void handle(ArrayList<UserWebAdminDb.Obj> objs) {
                if (objs == null || objs.size() < 1) {
                    callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "Login failed")
                    );
                    return;
                }

                UserWebAdminDb.Obj user = objs.get(0);
                if (listUser.containsKey(user.sessionId)) {
                    listUser.get(user.sessionId).lastActTime = System.currentTimeMillis();
                } else {
                    user.lastActTime = System.currentTimeMillis();
                    listUser.put(user.sessionId, user);
                }


                callback.handle(new JsonObject()
                        .putNumber("error", 0)
                        .putNumber("id", user.sessionId)
                        .putString("username", user.username)
                        .putString("desc", "Login successful")
                );

                /*if(!"admin".equalsIgnoreCase(username)){
                    context.getRequest().response().sendFile("./html/sendnoticloud.html");
                }*/
            }
        });
    }

    @Action(path = "/")
    public void home(HttpRequestContext context
            , final Handler<JsonObject> callback) {
        HttpServerRequest req = context.getRequest();
        String file = "";
        if (req.path().equals("/")) {
            file = "default.html";
        } else if (!req.path().contains("..")) {
            file = req.path();
        }

        req.response().sendFile("com/mservice/momo/web/internal/webadmin/" + file);
    }
    //invitefriend.end

    //////////////////phim123 statistic///////////

    private void update(int comman) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();

        serviceReq.Command = comman;

        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
    }

    //phim123 statistic.end

    @Action(path = "/phim123/statistic")
    public void phim123Statistic(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "checkStatusInviteFriend");

        MultiMap params = request.params();

        String strDate = "";
        String strRap = "";
        if ("GET".equalsIgnoreCase(request.method())) {
            strDate = params.get("date");
            strRap = params.get("tenRapFull");
        }

        long lDate = Misc.str2BeginDate(strDate);

        //lay thong tin tu DB

        promo123PhimDb.doStatistic(lDate, strRap, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                /*JsonObject jresult = new JsonObject();
                jresult.putNumber("number",numberofCommbo);
                jresult.putNumber("ticketcount",numberofTicket);*/

                JsonObject jo = new JsonObject();
                jo.putNumber("count", jsonObject.getNumber("number", 0));
                jo.putNumber("ticketcount", jsonObject.getNumber("ticketcount", 0));
                response(request, jo);
                return;
            }
        });

    }

    @Action(path = "/invitefriend/createcode")
    public void createCodeInviteFriend(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "createCodeInviteFriend");

        MultiMap params = request.params();
        String data = "";

        final JsonObject jo = new JsonObject();
        String txtNumber = "";

        if ("GET".equalsIgnoreCase(request.method())) {
            txtNumber = params.get("number");
        } else {
            data = context.postData;
        }

        if ("".equalsIgnoreCase(txtNumber)) {
            jo.putString("result", "Vui lòng nhập số điện thoại");
            response(request, jo);
            return;
        }

        //gui request tao ma va nhan ket qua tra ve
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.INVITE_CREATE_CODE;
        promoReqObj.CREATOR = "0" + DataUtil.strToInt(txtNumber);

        if (DataUtil.strToInt(promoReqObj.CREATOR) <= 0) {

            jo.putString("result", "Số điện thoại không hợp lệ");
            response(request, jo);

            log.add("so dien thoai khong hop le", "");
            log.writeLog();
            return;
        }

        log.add("function", "requestPromo");
        log.add("request promo invitefriend", promoReqObj.toJsonObject());
        Common mComm = new Common(vertx, logger, glbConfig);

        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                String result = jsonObject.getString("result", "");
                jo.putString("result", result);
                response(request, jo);

                log.add("generat invite code result", result);
                log.writeLog();
            }
        });
    }

    @Action(path = "/invitefriend/report")
    public void reportInviteFriend(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "reportInviteFriend");

        MultiMap params = request.params();
        String data = "";

        final JsonObject jo = new JsonObject();
        String strDate = "";

        if ("GET".equalsIgnoreCase(request.method())) {
            strDate = params.get("date");
        } else {
            data = context.postData;
        }

        long lDate = Misc.str2BeginDate(strDate);

        promoTrackDb.doStatistic(lDate, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                response(request, jsonObject);
                return;
            }
        });
    }

    //Tool xóa QueuedGift

    //invite friend update status
    @Action(path = "/invitefriend/updatestatus")
    public void updateStatusInviteFriend(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "checkStatusInviteFriend");

        MultiMap params = request.params();
        String data = "";

        final JsonObject jo = new JsonObject();
        String lstCode = "";
        String iStatus = "";
        if ("GET".equalsIgnoreCase(request.method())) {
            lstCode = params.get("lstcode");
            iStatus = params.get("status");
        } else {
            data = context.postData;
        }

        if ("".equalsIgnoreCase(lstCode)) {
            jo.putNumber("number", 0);
            response(request, jo);
            return;
        }
        if ("".equalsIgnoreCase(iStatus)) {
            jo.putNumber("number", 0);
            response(request, jo);
            return;
        }

        String[] arrCode = lstCode.split(";");
        if (arrCode == null || arrCode.length == 0) {
            jo.putNumber("number", 0);
            response(request, jo);
            return;
        }

        String[] finalArrCode = new String[arrCode.length];
        for (int i = 0; i < arrCode.length; i++) {
            finalArrCode[i] = arrCode[i].trim();
        }

        int status = DataUtil.strToInt(iStatus);

        //lay thong tin tu DB

        promoTrackDb.reactiveCode(finalArrCode, status, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                int number = jsonObject.getInteger("number", 0);
                jo.putNumber("number", number);
                response(request, jo);
            }
        });
    }

    //********************thong**************

    @Action(path = "/invitefriend/checkstatus")
    public void checkStatusInviteFriend(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "checkStatusInviteFriend");

        MultiMap params = request.params();
        String code = "";

        final JsonObject jo = new JsonObject();

        if ("GET".equalsIgnoreCase(request.method())) {
            code = params.get("code");
        } else {
            code = context.postData;
        }

        code = code.toUpperCase();
        jo.putString("code", code);

        //check sum first
        if ("".equalsIgnoreCase(code)) {
            jo.putNumber("error", -1);
            jo.putString("desc", "Mã khuyến mãi không hợp lệ");
            response(request, jo);
            return;
        }

        //upper code
        final String finalCode = code;

        if (code.length() != 6) {
            jo.putNumber("error", -1);
            jo.putString("desc", "Mã khuyến mãi phải là chuỗi 6 ký tự");
            /*result += finalCode + "|-1|";
            result += "Mã khuyến mãi phải là chuỗi 6 ký tự";*/
            response(request, jo);

            return;
        }

        //check sum
        /*CodeUtil codeUtil = new CodeUtil(5,6);

        boolean isCheckSumOk = codeUtil.isCodeValid();
        if (!isCheckSumOk) {

            jo.putNumber("error",-1);
            jo.putString("desc","Mã khuyến mãi không hợp lệ");
            response(request, jo);

            *//*result += finalCode + "|-1|";
            result += "Mã khuyến mãi không hợp lệ";
            response(request, result);*//*
            return;
        }
        */
        //lay thong tin tu DB
        promoTrackDb.getOne(code, new Handler<PromoTrackDb.Obj>() {
            @Override
            public void handle(final PromoTrackDb.Obj obj) {

                if (obj == null) {
                    jo.putNumber("error", -1);
                    jo.putString("desc", "Mã khuyến mãi không hợp lệ");
                    response(request, jo);
                    /*result1 += finalCode + "|-1|";
                    result1 += "Mã khuyến mãi không hợp lệ";
                    response(request, result1);*/

                    return;
                }

                if (obj.STATUS == PromoTrackDb.STATUS_PROMOTED) {
                    jo.putNumber("error", -1);
                    jo.putString("desc", "Mã đã được khuyến mãi thành công, Vui lòng kiểm tra lại");
                    jo.putString("for", "0" + obj.EXEC_NUMBER);
                    jo.putString("time", obj.EXEC_TIME);
                    response(request, jo);

                    /*result1 += finalCode + "|-1|";
                    result1 += "Mã đã được khuyến mãi thành công, Vui lòng kiểm tra lại";
                    response(request, result1);*/
                    return;
                }

                if (obj.STATUS == PromoTrackDb.STATUS_NEW) {


                    jo.putNumber("error", 0);
                    jo.putString("creator", "0" + obj.NUMBER);
                    String fdate = Misc.dateVNFormatWithTime(obj.TIME);
                    String tdate = Misc.dateVNFormatWithTime(obj.TIME + 30 * 24 * 60 * 60 * 1000L);

                    jo.putString("fdate", fdate);
                    jo.putString("tdate", tdate);

                    response(request, jo);
                    return;

                } else {
                    jo.putNumber("error", -1);
                    if (obj.STATUS == PromoTrackDb.STATUS_PROMOTED) {
                        jo.putString("desc", "Mã đã khuyến mãi cho 0" + obj.EXEC_NUMBER + " lúc " + obj.EXEC_TIME);
                    }
                    if (obj.STATUS == PromoTrackDb.STATUS_LOCKED) {
                        jo.putString("desc", "Mã đang tạm khóa Người Claim 0" + obj.EXEC_NUMBER + " lúc " + obj.EXEC_TIME);
                    }

                    if (obj.STATUS == PromoTrackDb.STATUS_EXPIRED) {
                        jo.putString("desc", "Mã đã hết hạn");
                    }
                    response(request, jo);
                }
            }
        });

    }

    @Action(path = "/invitefriend/checkgencode")
    public void checkGenCodeInviteFriend(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "reportInviteFriend");

        MultiMap params = request.params();
        String data = "";
        String strNumber = "";
        String strTranId = "";

        final JsonObject jo = new JsonObject();
        String strDate = "";

        if ("GET".equalsIgnoreCase(request.method())) {
            strNumber = params.get("number");
            strTranId = params.get("tranid");
        } else {
            data = context.postData;
        }

        final int finalNumber = DataUtil.strToInt(strNumber);
        final int finalTranId = DataUtil.strToInt(strTranId);

        if (finalNumber == 0 || finalTranId == 0) {

            jo.putString("desc", "Vui lòng nhập SĐT và TranId");

            response(request, jo);
            return;
        }

        transDb.getTransactionDetail(finalNumber, finalTranId, new Handler<TranObj>() {
            @Override
            public void handle(TranObj tranObj) {
                if (tranObj == null) {
                    jo.putString("desc", "Không tồn tại giao dịch này trên hệ thống");
                    response(request, jo);
                    return;
                }

                //todo check them du lieu tu backend hay kenh sim ????

                if (tranObj.cmdId == -1) {
                    jo.putString("desc", "Giao dịch không thực hiện qua kênh backend");
                    response(request, jo);
                    return;

                }

                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.INVITE_FRIEND_GEN_CODE;
                promoReqObj.CREATOR = "0" + finalNumber;
                promoReqObj.TRAN_TYPE = tranObj.tranType;
                promoReqObj.TRAN_AMOUNT = tranObj.amount;
                promoReqObj.RESEND = true;

                log.add("function", "requestPromo");
                log.add("request promo invitefrien", promoReqObj.toJsonObject());
                log.writeLog();

                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                        response(request, jsonObject);
                    }
                });
            }
        });
    }

    @Action(path = "/phim123/getComboGalaxy")
    public void getcombCGalaxy(HttpRequestContext context, final Handler<JsonObject> callback) {

        MultiMap params = context.getRequest().params();

        String code = params.get("code") == null ? "" : params.get("code").trim();
        String phone = params.get("phone") == null ? "" : params.get("phone").trim();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/phim123/getComboGalaxy");

        if (!"".equalsIgnoreCase(code)) {

            if ("".equalsIgnoreCase(code) || code.length() != 9) {
                callback.handle(new JsonObject()
                        .putString("error", "-1")
                        .putString("desc", "Combo không hợp lệ."));
                return;
            }
            boolean valid = glxUtil.isCodeValid(code, "MOMO");

            if (!valid) {
                callback.handle(new JsonObject()
                        .putString("error", "-1")
                        .putString("desc", "Combo không hợp lệ."));
                return;
            }
        }

        if ("".equals(code) && "".equals(phone)) {
            callback.handle(new JsonObject()
                    .putString("error", "-1")
                    .putString("desc", "Combo không hợp lệ."));
            return;
        }

        try {
            Long.parseLong(phone);
        } catch (Exception e) {
            callback.handle(new JsonObject()
                    .putString("error", "-1")
                    .putString("desc", "Số điện thoại không hợp lệ."));
            return;
        }

        log.add("code", code);
        log.add("phone", phone);

        promo123PhimGlxDb.get(code, phone, new Handler<Promo123PhimGlxDb.Obj>() {
            @Override
            public void handle(final Promo123PhimGlxDb.Obj obj) {
                JsonObject reObj = null;
                if (obj == null) {
                    reObj = new JsonObject();
                    reObj.putString("error", "-2");
                    reObj.putString("desc", "Không tìm thấy Combo.");
                } else {
                    reObj = obj.toJson();
                    reObj.putString("amount", Misc.formatAmount(obj.AMOUNT).replace(",", "."));
                    reObj.putString("update_time", Misc.dateVNFormatWithTime(obj.UPDATE_TIME));
                }
                log.add("desc", obj == null ? "Not Exists" : obj.DESC);
                log.writeLog();
                callback.handle(reObj);
            }
        });
    }

    // Tool kiểm tra trả thưởng cho Galaxy

    //Tool xóa số điện thoại
    @Action(path = "/tool/deletePhoneNumber")
    public void deletePhoneNumber(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        final HttpServerRequest request = context.getRequest();
        final JsonObject jo = new JsonObject();
        MultiMap params = request.params();
        String number = params.get("number");
        log.add("func", "deletePhoneNumber");
        try {

            final int finalNumber = DataUtil.strToInt(number);
            if (finalNumber == 0) {
                log.writeLog();
                jo.putString("desc", "Số điện thoại không hợp lệ!");
                response(request, jo);
                return;
            }
            phonesDb.removePhoneObj(finalNumber, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    log.add("deleted result", aBoolean);
                    jo.putString("desc", "Xóa không thành công.");
                    if (aBoolean == true) {
                        jo.putString("desc", "Số điện thoại đã bị xóa.");
                    }
                    response(request, jo);

                }
            });

        } catch (NumberFormatException e) {
            callback.handle(new JsonObject()
                    .putString("error", "-1")
                    .putString("desc", "Số điện thoại không hợp lệ."));
            return;
        }
        log.writeLog();
    }

    @Action(path = "/vc/deleteQueuedGift")
    public void deleteQueuedGift(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "deleteQueuedGift");
        MultiMap params = request.params();
        String strNumber = "";
        final JsonObject jo = new JsonObject();
        strNumber = params.get("number");

        if ("".equalsIgnoreCase(strNumber)) {
            return;
        }

        log.add("function", "delete QueuedGift");
        log.add("uereGift", strNumber);
        queuedGiftDb.deleteQueuedGift(strNumber, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                log.add("deleted result", aBoolean);

                jo.putString("desc", "xóa thành công.");
                if (aBoolean == true) {
                    jo.putString("desc", "xóa không thành công.");
                }
                log.writeLog();
                response(request, jo);
            }
        });

    }

    //Tool Trả voucher cho Galaxy

    @Action(path = "/tool/oldConnection")
    public void findByTime(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "Copy and create New collection");
        MultiMap params = request.params();
        String strTime = params.get("FromTime");
        String oldCollection = params.get("oldConnection");
        final String newCollection = params.get("newConnection");

        final JsonObject jo = new JsonObject();
        long Time = DataUtil.strToLong(strTime);

        cdhh.findByFromTime(Time, oldCollection, new Handler<ArrayList<CDHH.Obj>>() {
            @Override
            public void handle(ArrayList<CDHH.Obj> objs) {
                int rows = 0;
                if (objs != null && objs.size() > 0) {
                    rows = objs.size();
                    for (CDHH.Obj o : objs) {
                        cdhh.saveToNewCollection(o, newCollection, new Handler<Integer>() {
                            @Override
                            public void handle(Integer count) {
                            }
                        });
                    }
                }
                jo.putString("desc", "Số dòng được copy " + rows);
                response(request, jo);
            }
        });
    }

    @Action(path = "/vc/getOwner")
    public void vcGetOwner(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "vcGetOwner");
        MultiMap params = request.params();
        String strNumber = "";
        final JsonObject jo = new JsonObject();
        strNumber = params.get("number");
        strNumber = "0" + DataUtil.strToInt(strNumber);

        giftDb.findBy(strNumber, new Handler<List<Gift>>() {
            @Override
            public void handle(List<Gift> gifts) {
                JsonArray array = new JsonArray();
                if (gifts != null && gifts.size() > 0) {
                    for (Gift gift : gifts) {

                        JsonObject jo = gift.toJsonObject();

                        jo.putString("endDate", Misc.dateVNFormatWithTime(jo.getLong("endDate")));
                        jo.putString("startDate", Misc.dateVNFormatWithTime(jo.getLong("startDate")));
                        jo.putString("modifyDate", Misc.dateVNFormatWithTime(jo.getLong("modifyDate")));
                        jo.putString("status", Gift.getStatusText(jo.getInteger("status")));
                        jo.putString("amount", Misc.formatAmount(jo.getLong("amount")));
                        jo.putString("_id", jo.getString("_id"));

                        array.add(jo);
                    }
                }
                response(request, array);
                return;
            }
        });
    }

    @Action(path = "/mLogger")
    public void vcGetUsed(HttpRequestContext context
            , final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("func", "vcGetUsed");
        MultiMap params = request.params();
        String strNumber = "";
        final JsonObject jo = new JsonObject();
        strNumber = params.get("number");
        strNumber = "0" + DataUtil.strToInt(strNumber);

        queuedGiftDb.findBy(strNumber, new Handler<List<QueuedGift>>() {
            @Override
            public void handle(List<QueuedGift> gifts) {
                JsonArray array = new JsonArray();
                if (gifts != null && gifts.size() > 0) {
                    for (QueuedGift gift : gifts) {

                        JsonObject jo = gift.toJsonObject();
                        array.add(jo);
                    }
                }
                response(request, array);
                return;
            }
        });
    }

//    @Action(path = "/webadmin/html/cardtypepage.html")
//    public void getCardType(HttpRequestContext context
//            , final Handler<JsonObject> callback) {
//
//        final HttpServerRequest request = context.getRequest();
//        final Common.BuildLog log = new Common.BuildLog(logger);
//        log.add("func", "getCardType");
//        MultiMap params = request.params();
//        String strNumber ="";
//        final JsonObject jo = new JsonObject();
//        strNumber = params.get("number");
//        strNumber = "0" + DataUtil.strToInt(strNumber);
//
//        giftDb.findBy(strNumber, new Handler<List<Gift>>() {
//            @Override
//            public void handle(List<Gift> gifts) {
//                JsonArray array = new JsonArray();
//                if (gifts != null && gifts.size() > 0) {
//                    for (Gift gift : gifts) {
//
//                        JsonObject jo = gift.toJsonObject();
//
//                        jo.putString("endDate", Misc.dateVNFormatWithTime(jo.getLong("endDate")));
//                        jo.putString("startDate", Misc.dateVNFormatWithTime(jo.getLong("startDate")));
//                        jo.putString("modifyDate", Misc.dateVNFormatWithTime(jo.getLong("modifyDate")));
//                        jo.putString("status", Gift.getStatusText(jo.getInteger("status")));
//                        jo.putString("amount", Misc.formatAmount(jo.getLong("amount")));
//                        jo.putString("_id", jo.getString("_id"));
//
//                        array.add(jo);
//                    }
//                }
//                response(request, array);
//                return;
//            }
//        });
//    }

    @Action(path = "/tool/traThuongGalaxy")
    public void traThuongGalaxy(HttpRequestContext context, final Handler<JsonObject> handler) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("func", "traThuongGalaxy");
        MultiMap params = request.params();
        final JsonObject jo = new JsonObject();
        String strNumber = params.get("phoneNumber");
        int finalNumber = DataUtil.strToInt(strNumber);
        if (finalNumber == 0) {
            jo.putString("desc", "Số điện thoại không hợp lệ!");
            response(request, jo);
            return;

        }
        phim123Glx.findOne(finalNumber, new Handler<Promo123PhimGlxDb.Obj>() {
            @Override
            public void handle(final Promo123PhimGlxDb.Obj glxObj) {

                //chua tra thuong lan nao
                if (glxObj == null || glxObj.PROMO_COUNT == 0) {
                    jo.putString("desc", "Chưa trả voucher cho khách hàng.");
                    response(request, jo);
                    log.writeLog();
                    return;
                }

                //Đã trả voucher 30000
                if (glxObj.PROMO_COUNT == 1) {
                    jo.putString("desc", "Đã trả voucher 30000.");
                    response(request, jo);
                    return;
                }

                //Đã trả Cobo bắp nước
                if (glxObj.PROMO_COUNT == 2) {
                    jo.putString("desc", "Đã trả COMBO bắp nước.");
                    response(request, jo);
                    return;
                }
            }
        });
    }

    // update field  amount trong bang gift
    @Action(path = "/vc/updateOwner")
    public void updateOwner(HttpRequestContext context, final Handler<Object> callback) {


        MultiMap params = context.getRequest().params();
        JsonObject jsonObject = new JsonObject();
        final HttpServerRequest request = context.getRequest();
        final JsonObject result = new JsonObject();
        String amountOwner = params.get("amount");
        String giftId = params.get("_id");

        final long amount = DataUtil.strToLong(amountOwner);
        if (amount == 0) {
            result.putString("desc", "Số tiền không hợp lệ!");
            response(request, result);
            return;

        }
        jsonObject.putNumber("amount", amount);
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "update amount");
        log.add("_id", giftId);

        giftDb.updateOneAmount(giftId, jsonObject, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "update failed");

                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }
                log.writeLog();
                update(Common.ServiceReq.COMMAND.UPDATE_FEE);
                callback.handle(result);
                return;
            }
        });

    }

    @Action(path = "/tool/giveGiftGLx")
    public void giveGiftGLx(HttpRequestContext context, final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        final HttpServerRequest request = context.getRequest();
        final JsonObject jo = new JsonObject();
        MultiMap params = request.params();
        String number = params.get("number");
        String amount = params.get("amount");
        log.add("func", "giveGiftGLx");

        final int finalNumber = DataUtil.strToInt(number);
        final int fAmount = DataUtil.strToInt(amount);
        if (finalNumber == 0) {
            jo.putString("desc", "Số điện thoại không hợp lệ!");
            response(request, jo);
            log.writeLog();
            return;
        }

        if (fAmount == 0) {
            jo.putString("desc", "Số tiền không hợp lệ!");
            response(request, jo);
            log.writeLog();
            return;
        }

        FilmInfo filmInfo = new FilmInfo();
        filmInfo.amount = fAmount;

        //tra voucher cho Galaxy
        doPromoGalaxy(finalNumber, context, filmInfo);
    }

    private void doPromoGalaxy(final int finalNumber
            , final HttpRequestContext context
            , final FilmInfo filmInfo) {
        final HttpServerRequest request = context.getRequest();
        final JsonObject jo = new JsonObject();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + finalNumber);
        log.add("func", "doPromoGalaxy");

        final Promo.PromoReqObj reqGlxRec = new Promo.PromoReqObj();
        reqGlxRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
        reqGlxRec.PROMO_NAME = "galaxy";

        Misc.requestPromoRecord(vertx, reqGlxRec, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                final PromotionDb.Obj glxRec = new PromotionDb.Obj(jsonObject);

                //khong cho chuog trinh
                if (glxRec == null || glxRec.DATE_TO == 0 || glxRec.DATE_FROM == 0) {
                    log.add("galaxydesc", "Khong co chuong trinh khuyen mai nao");
                    jo.putString("desc", "Khong co chuong trinh khuyen mai nao");
                    response(request, jo);
                    log.writeLog();
                    return;
                }

                long curTime = System.currentTimeMillis();

                //het thoi gian khuyen mai
                if (curTime < glxRec.DATE_FROM || glxRec.DATE_TO < curTime) {
                    jo.putString("desc", "Hết thời gian khuyến mãi");
                    response(request, jo);
                    log.writeLog();
                    return;
                }

                phim123Glx.findOne(finalNumber, new Handler<Promo123PhimGlxDb.Obj>() {
                    @Override
                    public void handle(final Promo123PhimGlxDb.Obj glxObj) {

                        //het so lan khuyen mai
                        if (glxObj.PROMO_COUNT >= glxRec.MAX_TIMES) {
                            jo.putString("desc", "Số lần đã trả voucher " + glxObj.PROMO_COUNT);
                            response(request, jo);
                            log.writeLog();
                            return;
                        }
                        //chua tra thuong lan nao
                        if (glxObj.PROMO_COUNT == 0) {

                            if (filmInfo.amount < glxRec.TRAN_MIN_VALUE) {
                                log.add("galaxydesc", "Khong tra thuong vi : " + filmInfo.amount + " < " + glxRec.PER_TRAN_VALUE);
                                jo.putString("desc", "Số tiền không đủ để khuyến mãi " + filmInfo.amount + " < " + glxRec.PER_TRAN_VALUE);
                                response(request, jo);
                                log.writeLog();
                                return;
                            }

                            log.add("galaxydesc", "tra voucher 30000d");
                            //tang vc
                            ArrayList<Misc.KeyValue> listKeyValue = new ArrayList<>();
                            listKeyValue.add(new Misc.KeyValue("promo", "galaxy"));
                            giftManager.adjustGiftValue(glxRec.ADJUST_ACCOUNT
                                    , "0" + finalNumber
                                    , glxRec.PER_TRAN_VALUE
                                    , listKeyValue
                                    , new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            int error = jsonObject.getInteger("error", -1);
                                            long tranId = jsonObject.getLong("tranId", -1);
                                            log.add("error", error);
                                            log.add("galaxydesc", SoapError.getDesc(error));

                                            if (error == 0) {

                                                final String giftTypeId = "galaxy";
                                                GiftType giftType = new GiftType();
                                                giftType.setModelId(giftTypeId);

                                                ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                                Misc.KeyValue kv = new Misc.KeyValue();
                                                kv.Key = "cinema";
                                                kv.Value = "glx";
                                                keyValues.add(kv);

                                                giftManager.createLocalGift("0" + finalNumber
                                                        , glxRec.PER_TRAN_VALUE
                                                        , giftType
                                                        , tranId
                                                        , glxRec.ADJUST_ACCOUNT
                                                        , glxRec.DURATION
                                                        , keyValues, new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject jsonObject) {
                                                                int err = jsonObject.getInteger("error", -1);
                                                                long tranId = jsonObject.getInteger("tranId", -1);

                                                                log.add("galaxydesc", "tra thuong galaxy bang gift");
                                                                log.add("err", err);

                                                                String localResult = "";
                                                                if (err == 0) {

                                                                    localResult = "Trả voucher " + glxRec.PER_TRAN_VALUE + " thành công";
                                                                    Gift gift = new Gift(jsonObject.getObject("gift"));
                                                                    String tranComment = "Vui xem phim Galaxy cùng Ví MoMo.";
                                                                    String notiCaption = "Nhận thưởng thẻ quà tặng!";
                                                                    //String notiBody="Chúc mừng Bạn vừa nhận Thẻ quà tặng từ chương trình “Có MoMo không lo hết vé”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi” và kích hoạt thẻ quà tặng Bạn vừa nhận! Thẻ quà tặng có hiệu lực sau 2 tiếng kể từ khi nhận được. LH: (08) 399 171 99.";
                                                                    String notiBody = "Chúc mừng Bạn vừa nhận Thẻ quà tặng 30.000đ từ chương trình “Vui xem phim Galaxy cùng Ví MoMo”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi” và kích hoạt thẻ quà tặng Bạn vừa nhận! Thẻ quà tặng có hiệu lực sau 2 tiếng kể từ khi nhận được. LH: (08) 399 171 99.";

                                                                    String giftMessage = tranComment;//"Có MoMo không lo hết vé và hơn thế nữa";
                                                                    Misc.sendTranHisAndNotiGift(vertx
                                                                            , finalNumber
                                                                            , tranComment
                                                                            , tranId
                                                                            , glxRec.PER_TRAN_VALUE
                                                                            , gift
                                                                            , notiCaption
                                                                            , notiBody
                                                                            , giftMessage
                                                                            , transDb);

                                                                    //cap nhat so lan khuyen mai len 1
                                                                    Promo123PhimGlxDb.Obj saveObj = new Promo123PhimGlxDb.Obj();
                                                                    saveObj.ID = finalNumber + "";
                                                                    saveObj.STATUS = Promo123PhimGlxDb.STATUS_NEW;
                                                                    saveObj.DESC = "Trả thưởng bằng voucher";
                                                                    saveObj.PROMO_COUNT = 1;
                                                                    saveObj.PROMO_TIMEVN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
                                                                    saveObj.TIME = System.currentTimeMillis();
                                                                    saveObj.TIMEVN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
                                                                    saveObj.AMOUNT = filmInfo.amount;

                                                                    phim123Glx.save(saveObj, new Handler<Boolean>() {
                                                                        @Override
                                                                        public void handle(Boolean aBoolean) {

                                                                        }
                                                                    });
                                                                    log.writeLog();
                                                                } else {
                                                                    localResult = "Trả voucher " + glxRec.PER_TRAN_VALUE + " không thành công trên backend";
                                                                    log.writeLog();
                                                                }

                                                                jo.putString("desc", localResult);
                                                                response(request, jo);
                                                            }
                                                        });
                                            } else {
                                                jo.putString("desc", "Trả voucher gặp lỗi " + SoapError.getDesc(error));
                                                response(request, jo);
                                                log.add("galaxydesc", "Tra qua galaxy loi");
                                                log.writeLog();
                                            }
                                        }
                                    });
                            return;
                        }

                        //tang combo bap nuoc
                        if (glxObj.PROMO_COUNT == 1) {

                            //gia tri khong du tien de nhan qua
                            if (filmInfo.amount < glxRec.TRAN_MIN_VALUE) {
                                jo.putString("desc", "Giá trị giao dịch không đủ để nhận COMBO");
                                response(request, jo);
                                return;
                            }

                            //tang code
                            final String code = "MOMO" + codeUtilGLX.getNextCode();
                            JsonObject joUp = new JsonObject();
                            joUp.putString(colName.Phim123PromoGlxCols.PROMO_CODE, code);
                            joUp.putNumber(colName.Phim123PromoGlxCols.AMOUNT, filmInfo.amount);
                            joUp.putString(colName.Phim123PromoGlxCols.STATUS, Promo123PhimGlxDb.STATUS_NEW);
                            joUp.putString(colName.Phim123PromoGlxCols.DESC, Promo123PhimGlxDb.DESC_NEW);
                            joUp.putNumber(colName.Phim123PromoGlxCols.PROMO_COUNT, 2);

                            //cap nhat so lan khuyen mai len 1
                            phim123Glx.update(finalNumber, joUp, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {

                                    String bodyTmplate = "Mã nhận COMBO: %s, rạp Galaxy. Vui lòng xuất trình mã nhận Combo và vé xem phim theo quy định để nhận quà. LH: (08) 399 171 99.";
                                    String notiBody = String.format(bodyTmplate
                                            , code);

                                    //send noti
                                    String notiCap = "Nhận thưởng phiếu COMBO khuyến mãi!";
                                    Notification noti = new Notification();
                                    noti.receiverNumber = finalNumber;
                                    noti.sms = "";
                                    noti.tranId = System.currentTimeMillis();
                                    noti.priority = 2;
                                    noti.time = System.currentTimeMillis();
                                    noti.category = 0;
                                    noti.caption = notiCap;
                                    noti.body = notiBody;
                                    noti.status = Notification.STATUS_DETAIL;
                                    noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                    Misc.sendNoti(vertx, noti);
                                    jo.putString("desc", "Trả COMBO bắp nước thành công");
                                    response(request, jo);
                                }
                            });
                        }

                    }
                });
            }
        });
    }

    @Action(path = "/uploadFile")
    public void replyVtbUpload(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/cardtype/getAllCardTypes")
    public void getCardType(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "getCardType");
        MultiMap params = request.params();

        String parnerCode = params.get("partnerCode") == null ? "" : params.get("partnerCode");
        String cardType = params.get("cardType") == null ? "" : params.get("cardType");
        Boolean enable = (params.get("enable") == null || params.get("enable") == "") ? null : "true".equalsIgnoreCase(params.get("enable")) ? true : false;
        cardTypeDb.find(parnerCode, cardType, enable, 0, new Handler<ArrayList<CardTypeDb.Obj>>() {
            @Override
            public void handle(ArrayList<CardTypeDb.Obj> objs) {
                log.add("size of card type", objs.size());
                JsonArray array = new JsonArray();
                if (objs != null && objs.size() > 0) {
                    for (CardTypeDb.Obj o : objs) {
                        JsonObject jo = o.toJson();
                        jo.putString(colName.CardType.lastTime, Misc.dateVNFormatWithTime(o.lastTime));
                        array.add(jo);
                    }
                }
                response(request, array);
            }
        });
    }

    @Action(path = "/cardtype/upsert")
    public void upsertCardType(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "getCardType");
        MultiMap params = request.params();

        String id = params.get("id") == null ? "" : params.get("id");
        String parnerCode = params.get("partnerCode") == null ? "" : params.get("partnerCode");
        String cardType = params.get("cardType") == null ? "" : params.get("cardType");
        Boolean enable = params.get("enable") == null ? null : "true".equalsIgnoreCase(params.get("enable")) ? true : false;
        String icon = params.get("icon") == null ? "" : params.get("icon");
        String desc = params.get("desc") == null ? "" : params.get("desc");
        final CardTypeDb.Obj o = new CardTypeDb.Obj();
        o.id = id;
        o.enable = enable;
        o.iconUrl = icon;
        o.lastTime = System.currentTimeMillis();
        o.partnerCode = parnerCode;
        o.cardType = cardType;
        o.desc = desc;

        cardTypeDb.upsert(o, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                JsonObject jo = new JsonObject();
                if (aBoolean) {
                    jo.putNumber("error", 0);
                } else {
                    jo.putNumber("error", 1);
                    jo.putString("desc", "Khong cap nhat duoc du lieu");
                }
                response(request, jo);
            }
        });
    }

    private void response(HttpServerRequest request, JsonObject jo) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(jo.toString());
        request.response().end();
    }

    private void response(HttpServerRequest request, JsonArray joArr) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(joArr.toString());
        request.response().end();
    }

    private void response(HttpServerRequest request, String strResult) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "text/plain ; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(strResult);
        request.response().end();
    }

    //BEGIN 5 nhom tra thuong
    @Action(path = "/readCustomerCareFile")
    public void readerCustomerCareFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<CustomerCareRow> customerCareRowsQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            customerCareRowsQueue.add(new CustomerCareRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (customerCareRowsQueue.size() > 1) {
            CustomerCareRow customerCareRow = customerCareRowsQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        doCustomerCarePromotion(customerCareRowsQueue, arrayResult, callback);
        log.writeLog();
    }

    @Action(path = "/uploadCustomerCareFile")
    public void replyCustomerServiceUpload(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    //BEGIN 5 nhom tra thuong
    @Action(path = "/readGiftReturnFile")
    public void readGiftReturnFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<ReturnGiftRow> returnGiftRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            returnGiftRowQueue.add(new ReturnGiftRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (returnGiftRowQueue.size() > 1) {
            ReturnGiftRow returnGiftRow = returnGiftRowQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        doReturnGift(returnGiftRowQueue, arrayResult, callback);
        log.writeLog();
    }

    @Action(path = "/uploadGiftReturnCustomerFile")
    public void replyGiftReturnUpload(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    private void doReturnGift(final Queue<ReturnGiftRow> returnGiftRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {


        final Common.BuildLog log = new Common.BuildLog(logger);

        if (returnGiftRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final ReturnGiftRow rec = returnGiftRowQueue.poll();

        if (rec == null) {
            doReturnGift(returnGiftRowQueue, arrayResult, callback);
        } else {

            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.number) + "");

            if (number > 0) {
                // Trả khuyến mãi
                final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                keyValues.add(new Misc.KeyValue("program", rec.program));
                keyValues.add(new Misc.KeyValue("group", rec.group));
                ControlOnClickActivityDb.Obj controlObj = new ControlOnClickActivityDb.Obj();
                controlObj.key = number + "_" + rec.program.trim();
                controlObj.number = number + "";
                controlObj.program = rec.program;
                controlObj.service = rec.group;
                controlOnClickActivityDb.insert(controlObj, new Handler<Integer>() {
                    @Override
                    public void handle(Integer result) {
                        if (result != 0) {
                            log.add("desc", "tra duplicate cho so " + number);
                            doReturnGift(returnGiftRowQueue, arrayResult, callback);
                            return;
                        }
                        //Tra thuong trong core
                        giftManager.adjustGiftValue(rec.agent
                                , rec.number
                                , DataUtil.strToLong(rec.giftValue)
                                , keyValues, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {

                                        final int error = jsonObject.getInteger("error", -1);
                                        final long promotedTranId = jsonObject.getLong("tranId", -1);
                                        log.add("error", error);
                                        log.add("desc", SoapError.getDesc(error));

                                        //tra thuong trong core thanh cong
                                        if (error == 0) {
                                            final GiftType giftType = new GiftType();
                                            giftType.setModelId(rec.giftTypeId);
                                            giftManager.createLocalGift(rec.number
                                                    , DataUtil.strToLong(rec.giftValue)
                                                    , giftType
                                                    , promotedTranId
                                                    , rec.agent
                                                    , DataUtil.strToInt(rec.duration)
                                                    , keyValues, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject jsonObject) {
                                                            int err = jsonObject.getInteger("error", -1);
                                                            rec.error = err + "";
                                                            rec.errorDesc = "Good";
                                                            rec.time = "" + System.currentTimeMillis();
                                                            arrayResult.add(rec.toJson());
                                                            doReturnGift(returnGiftRowQueue, arrayResult, callback);
                                                            final Gift gift = new Gift(jsonObject.getObject("gift"));
                                                            final String giftId = gift.getModelId().trim();
                                                            if (err == 0 && !"deactive".equalsIgnoreCase(rec.group.trim().toLowerCase())) {
                                                                giftManager.useGift(rec.number, giftId, new Handler<JsonObject>() {
                                                                    @Override
                                                                    public void handle(JsonObject jsonObject) {

                                                                    }
                                                                });
                                                            }

                                                            log.add("desc", "tra thuong chuong trinh ironman bang gift");
                                                            log.add("err", err);

                                                        }
                                                    });
                                        } else {
                                            //tra thuong trong core khong thanh cong
                                            log.add("desc", "Lỗi " + SoapError.getDesc(error));
                                            log.add("Exception", "Exception " + SoapError.getDesc(error));
                                            log.writeLog();
                                            rec.error = error + "";
                                            rec.errorDesc = SoapError.getDesc(error);
                                            Date date = new Date();
                                            rec.time = DataUtil.timeDate(date);
                                            arrayResult.add(rec.toJson());
                                            doReturnGift(returnGiftRowQueue, arrayResult, callback);
                                        }
                                    }
                                });
                    }
                });
            } else {
                log.add("desc", "number equal " + number);
                doReturnGift(returnGiftRowQueue, arrayResult, callback);
                return;
            }
        }
    }

    private void doCustomerCarePromotion(final Queue<CustomerCareRow> customerCareRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {


        final Common.BuildLog log = new Common.BuildLog(logger);

        if (customerCareRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final CustomerCareRow rec = customerCareRowQueue.poll();

        if (rec == null) {
            doCustomerCarePromotion(customerCareRowQueue, arrayResult, callback);
        } else {

            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.number) + "");

            if (number > 0) {
                boolean enable = false;
                if (rec.enable.equalsIgnoreCase("true")) {
                    enable = true;
                }

                final CustomCareDb.Obj o = new CustomCareDb.Obj();
                o.number = rec.number;
                o.nameCustomer = rec.nameCustomer;
                o.enable = enable;
                o.group = rec.group;
                o.groupDesc = rec.groupDesc;
                o.orgDateFrom = rec.orgDateFrom;
                o.dateFrom = Misc.getDateAsLong(rec.orgDateFrom, "yyyy-MM-dd hh:mm:ss", logger, "");
                o.orgDateTo = rec.orgDateTo;
                o.dateTo = Misc.getDateAsLong(rec.orgDateTo, "yyyy-MM-dd hh:mm:ss", logger, "");
                o.promoCount = 0;
                o.duration = DataUtil.strToInt(rec.duration);
                o.momoAgent = rec.momoAgent;
                o.promoValue = DataUtil.strToInt(rec.promoValue);
                o.giftTypeId = rec.giftTypeId;
                customerCareDb.insert(o, new Handler<Integer>() {
                    @Override
                    public void handle(Integer error) {

                        rec.error = error + "";
                        if (error != 0 && !"6".equalsIgnoreCase(o.group)) {
                            rec.errorDesc = "Duplicate key";
                            arrayResult.add(rec.toJson());
                            doCustomerCarePromotion(customerCareRowQueue, arrayResult, callback);
                            log.writeLog();
                        } else if (error != 0 && "6".equalsIgnoreCase(o.group)) {
                            CustomCareObj.requestCustomCarePromo(vertx, o.number, 0, o.tranId, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jsonObject) {
                                    if (jsonObject != null) {
                                        rec.errorDesc = jsonObject.getString("desc", "");
                                        rec.error = jsonObject.getInteger("error", 0) + "";
                                        arrayResult.add(rec.toJson());
                                        doCustomerCarePromotion(customerCareRowQueue, arrayResult, callback);
                                    }
                                }
                            });
                        } else {
                            rec.errorDesc = "success";
                            if ("3".equalsIgnoreCase(o.group)) {
                                CustomCareObj.requestCustomCarePromo(vertx, o.number, 0, o.tranId, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {
                                        rec.errorDesc = jsonObject.getString("desc", "");
                                        rec.error = jsonObject.getInteger("error", 0) + "";
                                        arrayResult.add(rec.toJson());
                                        doCustomerCarePromotion(customerCareRowQueue, arrayResult, callback);
                                    }
                                });
                            } else {
                                arrayResult.add(rec.toJson());
                                doCustomerCarePromotion(customerCareRowQueue, arrayResult, callback);
                                log.writeLog();
                            }
                        }
                    }
                });
            }
        }
    }

    @Action(path = "/servicecustomer/searchallcustomercare")
    public void searchCustomerCareUpload(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "searchCustomerCareUpload");
        MultiMap params = request.params();

        String number = params.get("number") == null ? "" : params.get("number");
        String group = params.get("group") == null ? "" : params.get("group");


        JsonObject jsonSearch = new JsonObject();

        if (!"".equalsIgnoreCase(number)) {
            jsonSearch.putString(colName.CustomCarePromo.number, number);
        }
        if (!"".equalsIgnoreCase(group)) {
            jsonSearch.putString(colName.CustomCarePromo.group, group);
        }


        customerCareDb.searchWithFilter(jsonSearch, new Handler<ArrayList<CustomCareDb.Obj>>() {
            @Override
            public void handle(ArrayList<CustomCareDb.Obj> objs) {
                JsonArray array = new JsonArray();

                if (objs != null) {
                    for (CustomCareDb.Obj o : objs) {
                        array.add(o.toJson());
                    }
                    response(request, array);
                }
            }
        });
    }

    @Action(path = "/forceLoadAllCfg")
    public void forceUpdateAllConfig(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        //Truyen tham so vao ServiceReq de mang qua ServiceConfVerticle class.
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.FORCE_LOAD_ALL_CONFIG;

        //Truyen serviceReq qua ben ServiceConfVerticle de thuc hien update lai record
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());

        JsonObject jo = new JsonObject();
        jo.putNumber("error", 0);
        jo.putString("desc", "Success");
        callback.handle(jo);
    }

    //BEGIN 0000000004
    @Action(path = "/uploadBillPayProFile")
    public void replyBillPayUpload(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/readBillPayFile")
    public void readBillPayFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<BillPayPromoRow> billPayRowsQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            billPayRowsQueue.add(new BillPayPromoRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (billPayRowsQueue.size() > 1) {
            BillPayPromoRow billPayPromoRow = billPayRowsQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        doBillPayPromotion(billPayRowsQueue, arrayResult, callback);
        log.writeLog();
    }

    //END 0000000004

    private void doBillPayPromotion(final Queue<BillPayPromoRow> billPayRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {


        final Common.BuildLog log = new Common.BuildLog(logger);

        if (billPayRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final BillPayPromoRow rec = billPayRowQueue.poll();

        if (rec == null) {
            doBillPayPromotion(billPayRowQueue, arrayResult, callback);
        } else {

            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.number) + "");

            if (number > 0) {

                final BillPayPromosDb.Obj o = new BillPayPromosDb.Obj();
                BillPayPromoObj.requestBillPayPromo(vertx, rec.number, DataUtil.strToInt(rec.tranType),
                        DataUtil.strToLong(rec.tranId), rec.serviceId, rec.group, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject jsonObject) {
                                if (jsonObject != null) {
                                    int error = jsonObject.getInteger("error", 1000);
                                    String desc = jsonObject.getString("desc", "");
                                    rec.error = error + "";
                                    rec.errorDesc = desc;
                                    arrayResult.add(rec.toJson());
                                    doBillPayPromotion(billPayRowQueue, arrayResult, callback);
                                    log.writeLog();
                                }
                            }
                        }
                );
            }
        }
    }

    //Ham nay dung de xu ly ban thong tin khach hang tu Web tool
    @Action(path = "/banteotrymkhachhang")
    public void banPhaThongBaoHangVeChoKhach(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());
        final TransDb trans = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbConfig);
        final Gift gift = new Gift();
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(0, 934123456);
        integers.add(1, 973192101);
        integers.add(2, 934456789);
        integers.add(3, 909020290);
        integers.add(4, 946810122);
        integers.add(5, 936140582);
        integers.add(6, 944821459);
        integers.add(7, 936140582);
        integers.add(8, 1225523105);
        integers.add(9, 944819399);
        integers.add(10, 909621729);
        integers.add(11, 949510403);
        integers.add(12, 934777999);
//        phonesDb.getAllPhone(new Handler<ArrayList<Integer>>() {
//            @Override
//            public void handle(ArrayList<Integer> integers) {
        for (int number : integers) {
            //Ban pha khach hang
            shotNotiToAdvertBillPayPromo(number, trans, gift);
        }
        callback.handle(new JsonObject().putString("desc", "Ban phe qua"));
//            }
//        });
        log.writeLog();
    }
    //END 0000000021 Ma hoa soapin

    //Ham nay dung de ban thong tin quang cao promo cho user tu tool
    private void shotNotiToAdvertBillPayPromo(final int number, final TransDb tranDb, final Gift gift) {
        BillPayPromosDb billPayPromosDb = new BillPayPromosDb(vertx, logger);

        billPayPromosDb.findOne("0" + number, new Handler<BillPayPromosDb.Obj>() {
            @Override
            public void handle(BillPayPromosDb.Obj obj) {
                if (obj == null) {
                    String giftMessage = PromoContentNotification.BILL_PAY_PROMO_GIFT_MESSAGE_TIEP_CAN;
                    String notiCaption = PromoContentNotification.BILL_PAY_NOTI_CAPTION;
                    String tranComment = PromoContentNotification.BILL_PAY_PROMO_TRAN_COMMENT_TIEP_CAN;
                    String notiBody = PromoContentNotification.BILL_PAY_PROMO_DETAIL_NOTI_TIEP_CAN;

                    Misc.sendTranHisAndNotiGiftForBillPay(vertx
                            , DataUtil.strToInt("0" + number)
                            , tranComment
                            , 2015051505
                            , 200000
                            , gift
                            , notiCaption
                            , notiBody
                            , giftMessage
                            , tranDb);

                }
            }
        });
    }

    //END 0000000022 Tim tat ca gift het han

    ///searchallbillpayerror
    @Action(path = "/searchallbillpayerror")
    public void searchBillPayError(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "searchBillPayError");
        MultiMap params = request.params();

        String number = params.get("number") == null ? "" : params.get("number");
        String group = params.get("group") == null ? "" : params.get("group");


        JsonObject jsonSearch = new JsonObject();

        if (!"".equalsIgnoreCase(number)) {
            jsonSearch.putString(colName.BillPayPromoError.NUMBER, number);
        }
        if (!"".equalsIgnoreCase(group)) {
            jsonSearch.putString(colName.BillPayPromoError.GROUP, group);
        }


        billPayPromoErrorDb.searchWithFilter(jsonSearch, new Handler<ArrayList<BillPayPromoErrorDb.Obj>>() {
            @Override
            public void handle(ArrayList<BillPayPromoErrorDb.Obj> objs) {
                JsonArray array = new JsonArray();

                if (objs != null) {
                    for (BillPayPromoErrorDb.Obj o : objs) {
                        array.add(o.toJson());
                    }
                    response(request, array);
                }
            }
        });
    }

    ///searchusedvouchercustomer
    @Action(path = "/searchusedvouchercustomer")
    public void searchUsedVoucherCustomer(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "searchBillPayError");
        MultiMap params = request.params();

        String number = params.get("number") == null ? "" : params.get("number");
        String group = params.get("group") == null ? "" : params.get("group");


        JsonObject jsonSearch = new JsonObject();

        JsonObject notNull = new JsonObject();
        notNull.putObject(MongoKeyWords.NOT_EQUAL, null);

        JsonObject objNotNull = new JsonObject();
        objNotNull.putObject("giftInfoDetail", notNull);

        JsonObject notEqual = new JsonObject();
        notEqual.putString(MongoKeyWords.NOT_EQUAL, "");

        JsonObject objNotEqual = new JsonObject();
        objNotEqual.putObject("giftInfoDetail", notEqual);

        JsonArray arrayGiftInfo = new JsonArray();
        arrayGiftInfo.addObject(objNotNull);
        arrayGiftInfo.addObject(objNotEqual);

//        JsonObject and = new JsonObject();
//        and.putArray(MongoKeyWords.AND_$, arrayGiftInfo);

        jsonSearch.putArray(MongoKeyWords.AND_$, arrayGiftInfo);

        if (!"".equalsIgnoreCase(number)) {
            JsonObject json1 = new JsonObject();
            json1.putString("owner", number);
            JsonObject json2 = new JsonObject();
            json2.putString("oldOwner", number);
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(json1);
            jsonArray.add(json2);
            jsonSearch.putArray(MongoKeyWords.OR, jsonArray);
        }

        if (!"".equalsIgnoreCase(group)) {
            if (!"6".equalsIgnoreCase(group)) {
                JsonObject lte = new JsonObject();
                lte.putNumber(MongoKeyWords.LESS_OR_EQUAL, 4);
                jsonSearch.putObject("status", lte);
            } else {
                jsonSearch.putNumber("status", 6);
            }

        }

        giftDb.getGiftWithFilter(jsonSearch, new Handler<ArrayList<Gift>>() {
            @Override
            public void handle(ArrayList<Gift> gifts) {
                log.add("gifts", gifts);
                JsonArray array = new JsonArray();
                JsonObject jsonShow = null;
                int i = 0;
                if (gifts != null) {
                    for (Gift o : gifts) {
                        i = i + 1;
                        jsonShow = new JsonObject();
                        jsonShow.putNumber("id", i);
                        if (o.status == 6) {
                            jsonShow.putString("number", o.oldOwner);
                        } else {
                            jsonShow.putString("number", o.owner);
                        }
                        if (o.status == 6) {
                            jsonShow.putString("status", "Da su dung the");
                        } else {
                            jsonShow.putString("status", "Chua su dung the");
                        }
                        jsonShow.putString("gifttype", o.typeId);
                        array.add(jsonShow);
                    }
                    response(request, array);
                }
            }
        });
//        billPayPromoErrorDb.searchWithFilter(jsonSearch, new Handler<ArrayList<BillPayPromoErrorDb.Obj>>() {
//            @Override
//            public void handle(ArrayList<BillPayPromoErrorDb.Obj> objs) {
//                JsonArray array = new JsonArray();
//
//                if (objs != null) {
//                    for (BillPayPromoErrorDb.Obj o : objs) {
//                        array.add(o.toJson());
//                    }
//                    response(request, array);
//                }
//            }
//        });
    }

    //BEGIN 0000000021 Ma Hoa soapin
    @Action(path = "/encryptsoapin")
    public void encryptSoapin(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "encryptsoapin");
        MultiMap params = request.params();

        String username = params.get("username") == null ? "" : params.get("username");
        String password = params.get("password") == null ? "" : params.get("password");
        JsonObject jsonResult = new JsonObject();
        if (username.equalsIgnoreCase("") || password.equalsIgnoreCase("")) {
            jsonResult.putString("username", "0");
            jsonResult.putString("password", "0");
            callback.handle(jsonResult);
            return;
        }
        SoapInProcess soapInProcess = new SoapInProcess(logger, glbConfig);
        jsonResult = soapInProcess.returnInfoEncrypt(username, password);
        jsonResult.putNumber("error", 0);
        callback.handle(jsonResult);
    }
    //BEGIN 0000000023 Thu hoi gift het han


    //BEGIN 0000000024 Kiem tra thong tin store
    //checkstoreinfo

    @Action(path = "/decryptsoapin")
    public void decryptSoapin(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "decryptSoapin");
        MultiMap params = request.params();

        String username = params.get("username") == null ? "" : params.get("username");
        String password = params.get("password") == null ? "" : params.get("password");
        JsonObject jsonResult = new JsonObject();
        if (username.equalsIgnoreCase("") || password.equalsIgnoreCase("")) {
            jsonResult.putString("username", "0");
            jsonResult.putString("password", "0");
            callback.handle(jsonResult);
            return;
        }
        SoapInProcess soapInProcess = new SoapInProcess(logger, glbConfig);
        jsonResult = soapInProcess.returnInfoDecrypt(username, password);
        jsonResult.putNumber("error", 0);
        callback.handle(jsonResult);
    }

    //BEGIN 0000000022 Tim tat ca gift het han
    ///searchexpiredgift
    @Action(path = "/searchexpiredgift")
    public void searchExpiredGift(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "searchexpiredgift");
        MultiMap params = request.params();
        final String number = params.get(StringConstUtil.NUMBER) == null ? "" : params.get(StringConstUtil.NUMBER);
        long currentTime = System.currentTimeMillis();

        giftDb.findForExpiredNumber(currentTime, DataUtil.strToInt(number), new Handler<List<Gift>>() {
            @Override
            public void handle(List<Gift> gifts) {
                JsonArray array = new JsonArray();
                JsonObject jsonObject = new JsonObject();
                int i = 0;
                for (Gift gift : gifts) {
                    i = i + 1;
                    jsonObject = gift.toJsonObject();
                    jsonObject.putNumber("stt", i);
                    jsonObject.putString("start", DataUtil.timeDate(gift.startDate));
                    jsonObject.putString("end", DataUtil.timeDate(gift.endDate));
                    array.add(jsonObject);
                }
                response(request, array);
            }
        });
    }

    //BEGIN 0000000023 Thu hoi gift het han
    ///searchexpiredgift
    @Action(path = "/getexpiredgift")
    public void getExpiredGift(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "getExpiredGift");
        MultiMap params = request.params();

        final String number = params.get(StringConstUtil.NUMBER) == null ? "0" : params.get(StringConstUtil.NUMBER);

        long currentTime = System.currentTimeMillis();
        giftDb.findForExpiredNumber(currentTime, DataUtil.strToInt(number), new Handler<List<Gift>>() {
            @Override
            public void handle(final List<Gift> gifts) {
                if (gifts == null || gifts.size() == 0) {
//                    glog.add("waiting gifts will be expired size", 0);
//                    glog.writeLog();
                    JsonArray array = new JsonArray();
                    response(request, array);
                    return;
                }

//                glog.add("waiting gifts will be expired size", gifts.size());
//                glog.writeLog();

                //build queue prepare run expired
                final Queue<Gift> queueGifts = new ArrayDeque<Gift>();
                for (Gift gift : gifts) {
                    queueGifts.add(gift);
                }

                //begin process expired gifts here
                if (queueGifts.size() > 0) {
                    isRunning = true;
                    runExpiredGift(queueGifts, request);
                }
            }
        });
    }

    private void runExpiredGift(final Queue<Gift> queueGifts, final HttpServerRequest request) {
        JsonObject giftConfig = glbConfig.getObject("gift", new JsonObject());

        final String EXPIRE_GIFT_MOMO = giftConfig.getString("expireGiftMomo", null);
        if (queueGifts == null || queueGifts.size() == 0) {
            JsonArray array = new JsonArray();
            JsonObject jsonObject = new JsonObject();
            int i = 0;
            for (Gift gift : queueGifts) {
                i = i + 1;
                jsonObject = gift.toJsonObject();
                jsonObject.putNumber("stt", i);
                jsonObject.putString("start", DataUtil.timeDate(gift.startDate));
                jsonObject.putString("end", DataUtil.timeDate(gift.endDate));
                array.add(jsonObject);
            }
            response(request, array);
            isRunning = false;
            return;
        }

        //lay gift ra
        final Gift gift = queueGifts.poll();
        if (gift == null) {
            runExpiredGift(queueGifts, request);
        } else {

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(gift.owner);

            giftManager.transferGiftWhenExpired(gift, EXPIRE_GIFT_MOMO, null, log, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject result) {
                    log.add("transferGift result", result.encodePrettily());
                    int error = result.getInteger("error", -1000);
                    log.add("curDate", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                    log.add("startDate", Misc.dateVNFormatWithTime(gift.startDate.getTime()));
                    log.add("endDate", Misc.dateVNFormatWithTime(gift.endDate.getTime()));
                    log.add("amount", gift.amount);
                    log.add("tranId", gift.tranId);
                    log.add("typeId", gift.typeId);
                    log.add("statusdesc", Gift.getDesc(gift.status));
                    log.add("momo account", EXPIRE_GIFT_MOMO);
                    log.add("error", error);
                    log.add("errdesc", SoapError.getDesc(error));
                    log.writeLog();
                    runExpiredGift(queueGifts, request);
                }
            });
        }
    }

    @Action(path = "/checkstoreinfo")
    public void checkStoreInfo(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "checkStoreInfo");
        MultiMap params = request.params();

        final String number = params.get(StringConstUtil.NUMBER) == null ? "" : params.get(StringConstUtil.NUMBER);


        long currentTime = System.currentTimeMillis();
        final JsonObject jsonReply = new JsonObject();
        if (!number.equalsIgnoreCase("")) {
            agentsDb.getOneAgent(number, "checkStoreInfo", new Handler<AgentsDb.StoreInfo>() {
                @Override
                public void handle(final AgentsDb.StoreInfo storeInfo) {
                    if (storeInfo == null) {
                        jsonReply.putNumber("error", 1);
                        jsonReply.putString("desc", "This is not an agent");
                        callback.handle(jsonReply);
                        return;
                    }

                    phonesDb.getPhoneObjInfo(DataUtil.strToInt(number), new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(PhonesDb.Obj obj) {
                            if (obj == null) {
                                jsonReply.putNumber("error", 1);
                                jsonReply.putString("desc", "This is not an agent");
                                callback.handle(jsonReply);
                                return;
                            }
                            jsonReply.putNumber("error", 0);
                            jsonReply.putString(StringConstUtil.STORE_NAME, storeInfo.storeName);
                            jsonReply.putString(StringConstUtil.DELETE, convertBooleanToString(storeInfo.deleted));
                            jsonReply.putString(StringConstUtil.WAITING_REG, convertBooleanToString(obj.waitingReg));
                            jsonReply.putString(StringConstUtil.IS_SETUP, convertBooleanToString(obj.isSetup));
                            jsonReply.putString(StringConstUtil.IS_AGENT, convertBooleanToString(obj.isAgent));
                            jsonReply.putString(StringConstUtil.STATUS, convertStatus(storeInfo.status));
                            callback.handle(jsonReply);
                        }
                    });
                }
            });

        }


    }
    //END 0000000025    Cap nhat thong tin store

    private String convertBooleanToString(boolean delete) {
        String isDelete = "0";
        if (delete) {
            isDelete = "2";
        } else {
            isDelete = "1";
        }
        return isDelete;
    }

    private String convertStatus(int status) {
        String data = "";
        if (status == 0) {
            data = "0";
        } else if (status == 1) {
            data = "1";
        } else {
            data = "2";
        }
        return data;
    }

    //BEGIN 0000000025 Cap nhat thong tin store
    @Action(path = "/updatestoreinfo")
    public void updateStoreInfo(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "updateStoreInfo");
        MultiMap params = request.params();

        final String number = params.get(StringConstUtil.NUMBER) == null ? "" : params.get(StringConstUtil.NUMBER);
        final String delete = params.get(StringConstUtil.DELETE) == null ? "" : params.get(StringConstUtil.DELETE);
        final String waitingReg = params.get(StringConstUtil.WAITING_REG) == null ? "" : params.get(StringConstUtil.WAITING_REG);
        final String isSetup = params.get(StringConstUtil.IS_SETUP) == null ? "" : params.get(StringConstUtil.IS_SETUP);
        final String isAgent = params.get(StringConstUtil.IS_AGENT) == null ? "" : params.get(StringConstUtil.IS_AGENT);
        final String status = params.get(StringConstUtil.STATUS) == null ? "" : params.get(StringConstUtil.STATUS);

        long currentTime = System.currentTimeMillis();
        final JsonObject jsonReply = new JsonObject();
        if (!number.equalsIgnoreCase("")) {
            JsonObject jsonAgentUpdate = new JsonObject();

            jsonAgentUpdate.putBoolean(colName.AgentDBCols.DELETED, convertStringToBoolean(delete));
            jsonAgentUpdate.putNumber(colName.AgentDBCols.STATUS, DataUtil.strToInt(status));

            agentsDb.updatePartial(number, jsonAgentUpdate, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    if (aBoolean) {
                        JsonObject jsonPhoneUpdate = new JsonObject();
                        jsonPhoneUpdate.putBoolean(colName.PhoneDBCols.WAITING_REG, convertStringToBoolean(waitingReg));
                        jsonPhoneUpdate.putBoolean(colName.PhoneDBCols.IS_SETUP, convertStringToBoolean(isSetup));
                        jsonPhoneUpdate.putBoolean(colName.PhoneDBCols.IS_AGENT, convertStringToBoolean(isAgent));
                        phonesDb.update(DataUtil.strToInt(number), jsonPhoneUpdate, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                if (aBoolean) {
                                    jsonReply.putNumber("error", 0);
                                    callback.handle(jsonReply);
                                    return;
                                } else {
                                    jsonReply.putNumber("error", 1);
                                    callback.handle(jsonReply);
                                    return;
                                }
                            }
                        });
                    } else {
                        jsonReply.putNumber("error", 1);
                        callback.handle(jsonReply);
                        return;
                    }
                }
            });
        }


    }

    public boolean convertStringToBoolean(String data) {
        boolean isCheck = false;
        if (data.equalsIgnoreCase("2")) {
            isCheck = true;
        }
        return isCheck;
    }

    //checkvisabillpayinfo
    //Tool kiem tra thanh toan visa
    @Action(path = "/checkvisabillpayinfo")
    public void checkVisaBillpayInfo(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "checkVisaBillpayInfo");
        MultiMap params = request.params();

        String tranid = params.get("tranid") == null ? "" : params.get("tranid");
        String number = params.get("number") == null ? "" : params.get("number");
        final JsonObject jsonResult = new JsonObject();
        if (number.equalsIgnoreCase("") || tranid.equalsIgnoreCase("")) {
            jsonResult.putNumber("error", 1);
            jsonResult.putString("desc", "Chua co thong tin");
            callback.handle(jsonResult);
            return;
        }
        transDb.getTranById(DataUtil.strToInt(number), DataUtil.strToLong(tranid), new Handler<TranObj>() {
            @Override
            public void handle(TranObj tranObj) {
                if (tranObj != null) {
                    // Co tran id
                    JsonArray jsonArray = tranObj.share;
                    String cardType = "";
                    String cardCheckSum = "";
                    String tranType = "";
                    if (jsonArray != null && jsonArray.size() > 0) {
                        JsonObject jsonObject = jsonArray.get(0);
                        if (jsonObject.containsField(StringConstUtil.TRANDB_CARD_TYPE)) {
                            cardType = jsonObject.getString(StringConstUtil.TRANDB_CARD_TYPE, "");
                        }
                        if (jsonObject.containsField(StringConstUtil.TRANDB_CARD_NUMBER_VISA)) {
                            cardCheckSum = jsonObject.getString(StringConstUtil.TRANDB_CARD_NUMBER_VISA, "");
                        }
                        if (jsonObject.containsField(StringConstUtil.TRANDB_TRAN_TYPE)) {
                            tranType = jsonObject.getString(StringConstUtil.TRANDB_TRAN_TYPE, "");
                        }
                        jsonResult.putNumber("error", 0);
                        jsonResult.putString(StringConstUtil.TRANDB_CARD_TYPE, VMCardType.convertBankNameFromCardTypeId(cardType));
                        jsonResult.putString(StringConstUtil.TRANDB_CARD_NUMBER_VISA, cardCheckSum);
                        jsonResult.putString(StringConstUtil.TRANDB_TRAN_TYPE, tranType);
                        callback.handle(jsonResult);

                        return;
                    }
                    jsonResult.putNumber("error", 1);
                    jsonResult.putString("desc", "Khong co thong tin giao dich");
                    callback.handle(jsonResult);
                    return;
                }


                // Khong co transaction
                jsonResult.putNumber("error", 1);
                jsonResult.putString("desc", "Chua co thong tin");
                callback.handle(jsonResult);
                return;


            }
        });
    }

    //BEGIN upload visa promo
    //BEGIN 0000000004
    @Action(path = "/uploadVisaPromoFile")
    public void uploadVisaPromoFile(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/readVisaPromoFile")
    public void readVisaPromoFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<VisaPromoRow> visaRowsQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            visaRowsQueue.add(new VisaPromoRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (visaRowsQueue.size() > 1) {
            VisaPromoRow visaPromoRow = visaRowsQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        doVisaPromotion(visaRowsQueue, arrayResult, callback);
        log.writeLog();
    }

    private void doVisaPromotion(final Queue<VisaPromoRow> visaPromoRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {


        final Common.BuildLog log = new Common.BuildLog(logger);

        if (visaPromoRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final VisaPromoRow rec = visaPromoRowQueue.poll();

        if (rec == null) {
            doVisaPromotion(visaPromoRowQueue, arrayResult, callback);
        } else {

            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.phoneNumber) + "");
            final int tranType = DataUtil.strToInt(rec.tranType);
            final long tranId = DataUtil.strToLong(rec.tranId);
            final long tranAmount = DataUtil.strToLong(rec.totalAmount);
            final long visaAmount = DataUtil.strToLong(rec.visaAmount);
            final long visaTranId = DataUtil.strToLong(rec.visatranId);
            final String serviceId = rec.serviceId;
            final String cardnumber = rec.cardnumber;
            final long cashInTime = DataUtil.strToLong(rec.cashinTime);
            if (number > 0) {
                VisaMpointPromoObj.requestVisaMpointPromo(vertx, rec.phoneNumber, tranType, tranId, cardnumber, visaAmount, visaTranId, serviceId, tranAmount, cashInTime, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                        if (jsonObject != null) {
                            int error = jsonObject.getInteger("error", 1000);
                            String desc = jsonObject.getString("desc", "");
                            rec.error = error + "";
                            rec.errorDesc = desc;
                            arrayResult.add(rec.toJson());
                            doVisaPromotion(visaPromoRowQueue, arrayResult, callback);
                            log.writeLog();
                        }
                    }
                });

            }
        }
    }

    //End Upload visa promo
    // Load lai chuong trinh event config
    @Action(path = "/loadconfig/refreshdata")
    public void loadEventConfig(HttpRequestContext context, final Handler<JsonArray> callback) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.REFRESH_CONFIG_DATA;
        vertx.eventBus().send(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject jsonResponse = message.body();

                if (jsonResponse != null) {
                    callback.handle(jsonResponse.getArray("cdhh", new JsonArray()));
                    return;
                }


                callback.handle(new JsonArray());
            }
        });
    }

    //BEGIN upload visa promo
    //BEGIN 0000000004
    @Action(path = "/uploadRollbackPromoFile")
    public void uploadRollbackPromoFile(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/readRollbackPromoFile")
    public void readRollbackPromoFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<RollbackPromoRow> rollbackRowsQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            rollbackRowsQueue.add(new RollbackPromoRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (rollbackRowsQueue.size() > 1) {
            RollbackPromoRow rollbackPromoRow = rollbackRowsQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        doRollbackPromotion(rollbackRowsQueue, arrayResult, callback);
        log.writeLog();
    }

    //BEGIN 0000000055 Thu hoi gift loi

    private void doRollbackPromotion(final Queue<RollbackPromoRow> rollbackPromoRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {


        final Common.BuildLog log = new Common.BuildLog(logger);

        if (rollbackPromoRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final RollbackPromoRow rec = rollbackPromoRowQueue.poll();

        if (rec == null) {
            doRollbackPromotion(rollbackPromoRowQueue, arrayResult, callback);
        } else {

            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.phoneNumber) + "");
            final int tranType = DataUtil.strToInt(rec.tranType);
            final long tranId = DataUtil.strToLong(rec.tranId);
            final long tranAmount = DataUtil.strToLong(rec.totalAmount);
            final long visaAmount = DataUtil.strToLong(rec.visaAmount);
            final long visaTranId = DataUtil.strToLong(rec.visatranId);
            final String serviceId = rec.serviceId;
            final String cardnumber = rec.cardnumber;
            final String bankcode =  rec.bankcode;
            if (number > 0) {
//                RollBack50PerPromoObj.requestRollBack50PerPromo(vertx, rec.phoneNumber, tranType, tranId, cardnumber, visaAmount, visaTranId, serviceId, tranAmount, cashInTime, new Handler<JsonObject>() {
//                    @Override
//                    public void handle(JsonObject jsonObject) {
//                        if (jsonObject != null) {
//                            int error = jsonObject.getInteger("error", 1000);
//                            String desc = jsonObject.getString("desc", "");
//                            rec.error = error + "";
//                            rec.errorDesc = desc;
//                            arrayResult.add(rec.toJson());
//                            doRollbackPromotion(rollbackPromoRowQueue, arrayResult, callback);
//                            log.writeLog();
//                        }
//                    }
//                });
                RollBack50PerPromoObj.requestRollBack50PerPromo(vertx, rec.phoneNumber, tranType, tranId, StringConstUtil.RollBack50Percent.ROLLBACK_PROMO, cardnumber,tranAmount, bankcode, visaAmount, visaTranId, serviceId, 0, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                        if (jsonObject != null) {
                            int error = jsonObject.getInteger("error", 1000);
                            String desc = jsonObject.getString("desc", "");
                            int count = jsonObject.getInteger(VisaMpointPromoConst.COUNT, 0);
                            rec.error = error + "";
                            rec.errorDesc = desc;
                            rec.count = count + "";
                            arrayResult.add(rec.toJson());
                            doRollbackPromotion(rollbackPromoRowQueue, arrayResult, callback);
                            log.writeLog();
                        }
                    }
                });
            }
        }
    }

    //BEGIN 0000000022 Tim tat ca gift het han
    ///searchexpiredgift
    @Action(path = "/searchironmanerrorgift")
    public void searchIronManErrorGift(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "searchexpiredgift");
        MultiMap params = request.params();
        final String number = params.get(StringConstUtil.NUMBER) == null ? "" : params.get(StringConstUtil.NUMBER);
        long currentTime = System.currentTimeMillis();

        giftDb.findForExpiredNumber(currentTime, DataUtil.strToInt(number), new Handler<List<Gift>>() {
            @Override
            public void handle(List<Gift> gifts) {
                JsonArray array = new JsonArray();
                JsonObject jsonObject = new JsonObject();
                int i = 0;
                for (Gift gift : gifts) {
                    i = i + 1;
                    jsonObject = gift.toJsonObject();
                    jsonObject.putNumber("stt", i);
                    jsonObject.putString("start", DataUtil.timeDate(gift.startDate));
                    jsonObject.putString("end", DataUtil.timeDate(gift.endDate));
                    array.add(jsonObject);
                }
                response(request, array);
            }
        });
    }

    //BEGIN 0000000055 Thu hoi gift loi
    ///searchexpiredgift
    @Action(path = "/getironmanerrorgift")
    public void getIronManErrorGift(final HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "getExpiredGift");
        MultiMap params = request.params();

        final String number = params.get(StringConstUtil.NUMBER) == null ? "0" : params.get(StringConstUtil.NUMBER);

        long currentTime = System.currentTimeMillis();

        //final List<String> errorNumber = new ArrayList<>();
        final Queue<String> giftDequeue = new ArrayDeque<>();
        giftDb.getErrorGiftIronMan(new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray event) {
                if(event.size() > 0)
                {
                    for(int i = 0; i < event.size(); i++)
                    {
                        if(DataUtil.strToInt(((JsonObject) (event.get(i))).getString("_id")) > 0 && ((JsonObject) (event.get(i))).getInteger("count")
                                > 1 && !((JsonObject) (event.get(i))).getString("_id").equalsIgnoreCase("01000000000"))
                        {
                            //errorNumber.add(((JsonObject) (event.get(i))).getString("_id"));
                            giftDequeue.add(((JsonObject) (event.get(i))).getString("_id"));
                        }
                    }
                }
                if (giftDequeue.size() > 0) {
                    resolveErrorGift(giftDequeue, request);
                } else {
                    //Khong co so loi
                    JsonArray array = new JsonArray();
                    response(request, array);
                }

            }
        });
    }

    private void resolveErrorGift(final Queue<String> errorNumber, final HttpServerRequest request) {
        JsonObject ironmanConfig = glbConfig.getObject(StringConstUtil.IronManPromo.JSON_OBJECT, new JsonObject());

        final String EXPIRE_GIFT_MOMO = ironmanConfig.getString("agent", null);
        if (errorNumber == null || errorNumber.size() == 0) {
            JsonArray array = new JsonArray();
            response(request, array);
            isRunning = false;
            return;
        }
        String number = errorNumber.poll();
        giftDb.findOneIronManErrorGift(number, new Handler<List<Gift>>() {
            @Override
            public void handle(List<Gift> listGift) {
                if (listGift.size() > 0) {
                    final Gift gift = listGift.get(0);
                    //Thu hoi
                    final Common.BuildLog log = new Common.BuildLog(logger);
                    log.setPhoneNumber(gift.owner);

                    giftManager.transferGiftWhenExpired(gift, EXPIRE_GIFT_MOMO, null, log, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject result) {
                            log.add("transferGift result", result.encodePrettily());
                            int error = result.getInteger("error", -1000);
                            log.add("curDate", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                            log.add("startDate", Misc.dateVNFormatWithTime(gift.startDate.getTime()));
                            log.add("endDate", Misc.dateVNFormatWithTime(gift.endDate.getTime()));
                            log.add("amount", gift.amount);
                            log.add("tranId", gift.tranId);
                            log.add("typeId", gift.typeId);
                            log.add("statusdesc", Gift.getDesc(gift.status));
                            log.add("momo account", EXPIRE_GIFT_MOMO);
                            log.add("error", error);
                            log.add("errdesc", SoapError.getDesc(error));
                            log.writeLog();
                            if (errorNumber.size() > 0) {
                                resolveErrorGift(errorNumber, request);
                            } else {

                                JsonArray array = new JsonArray();
                                response(request, array);
                                isRunning = false;
                                return;

                            }
                        }
                    });
                }
            }
        });
    }

    @Action(path = "/uploadCustomerCareDollarHeartGroupFile")
    public void uploadCustomerCareDollarHeartGroupFile(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

//    public void insertDataIntoMongo(final Queue<String> queue,final DBCollection dbCollection_zalo_one)
//    {
//        final String data = queue.poll();
//        logger.debug("data " + data);
//
//
//
//
//        vertx.setTimer(30L, new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                dbCollection_zalo_one.insert(dbObject);
//                if(queue.size() > 0)
//                {
//                    logger.debug("insert success " + data);
//                    insertDataIntoMongo(queue, dbCollection_zalo_one);
//
//                }
//            }
//        });
//
//    }

    @Action(path = "/readCustomerCareDollarHeartGroupFile")
    public void readCustomerCareDollarHeartGroupFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
//        ArrayList<Object> arrayList = readDataFromExcelFile(filePath, hFile, logger);

        JsonArray jsonArrData = new JsonArray();
        for(int i = 0; i < arrayList.size(); i++)
        {
            jsonArrData.add(new ReturnGiftRow(arrayList.get(i).toString()).toJson());
        }
        arrayResult = new JsonArray();
        callback.handle(jsonArrData);
        log.writeLog();
    }

    @Action(path = "/readZaloGroupFile")
    public void readZaloGroupFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
//        ArrayList<Object> arrayList = readDataFromExcelFile(filePath, hFile, logger);

        JsonArray jsonArrData = new JsonArray();
        for (int i = 0; i < arrayList.size(); i++) {
            if (!"".equalsIgnoreCase(arrayList.get(i).toString())) {
                jsonArrData.add(new ReturnZaloRow(arrayList.get(i).toString()).toJson());
            }
        }
        arrayResult = new JsonArray();
        callback.handle(jsonArrData);
        log.writeLog();
    }

    @Action(path = "/saveMongoFromFile")
    public void saveMongoFromFile(HttpRequestContext context, final Handler<String> callback) throws UnknownHostException {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
        String result = "Insert ";
        List<String> arrayData = new ArrayList<>();
        MongoClient mongoClient = new MongoClient(zaloSaveMongoHost, 27017);
        DB db = mongoClient.getDB("newmomovn_db");
        DBCollection dbCollection_zalo_one = db.getCollection(zaloSaveMongoTable);
        BulkWriteOperation bulkWriteOperation = dbCollection_zalo_one.initializeOrderedBulkOperation();
        for(int i = 0; i < arrayList.size() ; i++)
        {
            final DBObject dbObject = new BasicDBObject().append("zalo_code", arrayList.get(i).toString());
            bulkWriteOperation.insert(dbObject);
        }
        bulkWriteOperation.execute();


//        insertDataIntoMongo(stringQueue, dbCollection_zalo_one);
        log.add("arrayData size", arrayData.size());

        result += arrayData.size() + " rows";
        log.add("result", "insert " + arrayData.size() + " rows");
        arrayResult = new JsonArray();
        callback.handle(result);
        log.writeLog();
    }

    @Action(path = "/saveMongoFromFileRepeat")
    public void saveMongoFromFileRepeat(HttpRequestContext context, final Handler<String> callback) throws UnknownHostException {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
        String result = "Insert repeat ";
        Collection arrayData = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            if (!"".equalsIgnoreCase(arrayList.get(i).toString())) {
                arrayData.add(arrayList.get(i).toString());
            }
        }
        log.add("arrayData size", arrayData.size());

        Collection listMongo = new ArrayList<>();
        MongoClient mongoClient = new MongoClient(zaloSaveMongoHost, 27017);
        DB db = mongoClient.getDB("newmomovn_db");
        DBCollection dbCollection_zalo_one = db.getCollection(zaloSaveMongoTable);
        DBCursor cursor = dbCollection_zalo_one.find();
        while (cursor.hasNext()) {
            DBObject dbo = cursor.next();
            listMongo.add(dbo.get("zalo_code").toString());
        }
        cursor.close();
        Collection<String> similar = new HashSet<String>(arrayData);
        Collection<String> different = new HashSet<String>();
        different.addAll(arrayData);

        similar.retainAll(listMongo);
        different.removeAll(similar);
        if (!different.isEmpty()) {
            for (String repeat : different) {
                DBObject dbObject = new BasicDBObject().append("zalo_code", repeat);
                dbCollection_zalo_one.insert(dbObject);
            }
        }
        result += different.size() + " rows";
        log.add("result", "insert " + different.size() + " rows");
        arrayResult = new JsonArray();
        callback.handle(result);
        log.writeLog();
    }

    @Action(path = "/refreshGlobalArrayResult")
    public void refreshGlobalArrayResult(HttpRequestContext context, final Handler<String> callback) {
        DollarHeartCustomerCareGiftGroupObj.requestCustomerCareGiftGroup(vertx, "", "", "", DataUtil.strToLong(""),
                DataUtil.strToInt("0"), "", "", new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                        int err = jsonObject.getInteger(StringConstUtil.ERROR, -1);
                        String desc = jsonObject.getString(StringConstUtil.DESCRIPTION, "clear");
                        logger.info("Description " + desc);
                        if(err == 0)
                        {
                            callback.handle("true");
                        }
                        else
                        {
                            callback.handle("false");
                        }

                    }
                }
        );
    }

    @Action(path = "/payDollarHeartGiftForCustomer")
    public void payDollarHeartGiftForCustomer(HttpRequestContext context, final Handler<JsonArray> callback) {
        String arrayData = context.getRequest().formAttributes().get("arrjson");
        final Common.BuildLog log = new Common.BuildLog(logger);
        JsonArray arrayList = new JsonArray(arrayData);
        final Queue<ReturnGiftRow> returnGiftRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            returnGiftRowQueue.add(new ReturnGiftRow((JsonObject)o));
        }
        if (arrayResult.size() == 0 && returnGiftRowQueue.size() > 1) {
            ReturnGiftRow returnGiftRow = returnGiftRowQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        int totalRows = returnGiftRowQueue.size();
        doReturnCustomerCareDollarHeartGroupGift(returnGiftRowQueue, arrayResult, totalRows, callback);
        log.writeLog();
    }

    @Action(path = "/sendZaloToCustomer")
    public void sendZaloToCustomer(HttpRequestContext context, final Handler<String> callback) {
        String arrayData = context.getRequest().formAttributes().get("arrjson");
        final String message = "Mã của bạn: %s, nhập ngay vào ứng dụng MoMo (mục Khuyến Mãi) để nhận quà là tiền mặt. Mã có giá trị đến ngày 2/2/2016. Cảm ơn bạn đã tham gia minigame của MoMo.";
        final Common.BuildLog log = new Common.BuildLog(logger);
        final JsonArray listZalo = new JsonArray(arrayData);
        final JsonArray listZaloEnd = new JsonArray();
        final AtomicInteger atomicInteger = new AtomicInteger(listZalo.size());

        final String fileName = "zaloresult" + System.currentTimeMillis() + ".txt";

        vertx.setPeriodic(2000, new Handler<Long>() {
            @Override
            public void handle(Long replyLong) {
                if (atomicInteger.decrementAndGet() < 0) {
                    log.writeLog();
                    logger.debug("List listZalo " + listZalo.size());
                    logger.debug("List listZaloEnd " + listZaloEnd.size());
                    vertx.cancelTimer(replyLong);
                    callback.handle(buildTable(listZaloEnd));

                    return;
                } else {
                    final int itemPosition = atomicInteger.intValue();

                    ConnectProcess connectProcess = new ConnectProcess(vertx, logger, glbConfig);
                    JsonObject zaloObj = listZalo.get(itemPosition);
                    String phone = zaloObj.getString("number").trim();
                    String code = zaloObj.getString("code").trim();
                    log.add("phone", "phone " + phone + " time " + System.currentTimeMillis());
                    log.add("code", code);
                    long phoneZalo = 0;
                    if (phone.length() > 0) {
                        phoneZalo = Long.valueOf("84" + phone.substring(1, phone.length()));
                    }
                    String messageZalo = String.format(message, code);
                    String result = "";
                    if (sendByZalo) {
                        result = connectProcess.sendByZalo(phoneZalo, messageZalo);
                    }
                    String error = "";
                    log.add("itemPosition", atomicInteger.intValue());
                    log.add("phoneZalo", phoneZalo);
                    log.add("result", result);
                    if (result.equalsIgnoreCase("")) {
                        error = "-1";
                        zaloObj.putString("error", error);
                        zaloObj.putString("errorDesc", result);
                    } else {
                        error = "0";
                        zaloObj.putString("error", error);
                        zaloObj.putString("errorDesc", result);
                    }
                    listZaloEnd.add(zaloObj);

                    String filePath = STATIC_FILE_DIRECTORY + fileName;
                    String resultText = phone + ";" + code + ";" + error + ";" + result;
                    Misc.writeAppendFile(resultText + System.lineSeparator(), filePath);
                    log.writeLog();
                }
            }
        });


        log.writeLog();
    }

    private void doReturnCustomerCareDollarHeartGroupGift(final Queue<ReturnGiftRow> returnGiftRowQueue
            , final JsonArray arrayResult
            , final int totalRows
            , final Handler<JsonArray> callback) {

        final JsonArray jsonPercentResult = new JsonArray();
        final Common.BuildLog log = new Common.BuildLog(logger);

        if (returnGiftRowQueue.size() == 0) {
            logger.info("DONE");
            callback.handle(arrayResult);
            return;
        }

        final ReturnGiftRow rec = returnGiftRowQueue.poll();

        if (rec == null) {
            doReturnCustomerCareDollarHeartGroupGift(returnGiftRowQueue, arrayResult, totalRows, callback);
        } else {
            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.number) + "");
            if (number > 0) {
                DollarHeartCustomerCareGiftGroupObj.requestCustomerCareGiftGroup(vertx, rec.number, rec.agent, rec.giftTypeId, DataUtil.strToLong(rec.giftValue),
                        DataUtil.strToInt(rec.duration), rec.program, rec.group, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject jsonObject) {
                                int err = jsonObject.getInteger("error", -1);
                                String desc = jsonObject.getString(StringConstUtil.DESCRIPTION, "Lỗi này không trả nội dung");
                                logger.info(rec.number + " error " + err);
                                rec.error = err + "";
                                rec.errorDesc = desc;
                                rec.time = "" + System.currentTimeMillis();
                                arrayResult.add(rec.toJson());
                                doReturnCustomerCareDollarHeartGroupGift(returnGiftRowQueue, arrayResult, totalRows, callback);
                            }
                        }
                );
            }
            else
            {
                doReturnCustomerCareDollarHeartGroupGift(returnGiftRowQueue, arrayResult, totalRows, callback);
            }
        }
    }

    public ArrayList<Object> readDataFromExcelFile(String filePath, String fileName, final Logger logger) {
        ArrayList<Object> dataFromExcelFile = new ArrayList<>();
//        NPOIFSFileSystem fs = new NPOIFSFileSystem(new File(filePath));
//        HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true);
        OPCPackage pkg = null;
        try {
            File file = new File(fileName);
            InputStream is = new FileInputStream(filePath);
//            POIFSFileSystem poifs = new POIFSFileSystem(file);
//            pkg = OPCPackage.open(file.getAbsoluteFile());

            XSSFWorkbook xssfWorkbook = null;
            xssfWorkbook = new XSSFWorkbook(is);
            Sheet sheet1 = xssfWorkbook.getSheetAt(0);
            for (Row row : sheet1) {
                String data = "";
                int numberOfRow = dataFromExcelFile.size();
                for (Cell cell : row) {
                    CellReference cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex());
                    logger.info(cellRef.formatAsString());
                    if(!"".equalsIgnoreCase(data))
                    {
                      data = data + ";";
                    }
                    boolean endData = false;
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            logger.info(cell.getRichStringCellValue().getString());
                            data = data + cell.getRichStringCellValue().getString();
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                logger.info(cell.getDateCellValue());
                                data = data + cell.getDateCellValue();
                            } else {
                                logger.info(cell.getNumericCellValue());
                                data = data + cell.getNumericCellValue();
                            }
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                            logger.info(cell.getBooleanCellValue());
                            data = data + cell.getBooleanCellValue();
                            break;
                        case Cell.CELL_TYPE_FORMULA:
                            logger.info(cell.getCellFormula());
                            data = data + cell.getBooleanCellValue();
                            break;
                        default:
                            logger.info("end data");
                            endData = true;
                            break;
                    }
                    dataFromExcelFile.add(data);
                    if(endData)
                        break;
                }
                if(numberOfRow == dataFromExcelFile.size())
                {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataFromExcelFile;
    }

    public String buildTable(JsonArray objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th>No</th>" +
                "  <th>Phone</th>" +
                "  <th>Code</th>" +
                "  <th>Error</th>" +
                "  <th>Zalo msg</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += getRowError(i, (JsonObject) objs.get(i));
            }
        }
        result += "</table>";
        return result;
    }

    public String getRowError(int position, JsonObject input) {
        String result = "";


        if (input.getString("error").equalsIgnoreCase("0")) {
            result += "<tr>\n";
        } else {
            result += "<tr style='color:red'>\n";
        }
        result +=
                "  <td rid='" + position + "'>" + position + "</td>" +
                        "  <td rid='" + position + "'>" + input.getString("number") + "</td>" +
                        "  <td rid='" + position + "'>" + input.getString("code") + "</td>" +
                        "  <td rid='" + position + "'>" + input.getString("error") + "</td>" +
                        "  <td rid='" + position + "'>" + input.getString("errorDesc") + "</td>" +
                        "</tr>";
        return result;
    }

    @Action(path = "/searchClaimCodeInfo")
    public void searchClaimCodeInfo(HttpRequestContext context, final Handler<JsonArray> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "checkStoreInfo");
        MultiMap params = request.params();

        final String number = params.get("phoneNumber") == null ? "" : params.get("phoneNumber");
        final String code = params.get("code") == null ? "" : params.get("code");
        final String device_imei = params.get("imei") == null ? "" : params.get("imei");
        final String program = params.get("program") == null ? "" : params.get("program");
        long currentTime = System.currentTimeMillis();
        final JsonObject jsonReply = new JsonObject();
        if (!"".equalsIgnoreCase(number) || !"".equalsIgnoreCase(code) || !"".equalsIgnoreCase(device_imei)|| !"".equalsIgnoreCase(program)) {
            JsonObject joFilter = new JsonObject();
            JsonObject joPhoneNumber = new JsonObject();
            JsonObject joImei = new JsonObject();
            JsonObject joCode = new JsonObject();

            joPhoneNumber.putString(colName.ClaimCode_AllCheckCols.PHONE, number);
            joImei.putString(colName.ClaimCode_AllCheckCols.DEVICE_IMEI, device_imei);
            joCode.putString(colName.ClaimCode_AllCheckCols.CODE, code);

            JsonArray joOr = new JsonArray();
            joOr.addObject(joPhoneNumber);
            joOr.addObject(joImei);
            joOr.addObject(joCode);

            joFilter.putArray(MongoKeyWords.OR, joOr);

            claimCode_allCheckDb.searchWithFilter(program, joFilter, new Handler<ArrayList<ClaimCode_AllCheckDb.Obj>>() {
                @Override
                public void handle(ArrayList<ClaimCode_AllCheckDb.Obj> listItems) {
                    JsonArray jarrResponse = new JsonArray();
                    for(int i = 0; i < listItems.size(); i++)
                    {
                        jarrResponse.addObject(listItems.get(i).toJson(0));
                    }
                    callback.handle(jarrResponse);
                }
            });
        }
    }

    @Action(path = "/mLogger")
    public void mLogger(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "mLogger");
        MultiMap params = request.params();

        final String number = params.get("phoneNumber") == null ? "" : params.get("phoneNumber");
        int phone_number = DataUtil.toInteger(number, 0);
        if (!"".equalsIgnoreCase(number) && phone_number > 0) {
            phonesDb.getPhoneObjInfo(phone_number, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj phoneObj) {
                    if(phoneObj != null)
                    {
                        callback.handle(phoneObj.toJsonObject());
                    }
                }
            });

        }
    }

    @Action(path = "/uploadCashback7PercentPromoFile")
    public void uploadCashback7PercentPromoFile(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/readCashback7PercentPromoFile")
    public void readCashback7PercentPromoFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        final Queue<Cashback7PercentRow> cashback7PercentRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            cashback7PercentRowQueue.add(new Cashback7PercentRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (cashback7PercentRowQueue.size() > 1) {
            Cashback7PercentRow cashback7PercentRow = cashback7PercentRowQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        doCashback7PercentPromotion(cashback7PercentRowQueue, arrayResult, callback);
        log.writeLog();
    }

    private void doCashback7PercentPromotion(final Queue<Cashback7PercentRow> cashback7PercentRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {


        final Common.BuildLog log = new Common.BuildLog(logger);

        if (cashback7PercentRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final Cashback7PercentRow rec = cashback7PercentRowQueue.poll();

        if (rec == null) {
            doCashback7PercentPromotion(cashback7PercentRowQueue, arrayResult, callback);
        } else {

            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.phoneNumber) + "");
            final long tranId = DataUtil.strToLong(rec.tranId);
            final long amount = DataUtil.strToLong(rec.amount);
            final long time = DataUtil.strToLong(rec.time);
            final long promotion_time = DataUtil.strToLong(rec.promotion_time);
            final String serviceId = rec.serviceId;
            if (number > 0) {
                zaloTetPromotionDb.findOne(colName.ZaloTetPromotionCol.TABLE_ALL, rec.phoneNumber, new Handler<ZaloTetPromotionDb.Obj>() {
                    @Override
                    public void handle(ZaloTetPromotionDb.Obj zaloTetObj) {

                        if(zaloTetObj != null)
                        {
                            ZaloTetPromotionObj.requestZaloPromo(vertx, rec.phoneNumber, "zalocode", StringConstUtil.ZaloPromo.ZALO_CASHBACK_PROGRAM, "last_imei", amount, tranId, serviceId, promotion_time, time, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jsonObject) {
                                    logger.info("Da tra thuong 7%");
                                    if (jsonObject != null) {
                                        int error = jsonObject.getInteger("error", 1000);
                                        String desc = jsonObject.getString("desc", "");
                                        long tid_bonus = jsonObject.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                        long amount = jsonObject.getLong(StringConstUtil.ZaloPromo.AMOUNT, 0);
                                        rec.error = error + "";
                                        rec.errorDesc = desc;
                                        rec.amount_bonus = amount + "";
                                        rec.tranIdBonus = tid_bonus + "";
                                        arrayResult.add(rec.toJson());
                                        doCashback7PercentPromotion(cashback7PercentRowQueue, arrayResult, callback);
                                        log.writeLog();
                                    }
                                }
                            });
                        }
                        else
                        {
                            rec.amount_bonus = "0";
                            rec.tranIdBonus = "0";
                            rec.error = 9999 + "";
                            rec.errorDesc = "Số này chưa claim code mà trả gì";
                            arrayResult.add(rec.toJson());
                            doCashback7PercentPromotion(cashback7PercentRowQueue, arrayResult, callback);
                            log.writeLog();
                        }

                    }
                });

            }
        }
    }

    @Action(path = "/uploadLiXiFile")
    public void uploadLiXiFile(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/readLixiFile")
    public void readLixiFile(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        String agent_ = params.get("agent") == null ? "" : params.get("agent").trim();
        String password_ = params.get("password") == null ? "" : params.get("password").trim();
        String code_ = params.get("code") == null ? "" : params.get("code").trim();
        this.agent = agent_;
        this.password = password_;
        this.code = code_;
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
//        ArrayList<Object> arrayList = readDataFromExcelFile(filePath, hFile, logger);

        JsonArray jsonArrData = new JsonArray();
        for(int i = 0; i < arrayList.size(); i++)
        {
            jsonArrData.add(new LixiRow(arrayList.get(i).toString()).toJson());
        }
        arrayResult = new JsonArray();
        callback.handle(jsonArrData);
        log.writeLog();
    }

    @Action(path = "/payLixiForEmployee")
    public void payLixiForEmployee(HttpRequestContext context, final Handler<JsonArray> callback) {
        String arrayData = context.getRequest().formAttributes().get("arrjson");
        MultiMap params = context.getRequest().params();

        final Common.BuildLog log = new Common.BuildLog(logger);
        JsonArray arrayList = new JsonArray(arrayData);
        final Queue<LixiRow> lixiRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            lixiRowQueue.add(new LixiRow((JsonObject)o));
        }
        if (arrayResult.size() == 0 && lixiRowQueue.size() > 1) {
            LixiRow lixiRow = lixiRowQueue.poll();
        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        int totalRows = lixiRowQueue.size();
        doLixi(lixiRowQueue, arrayResult, agent, password, code, callback);
        log.writeLog();
    }

    private void doLixi(final Queue<LixiRow> lixiRowQueue
            , final JsonArray arrayResult
            , final String agent_name
            , final String pin
            , final String code
            , final Handler<JsonArray> callback) {


        final Common.BuildLog log = new Common.BuildLog(logger);

        if (lixiRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }
        final LixiRow rec = lixiRowQueue.poll();
        if (rec == null) {
            doLixi(lixiRowQueue, arrayResult, agent_name, pin, code, callback);
        } else {
            final int number = DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(rec.phoneNumber) + "");
            final long amount = DataUtil.strToLong(rec.amount);
            if (number > 0) {
                LixiManageDb.Obj lixiObj = new LixiManageDb.Obj();
                lixiObj.money_value = amount;
                lixiObj.phone_number = rec.phoneNumber + "_" + code;
                lixiObj.time = System.currentTimeMillis();
                lixiManageDb.insert(lixiObj, new Handler<Integer>() {
                    @Override
                    public void handle(Integer result) {
                        if(result == 0)
                        {
                            JsonObject jsonDataClient = new JsonObject();
                            jsonDataClient.putString("title", rec.title);
                            jsonDataClient.putString("body", rec.body);
                            jsonDataClient.putString("code", code);
                            jsonDataClient.putString("name", rec.name);
                            transferProcess.processM2MerchantTransfer(pin, null, "", null, "m2m", amount, rec.phoneNumber, agent_name, "lixi", jsonDataClient, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jObjFromSoap) {                                                                                   //{"status":5,"error":1001,"tranId":17796333,"amt":100000,"io":-1,"ftime":1447471353200,"cusnum":"0934123456"}
                                    int error = jObjFromSoap.getInteger("error", -1);
                                    long tid = jObjFromSoap.getLong("tranId", 0);
                                    if (error == 0) {
                                        log.add("desc", "m2Merchant success");
                                        String desc = "Ngon";
                                        rec.error = error + "";
                                        rec.errorDesc = desc;
                                        rec.tranId = tid + "";
                                        rec.time = System.currentTimeMillis() + "";
                                        arrayResult.add(rec.toJson());
                                        log.writeLog();
                                        DollarHeartCustomerCareGiftGroupDb.Obj dollarObj = new DollarHeartCustomerCareGiftGroupDb.Obj();
                                        dollarObj.duration = 100;
                                        dollarObj.group = "lixi";
                                        dollarObj.tranId = tid;
                                        dollarObj.gift_amount = amount;
                                        dollarObj.gift_id = "lixi";
                                        dollarObj.money_value = amount;
                                        dollarObj.program = "lixi";
                                        dollarObj.phone_number = rec.phoneNumber;
                                        dollarObj.time = System.currentTimeMillis();
                                        dollarHeartCustomerCareGiftGroupDb.insert(dollarObj, new Handler<Integer>() {
                                            @Override
                                            public void handle(Integer event) {
                                                doLixi(lixiRowQueue, arrayResult, agent_name, pin, code, callback);
                                            }
                                        });
                                    } else {
                                        log.add("desc", "m2Merchant fail");
                                        String desc = "Fail";
                                        rec.tranId = tid + "";
                                        rec.error = error + "";
                                        rec.errorDesc = desc;
                                        rec.time = System.currentTimeMillis() + "";
                                        arrayResult.add(rec.toJson());
                                        log.writeLog();
                                        doLixi(lixiRowQueue, arrayResult, agent_name, pin, code, callback);
                                    }
                                }
                            });
                        }
                        else
                        {
                            log.add("desc", "Da duoc li xi roi ku");
                            String desc = "Da duoc li xi roi ku";
                            rec.tranId = 0 + "";
                            rec.error = 1000 + "";
                            rec.errorDesc = desc;
                            rec.time = System.currentTimeMillis() + "";
                            arrayResult.add(rec.toJson());
                            log.writeLog();
                            doLixi(lixiRowQueue, arrayResult, agent_name, pin, code, callback);
                        }
                    }
                });
            }
            else {
                log.add("desc", "Sai so dien thoai");
                String desc = "Sai so dien thoai";
                rec.tranId = 0 + "";
                rec.error = 1000 + "";
                rec.errorDesc = desc;
                rec.time = System.currentTimeMillis() + "";
                arrayResult.add(rec.toJson());
                log.writeLog();
                doLixi(lixiRowQueue, arrayResult, agent_name, pin, code, callback);
            }
        }
    }

    @Action(path = "/getConnectorService")
    public void getConnectorService(HttpRequestContext context, final Handler<Object> callback) {
        int command = Common.ServiceReq.COMMAND.GET_CONNECTOR_SERVICE_BUS_NAME;
        ServiceUtil.loadConnectorServiceBusName(command, vertx, logger, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray serviceArr) {
                callback.handle(serviceArr);
            }
        });
    }

    @Action(path = "/updateConnectorService")
    public void updateConnectorService(HttpRequestContext context, final Handler<Object> callback) {

        HttpServerRequest request = context.getRequest();
        MultiMap params = request.params();
        String service_id = params.get("service_id") == null ? "" : params.get("service_id").trim();
        String busname = params.get("busname") == null ? "" : params.get("busname").trim();
        String billpay = params.get("billpay") == null ? "" : params.get("billpay").trim();
        int service_type = params.get("service_type") == null ? 0 : DataUtil.strToInt(params.get("service_type").trim());
        String service_type_name = params.get("service_type_name") == null ? "" : params.get("service_type_name").trim();
        String type = params.get("type") == null ? "" : params.get("type").trim();

        final int command = Common.ServiceReq.COMMAND.UPDATE_CONNECTOR_SERVICE_BUS_NAME;
        ConnectorProxyBusNameDb.Obj connectorProxyObj = new ConnectorProxyBusNameDb.Obj();
        connectorProxyObj.serviceId = service_id;
        connectorProxyObj.serviceType = service_type;
        connectorProxyObj.busName = busname;
        connectorProxyObj.billPay = billpay;
        connectorProxyObj.serviceTypeName = service_type_name;
        connectorProxyObj.type = type;
        connectorProxyBusNameDb.upsert(connectorProxyObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(result);
                ServiceUtil.updateConnectorServiceBusName(command, vertx, logger);
            }
        });

    }

    @Action(path = "/reloadConnectorService")
    public void reloadConnectorService(HttpRequestContext context, final Handler<Object> callback) {
        int command = Common.ServiceReq.COMMAND.RELOAD_CONNECTOR_SERVICE_BUS_NAME;
        ServiceUtil.reloadConnectorServiceBusName(command, vertx, logger, new Handler<Boolean>() {
            @Override
            public void handle(Boolean isReload) {
                callback.handle(isReload);
            }
        });
    }

    //GET GIFT RULE
    @Action(path = "/getServiceGiftRule")
    public void getServiceGiftRules(HttpRequestContext context, final Handler<Object> callback) {
        int command = Common.ServiceReq.COMMAND.GET_SERVICE_GIFT_RULES;
        ServiceUtil.loadServiceGiftRules(command, vertx, logger, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray serviceArr) {
                callback.handle(serviceArr);
            }
        });
    }

    @Action(path = "/updateServiceGiftRules")
    public void updateServiceGiftRules(HttpRequestContext context, final Handler<Object> callback) {

        HttpServerRequest request = context.getRequest();
        MultiMap params = request.params();

        String service_id = params.get("service_id") == null ? "" : params.get("service_id").trim();
        String min_amount_service = params.get("min_amount_service") == null ? "" : params.get("min_amount_service").trim();
        String rule_service = params.get("rule_service") == null ? "" : params.get("rule_service").trim();

        final int command = Common.ServiceReq.COMMAND.UPDATE_SERVICE_GIFT_RULES;
        CheckServiceGiftRuleDb.Obj cheObj = new CheckServiceGiftRuleDb.Obj();
        cheObj.serviceId = service_id;
        cheObj.min_amount = DataUtil.strToLong(min_amount_service);
        cheObj.check_service = "true".equalsIgnoreCase(rule_service) ? true : false;

        checkServiceGiftRuleDb.upsert(cheObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(result);
                ServiceUtil.updateServiceGiftRules(command, vertx, logger);
            }
        });

    }

    @Action(path = "/reloadServiceGiftRule")
    public void reloadServiceGiftRule(HttpRequestContext context, final Handler<Object> callback) {
        int command = Common.ServiceReq.COMMAND.RELOAD_CONNECTOR_SERVICE_GIFT_RULES;
        ServiceUtil.reloadServiceGiftRules(command, vertx, logger, new Handler<Boolean>() {
            @Override
            public void handle(Boolean isReload) {
                callback.handle(isReload);
            }
        });
    }

    @Action(path = "/updateAllServiceGiftRules")
    public void updateAllServiceGiftRules(HttpRequestContext context, final Handler<Object> callback) {

        HttpServerRequest request = context.getRequest();
        MultiMap params = request.params();

        String rule_all = params.get("rule_all") == null ? "" : params.get("rule_all").trim();
        String min_amount_all = params.get("min_amount_all") == null ? "" : params.get("min_amount_all").trim();

        boolean checkRuleAll = "true".equalsIgnoreCase(rule_all) ? true : false;
        CheckServiceGiftRuleDb.Obj cheObj = new CheckServiceGiftRuleDb.Obj();
        if(checkRuleAll)
        {
            cheObj.min_amount = DataUtil.strToLong(min_amount_all);
            cheObj.check_all = checkRuleAll;
            cheObj.check_service = true;
        }
        else{
            cheObj.check_all = checkRuleAll;
            cheObj.check_service = false;
        }
        if("".equalsIgnoreCase(min_amount_all))
        {
            callback.handle(false);
            return;
        }
        final int command = Common.ServiceReq.COMMAND.UPDATE_SERVICE_GIFT_RULES;
        JsonObject joUpdate = cheObj.toJson();
        joUpdate.removeField(colName.CheckServiceGiftRuleCol.SERVICE_ID);
        checkServiceGiftRuleDb.updateAllField(joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(result);
                ServiceUtil.updateServiceGiftRules(command, vertx, logger);
            }
        });
    }



    // CLAIM CODE PROMOTION

    @Action(path = "/getClaimCodePromotion")
    public void getClaimCodePromotion(HttpRequestContext context, final Handler<Object> callback) {
        int command = 1;
        ClaimCodePromotionObj.loadClaimedCodePromo(vertx, command, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jarrClaimedCodePromotions) {
                callback.handle(jarrClaimedCodePromotions);
            }
        });
    }

    @Action(path = "/updateClaimCodePromotion")
    public void updateClaimCodePromotion(HttpRequestContext context, final Handler<Object> callback) {

        HttpServerRequest request = context.getRequest();
        MultiMap params = request.params();
        String _id = params.get("_id") == null ? "" : params.get("_id").trim();
        String promotion_name= params.get("promotion_name") == null ? "" : params.get("promotion_name").trim();
        String group= params.get("group") == null ? "" : params.get("group").trim();
        String prefix= params.get("prefix") == null ? "" : params.get("prefix").trim();
        String active= params.get("active") == null ? "" : params.get("active").trim();
        String number_of_gift= params.get("number_of_gift") == null ? "" : params.get("number_of_gift").trim();
        String gift_list= params.get("gift_list") == null ? "" : params.get("gift_list").trim();
        String gift_time= params.get("gift_time") == null ? "" : params.get("gift_time").trim();
        String momo_money= params.get("momo_money") == null ? "" : params.get("momo_money").trim();
        String money_time= params.get("money_time") == null ? "" : params.get("money_time").trim();
        String check_phone= params.get("check_phone") == null ? "" : params.get("check_phone").trim();
        String partnerName= params.get("partnerName") == null ? "" : params.get("partnerName").trim();
        String serviceId= params.get("serviceId") == null ? "" : params.get("serviceId").trim();
        String agent= params.get("agent") == null ? "" : params.get("agent").trim();
        String get_back_money= params.get("get_back_money") == null ? "" : params.get("get_back_money").trim();
        String uncheckDevice= params.get("uncheckDevice") == null ? "" : params.get("uncheckDevice").trim();
        String isMoMoPromotion= params.get("isMoMoPromotion") == null ? "" : params.get("isMoMoPromotion").trim();
        String notiTitle= params.get("notiTitle") == null ? "" : params.get("notiTitle").trim();
        String notiBody= params.get("notiBody") == null ? "" : params.get("notiBody").trim();
        String transBody= params.get("transBody") == null ? "" : params.get("transBody").trim();
        String notiRollbackTitle = params.get("notiRollbackTitle") == null ? "" : params.get("notiRollbackTitle").trim();
        String notiRollbackBody= params.get("notiRollbackBody") == null ? "" : params.get("notiRollbackBody").trim();
        String transRollbackBody= params.get("transRollbackBody") == null ? "" : params.get("transRollbackBody").trim();

        final int command = 2;
        ClaimCodePromotionDb.Obj claObj = new ClaimCodePromotionDb.Obj();
        claObj.promotion_id = _id.trim();
        claObj.promotion_name = promotion_name.trim();
        claObj.group = DataUtil.strToInt(group.trim());
        claObj.prefix = prefix.trim();
        claObj.activePromo = Boolean.parseBoolean(active);
        claObj.number_of_gift = DataUtil.strToInt(number_of_gift);
        claObj.gift_list = gift_list.trim();
        claObj.gift_time = DataUtil.strToInt(gift_time);
        claObj.momo_money = DataUtil.strToLong(momo_money);
        claObj.money_time = DataUtil.strToInt(money_time);
        claObj.check_phone = Boolean.parseBoolean(check_phone);
        claObj.partnerName = partnerName;
        claObj.serviceId = serviceId;
        claObj.agent = agent;
        claObj.getBackMoney = Boolean.parseBoolean(get_back_money);
        claObj.notiTitle = notiTitle;
        claObj.notiBody = notiBody;
        claObj.transBody = transBody;
        claObj.notiRollbackTitle = notiRollbackTitle;
        claObj.notiRollbackBody = notiRollbackBody;
        claObj.transRollbackBody = transRollbackBody;
        claObj.uncheckDevice = Boolean.parseBoolean(uncheckDevice);
        claObj.isMoMoPromotion = Boolean.parseBoolean(isMoMoPromotion);
        claimCodePromotionDb.upSert(_id, claObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                ClaimCodePromotionObj.updateClaimedCodePromo(command, vertx);
                callback.handle(result);
            }
        });


    }

    @Action(path = "/reloadClaimCodePromotion")
    public void reloadClaimCodePromotion(HttpRequestContext context, final Handler<Object> callback) {
        int command = 3;
        ClaimCodePromotionObj.reloadClaimedCodePromo(vertx, command, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(result);
            }
        });
    }

    @Action(path = "/searchClaimCode")
    public void searchClaimCode(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "mLogger");
        MultiMap params = request.params();
        final String code = params.get("code") == null ? "" : params.get("code");
        final String program = params.get("program") == null ? "" : params.get("program");
        claimCode_codeCheckDb.findOne(code, program, new Handler<ClaimCode_CodeCheckDb.Obj>() {
            @Override
            public void handle(ClaimCode_CodeCheckDb.Obj codeObj) {
                callback.handle(codeObj.toJson());
            }
        });
    }

    @Action(path = "/updateClaimCode")
    public void updateClaimCode(HttpRequestContext context, final Handler<Boolean> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "mLogger");
        MultiMap params = request.params();
        final String code = params.get("code") == null ? "" : params.get("code");
        final String program = params.get("program") == null ? "" : params.get("program");
        final String enabled = params.get("enabled") == null ? "" : params.get("enabled");
        JsonObject joUpdate = new JsonObject();
        joUpdate.putBoolean(colName.ClaimCode_CodeCheckCols.ENABLED, Boolean.parseBoolean(enabled));
        claimCode_codeCheckDb.updatePartial(code, program, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(result);
            }
        });
    }


    @Action(path = "/uploadEmailPopupFile")
    public void replyEmailPopupUpload(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }


    @Action(path = "/readEmailPopupFile")
    public void readEmailPopupFile(HttpRequestContext context, final Handler<Boolean> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String hFile = params.get("hFile") == null ? "" : params.get("hFile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
//
//        final Queue<String> phoneQueued = new ArrayDeque<>();
//        for (Object o : arrayList) {
//            phoneQueued.add(o.toString());
//        }
//
//        final JsonArray arrayResult = new JsonArray();
//
//        if (phoneQueued.size() > 1) {
//            String customerCareRow = phoneQueued.poll();
//        }

        //start to process send gift as promotion
        //doCustomerCarePromotion(customerCareRowsQueue,arrayResult,callback);
        doInsertEmailPopup(arrayList, callback);
        log.writeLog();
    }

    public void doInsertEmailPopup(ArrayList<Object> arrayList, final Handler<Boolean> callback)
    {
        String host = joMongo.getString("host", "172.16.44.175");
        int port = joMongo.getInteger("port", 27017);
        String dbName = joMongo.getString("db_name", "newmomovn_db");
        MongoClient mongoClientLocal = null;
        try {
            mongoClientLocal = new MongoClient( host , port);
            DB dbLocal = mongoClientLocal.getDB( dbName );
            DBCollection dbwoman = dbLocal.getCollection("EmailPopupDb");
            BulkWriteOperation bulkWriteOperation1 = dbwoman.initializeUnorderedBulkOperation();
            BulkWriteOperation bulkWriteOperation = dbwoman.initializeOrderedBulkOperation();
            for(int i = 0; i < arrayList.size() ; i++)
            {
                final DBObject dbObject = new BasicDBObject().append("_id", arrayList.get(i).toString().trim()).append("enable", true).append("email", "");
                bulkWriteOperation1.insert(dbObject);
            }
            bulkWriteOperation1.execute();
            callback.handle(true);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            callback.handle(false);
            return;
        }
        if(mongoClientLocal == null)
        {
            callback.handle(false);
            return;
        }

    }


    @Action(path = "/removeAllEmailData")
    public void removeAllEmailData(HttpRequestContext context, final Handler<Boolean> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        emailPopupDb.removeAllData(new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callback.handle(result);
            }
        });
        log.writeLog();
    }
}