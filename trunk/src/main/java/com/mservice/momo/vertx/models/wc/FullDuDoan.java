package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/15/14.
 */
public class FullDuDoan extends MongoModel{

    public String phoneNumber;
    public String matchId;
    public Integer result;
    public Integer a;
    public Integer b;
    public Long time;
    public Long zaloTime;
    public Long zaloId;

    public void setValues(DuDoan duDoan) {
        phoneNumber = duDoan.getModelId();
        matchId = duDoan.getMatchId();
        result = duDoan.getResult();
        a = duDoan.getA();
        b = duDoan.getB();
        zaloTime = duDoan.getZaloTime();
        time = duDoan.getTime();
        zaloId = duDoan.getZaloId();
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        json.putString("phoneNumber", phoneNumber);
        json.putString("matchId", matchId);
        json.putNumber("result", result);
        json.putNumber("a", a);
        json.putNumber("b", b);
        json.putNumber("time", time);
        json.putNumber("zaloTime", zaloTime);
        json.putNumber("zaloId", zaloId);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        phoneNumber = savedObject.getString("phoneNumber");
        matchId = savedObject.getString("matchId");
        result = savedObject.getInteger("result");
        a = savedObject.getInteger("a");
        b = savedObject.getInteger("b");
        time = savedObject.getLong("time");
        zaloTime = savedObject.getLong("zaloTime");
        zaloId = savedObject.getLong("zaloId");
    }
}
