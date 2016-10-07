package com.mservice.momo.util;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.NotiStatisticDb;
import com.mservice.momo.data.NotificationDb;
import com.mservice.momo.msg.StatisticModels;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.NotiStatistic;
import com.mservice.momo.vertx.models.Notification;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 4/28/14.
 */
public class StatisticUtils {

    private Vertx vertx;
    private Logger logger;

    private NotificationDb notificationDb;

    private NotiStatisticDb notiStatisticDb;

    public StatisticUtils(Vertx vertx, Logger logger, JsonObject glbConfig) {
        this.vertx = vertx;
        this.logger = logger;
        notificationDb = DBFactory.createNotiDb(vertx, logger, glbConfig);
        notiStatisticDb = new NotiStatisticDb(vertx, logger);
    }

    private static void fire(EventBus eventBus, int phoneNumber, StatisticModels.ActionType actionType, StatisticModels.Action.Channel chanel) {
        StatisticModels.Action action = StatisticModels.Action.newBuilder()
                .setType(actionType)
                .setPhoneNumber(phoneNumber)
                .setChannel(chanel)
                .build();
        eventBus.send(AppConstant.StatisticVerticle_ADDRESS_ACTION, action.toByteArray());
    }

    public static void fireLogin(EventBus eventBus, int phoneNumber, StatisticModels.Action.Channel channel) {
        fire(eventBus, phoneNumber, StatisticModels.ActionType.USER_LOGIN, channel);
    }

    public static void fireRegister(EventBus eventBus, int phoneNumber, StatisticModels.Action.Channel channel) {
        fire(eventBus, phoneNumber, StatisticModels.ActionType.USER_REGISTER, channel);
    }

    public static void fireTrans(EventBus eventBus, int phoneNumber, StatisticModels.Action.Channel channel) {
        fire(eventBus, phoneNumber, StatisticModels.ActionType.USER_TRANS, channel);
    }

    public static void fireNewConnection(EventBus eventBus,StatisticModels.Action.Channel channel) {
        fire(eventBus, 0, StatisticModels.ActionType.USER_ONLINE, channel);
    }

    public static void fireCloseConnection(EventBus eventBus,  StatisticModels.Action.Channel channel) {
        fire(eventBus, 0, StatisticModels.ActionType.USER_OFFLINE, channel);
    }

    public static void fireSendNotificationViaSocket(EventBus eventBus) {
        fire(eventBus, 0, StatisticModels.ActionType.SEND_NOTI_VIA_SOCKET, StatisticModels.Action.Channel.MOBILE);

    }
    public static void fireSendNotificationViaCloud(EventBus eventBus) {
        fire(eventBus, 0, StatisticModels.ActionType.SEND_NOTI_VIA_CLOUD, StatisticModels.Action.Channel.MOBILE);
    }


    public static void fireSendNotificationListViaSocket(EventBus eventBus) {
        fire(eventBus, 0, StatisticModels.ActionType.SEND_NOTI_LIST_VIA_SOCKET, StatisticModels.Action.Channel.MOBILE);

    }
    public static void fireSendNotificationListViaCloud(EventBus eventBus) {
        fire(eventBus, 0, StatisticModels.ActionType.SEND_NOTI_LIST_VIA_CLOUD, StatisticModels.Action.Channel.MOBILE);
    }

    private void getBroadcastNotiId(int phoneNumber, String notiId, final Handler<String> callback) {
        notificationDb.find(phoneNumber, notiId, new Handler<Notification>() {
            @Override
            public void handle(Notification noti) {



                if (noti == null || noti.refId == null || noti.refId.isEmpty()) {
                    callback.handle(null);
                    return;
                }
                callback.handle(noti.refId);
            }
        });
    }

    public void increaseNotiStatistic(String broadcastNotiId, Integer receive, Integer view, Integer tran) {
        NotiStatistic filter = new NotiStatistic(broadcastNotiId);
        NotiStatistic statistic = new NotiStatistic(receive, view, tran);
        notiStatisticDb.increase(filter, statistic, true, null);
    }

    public void broadCastNotiReceive(String broadcastNotiId) {
        increaseNotiStatistic(broadcastNotiId, 1, null, null);
    }

    public void broadCastNotiReceive(int phoneNumber, String notiId) {
        getBroadcastNotiId(phoneNumber, notiId, new Handler<String>() {
            @Override
            public void handle(String nId) {
                if (nId == null)
                    return;
                broadCastNotiReceive(nId);
            }
        });
    }

    public void broadcastNotiView(String broadcastNotiId) {
        increaseNotiStatistic(broadcastNotiId, null, 1, null);
    }

    public void broadcastNotiView(int phoneNumber, String notiId) {
        getBroadcastNotiId(phoneNumber, notiId, new Handler<String>() {
            @Override
            public void handle(String nId) {
                if (nId == null)
                    return;
                broadcastNotiView(nId);
            }
        });
    }

    public void broadcastNotiTran(String broadcastNotiId) {
        increaseNotiStatistic(broadcastNotiId, null, null, 1);
    }
}
