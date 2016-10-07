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
 * Time: 6:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImeisDb {
    /*
        imei : {
            imei     : 123456789
            imei_key : 123456789
            number   : 123456789
        }
    */

    public static final class Obj{
        public String imei;
        public String imei_key;
        public int number;
        public long createTime;
        public String operatingSystem;

        public Obj(JsonObject json){
            imei = json.getString(colName.ImeiDBCols.IMEI,"");
            imei_key = json.getString(colName.ImeiDBCols.IMEI_KEY,"");
            number = json.getInteger(colName.ImeiDBCols.NUMBER, 0);
            createTime =json.getLong(colName.ImeiDBCols.CREATE_TIME,0);
            operatingSystem =json.getString(colName.ImeiDBCols.OPERATING_SYSTEM,"");
        }
    }

    EventBus eventBus;
    public ImeisDb(EventBus eb){
        eventBus = eb;
    }

    public void findImeiObj(String imei, final Handler<ImeisDb.Obj> callback){
        findImei(imei, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                Obj result = null;
                if (jsonObject != null) {
                    result = new Obj(jsonObject);
                }
                callback.handle(result);
            }
        });
    }

    public void findImei(String imei, final Handler<JsonObject> callback){
        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION,MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.ImeiDBCols.TABLE);
        JsonObject match   = new JsonObject();
        match.putString(colName.ImeiDBCols.IMEI, imei);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                JsonObject result = json.getObject(MongoKeyWords.RESULT, null);
                callback.handle(result);
            }
        });
    }
    public void findImeiOld(String imei, final Handler<JsonObject> callback){
        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION,MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ImeiDBCols.TABLE);
        JsonObject match   = new JsonObject();
        match.putString(colName.ImeiDBCols.IMEI, imei);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                JsonObject result = null;
                if(json.getString(MongoKeyWords.STATUS).equals("ok") && json.getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray()).size() > 0){
                    result = json.getArray(MongoKeyWords.RESULT_ARRAY).get(0);
                }
                callback.handle(result);
            }
        });
    }

    public void upsertImei(String imei,String imeiKey,int number,String os, final Handler<Boolean> callback){

        //for query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION,MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ImeiDBCols.TABLE);

        //for where
        JsonObject match   = new JsonObject();
        match.putString(colName.ImeiDBCols.IMEI,imei);
        match.putNumber(colName.ImeiDBCols.NUMBER,number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set fields
        JsonObject objSet = new JsonObject();
        objSet.putNumber(colName.ImeiDBCols.NUMBER, number);
        objSet.putString(colName.ImeiDBCols.IMEI, imei);
        objSet.putString(colName.ImeiDBCols.IMEI_KEY, imeiKey);
        objSet.putString(colName.ImeiDBCols.OPERATING_SYSTEM, os);
        objSet.putNumber(colName.ImeiDBCols.CREATE_TIME, System.currentTimeMillis());

        JsonObject objNew = new JsonObject();
        objNew.putObject(MongoKeyWords.SET_$,objSet);

        query.putObject(MongoKeyWords.OBJ_NEW,objNew);

        query.putBoolean(MongoKeyWords.UPSERT,true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject obj = message.body();
                if (obj != null && obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    //maybe, will be used later
    public void getDiffNumbers(final int number, final String imei,final Handler<ArrayList<Integer>> cb){
        if(imei.equalsIgnoreCase("")){
            cb.handle(null);
            return;
        }
        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION,MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ImeiDBCols.TABLE);
        JsonObject match   = new JsonObject();
        match.putString(colName.ImeiDBCols.IMEI, imei);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                JsonArray arr = null;
                if(json.getString(MongoKeyWords.STATUS).equals("ok")
                        && json.getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray()).size() > 0){

                    arr = json.getArray(MongoKeyWords.RESULT_ARRAY);
                }
                if(arr == null){
                    cb.handle(null);
                    return;
                }
                ArrayList<Integer> arrayList = new ArrayList<>();
                for(Object o : arr){
                    JsonObject jo = (JsonObject)o;
                    if(jo.getInteger(colName.ImeiDBCols.NUMBER,0) != number){
                        arrayList.add(jo.getInteger(colName.ImeiDBCols.NUMBER));
                    }
                }

                if(arr.size() == 0){
                    cb.handle(null);
                    return;
                }

                cb.handle(arrayList);
            }
        });
    }
}
