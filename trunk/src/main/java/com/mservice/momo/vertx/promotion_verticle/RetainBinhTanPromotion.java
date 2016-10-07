package com.mservice.momo.vertx.promotion_verticle;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.RetainBinhTanPromo.RetainBinhTanDb;
import com.mservice.momo.data.RetainBinhTanPromo.RetainBinhTanVoucherDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.PhoneCheckDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.promotion_server.MainPromotionVerticle;
import com.mservice.momo.vertx.promotion_server.PromotionObj;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by congnguyenit on 9/26/16.
 */
public class RetainBinhTanPromotion extends MainPromotionVerticle implements Handler<Message<JsonObject>> {
    RetainBinhTanDb retainBinhTanDb;
    RetainBinhTanVoucherDb retainBinhTanVoucherDb;
    PhoneCheckDb phoneCheckDb;

    private void init() {
        retainBinhTanDb = new RetainBinhTanDb(vertx, logger);
        retainBinhTanVoucherDb = new RetainBinhTanVoucherDb(vertx, logger);
        phoneCheckDb = new PhoneCheckDb(vertx, logger);
    }
    @Override
    public void start() {
        super.start();
        init();
        vertx.eventBus().registerHandler(AppConstant.BINHTAN_RETAIN_PROMOTION_BUS_ADDRESS, this);
    }


    @Override
    public void handle(Message<JsonObject> message) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        JsonObject joReceive = message.body();
        final PromotionObj promotionObj = new PromotionObj(joReceive);
        log.setPhoneNumber(promotionObj.phoneNumber);

