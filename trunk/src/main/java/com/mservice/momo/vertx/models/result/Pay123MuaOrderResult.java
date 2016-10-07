package com.mservice.momo.vertx.models.result;

/**
 * Created by nam on 4/23/14.
 */
public enum Pay123MuaOrderResult {
    SUCCESS,
    INVALID_INPUTS,
    CANT_GET_ORDER_INFO,
    INVALID_ORDER_CODE,
    AMOUNT_NOT_MATCH, // {amount}
    ORDER_INVALID_INFO_SHOP_PHONE,
    TRANSFER_ERROR, // M2m
    TRANSFER_EXCEPTION, // M2m core error {errorCode}
    PAID_ORDER,
    SYSTEM_ERROR,
    INVALID_PIN_NUMBER,
    USER_NOT_REGISTERED,
    TARGET_NOT_REGISTERED,
    ACCESS_DENIED,
    NOT_ENOUGH_MONEY
}
