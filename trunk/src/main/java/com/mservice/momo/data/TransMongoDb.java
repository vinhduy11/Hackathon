package com.mservice.momo.data;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
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
 * Created by Administrator on 3/6/14.
 */
public class TransMongoDb implements TransDb {

    private EventBus eventBus;
    private Vertx vertx;
    private Logger logger;
    private TransMySQLDb mySQLDb;
    private boolean isRepli = false;

    public TransMongoDb(Vertx vertx, EventBus eb, Logger log, JsonObject glbConfig, boolean isMySQLReplication) {
        this.vertx = vertx;
        this.eventBus = eb;
        this.logger = log;
        isRepli = isMySQLReplication;
        mySQLDb = new TransMySQLDb(vertx, eb, log, glbConfig);
    }

    public void delRows(final int number, final ArrayList<Long> tranIds, final Handler<Boolean> callback) {

        //query
        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //matcher
        JsonArray array = new JsonArray();
        for (long item : tranIds) {
            array.addNumber(item);
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, array);

        matcher.putObject(colName.TranDBCols.TRAN_ID, in);
        query.putObject(MongoKeyWords.CRITERIA, matcher);

        //keys
        JsonObject keys = new JsonObject();
        keys.putNumber(colName.TranDBCols.TRAN_ID, 1);
        query.putObject(MongoKeyWords.KEYS, keys);

        //field del
        JsonObject delField = new JsonObject();
        delField.putBoolean(colName.TranDBCols.DELETED, true);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, delField);

