package com.mservice.momo.vertx.processor.promotion;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.binhtanpromotion.*;
import com.mservice.momo.data.ironmanpromote.IronManBonusTrackingTableDb;
import com.mservice.momo.data.ironmanpromote.IronManNewRegisterTrackingDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.promotion.PhoneCheckDb;
import com.mservice.momo.data.tracking.BanknetTransDb;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.binhtanpromotion.BinhTanPromotionObj;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by khoanguyen on 03/05/2016.
 */
public class AcquireBinhTanUserProcess extends PromotionProcess{


    AcquireBinhTanUserPromotionDb acquireBinhTanUserPromotionDb;
    AcquireBinhTanGroup2PromotionDb acquireBinhTanGroup2PromotionDb;
    AcquireBinhTanGroup3PromotionDb acquireBinhTanGroup3PromotionDb;
    DeviceDataUserDb deviceDataUserDb;
    JsonObject androidVersionObject;
    BanknetTransDb banknetTransDb;
    PhoneCheckDb phoneCheckDb;
    BinhTanDgdTransTrackingDb binhTanDgdTransTrackingDb;
    boolean isAndroidServer;
    private IronManBonusTrackingTableDb ironManBonusTrackingTableDb;
    public AcquireBinhTanUserProcess(Vertx vertx, Logger logger, JsonObject glbCfg) {
        super(vertx, logger, glbCfg);
        acquireBinhTanUserPromotionDb = new AcquireBinhTanUserPromotionDb(vertx, logger);
        acquireBinhTanGroup2PromotionDb = new AcquireBinhTanGroup2PromotionDb(vertx, logger);
        acquireBinhTanGroup3PromotionDb = new AcquireBinhTanGroup3PromotionDb(vertx, logger);
        deviceDataUserDb = new DeviceDataUserDb(vertx, logger);
        androidVersionObject = glbCfg.getObject("mobiapp_version").getObject("android");
        banknetTransDb = new BanknetTransDb(vertx, logger);
        phoneCheckDb = new PhoneCheckDb(vertx, logger);
        binhTanDgdTransTrackingDb = new BinhTanDgdTransTrackingDb(vertx, logger);
        ironManBonusTrackingTableDb = new IronManBonusTrackingTableDb(vertx, logger);
        isAndroidServer = glbCfg.getBoolean("isAndroidServer", false);
    }


