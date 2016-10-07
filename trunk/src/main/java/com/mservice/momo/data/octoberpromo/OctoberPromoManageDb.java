package com.mservice.momo.data.octoberpromo;

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
import java.util.Calendar;

/**
 * Created by concu on 10/14/15.
 */
public class OctoberPromoManageDb {

    private Vertx vertx;
    private Logger logger;

    public OctoberPromoManageDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE)
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

    public void updatePartial(String id
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.OctoberPromoManageCols.ID, id);
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

    public void update(String id
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.OctoberPromoManageCols.ID, id);
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
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);

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

    public void findOne(String phoneNumber, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.OctoberPromoManageCols.PHONE_NUMBER, phoneNumber);
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

//    public void findAndModify(final String number
//            , final Handler<Obj> callback) {
//
//        //query
//        JsonObject query = new JsonObject();
//        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
//        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);
//
//        //matcher
//        JsonObject match = new JsonObject();
//        match.putString(colName.OctoberPromoManageCols.PHONE_NUMBER, number);
//        query.putObject(MongoKeyWords.MATCHER, match);
//
//
////        JsonObject objNew = new JsonObject();
////        objNew.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_3, true);
//
//
//        JsonObject update = new JsonObject();
//        update.putObject(MongoKeyWords.SET_$, objNew);
//        query.putObject(MongoKeyWords.UPDATE, update);
//        query.putBoolean(MongoKeyWords.UPSERT, false);
//
//        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> event) {
//                JsonObject json = event.body();
//
//                Obj tranObj = null;
//
//                if (json != null && json.getString(MongoKeyWords.STATUS).equalsIgnoreCase("ok")) {
//                    JsonObject jR = json.getObject(MongoKeyWords.RESULT);
//                    tranObj = new Obj(jR);
//                }
//
//                callback.handle(tranObj);
//            }
//        });
//
//    }

    public void findOneCardCheckSum(String cardCheckSum, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.OctoberPromoManageCols.CARD_CHECK_SUM, cardCheckSum);
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
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.OctoberPromoManageCols.CMND, cmnd);
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

    public void findExpiredLuckyVoucher(long time, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, time * 24 * 60 * 60 * 1000L + System.currentTimeMillis());

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.OctoberPromoManageCols.END_TIME_GIFT_RECEIVED, expiredTime);

        // So luong noti da ban phai nho hon 2
        JsonObject numberOfNoti = new JsonObject();
        numberOfNoti.putNumber(MongoKeyWords.LESS_THAN, 4);

        //Bo dieu kien vao JsonObject noti
        JsonObject jsonNoti = new JsonObject();
        jsonNoti.putObject(colName.OctoberPromoManageCols.NUMBER_OF_NOTI, numberOfNoti);

        JsonObject jsonLucky = new JsonObject();
        jsonLucky.putBoolean(colName.OctoberPromoManageCols.IS_LUCKY_MAN, true);

        JsonObject jStatus = new JsonObject();
        jStatus.putNumber(colName.OctoberPromoManageCols.STATUS, 0);
        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonNoti); //dk noti

        andArray.addObject(jsonEndTime); //dk thoi gian

        andArray.addObject(jsonLucky); //dk may man

        andArray.addObject(jStatus); //dk trang thai

        JsonObject matcher = new JsonObject();
        matcher.putArray(MongoKeyWords.AND_$, andArray);

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

    public void findExpiredUnLuckyVoucher(long time, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, time * 24 * 60 * 60 * 1000L + System.currentTimeMillis());

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.OctoberPromoManageCols.END_TIME_GIFT_RECEIVED, expiredTime);

        // So luong noti da ban phai nho hon 2
        JsonObject numberOfNoti = new JsonObject();
        numberOfNoti.putNumber(MongoKeyWords.LESS_THAN, 4);

        //Bo dieu kien vao JsonObject noti
        JsonObject jsonNoti = new JsonObject();
        jsonNoti.putObject(colName.OctoberPromoManageCols.NUMBER_OF_NOTI, numberOfNoti);

        JsonObject jsonLucky = new JsonObject();
        jsonLucky.putBoolean(colName.OctoberPromoManageCols.IS_LUCKY_MAN, false);

        JsonObject jStatus = new JsonObject();
        jStatus.putNumber(colName.OctoberPromoManageCols.STATUS, 0);

        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonNoti); //dk noti

        andArray.addObject(jsonEndTime); //dk thoi gian

        andArray.addObject(jsonLucky); //dk may man

        andArray.addObject(jStatus); //dk trang thai

        JsonObject matcher = new JsonObject();
        matcher.putArray(MongoKeyWords.AND_$, andArray);

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

    public void findLuckyNumber(long time, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.OctoberPromoManageCols.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long beginDay = calendar.getTimeInMillis();
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long endDay = calendar.getTimeInMillis();

        JsonObject lessExpiredTime = new JsonObject();
        lessExpiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, endDay);

        JsonObject greaterExpiredTime = new JsonObject();
        greaterExpiredTime.putNumber(MongoKeyWords.GREATER_OR_EQUAL, beginDay);

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.OctoberPromoManageCols.NEXT_TIME_TO_RECEIVE_GIFT, lessExpiredTime);

        JsonObject jsonBeginTime = new JsonObject();
        jsonBeginTime.putObject(colName.OctoberPromoManageCols.NEXT_TIME_TO_RECEIVE_GIFT, greaterExpiredTime);

        //Bo dieu kien vao JsonObject noti
        JsonObject jsonCompareAutoTime = new JsonObject();
        jsonCompareAutoTime.putNumber(MongoKeyWords.LESS_THAN, System.currentTimeMillis() - time * 24 * 60 * 60 * 1000L);
