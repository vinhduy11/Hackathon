package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 10/2/14.
 */
public class PromoteCode extends MongoModel{

    //_id is the code;
    private PromoteAction promoteAction;
    private Integer status;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (promoteAction != null)
            json.putString("promoteAction", promoteAction.name());
        if (status != null)
            json.putNumber("status", status);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        try {
            promoteAction = PromoteAction.valueOf(savedObject.getString("promoteAction"));
        } catch (IllegalArgumentException e) {

        }
        status = savedObject.getInteger("status");
    }
}
