package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 5/23/15.
 */
public class EventContentDb {

    private Vertx vertx;
    private Logger logger;
    public EventContentDb(Vertx vertx, Logger logger){
        this.logger =logger;
        this.vertx =vertx;
    }


    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.eventContent.TABLE);

        if(filter != null && filter.getFieldNames().size() > 0){
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if(joArr != null && joArr.size() > 0){
                    for (int i =0;i< joArr.size();i++){
                        Obj obj = new Obj((JsonObject)joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }


    public void findOne(String serId, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.eventContent.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.eventContent.ID, serId);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public static class Obj {

        public String id = ""; // wallet will be get voucher

        public JsonObject cap = new JsonObject();  // ma nhom khuyen mai, 1,2.....

        //promoted
        public JsonObject body = new JsonObject(); // so luong da khuyen mai


        public Obj() {}

        public Obj(JsonObject jo) {

           id = jo.getString(colName.eventContent.ID, "");
           cap = jo.getObject(colName.eventContent.CAP, new JsonObject());
           body = jo.getObject(colName.eventContent.BODY, new JsonObject());

        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.eventContent.ID, id);
            jo.putObject(colName.eventContent.CAP, cap);
            jo.putObject(colName.eventContent.BODY, body);

            return jo;
        }
    }
}
