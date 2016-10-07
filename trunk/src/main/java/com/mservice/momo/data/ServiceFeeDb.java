package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 10/3/15.
 */
public class ServiceFeeDb {

    private EventBus eventBus;
    private Logger logger;
    private Vertx vertx;

    public ServiceFeeDb(Vertx vertx, Logger logger) {
        this.eventBus = vertx.eventBus();
        this.logger = logger;
        this.vertx = vertx;
    }

    public void getServices(int channel, int trantype, int inout_city, final Handler<ArrayList<Obj>> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceFeeDBCols.TABLE);

        //matcher
        match.putNumber(colName.ServiceFeeDBCols.CHANNEL, channel);
        match.putNumber(colName.ServiceFeeDBCols.TRANTYPE, trantype);
        match.putNumber(colName.ServiceFeeDBCols.INOUT_CITY, inout_city);

        query.putObject(MongoKeyWords.MATCHER, match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ArrayList<Obj> arrayList = null;
                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        arrayList = new ArrayList<>();
                        for (Object o : results) {
                            arrayList.add(new Obj((JsonObject) o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void upsert(String bankid
            , int channel
            , int trantype
            , double dynamicfee
            , long staticfee
            , int inoutcity
            , String bankname
            , int feetype
            , long minval
            , long maxval
            , String bankcore
            , final Handler<Boolean> callback) {
        JsonObject objNew = buildObj(bankid, channel, trantype, dynamicfee, staticfee, inoutcity, bankname, feetype, minval, maxval, bankcore);

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceFeeDBCols.TABLE);

        //matcher
        JsonObject criteria = new JsonObject();
        criteria.putString(colName.ServiceFeeDBCols.SERVICE_ID, bankid);
        criteria.putNumber(colName.ServiceFeeDBCols.CHANNEL, channel);
        criteria.putNumber(colName.ServiceFeeDBCols.TRANTYPE, trantype);
        criteria.putNumber(colName.ServiceFeeDBCols.INOUT_CITY, inoutcity);

        query.putObject(MongoKeyWords.CRITERIA, criteria);
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        //upsert
        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json = event.body();
                if (json != null && json.getString(MongoKeyWords.STATUS, "ko").equals("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }

    public void removeObj(String id, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceFeeDBCols.TABLE);

        match.putString(colName.ServiceFeeDBCols.ID, id);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void update(final Obj obj, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.ServiceFeeDBCols.TABLE);

        JsonObject match = new JsonObject();
        if (obj.KEY != null && !obj.KEY.isEmpty()) {
            match.putString(colName.ServiceFeeDBCols.ID, obj.KEY);
        } else {
            match.putString(colName.ServiceFeeDBCols.SERVICE_ID, obj.SERVICE_ID);
            match.putNumber(colName.ServiceFeeDBCols.TRANTYPE, obj.TRANTYPE);
            match.putNumber(colName.ServiceFeeDBCols.CHANNEL, obj.CHANNEL);
            match.putNumber(colName.ServiceFeeDBCols.INOUT_CITY, obj.INOUT_CITY);
        }

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject temp = obj.toJsonObject();
        temp.removeField(colName.ServiceFeeDBCols.ID);

        JsonObject setObj = new JsonObject();
        setObj.putObject(MongoKeyWords.SET_$, temp);

        query.putObject(MongoKeyWords.OBJ_NEW, setObj);
        query.putBoolean(MongoKeyWords.UPSERT, true);

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

    private JsonObject buildObj(String bankid
            , int channel
            , int trantype
            , double dynamicfee
            , long staticfee
            , int inout_city
            , String bankname
            , int feetype
            , long minval
            , long maxval
            , String bankcore
    ) {
        JsonObject j = new JsonObject();
        j.putString(colName.ServiceFeeDBCols.SERVICE_ID, bankid);
        j.putString(colName.ServiceFeeDBCols.SERVICE_NAME, bankname);
        j.putNumber(colName.ServiceFeeDBCols.CHANNEL, channel);
        j.putNumber(colName.ServiceFeeDBCols.TRANTYPE, trantype);
        j.putNumber(colName.ServiceFeeDBCols.DYNAMIC_FEE, dynamicfee);
        j.putNumber(colName.ServiceFeeDBCols.STATIC_FEE, staticfee);
        j.putNumber(colName.ServiceFeeDBCols.INOUT_CITY, inout_city);
        j.putNumber(colName.ServiceFeeDBCols.FEE_TYPE, feetype);
        j.putNumber(colName.ServiceFeeDBCols.MIN_VALUE, minval);
        j.putNumber(colName.ServiceFeeDBCols.MAX_VALUE, maxval);
        return j;
    }

    public void getFee(String bankid
            , int channel
            , int trantype
            , int inout_city
            , int feetype
            , long amount   // dung de tinh phi theo khoang ve sau nay
            , final Handler<Obj> callback) {

        if (feetype == MomoProto.FeeType.CASH_VALUE) {
            Obj obj = new Obj();
            //rut tien mat
            obj.FEE_TYPE = MomoProto.FeeType.CASH_VALUE;
            callback.handle(obj);
            return;
        }

        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_FEE;
        serviceReq.bankId = bankid;
        serviceReq.channel = channel;
        serviceReq.tranType = trantype;
        serviceReq.inoutCity = inout_city;
        serviceReq.amount = amount;

        eventBus.send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                Obj obj = null;
                if (message.body() != null) {
                    obj = new Obj(message.body());
                }

                callback.handle(obj);
            }
        });
    }

    public void getAll(final Handler<ArrayList<Obj>> callback) {
        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceFeeDBCols.TABLE);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServiceFeeDBCols.SERVICE_NAME, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                if (message.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if (results != null && results.size() > 0) {
                        arrayList = new ArrayList<>();
                        for (Object o : results) {
                            arrayList.add(new Obj((JsonObject) o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public static class Obj {

        public String KEY = "";
        public String SERVICE_ID = "";
        public int CHANNEL = 0; // banknet/smartlink/onepay
        public int TRANTYPE = 0; // bieu phi ap dung cho loai giao dich nao
        public double DYNAMIC_FEE = 0; // phi dong
        public int STATIC_FEE = 0; // phi tinh
        public int INOUT_CITY = 0; // trong,ngoai thanh pho
        public String SERVICE_NAME = ""; //ten ngan hang
        public int FEE_TYPE = 0; // khong phi : 1 co phi
        public long MIN_VALUE = 0;
        public long MAX_VALUE = 0;
        public String CORE_BANK_AGENT = "";

        public Obj(JsonObject jo) {
            SERVICE_ID = jo.getString(colName.ServiceFeeDBCols.SERVICE_ID, "");
            CHANNEL = jo.getInteger(colName.ServiceFeeDBCols.CHANNEL, 0);
            TRANTYPE = jo.getInteger(colName.ServiceFeeDBCols.TRANTYPE, 0);
            DYNAMIC_FEE = Double.parseDouble(String.valueOf(jo.getNumber(colName.ServiceFeeDBCols.DYNAMIC_FEE, 0)));
            STATIC_FEE = jo.getInteger(colName.ServiceFeeDBCols.STATIC_FEE, 0);
            INOUT_CITY = jo.getInteger(colName.ServiceFeeDBCols.INOUT_CITY, 0);
            KEY = jo.getString(colName.ServiceFeeDBCols.ID, "");
            SERVICE_NAME = jo.getString(colName.ServiceFeeDBCols.SERVICE_NAME, "");
            FEE_TYPE = jo.getInteger(colName.ServiceFeeDBCols.FEE_TYPE, 0);
            MIN_VALUE = jo.getLong(colName.ServiceFeeDBCols.MIN_VALUE, 0);
            MAX_VALUE = jo.getLong(colName.ServiceFeeDBCols.MAX_VALUE, 0);
        }

        public Obj() {
        }

        public JsonObject toJsonObject() {
            JsonObject result = new JsonObject();

            result.putString(colName.ServiceFeeDBCols.ID, KEY);
            result.putString(colName.ServiceFeeDBCols.SERVICE_ID, SERVICE_ID);
            result.putString(colName.ServiceFeeDBCols.SERVICE_NAME, SERVICE_NAME);
            result.putNumber(colName.ServiceFeeDBCols.TRANTYPE, TRANTYPE);
            result.putNumber(colName.ServiceFeeDBCols.CHANNEL, CHANNEL);
            result.putNumber(colName.ServiceFeeDBCols.INOUT_CITY, INOUT_CITY);
            result.putNumber(colName.ServiceFeeDBCols.DYNAMIC_FEE, DYNAMIC_FEE);
            result.putNumber(colName.ServiceFeeDBCols.STATIC_FEE, STATIC_FEE);

            result.putNumber(colName.ServiceFeeDBCols.FEE_TYPE, FEE_TYPE);
            result.putNumber(colName.ServiceFeeDBCols.MIN_VALUE, MIN_VALUE);
            result.putNumber(colName.ServiceFeeDBCols.MAX_VALUE, MAX_VALUE);
            return result;
        }

        public boolean isInvalid() {
            return true;
        }
    }
}
