package com.mservice.momo.vertx.processor.referral;

import com.mservice.common.BankInfo;
import com.mservice.momo.data.Card;
import com.mservice.momo.data.MappingWalletBankDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.binhtanpromotion.DeviceDataUserDb;
import com.mservice.momo.data.codeclaim.ClaimCodePromotionObj;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.referral.ReferralPromotionObj;
import com.mservice.momo.data.referral.ReferralV1CodeInputDb;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.util.claimcode.ClaimCodeUtils;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import com.mservice.visa.entity.VisaResponse;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 4/4/16.
 */
public class ReferralProcess extends PromotionProcess{

    Card card;
    ReferralV1CodeInputDb referralV1CodeInputDb;
    DeviceDataUserDb deviceDataUserDb;
    public ReferralProcess(Vertx vertx, Logger logger, JsonObject glbCfg) {
        super(vertx, logger, glbCfg);
        card = new Card(vertx.eventBus(), logger);
        referralV1CodeInputDb = new ReferralV1CodeInputDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        deviceDataUserDb = new DeviceDataUserDb(vertx, logger);
    }

    public void checkReferralPromotion(final Message<JsonObject> message,final Common.BuildLog log,final ClaimCodePromotionObj claimCodePromotionObj, JsonObject joExtra)
    {
        log.add("class"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "ReferralProcess" );
        log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "checkReferralPromotion");
        final String program = ClaimCodeUtils.checkClaimCodeProgram(claimCodePromotionObj.claimed_code);
        final JsonObject joReply = new JsonObject();
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
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj claim_promo = null;
                    JsonObject jsonTime = new JsonObject();
                    for (Object o : array) {
                        claim_promo = new PromotionDb.Obj((JsonObject) o);
                        if (claim_promo.NAME.equalsIgnoreCase(program.toString().trim())) {
                            promo_start_date = claim_promo.DATE_FROM;
                            promo_end_date = claim_promo.DATE_TO;
                            agent = claim_promo.ADJUST_ACCOUNT;
                            break;
                        }
                    }
                    if(promo_start_date > System.currentTimeMillis() || promo_end_date < System.currentTimeMillis())
                    {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Chuong trinh chua bat dau");
                        joReply.putNumber(StringConstUtil.ERROR, -1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Đã hết thời gian triển khai chương trình " + claim_promo.INTRO_DATA + ". Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                        message.reply(joReply);
                        log.writeLog();
                        return;
                    }
                    else if(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM.equalsIgnoreCase(program.toString().trim()))
                    {
                        log.add("INFO REFERRAL"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Chay chuong trinh REFERRAL V1");
                        AtomicInteger atomicCount = new AtomicInteger();
                        atomicCount.incrementAndGet();
                        if(atomicCount.intValue() == 1)
                        {
                            final PhonesDb.Obj phoneObj = new PhonesDb.Obj(claimCodePromotionObj.joExtra.getObject(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ, new JsonObject()));
                            if(phoneObj == null || phoneObj.number == 0 || "".equalsIgnoreCase(phoneObj.deviceInfo))
                            {
                                log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thieu thong tin device, vui long download app tu Store");
                                joReply.putNumber(StringConstUtil.ERROR, -1000);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Thiếu thông tin device, vui lòng down ứng dụng từ Store" + ". Vui lòng kiểm tra lại hoặc gọi 1900 5454 41 để được hỗ trợ.");
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }
                            log.add("extra key " + program, phoneObj.deviceInfo);
                            final String extraKey = phoneObj.deviceInfo;
                            String extraKeyAndroid = "";
                            final JsonObject joFilter = new JsonObject();
                            String os = "".equalsIgnoreCase(phoneObj.phoneOs) ? extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL).length > 1
                                    ? StringConstUtil.ANDROID_OS : StringConstUtil.IOS_OS : phoneObj.phoneOs;
                            if(StringConstUtil.WINDOW_OS.equalsIgnoreCase(os))
                            {
                                log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "WINDOW PHONES khong cho choi");
                                joReply.putNumber(StringConstUtil.ERROR, -1000);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị WINDOW PHONES không thể tham gia chương trình này");
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            }
                            else if(StringConstUtil.ANDROID_OS.equalsIgnoreCase(os))
                            {
                                final String[] extraKeyArr = extraKey.split(MomoMessage.BELL+MomoMessage.BELL+MomoMessage.BELL);
                                log.add("extra keyArr length " + program, extraKeyArr.length);
                                extraKeyAndroid = extraKeyArr.length > 1 ? extraKeyArr[0] : extraKey;
                                log.add("extraKeyAndroid " + program, extraKeyAndroid);

                                //KIEM TRA ANDROID moi
                                String extraKeyTemp = extraKeyArr.length > 1 ? extraKeyArr[1] : "";

                                if(!"".equalsIgnoreCase(extraKeyTemp))
                                {
                                    String []extraKeyTempArr = extraKeyTemp.split(MomoMessage.BELL);
                                    for(int i = 0; i < extraKeyTempArr.length; i++)
                                    {
                                        //bo check sim, van giu nguyen check device ao! Cong Nguyen 04/08/2016
                                        if(/*"XXX".equalsIgnoreCase(extraKeyTempArr[i].toString().trim()) ||*/ "-1".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())
                                                || "".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())
                                                /*|| "0".equalsIgnoreCase(extraKeyTempArr[i].toString().trim())*/)
                                        {
                                            joReply.putNumber(StringConstUtil.ERROR, 1000);
                                            joReply.putString(StringConstUtil.DESCRIPTION, "Bạn không đủ điều kiện để tham gia chương trình. Vui lòng kiểm tra lại hoặc gọi 1900 5454 41 để được hỗ trợ.");
                                            log.add("error " + program, "Bạn không đủ điều kiện để tham gia chương trình. Vui lòng kiểm tra lại hoặc gọi 1900 5454 41 để được hỗ trợ.");
                                            JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Bạn không đủ điều kiện để tham gia chương trình. Vui lòng kiểm tra lại hoặc gọi 1900 5454 41 để được hỗ trợ. " + " " + "0" + phoneObj.number);
                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, "0" + phoneObj.number, program, 1000, joDesc.toString());
                                            message.reply(joReply);
                                            log.writeLog();
                                            return;
                                        }
                                    }
                                }
                                String deviceInfo = extraKeyArr[0].toString().trim();
                                String[] deviceInfos = deviceInfo.split(MomoMessage.BELL);

