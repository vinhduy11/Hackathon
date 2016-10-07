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

/**
 * Created by concu on 4/19/14.
 */
public class CoreLastTime {
    private EventBus eventBus;
    private Logger logger;
    public static class Obj{
        public long core_last_update_time;
        public int row_id;
        public Obj(JsonObject jo){
            core_last_update_time = jo.getLong(colName.CoreLastTimeDBCols.LAST_UPDATE_TIME,0);
            row_id = jo.getInteger(colName.CoreLastTimeDBCols.ROW_ID,1);
        }
    }
    public CoreLastTime(EventBus eb, Logger logger){
        this.eventBus = eb;
        this.logger=logger;
    }

    public void upsertLastTime(final long core_last_update_time, final Handler<Boolean> callback){
        getLastTime(new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                long update_time = core_last_update_time > aLong ? core_last_update_time : aLong;
                upsert(update_time,new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        logger.info("Update core last time : " +aBoolean);
                        callback.handle(aBoolean);
                    }
                });
            }
        });

    }
    private void upsert(final long lastTime, final Handler<Boolean> cb){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CoreLastTimeDBCols.TABLE);

        //matcher
        JsonObject criteria = new JsonObject();
        criteria.putNumber(colName.CoreLastTimeDBCols.ROW_ID,1);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        //sort
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.CoreLastTimeDBCols.ROW_ID,1);
        objNew.putNumber(colName.CoreLastTimeDBCols.LAST_UPDATE_TIME,lastTime);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                if(json!= null){
                    boolean result = json.getString(MongoKeyWords.STATUS,"ko").equalsIgnoreCase("ok");
                    cb.handle(result);
                }
                else {
                    cb.handle(false);
                }
            }
        });
    }
    private void upsertOld(final long lastTime, final Handler<Boolean> cb){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.CoreLastTimeDBCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.CoreLastTimeDBCols.ROW_ID,1);
        query.putObject(MongoKeyWords.MATCHER, match);

        //sort
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.CoreLastTimeDBCols.ROW_ID,1);
        objNew.putNumber(colName.CoreLastTimeDBCols.LAST_UPDATE_TIME,lastTime);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                if(json!= null){
                    boolean result = json.getString(MongoKeyWords.STATUS).equalsIgnoreCase("ok");
                    cb.handle(result);
                }
                else {
                    cb.handle(false);
                }
            }
        });
    }

    public void getLastTime(final Handler<Long> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.CoreLastTimeDBCols.TABLE);
        match.putNumber(colName.CoreLastTimeDBCols.ROW_ID, 1);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jOM) {

            long lastTime = 0;
                JsonObject results = jOM.body().getObject(MongoKeyWords.RESULT);
                if(results != null){
                    lastTime = results.getLong(colName.CoreLastTimeDBCols.LAST_UPDATE_TIME,0);
                }
                callback.handle(lastTime);
            }
        });
    }

    public void getLastTimeOld(final Handler<Long> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CoreLastTimeDBCols.TABLE);
        match.putNumber(colName.CoreLastTimeDBCols.ROW_ID, 1);
        query.putObject(MongoKeyWords.MATCHER,match);

        //sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT,sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jOM) {

                long lastTime = 0;
                if(jOM.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray results = jOM.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        JsonObject item = results.get(0);
                        lastTime = item.getLong(colName.CoreLastTimeDBCols.LAST_UPDATE_TIME,0);
                    }
                }
                callback.handle(lastTime);
            }
        });
    }
}
