package com.mservice.momo.vertx.gift;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.gift.GiftToNumberDb;
import com.mservice.momo.data.gift.GiftTypeDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.models.GiftToNumber;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by nam on 10/17/14.
 */
public class GiftRollback {
    public static boolean ROLBACK_CHECKING;
    public static long ROLLBACK_TIMER;
    public static int ROLBACK_DAY;

    private Vertx vertx;
    private Logger logger;

    private GiftToNumberDb giftToNumberDb;
    private GiftDb giftDb;
    private TransDb tranDb;
    private GiftTypeDb giftTypeDb;
    private GiftManager giftManager;


    public GiftRollback(Vertx vertx, Logger logger, JsonObject globalConfig) {
        this.vertx = vertx;
        JsonObject giftConfig = globalConfig.getObject("gift", new JsonObject());

        ROLBACK_CHECKING = giftConfig.getBoolean("rollbackChecking", false);
        ROLLBACK_TIMER = giftConfig.getLong("rollbackTimer", 15) * 60 * 1000;
        ROLBACK_DAY = giftConfig.getInteger("rollbackDay", 3);

        this.logger = logger;

        giftToNumberDb = new GiftToNumberDb(vertx, logger);
        giftDb = new GiftDb(vertx, logger);
        tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, globalConfig);
        giftTypeDb = new GiftTypeDb(vertx, logger);
        giftManager = new GiftManager(vertx, logger, globalConfig);

        setTimer();
    }

    public void setTimer() {
        if (!ROLBACK_CHECKING) {
            logger.info("Gift transaction rollback timer is OFF");
            return;
        }

        logger.info("Start gift transaction rollback timer");
        vertx.setPeriodic(ROLLBACK_TIMER, new Handler<Long>() {
            @Override
            public void handle(Long timerId) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -ROLBACK_DAY);

                final Common.BuildLog log = new Common.BuildLog(logger);

                giftToNumberDb.find(cal.getTimeInMillis(), GiftToNumber.STATUS_NEW, new Handler<List<GiftToNumber>>() {
                    @Override
                    public void handle(List<GiftToNumber> giftToNumbers) {
                        log.add("Rollback Gift Transaction", giftToNumbers.size());
                        log.writeLog();

                        for (GiftToNumber giftToNumber : giftToNumbers) {
                            final GiftToNumber g2n = giftToNumber;

                            giftManager.giftToNumberRolback(g2n, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject result) {
                                    int error = result.getInteger("error");

                                    log.add("senderName", g2n.senderName);
                                    log.add("fromAgent", g2n.fromAgent);
                                    log.add("toAgent", g2n.toAgent);
                                    log.add("tranId", g2n.tranId);
                                    log.add("rollback error", error);
                                    log.writeLog();

                                    if (error == 0) {
                                        final int phone = DataUtil.strToInt(g2n.fromAgent);
                                        tranDb.updateTranStatusNew(phone, g2n.tranId, TranObj.STATUS_FAIL, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean result) {
                                                tranDb.getTranById(phone, g2n.tranId, new Handler<TranObj>() {
                                                    @Override
                                                    public void handle(TranObj tran) {
                                                        BroadcastHandler.sendOutSideTransSync(vertx, tran);
                                                        notifyReceiver(g2n);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    public void notifyReceiver(GiftToNumber g2n) {
        final Notification notification = new Notification();
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        notification.caption = "Tặng quà không thành công!";
        notification.body = "Món quà bạn gửi đến @toAgent vào lúc @startDate không thành công do người bạn này đã không mở ví.";
        notification.body = notification.body
                .replaceAll("@toAgent", g2n.toAgent)
                .replaceAll("@startDate", DataUtil.timeDate(g2n.getStartDate()));
//        notification.sms = "";
        notification.tranId = g2n.tranId;
//        notification.sender = sender;

        notification.time = new Date().getTime();
//        notification.refId = ;
        notification.receiverNumber = DataUtil.strToInt(g2n.fromAgent);

        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                , notification.toFullJsonObject(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
            }
        });
    }
}
