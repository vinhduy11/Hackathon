package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by concu on 4/18/14.
 */
public class CDHH  {
    private EventBus eventBus;
    private Logger logger;
    private Vertx vertx;
    public CDHH(Vertx vertx, Logger logger){
        this.eventBus=vertx.eventBus();
        this.logger = logger;
        this.vertx = vertx;
    }

    public void findAll(final String collName, String serviceid, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, collName);
        //matcher

        String curentDay = Misc.dateVNFormat(System.currentTimeMillis());
        match.putString(colName.CDHHCols.serviceid, serviceid);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if(results != null && results.size() > 0){
                        for(Object o : results){

                            JsonObject jo = (JsonObject)o;

                            Obj ob = new Obj(jo);
                            arrayList.add(ob);
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });

    }

    public void findAllWithFilter(final String collName
            ,JsonObject matcher
            ,final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, collName);
        //matcher
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if(results != null && results.size() > 0){
                        for(Object o : results){

                            JsonObject jo = (JsonObject)o;

                            Obj ob = new Obj(jo);
                            arrayList.add(ob);
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });

    }

    public void save(final Obj obj, CdhhConfig cdhhConfig , final Handler<Boolean> callback){

        String collName = cdhhConfig.collName;
        String periodName = cdhhConfig.periodName;

        if(collName == null || "".equalsIgnoreCase(collName) || periodName == null || "".equalsIgnoreCase(periodName)){
            logger.info("CDHH 2014, Ngoai gio binh chon roi");
            logger.info("CDHH 2014, collection name " + (collName == null ? "null" : collName));
            logger.info("CDHH 2014, period name " + periodName == null ? "null" : periodName);
            callback.handle(false);
            return;
        }

        //save bang tuan hoac bang 15 phut
        JsonObject query = new JsonObject()
                            .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                            .putString(MongoKeyWords.COLLECTION, collName)
                            .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if (event.body() != null) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });

        //dang save bang tong
        JsonObject queryTotal = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, queryTotal, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                //todo no need to do something here
            }
        });
    }

    public void save(final Obj obj, String collName , final Handler<Boolean> callback){

        //save bang tuan hoac bang 15 phut
        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, collName)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if (event.body() != null) {
                    callback.handle(true);
                }else{
                    callback.handle(false);
                }
            }
        });

        //dang save bang tong
        JsonObject queryTotal = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, queryTotal, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                //todo no need to do something here
            }
        });
    }

    public void saveToNewCollection(final Obj obj, String collName , final Handler<Integer> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, collName)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int count = 0;

                if(event.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = event.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            count+= ((JsonObject)o).getInteger(colName.CDHHCols.voteAmount,0);
                        }
                    }
                }
                callback.handle(count);
            }
        });


    }

    public void findByNumber(int number, String serviceId, final Handler<Integer> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table);
        //matcher

        String curentDay = Misc.dateVNFormat(System.currentTimeMillis());

        match.putString(colName.CDHHCols.day_vn, curentDay);
        match.putNumber(colName.CDHHCols.number, number);
        match.putString(colName.CDHHCols.serviceid, serviceId);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                int count = 0;

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                           count+= ((JsonObject)o).getInteger(colName.CDHHCols.voteAmount,0);
                        }
                    }
                }
                callback.handle(count);
            }
        });
    }

    public void findByNumberAndSection(String collection, boolean byDay, int number, String serviceId, int code, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        // UPDATEL: for each serviceId
        if (!byDay) {
            //not by day, so we ject in the session table
            query.putString(MongoKeyWords.COLLECTION, collection);
        } else {
            //by day, so we check in full table
            query.putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table);
        }

        //matcher

        match.putNumber(colName.CDHHCols.number, number);
        match.putString(colName.CDHHCols.serviceid, serviceId);
        // UPDATEL: for each serviceId
        if (!byDay) {

            match.putString(colName.CDHHCols.code, String.valueOf(code));
        } else {
            String curentDay = Misc.dateVNFormat(System.currentTimeMillis());
            match.putString(colName.CDHHCols.day_vn,curentDay);
        }
        query.putObject(MongoKeyWords.MATCHER,match);

        logger.info("check voting " + query.toString());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                int count = 0;

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            count+= ((JsonObject)o).getInteger(colName.CDHHCols.voteAmount,0);
                        }
                    }
                }
                callback.handle(count);
            }
        });
    }

    public void getCountByNumberAnDate(int number, final Handler<ArrayList<Obj>> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.COUNT);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table);

        //matcher
        JsonObject jo  = Misc.getStartAndEndCurrentDateInMilliseconds();
        long begin = jo.getLong(Misc.BeginDateInMilliSec);
        long end = jo.getLong(Misc.EndDateInMilliSec);

        JsonObject time = new JsonObject();
        time.putNumber(MongoKeyWords.GREATER_OR_EQUAL,begin);
        time.putNumber(MongoKeyWords.LESS_OR_EQUAL,end);

        match.putObject(colName.CDHHCols.time,time);
        match.putNumber(colName.CDHHCols.number, number);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CDHHCols.time, -1); // giam dan theo thoi gian
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void findByFromTime(final long time,String oldCollection, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject matcher = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, oldCollection);

        JsonObject sort = new JsonObject();
        sort.putNumber(colName.BNHVCols.time,-1);
        JsonObject gt = new JsonObject();
        gt.putNumber(MongoKeyWords.GREATER_OR_EQUAL,time);
        matcher.putObject(colName.BNHVCols.time,gt);
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }
//thong find by from time*******************

    public void findByTime(final long time, int batchSize, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject matcher = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CDHHCols.time,-1);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        JsonObject gt = new JsonObject();
        gt.putNumber(MongoKeyWords.GREATER,time);

        matcher.putObject(colName.CDHHCols.time,gt);
        query.putObject(MongoKeyWords.MATCHER,matcher);

        query.putObject(MongoKeyWords.SORT, sort);
        query.putNumber(MongoKeyWords.BATCH_SIZE, batchSize);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void countGroupBySBD(final Handler<HashMap<String,Long>> callback){
        JsonObject query    = new JsonObject();
        JsonObject matcher = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CDHHCols.time, -1);

        query.putObject(MongoKeyWords.SORT, sort);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                HashMap<String,Long> hashMap = new HashMap<String, Long>();
                long all = 0;
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            Obj obj = new Obj((JsonObject)o);

                            all += obj.voteAmount;

                            Long tt = hashMap.get(DataUtil.encode("0" + obj.code));
                            if(tt == null){
                                hashMap.put(DataUtil.encode("0" + obj.code), obj.voteAmount);
                            }else{
                               tt += obj.voteAmount;
                               hashMap.put(DataUtil.encode("0" + obj.code),tt);
                            }
                        }
                    }
                }
                callback.handle(hashMap);
            }
        });
    }

    public void countGroupBySBD(String collectionName,  final Handler<HashMap<String,Long>> callback){
        JsonObject query    = new JsonObject();
        JsonObject matcher = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, collectionName);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CDHHCols.code,-1);

        query.putObject(MongoKeyWords.SORT, sort);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                HashMap<String,Long> hashMap = new HashMap<String, Long>();
                long all = 0;
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            Obj obj = new Obj((JsonObject)o);

                            all += obj.voteAmount;

                            Long tt = hashMap.get(("0" + obj.code));
                            if(tt == null){
                                hashMap.put(("0" + obj.code), obj.voteAmount);
                            }else{
                                tt += obj.voteAmount;
                                hashMap.put(("0" + obj.code),tt);
                            }
                        }
                    }
                }
                callback.handle(hashMap);
            }
        });
    }

    public void countGroupByNumber(final Handler<HashMap<String,Long>> callback){
        JsonObject query    = new JsonObject();
        JsonObject matcher = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CDHHCols.time, -1);

        query.putObject(MongoKeyWords.SORT, sort);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                HashMap<String, Long> hashMap = new HashMap<String, Long>();
                long all = 0;
                if (jom.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if (results != null && results.size() > 0) {
                        for (Object o : results) {
                            Obj obj = new Obj((JsonObject) o);

                            all += obj.voteAmount;

                            Long tt = hashMap.get("0" + obj.number);
                            if (tt == null) {
                                hashMap.put("0" + obj.number, obj.voteAmount);
                            } else {
                                tt += obj.voteAmount;
                                hashMap.put("0" + obj.number, tt);
                            }
                        }
                    }
                }
                callback.handle(hashMap);
            }
        });
    }

    public static class Obj{
        public int number =0;             //icon loai giao dich vu co su dung voucher
        public long value = 0;    //ten cua voucher
        public long voteAmount =0;              //bat tat dich vu su dung vourcher
        public String time_vn ="";         //
        public String code = "";
        public String day_vn ="";         //
        public long time =0;
        public long tranid =0;
        public String name = "";
        public String serviceid ="";

        public Obj(JsonObject jo){
            number = jo.getInteger(colName.CDHHCols.number,0);
            value = jo.getLong(colName.CDHHCols.value, 0);
            voteAmount =jo.getLong(colName.CDHHCols.voteAmount,0);
            time_vn =jo.getString(colName.CDHHCols.time_vn, "");
            time =jo.getLong(colName.CDHHCols.time, 0);
            tranid = jo.getLong(colName.CDHHCols.tranid,0);
            code = jo.getString(colName.CDHHCols.code);
            day_vn = jo.getString(colName.CDHHCols.day_vn,"");
            name = jo.getString(colName.CDHHCols.name,"");
            serviceid = jo.getString(colName.CDHHCols.serviceid,"");

        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.CDHHCols.number,number);
            jo.putNumber(colName.CDHHCols.value,value);
            jo.putNumber(colName.CDHHCols.voteAmount,voteAmount);
            jo.putString(colName.CDHHCols.time_vn, time_vn);
            jo.putString(colName.CDHHCols.code, code);
            jo.putNumber(colName.CDHHCols.time, time);
            jo.putString(colName.CDHHCols.day_vn, day_vn);
            jo.putNumber(colName.CDHHCols.tranid, tranid);
            jo.putString(colName.CDHHCols.name, name);
            jo.putString(colName.CDHHCols.serviceid, serviceid);

            return jo;
        }

    }
}
