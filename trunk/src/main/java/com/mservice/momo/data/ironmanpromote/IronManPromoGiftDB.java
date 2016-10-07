package com.mservice.momo.data.ironmanpromote;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by concu on 9/8/15.
 */
public class IronManPromoGiftDB {

    private Vertx vertx;
    private Logger logger;

    public IronManPromoGiftDB(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.IronManPromoGift.PHONE_NUMBER, phoneNumber);
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

    public void update(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.IronManPromoGift.PHONE_NUMBER, phoneNumber);
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
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);

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
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.IronManPromoGift.PHONE_NUMBER, phoneNumber);
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

    public void findAndModify(final String number
            , final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.IronManPromoGift.PHONE_NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);


        JsonObject objNew = new JsonObject();
        objNew.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_3, true);


        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json = event.body();

                Obj tranObj = null;

                if (json != null && json.getString(MongoKeyWords.STATUS).equalsIgnoreCase("ok")) {
                    JsonObject jR = json.getObject(MongoKeyWords.RESULT);
                    tranObj = new Obj(jR);
                }

                callback.handle(tranObj);
            }
        });

    }

    public void findOneCardCheckSum(String cardCheckSum, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.IronManPromoGift.VISA_CARD, cardCheckSum);
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

    public void findBankInfo(String cardCheckSum, String cmnd, final Handler<List<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);

        JsonObject matcher = new JsonObject();

        JsonObject cardVisaObj = new JsonObject();
        cardVisaObj.putString(colName.IronManPromoGift.VISA_CARD, cardCheckSum);

        JsonObject cmndObj = new JsonObject();
        cmndObj.putString(colName.IronManPromoGift.CMND, cmnd);

        JsonArray jsonOr = new JsonArray();
        jsonOr.add(cardVisaObj);
        jsonOr.add(cmndObj);

        matcher.putArray(MongoKeyWords.OR, jsonOr);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                List<Obj> listObj = new ArrayList<Obj>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;
                        Obj obj = null;
                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);

                            String owner = record.getString(colName.IronManPromoGift.PHONE_NUMBER, "");
                            if (DataUtil.strToInt(owner) > 0) {
                                obj = new Obj();
                                obj.cmnd = record.getString(colName.IronManPromoGift.CMND, "");
                                obj.visa_card = record.getString(colName.IronManPromoGift.VISA_CARD, "");
                                obj.phone_number = record.getString(colName.IronManPromoGift.PHONE_NUMBER, "");
                                listObj.add(obj);
                            }
                        }
                    }
                } else {
                    logger.info(message.body().toString() + "  " + "status " + message.body().getString(MongoKeyWords.STATUS));
                }

                callback.handle(listObj);
