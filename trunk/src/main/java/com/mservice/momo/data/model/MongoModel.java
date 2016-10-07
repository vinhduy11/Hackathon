package com.mservice.momo.data.model;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 5/23/14.
 */
public abstract class MongoModel {
    private String modelId = null;

    public MongoModel() {
    }

    public MongoModel(JsonObject json) {
        this.modelId = json.getString("modelId");
    }

    public MongoModel(String modelId) {
        this.modelId = modelId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public abstract JsonObject getPersisFields();

    public abstract void setValues(JsonObject savedObject);

    @Override
    public String toString() {
        return String.format("MongoModel[id:%s, %s]", getModelId(), String.valueOf(getPersisFields()));
    }

    public JsonObject toJsonObject() {
        JsonObject json = getPersisFields();
        if (modelId != null)
            json.putString("_id", modelId);
        return json;
    }
}
