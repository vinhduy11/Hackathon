package com.mservice.momo.web.internal.webadmin.objs;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 5/28/15.
 */
public class ReturnGiftRow {

    //update data
    public String number = ""; // wallet will be get voucher

    public String giftTypeId = "";

    public String time = "";

    public String giftValue = "";

    public String duration = "";

    public String agent = "";

    public String error = "";

    public String errorDesc = "";

    public String program = "";

    public String group = "";

    public ReturnGiftRow(String rawRow) {
        String[] fields = rawRow.split(";");
        number = fields.length > 0 ? fields[0].trim() : "";
        giftTypeId = fields.length > 1 ? fields[1].trim() : "";
        giftValue = fields.length > 2 ? fields[2].trim() : "";
        duration = fields.length > 3 ? fields[3].trim() : "";
        agent = fields.length > 4 ? fields[4].trim() : "";
        program = fields.length > 5 ? fields[5].trim() : "";
        group = fields.length > 6 ? fields[6].trim() : "";

//        error = fields.length > 11 ? fields[11].trim() : "";
//        errorDesc = fields.length > 12 ? fields[12].trim() : "";
        /*
        o.number
        o.nameCustomer
        o.enable
        o.group
        o.groupDesc
        o.orgDateFrom
        o.orgDateTo
        o.duration
        o.momoAgent
        o.promoValue
        o.giftTypeId
        */
    }

    public ReturnGiftRow(JsonObject jo) {
        this.number = jo.getString("number", "0");
        giftTypeId = jo.getString("giftTypeId", "");
        time = jo.getString("time", "");
        giftValue = jo.getString("giftValue", "");
        duration = jo.getString("duration", "");
        agent = jo.getString("agent", "");
        error = jo.getString("error", "");
        errorDesc = jo.getString("errorDesc", "");
        program = jo.getString("program", "");
        group = jo.getString("group", "");
    }

    public JsonObject toJson() {

        JsonObject jo = new JsonObject();
        jo.putString("number", this.number);
        jo.putString("giftTypeId", giftTypeId);
        jo.putString("time", time);
        jo.putString("giftValue", this.giftValue);
        jo.putString("duration", this.duration);
        jo.putString("agent", this.agent);
        jo.putString("error", this.error);
        jo.putString("errorDesc", this.errorDesc);
        jo.putString("program", this.program);
        jo.putString("group", this.group);

        return jo;
    }

//    public ReturnGiftRow toRowObjFromJson(JsonObject jo) {
//
//
//        this.number = jo.getString("number", "0");
//        giftTypeId = jo.getString("giftTypeId", "");
//        time = jo.getString("time", "");
//        giftValue = jo.getString("giftValue", "");
//        duration = jo.getString("duration", "");
//        agent = jo.getString("agent", "");
//        error = jo.getString("error", "");
//        errorDesc = jo.getString("errorDesc", "");
//        program = jo.getString("program", "");
//        group = jo.getString("group", "");
//
//        return jo;
//    }
}
