package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.bank.entity.BankRequest;
import com.mservice.bank.entity.BankRequestFactory;
import com.mservice.bank.entity.BankResponse;
import com.mservice.common.Popup;
import com.mservice.momo.data.Card;
import com.mservice.momo.data.ConnectorHTTPPostPathDb;
import com.mservice.momo.data.MisPopupDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.customercaregiftgroup.DollarHeartCustomerCareGiftGroupDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.popup.EmailPopupDb;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.MainVerticle;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.shopping.entity.ShoppingRequest;
import com.mservice.shopping.entity.ShoppingRequestFactory;
import com.mservice.shopping.entity.ShoppingResponse;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.*;

/**
 * Created by concu on 6/18/15.
 */
public class MoMoInfoProcess extends InfoProcess {

    Vertx vertx;
    Logger logger;
    JsonObject glbCfg;
    JsonObject bankCfg;
    PhonesDb phonesDb;
    MisPopupDb misPopupDb;
    private JsonObject jsonShopping;
    private DollarHeartCustomerCareGiftGroupDb dollarHeartCustomerCareGiftGroupDb;
    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    private PromotionProcess promotionProcess;
    private Card card;
    private EmailPopupDb emailPopupDb;
    public MoMoInfoProcess(Vertx vertx, Logger logger, JsonObject glbCfg) {
        super(vertx, logger, glbCfg);
        this.vertx = vertx;
        this.logger = logger;
        this.glbCfg = glbCfg;
        this.bankCfg = glbCfg.getObject(StringConstUtil.BANK, new JsonObject());
        boolean checkStoreApp = MoMoInfoProcess.checkStoreApp();
        logger.info("checkStoreApp ------>" + checkStoreApp);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        jsonShopping = glbCfg.getObject(StringConstUtil.Shopping.ServiceType, new JsonObject());
        dollarHeartCustomerCareGiftGroupDb = new DollarHeartCustomerCareGiftGroupDb(vertx, logger);
        misPopupDb = new MisPopupDb(vertx, logger);
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);
        card = new Card(vertx.eventBus(), logger);
        emailPopupDb = new EmailPopupDb(vertx, logger);
    }

    public static boolean checkStoreApp() {

        boolean isStoreApp = false;
        if (MainVerticle.globalCfg != null) {
            isStoreApp = MainVerticle.globalCfg.getBoolean(StringConstUtil.CHECK_STORE_APP, false);
        }
        return isStoreApp;
    }

    public void getWalletConfirmInfo(NetSocket socket, final MomoMessage msg, final SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("func", "getWalletConfirmInfo");

        MomoProto.TextValueMsg textValueMsg;

        try {
            textValueMsg = MomoProto.TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            textValueMsg = null;
        }

        final MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();
        HashMap<String, String> hashMap = Misc.getKeyValuePairs(textValueMsg.getKeysList());

        final String confirmValue = hashMap.containsKey(StringConstUtil.WALLET_MAPPING_CONFIRMED) ? hashMap.get(StringConstUtil.WALLET_MAPPING_CONFIRMED) : "";
        final String partner = hashMap.containsKey(StringConstUtil.WALLET_MAPPING_PARTNER) ? hashMap.get(StringConstUtil.WALLET_MAPPING_PARTNER) : "";
        log.add("confirmValue", confirmValue);
        log.add("partner", partner);
        if(StringConstUtil.LIQUID_POPUP.equalsIgnoreCase(partner) || StringConstUtil.INFO_POPUP.equalsIgnoreCase(partner))
        {
            log.add("desc", "popup thanh khoan || info => khong lam gi nua");
            log.writeLog();
            return;
        }
        else if(StringConstUtil.POPUP_EMAIL.equalsIgnoreCase(partner))
        {
            final String email = hashMap.containsKey("email") ? hashMap.get("email") : "";
            log.add("desc", "popup email => cap nhat email");
//            if(!"".equalsIgnoreCase(email))
//            {
            log.add("desc", "updateEmailInfo");
            updateEmailInfo(email, msg.cmdPhone, socket, msg, data);
//                log.writeLog();
//                return;
//            }
            log.add("desc", "email is blank => cap nhat email");
            log.writeLog();
            return;
        }
        //app phai gui them 2 key
        //key cusname : ten vi can dinh danh
        //key cuscardid  : cmnd vi can dinh danh
        //confirm = 0 // NO unmap
        //confirm = 1 // YES xac nhan => khong lam gi.
        phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(final PhonesDb.Obj obj) {
                if (obj != null) {
                    if (confirmValue.equalsIgnoreCase(StringConstUtil.WALLET_MAPPING_NO)) {
                        // Thuc hien unmap vi
//                        BankRequest bankRequest = BankRequestFactory.unRegister(obj.bank_code, "0" + msg.cmdPhone, data.pin, System.currentTimeMillis());
                        BankRequest bankRequest = BankRequestFactory.createUnMapRequest(obj.bank_code, "0" + msg.cmdPhone, data.pin, System.currentTimeMillis(), "", new HashMap<String, String>());
                        String bank_verticle_address = bankCfg.getString(StringConstUtil.BANK_CONNECTOR_VERTICLE, "");
                        String method = bankCfg.getString(StringConstUtil.ALLOW_BANK_VIA_CONNECTOR, "cluster");
                        log.add("bankRequest", bankRequest);
                        log.add("bank_verticle_address", bank_verticle_address);
                        log.add("method", method);
                        long time = 60 * 1000L;
                        if (!bank_verticle_address.equalsIgnoreCase("") && "cluster".equalsIgnoreCase(method)) {
                            vertx.eventBus().sendWithTimeout(bank_verticle_address, bankRequest.getJsonObject(), time, new Handler<AsyncResult<Message<JsonObject>>>() {
                                @Override
                                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                                    if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                                        JsonObject result = messageAsyncResult.result().body();
                                        BankResponse bankResponse = new BankResponse(result);
                                        log.add("func", "unRegister");
                                        log.add("result", result);
                                        if (bankResponse != null && bankResponse.getResultCode() == 0) {
                                            sendSuccessNoti(bankResponse, msg);
                                            log.add("result", "sendSuccessNoti");
                                            log.writeLog();
                                            return;
                                        } else if (bankResponse != null && bankResponse.getResultCode() != 0) {
                                            sendFailNoti(bankResponse, msg);
                                            log.add("result", "sendFailNoti_1");
                                            log.writeLog();
                                            return;
                                        } else {
                                            log.add("result", "sendFailNoti_3");
                                            log.writeLog();
                                            return;
                                        }
                                    } else {
                                        sendFailNoti_Null(msg);
                                        log.add("result", "sendFailNoti_2");
                                        log.writeLog();
                                        return;
                                    }
                                }
                            });
                            return;
                        }
                        else {
                            JsonObject joBankReq = new JsonObject();
                            joBankReq.putNumber(BankHelperVerticle.COMMAND, BankHelperVerticle.BANK_IN_OUT);
                            joBankReq.putString(BankHelperVerticle.PHONE, "0" + msg.cmdPhone);
                            joBankReq.putObject(BankHelperVerticle.DATA, bankRequest.getJsonObject());
                            vertx.eventBus().sendWithTimeout(AppConstant.BankHelperVerticle_ADDRESS, joBankReq, time, new Handler<AsyncResult<Message<JsonObject>>>() {
                                @Override
                                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                                    if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                                        JsonObject result = messageAsyncResult.result().body();
                                        BankResponse bankResponse = new BankResponse(result);
                                        log.add("func", "unRegister");
                                        log.add("result", result);
                                        if (bankResponse != null && bankResponse.getResultCode() == 0) {
                                            sendSuccessNoti(bankResponse, msg);
                                            log.add("result", "sendSuccessNoti");
                                            log.writeLog();
                                            return;
                                        } else if (bankResponse != null && bankResponse.getResultCode() != 0) {
                                            sendFailNoti(bankResponse, msg);
                                            log.add("result", "sendFailNoti_1");
                                            log.writeLog();
                                            return;
                                        } else {
                                            log.add("result", "sendFailNoti_3");
                                            log.writeLog();
                                            return;
                                        }
                                    } else {
                                        sendFailNoti_Null(msg);
                                        log.add("result", "sendFailNoti_2");
                                        log.writeLog();
                                        return;
                                    }
                                }
                            });
                            return;
                        }
                    } else {
                        //Confirm
                        // Thuc hien xac nhan map vi
                        log.add("bankcode", obj.bank_code);
                        BankRequest bankRequest = BankRequestFactory.createConfirmRequest(obj.bank_code, "0" + msg.cmdPhone);
                        String bank_verticle_address = bankCfg.getString(StringConstUtil.BANK_CONNECTOR_VERTICLE, "");
                        String method = bankCfg.getString(StringConstUtil.ALLOW_BANK_VIA_CONNECTOR, "cluster");
                        log.add("bankRequest", bankRequest);
                        log.add("bank_verticle_address", bank_verticle_address);
                        log.add("method", method);
                        long time = 60 * 1000L;
                        //BEGIN CHECK BANK
                        if (!bank_verticle_address.equalsIgnoreCase("") && "cluster".equalsIgnoreCase(method)) {
                            vertx.eventBus().sendWithTimeout(bank_verticle_address, bankRequest.getJsonObject(), time, new Handler<AsyncResult<Message<JsonObject>>>() {
                                @Override
                                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                                    if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                                        JsonObject result = messageAsyncResult.result().body();
                                        confirmBankPopupWithConnector(result, log, msg, data);
                                    } else {
                                        //sendFailNoti_Null(msg);
                                        log.add("result", "sendFailNoti_2");
                                        log.add("result", "Khong nhan duoc cai gi ca, dcm network");
                                        log.writeLog();
                                        return;
                                    }
                                }
                            }); //end handle
                            return;
                        } // Compare bank_verticle
                        else {
                            JsonObject joBankReq = new JsonObject();
                            joBankReq.putNumber(BankHelperVerticle.COMMAND, BankHelperVerticle.BANK_IN_OUT);
                            joBankReq.putString(BankHelperVerticle.PHONE, "0" + msg.cmdPhone);
                            joBankReq.putObject(BankHelperVerticle.DATA, bankRequest.getJsonObject());
                            vertx.eventBus().sendWithTimeout(AppConstant.BankHelperVerticle_ADDRESS, joBankReq, 80 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                                @Override
                                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                                    if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                                        JsonObject result = messageAsyncResult.result().body();
                                        JsonObject joResponse = result.getObject(BankHelperVerticle.DATA, result);
                                        logger.info("result confirm from bank " + result.toString());
                                        confirmBankPopupWithConnector(joResponse, log, msg, data);
                                    } else {
                                        //sendFailNoti_Null(msg);
                                        log.add("result", "sendFailNoti_2");
                                        log.add("result", "Khong nhan duoc cai gi ca, dcm network");
                                        log.writeLog();
                                        return;
                                    }
                                }
                            });
                            //check bank
                            return;
                        }
                    } // END else
                } //End check phone obj
                else {
                    log.add("obj", "null");
                    log.writeLog();
                    return;
                }
            } // End handle
        }); // END Phonedb
    }

    private void updateEmailInfo(final String email, final int phoneNumber, final NetSocket socket, final MomoMessage msg, final SockData data)
    {
        JsonObject joUpdate = new JsonObject();
        if(!"".equalsIgnoreCase(email))
        {
            joUpdate.putString(colName.PhoneDBCols.EMAIL, email);
        }

        phonesDb.findAndModifyPhoneDb(phoneNumber, email, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj phoneObj) {
                if (phoneObj != null) {
                    data.setPhoneObj(phoneObj, logger, "");
                }
                JsonObject joEmailUpdate = new JsonObject();
                joEmailUpdate.putString(colName.EmailPopupCol.EMAIL, email);
                joEmailUpdate.putBoolean(colName.EmailPopupCol.ENABLE, false);
                emailPopupDb.upSert("0" + phoneNumber, joEmailUpdate, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
//                        vertx.setTimer(5 * 1000L, new Handler<Long>() {
//                            @Override
//                            public void handle(Long event) {
//                                mCommon.sendCurrentAgentInfo(vertx, socket, msg.cmdIndex, msg.cmdPhone, data);
//                            }
//                        });
//                        sendConfirmWalletReply(socket, msg);
                        mCommon.sendCurrentAgentInfo(vertx, socket, msg.cmdIndex, msg.cmdPhone, data);
                    }
                });
            }
        });

    }

    private void sendConfirmWalletReply(final NetSocket socket,final MomoMessage msg)
    {
        Buffer buffer = MomoMessage.buildBuffer(
                MomoProto.MsgType.CONFIRM_WALLET_MAPPING_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                MomoProto.StandardReply.newBuilder().setDesc("good").setRcode(0).setResult(true).build().toByteArray()
        );
        mCommon.writeDataToSocket(socket, buffer);
    }
    private void confirmBankPopupWithConnector(JsonObject result, Common.BuildLog log, MomoMessage msg, SockData data) {

        BankResponse bankResponse = new BankResponse(result);
        log.add("func", "createConfirmRequest");
        log.add("result", result);
        if (bankResponse != null && bankResponse.getResultCode() == 0) {
            sendSuccessNoti(bankResponse, msg);
            log.add("result", "sendSuccessNoti");
            log.add("result", "Connector da nhan lenh xac nhan");
            log.add("promotion mapping wallet", "kiem tra khuyen mai map vi");
            log.add("bankResponse ", bankResponse.getJsonObject());
//            promotionProcess.executeReferralPromotion(StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.BANK_MAPPING, null, null, data, bankResponse, log, new JsonObject());
//                                            promotionProcess.goBankMappingPromotion(null, data, bankResponse, log);
            log.writeLog();
            return;
        } else if (bankResponse != null && bankResponse.getResultCode() != 0) {
            //sendFailNoti(bankResponse, msg);
            log.add("result", "sendFailNoti_1");
            log.add("result", "Connector da nhan lenh xac nhan nhung thich tra loi cho vui");
            log.writeLog();
            return;
        } else {
            log.add("result", "sendFailNoti_3");
            log.add("result", "Connector da nhan lenh xac nhan nhung tra ve 1 cuc keo khong co gi, dcm connector");
            log.writeLog();
            return;
        }
    }

    private void sendFailNoti(BankResponse response, MomoMessage msg) {

        JsonObject jsonExtra = new JsonObject();

        int type = 0;
        com.mservice.common.Notification noti = response.getNotification();
        String header = noti != null ? noti.getHeader() : StringConstUtil.CANCEL_HEADER;
        String content = noti != null ? noti.getContent() : StringConstUtil.CANCEL_CONTENT;
        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, StringConstUtil.CONFIRM_BUTTON_TITLE);

        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        notification.caption = header;
        notification.body = content;
        notification.tranId = 0L;
        notification.cmdId = msg.cmdIndex;
        notification.time = new Date().getTime();
        notification.receiverNumber = msg.cmdPhone;
