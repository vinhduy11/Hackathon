package com.mservice.momo.vertx;

import com.google.protobuf.Internal;
import com.mservice.momo.data.TranStatusConfigDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.TranHisUtils;
import com.mservice.momo.vertx.models.TranStatusConfig;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by nam on 8/7/14.
 */
public class ConfigVerticle extends Verticle {
    public static final String CMD_GET_ALL_TRANSFER_CONFIG = "getAllTransferConfig";
    public static final String CMD_SET_ALL_TRANSFER_CONFIG = "setAllTransferConfig";
    public static final String CMD_GET_TRANSFER_CONFIG = "getTransferConfig";
    public static final String CMD_SET_TRANSFER_CONFIG = "setTransferConfig";

    public static final String CMD_RELOAD_CONFIG = "reloadConfig";

    private Map<Integer, TranStatusConfig> tranConfig;

    private TranStatusConfigDb tranStatusConfigDb;

    @Override
    public void start() {

        tranStatusConfigDb = new TranStatusConfigDb(vertx, container);

//        vertx.setPeriodic(1000, new Handler<Long>() {
//            @Override
//            public void handle(Long aLong) {
//                TranHisUtils.getTranStatusStatus(vertx, 1, new Handler<Integer>() {
//                    @Override
//                    public void handle(Integer status) {
//                        System.out.println("status: " + status);
//                        TranHisUtils.isTranStatusOn(vertx, 1, new Handler<Boolean>() {
//                            @Override
//                            public void handle(Boolean on) {
//                                System.out.println("on: " + on);
//                            }
//                        });
//                    }
//                });
//            }
//        });

//        checkStatusListAndInsert();
        vertx.setTimer(4000, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {
                        readConfigFromDatabase();
                    }
                }
        );

        vertx.eventBus().registerLocalHandler(AppConstant.ConfigVerticle, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> msg) {
                String cmd = msg.body().getString("cmd");
                switch (cmd) {
                    case CMD_GET_TRANSFER_CONFIG:
                        getTransferConfig(msg);
                        break;
                    case CMD_SET_TRANSFER_CONFIG:
                        setTranserConfig(msg);
                        break;
                    case CMD_GET_ALL_TRANSFER_CONFIG:
                        getAllTransferConfig(msg);
                        break;
                    case CMD_SET_ALL_TRANSFER_CONFIG:
                        setAllTransferConfig(msg);
                        break;
                }
            }
        });

        vertx.eventBus().registerHandler(AppConstant.ConfigVerticlePublish, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> msg) {
                String cmd = msg.body().getString("cmd");
                switch (cmd) {
                    case CMD_RELOAD_CONFIG:
                        readConfigFromDatabase();
                        break;
                }
                msg.reply(msg.body());
            }
        });

