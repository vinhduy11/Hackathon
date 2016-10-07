package com.mservice.momo.vertx.customercare;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by anhkhoa on 01/04/2015.
 */
public class CustomCareObj {
    public static final String CUSTOMER_CARE_BUSS_ADDRESS = "CUSTOMER_CARE_BUSS_ADDRESS";
    public String phoneNumber;
    public int tranType = 0;
    public long tranId;

    public CustomCareObj() {
    }

    ;

    public CustomCareObj(JsonObject jo) {
        phoneNumber = jo.getString("number", "");
        tranType = jo.getInteger("trantype", 0);
        tranId = jo.getLong("tranid", 0);
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString("number", phoneNumber);
        jo.putNumber("trantype", tranType);
        jo.putNumber("tranid", tranId);
        return jo;
    }

    public static void requestCustomCarePromo(Vertx vertx
                            ,String phoneNumber
                            ,int tranType
                            ,long tranId
                            ,final Handler<JsonObject> callback){

        CustomCareObj o = new CustomCareObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.tranType = tranType;
        vertx.eventBus().send(CUSTOMER_CARE_BUSS_ADDRESS, o.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }
}
