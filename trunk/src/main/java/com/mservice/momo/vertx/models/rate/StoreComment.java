package com.mservice.momo.vertx.models.rate;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;

import java.util.Date;

/**
 * Created by nam on 9/12/14.
 */
public class StoreComment extends MongoModel {
    public static final int STATUS_NEW = 0;
    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_REJECTED = 2;

    public Integer storeId;
    public Integer commenter;
    public String commenterName;
    public Integer star;
    public String content;
    public Date modifyDate;
    public Integer status;

    public StoreComment() {
    }

    public MomoProto.StoreComment.Builder toMomoProtoBuilder() {
        MomoProto.StoreComment.Builder builder = MomoProto.StoreComment.newBuilder();
        builder.setCommentId(getModelId() != null ? getModelId() : "");
        builder.setStoreId(storeId != null ? storeId : 0);
        builder.setCommenterPhone(commenter != null ? commenter : 0);
        builder.setCommenterName(commenterName);
        builder.setContent(content != null ? content : "");
        builder.setDate(modifyDate != null ? modifyDate.getTime() : 0);
        builder.setRateStar(star != null ? star : 0);
        builder.setStatus(status != null ? status : -1);

        return builder;
    }

    /**
     * Creating new Comment from Client's request
     *
     * @param c
     */
    public StoreComment(MomoProto.StoreComment c, int commenter, String commenterPhone) {
        this.storeId = c.getStoreId();
        this.commenter = commenter;
        this.commenterName = commenterPhone;
        this.star = c.getRateStar();
        this.content = c.getContent();
        this.modifyDate = new Date();
        this.status = STATUS_NEW;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (storeId != null)
            json.putNumber("storeId", storeId);
        if (commenter != null)
            json.putNumber("commenter", commenter);
        if (star != null)
            json.putNumber("star", star);
        if (content != null)
            json.putString("content", content);
        if (commenterName != null)
            json.putString("commenterName", commenterName);
        if (modifyDate != null)
            json.putNumber("modifyDate", modifyDate.getTime());
        if (status != null)
            json.putNumber("status", status);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        Long temp;
        storeId = savedObject.getInteger("storeId");
        commenter = savedObject.getInteger("commenter");
        commenterName = savedObject.getString("commenterName");
        star = savedObject.getInteger("star");
        content = savedObject.getString("content");
        temp = savedObject.getLong("modifyDate");
        if (temp != null)
            modifyDate = new Date(temp);
        status = savedObject.getInteger("status");
    }

    public boolean isValid() {
        if (storeId == null || star == null || commenter == null || modifyDate == null || status == null || content != null)
            return false;
        if (star < 1 || star > 5)
            return false;
        return true;
    }

    public boolean isValidStatus() {
        if (status == null) return false;
        if (status < 0 || status > 2)
        return false;
        return true;
    }

    public JsonObject toJsonObject() {
        return getPersisFields().putString("commentId", getModelId());
    }

    @Override
    public String toString() {
        return "StoreComment{" +
                "storeId=" + storeId +
                ", commenter=" + commenter +
                ", commenterName='" + commenterName + '\'' +
                ", star=" + star +
                ", content='" + content + '\'' +
                ", modifyDate=" + modifyDate +
                ", status=" + status +
                '}';
    }
}
