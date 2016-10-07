package com.mservice.momo.data.ironmanpromote;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 9/21/15.
 */
public class AndroidDataUserDb {

    /**
     * Created by concu on 9/8/15.
     */

    private Vertx vertx;
    private Logger logger;

    public AndroidDataUserDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final String id, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.AndroidDataUser.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, new JsonObject().putString(colName.AndroidDataUser.ID, id));


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if (!event.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(event.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }

    public static class Obj {

        public String id = "";


        public Obj() {
        }

        public Obj(JsonObject jo) {

            id = jo.getString(colName.AndroidDataUser.ID, "").trim();


        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.AndroidDataUser.ID, id.trim());

            return jo;
        }
    }

}
