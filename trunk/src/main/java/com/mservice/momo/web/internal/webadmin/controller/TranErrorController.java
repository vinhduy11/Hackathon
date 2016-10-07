package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.TranErrConfDb;
import com.mservice.momo.util.DataUtil;
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
 * Created by locnguyen on 26/07/2014.
 */
public class TranErrorController {
    private Vertx vertx;

    private Logger logger;

    private TranErrConfDb tranErrConfDb;

    public TranErrorController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        tranErrConfDb = new TranErrConfDb(vertx.eventBus(), logger);
    }

    @Action(path = "/tranerr/upsert")

    public void upsert(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        String username = WebAdminController.checkSession(context,callback);
        if ("".equalsIgnoreCase(username)){
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "mLogger");
        log.add("user",username);

        final JsonObject result = new JsonObject();

        TranErrConfDb.Obj obj = new TranErrConfDb.Obj();
        obj.ID = params.get("id_err");
        obj.errorCode = DataUtil.strToInt(params.get("err_code"));
        obj.tranType = DataUtil.strToInt(params.get("tran_type"));
        obj.tranName = params.get("tran_name");
        obj.desciption = params.get("desc");
        obj.contentTranHis = params.get("content");
        obj.notiTitle = params.get("noti_title");
        obj.notiBody = params.get("noti_body");

        log.add("params",obj.toJsonObject());

        if (!obj.isInvalid()){
            result.putNumber("error",-100);
            result.putString("desc","input invalid");
            log.writeLog();

            callback.handle(result);
            return;
        }

        tranErrConfDb.upsertError(obj.ID,obj,new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (aBoolean == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "upsert failed");
                }
                else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }
                log.writeLog();

                callback.handle(result);
                return;
            }
        });

    }

    @Action(path = "/tranerr/getall")
    public void getall(HttpRequestContext context, final Handler<JsonObject> callback) {

//        long id = -1;
//        if ((id = WebAdminController.checkSession(context,callback))<0){
//            return;
//        }

//        final CoreCommon.BuildLog log = new CoreCommon.BuildLog(logger);
//        log.add("function", "getall transaction error");
//        log.add("user",WebAdminController.listUser.get(id).username);

        tranErrConfDb.getListError(-1, -1, 1, 1000, new Handler<ArrayList<TranErrConfDb.Obj>>() {
            @Override
            public void handle(ArrayList<TranErrConfDb.Obj> objs) {

                JsonObject result = null;

                if (objs != null) {
                    String temp = buildTable(objs);
                    if (temp != null && !"".equalsIgnoreCase(temp))
                        result = new JsonObject().putString("table", temp);

                }

//                log.writeLog();

                callback.handle(result);
            };
        });
    }

    public String buildTable(ArrayList<TranErrConfDb.Obj> objs) {
        String result = "<table>";

        result += "<tr>\n" +
                "  <th>Error Code</th>" +
                "  <th>Tran Type</th>" +
                "  <th>Tran Name</th>" +
                "  <th>Description</th>" +
                "  <th>Content TranHis</th>" +
                "  <th>Noti Title</th>" +
                "  <th>Noti Body</th>" +
                "  <th></th>" +
                "</tr>";
        for (int i = 0; i < objs.size(); i++) {
            result += getRowError(i,objs.get(i));
        }
        result += "</table>";
        return result;
    }

    public String getRowError(int position,TranErrConfDb.Obj input) {
        String result = "";

        result += "<tr>\n" +
                "  <td rid='"+position+"'>" + input.errorCode + "</td>" +
                "  <td rid='"+position+"'>" + input.tranType + "</td>" +
                "  <td rid='"+position+"'>" + input.tranName + "</td>" +
                "  <td rid='"+position+"' width='300px'>" + input.desciption + "</td>" +
                "  <td rid='"+position+"' width='300px'>" + input.contentTranHis + "</td>" +
                "  <td rid='"+position+"' width='300px'>" + input.notiTitle + "</td>" +
                "  <td rid='"+position+"' width='300px'>" + input.notiBody + "</td>" +
                "  <td><button id_err = '" + input.ID + "'" +"val ='"+position+ "'>Edit</button></td>" +
                "</tr>";
        return result;
    }

}
