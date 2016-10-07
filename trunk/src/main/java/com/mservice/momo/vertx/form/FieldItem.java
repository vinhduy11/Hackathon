package com.mservice.momo.vertx.form;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/8/14.
 */
public class FieldItem {
        /*"fieldtype":"text",
        "fieldlabel":"Tài khoản",
        "key":"billid",
        "required":false,
        "isamt",true
        "line":1,
        "haschild":"0" //khong co
        "readonly":true - not edit, false : can edit
        */

    public String fieldtype ="";
    public String fieldlabel="";
    public String key ="";
    public boolean required =false;
    public int line=1;
    public int haschild =0;
    public boolean isamount = false;
    public boolean readonly = false;
    public String value = "";

    public FieldItem clone(){
        FieldItem fi = new FieldItem();
        fi.fieldtype = this.fieldtype;
        fi.fieldlabel = this.fieldlabel;
        fi.key = this.key;
        fi.required = this.required;
        fi.line = this.line;
        fi.haschild = this.haschild;
        fi.isamount = this.isamount;
        fi.readonly = this.readonly;
        fi.value = this.value;
        return fi;
    }

    public FieldItem(){}
    public JsonObject toJson(){
        JsonObject jo = new JsonObject();
        jo.putString("fieldtype",fieldtype);
        jo.putString("fieldlabel",fieldlabel);
        jo.putString("key",key);
        jo.putBoolean("required",required);
        jo.putNumber("line",line);
        jo.putNumber("haschild",haschild);
        jo.putBoolean("isamt",isamount);
        jo.putBoolean("readonly", readonly);
        jo.putString("value",value);
        return jo;
    }
    public FieldItem(JsonObject jo){
        fieldtype = jo.getString("fieldtype","");
        fieldlabel = jo.getString("fieldlabel","");
        key = jo.getString("key","");
        required = jo.getBoolean("required",false);
        line = jo.getInteger("line",1);
        haschild = jo.getInteger("haschild",0);
        isamount = jo.getBoolean("isamt",false);
        readonly = jo.getBoolean("readonly",false);
        value = jo.getString("value","");
    }
}
