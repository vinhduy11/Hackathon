package com.mservice.momo.vertx.promotion_server;

import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 7/24/16.
 */
public interface PromotionInterface {


    //Check device info and insert into DB Device.
    public void checkDevice(String deviceInfo, String os, int appCode, String program, int number, String imei, boolean checkGmail, boolean checkSim, String className, Common.BuildLog log, Handler<JsonObject> callBack);

    //Check maximum money or gift that user can receive.
    public void checkCountPromotion(int maxCount, int phoneNumber, String program, String className, Common.BuildLog log, Handler<Boolean> callback);

    //Save error description.
    public void saveErrorPromotionDescription(String program, int number, String desc, int errorCode, String deviceInfo, Common.BuildLog log, String className);

    //call back result before verticle
    public void callBack(int error, String description, Notification notification, Common.BuildLog log, Message msg, String className, JsonObject joExtra);
}
