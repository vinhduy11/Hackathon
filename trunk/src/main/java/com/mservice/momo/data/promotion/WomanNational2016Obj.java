package com.mservice.momo.data.promotion;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 3/3/16.
 */
public class WomanNational2016Obj {
    public String phoneNumber = "";
    public long cashinTid = 0;
    public String card_id = "";
    public String bank_code = "";
    public JsonObject joExtra = new JsonObject();

    public WomanNational2016Obj(){}
    public WomanNational2016Obj(JsonObject jo) {
        phoneNumber = jo.getString(colName.WomanNationalCols.PHONE_NUMBER, "");
        cashinTid = jo.getLong(colName.WomanNationalCols.CASHIN_TID, 0);
        card_id = jo.getString(colName.WomanNationalCols.CARD_ID, "");
        bank_code = jo.getString(colName.WomanNationalCols.BANK_CODE, "");
        joExtra = jo.getObject(StringConstUtil.WomanNationalField.EXTRA, new JsonObject());
    }

    public static void requestWomanNational2016Promo(final Vertx vertx
            , String phoneNumber
            , long cashinTid
            , String card_id
            , String bank_code
            , JsonObject joExtra
            , final Handler<JsonObject> callback) {

        final WomanNational2016Obj o = new WomanNational2016Obj();
        o.phoneNumber = phoneNumber;
        o.cashinTid = cashinTid;
        o.bank_code = bank_code;
        o.card_id = card_id;
        o.joExtra = joExtra;
        vertx.eventBus().send(AppConstant.WOMAN_NATIONAL_2016_PROMOTION_BUS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
                return;
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(colName.WomanNationalCols.PHONE_NUMBER, phoneNumber);
        jo.putNumber(colName.WomanNationalCols.CASHIN_TID, cashinTid);
        jo.putString(colName.WomanNationalCols.CARD_ID, card_id);
        jo.putString(colName.WomanNationalCols.BANK_CODE, bank_code);
        jo.putObject(StringConstUtil.WomanNationalField.EXTRA, joExtra);
        return jo;
    }
}
