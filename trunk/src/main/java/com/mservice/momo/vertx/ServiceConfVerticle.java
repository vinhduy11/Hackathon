package com.mservice.momo.vertx;

import com.mservice.momo.data.*;
import com.mservice.momo.data.connector.ConnectorProxyBusNameDb;
import com.mservice.momo.data.ironmanpromote.IronManBonusTrackingTableDb;
import com.mservice.momo.data.ironmanpromote.IronManRandomGiftManageDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.servicegiftrule.CheckServiceGiftRuleDb;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.data.web.ServiceDetailDb;
import com.mservice.momo.data.web.ServicePackageDb;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.object.FeeObject;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.gift.ExpiredGiftTimer;
import com.mservice.momo.vertx.gift.GiftRemindTimer;
import com.mservice.momo.vertx.gift.GiftRollback;
import com.mservice.momo.vertx.gift.SendGiftTimer;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.*;

/**
 * Created by locnguyen on 13/08/2014.
 */
public class ServiceConfVerticle extends Verticle {

    private Logger logger;
    private EventBus eventBus;

    private ServiceDb serviceDb;
    private ServiceDetailDb serviceDetailDb;
    private ServicePackageDb servicePackageDb;
    private WholeSystemPauseDb wholeSystemPauseDb;

    private MinMaxTranDb minMaxTranDb;
    private FeeDb feeDb;
    private ServiceFeeDb serviceFeeDb;
    private SettingsDb settingsDb;
    private CDHHPayBackSetting cdhhPayBackSetting;
    private CdhhConfigDb cdhhConfigDb;
    private ServiceCategory serviceCategory;

    private JsonArray listService;
    //private JsonArray listStoreService;
    private JsonArray listServiceEvent;
    private HashMap<String, JsonArray> listServiceDetail;
    private HashMap<String, HashMap<String, JsonArray>> listServicePackage;
    private JsonObject jsonShoppingVertilce;
    private HashMap<String, ServiceCategory.Obj> hashMapServiceCateogry = new HashMap<>();
    private JsonArray arrForm = new JsonArray();

    private JsonArray listMinMaxTran;
    private ArrayList<FeeDb.Obj> listFee;
    private ArrayList<ServiceFeeDb.Obj> listServiceFee;
    private ArrayList<FeeObject> feeObjects = new ArrayList<>();
    private ArrayList<FeeObject> serviceFeeObjects = new ArrayList<>();
    private JsonObject serverOnOff;

    //cdhh 2014
    private boolean cdhhOnOff = false;

    private ArrayList<CDHHPayBackSetting.Obj> paybackSetting = new ArrayList<>();

    private List<CdhhConfig> arrayCHDDWeekOrAquarterConfig = new ArrayList<>();

    //finalcial student contest 2014
    private JsonObject jsonFSCSetting = new JsonObject();
    private ServiceForm serviceForm;
    private boolean uatServer = false;
    private boolean isAndroid = false;

    private CardTypeDb cardTypeDb = null;
    private HashMap<String, FeeDb.Obj> hashMapFee = new HashMap<String, FeeDb.Obj>();
    private HashMap<String, ServiceFeeDb.Obj> hashMapServiceFee = new HashMap<String, ServiceFeeDb.Obj>();
    //private ArrayList<String, FeeDb.Obj> hashMapFee = new HashMap<String, FeeDb.Obj>();
    private Object lckCardType = new Object();
    private ArrayList<CardTypeDb.Obj> cardTypeArray = new ArrayList<CardTypeDb.Obj>();

    private IronManRandomGiftManageDb ironManRandomGiftManageDb;
    private JsonArray ironRandomGiftJsonArray = new JsonArray();

