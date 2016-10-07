
package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.ErrorCodeDb;
import com.mservice.momo.data.ServiceCategory;
import com.mservice.momo.data.TranErrConfDb;
import com.mservice.momo.data.m2mpromotion.MerchantPromosDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import java.util.ArrayList;
import java.util.ArrayList;

public class ErrorCodeController {
    private Vertx vertx;
    private EventBus eventBus;
    private Logger logger;
    private Container container;
    private ErrorCodeDb errorCodeDb;

//    public ErrorCodeController(Vertx vertx, Container container) {
//        this.vertx = vertx;
//        logger = container.logger();
//        errorCodeDb = new ErrorCodeDb(vertx.eventBus(), logger);
//    }
    public ErrorCodeController(final Vertx vertx, final Container container, JsonObject globalConfig) {
        this.vertx = vertx;
        this.container = container;
        errorCodeDb = new ErrorCodeDb(vertx.eventBus(),container.logger());

    }

    @Action(path = "/ErrCode/upsert")
    public void upSert(HttpRequestContext context, final Handler<Object> callback) {
        context.getRequest();
        MultiMap params = context.getRequest().params();


        final JsonObject joReply = new JsonObject();

        String error_code = params.get(colName.ErrorCodeMgtCols.ERROR_CODE);
        String description = params.get(colName.ErrorCodeMgtCols.DESCRIPTION);


        if("".equalsIgnoreCase(error_code)){
            joReply.putNumber("error",1);
            joReply.putString("desc","Vui lòng nhập nhập error code ");
            callback.handle(joReply);
            return;
        }

        ErrorCodeDb.Obj obj = new ErrorCodeDb.Obj();
        obj.errorCode = error_code;
        obj.description = description;



        errorCodeDb.upsertError(obj, new Handler<Boolean>() {
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


    @Action(path = "/ErrCode/getall")
    public void getAll(HttpRequestContext context, final Handler<Object> callback) {
        errorCodeDb.getAll(new Handler<ArrayList<ErrorCodeDb.Obj>> () {
            @Override
            public void handle(ArrayList<ErrorCodeDb.Obj> obj) {

                JsonArray jsonArray = new JsonArray();
                for (int i=0;i< obj.size();i++){
                    jsonArray.add(obj.get(i).toJsonObject());
                }
                callback.handle(jsonArray);
            }
        });
    }

    @Action(path = "/ErrCode/del")
    public void delete(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "delete service");
        final JsonObject result = new JsonObject();
        final String error_code = params.get(colName.ErrorCodeMgtCols.ERROR_CODE);
        log.add("error_code ", error_code);
        errorCodeDb.removeObj(error_code, new Handler<Boolean>() {
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



}
