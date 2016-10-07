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
public class ServiceForm {

    public static class Obj{
        public String id="";
        public int formnumber=1;
        public String name="";
        public String serviceid="";
        public String servicename="";
        public String caption="";
        public String textbutton="";
        public String desc="";
        public long lasttime=0;
        public String guide="";


        public Obj(JsonObject jo){
            id = jo.getString(colName.ServiceFormCols.id,"");
            formnumber = jo.getInteger(colName.ServiceFormCols.formnumber,1);
            name = jo.getString(colName.ServiceFormCols.name,"");
            serviceid = jo.getString(colName.ServiceFormCols.serviceid,"");
            servicename = jo.getString(colName.ServiceFormCols.servicename,"");
            caption = jo.getString(colName.ServiceFormCols.caption,"");
            textbutton = jo.getString(colName.ServiceFormCols.textbutton,"");
            desc = jo.getString(colName.ServiceFormCols.desc,"");
            lasttime = jo.getLong(colName.ServiceFormCols.lasttime,0);
            guide = jo.getString(colName.ServiceFormCols.guide,"");


        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.ServiceFormCols.id,id);
            jo.putNumber(colName.ServiceFormCols.formnumber,formnumber);
            jo.putString(colName.ServiceFormCols.name,name);
            jo.putString(colName.ServiceFormCols.serviceid,serviceid);
            jo.putString(colName.ServiceFormCols.caption, caption);
            jo.putString(colName.ServiceFormCols.textbutton, textbutton);
            jo.putString(colName.ServiceFormCols.desc, desc);
            jo.putNumber(colName.ServiceFormCols.lasttime, lasttime);
            jo.putString(colName.ServiceFormCols.guide, guide);
            return jo;
        }
    }
    private EventBus eventBus;
    private Logger logger;

    public ServiceForm(EventBus eb, Logger logger){
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
