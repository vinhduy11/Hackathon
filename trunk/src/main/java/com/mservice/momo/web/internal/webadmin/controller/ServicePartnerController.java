package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.ServiceCategory;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.data.web.ServiceDetailDb;
import com.mservice.momo.data.web.ServicePackageDb;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by locnguyen on 31/07/2014.
 */
public class ServicePartnerController {
    private Vertx vertx;

    private Logger logger;

    private ServiceDb serviceDb;
    private ServiceDetailDb serviceDetailDb;
    private ServicePackageDb servicePackageDb;
    private ServiceCategory serviceCategory;


    public ServicePartnerController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        serviceDb = new ServiceDb(vertx.eventBus(), logger);
        serviceDetailDb = new ServiceDetailDb(vertx.eventBus(), logger);
        servicePackageDb = new ServicePackageDb(vertx.eventBus(), logger);
        serviceCategory = new ServiceCategory(vertx.eventBus(), logger);

    }


    @Action(path = "/serpackage/del")
    public void serPackageDel(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "delete service detail");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        final String id = params.get(colName.ServicePackage.ID);

        log.add("id ", id);

        servicePackageDb.removeObj(id, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "delete failed");
                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }
                log.writeLog();

                update(Common.ServiceReq.COMMAND.UPDATE_PACKAGE);

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/serpackage/upsert")
    public void upsertPackage(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "upsert service package");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        ServicePackageDb.Obj obj = new ServicePackageDb.Obj();
        obj.id = params.get(colName.ServicePackage.ID);
        obj.serviceID = params.get(colName.ServicePackage.SERVICE_ID);
        obj.serviceName = params.get(colName.ServicePackage.SERVICE_NAME);
        obj.packageType = params.get(colName.ServicePackage.PACKAGE_TYPE);
        obj.packageName = params.get(colName.ServicePackage.PACKAGE_NAME);
        obj.packageValue = params.get(colName.ServicePackage.PACKAGE_VALUE);
        obj.description = params.get(colName.ServicePackage.DESCRIPTION);
        obj.linktodropbox = params.get("link");
        obj.lasttime = System.currentTimeMillis();
        obj.parentid = params.get("parentid") == null ? "" : params.get("parentid");
        obj.parentname = params.get("parentname") == null ? "" : params.get("parentname");
        obj.order = params.get("order") == null ? 100000 : DataUtil.strToInt(params.get("order"));
        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        servicePackageDb.upsertID(obj, new Handler<Boolean>() {
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

                update(Common.ServiceReq.COMMAND.UPDATE_PACKAGE);

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/serpackage/getall")
    public void getallPackagel(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String sid = params.get("sid");
        String linkto = params.get("linkto") == null ? "" : params.get("linkto");
        servicePackageDb.getlist(sid, "", "", linkto, new Handler<ArrayList<ServicePackageDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServicePackageDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTablePackage(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);

                callback.handle(result);
            }
        });
    }

    @Action(path = "/serpackage/getallAsArray")
    public void getallPackagelAsArray(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        String sid = params.get("sid");
        String parentid = params.get("parentid") == null ? "" : params.get("parentid");
        servicePackageDb.getAsArray(sid, "", parentid, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray array) {
                callback.handle(array);
            }
        });
    }

    @Action(path = "/serdetail/del")
    public void serdetaildelete(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "delete service detail");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        final String id = params.get(colName.ServiceCols.ID);

        log.add("id ", id);

        serviceDetailDb.removeObjByID(id, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "delete failed");
                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }
                log.writeLog();

                update(Common.ServiceReq.COMMAND.UPDATE_SERVICE_DETAIL_BY_SERVICE_ID);

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/serdetail/upsert")
    public void upsertDetail(HttpRequestContext context, final Handler<JsonObject> callback) {
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

        ServiceDetailDb.Obj obj = new ServiceDetailDb.Obj();
        obj.id = params.get(colName.ServiceDetailCols.ID);
        obj.serviceId = params.get(colName.ServiceDetailCols.SERVICE_ID);
        obj.fieldType = params.get(colName.ServiceDetailCols.FIELD_TYPE);
        obj.fieldLabel = params.get(colName.ServiceDetailCols.FIELD_LABEL);
        obj.isAmount = Boolean.parseBoolean(params.get(colName.ServiceDetailCols.IS_AMOUNT));
        obj.isBillId = Boolean.parseBoolean(params.get(colName.ServiceDetailCols.IS_BILLID));
        obj.key = params.get(colName.ServiceDetailCols.KEY);
        obj.required = Boolean.parseBoolean(params.get(colName.ServiceDetailCols.REQUIRED));
        obj.lastTime = System.currentTimeMillis();
        obj.order = params.get("order") == null ? 10000 : DataUtil.strToInt(params.get("order"));
        obj.hasChild = params.get("haschild") == null ? 0 : DataUtil.strToInt(params.get("haschild"));
        obj.line = params.get("line") == null ? 0 : DataUtil.strToInt(params.get("line"));

        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        serviceDetailDb.upsert(obj, new Handler<Boolean>() {
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

                update(Common.ServiceReq.COMMAND.UPDATE_SERVICE_DETAIL_BY_SERVICE_ID);

                callback.handle(result);
                return;
            }
        });

    }

    @Action(path = "/serdetail/getall")
    public void getallDetail(HttpRequestContext context, final Handler<JsonObject> callback) {

        MultiMap params = context.getRequest().params();
        String sid = params.get("sid");

        serviceDetailDb.getAll(sid, new Handler<ArrayList<ServiceDetailDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDetailDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTableDetail(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);

                callback.handle(result);
            }
        });
    }

    @Action(path = "/service/checkser")
    public void checkser(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "check service id");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        final String serviceId = params.get(colName.ServiceDetailCols.SERVICE_ID);
        String apptype = params.get("app_type") == null ? "EU" : params.get("app_type");
        boolean isStore = "EU".equalsIgnoreCase(apptype) ? false: true;

        if (StringUtils.isEmpty(serviceId)) {
            result.putNumber("error", 2);
            result.putString("desc", "Chua nhap service id");
            callback.handle(result);
            return;
        }

        serviceDb.getlist(isStore, serviceId, "", new Handler<ArrayList<ServiceDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDb.Obj> objs) {
                if (objs != null && objs.size() > 0) {
                    result.putNumber("error", 1);
                    result.putString("desc", "co");
                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "ko");
                }
                log.writeLog();

                callback.handle(result);
            }
        });
    }


    @Action(path = "/service/del")
    public void delete(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "delete service");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        final String id = params.get(colName.ServiceCols.ID);

        final String serviceId = params.get(colName.ServiceDetailCols.SERVICE_ID);

        log.add("id ", id);

        serviceDb.removeObj(id, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "delete failed");
                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                    serviceDetailDb.removeObj(serviceId, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            update(Common.ServiceReq.COMMAND.UPDATE_SERVICE);
                        }
                    });
                }
                log.writeLog();

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/service/upsert")
    public void upsert(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "upsert service");
        log.add("user", username);

        final JsonObject result = new JsonObject();
        //BEGIN 0000000050
