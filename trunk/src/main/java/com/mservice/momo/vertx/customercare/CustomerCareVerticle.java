package com.mservice.momo.vertx.customercare;

import com.mservice.momo.data.CustomCareDb;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;

/**
 * Created by anhkhoa on 01/04/2015.
 */
public class CustomerCareVerticle extends Verticle {

    private final String GROUP_ONE = "1";
    private final String GROUP_TWO = "2";
    private final String GROUP_THREE = "3";
    private final String GROUP_FOUR = "4";
    private final String GROUP_FIVE = "5";
    private final String GROUP_SIX = "6";
    private final int NOT_ERROR = 0;
    private CustomCareDb customerCareDb = null;
    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private TransDb tranDb;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        this.giftManager = new GiftManager(vertx, logger, glbCfg);
        this.customerCareDb = new CustomCareDb(vertx, logger);

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {

                JsonObject reqJson = message.body();
                final CustomCareObj ccObj = new CustomCareObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(ccObj.phoneNumber);
                log.add("trantype", ccObj.tranType);
                log.add("tranid", ccObj.tranId);

                customerCareDb.findOne(ccObj.phoneNumber, new Handler<CustomCareDb.Obj>() {
                    @Override
                    public void handle(final CustomCareDb.Obj obj) {

                        final JsonObject joReply = new JsonObject();
                        //so dien thoai khong nam trong chuong trinh khuyen mai
                        if (obj == null) {
                            joReply.putString("desc", "SDT khong nam trong chuong trinh khuyen mai");
                            message.reply(joReply);

                            log.add("desc", "So dien thoai " + ccObj.phoneNumber + " khong nam trong danh sach khuyen mai");
                            log.writeLog();
                            return;
                        }

                        //-----------------chua kich hoat nhan qua
                        if (obj.enable == false) {
                            log.add("desc", "So dien thoai " + ccObj.phoneNumber + " chua duoc kich hoat de nhan khuyen mai");

                            log.writeLog();
                            if(ccObj.tranType == 0)
                            {
                                joReply.putNumber("error", -1);
                                joReply.putString("desc", "So dien thoai " + ccObj.phoneNumber + " chua duoc kich hoat de nhan khuyen mai");
                                message.reply(joReply);
                            }
                            return;
                        }

                        //----------- het chuong trinh
                        long currentTime = System.currentTimeMillis();
                        if (currentTime < obj.dateFrom || obj.dateTo < currentTime) {
                            log.add("desc", "Het thoi gian khuyen mai");
                            log.writeLog();
                            if(ccObj.tranType == 0)
                            {
                                joReply.putNumber("error", -1);
                                joReply.putString("desc", "Het thoi gian khuyen mai");
                                message.reply(joReply);
                            }
                            return;
                        }

                        log.add("group", obj.group);
                        log.add("groupDesc", obj.groupDesc);

                        //-------------da tra thuong roi --> khong tra nua
                        if (obj.promoCount > 0) {
                            log.add("desc", "Da khuyen mai roi, so lan da khuyen mai: " + obj.promoCount);
                            log.writeLog();
                            if(ccObj.tranType == 0)
                            {
                                joReply.putNumber("error", -1);
                                joReply.putString("desc", "Da khuyen mai roi, so lan da khuyen mai: " + obj.promoCount);
                                message.reply(joReply);
                            }
                            return;
                        }
                        log.add("tranType_beforeCheckPromo", ccObj.tranType);
                        log.add("tranId_beforeCheckPromo", ccObj.tranId);
                        //-------------khong tra thuong cho cac giao dich M2M
                        int M2M_VALUE = 6;
                        int TRANSFER_MONEY_TO_PLACE_VALUE = 13;
                        int BANK_IN_VALUE = 1;
                        int BANK_NET_VERIFY_OTP_VALUE = 11;
                        int MOMO_TO_BANK_MANUAL_VALUE = 26;
                        int BANK_OUT_VALUE = 2;

                        if ((ccObj.tranType == 0 && ccObj.tranId != 0) || ccObj.tranType == M2M_VALUE || ccObj.tranType == TRANSFER_MONEY_TO_PLACE_VALUE
                                || ccObj.tranType == BANK_IN_VALUE || ccObj.tranType == BANK_NET_VERIFY_OTP_VALUE
                        || ccObj.tranType == MOMO_TO_BANK_MANUAL_VALUE || ccObj.tranType == BANK_OUT_VALUE
                                ) {
                            log.add("desc", "Giao dich la bay ba nen khong co khuyen mai");
                            log.writeLog();
                            if(ccObj.tranType == 0)
                            {
                                joReply.putNumber("error", -1);
                                joReply.putString("desc", "Giao dich la M2M nen khong co khuyen mai");
                                message.reply(joReply);
                            }
                            return;
                        }

                        // rule nhom 2
                        //Khi phát sinh 03 giao dịch (ngoại trừ nạp/rút/chuyển MoMo) trong vòng 30 ngày (Từ ngày 10/04/2015 đến ngày 10/05/2015)
                        if(GROUP_TWO.equalsIgnoreCase(obj.group) &&  (obj.transCount < 2)){

                            //---tang so luong giao dich da thuc hien len 1
                            JsonObject joUpdate = new JsonObject();
                            joUpdate.putNumber(colName.CustomCarePromo.transCount, (obj.transCount + 1));
                            customerCareDb.updatePartial(ccObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                }
                            });

                            log.add("trancount",obj.transCount);
                            log.add("desc", "Chua du so luong giao dich de tra thuong");
                            log.writeLog();
                            if(ccObj.tranType == 0)
                            {
                                joReply.putNumber("error", -1);
                                joReply.putString("desc", "Chua du so luong giao dich de tra thuong");
                                message.reply(joReply);
                            }
                            return;
                        }

                        //-------------rule nhom 4
                        //Khi phát sinh giao dịch topup đầu tiên trong vòng 30 ngày  (Từ ngày 10/04/2015 đến ngày 10/05/2015)
                        if(GROUP_FOUR.equalsIgnoreCase(obj.group) && ccObj.tranType != 0 && ccObj.tranType != MomoProto.TranHisV1.TranType.TOP_UP_VALUE){
                            log.add("group", obj.group);
                            log.add("desc", "Khong tra thuong vi khong phai la giao dich TOPUP");
                            log.writeLog();
                            if(ccObj.tranType == 0)
                            {
                                joReply.putNumber("error", -1);
                                joReply.putString("desc", "Khong tra thuong vi khong phai la giao dich TOPUP");
                                message.reply(joReply);
                            }
                            return;
                        }

                        if(GROUP_THREE.equalsIgnoreCase(obj.group))
                        {
                            doPromoForGroup03(message, joReply, ccObj, obj, log);
                        }
                        else{
                            doPromoForGroup(message, joReply, ccObj, obj, log);
                        }

                    }
                });

            }
        };

        vertx.eventBus().registerLocalHandler(CustomCareObj.CUSTOMER_CARE_BUSS_ADDRESS,myHandler);

    }

    //Thuc hien trao thuong
    private void doPromoForGroup(final Message<JsonObject> message
                                    ,final JsonObject joReply
                                    , final CustomCareObj reqObj
                                    , final CustomCareDb.Obj rowObj
                                    ,final Common.BuildLog log){

        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", "cuscare"));
        keyValues.add(new Misc.KeyValue("group", rowObj.group));

        //Tra thuong trong core
        giftManager.adjustGiftValue(rowObj.momoAgent
                , reqObj.phoneNumber
                , rowObj.promoValue
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error", error);
                log.add("desc", SoapError.getDesc(error));

                joReply.putNumber("error",error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    final GiftType giftType = new GiftType();
                    giftType.setModelId(rowObj.giftTypeId);

                    ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    Misc.KeyValue kv = new Misc.KeyValue();
                    kv.Key = "group";
                    kv.Value = rowObj.group;
                    keyValues.add(kv);

                    //tao gift tren backend
                    giftManager.createLocalGift(reqObj.phoneNumber
                            , rowObj.promoValue
                            , giftType
                            , promotedTranId
                            , rowObj.momoAgent
                            , rowObj.duration
                            , keyValues, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            int err = jsonObject.getInteger("error", -1);
                            long tranId = jsonObject.getInteger("tranId", -1);
                            Gift gift = new Gift(jsonObject.getObject("gift"));

                            log.add("desc", "tra thuong chuong trinh customer care bang gift");
                            log.add("err", err);

                            //------------tat ca thanh cong roi
                            if (err == 0) {

                                //-------cap nhat mongo db ghi nhan da tra thuong
                                JsonObject joUpdate = new JsonObject();
                                joUpdate.putString(colName.CustomCarePromo.proTimeVn,Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                                joUpdate.putNumber(colName.CustomCarePromo.promoCount, (rowObj.promoCount + 1));
                                joUpdate.putNumber(colName.CustomCarePromo.tranId,promotedTranId);
                                joUpdate.putNumber(colName.CustomCarePromo.transCount,(rowObj.transCount + 1));

                                customerCareDb.updatePartial(rowObj.number, joUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {}
                                });

                                log.add("group", rowObj.group);
                                String giftMessage = PromoContentNotification.GIFT_MESSAGE_CUSTOMER_CARE;
                                String notiCaption = PromoContentNotification.NOTIFY_CAPTION;
                                String tranComment = String.format(PromoContentNotification.TRANS_COMMENT_CUSTOMER_CARE,Misc.formatAmount(rowObj.promoValue).replace(",",".") );
                                String notiBody = String.format(PromoContentNotification.NOTIFY_BODY_CUSTOMER_CARE,Misc.formatAmount(rowObj.promoValue).replace(",","."));

                                Misc.sendTranHisAndNotiGift(vertx
                                        , DataUtil.strToInt(reqObj.phoneNumber)
                                        , tranComment
                                        , tranId
                                        , rowObj.promoValue
                                        , gift
                                        , notiCaption
                                        , notiBody
                                        , giftMessage
                                        , tranDb);

                                //------------thong bao nhan qua neu la nhom 3
                                if(GROUP_THREE.equalsIgnoreCase(rowObj.group)){
                                    final Notification noti = new Notification();
                                    String tmp = "Quý khách vừa được tặng voucher %sđ, Ví MoMo chân thành cảm ơn Quý khách đã sử dụng và mong tiếp tục được ủng hộ trong thời gian tới.";
                                    noti.caption="Đăng ký với Vietcombank để nhận KM";
                                    noti.receiverNumber = DataUtil.strToInt(rowObj.number);
                                    noti.sms = "";
                                    noti.tranId= System.currentTimeMillis(); // ban tren toan he thong
                                    noti.priority = 2;
                                    noti.time =  System.currentTimeMillis();
                                    noti.category = 0;
                                    noti.body = String.format(tmp,Misc.formatAmount(rowObj.promoValue).replace(",","."));
                                    noti.type = MomoProto.NotificationType.NOTI_VOUCHER_VIEW_VALUE;
                                    noti.htmlBody = noti.body;
                                    noti.status = Notification.STATUS_DETAIL;
                                    Misc.sendNoti(vertx,noti);
                                }

                                joReply.putString("desc", "Thành công");
                                message.reply(joReply);
                                log.writeLog();

                            } else {

                                joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                message.reply(joReply);
                                log.writeLog();
                            }
                        }
                    });
                } else {

                    //tra thuong trong core khong thanh cong
                    log.add("desc", "Tra qua cho chuong trinh customer care loi");
                    log.writeLog();
                    joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                    message.reply(joReply);
                }
            }
        });
    }



    private void doPromoForGroup03(final Message<JsonObject> message
            ,final JsonObject joReply
            , final CustomCareObj reqObj
            , final CustomCareDb.Obj rowObj
            ,final Common.BuildLog log){

        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", "cuscare"));
        keyValues.add(new Misc.KeyValue("group", rowObj.group));


        final GiftType giftType = new GiftType();
        giftType.setModelId(rowObj.giftTypeId);

        Misc.KeyValue kv = new Misc.KeyValue();
        kv.Key = "group";
        kv.Value = rowObj.group;
        keyValues.add(kv);

        //tao gift tren backend
        giftManager.createLocalGift(reqObj.phoneNumber
                , rowObj.promoValue
                , giftType
                , rowObj.tranId
                , rowObj.momoAgent
                , rowObj.duration
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                int err = jsonObject.getInteger("error", -1);
                long tranId = jsonObject.getInteger("tranId", -1);
                Gift gift = new Gift(jsonObject.getObject("gift"));

                log.add("desc", "tra thuong chuong trinh customer care bang gift");
                log.add("err", err);

                //------------tat ca thanh cong roi
                if (err == 0) {

                    //-------cap nhat mongo db ghi nhan da tra thuong
                    JsonObject joUpdate = new JsonObject();
                    joUpdate.putString(colName.CustomCarePromo.proTimeVn, Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                    joUpdate.putNumber(colName.CustomCarePromo.promoCount, (rowObj.promoCount + 1));
                    joUpdate.putNumber(colName.CustomCarePromo.tranId, rowObj.tranId);
                    joUpdate.putNumber(colName.CustomCarePromo.transCount, (rowObj.transCount + 1));

                    customerCareDb.updatePartial(rowObj.number, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                        }
                    });

                    log.add("group", rowObj.group);
                    String giftMessage = PromoContentNotification.GIFT_MESSAGE_CUSTOMER_CARE;
                    String notiCaption = PromoContentNotification.NOTIFY_CAPTION;
                    String tranComment = String.format(PromoContentNotification.TRANS_COMMENT_CUSTOMER_CARE, Misc.formatAmount(rowObj.promoValue).replace(",", "."));
                    String notiBody = String.format(PromoContentNotification.NOTIFY_BODY_CUSTOMER_CARE, Misc.formatAmount(rowObj.promoValue).replace(",", "."));

//                    Misc.sendTranHisAndNotiGift(vertx
//                            , DataUtil.strToInt(reqObj.phoneNumber)
//                            , tranComment
//                            , tranId
//                            , rowObj.promoValue
//                            , gift
//                            , notiCaption
//                            , notiBody
//                            , giftMessage
//                            , tranDb);

                    //------------thong bao nhan qua neu la nhom 3
//                    if (GROUP_THREE.equalsIgnoreCase(rowObj.group)) {
//                        final Notification noti = new Notification();
//                        String tmp = "Quý khách vừa được tặng voucher %sđ, Ví MoMo chân thành cảm ơn Quý khách đã sử dụng và mong tiếp tục được ủng hộ trong thời gian tới.";
//                        noti.caption = "Đăng ký với Vietcombank để nhận KM";
//                        noti.receiverNumber = DataUtil.strToInt(rowObj.number);
//                        noti.sms = "";
//                        noti.tranId = System.currentTimeMillis(); // ban tren toan he thong
//                        noti.priority = 2;
//                        noti.time = System.currentTimeMillis();
//                        noti.category = 0;
//                        noti.body = String.format(tmp, Misc.formatAmount(rowObj.promoValue).replace(",", "."));
//                        noti.type = MomoProto.NotificationType.NOTI_VOUCHER_VIEW_VALUE;
//                        noti.htmlBody = noti.body;
//                        noti.status = Notification.STATUS_DETAIL;
//                        Misc.sendNoti(vertx, noti);
//                    }

                    joReply.putString("desc", "Thành công");
                    message.reply(joReply);
                    log.writeLog();

                } else {
                    joReply.putString("desc", "Lỗi CustomerCareVerticle");
                    message.reply(joReply);
                    log.writeLog();
                }
            }
        });

    }
}

