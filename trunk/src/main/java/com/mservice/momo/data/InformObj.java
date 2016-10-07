package com.mservice.momo.data;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 2/2/15.
 */
public class InformObj {

    public static final String Cap ="cap";
    public static final String Body ="body";
    public static final String Sms ="sms";

    public String cap ="";
    public String body ="";
    public String sms="";
    public InformObj(JsonArray jo){
        cap = ((JsonObject)jo.get(0)).getString("cap","");
        body =((JsonObject)jo.get(1)).getString("body","");
        sms = ((JsonObject)jo.get(2)).getString("sms","");
    }
    public InformObj(){}
}
