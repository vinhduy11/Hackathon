package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.PgGlxDb;
import com.mservice.momo.util.DataUtil;
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
import java.util.ArrayList;

public class PgController {
    private Vertx vertx;
    private Logger logger;
    private PgGlxDb pgGlxDb;

    public PgController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        pgGlxDb = new PgGlxDb(vertx.eventBus(), logger);
    }

    @Action(path = "/pghist/get")
    public void getPg(HttpRequestContext context, final Handler<Object> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/pghist/get");

        MultiMap params = context.getRequest().params();

        String pgcode = params.get("pgcode") == null ? "" : params.get("pgcode").trim();
        String pgnum = params.get("pgnum") == null ? "" : params.get("pgnum").trim();
        String fromdate = params.get("fromdate") == null ? "" : params.get("fromdate").trim();
        String todate = params.get("todate") == null ? "" : params.get("todate").trim();

        String[] tempFromdate = fromdate.split(",");
        String[] tempTodate = todate.split(",");

        String strFdate = tempFromdate.length> 1 ? tempFromdate[0].trim() + " " + tempFromdate[1].trim()
                :  tempFromdate[0].trim()  + " 00:00:00";

        String strTdate = tempTodate.length> 1 ? tempTodate[0].trim() + " " + tempTodate[1].trim()
                :  tempTodate[0].trim()  + " 00:00:00";

        long fdate = Misc.getDateAsLong(strFdate, "dd/MM/yyyy HH:mm:ss", logger, "");

        long tdate = Misc.getDateAsLong(strTdate, "dd/MM/yyyy HH:mm:ss", logger, "");

        log.add("fromdate",fdate);
        log.add("todate",tdate);

        pgnum = DataUtil.stringToUNumber(pgnum)+"";

        String user = WebAdminController.checkSession(context, null);
        if ("".equalsIgnoreCase(user)){
            callback.handle(
                    (new JsonArray()).add(new JsonObject()
                            .putString("error", "-1")
                            .putString("desc", "Must login"))
            );
            return;
        }

        pgGlxDb.get(pgcode, pgnum, fdate, tdate, new Handler<ArrayList<PgGlxDb.Obj>>() {
            @Override
            public void handle(ArrayList<PgGlxDb.Obj> event) {
                JsonArray arrList = new JsonArray();
                for (PgGlxDb.Obj obj : event) {
                    JsonObject jo = obj.toJson();
                    jo.putString("amount",Misc.formatAmount(obj.value));
                    arrList.add(jo);
                }
                log.writeLog();
                callback.handle(arrList);
            }
        });
    }
}