package com.mservice.momo.vertx.promotion_server;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.promotion.PromotionCountControlDb;
import com.mservice.momo.data.promotion.PromotionDeviceControlDb;
import com.mservice.momo.data.promotion.PromotionErrorControlDb;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 7/24/16.
 */
public class MainPromotionVerticle extends Verticle implements PromotionInterface {

    protected JsonObject glbConfig;
    protected Logger logger;
    protected PromotionDeviceControlDb promotionDeviceControlDb;
    protected PromotionCountControlDb promotionCountControlDb;
    protected PromotionErrorControlDb promotionErrorControlDb;
    protected GiftManager giftManager;
    protected TransDb transDb;
    protected GiftDb giftDb;
    @Override
    public void start() {
        glbConfig = container.config();
        logger = container.logger();
        promotionDeviceControlDb = new PromotionDeviceControlDb(vertx, logger);
        promotionCountControlDb = new PromotionCountControlDb(vertx, logger);
        promotionErrorControlDb = new PromotionErrorControlDb(vertx, logger);
        giftManager = new GiftManager(vertx, logger, glbConfig);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbConfig);
        giftDb = new GiftDb(vertx, logger);
    }


    @Override
    public void checkDevice(String deviceInfo, String os, int appCode, String program, int phoneNumber, String imei, boolean checkGmail, boolean checkSim, String className, Common.BuildLog log, final Handler<JsonObject> callBack) {
        log.setPhoneNumber("0" + phoneNumber);
        getExtraKeyFromApp(program, phoneNumber, deviceInfo, log, os, imei, checkGmail, checkSim, className, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joResult) {
                callBack.handle(joResult);
            }
        });
    }

    @Override
    public void checkCountPromotion(final int maxCount, int phoneNumber, String program, final String className, final Common.BuildLog log, final Handler<Boolean> callback) {
        addLog("info", "check count", className, log);
        promotionCountControlDb.findAndIncCountUser(phoneNumber, program, new Handler<PromotionCountControlDb.Obj>() {
            @Override
            public void handle(PromotionCountControlDb.Obj promoCntObj) {
                if(promoCntObj == null || promoCntObj.count > maxCount)
                {
                    addLog("error", "promoCnt " + (promoCntObj == null ? "is null" : promoCntObj.count + ""), className, log);
                    callback.handle(false);
                }
                else {
                    addLog("info", "promoCnt " + promoCntObj.count, className, log);
                    callback.handle(true);
                }
            }
        });
    }

    @Override
    public void saveErrorPromotionDescription(String program, int number, String desc, int errorCode, String deviceInfo, Common.BuildLog log, String className) {
        addLog("info", "insert error " + number, className, log);
        PromotionErrorControlDb.Obj promotionErrorObj = new PromotionErrorControlDb.Obj();
        promotionErrorObj.phone = number;
        promotionErrorObj.deviceInfo = deviceInfo;
        promotionErrorObj.desc = desc;
        promotionErrorObj.error_code = errorCode;
        promotionErrorObj.program = program;
        promotionErrorObj.time = System.currentTimeMillis();
        promotionErrorControlDb.insert(program, promotionErrorObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {

            }
        });
    }

    @Override
    public void callBack(int error, String description, Notification notification, Common.BuildLog log, Message msg, String className, JsonObject joExtra) {
        JsonObject joCallback = new JsonObject();
        joCallback.putNumber(StringConstUtil.PromotionField.ERROR, error);
        joCallback.putString(StringConstUtil.PromotionField.DESCRIPTION, description);
        JsonObject joNoti = notification == null ? null : notification.toFullJsonObject();
        joCallback.putObject(StringConstUtil.PromotionField.NOTIFICATION, joNoti);
        joCallback.putObject(StringConstUtil.PromotionField.JSON_EXTRA, joExtra);
        addLog("info", "callback " + joCallback, className, log);
        log.writeLog();
        msg.reply(joCallback);
        return;
    }

    protected String checkOS(String extraKey)
    {
        String os = extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL).length > 1 ? StringConstUtil.ANDROID_OS : StringConstUtil.IOS_OS;
        return os;
    }

    private void getExtraKeyFromApp(final String program, final int phoneNumber, final String extraKey, final Common.BuildLog log, String os, final String imei, boolean checkGmail, boolean checkSim, final String className, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();
        addLog("extra key ", extraKey, className, log);
        String extraKeyAndroid = "";
        os = "".equalsIgnoreCase(os) ? checkOS(extraKey) : os;
        addLog("Info ", "OS PHONE " + os, className, log);
        if(StringConstUtil.ANDROID_OS.equalsIgnoreCase(os)) //if os of device is android
        {
            final String[] extraKeyArr = extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL); //get device info from app [0]: device [1]: sim info
            addLog("extra keyArr length ", extraKey, className, log);
            extraKeyAndroid = extraKeyArr.length > 1 ? extraKeyArr[0] : extraKey; //info device
            addLog("extraKeyAndroid ", extraKeyAndroid, className, log);
            String extraKeyTemp = extraKeyArr.length > 1 ? extraKeyArr[1] : ""; //info sim
            if(!"".equalsIgnoreCase(extraKeyTemp) && checkSim) //info sim is not blank
            {
                int count = 0;
                boolean isKilled = false;
                String []extraKeyTempArr = extraKeyTemp.split(MomoMessage.BELL);
                for(int i = 0; i < extraKeyTempArr.length; i++)
                {
                    if("0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())) //This valued is received from virtual machine (BlueStack)
                    {
                        isKilled = true;
                        break;
                    }
                    if(!"XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) && !"-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) && !"".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())
                            && !"0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())) //If device does not get any info like that => it's OK
                    {
                        addLog("info ", "Thiết bị này có sim", className, log);
                        count = count + 1;
                    }
                }
                if(isKilled) //If device is virtual machine
                {
                    joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5002);
                    joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5002));
                    addLog("error ", "Thiết bị này dùng máy ảo nên không được tham gia chương trình", className, log);
                    callback.handle(joReply);
                    return;
                }
                else if(count < 2) //If device does not have enough info => pay bonus later after checking
                {
                    joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5007);
                    joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5007));
                    addLog("error ", "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn", className, log);
                    callback.handle(joReply);
                    return;
                }
            }
        }

        String extraKeyFinal = "".equalsIgnoreCase(extraKeyAndroid) ? extraKey : extraKeyAndroid;
        final String[] address_tmp = extraKeyFinal.split(MomoMessage.BELL);
        addLog("size address tmp ", address_tmp.length + "", className, log);
        if (!os.equalsIgnoreCase("ios") && address_tmp.length == 0) {
            joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5006);
            joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5006));
            callback.handle(joReply);
        } else {
            final AtomicInteger integer = new AtomicInteger(address_tmp.length);
            final AtomicInteger empty = new AtomicInteger(0);
            if (!"".equalsIgnoreCase(os) && !os.equalsIgnoreCase("ios")) {
                addLog("os ", "android " + phoneNumber, className, log);
                takeNoteDeviceInfo(program, className, phoneNumber, log, joReply, address_tmp, integer, empty, extraKeyFinal, checkGmail, checkSim, callback);
            }
            else if (os.equalsIgnoreCase("ios")) {
                final PromotionDeviceControlDb.Obj deviceObj = new PromotionDeviceControlDb.Obj();
                deviceObj.phoneNumber = phoneNumber;
                deviceObj.id = imei;
                promotionDeviceControlDb.insert(deviceObj, program, new Handler<Integer>() {
                    @Override
                    public void handle(Integer result) {
                        if (result == 0) {
                            joReply.putNumber(StringConstUtil.PromotionField.ERROR, 0);
                            joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "GOOD");
                            callback.handle(joReply);
                        } else {
                            JsonObject joFilter = new JsonObject().putString(PromotionColName.PromotionDeviceControlCol.ID, deviceObj.id);
                            promotionDeviceControlDb.searchWithFilter(program, joFilter, new Handler<ArrayList<PromotionDeviceControlDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<PromotionDeviceControlDb.Obj> listDevices) {
                                    int phoneDup = 0;
                                    if (listDevices.size() > 0) {
                                        phoneDup = listDevices.get(0).phoneNumber;
                                    }
                                    joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5004);
                                    joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5004).replaceAll("phone1", phoneNumber + "").replaceAll("phone2", phoneDup + "")); //"Thiết bị IOS của số điện thoại " + phoneNumber + " đã được trả thưởng cho số điện thoại " + phoneDup);
                                    addLog("os ", "android " + phoneNumber, className, log);
                                    addLog("error ", "Thiết bị IOS của số điện thoại " + phoneNumber + " đã được trả thưởng cho số điện thoại " + phoneDup, className, log);
                                    callback.handle(joReply);
                                }
                            });
                        }
                    }
                });
            } else {
                addLog("info ", "Khong ton tai thiet bi nay" + phoneNumber, className, log);
                joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5005);
                joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5005));
                callback.handle(joReply);
            }
        }
    }

    private void takeNoteDeviceInfo(final String program, final String className, final int phoneNumber, final Common.BuildLog log, final JsonObject joReply, final String[] address_tmp, final AtomicInteger integer, final AtomicInteger empty, final String extraKey, final boolean checkGmail, final boolean checkSim, final Handler<JsonObject> callback) {
        vertx.setPeriodic(200L, new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                int position = integer.decrementAndGet();
                if (position < 0) {
                    addLog("info ", "position " + position, className, log);
                    vertx.cancelTimer(event);
                    if (empty.intValue() > 0 && checkSim) {
                        addLog("info ", "check SIM empty.intValue() != address_tmp.length", className, log);
                        joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5007);
                        joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5007));
                    }
                    else if(!checkSim && empty.intValue() > 1)
                    {
                        addLog("info ", "DONE check SIM empty.intValue() != address_tmp.length", className, log);
                        joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5007);
                        joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5007));
                    }
                    else {
                        addLog("info ", "data is enough => GOOD", className, log);
                        joReply.putNumber(StringConstUtil.PromotionField.ERROR, 0);
                        joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, "GOOD");
                    }
                    callback.handle(joReply);
                    return;
                }
                else {
                    if (address_tmp[position].equalsIgnoreCase("") || address_tmp[position].equalsIgnoreCase("XXX")) {
                        addLog("info ", "item " + address_tmp[position] , className, log);
                        empty.incrementAndGet();
                    }
//                    else if(address_tmp[position].equalsIgnoreCase("XXX")) {
//                        vertx.cancelTimer(event);
//                        addLog("error ", "Loi insert android data user bi xxx", className , log);
//                        joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5003);
//                        joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5003));
//                        callback.handle(joReply);
//                    }
                    else if(address_tmp[position].trim().equalsIgnoreCase("02:00:00:00:00:00"))
                    {
                        addLog("info ", address_tmp[position], className, log);
                        addLog("info ", "ANDROID 6 nen bo qua MAC ADDRESS", className, log);
                    }
                    else if(address_tmp[position].trim().contains("@") && !checkGmail)
                    {
                        addLog("info ", address_tmp[position] , className, log);
                        addLog("info ", "Khong luu thong tin gmail" , className, log);
                    }
                    else {
                        addLog("info ", address_tmp[position] , className, log);
                        final PromotionDeviceControlDb.Obj deviceDataUserObj = new PromotionDeviceControlDb.Obj();
                        deviceDataUserObj.id = address_tmp[position].toString().trim();
                        deviceDataUserObj.phoneNumber = phoneNumber;
                        promotionDeviceControlDb.insert(deviceDataUserObj, program, new Handler<Integer>() {
                            @Override
                            public void handle(Integer resultInsert) {
                                if (resultInsert != 0) {
                                    vertx.cancelTimer(event);
                                    JsonObject joFilter = new JsonObject().putString(PromotionColName.PromotionDeviceControlCol.ID, deviceDataUserObj.id);
                                    promotionDeviceControlDb.searchWithFilter(program, joFilter, new Handler<ArrayList<PromotionDeviceControlDb.Obj>>() {
                                        @Override
                                        public void handle(ArrayList<PromotionDeviceControlDb.Obj> listDevices) {
                                            int phoneDup = 0;
                                            if(listDevices.size() > 0)
                                            {
                                                phoneDup = listDevices.get(0).phoneNumber;
                                            }
                                            addLog("error ", "Loi insert android data user", className, log);
                                            joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5008);
                                            joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5008).replaceAll("phone1", phoneNumber + "").replaceAll("phone2", phoneDup + ""));
                                            callback.handle(joReply);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    protected void addLog(String title, String desc, String className, Common.BuildLog log)
    {
        log.add(title + " | " + className, desc);
    }

    protected void sendNotiViaCloud(String title, String content, String phoneNumber)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.caption = title;// "Nhận thưởng quà khuyến mãi";
        noti.body = content;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = 0L;
        noti.time = new Date().getTime();
        noti.extra = new JsonObject().toString();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        Misc.sendPromotionPopupNoti(vertx, noti);
    }

    protected void sendNotiViaBroadcast(String title, String content, String phoneNumber)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.caption = title;// "Nhận thưởng quà khuyến mãi";
        noti.body = content;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = 0L;
        noti.time = new Date().getTime();
        noti.extra = new JsonObject().toString();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        Misc.sendNotiFromTool(vertx, noti);
    }


    protected void sendNotiViaBroadcastWithUrl(String title, String content, String url, String phoneNumber)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.caption = title;// "Nhận thưởng quà khuyến mãi";
        noti.body = content;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = 0L;
        noti.time = new Date().getTime();
        noti.extra = new JsonObject().putString(StringConstUtil.RedirectNoti.URL, url).toString();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        Misc.sendNotiFromTool(vertx, noti);
    }

    protected void sendNotiViaCloudWithUrl(String title, String content, String url, String phoneNumber)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.caption = title;// "Nhận thưởng quà khuyến mãi";
        noti.body = content;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = 0L;
        noti.time = new Date().getTime();
        noti.extra = new JsonObject().toString();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        noti.extra = new JsonObject().putString(StringConstUtil.RedirectNoti.URL, url).toString();
        Misc.sendPromotionPopupNoti(vertx, noti);
    }
    protected Notification buildPopupGiftNotification(String title, String content, int type, int screenId, long giftTranId, String serviceId, Gift gift, long totalAmount, String phoneNumber, String partnerName)
    {
        Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
        noti.caption = title;// "Nhận thưởng quà khuyến mãi";
        noti.body = content;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = giftTranId;
        noti.time = new Date().getTime();
        noti.extra = new JsonObject()
                .putString("giftId", gift.getModelId())
                .putString("giftTypeId", gift.typeId)
                .putString("amount", String.valueOf(totalAmount))
                .putString("sender", partnerName)
                .putString("senderName", "MoMo")
                .putString("msg", content)
                .putNumber("status", gift.status)
                .putString("serviceid", serviceId)
                .putNumber("screenId", screenId)
                .putNumber("type", type)
                .putString("imageUrl", "")
                .toString();
        noti.receiverNumber = DataUtil.strToInt(phoneNumber);
        return noti;
    }

    protected void saveSuccessPromotionTransaction(String comment, long giftTranId, long totalAmount, String number, String partnerName, String partnerId, String billId, String desc, String html, JsonObject joExtra)
    {
        final TranObj mainObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        mainObj.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        mainObj.comment = comment;
        mainObj.tranId = giftTranId;
        mainObj.clientTime = currentTime;
        mainObj.ackTime = currentTime;
        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = totalAmount;
        mainObj.status = 4;
        mainObj.error = 0;
        mainObj.io = 1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.owner_number = DataUtil.strToInt(number);
        mainObj.partnerName = partnerName;
        mainObj.partnerId = partnerId;
        mainObj.billId = billId;
        mainObj.partnerRef = mainObj.comment;
        if(!Misc.isNullOrEmpty(desc)){
            mainObj.desc = desc;
        }
        JsonObject jsonHtml = new JsonObject();
        JsonArray jsonArrayShare = new JsonArray();
        jsonHtml.putString(StringConstUtil.HTML, html);
        jsonArrayShare.addObject(jsonHtml);
        jsonArrayShare.addObject(joExtra);
        mainObj.share = jsonArrayShare;
        transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                }
            }
        });
    }
    protected void giveVoucher(final long totalGiftAmount
            , final int giftTime
            , final String agent
            , final String phoneNumber
            , final String program
            , final List<String> listGiftName
            , final Common.BuildLog log
            , final String className
            , final boolean activeGift
            , final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        final JsonObject joReply = new JsonObject();
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", program));

        keyValues.add(new Misc.KeyValue("group", program));

        addLog("info ", "TOTAL GIFT " + listGiftName.size(), className, log);
        addLog("info ", "TOTAL VALUE " + totalGiftAmount, className, log);

        int timeForGift = giftTime;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , phoneNumber
                , totalGiftAmount
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {

                final int error = jsonObject.getInteger("error", -1);
                final long promotedTranId = jsonObject.getLong("tranId", -1);
                addLog("info ", error + "", className, log);
                addLog("info ", SoapError.getDesc(error) + "", className, log);

                joReply.putNumber("error", error);

                //tra thuong trong core thanh cong
                if (error == 0) {
                    //tao gift tren backend
                    final GiftType giftType = new GiftType();
                    final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                    final Misc.KeyValue kv = new Misc.KeyValue();

                    final long modifyDate = System.currentTimeMillis();
                    final String note = program;

                    keyValues.clear();

                    kv.Key = "group";
                    kv.Value = program;
                    keyValues.add(kv);
                    final AtomicInteger atomicInteger = new AtomicInteger(listGiftName.size());
                    addLog("info", "so luong voucher " + atomicInteger, className, log);

                    vertx.setPeriodic(250L, new Handler<Long>() {
                        @Override
                        public void handle(Long aPeriodicLong) {
                            if (atomicInteger.decrementAndGet() < 0) {
                                addLog("info", "out of range " + atomicInteger + " for number " + phoneNumber, className, log);
                                //callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                                vertx.cancelTimer(aPeriodicLong);
                            } else {
                                final int itemPosition = atomicInteger.intValue();
                                addLog("info", "itemPosition " + itemPosition, className, log);
                                giftType.setModelId(listGiftName.get(itemPosition).trim());
                                giftManager.createLocalGiftForBillPayPromoWithDetailGift(phoneNumber
                                        , totalGiftAmount
                                        , giftType
                                        , promotedTranId
                                        , agent
                                        , modifyDate
                                        , endGiftTime
                                        , keyValues, note, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject jsonObject) {
                                        int err = jsonObject.getInteger("error", -1);
                                        final long tranId = jsonObject.getInteger("tranId", -1);
                                        final Gift gift = new Gift(jsonObject.getObject("gift"));
                                        final String giftId = gift.getModelId().trim();
                                        addLog("info", "err " + err, className, log);
                                        addLog("info", "tra thuong chuong trinh", className, log);

                                        //------------tat ca thanh cong roi
                                        if (err == 0) {
                                            addLog("info", "Tao qua local thanh cong", className, log);
                                            if (activeGift) {
                                                giftManager.useGift(phoneNumber, giftId, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject jsonObject) {
                                                        if (itemPosition == 0) {
                                                            joReply.putNumber(StringConstUtil.PromotionField.ERROR, 0);
                                                            joReply.putNumber(StringConstUtil.PromotionField.GIFT_TID, promotedTranId);
                                                            joReply.putString(StringConstUtil.PromotionField.GIFT_ID, giftId);
                                                            joReply.putObject(StringConstUtil.PromotionField.GIFT, gift.toJsonObject());
                                                            joReply.putNumber(StringConstUtil.PromotionField.GIFT_POSITION, itemPosition);
                                                            callback.handle(joReply);
                                                            return;
                                                        }
                                                    }
                                                });
                                            } else {
                                                if (itemPosition == 0) {
                                                    joReply.putNumber(StringConstUtil.PromotionField.ERROR, 0);
                                                    joReply.putNumber(StringConstUtil.PromotionField.GIFT_TID, promotedTranId);
                                                    joReply.putString(StringConstUtil.PromotionField.GIFT_ID, giftId);
                                                    joReply.putObject(StringConstUtil.PromotionField.GIFT, gift.toJsonObject());
                                                    joReply.putNumber(StringConstUtil.PromotionField.GIFT_POSITION, itemPosition);
                                                    callback.handle(joReply);
                                                    return;
                                                }
                                            }
                                        } else {
                                            addLog("info", "Tao qua local that bai", className, log);
                                            joReply.putNumber(StringConstUtil.PromotionField.ERROR, 5007);
                                            joReply.putNumber(StringConstUtil.PromotionField.GIFT_TID, promotedTranId);
                                            joReply.putString(StringConstUtil.PromotionField.DESCRIPTION, PromoContentNotification.PromotionErrorMap.get(5007));
                                            joReply.putNumber(StringConstUtil.PromotionField.GIFT_POSITION, itemPosition);
                                            callback.handle(joReply);
                                            return;
                                        }
                                    }
                                });
                            }
                        }
                    });
                    return;
                } else {
                    //tra thuong trong core khong thanh cong
                    addLog("info", "Core loi", className, log);
                    addLog("info", SoapError.getDesc(error), className, log);
                    callback.handle(new JsonObject().putNumber(StringConstUtil.PromotionField.ERROR, error).putString(StringConstUtil.PromotionField.DESCRIPTION, "Core tra loi khi tra the qua tang " + error)
                            .putNumber(StringConstUtil.PromotionField.GIFT_TID, promotedTranId));
                    return;
                }
            }
        });
    }

    protected void activeGift(String phoneNumber, String giftId, final Handler<JsonObject> callBack)
    {
        giftManager.useGift(phoneNumber, giftId, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                callBack.handle(new JsonObject().putObject(StringConstUtil.PromotionField.DATA, jsonObject));
            }
        });
    }

    protected void lockGift(String phoneNumber, String giftId, final Handler<Boolean> callBack)
    {
        giftDb.lock(phoneNumber, giftId, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callBack.handle(result);
            }
        });
    }

    protected void unLockGift(String phoneNumber, String giftId, final Handler<Boolean> callBack)
    {
        giftDb.unlock(phoneNumber, giftId, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                callBack.handle(result);
            }
        });
    }

    private void giveMoney(final String program, final String agent, final long totalMoney, final String phoneNumber, final Common.BuildLog log,final String className, final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        addLog("info", "method giveMoney", className, log);
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = program;
        Misc.adjustment(vertx, agent, phoneNumber, totalMoney, Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        addLog("info", "core tra ket qua", className, log);
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            addLog("info", "core tra ket qua", className, log);
                            addLog("info", "core tra ket qua", className, log);
                            addLog("info", "status " + soapObjReply.status, className, log);
                            addLog("info", "bonus money tranId " + soapObjReply.tranId, className, log);
                            addLog("info", "core tra loi " + soapObjReply.error, className, log);
                            jsonReply.putNumber(StringConstUtil.PromotionField.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.PromotionField.DESCRIPTION,  "Khong thu duoc tien khach hang, core tra loi " + soapObjReply.error);
                            jsonReply.putNumber(StringConstUtil.PromotionField.GIFT_TID, soapObjReply.tranId);
                        }
                        else {
                            jsonReply.putNumber(StringConstUtil.PromotionField.ERROR, 0);
                            jsonReply.putNumber(StringConstUtil.PromotionField.GIFT_TID, soapObjReply.tranId);
                        }
                        callback.handle(jsonReply);
                    }
                });
    }

    protected void getPromotion(final String program, final Handler<JsonObject> callback) {
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        //Moi lan chay la kiem tra thoi gian promo.
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonObject joReply = new JsonObject();
                JsonArray array = json.getArray("array", null);
                String agent = "";
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(program)) {
                            agent = promoObj.ADJUST_ACCOUNT;
                            break;
                        }
                    }
                    //Kiem tra xem con thoi gian khuyen mai ko
                    if ("".equalsIgnoreCase(agent)) {
                        joReply.putString(StringConstUtil.DESCRIPTION, "Chua cau hinh chuong trinh de quet tu dong " + " " + StringConstUtil.GRAB_PROMO.NAME);
                        joReply.putNumber(StringConstUtil.ERROR, -1);
                        callback.handle(joReply);
                    } else {
                        //Tim thong tin diem giao dich
                        joReply.putObject(StringConstUtil.PROMOTION, promoObj.toJsonObject());
                        joReply.putNumber(StringConstUtil.ERROR, 0);
                        callback.handle(joReply);
                    }
                } else {
                    logger.info("Khong load duoc thong tin du lieu khuyen mai");
                    joReply.putString(StringConstUtil.DESCRIPTION, "Khong co thong tin");
                    joReply.putNumber(StringConstUtil.ERROR, -2);
                    callback.handle(joReply);
                }
            }
        });
    }
}
