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
 * Created by concu on 7/14/14.
 */
public class PromotionDb {
    private Logger logger;
    private EventBus eventBus;
    public PromotionDb(EventBus eventBus, Logger logger){
        this.logger =logger;
        this.eventBus =eventBus;
    }

    public void getPromotions(final Handler<ArrayList<Obj>> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoCols.TABLE);

        query.putNumber(MongoKeyWords.BATCH_SIZE,100);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if(results != null && results.size() > 0){
                    arrayList = new ArrayList<>();
                    for(Object o : results){
                        arrayList.add( new Obj((JsonObject)o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getPromotionById(final String Id, final Handler<Obj> callback){

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.PromoCols.TABLE);

        //matcher
        JsonObject matcher = new JsonObject();
        matcher.putString(colName.PromoCols.ID, Id);

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

    /**
     * Cap nhat 1 chuong trinh khuyen mai ban be
     * @param id
     * @param obj
     * @param callback
     */
    public void upsertPromo(String id, Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.PromoCols.TABLE);

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.PromoCols.ID);

        //khong cap nhat ngay tao
        data.removeField(colName.PromoCols.CREATE_TIME);

        JsonObject criteria = new JsonObject();

        if(id != null &&  !"".equalsIgnoreCase(id)){
            criteria.putString(colName.PromoCols.ID,id);
        }else {
            obj.CREATE_TIME = System.currentTimeMillis(); // lay ngay hien tai
            insertDoc(obj.toJsonObject(),new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    callback.handle(aBoolean);
                }
            });
            return;
        }

        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, data);
        query.putObject(MongoKeyWords.OBJ_NEW, update);

        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                if (obj!=null && obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });

    }

    public void insertDoc(JsonObject json, final Handler<Boolean> callback)
    {
        json.removeField(colName.PromoCols.ID);
        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection", colName.PromoCols.TABLE )
                .putObject("document", json);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if(createdId !=null) {
                        callback.handle(true);
                        return;
                    }
                }

                callback.handle(false);
            }
        });
    }
    
    public static class Obj{
        //const area
        public static final String PROMOTION_NAME = "PROMOTION_NAME";
        //end const area
        public String ID ="_id";
        public String NAME =""; // ten khuyen mai
        public String DESCRIPTION=""; // mo ta
        public long DATE_FROM =0; // khuyen mai tu ngay
        public long DATE_TO =0; // khuyen mai den ngay
        public long TRAN_MIN_VALUE=0; // giao tri toi thieu cua 1 giao dich de nhan khuyen mai : 20K
        public int PER_TRAN_VALUE =0; // gia tri khuyen mai duoc cong : 10k
        // TODO: 01/07/2016 Add new field to handle promotion process action
        public int TRAN_TYPE = 0;

        //thoi gian keo dai khuyen mai tu luc dang ky vi
        // 0: thuc hien khuyen mai the FromDate to ToDate
        //>1: thuc hien khuyen mai theo ngay tao moi vi
        public int DURATION =0;

        public long MAX_VALUE =0; // tong gia tri khuyen mai toi da 100K
        public String INTRO_DATA =""; // noi dung gio thieu chung trinh khuyen mai bang data
        public String INTRO_SMS =""; // noi dung ban sms gioi thieu chuong trinh khuyen mai

        // chuong trinh dang duoc kich hoat hay khong
        // false : khong ; true: kich hoat
        public boolean ACTIVE =false;
        // tinh theo % gia tri giao dich hoac gia tri co dinh
        // val --> gia tri khuyen mai = PROMOTION_VALUE; per gia tri khuyen mai = (PROMOTION_VALUE * Gia tri giao dich)/100
        public String TYPE = "protype";

        //so lan khuyen mai toi da, 10 lan
        public int MAX_TIMES = 0;
        public int MIN_TIMES= 0;

        /*"notiboiter" : "Quý khách đã nhận được %sđ tiền giới thiệu bạn bè. Người được giới thiệu: %s (%s)",
                "notismsiter" : "Quy khach da nhan duoc %sd tien gioi thieu ban be. Nguoi duoc gioi thieu: %s (%s)",
                "notiboitee" : "Quý khách đã nhận được %sđ tiền giới thiệu bạn bè. Người giới thiệu: %s (%s)",
                "notismsitee" : "Quy khach da nhan duoc %sd tien gioi thieu ban be. Nguoi gioi thieu: %s (%s)",*/

        //for noti
        public String NOTI_CAPTION ="";
        public String NOTI_BODY_INVITER = "";
        public String NOTI_SMS_INVITER = "";
        public String NOTI_BODY_INVITEE = "";
        public String NOTI_SMS_INVITEE = "";

        public String NOTI_COMMENT=""; // comment cho comment cua notification
        //tai khoan dung de chuyen tien khuyen mai cho khach hang
        public String ADJUST_ACCOUNT = "";
        public long CREATE_TIME = 0;

        public String ADJUST_PIN = "";
        public int DURATION_TRAN = 0;

        public String OFF_TIME_FROM="";
        public String OFF_TIME_TO="";
        public boolean STATUS = false;
        public boolean STATUS_IOS = false;
        public boolean STATUS_ANDROID = false;
        public boolean ENABLE_PHASE2=false;

        public String ADDRESSNAME = "";

        public boolean ISVERTICLE=false;

        public JsonObject EXTRA = new JsonObject();

        public Obj(){}
        public Obj(JsonObject jo){

            ID =jo.getString(colName.PromoCols.ID,"");
            NAME =jo.getString(colName.PromoCols.NAME,""); // ten khuyen mai
            DESCRIPTION=jo.getString(colName.PromoCols.DESCRIPTION); // mo ta
            DATE_FROM =jo.getLong(colName.PromoCols.DATE_FROM,0); // khuyen mai tu ngay
            DATE_TO =jo.getLong(colName.PromoCols.DATE_TO,0); // khuyen mai den ngay
            TRAN_MIN_VALUE=jo.getInteger(colName.PromoCols.TRAN_MIN_VALUE,0); // giao tri toi thieu cua 1 giao dich de nhan khuyen mai : 20K
            PER_TRAN_VALUE =jo.getInteger(colName.PromoCols.PER_TRAN_VALUE,0); // gia tri khuyen mai duoc cong : 10k
            // TODO: 01/07/2016 add new field to handle promotion process action
            TRAN_TYPE = jo.getInteger(colName.PromoCols.TRAN_TYPE, 0);

            //thoi gian keo dai khuyen mai tu luc dang ky vi
            // 0: thuc hien khuyen mai the FromDate to ToDate
            //>1: thuc hien khuyen mai theo ngay tao moi vi
            DURATION =jo.getInteger(colName.PromoCols.DURATION,0);

            MAX_VALUE = jo.getLong(colName.PromoCols.MAX_VALUE,0); // tong gia tri khuyen mai toi da 100K
            INTRO_DATA = jo.getString(colName.PromoCols.INTRO_DATA,""); // noi dung gio thieu chung trinh khuyen mai bang data
            INTRO_SMS =jo.getString(colName.PromoCols.INTRO_SMS,""); // noi dung ban sms gioi thieu chuong trinh khuyen mai

            // chuong trinh dang duoc kich hoat hay khong
            // false : khong ; true: kich hoat

            //active duoc tinh theo ngay hien hanh so voi ngay khuyen mai cuoi cung
            ACTIVE = true;
            if(System.currentTimeMillis() > DATE_TO){
                ACTIVE =jo.getBoolean(colName.PromoCols.ACTIVE,false);
            }

            // tinh theo % gia tri giao dich hoac gia tri co dinh
            // val --> gia tri khuyen mai = PROMOTION_VALUE; per gia tri khuyen mai = (PROMOTION_VALUE * Gia tri giao dich)/100
            TYPE = jo.getString(colName.PromoCols.TYPE,"");
            //so lan khuyen mai toi da, 10 lan
            MAX_TIMES = jo.getInteger(colName.PromoCols.MAX_TIMES,0);
            MIN_TIMES = jo.getInteger(colName.PromoCols.MIN_TIMES,0);

            //for noti
            NOTI_CAPTION =jo.getString(colName.PromoCols.NOTI_CAPTION,"");
            NOTI_BODY_INVITER = jo.getString(colName.PromoCols.NOTI_BODY_INVITER,"");
            NOTI_SMS_INVITER = jo.getString(colName.PromoCols.NOTI_SMS_INVITER,"");

            NOTI_BODY_INVITEE = jo.getString(colName.PromoCols.NOTI_BODY_INVITEE,"");
            NOTI_SMS_INVITEE = jo.getString(colName.PromoCols.NOTI_SMS_INVITEE,"");

            NOTI_COMMENT=jo.getString(colName.PromoCols.NOTI_COMMENT,"");
            //tai khoan dung de chuyen tien khuyen mai cho khach hang
            ADJUST_ACCOUNT = jo.getString(colName.PromoCols.ADJUST_ACCOUNT,"");

            CREATE_TIME = jo.getLong(colName.PromoCols.CREATE_TIME,0);

            ADJUST_PIN = jo.getString(colName.PromoCols.ADJUST_PIN,"");
            DURATION_TRAN = jo.getInteger(colName.PromoCols.DURATION_TRAN,-1);

            OFF_TIME_FROM = jo.getString(colName.PromoCols.OFF_TIME_FROM,"");
            OFF_TIME_TO = jo.getString(colName.PromoCols.OFF_TIME_TO,"");
            STATUS = jo.getBoolean(colName.PromoCols.STATUS,false);
            STATUS_IOS = jo.getBoolean(colName.PromoCols.STATUS_IOS,true);
            STATUS_ANDROID = jo.getBoolean(colName.PromoCols.STATUS_ANDROID,true);
            ENABLE_PHASE2 = jo.getBoolean(colName.PromoCols.ENABLE_PHASE2,false);
            ADDRESSNAME = jo.getString(colName.PromoCols.ADDRESSNAME,"promotionVerticle");
            ISVERTICLE = jo.getBoolean(colName.PromoCols.ISVERTICLE,false);
            EXTRA = jo.getObject(colName.PromoCols.EXTRA, new JsonObject());
        }

        public JsonObject toJsonObject(){
            JsonObject jo = new JsonObject();
            jo.putString(colName.PromoCols.ID,ID);
            jo.putString(colName.PromoCols.NAME,NAME); // ten khuyen mai
            jo.putString(colName.PromoCols.DESCRIPTION,DESCRIPTION); // mo ta
            jo.putNumber(colName.PromoCols.DATE_FROM,DATE_FROM); // khuyen mai tu ngay
            jo.putNumber(colName.PromoCols.DATE_TO,DATE_TO); // khuyen mai den ngay
            jo.putNumber(colName.PromoCols.TRAN_MIN_VALUE,TRAN_MIN_VALUE); // giao tri toi thieu cua 1 giao dich de nhan khuyen mai : 20K
            jo.putNumber(colName.PromoCols.PER_TRAN_VALUE,PER_TRAN_VALUE); // gia tri khuyen mai duoc cong : 10k

            // TODO: 01/07/2016 add new field to handle promotion process action
            jo.putNumber(colName.PromoCols.TRAN_TYPE, TRAN_TYPE);

            jo.putNumber(colName.PromoCols.DURATION,DURATION);

            jo.putNumber(colName.PromoCols.MAX_VALUE,MAX_VALUE); // tong gia tri khuyen mai toi da 100K
            jo.putString(colName.PromoCols.INTRO_DATA,INTRO_DATA); // noi dung gio thieu chung trinh khuyen mai bang data
            jo.putString(colName.PromoCols.INTRO_SMS,INTRO_SMS); // noi dung ban sms gioi thieu chuong trinh khuyen mai

            jo.putBoolean(colName.PromoCols.ACTIVE,ACTIVE);
            jo.putString(colName.PromoCols.TYPE,TYPE);
            jo.putNumber(colName.PromoCols.MAX_TIMES,MAX_TIMES);
            jo.putNumber(colName.PromoCols.MIN_TIMES,MIN_TIMES);

            jo.putString(colName.PromoCols.NOTI_CAPTION,NOTI_CAPTION);
            jo.putString(colName.PromoCols.NOTI_BODY_INVITER, NOTI_BODY_INVITER);
            jo.putString(colName.PromoCols.NOTI_SMS_INVITER, NOTI_SMS_INVITER);

            jo.putString(colName.PromoCols.NOTI_BODY_INVITEE, NOTI_BODY_INVITEE);
            jo.putString(colName.PromoCols.NOTI_SMS_INVITEE, NOTI_SMS_INVITEE);

            jo.putString(colName.PromoCols.NOTI_COMMENT, NOTI_COMMENT);

            jo.putString(colName.PromoCols.ADJUST_ACCOUNT,ADJUST_ACCOUNT);
            jo.putNumber(colName.PromoCols.CREATE_TIME,CREATE_TIME);

            jo.putString(colName.PromoCols.ADJUST_PIN,ADJUST_PIN);
            jo.putNumber(colName.PromoCols.DURATION_TRAN,DURATION_TRAN);

            jo.putString(colName.PromoCols.OFF_TIME_FROM,OFF_TIME_FROM);
            jo.putString(colName.PromoCols.OFF_TIME_TO,OFF_TIME_TO);
            jo.putBoolean(colName.PromoCols.STATUS,STATUS);
            jo.putBoolean(colName.PromoCols.STATUS_IOS,STATUS_IOS);
            jo.putBoolean(colName.PromoCols.STATUS_ANDROID,STATUS_ANDROID);
            jo.putBoolean(colName.PromoCols.ENABLE_PHASE2,ENABLE_PHASE2);
            jo.putString(colName.PromoCols.ADDRESSNAME, ADDRESSNAME);
            jo.putBoolean(colName.PromoCols.ISVERTICLE, ISVERTICLE);
            jo.putObject(colName.PromoCols.EXTRA, EXTRA);
            return jo;
        }

        public boolean isInvalid() {
            return true;
        }
    }
}
