package com.mservice.momo.data.binhtanpromotion;

import com.mservice.momo.data.ironmanpromote.IronManBonusTrackingTableDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by khoanguyen on 03/05/2016.
 */
public class AcquireBinhTanUserPromotionDb {
    private Vertx vertx;
    private Logger logger;

    public AcquireBinhTanUserPromotionDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if (!event.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(event.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);
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

    public void updateAllField(JsonObject joConditional, JsonObject joUpdate, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);


//        JsonObject criteria = new JsonObject();
        query.putObject(MongoKeyWords.CRITERIA, joConditional);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);
        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.MULTI, true);
        logger.info("query update end group 3 " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject obj = message.body();
                logger.info(obj.toString());
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    public void upsertPartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        Obj obj = new Obj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void findOne(String number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public void findAndManageUser(String number, boolean oldLock, boolean newlock, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, number);
        match.putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, oldLock);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject objNew = new JsonObject();
        objNew.putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, newlock);


        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);

        //update
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.MULTI, true);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyUsedVoucher: " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joBinhTan = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyUsedVoucher json is not null " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  joBinhTan);
                    if(joBinhTan != null)
                    {
                        Obj acquireObj = new Obj(joBinhTan);
                        callback.handle(acquireObj);
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

    public void findAndUpdateInfoUser(String number, boolean oldLock, boolean newlock, JsonObject joUpdate, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, number);
        match.putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, oldLock);
        query.putObject(MongoKeyWords.MATCHER, match);

