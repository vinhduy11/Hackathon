package com.mservice.momo.vertx;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.WcPlayerDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.wc.*;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.NotificationUtils;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.models.wc.*;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by nam on 6/9/14.
 */
public class WcVerticle extends Verticle{
//    public static String ADDRESS_REQUEST_CMD = "WcVerticle";

    public static final String CMD_MATCH_FINISH = "matchFinish";
    public static final String CMD_TRANS_RESET = "tranReset";
    public static final String CMD_TRANS_INIT = "tranInit";
    public static final String CMD_MAKE_TRANS = "makeTrans";
    public static final String CMD_GET_MESSAGES = "getMessages";
    public static final String CMD_SET_MESSAGES = "setMessages";
    public static final String CMD_SEND_ZALO_MESSAGE = "sendZaloMessage";
    public static final String CMD_MAKE_TRANSFER_TASK = "doTransferTask";

    public static final String CMD_GET_USER_STATUS = "CMD_IS_WC_PLAYER";
    public static final String CMD_SET_USER_STATUS = "CMD_SET_USER_STATUS";

    public static final String CMD_TANG_THUONG = "tangThuong";

    public static final String CMD_CLEAR_THUONG_THEM = "clearThuongThem";
    public static final String CMD_INIT_THUONG_THEM = "initThuongThem";
    public static final String CMD_MAKE_THUONG_THEM = "makeThuongThem";


    public static final String PARTNER_HOST = "momo.zapps.vn";
    public static final Integer PARTNER_PORT = 80;

    public static final String URI_GET_USER_STATUS = "/Services/GetMemberStatus.aspx";
    public static final String URI_SET_USER_STATUS = "/Services/SetMemberStatus.aspx";

    public static final String URI_ZALO_MESSAGE_SERVICE = "/Services/SendMessage.aspx?phone=%s&msg=%s";

    public static final String ADJUSTMENT_ACCOUNT = "wc2014trathuong";

    public static final String CMD_TEST_TRAN = "testTran";
    public static String MESSAGE_GIAI_TI_SO =  "Chúc mừng bạn đã trúng thưởng dự đoán tỉ số MoMo-Tỷ Phú World Cup: @aName @a @bName @b";
    public static String MESSAGE_GIAI_KET_QUA = "Chúc mừng bạn đã trúng thưởng dự đoán kết quả MoMo-Tỷ Phú World Cup: @aName @status @bName";
    public static String MESSAGE_GIAI_TI_SO_VA_KET_QUA = "Chúc mừng bạn đã trúng thưởng dự đoán kết quả và tỉ số MoMo-Tỷ Phú World Cup: @aName @a @bName @b";
    public static String MESSAGE_THUONG_THEM = "Chúc mừng bạn! Bạn đã nhận được phần thưởng liên kết từ việc những người bạn giới thiệu đã dự đoán đúng trận đấu @aName - @bName.";
    public static String MESSAGE_ZALO_WINER = "Xin chúc mừng! Thóc lại về kho. Bạn đã dự đoán đúng kết quả & tỷ số trận chung kết WC 2014 giữa Đức vs Achentina. Có @phanTramKetQua% người dự đoán đúng kết quả và @phanTramTiSo% người dự đoán đúng tỷ số trận đấu này. Cảm ơn bạn đã đồng hành cùng ví MoMo dự đoán World Cup 2014! Chia sẻ cảm xúc của bạn về chương trình trên trang facebook Ví MoMo: http://on.fb.me/1mqBrty";
    public static String MESSAGE_ZALO_WINER_KET_QUA = "Xin chúc mừng! Thóc lại về kho. Bạn đã dự đoán đúng kết quả trận chung kết WC 2014 giữa Đức vs Achentina. Có @phanTramKetQua% người dự đoán đúng kết quả và @phanTramTiSo% người dự đoán đúng tỷ số trận đấu này. Cảm ơn bạn đã đồng hành cùng ví MoMo dự đoán World Cup 2014! Chia sẻ cảm xúc của bạn về chương trình trên trang facebook Ví MoMo: http://on.fb.me/1mqBrty";
    public static String MESSAGE_ZALO_WINER_TI_SO = "Xin chúc mừng! Thóc lại về kho. Bạn đã dự đoán đúng tỷ số trận chung kết WC 2014 giữa Đức vs Achentina. Có @phanTramKetQua% người dự đoán đúng kết quả và @phanTramTiSo% người dự đoán đúng tỷ số trận đấu này. Cảm ơn bạn đã đồng hành cùng ví MoMo dự đoán World Cup 2014! Chia sẻ cảm xúc của bạn về chương trình trên trang facebook Ví MoMo: http://on.fb.me/1mqBrty";
    public static String MESSAGE_ZALO_LOSER = "Trận chung kết World Cup 2014 đã kết thúc không như bạn dự đoán. Cảm ơn bạn đã đồng hành cùng ví MoMo! Chia sẻ cảm xúc của bạn về chương trình trên trang facebook Ví MoMo: http://on.fb.me/1mqBrty";
    public static String ZALO_GROUP ="";
    public static String ZALO_CAPSET_ID = "";
    public static String ZALO_UPPER_LIMIT  ="";
    private MatchDb matchDb;
    private DuDoanDb duDoanDb;
    private ThuongThemDb thuongThemDb;
    private NewWcPlayerDb newWcPlayerDb;
    private TransDb transDb;
    private TangThuongDb tangThuongDb;
    private TransferTaskDb transferTaskDb;
    private Logger logger;
    private TransDb trandb;
    private WcPlayerDb wcPlayerDb;

    private void config(JsonObject globalConfig) {
        JsonObject serverConfig = globalConfig.getObject("server", new JsonObject());

        ZALO_GROUP = serverConfig.getString("zalo_group");
        ZALO_CAPSET_ID  = serverConfig.getString("zalo_capset_id");
        ZALO_UPPER_LIMIT  = serverConfig.getString("zalo_upper_limit");
    }

    @Override
    public void start() {
        JsonObject globalConfig = container.config();
        JsonObject wcVerticleConfig = globalConfig.getObject("wcVerticleConfig", new JsonObject());
        config(globalConfig);

        matchDb = new MatchDb(vertx, container);
        duDoanDb = new DuDoanDb(vertx, container);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), container.logger(), container.config());
        tangThuongDb = new TangThuongDb(vertx, container);
        thuongThemDb = new ThuongThemDb(vertx, container);
        newWcPlayerDb = new NewWcPlayerDb(vertx, container);
        wcPlayerDb = new WcPlayerDb(vertx, container);
        logger = container.logger();
        trandb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        transferTaskDb = new TransferTaskDb(vertx, container);

