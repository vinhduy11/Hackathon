package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/26/14.
 */
public class ThuongThem extends MongoModel {
    //KEY IS THE PHONE NUMBER

    public String matchId;
    public Long money;
    public JsonArray numbers;
    public Integer tranError;

    public Long sentMoney;
    public Boolean sentZaloMessage;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (money != null)
            json.putNumber("money", money);
        if (numbers != null)
            json.putArray("numbers", numbers);
        if (tranError != null)
            json.putNumber("tranError", tranError);
        if (sentMoney != null)
            json.putNumber("sentMoney", sentMoney);
        if (sentZaloMessage != null)
            json.putBoolean("sentZaloMessage", sentZaloMessage);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        money = savedObject.getLong("money");
        numbers = savedObject.getArray("numbers");
        tranError = savedObject.getInteger("tranError");
        sentMoney = savedObject.getLong("sentMoney");
        sentZaloMessage = savedObject.getBoolean("sentZaloMessage");
    }
}
