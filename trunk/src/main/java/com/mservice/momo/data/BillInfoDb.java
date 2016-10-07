package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 3/18/15.
 */
public class BillInfoDb {

    private Vertx vertx;
    private Logger logger;
    public BillInfoDb(Vertx vertx,Logger logger){
        this.vertx = vertx;
        this.logger = logger;
    }

    public static class Obj{
        public  String id="_id"; // id cua dong nay
        public  int number=0; // vi check
        public  String billId =""; // ma hoa dong
        public  String providerId=""; // nha cung cap dich vu
        public  long amount = 0; // tong tin thanh toan

        public  String bills = ""; //chi tiet hoa don
        public  String customerName =""; // ten khach hang
        public  String customerAddress=""; // dia chi khach hang
        public  String customerPhone =""; // dien thoai khach hang
        public  String servicePackage = ""; // goi dich vu khach hang dang su dung

        public  long checkedTime=0; // thoi gian check cuoc thanh cong
        public  String checkedTimeVn=""; // thoi gian check cuoc thanh cong vn
        public  boolean checked = false; // da check hoac chua check
        public  boolean forceCheck=false; // force or not check

        public Obj(){}
        public Obj(JsonObject jo){
            id = jo.getString(colName.BillInfo.id,"");
            number = jo.getInteger(colName.BillInfo.number, 0);
            billId = jo.getString(colName.BillInfo.billId,"");
            providerId = jo.getString(colName.BillInfo.providerId,"");
            amount = jo.getLong(colName.BillInfo.amount, 0);

            bills = jo.getString(colName.BillInfo.bills,"");
            customerName = jo.getString(colName.BillInfo.customerName,"");
            customerAddress = jo.getString(colName.BillInfo.customerAddress,"");
            customerPhone = jo.getString(colName.BillInfo.customerPhone,"");
            servicePackage = jo.getString(colName.BillInfo.servicePackage,"");
            checkedTime = jo.getLong(colName.BillInfo.checkedTime,0);
            checked = jo.getBoolean(colName.BillInfo.checked,false);
            forceCheck = jo.getBoolean(colName.BillInfo.forceCheck,false);
        }

        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.BillInfo.number,number);
            jo.putString(colName.BillInfo.billId, billId);
            jo.putString(colName.BillInfo.providerId, providerId);
            jo.putNumber(colName.BillInfo.amount, amount);
            jo.putString(colName.BillInfo.bills, bills);
            jo.putString(colName.BillInfo.customerName, customerName);
            jo.putString(colName.BillInfo.customerAddress, customerAddress);
            jo.putString(colName.BillInfo.customerPhone, customerPhone);
            jo.putString(colName.BillInfo.servicePackage, servicePackage);
            jo.putNumber(colName.BillInfo.checkedTime,checkedTime);
            jo.putString(colName.BillInfo.checkedTimeVn, checkedTimeVn);
            jo.putBoolean(colName.BillInfo.checked, checked);
            jo.putBoolean(colName.BillInfo.forceCheck, forceCheck);
            return jo;
        }

    }

    /**
     * update or insert new document with criteria billId and providerId
     * @param obj the object that will insert or update to DB
     * @param callback the result of this operation
     */
    public void upsert(Obj obj, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillInfo.table);

        JsonObject joUp = obj.toJson();
        JsonObject criteria   = new JsonObject();

        //criteria.putNumber(colName.BillInfo.number,obj.number);
        criteria.putString(colName.BillInfo.billId, obj.billId);
        criteria.putString(colName.BillInfo.providerId, obj.providerId);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUp);
        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject obj = message.body();

                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    /**
     * update some fields to existed record on DB by providerId and billId
     * @param providerId the provider Id of the service
     * @param billId    the billId
     * @param joUpdate  the fields that will be updated
     * @param callback the result of this operation
     */
    public void updatePartial(String providerId
                                ,String billId
                                ,JsonObject joUpdate
                                ,final Handler<Boolean> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.BillInfo.table);

        JsonObject criteria   = new JsonObject();
        //criteria.putNumber(colName.BillInfo.number,obj.number);
        criteria.putString(colName.BillInfo.billId, billId);
        criteria.putString(colName.BillInfo.providerId, providerId);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);
        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject obj = message.body();

                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

}
