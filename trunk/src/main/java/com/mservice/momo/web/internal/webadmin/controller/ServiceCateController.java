package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.ServiceCategory;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by nam on 10/6/14.
 */
public class ServiceCateController {
    private Vertx vertx;
    private Container container;
    private ServiceCategory serviceCategory;
    public ServiceCateController(final Vertx vertx, final Container container, JsonObject globalConfig) {
        this.vertx = vertx;
        this.container = container;
        serviceCategory = new ServiceCategory(vertx.eventBus(),container.logger());

    }

    @Action(path = "/servicecate/upsert")
    public void getAvailableTranType(HttpRequestContext context, final Handler<Object> callback) {
        context.getRequest();
        MultiMap params = context.getRequest().params();

        /*public static String id ="_id";
        public static String name ="name";
        public static String desc ="desc";
        public static String status ="stat";
        public static String lasttime ="ltime";*/

        final JsonObject joReply = new JsonObject();

        String id = params.get(colName.ServiceCatCols.id);
        String name = params.get(colName.ServiceCatCols.name);
        String desc = params.get(colName.ServiceCatCols.desc);
        String status = params.get(colName.ServiceCatCols.status);
        int order = params.get(colName.ServiceCatCols.order) == null?10000 : DataUtil.strToInt(params.get(colName.ServiceCatCols.order));
        String iconurl = params.get(colName.ServiceCatCols.iconurl)== null ? "" : params.get(colName.ServiceCatCols.iconurl);
        int star = params.get(colName.ServiceCatCols.star) == null ? 0 : DataUtil.strToInt(params.get(colName.ServiceCatCols.star));

        if("".equalsIgnoreCase(id)){
            joReply.putNumber("error",1);
            joReply.putString("desc","Vui lòng nhập nhập Category Id");
            callback.handle(joReply);
            return;
        }

        ServiceCategory.Obj obj = new ServiceCategory.Obj();
        obj.id = id;
        obj.name = name;
        obj.desc = desc;
        obj.status = status;
        obj.lasttime=System.currentTimeMillis();
        obj.order = order;
        obj.iconurl = iconurl;
        obj.star = star;

        serviceCategory.upsert(obj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if(aBoolean){
                    joReply.putNumber("error",0);
                    joReply.putString("desc","");
                    callback.handle(joReply);

                    //todo will do later if need
                    Common.ServiceReq serviceReq = new Common.ServiceReq();
                    serviceReq.Command =Common.ServiceReq.COMMAND.UPDATE_SERVICE_CATEGORY;
                    vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());

                }else{
                    joReply.putNumber("error",1);
                    joReply.putString("desc","Không thành công");
                    callback.handle(joReply);
                }
            }
        });
    }

    @Action(path = "/servicecate/getAll")
    public void getAll(HttpRequestContext context, final Handler<Object> callback) {
        serviceCategory.getAll(new Handler<ArrayList<ServiceCategory.Obj>> () {
            @Override
            public void handle(ArrayList<ServiceCategory.Obj> obj) {

                JsonArray jsonArray = new JsonArray();
                for (int i=0;i< obj.size();i++){
                    jsonArray.add(obj.get(i).toJson());
                }
                callback.handle(jsonArray);
            }
        });
    }

    @Action(path = "/servicecate/reload")
    public void reload(HttpRequestContext context, final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(container.logger());
        log.add("function", "reload servicecate");

        final JsonObject result = new JsonObject();

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command =Common.ServiceReq.COMMAND.UPDATE_SERVICE_CATEGORY;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());

        result.putNumber("error", 0);
        result.putString("desc", "");
        callback.handle(result);
    }
}