//        notification.extra = jsonExtra.toString();
        Misc.sendNoti(vertx, notification);
    }

    private void sendFailNoti_Null(MomoMessage msg) {

        JsonObject jsonExtra = new JsonObject();

        int type = 0;
        String header = StringConstUtil.CANCEL_HEADER;
        String content = StringConstUtil.CANCEL_CONTENT;
        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, StringConstUtil.CONFIRM_BUTTON_TITLE);

        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        notification.caption = header;
        notification.body = content;
        notification.tranId = 0L;
        notification.cmdId = msg.cmdIndex;
        notification.time = new Date().getTime();
        notification.receiverNumber = msg.cmdPhone;
//        notification.extra = jsonExtra.toString();
        Misc.sendNoti(vertx, notification);
    }

    /**
     * Gui noi dung noti khi nhan lenh thanh cong tu connector
     *
     * @param response
     * @param msg
     */
    private void sendSuccessNoti(BankResponse response, MomoMessage msg) {
        JsonObject jsonExtra = new JsonObject();
        int type = 0;

        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, StringConstUtil.CONFIRM_BUTTON_TITLE);
        com.mservice.common.Notification noti = response.getNotification();

        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        notification.caption = noti != null ? noti.getHeader() : "Hello";
        notification.body = noti != null ? noti.getContent() : "You have confirmed";
        notification.tranId = 0L;
        notification.cmdId = msg.cmdIndex;
        notification.time = new Date().getTime();
        notification.receiverNumber = msg.cmdPhone;
