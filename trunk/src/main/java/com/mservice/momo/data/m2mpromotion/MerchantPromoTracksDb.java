package com.mservice.momo.data.m2mpromotion;

import com.mservice.momo.data.SettingsDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by hung_thai on 9/26/14.
 */
public class MerchantPromoTracksDb {

    public static final String SUCCESS_CODE = "";

    public static final String TABLE_NAME = "MerchantPromoTrack";

    public static String getStatusText(int status){
        switch (status){
            case MerchantPromoTracksDb.STAT_NEW:
                return "Tạo mới";
            case MerchantPromoTracksDb.STAT_BLOCK:
                return "Tạm khóa";
            case MerchantPromoTracksDb.STAT_USED:
                return "Đã dùng";
            case MerchantPromoTracksDb.STAT_INVALID:
                return "Không hợp lệ";
            case MerchantPromoTracksDb.STAT_USED_ERROR:
                return "Dùng sai";
            default:
                return "Unknown";
        }
    }

    private SettingsDb mSettingsDb;
    private MerchantPromosDb mMerchantPromosDb;

    private Vertx vertx;
    private Logger logger;
    private EventBus eventBus;
    public MerchantPromoTracksDb(EventBus eventBus, Logger logger){
        this.logger =logger;
        this.eventBus =eventBus;
        mSettingsDb = new SettingsDb(eventBus,logger);
        mMerchantPromosDb = new MerchantPromosDb(eventBus,logger);
    }

    private static class ColNames{
        public static final String CODE = "_id";
        public static final String MERCHANT_NUM = "m_num"; //we using the num_list for this field
        public static final String MNUM_LIST = "m_list"; //we using the num_list for this field
        public static final String PROGRAM = "program"; //code cho chuong trinh nao
        public static final String CLIENT_NUM = "c_num";
        public static final String VALUE = "val";
        public static final String STATUS = "stat";
        public static final String CREATE_TIME = "c_time";
        public static final String EXPIRED_TIME = "e_day";
        public static final String CREATE_TRAN_ID = "c_tran_id";
        public static final String CLAIM_TIME = "l_time";
        public static final String CLAIM_ERROR = "l_error";
    }

    public static final int STAT_INVALID = 0;
    public static final int STAT_NEW = 1;
    public static final int STAT_BLOCK = 2;
    public static final int STAT_USED = 3;
    public static final int STAT_USED_ERROR = 4;


    public static class Obj{
        public String code;
        public String merchantNumber = "";
        public String mNumberList = "";
        public String program = "";
        public String clientNumber ;
        public long value;
        public int status;
        public Date createTime;
        public Date expiredTime;
        public long createTranId;
        public Date claimTime;
        public String claimError;

        public Obj(){}
        public Obj(JsonObject jo){
            code = jo.getString(ColNames.CODE,"");
            merchantNumber = jo.getString(ColNames.MERCHANT_NUM,"");
            mNumberList = jo.getString(ColNames.MNUM_LIST,"");
            program = jo.getString(ColNames.PROGRAM,"");

            clientNumber = jo.getString(ColNames.CLIENT_NUM,"");
            value = jo.getLong(ColNames.VALUE, 0);
            status = jo.getInteger(ColNames.STATUS, 0);
            createTime = new Date(jo.getLong(ColNames.CREATE_TIME,0));
            expiredTime = new Date(jo.getLong(ColNames.EXPIRED_TIME,0));
            createTranId = jo.getLong(ColNames.CREATE_TRAN_ID, 0);
            claimTime = new Date(jo.getLong(ColNames.CLAIM_TIME,0));
            claimError = jo.getString(ColNames.CLAIM_ERROR,"");
        }

