package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 2/18/14
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class AccessHistoryDb {

    EventBus eventBus;
    public AccessHistoryDb(EventBus eb){
        eventBus = eb;
    }

    private void saveAccessInfo(final int number, JsonObject newJsonObj, final Handler<Boolean> callback){

        callback.handle(true);
        return;
        /*JsonObject query    = new JsonObject();
        query.putString("action", "update");
        query.putString("collection", "ah" + number);
        JsonObject match   = new JsonObject();

        match.putNumber(colName.AccessHistoryDBCols.START_TIME_ACCESS, -1);
        query.putObject("criteria", match);

        JsonObject setCtnr = new JsonObject();
        setCtnr.putObject("$set", newJsonObj);
        query.putObject("objNew", setCtnr);
        query.putBoolean("upsert", true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<CoreMessage<JsonObject>>() {

            @Override
            public void handle(CoreMessage<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });*/
    }

    public static class AccessHistoryObj {
        public String ip = "";
        public long start_time_access;
        public long end_time_access;
        public String device_model;

        public AccessHistoryObj(JsonObject json){

            ip = json.getString(colName.AccessHistoryDBCols.IP, "");
            start_time_access = DataUtil.stringToUNumber(String.valueOf(json.getNumber(colName.AccessHistoryDBCols.START_TIME_ACCESS, -1))) ;
            end_time_access = DataUtil.stringToUNumber(String.valueOf(json.getNumber(colName.AccessHistoryDBCols.END_TIME_ACCESS, -1))) ;
            device_model = json.getString(colName.AccessHistoryDBCols.DEVICE_MODEL, "");
        }
    }

    public  void saveAccessTime(int number,String ip, long time_in, String device_model, final Handler callback){

        JsonObject objNew = new JsonObject();

        objNew.putString(colName.AccessHistoryDBCols.IP, ip);
        objNew.putNumber(colName.AccessHistoryDBCols.START_TIME_ACCESS, time_in);
        objNew.putNumber(colName.AccessHistoryDBCols.END_TIME_ACCESS, System.currentTimeMillis());
        objNew.putString(colName.AccessHistoryDBCols.DEVICE_MODEL, device_model);

        saveAccessInfo(number, objNew, callback);

    }


    public void getALLCHECK(final Handler<Message<ArrayList<String>>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, "check_phones");

        //sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT,sort);

        query.putNumber(MongoKeyWords.BATCH_SIZE,100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, callback);


        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                ArrayList<String> arr = new ArrayList<String>();

                //json.getArray("results")


            }
        });
    }





    public void getLatest(int number, int pageNum, int pageSize, final Handler callback){

        callback.handle(null);
        return;
        /*JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", "ah" + number);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id",-1);
        query.putObject("sort", sort);

        int skip = (pageNum - 1) * pageSize;
        int records = pageNum*pageSize;

        query.putNumber("skip",skip);
        query.putNumber("limit", records);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<CoreMessage<JsonObject>>() {

            @Override
            public void handle(CoreMessage<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();

                ArrayList<AccessHistoryObj>  arAH = new ArrayList<>();

                if (obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = jsonObjectMessage.body().getArray("results");

                    if(results != null && results.size() > 0){

                        for(int i=0;i<results.size();i++){
                            arAH.add( new AccessHistoryObj((JsonObject)results.get(i)));
                        }
                    }else{
                        callback.handle(null);
                    }

                    callback.handle(arAH);
                } else {

                }
            }
        });*/
    }

}
