package com.mservice.momo.vertx.models;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 4/22/14.
 */
public class Mua123OrderInfo {
    public static final int CODE_INTERNAL_ERROR = -1;
    public static final int CODE_ORDER_NOT_EXIST = -2;
    public static final int CODE_MAC_PARAMETER_ERROR = -3;
    public static final int CODE_TIMEOUT = -4;
    public static final int CODE_PAID_ORDER = -5;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;

    public int code;
    public String buyerName;
    public String buyerPhone;
    public String shopPhone;
    public Double deposit;
    public int status;

    public Mua123OrderInfo(JsonObject obj) {
        if (obj == null)
            return;
        this.code = obj.getInteger("code");
        this.buyerName = obj.getString("name");
        this.buyerPhone = obj.getString("sender_phone");
        this.shopPhone = obj.getString("receiver_phone");
        try {
            this.deposit = Double.parseDouble(obj.getString("deposit", "0"));
        }catch (NumberFormatException e) {
            e.printStackTrace();
        }
        this.status = obj.getInteger("status", Integer.MIN_VALUE);
    }

    public static String codeName(int code) {
        String result = "Unknown";
        switch (code) {
            case CODE_INTERNAL_ERROR:
                result = "123Mua side error.";
                break;
            case CODE_ORDER_NOT_EXIST:
                result = "Order is not exist.";
                break;
            case CODE_MAC_PARAMETER_ERROR:
                result = "Request params error.";
                break;
            case CODE_TIMEOUT:
                result = "Request time out.";
                break;
            case CODE_PAID_ORDER:
                result = "Order already paid.";
                break;
        }
        return result;
    }
}
