package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.ServiceCategory;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.web.HomePageDb;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.data.web.ServiceDetailDb;
import com.mservice.momo.data.web.ServicePackageDb;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by tumegame on 11/01/2016.
 */
public class HomeController {

    private Vertx vertx;

    private Logger logger;

    private HomePageDb homeDb;
    private ServiceDb serviceDb;
    private ServiceDetailDb serviceDetailDb;
    private ServicePackageDb servicePackageDb;
    private ServiceCategory serviceCategory;


    public HomeController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        homeDb = new HomePageDb(vertx.eventBus(), logger);
        serviceDb = new ServiceDb(vertx.eventBus(), logger);
        serviceDetailDb = new ServiceDetailDb(vertx.eventBus(), logger);
        servicePackageDb = new ServicePackageDb(vertx.eventBus(), logger);
        serviceCategory = new ServiceCategory(vertx.eventBus(), logger);

    }


    @Action(path = "/home/checkser")
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

        final String id = params.get(colName.ServiceDetailCols.ID);

        if (StringUtils.isEmpty(id)) {
            result.putNumber("error", 0);
            result.putString("desc", "Thieu id");
            callback.handle(result);
            return;
        }

        homeDb.getById(id, new Handler<Boolean>() {
            @Override
            public void handle(Boolean check) {
                if (check) {
                    log.add("desc", "co");
                    result.putNumber("error", 1);
                    result.putString("desc", "co");
                } else {
                    log.add("desc", "ko");
                    result.putNumber("error", 0);
                    result.putString("desc", "ko");
                }
                log.writeLog();

                callback.handle(result);
            }
        });
    }


    @Action(path = "/home/del")
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

        log.add("id ", id);

        homeDb.removeObj(id, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    log.add("desc", "delete failed");
                    result.putNumber("error", 1);
                    result.putString("desc", "delete failed");
                } else {
                    log.add("desc", "delete success");
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }
                log.writeLog();
                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/home/upsert")
    public void upsert(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "upsert service");

        final JsonObject result = new JsonObject();

        HomePageDb.Obj obj = new HomePageDb.Obj();
        obj.id = params.get(colName.HomeCols.ID);
        obj.serviceType = DataUtil.strToInt(params.get(colName.HomeCols.SERVICE_TYPE));
        obj.function = DataUtil.strToInt(params.get(colName.HomeCols.FUNC));
        obj.serviceID = params.get(colName.HomeCols.SERVICE_ID);
        obj.textPopup = params.get(colName.HomeCols.TEXT_POPUP);
        obj.webUrl = params.get(colName.HomeCols.WEB_URL);
        obj.cateId = params.get(colName.HomeCols.CAT_ID);
        obj.serviceName = params.get(colName.HomeCols.SERVICE_NAME);
        obj.iconUrl = params.get(colName.HomeCols.ICON_URL);
        obj.status = DataUtil.strToInt(params.get(colName.HomeCols.STATUS));
        obj.lastUpdateTime = System.currentTimeMillis();
        obj.position = DataUtil.strToInt(params.get(colName.HomeCols.POSITION));
        obj.order = DataUtil.strToInt(params.get(colName.HomeCols.ORDER));
        obj.isStore = false;
        obj.buttonTitle = params.get(colName.HomeCols.BUTTON_TITLE);

        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            log.add("desc", "input invalid");
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        homeDb.upsertID(obj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    log.add("desc", "upsert fail");
                    result.putNumber("error", 1);
                    result.putString("desc", "upsert fail");
                } else {
                    log.add("desc", "upsert success");
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

    @Action(path = "/home/getall")
    public void getall(HttpRequestContext context, final Handler<JsonObject> callback) {

        homeDb.getAll(new Handler<ArrayList<HomePageDb.Obj>>() {
            @Override
            public void handle(ArrayList<HomePageDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTable(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);

                callback.handle(result);
            }
        });
    }

    @Action(path = "/home/getallactive")
    public void getAllActive(HttpRequestContext context, final Handler<String> callback) {

        homeDb.getAllActive(new Handler<String>() {
            @Override
            public void handle(String reply) {

                String result = "{\"data\":" + reply + "}";
                callback.handle(result);
            }
        });
    }

    @Action(path = "/home/getServiceType")
    public void getByServiceType(HttpRequestContext context, final Handler<JsonObject> callback) {

        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "getServiceType");

        final int serType = params.get(colName.ServiceCols.SERVICE_TYPE) != null ? DataUtil.strToInt(params.get(colName.ServiceCols.SERVICE_TYPE)) : 0;
        final String serviceId = params.get(colName.ServiceCols.SERVICE_ID);
        log.add("function", params.toString());
        homeDb.getByServiceType(serType, serviceId, new Handler<ArrayList<HomePageDb.Obj>>() {
            @Override
            public void handle(ArrayList<HomePageDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTable(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);
                log.writeLog();
                callback.handle(result);
            }
        });
    }


    public String buildTable(ArrayList<HomePageDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Service type</th>" +
                "  <th>Function</th>" +
                "  <th>Service id</th>" +
                "  <th>Text popup</th>" +
                "  <th>Web url</th>" +
                "  <th>Catelogy Id</th>" +
                "  <th>Service name</th>" +
                "  <th>Icon url</th>" +
                "  <th>Status</th>" +
                "  <th>Position</th>" +
                "  <th>Order</th>" +
                "  <th>Button title</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += getRowError(i, objs.get(i));
            }
        }
        result += "</table>";
        return result;
    }

    public String getRowError(int position, HomePageDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td>" +
                "<button id= 'edit' id_ser = '" + input.id + "'" + "val ='" + position + "'>Edit</button>" +
                "<button id = 'del' id_ser = '" + input.id + "'" + "val ='" + position + "'>Del</button>" +
                "</td>" +
                "  <td rid='" + position + "'>";
        if (input.serviceType == 1)
            result += "Function";
        else if (input.serviceType == 2)
            result += "Service";
        else if (input.serviceType == 3)
            result += "Popup";
        else if (input.serviceType == 4)
            result += "html";
        else if (input.serviceType == 5)
            result += "Catelogy";
        else
            result += "Default";
        result += "</td>" +
                "  <td rid='" + position + "'>" + input.function + "</td>" +
                "  <td rid='" + position + "'>" + input.serviceID + "</td>" +
                "  <td rid='" + position + "'>" + input.textPopup + "</td>" +
                "  <td rid='" + position + "'>" + input.webUrl + "</td>" +
                "  <td rid='" + position + "'>" + input.cateId + "</td>" +
                "  <td rid='" + position + "'>" + input.serviceName + "</td>" +
                "  <td rid='" + position + "'>" + input.iconUrl + "</td>" +
                "  <td rid='" + position + "'>";
        if (input.status == 0)
            result += "New";
        else if (input.status == 1)
            result += "On";
        else if (input.status == 2)
            result += "Hot";
        else if (input.status == -1)
            result += "Off";
        else if (input.status == 3)
            result += "New_One_Time";
        else if (input.status == 4)
            result += "Hot_One_Time";
        result += "</td>" +

                "  <td rid='" + position + "'>";
        if (input.position == 0)
            result += "Nằm trên";
        else if (input.position == 1)
            result += "Nằm dưới";
        result += "</td>" +

                "  <td rid='" + position + "'>" + input.order + "</td>" +
                /*"  <td rid='" + position + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
                if (input.isStore)
                    result += " checked ";
                    result += ">" + "</td>" +*/
                "  <td rid='" + position + "'>" + input.buttonTitle + "</td>" +
                "</tr>";
        return result;
    }

    private void update(int comman) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = comman;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
    }
}
