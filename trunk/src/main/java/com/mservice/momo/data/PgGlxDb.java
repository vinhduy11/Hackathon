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

public class PgGlxDb {
    EventBus eventBus;
    Logger logger;

    public PgGlxDb(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    public static class Obj{
        public  String pgcode="";
        public  String number="";
        public  long value=0;
        public  long time=System.currentTimeMillis();
        public  String tranname ="";
        public  String timevn=Misc.dateVNFormatWithTime(System.currentTimeMillis());
        public  String serviceid = "";

        public Obj(){}
        public Obj(JsonObject jo){

            pgcode = jo.getString(colName.PgGalaxyHisCols.pgcode, "");
            number = jo.getString(colName.PgGalaxyHisCols.number, "");
            value = jo.getLong(colName.PgGalaxyHisCols.value, 0);
            time = jo.getLong(colName.PgGalaxyHisCols.time, 0);
            tranname = jo.getString(colName.PgGalaxyHisCols.tranname, "");
            timevn= jo.getString(colName.PgGalaxyHisCols.timevn, "");
            serviceid = jo.getString(colName.PgGalaxyHisCols.serviceid,"");
        }


        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.PgGalaxyHisCols.pgcode,pgcode);
            jo.putString(colName.PgGalaxyHisCols.number, number);
            jo.putNumber(colName.PgGalaxyHisCols.value, value);
            jo.putNumber(colName.PgGalaxyHisCols.time, time);
            jo.putString(colName.PgGalaxyHisCols.tranname,tranname);
            jo.putString(colName.PgGalaxyHisCols.timevn,timevn);
            jo.putString(colName.PgGalaxyHisCols.serviceid,serviceid);
            return jo;
        }
    }

    public void save(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.PgGalaxyHisCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                boolean result = false;
                if (event.body() != null && event.body().getString(MongoKeyWords.UPSERTED_ID, null) != null) {
                    result = true;
                }
                callback.handle(result);
            }
        });
    }

    public void findOne(final String number, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PgGalaxyHisCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.PgGalaxyHisCols.number, number);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj o = null;
                JsonObject jo = event.body().getObject(MongoKeyWords.RESULT, null);
                if(jo!= null){
                    o = new Obj(jo);
                }
                callback.handle(o);
            }
        });
    }


    public void find(final String pgcode, final String number, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PgGalaxyHisCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.PgGalaxyHisCols.pgcode, pgcode);
        match.putString(colName.PgGalaxyHisCols.number, number);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> list = new ArrayList<Obj>();
                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);

                if(array != null && array.size() > 0){
                    for (Object o : array){
                        list.add( new Obj ((JsonObject)o));
                    }
                }

                callback.handle(list);
            }
        });
    }

    public void get(final String pgcode
                                    ,final String number
                                    ,long fromTime
                                    ,long toTime,final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PgGalaxyHisCols.table);

        //matcher
        JsonObject match = new JsonObject();

        if(!"0".equalsIgnoreCase(number) && !"".equalsIgnoreCase(number))
        {
            match.putString(colName.PgGalaxyHisCols.number, number);
        }

        if(!"".equalsIgnoreCase(pgcode))
        {
            match.putString(colName.PgGalaxyHisCols.pgcode, pgcode);
        }

        if(fromTime > 0 && toTime > 0){
            JsonObject jo = new JsonObject();
            jo.putNumber(MongoKeyWords.GREATER_OR_EQUAL, fromTime)
                    .putNumber(MongoKeyWords.LESS_OR_EQUAL,toTime);
            match.putObject(colName.PgGalaxyHisCols.time, jo);
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
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
}
