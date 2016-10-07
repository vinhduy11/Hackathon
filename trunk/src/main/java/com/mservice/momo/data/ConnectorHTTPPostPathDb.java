package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created by concu on 1/5/16.
 */
public class ConnectorHTTPPostPathDb {
    private Vertx vertx;

    public ConnectorHTTPPostPathDb(Vertx vertx) {
        this.vertx = vertx;
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

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

    public void findContain(String contain, final Handler<ArrayList<Obj>> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

        JsonObject matcher = new JsonObject();
        JsonObject joRegex = new JsonObject();
        joRegex.putString("$regex", ".*"+contain+".*");
        matcher.putObject(colName.ConnectorHTTPPostPath.SERVICE_ID, joRegex);
        query.putObject(MongoKeyWords.MATCHER, matcher);

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

    public void findOne(String serviceId, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.ConnectorHTTPPostPath.SERVICE_ID, serviceId);
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

    public void removeObj(String id, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

        match.putString(colName.ConnectorHTTPPostPath.SERVICE_ID, id);

        query.putObject(MongoKeyWords.MATCHER, match);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void getAll(final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

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

    public void upsert(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

        JsonObject match = new JsonObject();
        match.putString(colName.ConnectorHTTPPostPath.SERVICE_ID, obj.serviceId);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, obj.toJson());

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void search(final String service, final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ConnectorHTTPPostPath.SERVICE_ID, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();
        if (service != null && !"".equalsIgnoreCase(service)) {
            JsonObject ne = new JsonObject();
            ne.putString(MongoKeyWords.REGEX, service);
            match.putObject(colName.ConnectorHTTPPostPath.SERVICE_ID, ne);
            query.putObject(MongoKeyWords.MATCHER, match);
        }

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void export(final Handler<String> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ConnectorHTTPPostPath.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ConnectorHTTPPostPath.SERVICE_ID, 1);
        query.putObject(MongoKeyWords.SORT, sort);


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<JsonObject> arrayList = null;
                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                // return default value
                callback.handle(results.toString());
            }
        });
    }


    public static class Obj {

        public String serviceId = "";
        public String path = "";
        public String host = "";
        public int port = 0;
        public int version = 0;
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            serviceId = jo.getString(colName.ConnectorHTTPPostPath.SERVICE_ID, "").trim();
            path = jo.getString(colName.ConnectorHTTPPostPath.PATH, "").trim();
            host = jo.getString(colName.ConnectorHTTPPostPath.HOST, "").trim();
            port = jo.getInteger(colName.ConnectorHTTPPostPath.PORT, 0);
            version = jo.getInteger(colName.ConnectorHTTPPostPath.VERSION, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ConnectorHTTPPostPath.SERVICE_ID, serviceId.trim());
            jo.putString(colName.ConnectorHTTPPostPath.PATH, path.trim());
            jo.putString(colName.ConnectorHTTPPostPath.HOST, host.trim());
            jo.putNumber(colName.ConnectorHTTPPostPath.PORT, port);
            jo.putNumber(colName.ConnectorHTTPPostPath.VERSION, version);
            return jo;
        }
    }
}
