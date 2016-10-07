package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.PromoTrackDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.RetainBinhTanPromo.RetainBinhTanDb;
import com.mservice.momo.data.RetainBinhTanPromo.RetainBinhTanVoucherDb;
import com.mservice.momo.data.binhtanpromotion.AcquireBinhTanGroup3PromotionDb;
import com.mservice.momo.data.binhtanpromotion.AcquireBinhTanUserPromotionDb;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.promotion.WomanNationalTableDb;
import com.mservice.momo.data.referral.ReferralV1CodeInputDb;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.TransferProcess;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import com.mservice.momo.web.internal.webadmin.objs.BankMappingGiftRaw;
import com.mservice.momo.web.internal.webadmin.objs.BinhTanGiftRaw;
import com.mservice.momo.web.internal.webadmin.objs.ReferralGiftRaw;
import com.mservice.momo.web.internal.webadmin.objs.RetainBinhTanRow;
import com.mservice.momo.web.internal.webadmin.verticle.WebAdminVerticle;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by congnguyenit on 8/11/16.
 */
public class ReturnGiftController {
    protected Vertx vertx;
    protected Logger logger;
    GiftDb giftDb = null;
    TransferProcess transferProcess;
    GiftManager giftManager;
    PromoTrackDb promoTrackDb;
    RetainBinhTanDb retainBinhTanDb;
    RetainBinhTanVoucherDb retainBinhTanVoucherDb;
    WomanNationalTableDb womanNationalTableDb;
    private ReferralV1CodeInputDb referralV1CodeInputDb;
    private ErrorPromotionTrackingDb errorPromotionTrackingDb;
    private AcquireBinhTanGroup3PromotionDb acquireBinhTanGroup3PromotionDb;
    private AcquireBinhTanUserPromotionDb acquireBinhTanUserPromotionDb;

    public ReturnGiftController(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.logger = container.logger();
        referralV1CodeInputDb = new ReferralV1CodeInputDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        giftManager = new GiftManager(vertx, logger, container.config());
        acquireBinhTanGroup3PromotionDb = new AcquireBinhTanGroup3PromotionDb(vertx, logger);
        acquireBinhTanUserPromotionDb = new AcquireBinhTanUserPromotionDb(vertx, logger);
        womanNationalTableDb = new WomanNationalTableDb(vertx, logger);
        retainBinhTanDb = new RetainBinhTanDb(vertx, logger);
        retainBinhTanVoucherDb = new RetainBinhTanVoucherDb(vertx, logger);
    }

