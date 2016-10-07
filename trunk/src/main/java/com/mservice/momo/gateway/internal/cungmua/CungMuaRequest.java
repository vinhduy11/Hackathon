package com.mservice.momo.gateway.internal.cungmua;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by duy.huynh on 09/06/2015.
 */
public class CungMuaRequest {
    public int requestType;
    public int cityId;
    public int categoryId;
    public String accountId;
    public long amount;
    public String transId;
    public String paymentDate;
    public int count;
    public String dealId;

    public JsonObject getJsonObject() {
        return null;
    }
}
