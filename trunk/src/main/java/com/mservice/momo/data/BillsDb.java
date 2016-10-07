package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created by Administrator on 2/25/14.
 */
public class BillsDb {

    public static class Obj{

        //shared
        public String billId;
        public String providerId;
        public String billDetail; // billID + BELL + amount + BELL + fromDate + BELL + toDate + LF
        public int status;
        public String ownerName;
        public String ownerAddress;
        public String ownerPhone;
        public long totalAmount;

        public long startDate;
        public long endDate;

        //other
        public int payType; // loai dich vu can thanh toan.
        public int tranferType;
        public long dueDate;
        public String payScheduler; // thanh toan truoc 7,5, 1 ngay
        public long payDate = 0; //ngay thanh toan

        //
        public int payChanel; // 0  thanh toan cho bill thuong, 1 bill khac
        public Obj(){}

        public Obj(JsonObject jsonObject){
            billId  = jsonObject.getString(colName.BillDBCols.BILL_ID,"");
            providerId = jsonObject.getString(colName.BillDBCols.PROVIDER_ID,"");
            billDetail = jsonObject.getString(colName.BillDBCols.BILL_DETAIL,"");
            status = jsonObject.getInteger(colName.BillDBCols.STATUS, 0);
            ownerName =jsonObject.getString(colName.BillDBCols.OWNER_NAME,"");
            ownerAddress = jsonObject.getString(colName.BillDBCols.OWNER_ADDRESS,"");
            ownerPhone = jsonObject.getString(colName.BillDBCols.OWNER_PHONE,"");
            totalAmount =jsonObject.getLong(colName.BillDBCols.TOTAL_AMOUNT,0);

            //other
            payType =jsonObject.getInteger(colName.BillDBCols.PAY_TYPE,0);
            tranferType =jsonObject.getInteger(colName.BillDBCols.TRANSFER_TYPE,0);
            payScheduler =jsonObject.getString(colName.BillDBCols.PAY_SCHEDULER,"");
            dueDate =jsonObject.getLong(colName.BillDBCols.DUE_DATE,0);

            startDate = jsonObject.getLong(colName.BillDBCols.START_DATE, 0);
            endDate = jsonObject.getLong(colName.BillDBCols.END_DATE, 0);

            //
            payChanel = jsonObject.getInteger(colName.BillDBCols.PAY_CHANEL, 0);
            payDate = jsonObject.getLong(colName.BillDBCols.PAY_DATE,0);
        }
    }

    EventBus eventBus;
    public BillsDb(EventBus eb){
        eventBus = eb;
    }

    public void getAllBills(int number, Obj obj ,final Handler<ArrayList<BillsDb.Obj>> callback){
        JsonObject query = new JsonObject();

        //matcher
        JsonObject match = new JsonObject();
//        match.putNumber(colName.BillDBCols.PAY_CHANEL,obj.payChanel);

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.BillDBCols.TABLE_PREFIX + String.valueOf(number));
        query.putObject(MongoKeyWords.MATCHER, match);

        //no sort
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();

                ArrayList<BillsDb.Obj> lst = new ArrayList<BillsDb.Obj>();

