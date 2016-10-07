package com.mservice.momo.vertx.gift;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.gift.*;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.gateway.internal.core.CoreCommon;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.msg.CoreMessage;
import com.mservice.momo.gateway.internal.core.objects.*;
import com.mservice.momo.gateway.internal.soapin.SoapVerticle;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.gift.models.*;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.*;

/**
 * Created by nam on 9/29/14.
 */
public class GiftManager {
    public static final String TRANTYPE_TRANSFER = "transfer";
    public static final String TRANTYPE_G2N = "g2n";
    public static long DEFAULT_CORE_TIMEOUT = 7 * 60 * 1000L;

    public String G2N_ACCOUNT = "";
    private long MIN_GIFT_PRICE = 5000;
    private long MAX_GIFT_PRICE = 2000000;

    private Vertx vertx;
    private Logger logger;
    private ServiceDb serviceDb;
    private GiftTypeDb giftTypeDb;
    private GiftDb giftDb;
    private GiftHistoryDb giftHistoryDb;
    private GiftToNumberDb giftToNumberDb;
    private QueuedGiftDb queuedGiftDb;
    private TimedGiftDb timedGiftDb;
    private PhonesDb phonesDb;
    private boolean isStoreBackend = false;

    public GiftManager(Vertx vertx, Logger logger, JsonObject globalConfig) {
        this.vertx = vertx;
        this.logger = logger;
        this.giftTypeDb = new GiftTypeDb(vertx, logger);
        this.serviceDb = new ServiceDb(vertx.eventBus(), logger);
        this.giftDb = new GiftDb(vertx, logger);
        this.giftHistoryDb = new GiftHistoryDb(vertx, logger);
        this.giftToNumberDb = new GiftToNumberDb(vertx, logger);
        this.timedGiftDb = new TimedGiftDb(vertx, logger);
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);

        JsonObject giftConfig = globalConfig.getObject("gift", new JsonObject());
        MIN_GIFT_PRICE = giftConfig.getLong("minGiftPrice", 10000);
        MAX_GIFT_PRICE = giftConfig.getLong("maxGiftPrice", 2000000);
        boolean isStoreBackend = globalConfig.getBoolean("storeApp", false);

        JsonObject sms_for_m2number = globalConfig.getObject("sms_for_m2number", new JsonObject());
        G2N_ACCOUNT = sms_for_m2number.getString("m2nworkingaccount", "0");
        queuedGiftDb = new QueuedGiftDb(vertx, logger);

