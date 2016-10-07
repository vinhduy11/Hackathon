package com.mservice.momo.web.internal.services;

import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.HttpResponseCommon;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by concu on 10/3/14.
 */
public class ControllerWarningRetailer {
    private Vertx vertx;
    private Logger logger;
    private JsonObject glbCfg;

    public ControllerWarningRetailer(Vertx vertx, Container container){
        this.vertx = vertx;
        this.glbCfg = container.config();
        this.logger = container.logger();
    }


    @Action(path = "/agent/warning")
    public void agentWarning(final HttpRequestContext context
            ,final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        final JsonObject jo = new JsonObject();

        HttpServerRequest request = context.getRequest();
        MultiMap params = request.params();
        String data = "";

        if ("GET".equalsIgnoreCase(request.method())){
            data = params.get("data");
        } else {
            data = context.postData;
        }

        /*{
            "caption": "Tiêu đề mất thanh khoản hay cảnh báo mất thanh khoản",
           "number":"0974540385",
           "sms":"noi dung sms",
           "body":"Nội dung body cua notification"
        }*/

        jo.putString("status","success");
        log.add("function","/agent/warning");
        log.add("data",data);
        log.writeLog();

        JsonObject joData = new JsonObject(data);

        String agentPhone = joData.getString("number","");
        String sms = joData.getString("sms","");
        String notiBody = joData.getString("body","");
        String caption = joData.getString("caption","");

        int number =DataUtil.strToInt(DataUtil.stringToVnPhoneNumber(agentPhone)+"");

        boolean valid = true;
        String errDesc = "";
        if(number <= 0){
            errDesc = "invalid number : " + agentPhone;
            valid = false;
        }

        if("".equalsIgnoreCase(sms)){
            errDesc += ", sms is empty.";
            valid =false;
        }

        if("".equalsIgnoreCase(notiBody)){
            errDesc += ", body is empty";
            valid =false;
        }

        if("".equalsIgnoreCase(caption)){
            errDesc += ", caption is empty";
            valid =false;
        }

        if(!valid){
            jo.putString("status","failed");
            jo.putString("desc",errDesc);
        }

        HttpResponseCommon.response(context.getRequest(),jo);
        if(!valid){return;}

        //build noti and send to client
        Notification noti = new Notification();
        noti.receiverNumber = number;
        noti.priority = 1; //1. send data, 2. send sms
        noti.sms= sms;
        noti.cmdId = System.currentTimeMillis();
        noti.body = notiBody;
        noti.bodyIOS = notiBody;
        noti.time = System.currentTimeMillis();
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.status = Notification.STATUS_DETAIL;
        noti.extra= new JsonObject().toString();
        noti.caption =caption;
        noti.category = 0;
        noti.tranId =System.currentTimeMillis();

        Misc.sendNoti(vertx,noti);
    }
}
