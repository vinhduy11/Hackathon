//BEGIN 0000000004
package com.mservice.momo.data;

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
 * Created by khoa on 5/11/15.
 */
public class BillPayPromosDb {

    public static final String TABLE_NAME = "billpayPromo";

    private Vertx vertx;
    private Logger logger;

    public BillPayPromosDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table)
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
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.BillPayPromo.NUMBER, phoneNumber);
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
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

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

    public void searchWithPersonalData(String cmnd, String phoneNumber, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

        JsonObject jsonCmnd = new JsonObject();
        jsonCmnd.putString(colName.BillPayPromo.CMND, cmnd);

        JsonObject jsonPhone = new JsonObject();
        jsonPhone.putString(colName.BillPayPromo.NUMBER, phoneNumber);

        JsonArray jsonArrayOr = new JsonArray();
        jsonArrayOr.addObject(jsonCmnd);
        jsonArrayOr.addObject(jsonPhone);

        JsonObject jsonOr = new JsonObject();
        jsonOr.putArray(MongoKeyWords.OR, jsonArrayOr);

        query.putObject(MongoKeyWords.MATCHER, jsonOr);
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

    public void findOne(String phoneNumber, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.BillPayPromo.NUMBER, phoneNumber);
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

    public void findOneCardCheckSum(String cardCheckSum, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.BillPayPromo.CARD_CHECK_SUM_VISA, cardCheckSum);
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

    public void findOneCMND(String cmnd, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.BillPayPromo.CMND, cmnd);
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


    public void findGift2NeedToActive(long startTime, long endTime, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

//        if(filter != null && filter.getFieldNames().size() > 0){
//            query.putObject(MongoKeyWords.MATCHER, filter);
//        }

        JsonObject gte = new JsonObject();
        JsonObject lte = new JsonObject();

        gte.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime);
        lte.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime);

        JsonObject matcher = new JsonObject();
        matcher.putObject(colName.BillPayPromo.ACTIVATED_TIME_2, gte);
        matcher.putObject(colName.BillPayPromo.ACTIVATED_TIME_2, lte);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);
        query.putObject(MongoKeyWords.MATCHER, matcher);

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

    public void findGift4NeedToActive(long startTime, long endTime, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

//        if(filter != null && filter.getFieldNames().size() > 0){
//            query.putObject(MongoKeyWords.MATCHER, filter);
//        }

        JsonObject gte = new JsonObject();
        JsonObject lte = new JsonObject();

        gte.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime);
        lte.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime);

        JsonObject matcher = new JsonObject();
        matcher.putObject(colName.BillPayPromo.ACTIVATED_TIME_4, gte);
        matcher.putObject(colName.BillPayPromo.ACTIVATED_TIME_4, lte);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);
        query.putObject(MongoKeyWords.MATCHER, matcher);

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

    public void findGiftNeedToActive(long startTime, long endTime, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromo.table);

        JsonObject activatedTime4 = new JsonObject();

        activatedTime4.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime);
        activatedTime4.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime);

        JsonObject activatedTime2 = new JsonObject();

        activatedTime2.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime);
        activatedTime2.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime);

        JsonObject activatedTime4_1 = new JsonObject();
        activatedTime4_1.putObject(colName.BillPayPromo.ACTIVATED_TIME_4, activatedTime4);

        JsonObject activatedTime2_1 = new JsonObject();
        activatedTime2_1.putObject(colName.BillPayPromo.ACTIVATED_TIME_2, activatedTime2);


        JsonArray orArray = new JsonArray();
        orArray.addObject(activatedTime2_1);
        orArray.addObject(activatedTime4_1);

        JsonObject matcher = new JsonObject();
        matcher.putArray(MongoKeyWords.OR, orArray);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);
        query.putObject(MongoKeyWords.MATCHER, matcher);

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

    public static class Obj {

        public String number = ""; // wallet will be get voucher

        public String group = "";  // ma nhom khuyen mai, 1,2.....

        //promoted
        public int promoCount = 0; // so luong da khuyen mai

        public long proTimeVn_1 = 0; // thoi gian VN tra khuyen mai
        public long proTimeVn_2 = 0; // thoi gian VN tra khuyen mai
        public long proTimeVn_3 = 0; // thoi gian VN tra khuyen mai
        public long proTimeVn_4 = 0; // thoi gian VN tra khuyen mai


        public boolean has_mpoint = false;
        public long mpoint_value = 0;
        public String serviceIdPoint = "";

        public String giftTypeId1 = "";
        public String giftTypeId2 = "";
        public String giftTypeId3 = "";
        public String giftTypeId4 = "";

        public String serviceId1 = "";
        public String serviceId2 = "";

        public long tranId1 = 0;
        public long tranId2 = 0;
        public long tranId3 = 0;
        public long tranId4 = 0;

        public int tranType1 = 0;
        public int tranType2 = 0;
        public int tranType3 = 0;
        public int tranType4 = 0;

        public String giftId1 = "";
        public String giftId2 = "";
        public String giftId3 = "";
        public String giftId4 = "";

        public long activatedTime2 = 0;
        public long activatedTime4 = 0;

        public String cardCheckSum = "";
        public String cmnd = "";

        public Obj() {
        }

        public Obj(JsonObject jo) {

            number = jo.getString(colName.BillPayPromo.NUMBER, "");
            group = jo.getString(colName.BillPayPromo.GROUP, "");
            promoCount = jo.getInteger(colName.BillPayPromo.PROMO_COUNT, 0);

            proTimeVn_1 = jo.getLong(colName.BillPayPromo.PROMO_TIME_1, 0);
            proTimeVn_2 = jo.getLong(colName.BillPayPromo.PROMO_TIME_2, 0);
            proTimeVn_3 = jo.getLong(colName.BillPayPromo.PROMO_TIME_3, 0);
            proTimeVn_4 = jo.getLong(colName.BillPayPromo.PROMO_TIME_4, 0);

            has_mpoint = jo.getBoolean(colName.BillPayPromo.HAS_MPOINT, false);
            mpoint_value = jo.getLong(colName.BillPayPromo.MPOINT_VALUE, 0);
            serviceIdPoint = jo.getString(colName.BillPayPromo.SERVICE_ID_POINT, "");

            giftTypeId1 = jo.getString(colName.BillPayPromo.GIFT_TYPE_ID_1, "");
            giftTypeId2 = jo.getString(colName.BillPayPromo.GIFT_TYPE_ID_2, "");
            giftTypeId3 = jo.getString(colName.BillPayPromo.GIFT_TYPE_ID_3, "");
            giftTypeId4 = jo.getString(colName.BillPayPromo.GIFT_TYPE_ID_4, "");

            serviceId1 = jo.getString(colName.BillPayPromo.SERVICE_ID_1, "");
            serviceId2 = jo.getString(colName.BillPayPromo.SERVICE_ID_2, "");

            tranId1 = jo.getLong(colName.BillPayPromo.TRAN_ID_1, 0);
            tranId2 = jo.getLong(colName.BillPayPromo.TRAN_ID_2, 0);
            tranId3 = jo.getLong(colName.BillPayPromo.TRAN_ID_3, 0);
            tranId4 = jo.getLong(colName.BillPayPromo.TRAN_ID_4, 0);

            tranType1 = jo.getInteger(colName.BillPayPromo.TRAN_TYPE_1, 0);
            tranType2 = jo.getInteger(colName.BillPayPromo.TRAN_TYPE_2, 0);
            tranType3 = jo.getInteger(colName.BillPayPromo.TRAN_TYPE_3, 0);
            tranType4 = jo.getInteger(colName.BillPayPromo.TRAN_TYPE_4, 0);

            activatedTime2 = jo.getLong(colName.BillPayPromo.ACTIVATED_TIME_2, 0);
            activatedTime4 = jo.getLong(colName.BillPayPromo.ACTIVATED_TIME_4, 0);

            giftId1 = jo.getString(colName.BillPayPromo.GIFT_ID_1, "");
            giftId2 = jo.getString(colName.BillPayPromo.GIFT_ID_2, "");
            giftId3 = jo.getString(colName.BillPayPromo.GIFT_ID_3, "");
            giftId4 = jo.getString(colName.BillPayPromo.GIFT_ID_4, "");

            cardCheckSum = jo.getString(colName.BillPayPromo.CARD_CHECK_SUM_VISA, "");
            cmnd = jo.getString(colName.BillPayPromo.CMND, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.BillPayPromo.NUMBER, number);
            jo.putString(colName.BillPayPromo.GROUP, group);
            jo.putNumber(colName.BillPayPromo.PROMO_COUNT, promoCount);

            jo.putNumber(colName.BillPayPromo.PROMO_TIME_1, proTimeVn_1);
            jo.putNumber(colName.BillPayPromo.PROMO_TIME_2, proTimeVn_2);
            jo.putNumber(colName.BillPayPromo.PROMO_TIME_3, proTimeVn_3);
            jo.putNumber(colName.BillPayPromo.PROMO_TIME_4, proTimeVn_4);

            jo.putBoolean(colName.BillPayPromo.HAS_MPOINT, has_mpoint);
            jo.putNumber(colName.BillPayPromo.MPOINT_VALUE, mpoint_value);
            jo.putString(colName.BillPayPromo.SERVICE_ID_POINT, serviceIdPoint);

            jo.putString(colName.BillPayPromo.GIFT_TYPE_ID_1, giftTypeId1);
            jo.putString(colName.BillPayPromo.GIFT_TYPE_ID_2, giftTypeId2);
            jo.putString(colName.BillPayPromo.GIFT_TYPE_ID_3, giftTypeId3);
            jo.putString(colName.BillPayPromo.GIFT_TYPE_ID_4, giftTypeId4);

            jo.putString(colName.BillPayPromo.SERVICE_ID_1, serviceId1);
            jo.putString(colName.BillPayPromo.SERVICE_ID_2, serviceId2);

            jo.putNumber(colName.BillPayPromo.TRAN_ID_1, tranId1);
            jo.putNumber(colName.BillPayPromo.TRAN_ID_2, tranId2);
            jo.putNumber(colName.BillPayPromo.TRAN_ID_3, tranId3);
            jo.putNumber(colName.BillPayPromo.TRAN_ID_4, tranId4);

            jo.putNumber(colName.BillPayPromo.TRAN_TYPE_1, tranType1);
            jo.putNumber(colName.BillPayPromo.TRAN_TYPE_2, tranType2);
            jo.putNumber(colName.BillPayPromo.TRAN_TYPE_3, tranType3);
            jo.putNumber(colName.BillPayPromo.TRAN_TYPE_4, tranType4);

            jo.putNumber(colName.BillPayPromo.ACTIVATED_TIME_2, activatedTime2);
            jo.putNumber(colName.BillPayPromo.ACTIVATED_TIME_4, activatedTime4);

            jo.putString(colName.BillPayPromo.GIFT_ID_1, giftId1);
            jo.putString(colName.BillPayPromo.GIFT_ID_2, giftId2);
            jo.putString(colName.BillPayPromo.GIFT_ID_3, giftId3);
            jo.putString(colName.BillPayPromo.GIFT_ID_4, giftId4);

            jo.putString(colName.BillPayPromo.CARD_CHECK_SUM_VISA, cardCheckSum);
            jo.putString(colName.BillPayPromo.CMND, cmnd);

            return jo;
        }
    }

}
//END 0000000004