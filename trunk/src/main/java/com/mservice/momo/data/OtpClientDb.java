package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 2/18/14
 * Time: 6:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class OtpClientDb {


    public static final class Obj{
        public String id="";
        public int retailer =0;
        public int customer_number = 0;
        public String opt = "";
        public String timevn = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        public long time = System.currentTimeMillis();

        public Obj(JsonObject json){
            id = json.getString("_id","");
            retailer = json.getInteger(colName.RetailerOtpClient.retailer, 0);
            customer_number = json.getInteger(colName.RetailerOtpClient.customer_number, 0);
            opt = json.getString(colName.RetailerOtpClient.opt, "");
            timevn =json.getString(colName.RetailerOtpClient.timevn,"");
            time =json.getLong(colName.RetailerOtpClient.time, 0);
        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.RetailerOtpClient.retailer,retailer);
            jo.putNumber(colName.RetailerOtpClient.customer_number,customer_number);
            jo.putString(colName.RetailerOtpClient.opt,opt);
            jo.putString(colName.RetailerOtpClient.timevn,timevn);
            jo.putNumber(colName.RetailerOtpClient.time,time);
            return jo;
        }
    }

    private EventBus eventBus;
    private Logger logger;
    public OtpClientDb(EventBus eb, Logger logger){
        this.eventBus = eb;
        this.logger = logger;
    }

    public void findOne(int retailerNumber, int customNumber , final Handler<Obj> callback){
        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION,MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.RetailerOtpClient.table);
        JsonObject match   = new JsonObject();

        match.putNumber(colName.RetailerOtpClient.retailer, retailerNumber);
        match.putNumber(colName.RetailerOtpClient.customer_number, customNumber);

        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json = jsonObjectMessage.body();
                JsonObject result = json.getObject(MongoKeyWords.RESULT, null);
                if(result!= null){
                    callback.handle(new Obj(result));
                    return;
                }

                callback.handle(null);
            }
        });
    }

    public void upsert(final Obj obj, final Handler<Boolean> callback){

        //object
        JsonObject joUpsert = obj.toJson();

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.RetailerOtpClient.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putNumber(colName.RetailerOtpClient.retailer, obj.retailer);
        match.putNumber(colName.RetailerOtpClient.customer_number, obj.customer_number);

        query.putObject(MongoKeyWords.MATCHER, match);
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, joUpsert);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject json  = jsonObjectMessage.body();
                boolean result = (json != null && json.getObject(MongoKeyWords.RESULT,null) != null)? true : false;
                callback.handle(result);
            }
        });
    }

    public void remove(String id, final Handler<Boolean> callback){

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.RetailerOtpClient.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.RetailerOtpClient.id, id);

        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void updatePartial(String id, JsonObject joUp, final Handler<Boolean> callback){


        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.RetailerOtpClient.table);

        //matcher
        JsonObject match = new JsonObject();
        match.putString(colName.RetailerOtpClient.id, id);

        query.putObject(MongoKeyWords.MATCHER, match);

        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));
        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, joUp);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);
        query.putBoolean(MongoKeyWords.NEW, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {

                if(callback == null) return;
                JsonObject json  = jsonObjectMessage.body();
                boolean result = (json!= null && json.getObject(MongoKeyWords.RESULT, null) != null) ? true : false;
                callback.handle(result);
            }
        });
    }
}
