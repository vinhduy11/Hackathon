package com.mservice.momo.vertx.billpaypromo;

import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.gateway.internal.connectorproxy.ConnectorCommon;
import com.mservice.momo.gateway.internal.db.oracle.DBProcess;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.visamaster.VMRequest;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.proxy.entity.ProxyRequest;
import com.mservice.proxy.entity.ProxyResponse;
import com.mservice.visa.entity.CardInfo;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by concu on 5/11/15.
 */
public class BillPayPromoVerticle extends Verticle {

    long time20days = 1000L * 60 * 60 * 24 * 20;
    JsonObject sbsCfg;
    private BillPayPromosDb billPayPromosDb = null;
    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private TransDb tranDb;
    private JsonObject billpayPromoCfg;
    private AgentsDb agentsDb;
    private BillPayPromoErrorDb billPayPromoErrorDb;
    private DBProcess dbProcess;
    private GiftDb giftDb;
    private PhonesDb phonesDb;
    private Card card;
    private JsonObject jsonOcbPromo;
    private MappingWalletOCBPromoDb mappingWalletOCBPromoDb;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.sbsCfg = glbCfg.getObject("cybersource", null);
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        this.agentsDb = new AgentsDb(vertx.eventBus(), logger);
        this.giftManager = new GiftManager(vertx, logger, glbCfg);
        this.billPayPromosDb = new BillPayPromosDb(vertx, logger);
        billpayPromoCfg = glbCfg.getObject("billpaypromo", null);
        this.billPayPromoErrorDb = new BillPayPromoErrorDb(vertx, logger);
        this.giftDb = new GiftDb(vertx, logger);
        this.card = new Card(vertx.eventBus(), logger);
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);
        jsonOcbPromo = glbCfg.getObject(StringConstUtil.OBCPromo.JSON_OBJECT, new JsonObject());
        mappingWalletOCBPromoDb = new MappingWalletOCBPromoDb(vertx, logger);
        //String vinaProxyVerticle = "vinaphoneProxyVerticle";
        //Khong load duoc agent tra thuong


