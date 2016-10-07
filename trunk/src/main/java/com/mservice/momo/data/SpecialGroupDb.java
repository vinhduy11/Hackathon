package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 6/2/16.
 */
public class SpecialGroupDb {

    private Vertx vertx;
    private Logger logger;

    public SpecialGroupDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.SpecialGroupCol.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        Obj obj = new Obj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void findOne(String phoneNumber, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.SpecialGroupCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.SpecialGroupCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public static class Obj {

        public String phone_number = "";

        public String group_id = "";

        public String group_name = "";

        public String discount = "";

        public String receiver_number = "";

        public Obj() {
        }

        public Obj(JsonObject jo) {

            phone_number = jo.getString(colName.SpecialGroupCol.PHONE_NUMBER, "");
            group_id = jo.getString(colName.SpecialGroupCol.GROUP_ID, "");
            group_name = jo.getString(colName.SpecialGroupCol.GROUP_NAME, "");
            discount = jo.getString(colName.SpecialGroupCol.DISCOUNT, "");
            receiver_number = jo.getString(colName.SpecialGroupCol.RECEIVER_NUMBER, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

             jo.putString(colName.SpecialGroupCol.PHONE_NUMBER, phone_number);
             jo.putString(colName.SpecialGroupCol.GROUP_ID, group_id);
             jo.putString(colName.SpecialGroupCol.GROUP_NAME, group_name);
             jo.putString(colName.SpecialGroupCol.DISCOUNT, discount);
             jo.putString(colName.SpecialGroupCol.RECEIVER_NUMBER, receiver_number);

            return jo;
        }
    }



}
