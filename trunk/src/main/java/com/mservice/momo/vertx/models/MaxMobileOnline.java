package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 5/24/14.
 */
public class MaxMobileOnline extends MongoModel{

    private Long userNumber;
    private Date time;

    public Long getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(Long userNumber) {
        this.userNumber = userNumber;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (userNumber != null)
            json.putNumber("userNumber", userNumber);
        if (time != null)
            json.putNumber("time", time.getTime());
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        Long temp;

        userNumber = savedObject.getLong("userNumber");
        temp = savedObject.getLong("time");
        time = new Date(temp);
    }
}