//        TranStatusConfig config;
//        MomoProto.TranHisV1.TranType.valueOf(1).getValueDescriptor();
//        config = new TranStatusConfig(MomoProto.TranHisV1.TranType., "[M2M] chuyển tiền", 1);
//        tranStatusConfigDb.save(config, null);
    }

    private void checkStatusListAndInsert() {
        for (int i = 1; i < 300; i++) {
            final MomoProto.TranHisV1.TranType tranType = MomoProto.TranHisV1.TranType.valueOf(i);
            if (tranType == null)
                continue;
            final TranStatusConfig t = new TranStatusConfig();
            t.tranType = tranType.getNumber();
            tranStatusConfigDb.findOne(t, new Handler<TranStatusConfig>() {
                @Override
                public void handle(TranStatusConfig tranStatusConfig) {
                    if (tranStatusConfig == null) {
                        String tranName = tranType.name() + " - ";
                        switch (tranType) {
                            case BANK_IN:
                                tranName += "Nạp tiền từ ngân hàng liên kết";
                                break;
                            case BANK_OUT:
                                tranName += "Rút tiền từ ngân hàng liên kết";
                                break;
                            case TOP_UP:
                                tranName += "Nạp tiền điện thoại";
                                break;
                            case TOP_UP_GAME:
                                tranName += "Nạp tiền Games";
                                break;
                            case M2C:
                                tranName += "Chuyển tiền M2C";
                                break;
                            case M2M:
                                tranName += "Chuyển tiền MoMo sang MoMo";
                                break;
                            case PAY_ONE_BILL:
                                tranName += "Thanh toán hóa đơn";
                                break;
                            case QUICK_PAYMENT:
                                tranName += "";
                                break;
                            case QUICK_DEPOSIT:
                                tranName += "";
                                break;
                            case BANK_NET_TO_MOMO:
                                tranName += "Nạp tiền từ thẻ ATM thông qua cổng Banknet";
                                break;
                            case BANK_NET_VERIFY_OTP:
                                tranName += "Verify banknet otp";
                                break;
                            case PAY_ONE_BILL_OTHER:
                                tranName += "Thanh toán các hóa đơn khác.";
                                break;
                            case TRANSFER_MONEY_TO_PLACE:
                                tranName += "Rút tiền tại điểm giao dịch";
                                break;
                            case BILL_PAY_TELEPHONE:
                                tranName += "Thanh toán hóa đơn điện thoại";
                                break;
                            case BILL_PAY_TICKET_AIRLINE:
                                tranName += "Thanh toán vé máy bay";
                                break;
                            case BILL_PAY_TICKET_TRAIN:
                                tranName += "Thanh toán vé tàu hỏa";
                                break;
                            case BILL_PAY_INSURANCE:
                                tranName += "Thanh toán tiền bảo hiểm";
                                break;
                            case BILL_PAY_INTERNET:
                                tranName += "Thanh toán cước Internet";
                                break;
                            case BILL_PAY_OTHER:
                                tranName += "Thanh toán các hóa đơn khác.";
                                break;
                            case DEPOSIT_CASH_OTHER:
                                tranName += "";
                                break;
                            case BUY_MOBILITY_CARD:
                                tranName += "Mua card điện thoại";
                                break;
                            case BUY_GAME_CARD:
                                tranName += "Mua card game";
                                break;
                            case BUY_OTHER:
                                tranName += "";
                                break;
                            case DEPOSIT_CASH:
                                tranName += "";
                                break;
                            case BILL_PAY_CINEMA:
                                tranName += "Thanh toán vé xem phim";
                                break;
                            case MOMO_TO_BANK_MANUAL:
                                tranName += "Rút tiền ra ngân hàng (manually)";
                                break;
                            case DEPOSIT_AT_HOME:
                                tranName += "Nạp tiền tận nơi";
                                break;
                            case WITHDRAW_AT_HOME:
                                tranName += "Rút tiền tận nơi";
                                break;
                            case BONUS:
                                tranName += "";
                                break;
                            case FEE:
                                tranName += "";
                                break;
                            case PHIM123:
                                tranName += "Thanh toán vé xem phim qua cổng 123PHim";
                                break;
                            case PAY_NUOCCL_BILL:
                                tranName += "Thanh toán hóa đơn nước chợ lớn";
                                break;
                            case PAY_AVG_BILL:
                                tranName += "Thanh toán há đơn truyền hình An Viên";
                                break;
                        }
                        t.name = tranName;
                        t.status = 1;
                        tranStatusConfigDb.save(t, new Handler<String>() {
                            @Override
                            public void handle(String s) {
                                container.logger().info("INSERT NEW TRANS STATUS: " + t);
                            }
                        });
                    }
                }
            });
        }
    }

    private void setAllTransferConfig(final Message<JsonObject> msg) {
        JsonObject json = msg.body().getObject("values");
        for (String fieldName: json.getFieldNames()) {
            try {
                Integer id = Integer.parseInt(fieldName);
                Integer status = Integer.parseInt(json.getString(fieldName));
                TranStatusConfig t = tranConfig.get(id);
                t.status = status;
            } catch (NumberFormatException e) {
                container.logger().error("can't parse setting values: " + e.getMessage());
            }
        }
        updateAllTranStatus(new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                JsonObject request = new JsonObject();
                request.putString("cmd", CMD_RELOAD_CONFIG);
//                vertx.eventBus().publish(AppConstant.ConfigVerticlePublish, request);
                vertx.eventBus().publish(AppConstant.HTTP_POST_BUS_ADDRESS,
                        Misc.makeHttpPostWrapperData(AppConstant.ConfigVerticlePublish, request));

                msg.reply(
                        new JsonObject().putNumber("error", 0)
                        .putString("desc", "Successfully")
                );
            }
        });
    }

    private void updateAllTranStatus(final Handler<Boolean> callback) {
        final AtomicInteger count = new AtomicInteger(0);
        for(TranStatusConfig t: tranConfig.values()) {
            tranStatusConfigDb.update(t, false, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    count.incrementAndGet();
                    if (count.get() == tranConfig.size()) {
                        callback.handle(true);
                    }
                }
            });
        }
    }

    private void getAllTransferConfig(Message<JsonObject> msg) {
        JsonArray arr = new JsonArray();
        for (TranStatusConfig transfer : tranConfig.values()) {
            arr.add(transfer.getPersisFields());
        }
        msg.reply(arr);
    }

    private void setTranserConfig(final Message<JsonObject> msg) {
        Integer tranType = msg.body().getInteger("tranType");
        Integer status = msg.body().getInteger("status");
        if (tranType == null || status == null) {
            throw new IllegalArgumentException("request body must contain 'tranType' & 'status'. ");
        }
        TranStatusConfig tranferStatusConfig = tranConfig.get(tranType);
        if (tranferStatusConfig == null) {
            msg.reply(
                    new JsonObject().putNumber("error", 1).putString("desc", "TranType doesn't exist.")
            );
            return;
        }

        tranferStatusConfig.status = status;
        tranStatusConfigDb.update(tranferStatusConfig, false, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                msg.reply(
                        new JsonObject().putNumber("error", 0).putString("desc", "Successful")
                );
            }
        });

    }

    private void readConfigFromDatabase() {
        TranStatusConfig filter = new TranStatusConfig();
        container.logger().info("READING SERVICE STATUS FROM DATABASE");
        tranStatusConfigDb.find(filter, 500, new Handler<List<TranStatusConfig>>() {
            @Override
            public void handle(List<TranStatusConfig> tranferStatusConfigs) {
                Map<Integer, TranStatusConfig> temp = new HashMap<Integer, TranStatusConfig>();
                for (TranStatusConfig tranferStatusConfig : tranferStatusConfigs) {
                    temp.put(tranferStatusConfig.tranType, tranferStatusConfig);
                    container.logger().info("SERVICE STATUS: " + tranferStatusConfig);
                }
                tranConfig = temp;
            }
        });
    }

    private void getTransferConfig(Message<JsonObject> msg) {
        Integer tranType = msg.body().getInteger("tranType");
        if (tranType == null) {
            throw new IllegalArgumentException("request body must contain 'tranType'. ");
        }
        TranStatusConfig tranferStatusConfig = tranConfig.get(tranType);
        int status;
        if (tranferStatusConfig == null)
            status = -1;
        else
            status = tranferStatusConfig.status;
        msg.reply(
                new JsonObject()
                        .putNumber("status", status)
                        .putNumber("tranType", tranType)
        );
    }
}
