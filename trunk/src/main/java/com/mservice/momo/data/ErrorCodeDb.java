
package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import java.util.ArrayList;


/**
 * Created by manhly on 25/07/2014.
 */
public class ErrorCodeDb {
    private Vertx vertx;
    EventBus eventBus;
    Logger logger;


    public ErrorCodeDb(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }
    public static class Obj {
        public String errorCode = "";
        public String description = "";
        public Obj() {}

        /***
         *
         * @param jo
         */
        public Obj(JsonObject jo){
            errorCode = jo.getString(colName.ErrorCodeMgtCols.ERROR_CODE,"");
            description = jo.getString(colName.ErrorCodeMgtCols.DESCRIPTION,"");

        }

        public JsonObject toJsonObject(){
            JsonObject json = new JsonObject();
            json.putString(colName.ErrorCodeMgtCols.ERROR_CODE,errorCode);
            json.putString(colName.ErrorCodeMgtCols.DESCRIPTION, description);
            return json;
        }

        public boolean isInvalid(){
            return true;
        }
    }


    public  void upsertError(Obj ob, final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = ob.toJsonObject();
        //newJsonObj.removeField(colName.ErrorCodeMgtCols.ERROR_CODE);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ErrorCodeMgtCols.TABLE);

        JsonObject match   = new JsonObject();
        match.putString(colName.ErrorCodeMgtCols.ERROR_CODE, ob.errorCode);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //fields set
        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, newJsonObj);

        //set
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
    public void getAll(final Handler<ArrayList<ErrorCodeDb.Obj>> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ErrorCodeMgtCols.TABLE);

        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ErrorCodeMgtCols.ERROR_CODE, 1);

        query.putObject(MongoKeyWords.SORT,sort);

        query.putNumber(MongoKeyWords.BATCH_SIZE,10000);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<ErrorCodeDb.Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray array = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    if(array !=null && array.size() >0){
                        for (Object o : array){
                            arrayList.add( new ErrorCodeDb.Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void removeObj(String error_code, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ErrorCodeMgtCols.TABLE);

        match.putString(colName.ErrorCodeMgtCols.ERROR_CODE, error_code);

        query.putObject(MongoKeyWords.MATCHER, match);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }



}
