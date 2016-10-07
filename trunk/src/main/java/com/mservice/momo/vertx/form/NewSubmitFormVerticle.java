package com.mservice.momo.vertx.form;

import com.mservice.momo.clientform.FormInfo;
import com.mservice.momo.clientform.SubmitForm;
import com.mservice.momo.clientform.TranslateForm;
import com.mservice.momo.clientform.TranslateObject;
import com.mservice.momo.data.BillInfoService;
import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.proxy.entity.ProxyRequest;
import com.mservice.proxy.entity.ProxyRequestFactory;
import com.mservice.proxy.entity.ProxyRequestType;
import com.mservice.proxy.entity.ProxyResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by nam on 6/15/14.
 */
public class NewSubmitFormVerticle extends Verticle {
    private static final String CONNECTOR_CONFIG_FILE = "/app/mservice/proxy/connector_backend.json";
    private static final String EXTRA_KEY = "extra";
    //todo danh sach cac service co check infor
    private static final ArrayList<String> arrayServices = new ArrayList<>();
    private static final ArrayList<String> oldServiceArray = new ArrayList<>();
    static {
        oldServiceArray.add("kaspersky");
        oldServiceArray.add("capdoihoanhao");
        oldServiceArray.add("buocnhayhoanvu");
        oldServiceArray.add("remix");
        oldServiceArray.add("vnidol");
        oldServiceArray.add("thevoice");
        oldServiceArray.add("zxu");
        oldServiceArray.add("jetstar");
        oldServiceArray.add("bac");
        oldServiceArray.add("onc");
        oldServiceArray.add("avg");
        oldServiceArray.add("trendmicro");
        oldServiceArray.add("mcc");
        oldServiceArray.add("epay_viettel");
        oldServiceArray.add("epay_mobifone");
        oldServiceArray.add("epay_vinafone");
        oldServiceArray.add("epay_vietnamobile");
        oldServiceArray.add("epay_beeline");
        oldServiceArray.add("epay_vinagame");
        oldServiceArray.add("epay_vtc");
        oldServiceArray.add("epay_oncash");
        oldServiceArray.add("epay_garena");
        oldServiceArray.add("epay_megacard");
        oldServiceArray.add("lacviet");
        oldServiceArray.add("vivoo");
        oldServiceArray.add("mytv");
        oldServiceArray.add("qpal");
        oldServiceArray.add("bkav");
        oldServiceArray.add("prudential");
        oldServiceArray.add("fimplus");
        oldServiceArray.add("railway");
        oldServiceArray.add("vtvcab");
        oldServiceArray.add("giatotviet");
        oldServiceArray.add("thienhoa");
        oldServiceArray.add("pingsky");
        oldServiceArray.add("ongame");
        oldServiceArray.add("ongate");
        oldServiceArray.add("homecredit");
        oldServiceArray.add("msd");
        oldServiceArray.add("missngoisao");
        oldServiceArray.add("fptplay");
        oldServiceArray.add("hayhaytv");
        oldServiceArray.add("gatepay");
        oldServiceArray.add("operationsmile");
        oldServiceArray.add("vtv_extension");
        oldServiceArray.add("scj");
        oldServiceArray.add("cungmua");
        oldServiceArray.add("taske");
        oldServiceArray.add("anvien");
        oldServiceArray.add("jupviec");
        oldServiceArray.add("vega");
        oldServiceArray.add("finstar");
        oldServiceArray.add("fshare");
        oldServiceArray.add("hsbc");
        oldServiceArray.add("napvxuclipvn");
    }

