package com.mservice.momo.vertx.models.rate;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 9/13/14.
 */
public class StoreWarningType extends MongoModel {

    public Integer typeId;
    public String name;

    public StoreWarningType() {
    }

    public StoreWarningType(Integer typeId, String name) {
        this.typeId = typeId;
        this.name = name;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (typeId != null)
            json.putNumber("typeId", typeId);
        if (name != null)
            json.putString("name", name);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        typeId = savedObject.getInteger("typeId");
        name = savedObject.getString("name");
    }

    public MomoProto.StoreWarningType.Builder toMomoProtoBuilder() {
        MomoProto.StoreWarningType.Builder builder = MomoProto.StoreWarningType.newBuilder();
        builder.setId(typeId);
        builder.setName(name);
        return builder;
    }
}
