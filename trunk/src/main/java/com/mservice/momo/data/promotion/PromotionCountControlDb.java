package com.mservice.momo.data.promotion;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.promotion_server.PromotionColName;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 7/25/16.
 */
public class PromotionCountControlDb {

    private Vertx vertx;
    private Logger logger;

    public PromotionCountControlDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, String program, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, PromotionColName.PromotionCountControlCol.TABLE + "_" + program)
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

    public void findAndIncCountUser(int phoneNumber, String program, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, PromotionColName.PromotionCountControlCol.TABLE + "_" + program);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(PromotionColName.PromotionCountControlCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject setField = new JsonObject();
        setField.putNumber(PromotionColName.PromotionCountControlCol.PHONE_NUMBER, phoneNumber);
        setField.putString(PromotionColName.PromotionCountControlCol.PROGRAM, program);
        setField.putNumber(PromotionColName.PromotionCountControlCol.TIME, System.currentTimeMillis());
        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(PromotionColName.PromotionCountControlCol.COUNT, 1);

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

        public int phoneNumber = 0;
        public String program = "";
        public long time = 0;
        public int count = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {
            phoneNumber = jo.getInteger(PromotionColName.PromotionCountControlCol.PHONE_NUMBER, 0);
            program = jo.getString(PromotionColName.PromotionCountControlCol.PROGRAM, "");
            time = jo.getLong(PromotionColName.PromotionCountControlCol.TIME, 0);
            count = jo.getInteger(PromotionColName.PromotionCountControlCol.COUNT, 0);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putNumber(PromotionColName.PromotionCountControlCol.PHONE_NUMBER, phoneNumber);
            jo.putString(PromotionColName.PromotionCountControlCol.PROGRAM, program);
            jo.putNumber(PromotionColName.PromotionCountControlCol.TIME, time);
            jo.putNumber(PromotionColName.PromotionCountControlCol.COUNT, count);
            return jo;
        }
    }

}