        public JsonObject toJsonObject(){
            JsonObject jo = new JsonObject();
            jo.putString(ColNames.CODE,code);
            jo.putString(ColNames.MERCHANT_NUM,merchantNumber);
            jo.putString(ColNames.MNUM_LIST,mNumberList);
            jo.putString(ColNames.PROGRAM,program);
            jo.putString(ColNames.CLIENT_NUM, clientNumber);
            jo.putNumber(ColNames.VALUE, value);
            jo.putNumber(ColNames.STATUS,status);
            jo.putNumber(ColNames.CREATE_TIME,createTime.getTime());
            jo.putNumber(ColNames.EXPIRED_TIME, expiredTime.getTime());
            jo.putNumber(ColNames.CREATE_TRAN_ID, createTranId);
            jo.putNumber(ColNames.CLAIM_TIME, claimTime.getTime());
            jo.putString(ColNames.CLAIM_ERROR,claimError);

            return jo;

        }

        public JsonObject toJsonforWeb(){

            JsonObject jo = new JsonObject();
            jo.putString(ColNames.CODE,code);
            jo.putString(ColNames.MERCHANT_NUM,merchantNumber);
            jo.putString(ColNames.MNUM_LIST,mNumberList);
            jo.putString(ColNames.PROGRAM,program);
            jo.putString(ColNames.CLIENT_NUM, clientNumber);
            jo.putString(ColNames.VALUE, Misc.formatAmount(value));
            jo.putString(ColNames.STATUS, getStatusText(status));
            jo.putString(ColNames.CREATE_TIME, Misc.dateVNFormatWithTime(createTime.getTime()));
            jo.putString(ColNames.EXPIRED_TIME, Misc.dateVNFormatWithTime(expiredTime.getTime()));
            jo.putNumber(ColNames.CREATE_TRAN_ID, createTranId);
            jo.putString(ColNames.CLAIM_TIME, claimTime.getTime() > 0 ?Misc.dateVNFormatWithTime(claimTime.getTime()) :"" );
            jo.putString(ColNames.CLAIM_ERROR,claimError);
            return  jo;
        }
    }

