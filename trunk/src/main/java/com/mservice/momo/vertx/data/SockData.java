package com.mservice.momo.vertx.data;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.UserSettingDb;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.UserSetting;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by User on 3/18/14.
 */
public class SockData {
    public String sockId = "";
    public boolean hello = false;
    public String ip = "";
    public String pin = "";
    public boolean isOnline = false;
    public long start_access_time=0;
    public String deviceModel="";
    public String imei ="";
    public boolean isSetup = false;
    public String sessionKey="";
    //bank-net.start
    public String bank_net_bank_code ="";
    public String card_holder_name ="";
    public String card_holder_number ="";
    public String card_holder_year="";
    public String card_holder_month ="";
    public String bank_net_bank_name ="";
    public int inviteTryCount = 0;
    public long inviteLockTill = 0;
    public String lastCinemaInvoiceNo = "";
    public String appVersion = "";
    public int appCode = 0;
    public long lastCmdInd = 0;
    public String Cinema ="";
    public long timer = 0;
    public boolean isNewMapUpdate = false;
    public String bank_code = "";
    public String bank_name = "";
    public long bank_tid = 0;
    public long bank_amount = 0;
    public UserSetting userSetting;
    public UserSettingDb userSettingDb;
    public HashMap<String, JsonObject> mPendingNotification = new HashMap<String, JsonObject>();
    public BroadcastHandler broadcastHandler = null;

    //bank-net.end
    public String pushToken = "";
    public String os = "";
    public AtomicInteger registerCount = new AtomicInteger(0);
    private int number = 0;
    private Vertx mVertx;
    private Common mCommon;
    private PhonesDb.Obj mPhoneObj = null;

    public SockData(Vertx vertx, Logger logger, JsonObject glbCfg) {
        mVertx = vertx;
        mCommon = new Common(vertx, logger, glbCfg);
        userSettingDb = new UserSettingDb(vertx, logger);

    }

    public void setPhoneObj(PhonesDb.Obj obj,Logger log, String ref){

        mPhoneObj = obj;
        pin =obj.pin;
        isSetup =obj.isSetup;
        appCode = obj.appCode;
        appVersion = obj.appVer;
        lastCmdInd = obj.lastCmdInd;
    }

    public PhonesDb.Obj getPhoneObj(){
        return mPhoneObj;
    }

    public void addPendingPacket(String notificationId, JsonObject message){
        mPendingNotification.put(notificationId, message);
    }

    public void removePendingPacket(String notificationId){
        mPendingNotification.remove(notificationId);
    }

    public void beginSession(final int _number, final NetSocket socket, EventBus eventBus,final Logger log, JsonObject glbConfig){

        number = _number;
        mPendingNotification.clear();

        registerBroadcast(eventBus, socket, log, glbConfig);

        log.debug("BEGIN NEW CONNECTION FOR <" + _number + ">");
        BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
        helper.setSenderNumber(_number);
        helper.setReceivers(socket.writeHandlerID());
        helper.setType(SoapProto.Broadcast.MsgType.CHECK_PREV_VALUE);

        log.debug("SEND CHECK_PREV_VALUE <" + _number + ">");
        eventBus.publish(Misc.getNumberBus(_number),helper.getJsonObject());
    }

    public void closeData( EventBus eventBus,Logger log){
        log.debug("close data for number <" + number + ">");
        //All remain notification has been called from the timer
        if(mPendingNotification.size() > 0){
            for(JsonObject obj : mPendingNotification.values()){
                //todo Notify sending packet fail.
                log.debug("send failed notification back to NotificationVerticle <" + number + ">");
                BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
                helper.setNewPhone(number);
                helper.setExtra(obj);
//                eventBus.publish(AppConstant.NotificationVerticle_ADDRESS_SEND_PACKET_FAIL, helper.getJsonObject());
                eventBus.publish(AppConstant.HTTP_POST_BUS_ADDRESS,
                        Misc.makeHttpPostWrapperData(AppConstant.NotificationVerticle_ADDRESS_SEND_PACKET_FAIL, helper.getJsonObject()));
            }
        }
        mPendingNotification.clear();
        unregisterBroadcast(log);
        sockId = "";
        number = 0;
        hello = false;
        ip = "";
        isOnline = false;
        mPhoneObj = null;
    }

    public int getNumber(){
        return number;
    }
    public void setNumber(int number){this.number =number;}