//        String billPay = params.get(colName.ServiceCols.BILLPAY);
//        JsonArray jsonArrayBillPay = new JsonArray();
//        if (!billPay.equalsIgnoreCase("")) {
//            JsonObject jsonObjBillPay = new JsonObject();
//            jsonObjBillPay.putString(StringConstUtil.OBCPromo.JSON_OBJECT, billPay);
//            jsonArrayBillPay.add(jsonObjBillPay);
//        }
        String billPay = params.get(colName.ServiceCols.BILLPAY);
        boolean isJsonObject = Misc.isValidJsonObject(billPay);
        if(!isJsonObject)
        {
            JsonObject jsonBillPay = new JsonObject();
            jsonBillPay.putString("sub_cat_id", billPay);
            billPay = jsonBillPay.toString();
        }
                //END 0000000050
        ServiceDb.Obj obj = new ServiceDb.Obj();
        String apptype = params.get("app_type") == null ? "EU" : params.get("app_type");
        obj.isStore = "EU".equalsIgnoreCase(apptype) ? false: true;
        obj.id = params.get(colName.ServiceCols.ID);
        obj.serviceID = params.get(colName.ServiceCols.SERVICE_ID);
        obj.serviceName = params.get(colName.ServiceCols.SERVICE_NAME);
        obj.serviceType = params.get(colName.ServiceCols.SERVICE_TYPE);
        obj.partnerCode = params.get(colName.ServiceCols.PARTNER_ID);
        obj.partnerSite = params.get(colName.ServiceCols.PARTNER_SITE);
        obj.iconUrl = params.get(colName.ServiceCols.ICON_URL);
        obj.textPopup = params.get(colName.ServiceCols.TEXT_POPUP);
        obj.hasCheckDebit = Boolean.parseBoolean(params.get(colName.ServiceCols.HAS_CHECK_DEBIT));
        obj.status = DataUtil.strToInt(params.get(colName.ServiceCols.STATUS));
        obj.titleDialog = params.get(colName.ServiceCols.TITLE_DIALOG);
        obj.billType = params.get(colName.ServiceCols.BILL_TYPE);
        obj.billerID = params.get(colName.ServiceCols.BILLERID);