    private JsonArray ironManTrackingTable = new JsonArray();
    private IronManBonusTrackingTableDb ironManBonusTrackingTableDb ;
    private ArrayList<ConnectorProxyBusNameDb.Obj> connectorProArrayList;
    private ArrayList<CheckServiceGiftRuleDb.Obj> serviceGiftRuleArrList;
    private ConnectorProxyBusNameDb connectorProxyBusNameDb;
    private CheckServiceGiftRuleDb serviceGiftRuleDb;
    private boolean isStoreApp = false;
    private SpecialGroupDb specialGroupDb;
    @Override
    public void start() {

        logger = container.logger();
        eventBus = vertx.eventBus();
        logger.info("Starting ServicePartnerVerticle");

        serviceDb = new ServiceDb(vertx.eventBus(), logger);
        serviceDetailDb = new ServiceDetailDb(vertx.eventBus(), logger);
        servicePackageDb = new ServicePackageDb(vertx.eventBus(), logger);
        wholeSystemPauseDb = new WholeSystemPauseDb(vertx.eventBus(), logger);
        minMaxTranDb = new MinMaxTranDb(vertx.eventBus(), logger);
        feeDb = new FeeDb(vertx, logger);
        serviceFeeDb = new ServiceFeeDb(vertx, logger);
        cdhhPayBackSetting = new CDHHPayBackSetting(vertx.eventBus(), logger);

        listService = new JsonArray();
        listServiceFee = new ArrayList<>();
        listServiceEvent = new JsonArray();
        listServiceDetail = new HashMap<>();
        listServicePackage = new HashMap<>();
        serverOnOff = new JsonObject();
        serviceCategory = new ServiceCategory(vertx.eventBus(), logger);

        listMinMaxTran = new JsonArray();
        listFee = new ArrayList<>();

        settingsDb = new SettingsDb(vertx.eventBus(), logger);
        cdhhConfigDb = new CdhhConfigDb(vertx, logger);
        serviceForm = new ServiceForm(vertx.eventBus(), logger);

        JsonObject glbCfg = container.config();
        uatServer = glbCfg.getBoolean("uat", false);
        isAndroid = glbCfg.getBoolean("isAndroidServer", false);
        cardTypeDb = new CardTypeDb(vertx, logger);
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        jsonShoppingVertilce = new JsonObject();
        connectorProArrayList = new ArrayList<>();
        connectorProxyBusNameDb = new ConnectorProxyBusNameDb(vertx, logger);
        serviceGiftRuleArrList = new ArrayList<>();
        serviceGiftRuleDb = new CheckServiceGiftRuleDb(vertx, logger);
        specialGroupDb = new SpecialGroupDb(vertx, logger);
        loadAllConfig();

        // start gift transaction rollback timer.
        logger.info("GiftRollback");
        new GiftRollback(vertx, logger, glbCfg);

        // start timer scan gifts will be sent out withtimed
        logger.info("SendGiftTimer");
        new SendGiftTimer(vertx, logger, glbCfg);

        //start timer scan gifts will be expired
        logger.info("ExpiredGiftTimer");
        new ExpiredGiftTimer(vertx, logger, glbCfg);

        // start timer scan gifts will be reminded to used
        logger.info("GiftRemindTimer");
        new GiftRemindTimer(vertx, logger, glbCfg);


        Handler<Message<JsonObject>> getServiceHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> msg) {
                final Common.ServiceReq serviceReq = new Common.ServiceReq(msg.body());

                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("ServicePartnerVerticle");
                log.add("server prefix", AppConstant.PREFIX);
                log.add("time", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                logger.info("SERVICE CONFIG RECEIVE " + msg.body());
                switch (serviceReq.Command) {

                    //for get information
                    case Common.ServiceReq.COMMAND.GET_SERVICE:
                        log.add("command type", "GET_SERVICE");
                        log.add("service list size", listService.size());

                        JsonArray array = new JsonArray();

                        for (Object o : listService) {
                            JsonObject jo = (JsonObject) o;
                            boolean isUAT = jo.getBoolean(colName.ServiceCols.UAT, false);

                            //server UAT
                            if (uatServer) {
                                array.add(jo);
                            } else {
                                //dich vu dang cau hinh tren uat
                                if (isUAT) {
                                    jo.putNumber(colName.ServiceCols.STATUS, 0); // off dich vu
                                }
                                array.add(jo);
                            }
                        }

                        msg.reply(array);
                        break;
                    case Common.ServiceReq.COMMAND.GET_SERVICE_EVENT:
                        log.add("command type", "GET_SERVICE_EVENT");
                        log.add("service event list size", listServiceEvent.size());

                        JsonArray arrayEvent = new JsonArray();

                        for (Object o : listServiceEvent) {
                            JsonObject jo = (JsonObject) o;
                            boolean isUAT = jo.getBoolean(colName.ServiceCols.UAT, false);

                            //server UAT
                            if (uatServer) {
                                arrayEvent.add(jo);
                            } else {
                                //dich vu dang cau hinh tren uat
                                if (isUAT) {
                                    jo.putNumber(colName.ServiceCols.STATUS, 0); // off dich vu
                                }
                                arrayEvent.add(jo);
                            }
                        }

                        msg.reply(arrayEvent);
                        break;
                    case Common.ServiceReq.COMMAND.GET_SERVICE_DETAIL_BY_SERVICE_ID:
                        log.add("command type", "GET_SERVICE_DETAIL_BY_SERVICE_ID");
                        log.add("service detail list size", listServiceDetail.size());
                        log.add("service id", serviceReq.ServiceId);

                        if (listServiceDetail.containsKey(serviceReq.ServiceId)) {
                            msg.reply(listServiceDetail.get(serviceReq.ServiceId));
                        } else {
                            msg.reply(new JsonArray());
                        }
                        break;

                    case Common.ServiceReq.COMMAND.GET_PACKAGE_LIST:
                        log.add("command type", "GET_PACKAGE_LIST");
                        log.add("service package list size", listServicePackage.size());
                        log.add("service id", serviceReq.ServiceId);
                        log.add("package type", serviceReq.PackageType);

                        JsonArray arrayReply = new JsonArray();
                        if (serviceReq.ServiceId != null
                                && !serviceReq.ServiceId.isEmpty()
                                && listServicePackage.containsKey(serviceReq.ServiceId)) {

                            if (serviceReq.PackageType != null && !serviceReq.PackageType.isEmpty()) {

                                if (listServicePackage.get(serviceReq.ServiceId).containsKey(serviceReq.PackageType)) {
                                    JsonArray arrayPackageList = listServicePackage.get(serviceReq.ServiceId).get(serviceReq.PackageType);
                                    for (int i = 0; i < arrayPackageList.size(); i++) {
                                        JsonObject jo = arrayPackageList.get(i);

                                        if ("".equalsIgnoreCase(jo.getString(colName.ServicePackage.PARENT_ID, ""))) {
                                            arrayReply.add(jo);
                                        }
                                    }
                                }
                                msg.reply(arrayReply);

                            } else {

                                for (Map.Entry<String, JsonArray> entry : listServicePackage.get(serviceReq.ServiceId).entrySet()) {
                                    for (Object o : entry.getValue()) {
                                        JsonObject jo = (JsonObject) o;
                                        if ("".equalsIgnoreCase(jo.getString(colName.ServicePackage.PARENT_ID, ""))) {
                                            arrayReply.add(jo);
                                        }
                                    }
                                }
                                msg.reply(arrayReply);
                            }
                        } else {
                            msg.reply(arrayReply);
                        }
                        break;
                    case Common.ServiceReq.COMMAND.GET_PACKAGE_LIST_BY_PARENT_ID:
                        JsonArray arrayPkReply = new JsonArray();
                        String packageId = serviceReq.PackageId;

                        for (Map.Entry<String, JsonArray> entry : listServicePackage.get(serviceReq.ServiceId).entrySet()) {
                            for (Object o : entry.getValue()) {
                                JsonObject jo = (JsonObject) o;
                                String parentId = jo.getString(colName.ServicePackage.PARENT_ID, "");
                                if (!"".equalsIgnoreCase(parentId) && packageId.equalsIgnoreCase(jo.getString(colName.ServicePackage.PARENT_ID, ""))) {
                                    arrayPkReply.add(jo);
                                }
                            }
                        }
                        msg.reply(arrayPkReply);
                        break;

                    case Common.ServiceReq.COMMAND.GET_SERVER_ONOFF:
                        log.add("command type", "GET_SERVER_ONOFF");
                        log.add("server is available", serverOnOff.toString());
                        msg.reply(serverOnOff);
                        break;

                    case Common.ServiceReq.COMMAND.GET_MINMAXTRAN:
                        log.add("command type", "GET_MINMAXTRAN");
                        log.add("size of minmaxtran", listMinMaxTran.size());

                        msg.reply(listMinMaxTran);
                        break;
                    case Common.ServiceReq.COMMAND.GET_ALL_CARD_TYPE:
                        log.add("command type", "GET_ALL_CARD_TYPE");
                        log.add("size of card type", cardTypeArray);
                        long ltime = serviceReq.lastTime;
                        JsonArray result = new JsonArray();
                        if (cardTypeArray != null && cardTypeArray.size() > 0) {
                            for (CardTypeDb.Obj o : cardTypeArray) {
                                if (o.lastTime > ltime) {
                                    // lay duoc tat ca nhung record thay doi
                                    result.add(o.toJson());
                                }
                            }
                        }

                        msg.reply(result);
                        break;
                    case Common.ServiceReq.COMMAND.GET_FEE:
                        log.add("command type", "GET_FEE");
                        log.add("size of GET_FEE", listFee.size());
                        log.add("BankID", serviceReq.bankId);
                        log.add("TranType", serviceReq.tranType);
                        log.add("Channel", serviceReq.channel);
                        log.add("InOutCity", serviceReq.inoutCity);
                        log.add("PackageId - nguoi nhan", serviceReq.PackageId); // So dien thoai nguoi nhan khi get m2m
                        log.add("PackageType - nguoi gui", serviceReq.PackageType); // So dien thoai nguoi nhan khi get m2m
                        //cai nay chua dung toi
                        log.add("ServiceReq", serviceReq.toJSON());
                        log.add("Amount", serviceReq.amount);
                        log.add("List fee size", feeObjects.size());

                        log.add("Hash map fee size", hashMapFee.size());

                        logger.info("bankID trantype chanel inout amount listfee hash" + serviceReq.bankId + " "
                                + serviceReq.tranType + " " + serviceReq.channel + " " + serviceReq.inoutCity + " " + serviceReq.amount + " "
                                + feeObjects.size() + " " + hashMapFee.size());
//                        log.writeLog();
                        final String senderNumber = serviceReq.PackageType;
                        final String receiverNumber = serviceReq.PackageId;
                        JsonObject joFilter = new JsonObject().putString(colName.SpecialGroupCol.RECEIVER_NUMBER, receiverNumber)
                                .putString(colName.SpecialGroupCol.PHONE_NUMBER, senderNumber);
                        logger.info(joFilter.toString());
                        specialGroupDb.searchWithFilter(joFilter, new Handler<ArrayList<SpecialGroupDb.Obj>>() {
                            @Override
                            public void handle(ArrayList<SpecialGroupDb.Obj> specialObjList) {
                                logger.info("SIZE OF SPECIAL GROUP IS SENDER: " + senderNumber + " RECEIVER: " + receiverNumber + specialObjList.size());
                                if(specialObjList.size() == 0)
                                {
                                    String dummyId = getDummyId(serviceReq);
                                    int feeType = 0;
                                    if (feeObjects.size() > 0) {
                                        for (FeeObject feeObj_tmp : feeObjects) {
                                            if (feeObj_tmp.getId().equalsIgnoreCase(dummyId) && feeObj_tmp.getMaxAmount() >= serviceReq.amount
                                                    && feeObj_tmp.getMinAmount() <= serviceReq.amount) {
                                                feeType = feeObj_tmp.getFeeType();
                                            }
                                        }
                                    }
                                    String dummyId_2 = dummyId + feeType + "";

                                    FeeDb.Obj feeObj = hashMapFee.get(dummyId_2);

                                    //todo need to test before apply production
                                    if (feeObj == null) {
                                        for (int i = 0; i < listFee.size(); i++) {
                                            FeeDb.Obj tmpObj = listFee.get(i);

                                            //tra bieu phi
                                            if ("dgd".equalsIgnoreCase(serviceReq.bankId)) {
                                                if (serviceReq.bankId.equalsIgnoreCase(tmpObj.BANKID)
                                                        && serviceReq.tranType == tmpObj.TRANTYPE
                                                        && serviceReq.inoutCity == tmpObj.INOUT_CITY) {
                                                    feeObj = tmpObj;
                                                    break;
                                                }
                                                //tra biet phi cho bank
                                            } else if ("m2m".equalsIgnoreCase(serviceReq.bankId)) {
                                                if (serviceReq.bankId.equalsIgnoreCase(tmpObj.BANKID)
                                                        && serviceReq.tranType == tmpObj.TRANTYPE) {
                                                    feeObj = tmpObj;
                                                    break;
                                                }
                                            } else if (serviceReq.bankId.equalsIgnoreCase(tmpObj.BANKID)
                                                    && serviceReq.tranType == tmpObj.TRANTYPE
                                                    && serviceReq.channel == tmpObj.CHANNEL
                                                    && serviceReq.inoutCity == tmpObj.INOUT_CITY) {
                                                feeObj = tmpObj;
                                                break;
                                                //giao dich c2c
                                            } else if ("c2c".equalsIgnoreCase(serviceReq.bankId)) {
                                                if (serviceReq.bankId.equalsIgnoreCase(tmpObj.BANKID)
                                                        && serviceReq.tranType == tmpObj.TRANTYPE) {
                                                    feeObj = tmpObj;
                                                    break;
                                                }
                                            }

                                        }
                                    }

                                    if (feeObj == null) {
                                        log.add("there is no fee record matched", "set default");
                                        log.add("dynamic fee", 1.2);
                                        log.add("static fee", 1100);
                                        feeObj = new FeeDb.Obj();
                                        if ("m2m".equalsIgnoreCase(serviceReq.bankId)) {
                                            feeObj.DYNAMIC_FEE = 0;
                                            feeObj.STATIC_FEE = 1000;
                                        } else {
                                            feeObj.DYNAMIC_FEE = 1.2;
                                            feeObj.STATIC_FEE = 1100;
                                        }
                                    }

                                    msg.reply(feeObj.toJsonObject());
                                }
                                else {
                                    FeeDb.Obj feeObj = new FeeDb.Obj();
                                    Double doubleDiscount = Double.parseDouble(specialObjList.get(0).discount);
                                    if(doubleDiscount.doubleValue() > 100)
                                    {
                                        feeObj.DYNAMIC_FEE = 0;
                                        feeObj.STATIC_FEE = doubleDiscount.intValue();
                                    }
                                    else{
                                        feeObj.DYNAMIC_FEE = doubleDiscount.doubleValue();
                                        feeObj.STATIC_FEE = 0;
                                    }
                                    msg.reply(feeObj.toJsonObject());
                                }
                            }
                        });

                        break;

                    case Common.ServiceReq.COMMAND.GET_SERVICE_FEE:
                        log.add("command type", "GET_FEE");
//                        log.add("size of GET_FEE", listServiceFee.size());
                        log.add("Service id", serviceReq.ServiceId);
                        log.add("TranType", serviceReq.tranType);
                        log.add("Channel", serviceReq.channel);
                        log.add("InOutCity", serviceReq.inoutCity);

                        //cai nay chua dung toi

                        log.add("Amount", serviceReq.amount);
                        log.add("List service fee size", serviceFeeObjects.size());

                        log.add("Hash map service fee size", hashMapServiceFee.size());

                        logger.info("ServiceId trantype chanel inout amount listfee hash" + serviceReq.ServiceId + " "
                                + serviceReq.tranType + " " + serviceReq.channel + " " + serviceReq.inoutCity + " " + serviceReq.amount + " "
                                + serviceFeeObjects.size() + " " + hashMapServiceFee.size());
//                        log.writeLog();
                        String dummyServiceId = getDummyServiceId(serviceReq);
                        int serviceFeeType = 0;
                        if (serviceFeeObjects.size() > 0) {
                            for (FeeObject feeObj_tmp : serviceFeeObjects) {
                                if (feeObj_tmp.getId().equalsIgnoreCase(dummyServiceId) && feeObj_tmp.getMaxAmount() >= serviceReq.amount
                                        && feeObj_tmp.getMinAmount() <= serviceReq.amount) {
                                    serviceFeeType = feeObj_tmp.getFeeType();
                                }
                            }
                        }
                        String dummyServiceId_2 = dummyServiceId + serviceFeeType + "";

                        ServiceFeeDb.Obj serviceFeeObj = hashMapServiceFee.get(dummyServiceId_2);

                        //todo need to test before apply production
                        if (serviceFeeObj == null) {
                            log.add("there is no fee record matched", "set default");
                            log.add("dynamic fee", 0);
                            log.add("static fee", 0);
                            serviceFeeObj = new ServiceFeeDb.Obj();
                            serviceFeeObj.DYNAMIC_FEE = 0.0;
                            serviceFeeObj.STATIC_FEE = 0;
//                            if ("prudential".equalsIgnoreCase(serviceReq.ServiceId)) {
//                                serviceFeeObj.DYNAMIC_FEE = 0;
//                                serviceFeeObj.STATIC_FEE = 0;
//                            } else {
//                                serviceFeeObj.DYNAMIC_FEE = 0;
//                                serviceFeeObj.STATIC_FEE = 0;
//                            }
                        }

                        msg.reply(serviceFeeObj.toJsonObject());

                        break;

                    case Common.ServiceReq.COMMAND.GET_SERVICE_TYPE:
                        log.add("command type", "GET_SERVICE_TYPE");
                        //lay thong tin la dich vu(service) hay hoa don (invoice)
                        String serviceId = serviceReq.ServiceId;

                        JsonArray byServiceType = new JsonArray();

                        for (Object o : listService) {
                            JsonObject jo = (JsonObject) o;
                            boolean isUAT = jo.getBoolean(colName.ServiceCols.UAT, false);
                            String sid = jo.getString(colName.ServiceCols.SERVICE_ID, "");
                            //co du lieu
                            if (serviceId.equalsIgnoreCase(sid)) {

                                //server UAT
                                if (uatServer) {
                                    byServiceType.add(jo);
                                    break;
                                } else {
                                    if (isUAT) {
                                        jo.putNumber(colName.ServiceCols.STATUS, 0); // off dich vu
                                    }
                                    byServiceType.add(jo);
                                    break;
                                }
                            }
                        }

                        msg.reply(byServiceType);
                        break;

                    case Common.ServiceReq.COMMAND.GET_SERVICE_BY_LAST_TIME:
                        //todo
                        long lastTime = serviceReq.lastTime;
                        log.add("command type", "GET_SERVICE_BY_LAST_TIME");
                        log.add("service list size", listService.size());
                        log.add("request last time", lastTime);

                        JsonArray arrayServiceLastTime = new JsonArray();
                        for (Object o : listService) {
                            JsonObject item = (JsonObject) o;
                            boolean isUAT = item.getBoolean(colName.ServiceCols.UAT, false);
                            long itemLastTime = item.getLong(colName.ServiceCols.LAST_UPDATE, 0);

                            if (itemLastTime > lastTime) {
                                //server UAT
                                if (uatServer) {
                                    arrayServiceLastTime.add(item);
                                } else {
                                    //dich vu dang cau hinh chay tren production
                                    if (isUAT) {
                                        item.putNumber(colName.ServiceCols.STATUS, 0); // off dich vu
                                    }
                                    arrayServiceLastTime.add(item);
                                }
                            }
                        }

                        log.add("service list size reply before sort", arrayServiceLastTime.size());
//                        arrayServiceLastTime = Misc.sortJsonByTime(arrayServiceLastTime, colName.ServiceCols.LAST_UPDATE);
                        msg.reply(arrayServiceLastTime);
                        break;

                    case Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID:

                        String srvId = serviceReq.ServiceId;

                        log.add("command type", "GET_SERVICE_BY_SERVICE_ID");
                        log.add("service list size", listService.size());
                        log.add("service id", srvId);

                        JsonArray arrayServices = new JsonArray();
                        for (Object o : listService) {
                            JsonObject item = (JsonObject) o;
                            boolean isUAT = item.getBoolean(colName.ServiceCols.UAT, false);
                            String sid = item.getString(colName.ServiceCols.SERVICE_ID, "");

                            if (srvId.equalsIgnoreCase(sid)) {

                                //server UAT
                                if (uatServer) {
                                    arrayServices.add(item);
                                    break;
                                } else {
                                    if (isUAT) {
                                        item.putNumber(colName.ServiceCols.STATUS, 0);//of dich vu
                                    }
                                    arrayServices.add(item);
                                    break;
                                }
                            }
                        }
                        msg.reply(arrayServices);
                        break;

                    //for escape halloween
                    case Common.ServiceReq.COMMAND.GET_PACKAGE_BY_AMOUNT:
                        long amount = serviceReq.amount;

                        log.add("command type", "GET_PACKAGE_BY_AMOUNT");
                        log.add("service list size", listServicePackage.size());
                        log.add("amount", amount);

                        HashMap<String, JsonArray> escapeHM = listServicePackage.get("escape");

                        Collection<JsonArray> arrays = escapeHM.values();

                        JsonObject joRpl = new JsonObject();

                        for (JsonArray jsonArray : arrays) {

                            for (int i = 0; i < jsonArray.size(); i++) {
                                JsonObject jo = jsonArray.get(i);
                                if (DataUtil.stringToUNumber(jo.getString(colName.ServicePackage.PACKAGE_VALUE, "0")) == amount) {
                                    joRpl.putString("pname", jo.getString(colName.ServicePackage.PACKAGE_NAME, ""));
                                    break;
                                }
                            }
                        }

                        msg.reply(joRpl);
                        break;

                    //CDHH
                    case Common.ServiceReq.COMMAND.GET_CDHH_ON_OFF:
                        msg.reply(cdhhOnOff);
                        break;

                    case Common.ServiceReq.COMMAND.GET_PAYBACK_EVENT:

                        CDHHPayBackSetting.Obj oPayBack = new CDHHPayBackSetting.Obj();
                        for (int i = 0; i < paybackSetting.size(); i++) {
                            CDHHPayBackSetting.Obj tO = paybackSetting.get(i);
                            if (serviceReq.ServiceId.equalsIgnoreCase(tO.serviceid)) {
                                oPayBack = tO;
                                break;
                            }
                        }
                        msg.reply(oPayBack.toJson());
                        break;

                    case Common.ServiceReq.COMMAND.GET_CDHH_CONFIG_WEEK_OR_AQUATER_ACTIVE:

                        JsonObject jorepl = new JsonObject();

                        for (int i = 0; i < arrayCHDDWeekOrAquarterConfig.size(); i++) {
                            CdhhConfig o = arrayCHDDWeekOrAquarterConfig.get(i);

                            if (o.active && serviceReq.ServiceId.equalsIgnoreCase(o.serviceId)) {
                                jorepl = o.toJsonObject();
                                break;
                            }
                        }
                        msg.reply(jorepl);
                        break;

                    //service category
                    case Common.ServiceReq.COMMAND.GET_SERVICE_CATEGORY_BY_LAST_TIME:
                        logger.info("GET_SERVICE_CATEGORY_BY_LAST_TIME");
                        JsonArray jsonArray = new JsonArray();
                        long lTime = serviceReq.lastTime;
                        for (String s : hashMapServiceCateogry.keySet()) {
                            ServiceCategory.Obj o = hashMapServiceCateogry.get(s);
                            if (o != null && o.lasttime > lTime) {
                                jsonArray.add(o.toJson());
                            }
                        }
                        logger.info("GET_SERVICE_CATEGORY_BY_LAST_TIME " + jsonArray.size() + " items");
                        msg.reply(jsonArray);
                        break;

                    case Common.ServiceReq.COMMAND.GET_IRON_MAN_PROMO_TRACKING_TABLE:
                        if(ironManTrackingTable != null && ironManTrackingTable.size() > 0)
                        {
                            msg.reply(ironManTrackingTable);
                        }
                        else
                        {
                            loadIronManConfig(new Handler<JsonArray>() {
                                @Override
                                public void handle(JsonArray objects) {
                                    msg.reply(objects);
                                }
                            });
                        }
                        break;

                    case Common.ServiceReq.COMMAND.GET_RANDOM_GIFT:
                        if(ironRandomGiftJsonArray != null && ironRandomGiftJsonArray.size() > 0)
                        {
                            msg.reply(ironRandomGiftJsonArray);
                        }
                        else
                        {
                            loadRandomIronPromoGifts(new Handler<JsonArray>() {
                                @Override
                                public void handle(JsonArray objects) {
                                    msg.reply(objects);
                                }
                            });
                        }
                        break;
                    case Common.ServiceReq.COMMAND.UPDATE_IRON_MAN_USER:
                        JsonObject jsonData = new JsonObject(serviceReq.PackageId.toString());
                        updateIronManUser(jsonData);
                        break;
                    //Them de load service tu shopping verticle
                    case Common.ServiceReq.COMMAND.GET_SHOPPING_VERTICLE_BY_SERVICE_ID:
                        //Lay thong tin server verticle tu file
                        JsonObject jsonCungmuaVerticle = new JsonObject();
                        if(jsonShoppingVertilce.containsField(serviceReq.ServiceId))
                        {
                            jsonCungmuaVerticle = jsonShoppingVertilce.getObject(serviceReq.ServiceId, new JsonObject());
                            jsonCungmuaVerticle.putBoolean("viacore", true);
                        }
                        else {
                            jsonCungmuaVerticle.putBoolean("viacore", false);
                        }
                        ViaConnectorObj viaConnectorObj = new ViaConnectorObj(jsonCungmuaVerticle);
                        msg.reply(viaConnectorObj.toJson());
                        break;
                    case Common.ServiceReq.COMMAND.GET_CONNECTOR_SERVICE_BUS_NAME:
                        JsonArray serviceArray = getConnectorServiceBusName();
                        msg.reply(serviceArray);
                        break;
                    case Common.ServiceReq.COMMAND.RELOAD_CONNECTOR_SERVICE_BUS_NAME:
                        reloadConnectorServiceBusName(msg);
                        break;
                    case Common.ServiceReq.COMMAND.GET_SERVICE_GIFT_RULES:
                        JsonArray serviceGiftRuleArr = getServiceGiftRules();
                        msg.reply(serviceGiftRuleArr);
                        break;
                    case Common.ServiceReq.COMMAND.RELOAD_CONNECTOR_SERVICE_GIFT_RULES:
                        reloadServiceGiftRules(msg);
                        break;
                    default:
                        log.add("ServicePartnerVerticle not support for the command " + serviceReq.Command, "");
                        break;
                }

                log.writeLog();
            }
        };

