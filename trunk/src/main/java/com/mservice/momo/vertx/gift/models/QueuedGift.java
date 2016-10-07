package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 10/24/14.
 */
public class QueuedGift extends MongoModel {

    public String owner;
    public String giftId;
    public String gifTypeId;
    public String service;

    public QueuedGift() {
    }

    public QueuedGift(String owner) {
        this.owner = owner;
    }

    public QueuedGift(String owner, String service) {
        this.owner = owner;
        this.service = service;
    }

    public QueuedGift clone(){
        QueuedGift q = new QueuedGift();
        q.owner = this.owner;
        q.giftId = this.giftId;
        q.gifTypeId = this.gifTypeId;
        q.service = this.service;
        return q;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (owner != null)
            json.putString("owner", owner);
        if (giftId != null)
            json.putString("giftId", giftId);
        if (gifTypeId != null)
            json.putString("gifTypeId", gifTypeId);
        if (service != null)
            json.putString("service", service);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        owner = savedObject.getString("owner");
        giftId = savedObject.getString("giftId");
        gifTypeId = savedObject.getString("gifTypeId");
        service = savedObject.getString("service");
    }
}