//        vertx.setTimer(2000, new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                JsonObject json = new JsonObject();
//                json.putString("cmd", CMD_GET_USER_STATUS);
//                json.putNumber("number", 987568815);
//                vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, json, new Handler<CoreMessage>() {
//                    @Override
//                    public void handle(CoreMessage event) {
//                        System.out.println("###");
//                        System.out.println(event.body());
//                    }
//                });
//            }
//        });


        final HttpClient client = vertx.createHttpClient()
                .setHost(PARTNER_HOST)
                .setPort(PARTNER_PORT)
                .setMaxPoolSize(10)
                .setConnectTimeout(15000)
                .setKeepAlive(false);

        vertx.eventBus().registerLocalHandler(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                String cmd = message.body().getString("cmd");
                switch (cmd) {
                    case CMD_MATCH_FINISH:
                        matchFinish(message);
                        break;
                    case CMD_TRANS_INIT:
                        transInit(message);
                        break;
                    case CMD_TRANS_RESET:
                        transReset(message);
                        break;
                    case CMD_GET_USER_STATUS:
                        getUserStatus(client, message);
                        break;
                    case CMD_SET_USER_STATUS:
                        setUserStatus(client, message);
                        break;
                    case CMD_MAKE_TRANS:
                        makeTrans(message);
                        break;
                    case CMD_TEST_TRAN:
                        testTran(message);
                        break;
                    case CMD_TANG_THUONG:
                        tangThuong(message);
                        break;
                    case CMD_GET_MESSAGES:
                        getMessages(message);
                        break;
                    case CMD_SET_MESSAGES:
                        setMessages(message);
                        break;
                    case CMD_SEND_ZALO_MESSAGE:
                        sendZaloMessage(client, message);
                        break;
                    case CMD_CLEAR_THUONG_THEM:
                        clearThuongThem(message);
                        break;
                    case CMD_INIT_THUONG_THEM:
                        initThuongThem(message);
                        break;
                    case CMD_MAKE_THUONG_THEM:
                        makeThuongThem(message);
                        break;
                    case CMD_MAKE_TRANSFER_TASK:
                        makeTransferTask(message);
                        break;
                    default: {
                        container.logger().warn("Unsupported command:" + message.body());
                        message.reply(
                                new JsonObject()
                                        .putNumber("error", 1)
                                        .putString("desc", "Unsupported command")
                        );
                    }
                }
            }
        });

