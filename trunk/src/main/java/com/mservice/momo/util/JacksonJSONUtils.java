package com.mservice.momo.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by duyhuynh on 14/07/2016.
 */
public class JacksonJSONUtils {

    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    public static <T> T jsonToObj(String object, Class<T> type) {
        try {
            return mapper.readValue(object, type);
        } catch (Exception e) {
            System.out.println("fromValue message failed: " + e.getMessage());
            return null;
        }
    }

    public static <T> T jsonToObj(JsonObject object, Class<T> type) {
        try {
            return mapper.readValue(object.encode(), type);
        } catch (Exception e) {
            System.out.println("fromValue message failed: " + e.getMessage());
            return null;
        }
    }

    public static JsonObject objToJsonObj(Object o) {
        try {
            return new JsonObject(mapper.writeValueAsString(o));
        } catch (Exception e) {
            System.out.println("fromValue message failed: " + e.getMessage());
            return null;
        }
    }

    public static boolean isJSON(String source) {
        try {
            JsonObject jo = new JsonObject(source);
            return jo != null;
        } catch (Exception e) {
            return false;
        }
    }
}
