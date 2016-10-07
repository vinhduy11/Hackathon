package com.mservice.momo.web.internal.services;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.VcbCmndRecs;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.db.mongo.MongoBase;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.HttpResponseCommon;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by locnguyen on 30/07/2014.
 */
public class ControllerVcbMapWallet {

    private Vertx vertx;
    private Logger logger;
    private PhonesDb phonesDb;
    private VcbCmndRecs vcbCmndRecs;

    public ControllerVcbMapWallet(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        phonesDb = new PhonesDb(vertx.eventBus(),logger);
        vcbCmndRecs = new VcbCmndRecs(vertx.eventBus(),logger);
    }

    @Action( path = "/vcb/mapwallet")
    public void notifyVcbMapWallet(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "notifyVcbMapWallet");

        MultiMap params = request.params();
        String data = "";

        if ("GET".equalsIgnoreCase(request.method())){
            data = params.get("data");
        } else {
            data = context.postData;
        }

        log.add("data", data == null ? "null" : data);

        /*{
            "number":"0974540385",
            "bankcode":"12345",
            "cardid":"271319190",
            "bankname":"Vietcombank" or Viettinbank

            "name":"Nguyen van a",
            "waitingReg":true
        }*/

        //
        JsonObject json = null;

        String tmpphone = "";
        String tmpbankcode = "";
        String tmpcardid ="";
        String tmpname = "";
        boolean tmpwaitingReg = false;
        String tmpbankname = "";

        try {
            json = new JsonObject(data);

            tmpphone = json.getString("number","0");
            tmpbankcode = json.getString("bankcode","");
            tmpcardid = json.getString("cardid","");
            tmpname = json.getString("name","");
            tmpwaitingReg = json.getBoolean("waitingReg");
            tmpbankname = json.getString("bankname","Vietcombank");

        } catch (Exception e) {
            log.add("exception intput", e.toString());
        }

        final JsonObject result = new JsonObject();

        //kiem tra input
        boolean isInputValid = true;
        if (DataUtil.strToInt(tmpphone) == 0){
            isInputValid =false;
            log.add("invalid phone number", tmpphone);
            result.putString("desc", "invalid phone number");
        }

        if ("".equalsIgnoreCase(tmpbankcode)){
            isInputValid =false;
            log.add("invalid bank code", tmpbankcode);
            result.putString("desc", "invalid bank code");
        }

        if("".equalsIgnoreCase(tmpcardid)){
            isInputValid =false;
            log.add("invalid card id", tmpcardid);
            result.putString("desc", "invalid card id");
        }

        result.putString("success", (isInputValid ? "ok" : "failed"));

        // luon tra ket qua ve cho client
        HttpResponseCommon.response(context.getRequest(), result);

        if (!isInputValid) {
            log.writeLog();
            return;
        }

        log.setPhoneNumber(tmpphone);
        final int phone = DataUtil.strToInt(tmpphone);
        final String bankcode = tmpbankcode;
        final String cardid = tmpcardid;

        final String name = tmpname;
        final boolean waitingReg =  tmpwaitingReg;
        final String bankname = tmpbankname;

        VcbCmndRecs.Obj cmnd = new VcbCmndRecs.Obj();
        cmnd.cardid = cardid;
        cmnd.number = "0" + phone;
        cmnd.bankcode = bankcode;
        cmnd.promocount = 0;
        cmnd.timevn = Misc.dateVNFormatWithTime(System.currentTimeMillis());

        log.add("json insert", cmnd.toJson().encodePrettily());

        vcbCmndRecs.insert(cmnd,new Handler<Integer>() {
            @Override
            public void handle(Integer result) {

                log.add("update cmnd result","vcb result " + result);
                if(result == MongoBase.DUPLICATE_CODE_ERROR){
                    //da co tren bang track
                   log.add("******", "vcb cmnd da co tren bang cmnd");
                   log.writeLog();
               }
            }
        });

        final JsonObject joUp = new JsonObject();
        joUp.putNumber(colName.PhoneDBCols.NUMBER, phone);
        /*joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, cardid);
        joUp.putString(colName.PhoneDBCols.BANK_CODE, bankcode);
        joUp.putString(colName.PhoneDBCols.BANK_NAME,bankname);
        joUp.putString(colName.PhoneDBCols.NAME,name);
        joUp.putString(colName.PhoneDBCols.CARD_ID,cardid);*/
        //da dang ky

        joUp.putBoolean(colName.PhoneDBCols.IS_REGED, true);

        //check the phone
        phonesDb.getPhoneObjInfo(phone,new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {

                // vi da dinh danh
                if(obj != null  && obj.isNamed){
                    joUp.putNumber(colName.PhoneDBCols.NUMBER, obj.number);
                    joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, cardid);
                    joUp.putString(colName.PhoneDBCols.BANK_CODE, bankcode);
                    joUp.putString(colName.PhoneDBCols.BANK_NAME,bankname);
                }else if(obj != null){

                    //cap nhat thong tin mapp vi
                    joUp.putNumber(colName.PhoneDBCols.NUMBER, obj.number);
                    joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, cardid);
                    joUp.putString(colName.PhoneDBCols.CARD_ID, cardid);
                    joUp.putString(colName.PhoneDBCols.BANK_CODE, bankcode);
                    joUp.putString(colName.PhoneDBCols.BANK_NAME,bankname);
                    joUp.putString(colName.PhoneDBCols.NAME,name);

                    joUp.putBoolean(colName.PhoneDBCols.WAITING_REG, false);
                    joUp.putBoolean(colName.PhoneDBCols.IS_REGED, obj.isReged);
                    joUp.putBoolean(colName.PhoneDBCols.IS_NAMED, true);
                    joUp.putBoolean(colName.PhoneDBCols.IS_ACTIVED, obj.isActived);
                    joUp.putBoolean(colName.PhoneDBCols.IS_SETUP,obj.isSetup);

                }else{
                    joUp.putBoolean(colName.PhoneDBCols.WAITING_REG, waitingReg);
                    //not set up yet
                    joUp.putBoolean(colName.PhoneDBCols.IS_SETUP, false);
                    joUp.putBoolean(colName.PhoneDBCols.IS_NAMED,true);
                    joUp.putString(colName.PhoneDBCols.NAME, name);

                    joUp.putNumber(colName.PhoneDBCols.NUMBER, phone);
                    joUp.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, cardid);
                    joUp.putString(colName.PhoneDBCols.CARD_ID, cardid);
                    joUp.putString(colName.PhoneDBCols.BANK_CODE, bankcode);
                    joUp.putString(colName.PhoneDBCols.BANK_NAME,bankname);

                }

                phonesDb.updatePartial(phone,joUp,new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj obj) {
                        String success = "failed";
                        if(obj != null){
                            success = "success";
                        }
                        logger.info("0" + phone + "| Cap nhat thong tin map vi vietcombank tren bang phone result: " + success);
                        if(obj != null){
                            logger.info(obj.toJsonObject());
                        }
                    }
                });

            }
        });
    }
}