    public void countByCouple(String client, String merchant, final Handler<List<Obj>> callback){
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        match.putString(ColNames.MERCHANT_NUM,merchant);
        match.putString(ColNames.CLIENT_NUM,client);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body();
                if (result.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    List<Obj> finalResult = new ArrayList<Obj>();

                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject jsonModel = (JsonObject) results.get(i);
                            Obj model = new Obj(jsonModel);
                            finalResult.add(model);
                        }
                    }
                    callback.handle(finalResult);
                } else {
                    callback.handle(null);
                }


            }
        });

    }

    public void getListByClientProgram(String client, String program, final Handler<List<Obj>> callback){
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        match.putString(ColNames.CLIENT_NUM,client);
        match.putString(ColNames.PROGRAM,program);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body();
                if (result.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    List<Obj> finalResult = new ArrayList<Obj>();

                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject jsonModel = (JsonObject) results.get(i);
                            Obj model = new Obj(jsonModel);
                            finalResult.add(model);
                        }
                    }
                    callback.handle(finalResult);
                } else {
                    callback.handle(null);
                }

            }
        });

    }


    public void search(String code
            ,String merchantAgent
            ,String cusAgent
            ,long fromDate, long toDate, final Handler<List<Obj>> callback){
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        if(!"".equalsIgnoreCase(code)){
            match.putString(ColNames.CODE, code);
        }

        if(!"".equalsIgnoreCase(merchantAgent)){
            match.putString(ColNames.MERCHANT_NUM, merchantAgent);
        }

        if(!"".equalsIgnoreCase(cusAgent)){
            match.putString(ColNames.CLIENT_NUM, cusAgent);
        }

        if(fromDate > 0 && toDate > 0){

            JsonObject logic = new JsonObject();
            logic.putNumber(MongoKeyWords.GREATER_OR_EQUAL,fromDate)
                 .putNumber(MongoKeyWords.LESS_OR_EQUAL, toDate);
            match.putObject(ColNames.CREATE_TIME,logic);
        }

        if(match.getFieldNames().size() > 0){
            query.putObject(MongoKeyWords.MATCHER,match);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE,10000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body();
                if (result.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    List<Obj> finalResult = new ArrayList<Obj>();

                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject jsonModel = (JsonObject) results.get(i);
                            Obj model = new Obj(jsonModel);
                            finalResult.add(model);
                        }
                    }
                    callback.handle(finalResult);
                } else {
                    callback.handle(null);
                }
            }
        });
    }

    public void findByCode(String code, final Handler<Obj> callback){
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        match.putString(ColNames.CODE,code);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body();
                if (result.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonObject jsonObject = event.body().getObject(MongoKeyWords.RESULT, null);

                    if (jsonObject != null) {
                        Obj model = new Obj(jsonObject);
                        callback.handle(model);
                    }else{
                        callback.handle(null);
                    }

                } else {
                    callback.handle(null);
                }
            }
        });
    }

    public void insert(String _code, String _mNumber, String _mList, String _program, String _cNumber, long _val,int _expDay, long _tid, final Handler<Obj> _callback) {
        final Obj o = new Obj();


        o.code = _code;
        o.merchantNumber = _mNumber;
        o.mNumberList = _mList;
        o.program = _program;
        o.clientNumber = _cNumber;
        o.value = _val;
        o.status = STAT_NEW;
        o.createTime = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(o.createTime);
        cal.add(Calendar.DATE, _expDay);

        cal.set(Calendar.HOUR_OF_DAY,23);
        cal.set(Calendar.MINUTE,59);
        cal.set(Calendar.SECOND,59);
        cal.set(Calendar.MILLISECOND,0);
        o.expiredTime = cal.getTime();

        o.createTranId = _tid;
        o.claimTime = new Date(0);
        o.claimError = SUCCESS_CODE;

        //todo do save here, put error to the claimError if

        /*public static final String CODE = "_id";
        public static final String MERCHANT_NUM = "m_num";
        public static final String CLIENT_NUM = "c_num";
        public static final String VALUE = "val";
        public static final String STATUS = "stat";
        public static final String CREATE_TIME = "c_time";
        public static final String EXPIRED_TIME = "e_day";
        public static final String CREATE_TRAN_ID = "c_tran_id";
        public static final String CLAIM_TIME = "l_time";
        public static final String CLAIM_ERROR = "l_error";*/

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT );
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);
        query.putObject(MongoKeyWords.DOCUMENT,o.toJsonObject());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if(!event.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    o.claimError = "co loi";
                    //lay message loi

                    JsonObject error = new JsonObject(event.body().getString("message","{code:-1}"));
                    o.claimError = error.getInteger("code",-1) + "";
                }
                _callback.handle(o);
            }
        });

    }

    public void trimExpiredCode(final Handler<Boolean> callback){
        //delete then retotal all row
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);



        //ngay het han lon hon ngay hien tai
        //{field: {$gt: value} }
        JsonObject expDateLessThan = new JsonObject().putNumber(MongoKeyWords.LESS_THAN,System.currentTimeMillis());
        match.putObject(ColNames.EXPIRED_TIME, expDateLessThan);
        //code chua su dung
        match.putNumber(ColNames.STATUS,STAT_NEW);
        query.putObject(MongoKeyWords.MATCHER, match);


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(true);
            }
        });

    }



    public void setStatus(String code, int curStatus, int newStatus, final Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //matcher
        match.putString(ColNames.CODE, code);
        match.putNumber(ColNames.STATUS, curStatus);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject jsonNew = new JsonObject();
        jsonNew.putNumber(ColNames.STATUS, newStatus);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jsonNew);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
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

    public void setClaimer(String code, String claimer, final Handler<Boolean> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //matcher
        match.putString(ColNames.CODE, code);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject jsonNew = new JsonObject();
        jsonNew.putString(ColNames.MERCHANT_NUM, claimer);

        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, jsonNew);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
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

}
