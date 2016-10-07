package com.mservice.momo.web.internal.webadmin.objs;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by congnguyenit on 8/11/16.
 */
public class ReferralGiftRaw {

    public String number = "";

    public String invitee_number = ""; // wallet will be get voucherr

    public String inviter_number = "";

    public String type = "";

    public String giftTypeId = "";

    public String time = "";

    public String giftValue = "";

    public String duration = "";

    public String agent = "";

    public String error = "";

    public String errorDesc = "";

    public String program = "";

    public String group = "";

    public ReferralGiftRaw(String rawRow) {
        String[] fields = rawRow.split(";");
        invitee_number = fields.length > 0 ? fields[0].trim() : "";
        inviter_number = fields.length > 1 ? fields[1].trim() : "";
        type = fields.length > 2 ? fields[2].trim() : "";
        group = fields.length > 3 ? fields[3].trim() : "";
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString("invitee_number", this.invitee_number);
        jo.putString("inviter_number", this.inviter_number);
        jo.putString("type", type);
        jo.putString("giftTypeId", giftTypeId);
        jo.putString("time", time);
        jo.putString("giftValue", this.giftValue);
        jo.putString("duration", this.duration);
        jo.putString("agent", this.agent);
        jo.putString("error", this.error);
        jo.putString("errorDesc", this.errorDesc);
        jo.putString("program", this.program);
        jo.putString("group", this.group);
        jo.putString("number", this.number);
        return jo;
    }
}
