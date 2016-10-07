package com.mservice.momo.data;

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
 * Created by concu on 12/9/15.
 */
public class MisPopupDb {
    private Vertx vertx;
    private Logger logger;

    public MisPopupDb(Vertx vertx, Logger logger) {
        this.logger = logger;
        this.vertx = vertx;
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<Obj>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.MisPopup.TABLE);

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

    public void findOne(String number, final Handler<Obj> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.MisPopup.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.MisPopup.PHONE, number);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    obj = new Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public void deletePopupData(String number, final Handler<Boolean> callback)
    {
        JsonObject query    = new JsonObject();
        JsonObject matcher   = new JsonObject();

        query.putString( MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.MisPopup.TABLE);

        matcher.putString(colName.MisPopup.PHONE,number);
//        matcher.putNumber(colName.BillDBCols.PAY_CHANEL, obj.payChanel);

        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putBoolean(MongoKeyWords.MULTI, true);
        vertx.eventBus().send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jObjMsg) {
                JsonObject obj = jObjMsg.body();
                if (obj.getString( MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });
    }




    public static class Obj {

        public String caption = "";
        public String body = "";
        public String phone_number = "";
        public long tranId = 0;
        public Obj() {
        }

        public Obj(JsonObject jo) {
//
            phone_number = jo.getString(colName.MisPopup.PHONE, "").trim();
            caption = jo.getString(colName.MisPopup.CAPTION, "").trim();
            body = jo.getString(colName.MisPopup.BODY, "");
            tranId = jo.getLong(colName.MisPopup.TRAN_ID, 0);
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();

            jo.putString(colName.MisPopup.PHONE, phone_number.trim());
            jo.putString(colName.MisPopup.CAPTION, caption.trim());
            jo.putString(colName.MisPopup.BODY, body.trim());
            jo.putNumber(colName.MisPopup.TRAN_ID, tranId);
            return jo;
        }
    }
}
