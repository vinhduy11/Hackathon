package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 3/23/15.
 */
public class CardTypeDb {
    private Vertx vertx;
    private Logger logger;
    public CardTypeDb(Vertx vertx,Logger logger){
        this.vertx = vertx;
        this.logger = logger;
    }

    // Update record xuong Database
    public void upsert(final Obj o, final Handler<Boolean> callback) {

        final JsonObject query = new JsonObject();
        //Dieu kien query
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_AND_MODIFY);
        query.putString(MongoKeyWords.COLLECTION, colName.CardType.table);

        //matcher
        final JsonObject match = new JsonObject();

        //Kiem tra gia tri id lay duoc tu web
        if(!"".equalsIgnoreCase(o.id)){
            //Dung de update truong hop ID co ton tai
            //Neu khac rong thi bo vao match
            match.putString(colName.CardType.id,o.id);
            //Dieu kien so sanh, de match vao matcher
            query.putObject(MongoKeyWords.MATCHER, match);
        }else{
            // Neu Id khong ton tai => dang insert 1 dong moi
            match.putString(colName.CardType.partnerCode,o.partnerCode);
            match.putString(colName.CardType.cardType,o.cardType);
            query.putObject(MongoKeyWords.MATCHER, match);
        }
        //sort
        query.putObject(MongoKeyWords.SORT, new JsonObject("{}"));

        JsonObject objNew = o.toJson();
        objNew.removeField(colName.CardType.id);

        //Dieu kien query
        final JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, objNew);
        query.putObject(MongoKeyWords.UPDATE, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        //Goi xuong DB de update record
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS,query,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject json  = event.body();
                if(json!= null){
                    boolean result = json.getString(MongoKeyWords.STATUS).equalsIgnoreCase("ok");

                    callback.handle(result);

                    //Neu ket qua OK (thanh cong), thi update luon cache record.
                    if (result) {
                        //Truyen tham so vao ServiceReq de mang qua ServiceConfVerticle class.
                        Common.ServiceReq serviceReq = new Common.ServiceReq();
                        serviceReq.Command = Common.ServiceReq.COMMAND.UPDATE_CARD_TYPE;

                        //Truyen serviceReq qua ben ServiceConfVerticle de thuc hien update lai record
                        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
                    }
                }
                else {
                    callback.handle(false);
                }
            }
        });
    }

    //Ham nay la ham tim kiem gia tri trong Database
    public void find(String partnerCode
                        , String cardType
                        , Boolean enable
                        , long lastTime
                        , final Handler<ArrayList<Obj>> callback){

        //1. query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.COLLECTION, colName.CardType.table);
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);

        //2.matcher
        JsonObject matcher = null;
        if(!"".equalsIgnoreCase(partnerCode) && partnerCode != null){
            matcher = matcher == null ? new JsonObject() : matcher;
            matcher.putString(colName.CardType.partnerCode,partnerCode);
        }
        //Neu cardType khong rong => dang search theo dieu kien
        if(!"".equalsIgnoreCase(cardType) && cardType !=null){
            matcher = matcher == null ? new JsonObject() : matcher;
            matcher.putString(colName.CardType.cardType,cardType);
        }

        if(enable != null){
            matcher = matcher == null ? new JsonObject() : matcher;
            matcher.putBoolean(colName.CardType.enable, enable.booleanValue());
        }

        if(lastTime != 0){
            matcher = matcher == null ? new JsonObject() : matcher;

            JsonObject gte = new JsonObject();
            gte.putNumber(MongoKeyWords.GREATER,lastTime);
            matcher.putObject(colName.CardType.lastTime,gte);

        }

        //Neu matcher khac null => dang search
        if(matcher!= null){
            query.putObject(MongoKeyWords.MATCHER,matcher);
        }

        //goi xuong bus mongoDb
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);
                ArrayList<Obj> arrayList = new ArrayList<>();

                if(results != null && results.size() > 0){
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public static class Obj {
        public String id = "_id";
        public String partnerCode = "pcode";
        public String cardType = "cardType";
        public String desc = "desc";
        public long lastTime = 0;
        public boolean enable = false;
        public String iconUrl = "";

        public Obj() {
        }

        public Obj(JsonObject jo) {
            id = jo.getString(colName.CardType.id, "");
            partnerCode = jo.getString(colName.CardType.partnerCode, "");
            cardType = jo.getString(colName.CardType.cardType, "");
            desc = jo.getString(colName.CardType.desc, "");
            lastTime = jo.getLong(colName.CardType.lastTime, 0);
            enable = jo.getBoolean(colName.CardType.enable, false);
            iconUrl = jo.getString(colName.CardType.iconUrl, "");
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.CardType.id, id);
            jo.putString(colName.CardType.partnerCode, partnerCode);
            jo.putString(colName.CardType.cardType, cardType);
            jo.putString(colName.CardType.desc, desc);
            jo.putNumber(colName.CardType.lastTime, lastTime);
            jo.putBoolean(colName.CardType.enable, enable);
            jo.putString(colName.CardType.iconUrl, iconUrl);
            return jo;
        }
    }
}
