package com.mservice.momo.vertx.gift;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.gift.TimedGiftDb;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftToNumber;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.gift.models.TimedGift;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.Date;
import java.util.List;

/**
 * Created by nam on 10/31/14.
 */
public class SendGiftTimer {
    public static boolean ON;
    public static Long TIME_DELAY;
    public String LINK;
    private Vertx vertx;
    private Logger logger;

    private GiftDb giftDb;
    private TransDb tranDb;
    private GiftManager giftManager;
    private TimedGiftDb timedGiftDb;

    public SendGiftTimer(Vertx vertx, Logger logger, JsonObject globalConfig) {
        this.vertx = vertx;
        this.logger = logger;

        JsonObject giftConfig = globalConfig.getObject("gift", new JsonObject());
        LINK = giftConfig.getString("link", "http://app.momo.vn/${token}");

        giftDb = new GiftDb(vertx, logger);
        tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, globalConfig);
        giftManager = new GiftManager(vertx, logger, globalConfig);
        timedGiftDb = new TimedGiftDb(vertx, logger);

        this.ON = giftConfig.getBoolean("sendTimerOn", false);

        //product
        this.TIME_DELAY = giftConfig.getLong("sendTimer", 1) * 60 * 1000;

        setTimer();
    }

    private void setTimer() {
        if (!this.ON) {
            logger.info("Gift timer is OFF");
            return;
        }

        logger.info("Gift timer is ON");

        vertx.setPeriodic(TIME_DELAY, new Handler<Long>() {
            @Override
            public void handle(Long result) {
                timedGiftDb.find(System.currentTimeMillis(), new Handler<List<TimedGift>>() {
                    @Override
                    public void handle(List<TimedGift> list) {

                        int size = (list == null ? 0 : list.size());

                        logger.info("total gift will be sent out by timed " + size);

                        for (int i = 0; i < list.size(); i++) {
                            final Common.BuildLog log = new Common.BuildLog(logger);
                            final TimedGift tg = list.get(i);

                            log.add("send gift " + (i + 1) + " out ", "**********");

                            int toPhone = DataUtil.strToInt(tg.toAgent);
                            if (toPhone == 0) {
                                log.add("error TimedGift", tg.toJsonObject());
                                return;
                            }

                            String pin = DataUtil.decode(tg.pin);

                            log.add("giftType", tg.giftType);
                            log.add("giftid", tg.giftId);
                            log.add("from", tg.fromAgent);
                            log.add("to", tg.toAgent);
                            log.add("pinlen", pin.length());
                            log.add("send gift", "by timer");
                            log.add("function", "transferGifDirectOrG2n");

                            giftManager.transferGifDirectOrG2n(tg.fromAgent
                                    , tg.fromAgentName
                                    , pin
                                    , tg.giftId
                                    , toPhone
                                    , tg.message, null, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject result) {
                                    int error = result.getInteger("error");
                                    String desc = result.getString("desc", "not defined");

                                    log.add("send gift result", "**********************");
                                    log.add("error", error);
                                    log.add("errdesc", SoapError.getDesc(error));

                                    if (error != 0) {
                                        tg.error = error;
                                        tg.desc = desc;
                                        timedGiftDb.update(tg, false, null);
                                        log.add("send gift out failed", "****************");
                                        log.writeLog();
                                        return;
                                    }

                                    log.add("send gift out ok", "*****************");

                                    timedGiftDb.remove(tg, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {
                                            log.add("remove from timedGift table result", aBoolean);
                                            log.writeLog();
                                        }
                                    });

                                    notifyReceiver(tg, result);
                                    notifySender(tg, result);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void notifySender(final TimedGift tg, JsonObject result) {

        long preTranId = tg.preTranId;
        final int phone = DataUtil.strToInt(tg.fromAgent);

        tranDb.getTranById(phone, preTranId, new Handler<TranObj>() {
            @Override
            public void handle(final TranObj tranObj) {
                if (tranObj == null) {
                    return;
                }

                tranObj.owner_number = phone;
                tranObj.finishTime = System.currentTimeMillis();
                tranObj.status = TranObj.STATUS_OK;

                tranDb.upsertTranOutSideNew(tranObj.owner_number, tranObj.getJSON(), new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        BroadcastHandler.sendOutSideTransSync(vertx, tranObj);
                    }
                });
            }
        });
    }

    public void notifyReceiver(TimedGift tg, JsonObject result) {
        final TranObj rcvTranObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        long rcvTranId = result.getLong("tranId", 0);

        Gift gift = new Gift(result.getObject("gift"));
        GiftType giftType = new GiftType(result.getObject("giftType"));

        rcvTranObj.owner_number = DataUtil.strToInt(tg.toAgent);
        rcvTranObj.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        rcvTranObj.comment = DataUtil.stringFormat("Quý khách đã nhận thẻ quà tặng của dịch vụ ${giftName} trị giá ${giftAmount}đ. \nLời nhắn: ${message}")
                .put("giftName", giftType.name)
                .put("giftAmount", String.format("%,d", gift.amount).replace(",", "."))
                .put("message", tg.message)
                .toString();
        rcvTranObj.tranId = rcvTranId;
        rcvTranObj.clientTime = currentTime;
        rcvTranObj.ackTime = currentTime;
        rcvTranObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        rcvTranObj.partnerName = tg.fromAgentName;
        rcvTranObj.partnerId = tg.fromAgent;
        rcvTranObj.amount = gift.amount;
        rcvTranObj.status = TranObj.STATUS_OK;
        rcvTranObj.error = 0;
        rcvTranObj.cmdId = -1;
        rcvTranObj.billId = gift.getModelId();
        rcvTranObj.io = 1;
        rcvTranObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;

        final Notification noti = new Notification();
        noti.receiverNumber = DataUtil.strToInt(tg.toAgent);
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
        noti.caption = "Nhận được quà";

        GiftToNumber giftToNumber = null;
        if (GiftManager.TRANTYPE_G2N.equalsIgnoreCase(result.getString("tranType", "transfer"))) {

            giftToNumber = new GiftToNumber(result.getObject("giftToNumber"));
            noti.body = DataUtil.stringFormat("Bạn vừa nhận được một món quà từ ${agent}(${agentName}).")
                    .put("agent", tg.fromAgent)
                    .put("agentName", tg.fromAgentName)
                    .toString();
            noti.sms = DataUtil.stringFormat("Ban nhan duoc mon qua tu ${agent}. Xem chi tiet tai: ${link}")
                    .put("agent", tg.fromAgent)
                    .put("link", giftToNumber == null ? "momo.vn" : DataUtil.stringFormat(LINK).put("link", giftToNumber.link))
                    .toString();
        } else {
            noti.body = DataUtil.stringFormat("Bạn vừa nhận được một món quà từ ${agent}(${agentName}).")
                    .put("agent", tg.fromAgent)
                    .put("agentName", tg.fromAgentName)
                    .toString();
            noti.sms = DataUtil.stringFormat("Ban nhan duoc mon qua tu ${agent}. Vui long dang nhap vao vi Momo de xem chi tiet.")
                    .put("agent", tg.fromAgent)
                    .toString();
        }
        noti.tranId = rcvTranId;
        noti.sender = DataUtil.strToInt(tg.fromAgent);

        noti.time = new Date().getTime();
        noti.extra = new JsonObject()
                .putString("giftId", gift.getModelId())
                .putString("giftTypeId", gift.typeId)
                .putString("msg", tg.message)
                .putString("senderName", tg.fromAgentName)
                .putString("amount", String.valueOf(gift.amount))
                .toString();

        Misc.sendNoti(vertx, noti);

        tranDb.upsertTranOutSideNew(rcvTranObj.owner_number, rcvTranObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, rcvTranObj);
                }
            }
        });
    }
}
