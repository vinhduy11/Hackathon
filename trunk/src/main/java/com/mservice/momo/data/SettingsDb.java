package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 5/12/14.
 */
public class SettingsDb {
    EventBus eventBus;
    Logger logger;

    public static final String TABLE_NAME = "MoMoSettings";

    public SettingsDb(EventBus eb, Logger log) {
        eventBus = eb;
        logger = log;
    }

    public void getLong(String key, final Handler<Long> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        JsonObject matcher   = new JsonObject();
        matcher.putString("KEY", key);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        JsonObject sort = new JsonObject();
        sort.putNumber("KEY",1);
        query.putObject(MongoKeyWords.SORT,sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                //
                if (json != null && json.getArray(MongoKeyWords.RESULT_ARRAY, null) != null) {
                    JsonArray result = json.getArray(MongoKeyWords.RESULT_ARRAY);
                    if (result.size() > 0) {
                        JsonObject obj = result.get(0);
                        callback.handle(obj.getLong("VAL"));
                    }
                    else {
                        callback.handle(0L);
                    }
                } else {
                    callback.handle(0L);
                }
            }
        });
    }

    public void setLong(String key, long val, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //for where
        JsonObject matcher   = new JsonObject();
        matcher.putString("KEY", key);
        query.putObject(MongoKeyWords.CRITERIA, matcher);

        //set fields
        JsonObject objNew = new JsonObject();
        objNew.putString("KEY", key);
        objNew.putNumber("VAL", val);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$,objNew);

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT,true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {

                boolean isUpdated = jsonObjectMessage.body().getBoolean(MongoKeyWords.IS_UPDATED,false);

                callback.handle(isUpdated);
            }
        });

    }

    public void incAndGetLong(String key, long delta,final Handler<Long> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //matcher
        JsonObject match = new JsonObject();
        match.putString("KEY", key);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber("VAL", delta);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);

        //sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.PhoneDBCols.NUMBER, -1);
        query.putObject(MongoKeyWords.SORT,sort);

        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                if(json!= null){
                    long newVal = json.getObject(MongoKeyWords.RESULT).getLong("VAL");
                    callback.handle(newVal);
                }
                else {
                    callback.handle(-1L);
                }
            }
        });
    }

}
