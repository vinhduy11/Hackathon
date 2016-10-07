package com.mservice.momo.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by MacBook on 4/1/15.
 */
public class MomoJackJsonPack {

    private static com.fasterxml.jackson.databind.ObjectMapper mMapper;

    static {
        mMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> T jsonToObj(String object, Class<T> type) {
        try {
            return mMapper.readValue(object, type);
        } catch (Exception e) {
            System.out.println("parse message failed: " + e.getMessage());
            return null;
        }
    }

    public static String objToString(Object o) {
        try {
            return mMapper.writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("parse message failed: " + e.getMessage());
            return null;
        }
    }
}
