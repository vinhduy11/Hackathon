package com.mservice.momo.data.referral;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 4/6/16.
 */
public class ReferralPromotionObj {
    public String phoneNumber = "";
    public String source = "";
    public String last_imei = "";
    public long amount = 0;
    public long tid = 0;
    public String serviceId = "";
    public JsonObject joExtra = new JsonObject();
    public ReferralPromotionObj() {
    }

    public ReferralPromotionObj(JsonObject jo) {
        phoneNumber = jo.getString(StringConstUtil.ReferralVOnePromoField.PHONE_NUMBER, "");
        source = jo.getString(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "");
        last_imei = jo.getString(StringConstUtil.ReferralVOnePromoField.DEVICE_IMEI, "");
        amount = jo.getLong(StringConstUtil.ReferralVOnePromoField.AMOUNT, 0);
        tid  = jo.getLong(StringConstUtil.ReferralVOnePromoField.TRAN_ID, 0);
        serviceId = jo.getString(StringConstUtil.ReferralVOnePromoField.SERVICE_ID, "");
        joExtra = jo.getObject(StringConstUtil.ReferralVOnePromoField.EXTRA, new JsonObject());
    };

    public static void requestReferralPromotion(final Vertx vertx
            , String phoneNumber
            , String source
            , String last_imei
            , long amount
            , long tid
            , String serviceId
            , JsonObject joExtra
            , final Handler<JsonObject> callback) {

        final ReferralPromotionObj o = new ReferralPromotionObj();
        o.phoneNumber = phoneNumber;
        o.source = source;
        o.last_imei = last_imei;
        o.amount = amount;
        o.tid = tid;
        o.serviceId = serviceId;
        o.joExtra = joExtra;
        vertx.eventBus().send(AppConstant.REFERRAL_PROMOTION_BUS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.ReferralVOnePromoField.PHONE_NUMBER, phoneNumber);
        jo.putString(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, source);
        jo.putString(StringConstUtil.ReferralVOnePromoField.DEVICE_IMEI, last_imei);
        jo.putNumber(StringConstUtil.ReferralVOnePromoField.AMOUNT, amount);
        jo.putNumber(StringConstUtil.ReferralVOnePromoField.TRAN_ID, tid);
        jo.putString(StringConstUtil.ReferralVOnePromoField.SERVICE_ID, serviceId);
        jo.putObject(StringConstUtil.ReferralVOnePromoField.EXTRA, joExtra);
        return jo;
    }
}
