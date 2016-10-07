package com.mservice.momo.data.binhtanpromotion;

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
 * Created by khoanguyen on 03/05/2016.
 */
public class DeviceDataUserDb {
    /**
     * Created by concu on 9/8/15.
     */

    private Vertx vertx;
    private Logger logger;

    public DeviceDataUserDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, String program, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.DeviceDataUser.TABLE + "_" + program)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if (!event.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(event.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(String program, JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        logger.info("search with filter ReferralV1");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.DeviceDataUser.TABLE + "_" + program);

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

    public static class Obj {

        public String id = "";
        public String phoneNumber = "";

        public Obj() {
        }

        public Obj(JsonObject jo) {
            id = jo.getString(colName.DeviceDataUser.ID, "").trim();
            phoneNumber = jo.getString(colName.DeviceDataUser.PHONE_NUMBER, "").trim();
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.DeviceDataUser.ID, id.trim());
            jo.putString(colName.DeviceDataUser.PHONE_NUMBER, phoneNumber.trim());
            return jo;
        }
    }
}
