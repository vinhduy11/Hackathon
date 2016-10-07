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
 * Created by locnguyen on 25/07/2014.
 * @author  locnguyen
 * @version 1.0
 * @since   2014-07-25
 */
public class UserWebAdminDb {

    EventBus eventBus;
    Logger logger;

    public UserWebAdminDb(EventBus eb, Logger log) {
        eventBus = eb;
        logger = log;
    }


    public static class Obj{
        public String username = "";
        public String password = "";
        public int rollType = 0;
        public long sessionId = 0;
        public long lastActTime = 0;

        public Obj(){

        }

        public Obj(JsonObject input){
            username = input.getString(colName.UserWebadmin.USERNAME,"");
            password = input.getString(colName.UserWebadmin.PASSWORD,"");
            sessionId = input.getLong(colName.UserWebadmin.SESSIONID,-1);
            rollType = input.getInteger(colName.UserWebadmin.ROLL_TYPE,-1);
//            lastActTime = input.getLong(colName.UserWebadmin.LASTACTIONTIME);
        }

        public JsonObject toJsonObject(){
            JsonObject json = new JsonObject();

            json.putString(colName.UserWebadmin.USERNAME,username);
            json.putString(colName.UserWebadmin.PASSWORD,password);
            json.putNumber(colName.UserWebadmin.SESSIONID, sessionId);
            json.putNumber(colName.UserWebadmin.ROLL_TYPE, rollType);
//            json.putNumber(colName.UserWebadmin.LASTACTIONTIME, lastActTime);

            return json;
        }
    }


    public void upsertUser(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.UserWebadmin.TABLE);

        JsonObject match = new JsonObject();
        match.putString(colName.UserWebadmin.USERNAME, obj.username);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, obj.toJsonObject());

        query.putObject(MongoKeyWords.OBJ_NEW, set);
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

    /**
     * lay ve thong tin 1 user
     * @param username  ten user
     * @param password  mat khau
     * @param pageNum   trang thu may
     * @param pageSize  so luong dong 1 trang
     * @param callback  doi tuong callback truyen vao cac dong lay duoc
     */

    public void getUserInfo(String username
            , String password
            , int pageNum
            , int pageSize
            , final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.UserWebadmin.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match
        JsonObject match = new JsonObject();
        if (username!=null & !"".equalsIgnoreCase(username)) {
            match.putString(colName.UserWebadmin.USERNAME, username);
        }
        else
        {
            callback.handle(null);
            return;
        }

        if (password != null && !"".equalsIgnoreCase(password)) {
            match.putString(colName.UserWebadmin.PASSWORD, password);
        }
        else {
            callback.handle(null);
            return;
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        int skip = (pageNum - 1) * pageSize;
        int records = pageNum * pageSize;

        query.putNumber(MongoKeyWords.SKIP, skip);
        query.putNumber(MongoKeyWords.LIMIT, records);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jo) {
                JsonObject obj = jo.body();
                ArrayList<Obj> listObj  = null;
                if ( obj != null
                        && obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY) != null
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY).size() >0
                        ) {

                    listObj = new ArrayList<Obj>();
                    for (int i=0; i<jo.body().getArray(MongoKeyWords.RESULT_ARRAY).size(); i++) {
                        JsonObject o = jo.body().getArray(MongoKeyWords.RESULT_ARRAY).get(i);
                        listObj.add(new Obj(o));
                    }

                }

                callback.handle(listObj);
            }
        });
    }


}
