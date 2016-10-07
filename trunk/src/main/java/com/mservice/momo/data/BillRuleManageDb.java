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
 * Created by concu on 7/3/15.
 */
public class BillRuleManageDb {

    private Vertx vertx;
    private Logger logger;

    public BillRuleManageDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.BillRuleManage.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


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

    public void updatePartial(String billId
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillRuleManage.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.BillRuleManage.BILL_ID, billId);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString(MongoKeyWords.STATUS, "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BillRuleManage.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        Obj obj = new Obj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void findOne(String billId, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillRuleManage.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.BillRuleManage.BILL_ID, billId);
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

    public void findAndModifyBillId(String billId, final Handler<Integer> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.BillRuleManage.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.BillRuleManage.BILL_ID, billId);
        query.putObject(MongoKeyWords.MATCHER, match);

        //field inc
        JsonObject incField = new JsonObject();
        incField.putNumber(colName.BillRuleManage.COUNT, 1);

        //obj inc
        JsonObject udpObj = new JsonObject();
        udpObj.putObject(MongoKeyWords.INCREMENT,incField);

        //update
        query.putObject(MongoKeyWords.UPDATE, udpObj);

        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);
//
//        JsonObject fields = new JsonObject();
//        fields.putNumber(colName.BillRuleManage.COUNT, 1);
//        query.putObject("fields", fields);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                if(json!= null){
                    int remain_count = json.getObject(MongoKeyWords.RESULT).getInteger(colName.BillRuleManage.COUNT);
                    remain_count = remain_count > 0 ? remain_count :0;
                    callback.handle(remain_count);
                }
                else {
                    callback.handle(0);
                }
            }
        });
    }

    public static class Obj {

        public String billId = "";
        public long startTime = 0;
        public long endTime = 0;
        public String serviceId = "";
        public long amount = 0;
        public String phoneNumber = "";
        public int tranType = 0;
        public long tranId = 0;
        public int count = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {

            billId = jo.getString(colName.BillRuleManage.BILL_ID, "");
            startTime = jo.getLong(colName.BillRuleManage.START_TIME, 0);
            endTime = jo.getLong(colName.BillRuleManage.END_TIME, 0);
            serviceId = jo.getString(colName.BillRuleManage.SERVICE_ID, "");
            amount = jo.getLong(colName.BillRuleManage.AMOUNT, 0);
            phoneNumber = jo.getString(colName.BillRuleManage.PHONE_NUMBER, "");
            tranId = jo.getLong(colName.BillRuleManage.TRAN_ID, 0);
            tranType = jo.getInteger(colName.BillRuleManage.TRAN_TYPE, 0);
            count = jo.getInteger(colName.BillRuleManage.COUNT, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.BillRuleManage.BILL_ID, billId);
            jo.putNumber(colName.BillRuleManage.START_TIME, startTime);
            jo.putNumber(colName.BillRuleManage.END_TIME, endTime);
            jo.putString(colName.BillRuleManage.SERVICE_ID, serviceId);
            jo.putNumber(colName.BillRuleManage.AMOUNT, amount);
            jo.putString(colName.BillRuleManage.PHONE_NUMBER, phoneNumber);
            jo.putNumber(colName.BillRuleManage.TRAN_ID, tranId);
            jo.putNumber(colName.BillRuleManage.TRAN_TYPE, tranType);
            jo.putNumber(colName.BillRuleManage.COUNT, count);
            return jo;
        }
    }
}
