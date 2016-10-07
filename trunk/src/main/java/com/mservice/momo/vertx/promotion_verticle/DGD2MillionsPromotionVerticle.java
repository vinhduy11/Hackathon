package com.mservice.momo.vertx.promotion_verticle;

import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.DGD2MillionsPromotionMembersDb;
import com.mservice.momo.data.promotion.DGD2MillionsPromotionObj;
import com.mservice.momo.data.promotion.DGD2MillionsPromotionTrackingDb;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

/**
* Created by concu on 3/10/16.
*/
public class DGD2MillionsPromotionVerticle extends Verticle {
    private Logger logger;
    private JsonObject glbCfg;
    private TransDb tranDb;

    private boolean isStoreApp;
    private boolean isUAT;
    private DGD2MillionsPromotionMembersDb dgd2MillionsPromotionMembersDb;
    private DGD2MillionsPromotionTrackingDb dgd2MillionsPromotionTrackingDb;
    private AgentsDb agentsDb;
    private boolean remindNoti;
    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        isUAT = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
        // jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());
        remindNoti = glbCfg.getBoolean(StringConstUtil.SEND_REMIND_NOTI, false);
        dgd2MillionsPromotionMembersDb = new DGD2MillionsPromotionMembersDb(vertx, logger);
        dgd2MillionsPromotionTrackingDb = new DGD2MillionsPromotionTrackingDb(vertx, logger);
        agentsDb = new AgentsDb(vertx.eventBus(), logger);
        long checkInfoDgdTime = 30; //
        long getFeeTime = 60;

        if(isUAT)
        {
            checkInfoDgdTime = 2;
            getFeeTime = 5;
        }


