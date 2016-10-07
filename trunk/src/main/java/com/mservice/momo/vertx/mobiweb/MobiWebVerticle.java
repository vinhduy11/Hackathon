package com.mservice.momo.vertx.mobiweb;

import com.mservice.momo.data.M2cOffline;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftToNumber;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.ConnectProcess;
import com.mservice.momo.vertx.processor.Misc;
import httl.Engine;
import httl.Template;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hung_thai on 5/13/14.
 */
public class MobiWebVerticle extends Verticle {

    public static final String WEB_ROOT = "mobiweb";
    public static String ARR_GROUP_CFG = "";
    public static String ARR_CAPSET_CFG = "";
    public static String UPPER_LIMIT_CFG = "";
    Logger mLogger;
    Template tplFirst = null;
    Template tplGetCashPage = null;
    Template tplCompletePage = null;
    Template tplSecond = null;
    Template tplResult = null;
    Template tplGetGiftPage = null;
    M2cOffline m2cOffline = null;
    private PhonesDb phonesDb;
    private ConnectProcess connectProcess;
    private GiftManager giftManager;
    private HashMap<String, RegInfo> mRegMaps = new HashMap<String, RegInfo>();
    private int port = 80;

    public void start() {

        //mobiweb_cfg.putNumber("mobiweb_port", container.config().getNumber("mobiweb_port"));
        connectProcess = new ConnectProcess(vertx, container.logger(), getContainer().config());

        giftManager = new GiftManager(vertx, mLogger, container.config());

        port = getContainer().config().getInteger("mobiweb_port");
        mLogger = getContainer().logger();
        mLogger.info("MobiWebVerticle starting");
        m2cOffline = new M2cOffline(vertx.eventBus(), mLogger);
        phonesDb = new PhonesDb(vertx.eventBus(), mLogger);

        JsonObject cfg = getContainer().config().getObject("server", new JsonObject());
//        JsonObject cfg = getContainer().config();
        ARR_GROUP_CFG = cfg.getString("arr_group_cfg", "19112,34112");
        ARR_CAPSET_CFG = cfg.getString("arr_capset_cfg", "509");
        UPPER_LIMIT_CFG = cfg.getString("upper_limit_cfg", "5000000");

        try {
            tplFirst = Engine.getEngine().getTemplate(WEB_ROOT + "/first.html");
            tplSecond = Engine.getEngine().getTemplate(WEB_ROOT + "/second.html");
            tplResult = Engine.getEngine().getTemplate(WEB_ROOT + "/result.html");

            tplGetCashPage = Engine.getEngine().getTemplate(WEB_ROOT + "/getcashpage.html");
            tplCompletePage = Engine.getEngine().getTemplate(WEB_ROOT + "/completepage.html");

            tplGetGiftPage = Engine.getEngine().getTemplate(WEB_ROOT + "/getGiftPage.html");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //MobiTemplate mobiTemplate = new MobiTemplate(WEB_ROOT+"/index.html");

        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {

            public void handle(final HttpServerRequest req) {

                req.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        mLogger.debug("REQ END");
                    }
                });

                //the first page
                String firstPage = req.path().substring(1);
                if (req.params().size() == 0 && firstPage.length() == 7 && !firstPage.contains(".")) {
                    renderCashPage(req, firstPage);
                    return;
                } else if (req.params().size() == 0 && firstPage.length() == 5 && !firstPage.contains(".")) {
                    renderFirstPage(req, firstPage);
                    return;
                } else if (req.params().size() == 0 && firstPage.length() == 8 && !firstPage.contains(".")) {
                    renderGetGiftPage(req, firstPage);
                    return;
                } else if (req.params().size() > 0) {
                    boolean found = false;

                    final String secondPage = req.params().get("2");
                    if (secondPage != null) {
                        found = true;
                        String name = (req.params().get("Name") == null ? "" : req.params().get("Name"));
                        String idCard = (req.params().get("IdCard") == null ? "" : req.params().get("IdCard"));
                        String email = (req.params().get("Email") == null ? "" : req.params().get("Email"));
                        RegInfo info = mRegMaps.get(secondPage);
                        if (info == null || name.length() == 0 || idCard.length() == 0) {
                            mRegMaps.remove(secondPage);
                            renderFirstPage(req, secondPage);
                        } else {
                            renderSecondPage(req, secondPage, "");

                            //todo create an otp and set a token for this page

                            info.recvName = name;
                            info.recvIdCard = idCard;
                            info.recvEmail = email;
                            info.recvOtp = DataUtil.getOtp();

                            SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                                    .setSmsId(0)
                                    .setToNumber(info.recvNumber)
                                    .setContent("Ban dang nhan tien tu " + info.sendName + ". Hay dien day so " + info.recvOtp + " vao o Nhap_ma_pin_ban_nhan_duoc")
                                    .build();
                            vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());

                            mLogger.debug("Send SMS to " + info.recvNumber + " OTP : " + info.recvOtp);
                        }
                    }

                    final String thirdPage = req.params().get("3");
                    if (thirdPage != null) {
                        found = true;
                        mLogger.info("THIRD PAGE REQUEST " + thirdPage);

                        String oldPin = (req.params().get("OldPin") == null ? "" : req.params().get("OldPin"));
                        String newPin1 = (req.params().get("NewPin1") == null ? "" : req.params().get("NewPin1"));

                        boolean checkOk = false;
                        RegInfo info = mRegMaps.get(thirdPage);

                        if (info == null) {
                            renderFirstPage(req, thirdPage);
                        } else if (info != null
                                && info.recvOtp.equals(oldPin)
                                && !oldPin.equals(newPin1)
                                && DataUtil.isValidPin(newPin1)
                                ) {
                            info.recvPin = newPin1;
                            checkOk = true;
                        }

                        if (checkOk) {
                            //todo dang ky tai khoan moi, thuc hien chuyen tien, hoan tat giao dich
                            register(req, info, thirdPage);
                        } else {
                            renderSecondPage(req, thirdPage, "Mã pin không hợp lệ");
                        }
                    }

                    if (!found) {
                        render404(req);
                        return;
                    }


                } else {
                    final String path = WEB_ROOT + "/" + req.path().replace("..", "").replace("/", "");

                    vertx.fileSystem().exists(path, new Handler<AsyncResult<Boolean>>() {
                        @Override
                        public void handle(AsyncResult<Boolean> event) {
                            if (event.result() == true) {
                                req.response().sendFile(path);
                                return;
                            } else {
                                render404(req);
                                return;
                            }
                        }
                    });

                }
            }
        }).listen(port);
    }

    private void renderGetGiftPage(final HttpServerRequest req, final String code) {
        GiftToNumber filter = new GiftToNumber();
        filter.link = code;
        giftManager.getGn2TransactionInfo(filter, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error", -100);
                if (error == 0) {
                    final GiftToNumber giftToNumber = new GiftToNumber(result.getObject("giftToNumber"));
                    final Gift gift = new Gift(result.getObject("gift"));
                    final GiftType giftType = new GiftType(result.getObject("giftType"));

                    if (giftToNumber.status == GiftToNumber.STATUS_NEW) {

                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters.put("title", "Bạn đã nhận được một món quà!");
                        parameters.put("giftIcon", giftType.icon);
                        parameters.put("giftName", giftType.name);
                        parameters.put("giftAmount", String.format("%,d", gift.amount).replace(",", "."));
                        parameters.put("senderNumber", giftToNumber.fromAgent);
                        parameters.put("senderName", giftToNumber.senderName);

                        ByteArrayOutputStream os = new ByteArrayOutputStream(2048);

                        try {
                            tplGetGiftPage.render(parameters, os);
                            req.response().end(os.toString("UTF-8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    renderResultPage(req, "Giao dịch <" + code + "> không tồn tại. Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
                    return;
                }
                renderResultPage(req, "Giao dịch <" + code + "> không tồn tại. Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
            }
        });
    }

    public void renderFirstPage(final HttpServerRequest req, final String code) {
        mLogger.info("FIRST PAGE REQUEST " + code);
        final Common.BuildLog log = new Common.BuildLog(mLogger);

        m2cOffline.getObject(code, new Handler<M2cOffline.Obj>() {
            @Override
            public void handle(M2cOffline.Obj obj) {

                if (obj == null) {

                    renderResultPage(req
                            , "Không tìm thấy giao dịch <" + code + ">. Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
                    log.writeLog();
                    return;
                } else {
                    log.setPhoneNumber("0" + obj.DESTINATION_PHONE);

                    if (obj.STATUS.equalsIgnoreCase(colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW))) {

                        RegInfo info = new RegInfo();
                        info.code = code;
                        info.amount = Misc.formatAmount(obj.AMOUNT);
                        info.sendName = obj.NAME;//  "Thai Tri Hung";
                        info.sendNumber = obj.SOURCE_PHONE;//  983050910;
                        info.recvNumber = obj.DESTINATION_PHONE;//  938018568;
                        info.sendTranId = obj.TRAN_ID;
                        mRegMaps.put(code, info);

                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters.put("sendCode", code);
                        parameters.put("sendAmount", info.amount);
                        parameters.put("sendFirstChar", info.sendName.equalsIgnoreCase("") == true ? "" : info.sendName.substring(0, 1));
                        parameters.put("sendName", info.sendName);
                        parameters.put("sendNumber", "0" + info.sendNumber);
                        parameters.put("recvNumber", "0" + info.recvNumber);

                        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);

                        try {
                            tplFirst.render(parameters, os);
                            req.response().end(os.toString("UTF-8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                            req.response().end();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            req.response().end();
                        }

                        return;
                    } else {
                        //todo : da nhan tien or da huy
                        renderResultPage(req, "Giao dịch <" + code + "> đã được thực hiện hoặc bị hủy . Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
                        return;
                    }
                }
            }
        });
    }

    public void renderCashPage(final HttpServerRequest req, final String code) {
        mLogger.info("FIRST PAGE REQUEST " + code);
        final Common.BuildLog log = new Common.BuildLog(mLogger);

        m2cOffline.getObject(code, new Handler<M2cOffline.Obj>() {
            @Override
            public void handle(M2cOffline.Obj obj) {

                if (obj == null) {
                    renderResultPage(req
                            , "Không tìm thấy giao dịch <" + code + ">. Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
                    log.writeLog();
                    return;
                } else {
                    log.setPhoneNumber("0" + obj.DESTINATION_PHONE);

                    if (obj.STATUS.equalsIgnoreCase(
                            colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.NEW))) {

                        RegInfo info = new RegInfo();
                        info.code = code;
                        info.amount = Misc.formatAmount(obj.AMOUNT);
                        info.sendName = obj.NAME;//  "Thai Tri Hung";
                        info.sendNumber = obj.SOURCE_PHONE;//  983050910;
                        info.recvNumber = obj.DESTINATION_PHONE;//  938018568;
                        info.sendTranId = obj.TRAN_ID;
                        mRegMaps.put(code, info);

                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters.put("sendCode", code);
                        parameters.put("sendAmount", info.amount);
                        parameters.put("sendFirstChar", info.sendName.equalsIgnoreCase("") == true ? "" : info.sendName.substring(0, 1));
                        parameters.put("sendName", info.sendName);
                        parameters.put("sendNumber", "0" + info.sendNumber);
                        parameters.put("recvNumber", "0" + info.recvNumber);

                        ByteArrayOutputStream os = new ByteArrayOutputStream(2 * 1024);

                        try {
                            tplGetCashPage.render(parameters, os);
                            //tplFirst.render (parameters, os);
                            req.response().end(os.toString("UTF-8"));
                        } catch (IOException e) {
                            mLogger.error("render getcashpage", e);
                            req.response().end();
                        } catch (ParseException e) {
                            mLogger.error("render getcashpage", e);
                            req.response().end();
                        }
                        return;
                    } else if (obj.STATUS.equalsIgnoreCase(
                            colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.APPROVED))) {
                        renderResultPage(req, "", "Số tiền đã được nhận");
                        return;

                    } else if (obj.STATUS.equalsIgnoreCase(
                            colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.ROLLBACK))) {
                        renderResultPage(req, "Giao dịch <" + code + "> đã trả lại cho người gửi "
                                + obj.NAME
                                + "(0" + obj.SOURCE_PHONE + ")"
                                + ". Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
                        return;

                    } else if (obj.STATUS.equalsIgnoreCase(
                            colName.M2NumberCols.Status.getStatus(colName.M2NumberCols.Status.EXPIRED))) {
                        renderResultPage(req, "Giao dịch <" + code + "> đã trả lại cho người gửi "
                                + obj.NAME
                                + "(0" + obj.SOURCE_PHONE + ")"
                                + ". Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
                        return;

                    } else {

                        renderResultPage(req, "Giao dịch <" + code + "> không tìm thấy trên hệ thống. Gọi chúng tôi để biết thêm thông tin", "(08) 399 171 99");
                        return;
                    }
                }
            }
        });
    }

    private void register(final HttpServerRequest req, final RegInfo info, final String code) {

        Buffer signUpBuf = MomoMessage.buildBuffer(
                SoapProto.MsgType.REGISTER_VALUE,
                0,
                info.recvNumber,
                SoapProto.Register.newBuilder()
                        .setName(info.recvName)
                        .setIdCard(info.recvIdCard)
                        .setChannel(Const.CHANNEL_MOBI)
                        .setPin(info.recvPin)
                        .setArrGroup(ARR_GROUP_CFG)
                        .setArrCapset(ARR_CAPSET_CFG)
                        .setUpperLimit(UPPER_LIMIT_CFG)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, signUpBuf, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {

                /*reply.putNumber("result",resultToClient);
                reply.putBoolean("isaddnew",isNewUser);*/

                int rcode = result.body().getInteger("result", -1);
                boolean isAddNew = result.body().getBoolean("isaddnew", false);

                if (rcode == MomoProto.SystemError.ALL_OK_VALUE) {
                    renderResultPage(req, "Chúc mừng bạn đã nhận được : ", info.amount + "đ");

                    //cap nhat thong tin ve Obj
                    JsonObject jo = new JsonObject();
                    jo.putNumber(colName.PhoneDBCols.NUMBER, info.recvNumber);
                    jo.putString(colName.PhoneDBCols.CARD_ID, info.recvIdCard);
                    jo.putString(colName.PhoneDBCols.NAME, info.recvName);
                    jo.putString(colName.PhoneDBCols.EMAIL, info.recvEmail);
                    jo.putBoolean(colName.PhoneDBCols.DELETED, false);
                    if (isAddNew) {
                        jo.putBoolean(colName.PhoneDBCols.IS_NAMED, false);
                    }

                    phonesDb.updatePartialNoReturnObj(info.recvNumber, jo, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                        }
                    });

                    /*phonesDb.updatePartial(info.recvNumber,jo, new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(PhonesDb.Obj obj) {}
                    });*/
                } else {
                    renderResultPage(req, "Giao dịch chưa thành công ! Mã lỗi <" + result.body() + ">. Gọi chúng tôi để biết thêm thông tin.", "(08) 399 171 99");
                    return;

                }
            }
        });
    }

    public void renderSecondPage(HttpServerRequest req, String code, String lastErr) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("sendCode", code);
        parameters.put("lastErr", lastErr);

        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);

        try {
            tplSecond.render(parameters, os);
            req.response().end(os.toString("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return;

    }

    public void render404(HttpServerRequest req) {
        renderResultPage(req, "", "");
    }

    public void renderResultPage(HttpServerRequest req, String title, String content) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("title", title);
        parameters.put("content", content);

        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);

        try {
            tplResult.render(parameters, os);
            req.response().end(os.toString("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return;
    }

    public static class RegInfo {
        public String code = "";
        public String amount = "0";
        public String sendName = "";
        public int sendNumber = 0;
        public long sendTranId = 0;
        public int recvNumber = 0;
        public String recvName = "";
        public String recvIdCard = "";
        public String recvEmail = "";
        public String recvOtp = "";
        public String recvPass = "";
        public String recvPin = "";
    }

}
