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
 * Created by concu on 7/29/14.
 */
public class PromotedDb {
    private EventBus eventBus;
    private Logger logger;
    public PromotedDb(EventBus eventBus,Logger logger){
        this.eventBus =eventBus;
        this.logger =logger;
    }

    public static class Obj{

        public int INVITER_NUMBER = 0;
        public int INVITEE_NUMBER = 0;
        public String INVITEE_NAME = "";
        public long PROMOTED_TIME = 0;

        public Obj(){}
        public Obj(JsonObject jo){
            INVITER_NUMBER = jo.getInteger(colName.PromotedCols.INVITER_NUMBER,0);
            INVITEE_NUMBER = jo.getInteger(colName.PromotedCols.INVITEE_NUMBER,0);
            INVITEE_NAME = jo.getString(colName.PromotedCols.INVITEE_NAME, "");
            PROMOTED_TIME = jo.getLong(colName.PromotedCols.PROMOTED_TIME, 0);
        }

        public JsonObject toJsonObject(){
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.PromotedCols.INVITER_NUMBER,INVITER_NUMBER);
            jo.putNumber(colName.PromotedCols.INVITEE_NUMBER,INVITEE_NUMBER);
            jo.putString(colName.PromotedCols.INVITEE_NAME, INVITEE_NAME);
            jo.putNumber(colName.PromotedCols.PROMOTED_TIME, PROMOTED_TIME);
            return jo;
        }
    }

    public void save(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.PromotedCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT , obj.toJsonObject());

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

    public void getAllByInviter(final int number, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PromotedCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.PromotedCols.INVITER_NUMBER, number);
        query.putObject(MongoKeyWords.MATCHER, match);

        //sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.PromotedCols.PROMOTED_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        query.putNumber(MongoKeyWords.BATCH_SIZE,10000);

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
}
