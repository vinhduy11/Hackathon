package com.mservice.momo.vertx.ironmanpromo;

import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 9/19/15.
 */
public class PreIronManPromoObj {

    //public static final String BILL_PAY_BUSS_ADDRESS = "BILL_PAY_BUSS_ADDRESS";

    public String phoneNumber = "";
    public String source = "";

    public PreIronManPromoObj() {
    }

    public PreIronManPromoObj(JsonObject jo) {
        phoneNumber = jo.getString(BillPayPromoConst.NUMBER, "");
        source = jo.getString(BillPayPromoConst.SOURCE, "");


    }

    public static void requestIronManPromo(final Vertx vertx
            , String phoneNumber
            , String source
            , final Handler<JsonObject> callback) {

        final IronManPromoObj o = new IronManPromoObj();
        o.phoneNumber = phoneNumber;
        o.source = source;

        vertx.eventBus().send(AppConstant.PRE_IRON_MAN_PROMO_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(BillPayPromoConst.NUMBER, phoneNumber);
        jo.putString(BillPayPromoConst.SOURCE, source);

        return jo;
    }
}
