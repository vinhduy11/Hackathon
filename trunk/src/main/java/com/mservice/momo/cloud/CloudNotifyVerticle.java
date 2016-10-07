package com.mservice.momo.cloud;

import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.Promo123PhimDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import javapns.Push;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.exceptions.InvalidDeviceTokenFormatException;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotifications;
import org.json.JSONException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by locnguyen on 05/07/2014.
 */
public class CloudNotifyVerticle extends Verticle {

    //final String userNameGCM = "64107010266" + "@gcm.googleapis.com";
    //final String passwordGCM = "AIzaSyAl_PzV2o5sJMJ3-4Joi-OMaDX9p5UXIzc";
    final String apiKeyGCM =    "AIzaSyAl_PzV2o5sJMJ3-4Joi-OMaDX9p5UXIzc";
    final String apiKeyGCMMMT = "AIzaSyCu3z2ImqGIHq_sHsyODhHs3uJGN5LtJhk";
    final String passwordAPNS = "8Y3XRZTH4Z";
    public Logger logger;
    public boolean isStop = false;
    EventBus eb;
    //final String passwordAPNS = "qwer1234";
    private PhonesDb phonesDb;
    private Map<Integer, JsonObject> listCacheToken;

    private boolean allow_send_noti_cloud = false;
    private boolean allow_send_noti_cloud_local = false;

    private boolean sendingNotificationBroadcast = false;
    private Promo123PhimDb promo123PhimDb;

    /*private ArrayList<String> tokenGCM = new ArrayList<String>();

    private ArrayList<String> tokenAPNS = new ArrayList<String>();

    private HashMap<Long, Notification> mapNoti;

    private HashMap<Long, ArrayList<String>> maptokenGCM;

    private HashMap<Long, ArrayList<String>> maptokenAPNS;*/

    private ConcurrentLinkedQueue<BatchObj> QWaiting = new ConcurrentLinkedQueue<>();

    private HashMap<Long,BatchObj> map = new HashMap<>();
    private int batchSize = 300;
    //sendNotiFrom123PhimTrack
    private int phim123BatchSize = 170;

    public void addToMap(Notification noti){

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + noti.receiverNumber);
        log.add("time", noti.time);
        log.add("os",noti.os);

        BatchObj batch =  map.get(noti.time);

        if(batch==null) {
            log.add("Add new batch","");
            batch = new BatchObj();
            batch.noti = noti;
            map.put(noti.time, batch);
        }

        //ios
        if("ios".equalsIgnoreCase(noti.os)){
            if(!"".equalsIgnoreCase(noti.token) && noti.token.length() == 64 ){
                batch.listTokenAPN.add(noti.token);
            }else{
                //reset token in phones
                JsonObject joUp = new JsonObject();
                joUp.putNumber(colName.PhoneDBCols.NUMBER, noti.receiverNumber);
                joUp.putString(colName.PhoneDBCols.PUSH_TOKEN,"");
                phonesDb.update(noti.receiverNumber,joUp,new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {}
                });

                //remove from cache
               JsonObject jo =  listCacheToken.get(noti.receiverNumber);
                if(jo!=null){
                    listCacheToken.remove(jo);
                }
            }

