package com.mservice.momo.data;

import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by User on 3/13/14.
 */
public class PartnerTransId {

    EventBus eventBus;
    public PartnerTransId(EventBus eb){
        eventBus = eb;
    }

    public void getSeed(String collectionName, final Handler<Integer> callback ){
        JsonObject query = new JsonObject();

        query.putString("action", "findAndModify");
        query.putString("collection", collectionName);

        query.putObject("sort", new JsonObject("{}"));

        JsonObject objNew = new JsonObject();

        objNew.putNumber("seed", 1);

        JsonObject incObj = new JsonObject();
        incObj.putObject("$inc",objNew);

        //update: { $inc: { seq: 1 } },

        JsonObject update = new JsonObject();
        query.putObject("update", incObj);
        query.putBoolean("upsert", true);
        query.putBoolean("new", true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                if(json!= null){
                    callback.handle(json.getObject("result").getInteger("seed"));
                }
                else {
                    callback.handle(0);
                }
            }
        });

    }
}