//        notification.extra = jsonExtra.toString();
        Misc.sendNoti(vertx, notification);
    }


    public void loadJsonObjectForm(HashMap<String, String> hashMap, final MomoMessage msg, final SockData data, final NetSocket sock, final Common common, final Logger logger
            , final long voucher, final long point, final long fee, final long totalAmount, boolean isStoreApp, long remainVoucher) {
        JsonObject joReply = new JsonObject();
        final Common.BuildLog log = new Common.BuildLog(logger);
        int wallet = hashMap.containsKey(Const.AppClient.SourceFrom) ? DataUtil.strToInt(hashMap.get(Const.AppClient.SourceFrom)) : 1;
        final String serviceId = hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : "";
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("SourceFrom", wallet);
        log.add("ServiceId", serviceId);
        log.add("voucher", voucher);
        log.add("point", point);
        log.add("totalAmount", totalAmount);
        log.add("fee", fee);
        log.add("remainVoucher", remainVoucher);
        ShoppingRequest shoppingRequest = ShoppingRequestFactory.createPopupConfirmBackendRequest(hashMap, point, voucher, "0" + msg.cmdPhone, data.pin, fee, totalAmount, remainVoucher);

        log.add("isStoreApp", isStoreApp);
        if (isStoreApp) {
            shoppingRequest = ShoppingRequestFactory.createPopupConfirmBackendAgencyRequest(hashMap, point, voucher, "0" + msg.cmdPhone, data.pin, fee, totalAmount);
        }
        final JsonObject request = shoppingRequest.getJsonObject();
        log.add("shopping request", request);

//        log.writeLog();
        final long timeout = 50 * 1000L;

        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = serviceId;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SHOPPING_VERTICLE_BY_SERVICE_ID;

        connectorHTTPPostPathDb.findOne(serviceId.toLowerCase(), new Handler<ConnectorHTTPPostPathDb.Obj>() {
            @Override
            public void handle(ConnectorHTTPPostPathDb.Obj connectorPathObj) {
                    if(connectorPathObj == null)
                    {
                        //Di theo luong bus
                        log.add("desc", "di theo luong bus address");
                        getSaleOffBillTranslateFormViaBus(msg, sock, common, log, request, timeout, serviceReq);
                        return;
                    }
                    log.add("desc", "theo http post");
                    getSaleOffBillTranslateFromViaHttp(connectorPathObj, log, serviceId, request, msg, common, sock);
                    return;
            }
        });
    }

    private void getSaleOffBillTranslateFromViaHttp(ConnectorHTTPPostPathDb.Obj connectorPathObj, final Common.BuildLog log, String serviceId, JsonObject request, final MomoMessage msg, final Common common, final NetSocket sock) {
        Misc.getDataFromConnector("0" + msg.cmdPhone, connectorPathObj.host, connectorPathObj.port, connectorPathObj.path, vertx, log, serviceId, request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joReplyFromConnector) {
                int error = joReplyFromConnector.getInteger(StringConstUtil.ERROR, 1000);
                if (error != 0) {
                    return;
                }
                JsonObject joData = joReplyFromConnector.getObject(BankHelperVerticle.DATA, null);
                if (joData == null) {
                    return;
                }
                ShoppingResponse response = new ShoppingResponse(joData);
                if (response != null) {
                    MomoProto.TextValue textValue = MomoProto.TextValue.getDefaultInstance();
                    //textValue.toBuilder().setText("form").setValue(response.getJsonObject().toString());
                    MomoProto.TextValueMsg.Builder builder = MomoProto.TextValueMsg.newBuilder();
//                    log.add("response", response.getJsonObject());
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.TRANSLATE_CONFIRM_INFO_REPLY_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            builder.addKeys(textValue.toBuilder().setText("form").setValue(response.getJsonObject().toString())).build()
                                    .toByteArray()
                    );
                    common.writeDataToSocket(sock, buf);
                    log.writeLog();
                } else {
                    log.add("result", "respone is null");
                    log.writeLog();
                }
                return;


            }
        });
    }

    private void getSaleOffBillTranslateFormViaBus(final MomoMessage msg, final NetSocket sock, final Common common, final Common.BuildLog log, final JsonObject request, final long timeout, Common.ServiceReq serviceReq) {
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject jsonVerticle = message.body();

                ViaConnectorObj viaConnectorObj = new ViaConnectorObj(jsonVerticle);
                if(viaConnectorObj.IsViaConnectorVerticle)
                {
                    vertx.eventBus().sendWithTimeout(viaConnectorObj.BusName, request, timeout, new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {

                            if (messageAsyncResult != null && messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                                ShoppingResponse response = new ShoppingResponse(messageAsyncResult.result().body());
                                if (response != null) {
                                    MomoProto.TextValue textValue = MomoProto.TextValue.getDefaultInstance();
                                    //textValue.toBuilder().setText("form").setValue(response.getJsonObject().toString());
                                    MomoProto.TextValueMsg.Builder builder = MomoProto.TextValueMsg.newBuilder();
                                    log.add("response", response.getJsonObject());
                                    Buffer buf = MomoMessage.buildBuffer(
                                            MomoProto.MsgType.TRANSLATE_CONFIRM_INFO_REPLY_VALUE,
                                            msg.cmdIndex,
                                            msg.cmdPhone,
                                            builder.addKeys(textValue.toBuilder().setText("form").setValue(response.getJsonObject().toString())).build()
                                                    .toByteArray()
                                    );
                                    common.writeDataToSocket(sock, buf);
                                    log.writeLog();
                                } else {
                                    log.add("result", "respone is null");
                                    log.writeLog();
                                }
                                return;
                            } else {
                                log.add("result", "bank_timeout");
                                log.writeLog();
                                return;
                            }
                        }
                    });
                }
                else
                {
                    log.add("result", "Thieu du lieu trong file shoppingVerticle.json");
                    log.writeLog();
                    return;
                }
            }
        });
    }


    private void showLiquidityPopup(final MomoMessage msg, final Common.BuildLog log) {
        misPopupDb.findOne("0" + msg.cmdPhone,  new Handler<MisPopupDb.Obj>() {
            @Override
            public void handle(MisPopupDb.Obj misPopup) {
                if(misPopup != null)
                {
                    //Ban popup thanh khoan cho diem giao dich
                    log.add("desc", "Co popup thanh khoan " + msg.cmdPhone);
                    log.writeLog();
                    Popup popup = new Popup(Popup.Type.CONFIRM);
                    popup.setHeader(misPopup.caption);
                    popup.setContent(misPopup.body);
                    popup.setInitiator(misPopup.phone_number);
                    popup.setEnabledClose(false);
                    popup.setOkButtonLabel("Xác nhận");
                    popup.setCancelButtonLabel("Hủy");
                    sendPopUpInformation(popup, msg, false, System.currentTimeMillis(), StringConstUtil.LIQUID_POPUP);
                    vertx.setTimer(2000L, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            deletePopupInformation(msg);
                        }
                    });
                }
                else
                {
                    log.add("desc", "Khong co popup thanh khoan");
                    log.writeLog();
                    return;
                }
            }
        });
    }

    //END 0000000050
    private void sendPopUpInformation(Popup popup, MomoMessage msg, boolean isConfirm, long tid, String partner) {
        JsonObject jsonExtra = new JsonObject();
        int type = isConfirm ? 1 : 0;

        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        if (isConfirm) {
            String button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }
        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, partner);
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = 0L;
        notification.cmdId = msg.cmdIndex;
        notification.time = new Date().getTime();
        notification.receiverNumber = msg.cmdPhone;
        notification.extra = jsonExtra.toString();

        if(popup != null)
        {
            Misc.sendNoti(vertx, notification);
        }
    }

    //Delete Popup Information
    private void deletePopupInformation(final MomoMessage msg)
    {
        misPopupDb.deletePopupData("0" + msg.cmdPhone, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                logger.info("deleted popup " + msg.cmdPhone + " " + aBoolean);
            }
        });
    }


    //Check User Reset pass
    public void checkUserResetPassword(final NetSocket socket, final MomoMessage msg, final SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "checkUserResetPassword");

        MomoProto.TextValueMsg textValueMsg;

        try {
            textValueMsg = MomoProto.TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            textValueMsg = null;
        }

