package com.mservice.momo.web.internal.services;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TranErrConfDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.HttpResponseCommon;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by locnguyen on 30/07/2014.
 */
public class ControllerBankOutManual {

    private Vertx vertx;
    private Logger logger;
    private TranErrConfDb tranErrConfDb;

    private TransDb transDb;
    private PhonesDb phonesDb;

    public ControllerBankOutManual(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        tranErrConfDb = new TranErrConfDb(vertx.eventBus(), logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        phonesDb = new PhonesDb(vertx.eventBus(),logger);
    }

    @Action( path = "/bankoutmanual/updatestatus")
    public void updateBantOutManualStatus(HttpRequestContext context
            , final Handler<JsonObject> callback) {

        HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("function", "updateBantOutManualStatus");

        MultiMap params = request.params();
        String data = "";

        if ("GET".equalsIgnoreCase(request.method())){
            data = params.get("data");
        } else {
            data = context.postData;
        }

        log.add("data", data == null ? "null" : data);

        //
        JsonObject json = null;
        long tranid = 0;
        int phone = 0;
        int error = -1;
        String bankName = "";

        try {
            json = new JsonObject(data);

            tranid = DataUtil.stringToUNumber(json.getString("tranid","0")) ;
            log.add("tranid", tranid);

            phone = DataUtil.strToInt(json.getString("phone", "0"));
            log.setPhoneNumber("0" + phone);

            error = DataUtil.strToInt(json.getString("error","-1")) ;
            log.add("error code", error);

            bankName = json.getString("bankname","");
            log.add("bank name", bankName);
        } catch (Exception e) {
            log.add("exception intput", e.toString());
        }

        final JsonObject result = new JsonObject();

        //kiem tra input
        boolean isInputValid = true;
        if (tranid == 0){
            isInputValid =false;
            log.add("invalid tranid", tranid);
            result.putString("description", "tranid invalid");
        }

        if (phone < 1){
            isInputValid =false;
            log.add("invalid phone number", phone);
            result.putString("description", "phone invalid");
        }

        if ("".equalsIgnoreCase(bankName)){
            isInputValid =false;
            log.add("invalid bank name", bankName);
            result.putString("description", "bankname invalid");
        }

        if (error < 0){
            isInputValid =false;
            log.add("invalid error code", error);
            result.putString("description", "error code invalid");
        }

        result.putNumber("success", (isInputValid ? 0 : 1));

        // luon tra ket qua ve cho client
        HttpResponseCommon.response(context.getRequest(), result);

        if (!isInputValid) {
            log.writeLog();
            return;
        }

        final long tranid_final = tranid;
        final int phone_final = phone;
        final int errorCode = error;

        //khong cap nhat khi giao dich chuyen tien bankout manual ghi nhan la thanh cong
        if(errorCode == 0){
            log.writeLog();
            return;
        }

        tranErrConfDb.getErrorInfo(errorCode, -1, 1, 100, new Handler<TranErrConfDb.Obj>() {
            @Override
            public void handle(final TranErrConfDb.Obj errObj) {
                if (errObj == null) {
                    log.add("khong lay duoc thong tin loi voi error code ", errorCode);
                    log.writeLog();
                    return;
                }

                //format errorObj
                //source string: "Hoàn trả lại giao dịch rút tiền về ngân hàng %s . Lý do: Sai số tài khoản."
                //obj.desciption = String.format(obj.desciption,bankName_final);
                //obj.notiBody = String.format(obj.notiBody,bankName_final);

                //todo  : neu co loi thi thuc hien tiep cac buoc sau

                transDb.getTransactionDetail(phone_final, tranid_final, new Handler<TranObj>() {
                    @Override
                    public void handle(final TranObj tranObj) {

                        //lay giao dich goc theo tranId va so dien thoai
                        if (tranObj == null) {

                            log.add("Khong co giao dich goc voi tranid ", tranid_final);
                            log.writeLog();
                            return;
                        }

                        // cap nhat
                        log.add("tran Obj Old", tranObj.getJSON());
                        final long curTime = System.currentTimeMillis();
                        tranObj.finishTime = curTime;
                        tranObj.tranId = curTime;
                        tranObj.status = TranObj.STATUS_OK;
                        tranObj.error = 0;
                        tranObj.io = 1;
                        tranObj.comment =  String.format(errObj.notiBody,tranObj.partnerRef); //obj.notiBody;
                        tranObj.owner_number= phone_final;

                        //them cai gi do de app cap nhat thong tin len GUI.

                        log.add("tran obj new", tranObj.getJSON());
                        transDb.upsertTran(phone_final, tranObj.getJSON(), new Handler<TranObj>() {
                            @Override
                            public void handle(TranObj tranObj) {
                                log.add("update new tran obj success", (tranObj == null ? "failed" : "ok"));
                            }
                        });

                        //gui lai giao dich
                        BroadcastHandler.sendOutSideTransSync(vertx, tranObj);

                        /*log.add("begin send NOTIFICATION","");
                        log.add("phone number", "0" + mainObj.owner_number);
                        log.add("tranid", tranId);*/

                        // ban noti

                        phonesDb.getPhoneObjInfo(tranObj.owner_number, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj phoneObj) {
                                String token = (phoneObj == null ? "" : phoneObj.pushToken);
                                String os = (phoneObj== null ? "" : phoneObj.phoneOs);

                                Notification noti = new Notification();
                                noti.receiverNumber = tranObj.owner_number;
                                noti.caption = errObj.notiTitle;   //"Ngày MoMo";
                                noti.body =  String.format(errObj.notiBody,tranObj.partnerRef);//obj.notiBody;       //"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";
                                noti.sms = "";                  //"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.";
                                noti.priority = 2;
                                noti.time = curTime;
                                noti.tranId = curTime;
                                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                noti.os = os;
                                noti.token = token;

                                log.add("notification to send", noti.toJsonObject());
                                log.writeLog();

                                vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                        ,noti.toFullJsonObject()
                                        ,new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> message) {
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