            log.add("add token to listTokenAPN","");
        }else if("android".equalsIgnoreCase(noti.os)){
            batch.listTokenGCM.add(noti.token);
            log.add("add token to listTokenGCM","");
        }else {
            log.add("notification khong phai la IOS/ ANDROID","");
        }

        log.writeLog();
    }

    @Override
    public void start() {

        container.logger().info("Start CloudNotifyVerticle");

        JsonObject glbCfg = container.config();

        allow_send_noti_cloud = glbCfg.getBoolean("allow_send_noti_cloud", false);
        allow_send_noti_cloud_local = glbCfg.getBoolean("allow_send_noti_cloud_local", false);
        eb = vertx.eventBus();
        logger = container.logger();

        phonesDb = new PhonesDb(eb, logger);

        listCacheToken = new HashMap<>();

        Handler<Message<JsonObject>> cloudNotiHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> jsonObjectMessage) {
                final Notification noti = Notification.parse(jsonObjectMessage.body());

                if (noti.tranId == -9999L
                        && noti.token!=null && !noti.token.isEmpty()
                        && noti.os!=null && !noti.os.isEmpty()) {

                    addToMap(noti);
                    jsonObjectMessage.reply(jsonObjectMessage.body());
                    return;
                }

                final Common.BuildLog log = new Common.BuildLog(logger);
                /*log.add("function", "sendtocloud");
                log.add("notification", noti.toFullJsonObject());
                log.setPhoneNumber("0" + noti.receiverNumber);
                log.add("Cache token in sock data: ", noti.token);
                log.add("Cache os in sock data: ", noti.os);*/

                // send all
                if (noti.receiverNumber == 9999 || noti.receiverNumber == 7777 || noti.receiverNumber == 8888) {
                    isStop = false;
                    sendAll(noti, noti.receiverNumber);
                }
//                else if(noti.receiverNumber ==1111){
//                    isStop = false;
//                    sendNotiFrom123PhimTrack(noti);
//                }else if(noti.receiverNumber ==5555){
//                    sendNotiGTBB(noti);
//                }

                // stop sending
                else if (noti.receiverNumber == 4444) {
                    isStop = true;
                } else if (!sendOne(noti, log)) {
                    //log.add("Don't have cache token in sock data", "");
                    if (listCacheToken.get(noti.receiverNumber) != null) {
                        noti.os = listCacheToken.get(noti.receiverNumber).getString("os");
                        noti.token = listCacheToken.get(noti.receiverNumber).getString("token");
                    }
                    //log.add("Cache token in cache list: ", noti.token);
                    //log.add("Cache os in cache list: ", noti.os);
                    if (!sendOne(noti, log)) {
                        //log.add("Don't have cache token in cache list", "");
                        phonesDb.getPhoneObjInfo(noti.receiverNumber, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj obj) {
                                if (obj == null) {
                                    //log.add("Don't have token in phoneDB", "");
                                    //log.writeLog();
                                    jsonObjectMessage.reply(jsonObjectMessage.body());
                                    return;
                                }

                                noti.token = obj.pushToken;
                                noti.os = obj.phoneOs;

                                //log.add("Token in PhoneDB: ", noti.token);
                                //log.add("OS  in PhoneDB: ", noti.os);

                                if (!sendOne(noti, log)) {
                                    log.add("Don't have token in phoneDB", "");
                                    log.writeLog();
                                }
                                jsonObjectMessage.reply(jsonObjectMessage.body());
                            }
                        });
                    }
                }
            }
        };

        Handler<Message<JsonObject>> updateHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> msg) {

                JsonObject jo = msg.body();
                logger.info("CloundNotifyVerticleUpdate receive: " + jo);
                String token = jo.getString(colName.PhoneDBCols.PUSH_TOKEN, "");
                String os = jo.getString(colName.PhoneDBCols.PHONE_OS, "");
                int number = jo.getInteger(colName.PhoneDBCols.NUMBER, 0);

                if (number > 0 && !token.isEmpty() && !os.isEmpty()) {
                    JsonObject obj = new JsonObject();
                    obj.putString("token", token);
                    obj.putString("os", os);

                    listCacheToken.put(number, obj);
                }
                msg.reply(jo);
            }
        };

        //cho phep send cloud
        if (allow_send_noti_cloud || allow_send_noti_cloud_local) {
            eb.registerHandler(AppConstant.CloundNotifyVerticle, cloudNotiHandler);

            sendBatch();
        }
        eb.registerLocalHandler(AppConstant.CloundNotifyVerticleUpdate, updateHandler);

        // test send