    public void executeRegisterUser(final String phoneNumber, final Common.BuildLog log, final SockData sockData,final JsonObject joExtra)
    {
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                long promo_start_date = 0;
                long promo_end_date = 0;
                boolean enableRegister = false;
                boolean enableIOSRegister = false;
                boolean enableANDROIDRegister = false;
                long currentTime = System.currentTimeMillis();
                String agent = "";
                long total_amount = 0;
                long perTranAmount = 0;
                final String extraKey = joExtra.getString(StringConstUtil.BinhTanPromotion.EXTRA_KEY, "");
                String os = extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL).length > 1 ? StringConstUtil.ANDROID_OS : StringConstUtil.IOS_OS ;
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    JsonObject jsonTime = new JsonObject();
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.BinhTanPromotion.PROGRAM)) {
                            promo_start_date = promoObj.DATE_FROM;
                            promo_end_date = promoObj.DATE_TO;
                            agent = promoObj.ADJUST_ACCOUNT;
                            total_amount = promoObj.TRAN_MIN_VALUE;
                            perTranAmount = promoObj.PER_TRAN_VALUE;
                            enableRegister = promoObj.STATUS;
                            enableIOSRegister = promoObj.STATUS_IOS;
                            enableANDROIDRegister = promoObj.STATUS_ANDROID;
                            break;
                        }
                    }
                    final PromotionDb.Obj finalPromoObj = promo_start_date > 0 ? promoObj : null;
                    //Check lan nua do dai chuoi ki tu
                    if ("".equalsIgnoreCase(agent) || finalPromoObj == null) {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                        JsonObject jsonReply = new JsonObject();
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                        log.writeLog();
                        return;
                    } else if(currentTime < promo_start_date || currentTime > promo_end_date) {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tham gia ngoai thoi gian khuyen mai nen khong ghi nhan");
                        JsonObject jsonReply = new JsonObject();
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Tham gia ngoai thoi gian khuyen mai nen khong ghi nhan");
                        log.writeLog();
                        return;
                    } else if(!enableRegister) {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Chuong trinh dang tam thoi bi off");
                        JsonObject jsonReply = new JsonObject();
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Chuong trinh dang tam thoi bi off nen khong ghi nhan");
                        log.writeLog();
                        return;
                    } else if((!enableIOSRegister && os.equalsIgnoreCase(StringConstUtil.IOS_OS)) || (!enableANDROIDRegister && os.equalsIgnoreCase(StringConstUtil.ANDROID_OS))) {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Chuong trinh dang tam thoi bi off cho rieng OS: " + sockData.os);
                        JsonObject jsonReply = new JsonObject();
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Chuong trinh dang tam thoi bi off nen khong ghi nhan register bang OS: " + sockData.os);
                        log.writeLog();
                        return;
                    }
                    //Luu thong tin khach hang
                    log.add("desc ", "Luu thong tin khach hang.");
                    log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM, "executeRegisterUser");
                    if("".equalsIgnoreCase(extraKey.trim().toString()))
                    {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "App moi khong gui thong tin du lieu ca nhan app.");
                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "App khong gui thong tin du lieu ca nhan app. " + phoneNumber + " OS: " + sockData.os + "info app: " + joExtra.toString());
                        log.writeLog();
                        return;
                    }
                    //notify tu dong thong bao user di den Binh Tan de nhan khuyen mai
                    /*************************************************************************/
                    acquireBinhTanUserPromotionDb.findOne(phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                        @Override
                        public void handle(AcquireBinhTanUserPromotionDb.Obj obj) {
                            if(obj == null || obj.time_group_3 == 0 || (!obj.isTopup && obj.echo < 3)) {
                                Misc.sendRedirectNoti(vertx, createJsonNotification(phoneNumber, PromoContentNotification.NOTI_BINH_TAN_REGISTER, PromoContentNotification.NOTI_BINH_TAN_PROMO_TITLE, "https://momo.vn/nap10tang50/"));
                                acquireBinhTanUserPromotionDb.findAndIncEchoUser(phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                                    @Override
                                    public void handle(AcquireBinhTanUserPromotionDb.Obj event) {
                                        logger.info("update so lan send noti");
                                    }
                                });
                            }
                        }
                    });
                    /*************************************************************************/
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Kiem tra extra key");
                    log.add("extra key " + StringConstUtil.BinhTanPromotion.PROGRAM, extraKey);
                    getExtraKeyFromApp(phoneNumber, extraKey, log, sockData, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joResponse) {
                            int err = joResponse.getInteger(StringConstUtil.ERROR, 1000);
                            String desc = joResponse.getString(StringConstUtil.DESCRIPTION, "ERROR");
                            if(err == 0)
                            {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, desc);
                                log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM, "recordBinhTanPromotionUser");
                                JsonArray jarrOr = new JsonArray();
                                JsonObject joImei = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.IMEI, sockData.imei);
                                JsonObject joPhoneNumber = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);
                                jarrOr.add(joImei);
                                jarrOr.add(joPhoneNumber);
                                JsonObject joFilter = new JsonObject();
                                joFilter.putArray(MongoKeyWords.OR, jarrOr);
                                acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
                                    @Override
                                    public void handle(ArrayList<AcquireBinhTanUserPromotionDb.Obj> listUserBinhTans) {
                                        if(listUserBinhTans.size() > 0)
                                        {
                                            boolean dupImei = false;
                                            for(AcquireBinhTanUserPromotionDb.Obj userObj: listUserBinhTans)
                                            {
                                                if(userObj.imei.equalsIgnoreCase(sockData.imei))
                                                {
                                                    dupImei = true;
                                                    break;
                                                }
                                            }
                                            if(dupImei)
                                            {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Trung IMEI roi nha bo !!!!!");
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Trung IMEI roi nha bo !!!!! " + phoneNumber);
                                            }
                                            else {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Trung SDT roi nha bo !!!!!");
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Trung SDT roi nha bo !!!!! " + phoneNumber);
                                            }
                                            log.writeLog();
                                            return;
                                        }
//                                        recordBinhTanPromotionUser(phoneNumber, extraKey, sockData);
                                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "checkBonusRandom !!!!!");
                                        checkBonusRandom(log, extraKey, sockData, phoneNumber);
                                        log.writeLog();
                                        return;
                                    }
                                });
                                return;
                            }
                            else {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, desc);
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, desc + " " + phoneNumber);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }
            }
        });
    }

    public void recordBinhTanPromotionUser(int programNumber, String phoneNumber, String extraKey, SockData sockData, boolean hasBonus, final Handler<JsonObject> callback)
    {
        JsonObject joExtra = new JsonObject();
        joExtra.putString(StringConstUtil.BinhTanPromotion.EXTRA_KEY, extraKey);
        joExtra.putString(StringConstUtil.DEVICE_IMEI, sockData.imei);
        joExtra.putBoolean(colName.AcquireBinhTanUserPromotionCol.HAS_BONUS, hasBonus);
        joExtra.putNumber(colName.AcquireBinhTanUserPromotionCol.PROGRAM_NUMBER, programNumber);
        BinhTanPromotionObj.requestAcquireBinhTanUserPromo(vertx, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, StringConstUtil.BinhTanPromotion.PROGRAM, joExtra, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                callback.handle(event);
            }
        });
    }

    public void recordBinhTanPromotionUser(String phoneNumber, String extraKey, SockData sockData)
    {
        JsonObject joExtra = new JsonObject();
        joExtra.putString(StringConstUtil.BinhTanPromotion.EXTRA_KEY, extraKey);
        joExtra.putString(StringConstUtil.DEVICE_IMEI, sockData.imei);
        BinhTanPromotionObj.requestAcquireBinhTanUserPromo(vertx, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, StringConstUtil.BinhTanPromotion.PROGRAM, joExtra, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {

            }
        });
    }

    public void checkBonusRandom(final Common.BuildLog log, final String extraKey, final SockData sockData, final String phoneNumber)
    {
        JsonObject joSearch = new JsonObject();
        joSearch.putString(colName.IronManBonusTrackingTable.PROGRAM, StringConstUtil.BinhTanPromotion.PROGRAM);
        ironManBonusTrackingTableDb.searchWithFilter(joSearch, new Handler<ArrayList<IronManBonusTrackingTableDb.Obj>>() {
            @Override
            public void handle(ArrayList<IronManBonusTrackingTableDb.Obj> objs) {
//                log.add("size iron BonusTrackingTable", objs.size());
                IronManBonusTrackingTableDb.Obj bonusObj = null;
                boolean hasBonus = false;
//                log.add("size iron BonusTrackingTable", objs);
                IronManBonusTrackingTableDb.Obj bonusTrackingObjLast = null;
                for (IronManBonusTrackingTableDb.Obj bonusTrackingObj : objs) {
                    if (System.currentTimeMillis() >= bonusTrackingObj.start_time && System.currentTimeMillis() <= bonusTrackingObj.end_time) {
                        log.add("bonusTrackingObj", bonusTrackingObj.toJson());
                        log.add("size iron", "nam trong chuong trinh binhtan");
                        log.add("program_number", bonusTrackingObj.program_number);
                        hasBonus = randomCheckIronManBonus(bonusTrackingObj, log);
                        bonusObj = bonusTrackingObj;
                        break;
                    } // END IF
                    else if(System.currentTimeMillis() < bonusTrackingObj.start_time) {
                        log.add("size iron", "khong co phien giao dich, lay phien gan nhat");
                        log.add("program_number", bonusTrackingObj.program_number);
                        hasBonus = false;
                        bonusObj = bonusTrackingObjLast == null ? bonusTrackingObj : bonusTrackingObjLast;
                        break;
                    }
                    bonusTrackingObjLast = bonusTrackingObj;
                } // END FOR
                if(bonusObj == null && bonusTrackingObjLast != null)
                {
                    bonusObj = bonusTrackingObjLast;
                }
                if (bonusObj != null && bonusObj.program_number != 0) {
                    log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM, " co phien tra thuong " + bonusObj.toJson());
                    log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM, " ket qa tra thuong " + hasBonus);
                    updateIronNewRegisterTrackingTable(log, hasBonus, bonusObj, sockData, phoneNumber, extraKey);
                }
                else {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "khong co phien tra thuong " + phoneNumber);
                    log.writeLog();
                }
            }
        });
    }


    //BEGIN 0000000052 IRON MAN
    private void updateIronNewRegisterTrackingTable(final Common.BuildLog log, boolean hasBonus, final IronManBonusTrackingTableDb.Obj ironManBonusTracking, SockData sockData, String phoneNumber, String extraKey)
    {
        log.add("func", "updateIronNewRegisterTrackingTable");
        log.add("func", ironManBonusTracking.program_number);
        final int program_number = ironManBonusTracking.program_number;
        log.add("program_number ", program_number);
        log.add("program ", ironManBonusTracking.program);
        recordBinhTanPromotionUser(program_number, phoneNumber, extraKey, sockData, hasBonus, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                saveInfoIronManDb(program_number, ironManBonusTracking);
                log.writeLog();
            }
        });
    }

    private void saveInfoIronManDb(final int program_number, final IronManBonusTrackingTableDb.Obj ironManBonusTracking)
    {
        acquireBinhTanUserPromotionDb.countUserStatus(ironManBonusTracking, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                if (jsonArray != null && jsonArray.size() > 0) {
                    Boolean isBonus = false;
                    int count = 0;
                    int receivedGift = 0;
                    int notReceivedGift = 0;
                    for (Object o : jsonArray) {
                        isBonus = ((JsonObject) o).getBoolean("_id");
                        boolean isBonusTmp = isBonus == null ? jsonArray.toString().contains("true") ? false : true : isBonus.booleanValue();
                        count = ((JsonObject) o).getInteger("count", 0);
                        if (isBonusTmp) {
                            receivedGift = count;
                        }
                        else{
                            notReceivedGift = count;
                        }
                    }
                    JsonObject jsonBonusTracking = ironManBonusTracking.toJson();
                    jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER);
                    jsonBonusTracking.removeField(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN);

                    jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_NEW_COMER, receivedGift + notReceivedGift);
                    jsonBonusTracking.putNumber(colName.IronManBonusTrackingTable.NUMBER_OF_BONUS_MAN, receivedGift);
                    ironManBonusTrackingTableDb.updatePartial(program_number, jsonBonusTracking, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    });
                }
            }
        });
    }

    private IronManNewRegisterTrackingDb.Obj createIronManObject(MomoMessage msg, boolean hasBonus, int program_number)
    {
        IronManNewRegisterTrackingDb.Obj obj = new IronManNewRegisterTrackingDb.Obj();
        obj.program_number = program_number;
        obj.phoneNumber = "0" + msg.cmdPhone;
        obj.timeRegistry = System.currentTimeMillis();
        obj.hasBonus = hasBonus;
        return obj;
    }

    private boolean randomCheckIronManBonus(IronManBonusTrackingTableDb.Obj trackingObj, Common.BuildLog log) {
        boolean hasBonus = false;

        //Khong can map voi ratio cua minh ma tinh tong cong cua phien
        if(trackingObj.not_ratio_flag && trackingObj.number_of_bonus_man < trackingObj.number_of_bonus_gave_man)
        {
            hasBonus = true;
        }
        //Map voi ti le ben he thong
        else
        {
            int randomValue = UUID.randomUUID().hashCode() % 2; //0: hen, 1:xui
            double currentRatio = Double.parseDouble("1");
            if(trackingObj.number_of_new_comer != 0)
            {
                currentRatio = Double.parseDouble(trackingObj.number_of_bonus_man  + "")/ Double.parseDouble(trackingObj.number_of_new_comer  + "");
            }
            double minRatio = Double.parseDouble(trackingObj.min_ratio + "")/100;
            double maxRatio = Double.parseDouble("" + trackingObj.max_ratio)/100;
            double ratio = Double.parseDouble("" + trackingObj.numerator)/ Double.parseDouble("" + trackingObj.denominator);
            double funnyRatio_1 = (maxRatio - ratio)/2;
            double funnyRatio_2 = (ratio - minRatio)/2;
            log.setPhoneNumber("randomCheckIronManBonus");
            log.add("currentRatio", currentRatio);
            log.add("minRatio", minRatio);
            log.add("maxRatio", maxRatio);
            log.add("ratio", ratio);
            log.add("funnyRatio_1", funnyRatio_1);
            log.add("funnyRatio_2", funnyRatio_2);
            //Kiem tra xem co rap vao ratio khong
            if (randomValue == 0) {
                //Thang nay hen, random duoc tra thuong.
                hasBonus = true;
                //Kiem tra xem no co vuot qua muc cho phep hay khong, neu co thi .... xin loi em no thoi
            } else {
                // So em nay xui, he thong khong cho tra thuong.
                hasBonus = false;
                //Kiem tra ti le hien tai
            }

            if(currentRatio > maxRatio)
            {
                hasBonus = false;
            }
            else if (currentRatio < minRatio)
            {
                hasBonus = true;
            }
            else if(currentRatio > ratio + funnyRatio_1)
            {
                hasBonus = false;
            }
            else if(currentRatio < ratio - funnyRatio_2)
            {
                hasBonus = true;
            }

        }
        log.add("bonus", hasBonus);
        return hasBonus;
    }
    /**
     * This method used to check device + imei of phoneNumber
     * @param phoneNumber
     * @param extraKey
     * @param log
     * @param data
     * @param callback
     */
    public void getExtraKeyFromApp(final String phoneNumber, final String extraKey, final Common.BuildLog log,final SockData data, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();
        log.add("extra key " + StringConstUtil.BinhTanPromotion.PROGRAM, extraKey);

        String extraKeyAndroid = "";
        if(StringConstUtil.ANDROID_OS.equalsIgnoreCase(data.os))
        {
            final String[] extraKeyArr = extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL);
            log.add("extra keyArr length " + StringConstUtil.BinhTanPromotion.PROGRAM, extraKeyArr.length);
            extraKeyAndroid = extraKeyArr.length > 1 ? extraKeyArr[0] : extraKey;
            log.add("extraKeyAndroid " + StringConstUtil.BinhTanPromotion.PROGRAM, extraKeyAndroid);

            //KIEM TRA ANDROID moi
            String extraKeyTemp = extraKeyArr.length > 1 ? extraKeyArr[1] : "";
//            if(!"".equalsIgnoreCase(extraKeyTemp))
//            {
//                int count = 0;
//                boolean isKilled = false;
//                String []extraKeyTempArr = extraKeyTemp.split(MomoMessage.BELL);
//                for(int i = 0; i < extraKeyTempArr.length; i++)
//                {
//
//                    if("0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))
//                    {
//                        isKilled = true;
//                        break;
//                    }
//                    if(!"XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) && !"-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) && !"".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())
//                            && !"0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))
//                    {
//                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thiết bị này có sim");
//                        count = count + 1;
//                    }
////                    if("XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) || "-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()))
////                    {
////                        joReply.putNumber(StringConstUtil.ERROR, 1000);
////                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
////                        log.add("error " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
////                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn " + " " + phoneNumber);
////                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, joDesc.toString());
////                        callback.handle(joReply);
////                        return;
////                    }
//                }
//                if(isKilled)
//                {
//                    joReply.putNumber(StringConstUtil.ERROR, 1000);
//                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này dùng máy ảo nên không được tham gia chương trình");
//                    log.add("error " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thiết bị này dùng máy ảo nên không được tham gia chương trình");
//                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này dùng máy ảo nên không được tham gia chương trình " + " " + phoneNumber);
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, joDesc.toString());
//                    callback.handle(joReply);
//                    return;
//                }
//                else if(count < 2)
//                {
//                    joReply.putNumber(StringConstUtil.ERROR, 1000);
//                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
//                    log.add("error " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
//                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn " + " " + phoneNumber);
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, joDesc.toString());
//                    callback.handle(joReply);
//                    return;
//                }
//            }
        }

        String extraKeyFinal = "".equalsIgnoreCase(extraKeyAndroid) ? extraKey : extraKeyAndroid;
        final String[] address_tmp = extraKeyFinal.split(MomoMessage.BELL);
        log.add("size address tmp " + StringConstUtil.BinhTanPromotion.PROGRAM, address_tmp.length);

        if (!data.os.equalsIgnoreCase("ios") && address_tmp.length == 0) {
           joReply.putNumber(StringConstUtil.ERROR, 1000);
           joReply.putString(StringConstUtil.DESCRIPTION, "Thieu du lieu extra key");
           Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Thieu du lieu extra key" + " " + phoneNumber);
           callback.handle(joReply);
        } else {
            final AtomicInteger integer = new AtomicInteger(address_tmp.length > 3? 3 : address_tmp.length);
            final AtomicInteger empty = new AtomicInteger(0);
            if (!"".equalsIgnoreCase(data.os) && !data.os.equalsIgnoreCase("ios")) {
                log.add("os " + StringConstUtil.BinhTanPromotion.PROGRAM, "android");
                takeNoteDeviceInfo(phoneNumber, log, callback, joReply, address_tmp, integer, empty, extraKeyFinal);
            }
//            else if(data.os.equalsIgnoreCase("ios") && data.appCode >= 1923)
//            {
//                //Thuc hien luu tru moi
//                log.add("os " + StringConstUtil.BinhTanPromotion.PROGRAM, "ios");
//                takeNoteDeviceInfo(phoneNumber, log, callback, joReply, address_tmp, integer, empty);
//            }
            else if (data.os.equalsIgnoreCase("ios")) {
                final DeviceDataUserDb.Obj deviceObj = new DeviceDataUserDb.Obj();
                deviceObj.phoneNumber = phoneNumber;
                deviceObj.id = data.imei;
                String[]iosKeys = extraKey.split(MomoMessage.BELL);
//                for(String iosData : iosKeys)
//                {
//                    if(iosData == null || iosData.contains("null"))
//                    {
//                        log.add("desc", "Thiet bi IOS khong gan sim " + extraKey + " phone " + phoneNumber);
//                        log.writeLog();
//                        joReply.putNumber(StringConstUtil.ERROR, 10000);
//                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiet bi IOS khong gan sim " + extraKey + " phone " + phoneNumber);
//                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, data.imei).putString(StringConstUtil.DESCRIPTION, "Thiet bi IOS khong gan sim " + extraKey + " phone " + phoneNumber);
//                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 10000, joDesc.toString());
//                        callback.handle(joReply);
//                        return;
//                    }
//                }
                deviceDataUserDb.insert(deviceObj, StringConstUtil.BinhTanPromotion.PROGRAM, new Handler<Integer>() {
                    @Override
                    public void handle(Integer result) {
                        if (result == 0) {
                            joReply.putNumber(StringConstUtil.ERROR, 0);
                            joReply.putString(StringConstUtil.DESCRIPTION, "GOOD");
                        } else {
                            joReply.putNumber(StringConstUtil.ERROR, 1000);
                            joReply.putString(StringConstUtil.DESCRIPTION, "Du lieu thiet bi da ton tai, khong cho so nay tham gia khuyen mai binh tan nua");
                            log.add("error " + StringConstUtil.BinhTanPromotion.PROGRAM, "Loi insert ios data user");
                            JsonObject joFilter = new JsonObject().putString(colName.DeviceDataUser.ID, deviceObj.id);
                            deviceDataUserDb.searchWithFilter(StringConstUtil.BinhTanPromotion.PROGRAM, joFilter, new Handler<ArrayList<DeviceDataUserDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<DeviceDataUserDb.Obj> listDevices) {
                                    String phoneDup = "";
                                    if(listDevices.size() > 0)
                                    {
                                        phoneDup = listDevices.get(0).phoneNumber;
                                    }
                                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, data.imei).putString(StringConstUtil.DESCRIPTION, "Thiết bị IOS của số điện thoại " + phoneNumber + " đã được trả thưởng cho số điện thoại " + phoneDup);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, joDesc.toString());
                                }
                            });
                        }
                        callback.handle(joReply);
                    }
                });
                return;
            } else {
//                processIronMan(buf, log, msg, sock);
                  log.add("desc" + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong ton tai thiet bi nay");
                  joReply.putNumber(StringConstUtil.ERROR, 1000);
                  joReply.putString(StringConstUtil.DESCRIPTION, "Khong ton tai thiet bi, khong cho tham gia chuong trinh");
                  Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong ton tai thiet bi, khong cho tham gia chuong trinh" + " " + phoneNumber);
                  callback.handle(joReply);
            }
        }

    }

    private void takeNoteDeviceInfo(final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback, final JsonObject joReply, final String[] address_tmp, final AtomicInteger integer, final AtomicInteger empty, final String extraKey) {
        vertx.setPeriodic(200L, new Handler<Long>() {
            @Override
            public void handle(final Long event) {
                int position = integer.decrementAndGet();
                if (position < 0) {
                    log.add("position " + StringConstUtil.BinhTanPromotion.PROGRAM, position);
                    vertx.cancelTimer(event);
                    if (empty.intValue() > 0) {
                        log.add("position " + StringConstUtil.BinhTanPromotion.PROGRAM, "empty.intValue() != address_tmp.length");
                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin gmail || imei || mac ghi nhan. + INFO: " + extraKey);
                    } else {
                        log.add("position " + StringConstUtil.BinhTanPromotion.PROGRAM, "data is enough => GOOD");
                        joReply.putNumber(StringConstUtil.ERROR, 0);
                        joReply.putString(StringConstUtil.DESCRIPTION, "GOOD");
                    }
                    callback.handle(joReply);
                    return;
                }
                else {
                    if (address_tmp[position].equalsIgnoreCase("")) {
                        log.add("item " + StringConstUtil.BinhTanPromotion.PROGRAM, address_tmp[position]);
                        empty.incrementAndGet();
                    }
                    /*else if(address_tmp[position].equalsIgnoreCase("XXX")) {
                        vertx.cancelTimer(event);
                        log.add("error " + StringConstUtil.BinhTanPromotion.PROGRAM, "Loi insert android data user bi xxx");
                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này không truyền đủ thông tin, sẽ kiểm tra và trả bù nếu hợp lệ. Xin cám ơn " +  phoneNumber);
                        callback.handle(joReply);
                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này không truyền đủ thông tin, sẽ kiểm tra và trả bù nếu hợp lệ. Xin cám ơn " +  phoneNumber);
                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, joDesc.toString());
                        return;
                    }*/
                    else if(address_tmp[position].trim().equalsIgnoreCase("02:00:00:00:00:00"))
                    {
                        log.add("item " + phoneNumber + " " + StringConstUtil.BinhTanPromotion.PROGRAM, address_tmp[position]);
                        log.add("item " + phoneNumber + " " + StringConstUtil.BinhTanPromotion.PROGRAM, "ANDROID 6 nen bo qua MAC ADDRESS");
                    }
                    else if(address_tmp[position].trim().contains("@"))
                    {
                        log.add("item " + phoneNumber + " " + StringConstUtil.BinhTanPromotion.PROGRAM, address_tmp[position]);
                        log.add("item " + phoneNumber + " " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong luu thong tin gmail");
                    }
                    else {
                        log.add("item " + StringConstUtil.BinhTanPromotion.PROGRAM, address_tmp[position]);
                        final DeviceDataUserDb.Obj deviceDataUserObj = new DeviceDataUserDb.Obj();
                        deviceDataUserObj.id = address_tmp[position].toString().trim();
                        deviceDataUserObj.phoneNumber = phoneNumber;
                        if(!deviceDataUserObj.id.equalsIgnoreCase("XXX")) {
                            deviceDataUserDb.insert(deviceDataUserObj, StringConstUtil.BinhTanPromotion.PROGRAM, new Handler<Integer>() {
                                @Override
                                public void handle(Integer resultInsert) {
                                    if (resultInsert != 0) {
                                        vertx.cancelTimer(event);
                                        log.add("error " + StringConstUtil.BinhTanPromotion.PROGRAM, "Loi insert android data user");
                                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "Du lieu da ton tai, khong cho so nay tham gia chuong trinh");
                                        callback.handle(joReply);
                                        JsonObject joFilter = new JsonObject().putString(colName.DeviceDataUser.ID, deviceDataUserObj.id);
                                        deviceDataUserDb.searchWithFilter(StringConstUtil.BinhTanPromotion.PROGRAM, joFilter, new Handler<ArrayList<DeviceDataUserDb.Obj>>() {
                                            @Override
                                            public void handle(ArrayList<DeviceDataUserDb.Obj> listDevices) {
                                                String phoneDup = "";
                                                if (listDevices.size() > 0) {
                                                    phoneDup = listDevices.get(0).phoneNumber;
                                                }
                                                JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị ANDROID của số điện thoại " + phoneNumber + " đã được trả thưởng cho số điện thoại " + phoneDup);
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, joDesc.toString());
                                            }
                                        });
                                        return;
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
        return;
    }


    //LOGIN
//    public void executeLoginUser(final MomoMessage message,final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
//    {
//        acquireBinhTanUserPromotionDb.findOne(phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
//            @Override
//            public void handle(AcquireBinhTanUserPromotionDb.Obj userObj) {
//                if(userObj == null)
//                {
//                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "User nay khong phai user moi, khong ghi nhan ton tai tham gia khuyen mai");
//                    log.writeLog();
//                    return;
//                }
//                else if(userObj.lock)
//                {
//                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "User nay da bi lock, khong duoc tham gia khuyen mai");
//                    log.writeLog();
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "User nay da bi lock, khong duoc tham gia khuyen mai" + " " + phoneNumber);
//                    return;
//                }
//                else if(userObj.end_group_2 || userObj.end_group_3 || userObj.next_time_bonus > 0 || userObj.next_time_rollback > 0)
//                {
//                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "User da duoc tra thuong, khong cho tra thuong nua");
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "User da duoc tra thuong, khong cho tra thuong nua" + " " + phoneNumber);
//                    log.writeLog();
//                    return;
//                }
//                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tra thuong user " + phoneNumber);
//                joExtra.putObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, userObj.toJson());
////                joExtra.putNumber(StringConstUtil.BinhTanPromotion.MSG_TYPE, convertTypeToString(StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.LOGIN));
//                BinhTanPromotionObj.requestAcquireBinhTanUserPromo(vertx, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, StringConstUtil.BinhTanPromotion.PROGRAM, joExtra, new Handler<JsonObject>() {
//                    @Override
//                    public void handle(JsonObject event) {
//                    }
//                });
//            }
//        });
//    }

    public void executeLoginUser(final MomoMessage message,final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        int code = androidVersionObject.getInteger("code", 76);
        if(code < 76)
        {
            code = 76;
        }
        final int androidCode = code;
        log.add("OS " + StringConstUtil.BinhTanPromotion.PROGRAM, sockData.os);
        log.add("APP CODE " + StringConstUtil.BinhTanPromotion.PROGRAM, sockData.appCode);
        log.add("APP VER " + StringConstUtil.BinhTanPromotion.PROGRAM, sockData.appVersion);

        acquireBinhTanUserPromotionDb.findAndManageUser(phoneNumber, false, true, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
            @Override
            public void handle(AcquireBinhTanUserPromotionDb.Obj userObj) {
                if (userObj == null) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "User nay khong phai user moi hoac user dang bi lock, khong cho thuc hien tra thuong");
                    log.writeLog();
                    return;
                }
                else if(sockData.os.trim().equalsIgnoreCase(StringConstUtil.ANDROID_OS) && DataUtil.strToLong(sockData.imei) == 0)
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "User nay choi simulator nen lock so khong cho tham gia");
                    JsonObject joUpdate = new JsonObject();
                    joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, true);
                    joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, true);
                    acquireBinhTanUserPromotionDb.findAndUpdateInfoUser(phoneNumber, true, false, joUpdate, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                        @Override
                        public void handle(AcquireBinhTanUserPromotionDb.Obj event) {
                            log.add("Da cap nhat lai, unlock Binh Tan cho sdt ", phoneNumber);
                            log.writeLog();
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "User nay choi simulator nen lock so khong cho tham gia " + " " + phoneNumber);
                        }
                    });
                    return;
                }
                else if("WINDOWS".equalsIgnoreCase(sockData.os.trim()))
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "App khong phai IOS va ANDROID.");
                    acquireBinhTanUserPromotionDb.findAndManageUser(phoneNumber, true, false, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                        @Override
                        public void handle(AcquireBinhTanUserPromotionDb.Obj event) {
                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "App khong phai IOS va ANDROID.");
                            log.writeLog();
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "App khong phai IOS va ANDROID. " + phoneNumber);
                        }
                    });
                    return;
                }
                else if (userObj.end_group_2 || userObj.end_group_3 || userObj.next_time_bonus > 0 || userObj.next_time_rollback > 0) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "User da duoc tra thuong, khong cho tra thuong nua");
                    acquireBinhTanUserPromotionDb.findAndManageUser(phoneNumber, true, false, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                        @Override
                        public void handle(AcquireBinhTanUserPromotionDb.Obj event) {
                            log.add("Da cap nhat lai, unlock Binh Tan cho sdt ", phoneNumber);
                            log.writeLog();
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "User da duoc tra thuong, khong cho tra thuong nua" + " " + phoneNumber);
                        }
                    });
                    return;
                }
                else if((!isStoreApp && StringConstUtil.ANDROID_OS.equalsIgnoreCase(sockData.os) && sockData.appCode < androidCode && sockData.appCode > 0)
                        ||  (!isStoreApp && StringConstUtil.IOS_OS.equalsIgnoreCase(sockData.os) && sockData.appCode < 1923 && sockData.appCode > 0))
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "App cu, vui long down app moi.");
                    acquireBinhTanUserPromotionDb.findAndManageUser(phoneNumber, true, false, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                        @Override
                        public void handle(AcquireBinhTanUserPromotionDb.Obj event) {
                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "App cu, vui long down app moi.");
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "App cu, vui long down app moi giup nhe . " + phoneNumber + " appcode hien tai " + sockData.appCode
                                    + " appver hien tai " + sockData.appVersion);
                            log.writeLog();
                            }
                    });
                    return;
                }
                else if(!userObj.hasBonus)
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tai khoan nay khong duoc tra thuong do random.");
                    acquireBinhTanUserPromotionDb.findAndManageUser(phoneNumber, true, false, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                        @Override
                        public void handle(AcquireBinhTanUserPromotionDb.Obj event) {
                            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tai khoan nay khong duoc tra thuong do random.");
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Tai khoan nay khong duoc tra thuong do random . " + phoneNumber);
                            log.writeLog();
                        }
                    });
                    return;
                }
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tra thuong user " + phoneNumber);
                joExtra.putObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, userObj.toJson());
