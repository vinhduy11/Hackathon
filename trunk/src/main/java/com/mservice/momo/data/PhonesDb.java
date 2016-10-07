package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 2/18/14
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class PhonesDb {

    public static int PHONE_MIN = 800000000;
    public static int PHONE_MAX = 1999999999;
    public static int LOGIN_MAX_COUNT =9;
    Vertx vertx;
    EventBus eventBus;
    Logger logger;

    public PhonesDb(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    public static boolean checkNumber(int number) {
        return (PHONE_MIN <= number && number <= PHONE_MAX);
    }

    public void getOtp(final int number, final Handler<String> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match
        if (number != 0) {
            JsonObject match = new JsonObject();
            match.putNumber(colName.PhoneDBCols.NUMBER, number);
            query.putObject(MongoKeyWords.MATCHER, match);
        }

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<Obj> arrayList = null;

                if (results != null && results.size() > 0) {

                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }
                String result = "";

                if (arrayList!=null && arrayList.size()>0){
                    result = arrayList.get(0).otp;
                }

                // return default value
                callback.handle(result);
            }
        });

    }

    public void getPage(final int groupNumber, final ArrayList<Integer> listPhone, final int fromIndex
            , final int pageSize
            , final String phoneOs
            , final Handler<ArrayList<Obj>> callback) {
        getPage(groupNumber, listPhone, fromIndex, pageSize, phoneOs, null, new Handler<ArrayList<Obj>>() {
            @Override
            public void handle(ArrayList<Obj> event) {
                callback.handle(event);
            }
        });
    }

    public void getPage(int groupNumber, final ArrayList<Integer> listPhone, final int fromIndex
            , final int pageSize
            , final String phoneOs
            , final String bankCode
            , final Handler<ArrayList<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        query.putNumber(MongoKeyWords.BATCH_SIZE,pageSize);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("number", 1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject exists = new JsonObject();
        exists.putBoolean("$exists",true).putString(MongoKeyWords.NOT_EQUAL,"");

        JsonObject match = new JsonObject();
//      24/08/2016 GET ALL PHONES, DONT EXCEPT ANYTHING
//        match.putObject(colName.PhoneDBCols.PUSH_TOKEN,exists);
//        match.putObject(colName.PhoneDBCols.PHONE_OS,exists);
        if(groupNumber == 8888 || groupNumber == 7777)
        {
            boolean isAgent = groupNumber == 8888 ? true : false;
            match.putBoolean(colName.PhoneDBCols.IS_AGENT,isAgent);
        }
//        24/08/2016 GET ALL PHONES, DONT EXCEPT ANYTHING
//        if(bankCode != null && !"".equalsIgnoreCase(bankCode)){
//            match.putString(colName.PhoneDBCols.BANK_CODE, bankCode);
//        }

        if(phoneOs!=null && !"".equalsIgnoreCase(phoneOs)){
            //ANDROID ; iOS
            match.putString(colName.PhoneDBCols.PHONE_OS, phoneOs);
        }

        match.putBoolean(colName.PhoneDBCols.DELETED,false);
//        24/08/2016 GET ALL PHONES, DONT EXCEPT ANYTHING
//        match.putObject("number", new JsonObject()
//                        .putNumber("$gt", fromIndex)
//        );
//        query.putObject(MongoKeyWords.MATCHER,match);


        if(listPhone != null && listPhone.size() > 0){
            JsonArray array = new JsonArray();
            for(Integer i : listPhone){
                array.add(i);
            }

            JsonObject in = new JsonObject();
            in.putArray(MongoKeyWords.IN_$, array);
            match.putObject(colName.PhoneDBCols.NUMBER, in);
        }

        query.putObject(MongoKeyWords.MATCHER, match);
        query.putNumber(MongoKeyWords.LIMIT, pageSize);

        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 0);
        fields.putNumber(colName.PhoneDBCols.NUMBER, 1);
        fields.putNumber(colName.PhoneDBCols.PUSH_TOKEN, 1);
        fields.putNumber(colName.PhoneDBCols.PHONE_OS, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<Obj> arrayList = null;

                if (results != null && results.size() > 0) {

                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getByNumList(final ArrayList<Integer> idList
            , final Handler<ArrayList<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        JsonObject match = new JsonObject();

        if(idList != null && idList.size() > 0){
            JsonArray array = new JsonArray();
            for(Integer i : idList){
                array.add(i);
            }

            JsonObject in = new JsonObject();
            in.putArray(MongoKeyWords.IN_$, array);
            match.putObject(colName.PhoneDBCols.NUMBER, in);
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<Obj> arrayList = null;

                if (results != null && results.size() > 0) {

                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }


    public void getPhonesByMatcher(JsonObject matcher
            , JsonObject fields
            , final Handler<ArrayList<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putObject(MongoKeyWords.KEYS, fields);
        query.putNumber(MongoKeyWords.BATCH_SIZE ,10000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<Obj> arrayList = null;

                if (results != null && results.size() > 0) {

                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }


    public void getPageIsInviter(final ArrayList<Integer> listPhone, boolean isInviter , final int fromIndex
            , final int pageSize
            , final String phoneOs
            , final Handler<ArrayList<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        query.putNumber(MongoKeyWords.BATCH_SIZE,pageSize);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("number", 1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject exists = new JsonObject();
        exists.putBoolean("$exists",true).putString(MongoKeyWords.NOT_EQUAL,"");

        JsonObject match = new JsonObject();

        match.putObject(colName.PhoneDBCols.PUSH_TOKEN,exists);
        match.putObject(colName.PhoneDBCols.PHONE_OS,exists);

        if(phoneOs!=null && !"".equalsIgnoreCase(phoneOs)){
            //ANDROID ; iOS
            match.putString(colName.PhoneDBCols.PHONE_OS, phoneOs);
        }

        match.putBoolean(colName.PhoneDBCols.DELETED,false);
        match.putBoolean(colName.PhoneDBCols.IS_INVITER,isInviter);

        query.putObject(MongoKeyWords.MATCHER,match);
        match.putObject("number", new JsonObject()
                        .putNumber("$gt", fromIndex)
        );

        if(listPhone != null && listPhone.size() > 0){
            JsonArray array = new JsonArray();
            for(Integer i : listPhone){
                array.add(i);
            }

            JsonObject in = new JsonObject();
            in.putArray(MongoKeyWords.IN_$, array);
            match.putObject(colName.PhoneDBCols.NUMBER, in);
        }

        query.putObject(MongoKeyWords.MATCHER, match);
        query.putNumber(MongoKeyWords.LIMIT, pageSize);

        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 0);
        fields.putNumber(colName.PhoneDBCols.NUMBER, 1);
        fields.putNumber(colName.PhoneDBCols.PUSH_TOKEN, 1);
        fields.putNumber(colName.PhoneDBCols.PHONE_OS, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<Obj> arrayList = null;

                if (results != null && results.size() > 0) {

                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getInviteeSuccess(final int number, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.PhoneDBCols.REFERENCE_NUMBER, number);

        JsonObject gte = new JsonObject();
        gte.putNumber(MongoKeyWords.GREATER_OR_EQUAL, 1);
        match.putObject(colName.PhoneDBCols.INVITEE_COUNT, gte);

        query.putObject(MongoKeyWords.MATCHER, match);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 1000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<Obj> arrayList = null;

                if (results != null && results.size() > 0) {

                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

//tool delete xoa so dien thoai
    public void removePhoneObj(int number, final  Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void getPhoneInfo(int number,final Handler<Message<JsonObject>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        /*{
            "action": "findone",
                "collection": "items",
                "matcher": {
            "_id": "ffeef2a7-5658-4905-a37c-cfb19f70471d"
        }
        */

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        match.putBoolean(colName.PhoneDBCols.DELETED, false);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, callback);
    }

    //hungtt we want to update then select with only one round

    public void updatePin(final int number,final String mpin, final Handler<Boolean> callback){


        //updatePartial(number, objNew, callback);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject objNew = new JsonObject();
        String endcodedPin = DataUtil.encode(mpin);
        objNew.putString(colName.PhoneDBCols.PIN,endcodedPin );

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, objNew);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }



    public void updateSessionTime(final int number,final long curTime
                                    ,final Handler<Boolean> callback){


        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject jo = new JsonObject();
        jo.putNumber(colName.PhoneDBCols.LAST_TIME,curTime);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jo);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });

    }

    public void setOtp(final int number, final String otp, final Handler<Boolean> callback){

        //updatePartial(number, objNew, callback);
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.PhoneDBCols.NUMBER, number);
        objNew.putNumber(colName.PhoneDBCols.OTP_TIME, System.currentTimeMillis());
        objNew.putString(colName.PhoneDBCols.OTP, otp);
        objNew.putBoolean(colName.PhoneDBCols.DELETED,false);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, objNew);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                String okResult = jsonObjectMessage.body().getString(MongoKeyWords.STATUS, "");
                boolean result = false;
                if("ok".equalsIgnoreCase(okResult)){
                    result = true;
                }
                callback.handle(result);

            }
        });
    }

    public void setToken(final int number, final String token, final String os, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject jo = new JsonObject();
        jo.putString(colName.PhoneDBCols.PUSH_TOKEN, token);
        jo.putString(colName.PhoneDBCols.PHONE_OS,os);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jo);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                boolean result = jsonObjectMessage.body().getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);

            }
        });
    }

    public void updatePartialNoReturnObj(final int number,JsonObject jo, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jo);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }

    public void update(final int number,JsonObject jo, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jo);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    public void updatePartial(final int number, JsonObject newJsonObj, final Handler<Obj> callback){
        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);

        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {

                if(callback == null) return;

                JsonObject json  = jsonObjectMessage.body();
                //
                if(json!= null && json.getObject(MongoKeyWords.RESULT, null) != null){
                    logger.info("get Obj ---------------->" + json.getObject(MongoKeyWords.RESULT));
                    Obj result = new Obj(json.getObject(MongoKeyWords.RESULT));
                    logger.info("get Obj result ---------------->" + result);
                    callback.handle(result);
                }
                else{
                    callback.handle(null);
                }
            }
        });
    }

    public void expireSession(final int number,final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject jo = new JsonObject();
        jo.putString(colName.PhoneDBCols.SESSION_KEY, "");

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jo);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                boolean result = jsonObjectMessage.body().getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);

            }
        });
    }




    public void getAllPhone(final Handler<ArrayList<Integer>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        match.putBoolean(colName.PhoneDBCols.DELETED,false);
        query.putObject(MongoKeyWords.MATCHER,match);

        //sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT,sort);

        //fields to get out
        //{ field1: <boolean>, field2: <boolean> ... }
        JsonObject fields = new JsonObject();
        fields.putNumber(colName.PhoneDBCols.NUMBER, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                ArrayList<Integer> arrayList = new ArrayList<Integer>();
                for(int i = 0;i< array.size();i++){
                    arrayList.add(((JsonObject)array.get(i)).getInteger(colName.PhoneDBCols.NUMBER));
                }

                callback.handle(arrayList);
            }
        });
    }

    public void getAllPhoneDataFromTool(int type, final Handler<ArrayList<Integer>> callback){
        //type = 0 => get All, type = 1 => get EU, type = 2 => get DGD
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        switch (type)
        {
            case 1:
                match.putBoolean(colName.PhoneDBCols.IS_AGENT, false);
                break;
            case 2:
                match.putBoolean(colName.PhoneDBCols.IS_AGENT, true);
                break;
            default:
                break;
        }
        match.putBoolean(colName.PhoneDBCols.DELETED,false);
        query.putObject(MongoKeyWords.MATCHER,match);

        //sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT,sort);

        //fields to get out
        //{ field1: <boolean>, field2: <boolean> ... }
        JsonObject fields = new JsonObject();
        fields.putNumber(colName.PhoneDBCols.NUMBER, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                ArrayList<Integer> arrayList = new ArrayList<Integer>();
                for(int i = 0;i< array.size();i++){
                    arrayList.add(((JsonObject)array.get(i)).getInteger(colName.PhoneDBCols.NUMBER));
                }

                callback.handle(arrayList);
            }
        });
    }
 /*   public static void main(String[] args) throws IOException {
        File file = new File("/Users/ios-001/IdeaProjects/svn/mservice/test.json");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");
        JsonArray a = new JsonArray(str);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("/Users/ios-001/IdeaProjects/svn/mservice/test.csv"));
        bufferedWriter.write("number\tname\tcardId\tmomo\tmload\tisReged\tisNamed\tisActived\tisSetup\tsessionKey\tlastImei\tlast_session_time\tpin\temail\tquestion\tanswer\tmpoint\tdateOfBirth\taddress\totp_time\totp\tbank_name\tbank_account\tbank_code\tbalance\tlogin_max_try_count\tlocked_till_time\tnew Date(createdDate)\treferenceNumber\tinviterCount\tinviteeCount\tnoNameTranCount\tdeleted\timeiKey\tphoneOs\tpushToken\tisInviter\tappVer\tappCode\tinviter\tlastCmdInd\tbankPersonalId\tnew Date(inviteTime)\twaitingReg\tisAgent\n");
        for(Object j : a) {
            JsonObject o = (JsonObject) j;
            Obj obj = new Obj(o);
            System.out.println(obj.toString());
            bufferedWriter.write(obj.toString() + "\n");
        }
        bufferedWriter.flush();
        bufferedWriter.close();
    }*/

    public void getPhoneHasReferal(long fromTime, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        match.putBoolean(colName.PhoneDBCols.DELETED,false);

        JsonObject exist = new JsonObject();
        exist.putBoolean("$exists", true);
        match.putObject(colName.PhoneDBCols.REFERENCE_NUMBER, exist);

        JsonObject gte = new JsonObject();
        gte.putNumber(MongoKeyWords.GREATER_OR_EQUAL,fromTime);
        match.putObject(colName.PhoneDBCols.CREATED_DATE, gte);

        query.putObject(MongoKeyWords.MATCHER,match);

         //fields to get out
        //{ field1: <boolean>, field2: <boolean> ... }
        JsonObject fields = new JsonObject();
        fields.putNumber(colName.PhoneDBCols.NUMBER, 1);
        fields.putNumber(colName.PhoneDBCols.REFERENCE_NUMBER, 1);
        fields.putNumber(colName.PhoneDBCols.CREATED_DATE, 1);

        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                ArrayList<Obj> arrayList = new ArrayList<>();
                for(int i = 0;i< array.size();i++){

                    PhonesDb.Obj p = new Obj((JsonObject)array.get(i));
                    //if(p.referenceNumber > 0){
                        arrayList.add(p);
                    //}
                }

                callback.handle(arrayList);
            }
        });

    }

    public void getPhoneList(ArrayList<Integer> listPhone, String phoneOs, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        if(phoneOs!=null && !"".equalsIgnoreCase(phoneOs)){
            //ANDROID ; iOS
            match.putString(colName.PhoneDBCols.PHONE_OS, phoneOs);
        }

        match.putBoolean(colName.PhoneDBCols.DELETED,false);
        query.putObject(MongoKeyWords.MATCHER,match);

        JsonArray array = new JsonArray();
        for(Integer i : listPhone){
            array.add(i);
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, array);
        match.putObject(colName.PhoneDBCols.NUMBER, in);

        //sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT,sort);

        //fields to get out
        //{ field1: <boolean>, field2: <boolean> ... }
        JsonObject fields = new JsonObject();

        fields.putNumber("_id",0);
        fields.putNumber(colName.PhoneDBCols.NUMBER, 1);
        fields.putNumber(colName.PhoneDBCols.PHONE_OS, 1);
        fields.putNumber(colName.PhoneDBCols.PUSH_TOKEN, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE,listPhone.size());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                ArrayList<Obj> arrayList = new ArrayList<>();
                for(int i = 0;i< array.size();i++){

                    Obj o = new Obj((JsonObject)array.get(i));
                    if(!"".equalsIgnoreCase(o.phoneOs) && !"".equalsIgnoreCase(o.pushToken)){
                        arrayList.add(o);
                    }
                }
                callback.handle(arrayList);
            }
        });

    }

    /**
     * Cong Nguyen
     *
     * @param listPhone
     * @param phoneOs
     * @param callback
     */
    public void getPhoneListFull(ArrayList<Integer> listPhone, String phoneOs, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        if(phoneOs!=null && !"".equalsIgnoreCase(phoneOs)){
            //ANDROID ; iOS
            match.putString(colName.PhoneDBCols.PHONE_OS, phoneOs);
        }

        match.putBoolean(colName.PhoneDBCols.DELETED,false);
        query.putObject(MongoKeyWords.MATCHER,match);

        JsonArray array = new JsonArray();
        for(Integer i : listPhone){
            array.add(i);
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, array);
        match.putObject(colName.PhoneDBCols.NUMBER, in);

        //sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT,sort);

        query.putNumber(MongoKeyWords.BATCH_SIZE,listPhone.size());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                ArrayList<Obj> arrayList = new ArrayList<>();
                for(int i = 0;i< array.size();i++){

                    Obj o = new Obj((JsonObject)array.get(i));
                    if(!"".equalsIgnoreCase(o.phoneOs) && !"".equalsIgnoreCase(o.pushToken)){
                        arrayList.add(o);
                    }
                }
                callback.handle(arrayList);
            }
        });

    }

    public void getPhoneObjInfo(final int number, final Handler<Obj> callback){
        logger.info("phone number " + number);
        getPhoneInfo(number,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                logger.info("phone number " + jsonObjectMessage);
                Obj obj = null;

                JsonObject jo = jsonObjectMessage.body().getObject(MongoKeyWords.RESULT,null);
                if(jo !=null){
                    obj = new Obj(jo);
                }

                callback.handle(obj);
            }
        });
    }

    //test.start

    public void getPhoneObjInfoLocal(final int number, final Handler<Obj> callback){
        getPhoneInfo(number,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {

                Obj obj = null;
                JsonObject jo = jsonObjectMessage.body().getObject(MongoKeyWords.RESULT, null);
                if(jo!=null){
                    obj = new Obj(jo);
                }

                callback.handle(obj);

            }
        });
    }

    public void updatePhone(final int number, JsonObject jo, final Handler<Boolean>callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jo);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
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

    public void updatePhoneWithOutUpsert(final int number, JsonObject jo, final Handler<Boolean>callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jo);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

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


    //test.end

    public void updateReferal(int number, String inviter, long inviteTime, final Handler<Integer> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match = new JsonObject();

        JsonObject joUp = new JsonObject();
        joUp.putNumber(colName.PhoneDBCols.INVITE_TIME,inviteTime);
        joUp.putString(colName.PhoneDBCols.INVITER, inviter);

        //matcher
        JsonObject notExist = new JsonObject();
        notExist.putBoolean("$exists",false);

        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        match.putObject(colName.PhoneDBCols.INVITE_TIME,notExist);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUp);
        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(obj.getInteger("number"));
                } else {
                    callback.handle(0);
                }
            }
        });

    }

    public void checkWhoIsMomoer(final List<Integer> numbers, final Handler<List<Integer>> callback){
        JsonArray jNumArr = new JsonArray();
        int    item_num = 0;

        for(int i = 0; i<numbers.size(); i++)
        {
            item_num = numbers.get(i);
            if(checkNumber(item_num)){
                jNumArr.addNumber(item_num);
            }
        }

        final ArrayList<Integer> phonesResult = new ArrayList<>();

        if(jNumArr.size() > 0){
            JsonObject query    = new JsonObject();
            JsonObject match   = new JsonObject();

            query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
            query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

            JsonObject in = new JsonObject();
            in.putArray(MongoKeyWords.IN_$, jNumArr);

            match.putObject(MongoKeyWords.NUMBER, in);
            query.putObject(MongoKeyWords.MATCHER,match);

            JsonObject keys = new JsonObject();
            keys.putNumber(colName.PhoneDBCols.NUMBER, 1);
            query.putObject(MongoKeyWords.KEYS,keys);


            eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {

                    //To change body of implemented methods use File | Settings | File Templates.
                    if(message.body().getString(MongoKeyWords.STATUS).equals("ok")){
                        JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                        if(results != null && results.size() > 0){
                            JsonObject item;
                            int num;
                            int i;

                            logger.debug("SIZE OF IS MOMO " + results.size());

                            for(i = 0; i<results.size();i++){
                                item = results.get(i);
                                num = item.getInteger(colName.PhoneDBCols.NUMBER, 0);
                                phonesResult.add(num);
                            }
                        }
                    }
                    callback.handle(phonesResult);
                }
            });
        }
        else {
            callback.handle(phonesResult);
        }
    }

    public void findAndModifyPhoneDb(int number, String email, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject objNew = new JsonObject();
        objNew.putString(colName.PhoneDBCols.EMAIL, email);


        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);

        //update
        query.putObject(MongoKeyWords.UPDATE, update);

        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyPhoneDb: " + query);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyPhoneDb " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joReferral = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyPhoneDb json is not null " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " +  joReferral);
                    if(joReferral != null)
                    {
                        Obj referralObj = new Obj(joReferral);
                        callback.handle(referralObj);
                    }
                    else {
                        callback.handle(null);
                    }
                }
                else {
                    callback.handle(null);
                }
            }
        });
    }

    public  void incMPoint(int number, long mpoint,final Handler callback){

        //new object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putNumber(colName.PhoneDBCols.MPOINT,mpoint);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);
        JsonObject match   = new JsonObject();

        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, set);
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

    public void getPhoneObjWithFields(final int number, JsonObject fields, final Handler<Obj> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);
        query.putObject(MongoKeyWords.KEYS,fields);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                JsonObject result = json.getObject(MongoKeyWords.RESULT, null);
                Obj obj = result == null ? null : new Obj(result);
                callback.handle(obj);
            }
        });


    }

    public void getTokenAndOs(final int number, final Handler<Obj> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);
    }

    //dem so lan login bi fail
    public void decrementAndGetLoginCnt(int number,long lockTill, final Handler<Integer> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.PhoneDBCols.NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, -1);


        JsonObject setField = new JsonObject();
        setField.putNumber(colName.PhoneDBCols.LOCKED_UNTIL, lockTill);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);
        udpObj.putObject(MongoKeyWords.SET_$,setField);


        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);

        //sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.PhoneDBCols.NUMBER, -1);
        query.putObject(MongoKeyWords.SORT,sort);

        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        JsonObject fields = new JsonObject();
        fields.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, 1);
        query.putObject("fields", fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                if(json!= null){
                    int remain_count = json.getObject(MongoKeyWords.RESULT).getInteger(colName.PhoneDBCols.MAX_LOGIN_COUNT);
                    remain_count = remain_count > 0 ? remain_count :0;
                    callback.handle(remain_count);
                }
                else {
                    callback.handle(0);
                }
            }
        });
    }



    public void getPhoneByMonth(String yyymmdd, String format ,final Handler<ArrayList<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        long dateofmonth = Misc.getDateAsLong(yyymmdd,format,logger,"");
        Misc.TimeOfMonth timeOfMonth = Misc.getBeginEndTimeOfMonth(dateofmonth);

        JsonObject gle = new JsonObject();
        gle.putNumber(MongoKeyWords.GREATER_OR_EQUAL, timeOfMonth.BeginTime)
            .putNumber(MongoKeyWords.LESS_OR_EQUAL, timeOfMonth.EndTime);

        JsonObject match = new JsonObject();
        match.putObject(colName.PhoneDBCols.CREATED_DATE, gle);

        query.putObject(MongoKeyWords.MATCHER, match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                // return default value

                ArrayList<Obj> arrayList = null;

                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if(array != null && array.size() >0){
                    arrayList = new ArrayList<Obj>();
                    for(int i =0; i < array.size();i++){
                        arrayList.add(new Obj( (JsonObject)array.get(i)));
                    }
                }

                callback.handle(arrayList);
            }
        });
    }
    //getCreatedate

    public void searchCreateDateByPhoneNumber(final String number, final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        JsonObject match = new JsonObject();
        if (number != null && !"".equalsIgnoreCase(number)) {
            match.putNumber(colName.PhoneDBCols.NUMBER, DataUtil.strToInt(number));
            query.putObject(MongoKeyWords.MATCHER, match);
        }

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void getPhoneInList(JsonArray jsonArray, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);


        JsonObject match = new JsonObject();
        JsonObject in = new JsonObject();
        JsonArray ar = new JsonArray();
        for(int i = 0;i< jsonArray.size();i++){
            ar.add(((JsonObject)jsonArray.get(i)).getInteger(colName.PhoneDBCols.NUMBER));
        }
        in.putArray(MongoKeyWords.IN_$,ar);

        match.putObject(colName.PhoneDBCols.REFERENCE_NUMBER, in);

        query.putObject(MongoKeyWords.MATCHER, match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                // return default value
                ArrayList<Obj> arrayList = null;
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                if(array!= null && array.size() > 0){
                    arrayList = new ArrayList<>();

                    for(int i=0;i<array.size();i++){
                        JsonObject jo = (JsonObject)array.get(i);
                        arrayList.add(new Obj(jo));
                    }
                }

                callback.handle(arrayList);
            }
        });
    }
    public void findByPhone(String phoneNumber, final Handler<PhonesDb.Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.PhoneDBCols.NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                PhonesDb.Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new PhonesDb.Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }

    public void getPhoneLessThanTime(long time, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PhoneDBCols.TABLE);

        JsonObject match = new JsonObject();

        JsonObject less = new JsonObject();
        less.putNumber(MongoKeyWords.LESS_OR_EQUAL, time);
        match.putObject(colName.PhoneDBCols.LAST_TIME, less);

        query.putObject(MongoKeyWords.MATCHER, match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                // return default value
                ArrayList<Obj> arrayList = null;
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                if(array!= null && array.size() > 0){
                    arrayList = new ArrayList<>();

                    for(int i=0;i<array.size();i++){
                        JsonObject jo = (JsonObject)array.get(i);
                        arrayList.add(new Obj(jo));
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    /**
     * Created with IntelliJ IDEA.
     * User: User
     * Date: 2/18/14
     * Time: 12:00 PM
     * To change this template use File | Settings | File Templates.
     */
    public static class Obj {
        public int number = 0;
        public String name = "";
        public String cardId = "";
        public long momo = 0;
        public long mload = 0;
        public boolean isReged = false;
        public boolean isNamed = false;
        public boolean isActived = false;
        public boolean isSetup = false;
        public String sessionKey = "";
        public String lastImei = "";
        public long last_session_time = 0;
        public String pin = "";
        public String email = "";
        public String question = "";
        public String answer = "";
        public long mpoint = 0;
        public String dateOfBirth = "";
        public String address = "";
        public long otp_time = 0;
        public String otp = "";

//        public String session_key = "";
        //for bank
        public String bank_name = "";
        public String bank_account = "";
        public String bank_code = "";
        public long balance = 0;

        //for max try count login
        public int login_max_try_count = 0;
        public long locked_till_time = 0;

        //promotion for inviter, invitee
        public long createdDate = 0;
        public int referenceNumber = 0;
        public int inviterCount = 0;
        public int inviteeCount = 0;

        //for no name account
        public int noNameTranCount = 0;
        public boolean deleted = false;

        //new
        public String imeiKey = "";
        public String phoneOs = "";
        public String pushToken = "";

        public boolean isInviter = false;
        public String appVer = "";
        public int appCode = 0;
        public String inviter = "";
        public long lastCmdInd = 0;
        public String bankPersonalId = "";
        public long inviteTime = 0;
        public boolean waitingReg = false;
        public boolean isAgent = false;
        public String deviceInfo = "";
        public int statusATMCard = 0;
        public String balanceTotal = "";
        public boolean isLockV1 = false;
        public Obj(JsonObject json) {
            number = json.getInteger(colName.PhoneDBCols.NUMBER, 0);
            name = json.getString(colName.PhoneDBCols.NAME, "");
            cardId = json.getString(colName.PhoneDBCols.CARD_ID, "");
            momo = json.getLong(colName.PhoneDBCols.MOMO, 0);
            mload = json.getLong(colName.PhoneDBCols.MLOAD, 0);
            isReged = json.getBoolean(colName.PhoneDBCols.IS_REGED, false);
            isNamed = json.getBoolean(colName.PhoneDBCols.IS_NAMED, false);
            isActived = json.getBoolean(colName.PhoneDBCols.IS_ACTIVED, false);
            sessionKey = json.getString(colName.PhoneDBCols.SESSION_KEY, "");
            lastImei = json.getString(colName.PhoneDBCols.LAST_IMEI, "");
            last_session_time = json.getLong(colName.PhoneDBCols.LAST_TIME, 0);
            pin = DataUtil.decode(json.getString(colName.PhoneDBCols.PIN, ""));
            email = json.getString(colName.PhoneDBCols.EMAIL, "");
            question = json.getString(colName.PhoneDBCols.QUESTION, "");
            answer = json.getString(colName.PhoneDBCols.ANSWER, "");

            mpoint = json.getLong(colName.PhoneDBCols.MPOINT, 0);
            dateOfBirth = json.getString(colName.PhoneDBCols.DATE_OF_BIRTH, "");
            address = json.getString(colName.PhoneDBCols.ADDRESS, "");
            otp_time = json.getLong(colName.PhoneDBCols.OTP_TIME, 0);
            otp = json.getString(colName.PhoneDBCols.OTP, "");

            //for bank
            bank_name = json.getString(colName.PhoneDBCols.BANK_NAME, "");
            bank_account = json.getString(colName.PhoneDBCols.BANK_ACCOUNT, "");
            bank_code = json.getString(colName.PhoneDBCols.BANK_CODE, "");
            login_max_try_count = json.getInteger(colName.PhoneDBCols.MAX_LOGIN_COUNT, 0);
            locked_till_time = json.getLong(colName.PhoneDBCols.LOCKED_UNTIL, 0);
            noNameTranCount = json.getInteger(colName.PhoneDBCols.NONAME_TRAN_COUNT, 0);
            deleted = json.getBoolean(colName.PhoneDBCols.DELETED, false);
            isSetup = json.getBoolean(colName.PhoneDBCols.IS_SETUP, false);

            imeiKey = json.getString(colName.PhoneDBCols.IMEI_KEY, "");
            phoneOs = json.getString(colName.PhoneDBCols.PHONE_OS, "");
            pushToken = json.getString(colName.PhoneDBCols.PUSH_TOKEN, "");

            //balance == so du momo
            balance = json.getLong(colName.PhoneDBCols.MOMO, 0);

            //promotion for reference
            referenceNumber = json.getInteger(colName.PhoneDBCols.REFERENCE_NUMBER, 0);
            createdDate = json.getLong(colName.PhoneDBCols.CREATED_DATE, 0L);

            //so lan tu minh duoc nhan
            inviterCount = json.getInteger(colName.PhoneDBCols.INVITER_COUNT, 0);

            //so lan duoc tang tu reference
            inviteeCount = json.getInteger(colName.PhoneDBCols.INVITEE_COUNT, 0);

            isInviter = json.getBoolean(colName.PhoneDBCols.IS_INVITER, false);

            appCode = json.getInteger(colName.PhoneDBCols.APPCODE, 0);
            appVer = json.getString(colName.PhoneDBCols.APPVER, "");
            inviter = json.getString(colName.PhoneDBCols.INVITER, "");
            lastCmdInd = json.getLong(colName.PhoneDBCols.LAST_CMD_IND, 0);
            bankPersonalId = json.getString(colName.PhoneDBCols.BANK_PERSONAL_ID, "");
            inviteTime = json.getLong(colName.PhoneDBCols.INVITE_TIME, 0);
            waitingReg = json.getBoolean(colName.PhoneDBCols.WAITING_REG,false);
            isAgent = json.getBoolean(colName.PhoneDBCols.IS_AGENT,false);

            deviceInfo = json.getString(colName.PhoneDBCols.DEVICE_INFO, "");
            statusATMCard = json.getInteger(colName.PhoneDBCols.STATUS_ATMCARD, 0);
            balanceTotal = json.getString(colName.PhoneDBCols.BALANCE_TOTAL, "");
            isLockV1 = json.getBoolean(colName.PhoneDBCols.IS_LOCKED_V1, false);
        }

        public Obj() {
        }

        public JsonObject toJsonObject() {

            JsonObject jo = new JsonObject();
            jo.putNumber(colName.PhoneDBCols.NUMBER, this.number);
            jo.putString(colName.PhoneDBCols.NAME, this.name);
            jo.putString(colName.PhoneDBCols.CARD_ID, this.cardId);
            jo.putNumber(colName.PhoneDBCols.MOMO, this.momo);
            jo.putNumber(colName.PhoneDBCols.MLOAD, this.mload);

            jo.putBoolean(colName.PhoneDBCols.IS_REGED, this.isReged);
            jo.putBoolean(colName.PhoneDBCols.IS_NAMED, this.isNamed);
            jo.putBoolean(colName.PhoneDBCols.IS_ACTIVED, this.isActived);
            jo.putBoolean(colName.PhoneDBCols.IS_SETUP, this.isSetup);
            //jo.putString(colName.PhoneDBCols.SESSION_KEY, this.session_key);
            jo.putString(colName.PhoneDBCols.LAST_IMEI, this.lastImei);
            jo.putNumber(colName.PhoneDBCols.LAST_TIME, this.last_session_time);
            jo.putString(colName.PhoneDBCols.PIN, DataUtil.encode(this.pin));
            jo.putString(colName.PhoneDBCols.EMAIL, this.email);
            jo.putString(colName.PhoneDBCols.QUESTION, this.question);
            jo.putString(colName.PhoneDBCols.ANSWER, this.answer);
            jo.putNumber(colName.PhoneDBCols.MPOINT, this.mpoint);
            jo.putString(colName.PhoneDBCols.DATE_OF_BIRTH, this.dateOfBirth);
            jo.putString(colName.PhoneDBCols.ADDRESS, this.address);
            jo.putNumber(colName.PhoneDBCols.OTP_TIME, this.otp_time);
            jo.putString(colName.PhoneDBCols.OTP, this.otp);

            jo.putString(colName.PhoneDBCols.SESSION_KEY, this.sessionKey);
            jo.putString(colName.PhoneDBCols.BANK_NAME, this.bank_name);
            jo.putString(colName.PhoneDBCols.BANK_ACCOUNT, this.bank_account);
            jo.putString(colName.PhoneDBCols.BANK_CODE, this.bank_code);
            jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, this.login_max_try_count);
            jo.putNumber(colName.PhoneDBCols.LOCKED_UNTIL, this.locked_till_time);
            jo.putBoolean(colName.PhoneDBCols.DELETED, this.deleted);
            jo.putString(colName.PhoneDBCols.IMEI_KEY, this.imeiKey);

            jo.putNumber(colName.PhoneDBCols.CREATED_DATE, this.createdDate);
            jo.putNumber(colName.PhoneDBCols.REFERENCE_NUMBER, this.referenceNumber);
            jo.putNumber(colName.PhoneDBCols.INVITER_COUNT, this.inviterCount);
            jo.putNumber(colName.PhoneDBCols.INVITEE_COUNT, this.inviteeCount);


            jo.putString(colName.PhoneDBCols.PUSH_TOKEN, this.pushToken);
            jo.putString(colName.PhoneDBCols.PHONE_OS, this.phoneOs);
            jo.putBoolean(colName.PhoneDBCols.IS_INVITER, isInviter);

            jo.putString(colName.PhoneDBCols.APPVER, this.appVer);
            jo.putNumber(colName.PhoneDBCols.APPCODE, this.appCode);
            jo.putString(colName.PhoneDBCols.INVITER, this.inviter);
            jo.putNumber(colName.PhoneDBCols.LAST_CMD_IND, this.lastCmdInd);
            jo.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, this.bankPersonalId);
            jo.putNumber(colName.PhoneDBCols.INVITE_TIME, this.inviteTime);
            jo.putBoolean(colName.PhoneDBCols.WAITING_REG, this.waitingReg);
            jo.putBoolean(colName.PhoneDBCols.IS_AGENT, this.isAgent);
            jo.putNumber(colName.PhoneDBCols.STATUS_ATMCARD, statusATMCard);
            jo.putString(colName.PhoneDBCols.DEVICE_INFO, this.deviceInfo);

            jo.putString(colName.PhoneDBCols.BALANCE_TOTAL, this.balanceTotal);
            jo.putBoolean(colName.PhoneDBCols.IS_LOCKED_V1, this.isLockV1);
            return jo;
        }

        @Override
        public String toString() {
            return number + "\t" +
                    name + "\t" +
                    cardId + "\t" +
                     momo + "\t" +
                     mload +"\t" +
                     isReged +"\t" +
                     isNamed +"\t" +
                     isActived +"\t" +
                     isSetup +"\t" +
                     sessionKey + "\t" +
                     lastImei + "\t" +
                     last_session_time +"\t" +
                     pin + "\t" +
                     email + "\t" +
                     question + "\t" +
                     answer + "\t" +
                     mpoint +"\t" +
                     dateOfBirth + "\t" +
                     address + "\t" +
                     otp_time +"\t" +
                     otp + "\t" +
                     bank_name + "\t" +
                     bank_account + "\t" +
                     bank_code + "\t" +
                     balance +"\t" +
                     login_max_try_count +"\t" +
                     locked_till_time +"\t" +
                     new Date(createdDate) +"\t" +
                     referenceNumber +"\t" +
                     inviterCount +"\t" +
                     inviteeCount +"\t" +
                     noNameTranCount +"\t" +
                     deleted +"\t" +
                     imeiKey + "\t" +
                     phoneOs + "\t" +
                     pushToken + "\t" +
                     isInviter +"\t" +
                     appVer + "\t" +
                     appCode +"\t" +
                     inviter + "\t" +
                     lastCmdInd +"\t" +
                     bankPersonalId + "\t" +
                     new Date(inviteTime) +"\t" +
                     waitingReg +"\t" +
                     isAgent +"\t" +
                    deviceInfo + "\t" + statusATMCard + "\t" + balanceTotal + "\t" + isLockV1;
        }
    }
}
