package com.mservice.momo.vertx.retailer;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/19/14.
 */
public class FeeObj {

    public static final String get_fee ="get_fee";

    public String command ="";
    public int tranType =0;
    public long from = 0;
    public long to = 0;
    public long fee = 0;
    public long tranAmount = 0;
    public int phoneNumber =0;

    public JsonObject toJson(){
        JsonObject jo = new JsonObject();
        jo.putNumber("trantype",tranType);
        jo.putNumber("from",from);
        jo.putNumber("to",to);
        jo.putNumber("fee",fee);
        jo.putString("cmd",command);
        jo.putNumber("tranamt",tranAmount);
        jo.putNumber("number",phoneNumber);
        return jo;
    }
    public FeeObj(){}
    public FeeObj(JsonObject jo){
        tranType = jo.getInteger("trantype",0);
        from = jo.getLong("from",0);
        to = jo.getLong("to",0);
        fee = jo.getLong("fee",0);
        command = jo.getString("cmd","");
        tranAmount = jo.getLong("tranamt",0);
        phoneNumber = jo.getInteger("number",0);
    }
}
