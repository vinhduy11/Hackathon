package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 9/26/14.
 */
public class RegImeiDb {

    private EventBus eventBus;
    private Logger logger;

    public RegImeiDb(EventBus eventBus, Logger logger){
        this.eventBus = eventBus;
        this.logger = logger;
    }

    public void upsert(final Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.RegImeiCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.RegImeiCols.id, obj.id);
        query.putObject(MongoKeyWords.MATCHER, match);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject objNew = obj.toJSON();

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                //todo
                boolean result  = "ok".equalsIgnoreCase(event.body().getString(MongoKeyWords.STATUS,""));
                callback.handle(result);
            }
        });
    }

    public void findOne(final String imei, final Handler<Obj> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.RegImeiCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.RegImeiCols.id, imei);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json = event.body().getObject(MongoKeyWords.RESULT, null);
                Obj obj = null;
                if (json != null) {
                    obj = new Obj(json);
                }

                callback.handle(obj);
            }
        });

    }

    public static class Obj{
        public  String id ="";
        public  int count =0;
        public  long time =0;

        public ArrayList<Integer> arrayListPhones = new ArrayList<>();

        public Obj(){}

        public Obj(JsonObject jo){
            id =jo.getString(colName.RegImeiCols.id,"");
            count =jo.getInteger(colName.RegImeiCols.count, 0);
            time =jo.getLong(colName.RegImeiCols.time, 0);
            String listPhones = jo.getString(colName.RegImeiCols.listPhone,"");

            if(listPhones!= null && !"".equalsIgnoreCase(listPhones)){
                String[] arrPhones = listPhones.split(",");
                for(int i = 0; i< arrPhones.length;i++){
                    int val = DataUtil.strToInt(arrPhones[i]);
                    if(val>0){
                        arrayListPhones.add(val);
                    }
                }
            }
        }

        public JsonObject toJSON(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.RegImeiCols.id, id);
            jo.putNumber(colName.RegImeiCols.count,count);
            jo.putNumber(colName.RegImeiCols.time,time);
            String str = "";
            if(arrayListPhones!= null && arrayListPhones.size() > 0){
                for(int i = 0 ; i< arrayListPhones.size(); i ++){
                    str+= arrayListPhones.get(i) + ",";
                }
            }

            if(!"".equalsIgnoreCase(str)){
                str.substring(0, str.length() -1);
            }

            jo.putString(colName.RegImeiCols.listPhone,str);

            return jo;
        }
    }

}
