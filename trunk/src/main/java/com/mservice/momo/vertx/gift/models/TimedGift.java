package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 10/31/14.
 */
public class TimedGift extends MongoModel {

    public String giftId;
    public String giftType;
    public String message;
    public String fromAgent;
    public String toAgent;
    public Long time;
    public Integer error;
    public String pin;
    public String fromAgentName;
    public String desc;
    public Long preTranId;

    public TimedGift() {
    }

    public TimedGift(JsonObject json) {
        super(json);
        setValues(json);
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (giftId != null)
            json.putString("giftId", giftId);
        if (giftType != null)
            json.putString("giftType", giftType);
        if (message != null)
            json.putString("message", message);
        if (fromAgent != null)
            json.putString("fromAgent", fromAgent);
        if (toAgent != null)
            json.putString("toAgent", toAgent);
        if (time != null)
            json.putNumber("time", time);

        if(pin != null)
            json.putString("pin",pin);

        if (fromAgentName != null)
            json.putString("fromAgentName", fromAgentName);

        if (error != null)
            json.putNumber("error", error);

        if(desc != null){
            json.putString("desc",desc);
        }

        if(preTranId != null)
        {
            json.putNumber("preTranId", preTranId);
        }

        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        this.giftId = savedObject.getString("giftId");
        this.giftType = savedObject.getString("giftType");
        this.message = savedObject.getString("message");
        this.fromAgent = savedObject.getString("fromAgent");
        this.toAgent = savedObject.getString("toAgent");
        this.time = savedObject.getLong("time");
        this.fromAgentName = savedObject.getString("fromAgentName");
        this.pin = savedObject.getString("pin");
        this.error = savedObject.getInteger("error");
        this.desc = savedObject.getString("desc","");
        this.preTranId = savedObject.getLong("preTranId");
    }
}
