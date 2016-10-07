package com.mservice.momo.vertx.models.rate;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 9/18/14.
 */
public class CoreAgentRating {
    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_WARNING = 2;

    public String agent;
    public Integer rating;
    public String user;
    public String content;
    public Integer type;

    public CoreAgentRating() {}

    public CoreAgentRating(StoreComment comment) {
        this.agent = "0" + comment.storeId;
        this.rating = comment.star;
        this.user = "0" + comment.commenter;
        this.content = comment.content;
        this.type = TYPE_COMMENT;
    }

    public CoreAgentRating(StoreWarning warning, String content) {
        this.agent = "0" + warning.storeId;
        this.rating = 0;
        this.user = "0" + warning.committer;
        this.content = content;
        this.type = TYPE_WARNING;
    }

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        if (agent != null)
            json.putString("agent", agent);
        if (rating !=null)
            json.putNumber("rating", rating);
        if (user != null)
            json.putString("user", user);
        if (content != null)
            json.putString("content", content);
        if (type !=null)
            json.putNumber("type", type);
        return json;
    }
    public void setValues(JsonObject json) {
        agent = json.getString("agent");
        rating = json.getInteger("rating");
        user = json.getString("user");
        content = json.getString("content");
        type = json.getInteger("type");
    }
}