//                joExtra.putNumber(StringConstUtil.BinhTanPromotion.MSG_TYPE, convertTypeToString(StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.LOGIN));
                BinhTanPromotionObj.requestAcquireBinhTanUserPromo(vertx, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, StringConstUtil.BinhTanPromotion.PROGRAM, joExtra, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                        log.add("response group 2 binhtan ", event);
                        acquireBinhTanUserPromotionDb.findAndManageUser(phoneNumber, true, false, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                            @Override
                            public void handle(AcquireBinhTanUserPromotionDb.Obj event) {
                                log.add("Da cap nhat lai, unlock Binh Tan cho sdt ", phoneNumber);
                                log.writeLog();
                            }
                        });

                    }
                });
            }
        });
    }

    private int convertTypeToString(StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION msgType)
    {
        int value = 0;
        switch (msgType)
        {
            case REGISTER:
                value = 1;
                break;
            case LOGIN:
                value = 2;
                break;
            case CASH_IN:
                value = 3;
                break;
            case BILL_PAY:
                value = 4;
                break;
            default:
                value = 0;
                break;
        }
        return value;
    }

    public void getInfoBankForBinhTan(final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        //Kiem tra xem da co thong tin cash
        String bankType = joExtra.getString(StringConstUtil.BANK_CODE, "");
        if("".equalsIgnoreCase(bankType))
        {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong co ton tai thong tin cashin " + phoneNumber);
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong co ton tai thong tin vi moi " + " " + phoneNumber);
            log.writeLog();
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong co ton tai thong tin cashin " + phoneNumber);
        }
        else if("bankin".equalsIgnoreCase(bankType))
        {
            log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Cashin tu bank lien ket " + phoneNumber);
//            phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
//                @Override
//                public void handle(PhonesDb.Obj phoneObj) {
//                    if(phoneObj == null || "".equalsIgnoreCase(phoneObj.bankPersonalId))
//                    {
//                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "khong co thong tin so dien thoai trong bang phone " + phoneNumber);
//                        log.writeLog();
//                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "khong co thong tin so dien thoai trong bang phone " + phoneNumber);
//                        return;
//                    }
//                    joExtra.putString(StringConstUtil.BinhTanPromotion.BANK_ID, phoneObj.bankPersonalId);
//                    recordCashInTransaction(phoneNumber, log, sockData, joExtra);
//                }
//            });
        }
        else if("banknet".equalsIgnoreCase(bankType))
        {
//            String merchantTranId = joExtra.getString(colName.BanknetTransCol.MERCHANT_TRAN_ID, "");
//            banknetTransDb.findOne(merchantTranId, new Handler<BanknetTransDb.Obj>() {
//                @Override
//                public void handle(BanknetTransDb.Obj bankNetObj) {
//                    if(bankNetObj == null || "".equalsIgnoreCase(bankNetObj.bank_acc))
//                    {
//                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "khong co thong tin so dien thoai trong bang banknet " + phoneNumber);
//                        log.writeLog();
//                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "khong co thong tin so dien thoai trong bang banknet " + phoneNumber);
//                        return;
//                    }
//                    joExtra.putString(StringConstUtil.BinhTanPromotion.BANK_ID, bankNetObj.bank_acc);
//                    recordCashInTransaction(phoneNumber, log, sockData, joExtra);
//                }
//            });
        }
        else if("m2m".equalsIgnoreCase(bankType))
        {
            joExtra.putString(StringConstUtil.BinhTanPromotion.BANK_ID, bankType);
            recordCashInTransactionByStore(phoneNumber, log, sockData, joExtra);
        }
        else {
//            joExtra.putString(StringConstUtil.BinhTanPromotion.BANK_ID, bankType);
//            recordCashInTransaction(phoneNumber, log, sockData, joExtra);
        }

    }

    public void recordCashInTransaction(final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        //Kiem tra xem da co thong tin cash
        JsonArray jArrOr = new JsonArray();

        JsonObject joFilter = new JsonObject();

        JsonObject joBankId = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.BANK_CARD_ID, joExtra.getString(StringConstUtil.BinhTanPromotion.BANK_ID, ""));
        JsonObject joPhoneNumber = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);

        jArrOr.add(joBankId);
        jArrOr.add(joPhoneNumber);

        joFilter.putArray(MongoKeyWords.OR, jArrOr);

        acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
            @Override
            public void handle(final ArrayList<AcquireBinhTanUserPromotionDb.Obj> acquireObjList) {
                if (acquireObjList.size() == 0) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong co ton tai thong tin vi moi " + phoneNumber);
                    log.writeLog();
                    return;
                } else if (acquireObjList.size() > 1) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thẻ này đã được sử dụng rồi, không trả thưởng. " + " phone 1 " + acquireObjList.get(0).phoneNumber + " bankId " + acquireObjList.get(0).bankId + " "
                            + " phone 2 " + acquireObjList.get(1).phoneNumber + " bankId " + acquireObjList.get(1).bankId);
                    log.writeLog();
                    //Chem
                    JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                    acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Thẻ này đã được sử dụng rồi, không trả thưởng và lock số này luôn. " + " phone 1 " + acquireObjList.get(0).phoneNumber + " bankId " + acquireObjList.get(0).bankId + " "
                                    + " phone 2 " + acquireObjList.get(1).phoneNumber + " bankId " + acquireObjList.get(1).bankId);
                        }
                    });
                    return;
                }
                AcquireBinhTanUserPromotionDb.Obj acquireObj = acquireObjList.get(0);
                if (acquireObj.end_group_3) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Da ket thuc chuong trinh khuyen mai " + phoneNumber);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Da ket thuc chuong trinh khuyen mai " + " " + phoneNumber);
                    log.writeLog();
                    return;
                }

                JsonObject joUpdate = new JsonObject();
                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.AMOUNT_CASHIN, joExtra.getLong(StringConstUtil.AMOUNT, 0));
                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TID_CASHIN, joExtra.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis()));
                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_CASHIN, System.currentTimeMillis());
                joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, true);
                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.GROUP, 3);
                joUpdate.putString(colName.AcquireBinhTanUserPromotionCol.BANK_CARD_ID, joExtra.getString(StringConstUtil.BinhTanPromotion.BANK_ID, ""));
                if (acquireObj.time_group_3 == 0) {
                    joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, System.currentTimeMillis());
                }
                acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {

                    }
                });
                return;
            }
        });
    }

    public void executeBillPayUser(final MomoMessage message, final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        acquireBinhTanUserPromotionDb.findOne(phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
            @Override
            public void handle(AcquireBinhTanUserPromotionDb.Obj acquireObj) {
                if(acquireObj == null)
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "khong ton tai thong tin tao vi cua user " + phoneNumber);
                    log.writeLog();
//                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Khong co ton tai thong tin vi moi " + " " + phoneNumber);
                    return;
                }
//                boolean isOk = checkPeriod(duration, acquireObj.time_cashin);
                log.add("time cashin " + StringConstUtil.BinhTanPromotion.PROGRAM, acquireObj.time_cashin + "" );
                if(acquireObj.end_group_3)
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Het han tham gia group 3 roi " + phoneNumber);
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Het han tham gia group 3 roi " + " " + phoneNumber);
                    return;
                } else if (acquireObj.isLocked)
                {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "tai khoan nay da bi lock " + phoneNumber);
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "tai khoan nay da bi lock " + " " + phoneNumber);
                    return;
                }
                joExtra.putObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, acquireObj.toJson());
                BinhTanPromotionObj.requestAcquireBinhTanUserPromo(vertx, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, StringConstUtil.BinhTanPromotion.PROGRAM, joExtra, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                    }
                });
            }
        });
    }

    public void recordCashInTransactionByStore(final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        //KIem tra xem store nay duoc quyen giao dich ko
        final String storeNumber = joExtra.getString(StringConstUtil.STORE_NUMBER, "");
        if("".equalsIgnoreCase(storeNumber))
        {
            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, "Giao dich khong qua diem giao dich");
            log.writeLog();
            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, 1000, "Giao dich khong qua diem giao dich nao => khong co so diem giao dich");
            return;
        }
        //Kiem tra xem da co thong tin cash
        JsonArray jArrOr = new JsonArray();

        JsonObject joFilter = new JsonObject();