//                Obj obj = null;
//                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
//                if (joResult != null) {
//                    obj = new Obj(joResult);
//                }
//
//                callback.handle(obj);
            }
        });
    }

    public void findOneCMND(String cmnd, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.IronManPromoGift.CMND, cmnd);
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

    public void findExpiredVoucherGroupOne(long time, JsonObject joFilter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);



        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, time * 60 * 60 * 1000L + System.currentTimeMillis());

        JsonObject endTime = new JsonObject();
        endTime.putObject(colName.IronManPromoGift.END_TIME_GROUP_1, expiredTime);

        JsonObject jsonUsedVoucher = new JsonObject();
        jsonUsedVoucher.putBoolean(colName.IronManPromoGift.USED_VOUCHER_1, false);

        JsonArray andArray = new JsonArray();
        andArray.addObject(endTime);
        andArray.addObject(jsonUsedVoucher);

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

    public void findExpiredVoucherGroupTwo(long time, JsonObject joFilter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, time * 24 * 60 * 60 * 1000L + System.currentTimeMillis());

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.IronManPromoGift.END_TIME_GROUP_2, expiredTime);

        // So luong noti da ban phai nho hon 2
        JsonObject numberOfNoti = new JsonObject();
        numberOfNoti.putNumber(MongoKeyWords.LESS_THAN, 2);

        //Bo dieu kien vao JsonObject noti
        JsonObject jsonNoti = new JsonObject();
        jsonNoti.putObject(colName.IronManPromoGift.NUMBER_OF_NOTI_2, numberOfNoti);

        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonNoti); //dk noti

        andArray.addObject(jsonEndTime); //dk thoi gian


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

    public void findExpiredVoucherGroupThree(long time, JsonObject joFilter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, time * 24 * 60 * 60 * 1000L + System.currentTimeMillis());

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.IronManPromoGift.END_TIME_GROUP_3, expiredTime);

        // So luong noti da ban phai nho hon 2
        JsonObject numberOfNoti = new JsonObject();
        numberOfNoti.putNumber(MongoKeyWords.LESS_THAN, 2);

        //Bo dieu kien vao JsonObject noti
        JsonObject jsonNoti = new JsonObject();
        jsonNoti.putObject(colName.IronManPromoGift.NUMBER_OF_NOTI_3, numberOfNoti);

        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonNoti); //dk noti

        andArray.addObject(jsonEndTime); //dk thoi gian


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

    public void findExpiredVoucherGroupFour(final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_THAN, System.currentTimeMillis());

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.IronManPromoGift.END_TIME_GROUP_4, expiredTime);

        JsonObject jsonUsed = new JsonObject();
        jsonUsed.putBoolean(colName.IronManPromoGift.USED_VOUCHER_12, false);

        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonUsed); //dk Da su dung chua

        andArray.addObject(jsonEndTime); //dk thoi gian


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

    public void findOneRemindedVoucherGroupFour(long time, JsonObject joFilter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, time * 60 * 60 * 1000L + System.currentTimeMillis());

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.IronManPromoGift.END_TIME_GROUP_4, expiredTime);

        // So luong noti da ban phai nho hon 2
        JsonObject numberOfNoti = new JsonObject();
        numberOfNoti.putNumber(MongoKeyWords.LESS_THAN, 2);

        //Bo dieu kien vao JsonObject noti
        JsonObject jsonNoti = new JsonObject();
        jsonNoti.putObject(colName.IronManPromoGift.NUMBER_OF_NOTI_4, numberOfNoti);

        JsonObject jsonUsed = new JsonObject();
        jsonUsed.putBoolean(colName.IronManPromoGift.USED_VOUCHER_12, false);
        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonNoti); //dk noti

        andArray.addObject(jsonEndTime); //dk thoi gian

        andArray.addObject(jsonUsed); //Nhung dua chua dung voucher


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

    public void findTwoRemindedVoucherGroupFour(long time, JsonObject joFilter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManPromoGift.TABLE);


        //Thoi gian het han qua phai 0 < time < 3 days
        JsonObject expiredTime = new JsonObject();
        expiredTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, time * 60 * 60 * 1000L + System.currentTimeMillis());

        //Bo dieu kien vao JsonObject
        JsonObject jsonEndTime = new JsonObject();
        jsonEndTime.putObject(colName.IronManPromoGift.END_TIME_GROUP_4, expiredTime);

        // So luong noti da ban phai nho hon 2
        JsonObject numberOfNoti = new JsonObject();
        numberOfNoti.putNumber(MongoKeyWords.LESS_THAN, 2);

        //Bo dieu kien vao JsonObject noti
        JsonObject jsonNoti = new JsonObject();
        jsonNoti.putObject(colName.IronManPromoGift.NUMBER_OF_NOTI_4, numberOfNoti);

        //Bo dieu kien vao array
        JsonArray andArray = new JsonArray();

        andArray.addObject(jsonNoti); //dk noti

        andArray.addObject(jsonEndTime); //dk thoi gian


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

        public String phone_number = "";
        public String cmnd = "";
        public String visa_card = "";

        public int promo_count = 0;

        public boolean has_voucher_group_1 = false;
        public boolean has_voucher_group_2 = false;
        public boolean has_voucher_group_3 = false;
        public boolean has_voucher_group_4 = false;

        public long start_time_group_1 = 0;
        public long start_time_group_2 = 0;
        public long start_time_group_3 = 0;
        public long start_time_group_4 = 0;

        public long end_time_group_1 = 0;
        public long end_time_group_2 = 0;
        public long end_time_group_3 = 0;
        public long end_time_group_4 = 0;

        public String service_v1 = "";

        public String service_v2 = "";
        public String service_v3 = "";
        public String service_v4 = "";
        public String service_v5 = "";

        public String service_v6 = "";
        public String service_v7 = "";
        public String service_v8 = "";
        public String service_v9 = "";
        public String service_v10 = "";
        public String service_v11 = "";
        public String service_v12 = "";

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
        public boolean used_voucher_11 = false;
        public boolean used_voucher_12 = false;

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
        public String gift_id_11 = "";

        public int number_of_noti_1 = 0;
        public int number_of_noti_2 = 0;
        public int number_of_noti_3 = 0;
        public int number_of_noti_4 = 0;
        public Obj() {
        }

        public Obj(JsonObject jo) {
            phone_number = jo.getString(colName.IronManPromoGift.PHONE_NUMBER, "");
            cmnd = jo.getString(colName.IronManPromoGift.CMND, "");
            visa_card = jo.getString(colName.IronManPromoGift.VISA_CARD, "");

            promo_count = jo.getInteger(colName.IronManPromoGift.PROMO_COUNT, 0);

            has_voucher_group_1 = jo.getBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_1, false);
            has_voucher_group_2 = jo.getBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_2, false);
            has_voucher_group_3 = jo.getBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_3, false);
            has_voucher_group_4 = jo.getBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_4, false);

            start_time_group_1 = jo.getLong(colName.IronManPromoGift.START_TIME_GROUP_1, 0);
            start_time_group_2 = jo.getLong(colName.IronManPromoGift.START_TIME_GROUP_2, 0);
            start_time_group_3 = jo.getLong(colName.IronManPromoGift.START_TIME_GROUP_3, 0);
            start_time_group_4 = jo.getLong(colName.IronManPromoGift.START_TIME_GROUP_4, 0);

            end_time_group_1 = jo.getLong(colName.IronManPromoGift.END_TIME_GROUP_1, 0);
            end_time_group_2 = jo.getLong(colName.IronManPromoGift.END_TIME_GROUP_2, 0);
            end_time_group_3 = jo.getLong(colName.IronManPromoGift.END_TIME_GROUP_3, 0);
            end_time_group_4 = jo.getLong(colName.IronManPromoGift.END_TIME_GROUP_4, 0);

            service_v1 = jo.getString(colName.IronManPromoGift.SERVICE_V1, "");

            service_v2 = jo.getString(colName.IronManPromoGift.SERVICE_V2, "");
            service_v3 = jo.getString(colName.IronManPromoGift.SERVICE_V3, "");
            service_v4 = jo.getString(colName.IronManPromoGift.SERVICE_V4, "");
            service_v5 = jo.getString(colName.IronManPromoGift.SERVICE_V5, "");

            service_v6 = jo.getString(colName.IronManPromoGift.SERVICE_V6, "");
            service_v7 = jo.getString(colName.IronManPromoGift.SERVICE_V7, "");
            service_v8 = jo.getString(colName.IronManPromoGift.SERVICE_V8, "");
            service_v9 = jo.getString(colName.IronManPromoGift.SERVICE_V9, "");
            service_v10 = jo.getString(colName.IronManPromoGift.SERVICE_V10, "");
            service_v11 = jo.getString(colName.IronManPromoGift.SERVICE_V11, "");
            service_v12 = jo.getString(colName.IronManPromoGift.SERVICE_V12, "");

            used_voucher_1 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_1, false);
            used_voucher_2 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_2, false);
            used_voucher_3 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_3, false);
            used_voucher_4 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_4, false);
            used_voucher_5 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_5, false);
            used_voucher_6 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_6, false);
            used_voucher_7 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_7, false);
            used_voucher_8 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_8, false);
            used_voucher_9 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_9, false);
            used_voucher_10 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_10, false);
            used_voucher_11 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_11, false);
            used_voucher_12 = jo.getBoolean(colName.IronManPromoGift.USED_VOUCHER_12, false);

            gift_id_1 = jo.getString(colName.IronManPromoGift.GIFT_ID_1, "");
            gift_id_2 = jo.getString(colName.IronManPromoGift.GIFT_ID_2, "");
            gift_id_3 = jo.getString(colName.IronManPromoGift.GIFT_ID_3, "");
            gift_id_4 = jo.getString(colName.IronManPromoGift.GIFT_ID_4, "");
            gift_id_5 = jo.getString(colName.IronManPromoGift.GIFT_ID_5, "");
            gift_id_6 = jo.getString(colName.IronManPromoGift.GIFT_ID_6, "");
            gift_id_7 = jo.getString(colName.IronManPromoGift.GIFT_ID_7, "");
            gift_id_8 = jo.getString(colName.IronManPromoGift.GIFT_ID_8, "");
            gift_id_9 = jo.getString(colName.IronManPromoGift.GIFT_ID_9, "");
            gift_id_10 = jo.getString(colName.IronManPromoGift.GIFT_ID_10, "");
            gift_id_11 = jo.getString(colName.IronManPromoGift.GIFT_ID_11, "");

            number_of_noti_1 = jo.getInteger(colName.IronManPromoGift.NUMBER_OF_NOTI_1, 0);
            number_of_noti_2 = jo.getInteger(colName.IronManPromoGift.NUMBER_OF_NOTI_2, 0);
            number_of_noti_3 = jo.getInteger(colName.IronManPromoGift.NUMBER_OF_NOTI_3, 0);
            number_of_noti_4 = jo.getInteger(colName.IronManPromoGift.NUMBER_OF_NOTI_4, 0);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.IronManPromoGift.PHONE_NUMBER, phone_number);
            jo.putString(colName.IronManPromoGift.CMND, cmnd);
            jo.putString(colName.IronManPromoGift.VISA_CARD, visa_card);

            jo.putNumber(colName.IronManPromoGift.PROMO_COUNT, promo_count);

            jo.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_1, has_voucher_group_1);
            jo.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_2, has_voucher_group_2);
            jo.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_3, has_voucher_group_3);
            jo.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_4, has_voucher_group_4);

            jo.putNumber(colName.IronManPromoGift.START_TIME_GROUP_1, start_time_group_1);
            jo.putNumber(colName.IronManPromoGift.START_TIME_GROUP_2, start_time_group_2);
            jo.putNumber(colName.IronManPromoGift.START_TIME_GROUP_3, start_time_group_3);
            jo.putNumber(colName.IronManPromoGift.START_TIME_GROUP_4, start_time_group_4);

            jo.putNumber(colName.IronManPromoGift.END_TIME_GROUP_1, end_time_group_1);
            jo.putNumber(colName.IronManPromoGift.END_TIME_GROUP_2, end_time_group_2);
            jo.putNumber(colName.IronManPromoGift.END_TIME_GROUP_3, end_time_group_3);
            jo.putNumber(colName.IronManPromoGift.END_TIME_GROUP_4, end_time_group_4);

            jo.putString(colName.IronManPromoGift.SERVICE_V1, service_v1);

            jo.putString(colName.IronManPromoGift.SERVICE_V2, service_v2);
            jo.putString(colName.IronManPromoGift.SERVICE_V3, service_v3);
            jo.putString(colName.IronManPromoGift.SERVICE_V4, service_v4);
            jo.putString(colName.IronManPromoGift.SERVICE_V5, service_v5);

            jo.putString(colName.IronManPromoGift.SERVICE_V6, service_v6);
            jo.putString(colName.IronManPromoGift.SERVICE_V7, service_v7);
            jo.putString(colName.IronManPromoGift.SERVICE_V8, service_v8);
            jo.putString(colName.IronManPromoGift.SERVICE_V9, service_v9);
            jo.putString(colName.IronManPromoGift.SERVICE_V10, service_v10);
            jo.putString(colName.IronManPromoGift.SERVICE_V11, service_v11);
            jo.putString(colName.IronManPromoGift.SERVICE_V12, service_v12);

            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_1, used_voucher_1);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_2, used_voucher_2);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_3, used_voucher_3);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_4, used_voucher_4);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_5, used_voucher_5);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_6, used_voucher_6);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_7, used_voucher_7);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_8, used_voucher_8);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_9, used_voucher_9);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_10, used_voucher_10);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_11, used_voucher_11);
            jo.putBoolean(colName.IronManPromoGift.USED_VOUCHER_12, used_voucher_12);

            jo.putString(colName.IronManPromoGift.GIFT_ID_1, gift_id_1);
            jo.putString(colName.IronManPromoGift.GIFT_ID_2, gift_id_2);
            jo.putString(colName.IronManPromoGift.GIFT_ID_3, gift_id_3);
            jo.putString(colName.IronManPromoGift.GIFT_ID_4, gift_id_4);
            jo.putString(colName.IronManPromoGift.GIFT_ID_5, gift_id_5);
            jo.putString(colName.IronManPromoGift.GIFT_ID_6, gift_id_6);
            jo.putString(colName.IronManPromoGift.GIFT_ID_7, gift_id_7);
            jo.putString(colName.IronManPromoGift.GIFT_ID_8, gift_id_8);
            jo.putString(colName.IronManPromoGift.GIFT_ID_9, gift_id_9);
            jo.putString(colName.IronManPromoGift.GIFT_ID_10, gift_id_10);
            jo.putString(colName.IronManPromoGift.GIFT_ID_11, gift_id_11);

            jo.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_1, number_of_noti_1);
            jo.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_2, number_of_noti_2);
            jo.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_3, number_of_noti_3);
            jo.putNumber(colName.IronManPromoGift.NUMBER_OF_NOTI_4, number_of_noti_4);

            return jo;
        }
    }




}
