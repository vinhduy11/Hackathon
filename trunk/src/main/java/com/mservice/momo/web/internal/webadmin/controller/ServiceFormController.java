package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.ServiceForm;
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
import org.vertx.java.platform.Container;

/**
 * Created by nam on 10/6/14.
 */
public class ServiceFormController {
    private Vertx vertx;
    private Container container;
    private ServiceForm serviceForm;
    public ServiceFormController(final Vertx vertx, final Container container, JsonObject globalConfig) {
        this.vertx = vertx;
        this.container = container;
        serviceForm = new ServiceForm(vertx.eventBus(),container.logger());

    }

    @Action(path = "/serviceform/upsert")
    public void getAvailableTranType(HttpRequestContext context, final Handler<Object> callback) {
        context.getRequest();
        MultiMap params = context.getRequest().params();

        /*public static String id="_id";
        public static String formnumber="frmnum";
        public static String name="name";
        public static String serviceid="sid";
        public static String servicename="sname";
        public static String caption="cap";
        public static String textbutton="txtbtn";
        public static String desc="desc";
        public static String lasttime="lasttime";*/

        final JsonObject joReply = new JsonObject();

        String id = params.get(colName.ServiceFormCols.id);
        int formnumber = DataUtil.strToInt(params.get(colName.ServiceFormCols.formnumber));
        String name = params.get(colName.ServiceFormCols.name);
        String serviceid = params.get(colName.ServiceFormCols.serviceid);
        String servicename = params.get(colName.ServiceFormCols.servicename);
        String caption = params.get(colName.ServiceFormCols.caption);
        String textbutton = params.get(colName.ServiceFormCols.textbutton);
        String desc = params.get(colName.ServiceFormCols.desc);
        long lasttime = System.currentTimeMillis();
        String guide = params.get(colName.ServiceFormCols.guide);

        ServiceForm.Obj obj = new ServiceForm.Obj();
        obj.id = id;
        obj.formnumber = formnumber;
        obj.name = name;
        obj.serviceid = serviceid;
        obj.servicename = servicename;
        obj.caption = caption;
        obj.textbutton=textbutton;
        obj.desc =desc;
        obj.lasttime = lasttime;
        obj.guide = guide;

        serviceForm.upsert(obj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if(aBoolean){
                    joReply.putNumber("error",0);
                    joReply.putString("desc","");
                    callback.handle(joReply);

                    //todo will do later if need
                    Common.ServiceReq serviceReq = new Common.ServiceReq();
                    serviceReq.Command =Common.ServiceReq.COMMAND.UPDATE_SERVICE_FORM;
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
        /*serviceCategory.getAll(new Handler<ArrayList<ServiceCategory.Obj>> () {
            @Override
            public void handle(ArrayList<ServiceCategory.Obj> obj) {

                JsonArray jsonArray = new JsonArray();
                for (int i=0;i< obj.size();i++){
                    jsonArray.add(obj.get(i).toJson());
                }
                callback.handle(jsonArray);
            }
        });*/
    }
}
