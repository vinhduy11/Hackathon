package com.mservice.momo.vertx.form;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/8/14.
 */
public class FieldData {
    /*"linkto":"droppkg",
    "text":"pro",
    "value":"Gói Doanh nghiệp",
    "parentid":"",
    "id":"pro" //id cua phan tu nay*/
    public String id="";
    public String linkto="";
    public String text ="";
    public String value ="";
    public String parentid="";
    public FieldData(){}
    public JsonObject toJson(){
        JsonObject jo = new JsonObject();
        jo.putString("id",id);
        jo.putString("linkto",linkto);
        jo.putString("text",text);
        jo.putString("value",value);
        jo.putString("parentid",parentid);
        return  jo;
    }

    public FieldData clone(){
        FieldData fd = new FieldData();
        fd.id = this.id;
        fd.linkto = this.linkto;
        fd.text = this.text;
        fd.value = this.value;
        fd.parentid = this.parentid;
        return fd;
    }


    public FieldData(JsonObject jo){

        id = jo.getString("id","");
        linkto = jo.getString("linkto","");
        text = jo.getString("text","");
        value = jo.getString("value","");
        parentid = jo.getString("parentid","");
    }
}
