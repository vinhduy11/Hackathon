package com.mservice.momo.data.referral;

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

/**
 * Created by concu on 4/5/16.
 */
public class ReferralV1CodeInputDb {
    private Vertx vertx;
    private Logger logger;

    public ReferralV1CodeInputDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {
        logger.info("insert ReferralV1");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE)
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

    public void updatePartial(String inviteeNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        logger.info("updatePartial ReferralV1");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, inviteeNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("status", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void upSert(String inviteeNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        logger.info("upSert ReferralV1");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, inviteeNumber);
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
        logger.info("search with filter ReferralV1");
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE);

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


    public void findOne(String inviteeNumber, final Handler<Obj> callback) {
        logger.info("findOne ReferralV1");
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, inviteeNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    logger.info(joResult.toString());
                    obj = new Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public void findAndModifyInviteeNumber(String inviteeNumber, int blockCondition, int block, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, inviteeNumber);
        match.putNumber(colName.ReferralV1CodeInputCol.IS_BLOCK, blockCondition);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.ReferralV1CodeInputCol.IS_BLOCK, block);
        ///obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);

        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyInviteeNumber: " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyInviteeNumber " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joReferral = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyInviteeNumber json is not null " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " +  joReferral);
                    if(joReferral != null)
                    {
                        ReferralV1CodeInputDb.Obj referralObj = new Obj(joReferral);
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

    public void findAndIncCountUser(String phoneNumber, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.ReferralV1CodeInputCol.COUNT, 1);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);
//        udpObj.putObject(MongoKeyWords.SET_$, setField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);
        query.putBoolean(MongoKeyWords.MULTI, false);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndIncCountUser count promotion : " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher count promotion" +  json);
                if(json!= null){
                    JsonObject joResponse = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndIncCountUser json is not null count promotion " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " +  joResponse);
                    if(joResponse != null)
                    {
                        Obj referralObj = new Obj(joResponse);
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

    public void findAndUpdateInfoUser(String number, JsonObject joUpdate, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.ReferralV1CodeInputCol.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, joUpdate);

        //update
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.MULTI, true);
        query.putBoolean(MongoKeyWords.UPSERT, false);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndUpdateInfoUser: " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " +  json);
                if(json!= null){
                    JsonObject joResponse = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndUpdateInfoUser json is not null " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + " " +  joResponse);
                    if(joResponse != null)
                    {
                        Obj acquireObj = new Obj(joResponse);
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
    public static class Obj {

        public String inviteeNumber = "";
        public String imei_code = "";
        public String inviterNumber = "";
        public long inputTime = 0;
        public long mappingTime = 0;
        public String inviterCardInfo = "";
        public String inviteeCardInfo = "";
        public String inviteeBankCode = "";
        public String inviterBankCode = "";

        public long bonus_time = 0;
        public long inviter_bonus_tid = 0;
        public long inviter_bonus_amount = 0;

        public long invitee_bonus_tid = 0;
        public long invitee_bonus_amount = 0;

        public boolean isMapped = false;
        public int isBlock = 0;
        int count = 0;
        public boolean sms;
        public boolean sms_mapped;
        public boolean invitee_extra_bonus = false;
        public boolean inviter_extra_bonus = false;
        public String noti = "";
        public boolean lock = false;
        public Obj() {
        }

        public Obj(JsonObject jo) {
            inviteeNumber = jo.getString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, "");
            imei_code = jo.getString(colName.ReferralV1CodeInputCol.IMEI_CODE, "");
            inviterNumber = jo.getString(colName.ReferralV1CodeInputCol.INVITER_NUMBER, "");
            inputTime = jo.getLong(colName.ReferralV1CodeInputCol.INPUT_TIME, 0);
            mappingTime = jo.getLong(colName.ReferralV1CodeInputCol.MAPPING_TIME, 0);
            inviterCardInfo = jo.getString(colName.ReferralV1CodeInputCol.INVITER_CARD_INFO, "");
            inviteeCardInfo = jo.getString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, "");
            inviteeBankCode = jo.getString(colName.ReferralV1CodeInputCol.INVITEE_BANK_CODE, "");
            inviterBankCode = jo.getString(colName.ReferralV1CodeInputCol.INVITER_BANK_CODE, "");
            bonus_time = jo.getLong(colName.ReferralV1CodeInputCol.BONUS_TIME, 0);
            inviter_bonus_tid = jo.getLong(colName.ReferralV1CodeInputCol.INVITER_BONUS_TID, 0);
            inviter_bonus_amount = jo.getLong(colName.ReferralV1CodeInputCol.INVITER_BONUS_AMOUNT, 0);

            invitee_bonus_tid = jo.getLong(colName.ReferralV1CodeInputCol.INVITEE_BONUS_TID, 0);
            invitee_bonus_amount = jo.getLong(colName.ReferralV1CodeInputCol.INVITEE_BONUS_AMOUNT, 0);
            count = jo.getInteger(colName.ReferralV1CodeInputCol.COUNT, 0);
            isMapped = jo.getBoolean(colName.ReferralV1CodeInputCol.IS_MAPPED, false);
            isBlock = jo.getInteger(colName.ReferralV1CodeInputCol.IS_BLOCK, 0);
            sms = jo.getBoolean(colName.ReferralV1CodeInputCol.SMS, false);
            sms_mapped = jo.getBoolean(colName.ReferralV1CodeInputCol.SMS_MAPPED, false);
            noti = jo.getString(colName.ReferralV1CodeInputCol.NOTI,"");
            invitee_extra_bonus = jo.getBoolean(colName.ReferralV1CodeInputCol.INVITEE_EXTRA_BONUS, false);
            inviter_extra_bonus = jo.getBoolean(colName.ReferralV1CodeInputCol.INVITER_EXTRA_BONUS, false);
            lock = jo.getBoolean(colName.ReferralV1CodeInputCol.LOCK, false);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, inviteeNumber);
            jo.putString(colName.ReferralV1CodeInputCol.IMEI_CODE, imei_code);
            jo.putString(colName.ReferralV1CodeInputCol.INVITER_NUMBER, inviterNumber);
            jo.putNumber(colName.ReferralV1CodeInputCol.INPUT_TIME, inputTime);
            jo.putNumber(colName.ReferralV1CodeInputCol.MAPPING_TIME, mappingTime);
            jo.putString(colName.ReferralV1CodeInputCol.INVITER_CARD_INFO, inviterCardInfo);
            jo.putString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, inviteeCardInfo);
            jo.putString(colName.ReferralV1CodeInputCol.INVITEE_BANK_CODE, inviteeBankCode);
            jo.putString(colName.ReferralV1CodeInputCol.INVITER_BANK_CODE, inviterBankCode);
            jo.putNumber(colName.ReferralV1CodeInputCol.BONUS_TIME, bonus_time);
            jo.putNumber(colName.ReferralV1CodeInputCol.INVITER_BONUS_TID, inviter_bonus_tid);
            jo.putNumber(colName.ReferralV1CodeInputCol.INVITER_BONUS_AMOUNT, inviter_bonus_amount);

            jo.putNumber(colName.ReferralV1CodeInputCol.INVITEE_BONUS_TID, invitee_bonus_tid);
            jo.putNumber(colName.ReferralV1CodeInputCol.INVITEE_BONUS_AMOUNT, invitee_bonus_amount);

            jo.putBoolean(colName.ReferralV1CodeInputCol.IS_MAPPED, isMapped);
            jo.putNumber(colName.ReferralV1CodeInputCol.IS_BLOCK, isBlock);
            jo.putNumber(colName.ReferralV1CodeInputCol.COUNT, count);
            jo.putBoolean(colName.ReferralV1CodeInputCol.SMS, sms);
            jo.putBoolean(colName.ReferralV1CodeInputCol.SMS_MAPPED, sms_mapped);
            jo.putString(colName.ReferralV1CodeInputCol.NOTI,noti);
            jo.putBoolean(colName.ReferralV1CodeInputCol.INVITEE_EXTRA_BONUS, invitee_extra_bonus);
            jo.putBoolean(colName.ReferralV1CodeInputCol.INVITER_EXTRA_BONUS, inviter_extra_bonus);
            jo.putBoolean(colName.ReferralV1CodeInputCol.LOCK, lock);
            return jo;
        }
    }
}
