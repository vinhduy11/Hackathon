package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nam on 4/28/14.
 */
public class StatisticDb {
    public static final String PRE_COLLECTION_NAME = "statistic_";

    private EventBus eventBus;

    public StatisticDb(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void getSumNumber(String type, Long startDate, Long endDate, final Handler<Long> handler) {

        JsonObject date = null;

        if (startDate != null) {
            date = new JsonObject();
            date.putNumber("$gte", startDate);
        }
        if (endDate != null) {
            if (date == null)
                date = new JsonObject();
            date.putNumber("$lte", endDate);
        }

        JsonObject matcher = new JsonObject();
        if (date != null)
            matcher.putObject("date", date);

        JsonObject sumNumber = new JsonObject()
                .putString("$sum", "$number");

        JsonObject grouper = new JsonObject()
                .putNumber("_id", 0)
                .putObject("sumNumber", sumNumber);

        JsonObject query = new JsonObject();
        query.putString("collection", PRE_COLLECTION_NAME + type);
        query.putString("action", "aggregate");
        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putObject(MongoKeyWords.GROUPER, grouper);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray result = event.body().getArray("result");
                if (result != null && result.size() > 0) {
                    JsonObject obj = result.get(0);
                    Long sumNumber = obj.getLong("sumNumber", 0L);
                    handler.handle(sumNumber);
                    return;
                }
                handler.handle(0L);
            }
        });
    }

    public void increaseDayAction(long date, String type, long number, final Handler<String> handler) {
//        {
//            "action": "find_and_modify",
//                "collection": "counters",
//                "matcher": { "_id": "people" },
//            "update": { "$inc": { "seq": 1 } },
//            "new": true
//        }
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("yyy_MM_dd");

        String dateInString = dateFormat.format(new Date(date));

        JsonObject matcher = new JsonObject()
                .putString("_id", dateInString)
                .putNumber("date", date);

        JsonObject update = new JsonObject()
                .putObject("$inc", new JsonObject()
                                .putNumber("number", number)
                );

        JsonObject query = new JsonObject()
                .putString("action", "find_and_modify")
                .putString("collection", PRE_COLLECTION_NAME + type)
                .putObject("matcher", matcher)
                .putObject("update", update)
                .putBoolean("new", true)
                .putBoolean("upsert", true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (handler != null)
                    handler.handle(event.body().getString("_id"));
            }
        });
    }

    public void increaseMonthAction(long date, String type, long number, final Handler<String> handler) {
//        {
//            "action": "find_and_modify",
//                "collection": "counters",
//                "matcher": { "_id": "people" },
//            "update": { "$inc": { "seq": 1 } },
//            "new": true
//        }
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("yyy_MM");

        String dateInString = dateFormat.format(new Date(date));

        JsonObject matcher = new JsonObject()
                .putString("_id", dateInString)
                .putNumber("date", date);

        JsonObject update = new JsonObject()
                .putObject("$inc", new JsonObject()
                                .putNumber("number", number)
                );

        JsonObject query = new JsonObject()
                .putString("action", "find_and_modify")
                .putString("collection", PRE_COLLECTION_NAME + type)
                .putObject("matcher", matcher)
                .putObject("update", update)
                .putBoolean("new", true)
                .putBoolean("upsert", true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (handler != null)
                    handler.handle(event.body().getString("_id"));
            }
        });
    }
}
