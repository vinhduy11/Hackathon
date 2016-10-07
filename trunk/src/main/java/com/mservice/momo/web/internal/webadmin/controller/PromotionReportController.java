package com.mservice.momo.web.internal.webadmin.controller;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by manhly on 26/07/2016.
 */
public class PromotionReportController {
    private Vertx vertx;
    private EventBus eventBus;
    private Logger logger;
    private Container container;
    private PhonesDb phonesDb;

    public PromotionReportController(final Vertx vertx, final Container container, JsonObject globalConfig) {
        this.vertx = vertx;
        this.container = container;
        phonesDb = new PhonesDb(vertx.eventBus(),container.logger());

    }
    private void response(HttpServerRequest request, String strResult) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "text/plain ; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(strResult);
        request.response().end();
    }
    @Action(path = "/getCreateByNumber")
    public void getCreateByNumber(HttpRequestContext context, final Handler<JsonObject> callback) {
        final HttpServerRequest request = context.getRequest();
        MultiMap params = request.params();
        final JsonObject joReply = new JsonObject();
        final JsonObject jo = new JsonObject();
        final String number1 = params.get("number");
        if("".equalsIgnoreCase(number1)){
            joReply.putNumber("error",1);
            joReply.putString("desc","Vui lòng nhập nhập SDT !");
            callback.handle(joReply);
            return;
        }
        int number =  Integer.parseInt(number1);
        phonesDb.getPhoneObjInfo(number, new Handler<PhonesDb.Obj>()  {
            @Override
            public void handle(PhonesDb.Obj obj) {
                String create_date="";
                JsonArray array = new JsonArray();
                if (obj != null ) {


                        JsonObject jo = obj.toJsonObject();
                        array.add(jo);
                    Long create_date1 = obj.createdDate;
                    create_date = Misc.dateVNFormatWithTime(create_date1);

                }
                response(request,create_date);
                return;


            }


        });
    }
}
