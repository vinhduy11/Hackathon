package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 10/3/14.
 */
public class UserSetting extends MongoModel {

    // phone is model id;

    public Boolean useMpoint;
    private static UserSetting defaultSetting;


    public UserSetting() {
    }

    public UserSetting(MomoProto.UserSetting proto) {
        useMpoint = proto.getUseMpoint();
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (useMpoint != null)
            json.putBoolean("useMpoint", useMpoint);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        useMpoint = savedObject.getBoolean("useMpoint");
    }

    public static UserSetting getDefaultSetting() {
        UserSetting setting = new UserSetting();
        setting.useMpoint = true;
        return setting;
    }

    public MomoProto.UserSetting.Builder toMomoProto() {
        MomoProto.UserSetting.Builder builder = MomoProto.UserSetting.newBuilder();
        if (useMpoint != null)
            builder.setUseMpoint(useMpoint);
        return builder;
    }
}
