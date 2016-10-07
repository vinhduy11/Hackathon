package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 4/18/14.
 */
public class CDHHErr {
    public static class Error{
        public static String NotOpen = "notOpen";
        public static String Closed = "closed";
        public static String OverAmount = "overAmt";
        public static String NotEnoughCash = "notECash";    
        public static String InvalidServiceId = "ivalidsid";
        public static String InvalidCode = "ivalidcode";
    }
    public static class Desc{
        public static String NotOpen = "Chưa mở cửa";
        public static String Closed = "Đã đóng cửa";
        public static String OverAmount = "Quá 30 tin nhắn trong ngày";
        public static String NotEnoughCash = "Không đủ tiền";
        public static String InvalidServiceId = "Mã dịch vụ không hợp lệ";
        public static String InvalidCode = "Sai SBD bình chọn";

    }
    
    public static class Obj{
        public int number =0;               //icon loai giao dich vu co su dung voucher
        public long voteAmount =0;          //bat tat dich vu su dung vourcher
        public String time_vn =Misc.dateVNFormatWithTime(System.currentTimeMillis());         //
        public String code = "";
        public long time =System.currentTimeMillis();
        public String error ="";
        public String desc ="";

        public Obj(JsonObject jo){
            number = jo.getInteger(colName.CDHHErrCols.number);
            voteAmount =jo.getLong(colName.CDHHErrCols.voteAmount);
            time_vn =jo.getString(colName.CDHHErrCols.time_vn);
            time =jo.getLong(colName.CDHHErrCols.time);
            code = jo.getString(colName.CDHHErrCols.code);
            error = jo.getString(colName.CDHHErrCols.error,"");
            desc = jo.getString(colName.CDHHErrCols.desc,"");

        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putNumber(colName.CDHHErrCols.number,number);
            jo.putNumber(colName.CDHHErrCols.voteAmount,voteAmount);
            jo.putString(colName.CDHHErrCols.time_vn, time_vn);
            jo.putString(colName.CDHHErrCols.code, code);
            jo.putNumber(colName.CDHHErrCols.time,time);
            jo.putString(colName.CDHHErrCols.error, error);
            jo.putString(colName.CDHHErrCols.desc, desc);

            return jo;
        }

    }
    private EventBus eventBus;
    private Logger logger;

    public CDHHErr(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void save(Obj obj , final Handler<Boolean> callback){

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.CDHHErrCols.table)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                boolean result = false;
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if (createdId != null) {
                        result = true;
                    }
                }
                callback.handle(result);
            }
        });
    }

    public void findByNumber(int number, final Handler<Integer> callback){

        JsonObject query    = new JsonObject();
        JsonObject match   = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.CDHHCols.table);
        //matcher

        String curentDay = Misc.dateVNFormat(System.currentTimeMillis());

        match.putString(colName.CDHHCols.day_vn, curentDay);
        match.putNumber(colName.CDHHCols.number, number);
        query.putObject(MongoKeyWords.MATCHER,match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                int count = 0;

                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){

                    JsonArray results = jom.body().getArray(MongoKeyWords.RESULT_ARRAY);
                    if(results != null && results.size() > 0){
                        for(Object o : results){
                           count+= ((JsonObject)o).getInteger(colName.CDHHCols.voteAmount,0);
                        }
                    }
                }
                callback.handle(count);
            }
        });
    }

}
