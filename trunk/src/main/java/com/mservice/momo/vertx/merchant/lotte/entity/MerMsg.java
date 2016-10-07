package com.mservice.momo.vertx.merchant.lotte.entity;

/**
 * Created by duyhuynh on 31/03/2016.
 */
public class MerMsg {

    public String merchantId = "";
    public String merchantPIN = "";
    public double time = 0;
    public String msgId = "";
    public int errorCode = 0;
    public String errorDesc = "";
    public String appCode = "LOTTE";
    public String appVer = "1.0";
    public String storeId = "";
    public String sessionHash = "";

    public void setErrorCode(MerErrorCode errorCode) {
        this.errorCode = errorCode.getCode();
        this.errorDesc = errorCode.getDesc();
    }

    public void setErrorCode(int errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    public void appendErrorDesc(String desc) {
        this.errorDesc += desc;
    }

    public boolean isSuccess() {
        return errorCode == 0;
    }
}
