package com.mservice.momo.data.victaxi;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.boon.Str;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by manhly on 11/08/2016.
 */
public class Vic1PromoDb {
    private Vertx vertx;
    private Logger logger;

    public Vic1PromoDb(Vertx vertx,Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }
    public void findByPhone(String phoneNumber, final Handler<Vic1PromoDb.Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic1PromoCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.Vic1PromoCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Vic1PromoDb.Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Vic1PromoDb.Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic1PromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.Vic1PromoCol.PHONE_NUMBER, phoneNumber);
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
        public  boolean isNamed = false;
        public String tranType = "";

        public int tranId = 0;
        public  long tranTime = 0;
        public  long bonusTime = 0;
        public long tranAmount = 0;
        public long bonusAmount = 0;
        public  boolean isCashin = false;
        public String error = "";
        public boolean useGift = false;


        public  Obj () {

        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();

            jo.putString(colName.Vic1PromoCol.PHONE_NUMBER,phoneNumber);
            jo.putBoolean(colName.Vic1PromoCol.IS_NAMED,isNamed);
            jo.putString(colName.Vic1PromoCol.TRAN_TYPE,tranType);

            jo.putNumber(colName.Vic1PromoCol.TRAN_ID,tranId);
            jo.putNumber(colName.Vic1PromoCol.TRAN_TIME,tranTime);
            jo.putNumber(colName.Vic1PromoCol.TRAN_AMOUNT,tranAmount);
            jo.putNumber(colName.Vic1PromoCol.BONUS_TIME,bonusTime);
            jo.putNumber(colName.Vic1PromoCol.BONUS_AMOUNT,bonusAmount);
            jo.putBoolean(colName.Vic1PromoCol.IS_CASHIN,isCashin);
            jo.putString(colName.Vic1PromoCol.ERROR,error);
            jo.putBoolean(colName.Vic1PromoCol.USED_GIFT,useGift);

            return jo;

        }
          public Obj(JsonObject jo){
              phoneNumber = jo.getString(colName.Vic1PromoCol.PHONE_NUMBER,"");
              isNamed = jo.getBoolean(colName.Vic1PromoCol.IS_NAMED,false);
              tranType = jo.getString(colName.Vic1PromoCol.TRAN_TYPE,"");

              tranId = jo.getInteger(colName.Vic1PromoCol.TRAN_ID,0);
              tranTime = jo.getInteger(colName.Vic1PromoCol.TRAN_TIME,0);
              tranAmount = jo.getLong(colName.Vic1PromoCol.TRAN_AMOUNT,0);
              bonusTime = jo.getInteger(colName.Vic1PromoCol.BONUS_TIME,0);
              bonusAmount = jo.getLong(colName.Vic1PromoCol.BONUS_AMOUNT,0);
              isCashin = jo.getBoolean(colName.Vic1PromoCol.IS_CASHIN,false);
              error = jo.getString(colName.Vic1PromoCol.ERROR,"");
              useGift = jo.getBoolean(colName.Vic1PromoCol.USED_GIFT,false);



          }
    }



}
