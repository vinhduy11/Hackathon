package com.mservice.momo.vertx.models.rate;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 9/13/14.
 */
public class StoreWarning extends MongoModel {
    public static final int STATUS_NEW = 0;
    public static final int STATUS_REVIEWED = 1;

    public Integer storeId;
    public String content;
    public Integer committer;
    public Integer status;
    public Integer warningType;
    public Date date;

    public StoreWarning() {
    }

    public StoreWarning(MomoProto.WarnStore w, int committer) {
        this.storeId = w.getStoreId();
        this.committer = committer;
        this.status = STATUS_NEW;
        this.warningType = w.getWarningType();
        this.date = new Date();
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (storeId != null)
            json.putNumber("storeId", storeId);
        if (content != null)
            json.putString("content", content);
        if (warningType != null)
            json.putNumber("warningType", warningType);
        if (committer != null)
            json.putNumber("committer", committer);
        if (status != null)
            json.putNumber("status", status);
        if (date != null)
            json.putNumber("date", date.getTime());
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        Long temp;
        storeId = savedObject.getInteger("storeId");
        content = savedObject.getString("content");
        warningType = savedObject.getInteger("warningType");
        committer = savedObject.getInteger("committer");
        status = savedObject.getInteger("status");

        temp = savedObject.getLong("date");
        if (temp != null) {
            date = new Date(temp);
        }
    }
}
