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
 * Created by manhly on 25/08/2016.
 */
public class VicPromoDb {
    private Vertx vertx;
    private Logger logger;

    public VicPromoDb(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void findByPhone(String phoneNumber, final Handler<VicPromoDb.Obj> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.VicPromoCol.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.VicPromoCol.PHONE_NUMBER, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                VicPromoDb.Obj obj = null;
                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new VicPromoDb.Obj(joResult);
                }
                callback.handle(obj);
            }
        });
    }



    public void insert(final VicPromoDb.Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.VicPromoCol.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.VicPromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.VicPromoCol.PHONE_NUMBER, phoneNumber);
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
        query.putString(MongoKeyWords.COLLECTION, colName.VicPromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.VicPromoCol.PHONE_NUMBER, phoneNumber);
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
        query.putString(MongoKeyWords.COLLECTION, colName.VicPromoCol.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.VicPromoCol.PHONE_NUMBER, phoneNumber);
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

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<VicPromoDb.Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VicPromoCol.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<VicPromoDb.Obj> arrayList = new ArrayList<VicPromoDb.Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        VicPromoDb.Obj obj = new VicPromoDb.Obj((JsonObject) joArr.get(i));
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
        public  boolean isPayVoucher1;
        public  boolean isNamed;
        public int tranType = 0;

        public String group = "";
        public int tranId1 = 0;
        public long tranTime1 =0;
        public  long bonusTime1 = 0;
        public long tranAmount1 = 0;
        public long bonusAmount1 = 0;
        public String giftId1 = "";
        public  boolean isCashin1 ;
        public int error1 = 0;
        public boolean sendCashinNoti1;
        public boolean sendCashinNoti2;
        public boolean sendCashinNoti3;
        public boolean useGift1 ;
        public long giftTime1 = 0;

        public  boolean isPayVoucher2 ;
        public int tranId2 = 0;
        public  long bonusTime2 = 0;
        public long tranAmount2 = 0;
        public long bonusAmount2 = 0;
        public String giftId2 = "";
        public  boolean isCashin2 ;
        public int error2 = 0;
        public boolean useGift2 ;
        public long giftTime2 = 0;

        public  boolean isPayVoucher3 ;
        public int tranId3 = 0;
        public  long bonusTime3 = 0;
        public long tranAmount3 = 0;
        public long bonusAmount3 = 0;
        public String giftId3 = "";
        public  boolean isCashin3 ;
        public int error3 = 0;
        public boolean useGift3 ;
        public long giftTime3 = 0;

        public  Obj () {

        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();

            jo.putString(colName.VicPromoCol.PHONE_NUMBER,phoneNumber);
            jo.putNumber(colName.VicPromoCol.REGISTER_TIME,registerTime);
            jo.putString(colName.VicPromoCol.PROMOTION_CODE,promotionCode);
            jo.putBoolean(colName.VicPromoCol.IS_NAMED,isNamed);
            jo.putNumber(colName.VicPromoCol.TRAN_TYPE,tranType);

            jo.putBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI1,sendCashinNoti1);
            jo.putBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI2,sendCashinNoti2);
            jo.putBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI3,sendCashinNoti3);


            jo.putString(colName.VicPromoCol.GROUP,group);
            jo.putNumber(colName.VicPromoCol.TRAN_ID1,tranId1);
            jo.putNumber(colName.VicPromoCol.TRAN_TIME1,tranTime1);
            jo.putNumber(colName.VicPromoCol.TRAN_AMOUNT1,tranAmount1);
            jo.putNumber(colName.VicPromoCol.BONUS_TIME1,bonusTime1);
            jo.putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER1,isPayVoucher1);
            jo.putNumber(colName.VicPromoCol.BONUS_AMOUNT1,bonusAmount1);
            jo.putString(colName.VicPromoCol.GIFTID1,giftId1);
            jo.putBoolean(colName.VicPromoCol.IS_CASHIN1,isCashin1);
            jo.putNumber(colName.VicPromoCol.ERROR1,error1);
            jo.putBoolean(colName.VicPromoCol.USED_GIFT1,useGift1);
            jo.putNumber(colName.VicPromoCol.BONUS_TIME1,bonusTime1);

            jo.putNumber(colName.VicPromoCol.TRAN_ID2,tranId2);
            jo.putNumber(colName.VicPromoCol.TRAN_AMOUNT2,tranAmount2);
            jo.putNumber(colName.VicPromoCol.BONUS_TIME2,bonusTime2);
            jo.putNumber(colName.VicPromoCol.BONUS_AMOUNT2,bonusAmount2);
            jo.putBoolean(colName.VicPromoCol.IS_CASHIN2,isCashin2);
            jo.putString(colName.VicPromoCol.GIFTID2,giftId2);
            jo.putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER2,isPayVoucher2);
            jo.putNumber(colName.VicPromoCol.ERROR2,error2);
            jo.putBoolean(colName.VicPromoCol.USED_GIFT2,useGift2);

            jo.putNumber(colName.VicPromoCol.TRAN_ID3,tranId3);
            jo.putNumber(colName.VicPromoCol.TRAN_AMOUNT3,tranAmount3);
            jo.putNumber(colName.VicPromoCol.BONUS_TIME3,bonusTime3);
            jo.putNumber(colName.VicPromoCol.BONUS_AMOUNT3,bonusAmount3);
            jo.putString(colName.VicPromoCol.GIFTID3,giftId3);
            jo.putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER3,isPayVoucher3);
            jo.putNumber(colName.VicPromoCol.ERROR3,error3);
            jo.putBoolean(colName.VicPromoCol.USED_GIFT3, useGift3);

            return jo;

        }
        public Obj(JsonObject jo){
            phoneNumber = jo.getString(colName.VicPromoCol.PHONE_NUMBER,"");
            registerTime = jo.getLong(colName.VicPromoCol.REGISTER_TIME,0);
            promotionCode = jo.getString(colName.VicPromoCol.PROMOTION_CODE,"");
            isNamed = jo.getBoolean(colName.VicPromoCol.IS_NAMED,false);
            tranType = jo.getInteger(colName.VicPromoCol.TRAN_TYPE,0);

            sendCashinNoti1 = jo.getBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI1,false);
            sendCashinNoti2 = jo.getBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI2,false);
            sendCashinNoti3 = jo.getBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI3,false);


            group = jo.getString(colName.VicPromoCol.GROUP,"");
            tranId1 = jo.getInteger(colName.VicPromoCol.TRAN_TIME1,0);
            tranTime1 = jo.getInteger(colName.VicPromoCol.TRAN_ID1,0);
            tranAmount1 = jo.getLong(colName.VicPromoCol.TRAN_AMOUNT1,0);
            bonusTime1 = jo.getLong(colName.VicPromoCol.BONUS_TIME1,0);
            isPayVoucher1 = jo.getBoolean(colName.VicPromoCol.IS_PAY_VOUCHER1,false);
            isCashin1 = jo.getBoolean(colName.VicPromoCol.IS_CASHIN1,false);
            isCashin2 = jo.getBoolean(colName.VicPromoCol.IS_CASHIN2,false);
            bonusAmount1 = jo.getLong(colName.VicPromoCol.BONUS_AMOUNT1,0);
            giftId1 = jo.getString(colName.VicPromoCol.GIFTID1,"");
            error1 = jo.getInteger(colName.VicPromoCol.ERROR1,0);
            useGift1 = jo.getBoolean(colName.VicPromoCol.USED_GIFT1,false);

            tranId2 = jo.getInteger(colName.VicPromoCol.TRAN_ID2,0);
            tranAmount2 = jo.getLong(colName.VicPromoCol.TRAN_AMOUNT2,0);
            bonusTime2 = jo.getLong(colName.VicPromoCol.BONUS_TIME2,0);
            bonusAmount2 = jo.getLong(colName.VicPromoCol.BONUS_AMOUNT2,0);
            giftId2 = jo.getString(colName.VicPromoCol.GIFTID2,"");
            isPayVoucher2 = jo.getBoolean(colName.VicPromoCol.IS_PAY_VOUCHER2,false);
            error2 = jo.getInteger(colName.VicPromoCol.ERROR2,0);
            useGift2 = jo.getBoolean(colName.VicPromoCol.USED_GIFT2,false);

            tranId3 = jo.getInteger(colName.VicPromoCol.TRAN_ID3,0);
            tranAmount3 = jo.getLong(colName.VicPromoCol.TRAN_AMOUNT3,0);
            bonusTime3 = jo.getLong(colName.VicPromoCol.BONUS_TIME3,0);
            bonusAmount3 = jo.getLong(colName.VicPromoCol.BONUS_AMOUNT3,0);
            giftId3 = jo.getString(colName.VicPromoCol.GIFTID3,"");
            isCashin3 = jo.getBoolean(colName.VicPromoCol.IS_CASHIN3,false);
            isPayVoucher3 = jo.getBoolean(colName.VicPromoCol.IS_PAY_VOUCHER3,false);
            error3 =jo.getInteger(colName.VicPromoCol.ERROR3,0);
            useGift3 = jo.getBoolean(colName.VicPromoCol.USED_GIFT3, false);

        }
    }


}