//        JsonObject objNew = new JsonObject();
        joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, newlock);


        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, joUpdate);

        //update
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.MULTI, true);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyUsedVoucher: " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joBinhTan = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyUsedVoucher json is not null " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  joBinhTan);
                    if(joBinhTan != null)
                    {
                        Obj acquireObj = new Obj(joBinhTan);
                        callback.handle(acquireObj);
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

    public void findAndIncEchoUser(String phoneNumber, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_ECHO, 1);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);
        query.putBoolean(MongoKeyWords.MULTI, true);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyUsedVoucher: " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joBinhTan = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyUsedVoucher json is not null " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  joBinhTan);
                    if(joBinhTan != null)
                    {
                        Obj referralObj = new Obj(joBinhTan);
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

    public void findAndIncOtpUser(String phoneNumber, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_OTP, 1);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);
        query.putBoolean(MongoKeyWords.MULTI, true);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyUsedVoucher: " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joBinhTan = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyUsedVoucher json is not null " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  joBinhTan);
                    if(joBinhTan != null)
                    {
                        Obj referralObj = new Obj(joBinhTan);
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

    public void countUserStatus(final IronManBonusTrackingTableDb.Obj ironManBonusTracking, final Handler<JsonArray> callback){

        JsonObject sumNumber = new JsonObject()
                .putNumber("$sum", 1);

        JsonObject grouper = new JsonObject()
                .putString("_id", "$has_bonus")
                .putObject("count", sumNumber);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.COLLECTION, colName.AcquireBinhTanUserPromotionCol.TABLE);
        query.putString("action", "aggregate");

        query.putObject(MongoKeyWords.GROUPER, grouper);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ironManBonusTracking.start_time);
        calendar.set(Calendar.SECOND, 0);
        long startBlockTime = calendar.getTimeInMillis();

        calendar.setTimeInMillis(ironManBonusTracking.end_time);
        calendar.set(Calendar.SECOND, 59);
        long endBlockTime = calendar.getTimeInMillis();

        JsonObject joStartTime = new JsonObject().putObject(colName.AcquireBinhTanUserPromotionCol.TIME, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, startBlockTime));
        JsonObject joEndTime = new JsonObject().putObject(colName.AcquireBinhTanUserPromotionCol.TIME, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, endBlockTime));
        JsonArray jarrAnd = new JsonArray().add(joStartTime).add(joEndTime);
        JsonObject joFilter = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd);
        query.putObject(MongoKeyWords.MATCHER, joFilter);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if(event.body() != null)
                {
                    JsonArray result = event.body().getArray("result");
                    callback.handle(result);
                }
                else {
                    callback.handle(new JsonArray());
                }
            }
        });
    }

    public static class Obj {

        public String phoneNumber = "";
        public String imei = "";
        public String extra_key = "";
        public long time = 0;
        public int group = 0;
        public JsonObject joExtra = new JsonObject();
        public long tid_cashin = 0;
        public long amount_cashin = 0;
        public long time_cashin = 0;
        public long time_group_3 = 0;
        public boolean end_group_2 = false;
        public boolean end_group_3 = false;
        public boolean lock = false;
        public long next_time_bonus = 0;
        public long next_time_rollback = 0;
        public int echo = 0;
        public int numberOfOtp = 0;
        public String bankId = "";
        public boolean isLocked = false;
        public boolean hasBonus = false;
        public boolean isTopup = false;
        public int noti_times;
        public long time_of_noti_fire;
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phoneNumber = jo.getString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, "");
            imei = jo.getString(colName.AcquireBinhTanUserPromotionCol.IMEI, "");
            extra_key = jo.getString(colName.AcquireBinhTanUserPromotionCol.EXTRA_KEY, "");
            time = jo.getLong(colName.AcquireBinhTanUserPromotionCol.TIME, 0);
            group = jo.getInteger(colName.AcquireBinhTanUserPromotionCol.GROUP, 0);
            joExtra = jo.getObject(colName.AcquireBinhTanUserPromotionCol.EXTRA, new JsonObject());
            tid_cashin = jo.getLong(colName.AcquireBinhTanUserPromotionCol.TID_CASHIN, 0);
            amount_cashin = jo.getLong(colName.AcquireBinhTanUserPromotionCol.AMOUNT_CASHIN, 0);
            time_cashin = jo.getLong(colName.AcquireBinhTanUserPromotionCol.TIME_CASHIN, 0);
            time_group_3 = jo.getLong(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, 0);
            end_group_2 = jo.getBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, false);
            end_group_3 = jo.getBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, false);
            lock = jo.getBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, false);
            next_time_bonus = jo.getLong(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_BONUS, 0);
            next_time_rollback = jo.getLong(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_ROLLBACK, 0);
            echo = jo.getInteger(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_ECHO, 0);
            numberOfOtp = jo.getInteger(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_OTP, 0);
            bankId = jo.getString(colName.AcquireBinhTanUserPromotionCol.BANK_CARD_ID, "");
            isLocked = jo.getBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, false);
            hasBonus = jo.getBoolean(colName.AcquireBinhTanUserPromotionCol.HAS_BONUS, false);
            isTopup = jo.getBoolean(colName.AcquireBinhTanUserPromotionCol.IS_TOPUP, false);
            noti_times = jo.getInteger(colName.AcquireBinhTanUserPromotionCol.NOTI_TIMES, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);
            jo.putString(colName.AcquireBinhTanUserPromotionCol.IMEI, imei);
            jo.putString(colName.AcquireBinhTanUserPromotionCol.EXTRA_KEY, extra_key);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME, time);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.GROUP, group);
            jo.putObject(colName.AcquireBinhTanUserPromotionCol.EXTRA, joExtra);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.TID_CASHIN, tid_cashin);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.AMOUNT_CASHIN, amount_cashin);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_CASHIN, time_cashin);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, time_group_3);
            jo.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, end_group_2);
            jo.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, end_group_3);
            jo.putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, lock);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_BONUS, next_time_bonus);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.NEXT_TIME_ROLLBACK, next_time_rollback);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_ECHO, echo);
            jo.putNumber(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_OTP, numberOfOtp);
            jo.putString(colName.AcquireBinhTanUserPromotionCol.BANK_CARD_ID, bankId);
            jo.putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, isLocked);
            jo.putBoolean(colName.AcquireBinhTanUserPromotionCol.HAS_BONUS, hasBonus);
            jo.putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_TOPUP, isTopup);
            return jo;
        }
    }
}
