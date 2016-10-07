package com.mservice.momo.vertx;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.wc.DuDoanDb;
import com.mservice.momo.data.wc.MatchDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.NotificationUtils;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.models.wc.DuDoan;
import com.mservice.momo.vertx.models.wc.Match;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by concu on 6/18/14.
 */
public class WcTransfer extends Verticle {
    public static final String CMD_MAKE_TRANS = "makeTrans";
    public static String MESSAGE_GIAI_TI_SO = "Chúc mừng bạn đã trúng thưởng dự đoán tỉ số MoMo-Tỷ Phú World Cup: @aName @a @bName @b.";
    public static String MESSAGE_GIAI_KET_QUA = "Chúc mừng bạn đã trúng thưởng dự đoán kết quả MoMo-Tỷ Phú World Cup: @aName @status @bName.";
    public static String MESSAGE_GIAI_TI_SO_VA_KET_QUA = "Chúc mừng bạn đã trúng thưởng dự đoán kết quả và tỉ số MoMo-Tỷ Phú World Cup: @aName @a @bName @b.";
    MatchDb matchDb;
    private DuDoanDb duDoanDb;
    private TransDb transDb;
    private AtomicBoolean isFullRunning = new AtomicBoolean(false);
    private AtomicBoolean isBatchRunning = new AtomicBoolean(false);
    private boolean isLastBatch = false;
    private Logger logger;
    private String currentMatchId;
    private Match currentMatch;
    private long currentSendTimerId = 0;
    private AtomicLong currentSendedCount = new AtomicLong(0);
    private int wc_batch_size;

    private String ADJUSTMENT_ACCOUNT = "";
    private boolean allow_run = false;

