package com.mservice.momo.vertx.gift;

import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by nam on 10/31/14.
 */
public class ExpiredGiftTimer {

    public static String EXPIRE_GIFT_MOMO;
    public static boolean ON;
    public static Long TIME_DELAY;
    private Vertx vertx;
    private Logger logger;
    private GiftDb giftDb;
    private GiftManager giftManager;

    private boolean isRunning = false;

    public ExpiredGiftTimer(Vertx vertx, Logger logger, JsonObject globalConfig) {
        this.vertx = vertx;
        this.logger = logger;

        JsonObject giftConfig = globalConfig.getObject("gift", new JsonObject());

        EXPIRE_GIFT_MOMO = giftConfig.getString("expireGiftMomo", null);
        if (EXPIRE_GIFT_MOMO == null) throw new IllegalAccessError("Config gift.expireGiftMomo can't be null!");

        giftDb = new GiftDb(vertx, logger);

        giftManager = new GiftManager(vertx, logger, globalConfig);

        this.ON = giftConfig.getBoolean("expiredTimerOn", false);
        this.TIME_DELAY = giftConfig.getLong("expiredTimer", 1) * 60 * 1000;

        setTimer();
    }

    private void setTimer() {
        if (!this.ON) {
            logger.info("Expired gift timer is OFF");
            return;
        }
        logger.info("Expired gift timer is ON");

        vertx.setPeriodic(TIME_DELAY, new Handler<Long>() {
            @Override
            public void handle(Long result) {

                logger.info("EXPIRED IS RUNNING " + isRunning);

                //we need to wait until the previous timer completely
                if (isRunning == true){
                    return;
                }

                final Common.BuildLog glog = new Common.BuildLog(logger);
                glog.setPhoneNumber("giftexpiredtimer");

                long expiredTime = System.currentTimeMillis();

                giftDb.findForExpired(expiredTime, new Handler<List<Gift>>() {
                    @Override
                    public void handle(final List<Gift> gifts) {
                        if (gifts == null || gifts.size() == 0) {
                            glog.add("waiting gifts will be expired size", 0);
                            glog.writeLog();
                            return;
                        }
                        glog.add("waiting gifts will be expired size", gifts.size());
                        glog.writeLog();

                        //build queue prepare run expired
                        final Queue<Gift> queueGifts = new ArrayDeque<Gift>();
                        for (Gift gift : gifts){
                            queueGifts.add(gift);
                        }

                        //begin process expired gifts here
                        if(queueGifts.size() >0){
                            isRunning = true;
                            runExpiredGift(queueGifts);
                        }
                    }
                });
            }
        });
    }

    private void runExpiredGift(final Queue<Gift> queueGifts){
        if(queueGifts == null || queueGifts.size() == 0){
            isRunning = false;
            return;
        }

        //lay gift ra
        final Gift gift = queueGifts.poll();
        if(gift == null){
            runExpiredGift(queueGifts);
        }else{

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(gift.owner);

            giftManager.transferGiftWhenExpired(gift, EXPIRE_GIFT_MOMO, null, log, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject result) {
                    log.add("transferGift result", result.encodePrettily());
                    int error = result.getInteger("error", -1000);
                    log.add("curDate", Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                    log.add("startDate", Misc.dateVNFormatWithTime(gift.startDate.getTime()));
                    log.add("endDate", Misc.dateVNFormatWithTime(gift.endDate.getTime()));
                    log.add("amount", gift.amount);
                    log.add("tranId", gift.tranId);
                    log.add("typeId", gift.typeId);
                    log.add("statusdesc", Gift.getDesc(gift.status));
                    log.add("momo account", EXPIRE_GIFT_MOMO);
                    log.add("error", error);
                    log.add("errdesc", SoapError.getDesc(error));
                    log.writeLog();
                    runExpiredGift(queueGifts);
                }
            });
        }
    }
}
