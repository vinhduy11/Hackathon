package com.mservice.momo.data.web;

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
 * Created by locnguyen on 31/07/2014.
 */
public class ServiceDb {
    private Logger logger;
    private EventBus eventBus;

    public ServiceDb(EventBus eventBus, Logger logger) {
        this.logger = logger;
        this.eventBus = eventBus;
    }

    public void removeObj(String id, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        JsonObject match = new JsonObject();

        query.putString(MongoKeyWords.ACTION, MongoKeyWords.DELETE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCols.TABLE);

        match.putString(colName.ServiceCols.ID, id);

//        match.putBoolean(colName.ServiceCols.DELETED,false);
        query.putObject(MongoKeyWords.MATCHER, match);

        //db.products.remove( { qty: { $gt: 20 } } )

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int count = result.body().getInteger("number", 0);
                callback.handle(count > 0);
            }
        });
    }

    public void upsert(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCols.TABLE);

        JsonObject match = new JsonObject();
        match.putString(colName.ServiceCols.SERVICE_ID, obj.serviceID);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, obj.toJsonObject());

        query.putObject(MongoKeyWords.OBJ_NEW, set);
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

    public void upsertID(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);

        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCols.TABLE);

        JsonObject criteria = new JsonObject();

        String id = obj.id;

        JsonObject data = obj.toJsonObject();
        data.removeField(colName.ServiceCols.ID);

        if (id != null && !"".equalsIgnoreCase(id)) {
            criteria.putString(colName.ServiceCols.ID, id);
        } else {
            insertDoc(obj.toJsonObject(), new Handler<Boolean>() {
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
                if (obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")) {
                    callback.handle(true);
                } else {
                    callback.handle(false);
                }
            }
        });

    }

    public void insertDoc(JsonObject json, final Handler<Boolean> callback) {
        json.removeField(colName.ServiceCols.ID);

        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection", colName.ServiceCols.TABLE)
                .putObject("document", json);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body() != null) {
                    String createdId = event.body().getString("_id");
                    if (createdId != null) {
                        callback.handle(true);
                        return;
                    }
                }

                callback.handle(false);
            }
        });
    }

    public void getlist(final boolean isStore, final String serviceID, final String catId, final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServiceCols.LAST_UPDATE, -1);
