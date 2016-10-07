package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 9/26/14.
 */
public class TopupSumDb {

    private EventBus eventBus;
    private Logger logger;

    public TopupSumDb(EventBus eventBus, Logger logger){
        this.eventBus = eventBus;
        this.logger = logger;
    }

    public void upsert(final Obj obj, final Handler<Boolean> callback){

        //appended field
        obj.time_vn = Misc.dateVNFormatWithTime(System.currentTimeMillis());

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.TopUpSumCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.TopUpSumCols.number, obj.number);
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

    public void findOne(final int number, final Handler<Obj> callback){

        String finalNumber = "" + number;

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.TopUpSumCols.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.TopUpSumCols.number, finalNumber);
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
        public String number ="";             //icon loai giao dich vu co su dung voucher
        public int total_amount = 0;    //ten cua voucher
        public long time =0;
        public String time_vn = "";
        public Obj(){}

        public Obj(JsonObject jo){
            number =jo.getString(colName.TopUpSumCols.number,"");
            total_amount =jo.getInteger(colName.TopUpSumCols.total_amount, 0);
            time =jo.getLong(colName.TopUpSumCols.time, 0);
            time_vn = jo.getString(colName.TopUpSumCols.time_vn,"");
        }

        public JsonObject toJSON(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.TopUpSumCols.number, number);
            jo.putNumber(colName.TopUpSumCols.total_amount,total_amount);
            jo.putNumber(colName.TopUpSumCols.time,time);
            jo.putString(colName.TopUpSumCols.time_vn,time_vn);
            return jo;
        }
    }

}
