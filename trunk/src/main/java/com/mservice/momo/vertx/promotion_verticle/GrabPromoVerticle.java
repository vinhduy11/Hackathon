package com.mservice.momo.vertx.promotion_verticle;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.grabpromo.GrabPromoDb;
import com.mservice.momo.data.grabpromo.GrabVoucherDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.promotion.PromotionCountTrackingDb;
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
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by congnguyenit on 8/30/16.
 */
public class GrabPromoVerticle extends MainPromotionVerticle implements Handler<Message<JsonObject>>{

//    GrabCodePromoDb grabCodePromoDb;
    GrabPromoDb grabPromoDb;
    GrabVoucherDb grabVoucherDb;
    PhonesDb phonesDb;
    ErrorPromotionTrackingDb errorPromotionTrackingDb;
    PromotionCountTrackingDb promotionCountTrackingDb;
    Common.BuildLog log;
    Hashtable<String, String> ramTable;
    private boolean isUAT;
    private boolean remindNoti;

    /**
     * init local variable method
     *
     */
    private void init() {
        super.start();
//        grabCodePromoDb = new GrabCodePromoDb(vertx, logger);
        grabPromoDb = new GrabPromoDb(vertx, logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        grabVoucherDb = new GrabVoucherDb(vertx, logger);
        promotionCountTrackingDb = new PromotionCountTrackingDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        log = new Common.BuildLog(logger);
        ramTable = new Hashtable<>();
        isUAT = glbConfig.getBoolean(StringConstUtil.IS_UAT, false);
        remindNoti = glbConfig.getBoolean(StringConstUtil.SEND_REMIND_PROMO, false);
    }

    /**
     *
     */
    @Override
    public void start() {
        init();
        if(remindNoti) {
            autoRemindNoti();
        }
        vertx.eventBus().registerHandler(AppConstant.GRAB_PROMOTION_BUS_ADDRESS, this);
    }

    /**
     *
     * @param message
     */
    @Override
    public void handle(final Message<JsonObject> message) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        JsonObject joReceive = message.body();
        final PromotionObj promotionObj = new PromotionObj(joReceive);
        log.setPhoneNumber(promotionObj.phoneNumber);

        switch (promotionObj.tranType) {
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                updateBillPay(message, log, promotionObj);
                break;
            case SoapProto.MsgType.M2MERCHANT_TRANSFER_VALUE:
                runPromo(message, log, promotionObj);
                break;
            default:
                callBack(2101, "Khong duoc tham gia grab do sai trans type", null, log, message, this.getClass().getSimpleName(), new JsonObject());
                break;
        }
        //String deviceInfo, String os, int appCode, String program, int phoneNumber, String imei, boolean checkGmail, boolean checkSim, String className, Common.BuildLog log, final Handler<JsonObject> callBack
    }



