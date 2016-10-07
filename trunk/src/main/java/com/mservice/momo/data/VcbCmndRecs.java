package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
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
public class VcbCmndRecs {

    public static class Obj{

        public String cardid     =   "";
        public String bankcode   =   "";
        public String timevn     =   Misc.dateVNFormatWithTime(System.currentTimeMillis());
        public String number     =   "";
        public int promocount    =   0;

        public Obj(JsonObject jo){
            cardid = jo.getString(colName.VcbCmndCols.cardid,"");
            bankcode = jo.getString(colName.VcbCmndCols.bankcode,"");
            timevn = jo.getString(colName.VcbCmndCols.timevn,"");
            number = jo.getString(colName.VcbCmndCols.number,"");
            promocount = jo.getInteger(colName.VcbCmndCols.promocount,0);

        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.VcbCmndCols.cardid,cardid);
            jo.putString(colName.VcbCmndCols.bankcode,bankcode);
            jo.putString(colName.VcbCmndCols.timevn,timevn);
            jo.putString(colName.VcbCmndCols.number,number);
            jo.putNumber(colName.VcbCmndCols.promocount,promocount);

            return jo;
        }
    }
    private EventBus eventBus;
    private Logger logger;

    public VcbCmndRecs(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.VcbCmndCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
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

    public void findOne(String cardid, final Handler<Obj> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();
        matcher.putString(colName.VcbCmndCols.cardid,cardid);

        query.putObject(MongoKeyWords.MATCHER,matcher);
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCmndCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {
                Obj obj =null;
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonObject jo = jom.body().getObject(MongoKeyWords.RESULT, null);
                    if(jo !=null){
                        obj = new Obj(jo);
                    }
                }
                callback.handle(obj);
            }
        });
    }

    public void findByPhone(String phoneNumber, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();
        matcher.putString(colName.VcbCmndCols.number,phoneNumber);

        query.putObject(MongoKeyWords.MATCHER,matcher);
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCmndCols.table);
        query.putNumber(MongoKeyWords.BATCH_SIZE,1000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {
                ArrayList<Obj> arrayList = new ArrayList<>();

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray jsonArray = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    if(jsonArray != null && jsonArray.size() > 0){
                        for(int i=0;i< jsonArray.size(); i++){
                            JsonObject jo = jsonArray.get(i);
                            arrayList.add(new Obj(jo));
                        }
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void findAll( final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        JsonObject ne = new JsonObject();
        ne.putString(MongoKeyWords.NOT_EQUAL,"core");

        matcher.putObject(colName.VcbCmndCols.number,ne);

        query.putObject(MongoKeyWords.MATCHER,matcher);
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCmndCols.table);
        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {
                ArrayList<Obj> arrayList = new ArrayList<>();

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray jsonArray = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    if(jsonArray != null && jsonArray.size() > 0){
                        for(int i=0;i< jsonArray.size(); i++){
                            JsonObject jo = jsonArray.get(i);
                            arrayList.add(new Obj(jo));
                        }
                    }
                }

                callback.handle(arrayList);
            }
        });
    }




    public void findOneByPhoneNumber(String phoneNumber, final Handler<Obj> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();
        matcher.putString(colName.VcbCmndCols.number,phoneNumber);

        query.putObject(MongoKeyWords.MATCHER,matcher);

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCmndCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {
                Obj obj = null;
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonObject jo = jom.body().getObject(MongoKeyWords.RESULT, null);
                    if(jo != null){
                        obj = new Obj(jo);
                    }
                }
                callback.handle(obj);
            }
        });
    }


    public void incProCount(String cardid, int delta, final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putNumber(colName.VcbCmndCols.promocount,delta);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCmndCols.table);
        JsonObject match   = new JsonObject();

        match.putString(colName.VcbCmndCols.cardid, cardid);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, set);

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
}
