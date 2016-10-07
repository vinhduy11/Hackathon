package com.mservice.momo.vertx.event;

import com.mservice.momo.data.*;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.CoreCommon;
import com.mservice.momo.gateway.internal.core.objects.Response;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;

/**
 * Created by concu on 12/30/14.
 */
public class TestProcessEvent {
    private JsonObject glbCfg = null;
    private Vertx vertx;
    private com.mservice.momo.vertx.processor.Common common;
    private Logger logger;
    private TransDb transDb;
    private EventContentDb eventContentDb;
    private CDHHPayBack cdhhPayBack;
    private CDHH cdhh;

    public TestProcessEvent(JsonObject glbCfg, Vertx vertx, Logger logger){
        this.glbCfg = glbCfg;
        this.vertx = vertx;
        this.logger = logger;
        this.transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);
        this.common = new com.mservice.momo.vertx.processor.Common(vertx,logger, glbCfg);
        cdhhPayBack = new CDHHPayBack(vertx.eventBus(),logger);
        this.eventContentDb = new EventContentDb(vertx, logger);
        cdhh = new CDHH(vertx,logger);
    }


    public void testEvent(
            final String    serviceId,
            final int       rootNumber,
            final String    rootPassword,
            final int       momoNumber,
            final int       exptNumber,
            final String    exptName,
            final int       voteCode,
            final int       voteQtty,
            final long      voteValue,
            final Handler<String> callback
    ) {

        //first we try to transfer amount from root to momo


        Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
            @Override
            public void handle(final CdhhConfig cdhhConfig) {

                boolean isOpened = true;
                if (cdhhConfig == null) {
                    isOpened = false;
                }
                long time = System.currentTimeMillis();

                if ((cdhhConfig.endTime == null
                        || cdhhConfig.startTime == null) && isOpened) {
                    isOpened = false;
                }

                if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                        || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                        && isOpened) {
                    isOpened = false;
                }

                if (isOpened == false) {
                    callback.handle("No open session for " + serviceId);
                    return;

                }

                boolean perType = "day".equalsIgnoreCase(getPerType(serviceId));

                String col= cdhhConfig.collName;
                if(perType){
                    //check by day
                    col = colName.CDHHCols.table;
                }
                logger.info("check valid col "+ col + " for number "  + exptNumber + " service " + serviceId + " code " + voteCode + " per day " + perType);

                //check exptNumber valid
                cdhh.findByNumberAndSection(cdhhConfig.collName, perType, exptNumber, serviceId, voteCode, new Handler<Integer>() {
                    @Override
                    public void handle(Integer count) {

                        long curTime = System.currentTimeMillis();
                        // UPDATE: for each serviceId
                        if (count + voteQtty > getMaxSMS(serviceId)) {
                            callback.handle("Over limit for number " + exptNumber + " resquest " + voteQtty + " current " + count + " max " + getMaxSMS(serviceId));
                            return;
                        }

                        // UPDATE: for each serviceId
                        final String toAgent = getRevieveAccount(serviceId, "" + voteCode);


                        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, "0" + momoNumber);
                        log.add("from root to momo", "0" + rootNumber + "-> 0" + momoNumber + " : " + voteValue);

                        //transfer from root to momo
                        CoreCommon.voteVent(vertx,"0" + rootNumber, rootPassword,voteValue,"0" + momoNumber, log,new Handler<Response>(){
                            @Override
                            public void handle(Response response) {

                                if (response.Error != 0) {
                                    callback.handle("root account has no money " + rootNumber + " - " + voteValue + " - error " + response.Error);
                                    log.add("from root to momo", "root account has no money " + rootNumber + " - " + voteValue + " - error " + response.Error);
                                    log.writeLog();
                                    return;
                                }
                                else{
                                    log.add("from root to momo done", "root account has no money " + rootNumber + " - " + voteValue);
                                    log.add("from root to momo done", "do voting now");

                                    CoreCommon.voteVent(vertx, "0" + momoNumber, "", voteValue, toAgent, log, new Handler<Response>() {
                                        @Override
                                        public void handle(final Response requestObj) {

                                            log.add("error", requestObj.Error);
                                            log.add("desc", SoapError.getDesc(requestObj.Error));

                                            //soapObjReply.error = 100;
                                            if (requestObj.Error != 0) {
                                                callback.handle("Không đủ tiền bình chọn " + momoNumber);
                                                log.add("vote from momo fail", "0" + momoNumber + " vote khong thanh cong loi " + requestObj.Error);
                                                log.writeLog();
                                                return;
                                            } else {
                                                log.add("vote from momo ok", "binh chon thanh cong, tien hanh luu du lieu");
                                                // UPDATE: for each serviceId
                                                saveEventInfo("" + voteCode, exptNumber, voteValue, voteQtty, requestObj.Tid, log, exptName, serviceId, cdhhConfig);

                                                log.add("vote from momo ok", "luu thanh cong, payback");

                                                Misc.getPayBackCDHHSetting(vertx, serviceId, new Handler<CDHHPayBackSetting.Obj>() {
                                                    @Override
                                                    public void handle(final CDHHPayBackSetting.Obj pbObj) {
                                                        if ((pbObj != null) && (pbObj.status == true)) {
                                                            //do payback
                                                            ArrayList<Misc.KeyValue> arrayList = new ArrayList<>();
                                                            arrayList.add(new Misc.KeyValue("payback", serviceId));

                                                            CoreCommon.voteVent(vertx, pbObj.paybackaccount, "", voteValue, "0" + momoNumber, log, new Handler<Response>() {
                                                                @Override
                                                                public void handle(final Response paypackToMomo) {
                                                                    if (requestObj.Error != 0) {
                                                                        log.add("payback that bai", paypackToMomo.Error);
                                                                        log.writeLog();
                                                                        callback.handle("payback that bai 0" + momoNumber + " payback that bai loi " + paypackToMomo.Error);
                                                                    }
                                                                    else{
                                                                        CoreCommon.voteVent(vertx, "0" + momoNumber, "", voteValue, "0" + rootNumber, log, new Handler<Response>() {
                                                                            @Override
                                                                            public void handle(Response momoToRootResp) {
                                                                                log.add("chuyen tien ve tai khoan tong ", momoToRootResp.Error + " - tid " + momoToRootResp.Tid);
                                                                                log.writeLog();
                                                                                callback.handle("chuyen tien tai khoan tong ket qua " + momoNumber + " loi " + momoToRootResp.Error);
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else{
                                                            log.add("payback that bai", "khong load duoc config paypack " + "0" + momoNumber);
                                                            log.writeLog();
                                                            callback.handle("payback that bai khong load duoc config paypack " + "0" + momoNumber);
                                                        }
                                                    }
                                                });
                                            }


                                        }
                                    });

                                }

                            }
                        });


                        //vote via core





                    }
                });
            }
        });
    }




    private void saveEventInfo(final String code
            , int phoneNumber
            , final long amount
            , final long voteAmount
            , long tranid
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final String name
            , final String serviceId
            , final CdhhConfig cdhhConfig) {
        //todo save

        final CDHH.Obj obj = new CDHH.Obj();

        obj.code = String.valueOf(DataUtil.strToInt(code));
        obj.number = phoneNumber;
        obj.time = System.currentTimeMillis();
        obj.value = amount;
        obj.day_vn = Misc.dateVNFormat(System.currentTimeMillis());
        obj.voteAmount = voteAmount;
        obj.time_vn = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        obj.tranid = tranid;
        obj.name = name;
        obj.serviceid = serviceId;

        log.add("save json", obj.toJson().encodePrettily());
        cdhh.save(obj, cdhhConfig, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });
    }



    private String getRevieveAccount(String serviceId, String msbc){
        JsonObject jsonObject = glbCfg.getObject(serviceId);
        if (jsonObject == null) {
            return "";
        }
        else {
            JsonArray jsonArray = jsonObject.getArray("account");
            int idx = DataUtil.strToInt(msbc)-1;
            if(idx > jsonArray.size() || idx < 0){
                idx = 0;
            }
            return jsonArray.get(idx);
        }
    }

    private int getMaxSMS(String serviceId){

        JsonObject jsonObject = glbCfg.getObject(serviceId);
        if (jsonObject == null) {
            return 0;
        }
        else {
            return jsonObject.getInteger("max_sms", 0);
        }
    }

    private String getPerType(String serviceId){

        JsonObject jsonObject = glbCfg.getObject(serviceId);
        if (jsonObject == null) {
            return "day";
        }
        else {
            return jsonObject.getString("per_type", "day");
        }
    }





}
