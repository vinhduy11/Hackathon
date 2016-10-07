package com.mservice.momo.data;

import com.mservice.momo.vertx.models.Notification;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

/**
 * Created by nam on 5/16/14.
 */
public interface NotificationDb {
    public static final String COLLECTION_NAME_PREFIX = "noti_";

    void updateNotifications(final int phone, final List<Notification> models, final Handler<List<Notification>> callback);

    void updateNotification(final int phone, final Notification model, final Handler<Integer> callback);

    void saveNotification(final int phone, final JsonObject json, final Handler<String> callback);
    void saveNotificationGenId(final int phone, final JsonObject json, final Handler<String> callback);
    //BEGIN 0000000004
    void upsertNotification(final int phone, final JsonObject json, final Handler<String> callback);

    void getAllNotification(int phone, Long startTime, List<Integer> statusValues, final Handler<List<Notification>> callback);

    void getAllNotificationForSync(int phone, Long startTime, List<Integer> statusValues, final Handler<List<Notification>> callback);

    void getNotiCollectionName(final Handler<List<String>> callback);

    void findNotiOld(final long start, final long end, final String collName, final Handler<JsonArray> callback);

    void deleteNotiOld(final long lastTime, final String collName, final Handler<JsonArray> callback);

    void writeJsonFile(final String fileName, final String json, final Handler<Boolean> callback);

    void getAllNotificationPage(int phone, int pageSize, int pageNum, final Long startTime, List<Integer> statusValues, final Handler<List<Notification>> callback);

    void find(int number, String notiId, final Handler<Notification> callback);

    void findNotiByTranId(int number, long tranId, final Handler<Notification> callback);

    //trim data for bignoti.start

    void getTime(final String collectName, int pageSize, int limit, final Handler<Long> callback);

    //trim data for bignoti.end

    void removeOldRecs(final String collectName, final long fromTime, final Handler<Integer> callback);

    void clearAllNoti(final int phone, final Handler<JsonObject> handler);
}
