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
 * Created by concu on 5/14/14.
 */
public class FeeDb {
    private EventBus eventBus;
    private Logger logger;
    private Vertx vertx;

    public FeeDb(Vertx vertx, Logger logger) {
        this.eventBus = vertx.eventBus();
        this.logger = logger;
        this.vertx = vertx;
    }

    public void getBanks(int channel, int trantype, int inout_city, final Handler<ArrayList<Obj>> callback) {
        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.FeeDBCols.TABLE);

        //matcher
        match.putNumber(colName.FeeDBCols.CHANNEL, channel);
        match.putNumber(colName.FeeDBCols.TRANTYPE, trantype);
        match.putNumber(colName.FeeDBCols.INOUT_CITY, inout_city);

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
        query.putString(MongoKeyWords.COLLECTION, colName.FeeDBCols.TABLE);

        //matcher
        JsonObject criteria = new JsonObject();
        criteria.putString(colName.FeeDBCols.BANKID, bankid);
        criteria.putNumber(colName.FeeDBCols.CHANNEL, channel);
        criteria.putNumber(colName.FeeDBCols.TRANTYPE, trantype);
        criteria.putNumber(colName.FeeDBCols.INOUT_CITY, inoutcity);

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
        query.putString(MongoKeyWords.COLLECTION, colName.FeeDBCols.TABLE);

        match.putString(colName.FeeDBCols.ID, id);

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

        query.putString(MongoKeyWords.COLLECTION, colName.FeeDBCols.TABLE);

        JsonObject match = new JsonObject();
        if (obj.KEY != null && !obj.KEY.isEmpty()) {
            match.putString(colName.FeeDBCols.ID, obj.KEY);
        } else {
            match.putString(colName.FeeDBCols.BANKID, obj.BANKID);
            match.putNumber(colName.FeeDBCols.TRANTYPE, obj.TRANTYPE);
            match.putNumber(colName.FeeDBCols.CHANNEL, obj.CHANNEL);
            match.putNumber(colName.FeeDBCols.INOUT_CITY, obj.INOUT_CITY);
        }

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject temp = obj.toJsonObject();
        temp.removeField(colName.FeeDBCols.ID);

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
        j.putString(colName.FeeDBCols.BANKID, bankid);
        j.putString(colName.FeeDBCols.BANK_NAME, bankname);
        j.putNumber(colName.FeeDBCols.CHANNEL, channel);
        j.putNumber(colName.FeeDBCols.TRANTYPE, trantype);
        j.putNumber(colName.FeeDBCols.DYNAMIC_FEE, dynamicfee);
        j.putNumber(colName.FeeDBCols.STATIC_FEE, staticfee);
        j.putNumber(colName.FeeDBCols.INOUT_CITY, inout_city);
        j.putNumber(colName.FeeDBCols.FEE_TYPE, feetype);
        j.putNumber(colName.FeeDBCols.MIN_VALUE, minval);
        j.putNumber(colName.FeeDBCols.MAX_VALUE, maxval);
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
        serviceReq.PackageId = "";
        serviceReq.PackageType = "";
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
        query.putString(MongoKeyWords.COLLECTION, colName.FeeDBCols.TABLE);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.FeeDBCols.BANK_NAME, 1);
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
        public String BANKID = "";
        public int CHANNEL = 0; // banknet/smartlink/onepay
        public int TRANTYPE = 0; // bieu phi ap dung cho loai giao dich nao
        public double DYNAMIC_FEE = 0; // phi dong
        public int STATIC_FEE = 0; // phi tinh
        public int INOUT_CITY = 0; // trong,ngoai thanh pho
        public String BANK_NAME = ""; //ten ngan hang
        public int FEE_TYPE = 0; // khong phi : 1 co phi
        public long MIN_VALUE = 0;
        public long MAX_VALUE = 0;
        public String CORE_BANK_AGENT = "";

        public Obj(JsonObject jo) {
            BANKID = jo.getString(colName.FeeDBCols.BANKID, "");
            CHANNEL = jo.getInteger(colName.FeeDBCols.CHANNEL, 0);
            TRANTYPE = jo.getInteger(colName.FeeDBCols.TRANTYPE, 0);
            DYNAMIC_FEE = Double.parseDouble(String.valueOf(jo.getNumber(colName.FeeDBCols.DYNAMIC_FEE, 0)));
            STATIC_FEE = jo.getInteger(colName.FeeDBCols.STATIC_FEE, 0);
            INOUT_CITY = jo.getInteger(colName.FeeDBCols.INOUT_CITY, 0);
            KEY = jo.getString(colName.FeeDBCols.ID, "");
            BANK_NAME = jo.getString(colName.FeeDBCols.BANK_NAME, "");
            FEE_TYPE = jo.getInteger(colName.FeeDBCols.FEE_TYPE, 0);
            MIN_VALUE = jo.getLong(colName.FeeDBCols.MIN_VALUE, 0);
            MAX_VALUE = jo.getLong(colName.FeeDBCols.MAX_VALUE, 0);
        }

        public Obj() {
        }

        public JsonObject toJsonObject() {
            JsonObject result = new JsonObject();

            result.putString(colName.FeeDBCols.ID, KEY);
            result.putString(colName.FeeDBCols.BANKID, BANKID);
            result.putString(colName.FeeDBCols.BANK_NAME, BANK_NAME);
            result.putNumber(colName.FeeDBCols.TRANTYPE, TRANTYPE);
            result.putNumber(colName.FeeDBCols.CHANNEL, CHANNEL);
            result.putNumber(colName.FeeDBCols.INOUT_CITY, INOUT_CITY);
            result.putNumber(colName.FeeDBCols.DYNAMIC_FEE, DYNAMIC_FEE);
            result.putNumber(colName.FeeDBCols.STATIC_FEE, STATIC_FEE);

            result.putNumber(colName.FeeDBCols.FEE_TYPE, FEE_TYPE);
            result.putNumber(colName.FeeDBCols.MIN_VALUE, MIN_VALUE);
            result.putNumber(colName.FeeDBCols.MAX_VALUE, MAX_VALUE);
            return result;
        }

        public boolean isInvalid() {
            return true;
        }
    }
}
