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
 * Created by concu on 4/17/14.
 */
public class DepositInPlace {

    public static class Obj{

        public int number;
        public String address;
        public String name;
        public long amount;
        public long create_time;
        public long approve_time;
        public int approve_by;
        public String status;
        public int error =0;
        public String remark;
        public Obj(JsonObject jObj){

            number =jObj.getInteger(colName.DepositInPlaceDBCols.NUMBER,0);
            address=jObj.getString(colName.DepositInPlaceDBCols.ADDRESS,"");
            name=jObj.getString(colName.DepositInPlaceDBCols.FULL_NAME,"");
            amount=jObj.getLong(colName.DepositInPlaceDBCols.AMOUNT,0);
            create_time=jObj.getLong(colName.DepositInPlaceDBCols.CREATE_TIME, 0);
            approve_time=jObj.getLong(colName.DepositInPlaceDBCols.APPROVE_TIME,0);
            approve_by=jObj.getInteger(colName.DepositInPlaceDBCols.APPROVE_BY, 0);
            status = jObj.getString(colName.DepositInPlaceDBCols.STATUS,"");

            error=jObj.getInteger(colName.DepositInPlaceDBCols.ERROR,0);
            remark = jObj.getString(colName.DepositInPlaceDBCols.REMARK,"");
        }
    }

    private EventBus eventBus;
    private Logger logger;
    private PhonesDb phonesDb;
    public DepositInPlace(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
        phonesDb = new PhonesDb(eb,log);
    }

