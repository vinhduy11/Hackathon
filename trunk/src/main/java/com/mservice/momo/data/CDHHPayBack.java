package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 4/18/14.
 */
public class CDHHPayBack {

    public static class Obj{
        public String number ="";               //icon loai giao dich vu co su dung voucher
        public long voteAmount =0;          //bat tat dich vu su dung vourcher
        public String serviceId = "";

        public Obj(JsonObject jo){
            number = jo.getString(colName.CDHHPayBackCols.number,"");
            voteAmount =jo.getLong(colName.CDHHPayBackCols.voteAmount,0);
        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.CDHHPayBackCols.number,number);
            jo.putNumber(colName.CDHHPayBackCols.voteAmount,voteAmount);
            return jo;
        }

    }
    private EventBus eventBus;
    private Logger logger;

    public CDHHPayBack(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void save(Obj obj , final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.CDHHPayBackCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                boolean result = false;
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if (createdId != null) {
                        result = true;
                    }
                }
                callback.handle(result);
            }
        });
    }

    public void getRecByList(ArrayList<String> list, String serviceId, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);

        String table = (serviceId.equalsIgnoreCase("capdoihoanhao") ? colName.CDHHPayBackCols.table
                : colName.CDHHPayBackCols.table + "_" + serviceId);

        query.putString(MongoKeyWords.COLLECTION, table);
        JsonObject match   = new JsonObject();

        JsonArray array = new JsonArray();
        for(int i=0;i< list.size();i++){
            array.add(list.get(i));
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$,array);

        match.putObject(colName.CDHHPayBackCols.number, in);
        query.putObject(MongoKeyWords.MATCHER, match);
        query.putNumber(MongoKeyWords.BATCH_SIZE,list.size());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonArray resultArr = jsonObjectMessage.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                ArrayList<Obj> arrayList = new ArrayList<Obj>();
                if(resultArr != null && resultArr.size() >0){
                    for (int i=0;i< resultArr.size();i++){
                        Obj o = new Obj((JsonObject)resultArr.get(i));
                        arrayList.add(o);
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void incVotedAmount(String phoneNumber, String serviceId, int incCount,final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putNumber(colName.CDHHPayBackCols.voteAmount,incCount);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        String table = (serviceId.equalsIgnoreCase("capdoihoanhao") ? colName.CDHHPayBackCols.table
                : colName.CDHHPayBackCols.table + "_" + serviceId);


        query.putString(MongoKeyWords.COLLECTION, table);
        JsonObject match   = new JsonObject();

        match.putString(colName.CDHHPayBackCols.number, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void updatePaidBackTickets(final String number,final long delta, final Handler<Boolean> callback){

        //updatePartial(number, objNew, callback);
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHPayBackCols.table);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CDHHPayBackCols.number, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.CDHHPayBackCols.voteAmount,delta);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, objNew);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }

    public void getVotedAmount(String  phoneNumber, String serviceId, final Handler<Integer> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);

        String table = (serviceId.equalsIgnoreCase("capdoihoanhao") ? colName.CDHHPayBackCols.table
                : colName.CDHHPayBackCols.table + "_" + serviceId);

        query.putString(MongoKeyWords.COLLECTION, table);

        //matcher
        match.putString(colName.CDHHPayBackCols.number, phoneNumber);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                int count = 0;
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonObject result = jom.body().getObject(MongoKeyWords.RESULT, null);
                    if(result != null){
                        count = result.getInteger(colName.CDHHPayBackCols.voteAmount,0);
                    }
                }
                callback.handle(count);
            }
        });
    }

}
