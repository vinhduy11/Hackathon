package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by KhoaNguyen on 8/25/2016.
 */
public class NotificationToolDb {

    Vertx vertx;
    Logger logger;

    //BEGIN 0000000004
    public NotificationToolDb(Vertx vertx, Logger logger){
        this.vertx = vertx;
        this.logger = logger;
    }
    public void upsertNotification(final int phone, final JsonObject json, final Handler<String> callback) {
        if (json.getFieldNames().size() == 0) {
            return;
        }

        //remove unused fields
//        json.removeField("_id");
//        json.removeField("sms");
//        json.removeField("token");
//        json.removeField("bodyIOS");
//        json.removeField("os");
//        json.removeField("btnTitle");

        logger.info(String.format("Save %d notification: %s", phone, json));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, "noti_tool");

        JsonObject criteria = new JsonObject();
        long tranId = 1000000L;
        criteria.putNumber("tranId", tranId);

        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, json);
        query.putObject(MongoKeyWords.OBJ_NEW, update);

        query.putBoolean(MongoKeyWords.UPSERT, true);
//        JsonObject query = new JsonObject()
//                .putString(MongoKeyWords.ACTION, MongoKeyWords.UPSERT)
//                .putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone)
//                .putObject(MongoKeyWords.DOCUMENT, json);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                final JsonObject jo = json;
                if (event.body() != null) {
                    String createdId = event.body().getString(MongoKeyWords.UPSERTED_ID, null);
                    if (createdId != null) {
                        if (createdId == "") {
                            createdId = "0";
                        }
                        callback.handle(createdId);
                        logger.info(String.format("Saved %d notification id: %s.", phone, createdId));
                        return;
                    }
                }
                logger.info(String.format("Save %d notification result: fail", phone));
                callback.handle(null);
            }
        });
    }
    //END 0000000004


    public void getAllNotification(int phone, Long startTime, List<Integer> statusValues, final Handler<List<Notification>> callback) {

        if (startTime == null) {
            startTime = 0L;
        }

        //khong phai du lieu dung roi --> hack chang ????
        if (startTime < 0) {
            logger.info("Sao lai co gia tri startTime Am: " + startTime);
            callback.handle(null);
            return;
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, "noti_tool");

        JsonObject matcher = new JsonObject();

        JsonObject timeFilter = new JsonObject();
        timeFilter.putNumber(MongoKeyWords.GREATER, startTime); // chi lay du lieu lon hon

        matcher.putObject("time", timeFilter);
        matcher.putNumber("receiverNumber", phone);
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.NotificationCols.TIME, -1);
        query.putObject("sort", sort);

        JsonObject notDeleted = new JsonObject()
                .putNumber(MongoKeyWords.NOT_EQUAL, Notification.STATUS_DELETED);

        matcher.putObject("status", notDeleted);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 120);

        //fields not returned to client
        JsonObject fields = new JsonObject();
        fields.putNumber("sms", 0);
        fields.putNumber("token", 0);
        fields.putNumber("os", 0);
        fields.putNumber("sms", 0);
        fields.putNumber("bodyIOS", 0);
        fields.putNumber("prefix", 0);

        query.putObject(MongoKeyWords.KEYS, fields);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                ArrayList<Notification> finalResult = new ArrayList<Notification>();

                JsonArray results = event.body().getArray("results", new JsonArray());

                if (results != null && results.size() > 0) {
                    finalResult = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject jsonModel = results.get(i);
                        Notification model = Notification.parse(jsonModel);
                        finalResult.add(model);
                    }
                }

                callback.handle(finalResult);

            }
        });
    }

    public void getAllUnReceiveNotification(Long startTime, int status, final Handler<List<Notification>> callback) {

        if (startTime == null) {
            startTime = 0L;
        }

        //khong phai du lieu dung roi --> hack chang ????
        if (startTime < 0) {
            logger.info("Sao lai co gia tri startTime Am: " + startTime);
            callback.handle(null);
            return;
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, "noti_tool");

        JsonObject matcher = new JsonObject();

        JsonObject timeFilter = new JsonObject();
        timeFilter.putNumber(MongoKeyWords.GREATER, startTime); // chi lay du lieu lon hon

        matcher.putObject("time", timeFilter);
        matcher.putNumber("status", status);
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.NotificationCols.TIME, -1);
        query.putObject("sort", sort);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 120);

        //fields not returned to client
        JsonObject fields = new JsonObject();
        fields.putNumber("sms", 0);
        fields.putNumber("token", 0);
        fields.putNumber("os", 0);
        fields.putNumber("sms", 0);
        fields.putNumber("bodyIOS", 0);
        fields.putNumber("prefix", 0);

        query.putObject(MongoKeyWords.KEYS, fields);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                ArrayList<Notification> finalResult = new ArrayList<Notification>();

                JsonArray results = event.body().getArray("results", new JsonArray());

                if (results != null && results.size() > 0) {
                    finalResult = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject jsonModel = results.get(i);
                        Notification model = Notification.parse(jsonModel);
                        finalResult.add(model);
                    }
                }

                callback.handle(finalResult);

            }
        });
    }

    public void upsertPartial(String _id
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, "noti_tool");
        JsonObject match = new JsonObject();

        //matcher
        match.putString("_id", _id);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void updateNotifications(final int phone, final List<Notification> models, final Handler<List<Notification>> callback) {
        final ArrayList<Notification> updatedNotifications = new ArrayList<>();
        long timerId = vertx.setPeriodic(100, new Handler<Long>() {
            int i = 0;
            boolean doNext = true;

            @Override
            public void handle(Long timerId) {
                if (!doNext)
                    return;
//                System.out.println(".");
                if (i < models.size()) {
                    doNext = false;

                    final Notification updating = models.get(i);
                    updateNotification(phone, updating, new Handler<Integer>() {
                        @Override
                        public void handle(Integer error) {
                            doNext = true;
                            i++;
                            if (error == 0) {
                                updatedNotifications.add(updating);
                            }
                        }
                    });
                } else {
                    logger.trace(String.format("Cancel timer(id:%d)", timerId));
                    vertx.cancelTimer(timerId);
                    callback.handle(updatedNotifications);
                }
            }
        });
        logger.trace(String.format("Create timer(id:%d) for updating multi %d's notifications", timerId, phone));
    }

    public void updateNotification(final int phone, final Notification model, final Handler<Integer> callback) {


        JsonObject updateValues = model.toJsonObject();
        updateValues.removeField("_id");

        logger.debug(String.format("Update %d notification{id: %s}value: %s", phone, model.id, updateValues));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, "noti_tool");
        JsonObject criteria = new JsonObject()
                .putString("_id", model.id).putNumber("receiverNumber", phone);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, updateValues);
        query.putObject(MongoKeyWords.OBJ_NEW, set);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS
                , query
                , new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(0);
            }
        });
    }

    public void getAllNotificationForSync(int phone, Long startTime, List<Integer> statusValues, final Handler<List<Notification>> callback) {

        if (startTime == null) {
            startTime = 0L;
        }

        //khong phai du lieu dung roi --> hack chang ????
        if (startTime < 0) {
            logger.info("Sao lai co gia tri startTime Am: " + startTime);
            callback.handle(null);
            return;
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, "noti_tool");

        JsonObject matcher = new JsonObject();

        JsonObject timeFilter = new JsonObject();
        timeFilter.putNumber(MongoKeyWords.GREATER, startTime); // chi lay du lieu lon hon

        matcher.putObject("time", timeFilter);

        JsonObject sort = new JsonObject();
        sort.putNumber(colName.NotificationCols.TIME, -1);
        query.putObject("sort", sort);

        JsonObject notDeleted = new JsonObject()
                .putNumber(MongoKeyWords.NOT_EQUAL, Notification.STATUS_DELETED);

        matcher.putObject("status", notDeleted);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 120);

        //fields not returned to client
        JsonObject fields = new JsonObject();
        fields.putNumber("sms", 0);
        fields.putNumber("token", 0);
        fields.putNumber("os", 0);
        fields.putNumber("sms", 0);
        fields.putNumber("bodyIOS", 0);
        fields.putNumber("prefix", 0);

        query.putObject(MongoKeyWords.KEYS, fields);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                ArrayList<Notification> finalResult = null;

                JsonArray results = event.body().getArray("results", null);

                if (results != null && results.size() > 0) {
                    finalResult = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject jsonModel = results.get(i);
                        Notification model = Notification.parseEncryptNoti(jsonModel);
                        finalResult.add(model);
                    }
                }

                callback.handle(finalResult);

            }
        });
    }



}