        Handler<Message<JsonObject>> updateServiceHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> msg) {
                Common.ServiceReq serviceReq = new Common.ServiceReq(msg.body());

                Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("ServicePartnerVerticle");
                log.add("server prefix", AppConstant.PREFIX);
                log.add("time", Misc.dateVNFormatWithTime(System.currentTimeMillis()));

                switch (serviceReq.Command) {
                    case Common.ServiceReq.COMMAND.UPDATE_SERVICE:

                        log.add("function", "update Service");
                        log.add("command type", "UPDATE_SERVICE");
//                        if(isStoreApp)
//                        {
//                            loadStoreService();
//                        }
//                        else{
//                            loadService();
//                        }
                        loadService();
                        break;
                    case Common.ServiceReq.COMMAND.UPDATE_SERVICE_DETAIL_BY_SERVICE_ID:
                        log.add("function", "update ServiceDetail");
                        log.add("command type", "UPDATE_SERVICE_DETAIL_BY_SERVICE_ID");
                        loadServiceDetail();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_PACKAGE:
                        log.add("function", "update ServicePackage");
                        log.add("command type", "UPDATE_PACKAGE");
                        loadServicePackage();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_SERVER_ONOFF:
                        log.add("command type", "UPDATE_SERVER_ONOFF");
                        log.add("function", "update OnOffServer");
                        getOnOffServer();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_MINMAXTRAN:
                        log.add("command type", "UPDATE_MINMAXTRAN");
                        log.add("function", "update minmaxtran");
                        loadMinMaxTran();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_FEE:
                        log.add("command type", "UPDATE_FEE");
                        log.add("function", "update fee");
                        feeObjects = new ArrayList<>();
                        log.add("function update fee", "feeObjects " + feeObjects.size());
                        loadFee();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_SERVICE_FEE:
                        log.add("command type", "UPDATE_FEE");
                        log.add("function", "update fee");
                        loadServiceFee();
                        break;


                    //for cdhh
                    case Common.ServiceReq.COMMAND.UPDATE_CDHH:
                        log.add("command type", "UPDATE_CDHH");
                        log.add("function", "update cdhh");
                        loadCDHH();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_CDHH_PAYBACK:
                        log.add("command type", "UPDATE_CDHH_PAYBACK");
                        log.add("function", "update cdhh payback");
                        loadCDHHPayBack();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_CDHH_CONFIG_WEEK_OR_AQUATER:
                        log.add("command type", "UPDATE_CDHH_CONFIG_WEEK_OR_AQUATER");
                        log.add("function", "update UPDATE_CDHH_CONFIG_WEEK_OR_AQUATER");
                        loadCDHHWeekOrAquarter();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_SERVICE_CATEGORY:
                        log.add("command type", "UPDATE_SERVICE_CATEGORY");
                        log.add("function", "update UPDATE_SERVICE_CATEGORY");
                        loadServiceCategory();
                        break;

                    case Common.ServiceReq.COMMAND.UPDATE_SERVICE_FORM:
                        log.add("command type", "UPDATE_SERVICE_FORM");
                        log.add("function", "update UPDATE_SERVICE_FORM");
                        loadServiceForm();
                        break;
                    case Common.ServiceReq.COMMAND.UPDATE_CARD_TYPE:
                        log.add("command type", "UPDATE_CARD_TYPE");
                        log.add("function", "update UPDATE_CARD_TYPE");
                        loadAllCardType();
                        break;

                    case Common.ServiceReq.COMMAND.FORCE_LOAD_ALL_CONFIG:
                        log.add("command type", "FORCE_LOAD_ALL_CONFIG");
                        log.add("function", "update FORCE_LOAD_ALL_CONFIG");
                        loadAllConfig();
                        break;
                    case Common.ServiceReq.COMMAND.REFRESH_CONFIG_DATA:
                        log.add("command type", "REFRESH_CONFIG_DATA");
                        log.add("function", "update REFRESH_CONFIG_DATA");
                        loadCDHHWeekOrAquarterAndReply(msg);
                        break;
                    case Common.ServiceReq.COMMAND.REFRESH_IRON_MAN_TRACKING_TABLE:
                        loadIronManConfig(new Handler<JsonArray>() {
                            @Override
                            public void handle(JsonArray objects) {
                                logger.info("DU lieu IRON MAN da duoc refrest lai " + objects);
                                msg.reply(new JsonObject().putNumber("error", 0));
                            }
                        });
                        break;
                    case Common.ServiceReq.COMMAND.REFRESH_RANDOM_GIFT:
                        loadRandomIronPromoGifts(new Handler<JsonArray>() {
                            @Override
                            public void handle(JsonArray jsonArray) {
                                logger.info("DU lieu Gift random da duoc refrest lai " + jsonArray);
                                msg.reply(new JsonObject().putNumber("error", 0));
                            }
                        });
                        break;
                    case Common.ServiceReq.COMMAND.UPDATE_CONNECTOR_SERVICE_BUS_NAME:
                        loadConnectorServiceBusName();
                        break;
                    case Common.ServiceReq.COMMAND.UPDATE_SERVICE_GIFT_RULES:
                        loadServiceGiftRule();
                        break;
                    default:
                        log.add("ServicePartnerVerticle not support for the command " + serviceReq.Command, "");
                        break;
                }

                msg.reply(msg.body());
                log.writeLog();
            }
        };

        eventBus.registerLocalHandler(AppConstant.ConfigVerticleService, getServiceHandler);

        eventBus.registerHandler(AppConstant.ConfigVerticleService_Update, updateServiceHandler);


        Handler<Message<JsonObject>> prefixupdateServiceHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> msg) {
                Common.ServiceReq serviceReq = new Common.ServiceReq(msg.body());

                Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("ServicePartnerVerticle");
                log.add("server prefix", AppConstant.PREFIX);
                log.add("time", Misc.dateVNFormatWithTime(System.currentTimeMillis()));

                switch (serviceReq.Command) {
                    case Common.ServiceReq.COMMAND.REFRESH_IRON_MAN_TRACKING_TABLE:
                        loadIronManConfig(new Handler<JsonArray>() {
                            @Override
                            public void handle(JsonArray objects) {
                                logger.info("DU lieu IRON MAN da duoc refrest lai " + objects);
                                msg.reply(new JsonObject().putNumber("error", 0));
                            }
                        });
                        break;
                    case Common.ServiceReq.COMMAND.REFRESH_RANDOM_GIFT:
                        loadRandomIronPromoGifts(new Handler<JsonArray>() {
                            @Override
                            public void handle(JsonArray jsonArray) {
                                logger.info("DU lieu Gift random da duoc refrest lai " + jsonArray);
                                msg.reply(new JsonObject().putNumber("error", 0));
                            }
                        });
                        break;
                    default:
                        log.add("ServicePartnerVerticle not support for the command " + serviceReq.Command, "");
                        break;
                }
            }
        };
        eventBus.registerHandler(AppConstant.ConfigVerticleService_PrefixUpdate, prefixupdateServiceHandler);
    }

    private void loadServiceForm() {

        serviceForm.getAll(new Handler<ArrayList<ServiceForm.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceForm.Obj> objs) {
                if (objs != null && objs.size() > 0) {
                    JsonArray tmp = new JsonArray();
                    arrForm = new JsonArray();
                    for (int i = 0; i < objs.size(); i++) {
                        tmp.add(objs.get(i).toJson());
                    }
                    arrForm = tmp;
                }
            }
        });
    }

    private void loadServiceCategory() {
        logger.info("loadServiceCategory");
        serviceCategory.getAll(new Handler<ArrayList<ServiceCategory.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceCategory.Obj> objs) {
                if (objs != null && objs.size() > 0) {
                    hashMapServiceCateogry.clear();
                    for (int i = 0; i < objs.size(); i++) {
                        hashMapServiceCateogry.put(objs.get(i).id, objs.get(i));
                    }
                }
            }
        });
        logger.info("loadServiceCategory " + hashMapServiceCateogry.size());
    }

    private void updateIronManUser(JsonObject jsonUpdate)
    {
        logger.info("updateIronManUser   ----->>>>>>>> " + jsonUpdate);
        int program_number = jsonUpdate.getInteger(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, 0);
        int program_number_tracking = 0;
        JsonArray jsonArray_tmp = new JsonArray();
        if(ironManTrackingTable != null && ironManTrackingTable.size() > 0 && program_number != 0)
        {
            for(Object obj : ironManTrackingTable)
            {
                program_number_tracking = ((JsonObject)obj).getInteger(colName.IronManBonusTrackingTable.PROGRAM_NUMBER);
                if(program_number == program_number_tracking)
                {
                    jsonArray_tmp.addObject(jsonUpdate);
                }
                else{
                    jsonArray_tmp.addObject(((JsonObject)obj));
                }

            }

            logger.info("ironManTrackingTable before   ----->>>>>>>> " + ironManTrackingTable);
            ironManTrackingTable = jsonArray_tmp;
            logger.info("ironManTrackingTable after   ----->>>>>>>> " + ironManTrackingTable);
            JsonObject joUpdate = jsonUpdate;
            joUpdate.removeField(colName.IronManBonusTrackingTable.PROGRAM_NUMBER);
            ironManBonusTrackingTableDb.updatePartial(program_number, jsonUpdate, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                }
            });
        }

    }

    private void loadCDHHWeekOrAquarter() {
        cdhhConfigDb.find(new CdhhConfig(), 100, new Handler<List<CdhhConfig>>() {
            @Override
            public void handle(List<CdhhConfig> cdhhConfigs) {
                if (cdhhConfigs != null) {
                    List<CdhhConfig> tmp = arrayCHDDWeekOrAquarterConfig;
                    arrayCHDDWeekOrAquarterConfig = cdhhConfigs;
                    tmp.clear();
                }
            }
        });
    }

    private void loadCDHHWeekOrAquarterAndReply(final Message<JsonObject> msg) {
        cdhhConfigDb.find(new CdhhConfig(), 100, new Handler<List<CdhhConfig>>() {
            @Override
            public void handle(List<CdhhConfig> cdhhConfigs) {
                if (cdhhConfigs != null) {
                    List<CdhhConfig> tmp = arrayCHDDWeekOrAquarterConfig;
                    arrayCHDDWeekOrAquarterConfig = cdhhConfigs;
                    tmp.clear();

                    JsonObject jsonReply = new JsonObject();
                    JsonArray arrayCDHH = new JsonArray();
                    for (CdhhConfig a : arrayCHDDWeekOrAquarterConfig) {
                        arrayCDHH.add(a.toJsonObject());
                    }
                    jsonReply.putArray("cdhh", arrayCDHH);
                    msg.reply(jsonReply);

                }
            }
        });
    }

    private void loadCDHH() {
        settingsDb.getLong("CDHH", new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                if (aLong == 0) {
                    cdhhOnOff = false;
                } else {
                    cdhhOnOff = true;
                }
            }
        });
    }

    private void loadCDHHPayBack() {
        cdhhPayBackSetting.findAll(new Handler<ArrayList<CDHHPayBackSetting.Obj>>() {
            @Override
            public void handle(ArrayList<CDHHPayBackSetting.Obj> objs) {

                if (objs != null && objs.size() > 0) {
                    paybackSetting.clear();
                    paybackSetting = objs;
                }
            }
        });
    }

    private void loadFee() {
        listFee = new ArrayList<>();
        feeDb.getAll(new Handler<ArrayList<FeeDb.Obj>>() {
            @Override
            public void handle(ArrayList<FeeDb.Obj> objs) {
                if (objs != null) {
                    logger.info("CHAY FEEDB," + objs.size());
                    listFee = objs;
                    getFeeHashMap(objs);
                    getListMapFee(objs);
                }
            }
        });
    }

    private void loadServiceFee() {
        listFee = new ArrayList<>();
        serviceFeeDb.getAll(new Handler<ArrayList<ServiceFeeDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceFeeDb.Obj> objs) {
                if (objs != null) {
                    logger.info("CHAY FEEDB," + objs.size());
//                    listServiceFee = objs;
                    getServiceFeeHashMap(objs);
                    getListMapServiceFee(objs);
                }
            }
        });
    }

    public void getFeeHashMap(ArrayList<FeeDb.Obj> objs) {
        if (objs == null || objs.size() == 0) {
            return;
        }
        for (FeeDb.Obj o : objs) {
            String dummyId = o.BANKID
                    + o.TRANTYPE
                    + o.CHANNEL
                    + o.INOUT_CITY
                    + o.FEE_TYPE;

            hashMapFee.put(dummyId, o);
        }
    }

    public void getListMapFee(ArrayList<FeeDb.Obj> objs) {
        if (objs == null || objs.size() == 0) {
            return;
        }
        FeeObject feeObject = null;
        for (FeeDb.Obj o : objs) {
            String dummyId = o.BANKID
                    + o.TRANTYPE
                    + o.CHANNEL
                    + o.INOUT_CITY;

            feeObject = new FeeObject();
            feeObject.setId(dummyId);
            feeObject.setMinAmount(o.MIN_VALUE);
            feeObject.setMaxAmount(o.MAX_VALUE);
            feeObject.setFeeType(o.FEE_TYPE);
            feeObjects.add(feeObject);
        }
    }

    public void getServiceFeeHashMap(ArrayList<ServiceFeeDb.Obj> objs) {
        if (objs == null || objs.size() == 0) {
            return;
        }

        for (ServiceFeeDb.Obj o : objs) {
            String dummyId = o.SERVICE_ID
                    + o.TRANTYPE
                    + o.CHANNEL
                    + o.INOUT_CITY
                    + o.FEE_TYPE;

            hashMapServiceFee.put(dummyId, o);
        }
    }

    public void getListMapServiceFee(ArrayList<ServiceFeeDb.Obj> objs) {
        if (objs == null || objs.size() == 0) {
            return;
        }
        FeeObject feeObject = null;
        for (ServiceFeeDb.Obj o : objs) {
            String dummyId = o.SERVICE_ID
                    + o.TRANTYPE
                    + o.CHANNEL
                    + o.INOUT_CITY;

            feeObject = new FeeObject();
            feeObject.setId(dummyId);
            feeObject.setMinAmount(o.MIN_VALUE);
            feeObject.setMaxAmount(o.MAX_VALUE);
            feeObject.setFeeType(o.FEE_TYPE);

            serviceFeeObjects.add(feeObject);
        }
    }


    private void loadAllCardType() {

        String partnerCode = "";
        final String cardType = "";
        long ltime = 0;
        Boolean enable = null;
        cardTypeDb.find(partnerCode, cardType, enable, ltime, new Handler<ArrayList<CardTypeDb.Obj>>() {
            @Override
            public void handle(ArrayList<CardTypeDb.Obj> objs) {
                synchronized (lckCardType) {
                    ArrayList<CardTypeDb.Obj> tmp = cardTypeArray;
                    cardTypeArray = objs;
                    tmp.clear();
                }
            }
        });
    }

    public void getCardType(ArrayList<CardTypeDb.Obj> objs) {
        if (objs == null || objs.size() == 0) {
            return;
        }

        for (CardTypeDb.Obj o : objs) {
            String dummyId = o.id;

        }
    }


    private void loadMinMaxTran() {
        minMaxTranDb.getlist(null, null, new Handler<ArrayList<MinMaxTranDb.Obj>>() {
            @Override
            public void handle(ArrayList<MinMaxTranDb.Obj> objs) {
                logger.info("CHAY MINMAXDB," + objs.size());
                listMinMaxTran = new JsonArray();
                if (objs != null) {
                    for (int i = 0; i < objs.size(); i++) {
                        listMinMaxTran.add(objs.get(i).toJsonObject());
                    }
                }
            }
        });
    }

    private void getOnOffServer() {
        serverOnOff = new JsonObject();

        wholeSystemPauseDb.getOne(new Handler<WholeSystemPauseDb.Obj>() {
            @Override
            public void handle(WholeSystemPauseDb.Obj obj) {
                if (obj == null || obj.ACTIVED == false) {
                    serverOnOff.putBoolean("ispaused", false);
                    serverOnOff.putString("caption", "");
                    serverOnOff.putString("body", "");
                } else {
                    serverOnOff.putBoolean("ispaused", true);
                    serverOnOff.putString("caption", obj.CAPTION);
                    serverOnOff.putString("body", obj.BODY);
                }
            }
        });
    }

    private void loadService() {
        listService = new JsonArray();
        boolean isStoreBackend = container.config().getBoolean("storeApp", false);
        serviceDb.getlist(isStoreBackend, "", "", new Handler<ArrayList<ServiceDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDb.Obj> objs) {
                logger.info("CHAY SERVICEDB," + objs.size());

                if (objs != null) {
                    for (int i = 0; i < objs.size(); i++) {
                        JsonObject serviceJson = objs.get(i).toJsonObject();
                        if(isAndroid && !isStoreApp)
                        {
                            int statusAndroid = serviceJson.getInteger(colName.ServiceCols.STATUS_ANDROID, 0);
                            serviceJson.putNumber(colName.ServiceCols.STATUS, statusAndroid);
                            listService.add(serviceJson);
                        }
                        else {
                            listService.add(serviceJson);
                        }
                        if ("event".equalsIgnoreCase(serviceJson.getString(colName.ServiceCols.CAT_ID))) {
                            listServiceEvent.add(serviceJson);
                        }
                    }
                    logger.info("listServiceEvent" + listServiceEvent);
                }
            }
        });
    }

