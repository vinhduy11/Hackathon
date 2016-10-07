package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by concu on 4/18/14.
 */
public class FSCSetting {

    /*6.1 Thông báo mua vé thành công
    Headline: Mua vé FSC thành công

    Body msg:

            6.2 Thông báo mua vé chưa thành công

    Trường hơp 1:
    Headline: Ví không đủ tiền
    Body msg: Giao dịch chưa thành công do số dư ví MoMo không đủ. Vui lòng nạp tiền vào ví để thực hiện giao dịch. Hotline (08)39917199.
    Trường hợp 2:
    Headline: Mua quá 3 vé FSC2014
    Body msg: Giao dịch không thành công do bạn đã vượt hạn mức 03 vé FSC/01 ví MoMo. Nếu có nhu cầu, bạn có thể mua vé tại điểm bán vé hoặc dùng ví MoMo khác. Cảm ơn bạn.*/

    public static class Cap{
        public static String success ="Thông báo mua vé thành công";
        public static String notenoughcash ="Ví không đủ tiền";
        public static String overlimit ="Mua quá %s vé FSC2014";
        public static String overmaxcode ="Quá số lượng code quy định";
        public static String nosetting ="Chưa có setting của FSC 2014";
        public static String endedprogram ="Hết chương trình FSC 2014";
    }

    public static class Body{
        public static String success ="Tuyệt vời ông mặt trời! Chúc mừng bạn đã mua vé tham dự FSC 2014 thành công. Mã vé của bạn là FSC%s. BTC FSC 2014 sẽ liên hệ với bạn để nhận vé cứng. Chi tiết liên hệ: 0922292672.";
        public static String notenoughcash ="Giao dịch chưa thành công do số dư ví MoMo không đủ. Vui lòng nạp tiền vào ví để thực hiện giao dịch. Hotline (08)39917199.";
        public static String overlimit ="Giao dịch không thành công do bạn đã vượt hạn mức 0%s vé FSC/01 ví MoMo. Nếu có nhu cầu, bạn có thể mua vé tại điểm bán vé hoặc dùng ví MoMo khác. Cảm ơn bạn.";
        public static String overmaxcode ="Quá số lượng code quy định";
        public static String nosetting ="Chưa có setting của FSC 2014";
        public static String endedprogram ="Hết chương trình FSC 2014";
    }

    public static class Obj{
        public String id="";
        public int maxcode =0;
        public int totalcode =0;
        public int usedcode =0;
        public String recieveaccount ="";
        public int maxticket =0;
        public long fromdate =0;
        public long todate   =0;



        public Obj(JsonObject jo){
            id =jo.getString(colName.FSCSettingCols.id,"");
            maxcode = jo.getInteger(colName.FSCSettingCols.maxcode,0);
            totalcode =jo.getInteger(colName.FSCSettingCols.totalcode,0);
            usedcode =jo.getInteger(colName.FSCSettingCols.usedcode,0);
            recieveaccount =jo.getString(colName.FSCSettingCols.recieveaccount,"");
            maxticket =jo.getInteger(colName.FSCSettingCols.maxticket,0);
            fromdate =jo.getLong(colName.FSCSettingCols.fromdate,0);
            todate =jo.getLong(colName.FSCSettingCols.todate,0);
        }
        public Obj(){}
        public JsonObject toJson(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.FSCSettingCols.id,id);
            jo.putNumber(colName.FSCSettingCols.maxcode,maxcode);
            jo.putNumber(colName.FSCSettingCols.totalcode,totalcode);
            jo.putNumber(colName.FSCSettingCols.usedcode,usedcode);
            jo.putString(colName.FSCSettingCols.recieveaccount,recieveaccount);
            jo.putNumber(colName.FSCSettingCols.maxticket,maxticket);
            jo.putNumber(colName.FSCSettingCols.fromdate,fromdate);
            jo.putNumber(colName.FSCSettingCols.todate,todate);
            return jo;
        }
    }
    private EventBus eventBus;
    private Logger logger;

    public FSCSetting(EventBus eb, Logger logger){
        this.eventBus=eb;
        this.logger = logger;
    }

    public void saveOrUpdate(Obj obj,final Handler<Boolean> callback){

        if(obj.id != null &&  !"".equalsIgnoreCase(obj.id)){
            update(obj,callback);
            return;
        }

        JsonObject jo = obj.toJson();
        jo.removeField(colName.CDHHPayBackSettingCols.id);

        JsonObject query = new JsonObject()
                .putString(MongoKeyWords.ACTION, MongoKeyWords.SAVE)
                .putString(MongoKeyWords.COLLECTION, colName.FSCSettingCols.table)
                .putObject(MongoKeyWords.DOCUMENT, jo);

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

    public  void update(Obj ob, final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = ob.toJson();
        newJsonObj.removeField(colName.FSCSettingCols.id);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.FSCSettingCols.table);

        JsonObject match   = new JsonObject();
        match.putString(colName.FSCSettingCols.id, ob.id);
        query.putObject(MongoKeyWords.CRITERIA, match);

        //fields set
        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, newJsonObj);
        query.putObject(MongoKeyWords.OBJ_NEW, newJsonObj);

        //set
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

    public void findOne(final Handler<Obj> callback){

        JsonObject query = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.FSCSettingCols.table);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jom) {

                Obj obj = new Obj();
                if(jom.body().getString(MongoKeyWords.STATUS).equals("ok")){
                    JsonObject result = jom.body().getObject(MongoKeyWords.RESULT, null);
                    if(result != null){
                        obj = new Obj(result);
                    }
                }

                callback.handle(obj);
            }
        });
    }

    public void increase(String field, String id, long delta,final Handler<Boolean> callback){

        //new object
        JsonObject newJsonObj = new JsonObject();
        newJsonObj.putNumber(field,delta);

        JsonObject query    = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.FSCSettingCols.table);
        JsonObject match   = new JsonObject();

        match.putString(colName.FSCSettingCols.id, id);
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
}
