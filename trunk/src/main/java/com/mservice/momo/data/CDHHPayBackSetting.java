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
public class CDHHPayBackSetting {

    public static class Obj{
        public boolean status =false; // on/off
        public int delaytime =0; // in secs
        public String id ="";
        public String paybackaccount ="";
        public int paybackmax =0;
        public String serviceid = "";

        /*"pay_back_account":"0974540385", // new
                "pay_back_max":3, // new
                "delay_pay_back_time" :1, //phut*/


        public Obj(JsonObject jo){
            status = jo.getBoolean(colName.CDHHPayBackSettingCols.status,false);
            delaytime =jo.getInteger(colName.CDHHPayBackSettingCols.delaytime,0);
            id = jo.getString(colName.CDHHPayBackSettingCols.id,"");
            paybackaccount = jo.getString(colName.CDHHPayBackSettingCols.paybackaccount,"");
            paybackmax = jo.getInteger(colName.CDHHPayBackSettingCols.paybbackmax,0);
            serviceid = jo.getString(colName.CDHHPayBackSettingCols.serviceid,"");

        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putBoolean(colName.CDHHPayBackSettingCols.status, status);
            jo.putNumber(colName.CDHHPayBackSettingCols.delaytime, delaytime);
            jo.putString(colName.CDHHPayBackSettingCols.id,id);
            jo.putString(colName.CDHHPayBackSettingCols.paybackaccount,paybackaccount);
            jo.putNumber(colName.CDHHPayBackSettingCols.paybbackmax,paybackmax);
            jo.putString(colName.CDHHPayBackSettingCols.serviceid,serviceid);

            return jo;
        }
    }
    private EventBus eventBus;
    private Logger logger;

    public CDHHPayBackSetting(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void saveOrUpdate(Obj obj,final Handler<Boolean> callback){

        if(obj.id != null &&  !"".equalsIgnoreCase(obj.id)){
            update(obj,callback);
            return;
        }

        JsonObject jo = obj.toJson();
        jo.removeField(colName.CDHHPayBackSettingCols.id);

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.CDHHPayBackSettingCols.table)
                .putObject(MongoKeyWords.DOCUMENT, jo);

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

    public  void update(Obj ob, final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = ob.toJson();
        newJsonObj.removeField(colName.CDHHPayBackSettingCols.id);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHPayBackSettingCols.table);

        JsonObject match   = new JsonObject();
        match.putString(colName.CDHHPayBackSettingCols.id, ob.id);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //fields set
        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, newJsonObj);

        //set
        query.putBoolean(MongoKeyWords.UPSERT, false);

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

    public void findOne(final Handler<Obj> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHPayBackSettingCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                Obj obj = new Obj();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonObject result = jom.body().getObject(MongoKeyWords.RESULT, null);
                    if(result != null){
                        obj = new Obj(result);
                    }
                }

                callback.handle(obj);
            }
        });
    }

    public void findAll(final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHPayBackSettingCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray array = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    if(array!=null && array.size() > 0){
                        for (int i=0;i< array.size();i++){
                            Obj obj = new Obj((JsonObject) array.get(i));
                            arrayList.add(obj);
                        }
                    }
                }

                callback.handle(arrayList);
            }
        });
    }


}