//    private void loadStoreService() {
//        listService = new JsonArray();
//        serviceDb.getlist("", "", new Handler<ArrayList<ServiceDb.Obj>>() {
//            @Override
//            public void handle(ArrayList<ServiceDb.Obj> objs) {
//                logger.info("CHAY SERVICEDB," + objs.size());
//
//                if (objs != null) {
//                    for (int i = 0; i < objs.size(); i++) {
//                        JsonObject serviceJson = objs.get(i).toJsonObject();
//                        listService.add(serviceJson);
//                        if ("event".equalsIgnoreCase(serviceJson.getString(colName.ServiceCols.CAT_ID))) {
//                            listServiceEvent.add(serviceJson);
//                        }
//                    }
//                    logger.info("listServiceEvent" + listServiceEvent);
//                }
//            }
//        });
//    }

    private void loadServiceDetail() {
        listServiceDetail = new HashMap<>();
        serviceDetailDb.getAll("", new Handler<ArrayList<ServiceDetailDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDetailDb.Obj> objs) {
                JsonArray listDetail;
                logger.info("CHAY SERVICEDETAILDB," + objs.size());
                if (objs != null) {
                    for (int i = 0; i < objs.size(); i++) {
                        listDetail = null;

                        if (listServiceDetail.containsKey(objs.get(i).serviceId)) {
                            listDetail = listServiceDetail.get(objs.get(i).serviceId);
                        }

                        if (listDetail == null) {
                            listDetail = new JsonArray();
                        }

                        listDetail.add(objs.get(i).toJsonObject());

                        listServiceDetail.put(objs.get(i).serviceId, listDetail);
                    }
                }
            }
        });
    }

    private void loadServicePackage() {
        listServicePackage = new HashMap<>();
        servicePackageDb.getlist("", "", "", "", new Handler<ArrayList<ServicePackageDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServicePackageDb.Obj> objs) {
                HashMap<String, JsonArray> mapPackage;
                JsonArray servicePackage;
                logger.info("CHAY SERVICEPACKAGEDB," + objs.size());
                if (objs != null) {
                    for (int i = 0; i < objs.size(); i++) {
                        mapPackage = null;

                        if (listServicePackage.containsKey(objs.get(i).serviceID)) {
                            mapPackage = listServicePackage.get(objs.get(i).serviceID);
                        }

                        if (mapPackage == null) {
                            mapPackage = new HashMap<>();
                        }

                        servicePackage = null;

                        if (mapPackage.containsKey(objs.get(i).packageType)) {
                            servicePackage = mapPackage.get(objs.get(i).packageType);
                        }

                        if (servicePackage == null) {
                            servicePackage = new JsonArray();
                        }

                        servicePackage.add(objs.get(i).toJsonObject());
                        mapPackage.put(objs.get(i).packageType, servicePackage);
                        listServicePackage.put(objs.get(i).serviceID, mapPackage);
                    }
                }
            }
        });
    }

    private String getDummyId(Common.ServiceReq serviceReq) {
        return serviceReq.bankId
                + serviceReq.tranType
                + serviceReq.channel
                + serviceReq.inoutCity;
    }

    private String getDummyServiceId(Common.ServiceReq serviceReq) {
        return serviceReq.ServiceId
                + serviceReq.tranType
                + serviceReq.channel
                + serviceReq.inoutCity;
    }

    private void loadAllConfig() {
//        if(isStoreApp)
//        {
//            loadStoreService();
//        }
//        else
//        {
//            loadService();
//        }
        loadService();
        loadServiceDetail();
        loadServicePackage();
        getOnOffServer();
        loadMinMaxTran();
        loadFee();
        loadServiceFee();
        loadServiceCategory();
        loadAllCardType();
        //
        loadCDHH();
        loadCDHHPayBack();
        loadCDHHWeekOrAquarter();
        loadShoppingVerticle();
        loadConnectorServiceBusName();
        loadServiceGiftRule();
        //BEGIN 0000000052 IRON MAN
//        loadIronManConfig(new Handler<JsonArray>() {
//            @Override
//            public void handle(JsonArray objects) {
//                logger.info("Chuong trinh khuyen mai IRON MAN day ne " + objects);
//            }
//        });
        //END 0000000052 IRON MAN


    }
    public void loadConnectorServiceBusName()
    {
        connectorProArrayList.clear();
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("loadConnectorServiceBusName");
        connectorProxyBusNameDb.searchWithFilter(new JsonObject(), new Handler<ArrayList<ConnectorProxyBusNameDb.Obj>>() {
            @Override
            public void handle(ArrayList<ConnectorProxyBusNameDb.Obj> listConnectorServiceBusNames) {
                for(int i = 0; i < listConnectorServiceBusNames.size(); i++)
                {
                    connectorProArrayList.add(listConnectorServiceBusNames.get(i));
                }
            }
        });
    }
    public void loadShoppingVerticle()
    {
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("loadShoppingVerticle");
        JsonObject jsonLoadShoppingVerticle = new JsonObject();
        jsonLoadShoppingVerticle = Misc.readJsonObjectFile("shoppingverticle.json");
        jsonShoppingVertilce = jsonLoadShoppingVerticle.getObject("connector_proxy_shopping_busname", new JsonObject());
        log.add("jsonShoppingVerticle", jsonShoppingVertilce);
    }

    public void loadServiceGiftRule()
    {
        serviceGiftRuleArrList.clear();
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("loadServiceGiftRule");
        serviceGiftRuleDb.searchWithFilter(new JsonObject(), new Handler<ArrayList<CheckServiceGiftRuleDb.Obj>>() {
            @Override
            public void handle(ArrayList<CheckServiceGiftRuleDb.Obj> serviceGiftRules) {
                for (int i = 0; i < serviceGiftRules.size(); i++) {
                    serviceGiftRuleArrList.add(serviceGiftRules.get(i));
                }
            }
        });
    }

    //BEGIN 0000000052 IRON MAN

    private void loadIronManConfig(final Handler<JsonArray> jsonArrayHandler)
    {
        ironManBonusTrackingTableDb = new IronManBonusTrackingTableDb(vertx, logger);
        ironManTrackingTable = new JsonArray();
        JsonObject jsonFilter = new JsonObject();
        jsonFilter.putString(colName.IronManBonusTrackingTable.PROGRAM, StringConstUtil.IronManPromo.IRON_PROMO);
        ironManBonusTrackingTableDb.searchWithFilter(jsonFilter, new Handler<ArrayList<IronManBonusTrackingTableDb.Obj>>() {
            @Override
            public void handle(ArrayList<IronManBonusTrackingTableDb.Obj> objs) {
                JsonObject jsonTrackingObj;
                for (IronManBonusTrackingTableDb.Obj ironBonusTrackingTableObj : objs) {

                    jsonTrackingObj = new JsonObject();

                    jsonTrackingObj.putString(colName.IronManBonusTrackingTable.PROGRAM, ironBonusTrackingTableObj.program.trim());
                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, ironBonusTrackingTableObj.program_number);

                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.NUMERATOR, ironBonusTrackingTableObj.numerator);
                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.DENOMINATOR, ironBonusTrackingTableObj.denominator);

                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.MIN_RATIO, ironBonusTrackingTableObj.min_ratio);
                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.MAX_RATIO, ironBonusTrackingTableObj.max_ratio);

                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.START_TIME, ironBonusTrackingTableObj.start_time);
                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.END_TIME, ironBonusTrackingTableObj.end_time);

                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, ironBonusTrackingTableObj.number_of_bonus_man);
                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, ironBonusTrackingTableObj.number_of_new_comer);
                    jsonTrackingObj.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_GAVE_MAN, ironBonusTrackingTableObj.number_of_bonus_gave_man);

                    jsonTrackingObj.putBoolean(colName.IronManBonusTrackingTable.NOT_RATIO_FLAG, ironBonusTrackingTableObj.not_ratio_flag);

                    ironManTrackingTable.add(jsonTrackingObj);
                }
                jsonArrayHandler.handle(ironManTrackingTable);
            }
        });

    }

    private void loadRandomIronPromoGifts(final Handler<JsonArray> jsonArrayHandler)
    {
        ironManRandomGiftManageDb = new IronManRandomGiftManageDb(vertx, logger);
        ironRandomGiftJsonArray = new JsonArray();
        JsonObject jsonFilter = new JsonObject();
//        jsonFilter.putString(colName.IronManBonusTrackingTable.PROGRAM, StringConstUtil.IronManPromo.IRON_PROMO);
        ironManRandomGiftManageDb.searchWithFilter(jsonFilter, new Handler<ArrayList<IronManRandomGiftManageDb.Obj>>() {
            @Override
            public void handle(ArrayList<IronManRandomGiftManageDb.Obj> objs) {
                JsonObject jsonTrackingObj;
                for (IronManRandomGiftManageDb.Obj ironRandomGiftObj : objs) {

                    jsonTrackingObj = new JsonObject();
                    jsonTrackingObj.putString(colName.IronManRandomGiftManage.GROUP, ironRandomGiftObj.group);
                    jsonTrackingObj.putString(colName.IronManRandomGiftManage.FIXED_GIFT, ironRandomGiftObj.fixed_gifts);
                    jsonTrackingObj.putString(colName.IronManRandomGiftManage.RANDOM_GIFT, ironRandomGiftObj.random_gifts);
                    jsonTrackingObj.putNumber(colName.IronManRandomGiftManage.NUMBER_OF_GIFT, ironRandomGiftObj.number_of_gift);

                    ironRandomGiftJsonArray.add(jsonTrackingObj);
                }
                jsonArrayHandler.handle(ironRandomGiftJsonArray);
            }
        });

    }
    //END 0000000052 IRON MAN

    //GET CONNECTOR SERVICE BUS NAME
    public JsonArray getConnectorServiceBusName()
    {
        JsonArray jarrReply = new JsonArray();
        for (int i = 0; i < connectorProArrayList.size(); i++) {
            jarrReply.add(connectorProArrayList.get(i).toJson());
        }
        return jarrReply;
    }


    public void reloadConnectorServiceBusName(final Message msg)
    {
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("loadConnectorServiceBusName");
        connectorProArrayList.clear();
        connectorProxyBusNameDb.searchWithFilter(new JsonObject(), new Handler<ArrayList<ConnectorProxyBusNameDb.Obj>>() {
            @Override
            public void handle(ArrayList<ConnectorProxyBusNameDb.Obj> listConnectorServiceBusNames) {
                for(int i = 0; i < listConnectorServiceBusNames.size(); i++)
                {
                    connectorProArrayList.add(listConnectorServiceBusNames.get(i));
                }
                msg.reply(true);
            }
        });
    }

    public JsonArray getServiceGiftRules()
    {
        JsonArray jarrReply = new JsonArray();
        for (int i = 0; i < serviceGiftRuleArrList.size(); i++) {
            jarrReply.add(serviceGiftRuleArrList.get(i).toJson());
        }
        return jarrReply;
    }

    public void reloadServiceGiftRules(final Message msg)
    {
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("reloadServiceGiftRules");
        serviceGiftRuleArrList.clear();
        serviceGiftRuleDb.searchWithFilter(new JsonObject(), new Handler<ArrayList<CheckServiceGiftRuleDb.Obj>>() {
            @Override
            public void handle(ArrayList<CheckServiceGiftRuleDb.Obj> serviObjArrayList) {
                for(int i = 0; i < serviObjArrayList.size(); i++)
                {
                    serviceGiftRuleArrList.add(serviObjArrayList.get(i));
                }
                msg.reply(true);
            }
        });
    }

}