    private void runPromo(final Message<JsonObject> message, final Common.BuildLog log, final PromotionObj promotionObj) {
        if (!promotionObj.serviceId.equalsIgnoreCase("grabcar_eu")) {
            //2000 ~ 2030
            callBack(2101, "Khong duoc tham gia grab", null, log, message, this.getClass().getSimpleName(), new JsonObject());
            return;
        }

        final PhonesDb.Obj userPhoneObj = new PhonesDb.Obj(promotionObj.joPhone);
        if(userPhoneObj.isAgent) {
            saveErrorPromotionDescription(StringConstUtil.GRAB_PROMO.NAME, userPhoneObj.number, "Khong cho DGD tham gia chuong trinh nha nha nha =))", 1000, userPhoneObj.deviceInfo, log, this.getClass().getName());
            callBack(1000, "Khong co DGD tham gia chuong trinh", null, log, message, this.getClass().getName(), new JsonObject());
            return;
        }
        JsonObject joExtra = promotionObj.joExtra;
        String code = joExtra.getString("driverId","");
        final String phone_number = promotionObj.phoneNumber;
        long amount = promotionObj.amount;
        long tranId = promotionObj.tranId;
        executeGrabPromo(userPhoneObj, code, amount, tranId, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joRes) {
                logger.info("Result Grab promotion: " + joRes);
                if(joRes.getInteger(StringConstUtil.ERROR, -1) != 0) {
                    addLog("info", joRes.getString(StringConstUtil.DESCRIPTION,""), this.getClass().getName(), log);
                    saveErrorPromotionDescription(StringConstUtil.GRAB_PROMO.NAME, userPhoneObj.number, joRes.getString(StringConstUtil.DESCRIPTION,""), joRes.getInteger(StringConstUtil.ERROR, -1), userPhoneObj.deviceInfo, log, this.getClass().getName());
                    //logToDb(joRes, phone_number, userPhoneObj.deviceInfo);
                    callBack(joRes.getInteger(StringConstUtil.ERROR, -1), joRes.getString(StringConstUtil.DESCRIPTION,""), null, log, message, this.getClass().getName(), new JsonObject());
                    return;
                }

                JsonObject giftResObj = joRes.getObject(StringConstUtil.EXTRA, new JsonObject());
                JsonObject promoObj = joRes.getObject("promo", new JsonObject());
                PromotionDb.Obj grabPromo = new PromotionDb.Obj(promoObj);
                Gift gift = new Gift(giftResObj.getObject(StringConstUtil.PromotionField.GIFT, new JsonObject()));
                String notibody = PromoContentNotification.GrabPromoContent.GiftMessageContent.replace("%amount%", "" + gift.amount);
                final long giftTranId = giftResObj.getLong(StringConstUtil.PromotionField.GIFT_TID, System.currentTimeMillis());
                saveSuccessPromotionTransaction(notibody, giftTranId, joRes.getLong(StringConstUtil.AMOUNT_GIFT, 0), promotionObj.phoneNumber, grabPromo.NOTI_CAPTION, "", "momo", notibody, "", new JsonObject());
                Notification notification = buildPopupGiftNotification(PromoContentNotification.GrabPromoContent.Title, notibody, 3, 16, giftTranId, "grabcar_eu", gift, gift.amount, promotionObj.phoneNumber, PromoContentNotification.GrabPromoContent.partner);
                callBack(0, joRes.getString(StringConstUtil.DESCRIPTION,""), notification, log, message, this.getClass().getName(), new JsonObject());
            }
        });
    }

    private void updateBillPay(final Message<JsonObject> message, final Common.BuildLog log, final PromotionObj promotionObj) {
        if (promotionObj.jarrGift.size() > 0) {
            final AtomicInteger countGift = new AtomicInteger(promotionObj.jarrGift.size());
            vertx.setPeriodic(250L, new Handler<Long>() {
                @Override
                public void handle(Long timer) {
                    int position = countGift.decrementAndGet();
                    if (position < 0) {
                        callBack(0, "", null, log, message, this.getClass().getName(), new JsonObject());
                        log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "DONE UPDATE GIFT");
                        vertx.cancelTimer(timer);
                    } else {
                        grabVoucherDb.findAndModifyUsedVoucher(promotionObj.phoneNumber, promotionObj.jarrGift.get(position).toString().trim(), new Handler<GrabVoucherDb.Obj>() {
                            @Override
                            public void handle(GrabVoucherDb.Obj event) {

                            }
                        });
                    }
                }
            });
        }
    }

    /**
     *
     * @param code
     * @param callback
     */
    private void checkID(final String code, final String id, final Handler<JsonObject> callback) {
        String phone_number = ramTable.remove(code);
        final JsonObject joReply = new JsonObject();
        grabPromoDb.searchWithFilter(new JsonObject().putString(colName.GrabPromoDbCols.CODE, code), new Handler<ArrayList<GrabPromoDb.Obj>>() {
            @Override
            public void handle(ArrayList<GrabPromoDb.Obj> arr) {
                if (arr.size() > 0) {
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "CODE tài xế đã có trong hệ thống rồi, troll nhau ah? id: " + code);
                    if(isUAT) {
                        logger.info("checkID -------> " + joReply.toString());
                    }
                    callback.handle(joReply);
                } else {
                    grabPromoDb.searchWithFilter(new JsonObject().putString(colName.GrabPromoDbCols.CARDID, id), new Handler<ArrayList<GrabPromoDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<GrabPromoDb.Obj> arrCard) {
                            if (arrCard.size() > 0) {
                                joReply.putNumber(StringConstUtil.ERROR, 1000);
                                joReply.putString(StringConstUtil.DESCRIPTION, "CMND đã có trong hệ thống rồi, troll nhau ah? id: " + code);
                            } else {
                                joReply.putNumber(StringConstUtil.ERROR, 0);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Nhập CODE tài xế thành công cmn rồi, xõa thôi :D");
                            }
                            if(isUAT) {
                                logger.info("checkID -------> " + joReply.toString());
                            }
                            callback.handle(joReply);
                        }
                    });

                }
            }
        });
    }

    /**
     *
     * @param phoneObj
     * @param _code
     * @param amount
     * @param tran_id
     * @param callback
     */
    private void executeGrabPromo(final PhonesDb.Obj phoneObj, String _code, final long amount, final long tran_id, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();
        final String code = _code.toLowerCase();
        final Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jo) {
                JsonArray array = jo.getArray("array", null);
                PromotionDb.Obj grabPromo = null;
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.GRAB_PROMO.NAME)) {
                            grabPromo = promoObj;
                            break;
                        }
                    }
                }
                if(isUAT) {
                    logger.info("test grab promo: " + grabPromo.toJsonObject());
                    logger.info("test grab user info: " + phoneObj.toJsonObject());
                }

                //sai cau hinh
                if (grabPromo == null) {
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Khong ton tai chuong trinh khuyen mai trong cau hinh server, kiem tra lai cau hinh nhe!");
                    callback.handle(joReply);
                    return;
                }

                long current = System.currentTimeMillis();
                //tham gia truoc hoac het thoi gian tham gia
                if(current < grabPromo.DATE_FROM || current > grabPromo.DATE_TO) {
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Đã hết thời gian tham gia chương trình, hẹn gặp lại trong chương trình khác!");
                    callback.handle(joReply);
                    return;
                }

                if(amount < grabPromo.TRAN_MIN_VALUE) {
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Giao dịch chưa đạt hạn mức cho phép nhận thưởng! " + amount + " < hạn mức: " + grabPromo.TRAN_MIN_VALUE);
                    callback.handle(joReply);
                    return;
                }

                final PromotionDb.Obj grabPromofn = grabPromo;
                if(isUAT) {
                    logger.info("cong test grab mo vi truoc ngay: " + phoneObj.createdDate + " < " + Misc.str2BeginDate(grabPromofn.ADJUST_PIN));
                    logger.info("tttttttttt: " + (phoneObj.createdDate < Misc.str2BeginDate(grabPromofn.ADJUST_PIN) ));
                }
                if(phoneObj.createdDate < Misc.str2BeginDate(grabPromofn.ADJUST_PIN) || !phoneObj.isNamed) {
                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                    joReply.putString(StringConstUtil.DESCRIPTION, "Mở ví trước ngày hoặc chưa định danh - create date: " +Misc.dateVNFormat(phoneObj.createdDate) + " - định danh: " + phoneObj.isNamed);
                    callback.handle(joReply);
                    return;
                }

                grabPromoDb.findByPhone("0" + phoneObj.number, new Handler<GrabPromoDb.Obj>() {
                    @Override
                    public void handle(final GrabPromoDb.Obj obj) {
                        if(obj == null) {
                            if(isUAT) {
                                logger.info(phoneObj.number + " Tham gia moi voi code: " + code + " cmnd: " + phoneObj.cardId);
                            }
                            recordUserInputCode(phoneObj, grabPromofn, "0" + phoneObj.number, code, amount, tran_id, phoneObj.createdDate, callback);
                        } else {
                            if(!obj.CODE.equalsIgnoreCase(code)) {
                                joReply.putNumber(StringConstUtil.ERROR, 1000);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Sao lại chơi nhập code khác? code cũ: " + obj.CODE + ", code mới: " + code);
                                callback.handle(joReply);
                                return;
                            }
                            if(isUAT) {
                                logger.info(phoneObj.number + " Tham gia lan n voi code: " + code + " cmnd: " + phoneObj.cardId);
                            }
                            giveVoucher2Member(grabPromofn, obj, "0" + phoneObj.number, amount, tran_id, callback);
                        }
                    }
                });
            }
        });
    }

    /**
     * have a new member join to promotion program, bravo =))
     * @param phone_number
     * @param code
     * @param amount
     * @param tran_id
     * @param callback
     */
    private void recordUserInputCode(final PhonesDb.Obj phoneObj, final PromotionDb.Obj grabPromo, final String phone_number, final String code, final long amount, final long tran_id, final long timeReg, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();
        if(!ramTable.isEmpty() && ramTable.contains(code)) {
            joReply.putNumber(StringConstUtil.ERROR, 1000);
            joReply.putString(StringConstUtil.DESCRIPTION, "2 chú vào cùng 1 lúc, cùng 1 code rồi nhé ? số vào trước: " + ramTable.get(code) + " - chú vào sau: " + phone_number);
            callback.handle(joReply);
            return;
        }
        ramTable.put(code, phone_number);
        checkID(code, phoneObj.cardId,new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject res) {
                int err = res.getInteger(StringConstUtil.ERROR, -1);
                if(err != 0) {
                    addLog("info", res.getString(StringConstUtil.DESCRIPTION, "Méo biết lỗi gì luôn, cứ báo là lỗi thôi :))"), this.getClass().getSimpleName(), log);
                    callback.handle(res);
                    return;
                }

                checkDevice(phoneObj.deviceInfo, phoneObj.phoneOs, phoneObj.appCode, "grab", phoneObj.number, phoneObj.lastImei, false, false, this.getClass().getSimpleName(), log, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject joResult) {
                        int err = joResult.getInteger(StringConstUtil.PromotionField.ERROR, -1);
                        logger.info("Grab ->" + joResult.toString());
                        String desc = joResult.getString(StringConstUtil.DESCRIPTION, "");
                        if (err != 0) {
                            addLog("info", desc, this.getClass().getSimpleName(), log);
                            joReply.putNumber(StringConstUtil.ERROR, 5088);
                            joReply.putString(StringConstUtil.DESCRIPTION, "Check device fail");
                            callback.handle(joResult);
                            return;
                        }
                        final GrabPromoDb.Obj grabObj = new GrabPromoDb.Obj();
                        grabObj.CODE = code;
                        grabObj.NUMBER = phone_number;
                        grabObj.TIME_INPUT = System.currentTimeMillis();
                        grabObj.TRAN_ID = Long.toString(tran_id);
                        grabObj.TIME_REGISTER = timeReg;
                        grabObj.CARD_ID = phoneObj.cardId;
                        if(isUAT) {
                            logger.info("grabPromoDb.insert " + grabObj.toJson());
                        }
                        grabPromoDb.insert(grabObj, new Handler<Integer>() {
                            @Override
                            public void handle(Integer res) {
                                if(res != 0) {
                                    log.add("insert fail " + StringConstUtil.GRAB_PROMO.NAME, grabObj.toJson());
                                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                                    joReply.putString(StringConstUtil.DESCRIPTION, "Có vấn đề trong quá trình kiểm tra khuyến mãi. Vui lòng liên hệ bộ phận CSKH để được hướng dẫn");
                                    callback.handle(joReply);
                                    return;
                                }

                                giveVoucher2Member(grabPromo, grabObj, phone_number, amount, tran_id, callback);
                            }
                        });
                    }});
            }
        });
    }

    /**
     *
     * @param grabPromo
     * @param grabObj
     * @param phone_number
     * @param amount
     * @param tran_id
     * @param callback
     */
    private void giveVoucher2Member(final PromotionDb.Obj grabPromo, final GrabPromoDb.Obj grabObj, final String phone_number, final long amount, long tran_id, final Handler<JsonObject> callback) {
        logger.info("Tra Thuong Grab -------------> " + phone_number);
        groupForPayGift(phone_number, grabPromo.DURATION_TRAN, grabObj.TIME_UPDATE, log, new Handler<Integer>() {
            @Override
            public void handle(final Integer group) {
                final JsonObject joReply = new JsonObject();
                if(group >= 0 && group <= 2) {
                    getListVoucher(grabPromo.INTRO_SMS, new Handler<List>() {
                        @Override
                        public void handle(final List listVoucher) {
                            if(listVoucher ==null || listVoucher.size() <= 0) {
                                log.add("Cau hinh sai " + StringConstUtil.GRAB_PROMO.NAME, "Kiểm tra cấu hình quà");
                                joReply.putNumber(StringConstUtil.ERROR, 1000);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Kiểm tra cấu hình quà");
                                callback.handle(joReply);
                                return;
                            }
                            JsonObject joFilter = new JsonObject();
                            joFilter.putString(colName.GrabVoucherDbCols.NUMBER, phone_number);
                            joFilter.putNumber(colName.GrabVoucherDbCols.TIME_OF_VOUCHER, group);

                            logger.info(group + "======>" + joFilter.toString());
                            grabVoucherDb.searchWithFilter(joFilter, new Handler<ArrayList<GrabVoucherDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<GrabVoucherDb.Obj> arrVoucher) {
                                    logger.info("------> " + arrVoucher.size());
                                    if(arrVoucher.size() > 0) {
                                        log.add("tra qua fail " + StringConstUtil.GRAB_PROMO.NAME, "Da nhan qua dot nay roi, dot: " + (group ) + " doi hoi nhieu qua =))");
                                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "Da nhan qua dot nay roi, dot: " + (group ) + " doi hoi nhieu qua =))");
                                        callback.handle(joReply);
                                        return;
                                    }
                                    JsonObject joUpdate = new JsonObject();
                                    joUpdate.putNumber(colName.GrabPromoDbCols.TIME_UPDATE, System.currentTimeMillis());
                                    joUpdate.putNumber(colName.GrabPromoDbCols.TIME_OF_BONUS, group);
                                    grabPromoDb.updatePartial(phone_number, joUpdate, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean res) {
                                            if(res) {
                                                String[] voucherInfo = getVoucherInfo(listVoucher, group).split(":");
                                                final long totalAmount = DataUtil.stringToUNumber(voucherInfo[1]);
                                                String voucherName = voucherInfo[0];
                                                List<String> lstVoucher = new ArrayList<String>();
                                                lstVoucher.add(voucherName);
                                                giveVoucher(totalAmount, grabPromo.DURATION, grabPromo.ADJUST_ACCOUNT, phone_number, grabPromo.NAME, lstVoucher, log, this.getClass().getName(), true, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(final JsonObject joRes) {
                                                        int err = joRes.getInteger(StringConstUtil.ERROR, -1);
                                                        if(err != 0) {
                                                            log.add("tra qua fail " + StringConstUtil.GRAB_PROMO.NAME, "Tra qua that bai, kiem tra lai du lieu, tra bu cho so: " + phone_number);
                                                            callback.handle(joRes);
                                                            return;
                                                        }
                                                        //successful

                                                        GrabVoucherDb.Obj obj = new GrabVoucherDb.Obj();
                                                        obj.CODE = grabObj.CODE;
                                                        obj.NUMBER = grabObj.NUMBER;
                                                        obj.CASHIN_AMOUNT = amount;
                                                        obj.CASHIN_TIME = obj.VOUCHER_TIME = System.currentTimeMillis();
                                                        obj.VOUCHER_AMOUNT = totalAmount;
                                                        obj.GIFT_ID = joRes.getString(StringConstUtil.PromotionField.GIFT_ID, "empty");
                                                        obj.TIME_OF_VOUCHER = group;
                                                        grabVoucherDb.insert(obj, new Handler<Integer>() {
                                                            @Override
                                                            public void handle(Integer intRes) {
                                                                if (intRes != 0) {
                                                                    log.add("insert fail " + StringConstUtil.GRAB_PROMO.NAME, "Khong update bang voucher duoc, kiem tra lai nhe");
                                                                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                                                                    joReply.putString(StringConstUtil.DESCRIPTION, "");
                                                                    callback.handle(joReply);
                                                                    return;
                                                                }
                                                                log.add("Success " + StringConstUtil.GRAB_PROMO.NAME, "Tra qua thanh cong");
                                                                joReply.putNumber(StringConstUtil.ERROR, 0);
                                                                joReply.putString(StringConstUtil.DESCRIPTION, "");
                                                                joReply.putObject("promo", grabPromo.toJsonObject());
                                                                joReply.putObject(StringConstUtil.EXTRA, joRes);
                                                                joReply.putNumber(StringConstUtil.AMOUNT_GIFT, totalAmount);
                                                                callback.handle(joReply);
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
                    });
                }
            }
        });

    }

    /**
     *
     * @param voucherInfo
     * @param callback
     */
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

    /**
     *
     * @param listVc
     * @param group
     * @return
     */
    private String getVoucherInfo(List<String> listVc, int group) {
        if(listVc.size() == 0) {
            return null;
        }

        int position = listVc.size() >= group + 1 ? group : 0;

        return listVc.size() > 0 ? listVc.get(position).toString().trim() : "";
    }

    /**
     *
     * @param phone_number
     * @param duration
     * @param timeUpdate
     * @param log
     * @param callback
     */
    private void groupForPayGift(String phone_number, final int duration, final long timeUpdate, Common.BuildLog log, final Handler<Integer> callback) {
        grabVoucherDb.searchWithFilter(new JsonObject().putString(colName.GrabVoucherDbCols.NUMBER, phone_number), new Handler<ArrayList<GrabVoucherDb.Obj>>() {
            @Override
            public void handle(ArrayList<GrabVoucherDb.Obj> arr) {
                int group = arr.size();
                long current = System.currentTimeMillis();
                logger.info("duration: " + duration + " ===========> " + duration * 24 * 60 * 60 * 1000L);
                long duration_time = duration * 24 * 60 * 60 * 1000L;
                long mileStoneTime = timeUpdate + duration_time;
                logger.info("timeUpdate: " + timeUpdate + " + duration_time: " + duration_time);
                logger.info("current: " + current + " < mileStoneTime: " + mileStoneTime + " ==> " +(mileStoneTime < current));
                group = mileStoneTime < current? group : -1;
                callback.handle(group);
            }
        });
    }

    private void autoRemindNoti() {
        final Calendar calendar_remind = Calendar.getInstance();
        if(!isUAT)
        {
            calendar_remind.set(Calendar.HOUR_OF_DAY, 20);
            calendar_remind.set(Calendar.MINUTE, 30);
            calendar_remind.set(Calendar.SECOND, 00);
        }
        final long timeRemind = isUAT ? 1000L * 60 * 7 : 1000L * 60 * 60 * 24;
        final long delayRemindTime = calendar_remind.getTimeInMillis() - System.currentTimeMillis() < 1 ? 1 : calendar_remind.getTimeInMillis() - System.currentTimeMillis();
        vertx.setTimer(delayRemindTime, new Handler<Long>() {
            @Override
            public void handle(Long RMTime) {
                //Kiem tra thong tin thu hoi nhom 2
                vertx.setPeriodic(timeRemind, new Handler<Long>() {
                    @Override
                    public void handle(final Long checkInfoTime) {
                        executeRemindNoti();
                    }
                });
                executeRemindNoti();
                vertx.cancelTimer(RMTime);
            }
        });
    }

    private void executeRemindNoti() {
        getPromotion(StringConstUtil.GRAB_PROMO.NAME, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonResponse) {
                int error = jsonResponse.getInteger(StringConstUtil.ERROR, -1000);
                final JsonObject joPromo = jsonResponse.getObject(StringConstUtil.PROMOTION, new JsonObject());
                if(error != 0) {
                    logger.info(jsonResponse.getString(StringConstUtil.DESCRIPTION, ""));
                    return;
                }

                PromotionDb.Obj promoObj = new PromotionDb.Obj(jsonResponse.getObject("promotion", new JsonObject()));
                long current = System.currentTimeMillis();
                //tham gia truoc hoac het thoi gian tham gia
                if(current < promoObj.DATE_FROM || current > promoObj.DATE_TO) {
                    if(isUAT) {
                        logger.info("FROM " + jsonResponse.getLong(colName.PromoCols.DATE_FROM,0) + "  ------  TO " +jsonResponse.getLong(colName.PromoCols.DATE_TO,0));
                        logger.info("cong date time: " + current +" < " + promoObj.DATE_FROM + "," + current + ">" + promoObj.DATE_TO);
                        logger.info("-----------------> hết time của ctkm, không bắn noti nữa nha =)))))" + jsonResponse.toString());
                    }
                    return;
                }
                final Calendar calendar_remind = Calendar.getInstance();
                calendar_remind.set(Calendar.HOUR_OF_DAY, 0);
                calendar_remind.set(Calendar.MINUTE, 0);
                calendar_remind.set(Calendar.SECOND, 0);
                final long startTime7 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 7;
                final long startTime14 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 14;
                calendar_remind.set(Calendar.HOUR_OF_DAY, 23);
                calendar_remind.set(Calendar.MINUTE, 59);
                calendar_remind.set(Calendar.SECOND, 59);
                final long endTime7 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 7;
                final long endTime14 = calendar_remind.getTimeInMillis() - 1000L * 60 * 60 * 24 * 14;

                JsonArray jarrAnd7 = new JsonArray();
                JsonObject joStartTime7 = new JsonObject();
                joStartTime7.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime7);

                JsonObject jStartTime7 = new JsonObject();
                jStartTime7.putObject(colName.GrabPromoDbCols.TIME_UPDATE, joStartTime7);

                jarrAnd7.add(jStartTime7);

                JsonObject joEndTime7 = new JsonObject();
                joEndTime7.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime7);

                JsonObject jEndTime7 = new JsonObject();
                jEndTime7.putObject(colName.GrabPromoDbCols.TIME_UPDATE, joEndTime7);

                jarrAnd7.add(jEndTime7);
                JsonObject joOr7D = new JsonObject().putArray(MongoKeyWords.OR, new JsonArray()
                        .add(new JsonObject().putNumber(colName.GrabPromoDbCols.TIME_OF_BONUS, 0))
                        .add(new JsonObject().putNumber(colName.GrabPromoDbCols.TIME_OF_BONUS, 1)));

                jarrAnd7.add(joOr7D);
                JsonObject joAnd7 = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd7);

                JsonArray jarrAnd14 = new JsonArray();

                JsonObject joStartTime14 = new JsonObject();
                joStartTime14.putNumber(MongoKeyWords.GREATER_OR_EQUAL, startTime14);

                JsonObject jStartTime14 = new JsonObject();
                jStartTime14.putObject(colName.GrabPromoDbCols.TIME_UPDATE, joStartTime14);
                jarrAnd14.add(jStartTime14);

                JsonObject joEndTime14 = new JsonObject();
                joEndTime14.putNumber(MongoKeyWords.LESS_OR_EQUAL, endTime14);

                JsonObject jEndTime14 = new JsonObject();
                jEndTime14.putObject(colName.GrabPromoDbCols.TIME_UPDATE, joEndTime14);
                jarrAnd14.add(jEndTime14);

                JsonObject joOr14D = new JsonObject().putArray(MongoKeyWords.OR, new JsonArray()
                        .add(new JsonObject().putNumber(colName.GrabPromoDbCols.TIME_OF_BONUS, 0))
                        .add(new JsonObject().putNumber(colName.GrabPromoDbCols.TIME_OF_BONUS, 1)));

                jarrAnd14.add(joOr14D);
                JsonObject joAnd14 = new JsonObject().putArray(MongoKeyWords.AND_$, jarrAnd14);

                JsonArray jArrOR = new JsonArray();
                jArrOR.add(joAnd7);
                jArrOR.add(joAnd14);
                JsonArray jArrBigAND = new JsonArray();
                JsonObject joRemind = new JsonObject();

                JsonObject jsonOr = new JsonObject();
                jsonOr.putArray(MongoKeyWords.OR, jArrOR);

                /***/
                /**is_locked**/
                JsonObject jLockNull = new JsonObject().putBoolean(colName.GrabPromoDbCols.IS_LOCK, null);
                JsonObject jLockFalse = new JsonObject().putBoolean(colName.GrabPromoDbCols.IS_LOCK, false);
                JsonArray jLockArr = new JsonArray().add(jLockNull).add(jLockFalse);
                JsonObject jLockOr = new JsonObject().putArray(MongoKeyWords.OR, jLockArr);

                jArrBigAND.add(jsonOr).add(jLockOr);

                JsonObject joEndPromo = new JsonObject();
                joEndPromo.putBoolean(colName.GrabPromoDbCols.END_PROMO, false);

                jArrBigAND.add(joEndPromo);
                joRemind.putArray(MongoKeyWords.AND_$, jArrBigAND);
                if(isUAT)
                {
                    logger.info("cong -> filter : " + joRemind.toString());
                }
                grabPromoDb.searchWithFilter(joRemind, new Handler<ArrayList<GrabPromoDb.Obj>>() {
                    @Override
                    public void handle(final ArrayList<GrabPromoDb.Obj> objs) {
                        if(isUAT) {
                            logger.info("array length: " + objs.size());
                        }
                        final AtomicInteger listItems = new AtomicInteger(objs.size());
                        final PromotionDb.Obj promoProgram = new PromotionDb.Obj(joPromo);
                        vertx.setPeriodic(500L, new Handler<Long>() {
                            @Override
                            public void handle(Long timer) {
                                final int position = listItems.decrementAndGet();
                                if(position < 0) {
                                    logger.info("desc " + StringConstUtil.GRAB_PROMO.NAME + " " + " done noti remind");
                                    vertx.cancelTimer(timer);
                                }
                                else {
                                    GrabPromoDb.Obj obj = objs.get(position);
                                    if(isUAT) {
                                        logger.info("thong tin checkedPromotion: " + obj.TIME_UPDATE + " > " + startTime7 +" && " + obj.TIME_UPDATE + " < " + endTime7 +" && " + obj.TIME_OF_BONUS);
                                    }
                                    if(obj.TIME_UPDATE >  startTime7 && obj.TIME_UPDATE < endTime7 && obj.TIME_OF_BONUS == 0) {
                                        logger.info("desc " + StringConstUtil.GRAB_PROMO.NAME + " " + " noti remind 7");
                                        String bodyNoti = PromoContentNotification.GrabPromoContent.AutoNotiContent.replace("%amount%","30.000").replace("%time%",Misc.dateVNFormat(promoProgram.DATE_TO)).replace("%link%","https://momo.vn/grab");
                                        String titleNoti = PromoContentNotification.GrabPromoContent.Title;
                                        String phoneNumber = objs.get(position).NUMBER;
                                        //JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/grabcar");
//                                        Misc.sendRedirectNoti(vertx, joNoti);
                                        sendNotiViaBroadcastWithUrl(titleNoti, bodyNoti, "https://momo.vn/grab", phoneNumber);
                                    }

                                    if(obj.TIME_UPDATE >  startTime14 && obj.TIME_UPDATE < endTime14 && obj.TIME_OF_BONUS == 0) {
                                        logger.info("desc " + StringConstUtil.GRAB_PROMO.NAME + " " + " noti remind 7");
                                        String bodyNoti = PromoContentNotification.GrabPromoContent.AutoNotiContent.replace("%amount%","30.000").replace("%time%",Misc.dateVNFormat(promoProgram.DATE_TO)).replace("%link%","https://momo.vn/grab");
                                        String titleNoti = PromoContentNotification.GrabPromoContent.Title;
                                        String phoneNumber = objs.get(position).NUMBER;
                                        //JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/grabcar");
//                                        Misc.sendRedirectNoti(vertx, joNoti);
                                        sendNotiViaBroadcastWithUrl(titleNoti, bodyNoti, "https://momo.vn/grab", phoneNumber);
                                    }

                                    if(obj.TIME_UPDATE >  startTime7 && obj.TIME_UPDATE < endTime7 && obj.TIME_OF_BONUS == 1) {
                                        logger.info("desc " + StringConstUtil.GRAB_PROMO.NAME + " " + " noti remind 7");
                                        String bodyNoti = PromoContentNotification.GrabPromoContent.AutoNotiContent.replace("%amount%","20.000").replace("%time%",Misc.dateVNFormat(promoProgram.DATE_TO)).replace("%link%","https://momo.vn/grab");
                                        String titleNoti = PromoContentNotification.GrabPromoContent.Title;
                                        String phoneNumber = objs.get(position).NUMBER;
                                        //JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/grabcar");
//                                        Misc.sendRedirectNoti(vertx, joNoti);
                                        sendNotiViaBroadcastWithUrl(titleNoti, bodyNoti, "https://momo.vn/grab", phoneNumber);
                                    }

                                    if(obj.TIME_UPDATE >  startTime14 && obj.TIME_UPDATE < endTime14 && obj.TIME_OF_BONUS == 1) {
                                        logger.info("desc " + StringConstUtil.GRAB_PROMO.NAME + " " + " noti remind 7");
                                        String bodyNoti = PromoContentNotification.GrabPromoContent.AutoNotiContent.replace("%amount%","20.000").replace("%time%",Misc.dateVNFormat(promoProgram.DATE_TO)).replace("%link%","https://momo.vn/grab");
                                        String titleNoti = PromoContentNotification.GrabPromoContent.Title;
                                        String phoneNumber = objs.get(position).NUMBER;
                                        //JsonObject joNoti = createJsonNotificationRedirect(phoneNumber, bodyNoti, titleNoti, "https://momo.vn/grabcar");
//                                        Misc.sendRedirectNoti(vertx, joNoti);
                                        sendNotiViaBroadcastWithUrl(titleNoti, bodyNoti, "https://momo.vn/grab", phoneNumber);
                                    }
                                }
                            }
                        });
                    }
                });

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
    private JsonObject createJsonNotificationRedirect(String storeNumber, String content, String title, String url)
    {
        JsonObject jo = new JsonObject();
        jo.putString(StringConstUtil.RedirectNoti.CAPTION, title);
        jo.putString(StringConstUtil.RedirectNoti.BODY, content);
        jo.putString(StringConstUtil.RedirectNoti.RECEIVER_NUMBER, storeNumber);
        jo.putNumber(StringConstUtil.RedirectNoti.TRAN_ID, 0L);
        jo.putString(StringConstUtil.RedirectNoti.URL, url);
        return jo;
    }
}