                                if(deviceInfos.length < 2)
                                {
                                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                                    joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
                                    log.add("error " + program, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn");
                                    JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã thiếu thông tin từ device ANDROID, vui lòng download app từ google play. Xin cám ơn " + " " + "0" + phoneObj.number);
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, "0" + phoneObj.number, program, 1000, joDesc.toString());
                                    message.reply(joReply);
                                    log.writeLog();
                                    return;
                                }
                                JsonArray joArrOr = new JsonArray();
                                for(String info : deviceInfos)
                                {
                                    if(!"".equalsIgnoreCase(info))
                                    {
                                        joArrOr.add(new JsonObject().putString(colName.DeviceDataUser.ID, info));
                                    }
                                }
                                joFilter.putArray(MongoKeyWords.OR, joArrOr);
                            }
                            else if(StringConstUtil.IOS_OS.equalsIgnoreCase(os)) {
                                joFilter.putString(colName.DeviceDataUser.ID, phoneObj.lastImei);
                            }
                            final PromotionDb.Obj claim_promo_final = claim_promo;
                            log.add("jofilter " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, joFilter.toString());
                            deviceDataUserDb.searchWithFilter(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, joFilter, new Handler<ArrayList<DeviceDataUserDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<DeviceDataUserDb.Obj> deviceInfoList) {
                                    boolean isOK = true;
                                    String otherPhoneNumber = "";
                                    for (DeviceDataUserDb.Obj devInfo : deviceInfoList) {
                                        log.add("claim number " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, claimCodePromotionObj.phoneNumber);
                                        log.add("id | number " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, devInfo.id + "|" + devInfo.phoneNumber);
                                        //Neu co bat ki 1 thong tin nao co so dien thoai khac voi so dien thoai dang nhap code thi tra loi
                                        if (!claimCodePromotionObj.phoneNumber.equalsIgnoreCase(devInfo.phoneNumber) && !"02:00:00:00:00:00".equalsIgnoreCase(devInfo.id.trim())) {
                                            isOK = false;
                                            otherPhoneNumber = devInfo.phoneNumber;
                                            break;
                                        }
                                    }
                                    log.add("isOK " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, isOK);
                                    if (isOK) {
                                        executeReferralV1Promotion(message, log, claimCodePromotionObj, claim_promo_final);
                                    }
                                    else if(!joFilter.toString().contains(colName.DeviceDataUser.ID))
                                    {
                                        joReply.putNumber(StringConstUtil.ERROR, 2001);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "Cập nhật thông tin người giới thiệu không thành công. Vui lòng đăng nhập lại ứng dụng và cập nhật lại thông tin người giới thiệu. Xin cám ơn.");
                                        log.add("error " + program, "Thieu thong tin filter");
                                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Cập nhật thông tin người giới thiệu không thành công. Vui lòng nhập lại thông tin. Xin cám ơn.");
                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, "0" + phoneObj.number, program, 1000, joDesc.toString());
                                        message.reply(joReply);
                                        log.writeLog();
                                        return;
                                    }
                                    else {
                                        joReply.putNumber(StringConstUtil.ERROR, 2001);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "Thiết bị của bạn đã được nhận thưởng từ chương trình Chia sẻ MoMo trong vai trò là người được giới thiệu. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                                        log.add("error " + program, "Thiết bị của bạn đã được nhận thưởng từ chương trình Chia sẻ MoMo trong vai trò là người được giới thiệu. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h. SĐT: ");
                                        JsonObject joDesc = new JsonObject().putString(StringConstUtil.DEVICE_IMEI, extraKey).putString(StringConstUtil.DESCRIPTION, "Thiết bị này đã được tham gia nhận thưởng từ số điện thoại " + otherPhoneNumber + ". Vui lòng sử dụng thiết bị di động khác để tham gia giới thiệu bạn bè");
                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, "0" + phoneObj.number, program, 1000, joDesc.toString());
                                        message.reply(joReply);
                                        log.writeLog();
                                        return;
                                    }
                                }
                            });

                            return;
                        }
                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Gui nhieu lenh xuong MONGO " + claimCodePromotionObj.phoneNumber);
                        log.writeLog();
                        return;
                    }
                    else {
                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Sai chuong trinh Referral");
                        joReply.putNumber(StringConstUtil.ERROR, 2001);
                        //Cong Nguyen change BA 01/08/2016
                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                        message.reply(joReply);
                        log.writeLog();
                        return;
                    }
                }
                return;
            }
        });
    }

    private void executeReferralV1Promotion(final Message<JsonObject> message,final Common.BuildLog log, final ClaimCodePromotionObj claimCodePromotionObj, final PromotionDb.Obj claimPromoObj)
    {
        final JsonObject joReply = new JsonObject();
        int phoneNumber = checkReferralV1InviterPhones(claimCodePromotionObj.claimed_code);
        JsonObject joPhoneObj = claimCodePromotionObj.joExtra.containsField(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ) ?
                claimCodePromotionObj.joExtra.getObject(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ, new JsonObject())
                : new JsonObject();
        if(phoneNumber == -1)
        {
            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So nguoi nhan sai format " + claimCodePromotionObj.claimed_code);
            joReply.putNumber(StringConstUtil.ERROR, 2001);
            //Cong Nguyen change BA 01/08/2016
            joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
            message.reply(joReply);
            log.writeLog();
            return;
        }

        //Kiem tra xem nguoi gioi thieu co dang lien ket visa hay lien ket ngan hang khong.
        final PhonesDb.Obj inviteePhoneObj = new PhonesDb.Obj(joPhoneObj);

        phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(final PhonesDb.Obj phoneObj) {
                if(phoneObj == null)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So nguoi nhan khong ton tai " + claimCodePromotionObj.claimed_code);
                    joReply.putNumber(StringConstUtil.ERROR, 2001);
                    //Cong Nguyen change BA 01/08/2016
                    joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
                else if(!"0".equalsIgnoreCase(inviteePhoneObj.bank_code) || !"".equalsIgnoreCase(inviteePhoneObj.bank_name)
                        || !"".equalsIgnoreCase(inviteePhoneObj.bankPersonalId) || !"".equalsIgnoreCase(inviteePhoneObj.bank_account))
                {
                    log.add("ERROR "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Co thong tin ngan hang cua nguoi nhap code roi.");
                    joReply.putNumber(StringConstUtil.ERROR, 2001);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Ví của bạn đã từng liên kết tài khoản ngân hàng nên không được tham gia chương trình Chia Sẻ MoMo trong vai trò người được giới thiệu. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
                //Cong Nguyen 10/08/2016 remove logic check visa
                else if("0".equalsIgnoreCase(phoneObj.bank_code) || "".equalsIgnoreCase(phoneObj.bank_code))
                {
                    //Kiem tra tiep xem nguoi gui co map visa ko
//                    card.findAllCardWithDeletedCard(inviteePhoneObj.number, new Handler<ArrayList<Card.Obj>>() {
//                        @Override
//                        public void handle(ArrayList<Card.Obj> inviteeListCards) {
//                            if(inviteeListCards.size() > 0)
//                            {
//                                log.add("So the nguoi nhap code dang map la: " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, inviteeListCards.size());
//                                log.add("ERROR "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Co thong tin ngan hang cua nguoi nhap code roi.");
//                                joReply.putNumber(StringConstUtil.ERROR, -1000);
//                                joReply.putString(StringConstUtil.DESCRIPTION, "Chương trình khuyến mãi áp dụng với ví chưa liên kết thẻ/ tài khoản thẻ. Vui lòng kiểm tra lại hoặc gọi 1900 5454 41 để được hỗ trợ.");
//                                message.reply(joReply);
//                                log.writeLog();
//                                return;
//                            }
//                            card.findAll(phoneObj.number, new Handler<ArrayList<Card.Obj>>() {
//                                @Override
//                                public void handle(ArrayList<Card.Obj> listCards) {
//                                    if(listCards.size() == 0)
//                                    {
//                                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "so nguoi gioi thieu chua map vi " + claimCodePromotionObj.claimed_code);
//                                        joReply.putNumber(StringConstUtil.ERROR, 2001);
//                                        //Cong Nguyen change BA 01/08/2016
//                                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ: người giới thiệu đang không liên kết ví MoMo với tài khoản ngân hàng. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h");
//                                        message.reply(joReply);
//                                        log.writeLog();
//                                        return;
//                                    }
//                                    log.add("so luong card" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, listCards.size());
////                                    List<Card.Obj> listVisaCard = new ArrayList<>();
////                                    for(Card.Obj cardObj : listCards)
////                                    {
////                                        if(!cardObj.deleted && StringConstUtil.PHONES_BANKID_SBS.equalsIgnoreCase(cardObj.bankid))
////                                        {
////                                            log.add("add the visa voi card la "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, cardObj.cardCheckSum);
////                                            listVisaCard.add(cardObj);
////                                            break;
////                                        }
////                                    }
////                                    if(listVisaCard.size() == 0)
////                                    {
//                                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "so nguoi gioi thieu chua map vi " + claimCodePromotionObj.claimed_code);
//                                        joReply.putNumber(StringConstUtil.ERROR, 2001);
//                                        //Cong Nguyen change BA 01/08/2016
//                                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
//                                        message.reply(joReply);
//                                        log.writeLog();
//                                        return;
////                                    }
////                                    log.add("thong tin card visa "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, listVisaCard.get(0).cardCheckSum);
////                                    checkCodeInputAndBonus(phoneObj, inviteePhoneObj, log, joReply, message, listVisaCard, claimPromoObj);
//                                }
//                            });
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "so nguoi gioi thieu chua map vi " + claimCodePromotionObj.claimed_code);
                    joReply.putNumber(StringConstUtil.ERROR, 2001);
                    //Cong Nguyen change BA 01/08/2016
                    joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ: người giới thiệu đang không liên kết ví MoMo với tài khoản ngân hàng được áp dụng. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h");
                    message.reply(joReply);
                    log.writeLog();
                    return;
//                        }
//                    });
                }
                //Cong Nguyen 04/08/2016 reject vietin bank
                else if(!"".equalsIgnoreCase(phoneObj.bankPersonalId)){
                    checkBankIsAccepted(phoneObj.bank_code, claimPromoObj, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean res) {
                            if(!res) {
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "so nguoi nhan lien ket vietinbank " + claimCodePromotionObj.claimed_code);
                                joReply.putNumber(StringConstUtil.ERROR, 2001);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ: người giới thiệu đang không liên kết ví MoMo với tài khoản ngân hàng được áp dụng. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h");
                                message.reply(joReply);
                                log.writeLog();
                                return;
                            } else {
                                log.add("thong tin bank "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneObj.bankPersonalId);
                                final List<Card.Obj> listCard = new ArrayList<Card.Obj>();
                                checkCodeInputAndBonus(phoneObj, inviteePhoneObj, log, joReply, message, listCard, claimPromoObj);
                            }
                        }
                    });
//                    if(/*reject vietinbank*/phoneObj.bank_code.equalsIgnoreCase("102")) {
//                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "so nguoi nhan lien ket vietinbank " + claimCodePromotionObj.claimed_code);
//                        joReply.putNumber(StringConstUtil.ERROR, 2001);
//                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
//                        message.reply(joReply);
//                        log.writeLog();
//                        return;
//                    }
//                    log.add("thong tin bank "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneObj.bankPersonalId);
//                    final List<Card.Obj> listCard = new ArrayList<Card.Obj>();
//                    card.findAllCardWithDeletedCard(inviteePhoneObj.number, new Handler<ArrayList<Card.Obj>>() {
//                        @Override
//                        public void handle(ArrayList<Card.Obj> inviteeListCards) {
//////                            if (inviteeListCards.size() > 0) {
////                                log.add("So the nguoi nhap code dang map la: " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, inviteeListCards.size());
////                                log.add("ERROR " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Co thong tin ngan hang cua nguoi nhap code roi.");
////                                joReply.putNumber(StringConstUtil.ERROR, -1000);
////                                joReply.putString(StringConstUtil.DESCRIPTION, "Chương trình khuyến mãi áp dụng với ví chưa liên kết thẻ/ tài khoản thẻ. Vui lòng kiểm tra lại hoặc gọi 1900 5454 41 để được hỗ trợ.");
////                                message.reply(joReply);
////                                log.writeLog();
////                                return;
////                            }
//                            checkCodeInputAndBonus(phoneObj, inviteePhoneObj, log, joReply, message, listCard, claimPromoObj);
//                        }
//                    });
                    return;
                }
                else {
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "khong co thong tin bankPersonalId " + phoneObj.bankPersonalId);
                    joReply.putNumber(StringConstUtil.ERROR, 2001);
                    //Cong Nguyen change BA 01/08/2016
                    joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ: người giới thiệu đang không liên kết ví MoMo với tài khoản ngân hàng được áp dụng. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
            }
        });


    }

    private void checkCodeInputAndBonus(final PhonesDb.Obj phoneObj, final PhonesDb.Obj inviteePhoneObj, final Common.BuildLog log, final JsonObject joReply, final Message<JsonObject> message, List<Card.Obj> listCard, final PromotionDb.Obj claimPromoObj) {
        //Kiem tra so dien thoai da nhan thuong chua, neu roi khong cho nhap code nua.
        final String cardInfo = listCard.size() > 0 ? listCard.get(0).cardCheckSum : phoneObj.bankPersonalId;
        final String bankCode=  listCard.size() > 0 ? "sbs" : phoneObj.bank_code;
        referralV1CodeInputDb.findOne("0" + inviteePhoneObj.number, new Handler<ReferralV1CodeInputDb.Obj>() {
            @Override
            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                if(referralObj != null && !"".equalsIgnoreCase(referralObj.inviteeCardInfo) && referralObj.isMapped)
                {
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai " + referralObj.inviteeNumber + "da nhap code va nhan thuong thanh " +
                            "cong truoc do tu so dien thoai " + referralObj.inviterNumber + " thoi gian tra thuong " + Misc.dateVNFormatWithDot(referralObj.bonus_time));
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Da ghi nhan so duoc tra thuong: nguoi nhap code " + inviteePhoneObj.number
                                + " so nguoi duoc nhap code " + phoneObj.number);
                    joReply.putNumber(StringConstUtil.ERROR, 2001);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã được nhận thưởng từ chương trình Chia sẻ MoMo trong vai trò là người được giới thiệu. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                }
                checkInviteeInfo(cardInfo, bankCode, log, inviteePhoneObj, joReply, message, phoneObj, claimPromoObj);
            }
        });
    }

    /**
     *
     *
     * @param cardInfo
     * @param bankCode
     * @param log
     * @param inviteePhoneObj
     * @param joReply
     * @param message
     * @param phoneObj
     * @param claimPromoObj
     */
    private void checkInviteeInfo(final String cardInfo, final String bankCode, final Common.BuildLog log, final PhonesDb.Obj inviteePhoneObj, final JsonObject joReply, final Message<JsonObject> message, final PhonesDb.Obj phoneObj, final PromotionDb.Obj claimPromoObj) {
        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thoa dieu kien nguoi gioi thieu");
//        if(inviteePhoneObj.createdDate < (System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30))
//        {
//            log.add("ngay tao vi qua lau, < 30 ngay "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, inviteePhoneObj.createdDate + " < " + (System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
//            joReply.putNumber(StringConstUtil.ERROR, -1000);
//            joReply.putString(StringConstUtil.DESCRIPTION, "Chương trình khuyến mãi áp dụng với ví được tạo trong vòng 30 ngày gần nhất. Vui lòng kiểm tra lại hoặc gọi 1900 5454 41 để được hỗ trợ.");
//            message.reply(joReply);
//            log.writeLog();
//            return;
//        }
//        else
        checkBankIsAccepted(inviteePhoneObj.bank_code, claimPromoObj, new Handler<Boolean>() {
            @Override
            public void handle(Boolean res) {
                if(res) {
                    log.add("ERROR "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Co thong tin ngan hang cua nguoi nhap code roi.");
                    joReply.putNumber(StringConstUtil.ERROR, 2001);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Ví của bạn đã từng liên kết tài khoản ngân hàng nên không được tham gia chương trình Chia Sẻ MoMo trong vai trò người được giới thiệu. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                    message.reply(joReply);
                    log.writeLog();
                    return;
                } else {
                    if(!"".equalsIgnoreCase(inviteePhoneObj.cardId) && (phoneObj.cardId.equalsIgnoreCase(inviteePhoneObj.cardId) || phoneObj.bankPersonalId.equalsIgnoreCase(inviteePhoneObj.cardId)))
                    {
                        log.add("ERROR "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thong tin dinh danh cua nguoi nhap va nguoi moi giong nhau ." + phoneObj.cardId + " " + phoneObj.bankPersonalId + " " + inviteePhoneObj.cardId);
                        joReply.putNumber(StringConstUtil.ERROR, 2001);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                        message.reply(joReply);
                        log.writeLog();
                        return;
                    }
                    else if(phoneObj.isAgent)
                    {
                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So nguoi nhan la diem giao dich " + phoneObj.number);
                        joReply.putNumber(StringConstUtil.ERROR, 2001);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                        message.reply(joReply);
                        log.writeLog();
                        return;
                    }
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Kiem tra dieu kien cua nguoi nhap code");
                    log.add("Thong tin nguoi nhap code "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, inviteePhoneObj.number);

                    log.add("kiem tra xem da thoi gian mo vi "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, inviteePhoneObj.createdDate);

                    log.add("INFO REFERRAL "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,"So dien thoai nguoi nhap code da thoa dieu kien");
                    log.add("INFO REFERRAL "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,"Luu lai thong tin khach hang ");
                    JsonObject joUpsert = new JsonObject();
                    joUpsert.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, "0" + inviteePhoneObj.number);
                    joUpsert.putString(colName.ReferralV1CodeInputCol.INVITER_NUMBER, "0" + phoneObj.number);
                    joUpsert.putString(colName.ReferralV1CodeInputCol.INVITER_BANK_CODE, bankCode);
                    joUpsert.putNumber(colName.ReferralV1CodeInputCol.INPUT_TIME, System.currentTimeMillis());
                    joUpsert.putString(colName.ReferralV1CodeInputCol.INVITER_CARD_INFO, cardInfo);
                    joUpsert.putNumber(colName.ReferralV1CodeInputCol.IS_BLOCK, 0);
                    referralV1CodeInputDb.upSert("0" + inviteePhoneObj.number, joUpsert, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean isResult) {
                            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Da cap nhat thong tin gioi thieu cho so dien thoai " + inviteePhoneObj.number + " sdt gioi thieu: "
                                    + phoneObj.number);
                            log.writeLog();
                            joReply.putNumber(StringConstUtil.ERROR, 2001);

                            //change message from BA changed 01/08/2016
                            joReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã nhập mã thành công! Hãy liên kết tài khoản ngân hàng và nạp tiền/thanh toán để nhận thẻ quà tặng 100.000đ nạp tiền điện thoại! Chạm để xem hướng dẫn. Liên hệ hotro@momo.vn để được hỗ trợ trong vòng 24h.");
                            joReply.putString(StringConstUtil.URL, "https://momo.vn/chiasemomo/huong-dan-lien-ket.html");


                            message.reply(joReply);
                            /*********************Notify automatic**********************/
                            //Cong Nguyen 11/07/2016 notify ve app cua user A(nguoi gioi thieu)
                            referralV1CodeInputDb.findOne("0" + inviteePhoneObj.number, new Handler<ReferralV1CodeInputDb.Obj>() {
                                @Override
                                public void handle(ReferralV1CodeInputDb.Obj obj) {
                                    String phoneNumber = "0" + phoneObj.number;
                                    if(!obj.noti.equalsIgnoreCase(phoneNumber)) {
                                        logger.info("Nhap ma khuyen mai la so nguoi gioi thieu: " + phoneNumber);
                                        String body = PromoContentNotification.NOTI_AUTO_REFFERAL_UA_BODY.replaceAll("%contact%","0" + inviteePhoneObj.number);
                                        logger.info("message noti: " + body);
                                        String url = "https://momo.vn/chiasemomo/huong-dan-lien-ket.html";
                                        //Misc.sendPopupReferral(vertx ,phoneNumber, body, PromoContentNotification.CHIA_SE_MOMO_TITLE, url);
                                        Misc.sendRedirectNoti(vertx, createJsonNotification(phoneNumber, body, claimPromoObj.NOTI_CAPTION, url ));
                                        referralV1CodeInputDb.findAndUpdateInfoUser("0" + inviteePhoneObj.number,
                                                new JsonObject().putString(colName.ReferralV1CodeInputCol.NOTI, phoneNumber),
                                                new Handler<ReferralV1CodeInputDb.Obj>() {
                                                    @Override
                                                    public void handle(ReferralV1CodeInputDb.Obj event) {

                                                    }
                                                });
                                    }
                                }
                            });

                            /*******************************************/
                        }
                    });
                }
            }
        });
