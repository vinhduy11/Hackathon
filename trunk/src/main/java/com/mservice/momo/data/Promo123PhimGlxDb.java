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

public class Promo123PhimGlxDb {
    EventBus eventBus;
    Logger logger;

    public static String STATUS_NEW = "new";
    public static String STATUS_COMPLETED = "cmp";
    public static String STATUS_INVALID = "invalid";
    public static String DESC_NEW = "Chưa trả commbo cứng";
    public static String DESC_COMPLETED = "Đã trả combo cứng";

    public Promo123PhimGlxDb(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    public static class Obj{
        public String ID = "";
        public String INVOICE_NO="";
        public String TICKET_CODE="";
        public String FILM_NAME="";
        public String RAP="";
        public String DISPLAY_TIME="";
        public String BUY_TIME="";
        public String SEAT_LIST="";
        public int NUMBER_OF_SEAT=0;
        public String PROMO_CODE="";
        public long TIME= 0;
        public String TIMEVN ="";
        public int PROMO_COUNT = 0;
        public String PROMO_TIMEVN = "";
        public long AMOUNT = 0;
        public String UPDATE_BY="";
        public long UPDATE_TIME=0;
        public String SUBRAP ="";
        public String STATUS="";
        public String DESC ="";

/*
        public static String ID = "_id";                // so dien thoai
        public static String INVOICE_NO="invno";        //ma hoa don cua 123phim tra cho minh
        public static String TICKET_CODE="tkcode";      //ma ve 123phim tra cho minh
        public static String FILM_NAME="fname";         //ten phim
        public static String RAP="rap";                 // rap
        public static String DISPLAY_TIME="dispaytime"; //gio chieu
        public static String BUY_TIME="buytime";        // ngay mua
        public static String SEAT_LIST="seatlst";       // danh sach ghe
        public static String NUMBER_OF_SEAT="numofseat";// so luong ghe
        public static String PROMO_CODE="code";         // ma khuyen mai
        public static String TIME="time";               // thoi gian tao record
        public static String PROMO_COUNT="p_count";     // thoi gian tao record
        public static String PROMO_TIME="p_time";       // thoi gian tao record
        public static String AMOUNT ="amt";             // gia tri mua ve
        public static String UPDATE_BY="update_by";     // user tra combo
        public static String UPDATE_TIME="update_time"; // thoi gian tra combo
        public static String STATUS="status";           // trang thai combo*/


        public Obj(){}
        public Obj(JsonObject jo){

            ID = jo.getString(colName.Phim123PromoGlxCols.ID);
            INVOICE_NO= jo.getString(colName.Phim123PromoGlxCols.INVOICE_NO, "");
            TICKET_CODE = jo.getString(colName.Phim123PromoGlxCols.TICKET_CODE, "");
            FILM_NAME=jo.getString(colName.Phim123PromoGlxCols.FILM_NAME,"");
            RAP=jo.getString(colName.Phim123PromoGlxCols.RAP,"");
            DISPLAY_TIME=jo.getString(colName.Phim123PromoGlxCols.DISPLAY_TIME,"");
            BUY_TIME=jo.getString(colName.Phim123PromoGlxCols.BUY_TIME,"");
            SEAT_LIST=jo.getString(colName.Phim123PromoGlxCols.SEAT_LIST,"");
            NUMBER_OF_SEAT=jo.getInteger(colName.Phim123PromoGlxCols.NUMBER_OF_SEAT, 0);
            PROMO_CODE=jo.getString(colName.Phim123PromoGlxCols.PROMO_CODE, "");
            TIME= jo.getLong(colName.Phim123PromoGlxCols.TIME, 0);
            TIMEVN = jo.getString(colName.Phim123PromoGlxCols.TIMEVN, "");
            PROMO_COUNT= jo.getInteger(colName.Phim123PromoGlxCols.PROMO_COUNT, 0);
            PROMO_TIMEVN= jo.getString(colName.Phim123PromoGlxCols.PROMO_TIMEVN, PROMO_TIMEVN);
            AMOUNT = jo.getLong(colName.Phim123PromoGlxCols.AMOUNT, 0);
            SUBRAP = jo.getString(colName.Phim123PromoGlxCols.SUBRAP,"");
            UPDATE_BY=jo.getString(colName.Phim123PromoGlxCols.UPDATE_BY,"");
            UPDATE_TIME=jo.getLong(colName.Phim123PromoGlxCols.UPDATE_TIME,0);
            STATUS=jo.getString(colName.Phim123PromoGlxCols.STATUS,"");
            DESC=jo.getString(colName.Phim123PromoGlxCols.DESC,"");
        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();

            jo.putString(colName.Phim123PromoGlxCols.ID,ID);
            jo.putString(colName.Phim123PromoGlxCols.INVOICE_NO, INVOICE_NO);
            jo.putString(colName.Phim123PromoGlxCols.TICKET_CODE,TICKET_CODE);
            jo.putString(colName.Phim123PromoGlxCols.FILM_NAME,FILM_NAME);
            jo.putString(colName.Phim123PromoGlxCols.RAP,RAP);
            jo.putString(colName.Phim123PromoGlxCols.DISPLAY_TIME,DISPLAY_TIME);

            jo.putString(colName.Phim123PromoGlxCols.BUY_TIME,BUY_TIME);
            jo.putString(colName.Phim123PromoGlxCols.SEAT_LIST,SEAT_LIST);
            jo.putNumber(colName.Phim123PromoGlxCols.NUMBER_OF_SEAT, NUMBER_OF_SEAT);

            jo.putString(colName.Phim123PromoGlxCols.PROMO_CODE,PROMO_CODE);
            jo.putNumber(colName.Phim123PromoGlxCols.TIME, TIME);
            jo.putString(colName.Phim123PromoGlxCols.TIMEVN, TIMEVN);
            jo.putNumber(colName.Phim123PromoGlxCols.PROMO_COUNT, PROMO_COUNT);
            jo.putString(colName.Phim123PromoGlxCols.PROMO_TIMEVN, PROMO_TIMEVN);
            jo.putNumber(colName.Phim123PromoGlxCols.AMOUNT, AMOUNT);
            jo.putNumber(colName.Phim123PromoGlxCols.TIME,TIME);
            jo.putString(colName.Phim123PromoGlxCols.SUBRAP, SUBRAP);
            jo.putString(colName.Phim123PromoGlxCols.UPDATE_BY,UPDATE_BY);
            jo.putNumber(colName.Phim123PromoGlxCols.UPDATE_TIME, UPDATE_TIME);
            jo.putString(colName.Phim123PromoGlxCols.STATUS, STATUS);
            jo.putString(colName.Phim123PromoGlxCols.DESC,DESC);
            jo.putNumber(colName.Phim123PromoGlxCols.AMOUNT,AMOUNT);
            return jo;
        }
    }

    public void save(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.Phim123PromoGlxCols.TABLE)
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

    public void update(final int number, final JsonObject joUpdated, final Handler<Boolean> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoGlxCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.Phim123PromoGlxCols.ID, number+"");
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
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoGlxCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.Phim123PromoGlxCols.ID, number + "");
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Promo123PhimGlxDb.Obj obj = new Obj();
                JsonObject jo = event.body().getObject(MongoKeyWords.RESULT, null);
                if(jo != null){
                    obj = new Obj(jo);
                }

