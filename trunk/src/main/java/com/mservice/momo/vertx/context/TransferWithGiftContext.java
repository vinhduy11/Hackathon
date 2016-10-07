package com.mservice.momo.vertx.context;

import com.mservice.momo.data.Card;
import com.mservice.momo.data.HcPruVoucherManagerDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.CoreCommon;
import com.mservice.momo.gateway.internal.core.objects.Response;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.ServiceUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.QueuedGiftResult;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.UserSetting;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.GiftProcess;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.vcb.VcbCommon;
import com.mservice.momo.vertx.vcb.VcbNoti;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Queue;

/**
 * Created by nam on 10/23/14.
 */
public class TransferWithGiftContext {
    public String giftId;

    public String pin;
    public int phone;
    public String serviceId;
    public String billId; // or toup phone

    public long amount;

    public QueuedGiftResult queuedGiftResult;
//    public List<QueuedGift> queuedGifts;
//    public Collection<Gift> gifts;

    public long voucher;
    public long point;
    public long momo;

    public long curGift;
    public long curPoint;
    public long curMomo;

    public long pointToGift;

    public boolean usePoint;

    public int error;
    public long tranId;
    public int tranType = 0;

    public Long returnPointTranId;
    public Integer returnPointError;
    public Integer backendGiftTran;

    public Long returnMomoTranId;
    public Integer returnMomoError;

    public long remainVoucher = 0;

    public static void build(final int phone,
                             final String serviceId,
                             String billId,
                             final long amount,
                             final Vertx vertx,
                             final GiftManager giftManager,
                             final SockData data,
                             final long minPointToUse,
                             final long minAmountToUseMoint,
                             final Logger logger,
                             final Handler<TransferWithGiftContext> callback) {
        final TransferWithGiftContext context = new TransferWithGiftContext();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "buildWithGift");
        log.add("sid", serviceId);

