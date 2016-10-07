package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/13/14.
 */
public class TangThuong extends MongoModel{

    public Integer sent;

    public Integer isSent() {
        return sent;
    }

    public void setSent(Integer sent) {
        this.sent = sent;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (sent != null)
            json.putNumber("sent", sent);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        sent = savedObject.getInteger("sent");
    }
}
