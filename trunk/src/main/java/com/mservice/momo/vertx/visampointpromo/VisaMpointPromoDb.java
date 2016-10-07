package com.mservice.momo.vertx.visampointpromo;

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
 * Created by concu on 5/21/15.
 */
public class VisaMpointPromoDb {

    private Vertx vertx;
    private Logger logger;
    public VisaMpointPromoDb(Vertx vertx, Logger logger){
        this.logger =logger;
        this.vertx =vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.VisaMPointPromo.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMPointPromo.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.VisaMPointPromo.NUMBER, phoneNumber);
        match.putBoolean(colName.VisaMPointPromo.END_MONTH, false);

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

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMPointPromo.TABLE);

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

    public void searchPersonalDataList(String cardcheckSum, String phoneNumber, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMPointPromo.TABLE);

        JsonObject jsonCmnd = new JsonObject();
        jsonCmnd.putString(colName.VisaMPointPromo.CARD_NUMBER, cardcheckSum);

        JsonObject jsonPhone = new JsonObject();
        jsonPhone.putString(colName.VisaMPointPromo.NUMBER, phoneNumber);

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
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMPointPromo.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.VisaMPointPromo.NUMBER, phoneNumber);
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

        public String number = "";
        public String card_number = "";
        public long mpoint_1 = 0;
        public long mpoint_2 = 0;
        public int promo_count = 0;
        public long time_1 = 0;
        public long time_2 = 0;
        public boolean end_month = false;
        public long tid_1 = 0;
        public long tid_2 = 0;
        public int trantype_1 = 0;
        public int trantype_2 = 0;

        public long tid_visa_1 = 0;
        public long tid_visa_2 = 0;

        public String service_id_1 = "";
        public String service_id_2 = "";

        public long total_amount_1 = 0;
        public long total_amount_2 = 0;

        public long cashin_amount_1 = 0;
        public long cashin_amount_2 = 0;

        public long cashinTime_1 = 0;
        public long cashinTime_2 = 0;


        public Obj() {}

        public Obj(JsonObject jo) {

            number = jo.getString(colName.VisaMPointPromo.NUMBER, "");

            card_number = jo.getString(colName.VisaMPointPromo.CARD_NUMBER, "");
            mpoint_1 = jo.getLong(colName.VisaMPointPromo.MPOINT_1, 0);
            mpoint_2 = jo.getLong(colName.VisaMPointPromo.MPOINT_2, 0);
            promo_count = jo.getInteger(colName.VisaMPointPromo.PROMO_COUNT, 0);
            time_1 = jo.getLong(colName.VisaMPointPromo.TIME_1, 0);
            time_2 = jo.getLong(colName.VisaMPointPromo.TIME_2, 0);
            end_month = jo.getBoolean(colName.VisaMPointPromo.END_MONTH, false);
            tid_1 = jo.getLong(colName.VisaMPointPromo.TID_1, 0);
            tid_2 = jo.getLong(colName.VisaMPointPromo.TID_2, 0);
            trantype_1 = jo.getInteger(colName.VisaMPointPromo.TRANTYPE_1, 0);
            trantype_2 = jo.getInteger(colName.VisaMPointPromo.TRANTYPE_2, 0);
            tid_visa_1 = jo.getLong(colName.VisaMPointPromo.TID_VISA_1, 0);
            tid_visa_2 = jo.getLong(colName.VisaMPointPromo.TID_VISA_2, 0);

            service_id_1 = jo.getString(colName.VisaMPointPromo.SERVICE_ID_1, "");
            service_id_2 = jo.getString(colName.VisaMPointPromo.SERVICE_ID_2, "");

            total_amount_1 = jo.getLong(colName.VisaMPointPromo.TOTAL_AMOUNT_1, 0);
            total_amount_2 = jo.getLong(colName.VisaMPointPromo.TOTAL_AMOUNT_2, 0);

            cashin_amount_1 = jo.getLong(colName.VisaMPointPromo.CASH_IN_AMOUNT_1, 0);
            cashin_amount_2 = jo.getLong(colName.VisaMPointPromo.CASH_IN_AMOUNT_2, 0);

            cashinTime_1 = jo.getLong(colName.VisaMPointPromo.CASH_IN_TIME_1, 0);
            cashinTime_2 = jo.getLong(colName.VisaMPointPromo.CASH_IN_TIME_2, 0);

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.VisaMPointPromo.NUMBER, number);

            jo.putString(colName.VisaMPointPromo.CARD_NUMBER, card_number);
            jo.putNumber(colName.VisaMPointPromo.MPOINT_1, mpoint_1);
            jo.putNumber(colName.VisaMPointPromo.MPOINT_2, mpoint_2);
            jo.putNumber(colName.VisaMPointPromo.PROMO_COUNT, promo_count);
            jo.putNumber(colName.VisaMPointPromo.TIME_1, time_1);
            jo.putNumber(colName.VisaMPointPromo.TIME_2, time_2);
            jo.putBoolean(colName.VisaMPointPromo.END_MONTH, end_month);
            jo.putNumber(colName.VisaMPointPromo.TID_1, tid_1);
            jo.putNumber(colName.VisaMPointPromo.TID_2, tid_2);
            jo.putNumber(colName.VisaMPointPromo.TRANTYPE_1, trantype_1);
            jo.putNumber(colName.VisaMPointPromo.TRANTYPE_2, trantype_2);
            jo.putNumber(colName.VisaMPointPromo.TID_VISA_1, tid_visa_1);
            jo.putNumber(colName.VisaMPointPromo.TID_VISA_2, tid_visa_2);

            jo.putString(colName.VisaMPointPromo.SERVICE_ID_1, service_id_1);
            jo.putString(colName.VisaMPointPromo.SERVICE_ID_2, service_id_2);

            jo.putNumber(colName.VisaMPointPromo.TOTAL_AMOUNT_1, total_amount_1);
            jo.putNumber(colName.VisaMPointPromo.TOTAL_AMOUNT_2, total_amount_2);

            jo.putNumber(colName.VisaMPointPromo.CASH_IN_AMOUNT_1, cashin_amount_1);
            jo.putNumber(colName.VisaMPointPromo.CASH_IN_AMOUNT_2, cashin_amount_2);

            jo.putNumber(colName.VisaMPointPromo.CASH_IN_TIME_1, cashinTime_1);
            jo.putNumber(colName.VisaMPointPromo.CASH_IN_TIME_2, cashinTime_2);

            return jo;
        }
    }
}
