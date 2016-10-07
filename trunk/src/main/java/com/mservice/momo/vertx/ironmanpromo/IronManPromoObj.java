package com.mservice.momo.vertx.ironmanpromo;

import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 9/7/15.
 */
public class IronManPromoObj {

    //public static final String BILL_PAY_BUSS_ADDRESS = "BILL_PAY_BUSS_ADDRESS";

    public int tranType = 0;
    public long tranId = 0;
    public String phoneNumber = "";
    public String serviceId = "";
    public String source = "";
    public String cardCheckSum = "";
    public String cmnd = "";

    public IronManPromoObj() {
    }

    public IronManPromoObj(JsonObject jo) {
        phoneNumber = jo.getString(BillPayPromoConst.NUMBER, "");
        tranType = jo.getInteger(BillPayPromoConst.TRAN_TYPE, 0);
        tranId = jo.getLong(BillPayPromoConst.TRAN_ID, 0);
        serviceId = jo.getString(BillPayPromoConst.SERVICE_ID, "");
        source = jo.getString(BillPayPromoConst.SOURCE, "");
        cardCheckSum = jo.getString(BillPayPromoConst.CARD_CHECK_SUM, "");
        cmnd = jo.getString(BillPayPromoConst.CMND, "");

    }

    public static void requestIronManPromo(final Vertx vertx
            , String phoneNumber
            , int tranType
            , long tranId
            , String serviceId
            , String source
            , String cmnd
            , String cardCheckSum
            , final Handler<JsonObject> callback) {

        final IronManPromoObj o = new IronManPromoObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.tranType = tranType;
        o.serviceId = serviceId;
        o.source = source;
        o.cmnd = cmnd;
        o.cardCheckSum = cardCheckSum;

        vertx.eventBus().send(AppConstant.IRON_MAN_PROMO_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
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
        jo.putNumber(BillPayPromoConst.TRAN_TYPE, tranType);
        jo.putNumber(BillPayPromoConst.TRAN_ID, tranId);
        jo.putString(BillPayPromoConst.SERVICE_ID, serviceId);
        jo.putString(BillPayPromoConst.SOURCE, source);
        jo.putString(BillPayPromoConst.CARD_CHECK_SUM, cardCheckSum);
        jo.putString(BillPayPromoConst.CMND, cmnd);
        return jo;
    }
}
