package com.mservice.momo.vertx.gift.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nam on 10/2/14.
 */
public enum PromoteAction {
    DEMO10("10", "Tặng 10k tiền khuyến mãi"),
    DEMO20("20", "Tặng 20k tiền khuyến mãi"),
    DEMO50("50", "Tặng 50k tiền khuyến mãi");


    private final String codePrefix;
    private final String desc;

    PromoteAction(String codePrefix, String desc) {
        this.codePrefix = codePrefix;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    private static final Map<String, PromoteAction> intToTypeMap = new HashMap<String, PromoteAction>();

    static {
        for (PromoteAction type : PromoteAction.values()) {
            intToTypeMap.put(type.codePrefix, type);
        }
    }

    public static PromoteAction fromPrefix(String prefix) {
        return intToTypeMap.get(prefix);
    }
}
