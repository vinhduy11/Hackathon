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
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * Created by concu on 9/11/14.
 */
public class PromoTrackDb {

    EventBus eventBus;
    Logger logger;

    public PromoTrackDb(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    public static int STATUS_NEW = 0;
    public static int STATUS_PROMOTED = 1;
    public static int STATUS_LOCKED = 2;
    public static int STATUS_EXPIRED = 3;

    public static class Obj{
        public String PROMO_CODE =  "";
        public int NUMBER =  0;
        public long TIME = 0;
        public String TIME_VN ="";
        public int STATUS = 0; // failed
        public int ERROR =  0;
        public String DESCRIPTION = "";
        public int EXEC_NUMBER=0;
        public String EXEC_TIME="";
        public long LAST_EXEC_TIME=0;
        public int PARTNER=0;

        public Obj(){}
        public Obj(JsonObject jo){

            PROMO_CODE =  jo.getString(colName.PromoTrackCols.PROMO_CODE,"");
            NUMBER =  jo.getInteger(colName.PromoTrackCols.NUMBER,0);
            TIME = jo.getLong(colName.PromoTrackCols.TIME,0);
            TIME_VN = jo.getString(colName.PromoTrackCols.TIME_VN,"");
            STATUS = jo.getInteger(colName.PromoTrackCols.STATUS,-1);
            ERROR =  jo.getInteger(colName.PromoTrackCols.ERROR,-1);
            DESCRIPTION = jo.getString(colName.PromoTrackCols.DESCRIPTION,"");
            EXEC_NUMBER = jo.getInteger(colName.PromoTrackCols.EXEC_NUMBER,0);
            EXEC_TIME = jo.getString(colName.PromoTrackCols.EXEC_TIME,"");
            PARTNER = jo.getInteger(colName.PromoTrackCols.PARTNER,0);
            LAST_EXEC_TIME = jo.getLong(colName.PromoTrackCols.EXEC_TIME_LAST,0);
        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.PromoTrackCols.PROMO_CODE,this.PROMO_CODE);
            jo.putNumber(colName.PromoTrackCols.NUMBER, this.NUMBER);
            jo.putNumber(colName.PromoTrackCols.TIME,this.TIME);
            jo.putString(colName.PromoTrackCols.TIME_VN,this.TIME_VN);

            jo.putNumber(colName.PromoTrackCols.STATUS, this.STATUS);
            jo.putNumber(colName.PromoTrackCols.ERROR, this.ERROR);
            jo.putString(colName.PromoTrackCols.DESCRIPTION,this.DESCRIPTION);

            jo.putNumber(colName.PromoTrackCols.EXEC_NUMBER,EXEC_NUMBER);
            jo.putString(colName.PromoTrackCols.EXEC_TIME,EXEC_TIME);

            jo.putNumber(colName.PromoTrackCols.PARTNER,PARTNER);
            jo.putNumber(colName.PromoTrackCols.EXEC_TIME_LAST,LAST_EXEC_TIME);

            return jo;
        }
    }

    public void save(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE)
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

