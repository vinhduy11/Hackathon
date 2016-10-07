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
 * Created by concu on 5/27/15.
 */
public class VisaMpointErrorDb {



    private Vertx vertx;
    private Logger logger;
    public VisaMpointErrorDb(Vertx vertx, Logger logger){
        this.logger =logger;
        this.vertx =vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.VisaMpointError.TABLE)
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

//    public void updatePartial(String cardnumber
//            , JsonObject joUpdate, final Handler<Boolean> callback){
//
//        JsonObject query = new JsonObject();
//        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
//        query.putString(MongoKeyWords.COLLECTION, colName.VisaMpointError.TABLE);
//        JsonObject match = new JsonObject();
//
//        //matcher
//        match.putString(colName.VMCardIdCardNumber.CARDNUMBER, cardnumber);
//        query.putObject(MongoKeyWords.CRITERIA, match);
//
//
//        JsonObject fieldsSet = new JsonObject();
//        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);
//
//        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
//        query.putBoolean(MongoKeyWords.UPSERT, false);
//
//        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> jsonObjectssage) {
//                JsonObject obj = jsonObjectMessage.body();
//                boolean result = obj.getString("ok","").equalSSgnoreCase("ok");
//                callback.handle(result);
//            }
//        });
//    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VisaMpointError.TABLE);

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



    public static class Obj {

        public String number = "";
        public int error = 0;
        public String desc_error = "";
        public long time = 0;
        public long tranid = 0;
        public long trantype = 0;
        public String cardnumber = "";
        public int count = 0;

        public Obj() {}

        public Obj(JsonObject jo) {

            number = jo.getString(colName.VisaMpointError.NUMBER, "");
            error = jo.getInteger(colName.VisaMpointError.ERROR, 0);
            desc_error = jo.getString(colName.VisaMpointError.DESC_ERROR, "");
            time = jo.getLong(colName.VisaMpointError.TIME, 0);
            tranid = jo.getLong(colName.VisaMpointError.TRANID, 0);
            trantype = jo.getInteger(colName.VisaMpointError.TRANTYPE, 0);
            count = jo.getInteger(colName.VisaMpointError.COUNT, 0);
            cardnumber = jo.getString(colName.VisaMpointError.CARDNUMBER, "");

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

              jo.putString(colName.VisaMpointError.NUMBER, number);
              jo.putNumber(colName.VisaMpointError.ERROR, error);
              jo.putString(colName.VisaMpointError.DESC_ERROR, desc_error);
              jo.putNumber(colName.VisaMpointError.TIME, time);
              jo.putNumber(colName.VisaMpointError.TRANID, tranid);
              jo.putNumber(colName.VisaMpointError.TRANTYPE, trantype);
                jo.putNumber(colName.VisaMpointError.COUNT, count);
              jo.putString(colName.VisaMpointError.CARDNUMBER, cardnumber);
            return jo;
        }
    }










}
