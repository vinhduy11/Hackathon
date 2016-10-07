package com.mservice.momo.data.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/11/14.
 */
public class WcPlayer extends MongoModel {
    public static int STATUS_UNKNOWN = -1;
    public static int STATUS_UNFOLLOW = 0;
    public static int STATUS_FOLLOW = 1;
    public static int STATUS_FOLLOW_MOMOER = 2;
    public static int STATUS_LOCKED = 3;
    public static int STATUS_QUEUE = 4;

    private Integer status;
    private Long time;

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (status != null)
            json.putNumber("status", status);
        if (time != null)
            json.putNumber("time", time);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        status = savedObject.getInteger("status");
        time = savedObject.getLong("time");
    }
}