        switch (promotionObj.tranType) {
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                givoucher2nd(message, log, promotionObj);
                break;
            case MomoProto.MsgType.CAN_USE_GIFT_VALUE:
                rejectCanUseGift(message, log, promotionObj);
                break;
            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                activeGift2nd(message, log, promotionObj, true);
                break;
            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
            case MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE:
            case MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE:
                activeGift2nd(message, log, promotionObj, false);
                break;
            default:
                callBack(2101, "Khong duoc tham gia retain binh tan do sai trans type", null, log, message, this.getClass().getName(), new JsonObject());
                break;
        }
    }

    private void rejectCanUseGift(final Message<JsonObject> message, final Common.BuildLog log, final PromotionObj promotionObj) {
        final String phone_number = promotionObj.phoneNumber;
        final PhonesDb.Obj phoneObj = new PhonesDb.Obj(promotionObj.joPhone);
        final JsonObject joEx = promotionObj.joExtra;
        final String giftId = joEx.getString(StringConstUtil.PromotionField.GIFT_ID, "");
        if (promotionObj.serviceId.equalsIgnoreCase("[\"topup\"]")) {
            JsonObject filter = new JsonObject();
            filter.putString(colName.RetainBinhTanVoucherDbCols.NUMBER, phone_number);
            JsonObject filter1 = new JsonObject();
            filter1.putNumber(colName.RetainBinhTanVoucherDbCols.TIME_OF_VOUCHER, 2);
            JsonObject filter2 = new JsonObject();
            filter2.putNumber(colName.RetainBinhTanVoucherDbCols.TIME_CASHIN, 0);
            JsonArray jarrAnd = new JsonArray().add(filter).add(filter1).add(filter2);
            JsonObject jAndFilter = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd);
            retainBinhTanVoucherDb.searchWithFilter(jAndFilter, new Handler<ArrayList<RetainBinhTanVoucherDb.Obj>>() {
                @Override
                public void handle(ArrayList<RetainBinhTanVoucherDb.Obj> arrObj) {
                    if(arrObj.size() == 0) {
                        addLog("RetainBinhTanPromotion", "Duoc active qua " + giftId, this.getClass().getSimpleName(), log);
                        callBack(0, "Duoc active qua", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                        return;
                    }
                    JsonObject jsonExtra = new JsonObject();
                    jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, "Xác nhận");
                    jsonExtra.putNumber(StringConstUtil.TYPE, 3);
                    Notification notification = new Notification();
                    notification.priority = 2;
                    notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
                    notification.caption = "Thông tin thẻ quà tặng";
                    notification.body = "Bạn hãy nạp tối thiểu 20.000đ vào Ví để sử dụng voucher này";
                    notification.cmdId = 0L;
                    notification.time = System.currentTimeMillis();
                    notification.receiverNumber = DataUtil.strToInt(promotionObj.phoneNumber);
                    notification.extra = jsonExtra.toString();
                    callBack(1000, "khong duoc active qua", notification, log, message, this.getClass().getSimpleName(), new JsonObject().putString(StringConstUtil.PromotionField.GIFT_ID, giftId).putString(StringConstUtil.PromotionField.SERVICE_ID, promotionObj.serviceId).putNumber(StringConstUtil.PromotionField.ERROR, 1000));
                }
            });
        } else {
            addLog("RetainBinhTanPromotion", "Duoc active qua " + giftId, this.getClass().getName(), log);
            callBack(0, "Duoc active qua", null, log, message, this.getClass().getName(), new JsonObject());
            return;
        }

    }

    private void activeGift2nd(final Message<JsonObject> message, final Common.BuildLog log, final PromotionObj promotionObj, boolean isM2M) {
        final String phoneNumber =  isM2M ? promotionObj.customerNumber : promotionObj.phoneNumber;
            getPromotion(StringConstUtil.RETAIN_BINHTAN_PROMO.NAME, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject joPromo) {
                    if(joPromo.getInteger(StringConstUtil.ERROR, -1) != 0) {
                        addLog("RetainBinhTanPromotion", joPromo.getString(StringConstUtil.DESCRIPTION,""), this.getClass().getSimpleName(), log);
                        callBack(1000, joPromo.getString(StringConstUtil.DESCRIPTION,""), null, log, message, this.getClass().getName(), new JsonObject());
                        return;
                    }
                    PromotionDb.Obj promotionConfObj = new PromotionDb.Obj(joPromo.getObject(StringConstUtil.PROMOTION, new JsonObject()));

                    if(promotionObj.amount < promotionConfObj.TRAN_MIN_VALUE) {
                        addLog("RetainBinhTanPromotion", "Chua dat han muc duoc huong khuyen mai", this.getClass().getSimpleName(), log);
                        callBack(1000, "Chua dat han muc duoc huong khuyen mai", null, log, message, this.getClass().getName(), new JsonObject());
                        return;
                    }

                    JsonObject filter = new JsonObject();
                    filter.putString(colName.RetainBinhTanVoucherDbCols.NUMBER, phoneNumber);
                    JsonObject joAnd = new JsonObject();
                    JsonArray jarrAnd = new JsonArray();
                    jarrAnd.add(new JsonObject().putString(colName.RetainBinhTanVoucherDbCols.NUMBER, phoneNumber)).add(new JsonObject().putNumber(colName.RetainBinhTanVoucherDbCols.TIME_CASHIN, 0)).add(new JsonObject().putNumber(colName.RetainBinhTanVoucherDbCols.TIME_OF_VOUCHER, 2));
                    joAnd.putArray(MongoKeyWords.AND_$, jarrAnd);

                    retainBinhTanVoucherDb.searchWithFilter(joAnd, new Handler<ArrayList<RetainBinhTanVoucherDb.Obj>>() {
                        @Override
                        public void handle(final ArrayList<RetainBinhTanVoucherDb.Obj> arrRes) {
                            if(arrRes == null || arrRes.size() == 0) {
                                addLog("RetainBinhTanPromotion", "Khong co qua khuyen mai de active", this.getClass().getSimpleName(), log);
                                callBack(1000, "Khong co qua khuyen mai de active", null, log, message, this.getClass().getName(), new JsonObject());
                                return;
                            }
                            final AtomicInteger size = new AtomicInteger(arrRes.size());

                            vertx.setPeriodic(200L, new Handler<Long>() {
                                @Override
                                public void handle(Long time) {
                                    final int position = size.decrementAndGet();
                                    if(position < 0) {
                                        retainBinhTanVoucherDb.updatePartial(phoneNumber, new JsonObject().putNumber(colName.RetainBinhTanVoucherDbCols.TIME_CASHIN, System.currentTimeMillis()), new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {
                                                addLog("RetainBinhTanPromotion", "Active qua thanh cong", this.getClass().getName(), log);
                                                callBack(0, "Active qua thanh cong", null, log, message, this.getClass().getName(), new JsonObject());
                                            }
                                        });
                                        vertx.cancelTimer(time);
                                        return;
                                    }
                                    RetainBinhTanVoucherDb.Obj reObj = arrRes.get(position);
                                    activeGift(phoneNumber, reObj.GIFT_ID, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject event) {

                                        }
                                    });
                                }
                            });

                        }
                    });
                }
            });

    }

    private void givoucher2nd(final Message<JsonObject> message, final Common.BuildLog log, final PromotionObj promotionObj) {
        final String phone_number = promotionObj.phoneNumber;
        final PhonesDb.Obj phoneObj = new PhonesDb.Obj(promotionObj.joPhone);
        long amount = promotionObj.amount;
        long tranId = promotionObj.tranId;
        retainBinhTanDb.findByPhone(phone_number, new Handler<RetainBinhTanDb.Obj>() {
            @Override
            public void handle(RetainBinhTanDb.Obj obj) {
                if(obj == null) {
                    callBack(1000, "Khong thuoc doi tuong huong khuyen mai", null, log, message, this.getClass().getName(), new JsonObject());
                    return;
                }
                JsonObject filter = new JsonObject();
                filter.putString(colName.RetainBinhTanVoucherDbCols.NUMBER, phone_number);
                retainBinhTanVoucherDb.searchWithFilter(filter, new Handler<ArrayList<RetainBinhTanVoucherDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<RetainBinhTanVoucherDb.Obj> objArrayList) {
                        if(objArrayList.size() == 0 || objArrayList.size() > 1) {
                            callBack(1000, "Doi tuong khong duoc huong khuyen mai lan 2", null, log, message, this.getClass().getName(), new JsonObject());
                            return;
                        }

                        getPromotion(StringConstUtil.RETAIN_BINHTAN_PROMO.NAME, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject joRes) {

                                logger.info("------->" + joRes.toString());
                                final JsonObject joPromo = joRes.getObject(StringConstUtil.PROMOTION, new JsonObject());
                                final PromotionDb.Obj promoObj = new PromotionDb.Obj(joPromo);
                                long current = System.currentTimeMillis();
                                long end_time = promoObj.DATE_TO + 7 * 24 * 60 * 60 * 1000;
                                if(current < promoObj.DATE_FROM || current > end_time) {
                                    callBack(1000, "Da het thoi gian tham gia chuong trinh ", null, log, message, this.getClass().getName(), new JsonObject());
                                    saveErrorPromotionDescription(StringConstUtil.RETAIN_BINHTAN_PROMO.NAME, phoneObj.number, "update db fail, kiem tra ket noi", 1000, phoneObj.deviceInfo, log, this.getClass().getName());
                                    return;
                                }
                                getListVoucher(promoObj.INTRO_SMS, new Handler<List>() {
                                    @Override
                                    public void handle(final List listVoucher) {
                                        if(listVoucher ==null || listVoucher.size() <= 0) {
                                            log.add("Cau hinh sai " + StringConstUtil.RETAIN_BINHTAN_PROMO.NAME, "Kiểm tra cấu hình quà");
                                            callBack(1000, "Chua cau hinh tra qua", null, log, message, this.getClass().getName(), new JsonObject());
                                            return;
                                        }
                                        JsonObject joUpdate = new JsonObject();
                                        joUpdate.putNumber(colName.RetainBinhTanDbCols.UPDATE_TIME, System.currentTimeMillis());
                                        retainBinhTanDb.updatePartial(phone_number, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean res) {
                                                if(!res) {
                                                    callBack(1000, "update db fail, kiem tra ket noi", null, log, message, this.getClass().getName(), new JsonObject());
                                                    saveErrorPromotionDescription(StringConstUtil.RETAIN_BINHTAN_PROMO.NAME, phoneObj.number, "update db fail, kiem tra ket noi", 1000, phoneObj.deviceInfo, log, this.getClass().getName());
                                                    return;
                                                }
                                                String[] voucherInfo = getVoucherInfo(listVoucher, 2).split(":");
                                                final long totalAmount = DataUtil.stringToUNumber(voucherInfo[1]);
                                                String voucherName = voucherInfo[0];
                                                List<String> lstVoucher = new ArrayList<String>();
                                                lstVoucher.add(voucherName);

                                                giveVoucher(totalAmount, promoObj.DURATION, promoObj.ADJUST_ACCOUNT, phone_number, promoObj.NAME, lstVoucher, log, this.getClass().getName(), false, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(final JsonObject joRes) {
                                                        int err = joRes.getInteger(StringConstUtil.ERROR, -1);
                                                        if(err != 0) {
                                                            log.add("tra qua fail " + StringConstUtil.GRAB_PROMO.NAME, "Tra qua that bai, kiem tra lai du lieu, tra bu cho so: " + phone_number);
                                                            callBack(1000, "Tra qua that bai, kiem tra lai du lieu", null, log, message, this.getClass().getName(), new JsonObject());
                                                            saveErrorPromotionDescription(StringConstUtil.RETAIN_BINHTAN_PROMO.NAME, phoneObj.number, "Tra qa that bai, kiem tra lai du lieu", 1000, phoneObj.deviceInfo, log, this.getClass().getName());
                                                            return;
                                                        }

                                                        RetainBinhTanVoucherDb.Obj vcObj = new RetainBinhTanVoucherDb.Obj();
                                                        vcObj.GIFT_ID = joRes.getString(StringConstUtil.PromotionField.GIFT_ID);
                                                        vcObj.NUMBER = phone_number;
                                                        vcObj.VOUCHER_TIME = System.currentTimeMillis();
                                                        vcObj.VOUCHER_AMOUNT = totalAmount;
                                                        vcObj.TIME_OF_VOUCHER = 2;
                                                        retainBinhTanVoucherDb.insert(vcObj, new Handler<Integer>() {
                                                            @Override
                                                            public void handle(Integer vcres) {
                                                                if(vcres != 0) {
                                                                    callBack(1000, "insert bang voucher that bai", null, log, message, this.getClass().getName(), new JsonObject());
                                                                    saveErrorPromotionDescription(StringConstUtil.RETAIN_BINHTAN_PROMO.NAME, phoneObj.number, "insert bang voucher that bai", 1000, phoneObj.deviceInfo, log, this.getClass().getName());
                                                                    return;
                                                                }

                                                                Gift gift = new Gift(joRes);
                                                                final long giftTranId = joRes.getLong(StringConstUtil.PromotionField.GIFT_TID, System.currentTimeMillis());
                                                                String time = Misc.dateVNFormat(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000);
                                                                saveSuccessPromotionTransaction(promoObj.NOTI_COMMENT.replace("%time%",time ), giftTranId, totalAmount, promotionObj.phoneNumber, promoObj.NOTI_CAPTION, "", "momo", promoObj.NOTI_COMMENT, "", new JsonObject());
                                                                Notification notification = buildPopupGiftNotification(promoObj.NOTI_CAPTION, promoObj.NOTI_COMMENT.replace("%time%",time ), 3, 16, giftTranId, "retain_binhtan", gift, gift.amount, promotionObj.phoneNumber, promoObj.NOTI_CAPTION);
                                                                callBack(0, joRes.getString(StringConstUtil.DESCRIPTION,""), notification, log, message, this.getClass().getName(), new JsonObject());
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void getListVoucher(String voucherInfo, Handler<List> callback)
    {
        String[] listGift = voucherInfo.split(";");
        List<String> listVoucher = new ArrayList<>();
        String []gift = {};
        for(String giftInfo : listGift)
        {
            gift = giftInfo.trim().split(":");
            if(gift.length == 2)
            {
                listVoucher.add(giftInfo.trim());
            }
        }

        callback.handle(listVoucher);

    }

    private String getVoucherInfo(List<String> listVc, int group) {
        if(listVc.size() == 0) {
            return null;
        }

        int position = listVc.size() >= group? group - 1 : 0;

        return listVc.size() > 0 ? listVc.get(position).toString().trim() : "";
    }

}
