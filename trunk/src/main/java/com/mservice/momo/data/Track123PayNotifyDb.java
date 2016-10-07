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

/**
 * Created by concu on 8/12/14.
 */
public class Track123PayNotifyDb {
    
    public static class Obj{
        public String ID = "_id";
        public String MTRANSACTIONID = "";
        public String BANK_CODE = "";
        public String TRAN_STATUS = "";
        public String DESCRIPTION = "";
        public String TIMESTAMP = "";
        public String CHECK_SUM = "";
        public int PHONE_NUMBER = 0;
        public long AMOUNT = 0;
        public long CREATE_TIME = System.currentTimeMillis();
        public String ERROR_DESC ="";
        public String CREATE_TIME_VN = Misc.dateVNFormatWithTime(System.currentTimeMillis()) ;

        public Obj(){}
        public Obj(JsonObject jo){
            ID = jo.getString("_id","");
            MTRANSACTIONID = jo.getString(colName.Track123PayNotify.MTRANSACTIONID,"");
            BANK_CODE = jo.getString(colName.Track123PayNotify.BANK_CODE,"");
            TRAN_STATUS = jo.getString(colName.Track123PayNotify.TRAN_STATUS,"");
            DESCRIPTION = jo.getString(colName.Track123PayNotify.DESCRIPTION,"");
            TIMESTAMP = jo.getString(colName.Track123PayNotify.TIMESTAMP,"");
            CHECK_SUM = jo.getString(colName.Track123PayNotify.CHECK_SUM,"");
            PHONE_NUMBER = jo.getInteger(colName.Track123PayNotify.PHONE_NUMBER, 0);
            AMOUNT = jo.getLong(colName.Track123PayNotify.AMOUNT, 0);
            CREATE_TIME = jo.getLong(colName.Track123PayNotify.CREATE_TIME,0);
            CREATE_TIME_VN = jo.getString(colName.Track123PayNotify.CREATE_TIME_VN,"");
            ERROR_DESC = jo.getString(colName.Track123PayNotify.ERROR_DESC,"");
        }

        public JsonObject toJSON(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.Track123PayNotify.ID,ID);
            jo.putString(colName.Track123PayNotify.MTRANSACTIONID, MTRANSACTIONID);
            jo.putString(colName.Track123PayNotify.BANK_CODE,BANK_CODE);
            jo.putString(colName.Track123PayNotify.TRAN_STATUS,TRAN_STATUS);
            jo.putString(colName.Track123PayNotify.DESCRIPTION,DESCRIPTION);
            jo.putString(colName.Track123PayNotify.TIMESTAMP,TIMESTAMP);
            jo.putString(colName.Track123PayNotify.CHECK_SUM,CHECK_SUM);
            jo.putNumber(colName.Track123PayNotify.PHONE_NUMBER, PHONE_NUMBER);
            jo.putNumber(colName.Track123PayNotify.AMOUNT,AMOUNT);
            jo.putNumber(colName.Track123PayNotify.CREATE_TIME,CREATE_TIME);
            jo.putString(colName.Track123PayNotify.CREATE_TIME_VN,CREATE_TIME_VN);
            jo.putString(colName.Track123PayNotify.ERROR_DESC,ERROR_DESC);
            return jo;
        }
    }

    EventBus eventBus;
    Logger logger;
    public Track123PayNotifyDb(EventBus eventBus, Logger logger){
        this.eventBus =eventBus;
        this.logger = logger;
    }

    public void save(Obj trackObj, final Handler<Boolean> callback){

        JsonObject jo = trackObj.toJSON();
        jo.removeField(colName.Track123PayNotify.ID);

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.Track123PayNotify.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, jo);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                boolean result = false;
                if (event.body() != null) {
                    result = true;
                    String createdId = event.body().getString("_id");
                    logger.debug("Id generated " + createdId);
                }
                callback.handle(result);
            }
        });
    }

    public void findAlll(final Handler<ArrayList<String>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Track123PayNotify.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.Track123PayNotify.BANK_CODE, "123PCC");
        matcher.putString(colName.Track123PayNotify.TRAN_STATUS, "1");


        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putNumber(MongoKeyWords.BATCH_SIZE,5000);

        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 0);
        fields.putNumber(colName.Track123PayNotify.MTRANSACTIONID,1);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                ArrayList<String> arrayList = new ArrayList<String>();

                if(results != null && results.size() > 0){
                    for(int i =0;i<results.size();i++){

                        String s = ((JsonObject)results.get(i)).getString(colName.Track123PayNotify.MTRANSACTIONID,"");
                        if(!"".equalsIgnoreCase(s)){
                            arrayList.add(s);
                        }
                    }

                }
                callback.handle(arrayList);
            }
        });
    }

    public void isMTranProcessed(String mTranId, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.Track123PayNotify.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.Track123PayNotify.MTRANSACTIONID, mTranId);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonObject results = message.body().getObject(MongoKeyWords.RESULT, null);
                boolean result = false;

                if(results!=null){
                    result = true;
                }
                callback.handle(result);
            }
        });
    }

    public void isMTranProcessedOld(String mTranId, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.Track123PayNotify.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.Track123PayNotify.MTRANSACTIONID, mTranId);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        //batch size
        query.putNumber(MongoKeyWords.BATCH_SIZE, 1);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                boolean result = false;

                if(results!=null && results.size() >0){
                    result = true;
                }
                callback.handle(result);
            }
        });
    }




}
