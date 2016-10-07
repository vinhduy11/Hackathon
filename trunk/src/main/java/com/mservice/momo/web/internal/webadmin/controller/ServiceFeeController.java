package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.ServiceFeeDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by concu on 10/3/15.
 */
public class ServiceFeeController {
    private Vertx vertx;
    private Logger logger;

    private ServiceFeeDb serviceFeeDb;

    public ServiceFeeController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();

        serviceFeeDb = new ServiceFeeDb(vertx, logger);
    }

    @Action(path = "/servicefee/del")
    public void delete(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "delete minmaxtran detail");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        final String id = params.get(colName.ServiceFeeDBCols.ID);

        log.add("id ", id);

        serviceFeeDb.removeObj(id, new Handler<Boolean>() {
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

                update(Common.ServiceReq.COMMAND.UPDATE_SERVICE_FEE);

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/servicefee/upsert")
    public void upsert(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = "";
        username = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(username)) {
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "upsert minmaxtran package");
        log.add("user", username);

        final JsonObject result = new JsonObject();

        ServiceFeeDb.Obj obj = new ServiceFeeDb.Obj();
        obj.KEY = params.get(colName.ServiceFeeDBCols.ID);
        obj.SERVICE_ID = params.get(colName.ServiceFeeDBCols.SERVICE_ID);
        obj.SERVICE_NAME = params.get(colName.ServiceFeeDBCols.SERVICE_NAME);
        //obj.CORE_BANK_AGENT = params.get(colName.FeeDBCols.CORE_BANK_AGENT);
        obj.TRANTYPE = DataUtil.strToInt(params.get(colName.ServiceFeeDBCols.TRANTYPE));
        obj.CHANNEL = DataUtil.strToInt(params.get(colName.ServiceFeeDBCols.CHANNEL));
        obj.INOUT_CITY = DataUtil.strToInt(params.get(colName.ServiceFeeDBCols.INOUT_CITY));
        obj.DYNAMIC_FEE = Double.valueOf(params.get(colName.ServiceFeeDBCols.DYNAMIC_FEE));
        obj.STATIC_FEE = DataUtil.strToInt(params.get(colName.ServiceFeeDBCols.STATIC_FEE));
        obj.FEE_TYPE = DataUtil.strToInt(params.get(colName.ServiceFeeDBCols.FEE_TYPE));
        obj.MIN_VALUE = DataUtil.stringToUNumber(params.get(colName.ServiceFeeDBCols.MIN_VALUE));
        obj.MAX_VALUE = DataUtil.stringToUNumber(params.get(colName.ServiceFeeDBCols.MAX_VALUE));

        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        serviceFeeDb.update(obj, new Handler<Boolean>() {
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

                update(Common.ServiceReq.COMMAND.UPDATE_SERVICE_FEE);

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/servicefee/getall")
    public void getallPackagel(HttpRequestContext context, final Handler<JsonObject> callback) {
        serviceFeeDb.getAll(new Handler<ArrayList<ServiceFeeDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceFeeDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTable(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);

                callback.handle(result);
            }
        });
    }

    public String buildTable(ArrayList<ServiceFeeDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Service ID</th>" +
                "  <th>Service Name</th>" +
                "  <th>Tran Type</th>" +
                "  <th>Channel</th>" +
                "  <th>InOut City</th>" +
                "  <th>Dynamic Fee</th>" +
                "  <th>Static Fee</th>" +
                "  <th>Fee Type</th>" +
                "  <th>Min Value</th>" +
                "  <th>Max Value</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += getRow(i, objs.get(i));
            }
        }
        result += "</table>";
        return result;
    }

    public String getRow(int position, ServiceFeeDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td><button id= 'edit' _id = '" + input.KEY + "'" + "val ='" + position + "'>Edit</button>" +
                "<button id= 'del' _id = '" + input.KEY + "'" + "val ='" + position + "'>Del</button>" +
                "</td>" +
                "  <td rid='" + position + "'>" + input.SERVICE_ID + "</td>" +
                "  <td rid='" + position + "'>" + input.SERVICE_NAME + "</td>";
//                "  <td rid='" + position + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
//
//        if (input.isnamed)
//            result += " checked ";
//        result += ">" + "</td>";

        result += "  <td rid='" + position + "'>" + input.TRANTYPE + "</td>" +
                "  <td rid='" + position + "'>" + input.CHANNEL + "</td>" +
                "  <td rid='" + position + "'>" + input.INOUT_CITY + "</td>" +
                "  <td rid='" + position + "'>" + input.DYNAMIC_FEE + "</td>" +
                "  <td rid='" + position + "'>" + input.STATIC_FEE + "</td>" +
                "  <td rid='" + position + "'>" + input.FEE_TYPE + "</td>" +
                "  <td rid='" + position + "'>" + input.MIN_VALUE + "</td>" +
                "  <td rid='" + position + "'>" + input.MAX_VALUE + "</td>";


        result += "</tr>";
        return result;
    }

    private void update(int comman){
        Common.ServiceReq serviceReq = new Common.ServiceReq();

        serviceReq.Command = comman;

        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update,serviceReq.toJSON());
    }
}
