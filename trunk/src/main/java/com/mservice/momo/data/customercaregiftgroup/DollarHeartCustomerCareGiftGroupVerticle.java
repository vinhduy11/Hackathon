package com.mservice.momo.data.customercaregiftgroup;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.ReactiveDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.NotificationUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by concu on 11/24/15.
 */
public class DollarHeartCustomerCareGiftGroupVerticle extends Verticle {


    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private DollarHeartCustomerCareGiftGroupDb dollarHeartCustomerCareGiftGroupDb;
    private TransDb tranDb;
    private JsonObject joReactive;
    public ArrayList<String> phoneNumberList;
    private ReactiveDb reactiveDb;

    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.giftManager = new GiftManager(vertx, logger, glbCfg);
        dollarHeartCustomerCareGiftGroupDb = new DollarHeartCustomerCareGiftGroupDb(vertx, logger);
        tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        joReactive = glbCfg.getObject(StringConstUtil.JSON_NUMBER_OF_REACTIVE_CUSTOMER_GIFT);
        reactiveDb = new ReactiveDb(vertx, logger);
        //QUet dich vu Iron man de nhac nho user su dung qua
        phoneNumberList = new ArrayList<>();
        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final DollarHeartCustomerCareGiftGroupObj dollarHeartCustomerCareGiftGroupObj = new DollarHeartCustomerCareGiftGroupObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(dollarHeartCustomerCareGiftGroupObj.phoneNumber);
                log.add("phoneNumber", dollarHeartCustomerCareGiftGroupObj.phoneNumber);
                log.add("program", dollarHeartCustomerCareGiftGroupObj.program);
                log.add("agent", dollarHeartCustomerCareGiftGroupObj.agent);
                log.add("giftValue", dollarHeartCustomerCareGiftGroupObj.giftValue);
                log.add("giftTypeId", dollarHeartCustomerCareGiftGroupObj.giftTypeId);
                log.add("duration", dollarHeartCustomerCareGiftGroupObj.duration);

