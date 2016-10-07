package com.mservice.momo.data.customercaregiftgroup;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/24/15.
 */
public class DollarHeartCustomerCareGiftGroupObj {

    //public static final String BILL_PAY_BUSS_ADDRESS = "BILL_PAY_BUSS_ADDRESS";

    public String phoneNumber = "";
    public String agent = "";
    public String giftTypeId = "";
    public long giftValue = 0;
    public int duration = 0;
    public String program = "";
    public String group = "";

    public DollarHeartCustomerCareGiftGroupObj() {
    }

    public DollarHeartCustomerCareGiftGroupObj(JsonObject jo) {
        phoneNumber = jo.getString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.PHONE_NUMBER, "");
        agent = jo.getString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.AGENT, "");
        giftTypeId = jo.getString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.GIFT_TYPE_ID, "");
        giftValue = jo.getLong(StringConstUtil.DollarHeartCustomerCareGiftGroupString.GIFT_VALUE, 0);
        duration = jo.getInteger(StringConstUtil.DollarHeartCustomerCareGiftGroupString.DURATION, 0);
        program = jo.getString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.PROGRAM, "");
        group = jo.getString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.GROUP, "");


    }

    public static void requestCustomerCareGiftGroup(final Vertx vertx
            , String phoneNumber
            , String agent
            , String giftTypeId, long giftValue, int duration, String program, String group
            , final Handler<JsonObject> callback) {

        final DollarHeartCustomerCareGiftGroupObj o = new DollarHeartCustomerCareGiftGroupObj();
        o.phoneNumber = phoneNumber;
        o.agent = agent;
        o.giftTypeId = giftTypeId;
        o.giftValue = giftValue;
        o.duration = duration;
        o.program = program;
        o.group = group;
        vertx.eventBus().send(AppConstant.DOLLAR_HEART_CUSTOMER_CARE_GROUP, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.PHONE_NUMBER, phoneNumber);
        jo.putString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.AGENT, agent);
        jo.putString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.GIFT_TYPE_ID, giftTypeId);
        jo.putNumber(StringConstUtil.DollarHeartCustomerCareGiftGroupString.GIFT_VALUE, giftValue);
        jo.putNumber(StringConstUtil.DollarHeartCustomerCareGiftGroupString.DURATION, duration);
        jo.putString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.PROGRAM, program);
        jo.putString(StringConstUtil.DollarHeartCustomerCareGiftGroupString.GROUP, group);
        return jo;
    }
}
