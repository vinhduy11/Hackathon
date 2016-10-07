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

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * Created by concu on 9/19/14.
 */
public class TrackGenCodeDb {

    EventBus eventBus;
    Logger logger;

    public TrackGenCodeDb(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    public static class Obj{

        public int ITEE_NUMBER=0;
        public String ITEE_CODE="";
        public String ITEE_DESC="";
        public String ITEE_REGISTER_DATE="";
        public int ITEE_PROCOUNT=0;

        public int ITER_NUMBER=0;
        public String ITER_CODE="";
        public String ITER_DESC="";
        public String ITER_REGISTER_DATE="";
        public int ITER_PROCOUNT=0;


        public long TIME=System.currentTimeMillis();
        public long AMOUNT=0;
        public String TRAN_TYPE="";

        public Obj(){}
        public Obj(JsonObject jo){

            ITEE_NUMBER = jo.getInteger(colName.TrackGenCodeCols.ITEE_NUMBER);
            ITEE_CODE = jo.getString(colName.TrackGenCodeCols.ITEE_CODE);
            ITEE_DESC = jo.getString(colName.TrackGenCodeCols.ITEE_DESC);
            ITEE_REGISTER_DATE=jo.getString(colName.TrackGenCodeCols.ITEE_REGISTER_DATE);
            ITEE_PROCOUNT=jo.getInteger(colName.TrackGenCodeCols.ITEE_PROCOUNT);

            ITER_NUMBER = jo.getInteger(colName.TrackGenCodeCols.ITER_NUMBER);
            ITER_CODE = jo.getString(colName.TrackGenCodeCols.ITER_CODE);
            ITER_DESC = jo.getString(colName.TrackGenCodeCols.ITER_DESC);
            ITER_REGISTER_DATE=jo.getString(colName.TrackGenCodeCols.ITER_REGISTER_DATE);
            ITER_PROCOUNT=jo.getInteger(colName.TrackGenCodeCols.ITER_PROCOUNT);

            TIME = jo.getLong(colName.TrackGenCodeCols.TIME);
            AMOUNT = jo.getLong(colName.TrackGenCodeCols.AMOUNT);
            TRAN_TYPE = jo.getString(colName.TrackGenCodeCols.TRAN_TYPE);
        }

        public JsonObject toJSON(){
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.TrackGenCodeCols.ITEE_NUMBER,ITEE_NUMBER);
            jo.putString(colName.TrackGenCodeCols.ITEE_CODE, ITEE_CODE);
            jo.putString(colName.TrackGenCodeCols.ITEE_DESC, ITEE_DESC);
            jo.putString(colName.TrackGenCodeCols.ITEE_REGISTER_DATE, ITEE_REGISTER_DATE);
            jo.putNumber(colName.TrackGenCodeCols.ITEE_PROCOUNT, ITEE_PROCOUNT);

            jo.putNumber(colName.TrackGenCodeCols.ITER_NUMBER,ITER_NUMBER);
            jo.putString(colName.TrackGenCodeCols.ITER_CODE, ITER_CODE);
            jo.putString(colName.TrackGenCodeCols.ITER_DESC, ITER_DESC);
            jo.putString(colName.TrackGenCodeCols.ITER_REGISTER_DATE, ITER_REGISTER_DATE);
            jo.putNumber(colName.TrackGenCodeCols.ITER_PROCOUNT, ITER_PROCOUNT);

            jo.putNumber(colName.TrackGenCodeCols.TIME,TIME);
            jo.putNumber(colName.TrackGenCodeCols.AMOUNT,AMOUNT);
            jo.putString(colName.TrackGenCodeCols.TRAN_TYPE, TRAN_TYPE);
            return jo;
        }
    }

    public void save(Obj obj, final Handler<Boolean> callback){
        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.TrackGenCodeCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJSON());

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

    public void find(int iteeNumber, int iterNumber, long ldate, final Handler<JsonArray> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoTrackCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();

        if(iteeNumber > 0){
            match.putNumber(colName.TrackGenCodeCols.ITEE_NUMBER,iteeNumber);
        }

        if(iterNumber>0){
            match.putNumber(colName.TrackGenCodeCols.ITER_NUMBER,iterNumber);
        }

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
        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY,null);
                callback.handle(array);

            }
        });
    }







}
