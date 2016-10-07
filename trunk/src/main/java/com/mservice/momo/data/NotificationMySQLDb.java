package com.mservice.momo.data;

import com.mservice.momo.data.model.DBAction;
import com.mservice.momo.data.model.DBMsg;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Misc;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nam on 5/16/14.
 */
public class NotificationMySQLDb implements NotificationDb {

    public static final String PREFIX_LOG = "MySQL_Noti ";
    private Vertx vertx;
    private Logger logger;
    private JsonObject config;

    public NotificationMySQLDb(Vertx vertx, Logger logger, JsonObject glbConfig) {
        this.vertx = vertx;
        this.logger = logger;
        this.config = glbConfig;
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

        model.sender = phone;

        DBMsg dbMsg = new DBMsg(phone, DBAction.Noti.UPDATE_NOTI);
        dbMsg.params = model.toJsonArray().toList();

        logger.info(String.format("Update %d notification{id: %s}value: %s", phone, model.id, model.toJsonObject()));

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.NOTI_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "updateNotification is " + (dbResMsg.err == 0));
                callback.handle(0);
            }
        });
    }

    public void saveNotification(final int phone, final JsonObject json, final Handler<String> callback) {
        if (json.getFieldNames().size() == 0) {
            return;
        }
        final String createdId = UUID.randomUUID().toString();
        json.putString("_id", createdId);
        json.putNumber(colName.NotificationCols.SENDER, phone);
        long tranId = json.getLong(colName.NotificationCols.TRAN_ID, 0L);

        DBMsg dbMsg = new DBMsg(phone, tranId, DBAction.Noti.SAVE_NOTI);
        dbMsg.params = Notification.parse(json).toJsonArray().toList();

        logger.info(String.format("Save %d notification: %s", phone, json));

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.NOTI_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "saveNotification is " + (dbResMsg.err == 0));
                if (dbResMsg.err == 0) {
                    callback.handle(createdId);
                } else {
                    callback.handle(null);
                }
            }
        });
    }


    public void saveNotificationGenId(final int phone, final JsonObject json, final Handler<String> callback) {
        if (json.getFieldNames().size() == 0) {
            return;
        }
        String createdId = json.getString("_id", "");
        if (StringUtils.isEmpty(createdId)) {
            createdId = UUID.randomUUID().toString();
            json.putString("_id", createdId);
        }
        json.putNumber(colName.NotificationCols.SENDER, phone);
        long tranId = json.getLong(colName.NotificationCols.TRAN_ID, 0L);

        DBMsg dbMsg = new DBMsg(phone, tranId, DBAction.Noti.SAVE_NOTI);
        dbMsg.params = Notification.parse(json).toJsonArray().toList();

        logger.info(String.format("Save %d notification: %s", phone, json));

        final String finalId = createdId;
        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.NOTI_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "saveNotification is " + (dbResMsg.err == 0));
                if (dbResMsg.err == 0) {
                    callback.handle(finalId);
                } else {
                    callback.handle(null);
                }
            }
        });
    }

    public void saveNotifications(final long phone, final JsonArray array, final Handler<Integer> callback) {

        final AtomicInteger count = new AtomicInteger(array.size());
        saveOneNoti(count, phone, array, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {
                logger.info(PREFIX_LOG + "saveNotifications is true, rows = " + integer);
            }
        });
    }

    private void saveOneNoti(final AtomicInteger count, final long phone, final JsonArray array, final Handler<Integer> callback) {

        JsonObject json = array.get(count.get());
        saveNotification((int) phone, json, new Handler<String>() {
            @Override
            public void handle(String s) {
                if (count.decrementAndGet() <= 0) {
                    callback.handle(array.size());
                } else {
                    saveOneNoti(count, phone, array, callback);
                }
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

        JsonArray params = new JsonArray();
        params.add(startTime);
        params.add(phone);
        params.add(Notification.STATUS_DELETED);
        params.add(120);

        DBMsg dbMsg = new DBMsg(phone, DBAction.Noti.GET_NOTI_FOR_SYNC);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.NOTI_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "getAllNotificationForSync is " + (dbResMsg.err == 0));
                ArrayList<Notification> finalResult = null;
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    finalResult = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        JsonArray jsonModel = results.get(i);
                        JsonObject ob = Notification.fromArrayToJsonObj(jsonModel);
                        Notification model = Notification.parseEncryptNoti(ob);
                        finalResult.add(model);
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    public void find(int number, String notiId, final Handler<Notification> callback) {

        JsonArray params = new JsonArray();
        params.add(notiId);
        params.add(number);

        DBMsg dbMsg = new DBMsg(number, DBAction.Noti.GET_NOTI);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.NOTI_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "find is " + (dbResMsg.err == 0));
                Notification model = null;
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        JsonArray jsonModel = results.get(0);
                        JsonObject ob = Notification.fromArrayToJsonObj(jsonModel);
                        model = Notification.parseEncryptNoti(ob);
                    }
                }
                if (callback != null) {
                    callback.handle(model);
                }
            }
        });
    }

    public void removeOldRecs(String collectName, long fromTime, final Handler<Integer> callback) {

        String[] arr = collectName.split("_");
        JsonArray params = new JsonArray();
        int phone = DataUtil.strToInt(arr[1]);
        params.add(phone);
        params.add(fromTime);

        DBMsg dbMsg = new DBMsg(phone, DBAction.Noti.REMOVE_OLD_NOTI_BY_TIME);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.NOTI_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "removeOldRecs is " + (dbResMsg.err == 0));

                int rows = dbResMsg.res != null && dbResMsg.res.size() > 0 ? (Integer) dbResMsg.res.get(0) : 0;
                callback.handle(rows);
            }
        });
    }

    public void clearAllNoti(int phone, final Handler<JsonObject> handler) {

        JsonArray params = new JsonArray();
        params.add(phone);

        DBMsg dbMsg = new DBMsg(phone, DBAction.Noti.REMOVE_OLD_NOTI_BY_USER);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.NOTI_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "clearAllNoti is " + (dbResMsg.err == 0));
                handler.handle(new JsonObject().putNumber("error", (dbResMsg.err == 0) ? 0 : 1));
            }
        });
    }

    @Override
    public void upsertNotification(int phone, JsonObject json, Handler<String> callback) {

    }

    @Override
    public void getAllNotification(int phone, Long startTime, List<Integer> statusValues, Handler<List<Notification>> callback) {

    }

    @Override
    public void getNotiCollectionName(Handler<List<String>> callback) {

    }

    @Override
    public void findNotiOld(long start, long end, String collName, Handler<JsonArray> callback) {

    }

    @Override
    public void deleteNotiOld(long lastTime, String collName, Handler<JsonArray> callback) {

    }

    @Override
    public void writeJsonFile(String fileName, String json, Handler<Boolean> callback) {

    }

    @Override
    public void getAllNotificationPage(int phone, int pageSize, int pageNum, Long startTime, List<Integer> statusValues, Handler<List<Notification>> callback) {

    }

    @Override
    public void findNotiByTranId(int number, long tranId, Handler<Notification> callback) {

    }

    @Override
    public void getTime(String collectName, int pageSize, int limit, Handler<Long> callback) {

    }
}