//        if(!"0".equalsIgnoreCase(inviteePhoneObj.bank_code) || !"".equalsIgnoreCase(inviteePhoneObj.bank_name)
//                || !"".equalsIgnoreCase(inviteePhoneObj.bankPersonalId) || !"".equalsIgnoreCase(inviteePhoneObj.bank_account))
//        {
//            log.add("ERROR "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Co thong tin ngan hang cua nguoi nhap code roi.");
//            joReply.putNumber(StringConstUtil.ERROR, 2001);
//            joReply.putString(StringConstUtil.DESCRIPTION, "Ví của bạn đã từng liên kết tài khoản ngân hàng nên không được tham gia chương trình Chia Sẻ MoMo trong vai trò người được giới thiệu. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
//            message.reply(joReply);
//            log.writeLog();
//            return;
//        }
//        else if(!"".equalsIgnoreCase(inviteePhoneObj.cardId) && (phoneObj.cardId.equalsIgnoreCase(inviteePhoneObj.cardId) || phoneObj.bankPersonalId.equalsIgnoreCase(inviteePhoneObj.cardId)))
//        {
//            log.add("ERROR "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thong tin dinh danh cua nguoi nhap va nguoi moi giong nhau ." + phoneObj.cardId + " " + phoneObj.bankPersonalId + " " + inviteePhoneObj.cardId);
//            joReply.putNumber(StringConstUtil.ERROR, 2001);
//            joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
//            message.reply(joReply);
//            log.writeLog();
//            return;
//        }
//        else if(phoneObj.isAgent)
//        {
//            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So nguoi nhan la diem giao dich " + phoneObj.number);
//            joReply.putNumber(StringConstUtil.ERROR, 2001);
//            joReply.putString(StringConstUtil.DESCRIPTION, "Mã khuyến mãi không hợp lệ. Vui lòng kiểm tra lại hoặc gửi thắc mắc của bạn về: hotro@momo.vn để được hỗ trợ trong vòng 24h.");
//            message.reply(joReply);
//            log.writeLog();
//            return;
//        }
//        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Kiem tra dieu kien cua nguoi nhap code");
//        log.add("Thong tin nguoi nhap code "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, inviteePhoneObj.number);
//
//        log.add("kiem tra xem da thoi gian mo vi "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, inviteePhoneObj.createdDate);
//
//        log.add("INFO REFERRAL "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,"So dien thoai nguoi nhap code da thoa dieu kien");
//        log.add("INFO REFERRAL "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,"Luu lai thong tin khach hang ");
//        JsonObject joUpsert = new JsonObject();
//        joUpsert.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, "0" + inviteePhoneObj.number);
//        joUpsert.putString(colName.ReferralV1CodeInputCol.INVITER_NUMBER, "0" + phoneObj.number);
//        joUpsert.putString(colName.ReferralV1CodeInputCol.INVITER_BANK_CODE, bankCode);
//        joUpsert.putNumber(colName.ReferralV1CodeInputCol.INPUT_TIME, System.currentTimeMillis());
//        joUpsert.putString(colName.ReferralV1CodeInputCol.INVITER_CARD_INFO, cardInfo);
//        joUpsert.putNumber(colName.ReferralV1CodeInputCol.IS_BLOCK, 0);
//        referralV1CodeInputDb.upSert("0" + inviteePhoneObj.invitee_number, joUpsert, new Handler<Boolean>() {
//            @Override
//            public void handle(Boolean isResult) {
//                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Da cap nhat thong tin gioi thieu cho so dien thoai " + inviteePhoneObj.number + " sdt gioi thieu: "
//                        + phoneObj.number);
//                log.writeLog();
//                joReply.putNumber(StringConstUtil.ERROR, 2001);
//
//                //change message from BA changed 01/08/2016
//                joReply.putString(StringConstUtil.DESCRIPTION, "Bạn đã nhập mã thành công! Hãy liên kết tài khoản ngân hàng và nạp tiền/thanh toán để nhận thẻ quà tặng 100.000đ nạp tiền điện thoại! Chạm để xem hướng dẫn. Liên hệ hotro@momo.vn để được hỗ trợ trong vòng 24h.");
//                joReply.putString(StringConstUtil.URL, "https://momo.vn/chiasemomo/huong-dan-lien-ket.html");
//
//
//                message.reply(joReply);
//                /*********************Notify automatic**********************/
//                //Cong Nguyen 11/07/2016 notify ve app cua user A(nguoi gioi thieu)
//                referralV1CodeInputDb.findOne("0" + inviteePhoneObj.number, new Handler<ReferralV1CodeInputDb.Obj>() {
//                    @Override
//                    public void handle(ReferralV1CodeInputDb.Obj obj) {
//                        String phoneNumber = "0" + phoneObj.number;
//                        if(!obj.noti.equalsIgnoreCase(phoneNumber)) {
//                            logger.info("Nhap ma khuyen mai la so nguoi gioi thieu: " + phoneNumber);
//                            String body = PromoContentNotification.NOTI_AUTO_REFFERAL_UA_BODY.replaceAll("%contact%","0" + inviteePhoneObj.number);
//                            logger.info("message noti: " + body);
//                            String url = "https://momo.vn/chiasemomo/huong-dan-lien-ket.html";
//                            //Misc.sendPopupReferral(vertx ,phoneNumber, body, PromoContentNotification.CHIA_SE_MOMO_TITLE, url);
//                            Misc.sendRedirectNoti(vertx, createJsonNotification(phoneNumber, body, claimPromoObj.NOTI_CAPTION, url ));
//                            referralV1CodeInputDb.findAndUpdateInfoUser("0" + inviteePhoneObj.number,
//                                    new JsonObject().putString(colName.ReferralV1CodeInputCol.NOTI, phoneNumber),
//                                    new Handler<ReferralV1CodeInputDb.Obj>() {
//                                @Override
//                                public void handle(ReferralV1CodeInputDb.Obj event) {
//
//                                }
//                            });
//                        }
//                    }
//                });
//
//                /*******************************************/
//            }
//        });
    }

    private int checkReferralV1InviterPhones(String code)
    {
//        String[] code_tmp = code.split(StringConstUtil.ReferralVOnePromoField.SPLIT_SYMBOL);
        if(code.length() < 7)
        {
            return -1;
        }
        int phoneNumber = DataUtil.strToInt(code.toString().trim());
        if(phoneNumber > 0 && Misc.checkNumber(phoneNumber))
        {
            return phoneNumber;
        }
        return -1;
    }

    public void checkBonusReferralV1Promotion(final SockData data, final VisaResponse visaResponse, final BankInfo bankInfo, final Common.BuildLog log)
    {
        log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "checkBonusReferralV1Promotion");
        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Kiem tra thong tin co nhap code truoc khong ???");
        final String phoneNumber = visaResponse != null ? visaResponse.getVisaRequest().getPhoneNumber()
                        : bankInfo.getPhoneNumber();
        final String bankCode = visaResponse != null ? "sbs" : bankInfo.getCoreBankCode();
        final String cardInfo = visaResponse != null ? visaResponse.getCardChecksum() : bankInfo.getCustomerId();
        final String deviceImei = data != null ? data.imei : "";
        log.add("bankcode checkBonusReferralV1Promotion " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, bankCode);
        log.add("cardInfo checkBonusReferralV1Promotion " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, cardInfo);
        log.add("deviceImei checkBonusReferralV1Promotion " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, deviceImei);
        referralV1CodeInputDb.findOne(phoneNumber, new Handler<ReferralV1CodeInputDb.Obj>() {
            @Override
            public void handle(final ReferralV1CodeInputDb.Obj referralObj) {
                if(referralObj == null)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong tham gia input code " + phoneNumber);
                    log.writeLog();
                    return;
                }
                else if(referralObj.isMapped)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Sdt " + phoneNumber + " da duoc nhan thuong nen khong cap nhat du lieu ngan hang.");
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Da duoc nhan thuong roi nen khong duoc tham gia chuong trinh nay nua em nhe");
                    return;
                }
                else if(cardInfo.equalsIgnoreCase(referralObj.inviterCardInfo))
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thong tin ma the trung voi thong tin the nguoi gioi thieu " + referralObj.inviterCardInfo);
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thong tin cmnd/the visa trung voi thong tin nguoi gioi thieu, khong tra thuong");
                    return;
                }
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj promoObj = null;
                            JsonObject jsonTime = new JsonObject();
                            for (Object o : array) {
                                promoObj = new PromotionDb.Obj((JsonObject) o);
                                if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM)) {
                                    break;
                                }
                            }
                            checkBankIsAccepted(bankCode, promoObj, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean res) {
                                    if (res) {
                                        phonesDb.getPhoneObjInfo(DataUtil.strToInt(referralObj.inviterNumber), new Handler<PhonesDb.Obj>() {
                                            @Override
                                            public void handle(PhonesDb.Obj inviterPhoneObj) {
                                                if(inviterPhoneObj == null)
                                                {
                                                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co thong tin nguoi gioi thieu " + inviterPhoneObj.number);
                                                    log.writeLog();
                                                    return;
                                                }
                                                else if(inviterPhoneObj.cardId.equalsIgnoreCase(cardInfo))
                                                {
                                                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thong tin the trung voi thong tin chung minh dinh nhan cua nguoi gioi thieu " + inviterPhoneObj.number);
                                                    log.writeLog();
                                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thong tin the ngan hang nay trung voi thong tin chung minh dinh nhan cua nguoi gioi thieu");
                                                    return;
                                                }
                                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Co tham gia input code " + phoneNumber);
                                                //Kiem tra tiep thong tin map vi voi cac thong tin con lai xem em no co cheat ko.
                                                JsonObject joFilter = new JsonObject();

                                                JsonObject joInviterCardInfo = new JsonObject();
                                                joInviterCardInfo.putString(colName.ReferralV1CodeInputCol.INVITER_CARD_INFO, cardInfo);

                                                JsonObject isMapped = new JsonObject();
                                                isMapped.putBoolean(colName.ReferralV1CodeInputCol.IS_MAPPED, true);

                                                JsonArray jArrAnd = new JsonArray();
                                                jArrAnd.add(isMapped);
                                                jArrAnd.add(joInviterCardInfo);

                                                JsonObject joAnd = new JsonObject();
                                                joAnd.putArray(MongoKeyWords.AND_$, jArrAnd);
                                                JsonObject joInviteeCardInfo = new JsonObject();
                                                joInviteeCardInfo.putString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, cardInfo);

                                                JsonArray joOr = new JsonArray();
                                                joOr.add(joInviteeCardInfo);
                                                joOr.add(joAnd);

                                                if(!"".equalsIgnoreCase(deviceImei))
                                                {
                                                    JsonObject joDeviceImei = new JsonObject();
                                                    joDeviceImei.putString(colName.ReferralV1CodeInputCol.IMEI_CODE, deviceImei);
                                                    joOr.add(joDeviceImei);
                                                }
                                                joFilter.putArray(MongoKeyWords.OR, joOr);
                                                log.add("query check referral"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, joFilter.toString());
                                                referralV1CodeInputDb.searchWithFilter(joFilter, new Handler<ArrayList<ReferralV1CodeInputDb.Obj>>() {
                                                    @Override
                                                    public void handle(ArrayList<ReferralV1CodeInputDb.Obj> listReferralItem) {
                                                        logger.info(listReferralItem.size());
                                                        if(listReferralItem.size() > 0)
                                                        {
                                                            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thong tin CMND/the visa nay da duoc su dung truoc do " + cardInfo + " device " + deviceImei);
                                                            log.add("so luong trung "+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, listReferralItem.size());
                                                            log.writeLog();
                                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thong tin CMND/the visa nay da duoc su dung truoc do " + cardInfo + " device " + deviceImei);
                                                            return;
                                                        }
                                                        JsonObject joUpdate = new JsonObject();
                                                        joUpdate.putString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, cardInfo);
                                                        joUpdate.putString(colName.ReferralV1CodeInputCol.INVITEE_BANK_CODE, bankCode);
                                                        joUpdate.putNumber(colName.ReferralV1CodeInputCol.MAPPING_TIME, System.currentTimeMillis());
                                                        if(!"".equalsIgnoreCase(deviceImei))
                                                        {
                                                            joUpdate.putString(colName.ReferralV1CodeInputCol.IMEI_CODE, deviceImei);
                                                        }

                                                        referralV1CodeInputDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean event) {
                                                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "da ghi nhan truong hop tra thuong referral cho sdt " + phoneNumber);
                                                                log.writeLog();
                                                                Misc.sendPopupReferral(vertx, phoneNumber, PromoContentNotification.NOTI_AUTO_MAPPING_SUCCESS, PromoContentNotification.CHIA_SE_MOMO_TITLE, "https://momo.vn/chiasemomo/huong-dan-nap-tien-va-thanh-toan.html");
                                                                return;

                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });

                                    } else {
                                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Map voi ngan hang ngoai danh sach ngan hang duoc chap nha");
                                        log.writeLog();
                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Map voi ngan hang ngoai danh sach ngan hang duoc chap nhan");
                                        JsonObject joUpdate = new JsonObject().putBoolean(colName.ReferralV1CodeInputCol.LOCK, true);
                                        referralV1CodeInputDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {

                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

//    public void checkAndBonusReferralV1Promotion(final VisaResponse visaResponse, SockData data, final BankResponse bankResponse,final  Common.BuildLog log)
//    {
//        //Kiem tra thong tin referral 1
//        final String phoneNumber = visaResponse != null ? visaResponse.getVisaRequest().getPhoneNumber() : bankResponse.getRequest().getInitiator();
//        final String cardInfo = visaResponse != null ? visaResponse.getCardChecksum() : bankResponse.getBankInfo() != null ? bankResponse.getBankInfo().getCustomerId() : "";
//        final String imei = data == null ? "" : data.imei;
//        final JsonObject joExtra = new JsonObject();
//        final JsonObject joFilter = new JsonObject();
//        JsonArray joOr = new JsonArray();
//        JsonObject joInviteeNumber = new JsonObject();
//        joInviteeNumber.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, phoneNumber);
//        String typeBank = visaResponse != null ? StringConstUtil.PHONES_BANKID_SBS : StringConstUtil.BANK;
//        joExtra.putString(StringConstUtil.TYPE, typeBank);
//        joOr.add(joInviteeNumber);
//        if(data != null)
//        {
//            JsonObject joImei = new JsonObject();
//            joImei.putString(colName.ReferralV1CodeInputCol.IMEI_CODE, data.imei);
//            joOr.add(joImei);
//        }
//
//        joFilter.putArray(MongoKeyWords.OR, joOr);
//        logger.info("query checkandbonus" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + joFilter.toString());
//        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 0, 1, new Handler<ReferralV1CodeInputDb.Obj>() {
//            @Override
//            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
//                if(referralObj == null)
//                {
//                    logger.info("Dang co flow check " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + joFilter.toString());
//                    return;
//                }
//                checkAndExecuteReferral(visaResponse, log, phoneNumber, cardInfo, imei, joExtra, joFilter);
//            }
//        });
//
//    }

    public void checkAndBonusReferralV1PromotionCashIn(SockData data, final PhonesDb.Obj phoneObj ,final  Common.BuildLog log)
    {
        //Kiem tra thong tin referral 1
        final String phoneNumber = "0" + phoneObj.number;
        final String cardInfo = phoneObj.bankPersonalId;
        final String imei = data == null ? "" : data.imei;
        final JsonObject joExtra = new JsonObject();
        final JsonObject joFilter = new JsonObject();
        JsonArray joOr = new JsonArray();
        JsonObject joInviteeNumber = new JsonObject();
        joInviteeNumber.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, phoneNumber);
        String typeBank = StringConstUtil.BANK;
        joExtra.putString(StringConstUtil.TYPE, typeBank);
        joOr.add(joInviteeNumber);
        if(data != null)
        {
            JsonObject joImei = new JsonObject();
            joImei.putString(colName.ReferralV1CodeInputCol.IMEI_CODE, data.imei);
            joOr.add(joImei);
        }

        joFilter.putArray(MongoKeyWords.OR, joOr);
        logger.info("query checkandbonus" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + joFilter.toString());
        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 0, 1, new Handler<ReferralV1CodeInputDb.Obj>() {
            @Override
            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                if(referralObj == null)
                {
                    logger.info("Dang co flow check " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + joFilter.toString());
                    return;
                }
                joExtra.putObject(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ, phoneObj.toJsonObject());
                logger.info("before checkAndExecuteReferral: " + joExtra);
                checkAndExecuteReferral(null, log, phoneNumber, cardInfo, imei, joExtra, joFilter, referralObj);
            }
        });

    }


    private void checkAndExecuteReferral(final VisaResponse visaResponse, final Common.BuildLog log, final String phoneNumber, final String cardInfo, final String imei, final JsonObject joExtra, JsonObject joFilter, final ReferralV1CodeInputDb.Obj referralObj) {
        referralV1CodeInputDb.searchWithFilter(joFilter, new Handler<ArrayList<ReferralV1CodeInputDb.Obj>>() {
            @Override
            public void handle(ArrayList<ReferralV1CodeInputDb.Obj> listReferral) {
                ReferralV1CodeInputDb.Obj referralObjFinal = null;
                logger.info("result searching Referral of phoneNumber " + phoneNumber + " is " + listReferral.size());
                if(listReferral.size() == 0)
                {
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co thong tin tra thuong referral");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                        }
                    });
                    return;
                }
                else if(listReferral.size() > 1)
                {
                    log.add("desc", "Kiem tra xem da co thiet bi duoc tra thuong chua");
                    boolean hasBonus = false;
                    for(ReferralV1CodeInputDb.Obj referralObj : listReferral)
                    {
                        if(referralObj.isMapped)
                        {
                            hasBonus = true;
                            break;
                        }
                        if(referralObj.inviteeNumber.equalsIgnoreCase(phoneNumber))
                        {
                            referralObjFinal = referralObj;
                        }
                    }
                    log.add("hasBonus", hasBonus);
                    if(hasBonus)
                    {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thiet bi da duoc su dung va tra thuong " + phoneNumber);
                        log.writeLog();
                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                            @Override
                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thiet bi nay da duoc su dung roi nhe");
                            }
                        });
                        return;
                    }
                }

                referralObjFinal = listReferral.size() > 1 ? referralObjFinal : listReferral.get(0);
                if(referralObjFinal == null)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co ghi nhan thong tin khach hang do referral = null ");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Khong co ghi nhan thong tin khach hang do referral = null  !!!");
                        }
                    });
                    return;
                }
                else if("".equalsIgnoreCase(referralObjFinal.inviteeCardInfo) || "".equalsIgnoreCase(referralObjFinal.inviterCardInfo)
                        || "".equalsIgnoreCase(referralObjFinal.inviteeNumber) || "".equalsIgnoreCase(referralObjFinal.inviterNumber))
                {
                    logger.info("BEGIN BONUS FOR USER 1");
                    bonusForUserWithOutInviteeCardInfo(log, phoneNumber, cardInfo, joExtra, imei);
                    return;
                }
                else if(!"".equalsIgnoreCase(cardInfo) && referralObjFinal.inviterCardInfo.equalsIgnoreCase(cardInfo))
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thong tin the visa/bank giong voi thong tin ve visa/bank cua inviter.");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thong tin visa/bank trung voi thong tin visa/bank cua thang gioi thieu roi, chia buon nhe !!!");
                        }
                    });

                    return;
                }
                else if(referralObjFinal.bonus_time > 0 || referralObjFinal.invitee_bonus_amount > 0
                        || referralObjFinal.inviter_bonus_amount > 0 || referralObjFinal.invitee_bonus_tid > 0
                        || referralObjFinal.inviter_bonus_tid > 0 || referralObjFinal.isMapped)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Da tra thuong, khong tra thuong nua ");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Da tra thuong roi, khong tra nua nhe @_@ dua doi vai !!!");
                        }
                    });
                    return;
                }
                else {
                    //Tra thuong
//                    if(visaResponse != null && visaResponse.getCardInfos().size() > 0)
//                    {
//                        joExtra.putString(StringConstUtil.ReferralVOnePromoField.BANK_ACC, visaResponse.getCardInfos().get(0).getCardNumber());
//                    }
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra thuong referral V1");
                    JsonObject joFilter = new JsonObject();
                    joFilter.putString(colName.MappingWalletBank.NUMBER, phoneNumber);
                    final ReferralV1CodeInputDb.Obj referralObjFinalTmp = referralObjFinal;
                    mappingWalletBankDb.searchWithFilter(joFilter, new Handler<ArrayList<MappingWalletBankDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<MappingWalletBankDb.Obj> mappingWalletList) {
                            if(mappingWalletList.size() == 0)
                            {
                                logger.info("BEGIN BONUS FOR USER 2");
                                bonusForUserWithOutInviteeCardInfo(log, phoneNumber, cardInfo, joExtra, imei);
                            }
                            else if(mappingWalletList.size() == 1)
                            {
                                logger.info("BEGIN BONUS FOR USER 3");
                                joExtra.putObject(StringConstUtil.ReferralVOnePromoField.REFERRAL_OBJ, referralObjFinalTmp.toJson());
                                joExtra.putNumber(colName.MappingWalletBank.MAPPING_TIME, mappingWalletList.get(0).mapping_time);
                                ReferralPromotionObj.requestReferralPromotion(vertx, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM
                                        , imei, 0, 0, "", joExtra, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject joResponse) {
                                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                                            @Override
                                            public void handle(final ReferralV1CodeInputDb.Obj referralObj) {
                                            }
                                        });
                                    }
                                });
                            }
                            else {
                                log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Map ví nhiều hơn 1 lần nên không trả thưởng ");
                                log.writeLog();
                                referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                                    @Override
                                    public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Map ví nhiều hơn 1 lần nên không trả thưởng !!!");
                                    }
                                });
                                return;
                            }
                        }
                    });

                    return;
                }
            }
        });
    }

    private void bonusForUserWithOutInviteeCardInfo(final Common.BuildLog log, final String phoneNumber, final String cardInfo, final JsonObject joExtra, final String imei) {
        log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "chua ghi nhan thong tin bank ve mongo");
        log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Lay thong tin BANK tu Connector");
        JsonObject joQuery = new JsonObject()
                .putString(colName.WomanNationalCols.PHONE_NUMBER, phoneNumber)
                .putString(colName.WomanNationalCols.CARD_ID, cardInfo)
                .putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.GET_INFO_BANK_USER);
        vertx.eventBus().sendWithTimeout(AppConstant.LStandbyOracleVerticle_ADDRESS, joQuery, 60000L, new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> dataRespond) {
                if (dataRespond != null && dataRespond.result() != null && dataRespond.succeeded()) {
                    JsonObject joResponse = dataRespond.result().body();
                    JsonArray jarrData = joResponse.getArray(StringConstUtil.RESULT, new JsonArray());
                    if (jarrData.size() < 1 || jarrData.size() > 1) {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai || CMND da map vi nhieu lan nen khong tra thuong ");
                        log.writeLog();
                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                            @Override
                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "So dien thoai || CMND da map vi nhieu lan nen khong tra thuong");
                            }
                        });
                        return;
                    }
                    JsonObject joData = jarrData.get(0);
                    final String bankCode = joData.getString(colName.PhoneDBCols.BANK_CODE, "");
                    final String bankName = joData.getString(colName.PhoneDBCols.BANK_NAME, "");