//        final MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();
        HashMap<String, String> hashMap = Misc.getKeyValuePairs(textValueMsg.getKeysList());

        final String appCode = hashMap.containsKey(StringConstUtil.ResetPasswordFields.APP_CODE) ? hashMap.get(StringConstUtil.ResetPasswordFields.APP_CODE) : "";
        final String appOs = hashMap.containsKey(StringConstUtil.ResetPasswordFields.APP_OS) ? hashMap.get(StringConstUtil.ResetPasswordFields.APP_OS) : "";
        log.add("appCode", appCode);
        log.add("appOs", appOs);

        TransferWithGiftContext.getTotalBalance(vertx, msg.cmdPhone, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject joReply) {
                long amount = joReply.getLong(StringConstUtil.AMOUNT_MOMO, 0);
                long giftAmount = joReply.getLong(StringConstUtil.AMOUNT_GIFT, 0);
                long pointAmount = joReply.getLong(StringConstUtil.AMOUNT_POINT, 0);

                final MomoProto.TextValueMsg.Builder textValueMsgBuilderRep = MomoProto.TextValueMsg.newBuilder();



                if(amount > 0 || giftAmount > 0 || pointAmount > 0)
                {
                    //Return fail
                    returnCheckPasswordFail(textValueMsgBuilderRep, msg, socket, log, MomoProto.MsgType.CHECK_USER_RESET_PASSWORD_REPLY_VALUE);
                    return;
                }
                phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj phoneObj) {
                        if(phoneObj == null)
                        {
                            //Return fail
                            returnCheckPasswordFail(textValueMsgBuilderRep, msg, socket, log, MomoProto.MsgType.CHECK_USER_RESET_PASSWORD_REPLY_VALUE);
                            return;
                        }
                        else if(!"0".equalsIgnoreCase(phoneObj.bank_code))
                        {
                            //Return fail
                            returnCheckPasswordFail(textValueMsgBuilderRep, msg, socket, log, MomoProto.MsgType.CHECK_USER_RESET_PASSWORD_REPLY_VALUE);
                            return;
                        }

                        card.findAll(msg.cmdPhone, new Handler<ArrayList<Card.Obj>>() {
                            @Override
                            public void handle(ArrayList<Card.Obj> listCards) {
                                if(listCards.size() > 0)
                                {
                                    //Return fail
                                    returnCheckPasswordFail(textValueMsgBuilderRep, msg, socket, log, MomoProto.MsgType.CHECK_USER_RESET_PASSWORD_REPLY_VALUE);
                                    return;
                                }
                                returnCheckPasswordTrue(textValueMsgBuilderRep, msg, socket, log, MomoProto.MsgType.CHECK_USER_RESET_PASSWORD_REPLY_VALUE);
                                return;
                            }
                        });

                    }
                });
                return;

            }
        });

    }

    private void returnCheckPasswordFail(MomoProto.TextValueMsg.Builder textValueMsgBuilderRep, MomoMessage msg, NetSocket socket, Common.BuildLog log, int msgType) {
        textValueMsgBuilderRep.addKeys(Misc.getTextValueBuilder(StringConstUtil.RESULT, "false"));

        Buffer buffer = MomoMessage.buildBuffer(
                msgType,
                msg.cmdIndex,
                msg.cmdPhone,
                textValueMsgBuilderRep.build()
                        .toByteArray()
        );
        //Truyen nhung gia tri thay doi len cho Client.
        mCommon.writeDataToSocket(socket, buffer);
        log.writeLog();
    }

    private void returnCheckPasswordTrue(MomoProto.TextValueMsg.Builder textValueMsgBuilderRep, MomoMessage msg, NetSocket socket, Common.BuildLog log, int msgType) {
        textValueMsgBuilderRep.addKeys(Misc.getTextValueBuilder(StringConstUtil.RESULT, "true"));

        Buffer buffer = MomoMessage.buildBuffer(
                msgType,
                msg.cmdIndex,
                msg.cmdPhone,
                textValueMsgBuilderRep.build()
                        .toByteArray()
        );
        //Truyen nhung gia tri thay doi len cho Client.
        mCommon.writeDataToSocket(socket, buffer);
        log.writeLog();
    }

    public void resetPinForPoorUser(final NetSocket socket, final MomoMessage msg, final SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "resetPinForPoorUser");

        MomoProto.TextValueMsg textValueMsg;

        try {
            textValueMsg = MomoProto.TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            textValueMsg = null;
        }

