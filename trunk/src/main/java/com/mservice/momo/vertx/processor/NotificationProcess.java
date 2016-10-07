package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.NotificationDb;
import com.mservice.momo.data.NotificationToolDb;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.StatisticUtils;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.Notification;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nam on 5/19/14.
 */
public class NotificationProcess {
    private Vertx vertx;
    private Container container;

    private NotificationDb notificationDb;
    private StatisticUtils statisticUtils;
    private NotificationToolDb notificationToolDb;
    private Common mCom;

    public NotificationProcess(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
        notificationDb = DBFactory.createNotiDb(vertx, container.logger(), container.config());
        statisticUtils = new StatisticUtils(vertx, container.logger(), container.config());
        mCom = new Common(vertx,container.logger(),container.config());
        notificationToolDb = new NotificationToolDb(vertx, container.logger());
    }

    public void updateNotificationsStatus(final NetSocket mSocket, SockData data, final MomoMessage msg) {
        MomoProto.UpdateNotificationsStatus rawRequest;
        try {
            rawRequest = MomoProto.UpdateNotificationsStatus.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            rawRequest = null;
        }
        if (rawRequest == null) {
            mCom.writeErrorToSocket(mSocket);
            return;
        }
        final MomoProto.UpdateNotificationsStatus request = rawRequest;
        final ArrayList<Notification> notifications = new ArrayList<>();

        final AtomicInteger countNoti = new AtomicInteger(request.getNotificationsList().size());
        vertx.setPeriodic(250L, new Handler<Long>() {
            @Override
            public void handle(Long timerSendNoti) {
                 int positionNoti = countNoti.decrementAndGet();
                if(positionNoti < 0)
                {
                    vertx.cancelTimer(timerSendNoti);
                    return;
                }
                Notification notification = new Notification();
                notification.id = request.getNotificationsList().get(positionNoti).getId();
                notification.status = request.getNotificationsList().get(positionNoti).getStatus();
                if(notification.status == 0) {
                    container.logger().debug(String.format("REJECT updating notification request from %d : %s", msg.cmdPhone, notification.toJsonObject().toString()));
                    return;
                }
                notifications.add(notification);

                //TODO: Nếu status chuyển từ 0->2 thì thống kê có một noti đc đoc
                if (notification.status == Notification.STATUS_READ) {
                    notificationDb.find(msg.cmdPhone, request.getNotificationsList().get(positionNoti).getId(), new Handler<Notification>() {
                        @Override
                        public void handle(Notification notification) {
                            if (notification != null && notification.status == Notification.STATUS_DETAIL) {
                                if (notification.refId != null && !notification.refId.isEmpty())
                                    statisticUtils.increaseNotiStatistic(notification.refId, null, 1, null);
                            }
                        }
                    });
                }
                //Update noti from tool
                JsonObject joUpdate = new JsonObject().putNumber("status", 1);
                notificationToolDb.upsertPartial(request.getNotificationsList().get(positionNoti).getId(), joUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {

                    }
                });
            }
        });
//        for (MomoProto.Notification momoNoti : request.getNotificationsList()) {
//
//        }



        notificationDb.updateNotifications(msg.cmdPhone, notifications, new Handler<List<Notification>>() {
            @Override
            public void handle(List<Notification> updatedNotifications) {
                MomoProto.UpdateNotificationsStatusReply.Builder replyBody = MomoProto.UpdateNotificationsStatusReply.newBuilder();
                replyBody.setFlag(request.getFlag()); // response flag back to client (Their use to determine the final update packet).


                for (Notification notification : updatedNotifications) {
                    replyBody.addNotifications(notification.toMomoProto());
                }
                MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.UPDATE_NOTIFICATIONS_STATUS_REPLY_VALUE, msg.cmdIndex, msg.cmdPhone,
                        replyBody.build().toByteArray()
                );
                mCom.writeDataToSocket(mSocket, momoMessage.toBuffer());
            }
        });
    }
}