    public void start() {
        logger = container.logger();
        duDoanDb = new DuDoanDb(vertx,container);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        matchDb = new MatchDb(vertx,getContainer());

        JsonObject jo = getContainer().config();
        /*{"batch_size":2000,adjust_account:"confirm"}*/

        wc_batch_size = jo.getInteger("batch_size",20);
        ADJUSTMENT_ACCOUNT = jo.getString("adjust_account","wc2014trathuong");
        allow_run = jo.getBoolean("allow_run",false);

        isFullRunning.set(false);

        vertx.eventBus().registerLocalHandler(AppConstant.WcTransfer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message)
            {
                String cmd = message.body().getString("cmd");
                if(cmd.equalsIgnoreCase(CMD_MAKE_TRANS)){
                    logger.debug("CMD_MAKE_TRANS");
                    if(!allow_run){
                        message.reply("We don't allow run on this server...");
                        return;
                    }

                    if(isFullRunning.compareAndSet(false,true) && allow_run){
                        makeTrans(message);
                    }
                    else {
                        message.reply("Still in progress, please wait...");
                    }
                }
            }
        });

    }

    private void makeTrans(final Message<JsonObject> message) {

        logger.debug("makeTrans()");
        isLastBatch =false;
        //INPUT VALIDATION
        final String matchId = message.body().getString("matchId");
        if (matchId == null || matchId.isEmpty()) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "MatchId can't be null or empty!")
            );
            isFullRunning.compareAndSet(true, false);

            logger.debug("makeTrans()> matchId == null || matchId.isEmpty() ");
            return;

        }

        currentMatchId  = matchId;

        Match matchFilter = new Match();
        matchFilter.setModelId(matchId);

        //lay thong tin tran dau
        logger.debug("makeTrans()> matchDb");
        matchDb.findOne(matchFilter, new Handler<Match>() {
            @Override
            public void handle(final Match match) {
                logger.debug("makeTrans()> findOne() " + match);
                if(match == null){
                    isFullRunning.compareAndSet(true, false);
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 2)
                                    .putString("desc", "MatchId can't be null or empty!")
                    );
                    return;
                }
                else{
                    currentMatch = match;
                    logger.info("WC START TRANSFER ");
                    startNextBatch(0);
                }
            }
        });
    }

    private void startNextBatch(final long currentBatchSize) {
        currentSendTimerId = vertx.setPeriodic(1000, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                if(currentSendedCount.compareAndSet(currentBatchSize,0)){
                    vertx.cancelTimer(currentSendTimerId);
                    sendNextBatch();
                }
                else{
                    logger.info("chua cap nhat xong " + currentSendedCount.get() + "/" + currentBatchSize);
                }
            }
        });
    }
    
    private void sendNextBatch() {

        logger.info("isLastBatch<" + isLastBatch + "> isFullRunning<" + isFullRunning.get() +">");

        if (currentMatchId == null || currentMatchId.isEmpty() || currentMatch == null || isLastBatch == true) {
            logger.info("SEND MONEY DONE.....");
            isFullRunning.compareAndSet(true, false);
            return;
        }

        duDoanDb.getWinnerBatch(currentMatchId, wc_batch_size, new Handler<ConcurrentLinkedQueue<DuDoan>>() {
            @Override
            public void handle(final ConcurrentLinkedQueue<DuDoan> concurrentDeque) {

                if (concurrentDeque == null || concurrentDeque.size() == 0 || concurrentDeque.isEmpty()) {
                    isFullRunning.compareAndSet(true, false);
                    return;
                }

                //batch cuoi cung can gui di
                if (concurrentDeque.size() < wc_batch_size) {
                    //
                    isLastBatch = true;
                    //allow request from web
                    isFullRunning.set(false);

                } else {
                    isLastBatch = false;
                    //not allow request from web
                    isFullRunning.set(true);
                }

                logger.info("SEND MONEY FOR NEW BATCH <" + concurrentDeque.size() + ">");

                //MATCH INFOMATION
                String status = "hòa";
                if (currentMatch.getResult() < 0) {
                    status = "thua";
                } else if (currentMatch.getResult() > 0) {
                    status = "thắng";
                }

                String[] arr = currentMatch.getName().split("-");
                final String aName = arr[0].trim();
                final String bName = arr[1].trim();
                final String fStatus = status;

                for (int i = 0; i < concurrentDeque.size(); i++) {

                    //lay 1 du doan ra xu ly
                    final DuDoan item = concurrentDeque.poll();
                    Integer phone = null;
                    try {
                        phone = Integer.parseInt(item.getModelId());
                    } catch (NumberFormatException e) {
                        logger.error("WC. CAN'T PARSE NUMBER. ");
                    }

                    if (phone == null) {
                        //TODO: KHONG CHUYEN, luu lai voi ma loi bang 9999
                        item.setsMoney(item.getMoney());
                        item.setMoney(0L);
                        item.setTranError(9999);

                        logger.info("WC update DuDoan: " + item);
                        duDoanDb.update(item, false, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean event) {
                                long send = currentSendedCount.incrementAndGet();
                                logger.info("WC saved DuDoan: " + item + " left " + send);
                            }
                        });
                    }
                    else {
                        final int fphone = phone;

                        logger.info("THUC HIEN CHUYEN TIEN CHO SO DIEN THOAI " + fphone);
                        transfer(phone, item.getMoney(), item, currentMatch, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject joResult) {

                                if (joResult == null || joResult.getInteger("ERROR", -1) != 0) {
                                    logger.info("WC TRANSFER FINISHED FALSE: " + fphone + " AMOUNT:" + item.getMoney() + " RESULT: " + joResult.getNumber("ERROR"));
                                    item.setsMoney(item.getMoney());
                                    item.setMoney(0L);
                                    item.setTranError(joResult.getInteger("ERROR", -1));
                                    logger.info("WC update DuDoan: " + item);

                                    duDoanDb.update(item, false, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {
                                            long send = currentSendedCount.incrementAndGet();
                                            logger.info("WC saved DuDoan: " + item + " left " + send);
                                        }
                                    });
                                } else {
                                    logger.info("WC TRANSFER FINISHED DONE: " + fphone + " AMOUNT:" + item.getMoney() + " RESULT: " + joResult.getNumber("ERROR"));
                                    final TranObj mainObj = new TranObj();
                                    mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                                    mainObj.tranId = joResult.getLong("TID", System.currentTimeMillis());
                                    mainObj.clientTime = System.currentTimeMillis();
                                    mainObj.ackTime = System.currentTimeMillis();
                                    mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
                                    mainObj.amount = item.getMoney();
                                    mainObj.status = TranObj.STATUS_OK;
                                    mainObj.error = 0;
                                    mainObj.cmdId = -1;
                                    mainObj.billId = "-1";
                                    mainObj.io = +1;
                                    mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                    mainObj.category = -1;
                                    mainObj.partnerName = "MoMo";
                                    mainObj.partnerId = "839917199";

                                    if (item.getResult() == currentMatch.getResult() && item.getA() == currentMatch.getA() && item.getB() == currentMatch.getB()) {
                                        logger.info("TRUNG 2 GIAI");
                                        mainObj.comment = MESSAGE_GIAI_TI_SO_VA_KET_QUA;
                                    } else if (item.getResult() == currentMatch.getResult()) {
                                        logger.info("TRUNG GIAI KET QUA");
                                        mainObj.comment = MESSAGE_GIAI_KET_QUA;
                                    } else if (item.getA() == currentMatch.getA() && item.getB() == currentMatch.getB()) {
                                        logger.info("TRUNG GIAI TI SO");
                                        mainObj.comment = MESSAGE_GIAI_TI_SO;
                                    } else {
                                        logger.info("KHONG TRUNG GIAI NAO, SAO CO TIEN DUOC");
                                        mainObj.comment = "KHONG TRUNG GIAI NAO, SAO CO TIEN DUOC";
                                    }

                                    mainObj.comment = mainObj.comment.replaceAll("@aName", aName);
                                    mainObj.comment = mainObj.comment.replaceAll("@bName", bName);
                                    mainObj.comment = mainObj.comment.replaceAll("@status", fStatus);
                                    mainObj.comment = mainObj.comment.replaceAll("@a", String.valueOf(currentMatch.getA()));
                                    mainObj.comment = mainObj.comment.replaceAll("@b", String.valueOf(currentMatch.getB()));
                                    mainObj.comment = mainObj.comment.replaceAll("@amount", String.valueOf(item.getMoney()));
                                    mainObj.owner_number = fphone;
                                    mainObj.owner_name = "MoMo";

                                    item.setsMoney(item.getMoney());
                                    item.setMoney(0L);
                                    item.setTranError(joResult.getInteger("ERROR", 0));
                                    logger.info("WC update DuDoan: " + item);

                                    duDoanDb.update(item, false, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {
                                            long send = currentSendedCount.incrementAndGet();
                                            logger.info("WC saved DuDoan: " + item + " left " + send);

                                            logger.info("WC transDb.upsertTranOutSide");



                                            transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean result) {

                                                    if (!result) {

                                                        //*********
                                                        BroadcastHandler.LocalMsgHelper helperTran = new BroadcastHandler.LocalMsgHelper();
                                                        helperTran.setType(SoapProto.Broadcast.MsgType.MONEY_RECV_VALUE);
                                                        helperTran.setSenderNumber(0);
                                                        helperTran.setReceivers("0" + fphone);
                                                        helperTran.setExtra(mainObj.getJSON());

                                                        vertx.eventBus().send(Misc.getNumberBus(fphone), helperTran.getJsonObject());
//                                                        log.add("createRequestPushNotification ----------------------> ", "six");
                                                        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                                                , NotificationUtils.createRequestPushNotification(fphone, 2, mainObj));

                                                    } else {
                                                        logger.debug("Luu that bai");
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                    }
                }

                //after for, we call next batch
                startNextBatch(concurrentDeque.size());

              /*  Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });

                t.start();*/
            }
        });
    }

    private void transfer(final int number, final long amount, final DuDoan duDoan, final Match match, final Handler<JsonObject> callback) {
//        CoreCommon.adjustment(vertx
//                , ADJUSTMENT_ACCOUNT
//                , amount
//                , "0" + number
//                , "WC"
//                , logger
//                , callback);
    }


}
