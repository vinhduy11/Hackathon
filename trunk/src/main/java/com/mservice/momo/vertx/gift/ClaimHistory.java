package com.mservice.momo.vertx.gift;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 10/21/14.
 */
public class ClaimHistory extends MongoModel {

    public String fromAgent;
    public Integer toPhone;
    public Long time;
    public Integer tranError;
    public Long amount;
    public Long tranId;
    public String giftId;
    public String code;
    public String comment;

    public ClaimHistory() {
    }

    public ClaimHistory(String fromAgent, int toPhone, int tranError, long amount, long tranId, String giftId, String code) {
        this.fromAgent = fromAgent;
        this.toPhone = toPhone;
        this.tranError = tranError;
        this.amount = amount;
        this.tranId = tranId;
        this.giftId = giftId;
        this.code = code;
        this.time = System.currentTimeMillis();
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (fromAgent != null)
            json.putString("fromAgent", fromAgent);
        if (toPhone != null)
            json.putNumber("toPhone", toPhone);
        if (time != null)
            json.putNumber("time", time);
        if (tranError != null)
            json.putNumber("tranError", tranError);
        if (amount != null)
            json.putNumber("amount", amount);
        if (tranId != null)
            json.putNumber("tranId", tranId);
        if (giftId != null)
            json.putString("giftId", giftId);
        if (code != null)
            json.putString("code", code);
        if (comment != null)
            json.putString("comment", comment);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        fromAgent = savedObject.getString("fromAgent");
        toPhone = savedObject.getInteger("toPhone");
        time = savedObject.getLong("time");
        tranError = savedObject.getInteger("tranError");
        amount = savedObject.getLong("amount");
        tranId = savedObject.getLong("tranId");
        giftId = savedObject.getString("giftId");
        code = savedObject.getString("code");
        comment = savedObject.getString("comment");
    }
}