        context.phone = phone;
        context.billId = billId;
        context.amount = amount;
        context.serviceId = serviceId;
        context.pin = data.pin;
        //TODO: Get user setting
        data.getUserSetting(new Handler<UserSetting>() {
            @Override
            public void handle(UserSetting setting) {
                context.usePoint = setting.useMpoint;
                //TODO: get queued gift
                //prioviderName "Nap tien dien thoai" -> serviceid= 'topup'
                final long minvalue = 60000;
                final long l20 = 20 * 24 * 60 * 60 * 1000L;
                final long currentTime = System.currentTimeMillis();
                PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);
                phonesDb.getPhoneObjInfo(phone, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj obj) {
                        if (obj != null && obj.isAgent) {
//                            callback.handle(context);
                            checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, false, false, callback);
                            return;
                        }
//                        if (serviceId.equalsIgnoreCase(StringConstUtil.PRUDENTIAL)) {
//                            HcPruVoucherManagerDb hcPruVoucherManagerDb = new HcPruVoucherManagerDb(vertx, logger);
//                            hcPruVoucherManagerDb.findOne(context.billId, new Handler<HcPruVoucherManagerDb.Obj>() {
//                                @Override
//                                public void handle(HcPruVoucherManagerDb.Obj obj) {
//                                    if (obj == null) {
//                                        checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
//                                        return;
//                                    } else if (obj != null && (obj.time + l20) < currentTime) {
//                                        checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
//                                        return;
//                                    } else {
//                                        checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, false, true, callback);
////                                        callback.handle(context);
//                                        return;
//                                    }
//                                }
//                            });
//                        } else
//                        if (serviceId.equalsIgnoreCase(StringConstUtil.PHIM123) || serviceId.equalsIgnoreCase(StringConstUtil.CUNGMUA)
//                                ||serviceId.equalsIgnoreCase(StringConstUtil.HAYHAYTV) ||serviceId.equalsIgnoreCase(StringConstUtil.FPTPLAY) ||serviceId.equalsIgnoreCase(StringConstUtil.FSHARE)
//                                ||serviceId.equalsIgnoreCase(StringConstUtil.SCJ) ||serviceId.equalsIgnoreCase(StringConstUtil.OPERATION_SMILE) || serviceId.equalsIgnoreCase(StringConstUtil.HOME_CREDIT)
//                                || serviceId.equalsIgnoreCase(StringConstUtil.PRUDENTIAL))
//                        ================
//                        if (context.amount >= minvalue)
//                        {
////                            if (context.amount >= minvalue) {
////                                checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
////                                return;
////                            } else {
////                                checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, false, true, callback);
//////                                callback.handle(context);
////                                return;
////                            }
//                                checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
//                                return;
//                        }
//                        else {
//                            checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, false, true, callback);
//                            return;
//                        }
//                        ==============
                        int command = Common.ServiceReq.COMMAND.GET_SERVICE_GIFT_RULES;
                        ServiceUtil.loadServiceGiftRuleByServiceId(serviceId, command, vertx, logger, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject jsonObjectService) {
                                long min_amount = jsonObjectService.getLong(colName.CheckServiceGiftRuleCol.MIN_AMOUNT, 0);

                                boolean checkAll = jsonObjectService.getBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED_ALL, false);
                                boolean checkService = jsonObjectService.getBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED, false);
                                String serviceIdGiftRule = jsonObjectService.getString(colName.CheckServiceGiftRuleCol.SERVICE_ID, "");
                                log.add("min_amount", min_amount);
                                log.add("checkAll", checkAll);
                                log.add("checkService", checkService);
                                log.add("serviceIdGiftRule", serviceIdGiftRule);
                                if(checkAll && context.amount >= min_amount)
                                {
                                    log.add("checkQueuedGift", "checkQueuedGift1");
                                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
                                }
                                else if(!checkAll && !checkService)
                                {
                                    log.add("checkQueuedGift", "checkQueuedGift5");
                                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
                                }
                                else if(!checkAll && checkService && !serviceId.equalsIgnoreCase(serviceIdGiftRule))
                                {
                                    log.add("checkQueuedGift", "checkQueuedGift2");
                                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
                                }
                                else if(!checkAll && checkService && serviceId.equalsIgnoreCase(serviceIdGiftRule) && context.amount >= min_amount)
                                {
                                    log.add("checkQueuedGift", "checkQueuedGift3");
                                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
                                }
                                else {
                                    log.add("checkQueuedGift", "checkQueuedGift4");
                                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, false, true, callback);
                                }
                                return;
                            }
                        });