                callback.handle(obj);
            }
        });
    }

    public void get(final String code, final String phone, final Handler<Obj> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoGlxCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();

        if(!"".equalsIgnoreCase(code)){
            match.putString(colName.Phim123PromoGlxCols.PROMO_CODE, code);
        }

        if(!"".equalsIgnoreCase(phone)){
            match.putString(colName.Phim123PromoGlxCols.ID, Long.parseLong(phone)+"");
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Promo123PhimGlxDb.Obj obj = null;
                JsonObject jo = event.body().getObject(MongoKeyWords.RESULT, null);
                if(jo != null){
                    obj = new Obj(jo);
                }

                callback.handle(obj);
            }
        });
    }

    public void export(final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoGlxCols.TABLE);

        //matcher
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.Phim123PromoGlxCols.STATUS, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;
                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                if(results != null && results.size() > 0){
                    arrayList = new ArrayList<>();
                    for(Object o : results){
                        JsonObject jo = (JsonObject)o;
                        arrayList.add( new Obj(jo));
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void report(final String username, final long fromTime,final long toTime, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Phim123PromoGlxCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();

        if(!"".equalsIgnoreCase(username))
        {
            match.putString(colName.Phim123PromoGlxCols.UPDATE_BY, username);
        }else {
            JsonObject ne = new JsonObject();
            ne.putString(MongoKeyWords.NOT_EQUAL,"");
            match.putObject(colName.Phim123PromoGlxCols.UPDATE_BY, ne);
        }

        if(fromTime > 0 && toTime > 0){
            JsonObject jo = new JsonObject();
            jo.putNumber(MongoKeyWords.GREATER_OR_EQUAL, fromTime)
                    .putNumber(MongoKeyWords.LESS_OR_EQUAL,toTime);
            match.putObject(colName.Phim123PromoGlxCols.UPDATE_TIME, jo);
        }

        if(match.getFieldNames().size() > 0){
            query.putObject(MongoKeyWords.MATCHER, match);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;
                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if(results != null && results.size() > 0){
                    arrayList = new ArrayList<>();
                    for(Object o : results){
                        JsonObject jo = (JsonObject)o;
                        arrayList.add( new Obj(jo));
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

}