//        sort.putNumber(colName.ServiceCols.SERVICE_NAME, 1);
//        sort.putNumber(colName.ServiceCols.ORDER, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();
        if (serviceID != null && !"".equalsIgnoreCase(serviceID)) {
            JsonObject ne = new JsonObject();
            ne.putString(MongoKeyWords.REGEX, serviceID);
            match.putObject(colName.ServiceCols.SERVICE_ID, ne);
        }

        if (catId != null && !"".equalsIgnoreCase(catId)) {
            match.putString(colName.ServiceCols.CAT_ID, catId);
        }
        match.putBoolean(colName.ServiceCols.IS_STORE, isStore);
        //match.putBoolean(colName.ServiceCols.NOT_LOAD_EU_SERVICE, false);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = new ArrayList<>();

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getUserServiceList(final String serviceID, final String catId, final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServiceCols.LAST_UPDATE, -1);
//        sort.putNumber(colName.ServiceCols.SERVICE_NAME, 1);
//        sort.putNumber(colName.ServiceCols.ORDER, 1);
        query.putObject(MongoKeyWords.SORT, sort);

        JsonObject match = new JsonObject();
        if (serviceID != null && !"".equalsIgnoreCase(serviceID)) {
            match.putString(colName.ServiceCols.SERVICE_ID, serviceID);
        }

        if (catId != null && !"".equalsIgnoreCase(catId)) {
            match.putString(colName.ServiceCols.CAT_ID, catId);
        }
        match.putBoolean(colName.ServiceCols.NOT_LOAD_EU_SERVICE, false);
        query.putObject(MongoKeyWords.MATCHER, match);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }

                // return default value
                callback.handle(arrayList);
            }
        });
    }

    public void getAll(final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber(colName.ServiceCols.LAST_UPDATE, -1);
        query.putObject(MongoKeyWords.SORT, sort);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ArrayList<Obj> arrayList = null;

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                if (results != null && results.size() > 0) {
                    arrayList = new ArrayList<>();
                    for (Object o : results) {
                        arrayList.add(new Obj((JsonObject) o));
                    }
                }
                // return default value
                callback.handle(arrayList);
            }
        });
    }

    //cong nguyen add new method
    public void findOne(String serviceId, final Handler<Obj> callback) {
        logger.info("findOne ServiceDb");
        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.ServiceCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.ServiceCols.SERVICE_ID, serviceId);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                Obj obj = null;

                JsonObject joResult = event.body().getObject(MongoKeyWords.RESULT, null);
                if (joResult != null) {
                    logger.info(joResult.toString());
                    obj = new Obj(joResult);
                }

                callback.handle(obj);
            }
        });
    }

    public static class Obj {
        public String id = "";
        public String serviceType = "";
        public String serviceID = "";
        public String serviceName = "";
        public String partnerCode = "";
        public String partnerSite = "";
        public String iconUrl = "";
        public String textPopup = "";
        public Boolean hasCheckDebit = true;
        public Integer status = 0;
        public Integer statusAndroid = 0;
        public String titleDialog = "";
        public long lastUpdateTime = 0;
        public String billType = "text";
        public String billerID = "";
        public String billPay = "";
        public boolean IsPromo = false;

        //new
        public int order = 10000;
        public int star = 0;
        public int totalForm = 1;// tong so form cua dinh vu nay
        public String cateName = "";
        public String cateId = "";
        public boolean uat = false;
        public String webPaymentUrl = "";

        public boolean check_time_on_one_bill = false;
        public int pay_time_on_one_bill = 0;

        public boolean not_load_eu_service = false;
        public boolean isStore = false;
        public Integer active = 0;

        public Obj() {
        }

        public Obj(JsonObject input) {
            id = input.getString(colName.ServiceCols.ID, "");
            serviceType = input.getString(colName.ServiceCols.SERVICE_TYPE, "");
            serviceID = input.getString(colName.ServiceCols.SERVICE_ID, "");
            serviceName = input.getString(colName.ServiceCols.SERVICE_NAME, "");
            partnerCode = input.getString(colName.ServiceCols.PARTNER_ID, "");
            partnerSite = input.getString(colName.ServiceCols.PARTNER_SITE, "");
            iconUrl = input.getString(colName.ServiceCols.ICON_URL, "");
            textPopup = input.getString(colName.ServiceCols.TEXT_POPUP, "");
            hasCheckDebit = input.getBoolean(colName.ServiceCols.HAS_CHECK_DEBIT, false);
            status = input.getInteger(colName.ServiceCols.STATUS, 0);
            statusAndroid = input.getInteger(colName.ServiceCols.STATUS_ANDROID, 0);
            titleDialog = input.getString(colName.ServiceCols.TITLE_DIALOG, "");
            lastUpdateTime = input.getLong(colName.ServiceCols.LAST_UPDATE, 0);
            billType = input.getString(colName.ServiceCols.BILL_TYPE, "text");

            billerID = input.getString(colName.ServiceCols.BILLERID, "");
            billPay = input.getString(colName.ServiceCols.BILLPAY, "");
            IsPromo = input.getBoolean(colName.ServiceCols.IS_PROMO, false);

            order = input.getInteger(colName.ServiceCols.ORDER, 10000);
            star = input.getInteger(colName.ServiceCols.STAR, 0);
            totalForm = input.getInteger(colName.ServiceCols.TOTAL_FORM, 1);
            cateName = input.getString(colName.ServiceCols.CAT_NAME, "");
            cateId = input.getString(colName.ServiceCols.CAT_ID, "");
            uat = input.getBoolean(colName.ServiceCols.UAT, false);
            active = input.getInteger(colName.ServiceCols.ACTIVE, 1);
            webPaymentUrl = input.getString(colName.ServiceCols.WEB_PAYMENT_URL, "");

            check_time_on_one_bill = input.getBoolean(colName.ServiceCols.CHECK_TIME_ON_ONE_BILL, false);
            pay_time_on_one_bill = input.getInteger(colName.ServiceCols.PAY_TIME_ON_ONE_BILL, 0);
            not_load_eu_service = input.getBoolean(colName.ServiceCols.NOT_LOAD_EU_SERVICE, false);
            isStore = input.getBoolean(colName.ServiceCols.IS_STORE, false);
        }

        public JsonObject toJsonObject() {
            JsonObject output = new JsonObject();

            output.putString(colName.ServiceCols.ID, id);
            output.putString(colName.ServiceCols.SERVICE_TYPE, serviceType);
            output.putString(colName.ServiceCols.SERVICE_ID, serviceID);
            output.putString(colName.ServiceCols.SERVICE_NAME, serviceName);
            output.putString(colName.ServiceCols.PARTNER_ID, partnerCode);
            output.putString(colName.ServiceCols.PARTNER_SITE, partnerSite);
            output.putString(colName.ServiceCols.ICON_URL, iconUrl);
            output.putString(colName.ServiceCols.TEXT_POPUP, textPopup);
            output.putBoolean(colName.ServiceCols.HAS_CHECK_DEBIT, hasCheckDebit);
            output.putNumber(colName.ServiceCols.STATUS, status);
            output.putNumber(colName.ServiceCols.STATUS_ANDROID, statusAndroid);
            output.putString(colName.ServiceCols.TITLE_DIALOG, titleDialog);
            output.putNumber(colName.ServiceCols.LAST_UPDATE, lastUpdateTime);

            output.putString(colName.ServiceCols.BILL_TYPE, billType);

            output.putString(colName.ServiceCols.BILLERID, billerID);
            output.putString(colName.ServiceCols.BILLPAY, billPay);
            output.putBoolean(colName.ServiceCols.IS_PROMO, IsPromo);

            output.putNumber(colName.ServiceCols.ORDER, order);
            output.putNumber(colName.ServiceCols.STAR, star);
            output.putNumber(colName.ServiceCols.TOTAL_FORM, totalForm);
            output.putString(colName.ServiceCols.CAT_NAME, cateName);
            output.putString(colName.ServiceCols.CAT_ID, cateId);
            output.putBoolean(colName.ServiceCols.UAT, uat);
            output.putNumber(colName.ServiceCols.ACTIVE, active);

            output.putString(colName.ServiceCols.WEB_PAYMENT_URL, webPaymentUrl);

            output.putNumber(colName.ServiceCols.PAY_TIME_ON_ONE_BILL, pay_time_on_one_bill);

            output.putBoolean(colName.ServiceCols.CHECK_TIME_ON_ONE_BILL, check_time_on_one_bill);

            output.putBoolean(colName.ServiceCols.NOT_LOAD_EU_SERVICE, not_load_eu_service);
            output.putBoolean(colName.ServiceCols.IS_STORE, isStore);
            return output;
        }

        public boolean isInvalid() {
            return true;
        }
    }

}