//        final MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();
        HashMap<String, String> hashMap = Misc.getKeyValuePairs(textValueMsg.getKeysList());

        final String appCode = hashMap.containsKey(StringConstUtil.ResetPasswordFields.APP_CODE) ? hashMap.get(StringConstUtil.ResetPasswordFields.APP_CODE) : "";
        final String appOs = hashMap.containsKey(StringConstUtil.ResetPasswordFields.APP_OS) ? hashMap.get(StringConstUtil.ResetPasswordFields.APP_OS) : "";
        log.add("appCode", appCode);
        log.add("appOs", appOs);
        String newPass = generateNewPin();
        log.add("newPass", newPass);
        //build buffer --> soap verticle
        Buffer resetPin = MomoMessage.buildBuffer(
                SoapProto.MsgType.RECOVERY_NEW_PIN_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                SoapProto.RecoveryNewPin.newBuilder()
                        .setNumber("0" + msg.cmdPhone)
                        .setNewPin(newPass)
                        .build()
                        .toByteArray()
        );
        final MomoProto.TextValueMsg.Builder textValueMsgBuilderRep = MomoProto.TextValueMsg.newBuilder();
        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, resetPin, new Handler<Message<Integer>>() {
            @Override
            public void handle(Message<Integer> result) {
                if(result != null && result.body() == 0)
                {
                    returnCheckPasswordTrue(textValueMsgBuilderRep, msg, socket, log, MomoProto.MsgType.RESET_PASSWORD_REPLY_VALUE);
                }
                else
                {
                    returnCheckPasswordFail(textValueMsgBuilderRep, msg, socket, log, MomoProto.MsgType.RESET_PASSWORD_REPLY_VALUE);
                }
            }
        });

    }

    public String generateNewPin()
    {
        String newPin = "";
        String currentTime = System.currentTimeMillis() + "";
        newPin = currentTime.substring(currentTime.length() - 7, currentTime.length() - 1);
        return newPin;
    }


    public void sendEmail(final NetSocket socket, final MomoMessage msg, final SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "sendEmail");

        MomoProto.TextValueMsg textValueMsg;

        try {
            textValueMsg = MomoProto.TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            textValueMsg = null;
        }

