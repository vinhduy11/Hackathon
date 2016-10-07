package com.mservice.momo.web;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by ntunam on 4/7/14.
 */
//HasStateJsonObject
public class Session extends JsonObject {

    private String sessionId;

    public Session(String sessionId, String jsonString) {
        super(jsonString);
        this.sessionId = sessionId;
    }

    public Session(String sessionId, JsonObject json) {
        super(json.toMap());
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public JsonObject toJsonObject() {
        return this;
    }
}
