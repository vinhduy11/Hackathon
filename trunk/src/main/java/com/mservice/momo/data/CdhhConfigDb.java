package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.CdhhConfig;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;


/**
 * Created by nam on 10/29/14.
 */
public class CdhhConfigDb extends MongoModelController<CdhhConfig> {

    public CdhhConfigDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(CdhhConfig model) {
        return "cdhhConfig";
    }

    @Override
    public CdhhConfig newModelInstance() {
        return new CdhhConfig();
    }

    public void getActivePeriod(final Handler<CdhhConfig> callback) {
        CdhhConfig filter = new CdhhConfig();
        filter.active = true;
        findOne(filter, new org.vertx.java.core.Handler<CdhhConfig>() {
            @Override
            public void handle(CdhhConfig result) {
                callback.handle(result);
            }
        });
    }

    public void setActive(final String cdhhConfigId, final String serviceId, final Handler<JsonObject> callback) {
        final CdhhConfig filter = new CdhhConfig();
        filter.serviceId = serviceId;
        CdhhConfig newValue = new CdhhConfig();
        newValue.active = false;
        newValue.serviceId = serviceId;
        updateMulti(filter, newValue, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                filter.setModelId(cdhhConfigId);
                filter.active = true;
                update(filter, false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
                        callback.handle(new JsonObject().putNumber("error", 0));
                    }
                });
            }
        });
    }

    public void getActiveCollection(String serviceId, String collection, final Handler<CdhhConfig> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));

        if (collection.equalsIgnoreCase("")) {
            match.putString("serviceId", serviceId);
            match.putBoolean("report", true);
            query.putObject(MongoKeyWords.MATCHER, match);
        } else {
            match.putString("serviceId", serviceId);
            match.putString("collName", collection);
            query.putObject(MongoKeyWords.MATCHER, match);
        }

        //sort
//        JsonObject sort = new JsonObject();
//        sort.putNumber("endTime", -1);
//
//        query.putObject(MongoKeyWords.SORT,sort);
//        query.putNumber(MongoKeyWords.LIMIT,1);


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject jo = message.body().getObject(MongoKeyWords.RESULT, null);

                CdhhConfig cdhhConfig = null;
                if (jo != null) {
                    cdhhConfig = new CdhhConfig();
                    cdhhConfig.serviceId = jo.getString("serviceId", "");
                    cdhhConfig.collName = jo.getString("collName", "");
                    cdhhConfig.active = jo.getBoolean("active");
                    cdhhConfig.minCode = jo.getInteger("minCode", 0);
                    cdhhConfig.maxCode = jo.getInteger("maxCode", 0);
                }
                callback.handle(cdhhConfig);
            }
        });
    }

    public void getVoteResult(String collectionName, String serviceId, final Handler<ArrayList<VotedObj>> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, collectionName);

        match.putString("serviceid", serviceId);

        query.putObject(MongoKeyWords.MATCHER, match);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000000);

        JsonObject sort = new JsonObject();
        sort.putNumber("time", -1);

        query.putObject(MongoKeyWords.SORT, sort);
         /*{
        "_id" : "970ccc8c-f06a-4c3c-bf15-7a4c42c20f56",
            "number" : 974540385,
            "value" : NumberLong(25000),
            "vamount" : NumberLong(6),
            "timevn" : "23/01/2015 17:42:48",
            "code" : "1",
            "time" : NumberLong(1422009768315),
            "dayvn" : "23/01/2015",
            "tid" : NumberLong(3862019),
            "name" : "LINH",
            "serviceid" : "remix"
    }*/


        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 0);
        fields.putNumber("number", 1);
        fields.putNumber("value", 1);
        fields.putNumber("vamount", 1);
        fields.putNumber("timevn", 1);
        fields.putNumber("code", 1);
        fields.putNumber("name", 1);
        //fields.putNumber("serviceid",1);

        query.putObject(MongoKeyWords.KEYS, fields);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<VotedObj> arrayList = new ArrayList<VotedObj>();
                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY, null);

                if (array != null && array.size() > 0) {
                    for (int i = 0; i < array.size(); i++) {
                        arrayList.add(new VotedObj((JsonObject) array.get(i)));
                    }
                }
                callback.handle(arrayList);
            }
        });


    }

    public void getVoteResultByArray(String collectionName, String serviceId, final Handler<JsonArray> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, collectionName);

        match.putString("serviceid", serviceId);

        query.putObject(MongoKeyWords.MATCHER, match);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000000);


         /*{
        "_id" : "970ccc8c-f06a-4c3c-bf15-7a4c42c20f56",
            "number" : 974540385,
            "value" : NumberLong(25000),
            "vamount" : NumberLong(6),
            "timevn" : "23/01/2015 17:42:48",
            "code" : "1",
            "time" : NumberLong(1422009768315),
            "dayvn" : "23/01/2015",
            "tid" : NumberLong(3862019),
            "name" : "LINH",
            "serviceid" : "remix"
    }*/


        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 0);
        fields.putNumber("number", 1);
        fields.putNumber("value", 1);
        fields.putNumber("vamount", 1);
        fields.putNumber("timevn", 1);
        fields.putNumber("code", 1);
        fields.putNumber("name", 1);
        //fields.putNumber("serviceid",1);

        query.putObject(MongoKeyWords.KEYS, fields);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray array = message.body().getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray());

                callback.handle(array);
            }
        });


    }

    public static class VotedObj {
        public int number = 0;
        public int value = 0;
        public int votedAmount = 0;
        public String timevn = "";
        public String code = "";
        public String name = "";
        //public String serviceId="";

        public VotedObj(JsonObject jo) {
            number = jo.getInteger("number", 0);
            value = jo.getInteger("value", 0);
            votedAmount = jo.getInteger("vamount", 0);
            timevn = jo.getString("timevn", "");
            code = jo.getString("code", "");
            name = jo.getString("name", "noname");
            //serviceId = jo.getString("serviceid", "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putNumber("number", number);
            jo.putNumber("value", value);
            jo.putNumber("vamount", votedAmount);
            jo.putString("timevn", timevn);
            jo.putString("code", code);
            jo.putString("name", name);
            //jo.putString("serviceid",serviceId);
            return jo;
        }
    }

}
