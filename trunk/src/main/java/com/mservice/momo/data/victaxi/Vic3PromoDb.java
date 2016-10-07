package com.mservice.momo.data.victaxi;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by manhly on 11/08/2016.
 */
public class Vic3PromoDb {
    private Vertx vertx;
    private Logger logger;

    public Vic3PromoDb(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void findByPhone(String phoneNumber, final Handler<Vic3PromoDb.Obj> callback) {
        //query
        logger.info("ASSRGDGGGGGDFF"+ phoneNumber);
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic3PromoCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.Vic3PromoCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Vic3PromoDb.Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Vic3PromoDb.Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }

    public void insert(final Vic3PromoDb.Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.Vic3PromoCol.TABLE)
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
    public void update(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic3PromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.Vic3PromoCol.PHONE_NUMBER, phoneNumber);
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
    public void updatePromotionCode(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic3PromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.Vic3PromoCol.PHONE_NUMBER, phoneNumber);
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

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic3PromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.Vic3PromoCol.PHONE_NUMBER, phoneNumber);
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

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Vic3PromoCol.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Vic3PromoDb.Obj> arrayList = new ArrayList<Vic3PromoDb.Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        Vic3PromoDb.Obj obj = new Vic3PromoDb.Obj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public static class Obj{

        public String phoneNumber = "";
        public long registerTime = 0;
        public String promotionCode = "";
        public  boolean isPayVoucher1 = false;
        public  boolean isNamed = false;
        public int tranType = 0;

        public int tranId1 = 0;
        public long tranTime1 =0;
        public  long bonusTime1 = 0;
        public long tranAmount1 = 0;
        public long bonusAmount1 = 0;
        public  boolean isCashin1 = false;
        public int error1 = 0;
        public boolean useGift1 = false;
        public long giftTime1 = 0;

        public  boolean isPayVoucher2 = false;
        public int tranId2 = 0;
        public  long bonusTime2 = 0;
        public long tranAmount2 = 0;
        public long bonusAmount2 = 0;
        public  boolean isCashin2 = false;
        public String error2 = "";
        public boolean useGift2 = false;
        public long giftTime2 = 0;

        public  boolean isPayVoucher3 = false;
        public int tranId3 = 0;
        public  long bonusTime3 = 0;
        public long tranAmount3 = 0;
        public long bonusAmount3 = 0;
        public  boolean isCashin3 = false;
        public String error3 = "";
        public boolean useGift3 = false;
        public long giftTime3 = 0;

        public  Obj () {

        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();

            jo.putString(colName.Vic3PromoCol.PHONE_NUMBER,phoneNumber);
            jo.putNumber(colName.Vic3PromoCol.REGISTER_TIME,registerTime);
            jo.putString(colName.Vic3PromoCol.PROMOTION_CODE,promotionCode);
            jo.putBoolean(colName.Vic3PromoCol.IS_NAMED,isNamed);
            jo.putNumber(colName.Vic3PromoCol.TRAN_TYPE,tranType);

            jo.putNumber(colName.Vic3PromoCol.TRAN_ID1,tranId1);
            jo.putNumber(colName.Vic3PromoCol.TRAN_TIME1,tranTime1);
            jo.putNumber(colName.Vic3PromoCol.TRAN_AMOUNT1,tranAmount1);
            jo.putNumber(colName.Vic3PromoCol.BONUS_TIME1,bonusTime1);
            jo.putBoolean(colName.Vic3PromoCol.IS_PAY_VOUCHER1,isPayVoucher1);
            jo.putNumber(colName.Vic3PromoCol.BONUS_AMOUNT1,bonusAmount1);
            jo.putBoolean(colName.Vic3PromoCol.IS_CASHIN1,isCashin1);
            jo.putNumber(colName.Vic3PromoCol.ERROR1,error1);
            jo.putBoolean(colName.Vic3PromoCol.USED_GIFT1,useGift1);
            jo.putNumber(colName.Vic3PromoCol.BONUS_TIME1,bonusTime1);

            jo.putNumber(colName.Vic3PromoCol.TRAN_ID2,tranId2);
            jo.putNumber(colName.Vic3PromoCol.TRAN_AMOUNT2,tranAmount2);
            jo.putNumber(colName.Vic3PromoCol.BONUS_TIME2,bonusTime2);
            jo.putNumber(colName.Vic3PromoCol.BONUS_AMOUNT2,bonusAmount2);
            jo.putBoolean(colName.Vic3PromoCol.IS_CASHIN2,isCashin2);
            jo.putBoolean(colName.Vic3PromoCol.IS_PAY_VOUCHER2,isPayVoucher2);
            jo.putString(colName.Vic3PromoCol.ERROR2,error2);
            jo.putBoolean(colName.Vic3PromoCol.USED_GIFT2,useGift2);

            jo.putNumber(colName.Vic3PromoCol.TRAN_ID3,tranId3);
            jo.putNumber(colName.Vic3PromoCol.TRAN_AMOUNT3,tranAmount3);
            jo.putNumber(colName.Vic3PromoCol.BONUS_TIME3,bonusTime3);
            jo.putNumber(colName.Vic3PromoCol.BONUS_AMOUNT3,bonusAmount3);
            jo.putBoolean(colName.Vic3PromoCol.IS_PAY_VOUCHER3,isPayVoucher3);
            jo.putString(colName.Vic3PromoCol.ERROR3,error3);
            jo.putBoolean(colName.Vic3PromoCol.USED_GIFT3, useGift3);

            return jo;

        }
          public Obj(JsonObject jo){
              phoneNumber = jo.getString(colName.Vic3PromoCol.PHONE_NUMBER,"");
              registerTime = jo.getLong(colName.Vic3PromoCol.REGISTER_TIME,0);
              promotionCode = jo.getString(colName.Vic3PromoCol.PROMOTION_CODE,"");
              isNamed = jo.getBoolean(colName.Vic3PromoCol.IS_NAMED,false);
              tranType = jo.getInteger(colName.Vic3PromoCol.TRAN_TYPE,0);

              tranId1 = jo.getInteger(colName.Vic3PromoCol.TRAN_TIME1,0);
              tranTime1 = jo.getInteger(colName.Vic3PromoCol.TRAN_ID1,0);
              tranAmount1 = jo.getLong(colName.Vic3PromoCol.TRAN_AMOUNT1,0);
              bonusTime1 = jo.getInteger(colName.Vic3PromoCol.BONUS_TIME1,0);
              isPayVoucher1 = jo.getBoolean(colName.Vic3PromoCol.IS_PAY_VOUCHER1,false);
              isCashin2 = jo.getBoolean(colName.Vic3PromoCol.IS_CASHIN2,false);
              bonusAmount1 = jo.getLong(colName.Vic3PromoCol.BONUS_AMOUNT1,0);
              error1 = jo.getInteger(colName.Vic3PromoCol.ERROR1,0);
              useGift1 = jo.getBoolean(colName.Vic3PromoCol.USED_GIFT1,false);

              tranId2 = jo.getInteger(colName.Vic3PromoCol.TRAN_ID2,0);
              tranAmount2 = jo.getLong(colName.Vic3PromoCol.TRAN_AMOUNT2,0);
              bonusTime2 = jo.getLong(colName.Vic3PromoCol.BONUS_TIME2,0);
              bonusAmount2 = jo.getLong(colName.Vic3PromoCol.BONUS_AMOUNT2,0);
              isPayVoucher2 = jo.getBoolean(colName.Vic3PromoCol.IS_PAY_VOUCHER2,false);
              error2 = jo.getString(colName.Vic3PromoCol.ERROR2,"");
              useGift2 = jo.getBoolean(colName.Vic3PromoCol.USED_GIFT2,false);

              tranId3 = jo.getInteger(colName.Vic3PromoCol.TRAN_ID3,0);
              tranAmount3 = jo.getLong(colName.Vic3PromoCol.TRAN_AMOUNT3,0);
              bonusTime3 = jo.getLong(colName.Vic3PromoCol.BONUS_TIME3,0);
              bonusAmount3 = jo.getLong(colName.Vic3PromoCol.BONUS_AMOUNT3,0);
              isCashin3 = jo.getBoolean(colName.Vic3PromoCol.IS_CASHIN3,false);
              isPayVoucher3 = jo.getBoolean(colName.Vic3PromoCol.IS_PAY_VOUCHER3,false);
              error3 =jo.getString(colName.Vic3PromoCol.ERROR3,"");
              useGift3 = jo.getBoolean(colName.Vic3PromoCol.USED_GIFT3, false);

          }
    }



}
