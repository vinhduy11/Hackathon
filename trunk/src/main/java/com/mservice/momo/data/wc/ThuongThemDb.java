package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.wc.ThuongThem;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 6/26/14.
 */
public class ThuongThemDb extends MongoModelController<ThuongThem> {
    public ThuongThemDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(ThuongThem model) {
        return "wc_thuongThem_" + model.matchId;
    }

    @Override
    public ThuongThem newModelInstance() {
        return new ThuongThem();
    }


    public void initThuongThem(String matchId, final Handler<JsonObject> callback) {
        JsonObject request = new JsonObject();
        request.putString("matchId", matchId);
        request.putString("action", "initThuongThem");
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }

    public void getOneThuongThem(final String matchId, final Handler<ThuongThem> callback){
        ThuongThem thuongThem = new ThuongThem();
        thuongThem.matchId = matchId;

        JsonObject matcher = new JsonObject();
        matcher.putObject("money", new JsonObject().putNumber("$gte", 1));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(thuongThem));
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
                    ThuongThem m = newModelInstance();
                    m.setModelId(result.getString("_id"));
                    m.setValues(result);
                    m.matchId = matchId;
                    callback.handle(m);
                }
            }
        });
    }
}