//        Notification noti = new Notification();
//        noti.receiverNumber = 1111;
//        noti.caption = "caption";
//        noti.body = "body";
//        noti.bodyIOS = "bodyIOS";
//        noti.sms = "";
//        noti.tranId= -10000L;
//        noti.type = 0;
//        noti.priority = 2;
//        noti.time = System.currentTimeMillis();
//
//        ArrayList<String> listTokenTemp = new ArrayList<>();
//        listTokenTemp.add("99a3fd65aea0bde990c923668ff58a439fcd40c41ccbea667dc2b13c8f56b609");
//        listTokenTemp.add("53df6e304dae7cd8723aa86d4f884002584ad4721428e3898fba50e0ba5f0b55");
//
//        sendListtoAPNS(listTokenTemp,noti);
    }

    private void sendBatch() {
        vertx.setPeriodic(10000, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {

                Set<Long> keys = map.keySet();
                Iterator<Long> iterator =  keys.iterator();
                Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("timersendcloud");
                if (iterator.hasNext()) {
                    BatchObj obj = map.remove(iterator.next());
                    log.add("Waiting remaining size", map.size());

                    if(obj.listTokenGCM != null && obj.listTokenGCM.size() > 0){
                        log.add("send GCM", "");
                        log.add("number of token GCM",obj.listTokenGCM.size());

                        sendListtoGCM(obj.listTokenGCM, obj.noti);

                        log.add("clear GCM list of token  from memory","");
                        //clear data
                        obj.listTokenGCM.clear();
                    }

                    if(obj.listTokenAPN != null && obj.listTokenAPN.size() > 0){

                        log.add("send APN", "");
                        log.add("number of token APN",obj.listTokenAPN.size());

                        sendListtoAPNS(obj.listTokenAPN,obj.noti);

                        log.add("clear APN list of token  from memory","");
                        //clear data
                        obj.listTokenAPN.clear();
                    }

                    log.writeLog();
                    return;

                }else{
                    log.add("Waiting MAP there is no data to send batch", "");
                }
                log.writeLog();
            }
        });
    }

    public boolean sendOne(final Notification noti, final Common.BuildLog log) {

        if (noti.token != null
                && !noti.token.isEmpty()
                && noti.os != null
                && !noti.os.isEmpty()) {

            ArrayList<String> token = new ArrayList<>();
            token.add(noti.token);

            if ("ANDROID".equalsIgnoreCase(noti.os.toUpperCase())) {
                listCacheToken.put(noti.receiverNumber
                        , new JsonObject().putString("os", noti.os)
                        .putString("token", noti.token));
                sendListtoGCM(token, noti);
            } else if ("IOS".equalsIgnoreCase(noti.os.toUpperCase())) {

                //64 token ios has length = 64
                if (!"".equalsIgnoreCase(noti.token) && noti.token.length() == 64){
                    listCacheToken.put(noti.receiverNumber
                            , new JsonObject().putString("os", noti.os)
                            .putString("token", noti.token));
                    sendListtoAPNS(token, noti);
                }else{
                    //remove token from DB
                    JsonObject joUp = new JsonObject();
                    joUp.putNumber(colName.PhoneDBCols.NUMBER,noti.receiverNumber);
                    joUp.putString(colName.PhoneDBCols.PUSH_TOKEN,"");
                    phonesDb.update(noti.receiverNumber,joUp,new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            logger.info("0" + noti.receiverNumber + "|" + "remove invalid token ios: " + noti.token);
                        }
                    });

                    //remove token from cache
                    JsonObject jo= listCacheToken.get(noti.receiverNumber);
                    if(jo != null){
                        listCacheToken.remove(jo);
                    }
                }
            } else {
                logger.info("not support for " + noti.os);
            }
            return true;
        }

        return false;
    }

    public void sendAll(final Notification noti, final int groupNumber) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("loadpage");
        log.add("function", "send");
        log.add("pagesize", batchSize);
        if (sendingNotificationBroadcast)
        {
            log.add("ERROR", "Notification đang được gửi broadcast. Chờ 1 minute nữa!!!");
        }
        sendingNotificationBroadcast = true;

        final Notification sharedNoti = new Notification();
        sharedNoti.receiverNumber = noti.receiverNumber;
        sharedNoti.caption = noti.caption;
        sharedNoti.body = noti.body;
        sharedNoti.bodyIOS = noti.bodyIOS;
        sharedNoti.sms = noti.sms;
        sharedNoti.tranId= noti.tranId; // ban tren toan he thong
        sharedNoti.status = noti.status;
        sharedNoti.type = noti.type;
        sharedNoti.priority = noti.priority;
        sharedNoti.time =  noti.time;
        sharedNoti.refId = noti.id;
        sharedNoti.btnTitle = noti.btnTitle;
        sharedNoti.btnStatus = noti.btnStatus;

        final String fBank = (noti.extra == null ? "" : (new JsonObject(noti.extra).getString("bank", "")));

        final String deviceType = (noti.extra == null ? "" : (new JsonObject(noti.extra).getString("devicetype", "")));

        final Set<Integer> sets = new HashSet<>();
        //reset id
        noti.id = null;
        vertx.setPeriodic(4000, new Handler<Long>() {
            boolean sendNext = true;
            int mFromNumber = 0;
            long mTotal = 0;
            @Override
            public void handle(final Long timerId) {

                if(!sendNext){
                    log.add("wating load batch","");
                    log.writeLog();
                    return;
                }
                sendNext = false;

                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("loadpage");
                log.add("batch size", batchSize);
                log.add("device type", deviceType);

                String phoneOs = "Unknown";
                //android
                if(!"".equalsIgnoreCase(sharedNoti.body ) && !"".equalsIgnoreCase(sharedNoti.bodyIOS)){
                    phoneOs = "";
                }else if(!"".equalsIgnoreCase(sharedNoti.body )){
                    phoneOs= "ANDROID";
                }else if(!"".equalsIgnoreCase(sharedNoti.bodyIOS )){
                    phoneOs= "iOS";
                }

                log.add("phoneOs", phoneOs);

                if("Unknown".equalsIgnoreCase(phoneOs)){
                    log.add("khong co noi dung cua android va ios","");
                    log.writeLog();
                    return;
                }

                //force to send by device type
                if("android".equalsIgnoreCase(deviceType)){
                    phoneOs= "ANDROID";
                }else if("ios".equalsIgnoreCase(deviceType)){
                    phoneOs= "iOS";
                }

                log.add("mFromNumber", mFromNumber);
                log.add("device type", deviceType);
                phonesDb.getPage(groupNumber, null, mFromNumber, batchSize, phoneOs, fBank, new Handler<ArrayList<PhonesDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<PhonesDb.Obj> objs) {

                        log.add("phones count", (objs != null ? objs.size() : 0));

                        if (objs != null && objs.size() > 0) {

                            mTotal += objs.size();
                            for (int i = 0; i < objs.size(); i++) {
                                sets.add(objs.get(i).number);

                                mFromNumber = objs.get(i).number;
                                if (!isStop) {
                                    noti.receiverNumber = objs.get(i).number;
                                    noti.os = objs.get(i).phoneOs;
                                    noti.token = objs.get(i).pushToken;
                                    noti.tranId = -9999L;
                                    noti.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
                                    noti.refId = sharedNoti.refId;
                                    noti.btnTitle = sharedNoti.btnTitle;
                                    noti.btnStatus = sharedNoti.btnStatus;
                                    if("android".equalsIgnoreCase(objs.get(i).phoneOs)){
                                        noti.body = sharedNoti.body;

                                    }else if("ios".equalsIgnoreCase(objs.get(i).phoneOs)){
                                        noti.body = sharedNoti.bodyIOS;
                                    }

                                    //ban noti
                                    log.add("number", objs.get(i).number);

                                    Misc.sendNoti(vertx, noti);

                                    //log.add("notification", noti.toJsonObject());
                                }
                            }
                            if (!isStop) {
                                sendNext = true;
                            }
                        }

                        if (objs == null || objs.size() < batchSize) {
                            vertx.cancelTimer(timerId);
                            log.add("Total message number " , mTotal);
                            log.add("Send noti " , "done!");
                            log.add("Count", sets.size());

                            sendingNotificationBroadcast = false;
                        }
                        log.writeLog();
                    }
                });
            }
        });
    }

    public void sendNotiGTBB(final Notification noti) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("loadpage");
        log.add("function", "send");

        log.add("pagesize", batchSize);
        if (sendingNotificationBroadcast) {
            log.add("ERROR", "Notification đang được gửi broadcast. Chờ 1 minute nữa!!!");

        }
        sendingNotificationBroadcast = true;

        final Notification sharedNoti = new Notification();

        sharedNoti.receiverNumber = noti.receiverNumber;
        sharedNoti.caption = noti.caption;
        sharedNoti.body = noti.body;
        sharedNoti.bodyIOS = noti.bodyIOS;
        sharedNoti.sms = noti.sms;
        sharedNoti.tranId= noti.tranId; // ban tren toan he thong
        sharedNoti.status = noti.status;
        sharedNoti.type = noti.type;
        sharedNoti.priority = noti.priority;
        sharedNoti.time =  noti.time;
        sharedNoti.refId = noti.id;
        sharedNoti.btnTitle = noti.btnTitle;
        sharedNoti.btnStatus = noti.btnStatus;

        final Set<Integer> sets = new HashSet<>();
        //reset id
        noti.id = null;
        vertx.setPeriodic(6000, new Handler<Long>() {
            boolean sendNext = true;
            int mFromNumber = 0;
            boolean isInviter = true;
            long mTotal = 0;
            @Override
            public void handle(final Long timerId) {

                if(!sendNext){
                    log.add("wating load batch","");
                    log.writeLog();
                    return;
                }
                sendNext = false;

                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("loadpage");
                log.add("batch size", batchSize);

                String phoneOs = "Unknown";
                //android
                if(!"".equalsIgnoreCase(sharedNoti.body ) && !"".equalsIgnoreCase(sharedNoti.bodyIOS)){
                    phoneOs = "";
                }else if(!"".equalsIgnoreCase(sharedNoti.body )){
                    phoneOs= "ANDROID";
                }else if(!"".equalsIgnoreCase(sharedNoti.bodyIOS )){
                    phoneOs= "iOS";
                }

                log.add("phoneOs", phoneOs);

                if("Unknown".equalsIgnoreCase(phoneOs)){
                    log.add("khong co noi dung cua android va ios","");
                    log.writeLog();
                    return;
                }

                log.add("mFromNumber", mFromNumber);
                phonesDb.getPageIsInviter(null
                                        ,isInviter
                                        ,mFromNumber
                                        ,batchSize
                                        ,phoneOs
                                        ,new Handler<ArrayList<PhonesDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<PhonesDb.Obj> objs) {

                        log.add("phones count", (objs != null ? objs.size() : 0));

                        if (objs != null && objs.size() > 0) {

                            mTotal += objs.size();

                            //todo send data first, and then send cloud

                            for (int i = 0; i < objs.size(); i++) {
                                sets.add(objs.get(i).number);

                                mFromNumber = objs.get(i).number;

                                logger.info("NUMBER " + objs.get(i).number);

                                if (!isStop) {
                                    noti.receiverNumber = objs.get(i).number;
                                    noti.os = objs.get(i).phoneOs;
                                    noti.token = objs.get(i).pushToken;
                                    noti.tranId = -9999L;
                                    noti.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
                                    noti.refId = sharedNoti.refId;
                                    noti.btnTitle = sharedNoti.btnTitle;
                                    noti.btnStatus = sharedNoti.btnStatus;
                                    if("android".equalsIgnoreCase(objs.get(i).phoneOs)){
                                        noti.body = sharedNoti.body;

                                    }else if("ios".equalsIgnoreCase(objs.get(i).phoneOs)){
                                        noti.body = sharedNoti.bodyIOS;
                                    }

                                    //ban noti
                                    log.add("number", objs.get(i).number);
                                    Misc.sendNoti(vertx, noti);
                                }
                            }

                            //sendCloudOnly(objs, noti, sharedNoti);

                            if (!isStop) {
                                sendNext = true;
                            }
                        }

                        if (objs == null || objs.size() < batchSize) {
                            vertx.cancelTimer(timerId);
                            log.add("Total message number " , mTotal);
                            log.add("Send noti " , "done!");
                            log.add("Count", sets.size());

                            sendingNotificationBroadcast = false;
                        }
                        log.writeLog();
                    }
                });
            }
        });
    }

    private void sendCloudOnly(ArrayList<PhonesDb.Obj> objs, Notification noti, Notification sharedNoti) {
        //todo chi ban cloud khong thoi
        ArrayList<String> listTokenAndroid = new ArrayList<>();
        ArrayList<String> listTokenIos = new ArrayList<>();

        //noti for android via cloud
        Notification notiAndroid = new Notification(noti);
        notiAndroid.receiverNumber = 0;
        notiAndroid.os = "ANDROID";
        notiAndroid.token = "";
        notiAndroid.tranId = -9999L;
        notiAndroid.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
        notiAndroid.refId = sharedNoti.refId;
        notiAndroid.btnTitle = sharedNoti.btnTitle;
        notiAndroid.btnStatus = sharedNoti.btnStatus;
        notiAndroid.body = sharedNoti.body;


        //noti for ios via cloud
        Notification notiIOS = new Notification(noti);
        notiIOS.receiverNumber = 0;
        notiIOS.os = "iOS";
        notiIOS.token = "";
        notiIOS.tranId = -9999L;
        notiIOS.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
        notiIOS.refId = sharedNoti.refId;
        notiIOS.btnTitle = sharedNoti.btnTitle;
        notiIOS.btnStatus = sharedNoti.btnStatus;
        notiIOS.body = sharedNoti.bodyIOS;

        if(objs!= null && objs.size() > 0){

            for (int i=0;i<objs.size();i++){
                PhonesDb.Obj o = objs.get(i);

                if("android".equalsIgnoreCase(o.phoneOs) && !"".equalsIgnoreCase(o.pushToken)){
                    listTokenAndroid.add(o.pushToken);
                }else if("ios".equalsIgnoreCase(o.phoneOs) && !"".equalsIgnoreCase(o.pushToken)){
                    listTokenIos.add(o.pushToken);
                }else{
                    logger.info("sendNotiFrom123PhimTrack not support for " + o.phoneOs);
                }
            }

            if(listTokenAndroid.size() > 0){
                sendListtoGCM(listTokenAndroid,notiAndroid);
                listTokenAndroid.clear();
            }

            if(listTokenIos.size() > 0){
                sendListtoAPNS(listTokenIos, notiIOS);
                listTokenIos.clear();
            }

        }else{
            logger.info("No Phone Object to send cloud");

        }
    }

    public void sendNotiFrom123PhimTrack(final Notification noti) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("loadpage123phim");
        log.add("function", "sendNotiFrom123PhimTrack");

        log.add("pagesize123phim", phim123BatchSize);

        //sendingNotificationBroadcast = true;

        final Notification sharedNoti = new Notification();

        sharedNoti.receiverNumber = noti.receiverNumber;
        sharedNoti.caption = noti.caption;
        sharedNoti.body = noti.body;
        sharedNoti.bodyIOS = noti.bodyIOS;
        sharedNoti.sms = noti.sms;
        sharedNoti.tranId= noti.tranId; //ban tren toan he thong
        sharedNoti.status = noti.status;
        sharedNoti.type = noti.type;
        sharedNoti.priority = noti.priority;
        sharedNoti.time =  noti.time;
        sharedNoti.refId = noti.id;
        sharedNoti.btnTitle = noti.btnTitle;
        sharedNoti.btnStatus = noti.btnStatus;

        final Set<Integer> sets = new HashSet<>();
        //reset id
        noti.id = null;

        vertx.setPeriodic(8000,new Handler<Long>() {
            boolean sendNext = true;
            int mFromNumber = 0;
            int mTotal = 0;
            @Override
            public void handle(final Long aLong) {

                if(!sendNext){
                    log.add("wating load batch of 123phim","");
                    log.writeLog();
                    return;
                }
                sendNext = false;

                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("loadpage123phim");
                log.add("batch size", phim123BatchSize);

                String phoneOs = "Unknown";
                //android
                if(!"".equalsIgnoreCase(sharedNoti.body ) && !"".equalsIgnoreCase(sharedNoti.bodyIOS)){
                    phoneOs = "";
                }else if(!"".equalsIgnoreCase(sharedNoti.body )){
                    phoneOs= "ANDROID";
                }else if(!"".equalsIgnoreCase(sharedNoti.bodyIOS )){
                    phoneOs= "iOS";
                }

                log.add("phoneOs", phoneOs);

                if("Unknown".equalsIgnoreCase(phoneOs)){
                    log.add("khong co noi dung cua android va ios","");
                    log.writeLog();
                    return;
                }

                log.add("mFromNumber", mFromNumber);

                promo123PhimDb.getPage(mFromNumber,phim123BatchSize,new Handler<ArrayList<Integer>>() {
                    @Override
                    public void handle(ArrayList<Integer> integers) {
                        if(integers == null || integers.size() == 0){
                            sendNext = false;
                            vertx.cancelTimer(aLong);
                        }
                        //set phone for next round
                        mFromNumber = integers.get(integers.size() -1);
                        mTotal += integers.size();

                        logger.info("TOTALSEND " + mTotal);

                        //lay danh sach phone trong list
                        phonesDb.getPhoneList(integers,"",new Handler<ArrayList<PhonesDb.Obj>>() {
                            @Override
                            public void handle(ArrayList<PhonesDb.Obj> objs) {
                                log.add("phones count", (objs != null ? objs.size() : 0));

                                if (objs != null && objs.size() > 0) {

                                    //todo ban data truoc --> ban cloud sau : bi nhieu cai duplicate
                                    for (int i = 0; i < objs.size(); i++) {

                                        if (!isStop) {
                                            noti.receiverNumber = objs.get(i).number;
                                            noti.os = objs.get(i).phoneOs;
                                            noti.token = objs.get(i).pushToken;
                                            noti.tranId = -9999L;
                                            noti.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
                                            noti.refId = sharedNoti.refId;
                                            noti.btnTitle = sharedNoti.btnTitle;
                                            noti.btnStatus = sharedNoti.btnStatus;
                                            if("android".equalsIgnoreCase(objs.get(i).phoneOs)){
                                                noti.body = sharedNoti.body;

                                            }else if("ios".equalsIgnoreCase(objs.get(i).phoneOs)){
                                                noti.body = sharedNoti.bodyIOS;
                                            }

                                            log.add("number", objs.get(i).number);

                                            //todo ban noti
                                            Misc.sendNoti(vertx, noti);
                                        }
                                    }

                                    //quet vong tiep theo
                                    sendNext = true;
                                }
                                log.writeLog();
                            }
                        });
                    }
                });
            }
        });

        vertx.setPeriodic(4000, new Handler<Long>() {
            boolean sendNext = true;
            int mFromNumber = 0;
            long mTotal = 0;
            @Override
            public void handle(final Long timerId) {

                if(!sendNext){
                    log.add("wating load batch","");
                    log.writeLog();
                    return;
                }
                sendNext = false;

                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("loadpage");
                log.add("batch size", batchSize);

                String phoneOs = "Unknown";
                //android
                if(!"".equalsIgnoreCase(sharedNoti.body ) && !"".equalsIgnoreCase(sharedNoti.bodyIOS)){
                    phoneOs = "";
                }else if(!"".equalsIgnoreCase(sharedNoti.body )){
                    phoneOs= "ANDROID";
                }else if(!"".equalsIgnoreCase(sharedNoti.bodyIOS )){
                    phoneOs= "iOS";
                }

                log.add("phoneOs", phoneOs);

                if("Unknown".equalsIgnoreCase(phoneOs)){
                    log.add("khong co noi dung cua android va ios","");
                    log.writeLog();
                    return;
                }

                log.add("mFromNumber", mFromNumber);
                phonesDb.getPage(0, null, mFromNumber, batchSize,phoneOs, new Handler<ArrayList<PhonesDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<PhonesDb.Obj> objs) {

                        log.add("phones count", (objs != null ? objs.size() : 0));

                        if (objs != null && objs.size() > 0) {

                            mTotal += objs.size();
                            for (int i = 0; i < objs.size(); i++) {
                                sets.add(objs.get(i).number);

                                mFromNumber = objs.get(i).number;
                                if (!isStop) {
                                    noti.receiverNumber = objs.get(i).number;
                                    noti.os = objs.get(i).phoneOs;
                                    noti.token = objs.get(i).pushToken;
                                    noti.tranId = -9999L;
                                    noti.status = Notification.STATUS_DETAIL; // cho phep xem chi tiet noi dung
                                    noti.refId = sharedNoti.refId;
                                    noti.btnTitle = sharedNoti.btnTitle;
                                    noti.btnStatus = sharedNoti.btnStatus;
                                    if("android".equalsIgnoreCase(objs.get(i).phoneOs)){
                                        noti.body = sharedNoti.body;

                                    }else if("ios".equalsIgnoreCase(objs.get(i).phoneOs)){
                                        noti.body = sharedNoti.bodyIOS;
                                    }

                                    //ban noti
                                    log.add("number", objs.get(i).number);

                                    Misc.sendNoti(vertx, noti);
                                }
                            }
                            if (!isStop) {
                                sendNext = true;
                            }
                        }

                        if (objs == null || objs.size() < batchSize) {
                            vertx.cancelTimer(timerId);
                            log.add("Total message number " , mTotal);
                            log.add("Send noti " , "done!");
                            log.add("Count", sets.size());

                            sendingNotificationBroadcast = false;
                        }
                        log.writeLog();
                    }
                });
            }
        });
    }

    public void sendListtoGCM(ArrayList<String> token, Notification noti) {
        // test
//        token.clear();
//        token.add("abc");
//        token.add("APA91bF7I40Va9eJX1QhrCKlXlInOQqVLILod215t4uRCoJWxknwskr4QdD9NVItZ69ii_f4F0G49_qFGwBzpkgIf2TbG5DUj9up9_oUDXya_oPe9i7zUg1EhXC4coZJUb3axsG4FPJ8r_Mln4vIuBlpR-d_EJ7u92qhrWeUEAVIAzLQ920OT-w");
//   a Tổng     token.add("APA91bFvGmHP-9ptnxn6d2o5d1KXDOcpJ3fP117UrMSThvtrqF-sG5TEkLr0Xcc3IebY2NCJzki327chnm4jAXsvtEpNlrL06Keq18hhAgtSI8H7zwXLzc7ha8P4b0KO9VTaUjGZZE4OHVVW3i0MRekUigHKvYVzP4IvkBvqkomtCDEJmbiFFlw");
//        token.add("APA91bEUkpybFsAMFtFwPg1lx6dO_Ai7OBWrh_eTEPYmVcaD8uI_XuGoxIbtE_SNG7pTDgpDK33Uw9nyQOAXMPuukfuN2y452r4Lc9InYALCa1IIWTmr2TVf-Mu93o1AqF4zZT1XJ67ZYyJ6wRhQOGZFnFvkJ8wBSqwN_akyLUWza78HYFBTUak");
//        token.add("APA91bG1IR66mepzoF9_Yj2Gsqv5R0hMcUAYbMgnfPblKlZ2nouyAjAfzA4NTbRFiKwFB00Ze6h0YAc-aOoah6xaZFaUNkFTw6fLAux3Tl8RV9RIDh2MfyfCfwnqg7qcH1vkq7We2EWxffBLa82QQWxjqg_eq9aMXkEQL5EP6ECGGv097iR6020");
//        token.add("APA91bEzqdm5MHZUuB0aMZVq8KyAUGveDB_FtGzxhgSMcLgAMWhhZ2div5ANbTMG18-6nRTBObBruk0iTgBmdmmFhxphjwJhGgGz33ON9oSWQoRU9aPVuQr_TpMs_lvB2lhZKl8FGPleOXdUCOufrVWhKZjzjgYtYYWcsD5JT-eNRcMgcpUMaXM");
        //

        Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "sendtoGCM");
        //log.add("prefix", noti.prefix);
        //log.add("total token", token.size());

        if(token.size()>1){
            log.setPhoneNumber("sendbatch");
        }else{
            log.setPhoneNumber("0" + noti.receiverNumber);
        }
        log.add("message", noti.getMessageForAndroid());

        Sender sender = new Sender(apiKeyGCM);
        MulticastResult result;

        com.google.android.gcm.server.Message.Builder messageBuidler = new com.google.android.gcm.server.Message.Builder();

        // build message to send
        messageBuidler.addData("message", noti.getMessageForAndroid().toString());

        // test.start
