package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.wc.Match;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.*;

/**
 * Created by nam on 6/8/14.
 */
public class MatchDb extends MongoModelController<Match> {
    public MatchDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(Match model) {
        return "wc_match";
    }

    @Override
    public Match newModelInstance() {
        return new Match();
    }



    public void getSortedList(final Match filter, final Handler<Collection<Match>> callback) {
        JsonObject matcher = null;
        if (filter!=null) {
            matcher = filter.getPersisFields();
            if (filter.getModelId() != null && !filter.getModelId().isEmpty()) {
                matcher.putString("_id", filter.getModelId());
            }
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(filter));
        query.putObject(MongoKeyWords.SORT_$, new JsonObject().putNumber("_id", -1));
        if (matcher != null)
            query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
//                ArrayList<Match> mList = new ArrayList<Match>();
                SortedSet<Match> mList = new TreeSet<Match>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            Match m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(filter) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }


}
