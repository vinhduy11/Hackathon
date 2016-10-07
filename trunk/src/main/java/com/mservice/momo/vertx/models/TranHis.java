package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 8/5/14.
 */
public class TranHis extends MongoModel{

    public Long tranId; // return from core doing transaction, cTID
    public Long client_time; // time at calling webservice
    public Long ackTime; // ack time from server to client
    public Long finishTime; // the time that server response result.
    public Integer tranType; //ex : MomoProto.MsgType.BANK_IN_VALUE
    public Integer io; // direction of transaction.
    public Integer category; // type of transfer, chuyen tien ve que, cho vay, khac
    public String partnerId; // ban hang cua minh : providerId ..
    public String partnerCode; // ma doi tac
    public String partnerName; // ten doi tac
    public String partner_ref;
    public String billId; // for invoice, billID
    public Long amount; // gia tri giao dich
    public String comment; // ghi chu
    public Integer status; // trang thai giao dich
    public Integer error; // ma loi giao dich
    public Long command_Ind; // command index
    public Integer source_from; // tu nguon nao
    public String partner_extra_1;


    @Override
    public void setValues(JsonObject savedObject) {
        this.tranId = savedObject.getLong("tranId");
        this.client_time = savedObject.getLong("client_time");
        this.ackTime = savedObject.getLong("ackTime");
        this.finishTime = savedObject.getLong("finishTime");

        this.tranType = savedObject.getInteger("tranType");
        this.io = savedObject.getInteger("io");
        this.category = savedObject.getInteger("category");

        this.partnerId = savedObject.getString("partnerId");
        this.partnerCode = savedObject.getString("partnerCode");
        this.partnerName = savedObject.getString("partnerName");
        this.partner_ref = savedObject.getString("partner_ref");
        this.billId = savedObject.getString("billId");

        this.amount = savedObject.getLong("amount");
        this.comment = savedObject.getString("comment");
        this.status = savedObject.getInteger("status");
        this.error = savedObject.getInteger("error");
        this.command_Ind = savedObject.getLong("command_Ind");
        this.source_from = savedObject.getInteger("source_from");
        this.partner_extra_1 = savedObject.getString("partner_extra_1");
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();

        if (tranId != null) json.putNumber("tranId", tranId);
        if (tranId != null) json.putNumber("client_time", client_time);
        if (tranId != null) json.putNumber("ackTime", ackTime);
        if (tranId != null) json.putNumber("finishTime", finishTime);
        if (tranId != null) json.putNumber("tranType", tranType);
        if (tranId != null) json.putNumber("io", io);
        if (tranId != null) json.putNumber("category", category);

        if (partnerId != null) json.putString("partnerId", partnerId);
        if (partnerCode != null) json.putString("partnerCode", partnerCode);
        if (partnerName != null) json.putString("partnerName", partnerName);
        if (partner_ref != null) json.putString("partner_ref", partner_ref);
        if (billId != null) json.putString("partnerId", billId);

        if (amount != null) json.putNumber("tranId", amount);
        if (comment != null) json.putString("comment", comment);

        if (status != null) json.putNumber("status", status);
        if (error != null) json.putNumber("error", error);
        if (command_Ind != null) json.putNumber("command_Ind", command_Ind);
        if (source_from != null) json.putNumber("source_from", source_from);
        if (comment != null) json.putString("partner_extra_1", partner_extra_1);

        return json;
    }
}
