package com.mservice.momo.vertx.gift;

import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.List;

/**
 * Created by nam on 10/31/14.
 */
public class GiftRemindTimer {

    public static boolean ON;
    public static Long TIME_DELAY;

    private Vertx vertx;
    private Logger logger;
    private GiftDb giftDb;
    private String notiCap ="";
    private String bodyTmp = "";
    private long day7 =7;
    private long day3 =3;
    private long day1=1;

    public GiftRemindTimer(Vertx vertx, Logger logger, JsonObject globalConfig) {
        this.vertx = vertx;
        this.logger = logger;
        giftDb = new GiftDb(vertx, logger);

        JsonObject joCfg = globalConfig.getObject("gift");
        JsonObject joScan = joCfg.getObject("remind", null);
        if(joScan != null){
//            notiCap = joScan.getString("cap","Quà tặng hết hạn");
//            bodyTmp = joScan.getString("body","Quà tặng của bạn chỉ còn %s ngày là hết hạn sử dụng. Vui lòng sử dụng nhanh chóng.");

            notiCap = PromoContentNotification.GIFT_EXPIRED_CAPTION;
            bodyTmp = PromoContentNotification.GIFT_EXPIRED_BODY;
            JsonArray array = joScan.getArray("period");

            day7 = ((JsonObject) array.get(0)).getLong("7day",7);
            day3 = ((JsonObject) array.get(1)).getLong("3day",3);
            day1 = ((JsonObject) array.get(2)).getLong("1day",1);

            this.ON = joScan.getBoolean("scanOn",false);
            this.TIME_DELAY = joScan.getLong("scanTime", 1) * 60 * 1000;

            setTimer();
        }else{
            logger.info("KHONG CHAY NHAC NHO QUA TANG HET HAN TREN SERVER NAY");
        }
    }

    public static long getLeftDays(long endTime) {
        long curTime = System.currentTimeMillis();
        long offset = 24*60*60*1000L;
        long days = 0;
        while (curTime <= endTime) {
            curTime += offset;
            days++;
        }
        return days;
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

                final Common.BuildLog glog = new Common.BuildLog(logger);
                glog.setPhoneNumber("remind_to_use_gift");

                giftDb.findForRemind(day7,day3,day1, new Handler<List<Gift>>() {
                    @Override
                    public void handle(final List<Gift> gifts) {
                        if(gifts == null || gifts.size() == 0){
                            glog.add("desc", "no gift to remind");
                            glog.writeLog();
                            return;
                        }

                        glog.add("number of gifts will be remined", gifts.size());
                        glog.writeLog();

                        final int maxSize = gifts.size();
                        final JsonObject joPos = new JsonObject();
                        joPos.putNumber("pos",0);

                        vertx.setPeriodic(300, new Handler<Long>() {
                            @Override
                            public void handle(Long timerId) {
                                final int curPos = joPos.getInteger("pos");

                                if(curPos < maxSize){

                                    final Gift gift = gifts.get(curPos);
                                    final Common.BuildLog log = new Common.BuildLog(logger);
                                    log.setPhoneNumber(gift.owner);

                                    long days =getLeftDays(gift.endDate.getTime());
                                    log.add("dayleft", days);
                                    //only send noti when the amount of left days in (7,3,1)
                                    if(days == day7 || days == day3 || days == day1){
                                        //send noti
                                        long curTime = System.currentTimeMillis();
                                        Notification noti = new Notification();
                                        noti.receiverNumber = DataUtil.strToInt(gift.owner);
                                        noti.caption = notiCap;
                                        //noti.body = String.format(bodyTmp, getLeftDays(gift.endDate.getTime())) ;
                                        noti.body = String.format(bodyTmp, gift.amount) ;
                                        noti.bodyIOS = noti.body;
                                        noti.type = MomoProto.NotificationType.REMIND_EXPIRED_VALUE;
                                        noti.status = Notification.STATUS_DISPLAY;
                                        noti.tranId = curTime;
                                        noti.cmdId =curTime;
                                        noti.time = curTime;
                                        noti.priority = 2;
                                        JsonObject jo = new JsonObject();
                                        jo.putString("giftId", gift.getModelId());
                                        jo.putString("giftTypeId",gift.typeId);

                                        noti.extra =jo.toString();
                                        Misc.sendNoti(vertx, noti);

                                    }

                                    joPos.putNumber("pos", curPos +1);
                                    log.writeLog();

                                }else{
                                    vertx.cancelTimer(timerId);
                                }
                            }
                        });
                    }
                });
            }
        });
    }
}
