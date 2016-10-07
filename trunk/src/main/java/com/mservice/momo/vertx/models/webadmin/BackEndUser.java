package com.mservice.momo.vertx.models.webadmin;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 9/19/14.
 */
public class BackEndUser extends MongoModel{
    // userName is the key;
    public String password;
    public JsonArray roles;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (password != null)
            json.putString("password", password);
        if (roles != null)
            json.putArray("roles", roles);
        return json;
    }

    public void addRole(String role) {
        if (roles == null)
            roles = new JsonArray();
        if (!roles.contains(role))
            roles.add(role);
    }

    @Override
    public void setValues(JsonObject savedObject) {
        password = savedObject.getString("password");
        roles = savedObject.getArray("roles");
    }
}