    @Action(path = "/proccessreturngift/referral")
    public void returnReferralGiftProccessing(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        String hFile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = WebAdminVerticle.STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
        final Queue<ReferralGiftRaw> returnGiftRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            returnGiftRowQueue.add(new ReferralGiftRaw(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (returnGiftRowQueue.size() > 1) {
            ReferralGiftRaw returnGiftRow = returnGiftRowQueue.poll();
            arrayResult.add(returnGiftRow.toJson());
        }

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        logger.info("Before run size returnGiftRowQueue ->" + returnGiftRowQueue.size());
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        logger.info("Object: " + ((JsonObject) o).toString());
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM)) {
                            List<String> listVoucher = getListVoucher(promoObj.INTRO_SMS);
                            doReturnGiftForReferralPromo(returnGiftRowQueue, arrayResult, listVoucher, log, promoObj, callback);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void doReturnGiftForReferralPromo(final Queue<ReferralGiftRaw> returnGiftRowQueue
            , final JsonArray arrayResult
            , final List<String> listVoucher
            , final Common.BuildLog log
            , final PromotionDb.Obj promoObj
            , final Handler<JsonArray> callback) {

        logger.info("Size returnGiftRowQueue ->" + returnGiftRowQueue.size());
        if (returnGiftRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final ReferralGiftRaw rec = returnGiftRowQueue.poll();

        if (rec == null) {
            logger.info("rec is null");
            rec.error = "1000";
            rec.errorDesc = "null field";
            rec.time = "" + System.currentTimeMillis();
            arrayResult.add(rec.toJson());
            doReturnGiftForReferralPromo(returnGiftRowQueue, arrayResult, listVoucher, log, promoObj, callback);
            return;
        } else {
            logger.info("rec is not null");
            if (rec.type.equalsIgnoreCase("A")) {
                logger.info("rec is type A, invitee: " + rec.invitee_number + " - inviter: " + rec.inviter_number);
                logger.info("invitee user duoc tra bu: " + rec.invitee_number);
                rec.number = rec.inviter_number;
                referralV1CodeInputDb.findOne(rec.invitee_number, new Handler<ReferralV1CodeInputDb.Obj>() {
                    @Override
                    public void handle(ReferralV1CodeInputDb.Obj obj) {
                        if (obj != null && obj.inviter_bonus_tid == 0 && !obj.isMapped && obj.inviterNumber.equalsIgnoreCase(rec.inviter_number)) {
                            logger.info("obj = " + obj.inviterNumber + " == " + rec.inviter_number);
                            logger.info("obj.inviterNumber.equalsIgnoreCase(rec.inviter_number) = " + obj.inviterNumber.equalsIgnoreCase(rec.inviter_number));
                            logger.info("condition of type A is ok");
                            bonusForInviter(promoObj.ADJUST_ACCOUNT, rec.inviter_number, promoObj.PER_TRAN_VALUE, rec.invitee_number, log, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject res) {
                                    rec.error = "" + res.getInteger(StringConstUtil.ERROR);
                                    rec.errorDesc = res.getString(StringConstUtil.DESCRIPTION);
                                    rec.time = "" + System.currentTimeMillis();
                                    arrayResult.add(rec.toJson());
                                    logger.info("success type A and recursive call");
                                    doReturnGiftForReferralPromo(returnGiftRowQueue, arrayResult, listVoucher, log, promoObj, callback);
                                    return;
                                }
                            });
                        } else {
                            logger.info("condition of type A is not ok");
                            rec.error = "1000";
                            rec.errorDesc = "Kiem tra gia tri dau vao hoac da duoc tra thuong";
                            rec.time = "" + System.currentTimeMillis();
                            arrayResult.add(rec.toJson());
                            logger.info("type A recursive call");
                            doReturnGiftForReferralPromo(returnGiftRowQueue, arrayResult, listVoucher, log, promoObj, callback);
                            return;
                        }
                    }
                });
            } else if (rec.type.equalsIgnoreCase("B")) {
                logger.info("rec is type B, invitee: " + rec.invitee_number + " - inviter: " + rec.inviter_number);
                rec.number = rec.invitee_number;
                referralV1CodeInputDb.findOne(rec.invitee_number, new Handler<ReferralV1CodeInputDb.Obj>() {
                    @Override
                    public void handle(ReferralV1CodeInputDb.Obj obj) {
                        if (obj != null && obj.invitee_bonus_tid == 0 && !obj.isMapped) {
                            logger.info("condition of type B is ok");
                            bonusForInvitee(promoObj.ADJUST_ACCOUNT, rec.invitee_number, promoObj.TRAN_MIN_VALUE, promoObj.DURATION, listVoucher, log, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject res) {
                                    rec.error = "" + res.getInteger(StringConstUtil.ERROR);
                                    rec.errorDesc = res.getString(StringConstUtil.DESCRIPTION, rec.error.equalsIgnoreCase("0")? "tra qua thanh cong":"Tra qua khong thanh cong");
                                    rec.time = "" + System.currentTimeMillis();
                                    arrayResult.add(rec.toJson());
                                    logger.info("success type B and recursive call");
                                    doReturnGiftForReferralPromo(returnGiftRowQueue, arrayResult, listVoucher, log, promoObj, callback);
                                    return;
                                }
                            });
                        } else {
                            logger.info("condition of type B is not ok");
                            rec.error = "1000";
                            rec.errorDesc = "Kiem tra gia tri dau vao hoac da duoc tra thuong";
                            rec.time = "" + System.currentTimeMillis();
                            arrayResult.add(rec.toJson());
                            doReturnGiftForReferralPromo(returnGiftRowQueue, arrayResult, listVoucher, log, promoObj, callback);
                            return;
                        }
                    }
                });
            } else {
                logger.info("empty field ");
                doReturnGiftForReferralPromo(returnGiftRowQueue, arrayResult, listVoucher, log, promoObj, callback);
                return;
            }
        }
    }

    private void bonusForInviter(String agent, String inviter_number, final long giftValue, final String invitee_number, Common.BuildLog log, final Handler<JsonObject> callback) {
        giveBonusMoneyForInviter(agent, giftValue, inviter_number, log, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject res) {
                int err = res.getInteger(StringConstUtil.ERROR, -1);
                if(err == 0) {
                    long moneyTranId = res.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                    JsonObject joUpdate = new JsonObject();
                    joUpdate.putNumber(colName.ReferralV1CodeInputCol.BONUS_TIME, System.currentTimeMillis());
                    joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITER_BONUS_TID, moneyTranId);
                    joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITER_BONUS_AMOUNT, giftValue);
                    joUpdate.putBoolean(colName.ReferralV1CodeInputCol.INVITER_EXTRA_BONUS, true);
                    referralV1CodeInputDb.updatePartial(invitee_number, joUpdate, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {
                            JsonObject joReply = new JsonObject();
                            joReply.putNumber(StringConstUtil.ERROR, 0);
                            joReply.putString(StringConstUtil.DESCRIPTION, "Tra qua cho user invitee thanh cong");
                            if(!result) {
                                logger.info("Tra qua cho user invitee thanh cong, nhung insert data fail" + result);
                                joReply.putNumber(StringConstUtil.ERROR, 1000);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Tra qua cho user invitee thanh cong, nhung insert data fail");
                            }
                            callback.handle(joReply);
                        }
                    });
                } else {
                    callback.handle(res);
                }
            }
        });
    }

    private void giveBonusMoneyForInviter(final String agent, final long value_of_money, final String phoneNumber, final Common.BuildLog log, final Handler<JsonObject> callback) {
        final JsonObject jsonReply = new JsonObject();
        log.setPhoneNumber(phoneNumber);
        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "getFeeFromStore");
        log.add("phone " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, phoneNumber);
        log.add("agent " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, agent);
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM;
        Misc.adjustment(vertx, agent, phoneNumber, value_of_money,
            Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                @Override
                public void handle(Common.SoapObjReply soapObjReply) {
                    if (soapObjReply != null && soapObjReply.error != 0) {
                        log.add("status " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, soapObjReply.status);
                        log.add("tid " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, soapObjReply.tranId);
                        log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "core tra loi");
                        log.writeLog();
                        JsonObject jsonReply = new JsonObject();
                        jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                        jsonReply.putString(StringConstUtil.DESCRIPTION, "Khong tang duoc tien khach hang, core tra loi");
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        callback.handle(jsonReply);
                        return;
                    }
                    jsonReply.putNumber(StringConstUtil.ERROR, 0);
                    jsonReply.putString(StringConstUtil.DESCRIPTION, "Tra qua cho user inviter thanh cong");
                    jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                    callback.handle(jsonReply);
                }
            });
    }


    private void bonusForInvitee(String agent, final String invitee_number, final long giftValue, int duration,List<String> listVoucher, Common.BuildLog log, final Handler<JsonObject> callback) {
        String source = StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM;
        giveVoucherForUser(giftValue, duration, agent, source, invitee_number, listVoucher, log, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject jo) {
                int error = jo.getInteger(StringConstUtil.ERROR, -1);
                if(error == 0) {
                    long giftTid = jo.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                    JsonObject joUpdate = new JsonObject();
                    joUpdate.putNumber(colName.ReferralV1CodeInputCol.BONUS_TIME, System.currentTimeMillis());
                    joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITEE_BONUS_AMOUNT, giftValue);
                    joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITER_BONUS_AMOUNT, giftValue);
                    joUpdate.putNumber(colName.ReferralV1CodeInputCol.INVITEE_BONUS_TID, giftTid);
                    joUpdate.putBoolean(colName.ReferralV1CodeInputCol.IS_MAPPED, true);
                    joUpdate.putBoolean(colName.ReferralV1CodeInputCol.INVITEE_EXTRA_BONUS, true);
                    referralV1CodeInputDb.findAndUpdateInfoUser(invitee_number, joUpdate, new Handler<ReferralV1CodeInputDb.Obj>() {
                        @Override
                        public void handle(ReferralV1CodeInputDb.Obj obj) {
                            if(obj == null) {
                                jo.putNumber(StringConstUtil.ERROR, 1000);
                                jo.putString(StringConstUtil.DESCRIPTION, "Tra qua cho user inviter thanh cong, nhung insert data fail");
                            }
                            callback.handle(jo);
                        }
                    });
                } else {
                    callback.handle(jo);
                }
            }
        });
    }

    //Ham de suat yeu cau tra thuong.
    //Thuc hien trao thuong
    private void giveVoucherForUser(final long value_of_gift
            , final int time_for_gift
            , final String agent
            , final String source
            , final String phoneNumber
            , final List<String> listVoucher
            , final Common.BuildLog log
            , final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", source));

        keyValues.add(new Misc.KeyValue("group", source));

        final JsonObject joReply = new JsonObject();

        log.add("TOTAL GIFT " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, listVoucher.size());
        log.add("TOTAL VALUE " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, value_of_gift);

        int timeForGift = time_for_gift;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , phoneNumber
                , value_of_gift
                , keyValues, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {

                        final int error = jsonObject.getInteger("error", -1);
                        final long promotedTranId = jsonObject.getLong("tranId", -1);
                        log.add("error" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, error);
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, SoapError.getDesc(error));

                        joReply.putNumber("error", error);

                        //tra thuong trong core thanh cong
                        if (error == 0) {
                            //tao gift tren backend
                            final GiftType giftType = new GiftType();
                            final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                            final Misc.KeyValue kv = new Misc.KeyValue();

                            final long modifyDate = System.currentTimeMillis();
                            final String note = source;

                            keyValues.clear();

                            kv.Key = "group";
                            kv.Value = source;
                            keyValues.add(kv);
                            final int itemPosition = 0;
                            log.add("itemPosition " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, itemPosition);
                            giftType.setModelId(listVoucher.get(itemPosition).trim());
                            giftManager.createLocalGiftForBillPayPromoWithDetailGift(phoneNumber
                                    , value_of_gift
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
                                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "tra thuong chuong trinh zalo bang gift");
                                            log.add("err " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, err);

                                            //------------tat ca thanh cong roi
                                            if (err == 0) {
                                                log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tao qua local thanh cong");
                                                giftManager.useGift(phoneNumber, giftId, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject jsonObject) {
                                                    joReply.putNumber(StringConstUtil.ERROR, 0);
                                                    joReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId);
                                                        joReply.putString(StringConstUtil.DESCRIPTION, "Tao qua local thanh cong");
                                                    callback.handle(joReply);
                                                    return;
                                                    }
                                                });
                                            } else {
                                                log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tao qua local fail");
                                                joReply.putNumber(StringConstUtil.ERROR, 1000);
                                                joReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId);
                                                joReply.putString(StringConstUtil.DESCRIPTION, "Tao qua local fail");
                                                callback.handle(joReply);
                                                return;
                                            }
                                        }
                                    });
                        } else {
                            //tra thuong trong core khong thanh cong
                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Core loi");
                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Lỗi " + SoapError.getDesc(error));
                            log.add("Exception " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Exception " + SoapError.getDesc(error));
                            callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, error).putString(StringConstUtil.DESCRIPTION, "Hệ thống tạm thời gián đoạn. Vui lòng thực hiện lại sau 30 phút")
                                    .putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId));
                            return;
                        }
                    }
                });
    }
    /*************/
    /******************************************************************************************************************************************************************************************************************************************************************************************************/
