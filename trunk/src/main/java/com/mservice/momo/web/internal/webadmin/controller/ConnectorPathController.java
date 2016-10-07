package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.apache.axis.utils.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by tumegame on 22/01/2016.
 */
public class ConnectorPathController {
    private Vertx vertx;
    private Logger logger;
    private ConnectorHTTPPostPathDb connDb;

    public ConnectorPathController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        connDb = new ConnectorHTTPPostPathDb(vertx);
    }

    @Action(path = "/connectorpath/getall")
    public void getall(HttpRequestContext context, final Handler<JsonObject> callback) {

        connDb.getAll(new Handler<ArrayList<ConnectorHTTPPostPathDb.Obj>>() {
            @Override
            public void handle(ArrayList<ConnectorHTTPPostPathDb.Obj> objs) {
                JsonObject result = null;
                String temp = buildTable(objs);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);
                callback.handle(result);
            }
        });
    }

    @Action(path = "/connectorpath/del")
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

        final String id = params.get(colName.ConnectorHTTPPostPath.SERVICE_ID);

        log.add("id ", id);

        connDb.removeObj(id, new Handler<Boolean>() {
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

    @Action(path = "/connectorpath/upsert")
    public void upsert(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "upsert service");

        final JsonObject result = new JsonObject();

        ConnectorHTTPPostPathDb.Obj obj = new ConnectorHTTPPostPathDb.Obj();
        obj.serviceId = params.get(colName.ConnectorHTTPPostPath.SERVICE_ID);
        obj.path = params.get(colName.ConnectorHTTPPostPath.PATH);
        obj.host = params.get(colName.ConnectorHTTPPostPath.HOST);
        obj.port = DataUtil.strToInt(params.get(colName.ConnectorHTTPPostPath.PORT));

        log.add("params", obj.toJson());

        connDb.upsert(obj, new Handler<Boolean>() {
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

                //update(Common.ServiceReq.COMMAND.UPDATE_SERVICE);

                callback.handle(result);
                return;
            }
        });

    }


    @Action(path = "/connectorpath/search")
    public void search(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        final String id = params.get(colName.ConnectorHTTPPostPath.SERVICE_ID);
        connDb.search(id, new Handler<ArrayList<ConnectorHTTPPostPathDb.Obj>>() {
            @Override
            public void handle(ArrayList<ConnectorHTTPPostPathDb.Obj> reply) {
                JsonObject result = null;
                String temp = buildTable(reply);
                if (temp != null && !"".equalsIgnoreCase(temp))
                    result = new JsonObject().putString("table", temp);
                callback.handle(result);
            }
        });
    }

    @Action(path = "/connectorpath/export")
    public void getAllActive(HttpRequestContext context, final Handler<String> callback) {

        connDb.export(new Handler<String>() {
            @Override
            public void handle(String reply) {
                callback.handle(reply);
            }
        });
    }

    @Action(path = "/connectorpath/checkser")
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

        final String serviceId = params.get(colName.ConnectorHTTPPostPath.SERVICE_ID);

        if (StringUtils.isEmpty(serviceId)) {
            result.putNumber("error", 2);
            result.putString("desc", "Chua nhap service id");
            callback.handle(result);
            return;

        }

        connDb.findOne(serviceId, new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj objs) {
                if (objs != null && objs.serviceId != null) {
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


    public String buildTable(ArrayList<ConnectorHTTPPostPathDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th></th>" +
                "  <th>Service id</th>" +
                "  <th>Path</th>" +
                "  <th>Host</th>" +
                "  <th>Port</th>" +
                "</tr>";
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                result += getRowError(i, objs.get(i));
            }
        }
        result += "</table>";
        return result;
    }

    public String getRowError(int position, ConnectorHTTPPostPathDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td>" +
                "<button id= 'edit' id_ser = '" + input.serviceId + "'" + "val ='" + position + "'>Edit</button>" +
                "<button id = 'del' id_ser = '" + input.serviceId + "'" + "val ='" + position + "'>Del</button>" +
                "</td>" +
                "  <td rid='" + position + "'>" + input.serviceId + "</td>" +
                "  <td rid='" + position + "'>" + input.path + "</td>" +
                "  <td rid='" + position + "'>" + input.host + "</td>" +
                "  <td rid='" + position + "'>" + input.port + "</td>" +
                "</tr>";
        return result;
    }

   /* private void update(int comman) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = comman;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
    }*/

}
