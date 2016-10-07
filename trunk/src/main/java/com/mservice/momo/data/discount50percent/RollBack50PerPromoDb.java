package com.mservice.momo.data.discount50percent;

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
 * Created by concu on 10/28/15.
 */
public class RollBack50PerPromoDb {

    private Vertx vertx;
    private Logger logger;
    public RollBack50PerPromoDb(Vertx vertx, Logger logger){
        this.logger =logger;
        this.vertx =vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.RollBack50PerPromo.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if(!event.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonObject error = new JsonObject(event.body().getString("message","{code:-1}"));
                    result =error.getInteger("code",-1);
                }
                callback.handle(result);
            }
        });
    }

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.RollBack50PerPromo.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.RollBack50PerPromo.PHONE_NUMBER, phoneNumber);

        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok","").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void upsertData(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.RollBack50PerPromo.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.RollBack50PerPromo.PHONE_NUMBER, phoneNumber);

        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok","").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.RollBack50PerPromo.TABLE);

        if(filter != null && filter.getFieldNames().size() > 0){
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if(joArr != null && joArr.size() > 0){
                    for (int i =0;i< joArr.size();i++){
                        Obj obj = new Obj((JsonObject)joArr.get(i));
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
        query.putString(MongoKeyWords.COLLECTION, colName.RollBack50PerPromo.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.RollBack50PerPromo.PHONE_NUMBER, phoneNumber);
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

    public void findInfoCustomer(String phoneNumber, String cmnd, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.RollBack50PerPromo.TABLE);

        JsonObject joPhone = new JsonObject();
        joPhone.putString(colName.RollBack50PerPromo.PHONE_NUMBER, phoneNumber);

        JsonObject joCMND = new JsonObject();
        joCMND.putString(colName.RollBack50PerPromo.CMND, cmnd);

        JsonArray jaOr = new JsonArray();
        jaOr.add(joPhone);
        jaOr.add(joCMND);

        JsonObject matcher = new JsonObject();
        matcher.putArray(MongoKeyWords.OR, jaOr);

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

    public void searchPersonalDataList(String cmnd, String phoneNumber, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.RollBack50PerPromo.TABLE);

        JsonObject jsonCmnd = new JsonObject();
        jsonCmnd.putString(colName.RollBack50PerPromo.CMND, cmnd);

        JsonObject jsonPhone = new JsonObject();
        jsonPhone.putString(colName.RollBack50PerPromo.PHONE_NUMBER, phoneNumber);

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

    public static class Obj {

        public String phone_number = "";
        public int promo_count = 0;
        public long tran_amount = 0;
        public long bank_amount = 0;
        public long bonus_amount = 0;
        public String bank_code = "";
        public String cmnd = "";
        public long tran_id = 0;

        public long bank_tran_id = 0;
        public long bonus_time = 0;
        public long promo_tran_id = 0;
        public String service_id = "";


        public Obj() {}

        public Obj(JsonObject jo) {

            phone_number = jo.getString(colName.RollBack50PerPromo.PHONE_NUMBER, "");
            promo_count = jo.getInteger(colName.RollBack50PerPromo.PROMO_COUNT, 0);
            bank_code = jo.getString(colName.RollBack50PerPromo.BANK_CODE, "");
            cmnd = jo.getString(colName.RollBack50PerPromo.CMND, "");

            tran_amount = jo.getLong(colName.RollBack50PerPromo.AMOUNT, 0);

            bank_amount = jo.getLong(colName.RollBack50PerPromo.BANK_AMOUNT, 0);
            bonus_amount = jo.getLong(colName.RollBack50PerPromo.BONUS_AMOUNT, 0);

            tran_id = jo.getLong(colName.RollBack50PerPromo.TRAN_ID, 0);
            bank_tran_id = jo.getLong(colName.RollBack50PerPromo.BANK_TRAN_ID, 0);

            bonus_time = jo.getLong(colName.RollBack50PerPromo.TIME, 0);

            service_id = jo.getString(colName.RollBack50PerPromo.SERVICE_ID, "");

            promo_tran_id = jo.getLong(colName.RollBack50PerPromo.PROMO_TRAN_ID, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.RollBack50PerPromo.PHONE_NUMBER, phone_number);

            jo.putNumber(colName.RollBack50PerPromo.PROMO_COUNT, promo_count);

            jo.putNumber(colName.RollBack50PerPromo.AMOUNT, tran_amount);
            jo.putNumber(colName.RollBack50PerPromo.BONUS_AMOUNT, bonus_amount);
            jo.putNumber(colName.RollBack50PerPromo.BANK_AMOUNT, bank_amount);

            jo.putString(colName.RollBack50PerPromo.BANK_CODE, bank_code);
            jo.putString(colName.RollBack50PerPromo.CMND, cmnd);

            jo.putNumber(colName.RollBack50PerPromo.TRAN_ID, tran_id);
            jo.putNumber(colName.RollBack50PerPromo.BANK_TRAN_ID, bank_tran_id);

            jo.putNumber(colName.RollBack50PerPromo.PROMO_TRAN_ID, promo_tran_id);

            jo.putNumber(colName.RollBack50PerPromo.TIME, bonus_time);

            jo.putString(colName.RollBack50PerPromo.SERVICE_ID, service_id);


            return jo;
        }
    }
}
