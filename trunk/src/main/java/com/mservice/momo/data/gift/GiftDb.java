package com.mservice.momo.data.gift;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.models.Gift;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 9/26/14.
 */
public class GiftDb extends MongoModelController<Gift> {
    private Logger logger;
    private EventBus eventBus;

    public GiftDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
        this.eventBus = vertx.eventBus();
        this.logger = logger;
    }

    @Override
    public String getCollectionName(Gift model) {
        return "gift";
    }

    @Override
    public Gift newModelInstance() {
        return new Gift();
    }

    public void lock(String owner, String giftId, final Handler<Boolean> callback) {
        changeLock(owner, giftId, false, true, callback);
    }

    public void unlock(String owner, String giftId, final Handler<Boolean> callback) {
        changeLock(owner, giftId, true, false, callback);
    }

    public void getStableGift(String owner, final Handler<List<Gift>> callback) {
        JsonObject matcher = new JsonObject();
        matcher.putString("owner", owner);

//        JsonArray or = new JsonArray();
//        or.add(new JsonObject().putNumber("status", Gift.STATUS_NEW));
//        or.add(new JsonObject().putNumber("status", Gift.STATUS_VIEWED));
//
//        matcher.putArray("$or", or);
        matcher.putObject("status", new JsonObject().putNumber("$ne", Gift.STATUS_TIMED));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putNumber(MongoKeyWords.LIMIT, 200);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        if (matcher != null)
            query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<Gift>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int num;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            Gift m = newModelInstance();

                            //linh moi them
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

    public void changeLock(String owner, String giftId, boolean currentValue, boolean newValue, final Handler<Boolean> callback) {
        JsonObject updateValues = new JsonObject().putBoolean("lock", newValue);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        JsonObject criteria = new JsonObject()
                .putString("_id", giftId)
                .putString("owner", owner)
                .putBoolean("lock", currentValue);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, updateValues);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject reply = message.body();
                if (callback != null) {
                    boolean result = false;
                    if (reply.getInteger("number", 0) > 0 && "ok".equals(reply.getString("status", "no")) && reply.getBoolean("isUpdated", false)) {
                        result = true;
                    }
                    callback.handle(result);
                }
            }
        });
    }

    public void find(long endDate, String notOwner, final Handler<List<Gift>> callback) {
        JsonObject matcher = new JsonObject();

        JsonObject lte = new JsonObject()
                .putNumber("$lte", endDate);
        matcher.putObject("endDate", lte);
        matcher.putObject("owner", new JsonObject().putString("$ne", notOwner));

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            Gift m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

    public void findForExpired(long expiredTime, final Handler<List<Gift>> callback) {
        JsonObject matcher = new JsonObject();

        JsonObject lte = new JsonObject()
                .putNumber("$lte", expiredTime);
        matcher.putObject("endDate", lte);

        JsonArray array = new JsonArray();
        array.add(Gift.STATUS_COMPLETE); // da su dung thanh cong
        array.add(Gift.STATUS_EXPIRED); // da het han

        JsonObject notIn = new JsonObject();
        notIn.putArray(MongoKeyWords.NOT_IN, array);
        matcher.putObject("status", notIn);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putNumber(MongoKeyWords.LIMIT, 5000);
        query.putNumber(MongoKeyWords.BATCH_SIZE,100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);

                            String owner = record.getString("owner", "");
                            if (DataUtil.strToInt(owner) > 0) {
                                Gift m = newModelInstance();
                                m.setModelId(record.getString("_id"));
                                m.setValues(record);
                                mList.add(m);
                            }
                        }
                    }
                } else {
                    logger.info(message.body().toString() + "  " + "status " + message.body().getString(MongoKeyWords.STATUS));
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

    public void findForExpiredNumber(long expiredTime, int number, final Handler<List<Gift>> callback) {
        JsonObject matcher = new JsonObject();

        JsonObject lte = new JsonObject()
                .putNumber("$lte", expiredTime);
        matcher.putObject("endDate", lte);

        JsonArray array = new JsonArray();
        array.add(Gift.STATUS_COMPLETE); // da su dung thanh cong
        array.add(Gift.STATUS_EXPIRED); // da het han

        JsonObject notIn = new JsonObject();
        notIn.putArray(MongoKeyWords.NOT_IN, array);
        matcher.putObject("status", notIn);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, matcher);
        if (number > 0) {
            query.putNumber(MongoKeyWords.LIMIT, number);
        }
        //sort
        JsonObject fieldSort = new JsonObject();
        fieldSort.putNumber(StringConstUtil.GIFT_ENDDATE, -1);
        query.putObject(MongoKeyWords.SORT, fieldSort);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);

                            String owner = record.getString("owner", "");
                            if (DataUtil.strToInt(owner) > 0) {
                                Gift m = newModelInstance();
                                m.setModelId(record.getString("_id"));
                                m.setValues(record);
                                mList.add(m);
                            }
                        }
                    }
                } else {
                    logger.info(message.body().toString() + "  " + "status " + message.body().getString(MongoKeyWords.STATUS));
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }


    public void getErrorGiftIronMan(final Handler<JsonArray> callback){

        JsonObject sumNumber = new JsonObject()
                .putNumber("$sum", 1);

        JsonObject grouper = new JsonObject()
                .putString("_id", "$owner")
                .putObject("count", sumNumber);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putString("action", "aggregate");

        query.putObject(MongoKeyWords.GROUPER, grouper);
        query.putObject(MongoKeyWords.MATCHER, new JsonObject().putString("giftInfoDetail", "iron_promo_1"));
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if(event.body() != null)
                {
                    JsonArray result = event.body().getArray("result");
                    callback.handle(result);
                }
                else {
                    callback.handle(new JsonArray());
                }
            }
        });
    }

    // update field amount trong banh gift
    public void updateOneAmount(String giftId, JsonObject jsonObject, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        JsonObject criteria = new JsonObject();
        JsonObject update = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        criteria.putString("_id", giftId);
        query.putObject(MongoKeyWords.CRITERIA, criteria);
        update.putObject(MongoKeyWords.SET_$, jsonObject);
        query.putObject(MongoKeyWords.OBJ_NEW, update);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                //
                if (json != null && json.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void getGiftWithFilter(JsonObject filter, final Handler<ArrayList<Gift>> callback) {
        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        query.putObject(MongoKeyWords.MATCHER, filter);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            Gift m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });

    }


    public void findBy(String owner, final Handler<List<Gift>> callback) {
        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));

        if (!"".equalsIgnoreCase(owner)) {
            matcher.putString("owner", owner);
            query.putObject(MongoKeyWords.MATCHER, matcher);
        }

        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 1); // khong lay Id ve
        //lay danh sach cac field trong bang Gift
        fields.putNumber("typeId", 1);
        fields.putNumber("amount", 1);
        fields.putNumber("startDate", 1);
        fields.putNumber("endDate", 1);
        fields.putNumber("modifyDate", 1);
        fields.putNumber("owner", 1);
        fields.putNumber("status", 1);
        fields.putNumber("lock", 1);
        fields.putNumber("_id", 1);

        query.putObject(MongoKeyWords.KEYS, fields);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 1000000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            Gift m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

    public void findOneIronManErrorGift(String owner, final Handler<List<Gift>> callback) {
        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));

        if (!"".equalsIgnoreCase(owner)) {
            matcher.putString("owner", owner);
            matcher.putString("giftInfoDetail", "iron_promo_1");
            query.putObject(MongoKeyWords.MATCHER, matcher);
        }

        query.putNumber(MongoKeyWords.LIMIT, 1);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 1000000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;

                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);
                            Gift m = newModelInstance();
                            m.setModelId(record.getString("_id"));
                            m.setValues(record);
                            mList.add(m);
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }


    public void update(String giftId, JsonObject updateValues, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));
        JsonObject criteria = new JsonObject()
                .putString("_id", giftId);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, updateValues);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject reply = message.body();
                if (callback != null) {
                    boolean result = false;
                    if (reply.getInteger("number", 0) > 0 && "ok".equals(reply.getString("status", "no")) && reply.getBoolean("isUpdated", false)) {
                        result = true;
                    }
                    callback.handle(result);
                }
            }
        });
    }

    public void findForRemind(final long left7day
            , final long left3day
            , final long left1day
            , final Handler<List<Gift>> callback) {
        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, getCollectionName(null));

        //or condition
        JsonArray array = new JsonArray();
