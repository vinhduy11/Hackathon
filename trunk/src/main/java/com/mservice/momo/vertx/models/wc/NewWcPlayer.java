package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/27/14.
 */
public class NewWcPlayer extends MongoModel {

    public Integer number;
    public String name;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (number != null)
            json.putNumber("number", number);
        if (name != null)
            json.putString("name", name);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        number = savedObject.getInteger("number");
        name = savedObject.getString("name");
    }
}