//        JsonObject jsonCompareAutoTime = new JsonObject();
//        jsonCompareAutoTime.putNumber(MongoKeyWords.LESS_THAN, System.currentTimeMillis() - 5 * 60 * 1000L);
        JsonObject jsonAutoTime = new JsonObject();
        jsonAutoTime.putObject(colName.OctoberPromoManageCols.AUTO_TIME_TO_RECEIVE_GIFT, jsonCompareAutoTime);

        JsonObject jsonLucky = new JsonObject();
        jsonLucky.putBoolean(colName.OctoberPromoManageCols.IS_LUCKY_MAN, true);

        JsonObject jsonCompareCount = new JsonObject();
        jsonCompareCount.putNumber(MongoKeyWords.LESS_THAN, 10);

        JsonObject jsonCount = new JsonObject();
        jsonCount.putObject(colName.OctoberPromoManageCols.PROMO_COUNT, jsonCompareCount);

        JsonObject jsonStatus = new JsonObject();
        jsonStatus.putNumber(colName.OctoberPromoManageCols.STATUS, 1);
        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonAutoTime); //dk noti

        andArray.addObject(jsonEndTime); //dk thoi gian

        andArray.addObject(jsonBeginTime); //dk thoi gian

        andArray.addObject(jsonLucky); //dk may man

        andArray.addObject(jsonCount);

        andArray.addObject(jsonStatus);

        JsonObject matcher = new JsonObject();
        matcher.putArray(MongoKeyWords.AND_$, andArray);

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

        public String id = "";
        public String phone_number = "";
        public String cmnd = "";
        public String card_check_sum = "";

        public int promo_count = 0;

        public int tran_type = 0;
        public long tran_id = 0;

        public boolean is_lucky_man = false;
        public int number_of_noti = 0;

        public long start_time_gift_received = 0;
        public long end_time_gift_received = 0;
        public long next_time_to_receive_gift = 0;
        public long auto_time_to_receive_gift = 0;