                if("".equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.phoneNumber) && "".equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.program)
                        && "".equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.giftTypeId))
                {
                    phoneNumberList.clear();
                    phoneNumberList = new ArrayList<>();
                    log.add("desc", "phone list da clear");
                    log.writeLog();
                    message.reply(new JsonObject().putString(StringConstUtil.DESCRIPTION, "clear").putNumber(StringConstUtil.ERROR, 0));
                    return;
                }
                else if("money".equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.giftTypeId))
                {
                    log.add("desc", "tra thuong tien mat cho user");
                    giveMoneyToUser(dollarHeartCustomerCareGiftGroupObj, log, message);
                    //Tra tien mat
                }
                else
                {
                    log.add("desc", "tra thuong gift cho user");
                    //Tra voucher
                    final int giftsOfProgram = joReactive.getInteger(dollarHeartCustomerCareGiftGroupObj.group);
                    JsonObject joFilter = new JsonObject();
                    joFilter.putString(colName.ReactiveDbCol.PHONE_NUMBER, dollarHeartCustomerCareGiftGroupObj.phoneNumber);
                    reactiveDb.countNumberOfRow(dollarHeartCustomerCareGiftGroupObj.group, joFilter, new Handler<Integer>() {
                        @Override
                        public void handle(Integer numberOfGift) {
                            if(numberOfGift < giftsOfProgram)
                            {
                                giveGiftToUser(dollarHeartCustomerCareGiftGroupObj, log, message);
                            }
                            else{
                                JsonObject joReply = new JsonObject();
                                joReply.putNumber("error", 1000);
                                joReply.putString("desc", "Số này đã có đủ quà theo qui định rồi nhé");
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }
                        }
                    });

                }

            }
        };
        vertx.eventBus().registerLocalHandler(AppConstant.DOLLAR_HEART_CUSTOMER_CARE_GROUP, myHandler);
    }

    private void giveMoneyToUser(final DollarHeartCustomerCareGiftGroupObj dollarHeartCustomerCareGiftGroupObj, final Common.BuildLog log, final Message message)
    {

        final JsonObject jsonReply = new JsonObject();
        log.add("desc", "giveMoneyToUser");
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = dollarHeartCustomerCareGiftGroupObj.program;
        Misc.adjustment(vertx, dollarHeartCustomerCareGiftGroupObj.agent, dollarHeartCustomerCareGiftGroupObj.phoneNumber, dollarHeartCustomerCareGiftGroupObj.giftValue,
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
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "core tra loi");
                            message.reply(jsonReply);
                            return;
                        }
                        //Yeu cau xong tien mat
                        //Luu qua trong Db va ban noti thong bao
                        giveMoney(message, dollarHeartCustomerCareGiftGroupObj, log, soapObjReply, jsonReply);
                    }
                });
    }

    private void giveMoney(final Message message, final DollarHeartCustomerCareGiftGroupObj dollarHeartCustomerCareGiftGroupObj, final Common.BuildLog log, final Common.SoapObjReply soapObjReply
    , final JsonObject joReply)
    {
        log.add("desc", "giveMoney");
        DollarHeartCustomerCareGiftGroupDb.Obj dollarObj = new DollarHeartCustomerCareGiftGroupDb.Obj();
        dollarObj.gift_id = "";
        dollarObj.gift_amount = 0;
        dollarObj.is_received_noti = false;
        dollarObj.program = dollarHeartCustomerCareGiftGroupObj.program;
        dollarObj.money_value = dollarHeartCustomerCareGiftGroupObj.giftValue;
        dollarObj.time = System.currentTimeMillis();
        dollarObj.phone_number = dollarHeartCustomerCareGiftGroupObj.phoneNumber;
        dollarObj.tranId = soapObjReply.tranId;
        dollarObj.group = dollarHeartCustomerCareGiftGroupObj.group;
        dollarObj.duration = dollarHeartCustomerCareGiftGroupObj.duration;
        dollarHeartCustomerCareGiftGroupDb.insert(dollarObj, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {
                fireMoneyNotiAndSendTranHist(message, dollarHeartCustomerCareGiftGroupObj, soapObjReply.tranId, log);
            }
        });
    }

    private void fireMoneyNotiAndSendTranHist(final Message<JsonObject> message, final DollarHeartCustomerCareGiftGroupObj dollarHeartCustomerCareGiftGroupObj
            , final long tranId
            , final Common.BuildLog log)
    {
        log.add("desc", "fireMoneyNotiAndSendTranHist");
        long endTime = 1000L * 60 * 60 * 24 * dollarHeartCustomerCareGiftGroupObj.duration + System.currentTimeMillis();
        final String noti_title = PromoContentNotification.REACTIVE_HEART_CUSTOMER_CARE_MONEY_NOTI_TITLE;


        final String noti_body = String.format(PromoContentNotification.REACTIVE_HEART_CUSTOMER_CARE_MONEY_NOTI_BODY, Misc.dateVNFormatWithDot(endTime));

        final String tran_comment = String.format(PromoContentNotification.REACTIVE_HEART_CUSTOMER_CARE_MONEY_COMMENT_TRANHIS, Misc.dateVNFormatWithDot(endTime));
        JsonObject joTranHis = new JsonObject();
        joTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.MPOINT_CLAIM_VALUE);
        joTranHis.putString(colName.TranDBCols.COMMENT, tran_comment);
        joTranHis.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        joTranHis.putNumber(colName.TranDBCols.AMOUNT, dollarHeartCustomerCareGiftGroupObj.giftValue);
        joTranHis.putNumber(colName.TranDBCols.STATUS, 4);
        joTranHis.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(dollarHeartCustomerCareGiftGroupObj.phoneNumber));
        joTranHis.putString(colName.TranDBCols.BILL_ID, "");
        joTranHis.putString(StringConstUtil.HTML, "");
        joTranHis.putNumber(colName.TranDBCols.IO, 1);
        Misc.sendingStandardTransHisFromJson(vertx, tranDb, joTranHis, new JsonObject());

        JsonObject joNoti = new JsonObject();
        joNoti.putString(StringConstUtil.StandardNoti.CAPTION, noti_title);
        joNoti.putString(StringConstUtil.StandardNoti.BODY, noti_body);
        joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, dollarHeartCustomerCareGiftGroupObj.phoneNumber);
        joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tranId);
        Misc.sendStandardNoti(vertx, joNoti);

        JsonObject joReply = new JsonObject();
        joReply.putNumber(StringConstUtil.ERROR, 0);
        joReply.putString(StringConstUtil.DESCRIPTION, "Thanh cong");
        message.reply(joReply);


    }

    private void giveGiftToUser(final DollarHeartCustomerCareGiftGroupObj dollarHeartCustomerCareGiftGroupObj, final Common.BuildLog log, final Message message)
    {
        log.add("desc", "giveGiftToUser");
        // Trả khuyến mãi
        // Them thong tin service id va so voucher vao core
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", dollarHeartCustomerCareGiftGroupObj.program));


        final JsonObject joReply = new JsonObject();
        keyValues.add(new Misc.KeyValue("group", dollarHeartCustomerCareGiftGroupObj.group));

        giftManager.adjustGiftValue(dollarHeartCustomerCareGiftGroupObj.agent
                , dollarHeartCustomerCareGiftGroupObj.phoneNumber
                , dollarHeartCustomerCareGiftGroupObj.giftValue
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {


                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                log.add("error", error);
                log.add("desc", SoapError.getDesc(error));

                joReply.putNumber("error", error);
                joReply.putString("desc", SoapError.getDesc(error));

                //tra thuong trong core thanh cong
                if (error == 0) {
                    //tao gift tren backend
                    final GiftType giftType = new GiftType();
                    final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    final Misc.KeyValue kv = new Misc.KeyValue();

                    final long modifyDate = System.currentTimeMillis();

                    //for (int i = 0; i < listVoucher.size(); i++) {
                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = dollarHeartCustomerCareGiftGroupObj.group;
                    keyValues.add(kv);
                    giftType.setModelId(dollarHeartCustomerCareGiftGroupObj.giftTypeId);
                    giftManager.createLocalGiftForBillPayPromoWithDetailGift(dollarHeartCustomerCareGiftGroupObj.phoneNumber
                            , dollarHeartCustomerCareGiftGroupObj.giftValue
                            , giftType
                            , promotedTranId
                            , dollarHeartCustomerCareGiftGroupObj.agent
                            , modifyDate
                            , dollarHeartCustomerCareGiftGroupObj.duration
                            , keyValues, dollarHeartCustomerCareGiftGroupObj.group, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            int err = jsonObject.getInteger("error", -1);
                            final long tranId = jsonObject.getInteger("tranId", -1);
                            final Gift gift = new Gift(jsonObject.getObject("gift"));
                            final String giftId = gift.getModelId().trim();
                            log.add("err", err);

                            //------------tat ca thanh cong roi
                            if (err == 0) {
                                giftManager.useGift(dollarHeartCustomerCareGiftGroupObj.phoneNumber, giftId, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonUseGift) {
                                        //Luu qua trong Db va ban noti thong bao
                                        ReactiveDb.Obj reactiveObj = new ReactiveDb.Obj();
                                        reactiveObj.phone_number = dollarHeartCustomerCareGiftGroupObj.phoneNumber;
                                        reactiveObj.giftType = giftType.getModelId();
                                        reactiveObj.gift_id = giftId;
                                        reactiveObj.duration = dollarHeartCustomerCareGiftGroupObj.duration;
                                        reactiveObj.gift_amount = dollarHeartCustomerCareGiftGroupObj.giftValue;
                                        reactiveObj.time = System.currentTimeMillis();
                                        reactiveObj.tranId = tranId;
                                        reactiveDb.insert(dollarHeartCustomerCareGiftGroupObj.group, reactiveObj, new Handler<Integer>() {
                                            @Override
                                            public void handle(Integer event) {
                                                giveVoucher(message, joReply, dollarHeartCustomerCareGiftGroupObj, gift, tranId, log, giftId);
                                            }
                                        } );
                                    }
                                });
                                return;
                            } else {
                                joReply.putNumber("error", err);
                                joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }
                        }
                    });

                    return;
                } else {
                    //tra thuong trong core khong thanh cong
                    log.add("desc", "Lỗi " + SoapError.getDesc(error));
                    log.add("Exception", "Exception " + SoapError.getDesc(error));
                    log.writeLog();
                    message.reply(joReply);
                    return;
                }
            }
        });
    }


    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    public void giveVoucher(final Message<JsonObject> message
            , final JsonObject joReply
            , final DollarHeartCustomerCareGiftGroupObj customerCareGiftGroupObj
            , final Gift gift
            , final long tranId
            , final Common.BuildLog log
            , final String giftId) {

        DollarHeartCustomerCareGiftGroupDb.Obj dollarObj = new DollarHeartCustomerCareGiftGroupDb.Obj();
        dollarObj.gift_id = giftId;
        dollarObj.gift_amount = customerCareGiftGroupObj.giftValue;
        dollarObj.is_received_noti = false;
        dollarObj.program = customerCareGiftGroupObj.program;
        dollarObj.money_value = 0;
        dollarObj.time = System.currentTimeMillis();
        dollarObj.phone_number = customerCareGiftGroupObj.phoneNumber;
        dollarObj.tranId = tranId;
        dollarObj.group = customerCareGiftGroupObj.group;
        dollarObj.duration = customerCareGiftGroupObj.duration;
        dollarHeartCustomerCareGiftGroupDb.insert(dollarObj, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {

                fireNotiAndSendTranHist(message, customerCareGiftGroupObj, joReply, gift, tranId, log);
            }
        });
    }

    private void fireNotiAndSendTranHist(final Message<JsonObject> message, final DollarHeartCustomerCareGiftGroupObj dollarHeartCustomerCareGiftGroupObj, final JsonObject joReply
            , final Gift gift
            , final long tranId
            , final Common.BuildLog log)
    {


//        final long gift_value = group_dollar.equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.group) ? group_dollar_value : group_heart_value;


        HashMap<String, String> hashMapInfo = NotificationUtils.getReactiveInfo(dollarHeartCustomerCareGiftGroupObj.group);
        long time = hashMapInfo.size() > 0 && hashMapInfo.containsKey(StringConstUtil.GIFT_ENDDATE) ? Long.parseLong(hashMapInfo.get(StringConstUtil.GIFT_ENDDATE)) : 0;
        long endtime = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * time;
        final String gift_value_str = hashMapInfo.size() > 0 && hashMapInfo.containsKey(StringConstUtil.GIFT_AMOUNT) ? hashMapInfo.get(StringConstUtil.GIFT_AMOUNT) : "0";
        final String noti_title = hashMapInfo.size() > 0 && hashMapInfo.containsKey(StringConstUtil.TITLE_NOTI) ? hashMapInfo.get(StringConstUtil.TITLE_NOTI) : "";
        final String noti_body = hashMapInfo.size() > 0 && hashMapInfo.containsKey(StringConstUtil.CONTENT_NOTI) ? String.format(hashMapInfo.get(StringConstUtil.CONTENT_NOTI),Misc.dateVNFormatWithDot(endtime)) : "";
        final String tran_comment = hashMapInfo.size() > 0 && hashMapInfo.containsKey(StringConstUtil.CONTENT_TRAN) ? String.format(hashMapInfo.get(StringConstUtil.CONTENT_TRAN),Misc.dateVNFormatWithDot(endtime)) : "";
        final String gift_message = hashMapInfo.size() > 0 && hashMapInfo.containsKey(StringConstUtil.CONTENT_GIFT) ? String.format(hashMapInfo.get(StringConstUtil.CONTENT_GIFT),Misc.dateVNFormatWithDot(endtime)) : "";

        final long gift_value = Long.parseLong(gift_value_str);
//        final String noti_title = PromoContentNotification.PRU_CASTROL_GIFT_NOTI_TITLE;
//
//        final String noti_body = group_dollar.equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.group) ?
//                String.format(PromoContentNotification.PRU_CASTROL_GIFT_NOTI_BODY, Misc.dateVNFormatWithDot(endtime))
//                : String.format(PromoContentNotification.PRU_CASTROL_GIFT_NOTI_BODY_2, Misc.dateVNFormatWithDot(endtime));
//
//        final String tran_comment = group_dollar.equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.group) ?
//                String.format(PromoContentNotification.PRU_CASTROL_GIFT_COMMENT_TRANHIS, Misc.dateVNFormatWithDot(endtime))
//                : String.format(PromoContentNotification.PRU_CASTROL_GIFT_COMMENT_TRANHIS_2, Misc.dateVNFormatWithDot(endtime));
//        final String gift_message = group_dollar.equalsIgnoreCase(dollarHeartCustomerCareGiftGroupObj.group) ?
//                String.format(PromoContentNotification.PRU_CASTROL_GIFT_COMMENT_TRANHIS, Misc.dateVNFormatWithDot(endtime))
//                : String.format(PromoContentNotification.PRU_CASTROL_GIFT_COMMENT_TRANHIS_2, Misc.dateVNFormatWithDot(endtime));

        JsonObject jsonSearch = new JsonObject();
        jsonSearch.putString(colName.DollarHeartCustomerCareGiftGroup.NUMBER, dollarHeartCustomerCareGiftGroupObj.phoneNumber);
        jsonSearch.putNumber(colName.DollarHeartCustomerCareGiftGroup.MONEY_VALUE, 0);
        if(!phoneNumberList.contains(dollarHeartCustomerCareGiftGroupObj.phoneNumber))
        {
            phoneNumberList.add(dollarHeartCustomerCareGiftGroupObj.phoneNumber);
            log.add("obj", "null");
            log.add("desc", "ban noti");
            log.add("desc", dollarHeartCustomerCareGiftGroupObj.phoneNumber);

            Misc.sendTranHisAndNotiBillPayGift(vertx
                    , DataUtil.strToInt(dollarHeartCustomerCareGiftGroupObj.phoneNumber)
                    , tran_comment
                    , tranId
                    , gift_value
                    , gift
                    , noti_title
                    , noti_body
                    , gift_message
                    , tranDb);

        }
        //------------thong bao nhan qua neu la nhom 1
        joReply.putNumber("error", 0);
        joReply.putString("desc", "Thành công");
        message.reply(joReply);
        log.writeLog();
        return;


    }
}
