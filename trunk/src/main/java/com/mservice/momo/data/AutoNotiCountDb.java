package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by congnguyenit on 7/27/16.
 */
public class AutoNotiCountDb {

    private Vertx vertx;
    private Logger logger;

    public AutoNotiCountDb(Vertx vertx, Logger logger) {
        this.vertx = vertx;
        this.logger = logger;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.AutoNotiCount.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                int result = 0;
                if (!message.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(message.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }

    public void findAndIncCount(String phoneNumber, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.AutoNotiCount.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.AutoNotiCount.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.AutoNotiCount.COUNT, 1);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);
        query.putBoolean(MongoKeyWords.MULTI, false);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndIncCount count promotion : " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndIncCount count promotion" +  json);
                if(json!= null){
                    JsonObject joAutoNotiCount = json.getObject(MongoKeyWords.RESULT);
                    if(joAutoNotiCount != null)
                    {
                        Obj referralObj = new Obj(joAutoNotiCount);
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

    public void findOneByType(String number, String type, final Handler<AutoNotiCountDb.Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.AutoNotiCount.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.AutoNotiCount.PHONE_NUMBER, number).putString(colName.AutoNotiCount.TYPE, type);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                AutoNotiCountDb.Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new AutoNotiCountDb.Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.AutoNotiCount.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.AutoNotiCount.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public static class Obj {
        public String phoneNumber;
        public String type;
        public int count;
        public long cashinTime;

        public Obj(JsonObject jo) {
            phoneNumber = jo.getString(colName.AutoNotiCount.PHONE_NUMBER, "");
            type = jo.getString(colName.AutoNotiCount.TYPE, "");
            count = jo.getInteger(colName.AutoNotiCount.COUNT, 1);
            cashinTime = jo.getLong(colName.AutoNotiCount.CASHIN_TIME, 0L);
        }

        public JsonObject toJson() {
            return
                    new JsonObject().putString(colName.AutoNotiCount.PHONE_NUMBER, phoneNumber)
                    .putString(colName.AutoNotiCount.TYPE, type)
                    .putNumber(colName.AutoNotiCount.COUNT, count)
                    .putNumber(colName.AutoNotiCount.CASHIN_TIME, cashinTime);

        }
    }
}