//Chuong trinh Binh Tan

    @Action(path = "/proccessreturngift/binhtan")
    public void returnBinhTanPromoGiftProccessing(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        String hFile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = WebAdminVerticle.STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
        final Queue<BinhTanGiftRaw> returnGiftRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            returnGiftRowQueue.add(new BinhTanGiftRaw(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (returnGiftRowQueue.size() > 1) {
            BinhTanGiftRaw returnGiftRow = returnGiftRowQueue.poll();
            arrayResult.add(returnGiftRow.toJson());
        }

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        logger.info("Before run size returnGiftRowQueue ->" + returnGiftRowQueue.size());
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        logger.info("Object: " + ((JsonObject) o).toString());
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3)) {
                            List<String> listVoucher = getListVoucher(promoObj.INTRO_SMS);
                            doReturnGiftForBinhTanPromo(returnGiftRowQueue, arrayResult, listVoucher, promoObj, log, callback);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void doReturnGiftForBinhTanPromo(final Queue<BinhTanGiftRaw> returnGiftRowQueue
            , final JsonArray arrayResult
            , final List<String> listVoucher
            , final PromotionDb.Obj promoObj
            , final Common.BuildLog log
            , final Handler<JsonArray> callback) {

        if (returnGiftRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final BinhTanGiftRaw rec = returnGiftRowQueue.poll();

        if (rec == null) {
            doReturnGiftForBinhTanPromo(returnGiftRowQueue, arrayResult, listVoucher, promoObj, log, callback);
        } else {
            acquireBinhTanUserPromotionDb.findOne(rec.number, new Handler<AcquireBinhTanUserPromotionDb.Obj>() {
                @Override
                public void handle(final AcquireBinhTanUserPromotionDb.Obj obj) {
                    if(obj != null && !obj.end_group_3) {
                        JsonObject joFilter = new JsonObject().putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER,rec.number);
                        acquireBinhTanGroup3PromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanGroup3PromotionDb.Obj>>() {
                            @Override
                            public void handle(ArrayList<AcquireBinhTanGroup3PromotionDb.Obj> arrayList) {
                                if(arrayList.size() > 2) {
                                    rec.error = "1000";
                                    rec.errorDesc = "da nhan du so qua, end group user";
                                    arrayResult.add(rec.toJson());
                                    JsonObject joUpdate = new JsonObject().putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, true);
                                    acquireBinhTanUserPromotionDb.updatePartial(rec.number, joUpdate, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {

                                        }
                                    });
                                    doReturnGiftForBinhTanPromo(returnGiftRowQueue, arrayResult, listVoucher, promoObj, log, callback);
                                } else {
                                    processBinhTan(promoObj.ADJUST_ACCOUNT, rec.number, DataUtil.strToLong(rec.giftValue), rec.giftTypeId, promoObj.DURATION,
                                            DataUtil.strToLong(rec.tid_billpay), DataUtil.strToLong(rec.amount_billpay), obj, log, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject event) {
                                                    rec.error = "" + event.getInteger(StringConstUtil.ERROR, -1);
                                                    rec.errorDesc = event.getString(StringConstUtil.DESCRIPTION, "loi khong xac dinh");
                                                    arrayResult.add(rec.toJson());
                                                    doReturnGiftForBinhTanPromo(returnGiftRowQueue, arrayResult, listVoucher, promoObj, log, callback);
                                                }
                                            });
                                }
                            }
                        });
                    } else {
                        processBinhTan(promoObj.ADJUST_ACCOUNT, rec.number, DataUtil.strToLong(rec.giftValue), rec.giftTypeId, promoObj.DURATION,
                                DataUtil.strToLong(rec.tid_billpay), DataUtil.strToLong(rec.amount_billpay), obj, log, new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject event) {
                                        rec.error = "" + event.getInteger(StringConstUtil.ERROR, -1);
                                        rec.errorDesc = event.getString(StringConstUtil.DESCRIPTION, "loi khong xac dinh");
                                        arrayResult.add(rec.toJson());
                                        doReturnGiftForBinhTanPromo(returnGiftRowQueue, arrayResult, listVoucher, promoObj, log, callback);
                                    }
                                });
                    }
                }
            });
        }
    }

    private void processBinhTan(final String agent, final String phoneNumber, final long giftValue, final String giftTypeId, final int duration,
                                final long tid_billpay, final long amount_billpay, final AcquireBinhTanUserPromotionDb.Obj acquireObj,
                                final Common.BuildLog log, final Handler<JsonObject> callback) {

        final JsonObject joReply = new JsonObject();
                if(acquireObj != null) {
                    String source = StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3;
                    final int group = groupPayGift(duration, acquireObj.time_group_3, log);
                    giveVoucherForUserBinhTan(giftValue, duration, agent, source, phoneNumber, giftTypeId, log, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joResponse) {
                            //todo tao db group 3 va insert.
                            int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                            long tranId = joResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                            String giftId = joResponse.getString(StringConstUtil.GIFT_ID, "");
                            if (err == 0) {
                                log.add("desc " + StringConstUtil.BinhTanPromotion.PROGRAM, "Tra qua thanh cong " + phoneNumber);
                                AcquireBinhTanGroup3PromotionDb.Obj acquireObj3 = new AcquireBinhTanGroup3PromotionDb.Obj();
                                acquireObj3.phoneNumber = phoneNumber;
                                acquireObj3.tid_billpay = tid_billpay;
                                acquireObj3.amount_billpay = amount_billpay;
                                acquireObj3.tid_cashin = acquireObj.tid_cashin;
                                acquireObj3.time_cashin = acquireObj.time_cashin;
                                acquireObj3.amount_cashin = acquireObj.amount_cashin;
                                acquireObj3.tid_bonus = tranId;
                                acquireObj3.amount_bonus = giftValue;
                                acquireObj3.time_bonus = System.currentTimeMillis();
                                acquireObj3.giftId = giftId;
                                acquireObj3.extra_bonus = true;
                                acquireBinhTanGroup3PromotionDb.insert(acquireObj3, new Handler<Integer>() {
                                    @Override
                                    public void handle(Integer result) {
                                        if (group == 3) {
                                            JsonObject joUpdate = new JsonObject();
                                            joUpdate.putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, true);
                                            acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean event) {
                                                    if(event) {
                                                        joReply.putNumber(StringConstUtil.ERROR, 0);
                                                        joReply.putString(StringConstUtil.DESCRIPTION, "tra qua va lock user thanh cong");
                                                        callback.handle(joReply);
                                                    } else {
                                                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                                                        joReply.putString(StringConstUtil.DESCRIPTION, "Tra qua thanh cong nhung khong lock duoc user da nhan du 3 qua");
                                                    }
                                                }
                                            });
                                        } else {
                                            joReply.putNumber(StringConstUtil.ERROR, 0);
                                            joReply.putString(StringConstUtil.DESCRIPTION, "Tra qua thanh cong");
                                            callback.handle(joReply);
                                        }
                                        JsonObject joFilter = new JsonObject().putString(colName.AcquireBinhTanGroup3PromotionCol.PHONE_NUMBER, phoneNumber);
                                        acquireBinhTanGroup3PromotionDb.searchWithFilter(joFilter, new Handler<ArrayList<AcquireBinhTanGroup3PromotionDb.Obj>>() {
                                            @Override
                                            public void handle(ArrayList<AcquireBinhTanGroup3PromotionDb.Obj> arrayList) {
                                                if(arrayList.size() > 2) {
                                                    JsonObject joUpdate = new JsonObject()
                                                            .putBoolean(colName.AcquireBinhTanUserPromotionCol.END_GROUP_3, true)
                                                            .putBoolean(colName.AcquireBinhTanUserPromotionCol.IS_LOCKED, true);
                                                    acquireBinhTanUserPromotionDb.updatePartial(phoneNumber, joUpdate, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean event) {
                                                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.BinhTanPromotion.PROGRAM, 1000, "bi lock do da nhan du so qua tu tool tra bu nhe !!! " + phoneNumber);
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                });
                            } else {
                                //todo luu lai bang loi
                                joReply.putNumber(StringConstUtil.ERROR, err);
                                joReply.putString(StringConstUtil.DESCRIPTION, joResponse.getString(StringConstUtil.DESCRIPTION,"khong xac dinh"));
                                callback.handle(joReply);
                                return;
                            }
                        }
                    });
                }

    }

    private int groupPayGift(int duration, long startTimeInG3, Common.BuildLog log)
    {
        log.add("method " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "groupPayGift");
        log.add("startTimeInG3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, startTimeInG3);
        int group = -1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTimeInG3);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        long endStartTimeG3 = /*isUAT ? startTimeInG3 : */calendar.getTimeInMillis();
        log.add("endStartTimeG3 " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, endStartTimeG3);
        long durationMillis = 0;
        /*if(isUAT)
        {
            durationMillis = duration * 1000L * 60;
        }
        else {*/
        durationMillis = duration * 1000L * 60 * 60 * 24;
        /*}*/
        log.add("durationMillis " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, durationMillis);
        if(startTimeInG3 > System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "0");
            group = 0;
        }
        else if(endStartTimeG3 + durationMillis >= System.currentTimeMillis() && startTimeInG3 <= System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "1");
            group = 1;
        }
        else if(endStartTimeG3 + 2 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + durationMillis <= System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "2");
            group = 2;
        }
        else if(endStartTimeG3 + 3 * durationMillis >= System.currentTimeMillis()
                && endStartTimeG3 + 2 * durationMillis <= System.currentTimeMillis())
        {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "3");
            group = 3;
        }
        else if(endStartTimeG3 + 3 * durationMillis < System.currentTimeMillis()) {
            log.add("group " + StringConstUtil.BinhTanPromotion.PROGRAM_GROUP3, "4");
            group = 4;
        }
        return group;
    }

    private void giveVoucherForUserBinhTan(final long value_of_gift
            , final int time_for_gift
            , final String agent
            , final String source
            , final String phoneNumber
            , final String giftTypeId
            , final Common.BuildLog log
            , final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", source));

        keyValues.add(new Misc.KeyValue("group", source));

        final JsonObject joReply = new JsonObject();

        log.add("TOTAL GIFT " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, giftTypeId);
        log.add("TOTAL VALUE " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, value_of_gift);

        int timeForGift = time_for_gift;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , phoneNumber
                , value_of_gift
                , keyValues, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {

                        final int error = jsonObject.getInteger("error", -1);
                        final long promotedTranId = jsonObject.getLong("tranId", -1);
                        log.add("error" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, error);
                        log.add("desc" + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, SoapError.getDesc(error));

                        joReply.putNumber("error", error);

                        //tra thuong trong core thanh cong
                        if (error == 0) {
                            //tao gift tren backend
                            final GiftType giftType = new GiftType();
                            final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                            final Misc.KeyValue kv = new Misc.KeyValue();

                            final long modifyDate = System.currentTimeMillis();
                            final String note = source;

                            keyValues.clear();

                            kv.Key = "group";
                            kv.Value = source;
                            keyValues.add(kv);
                            final int itemPosition = 0;
                            log.add("itemPosition " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, itemPosition);
                            giftType.setModelId(giftTypeId);
                            giftManager.createLocalGiftForBillPayPromoWithDetailGift(phoneNumber
                                    , value_of_gift
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
                                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "tra thuong chuong trinh zalo bang gift");
                                            log.add("err " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, err);

                                            //------------tat ca thanh cong roi
                                            if (err == 0) {
                                                log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tao qua local thanh cong");
                                                giftManager.useGift(phoneNumber, giftId, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject jsonObject) {
                                                        joReply.putNumber(StringConstUtil.ERROR, 0);
                                                        joReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId);
                                                        joReply.putString(StringConstUtil.DESCRIPTION, "Tao qua local thanh cong");
                                                        joReply.putString(StringConstUtil.GIFT_ID, giftId);
                                                        callback.handle(joReply);
                                                        return;
                                                    }
                                                });
                                            } else {
                                                log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Tao qua local fail");
                                                joReply.putNumber(StringConstUtil.ERROR, 1000);
                                                joReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId);
                                                joReply.putString(StringConstUtil.DESCRIPTION, "Tao qua local fail");
                                                callback.handle(joReply);
                                                return;
                                            }
                                        }
                                    });
                        } else {
                            //tra thuong trong core khong thanh cong
                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Core loi");
                            log.add("desc " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Lỗi " + SoapError.getDesc(error));
                            log.add("Exception " + StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM, "Exception " + SoapError.getDesc(error));
                            callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, error).putString(StringConstUtil.DESCRIPTION, "Hệ thống tạm thời gián đoạn. Vui lòng thực hiện lại sau 30 phút")
                                    .putNumber(StringConstUtil.TRANDB_TRAN_ID, promotedTranId));
                            return;
                        }
                    }
                });
    }
    /******************************************************************************************************************************************************************************************************************************************************************************************************/
