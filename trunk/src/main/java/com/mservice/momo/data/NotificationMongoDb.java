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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by duyhuynh on 07/07/2016.
 */
public class NotificationMongoDb implements NotificationDb {

    private Vertx vertx;
    private Logger logger;
    private NotificationMySQLDb mySQLDb;
    private boolean isRepli = false;

    public NotificationMongoDb(Vertx vertx, Logger logger, JsonObject glbConfig, boolean isReplication) {
        this.vertx = vertx;
        this.logger = logger;
        isRepli = isReplication;
        mySQLDb = new NotificationMySQLDb(vertx, logger, glbConfig);
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



//    public void updateNotification(final int phone, final Notification model, final Handler<Integer> callback) {
//
//    }

    public void updateNotification(final int phone, final Notification model, final Handler<Integer> callback) {


        JsonObject updateValues = model.toJsonObject();
        updateValues.removeField("_id");

        logger.debug(String.format("Update %d notification{id: %s}value: %s", phone, model.id, updateValues));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone);
        JsonObject criteria = new JsonObject()
                .putString("_id", model.id);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, updateValues);
        query.putObject(MongoKeyWords.OBJ_NEW, set);

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS
                , query
                , new Handler<Message<JsonObject>>() {

                    @Override
                    public void handle(Message<JsonObject> event) {
                        if (isRepli) {
                            mySQLDb.updateNotification(phone, model, new Handler<Integer>() {
                                @Override
                                public void handle(Integer integer) {
                                }
                            });
                        }
                        callback.handle(0);
                    }
                });
    }
    /*public void updateNotificationOld(final int phone, final Notification model, final Handler<Integer> callback) {


        JsonObject updateValues = model.toJsonObject();
        updateValues.removeField("_id");

        logger.debug(String.format("Update %d notification{id: %s}value: %s", phone, model.id, updateValues));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);

        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone);
        JsonObject matcher = new JsonObject()
                .putString("_id", model.id);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, updateValues);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);
        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> event) {
                callback.handle(0);
            }
        });
    }*/

    public void saveNotification(final int phone, final JsonObject json, final Handler<String> callback) {
        if (json.getFieldNames().size() == 0) {
            return;
        }

        //remove unused fields
        json.removeField("_id");
        json.removeField("sms");
        json.removeField("token");
        json.removeField("bodyIOS");
        json.removeField("os");
//        json.removeField("btnTitle");

        logger.debug(String.format("Save %d notification: %s", phone, json));

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone)
                .putObject(MongoKeyWords.DOCUMENT, json);

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> event) {
                if (isRepli) {
                    mySQLDb.saveNotification(phone, json, new Handler<String>() {
                        @Override
                        public void handle(String s) {
                        }
                    });
                }
                final JsonObject jo = json;
                if (event.body() != null) {
                    String createdId = event.body().getString(MongoKeyWords.UPSERTED_ID, null);
                    if (createdId != null) {
                        callback.handle(createdId);
                        logger.debug(String.format("Saved %d notification id: %s.", phone, createdId));
                        return;
                    }
                }
                logger.debug(String.format("Save %d notification result: fail", phone));
                callback.handle(null);
            }
        });
    }

    //BEGIN 0000000004
    public void upsertNotification(final int phone, final JsonObject json, final Handler<String> callback) {
        if (json.getFieldNames().size() == 0) {
            return;
        }

        //remove unused fields
        json.removeField("_id");
        json.removeField("sms");
        json.removeField("token");
        json.removeField("bodyIOS");
        json.removeField("os");
//        json.removeField("btnTitle");

        logger.debug(String.format("Save %d notification: %s", phone, json));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone);

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
        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (isRepli) {
                    mySQLDb.upsertNotification(phone, json, new Handler<String>() {
                        @Override
                        public void handle(String s) {
                        }
                    });
                }
                final JsonObject jo = json;
                if (event.body() != null) {
                    String createdId = event.body().getString(MongoKeyWords.UPSERTED_ID, null);
                    if (createdId != null) {
                        if (createdId == "") {
                            createdId = "0";
                        }
                        callback.handle(createdId);
                        logger.debug(String.format("Saved %d notification id: %s.", phone, createdId));
                        return;
                    }
                }
                logger.debug(String.format("Save %d notification result: fail", phone));
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
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone);

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

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                ArrayList<Notification> finalResult = null;

                JsonArray results = event.body().getArray("results", null);

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
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone);

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

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
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

    public void getNotiCollectionName(final Handler<List<String>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "getCollections");

        query.putNumber(MongoKeyWords.BATCH_SIZE, 1000000);

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray a = event.body().getArray("collections");
                List<String> collNames = new ArrayList<String>();
                for (Object o : a) {
                    if (o.toString().startsWith("noti_")) {
                        collNames.add(o.toString());
                    }
                }
                callback.handle(collNames);
            }
        });
    }

    public void findNotiOld(final long start, final long end, final String collName, final Handler<JsonArray> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "find");
        query.putString(MongoKeyWords.COLLECTION, collName);
        JsonObject matcher = new JsonObject();
        JsonObject timeFilter = new JsonObject();
        timeFilter.putNumber(MongoKeyWords.GREATER_OR_EQUAL, start);
        timeFilter.putNumber(MongoKeyWords.LESS_OR_EQUAL, end);
        matcher.putObject("time", timeFilter);
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000000);

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body().getArray("results"));
            }
        });
    }

    public void deleteNotiOld(final long lastTime, final String collName, final Handler<JsonArray> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "delete");
        query.putString(MongoKeyWords.COLLECTION, collName);
        JsonObject matcher = new JsonObject();
        JsonObject timeFilter = new JsonObject();
        timeFilter.putNumber(MongoKeyWords.LESS_OR_EQUAL, lastTime);
        matcher.putObject("time", timeFilter);
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000000);

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                System.out.println(event.body());
                callback.handle(event.body().getArray("results"));
            }
        });
    }

    public void writeJsonFile(final String fileName, final String json, final Handler<Boolean> callback) {
        BufferedOutputStream bos = null;
        boolean result = false;
        try {
            File file = new File("./backup_noti");
            if (!file.exists()) {
                if (file.mkdir()) {
                    System.out.println("Directory backup_noti is created!");
                } else {
                    System.out.println("Failed to create directory backup_noti!");
                }
            }
            bos = new BufferedOutputStream(new FileOutputStream("./backup_noti/" + fileName));
            bos.write(json.getBytes());
            bos.flush();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        callback.handle(result);
    }

    public void getAllNotificationPage(int phone, int pageSize, int pageNum, final Long startTime, List<Integer> statusValues, final Handler<List<Notification>> callback) {
        JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", COLLECTION_NAME_PREFIX + phone);

        JsonObject timeFilter = null;
        if (startTime != null) {
            timeFilter = new JsonObject();
            timeFilter.putNumber("$gte", startTime);
        }
//        if (endTime != null) {
//            if (timeFilter == null) timeFilter = new JsonObject();
//            timeFilter.putNumber("$lte", endTime);
//        }

        query.putNumber("skip", (pageNum - 1) * pageSize);
        query.putNumber("limit", pageSize);

        JsonObject sort = new JsonObject();
        sort.putNumber(colName.NotificationCols.TIME, -1);
        query.putObject("sort", sort);

        JsonArray jsonStatusValues = new JsonArray();
        for (Integer i : statusValues) {
            jsonStatusValues.add(i);
        }

        JsonObject in = new JsonObject()
                .putArray(MongoKeyWords.IN_$, jsonStatusValues);
        JsonObject exists = new JsonObject()
                .putBoolean("$exists", false);
        JsonObject statusIn = new JsonObject()
                .putObject("status", in);
        JsonObject statusNotExist = new JsonObject()
                .putObject("status", exists);

        JsonArray ids = new JsonArray()
//                .add(null)
                .add("");
        JsonObject notInIds = new JsonObject()
                .putArray("$nin", ids);
        JsonObject idNotIn = new JsonObject()
                .putObject("_id", notInIds);

        JsonArray or = new JsonArray()
                .add(statusIn)
                .add(statusNotExist)
                .add(idNotIn);

        JsonObject matcher = new JsonObject();
        matcher.putArray("$or", or);

        if (timeFilter != null) {
            matcher.putObject("time", timeFilter);
        }

        query.putObject("matcher", matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject obj = event.body();

                ArrayList<Notification> finalResult = new ArrayList<Notification>();

                if (obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject jsonModel = (JsonObject) results.get(i);
                            Notification model = Notification.parse(jsonModel);
                            finalResult.add(model);
                        }
                    }

                    callback.handle(finalResult);
                } else {
                    callback.handle(null);
                }
            }
        });
    }

    public void find(int number, String notiId, final Handler<Notification> callback) {
        JsonObject matcher = new JsonObject().putString("_id", notiId);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + number);
        query.putObject(MongoKeyWords.MATCHER, matcher);


        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result", null);
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }

                if (callback != null) {
                    Notification m = Notification.parse(result);
                    callback.handle(m);
                }
            }
        });
    }

    public void findNotiByTranId(int number, long tranId, final Handler<Notification> callback) {
        JsonObject matcher = new JsonObject().putNumber("tranId", tranId);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + number);
        query.putObject(MongoKeyWords.MATCHER, matcher);


        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result", null);
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }

                if (callback != null) {
                    Notification m = Notification.parse(result);
                    callback.handle(m);
                }
            }
        });
    }

    //trim data for bignoti.start

    public void getTime(final String collectName, int pageSize, int limit, final Handler<Long> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, collectName);

        JsonObject matcher = new JsonObject();

        JsonObject sort = new JsonObject();
        sort.putNumber(colName.NotificationCols.TIME, -1);
        query.putObject("sort", sort);


        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putNumber(MongoKeyWords.BATCH_SIZE, pageSize);

        //fields not returned to client
        JsonObject fields = new JsonObject();
        fields.putNumber(colName.NotificationCols.TIME, 1);
        fields.putNumber("_id", 0);
        query.putObject(MongoKeyWords.KEYS, fields);


        query.putNumber(MongoKeyWords.SKIP, pageSize - limit);
        query.putNumber(MongoKeyWords.LIMIT, limit);

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject obj = event.body();

                long lastTime = 0;

                if (obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    if (results != null && results.size() > 0) {
                        lastTime = ((JsonObject) results.get(0)).getLong(colName.NotificationCols.TIME, 0);
                    }

                }

                callback.handle(lastTime);
            }
        });

    }

    //trim data for bignoti.end

    public void removeOldRecs(final String collectName, final long fromTime, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, collectName);

        JsonObject less = new JsonObject();
        less.putNumber(MongoKeyWords.LESS_OR_EQUAL, fromTime);

        match.putObject(colName.NotificationCols.TIME, less);
        query.putObject(MongoKeyWords.MATCHER, match);

        //db.products.remove( { qty: { $gt: 20 } } )

        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> result) {
                if (isRepli) {
                    mySQLDb.removeOldRecs(collectName, fromTime, new Handler<Integer>() {
                        @Override
                        public void handle(Integer integer) {
                        }
                    });
                }
                int count = result.body().getInteger("number", 0);
                callback.handle(count);
            }
        });
    }

    public void clearAllNoti(final int phone, final Handler<JsonObject> handler) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "dropCollection");
        query.putString(MongoKeyWords.COLLECTION, COLLECTION_NAME_PREFIX + phone);


        vertx.eventBus().send(AppConstant.MongoVerticle_NOTIFICATION_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> result) {
                if (isRepli) {
                    mySQLDb.clearAllNoti(phone, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                        }
                    });
                }
                String status = result.body().getString("status", "fail");
                int error = "ok".equals(status) ? 0 : 1;
                handler.handle(new JsonObject().putNumber("error", error));
            }
        });
    }

    @Override
    public void saveNotificationGenId(int phone, JsonObject json, Handler<String> callback) {

    }
}
