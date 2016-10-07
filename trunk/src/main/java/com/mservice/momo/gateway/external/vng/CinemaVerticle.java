package com.mservice.momo.gateway.external.vng;

import banknetvn.md5.checkMD5;
import com.mservice.momo.data.tracking.Tracking123PhimDb;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.net.ConnectException;
import java.rmi.RemoteException;

/**
 * Created by concu on 6/17/14.
 */
public class CinemaVerticle extends Verticle {

    public static final String COMMAND = "cmd";
    private final String content_type = "Content-Type";
    private String urlCreateOrder = "";
    private String urlConfirmOrder = "";
    private String urlCancelOrder = "";
    private String urlCheckStatus = "";
    private String host = "";
    private int port = 80;
    private String key = "";
    private String header_token_key = "";
    private String header_version_key = "";
    private String header_version_value = "";
    private String header_content_type = "";
    private Logger logger;
    private HttpClient httpClient;
    private boolean isUat;
    private boolean isStore;
    private Tracking123PhimDb tracking123PhimDb;
    public static String nullToDefault(String val, String def) {
        return (val != null ? val : def);
    }

    @Override
    public void start() {

        logger = container.logger();
        JsonObject glbCfg = container.config();
        loadCfg(glbCfg);

        httpClient = vertx.createHttpClient()
                .setHost(host)
                .setPort(port)
                .setMaxPoolSize(20)
                .setConnectTimeout(300000) // 5 phut
                .setKeepAlive(false);

        vertx.eventBus().registerLocalHandler(AppConstant.VinaGameCinemaVerticle_ADDRESS, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> msg) {
                JsonObject jo = msg.body();

                String cmdType = jo.getString(COMMAND, "");

                try {
                    switch (cmdType) {
                        case CmdType.CREATE_ORDER:
                            logger.info("CREATE_ORDER");
                            doCreateOrder(msg);
                            break;
                        case CmdType.CONFIRM_ORDER:
                            logger.info("CONFIRM_ORDER");
                            //for dev only
                            if(isUat)
                            {
                                JsonObject json = new JsonObject();
                                json.putNumber(vngClass.Res.error, 0);
                                json.putString(vngClass.Res.desc, "");
                                json.putString(vngClass.Res.date_confirm, "");
                                json.putString(vngClass.Res.ticket_code, "123456");
                                msg.reply(json);

                            }
                            else {
                                //productionnoti
                                doConfirmOrder(msg);
                            }
                            break;
                        case CmdType.CANCEL_ORDER:
                            logger.info("CANCEL_ORDER");
                            doCancelOrder(msg);
                            break;
                        default:
                            logger.info("CinemaVerticle not support for commands " + cmdType);
                            break;
                    }
                } catch (ConnectException ce) {
                    jo.putNumber(vngClass.Res.error, 5008); //timeout
                    jo.putString(vngClass.Res.desc, "Request timeout + 123phim");
                    logger.info("Exception " + ce.getMessage());
                    logger.info("Exception 123phim" + ce.getMessage());
                    //Khong tra ve app 23062016 khi exception
//                    msg.reply(jo);

                } catch (RemoteException re) {
                    jo.putNumber(vngClass.Res.error, 5008); //timeout
                    jo.putString(vngClass.Res.desc, "Request timeout + 123phim");
                    logger.info("Exception " + re.getMessage());
                    logger.info("Exception 123phim" + re.getMessage());
                    //Khong tra ve app 23062016 khi exception
//                    msg.reply(jo);

                } catch (Exception e) {
                    jo.putNumber(vngClass.Res.error, 9999); //loi he thong
                    jo.putString(vngClass.Res.desc, e.getMessage());
                    logger.info("Exception " + e.getMessage());
                    logger.info("Exception 123phim" + e.getMessage());
                    //Khong tra ve app 23062016 khi exception
//                    msg.reply(jo);
                }
            }
        });
    }

    private void doCreateOrder(final Message<JsonObject> msg) throws RemoteException, ConnectException {

        final JsonObject jo = msg.body();
        final String phoneNumber = jo.getString(vngClass.PHONE_NUMBER, "0");
        logger.info("doCreateOrder " + phoneNumber);
        final long time = jo.getLong(vngClass.TIME, 0);
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(phoneNumber);
        log.setTime(time);

        log.add("request url", urlCreateOrder);
        logger.info(urlCreateOrder.toString() + " " + phoneNumber);
        HttpClientRequest req = httpClient.post(urlCreateOrder, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse response) {
                int statusCode = response.statusCode();

                //neu request khong thanh cong-->bi server tu choi luon
                if (statusCode != 200) {

                    //411: length required
                    //401: unauthorized

                    msg.reply(
                            new JsonObject()
                                    .putNumber(vngClass.Res.error, (statusCode + 10000))
                                    .putString(vngClass.Res.desc, response.statusMessage())
                    );
                    log.add("error", statusCode);
                    log.add("desc", response.statusMessage());
                    log.writeLog();
                    return;
                }

                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        JsonObject jsonRpl = new JsonObject();

                        log.add("" +
                                "" +
                                "Rcv buffer", buffer.toString());

                        try {
                            JsonObject result = new JsonObject(buffer.toString());

                            log.add("Rcv json", result.toString());

                            //dat don hang bi loi
                            //{"error_code":1400,"error_description":"Seat already booked"}
                            if (result.getInteger(vngClass.ResError.error_code) != null) {

                                log.add("error", result.getInteger(vngClass.ResError.error_code));
                                log.add("desc", result.getString(vngClass.ResError.error_description));

                                int error = result.getInteger(vngClass.ResError.error_code);
                                if (error == 0) {
                                    error = -4992; // map voi banknet timeout
                                }

                                jsonRpl.putNumber(vngClass.Res.error, error + 10000);
                                jsonRpl.putString(vngClass.Res.desc, result.getString(vngClass.ResError.error_description));

                                msg.reply(jsonRpl);

                                log.writeLog();
                                return;
                            }

                            JsonObject last = result.getObject("result");
                            jsonRpl.putNumber(vngClass.Res.error, 0);
                            jsonRpl.putString(vngClass.Res.desc, "");
                            jsonRpl.putString(vngClass.Res.invoice_no, last.getString(vngClass.Res.invoice_no));
                            jsonRpl.putString(vngClass.Res.ticket_code, last.getString(vngClass.Res.ticket_code));
                            jsonRpl.putString(vngClass.Res.price_before, last.getString(vngClass.Res.price_before));
                            jsonRpl.putString(vngClass.Res.price_after, last.getString(vngClass.Res.price_after));

                            String session_time = last.getString(vngClass.Res.session_time, "");
                            String session_id = last.getString(vngClass.Res.session_id, "");
                            String film_name = last.getString(vngClass.Res.film_name, "");

                            jsonRpl.putString(vngClass.Res.session_id, session_id);
                            jsonRpl.putString(vngClass.Res.session_time, session_time);
                            jsonRpl.putString(vngClass.Res.film_name, film_name);

                            JsonObject jsonListPrice = last.getObject(vngClass.Res.list_price);
                            jsonRpl.putObject(vngClass.Res.list_price, jsonListPrice);

                            msg.reply(jsonRpl);

                        } catch (DecodeException e) {

                            jsonRpl.putNumber(vngClass.Res.error, 9999);
                            jsonRpl.putString(vngClass.Res.desc, "Unexpected result.");
                            logger.info("Exception 123phim" + e.getMessage());
                            //Khong tra ve app khi exception 23062016
//                            msg.reply(jsonRpl);
                        }
                        log.writeLog();
                        keepTrackInfoTran(jsonRpl.getString(vngClass.Res.session_id, ""), phoneNumber, System.currentTimeMillis(), jsonRpl);
                    }
                });
            }
        });

        buildHeader(req, log);
        buildParams(req, jo, log);

    }

    private void keepTrackInfoTran(String billId, String phoneNumber, long tranId, JsonObject joReply) {
        Tracking123PhimDb.Obj tra123PhimObj = new Tracking123PhimDb.Obj();
        tra123PhimObj.billId = billId;
        tra123PhimObj.phoneNumber = phoneNumber;
        tra123PhimObj.time = System.currentTimeMillis();
        tra123PhimObj.tranId = tranId;
        tra123PhimObj.joExtra = joReply;
        tracking123PhimDb.insert(tra123PhimObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {

            }
        });
    }

    private void buildHeader(HttpClientRequest req, Common.BuildLog log) {
        log.add("func", "buildHeader");
        log.add(header_version_key, header_version_value);
        log.add(content_type, header_content_type);

        String token = genTokenKey(log);
        req.putHeader(header_token_key, token);
        log.add(header_token_key, token);

        req.putHeader(header_version_key, header_version_value);
        req.putHeader(content_type, header_content_type);
    }

    private void buildParams(HttpClientRequest req, JsonObject jo, Common.BuildLog log) {

        /*public String session_id = "";
        public String customer_name = "";
        public String customer_email = "";
        public String customer_phone = "";
        public List<String> list_seat = null;
        public String client_ip = "118.69.210.244"; //fixed default value
        public String device_id = "";
        public String phone_number="";
        public long time =0;
        public String session_time ="";*/

        String cmdType = jo.getString(COMMAND, "");
        //refine json
        jo.removeField(COMMAND);
        Buffer buffer = null;
        switch (cmdType) {
            case CmdType.CREATE_ORDER:
                //test_key=momo
                // jo.putString("test_key","momo");
                String galaxy_info = jo.getString("galaxy_info", "");
                jo.removeField("galaxy_info");

                if (!"".equalsIgnoreCase(galaxy_info)) {
                    int app_code = jo.getInteger(StringConstUtil.APP_CODE, 0);
                    String os = jo.getString(StringConstUtil.APP_OS, "");
                    log.add("app code", app_code);
                    log.add("app os", os);
                    if(isUat || (isStore && StringConstUtil.ANDROID_OS.equalsIgnoreCase(os) && app_code > 121) || (!isStore && StringConstUtil.ANDROID_OS.equalsIgnoreCase(os) && app_code > 73)
                            || (isStore && StringConstUtil.IOS_OS.equalsIgnoreCase(os) && app_code > 110) || (!isStore && StringConstUtil.IOS_OS.equalsIgnoreCase(os) && app_code > 1923))
                    {
                        jo.putObject("booking_data", new JsonObject(galaxy_info));
                        log.add("booking_data", jo.getObject("booking_data").encodePrettily());
                    }
                    else {
                        jo.putObject("galaxy_info", new JsonObject(galaxy_info));
                        log.add("galaxy_info", jo.getObject("galaxy_info").encodePrettily());
                    }

                    JsonObject pi = new JsonObject();
                    pi.putString("cardHolderName", "");
                    pi.putString("customer_phone", jo.getString("customer_phone", ""));
                    pi.putString("customer_email", jo.getString("customer_email", ""));
                    pi.putString("customer_name", jo.getString("customer_email", ""));
                    pi.putString("site_type", "0");
                    pi.putString("udid", "");
                    pi.putString("payment_type", "");
                    pi.putString("bankCode", "");
                    pi.putString("cardNumber", "");
                    jo.putObject("payment_info", pi);

                    /*
                    "customer_phone": "0974540385",
                    "customer_email": "gdidjdk@gmail.com",
                    "customer_name": "LINH",

                    "payment_info": {
                        "cardHolderName": "HOANG DUC THIEN",
                        "customer_phone": "01274864436",
                        "customer_email": "d.thien.mmt04@gmail.com",
                        "customer_name": "d.thien.mmt04@gmail.com",
                        "site_type": "2",
                        "udid": "Android-8f9c829687c4d616",
                        "payment_type": "ATM",
                        "bankCode": "EIB",
                        "cardNumber": "9704310000859341"
                    }
                    */
                }
                buffer = new Buffer(jo.toString());

                log.add("request session_id", jo.getString("session_id", ""));
                log.add("request session_time", jo.getString("session_time", ""));
                log.add("request film_name", jo.getString("film_name", ""));
                log.add("customer_name", jo.getString("customer_name", ""));
                log.add("device_id", jo.getString("device_id", ""));

                log.add("SND create", jo.toString());
                log.add("Content-Length", buffer.length());
//                log.writeLog();
                logger.info("SND create " + jo.toString());
                req.putHeader("Content-Length", buffer.length() + "");
                req.end(buffer);
                break;

            case CmdType.CONFIRM_ORDER:
                //"invoice_no":20130603155335979315
                buffer = new Buffer(jo.toString());
                logger.info("Buffer confirm " + buffer.toString());
                logger.info("SND confirm " + jo.toString());
                logger.info("req confirm " + req);
                req.putHeader("Content-Length", buffer.length() + "");
                req.end(buffer);
                break;

            case CmdType.CANCEL_ORDER:
                //"invoice_no":20130603155335979315
                buffer = new Buffer(jo.toString());
                logger.info("SND cancel " + jo.toString());
                req.putHeader("Content-Length", buffer.length() + "");
                req.end(buffer);
                break;

            default:
                logger.info("buildParams not support command type " + cmdType);
                break;
        }
    }

    private void doConfirmOrder(final Message<JsonObject> msg) throws ConnectException, RemoteException {
        final JsonObject jo = msg.body();

        final String phoneNumber = jo.getString(vngClass.PHONE_NUMBER, "");
        final long time = jo.getLong(vngClass.TIME, 0);
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(phoneNumber);
        log.setTime(time);
        log.add("request url", urlConfirmOrder);
        final String invoice_no = jo.getString(vngClass.Res.invoice_no, "");

        HttpClientRequest req = httpClient.post(urlConfirmOrder, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse response) {
                int statusCode = response.statusCode();
                log.add("statusCode", statusCode);
                //neu request khong thanh cong-->bi server tu choi luon
                if (statusCode != 200) {
                    checkQueryStatus(0, invoice_no, log, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            int error = jsonObject.getInteger(vngClass.Res.error, -1);
                            if (error != 0) {
                                log.add("error", error);
                                log.add("desc", jsonObject.getString(vngClass.Res.desc, "error"));

                                log.writeLog();
                                msg.reply(jsonObject);
                                keepTrackInfoTran(jsonObject.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), jsonObject);
                                return;

                            }
                            log.add("error", error);
                            log.add("desc", jsonObject.getString(vngClass.Res.desc, ""));
                            log.add("checkquery_invoice_no", jsonObject.getString(vngClass.Res.invoice_no, "no invoice_no"));
                            log.add("checkquery_status_id", jsonObject.getString(vngClass.Res.status_id, "no status_id"));
                            log.add("checkquery_status_desc", jsonObject.getString(vngClass.Res.status_desc, "no desc"));
                            log.add("checkquery_date_add", jsonObject.getString(vngClass.Res.date_add, "no date add"));
                            log.add("checkquery_date_confirm", jsonObject.getString(vngClass.Res.date_confirm, "no date confirm"));
                            log.add("checkquery_date_cancel", jsonObject.getString(vngClass.Res.date_cancel, "no date cancel"));
                            log.add("checkquery_ticket_code", jsonObject.getString(vngClass.Res.ticket_code, "no ticket code"));
                            log.writeLog();
                            msg.reply(jsonObject);
                            keepTrackInfoTran(jsonObject.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), jsonObject);
                            return;

                        }
                    });

                    //411: length required
                    //401: unauthorized
