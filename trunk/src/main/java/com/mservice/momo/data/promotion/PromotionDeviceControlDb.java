package com.mservice.momo.data.promotion;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.promotion_server.PromotionColName;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 7/25/16.
 */
public class PromotionDeviceControlDb {

    private Vertx vertx;
    private Logger logger;

    public PromotionDeviceControlDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, String program, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, PromotionColName.PromotionDeviceControlCol.TABLE + "_" + program)
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
        query.putString(MongoKeyWords.COLLECTION, PromotionColName.PromotionDeviceControlCol.TABLE + "_" + program);

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
        public int phoneNumber = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {
            id = jo.getString(PromotionColName.PromotionDeviceControlCol.ID, "").trim();
            phoneNumber = jo.getInteger(PromotionColName.PromotionDeviceControlCol.PHONE_NUMBER, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(PromotionColName.PromotionDeviceControlCol.ID, id.trim());
            jo.putNumber(PromotionColName.PromotionDeviceControlCol.PHONE_NUMBER, phoneNumber);
            return jo;
        }
    }
}
