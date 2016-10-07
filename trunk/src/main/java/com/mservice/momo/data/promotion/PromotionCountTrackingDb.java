package com.mservice.momo.data.promotion;

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
 * Created by concu on 6/14/16.
 */
public class PromotionCountTrackingDb {
    private Vertx vertx;
    private Logger logger;

    public PromotionCountTrackingDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, String program, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.PromotionCountTracking.TABLE + "_" + program)
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

    public void findAndIncCountUser(String phoneNumber, String program, String id, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.PromotionCountTracking.TABLE + "_" + program);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.PromotionCountTracking.ID, id);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject setField = new JsonObject();
        setField.putString(colName.PromotionCountTracking.ID, id);
        setField.putString(colName.PromotionCountTracking.PROGRAM, program);
        setField.putString(colName.PromotionCountTracking.PHONE_NUMBER, phoneNumber);
        setField.putNumber(colName.PromotionCountTracking.TIME, System.currentTimeMillis());
        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.PromotionCountTracking.COUNT, 1);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);
        udpObj.putObject(MongoKeyWords.SET_$, setField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);
        query.putBoolean(MongoKeyWords.MULTI, false);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        logger.info("query findAndModifyUsedVoucher count promotion : " + query);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                logger.info("findAndModifyUsedVoucher count promotion" +  json);
                if(json!= null){
                    JsonObject joBinhTan = json.getObject(MongoKeyWords.RESULT);
                    logger.info("findAndModifyUsedVoucher json is not null count promotion " + StringConstUtil.BinhTanPromotion.PROGRAM + " " +  joBinhTan);
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

    public static class Obj {

        public String id = "";
        public String phoneNumber = "";
        public String program = "";
        public long time = 0;
        public int count = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {

            id = jo.getString(colName.PromotionCountTracking.ID, "");
            phoneNumber = jo.getString(colName.PromotionCountTracking.PHONE_NUMBER, "");
            program = jo.getString(colName.PromotionCountTracking.PROGRAM, "");
            time = jo.getLong(colName.PromotionCountTracking.TIME, 0);
            count = jo.getInteger(colName.PromotionCountTracking.COUNT, 0);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
             jo.getString(colName.PromotionCountTracking.ID, id);
             jo.getString(colName.PromotionCountTracking.PHONE_NUMBER, phoneNumber);
             jo.getString(colName.PromotionCountTracking.PROGRAM, program);
             jo.getLong(colName.PromotionCountTracking.TIME, time);
             jo.getInteger(colName.PromotionCountTracking.COUNT, count);
            return jo;
        }
    }

}
