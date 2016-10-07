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
public class FSCRecords {

    public static class Obj{
        public String code ="_id";
        public int number =0;
        public String name ="name";
        public long time =System.currentTimeMillis();
        public String timevn = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        public String mssv ="";
        public int status =0;

        public Obj(JsonObject jo){
            code =jo.getString(colName.FSCRecCols.code,"");
            number =jo.getInteger(colName.FSCRecCols.number,0);
            name =jo.getString(colName.FSCRecCols.name,"");
            time =jo.getLong(colName.FSCRecCols.time, 0);
            timevn =jo.getString(colName.FSCRecCols.timevn,"");
            mssv =jo.getString(colName.FSCRecCols.mssv,"");
            status =jo.getInteger(colName.FSCRecCols.status,0);
        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.FSCRecCols.code,code);
            jo.putNumber(colName.FSCRecCols.number, number);
            jo.putString(colName.FSCRecCols.name, name);
            jo.putNumber(colName.FSCRecCols.time, time);
            jo.putString(colName.FSCRecCols.timevn, timevn);
            jo.putString(colName.FSCRecCols.mssv, mssv);
            jo.putNumber(colName.FSCRecCols.status,status);
            return jo;
        }
    }
    private EventBus eventBus;
    private Logger logger;

    public FSCRecords(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT );
        query.putString(MongoKeyWords.COLLECTION, colName.FSCRecCols.table);
        query.putObject(MongoKeyWords.DOCUMENT,obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                int rcode = 0;
                if(!event.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonObject error = new JsonObject(event.body().getString("message","{code:-1}"));
                    rcode = error.getInteger("code",-1);
                }
                callback.handle(rcode);
            }
        });
    }

    public void find(String code,int number,long fromdate,long todate, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        if(!"".equalsIgnoreCase(code)){
            matcher.putString(colName.FSCRecCols.code, code);
        }
        if(number > 0){
            matcher.putNumber(colName.FSCRecCols.number,number);
        }

        if(fromdate > 0 && todate > 0){
            JsonObject between = new JsonObject();
            between.putNumber(MongoKeyWords.GREATER_OR_EQUAL,fromdate);
            between.putNumber(MongoKeyWords.LESS_OR_EQUAL,todate);
            matcher.putObject(colName.FSCRecCols.time, between);
        }
        if(matcher.getFieldNames().size() >0){
            query.putObject(MongoKeyWords.MATCHER,matcher);
        }

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.FSCRecCols.table);
        query.putNumber(MongoKeyWords.BATCH_SIZE,10000);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray array = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    if(array !=null && array.size() >0){
                        for (Object o : array){
                            arrayList.add( new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void search(String code,int number,long fromdate,long todate, final Handler<JsonArray> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.FSCRecCols.table);
        if(!"".equalsIgnoreCase(code)){
            matcher.putString(colName.FSCRecCols.code, code);
        }
        if(number > 0){
            matcher.putNumber(colName.FSCRecCols.number,number);
        }

        if(fromdate > 0 && todate > 0){
            JsonObject between = new JsonObject();
            between.putNumber(MongoKeyWords.GREATER_OR_EQUAL,fromdate);
            between.putNumber(MongoKeyWords.LESS_OR_EQUAL,todate);
            matcher.putObject(colName.FSCRecCols.time, between);
        }
        if(matcher.getFieldNames().size() >0){
            query.putObject(MongoKeyWords.MATCHER,matcher);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE,10000);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                JsonArray array = new JsonArray();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray array1 = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    if(array !=null){
                        array = array1;
                    }
                }
                callback.handle(array);
            }
        });
    }

}
