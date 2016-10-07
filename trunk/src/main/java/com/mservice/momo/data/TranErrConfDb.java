package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by locnguyen on 25/07/2014.
 */
public class TranErrConfDb {

    EventBus eventBus;
    Logger logger;

    public TranErrConfDb(EventBus eb, Logger log) {
        eventBus = eb;
        logger = log;
    }

    public static class Obj {
        public String ID="";
        public int errorCode = 0;
        public String desciption = "";
        public int tranType = 0;
        public String tranName = "";
        public String contentTranHis = "";
        public String notiTitle = "";
        public String notiBody = "";

        public Obj() {}

        /***
         *
         * @param input
         */
        public Obj(JsonObject input){
            ID = input.getString(colName.TranErrorConfig.ID,"");
            errorCode = input.getInteger(colName.TranErrorConfig.ERROR_CODE,-1);
            desciption = input.getString(colName.TranErrorConfig.DESCRIPTION,"");
            tranType = input.getInteger(colName.TranErrorConfig.TRAN_TYPE,-1);
            tranName = input.getString(colName.TranErrorConfig.TRAN_NAME,"");
            contentTranHis = input.getString(colName.TranErrorConfig.CONTENT_TRANHIS,"");
            notiTitle = input.getString(colName.TranErrorConfig.NOTI_TITLE,"");
            notiBody = input.getString(colName.TranErrorConfig.NOTI_BODY,"");
        }

        public JsonObject toJsonObject(){
            JsonObject json = new JsonObject();

            json.putString(colName.TranErrorConfig.ID,ID);
            json.putNumber(colName.TranErrorConfig.ERROR_CODE,errorCode);
            json.putNumber(colName.TranErrorConfig.TRAN_TYPE,tranType);
            json.putString(colName.TranErrorConfig.TRAN_NAME, tranName);
            json.putString(colName.TranErrorConfig.DESCRIPTION, desciption);
            json.putString(colName.TranErrorConfig.CONTENT_TRANHIS, contentTranHis);
            json.putString(colName.TranErrorConfig.NOTI_TITLE, notiTitle);
            json.putString(colName.TranErrorConfig.NOTI_BODY, notiBody);

            return json;
        }

        public boolean isInvalid(){
            return true;
        }
    }

    /***
     *
     * @param obj
     * @param callback
     */
    public void upsertError(String id, Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.TranErrorConfig.TABLE);

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.TranErrorConfig.ID);

        JsonObject criteria = new JsonObject();

        if(id != null &&  !"".equalsIgnoreCase(id)){
            criteria.putString(colName.TranErrorConfig.ID,id);
        }else {
            criteria.putNumber(colName.TranErrorConfig.ERROR_CODE,-10000000);
        }

        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, data);
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
    public void upsertErrorOld(String id, Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);

        query.putString(MongoKeyWords.COLLECTION, colName.TranErrorConfig.TABLE);

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.TranErrorConfig.ID);

        JsonObject match = new JsonObject();

        if(id != null &&  !"".equalsIgnoreCase(id)){
            match.putString(colName.TranErrorConfig.ID,id);
        }else {
            match.putNumber(colName.TranErrorConfig.ERROR_CODE,-10000000);
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, data);
        query.putObject(MongoKeyWords.UPDATE, update);

        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

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

    public void getErrorInfo(int error_code
                            ,int tran_type
                            ,int pageNum
                            ,int pageSize
                            ,final Handler<Obj> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranErrorConfig.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match
        JsonObject match = new JsonObject();

        if (error_code != -1) {
            match.putNumber(colName.TranErrorConfig.ERROR_CODE, error_code);
        }
        if (tran_type != -1) {
            match.putNumber(colName.TranErrorConfig.TRAN_TYPE, tran_type);
        }

        if (match.size()>0)
            query.putObject(MongoKeyWords.MATCHER, match);

        int skip = (pageNum - 1) * pageSize;
        int records = pageNum * pageSize;

        query.putNumber(MongoKeyWords.SKIP, skip);
        query.putNumber(MongoKeyWords.LIMIT, records);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jo) {
                JsonObject obj = jo.body();
                Obj listObj  = null;
                if ( obj != null
                        && obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY) != null
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY).size() >0
                        ) {

                    listObj = new Obj( (JsonObject)jo.body().getArray(MongoKeyWords.RESULT_ARRAY).get(0));
                }

                callback.handle(listObj);
            }
        });
    }

    public void getListError(int error_code
            ,int tran_type
            ,int pageNum
            ,int pageSize
            ,final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranErrorConfig.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match
        JsonObject match = new JsonObject();

        if (error_code != -1) {
            match.putNumber(colName.TranErrorConfig.ERROR_CODE, error_code);
        }
        if (tran_type != -1) {
            match.putNumber(colName.TranErrorConfig.TRAN_TYPE, tran_type);
        }

        if (match.size() > 0)
            query.putObject(MongoKeyWords.MATCHER, match);

        int skip = (pageNum - 1) * pageSize;
        int records = pageNum * pageSize;

        query.putNumber(MongoKeyWords.SKIP, skip);
        query.putNumber(MongoKeyWords.LIMIT, records);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jo) {
                JsonObject obj = jo.body();
                ArrayList<Obj> listObj = null;
                if (obj != null
                        && obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY) != null
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY).size() > 0
                        ) {

                    listObj = new ArrayList<>();
                    for (int i = 0; i < jo.body().getArray(MongoKeyWords.RESULT_ARRAY).size(); i++) {
                        JsonObject o = jo.body().getArray(MongoKeyWords.RESULT_ARRAY).get(i);
                        listObj.add(new Obj(o));
                    }

                }

                callback.handle(listObj);
            }
        });
    }

}