    public void getOne(final String code, final Handler<Obj> callback){


        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        match.putString(colName.PromoTrackCols.PROMO_CODE, code);

        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Obj obj = null;
                if(event.body() != null && event.body().getObject(MongoKeyWords.RESULT,null)!= null ){
                    obj = new Obj(event.body().getObject(MongoKeyWords.RESULT,null));
                }
                callback.handle(obj);
            }
        });
    }

    public void findOne(final String code, int phoneNumber, final Handler<Obj> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        match.putString(colName.PromoTrackCols.PROMO_CODE, code);
        match.putNumber(colName.PromoTrackCols.NUMBER,phoneNumber);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Obj obj = null;
                if(event.body() != null && event.body().getObject(MongoKeyWords.RESULT,null)!= null ){
                    obj = new Obj(event.body().getObject(MongoKeyWords.RESULT,null));
                }
                callback.handle(obj);
            }
        });
    }

    public void getLatestByLastExecTime(final int execNumber, int limit, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.PromoTrackCols.EXEC_TIME_LAST, -1);

        query.putObject(MongoKeyWords.SORT,sort);

        match.putNumber(colName.PromoTrackCols.EXEC_NUMBER, execNumber);
        query.putObject(MongoKeyWords.MATCHER,match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,limit);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Obj obj = null;
                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);

                ArrayList<Obj> arrayList = null;

                logger.info("array.size " +  array.size());
                if(event.body() != null && array != null ){
                    arrayList = new ArrayList<Obj>();
                    for(int i = 0; i<array.size(); i ++) {
                        Obj tmp = new Obj((JsonObject) array.get(i));
                        arrayList.add(tmp);

                    }
                }
                callback.handle(arrayList);
            }
        });
    }


    public void getAllByNumber(final int number, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        match.putNumber(colName.PromoTrackCols.NUMBER, number);

        query.putObject(MongoKeyWords.MATCHER,match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = null;
                if(event.body() != null && event.body().getArray(MongoKeyWords.RESULT_ARRAY, null)!= null ){

                    JsonArray arr = event.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    arrayList = new ArrayList<>();
                    for(Object o : arr){
                        arrayList.add(new Obj((JsonObject)o));
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void getByNumber(final int number, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        match.putNumber(colName.PromoTrackCols.PARTNER, number);

        query.putObject(MongoKeyWords.MATCHER,match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,2);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = null;
                if(event.body() != null && event.body().getArray(MongoKeyWords.RESULT_ARRAY, null)!= null ){

                    JsonArray arr = event.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    arrayList = new ArrayList<>();
                    for(Object o : arr){
                        arrayList.add(new Obj((JsonObject)o));
                    }
                }
                callback.handle(arrayList);
            }
        });
    }



    public void update(final JsonObject jsonNew, final Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        match.putString(colName.PromoTrackCols.PROMO_CODE
                ,jsonNew.getString(colName.PromoTrackCols.PROMO_CODE,""));


        //truong hop cap nhat trang thai lock
        int status = jsonNew.getInteger(colName.PromoTrackCols.STATUS,STATUS_NEW);
        if(status == STATUS_LOCKED){
            match.putNumber(colName.PromoTrackCols.STATUS, STATUS_NEW);
        }

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jsonNew);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);

        //query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }

    public void reactiveCode(String[] listCode, int inputStatus, final Handler<JsonObject> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        JsonArray array = new JsonArray();

        for(String s : listCode){
            array.add(s.trim());
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, array);

        //matcher
        match.putObject(colName.PromoTrackCols.PROMO_CODE, in);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject status = new JsonObject();
        status.putNumber(colName.PromoTrackCols.STATUS,inputStatus);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, status);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);

        //query.putBoolean(MongoKeyWords.UPSERT, true);

        query.putBoolean(MongoKeyWords.MULTI,true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject jsonObject = jsonObjectMessage.body();
                callback.handle(jsonObject);
            }
        });

    }

    public void getClaimedCount(final int number, final Handler<Integer> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.COUNT);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        match.putNumber(colName.PromoTrackCols.EXEC_NUMBER, number);

        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int count = 0;
                if(event.body() != null && event.body().getString(MongoKeyWords.STATUS, "")== "ok" ){
                    count = event.body().getInteger("count",0);
                }

                callback.handle(count);
            }
        });

    }

    public void doStatistic(long ldate, final Handler<JsonObject> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();

        if(ldate >0){

            Date inputDate = new Date(ldate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(inputDate);

            long sDate = Misc.getDate(calendar.get(Calendar.YEAR)
                    , calendar.get(Calendar.MONTH) + 1
                    , calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

            long eDate = Misc.getDate(calendar.get(Calendar.YEAR)
                    , calendar.get(Calendar.MONTH) + 1
                    , calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);

            JsonObject dk = new JsonObject();
            dk.putNumber(MongoKeyWords.GREATER_OR_EQUAL, sDate);
            dk.putNumber(MongoKeyWords.LESS_OR_EQUAL,eDate);

            match.putObject(colName.PromoTrackCols.TIME, dk);
        }


        Set<String> set = match.getFieldNames();
        if(set.size()>0){
            query.putObject(MongoKeyWords.MATCHER,match);
        }

        //fields not returned to client
        JsonObject fields = new JsonObject();
        fields.putNumber("_id",0);
        fields.putNumber(colName.PromoTrackCols.STATUS, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int inew = 0;
                int ilock =0;
                int ipromoted =0;
                int iexpired = 0;
                int total = 0;

                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY,null);
                if(array != null){
                    total = array.size();
                    for(Object o : array){

                        JsonObject jo  = (JsonObject)o;
                        int status = jo.getInteger(colName.PromoTrackCols.STATUS,0);
                        if(status == STATUS_NEW){
                            inew +=1;
                            continue;
                        }

                        if(status == STATUS_PROMOTED){
                            ipromoted +=1;
                            continue;
                        }

                        if(status == STATUS_LOCKED){
                            ilock +=1;
                            continue;
                        }

                        if(status == STATUS_EXPIRED){
                            iexpired +=1;
                            continue;
                        }
                    }
                }

                JsonObject jresult = new JsonObject();
                jresult.putNumber("total",total);
                jresult.putNumber("new",inew);
                jresult.putNumber("promoted",ipromoted);
                jresult.putNumber("locked",ilock);
                jresult.putNumber("expired",iexpired);

                callback.handle(jresult);
            }
        });
    }
}
