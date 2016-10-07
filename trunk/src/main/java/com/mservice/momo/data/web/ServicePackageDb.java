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
 * Created by locnguyen on 12/08/2014.
 */
public class ServicePackageDb {

    private Logger logger;
    private EventBus eventBus;

    public void update(final JsonObject obj, final Handler<JsonObject> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServicePackage.TABLE);

        JsonObject criteria   = new JsonObject();
        criteria.putString(colName.ServicePackage.ID,obj.getString(colName.ServicePackage.ID));

        query.putObject(MongoKeyWords.CRITERIA, criteria);

        obj.removeField(colName.ServicePackage.ID);
        obj.removeField(colName.ServicePackage.DESCRIPTION);

        JsonObject upsert = new JsonObject();
        upsert.putObject(MongoKeyWords.SET_$,obj);

        query.putObject(MongoKeyWords.OBJ_NEW, upsert);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                callback.handle(obj);
            }
        });
    }

    public void removeObj(String id, final  Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServicePackage.TABLE);

        match.putString(colName.ServicePackage.ID, id);

        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void getlist(final String serviceID
                            ,final String packageType
                            ,final String parentid
                            ,final String linkto
                            ,final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServicePackage.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServicePackage.SERVICE_ID, 1);
        sort.putNumber(colName.ServicePackage.ORDER,1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();
        if (serviceID != null && !serviceID.isEmpty()){
            match.putString(colName.ServicePackage.SERVICE_ID,serviceID);
        }

        if (packageType != null && !packageType.isEmpty()){
            match.putString(colName.ServicePackage.PACKAGE_TYPE,packageType);
        }
        if(linkto != null && !linkto.isEmpty()){
            match.putString(colName.ServicePackage.LINKTODROPBOX,linkto);
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
                        Obj obj =new Obj((JsonObject)o);
                        arrayList.add(obj);
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }


    public void getAsArray(final String serviceID
            ,final String packageType
            ,final String parentid
            ,final Handler<JsonArray> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServicePackage.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServicePackage.SERVICE_ID, 1);
        sort.putNumber(colName.ServicePackage.LAST_TIME,-1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();
        if (serviceID != null && !serviceID.isEmpty()){
            match.putString(colName.ServicePackage.SERVICE_ID,serviceID);
        }

        if (packageType != null && !packageType.isEmpty()){
            match.putString(colName.ServicePackage.PACKAGE_TYPE,packageType);
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray());
                // return default value
                callback.handle(results);
            }
        });
    }



    public void upsertID(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.ServicePackage.TABLE);

        JsonObject criteria = new JsonObject();

        String id = obj.id;

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.ServicePackage.ID);

        if(id != null &&  !"".equalsIgnoreCase(id)){
            criteria.putString(colName.ServicePackage.ID,id);
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
                if (obj!= null && obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void insertDoc(JsonObject json, final Handler<Boolean> callback)
    {
        json.removeField(colName.ServicePackage.ID);

        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection", colName.ServicePackage.TABLE )
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

    public static class Obj{
        public String id = "";
        public String serviceID = "";
        public String serviceName = "";
        public String packageType = "";
        public String packageName = "";
        public String packageValue = "";
        public String description = "";
        public String linktodropbox = "";
        public long lasttime = 0;
        public String parentid = "";
        public String parentname = "";
        public int order = 100000;

        public Obj(){
        }

        public Obj(JsonObject input){
            id = input.getString(colName.ServicePackage.ID,"");
            serviceID = input.getString(colName.ServicePackage.SERVICE_ID,"");
            serviceName = input.getString(colName.ServicePackage.SERVICE_NAME,"");
            packageType = input.getString(colName.ServicePackage.PACKAGE_TYPE,"");
            packageName = input.getString(colName.ServicePackage.PACKAGE_NAME,"");
            packageValue = input.getString(colName.ServicePackage.PACKAGE_VALUE,"");
            description = input.getString(colName.ServicePackage.DESCRIPTION,"");
            linktodropbox = input.getString(colName.ServicePackage.LINKTODROPBOX,"");
            lasttime = input.getLong(colName.ServicePackage.LAST_TIME,0);
            parentid = input.getString(colName.ServicePackage.PARENT_ID,"");
            parentname = input.getString(colName.ServicePackage.PARENT_NAME,"");
            order = input.getInteger(colName.ServicePackage.ORDER,100000);

        }

        public JsonObject toJsonObject(){
            JsonObject output = new JsonObject();

            output.putString(colName.ServicePackage.ID,id);
            output.putString(colName.ServicePackage.SERVICE_ID,serviceID);
            output.putString(colName.ServicePackage.SERVICE_NAME,serviceName);
            output.putString(colName.ServicePackage.PACKAGE_TYPE,packageType);
            output.putString(colName.ServicePackage.PACKAGE_NAME,packageName);
            output.putString(colName.ServicePackage.PACKAGE_VALUE, packageValue);
            output.putString(colName.ServicePackage.DESCRIPTION,description);
            output.putString(colName.ServicePackage.LINKTODROPBOX,linktodropbox);
            output.putNumber(colName.ServicePackage.LAST_TIME,lasttime);
            output.putString(colName.ServicePackage.PARENT_ID,parentid);
            output.putString(colName.ServicePackage.PARENT_NAME,parentname);
            output.putNumber(colName.ServicePackage.ORDER,order);

            return output;
        }

        public boolean isInvalid() {
            return true;
        }
    }

    public ServicePackageDb(EventBus eventBus, Logger logger){
        this.logger =logger;
        this.eventBus =eventBus;
    }
}
