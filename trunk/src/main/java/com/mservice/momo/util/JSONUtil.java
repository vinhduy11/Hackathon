package com.mservice.momo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStore;
import org.boon.core.reflection.MapperSimple;
import org.boon.core.reflection.fields.FieldAccessMode;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by duyhv on 12/7/2015.
 */
public class JSONUtil {
    static org.boon.core.reflection.MapperSimple mapMapper =  new MapperSimple(FieldAccessMode.FIELD.create(true));
    static ObjectMapper objMapper = JsonFactory.create();
    static com.fasterxml.jackson.databind.ObjectMapper jackMapper = new com.fasterxml.jackson.databind.ObjectMapper();


    public static <T> T fromStrToObj(String json, Class<T> type){
        try {
            return jackMapper.readValue(json, type);
        } catch (Exception ex) {
            return null;
        }
    }

    public static String fromObjToStr(Object obj){
        try {
            return jackMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonObject fromObjToJsonObj(Object obj){
        return new JsonObject(mapMapper.toMap(obj));
    }

    public static <T> T fromJsonObjToObj(JsonObject json, Class<T> type){
        return mapMapper.fromMap(convertJsonToHashMap(json.toString()), type);
    }

    private static Map<String, Object> convertJsonToHashMap(String t) {

        HashMap<String, Object> map = new HashMap<String, Object>();
        JsonObject jObject = new JsonObject(t);
        Set<String> set = jObject.getFieldNames();
        String value = "";
        for (String fieldName : set) {
            value = String.valueOf(jObject.getValue(fieldName));
            map.put(fieldName, value);
        }
        return map;
    }

    public static void main(String[] args) {
        JsonObject jo = new JsonObject();
        jo.putNumber("ID", 1);
        jo.putString("NAME", "NAME");
        JSONUtil.fromObjToStr(jo);
        MStore st = JSONUtil.fromJsonObjToObj(jo, MStore.class);
    }
}
