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
 * Created by locnguyen on 21/07/2014.
 */
public class TrackerDb {

    EventBus eventBus;
    Logger logger;

    public TrackerDb(EventBus eb, Logger log) {
        eventBus = eb;
        logger = log;
    }

    /*tao moi : new
    locked : dang lock tien
    commit : da chuyen tien
    rollback : da hoan tien
    adjusted : tra lai tien cho khach hang
    cancel : da huy*/
    public static String ADD_NEW  = "new";
    public static String LOCKED  = "locked";
    public static String ROLLBACK  = "rollback";
    public static String ADJUSTED  = "adjusted";
    public static String CANCEL  = "cancel";
    public static String COMMIT  = "commit";
    public static String LOCKED_FAILED ="lockedfailed";
    public static String CONFIRM_FAILED ="confirm123phimfailed";


    public static class Obj {
        public long tran_id = 0;
        public String name = "";
        public String execNum ="";
        public String phoneNum = "";
        public String email = "";
        public long amout = 0;
        public String partnerAcc = "";
        public String status = "";
        public String invoiceNo = "";
        public String ticketCode = "";
        public long error = -1;
        public String errorDesc = "";
        public String priceBefore = "";
        public String priceAfter = "";
        public String listPrices = "";
        public String dateConfirm = "";
        public String dateCancel = "";
        public long createTime = 0;

        public Obj()
        {

        }

        public Obj(JsonObject input) {
            tran_id = input.getLong(colName.PartnerTrackCols.TRAN_ID, 0);
            name = input.getString(colName.PartnerTrackCols.NAME, "");
            execNum = input.getString(colName.PartnerTrackCols.EXECUTE_NUMBER, "");
            phoneNum = input.getString(colName.PartnerTrackCols.PHONE_NUMBER, "");
            email = input.getString(colName.PartnerTrackCols.EMAIL, "");
            amout = input.getLong(colName.PartnerTrackCols.AMOUT, 0);
            partnerAcc = input.getString(colName.PartnerTrackCols.PARTNER_ACC, "");
            status = input.getString(colName.PartnerTrackCols.STATUS, "");
            invoiceNo = input.getString(colName.PartnerTrackCols.INVOICE_NO, "");
            ticketCode = input.getString(colName.PartnerTrackCols.TICKET_CODE, "");
            error = input.getLong(colName.PartnerTrackCols.ERROR, -1);
            errorDesc = input.getString(colName.PartnerTrackCols.DESCRIPTION, "");
            priceBefore = input.getString(colName.PartnerTrackCols.PRICE_BEFORE, "");
            priceAfter = input.getString(colName.PartnerTrackCols.PRICE_AFTER, "");
            listPrices = input.getString(colName.PartnerTrackCols.LIST_PRICE, "");
            dateConfirm = input.getString(colName.PartnerTrackCols.DATE_COMFIRM, "");
            dateCancel = input.getString(colName.PartnerTrackCols.DATE_CANCEL, "");
            createTime = input.getLong(colName.PartnerTrackCols.CREATE_DATE, 0);
        }

        public JsonObject toJsonObj() {
            JsonObject objNew = new JsonObject();

            objNew.putNumber(colName.PartnerTrackCols.TRAN_ID, tran_id);
            objNew.putString(colName.PartnerTrackCols.NAME, name);
            objNew.putString(colName.PartnerTrackCols.EXECUTE_NUMBER, execNum);
            objNew.putString(colName.PartnerTrackCols.PHONE_NUMBER, phoneNum);
            objNew.putString(colName.PartnerTrackCols.EMAIL, email);
            objNew.putNumber(colName.PartnerTrackCols.AMOUT, amout);
            objNew.putString(colName.PartnerTrackCols.PARTNER_ACC, partnerAcc);
            objNew.putString(colName.PartnerTrackCols.STATUS, status);
            objNew.putString(colName.PartnerTrackCols.INVOICE_NO, invoiceNo);
            objNew.putString(colName.PartnerTrackCols.TICKET_CODE, ticketCode);
            objNew.putNumber(colName.PartnerTrackCols.ERROR, error);
            objNew.putString(colName.PartnerTrackCols.DESCRIPTION, errorDesc);
            objNew.putString(colName.PartnerTrackCols.PRICE_BEFORE, priceBefore);
            objNew.putString(colName.PartnerTrackCols.PRICE_AFTER, priceAfter);
            objNew.putString(colName.PartnerTrackCols.LIST_PRICE, listPrices);
            objNew.putString(colName.PartnerTrackCols.DATE_COMFIRM, dateConfirm);
            objNew.putString(colName.PartnerTrackCols.DATE_CANCEL, dateCancel);
            objNew.putNumber(colName.PartnerTrackCols.CREATE_DATE, createTime);

            return objNew;
        }
    }

