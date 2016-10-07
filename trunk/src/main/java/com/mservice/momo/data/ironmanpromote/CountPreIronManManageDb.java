package com.mservice.momo.data.ironmanpromote;

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
 * Created by concu on 9/19/15.
 */
public class CountPreIronManManageDb {


    /**
     * Created by concu on 9/8/15.
     */

        private Vertx vertx;
        private Logger logger;

        public CountPreIronManManageDb(Vertx vertx, Logger logger) {
            this.logger = logger;
            this.vertx = vertx;
        }

        public void insert(final Obj obj, final Handler<Integer> callback) {

            JsonObject query = new JsonObject();
            query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                    .putString(MongoKeyWords.COLLECTION, colName.CountPreIronManManage.TABLE)
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

        public void upsertPartial(String group
                , JsonObject joUpdate, final Handler<Boolean> callback) {

            JsonObject query = new JsonObject();
            query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
            query.putString(MongoKeyWords.COLLECTION, colName.CountPreIronManManage.TABLE);
            JsonObject match = new JsonObject();

            //matcher
            match.putString(colName.CountPreIronManManage.GROUP, group);
            query.putObject(MongoKeyWords.CRITERIA, match);


            JsonObject fieldsSet = new JsonObject();
            fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

            query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
            query.putBoolean(MongoKeyWords.UPSERT, true);

            vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> jsonObjectMessage) {
                    JsonObject obj = jsonObjectMessage.body();
                    boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                    callback.handle(result);
                }
            });
        }

        public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

            //query
            JsonObject query = new JsonObject();
            query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
            query.putString(MongoKeyWords.COLLECTION, colName.CountPreIronManManage.TABLE);

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

        public void findOne(String group, final Handler<Obj> callback) {

            //query
            JsonObject query = new JsonObject();
            query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
            query.putString(MongoKeyWords.COLLECTION, colName.CountPreIronManManage.TABLE);

            JsonObject matcher = new JsonObject();
            matcher.putString(colName.CountPreIronManManage.GROUP, group);
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

            public String group = "";

            public int total = 0;

            public Obj() {
            }

            public Obj(JsonObject jo) {

                group = jo.getString(colName.CountPreIronManManage.GROUP, "").trim();
                total = jo.getInteger(colName.CountPreIronManManage.TOTAL, 0);


            }

            public JsonObject toJson() {
                JsonObject jo = new JsonObject();

                jo.putString(colName.CountPreIronManManage.GROUP, group.trim());
                jo.putNumber(colName.CountPreIronManManage.TOTAL, total);


                return jo;
            }
        }

}
