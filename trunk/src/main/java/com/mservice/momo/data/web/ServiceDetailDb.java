package com.mservice.momo.data.web;

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
 * Created by locnguyen on 01/08/2014.
 */
public class ServiceDetailDb {
    private Logger logger;
    private EventBus eventBus;

    public void removeObjByID(String id, final  Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceDetailCols.TABLE);

        match.putString(colName.ServiceDetailCols.ID, id);

//        match.putBoolean(colName.ServiceCols.DELETED,false);
        query.putObject(MongoKeyWords.MATCHER,match);

        //db.products.remove( { qty: { $gt: 20 } } )

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }
    public void removeObj(String serviceId, final  Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceDetailCols.TABLE);

        match.putString(colName.ServiceDetailCols.SERVICE_ID, serviceId);

//        match.putBoolean(colName.ServiceCols.DELETED,false);
        query.putObject(MongoKeyWords.MATCHER,match);

        //db.products.remove( { qty: { $gt: 20 } } )

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }


    public void upsert(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.ServiceDetailCols.TABLE);

        JsonObject criteria = new JsonObject();

        String id = obj.id;

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.ServiceDetailCols.ID);

        if(id != null &&  !"".equalsIgnoreCase(id)){
            criteria.putString(colName.ServiceDetailCols.ID,id);
        }else {
            insertDoc(obj.toJsonObject(),new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    callback.handle(aBoolean);
                }
            });
            return;
        }

        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, data);
        query.putObject(MongoKeyWords.OBJ_NEW, update);

        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void insertDoc(JsonObject json, final Handler<Boolean> callback)
    {
        json.removeField(colName.ServiceDetailCols.ID);
        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection", colName.ServiceDetailCols.TABLE )
                .putObject("document", json);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if(createdId !=null) {
                        callback.handle(true);
                        return;
                    }
                }

                callback.handle(false);
            }
        });
    }

    public void getAll(String sid, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceDetailCols.TABLE);

        //sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServiceDetailCols.ORDER, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        if(sid !=null && !"".equalsIgnoreCase(sid)){
            JsonObject matcher = new JsonObject();
            matcher.putString(colName.ServiceDetailCols.SERVICE_ID,sid);
            query.putObject(MongoKeyWords.MATCHER,matcher);
        }

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

    public void getByServiceId(String serviceId, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceDetailCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServiceDetailCols.LAST_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();

        match.putString(colName.ServiceCols.SERVICE_ID,serviceId);

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

    public static class Obj{
        public String id = "";
        public String serviceId = "";
        public String fieldLabel = "";
        public String fieldType = "";
        public Boolean isAmount = false;
        public Boolean isBillId = false;
        public String key = "";
        public Boolean required = false;
        public long lastTime = 0;
        public int order= 10000;
        public int hasChild = 0;// default la khong co
        public int line = 1; // default 1 line


        public Obj(){
        }

        public Obj(JsonObject input){
            id = input.getString(colName.ServiceDetailCols.ID,"");
            serviceId = input.getString(colName.ServiceDetailCols.SERVICE_ID,"");
            fieldLabel = input.getString(colName.ServiceDetailCols.FIELD_LABEL,"");
            fieldType = input.getString(colName.ServiceDetailCols.FIELD_TYPE,"");
            isAmount = input.getBoolean(colName.ServiceDetailCols.IS_AMOUNT, false);
            isBillId = input.getBoolean(colName.ServiceDetailCols.IS_BILLID, false);
            key = input.getString(colName.ServiceDetailCols.KEY, "");
            required = input.getBoolean(colName.ServiceDetailCols.REQUIRED, false);
            lastTime = input.getLong(colName.ServiceDetailCols.LAST_TIME, 0);
            order = input.getInteger(colName.ServiceDetailCols.ORDER, 10000);
            hasChild = input.getInteger(colName.ServiceDetailCols.HAS_CHILD, 0);
            line = input.getInteger(colName.ServiceDetailCols.LINE, 1);
        }

        public JsonObject toJsonObject(){
            JsonObject output = new JsonObject();

            output.putString(colName.ServiceDetailCols.ID,id);
            output.putString(colName.ServiceDetailCols.SERVICE_ID,serviceId);
            output.putString(colName.ServiceDetailCols.FIELD_LABEL,fieldLabel);
            output.putString(colName.ServiceDetailCols.FIELD_TYPE,fieldType);
            output.putBoolean(colName.ServiceDetailCols.IS_AMOUNT, isAmount);
            output.putBoolean(colName.ServiceDetailCols.IS_BILLID,isBillId);
            output.putString(colName.ServiceDetailCols.KEY, key);
            output.putBoolean(colName.ServiceDetailCols.REQUIRED, required);
            output.putNumber(colName.ServiceDetailCols.LAST_TIME, lastTime);
            output.putNumber(colName.ServiceDetailCols.ORDER, order);
            output.putNumber(colName.ServiceDetailCols.HAS_CHILD, hasChild);
            output.putNumber(colName.ServiceDetailCols.LINE, line);
            return output;
        }

        public boolean isInvalid() {
            return true;
        }
    }

    public ServiceDetailDb(EventBus eventBus, Logger logger){
        this.logger =logger;
        this.eventBus =eventBus;
    }
}