//        public String service_v1 = "";
//        public String service_v2 = "";
//        public String service_v3 = "";
//        public String service_v4 = "";
//        public String service_v5 = "";
//        public String service_v6 = "";
//        public String service_v7 = "";
//        public String service_v8 = "";
//        public String service_v9 = "";
//        public String service_v10 = "";


        public boolean used_voucher_1 = false;
        public boolean used_voucher_2 = false;
        public boolean used_voucher_3 = false;
        public boolean used_voucher_4 = false;
        public boolean used_voucher_5 = false;
        public boolean used_voucher_6 = false;
        public boolean used_voucher_7 = false;
        public boolean used_voucher_8 = false;
        public boolean used_voucher_9 = false;
        public boolean used_voucher_10 = false;

        public String gift_id_1 = "";
        public String gift_id_2 = "";
        public String gift_id_3 = "";
        public String gift_id_4 = "";
        public String gift_id_5 = "";
        public String gift_id_6 = "";
        public String gift_id_7 = "";
        public String gift_id_8 = "";
        public String gift_id_9 = "";
        public String gift_id_10 = "";

        public int status = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {

            id = jo.getString(colName.OctoberPromoManageCols.ID, "");
            phone_number = jo.getString(colName.OctoberPromoManageCols.PHONE_NUMBER, "");
            cmnd = jo.getString(colName.OctoberPromoManageCols.CMND, "");
            card_check_sum = jo.getString(colName.OctoberPromoManageCols.CARD_CHECK_SUM, "");

            promo_count = jo.getInteger(colName.OctoberPromoManageCols.PROMO_COUNT, 0);

            tran_type = jo.getInteger(colName.OctoberPromoManageCols.TRAN_TYPE, 0);

            tran_id = jo.getLong(colName.OctoberPromoManageCols.TRAN_ID, 0);

            is_lucky_man = jo.getBoolean(colName.OctoberPromoManageCols.IS_LUCKY_MAN, false);

            start_time_gift_received = jo.getLong(colName.OctoberPromoManageCols.START_TIME_GIFT_RECEIVED, 0);
            end_time_gift_received = jo.getLong(colName.OctoberPromoManageCols.END_TIME_GIFT_RECEIVED, 0);
            next_time_to_receive_gift = jo.getLong(colName.OctoberPromoManageCols.NEXT_TIME_TO_RECEIVE_GIFT, 0);
            auto_time_to_receive_gift = jo.getLong(colName.OctoberPromoManageCols.AUTO_TIME_TO_RECEIVE_GIFT, 0);
//            service_v1 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_1, "");
//            service_v2 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_2, "");
//            service_v3 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_3, "");
//            service_v4 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_4, "");
//            service_v5 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_5, "");
//            service_v6 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_6, "");
//            service_v7 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_7, "");
//            service_v8 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_8, "");
//            service_v9 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_9, "");
//            service_v10 = jo.getString(colName.OctoberPromoManageCols.SERVICE_ID_10, "");

            used_voucher_1 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_1, false);
            used_voucher_2 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_2, false);
            used_voucher_3 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_3, false);
            used_voucher_4 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_4, false);
            used_voucher_5 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_5, false);
            used_voucher_6 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_6, false);
            used_voucher_7 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_7, false);
            used_voucher_8 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_8, false);
            used_voucher_9 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_9, false);
            used_voucher_10 = jo.getBoolean(colName.OctoberPromoManageCols.USED_GIFT_10, false);

            gift_id_1 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_1, "");
            gift_id_2 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_2, "");
            gift_id_3 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_3, "");
            gift_id_4 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_4, "");
            gift_id_5 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_5, "");
            gift_id_6 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_6, "");
            gift_id_7 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_7, "");
            gift_id_8 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_8, "");
            gift_id_9 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_9, "");
            gift_id_10 = jo.getString(colName.OctoberPromoManageCols.GIFT_ID_10, "");

            number_of_noti = jo.getInteger(colName.OctoberPromoManageCols.NUMBER_OF_NOTI, 0);
            status = jo.getInteger(colName.OctoberPromoManageCols.STATUS, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.OctoberPromoManageCols.ID, id);
            jo.putString(colName.OctoberPromoManageCols.PHONE_NUMBER, phone_number);
            jo.putString(colName.OctoberPromoManageCols.CMND, cmnd);
            jo.putString(colName.OctoberPromoManageCols.CARD_CHECK_SUM, card_check_sum);
            jo.putNumber(colName.OctoberPromoManageCols.PROMO_COUNT, promo_count);

            jo.putNumber(colName.OctoberPromoManageCols.TRAN_TYPE, tran_type);
            jo.putNumber(colName.OctoberPromoManageCols.TRAN_ID, tran_id);

            jo.putBoolean(colName.OctoberPromoManageCols.IS_LUCKY_MAN, is_lucky_man);

            jo.putNumber(colName.OctoberPromoManageCols.START_TIME_GIFT_RECEIVED, start_time_gift_received);
            jo.putNumber(colName.OctoberPromoManageCols.END_TIME_GIFT_RECEIVED, end_time_gift_received);
            jo.putNumber(colName.OctoberPromoManageCols.NEXT_TIME_TO_RECEIVE_GIFT, next_time_to_receive_gift);
            jo.putNumber(colName.OctoberPromoManageCols.AUTO_TIME_TO_RECEIVE_GIFT, auto_time_to_receive_gift);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_1, service_v1);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_2, service_v2);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_3, service_v3);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_4, service_v4);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_5, service_v5);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_6, service_v6);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_7, service_v7);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_8, service_v8);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_9, service_v9);
