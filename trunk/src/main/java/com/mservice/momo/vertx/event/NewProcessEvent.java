package com.mservice.momo.vertx.event;

import com.floreysoft.jmte.Engine;
import com.mservice.momo.data.*;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.gateway.internal.core.CoreCommon;
import com.mservice.momo.gateway.internal.core.objects.Response;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.event.bnhv.Noti;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by concu on 12/30/14.
 */
public class NewProcessEvent {
    private JsonObject glbCfg = null;
    private Vertx vertx;
    private com.mservice.momo.vertx.processor.Common common;
    private Logger logger;
    private TransDb transDb;
    private EventContentDb eventContentDb;
    private CDHHPayBack cdhhPayBack;
    private CDHH cdhh;
    private String payBack24h_content = "";
    private String payBack24h_header = "";

    public NewProcessEvent(JsonObject glbCfg, Vertx vertx, Logger logger) {
        this.glbCfg = glbCfg;
        this.vertx = vertx;
        this.logger = logger;
        this.transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, glbCfg);
        this.common = new com.mservice.momo.vertx.processor.Common(vertx, logger, glbCfg);
        cdhhPayBack = new CDHHPayBack(vertx.eventBus(), logger);
        this.eventContentDb = new EventContentDb(vertx, logger);
        cdhh = new CDHH(vertx, logger);
    }

    public void doEvent(final MomoMessage msg
            , final NetSocket sock
            , final SockData _data
            , final EventContentDb.Obj eventContent
            , final MomoProto.TranHisV1 tranHisV0) {

        // Use for build comment, body,... in notification
        final Map<String, Object> templateParams = new HashMap<String, Object>();

        HashMap<String, String> hashMap = Misc.getKeyValuePairs(tranHisV0.getKvpList());

        String tBillId = tranHisV0.getBillId() == null ? "" : tranHisV0.getBillId();

        final String billId = "".equalsIgnoreCase(tBillId) ? (hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "") : tBillId;

        // Split bill id to get SDB, Ho ten
        String[] tempBillId = billId.split("\\$", 2);
        final String code = (tempBillId.length > 0) ? tempBillId[0] : "";
        final String name = (tempBillId.length > 1) ? tempBillId[1] : "";
        templateParams.put("name", name);
        templateParams.put("code", code);

        String tmpServiceId = tranHisV0.getPartnerId() == null ? "" : tranHisV0.getPartnerId();

        final String serviceId = "".equalsIgnoreCase(tmpServiceId) ? (hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : "") : tmpServiceId;

        long tAmount = tranHisV0.getAmount();

        final long amount = (tAmount > 0 ? tAmount : hashMap.containsKey(Const.AppClient.Amount) ? DataUtil.stringToUNumber(hashMap.get(Const.AppClient.Amount)) : 0);

        // UPDATE: for each serviceId
        final long votedAmount = getVoteAmount(serviceId, amount);
        templateParams.put("amount_sms", votedAmount);

        final com.mservice.momo.vertx.processor.Common.BuildLog log = new com.mservice.momo.vertx.processor.Common.BuildLog(logger, "0" + msg.cmdPhone);
        log.add("service id", serviceId);
        log.add("msbc", billId);
        log.add("amount", amount);
        log.add("vote quantity", votedAmount);
        log.add("eventContent", eventContent);
        glbCfg.putObject("event_noti_resource", eventContent.toJson());
        log.add("glb", glbCfg);
        final MomoProto.TranHisV1 tranHisV1 = MomoProto.TranHisV1.newBuilder(tranHisV0).setBillId(name).build();
        payBack24h_content = getServiceResource(serviceId, "Body.PayBack24", templateParams);
        payBack24h_header = getServiceResource(serviceId, "Cap.PayBack24", templateParams);
        Misc.getCdhhWeekOrQuaterActive(vertx, serviceId, new Handler<CdhhConfig>() {
            @Override
            public void handle(final CdhhConfig cdhhConfig) {

                boolean isOpened = true;
                if (cdhhConfig == null) {
                    isOpened = false;
                }
                long time = System.currentTimeMillis();

                if ((cdhhConfig.endTime == null
                        || cdhhConfig.startTime == null) && isOpened) {
                    isOpened = false;
                }

                if (((cdhhConfig.endTime != null && time > cdhhConfig.endTime)
                        || (cdhhConfig.startTime != null && time < cdhhConfig.startTime))
                        && isOpened) {
                    isOpened = false;
                }

                if (isOpened == false) {
                    final long tid = System.currentTimeMillis();

                    //todo chua mo cua
                    String comment = getServiceResource(serviceId, "Body.NotOpened", templateParams); //Noti.Body.NotOpened;
                    Notification noti = new Notification();
                    noti.receiverNumber = msg.cmdPhone;
                    noti.caption = getServiceResource(serviceId, "Cap.NotOpened", templateParams); //Noti.Cap.NotOpened;
                    noti.body = comment;
                    noti.bodyIOS = comment;
                    noti.sms = "";
                    noti.tranId = tid; // tran id khi ban theo danh sach
                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                    noti.priority = 2;
                    noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                    noti.time = System.currentTimeMillis();

                    //ban notification
                    Misc.sendNoti(vertx, noti);

                    Misc.buildTranHisAndSend(msg
                            , 0
                            , MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE
                            , tid
                            , comment
                            , tranHisV1
                            , transDb
                            , common
                            , new JsonObject()
                            , Noti.Cap.NotOpened, sock);

                    return;

                }

                boolean perType = "day".equalsIgnoreCase(getPerType(serviceId));
                cdhh.findByNumberAndSection(cdhhConfig.collName, perType, msg.cmdPhone, serviceId, DataUtil.strToInt(code), new Handler<Integer>() {
                    @Override
                    public void handle(Integer count) {

                        long curTime = System.currentTimeMillis();
                        // UPDATE: for each serviceId
                        if (count + votedAmount > getMaxSMS(serviceId)) {

                            String comment = getServiceResource(serviceId, "Body.OverAmount", templateParams); //Noti.Body.OverAmount;
                            Notification noti = new Notification();
                            noti.receiverNumber = msg.cmdPhone;
                            noti.caption = getServiceResource(serviceId, "Cap.OverAmount", templateParams); //Noti.Cap.OverAmount;
                            noti.body = comment;
                            noti.bodyIOS = comment;
                            noti.sms = "";
                            noti.tranId = curTime; // tran id khi ban theo danh sach
                            noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                            noti.priority = 2;
                            noti.status = Notification.STATUS_DISPLAY;
                            noti.time = System.currentTimeMillis();

                            //ban notification
                            Misc.sendNoti(vertx, noti);

                            Misc.buildTranHisAndSend(msg
                                    , 0
                                    , MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE
                                    , curTime
                                    , comment
                                    , tranHisV1
                                    , transDb
                                    , common
                                    , new JsonObject()
                                    , Noti.Cap.OverAmount, sock);

                            return;
                        }

                        // UPDATE: for each serviceId
                        String toAgent = getRevieveAccount(serviceId, code);

                        // UPDATE: for each serviceId
                        if (isVoteViaCore(serviceId)) {

                            CoreCommon.voteVent(vertx, "0" + msg.cmdPhone, "", amount, toAgent, log, new Handler<Response>() {
                                @Override
                                public void handle(final Response requestObj) {

                                    long tid = System.currentTimeMillis();
                                    String comment = "";
                                    int error;
                                    String description = "Không đủ tiền bình chọn";
                                    long finalAmount = amount;

                                    log.add("error", requestObj.Error);
                                    log.add("desc", SoapError.getDesc(requestObj.Error));

                                    //soapObjReply.error = 100;
                                    if (requestObj.Error != 0) {

                                        error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
                                        finalAmount = 0;
                                        long curTime = System.currentTimeMillis();
                                        comment = getServiceResource(serviceId, "Body.NotEnoughCash", templateParams); //Noti.Body.NotEnoughCash;
                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.caption = getServiceResource(serviceId, "Cap.NotEnoughCash", templateParams); // Noti.Cap.NotEnoughCash;
                                        noti.body = comment;
                                        noti.bodyIOS = comment;
                                        noti.sms = "";
                                        noti.tranId = curTime; // tran id khi ban theo danh sach
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        noti.priority = 2;
                                        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                        noti.time = System.currentTimeMillis();
                                        noti.cmdId = msg.cmdIndex;
                                        //ban notification
                                        Misc.sendNoti(vertx, noti);

                                    } else {
                                        error = 0;
                                        description = "";
//                                        comment = String.format(Noti.Body.VoteOk, billId, String.valueOf(votedAmount));
                                        comment = getServiceResource(serviceId, "Body.VoteOk", templateParams);

                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.caption = getServiceResource(serviceId, "Cap.VoteOk", templateParams); // Noti.Cap.VoteOk;
                                        noti.body = comment;
                                        noti.bodyIOS = comment;
                                        noti.sms = "";
                                        noti.tranId = requestObj.Tid;
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        noti.priority = 2;
                                        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                        noti.time = System.currentTimeMillis();
                                        noti.cmdId = msg.cmdIndex;

                                        //ban notification
                                        Misc.sendNoti(vertx, noti);

                                        //todo tra lai tien khi test

                                        //todo save du lieu
                                        String name = (_data != null && _data.getPhoneObj() != null ? _data.getPhoneObj().name : "");

                                        // UPDATE: for each serviceId
                                        saveEventInfo(code, msg.cmdPhone, amount, votedAmount, requestObj.Tid, log, name, serviceId, cdhhConfig);

                                        Misc.getPayBackCDHHSetting(vertx, serviceId, new Handler<CDHHPayBackSetting.Obj>() {
                                            @Override
                                            public void handle(final CDHHPayBackSetting.Obj pbObj) {

                                                cdhhPayBack.getVotedAmount("0" + msg.cmdPhone, serviceId, new Handler<Integer>() {
                                                    @Override
                                                    public void handle(Integer votedAmout) {
                                                        if (votedAmout < 3) {
                                                            //ban noti hoan tien
                                                            sendNotiPayBack(msg.cmdPhone);
                                                        }
                                                    }
                                                });

                                                if ((pbObj != null) && (pbObj.status == true)) {
                                                    requestPayBack(requestObj.Tid
                                                            , msg.cmdPhone
                                                            , amount
                                                            , pbObj.paybackaccount
                                                            , pbObj.delaytime
                                                            , pbObj.paybackmax, serviceId);
                                                }
                                            }
                                        });
                                    }

                                    Misc.buildTranHisAndSend(msg
                                            , finalAmount
                                            , error
                                            , requestObj.Tid
                                            , comment
                                            , tranHisV1
                                            , transDb
                                            , common
                                            , new JsonObject()
                                            , description
                                            , sock);

                                    if (error == 0) {
                                        common.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                                    }
                                }
                            });

                        } else {

                            Misc.adjustment(vertx, "0" + msg.cmdPhone
                                    , toAgent
                                    , amount
                                    , WalletType.MOMO
                                    , null, log, new Handler<com.mservice.momo.vertx.processor.Common.SoapObjReply>() {
                                @Override
                                public void handle(final com.mservice.momo.vertx.processor.Common.SoapObjReply soapObjReply) {

                                    String comment = "";
                                    int error;
                                    String description = "Không đủ tiền bình chọn";
                                    long finalAmount = amount;

                                    log.add("error", soapObjReply.error);
                                    log.add("desc", SoapError.getDesc(soapObjReply.error));

                                    //soapObjReply.error = 100;
                                    if (soapObjReply.error != 0) {

                                        error = MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE;
                                        finalAmount = 0;
                                        long curTime = System.currentTimeMillis();
                                        comment = getServiceResource(serviceId, "Body.NotEnoughCash", templateParams); //Noti.Body.NotEnoughCash;
                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.caption = getServiceResource(serviceId, "Cap.NotEnoughCash", templateParams); //Noti.Cap.NotEnoughCash;
                                        noti.body = comment;
                                        noti.bodyIOS = comment;
                                        noti.sms = "";
                                        noti.tranId = curTime; // tran id khi ban theo danh sach
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        noti.priority = 2;
                                        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                        noti.time = System.currentTimeMillis();
                                        noti.cmdId = msg.cmdIndex;
                                        //ban notification
                                        Misc.sendNoti(vertx, noti);

                                    } else {
                                        error = 0;
                                        description = "";
                                        //comment = String.format(Noti.Body.VoteOk, billId, String.valueOf(votedAmount));
                                        comment = getServiceResource(serviceId, "Body.VoteOk", templateParams);
                                        Notification noti = new Notification();
                                        noti.receiverNumber = msg.cmdPhone;
                                        noti.caption = getServiceResource(serviceId, "Cap.VoteOk", templateParams); //Noti.Cap.VoteOk;
                                        noti.body = comment;
                                        noti.bodyIOS = comment;
                                        noti.sms = "";
                                        noti.tranId = soapObjReply.tranId;
                                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
                                        noti.priority = 2;
                                        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                        noti.time = System.currentTimeMillis();
                                        noti.cmdId = msg.cmdIndex;

                                        //ban notification
                                        Misc.sendNoti(vertx, noti);

                                        //todo save du lieu
                                        String name = (_data != null && _data.getPhoneObj() != null ? _data.getPhoneObj().name : "");

                                        // UPDATE: for each serviceId
                                        saveEventInfo(code
                                                , msg.cmdPhone
                                                , amount, votedAmount
                                                , soapObjReply.tranId
                                                , log
                                                , name
                                                , serviceId
                                                , cdhhConfig);

                                        Misc.getPayBackCDHHSetting(vertx, serviceId, new Handler<CDHHPayBackSetting.Obj>() {
                                            @Override
                                            public void handle(final CDHHPayBackSetting.Obj pbObj) {

                                                cdhhPayBack.getVotedAmount("0" + msg.cmdPhone, serviceId, new Handler<Integer>() {
                                                    @Override
                                                    public void handle(Integer votedAmout) {
                                                        if (votedAmout < 3) {
                                                            sendNotiPayBack(msg.cmdPhone);
                                                        }
                                                    }
                                                });

                                                if ((pbObj != null) && (pbObj.status == true)) {
                                                    requestPayBack(soapObjReply.tranId
                                                            , msg.cmdPhone
                                                            , amount
                                                            , pbObj.paybackaccount
                                                            , pbObj.delaytime
                                                            , pbObj.paybackmax, serviceId);
                                                }
                                            }
                                        });
                                    }

                                    Misc.buildTranHisAndSend(msg
                                            , finalAmount
                                            , error
                                            , soapObjReply.tranId
                                            , comment
                                            , tranHisV1
                                            , transDb
                                            , common
                                            , new JsonObject()
                                            , description
                                            , sock);

                                    if (error == 0) {
                                        common.sendCurrentAgentInfo(vertx, sock, msg.cmdIndex, msg.cmdPhone, _data);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    //bnhv2015.start
    private void requestPayBack(long tranId
            , int number
            , long amount
            , String paybackaccount
            , int delaytime
            , int paybackmax, String serviceId) {
        JsonObject jsonPayBackReq = new JsonObject();
        jsonPayBackReq.putString("number", "0" + number);
        jsonPayBackReq.putNumber("amount", getVoteAmount(serviceId, amount));
        jsonPayBackReq.putNumber("tranid", tranId);
        jsonPayBackReq.putString("pbacc", paybackaccount);
        jsonPayBackReq.putNumber("dtime", delaytime);
        jsonPayBackReq.putNumber("pbmax", paybackmax);
        jsonPayBackReq.putString("sid", serviceId);
        jsonPayBackReq.putObject("glbconfig", glbCfg);

        vertx.eventBus().send(AppConstant.PayBackCDHHVerticle_ADDRESS, jsonPayBackReq, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

            }
        });
    }


    private void saveEventInfo(final String code
            , int phoneNumber
            , final long amount
            , final long voteAmount
            , long tranid
            , final com.mservice.momo.vertx.processor.Common.BuildLog log
            , final String name
            , final String serviceId
            , final CdhhConfig cdhhConfig) {
        //todo save

        final CDHH.Obj obj = new CDHH.Obj();

        obj.code = String.valueOf(DataUtil.strToInt(code));
        obj.number = phoneNumber;
        obj.time = System.currentTimeMillis();
        obj.value = amount;
        obj.day_vn = Misc.dateVNFormat(System.currentTimeMillis());
        obj.voteAmount = voteAmount;
        obj.time_vn = Misc.dateVNFormatWithTime(System.currentTimeMillis());
        obj.tranid = tranid;
        obj.name = name;
        obj.serviceid = serviceId;

        log.add("save json", obj.toJson().encodePrettily());
        cdhh.save(obj, cdhhConfig, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
            }
        });
    }

    private void sendNotiPayBack(int rcvNumber) {

        String content = payBack24h_content.equalsIgnoreCase("") ? Noti.Body.PayBack24 : payBack24h_content;
        String header = payBack24h_header.equalsIgnoreCase("") ? Noti.Cap.PayBack24 : payBack24h_header;
        Notification noti = new Notification();
        noti.receiverNumber = rcvNumber;
        noti.caption = header;
        noti.body = content;
        noti.bodyIOS = content;
        noti.sms = "";
        noti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.priority = 2;
        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
        noti.time = System.currentTimeMillis();

        Misc.sendNoti(vertx, noti);
    }

    private long getVoteAmount(String serviceID, long amount) {

        JsonObject jsonObject = glbCfg.getObject(serviceID);
        if (jsonObject == null) {
            return 0;
        } else {
            int minVal = jsonObject.getInteger("min_val", 5000);
            return amount / jsonObject.getInteger("sms_price", minVal);
        }
    }

    //Check bau chon.
    private String getRevieveAccount(String serviceId, String msbc) {
        JsonObject jsonObject = glbCfg.getObject(serviceId);
        if (jsonObject == null) {
            return "";
        } else {
            JsonArray jsonArray = jsonObject.getArray("account");
            int idx = 0;
            int size = jsonArray.size();
            if (DataUtil.strToInt(msbc) > size) {
                idx = DataUtil.strToInt(msbc) - size - 1;
            } else {
                idx = DataUtil.strToInt(msbc) - 1;
            }

            if (idx >= size || idx < 0) {
                idx = 0;
            }
            return jsonArray.get(idx);
        }
    }

    private int getMaxSMS(String serviceId) {

        JsonObject jsonObject = glbCfg.getObject(serviceId);
        if (jsonObject == null) {
            return 0;
        } else {
            return jsonObject.getInteger("max_sms", 0);
        }
    }

    private String getPerType(String serviceId) {

        JsonObject jsonObject = glbCfg.getObject(serviceId);
        if (jsonObject == null) {
            return "day";
        } else {
            return jsonObject.getString("per_type", "day");
        }
    }

    private boolean isVoteViaCore(String serviceId) {
        JsonObject jsonObject = glbCfg.getObject(serviceId);
        if (jsonObject == null) {
            return false;
        } else {
            return jsonObject.getBoolean("vote_via_core", false);
        }
    }

    public String getServiceResource(String serviceId, String type, Map<String, Object> params) {
        Engine engine = new Engine();
        String resource = "";
//        JsonObject serviceResource = glbCfg.getObject("event_noti_resource").getObject(serviceId);
        JsonObject serviceResource = glbCfg.getObject("event_noti_resource");

        switch (type) {
            case "Body.NotOpened":
                resource = serviceResource.getObject("body").getString("NotOpened");
                break;
            case "Cap.NotOpened":
                resource = serviceResource.getObject("cap").getString("NotOpened");
                break;
            case "Body.OverAmount":
                resource = serviceResource.getObject("body").getString("OverAmount");
                break;
            case "Cap.OverAmount":
                resource = serviceResource.getObject("cap").getString("OverAmount");
                break;
            case "Body.NotEnoughCash":
                resource = serviceResource.getObject("body").getString("NotEnoughCash");
                break;
            case "Cap.NotEnoughCash":
                resource = serviceResource.getObject("cap").getString("NotEnoughCash");
                break;
            case "Body.VoteOk":
                resource = serviceResource.getObject("body").getString("VoteOk");
                break;
            case "Cap.VoteOk":
                resource = serviceResource.getObject("cap").getString("VoteOk");
                break;
            case "Cap.PayBack24":
                resource = serviceResource.getObject("cap").getString("PayBack24");
                break;
            case "Body.PayBack24":
                resource = serviceResource.getObject("body").getString("PayBack24");
                break;
        }
        return engine.transform(resource, params);
    }

}
