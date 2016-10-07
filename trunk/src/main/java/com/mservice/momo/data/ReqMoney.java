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
 * Created by concu on 5/6/14.
 */
public class ReqMoney {

    EventBus eventBus;
    Logger logger;
    private static int REQUEST_MONEY_MAX_PER_DAY =0;

    public static class Obj{
        public int number =0;
        public int count =0;
        public long curtime =0;
        public Obj(JsonObject jo){
            number =  jo.getInteger(colName.ReqMoneyCols.NUMBER,0);
            count  =  jo.getInteger(colName.ReqMoneyCols.COUNT,0);
            curtime = jo.getLong(colName.ReqMoneyCols.CURRENT_TIME, 0);
        }
    }

    public ReqMoney(EventBus eb, Logger log) {
        eventBus = eb;
        logger = log;
    }
    public static void loadCfg(int request_money_max_per_day){
        REQUEST_MONEY_MAX_PER_DAY = request_money_max_per_day;
    }

    public void execRequestMoney(final int number,final Handler<Integer> callback){
        getInfo(number,new Handler<Obj>() {
            @Override
            public void handle(Obj obj) {

                int count = 1;
                long curTime = System.currentTimeMillis();

                if(obj == null){
                    //chua yeu cau lan nao
                    logger.info("Chua request lan nao " + number);
                    setMoneyRequestCountCurrentDay(number, count, curTime, callback);
                    return;
                }

                JsonObject jo = Misc.getStartAndEndCurrentDateInMilliseconds();
                long begin = jo.getLong(Misc.BeginDateInMilliSec,0);
                long end = jo.getLong(Misc.EndDateInMilliSec,0);

                curTime = obj.curtime;
                //trong ngay
                if(curTime >=begin && curTime <= end){

                    logger.info("Trong ngay " + number);

                    if(obj.count < REQUEST_MONEY_MAX_PER_DAY){
                        setMoneyRequestCountCurrentDay(number, obj.count + 1, curTime, callback);
                    }else{
                        callback.handle(obj.count + 1);
                    }
                }else{
                //khac ngay
                    logger.info("Khac ngay " + number);
                    count = 1;
                    curTime = System.currentTimeMillis();
                    setMoneyRequestCountCurrentDay(number, count, curTime, callback);
                }
            }
        });
    }

    public void getInfo(final int number,final Handler<Obj> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.ReqMoneyCols.TABLE);

        //matcher
        match.putNumber(colName.ReqMoneyCols.NUMBER,number);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject fields = new JsonObject();
        fields.putNumber(colName.ReqMoneyCols.CURRENT_TIME, 1);
        fields.putNumber(colName.ReqMoneyCols.COUNT, 1);
        fields.putNumber("_id",0);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonObject jo = message.body().getObject(MongoKeyWords.RESULT,null);
                if(jo != null){
                    callback.handle(new Obj(jo));
                    return;
                }

                callback.handle(null);
            }
        });

    }

    private void setMoneyRequestCountCurrentDay(final int number, final int count, final long curTime, final Handler<Integer> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ReqMoneyCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.ReqMoneyCols.NUMBER, number);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //new Obj
        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.ReqMoneyCols.COUNT,count);
        objNew.putNumber(colName.ReqMoneyCols.CURRENT_TIME,curTime);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, objNew);

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                boolean isUpdated = event.body().getBoolean(MongoKeyWords.IS_UPDATED,false);
                int rCnt = 0;
                if(isUpdated){
                    rCnt = count;
                }
                callback.handle(rCnt);
            }
        });
    }


}