//                                final String phoneNumberTmp = joData.getString(colName.PhoneDBCols.NUMBER, "");
                    final String bankCardId = joData.getString(colName.PhoneDBCols.BANK_PERSONAL_ID, "");
                    final long mappingTime = joData.getLong(colName.MappingWalletBank.MAPPING_TIME, 0);
                    final long unmappingTime = joData.getLong(colName.MappingWalletBank.UNMAPPING_TIME, 0);

                    JsonObject joUpdateReferral = new JsonObject().putString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, bankCardId)
                            .putString(colName.ReferralV1CodeInputCol.INVITEE_BANK_CODE, bankCode)
                            .putNumber(colName.ReferralV1CodeInputCol.MAPPING_TIME, mappingTime);
                    Calendar calendar = Calendar.getInstance();
//                                Date simpleDateFormat = null;
//                                try {
//                                    simpleDateFormat = new SimpleDateFormat("dd-MMM-yy hh.mm.ss.S a").parse(mappingTime);
//                                } catch (ParseException e) {
//                                    e.printStackTrace();
//                                }
                    log.add("unmapping Time ", unmappingTime);
                    log.add("bankCode ", bankCode);
                    log.add("mappingTime ", mappingTime);
                    log.add("bankName ", bankName);
                    log.add("bankCardId ", bankCardId);
                    if(mappingTime == 0 || "".equalsIgnoreCase(bankCode) || "".equalsIgnoreCase(bankName) || "".equalsIgnoreCase(bankCardId))
                    {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thiếu thông tin trả thưởng từ hệ thống core nên kiểm tra và trả bù lại sau " + phoneNumber);
                        log.writeLog();
                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                            @Override
                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thiếu thông tin trả thưởng từ hệ thống core nên kiểm tra và trả bù lại sau " + phoneNumber);
                            }
                        });
                        return;
                    }
                    else if(unmappingTime != 0)
                    {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "ví này unmap nên không trả thưởng " + phoneNumber);
                        log.writeLog();
                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                            @Override
                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "ví này unmap nên không trả thưởng " + unmappingTime);
                            }
                        });
                        return;
                    }
