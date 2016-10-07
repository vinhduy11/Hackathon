package com.mservice.momo.web.internal.webadmin.objs;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 3/9/15.
 */
public class VtbRow {
    //SDT;Bankname;Bankcode;Name;Error;Desc
    public String number = "";
    public String cmnd ="";
    public String bankname ="";
    public String bankcode="";
    public String name="";
    public String error = "";
    public String desc = "";
    public VtbRow(String rawRow){
        String[] fields = rawRow.split(";");
        number = fields.length>0 ? fields[0].trim() : "";
        cmnd = fields.length>1 ? fields[1].trim() : "";
        bankname = fields.length>2 ? fields[2].trim() : "";
        bankcode = fields.length>3 ? fields[3].trim() : "";
        name = fields.length>4 ? fields[4].trim() : "";
        error = "";
        desc ="";
    }
    public VtbRow(){}
    public JsonObject toJson(){
        JsonObject jo = new JsonObject();
        jo.putString("number",this.number);
        jo.putString("cmnd",this.cmnd);
        jo.putString("bankname",bankname);
        jo.putString("bankcode",bankcode);
        jo.putString("name",name);
        jo.putString("error",error);
        jo.putString("desc",desc);
        return jo;
    }
}