//        token.clear();
//        if (token.size() == 0){
//            log.writeLog();
//            return;
//        }
        //test.end

        // send
        try {
            result = sender.sendNoRetry(messageBuidler.build(), token);
            //log.add("Send to GCM - Result success: ", result.getSuccess() + "");
            //log.add("Send to GCM - Result failure: ", result.getFailure() + "");
            /*for (int i = 0; i < result.getResults().size(); i++) {
                //log.add("Result token " + (i + 1) + " : ", result.getResults().get(i) + "");
            }*/

        }catch (IOException e) {
            //log.add("exception", "IOException " + e.getMessage());
        }catch (Exception e) {
            //log.add("exception", e.getMessage());
        }
        log.writeLog();
    }

    public void sendListtoGCMMMT(ArrayList<String> token, Notification noti, Logger logger) {
        // test
//        token.clear();
//        token.add("abc");
//        token.add("APA91bF7I40Va9eJX1QhrCKlXlInOQqVLILod215t4uRCoJWxknwskr4QdD9NVItZ69ii_f4F0G49_qFGwBzpkgIf2TbG5DUj9up9_oUDXya_oPe9i7zUg1EhXC4coZJUb3axsG4FPJ8r_Mln4vIuBlpR-d_EJ7u92qhrWeUEAVIAzLQ920OT-w");
//   a Tổng     token.add("APA91bFvGmHP-9ptnxn6d2o5d1KXDOcpJ3fP117UrMSThvtrqF-sG5TEkLr0Xcc3IebY2NCJzki327chnm4jAXsvtEpNlrL06Keq18hhAgtSI8H7zwXLzc7ha8P4b0KO9VTaUjGZZE4OHVVW3i0MRekUigHKvYVzP4IvkBvqkomtCDEJmbiFFlw");
//        token.add("APA91bEUkpybFsAMFtFwPg1lx6dO_Ai7OBWrh_eTEPYmVcaD8uI_XuGoxIbtE_SNG7pTDgpDK33Uw9nyQOAXMPuukfuN2y452r4Lc9InYALCa1IIWTmr2TVf-Mu93o1AqF4zZT1XJ67ZYyJ6wRhQOGZFnFvkJ8wBSqwN_akyLUWza78HYFBTUak");
//        token.add("APA91bG1IR66mepzoF9_Yj2Gsqv5R0hMcUAYbMgnfPblKlZ2nouyAjAfzA4NTbRFiKwFB00Ze6h0YAc-aOoah6xaZFaUNkFTw6fLAux3Tl8RV9RIDh2MfyfCfwnqg7qcH1vkq7We2EWxffBLa82QQWxjqg_eq9aMXkEQL5EP6ECGGv097iR6020");
//        token.add("APA91bEzqdm5MHZUuB0aMZVq8KyAUGveDB_FtGzxhgSMcLgAMWhhZ2div5ANbTMG18-6nRTBObBruk0iTgBmdmmFhxphjwJhGgGz33ON9oSWQoRU9aPVuQr_TpMs_lvB2lhZKl8FGPleOXdUCOufrVWhKZjzjgYtYYWcsD5JT-eNRcMgcpUMaXM");
        //

        Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "sendtoGCM");
        //log.add("prefix", noti.prefix);
        //log.add("total token", token.size());

        if(token.size()>1){
            log.setPhoneNumber("sendbatch");
        }else{
            log.setPhoneNumber("0" + noti.receiverNumber);
        }
        log.add("message", noti.getMessageForAndroid());

        String s = "AIzaSyCu3z2ImqGIHq_sHsyODhHs3uJGN5LtJhk";
        Sender sender = new Sender( s);
        MulticastResult result;

        com.google.android.gcm.server.Message.Builder messageBuidler = new com.google.android.gcm.server.Message.Builder();

        // build message to send
        messageBuidler.addData("message", noti.getMessageForAndroid().toString());

        // test.start
