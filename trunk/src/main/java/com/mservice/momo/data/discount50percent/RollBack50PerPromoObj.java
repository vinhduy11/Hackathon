package com.mservice.momo.data.discount50percent;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/3/15.
 */
public class RollBack50PerPromoObj {
    //public static final String BILL_PAY_BUSS_ADDRESS = "BILL_PAY_BUSS_ADDRESS";

    public int tranType = 0;
    public long tranId = 0;
    public String phoneNumber = "";
    public String source = "";
    public String cmnd = "";
    public long bank_tid = 0;
    public long bank_amount = 0;
    public String bank_code = "";
    public long tran_amount = 0;
    public String serviceId = "";
    public int appCode = 0;
    public RollBack50PerPromoObj() {
    }

    public RollBack50PerPromoObj(JsonObject jo) {
        phoneNumber = jo.getString(StringConstUtil.RollBack50Percent.PHONE_NUMBER, "");
        tranType = jo.getInteger(StringConstUtil.RollBack50Percent.TRAN_TYPE, 0);
        tranId = jo.getLong(StringConstUtil.RollBack50Percent.TRAN_ID, 0);
        source = jo.getString(StringConstUtil.RollBack50Percent.SOURCE, "");
        cmnd = jo.getString(StringConstUtil.RollBack50Percent.CMND, "");
        bank_tid = jo.getLong(StringConstUtil.RollBack50Percent.BANK_TID, 0);
        bank_amount = jo.getLong(StringConstUtil.RollBack50Percent.BANK_AMOUNT, 0);
        bank_code = jo.getString(StringConstUtil.RollBack50Percent.BANK_CODE, "");
        serviceId = jo.getString(StringConstUtil.RollBack50Percent.SERVICE_ID, "");
        tran_amount = jo.getLong(StringConstUtil.RollBack50Percent.TRAN_AMOUNT, 0);
        appCode = jo.getInteger(StringConstUtil.RollBack50Percent.APP_CODE, 0);
    }

    public static void requestRollBack50PerPromo(final Vertx vertx
            , String phoneNumber
            , int tranType
            , long tranId
            , String source
            , String cmnd
            , long tran_amount
            , String bank_code
            , long bank_amount
            , long bank_tid
            , String serviceId
            , int appCode
            , final Handler<JsonObject> callback) {

        final RollBack50PerPromoObj o = new RollBack50PerPromoObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.tranType = tranType;
        o.source = source;
        o.cmnd = cmnd;
        o.bank_amount = bank_amount;
        o.bank_tid = bank_tid;
        o.bank_code = bank_code;
        o.tran_amount = tran_amount;
        o.serviceId = serviceId;
        o.appCode = appCode;
        vertx.eventBus().send(AppConstant.ROLLBACK_50PERCENT_PROMO_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.RollBack50Percent.PHONE_NUMBER, phoneNumber);
        jo.putNumber(StringConstUtil.RollBack50Percent.TRAN_TYPE, tranType);
        jo.putNumber(StringConstUtil.RollBack50Percent.TRAN_ID, tranId);
        jo.putString(StringConstUtil.RollBack50Percent.SOURCE, source);
        jo.putString(StringConstUtil.RollBack50Percent.CMND, cmnd);
        jo.putString(StringConstUtil.RollBack50Percent.SERVICE_ID, serviceId);
        jo.putString(StringConstUtil.RollBack50Percent.BANK_CODE, bank_code);
        jo.putNumber(StringConstUtil.RollBack50Percent.BANK_AMOUNT, bank_amount);
        jo.putNumber(StringConstUtil.RollBack50Percent.BANK_TID, bank_tid);
        jo.putNumber(StringConstUtil.RollBack50Percent.TRAN_AMOUNT, tran_amount);
        jo.putNumber(StringConstUtil.RollBack50Percent.APP_CODE, appCode);
        return jo;
    }
}