                if (obj.getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = jsonObjectMessage.body().getArray(MongoKeyWords.RESULT_ARRAY);

                    for (Object o : results) {
                        lst.add(new BillsDb.Obj((JsonObject) o));
                    }
                }
                callback.handle(lst);
            }
        });
    }

    public void deleteBill(int number, Obj obj, final Handler<Boolean> callback)
    {
        JsonObject query    = new JsonObject();
        JsonObject matcher   = new JsonObject();

        matcher.putString(colName.BillDBCols.PROVIDER_ID,obj.providerId);
        matcher.putString(colName.BillDBCols.BILL_ID,obj.billId);
//        matcher.putNumber(colName.BillDBCols.PAY_CHANEL, obj.payChanel);

        query.putString( MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillDBCols.TABLE_PREFIX + String.valueOf(number));
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jObjMsg) {
                JsonObject obj = jObjMsg.body();
                if (obj.getString( MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void addBill(int number, Obj obj, final Handler<Boolean> callback)
    {
        JsonObject jObjNew = createJsonObj(obj);

        JsonObject query  = new JsonObject();
        JsonObject criteria  = new JsonObject();

        criteria.putString(colName.BillDBCols.PROVIDER_ID,obj.providerId);
        criteria.putString(colName.BillDBCols.BILL_ID,obj.billId);
        criteria.putNumber(colName.BillDBCols.PAY_CHANEL,obj.payChanel);

        query.putObject(MongoKeyWords.CRITERIA, criteria);
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillDBCols.TABLE_PREFIX + number);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, jObjNew);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }

    public void setBillAmount(int number, JsonObject jBInf, final Handler<Boolean> callback)
    {
        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillDBCols.TABLE_PREFIX + String.valueOf(number));
        JsonObject matcher   = new JsonObject();

        matcher.putString(colName.BillDBCols.PROVIDER_ID, jBInf.getString(colName.BillDBCols.PROVIDER_ID,""));
        matcher.putString(colName.BillDBCols.BILL_ID, jBInf.getString(colName.BillDBCols.BILL_ID,""));


        query.putObject(MongoKeyWords.CRITERIA, matcher);

        JsonObject newObj = new JsonObject();

        newObj.putNumber(colName.BillDBCols.CHECK, System.currentTimeMillis());
        newObj.putString(colName.BillDBCols.PROVIDER_ID, jBInf.getString(colName.BillDBCols.PROVIDER_ID,""));
        newObj.putString(colName.BillDBCols.BILL_ID, jBInf.getString(colName.BillDBCols.BILL_ID,""));
        newObj.putNumber(colName.BillDBCols.TOTAL_AMOUNT, jBInf.getLong(colName.BillDBCols.TOTAL_AMOUNT));

        newObj.putString(colName.BillDBCols.OWNER_NAME, "");
        newObj.putString(colName.BillDBCols.OWNER_PHONE, "");
        newObj.putString(colName.BillDBCols.OWNER_ADDRESS, "");
        newObj.putString(colName.BillDBCols.BILL_DETAIL, "");

        //extra
        newObj.putNumber(colName.BillDBCols.STATUS, -1);
        newObj.putNumber(colName.BillDBCols.PAY_TYPE, -1);
        newObj.putNumber(colName.BillDBCols.TRANSFER_TYPE, -1);
        newObj.putString(colName.BillDBCols.PAY_SCHEDULER, "");
        newObj.putNumber(colName.BillDBCols.DUE_DATE, -1);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$,newObj);

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }

    public void resetBillAmount(int number, String providerId, String billId, final Handler<Boolean> callback)
    {
        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillDBCols.TABLE_PREFIX + String.valueOf(number));
        JsonObject matcher   = new JsonObject();

        matcher.putString(colName.BillDBCols.PROVIDER_ID, providerId);
        matcher.putString(colName.BillDBCols.BILL_ID, billId);

        query.putObject(MongoKeyWords.CRITERIA, matcher);

        JsonObject newObj = new JsonObject();
        newObj.putNumber(colName.BillDBCols.TOTAL_AMOUNT, 0);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$,newObj);

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED,false);
                callback.handle(result);
            }
        });
    }

    private JsonObject createJsonObj(Obj o){
        JsonObject j = new JsonObject();
        j.putString(colName.BillDBCols.PROVIDER_ID,o.providerId);
        j.putString(colName.BillDBCols.BILL_ID,o.billId);
        j.putString(colName.BillDBCols.BILL_DETAIL,o.billDetail);
        j.putNumber(colName.BillDBCols.STATUS,o.status);
        j.putString(colName.BillDBCols.OWNER_NAME,o.ownerName);
        j.putString(colName.BillDBCols.OWNER_ADDRESS,o.ownerAddress);
        j.putString(colName.BillDBCols.OWNER_PHONE,o.ownerPhone);
        j.putNumber(colName.BillDBCols.TOTAL_AMOUNT,o.totalAmount);
        j.putNumber(colName.BillDBCols.PAY_TYPE,o.payType);
        j.putNumber(colName.BillDBCols.TRANSFER_TYPE,o.tranferType);
        j.putNumber(colName.BillDBCols.DUE_DATE,o.dueDate);
        j.putString(colName.BillDBCols.PAY_SCHEDULER,o.payScheduler);
        j.putNumber(colName.BillDBCols.PAY_CHANEL,o.payChanel);
        j.putNumber(colName.BillDBCols.START_DATE, o.startDate);
        j.putNumber(colName.BillDBCols.END_DATE, o.endDate);
        j.putNumber(colName.BillDBCols.PAY_DATE,o.payDate);

        return j;
    }

}
