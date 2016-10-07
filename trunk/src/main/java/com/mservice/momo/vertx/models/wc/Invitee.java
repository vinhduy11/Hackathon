package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/25/14.
 */
public class Invitee extends MongoModel {

    public Integer inviter;
    public Long time;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (inviter != null)
            json.putNumber("inviter", inviter);
        if (time != null)
            json.putNumber("time", time);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        inviter = savedObject.getInteger("inviter");
        time = savedObject.getLong("time");
    }

}
