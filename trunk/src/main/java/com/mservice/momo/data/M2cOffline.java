package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 4/26/14.
 */
public class M2cOffline {
    EventBus eventBus;
    Logger logger;
    public M2cOffline(EventBus eb, Logger log){
        eventBus = eb;
        logger = log;
    }

    public static class Obj{
        public int SOURCE_PHONE;
        public int DESTINATION_PHONE;
        public String NAME;
        public String CARD_ID;
        public long AMOUNT;
        public String NOTICE;
        public long CREATE_TIME;
        public long UPDATE_TIME;
        public String UPDATE_BY;
        public String STATUS;
        public int ERROR;
        public String REMARK;
        public long TRAN_ID;
        public String SHORT_LINK;
        //for more
        public long COMMAND_INDEX;
        public int MESSAGE_TYPE;
        public long SEED_NUMBER;
        public long SMS_SENDED;
        public String CODE;

        public Obj(JsonObject jo){
            SOURCE_PHONE = jo.getInteger(colName.M2NumberCols.SOURCE_PHONE,0);
            DESTINATION_PHONE = jo.getInteger(colName.M2NumberCols.DESTINATION_PHONE, 0);
            NAME = jo.getString(colName.M2NumberCols.NAME,"");
            CARD_ID = jo.getString(colName.M2NumberCols.CARD_ID,"");
            AMOUNT =jo.getLong(colName.M2NumberCols.AMOUNT,0);
            NOTICE = jo.getString(colName.M2NumberCols.NOTICE,"");
            CREATE_TIME = jo.getLong(colName.M2NumberCols.CREATE_TIME,0);
            UPDATE_TIME= jo.getLong(colName.M2NumberCols.UPDATE_TIME,0);
            UPDATE_BY = jo.getString(colName.M2NumberCols.UPDATE_BY, "");;
            STATUS = jo.getString(colName.M2NumberCols.STATUS, "");;
            ERROR = jo.getInteger(colName.M2NumberCols.ERROR, 0);
            REMARK = jo.getString(colName.M2NumberCols.REMARK,"");
            TRAN_ID = jo.getLong(colName.M2NumberCols.TRAN_ID,0);
            SHORT_LINK =jo.getString(colName.M2NumberCols.SHORT_LINK,"");
            SEED_NUMBER = jo.getLong(colName.M2NumberCols.SEED_NUMBER, 0);
            COMMAND_INDEX =jo.getLong(colName.M2NumberCols.COMMAND_INDEX,0);
            MESSAGE_TYPE =jo.getInteger(colName.M2NumberCols.MESSAGE_TYPE,0);
            SMS_SENDED = jo.getInteger(colName.M2NumberCols.SMS_SENDED,1);
            CODE = jo.getString(colName.M2NumberCols.CODE,SHORT_LINK);

        }
    }

    public void insert(final int s_phone
                            ,final int des_phone
                            ,String name
                            ,String card_id
                            ,long amount
                            ,String notice
                            ,long tranId
                            ,String link
                            ,int error
                            ,final Handler<Integer> cb){
        JsonObject jo = buildJO(s_phone,des_phone,name,card_id,amount,notice, tranId,link,error);
        addNew(jo, new Handler<Integer>() {
            @Override
            public void handle(Integer result) {
                logger.info("M2cOffline upsert : " + result);
                cb.handle(result);
            }
        });
    }

    public void incSmsSent(long tranId,final Handler<Boolean> callback){
        //new object
        JsonObject newJson = new JsonObject();
        newJson.putNumber(colName.M2NumberCols.SMS_SENDED,1);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);