//        JsonObject joBankId = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.BANK_CARD_ID, joExtra.getString(StringConstUtil.BinhTanPromotion.BANK_ID, ""));
        JsonObject joPhoneNumber = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);

//        jArrOr.add(joBankId);
        jArrOr.add(joPhoneNumber);

        joFilter.putArray(MongoKeyWords.OR, jArrOr);

        acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
            @Override
            public void handle(final ArrayList<AcquireBinhTanUserPromotionDb.Obj> acquireObjList) {
                if (acquireObjList.size() == 0) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Khong co ton tai thong tin vi moi " + phoneNumber);
                    //todo SendNotification to DGD.
                    JsonObject joFilter = new JsonObject().putString(colName.ErrorPromotionTrackingCol.PHONE_NUMBER, phoneNumber);
                    errorPromotionTrackingDb.searchWithFilter(StringConstUtil.BinhTanPromotion.PROGRAM, joFilter, new Handler<ArrayList<ErrorPromotionTrackingDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<ErrorPromotionTrackingDb.Obj> listErrorPromos) {
                            int notiType = 0;
                            String dupDeviceInfo = "";
                            if (listErrorPromos.size() == 0) {
                                notiType = 1;
                            } else {
                                for (ErrorPromotionTrackingDb.Obj errObj : listErrorPromos) {
                                    if (!"".equalsIgnoreCase(errObj.deviceInfo)) {
                                        notiType = 2;
                                        dupDeviceInfo = errObj.deviceInfo;
                                        break;
                                    }
                                }
                            }
                            if (notiType == 1) {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "notiType == 1" + phoneNumber);
                                //todo mo vi truoc thoi gian khuyen mai => tim thoi gian mo vi.
                                PhonesDb.Obj phoneObj = sockData.getPhoneObj();
                                if(phoneObj != null)
                                {
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000, String.format(PromoContentNotification.NOTI_BINH_TAN_NOT_NEW_NUMBER, Misc.dateVNFormatWithDot(phoneObj.createdDate)));
                                }
//                                JsonObject joObject = createJsonNotification(storeNumber, PromoContentNotification.NOTI_BINH_TAN_NOT_NEW_NUMBER, PromoContentNotification.NOTI_BINH_TAN_TITLE);
//                                Misc.sendStandardNoti(vertx, joObject);
                            } else if (notiType == 2) {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "notiType == 2 " + phoneNumber);
                                //todo Find duplicate device
                                findBeforeBonusDevice(dupDeviceInfo, storeNumber, phoneNumber);
                            } else {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "notyType == 3 " + phoneNumber);
                            }
                            log.writeLog();
                        }
                    });
                    return;
                } else if (acquireObjList.size() > 1) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thẻ này đã được sử dụng rồi, không trả thưởng. " + " phone 1 " + acquireObjList.get(0).phoneNumber + " bankId " + acquireObjList.get(0).bankId + " "
                            + " phone 2 " + acquireObjList.get(1).phoneNumber + " bankId " + acquireObjList.get(1).bankId);
                    log.writeLog();
                    //Chem
                    JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                    acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Thẻ này đã được sử dụng rồi, không trả thưởng và lock số này luôn. " + " phone 1 " + acquireObjList.get(0).phoneNumber + " bankId " + acquireObjList.get(0).bankId + " "
                                    + " phone 2 " + acquireObjList.get(1).phoneNumber + " bankId " + acquireObjList.get(1).bankId);
                        }
                    });
                    return;
                }
                AcquireBinhTanUserPromotionDb.Obj acquireObj = acquireObjList.get(0);
                if (acquireObj.end_group_3) {
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Da ket thuc chuong trinh khuyen mai " + phoneNumber);
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Da ket thuc chuong trinh khuyen mai " + " " + phoneNumber);
                    log.writeLog();
                    return;
                }
                joExtra.putObject(StringConstUtil.BinhTanPromotion.ACQUIRE_USER_OBJ, acquireObj.toJson());
                //Kiem tra thoi gian giao dich va so luong giao dich cua DGD do.
                phoneCheckDb.findOne(storeNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, new Handler<PhoneCheckDb.Obj>() {
                    @Override
                    public void handle(final PhoneCheckDb.Obj phoneDgdObj) {
                        if(phoneDgdObj != null)
                        {
//                            String program = "";
//                            if(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5.equalsIgnoreCase(phoneDgdObj.program))
//                            {
//                                program =
//                            }
//                            else if(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5.equalsIgnoreCase(phoneDgdObj.program))
//                            {
//
//                            }
//                            else {
//
//                            }
//                            String program = StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4.equalsIgnoreCase(phoneDgdObj.program) ? StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4
//                                    : StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5;
                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, phoneDgdObj.program + " phone: " + storeNumber);
                            if(!StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4.equalsIgnoreCase(phoneDgdObj.program) && !StringConstUtil.BinhTanPromotion.PROGRAM_GROUP5.equalsIgnoreCase(phoneDgdObj.program))
                            {
                                log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, phoneDgdObj.program + " phone: " + storeNumber + " da bi lock => khong cho tham gia chuong trinh");
                                log.writeLog();
                                logger.info("DGD " + storeNumber + " program " + phoneDgdObj.program);
                                return;
                            }
                            BinhTanPromotionObj.requestAcquireBinhTanUserPromo(vertx, phoneNumber, phoneDgdObj.program, StringConstUtil.BinhTanPromotion.CASHIN_SOURCE, joExtra, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject event) {
                                }
                            });
                        } else {
                            log.add("DESC " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, "Diem giao dich nay khong duoc tham gia chuong trinh Binh Tan, vui long ra diem giao dich khac");
                            log.writeLog();
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP4, 1000, "Diem giao dich nay khong duoc tham gia chuong trinh Binh Tan, vui long ra diem giao dich khac");
                            return;
                        }
                    }
                });
