package com.mservice.momo.vertx.models.smartlink;

import javapns.notification.management.VPNPayload;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.json.JsonObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by nam on 7/22/14.
 */
public class SmartLinkResponse {

    public String vpc_Version;
    public String vpc_Locale;
    public String vpc_Command;
    public String vpc_Merchant;
    public String vpc_MerchTxnRef;
    public String vpc_Amount;
    public String vpc_Currency;
    public String vpc_OrderInfo;
    public String vpc_ResponseCode;
    public String vpc_TransactionNo;
    public String vpc_CardType;
    public String vpc_BatchNo;
    public String vpc_AcqResponseCode;
    public String vpc_Message;
    public String vpc_AdditionalData;
    public String vpc_SecureHash;

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.putString("vpc_Version", vpc_Version);
        json.putString("vpc_Locale", vpc_Locale);
        json.putString("vpc_Command", vpc_Command);
        json.putString("vpc_Merchant", vpc_Merchant);
        json.putString("vpc_MerchTxnRef", vpc_MerchTxnRef);
        json.putString("vpc_Amount", vpc_Amount);
        json.putString("vpc_Currency", vpc_Currency);
        json.putString("vpc_OrderInfo", vpc_OrderInfo);
        json.putString("vpc_ResponseCode", vpc_ResponseCode);
        json.putString("vpc_TransactionNo", vpc_TransactionNo);
        json.putString("vpc_CardType", vpc_CardType);
        json.putString("vpc_Version", vpc_Version);
        json.putString("vpc_AcqResponseCode", vpc_AcqResponseCode);
        json.putString("vpc_Message", vpc_Message);
        json.putString("vpc_AdditionalData", vpc_AdditionalData);
        json.putString("vpc_SecureHash", vpc_SecureHash);
        return json;
    }

    public void setValues(JsonObject json) {
        vpc_Version = json.getString("vpc_Version");
        vpc_Locale = json.getString("vpc_Locale");
        vpc_Command = json.getString("vpc_Command");
        vpc_Merchant = json.getString("vpc_Merchant");
        vpc_MerchTxnRef = json.getString("vpc_MerchTxnRef");
        vpc_Amount = json.getString("vpc_Amount");
        vpc_Currency = json.getString("vpc_Currency");
        vpc_OrderInfo = json.getString("vpc_OrderInfo");
        vpc_ResponseCode = json.getString("vpc_ResponseCode");
        vpc_TransactionNo = json.getString("vpc_TransactionNo");
        vpc_CardType = json.getString("vpc_CardType");
        vpc_BatchNo = json.getString("vpc_BatchNo");
        vpc_AcqResponseCode = json.getString("vpc_AcqResponseCode");
        vpc_Message = json.getString("vpc_Message");
        vpc_AdditionalData = json.getString("vpc_AdditionalData");
        vpc_SecureHash = json.getString("vpc_SecureHash");
    }

    public void setValues(MultiMap params) {
        vpc_Version = params.get("vpc_Version");
        vpc_Locale = params.get("vpc_Locale");
        vpc_Command = params.get("vpc_Command");
        vpc_Merchant = params.get("vpc_Merchant");
        vpc_MerchTxnRef = params.get("vpc_MerchTxnRef");
        vpc_Amount = params.get("vpc_Amount");
        vpc_Currency = params.get("vpc_Currency");
        vpc_OrderInfo = params.get("vpc_OrderInfo");
        vpc_ResponseCode = params.get("vpc_ResponseCode");
        vpc_TransactionNo = params.get("vpc_TransactionNo");
        vpc_CardType = params.get("vpc_CardType");
        vpc_BatchNo = params.get("vpc_BatchNo");
        vpc_AcqResponseCode = params.get("vpc_AcqResponseCode");
        vpc_Message = params.get("vpc_Message");
        vpc_AdditionalData = params.get("vpc_AdditionalData");
        vpc_SecureHash = params.get("vpc_SecureHash");
    }

}
