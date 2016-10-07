package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 10/29/14.
 */
public class CdhhConfig extends MongoModel {

    public String periodName;
    public String collName;
    public Long startTime;
    public Long endTime;
    public Boolean active;
    public String serviceId;
    public Integer minCode;
    public Integer maxCode;

    public Boolean report;

    public CdhhConfig() {

    }

    public CdhhConfig(JsonObject newValues) {
        this.setModelId(newValues.getString("_id"));
        this.setValues(newValues);
    }


    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (periodName != null)
            json.putString("periodName", periodName);
        if (collName != null)
            json.putString("collName", collName);
        if (active != null)
            json.putBoolean("active", active);
        if (startTime != null)
            json.putNumber("startTime", startTime);
        if (endTime != null)
            json.putNumber("endTime", endTime);

        if (serviceId != null) {
            json.putString("serviceId", serviceId);
        }

        if (minCode != null) {
            json.putNumber("minCode", minCode);
        }
        if (maxCode != null) {
            json.putNumber("maxCode", maxCode);
        }
        if (report != null) {
            json.putBoolean("report", report);
        }


        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        this.periodName = savedObject.getString("periodName");
        this.collName = savedObject.getString("collName");
        this.active = savedObject.getBoolean("active");
        this.startTime = savedObject.getLong("startTime");
        this.endTime = savedObject.getLong("endTime");
        this.serviceId = savedObject.getString("serviceId");
        this.minCode = savedObject.getInteger("minCode", 0);
        this.maxCode = savedObject.getInteger("maxCode", 0);
        this.report = savedObject.getBoolean("report", false);
    }
}
