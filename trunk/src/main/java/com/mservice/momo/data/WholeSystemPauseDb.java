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
 * Created by concu on 8/5/14.
 */
public class WholeSystemPauseDb {
    EventBus eventBus;
    Logger logger;

    public WholeSystemPauseDb(EventBus eventBus,Logger logger){
        this.eventBus =eventBus;
        this.logger=logger;
    }

    public static class Obj{
        public String ID = "";
        public String CAPTION = "";
        public String BODY = "";
        public boolean ACTIVED= false;
        public long LAST_CHANGED = 0;
        public String CHANGED_BY = "";

        public Obj(){}

        public Obj(JsonObject jo){
            ID = jo.getString(colName.WhoSystemPauseCols.ID,"");
            CAPTION =jo.getString(colName.WhoSystemPauseCols.CAPTION,"");
            BODY =jo.getString(colName.WhoSystemPauseCols.BODY,"");
            ACTIVED= jo.getBoolean(colName.WhoSystemPauseCols.ACTIVED,false);
            LAST_CHANGED = jo.getLong(colName.WhoSystemPauseCols.LAST_CHANGED,0);
            CHANGED_BY = jo.getString(colName.WhoSystemPauseCols.CHANGED_BY,"");
        }

        public JsonObject toJsonObject(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.WhoSystemPauseCols.ID,ID);
            jo.putString(colName.WhoSystemPauseCols.CAPTION, CAPTION);
            jo.putString(colName.WhoSystemPauseCols.BODY, BODY);
            jo.putBoolean(colName.WhoSystemPauseCols.ACTIVED, ACTIVED);
            jo.putNumber(colName.WhoSystemPauseCols.LAST_CHANGED, LAST_CHANGED);
            jo.putString(colName.WhoSystemPauseCols.CHANGED_BY, CHANGED_BY);
            return jo;
        }

        public boolean isInvalid() {
            return true;
        }
    }

    public void getOne(final Handler<Obj> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.WhoSystemPauseCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                Obj arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if(results != null && results.size() > 0){
                    arrayList = new Obj((JsonObject)results.get(0));
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getlist(final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.WhoSystemPauseCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

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

    public void updateID(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.WhoSystemPauseCols.TABLE);

        JsonObject criteria = new JsonObject();

        String id = obj.ID;

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.WhoSystemPauseCols.ID);

        if(id != null &&  !"".equalsIgnoreCase(id)){
            criteria.putString(colName.WhoSystemPauseCols.ID,id);
        }else {
            callback.handle(false);
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

    public void save(Obj o, final Handler<Boolean> callback){

        JsonObject jo = o.toJsonObject();
        jo.removeField(colName.WhoSystemPauseCols.ID);

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.WhoSystemPauseCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT , jo);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                boolean ok = false;
                if (event.body() != null) {
                    ok = true;
                }
                callback.handle(ok);
            }
        });
    }

    public void getCurrentStatus(final Handler<Obj> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.WhoSystemPauseCols.TABLE);

        //matcher
        match.putBoolean(colName.WhoSystemPauseCols.ACTIVED, true);

        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        //limit
        query.putNumber(MongoKeyWords.LIMIT, 1);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Obj obj = null;
                if(message.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        obj= new Obj((JsonObject)results.get(0));
                    }
                }
                callback.handle(obj);
            }
        });
    }


    
}