        DEFAULT_CORE_TIMEOUT = globalConfig.getLong("coreTimeOut", 7 * 60 * 1000L);

//        GiftType giftType = new GiftType();
//        giftType.setModelId("TYPE01");
//        giftType.serviceId = "topup";
//        giftType.name = "Topup Gift";
//        giftType.desc = "Topup Gift Store";
//        giftType.icon = "http://adi.vcmedia.vn/adt/cpc/ssvimg/2014/09/90x90-11411944911.jpg";
//        giftType.image = "http://afamily1.vcmedia.vn/zoom/201_126/KKeeNwcSSZYWZsF7Ky9KzFlY9R0wGt/Image/2014/03/tumblr_msaukqFeb71qf3v8bo1_500-37045.jpg";
//        giftType.transfer = true;
//        giftType.status = GiftType.STATUS_ACTIVE;
//        giftType.modifyDate = new Date();
//        giftType.isNew = true;
//        giftType.price = new JsonArray().add(20000).add(40000).add(80000).add(-1);
//        giftTypeDb.save(giftType, new Handler<String>() {
//            @Override
//            public void handle(String event) {
//                System.err.println("saved");
//            }
//        });

    }

    public void adjustGiftValue(final String fromAgent
            , final String toAgent
            , final long amount
            , final ArrayList<Misc.KeyValue> keyValues
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, fromAgent, "adjustGiftValue");
        log.add("fromAgent", fromAgent);
        log.add("toAgent", toAgent);
        log.add("amount", amount);

        Misc.adjustment(vertx
                , fromAgent
                , toAgent
                , amount
                , Core.WalletType.VOUCHER_VALUE
                , keyValues
                , log, new Handler<com.mservice.momo.vertx.processor.Common.SoapObjReply>() {
            @Override
            public void handle(final com.mservice.momo.vertx.processor.Common.SoapObjReply soapObjReply) {
                log.add("tranId", soapObjReply.tranId);
                log.add("error", soapObjReply.error);
                log.writeLog();
                callback.handle(new JsonObject()
                        .putNumber("error", soapObjReply.error)
                        .putNumber("tranId", soapObjReply.tranId));
            }
        });
    }

    public void adjustGift(final String fromAgent
            , final String toAgent
            , final String giftId
            , final ArrayList<Misc.KeyValue> keyValues
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, fromAgent, "adjustGift");

        Gift filter = new Gift();
        filter.setModelId(giftId);
        giftDb.findOne(filter, new Handler<Gift>() {
            @Override
            public void handle(Gift gift) {
                if (gift == null) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", GiftError.NO_SUCH_GIFT)
                                    .putString("desc", "No such gift")
                    );

                    log.add("error", "No such gift Id");
                    log.writeLog();
                    return;
                }
                adjustGift(fromAgent, toAgent, gift, keyValues, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        callback.handle(result);
                    }
                });
            }
        });
    }

    /**
     * Warning: Gift must has valid values.
     */
    private void adjustGift(final String fromAgent
            , final String toAgent
            , final Gift gift
            , final ArrayList<Misc.KeyValue> keyValues
            , final Handler<JsonObject> callback) {

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, fromAgent, "adjustGift");
        if (!gift.owner.equals(fromAgent)) {
            log.add("giftId", gift.getModelId());
            log.add("owner", gift.owner);

            callback.handle(new JsonObject()
                            .putNumber("error", GiftError.NOT_OWNED)
                            .putString("desc", fromAgent + " is not owned")
            );
            log.add("********", fromAgent + " not own this gift");
            log.writeLog();
            return;
        }

        //use adjust core function to transfer gift.amount fromAgent --> toAgent
        adjustGiftValue(fromAgent, toAgent, gift.amount, keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                final int error = result.getInteger("error");
                final long tranId = result.getLong("tranId");

                if (error == 0) {

                    gift.owner = toAgent;
                    gift.status = Gift.STATUS_NEW;
                    gift.oldOwner = fromAgent;
                    gift.note = "receive gift from " + fromAgent;

                    giftDb.update(gift, false, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                            GiftHistory history = new GiftHistory(gift.getModelId(), fromAgent, toAgent, tranId);
                            history.note = "adjustGift";
                            giftHistoryDb.save(history, null);

                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", 0)
                                            .putString("desc", "success")
                                            .putNumber("tranId", tranId)
                                            .putObject("gift", gift.toJsonObject())
                            );
                        }
                    });
                    return;
                }

                callback.handle(new JsonObject()
                                .putNumber("error", error)
                                .putNumber("tranId", tranId)
                );
            }
        });
    }

    private void adjustGiftWhenExpired(final String fromAgent
            , final String toAgent
            , final Gift gift
            , final ArrayList<Misc.KeyValue> keyValues
            , final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger, fromAgent, "adjustGift");
        if (!gift.owner.equals(fromAgent)) {
            log.add("giftId", gift.getModelId());
            log.add("owner", gift.owner);

            callback.handle(new JsonObject()
                            .putNumber("error", GiftError.NOT_OWNED)
                            .putString("desc", fromAgent + " is not owned")
            );
            log.add("********", fromAgent + " not own this gift");
            log.writeLog();
            return;
        }

        adjustGiftValue(fromAgent
                , toAgent
                , gift.amount
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                final int error = result.getInteger("error", -1);
                final long tranId = result.getLong("tranId", System.currentTimeMillis());

                if (error == 0) {

                    gift.oldOwner = fromAgent;
                    gift.owner = toAgent;
                    gift.status = Gift.STATUS_EXPIRED;
                    gift.note = "expired";
                    gift.tranId = tranId;
                    gift.tranDate = System.currentTimeMillis();

                    giftDb.update(gift.getModelId(), gift.getPersisFields(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                            GiftHistory history = new GiftHistory(gift.getModelId(), fromAgent, toAgent, tranId);
                            history.note = "adjustGift by expired";
                            giftHistoryDb.save(history, null);

                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", 0)
                                            .putString("desc", "success")
                                            .putNumber("tranId", tranId)
                                            .putObject("gift", gift.toJsonObject())
                            );

                            //remove from queued
                            queuedGiftDb.removeGift(gift.getModelId(), new Handler<Integer>() {
                                @Override
                                public void handle(Integer integer) {
                                    log.add("number of gift has been removed from queued " + integer, "---");
                                    log.writeLog();
                                }
                            });
                        }
                    });
                    return;
                }
                else if(error == 1001)
                {
                    gift.oldOwner = fromAgent;
                    gift.owner = toAgent;
                    gift.status = Gift.STATUS_COMPLETE;
                    gift.note = "adjustGift by 1001";
                    gift.tranId = tranId;
                    gift.tranDate = System.currentTimeMillis();

                    giftDb.update(gift.getModelId(), gift.getPersisFields(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {

                            GiftHistory history = new GiftHistory(gift.getModelId(), fromAgent, toAgent, tranId);
                            history.note = "adjustGift by 1001";
                            giftHistoryDb.save(history, null);

                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", 1001)
                                            .putString("desc", "fail")
                            );

                            //remove from queued
                            queuedGiftDb.removeGift(gift.getModelId(), new Handler<Integer>() {
                                @Override
                                public void handle(Integer integer) {
                                    log.add("number of gift has been removed from queued " + integer, "---");
                                    log.writeLog();
                                }
                            });
                        }
                    });
                    return;
                }

                callback.handle(new JsonObject()
                                .putNumber("error", error)
                                .putNumber("tranId", tranId)
                );
            }
        });
    }

    public void getAllTranType(final Handler<JsonArray> callback) {
        serviceDb.getlist(false, null, null, new Handler<ArrayList<ServiceDb.Obj>>() {
            @Override
            public void handle(ArrayList<ServiceDb.Obj> service) {
                JsonArray arr = new JsonArray();
                for (ServiceDb.Obj obj : service) {
                    arr.add(new JsonObject()
                                    .putString("tranTypeId", obj.serviceID)
                                    .putString("tranTypeName", obj.serviceName)
                    );
                }
                arr.add(new JsonObject()
                        .putString("tranTypeId", "topup")
                        .putString("tranTypeName", "*Nạp tiền điện thoại"));
                arr.add(new JsonObject()
                        .putString("tranTypeId", "123phim")
                        .putString("tranTypeName", "*123 phim"));
                callback.handle(arr);
            }
        });
    }

    public void changeGiftOwner(final String giftId
            , final String owner
            , final String newOwner
            , final long tranId
            , final Handler<JsonObject> callback) {

        Gift filter = new Gift();
        filter.setModelId(giftId);

        filter.owner = owner;
        giftDb.findOne(filter, new Handler<Gift>() {
            @Override
            public void handle(final Gift gift) {
                if (gift == null) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", GiftError.NOT_OWNED)
                                    .putString("desc", owner + " is not owner")
                                    .putObject("gift", gift.toJsonObject())
                    );
                    return;
                }
                gift.oldOwner = gift.owner;
                gift.status = Gift.STATUS_COMPLETE;
                gift.note = "complete";
                gift.owner = newOwner;
                gift.tranId = tranId;
                gift.tranDate = System.currentTimeMillis();
                giftDb.update(gift, false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
                        callback.handle(
                                new JsonObject()
                                        .putNumber("error", 0)
                                        .putObject("gift", gift.toJsonObject())
                        );
                        GiftHistory history = new GiftHistory(giftId, owner, newOwner, tranId);
                        history.note = "changeGiftOwner";
                        giftHistoryDb.save(history, null);
                    }
                });
            }
        });
    }

    private void voucherToPoint(final String agent, final String pin, final Gift gift, final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, agent, "voucherToPoint");

        CoreCommon.voucherToPoint(vertx, logger, agent, pin, gift.amount, DEFAULT_CORE_TIMEOUT, new Handler<Response>() {
            @Override
            public void handle(Response reply) {
                int error = reply.Error;
                long tranId = reply.Tid;

                if (error == 0) {
                    gift.owner = "0";
                    gift.status = Gift.STATUS_NEW;
                    gift.tranId = tranId;
                    gift.tranDate = System.currentTimeMillis();
                    giftDb.update(gift, false, null);

                    GiftHistory history = new GiftHistory(gift.getModelId(), agent, "0", tranId);
                    history.note = "giftToPoin";
                    giftHistoryDb.save(history, null);
                }

                callback.handle(new JsonObject()
                                .putNumber("error", error)
                                .putNumber("tranId", tranId)
                );
            }
        });
    }

    private void momoToVoucher(final String agent
            , final String pin
            , final long amount
            , final GiftType giftType
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, agent, "momoToVoucher");

        CoreCommon.momoToVoucher(vertx, logger, agent, pin, amount, DEFAULT_CORE_TIMEOUT, new Handler<Response>() {
            @Override
            public void handle(final Response coreReply) {
                int error = coreReply.Error;
                if (error == 0) {
                    final Gift gift = new Gift();
                    //gift.code = ;
                    gift.typeId = giftType.getModelId();
                    gift.amount = amount;
                    gift.startDate = new Date();

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.YEAR, 1);

                    gift.endDate = calendar.getTime();
                    gift.modifyDate = new Date();
                    gift.owner = agent;
                    gift.status = Gift.STATUS_NEW;
                    gift.lock = false;
                    giftDb.save(gift, new Handler<String>() {
                        @Override
                        public void handle(String giftId) {
                            gift.setModelId(giftId);
                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", 0)
                                            .putNumber("tranId", coreReply.Tid)
                                            .putObject("gift", gift.toJsonObject())
                            );
                            log.add("error", 0);
                            log.writeLog();

                            GiftHistory history = new GiftHistory(giftId, "0", agent, coreReply.Tid);
                            history.note = "momoToGift";
                            giftHistoryDb.save(history, null);
                        }
                    });
                    return;
                }

                log.add("error", coreReply.Error);
                log.writeLog();

                callback.handle(new JsonObject()
                                .putNumber("error", error)
                                .putNumber("tranId", coreReply.Tid)
                );
            }
        });
    }

    public void createLocalGift(final String ownerAgent
            , final long amount
            , final GiftType giftType
            , final long tId
            , final String sourceFrom
            , final int duration
            , final ArrayList<Misc.KeyValue> listKeysForGift
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, ownerAgent, sourceFrom);

        final Gift gift = new Gift();
        //gift.code = ;
        gift.typeId = giftType.getModelId();
        gift.amount = amount;
        gift.startDate = new Date();

        /*Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);*/

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, duration);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        gift.endDate = calendar.getTime();

        //gift.endDate = calendar.getTime();
        gift.modifyDate = new Date();
        gift.owner = ownerAgent;
        gift.status = Gift.STATUS_NEW;
        gift.lock = false;

        gift.getExtra().putString("creator", "sys");

        if (listKeysForGift != null && listKeysForGift.size() > 0) {
            for (int i = 0; i < listKeysForGift.size(); i++) {
                Misc.KeyValue kv = listKeysForGift.get(i);
                gift.getExtra().putString(kv.Key, kv.Value);
            }
        }

        giftDb.save(gift, new Handler<String>() {
            @Override
            public void handle(String giftId) {
                gift.setModelId(giftId);
                callback.handle(
                        new JsonObject()
                                .putNumber("error", 0)
                                .putNumber("tranId", tId)
                                .putObject("gift", gift.toJsonObject())
                );
                log.add("error", 0);
                log.writeLog();

                GiftHistory history = new GiftHistory(giftId, "system", ownerAgent, tId);
                history.note = "adjust_" + sourceFrom;
                giftHistoryDb.save(history, null);
            }
        });
    }

    public void createLocalGiftForBillPayPromo(final String ownerAgent
            , final long amount
            , final GiftType giftType
            , final long tId
            , final String sourceFrom
            , final long startDate
            , final int duration
            , final ArrayList<Misc.KeyValue> listKeysForGift
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, ownerAgent, sourceFrom);

        final Gift gift = new Gift();
        //gift.code = ;
        gift.typeId = giftType.getModelId();
        gift.amount = amount;
        gift.startDate = new Date(startDate);

        /*Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);*/

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, duration);
        gift.endDate = calendar.getTime();

        //gift.endDate = calendar.getTime();
        gift.modifyDate = new Date();
        gift.owner = ownerAgent;
        gift.status = Gift.STATUS_NEW;
        gift.lock = false;

        gift.getExtra().putString("creator", "sys");

        if (listKeysForGift != null && listKeysForGift.size() > 0) {
            for (int i = 0; i < listKeysForGift.size(); i++) {
                Misc.KeyValue kv = listKeysForGift.get(i);
                gift.getExtra().putString(kv.Key, kv.Value);
            }
        }

        giftDb.save(gift, new Handler<String>() {
            @Override
            public void handle(String giftId) {
                gift.setModelId(giftId);
                callback.handle(
                        new JsonObject()
                                .putNumber("error", 0)
                                .putNumber("tranId", tId)
                                .putObject("gift", gift.toJsonObject())
                );
                log.add("error", 0);
                log.writeLog();

                GiftHistory history = new GiftHistory(giftId, "system", ownerAgent, tId);
                history.note = "adjust_" + sourceFrom;
                giftHistoryDb.save(history, null);
            }
        });
    }

    public void createLocalGiftForBillPayPromoWithDetailGift(final String ownerAgent
            , final long amount
            , final GiftType giftType
            , final long tId
            , final String sourceFrom
            , final long modifyDate
            , final int duration
            , final ArrayList<Misc.KeyValue> listKeysForGift
            , final String giftInfoDetail
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, ownerAgent, sourceFrom);

        final Gift gift = new Gift();
        //gift.code = ;
        gift.typeId = giftType.getModelId();

        gift.amount = amount;
        gift.startDate = new Date();

        /*Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);*/

        Calendar calendar = Calendar.getInstance();
        long giftTime = 1000L * 60 * 60 * 24 * duration + System.currentTimeMillis();
        calendar.setTimeInMillis(giftTime);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.setTimeInMillis(giftTime);
        gift.endDate = calendar.getTime();
        gift.giftInfoDetail = giftInfoDetail;
        //gift.endDate = calendar.getTime();
        gift.modifyDate = new Date();
        gift.owner = ownerAgent;
        gift.status = Gift.STATUS_NEW;
        gift.lock = false;

        gift.getExtra().putString("creator", "sys");

        if (listKeysForGift != null && listKeysForGift.size() > 0) {
            for (int i = 0; i < listKeysForGift.size(); i++) {
                Misc.KeyValue kv = listKeysForGift.get(i);
                gift.getExtra().putString(kv.Key, kv.Value);
            }
        }

        giftDb.save(gift, new Handler<String>() {
            @Override
            public void handle(String giftId) {
                gift.setModelId(giftId);
                callback.handle(
                        new JsonObject()
                                .putNumber("error", 0)
                                .putNumber("tranId", tId)
                                .putObject("gift", gift.toJsonObject())
                );
                log.add("error", 0);
                log.writeLog();

                GiftHistory history = new GiftHistory(giftId, "system", ownerAgent, tId);
                history.note = "adjust_" + sourceFrom;
                giftHistoryDb.save(history, null);
            }
        });
    }

    public void createLocalGiftForBillPayPromoWithDetailGiftWithLock(final String ownerAgent
            , final long amount
            , final GiftType giftType
            , final long tId
            , final String sourceFrom
            , final long modifyDate
            , final int duration
            , final ArrayList<Misc.KeyValue> listKeysForGift
            , final String giftInfoDetail
            , final boolean isLock
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, ownerAgent, sourceFrom);

        final Gift gift = new Gift();
        //gift.code = ;
        gift.typeId = giftType.getModelId();

        gift.amount = amount;
        gift.startDate = new Date();

        /*Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);*/

        Calendar calendar = Calendar.getInstance();
        long giftTime = 1000L * 60 * 60 * 24 * duration + System.currentTimeMillis();
        calendar.setTimeInMillis(giftTime);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.setTimeInMillis(giftTime);
        gift.endDate = calendar.getTime();
        gift.giftInfoDetail = giftInfoDetail;
        //gift.endDate = calendar.getTime();
        gift.modifyDate = new Date();
        gift.owner = ownerAgent;
        gift.status = Gift.STATUS_NEW;
        gift.lock = isLock;

        gift.getExtra().putString("creator", "sys");

        if (listKeysForGift != null && listKeysForGift.size() > 0) {
            for (int i = 0; i < listKeysForGift.size(); i++) {
                Misc.KeyValue kv = listKeysForGift.get(i);
                gift.getExtra().putString(kv.Key, kv.Value);
            }
        }

        giftDb.save(gift, new Handler<String>() {
            @Override
            public void handle(String giftId) {
                gift.setModelId(giftId);
                callback.handle(
                        new JsonObject()
                                .putNumber("error", 0)
                                .putNumber("tranId", tId)
                                .putObject("gift", gift.toJsonObject())
                );
                log.add("error", 0);
                log.writeLog();

                GiftHistory history = new GiftHistory(giftId, "system", ownerAgent, tId);
                history.note = "adjust_" + sourceFrom;
                giftHistoryDb.save(history, null);
            }
        });
    }

    public void createGift(final String agent
            , final String pin
            , final long amount
            , final String giftTypeId
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, agent, "createGift");

        log.add("amount", amount);
        log.add("giftTypeId", giftTypeId);

        if (amount < MIN_GIFT_PRICE || amount > MAX_GIFT_PRICE) {
            callback.handle(new JsonObject()
                    .putNumber("error", GiftError.WRONG_PRICE_AMOUNT)
                    .putString("desc", "Gift price must be greater than " + MIN_GIFT_PRICE + " and less than " + MAX_GIFT_PRICE));
            log.add("ERROR", "Gift price must be greater than " + MIN_GIFT_PRICE + " and less than " + MAX_GIFT_PRICE);
            log.writeLog();
            return;
        }

        GiftType filter = new GiftType();
        filter.setModelId(giftTypeId);

        giftTypeDb.findOne(filter, new Handler<GiftType>() {
            @Override
            public void handle(final GiftType giftType) {
                if (giftType == null) {
                    callback.handle(new JsonObject()
                            .putNumber("error", GiftError.NO_SUCH_GIFT)
                            .putString("desc", "No such giftTypeId"));
                    log.add("ERROR", "No such giftTypeId");
                    log.writeLog();
                    return;
                }

                if (giftType.status.intValue() != GiftType.STATUS_ACTIVE.intValue()) {
                    callback.handle(new JsonObject()
                            .putNumber("error", GiftError.GIFT_TYPE_NOT_ACTIVE)
                            .putString("desc", "GiftTypeId is not ACTIVE"));
                    log.add("ERROR", "GiftTypeId is not ACTIVE");
                    log.writeLog();
                    return;
                }

                momoToVoucher(agent, pin, amount, giftType, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        result.putObject("giftType", giftType.toJsonObject());
                        callback.handle(result);
                    }
                });

            }
        });
    }

    public void getAllGift(int phone, final Handler<List<Gift>> handler) {
        final Gift gift = new Gift();
        gift.owner = "0" + phone;
        giftDb.find(gift, 1000, new Handler<List<Gift>>() {
            @Override
            public void handle(List<Gift> result) {
                handler.handle(result);
            }
        });
    }

    public void getGift(String giftId, final Handler<Gift> callback) {
        final Gift gift = new Gift();
        gift.setModelId(giftId);
        giftDb.findOne(gift, new Handler<Gift>() {
            @Override
            public void handle(Gift result) {
                callback.handle(result);
            }
        });
    }

    public void getAgentGift(final String owner, String giftId, final Handler<Gift> callback) {
        final Gift gift = new Gift();
        gift.setModelId(giftId);
        gift.owner = owner;
        giftDb.findOne(gift, new Handler<Gift>() {
            @Override
            public void handle(Gift result) {
                callback.handle(result);
            }
        });
    }

    public void getGiftType(final Gift gift, final Handler<GiftType> callback) {
        if (gift == null) {
            callback.handle(null);
            return;
        }
        final GiftType filter = new GiftType();
        filter.setModelId(gift.typeId);
        giftTypeDb.findOne(filter, new Handler<GiftType>() {
            @Override
            public void handle(GiftType giftType) {
                callback.handle(giftType);
            }
        });
    }

    public void transferGift(final String fromAgent
            , final String giftId
            , final String toAgent
            , final ArrayList<Misc.KeyValue> keyValues
            , final Handler<JsonObject> callback) {

        if (fromAgent == null || giftId == null || toAgent == null) {
            callback.handle(new JsonObject().putNumber("error", GiftError.SYSTEM_ERROR));
            throw new IllegalArgumentException("fromAgent & giftId & toAgent can't be null!");
        }

        final Gift filter = new Gift();
        filter.setModelId(giftId);
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, fromAgent, "transferGift");

        if (fromAgent.equals(toAgent)) {
            callback.handle(
                    new JsonObject()
                            .putNumber("error", GiftError.ALREADY_OWNED)
                            .putString("desc", "fromAgent and toAgent can't be equal.")
            );

            log.add("error", "fromAgent and toAgent can't be equal.");
            log.writeLog();
            return;
        }

        giftDb.findOne(filter, new Handler<Gift>() {
            @Override
            public void handle(final Gift gift) {
                if (gift == null) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", GiftError.NO_SUCH_GIFT)
                                    .putString("desc", "No such giftId")
                    );

                    log.add("error", "No such giftId.");
                    log.writeLog();

                    return;
                }
                if (!fromAgent.equals(gift.owner)) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", GiftError.NOT_OWNED)
                                    .putString("desc", fromAgent + " is not owner")
                                    .putObject("gift", gift.toJsonObject())
                    );

                    log.add("error", fromAgent + " is not owner");
                    log.writeLog();

                    return;
                }

                GiftType giftTypeFilter = new GiftType();
                giftTypeFilter.setModelId(gift.typeId);
                giftTypeDb.findOne(giftTypeFilter, new Handler<GiftType>() {
                    @Override
                    public void handle(final GiftType giftType) {
                        if (giftType == null) {
                            callback.handle(new JsonObject()
                                            .putNumber("error", GiftError.SYSTEM_ERROR)
                                            .putString("desc", "Gift has an error gifType")
                                            .putObject("gift", gift.toJsonObject())
                            );
                            return;
                        }
                        if (!giftType.transfer) {
                            callback.handle(new JsonObject()
                                            .putNumber("error", GiftError.NOT_TRANSFERABLE)
                                            .putString("desc", "Gift is not allowed to transfer.")
                                            .putObject("gift", gift.toJsonObject())
                                            .putObject("giftType", giftType.toJsonObject())
                            );

                            log.add("error", "Gift isn't transferable!");
                            log.writeLog();

                            return;
                        }


                        //transfer gift with amount fromAgent --> toAgent
                        adjustGift(fromAgent, toAgent, gift, keyValues, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject result) {
                                final int error = result.getInteger("error");
                                final long tranId = result.getLong("tranId");
                                callback.handle(new JsonObject()
                                                .putNumber("error", error)
                                                .putNumber("tranId", tranId)
                                                .putObject("gift", gift.toJsonObject())
                                                .putObject("giftType", giftType.toJsonObject())
                                );
                            }
                        });
                    }
                });
            }
        });
    }

    public void transferGiftWhenExpired(final Gift gift
            , final String toAgent
            , final ArrayList<Misc.KeyValue> keyValues
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final Handler<JsonObject> callback) {

        if (gift.owner == null || gift.getModelId() == null || toAgent == null) {
            callback.handle(new JsonObject().putNumber("error", GiftError.SYSTEM_ERROR));
            throw new IllegalArgumentException("owner & giftId & toAgent can't be null!");
        }

        log.add("func", "transferGiftWhenExpired");
        log.add("fromAgent", gift.owner == null ? "" : gift.owner);
        log.add("toAgent", toAgent);
        log.add("amount", gift.amount);

        log.add("start adjust in core", "-------------");

        adjustGiftWhenExpired(gift.owner
                , toAgent
                , gift
                , keyValues, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                final int error = result.getInteger("error", -1);
                final long tranId = result.getLong("tranId", System.currentTimeMillis());
                callback.handle(new JsonObject()
                                .putNumber("error", error)
                                .putNumber("tranId", tranId)
                                .putObject("gift", gift.toJsonObject())
                );
            }
        });
    }

    public void getGiftType(long time, final Handler<List<GiftType>> callback) {
        giftTypeDb.findWithTime(time, new Handler<List<GiftType>>() {
            @Override
            public void handle(List<GiftType> types) {
                callback.handle(types);
            }
        });
    }

    public void getGetGiftType(String giftTypeId, final Handler<GiftType> callback) {
        GiftType filter = new GiftType();
        filter.setModelId(giftTypeId);
        giftTypeDb.findOne(filter, new Handler<GiftType>() {
            @Override
            public void handle(GiftType result) {
                callback.handle(result);
            }
        });
    }

    public void giftToPoint(final String agent, final String pin, final String giftId, final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, agent, "giftToPoint");

        Gift filter = new Gift();
        filter.setModelId(giftId);
        giftDb.findOne(filter, new Handler<Gift>() {
            @Override
            public void handle(final Gift gift) {
                if (gift == null) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", GiftError.NO_SUCH_GIFT)
                                    .putString("desc", "No such giftId")
                    );
                    return;
                }
                if (!gift.owner.equals(agent)) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", GiftError.NOT_OWNED)
                                    .putString("desc", agent + " is not owner")
                    );
                    return;
                }

                voucherToPoint(agent, pin, gift, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        result.putObject("gift", gift.toJsonObject());
                        callback.handle(result);
                    }
                });
            }
        });
    }

    public void transferG2n(final String fromAgent
            , final String fromAgentName
            , final String pin
            , final String toAgent
            , final Gift gift
            , final String comment
            , final ArrayList<Misc.KeyValue> keyValues
            , final Handler<JsonObject> callback) {
        Request lockObj = new Request();

        lockObj.TYPE = Command.TRANSFER_WITH_LOCK;
        lockObj.SENDER_PIN = pin;
        lockObj.TRAN_AMOUNT = gift.amount;
        lockObj.SENDER_NUM = fromAgent;
        lockObj.RECVER_NUM = toAgent;
        lockObj.TARGET = G2N_ACCOUNT;
        lockObj.WALLET = WalletType.VOUCHER;
        lockObj.KeyValueList = new ArrayList<>();
        lockObj.KeyValueList.add(new KeyValue("m2mtype", "m2n"));
        lockObj.KeyValueList.add(new KeyValue(Const.CoreVC.Recipient, toAgent));
        lockObj.PHONE_NUMBER = fromAgent;
        lockObj.TIME = new Date().getTime();

        if (keyValues != null) {
            for (int i = 0; i < keyValues.size(); i++) {
                Misc.KeyValue kv = keyValues.get(i);
                lockObj.KeyValueList.add(new KeyValue(kv.Key, kv.Value));
            }
        }

        final JsonObject lockJo = lockObj.toJsonObject();

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, fromAgent, "transferG2n");
        log.add("Thuc hien treo tien", G2N_ACCOUNT);
        log.writeLog();

        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, lockJo, new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                Buffer buf = message.body();
                final MomoMessage replyMessage = MomoMessage.fromBuffer(buf);
                Core.StandardReply rpl;
                try {
                    rpl = Core.StandardReply.parseFrom(replyMessage.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                final int error;
                final long tranId;
                if (rpl == null) {
                    error = -10000;
                    tranId = -10000;
                } else {
                    error = rpl.getErrorCode();
                    tranId = rpl.getTid();
                }

                log.add("TreoTien tranId", tranId);
                log.add("TreoTien error", error);
                log.writeLog();

                if (error == 0) {
                    final GiftToNumber g2n = new GiftToNumber(fromAgent
                            , toAgent
                            , comment
                            , tranId
                            , gift.getModelId()
                            , gift.typeId
                            , fromAgentName);

                    giftToNumberDb.save(g2n, new Handler<String>() {
                        @Override
                        public void handle(String id) {

                            GiftHistory history = new GiftHistory(gift.getModelId(), gift.owner, g2n.toAgent, tranId);
                            history.middleAgent = G2N_ACCOUNT;
                            history.note = "submit";
                            giftHistoryDb.save(history, null);

                            gift.oldOwner = gift.owner;
                            gift.note = "gift to number, wait receive";
                            gift.owner = G2N_ACCOUNT;
                            gift.status = Gift.STATUS_GIFT_TO_NUMBER;

                            giftDb.update(gift, false, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean result) {
                                    callback.handle(new JsonObject()
                                                    .putNumber("error", error)
                                                    .putNumber("tranId", tranId)
                                                    .putObject("giftToNumber", g2n.toJsonObject())
                                                    .putObject("gift", gift.toJsonObject())
                                    );
                                }
                            });
                        }
                    });
                    return;
                }
                callback.handle(new JsonObject()
                                .putNumber("error", error)
                                .putNumber("tranId", tranId)
                );

            }
        });
    }


    public void transferGifDirectOrG2n(final String fromAgent
            , final String fromAgentName
            , final String pin
            , final String giftId
            , final int toPhone
            , final String comment
            , final ArrayList<Misc.KeyValue> keyValues
            , final Handler<JsonObject> callback) {
        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, fromAgent);

        Misc.getAgentStatus(vertx, toPhone, log, phonesDb, new Handler<SoapVerticle.ObjCoreStatus>() {
            @Override
            public void handle(SoapVerticle.ObjCoreStatus objCoreStatus) {
                //registered
                if (objCoreStatus.isReged) {
                    log.add("tranferType", "directly");
                    log.writeLog();
                    transferGift(fromAgent
                            , giftId
                            , "0" + toPhone, keyValues, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject result) {
                            result.putString("tranType", TRANTYPE_TRANSFER);
                            callback.handle(result);
                        }
                    });
                    return;
                }

                log.add("tranferType", "G2N");
                log.writeLog();

                Gift filter = new Gift();
                filter.setModelId(giftId);

                giftDb.findOne(filter, new Handler<Gift>() {
                    @Override
                    public void handle(final Gift gift) {
                        if (gift == null) {
                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", GiftError.NO_SUCH_GIFT)
                                            .putString("desc", "No such giftId")
                            );
                            return;
                        }
                        if (!gift.owner.equals(fromAgent)) {
                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", GiftError.NOT_OWNED)
                                            .putString("desc", fromAgent + " is not owner")
                            );
                            return;
                        }

                        transferG2n(fromAgent, fromAgentName, pin, "0" + toPhone, gift, comment, keyValues, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject result) {
                                result.putString("tranType", TRANTYPE_G2N);
                                callback.handle(result);
                            }
                        });

                    }
                });
            }
        });
    }

    public void commitG2nTransaction(final String receiverAgent, final Handler<List<GiftToNumber>> callback) {
        final List<GiftToNumber> callbackResult = new ArrayList<>();

        GiftToNumber filter = new GiftToNumber();
        filter.toAgent = receiverAgent;
        filter.status = GiftToNumber.STATUS_NEW;


        giftToNumberDb.find(filter, 100, new Handler<List<GiftToNumber>>() {
            @Override
            public void handle(final List<GiftToNumber> giftToCommit) {
                if (giftToCommit == null || giftToCommit.size() == 0) {
                    callback.handle(callbackResult);
                    return;
                }

                for (GiftToNumber giftToNumber : giftToCommit) {

                    final GiftToNumber g2n = giftToNumber;

                    //commit this transaction in core
                    final Request commitObj = new Request();
                    commitObj.TYPE = Command.COMMIT;
                    commitObj.TRAN_ID = g2n.tranId;
                    commitObj.WALLET = WalletType.VOUCHER;
                    commitObj.PHONE_NUMBER = receiverAgent;
                    commitObj.TIME = new Date().getTime();

                    final JsonObject commitJo = commitObj.toJsonObject();

                    vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, commitJo, new Handler<Message<Buffer>>() {
                        @Override
                        public void handle(Message<Buffer> message) {
                            CoreMessage momoMessage = CoreMessage.fromBuffer(message.body());
                            Core.StandardReply reply;
                            try {
                                reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
                            } catch (Exception ex) {
                                reply = null;
                            }

                            Response crObj = Misc.getCoreReplyObj(reply);

                            g2n.status = GiftToNumber.STATUS_COMMITED;
                            g2n.tranError = reply.getErrorCode();
                            giftToNumberDb.update(g2n, false, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean event) {
                                    callbackResult.add(g2n);
                                    if (callbackResult.size() >= giftToCommit.size())
                                        callback.handle(callbackResult);
                                }
                            });

                            if (crObj.Error == 0) {

                                GiftHistory history = new GiftHistory(g2n.giftId, g2n.fromAgent, g2n.toAgent, g2n.tranId);
                                history.middleAgent = G2N_ACCOUNT;
                                history.note = "commit";
                                giftHistoryDb.save(history, null);

                                giftDb.findOne(g2n.giftId, new Handler<Gift>() {
                                    @Override
                                    public void handle(Gift gift) {
                                        if (gift != null) {
                                            gift.note = "receive gift from " + (gift.oldOwner == null ? "other" : gift.oldOwner);
                                            gift.oldOwner = gift.owner;
                                            gift.owner = receiverAgent;
                                            gift.status = Gift.STATUS_NEW;

                                        } else {
                                            gift = new Gift(g2n.giftId);
                                            gift.owner = receiverAgent;
                                            gift.status = Gift.STATUS_NEW;
                                            gift.note = "";
                                        }

                                        giftDb.update(gift, false, null);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    public void getGn2TransactionInfo(GiftToNumber filter, final Handler<JsonObject> callback) {
        final JsonObject result = new JsonObject();
        giftToNumberDb.findOne(filter, new Handler<GiftToNumber>() {
            @Override
            public void handle(GiftToNumber g2n) {
                if (g2n == null) {
                    callback.handle(result.putNumber("error", 1).putString("desc", "Not found"));
                    return;
                }

                result.putObject("giftToNumber", g2n.toJsonObject());

                Gift giftFilter = new Gift(g2n.giftId);
                giftDb.findOne(giftFilter, new Handler<Gift>() {
                    @Override
                    public void handle(Gift gift) {
                        if (gift == null) {
                            callback.handle(
                                    result.putNumber("error", 2)
                                            .putString("desc", "System error: gift id not found")
                            );
                            return;
                        }

                        result.putObject("gift", gift.toJsonObject());

                        GiftType giftTypeFilter = new GiftType(gift.typeId);
                        giftTypeDb.findOne(giftTypeFilter, new Handler<GiftType>() {
                            @Override
                            public void handle(GiftType giftType) {
                                if (giftType == null) {
                                    callback.handle(result.putNumber("error", 3).putString("desc", "System error: giftType id not found"));
                                    return;
                                }
                                result.putObject("giftType", giftType.toJsonObject());
                                callback.handle(result.putNumber("error", 0));
                            }
                        });
                    }
                });
            }
        });
    }

    public void giftToNumberRolback(final GiftToNumber g2n, final Handler<JsonObject> callback) {
        final JsonObject result = new JsonObject();
        if (g2n.tranError != null && g2n.tranError != 0) {
            return;
        }

        final int fromPhone = DataUtil.strToInt(g2n.fromAgent);

        Gift giftFilter = new Gift(g2n.giftId);
        giftDb.findOne(giftFilter, new Handler<Gift>() {
            @Override
            public void handle(final Gift gift) {
                if (gift == null) {
                    callback.handle(
                            result.putNumber("error", -1)
                                    .putString("desc", "System error: gift id not found")
                    );
                    return;
                }

                if (!gift.owner.equals(G2N_ACCOUNT)) {
                    callback.handle(
                            result.putNumber("error", -2)
                                    .putString("desc", "This gift is not in G2n Transactgion")
                    );
                    return;
                }

                Request rbObj = new Request();
                rbObj.TYPE = Command.ROLLBACK;
                rbObj.TRAN_ID = g2n.tranId;
                rbObj.PHONE_NUMBER = "0" + fromPhone;
                rbObj.TIME = System.currentTimeMillis();

                vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS
                        , rbObj.toJsonObject(), new Handler<Message<Buffer>>() {
                    @Override
                    public void handle(Message<Buffer> message) {
                        Buffer buf = message.body();
                        final MomoMessage reply = MomoMessage.fromBuffer(buf);
                        Core.StandardReply rpl;
                        try {
                            rpl = Core.StandardReply.parseFrom(reply.cmdBody);
                        } catch (Exception ex) {
                            rpl = null;
                        }

                        int error = rpl.getErrorCode();

                        g2n.tranError = error;
                        g2n.status = GiftToNumber.STATUS_ROLLBACK;
                        g2n.endDate = System.currentTimeMillis();

                        if (error != 0) {
                            g2n.status = GiftToNumber.STATUS_ROLLBACK_ERROR;
                        }

                        giftToNumberDb.update(g2n, false, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean event) {
                                result.putObject("gift", gift.toJsonObject());
                                gift.owner = g2n.fromAgent;
                                gift.status = Gift.STATUS_NEW;
                                giftDb.update(gift, false, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean event) {
                                        callback.handle(result.putNumber("error", 0));
                                    }
                                });

                                GiftHistory history = new GiftHistory(g2n.giftId, g2n.fromAgent, G2N_ACCOUNT, g2n.tranId);
                                history.note = "rollback";
                                giftHistoryDb.save(history, null);
                            }
                        });
                    }
                });

            }
        });
    }

    public void getGiftInfo(String giftId, final Handler<JsonObject> callback) {
        Gift filter = new Gift(giftId);
        final JsonObject reply = new JsonObject();
        giftDb.findOne(filter, new Handler<Gift>() {
            @Override
            public void handle(Gift gift) {
                if (gift == null) {
                    reply.putNumber("error", GiftError.NO_SUCH_GIFT);
                    reply.putString("desc", GiftError.getDesc(GiftError.NO_SUCH_GIFT));
                    callback.handle(reply);
                    return;
                }
                reply.putObject("gift", gift.toJsonObject());
                GiftType giftTypeFilter = new GiftType(gift.typeId);
                giftTypeDb.findOne(giftTypeFilter, new Handler<GiftType>() {
                    @Override
                    public void handle(GiftType giftType) {
                        if (giftType == null) {
                            reply.putNumber("error", GiftError.NO_SUCH_GIFTYPE);
                            reply.putString("desc", "NO_SUCH_GIFTTYPE");
                            callback.handle(reply);
                            return;
                        }
                        reply.putNumber("error", 0);
                        reply.putObject("giftType", giftType.toJsonObject());
                        callback.handle(reply);
                    }
                });
            }
        });
    }

    public void useGift(final String agent, final String giftId, final Handler<JsonObject> callback) {
        giftDb.lock(agent, giftId, new Handler<Boolean>() {
            @Override
            public void handle(Boolean locked) {
                if (!locked) {
                    callback.handle(new JsonObject()
                                    .putNumber("error", MomoProto.TranHisV1.ResultCode.NO_SUCH_GIFT_VALUE)
                                    .putString("desc", "Can't lock the gift")
                    );
                    return;
                }
                getGiftInfo(giftId, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject result) {
                        long currentTime = System.currentTimeMillis();
                        final Gift gift = new Gift(result.getObject("gift"));
                        final GiftType giftType = new GiftType(result.getObject("giftType"));
                        if (gift == null || giftType == null) {
                            giftDb.unlock(agent, giftId, null);
                            callback.handle(new JsonObject()
                                            .putNumber("error", MomoProto.TranHisV1.ResultCode.NO_SUCH_GIFT_VALUE)
                                            .putString("desc", "No such gift!")
                            );
                            return;
                        }
                        long activeTime = currentTime;
                        if (gift.getExtra().containsField(BillPayPromoConst.ACTIVE_TIME)) {
                            activeTime = DataUtil.strToLong(gift.getExtra().getString(BillPayPromoConst.ACTIVE_TIME, currentTime + ""));
                        }

                        if (currentTime < activeTime) {
                            giftDb.unlock(agent, giftId, null);
//                            String startDate = Misc.dateVNFormatWithDot(currentTime);
                            JsonObject jsonObjectExtra = gift.extra;

                            long startDateLong = gift.startDate.getTime() + 20 * 24 * 60 * 60 * 1000L;

                            String startDate = Misc.dateVNFormatWithDot(startDateLong);
                            if (jsonObjectExtra.containsField(BillPayPromoConst.ACTIVE_TIME)) {
                                String longTime = jsonObjectExtra.getString(BillPayPromoConst.ACTIVE_TIME);
                                startDate = Misc.dateVNFormatWithDot(Long.parseLong(longTime));
                            }

                            callback.handle(new JsonObject()
                                            .putNumber("error", GiftError.ACTIVATED_TIME_ERROR)
                                            .putString("desc", String.format(PromoContentNotification.BILL_PAY_PROMO_CAN_NOT_ACTIVE_GIFT, startDate))
                            );
                            return;
                        } else if (currentTime < gift.startDate.getTime()) {
                            giftDb.unlock(agent, giftId, null);
//                            String startDate = Misc.dateVNFormatWithDot(currentTime);
                            JsonObject jsonObjectExtra = gift.extra;

                            long startDateLong = gift.startDate.getTime();

                            String startDate = Misc.dateVNFormatWithDot(startDateLong);
                            if (jsonObjectExtra.containsField(BillPayPromoConst.ACTIVE_TIME)) {
                                String longTime = jsonObjectExtra.getString(BillPayPromoConst.ACTIVE_TIME);
                                startDate = Misc.dateVNFormatWithDot(Long.parseLong(longTime));
                            }

                            callback.handle(new JsonObject()
                                            .putNumber("error", GiftError.ACTIVATED_TIME_ERROR)
                                            .putString("desc", String.format(PromoContentNotification.BILL_PAY_PROMO_CAN_NOT_ACTIVE_GIFT, startDate))
                            );
                            return;
                        }


                        if (gift.status == Gift.STATUS_USED) {
                            giftDb.unlock(agent, giftId, null);
                            callback.handle(new JsonObject()
                                            .putNumber("error", MomoProto.TranHisV1.ResultCode.NO_SUCH_GIFT_VALUE)
                                            .putString("desc", "Gift is used!")
                            );
                            return;
                        }

                        final QueuedGift queuedGift = new QueuedGift(agent, giftType.serviceId);
                        queuedGift.giftId = gift.getModelId();
                        queuedGift.gifTypeId = gift.typeId;

                        //check this gift was already in queued
                        queuedGiftDb.findOne(gift.getModelId(), new Handler<QueuedGift>() {
                            @Override
                            public void handle(QueuedGift queuedObj) {

                                //chua kich hoat
                                if (queuedObj == null) {

                                    queuedGiftDb.save(queuedGift, new Handler<String>() {
                                        @Override
                                        public void handle(String event) {
                                            if (event == null) {
                                                giftDb.unlock(agent, giftId, null);
                                                callback.handle(new JsonObject()
                                                                .putNumber("error", GiftError.SYSTEM_ERROR)
                                                                .putObject("gift", gift.toJsonObject())
                                                                .putObject("giftType", giftType.toJsonObject())
                                                );
                                                return;
                                            }
                                            gift.status = Gift.STATUS_USED;
                                            gift.lock = false;
                                            giftDb.update(gift, false, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean result) {
                                                    callback.handle(new JsonObject()
                                                                    .putNumber("error", 0)
                                                                    .putObject("gift", gift.toJsonObject())
                                                                    .putObject("giftType", giftType.toJsonObject())
                                                    );
                                                }
                                            });
                                        }
                                    });
                                    return;
                                }

                                //this gift was activated to use successfully before
                                gift.status = Gift.STATUS_USED;
                                gift.lock = false;
                                giftDb.update(gift, false, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean result) {
                                        callback.handle(new JsonObject()
                                                        .putNumber("error", 0)
                                                        .putObject("gift", gift.toJsonObject())
                                                        .putObject("giftType", giftType.toJsonObject())
                                        );
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    public void setGiftTimed(final String fromAgent
            , final String agentName
            , final String pin
            , final String toAgent
            , final String gifId
            , final String message
            , final long time
            , final Handler<JsonObject> callback) {
        getGiftInfo(gifId, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error", -10000);
                if (error != 0) {
                    callback.handle(result);
                    return;
                }
                final Gift gift = new Gift(result.getObject("gift"));
                final GiftType giftType = new GiftType(result.getObject("giftType"));
                if (!fromAgent.equalsIgnoreCase(gift.owner)) {
                    callback.handle(new JsonObject()
                                    .putNumber("error", GiftError.NOT_OWNED)
                                    .putString("desc", fromAgent + " is not owned")
                    );
                    return;
                }
                if (gift.status != Gift.STATUS_NEW && gift.status != Gift.STATUS_VIEWED) {
                    callback.handle(new JsonObject()
                                    .putNumber("error", GiftError.GIFT_NOT_STABLE)
                                    .putString("desc", "Expected state is " + Gift.STATUS_NEW + " or " + Gift.STATUS_VIEWED)
                    );
                    return;
                }

                Gift newValue = new Gift();
                newValue.setModelId(gifId);
                newValue.status = Gift.STATUS_TIMED;

                giftDb.update(newValue, false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
                        final TimedGift timedGift = new TimedGift();
                        timedGift.giftId = gift.getModelId();
                        timedGift.giftType = gift.typeId;
                        timedGift.message = message;
                        timedGift.fromAgent = fromAgent;
                        timedGift.toAgent = toAgent;
                        timedGift.time = time;
                        timedGift.fromAgentName = agentName;
                        timedGift.pin = DataUtil.encode(pin);
                        timedGift.preTranId = System.currentTimeMillis();

                        //update for the last request from client
                        timedGiftDb.upsert(timedGift, new Handler<TimedGift>() {
                            @Override
                            public void handle(TimedGift timedGift) {
                                callback.handle(new JsonObject()
                                                .putNumber("error", 0)
                                                .putObject("gift", gift.toJsonObject())
                                                .putObject("giftType", giftType.toJsonObject())
                                                .putObject("timedGift", timedGift.toJsonObject())
                                );
                            }
                        });

                        /*timedGiftDb.save(timedGift, new Handler<String>() {
                            @Override
                            public void handle(String event) {
                                callback.handle(new JsonObject()
                                                .putNumber("error", 0)
                                                .putObject("gift", gift.toJsonObject())
                                                .putObject("giftType", giftType.toJsonObject())
                                                .putObject("timedGift", timedGift.toJsonObject())
                                );
                            }
                        });*/
                    }
                });
            }
        });
    }

    /**
     * @param agent
     * @param serviceId not an array
     * @param callback
     */
    public void getQueuedGiftWithExclude(final String agent
            , final String serviceId
            , final ArrayList<String> excludeExtraVal
            , final String excludeKey
            , final Handler<QueuedGiftResult> callback) {
        final QueuedGift filter = new QueuedGift(agent);
        queuedGiftDb.find(filter, 100, new Handler<List<QueuedGift>>() {
            @Override
            public void handle(List<QueuedGift> queuedGifts) {
                final QueuedGiftResult result = new QueuedGiftResult();

                for (QueuedGift queuedGift : queuedGifts) {
                    if (queuedGift.service.contains(serviceId)) {
                        result.queuedGifts.add(queuedGift);
                    }
                }
                if (result.queuedGifts.size() == 0) {
                    callback.handle(result);
                    return;
                }

                final JsonObject joMaxQueuedSize = new JsonObject();
                joMaxQueuedSize.putNumber("size", result.queuedGifts.size());

//                JsonArray jarrOr = new JsonArray();
//                for (QueuedGift queuedGift : result.queuedGifts) {
//                    if (queuedGift.service.contains(serviceId)) {
//                        jarrOr.add(queuedGift.service);
//                    }
//                }
//                JsonObject joOr = new JsonObject().putArray(MongoKeyWords.OR, jarrOr);
//                JsonObject joF = new JsonObject().putObject("_id", joOr);
//                giftDb.getGiftWithFilter(joF, new Handler<ArrayList<Gift>>() {
//                    @Override
//                    public void handle(ArrayList<Gift> arrGift) {
//                        for (Gift gift: arrGift) {
//                            JsonObject extra = gift.getExtra();
//                            String val = (excludeKey != null ? extra.getString(excludeKey, "") : "");
//
//                            if (!"".equalsIgnoreCase(val) && excludeExtraVal.contains(val)) {
//                                int curQueuedSize = joMaxQueuedSize.getInteger("size");
//                                curQueuedSize--;
//                                joMaxQueuedSize.putNumber("size", curQueuedSize);
//                            } else {
//                                result.gifts.put(gift.getModelId(), gift);
//                            }
//                        }
//
//                        if (result.gifts.size() == result.queuedGifts.size()) {
//                            callback.handle(result);
//                        }
//                        else {
//                            callback.handle(new QueuedGiftResult());
//                        }
//
//                    }
//                });

                for (QueuedGift queuedGift : result.queuedGifts) {
                    if (queuedGift.service.contains(serviceId)) {
                        Gift giftFilter = new Gift(agent);
                        giftFilter.setModelId(queuedGift.giftId);

                        giftDb.findOne(giftFilter, new Handler<Gift>() {
                            @Override
                            public void handle(Gift gift) {
                                if (gift != null) {
                                    JsonObject extra = gift.getExtra();
                                    String val = (excludeKey != null ? extra.getString(excludeKey, "") : "");

                                    if (!"".equalsIgnoreCase(val) && excludeExtraVal.contains(val)) {
                                        int curQueuedSize = joMaxQueuedSize.getInteger("size");
                                        curQueuedSize--;
                                        joMaxQueuedSize.putNumber("size", curQueuedSize);
                                    } else {
                                        result.gifts.put(gift.getModelId(), gift);
                                    }
                                }

                                int lastSize = joMaxQueuedSize.getInteger("size");

                                if (result.gifts.size() == lastSize) {
                                    callback.handle(result);
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    public void getQueuedGift(final String agent
            , final String serviceId
            , final Handler<QueuedGiftResult> callback) {

        final QueuedGift filter = new QueuedGift(agent);
//        if(!serviceId.equalsIgnoreCase(StringConstUtil.PRUDENTIAL) && !serviceId.equalsIgnoreCase(StringConstUtil.HOME_CREDIT))
//        {
        queuedGiftDb.find(filter, 100, new Handler<List<QueuedGift>>() {
            @Override
            public void handle(List<QueuedGift> gifts) {
                final QueuedGiftResult result = new QueuedGiftResult();
                JsonArray jarrOr = new JsonArray();
                for (QueuedGift queuedGift : gifts) {
                    if (queuedGift.service.contains(serviceId)) {
                        result.queuedGifts.add(queuedGift);
                        jarrOr.add(new JsonObject().putString("_id",queuedGift.giftId));
                    }
                }
                if (result.queuedGifts.size() == 0 || jarrOr.size() == 0) {
                    callback.handle(result);
                    return;
                }
                JsonObject joOr = new JsonObject().putArray(MongoKeyWords.OR, jarrOr);
                giftDb.getGiftWithFilter(joOr, new Handler<ArrayList<Gift>>() {
                    @Override
                    public void handle(ArrayList<Gift> arrGift) {
                        for (Gift gift: arrGift) {
                            result.gifts.put(gift.getModelId(), gift);
                        }

                        if (result.gifts.size() == result.queuedGifts.size()) {
                            callback.handle(result);
                        }
                        else {
                            callback.handle(new QueuedGiftResult());
                        }

                    }
                });
                //giftDb.getGiftWithFilter();
//                for (QueuedGift queuedGift : result.queuedGifts) {
//                    if (queuedGift.service.contains(serviceId)) {
//                        Gift giftFilter = new Gift(agent);
//                        giftFilter.setModelId(queuedGift.giftId);
//                        giftDb.findOne(giftFilter, new Handler<Gift>() {
//                            @Override
//                            public void handle(Gift gift) {
//                                if (gift != null && !gift.lock) {
//                                    result.gifts.put(gift.getModelId(), gift);
//                                }
//
//                                if (result.gifts.size() == result.queuedGifts.size()) {
//                                    callback.handle(result);
//                                }
//                                else {
//                                    callback.handle(new QueuedGiftResult());
//                                }
//                            }
//                        });
//                    }
//                }
            }
        });
//        }
//        else{
//            final QueuedGiftResult result = new QueuedGiftResult();
//            if(result.queuedGifts.size() == 0) {
//                callback.handle(result);
//                return;
//            }
//        }
    }

    public void getQueuedGift(final String agent, final Queue<String> queueServiceId, final Handler<QueuedGiftResult> callback) {
        final QueuedGift filter = new QueuedGift(agent);
        queuedGiftDb.find(filter, 100, new Handler<List<QueuedGift>>() {
            @Override
            public void handle(List<QueuedGift> gifts) {
                final QueuedGiftResult result = new QueuedGiftResult();

                while (queueServiceId.size() > 0) {
                    String serviceId = queueServiceId.poll();
                    List<QueuedGift> removeList = new ArrayList<>();

                    for (QueuedGift queuedGift : gifts) {
                        if (queuedGift.service.contains(serviceId)) {
                            QueuedGift cloneGift = queuedGift.clone();
                            removeList.add(queuedGift);
                            result.queuedGifts.add(cloneGift);
                        }
                    }

                    for (int i = 0; i < removeList.size(); i++) {
                        QueuedGift q = removeList.get(i);
                        gifts.remove(q);
                    }
                }

                if (result.queuedGifts.size() == 0) {
                    callback.handle(result);
                    return;
                }

                JsonArray jarrOr = new JsonArray();
                for (QueuedGift queuedGift : result.queuedGifts) {
                    if (queuedGift != null) {
                        jarrOr.add(new JsonObject().putString("_id",queuedGift.giftId));
                    }
                }
                JsonObject joOr = new JsonObject().putArray(MongoKeyWords.OR, jarrOr);
                giftDb.getGiftWithFilter(joOr, new Handler<ArrayList<Gift>>() {
                    @Override
                    public void handle(ArrayList<Gift> arrGift) {
                        for (Gift gift: arrGift) {
                            result.gifts.put(gift.getModelId(), gift);
                        }

                        if (result.gifts.size() == result.queuedGifts.size()) {
                            callback.handle(result);
                        } else {
                            callback.handle(new QueuedGiftResult());
                        }

                    }
                });

//                for (QueuedGift queuedGift : result.queuedGifts) {
//                    Gift giftFilter = new Gift(agent);
//                    giftFilter.setModelId(queuedGift.giftId);
//                    giftDb.findOne(giftFilter, new Handler<Gift>() {
//                        @Override
//                        public void handle(Gift gift) {
//                            if (gift != null) {
//                                result.gifts.put(gift.getModelId(), gift);
//                            }
//
//                            if (result.gifts.size() == result.queuedGifts.size()) {
//                                callback.handle(result);
//                            }
//                        }
//                    });
//                }
            }
        });
    }

    public void getQueuedGiftWithExclude(final String agent
            , final Queue<String> queueServiceId
            , final ArrayList<String> excludeVals
            , final String excludeKey
            , final Handler<QueuedGiftResult> callback) {
        final QueuedGift filter = new QueuedGift(agent);
        queuedGiftDb.find(filter, 100, new Handler<List<QueuedGift>>() {
            @Override
            public void handle(List<QueuedGift> gifts) {
                final QueuedGiftResult result = new QueuedGiftResult();

                while (queueServiceId.size() > 0) {
                    String serviceId = queueServiceId.poll();
                    List<QueuedGift> removeList = new ArrayList<>();

                    for (QueuedGift queuedGift : gifts) {
                        if (queuedGift.service.contains(serviceId)) {
                            QueuedGift cloneGift = queuedGift.clone();
                            removeList.add(queuedGift);
                            result.queuedGifts.add(cloneGift);
                        }
                    }

                    for (int i = 0; i < removeList.size(); i++) {
                        QueuedGift q = removeList.get(i);
                        gifts.remove(q);
                    }
                }

                if (result.queuedGifts.size() == 0) {
                    callback.handle(result);
                    return;
                }


                final JsonObject joMaxQueuedSize = new JsonObject();
                joMaxQueuedSize.putNumber("size", result.queuedGifts.size());

                for (QueuedGift queuedGift : result.queuedGifts) {
                    Gift giftFilter = new Gift(agent);
                    giftFilter.setModelId(queuedGift.giftId);
                    giftDb.findOne(giftFilter, new Handler<Gift>() {
                        @Override
                        public void handle(Gift gift) {
                            if (gift != null) {

                                String val = gift.getExtra().getString(excludeKey, "");
                                if (excludeVals.contains(val)) {
                                    int curSize = joMaxQueuedSize.getInteger("size");
                                    curSize--;
                                    joMaxQueuedSize.putNumber("size", curSize);
                                } else {
                                    result.gifts.put(gift.getModelId(), gift);
                                }
                            }

                            int curQueuedSize = joMaxQueuedSize.getInteger("size");
                            if (result.gifts.size() == curQueuedSize) {
                                callback.handle(result);
                            }
                        }
                    });
                }
            }
        });
    }


    //add new
    public void updateGift(Gift model, boolean upsert, final Handler<Boolean> callback) {
        giftDb.update(model, upsert, callback);
    }

    public void loadGiftType()
    {

    }
}