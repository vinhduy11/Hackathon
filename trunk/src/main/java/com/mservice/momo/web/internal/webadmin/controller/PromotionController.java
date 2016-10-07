package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.m2mpromotion.MerchantPromoTracksDb;
import com.mservice.momo.data.m2mpromotion.MerchantPromosDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.*;

/**
 * Created by locnguyen on 29/07/2014.
 */
public class PromotionController {
    private Vertx vertx;

    private Logger logger;
    private PromotionDb promotionDb;
    private Common mCom;
    private MerchantPromosDb merchantPromosDb;
    private MerchantPromoTracksDb merchantPromoTracksDb;
    private HashMap<String,String> hashMapGroup = new HashMap<>();

    public PromotionController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        promotionDb = new PromotionDb(vertx.eventBus(), logger);
        mCom = new Common(vertx,logger, container.config());

        merchantPromosDb = new MerchantPromosDb(vertx.eventBus(), container.logger());
        merchantPromoTracksDb = new MerchantPromoTracksDb(vertx.eventBus(),logger);

        /*"chain_coffee_group":[
        {"user_group":"RTEDCFV", "value": "hn"}
        ,{"user_group":"GBNHJKI", "value": "hcm"}
        ]*/
        JsonObject glbCfg = container.config();
        JsonArray array = glbCfg.getArray("chain_coffee_group", null);
        for (int i =0;i< array.size();i++){
            JsonObject jo = array.get(i);
            hashMapGroup.put(jo.getString("user_group"), jo.getString("value"));
        }
    }

    @Action(path = "/promo/upsert")
    public void upsert(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = WebAdminController.checkSession(context,callback);
        if ("".equalsIgnoreCase(username)){
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "upsert promotion");
        log.add("user",username);

        final JsonObject result = new JsonObject();

        PromotionDb.Obj obj = new PromotionDb.Obj();
        obj.ID = params.get("id_promo");
        obj.NAME = params.get("name");
        obj.DATE_FROM = Misc.str2BeginDate(params.get("frDate"));
        obj.DATE_TO = Misc.str2EndDate(params.get("toDate"));
        obj.TRAN_MIN_VALUE = DataUtil.stringToUNumber(params.get("tranMinVal"));
        obj.PER_TRAN_VALUE = DataUtil.strToInt(params.get("preTranVal"));
        obj.DURATION = DataUtil.strToInt(params.get("dru"));
        obj.INTRO_DATA = params.get("introData");
        obj.INTRO_SMS = params.get("introSms");
        obj.MIN_TIMES = DataUtil.strToInt(params.get("minTimes"));
        obj.MAX_TIMES = DataUtil.strToInt(params.get("maxTimes"));
        obj.NOTI_CAPTION = params.get("notiCap");
        obj.NOTI_BODY_INVITER = params.get("bodyInviter");
        obj.NOTI_SMS_INVITER = params.get("smsInviter");
        obj.NOTI_BODY_INVITEE = params.get("bodyInvitee");
        obj.NOTI_SMS_INVITEE = params.get("smsInvitee");
        obj.NOTI_COMMENT = params.get("notiCom");
        obj.ADJUST_ACCOUNT = params.get("adjustAcc");
        obj.ADJUST_PIN = params.get("pin").trim();
        obj.DURATION_TRAN = DataUtil.strToInt(params.get("durationTran"));

        obj.OFF_TIME_FROM = params.get("OffTimeFrom").trim();
        obj.OFF_TIME_TO = params.get("OffTimeTo").trim();
        obj.STATUS = "true".equalsIgnoreCase(params.get("status").trim()) ? true : false;
        obj.STATUS_IOS = "true".equalsIgnoreCase(params.get("statusIOS").trim()) ? true : false;
        obj.STATUS_ANDROID = "true".equalsIgnoreCase(params.get("statusANDROID").trim()) ? true : false;
        obj.ENABLE_PHASE2 = "true".equalsIgnoreCase(params.get("enablePhase2").trim()) ? true : false;

        obj.EXTRA = toJSONValid(params.get("extra").trim());


        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        promotionDb.upsertPromo(obj.ID, obj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "upsert failed");
                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }

                callback.handle(result);

                Promo.PromoReqObj reqObj = new Promo.PromoReqObj();
                reqObj.COMMAND =Promo.PromoType.PROMO_UPDATE_DATA;
                log.add("json request", reqObj.toJsonObject());

                vertx.eventBus().publish(AppConstant.Promotion_ADDRESS_UPDATE,reqObj.toJsonObject());
                return;
            }
        });
    }

    @Action(path = "/promo/reload")
    public void reload(HttpRequestContext context, final Handler<JsonObject> callback) {

        String username = WebAdminController.checkSession(context,callback);
        if ("".equalsIgnoreCase(username)){
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "reload promotion");
        log.add("user",username);

        final JsonObject result = new JsonObject();

        Promo.PromoReqObj reqObj = new Promo.PromoReqObj();
        reqObj.COMMAND =Promo.PromoType.PROMO_UPDATE_DATA;
        log.add("json request", reqObj.toJsonObject());

        vertx.eventBus().publish(AppConstant.Promotion_ADDRESS_UPDATE,reqObj.toJsonObject());

        result.putNumber("error", 0);
        result.putString("desc", "");
        callback.handle(result);
    }

    @Action(path = "/promo/getall")
    public void getall(HttpRequestContext context, final Handler<JsonObject> callback) {

        promotionDb.getPromotions(new Handler<ArrayList<PromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<PromotionDb.Obj> objs) {
                JsonObject result = null;

                if (objs != null && objs.size() > 0) {
                    String temp = buildTable(objs);
                    if (temp != null && !"".equalsIgnoreCase(temp))
                        result = new JsonObject().putString("table", temp);

                }

                JsonArray listObj = new JsonArray();
                for (int i = 0; i < objs.size(); i++) {
                    listObj.add(objs.get(i).toJsonObject());
                }

                result.putArray("data", listObj);
                callback.handle(result);
            }
        });
    }

    public String buildTable(ArrayList<PromotionDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Promo Name</th>" +
                "  <th>From Date</th>" +
                "  <th>To Date</th>" +
                "  <th>Tran Min Value</th>" +
                "  <th>Per Tran Value</th>" +
                "  <th>Duration</th>" +
                "  <th>Intro Data</th>" +
                "  <th>Intro SMS/ Gift Type ID</th>" +
                "  <th>Min Times</th>" +
                "  <th>Max Times</th>" +
                "  <th>Noti Caption</th>" +
                "  <th>Noti Body Inviter</th>" +
                "  <th>Noti SMS Inviter</th>" +
                "  <th>Noti Body Invitee</th>" +
                "  <th>Noti SMS Invitee</th>" +
                "  <th>Noti Comment</th>" +
                "  <th>Adjust Account</th>" +
                "  <th>Duration Tran</th>" +
                "  <th>Pin</th>" +
                "  <th>OffTime From</th>" +
                "  <th>OffTime To</th>" +
                "  <th>Status</th>" +
                "  <th>Status IOS</th>" +
                "  <th>Status ANDROID</th>" +
                "  <th>Enable Phase 2</th>" +
                "  <th>Extra Infomation</th>" +
                "</tr>";
        for (int i = 0; i < objs.size(); i++) {
            result += getRowError(i, objs.get(i));
        }


        result += "</table>";
        return result;
    }

    public String getRowError(int position, PromotionDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td><button id_promo = '" + input.ID + "'" + "val ='" + position + "'>Edit</button></td>" +
                "  <td rid='" + position + "'>" + input.NAME + "</td>" +
                "  <td rid='" + position + "'>" + Misc.dateVNFormat(input.DATE_FROM) + "</td>" +
                "  <td rid='" + position + "'>" + Misc.dateVNFormat(input.DATE_TO) + "</td>" +
                "  <td rid='" + position + "'>" + input.TRAN_MIN_VALUE + "</td>" +
                "  <td rid='" + position + "'>" + input.PER_TRAN_VALUE + "</td>" +
                "  <td rid='" + position + "'>" + input.DURATION + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.INTRO_DATA + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.INTRO_SMS + "</td>" +
                "  <td rid='" + position + "'>" + input.MIN_TIMES + "</td>" +
                "  <td rid='" + position + "'>" + input.MAX_TIMES + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.NOTI_CAPTION + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.NOTI_BODY_INVITER + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.NOTI_SMS_INVITER + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.NOTI_BODY_INVITEE + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.NOTI_SMS_INVITEE + "</td>" +
                "  <td class='area' rid='" + position + "'>" + input.NOTI_COMMENT + "</td>" +
                "  <td rid='" + position + "'>" + input.ADJUST_ACCOUNT + "</td>" +
                "  <td rid='" + position + "'>" + input.DURATION_TRAN + "</td>" +
                "  <td rid='" + position + "'>" + input.ADJUST_PIN + "</td>" +
                "  <td rid='" + position + "'>" + input.OFF_TIME_FROM + "</td>" +
                "  <td rid='" + position + "'>" + input.OFF_TIME_TO + "</td>" +
                "  <td rid='" + position + "'>" + input.STATUS+ "</td>" +
                "  <td rid='" + position + "'>" + input.STATUS_IOS+ "</td>" +
                "  <td rid='" + position + "'>" + input.STATUS_ANDROID+ "</td>" +
                "  <td rid='" + position + "'>" + input.ENABLE_PHASE2+ "</td>" +
                "  <td rid='" + position + "'>" + input.EXTRA+ "</td>" +
                "</tr>";
        return result;
    }


    @Action(path = "/service/merchant/getAll")
    public void getAll(HttpRequestContext context, final Handler<Object> callback) {

        merchantPromosDb.getAllMerchant(new Handler<List<MerchantPromosDb.Obj>>() {
            @Override
            public void handle(List<MerchantPromosDb.Obj> event) {
                JsonArray arr = new JsonArray();
                for (MerchantPromosDb.Obj obj : event) {
                    arr.add(obj.toJsonObject());
                }
                callback.handle(arr);
            }
        });
    }

    @Action(path = "/service/merchant/filter")
    public void getWithFilter(HttpRequestContext context, final Handler<Object> callback) {

        MultiMap params = context.getRequest().params();

        String cofAgent = params.get("cofAgent");
        String group = params.get("group");
        String program = params.get("program");
        String groupMapped = hashMapGroup.get(group);

        if("".equalsIgnoreCase(group) || "".equalsIgnoreCase(groupMapped) || groupMapped == null){
            callback.handle(new JsonArray());
            return;
        }

        merchantPromosDb.getMerchantWithFilter(cofAgent,program,groupMapped, new Handler<List<MerchantPromosDb.Obj>>() {
            @Override
            public void handle(List<MerchantPromosDb.Obj> objs) {
                JsonArray arr = new JsonArray();
                for (MerchantPromosDb.Obj obj : objs) {
                    arr.add(obj.toJsonObject());
                }
                callback.handle(arr);
            }
        });

        /*merchantPromosDb.getAllMerchant(new Handler<List<MerchantPromosDb.Obj>>() {
            @Override
            public void handle(List<MerchantPromosDb.Obj> event) {
                JsonArray arr = new JsonArray();
                for (MerchantPromosDb.Obj obj : event) {
                    arr.add(obj.toJsonObject());
                }
                callback.handle(arr);
            }
        });*/
    }

    @Action(path = "/service/merchant/gencode")
    public void M2MCoffeeGenCode(HttpRequestContext context, final Handler<Object> callback) {

        /*mernum: $("#txtMernum").val(),
                cusnum: $("#txtCusnum").val(),
                tranid: Date.parse($("#txtTranid").val()),
                amount: Date.parse($("#txtAmount").val())*/

        MultiMap params = context.getRequest().params();

        String mernum=params.get("mernum");
        String cusnum = params.get("cusnum");
        long tranid = DataUtil.stringToUNumber(params.get("tranid"));
        long amount= DataUtil.stringToUNumber(params.get("amount"));

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.FORCE_PROMO_M2M_GEN_CODE;
        promoReqObj.CREDITOR ="0" + DataUtil.strToInt(mernum);
        promoReqObj.DEBITOR = "0" + DataUtil.strToInt(cusnum);

        promoReqObj.TRAN_AMOUNT = amount;
        promoReqObj.TRAN_TYPE = MomoProto.TranHisV1.TranType.M2M_VALUE;
        promoReqObj.TRAN_ID = tranid;

        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                callback.handle(jsonObject);
            }
        });
    }


    @Action(path = "/service/merchant/search")
    public void getSearch(HttpRequestContext context, final Handler<Object> callback) {


        MultiMap params = context.getRequest().params();
        String code= params.get("code");
        code = code.toUpperCase();
        if(!"".equalsIgnoreCase(code) &&  !code.startsWith("MOMO")){
            code = "MOMO" + code;
        }
        String cofAgent=params.get("cofAgent");
        String cusAgent = params.get("cusAgent");
        String fromDate = params.get("fromDate");
        String toDate=params.get("toDate");

        long lFromDate = 0;
        if(fromDate !=null && !"".equalsIgnoreCase(fromDate)){
            fromDate += " 00:00:00";
            lFromDate = Misc.getDateAsLong(fromDate,"dd/MM/yyyy hh:mm:ss",logger,"");
        }

        long lToDate = 0;

        if(toDate != null && !"".equalsIgnoreCase(toDate)){
            toDate+= " 23:59:59";
            lToDate = Misc.getDateAsLong(toDate,"dd/MM/yyyy hh:mm:ss",logger,"");
        }

        merchantPromoTracksDb.search(code,cofAgent,cusAgent,lFromDate,lToDate,new Handler<List<MerchantPromoTracksDb.Obj>>() {
            @Override
            public void handle(List<MerchantPromoTracksDb.Obj> objs) {
                JsonArray array = new JsonArray();

                if(objs != null && objs.size() > 0){

                    for (MerchantPromoTracksDb.Obj obj : objs) {
                        array.add(obj.toJsonforWeb());
                    }
                }
                callback.handle(array);
            }
        });
    }

    public Date toDate(String val) {
        long v = 0;
        v = Long.parseLong(val);
        return new Date(v);
    }

    @Action(path = "/service/merchant/upsert")
    public void upsertMerchant(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        final String id = params.get("_id");
        final String name = params.get("name");
        final String from_date = params.get("from_date");
        final String to_date = params.get("to_date");

        final String total_code = params.get("total_code");
        final String used_code = params.get("used_code");
        final String max_code = params.get("max_code");
        final String total_val = params.get("total_val");

        final String max_val = params.get("max_val");
        final String threshold = params.get("threshold");
        final String exp_day = params.get("exp_day");
        final String source_acc = params.get("source_acc");

        final String source_pin = params.get("source_pin");
        final String comment = params.get("comment");
        final String max_tran_per_day = params.get("max_tran_per_day");
        final String val_list = params.get("val_list");
        final String num_list = params.get("num_list");
        final String group = params.get("group");
        final String program = params.get("program");


        if (id == null || name == null || from_date == null || to_date == null || total_code == null ||
                used_code == null || max_code == null || total_val == null || max_val == null || threshold == null
                || exp_day == null || source_acc == null || source_pin == null || comment == null ||
                max_tran_per_day == null || val_list == null || num_list == null || group == null || program ==null) {
            callback.handle(new JsonObject().putNumber("error", 2).putString("desc", "missing params"));
            return;
        }


        final MerchantPromosDb.Obj obj = new MerchantPromosDb.Obj();
        obj.number = id;
        obj.name = name;
        obj.fromDate = toDate(from_date);
        obj.toDate = toDate(to_date);

        obj.totalCode = Integer.parseInt(total_code);
        obj.usedCode= Integer.parseInt(used_code);
        obj.maxCode = Integer.parseInt(max_code);
        obj.totalVal = Integer.parseInt(total_val);

        obj.maxVal = Integer.parseInt(max_val);
        obj.threshold = Long.parseLong(threshold);
        obj.expireDay = Integer.parseInt(exp_day);
        obj.sourceAcc = source_acc;

        obj.sourcePin = source_pin;
        obj.comment = comment;
        obj.maxTranPerDay = Integer.parseInt(max_tran_per_day);
        obj.valList = toLongList(val_list);
        obj.numList = toArrayList(num_list);
        obj.group = group;
        obj.program = program;

        merchantPromosDb.findMoreByNumberInList(num_list,new Handler<ArrayList<MerchantPromosDb.Obj>>() {
            @Override
            public void handle(ArrayList<MerchantPromosDb.Obj> arrayList) {

                //khong co  or trung voi chinh no
                if(arrayList == null || arrayList.size() <= 1 ){
                    merchantPromosDb.upsertMerchant(obj, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            callback.handle(new JsonObject()
                                    .putNumber("error", 0)
                                    .putString("desc", "Success!"));
                        }
                    });
                }else{
                    callback.handle(new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "Đã nằm trong danh sách khác"));
                }
            }
        });
    }

    private ArrayList<String> toArrayList(String num_list){
        String[] ar = num_list.split(",");
        ArrayList list = new ArrayList();
        for (int i=0;i<ar.length;i++){
            list.add(ar[i]);
        }
        return  list;
    }

    @Action(path = "/service/merchant/view")
    public void viewMerchant(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String id = params.get("_id");
        String name = params.get("name");
        String from_date = params.get("from_date");
        String to_date = params.get("to_date");

        String total_code = params.get("total_code");
        String used_code = params.get("used_code");
        String max_code = params.get("max_code");
        String total_val = params.get("total_val");

        String max_val = params.get("max_val");
        String threshold = params.get("threshold");
        String exp_day = params.get("exp_day");
        String source_acc = params.get("source_acc");

        String source_pin = params.get("source_pin");
        String comment = params.get("comment");
        String max_tran_per_day = params.get("max_tran_per_day");
        String val_list = params.get("val_list");

        if (id == null || name == null || from_date == null || to_date == null || total_code == null ||
                used_code == null || max_code == null || total_val == null || max_val == null || threshold == null
                || exp_day == null || source_acc == null || source_pin == null || comment == null ||
                max_tran_per_day == null || val_list == null) {
            callback.handle(new JsonObject().putNumber("error", 2).putString("desc", "missing params"));
            return;
        }

        MerchantPromosDb.Obj obj = new MerchantPromosDb.Obj();
        try {
            obj.number = id;
            obj.name = name;
            obj.fromDate = toDate(from_date);
            obj.toDate = toDate(to_date);

            obj.totalCode = Integer.parseInt(total_code);
            obj.usedCode= Integer.parseInt(used_code);
            obj.maxCode = Integer.parseInt(max_code);
            obj.totalVal = Integer.parseInt(total_val);

            obj.maxVal = Integer.parseInt(max_val);
            obj.threshold = Long.parseLong(threshold);
            obj.expireDay = Integer.parseInt(exp_day);
            obj.sourceAcc = source_acc;

            obj.sourcePin = source_pin;
            obj.comment = comment;
            obj.maxTranPerDay = Integer.parseInt(max_tran_per_day);
            obj.valList = toLongList(val_list);
        } catch (Exception e) {
            callback.handle(new JsonObject()
                            .putNumber("error", 3)
                            .putString("desc", "Invalid param value!")
                            .putString("e", e.getMessage())
            );
            return;
        }
        merchantPromosDb.upsertMerchant(obj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                callback.handle(new JsonObject()
                                .putNumber("error", 0)
                                .putString("desc", "Success!")
                );
                return;
            }
        });
    }


    private ArrayList<Long> toLongList(String val_list) {
        ArrayList<Long> list = new ArrayList<>();
        String[] arr = val_list.split(",");
        for (String str : arr) {
            try {
                Long val = Long.valueOf(str);
                list.add(val);
            } catch (Exception e) {

            }
        }
        return list;
    }

    public JsonObject toJSONValid(String test) {
        try {
            return new JsonObject(test);
        } catch (Exception ex) {
            return new JsonObject();
        }
    }
}