//                                calendar.setTime(simpleDateFormat);
//                                final long mappingTimeUser = calendar.getTimeInMillis();
                    if(mappingTime < System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 7)
                    {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Kiem tra va hoan tra lai voucher neu thoa dieu kien nhan thuong " + phoneNumber);
                        log.writeLog();
                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                            @Override
                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Kiem tra va hoan tra lai voucher neu thoa dieu kien nhan thuong " + phoneNumber + " " + mappingTime);
                            }
                        });
                        return;
                    }
                    referralV1CodeInputDb.findAndUpdateInfoUser(phoneNumber, joUpdateReferral, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(final ReferralV1CodeInputDb.Obj newReferralObj) {
                            JsonObject joFilter = new JsonObject();
                            JsonObject joNumber = new JsonObject().putString(colName.MappingWalletBank.NUMBER, phoneNumber);
                            JsonObject joCardInfo = new JsonObject().putString(colName.MappingWalletBank.CUSTOMER_ID, cardInfo);
                            JsonArray jarrOr = new JsonArray().add(joNumber).add(joCardInfo);
                            joFilter.putArray(MongoKeyWords.OR, jarrOr);
                            logger.info("newReferralObj: -->" + newReferralObj.toJson());
                            mappingWalletBankDb.searchWithFilter(joFilter, new Handler<ArrayList<MappingWalletBankDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<MappingWalletBankDb.Obj> mappingWalletList) {
                                    if(mappingWalletList.size() > 1)
                                    {
                                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Map vi > 1 " + phoneNumber);
                                        log.writeLog();
                                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                                            @Override
                                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Map vi > 1 " + phoneNumber + " " + mappingTime);
                                            }
                                        });
                                        return;
                                    }
                                    else if(mappingWalletList.size() == 1 && mappingWalletList.get(0).mapping_time < (System.currentTimeMillis() - 1000L * 60 * 60))
                                    {
                                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Map vi da lau roi " + phoneNumber);
                                        log.writeLog();
                                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                                            @Override
                                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Map vi da lau roi " + phoneNumber);
                                            }
                                        });
                                        return;
                                    }
                                    else {
                                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra thuong referral V1");
                                        if(newReferralObj.inviteeNumber.trim().equalsIgnoreCase("")) {
                                            logger.info("newReferralObj.inviteeNumber = " + phoneNumber);
                                            newReferralObj.inviteeNumber = phoneNumber;
                                        }
                                        joExtra.putObject(StringConstUtil.ReferralVOnePromoField.REFERRAL_OBJ, newReferralObj.toJson());
                                        joExtra.putNumber(colName.MappingWalletBank.MAPPING_TIME, mappingTime);

                                        ReferralPromotionObj.requestReferralPromotion(vertx, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM
                                                , imei, 0, 0, "", joExtra, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject joResponse) {
                                                referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                                                    @Override
                                                    public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                                        //tra thuong referrals
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    JsonObject joWalletUpdate = new JsonObject();
                    String id = phoneNumber + bankCode + bankCardId;
                    joWalletUpdate.putString(colName.MappingWalletBank.NUMBER, phoneNumber);
                    joWalletUpdate.putString(colName.MappingWalletBank.BANK_NAME, bankName);
                    joWalletUpdate.putString(colName.MappingWalletBank.BANK_CODE, bankCode);
                    joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_NAME, "");
                    joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_ID, bankCardId);
                    joWalletUpdate.putNumber(colName.MappingWalletBank.MAPPING_TIME, mappingTime);
                    mappingWalletBankDb.upsertWalletBank(id, joWalletUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                        }
                    });
                } else {
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co ghi nhan thong tin khach hang ");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Khong co thong tin khach hang day du trong table referral nen khong tra, se tra bu hoac thuc hien lai sau !!!");
                        }
                    });
                    return;
                }
            }
        });
    }

    public void checkAndBonusReferralV1PromotionFromBackend(final String phoneNumber,final  String cardInfo, final String imei, final String bankAcc, final  Common.BuildLog log)
    {
        //Kiem tra thong tin referral 1
        final JsonObject joExtra = new JsonObject();
        final JsonObject joFilter = new JsonObject();
        JsonArray joOr = new JsonArray();
        JsonObject joInviteeNumber = new JsonObject();
        joInviteeNumber.putString(colName.ReferralV1CodeInputCol.INVITEE_NUMBER, phoneNumber);
        joOr.add(joInviteeNumber);
        if(!"".equalsIgnoreCase(imei))
        {
            JsonObject joImei = new JsonObject();
            joImei.putString(colName.ReferralV1CodeInputCol.IMEI_CODE, imei);
            joOr.add(joImei);
        }

        joFilter.putArray(MongoKeyWords.OR, joOr);
        logger.info("query checkandbonus" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + joFilter.toString());
//        checkAndExecuteReferralFromBackend(phoneNumber, cardInfo, imei, bankAcc, log, joExtra, joFilter);
        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 0, 1, new Handler<ReferralV1CodeInputDb.Obj>() {
            @Override
            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                if(referralObj == null)
                {
                    logger.info("Dang co flow check " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM + joFilter.toString());
                    return;
                }
                checkAndExecuteReferralFromBackend(phoneNumber, cardInfo, imei, bankAcc, log, joExtra, joFilter);
            }
        });

    }

    private void checkAndExecuteReferralFromBackend(final String phoneNumber, final String cardInfo, final String imei, final String bankAcc, final Common.BuildLog log, final JsonObject joExtra, JsonObject joFilter) {
        referralV1CodeInputDb.searchWithFilter(joFilter, new Handler<ArrayList<ReferralV1CodeInputDb.Obj>>() {
            @Override
            public void handle(ArrayList<ReferralV1CodeInputDb.Obj> listReferral) {
                ReferralV1CodeInputDb.Obj referralObjFinal = null;
                if(listReferral.size() == 0)
                {
                    log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co thong tin tra thuong referral");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {

                        }
                    });
                    return;
                }
                else if(listReferral.size() > 1)
                {
                    log.add("desc", "Kiem tra xem da co thiet bi duoc tra thuong chua");
                    boolean hasBonus = false;
                    for(ReferralV1CodeInputDb.Obj referralObj : listReferral)
                    {
                        if(referralObj.isMapped)
                        {
                           hasBonus = true;
                           break;
                        }
                        if(referralObj.inviteeNumber.equalsIgnoreCase(phoneNumber))
                        {
                            referralObjFinal = referralObj;
                        }
                    }
                    log.add("hasBonus", hasBonus);
                    if(hasBonus)
                    {
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thiet bi da duoc su dung va tra thuong " + phoneNumber);
                        log.writeLog();
                        referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                            @Override
                            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thiet bi nay da duoc su dung roi nhe");
                            }
                        });
                        return;
                    }
                }

                referralObjFinal = listReferral.size() > 1 ? referralObjFinal : listReferral.get(0);
                if(referralObjFinal == null)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co ghi nhan thong tin khach hang ");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Khong co ghi nhan thong tin khach hang");
                        }
                    });
                    return;
                }
                if("".equalsIgnoreCase(referralObjFinal.inviteeCardInfo) || "".equalsIgnoreCase(referralObjFinal.inviterCardInfo)
                        || "".equalsIgnoreCase(referralObjFinal.inviteeNumber) || "".equalsIgnoreCase(referralObjFinal.inviterNumber))
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Khong co ghi nhan thong tin khach hang ");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thong tin khach hang khong duoc ghi nhan trong table, se check lai sau");
                        }
                    });
                    return;
                }
                else if(!"".equalsIgnoreCase(cardInfo) && referralObjFinal.inviterCardInfo.equalsIgnoreCase(cardInfo))
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Thong tin the visa/bank giong voi thong tin ve visa/bank cua inviter.");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Thong tin the visa/bank giong voi thong tin ve visa/bank cua thang gioi thieu.");
                        }
                    });
                    return;
                }
                else if(referralObjFinal.bonus_time > 0 || referralObjFinal.invitee_bonus_amount > 0
                        || referralObjFinal.inviter_bonus_amount > 0 || referralObjFinal.invitee_bonus_tid > 0
                        || referralObjFinal.inviter_bonus_tid > 0)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Da tra thuong, khong tra thuong nua ");
                    log.writeLog();
                    referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Da tra thuong roi, dua doi vai");
                        }
                    });

                    return;
                }
                else {
                    //Tra thuong
                    if(!"".equalsIgnoreCase(bankAcc))
                    {
                        joExtra.putString(StringConstUtil.ReferralVOnePromoField.BANK_ACC, bankAcc);
                    }
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tra thuong referral V1");
                    joExtra.putObject(StringConstUtil.ReferralVOnePromoField.REFERRAL_OBJ, referralObjFinal.toJson());
                    ReferralPromotionObj.requestReferralPromotion(vertx, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM
                            , imei, 0, 0, "", joExtra, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joResponse) {
                            referralV1CodeInputDb.findAndModifyInviteeNumber(phoneNumber, 1, -1, new Handler<ReferralV1CodeInputDb.Obj>() {
                                @Override
                                public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                }
                            });
                        }
                    });
                    return;
                }
            }
        });
    }

    public void executeCashBackReferralV1Promotion(final SockData sockData, final String phoneNumber,final long tranId,
                                                   final long amount, final String serviceId,final Common.BuildLog log)
    {
        log.add("method"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,"executeCashBackReferralV1Promotion");
        log.add("sdt invitee referral"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneNumber);
        final String imei = sockData == null ? "" : sockData.imei;
        final JsonObject joExtra = new JsonObject();
        //Kiem tra thong tin the cua user
        referralV1CodeInputDb.findOne(phoneNumber, new Handler<ReferralV1CodeInputDb.Obj>() {
            @Override
            public void handle(final ReferralV1CodeInputDb.Obj referralObj) {
                if(referralObj == null)
                {
                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai " + phoneNumber + " khong co tham gia referral");
                    log.writeLog();
                    return;
                }
                phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(final PhonesDb.Obj phoneObj) {
                        if(phoneObj == null)
                        {
                            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai " + phoneNumber + " khong co thong tin");
                            log.writeLog();
                            return;
                        }
                        else if("0".equalsIgnoreCase(phoneObj.bank_code) || "".equalsIgnoreCase(phoneObj.bank_code))
                        {
                            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "So dien thoai " + phoneNumber + " khong co map vi thoi diem hien tai => kiem tra visa");

                            card.findAll(DataUtil.strToInt(phoneNumber), new Handler<ArrayList<Card.Obj>>() {
                                @Override
                                public void handle(ArrayList<Card.Obj> listCards) {
                                    boolean isMappingVisa = false;
                                    Card.Obj currentCard = null;
                                    for(Card.Obj card : listCards)
                                    {
                                        if(!card.deleted && StringConstUtil.PHONES_BANKID_SBS.equalsIgnoreCase(card.bankid))
                                        {
                                            log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Mapping VISA CARD");
                                            isMappingVisa = true;
                                            currentCard = card;
                                            break;
                                        }
                                    }
                                    log.add("isMAPPINGVISA"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, isMappingVisa);
                                    if(!isMappingVisa || currentCard == null)
                                    {
                                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "is not MAPPING VISA => khong tra thuong");
                                        log.writeLog();
                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, 1000, "Khong map ngan hang, cung khong map visa, vay ma doi cashback referral la sao");
                                        return;
                                    }
                                    log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "requestCashBackReferralPromotion");
                                    joExtra.putString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, currentCard.cardCheckSum);
                                    joExtra.putString(colName.ReferralV1CodeInputCol.INVITEE_BANK_CODE, "sbs");
                                    requestCashBackReferralPromotion(joExtra, referralObj, phoneObj, phoneNumber, imei, amount, tranId, serviceId);
                                }
                            });
                            return;
                        }
                        log.add("desc"+ StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "requestCashBackReferralPromotion");
                        joExtra.putString(colName.ReferralV1CodeInputCol.INVITEE_CARD_INFO, phoneObj.bankPersonalId);
                        joExtra.putString(colName.ReferralV1CodeInputCol.INVITEE_BANK_CODE, phoneObj.bank_code);
                        requestCashBackReferralPromotion(joExtra, referralObj, phoneObj, phoneNumber, imei, amount, tranId, serviceId);
                        return;
                    }
                });
            }
        });
        return;
    }

    private void requestCashBackReferralPromotion(JsonObject joExtra, ReferralV1CodeInputDb.Obj referralObj, PhonesDb.Obj phoneObj, String phoneNumber, String imei, long amount, long tranId, String serviceId) {
        joExtra.putObject(StringConstUtil.ReferralVOnePromoField.REFERRAL_OBJ, referralObj.toJson());
        joExtra.putObject(StringConstUtil.ReferralVOnePromoField.INVITEE_PHONE_OBJ, phoneObj.toJsonObject());
        ReferralPromotionObj.requestReferralPromotion(vertx, phoneNumber, StringConstUtil.ReferralVOnePromoField.REFERRAL_CASHBACK_PROGRAM,
                imei, amount, tranId, serviceId, joExtra, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject joResponseReferral) {

                    }
                }
        );
    }

    private JsonObject createJsonNotification(String phone, String content, String title, String url)
    {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.RedirectNoti.CAPTION, title);
        jo.putString(StringConstUtil.RedirectNoti.BODY, content);
        jo.putString(StringConstUtil.RedirectNoti.RECEIVER_NUMBER, phone);
        jo.putNumber(StringConstUtil.RedirectNoti.TRAN_ID, System.currentTimeMillis());
        jo.putString(StringConstUtil.RedirectNoti.URL, url);
        logger.info("Noti content: " + jo.toString());
        return jo;
    }

    private void checkBankIsAccepted(String bankId, PromotionDb.Obj prompromotionObj, Handler<Boolean> callback) {
        String strListBank = prompromotionObj.ADJUST_PIN;
        String[] listBank = strListBank.split(";");
        //accept all of bank
        if(listBank.length == 0) {
            callback.handle(true);
            return;
        }

        for (String obj:listBank) {
            if(obj.equalsIgnoreCase(bankId)) {
                callback.handle(true);
                return;
            }
        }
        callback.handle(false);
    }
}