//         vertx.setPeriodic(5*60*1000L, new Handler<Long>() {
        //test, 15 phut kiem tra 1 lan
        vertx.setPeriodic(12 * 60 * 60 * 1000L, new Handler<Long>() { //production 1 ngay kiem tra 1 lan
            @Override
            public void handle(Long aLong) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                final long startDate = calendar.getTimeInMillis();
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                final long endDate = calendar.getTimeInMillis();
                final int timeForGift = billpayPromoCfg.getInteger("timeforgift", 30);
                billPayPromosDb.findGiftNeedToActive(startDate, endDate, new Handler<ArrayList<BillPayPromosDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<BillPayPromosDb.Obj> objs) {
                        if (objs != null && objs.size() > 0) {
                            for (final BillPayPromosDb.Obj obj : objs) {

                                if (obj.activatedTime2 >= startDate && obj.activatedTime2 <= endDate) {
                                    giftManager.useGift(obj.number, obj.giftId2, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            if (jsonObject != null) {
                                                int error = jsonObject.getInteger("error", -1);
                                                if (error == 0) {
                                                    Common.ServiceReq serviceReq = new Common.ServiceReq();
                                                    serviceReq.ServiceId = obj.serviceId1;
                                                    serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
                                                    Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
                                                        @Override
                                                        public void handle(JsonArray objects) {
                                                            //Ban noti.
                                                            long tranId = 0;

                                                            long endTime = obj.activatedTime2 + (1000L * 60 * 60 * 24 * timeForGift);
                                                            final Notification noti = new Notification();
                                                            noti.priority = 2;
                                                            noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                                            noti.caption = PromoContentNotification.BILL_PAY_PROMO_ACTIVATED_GIFT_NOTI_TITLE;// "Nhận thưởng quà khuyến mãi";
                                                            noti.body = String.format(PromoContentNotification.BILL_PAY_PROMO_ACTIVATED_GIFT_NOTI_BODY, ((JsonObject) objects.get(0)).getString(colName.ServiceCols.SERVICE_NAME, ""), Misc.dateVNFormatWithDot(endTime));//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                                                            noti.tranId = tranId;
                                                            noti.time = new Date().getTime();
                                                            noti.receiverNumber = DataUtil.strToInt(obj.number);
                                                            Misc.sendNoti(vertx, noti);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });//END GIFT MANAGER
                                }
                                if (obj.activatedTime4 >= startDate && obj.activatedTime4 <= endDate) {

                                    giftManager.useGift(obj.number, obj.giftId4, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            if (jsonObject != null) {
                                                int error = jsonObject.getInteger("error", -1);
                                                if (error == 0) {
                                                    Common.ServiceReq serviceReq = new Common.ServiceReq();
                                                    serviceReq.ServiceId = obj.serviceId2;
                                                    serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
                                                    Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
                                                        @Override
                                                        public void handle(JsonArray objects) {
                                                            //Ban noti.
                                                            long tranId = 0;

                                                            long endTime = obj.activatedTime4 + (1000L * 60 * 60 * 24 * timeForGift);
                                                            final Notification noti = new Notification();
                                                            noti.priority = 2;
                                                            noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                                            noti.caption = PromoContentNotification.BILL_PAY_PROMO_ACTIVATED_GIFT_NOTI_TITLE;// "Nhận thưởng quà khuyến mãi";
                                                            noti.body = String.format(PromoContentNotification.BILL_PAY_PROMO_ACTIVATED_GIFT_NOTI_BODY, ((JsonObject) objects.get(0)).getString(colName.ServiceCols.SERVICE_NAME, ""), Misc.dateVNFormatWithDot(endTime));//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                                                            noti.tranId = tranId;
                                                            noti.time = new Date().getTime();
                                                            noti.receiverNumber = DataUtil.strToInt(obj.number);
                                                            Misc.sendNoti(vertx, noti);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });//END GIFT MANAGER
                                }
                            }
                        }
                    }
                });
            }
        });

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final BillPayPromoObj billPayPromoObj = new BillPayPromoObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(billPayPromoObj.phoneNumber);
                log.add("phoneNumber", billPayPromoObj.phoneNumber);
                log.add("trantype", billPayPromoObj.tranType);
                log.add("tranid", billPayPromoObj.tranId);
                log.add("source", billPayPromoObj.source);
                log.add("serviceId", billPayPromoObj.serviceId);

                final JsonObject jsonReply = new JsonObject();

                if (billPayPromoObj.phoneNumber.equalsIgnoreCase("") || billPayPromoObj.serviceId.equalsIgnoreCase("")
                        || DataUtil.strToLong(billPayPromoObj.phoneNumber) <= 0) {
                    log.add("desc", "So dien thoai la so dien thoai khong co that.");
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Giao dich loi");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }

                agentsDb.getOneAgent(billPayPromoObj.phoneNumber, "BillPayPromoVerticle", new Handler<AgentsDb.StoreInfo>() {
                    @Override
                    public void handle(AgentsDb.StoreInfo storeInfo) {
                        if (storeInfo != null && storeInfo.status != 2) {
                            log.add("desc", "So dien thoai la diem giao dich.");
                            jsonReply.putNumber("error", 1000);
                            jsonReply.putString("desc", "Chương trình này không áp dụng cho số điện thoại là điểm giao dịch");
                            message.reply(jsonReply);
                            log.writeLog();
                            return;
                        }
                        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject json) {
                                JsonArray array = json.getArray("array", null);
                                log.add("phoneNumber", billPayPromoObj.phoneNumber);
                                log.add("serviceId", billPayPromoObj.serviceId);
                                log.add("source", billPayPromoObj.source);
                                log.add("tranType", billPayPromoObj.tranType);
                                log.add("tranId", billPayPromoObj.tranId);

                                if (billpayPromoCfg == null) {
                                    jsonReply.putNumber("error", 1000);
                                    jsonReply.putString("desc", "Hệ thống đang bảo trì. Chúng tôi đã ghi nhận bạn tham gia chương trình và sẽ phản hồi trong thời gian sớm nhất. Chi tiết liên hệ: (08)39917199");
                                    log.add("desc", "Khong lay duoc thong tin agent");
                                    message.reply(jsonReply);
                                    return;
                                }

                                final String vinaProxyVerticle = billpayPromoCfg.getString("vina_proxy_verticle", "vinaphoneProxyVerticle");

                                final String providerNetworkMobile = DataUtil.phoneProviderName(billPayPromoObj.phoneNumber);
                                log.add("providerNetworkMobile", providerNetworkMobile);
                                long ocb_promo_start_date = 0;
                                long ocb_promo_end_date = 0;
                                long mgd_promo_start_date = 0;
                                long mgd_promo_end_date = 0;
                                long currentTime = System.currentTimeMillis();
                                if (array != null && array.size() > 0) {
                                    ArrayList<PromotionDb.Obj> objs = new ArrayList<>();
                                    PromotionDb.Obj obj_promo = null;
                                    JsonObject jsonTime = new JsonObject();
                                    for (Object o : array) {
//                                        objs.add(new PromotionDb.Obj((JsonObject) o));
                                        obj_promo = new PromotionDb.Obj((JsonObject) o);
                                        if (obj_promo.NAME.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO)) {
                                            mgd_promo_start_date = obj_promo.DATE_FROM;
                                            mgd_promo_end_date = obj_promo.DATE_TO;
                                        } else if (obj_promo.NAME.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO_PROGRAM)) {
                                            ocb_promo_start_date = obj_promo.DATE_FROM;
                                            ocb_promo_end_date = obj_promo.DATE_TO;
                                        }
                                    }

                                    // Nhap source tu admin.
                                    if (billPayPromoObj.source.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO)
                                            && currentTime >= ocb_promo_start_date && currentTime <= ocb_promo_end_date) {
                                        //Kiem tra xem co map vi trong thoi gian khuyen mai khong
                                        String OCB_BANK_CODE = jsonOcbPromo.getString(StringConstUtil.OBCPromo.BANK_CODE, "104");
                                        String ID = billPayPromoObj.phoneNumber + OCB_BANK_CODE;
                                        final long ocb_start_date = ocb_promo_start_date;
                                        final long ocb_end_date = ocb_promo_end_date;
                                        JsonObject jsonFilter = new JsonObject();
                                        jsonFilter.putString(colName.MappingWalletBank.BANK_CODE, StringConstUtil.OBCPromo.BANK_CODE);
                                        jsonFilter.putString(colName.MappingWalletBank.NUMBER, billPayPromoObj.phoneNumber);

                                        mappingWalletOCBPromoDb.findOne(billPayPromoObj.phoneNumber, new Handler<MappingWalletOCBPromoDb.Obj>() {
                                            @Override
                                            public void handle(MappingWalletOCBPromoDb.Obj mappingOCBPromoObj) {
                                                if(mappingOCBPromoObj == null)
                                                {
                                                    //Chu nay khong co trong chuong trinh khuyen mai
                                                    //Khong co map vi OCB
                                                    log.add("desc", "Khong co thong tin trong bang mapping wallet OCB map vi");
                                                    log.writeLog();
                                                    return;
                                                }
                                                else if(mappingOCBPromoObj.mapping_time >= ocb_start_date && mappingOCBPromoObj.mapping_time <= ocb_end_date)
                                                {
                                                    log.add("desc", "Co thong tin trong bang mapping wallet map vi va map vi trong thoi gian khuyen mai");
                                                    promoteGiftForOCB(billPayPromoObj, log, message);
                                                    return;
                                                }
                                                else
                                                {
                                                    log.add("desc", "Co thong tin trong bang mapping wallet map vi nhung map vi ngoai thoi gian khuyen mai.");
                                                    log.writeLog();
                                                    return;
                                                }

                                            }
                                        });
                                        return;
                                    } else if (billPayPromoObj.source.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO)
                                            && currentTime >= mgd_promo_start_date && currentTime <= mgd_promo_end_date) {
                                        if (billPayPromoObj.serviceId.equalsIgnoreCase("vinahcm") && providerNetworkMobile.equalsIgnoreCase("Vinaphone")) {
                                            long timeout = 60000;
                                            ProxyRequest proxyRequest = ConnectorCommon.createCheckInforRequest(
                                                    billPayPromoObj.phoneNumber, "", billPayPromoObj.serviceId, "", billPayPromoObj.phoneNumber);
                                            vertx.eventBus().sendWithTimeout(vinaProxyVerticle, proxyRequest.getJsonObject(), timeout, new Handler<AsyncResult<Message<JsonObject>>>() {
                                                @Override
                                                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                                                    if (messageAsyncResult != null && messageAsyncResult.succeeded()) {
                                                        if (messageAsyncResult.result() != null) {
                                                            JsonObject messReply = messageAsyncResult.result().body();
                                                            ProxyResponse proxyResponse = new ProxyResponse(messReply);
                                                            if (proxyResponse != null && proxyResponse.getProxyResponseCode() == 0 && providerNetworkMobile.equalsIgnoreCase("Vinaphone")) {
                                                                promoteGiftForMGD(billPayPromoObj, log, message);
                                                                return;
                                                            } else {
                                                                log.add("desc", "Rất tiếc, thẻ quà tặng  này chỉ dùng được với thuê bao Vinaphone trả sau.");
                                                                jsonReply.putNumber("error", 1000);
                                                                jsonReply.putString("desc", "Rất tiếc, thẻ quà tặng  này chỉ dùng được với thuê bao Vinaphone trả sau.");
                                                                message.reply(jsonReply);
                                                                log.writeLog();
                                                                return;
                                                            }
                                                        } else {
                                                            jsonReply.putNumber("error", 1000);
                                                            jsonReply.putString("desc", "Hệ thống đang bảo trì. Chúng tôi đã ghi nhận bạn tham gia chương trình và sẽ phản hồi trong thời gian sớm nhất. Chi tiết liên hệ: (08)39917199");
                                                            log.add("desc", "Khong lay duoc thong tin agent");
                                                            message.reply(jsonReply);
                                                            return;
                                                        }
                                                    } else {
                                                        log.add("desc", "Rất tiếc, thẻ quà tặng  này chỉ dùng được với thuê bao Vinaphone trả sau.");
                                                        jsonReply.putNumber("error", 1000);
                                                        jsonReply.putString("desc", "Rất tiếc, thẻ quà tặng  này chỉ dùng được với thuê bao Vinaphone trả sau.");
                                                        message.reply(jsonReply);
                                                        log.writeLog();
                                                        return;
                                                    }
                                                }
                                            });
                                            return;
                                        } else if (billPayPromoObj.serviceId.equalsIgnoreCase("vinahcm") && !providerNetworkMobile.equalsIgnoreCase("Vinaphone")) {
                                            log.add("desc", "Rất tiếc, thẻ quà tặng  này chỉ dùng được với thuê bao Vinaphone trả sau.");
                                            jsonReply.putNumber("error", 1000);
                                            jsonReply.putString("desc", "Rất tiếc, thẻ quà tặng  này chỉ dùng được với thuê bao Vinaphone trả sau.");
                                            message.reply(jsonReply);
                                            log.writeLog();
                                            return;
                                        } else if (billPayPromoObj.serviceId.equalsIgnoreCase(StringConstUtil.VTHN)) {
                                            checkVTHNService(billPayPromoObj, log, message);
                                            log.writeLog();
                                            return;
                                        }
                                        promoteGiftForMGD(billPayPromoObj, log, message);
                                        return;
                                    } else if ((currentTime < ocb_promo_start_date || currentTime > ocb_promo_end_date) &&
                                            billPayPromoObj.source.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO)) {
                                        log.add("desc", "Đã hết thời gian khuyến mãi.");
//                                        jsonReply.putNumber("error", 1000);
//                                        jsonReply.putString("desc", "Đã hết thời gian khuyến mãi.");
//                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    } else if ((currentTime < mgd_promo_start_date || currentTime > mgd_promo_end_date) &&
                                            billPayPromoObj.source.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO)) {
                                        log.add("desc", "Đã hết thời gian khuyến mãi.");
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Đã hết thời gian khuyến mãi.");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }
                                    //B0006E
                                    // Theo luon thanh toan
                                    else {
                                        jsonTime.putNumber(BillPayPromoConst.START_DATE_VCB, ocb_promo_start_date);
                                        jsonTime.putNumber(BillPayPromoConst.END_DATE_VCB, ocb_promo_end_date);
                                        jsonTime.putNumber(BillPayPromoConst.START_DATE_MGD, mgd_promo_start_date);
                                        jsonTime.putNumber(BillPayPromoConst.END_DATE_MGD, mgd_promo_end_date);
                                        log.add("desc", "Luon thanh toan.");
                                        promoteGiftForBillPay(billPayPromoObj, log, jsonTime, message);

                                        log.writeLog();
                                        return;
                                    }
                                }
                            }
                        });
                    }
                });
            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.BILL_PAY_BUSS_ADDRESS, myHandler);
    }

    //This method used to .... "kiem tra dich vu vien thong ha noi xem co thoa dieu kien tra thuong billpay khong"
    public void checkVTHNService(final BillPayPromoObj billPayPromoObj, final Common.BuildLog log, final Message<JsonObject> message) {
        phonesDb.getPhoneObjInfo(DataUtil.strToInt(billPayPromoObj.phoneNumber), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                logger.info(obj);
                JsonObject joReply = new JsonObject();
                if (obj != null) {
                    if (StringConstUtil.VCB_NAME.equalsIgnoreCase(obj.bank_name) && StringConstUtil.VCB_CODE.equalsIgnoreCase(obj.bank_code) && !"".equalsIgnoreCase(obj.bankPersonalId)
                            && obj.bankPersonalId != null) {
                        // Co map the VCB
                        billPayPromoObj.cmnd = obj.bankPersonalId;
                        checkVCBRule(billPayPromoObj, log, message);

                    } else if (StringConstUtil.VIETIN_NAME.equalsIgnoreCase(obj.bank_name) && StringConstUtil.VIETIN_CODE.equalsIgnoreCase(obj.bank_code)) {
                        // Co map the Vietin
                        promoteGiftForMGD(billPayPromoObj, log, message);
                    } else {
                        checkVisaCard(billPayPromoObj, log, message);
                    }
                    return;
                } else {
                    //So nay khong co trong bang phone ... so xao xi tun
                    log.add("desc", BillPayPromoConst.VTHN_CARDNUMBER_ERROR_CONTENT);
                    joReply.putNumber("error", 1000);
                    joReply.putString("desc", BillPayPromoConst.VTHN_CARDNUMBER_ERROR_CONTENT);
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
            }
        });
    }

    //Check vcb cho vien thong ha noi
    public void checkVCBRule(final BillPayPromoObj billPayPromoObj, final Common.BuildLog log, final Message<JsonObject> message) {
        billPayPromosDb.findOneCMND(billPayPromoObj.cmnd, new Handler<BillPayPromosDb.Obj>() {
            @Override
            public void handle(BillPayPromosDb.Obj obj) {
                JsonObject joReply = new JsonObject();
                if (obj != null) {
                    // Da co chung minh nhan dan
                    log.add("desc", BillPayPromoConst.VTHN_CMND_ERROR_CONTENT);
                    joReply.putNumber("error", 1000);
                    joReply.putString("desc", BillPayPromoConst.VTHN_CMND_ERROR_CONTENT);
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }

                promoteGiftForMGD(billPayPromoObj, log, message);
                return;
            }
        });
    }


    //check visa chi vien thong ha noi
    public void checkVisaCard(final BillPayPromoObj billPayPromoObj, final Common.BuildLog log, final Message<JsonObject> message) {
        String sbsBusAddress = sbsCfg.getString("proxyBusAddress", "cyberSourceVerticle");
        VMRequest.getCardList(vertx, sbsBusAddress, billPayPromoObj.phoneNumber, "web", log, new Handler<List<CardInfo>>() {
            @Override
            public void handle(List<CardInfo> cardInfos) {
                final JsonObject jsonReply = new JsonObject();

                if (cardInfos != null && cardInfos.size() > 0) {
                    logger.info("So the la " + cardInfos.size());
                    // Co the visa ....
                    final ArrayList<String> cardCheckSumInfo = new ArrayList<String>();

                    doCheckSumInfor(billPayPromoObj, cardCheckSumInfo, cardInfos, jsonReply, log, 0, message);
                    return;
                } else {
                    // Khong co the visa, khong cho nhan qua luon
                    log.add("desc", BillPayPromoConst.VTHN_NOT_MAPPING_CONTENT);
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", BillPayPromoConst.VTHN_NOT_MAPPING_CONTENT);
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
            }
        });
    }

    private void doCheckSumInfor(final BillPayPromoObj billPayPromoObj, final List<String> cardCheckSumInfo, final List<CardInfo> cardInfos, final JsonObject jsonReply, final Common.BuildLog log, final int index, final Message<JsonObject> message) {
        if (index == cardInfos.size()) {
            // todo
            billPayPromoObj.cardCheckSum = cardCheckSumInfo.get(0);
            promoteGiftForMGD(billPayPromoObj, log, message);
            return;
        }

        final CardInfo cardInfo = cardInfos.get(index);

        billPayPromosDb.findOneCardCheckSum(cardInfo.getHashString(), new Handler<BillPayPromosDb.Obj>() {
            @Override
            public void handle(BillPayPromosDb.Obj obj) {
                if (obj != null) {
                    //Da co thong tin card, khong tra thuong nhe
                    log.add("desc", BillPayPromoConst.VTHN_CARDNUMBER_ERROR_CONTENT);
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", BillPayPromoConst.VTHN_CARDNUMBER_ERROR_CONTENT);
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                } else {
                    log.add("hash the", cardInfo.getHashString());
                    cardCheckSumInfo.add(cardInfo.getHashString());
                }

                doCheckSumInfor(billPayPromoObj, cardCheckSumInfo, cardInfos, jsonReply, log, index + 1, message);
            }
        });
    }

    // Thuc hien tra thuong theo luon OCB
    public void promoteGiftForOCB(final BillPayPromoObj billPayPromoObj, final Common.BuildLog log, final Message<JsonObject> message) {

        phonesDb.getPhoneObjInfo(DataUtil.strToInt(billPayPromoObj.phoneNumber), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(final PhonesDb.Obj phoneObj) {

                if(phoneObj == null)
                {
                    log.add("desc", "Thong tin khach hang khong ton tai");
                    log.writeLog();
                    return;
                }
                else if(phoneObj.bankPersonalId.equalsIgnoreCase(""))
                {
                    log.add("desc", "Khong co thong tin chung minh nhan dan");
                    log.writeLog();
                    return;
                }
                billPayPromosDb.findOne(billPayPromoObj.phoneNumber, new Handler<BillPayPromosDb.Obj>() {
                    @Override
                    public void handle(final BillPayPromosDb.Obj billPayObj) {
                        if(billPayObj == null)
                        {
                            //Chua duoc tra thuong lan nao cho so dien thoai nay, tra thuong cho em no
                            //Tra lan dau tien cho khach hang
                            log.add("ocb promo", "chuc mung tham gia lan dau tien ocb promo nhe, cho ku 100k");
                            giveVoucher100ForOcbPromo(billPayPromoObj, phoneObj, billPayObj, log, message);
                            log.writeLog();
                            //Thanh toan cho no 100k
                            return;
                        }
                        else if(billPayObj.promoCount > 4){
                            // Qua so lan tra thuong
                            log.add("desc", "Qua so lan tra thuong");
                            log.writeLog();
                            return;
                        }
                        if (!BillPayPromoConst.OCB_PROMO.equalsIgnoreCase(billPayObj.group)) {
//                    jsonReply.putNumber("error", 1000);
//                    jsonReply.putString("desc", "Da dang ki dich vu khuyen mai khac");
                            log.add("desc", "Da dang ki dich vu khuyen mai muon gio dong roi nhe");
//                    message.reply(jsonReply);\
                            log.writeLog();
                            return;
                        }
                        //Da co qua 1, 3, tinh tra tiep 2 hoac 4
                        //Kiem tra dich vu
                        final Common.ServiceReq serviceReq = new Common.ServiceReq();
                        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
                        serviceReq.ServiceId = billPayPromoObj.serviceId;
                        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
                            @Override
                            public void handle(JsonArray arrayObjects) {
                                if(arrayObjects.size() > 0)
                                {
                                    //Co trong danh sach dich vu
                                    log.add("desc", "Dich vu ton tai");
                                    String part_id = ((JsonObject) arrayObjects.get(0)).getString(colName.ServiceCols.PARTNER_ID, "").trim().toString();
                                    if(part_id.equalsIgnoreCase("epay") || billPayPromoObj.serviceId.equalsIgnoreCase("topup"))
                                    {
                                        //Check de tra qua 2.
                                        if(!billPayObj.giftId2.equalsIgnoreCase("") || billPayObj.proTimeVn_2 > 0)
                                        {
                                            //Da nhan qua 2 roi, khong tra thuong nua
                                            log.add("desc", "Da nhan qua roi, khong tra thuong nua nhe");
                                            log.writeLog();
                                            return;
                                        }
                                        //Chua nhan qua ne, tra thuong ne.
                                        log.add("desc","Tra thuong qua 2 nhom 1 cho ku cau");
                                        giveFirstOcbPromoVoucher(billPayPromoObj, phoneObj, billPayObj, log, message);
                                    }
                                    else{
                                        //Check de tra qua 4.
                                        if(billPayObj.giftId4.equalsIgnoreCase("") || billPayObj.proTimeVn_4 > 0)
                                        {
                                            //Da nhan qua 4 roi, khong tra thuong nua
                                            log.add("desc", "Da nhan qua roi, khong tra thuong nua nhe");
                                            log.writeLog();
                                            return;
                                        }

                                        //Chua nhan qua, tra thuong nao
                                        log.add("desc","Tra thuong qua 2 nhom 2 cho ku cau");
                                        giveSecondOcbPromoVoucher(billPayPromoObj, phoneObj, billPayObj, log, message);
                                    }
                                }
                                else
                                {
                                    log.add("desc","Dich vu khong ton tai de tra thuong");
                                    log.writeLog();
                                    return;
                                }

                            }
                        });

                    }
                });

            }
        });
    }

    //Tra 100k cho tham gia OCB lan dau tien
    private void giveVoucher100ForOcbPromo(final BillPayPromoObj billPayPromoObj, final PhonesDb.Obj phoneObj, final BillPayPromosDb.Obj billDbObj, final Common.BuildLog log, final Message<JsonObject> message) {
        //public void givePoint1(final String fromAgent, final Common.BuildLog log, final VisaMpointPromoDb.Obj dbObj, final VisaMpointPromoObj requestObj, final Message<JsonObject> message) {
        log.add("func", "giveVoucher100ForOcbPromo");
        final String fromAgent = jsonOcbPromo.getString(StringConstUtil.OBCPromo.AGENT_POINT, "");
        final long mpoint = jsonOcbPromo.getLong(StringConstUtil.OBCPromo.POINT, 0);
        String giftType_1 = jsonOcbPromo.getString(StringConstUtil.OBCPromo.GIFT_TYPE_1, "");
        String giftType_2 = jsonOcbPromo.getString(StringConstUtil.OBCPromo.GIFT_TYPE_2, "");
        log.add("fromAgent", fromAgent);
        log.add("point", mpoint);

        billPayPromoObj.cmnd = phoneObj.bankPersonalId;
        doAllPromoForVoucher(message, jsonOcbPromo, billPayPromoObj, billDbObj, 0, giftType_1, "ocb", log);
        doAllPromoForVoucher(message, jsonOcbPromo, billPayPromoObj, billDbObj, 0, giftType_2, "ocb", log);

        return;
    }

    public void sendNotiAndTranHis100Momo(Common.BuildLog log, long tid, String phoneNumber, long amount) {
        final Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.caption = BillPayPromoConst.HEADER_NOTI_100_MOMO_OCB_PROMO;// "Nhận thưởng quà khuyến mãi";
        noti.body = BillPayPromoConst.CONTENT_NOTI_100_MOMO_OCB_PROMO;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = tid;
        noti.time = new Date().getTime();

        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        Misc.sendNoti(vertx, noti);

        JsonObject jsonTrans = new JsonObject();
        jsonTrans.putNumber(colName.TranDBCols.TRAN_TYPE, 7);
        jsonTrans.putString(colName.TranDBCols.COMMENT, BillPayPromoConst.CONTENT_TRANHIS_100_MOMO_OCB_PROMO);
        jsonTrans.putNumber(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());
        jsonTrans.putNumber(colName.TranDBCols.AMOUNT, amount);
        jsonTrans.putNumber(colName.TranDBCols.STATUS, 0);
        jsonTrans.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        jsonTrans.putString(colName.TranDBCols.BILL_ID, "");
        Misc.sendingStandardTransHisFromJson(vertx, tranDb, jsonTrans, new JsonObject());


        log.writeLog();

        logger.info("Nhan tien 100k momo Thanh cong");
        return;
    }

    //Tra voucher dau tien cho khach hang
    private void giveFirstOcbPromoVoucher(final BillPayPromoObj billPayPromoObj, final PhonesDb.Obj phoneObj, final BillPayPromosDb.Obj billPayObj, final Common.BuildLog log, final Message<JsonObject> message) {
        //Kiem tra xem dich vu khach hang thanh toan co nam trong nhung dich vu tra thuong khong.

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = com.mservice.momo.vertx.processor.Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
        serviceReq.ServiceId = billPayPromoObj.serviceId;
        log.add("func: ", "giveFirstOcbPromoVoucher");
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray array) {
                if (array != null) {

                    log.add("service info", array.size());
                    try {
                        ServiceDb.Obj obj = new ServiceDb.Obj((JsonObject) array.get(0));
                        if (!obj.billPay.equalsIgnoreCase("") && obj.billPay.contains(StringConstUtil.OBCPromo.OCB)) {
                            log.add("thong tin obj bill pay", obj.billPay);
                            //Cho qua nhe
                            log.add("desc", "tra qua khuyen mai ocb lan 1 nhe");
                            doAllPromoForVoucher(message, jsonOcbPromo, billPayPromoObj, billPayObj, 1, "ocb", "ocb", log);
                            return;
                        }
                        log.add("desc --->", "dich vu nay khong duoc tra thuong nhe babe");
                        log.writeLog();
                        return;

                    } catch (Exception e) {
                        log.add("desc", "Khong parse duoc thong tin service");
                        log.writeLog();
                    }
                }
            }
        });
    }

    //Tra voucher thu hai cho khach hang
    private void giveSecondOcbPromoVoucher(final BillPayPromoObj billPayPromoObj, final PhonesDb.Obj phoneObj, final BillPayPromosDb.Obj billPayObj, final Common.BuildLog log, final Message<JsonObject> message) {
        //Kiem tra voucher dau tien da duoc su dung ngon lanh chua, neu roi thi moi cho voucher 2
        giftDb.findOne(billPayObj.giftId1, new Handler<Gift>() {
            @Override
            public void handle(Gift gift) {
                if (gift != null && gift.status == 6) {
                    //Da dung qua thanh cong, tra no qua tiep nhe.
                    //Tra qua 2.
                    log.add("desc", "tra qua khuyen mai ocb lan 2 nhe");
                    doAllPromoForVoucher(message, jsonOcbPromo, billPayPromoObj, billPayObj, 2, "ocb", "ocb", log);
                    return;
                } else {
                    //Loi
                }
            }
        });
    }

    // Thuc hien tra thuong theo luon Muon gio dong
    public void promoteGiftForMGD(final BillPayPromoObj billPayPromoObj, final Common.BuildLog log, final Message<JsonObject> message) {
        final String phoneNumber = billPayPromoObj.phoneNumber;
        billPayPromosDb.findOne(phoneNumber, new Handler<BillPayPromosDb.Obj>() {
            @Override
            public void handle(final BillPayPromosDb.Obj obj) {

                log.add("func", "billPayPromosDb.findOne");

                final JsonObject jsonReply = new JsonObject();

                //Khach hang chua thuc hien chuong trinh khuyen mai
                if (obj == null) {
                    doPromoForVoucher(message, jsonReply, billPayPromoObj, obj, 1, log);
                    return;
                }

                //Kiem tra nhap tu chuong trinh khuyen mai
                //tranType = 0 => nhap tu khuyen mai
                //tranType = -10 => tra tay
                if (billPayPromoObj.source.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO) && billPayPromoObj.tranType == 0) {
                    if (billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId1) || billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId2)) {
                        jsonReply.putNumber("error", 1000);
                        jsonReply.putString("desc", "Bạn đã nhận thẻ quà tặng cho dịch vụ này. Vui lòng chọn hoá đơn/dịch vụ khác.");
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    } else if (!"".equalsIgnoreCase(obj.serviceId1) && !"".equalsIgnoreCase(obj.serviceId2)) {
                        jsonReply.putNumber("error", 1000);
                        jsonReply.putString("desc", "Bạn đã hết lượt chọn hóa đơn/dịch vụ để nhận thẻ quà tặng. Mỗi khách hàng chỉ được chọn tối đa hai dịch vụ để nhận thẻ quà tặng.");
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    } else if (!billPayPromoObj.source.equalsIgnoreCase(obj.group)) {
                        jsonReply.putNumber("error", 1000);
                        jsonReply.putString("desc", "Chương trình này không áp dụng cho khách hàng đã tham gia chương trình <Trải nghiệm MoMo cùng Vietcombank>. Chi tiết gọi: (08) 39917199.");
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    }
                }

                if (obj.promoCount > 3) {
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Qua so lan tra thuong roi, khong tra nua");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
                //Khach hang da co trong chuong trinh khuyen mai.

                //Kiem tra khach hang

                //Neu khach hang thuc hien khong phai VCB va co so voucher > 4
                if (billPayPromoObj.source.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO) && obj.promoCount >= 4) {
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Qua so lan tra thuong roi, khong tra nua");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }

                // Chuong trinh gio dong va tat ca cac gift deu duoc nhan thi khong cho nhan nua
                if (billPayPromoObj.source.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO) &&
                        !"".equalsIgnoreCase(obj.giftTypeId1) && !"".equalsIgnoreCase(obj.giftTypeId2)
                        && !"".equalsIgnoreCase(obj.giftTypeId3) && !"".equalsIgnoreCase(obj.giftTypeId4)) {
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Qua so lan tra thuong roi, khong tra nua");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }

                //Neu dich vu khach hang khong giong voi dich vu da luu trong database
                if (!BillPayPromoConst.MUON_GIO_DONG_PROMO.equalsIgnoreCase(obj.group)) {
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Da dang ki dich vu khuyen mai khac");
                    log.add("desc", "Da dang ki dich vu khuyen mai khac");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }

                //Khong load duoc agent tra thuong
                if (billpayPromoCfg == null) {
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Hệ thống đang bảo trì. Chúng tôi đã ghi nhận bạn tham gia chương trình và sẽ phản hồi trong thời gian sớm nhất. Chi tiết liên hệ: (08)39917199");
                    log.add("desc", "Khong lay duoc thong tin agent");
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }

                //Kiem tra dich vu khach hang su dung voi dich vu da duoc luu trong database 1
                if (billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId1)) {
                    if ("".equalsIgnoreCase(obj.giftTypeId1)) {
                        //Them gift cho voucher 1.
                        log.add("desc", "Tra voucher 1");
                        doPromoForVoucher(message, jsonReply, billPayPromoObj, obj, 1, log);
                        log.writeLog();
                        return;
                    } else if ("".equalsIgnoreCase(obj.giftTypeId2)) {
                        //Them gift cho voucher 2.
                        log.add("desc", "Tra voucher 2");
                        doPromoForVoucher(message, jsonReply, billPayPromoObj, obj, 2, log);
                        return;
                    } else {
                        //Da nhan du voucher cho dich vu nay, khong cho nhan nua
                        jsonReply.putNumber("error", 1000);
                        jsonReply.putString("desc", "Da dang ki dich vu khuyen mai khac");
                        log.add("desc", "Qua da tra day du cho dich vu " + billPayPromoObj.serviceId);
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    }
                } else if (billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId2)) {
                    if ("".equalsIgnoreCase(obj.giftTypeId3)) {
                        //Them gift cho voucher 3
                        log.add("desc", "Tra voucher 3");
                        doPromoForVoucher(message, jsonReply, billPayPromoObj, obj, 3, log);
                        log.writeLog();
                        return;
                    } else if ("".equalsIgnoreCase(obj.giftTypeId4)) {
                        log.add("desc", "Tra voucher 4");
                        doPromoForVoucher(message, jsonReply, billPayPromoObj, obj, 4, log);
                        log.writeLog();
                        return;
                        // Them gift cho voucher 4
                    } else {
                        //Da nhan du phan thuong cho dich vu, khong cho nhan nua.
                        jsonReply.putNumber("error", 1000);
                        jsonReply.putString("desc", "Da dang ki dich vu khuyen mai khac");
                        log.add("desc", "Qua da tra day du cho dich vu " + billPayPromoObj.serviceId);
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    }
                } else if ("".equalsIgnoreCase(obj.serviceId1)) {
                    //Insert vao serviceId 1 vs voucher 1
                    doPromoForVoucher(message, jsonReply, billPayPromoObj, obj, 1, log);
                    log.writeLog();
                    return;
                } else if ("".equalsIgnoreCase(obj.serviceId2)) {
                    //Upsert vao serviceId2 vs voucher 3
                    doPromoForVoucher(message, jsonReply, billPayPromoObj, obj, 3, log);
                    log.writeLog();
                    return;
                } else {
                    //Da nhan du phan thuong cho dich vu, khong cho nhan nua.
                    jsonReply.putNumber("error", 1000);
                    jsonReply.putString("desc", "Da dang ki dich vu khuyen mai khac");
                    log.add("desc", "Qua da tra day du cho dich vu " + billPayPromoObj.serviceId);
                    message.reply(jsonReply);
                    log.writeLog();
                    return;
                }
            }
        });
    }


    // Thuc hien kiem tra tra thuong
    public void promoteGiftForBillPay(final BillPayPromoObj billPayPromoObj, final Common.BuildLog log, final JsonObject jsonTime, final Message<JsonObject> message) {
        final String phoneNumber = billPayPromoObj.phoneNumber;

        if (billPayPromoObj.serviceId.equalsIgnoreCase("")) {
            return;
        }


        billPayPromosDb.findOne(phoneNumber, new Handler<BillPayPromosDb.Obj>() {
            @Override
            public void handle(final BillPayPromosDb.Obj obj) {

                log.add("func", "billPayPromosDb.findOne");

                final JsonObject jsonReply = new JsonObject();

                //Khach hang chua thuc hien chuong trinh khuyen mai
                if (obj == null) {
                    //Kiem tra mapvi VCB.

                    return;
                } else {
//                    if (obj.group.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO)) {
//                        log.add("desc", "obj.group.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO)");
//                        billPayPromoObj.source = BillPayPromoConst.OCB_PROMO;
//                        promoteGiftForOCB(billPayPromoObj, log, message);
//                    } else
                    if (obj.group.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO)) {
                        long mgdStartDate = jsonTime.getLong(BillPayPromoConst.START_DATE_MGD, 0);
                        long mgdEndDate = jsonTime.getLong(BillPayPromoConst.END_DATE_MGD, 0);
                        long currentTime = System.currentTimeMillis();
                        if (currentTime < mgdStartDate || currentTime > mgdEndDate) {
                            log.add("desc", "Đã hết thời gian khuyến mãi.");
                            jsonReply.putNumber("error", 1000);
                            jsonReply.putString("desc", "Đã hết thời gian khuyến mãi.");
                            message.reply(jsonReply);
                            log.writeLog();
                            return;
                        }
                        if (!billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId1) && "".equalsIgnoreCase(obj.serviceId2)) {
                            log.add("desc", "Thanh toan khong dung voi dich vu 1 da chon, khong tra thuong");
                            jsonReply.putNumber("error", 1000);
                            jsonReply.putString("desc", "Thanh toan khong dung voi dich vu da chon, khong tra thuong");
                            message.reply(jsonReply);
                            log.writeLog();
                            return;
                        } else if (!billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId1) && !billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId2)) {
                            log.add("desc", "Thanh toan khong dung voi dich vu 1 vs 2 da chon, khong tra thuong");
                            jsonReply.putNumber("error", 1000);
                            jsonReply.putString("desc", "Thanh toan khong dung voi dich vu 1 2 da chon, khong tra thuong");
                            message.reply(jsonReply);
                            log.writeLog();
                            return;
                        } else if (billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId1)) {
                            //Kiem tra xem qua 1 da duoc su dung chua nhe.
                            log.add("desc", "billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId1)");
                            log.add("giftId", obj.giftId1);
                            giftDb.findOne(obj.giftId1, new Handler<Gift>() {
                                @Override
                                public void handle(Gift gift) {
                                    if (gift != null && gift.status > 3) {
                                        billPayPromoObj.source = BillPayPromoConst.MUON_GIO_DONG_PROMO;
                                        promoteGiftForMGD(billPayPromoObj, log, message);
                                        return;
                                    } else {
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Qua 1 chua duoc su dung thanh cong nhe");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }
                                }
                            });
                        } else if (billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId2)) {
                            // Kiem tra xem qua 3 da duoc su dung chua
                            log.add("desc", "billPayPromoObj.serviceId.equalsIgnoreCase(obj.serviceId2)");
                            log.add("giftId _3", obj.giftId3);
                            giftDb.findOne(obj.giftId3, new Handler<Gift>() {
                                @Override
                                public void handle(Gift gift) {
                                    if (gift != null && gift.status > 3) {
                                        billPayPromoObj.source = BillPayPromoConst.MUON_GIO_DONG_PROMO;
                                        promoteGiftForMGD(billPayPromoObj, log, message);
                                        return;
                                    } else {
                                        jsonReply.putNumber("error", 1000);
                                        jsonReply.putString("desc", "Qua 3 chua duoc su dung thanh cong nhe");
                                        message.reply(jsonReply);
                                        log.writeLog();
                                        return;
                                    }
                                }
                            });
                        } else {
                            jsonReply.putNumber("error", 1000);
                            jsonReply.putString("desc", "eo biet vi sao loi boi vi tu nhien may co trong table nay");
                            message.reply(jsonReply);
                            log.writeLog();
                            return;
                        }
                    } else {
                        jsonReply.putNumber("error", 1000);
                        jsonReply.putString("desc", "eo biet vi sao loi boi vi tu nhien may co trong table nay");
                        message.reply(jsonReply);
                        log.writeLog();
                        return;
                    }
                }
                //Khach hang da co trong chuong trinh khuyen mai.
            }
        });
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void doPromoForVoucher(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final int numVoucher
            , final Common.BuildLog log) {

        // Trả khuyến mãi
        // Them thong tin service id va so voucher vao core
        String group = reqObj.source + "_" + reqObj.serviceId + "_" + numVoucher;
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", "billpayPromo"));

//        keyValues.add(new Misc.KeyValue("group", reqObj.source));
        keyValues.add(new Misc.KeyValue("group", group));

        final String agentName = billpayPromoCfg.getString("agent", "cskh");
        final int timeForGift = billpayPromoCfg.getInteger("timeforgift", 30);
        final long giftValue = billpayPromoCfg.getLong("valueofgift", 50000);
        final int timeForActive = billpayPromoCfg.getInteger("timeforactive", 20);
        final String giftTypeId = reqObj.serviceId;

        //Tra thuong trong core
        giftManager.adjustGiftValue(agentName
                , reqObj.phoneNumber
                , giftValue
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error", error);
                log.add("desc", SoapError.getDesc(error));

                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    final GiftType giftType = new GiftType();
                    giftType.setModelId(giftTypeId);

                    ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    Misc.KeyValue kv = new Misc.KeyValue();
                    kv.Key = "group";
                    kv.Value = reqObj.source;
                    keyValues.add(kv);
                    int endGiftTime = 0;
                    long modifyDate = 0;
                    if (numVoucher == 2 || numVoucher == 4) {
                        endGiftTime = timeForGift + timeForActive;
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        modifyDate = calendar.getTimeInMillis() + (timeForActive * 24 * 60 * 60 * 1000L);
                    } else {
                        endGiftTime = timeForGift;
                        modifyDate = System.currentTimeMillis();
                    }
                    Misc.KeyValue kv_1 = new Misc.KeyValue();
                    kv_1.Key = BillPayPromoConst.ACTIVE_TIME;
                    kv_1.Value = modifyDate + "";
                    keyValues.add(kv_1);
                    String note = reqObj.source + "_" + numVoucher + "";
                    //tao gift tren backend
                    giftManager.createLocalGiftForBillPayPromoWithDetailGift(reqObj.phoneNumber
                            , giftValue
                            , giftType
                            , promotedTranId
                            , agentName
                            , modifyDate
                            , endGiftTime
                            , keyValues, note, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            int err = jsonObject.getInteger("error", -1);
                            final long tranId = jsonObject.getInteger("tranId", -1);
                            final Gift gift = new Gift(jsonObject.getObject("gift"));
                            String giftId = gift.getModelId();
                            log.add("desc", "tra thuong chuong trinh customer care bang gift");
                            log.add("err", err);

                            //------------tat ca thanh cong roi
                            if (err == 0) {
                                if (numVoucher == 1) {
                                    giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            gift.status = 3;
                                            giveVoucherOne(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                            return;
                                        }
                                    });
                                } else if (numVoucher == 2 && reqObj.source.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO)) {
                                    giveVoucherTwo(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                    return;
                                } else if (numVoucher == 2 && reqObj.source.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO)) {
                                    giveVoucherTwo(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                    return;
                                } else if (numVoucher == 3) {
                                    giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            gift.status = 3;
                                            giveVoucherThree(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                            return;
                                        }
                                    });
                                } else if (numVoucher == 4) {
                                    giveVoucherFour(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                    return;
                                }
//                                giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
//                                    @Override
//                                    public void handle(JsonObject jsonObject) {
//                                        if (numVoucher == 1) {
//                                            giveVoucherOne(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, log);
//                                        } else if (numVoucher == 2 && reqObj.source.equalsIgnoreCase(BillPayPromoConst.MUON_GIO_DONG_PROMO)) {
//                                            giveVoucherTwo(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, log);
//                                        } else if (numVoucher == 2 && reqObj.source.equalsIgnoreCase(BillPayPromoConst.OCB_PROMO)) {
//                                            giveVoucherTwo(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, log);
//                                        } else if (numVoucher == 3) {
//                                            giveVoucherThree(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, log);
//                                        } else if (numVoucher == 4) {
//                                            giveVoucherFour(message, joReply, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, log);
//                                        }
//                                    }
//                                });
                            } else {
                                joReply.putNumber("error", 1000);
                                joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                } else {
                    //tra thuong trong core khong thanh cong
                    log.add("desc", "Lỗi " + SoapError.getDesc(error));
                    log.add("Exception", "Exception " + SoapError.getDesc(error));
                    log.writeLog();
                    if (error == 7) {
                        joReply.putNumber("error", error);
                        joReply.putString("desc", "Rất tiếc! Bạn chưa đủ điều kiện nhận quà tặng từ Chương trình khuyến mãi. Vui lòng gọi 083917199 để biết thêm chi tiết.");
                    } else {
                        joReply.putNumber("error", error);
                        joReply.putString("desc", "Hệ thống đang bảo trì. Chúng tôi đã ghi nhận bạn tham gia chương trình và sẽ phản hồi trong thời gian sớm nhất. Chi tiết liên hệ: (08)39917199");
                    }
                    //Them json khi billpay bi loi he thong tu core
                    BillPayPromoErrorDb.Obj billPayErrorObj = new BillPayPromoErrorDb.Obj();
                    billPayErrorObj.id = reqObj.phoneNumber + reqObj.serviceId;
                    billPayErrorObj.number = reqObj.phoneNumber;
                    billPayErrorObj.group = reqObj.source;
                    billPayErrorObj.error_code = error;
                    billPayErrorObj.error_desc = SoapError.getDesc(error);
                    billPayErrorObj.service_id = reqObj.serviceId;
                    billPayErrorObj.time_error = System.currentTimeMillis();

                    billPayPromoErrorDb.insert(billPayErrorObj, new Handler<Integer>() {
                        @Override
                        public void handle(Integer integer) {

                        }
                    });
                    message.reply(joReply);

                    log.writeLog();
                    return;
                }
            }
        });
    }

    public void giveVoucherOne(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final int timeForActive
            , final Common.BuildLog log) {
//-------cap nhat mongo db ghi nhan da tra thuong


        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(final JsonArray objects) {
                final long activatedTime = System.currentTimeMillis();
                log.add("array", objects);
                BillPayPromosDb.Obj rowObj_insert = new BillPayPromosDb.Obj();
                rowObj_insert.number = reqObj.phoneNumber;
                rowObj_insert.serviceId1 = reqObj.serviceId;
                rowObj_insert.group = reqObj.source;
                rowObj_insert.proTimeVn_1 = activatedTime;
                rowObj_insert.giftTypeId1 = reqObj.serviceId;
                rowObj_insert.promoCount = 1;
                rowObj_insert.tranId1 = promotedTranId;
                rowObj_insert.tranType1 = reqObj.tranType;
                rowObj_insert.activatedTime2 = 0;
                rowObj_insert.activatedTime4 = 0;
                rowObj_insert.giftId1 = gift.getModelId();
                rowObj_insert.cardCheckSum = reqObj.cardCheckSum;
                rowObj_insert.cmnd = reqObj.cmnd;

                billPayPromosDb.insert(rowObj_insert, new Handler<Integer>() {
                    @Override
                    public void handle(Integer integer) {
                        if (integer == 0) {
                            //Insert DB thanh cong
                            long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                            long endTime = activatedTime + timeforgift;
                            String date = Misc.dateVNFormatWithDot(endTime);
                            log.add("group", reqObj.source);
                            String giftMessage = "giftMessage";
                            String notiCaption = PromoContentNotification.BILL_PAY_PROMO_TITLE;
                            String serviceName = "serviceName";
                            String tranComment = "tranComment";
                            String notiBody = "notiBody";


                            if (objects != null && objects.size() > 0) {
                                serviceName = ((JsonObject) objects.get(0)).getString("ser_name", reqObj.serviceId.toUpperCase()).toString();
                                giftMessage = String.format(PromoContentNotification.BILL_PAY_PROMO_GIFT_MESSAGE_ONE, serviceName, date);
                                tranComment = String.format(PromoContentNotification.BILL_PAY_PROMO_TRAN_COMMENT_ONE, serviceName, date);
                                notiBody = String.format(PromoContentNotification.BILL_PAY_PROMO_DETAIL_NOTI_ONE, serviceName, date);
                            }
                            Misc.sendTranHisAndNotiBillPayGift(vertx
                                    , DataUtil.strToInt(reqObj.phoneNumber)
                                    , tranComment
                                    , tranId
                                    , giftValue
                                    , gift
                                    , notiCaption
                                    , notiBody
                                    , giftMessage
                                    , tranDb);
                            //------------thong bao nhan qua neu la nhom 1
                            joReply.putNumber("error", 0);
                            joReply.putString("desc", "Thành công");
                            message.reply(joReply);
                            log.writeLog();
                            return;
                        } else {
                            joReply.putNumber("error", 1000);
                            joReply.putString("desc", "Khong insert database thanh cong _ gift 1");
                            message.reply(joReply);
                            log.writeLog();
                            return;
                        }
                    }
                });

            }
        });
    }


    public void giveVoucherTwo(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final int timeForActive
            , final Common.BuildLog log) {
//-------cap nhat mongo db ghi nhan da tra thuong
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(final JsonArray objects) {
                final long receivedGiftTime = System.currentTimeMillis();
                JsonObject joUpdate = new JsonObject();
                joUpdate.putNumber(colName.BillPayPromo.PROMO_TIME_2, receivedGiftTime);
                joUpdate.putNumber(colName.BillPayPromo.PROMO_COUNT, (rowObj.promoCount + 1));
                joUpdate.putNumber(colName.BillPayPromo.TRAN_ID_2, promotedTranId);
                joUpdate.putNumber(colName.BillPayPromo.TRAN_TYPE_2, reqObj.tranType);
                joUpdate.putString(colName.BillPayPromo.GIFT_TYPE_ID_2, reqObj.serviceId);
                final long timeToActivatedGift = receivedGiftTime + (timeForActive * 24 * 60 * 60 * 1000L);
                joUpdate.putNumber(colName.BillPayPromo.ACTIVATED_TIME_2, timeToActivatedGift);
                joUpdate.putString(colName.BillPayPromo.GIFT_ID_2, gift.getModelId());


                billPayPromosDb.updatePartial(rowObj.number, joUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        if (!aBoolean) {
                            long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                            String startDateToUseGift = Misc.dateVNFormatWithDot(timeToActivatedGift);
                            long endTime = timeToActivatedGift + timeforgift;
                            String endDate = Misc.dateVNFormatWithDot(endTime);

                            log.add("group", reqObj.source);
                            String giftMessage = "giftMessage";
                            String notiCaption = PromoContentNotification.BILL_PAY_PROMO_TITLE;
                            String serviceName = "serviceName";
                            String tranComment = "tranComment";
                            String notiBody = "notiBody";


                            if (objects != null && objects.size() > 0) {
                                serviceName = ((JsonObject) objects.get(0)).getString("ser_name", reqObj.serviceId.toUpperCase()).toString();
                                giftMessage = String.format(PromoContentNotification.BILL_PAY_PROMO_GIFT_MESSAGE_TWO, serviceName, startDateToUseGift, endDate);
                                tranComment = String.format(PromoContentNotification.BILL_PAY_PROMO_TRAN_COMMENT_TWO, serviceName, startDateToUseGift, endDate);
                                notiBody = String.format(PromoContentNotification.BILL_PAY_PROMO_DETAIL_NOTI_TWO, serviceName, startDateToUseGift, endDate);
                            }
                            Misc.sendTranHisAndNotiBillPayGift(vertx
                                    , DataUtil.strToInt(reqObj.phoneNumber)
                                    , tranComment
                                    , tranId
                                    , giftValue
                                    , gift
                                    , notiCaption
                                    , notiBody
                                    , giftMessage
                                    , tranDb);
                            //------------thong bao nhan qua neu la nhom 1
                            joReply.putNumber("error", 0);
                            joReply.putString("desc", "Thành công");
                            message.reply(joReply);
                            log.writeLog();
                            return;
                        }
                    }
                });


            }
        });
    }


    public void giveVoucherThree(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final int timeForActive
            , final Common.BuildLog log) {
//-------cap nhat mongo db ghi nhan da tra thuong
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(final JsonArray objects) {
                final long activatedTime = System.currentTimeMillis();
                JsonObject joUpdate = new JsonObject();
                joUpdate.putNumber(colName.BillPayPromo.PROMO_TIME_3, activatedTime);
                joUpdate.putNumber(colName.BillPayPromo.PROMO_COUNT, (rowObj.promoCount + 1));
                joUpdate.putNumber(colName.BillPayPromo.TRAN_ID_3, promotedTranId);
                joUpdate.putNumber(colName.BillPayPromo.TRAN_TYPE_3, reqObj.tranType);
                joUpdate.putString(colName.BillPayPromo.GIFT_TYPE_ID_3, reqObj.serviceId);
                joUpdate.putString(colName.BillPayPromo.SERVICE_ID_2, reqObj.serviceId);
                joUpdate.putString(colName.BillPayPromo.GIFT_ID_3, gift.getModelId());
                if (!"".equalsIgnoreCase(reqObj.cardCheckSum)) {
                    joUpdate.putString(colName.BillPayPromo.CARD_CHECK_SUM_VISA, reqObj.cardCheckSum);
                }
                if (!"".equalsIgnoreCase(reqObj.cmnd)) {
                    joUpdate.putString(colName.BillPayPromo.CMND, reqObj.cmnd);
                }
                billPayPromosDb.updatePartial(rowObj.number, joUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        if (!aBoolean) {
                            long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                            long endTime = activatedTime + timeforgift;
                            String date = Misc.dateVNFormatWithDot(endTime);
                            log.add("group", reqObj.source);
                            String giftMessage = "giftMessage";
                            String notiCaption = PromoContentNotification.BILL_PAY_PROMO_TITLE;
                            String serviceName = "serviceName";
                            String tranComment = "tranComment";
                            String notiBody = "notiBody";


                            if (objects != null && objects.size() > 0) {
                                serviceName = ((JsonObject) objects.get(0)).getString("ser_name", reqObj.serviceId.toUpperCase()).toString();
                                giftMessage = String.format(PromoContentNotification.BILL_PAY_PROMO_GIFT_MESSAGE_THREE, serviceName, date);
                                tranComment = String.format(PromoContentNotification.BILL_PAY_PROMO_TRAN_COMMENT_THREE, serviceName, date);
                                notiBody = String.format(PromoContentNotification.BILL_PAY_PROMO_DETAIL_NOTI_THREE, serviceName, date);
                            }
                            Misc.sendTranHisAndNotiBillPayGift(vertx
                                    , DataUtil.strToInt(reqObj.phoneNumber)
                                    , tranComment
                                    , tranId
                                    , giftValue
                                    , gift
                                    , notiCaption
                                    , notiBody
                                    , giftMessage
                                    , tranDb);
                            //------------thong bao nhan qua neu la nhom 1
                            joReply.putNumber("error", 0);
                            joReply.putString("desc", "Thành công");
                            message.reply(joReply);
                            log.writeLog();
                            return;
                        }
                    }
                });
            }
        });
    }


    public void giveVoucherFour(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final int timeForActive
            , final Common.BuildLog log) {
//-------cap nhat mongo db ghi nhan da tra thuong
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(final JsonArray objects) {
                final long timeReceivedGift = System.currentTimeMillis();
                JsonObject joUpdate = new JsonObject();
                joUpdate.putNumber(colName.BillPayPromo.PROMO_TIME_4, timeReceivedGift);
                joUpdate.putNumber(colName.BillPayPromo.PROMO_COUNT, (rowObj.promoCount + 1));
                joUpdate.putNumber(colName.BillPayPromo.TRAN_ID_4, promotedTranId);
                joUpdate.putNumber(colName.BillPayPromo.TRAN_TYPE_4, reqObj.tranType);
                joUpdate.putString(colName.BillPayPromo.GIFT_TYPE_ID_4, reqObj.serviceId);
                final long timeToActivatedGift = timeReceivedGift + (timeForActive * 24 * 60 * 60 * 1000L);
                joUpdate.putNumber(colName.BillPayPromo.ACTIVATED_TIME_4, timeToActivatedGift);
                joUpdate.putString(colName.BillPayPromo.GIFT_ID_4, gift.getModelId());
                billPayPromosDb.updatePartial(rowObj.number, joUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        if (!aBoolean) {
                            long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                            String startDateToUseGift = Misc.dateVNFormatWithDot(timeToActivatedGift);
                            long endTime = timeToActivatedGift + timeforgift;
                            String endDate = Misc.dateVNFormatWithDot(endTime);

                            log.add("group", reqObj.source);
                            String giftMessage = "giftMessage";
                            String notiCaption = PromoContentNotification.BILL_PAY_PROMO_TITLE;
                            String serviceName = "serviceName";
                            String tranComment = "tranComment";
                            String notiBody = "notiBody";


                            if (objects != null && objects.size() > 0) {
                                serviceName = ((JsonObject) objects.get(0)).getString("ser_name", reqObj.serviceId.toUpperCase()).toString();
                                giftMessage = String.format(PromoContentNotification.BILL_PAY_PROMO_GIFT_MESSAGE_FOUR, serviceName, startDateToUseGift, endDate);
                                tranComment = String.format(PromoContentNotification.BILL_PAY_PROMO_TRAN_COMMENT_FOUR, serviceName, startDateToUseGift, endDate);
                                notiBody = String.format(PromoContentNotification.BILL_PAY_PROMO_DETAIL_NOTI_FOUR, serviceName, startDateToUseGift, endDate);
                            }
                            Misc.sendTranHisAndNotiBillPayGift(vertx
                                    , DataUtil.strToInt(reqObj.phoneNumber)
                                    , tranComment
                                    , tranId
                                    , giftValue
                                    , gift
                                    , notiCaption
                                    , notiBody
                                    , giftMessage
                                    , tranDb);
                            //------------thong bao nhan qua neu la nhom 1
                            joReply.putNumber("error", 0);
                            joReply.putString("desc", "Thành công");
                            message.reply(joReply);
                            log.writeLog();
                            return;
                        }
                    }
                });
            }
        });
    }


    //BEGIN 0000000050
    public void giveVoucherOneBankPromo(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final int timeForActive
            , final Common.BuildLog log) {
//-------cap nhat mongo db ghi nhan da tra thuong
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;

        final long activatedTime = System.currentTimeMillis();
        JsonObject joUpdate = new JsonObject();
        joUpdate.putNumber(colName.BillPayPromo.PROMO_TIME_3, activatedTime);
        joUpdate.putNumber(colName.BillPayPromo.PROMO_COUNT, (rowObj.promoCount + 1));
        joUpdate.putNumber(colName.BillPayPromo.TRAN_ID_3, promotedTranId);
        joUpdate.putNumber(colName.BillPayPromo.TRAN_TYPE_3, reqObj.tranType);
        joUpdate.putString(colName.BillPayPromo.GIFT_TYPE_ID_3, reqObj.serviceId);
        joUpdate.putString(colName.BillPayPromo.SERVICE_ID_1, reqObj.serviceId);
        joUpdate.putString(colName.BillPayPromo.GIFT_ID_3, gift.getModelId());
        if (!"".equalsIgnoreCase(reqObj.cardCheckSum)) {
            joUpdate.putString(colName.BillPayPromo.CARD_CHECK_SUM_VISA, reqObj.cardCheckSum);
        }
        if (!"".equalsIgnoreCase(reqObj.cmnd)) {
            joUpdate.putString(colName.BillPayPromo.CMND, reqObj.cmnd);
        }
        billPayPromosDb.updatePartial(rowObj.number, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (!aBoolean) {
                    long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                    long endTime = activatedTime + timeforgift;
                    String date = Misc.dateVNFormatWithDot(endTime);
                    log.add("group", reqObj.source);
                    String giftMessage = "giftMessage";
                    String notiCaption = PromoContentNotification.BILL_PAY_PROMO_TITLE;
                    String serviceName = "serviceName";
                    String tranComment = "tranComment";
                    String notiBody = "notiBody";


                    giftMessage = String.format(PromoContentNotification.OCB_PROMO_50_VOUCHER_GIFT_1, date);
                    tranComment = String.format(PromoContentNotification.OCB_PROMO_50_VOUCHER_GIFT_1, date);
                    notiBody = String.format(PromoContentNotification.OCB_PROMO_50_VOUCHER_GIFT_1, date);

                    Misc.sendTranHisAndNotiBillPayGift(vertx
                            , DataUtil.strToInt(reqObj.phoneNumber)
                            , tranComment
                            , tranId
                            , giftValue
                            , gift
                            , notiCaption
                            , notiBody
                            , giftMessage
                            , tranDb);
                    //------------thong bao nhan qua neu la nhom 1
                    joReply.putNumber("error", 0);
                    joReply.putString("desc", "Thành công");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
            }
        });
    }


    public void giveVoucherTwoBankPromo(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final int timeForActive
            , final Common.BuildLog log) {
//-------cap nhat mongo db ghi nhan da tra thuong
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;


        final long timeReceivedGift = System.currentTimeMillis();
        JsonObject joUpdate = new JsonObject();
        joUpdate.putNumber(colName.BillPayPromo.PROMO_TIME_4, timeReceivedGift);
        joUpdate.putNumber(colName.BillPayPromo.PROMO_COUNT, (rowObj.promoCount + 1));
        joUpdate.putNumber(colName.BillPayPromo.TRAN_ID_4, promotedTranId);
        joUpdate.putString(colName.BillPayPromo.SERVICE_ID_2, reqObj.serviceId);
        joUpdate.putNumber(colName.BillPayPromo.TRAN_TYPE_4, reqObj.tranType);
        joUpdate.putString(colName.BillPayPromo.GIFT_TYPE_ID_4, reqObj.serviceId);
        final long timeToActivatedGift = timeReceivedGift + (timeForActive * 24 * 60 * 60 * 1000L);
        joUpdate.putNumber(colName.BillPayPromo.ACTIVATED_TIME_4, timeToActivatedGift);
        joUpdate.putString(colName.BillPayPromo.GIFT_ID_4, gift.getModelId());
        billPayPromosDb.updatePartial(rowObj.number, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                if (!aBoolean) {
                    long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                    String startDateToUseGift = Misc.dateVNFormatWithDot(timeToActivatedGift);
                    long endTime = timeToActivatedGift + timeforgift;
                    String endDate = Misc.dateVNFormatWithDot(endTime);

                    log.add("group", reqObj.source);
                    String giftMessage = "giftMessage";
                    String notiCaption = PromoContentNotification.BILL_PAY_PROMO_TITLE;
                    String serviceName = "serviceName";
                    String tranComment = "tranComment";
                    String notiBody = "notiBody";

                    giftMessage = String.format(PromoContentNotification.OCB_PROMO_50_VOUCHER_GIFT_2, startDateToUseGift, endDate);
                    tranComment = String.format(PromoContentNotification.OCB_PROMO_50_VOUCHER_GIFT_2, startDateToUseGift, endDate);
                    notiBody = String.format(PromoContentNotification.OCB_PROMO_50_VOUCHER_GIFT_2, startDateToUseGift, endDate);

                    Misc.sendTranHisAndNotiBillPayGift(vertx
                            , DataUtil.strToInt(reqObj.phoneNumber)
                            , tranComment
                            , tranId
                            , giftValue
                            , gift
                            , notiCaption
                            , notiBody
                            , giftMessage
                            , tranDb);
                    //------------thong bao nhan qua neu la nhom 1
                    joReply.putNumber("error", 0);
                    joReply.putString("desc", "Thành công");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
            }
        });
    }

    public void giveVoucher100BankPromo(final Message<JsonObject> message
            , final JsonObject joReply
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final Gift gift
            , final long tranId
            , final long promotedTranId
            , final long giftValue
            , final int timeForGift
            , final int timeForActive
            , final Common.BuildLog log) {
//-------cap nhat mongo db ghi nhan da tra thuong


        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;


        final long activatedTime = System.currentTimeMillis();
        BillPayPromosDb.Obj rowObj_insert = new BillPayPromosDb.Obj();
        rowObj_insert.number = reqObj.phoneNumber;
        rowObj_insert.serviceIdPoint = reqObj.serviceId;
        rowObj_insert.group = reqObj.source;
        rowObj_insert.proTimeVn_1 = activatedTime;
        rowObj_insert.giftTypeId1 = reqObj.serviceId;
        rowObj_insert.promoCount = 1;
        rowObj_insert.tranId1 = promotedTranId;
        rowObj_insert.tranType1 = reqObj.tranType;
        rowObj_insert.activatedTime2 = 0;
        rowObj_insert.activatedTime4 = 0;
        rowObj_insert.giftId1 = gift.getModelId();
        rowObj_insert.cardCheckSum = reqObj.cardCheckSum;
        rowObj_insert.cmnd = reqObj.cmnd;

        billPayPromosDb.insert(rowObj_insert, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {
                if (integer == 0) {
                    //Insert DB thanh cong
                    long timeforgift = 60 * 1000L * 60 * 24 * timeForGift;
                    long endTime = activatedTime + timeforgift;
                    String date = Misc.dateVNFormatWithDot(endTime);
                    log.add("group", reqObj.source);
                    String giftMessage = "giftMessage";
                    String notiCaption = PromoContentNotification.BILL_PAY_PROMO_TITLE;
                    String serviceName = "serviceName";
                    String tranComment = "tranComment";
                    String notiBody = "notiBody";


                    giftMessage = PromoContentNotification.OCB_PROMO_100_VOUCHER_GIFT;
                    tranComment = BillPayPromoConst.CONTENT_TRANHIS_100_MOMO_OCB_PROMO;
                    notiBody = PromoContentNotification.OCB_PROMO_100_VOUCHER_GIFT;

                    Misc.sendTranHisAndNotiBillPayGift(vertx
                            , DataUtil.strToInt(reqObj.phoneNumber)
                            , tranComment
                            , tranId
                            , giftValue
                            , gift
                            , notiCaption
                            , notiBody
                            , giftMessage
                            , tranDb);
                    //------------thong bao nhan qua neu la nhom 1
                    joReply.putNumber("error", 0);
                    joReply.putString("desc", "Thành công");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                } else {
                    joReply.putNumber("error", 1000);
                    joReply.putString("desc", "Khong insert database thanh cong _ gift 1");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
            }

        });
    }

    //END 0000000050

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void doAllPromoForVoucher(final Message<JsonObject> message
            , final JsonObject jsonConfig
            , final BillPayPromoObj reqObj
            , final BillPayPromosDb.Obj rowObj
            , final int numVoucher
            , final String gift_type_id
            , final String nameOfProgram
            , final Common.BuildLog log) {

        // Trả khuyến mãi
        // Them thong tin service id va so voucher vao core
        String group = reqObj.source + "_" + reqObj.serviceId + "_" + numVoucher;
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", nameOfProgram));

        keyValues.add(new Misc.KeyValue("group", group));

        final String agentName = jsonConfig.getString("agent", "cskh");
        final int timeForGift = jsonConfig.getInteger("timeforgift", 45);
        final long giftValue = numVoucher == 0 ? jsonConfig.getLong(StringConstUtil.OBCPromo.POINT, 100000) : jsonConfig.getLong("valueofgift", 50000);
        final int timeForActive = jsonConfig.getInteger("timeforactive", 20);
        final String giftTypeId = gift_type_id;

        //Tra thuong trong core
        giftManager.adjustGiftValue(agentName
                , reqObj.phoneNumber
                , giftValue
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error", error);
                log.add("desc", SoapError.getDesc(error));

//                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    final GiftType giftType = new GiftType();
                    giftType.setModelId(giftTypeId);

                    ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    Misc.KeyValue kv = new Misc.KeyValue();
                    kv.Key = "group";
                    kv.Value = reqObj.source;
                    keyValues.add(kv);
                    int endGiftTime = 0;
                    long modifyDate = 0;
                    //Neu chuong trinh khuyen mai la ocb
                    if (nameOfProgram.equalsIgnoreCase(StringConstUtil.OBCPromo.OCB) && numVoucher == 2) {
                        endGiftTime = timeForGift + timeForActive;
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        modifyDate = calendar.getTimeInMillis() + (timeForActive * 24 * 60 * 60 * 1000L);
                    } else {
                        endGiftTime = timeForGift;
                        modifyDate = System.currentTimeMillis();
                    }
                    Misc.KeyValue kv_1 = new Misc.KeyValue();
                    kv_1.Key = BillPayPromoConst.ACTIVE_TIME;
                    kv_1.Value = modifyDate + "";
                    keyValues.add(kv_1);
                    String note = reqObj.source + "_" + numVoucher + "";
                    //tao gift tren backend
                    giftManager.createLocalGiftForBillPayPromoWithDetailGift(reqObj.phoneNumber
                            , giftValue
                            , giftType
                            , promotedTranId
                            , agentName
                            , modifyDate
                            , endGiftTime
                            , keyValues, note, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            int err = jsonObject.getInteger("error", -1);
                            final long tranId = jsonObject.getInteger("tranId", -1);
                            final Gift gift = new Gift(jsonObject.getObject("gift"));
                            final String giftId = gift.getModelId();
                            log.add("desc", "tra thuong chuong trinh khuyen mai " + nameOfProgram + " bang gift");
                            log.add("err", err);

                            //------------tat ca thanh cong roi
                            if (err == 0) {

                                if (nameOfProgram.equalsIgnoreCase(StringConstUtil.OBCPromo.OCB) && numVoucher == 1) {
                                    giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {
                                                    gift.status = 3;
                                                    giveVoucherOneBankPromo(message, jsonConfig, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                                    return;
                                                }
                                            });
                                        }
                                    });
                                } else if (nameOfProgram.equalsIgnoreCase(StringConstUtil.OBCPromo.OCB) && numVoucher == 2) {
                                    giveVoucherTwoBankPromo(message, jsonConfig, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                    return;
                                } else if (nameOfProgram.equalsIgnoreCase(StringConstUtil.OBCPromo.OCB) && numVoucher == 0) {
                                    giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject jsonObject) {
                                            giftManager.useGift(reqObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {
                                                    gift.status = 3;
                                                    giveVoucher100BankPromo(message, jsonConfig, reqObj, rowObj, gift, tranId, promotedTranId, giftValue, timeForGift, timeForActive, log);
                                                    return;
                                                }
                                            });
                                        }
                                    });
                                    return;
                                } else {
                                    log.add("program", "chuong trinh nao do bi an");
                                    log.writeLog();
                                    return;
                                }
                            } else {
//                                joReply.putNumber("error", 1000);
//                                joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
//                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }
            }
        });
    }
}
