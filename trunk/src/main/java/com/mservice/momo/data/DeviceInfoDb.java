package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 2/18/14
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeviceInfoDb {

    EventBus eventBus;

    public DeviceInfoDb(EventBus eb) {
        eventBus = eb;
    }

    public void saveDeviceInfo(int number
            , String deviceName
            , String deviceVersion
            , String deviceModel
            , String deviceManufacturer
            , String appVersion
            , String deviceWidth
            , String deviceHeigth
            , String devicePrimaryEmail
            , String operatingSystem
            , String token
            , final Handler<Boolean> callback) {

        //newOBJ
        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.DeviceInfoDBCols.NUMBER, number);
        objNew.putString(colName.DeviceInfoDBCols.DEVICE_NAME, deviceName);
        objNew.putString(colName.DeviceInfoDBCols.DEVICE_VERSION, deviceVersion);
        objNew.putString(colName.DeviceInfoDBCols.DEVICE_MODEL, deviceModel);
        objNew.putString(colName.DeviceInfoDBCols.DEVICE_MANUFACTURER, deviceManufacturer);
        objNew.putString(colName.DeviceInfoDBCols.APP_VERSION, appVersion);
        objNew.putString(colName.DeviceInfoDBCols.DEVICE_WIDTH, deviceWidth);
        objNew.putString(colName.DeviceInfoDBCols.DEVICE_HEIGTH, deviceHeigth);
        objNew.putString(colName.DeviceInfoDBCols.DEVICE_PRIMARY_EMAIL, devicePrimaryEmail);
        objNew.putString(colName.DeviceInfoDBCols.OPERATING_SYSTEM, operatingSystem);
        objNew.putString(colName.DeviceInfoDBCols.TOKEN, token);
        objNew.putNumber(colName.DeviceInfoDBCols.CREATE_TIME, System.currentTimeMillis());

        saveOrUpdateDeviceInfo(number, objNew, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                callback.handle(aBoolean);
            }
        });
    }

    public static class DeviceInforObj {
        public int number;
        public String deviceName;
        public String deviceVersion;
        public String deviceModel;  // luu trong access history
        public String deviceManufacturer;
        public String appVersion;
        public String deviceSWidth;
        public String deviceSHeight;
        public String devicePrimaryEmail;
        public String operatingSystem;
        public String token;
        public long createTime;


        public DeviceInforObj(JsonObject json) {
            number = json.getInteger(colName.DeviceInfoDBCols.NUMBER, 0);
            deviceName = json.getString(colName.DeviceInfoDBCols.DEVICE_NAME, "");
            deviceVersion = json.getString(colName.DeviceInfoDBCols.DEVICE_VERSION, "");
            deviceModel = json.getString(colName.DeviceInfoDBCols.DEVICE_MODEL, "");
            deviceManufacturer = json.getString(colName.DeviceInfoDBCols.DEVICE_MANUFACTURER, "");
            appVersion = json.getString(colName.DeviceInfoDBCols.APP_VERSION, "");
            deviceSWidth = json.getString(colName.DeviceInfoDBCols.DEVICE_WIDTH, "");
            deviceSHeight = json.getString(colName.DeviceInfoDBCols.DEVICE_HEIGTH, "");
            devicePrimaryEmail = json.getString(colName.DeviceInfoDBCols.DEVICE_PRIMARY_EMAIL, "");
            operatingSystem = json.getString(colName.DeviceInfoDBCols.OPERATING_SYSTEM, "");
            token = json.getString(colName.DeviceInfoDBCols.TOKEN, "");
            createTime = json.getLong(colName.DeviceInfoDBCols.CREATE_TIME, 0);

        }
    }

    public void getOneLastTime(final int number, final Handler<JsonObject> callback) {

        if(number <=0){
            callback.handle(null);
            return;
        }

        /*"action": "findone",
                "collection": <collection>,
                "matcher": <matcher>,
                "keys": <keys>*/


        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.DeviceInfoDBCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.DeviceInfoDBCols.CREATE_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match

        JsonObject match = new JsonObject();
        match.putNumber(colName.DeviceInfoDBCols.NUMBER, number);

        //not equals
        JsonObject ne = new JsonObject();
        ne.putString(MongoKeyWords.NOT_EQUAL, "");

        match.putObject(colName.DeviceInfoDBCols.TOKEN,ne);

        query.putObject(MongoKeyWords.MATCHER, match);

        //fields

        JsonObject fields = new JsonObject();
        fields.putNumber(colName.DeviceInfoDBCols.NUMBER, 1);
        fields.putNumber(colName.DeviceInfoDBCols.OPERATING_SYSTEM, 1);
        fields.putNumber(colName.DeviceInfoDBCols.TOKEN, 1);
        fields.putNumber("_id", 0);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jo) {
                JsonArray results = jo.body().getArray(MongoKeyWords.RESULT_ARRAY, null);

                JsonObject result = null;
                JsonObject jreturn = null;

                if(results != null && results.size() > 0){
                    result = results.get(0);

                    jreturn = new JsonObject();
                    jreturn.putNumber(colName.PhoneDBCols.NUMBER,number);
                    jreturn.putString(colName.PhoneDBCols.PHONE_OS,result.getString(colName.DeviceInfoDBCols.OPERATING_SYSTEM,""));
                    jreturn.putString(colName.PhoneDBCols.PUSH_TOKEN,result.getString(colName.DeviceInfoDBCols.TOKEN,""));

                }

                callback.handle(jreturn);
            }
        });
    }

    //maybe, used later
    public void getDeviceInfo(int number, int pageNum, int pageSize, final Handler callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.DeviceInfoDBCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match
        if (number != 0) {
            JsonObject match = new JsonObject();
            match.putNumber(colName.DeviceInfoDBCols.NUMBER, number);
            query.putObject(MongoKeyWords.MATCHER, match);
        }

        int skip = (pageNum - 1) * pageSize;

        query.putNumber(MongoKeyWords.SKIP, skip);
        query.putNumber(MongoKeyWords.LIMIT, pageSize);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jo) {
                JsonObject obj = jo.body();

                ArrayList<DeviceInforObj> arAH = new ArrayList<>();

                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = jo.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if (results != null && results.size() > 0) {

                        for (int i = 0; i < results.size(); i++) {
                            arAH.add(new DeviceInforObj((JsonObject) results.get(i)));
                        }
                    } else {
                        callback.handle(null);
                        return;
                    }

                    callback.handle(arAH);
                } else {

                }
            }
        });

    }

    public void getOS(final int number, final Handler<String> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.DeviceInfoDBCols.TABLE);

        //sort desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.DeviceInfoDBCols.CREATE_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match
        JsonObject match = new JsonObject();
        match.putNumber(colName.DeviceInfoDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //limit
        query.putNumber(MongoKeyWords.LIMIT, 1);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jo) {
                JsonObject obj = jo.body();
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = jo.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if (results != null && results.size() > 0) {
                        JsonObject o = results.get(0);
                        callback.handle(o.getString(colName.DeviceInfoDBCols.OPERATING_SYSTEM, ""));
                    } else {
                        callback.handle("");
                    }

                } else {
                    callback.handle("");
                }
            }
        });
    }


    private void saveOrUpdateDeviceInfo(final int number, JsonObject newJsonObj, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.DeviceInfoDBCols.TABLE);
        JsonObject match = new JsonObject();

        match.putNumber(colName.DeviceInfoDBCols.NUMBER, number);
        match.putString(colName.DeviceInfoDBCols.DEVICE_MODEL, newJsonObj.getString(colName.DeviceInfoDBCols.DEVICE_MODEL));
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject setCtnr = new JsonObject();
        setCtnr.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, setCtnr);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }
}
