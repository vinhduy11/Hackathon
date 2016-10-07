package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 3/30/15.
 */
public class CustomCareDb {

    private Vertx vertx;
    private Logger logger;


    public CustomCareDb(Vertx vertx, Logger logger) {
        this.vertx = vertx;
        this.logger = logger;
    }

    public void upsert(Obj o, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CustomCarePromo.table);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CustomCarePromo.number, o.number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, o.toJson());

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.CustomCarePromo.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if(!event.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonObject error = new JsonObject(event.body().getString("message","{code:-1}"));
                    result =error.getInteger("code",-1);
                }
                callback.handle(result);
            }
        });
    }

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CustomCarePromo.table);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.CustomCarePromo.number, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok","").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CustomCarePromo.table);

        if(filter != null && filter.getFieldNames().size() > 0){
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if(joArr != null && joArr.size() > 0){
                    for (int i =0;i< joArr.size();i++){
                        Obj obj = new Obj((JsonObject)joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }


    public void findOne(String phoneNumber, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.CustomCarePromo.table);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.CustomCarePromo.number, phoneNumber);
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




    public static class Obj {

        public String number = ""; // wallet will be get voucher
        public boolean enable = true; // force cho phep tra thuong khong
        public String group = "";  // ma nhom khuyen mai, 1,2.....
        public String groupDesc = ""; // mo ta nhom nhan khuyen mai
        public String upTimeVn = Misc.dateVNFormatWithTime(System.currentTimeMillis()); // thoi gian upload file len he thong

        //promoted
        public int promoCount = 0; // so luong da khuyen mai
        public String proTimeVn = ""; // thoi gian VN tra khuyen mai

        //promtion program
        //format yyyy-MM-dd hh:mm:ss
        public long dateFrom = 0; // chuong trinh khuyen mai bat dau tu ngay
        public String orgDateFrom = "";

        //format yyyy-MM-dd hh:mm:ss
        public long dateTo = 0; // chuong trinh khuyen mai keo dai den ngay
        public String orgDateTo = "";
        public int duration = 0; // thoi gian duoc cong them ke tu khi tao qua
        public int promoValue = 0; //gia tri khuyen mai
        public String momoAgent = ""; //tai khoan dung tra khuyen mai
        public String giftTypeId = ""; // loai voucher quy dinh cac dich vu duoc phep su dung
        public long tranId = 0;
        public String nameCustomer = "";
        public int transCount = 0;
        public Obj() {}

        public Obj(JsonObject jo) {


            number = jo.getString(colName.CustomCarePromo.number, "");
            enable = jo.getBoolean(colName.CustomCarePromo.enable, true);
            group = jo.getString(colName.CustomCarePromo.group, "");
            groupDesc = jo.getString(colName.CustomCarePromo.groupDesc, "");
            upTimeVn = jo.getString(colName.CustomCarePromo.upTimeVn);
            promoCount = jo.getInteger(colName.CustomCarePromo.promoCount, 0);
            proTimeVn = jo.getString(colName.CustomCarePromo.proTimeVn, "");
            dateFrom = jo.getLong(colName.CustomCarePromo.dateFrom, 0);
            orgDateFrom = jo.getString(colName.CustomCarePromo.orgDateFrom, "");
            dateTo = jo.getLong(colName.CustomCarePromo.dateTo, 0);
            orgDateTo = jo.getString(colName.CustomCarePromo.orgDateTo, "");
            duration = jo.getInteger(colName.CustomCarePromo.duration, 0);
            promoValue = jo.getInteger(colName.CustomCarePromo.promoValue, 0);
            momoAgent = jo.getString(colName.CustomCarePromo.momoAgent, "");
            giftTypeId = jo.getString(colName.CustomCarePromo.giftTypeId, "");
            tranId = jo.getLong(colName.CustomCarePromo.tranId, 0);
            nameCustomer = jo.getString(colName.CustomCarePromo.nameCustomer, "");
            transCount = jo.getInteger(colName.CustomCarePromo.transCount, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.CustomCarePromo.number, number);
            jo.putBoolean(colName.CustomCarePromo.enable, enable);
            jo.putString(colName.CustomCarePromo.group, group);
            jo.putString(colName.CustomCarePromo.groupDesc, groupDesc);
            jo.putString(colName.CustomCarePromo.upTimeVn, upTimeVn);
            jo.putNumber(colName.CustomCarePromo.promoCount, promoCount);
            jo.putString(colName.CustomCarePromo.proTimeVn, proTimeVn);
            jo.putNumber(colName.CustomCarePromo.dateFrom, dateFrom);
            jo.putString(colName.CustomCarePromo.orgDateFrom, orgDateFrom);
            jo.putNumber(colName.CustomCarePromo.dateTo, dateTo);
            jo.putString(colName.CustomCarePromo.orgDateTo, orgDateTo);
            jo.putNumber(colName.CustomCarePromo.duration, duration);
            jo.putNumber(colName.CustomCarePromo.promoValue, promoValue);
            jo.putString(colName.CustomCarePromo.momoAgent, momoAgent);
            jo.putString(colName.CustomCarePromo.giftTypeId, giftTypeId);
            jo.putNumber(colName.CustomCarePromo.tranId, tranId);
            jo.putString(colName.CustomCarePromo.nameCustomer, nameCustomer);
            jo.putNumber(colName.CustomCarePromo.transCount, transCount);
            return jo;
        }
    }


}