//                        else if (serviceId.equalsIgnoreCase(StringConstUtil.VTHN)) {
//                            if (obj != null) {
//                                if (StringConstUtil.VCB_NAME.equalsIgnoreCase(obj.bank_name) && StringConstUtil.VCB_CODE.equalsIgnoreCase(obj.bank_code)) {
//                                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
//                                    return;
//                                } else if (StringConstUtil.VIETIN_NAME.equalsIgnoreCase(obj.bank_name) && StringConstUtil.VIETIN_CODE.equalsIgnoreCase(obj.bank_code)) {
//                                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
//                                    return;
//                                } else {
//                                    checkVisaRule(vertx, giftManager, context, logger, minAmountToUseMoint, minPointToUse, phone, callback);
//                                    return;
//                                }
//                            }
//                        }

                    }
                });//End phoneDb
            }
        });
    }

    private static void checkVisaRule(final Vertx vertx, final GiftManager giftManager, final TransferWithGiftContext context, final Logger logger,
                                      final long minAmountToUseMoint,
                                      final long minPointToUse,
                                      final int phone,
                                      final Handler<TransferWithGiftContext> callback) {
        Card card = new Card(vertx.eventBus(), logger);
        card.findAll(phone, new Handler<ArrayList<Card.Obj>>() {
            @Override
            public void handle(ArrayList<Card.Obj> objs) {
                Common.BuildLog log = new Common.BuildLog(logger);
                if (objs != null && objs.size() > 0) {
                    boolean mapVisa = false;
                    for (Card.Obj obj : objs) {
                        if (!obj.deleted && obj.bankid.equalsIgnoreCase(StringConstUtil.PHONES_BANKID_SBS)) {
                            mapVisa = true;
                            break;
                        }
                    }
                    if (!mapVisa) {
                        checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, false, true, callback);
//                        callback.handle(context);
                        return;
                    }

                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, true, true, callback);
                    return;
                } else {
                    //Khong map the quoc te ... khong cho dung gift
                    checkQueuedGift(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, false, true, callback);
//                    callback.handle(context);
                    return;
                }
            }
        });
    }

    private static void checkQueuedGift(final Vertx vertx, GiftManager giftManager, final TransferWithGiftContext context, final Common.BuildLog log,
                                        final long minAmountToUseMoint,
                                        final long minPointToUse,
                                        final boolean canUseGift,
                                        final boolean canUsePoint,
                                        final Handler<TransferWithGiftContext> callback) {
        log.add("func", "checkQueuedGift");
        giftManager.getQueuedGift("0" + context.phone, context.serviceId, new Handler<QueuedGiftResult>() {
            @Override
            public void handle(final QueuedGiftResult queuedGiftResult) {
                context.queuedGiftResult = queuedGiftResult;

                getBalance(vertx, context, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                        if (context.amount < minAmountToUseMoint || context.curPoint < minPointToUse) {
                            context.usePoint = false;
                        }

                        context.voucher = 0;
                        context.point = 0;
                        int i = 0;
                        log.add("number of gift", i);
                        if (context.queuedGiftResult.gifts != null && !context.queuedGiftResult.gifts.isEmpty() && canUseGift) {
                            for (Gift gift : context.queuedGiftResult.gifts.values()) {
                                i = i + 1;
                                log.add("number of gift", i);
                                log.add("amount " + i, gift.amount);
                                context.voucher += gift.amount;
                            }
                        }

                        //use point
                        if (context.usePoint) {
                            if (!canUseGift) {
                                context.voucher = 0;
                            }
                            context.point = Math.min(context.amount - context.voucher, context.curPoint);
                            if (context.point < 0) {
                                context.point = 0;
                            }
                        }

                        // check voucher at backend and core side
                        //1. voucher backend > voucher core --> not use voucher at this time
                        if (context.voucher > context.curGift) {
                            log.setPhoneNumber("0" + context.phone);
                            log.add("voucherBackendCoreNotEqual",
                                    "number: 0" + context.phone +
                                            ", backend > core: " + context.voucher + " > " + context.curGift
                            );

                            // reset use voucher
                            context.voucher = 0;
                        }

                        if (!canUseGift) {
                            context.voucher = 0;
                        }

                        if (!canUsePoint) {
                            context.point = 0;
                        }
                        log.add("canUseGift",canUseGift);
                        log.add("canUsePoint",canUsePoint);
                        //calculate how much momo value is used
                        context.momo = context.amount - context.voucher - context.point;

                        if (context.momo < 0 && canUseGift) {
                            context.remainVoucher = -context.momo;
                            context.voucher -= context.remainVoucher;
                        }

//                        log.writeLog();
                        callback.handle(context);
                    }
                });
            }
        });
    }

    public static void buildWithExclude(final int phone,
                                        final String serviceId,
                                        String billId,
                                        final long amount,
                                        final Vertx vertx,
                                        final GiftManager giftManager,
                                        final SockData data,
                                        final long minPointToUse,
                                        final long minAmountToUseMoint,
                                        final ArrayList<String> excludeVals,
                                        final String excludeKey, final Logger logger,
                                        final Handler<TransferWithGiftContext> callback) {
        final TransferWithGiftContext context = new TransferWithGiftContext();

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.add("func", "buildWithExclude");
        log.add("sid", serviceId);

        context.phone = phone;
        context.billId = billId;
        context.amount = amount;
        context.serviceId = serviceId;
        context.pin = data.pin;
        //TODO: Get user setting
        data.getUserSetting(new Handler<UserSetting>() {
            @Override
            public void handle(UserSetting setting) {
                context.usePoint = setting.useMpoint;
                //TODO: get queued gift
                final long minvalue = 60000;
                final long l20 = 20 * 24 * 60 * 60 * 1000L;
                final long currentTime = System.currentTimeMillis();
                PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);
                phonesDb.getPhoneObjInfo(context.phone, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj obj) {
                        if (obj != null && obj.isAgent) {
//                            callback.handle(context);
                            checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, false, false, callback);
                            return;
                        }
                        if (serviceId.equalsIgnoreCase(StringConstUtil.PRUDENTIAL) || serviceId.equalsIgnoreCase(StringConstUtil.HOME_CREDIT)) {
                            HcPruVoucherManagerDb hcPruVoucherManagerDb = new HcPruVoucherManagerDb(vertx, logger);
                            hcPruVoucherManagerDb.findOne(context.billId, new Handler<HcPruVoucherManagerDb.Obj>() {
                                @Override
                                public void handle(HcPruVoucherManagerDb.Obj obj) {
                                    if (obj == null) {
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);
                                        return;
                                    } else if (obj != null && (obj.time + l20) < currentTime) {
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);
                                        return;
                                    } else {
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, false, true, callback);
//                                        callback.handle(context);
                                        return;
                                    }
                                }
                            });
                        } else
