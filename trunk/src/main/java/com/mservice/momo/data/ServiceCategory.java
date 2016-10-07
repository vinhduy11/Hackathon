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
 * Created by concu on 4/18/14.
 */
public class ServiceCategory {

    public static class Obj{
        public String id ="_id";
        public String name ="name";
        public String desc ="desc";
        public String status ="stat"; // on/off
        public long lasttime = 0;
        public int order = 10000;
        public String iconurl= "";
        public int star = 0;


        public Obj(JsonObject jo){
            id =jo.getString(colName.ServiceCatCols.id,"");
            name =jo.getString(colName.ServiceCatCols.name,"");
            desc =jo.getString(colName.ServiceCatCols.desc,"");
            status =jo.getString(colName.ServiceCatCols.status,""); // on/off
            lasttime = jo.getLong(colName.ServiceCatCols.lasttime, 0);
            order = jo.getInteger(colName.ServiceCatCols.order, 10000);
            iconurl =jo.getString(colName.ServiceCatCols.iconurl,""); // on/off
            star =jo.getInteger(colName.ServiceCatCols.star,0); // on/off

        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.ServiceCatCols.id,id);
            jo.putString(colName.ServiceCatCols.name,name);
            jo.putString(colName.ServiceCatCols.desc,desc);
            jo.putString(colName.ServiceCatCols.status,status);
            jo.putNumber(colName.ServiceCatCols.lasttime, lasttime);
            jo.putNumber(colName.ServiceCatCols.order, order);
            jo.putString(colName.ServiceCatCols.iconurl,iconurl);
            jo.putNumber(colName.ServiceCatCols.star,star);

            return jo;
        }
    }
    private EventBus eventBus;
    private Logger logger;

    public ServiceCategory(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public  void upsert(Obj ob, final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = ob.toJson();
        newJsonObj.removeField(colName.ServiceCatCols.id);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCatCols.table);

        JsonObject match   = new JsonObject();
        match.putString(colName.ServiceCatCols.id, ob.id);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //fields set
        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, newJsonObj);

        //set
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

    public void getAll(final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCatCols.table);

        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServiceCatCols.order, 1);

        query.putObject(MongoKeyWords.SORT,sort);

        query.putNumber(MongoKeyWords.BATCH_SIZE,10000);
        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                ArrayList<Obj> arrayList = new ArrayList<>();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonArray array = jom.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                    if(array !=null && array.size() >0){
                        for (Object o : array){
                            arrayList.add( new Obj((JsonObject)o));
                        }
                    }
                }
                callback.handle(arrayList);
            }
        });
    }

}
