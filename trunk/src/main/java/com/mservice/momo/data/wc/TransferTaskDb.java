package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.wc.TransferTask;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 7/1/14.
 */
public class TransferTaskDb extends MongoModelController<TransferTask> {
    public TransferTaskDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(TransferTask model) {
        return "wc_transferTask";
    }

    @Override
    public TransferTask newModelInstance() {
        return new TransferTask();
    }

    public void getOneTransferTask(final Handler<TransferTask> callback) {
        TransferTask transferTask = new TransferTask();

        JsonObject matcher = new JsonObject();
        matcher.putObject("money", new JsonObject().putNumber("$gte", 1));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }
                String value = event.body().getObject("result").getString("value", "");
                if (callback != null) {
                    TransferTask m = newModelInstance();
                    m.setModelId(result.getString("_id"));
                    m.setValues(result);
                    callback.handle(m);
                }
            }
        });
    }
}