//Chuong trinh LKTK

    @Action(path = "/proccessreturngift/lktk")
    public void returnBankMapPromoGiftProccessing(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        String hFile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = WebAdminVerticle.STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
        final Queue<BankMappingGiftRaw> returnGiftRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            returnGiftRowQueue.add(new BankMappingGiftRaw(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (returnGiftRowQueue.size() > 1) {
            BankMappingGiftRaw returnGiftRow = returnGiftRowQueue.poll();
            arrayResult.add(returnGiftRow.toJson());
        }

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        logger.info("Before run size returnGiftRowQueue ->" + returnGiftRowQueue.size());
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        logger.info("Object: " + ((JsonObject) o).toString());
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.WomanNationalField.PROGRAM)) {
                            List<String> listVoucher = getListVoucher(promoObj.INTRO_SMS);
                            doReturnGiftForBankMapPromo(returnGiftRowQueue, arrayResult, listVoucher, log, callback);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void doReturnGiftForBankMapPromo(final Queue<BankMappingGiftRaw> returnGiftRowQueue
            , final JsonArray arrayResult
            , final List<String> listVoucher
            , final Common.BuildLog log
            , final Handler<JsonArray> callback) {

        if (returnGiftRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final BankMappingGiftRaw rec = returnGiftRowQueue.poll();

        if (rec == null) {
            doReturnGiftForBankMapPromo(returnGiftRowQueue, arrayResult, listVoucher, log, callback);
        } else {
            womanNationalTableDb.findOne(rec.number, new Handler<WomanNationalTableDb.Obj>() {
                @Override
                public void handle(WomanNationalTableDb.Obj obj) {
                     if(obj == null || obj.giftId.equalsIgnoreCase("") ) {
                        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                        logger.info("Before run size returnGiftRowQueue ->" + returnGiftRowQueue.size());
                        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject json) {
                                JsonArray array = json.getArray("array", null);
                                if (array != null && array.size() > 0) {
                                    PromotionDb.Obj promoObj = null;
                                    for (Object o : array) {
                                        logger.info("Object: " + o.toString());
                                        promoObj = new PromotionDb.Obj((JsonObject) o);
                                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.WomanNationalField.PROGRAM)) {
                                            giveListVouchersForUser(DataUtil.strToLong(rec.giftValue), promoObj.DURATION,
                                                    promoObj.ADJUST_ACCOUNT, rec.number, rec.card_id, DataUtil.strToLong(rec.cashinTid), rec.bank_code,
                                                    listVoucher, promoObj.ENABLE_PHASE2, log, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject event) {
                                                    rec.error = "" +event.getNumber(StringConstUtil.ERROR, -1);
                                                    rec.errorDesc = event.getString(StringConstUtil.DESCRIPTION, "Loi khong xac dinh");
                                                    arrayResult.add(rec.toJson());
                                                    doReturnGiftForBankMapPromo(returnGiftRowQueue, arrayResult, listVoucher, log, callback);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        });

                    } else {
                         rec.error = "1000" ;
                         rec.errorDesc = "da ghi nhan tra thuong truoc do hoac so dt khong ton tai trong db";
                         arrayResult.add(rec.toJson());
                         doReturnGiftForBankMapPromo(returnGiftRowQueue, arrayResult, listVoucher, log, callback);
                     }
                }
            });
        }
    }

    private void giveListVouchersForUser(final long value_of_gift
            , final int duaration
            , final String agent
            , final String phoneNumber
            , final String card_id
            , final long cashinTid
            , final String bank_code
            , final List<String> listVoucher
            , final boolean ENABLE_PHASE2
            , final Common.BuildLog log
            , final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", StringConstUtil.WomanNationalField.PROGRAM));

        keyValues.add(new Misc.KeyValue("group", StringConstUtil.WomanNationalField.PROGRAM));

        final JsonObject joReply = new JsonObject();

        log.add("TOTAL GIFT " + StringConstUtil.WomanNationalField.PROGRAM, listVoucher.size());
        log.add("TOTAL VALUE " + StringConstUtil.WomanNationalField.PROGRAM, value_of_gift);

        int timeForGift = duaration;
        //Tra thuong trong core
        final int endGiftTime = timeForGift;
        giftManager.adjustGiftValue(agent
                , phoneNumber
                , value_of_gift
                , keyValues, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {

                        final int error = jsonObject.getInteger("error", -1);
                        final long promotedTranId = jsonObject.getLong("tranId", -1);
                        log.add("error" + StringConstUtil.WomanNationalField.PROGRAM, error);
                        log.add("desc" + StringConstUtil.WomanNationalField.PROGRAM, SoapError.getDesc(error));

                        joReply.putNumber("error", error);

                        //tra thuong trong core thanh cong
                        if (error == 0) {
                            //tao gift tren backend
                            final GiftType giftType = new GiftType();
                            final ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                            final Misc.KeyValue kv = new Misc.KeyValue();

                            final long modifyDate = System.currentTimeMillis();
                            final String note = StringConstUtil.WomanNationalField.PROGRAM;

                            keyValues.clear();

                            kv.Key = "group";
                            kv.Value = StringConstUtil.WomanNationalField.PROGRAM;
                            keyValues.add(kv);
                            //final AtomicInteger atomicInteger = new AtomicInteger(listVoucher.size());
                            log.add("so luong voucher " + StringConstUtil.WomanNationalField.PROGRAM, listVoucher.size());

                            for(int index = 0; index < listVoucher.size(); index ++ ) {
                                {
                                    final int itemPosition = index;
                                    log.add("itemPosition " + StringConstUtil.WomanNationalField.PROGRAM, itemPosition);
                                    String[] gift = listVoucher.get(itemPosition).split(":");
                                    giftType.setModelId(gift[0].toString().trim());
                                    long giftValue = DataUtil.strToLong(gift[1].toString().trim());
                                    if(giftValue < 1)
                                    {
                                        log.add("func " + StringConstUtil.WomanNationalField.PROGRAM, "Thong tin cau hinh qua khong chinh xac " + phoneNumber);
                                        log.writeLog();
                                        callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, -1000).putString(StringConstUtil.DESCRIPTION, "Thông tin cấu hình quà không đúng, vận hành chỉnh lại webadmin"));
                                        return;
                                    }

                                    giftManager.createLocalGiftForBillPayPromoWithDetailGift(phoneNumber
                                            , giftValue
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
                                                    log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "tra thuong chuong trinh woman bang gift");
                                                    log.add("err " + StringConstUtil.WomanNationalField.PROGRAM, err);

                                                    //------------tat ca thanh cong roi
                                                    if (err == 0) {
                                                        log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Tao qua local woman thanh cong");
                                                        if(ENABLE_PHASE2)
                                                        {
                                                            giftManager.useGift(phoneNumber, giftId, new Handler<JsonObject>() {
                                                                @Override
                                                                public void handle(JsonObject jsonObject) {
                                                                    if(itemPosition == 0)
                                                                    {
                                                                        updateVoucherInfoForUser(giftId, card_id, cashinTid, phoneNumber, bank_code, new Handler<Boolean>() {
                                                                            @Override
                                                                            public void handle(Boolean result) {
                                                                                    joReply.putNumber(StringConstUtil.ERROR, 1000);
                                                                                    joReply.putString(StringConstUtil.DESCRIPTION, "tra qua thanh cong");
                                                                                    callback.handle(joReply);
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else {
                                                            if(itemPosition == 0)
                                                            {
                                                                updateVoucherInfoForUser(giftId, card_id, cashinTid, bank_code, phoneNumber, new Handler<Boolean>() {
                                                                    @Override
                                                                    public void handle(Boolean result) {
                                                                            joReply.putNumber(StringConstUtil.ERROR, 0);
                                                                            joReply.putString(StringConstUtil.DESCRIPTION, "tra qua thanh cong");
                                                                            callback.handle(joReply);
                                                                    }
                                                                });
                                                            }
                                                        }

                                                    } else {
                                                        log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Tao qua woman fail");
                                                        log.writeLog();
                                                        joReply.putNumber(StringConstUtil.ERROR, err);
                                                        joReply.putString(StringConstUtil.DESCRIPTION, "Lỗi backend => không thể tạo quà tặng cho khách hàng.");
                                                        callback.handle(joReply);
                                                    }
                                                }
                                            });
                                }
                            }
                        } else {
                            //tra thuong trong core khong thanh cong
                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Core loi");
                            log.add("desc " + StringConstUtil.WomanNationalField.PROGRAM, "Lỗi " + SoapError.getDesc(error));
                            log.add("Exception " + StringConstUtil.WomanNationalField.PROGRAM, "Exception " + SoapError.getDesc(error));
                            joReply.putNumber(StringConstUtil.ERROR, error);
                            joReply.putString(StringConstUtil.DESCRIPTION, SoapError.getDesc(error));
                            callback.handle(joReply);
                            log.writeLog();
                        }
                    }
                });
    }

    private void updateVoucherInfoForUser(String giftId,
                                          String card_id,
                                          long cashinTid,
                                          String phoneNumber,
                                          String bank_code,
                                          final Handler<Boolean> callback) {
        WomanNationalTableDb.Obj womanObj = new WomanNationalTableDb.Obj();
        womanObj.cardId = card_id;
        womanObj.cashInTid = cashinTid;
        womanObj.phoneNumber = phoneNumber;
        womanObj.bankCode = bank_code;
        womanObj.giftId = giftId;
        womanObj.cashInTime = System.currentTimeMillis();

        womanNationalTableDb.upSert(phoneNumber, womanObj.toJson(), new Handler<Boolean>() {

            @Override
            public void handle(Boolean result) {
                callback.handle(result);
            }
        });
    }

    private List<String> getListVoucher(String voucherInfo)
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

        return listVoucher;

    }

    /*****************************************************************************************************************************************************/
    //RetainBinhTanPromotion
    @Action(path = "/proccessreturngift/retainbt")
    public void returnRetainBinhTanPromoGiftProccessing(HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        final Common.BuildLog log = new Common.BuildLog(logger);
        String hFile = params.get("hfile") == null ? "" : params.get("hfile").trim();
        log.add("hFile", hFile);

        String[] ar = hFile.split("\\\\");
        hFile = ar[ar.length - 1];

        String filePath = WebAdminVerticle.STATIC_FILE_DIRECTORY + hFile;

        log.add("filePath", filePath);
        logger.info("filePath: " + filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);
        final Queue<RetainBinhTanRow> returnGiftRowQueue = new ArrayDeque<>();
        for (Object o : arrayList) {
            returnGiftRowQueue.add(new RetainBinhTanRow(o.toString()));
        }

        final JsonArray arrayResult = new JsonArray();

        if (returnGiftRowQueue.size() > 1) {
            RetainBinhTanRow returnGiftRow = returnGiftRowQueue.poll();
            arrayResult.add(returnGiftRow.toJson());
        }

        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        logger.info("Before run size returnGiftRowQueue ->" + returnGiftRowQueue.size());
        doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
    }

    private void doReturnGiftForRetainBinhTanPromo(final Queue<RetainBinhTanRow> returnGiftRowQueue
            , final JsonArray arrayResult
            , final Handler<JsonArray> callback) {

        if (returnGiftRowQueue.size() == 0) {
            callback.handle(arrayResult);
            return;
        }

        final RetainBinhTanRow rec = returnGiftRowQueue.poll();

        if (rec == null) {
            doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
        } else {
            if(rec.giftValue.equalsIgnoreCase("10000")) {
                retainBinhTanDb.findByPhone(rec.number, new Handler<RetainBinhTanDb.Obj>() {
                    @Override
                    public void handle(RetainBinhTanDb.Obj reObj) {
                        if(reObj == null) {
                            long vcAmount = DataUtil.stringToUNumber(rec.giftValue);
                            JsonObject filterObj = new JsonObject();
                            JsonObject vcValue = new JsonObject().putNumber(colName.RetainBinhTanVoucherDbCols.VOUCHER_AMOUNT, vcAmount);
                            JsonObject vcNumber = new JsonObject().putString(colName.RetainBinhTanVoucherDbCols.NUMBER, rec.number);
                            JsonArray jarr = new JsonArray().add(vcValue).add(vcNumber);
                            filterObj.putArray(MongoKeyWords.AND_$, jarr);
                            retainBinhTanVoucherDb.searchWithFilter(filterObj, new Handler<ArrayList<RetainBinhTanVoucherDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<RetainBinhTanVoucherDb.Obj> array) {
                                    if(array.size() > 0) {
                                        rec.error = "-1";
                                        rec.errorDesc = "da ghi nhan tra thuong truoc do";
                                        arrayResult.add(rec.toJson());
                                        doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
                                        return;
                                    }
                                    giveRetainBTVoucher(rec.number, rec.tid_billpay, DataUtil.strToLong(rec.last_trans_time), DataUtil.strToInt(rec.duration), 10000, rec.agent, 1, new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject joRes) {
                                            int err = joRes.getInteger(StringConstUtil.ERROR, -1);
                                            rec.error = String.valueOf(err);
                                            rec.errorDesc = joRes.getString(StringConstUtil.DESCRIPTION, "");
                                            arrayResult.add(rec.toJson());
                                            doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
                                        }
                                    });
                                }
                            });
                        } else {
                            rec.error = "-1";
                            rec.errorDesc = "reject tam thoi";
                            arrayResult.add(rec.toJson());
                            doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
                        } /*else {
                        giveRetainBTVoucher(rec.number, rec.tid_billpay, DataUtil.strToLong(rec.last_trans_time), DataUtil.strToInt(rec.duration), 30000, rec.agent, 2, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject joRes) {
                                int err = joRes.getInteger(StringConstUtil.ERROR, -1);
                                rec.error = String.valueOf(err);
                                rec.errorDesc = joRes.getString(StringConstUtil.DESCRIPTION, "");
                                arrayResult.add(rec.toJson());
                                doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
                            }
                        });
                    }*/
                    }
                });
            } else if(rec.giftValue.equalsIgnoreCase("30000")) {
                giveRetainBTVoucher(rec.number, rec.tid_billpay, DataUtil.strToLong(rec.last_trans_time), DataUtil.strToInt(rec.duration), 30000, rec.agent, 2, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject joRes) {
                        int err = joRes.getInteger(StringConstUtil.ERROR, -1);
                        rec.error = String.valueOf(err);
                        rec.errorDesc = joRes.getString(StringConstUtil.DESCRIPTION, "");
                        arrayResult.add(rec.toJson());
                        doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
                    }
                });
            } else {
                rec.error = "-1";
                rec.errorDesc = "khong dung voi gia tri nhan thuong cua chuong trinh";
                arrayResult.add(rec.toJson());
                doReturnGiftForRetainBinhTanPromo(returnGiftRowQueue, arrayResult, callback);
            }

        }
    }

