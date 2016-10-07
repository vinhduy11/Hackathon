package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 10/7/14.
 */
public class GiftHistory extends MongoModel {

    public String giftId;
    public String from;
    public String to;
    public String middleAgent;
    public Long tranId;
    public Long time;
    public String note;

    public GiftHistory() {

    }

    public GiftHistory(String giftId, String from, String to, long tranId) {
        this.giftId = giftId;
        this.from = from;
        this.to = to;
        this.tranId = tranId;
        this.time = new Date().getTime();
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (giftId != null)
            json.putString("giftId", giftId);
        if (from != null)
            json.putString("from", from);
        if (to != null)
            json.putString("to", to);
        if (tranId != null)
            json.putNumber("tranId", tranId);
        if (time != null)
            json.putNumber("time", time);
        if (middleAgent != null)
            json.putString("middleAgent", middleAgent);
        if (note != null)
            json.putString("note", note);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        giftId = savedObject.getString("giftId");
        from = savedObject.getString("from");
        to = savedObject.getString("to");
        tranId = savedObject.getLong("tranId");
        time = savedObject.getLong("time");
        middleAgent = savedObject.getString("middleAgent");
        note = savedObject.getString("note");
    }
}
