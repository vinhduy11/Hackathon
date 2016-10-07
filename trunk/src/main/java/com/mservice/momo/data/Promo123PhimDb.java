package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
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
public class Promo123PhimDb {

    EventBus eventBus;
    Logger logger;

    public Promo123PhimDb(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    //public static long BeginDate = CoreCommon.getDate(2014,9,15,0,0,0);
    //public static long EndDate = CoreCommon.getDate(2014,10,31,23,59,59);

    public static class Obj{
        public int ID = 0;
        public String INVOICE_NO="";
        public String TICKET_CODE="";
        public String FILM_NAME="";
        public String RAP="";
        public String DISPLAY_TIME="";
        public String BUY_TIME="";
        public String SEAT_LIST="";
        public int NUMBER_OF_SEAT=0;
        public String PROMO_CODE="";
        public long TIME= System.currentTimeMillis();
        public int PROMO_COUNT = 0;
        public long PROMO_TIME = 0;
        public Obj(){}
        public Obj(JsonObject jo){

            ID = DataUtil.strToInt(jo.getString(colName.Phim123PromoCols.ID,"")) ;
            INVOICE_NO= jo.getString(colName.Phim123PromoCols.INVOICE_NO, "");
            TICKET_CODE = jo.getString(colName.Phim123PromoCols.TICKET_CODE,"");
            FILM_NAME=jo.getString(colName.Phim123PromoCols.FILM_NAME,"");
            RAP=jo.getString(colName.Phim123PromoCols.RAP,"");
            DISPLAY_TIME=jo.getString(colName.Phim123PromoCols.DISPLAY_TIME,"");
            BUY_TIME=jo.getString(colName.Phim123PromoCols.BUY_TIME,"");
            SEAT_LIST=jo.getString(colName.Phim123PromoCols.SEAT_LIST,"");
            NUMBER_OF_SEAT=jo.getInteger(colName.Phim123PromoCols.NUMBER_OF_SEAT, 0);
            PROMO_CODE=jo.getString(colName.Phim123PromoCols.PROMO_CODE,"");
            TIME= jo.getLong(colName.Phim123PromoCols.TIME, 0);
            PROMO_COUNT= jo.getInteger(colName.Phim123PromoCols.PROMO_COUNT, 0);
            PROMO_TIME= jo.getLong(colName.Phim123PromoCols.PROMO_TIME, 0);
        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();

            jo.putNumber(colName.Phim123PromoCols.ID,ID);
            jo.putString(colName.Phim123PromoCols.INVOICE_NO, INVOICE_NO);
            jo.putString(colName.Phim123PromoCols.TICKET_CODE,TICKET_CODE);
            jo.putString(colName.Phim123PromoCols.FILM_NAME,FILM_NAME);
            jo.putString(colName.Phim123PromoCols.RAP,RAP);
            jo.putString(colName.Phim123PromoCols.DISPLAY_TIME,DISPLAY_TIME);

            jo.putString(colName.Phim123PromoCols.BUY_TIME,BUY_TIME);
            jo.putString(colName.Phim123PromoCols.SEAT_LIST,SEAT_LIST);
            jo.putNumber(colName.Phim123PromoCols.NUMBER_OF_SEAT, NUMBER_OF_SEAT);

            jo.putString(colName.Phim123PromoCols.PROMO_CODE,PROMO_CODE);
            jo.putNumber(colName.Phim123PromoCols.TIME,TIME);
            jo.putNumber(colName.Phim123PromoCols.PROMO_COUNT,PROMO_COUNT);
            jo.putNumber(colName.Phim123PromoCols.PROMO_TIME,PROMO_TIME);

            return jo;

        }
    }

    public void save(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.Phim123PromoCols.TABLE)
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

    public void isExist(final int number, final Handler<Boolean> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.Phim123PromoCols.ID, number);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject fields = new JsonObject();
        fields.putNumber(colName.Phim123PromoCols.ID, 1);

        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                boolean result = false;
                JsonObject jo = event.body().getObject(MongoKeyWords.RESULT, null);
                if(jo != null){
                    String strId =  jo.getString(colName.Phim123PromoCols.ID, "0");
                    int returnId = DataUtil.strToInt(strId);
                    if(returnId == number){
                        result = true;
                    }
                }

                callback.handle(result);
            }
        });
    }

    public void update(final int number, final JsonObject joUpdated, final Handler<Boolean> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.Phim123PromoCols.ID, number);
        query.putObject(MongoKeyWords.CRITERIA,match);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdated);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });

    }

    public void findOne(final int number, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.Phim123PromoCols.ID, number);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Promo123PhimDb.Obj obj = null;
                JsonObject jo = event.body().getObject(MongoKeyWords.RESULT, null);
                if(jo != null){
                    obj = new Obj(jo);
                }

                callback.handle(obj);
            }
        });
    }

    public void doStatistic(long ldate, String rap, final Handler<JsonObject> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoCols.TABLE);

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

            match.putObject(colName.Phim123PromoCols.TIME, dk);
        }

        if(!"all".equalsIgnoreCase(rap)){
            match.putString(colName.Phim123PromoCols.PROMO_CODE,rap);
        }

        Set<String> set = match.getFieldNames();
        if(set.size()>0){
            query.putObject(MongoKeyWords.MATCHER,match);
        }

        //fields returned to client
        JsonObject fields = new JsonObject();
        fields.putNumber(colName.Phim123PromoCols.NUMBER_OF_SEAT, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int numberofCommbo = 0;
                int numberofTicket = 0;
                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY,null);
                if(array != null){
                    numberofCommbo = array.size();
                    for(Object o : array){
                        numberofTicket +=((JsonObject)o).getInteger(colName.Phim123PromoCols.NUMBER_OF_SEAT,0);
                    }
                }

                JsonObject jresult = new JsonObject();
                jresult.putNumber("number",numberofCommbo);
                jresult.putNumber("ticketcount",numberofTicket);

                callback.handle(jresult);
            }
        });
    }


    public void getPage(int phoneNumber, int batchSize, final Handler<ArrayList<Integer>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoCols.TABLE);

        //matcher
        JsonObject gt = new JsonObject();
        gt.putNumber(MongoKeyWords.GREATER,phoneNumber);
        JsonObject match = new JsonObject();
        match.putObject(colName.Phim123PromoCols.ID, gt);
        query.putObject(MongoKeyWords.MATCHER, match);


        JsonObject sort = new JsonObject();
        sort.putNumber(colName.Phim123PromoCols.ID, 1);

        query.putObject(MongoKeyWords.SORT,sort);
        query.putNumber(MongoKeyWords.BATCH_SIZE, batchSize);

        JsonObject fields = new JsonObject();
        fields.putNumber(colName.Phim123PromoCols.ID, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Integer> arrayList = null;

                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);

                if(array != null && array.size() >0){
                    arrayList = new ArrayList<>();
                    for (int i=0;i<array.size();i++){
                        JsonObject o = array.get(i);
                        int number = o.getInteger(colName.Phim123PromoCols.ID, 0);
                        if(number >0){
                            arrayList.add(number);
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });

    }

}
