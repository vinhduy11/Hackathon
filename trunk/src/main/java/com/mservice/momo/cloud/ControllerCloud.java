package com.mservice.momo.cloud;

import com.mongodb.*;
import com.mservice.momo.data.NotificationToolDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.redis.NotificationRedisVerticle;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 3/17/15.
 */
public class ControllerCloud {
    public static final int DEFAULT_PRIORITY = 10;
    private String STATIC_FILE_DIRECTORY = "";
    private Vertx vertx;
    private Logger logger;
    private JsonObject glbCfg;
    private Queue<Integer> QPhones = new LinkedList<>();
    private PhonesDb phonesDb;
    private NotificationToolDb notificationToolDb;
    private JsonObject joMongo;
    public ControllerCloud(Vertx vertx, Container container, String STATIC_FILE_DIRECTORY) {
        this.vertx = vertx;
        logger = container.logger();
        glbCfg = container.config();
        this.STATIC_FILE_DIRECTORY = STATIC_FILE_DIRECTORY;
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        notificationToolDb = new NotificationToolDb(vertx, container.logger());
        joMongo = glbCfg.getObject("mongo", new JsonObject());
    }

    @Action(path = "/sendnoti/upload")
    public void sendNotiUpload(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        callback.handle(new JsonObject()
                .putString("error", "0")
                .putString("desc", "Upload file " + hfile + " success"));
    }

