package com.mservice.momo.data.zalo;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 2/3/16.
 */
public class ZaloNotiObj {

    private String phoneNumber = "";
    private String group = "";
    private JsonObject jsonData;

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String _phoneNumber)
    {
        phoneNumber = _phoneNumber;
    }

    public JsonObject getJsonData()
    {
        if(jsonData == null)
        {
            jsonData = new JsonObject();
        }
        return jsonData;
    }

    public void setJsonData(JsonObject _jsonData)
    {
        jsonData = _jsonData;
    }
    public String getGroup()
    {
        return group;
    }

    public void setGroup(String _group)
    {
        group = _group;
    }
}
