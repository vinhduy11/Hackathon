package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 8/7/14.
 */
public class TranStatusConfig extends MongoModel {

    public Integer tranType;
    public String name;
    public Integer status;

    public TranStatusConfig() {
    }

    public TranStatusConfig(Integer tranType, String name, Integer status) {
        this.tranType = tranType;
        this.name = name;
        this.status = status;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (tranType != null)
            json.putNumber("tranType", tranType);
        if (name != null)
            json.putString("name", name);
        if (status != null)
            json.putNumber("status", status);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        this.tranType = savedObject.getInteger("tranType");
        this.name = savedObject.getString("name");
        this.status = savedObject.getInteger("status");
    }

    @Override
    public String toString() {
        return "TranStatusConfig{" +
                "tranType=" + tranType +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