//                JsonObject joUpdate = new JsonObject();
//                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.AMOUNT_CASHIN, joExtra.getLong(StringConstUtil.AMOUNT, 0));
//                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TID_CASHIN, joExtra.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis()));
//                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_CASHIN, System.currentTimeMillis());
//                joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_2, true);
//                joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.GROUP, 3);
//                joUpdate.putString(colName.AcquireBinhTanUserPromotionCol.BANK_CARD_ID, joExtra.getString(StringConstUtil.BinhTanPromotion.BANK_ID, ""));
//                if (acquireObj.time_group_3 == 0) {
//                    joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.TIME_GROUP_3, System.currentTimeMillis());
//                }
//                acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
//                    @Override
//                    public void handle(Boolean event) {
//
//                    }
//                });
                return;
            }
        });
    }


    private void findBeforeBonusDevice(final String deviceInfo,final String storeNumber,final String phoneNumber)
    {
        String deviceInfoTmp = deviceInfo.split(MomoMessage.BELL + MomoMessage.BELL + MomoMessage.BELL).length > 1 ? deviceInfo.split(MomoMessage.BELL + MomoMessage.BELL + MomoMessage.BELL)[0] : "";

        if("".equalsIgnoreCase(deviceInfoTmp))
        {
            JsonObject joObject = createJsonNotification(storeNumber, PromoContentNotification.NOTI_BINH_TAN_DEVICE_REMIND, PromoContentNotification.NOTI_BINH_TAN_HAS_RECEIVE_BONUS_TITLE);
            Misc.sendStandardNoti(vertx, joObject);
        }
        else {
            String[] devicesInfos = deviceInfoTmp.split(MomoMessage.BELL);
            JsonObject joId = null;
            JsonArray jarrOr = new JsonArray();

            for(String info : devicesInfos)
            {
                if(!"".equalsIgnoreCase(info))
                {
                    joId = new JsonObject();
                    joId.putString(colName.DeviceDataUser.ID, info);
                    jarrOr.add(joId);

                }
            }

            if(jarrOr.size() > 0)
            {
                JsonObject joFilter = new JsonObject().putArray(MongoKeyWords.OR, jarrOr);
                deviceDataUserDb.searchWithFilter(StringConstUtil.BinhTanPromotion.PROGRAM, joFilter, new Handler<ArrayList<DeviceDataUserDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<DeviceDataUserDb.Obj> listDeviceInfos) {
                        if(listDeviceInfos.size() > 0)
                        {
                            String otherPhoneNumber = "";
                            for(DeviceDataUserDb.Obj deviceObj : listDeviceInfos)
                            {
                                if(!phoneNumber.equalsIgnoreCase(deviceObj.phoneNumber))
                                {
                                    otherPhoneNumber = deviceObj.phoneNumber;
                                    break;
                                }
                            }
                            JsonObject joObject = createJsonNotification(storeNumber, String.format(PromoContentNotification.NOTI_BINH_TAN_DEVICE_REMIND_SDT, otherPhoneNumber), PromoContentNotification.NOTI_BINH_TAN_HAS_RECEIVE_BONUS_TITLE);
                            Misc.sendStandardNoti(vertx, joObject);
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000, String.format(PromoContentNotification.NOTI_BINH_TAN_DEVICE_REMIND_SDT, otherPhoneNumber));
                        }
                        else {
                            JsonObject joObject = createJsonNotification(storeNumber, PromoContentNotification.NOTI_BINH_TAN_ERROR_DEFAULT, PromoContentNotification.NOTI_BINH_TAN_TITLE);
                            Misc.sendStandardNoti(vertx, joObject);
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_OUT, 1000, PromoContentNotification.NOTI_BINH_TAN_ERROR_DEFAULT);
                        }
                    }
                });
            }
            else {
                JsonObject joObject = createJsonNotification(storeNumber, PromoContentNotification.NOTI_BINH_TAN_DEVICE_REMIND, PromoContentNotification.NOTI_BINH_TAN_TITLE);
                Misc.sendStandardNoti(vertx, joObject);
            }


        }


    }
    private JsonObject createJsonNotification(String storeNumber, String content, String title)
    {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.StandardNoti.CAPTION, title);
        jo.putString(StringConstUtil.StandardNoti.BODY, content);
        jo.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, storeNumber);
        jo.putNumber(StringConstUtil.StandardNoti.TRAN_ID, System.currentTimeMillis());
        return jo;
    }

    public void updateInfoBillPayUser(final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "updateInfoBillPayUser " + phoneNumber);
        acquireBinhTanGroup2PromotionDb.findAndModifyUsedVoucher(phoneNumber, 0, 1, new Handler<AcquireBinhTanGroup2PromotionDb.Obj>() {
            @Override
            public void handle(AcquireBinhTanGroup2PromotionDb.Obj event) {
                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "update success " + phoneNumber);
                log.writeLog();
            }
        });

        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "updateInfoBillPayUser group 3 " + phoneNumber);
        final JsonArray joListGiftId = joExtra.getArray(StringConstUtil.GIFT_ID, new JsonArray());
        if(joListGiftId.size() > 0)
        {
            final AtomicInteger countGift = new AtomicInteger(joListGiftId.size());
            vertx.setPeriodic(250L, new Handler<Long>() {
                @Override
                public void handle(Long timer) {
                    int position = countGift.decrementAndGet();
                    if(position < 0)
                    {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "DONE UPDATE GIFT");
                        vertx.cancelTimer(timer);
                    }
                    else
                    {
                        acquireBinhTanGroup3PromotionDb.findAndModifyUsedVoucher(phoneNumber, joListGiftId.get(position).toString().trim(), new Handler<AcquireBinhTanGroup3PromotionDb.Obj>() {
                            @Override
                            public void handle(AcquireBinhTanGroup3PromotionDb.Obj event) {

                            }
                        });
                    }
                }
            });
        }
    }

    public void getEchoCommand(final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "getEchoCommand " + phoneNumber);

        BinhTanPromotionObj.requestAcquireBinhTanUserPromo(vertx, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM_GROUP2, StringConstUtil.BinhTanPromotion.ECHO, joExtra, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
            }
        });
    }

    public void getOTPCommand(final String phoneNumber, final Common.BuildLog log, final SockData sockData, final JsonObject joExtra)
    {
        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "getOTPCommand " + phoneNumber);
        final JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.LOCK_STATUS, true);

        acquireBinhTanUserPromotionDb.findAndIncOtpUser(phoneNumber, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
            @Override
            public void handle(AcquireBinhTanUserPromotionDb.Obj acquireObj) {
                if (acquireObj != null && acquireObj.numberOfOtp >= 2) {
                    joUpdate.putNumber(colName.AcquireBinhTanUserPromotionCol.NUMBER_OF_OTP, acquireObj.numberOfOtp);
                    acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                        }
                    });
                }
            }
        });
    }

    public void executeRegisterBonusUser(final String phoneNumber, final Common.BuildLog log, final String extraKey, final String os, final String imei)
    {
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
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
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.BinhTanPromotion.PROGRAM)) {
                            promo_start_date = promoObj.DATE_FROM;
                            promo_end_date = promoObj.DATE_TO;
                            agent = promoObj.ADJUST_ACCOUNT;
                            total_amount = promoObj.TRAN_MIN_VALUE;
                            perTranAmount = promoObj.PER_TRAN_VALUE;
                            break;
                        }
                    }
                    final PromotionDb.Obj finalPromoObj = promo_start_date > 0 ? promoObj : null;
                    //Check lan nua do dai chuoi ki tu
                    if ("".equalsIgnoreCase(agent) || finalPromoObj == null) {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                        JsonObject jsonReply = new JsonObject();
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Thieu thong tin agent hoac chuong trinh chua duoc cau hinh");
                        log.writeLog();
                        return;
                    }
                    else if(currentTime < promo_start_date || currentTime > promo_end_date)
                    {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tham gia ngoai thoi gian khuyen mai nen khong ghi nhan");
                        JsonObject jsonReply = new JsonObject();
                        jsonReply.putNumber(StringConstUtil.ERROR, -1000);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Tham gia ngoai thoi gian khuyen mai nen khong ghi nhan");
                        log.writeLog();
                        return;
                    }
                    //Luu thong tin khach hang
                    log.add("desc ", "Luu thong tin khach hang.");
                    log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM, "executeRegisterUser");
