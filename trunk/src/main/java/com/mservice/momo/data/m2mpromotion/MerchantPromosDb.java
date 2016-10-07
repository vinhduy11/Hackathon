package com.mservice.momo.data.m2mpromotion;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by hung_thai on 9/25/14.
 */
public class MerchantPromosDb {

    public static final String TABLE_NAME = "MerchantPromo";

    private Logger logger;
    private EventBus eventBus;
    public MerchantPromosDb(EventBus eventBus, Logger logger){
        this.logger =logger;
        this.eventBus =eventBus;
    }

    public static class ColNames{
        public static final String NUMBER = "_id"; // so dien thoai chinh
        public static final String NUMLIST = "num_list"; // cac so dien thoai thuc hien thanh toan cho quan nay
        public static final String GROUP = "group"; // quan thuoc nhom nao (saigon, hanoi, all)
        public static final String PROGRAM = "program"; // quan dang chay chuong trinh nao (de kiem tra tong so luong code theo chuong trinh)
        public static final String NAME = "name"; // ten quan cafe
        public static final String FROM_DATE = "from_date"; // tu ngay
        public static final String TO_DATE = "to_date";// den ngay
        public static final String TOTAL_CODE = "total_code";// tong so code sinh ra
        public static final String USED_CODE = "used_code"; // so luong code da dung
        public static final String MAX_CODE = "max_code"; // chan tren so code sinh
        public static final String TOTAL_VAL = "total_val"; // tong gia tri da khuyen mai
        public static final String MAX_VAL = "max_val"; // chan tren tong gia tri khuyen mai toi da
        public static final String VAL_LIST = "val_list"; //15.000,10.000,5.000 // danh sach giao cho cac lan khuyen mai
        public static final String EXPIRE_DAY = "exp_day"; // so ngay het hanh ke tu khi code sinh ra
        public static final String SOURCE_ACC = "source_acc"; // tai khoan khuyen mai
        public static final String SOURCE_PIN = "source_pin"; // mat khau tai khoan khuyen mai
        public static final String COMMENT = "comment"; //15.000,10.000,5.000
        public static final String THRESHOLD = "threshold";//vuot qua so tien nay moi thuc hien chi tra
        public static final String MAX_TRAN_PER_DAY = "max_tran_per_day";//So lan toi da chuyen tien tu 1 so --> merchant

    }

    public static class Obj{
        public String number = "";
        public ArrayList<String> numList = new ArrayList<>();
        public String group = "";
        public String program = "";

        public String name = "";
        public Date fromDate = new Date(0);
        public Date toDate = new Date(0);
        public int totalCode = 0;
        public int usedCode = 0;
        public int maxCode = 0;
        public long totalVal = 0;
        public long maxVal = 0;
        public long threshold = 0;

        public ArrayList<Long> valList = new ArrayList<>();
        public int expireDay = 0;
        public String sourceAcc="" ;
        public String sourcePin="" ;
        public String comment;
        public int maxTranPerDay = 0;

        public Obj(){}
        public Obj(JsonObject jo){
            number = jo.getString(ColNames.NUMBER,"");
            JsonArray array = jo.getArray(ColNames.NUMLIST, new JsonArray());

            numList = new ArrayList<String>(array.toList()) ;
            group = jo.getString(ColNames.GROUP,"");
            program = jo.getString(ColNames.PROGRAM,"");

            name = jo.getString(ColNames.NAME,"");
            fromDate = new Date(jo.getLong(ColNames.FROM_DATE,0));
            toDate = new Date(jo.getLong(ColNames.TO_DATE,0));
            totalCode = jo.getInteger(ColNames.TOTAL_CODE,0);
            usedCode = jo.getInteger(ColNames.USED_CODE,0);
            maxCode= jo.getInteger(ColNames.MAX_CODE,0);
            totalVal= jo.getLong(ColNames.TOTAL_VAL, 0);
            maxVal= jo.getLong(ColNames.MAX_VAL,0);
            threshold= jo.getLong(ColNames.THRESHOLD,0);
            maxTranPerDay= jo.getInteger(ColNames.MAX_TRAN_PER_DAY,0);

            String list = jo.getString(ColNames.VAL_LIST, "");
            String[] vals = list.split(",");
            valList = new ArrayList<>();
            for(String s : vals){
                long l = DataUtil.stringToUNumber(s);
                if(l > 0){
                    valList.add(l);
                }
            }

            expireDay = jo.getInteger(ColNames.EXPIRE_DAY,0);
            sourceAcc = jo.getString(ColNames.SOURCE_ACC,"");
            sourcePin = jo.getString(ColNames.SOURCE_PIN,"");
            comment = jo.getString(ColNames.COMMENT,"");
        }

