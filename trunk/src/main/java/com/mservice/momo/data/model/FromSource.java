package com.mservice.momo.data.model;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 9/19/14.
 */
public class FromSource {
    private static String RESULT="result";
    private static String SERVICE_SOURCE ="service_source";
    public static class Obj{
        public boolean Result = false;
        public String ServiceSource = "";

        public Obj(){}
        public Obj(JsonObject jo){
            Result = jo.getBoolean(RESULT,false);
            ServiceSource = jo.getString(SERVICE_SOURCE,"");
        }

        public JsonObject toJSON(){
            JsonObject jo = new JsonObject();
            jo.putBoolean(RESULT,Result);
            jo.putString(SERVICE_SOURCE,ServiceSource);
            return jo;
        }
    }
}