//                    final String extraKey = joExtra.getString(StringConstUtil.BinhTanPromotion.EXTRA_KEY, "");
                    if("".equalsIgnoreCase(extraKey) && !StringConstUtil.IOS_OS.equalsIgnoreCase(os))
                    {
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "App moi khong gui thong tin du lieu ca nhan app.");
                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "App khong gui thong tin du lieu ca nhan app. " + phoneNumber);
                        log.writeLog();
                        return;
                    }
                    final SockData sockData = new SockData(vertx, logger, glbConfig);
                    sockData.os = os;
                    sockData.imei = imei;
                    log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Kiem tra extra key");
                    log.add("extra key " + StringConstUtil.BinhTanPromotion.PROGRAM, extraKey);
                    getExtraKeyFromApp(phoneNumber, extraKey, log, sockData, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joResponse) {
                            int err = joResponse.getInteger(StringConstUtil.ERROR, 1000);
                            String desc = joResponse.getString(StringConstUtil.DESCRIPTION, "ERROR");
                            if(err == 0)
                            {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, desc);
                                log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM, "recordBinhTanPromotionUser");
                                JsonArray jarrOr = new JsonArray();
                                JsonObject joImei = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.IMEI, sockData.imei);
                                JsonObject joPhoneNumber = new JsonObject().putString(colName.AcquireBinhTanUserPromotionCol.PHONE_NUMBER, phoneNumber);
                                jarrOr.add(joImei);
                                jarrOr.add(joPhoneNumber);
                                JsonObject joFilter = new JsonObject();
                                joFilter.putArray(MongoKeyWords.OR, jarrOr);
                                acquireBinhTanUserPromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanUserPromotionDb.Obj>>() {
                                    @Override
                                    public void handle(ArrayList<AcquireBinhTanUserPromotionDb.Obj> listUserBinhTans) {
                                        if(listUserBinhTans.size() > 0)
                                        {
                                            boolean dupImei = false;
                                            for(AcquireBinhTanUserPromotionDb.Obj userObj: listUserBinhTans)
                                            {
                                                if(userObj.imei.equalsIgnoreCase(sockData.imei))
                                                {
                                                    dupImei = true;
                                                    break;
                                                }
                                            }
                                            if(dupImei)
                                            {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Trung IMEI roi nha bo !!!!!");
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Trung IMEI roi nha bo !!!!! " + phoneNumber);
                                            }
                                            else {
                                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Trung SDT roi nha bo !!!!!");
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "Trung SDT roi nha bo !!!!! " + phoneNumber);
                                            }
                                            log.writeLog();
                                            return;
                                        }
                                        recordBinhTanPromotionUser(phoneNumber, extraKey, sockData);
                                        log.writeLog();
                                        return;
                                    }
                                });
                                return;
                            }
                            else {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, desc);
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, desc + " " + phoneNumber);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     *
     * @param storeNumber
     * @param content
     * @param title
     * @param url
     * @return
     */
    private JsonObject createJsonNotification(String storeNumber, String content, String title, String url)
    {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.RedirectNoti.CAPTION, title);
        jo.putString(StringConstUtil.RedirectNoti.BODY, content);
        jo.putString(StringConstUtil.RedirectNoti.RECEIVER_NUMBER, storeNumber);
        jo.putNumber(StringConstUtil.RedirectNoti.TRAN_ID, System.currentTimeMillis());
        jo.putString(StringConstUtil.RedirectNoti.URL, url);
        return jo;
    }

}