        public JsonObject toJsonObject(){
            JsonObject jo = new JsonObject();
            jo.putString(ColNames.NUMBER,number);

            if(!numList.contains(number)){
                numList.add(number);
            }

            JsonArray array = new JsonArray(numList.toArray());
            jo.putArray(ColNames.NUMLIST,array);

            jo.putString(ColNames.GROUP,group);
            jo.putString(ColNames.PROGRAM,program);

            jo.putString(ColNames.NAME,name);
            jo.putNumber(ColNames.FROM_DATE,fromDate.getTime());
            jo.putNumber(ColNames.TO_DATE,toDate.getTime());

            jo.putNumber(ColNames.TOTAL_CODE,totalCode);
            jo.putNumber(ColNames.USED_CODE,usedCode);
            jo.putNumber(ColNames.MAX_CODE,maxCode);
            jo.putNumber(ColNames.TOTAL_VAL, totalVal);
            jo.putNumber(ColNames.MAX_VAL,maxVal);
            jo.putNumber(ColNames.THRESHOLD,threshold);

            jo.putNumber(ColNames.EXPIRE_DAY,expireDay);
            jo.putString(ColNames.SOURCE_ACC,sourceAcc);
            jo.putString(ColNames.SOURCE_PIN,sourcePin);
            jo.putString(ColNames.COMMENT,comment);
            jo.putNumber(ColNames.MAX_TRAN_PER_DAY,maxTranPerDay);

            String list = "";

            for(long l : valList){
                list = list + l + ",";
            }

            if(list.endsWith(",")){
                list = list.substring(0,list.length() - 1);
            }
            jo.putString(ColNames.VAL_LIST,list);

            return jo;
            
        }

    }

    public void increase(String field, String merchant, long delta,final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putNumber(field,delta);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);
        JsonObject match   = new JsonObject();

        match.putString(ColNames.NUMBER, merchant);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }


    //use when inser/update from web page admin
    public void upsertMerchant(Obj obj, final Handler<Boolean> callback){
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        JsonObject data = obj.toJsonObject();
//        data.removeField(colName.PromoCols.ID);

        JsonObject criteria = new JsonObject();
        criteria.putString("_id", obj.number);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, data);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                String result = obj.getString(MongoKeyWords.STATUS, "ko");
                if (obj!=null && "ok".equalsIgnoreCase(result)) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });

    }

    public void findOneByMerchantNumber(final String merchantNumber, final Handler<Obj> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //matcher
        JsonObject matcher = new JsonObject();
        matcher.putString(ColNames.NUMBER, merchantNumber);

        //add matcher
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                Obj obj = null;

                JsonObject results = message.body().getObject(MongoKeyWords.RESULT);

                if(results != null){
                    obj = new Obj(results);
                }
                callback.handle(obj);
            }
        });
    }

    public void findOneByNumberInList(final String numberInList, final Handler<Obj> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //matcher
        JsonObject matcher = new JsonObject();
        JsonArray array = new JsonArray();
        String[] arr = numberInList.split(",");

        for(int i =0;i< arr.length;i ++){
            array.add(arr[i]);
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$,array);
        matcher.putObject(ColNames.NUMLIST, in);

        //add matcher
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                Obj obj = null;

                JsonObject results = message.body().getObject(MongoKeyWords.RESULT);

                if(results != null){
                    obj = new Obj(results);
                }
                callback.handle(obj);
            }
        });
    }

    public void findMoreByNumberInList(final String numberInList, final Handler<ArrayList<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //matcher
        JsonObject matcher = new JsonObject();
        JsonArray array = new JsonArray();
        String[] arr = numberInList.split(",");

        for(int i =0;i< arr.length;i ++){
            array.add(arr[i]);
        }

        JsonObject in = new JsonObject();
        in.putArray(MongoKeyWords.IN_$,array);
        matcher.putObject(ColNames.NUMLIST, in);

        //add matcher
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray array1 = message.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if(array1 != null && array1.size() > 0){

                    for (int i=0;i< array1.size();i++){
                        JsonObject jo = array1.get(i);
                        arrayList.add(new Obj(jo));
                    }
                }

                callback.handle(arrayList);
            }
        });
    }


    public void getAllMerchant( final Handler<List<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);
        //matcher
        JsonObject matcher = new JsonObject();
        //add matcher
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body();
                if (result.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    List<Obj> finalResult = new ArrayList<>();

                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject jsonModel = results.get(i);
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

    public void getMerchantWithFilter(String merchant, String program, String groupMapped, final Handler<List<Obj>> callback){
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, TABLE_NAME);

        //liker
        JsonObject matcher = new JsonObject();

        if(!"".equalsIgnoreCase(merchant)){
            JsonArray array = new JsonArray();
            array.add(merchant);
            JsonObject in = new JsonObject();
            in.putArray(MongoKeyWords.IN_$,array);

            matcher.putObject(ColNames.NUMLIST, in);
        }

        if(!"".equalsIgnoreCase(program)){
            matcher.putString(ColNames.PROGRAM, program);
        }

        if(!"".equalsIgnoreCase(groupMapped) && !"all".equalsIgnoreCase(groupMapped)){
            matcher.putString(ColNames.GROUP,groupMapped);
        }

        query.putObject(MongoKeyWords.MATCHER, matcher);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            public void handle(Message<JsonObject> event) {
                JsonObject result = event.body();
                if (result.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    List<Obj> finalResult = new ArrayList<>();

                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject jsonModel = results.get(i);
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

}
