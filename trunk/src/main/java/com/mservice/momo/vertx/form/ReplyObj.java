package com.mservice.momo.vertx.form;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created by concu on 11/8/14.
 */
public class ReplyObj {
    public ArrayList<FieldItem> items = new ArrayList<>();
    public ArrayList<FieldData> datas = new ArrayList<>();

    public ReplyObj(JsonObject jo){

        JsonArray jsonArrayFields = jo.getArray("field", new JsonArray());
        JsonArray jsonArrayDatas = jo.getArray("data", new JsonArray());
        for (int i=0;i<jsonArrayFields.size();i++){
            items.add(new FieldItem( (JsonObject)jsonArrayFields.get(i)));
        }

        for (int i=0; i<jsonArrayDatas.size();i++){
            datas.add(new FieldData( (JsonObject) jsonArrayDatas.get(i)));
        }
    }
    public ReplyObj(){}
}
