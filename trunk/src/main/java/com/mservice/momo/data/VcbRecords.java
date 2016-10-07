package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
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
public class VcbRecords {

    public static class Obj{

/*         public static String id="_id"; // --> phone number
        public static String bankcode="bnkcode";
        public static String time = "time";
        public static String timevn = "timevn";
        public static String table = "vcbtb";*/

        public int number =0;
        public String bankcode ="name";
        public long time =System.currentTimeMillis();
        public String timevn = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        public String vourcherid ="";
        public String partner = "";
        public String card_id ="";
        public long tranid = 0;
        public String desc = "";
        public int tranType = 0;

        public Obj(JsonObject jo){
            number = jo.getInteger(colName.VcbCols.number,0);
            bankcode = jo.getString(colName.VcbCols.bankcode,"");
            time = jo.getLong(colName.VcbCols.time,0);
            timevn = jo.getString(colName.VcbCols.timevn,"");
            vourcherid = jo.getString(colName.VcbCols.voucherid,"");
            partner = jo.getString(colName.VcbCols.partner,"");
            card_id = jo.getString(colName.VcbCols.card_id,"");
            tranid = jo.getLong(colName.VcbCols.tranid,0);
            desc = jo.getString(colName.VcbCols.desc,"");

        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.VcbCols.number,number);
            jo.putString(colName.VcbCols.bankcode,bankcode);
            jo.putNumber(colName.VcbCols.time, time);
            jo.putString(colName.VcbCols.timevn, timevn);
            jo.putString(colName.VcbCols.voucherid,vourcherid);
            jo.putString(colName.VcbCols.partner,partner);
            jo.putString(colName.VcbCols.card_id,card_id);
            jo.putNumber(colName.VcbCols.tranid,tranid);
            if(tranType == 0){
                jo.putString(colName.VcbCols.desc,"addField");
            }else{
                jo.putString(colName.VcbCols.desc, MomoProto.TranHisV1.TranType.valueOf(tranType).name());
            }
            return jo;
        }
    }
    private EventBus eventBus;
    private Logger logger;

    public VcbRecords(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void save(final Obj obj, final Handler<Obj> callback) {

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.VcbCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if (createdId != null) {
                        callback.handle(obj);
                        return;
                    }
                }
                callback.handle(null);
            }
        });
    }

    public void countBy(int number, String partnerNumber, final Handler<Integer> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        matcher.putNumber(colName.VcbCols.number,number);
        matcher.putString(colName.VcbCols.partner,partnerNumber);

        query.putObject(MongoKeyWords.MATCHER,matcher);

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {
                int count = 0;
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonObject  jo = jom.body().getObject(MongoKeyWords.RESULT, null);
                    count = (jo != null ? 1 : 0);
                }
                callback.handle(count);
            }
        });
    }

    public void countVCBCreatedBy(int phoneNumber, final Handler<Integer> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();
        matcher.putNumber(colName.VcbCols.number,phoneNumber);
        query.putObject(MongoKeyWords.MATCHER,matcher);

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                int count = 0;
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray jo = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    count = (jo !=null ? jo.size():0);
                }
                callback.handle(count);
            }
        });
    }

    public void countVCBByCardIdOrPhone(int phoneNumber, String cardId, final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        JsonObject joCard = new JsonObject();
        joCard.putString(colName.VcbCols.card_id,cardId);

        JsonObject phone = new JsonObject();
        phone.putNumber(colName.VcbCols.number, phoneNumber);

        JsonArray orAr = new JsonArray();
        orAr.add(joCard).add(phone);

        matcher.putArray("$or",orAr);
        query.putObject(MongoKeyWords.MATCHER,matcher);

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCols.table);

        query.putNumber(MongoKeyWords.BATCH_SIZE,1000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray joArr = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);

                    joArr = joArr != null ? joArr : new JsonArray();

                    for(int i=0;i< joArr.size(); i++){
                        Obj o = new Obj((JsonObject)joArr.get(i));
                        arrayList.add(o);
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

    public void getVCBByCardIdOrPhone(int phoneNumber, String cardId, final Handler<JsonArray> callback){

        JsonObject query = new JsonObject();
        JsonObject matcher = new JsonObject();

        JsonObject joCard = new JsonObject();
        joCard.putString(colName.VcbCols.card_id,cardId);

        JsonObject phone = new JsonObject();
        phone.putNumber(colName.VcbCols.number, phoneNumber);

        JsonArray orAr = new JsonArray();
        orAr.add(joCard).add(phone);

        matcher.putArray("$or",orAr);
        query.putObject(MongoKeyWords.MATCHER,matcher);

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.VcbCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                JsonArray jo = new JsonArray();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    jo = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray());
                }
                callback.handle(jo);
            }
        });
    }

//    public void getVCBAccountByTime(long beginTime, long endTime, final Handler<JsonArray> callback){
//
//         query = new BasicDBObject("j", new BasicDBObject("$ne", 3))
//                .append("k", new BasicDBObject("$gt", 10));
//
//        cursor = coll.find(query);
//
//        try {
//            while(cursor.hasNext()) {
//                System.out.println(cursor.next());
//            }
//        } finally {
//            cursor.close();
//        }
//
//        JsonObject query = new JsonObject();
//        JsonObject matcher = new JsonObject();
//
//        JsonObject joBegin = new JsonObject();
//        joBegin.putNumber(colName.VcbCols.time, beginTime);
//
//        JsonObject joEnd = new JsonObject();
//        joEnd.putNumber(colName.VcbCols.time, endTime);
//
////        JsonArray orAr = new JsonArray();
////        orAr.add(joCard).add(phone);
//
////        matcher.putArray("$or",orAr);
//        matcher.putObject(MongoKeyWords.GREATER_OR_EQUAL, joBegin);
//        matcher.putObject(MongoKeyWords.LESS_OR_EQUAL, joEnd);
//        query.putObject(MongoKeyWords.MATCHER,matcher);
//
//        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
//        query.putString(MongoKeyWords.COLLECTION, colName.VcbCols.table);
//
//        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonArray>>() {
//            @Override
//            public void handle(Message<JsonArray> jom) {
//
//                if(jom != null && jom.body() != null && jom.body().size() > 0)
//                {
//                    JsonArray jo = jom.body();
//                    callback.handle(jo);
//                }
//                else
//                {
//                    callback.handle(null);
//                }
//                //                if(jom.body().(MongoKeyWords.STATUS).equals("ok")){
////                    jo = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray());
////                }
//
//            }
//        });
//    }

}
