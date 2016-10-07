package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.MinMaxTranDb;
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
 * Created by locnguyen on 25/08/2014.
 */
public class MinMaxTranController {

    private Vertx vertx;
    private Logger logger;

    private MinMaxTranDb minMaxTranDb;

    public MinMaxTranController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();

        minMaxTranDb = new MinMaxTranDb(vertx.eventBus(), logger);
    }

    @Action(path = "/minmaxtran/del")
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

        final String id = params.get(colName.MinMaxForTranCols.ROW_ID);

        log.add("id ", id);

        minMaxTranDb.removeObj(id, new Handler<Boolean>() {
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

                update(Common.ServiceReq.COMMAND.UPDATE_MINMAXTRAN);

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/minmaxtran/upsert")
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

        MinMaxTranDb.Obj obj = new MinMaxTranDb.Obj();
        obj.rowid = params.get(colName.ServicePackage.ID);
        obj.trantype = DataUtil.strToInt(params.get(colName.MinMaxForTranCols.TRAN_TYPE));
        obj.tranname = params.get(colName.MinMaxForTranCols.TRAN_NAME);
        obj.isnamed = Boolean.valueOf(params.get(colName.MinMaxForTranCols.IS_NAMED));
        obj.minvalue = DataUtil.stringToUNumber(params.get(colName.MinMaxForTranCols.MIN_VALUE));
        obj.maxvalue = DataUtil.stringToUNumber(params.get(colName.MinMaxForTranCols.MAX_VALUE));

        log.add("params", obj.toJsonObject());

        if (!obj.isInvalid()) {
            result.putNumber("error", -100);
            result.putString("desc", "input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        minMaxTranDb.update(obj, new Handler<Boolean>() {
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

                update(Common.ServiceReq.COMMAND.UPDATE_MINMAXTRAN);

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/minmaxtran/getall")
    public void getallPackagel(HttpRequestContext context, final Handler<JsonObject> callback) {
        minMaxTranDb.getlist(null, null, new Handler<ArrayList<MinMaxTranDb.Obj>>() {
            @Override
            public void handle(ArrayList<MinMaxTranDb.Obj> objs) {
                JsonObject result = null;

                String temp = buildTable(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);

                callback.handle(result);

            }
        });
    }

    public String buildTable(ArrayList<MinMaxTranDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Tran Type</th>" +
                "  <th>Tran Name</th>" +
                "  <th>Is Named</th>" +
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

    public String getRow(int position, MinMaxTranDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td><button id= 'edit' _id = '" + input.rowid + "'" + "val ='" + position + "'>Edit</button>" +
                "<button id= 'del' _id = '" + input.rowid + "'" + "val ='" + position + "'>Del</button>" +
                "</td>" +
                "  <td rid='" + position + "'>" + input.trantype + "</td>" +
                "  <td rid='" + position + "'>" + input.tranname + "</td>" +
                "  <td rid='" + position + "'>" + "<input style = 'width:20px; height:20px' type='checkbox' disabled";
        if (input.isnamed)
            result += " checked ";
        result += ">" + "</td>";

        result += "  <td rid='" + position + "'>" + input.minvalue + "</td>" +
                "  <td rid='" + position + "'>" + input.maxvalue + "</td>";


        result += "</tr>";
        return result;
    }

    private void update(int comman){
        Common.ServiceReq serviceReq = new Common.ServiceReq();

        serviceReq.Command = comman;

        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update,serviceReq.toJSON());
    }

}
