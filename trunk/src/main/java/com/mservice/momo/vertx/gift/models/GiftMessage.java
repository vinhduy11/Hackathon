package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 10/15/14.
 */
public class GiftMessage extends MongoModel{

    // modelId is the transactionId

    public String senderMsg;
    public String receiverMsg;
    public Date sendTime;
    public Date receiveTime;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (senderMsg != null)
            json.putString("senderMsg", senderMsg);
        if (receiverMsg != null)
            json.putString("receiverMsg", receiverMsg);
        if (sendTime != null)
            json.putNumber("sendTime", sendTime.getTime());
        if (receiveTime != null)
            json.putNumber("receiveTime", receiveTime.getTime());
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        Long temp;
        this.senderMsg = savedObject.getString("senderMsg");
        this.receiverMsg = savedObject.getString("receiverMsg");

        temp = savedObject.getLong("sendTime");
        if (temp != null)
            this.sendTime = new Date(temp);

        temp = savedObject.getLong("receiveTime");
        if (temp != null)
            this.receiveTime = new Date(temp);
    }

    public void setSendTime(long time) {
        this.sendTime = new Date(time);
    }
}