//    public String   NUMBER;
//    public String   LAST_TID;
//    public long     LAST_TRANS_TIME;
//    public int      TIME_OF_VOUCHER;
//    public long     START_TIME;
//    public long     END_TIME;
//    public long     UPDATE_TIME;
    private void giveRetainBTVoucher(final String number, final String last_tid, long last_trans_time, final int duration, final long amount, final String agent, final int vcTime, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();
        RetainBinhTanDb.Obj obj = new RetainBinhTanDb.Obj();
        obj.NUMBER = number;
        obj.LAST_TID = last_tid;
        obj.LAST_TRANS_TIME = last_trans_time;
        obj.START_TIME = obj.UPDATE_TIME = System.currentTimeMillis();
        if(vcTime == 1) {
            retainBinhTanDb.insert(obj, new Handler<Integer>() {
                @Override
                public void handle(Integer res) {
                    if(res != 0) {

                        joReply.putNumber(StringConstUtil.ERROR, -1);
                        joReply.putString(StringConstUtil.DESCRIPTION, "insert bang mongo khong thanh cong, kiem tra ket noi");
                        callback.handle(joReply);
                        return;
                    }
                    List<String> lstGiftName = new ArrayList<String>();
                    lstGiftName.add("retain_binhtan_gift");
                    giveVoucher(amount, duration, agent, number, "retain_binhtan_promo", lstGiftName, true, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject joResponse) {
                            int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                            if(err != 0) {
                                callback.handle(joResponse);
                                return;
                            }

                            RetainBinhTanVoucherDb.Obj vcObj = new RetainBinhTanVoucherDb.Obj();
                            vcObj.GIFT_ID = joResponse.getString(StringConstUtil.PromotionField.GIFT_ID);
                            vcObj.NUMBER = number;
                            vcObj.VOUCHER_TIME = System.currentTimeMillis();
                            vcObj.VOUCHER_AMOUNT = amount;
                            vcObj.TIME_OF_VOUCHER = vcTime;
                            retainBinhTanVoucherDb.insert(vcObj, new Handler<Integer>() {
                                @Override
                                public void handle(Integer vcres) {
                                    if(vcres != 0) {
                                        joReply.putNumber(StringConstUtil.ERROR, -1);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "insert bang mongo khong thanh cong, kiem tra ket noi");
                                        callback.handle(joReply);
                                        return;
                                    }
                                    joReply.putNumber(StringConstUtil.ERROR, 0);
                                    joReply.putString(StringConstUtil.DESCRIPTION, "tra qua thanh cong");
                                    callback.handle(joReply);
                                }
                            });
                        }
                    });
                }
            });
        } else {
            List<String> lstGiftName = new ArrayList<String>();
            lstGiftName.add("retain_binhtan_gift");
            giveVoucher(amount, duration, agent, number, "retain_binhtan_promo", lstGiftName, true, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject joResponse) {
                    int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                    if(err != 0) {
                        callback.handle(joResponse);
                        return;
                    }

                    RetainBinhTanVoucherDb.Obj vcObj = new RetainBinhTanVoucherDb.Obj();
                    vcObj.GIFT_ID = joResponse.getString(StringConstUtil.PromotionField.GIFT_ID);
                    vcObj.NUMBER = number;
                    vcObj.VOUCHER_TIME = System.currentTimeMillis();
                    vcObj.VOUCHER_AMOUNT = amount;
                    vcObj.TIME_OF_VOUCHER = vcTime;
                    retainBinhTanVoucherDb.insert(vcObj, new Handler<Integer>() {
                        @Override
                        public void handle(Integer vcres) {
                            if(vcres != 0) {
                                joReply.putNumber(StringConstUtil.ERROR, -1);
                                joReply.putString(StringConstUtil.DESCRIPTION, "insert bang mongo khong thanh cong, kiem tra ket noi");
                                callback.handle(joReply);
                                return;
                            }
                            joReply.putNumber(StringConstUtil.ERROR, 0);
                            joReply.putString(StringConstUtil.DESCRIPTION, "tra qua thanh cong");
                            callback.handle(joReply);
                        }
                    });
                }
            });
        }

    }

    protected void giveVoucher(final long totalGiftAmount
            , final int giftTime
            , final String agent
            , final String phoneNumber
            , final String program
            , final List<String> listGiftName
            , final boolean activeGift
            , final Handler<JsonObject> callback) {
        // Trả khuyến mãi
        final JsonObject joReply = new JsonObject();
        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
        keyValues.add(new Misc.KeyValue("program", program));

        keyValues.add(new Misc.KeyValue("group", program));
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

                            vertx.setPeriodic(250L, new Handler<Long>() {
                                @Override
                                public void handle(Long aPeriodicLong) {
                                    if (atomicInteger.decrementAndGet() < 0) {
                                        //callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                                        vertx.cancelTimer(aPeriodicLong);
                                    } else {
                                        final int itemPosition = atomicInteger.intValue();
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

                                                        //------------tat ca thanh cong roi
                                                        if (err == 0) {
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
                            callback.handle(new JsonObject().putNumber(StringConstUtil.PromotionField.ERROR, error).putString(StringConstUtil.PromotionField.DESCRIPTION, "Core tra loi khi tra the qua tang " + error)
                                    .putNumber(StringConstUtil.PromotionField.GIFT_TID, promotedTranId));
                            return;
                        }
                    }
                });
    }
}