//        token.clear();
//        if (token.size() == 0){
//            log.writeLog();
//            return;
//        }
        //test.end

        // send
        try {
            result = sender.sendNoRetry(messageBuidler.build(), token);
            logger.info("A");
            //log.add("Send to GCM - Result success: ", result.getSuccess() + "");
            //log.add("Send to GCM - Result failure: ", result.getFailure() + "");
            /*for (int i = 0; i < result.getResults().size(); i++) {
                //log.add("Result token " + (i + 1) + " : ", result.getResults().get(i) + "");
            }*/

        }catch (IOException e) {
            log.add("exception", "IOException " + e.getMessage());
        }catch (Exception e) {
            log.add("exception", e.getMessage());
        }
        log.writeLog();
    }

    public void sendListtoAPNS(ArrayList<String> token, Notification noti) {
        // test
//        token.clear();
//        token.add("abc");
//        token.add("a24839c93ca3cc7268c7e0d9241571f87e39899f77eccefdccaa7831253c5192");
//        token.add("53df6e304dae7cd8723aa86d4f884002584ad4721428e3898fba50e0ba5f0b55");
//        token.add("99a3fd65aea0bde990c923668ff58a439fcd40c41ccbea667dc2b13c8f56b609");
//        token.add("a7049f8a3fac22124cc5c1a85dae93d55df3c3eecb8107f9b2cb51f8444ab8df");
        //

        Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "sendtoAPNS");
        log.add("prefix", noti.prefix);

        if(token.size()>1){
            log.setPhoneNumber("sendbatch");
        }else{
            log.setPhoneNumber("0" + noti.receiverNumber);
        }
        log.add("total token", token.size());
        for (int i = 0; i < token.size(); i++) {
            log.add("token " + (i + 1), token.get(i));
        }

        //InputStream keyStoreAPNS = this.getClass().getResourceAsStream("/iphone_test.p12");
        InputStream keyStoreAPNS = this.getClass().getResourceAsStream("/aps_production.p12");
        PushedNotifications result;

        /* Build a blank payload to customize */
        PushNotificationPayload payload = PushNotificationPayload.complex();

        try {
            // build message to send
            payload.addCustomDictionary("type", noti.type);
            log.add("type", noti.type);

            if (noti.id != null && !noti.id.isEmpty()) {
                payload.addCustomDictionary("id", noti.id);
                log.add("id", noti.id + "");
            }
            if (noti.refId != null && !noti.refId.isEmpty()) {
                payload.addCustomDictionary("refId", noti.refId);
            }
            if (noti.btnTitle != null && !noti.btnTitle.isEmpty()) {
                payload.addCustomDictionary("btnTitle", noti.btnTitle);
            }
            if (noti.btnStatus != null){
                payload.addCustomDictionary("btnStatus", noti.btnStatus);
            }

            // format lenght payload
            int cutLength = 256 - payload.getPayloadSize();
            payload.addAlert(noti.getBodyForIOS(cutLength));
            payload.addSound("default");
            while (payload.getPayloadSize() > 256) {
                payload.addAlert(noti.getBodyForIOS(--cutLength));
            }

            log.add("body", noti.getBodyForIOS(cutLength));

            // test.start
//            token.clear();
//            if (token.size() == 0){
//                log.writeLog();
//                return;
//            }
            // test.end

            // send
            result = Push.payload(payload
                    , keyStoreAPNS
                    , passwordAPNS
                    , true
                    , 10
                    , token);

            log.add("Send to APNS - success:", result.getSuccessfulNotifications().size() + "");
            log.add("Send to APNS - failure:", result.getFailedNotifications().size() + "");
            /*for (int i = 0; i < result.size(); i++) {
                log.add("Result token " + (i + 1) + ":", result.get(i) + "");
            }*/

        } catch (CommunicationException e) {
            log.add("exception", "CommunicationException " + e.getMessage());
        } catch (KeystoreException e) {
            log.add("exception", "KeystoreException " + e.getMessage());
        } catch (JSONException e) {
            log.add("exception", "JSONException (build message) " + e.getMessage());
        } catch (InvalidDeviceTokenFormatException ef){
            if(token.size() == 1){
                log.add("invalidTokenLen", "number: 0" + noti.receiverNumber  + ", InvalidDeviceTokenFormatException " +   ef.getMessage());
            }
        } catch (Exception e) {
            log.add("exception", e.getMessage());
        }

        log.writeLog();
    }

    public static class BatchObj {
        public Notification noti;
        public ArrayList<String> listTokenGCM;
        public ArrayList<String> listTokenAPN;

        public BatchObj() {
            noti = null;
            listTokenAPN = new ArrayList<>();
            listTokenGCM = new ArrayList<>();
        }
    }
}