        //Kiem tra thong tin diem giao dich da duoc cho phep hoat dong hay chua
        if(remindNoti)
        {
            logger.info("GET FEE AND CHECK INFO DGD");
            vertx.setPeriodic(1000L * 60 * checkInfoDgdTime, new Handler<Long>() {
                @Override
                public void handle(final Long checkInfoTime) {
                    //Kiem tra chuong trinh khuyen mai con dang chay khong
                    getPromotion(StringConstUtil.DGD2MillionsPromoField.GET_FEE_PROGRAM, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonResponse) {
                            int error = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                            final long startPromotionDate = jsonResponse.getLong(StringConstUtil.DGD2MillionsPromoField.START_DATE, System.currentTimeMillis());
                            final int duration = jsonResponse.getInteger(StringConstUtil.DURATION, 0);
                            if(error == 0)
                            {
                                final long oneDayPast = System.currentTimeMillis() - 1000L * 60 * 60 * 24;
                                logger.info(jsonResponse.toString());
                                final long lastUpdate = oneDayPast > startPromotionDate ? oneDayPast : startPromotionDate;
                                logger.info(lastUpdate);
                                JsonObject joFilter = new JsonObject();
                                //Nhung diem giao dich co active date > thoi gian lastupdate (1 ngay)
                                JsonObject joActiveDateGreater = new JsonObject();
                                joActiveDateGreater.putNumber(MongoKeyWords.GREATER_OR_EQUAL, lastUpdate);
                                //Dieu kien 1
                                JsonObject jsonV1 = new JsonObject();
                                jsonV1.putObject(colName.AgentDBCols.ACTIVE_DATE, joActiveDateGreater);

                                //Tim nhung store co lastUpdate lon hon thoi gian 1 ngay gan nhat
                                JsonObject joLastUpdateGreater = new JsonObject();
                                joLastUpdateGreater.putNumber(MongoKeyWords.GREATER_OR_EQUAL, lastUpdate);
                                JsonObject joLastUpdate = new JsonObject();
                                joLastUpdate.putObject(colName.AgentDBCols.LAST_UPDATE_TIME, joLastUpdateGreater);
                                //Nhung store co thoi gian active trong thoi gian khuyen mai
                                JsonObject joActiveDateGreater2 = new JsonObject();
                                joActiveDateGreater2.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startPromotionDate);
                                JsonObject joActiveDate2 = new JsonObject();
                                joActiveDate2.putObject(colName.AgentDBCols.ACTIVE_DATE, joActiveDateGreater2);
                                //Dieu kien v2
                                JsonArray jsonAnd = new JsonArray();
                                jsonAnd.add(joLastUpdate);
                                jsonAnd.add(joActiveDate2);

                                JsonObject jsonV2 = new JsonObject();
                                jsonV2.putArray(MongoKeyWords.AND_$, jsonAnd);

                                JsonArray jsonOr = new JsonArray();
                                jsonOr.add(jsonV1);
                                jsonOr.add(jsonV2);

                                //Dieu kien OR dau tien
                                JsonObject joBigOr = new JsonObject();
                                joBigOr.putArray(MongoKeyWords.OR, jsonOr);

                                //Check dieu kien status
                                JsonObject joStatus = new JsonObject();
                                joStatus.putNumber(colName.AgentDBCols.STATUS, 0);

                                //Check dieu kien tong dai li
                                JsonObject joTDL = new JsonObject();
                                joTDL.putBoolean(colName.AgentDBCols.TDL, false);

                                //Check dieu kien Agent
                                JsonArray jarrType = new JsonArray();
                                jarrType.add(0);
                                jarrType.add(1);
                                jarrType.add(4);
                                JsonObject joIn = new JsonObject();
                                joIn.putArray(MongoKeyWords.IN_$, jarrType);
                                JsonObject joAgentType = new JsonObject();
                                joAgentType.putObject(colName.AgentDBCols.AGENT_TYPE, joIn);


                                JsonArray joBigAnd = new JsonArray();
                                joBigAnd.add(joBigOr);
                                joBigAnd.add(joStatus);
                                joBigAnd.add(joAgentType);
                                joBigAnd.add(joTDL);

                                joFilter.putArray(MongoKeyWords.AND_$, joBigAnd);
                                logger.info("query " + joFilter.toString());
                                agentsDb.searchWithFilter(joFilter, new Handler<ArrayList<AgentsDb.StoreInfo>>() {
                                    @Override
                                    public void handle(final ArrayList<AgentsDb.StoreInfo> listStores) {
                                        logger.info("number of new stores " + listStores.size() );
                                        final AtomicInteger atomicInteger = new AtomicInteger(listStores.size());
                                        vertx.setPeriodic(200L, new Handler<Long>() {
                                            @Override
                                            public void handle(Long insertDbTime) {
                                                int position = atomicInteger.decrementAndGet();
                                                if(position < 0)
                                                {
                                                    vertx.cancelTimer(insertDbTime);
                                                    logger.info("Insert xong thong tin vo DB DGD Member");
                                                }
                                                else {
                                                    final AgentsDb.StoreInfo storeInfo = listStores.get(position);
                                                    Calendar calendar = Calendar.getInstance();
                                                    calendar.setTimeInMillis(storeInfo.activeDate);
                                                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                                                    calendar.set(Calendar.MINUTE, 59);
                                                    calendar.set(Calendar.SECOND, 59);
                                                    long endDate = calendar.getTimeInMillis();
                                                    long next7Days = endDate + 1000L * 60 * 60 * 24 * duration;
                                                    JsonObject joUpsert = new JsonObject();
//                                                joUpsert.putNumber(colName.DGD2MillionsPromotionMembersCol.STORE_ID, storeInfo.rowCoreId);
                                                    joUpsert.putString(colName.DGD2MillionsPromotionMembersCol.MOMO_PHONE, storeInfo.momoNumber);
                                                    joUpsert.putString(colName.DGD2MillionsPromotionMembersCol.STORE_NAME, storeInfo.storeName);
                                                    //joUpsert.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS, false);
                                                    //joUpsert.putNumber(colName.DGD2MillionsPromotionMembersCol.TID_FEE, 0);
                                                    joUpsert.putNumber(colName.DGD2MillionsPromotionMembersCol.ACTIVED_TIME, storeInfo.activeDate);
                                                    //joUpsert.putBoolean(colName.DGD2MillionsPromotionMembersCol.IS_ACTIVED, false);
                                                    //joUpsert.putNumber(colName.DGD2MillionsPromotionMembersCol.REGISTER_TIME, 0);
                                                    joUpsert.putNumber(colName.DGD2MillionsPromotionMembersCol.REGISTER_END_TIME, next7Days);
                                                    dgd2MillionsPromotionMembersDb.upSert(String.valueOf(storeInfo.rowCoreId), joUpsert, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean event) {
                                                        }
                                                    });
                                                }

                                            }
                                        });
                                    }
                                });
                            }
                            else {
                                logger.info(jsonResponse.getString(StringConstUtil.DESCRIPTION, ""));
                            }
                        }
                    });
                }
            });

            //Kiem tra diem giao dich hoat dong de lay fee tham gia khuyen mai
            vertx.setPeriodic(1000L * 60 * getFeeTime, new Handler<Long>() {
                @Override
                public void handle(final Long feeTime) {
                    //Kiem tra de thu hoi phi diem giao dich
                    getPromotion(StringConstUtil.DGD2MillionsPromoField.GET_FEE_PROGRAM, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonResponsePromo) {
                            int error = jsonResponsePromo.getInteger(StringConstUtil.ERROR, -1000);
                            final String agent = jsonResponsePromo.getString(StringConstUtil.DGD2MillionsPromoField.AGENT, "");
                            final long amount = jsonResponsePromo.getLong(StringConstUtil.DGD2MillionsPromoField.AMOUNT, 0);
                            final String partnerName = jsonResponsePromo.getString(colName.TranDBCols.PARTNER_NAME, "");
                            final String bodyTrans = jsonResponsePromo.getString(colName.TranDBCols.COMMENT, "");
                            final String titleNoti = jsonResponsePromo.getString(StringConstUtil.StandardNoti.CAPTION, "");
                            final String bodyNoti = jsonResponsePromo.getString(StringConstUtil.StandardNoti.BODY, "");
                            if(error == 0){
                                final JsonObject joFilter = new JsonObject();
                                joFilter.putBoolean(colName.DGD2MillionsPromotionMembersCol.IS_ACTIVED, null);
                                joFilter.putNumber(colName.DGD2MillionsPromotionMembersCol.REGISTER_TIME, null);
                                joFilter.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_3MONTHS, null);
                                dgd2MillionsPromotionMembersDb.searchWithFilter(joFilter, new Handler<ArrayList<DGD2MillionsPromotionMembersDb.Obj>>() {
                                    @Override
                                    public void handle(final ArrayList<DGD2MillionsPromotionMembersDb.Obj> listDgdMembers) {
                                        //Lay duoc danh sach nhung dua chua active va tham gia chuong trinh, chay thu hoi 1 trieu.
                                        logger.info("danh sach lay fee " + listDgdMembers.size());
                                        final AtomicInteger listDgd = new AtomicInteger(listDgdMembers.size());
                                        vertx.setPeriodic(500L, new Handler<Long>() {
                                            @Override
                                            public void handle(final Long getFeeTimer) {
                                                final int position = listDgd.decrementAndGet();
                                                if(position < 0)
                                                {
                                                    vertx.cancelTimer(getFeeTimer);
                                                }
                                                else {
                                                    final Common.BuildLog log = new Common.BuildLog(logger);
                                                    final String phoneNumber = listDgdMembers.get(position).momo_phone;
                                                    final String storeId = listDgdMembers.get(position).storeId + "";
                                                    getFeeFromStore(agent, amount, phoneNumber, log, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject jsonResponseFromCore) {

                                                            final int error = jsonResponseFromCore.getInteger(StringConstUtil.ERROR, -1000);
                                                            final long tranId = jsonResponseFromCore.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                                                            JsonObject joUpdate = new JsonObject();
                                                            joUpdate.putNumber(colName.DGD2MillionsPromotionMembersCol.ERROR_CODE, error);
                                                            if(error == 0)
                                                            {
                                                                joUpdate.putBoolean(colName.DGD2MillionsPromotionMembersCol.IS_ACTIVED, true);
                                                                joUpdate.putNumber(colName.DGD2MillionsPromotionMembersCol.REGISTER_TIME, System.currentTimeMillis());
                                                                joUpdate.putNumber(colName.DGD2MillionsPromotionMembersCol.TID_FEE, tranId);
                                                                joUpdate.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_2MIL, false);
                                                            }
                                                            else if(System.currentTimeMillis() > listDgdMembers.get(position).register_end_time )
                                                            {
                                                                joUpdate.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_3MONTHS, true);
                                                            }
                                                            dgd2MillionsPromotionMembersDb.updatePartial(storeId, joUpdate, new Handler<Boolean>() {
                                                                @Override
                                                                public void handle(Boolean result) {
                                                                    if(error == 0)
                                                                    {
                                                                        //Send Noti va LSGD thong bao voi user ban da duoc tham gia chuong trinh diem giao dich.
                                                                        fireMoneyNotiAndSendTranHist(phoneNumber, amount, tranId, log, titleNoti, bodyNoti, bodyTrans, partnerName, -1);
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        });

                                    }
                                });
                            }
                            else {
                                logger.info("DGD2MillionsPromotionVerticle " + jsonResponsePromo.getString(StringConstUtil.DESCRIPTION, ""));
                            }
                        }
                    });
                }
            });
        }
        else
        {
            logger.info("DONT GET FEE AND CHECK INFO DGD");
        }


        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final DGD2MillionsPromotionObj dgd2MillionsPromotionObj = new DGD2MillionsPromotionObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(dgd2MillionsPromotionObj.phoneNumber);
                log.add("phoneNumber", dgd2MillionsPromotionObj.phoneNumber);
                log.add("tranId", dgd2MillionsPromotionObj.tranId);
                log.add("serviceID", dgd2MillionsPromotionObj.serviceId);
                log.add("group", dgd2MillionsPromotionObj.group);
                log.add("amount", dgd2MillionsPromotionObj.amount);
                log.add("billId", dgd2MillionsPromotionObj.billId);

                final JsonObject jsonReply = new JsonObject();
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;

                if(!isStoreApp)
                {
                    log.add("desc", "Khong danh co app EU");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Khong danh cho app EU");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }
                else if("".equalsIgnoreCase(dgd2MillionsPromotionObj.phoneNumber) || "".equalsIgnoreCase(dgd2MillionsPromotionObj.serviceId)
                        || dgd2MillionsPromotionObj.tranId == 0 || dgd2MillionsPromotionObj.group == 0 || dgd2MillionsPromotionObj.amount == 0 )
                {
                    log.add("desc", "Du lieu thieu sot nghiem trong");
                    jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Du lieu thieu sot nghiem trong");
                    log.writeLog();
                    message.reply(jsonReply);
                    return;
                }

                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        boolean enableGiftGiving = false;
                        long promo_start_date = 0;
                        long promo_end_date = 0;
                        long currentTime = System.currentTimeMillis();
                        String agent = "";
                        long total_amount = 0;
                        long perTranAmount = 0;
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj promoObj = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
                                promoObj = new PromotionDb.Obj((JsonObject) o);
                                if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.DGD2MillionsPromoField.BONUS_PROGRAM)) {
                                    promo_start_date = promoObj.DATE_FROM;
                                    promo_end_date = promoObj.DATE_TO;
                                    agent = promoObj.ADJUST_ACCOUNT;
                                    total_amount = promoObj.TRAN_MIN_VALUE;
                                    perTranAmount = promoObj.PER_TRAN_VALUE;
                                    break;
                                }
                            }
                            final PromotionDb.Obj dgdPromoObj = promo_start_date > 0 ? promoObj : null;
                            //Check lan nua do dai chuoi ki tu
                            if ("".equalsIgnoreCase(agent) || dgdPromoObj == null) {
                                log.add("desc", "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                JsonObject jsonReply = new JsonObject();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else if (currentTime < promo_start_date || currentTime > promo_end_date) {
                                log.add("desc", "Chua bat dau chay chuong trinh " + dgdPromoObj.NAME);
                                log.writeLog();
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Chua bat dau chay chuong trinh.");
                                message.reply(jsonReply);
                                return;
                            } else if ("".equalsIgnoreCase(dgd2MillionsPromotionObj.phoneNumber) || DataUtil.strToLong(dgd2MillionsPromotionObj.phoneNumber) <= 0) {
                                log.add("desc", "So dien thoai la so dien thoai khong co that.");
                                jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                                jsonReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ, vui lòng nhập mã khác hoặc gọi (08) 399 171 99 để được hỗ trợ");
                                message.reply(jsonReply);
                                log.writeLog();
                                return;
                            } else {
                                //Kiem tra xem da tra thuong cho em nay chua

                                agentsDb.getOneAgent(dgd2MillionsPromotionObj.phoneNumber, "dgd2million", new Handler<AgentsDb.StoreInfo>() {
                                    @Override
                                    public void handle(final AgentsDb.StoreInfo storeInfo) {
                                        log.add("desc", "Lay rcoreId");
                                        if(storeInfo != null)
                                        {
                                            dgd2MillionsPromotionMembersDb.findOne(storeInfo.rowCoreId + "", new Handler<DGD2MillionsPromotionMembersDb.Obj>() {
                                                @Override
                                                public void handle(final DGD2MillionsPromotionMembersDb.Obj dgdMemberObj) {
                                                    if(dgdMemberObj != null && dgdMemberObj.is_actived && !dgdMemberObj.end_bonus_2mils)
                                                    {
                                                        //Kiem tra xem co qua 90 ngay chua
                                                        Calendar calendar = Calendar.getInstance();
                                                        calendar.setTimeInMillis(dgdMemberObj.register_time);
                                                        calendar.set(Calendar.HOUR_OF_DAY, 23);
                                                        calendar.set(Calendar.MINUTE, 59);
                                                        calendar.set(Calendar.SECOND, 59);
                                                        long endDay = calendar.getTimeInMillis();
                                                        long endBonusDay = endDay + 1000L * 60 * 60 * 24 * dgdPromoObj.DURATION;
                                                        if(System.currentTimeMillis() > endBonusDay)
                                                        {
                                                            log.add("desc", "Qua 3 thang khong tra thuong nua");
                                                            log.writeLog();
                                                            return;
                                                        }
                                                        int month = checkMonth(dgdMemberObj.register_time);
                                                        JsonObject joFilter = new JsonObject();
                                                        joFilter.putString(colName.DGD2MillionsPromotionTrackingCol.STORE_ID, dgdMemberObj.storeId);
                                                        joFilter.putNumber(colName.DGD2MillionsPromotionTrackingCol.ERROR_CODE, 0);
                                                        long beginTime = 0;
                                                        long endTime = 0;
//                                                        JsonObject joStartTime = new JsonObject();
//                                                        JsonObject joEndTime = new JsonObject();
//                                                        JsonObject joStart = new JsonObject();
//                                                        JsonObject joEnd = new JsonObject();

//                                                        JsonArray jarrAnd = new JsonArray();

                                                        if(month == 1)
                                                        {
                                                            beginTime = dgdMemberObj.register_time;
                                                            endTime = dgdMemberObj.register_time + 1000L*60*60*24*30;

                                                        }
                                                        else if(month == 2)
                                                        {
                                                            beginTime = dgdMemberObj.register_time + 1000L*60*60*24*30;
                                                            endTime = dgdMemberObj.register_time + 1000L*60*60*24*60;
                                                        }
                                                        else if(month == 3)
                                                        {
                                                            beginTime = dgdMemberObj.register_time + 1000L*60*60*24*60;
                                                            endTime = dgdMemberObj.register_time + 1000L*60*60*24*90;
                                                        }
                                                        else {
                                                            log.add("desc", "Gia tri month " + month + " Qua 3 thang khong tra thuong nua");
                                                            log.writeLog();
                                                            return;
                                                        }
//                                                        joStartTime.putNumber(MongoKeyWords.GREATER_OR_EQUAL, beginTime);
//                                                        joEndTime.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime);
//                                                        joStart.putObject(colName.DGD2MillionsPromotionTrackingCol.TIME, joStartTime);
//                                                        joEnd.putObject(colName.DGD2MillionsPromotionTrackingCol.TIME, joEndTime);
//                                                        jarrAnd.add(joStart);
//                                                        jarrAnd.add(joEnd);
//                                                        joFilter.putArray(MongoKeyWords.AND_$, jarrAnd);
                                                        final long beginMonth = beginTime;
                                                        final long endMonth = endTime;
                                                        dgd2MillionsPromotionTrackingDb.searchWithFilter(joFilter, new Handler<ArrayList<DGD2MillionsPromotionTrackingDb.Obj>>() {
                                                            @Override
                                                            public void handle(ArrayList<DGD2MillionsPromotionTrackingDb.Obj> listBillPay) {
//                                                                if(listBillPay.size() > dgdPromoObj.MAX_TIMES - 1)
//                                                                {
//                                                                    log.add("desc", "Qua " + dgdPromoObj.MAX_TIMES + " giao dich");
//                                                                    log.writeLog();
//                                                                    return;
//                                                                }
                                                                //Kiem tra xem tong so tien > giao dich gan cuoi cung chua
                                                                long totalAmount = 0;
                                                                for(int i = 0; i < listBillPay.size(); i++)
                                                                {
                                                                    totalAmount = totalAmount + listBillPay.get(i).amount;
                                                                }
                                                                log.add("totalAmount", totalAmount);
                                                                boolean updateEndPromo = false;
                                                                if(totalAmount >= dgdPromoObj.TRAN_MIN_VALUE - dgdPromoObj.PER_TRAN_VALUE - 1)
                                                                {
                                                                    updateEndPromo = true;
                                                                }
                                                                executeListBillPay(updateEndPromo, beginMonth, endMonth, dgd2MillionsPromotionObj, dgdMemberObj, listBillPay, log, dgdPromoObj);
                                                            }
                                                        });
                                                    }
                                                    else {
                                                        log.add("desc", "Khong tra thuong tien vi khong co thong tin tra thuong hoac chua duoc kich hoat");
                                                        log.writeLog();
                                                        return;
                                                    }
                                                }
                                            });
                                        }
                                        else {
                                            log.add("desc", "Khong tra thuong tien vi khong co thong tin store");
                                            log.writeLog();
                                            return;
                                        }
                                    }
                                });

                            }
                        }
                    }
                });
            }
        };

        vertx.eventBus().registerLocalHandler(AppConstant.OPEN_NEW_STORE_PROMOTION_BUS_ADDRESS, myHandler);
    }

    private int checkMonth(long registerTime)
    {
        long currentTime = System.currentTimeMillis();
        if(currentTime >= registerTime && currentTime <= registerTime + 1000L*60*60*24*30)
        {
            return 1;
        }
        else if(currentTime > registerTime + 1000L*60*60*24*30 && currentTime <= registerTime + 1000L*60*60*24*60) {
            return 2;
        }
        else if(currentTime > registerTime + 1000L*60*60*24*60 && currentTime <= registerTime + 1000L*60*60*24*90) {
            return 3;
        }
        else {
            return 0;
        }
    }
    private void executeListBillPay(final boolean updateEndPromo, long beginTime, long endTime, final DGD2MillionsPromotionObj dgd2MillionsPromotionObj, final DGD2MillionsPromotionMembersDb.Obj dgdMemberObj, ArrayList<DGD2MillionsPromotionTrackingDb.Obj> listBillPay,final Common.BuildLog log, final PromotionDb.Obj dgdPromoObj)
    {
        log.add("function", "executeListBillPay");
        int resultOk = checkPromotionRule(beginTime, endTime, dgd2MillionsPromotionObj, listBillPay, log, dgdPromoObj);
        if(resultOk == 0)
        {
            log.add("resultOk", resultOk);
            //Tra thuong sau khi thoa rule
            giveBonusMoneyForStore(dgdPromoObj.ADJUST_ACCOUNT, dgdPromoObj.PER_TRAN_VALUE, dgdMemberObj.momo_phone, log, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resultFromCore) {
                    final int error = resultFromCore.getInteger(StringConstUtil.ERROR, -1000);
                    final long tranId = resultFromCore.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                    DGD2MillionsPromotionTrackingDb.Obj dgdTrackingObj = new DGD2MillionsPromotionTrackingDb.Obj();
                    dgdTrackingObj.error_code = error;
                    dgdTrackingObj.momo_phone = dgd2MillionsPromotionObj.phoneNumber;
                    dgdTrackingObj.serviceId = dgd2MillionsPromotionObj.serviceId;
                    dgdTrackingObj.tid_cashback = tranId;
                    dgdTrackingObj.storeId = dgdMemberObj.storeId;
                    dgdTrackingObj.group = dgd2MillionsPromotionObj.group;
                    dgdTrackingObj.tid_billpay = dgd2MillionsPromotionObj.tranId;
                    dgdTrackingObj.amount = dgdPromoObj.PER_TRAN_VALUE;
                    dgdTrackingObj.time = System.currentTimeMillis();
                    dgdTrackingObj.billId = dgd2MillionsPromotionObj.billId;
                    dgd2MillionsPromotionTrackingDb.insert(dgdTrackingObj, new Handler<Integer>() {
                        @Override
                        public void handle(Integer event) {
                            if(error == 0)
                            {
//                                Chúc mừng ĐGD đã nhận được <số tiền>đ (ưu đãi cho giao dịch mã số <số TID>) từ chương trình hỗ trợ ĐGD của M_Service. Hãy tiếp tục đẩy mạnh dịch vụ và gia tăng ưu đãi!
                                log.add("desc", "fireMoneyNotiAndSendTranHist");
                                //Send Noti
                                final String body = String.format(dgdPromoObj.NOTI_COMMENT, dgdPromoObj.PER_TRAN_VALUE, dgd2MillionsPromotionObj.tranId);
                                //Cap nhat member
                                if(updateEndPromo)
                                {
                                    JsonObject joUpdate = new JsonObject();
                                    joUpdate.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_2MIL, true);
                                    dgd2MillionsPromotionMembersDb.updatePartial(dgdMemberObj.storeId, joUpdate, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {
                                            fireMoneyNotiAndSendTranHist(dgd2MillionsPromotionObj.phoneNumber, dgdPromoObj.PER_TRAN_VALUE, tranId, log, dgdPromoObj.NOTI_CAPTION, body, body, dgdPromoObj.INTRO_DATA, 1);
                                        }
                                    });
                                    return;
                                }
                                fireMoneyNotiAndSendTranHist(dgd2MillionsPromotionObj.phoneNumber, dgdPromoObj.PER_TRAN_VALUE, tranId, log, dgdPromoObj.NOTI_CAPTION, body, body, dgdPromoObj.INTRO_DATA, 1);
                                return;
                            }
                        }
                    });
                }
            });
        }
        else if(resultOk == 2)
        {
            log.add("resultOk", resultOk);
            //DGD da nhan du 2 trieu dong.
            JsonObject joUpdate = new JsonObject();
            joUpdate.putBoolean(colName.DGD2MillionsPromotionMembersCol.END_BONUS_2MIL, true);
            dgd2MillionsPromotionMembersDb.updatePartial(dgdMemberObj.storeId, joUpdate, new Handler<Boolean>() {
                @Override
                public void handle(Boolean event) {
                    log.add("desc", "Da update DB khi nhan du 2 trieu " + dgdMemberObj.storeId + " phone: " + dgdMemberObj.momo_phone);
                    log.writeLog();
                }
            });
            return;
        }
        else {
            log.add("resultOk", resultOk);
            log.writeLog();
            return;
        }
    }

    private int checkPromotionRule(long beginTime, long endTime, final DGD2MillionsPromotionObj dgd2MillionsPromotionObj, ArrayList<DGD2MillionsPromotionTrackingDb.Obj> listBillPay, Common.BuildLog log, final PromotionDb.Obj dgdPromoObj)
    {
        long totalAmount = 0;
        int countGroup1 = 0;
        int countMonth = 0;
        for(int i = 0; i < listBillPay.size(); i++)
        {
            totalAmount = totalAmount + listBillPay.get(i).amount;
            if(1 == listBillPay.get(i).group && listBillPay.get(i).time >= beginTime && listBillPay.get(i).time < endTime)
            {
                countGroup1 = countGroup1 + 1;
            }

            if(listBillPay.get(i).time >= beginTime && listBillPay.get(i).time < endTime)
            {
                countMonth = countMonth + 1;
            }
        }
        log.add("totalAmount", totalAmount);
        log.add("countGroup1", countGroup1);
        log.add("countMonth", countMonth);
        if(totalAmount >= dgdPromoObj.TRAN_MIN_VALUE - 1)
        {
            return 2;
        }
        else if(dgd2MillionsPromotionObj.group == 1 && countGroup1 >= dgdPromoObj.MIN_TIMES)
        {
            return 1;
        }
        else if(countMonth > dgdPromoObj.MAX_TIMES - 1)
        {
            return 3;
        }
        else {
            return 0;
        }
    }

    private void getPromotion(final String program, final Handler<JsonObject> callback)
    {
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        //Moi lan chay la kiem tra thoi gian promo.
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonObject joReply = new JsonObject();
                JsonArray array = json.getArray("array", null);
                long promo_start_date = 0;
                long promo_end_date = 0;
                long currentTime = System.currentTimeMillis();
                String agent = "";
                long amount = 0;
                String titleNoti = "";
                String bodyNoti = "";
                String bodyTrans = "";
                String partnerName = "";
                int duration = 0;
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(program)) {
                            promo_start_date = promoObj.DATE_FROM;
                            promo_end_date = promoObj.DATE_TO;
                            agent = promoObj.ADJUST_ACCOUNT;
                            amount = promoObj.PER_TRAN_VALUE;
                            titleNoti = promoObj.NOTI_CAPTION;
                            bodyNoti = promoObj.NOTI_COMMENT;
                            bodyTrans = promoObj.NOTI_COMMENT;
                            partnerName = promoObj.INTRO_DATA;
                            duration = promoObj.DURATION;
                            break;
                        }
                    }
                    //Kiem tra xem con thoi gian khuyen mai ko
                    if(currentTime < promo_start_date || currentTime > promo_end_date || "".equalsIgnoreCase(agent) || amount == 0)
                    {
                        logger.info("program 2 trieu cho diem giao dich");
                        logger.info("thong tin agent1 && agent 2 " + agent);
                        logger.info("thong tin start date " + promo_start_date);
                        logger.info("thong tin end date " + promo_end_date);
                        logger.info("thong tin amount " + amount);
                        logger.info("Ngoai thoi gian khuyen mai, khong ghi nhan diem giao dich");
                        joReply.putString(StringConstUtil.DESCRIPTION, "Khong nam trong thoi gian khuyen mai");
                        joReply.putNumber(StringConstUtil.ERROR, -1);
                        callback.handle(joReply);
                    }
                    else {
                        //Tim thong tin diem giao dich
                        joReply.putNumber(StringConstUtil.DGD2MillionsPromoField.START_DATE, promo_start_date);
                        joReply.putNumber(StringConstUtil.DGD2MillionsPromoField.END_DATE, promo_end_date);
                        joReply.putString(StringConstUtil.DGD2MillionsPromoField.AGENT, agent);
                        joReply.putNumber(StringConstUtil.DGD2MillionsPromoField.AMOUNT, amount);
                        joReply.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
                        joReply.putString(colName.TranDBCols.COMMENT, bodyTrans);
                        joReply.putString(StringConstUtil.StandardNoti.CAPTION, titleNoti);
                        joReply.putString(StringConstUtil.StandardNoti.BODY, bodyNoti);
                        joReply.putNumber(StringConstUtil.DURATION,duration );
                        joReply.putNumber(StringConstUtil.ERROR, 0);
                        callback.handle(joReply);
                    }
                }
                else
                {
                    logger.info("Khong load duoc thong tin du lieu khuyen mai");
                    joReply.putString(StringConstUtil.DESCRIPTION, "Khong co thong tin");
                    joReply.putNumber(StringConstUtil.ERROR, -2);
                    callback.handle(joReply);
                }
            }
        });
    }

    private void giveBonusMoneyForStore(final String agent, final long value_of_money, final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.setPhoneNumber(phoneNumber);
        log.add("desc", "getFeeFromStore");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = "dgd_new_open";
        Misc.adjustment(vertx, agent, phoneNumber, value_of_money,
                Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("desc", "core tra ket qua");
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            log.add("core tra loi ", soapObjReply.error);
                            log.add("status ", soapObjReply.status);
                            log.add("tid", soapObjReply.tranId);
                            log.add("desc", "core tra loi");
                            log.writeLog();
                            JsonObject jsonReply = new JsonObject();
                            jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "Khong thu duoc tien khach hang, core tra loi");
                            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                            callback.handle(jsonReply);
                            return;
                        }
                        //Yeu cau xong tien mat
                        //Luu qua trong Db va ban noti thong bao