//                        if (serviceId.equalsIgnoreCase(StringConstUtil.PHIM123))
                        {
//                            if (context.amount > minvalue) {
//                                checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);
//                                return;
//                            } else {
//                                checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, false, true, callback);
////                                callback.handle(context);
//                                return;
//                            }
                            int command = Common.ServiceReq.COMMAND.GET_SERVICE_GIFT_RULES;
                            ServiceUtil.loadServiceGiftRuleByServiceId(serviceId, command, vertx, logger, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject jsonObjectService) {
                                    long min_amount = jsonObjectService.getLong(colName.CheckServiceGiftRuleCol.MIN_AMOUNT, 0);
                                    boolean checkAll = jsonObjectService.getBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED_ALL, false);
                                    boolean checkService = jsonObjectService.getBoolean(colName.CheckServiceGiftRuleCol.HAS_CHECKED, false);
                                    String serviceIdGiftRule = jsonObjectService.getString(colName.CheckServiceGiftRuleCol.SERVICE_ID, "");
                                    log.add("min_amount", "min_amount");
                                    log.add("checkAll", "checkAll");
                                    log.add("checkService", "checkService");
                                    log.add("serviceIdGiftRule", "serviceIdGiftRule");
                                    log.add("checkQueuedGift", "checkQueuedGift4");
                                    if(checkAll && context.amount >= min_amount)
                                    {
                                        log.add("checkQueuedGiftExclude", "checkQueuedGiftExclude1");
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);

                                    }
                                    else if(!checkAll && !checkService)
                                    {
                                        log.add("checkQueuedGiftExclude", "checkQueuedGiftExclude5");
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);
                                    }
                                    else if(!checkAll && checkService && !serviceId.equalsIgnoreCase(serviceIdGiftRule))
                                    {
                                        log.add("checkQueuedGiftExclude", "checkQueuedGiftExclude2");
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);
                                    }
                                    else if(!checkAll && checkService && serviceId.equalsIgnoreCase(serviceIdGiftRule) && context.amount >= min_amount)
                                    {
                                        log.add("checkQueuedGiftExclude", "checkQueuedGiftExclude3");
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);
                                    }
                                    else {
                                        log.add("checkQueuedGiftExclude", "checkQueuedGiftExclude4");
                                        checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, false, true, callback);
                                    }
                                    return;
                                }
                            });
                        }
