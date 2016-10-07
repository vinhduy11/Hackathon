/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mservice.momo.clientform;

import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 *
 * @author java0111
 */
public class TranslateObject {

    private static final String LABEL_KEY = "key";
    private static final String VALUE_KEY = "value";
    private static final String CONTENT_KEY = "content";
    private final String label;
    private final Map<String, Object> valueLabels;
    private final String content;
    public TranslateObject(JsonObject jsonObject) {
        label = jsonObject.getString(LABEL_KEY, "");
        valueLabels = jsonObject.getObject(VALUE_KEY, new JsonObject()).toMap();
        content = jsonObject.getString(CONTENT_KEY, "");
    }

    public String getLabel() {
        return label;
    }

    public String getValueLabel(String value) {
        if (valueLabels.containsKey(value)) {
            return valueLabels.get(value).toString();
        } else {
            return value;
        }
    }

    public String getContent(){
        return content;
    }
}
