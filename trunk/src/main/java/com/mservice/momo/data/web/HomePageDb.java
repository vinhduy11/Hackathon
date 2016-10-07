package com.mservice.momo.data.web;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by tumegame on 12/01/2016.
 */
public class HomePageDb {

    private Logger logger;
    private EventBus eventBus;

    public HomePageDb(EventBus eventBus, Logger logger) {
        this.logger = logger;
        this.eventBus = eventBus;
    }

    public void removeObj(String id, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.HomeCols.TABLE);

        match.putString(colName.HomeCols.ID, id);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void upsert(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.HomeCols.TABLE);

        JsonObject match = new JsonObject();
        match.putString(colName.HomeCols.ID, obj.id);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, obj.toJsonObject());

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

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

    public void upsertID(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.HomeCols.TABLE);

        JsonObject criteria = new JsonObject();

        String id = obj.id;

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.HomeCols.ID);

        if (id != null && !"".equalsIgnoreCase(id)) {
            criteria.putString(colName.HomeCols.ID, id);
        } else {
            insertDoc(obj.toJsonObject(), new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    callback.handle(aBoolean);
                }
            });
            return;
        }

        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, data);
        query.putObject(MongoKeyWords.OBJ_NEW, update);

        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
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

    public void insertDoc(JsonObject json, final Handler<Boolean> callback) {
        json.removeField(colName.HomeCols.ID);

        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection", colName.HomeCols.TABLE)
                .putObject("document", json);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if (createdId != null) {
                        callback.handle(true);
                        return;
                    }
                }

                callback.handle(false);
            }
        });
    }

    public void getAll(final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.HomeCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.HomeCols.POSITION, 1);
        sort.putNumber(colName.HomeCols.ORDER, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
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

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getByServiceType(final int type, final String service, final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.HomeCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.HomeCols.ORDER, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();

        if (type != 0) {
            match.putNumber(colName.HomeCols.SERVICE_TYPE, type);
            if (service != null && !"".equalsIgnoreCase(service)) {
                match.putString(colName.HomeCols.SERVICE_ID, service);
            }
            query.putObject(MongoKeyWords.MATCHER, match);
            //search all cate
        } else {
            if (service != null && !"".equalsIgnoreCase(service)) {
                JsonObject ne = new JsonObject();
                ne.putString(MongoKeyWords.REGEX, service);
                match.putObject(colName.HomeCols.SERVICE_ID, ne);
                query.putObject(MongoKeyWords.MATCHER, match);
            }
        }
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
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

    public void getById(final String id, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.HomeCols.TABLE);

        JsonObject match = new JsonObject();
        match.putString(colName.HomeCols.ID, id);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                boolean check = false;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    check = true;
                }

                callback.handle(check);
            }
        });
    }

    public void getAllActive(final Handler<String> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.HomeCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.HomeCols.POSITION, 1);
        sort.putNumber(colName.HomeCols.ORDER, 1);
        query.putObject(MongoKeyWords.SORT, sort);


        JsonObject ne = new JsonObject();
        ne.putNumber(MongoKeyWords.NOT_EQUAL, -1);

        JsonObject match = new JsonObject();
        match.putObject(colName.HomeCols.STATUS, ne);

        query.putObject(MongoKeyWords.MATCHER, match);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
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
        public String id = "";
        public int serviceType = 0;
        public int function = 0;
        public String serviceID = "";
        public String textPopup = "";
        public String webUrl = "";
        public String cateId = "";
        public String serviceName = "";
        public String iconUrl = "";
        public Integer status = 0;
        public long lastUpdateTime = 0;
        public int position = 0;
        public int order = 1000;
        public boolean isStore = false;
        public String buttonTitle = "";

        public Obj() {
        }

        public Obj(JsonObject input) {
            id = input.getString(colName.HomeCols.ID, "");
            serviceType = input.getInteger(colName.HomeCols.SERVICE_TYPE, 0);
            function = input.getInteger(colName.HomeCols.FUNC, 0);
            serviceID = input.getString(colName.HomeCols.SERVICE_ID, "");
            textPopup = input.getString(colName.HomeCols.TEXT_POPUP, "");
            webUrl = input.getString(colName.HomeCols.WEB_URL, "");
            cateId = input.getString(colName.HomeCols.CAT_ID, "");
            serviceName = input.getString(colName.HomeCols.SERVICE_NAME, "");
            iconUrl = input.getString(colName.HomeCols.ICON_URL, "");
            status = input.getInteger(colName.HomeCols.STATUS, 0);
            lastUpdateTime = input.getLong(colName.HomeCols.LAST_UPDATE, 0);
            position = input.getInteger(colName.HomeCols.POSITION, 0);
            order = input.getInteger(colName.HomeCols.ORDER, 10000);
            isStore = input.getBoolean(colName.HomeCols.IS_STORE, false);
            buttonTitle = input.getString(colName.HomeCols.BUTTON_TITLE, "");
        }

        public JsonObject toJsonObject() {
            JsonObject output = new JsonObject();

            output.putString(colName.HomeCols.ID, id);
            output.putNumber(colName.HomeCols.SERVICE_TYPE, serviceType);
            output.putNumber(colName.HomeCols.FUNC, function);
            output.putString(colName.HomeCols.SERVICE_ID, serviceID);
            output.putString(colName.HomeCols.TEXT_POPUP, textPopup);
            output.putString(colName.HomeCols.WEB_URL, webUrl);
            output.putString(colName.HomeCols.CAT_ID, cateId);
            output.putString(colName.HomeCols.SERVICE_NAME, serviceName);
            output.putString(colName.HomeCols.ICON_URL, iconUrl);
            output.putNumber(colName.HomeCols.STATUS, status);
            output.putNumber(colName.HomeCols.LAST_UPDATE, lastUpdateTime);
            output.putNumber(colName.HomeCols.POSITION, position);
            output.putNumber(colName.HomeCols.ORDER, order);
            output.putBoolean(colName.HomeCols.IS_STORE, isStore);
            output.putString(colName.HomeCols.BUTTON_TITLE, buttonTitle);
            return output;
        }

        public boolean isInvalid() {
            return true;
        }
    }
}
