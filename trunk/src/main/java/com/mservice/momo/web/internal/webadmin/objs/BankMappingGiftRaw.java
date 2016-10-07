package com.mservice.momo.web.internal.webadmin.objs;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by congnguyenit on 8/18/16.
 */
public class BankMappingGiftRaw {
    public String number = "";

    public String giftTypeId = "";

    public String giftValue = "";

    public String time = "";

    public String duration = "";

    public String agent = "";

    public String error = "";

    public String errorDesc = "";

    public String program = "";

    public String tid_billpay = "";

    public String amount_billpay = "";

    public String card_id = "";

    public String cashinTid = "";

    public String bank_code = "";

    public BankMappingGiftRaw(String rawRow) {
        String[] fields = rawRow.split(";");
        number = fields.length > 0 ? fields[0].trim() : "";
        giftTypeId = fields.length > 1 ? fields[1].trim() : "";
        giftValue = fields.length > 2 ? fields[2].trim() : "";
        time = fields.length > 3 ? fields[3].trim() : "";
        card_id = fields.length > 4 ? fields[4].trim() : "";
        bank_code = fields.length > 5 ? fields[5].trim() : "";
        cashinTid = fields.length > 6 ? fields[6].trim() : "";
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString("giftTypeId", giftTypeId);
        jo.putString("time", time);
        jo.putString("giftValue", this.giftValue);
        jo.putString("duration", this.duration);
        jo.putString("agent", this.agent);
        jo.putString("error", this.error);
        jo.putString("errorDesc", this.errorDesc);
        jo.putString("program", this.program);
        jo.putString("number", this.number);
        return jo;
    }
}
