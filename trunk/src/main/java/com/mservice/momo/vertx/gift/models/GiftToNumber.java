package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.util.DataUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 10/16/14.
 */
public class GiftToNumber extends MongoModel{
    public static final int STATUS_NEW = 0;
    public static final int STATUS_ROLLBACK = 1;
    public static final int STATUS_COMMITED = 2;
    public static final int STATUS_ROLLBACK_ERROR = 3;

    public String fromAgent;
    public Long startDate;
    public String toAgent;
    public Long endDate;
    public String comment;
    public String giftId;
    public String giftTypeId;

    public Long tranId;
    public Integer tranError;
    public Integer status;
    public String senderName;

    public String link;

    public GiftToNumber() {

    }

    public GiftToNumber(String fromAgent, String toAgent, String comment, long tranId, String giftId, String giftTypeId, String senderName) {
        this.fromAgent = fromAgent;
        this.startDate = new Date().getTime();
        this.toAgent = toAgent;
        this.comment = comment;
        this.tranId = tranId;
        this.status = STATUS_NEW;
        this.giftId = giftId;
        this.giftTypeId =giftTypeId;
        this.senderName = senderName;
        this.link = RandomStringUtils.randomAlphanumeric(8).toLowerCase();
    }

    public GiftToNumber(JsonObject giftToNumber) {
        setValues(giftToNumber);
        setModelId(giftToNumber.getString("_id"));
    }

    public Date getStartDate() {
        if (startDate == null)
            return null;
        return new Date(startDate);
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (fromAgent != null)
            json.putString("fromAgent", fromAgent);
        if (startDate != null)
            json.putNumber("startDate", startDate);
        if (toAgent != null)
            json.putString("toAgent", toAgent);
        if (endDate != null)
            json.putNumber("endDate", endDate);
        if (comment != null)
            json.putString("comment", comment);
        if (tranId != null)
            json.putNumber("tranId", tranId);
        if (giftId != null)
            json.putString("giftId", giftId);
        if (tranError != null)
            json.putNumber("tranError", tranError);
        if (giftTypeId != null)
            json.putString("giftTypeId", giftTypeId);
        if (senderName != null)
            json.putString("senderName", senderName);
        if (link != null)
            json.putString("link", link);
        if (status != null)
            json.putNumber("status", status);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        fromAgent = savedObject.getString("fromAgent");
        startDate = savedObject.getLong("startDate");
        toAgent = savedObject.getString("toAgent");
        endDate = savedObject.getLong("endDate");
        comment = savedObject.getString("comment");
        tranId = savedObject.getLong("tranId");
        giftId = savedObject.getString("giftId");
        tranError = savedObject.getInteger("tranError");
        giftTypeId = savedObject.getString("giftTypeId");
        senderName = savedObject.getString("senderName");
        link = savedObject.getString("link");
        status =savedObject.getInteger("status");
    }
}
