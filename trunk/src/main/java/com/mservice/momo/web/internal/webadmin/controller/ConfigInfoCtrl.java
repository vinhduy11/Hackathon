package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 4/14/14.
 */
public class ConfigInfoCtrl {

    private Vertx vertx;

    public ConfigInfoCtrl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Action(path = "/service/config/getTestInfo")
    public void getTestInfo(HttpRequestContext context, Handler<Object> callback) {
        JsonObject obj = new JsonObject();

        obj.putBoolean("testMode", ServerVerticle.TEST_MODE);
        obj.putString("phones", ServerVerticle.PHONES_FOR_TEST);
        callback.handle(obj);
    }

    @Action(path = "/service/config/setTestInfo")
    public void setTestInfo(HttpRequestContext context, Handler<Object> callback) {
        String testMode = context.getPostParams().get("testMode");
        String phones = context.getPostParams().get("phones");

        if (testMode != null) {
            ServerVerticle.TEST_MODE = testMode.equals("true");
        }
        if (phones != null) {
            ServerVerticle.PHONES_FOR_TEST = phones;
        }

        JsonObject obj = new JsonObject();

        obj.putBoolean("testMode", ServerVerticle.TEST_MODE);
        obj.putString("phones", ServerVerticle.PHONES_FOR_TEST);
        callback.handle(obj);
    }




    @Action(path = "/service/config/getServiceStatus")
    public void getServiceStatus(HttpRequestContext context, final Handler<Object> callback) {
        JsonObject request = new JsonObject();

        request.putString(colName.CfgAtRuning.TASK, Const.GET_ALL_TRAN_SERVICE_STATUS);

        vertx.eventBus().send(ServerVerticle.CHANGE_CFG, request, new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> value) {
                JsonArray array = value.body();
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i);
                    obj.putString("desc", getDescription(obj.getString("name")));
                }
                callback.handle(array);
            }
        });
    }

    @Action(path = "/service/stopAll")
    public void sendStop(HttpRequestContext context, final Handler<Object> callback) {
        JsonObject request = new JsonObject();

        request.putString(colName.CfgAtRuning.TASK, Const.SET_TRAN_SERVICE_STATUS);
        request.putNumber(colName.CfgAtRuning.TRAN_TYPE, Const.ALL_TRANSACTION);
        request.putBoolean(colName.CfgAtRuning.VALUE, false);

        vertx.eventBus().send(ServerVerticle.CHANGE_CFG, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> value) {
//                JsonArray array = value.body();
                callback.handle(value.body());
            }
        });
    }

    @Action(path = "/service/startAll")
    public void startAll(HttpRequestContext context, final Handler<Object> callback) {
        JsonObject request = new JsonObject();

        request.putString(colName.CfgAtRuning.TASK, Const.SET_TRAN_SERVICE_STATUS);
        request.putNumber(colName.CfgAtRuning.TRAN_TYPE, Const.ALL_TRANSACTION);
        request.putBoolean(colName.CfgAtRuning.VALUE, true);

        vertx.eventBus().send(ServerVerticle.CHANGE_CFG, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> value) {
//                JsonArray array = value.body();
                callback.handle(value.body());
            }
        });
    }

    private String getDescription(String name) {
        String desc = "";
        switch (MomoProto.TranHisV1.TranType.valueOf(name)) {
            case TOP_UP:
                desc= "Nạp tiền điện thoại";
                break;
            case TOP_UP_GAME:
                desc = "Nạp card Game.";
                break;
            case M2C:
                desc = "Chuyển tiền M2C.";
                break;
            case M2M:
                desc = "Chuyển tiền M2M.";
                break;
            case PAY_ONE_BILL:
                desc = "Thanh toán hóa đơn.";
                break;
            case QUICK_PAYMENT:
                desc = "";
                break;
            case QUICK_DEPOSIT:
                desc = "";
                break;
            case BANK_NET_TO_MOMO:
                desc = "Chuyển tiền từ ngân hàn về MoMo thông qua Banknet.";
                break;
            case BANK_NET_VERIFY_OTP:
                desc = "";
                break;
            case PAY_ONE_BILL_OTHER:
                desc = "";
                break;
            case TRANSFER_MONEY_TO_PLACE:
                desc = "Rút tiền tại điểm giao dịch.";
                break;
            case BILL_PAY_TELEPHONE:
                desc = "Thanh toán hóa đơn điện thoại.";
                break;
            case BILL_PAY_TICKET_AIRLINE:
                desc = "Thanh toán vé máy bay.";
                break;
            case BILL_PAY_TICKET_TRAIN:
                desc = "Thanh toán vé tàu lửa.";
                break;
            case BILL_PAY_INSURANCE:
                desc = "Thanh toán bảo hiểm.";
                break;
            case BILL_PAY_INTERNET:
                desc = "Than toán cước internet.";
                break;
            case BILL_PAY_OTHER:
                desc = "Các thanh toán khác.";
                break;
            case DEPOSIT_CASH_OTHER:
                desc = "";
                break;
            case BUY_MOBILITY_CARD:
                desc = "Mua card điện thoại.";
                break;
            case BUY_GAME_CARD:
                desc = "Mua card game.";
                break;
            case BUY_OTHER:
                desc = "";
                break;
            case DEPOSIT_CASH:
                desc = "Rút tiền mặt.";
                break;
            case BILL_PAY_CINEMA:
                desc = "Thanh toán vé xem phim.";
                break;
            case MOMO_TO_BANK_MANUAL:
                desc = "Rút tiền về Ngân Hàng.";
                break;
            case DEPOSIT_AT_HOME:
                desc = "Nạp tiền tận nơi.";
                break;
            case WITHDRAW_AT_HOME:
                desc = "Rút tiền tận nơi";
                break;

        }
        return desc;
    }

//    @Action(path = "/123mua/pay")
//    public void test123MuaPay(HttpRequestContext context, final Handler<Object> callback) {
//
//
//        CmdModels.Pay123MuaOrder payOrder = CmdModels.Pay123MuaOrder.newBuilder()
//                .setAmount(100000)
//                .setPin("123456")
//                .setPhoneNumber(987568815)
//                .setReceiverPhoneNumber(979754034)
//                .setComment("test")
//                .setOrderId("VNG_16354951635241685")
//                .build();
//        MomoCommand cmd = new MomoCommand(CmdModels.CommandType.PAY_123MUA_ORDER, payOrder);
//        vertx.eventBus().send(CommandVerticle.ADDRESS, MomoCommand.toBuffer(cmd));
//
//    }

}
