package com.mservice.momo.web;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created by ntunam on 4/7/14.
 */
public class HasStateJsonObject extends JsonObject{

    private boolean changed = false;

    public HasStateJsonObject(String jsonString) {
        super(jsonString);
    }
    public HasStateJsonObject(Map<String, Object> map) {
        super(map);
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public JsonObject putArray(String fieldName, JsonArray value) {
        this.changed = true;
        return super.putArray(fieldName, value);
    }

    @Override
    public JsonObject putBinary(String fieldName, byte[] binary) {
        this.changed = true;
        return super.putBinary(fieldName, binary);
    }

    @Override
    public JsonObject putBoolean(String fieldName, Boolean value) {
        this.changed = true;
        return super.putBoolean(fieldName, value);
    }

    @Override
    public JsonObject putElement(String fieldName, JsonElement value) {
        this.changed = true;
        return super.putElement(fieldName, value);
    }

    @Override
    public JsonObject putNumber(String fieldName, Number value) {
        this.changed = true;
        return super.putNumber(fieldName, value);
    }

    @Override
    public JsonObject putObject(String fieldName, JsonObject value) {
        this.changed = true;
        return super.putObject(fieldName, value);
    }

    @Override
    public JsonObject putString(String fieldName, String value) {
        this.changed = true;
        return super.putString(fieldName, value);
    }

    @Override
    public JsonObject putValue(String fieldName, Object value) {
        this.changed = true;
        return super.putValue(fieldName, value);
    }
}
