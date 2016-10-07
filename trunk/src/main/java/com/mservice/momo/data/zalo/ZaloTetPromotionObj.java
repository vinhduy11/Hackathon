package com.mservice.momo.data.zalo;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 1/25/16.
 */
public class ZaloTetPromotionObj {

    public String zalo_code = "";
    public String phoneNumber = "";
    public String source = "";
    public String last_imei = "";
    public long amount = 0;
    public long tid = 0;
    public String serviceId = "";
    public long promotionTime = 0;
    public long cashbackTime = 0;
    public ZaloTetPromotionObj() {
    }

    public ZaloTetPromotionObj(JsonObject jo) {
        phoneNumber = jo.getString(colName.ZaloTetPromotionCol.PHONE_NUMBER, "");
        zalo_code = jo.getString(colName.ZaloTetPromotionCol.ZALO_CODE, "");
        source = jo.getString(StringConstUtil.ZaloPromo.SOURCE, "");
        last_imei = jo.getString(colName.ZaloTetPromotionCol.DEVICE_IMEI, "");
        amount = jo.getLong(StringConstUtil.ZaloPromo.AMOUNT, 0);
        tid  = jo.getLong(StringConstUtil.ZaloPromo.TRAN_ID, 0);
        serviceId = jo.getString(StringConstUtil.ZaloPromo.SERVICE_ID, "");
        promotionTime = jo.getLong(StringConstUtil.ZaloPromo.PROMOTION_TIME, 0);
        cashbackTime =  jo.getLong(StringConstUtil.ZaloPromo.CASH_BACK_TIME, 0);
    };

    public static void requestZaloPromo(final Vertx vertx
            , String phoneNumber
            , String zaloCode
            , String source
            , String last_imei
            , long amount
            , long tid
            , String serviceId
            , long promotionTime
            , long cashbackTime
            , final Handler<JsonObject> callback) {

        final ZaloTetPromotionObj o = new ZaloTetPromotionObj();
        o.phoneNumber = phoneNumber;
        o.zalo_code = zaloCode;
        o.source = source;
        o.last_imei = last_imei;
        o.amount = amount;
        o.tid = tid;
        o.serviceId = serviceId;
        o.promotionTime = promotionTime;
        o.cashbackTime = cashbackTime;
        vertx.eventBus().send(AppConstant.ZALO_PROMOTION_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, phoneNumber);
        jo.putString(colName.ZaloTetPromotionCol.ZALO_CODE, zalo_code);
        jo.putString(StringConstUtil.ZaloPromo.SOURCE, source);
        jo.putString(colName.ZaloTetPromotionCol.DEVICE_IMEI, last_imei);
        jo.putNumber(StringConstUtil.ZaloPromo.AMOUNT, amount);
        jo.putNumber(StringConstUtil.ZaloPromo.TRAN_ID, tid);
        jo.putNumber(StringConstUtil.ZaloPromo.PROMOTION_TIME, promotionTime);
        jo.putNumber(StringConstUtil.ZaloPromo.CASH_BACK_TIME, cashbackTime);
        jo.putString(StringConstUtil.ZaloPromo.SERVICE_ID, serviceId);
        return jo;
    }
}
