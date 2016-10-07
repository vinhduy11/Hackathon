package com.mservice.momo.vertx.form;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;

/**
 * Created by concu on 11/8/14.
 */
public class RequestObj {
    public String command="";
    public String serviceid ="" ;
    public int nextform=1;
    public String parentid;
    public HashMap<String,String> hashMap = new HashMap<>();
    public int phoneNumber = 0;

    public RequestObj(){}

    public JsonObject toJson(){
        JsonObject jo = new JsonObject();
        jo.putString("cmd",command);
        jo.putString("serviceid",serviceid);
        jo.putNumber("nextform", nextform);
        jo.putString("parentid",parentid);
        jo.putNumber("number",phoneNumber);

        JsonArray array = new JsonArray();
        for (String s : hashMap.keySet()) {
            JsonObject json = new JsonObject();
            json.putString(s, hashMap.get(s));
            array.add(json);
        }
        jo.putArray("arr",array);
        return jo;
    }
    public RequestObj(JsonObject jo){
        serviceid = jo.getString("serviceid","");
        nextform = jo.getInteger("nextform",1);
        parentid = jo.getString("parentid","");
        command = jo.getString("cmd","");
        phoneNumber = jo.getInteger("number",0);
        hashMap = new HashMap<>();

        JsonArray jsonArray = jo.getArray("arr",new JsonArray());
        for(int i =0;i< jsonArray.size();i++){
            JsonObject j = jsonArray.get(i);

            for (String s : j.getFieldNames()) {
                hashMap.put(s,j.getString(s));
                break;
            }
        }
    }
}