//        obj.billPay = params.get(colName.ServiceCols.BILLPAY);
        obj.billPay = billPay;
        obj.IsPromo = Boolean.parseBoolean(params.get(colName.ServiceCols.IS_PROMO));
        obj.lastUpdateTime = System.currentTimeMillis();

        obj.order = params.get(colName.ServiceCols.ORDER) == null ? 10000 : DataUtil.strToInt(params.get(colName.ServiceCols.ORDER));
        obj.star = DataUtil.strToInt(params.get(colName.ServiceCols.STAR));
        obj.totalForm = DataUtil.strToInt(params.get(colName.ServiceCols.TOTAL_FORM) == null ? "1" : params.get(colName.ServiceCols.TOTAL_FORM));
        obj.cateName = params.get(colName.ServiceCols.CAT_NAME) == null ? "" : params.get(colName.ServiceCols.CAT_NAME);
        obj.cateId = params.get(colName.ServiceCols.CAT_ID) == null ? "" : params.get(colName.ServiceCols.CAT_ID);
        obj.uat = params.get(colName.ServiceCols.UAT) == null ? false : Boolean.parseBoolean(params.get(colName.ServiceCols.UAT));
        obj.webPaymentUrl = params.get(colName.ServiceCols.WEB_PAYMENT_URL) == null ? "" : params.get(colName.ServiceCols.WEB_PAYMENT_URL);

        obj.check_time_on_one_bill = params.get(colName.ServiceCols.CHECK_TIME_ON_ONE_BILL) == null ? false : Boolean.parseBoolean(params.get(colName.ServiceCols.CHECK_TIME_ON_ONE_BILL));
        obj.pay_time_on_one_bill = params.get(colName.ServiceCols.PAY_TIME_ON_ONE_BILL) == null ? 0 : DataUtil.strToInt(params.get(colName.ServiceCols.PAY_TIME_ON_ONE_BILL));
        obj.not_load_eu_service = params.get(colName.ServiceCols.NOT_LOAD_EU_SERVICE) == null ? false : Boolean.parseBoolean(params.get(colName.ServiceCols.NOT_LOAD_EU_SERVICE));
        obj.statusAndroid = DataUtil.strToInt(params.get(colName.ServiceCols.STATUS_ANDROID));
        obj.active = DataUtil.strToInt(params.get(colName.ServiceCols.ACTIVE));
        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        serviceDb.upsertID(obj, new Handler<Boolean>() {
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

                update(Common.ServiceReq.COMMAND.UPDATE_SERVICE);

                callback.handle(result);
                return;
            }
        });

    }

    @Action(path = "/service/getall")
    public void getall(HttpRequestContext context, final Handler<JsonObject> callback) {

        MultiMap params = context.getRequest().params();
        String sid = params.get("serviceid") == null ? "" : params.get("serviceid");
        String catid = params.get("cateid") == null ? "" : params.get("cateid");
        String apptype = params.get("apptype") == null ? "EU" : params.get("apptype");
        boolean isStore = "EU".equalsIgnoreCase(apptype) ? false: true;

        serviceDb.getlist(isStore, sid, catid, new Handler<ArrayList<ServiceDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTable(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);

                callback.handle(result);
            }
        });
    }

    @Action(path = "/service/getallcategory")
    public void getallCategory(HttpRequestContext context, final Handler<JsonArray> callback) {

        MultiMap params = context.getRequest().params();
        String sid = params.get("sid") == null ? "" : params.get("sid");
        String catid = params.get("catid") == null ? "" : params.get("catid");
        serviceCategory.getAll(new Handler<ArrayList<ServiceCategory.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceCategory.Obj> objs) {
                JsonArray result = new JsonArray();
                if (objs != null && objs.size() > 0) {
                    for (int i = 0; i < objs.size(); i++) {
                        result.add(objs.get(i).toJson());
                    }
                }
                callback.handle(result);
            }
        });
    }

    @Action(path = "/service/getallserviceid")
    public void getallServiceId(HttpRequestContext context, final Handler<JsonArray> callback) {


        serviceDb.getAll(new Handler<ArrayList<ServiceDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDb.Obj> objs) {
                JsonArray result = new JsonArray();
                if (objs != null && objs.size() > 0) {
                    for (int i = 0; i < objs.size(); i++) {
                        result.add(objs.get(i).toJsonObject());
                    }
                }
                callback.handle(result);
            }
        });
    }


    public String buildTablePackage(ArrayList<ServicePackageDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Service ID</th>" +
                "  <th>Service Name</th>" +
                "  <th>Package Type</th>" +
                "  <th>Package Text</th>" +
                "  <th>Package Value</th>" +
                "  <th>Description</th>" +
                "  <th>Link to Dropbox(key)</th>" +
                "  <th>Parent name</th>" +
                "  <th>Order</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += getRowServicePackage(i, objs.get(i));
            }
        }
        result += "</table>";
        return result;
    }

    public String getRowServicePackage(int position, ServicePackageDb.Obj input) {
        String result = "";
        result += "<tr>\n" +
                "<td>" +
                "<button id='edit' id_ser = '" + input.id + "'" + "parentid = '" + input.parentid + "'" + "val ='" + position + "'>Edit</button>" +
                "<button id='del' id_ser = '" + input.id + "'" + "val ='" + position + "'>Del</button>" +
                "</td>" +
                "  <td rid='" + position + "'>" + input.serviceID + "</td>" +
                "  <td rid='" + position + "'>" + input.serviceName + "</td>" +
                "  <td rid='" + position + "'>" + input.packageType + "</td>" +
                "  <td rid='" + position + "'>" + input.packageName + "</td>" +
                "  <td rid='" + position + "'>" + input.packageValue + "</td>" +
                "  <td rid='" + position + "'>" + input.description + "</td>" +
                "  <td rid='" + position + "'>" + input.linktodropbox + "</td>" +
                "  <td rid='" + position + "'>" + input.parentname + "</td>" +
                "  <td rid='" + position + "'>" + input.order + "</td>" +
                "</tr>";
        return result;
    }

    public String buildTableDetail(ArrayList<ServiceDetailDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Service id</th>" +
                "  <th>Field type</th>" +
                "  <th>Field label</th>" +
                "  <th>Is amount</th>" +
                "  <th>Is billId</th>" +
                "  <th>Key</th>" +
                "  <th>Required</th>" +
                "  <th>Order</th>" +
                "  <th>Has child</th>" +
                "  <th>Line</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += getRowServiceDetail(i, objs.get(i));
            }
        }
        result += "</table>";
        return result;
    }

    public String getRowServiceDetail(int position, ServiceDetailDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td><button id= 'edit' id_ser_de = '" + input.id + "'" + "val ='" + position + "'>Edit</button>" +
                "<button id= 'del' id_ser_de = '" + input.id + "'" + "val ='" + position + "'>Del</button>" +
                "</td>" +
                "  <td rid='" + position + "'>" + input.serviceId + "</td>" +
                "  <td rid='" + position + "'>" + input.fieldType + "</td>" +
                "  <td rid='" + position + "'>" + input.fieldLabel + "</td>" +
                "  <td rid='" + position + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
        if (input.isAmount)
            result += " checked ";
        result += ">" + "</td>";
        result += "  <td rid='" + position + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
        if (input.isBillId)
            result += " checked ";
        result += ">" + "</td>";

        result += "  <td rid='" + position + "'>" + input.key + "</td>";

        result += "  <td rid='" + position + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
        if (input.required)
            result += " checked ";
        result += ">" + "</td>";

        result += "</td>";
        result += "  <td rid='" + position + "'>" + input.order + "</td>" +
                "  <td rid='" + position + "'>" + (input.hasChild == 0 ? "No" : "Yes") + "</td>" +
                "  <td rid='" + position + "'>" + input.line + "</td>" +
                "</tr>";
        return result;
    }

    public String buildTable(ArrayList<ServiceDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>App Type</th>" +
                "  <th>Service id</th>" +
                "  <th>Service Name</th>" +
                "  <th>Service type</th>" +
                "  <th>Partner id</th>" +
                "  <th>Partner site</th>" +
                "  <th>Icon url</th>" +
                "  <th>Text popup</th>" +
                "  <th>Check debit</th>" +
                "  <th>Status</th>" +
                "  <th>Title dialog</th>" +
                "  <th>Bill type</th>" +
                "  <th>Billerid</th>" +
                "  <th>Billyay</th>" +
                "  <th>Ispromo</th>" +
                "  <th>Order</th>" +
                "  <th>Star</th>" +
                "  <th>Next form</th>" +
                "  <th>Category</th>" +
                "  <th>UAT</th>" +
                "  <th>Web Payment Url</th>" +
                "  <th>Check Time To Pay</th>" +
                "  <th>Duration To Pay/GroupDGD</th>" +
                "  <th>Not load eu service</th>" +
                "  <th>Status Android</th>" +
                "  <th>Active</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += getRowError(i, objs.get(i));
            }
        }
        result += "</table>";
        return result;
    }

    public String getRowError(int position, ServiceDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td>" +
                "<button id= 'edit' id_ser = '" + input.id + "'" + " catid='" + input.cateId + "'  val ='" + position + "'>Edit</button>" +
//                "<button id = 'del' id_ser = '" + input.id + "'" + "val ='" + position + "'>Del</button>" +
                "</td>" +
                "  <td rid='" + position + "'>" + (input.isStore ? "DGD" : "EU") + "</td>" +
                "  <td rid='" + position + "'>" + input.serviceID + "</td>" +
                "  <td rid='" + position + "'>" + input.serviceName + "</td>" +
                "  <td rid='" + position + "'>" + input.serviceType + "</td>" +
                "  <td rid='" + position + "'>" + input.partnerCode + "</td>" +
                "  <td rid='" + position + "'>" + input.partnerSite + "</td>" +
                "  <td rid='" + position + "'>" + input.iconUrl + "</td>" +
                "  <td rid='" + position + "'>" + input.textPopup + "</td>" +
                "  <td rid='" + position + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
        if (input.hasCheckDebit)
            result += " checked ";
        result += ">" + "</td>" +

                "  <td rid='" + position + "'>";
        if (input.status == 0)
            result += "Off";
        else if (input.status == 1)
            result += "On";
        else
            result += "Future";
        result += "</td>" +


                "  <td rid='" + position + "'>" + input.titleDialog + "</td>";
        result += "</td>" +

                "  <td rid='" + position + "'>" + input.billType + "</td>" +
                "  <td rid='" + position + "'>" + input.billerID + "</td>" +
                "  <td rid='" + position + "'>" + input.billPay + "</td>" +
                "  <td rid='" + position + "'>" + input.IsPromo + "</td>" +

                "  <td rid='" + position + "'>" + input.order + "</td>" +
                "  <td rid='" + position + "'>" + input.star + "</td>" +
                "  <td rid='" + position + "'>" + input.totalForm + "</td>" +
                "  <td rid='" + position + "'>" + input.cateName + "</td>" +
                "  <td rid='" + position + "'>" + input.uat + "</td>" +
                "  <td rid='" + position + "'>" + input.webPaymentUrl + "</td>" +
                "  <td rid='" + position + "'>" + input.check_time_on_one_bill + "</td>" +
                "  <td rid='" + position + "'>" + input.pay_time_on_one_bill + "</td>" +
                "  <td rid='" + position + "'>" + input.not_load_eu_service + "</td>" +
                "  <td rid='" + position + "'>";
        if (input.statusAndroid == 0)
            result += "Off";
        else if (input.statusAndroid == 1)
            result += "On";
        else
            result += "Future";
        result += "</td>" +
                /***/
                "  <td rid='" + position + "'>";
        if (input.active == 0)
            result += "Off";
        else
            result += "On";
        result += "</td>" +
                /***/
                "</tr>";

        return result;
    }

    private void update(int comman) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = comman;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
    }

    @Action(path = "/service/reload")
    public void reload(HttpRequestContext context, final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "reload service partner");

        final JsonObject result = new JsonObject();

        update(Common.ServiceReq.COMMAND.UPDATE_SERVICE);

        result.putNumber("error", 0);
        result.putString("desc", "");
        callback.handle(result);
    }

    @Action(path = "/serdetail/reload")
    public void reloadSerdetail(HttpRequestContext context, final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "reload service detail");

        final JsonObject result = new JsonObject();

        update(Common.ServiceReq.COMMAND.UPDATE_SERVICE_DETAIL_BY_SERVICE_ID);

        result.putNumber("error", 0);
        result.putString("desc", "");
        callback.handle(result);
    }
}