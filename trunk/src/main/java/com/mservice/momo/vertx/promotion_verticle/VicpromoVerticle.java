package com.mservice.momo.vertx.promotion_verticle;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.victaxi.VicPromoDb;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
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
import org.vertx.java.core.logging.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by manhly on 27/09/2016.
 */
public class VicpromoVerticle extends MainPromotionVerticle {
    Logger logger;
    JsonObject glbCfg;
    private boolean remindNoti;
    private VicPromoDb vicPromoDb;
    private boolean isUAT;

    public void start() {
        super.start();
        logger = container.logger();
        glbCfg = container.config();
        vicPromoDb = new VicPromoDb(vertx, logger);
        remindNoti = glbCfg.getBoolean(StringConstUtil.SEND_REMIND_PROMO, true);
        isUAT = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
        if (remindNoti) {
            remind15DaysNotCashin1();
            give2ndVoucherUsedGift();

        }
        vertx.eventBus().registerHandler(AppConstant.VIC_PROMOTION_BUS_ADDRESS, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final Common.BuildLog log = new Common.BuildLog(logger);
                JsonObject joReceive = message.body();
                final PromotionObj promotionObj = new PromotionObj(joReceive);
                log.setPhoneNumber(promotionObj.phoneNumber);

                final PhonesDb.Obj userPhoneObj = new PhonesDb.Obj(promotionObj.joPhone);
                final String phoneNumber = (promotionObj.tranType == MomoProto.TranHisV1.TranType.M2M_VALUE) ? promotionObj.customerNumber : promotionObj.phoneNumber;
                vicPromoDb.findByPhone(phoneNumber, new Handler<VicPromoDb.Obj>() {
                    @Override
                    public void handle(final VicPromoDb.Obj obj) {
                        if (obj == null) {
                            //// TODO: 25/08/2016  callback
                            //// TODO: 28/09/2016 execute show popup for register
                            if (promotionObj.joExtra.getString("code", "").trim().equals("VIC") || promotionObj.joExtra.getString("code", "").trim().equals("Vic") || promotionObj.joExtra.getString("code", "").trim().equals("vic")) {
                                //// TODO: 29/09/2016 callback n
                                JsonObject jsonExtra = new JsonObject();
                                jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, true);
                                jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, "Xác Nhận");
                                jsonExtra.putNumber(StringConstUtil.TYPE, 3);
                                Notification notification = new Notification();
                                notification.priority = 2;
                                notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
                                notification.caption = "Vui lòng đăng ký để nhận khuyến mãi";
                                notification.body = "Chương trình khuyến mãi chỉ dành cho khách hàng của VIC Taxi. Vui lòng gọi đến tổng đài đăng ký và xác nhận thông tin để nhận tới 03 chuyến xe miễn phí 50.000đ. Liên hệ: 043 8230 230";
                                notification.cmdId = 0L;
                                notification.time = System.currentTimeMillis();
                                notification.receiverNumber = DataUtil.strToInt(promotionObj.phoneNumber);
                                notification.extra = jsonExtra.toString();
                                callBack(5103, "Khong nam trong danh sach khuyen mai", notification, log, message, this.getClass().getSimpleName(), new JsonObject());
                            }
                            addLog("VicPromoVerticle", "Khong nam trong danh sach khuyen mai", this.getClass().getSimpleName(), log);
                            callBack(5100, "Khong nam trong danh sach khuyen mai", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                            return;
                        }
                        getPromotionProgram(message, log, promotionObj, userPhoneObj, new Handler<PromotionDb.Obj>() {
                            @Override
                            public void handle(final PromotionDb.Obj programObj) {
                                if (!(programObj == null)) {
                                    if (promotionObj.tranType == MomoProto.MsgType.CAN_USE_GIFT_VALUE) {
                                        if (promotionObj.serviceId.equalsIgnoreCase("[\"taxi\"]")) {
                                            final String giftId = promotionObj.joExtra.getString("gift_id");//
                                            Notification notification = new Notification();
                                            notification.priority = 2;
                                            notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
                                            notification.caption = "Thông tin thẻ quà tặng";
                                            notification.body = "Thẻ quà tặng chưa kích hoạt. Bạn hãy nạp tối thiểu 10.000đ vào ví MoMo để kích hoạt thẻ quà tặng: giảm ngay 50.000đ khi đi VIC Taxi và thanh toán bằng MoMo.";
                                            notification.cmdId = 0L;
                                            notification.time = System.currentTimeMillis();
                                            notification.receiverNumber = DataUtil.strToInt(promotionObj.phoneNumber);
                                            notification.extra = new JsonObject().putString(StringConstUtil.RedirectNoti.URL, "https://momo.vn/thanhtoanvic")
                                                    .putBoolean(StringConstUtil.BUTTON_TITLE_X, true)
                                                    .putString(StringConstUtil.BUTTON_TITLE_1, "Xem Chi Tiết")
                                                    .putNumber(StringConstUtil.TYPE, 3).toString();
                                            callBack(10000, "0 Dc Active", notification, log, message, this.getClass().getSimpleName(), new JsonObject().putString(StringConstUtil.PromotionField.GIFT_ID, giftId).putString(StringConstUtil.PromotionField.SERVICE_ID, promotionObj.serviceId).putNumber(StringConstUtil.PromotionField.ERROR, 1000));
                                        } else {
                                            addLog("VicPromoVerticle", "Duoc active qua", this.getClass().getSimpleName(), log);
                                            callBack(0, "Duoc active qua", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                        }
                                    } else if (promotionObj.tranType == MomoProto.MsgType.LOGIN_VALUE) {
                                        JsonObject joUpdate = new JsonObject();
                                        boolean isName = userPhoneObj.isNamed;
                                        joUpdate.putBoolean(colName.VicPromoCol.IS_NAMED, isName);
                                        vicPromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {
                                                if (event) {
                                                    callBack(5132, "update thanh cong dinh danh", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                } else {
                                                    callBack(5132, "update dinh danh fail", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                }

                                            }
                                        });

                                    } else if (promotionObj.joExtra.getString("code", "").trim().equals("VIC") || promotionObj.joExtra.getString("code", "").trim().equals("Vic") || promotionObj.joExtra.getString("code", "").trim().equals("vic")) {

                                        if (obj.bonusTime1 == 0) {
                                            checkDevice(userPhoneObj.deviceInfo, userPhoneObj.phoneOs, userPhoneObj.appCode, "vic_promo", DataUtil.strToInt(phoneNumber), userPhoneObj.lastImei, false, false, this.getClass().getSimpleName(), log, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject joResult) {
                                                    int err = joResult.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                                                    String desc = joResult.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                                                    if (err != 0) {
                                                        addLog("info", desc, this.getClass().getSimpleName(), log);
                                                        callBack(5101, "check divice false", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                        saveErrorPromotionDescription("vic_promo", DataUtil.strToInt(promotionObj.phoneNumber), joResult.getString(StringConstUtil.PromotionField.DESCRIPTION, "Loi check device!"), 5101, "", log, this.getClass().getName());
                                                        return;
                                                    }
                                                    List<String> listVoucher = new ArrayList<String>() {{
                                                        add("vic_gift");
                                                    }};

                                                    giveVoucher(programObj.PER_TRAN_VALUE, programObj.DURATION, programObj.ADJUST_ACCOUNT, promotionObj.phoneNumber, programObj.NAME, listVoucher, log, this.getClass().getSimpleName(), false, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(final JsonObject joResponse) {
                                                            int error = joResponse.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                                                            String desc = joResponse.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                                                            final long giftTranId = joResponse.getLong(StringConstUtil.PromotionField.GIFT_TID, System.currentTimeMillis());
                                                            //todo if success
                                                            JsonObject joUpdate = new JsonObject();
                                                            joUpdate.putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER1, true);
                                                            joUpdate.putString(colName.VicPromoCol.PROMOTION_CODE, promotionObj.joExtra.getString("code"));
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_AMOUNT1, programObj.PER_TRAN_VALUE);
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_TIME1, System.currentTimeMillis());
                                                            joUpdate.putNumber(colName.VicPromoCol.ERROR1, error);
                                                            vicPromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                @Override
                                                                public void handle(Boolean event) {
                                                                    Gift gift = new Gift(joResponse.getObject(StringConstUtil.PromotionField.GIFT, new JsonObject()));
                                                                    Long temp = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30);
                                                                    String expireVoucherDate = Misc.dateVNFormat(temp);
                                                                    String notibody = StringConstUtil.vicPromoNoti.BODYGIFT1.replace("%s", expireVoucherDate);
                                                                    Notification notification = buildPopupGiftNotification(StringConstUtil.vicPromoNoti.CAPTIONNOTIGIFT1, notibody, 3, 28, giftTranId, "vic_gift", gift, programObj.PER_TRAN_VALUE, promotionObj.phoneNumber, StringConstUtil.vicPromoNoti.CAPTIONNOTIGIFT1);
                                                                    saveSuccessPromotionTransaction(notibody, giftTranId, programObj.PER_TRAN_VALUE, promotionObj.phoneNumber, "Thẻ quà tặng VIC", "", "momo", notibody, "", new JsonObject());//
                                                                    callBack(0, "success", notification, log, message, this.getClass().getSimpleName(), new JsonObject());

                                                                }
                                                            });
                                                        }
                                                    });


                                                }
                                            });


                                        } else {
                                            addLog("info", "khach hang da nhap claimcode va da nhan qua", this.getClass().getSimpleName(), log);
                                            callBack(5102, "khach hang da nhap claimcode va da nhan qua", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                        }


                                    } else if (promotionObj.tranType == MomoProto.TranHisV1.TranType.BANK_IN_VALUE
                                            || promotionObj.tranType == MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE
                                            || (promotionObj.tranType == MomoProto.TranHisV1.TranType.M2M_VALUE && promotionObj.isStoreApp
                                            || promotionObj.tranType == MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE)) {
                                        doPromotionVic();
                                    } else if (promotionObj.tranType == MomoProto.TranHisV1.TranType.M2M_VALUE) {
                                        final JsonObject json = new JsonObject();
                                        json.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.GET_GROUP_AGENT);
                                        json.putString("mmphone", promotionObj.phoneNumber);
                                        vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS, json, new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> message) {
                                                final List<String> coreGroupIds = Arrays.asList(message.body().getString("result", "").split(","));
                                                boolean isTaxiVic = false;
                                                for (String group : coreGroupIds) {
                                                    if (group.contains("66122")) {
                                                        isTaxiVic = true;
                                                        break;
                                                    }
                                                }
                                                if (isTaxiVic) {
                                                    doPromotionVic();
                                                } else {
                                                    callBack(5138, "User chuyen tien khong thuoc nhom taxi vic ", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                }
                                            }
                                        });
                                    } else if (promotionObj.tranType == MomoProto.TranHisV1.TranType.PAY_ONE_SALE_OFF_BILL_VALUE) {
                                        if (obj.useGift1 == false && obj.isCashin1 == true && obj.useGift2 == false && obj.useGift3 == false) {
                                            JsonObject joUpdate = new JsonObject();
                                            joUpdate.putBoolean(colName.VicPromoCol.USED_GIFT1, true);
                                            joUpdate.putNumber(colName.VicPromoCol.GIFT_TIME1, System.currentTimeMillis());
                                            vicPromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean event) {
                                                    if (event) {
                                                        callBack(5133, "update thanh cong su dung qua 1", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    } else {
                                                        callBack(5133, "update fail su dung qua 1", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    }

                                                }
                                            });
                                            return;
                                        } else {
                                            callBack(5137, "Khach hang da su dung qua 1", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                        }
                                        if (obj.useGift1 == true && obj.useGift2 == false && obj.isCashin2 == true && obj.isCashin3 == false) {
                                            JsonObject joUpdate = new JsonObject();
                                            joUpdate.putBoolean(colName.VicPromoCol.USED_GIFT2, true);
                                            joUpdate.putNumber(colName.VicPromoCol.GIFT_TIME2, System.currentTimeMillis());
                                            vicPromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean event) {
                                                    if (event) {
                                                        callBack(5134, "update thanh cong su dung qua 2", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    } else {
                                                        callBack(5134, "update fail su dung qua 2", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    }
                                                }
                                            });
                                            return;
                                        } else {
                                            callBack(5136, "Khach hang da su dung qua 2", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                        }
                                        if (obj.useGift1 == true && obj.useGift2 == true && obj.useGift3 == false && obj.isCashin3 == true && obj.isNamed == true) {
                                            JsonObject joUpdate = new JsonObject();
                                            joUpdate.putBoolean(colName.VicPromoCol.USED_GIFT3, true);
                                            joUpdate.putNumber(colName.VicPromoCol.GIFT_TIME3, System.currentTimeMillis());
                                            vicPromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean event) {
                                                    if (event) {
                                                        callBack(5134, "update thanh cong su dung qua 3", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    } else {
                                                        callBack(5134, "update fail su dung qua 3", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    }
                                                }
                                            });
                                            return;
                                        } else {
                                            callBack(5135, "Khach hang da su dung qua 3", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                        }


                                    } else

                                    {
                                        callBack(6000, "", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                    }

                                } else {
                                    //todo callback
                                    addLog("VicPromoVerticle", "KH khong thuoc CTKM VIC ", this.getClass().getSimpleName(), log);
                                    callBack(5102, "KH khong thuoc CTKM VIC ", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                    return;
                                }
                            }

                            private void doPromotionVic() {

                                if (promotionObj.amount >= 10000) {
                                    //check xem tra qua lan thu may
                                    if (obj.bonusTime1 > 0 && obj.bonusTime2 == 0 && obj.bonusTime3 == 0 && !obj.useGift1 && !obj.sendCashinNoti1) {
                                        giftDb.findBy(obj.phoneNumber, new Handler<List<Gift>>() {
                                            @Override
                                            public void handle(List<Gift> gifts) {
                                                if (gifts != null && gifts.size() > 0) {
                                                    final String giftId = gifts.get(0).getModelId();
                                                    Gift vicGift = null;
                                                    for (Gift gift : gifts) {
                                                        if ("vic_gift".equalsIgnoreCase(gift.typeId)) {
                                                            vicGift = gift;
                                                            break;
                                                        }
                                                    }
                                                    activeGift(obj.phoneNumber, vicGift.getModelId(), new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject event) {
                                                            callBack(5139, "active gift", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                        }
                                                    });
                                                    unLockGift(obj.phoneNumber, vicGift.getModelId(), new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean isUnLock) {
                                                            if (isUnLock) {
                                                                JsonObject joUpdate = new JsonObject();
                                                                joUpdate.putNumber(colName.VicPromoCol.TRAN_ID1, promotionObj.tranId);
                                                                joUpdate.putNumber(colName.VicPromoCol.TRAN_AMOUNT1, promotionObj.amount);
                                                                joUpdate.putBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI1, true);
                                                                joUpdate.putBoolean(colName.VicPromoCol.IS_CASHIN1, isUnLock);
                                                                joUpdate.putString(colName.VicPromoCol.GIFTID1, giftId);
                                                                vicPromoDb.updatePartial(obj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                    @Override
                                                                    public void handle(Boolean event) {

                                                                    }
                                                                });
                                                                Long temp = obj.bonusTime1 + (1000L * 60 * 60 * 24 * 30);
                                                                String expireVoucherDate = Misc.dateVNFormat(temp);
                                                                String notibody = StringConstUtil.vicPromoNoti.BODY_CACSHIN1.replace("%s", expireVoucherDate);
                                                                String bodyNoti = notibody;
                                                                String titleNoti = StringConstUtil.vicPromoNoti.CAPTIONNOTI_CASHIN1;
                                                                Notification notification = new Notification();
                                                                notification.priority = 2;
                                                                notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                                                notification.caption = titleNoti;
                                                                notification.body = bodyNoti;
                                                                notification.cmdId = 0L;
                                                                notification.time = System.currentTimeMillis();
                                                                notification.extra = new JsonObject().putString(StringConstUtil.RedirectNoti.URL, "https://momo.vn/thanhtoanvic").toString();
                                                                notification.receiverNumber = DataUtil.strToInt(obj.phoneNumber);
                                                                callBack(5141, "unlock gift1 thanh cong", notification, log, message, this.getClass().getSimpleName(), new JsonObject());

                                                            } else {
                                                                callBack(5142, "lock gift1 fail", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    callBack(5143, "chua co qua lan 1 trong bang gift EU group3, chua tao dc qua cho user nay", null, log, message, this.getClass().getSimpleName(), new JsonObject());

                                                }


                                            }
                                        });
                                    } else if (obj.bonusTime1 > 0 && obj.bonusTime2 > 0 && obj.bonusTime3 == 0 && !obj.isCashin2 && !obj.useGift2 && !obj.sendCashinNoti2) {
                                        giftDb.findBy(promotionObj.phoneNumber, new Handler<List<Gift>>() {
                                            @Override
                                            public void handle(List<Gift> gifts) {
                                                if (gifts != null && gifts.size() > 0) {
                                                    final String giftId = gifts.get(0).getModelId();
                                                    Gift vicGift = null;
                                                    for (Gift gift : gifts) {
                                                        if ("vic_gift".equalsIgnoreCase(gift.typeId)) {
                                                            vicGift = gift;
                                                            break;
                                                        }
                                                    }
                                                    activeGift(obj.phoneNumber, vicGift.getModelId(), new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject event) {
                                                            callBack(5140, "unlock gift", null, log, message, this.getClass().getSimpleName(), new JsonObject());

                                                        }
                                                    });
                                                    unLockGift(obj.phoneNumber, vicGift.getModelId(), new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean isUnLock) {
                                                            if (isUnLock) {
                                                                JsonObject joUpdate = new JsonObject();
                                                                joUpdate.putNumber(colName.VicPromoCol.TRAN_ID2, promotionObj.tranId);
                                                                joUpdate.putNumber(colName.VicPromoCol.TRAN_AMOUNT2, promotionObj.amount);
                                                                joUpdate.putBoolean(colName.VicPromoCol.IS_CASHIN2, isUnLock);
                                                                joUpdate.putBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI2, true);
                                                                joUpdate.putString(colName.VicPromoCol.GIFTID2, giftId);
                                                                vicPromoDb.updatePartial(obj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                    @Override
                                                                    public void handle(Boolean event) {
                                                                        callBack(5140, "unlock gift", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                                    }
                                                                });

                                                                Long temp = obj.bonusTime2 + (1000L * 60 * 60 * 24 * 30);
                                                                String expireVoucherDate = Misc.dateVNFormat(temp);
                                                                String notibody = StringConstUtil.vicPromoNoti.BODY_CACSHIN1.replace("%s", expireVoucherDate);
                                                                String bodyNoti = notibody;
                                                                String titleNoti = StringConstUtil.vicPromoNoti.CAPTIONNOTI_CASHIN1;
                                                                Notification notification = new Notification();
                                                                notification.priority = 2;
                                                                notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                                                notification.caption = titleNoti;
                                                                notification.body = bodyNoti;
                                                                notification.cmdId = 0L;
                                                                notification.time = System.currentTimeMillis();
                                                                notification.receiverNumber = DataUtil.strToInt(obj.phoneNumber);
                                                                callBack(5141, "unlock gift2 thanh cong", notification, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                            } else {
                                                                callBack(5142, "unlock gift2 fail", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    callBack(5143, "chua co qua lan 2 trong bang gift EU group3, chua tao dc qua cho user nay", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                    return;
                                                }


                                            }
                                        });

                                    } else if (obj.bonusTime1 > 0 && obj.bonusTime2 > 0 && obj.bonusTime3 > 0 && !obj.isCashin3 && !obj.useGift3 && !obj.sendCashinNoti3) {
                                        giftDb.findBy(promotionObj.phoneNumber, new Handler<List<Gift>>() {
                                            @Override
                                            public void handle(List<Gift> gifts) {
                                                if (gifts != null && gifts.size() > 0) {
                                                    final String giftId = gifts.get(0).getModelId();
                                                    Gift vicGift = null;
                                                    for (Gift gift : gifts) {
                                                        if ("vic_gift".equalsIgnoreCase(gift.typeId)) {
                                                            vicGift = gift;
                                                            break;
                                                        }
                                                    }
                                                    activeGift(promotionObj.phoneNumber, vicGift.getModelId(), new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject event) {
                                                            callBack(5144, "active gift3 ", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                        }
                                                    });
                                                    unLockGift(promotionObj.phoneNumber, vicGift.getModelId(), new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean isUnLock) {
                                                            if (isUnLock) {
                                                                JsonObject joUpdate = new JsonObject();
                                                                joUpdate.putNumber(colName.VicPromoCol.TRAN_ID3, promotionObj.tranId);
                                                                joUpdate.putNumber(colName.VicPromoCol.TRAN_AMOUNT3, promotionObj.amount);
                                                                joUpdate.putBoolean(colName.VicPromoCol.IS_CASHIN3, isUnLock);
                                                                joUpdate.putBoolean(colName.VicPromoCol.SEND_CASHIN_NOTI3, true);
                                                                joUpdate.putString(colName.VicPromoCol.GIFTID1, giftId);
                                                                vicPromoDb.updatePartial(promotionObj.phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                    @Override
                                                                    public void handle(Boolean event) {
                                                                        callBack(5145, "active gift3 ", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                                    }
                                                                });
                                                                Long temp = obj.bonusTime2 + (1000L * 60 * 60 * 24 * 30);
                                                                String expireVoucherDate = Misc.dateVNFormat(temp);
                                                                String notibody = StringConstUtil.vicPromoNoti.BODY_CACSHIN1.replace("%s", expireVoucherDate);
                                                                String bodyNoti = notibody;
                                                                String titleNoti = StringConstUtil.vicPromoNoti.CAPTIONNOTI_CASHIN1;
                                                                Notification notification = new Notification();
                                                                notification.priority = 2;
                                                                notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                                                notification.caption = titleNoti;
                                                                notification.body = bodyNoti;
                                                                notification.cmdId = 0L;
                                                                notification.time = System.currentTimeMillis();
                                                                notification.receiverNumber = DataUtil.strToInt(obj.phoneNumber);
                                                                callBack(5101, "unlock gift3 thanh cong", null, log, message, this.getClass().getSimpleName(), new JsonObject());

                                                            } else {
                                                                callBack(5102, "unlock gift3 fail", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    callBack(5100, "chua co qua lan 3 trong bang gift EU group3, chua tao dc qua cho user nay", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                                }


                                            }
                                        });

                                    }

                                } else {
                                    callBack(5157, "giao dich duoi 10000", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                                }


                            }
                        });

                    }
                });
            }
        });
    }

    public void getPromotionProgram(final Message message, final Common.BuildLog log, final PromotionObj promotionObj, final PhonesDb.Obj phoneObj, final Handler<PromotionDb.Obj> callback) {
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        final long createdDate = phoneObj.createdDate;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                long promo_start_date = 0;
                long promo_end_date = 0;
                long min_value = 0;
                long gift_amount = 50000;
                String agent = "";

                long currentTime = System.currentTimeMillis();
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase("vic_promo")) {
                            promo_start_date = promoObj.DATE_FROM;
                            promo_end_date = promoObj.DATE_TO;
                            min_value = promoObj.TRAN_MIN_VALUE;
                            gift_amount = promoObj.PER_TRAN_VALUE;
                            agent = promoObj.ADJUST_ACCOUNT;
                            break;
                        }
                    }

                    if (!(System.currentTimeMillis() >= promo_start_date && System.currentTimeMillis() <= promo_end_date)) {
                        //todo addlog
                        addLog("VicPromoVerticle", "Ngoai thoi gian khuyen mai", this.getClass().getSimpleName(), log);
                        callBack(5102, "Ngoai thoi gian khuyen mai", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                        saveErrorPromotionDescription("VicTaxiPromotion", DataUtil.strToInt(promotionObj.phoneNumber), "Khong nam trong danh sach khuyen mai", 5102, phoneObj.deviceInfo, log, this.getClass().getSimpleName());
                        return;
                    }

                    if ("".equalsIgnoreCase(agent)) {
                        //todo addlog
                        addLog("VicPromoVerticle", "Agent is blank", this.getClass().getSimpleName(), log);
                        callBack(5103, "Agent is blank", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                        saveErrorPromotionDescription("VicTaxiPromotion", DataUtil.strToInt(promotionObj.phoneNumber), "Agent is blank", 5103, phoneObj.deviceInfo, log, this.getClass().getSimpleName());
                        return;
                    }
                    callback.handle(promoObj);
                }
            }
        });
    }

    //Sau 15 ngày kể từ ngày nhận thẻ quà tặng 1, Hệ thống kiểm tra KH đã cashin chưa. VIC1
    private void remind15DaysNotCashin1() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 22);
        calendar.set(Calendar.MINUTE, 25);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = isUAT ? (calendarTime - currentTime > 0 ? calendarTime - currentTime : 1) : (calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime));
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait15daysTimerSet) {
                vertx.cancelTimer(wait15daysTimerSet);
                vertx.setPeriodic(isUAT ? 1000L * 60 * 1 : 1000L * 60 * 60 * 24, new Handler<Long>() {
                    @Override
                    public void handle(Long wait14daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();

                        JsonArray jarrAnd = new JsonArray();

                        JsonObject joGreaterTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME1, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 16)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME1, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 15)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);

                        JsonObject joUsedGift = new JsonObject().putBoolean(colName.VicPromoCol.IS_CASHIN1, null);
                        jarrAnd.add(joUsedGift);
                        JsonObject joPayVC1 = new JsonObject().putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER1, true);
                        jarrAnd.add(joPayVC1);

                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        vicPromoDb.searchWithFilter(joSearch, new Handler<ArrayList<VicPromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<VicPromoDb.Obj> listVicPromo) {
                                final AtomicInteger count = new AtomicInteger(listVicPromo.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        int position = count.decrementAndGet();
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }
                                        Long temp = listVicPromo.get(position).bonusTime1 + (1000L * 60 * 60 * 24 * 30);
                                        String expireVoucherDate = Misc.dateVNFormat(temp);
                                        String notibody = StringConstUtil.vicPromoNoti.BODYNOTI15.replace("%s", expireVoucherDate);
                                        String bodyNoti = notibody;
                                        String titleNoti = StringConstUtil.vicPromoNoti.CAPTIONNOTI;
                                        sendNotiViaBroadcast(titleNoti, bodyNoti, listVicPromo.get(position).phoneNumber);

                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    //Hệ thống gửi voucher 2 và notification thông báo đến KH sau 7 ngày kể từ ngày sử dụng voucher
    private void give2ndVoucherUsedGift() {
        final Common.BuildLog log = new Common.BuildLog(logger);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 00);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = isUAT ? (calendarTime - currentTime > 0 ? calendarTime - currentTime : 1) : (calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime));
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait7daysTimerSet) {
                vertx.cancelTimer(wait7daysTimerSet);
                vertx.setPeriodic(isUAT ? 1000L * 60 * 10 : 1000L * 60 * 60 * 24, new Handler<Long>() {
                    @Override
                    public void handle(Long wait7daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();

                        JsonArray jarrAnd = new JsonArray();

                        JsonObject joGreaterTime = new JsonObject().putObject(colName.VicPromoCol.GIFT_TIME1, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 8)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.VicPromoCol.GIFT_TIME1, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 7)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);

                        JsonObject joUsedGift1 = new JsonObject().putBoolean(colName.VicPromoCol.USED_GIFT1, true);
                        jarrAnd.add(joUsedGift1);

                        JsonObject joUsedGift2 = new JsonObject().putBoolean(colName.VicPromoCol.USED_GIFT2, null);
                        jarrAnd.add(joUsedGift2);


                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        vicPromoDb.searchWithFilter(joSearch, new Handler<ArrayList<VicPromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<VicPromoDb.Obj> listVicPromo) {
                                final AtomicInteger count = new AtomicInteger(listVicPromo.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        final int position = count.decrementAndGet();
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }
                                        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                                        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                                        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject json) {
                                                JsonArray array = json.getArray("array", null);
                                                long promo_start_date = 0;
                                                long promo_end_date = 0;
                                                long min_value = 0;
                                                long gift_amount = 50000;
                                                int gift_time = 0;
                                                String agent = "";
                                                String program = "";
                                                long currentTime = System.currentTimeMillis();
                                                List<String> listVoucher = new ArrayList<String>() {{
                                                    add("vic_gift");
                                                }};
                                                if (array != null && array.size() > 0) {
                                                    PromotionDb.Obj promoObj = null;
                                                    for (Object o : array) {
                                                        promoObj = new PromotionDb.Obj((JsonObject) o);
                                                        if (promoObj.NAME.equalsIgnoreCase("vic_promo")) {
                                                            promo_start_date = promoObj.DATE_FROM;
                                                            promo_end_date = promoObj.DATE_TO;
                                                            min_value = promoObj.TRAN_MIN_VALUE;
                                                            gift_amount = promoObj.PER_TRAN_VALUE;
                                                            gift_time = promoObj.DURATION;
                                                            agent = promoObj.ADJUST_ACCOUNT;
                                                            program = promoObj.NAME;
                                                            break;
                                                        }
                                                    }
                                                    final long finalAmount = gift_amount;
                                                    giveVoucher(finalAmount, gift_time, agent, listVicPromo.get(position).phoneNumber, program, listVoucher, log, this.getClass().getSimpleName(), false, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(final JsonObject joResponse) {
                                                            int error = joResponse.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                                                            String desc = joResponse.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                                                            final long giftTranId = joResponse.getLong(StringConstUtil.PromotionField.GIFT_TID, System.currentTimeMillis());
                                                            //todo if success
                                                            JsonObject joUpdate = new JsonObject();
                                                            joUpdate.putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER2, true);
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_AMOUNT2, finalAmount);
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_TIME2, System.currentTimeMillis());
                                                            joUpdate.putNumber(colName.VicPromoCol.ERROR2, error);
                                                            vicPromoDb.updatePartial(listVicPromo.get(position).phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                @Override
                                                                public void handle(Boolean event) {
                                                                    Gift gift = new Gift(joResponse.getObject(StringConstUtil.PromotionField.GIFT, new JsonObject()));
                                                                    Long temp = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30);
                                                                    String expireVoucherDate = Misc.dateVNFormat(temp);
                                                                    String notibody = StringConstUtil.vicPromoNoti.BODY_GIVEVOUCHER2.replace("%s", expireVoucherDate);
                                                                    saveSuccessPromotionTransaction(notibody, giftTranId, finalAmount, listVicPromo.get(position).phoneNumber, "Thẻ quà tặng VIC", "", "momo", notibody, "", new JsonObject());
                                                                    sendNotiViaBroadcast(StringConstUtil.vicPromoNoti.CAPTIONNOTI_GIVEVOUCHER2, notibody, listVicPromo.get(position).phoneNumber);

                                                                }
                                                            });
                                                        }
                                                    });

                                                } else {
                                                    addLog("info", "Chua cau hinh chuong trinh tra thuong cho chuong trinh tra qua tu dong lan 2 da su dung qua 1" + listVicPromo.get(position).phoneNumber, this.getClass().getSimpleName(), log);
                                                    return;

                                                }
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

    // sau 7 ngày từ khi voucher lần 1 đã hết hạn và tự động thu hồi
    private void give2ndVoucherNotUsedGift() {
        final Common.BuildLog log = new Common.BuildLog(logger);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 32);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = isUAT ? (calendarTime - currentTime > 0 ? calendarTime - currentTime : 1) : (calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime));
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait37daysTimerSet) {
                vertx.cancelTimer(wait37daysTimerSet);
                vertx.setPeriodic(isUAT ? 1000L * 60 * 1 : 1000L * 60 * 60 * 24, new Handler<Long>() {
                    @Override
                    public void handle(Long wait7daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();

                        JsonArray jarrAnd = new JsonArray();
                        JsonObject joGreaterTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME1, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 38)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME1, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 37)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);

                        JsonObject joUsedGift1 = new JsonObject().putBoolean(colName.VicPromoCol.USED_GIFT1, null);
                        jarrAnd.add(joUsedGift1);


                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        vicPromoDb.searchWithFilter(joSearch, new Handler<ArrayList<VicPromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<VicPromoDb.Obj> listVicPromo) {
                                final AtomicInteger count = new AtomicInteger(listVicPromo.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        final int position = count.decrementAndGet();
//                                        listLottePromo1.get(position).phoneNumber
                                        //build noti and send
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }

                                        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                                        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                                        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject json) {
                                                JsonArray array = json.getArray("array", null);
                                                long promo_start_date = 0;
                                                long promo_end_date = 0;
                                                long min_value = 0;
                                                long gift_amount = 50000;
                                                int gift_time = 0;
                                                String agent = "";
                                                String program = "";
                                                long currentTime = System.currentTimeMillis();
                                                List<String> listVoucher = new ArrayList<String>() {{
                                                    add("vic_gift");
                                                }};
                                                if (array != null && array.size() > 0) {
                                                    PromotionDb.Obj promoObj = null;
                                                    for (Object o : array) {
                                                        promoObj = new PromotionDb.Obj((JsonObject) o);
                                                        if (promoObj.NAME.equalsIgnoreCase("vic_promo")) {
                                                            promo_start_date = promoObj.DATE_FROM;
                                                            promo_end_date = promoObj.DATE_TO;
                                                            min_value = promoObj.TRAN_MIN_VALUE;
                                                            gift_amount = promoObj.PER_TRAN_VALUE;
                                                            gift_time = promoObj.DURATION;
                                                            agent = promoObj.ADJUST_ACCOUNT;
                                                            program = promoObj.NAME;
                                                            break;
                                                        }
                                                    }

                                                    final long finalAmount = gift_amount;
                                                    //tra thuong
                                                    giveVoucher(finalAmount, gift_time, agent, listVicPromo.get(position).phoneNumber, program, listVoucher, log, this.getClass().getSimpleName(), false, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(final JsonObject joResponse) {
                                                            int error = joResponse.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                                                            String desc = joResponse.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                                                            final long giftTranId = joResponse.getLong(StringConstUtil.PromotionField.GIFT_TID, System.currentTimeMillis());
                                                            //todo if success
                                                            JsonObject joUpdate = new JsonObject();
                                                            joUpdate.putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER2, true);
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_AMOUNT2, finalAmount);
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_TIME2, System.currentTimeMillis());
                                                            joUpdate.putNumber(colName.VicPromoCol.ERROR1, error);
                                                            vicPromoDb.updatePartial(listVicPromo.get(position).phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                @Override
                                                                public void handle(Boolean event) {
                                                                    Long temp = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30);
                                                                    String expireVoucherDate = Misc.dateVNFormat(temp);
                                                                    String notibody = StringConstUtil.vicPromoNoti.BODY_GIVEVOUCHER2.replace("%s", expireVoucherDate);
                                                                    saveSuccessPromotionTransaction(notibody, giftTranId, finalAmount, listVicPromo.get(position).phoneNumber, "Thẻ quà tặng VIC", "", "momo", notibody, "", new JsonObject());
                                                                    sendNotiViaBroadcast(StringConstUtil.vicPromoNoti.CAPTIONNOTI_GIVEVOUCHER2, notibody, listVicPromo.get(position).phoneNumber);

                                                                }
                                                            });
                                                        }
                                                    });

                                                } else {
                                                    addLog("info", "Chua cau hinh chuong trinh tra thuong cho chuong trinh tra qua tu dong lan 2 da su dung qua 1" + listVicPromo.get(position).phoneNumber, this.getClass().getSimpleName(), log);
                                                    return;
                                                }
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


    //Sau 15 ngày kể từ ngày nhận thẻ quà tặng 2, Hệ thống kiểm tra KH đã cashin chưa. VIC1
    private void remind15DaysNotCashin2() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 22);
        calendar.set(Calendar.MINUTE, 25);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = isUAT ? (calendarTime - currentTime > 0 ? calendarTime - currentTime : 1) : (calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime));
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait15daysTimerSet) {
                vertx.cancelTimer(wait15daysTimerSet);
                vertx.setPeriodic(isUAT ? 1000L * 60 * 1 : 1000L * 60 * 60 * 24, new Handler<Long>() {
                    @Override
                    public void handle(Long wait14daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();

                        JsonArray jarrAnd = new JsonArray();

                        JsonObject joGreaterTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME2, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 16)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME2, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 15)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);

                        JsonObject joUsedGift = new JsonObject().putBoolean(colName.VicPromoCol.IS_CASHIN2, null);
                        jarrAnd.add(joUsedGift);

                        JsonObject joPayVC2 = new JsonObject().putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER2, true);
                        jarrAnd.add(joPayVC2);

                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        vicPromoDb.searchWithFilter(joSearch, new Handler<ArrayList<VicPromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<VicPromoDb.Obj> listVicPromo) {
                                final AtomicInteger count = new AtomicInteger(listVicPromo.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        int position = count.decrementAndGet();
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }
                                        Long temp = listVicPromo.get(position).bonusTime1 + (1000L * 60 * 60 * 24 * 30);
                                        String expireVoucherDate = Misc.dateVNFormat(temp);
                                        String notibody = StringConstUtil.vicPromoNoti.BODYNOTI15.replace("%s", expireVoucherDate);
                                        String bodyNoti = notibody;
                                        String titleNoti = StringConstUtil.vicPromoNoti.CAPTIONNOTI;
                                        sendNotiViaBroadcast(titleNoti, bodyNoti, listVicPromo.get(position).phoneNumber);

                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    //Sau 7 ngày từ khi KH sử dụng voucher lần 2 để thanh toán dịch vụ taxi Vic
    private void give3rndVoucherUsedGift() {
        final Common.BuildLog log = new Common.BuildLog(logger);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE,00);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = isUAT ? (calendarTime - currentTime > 0 ? calendarTime - currentTime : 1) : (calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime));
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait7daysTimerSet) {
                vertx.cancelTimer(wait7daysTimerSet);
                vertx.setPeriodic(isUAT ? 1000L * 60 * 1 : 1000L * 60 * 60 * 24, new Handler<Long>() {
                    @Override
                    public void handle(Long wait7daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();

                        JsonArray jarrAnd = new JsonArray();

                        JsonObject joGreaterTime = new JsonObject().putObject(colName.VicPromoCol.GIFT_TIME2, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 8)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.VicPromoCol.GIFT_TIME2, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 7)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);


                        JsonObject joUsedGift2 = new JsonObject().putBoolean(colName.VicPromoCol.USED_GIFT2, true);
                        jarrAnd.add(joUsedGift2);

                        JsonObject joUsedGift3 = new JsonObject().putBoolean(colName.VicPromoCol.USED_GIFT3, null);
                        jarrAnd.add(joUsedGift3);


                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        vicPromoDb.searchWithFilter(joSearch, new Handler<ArrayList<VicPromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<VicPromoDb.Obj> listVicPromo3) {
                                final AtomicInteger count = new AtomicInteger(listVicPromo3.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        final int position = count.decrementAndGet();
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }
                                        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                                        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                                        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject json) {
                                                JsonArray array = json.getArray("array", null);
                                                long promo_start_date = 0;
                                                long promo_end_date = 0;
                                                long min_value = 0;
                                                long gift_amount = 50000;
                                                int gift_time = 0;
                                                String agent = "";
                                                String program = "";
                                                long currentTime = System.currentTimeMillis();
                                                List<String> listVoucher = new ArrayList<String>() {{
                                                    add("vic_gift");
                                                }};
                                                if (array != null && array.size() > 0) {
                                                    PromotionDb.Obj promoObj = null;
                                                    for (Object o : array) {
                                                        promoObj = new PromotionDb.Obj((JsonObject) o);
                                                        if (promoObj.NAME.equalsIgnoreCase("vic_promo")) {
                                                            promo_start_date = promoObj.DATE_FROM;
                                                            promo_end_date = promoObj.DATE_TO;
                                                            min_value = promoObj.TRAN_MIN_VALUE;
                                                            gift_amount = promoObj.PER_TRAN_VALUE;
                                                            gift_time = promoObj.DURATION;
                                                            agent = promoObj.ADJUST_ACCOUNT;
                                                            program = promoObj.NAME;
                                                            break;
                                                        }
                                                    }

                                                    final long finalAmount = gift_amount;
                                                    //tra thuong
                                                    giveVoucher(finalAmount, gift_time, agent, listVicPromo3.get(position).phoneNumber, program, listVoucher, log, this.getClass().getSimpleName(), false, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(final JsonObject joResponse) {
                                                            int error = joResponse.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                                                            String desc = joResponse.getString(StringConstUtil.PromotionField.DESCRIPTION, "");
                                                            final long giftTranId = joResponse.getLong(StringConstUtil.PromotionField.GIFT_TID, System.currentTimeMillis());
                                                            //todo if success
                                                            JsonObject joUpdate = new JsonObject();
                                                            joUpdate.putBoolean(colName.VicPromoCol.IS_PAY_VOUCHER3, true);
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_AMOUNT3, finalAmount);
                                                            joUpdate.putNumber(colName.VicPromoCol.BONUS_TIME3, System.currentTimeMillis());
                                                            joUpdate.putNumber(colName.VicPromoCol.ERROR3, error);
                                                            vicPromoDb.updatePartial(listVicPromo3.get(position).phoneNumber, joUpdate, new Handler<Boolean>() {
                                                                @Override
                                                                public void handle(Boolean event) {

                                                                        Gift gift = new Gift(joResponse.getObject(StringConstUtil.PromotionField.GIFT, new JsonObject()));
                                                                        Long temp = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30);
                                                                        String expireVoucherDate = Misc.dateVNFormat(temp);
                                                                        String notibody = StringConstUtil.vicPromoNoti.BODY_GIVEVOUCHER3.replace("%s", expireVoucherDate);
                                                                        saveSuccessPromotionTransaction(notibody, giftTranId, finalAmount, listVicPromo3.get(position).phoneNumber, "Thẻ quà tặng VIC", "", "momo", notibody, "", new JsonObject());//
                                                                        sendNotiViaBroadcast(StringConstUtil.vicPromoNoti.CAPTIONNOTI_GIVEVOUCHER3,notibody,listVicPromo3.get(position).phoneNumber);
                                                                        callBack(0, "success", null, log, null, this.getClass().getSimpleName().toString(), new JsonObject());
                                                                        return;

                                                                }
                                                            });
                                                        }
                                                    });

                                                } else {
                                                    callBack(5101, "Chua cau hinh chuong trinh tra thuong", null, log, null, this.getClass().getSimpleName(), new JsonObject());
                                                }
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