//                        else {
//                            checkQueuedGiftExclude(vertx, giftManager, context, log, minAmountToUseMoint, minPointToUse, excludeVals, excludeKey, true, true, callback);
//                            return;
//                        }
                    }
                });
            }
        });
    }

    public static void checkQueuedGiftExclude(final Vertx vertx, GiftManager giftManager, final TransferWithGiftContext context, final Common.BuildLog log,
                                              final long minAmountToUseMoint,
                                              final long minPointToUse,
                                              final ArrayList<String> excludeVals,
                                              final String excludeKey,
                                              final boolean canUseGift,
                                              final boolean canUsePoint,
                                              final Handler<TransferWithGiftContext> callback) {
        giftManager.getQueuedGiftWithExclude("0" + context.phone, context.serviceId, excludeVals, excludeKey, new Handler<QueuedGiftResult>() {
            @Override
            public void handle(QueuedGiftResult queuedGiftResult) {
                context.queuedGiftResult = queuedGiftResult;

                getBalance(vertx, context, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {
                        if (context.amount < minAmountToUseMoint || context.curPoint < minPointToUse) {
                            context.usePoint = false;
                        }

                        context.voucher = 0;
                        context.point = 0;
                        if (context.queuedGiftResult.gifts != null && !context.queuedGiftResult.gifts.isEmpty()) {
                            for (Gift gift : context.queuedGiftResult.gifts.values()) {
                                context.voucher += gift.amount;
                            }
                        }

                        //use point
                        if (context.usePoint) {
                            if(!canUseGift)
                            {
                                context.voucher = 0;
                            }
                            context.point = Math.min(context.amount - context.voucher, context.curPoint);
                            if (context.point < 0) {
                                context.point = 0;
                            }
                        }

                        // check voucher at backend and core side
                        //1. voucher backend > voucher core --> not use voucher at this time
                        if (context.voucher > context.curGift) {
                            log.setPhoneNumber("0" + context.phone);
                            log.add("voucherBackendCoreNotEqual",
                                    "number: 0" + context.phone +
                                            ", backend > core: " + context.voucher + " > " + context.curGift
                            );
//                            log.writeLog();

                            // reset use voucher
                            context.voucher = 0;
                        }

                        if (!canUseGift) {
                            context.voucher = 0;
                        }
                        if (!canUsePoint) {
                            context.point = 0;
                        }
                        //calculate how much momo value is used
                        context.momo = context.amount - context.voucher - context.point;

                        if (context.momo < 0 && canUseGift) {
                            context.remainVoucher = -context.momo;
                            context.voucher -= context.remainVoucher;
                        }

                        callback.handle(context);
                    }
                });
            }
        });
    }

    public static void build(final int phone,
                             final Queue<String> queueServiceId,
                             final String billId,
                             final long amount,
                             final Vertx vertx,
                             final GiftManager giftManager,
                             final SockData data,
                             final long minPointToUse,
                             final long minAmountToUseMoint, final Logger logger,
                             final Handler<TransferWithGiftContext> callback) {
        final TransferWithGiftContext context = new TransferWithGiftContext();

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger);
        log.add("func", "build");
        log.add("with", "queued services");

        context.phone = phone;
        context.billId = billId;
        context.amount = amount;
        //context.serviceId = serviceId;
        context.pin = data.pin;

        //TODO: Get user setting
        data.getUserSetting(new Handler<UserSetting>() {
            @Override
            public void handle(final UserSetting setting) {
                context.usePoint = setting.useMpoint;
                //TODO: get queued gift
                giftManager.getQueuedGift("0" + context.phone, queueServiceId, new Handler<QueuedGiftResult>() {
                    @Override
                    public void handle(final QueuedGiftResult queuedGiftResult) {
                        context.queuedGiftResult = queuedGiftResult;

                        getBalance(vertx, context, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject event) {
                                if (amount < minAmountToUseMoint || context.curPoint < minPointToUse) {
                                    context.usePoint = false;
                                }

                                context.voucher = 0;
                                context.point = 0;
                                if (context.queuedGiftResult.gifts != null && !context.queuedGiftResult.gifts.isEmpty()) {
                                    for (Gift gift : context.queuedGiftResult.gifts.values()) {
                                        context.voucher += gift.amount;
                                    }
                                }

                                //use point
                                if (context.usePoint) {
                                    context.point = Math.min(context.amount - context.voucher, context.curPoint);
                                    if (context.point < 0) {
                                        context.point = 0;
                                    }
                                }

                                // check voucher at backend and core side
                                //1. voucher backend > voucher core --> not use voucher at this time
                                if (context.voucher > context.curGift) {
                                    log.setPhoneNumber("0" + phone);
                                    log.add("voucherBackendCoreNotEqual",
                                            "number: 0" + phone +
                                                    ", backend > core: " + context.voucher + " > " + context.curGift
                                    );
                                    log.writeLog();

                                    // reset use voucher
                                    context.voucher = 0;
                                }
                                context.momo = context.amount - context.voucher - context.point;

                                if (context.momo < 0) {
                                    context.remainVoucher = -context.momo;
                                    context.voucher -= context.remainVoucher;
                                }
                                callback.handle(context);
                            }
                        });
                    }
                });
            }
        });
    }

    private static void getBalance(Vertx vertx
            , final TransferWithGiftContext context
            , final Handler<JsonObject> callback) {
        JsonObject jo = new JsonObject();
        jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
        jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER, context.phone);

        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonRpl) {
                context.curMomo = jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0);
                context.curPoint = jsonRpl.body().getLong(colName.CoreBalanceCols.POINT, 0);
                context.curGift = jsonRpl.body().getLong(colName.CoreBalanceCols.VOUCHER, 0);

                callback.handle(jsonRpl.body());
            }
        });
    }

    public static void getTotalBalance(Vertx vertx
            , int phoneNumber
            , final Handler<JsonObject> callback) {
        JsonObject jo = new JsonObject();
        jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
        jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER, phoneNumber);

        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonRpl) {
                final JsonObject joReply = new JsonObject();
                joReply.putNumber(StringConstUtil.AMOUNT_MOMO, jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0));
                joReply.putNumber(StringConstUtil.AMOUNT_GIFT, jsonRpl.body().getLong(colName.CoreBalanceCols.VOUCHER, 0));
                joReply.putNumber(StringConstUtil.AMOUNT_POINT, jsonRpl.body().getLong(colName.CoreBalanceCols.POINT, 0));
