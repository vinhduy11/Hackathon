package com.mservice.momo.vertx.binhtanpromotion;

import com.mservice.momo.data.binhtanpromotion.AcquireBinhTanUserPromotionDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by khoanguyen on 03/05/2016.
 */
public class BinhTanPromotionObj {
    public String phoneNumber = "";
    public JsonObject joExtra = new JsonObject();
    public String program = "";
    public String source = "";
    public BinhTanPromotionObj() {
    }

    public BinhTanPromotionObj(JsonObject jo) {
        phoneNumber = jo.getString(StringConstUtil.BinhTanPromotion.PHONE_NUMBER, "");
        joExtra = jo.getObject(StringConstUtil.BinhTanPromotion.EXTRA_KEY, new JsonObject());
        program = jo.getString(StringConstUtil.BinhTanPromotion.PROGRAM_KEY, "");
        source = jo.getString(StringConstUtil.BinhTanPromotion.SOURCE, "");
    }

    public static void requestAcquireBinhTanUserPromo(final Vertx vertx
            , String phoneNumber
            , String program
            , String source
            , JsonObject joExtra
            , final Handler<JsonObject> callback) {

        final BinhTanPromotionObj o = new BinhTanPromotionObj();
        o.phoneNumber = phoneNumber;
        o.joExtra = joExtra;
        o.program = program;
        o.source = source;
        vertx.eventBus().send(AppConstant.BINHTAN_PROMOTION_BUS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.BinhTanPromotion.PHONE_NUMBER, phoneNumber);
        jo.putObject(StringConstUtil.BinhTanPromotion.EXTRA_KEY, joExtra);
        jo.putString(StringConstUtil.BinhTanPromotion.PROGRAM_KEY, program);
        jo.putString(StringConstUtil.BinhTanPromotion.SOURCE, source);
        return jo;
    }
}