//        JsonObject lte7 = new JsonObject();
//        lte7.putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() + left7day * 24 * 60 * 60 * 1000L);
//        JsonObject endDate7 = new JsonObject();
//        endDate7.putObject("endDate", lte7);

//        JsonObject lte3 = new JsonObject();
//        lte3.putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() + left3day * 24 * 60 * 60 * 1000L);
//        JsonObject endDate3 = new JsonObject();
//        endDate3.putObject("endDate", lte3);

        JsonObject lte1 = new JsonObject();
        lte1.putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() + left1day * 24 * 60 * 60 * 1000L);
        JsonObject endDate1 = new JsonObject();
        endDate1.putObject("endDate", lte1);

        //array.add(endDate7);
        //array.add(endDate3);
        array.add(endDate1);

        matcher.putArray(MongoKeyWords.OR, array);

        //with status : new,viewed,activated
        JsonArray arrayStatus = new JsonArray();
        arrayStatus.add(Gift.STATUS_NEW);
        arrayStatus.add(Gift.STATUS_VIEWED);
        arrayStatus.add(Gift.STATUS_USED);

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, arrayStatus);

        matcher.putObject("status", in);

        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putNumber(MongoKeyWords.BATCH_SIZE,100000);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Gift> mList = new ArrayList<>();
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        JsonObject record;
                        int i;
                        for (i = 0; i < results.size(); i++) {
                            record = results.get(i);

                            //only get end-user's gifts
                            String owner = record.getString("owner");
                            if (DataUtil.strToInt(owner) > 0) {
                                Gift m = newModelInstance();
                                m.setModelId(record.getString("_id"));
                                m.setValues(record);
                                mList.add(m);
                            }
                        }
                    }
                } else {
                    logger.error("Can't find objects from " + getCollectionName(null) + " collection.");
                }
                callback.handle(mList);
            }
        });
    }

    public void findOne(final String giftId, final Handler<Gift> callback) {

        JsonObject matcher = new JsonObject();
        matcher.putString("_id", giftId);

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

                if (callback != null) {
                    Gift gift = new Gift();
                    gift.setModelId(giftId);
                    gift.setValues(result);
                    callback.handle(gift);
                }
            }
        });
    }


}