//Sau 15 ngày kể từ ngày nhận thẻ quà tặng, Hệ thống kiểm tra KH đã Định Danh và cashin chưa.
    //(1) Nếu đã Định danh và cashin, không bắn Noti tự động.
            //(2) Nếu đã Định danh và chưa cashin, tự động bắn Noti nhắc nhở KH thực hiện cashin để sử dụng Thẻ quà tặng
    private void remind15DaysNotUseGift3() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        long currentTime = System.currentTimeMillis();
        long calendarTime = calendar.getTimeInMillis();

        long waitTime = calendarTime - currentTime > 0 ? calendarTime - currentTime : (calendarTime + (24 * 60 * 60 * 1000) - currentTime);
        //long waitTime = calendarTime - currentTime > 0 ? calendarTime - currentTime :1;
        vertx.setTimer(waitTime, new Handler<Long>() {
            @Override
            public void handle(Long wait15daysTimerSet) {
                vertx.cancelTimer(wait15daysTimerSet);
                vertx.setPeriodic(1000L * 60 * 60 * 24, new Handler<Long>() {
                    //vertx.setPeriodic(1000L * 60 * 2, new Handler<Long>() {
                    @Override
                    public void handle(Long wait14daysTimerPeriodic) {
                        JsonObject joSearch = new JsonObject();

                        JsonArray jarrAnd = new JsonArray();

                        JsonObject joGreaterTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME3, new JsonObject().putNumber(MongoKeyWords.GREATER_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 16)));
                        JsonObject joLessTime = new JsonObject().putObject(colName.VicPromoCol.BONUS_TIME3, new JsonObject().putNumber(MongoKeyWords.LESS_OR_EQUAL, System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 15)));

                        jarrAnd.add(joGreaterTime);
                        jarrAnd.add(joLessTime);

//                        JsonObject isCashin3 = new JsonObject().putBoolean(colName.VicPromoCol.IS_CASHIN3, null);
//                        jarrAnd.add(isCashin3);

                        joSearch.putArray(MongoKeyWords.AND_$, jarrAnd);
                        vicPromoDb.searchWithFilter(joSearch, new Handler<ArrayList<VicPromoDb.Obj>>() {
                            @Override
                            public void handle(final ArrayList<VicPromoDb.Obj> listVicPromo) {
                                final AtomicInteger count = new AtomicInteger(listVicPromo.size());
                                vertx.setPeriodic(200L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        int position = count.decrementAndGet();
//                                        listLottePromo1.get(position).phoneNumber
                                        //build noti and send
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            return;
                                        }
                                        //Build noti and send
                                       if(listVicPromo.get(position).isCashin3 == false && listVicPromo.get(position).isNamed  == true){

                                           Long temp = listVicPromo.get(position).bonusTime1 + (1000L * 60 * 60 * 24 * 30);
                                           String expireVoucherDate = Misc.dateVNFormat(temp);
                                           String notibody = StringConstUtil.vicPromoNoti.BODYGIFT3.replace("%s", expireVoucherDate);
                                           String bodyNoti = notibody;
                                           String titleNoti = StringConstUtil.vicPromoNoti.CAPTIONNOTIGIFT3;
                                           sendNotiViaBroadcast(titleNoti, bodyNoti, listVicPromo.get(position).phoneNumber);

                                       }
                                        else if(listVicPromo.get(position).isCashin3 == false && listVicPromo.get(position).isNamed  == false){
                                           Long temp = listVicPromo.get(position).bonusTime1 + (1000L * 60 * 60 * 24 * 30);
                                           String expireVoucherDate = Misc.dateVNFormat(temp);
                                           String notibody = StringConstUtil.vicPromoNoti.BODYGIFT3.replace("%s", expireVoucherDate);
                                           String bodyNoti = notibody;
                                           String titleNoti = StringConstUtil.vicPromoNoti.CAPTIONNOTIGIFT3;
                                           sendNotiViaBroadcast(titleNoti, bodyNoti, listVicPromo.get(position).phoneNumber);
                                       }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }



}
