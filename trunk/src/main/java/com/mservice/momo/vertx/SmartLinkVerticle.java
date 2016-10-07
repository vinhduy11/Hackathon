package com.mservice.momo.vertx;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.smartlink.SmartLinkResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by nam on 7/22/14.
 */
public class SmartLinkVerticle extends Verticle {
    public static final String CMD_TRANSFER_REQUEST = "transferRequest";
    public static final String CMD_TRANSFER_COMPLETE = "transferComplete";
    public static final String CMD_TRANSFER_CANCEL = "transferCancel";


    public String smartLinkMomoAccount = "0974540385";

    private Logger logger;

    private TransDb transDb;

    @Override
    public void start() {

        logger = container.logger();
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), container.logger(), container.config());

        vertx.eventBus().registerLocalHandler(AppConstant.SmartLinkVerticle, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject request = message.body();

                String cmd = request.getString("cmd");
                switch (cmd) {
                    case CMD_TRANSFER_REQUEST:
                        //cmd, amount

                        break;
                    case CMD_TRANSFER_COMPLETE:
                        transferComplete(message);
                        break;
                    case CMD_TRANSFER_CANCEL:

                        break;
                }
            }
        });
    }

    private void transferComplete(Message<JsonObject> message) {
        JsonObject request = message.body();
        JsonObject result = request.getObject("result");
        if (result == null) {
            throw new IllegalArgumentException("Missing 'result' param");
        }

        SmartLinkResponse smartLinkResponse = new SmartLinkResponse();
        smartLinkResponse.setValues(result);


        Integer error = Integer.parseInt(smartLinkResponse.vpc_ResponseCode);
        Integer phoneNumber = Integer.parseInt(smartLinkResponse.vpc_OrderInfo);
        Long amount = Long.parseLong(smartLinkResponse.vpc_Amount);
        if (error == 0) {
            logger.info("Adjust smartLink to number");
            transferSmartLinkToMomo(phoneNumber, amount);
        } else {
            Notification noti = new Notification();
            noti.receiverNumber = phoneNumber;
            noti.caption = "Kết quả chuyền tiền từ kênh SmartLink";
            noti.body = getErrorDescription(error);
            noti.sms = "Loi chuyen tien tu SmartLink. Ma loi: " + error;
            noti.time = System.currentTimeMillis();


            vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION, noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    logger.info("Send noti result: " + event.body());
                }
            });
        }
    }

    private void transferSmartLinkToMomo(final Integer phoneNumber, Long amount) {
        /*transfer(phoneNumber, "Nhận tiền từ SmartLink.", amount, new Handler<Integer>() {
            @Override
            public void handle(Integer tranError) {
                logger.info("Transfer SmartLink to " + phoneNumber + " result: " + tranError);
            }
        });*/
    }


    /*private void transfer(final int number, final String transferComment, final long amount, final Handler<Integer> callback) {

        final CoreCommon.BuildLog log = new CoreCommon.BuildLog(logger);
        log.setPhoneNumber("0" + number);
        log.add("finction","transfer");

        Buffer buffer = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                , 0
                , number
                , SoapProto.AdjustMent.newBuilder()
                        .setSource(smartLinkMomoAccount)
                        .setTarget("0" + number)
                        .setAmount(amount)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<CoreMessage<JsonObject>>() {
            @Override
            public void handle(CoreMessage<JsonObject> result) {

                int error = result.body().getInteger(colName.TranDBCols.ERROR,-1);
                log.add("error result", error);

                if (error == 0) {


                    final TransDb.TranObj mainObj = new TransDb.TranObj();
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

                    log.add("WC", "upsertTranOutSideNew");

                    transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {
                            log.add("isUpdated", result);

                            BroadcastHandler.sendOutSideTransSync(vertx, mainObj);

                            vertx.eventBus().send(
                                    AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                    , NotificationUtils.createRequestPushNotification(mainObj.owner_number, 2, mainObj)
                            );

                            log.writeLog();
                        }
                    });
                }

                callback.handle(error);
            }
        });

    }*/

    public String getErrorDescription(int error) {
        String message = "Đã có lỗi xãy ra, mã lỗi: " + error;
        switch (error) {
            case 1:
                message = "Ngân hàng từ chối thực hiện với lý do: thẻ hoặc tài khoản đã bị khóa.";
                break;
//            case 2:
//                message = "";
//                break;
            case 3:
                message = "Thực hiện chuyển tiền không thành công do: thẻ của bạn đã hết hạn sử dụng";
                break;
            case 4:
                message = "Thực hiện chuyển tiền không thành công do: sai OTP quá 3 lần hoặc quá hạn mức trong ngày.";
                break;
            case 5:
                message = "Thực hiện chuyển tiền không thành công do: không có trả lời từ Ngân Hàng.";
                break;
            case 6:
                message = "Thực hiện chuyển tiền không thành công do: lỗi giao tiếp với Ngân Hàng.";
                break;
            case 7:
                message = "Thực hiện chuyển tiền không thành công do: số dư tài khoản của bạn không đủ.";
                break;
            case 8:
                message = "Thực hiện chuyển tiền không thành công do: lỗi dữ liệu truyền nhận.";
                break;
            case 9:
                message = "Thực hiện chuyển tiền không thành công do kiểu giao dịch không được hỗ trợ.";
                break;
        }
        return message;
    }

}
