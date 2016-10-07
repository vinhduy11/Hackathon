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

/**
 * Created by concu on 6/27/14.
 */
public class MinMaxTranDb {
    private EventBus eventBus;
    private Logger logger;

    public MinMaxTranDb(EventBus eventBus, Logger logger) {
        this.eventBus = eventBus;
        this.logger = logger;
    }

    public static class Obj {
        public String rowid = "";
        public int trantype = 0;
        public String tranname = "";
        public long minvalue = 0;
        public long maxvalue = 0;
        public boolean isnamed = true;
        public String lastmodifyby = "";
        public long lastmodifytime = 0;

        public Obj(JsonObject json) {
            trantype = json.getInteger(colName.MinMaxForTranCols.TRAN_TYPE, 0);
            tranname = json.getString(colName.MinMaxForTranCols.TRAN_NAME, "");
            minvalue = json.getLong(colName.MinMaxForTranCols.MIN_VALUE, 0);
            maxvalue = json.getLong(colName.MinMaxForTranCols.MAX_VALUE, 0);
            isnamed = json.getBoolean(colName.MinMaxForTranCols.IS_NAMED, false);
            rowid = json.getString(colName.MinMaxForTranCols.ROW_ID, "");

            lastmodifyby = json.getString(colName.MinMaxForTranCols.LAST_MODIFY_BY, "system");
            lastmodifytime = json.getLong(colName.MinMaxForTranCols.LAST_MODIFY_TIME, 0);

        }

        public Obj() {
        }

        public JsonObject toJsonObject() {
            JsonObject result = new JsonObject();
            result.putNumber(colName.MinMaxForTranCols.TRAN_TYPE, trantype);
            result.putString(colName.MinMaxForTranCols.TRAN_NAME, tranname);
            result.putNumber(colName.MinMaxForTranCols.MIN_VALUE, minvalue);
            result.putNumber(colName.MinMaxForTranCols.MAX_VALUE, maxvalue);
            result.putBoolean(colName.MinMaxForTranCols.IS_NAMED, isnamed);
            result.putString(colName.MinMaxForTranCols.ROW_ID, rowid);

            result.putString(colName.MinMaxForTranCols.LAST_MODIFY_BY, lastmodifyby);
            result.putNumber(colName.MinMaxForTranCols.LAST_MODIFY_TIME, lastmodifytime);

            return result;
        }

        public boolean isInvalid() {
            return true;
        }
    }

    public void removeObj(String id, final  Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.MinMaxForTranCols.TABLE);

        match.putString(colName.MinMaxForTranCols.ROW_ID, id);

        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void getMinMaxForTran(final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.MinMaxForTranCols.TABLE);

        //sort
        JsonObject fieldSort = new JsonObject();
        fieldSort.putNumber(colName.MinMaxForTranCols.TRAN_TYPE, 1);

        //add sort
        query.putObject(MongoKeyWords.SORT, fieldSort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<Obj> arrayList = null;

                if (results != null && results.size() > 0) {

                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void update(final Obj obj, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.MinMaxForTranCols.TABLE);

        JsonObject match = new JsonObject();
        if (obj.rowid != null && !obj.rowid.isEmpty()) {
            match.putString(colName.MinMaxForTranCols.ROW_ID, obj.rowid);
        } else {
            match.putNumber(colName.MinMaxForTranCols.TRAN_TYPE,obj.trantype);
            match.putBoolean(colName.MinMaxForTranCols.IS_NAMED,obj.isnamed);
        }

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject temp = obj.toJsonObject();
        temp.removeField(colName.MinMaxForTranCols.ROW_ID);

        JsonObject setObj = new JsonObject();
        setObj.putObject(MongoKeyWords.SET_$, temp);

        query.putObject(MongoKeyWords.OBJ_NEW,setObj);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                //
                if (json != null && json.getString(MongoKeyWords.STATUS,"ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void getlist(final Integer tranType, final Boolean isNamed, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.MinMaxForTranCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.MinMaxForTranCols.TRAN_NAME, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();
        if (tranType != null && tranType>0 ){
            match.putNumber(colName.MinMaxForTranCols.TRAN_TYPE, tranType);
        }

        if (isNamed != null){
            match.putBoolean(colName.MinMaxForTranCols.IS_NAMED, isNamed);
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if(results != null && results.size() > 0){
                    arrayList = new ArrayList<>();
                    for(Object o : results){
                        arrayList.add( new Obj((JsonObject)o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void upsert(int trantype
                                    ,String tranname
                                    ,boolean isnamed
                                    ,long minvalue
                                    ,long maxvalue
                                    ,String createby
                                    ,final Handler<Obj> callback) {

        JsonObject jo = new JsonObject();
        jo.putNumber(colName.MinMaxForTranCols.TRAN_TYPE, trantype);
        jo.putString(colName.MinMaxForTranCols.TRAN_NAME, tranname);
        jo.putBoolean(colName.MinMaxForTranCols.IS_NAMED, isnamed);
        jo.putNumber(colName.MinMaxForTranCols.MIN_VALUE, minvalue);
        jo.putNumber(colName.MinMaxForTranCols.MAX_VALUE, maxvalue);
        jo.putString(colName.MinMaxForTranCols.LAST_MODIFY_BY, createby);
        jo.putNumber(colName.MinMaxForTranCols.LAST_MODIFY_TIME, System.currentTimeMillis());

        upsertPartial(jo, callback);
    }

    public void upsertPartial(JsonObject newJsonObj, final Handler<Obj> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);

        query.putString(MongoKeyWords.COLLECTION, colName.MinMaxForTranCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.MinMaxForTranCols.TRAN_TYPE
                , newJsonObj.getInteger(colName.MinMaxForTranCols.TRAN_TYPE));
        match.putBoolean(colName.MinMaxForTranCols.IS_NAMED
                , newJsonObj.getBoolean(colName.MinMaxForTranCols.IS_NAMED));
        query.putObject(MongoKeyWords.MATCHER, match);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                //
                if (json != null && json.getObject(MongoKeyWords.RESULT, null) != null) {
                    Obj result = new Obj(json.getObject(MongoKeyWords.RESULT));
                    callback.handle(result);
                } else {
                    callback.handle(null);
                }
            }
        });


    }
}
