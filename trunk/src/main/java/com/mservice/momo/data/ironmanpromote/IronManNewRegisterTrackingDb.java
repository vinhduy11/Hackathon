package com.mservice.momo.data.ironmanpromote;

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
 * Created by concu on 9/7/15.
 * This class used to save infomation of new user who join Iron man program.
 */
public class IronManNewRegisterTrackingDb {

    private Vertx vertx;
    private Logger logger;

    public IronManNewRegisterTrackingDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.IronManNewRegisterTracking.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.IronManNewRegisterTracking.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.IronManNewRegisterTracking.PHONE_NUMBER, phoneNumber);
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

    public void upsertData(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManNewRegisterTracking.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.IronManNewRegisterTracking.PHONE_NUMBER, phoneNumber);
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
        query.putString(MongoKeyWords.COLLECTION, colName.IronManNewRegisterTracking.TABLE);

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
        query.putString(MongoKeyWords.COLLECTION, colName.IronManNewRegisterTracking.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.IronManNewRegisterTracking.PHONE_NUMBER, phoneNumber);
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

    public void countUserStatus(int program_number, final Handler<JsonArray> callback){

        JsonObject sumNumber = new JsonObject()
                .putNumber("$sum", 1);

        JsonObject grouper = new JsonObject()
                .putString("_id", "$has_bonus")
                .putObject("count", sumNumber);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.COLLECTION, colName.IronManNewRegisterTracking.TABLE);
        query.putString("action", "aggregate");

        query.putObject(MongoKeyWords.GROUPER, grouper);
        query.putObject(MongoKeyWords.MATCHER, new JsonObject().putNumber(colName.IronManNewRegisterTracking.PROGRAM_NUMBER, program_number));
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if(event.body() != null)
                {
                    JsonArray result = event.body().getArray("result");
                    callback.handle(result);
                }
                else {
                    callback.handle(new JsonArray());
                }
            }
        });
    }
    public static class Obj {

        //Phone number of User
        public String phoneNumber = "";

        //Bankcode of user (VCB or OCB)
        public String bankCode = "";

        //CMND of USER if having bankcode
        public String cmnd = "";

        //Card Visa number
        public String visaCardNumber = "";

        //Time to Register
        public long timeRegistry = 0;

        //user has bonus or note.
        public boolean hasBonus = false;

        public int program_number = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {

            phoneNumber = jo.getString(colName.IronManNewRegisterTracking.PHONE_NUMBER, "").trim();

            bankCode = jo.getString(colName.IronManNewRegisterTracking.BANK_CODE, "").trim();

            cmnd = jo.getString(colName.IronManNewRegisterTracking.CMND, "").trim();

            visaCardNumber = jo.getString(colName.IronManNewRegisterTracking.VISA_CARD_NUMBER, "").trim();

            timeRegistry = jo.getLong(colName.IronManNewRegisterTracking.TIME_REGISTRY, 0);

            hasBonus = jo.getBoolean(colName.IronManNewRegisterTracking.HAS_BONUS, false);

            program_number = jo.getInteger(colName.IronManNewRegisterTracking.PROGRAM_NUMBER, 0);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.IronManNewRegisterTracking.PHONE_NUMBER, phoneNumber.trim());
            jo.putString(colName.IronManNewRegisterTracking.BANK_CODE, bankCode.trim());
            jo.putString(colName.IronManNewRegisterTracking.CMND, cmnd.trim());
            jo.putString(colName.IronManNewRegisterTracking.VISA_CARD_NUMBER, visaCardNumber.trim());
            jo.putNumber(colName.IronManNewRegisterTracking.TIME_REGISTRY, timeRegistry);
            jo.putBoolean(colName.IronManNewRegisterTracking.HAS_BONUS, hasBonus);
            jo.putNumber(colName.IronManNewRegisterTracking.PROGRAM_NUMBER, program_number);

            return jo;
        }
    }

}