    private void registerBroadcast(EventBus eventBus, NetSocket socket, Logger log, JsonObject glbConfig) {
        if (broadcastHandler != null) {
            if (broadcastHandler.getmNumber() != number) {
                unregisterBroadcast(log);
            } else {
                log.debug("broadcastHandler has a one.");
                return;
            }

        }
        broadcastHandler = new BroadcastHandler(number, socket, new PhonesDb(eventBus, log), log, this, mVertx, glbConfig);
        //log.info("register broadcast channel for number " + number + " - id " + sockId + " count " + registerCount.incrementAndGet());
        
        if (registerCount.get() > 1) {
            log.error("registerCount " + registerCount.get());
        }

        eventBus.registerHandler(ServerVerticle.MOMO_BROADCAST, broadcastHandler);
        eventBus.registerHandler(Misc.getNumberBus(number), broadcastHandler);
    }

    private void unregisterBroadcast(Logger log){
        if(broadcastHandler != null){
            broadcastHandler.unregister();
            broadcastHandler = null;
        }else{
            //log.info("unregister publish channel for number with broadcastChannelRegistered null");
        }
    }


    public void getUserSetting(final Handler<UserSetting> callback) {
        if (userSetting != null) {
            callback.handle(userSetting);
            return;
        }
        UserSetting filter = new UserSetting();
        filter.setModelId("0" + number);

        userSettingDb.findOne(filter, new Handler<UserSetting>() {
            @Override
            public void handle(UserSetting result) {
                if (result == null) {
                    userSetting = UserSetting.getDefaultSetting();
                } else {
                    userSetting = result;
                }
                callback.handle(userSetting);
            }
        });
    }



    public void fromJson(JsonObject jo) {
        sockId = jo.getString("sockId","");
        number = jo.getInteger("number",0);
        hello = jo.getBoolean("hello",false);
        ip = jo.getString("ip","");
        pin = jo.getString("pin","");
        isOnline = jo.getBoolean("isOnline",false);
        start_access_time = jo.getLong("start_access_time",0);
        deviceModel = jo.getString("deviceModel","");
        imei = jo.getString("imei","");
        isSetup = jo.getBoolean("isSetup",false);
        sessionKey = jo.getString("sessionKey","");
        bank_net_bank_code = jo.getString("bank_net_bank_code","");
        card_holder_name = jo.getString("card_holder_name","");
        card_holder_number = jo.getString("card_holder_number","");
        card_holder_year = jo.getString("card_holder_year","");
        card_holder_month = jo.getString("card_holder_month","");
        bank_net_bank_name = jo.getString("bank_net_bank_name","");
        inviteTryCount = jo.getInteger("inviteTryCount",0);
        inviteLockTill = jo.getLong("inviteLockTill",0);
        lastCinemaInvoiceNo = jo.getString("lastCinemaInvoiceNo","");
        appVersion = jo.getString("appVersion","");
        appCode = jo.getInteger("appCode",0);
        lastCmdInd = jo.getLong("lastCmdInd",0);
        Cinema = jo.getString("Cinema","");
        timer = jo.getLong("timer",0);
        bank_code = jo.getString("bank_code","");
        bank_tid = jo.getLong("bank_tid",0);
        bank_amount = jo.getLong("bank_amount",0);
        pushToken = jo.getString("pushToken","");
        os = jo.getString("os","");
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString("sockId",sockId);
        jo.putNumber("number",number);
        jo.putBoolean("hello",hello);
        jo.putString("ip",ip);
        jo.putString("pin",pin);
        jo.putBoolean("isOnline",isOnline);
        jo.putNumber("start_access_time",start_access_time);
        jo.putString("deviceModel",deviceModel);
        jo.putString("imei",imei);
        jo.putBoolean("isSetup",isSetup);
        jo.putString("sessionKey",sessionKey);
        jo.putString("bank_net_bank_code",bank_net_bank_code);
        jo.putString("card_holder_name",card_holder_name);
        jo.putString("card_holder_number",card_holder_number);
        jo.putString("card_holder_year",card_holder_year);
        jo.putString("card_holder_month",card_holder_month);
        jo.putString("bank_net_bank_name",bank_net_bank_name);
        jo.putNumber("inviteTryCount",inviteTryCount);
        jo.putNumber("inviteLockTill",inviteLockTill);
        jo.putString("lastCinemaInvoiceNo",lastCinemaInvoiceNo);
        jo.putString("appVersion",appVersion);
        jo.putNumber("appCode",appCode);
        jo.putNumber("lastCmdInd",lastCmdInd);
        jo.putString("Cinema",Cinema);
        jo.putNumber("timer",timer);
        jo.putString("bank_code",bank_code);
        jo.putNumber("bank_tid",bank_tid);
        jo.putNumber("bank_amount",bank_amount);
        jo.putString("pushToken",pushToken);
        jo.putString("os",os);
        return jo;
    }
}