//                    msg.reply(
//                            new JsonObject()
//                                    .putNumber(vngClass.Res.error, (statusCode + 10000))
//                                    .putString(vngClass.Res.desc, response.statusMessage())
//                    );
//                    log.add("error",statusCode);
//                    log.add("desc",response.statusMessage());
//                    log.writeLog();
//                      return;
                }
                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        JsonObject json = new JsonObject();
                        try {
                            JsonObject result = new JsonObject(buffer.toString());
                            log.add("Rcv confirm json", result.toString());

                            //xac nhan don hang bi loi
                            //{"error_code":1002,"error_description":"Could not confirm"}
                            if (result.getInteger(vngClass.ResError.error_code) != null) {

                                int error = result.getInteger(vngClass.ResError.error_code);
                                if (error == 0) {
                                    error = -4992;
                                }
                                //Transaction already confirmed
                                else if (error == 1002) {
                                    error = 1400; // ghe da co nguoi dat
                                } else if (error == 9400) {
                                    logger.info("123 PHIM " + phoneNumber + " DANG DUOC XU LY 123phim + result " + result.toString() );
                                    return;
//                                    checkQueryStatus(0, invoice_no, log, new Handler<JsonObject>() {
//                                        @Override
//                                        public void handle(JsonObject jsonObject) {
//                                            int error = jsonObject.getInteger(vngClass.Res.error, -1);
//                                            if (error != 0) {
//                                                log.add("error", error);
//                                                log.add("desc", jsonObject.getString(vngClass.Res.desc, "error"));
//
//                                                log.writeLog();
//                                                msg.reply(jsonObject);
//                                                return;
//                                            }
//                                            log.add("error", error);
//                                            log.add("desc", jsonObject.getString(vngClass.Res.desc, ""));
//                                            log.add("checkquery_invoice_no", jsonObject.getString(vngClass.Res.invoice_no, "no invoice_no"));
//                                            log.add("checkquery_status_id", jsonObject.getString(vngClass.Res.status_id, "no status_id"));
//                                            log.add("checkquery_status_desc", jsonObject.getString(vngClass.Res.status_desc, "no desc"));
//                                            log.add("checkquery_date_add", jsonObject.getString(vngClass.Res.date_add, "no date add"));
//                                            log.add("checkquery_date_confirm", jsonObject.getString(vngClass.Res.date_confirm, "no date confirm"));
//                                            log.add("checkquery_date_cancel", jsonObject.getString(vngClass.Res.date_cancel, "no date cancel"));
//                                            log.add("checkquery_ticket_code", jsonObject.getString(vngClass.Res.ticket_code, "no ticket code"));
//                                            log.writeLog();
//                                            msg.reply(jsonObject);
//                                            keepTrackInfoTran(jsonObject.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), jsonObject);
//                                            return;
//
//                                        }
//                                    });
                                }

                                json.putNumber(vngClass.Res.error, error + 10000);
                                json.putString(vngClass.Res.desc, result.getString(vngClass.ResError.error_description));

                                msg.reply(json);
                                keepTrackInfoTran(json.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), json);
                                log.writeLog();
                                return;
                            }

                            JsonObject last = result.getObject("result");
                            json.putNumber(vngClass.Res.error, 0);
                            json.putString(vngClass.Res.desc, "");
                            json.putString(vngClass.Res.date_confirm, last.getString(vngClass.Res.date_confirm));
                            json.putString(vngClass.Res.ticket_code, last.getString(vngClass.Res.ticket_code));

                            log.add("request result", last);

                            msg.reply(json);
                            keepTrackInfoTran(json.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), json);
                        } catch (DecodeException e) {

                            json.putNumber(vngClass.Res.error, 9999);
                            json.putString(vngClass.Res.desc, "Unexpected result.");

                            log.add("exception", e.getMessage());
                            logger.info("Exception 123phim" + e.getMessage());
                            //Khong tra ket qua ve cho app 23062016
//                            msg.reply(json);
                            keepTrackInfoTran(json.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), json);
                        }
                        log.writeLog();
                    }
                });
            }
        });
        buildHeader(req, log);
        buildParams(req, jo, log);

    }


    private void checkQueryStatus(final int tryCount, final String invoice_no, final Common.BuildLog log, final Handler<JsonObject> callback) {
        JsonObject joInvoice = new JsonObject();
        joInvoice.putString("invoice_no", invoice_no);
        joInvoice.putString(COMMAND, CmdType.CONFIRM_ORDER);
        log.add("func", "checkQueryStatus");
        log.add("invoice_no", invoice_no);
        String url = "".equalsIgnoreCase(urlCheckStatus) ? "/mservice/api/order/query?invoice_no=" : urlCheckStatus;
        logger.info(url);
        HttpClientRequest request = httpClient.post(url + invoice_no, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse httpClientResponse) {
                log.add("statusCode", httpClientResponse.statusCode());
                if (httpClientResponse.statusCode() != 200) {
                    log.add("tryCount", tryCount);
                    if (tryCount >= 3) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.putBoolean("result", false);
                        jsonObject.putNumber(vngClass.Res.error, -1000);
                        jsonObject.putString(vngClass.Res.desc, "Thu lai 3 lan khong ket noi duoc");
                        callback.handle(jsonObject);
//                        log.writeLog();
                        return;
                    } else {
                        vertx.setTimer(60000, new Handler<Long>() {
                            @Override
                            public void handle(Long aLong) {
                                checkQueryStatus(tryCount + 1, invoice_no, log, callback);
                            }
                        });
                    }
                }
                httpClientResponse.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {

                        JsonObject json = new JsonObject();
                        try {
                            JsonObject result = new JsonObject(buffer.toString());
                            log.add("Rcv confirm json", result.toString());
                            if (result.getInteger(vngClass.ResError.error_code) != null) {

                                int error = result.getInteger(vngClass.ResError.error_code, 1000);
                                log.add("error", error);
                                if (error == 0) {
                                    error = -4992;
                                }
                                //Transaction already confirmed
                                else if (error == 1002) {
                                    error = 1400; // ghe da co nguoi dat
                                } else if (error == 9400) {
//                                    checkQueryStatus(tryCount + 1, invoice_no, log, callback);
                                    //DONT REPLY TO APP
                                    logger.info("123 PHIM " + " DANG DUOC XU LY 123phim + result " + result.toString() );
                                    return;
                                }

                                json.putNumber(vngClass.Res.error, error + 10000);
                                json.putString(vngClass.Res.desc, result.getString(vngClass.ResError.error_description, ""));
//                                log.writeLog();
                                callback.handle(json);
                                return;
                            }

                            JsonObject last = result.getObject("result", new JsonObject());

                            JsonObject jsonObject = new JsonObject();
                            jsonObject.putBoolean("result", true);
                            json.putNumber(vngClass.Res.error, 0);
                            json.putString(vngClass.Res.desc, "");
                            jsonObject.putString(vngClass.Res.invoice_no, last.getString(vngClass.Res.invoice_no, ""));
                            jsonObject.putString(vngClass.Res.status_id, last.getString(vngClass.Res.status_id, ""));
                            jsonObject.putString(vngClass.Res.status_desc, last.getString(vngClass.Res.status_desc, ""));
                            jsonObject.putString(vngClass.Res.ticket_code, last.getString(vngClass.Res.ticket_code, ""));
                            jsonObject.putString(vngClass.Res.date_add, last.getString(vngClass.Res.date_add, ""));
                            jsonObject.putString(vngClass.Res.date_confirm, last.getString(vngClass.Res.date_confirm, ""));
                            jsonObject.putString(vngClass.Res.date_cancel, last.getString(vngClass.Res.date_cancel, ""));

//
                            log.add("request result", last);
                            callback.handle(jsonObject);
//
//                            msg.reply(json);

                        } catch (DecodeException e) {

                            json.putNumber(vngClass.Res.error, 9999);
                            json.putString(vngClass.Res.desc, "Unexpected result.");

                            log.add("exception", e.getMessage());
                            logger.info("Exception 123phim" + e.getMessage());
                            //Khong tra ve app khi exception 23062016
//                            callback.handle(json);
//                            msg.reply(json);
                        }
//                        log.writeLog();

                        //todo: parse buffer result
                    }
                });
            }
        });
        buildHeader(request, log);
        buildParams(request, joInvoice, log);

    }

    private void doCancelOrder(final Message<JsonObject> msg) throws ConnectException, RemoteException {
        final JsonObject jo = msg.body();
        final String phoneNumber = jo.getString(vngClass.PHONE_NUMBER, "");
        final long time = jo.getLong(vngClass.TIME, 0);
        final String invoice = jo.getString(vngClass.ConfirmOrCancel.invoice_no, "");

        if(Misc.isNullOrEmpty(invoice)){
            logger.info("invoice_no is null or empty" );
            msg.reply(msg);
            return;
        }

        HttpClientRequest req = httpClient.post(urlCancelOrder, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse response) {
                int statusCode = response.statusCode();

                //neu request khong thanh cong-->bi server tu choi luon
                if (statusCode != 200) {
                    logger.info("request " + urlCancelOrder + " statusCode,statusMessage " + response.statusCode() + "," + response.statusMessage());

                    //411: length required
                    //401: unauthorized
                    msg.reply(
                            new JsonObject()
                                    .putNumber(vngClass.Res.error, (statusCode + 10000))
                                    .putString(vngClass.Res.desc, response.statusMessage())
                    );
                    return;
                }

                response.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        JsonObject json = new JsonObject();

                        try {
                            JsonObject result = new JsonObject(buffer.toString());

                            logger.info("Rcv cancel json " + result.toString());

                            //huy don hang
                            if (result.getInteger(vngClass.ResError.error_code) != null) {

                                logger.info("request " + urlCancelOrder
                                                + " get error " + result.getInteger(vngClass.ResError.error_code)
                                                + " ,message " + result.getString(vngClass.ResError.error_description)
                                );

                                int error = result.getInteger(vngClass.ResError.error_code);
                                if (error == 0) {
                                    error = -4992;
                                }

                                json.putNumber(vngClass.Res.error, error + 10000);
                                json.putString(vngClass.Res.desc, result.getString(vngClass.ResError.error_description));
                                msg.reply(json);
                                return;
                            }
                            //dat don hang thanh cong
                            /*{ "invoice_no":20130603155335979315,
                               "date_cancel":"2013-06-03 15:53:35",
                            }*/

                            JsonObject last = result.getObject("result");
                            json.putNumber(vngClass.Res.error, 0);
                            json.putString(vngClass.Res.desc, "");
                            json.putString(vngClass.Res.invoice_no, last.getString(vngClass.Res.invoice_no));
                            json.putString(vngClass.Res.date_cancel, last.getString(vngClass.Res.date_cancel));
                            msg.reply(json);
                            keepTrackInfoTran(json.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), json);
                        } catch (DecodeException e) {

                            json.putNumber(vngClass.Res.error, 9999);
                            json.putString(vngClass.Res.desc, "Unexpected result.");
                            logger.info("request " + urlCancelOrder + " " + e.getMessage());
                            logger.info("Exception 123phim" + e.getMessage());
                            //Khong tra ket qua ve app khi exception 23062016
//                            msg.reply(json);
                            keepTrackInfoTran(json.getString(vngClass.Res.invoice_no, "no invoice_no"), phoneNumber, System.currentTimeMillis(), json);
                        }
                    }
                });
            }
        });

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(phoneNumber);
        log.setTime(time);
        buildHeader(req, log);
        buildParams(req, jo, log);

    }

    private void loadCfg(JsonObject glbCfg) {

        JsonObject jo = glbCfg.getObject("vng_123phim");
        host = jo.getString("host");
        port = jo.getInteger("port");
        key = jo.getString("key");
        urlCreateOrder = jo.getString("urlCreateOrder");
        urlConfirmOrder = jo.getString("urlConfirmOrder");
        urlCancelOrder = jo.getString("urlCancelOrder");
        urlCheckStatus = jo.getString("urlCheckStatus", "");
        header_token_key = jo.getString("header_token_key");
        header_version_key = jo.getString("header_version_key");
        header_version_value = jo.getString("header_version_value");
        header_content_type = jo.getString("header_content_type");
        isUat = glbCfg.getBoolean(StringConstUtil.IS_UAT, false);
        tracking123PhimDb = new Tracking123PhimDb(vertx, logger);
        isStore = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
    }

    private String genTokenKey(Common.BuildLog log) {
        log.add("func", "genTokenKey");

        String result = "";
        checkMD5 md5 = new checkMD5();
        long curTime = System.currentTimeMillis();
        boolean done = false;

        while (!done) {
            try {
                result = md5.getMD5Hash(key + System.currentTimeMillis() + "");
                done = true;
            } catch (Exception e) {
                log.add("genTokenKey Can not get MD5", e.getMessage());
            }
        }

        String token = result + " " + curTime;

        log.add("key", key);
        log.add("current time", curTime);
        log.add("token", token);

        return token;// result + " " + curTime;
    }

    public static class CmdType {

        public static final String CREATE_ORDER = "createorder";
        public static final String CONFIRM_ORDER = "confirmorder";
        public static final String CANCEL_ORDER = "cancelorder";
    }
}
