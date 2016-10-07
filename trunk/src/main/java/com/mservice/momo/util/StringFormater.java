package com.mservice.momo.util;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nam on 10/22/14.
 */
public class StringFormater {
    private String template;
    private Map valuesMap;

    public StringFormater(String templdate) {
        this.template = templdate;
        this.valuesMap = new HashMap<>();
    }

    public StringFormater put(String name, Object value) {
        valuesMap.put(name, String.valueOf(value));
        return this;
    }

    @Override
    public String toString() {
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        return sub.replace(template);
    }
}