        query.putObject(MongoKeyWords.OBJ_NEW, set);

        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.MULTI, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                if (isRepli) {
                    mySQLDb.delRows(number, tranIds, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                        }
                    });
                }
                JsonObject j = jsonObjectMessage.body();
                boolean result = (j.getString(MongoKeyWords.STATUS, "") == "ok" && j.getInteger(MongoKeyWords.NUMBER, 0) == tranIds.size());
                callback.handle(result);
            }
        });
    }

    public void getStatisticTranPerDay(final int number, final Handler<JsonObject> callback) {

        JsonObject jo = Misc.getStartAndEndCurrentDateInMilliseconds();
        long beginDate = jo.getLong(Misc.BeginDateInMilliSec, 0);
        long endDate = jo.getLong(Misc.EndDateInMilliSec, 0);

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        //matcher
        JsonObject inDay = new JsonObject();
        inDay.putNumber(MongoKeyWords.GREATER_OR_EQUAL, beginDate)
                .putNumber(MongoKeyWords.LESS_OR_EQUAL, endDate);

        match.putObject(colName.TranDBCols.FINISH_TIME, inDay);
        match.putNumber(colName.TranDBCols.ERROR, 0);

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        query.putObject(MongoKeyWords.MATCHER, match);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                int tranCount = 0;
                long totalIn = 0;
                long totalOut = 0;
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {

                        tranCount = results.size();
                        for (int i = 0; i < results.size(); i++) {

                            JsonObject jo = results.get(i);

                            int io = jo.getInteger(colName.TranDBCols.IO, 0);
                            if (io == 1) {
                                totalIn += jo.getLong(colName.TranDBCols.AMOUNT, 0);
                            } else {
                                totalOut += jo.getLong(colName.TranDBCols.AMOUNT, 0);
                            }
                        }
                    }
                }
                JsonObject j = new JsonObject();
                j.putNumber("value", Math.max(totalIn, totalOut));
                j.putNumber("count", tranCount);
                callback.handle(j);
            }
        });
    }

    public void upsertTranOutSideNew(final int number, final JsonObject obj, final Handler<Boolean> callback) {
        final JsonObject tranObjJson = obj.copy();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("class ------>", "upsertTranOutSideNew");
        tranObjJson.removeField(colName.TranDBCols.OWNER_NUMBER);
        tranObjJson.removeField(colName.TranDBCols.OWNER_NAME);
        tranObjJson.removeField(colName.TranDBCols.PARRENT_TRAN_TYPE);
        tranObjJson.removeField(Const.AppClient.Html);
        tranObjJson.removeField(Const.AppClient.Qrcode);

        //neu co comment thi lay, khong thi remove luon
        if ("".equalsIgnoreCase(tranObjJson.getString(colName.TranDBCols.COMMENT, ""))) {
            tranObjJson.removeField(colName.TranDBCols.COMMENT);
        }

        tranObjJson.putBoolean(colName.TranDBCols.DELETED, false);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        JsonObject criteria = new JsonObject();
        criteria.putNumber(colName.TranDBCols.TRAN_ID
                , tranObjJson.getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis()));

        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, tranObjJson);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);

        query.putBoolean(MongoKeyWords.UPSERT, true);
        log.add("method ------>", "InsertDB");
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                if (isRepli) {
                    mySQLDb.upsertTranOutSideNew(number, obj, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                        }
                    });
                }
                JsonObject obj = jsonObjectMessage.body();

                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                if (result) {
                    log.add("result ------>", result);
                }
                callback.handle(result);
            }
        });
    }


    public void upsertTran(final int number, final JsonObject tranObjJson, final Handler<TranObj> callback) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("class ------>", "upsertTran");
        //remove some fields no need to store in DB
        tranObjJson.removeField(colName.TranDBCols.PARRENT_TRAN_TYPE);
        tranObjJson.removeField(colName.TranDBCols.OWNER_NUMBER);
        tranObjJson.removeField(colName.TranDBCols.OWNER_NAME);
        tranObjJson.removeField(colName.TranDBCols.IS_M2NUMBER);
        tranObjJson.removeField(colName.TranDBCols.FORCE_COMMENT);
        tranObjJson.removeField(colName.TranDBCols.DESCRIPTION);

        tranObjJson.putBoolean(colName.TranDBCols.DELETED, false);

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number)
                .putObject(MongoKeyWords.DOCUMENT, tranObjJson);
        log.add("method ------>", "InsertDB");
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (isRepli) {
                    mySQLDb.upsertTran(number, tranObjJson, new Handler<TranObj>() {
                        @Override
                        public void handle(TranObj tranObj) {

                        }
                    });
                }
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if (createdId != null) {
                        TranObj tran = new TranObj(tranObjJson);
                        callback.handle(tran);
                        return;
                    }
                }
                callback.handle(null);
            }
        });
    }

    public void find(int number, final long fromFinishTime, boolean isStore, final Handler<ArrayList<TranObj>> callback) {

        //du lieu khong phai tu app xuong roi, chac bi tan cong ha ?????
        if (fromFinishTime < 0) {
            logger.info("Sao lai co du lieu nay, fromFinishTime: " + fromFinishTime);
            callback.handle(null);
            return;
        }

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //quet them nhung giao dich ngoai backend
        long finishTime = fromFinishTime - TranObj.TRAN_SYNC_PREV_MINUTES * 60 * 1000;

        //field greater
        JsonObject greater = new JsonObject();
        greater.putNumber(MongoKeyWords.GREATER, finishTime);

        JsonObject matcher = new JsonObject();
        matcher.putObject(colName.TranDBCols.FINISH_TIME, greater);

        matcher.putBoolean(colName.TranDBCols.DELETED, false);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        //sort field
        JsonObject fieldSort = new JsonObject();
        if (isStore) {
            fieldSort.putNumber(colName.TranDBCols.FINISH_TIME, 1);
        } else {
            fieldSort.putNumber(colName.TranDBCols.FINISH_TIME, -1);
        }

        query.putObject(MongoKeyWords.SORT, fieldSort);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 1000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                //
                if (json != null && json.getArray(MongoKeyWords.RESULT_ARRAY, null) != null) {
                    JsonArray result = json.getArray(MongoKeyWords.RESULT_ARRAY);
                    ArrayList list = new ArrayList();

                    //equal to client requested item
                    if (list.size() == 1) {
                        callback.handle(null);
                        return;
                    }

                    for (int i = 0; i < result.size(); i++) {
                        TranObj obj = new TranObj((JsonObject) result.get(i));
                        if (obj.finishTime != fromFinishTime) {
                            list.add(obj);
                        }
                    }
                    callback.handle(list);
                } else {
                    callback.handle(null);
                }
            }
        });

    }

    public void countWithFilter(int phoneNumber, Long startTime,
                                Long endTime, Integer status, List<Integer> types, final Handler<Long> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "findWithFilter");
        query.putString("collection", colName.TranDBCols.TABLE_PREFIX + phoneNumber);

        JsonObject timeFilter = null;
        if (startTime != null) {
            timeFilter = new JsonObject();
            timeFilter.putNumber("$gte", startTime);
        }
        if (endTime != null) {
            if (timeFilter == null) timeFilter = new JsonObject();
            timeFilter.putNumber("$lte", endTime);
        }

        JsonObject matcher = new JsonObject();
        if (status != null)
            matcher.putNumber("status", status);
        if (types != null) {
            JsonArray array = new JsonArray();
            JsonObject in = new JsonObject();
            for (Integer type : types) {
                if (type != null)
                    array.add(type);
            }
            if (array.size() > 0) {
                in.putArray(MongoKeyWords.IN_$, array);
                matcher.putObject("tranType", in);
            }
        }
        if (timeFilter != null) {
            matcher.putObject("ftime", timeFilter);
        }
        query.putObject("matcher", matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Long count = event.body().getLong("count");
                callback.handle(count);
            }
        });
    }

    public void getTransaction(int phoneNumber, int pageSize, int pageNumber, Long startTime,
                               Long endTime, Integer status, List<Integer> types, final Handler<ArrayList<TranObj>> callback) {

        JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", "tran_" + phoneNumber);

        int skip = (pageNumber - 1) * pageSize;
//        int records = pageNumber * pageSize;
        query.putNumber("skip", skip);
        query.putNumber("limit", pageSize);

        JsonObject timeFilter = null;
        if (startTime != null) {
            timeFilter = new JsonObject();
            timeFilter.putNumber("$gte", startTime);
        }
        if (endTime != null) {
            if (timeFilter == null) timeFilter = new JsonObject();
            timeFilter.putNumber("$lte", endTime);
        }

        JsonObject matcher = new JsonObject();
        if (status != null)
            matcher.putNumber("status", status);
        if (types != null) {
            JsonArray array = new JsonArray();
            JsonObject in = new JsonObject();
            for (Integer type : types) {
                array.add(type);
            }
            if (array.size() > 0) {
                in.putArray(MongoKeyWords.IN_$, array);
                matcher.putObject("tranType", in);
            }
        }
        if (timeFilter != null) {
            matcher.putObject("ftime", timeFilter);
        }


        JsonObject sort = new JsonObject();
        sort.putNumber("ftime", -1);
        query.putObject("sort", sort);

        query.putObject("matcher", matcher);


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject obj = event.body();

                ArrayList<TranObj> finalResult = new ArrayList<TranObj>();

                if (obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    if (results != null) {

                        for (int i = 0; i < results.size(); i++) {
                            JsonObject model = (JsonObject) results.get(i);
                            TranObj tranObj = new TranObj(model);
                            finalResult.add(tranObj);
                        }
                    }

                    callback.handle(finalResult);
                } else {
                    callback.handle(null);
                }
            }
        });
    }

    public void getTransactionDetail(int phoneNumber, long transactionId, final Handler<TranObj> callback) {
        JsonObject matcher = new JsonObject()
                .putNumber("tranId", transactionId);

        JsonObject query = new JsonObject()
                .putString("action", "findone")
                .putString("collection", "tran_" + phoneNumber)
                .putObject("matcher", matcher);


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject obj = event.body();

                if (obj.getString("status", "ko").equalsIgnoreCase("ok")
                        && obj.containsField("result")) {
                    JsonObject model = (JsonObject) obj.getObject("result");
                    TranObj tranObj = new TranObj(model);
                    callback.handle(tranObj);
                } else {
                    callback.handle(null);
                }
            }
        });
    }

    public void updateTranStatusNew(final int number
            , final long tranId
            , final int status
            , final Handler<Boolean> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject criteria = new JsonObject();
        criteria.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.TranDBCols.STATUS, status);

        //cap nhat lai ngay ket thuc giao dich
        objNew.putNumber(colName.TranDBCols.FINISH_TIME, System.currentTimeMillis());

        //cap nhat lai trang thai giao dich
        if (status != TranObj.STATUS_OK) {
            objNew.putNumber(colName.TranDBCols.ERROR, TranObj.STATUS_FAIL);
        } else {
            objNew.putNumber(colName.TranDBCols.ERROR, 0);
        }

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (isRepli) {
                    mySQLDb.updateTranStatusNew(number, tranId, status, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                        }
                    });
                }
                JsonObject json = event.body();
                boolean result = json.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });

    }

    public void updateTranStatus(final int number
            , final long tranId
            , final int status
            , final Handler<TranObj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        query.putObject(MongoKeyWords.MATCHER, match);

        //sort
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject objNew = new JsonObject();
        objNew.putNumber(colName.TranDBCols.STATUS, status);

        //cap nhat lai ngay ket thuc giao dich
        objNew.putNumber(colName.TranDBCols.FINISH_TIME, System.currentTimeMillis());

        //cap nhat lai trang thai giao dich
        if (status != TranObj.STATUS_OK) {
            objNew.putNumber(colName.TranDBCols.ERROR, TranObj.STATUS_FAIL);
        } else {
            objNew.putNumber(colName.TranDBCols.ERROR, 0);
        }

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (isRepli) {
                    mySQLDb.updateTranStatus(number, tranId, status, new Handler<TranObj>() {
                        @Override
                        public void handle(TranObj tranObj) {
                        }
                    });
                }
                JsonObject json = event.body();

                TranObj tranObj = null;

                if (json != null && json.getString(MongoKeyWords.STATUS).equalsIgnoreCase("ok")) {
                    JsonObject jR = json.getObject(MongoKeyWords.RESULT);
                    tranObj = new TranObj(jR);
                }

                callback.handle(tranObj);
            }
        });

    }

    public void getTranExistOnServer(final int number, final ArrayList<Long> cIds, final Handler<ArrayList<Long>> callback) {
        JsonArray jNumArr = new JsonArray();

        for (long item : cIds) {
            jNumArr.addNumber(item);
        }

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, jNumArr);

        match.putObject(colName.TranDBCols.COMMAND_INDEX, in);
        query.putObject(MongoKeyWords.MATCHER, match);

        JsonObject keys = new JsonObject();
        keys.putNumber(colName.TranDBCols.COMMAND_INDEX, 1);
        query.putObject(MongoKeyWords.KEYS, keys);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Long> list = null;
                //To change body of implemented methods use File | Settings | File Templates.
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    if (results != null && results.size() > 0) {
                        list = new ArrayList<>();
                        JsonObject item;
                        for (int i = 0; i < results.size(); i++) {
                            item = results.get(i);
                            list.add(item.getLong(colName.TranDBCols.COMMAND_INDEX));
                        }
                    }
                }
                callback.handle(list);
            }
        });

    }

    public void getTranById(final int phone, final long tranId, final Handler<TranObj> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + phone);

        match.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }
                if (callback != null) {
                    TranObj obj = new TranObj(result);
                    callback.handle(obj);
                }
            }
        });
    }

    public void countAgentTran(final int phoneNumber, final Handler<Long> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "count");
        query.putString("collection", colName.TranDBCols.TABLE_PREFIX + phoneNumber);

        JsonObject matcher = new JsonObject();
        matcher.putNumber(colName.TranDBCols.IO, -1);
        matcher.putNumber(colName.TranDBCols.ERROR, 0);
        query.putObject("matcher", matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Long count = event.body().getLong("count");
                callback.handle(count);
            }
        });
    }

    public void getTransInList(final int number, ArrayList<Long> cIds, final Handler<ArrayList<TranObj>> callback) {
        JsonArray jNumArr = new JsonArray();

        for (long item : cIds) {
            jNumArr.addNumber(item);
        }

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, jNumArr);

        match.putObject(colName.TranDBCols.COMMAND_INDEX, in);
        query.putObject(MongoKeyWords.MATCHER, match);

        //sort field
        JsonObject fieldSort = new JsonObject();
        fieldSort.putNumber(colName.TranDBCols.COMMAND_INDEX, -1);

        query.putObject(MongoKeyWords.SORT, fieldSort);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 1000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                ArrayList<TranObj> list = null;
                //
                if (json != null && json.getArray(MongoKeyWords.RESULT_ARRAY, null) != null) {
                    list = new ArrayList();
                    JsonArray result = json.getArray(MongoKeyWords.RESULT_ARRAY);

                    for (int i = 0; i < result.size(); i++) {
                        TranObj obj = new TranObj((JsonObject) result.get(i));
                        list.add(obj);
                    }
                }
                callback.handle(list);
            }
        });
    }

    public void sumTranInCurrentMonth(final int number
            , ArrayList<Integer> listTranType
            , final Handler<Long> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //Misc.TimeOfMonth timeOfMonth = Misc.getBeginEndTimeOfMonth(System.currentTimeMillis());
        Misc.TimeOfMonth timeOfMonth = Misc.getBeginEndTimeOfMonth(System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L);

        JsonObject time = new JsonObject();
        time.putNumber(MongoKeyWords.GREATER_OR_EQUAL, timeOfMonth.BeginTime)
                .putNumber(MongoKeyWords.LESS_OR_EQUAL, timeOfMonth.EndTime);

        JsonArray array = new JsonArray();
        for (Integer i : listTranType) {
            array.add(i);
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, array);
        match.putObject(colName.TranDBCols.TRAN_TYPE, in);

        match.putObject(colName.TranDBCols.FINISH_TIME, time);
        match.putNumber(colName.TranDBCols.ERROR, 0);

        //sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.TranDBCols.FINISH_TIME, -1);

        query.putObject(MongoKeyWords.SORT, sort);

        query.putObject(MongoKeyWords.MATCHER, match);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000);

        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 0);
        fields.putNumber(colName.TranDBCols.AMOUNT, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                long totalValue = 0;

                if (json != null && json.getArray(MongoKeyWords.RESULT_ARRAY, null) != null) {

                    JsonArray result = json.getArray(MongoKeyWords.RESULT_ARRAY);

                    for (int i = 0; i < result.size(); i++) {
                        totalValue += ((JsonObject) result.get(i)).getLong(colName.TranDBCols.AMOUNT, 0);
                    }
                }
                callback.handle(totalValue);
            }
        });
    }

    //vcb.start
    public void findOneVcbTran(int number, String bankCode, long promoFromDate, long promoToDate, final Handler<TranObj> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //nam trong khoang
        JsonObject between = new JsonObject();
        between.putNumber(MongoKeyWords.GREATER_OR_EQUAL, promoFromDate)
                .putNumber(MongoKeyWords.LESS_OR_EQUAL, promoToDate);

        match.putObject(colName.TranDBCols.FINISH_TIME, between);
        match.putNumber(colName.TranDBCols.ERROR, 0);
        match.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
        match.putString(colName.TranDBCols.PARTNER_CODE, bankCode);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();

                TranObj obj = null;

                if (json != null && json.getObject(MongoKeyWords.RESULT, null) != null) {
                    obj = new TranObj(json.getObject(MongoKeyWords.RESULT));
                }
                callback.handle(obj);
            }
        });
    }

    public void findOneByTranType(int number, int tranType, long upperTime, final Handler<Integer> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        JsonObject lessOrEqual = new JsonObject();
        lessOrEqual.putNumber(MongoKeyWords.LESS_OR_EQUAL, upperTime);

        match.putNumber(colName.TranDBCols.ERROR, 0);
        match.putNumber(colName.TranDBCols.TRAN_TYPE, tranType);
        match.putObject(colName.TranDBCols.FINISH_TIME, lessOrEqual);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();

                int count = 0;

                if (json != null && json.getObject(MongoKeyWords.RESULT, null) != null) {
                    count = 1;
                }
                callback.handle(count);
            }
        });
    }

    public void findOneInBillPayAndPhim(int number, long promoFromDate, long tranFinishTime, final Handler<TranObj> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //nam trong khoang
        JsonObject between = new JsonObject();
        between.putNumber(MongoKeyWords.GREATER_OR_EQUAL, promoFromDate)
                .putNumber(MongoKeyWords.LESS_OR_EQUAL, tranFinishTime);

        match.putObject(colName.TranDBCols.FINISH_TIME, between);
        match.putNumber(colName.TranDBCols.ERROR, 0);

        JsonArray inArr = new JsonArray();
        inArr.add(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
        inArr.add(MomoProto.TranHisV1.TranType.PHIM123_VALUE);

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$, inArr);
        match.putObject(colName.TranDBCols.TRAN_TYPE, in);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();

                TranObj obj = null;

                if (json != null && json.getObject(MongoKeyWords.RESULT, null) != null) {
                    obj = new TranObj(json.getObject(MongoKeyWords.RESULT));
                }
                callback.handle(obj);
            }
        });
    }

    public void getTheTwoLastTranOfNumber(int number, final Handler<JsonArray> callback) {
        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //sort field
        JsonObject fieldSort = new JsonObject();
        fieldSort.putNumber(colName.TranDBCols.CLIENT_TIME, -1);

        query.putObject(MongoKeyWords.SORT, fieldSort);
        query.putNumber(MongoKeyWords.LIMIT, 2);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                callback.handle(array);
                //

            }
        });
    }

    public void getTheThreeLastTranOfNumber(int number, final Handler<JsonArray> callback) {
        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + number);

        //sort field
        JsonObject fieldSort = new JsonObject();
        fieldSort.putNumber(colName.TranDBCols.CLIENT_TIME, -1);

        query.putObject(MongoKeyWords.SORT, fieldSort);
        query.putNumber(MongoKeyWords.LIMIT, 3);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                callback.handle(array);
                //

            }
        });
    }

    public void countTop2WithFilter(int phoneNumber, Long startTime,
                                    final Long endTime, Integer error, ArrayList<Integer> trantypes, int limit, final Handler<Long> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString("collection", colName.TranDBCols.TABLE_PREFIX + phoneNumber);

        JsonObject timeFilter = null;
        if (startTime != null) {
            timeFilter = new JsonObject();
            timeFilter.putNumber("$gte", startTime);
        }
        if (endTime != null) {
            if (timeFilter == null) timeFilter = new JsonObject();
            timeFilter.putNumber("$lte", endTime);
        }

        JsonObject matcher = new JsonObject();
        if (error != null)
            matcher.putNumber("error", error);

        matcher.putBoolean("del", false);

        if (trantypes != null) {
            JsonArray array = new JsonArray();
            JsonObject notIn = new JsonObject();
            for (Integer type : trantypes) {
                if (type != null)
                    array.add(type);
            }
            if (array.size() > 0) {
                notIn.putArray(MongoKeyWords.NOT_IN, array);
                matcher.putObject("tranType", notIn);
            }
        }
        if (timeFilter != null) {
            matcher.putObject("ftime", timeFilter);
        }
        query.putObject("matcher", matcher);
        query.putNumber(MongoKeyWords.LIMIT, limit);
        JsonObject jsonKey = new JsonObject();
        jsonKey.putNumber("_id", 1);

        query.putObject(MongoKeyWords.KEYS, jsonKey);


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);

                long count = array == null ? 0 : array.size();

                callback.handle(count);
            }
        });
    }

    public void searchWithFilter(int phoneNumber, JsonObject filter, final Handler<ArrayList<TranObj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString("collection", colName.TranDBCols.TABLE_PREFIX + phoneNumber);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<TranObj> arrayList = new ArrayList<TranObj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        TranObj obj = new TranObj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void getTranByBillId(final int phone, final String billId, final Handler<TranObj> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + phone);

        match.putString(colName.TranDBCols.BILL_ID, billId);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }
                if (callback != null) {
                    TranObj obj = new TranObj(result);
                    callback.handle(obj);
                }
            }
        });
    }

    public void getTranByPartnerInvNo(final int phone, final String partnerInvNo, final Handler<TranObj> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.TranDBCols.TABLE_PREFIX + phone);

        match.putString(colName.TranDBCols.DESCRIPTION, partnerInvNo);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body().getObject("result");
                if (result == null) {
                    if (callback != null)
                        callback.handle(null);
                    return;
                }
                if (callback != null) {
                    TranObj obj = new TranObj(result);
                    callback.handle(obj);
                }
            }
        });
    }
}
