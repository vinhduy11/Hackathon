package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 11/4/14.
 */
public class GiftTran extends MongoModel {

    //key is tranid
    public Integer phone;
    public Boolean thank;

    public List<String> giftIds;
    public String serviceId;

    public Long curVoucher;
    public Long curPoint;
    public Long curMomo;

    public Long useVoucher;
    public Long usePoint;
    public Long useMomo;

    public Long amount;

    public Long returnPointTranId;
    public Integer returnPointError;
    public Long returnPoint;
    public Integer backendGiftTran;

    public Long returnMomoTranId;
    public Integer returnMomoError;

    public Long time;

    public GiftTran() {

    }

    public GiftTran(TransferWithGiftContext context) {
        super(String.valueOf(context.tranId));
        phone = context.phone;
        if (context.queuedGiftResult != null && context.queuedGiftResult.gifts != null && !context.queuedGiftResult.gifts.isEmpty()) {
            giftIds = new ArrayList<>();
            for(String key: context.queuedGiftResult.gifts.keySet())
                giftIds.add(key);
        }
        serviceId = context.serviceId;
        curVoucher = context.curGift;
        curPoint = context.curPoint;
        curMomo = context.curMomo;

        useVoucher = context.voucher;
        usePoint = context.point;
        useMomo = context.momo;

        returnPointTranId = context.returnPointTranId;
        returnPointError = context.returnPointError;
        backendGiftTran = context.backendGiftTran;
        amount = context.amount;
        returnMomoTranId = context.returnMomoTranId;
        returnMomoError = context.returnMomoError;
        time = System.currentTimeMillis();
    }

    public GiftTran(String modelId) {
        super(modelId);
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (thank != null)
            json.putBoolean("thank", thank);
        if (phone != null)
            json.putNumber("phone", phone);
        if (giftIds != null && !giftIds.isEmpty()) {
            JsonArray arr = new JsonArray();
            for(String giftId: giftIds) {
                arr.add(giftId);
            }
            json.putArray("giftIds", arr);
        }
        if (serviceId != null)
            json.putString("serviceId", serviceId);
        if (curVoucher != null)
            json.putNumber("curVoucher", curVoucher);
        if (curPoint != null)
            json.putNumber("curPoint", curPoint);
        if (curMomo != null)
            json.putNumber("curMomo", curMomo);
        if (useVoucher != null)
            json.putNumber("useVoucher", useVoucher);
        if (usePoint != null)
            json.putNumber("usePoint", usePoint);
        if (useMomo != null)
            json.putNumber("useMomo", useMomo);
        if (returnPoint != null)
            json.putNumber("returnPoint", returnPoint);
        if (returnPointTranId != null)
            json.putNumber("returnPointTranId", returnPointTranId);
        if (returnPointError != null)
            json.putNumber("returnPointError", returnPointError);
        if (backendGiftTran != null)
            json.putNumber("backendGiftTran", backendGiftTran);
        if (amount != null)
            json.putNumber("amount", amount);
        if (returnMomoTranId != null)
            json.putNumber("returnMomoTranId", returnMomoTranId);
        if (returnMomoError != null)
            json.putNumber("returnMomoError", returnMomoError);
        if (time != null)
            json.putNumber("time", time);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        this.thank = savedObject.getBoolean("thank");
        JsonArray arr = savedObject.getArray("giftIds");
        if (arr != null) {
            this.giftIds = arr.toList();
        }
        this.serviceId = savedObject.getString("serviceId");
        this.curVoucher = savedObject.getLong("curVoucher");
        this.curPoint = savedObject.getLong("curPoint");
        this.curMomo = savedObject.getLong("curMomo");
        this.usePoint = savedObject.getLong("usePoint");
        this.useMomo = savedObject.getLong("useMomo");
        this.returnPoint = savedObject.getLong("returnPoint");
        this.returnPointTranId = savedObject.getLong("returnPointTranId");
        this.returnPointError = savedObject.getInteger("returnPointError");
        this.backendGiftTran = savedObject.getInteger("backendGiftTran");
        this.amount = savedObject.getLong("amount");
        this.returnMomoTranId = savedObject.getLong("returnMomoTranId");
        this.returnMomoError = savedObject.getInteger("returnMomoError");
        this.time = savedObject.getLong("time");
    }
}
