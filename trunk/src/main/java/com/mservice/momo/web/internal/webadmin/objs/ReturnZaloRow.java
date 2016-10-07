package com.mservice.momo.web.internal.webadmin.objs;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by tumegame on 28/01/2016.
 */
public class ReturnZaloRow {

    //update data
    public String number = "";

    public String code = "";

    public String error = "";

    public String errorDesc = "";


    public ReturnZaloRow(String rawRow) {
        String[] fields = rawRow.split(";");
        number = fields.length > 0 ? fields[0].trim() : "";
        code = fields.length > 1 ? fields[1].trim() : "";
    }

    public ReturnZaloRow(JsonObject jo) {
        this.number = jo.getString("number", "");
        this.code = jo.getString("code", "");

    }

    public JsonObject toJson() {

        JsonObject jo = new JsonObject();
        jo.putString("number", this.number);
        jo.putString("code", this.code);
        return jo;
    }


}
