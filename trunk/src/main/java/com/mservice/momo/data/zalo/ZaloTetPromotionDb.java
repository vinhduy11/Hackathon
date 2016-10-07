package com.mservice.momo.data.zalo;

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
 * Created by concu on 1/22/16.
 */
public class ZaloTetPromotionDb {

    private Vertx vertx;
    private Logger logger;

    public ZaloTetPromotionDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(String Collection, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, Collection)
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

    public void updatePartial(String Collection, String zaloCode
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, Collection);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ZaloTetPromotionCol.ZALO_CODE, zaloCode);
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

    public void updatePhoneNumberPartial(String Collection, String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, Collection);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, phoneNumber);
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

    public void upSert(String Collection, String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, Collection);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, phoneNumber);
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

    public void searchWithFilter(String Collection, JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, Collection);

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


    public void findZaloUserInfo(String Collection, String phoneNumber, String last_imei, String zalo_code, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, Collection);

        JsonObject jsonPhone = new JsonObject();

        jsonPhone.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, phoneNumber);

        JsonObject jsonImei = new JsonObject();

        jsonImei.putString(colName.ZaloTetPromotionCol.DEVICE_IMEI, last_imei);

        JsonObject jsonZaloCode = new JsonObject();

        jsonZaloCode.putString(colName.ZaloTetPromotionCol.ZALO_CODE, zalo_code);

        JsonArray orArray = new JsonArray();
        orArray.addObject(jsonPhone);
        orArray.addObject(jsonImei);
        orArray.addObject(jsonZaloCode);

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


    public void findOne(String Collection, String phoneNumber, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, Collection);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, phoneNumber);
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

//    public void findAndModify(final String Collection, final String number
//            , final Handler<Obj> callback) {
//
//        //query
//        JsonObject query = new JsonObject();
//        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
//        query.putString(MongoKeyWords.COLLECTION, Collection);
//
//        //matcher
//        JsonObject match = new JsonObject();
//        match.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, number);
//        query.putObject(MongoKeyWords.MATCHER, match);
//
//
//        JsonObject objNew = new JsonObject();
//        objNew.putBoolean(colName.IronManPromoGift.HAS_VOUCHER_GROUP_3, true);
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

    public void findOneZaloCode(final String Collection, String zaloCode, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, Collection);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.ZaloTetPromotionCol.ZALO_CODE, zaloCode);
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

    public static class Obj {

        public String phone_number = "";
        public String zalo_code = "";
        public long time = 0;

        public String gift_id_1 = "";
        public String gift_type_1 = "";

        public String gift_id_2 = "";
        public String gift_type_2 = "";

        public boolean hadMoney = false;
        public int moneyStatus = 0;

        public long end_time_money = 0;
        public long end_time_gift = 0;

        public String device_imei = "";
        public boolean hadBonus7Percent = false;
        public long end_bonus_7percent_time = 0;
        public String cashin_type = "";
        public long cashin_tid = 0;
        public boolean used_voucher_1 = false;
        public boolean used_voucher_2 = false;
        public Obj() {
        }

        public Obj(JsonObject jo) {
            phone_number = jo.getString(colName.ZaloTetPromotionCol.PHONE_NUMBER, "");
            zalo_code = jo.getString(colName.ZaloTetPromotionCol.ZALO_CODE, "");
            time = jo.getLong(colName.ZaloTetPromotionCol.TIME, 0);

            gift_id_1 = jo.getString(colName.ZaloTetPromotionCol.GIFT_ID_1, "");
            gift_type_1 = jo.getString(colName.ZaloTetPromotionCol.GIFT_TYPE_1, "");

            gift_id_2 = jo.getString(colName.ZaloTetPromotionCol.GIFT_ID_2, "");
            gift_type_2 = jo.getString(colName.ZaloTetPromotionCol.GIFT_TYPE_2, "");

            hadMoney = jo.getBoolean(colName.ZaloTetPromotionCol.HAD_MONEY, false);
            moneyStatus = jo.getInteger(colName.ZaloTetPromotionCol.MONEY_STATUS, 0);

            end_time_money = jo.getLong(colName.ZaloTetPromotionCol.END_TIME_MONEY, 0);
            end_time_gift = jo.getLong(colName.ZaloTetPromotionCol.END_TIME_GIFT, 0);

            device_imei = jo.getString(colName.ZaloTetPromotionCol.DEVICE_IMEI, "");
            hadBonus7Percent = jo.getBoolean(colName.ZaloTetPromotionCol.HAD_BONUS_7PERCENT, false);
            end_bonus_7percent_time = jo.getLong(colName.ZaloTetPromotionCol.END_BONUS_7PERCENT_TIME, 0);
            cashin_type = jo.getString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, "");
            cashin_tid = jo.getLong(colName.ZaloTetPromotionCol.CASH_IN_TID, 0);
            used_voucher_1  = jo.getBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, false);
            used_voucher_2  = jo.getBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, false);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, phone_number);
            jo.putString(colName.ZaloTetPromotionCol.ZALO_CODE, zalo_code);
            jo.putNumber(colName.ZaloTetPromotionCol.TIME, time);

            jo.putString(colName.ZaloTetPromotionCol.GIFT_ID_1, gift_id_1);

            jo.putString(colName.ZaloTetPromotionCol.GIFT_TYPE_1, gift_type_1);
            jo.putString(colName.ZaloTetPromotionCol.GIFT_ID_2, gift_id_2);
            jo.putString(colName.ZaloTetPromotionCol.GIFT_TYPE_2, gift_type_2);
            jo.putBoolean(colName.ZaloTetPromotionCol.HAD_MONEY, hadMoney);
            jo.putBoolean(colName.ZaloTetPromotionCol.HAD_BONUS_7PERCENT, hadBonus7Percent);
            jo.putNumber(colName.ZaloTetPromotionCol.MONEY_STATUS, moneyStatus);

            jo.putNumber(colName.ZaloTetPromotionCol.END_TIME_MONEY, end_time_money);
            jo.putNumber(colName.ZaloTetPromotionCol.END_TIME_GIFT, end_time_gift);
            jo.putString(colName.ZaloTetPromotionCol.DEVICE_IMEI, device_imei);
            jo.putNumber(colName.ZaloTetPromotionCol.END_BONUS_7PERCENT_TIME, end_bonus_7percent_time);
            jo.putString(colName.ZaloTetPromotionCol.CASH_IN_TYPE, cashin_type);
            jo.putNumber(colName.ZaloTetPromotionCol.CASH_IN_TID, cashin_tid);

            jo.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_1, used_voucher_1);
            jo.putBoolean(colName.ZaloTetPromotionCol.USED_VOUCHER_2, used_voucher_2);
            return jo;
        }
    }




}
