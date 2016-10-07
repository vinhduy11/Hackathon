package com.mservice.momo.vertx.visampointpromo;

import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 5/21/15.
 */
public class VisaMpointPromoObj {


    //public static final String VISA_MPOINT_BUSS_ADDRESS = "VISA_MPOINT_BUSS_ADDRESS";

    public int tranType = 0;
    public long tranId = 0;
    public String phoneNumber = "";
    public String cardnumber = "";
    public long visaAmount = 0;
    public long visatranId = 0;

    //add them
    public String serviceId = "";
    public long totalAmount = 0;

    //add them
    public long cashinTime = 0;

    public VisaMpointPromoObj() {
    }

    public VisaMpointPromoObj(JsonObject jo) {
        phoneNumber = jo.getString(VisaMpointPromoConst.NUMBER, "");
        tranType = jo.getInteger(VisaMpointPromoConst.TRANTYPE, 0);
        tranId = jo.getLong(VisaMpointPromoConst.TID, 0);
        cardnumber = jo.getString(VisaMpointPromoConst.CARD_NUMBER, "");
        visaAmount = jo.getLong(VisaMpointPromoConst.VISA_AMOUNT, 0);
        visatranId = jo.getLong(VisaMpointPromoConst.VISA_TRAN_ID, 0);

        serviceId = jo.getString(VisaMpointPromoConst.SERVICE_ID, "");
        totalAmount = jo.getLong(VisaMpointPromoConst.TOTAL_AMOUNT, 0);

        cashinTime = jo.getLong(VisaMpointPromoConst.CASHIN_TIME, 0);


    }

    public static void requestVisaMpointPromo(Vertx vertx
            , String phoneNumber
            , int tranType
            , long tranId
            , String cardnumber
            , long visaAmount
            , long visatranId
            , String serviceId
            , long totalAmount
            , long cashinTime
            , final Handler<JsonObject> callback) {

        VisaMpointPromoObj o = new VisaMpointPromoObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.tranType = tranType;
        o.cardnumber = cardnumber;
        o.visaAmount = visaAmount;
        o.visatranId = visatranId;
        o.serviceId = serviceId;
        o.totalAmount = totalAmount;
        o.cashinTime = cashinTime;
        vertx.eventBus().send(AppConstant.VISA_MPOINT_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(BillPayPromoConst.NUMBER, phoneNumber);
        jo.putNumber(VisaMpointPromoConst.TRANTYPE, tranType);
        jo.putNumber(VisaMpointPromoConst.TID, tranId);
        jo.putString(VisaMpointPromoConst.CARD_NUMBER, cardnumber);
        jo.putNumber(VisaMpointPromoConst.VISA_AMOUNT, visaAmount);
        jo.putNumber(VisaMpointPromoConst.VISA_TRAN_ID, visatranId);

        jo.putString(VisaMpointPromoConst.SERVICE_ID, serviceId);
        jo.putNumber(VisaMpointPromoConst.TOTAL_AMOUNT, totalAmount);
        jo.putNumber(VisaMpointPromoConst.CASHIN_TIME, cashinTime);
        return jo;
    }
}
