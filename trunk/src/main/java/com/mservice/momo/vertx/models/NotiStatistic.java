package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 9/18/14.
 */
public class NotiStatistic extends MongoModel {

    private Integer receive;
    private Integer view;
    private Integer tran;

    public NotiStatistic() {
    }

    public NotiStatistic(Integer receive, Integer view, Integer tran) {
        this.receive = receive;
        this.view = view;
        this.tran = tran;
    }

    public NotiStatistic(String broadcastNotiId) {
        super(broadcastNotiId);
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (receive != null)
            json.putNumber("receive", receive);
        if (view != null)
            json.putNumber("view", view);
        if (tran != null)
            json.putNumber("tran", tran);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        receive = savedObject.getInteger("receive");
        view = savedObject.getInteger("view");
        tran = savedObject.getInteger("tran");
    }
}