//                context.curMomo = jsonRpl.body().getLong(colName.CoreBalanceCols.BALANCE, 0);
//                context.curPoint = jsonRpl.body().getLong(colName.CoreBalanceCols.POINT, 0);
//                context.curGift = jsonRpl.body().getLong(colName.CoreBalanceCols.VOUCHER, 0);
                callback.handle(joReply);
            }
        });
    }

    public void writeLog(Logger logger) {
        com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, phone);
        log.add("serviceId", serviceId);
        log.add("billId", billId);
        log.add("giftId", giftId);
        log.add("amount", amount);
        log.add("gifts", queuedGiftResult);
        log.add("usePoint", usePoint);
        log.add("curGift", curGift);
        log.add("curPoint", curPoint);
        log.add("curMomo", curMomo);
        log.add("voucher", voucher);
        log.add("point", point);
        log.add("momo", momo);
        log.add("error", error);
        log.add("tranId", tranId);
        log.add("remainVoucher", remainVoucher);
        log.writeLog();
    }

    public void transferGiftIfNeeded(final Vertx vertx, final GiftProcess giftProcess, TransferWithGiftContext context, final Handler<JsonObject> callback) {

        if (queuedGiftResult.gifts != null && !queuedGiftResult.gifts.isEmpty() && error == 0 && context.voucher > 0) {

            giftProcess.setTranWithGift(this, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject result) {
                    backendGiftTran = result.getInteger("error", -1000);
                    callback.handle(result);

                    if (backendGiftTran == 0) {
                        String inviter = "";
                        String giftType = "";
                        for (Gift gift : queuedGiftResult.gifts.values()) {
                            String p = gift.getExtra().getString("inviter", null);
                            if (p != null) {
                                inviter = p;
                                giftType = gift.typeId;
                                break;
                            }
                        }

                        //request promotion for A when B use gift successfully
                        long phoneA = DataUtil.stringToVnPhoneNumber(inviter);

                        if (phoneA > 0 && "VCBPROMO_B".equalsIgnoreCase(giftType)) {

                            VcbCommon.requestGiftForA(vertx
                                    , DataUtil.strToInt(inviter)
                                    , tranType
                                    , tranId
                                    , amount
                                    , "0" + phone);

                            //todo ban noti chuc mung su dung qua thanh cong
                            Notification notiOk = new Notification();
                            notiOk.receiverNumber = phone;
                            notiOk.caption = VcbNoti.UseVoucher.Cap.BUseVcOk;
                            notiOk.body = String.format(VcbNoti.UseVoucher.Body.BUseVcOk, inviter);
                            notiOk.htmlBody = String.format(VcbNoti.UseVoucher.Body.BUseVcOk, inviter);
                            notiOk.sms = "";
                            notiOk.tranId = System.currentTimeMillis(); // ban tren toan he thong
                            notiOk.status = Notification.STATUS_DETAIL;
                            notiOk.type = MomoProto.NotificationType.NOTI_STUDENT_VALUE;
                            notiOk.priority = 2;
                            notiOk.time = System.currentTimeMillis();
                            notiOk.category = 0;
                            Misc.sendNoti(vertx, notiOk);
                        }
                    }
                }
            });
            return;
        }
        callback.handle(new JsonObject().putNumber("error", 0));
    }

    public void momoToPoint(Vertx vertx, Logger logger, long timeOut, final Handler<JsonObject> callback) {
        long returnPoint = momo < 0 ? -momo : 0;
        if (returnPoint <= 0) {
            callback.handle(new JsonObject().putNumber("error", 0).putNumber("tranId", 0));
            return;
        }
        CoreCommon.voucherToPoint(vertx, logger, "0" + phone, pin, returnPoint, timeOut, new Handler<Response>() {
            @Override
            public void handle(Response reply) {
                returnPointTranId = reply.Tid;
                returnPointError = reply.Error;
                callback.handle(new JsonObject()
                                .putNumber("error", reply.Error)
                                .putNumber("tranId", reply.Tid)
                );
            }
        });
    }

    public void returnMomo(Vertx vertx
            , Logger logger
            , String receiveAgent, int error, final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger, phone, "returnMomo");
        log.setPhoneNumber("0" + phone);
        log.add("func", "returnMomo");
        log.add("receiveAgent", receiveAgent);
        log.add("remainVoucher", remainVoucher);

        if (remainVoucher > 0 && error == 0) {
            Misc.adjustment(vertx, "0" + phone
                    , receiveAgent
                    , remainVoucher
                    , WalletType.VOUCHER
                    , null
                    , log
                    , new Handler<com.mservice.momo.vertx.processor.Common.SoapObjReply>() {
                @Override
                public void handle(com.mservice.momo.vertx.processor.Common.SoapObjReply reply) {
                    returnMomoTranId = reply.tranId;
                    returnMomoError = reply.error;
                    log.add("tranid", reply.tranId);
                    log.add("error", reply.error);
                    log.add("desc", SoapError.getDesc(reply.error));

                    callback.handle(new JsonObject()
                                    .putNumber("error", reply.error)
                                    .putNumber("tranId", reply.tranId)
                    );
                    log.writeLog();
                }
            });
            return;
        }
        log.add("desc", "Giao dich goc bi loi");
        log.add("error", error);
        log.add("desc", SoapError.getDesc(error));

        //
        callback.handle(new JsonObject()
                        .putNumber("error", 0)
                        .putNumber("tranId", tranId)
        );
        log.writeLog();
    }
}
