package com.mservice.momo.data.codeclaim;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.promotion_server.PromotionObj;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by manhly on 12/07/2016.
 */
public class TestPromoVerticle extends Verticle {
    @Override
    public void start() {
//        super.start();


        vertx.setTimer(10000, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                final PromotionObj o = new PromotionObj();
                o.phoneNumber = "0906399238";
                o.serviceId = "vic1_promo";
                o.amount = 10000;
                o.tranType = 1;
                o.joExtra = new JsonObject();

                vertx.eventBus().send(AppConstant.VIC_PROMOTION_BUS_ADDRESS, o.toPromotionJsonObject(), new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {



                    }
                });

            }
        });


    }
}
