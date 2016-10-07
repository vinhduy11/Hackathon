package com.mservice.momo.vertx.models.rate;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

/**
 * Created by nam on 9/12/14.
 */
public class StoreStar extends MongoModel {

    public Integer storeId;
    public Integer s1;
    public Integer s2;
    public Integer s3;
    public Integer s4;
    public Integer s5;

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (storeId != null)
            json.putNumber("storeId", storeId);
        if (s1 != null)
            json.putNumber("s1", s1);
        if (s2 != null)
            json.putNumber("s2", s2);
        if (s3 != null)
            json.putNumber("s3", s3);
        if (s4 != null)
            json.putNumber("s4", s4);
        if (s5 != null)
            json.putNumber("s5", s5);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        storeId = savedObject.getInteger("storeId");
        s1 = savedObject.getInteger("s1");
        s2 = savedObject.getInteger("s2");
        s3 = savedObject.getInteger("s3");
        s4 = savedObject.getInteger("s4");
        s5 = savedObject.getInteger("s5");
    }

    public MomoProto.StoreRateInfo toMomoProto() {
        MomoProto.StoreRateInfo.Builder builder = MomoProto.StoreRateInfo.newBuilder()
                .setStoreId(storeId);
        if (s1 == null)
            builder.setS1(0);
        else
            builder.setS1(s1);

        if (s2 == null)
            builder.setS2(0);
        else
            builder.setS2(s2);

        if (s3 == null)
            builder.setS3(0);
        else
            builder.setS3(s3);

        if (s4 == null)
            builder.setS4(0);
        else
            builder.setS4(s4);

        if (s5 == null)
            builder.setS5(0);
        else
            builder.setS5(s5);

        return builder.build();
    }

    public void increaseStar(int star, int value) {
        switch (star) {
            case 1:
                if (s1 == null) s1 = 0;
                s1 += value;
                break;
            case 2:
                if (s2 == null) s2 = 0;
                s2 += value;
                break;
            case 3:
                if (s3 == null) s3 = 0;
                s3 += value;
                break;
            case 4:
                if (s4 == null) s4 = 0;
                s4 += value;
                break;
            case 5:
                if (s5 == null) s5 = 0;
                s5 += value;
                break;
            default:
                throw new IllegalAccessError();
        }
    }

    @Override
    public String toString() {
        return "StoreStar{" +
                "storeId=" + storeId +
                ", s1=" + s1 +
                ", s2=" + s2 +
                ", s3=" + s3 +
                ", s4=" + s4 +
                ", s5=" + s5 +
                '}';
    }
}