    private static JsonObject JsonObjectExtra = new JsonObject();
    private final HashMap<String, ArrayList<FormObj>> hashMapForm = new HashMap<>();
    private final HashMap<String, BillInfoService> hashMapBIS = new HashMap<>();
    private final Map<String, Map<String, TranslateObject>> translateMap = new HashMap<>();
    private final HashMap<String, JsonObject> hashMapFormExtra = new HashMap<>();
    private final HashMap<String, JsonObject> hashMapCOREBUS = new HashMap<>();
    private Logger logger;
    private JsonObject glbCfg;
    private SubmitForm submitForm;
    private TranslateForm translateForm;
    private FormInfo formInfo;
    private boolean checkStoreApp;
    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    @Override
    public void start() {
        this.logger = container.logger();
        this.glbCfg = container.config();
        submitForm = new SubmitForm(logger, vertx, glbCfg);
        translateForm = new TranslateForm(logger, vertx, glbCfg);
        formInfo = new FormInfo(vertx, logger);
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        readAllFilesFromDir("formconfig", logger);

        logger.info("hashmapForm ---->" + hashMapForm.toString());
        loadCoreBusInfo();
        //Kiem tra app diem giao dich hay app enduser
        checkStoreApp = glbCfg.containsField(StringConstUtil.CHECK_STORE_APP) ? glbCfg.getBoolean(StringConstUtil.CHECK_STORE_APP) : false;
        if (checkStoreApp) {
            vertx.eventBus().registerHandler(AppConstant.DGD_SubmitForm_Address,
                    new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {

                            RequestObj reqObj = new RequestObj(message.body());
                            switch (reqObj.command) {
                                case Command.get_form_fields:
                                    getFormFields(message, reqObj);
                                    break;
                                case Command.get_data_for_dropbox:
                                    getDataForDropbox(message, reqObj);
                                    break;

                                case Command.get_total_form:
                                    getTotalForms(message, reqObj);
                                    break;
                                case Command.translate_confirmation:
                                    doTranslateForm(message, reqObj);
                                    break;
                                case Command.submit_form:
                                    doSubmitForm(message, reqObj);
                                    break;
                                case Command.remove_cache_bill_info:
                                    doRemoveCache(message, reqObj);
                                    break;
                                case Command.get_via_core_info:
                                    doGetViaCore(message, reqObj);
                                    break;

                                default:
                                    container.logger().info("SubmitFormVerticle  not suport for the command " + reqObj.command);
                                    message.reply(new JsonObject().putArray("field",
                                            new JsonArray()).putArray("data",
                                            new JsonArray()));
                                    break;
                            }
                        }
                    }
            );
            return;
        }
        vertx.eventBus().registerHandler(AppConstant.SubmitForm_Address,
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {

                        RequestObj reqObj = new RequestObj(message.body());
                        switch (reqObj.command) {
                            case Command.get_form_fields:
                                getFormFields(message, reqObj);
                                break;
                            case Command.get_data_for_dropbox:
                                getDataForDropbox(message, reqObj);
                                break;

                            case Command.get_total_form:
                                getTotalForms(message, reqObj);
                                break;
                            case Command.translate_confirmation:
                                doTranslateForm(message, reqObj);
                                break;
                            case Command.submit_form:
                                doSubmitForm(message, reqObj);
                                break;
                            case Command.remove_cache_bill_info:
                                doRemoveCache(message, reqObj);
                                break;
                            case Command.get_via_core_info:
                                doGetViaCore(message, reqObj);
                                break;

                            default:
                                container.logger().info("SubmitFormVerticle  not suport for the command " + reqObj.command);
                                message.reply(new JsonObject().putArray("field",
                                        new JsonArray()).putArray("data",
                                        new JsonArray()));
                                break;
                        }
                    }
                }
        );
        return;


    }

    private void doGetViaCore(Message<JsonObject> message, RequestObj reqObj) {
        JsonObject jo = hashMapCOREBUS.get(reqObj.serviceid);
        ViaConnectorObj viaConnectorObj = new ViaConnectorObj();
        if (jo != null) {

            String busname = jo.getString("busname", "");
            String billpay = jo.getString("billpay", "");
            boolean viacore = (!"".equalsIgnoreCase(busname) && !"".equalsIgnoreCase(billpay));
            boolean isNamedChecked = jo.getBoolean(ViaConnectorObj.IS_NAME_CHECKED, false);
            viaConnectorObj.IsViaConnectorVerticle = viacore;
            viaConnectorObj.BusName = busname;
            viaConnectorObj.BillPay = billpay;
            viaConnectorObj.isNamedChecked = isNamedChecked;

        }

        message.reply(viaConnectorObj.toJson());
    }

    private void loadCoreBusInfo() {
        JsonObject jo = glbCfg.getObject("connector_proxy_busname");
        if (jo != null) {
            for (String s : jo.getFieldNames()) {
                hashMapCOREBUS.put(s, jo.getObject(s));
                arrayServices.add(s);
            }
        }
    }