        JsonObject match   = new JsonObject();
        match.putNumber(colName.M2NumberCols.TRAN_ID, tranId);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJson);
        query.putObject(MongoKeyWords.OBJ_NEW, set);

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

    private void addNew(JsonObject objNew, final Handler<Integer> cb){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);
        query.putObject(MongoKeyWords.DOCUMENT,objNew);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if(!event.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonObject error = new JsonObject(event.body().getString("message","{code:-1}"));
                    result =error.getInteger("code",-1);
                }
                cb.handle(result);
            }
        });
    }

    private JsonObject buildJO(int src_phone
                                ,int des_phone
                                ,String name
                                ,String card_id
                                ,long amount
                                ,String notice
                                ,long tranId
                                ,String link
                                ,int error){
       JsonObject j = new JsonObject();

        j.putString(colName.M2NumberCols.CODE,link);
        j.putNumber(colName.M2NumberCols.SOURCE_PHONE, src_phone);
        j.putNumber(colName.M2NumberCols.DESTINATION_PHONE, des_phone);

        j.putString(colName.M2NumberCols.NAME,name);
        j.putString(colName.M2NumberCols.CARD_ID,card_id);
        j.putNumber(colName.M2NumberCols.AMOUNT, amount);
        j.putString(colName.M2NumberCols.REMARK,notice);
        j.putNumber(colName.M2NumberCols.CREATE_TIME,System.currentTimeMillis());

        j.putNumber(colName.M2NumberCols.UPDATE_TIME, 0);
        j.putString(colName.M2NumberCols.UPDATE_BY, "");
        j.putString(colName.M2NumberCols.STATUS, colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW));
        j.putNumber(colName.M2NumberCols.ERROR, error);
        j.putString(colName.M2NumberCols.REMARK, "");
        j.putNumber(colName.M2NumberCols.TRAN_ID, tranId);

        j.putString(colName.M2NumberCols.SHORT_LINK, link);

        j.putNumber(colName.M2NumberCols.SMS_SENDED,1);

        return j;
    }


    public void getObject(final String link, final Handler<Obj> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);


        JsonArray array = new JsonArray();

        array.add( new JsonObject().putString(colName.M2NumberCols.SHORT_LINK,link));
        array.add( new JsonObject().putString(colName.M2NumberCols.CODE,link));

        //matcher
        match.putArray("$or",array);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                    JsonObject obj  = event.body().getObject(MongoKeyWords.RESULT);
                    if(obj != null){
                        callback.handle(new Obj(obj));
                        return;
                    }
                    callback.handle(null);
            }
        });
    }

    public void getObjectOld(final String link, final Handler<Obj> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);

        //matcher
        match.putString(colName.M2NumberCols.SHORT_LINK,link);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        query.putNumber(MongoKeyWords.LIMIT,1);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if(event.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray arr  = event.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(arr != null && arr.size()>0){
                        callback.handle(new Obj((JsonObject) arr.get(0)));
                        return;
                    }
                    callback.handle(null);
                }else{
                    callback.handle(null);
                }
            }
        });
    }

    public void updateAndGetObjectByTranId(final long tranId,final String status, final String updateBy, final Handler<Boolean> callback){
        //object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putString(colName.M2NumberCols.STATUS,status);
        newJsonObj.putNumber(colName.M2NumberCols.UPDATE_TIME,System.currentTimeMillis());
        newJsonObj.putString(colName.M2NumberCols.UPDATE_BY,updateBy);

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);

        //matcher
        JsonObject criteria = new JsonObject();
        criteria.putNumber(colName.M2NumberCols.TRAN_ID, tranId);
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);


        /*JsonObject newJson = new JsonObject();
        newJson.putNumber(colName.M2NumberCols.SMS_SENDED,1);
        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJson);
        query.putObject(MongoKeyWords.OBJ_NEW, set);*/


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json  = jsonObjectMessage.body();
                //
                if(json!= null && json.getString(MongoKeyWords.STATUS,"ko").equalsIgnoreCase("ok")){
                    callback.handle(true);
                }
                else{
                    callback.handle(false);
                }
            }
        });
    }
    public void updateAndGetObjectByTranIdOld(final long tranId,final String status, final String updateBy, final Handler<Obj> callback){
        //object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putString(colName.M2NumberCols.STATUS,status);
        newJsonObj.putNumber(colName.M2NumberCols.UPDATE_TIME,System.currentTimeMillis());
        newJsonObj.putString(colName.M2NumberCols.UPDATE_BY,updateBy);

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.M2NumberCols.TRAN_ID, tranId);
        query.putObject(MongoKeyWords.MATCHER, match);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);


        /*JsonObject newJson = new JsonObject();
        newJson.putNumber(colName.M2NumberCols.SMS_SENDED,1);
        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJson);
        query.putObject(MongoKeyWords.OBJ_NEW, set);*/


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json  = jsonObjectMessage.body();
                //
                if(json!= null && json.getObject(MongoKeyWords.RESULT, null) != null){
                    Obj result = new Obj(json.getObject(MongoKeyWords.RESULT));
                    callback.handle(result);
                }
                else{
                    callback.handle(null);
                }
            }
        });
    }

    public void getObjectByReceviedNumber(final int destination_number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);

        //matcher
        match.putNumber(colName.M2NumberCols.DESTINATION_PHONE, destination_number);
        match.putString(colName.M2NumberCols.STATUS
                , colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW));
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = null;
                if(event.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray arr  = event.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(arr != null && arr.size()>0){

                        arrayList = new ArrayList<>();
                        for(int i = 0;i<arr.size();i++){
                            arrayList.add( new Obj((JsonObject) arr.get(i)));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void updateByLink(final String link, final String status,final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putString(colName.M2NumberCols.STATUS,status);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);
        JsonObject match   = new JsonObject();

        match.putString(colName.M2NumberCols.SHORT_LINK, link);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //set
        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.INCREMENT, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);

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

    public void getWaitingObjs(final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.M2NumberCols.TABLE);

        //matcher
        match.putString(colName.M2NumberCols.STATUS
                , colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW));
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT,sort);

        //batch size
        query.putNumber(MongoKeyWords.BATCH_SIZE, 10000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = null;
                if(event.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray arr  = event.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    logger.info("So luong lenh M2Number dang cho xu ly " +  arr.size());
                    if(arr != null && arr.size()>0){

                        arrayList = new ArrayList<Obj>();
                        for(Object o : arr){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

}