//            jo.putString(colName.OctoberPromoManageCols.SERVICE_ID_10, service_v10);

            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_1, used_voucher_1);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_2, used_voucher_2);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_3, used_voucher_3);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_4, used_voucher_4);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_5, used_voucher_5);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_6, used_voucher_6);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_7, used_voucher_7);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_8, used_voucher_8);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_9, used_voucher_9);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_10, used_voucher_10);

            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_1, gift_id_1);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_2, gift_id_2);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_3, gift_id_3);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_4, gift_id_4);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_5, gift_id_5);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_6, gift_id_6);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_7, gift_id_7);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_8, gift_id_8);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_9, gift_id_9);
            jo.putString(colName.OctoberPromoManageCols.GIFT_ID_10, gift_id_10);

            jo.putNumber(colName.OctoberPromoManageCols.STATUS, status);
            jo.putNumber(colName.OctoberPromoManageCols.NUMBER_OF_NOTI, number_of_noti);

            return jo;
        }

        public JsonObject toJsonWithNotGiftId() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.OctoberPromoManageCols.ID, id);
            jo.putString(colName.OctoberPromoManageCols.PHONE_NUMBER, phone_number);
            jo.putString(colName.OctoberPromoManageCols.CMND, cmnd);
            jo.putString(colName.OctoberPromoManageCols.CARD_CHECK_SUM, card_check_sum);
            jo.putNumber(colName.OctoberPromoManageCols.PROMO_COUNT, promo_count);

            jo.putNumber(colName.OctoberPromoManageCols.TRAN_TYPE, tran_type);
            jo.putNumber(colName.OctoberPromoManageCols.TRAN_ID, tran_id);

            jo.putBoolean(colName.OctoberPromoManageCols.IS_LUCKY_MAN, is_lucky_man);

            jo.putNumber(colName.OctoberPromoManageCols.START_TIME_GIFT_RECEIVED, start_time_gift_received);
            jo.putNumber(colName.OctoberPromoManageCols.END_TIME_GIFT_RECEIVED, end_time_gift_received);
            jo.putNumber(colName.OctoberPromoManageCols.NEXT_TIME_TO_RECEIVE_GIFT, next_time_to_receive_gift);
            jo.putNumber(colName.OctoberPromoManageCols.AUTO_TIME_TO_RECEIVE_GIFT, auto_time_to_receive_gift);

            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_1, used_voucher_1);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_2, used_voucher_2);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_3, used_voucher_3);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_4, used_voucher_4);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_5, used_voucher_5);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_6, used_voucher_6);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_7, used_voucher_7);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_8, used_voucher_8);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_9, used_voucher_9);
            jo.putBoolean(colName.OctoberPromoManageCols.USED_GIFT_10, used_voucher_10);

            jo.putNumber(colName.OctoberPromoManageCols.STATUS, status);
            jo.putNumber(colName.OctoberPromoManageCols.NUMBER_OF_NOTI, number_of_noti);

            return jo;
        }
    }

}
