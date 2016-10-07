package com.mservice.momo.gateway.internal.cungmua;

import org.vertx.java.core.json.JsonObject;

import java.util.List;

/**
 * Created by duy.huynh on 09/06/2015.
 */
public class CungMuaResponse {
    public int requestType;
    public int resultCode = 0;
    public String description = "";

    public List<City> cities;
    public List<Category> categories;

    public String transId;
    public String partnerTransId;
    public JsonObject data;
    public JsonObject pager;

    public JsonObject getJsonObject() {
        return null;
    }
}
