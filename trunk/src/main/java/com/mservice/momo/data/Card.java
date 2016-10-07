package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.visa.entity.VisaResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 4/18/14.
 */
public class Card {
    private EventBus eventBus;
    private Logger logger;
    public Card(EventBus eb,Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public static JsonObject buildCardJsonObj(MomoProto.CardItem bn2MM) {
        if (bn2MM == null) return null;

        JsonObject o = new JsonObject();

        o.putString(colName.CardDBCols.CARD_HOLDER_NAME
                , bn2MM.getCardHolderName() == null ? "" : bn2MM.getCardHolderName());

        o.putString(colName.CardDBCols.CARD_HOLDER_NUMBER
                , bn2MM.getCardHolderNumber() == null ? "" : bn2MM.getCardHolderNumber());

        o.putString(colName.CardDBCols.CARD_HOLDER_YEAR
                , bn2MM.getCardHolderYear() == null ? "" : bn2MM.getCardHolderYear());

        o.putString(colName.CardDBCols.CARD_HOLDER_MONTH
                , bn2MM.getCardHolderMonth() == null ? "" : bn2MM.getCardHolderMonth());

        o.putString(colName.CardDBCols.BANK_NAME
                , bn2MM.getBankName() == null ? "" : bn2MM.getBankName());

        o.putString(colName.CardDBCols.BANKID
                , bn2MM.getBankId() == null ? "" : bn2MM.getBankId());

        o.putNumber(colName.CardDBCols.BANK_TYPE, bn2MM.getBankType());

        o.putBoolean(colName.CardDBCols.DELETED, bn2MM.getStatus() == 1 ? false : true);

        o.putNumber(colName.CardDBCols.ROW_ID, bn2MM.getRowId());
        o.putNumber(colName.CardDBCols.LAST_SYNC_TIME, bn2MM.getLastSyncTime());
        o.putNumber(colName.CardDBCols.STATUS, bn2MM.getStatus());
        o.putString(colName.CardDBCols.CARD_TYPE, bn2MM.getCardType() == null ? "" : bn2MM.getCardType());
        o.putString(colName.CardDBCols.CARD_ID, bn2MM.getCardId() == null ? "" : bn2MM.getCardId());

        return o;
    }

    public void findAll(final int number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        match.putBoolean(colName.CardDBCols.DELETED, false);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void findAllCardVisa(final int number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
//        match.putBoolean(colName.CardDBCols.DELETED, false);
        match.putString(colName.CardDBCols.BANKID, StringConstUtil.PHONES_BANKID_SBS);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void findAllActivedCardVisa(final int number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        match.putBoolean(colName.CardDBCols.DELETED, false);
        match.putString(colName.CardDBCols.BANKID, StringConstUtil.PHONES_BANKID_SBS);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CardDBCols.LAST_SYNC_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void findAllActivedATMCard(final int number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        match.putBoolean(colName.CardDBCols.DELETED, false);
        match.putNumber(colName.CardDBCols.BANK_TYPE, 10);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CardDBCols.LAST_SYNC_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void findActivedCardVisaWithCardCheckSum(final String cardCheckSum, final int number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        match.putBoolean(colName.CardDBCols.DELETED, false);
        match.putString(colName.CardDBCols.BANKID, StringConstUtil.PHONES_BANKID_SBS);
        match.putString(colName.CardDBCols.CARD_CHECKSUM, cardCheckSum);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CardDBCols.LAST_SYNC_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void findActivedCardVisaWithBankAcc(final String bankAcc, final int number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        match.putBoolean(colName.CardDBCols.DELETED, false);
        match.putString(colName.CardDBCols.BANKID, StringConstUtil.PHONES_BANKID_SBS);
        match.putString(colName.CardDBCols.CARD_HOLDER_NUMBER, bankAcc);
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CardDBCols.LAST_SYNC_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void findAllCardWithDeletedCard(final int number, final Handler<ArrayList<Obj>> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        query.putObject(MongoKeyWords.MATCHER,match);

        //no sort
        JsonObject sort = new JsonObject("{}");
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                            arrayList.add(new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void addOrUpdate(final int number
                            ,final ArrayList<JsonObject> arr
                            ,final Handler<ArrayList<MomoProto.CardItem>> callback){

        final ArrayList<MomoProto.CardItem> list = new ArrayList<>();

        for(int i=0;i<arr.size();i++){
            final int pos = i;

            getMaxLastTime(number,new Handler<Integer>() {
                @Override
                public void handle(Integer rowNum) {

                    int curNum = ( arr.get (pos).getInteger(colName.CardDBCols.LAST_SYNC_TIME,0) == 0
                            ? rowNum : arr.get (pos).getInteger(colName.CardDBCols.LAST_SYNC_TIME));

                    //reset to upsert data
                    arr.get (pos).putNumber(colName.CardDBCols.LAST_SYNC_TIME,curNum);

                    upsertOne(number,arr.get(pos),new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jo) {

                            if(jo != null){
                                list.add(buildCardJsonObj(jo));
                            }else{
                                list.add(buildCardJsonObj(arr.get(pos)));
                            }

                            if(pos == (arr.size() - 1)){
                                callback.handle(list);
                            }
                        }
                    });
                }
            });
        }
    }

    public void deleteVMCard(final int number, final VisaResponse visaResponse
            , final Handler<JsonObject> callback) {

        String cardId = visaResponse.getVisaRequest().getCardId();

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.CardDBCols.CARD_ID, cardId);

        query.putObject(MongoKeyWords.CRITERIA, match);

        //Field delete
        JsonObject delField = new JsonObject();

        delField.putBoolean(colName.CardDBCols.DELETED, true);
        delField.putNumber(colName.CardDBCols.LAST_SYNC_TIME, System.currentTimeMillis());

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, delField);

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if (event.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    callback.handle(event.body());
                } else {
                    callback.handle(null);
                }
            }
        });

    }

    public void deleteBanklinkCard(final int number, final String bankCode
            , final Handler<JsonObject> callback) {
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.CardDBCols.BANKID, bankCode);
        match.putNumber(colName.CardDBCols.BANK_TYPE, 10);

        query.putObject(MongoKeyWords.CRITERIA, match);

        //Field delete
        JsonObject delField = new JsonObject();

        delField.putBoolean(colName.CardDBCols.DELETED, true);
        delField.putNumber(colName.CardDBCols.LAST_SYNC_TIME, System.currentTimeMillis());

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, delField);

        query.putObject(MongoKeyWords.OBJ_NEW, set);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.MULTI, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if (event.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    callback.handle(event.body());
                } else {
                    callback.handle(null);
                }
            }
        });

    }

    public void deleteAllVMCard(final String number
            , final Handler<Boolean> callback) {

        logger.info("deleteAllVMCard");
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.CardDBCols.BANKID, "sbs");

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body().getString(MongoKeyWords.STATUS).equals("ok")) {
                    callback.handle(true);
                    logger.info("deleteAllVMCard -> true");
                } else {
                    callback.handle(false);
                    logger.info("deleteAllVMCard -> false");
                }
            }
        });

    }

    private void getMaxLastTime(final int number,final Handler<Integer> callback){
        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //sort
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.CardDBCols.LAST_SYNC_TIME, -1);
        query.putObject(MongoKeyWords.SORT, sort);
        query.putNumber(MongoKeyWords.LIMIT,1);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int nextNum = 1;
                JsonArray array = event.body().getArray(MongoKeyWords.RESULT_ARRAY,null);
                if(array !=null && array.size() >0){
                    nextNum = ((JsonObject) array.get(0)).getInteger(colName.CardDBCols.LAST_SYNC_TIME,0) + 1;
                }
                callback.handle(nextNum);
            }
        });
    }

    public void insert(final long number, final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
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

    public void upsertOne(final int number, final JsonObject objNew
                                            ,final Handler<JsonObject> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.CardDBCols.LAST_SYNC_TIME
                , objNew.getInteger(colName.CardDBCols.LAST_SYNC_TIME, 0));

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, objNew);

        query.putObject(MongoKeyWords.OBJ_NEW, set);

        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                if(event.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    callback.handle(objNew);
                }else {
                    callback.handle(null);
                }
            }
        });
    }


    public void upsertVisaMaster(final int number, final JsonObject objNew
            ,final Handler<Boolean> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.CardDBCols.BANKID, "sbs");
        match.putString(colName.CardDBCols.CARD_TYPE,objNew.getString(colName.CardDBCols.CARD_TYPE,""));
        match.putString(colName.CardDBCols.CARD_HOLDER_NUMBER, objNew.getString(colName.CardDBCols.CARD_HOLDER_NUMBER, ""));

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, objNew);

        query.putObject(MongoKeyWords.OBJ_NEW, set);

        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                boolean result = event.body().getString(MongoKeyWords.STATUS).equals("ok");
                callback.handle(result);
            }
        });
    }

    public void upsertATMCard(final int number, final JsonObject joUpdate
            ,final Handler<Boolean> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.CardDBCols.BANKID, "atm");
        match.putString(colName.CardDBCols.CARD_TYPE, joUpdate.getString(colName.CardDBCols.CARD_TYPE, ""));
        match.putString(colName.CardDBCols.CARD_CHECKSUM, joUpdate.getString(colName.CardDBCols.CARD_CHECKSUM, ""));

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, set);

        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                boolean result = event.body().getString(MongoKeyWords.STATUS).equals("ok");
                callback.handle(result);
            }
        });
    }

    public void deleteATMCard(final int number, String cardId, JsonObject joUpdate
            ,final Handler<Boolean> callback){
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.CardDBCols.TABLE_PREFIX + number);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.CardDBCols.CARD_ID, cardId);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, set);

        query.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                boolean result = event.body().getString(MongoKeyWords.STATUS).equals("ok");
                callback.handle(result);
            }
        });
    }

    private  MomoProto.CardItem buildCardJsonObj(JsonObject o){

        MomoProto.CardItem.Builder builder = MomoProto.CardItem.newBuilder();
        builder.setCardHolderName(o.getString(colName.CardDBCols.CARD_HOLDER_NAME,""));
        builder.setCardHolderNumber(o.getString(colName.CardDBCols.CARD_HOLDER_NUMBER, ""));
        builder.setCardHolderYear(o.getString(colName.CardDBCols.CARD_HOLDER_YEAR, ""));
        builder.setCardHolderMonth(o.getString(colName.CardDBCols.CARD_HOLDER_MONTH, ""));
        builder.setBankName(o.getString(colName.CardDBCols.BANK_NAME, ""));
        builder.setBankId(o.getString(colName.CardDBCols.BANKID, ""));
        builder.setBankType(o.getInteger(colName.CardDBCols.BANK_TYPE, 0));
        builder.setStatus(o.getInteger(colName.CardDBCols.STATUS));
        builder.setLastSyncTime(o.getLong(colName.CardDBCols.LAST_SYNC_TIME, 0));
        builder.setRowId(o.getInteger(colName.CardDBCols.ROW_ID, 0));
        builder.setCardType(o.getString(colName.CardDBCols.CARD_TYPE, ""));
        builder.setCardId(o.getString(colName.CardDBCols.CARD_ID, ""));
        return builder.build();
    }

    public static class Obj {
        public String card_holder_year = "";
        public String card_holder_month = "";
        public String card_holder_number = "";
        public String card_holder_name = "";
        public String bankid = "";
        public int bank_type = 0;
        public String bank_name = "";
        public boolean deleted = false;
        public int row_id = 0;
        public long last_sync_time = 0;
        public int status = 0;
        public String cardType = "";
        public String cardId = "";
        public String cardCheckSum = "";

        public Obj(){}
        public Obj(JsonObject jo) {
            card_holder_year = jo.getString(colName.CardDBCols.CARD_HOLDER_YEAR, "");
            card_holder_month = jo.getString(colName.CardDBCols.CARD_HOLDER_MONTH, "");
            card_holder_number = jo.getString(colName.CardDBCols.CARD_HOLDER_NUMBER, "");
            card_holder_name = jo.getString(colName.CardDBCols.CARD_HOLDER_NAME, "");
            bankid = jo.getString(colName.CardDBCols.BANKID, "");
            deleted = jo.getBoolean(colName.CardDBCols.DELETED, false);
            bank_type = jo.getInteger(colName.CardDBCols.BANK_TYPE, 0);
            bank_name = jo.getString(colName.CardDBCols.BANK_NAME, "");
            row_id = jo.getInteger(colName.CardDBCols.ROW_ID, 0);
            last_sync_time = jo.getLong(colName.CardDBCols.LAST_SYNC_TIME, 0);
            status = jo.getInteger(colName.CardDBCols.STATUS
                    , jo.getBoolean(colName.CardDBCols.DELETED, false) == true ? 2 : 1);
            cardType = jo.getString(colName.CardDBCols.CARD_TYPE, "");
            cardId = jo.getString(colName.CardDBCols.CARD_ID, "");
            cardCheckSum = jo.getString(colName.CardDBCols.CARD_CHECKSUM, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
              jo.putString(colName.CardDBCols.CARD_HOLDER_YEAR, card_holder_year);
              jo.putString(colName.CardDBCols.CARD_HOLDER_MONTH, card_holder_month);
              jo.putString(colName.CardDBCols.CARD_HOLDER_NUMBER, card_holder_number);
              jo.putString(colName.CardDBCols.CARD_HOLDER_NAME, card_holder_name);
              jo.putString(colName.CardDBCols.BANKID, bankid);
              jo.putBoolean(colName.CardDBCols.DELETED, deleted);
              jo.putNumber(colName.CardDBCols.BANK_TYPE, bank_type);
              jo.putString(colName.CardDBCols.BANK_NAME, bank_name);
              jo.putNumber(colName.CardDBCols.ROW_ID, row_id);
              jo.putNumber(colName.CardDBCols.LAST_SYNC_TIME, last_sync_time);
              jo.putNumber(colName.CardDBCols.STATUS, status);
              jo.putString(colName.CardDBCols.CARD_TYPE, cardType);
              jo.putString(colName.CardDBCols.CARD_ID, cardId);
              jo.putString(colName.CardDBCols.CARD_CHECKSUM, cardCheckSum);
            return jo;

        }
    }

}