    public void upsertOne(final int number,final String address, final String name, final long amount, final Handler<Boolean> callback){

        phonesDb.getPhoneObjInfo(number,new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {

                JsonObject objNew = buildOneRow(number,address,name,amount);
                if(name.equalsIgnoreCase("")){
                    objNew.putString(colName.DepositInPlaceDBCols.FULL_NAME, obj != null ? obj.name : "");
                }

                upsertRow(objNew,callback);
            }
        });
    }

    public void approve(final int number,final long create_time
            ,final String action,final int approved_number,final long amount
            ,final int error,final String remark,final Handler<Boolean> callback){

        getRow2Approve(number, new Handler<Obj>() {
            @Override
            public void handle(Obj obj) {

                if (obj != null && obj.number == number && obj.create_time == create_time && obj.amount == amount
                        && obj.status.equals(colName.DepositInPlaceDBCols.Status.getStatus(colName.DepositInPlaceDBCols.Status.NEW))) {

                    obj.approve_time = System.currentTimeMillis();
                    obj.approve_by = approved_number;
                    obj.status = action;
                    obj.error = error;
                    obj.remark = remark;

                    JsonObject objNew = fromObj2Json(obj);
                    upsertRow(objNew, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            logger.info("Approved for number, action, result " + number + "," + action + "," + aBoolean);
                            callback.handle(aBoolean);
                        }
                    });

                } else {
                    logger.info("Can not find the record to approve number, action " + number + "," + action);
                    callback.handle(false);
                }
            }
        });
    }

    public void getRow2Approve(final int number,final Handler<Obj> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.DepositInPlaceDBCols.TABLE);

        match.putNumber(colName.DepositInPlaceDBCols.NUMBER, number);
        match.putString(colName.DepositInPlaceDBCols.STATUS
                , colName.DepositInPlaceDBCols.Status.getStatus(colName.DepositInPlaceDBCols.Status.NEW));
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                Obj obj = null;

                JsonObject results = jsonObjectMessage.body().getObject(MongoKeyWords.RESULT);
                if (results != null) {
                    obj = new Obj(results);
                }
                callback.handle(obj);
            }
        });
    }

    public void getRow2ApproveOld(final int number,final Handler<Obj> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.DepositInPlaceDBCols.TABLE);

        match.putNumber(colName.DepositInPlaceDBCols.NUMBER, number);
        match.putString(colName.DepositInPlaceDBCols.STATUS
                , colName.DepositInPlaceDBCols.Status.getStatus(colName.DepositInPlaceDBCols.Status.NEW));
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                Obj obj = null;

                if(jsonObjectMessage.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray results = jsonObjectMessage.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        JsonObject item = results.get(0);
                        obj = new Obj(item);
                    }
                }
                callback.handle(obj);
            }
        });
    }

    private void upsertRow(final JsonObject objNew
                           ,final Handler<Boolean> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.DepositInPlaceDBCols.TABLE);

        //matcher
        JsonObject criteria = new JsonObject();
        criteria.putNumber(colName.DepositInPlaceDBCols.NUMBER, objNew.getInteger(colName.DepositInPlaceDBCols.NUMBER,0));
        criteria.putString(colName.DepositInPlaceDBCols.STATUS,
                colName.DepositInPlaceDBCols.Status.getStatus(colName.DepositInPlaceDBCols.Status.NEW));


        query.putObject(MongoKeyWords.CRITERIA, criteria);

        //sort
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

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
                    callback.handle(result);
                }
                else {
                    callback.handle(false);
                }
            }
        });
    }

    private void upsertRowOld(final JsonObject objNew
                           ,final Handler<Boolean> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.DepositInPlaceDBCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.DepositInPlaceDBCols.NUMBER, objNew.getInteger(colName.DepositInPlaceDBCols.NUMBER,0));
        match.putString(colName.DepositInPlaceDBCols.STATUS,
                colName.DepositInPlaceDBCols.Status.getStatus(colName.DepositInPlaceDBCols.Status.NEW));


        query.putObject(MongoKeyWords.MATCHER, match);

        //sort
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

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
                    callback.handle(result);
                }
                else {
                    callback.handle(false);
                }
            }
        });
    }

    private JsonObject buildOneRow(int number,String address, String name, long amount){

        JsonObject o = new JsonObject();
        o.putNumber(colName.DepositInPlaceDBCols.NUMBER,number);
        o.putString(colName.DepositInPlaceDBCols.ADDRESS,address);
        o.putString(colName.DepositInPlaceDBCols.FULL_NAME,name);
        o.putNumber(colName.DepositInPlaceDBCols.AMOUNT,amount);
        o.putNumber(colName.DepositInPlaceDBCols.CREATE_TIME,System.currentTimeMillis());
        o.putNumber(colName.DepositInPlaceDBCols.APPROVE_TIME,0);
        o.putNumber(colName.DepositInPlaceDBCols.APPROVE_BY,0);
        o.putString(colName.DepositInPlaceDBCols.STATUS
                                ,colName.DepositInPlaceDBCols.Status.getStatus(colName.DepositInPlaceDBCols.Status.NEW));

        o.putNumber(colName.DepositInPlaceDBCols.ERROR,0);
        o.putString(colName.DepositInPlaceDBCols.REMARK,"");
        return  o;
    }

    private JsonObject fromObj2Json(Obj o){
        JsonObject j = new JsonObject();
        j.putNumber(colName.DepositInPlaceDBCols.NUMBER,o.number);
        j.putString(colName.DepositInPlaceDBCols.ADDRESS,o.address);
        j.putString(colName.DepositInPlaceDBCols.FULL_NAME,o.name);
        j.putNumber(colName.DepositInPlaceDBCols.AMOUNT,o.amount);
        j.putNumber(colName.DepositInPlaceDBCols.CREATE_TIME,o.create_time);
        j.putNumber(colName.DepositInPlaceDBCols.APPROVE_TIME,o.approve_time);
        j.putNumber(colName.DepositInPlaceDBCols.APPROVE_BY,o.approve_by);
        j.putString(colName.DepositInPlaceDBCols.STATUS,o.status);
        j.putNumber(colName.DepositInPlaceDBCols.ERROR,0);
        j.putString(colName.DepositInPlaceDBCols.REMARK,"");
        return j;
    }

}