//        final MomoProto.StandardReply.Builder builder = MomoProto.StandardReply.newBuilder();
        HashMap<String, String> hashMap = Misc.getKeyValuePairs(textValueMsg.getKeysList());

        final String appCode = hashMap.containsKey(StringConstUtil.EMAIL.APP_CODE) ? hashMap.get(StringConstUtil.EMAIL.APP_CODE) : "";
        final String appVer = hashMap.containsKey(StringConstUtil.EMAIL.APP_VER) ? hashMap.get(StringConstUtil.EMAIL.APP_VER) : "";
        final String subject = hashMap.containsKey(StringConstUtil.EMAIL.SUBJECT) ? hashMap.get(StringConstUtil.EMAIL.SUBJECT) : "";
        final String body = hashMap.containsKey(StringConstUtil.EMAIL.BODY) ? hashMap.get(StringConstUtil.EMAIL.BODY) : "";
        final String reference = hashMap.containsKey(StringConstUtil.EMAIL.REFERENCE) ? hashMap.get(StringConstUtil.EMAIL.REFERENCE) : "";
        final String emailType = hashMap.containsKey(StringConstUtil.EMAIL.EMAIL_TYPE) ? hashMap.get(StringConstUtil.EMAIL.EMAIL_TYPE) : "";
        log.add("appCode", appCode);
        log.add("appVer", appVer);
        log.add("subject", subject);
        log.add("body", body);
        Misc.sendEmail(glbCfg, subject, body, log);
        log.writeLog();

    }

    public void buildURLNapas(final NetSocket socket, final MomoMessage msg, final SockData data) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func", "buildURLNapas");
        JsonObject joNapas = glbCfg.getObject("napas", new JsonObject());
        String url = joNapas.getString("url_check_request", "https://sandbox.napas.com.vn/gateway/vpcpay.do?");
        JsonObject joKey = joNapas.getObject("key_napas", new JsonObject());
        Map<String, Object> mapKey = joKey.toMap();
        List<String> listParams = new ArrayList<>();
        if (mapKey != null && mapKey.keySet() != null && mapKey.keySet().iterator() != null) {
            Iterator<String> iteratorKey = mapKey.keySet().iterator();
            String key = "";
            String value = "";
            String params = "";

            while (iteratorKey.hasNext()) {
                key = iteratorKey.next();
                value = mapKey.get(key).toString();
                params = key + "=" + value;
                listParams.add(params);
            }
        }
        MomoProto.TextValueMsg textValueMsg;

        try {
            textValueMsg = MomoProto.TextValueMsg.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            textValueMsg = null;
        }

        HashMap<String, String> hashMap = Misc.getKeyValuePairs(textValueMsg.getKeysList());
        final String amount = hashMap.containsKey("amount") ? hashMap.get("amount") : "";
        final String cardType = hashMap.containsKey("cardType") ? hashMap.get("cardType") : "";
        final String paymentGateway = hashMap.containsKey("paymentGateway") ? hashMap.get("paymentGateway") : "";
        final String ticketNo = hashMap.containsKey("TicketNo") ? hashMap.get("TicketNo") : "";
        log.add("amount", amount);
        log.add("cardType", cardType);
        log.add("paymentGateway", paymentGateway);
        String paramApp = "vpc_Amount=" + amount + "&vpc_CardType=" + cardType + "&vpc_PaymentGateway=" + paymentGateway + "&vpc_MerchTxnRef=" + UUID.randomUUID().toString()
                + "&vpc_TicketNo=" + ticketNo;
        url = url + paramApp;

        String paramServer = "";
        for (int i = 0; i < listParams.size(); i++) {
            paramServer = paramServer + "&" + listParams.get(i).toString().trim();
        }
        String totalParam = paramApp + paramServer;
        String md5 = MD5(totalParam);

        url = url + paramServer + "&vpc_SecureHash=" + md5;
        log.add("url Total", url);
        MomoProto.TextValue textValue = MomoProto.TextValue.getDefaultInstance();
        MomoProto.TextValueMsg.Builder builder = MomoProto.TextValueMsg.newBuilder();
        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.BUILD_URL_NAPAS_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.addKeys(textValue.toBuilder().setText("urlNapas").setValue(url)).build()
                        .toByteArray()
        );
        mCommon.writeDataToSocket(socket, buf);
        log.writeLog();
    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

}
