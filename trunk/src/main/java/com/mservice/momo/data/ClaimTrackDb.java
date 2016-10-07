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
public class ClaimTrackDb {

    EventBus eventBus;
    Logger logger;

    public ClaimTrackDb(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    public static int STATUS_NEW = 0;
    public static int STATUS_LOCKED = 1;
    public static int STATUS_CLAIMED = 2;
    public static int STATUS_EXPIRED = 3;

    public static class Obj{
        public String CLAIM_CODE =  "code";
        public int NUMBER   = 0;
        public long TIME    = 0;
        public String TIME_VN = "";
        public int STATUS = -1;
        public int PARTNER = 0;
        public int ERROR =  -1;
        public String DESCRIPTION   = "";
        public int EXEC_NUMBER      = 0;
        public String EXEC_TIME     = "";

        public Obj(){}
        public Obj(JsonObject jo){

            CLAIM_CODE =  jo.getString(colName.ClaimPointCols.CLAIM_CODE,"");
            NUMBER =  jo.getInteger(colName.ClaimPointCols.NUMBER,0);
            TIME = jo.getLong(colName.ClaimPointCols.TIME,0);
            TIME_VN = jo.getString(colName.ClaimPointCols.TIME_VN,"");
            STATUS = jo.getInteger(colName.ClaimPointCols.STATUS,-1);
            ERROR =  jo.getInteger(colName.ClaimPointCols.ERROR,-1);
            DESCRIPTION = jo.getString(colName.ClaimPointCols.DESCRIPTION,"");
            EXEC_NUMBER = jo.getInteger(colName.ClaimPointCols.EXEC_NUMBER,0);
            EXEC_TIME = jo.getString(colName.ClaimPointCols.EXEC_TIME,"");
            PARTNER = jo.getInteger(colName.ClaimPointCols.PARTNER,0);
        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.ClaimPointCols.CLAIM_CODE,this.CLAIM_CODE);
            jo.putNumber(colName.ClaimPointCols.NUMBER, this.NUMBER);
            jo.putNumber(colName.ClaimPointCols.TIME,this.TIME);
            jo.putString(colName.ClaimPointCols.TIME_VN,this.TIME_VN);

            jo.putNumber(colName.ClaimPointCols.STATUS, this.STATUS);
            jo.putNumber(colName.ClaimPointCols.ERROR, this.ERROR);
            jo.putString(colName.ClaimPointCols.DESCRIPTION,this.DESCRIPTION);

            jo.putNumber(colName.ClaimPointCols.EXEC_NUMBER,EXEC_NUMBER);
            jo.putString(colName.ClaimPointCols.EXEC_TIME,EXEC_TIME);

            jo.putNumber(colName.ClaimPointCols.PARTNER,PARTNER);

            return jo;
        }
    }

    public void save(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.ClaimPointCols.TABLE)
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
        query.putString(MongoKeyWords.COLLECTION, colName.ClaimPointCols.TABLE);

        //matcher
        match.putString(colName.ClaimPointCols.CLAIM_CODE, code);

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

    public void getAllByNumber(final int number, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ClaimPointCols.TABLE);

        //matcher
        match.putNumber(colName.ClaimPointCols.NUMBER, number);

        query.putObject(MongoKeyWords.MATCHER,match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000);

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

    public void getAllByPartner(final int partner, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ClaimPointCols.TABLE);

        //matcher
        match.putNumber(colName.ClaimPointCols.PARTNER, partner);

        query.putObject(MongoKeyWords.MATCHER,match);

        query.putNumber(MongoKeyWords.BATCH_SIZE,10);

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
        query.putString(MongoKeyWords.COLLECTION, colName.ClaimPointCols.TABLE);

        //matcher
        match.putString(colName.ClaimPointCols.CLAIM_CODE
                ,jsonNew.getString(colName.ClaimPointCols.CLAIM_CODE,""));


        //trang thai muon cap nhat la tu new -> locked
        int status = jsonNew.getInteger(colName.ClaimPointCols.STATUS,STATUS_NEW);
        if(status == STATUS_LOCKED){
            match.putNumber(colName.ClaimPointCols.STATUS, STATUS_NEW);
        }

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jsonNew);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);

        //neu khong co thi khong insert dong moi vao DB
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
        query.putString(MongoKeyWords.COLLECTION, colName.ClaimPointCols.TABLE);

        JsonArray array = new JsonArray();

        for(String s : listCode){
            array.add(s.trim());
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, array);

        //matcher
        match.putObject(colName.ClaimPointCols.CLAIM_CODE, in);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject status = new JsonObject();
        status.putNumber(colName.ClaimPointCols.STATUS,inputStatus);

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

    public void doStatistic(long ldate, final Handler<JsonObject> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ClaimPointCols.TABLE);

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

            match.putObject(colName.ClaimPointCols.TIME, dk);
        }


        Set<String> set = match.getFieldNames();
        if(set.size()>0){
            query.putObject(MongoKeyWords.MATCHER,match);
        }

        //fields not returned to client
        JsonObject fields = new JsonObject();
        fields.putNumber("_id",0);
        fields.putNumber(colName.ClaimPointCols.STATUS, 1);
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

                        if(status == STATUS_CLAIMED){
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
