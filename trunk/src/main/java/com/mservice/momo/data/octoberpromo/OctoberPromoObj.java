package com.mservice.momo.data.octoberpromo;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 10/15/15.
 */
public class OctoberPromoObj {

    //public static final String BILL_PAY_BUSS_ADDRESS = "BILL_PAY_BUSS_ADDRESS";

    public int tranType = 0;
    public long tranId = 0;
    public String phoneNumber = "";
    public String source = "";
    public String cardCheckSum = "";
    public String cmnd = "";

    public OctoberPromoObj() {
    }

    public OctoberPromoObj(JsonObject jo) {
        phoneNumber = jo.getString(StringConstUtil.OctoberPromoProgram.PHONE_NUMBER, "");
        tranType = jo.getInteger(StringConstUtil.OctoberPromoProgram.TRAN_TYPE, 0);
        tranId = jo.getLong(StringConstUtil.OctoberPromoProgram.TRAN_ID, 0);
        source = jo.getString(StringConstUtil.OctoberPromoProgram.SOURCE, "");
        cardCheckSum = jo.getString(StringConstUtil.OctoberPromoProgram.CARD_CHECK_SUM, "");
        cmnd = jo.getString(StringConstUtil.OctoberPromoProgram.CMND, "");

    }

    public static void requestOctoberPromo(final Vertx vertx
            , String phoneNumber
            , int tranType
            , long tranId
            , String source
            , String cmnd
            , String cardCheckSum
            , final Handler<JsonObject> callback) {

        final OctoberPromoObj o = new OctoberPromoObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.tranType = tranType;
        o.source = source;
        o.cmnd = cmnd;
        o.cardCheckSum = cardCheckSum;

        vertx.eventBus().send(AppConstant.OCTOBER_PROMO_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.OctoberPromoProgram.PHONE_NUMBER, phoneNumber);
        jo.putNumber(StringConstUtil.OctoberPromoProgram.TRAN_TYPE, tranType);
        jo.putNumber(StringConstUtil.OctoberPromoProgram.TRAN_ID, tranId);
        jo.putString(StringConstUtil.OctoberPromoProgram.SOURCE, source);
        jo.putString(StringConstUtil.OctoberPromoProgram.CARD_CHECK_SUM, cardCheckSum);
        jo.putString(StringConstUtil.OctoberPromoProgram.CMND, cmnd);
        return jo;
    }
}