    @Action(path = "/sendnoti/cloud")
//    @Action(path = "/sendnoti/cloud", roles = {Role.SUPER, Role.SEND_NOTI})
    public void sendnoticloud(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("sendcloud");
        logger.info("address: " + context.getRequest().remoteAddress().getAddress());

        String body = (params.get("body") == null ? "" : params.get("body").trim());
        String caption = (params.get("caption") == null ? "" : params.get("caption").trim());
        String bodyIOS = (params.get("bodyIOS") == null ? "" : params.get("bodyIOS").trim());
        String htmlIos = (params.get("htmlBodyIOS") == null ? "" : params.get("htmlBodyIOS").trim());
        String extraField = (params.get("extraField") == null ? "" : params.get("extraField").trim());
        String phones = (params.get("phone") == null ? "" : params.get("phone").trim());
        String tBtnTitle = (params.get("btnTitle") == null ? "" : params.get("btnTitle").trim());
        Integer tBtnStatus = (DataUtil.strToInt(params.get("btnStatus") == null ? "0" : params.get("btnStatus").trim()));
        String serviceId = (params.get("serviceId") == null ? "" : params.get("serviceId").trim());
        String bank = (params.get("bank") == null ? "" : params.get("bank").trim());
        final int category = params.get("category") == null ? 1 : DataUtil.strToInt(params.get("category").trim());
        Integer tNotitype = (params.get("notiType") == null ? -1 : DataUtil.strToInt(params.get("notiType").trim()));
        String hfile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        String dtype = params.get("dtype") == null ? "" : params.get("dtype").trim();
        String url = params.get("url") == null ? "" : params.get("url").trim();
        // popup info
        final int popupType = params.get("popupType") == null ? 1 : DataUtil.strToInt(params.get("popupType").trim());
        String popupButtonTitle1 = (params.get("popupButtonTitle1") == null ? "" : params.get("popupButtonTitle1").trim());
        String popupButtonTitle2 = (params.get("popupButtonTitle2") == null ? "" : params.get("popupButtonTitle2").trim());
        final boolean popupButtonX = params.get("popupButtonX") == null ? false : Boolean.valueOf(params.get("popupButtonX").trim());
        log.add("caption", caption);
        log.add("body", body);
        log.add("bodyIOS", bodyIOS);
        log.add("htmlIos", htmlIos);
        log.add("phones", phones);
        log.add("btnTitle", tBtnTitle);
        log.add("btnStatus", tBtnStatus);
        log.add("serviceId", serviceId);
        log.add("bank", bank);
        log.add("category", category);
        log.add("notitype", tNotitype);
        log.add("hfile", hfile);
        log.add("device type", dtype);
        log.add("url", url);
        log.add("popupType", popupType);
        log.add("popupButtonTitle1", popupButtonTitle1);
        log.add("popupButtonTitle2", popupButtonTitle2);
        log.add("popupButtonX", popupButtonX);
        final String htmlBodyIos = htmlIos;
        final String btnTitle = tBtnTitle;
        final Integer btnStatus = tBtnStatus;

        if (tNotitype == -1) {
            callback.handle(new JsonObject().putString("error", "-100").putString("desc", "Notification type chưa đúng"));
            log.add("***", "notification type chua dung");
            log.writeLog();
            return;
        }

        final Integer notitype = tNotitype;

        //khong gui noi dung cho ca android va IOS
        if ("".equalsIgnoreCase(body) && "".equalsIgnoreCase(bodyIOS)) {
            callback.handle(new JsonObject().putString("error", "-100").putString("desc", "Không gửi noi dung tin nhan ra ngoai ra ngoai"));
            log.add("***", "Khong ban cloud vi khong co noi dung nao ca");
            log.writeLog();
            return;
        }

        if ("".equalsIgnoreCase(bodyIOS) && !"".equalsIgnoreCase(body)) {
            bodyIOS = body;
        }

        String[] temp = phones.split(";");
        ArrayList<Integer> listphone = new ArrayList<>();
        for (String s : temp) {
            int item = DataUtil.strToInt(s);
            if (item > 0) {
                listphone.add(item);
            }
        }

        if (listphone == null && listphone.size() < 1) {
            callback.handle(new JsonObject()
                    .putString("error", "-100")
                    .putString("desc", "We don't have any valid phone to send."));
            log.add("***", "We don't have any valid phone to send.");
            log.writeLog();
            return;
        }

        JsonObject extraJo = Misc.isValidJsonObject(extraField) ? new JsonObject(extraField) : new JsonObject();
        extraJo.putString("serviceId", serviceId);

        if (!"".equalsIgnoreCase(bank)) {
            extraJo.putString("bank", bank);
        }
        if (!"".equalsIgnoreCase(dtype)) {
            extraJo.putString("devicetype", dtype);
        }
        if (!"".equalsIgnoreCase(url)) {
            extraJo.putString("url", url);
        }
        // popup info
        extraJo.putNumber(StringConstUtil.TYPE, popupType);
        extraJo.putString(StringConstUtil.BUTTON_TITLE_1, popupButtonTitle1);
        extraJo.putString(StringConstUtil.BUTTON_TITLE_2, popupButtonTitle2);
        extraJo.putBoolean(StringConstUtil.BUTTON_TITLE_X, popupButtonX);
        extraJo.putString(StringConstUtil.SERVICE_ID, serviceId);
        int priorityTemp = DEFAULT_PRIORITY;
        if (MomoProto.NotificationType.POPUP_INFORMATION_VALUE == notitype) {
            priorityTemp = 2;
        }
        final int priority = priorityTemp;

        final String fExtra = extraJo.toString();

        final Notification noti;

        final String fbodyAndroid = body;
        final String fbodyIOS = bodyIOS;
        final String fCaption = caption;

        long totalRec = 0;

        //send notification by file
        if (!"".equalsIgnoreCase(hfile)) {

            final String mRefId = "LIST" + System.currentTimeMillis();
            final Notification fNoti = new Notification();
            fNoti.caption = caption;
            fNoti.body = body;
            fNoti.bodyIOS = bodyIOS;
            fNoti.sms = "";
            fNoti.tranId = -9999L;
            fNoti.type = notitype;
            fNoti.status = Notification.STATUS_DETAIL;
            fNoti.priority = priority;
            fNoti.time = System.currentTimeMillis();
            fNoti.btnTitle = btnTitle;
            fNoti.btnStatus = btnStatus;
            fNoti.htmlBody = htmlIos;
            fNoti.extra = fExtra;
            fNoti.category = category;

            //todo temporary
            //C:\fakepath\push.txt
            String[] ar = hfile.split("\\\\");
            hfile = ar[ar.length - 1];

            String filePath = STATIC_FILE_DIRECTORY + hfile;

            log.add("filePath", filePath);
            logger.info("filePath: " + filePath);
            ArrayList<Object> arrayList = Misc.readFile(filePath, log);
            totalRec = arrayList.size();

            if (arrayList != null && arrayList.size() > 0) {
                for (int i = 0; i < arrayList.size(); i++) {
                    Integer p = DataUtil.strToInt(arrayList.get(i).toString());
                    if (p > 0) {
                        QPhones.add(p);
                    }
                }
            }
            final long currentTime = System.currentTimeMillis();
            final List<Notification> listNoti = new ArrayList<>();

            if (QPhones.size() > 0) {
                vertx.setPeriodic(3000, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {

                        if (QPhones.size() > 0) {
                            ArrayList<Integer> phones = new ArrayList<>();

                            for (int i = 0; i < 100; i++) {
                                Integer p = QPhones.poll();
                                if (p != null) {
                                    phones.add(p);
                                }
                            }

                            phonesDb.getPage(0, phones, 1, phones.size(), "", new Handler<ArrayList<PhonesDb.Obj>>() {
                                @Override
                                public void handle(final ArrayList<PhonesDb.Obj> objs) {
                                    if (objs != null && objs.size() > 0) {
                                        final AtomicInteger countNoti = new AtomicInteger(objs.size());
                                        vertx.setPeriodic(75L, new Handler<Long>() {
                                            @Override
                                            public void handle(Long timerSendNoti) {
                                                int positionNoti = countNoti.decrementAndGet();
                                                if (positionNoti < 0) {
                                                    vertx.cancelTimer(timerSendNoti);
                                                    return;
                                                }
                                                Common.BuildLog llog = new Common.BuildLog(logger);
                                                llog.setPhoneNumber("0" + objs.get(positionNoti).number);

                                                Notification notiL = new Notification(fNoti);
                                                notiL.receiverNumber = objs.get(positionNoti).number;
                                                notiL.os = objs.get(positionNoti).phoneOs;
                                                notiL.token = objs.get(positionNoti).pushToken;

                                                if ("ios".equalsIgnoreCase(objs.get(positionNoti).phoneOs)) {

                                                    //body text cua ios
                                                    if (!"".equalsIgnoreCase(fbodyIOS)) {
                                                        notiL.body = fbodyIOS;
                                                        notiL.bodyIOS = fbodyIOS;
                                                    } else {
                                                        //lay body text cua android
                                                        notiL.body = fbodyAndroid;
                                                        notiL.bodyIOS = fbodyAndroid;
                                                    }
                                                    //html cho ios
                                                    if (!"".equalsIgnoreCase(htmlBodyIos)) {
                                                        notiL.htmlBody = htmlBodyIos;
                                                    } else {
                                                        //body text cho ios
                                                        if (!"".equalsIgnoreCase(fbodyIOS)) {
                                                            notiL.htmlBody = fbodyIOS;
                                                        } else {
                                                            //body text cua android
                                                            notiL.htmlBody = fbodyAndroid;
                                                        }
                                                    }
                                                } else {
                                                    notiL.body = fbodyAndroid;
                                                    notiL.htmlBody = "";

                                                }
                                                logger.info(notiL.toFullJsonObject().encodePrettily());

                                                notiL.refId = mRefId;
                                                listNoti.add(notiL);
                                                Misc.sendNotiFromToolRedis(vertx, notiL);
                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            vertx.cancelTimer(aLong);
                            //todo broadcash to all server to sendNoti.
//                            "delay_time_remind" : 5, //seconds
//                                    "delay_time_cloud" : 5, //seconds
//                                    "off_cloud" : true
                            final long delay_time_remind = glbCfg.getObject("redis", new JsonObject()).getInteger("delay_time_remind", 10);
                            final long delay_time_cloud = glbCfg.getObject("redis", new JsonObject()).getInteger("delay_time_cloud", 300);
                            final boolean off_cloud = glbCfg.getObject("redis", new JsonObject()).getBoolean("off_cloud", false);

                            vertx.setTimer(1000L * delay_time_remind, new Handler<Long>() {
                                @Override
                                public void handle(Long remindTimer) {
                                    vertx.cancelTimer(remindTimer);
                                    Notification notiRemind = new Notification();
                                    Misc.remindSendNoti(vertx, notiRemind);
                                    vertx.setTimer(1000L * delay_time_cloud, new Handler<Long>() {
                                        @Override
                                        public void handle(Long delay_time_cloud) {
                                            //todo check again in Redis and send cloud
                                            vertx.cancelTimer(delay_time_cloud);
                                            //Set periodic send Noti
                                            vertx.setPeriodic(120000L, new Handler<Long>() {
                                                @Override
                                                public void handle(final Long timeGetRedis) {
                                                    JsonObject joGetNumber = new JsonObject();
                                                    joGetNumber.putString(StringConstUtil.BroadcastField.COMMAND, NotificationRedisVerticle.GET_ALL_NOTI);
                                                    vertx.eventBus().sendWithTimeout(AppConstant.RedisVerticle_NOTIFICATION_FROM_TOOL, joGetNumber, 900000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                                                        @Override
                                                        public void handle(AsyncResult<Message<JsonObject>> msgResponseFromRedis) {
                                                            if (msgResponseFromRedis.failed()) {
                                                                logger.info("SEND REDIS FROM TOOL FAIL");
                                                                return;
                                                            }
                                                            logger.info("SEND REDIS VIA CLOUD");
                                                            JsonObject joResponseFromRedis = msgResponseFromRedis.result().body();
                                                            final JsonArray jarrData = joResponseFromRedis.getArray(StringConstUtil.DATA, new JsonArray());
                                                            final AtomicInteger countNoti = new AtomicInteger(jarrData.size());
                                                            final Map<String, String> listCloudNoti = new HashMap<String, String>();
                                                            if (jarrData.size() == 0 || countNoti.intValue() == 0) {
                                                                logger.info("END PERIODIC timeGetRedis");
                                                                vertx.cancelTimer(timeGetRedis);
                                                                return;
                                                            }
                                                            vertx.setPeriodic(100L, new Handler<Long>() {
                                                                @Override
                                                                public void handle(Long timerSendNoti) {
                                                                    int positionNoti = countNoti.decrementAndGet();
                                                                    if (positionNoti < 0) {
                                                                        vertx.cancelTimer(timerSendNoti);
                                                                        logger.info("DONE SEND VIA CLOUD " + mRefId);
                                                                        doInsertNotiAdver(listNoti);
                                                                        return;
                                                                    }
                                                                    JsonObject joDataNoti = jarrData.get(positionNoti);
                                                                    int number = DataUtil.strToInt(joDataNoti.getString(StringConstUtil.NUMBER, "0"));
                                                                    JsonObject joNoti = joDataNoti.getObject(StringConstUtil.NOTIFICATION_OBJ, null);
                                                                    logger.info("SEND REDIS VIA CLOUD position " + positionNoti + " with number " + number);

                                                                    if (joNoti != null) {
                                                                        Notification notiCloud = Notification.parse(joNoti);
                                                                        notiCloud.receiverNumber = number;
                                                                        listCloudNoti.put(notiCloud.receiverNumber + "", notiCloud.toFullJsonObject().toString());
                                                                        if (off_cloud) {
                                                                            logger.info("OFF CLOUD,  PLEASE OPEN");
                                                                        } else {
                                                                            Misc.sendNotiViaCloud(vertx, notiCloud);
                                                                        }

                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            });
                                            return;
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            }
        } //END SEND NOTI VIA FILE
        else if (listphone != null && listphone.size() == 1) {
            String str = "ban cloud cho danh sach ve xem phim dot 1";
            if (listphone.get(0) == 9999) {
                str = "ban cloud tren toan he thong";
            }
            log.add(str, "");

            noti = new Notification();
            noti.receiverNumber = listphone.get(0);
            noti.caption = caption;
            if ("".equalsIgnoreCase(body)) {
                noti.body = bodyIOS;
            } else {
                noti.body = body;
            }
            noti.bodyIOS = bodyIOS;
            noti.sms = "";
//            noti.tranId = -10000L; // ban tren toan he thong fix tool noti
            noti.tranId = -10001L;
            noti.type = notitype;
            noti.status = Notification.STATUS_DISPLAY;
            noti.priority = priority;
            noti.time = System.currentTimeMillis();
            noti.btnTitle = btnTitle;
            noti.btnStatus = btnStatus;
            noti.htmlBody = htmlIos;
            noti.extra = fExtra;
            noti.category = category;

            //ban notification
            Misc.sendNotiViaCloud(vertx, noti);
            log.add(str, noti.toFullJsonObject().encodePrettily());
            log.writeLog();

        } //END SEND NOTI VIA LIST PHONE == 1
        else {

            String phoneOs = "Unknown";
            //android
            if (!"".equalsIgnoreCase(body) && !"".equalsIgnoreCase(bodyIOS)) {
                phoneOs = "";
            } else if (!"".equalsIgnoreCase(body)) {
                phoneOs = "ANDROID";
            } else if (!"".equalsIgnoreCase(bodyIOS)) {
                phoneOs = "iOS";
            }

            if ("Unknown".equalsIgnoreCase(phoneOs)) {
                log.add("khong co noi dung cua android va ios", "");
                log.writeLog();
                callback.handle(new JsonObject().putString("error", "-100")
                        .putString("desc", "Khong co noi dung cua android va ios"));
                return;
            }

            //force to send to device type
            if ("android".equalsIgnoreCase(dtype)) {
                phoneOs = "ANDROID";
            } else if ("ios".equalsIgnoreCase(dtype)) {
                phoneOs = "iOS";
            }

            final String mRefId = "LIST" + System.currentTimeMillis();

            phonesDb.getPage(0, listphone, 1, listphone.size(), phoneOs, new Handler<ArrayList<PhonesDb.Obj>>() {
                @Override
                public void handle(ArrayList<PhonesDb.Obj> objs) {

                    if (objs != null && objs.size() > 0) {

                        for (int i = 0; i < objs.size(); i++) {

                            Common.BuildLog logg = new Common.BuildLog(logger);
                            logg.setPhoneNumber("sendlist");

                            Notification noti = null;

                            if ("android".equalsIgnoreCase(objs.get(i).phoneOs)) {
                                noti = new Notification();
                                noti.caption = fCaption;
                                noti.priority = priority;
                                noti.receiverNumber = objs.get(i).number;
                                noti.os = objs.get(i).phoneOs;
                                noti.token = objs.get(i).pushToken;
                                noti.tranId = -9999L;
                                noti.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
                                noti.body = fbodyAndroid;
                                noti.type = notitype;
                                noti.time = System.currentTimeMillis();
                                noti.btnTitle = btnTitle;
                                noti.btnStatus = btnStatus;
                                noti.htmlBody = htmlBodyIos;
                                noti.extra = fExtra;
                                noti.category = category;

                            } else if ("ios".equalsIgnoreCase(objs.get(i).phoneOs)) {
                                noti = new Notification();
                                noti.caption = fCaption;
                                noti.priority = priority;
                                noti.receiverNumber = objs.get(i).number;
                                noti.os = objs.get(i).phoneOs;
                                noti.token = objs.get(i).pushToken;
                                noti.tranId = -9999L;
                                noti.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
                                noti.body = fbodyIOS;
                                noti.type = notitype;
                                noti.time = System.currentTimeMillis();
                                noti.btnTitle = btnTitle;
                                noti.btnStatus = btnStatus;
                                noti.htmlBody = htmlBodyIos;
                                noti.extra = fExtra;
                                noti.category = category;
                            }

                            noti.refId = mRefId;

                            if (noti != null) {
                                logg.add("number", noti.receiverNumber);
                                logg.add("caption", noti.caption);
                                logg.add("os", noti.os);
                                logg.add("body", noti.body);
                                logg.writeLog();
                                //ban noti
                                Misc.sendNotiViaCloud(vertx, noti);
                            }
                        }
                    }
                }
            });
        }
        /////////////////////////////////////////////////////////

        JsonObject jsonObject = new JsonObject();
        jsonObject.putNumber("error", 0);
        if (!"".equalsIgnoreCase(hfile)) {
            jsonObject.putString("desc", "Tổng số phones sẽ bắn notification " + totalRec);
        } else {
            jsonObject.putString("desc", "Thành công");
        }
        callback.handle(jsonObject);
    }


    public void doInsertNotiAdver(List<Notification> listNotis)
    {
        String host = joMongo.getString("host", "172.16.44.175");
        int port = joMongo.getInteger("port", 27017);
        String dbName = joMongo.getString("db_name", "newmomovn_db");
        MongoClient mongoClientLocal = null;
        try {
            mongoClientLocal = new MongoClient( host , port);
            DB dbLocal = mongoClientLocal.getDB( dbName );
            DBCollection dbwoman = dbLocal.getCollection("noti_tool");
            BulkWriteOperation bulkWriteOperation1 = dbwoman.initializeUnorderedBulkOperation();
            Notification notification;
            Set set;
            Map map;
            DBObject dbObject;
            for(int i = 0; i < listNotis.size() ; i++)
            {
                dbObject = new BasicDBObject();
                notification = listNotis.get(i);
                set = notification.toFullJsonObject().getFieldNames();
                map = notification.toFullJsonObject().toMap();
                for(int j = 0; j < set.size(); j++)
                {
                    dbObject.put(set.toArray()[j].toString().trim(), map.get(set.toArray()[j].toString().trim()).toString().trim());
                    bulkWriteOperation1.insert(dbObject);
                }
            }
            bulkWriteOperation1.execute();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }
    }



}
