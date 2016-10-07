package com.mservice.momo.gateway.internal.connectorproxy;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 10/6/14.
 */
public class ViaConnectorObj {

    public static String IS_NAME_CHECKED = "isNamedChecked";
    public String BusName="";
    public String BillPay ="";
    public boolean IsViaConnectorVerticle=false;
    public int type = 0;
    public boolean isNamedChecked = false;
    public ViaConnectorObj(){}
    public JsonObject toJson(){
        JsonObject jo = new JsonObject();
        jo.putString("busname",BusName);
        jo.putString("billpay",BillPay);
        jo.putBoolean("viacore",IsViaConnectorVerticle);
        jo.putBoolean(IS_NAME_CHECKED, isNamedChecked);
        return jo;
    }
    public ViaConnectorObj(JsonObject jo){
        BusName = jo.getString("busname","");
        BillPay = jo.getString("billpay","");
        IsViaConnectorVerticle = jo.getBoolean("viacore",false);
        isNamedChecked = jo.getBoolean(IS_NAME_CHECKED, false);
    }
}
