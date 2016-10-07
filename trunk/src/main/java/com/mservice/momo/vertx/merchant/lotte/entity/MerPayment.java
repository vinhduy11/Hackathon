package com.mservice.momo.vertx.merchant.lotte.entity;

/**
 * Created by duyhuynh on 31/03/2016.
 */
public class MerPayment extends MerMsg {

    public String TID;
    public String description;
    public String voucherCode;
    public double amount;
    public double oriAmount;
    public String hash;
    public String idRef;
    public String phone;
    public String paymentCode;
}
