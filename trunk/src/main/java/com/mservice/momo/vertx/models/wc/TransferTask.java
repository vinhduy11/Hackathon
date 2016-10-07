package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 7/1/14.
 */
public class TransferTask extends MongoModel {

    public Integer phone;
    public Long money;
    public Integer tranError;
    public Long sentMoney;
    public String comment;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (phone != null)
            json.putNumber("phone", phone);
        if (money != null)
            json.putNumber("money", money);
        if (tranError != null)
            json.putNumber("tranError", tranError);
        if (sentMoney != null)
            json.putNumber("sentMoney", sentMoney);
        if (comment != null)
            json.putString("comment", comment);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        phone = savedObject.getInteger("phone");
        money = savedObject.getLong("money");
        tranError = savedObject.getInteger("tranError");
        sentMoney = savedObject.getLong("sentMoney");
        comment = savedObject.getString("comment");
    }
}
