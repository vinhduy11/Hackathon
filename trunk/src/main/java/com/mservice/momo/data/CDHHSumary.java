package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.CdhhConfig;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * This is concu. medium size -> good man
 * Created by concu on 4/18/14.
 */
public class CDHHSumary {

    public static class Obj{
        public String id ="";     // icon loai giao dich vu co su dung voucher
        public long value = 0;    // ten cua voucher

        public Obj(JsonObject jo){
            id = DataUtil.decode(jo.getString(colName.CDHHSumCols.id));
            value = jo.getLong(colName.CDHHCols.value);
        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.CDHHSumCols.id, id);
            jo.putNumber(colName.CDHHCols.value,value);
            return jo;
        }

    }
    private EventBus eventBus;
    private Logger logger;

    public CDHHSumary(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void increase(String maCapDoi, long delta,final CdhhConfig cdhhConfig , final Handler<Boolean> callback){

        //todo save to total table
        //new object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putNumber(colName.CDHHSumCols.value,delta);
        final JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHSumCols.table);

        JsonObject match   = new JsonObject();
        match.putString(colName.CDHHSumCols.id, DataUtil.encode(maCapDoi));
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();

                boolean result = false;
                if(obj.getString(MongoKeyWords.STATUS,"ok").equalsIgnoreCase("ok")){
                    result = true;
                }
                callback.handle(result);
            }
        });


        //todo save to total by week or quarter
        String collName = (cdhhConfig.collName == null ? "" : cdhhConfig.collName);
        String collectionName = colName.CDHHSumCols.table + "_" + collName;

        if(!"".equalsIgnoreCase(collName)){

            JsonObject newJsonByWorQ = new JsonObject();
            newJsonByWorQ.putNumber(colName.CDHHSumCols.value, delta);
            final JsonObject queryByWorQ    = new JsonObject();

            queryByWorQ.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
            queryByWorQ.putString(MongoKeyWords.COLLECTION, collectionName);

            JsonObject matchByWorQ   = new JsonObject();
            matchByWorQ.putString(colName.CDHHSumCols.id, DataUtil.encode(maCapDoi));
            queryByWorQ.putObject(MongoKeyWords.CRITERIA, matchByWorQ);

            //set
            JsonObject setByWorQ = new JsonObject();
            setByWorQ.putObject(MongoKeyWords.INCREMENT, newJsonByWorQ);
            queryByWorQ.putObject(MongoKeyWords.OBJ_NEW, setByWorQ);
            queryByWorQ.putBoolean(MongoKeyWords.UPSERT, true);

            eventBus.send(AppConstant.MongoVerticle_ADDRESS, queryByWorQ, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> jsonObjectMessage) {
                    //todo no need to do something here
                }
            });
        }
    }

    public void update(String maCapDoi, long delta, final CdhhConfig cdhhConfig, final Handler<Boolean> callback){


        //todo update to total table
        final JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHSumCols.table);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CDHHSumCols.id, maCapDoi);
        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject objNew = new JsonObject();

        objNew.putNumber(colName.CDHHSumCols.value,delta);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, objNew);

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

        //todo update to total table by week or quarter
        String collName = cdhhConfig.collName == null  ? "" : cdhhConfig.collName;
        String collectName = colName.CDHHSumCols.table + "_" + collName;

        if("".equalsIgnoreCase(collName)){
            logger.info("cdhhConfig.collName == null in CDHHSumary");
            return;
        }

        final JsonObject queryByWorQ = new JsonObject();
        queryByWorQ.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        queryByWorQ.putString(MongoKeyWords.COLLECTION, collectName);
        JsonObject matchByWorQ = new JsonObject();

        //matcher
        matchByWorQ.putString(colName.CDHHSumCols.id, maCapDoi);
        matchByWorQ.putObject(MongoKeyWords.CRITERIA, matchByWorQ);

        JsonObject objNewByWorQ = new JsonObject();

        objNewByWorQ.putNumber(colName.CDHHSumCols.value,delta);

        JsonObject fieldsSetByWorQ = new JsonObject();
        fieldsSetByWorQ.putObject(MongoKeyWords.SET_$, objNewByWorQ);

        queryByWorQ.putObject(MongoKeyWords.OBJ_NEW, fieldsSetByWorQ);
        queryByWorQ.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, queryByWorQ, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                //todo no need to do something here
            }
        });

    }

}
