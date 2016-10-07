package com.mservice.momo.vertx.visampointpromo;

import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.NotificationUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.GiftProcess;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by concu on 5/21/15.
 */
public class VisaMpointPromoVerticle extends Verticle {

    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private TransDb tranDb;
    private JsonObject billpayPromoCfg;
    private AgentsDb agentsDb;
    private GiftProcess giftProcess;
    private Common common;
    private VisaMpointPromoDb visaMpointPromoDb;
    private long totalMoney;
    private int percent;
    private String agent;
    private VisaMpointErrorDb visaMpointErrorDb;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.common = new Common(vertx, logger, container.config());
        this.glbCfg = container.config();
        final JsonObject visaMpointCfg = glbCfg.getObject("visampointpromo", new JsonObject());

        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        this.agentsDb = new AgentsDb(vertx.eventBus(), logger);
        this.giftManager = new GiftManager(vertx, logger, glbCfg);
        this.giftProcess = new GiftProcess(common, vertx, logger, glbCfg);
        this.visaMpointPromoDb = new VisaMpointPromoDb(vertx, logger);
        this.visaMpointErrorDb = new VisaMpointErrorDb(vertx, logger);

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final VisaMpointPromoObj visaMpointPromoObj = new VisaMpointPromoObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(visaMpointPromoObj.phoneNumber);
                log.add("", visaMpointPromoObj.phoneNumber);
                log.add("trantype", visaMpointPromoObj.tranType);
                log.add("tranid", visaMpointPromoObj.tranId);
                log.add("cardnumber", visaMpointPromoObj.cardnumber);
                log.add("amount", visaMpointPromoObj.visaAmount);
                logger.info("tranIdVisa is " + visaMpointPromoObj.visatranId);


                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        final JsonObject joReply = new JsonObject();
                        JsonArray array = json.getArray("array", null);
                        log.add("func", "requestPromoRecord");
                        if (array != null && array.size() > 0) {
                            ArrayList<PromotionDb.Obj> objs = new ArrayList<>();
                            PromotionDb.Obj obj_promo = null;
                            JsonObject jsonTime = new JsonObject();
                            long visaStartTime = 0;
                            long visaEndTime = 0;
                            for (Object o : array) {
//                                        objs.add(new PromotionDb.Obj((JsonObject) o));
                                obj_promo = new PromotionDb.Obj((JsonObject) o);
                                if (obj_promo.NAME.equalsIgnoreCase(VisaMpointPromoConst.VISA_PROMO)) {
                                    visaStartTime = obj_promo.DATE_FROM;
                                    visaEndTime = obj_promo.DATE_TO;
                                    break;
                                }
                            }

                            long currentTime = System.currentTimeMillis();

                            if (currentTime < visaStartTime || currentTime > visaEndTime) {
                                //Het thoi han khuyen mai
                                log.add("error", 1000);
                                log.add("desc", "Da het thoi gian khuyen mai");

                                joReply.putNumber("error", 1000);
                                joReply.putString("desc", "Da het thoi gian khuyen mai");
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }


                            JsonObject jsonSearch = new JsonObject();
                            jsonSearch.putString(colName.VisaMPointPromo.NUMBER, visaMpointPromoObj.phoneNumber);
                            //jsonSearch.putBoolean(colName.VisaMPointPromo.END_MONTH, false);
                            final long startTime = visaStartTime;
                            final long endTime = visaEndTime;
                            visaMpointPromoDb.searchWithFilter(jsonSearch, new Handler<ArrayList<VisaMpointPromoDb.Obj>>() {
                                @Override
                                public void handle(final ArrayList<VisaMpointPromoDb.Obj> objs) {

                                    final JsonObject jsonReply = new JsonObject();
                                    ArrayList<VisaMpointPromoDb.Obj> objs_tmp = new ArrayList<VisaMpointPromoDb.Obj>();
                                    if (objs == null) //Loi khong biet vi sao loi
                                    {
                                        log.add("error", 1000);
                                        log.add("desc", "Loi objs bi null, check lai code");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Loi objs bi null, check lai code");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }
                                    objs_tmp = (ArrayList<VisaMpointPromoDb.Obj>) objs.clone();
                                    // Chua co record nao hoac co record ma da update het thang
                                    if (objs != null && objs.size() > 0) {
                                        //Loi khong update, yeu cau backend update
//                                            log.add("error", 1000);
//                                            log.add("desc", "Loi code, backend khong update record");
//                                            jsonReply.putNumber("error", 1000);
//                                            jsonReply.putString("desc", "Loi code, backend khong update record");
//                                            message.reply(jsonReply);
//                                            log.writeLog();
//                                            return;
                                        for (VisaMpointPromoDb.Obj visaObj : objs_tmp) {
                                            if (visaObj.end_month) {
                                                if (visaObj.time_1 < startTime || visaObj.time_2 < startTime) {
                                                    objs.remove(visaObj);
                                                } else {
                                                    //Da nhan duoc day du point => khong tra thuong nua ....
                                                    log.add("error", 1000);
                                                    log.add("desc", "Da nhan day du qua roi");
                                                    jsonReply.putNumber("error", 1000);
                                                    jsonReply.putString("desc", "Da nhan day du qua roi");
                                                    message.reply(jsonReply);
                                                    log.writeLog();
                                                    return;
                                                }
                                            }
                                        }

                                    }
                                    if (objs != null && objs.size() == 0) {
                                        JsonObject checkCardNumberJson = new JsonObject();
                                        checkCardNumberJson.putString(colName.VisaMPointPromo.CARD_NUMBER, visaMpointPromoObj.cardnumber);
                                        visaMpointPromoDb.searchWithFilter(checkCardNumberJson, new Handler<ArrayList<VisaMpointPromoDb.Obj>>() {
                                            @Override
                                            public void handle(ArrayList<VisaMpointPromoDb.Obj> checkCardNumberObjs) {

//                                                    if (visaMpointCfg == null) {
//                                                        //Loi he thong
//                                                        log.add("error", 1000);
//                                                        log.add("desc", "Loi khong load duoc file config");
//                                                        jsonReply.putNumber("error", 1000);
//                                                        jsonReply.putString("desc", "Loi khong load duoc file config");
//                                                        message.reply(jsonReply);
//                                                        log.writeLog();
//                                                        return;
//                                                    }
                                                totalMoney = visaMpointCfg.getLong("total", 30000);
                                                percent = visaMpointCfg.getInteger("percent", 10);
                                                agent = visaMpointCfg.getString("agent", "visa_promo");

                                                if (checkCardNumberObjs == null) {
                                                    log.add("error", 1000);
                                                    log.add("desc", "checkCardNumber == null");
                                                    jsonReply.putNumber("error", 1000);
                                                    jsonReply.putString("desc", "Loi he thong");
                                                    message.reply(jsonReply);
                                                    log.writeLog();
                                                    return;
                                                } else if (checkCardNumberObjs != null && checkCardNumberObjs.size() > 0) {
                                                    if (checkCardNumberObjs.get(0).number.equalsIgnoreCase(visaMpointPromoObj.phoneNumber)) {
                                                        VisaMpointPromoDb.Obj obj = new VisaMpointPromoDb.Obj();
                                                        obj.number = visaMpointPromoObj.phoneNumber;
                                                        obj.card_number = visaMpointPromoObj.cardnumber;
                                                        obj.time_1 = System.currentTimeMillis();
                                                        obj.time_2 = 0;
                                                        obj.tid_1 = visaMpointPromoObj.tranId;
                                                        obj.tid_2 = 0;
                                                        obj.trantype_1 = visaMpointPromoObj.tranType;
                                                        obj.trantype_2 = 0;
                                                        obj.end_month = false;
                                                        obj.mpoint_1 = calculateMpoint(visaMpointPromoObj.visaAmount, totalMoney);
                                                        obj.mpoint_2 = 0;
                                                        obj.promo_count = 1;
                                                        obj.tid_visa_1 = visaMpointPromoObj.visatranId;

                                                        obj.service_id_1 = visaMpointPromoObj.serviceId;
                                                        obj.total_amount_1 = visaMpointPromoObj.totalAmount;
                                                        obj.cashin_amount_1 = visaMpointPromoObj.visaAmount;

                                                        obj.cashinTime_1 = visaMpointPromoObj.cashinTime;

                                                        givePoint1(agent, log, obj, visaMpointPromoObj, message);
                                                        log.writeLog();
                                                        return;
                                                    } else {
                                                        log.add("error", 1000);
                                                        log.add("desc", "Ban da the nay voi mot vi khac, vui long map lai");
                                                        jsonReply.putNumber("error", 1000);
                                                        jsonReply.putString("desc", "Ban da the nay voi mot vi khac, vui long map lai");
                                                        message.reply(jsonReply);
                                                        log.writeLog();
                                                        return;
                                                    }
                                                } else if (checkCardNumberObjs != null && checkCardNumberObjs.size() == 0) {
                                                    VisaMpointPromoDb.Obj obj = new VisaMpointPromoDb.Obj();
                                                    obj.number = visaMpointPromoObj.phoneNumber;
                                                    obj.card_number = visaMpointPromoObj.cardnumber;
                                                    obj.time_1 = System.currentTimeMillis();
                                                    obj.time_2 = 0;
                                                    obj.tid_1 = visaMpointPromoObj.tranId;
                                                    obj.tid_2 = 0;
                                                    obj.trantype_1 = visaMpointPromoObj.tranType;
                                                    obj.trantype_2 = 0;
                                                    obj.end_month = false;
                                                    obj.mpoint_1 = calculateMpoint(visaMpointPromoObj.visaAmount, totalMoney);
                                                    obj.mpoint_2 = 0;
                                                    obj.promo_count = 1;
                                                    obj.tid_visa_1 = visaMpointPromoObj.visatranId;

                                                    obj.service_id_1 = visaMpointPromoObj.serviceId;
                                                    obj.total_amount_1 = visaMpointPromoObj.totalAmount;
                                                    obj.cashin_amount_1 = visaMpointPromoObj.visaAmount;

                                                    obj.cashinTime_1 = visaMpointPromoObj.cashinTime;

                                                    givePoint1(agent, log, obj, visaMpointPromoObj, message);
                                                    log.writeLog();
                                                    return;
                                                } else {
                                                    log.add("error", 1000);
                                                    log.add("desc", "checkCardNumber == null");
                                                    jsonReply.putNumber("error", 1000);
                                                    jsonReply.putString("desc", "Loi he thong");
                                                    message.reply(jsonReply);
                                                    log.writeLog();
                                                    return;
                                                }
                                            }
                                        });
                                        return;
                                    }


                                    //Lay thoi gian dau tien cua thang hien tai.
//                                        Calendar calendar = Calendar.getInstance();
//                                        long currentTime = System.currentTimeMillis();
//                                        int month = calendar.get(Calendar.MONTH);
//                                        int year = calendar.get(Calendar.YEAR);
//
//                                        calendar.set(year, month, 1, 0, 0, 0);
//                                        long startMonth = calendar.getTimeInMillis();

                                    if (objs.get(0) == null) {
                                        log.add("error", 1000);
                                        log.add("desc", "Loi objs.get(0) bi null, check lai code");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Loi objs.get(0) bi null, check lai code");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }

                                    if (objs.get(0).tid_visa_1 == visaMpointPromoObj.visatranId || objs.get(0).tid_visa_2 == visaMpointPromoObj.visatranId) {
                                        log.add("error", 1000);
                                        log.add("desc", "Da tra thuong, khong tra nua");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Da tra thuong, khong tra nua");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }

                                    if (!visaMpointPromoObj.cardnumber.equalsIgnoreCase(objs.get(0).card_number)) {
                                        log.add("error", 1000);
                                        log.add("desc", "Loi mapping 1 the khac vao vi, khong tra thuong cho ku");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Loi mapping 1 the khac vao vi, khong tra thuong cho ku");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }

                                    if (objs.get(0).time_1 != 0 && objs.get(0).time_2 != 0) {
                                        log.add("error", 1000);
                                        log.add("desc", "Nhan du so lan tra thuong");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Nhan du so lan tra thuong");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }

                                    if (objs.get(0).promo_count > 1) {
                                        //Nhan roi nhan hoai, tham qua
                                        log.add("error", 1000);
                                        log.add("desc", "Da nhan day du qua thuong");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Da nhan day du qua thuong");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;

                                    }
                                    long point1 = objs.get(0).mpoint_1;
                                    long point2 = objs.get(0).mpoint_2;

//                                        if (point1 > 29999) {
//                                            //Nhan day du roi, khong tra nua.
//                                            log.add("error", 1000);
//                                            log.add("desc", "Da nhan day du qua thuong");
//                                            jsonReply.putNumber("error", 1000);
//                                            jsonReply.putString("desc", "Da nhan day du qua thuong");
//                                            message.reply(jsonReply);
//                                            return;
//                                        }

                                    if (point1 != 0 && point2 != 0) {
                                        //Nhan day du roi, khong tra nua.
                                        log.add("error", 1000);
                                        log.add("desc", "Da nhan day du qua thuong");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Da nhan day du qua thuong");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }


                                    if (objs.get(0).time_1 < startTime) {
                                        //todo Update lai thanh true.
                                        JsonObject jsonUpdate = new JsonObject();
                                        jsonUpdate.putBoolean(colName.VisaMPointPromo.END_MONTH, true);
                                        visaMpointPromoDb.updatePartial(visaMpointPromoObj.phoneNumber, jsonUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {

                                            }
                                        });
                                        VisaMpointPromoDb.Obj obj = new VisaMpointPromoDb.Obj();
                                        obj.number = visaMpointPromoObj.phoneNumber;
                                        obj.card_number = visaMpointPromoObj.cardnumber;
                                        obj.time_1 = System.currentTimeMillis();
                                        obj.time_2 = 0;
                                        obj.tid_1 = visaMpointPromoObj.tranId;
                                        obj.tid_2 = 0;
                                        obj.trantype_1 = visaMpointPromoObj.tranType;
                                        obj.trantype_2 = 0;
                                        obj.end_month = false;
                                        obj.mpoint_1 = calculateMpoint(visaMpointPromoObj.visaAmount, totalMoney);
                                        obj.mpoint_2 = 0;
                                        obj.promo_count = 1;
                                        obj.tid_visa_1 = visaMpointPromoObj.visatranId;

                                        //
                                        obj.service_id_1 = visaMpointPromoObj.serviceId;
                                        obj.total_amount_1 = visaMpointPromoObj.totalAmount;
                                        obj.cashin_amount_1 = visaMpointPromoObj.visaAmount;

                                        obj.cashinTime_1 = visaMpointPromoObj.cashinTime;

                                        givePoint1(agent, log, obj, visaMpointPromoObj, message);
                                        //Tao record cho thang moi.
                                        log.writeLog();
                                        return;
                                    }

//                                        if (point1 == 0) {
//                                            // Tra thuong vao point 1
//                                            point1 = calculateMpoint(visaMpointPromoObj.amount, totalMoney);
//                                            VisaMpointPromoDb.Obj obj = new VisaMpointPromoDb.Obj();
//                                            obj.number = visaMpointPromoObj.phoneNumber;
//                                            obj.card_number = visaMpointPromoObj.cardnumber;
//                                            obj.time_1 = System.currentTimeMillis();
//                                            obj.time_2 = 0;
//                                            obj.tid_1 = visaMpointPromoObj.tranId;
//                                            obj.tid_2 = 0;
//                                            obj.trantype_1 = visaMpointPromoObj.tranType;
//                                            obj.trantype_2 = 0;
//                                            obj.end_month = false;
//                                            obj.mpoint_1 = point1;
//                                            obj.mpoint_2 = 0;
//                                            obj.promo_count = 1;
//                                            givePoint1(agent, log, obj, visaMpointPromoObj, message);
//                                        }
//                                        else
                                    if (point2 == 0) {
                                        //Se nhan qua 2.
                                        point2 = calculateMpoint(visaMpointPromoObj.visaAmount, totalMoney);
                                        givePoint2(agent, point2, log, visaMpointPromoObj, message);
                                        log.writeLog();
                                    }
                                    log.writeLog();
                                }
                            });//end
                        }
                    }
                });
            }

        };
        vertx.eventBus().registerLocalHandler(AppConstant.VISA_MPOINT_BUSS_ADDRESS, myHandler);

    }


    public void givePoint1(final String fromAgent, final Common.BuildLog log, final VisaMpointPromoDb.Obj dbObj, final VisaMpointPromoObj requestObj, final Message<JsonObject> message) {
        log.add("func", "givePoint1");
        Misc.adjustment(vertx, fromAgent, requestObj.phoneNumber, dbObj.mpoint_1, Core.WalletType.POINT_VALUE, new ArrayList<Misc.KeyValue>(), new Common.BuildLog(logger), new Handler<Common.SoapObjReply>() {
            @Override
            public void handle(Common.SoapObjReply coreReply) {
                //ClaimHistory history = new ClaimHistory(fromAgent, requestObj.phoneNumber, coreReply.error, pointAmount, coreReply.tranId, null, code);
                //history.comment = "transferPoint";
                //claimHistoryDb.save(history, null);
                log.add("func", "adjustment");
                log.add("error", coreReply.error);
                log.add("tranId", coreReply.tranId);
//                log.writeLog();
                JsonObject jsonRep = new JsonObject();
                if (coreReply.error != 0) {
                    log.add("error", coreReply.error);
                    log.add("amount", coreReply.amount);
                    log.add("desc", "Loi tra ve tu core");
                    jsonRep.putNumber("error", coreReply.error);
                    jsonRep.putNumber("amount", coreReply.amount);
                    jsonRep.putString("desc", "Loi tra ve tu core");
                    message.reply(jsonRep);
                    VisaMpointErrorDb.Obj vsErrorObj = new VisaMpointErrorDb.Obj();
                    vsErrorObj.cardnumber = requestObj.cardnumber;
                    vsErrorObj.number = requestObj.phoneNumber;
                    vsErrorObj.tranid = requestObj.tranId;
                    vsErrorObj.trantype = requestObj.tranType;
                    vsErrorObj.error = coreReply.error;
                    vsErrorObj.desc_error = "Loi tra ve tu core";
                    vsErrorObj.time = System.currentTimeMillis();
                    vsErrorObj.count = 1;
                    visaMpointErrorDb.insert(vsErrorObj, new Handler<Integer>() {
                        @Override
                        public void handle(Integer integer) {

                        }
                    });
                    log.writeLog();
                    return;
                }

                visaMpointPromoDb.insert(dbObj, new Handler<Integer>() {
                    @Override
                    public void handle(Integer integer) {

                    }
                });

                String notiCaption = "Nhận tiền khuyến mãi ";
                String notiBody = "Quý khách vừa nhận được " + NotificationUtils.getAmount(dbObj.mpoint_1) + "đ vào Tài khoản KM khi thực hiện thanh toán bằng thẻ Visa. Quý khách sẽ sử dụng được tiền trong Tài khoản KM khi tích lũy đủ 50.000đ. Chi tiết liên hệ (08 39917199 hoặc http: momo.vn/thanhtoanhoadon";
                final Notification noti = new Notification();
                noti.priority = 2;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
                noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                noti.tranId = coreReply.tranId;
                noti.time = new Date().getTime();

                noti.receiverNumber = DataUtil.strToInt(requestObj.phoneNumber);
                Misc.sendNoti(vertx, noti);
                log.writeLog();

//                common.sendCurrentAgentInfo(vertx, sock, 0, msg.cmdPhone, data);
                jsonRep.putNumber(StringConstUtil.TRANDB_TRAN_ID, coreReply.tranId);
                jsonRep.putNumber("error", coreReply.error);
                jsonRep.putString("desc", "Thanh cong");
                logger.info("error is " + coreReply.error);
                logger.info("desc is Thanh cong");
                message.reply(jsonRep);
                return;
            }
        });
    }


    public void givePoint2(final String fromAgent, final long pointAmount, final Common.BuildLog log, final VisaMpointPromoObj requestObj, final Message<JsonObject> message) {
        log.add("func", "givePoint2");
        Misc.adjustment(vertx, fromAgent, requestObj.phoneNumber, pointAmount, Core.WalletType.POINT_VALUE, new ArrayList<Misc.KeyValue>(), new Common.BuildLog(logger), new Handler<Common.SoapObjReply>() {
            @Override
            public void handle(Common.SoapObjReply coreReply) {
                log.add("func", "adjustment");
                log.add("error", coreReply.error);
                log.add("tranId", coreReply.tranId);
//                log.writeLog();
                JsonObject jsonRep = new JsonObject();
                if (coreReply.error != 0) {


                    log.add("error", coreReply.error);
                    log.add("amount", coreReply.amount);
                    log.add("desc", "Loi tra ve tu core");
                    jsonRep.putNumber("error", coreReply.error);
                    jsonRep.putNumber("amount", coreReply.amount);
                    jsonRep.putString("desc", "Loi tra ve tu core");
                    VisaMpointErrorDb.Obj vsErrorObj = new VisaMpointErrorDb.Obj();
                    vsErrorObj.cardnumber = requestObj.cardnumber;
                    vsErrorObj.number = requestObj.phoneNumber;
                    vsErrorObj.tranid = requestObj.tranId;
                    vsErrorObj.trantype = requestObj.tranType;
                    vsErrorObj.error = coreReply.error;
                    vsErrorObj.desc_error = "Loi tra ve tu core";
                    vsErrorObj.time = System.currentTimeMillis();
                    vsErrorObj.count = 2;
                    visaMpointErrorDb.insert(vsErrorObj, new Handler<Integer>() {
                        @Override
                        public void handle(Integer integer) {

                        }
                    });
                    message.reply(jsonRep);
                    log.writeLog();
                    return;
                }

//                VisaMpointPromoDb.Obj obj = new VisaMpointPromoDb.Obj();
//                obj.mpoint_2 = pointAmount;
//                obj.tid_2 = requestObj.tranId;
//                obj.trantype_2 = requestObj.tranType;
//                obj.end_month = false;
//                obj.promo_count = 2;
//                obj.time_2 = System.currentTimeMillis();
                JsonObject joUpdate = new JsonObject();
                joUpdate.putNumber(colName.VisaMPointPromo.MPOINT_2, pointAmount);
                joUpdate.putNumber(colName.VisaMPointPromo.TID_2, requestObj.tranId);
                joUpdate.putNumber(colName.VisaMPointPromo.TRANTYPE_2, requestObj.tranType);
                joUpdate.putNumber(colName.VisaMPointPromo.PROMO_COUNT, 2);
                joUpdate.putBoolean(colName.VisaMPointPromo.END_MONTH, true);
                joUpdate.putNumber(colName.VisaMPointPromo.TIME_2, System.currentTimeMillis());
                joUpdate.putNumber(colName.VisaMPointPromo.TID_VISA_2, requestObj.visatranId);

                joUpdate.putString(colName.VisaMPointPromo.SERVICE_ID_2, requestObj.serviceId);
                joUpdate.putNumber(colName.VisaMPointPromo.TOTAL_AMOUNT_2, requestObj.totalAmount);
                joUpdate.putNumber(colName.VisaMPointPromo.CASH_IN_AMOUNT_2, requestObj.visaAmount);

                joUpdate.putNumber(colName.VisaMPointPromo.CASH_IN_TIME_2, requestObj.cashinTime);


                visaMpointPromoDb.updatePartial(requestObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {

                    }
                });


                String notiCaption = "Nhận tiền khuyến mãi ";
                String notiBody = "Quý khách vừa nhận được " + NotificationUtils.getAmount(pointAmount) + "đ vào Tài khoản KM khi thực hiện thanh toán bằng thẻ Visa. Quý khách sẽ sử dụng được tiền trong Tài khoản KM khi tích lũy đủ 50.000đ. Chi tiết liên hệ (08 39917199 hoặc http: momo.vn/thanhtoanhoadon";
                final Notification noti = new Notification();
                noti.priority = 2;
                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
                noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                noti.tranId = coreReply.tranId;
                noti.time = new Date().getTime();
//                noti.extra = new JsonObject()
//                        .putString("giftId", gift.getModelId())
//                        .putString("giftTypeId", gift.typeId)
//                        .putString("amount", String.valueOf(tranAmount))
//                        .putString("sender", "Trải nghiệm thanh toán")
//                        .putString("senderName", "MoMo")
//                        .putString("msg",giftMessage)
//                        .putNumber("status", gift.status)
//                        .putString("serviceid", gift.typeId)
//                        .toString();

                noti.receiverNumber = DataUtil.strToInt(requestObj.phoneNumber);

                Misc.sendNoti(vertx, noti);
                log.writeLog();
                jsonRep.putNumber(StringConstUtil.TRANDB_TRAN_ID, coreReply.tranId);
                jsonRep.putNumber("error", coreReply.error);
                jsonRep.putString("desc", "Thanh cong");
                logger.info("error is " + coreReply.error);
                logger.info("desc is Thanh cong");
                message.reply(jsonRep);
                return;
            }
        });
    }

    public long calculateMpoint(long amount, long totalBalance) {

        long mpoint = 0;

        long mpoint_tmp = (long) (Math.ceil(percent * 0.01 * amount));

        if (mpoint_tmp > totalBalance) {
            mpoint = totalBalance;
        } else {
            mpoint = mpoint_tmp;
        }

        return mpoint;
    }
}
