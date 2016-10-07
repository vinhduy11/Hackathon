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
 */
public class IronManBonusTrackingTableDb {

    private Vertx vertx;
    private Logger logger;

    public IronManBonusTrackingTableDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.IronManBonusTrackingTable.TABLE)
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

    public void updatePartial(int program_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManBonusTrackingTable.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, program_number);
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

    public void upsertPartial(int program_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManBonusTrackingTable.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putNumber(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, program_number);
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
        query.putString(MongoKeyWords.COLLECTION, colName.IronManBonusTrackingTable.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);
        query.putObject(MongoKeyWords.SORT, new JsonObject().putNumber(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, 1));
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

    public void findOne(int number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.IronManBonusTrackingTable.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putNumber(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, number);
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

        public String program = "";
        public int program_number = 0;
        public int numerator = 0;
        public int denominator = 0;
        public long start_time = 0;
        public long end_time = 0;
        public int number_of_bonus_man = 0;
        public int number_of_new_comer = 0;
        public int max_ratio = 0;
        public int min_ratio = 0;
        public boolean not_ratio_flag = false;

        public int number_of_bonus_gave_man = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            program = jo.getString(colName.IronManBonusTrackingTable.PROGRAM, "").trim();

            program_number = jo.getInteger(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, 0);

            numerator = jo.getInteger(colName.IronManBonusTrackingTable.NUMERATOR, 0);

            denominator = jo.getInteger(colName.IronManBonusTrackingTable.DENOMINATOR, 0);

            start_time = jo.getLong(colName.IronManBonusTrackingTable.START_TIME, 0);

            end_time = jo.getLong(colName.IronManBonusTrackingTable.END_TIME, 0);

            number_of_bonus_man = jo.getInteger(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, 0);

            number_of_new_comer = jo.getInteger(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, 0);

            not_ratio_flag = jo.getBoolean(colName.IronManBonusTrackingTable.NOT_RATIO_FLAG, false);

            max_ratio = jo.getInteger(colName.IronManBonusTrackingTable.MAX_RATIO, 0);
            min_ratio = jo.getInteger(colName.IronManBonusTrackingTable.MIN_RATIO, 0);

            number_of_bonus_gave_man = jo.getInteger(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_GAVE_MAN, 0);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.IronManBonusTrackingTable.PROGRAM, program.trim());
            jo.putNumber(colName.IronManBonusTrackingTable.PROGRAM_NUMBER, program_number);

            jo.putNumber(colName.IronManBonusTrackingTable.NUMERATOR, numerator);
            jo.putNumber(colName.IronManBonusTrackingTable.DENOMINATOR, denominator);

            jo.putNumber(colName.IronManBonusTrackingTable.START_TIME, start_time);
            jo.putNumber(colName.IronManBonusTrackingTable.END_TIME, end_time);

            jo.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, number_of_bonus_man);
            jo.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, number_of_new_comer);

            jo.putBoolean(colName.IronManBonusTrackingTable.NOT_RATIO_FLAG, not_ratio_flag);

            jo.putNumber(colName.IronManBonusTrackingTable.MAX_RATIO, max_ratio);
            jo.putNumber(colName.IronManBonusTrackingTable.MIN_RATIO, min_ratio);

            jo.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_GAVE_MAN, number_of_bonus_gave_man);

            return jo;
        }
    }



}
