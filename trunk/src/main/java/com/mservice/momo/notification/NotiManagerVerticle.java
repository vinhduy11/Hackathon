package com.mservice.momo.notification;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by concu on 9/19/16.
 */
public class NotiManagerVerticle extends Verticle {

    public static final int ADD = 1;
    public static final int DELETE = 2;
    private List<Integer> listNumber;

    @Override
    public void start() {
        final JsonObject globalConfig = container.config();
        final Logger logger = container.logger();
        listNumber = new ArrayList<>();

        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {

            }
        });
        vertx.eventBus().registerLocalHandler("", new Handler<Message>() {
            @Override
            public void handle(Message msg) {
                final Common.BuildLog log = new Common.BuildLog(logger);
                if(msg == null || msg.body() == null)
                {
                    log.add("desc: ","msg is null");
                    log.writeLog();
                    msg.reply(new JsonObject().putNumber(StringConstUtil.ERROR, -1).putString(StringConstUtil.DESCRIPTION, "Error"));
                    return;
                }
                JsonObject jReq = (JsonObject)msg.body();
                int cmd = jReq.getInteger(StringConstUtil.KEY, 0);
                int phoneNumber = jReq.getInteger(StringConstUtil.NUMBER, 0);
                log.add("cmd noti destination: " + this.getClass().getSimpleName(),cmd);
                log.add("phoneNumber noti destination: "+ this.getClass().getSimpleName(),phoneNumber);

                switch (cmd){
                    case ADD:
                        listNumber.add(phoneNumber);
                        break;
                    case DELETE:
                        listNumber.remove(phoneNumber);
                        break;
                    default:
                        log.add("desc " + this.getClass().getSimpleName(), "error cmd");
                        break;
                }
                log.writeLog();

            }
        });


    }

}
