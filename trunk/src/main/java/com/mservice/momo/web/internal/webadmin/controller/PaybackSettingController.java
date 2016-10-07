package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.CDHHPayBackSetting;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by nam on 10/6/14.
 */
public class PaybackSettingController {
    private Vertx vertx;
    private Container container;
    private CDHHPayBackSetting cdhhPayBackSetting;

    public PaybackSettingController(final Vertx vertx, final Container container, JsonObject globalConfig) {
        this.vertx = vertx;
        this.container = container;
        cdhhPayBackSetting = new CDHHPayBackSetting(vertx.eventBus(),container.logger());

    }

    @Action(path = "/cdhh_payback/upsert")
    public void getAvailableTranType(HttpRequestContext context, final Handler<Object> callback) {
        context.getRequest();
        MultiMap params = context.getRequest().params();
        String id = params.get("id").trim();
        String dur =params.get("dtime").trim();
        String stat =params.get("stat").trim();
        int delaytime = DataUtil.strToInt(dur);

        String serviceId = params.get("sid");

        boolean status = "on".equalsIgnoreCase(stat) ? true : false;
        CDHHPayBackSetting.Obj o = new CDHHPayBackSetting.Obj();
        o.id = id;
        o.delaytime =delaytime;
        o.status = status;
        o.paybackaccount = params.get("pbacc").trim();
        o.paybackmax = DataUtil.strToInt(params.get("pbmax"));
        o.serviceid = serviceId;

        cdhhPayBackSetting.saveOrUpdate(o, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if(aBoolean){
                    callback.handle("Thành công");

                    //request reload config
                    Common.ServiceReq serviceReq = new Common.ServiceReq();
                    serviceReq.Command =Common.ServiceReq.COMMAND.UPDATE_CDHH_PAYBACK;
                    vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
                }else{
                    callback.handle("Không thành công");
                }
            }
        });
    }

    @Action(path = "/cdhh_forcepayback/upsert")
    public void forcePayBack(HttpRequestContext context, final Handler<Object> callback) {
        context.getRequest();
        MultiMap params = context.getRequest().params();
        int inumber = DataUtil.strToInt(params.get("number").trim());
        String serviceId = params.get("serviceid");
        if(inumber == 0 ){
            callback.handle("Phone number không hợp lệ");
            return;
        }

        final String number ="0" + DataUtil.strToInt(params.get("number").trim());

        Misc.getPayBackCDHHSetting(vertx,serviceId, new Handler<CDHHPayBackSetting.Obj>() {
            @Override
            public void handle(CDHHPayBackSetting.Obj obj) {

                JsonObject joRequest = new JsonObject();
                joRequest.putString("number",number);
                joRequest.putNumber("amount", 10);
                joRequest.putNumber("tranid", System.currentTimeMillis());
                joRequest.putString("pbacc", obj.paybackaccount);
                joRequest.putNumber("dtime", 1);
                joRequest.putNumber("pbmax", obj.paybackmax);

                vertx.eventBus().send(AppConstant.PayBackCDHHVerticle_ADDRESS
                        ,joRequest, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        callback.handle(message.body().getString("desc"));
                    }
                });
            }
        });
    }

    @Action(path = "/cdhh_payback/getone")
    public void getOne(HttpRequestContext context, final Handler<Object> callback) {
        cdhhPayBackSetting.findOne(new Handler<CDHHPayBackSetting.Obj>() {
            @Override
            public void handle(CDHHPayBackSetting.Obj obj) {
                callback.handle(obj.toJson());
            }
        });
    }

    @Action(path = "/cdhh_payback/getall")
    public void getAll(HttpRequestContext context, final Handler<Object> callback) {
        cdhhPayBackSetting.findAll(new Handler<ArrayList<CDHHPayBackSetting.Obj>> () {
            @Override
            public void handle(ArrayList<CDHHPayBackSetting.Obj> arrayList) {

                JsonArray array = new JsonArray();

                for (int i =0; i< arrayList.size();i++){
                    JsonObject jo = arrayList.get(i).toJson();
                    boolean status = jo.getBoolean(colName.CDHHPayBackSettingCols.status, false);

                    jo.removeField(colName.CDHHPayBackSettingCols.status);
                    if(status){
                        jo.putString(colName.CDHHPayBackSettingCols.status,"on");
                    }else {
                        jo.putString(colName.CDHHPayBackSettingCols.status,"off");
                    }

                    array.add(jo);
                }
                callback.handle(array);
            }
        });
    }
}
