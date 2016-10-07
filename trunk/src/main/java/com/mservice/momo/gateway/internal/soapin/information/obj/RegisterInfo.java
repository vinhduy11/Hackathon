package com.mservice.momo.gateway.internal.soapin.information.obj;

import com.mservice.momo.data.model.colName;
import org.vertx.java.core.json.JsonObject;

public class RegisterInfo {

    String phone = "";
    String name = "";
    String persional_id = "";
    String province = "";
    boolean isRegisterNew;
    long time_receive = 0;
    long transid = 0;
    String retailer_phone = "";
    int level = 0;
    String newPin = "";
    String email = "";
    String dateOfBirth = "";
    boolean isNamed = false;

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersional_id() {
        return persional_id;
    }

    public void setPersional_id(String persional_id) {
        this.persional_id = persional_id;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public boolean isRegisterNew() {
        return isRegisterNew;
    }

    public void setRegisterNew(boolean isRegisterNew) {
        this.isRegisterNew = isRegisterNew;
    }

    public long getTime_receive() {
        return time_receive;
    }

    public void setTime_receive(long time_receive) {
        this.time_receive = time_receive;
    }

    public long getTransid() {
        return transid;
    }

    public void setTransid(long transid) {
        this.transid = transid;
    }

    public String getRetailer_phone() {
        return retailer_phone;
    }

    public void setRetailer_phone(String retailer_phone) {
        this.retailer_phone = retailer_phone;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String mail) {
        email = mail;
    }

    public boolean isNamed() {
        return isNamed;
    }

    public void setIsNamed(boolean _isName) {
        this.isNamed = _isName;
    }

    public JsonObject toJsonObject() {
        JsonObject jsonReturn = new JsonObject();

        jsonReturn.putString(colName.RegisterInfoCol.PHONE, getPhone());
        jsonReturn.putString(colName.RegisterInfoCol.NAME, getName());
        jsonReturn.putString(colName.RegisterInfoCol.CARD_ID, getPersional_id());
        jsonReturn.putString(colName.RegisterInfoCol.ADDRESS, getProvince());
        jsonReturn.putString(colName.RegisterInfoCol.DATE_OF_BIRTH, getDateOfBirth());
        jsonReturn.putBoolean(colName.RegisterInfoCol.IS_NAME, isNamed());


        return jsonReturn;
    }
}
