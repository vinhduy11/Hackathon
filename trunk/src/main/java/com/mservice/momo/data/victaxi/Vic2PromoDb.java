package com.mservice.momo.data.victaxi;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by manhly on 11/08/2016.
 */
public class Vic2PromoDb {
    private Vertx vertx;
    private Logger logger;

    public Vic2PromoDb(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void findByPhone(String phoneNumber, final Handler<Vic2PromoDb.Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic1PromoCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.Vic2PromoCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Vic2PromoDb.Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Vic2PromoDb.Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic2PromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.LottePromoCol.PHONE_NUMBER, phoneNumber);
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

    public static class Obj{

        public String phoneNumber = "";
        //public long registerTime = 0;
        public  boolean isNamed = false;
        public String tranType = "";

        public int tranId1 = 0;
        public  long bonusTime1 = 0;
        public long tranAmount1 = 0;
        public long bonusAmount1 = 0;
        public String error1 = "";
        public boolean useGift1 = false;

        public int tranId2 = 0;
        public  long bonusTime2 = 0;
        public long tranAmount2 = 0;
        public long bonusAmount2 = 0;
        public String error2= "";
        public boolean useGift2 = false;
//
        public int tranId3 = 0;
        public  long bonusTime3 = 0;
        public long tranAmount3 = 0;
        public long bonusAmount3 = 0;
        public String error3 = "";
        public boolean useGift3 = false;

        public  Obj () {

        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();

            jo.putString(colName.Vic2PromoCol.PHONE_NUMBER,phoneNumber);
            jo.putBoolean(colName.Vic2PromoCol.IS_NAMED,isNamed);
            jo.putString(colName.Vic2PromoCol.TRAN_TYPE,tranType);

            jo.putNumber(colName.Vic2PromoCol.TRAN_ID1,tranId1);
            jo.putNumber(colName.Vic2PromoCol.TRAN_AMOUNT1,tranAmount1);
            jo.putNumber(colName.Vic2PromoCol.BONUS_TIME1,bonusTime1);
            jo.putNumber(colName.Vic2PromoCol.BONUS_AMOUNT1,bonusAmount1);
            jo.putString(colName.Vic2PromoCol.ERROR1,error1);
            jo.putBoolean(colName.Vic2PromoCol.USED_GIFT1,useGift1);

            jo.putNumber(colName.Vic2PromoCol.TRAN_ID2,tranId2);
            jo.putNumber(colName.Vic2PromoCol.TRAN_AMOUNT2,tranAmount2);
            jo.putNumber(colName.Vic2PromoCol.BONUS_TIME2,bonusTime2);
            jo.putBoolean(colName.Vic2PromoCol.USED_GIFT1,useGift1);
            jo.putString(colName.Vic2PromoCol.ERROR2,error2);
            jo.putBoolean(colName.Vic2PromoCol.USED_GIFT1,useGift1);

            jo.putNumber(colName.Vic2PromoCol.TRAN_ID3,tranId3);
            jo.putNumber(colName.Vic2PromoCol.TRAN_AMOUNT3,tranAmount3);
            jo.putNumber(colName.Vic2PromoCol.BONUS_TIME3,bonusTime3);
            jo.putNumber(colName.Vic2PromoCol.BONUS_AMOUNT3,bonusAmount3);
            jo.putString(colName.Vic2PromoCol.ERROR3,error3);
            jo.putBoolean(colName.Vic2PromoCol.USED_GIFT3, useGift3);

            return jo;

        }
          public Obj(JsonObject jo){
              phoneNumber = jo.getString(colName.Vic2PromoCol.PHONE_NUMBER,"");
              isNamed = jo.getBoolean(colName.Vic2PromoCol.IS_NAMED,false);
              tranType = jo.getString(colName.Vic2PromoCol.TRAN_TYPE,"");

              tranId1 = jo.getInteger(colName.Vic2PromoCol.TRAN_ID1,0);
              tranAmount1 = jo.getLong(colName.Vic2PromoCol.TRAN_AMOUNT1,0);
              bonusTime1 = jo.getInteger(colName.Vic2PromoCol.BONUS_TIME1,0);
              bonusAmount1 = jo.getLong(colName.Vic2PromoCol.BONUS_AMOUNT1,0);
              error1 = jo.getString(colName.Vic2PromoCol.ERROR1,"");
              useGift1 = jo.getBoolean(colName.Vic2PromoCol.USED_GIFT1,false);

              tranId2 = jo.getInteger(colName.Vic2PromoCol.TRAN_ID2,0);
              tranAmount2 = jo.getLong(colName.Vic2PromoCol.TRAN_AMOUNT2,0);
              bonusTime2 = jo.getLong(colName.Vic2PromoCol.BONUS_TIME2,0);
              bonusAmount2 = jo.getLong(colName.Vic2PromoCol.BONUS_AMOUNT2,0);
              error2 = jo.getString(colName.Vic2PromoCol.ERROR2,"");
              useGift2 = jo.getBoolean(colName.Vic2PromoCol.USED_GIFT2,false);

              tranId3 = jo.getInteger(colName.Vic2PromoCol.TRAN_ID3,0);
              tranAmount3 = jo.getLong(colName.Vic2PromoCol.TRAN_AMOUNT3,0);
              bonusTime3 = jo.getLong(colName.Vic2PromoCol.BONUS_TIME3,0);
              bonusAmount3 = jo.getLong(colName.Vic2PromoCol.BONUS_AMOUNT3,0);
              error3 =jo.getString(colName.Vic2PromoCol.ERROR3,"");
              useGift3 = jo.getBoolean(colName.Vic2PromoCol.USED_GIFT3, false);

          }
    }



}