//        vertx.setPeriodic(10000, new Handler<Long>() {
//            @Override
//            public void handle(Long timerId) {
//                matchDb.find(null, 0, new Handler<List<Match>>() {
//                    @Override
//                    public void handle(final List<Match> matches) {
//                        for(final Match match: matches) {
//                            final DuDoan filter = new DuDoan();
//                            filter.setMatchId(match.getModelId());
//                            duDoanDb.count(filter, new Handler<Long>() {
//                                @Override
//                                public void handle(Long soNguoiThamGia) {
//                                    match.setSoNguoiChoi(soNguoiThamGia);
//                                    matchDb.update(match, false, null);
//                                }
//                            });
//                        }
//                    }
//                });
//            }
//        });

        if (wcVerticleConfig.getBoolean("enableUpdateZaloStatus", false) == true) {
            checkNewPlayers(client);
        }
    }

    private void makeTransferTask(Message<JsonObject> message) {
        message.reply(new JsonObject().putNumber("error", 0).putString("desc", "Transfering..."));

        vertx.setPeriodic(100, new Handler<Long>() {
            boolean doNext = true;
            @Override
            public void handle(final Long timerId) {
                if (!doNext)
                    return;
                doNext = false;
                transferTaskDb.getOneTransferTask(new Handler<TransferTask>() {
                    @Override
                    public void handle(final TransferTask transferTask) {
                        if(transferTask==null) {
                            logger.info("TRANSFER FINISHED");
                            vertx.cancelTimer(timerId);
                            return;
                        }
                        transfer(transferTask.phone, transferTask.comment, transferTask.money, new Handler<Integer>() {
                            @Override
                            public void handle(Integer error) {
                                logger.info("WC TRANSFER " + transferTask.money + " TO " + transferTask.phone + ", Result:" + error);

                                transferTask.sentMoney = transferTask.money;
                                transferTask.tranError = error;
                                transferTask.money = 0L;
                                transferTaskDb.update(transferTask, false, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {
                                        doNext = true;
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void checkNewPlayers(final HttpClient client) {
        vertx.setPeriodic(10000, new Handler<Long>() {
            @Override
            public void handle(Long timerId) {
                newWcPlayerDb.find(null, 10, new Handler<List<NewWcPlayer>>() {
                    @Override
                    public void handle(List<NewWcPlayer> newWcPlayers) {
                        logger.info("Checking new players's Zalo Status: " + newWcPlayers);
                        for (final NewWcPlayer wcPlayer : newWcPlayers) {
                            final NewWcPlayer fWcPlayer = wcPlayer;
                            getZaloStatus(client, wcPlayer.number, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject zaloStatus) {
                                    if (zaloStatus.getInteger("error") != 0)
                                        return;
                                    Integer status = zaloStatus.getInteger("status");
                                    if (status == null) {
                                        return;
                                    }
                                    if (status == 1) {
                                        setZaloStatus(client, wcPlayer.number, 2, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject setStatusResult) {
                                                if (setStatusResult.getInteger("error") == 0) {
                                                    Integer zaloResult = setStatusResult.getInteger("zaloError");
                                                    if (zaloResult != null && zaloResult == 0) {
                                                        addCoreZaloGroup(fWcPlayer, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean successful) {
                                                                if (successful) {
                                                                    newWcPlayerDb.remove(fWcPlayer, null);
                                                                    logger.info("Successfully adding Zalo member:" + fWcPlayer);
                                                                } else {
                                                                    logger.error("addCoreZaloGroup result:" + fWcPlayer);
                                                                }
                                                            }
                                                        });
                                                    }
                                                    return;
                                                }
                                                logger.error("setZaloStatus result:" + setStatusResult);
                                            }
                                        });
                                        return;
                                    }
                                    //TODO: Remove out of the queue when status is different from 1.
                                    newWcPlayerDb.remove(fWcPlayer, null);
                                    logger.info("Remove ");
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    public void addCoreZaloGroup(final NewWcPlayer wcPlayer, Handler<Boolean> callback) {


        //send Zalo with status follow + momoer
        JsonObject follow_momoer = new JsonObject();
        follow_momoer.putString("cmd", WcVerticle.CMD_SET_USER_STATUS);
        follow_momoer.putNumber("number", wcPlayer.number);
        follow_momoer.putNumber("status", 2); // 2: follow + momo member

        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, follow_momoer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                logger.info("ket qua dang ky follow + momoer voi zalo " + reply.body());
            }
        });

        //TODO: Save WC PLAYER
        final WcPlayer player = new WcPlayer();
        player.setModelId(String.valueOf(wcPlayer.number));
        player.setStatus(2);

        logger.info(wcPlayer.number + "|save player: " + player);
        wcPlayerDb.save(player, new Handler<String>() {
            @Override
            public void handle(String event) {
                logger.info(wcPlayer.number + "|saved player: ");
            }
        });

        //todo xem nhu thanh cong --> set nhom zalo cho thang nay
        Buffer buffer = MomoMessage.buildBuffer(SoapProto.MsgType.MAP_AGENT_TO_ZALO_GROUP_VALUE
                , 0
                , wcPlayer.number
                , SoapProto.ZaloGroup.newBuilder()
                .setZaloGroup(ZALO_GROUP)
                .setZaloCapsetId(ZALO_CAPSET_ID)
                .setZaloUpperLimit(ZALO_UPPER_LIMIT)
                .build().toByteArray());

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> reply) {
                logger.info(wcPlayer.number + ",set group zalo " + reply.body());
            }
        });

//        createTwoTransactions(wcPlayer.number, wcPlayer.name);

        callback.handle(true);

    }

    /*public void createTwoTransactions(int number, String name) {

        //TODO CREATE TWO TRANSACTIONS.
        final TransDb.TranObj mainObj = new TransDb.TranObj();
        mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
        mainObj.tranId = System.currentTimeMillis();
        mainObj.clientTime = System.currentTimeMillis();
        mainObj.ackTime = System.currentTimeMillis();
        mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = 64000;
        mainObj.status = TranObj.STATUS_OK;
        mainObj.error = 0;
        mainObj.cmdId = -1;
        mainObj.billId = "-1";
        mainObj.io = +1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.category = -1;
        mainObj.partnerName = "MoMo";
        mainObj.comment = "Bạn đã nhận được 64.000đ.";
        mainObj.owner_number = number;
        mainObj.owner_name = name;


        logger.info("createTwoTransactions");
        trandb.upsertTranOutSide(mainObj.owner_number, mainObj.getJSON(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject obj) {
                if (obj != null && obj.getObject(RESULT, null) != null) {
                    JsonObject json = obj.getObject(RESULT);
                    TransDb.TranObj result = new TransDb.TranObj(json);
                    if (result.cmdId < -1) {
                        logger.debug("Cap nhat");
                    } else {
                        logger.debug("Tao moi");

                        BroadcastHandler.sendOutSideTransSync(vertx, mainObj);

                        vertx.eventBus().send(
                                AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                , NotificationUtils.createRequestPushNotification(mainObj.owner_number, 2, mainObj)
                        );
                    }
                } else {
                    logger.debug("Luu that bai");
                }
            }
        });


        //TODO CREATE TWO TRANSACTIONS.
        final TransDb.TranObj outObject = new TransDb.TranObj();
        outObject.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
        outObject.tranId = System.currentTimeMillis();
        outObject.clientTime = System.currentTimeMillis();
        outObject.ackTime = System.currentTimeMillis();
        outObject.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
        outObject.amount = 64000;
        outObject.status = TranObj.STATUS_OK;
        outObject.error = 0;
        outObject.cmdId = -1;
        outObject.billId = "-1";
        outObject.io = -1;
        outObject.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        outObject.category = -1;
        outObject.partnerName = "MoMo";
        outObject.comment = "Bạn đã dùng 64.000đ để tham gia MOMO - TỶ PHÚ WORLD CUP 2014.";
        outObject.owner_number = number;
        outObject.owner_name = name;

        trandb.upsertTranOutSide(outObject.owner_number, outObject.getJSON(), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject obj) {
                if (obj != null && obj.getObject(RESULT, null) != null) {
                    JsonObject json = obj.getObject(RESULT);
                    TransDb.TranObj result = new TransDb.TranObj(json);
                    if (result.cmdId < -1) {
                        logger.debug("Cap nhat");
                    } else {
                        logger.debug("Tao moi");

                        BroadcastHandler.sendOutSideTransSync(vertx, outObject);

                        vertx.eventBus().send(
                                AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                , NotificationUtils.createRequestPushNotification(outObject.owner_number, 2, outObject)
                        );
                    }
                } else {
                    logger.debug("Luu that bai");
                }
            }
        });
    }*/

    private void clearThuongThem(final Message<JsonObject> message) {
        final String matchId = message.body().getString("matchId");
        if (matchId == null) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "Missing matchId.")
            );
            return;
        }

        Match matchFilter = new Match();
        matchFilter.setModelId(matchId);
        matchDb.findOne(matchFilter, new Handler<Match>() {
            @Override
            public void handle(Match match) {
                if (match == null) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("desc", "Match id does not exist.")
                    );
                    return;
                }
                ThuongThem filter = new ThuongThem();
                filter.matchId = matchId;

                ThuongThem newThuongThem = new ThuongThem();
                newThuongThem.money = 0L;
                newThuongThem.numbers = new JsonArray();

                thuongThemDb.updateMulti(filter, newThuongThem, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {
                        message.reply(
                                new JsonObject()
                                        .putNumber("error", 0)
                                        .putString("desc", "Successfully.")
                        );
                    }
                });
            }
        });
    }

    private void initThuongThem(final Message<JsonObject> message) {
        final String matchId = message.body().getString("matchId");
        if (matchId == null) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "Missing matchId.")
            );
            return;
        }


        Match matchFilter = new Match();
        matchFilter.setModelId(matchId);
        matchDb.findOne(matchFilter, new Handler<Match>() {
            @Override
            public void handle(Match match) {
                if (match == null) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("desc", "Match id does not exist.")
                    );
                    return;
                }

                thuongThemDb.getOneThuongThem(matchId, new Handler<ThuongThem>() {
                    @Override
                    public void handle(ThuongThem thuongThem) {
                        if (thuongThem != null) {
                            message.reply(
                                    new JsonObject()
                                            .putNumber("error", 3)
                                            .putString("desc", "Clean thuongThem collection first.")
                            );
                            return;
                        }
                        thuongThemDb.initThuongThem(matchId, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject result) {
                                message.reply(result);
                            }
                        });
                    }
                });
            }
        });
    }

    private void makeThuongThem(final Message<JsonObject> message) {
        final String matchId = message.body().getString("matchId");
        if (matchId == null) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "Missing matchId.")
            );
            return;
        }


        Match matchFilter = new Match();
        matchFilter.setModelId(matchId);
        matchDb.findOne(matchFilter, new Handler<Match>() {
            @Override
            public void handle(final Match match) {
                if (match == null) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("desc", "Match id does not exist.")
                    );
                    return;
                }

                vertx.setPeriodic(1000, new Handler<Long>() {
                    boolean doNext = true;

                    @Override
                    public void handle(final Long timerId) {
                        if (!doNext)
                            return;
                        doNext = false;
                        thuongThemDb.getOneThuongThem(matchId, new Handler<ThuongThem>() {
                            @Override
                            public void handle(final ThuongThem thuongThem) {
                                if (thuongThem == null) {
                                    vertx.cancelTimer(timerId);
                                    logger.info("THUONG THEM finished");
                                    return;
                                }
                                logger.info("THUONG THEM" + thuongThem);
                                String number = thuongThem.getModelId();
                                try {
                                    final int phone = Integer.parseInt(number);
                                    transfer(phone, thuongThem, match, new Handler<Integer>() {
                                        @Override
                                        public void handle(Integer error) {
                                            logger.info("WC TRANSFER " + thuongThem.money + " TO " + phone + " RESULT: " + error);
                                            thuongThem.tranError = error;
                                            thuongThem.sentMoney = thuongThem.money;
                                            thuongThem.money = 0L;
                                            thuongThemDb.save(thuongThem, new Handler<String>() {
                                                @Override
                                                public void handle(String s) {
                                                    logger.info("THUONG THEM: " + thuongThem);
                                                    doNext = true;
                                                }
                                            });
                                        }
                                    });
                                } catch (NumberFormatException e) {
                                    logger.error("THUONG THEM: Can't get Phone Number : " + thuongThem);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private String buildMessage(Match match, String msg) {
        String status = "hòa";
        if (match.getResult() < 0) {
            status = "thua";
        } else if (match.getResult() > 0) {
            status = "thắng";
        }

        String[] arr = match.getName().split("-");
        String aName = arr[0].trim();
        String bName = arr[1].trim();

        String fMessage = msg;

        fMessage = fMessage.replaceAll("@aName", aName);
        fMessage = fMessage.replaceAll("@bName", bName);
        fMessage = fMessage.replaceAll("@status", status);
        fMessage = fMessage.replaceAll("@a", String.valueOf(match.getA()));
        fMessage = fMessage.replaceAll("@b", String.valueOf(match.getB()));
        fMessage = fMessage.replaceAll("@phanTramKetQua", String.format("%.2f", match.getPhanTramKetQua()));
        fMessage = fMessage.replaceAll("@phanTramTiSo", String.format("%.2f", match.getPhanTramTiSo()));

        return fMessage;
    }

    private void sendZaloMessage(final HttpClient client,final Message<JsonObject> request) {
        final String matchId = request.body().getString("matchId");
        if (matchId == null) {
            request.reply(
                    new JsonObject().putNumber("error", 2)
                            .putString("desc", "Missing matchId.")
            );
            return;
        }

        Match filter = new Match();
        filter.setModelId(matchId);
        matchDb.findOne(filter, new Handler<Match>() {
            @Override
            public void handle(final Match match) {
                if (match == null) {
                    request.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("desc", "MatchId doesn't exist.")
                    );
                    return;
                }
                if (match.getResult() == null || match.getA() == null || match.getB() == null || match.getPhanTramKetQua() == null || match.getPhanTramTiSo() == null) {
                    request.reply(
                            new JsonObject()
                                    .putNumber("error", 2)
                                    .putString("desc", "Match is missing result.")
                                    .putObject("match", match.getPersisFields())
                    );
                    return;
                }

                request.reply(new JsonObject().putNumber("error", 0).putString("desc", "Messages is sending..."));

                vertx.setPeriodic(100, new Handler<Long>() {
                    @Override
                    public void handle(final Long timerId) {
                        duDoanDb.nextDuDoanHasMessage(matchId, new Handler<DuDoan>() {
                            boolean sentNext = true;
                            @Override
                            public void handle(DuDoan duDoan) {
                                if(!sentNext)
                                    return;
                                sentNext = false;
                                if (duDoan == null) {
                                    vertx.cancelTimer(timerId);
                                    return;
                                }
                                final String phone = duDoan.getModelId();
                                String message;
                                if (duDoan.getResult() == match.getResult() && (duDoan.getA() == match.getA() && duDoan.getB() == match.getB())) {
                                    message = MESSAGE_ZALO_WINER;
                                } else if (duDoan.getResult() == match.getResult()) {
                                    message = MESSAGE_ZALO_WINER_KET_QUA;
                                } else if (duDoan.getA() == match.getA() && duDoan.getB() == match.getB()) {
                                    message = MESSAGE_ZALO_WINER_TI_SO;
                                } else {
                                    message = MESSAGE_ZALO_LOSER;
                                }
                                message = buildMessage(match, message);

                                String uri = null;
                                try {
                                    uri = String.format(URI_ZALO_MESSAGE_SERVICE, phone, java.net.URLEncoder.encode(message,"UTF-8"));
                                } catch (UnsupportedEncodingException e) {
                                    logger.error(e.getMessage(), e);
                                }

                                duDoan.setSentZaloMessage(true);
                                duDoanDb.update(duDoan, false, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean event) {
                                        sentNext = true;
                                    }
                                });

                                if (uri == null) {
                                    container.logger().error("Can't build ZALO message.");
                                    sentNext = true;
                                    return;
                                }
                                container.logger().info("SEND ZALO MESSAGE :" + message);
                                client.get(uri, new Handler<HttpClientResponse>() {
                                    @Override
                                    public void handle(HttpClientResponse response) {

                                        response.dataHandler(new Handler<Buffer>() {
                                            @Override
                                            public void handle(Buffer buffer) {
                                                container.logger().info(phone + " ZALO Reponse " + buffer.toString());
                                                try {
                                                    JsonObject result = new JsonObject(buffer.toString());
                                                    container.logger().info("Send ZALO message result: " + result);
                                                } catch (DecodeException e) {
                                                    container.logger().info("Send ZALO message fail");
                                                    container.logger().info("Unexpected result:" + buffer.toString());
                                                }
                                            }
                                        });
                                    }
                                }).end();
                            }
                        });
                    }
                });
            }
        });
    }

    private void getMessages(Message<JsonObject> message) {
        String giaiTiso = MESSAGE_GIAI_TI_SO;
        String giaiKetQua = MESSAGE_GIAI_KET_QUA;
        String giaiTiSoVaKetQua = MESSAGE_GIAI_TI_SO_VA_KET_QUA;

        String aName = "AAA";
        String bName = "BBB";
        String status = "thắng";
        long amount = 5678;
        int a = 5;
        int b = 2;

        giaiTiso = giaiTiso.replaceAll("@aName", aName);
        giaiTiso = giaiTiso.replaceAll("@bName", bName);
        giaiTiso = giaiTiso.replaceAll("@status", status);
        giaiTiso = giaiTiso.replaceAll("@a", String.valueOf(a));
        giaiTiso = giaiTiso.replaceAll("@b", String.valueOf(b));
        giaiTiso = giaiTiso.replaceAll("@amount", String.valueOf(amount));

        giaiKetQua = giaiKetQua.replaceAll("@aName", aName);
        giaiKetQua = giaiKetQua.replaceAll("@bName", bName);
        giaiKetQua = giaiKetQua.replaceAll("@status", status);
        giaiKetQua = giaiKetQua.replaceAll("@a", String.valueOf(a));
        giaiKetQua = giaiKetQua.replaceAll("@b", String.valueOf(b));
        giaiKetQua = giaiKetQua.replaceAll("@amount", String.valueOf(amount));

        giaiTiSoVaKetQua = giaiTiSoVaKetQua.replaceAll("@aName", aName);
        giaiTiSoVaKetQua = giaiTiSoVaKetQua.replaceAll("@bName", bName);
        giaiTiSoVaKetQua = giaiTiSoVaKetQua.replaceAll("@status", status);
        giaiTiSoVaKetQua = giaiTiSoVaKetQua.replaceAll("@a", String.valueOf(a));
        giaiTiSoVaKetQua = giaiTiSoVaKetQua.replaceAll("@b", String.valueOf(b));
        giaiTiSoVaKetQua = giaiTiSoVaKetQua.replaceAll("@amount", String.valueOf(amount));

        message.reply(
                new JsonObject()
                        .putString("MESSAGE_GIAI_TI_SO", giaiTiso)
                        .putString("MESSAGE_GIAI_KET_QUA", giaiKetQua)
                        .putString("MESSAGE_GIAI_TI_SO_VA_KET_QUA", giaiTiSoVaKetQua)
        );
    }

    private void setMessages(Message<JsonObject> message) {
        MESSAGE_GIAI_TI_SO = message.body().getString("MESSAGE_GIAI_TI_SO", MESSAGE_GIAI_TI_SO);
        MESSAGE_GIAI_KET_QUA = message.body().getString("MESSAGE_GIAI_KET_QUA", MESSAGE_GIAI_KET_QUA);
        MESSAGE_GIAI_TI_SO_VA_KET_QUA = message.body().getString("MESSAGE_GIAI_TI_SO_VA_KET_QUA", MESSAGE_GIAI_TI_SO_VA_KET_QUA);
        message.reply(
                new JsonObject()
                        .putString("MESSAGE_GIAI_TI_SO", MESSAGE_GIAI_TI_SO)
                        .putString("MESSAGE_GIAI_KET_QUA", MESSAGE_GIAI_KET_QUA)
                        .putString("MESSAGE_GIAI_TI_SO_VA_KET_QUA", MESSAGE_GIAI_TI_SO_VA_KET_QUA)
        );
    }

    private void tangThuong(Message<JsonObject> message) {
        vertx.setPeriodic(50, new Handler<Long>() {
            boolean sentNext = true;

            @Override
            public void handle(final Long timerId) {
                if (sentNext == false)
                    return;
                sentNext = false;
                tangThuongDb.getTangThuong(new Handler<TangThuong>() {
                    @Override
                    public void handle(final TangThuong thuong) {
                        if (thuong == null) {
                            vertx.cancelTimer(timerId);
                            container.logger().info("SENT FINISHED");
                            sentNext = false;
                            return;
                        }
                        Integer phone = null;
                        try {
                            phone = Integer.parseInt(thuong.getModelId());
                        } catch (NumberFormatException e) {
                            sentNext = true;
                            container.logger().error("Can't parse number " + thuong.getModelId());
                            return;
                        }

                        container.logger().info("Sent money to " + phone + " amount:" + 10000);
                        transfer(phone, 10000, null, new Handler<Integer>() {
                            @Override
                            public void handle(Integer event) {
                                thuong.setSent(1);
                                tangThuongDb.update(thuong, false, null);
                                sentNext = true;
                            }
                        });
                    }
                });
            }
        });
    }

    private void testTran(final Message<JsonObject> message) {
        Integer number = message.body().getInteger("number");
        Long amount = message.body().getLong("amount");
        transfer(number, amount, null, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {
                message.reply(new JsonObject().putNumber("result", event));
            }
        });
    }

    private void makeTrans(final Message<JsonObject> message) {
        //INPUT VALIDATION
        final String matchId = message.body().getString("matchId");
        if (matchId == null || matchId.isEmpty()) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "MatchId can't be null or empty!")
            );
            return;
        }

        Match matchFilter = new Match();
        matchFilter.setModelId(matchId);
        matchDb.findOne(matchFilter, new Handler<Match>() {
            @Override
            public void handle(final Match match) {

                container.logger().info("WC START TRANSFER ");

                message.reply(new JsonObject().putNumber("error", 0).putString("desc", "Transferring,..."));

                long timerId = vertx.setPeriodic(100, new Handler<Long>() {
                    boolean doNext = true;

                    @Override
                    public void handle(final Long timerId) {
                        if (!doNext)
                            return;
                        doNext = false;
                        duDoanDb.getOneWinner(matchId, new Handler<DuDoan>() {
                            @Override
                            public void handle(final DuDoan duDoan) {
                                if (duDoan == null) {
                                    container.logger().info("WC TRAN FINISHED.");
                                    container.logger().info("stop timer: " + timerId);
                                    vertx.cancelTimer(timerId);
                                    doNext = false;
                                    return;
                                }
                                Integer phone = null;
                                try {
                                    phone = Integer.parseInt(duDoan.getModelId());
                                } catch (NumberFormatException e) {
                                    container.logger().error("WC. CAN'T PARSE NUMBER. ");
                                }

                                if (phone == null) {
                                    //TODO: KHONG CHUYEN, luu lai voi ma loi bang 9999
                                    duDoan.setsMoney(duDoan.getMoney());
                                    duDoan.setMoney(0L);
                                    duDoan.setTranError(9999);

                                    container.logger().info("WC update DuDoan: " + duDoan);
                                    duDoanDb.update(duDoan, false, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {
                                            container.logger().info("WC saved DuDoan: " + duDoan);
                                            doNext = true;
                                        }
                                    });
                                    return;
                                }

                                //TODO: THUC HIEN CHUYEN
                                container.logger().info("WC TRANSFER: " + phone + " AMOUNT:" + duDoan.getMoney());
                                final int fphone = phone;
                                transfer(phone, duDoan.getMoney(), match, new Handler<Integer>() {
                                    @Override
                                    public void handle(Integer error) {
                                        container.logger().info("WC TRANSFER FINISHED: " + fphone + " AMOUNT:" + duDoan.getMoney() + " RESULT: " + error);
                                        duDoan.setsMoney(duDoan.getMoney());
                                        duDoan.setMoney(0L);
                                        duDoan.setTranError(error);

                                        container.logger().info("WC update DuDoan: " + duDoan);
                                        duDoanDb.update(duDoan, false, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean event) {
                                                container.logger().info("WC saved DuDoan: " + duDoan);
                                                doNext = true;
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

    private void transfer1(final int number, final long amount, final Handler<Integer> callback) {
        container.logger().info("TRANSFER TO " + number + " AMOUNT " + amount);
        callback.handle(0);
    }

    private void transfer(final int number, final ThuongThem thuongThem, final Match match, final Handler<Integer> callback) {
        logger.info("WC TRANSFER " + thuongThem.money + " TO " + number);

        Buffer buffer = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                , 0
                , number
                , SoapProto.commonAdjust.newBuilder()
                        .setSource(ADJUSTMENT_ACCOUNT)
                        .setTarget("0" + number)
                        .setAmount(thuongThem.money)
                        .setPhoneNumber("0" + number)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {

                int error = result.body().getInteger(colName.TranDBCols.ERROR);

                if (error == 0) {


                    final TranObj mainObj = new TranObj();
                    mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                    mainObj.tranId = System.currentTimeMillis();
                    mainObj.clientTime = System.currentTimeMillis();
                    mainObj.ackTime = System.currentTimeMillis();
                    mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
                    mainObj.amount = thuongThem.money;
                    mainObj.status = TranObj.STATUS_OK;
                    mainObj.error = 0;
                    mainObj.cmdId = -1;
                    mainObj.billId = "-1";
                    mainObj.io = +1;
                    mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                    mainObj.category = -1;
                    mainObj.partnerName = "MoMo";

                    //MATCH INFOMATION
                    String status = "hòa";
                    if (match.getResult() < 0) {
                        status = "thua";
                    } else if (match.getResult() > 0) {
                        status = "thắng";
                    }
                    String[] arr = match.getName().split("-");
                    String aName = arr[0].trim();
                    String bName = arr[1].trim();

                    StringBuffer numbers = new StringBuffer();
                    numbers.append("0").append(thuongThem.numbers.get(0));
                    for (int i = 1; i < thuongThem.numbers.size(); i++) {
                        numbers.append(", 0").append(thuongThem.numbers.get(i));
                    }


                    mainObj.comment = MESSAGE_THUONG_THEM;

                    mainObj.comment = mainObj.comment.replaceAll("@aName", aName);
                    mainObj.comment = mainObj.comment.replaceAll("@bName", bName);
                    mainObj.comment = mainObj.comment.replaceAll("@status", status);
                    mainObj.comment = mainObj.comment.replaceAll("@a", String.valueOf(match.getA()));
                    mainObj.comment = mainObj.comment.replaceAll("@b", String.valueOf(match.getB()));
                    mainObj.comment = mainObj.comment.replaceAll("@amount", String.valueOf(thuongThem.money));
                    mainObj.comment = mainObj.comment.replaceAll("@numbers", numbers.toString());

//                    mainObj.comment = "Quà tặng bất ngờ từ MoMo: Chúc mừng bạn nhận được 10,000 VND khi đăng ký tham gia MoMo - Tỷ Phú World Cup. Hãy dự đoán các trận đấu Mexico - Cameroon, Hà Lan - Tây Ban Nha, Chile - Australia đêm 13 và rạng sáng 14/06/2014 để trúng thưởng lớn.";
                    mainObj.owner_number = number;
                    mainObj.owner_name = "MoMo";


                    container.logger().info("WC transDb.upsertTranOutSide");
                    transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean isUpdated) {
                            if(!isUpdated){
                                BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
//                                log.add("createRequestPushNotification ----------------------> ", "9");
                                vertx.eventBus().send(
                                        AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                        , NotificationUtils.createRequestPushNotification(mainObj.owner_number, 2, mainObj)
                                );
                            }
                        }
                    });
                }

                callback.handle(error);
            }
        });

    }

    private void transfer(final int number, final String transferComment, final long amount, final Handler<Integer> callback) {
        Buffer buffer = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                , 0
                , number
                , SoapProto.commonAdjust.newBuilder()
                        .setSource(ADJUSTMENT_ACCOUNT)
                        .setTarget("0" + number)
                        .setAmount(amount)
                        .setPhoneNumber("0" + number)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {

                int error = result.body().getInteger(colName.TranDBCols.ERROR);

                if (error == 0) {


                    final TranObj mainObj = new TranObj();
                    mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                    mainObj.tranId = System.currentTimeMillis();
                    mainObj.clientTime = System.currentTimeMillis();
                    mainObj.ackTime = System.currentTimeMillis();
                    mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
                    mainObj.amount = amount;
                    mainObj.status = TranObj.STATUS_OK;
                    mainObj.error = 0;
                    mainObj.cmdId = -1;
                    mainObj.billId = "-1";
                    mainObj.io = +1;
                    mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                    mainObj.category = -1;
                    mainObj.partnerName = "MoMo";


                    mainObj.comment = transferComment;

                    mainObj.owner_number = number;
                    mainObj.owner_name = "MoMo";


                    container.logger().info("WC transDb.upsertTranOutSide");
                    transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {
                            if(!result){
                                BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
//                                log.add("createRequestPushNotification ----------------------> ", "8");
                                vertx.eventBus().send(
                                        AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                        , NotificationUtils.createRequestPushNotification(mainObj.owner_number, 2, mainObj)
                                );
                            }
                        }
                    });
                }

                callback.handle(error);
            }
        });

    }

    private void transfer(final int number, final long amount, final Match match, final Handler<Integer> callback) {

        Buffer buffer = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                , 0
                , number
                , SoapProto.commonAdjust.newBuilder()
                        .setSource(ADJUSTMENT_ACCOUNT)
                        .setTarget("0" + number)
                        .setAmount(amount)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {

                int error = result.body().getInteger(colName.TranDBCols.ERROR);

                if (error == 0) {


                    final TranObj mainObj = new TranObj();
                    mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                    mainObj.tranId = System.currentTimeMillis();
                    mainObj.clientTime = System.currentTimeMillis();
                    mainObj.ackTime = System.currentTimeMillis();
                    mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
                    mainObj.amount = amount;
                    mainObj.status = TranObj.STATUS_OK;
                    mainObj.error = 0;
                    mainObj.cmdId = -1;
                    mainObj.billId = "-1";
                    mainObj.io = +1;
                    mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                    mainObj.category = -1;
                    mainObj.partnerName = "MoMo";

                    //MATCH INFOMATION
                    String status = "hòa";
                    if (match.getResult() < 0) {
                        status = "thua";
                    } else if (match.getResult() > 0) {
                        status = "thắng";
                    }
                    String[] arr = match.getName().split("-");
                    String aName = arr[0].trim();
                    String bName = arr[1].trim();


                    if (amount == match.getGiaiKetQua()) {
                        mainObj.comment = MESSAGE_GIAI_KET_QUA;
                    } else if (amount == match.getGiaiTiSo()) {
                        mainObj.comment = MESSAGE_GIAI_TI_SO;
                    } else {
                        mainObj.comment = MESSAGE_GIAI_TI_SO_VA_KET_QUA;
                    }

                    mainObj.comment = mainObj.comment.replaceAll("@aName", aName);
                    mainObj.comment = mainObj.comment.replaceAll("@bName", bName);
                    mainObj.comment = mainObj.comment.replaceAll("@status", status);
                    mainObj.comment = mainObj.comment.replaceAll("@a", String.valueOf(match.getA()));
                    mainObj.comment = mainObj.comment.replaceAll("@b", String.valueOf(match.getB()));
                    mainObj.comment = mainObj.comment.replaceAll("@amount", String.valueOf(amount));


//                    mainObj.comment = "Quà tặng bất ngờ từ MoMo: Chúc mừng bạn nhận được 10,000 VND khi đăng ký tham gia MoMo - Tỷ Phú World Cup. Hãy dự đoán các trận đấu Mexico - Cameroon, Hà Lan - Tây Ban Nha, Chile - Australia đêm 13 và rạng sáng 14/06/2014 để trúng thưởng lớn.";
                    mainObj.owner_number = number;
                    mainObj.owner_name = "MoMo";


                    container.logger().info("WC transDb.upsertTranOutSide");
                    transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean isUpdated) {
                            if(!isUpdated){
                                BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
//                                log.add("createRequestPushNotification ----------------------> ", "seven");
                                vertx.eventBus().send(
                                        AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                        , NotificationUtils.createRequestPushNotification(mainObj.owner_number, 2, mainObj)
                                );
                            }
                        }
                    });
                }

                callback.handle(error);
            }
        });

    }

    private void setUserStatus(HttpClient client, final Message<JsonObject> message) {
        final Integer number = message.body().getInteger("number");
        final Integer status = message.body().getInteger("status");

        if (number == null || status == null) {
            container.logger().error("Missing params: number or status");
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "Missing params: number or status")
            );
            return;
        }

        final String url = getSetUserStatusUrl(number, status);
        container.logger().info(number + " request Zalo: " + url);
        client.get(url, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                container.logger().info(number + " ZALO Reponse status Code" + response.statusCode());
                if (response.statusCode() != 200) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 3)
                                    .putString("desc", "Unexpected result.")
                    );
                    container.logger().error("[WC] GET " + url + " response unexpected status code: " + response.statusCode());
                    container.logger().error(response.statusMessage());
                    return;
                }

                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        container.logger().info(number + " ZALO Reponse " + buffer.toString());
                        try {
                            JsonObject result = new JsonObject(buffer.toString());
                            JsonObject json = new JsonObject()
                                    .putNumber("error", 0)
                                    .putObject("result", result);

                            message.reply(json);
                        } catch (DecodeException e) {
                            container.logger().warn(String.format("Request[%s}: Unexpected response : %s", url, String.valueOf(buffer)));
                            message.reply(
                                    new JsonObject()
                                            .putNumber("error", 3)
                                            .putString("desc", "Unexpected result.")
                            );
                        }
                    }
                });
            }
        }).end();
    }


    private void isMomoer(int number, final Handler<Boolean> callback) {
        MomoMessage momoMessage = new MomoMessage(SoapProto.MsgType.CHECK_USER_STATUS_VALUE,0,number, "".getBytes());
        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, momoMessage.toBuffer(), new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> response) {
                MomoMessage momo = MomoMessage.fromBuffer(response.body());
                try {
                    MomoProto.RegStatus status =  MomoProto.RegStatus.parseFrom(momo.cmdBody);

                    callback.handle(status.getIsReged());
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                    callback.handle(false);
                }
            }
        });
    }

    public void setZaloStatus(final HttpClient client, final int number, int status, final Handler<JsonObject> callback) {

        final String url = getSetUserStatusUrl(number, status);
        container.logger().info(number + " request Zalo: " + url);
        client.get(url, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                container.logger().info(number + " ZALO Reponse status Code" + response.statusCode());
                if (response.statusCode() != 200) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", 3)
                                    .putString("desc", "Unexpected result.")
                    );
                    container.logger().error("[WC] GET " + url + " response unexpected status code: " + response.statusCode());
                    container.logger().error(response.statusMessage());
                    return;
                }

                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        container.logger().info(number + " ZALO Reponse " + buffer.toString());
                        try {
                            JsonObject result = new JsonObject(buffer.toString());
                            JsonObject json = new JsonObject()
                                    .putNumber("error", 0)
                                    .putNumber("zaloError", result.getNumber("ErrorCode", -100));

                            callback.handle(json);
                        } catch (DecodeException e) {
                            container.logger().warn(String.format("Request[%s}: Unexpected response : %s", url, String.valueOf(buffer)));
                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", 3)
                                            .putString("desc", "Unexpected result.")
                            );
                        }
                    }
                });
            }
        }).end();
    }

    public void getZaloStatus(final HttpClient client, final int number, final Handler<JsonObject> callback) {
        //Test
        /*if(1==1) {
            callback.handle(new JsonObject().putNumber("error",0).putNumber("status", 1));
            return;
        }*/

        final String url = getGetUserStatusUrl(number);

        container.logger().info(number + " request ZALO: " + url);
        client.get(url, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse response) {
                if (response.statusCode() != 200) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", 3)
                                    .putString("desc", "Unexpected result.")
                    );
                    container.logger().error("[WC] GET " + url + " response unexpected status code: " + response.statusCode());
                    container.logger().error(response.statusMessage());
                    return;
                }

                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        container.logger().info(number + " ZALO Reponse " + buffer.toString());
                        try {
                            JsonArray result = new JsonArray(buffer.toString());
                            if (result.size() == 0) {
                                container.logger().error(String.format("Request[%s}: Unexpected response : %s", url, String.valueOf(buffer)));
                                JsonObject json = new JsonObject()
                                        .putNumber("error", 3)
                                        .putString("desc", "Unexpected result.");
                                callback.handle(json);
                                return;
                            }
                            JsonObject json = result.get(0);
                            Integer status = json.getInteger("Status");
                            if (status == null) {
                                container.logger().error(String.format("Request[%s}: Has no 'Status' : %s", url, String.valueOf(buffer)));
                                callback.handle(
                                        new JsonObject()
                                                .putNumber("error", 3)
                                                .putString("desc", "Unexpected result.")
                                );
                                return;
                            }
                            JsonObject response = new JsonObject()
                                    .putNumber("error", 0)
                                    .putNumber("status", status);
                            callback.handle(response);
                        } catch (DecodeException e) {
                            container.logger().error(String.format("Request[%s}: Unexpected response : %s", url, String.valueOf(buffer)));
                            callback.handle(
                                    new JsonObject()
                                            .putNumber("error", 3)
                                            .putString("desc", "Unexpected result.")
                            );
                        }
                    }
                });
            }
        }).end();
        ;
    }

    private void getUserStatus(final HttpClient client, final Message<JsonObject> message) {
        final Integer number = message.body().getInteger("number");
        if (number == null) {
            container.logger().error("Missing params: number");
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "Missing params: Number")
            );
            return;
        }

        isMomoer(number, new Handler<Boolean>() {
            @Override
            public void handle(Boolean isMomoer) {
                if(!isMomoer) {
                    message.reply(new JsonObject()
                            .putNumber("error", 0)
                            .putNumber("status", -1000)
                            .putString("desc", "Number is not momoer."));
                    return;
                }



                final String url = getGetUserStatusUrl(number);

                container.logger().info(number + " request ZALO: " + url);
                client.get(url, new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(final HttpClientResponse response) {
                        if (response.statusCode() != 200) {
                            message.reply(
                                    new JsonObject()
                                            .putNumber("error", 3)
                                            .putString("desc", "Unexpected result.")
                            );
                            container.logger().error("[WC] GET " + url + " response unexpected status code: " + response.statusCode());
                            container.logger().error(response.statusMessage());
                            return;
                        }

                        response.dataHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                container.logger().info(number + " ZALO Reponse " + buffer.toString());
                                try {
                                    JsonArray result = new JsonArray(buffer.toString());
                                    if (result.size() == 0) {
                                        container.logger().error(String.format("Request[%s}: Unexpected response : %s", url, String.valueOf(buffer)));
                                        JsonObject json = new JsonObject()
                                                .putNumber("error", 3)
                                                .putString("desc", "Unexpected result.");
                                        message.reply(json);
                                        return;
                                    }
                                    JsonObject json = result.get(0);
                                    Integer status = json.getInteger("Status");
                                    if (status == null) {
                                        container.logger().error(String.format("Request[%s}: Has no 'Status' : %s", url, String.valueOf(buffer)));
                                        message.reply(
                                                new JsonObject()
                                                        .putNumber("error", 3)
                                                        .putString("desc", "Unexpected result.")
                                        );
                                        return;
                                    }
                                    JsonObject response = new JsonObject()
                                            .putNumber("error", 0)
                                            .putNumber("status", status);
                                    message.reply(response);
                                } catch (DecodeException e) {
                                    container.logger().error(String.format("Request[%s}: Unexpected response : %s", url, String.valueOf(buffer)));
                                    message.reply(
                                            new JsonObject()
                                                    .putNumber("error", 3)
                                                    .putString("desc", "Unexpected result.")
                                    );
                                }
                            }
                        });
                    }
                }).end();;


            }
        });


    }

    public String getGetUserStatusUrl(int phoneNumber) {
        return URI_GET_USER_STATUS + "?phone=" + phoneNumber;
    }

    public String getSetUserStatusUrl(int phoneNumber, int status) {
        return URI_SET_USER_STATUS + "?phone=" + phoneNumber + "&status=" + status;
    }

    private void transReset(final Message<JsonObject> message) {
        //INPUT VALIDATION
        final String matchId = message.body().getString("matchId");
        if (matchId == null || matchId.isEmpty()) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "MatchId can't be null or empty!")
            );
            return;
        }

        DuDoan filter = new DuDoan();
        filter.setMatchId(matchId);

        DuDoan newValue = new DuDoan();
        newValue.setMoney(0L);
        newValue.setTranError(-1);
        duDoanDb.updateMulti(filter, newValue, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                message.reply(
                        new JsonObject()
                                .putNumber("error", 0)
                                .putString("desc", "Successfully!")
                );
                return;
            }
        });
    }

    private void transInit(final Message<JsonObject> message) {
        //INPUT VALIDATION
        final String matchId = message.body().getString("matchId");
        if (matchId == null || matchId.isEmpty()) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "MatchId can't be null or empty!")
            );
            return;
        }


        Match filter = new Match();
        filter.setModelId(matchId);
        matchDb.findOne(filter, new Handler<Match>() {
            @Override
            public void handle(final Match match) {
                if (match == null ) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 3)
                                    .putString("desc", "MatchId doesn't exist!")
                    );
                    return;
                }
                if (match.getResult() == null || match.getA() == null || match.getB() == null) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 4)
                                    .putString("desc", "Match doesn't not have result.")
                    );
                    return;
                }
                if (match.getGiaiKetQua() == null || match.getGiaiTiSo() == null) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 5)
                                    .putString("desc", "Match was not finished. call matchFinish first!")
                    );
                    return;
                }

                //TODO Kiem tra xem da clean danh sach nhan thuong chua.
                duDoanDb.getOneWinner(matchId, new Handler<DuDoan>() {
                    @Override
                    public void handle(DuDoan winner) {
                        if (winner != null) {
                            message.reply(new JsonObject().putNumber("error", 6).putString("desc", "Init winner list first."));
                            return;
                        }
                        //DANH SACH TRA THUONG DA DC CLEAR.

                        DuDoan filter = new DuDoan();
                        filter.setMatchId(match.getModelId());
                        filter.setResult(match.getResult());

                        DuDoan increaseValue = new DuDoan();
                        increaseValue.setMoney(match.getGiaiKetQua());

                        duDoanDb.increase(filter, increaseValue, new Handler<Long>() {
                            @Override
                            public void handle(Long event) {

                                DuDoan filter = new DuDoan();
                                filter.setMatchId(match.getModelId());
                                filter.setA(match.getA());
                                filter.setB(match.getB());

                                DuDoan increaseValue = new DuDoan();
                                increaseValue.setMoney(match.getGiaiTiSo());

                                duDoanDb.increase(filter, increaseValue, new Handler<Long>() {
                                    @Override
                                    public void handle(Long event) {
                                        message.reply(
                                                new JsonObject()
                                                        .putNumber("error", 0)
                                                        .putString("desc", "Successfully!")
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

    private void matchFinish(final Message<JsonObject> message) {

        //INPUT VALIDATION
        String matchId = message.body().getString("matchId");
        if (matchId == null || matchId.isEmpty()) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "MatchId can't be null or empty!")
            );
            return;
        }
        final Integer mResult = message.body().getInteger("result");
        if (mResult == null) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "result can't be null or empty!")
            );
            return;
        }

        final Integer mA = message.body().getInteger("a");
        if (mA == null) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "a can't be null or empty!")
            );
            return;
        }

        final Integer mB = message.body().getInteger("b");
        if (mB == null) {
            message.reply(
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "b can't be null or empty!")
            );
            return;
        }


        Match filter = new Match();
        filter.setModelId(matchId);
        //TODO FIND MATCH
        matchDb.findOne(filter, new Handler<Match>() {
            @Override
            public void handle(final Match match) {
                if (match == null) {
                    message.reply(
                            new JsonObject()
                                    .putNumber("error", 3)
                                    .putString("desc", "MatchId doesn't exist!")
                    );
                    return;
                }

                match.setResult(mResult);
                match.setA(mA);
                match.setB(mB);

                matchDb.update(match, false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
                        countMatchResult(message, match);
                    }
                });
            }
        });
    }

    private void countMatchResult(final Message<JsonObject> request, final Match match) {
        final DuDoan duDoan = new DuDoan();
        duDoan.setMatchId(match.getModelId());
        //TODO: COUNT NGUOI CHOI
        duDoanDb.count(duDoan, new Handler<Long>() {
            @Override
            public void handle(final Long soNguoiChoi) {
                DuDoan filter = new DuDoan();
                filter.setMatchId(match.getModelId());
                filter.setResult(match.getResult());
                //TODO: COUNT DUNG KET QUA
                duDoanDb.count(filter, new Handler<Long>() {
                    @Override
                    public void handle(final Long soNguoiDungKetQua) {
                        DuDoan filter = new DuDoan();
                        filter.setMatchId(match.getModelId());
                        filter.setA(match.getA());
                        filter.setB(match.getB());
                        //TODO: COUNT DUNG TI SO
                        duDoanDb.count(filter, new Handler<Long>() {
                            @Override
                            public void handle(Long soNguoiDungTiSo) {
                                Long giaiDungKetQua = soNguoiDungKetQua != 0 ? ((soNguoiChoi * 500)) / soNguoiDungKetQua : 0;
                                Long giaiDungTiSo = soNguoiDungTiSo != 0 ? ((soNguoiChoi * 500)) / soNguoiDungTiSo : 0;

                                match.setSoNguoiChoi(soNguoiChoi);
                                match.setDungKetQua(soNguoiDungKetQua);
                                match.setDungTiSo(soNguoiDungTiSo);
                                match.setGiaiKetQua(giaiDungKetQua);
                                match.setGiaiTiSo(giaiDungTiSo);

                                if(soNguoiChoi>0){
                                    match.setPhanTramKetQua((Double.valueOf(soNguoiDungKetQua) / soNguoiChoi * 100));
                                    match.setPhanTramTiSo(Double.valueOf(soNguoiDungTiSo) / soNguoiChoi * 100);
                                }

                                matchDb.update(match, false, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean event) {
                                        request.reply(new JsonObject().putNumber("error", 0).putString("desc", "Successfully"));
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
