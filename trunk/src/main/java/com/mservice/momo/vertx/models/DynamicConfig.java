package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/2/14.
 */
public class DynamicConfig extends MongoModel {

    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (name != null)
            json.putString("name", name);
        if (value != null)
            json.putString("value", value);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        name = savedObject.getString("name");
        value = savedObject.getString("value");
    }

}
