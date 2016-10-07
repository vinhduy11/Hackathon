package com.mservice.momo.data.codeclaim;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import java.util.ArrayList;

/**
 * Created by manhly on 14/07/2016.
 */
public class ClaimCode_FecreditPromotionDb {

    private Vertx vertx;
    private Logger logger;

    public ClaimCode_FecreditPromotionDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;


    }
    public void insert(final Obj obj, final Handler<Integer> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.INSERT)
                .putString(MongoKeyWords.COLLECTION, colName.FeCreditPromotionCols.TABLE)
                .putObject(MongoKeyWords.DOCUMENT, obj.toJson());


        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                int result = 0;
                if (!event.body().getString(MongoKeyWords.STATUS).equals("ok")) {

                    JsonObject error = new JsonObject(event.body().getString("message", "{code:-1}"));
                    result = error.getInteger("code", -1);
                }
                callback.handle(result);
            }
        });
    }

    public void updatePartial(String phone_number
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.FeCreditPromotionCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.FeCreditPromotionCols.PHONE_NUMBER, phone_number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void upSert(String phone_number
            , Obj obj, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.FeCreditPromotionCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.FeCreditPromotionCols.PHONE_NUMBER, phone_number);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, obj.toJson());

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getString("ok", "").equalsIgnoreCase("ok");
                callback.handle(result);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.FeCreditPromotionCols.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<Obj> arrayList = new ArrayList<Obj>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        Obj obj = new Obj((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void findOne(String phone_number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.FeCreditPromotionCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.FeCreditPromotionCols.PHONE_NUMBER, phone_number);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Obj(joResult);
                    callback.handle(obj);
                }
                else{
                    callback.handle(null);
                }


            }
        });
    }

    public static class Obj {
        public String promotion_name = "";
        public String phone_number = "";
        public String div_no = "";
        public String prefix = "";
        public int number_of_gift = 0;
        public String gift_list = "";
        public long gift_time = 0;
        public long momo_money = 0;
        public int money_time = 0;
        public String agent = "";
        public String notiTitle = "";
        public String notiBody = "";
        public String transBody = "";
        public String notiRollbackTitle = "";
        public String notiRollbackBody = "";
        public String transRollbackBody = "";
        public String partnerName = "";
        public String serviceId = "";
        public boolean getBackMoney = false;
        public boolean activePromo = false;
        public int group = 0;

        public Obj() {
        }

        public Obj(JsonObject jo) {
            promotion_name = jo.getString(colName.FeCreditPromotionCols.PROMOTION_NAME, "");
            phone_number = jo.getString(colName.FeCreditPromotionCols.PHONE_NUMBER, "");
            div_no = jo.getString(colName.FeCreditPromotionCols.DIV_NO, "");
            prefix = jo.getString(colName.ClaimCodePromotionCols.PREFIX, "");
            number_of_gift = jo.getInteger(colName.ClaimCodePromotionCols.NUMBER_OF_GIFT, 0);
            gift_list = jo.getString(colName.ClaimCodePromotionCols.GIFT_LIST, "");
            gift_time = jo.getInteger(colName.ClaimCodePromotionCols.GIFT_TIME, 0);
            momo_money = jo.getLong(colName.ClaimCodePromotionCols.MOMO_MONEY, 0);
            money_time = jo.getInteger(colName.ClaimCodePromotionCols.MONEY_TIME, 0);
            agent = jo.getString(colName.ClaimCodePromotionCols.AGENT, "");
            notiTitle = jo.getString(colName.ClaimCodePromotionCols.NOTI_TITLE, "");
            notiBody = jo.getString(colName.ClaimCodePromotionCols.NOTI_BODY, "");
            transBody = jo.getString(colName.ClaimCodePromotionCols.TRANS_BODY, "");
            notiRollbackTitle = jo.getString(colName.ClaimCodePromotionCols.NOTI_ROLLBACK_TITLE, "");
            notiRollbackBody = jo.getString(colName.ClaimCodePromotionCols.NOTI_ROLLBACK_BODY, "");
            transRollbackBody = jo.getString(colName.ClaimCodePromotionCols.TRANS_ROLLBACK_BODY, "");
            partnerName = jo.getString(colName.ClaimCodePromotionCols.PARTNER_NAME, "");
            serviceId = jo.getString(colName.ClaimCodePromotionCols.SERVICE_ID, "");
            getBackMoney = jo.getBoolean(colName.ClaimCodePromotionCols.GET_BACK_MONEY, false);
            activePromo = jo.getBoolean(colName.ClaimCodePromotionCols.ACTIVE_PROMO, false);
            group = jo.getInteger(colName.ClaimCodePromotionCols.GROUP, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.FeCreditPromotionCols.PROMOTION_NAME, promotion_name);
            jo.putString(colName.FeCreditPromotionCols.PHONE_NUMBER, phone_number);
            jo.putString(colName.FeCreditPromotionCols.DIV_NO, div_no);
            jo.putString(colName.FeCreditPromotionCols.PREFIX, prefix);
            jo.putNumber(colName.FeCreditPromotionCols.NUMBER_OF_GIFT, number_of_gift);
            jo.putString(colName.FeCreditPromotionCols.GIFT_LIST, gift_list);
            jo.putNumber(colName.FeCreditPromotionCols.GIFT_TIME, gift_time);
            jo.putNumber(colName.FeCreditPromotionCols.MOMO_MONEY, momo_money);
            jo.putNumber(colName.FeCreditPromotionCols.MONEY_TIME, money_time);
            jo.putString(colName.FeCreditPromotionCols.AGENT, agent);
            jo.putString(colName.FeCreditPromotionCols.NOTI_TITLE, notiTitle);
            jo.putString(colName.FeCreditPromotionCols.NOTI_BODY, notiBody);
            jo.putString(colName.FeCreditPromotionCols.TRANS_BODY, transBody);
            jo.putString(colName.FeCreditPromotionCols.PARTNER_NAME, partnerName);
            jo.putString(colName.FeCreditPromotionCols.SERVICE_ID, serviceId);
            jo.putBoolean(colName.FeCreditPromotionCols.GET_BACK_MONEY, getBackMoney);
            jo.putString(colName.FeCreditPromotionCols.NOTI_ROLLBACK_TITLE, notiRollbackTitle);
            jo.putString(colName.FeCreditPromotionCols.NOTI_ROLLBACK_BODY, notiRollbackBody);
            jo.putString(colName.FeCreditPromotionCols.TRANS_ROLLBACK_BODY, transRollbackBody);
            jo.putBoolean(colName.FeCreditPromotionCols.ACTIVE_PROMO, activePromo);
            jo.putNumber(colName.FeCreditPromotionCols.GROUP, group);
            return jo;
        }
    }

}
