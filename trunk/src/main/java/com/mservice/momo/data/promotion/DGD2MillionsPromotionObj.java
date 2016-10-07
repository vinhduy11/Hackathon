package com.mservice.momo.data.promotion;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 3/14/16.
 */
public class DGD2MillionsPromotionObj {
    public String phoneNumber = "";
    public long tranId = 0;
    public int group = 0;
    public String serviceId = "";
    public long amount = 0;
    public String billId = "";

    public DGD2MillionsPromotionObj(){}
    public DGD2MillionsPromotionObj(JsonObject jo) {
        phoneNumber = jo.getString(colName.DGD2MillionsPromotionTrackingCol.MOMO_PHONE, "");
        tranId = jo.getLong(colName.DGD2MillionsPromotionTrackingCol.TID_BILLPAY, 0);
        group = jo.getInteger(colName.DGD2MillionsPromotionTrackingCol.GROUP, 0);
        serviceId = jo.getString(colName.DGD2MillionsPromotionTrackingCol.SERVICE_ID, "");
        amount = jo.getLong(colName.DGD2MillionsPromotionTrackingCol.AMOUNT, 0);
        billId = jo.getString(colName.DGD2MillionsPromotionTrackingCol.BILL_ID, "");
    }

    public static void requestDgd2MillionsPromotion(final Vertx vertx
            , String billId
            , String phoneNumber
            , long tranId
            , int group
            , String serviceId
            , long amount
            , final Handler<JsonObject> callback) {

        final DGD2MillionsPromotionObj o = new DGD2MillionsPromotionObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.group = group;
        o.serviceId = serviceId;
        o.amount = amount;
        o.billId = billId;
        vertx.eventBus().send(AppConstant.OPEN_NEW_STORE_PROMOTION_BUS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(colName.DGD2MillionsPromotionTrackingCol.MOMO_PHONE, phoneNumber);
        jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.TID_BILLPAY, tranId);
        jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.GROUP, group);
        jo.putString(colName.DGD2MillionsPromotionTrackingCol.SERVICE_ID, serviceId);
        jo.putNumber(colName.DGD2MillionsPromotionTrackingCol.AMOUNT, amount);
        jo.putString(colName.DGD2MillionsPromotionTrackingCol.BILL_ID, billId);
        return jo;
    }
}