//                        giveMoney(message, zaloTetPromotionObj, log, soapObjReply, jsonReply);
                        jsonReply.putNumber(StringConstUtil.ERROR, 0);
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        callback.handle(jsonReply);
                    }
                });
    }


    private void getFeeFromStore(final String agent, final long value_of_money, final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.setPhoneNumber(phoneNumber);
        log.add("desc", "getFeeFromStore");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = "dgd_new_open";
        Misc.adjustment(vertx, phoneNumber, agent, value_of_money,
                Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("desc", "core tra ket qua");
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            log.add("core tra loi ", soapObjReply.error);
                            log.add("status ", soapObjReply.status);
                            log.add("tid", soapObjReply.tranId);
                            log.add("desc", "core tra loi");
                            log.writeLog();
                            JsonObject jsonReply = new JsonObject();
                            jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "Khong thu duoc tien khach hang, core tra loi");
                            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                            callback.handle(jsonReply);
                            return;
                        }
                        //Yeu cau xong tien mat
                        //Luu qua trong Db va ban noti thong bao
//                        giveMoney(message, zaloTetPromotionObj, log, soapObjReply, jsonReply);
                        jsonReply.putNumber(StringConstUtil.ERROR, 0);
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        callback.handle(jsonReply);
                    }
                });
    }

    private void fireMoneyNotiAndSendTranHist(final String phoneNumber, final long amount
            , final long tranId
            , final Common.BuildLog log
            , final String titleNoti
            , final String bodyNoti
            , final String bodyTrans
            , final String partnerName
            , final int io)
    {
        log.add("desc", "fireMoneyNotiAndSendTranHist");

        JsonObject joTranHis = new JsonObject();
        joTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.FEE_VALUE);
        joTranHis.putString(colName.TranDBCols.COMMENT, bodyTrans);
        joTranHis.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        joTranHis.putNumber(colName.TranDBCols.AMOUNT, amount);
        joTranHis.putNumber(colName.TranDBCols.STATUS, 4);
        joTranHis.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        joTranHis.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
        joTranHis.putString(colName.TranDBCols.BILL_ID, "");
        joTranHis.putString(StringConstUtil.HTML, "");
        joTranHis.putNumber(colName.TranDBCols.IO, io);
        Misc.sendingStandardTransHisFromJson(vertx, tranDb, joTranHis, new JsonObject());

        JsonObject joNoti = new JsonObject();
        joNoti.putString(StringConstUtil.StandardNoti.CAPTION, titleNoti);
        joNoti.putString(StringConstUtil.StandardNoti.BODY, bodyNoti);
        joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phoneNumber);
        joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tranId);
        Misc.sendStandardNoti(vertx, joNoti);

    }
}
