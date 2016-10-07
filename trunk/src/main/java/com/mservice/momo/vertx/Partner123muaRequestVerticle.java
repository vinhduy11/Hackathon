package com.mservice.momo.vertx;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.Partner123MuaOrderDb;
import com.mservice.momo.data.Partner123MuaRequestFailDb;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.vertx.models.Mua123OrderInfo;
import com.mservice.momo.vertx.models.result.Pay123MuaOrderResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by nam on 4/16/14.
 */
public class Partner123muaRequestVerticle extends Verticle {
    public static String VERTICLE_ADDRESS = "momo.PartnerRequestVerticle";

    public static final int CMD_SEND_GET_ORDER = 1;
    public static final int CMD_SEND_PAY_ORDER = 2;
    public static final int CMD_PAY_ORDER = 3;

    public static String PARTNER_HOST = "localhost";
    public static int PARTNER_PORT = 18080;

    public static String PARTNER_PASS = "123mua-MoMo";

    public static String URI_GET_ORDER_INFO = "/api/order";
    public static String URI_ORDER_PAY = "/api/pay";

    private Partner123MuaRequestFailDb paidDb;

    @Override
    public void start() {
        final Logger logger = container.logger();
        JsonObject globalConfig = container.config();

        JsonObject config = globalConfig.getObject("partner123muaRequestVerticle");

        if (config.getBoolean("active", false)) {
            logger.info("Partner123muaRequestVerticle was DEACTIVATED!");
            return;
        }

        PARTNER_HOST = config.getString("host", "");
        PARTNER_PORT = config.getInteger("port", 80);
        URI_GET_ORDER_INFO = config.getString("urlGetOrderInfo", "");
        URI_ORDER_PAY = config.getString("urlSetOrderPay", "");


        final HttpClient client = vertx.createHttpClient()
                .setHost(PARTNER_HOST)
                .setPort(PARTNER_PORT)
                .setMaxPoolSize(10)
                .setConnectTimeout(15000);


        final Partner123MuaOrderDb mua123Db = new Partner123MuaOrderDb(vertx.eventBus(), container.logger());
        paidDb = new Partner123MuaRequestFailDb(vertx.eventBus(), container.logger());

        vertx.eventBus().registerLocalHandler(VERTICLE_ADDRESS, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> request) {
                JsonObject msg = request.body();

                Integer cmdId = msg.getInteger("cmdId");

                if (cmdId == null) {
                    request.reply();
                    return;
                }
                switch (cmdId.intValue()) {
                    case CMD_PAY_ORDER:
                        payOrder(mua123Db, client, request);
                        break;
                    case CMD_SEND_GET_ORDER:
                        getOrderByID(client, request);
                        break;
                    case CMD_SEND_PAY_ORDER:
                        setOrderPay(client, request);
                        break;
                    default:
                        logger.error("[Partner123muaRequestVerticle] Unknown command");
                }

            }
        });
    }

    private void payOrder(final Partner123MuaOrderDb mua123Db, final HttpClient client, final Message<JsonObject> request) {
        final int phoneNumber = request.body().getInteger("phoneNumber", -1);
        final String orderId= request.body().getString("orderId");
        final long amount = request.body().getLong("amount", 0L);
        final String pin = request.body().getString("pin");

        if (phoneNumber < 0 || orderId == null || orderId.trim().length() == 0 || amount == 0L || pin == null || pin.trim().length() == 0) {
            container.logger().warn("Invalid command :" + request.body());
            request.reply(
                    new JsonObject()
                            .putString("result", Pay123MuaOrderResult.INVALID_INPUTS.toString())
            );
            return;
        }

        container.logger().trace("checkPaidOrderInDatabase()");
        checkPaidOrderInDatabase(orderId, mua123Db, paidDb, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject obj) {
                int result = obj.getInteger("result", -1);

                if (result == -1) { //System error
                    request.reply(
                            new JsonObject()
                                    .putString("result", Pay123MuaOrderResult.SYSTEM_ERROR.toString())
                    );
                    return;
                }
                if (result == 1) { // paid order
                    container.logger().debug(String.format("%d request pay a paid order(id:%s)", phoneNumber, orderId));
                    request.reply(
                            new JsonObject()
                                    .putString("result", Pay123MuaOrderResult.PAID_ORDER.toString())
                    );
                    return;
                }


                container.logger().trace("requestOrderInfo()");
                requestOrderInfo(client, orderId, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject result) {
                        if (result == null) {
                            request.reply(
                                    new JsonObject()
                                            .putString("result", Pay123MuaOrderResult.CANT_GET_ORDER_INFO.toString())
                            );
                            return;
                        }
                        final Mua123OrderInfo info = new Mua123OrderInfo(result);

                        if (info.code < 0) { //error
                            if (info.code == Mua123OrderInfo.CODE_ORDER_NOT_EXIST) {
                                request.reply(
                                        new JsonObject()
                                                .putString("result", Pay123MuaOrderResult.INVALID_ORDER_CODE.toString())
                                );
                                return;
                            }
                            String logMessage = String.format("[123Mua payment] [order: %s] %s", orderId, Mua123OrderInfo.codeName(info.code));
                            container.logger().info(logMessage);
                            request.reply(
                                    new JsonObject()
                                            .putString("result", Pay123MuaOrderResult.CANT_GET_ORDER_INFO.toString())
                            );
                            return;
                        }

                        if (info.status == Mua123OrderInfo.STATUS_PAID) {
                            request.reply(
                                    new JsonObject()
                                            .putString("result", Pay123MuaOrderResult.PAID_ORDER.toString())
                            );
                            return;
                        }

                        if (amount != info.deposit.longValue()) {
                            String logMessage = String.format("[Partner123RequestVerticle] [requestOrderInfo] [order: %s] amount was %d, but it must be %d", orderId, amount, info.deposit.longValue());
                            container.logger().info(logMessage);
                            request.reply(
                                    new JsonObject()
                                            .putString("result", Pay123MuaOrderResult.AMOUNT_NOT_MATCH.toString())
                                            .putNumber("amount", info.deposit.longValue())
                            );
                            return;
                        }

                        final int receiverPhone;
                        try {
                            receiverPhone = Integer.valueOf(info.shopPhone);
                        } catch (NumberFormatException e) {
                            //shopPhone number is invalid
                            container.logger().warn(String.format("[Partner123RequestVerticle] [requestOrderInfo] [orderId: %d] shopPhone number is invalid :%s", info.code, info.shopPhone));
                            //TODO: Notify can't pay order
                            request.reply(
                                    new JsonObject()
                                            .putString("result", Pay123MuaOrderResult.ORDER_INVALID_INFO_SHOP_PHONE.toString())
                            );
                            return;
                        }

                        CmdModels.GetAgentInfo.Builder agentInfoQuery = CmdModels.GetAgentInfo.newBuilder()
                                .setPhoneNumber(phoneNumber);
                        MomoCommand momoCommand = new MomoCommand(CmdModels.CommandType.GET_AGENT_INFO, agentInfoQuery.build());

                        container.logger().debug("[Partner123mua]>[CommandVerticle] " + agentInfoQuery);
                        vertx.eventBus().send(AppConstant.CommandVerticle_ADDRESS, MomoCommand.toBuffer(momoCommand), new Handler<Message<Object>>() {
                            @Override
                            public void handle(Message<Object> agentInfoCommandResult) {
                                byte[] arr = (byte[]) agentInfoCommandResult.body();
                                Buffer buffer = new Buffer(arr);

                                MomoCommand cmd = null;
                                try {
                                    cmd = MomoCommand.fromBuffer(buffer);
                                    if (cmd.getCommandType() == CmdModels.CommandType.GET_AGENT_INFO_REPLY) {
                                        CmdModels.GetAgentInfoReply agentInfo = (CmdModels.GetAgentInfoReply) cmd.getBody();


                                        if ("".equals(agentInfo.getName()) && agentInfo.getMomo() == 0 && agentInfo.getIsNamedAccount() == false) {
                                            request.reply(
                                                    new JsonObject()
                                                            .putString("result", Pay123MuaOrderResult.USER_NOT_REGISTERED.toString())
                                            );
                                            return;
                                        }

                                        if (agentInfo.getMomo() < info.deposit.longValue()) {
                                            request.reply(
                                                    new JsonObject()
                                                            .putString("result", Pay123MuaOrderResult.NOT_ENOUGH_MONEY.toString())
                                            );
                                            return;
                                        }

                                        CmdModels.Pay123MuaOrder.Builder cmdBody = CmdModels.Pay123MuaOrder.newBuilder()
                                                .setAmount(amount)
                                                .setPhoneNumber(phoneNumber)
                                                .setReceiverPhoneNumber(receiverPhone)
                                                .setPin(pin)
                                                .setOrderId(orderId)
                                                .setComment(String.format("Thanh toan coc cho don hang %s", orderId, amount));

                                        MomoCommand momoCommand = new MomoCommand(CmdModels.CommandType.PAY_123MUA_ORDER, cmdBody.build());

                                        container.logger().debug("[Partner123mua]>[CommandVerticle] " + momoCommand);
                                        vertx.eventBus().send(AppConstant.CommandVerticle_ADDRESS, MomoCommand.toBuffer(momoCommand), new Handler<Message<Object>>() {
                                            @Override
                                            public void handle(Message<Object> message) {
                                                byte[] arr = (byte[]) message.body();
                                                Buffer buffer = new Buffer(arr);
                                                try {
                                                    MomoCommand cmd = MomoCommand.fromBuffer(buffer);

                                                    if (cmd.getCommandType() == CmdModels.CommandType.PAY_123MUA_ORDER_REPLY) {
                                                        final CmdModels.Pay123MuaOrderReply reply = (CmdModels.Pay123MuaOrderReply) cmd.getBody();

                                                        if (reply.getResult() == CmdModels.Pay123MuaOrderReply.Result.SUCCESS) {

                                                            container.logger().info(String.format("Thanh toan thanh cong don dat hang 123mua (orderId %s). Adjustment (%d->%d, %d)", orderId, phoneNumber, receiverPhone, amount));
                                                            //TODO: Insert Order into database
                                                            Partner123MuaOrderDb.Order order = new Partner123MuaOrderDb.Order(orderId, System.currentTimeMillis(), Partner123MuaOrderDb.Order.STATUS_PAID, amount);

                                                            mua123Db.saveOrder(order, new Handler<String>() {
                                                                @Override
                                                                public void handle(String event) {

                                                                }
                                                            });

                                                            //TODO: Reply success
                                                            request.reply(
                                                                    new JsonObject()
                                                                            .putString("result", Pay123MuaOrderResult.SUCCESS.toString())
                                                                            .putObject("orderInfo", result)
                                                            );


                                                            //TODO: Notify 123mua: Order was paid
                                                            container.logger().trace("Partner123mua>requestPayOrder() ");
                                                            requestPayOrder(client, orderId, new Handler<JsonObject>() {
                                                                @Override
                                                                public void handle(JsonObject response) {
                                                                    int code = Integer.MAX_VALUE;
                                                                    if (response != null) {
                                                                        code = response.getInteger("code", Integer.MAX_VALUE);
                                                                    }
                                                                    if (code != 0) {
                                                                        //TODO: Save error request to database
                                                                        String tranId = reply.getTransactionId();
                                                                        String payPhone = String.valueOf(phoneNumber);
                                                                        Partner123MuaRequestFailDb.PaidOrder order = new Partner123MuaRequestFailDb.PaidOrder(orderId, tranId, info.buyerPhone, info.shopPhone, payPhone, amount, "" + code, System.currentTimeMillis());

                                                                        paidDb.saveOrder(order, new Handler<String>() {
                                                                                    @Override
                                                                                    public void handle(String event) {

                                                                                    }
                                                                                }
                                                                        );
                                                                    }
                                                                }
                                                            });
                                                            return;
                                                        }


                                                        if (reply.getResult() == CmdModels.Pay123MuaOrderReply.Result.ACCESS_DENIED) {
                                                            request.reply(
                                                                    new JsonObject()
                                                                            .putString("result", Pay123MuaOrderResult.ACCESS_DENIED.toString())
                                                            );
                                                            return;
                                                        }

                                                        // Can't transfer
                                                        request.reply(
                                                                new JsonObject()
                                                                        .putString("result", Pay123MuaOrderResult.TRANSFER_ERROR.toString())
                                                        );
                                                    }
                                                    // Can't transfer
                                                    //TODO: Send Sms notifications: Can't transfer

                                                    CmdModels.Error reply = (CmdModels.Error) cmd.getBody();

                                                    if (reply.getCode() == 100 || reply.getCode() == 1014) {
                                                        request.reply(
                                                                new JsonObject()
                                                                        .putString("result", Pay123MuaOrderResult.INVALID_PIN_NUMBER.toString())
                                                        );
                                                        return;
                                                    }

                                                    if (reply.getCode() == 14) {
                                                        request.reply(
                                                                new JsonObject()
                                                                        .putString("result", Pay123MuaOrderResult.TARGET_NOT_REGISTERED.toString())
                                                        );
                                                        return;
                                                    }

                                                    request.reply(
                                                            new JsonObject()
                                                                    .putString("result", Pay123MuaOrderResult.TRANSFER_EXCEPTION.toString())
                                                                    .putNumber("errorCode", reply.getCode())
                                                    );

                                                } catch (InvalidProtocolBufferException e) {
                                                    e.printStackTrace();
                                                    request.reply(
                                                            new JsonObject()
                                                                    .putString("result", Pay123MuaOrderResult.SYSTEM_ERROR.toString())
                                                    );
                                                }
                                            }
                                        });

                                    }
                                } catch (InvalidProtocolBufferException e) {
                                    e.printStackTrace();
                                    request.reply(
                                            new JsonObject()
                                                    .putString("result", Pay123MuaOrderResult.SYSTEM_ERROR.toString())
                                    );
                                }
                            }
                        });
                    }
                });


            }
        });
    }

    /**
     * @param orderCode
     * @param mua123Db
     * @param paidDb
     * @param callback  -1: systemError
     *                  0:
     *                  1: paid
     */
    private void checkPaidOrderInDatabase(final String orderCode, final Partner123MuaOrderDb mua123Db, final Partner123MuaRequestFailDb paidDb, final Handler<JsonObject> callback) {
        //TODO: Checking pay history
        container.logger().debug("[partner123mua].[checkPaidOrderInDatabase] > [mua123Db.getOrder] ");
        mua123Db.getOrder(orderCode, new Handler<Partner123MuaOrderDb.Order>() {
            @Override
            public void handle(Partner123MuaOrderDb.Order result) {
                if (result != null && result.status == Partner123MuaOrderDb.Order.STATUS_PAID) {
                    callback.handle(new JsonObject().putNumber("result", 1));
                    return;
                }
                //TODO: Checking paid order
                container.logger().debug("[partner123mua].[checkPaidOrderInDatabase]>[paidDb.getOrder] ");
                paidDb.getOrder(orderCode, new Handler<Partner123MuaRequestFailDb.PaidOrder>() {
                    @Override
                    public void handle(Partner123MuaRequestFailDb.PaidOrder result) {
                        if (result != null) {
                            callback.handle(new JsonObject().putNumber("result", 1));
                            return;
                        }
                        //TODO:
                        callback.handle(new JsonObject().putNumber("result", 0));
                        return;
                    }
                });
            }
        });
    }

    //
    private void requestOrderInfo(HttpClient client, String orderCode, final Handler<JsonObject> callback) {
        long timeStamp = System.currentTimeMillis();
        DigestUtils.md5(PARTNER_PASS + timeStamp);
        String mac = DigestUtils.md5Hex(PARTNER_PASS + timeStamp);

        if (orderCode == null) {
            container.logger().error("requestOrderInfo(): Param missing :orderId");
            callback.handle(null);
            return;
        }

        final String uri = URI_GET_ORDER_INFO + "&orderCode=" + orderCode + "&ts=" + timeStamp + "&mac=" + mac;

        container.logger().info("requestOrderInfo " + PARTNER_HOST + uri);

        client.get(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                if (response.statusCode() != 200) {
                    container.logger().warn(String.format("[requestOrderInfo] response unexpected result {statusCode: %d}", response.statusCode()));
                    callback.handle(null);
                    return;
                }
                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            JsonObject obj = new JsonObject(buffer.toString());
                            container.logger().info(String.format("[requestOrderInfo: %s] response: %s", uri, obj.toString()));
                            callback.handle(obj);
                        } catch (DecodeException e) {
                            container.logger().error(String.format("[123muaRequestVerticle] [requestOrderInfo: %s] : Unexpected response : %s", uri, String.valueOf(buffer)));
                            callback.handle(null);
                        }
                    }
                });
            }
        }).end();
    }

    private void requestPayOrder(HttpClient client, String orderCode, final Handler<JsonObject> callback) {
        long timeStamp = System.currentTimeMillis();
        DigestUtils.md5(PARTNER_PASS + timeStamp);
        String mac = DigestUtils.md5Hex(PARTNER_PASS + timeStamp);

        if (orderCode == null) {
            container.logger().error("requestPayOrder(): Param missing :orderId");
            return;
        }

        final String uri = URI_ORDER_PAY + "&orderCode=" + orderCode + "&ts=" + timeStamp + "&mac=" + mac;

        container.logger().info("requestPayOrder: " + PARTNER_HOST + uri);

        client.get(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                if (response.statusCode() != 200) {
                    container.logger().warn("[request] [123mua] receives unexpected (expected 200) response status code : " + response.statusCode());
                    callback.handle(null);
                    return;
                }
                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            JsonObject obj = new JsonObject(buffer.toString());
                            container.logger().debug(String.format("requestPayOrder response: %s", obj.toString()));
                            callback.handle(obj);
                        } catch (DecodeException e) {
                            container.logger().error(String.format("requestPayOrder response unexpected result : %s", String.valueOf(buffer)));
                            callback.handle(null);
                        }
                    }
                });
            }
        }).end();
    }

    /**
     * {
     * cmdId = CMD_SEND_GET_ORDER
     * orderId: ORDER0001,
     * }
     *
     * @param client
     * @param request
     */
    private void setOrderPay(HttpClient client, final Message<JsonObject> request) {
        long timeStamp = System.currentTimeMillis();
        DigestUtils.md5(PARTNER_PASS + timeStamp);
        String mac = DigestUtils.md5Hex(PARTNER_PASS + timeStamp);

        String orderCode = request.body().getString("orderId");
        if (orderCode == null) {
            container.logger().error("set 123mua be paid: Param missing [orderId]");
            return;
        }

        final String uri = URI_ORDER_PAY + "?orderCode=" + orderCode + "&ts=" + timeStamp + "&mac=" + mac;

        container.logger().info("[request] [123mua] " + uri);

        client.get(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                if (response.statusCode() != 200) {
                    request.fail(response.statusCode(), "Request error");
                    return;
                }
                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            JsonObject obj = new JsonObject(buffer.toString());
                            request.reply(obj);
                        } catch (DecodeException e) {
                            container.logger().warn(String.format("Request[%s}: Unexpected response : %s", uri, String.valueOf(buffer)));
                            request.reply();
                        }
                    }
                });
            }
        }).end();
    }

    private void getOrderByID(final HttpClient client, final Message<JsonObject> request) {
        long timeStamp = System.currentTimeMillis();
        DigestUtils.md5(PARTNER_PASS + timeStamp);
        String mac = DigestUtils.md5Hex(PARTNER_PASS + timeStamp);

        String orderCode = request.body().getString("orderId");
        if (orderCode == null) {
            container.logger().error("Get 123mua order by id exception: Param missing [orderId]");
            return;
        }

        final String uri = URI_GET_ORDER_INFO + "&orderCode=" + orderCode + "&ts=" + timeStamp + "&mac=" + mac;

        container.logger().info("[request] [123mua] " + uri);

        client.get(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                if (response.statusCode() != 200) {
                    request.reply();
                    return;
                }
                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            JsonObject obj = new JsonObject(buffer.toString());
                            request.reply(obj);
                        } catch (DecodeException e) {
                            container.logger().warn(String.format("Request[%s}: Unexpected response : %s", uri, String.valueOf(buffer)));
                            request.reply();
                        }
                    }
                });
            }
        }).end();

    }
}
