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
 * Created by concu on 5/18/15.
 */
public class BillPayPromoErrorDb {

    private Vertx vertx;
    private Logger logger;
    public BillPayPromoErrorDb(Vertx vertx, Logger logger){
        this.logger =logger;
        this.vertx =vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.BillPayPromoError.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromoError.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.BillPayPromoError.NUMBER, phoneNumber);
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

    public void upsertBillPayError(JsonObject si, final Handler<Boolean> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromoError.TABLE);

        //matcher
        JsonObject criteria = new JsonObject();
        criteria.putNumber(colName.BillPayPromoError.NUMBER, si.getInteger(colName.BillPayPromoError.NUMBER));
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, si);

        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                boolean result = message.body().getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromoError.TABLE);

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
        query.putString(MongoKeyWords.COLLECTION, colName.BillPayPromoError.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.BillPayPromoError.NUMBER, phoneNumber);
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
        public String id = "";
        public String number = "";
        public String group = "";
        public String service_id = "";
//        public String service_id_2 = "";
        public int error_code = 0;
        public String error_desc = "";
        public long time_error = 0;
//        public int status = 0; //0: chua tra, 1: tra roi.

        public Obj() {}

        public Obj(JsonObject jo) {
            id = jo.getString(colName.BillPayPromoError.ID, "");
            number = jo.getString(colName.BillPayPromoError.NUMBER, "");
            group = jo.getString(colName.BillPayPromoError.GROUP, "");
            service_id = jo.getString(colName.BillPayPromoError.SERVICE_ID, "");
//            service_id_2 = jo.getString(colName.BillPayPromoError.SERVICE_ID_2, "");
            error_code = jo.getInteger(colName.BillPayPromoError.ERROR_CODE, 0);
            error_desc = jo.getString(colName.BillPayPromoError.ERROR_DESC, "");
            time_error = jo.getLong(colName.BillPayPromoError.TIME_ERROR, 0);
//            promo_count = jo.getInteger(colName.BillPayPromoError.PROMO_COUNT, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.BillPayPromoError.ID, id);
            jo.putString(colName.BillPayPromoError.NUMBER, number);
            jo.putString(colName.BillPayPromoError.GROUP, group);
            jo.putString(colName.BillPayPromoError.SERVICE_ID, service_id);
//            jo.putString(colName.BillPayPromoError.SERVICE_ID_2, service_id_2);
            jo.putNumber(colName.BillPayPromoError.ERROR_CODE, error_code);
            jo.putString(colName.BillPayPromoError.ERROR_DESC, error_desc);
            jo.putNumber(colName.BillPayPromoError.TIME_ERROR, time_error);
//            jo.putNumber(colName.BillPayPromoError.PROMO_COUNT, promo_count);
            return jo;
        }
    }
}