    public void upsertTracker(Obj obj, final Handler<Boolean> callback) {
        if (obj == null) {
            callback.handle(false);
        }

        obj.createTime = System.currentTimeMillis();

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.PartnerTrackCols.TABLE);

        JsonObject match = new JsonObject();
        match.putNumber(colName.PartnerTrackCols.TRAN_ID, obj.tran_id);

        query.putObject(MongoKeyWords.CRITERIA, match);

        JsonObject set = new JsonObject();
        set.putObject(MongoKeyWords.SET_$, obj.toJsonObj());

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

    /*
    *
    *
    *
    * */

    public void getTrackerInfo(long tran_id
            , String invoiceNo
            , String ticket_code
            , long fromDate
            , long toDate
            , int pageNum
            , int pageSize, final Handler<ArrayList<Obj>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.PartnerTrackCols.TABLE);

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("_id", -1);
        query.putObject(MongoKeyWords.SORT, sort);

        //match
        JsonObject match = new JsonObject();
        if (tran_id != 0) {
            match.putNumber(colName.PartnerTrackCols.TRAN_ID, tran_id);
        }

        if (invoiceNo != null && !"".equalsIgnoreCase(invoiceNo)) {
            match.putString(colName.PartnerTrackCols.INVOICE_NO, invoiceNo);
        }

        if (ticket_code != null && !"".equalsIgnoreCase(ticket_code)) {
            match.putString(colName.PartnerTrackCols.TICKET_CODE, ticket_code);
        }

        JsonObject gte = null;
        if (fromDate != 0) {
            gte = new JsonObject();
            gte.putNumber(MongoKeyWords.GREATER_OR_EQUAL, fromDate);
            match.putObject(colName.PartnerTrackCols.CREATE_DATE, gte);
        }
        JsonObject lte = null;
        if (toDate != 0) {
            lte = new JsonObject();
            lte.putNumber(MongoKeyWords.LESS_OR_EQUAL, toDate);
            match.putObject(colName.PartnerTrackCols.CREATE_DATE, lte);
        }

        query.putObject(MongoKeyWords.MATCHER, match);

        int skip = (pageNum - 1) * pageSize;
        int records = pageNum * pageSize;

        query.putNumber(MongoKeyWords.SKIP, skip);
        query.putNumber(MongoKeyWords.LIMIT, records);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> jo) {
                JsonObject obj = jo.body();
                ArrayList<Obj> listObj  = null;
                if ( obj != null
                        && obj.getString(MongoKeyWords.STATUS, "ko").equalsIgnoreCase("ok")
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY) != null
                        && jo.body().getArray(MongoKeyWords.RESULT_ARRAY).size() >0
                        ) {

                    listObj = new ArrayList<Obj>();
                    for (int i=0; i<jo.body().getArray(MongoKeyWords.RESULT_ARRAY).size(); i++) {
                        JsonObject o = jo.body().getArray(MongoKeyWords.RESULT_ARRAY).get(i);
                        listObj.add(new Obj(o));
                    }

                }

                callback.handle(listObj);
            }
        });
    }
}

