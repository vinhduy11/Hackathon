package com.mservice.momo.data.codeclaim;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 2/23/16.
 */
public class ClaimCodePromotionObj {
    public String claimed_code = "";
    public String phoneNumber = "";
    public String source = "";
    public String last_imei = "";
    public long amount = 0;
    public long tid = 0;
    public String serviceId = "";
    public long promotionTime = 0;
    public int command = 0; // 1: load, 2: update, 3:reload
    public JsonObject joExtra = new JsonObject();

    public ClaimCodePromotionObj() {
    }

    public ClaimCodePromotionObj(JsonObject jo) {
        phoneNumber = jo.getString(StringConstUtil.ClaimCodePromotion.PHONE_NUMBER, "");
        claimed_code = jo.getString(StringConstUtil.ClaimCodePromotion.CODE, "");
        source = jo.getString(StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM, "");
        last_imei = jo.getString(StringConstUtil.ClaimCodePromotion.DEVICE_IMEI, "");
        amount = jo.getLong(StringConstUtil.ClaimCodePromotion.AMOUNT, 0);
        tid  = jo.getLong(StringConstUtil.ClaimCodePromotion.TRAN_ID, 0);
        serviceId = jo.getString(StringConstUtil.ClaimCodePromotion.SERVICE_ID, "");
        promotionTime = jo.getLong(StringConstUtil.ClaimCodePromotion.PROMOTION_TIME, 0);
        command = jo.getInteger(StringConstUtil.ClaimCodePromotion.COMMAND, 0);
        joExtra = jo.getObject(StringConstUtil.ClaimCodePromotion.EXTRA, new JsonObject());
    };

    /**
     * Kiem tra chuong trinh khuyen mai thong qua goi ClaimCodePromotionVerticle
     * va tra thuong tai ClaimCodePromotionVerticle
     *
     * @param vertx
     * @param phoneNumber
     * @param claimed_code
     * @param source
     * @param last_imei
     * @param amount
     * @param tid
     * @param serviceId
     * @param promotionTime
     * @param joExtra
     * @param callback
     */
    public static void requestClaimedCodePromo(final Vertx vertx
            , String phoneNumber
            , String claimed_code
            , String source
            , String last_imei
            , long amount
            , long tid
            , String serviceId
            , long promotionTime
            , JsonObject joExtra
            , final Handler<JsonObject> callback) {

        final ClaimCodePromotionObj o = new ClaimCodePromotionObj();
        o.phoneNumber = phoneNumber;
        o.claimed_code = claimed_code;
        o.source = source;
        o.last_imei = last_imei;
        o.amount = amount;
        o.tid = tid;
        o.serviceId = serviceId;
        o.promotionTime = promotionTime;
        o.joExtra = joExtra;
        vertx.eventBus().send(AppConstant.CLAIMED_CODE_PROMOTION_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public static void loadClaimedCodePromo(final Vertx vertx, int command
            , final Handler<JsonArray> callback) {

        final ClaimCodePromotionObj o = new ClaimCodePromotionObj();
        o.command = command;
        vertx.eventBus().send(AppConstant.LOAD_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public static void reloadClaimedCodePromo(final Vertx vertx, int command
            , final Handler<Boolean> callback) {

        final ClaimCodePromotionObj o = new ClaimCodePromotionObj();
        o.command = command;
        vertx.eventBus().send(AppConstant.LOAD_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS, o.toJson(), new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public static void updateClaimedCodePromo(int command, final Vertx vertx) {
        final ClaimCodePromotionObj o = new ClaimCodePromotionObj();
        o.command = command;
        vertx.eventBus().publish(AppConstant.UPDATE_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS, o.toJson());
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.ClaimCodePromotion.PHONE_NUMBER, phoneNumber);
        jo.putString(StringConstUtil.ClaimCodePromotion.CODE, claimed_code);
        jo.putString(StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM, source);
        jo.putString(StringConstUtil.ClaimCodePromotion.DEVICE_IMEI, last_imei);
        jo.putNumber(StringConstUtil.ClaimCodePromotion.AMOUNT, amount);
        jo.putNumber(StringConstUtil.ClaimCodePromotion.TRAN_ID, tid);
        jo.putNumber(StringConstUtil.ClaimCodePromotion.PROMOTION_TIME, promotionTime);
        jo.putString(StringConstUtil.ClaimCodePromotion.SERVICE_ID, serviceId);
        jo.putNumber(StringConstUtil.ClaimCodePromotion.COMMAND, command);
        jo.putObject(StringConstUtil.ClaimCodePromotion.EXTRA, joExtra);
        return jo;
    }
}