//        private void loadCoreBusInfo() {
//            ServiceUtil.loadConnectorServiceBusName(0, vertx, logger, new Handler<JsonArray>() {
//                @Override
//                public void handle(JsonArray jsonArrayService) {
//
//                }
//            });
//            JsonObject jo = glbCfg.getObject("connector_proxy_busname");
//            if (jo != null) {
//                for (String s : jo.getFieldNames()) {
//                    hashMapCOREBUS.put(s, jo.getObject(s));
//                    arrayServices.add(s);
//                }
//            }
//        }

    private void doRemoveCache(Message<JsonObject> message, RequestObj reqObj) {
        String key = reqObj.serviceid + reqObj.phoneNumber;
        JsonObject jo = new JsonObject();

        if (hashMapBIS.containsKey(key)) {
            hashMapBIS.get(key);
            jo.putString("desc", "remove key " + key + " success");
        } else {
            jo.putString("desc", "no value for key " + key + " to remove");
        }
        message.reply(jo);
    }

    private void doSubmitForm(final Message<JsonObject> message, final RequestObj reqObj) {

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceid;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {

                HashMap<String, String> hm = reqObj.hashMap;
                String serviceId = hm.containsKey(Const.AppClient.ServiceId) ? hm.get(Const.AppClient.ServiceId) : "";
                int phoneNumber = reqObj.phoneNumber;

                ArrayList<FormObj> forms = hashMapForm.get(serviceId);

                int totalForm = forms == null ? 0 : forms.size();
                if (jsonArray != null && jsonArray.size() > 0 && ((JsonObject) jsonArray.get(0)).getString(colName.ServiceCols.CAT_ID).equalsIgnoreCase(StringConstUtil.EVENT)
                        && !"missngoisao".equals(serviceId)) {
                    submitForm.processSubmitFrmEvent(message, hm, serviceId, phoneNumber);
                    return;
                }

                switch (serviceId) {
                    case "kaspersky":
                        submitForm.processSubmitFrmTopup(message, hm, serviceId, phoneNumber, totalForm);
                        break;
                    case "capdoihoanhao":
                        submitForm.processSubmitFrmCDHH(message, hm, serviceId, phoneNumber);
                        break;

                    case "buocnhayhoanvu":
                        submitForm.processSubmitFrmBNHV(message, hm, serviceId, phoneNumber);
                        break;

                    case "remix":
                        submitForm.processSubmitFrmREMIX(message, hm, serviceId, phoneNumber);
                        break;

                    case "thevoice":
                    case "vnidol":
                        submitForm.processSubmitFrmVNIDOL(message, hm, serviceId, phoneNumber);
                        break;

                    case "invitefriend":
                        submitForm.processSubmitFrmInviteFriend(message, hm, serviceId, phoneNumber);
                        break;

                    case "coffee_shop":
                        submitForm.processSubmitFrmCoffeeShop(message, hm, serviceId, phoneNumber);
                        break;
                    case "zxu":
                        submitForm.processSubmitFrmZingxu(message, hm, serviceId, phoneNumber, totalForm);
                        break;
                    case "jetstar":
                        submitForm.processSubmitFrmJetStar(message, hm, serviceId, phoneNumber, totalForm);
                        break;

                    case "bac":
                        submitForm.processSubmitFrmBac(message, hm, serviceId, phoneNumber, totalForm);
                        break;

                    case "onc":
                        submitForm.processSubmitFrmOnc(message, hm, serviceId, phoneNumber, totalForm);
                        break;

                    case "avg":
                        submitForm.processSubmitFrmAvg(message, hm, serviceId, phoneNumber, totalForm, hashMapBIS);
                        break;

                    case "trendmicro":
                    case "mcc":
                    case "epay_viettel":
                    case "epay_mobifone":
                    case "epay_vinafone":
                    case "epay_vietnamobile":
                    case "epay_beeline":
                    case "epay_vinagame":
                    case "epay_vtc":
                    case "epay_oncash":
                    case "epay_garena":
                    case "epay_megacard":
                    case "lacviet":
                    case "qpal":
                    case "bkav":
                    case "fimplus":
                    case "giatotviet":
                    case "operationsmile":
                    case "fptplay":
                    case "hayhaytv":
                    case "gatepay":
                    case "jupviec":
                        submitForm.processSubmitFrmTopup(message, hm, serviceId, phoneNumber, totalForm);
                        break;

                    case "mytv":
                    case "fshare":
                    case "prudential":
                    case "vtvcab":
                    case "thienhoa":
                    case "pingsky":
                    case "msd":
                    case "evnhcm":
                    case "adslfpt":
                    case "nuoccholon":
                    case "nuocbenthanh":
                    case "phtwater":
                    case "thwater":
                    case "vthcm":
                    case "railway":
                    case "vivoo":
                    case "survey":
                    case "scj":
                    case "cungmua":
                    case "taske":
                    case "evn_khanhhoa":
                    case "evn_daklak":
                    case "evn_danang":
                    case "anvien":
                    case "mobifone":
                    case "vega":
                    case "missngoisao":
                    case "finstar":
                    case "hsbc":
                    case "scb":
                    case "tdwater":
                    case "napvxuclipvn":
                        submitForm.processSubmitFrmTopupCheckInfor(message, hm, serviceId, phoneNumber, totalForm, hashMapBIS);
                        break;

                    case "homecredit":
                    case "ongame":
                    case "ongate":
                        submitForm.processSubmitFrmTopupCheckInforUseProxyRespose(message, hm, serviceId, phoneNumber, totalForm, hashMapBIS);
                        break;

                    case "vtv_extension":
                        submitForm.processSubmitFrmTopupSecondCheckInfor(message, hm, serviceId, phoneNumber, totalForm, hashMapBIS);
                        break;

                    case "proxy_confirm":
                        submitForm.processSubmitFrmTopupCheckInforOTP(message, hm, serviceId, phoneNumber, totalForm, hashMapBIS);
                        break;
                    //BEGIN 0000000004
                    case "promotion":
                        submitForm.processSubmitFrmBillPayPromo(message, hm, serviceId, phoneNumber);
                        break;
                    //END 0000000004
//                    default:
//                        submitForm.processDefault(message, hm, serviceId, phoneNumber);
//                        logger.info("Not support for service id " + serviceId);
//                        break;

                    default:
                        getAutoSubmitForm(message, reqObj, hm, totalForm, serviceId);
                        break;
                }
            }
        });
    }

    private void getAutoSubmitForm(final Message message, final RequestObj requestObj, final HashMap<String, String> hm, int totalForm, String serviceId)
    {
        logger.info(requestObj.toJson());
        if(arrayServices.contains(serviceId) && hashMapForm.get(serviceId).size() > 1)
        {
            submitForm.processSubmitFrmTopupCheckInfor(message, hm, serviceId, requestObj.phoneNumber, totalForm, hashMapBIS);
        }
        else if(arrayServices.contains(serviceId) && hashMapForm.get(serviceId).size() == 1){
            submitForm.processSubmitFrmTopup(message, hm, serviceId, requestObj.phoneNumber, totalForm);
        }
        else{
            submitForm.processDefault(message, hm, serviceId, requestObj.phoneNumber);
            logger.info("Not support for service id " + serviceId);
        }
    }

    //DONE AUTO TRANSLATE FORM
    private void doTranslateForm(final Message<JsonObject> message, final RequestObj reqObj) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + reqObj.phoneNumber);
        log.add("method", "doTranslateForm");
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceid;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {

                if (jsonArray != null && jsonArray.size() > 0 && ((JsonObject) jsonArray.get(0)).getString(colName.ServiceCols.CAT_ID).equalsIgnoreCase(StringConstUtil.EVENT)
                        && !"missngoisao".equals(reqObj.serviceid)) {
                    translateForm.translateSubmitFrmEvent(message, reqObj);
                    return;
                }
                if(!oldServiceArray.contains(reqObj.serviceid))
                {
                    logger.info("get automatic translate form " + reqObj.serviceid);
                    log.add("desc", "get automatic translate form");
                    JsonObject jo = hashMapCOREBUS.get(reqObj.serviceid);
                    final String busName = jo != null ? jo.getString("busname", "") : "";
                    logger.info("busname call translate form " + busName);
                    if(!"".equalsIgnoreCase(busName))
                    {
                        log.add("desc", "busname not equal blank");
                        ProxyRequest proxyRequest = ProxyRequestFactory.createBackendRequest(ProxyRequestType.POPUP_CONFIRM);
                        if(checkStoreApp)
                        {
                            log.add("desc", "is store app");
                            proxyRequest = ProxyRequestFactory.createBackendAgencyRequest(ProxyRequestType.POPUP_CONFIRM);
                        }
                        proxyRequest = fillProxyRequestData(reqObj, proxyRequest);
                        final ProxyRequest finalProxyRequest = proxyRequest;
                        connectorHTTPPostPathDb.findOne(proxyRequest.getServiceCode().toLowerCase(), new Handler<ConnectorHTTPPostPathDb.Obj>() {
                            @Override
                            public void handle(ConnectorHTTPPostPathDb.Obj connectorPostPathObj) {
                                if(connectorPostPathObj == null)
                                {
                                    log.add("desc", "via bus address");
                                    logger.info("number " + finalProxyRequest.getCustomerPhone() + " service id " + finalProxyRequest.getServiceCode() + " via busaddress");
                                    getConnectorTranslateFormViaBus(busName, finalProxyRequest, message, reqObj);
                                    log.writeLog();
                                    return;
                                }
                                else {
                                    log.add("desc", "via bus http post");
                                    logger.info("number " + finalProxyRequest.getCustomerPhone() + " service id " + finalProxyRequest.getServiceCode() + " via httppost");
                                    getConnectorTranslateFormViaHttp(connectorPostPathObj, log, finalProxyRequest, message, reqObj);
                                    return;
                                }
                            }
                        });
                    }
                    else {
                        getAutoTranslateForm(message, reqObj);
                    }
                }
                else {
                    switch (reqObj.serviceid) {
                        case "kaspersky":
                            translateForm.translateKaspersky(message, reqObj);
                            break;
                        case "capdoihoanhao":
                            translateForm.translateSubmitFrmCDHH(message, reqObj);
                            break;
                        case "buocnhayhoanvu":
                            translateForm.translateSubmitFrmBNHV(message, reqObj);
                        case "remix":
                            translateForm.translateSubmitFrmREMIX(message, reqObj);
                            break;

                        case "vnidol":
                            translateForm.translateSubmitFrmVNIDOL(message, reqObj);
                            break;

                        case "thevoice":
                            translateForm.translateSubmitFrmTHEVOICE(message, reqObj);

                        case "zxu":
                            translateForm.translateSubmitFrmZingxu(message, reqObj);
                            break;
                        case "jetstar":
                            translateForm.translateSubmitFrmJetStar(message, reqObj);
                            break;

                        case "bac":
                            translateForm.translateSubmitFrmBac(message, reqObj);
                            break;

                        case "onc":
                            translateForm.translateSubmitFrmOnc(message, reqObj);
                            break;

                        case "avg":
                            translateForm.translateSubmitFrmAvg(message, reqObj, hashMapBIS);
                            break;

                        case "trendmicro":
                            translateForm.translateSubmitFrmTrendmicro(message, reqObj);
                            break;

                        case "mcc":
                            translateForm.translateSubmitFrmMcc(message, reqObj);
                            break;

                        case "epay_viettel":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_mobifone":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_vinafone":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_vietnamobile":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_beeline":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_vinagame":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_vtc":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_oncash":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_garena":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "epay_megacard":
                            translateForm.translateSubmitFrmEpay(message, reqObj);
                            break;

                        case "lacviet":
                            translateForm.translateSubmitFrmLacviet(message, reqObj);
                            break;

                        case "vivoo":
                            translateForm.translateSubmitFrmVivoo(message, reqObj);
                            break;

                        case "mytv":
                            translateForm.translateSubmitFrmMytv(message, reqObj);
                            break;

                        case "qpal":
                            translateForm.translateSubmitFrmQpal(message, reqObj);
                            break;

                        case "bkav":
                            translateForm.translateSubmitFrmBkav(message, reqObj);
                            break;

                        case "prudential":
                            translateForm.translateSubmitFrmPrudential(message, reqObj, hashMapBIS);
                            break;

                        case "fimplus":
                            translateForm.translateSubmitFrmFimplus(message, reqObj);
                            break;

                        case "railway":
                            translateForm.translateSubmitFrmRailway(message, reqObj, hashMapBIS);
                            break;

                        case "vtvcab":
                            translateForm.translateSubmitFrmVtvcab(message, reqObj);
                            break;

                        case "giatotviet":
                            translateForm.translateSubmitFrmGiatotviet(message, reqObj);
                            break;

                        case "thienhoa":
                            translateForm.translateSubmitFrmThienHoa(message, reqObj, hashMapBIS);
                            break;

                        case "pingsky":
                            translateForm.translateSubmitFrmPingsky(message, reqObj, hashMapBIS);
                            break;

                        case "ongame":
                        case "ongate":
                            translateForm.translateSubmitFrmOnGame(message, reqObj, hashMapBIS);
                            break;

                        case "homecredit":
                            translateForm.translateSubmitFrmHomeCredit(message, reqObj, hashMapBIS);
                            break;

                        case "msd":
                            translateForm.translateSubmitFrmMSD(message, reqObj, hashMapBIS);
                            break;

                        case "missngoisao":
                            translateForm.translateSubmitFrmMissNgoiSao(message, reqObj);
                            break;

                        case "fptplay":
                        case "hayhaytv":
                        case "gatepay":
                        case "operationsmile":
                        case "vtv_extension":
                        case "scj":
                        case "cungmua":
                        case "taske":
                        case "anvien":
                        case "jupviec":
                        case "vega":
                        case "finstar":
                        case "fshare":
                        case "hsbc":
                        case "napvxuclipvn":
                            translateForm.translateSubmitFrmAll(translateMap.get(reqObj.serviceid), message, reqObj);
                            break;
                        default:
                            translateForm.translateDefault(message, reqObj);
                            logger.info("Not support for service id " + reqObj.serviceid);
                            break;

                    }
                } //END IF OLD SERVICE
            }
        });
    }

    private void getConnectorTranslateFormViaHttp(ConnectorHTTPPostPathDb.Obj connectorPostPathObj, final Common.BuildLog log, ProxyRequest finalProxyRequest, final Message<JsonObject> message, final RequestObj reqObj) {
        logger.info("data to connector " + finalProxyRequest.getInitiator() + " data " + finalProxyRequest.getJsonObject());
        Misc.getDataFromConnector(finalProxyRequest.getInitiator(), connectorPostPathObj.host, connectorPostPathObj.port, connectorPostPathObj.path, vertx, log, connectorPostPathObj.serviceId, finalProxyRequest.getJsonObject(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joReplyFromConnector) {
                int error = joReplyFromConnector.getInteger(StringConstUtil.ERROR, 1000);
                if (error != 0) {
                    log.add("loi tra ve tu connector ", error);
                    getAutoTranslateForm(message, reqObj);
                    log.writeLog();
                    return;
                }

                JsonObject connectorData = joReplyFromConnector.getObject(BankHelperVerticle.DATA, null);
                if (connectorData == null) {
                    log.add("connector data", "null oi");
                    getAutoTranslateForm(message, reqObj);
                    log.writeLog();
                    return;
                }
//                log.add("connector data", connectorData);
                log.writeLog();
                ProxyResponse proxyResponse = new ProxyResponse(connectorData);
                Map map = proxyResponse.getShowData();
                logger.info(map.size());
                if (map.size() == 0) {
                    logger.info("getAutoTranslateForm");
                    getAutoTranslateForm(message, reqObj);
                } else {
                    logger.info("getAutoTranslateFormWithMap");
                    getAutoTranslateFormWithMap(message, reqObj, map);
                }
            }
        });
    }

    private void getConnectorTranslateFormViaBus(String busName, ProxyRequest proxyRequest, final Message<JsonObject> message, final RequestObj reqObj) {
        vertx.eventBus().send(busName, proxyRequest.getJsonObject(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> responseConnector) {
                ProxyResponse proxyResponse = new ProxyResponse(responseConnector.body());
                Map map = proxyResponse.getShowData();
                logger.info(map.size());
                if(map.size() == 0)
                {
                    logger.info("getAutoTranslateForm");
                    getAutoTranslateForm(message, reqObj);
                }
                else{
                    logger.info("getAutoTranslateFormWithMap");
                    getAutoTranslateFormWithMap(message, reqObj, map);
                }
            }
        });
    }

    private ProxyRequest fillProxyRequestData(RequestObj requestObj, ProxyRequest proxyRequest)
    {
        HashMap<String, String> hashValue = requestObj.hashMap;
//        billID
//        debitorName
//        debitorPhone
//        debitorEmail
//        amount
//        serviceCode
//        customerEmail
//        customerName
        String billId = hashValue.containsKey(Const.AppClient.BillId) ? hashValue.get(Const.AppClient.BillId) : "";
        long amount = hashValue.containsKey(Const.AppClient.Amount) ? DataUtil.strToLong(hashValue.get(Const.AppClient.Amount)) : 0;
        String serviceCode = requestObj.serviceid;
        String customerName = hashValue.containsKey(Const.DGD.CusName) ? hashValue.get(Const.DGD.CusName) : "";
        String debitorEmail = hashValue.containsKey(Const.AppClient.Email) ? hashValue.get(Const.AppClient.Email) : "";
        String customerEmail = hashValue.containsKey(Const.DGD.CusEmail) ? hashValue.get(Const.DGD.CusEmail) : "";
        String userName = hashValue.containsKey(Const.AppClient.FullName) ? hashValue.get(Const.AppClient.FullName) : "";
        String debitorPhone = "0" + requestObj.phoneNumber;
        proxyRequest.setServiceCode(serviceCode);
        proxyRequest.setAmount(amount);
        proxyRequest.setBillId(billId);
        proxyRequest.setCustomerEmail(customerEmail);
        proxyRequest.setDebitor(debitorPhone);
        proxyRequest.setCustomerName(customerName);
        proxyRequest.setCustomerInfo(userName);
        proxyRequest.setDebitorWalletType(1);
        return proxyRequest;
    }

    private void getAutoTranslateForm(Message message, RequestObj requestObj)
    {
        if(hashMapForm.containsKey(requestObj.serviceid))
        {
            //translateForm.getAutoTranslateForm(message, requestObj, hashMapForm.get(requestObj.serviceid));
            translateForm.translateSubmitFrmAll(translateMap.get(requestObj.serviceid), message, requestObj);
        }
        else
        {
            logger.info("Khong co thong tin form");
            translateForm.translateDefault(message, requestObj);
        }
    }
    private void getAutoTranslateFormWithMap(Message message, RequestObj requestObj, Map map)
    {
        if(hashMapForm.containsKey(requestObj.serviceid))
        {
            //translateForm.getAutoTranslateForm(message, requestObj, hashMapForm.get(requestObj.serviceid));
            translateForm.translateSubmitFrmAllWithMap(translateMap.get(requestObj.serviceid), message, requestObj, map);
        }
        else
        {
            logger.info("Khong co thong tin form");
            translateForm.translateDefault(message, requestObj);
        }
    }

    private void getTotalForms(Message<JsonObject> message, RequestObj reqObj) {
        ArrayList<FormObj> form = hashMapForm.get(reqObj.serviceid);
        int totalForm = form == null ? 0 : form.size();
        message.reply(totalForm);
    }

    private void getDataForDropbox(Message<JsonObject> message,
                                   RequestObj reqObj) {

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("getDataForDropbox");
        log.add("serviceid", reqObj.serviceid);
        log.add("parentid", reqObj.parentid);
        log.add("nextform", reqObj.nextform);

        ArrayList<FormObj> form = hashMapForm.get(reqObj.serviceid);

        JsonObject joResult = new JsonObject();

        if (form == null) {
            joResult.putArray("data", new JsonArray());
            message.reply(joResult);
            return;
        }

        ArrayList<FieldData> arrayList = form.get(reqObj.nextform).fieldDatas;

        JsonArray array = new JsonArray();
        for (int i = 0; i < arrayList.size(); i++) {
            if (reqObj.parentid.equalsIgnoreCase(arrayList.get(i).parentid)) {
                array.add(arrayList.get(i).toJson());
            }
        }

        joResult.putArray("data", array);
        message.reply(joResult);
        log.add("return to client", joResult.encodePrettily());
        log.writeLog();
    }

    private void getFormFields(Message<JsonObject> message, RequestObj reqObj) {
        ArrayList<FormObj> form = hashMapForm.get(reqObj.serviceid);
        if (form == null) {
            JsonObject joResult = new JsonObject();
            joResult.putArray("field", new JsonArray());
            joResult.putArray("data", new JsonArray());
            message.reply(joResult);
            return;
        }

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + reqObj.phoneNumber);
        log.add("sid", reqObj.serviceid);
        log.add("nextform", reqObj.nextform);

        int nextForm = Math.min(reqObj.nextform, form.size() - 1);

        FormObj formObj = form.get(nextForm).clone();

        if ((nextForm == 1) && arrayServices.contains(reqObj.serviceid)) {
            logger.info("FORM 1 SERVICE ID " + reqObj.serviceid + " " + reqObj.phoneNumber );
            formInfo.refineNewFormInfo(formObj, reqObj, hashMapBIS, log, hashMapCOREBUS.get(reqObj.serviceid));
        }
        if ((nextForm == 2) && arrayServices.contains(reqObj.serviceid)) {
            logger.info("FORM 2 SERVICE ID " + reqObj.serviceid + " " + reqObj.phoneNumber );
            formInfo.refineNewFormInfo(formObj, reqObj, hashMapBIS, log, hashMapCOREBUS.get(reqObj.serviceid));
        }

        JsonObject joReply = formObj.toJson();

        message.reply(joReply);
    }

    private void readAllFilesFromDir(String dir, Logger logger) {
        final File folder = new File(dir);
        for (final File fileEntry : folder.listFiles()) {
            Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("formconfig");
            log.add("file name", fileEntry.getPath());
            log.writeLog();
            readOneFile(fileEntry.getPath(), log);
            log.add("end", "done");
            log.writeLog();
        }
    }

    private void readOneFile(String fileName, Common.BuildLog log) {
        BufferedReader br = null;
        try {

            try {
                br = new BufferedReader(new FileReader(fileName));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                String fullContent = sb.toString();
                JsonObject jsonObject = new JsonObject(fullContent);

                //lay ten dich vu
                String serviceId = "";
                for (String s : jsonObject.getFieldNames()) {
                    serviceId = s;
                    break;
                }

                //lay danh sach form
                JsonArray arrayForm = jsonObject.getArray(serviceId);

                ArrayList<FormObj> arrayListForm = new ArrayList<>();

                if (arrayForm != null && arrayForm.size() > 0) {
                    for (int i = 0; i < arrayForm.size(); i++) {
                        JsonObject joFrm = arrayForm.get(i);

                        String frmId = "";
                        for (String s : joFrm.getFieldNames()) {
                            frmId = s;
                            break;
                        }
                        arrayListForm.add(new FormObj(joFrm.getObject(frmId), frmId));
                    }
                }

                hashMapForm.put(serviceId, arrayListForm);
                if (jsonObject.containsField(EXTRA_KEY)) {
                    Map<String, TranslateObject> map = new HashMap<>();
                    JsonObject extraobject = jsonObject.getObject(EXTRA_KEY);
                    Set<String> fieldNames = extraobject.getFieldNames();
                    for (String fieldName : fieldNames) {
                        if (isJsonObjectValid(extraobject, fieldName)) {
                            map.put(fieldName, new TranslateObject(extraobject.getObject(fieldName)));
                        }
                    }
                    translateMap.put(serviceId, map);
                }

            } catch (IOException e) {
                e.printStackTrace();
                log.add("error", e.getMessage());
            }

        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                log.add("error", e.getMessage());
            }
        }
    }

    private boolean isJsonObjectValid(JsonObject testJObject, String fieldName) {
        try {
            testJObject.getObject(fieldName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
