package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 5/23/14.
 */
public class UserInfo extends MongoModel {

    private Integer phoneNumber;

    private Date lastLoginFromWeb;
    private Date lastLoginFromMobile;

    private Date lastWebTransTime;
    private Date lastMobileTransTime;

    public Integer getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Integer phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getLastLoginFromWeb() {
        return lastLoginFromWeb;
    }

    public void setLastLoginFromWeb(Date lastLoginFromWeb) {
        this.lastLoginFromWeb = lastLoginFromWeb;
    }

    public Date getLastLoginFromMobile() {
        return lastLoginFromMobile;
    }

    public void setLastLoginFromMobile(Date lastLoginFromMobile) {
        this.lastLoginFromMobile = lastLoginFromMobile;
    }

    public Date getLastWebTransTime() {
        return lastWebTransTime;
    }

    public void setLastWebTransTime(Date lastWebTransTime) {
        this.lastWebTransTime = lastWebTransTime;
    }

    public Date getLastMobileTransTime() {
        return lastMobileTransTime;
    }

    public void setLastMobileTransTime(Date lastMobileTransTime) {
        this.lastMobileTransTime = lastMobileTransTime;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (phoneNumber != null)
            json.putNumber("phoneNumber", phoneNumber);
        if (lastLoginFromWeb != null)
            json.putNumber("lastLoginFromWeb", lastLoginFromWeb.getTime());
        if (lastLoginFromMobile != null)
            json.putNumber("lastLoginFromMobile", lastLoginFromMobile.getTime());
        if (lastWebTransTime != null)
            json.putNumber("lastWebTransTime", lastWebTransTime.getTime());
        if (lastMobileTransTime != null)
            json.putNumber("lastMobileTransTime", lastMobileTransTime.getTime());
        return json;
    }

    @Override
    public void setValues(JsonObject dao) {
        Long temp;
        phoneNumber = dao.getInteger("phoneNumber");
        temp = dao.getLong("lastLoginFromWeb");
        if (temp != null)
            lastLoginFromWeb = new Date(temp);
        temp = dao.getLong("lastLoginFromMobile");
        if (temp != null)
            lastLoginFromMobile = new Date(temp);
        temp = dao.getLong("lastWebTransTime");
        if (temp != null)
            lastWebTransTime = new Date(temp);
        temp = dao.getLong("lastMobileTransTime");
        if (temp != null)
            lastMobileTransTime = new Date(temp);
    }
}
