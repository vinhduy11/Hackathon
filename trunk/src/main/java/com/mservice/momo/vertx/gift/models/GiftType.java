package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 9/26/14.
 */
public class GiftType extends MongoModel {

    public static final Integer STATUS_ACTIVE = MomoProto.GiftType.Status.ACTIVE_VALUE;
    public static final Integer STATUS_INACTIVE = MomoProto.GiftType.Status.INACTIVE_VALUE;
    public static final Integer STATUS_DELETED = MomoProto.GiftType.Status.DELETED_VALUE;
    //    public String id;
    public String serviceId;
    public String name;
    public String desc;
    public String icon;
    public String image;
    public Boolean transfer;
    public Integer status; //
    public Date modifyDate;
    public Boolean isNew;
    public JsonArray price;
    public String policy;
    public Boolean visible;

    public GiftType() {
    }

    public GiftType(String id) {
        setModelId(id);
    }

    public GiftType(JsonObject jsonObject) {
        if (jsonObject == null)
            return;
        setValues(jsonObject);
        this.setModelId(jsonObject.getString("_id"));
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();

        if (serviceId != null)
            json.putString("serviceId", serviceId);
        if (name != null)
            json.putString("name", name);
        if (desc != null)
            json.putString("desc", desc);
        if (icon != null)
            json.putString("icon", icon);
        if (image != null)
            json.putString("image", image);
        if (transfer != null)
            json.putBoolean("transfer", transfer);
        if (status != null)
            json.putNumber("status", status);
        if (modifyDate != null)
            json.putNumber("modifyDate", modifyDate.getTime());
        if (isNew != null)
            json.putBoolean("isNew", isNew);
        if (price != null)
            json.putArray("price", price);
        if (policy != null)
            json.putString("policy", policy);

        if(visible !=null){
            json.putBoolean("visible",visible);
        }

        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        Long temp;

        serviceId = savedObject.getString("serviceId");
        name = savedObject.getString("name");
        desc = savedObject.getString("desc");
        icon = savedObject.getString("icon");
        image = savedObject.getString("image");
        transfer = savedObject.getBoolean("transfer");
        status = savedObject.getInteger("status",-1);

        temp = savedObject.getLong("modifyDate");
        if (temp != null)
            modifyDate = new Date(temp);

        isNew = savedObject.getBoolean("isNew");

        price = savedObject.getArray("price");

        policy = savedObject.getString("policy");

        visible = savedObject.getBoolean("visible",true);
    }

    public MomoProto.GiftType toMomoProto() {
        MomoProto.GiftType.Builder builder = MomoProto.GiftType.newBuilder();
        builder.setId(getModelId());
        builder.setServiceId(serviceId);
        builder.setName(name);
        builder.setDesc(desc);
        builder.setIcon(icon);
        builder.setImage(image);
        builder.setTransfer(transfer);
        builder.setModifyDate(modifyDate.getTime());
        builder.setIsNew(isNew);
        for (int i = 0; i < price.size(); i++) {
            builder.addPrice((Long) price.get(i));
        }

        builder.setPolicy(policy);

        if(status.intValue() != GiftType.STATUS_ACTIVE.intValue()){
            builder.setStatus(status.intValue());
        }else{
            boolean show = (visible == null ? true : visible);
            if(show){
                builder.setStatus(GiftType.STATUS_ACTIVE);
            }else{
                builder.setStatus(GiftType.STATUS_INACTIVE);
            }
        }

        return builder.build();
    }

    public boolean isInvalid() {
        return serviceId != null &&
                name != null &&
                desc != null &&
                icon != null &&
                image != null &&
                transfer != null &&
                status != null &&
                modifyDate != null &&
                isNew != null &&
                price != null;
    }
}
