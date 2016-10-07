package com.mservice.momo.data.promotion;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 4/25/16.
 */
public class CashBackPromotionObj {
    public String phoneNumber = "";
    public long tranId = 0;
    public String program = "";
    public String serviceId = "";
    public long amount = 0;
    public int rate = 0;
    public String bankAcc = "";
    public String cardInfo = "";
    public String deviceImei = "";
    public int tranType = 0;
    public JsonObject joExtra = new JsonObject();
    public CashBackPromotionObj(){}
    public CashBackPromotionObj(JsonObject jo) {
        phoneNumber = jo.getString(colName.CashBackCol.PHONE_NUMBER, "");
        tranId = jo.getLong(colName.CashBackCol.TRAN_ID, 0);
        program = jo.getString(colName.CashBackCol.PROGRAM, "");
        serviceId = jo.getString(colName.CashBackCol.SERVICE_ID, "");
        amount = jo.getLong(colName.CashBackCol.AMOUNT, 0);
        rate = jo.getInteger(colName.CashBackCol.RATE, 0);
        cardInfo = jo.getString(colName.CashBackCol.CARD_INFO, "");
        bankAcc = jo.getString(colName.CashBackCol.BANK_ACC, "");
        deviceImei = jo.getString(colName.CashBackCol.DEVICE_IMEI, "");
        tranType = jo.getInteger(colName.CashBackCol.TRAN_TYPE, 0);
        joExtra = jo.getObject(StringConstUtil.JO_EXTRA, new JsonObject());
    }

    public static void requestCashBackPromotion(final Vertx vertx
            , String program
            , String phoneNumber
            , long tranId
            , int rate
            , String serviceId
            , long amount
            , String bankAcc, String deviceImei, String cardInfo, int tranType, JsonObject joExtra
            , final Handler<JsonObject> callback) {

        final CashBackPromotionObj o = new CashBackPromotionObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.rate = rate;
        o.serviceId = serviceId;
        o.amount = amount;
        o.program = program;
        o.bankAcc = bankAcc;
        o.deviceImei = deviceImei;
        o.cardInfo = cardInfo;
        o.tranType = tranType;
        o.joExtra = joExtra;
        vertx.eventBus().send(AppConstant.CASH_BACK_PROMOTION_BUS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(colName.CashBackCol.PHONE_NUMBER, phoneNumber);
        jo.putNumber(colName.CashBackCol.TRAN_ID, tranId);
        jo.putString(colName.CashBackCol.PROGRAM, program);
        jo.putString(colName.CashBackCol.SERVICE_ID, serviceId);
        jo.putNumber(colName.CashBackCol.AMOUNT, amount);
        jo.putNumber(colName.CashBackCol.RATE, rate);
        jo.putString(colName.CashBackCol.CARD_INFO, cardInfo);
        jo.putString(colName.CashBackCol.BANK_ACC, bankAcc);
        jo.putString(colName.CashBackCol.DEVICE_IMEI, deviceImei);
        jo.putNumber(colName.CashBackCol.TRAN_TYPE, tranType);
        jo.putObject(StringConstUtil.JO_EXTRA, joExtra);
        return jo;
    }
}
