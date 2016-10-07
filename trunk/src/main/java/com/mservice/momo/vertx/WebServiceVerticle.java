package com.mservice.momo.vertx;

import com.mservice.momo.data.*;
import com.mservice.momo.data.wc.*;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.smartlink.SmartLinkResponse;
import com.mservice.momo.vertx.models.wc.*;
import com.mservice.momo.vertx.processor.ConnectProcess;
import com.mservice.momo.web.Cookie;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.Session;
import com.mservice.momo.web.external.services.pay123.Controller123Pay;
import com.mservice.momo.web.internal.services.ControllerBankOutManual;
import com.mservice.momo.web.internal.services.ControllerVcbMapWallet;
import com.mservice.momo.web.internal.services.ControllerWarningRetailer;
import com.mservice.momo.web.internal.webadmin.controller.TranContoller;
import com.mservice.momo.web.internal.webadmin.handler.ControllerMapper;
import com.mservice.momo.web.internal.webadmin.handler.RenderHandler;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.vertx.java.core.*;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

import java.util.*;

/**
 * Created by nam on 4/16/14.
 */
public class WebServiceVerticle extends Verticle {

    //    private static final String MOMO_KEYSTORE_PATH = "localhost.jks";
    private static final String MOMO_KEYSTORE_PATH = "momokeystore.jks";

    private static final String MOMO_KEYSTORE_PASSWORD = "MomoApp";
    private static int PORT = 8081;
    private static String HOST_ADDRESS = "0.0.0.0";

    public static final int CODE_PARAMS_ERROR = -2;
    public static final int CODE_INTERNAL_ERROR = -1;
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_NO_SUCH_ORDER = 1;


//    public static final int STATUS_PAID = 1;
//    public static final int STATUS_PENDING = 0;

    private PhonesDb phonesDb;

    private MatchDb matchDb;
    private DuDoanDb duDoanDb;
    private FullDuDoanDb fullDuDoanDb;
    private Map<String, Match> matches;

    private InviteeDb inviteeDb;
    private FullInviTeeDb fullInviTeeDb;

    private TrackerDb trackerDb;
    private TranContoller tranContoller;

    private String staticResourceDir;
    private ConnectProcess connectProcess;

    @Override
    public void start() {
        final Logger logger = container.logger();
        JsonObject globalConfig = container.config();
        JsonObject webServiceVerticleConfig = globalConfig.getObject("webServiceVerticle", new JsonObject());
        this.PORT = webServiceVerticleConfig.getInteger("port", 8081);
        this.HOST_ADDRESS = webServiceVerticleConfig.getString("hostAddress", "0.0.0.0");

        staticResourceDir = globalConfig.getObject("userResourceVerticle").getString("staticResourceDir", "/tmp");
        if (!staticResourceDir.endsWith("/"))
            staticResourceDir = staticResourceDir + "/";

        final Partner123MuaOrderDb m123muaOrderDb = new Partner123MuaOrderDb(vertx.eventBus(), container.logger());
        final AgentsDb mAgentDb = new AgentsDb(vertx.eventBus(), logger);
        phonesDb = new PhonesDb(vertx.eventBus(), container.logger());

        matches = new HashMap<>();
        matchDb = new MatchDb(vertx, container);
        duDoanDb = new DuDoanDb(vertx, container);
        fullDuDoanDb = new FullDuDoanDb(vertx, container);
        inviteeDb = new InviteeDb(vertx, container);
        fullInviTeeDb = new FullInviTeeDb(vertx, container);

        trackerDb = new TrackerDb(vertx.eventBus(), container.logger());
        tranContoller = new TranContoller(vertx, container);
        final ConcurrentSharedMap<String, JsonObject> sessions = vertx.sharedData().getMap(AppConstant.WebAdminVerticle_WEB_ADMIN_SESSION_MAP);

        final ControllerMapper controllerMapper = new ControllerMapper(vertx, container);

        controllerMapper.addController(new Controller123Pay(vertx,container));
        controllerMapper.addController(new ControllerBankOutManual(vertx,container));
        controllerMapper.addController(new ControllerVcbMapWallet(vertx,container));
        controllerMapper.addController(new ControllerWarningRetailer(vertx,container));

        final RenderHandler renderHandler = new RenderHandler(vertx, container);
        controllerMapper.setNextHandler(renderHandler);

        connectProcess = new ConnectProcess(vertx, container.logger(),globalConfig);

        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest request) {
                        logger.info("[WebService] [" + request.method() + "] " + request.uri());

                        String path = request.path();
                        if (path.equalsIgnoreCase("/stores/list")) {
                            getStoresList(vertx, container, mAgentDb, request);
                        } else if (path.equalsIgnoreCase("/123mua/order/status")) {
                            getOrderStatus(vertx, container, m123muaOrderDb, request);
                        } else if (path.equalsIgnoreCase("/momo/isMomoer")) {
                            isMomoerProcess(vertx, container, request);
                        } else if (path.equalsIgnoreCase("/wc/dudoan")) {
                            duDoan(request);
                        } else if (path.equalsIgnoreCase("/wc/ketqua")) {
                            ketQua(request);
                        } else if (path.equalsIgnoreCase("/wc/matchlist")) {
                            getMatchList(request);
                        } else if (path.equalsIgnoreCase("/wc/invite")) {
                            setInvite(request);
                        } else if (path.equalsIgnoreCase("/123phim/search")) {
                            phim123search(request);
                        } else if (path.equalsIgnoreCase("/123phim/tran")) {
                            phim123action(request);
                        } else if (path.equalsIgnoreCase("/smartLink/tranFinish")) {
                            smartLink_tranFinish(request);
                        } else if (path.equalsIgnoreCase("/smartLink/tranCancel")) {
                            smartLink_tranCancel(request);
                        } else if (path.equalsIgnoreCase("/resource/upload")) {
                            uploadFile(request);
//                        } else if (path.equalsIgnoreCase("/bankoutmanual/updatestatus")) {
//                            updateBantOutManualStatus(request);
                        } else if (path.equalsIgnoreCase("/momo/isPinCorrect")) {
                            isPinCorrect(request);
                        } else {
                            request.bodyHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer postData) {
                                    logger.info(postData.toString());
                                    String cookieString = request.headers().get("Cookie");
                                    if (cookieString == null || cookieString.trim().length() == 0) {
                                        cookieString = "{}";
                                    }
                                    Cookie cookie;
                                    try {
                                        cookie = new Cookie(cookieString);
                                    } catch (DecodeException e) {
                                        cookie = new Cookie("{}");
                                    }

                                    Session session = null;
                                    String sessionId = cookie.getString("sessionId");
                                    if (sessionId != null) {
                                        sessionId = sessionId.trim();
                                        if (sessionId.length() > 0) {
                                            JsonObject sessionJsonObject = sessions.get(sessionId);
                                            if (sessionJsonObject != null) {
                                                session = new Session(sessionId, sessionJsonObject);
                                            }
                                        }
                                    }
                                    if (session == null) {
                                        sessionId = UUID.randomUUID().toString();
                                        session = new Session(sessionId, "{}");
                                    }

                                    HttpRequestContext context = new HttpRequestContext(request, cookie, session);
                                    context.setPostParams(postData.toString());

                                    //Handling request
                                    controllerMapper.handle(context);
                                }
                            });
                        }

                    }
                })
                .setSSL(false)
                .setKeyStorePath(MOMO_KEYSTORE_PATH)
                .setKeyStorePassword(MOMO_KEYSTORE_PASSWORD)
                .setClientAuthRequired(false)
//                .setTrustStorePath(MOMO_KEYSTORE_PATH)
//                .setTrustStorePassword(MOMO_KEYSTORE_PASSWORD)
                .listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
                    @Override
                    public void handle(AsyncResult<HttpServer> event) {
                        if (event.succeeded()) {
                            logger.info("WebServiceVerticle's listening on " + HOST_ADDRESS + ":" + PORT);
                        }
                    }
                });
    }

    private void isPinCorrect(final HttpServerRequest request) {
        request.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void aVoid) {
                MultiMap attribute = request.params();
                String phone = attribute.get("phone");
                String p = attribute.get("pin");

                if (phone == null || p == null) {
                    response(request, new JsonObject()
                                    .putNumber("error", -1)
                                    .putString("desc", "Missing parameter.")
                    );
                    return;
                }

                Integer pNumber = null;
                try {
                    pNumber = Integer.parseInt(phone);
                } catch (NumberFormatException e) {

                }


                try {
                    p = new String(Hex.decodeHex(p.toCharArray()));
                } catch (DecoderException e) {
                    response(request, new JsonObject()
                                    .putNumber("error", -3)
                                    .putString("desc", "DecoderException.")
                    );
                    return;
                }

                p = p.replace("daotao.momo.vn", "");

                final String pin = p;

                if (pNumber == null) {
                    response(request, new JsonObject()
                                    .putNumber("error", -2)
                                    .putString("desc", "Invalid phone number.")
                    );
                    return;
                }

                final int phoneNumber = pNumber;

                phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(PhonesDb.Obj phoneObj) {

                        if (phoneObj == null) {
                            response(request, new JsonObject()
                                            .putNumber("error", 100)
                                            .putString("desc", "Agent not found.")
                            );
                            return;
                        }

                        final SockData sockData = new SockData(vertx,container.logger(), container.config());
                        sockData.setPhoneObj(phoneObj, container.logger(), "Web set phone object at begin request login");
                        sockData.isSetup = true;



                        MomoMessage msg = new MomoMessage(MomoProto.MsgType.LOGIN_VALUE
                                ,System.currentTimeMillis()
                                ,phoneNumber,
                                MomoProto.LogIn.newBuilder()
                                        .setMpin(pin)
                                        .setDeviceModel("")
                                        .build().toByteArray()
                        );

                        connectProcess.processLogIn(null, msg, sockData, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject loginResult) {
                                int error = loginResult.getInteger("error", 100);
                                long lockTill = loginResult.getLong("lockTill", 0);
                                response(request, new JsonObject()
                                                .putNumber("error", error)
                                                .putNumber("lockTill", lockTill)
                                );
                            }
                        });
                    }
                });
            }
        });
    }

    private void uploadFile(HttpServerRequest request) {
        request.expectMultiPart(true);
        request.uploadHandler(new Handler<HttpServerFileUpload>() {
            public void handle(HttpServerFileUpload upload) {
                upload.streamToFileSystem(staticResourceDir + upload.filename());
            }
        });
    }


    /**
     * url : http://10.10.10.14:28080/bankoutmanual/updatestatus  //dia chi IP se duoc config la IP server khi dua len production
     * <p/>
     * request params key: "data"  la json nhu sau:
     * data : {
     * tranid : 123456,
     * phone: 1222002002,
     * error : 2
     * bankname:"Ten ngan hang"
     * }
     * <p/>
     * response params key: "result" la json nhu sau:
     * result : {
     * success : 0 , // = 0 neu backend nhan thong tin thanh cong; != 0 neu ko thanh cong
     * description: "mo ta loi neu co loi"
     * }
     */
    /*private void updateBantOutManualStatus(final HttpServerRequest request) {

        final CoreCommon.BuildLog log = new CoreCommon.BuildLog(container.logger());

        log.add("function", "updateBantOutManualStatus");

        MultiMap params = request.params();

        String data = params.get("data");
        log.add("data", data);

        //
        JsonObject json = null;
        long tranid = 0;
        int phone = 0;
        int error = -1;
        String bankName = "";

        try {
            json = new JsonObject(data);

            tranid = json.getLong("tranid", 0);
            log.add("tranid", tranid);

            phone = DataUtil.strToInt(json.getString("phone", "0"));
            log.setPhoneNumber("0" + phone);

            error = json.getInteger("error", -1);
            log.add("error code", error);

            bankName = json.getString("bankname", "");
            log.add("bank name", bankName);
        } catch (Exception e) {
            log.add("exception intput", e.toString());
            log.writeLog();
        }

        final JsonObject result = new JsonObject();

        //kiem tra input
        boolean isInputValid = true;
        if (tranid == 0) {
            isInputValid = false;
            log.add("invalid tranid", tranid);
            result.putString("description", "tranid invalid");
        }

        if (phone < 1) {
            isInputValid = false;
            log.add("invalid phone number", phone);
            result.putString("description", "phone invalid");
        }

        if ("".equalsIgnoreCase(bankName)) {
            isInputValid = false;
            log.add("invalid bank name", bankName);
            result.putString("description", "bankname invalid");
        }

        if (error < 0) {
            isInputValid = false;
            log.add("invalid error code", error);
            result.putString("description", "error code invalid");
        }

        result.putNumber("success",(isInputValid ? 0 : 1));

        // luon tra ket qua ve cho client
        response(request, result);

        if (!isInputValid) {
            log.writeLog();
            return;
        }

        final long tranid_final = tranid;
        final int phone_final = phone;
        final String bankName_final = bankName;
        final int errorCode = error;


        tranErrConfDb.getErrorInfo(errorCode, -1, 1, 100, new Handler<TranErrConfDb.Obj>() {
            @Override
            public void handle(final TranErrConfDb.Obj obj) {
                if (obj == null) {
                    log.add("khong lay duoc thong tin loi voi error code ", errorCode);
                    log.writeLog();
                    return;
                }

                // format errorObj

                //source string: "Hoàn trả lại giao dịch rút tiền về ngân hàng %s . Lý do: Sai số tài khoản."
                obj.desciption = String.format(obj.desciption, bankName_final);
                obj.notiBody = String.format(obj.notiBody, bankName_final);

                //todo  : neu co loi thi thuc hien tiep cac buoc sau

                transDb.getTransactionDetail(phone_final, tranid_final, new Handler<TransDb.TranObj>() {
                    @Override
                    public void handle(final TransDb.TranObj tranObj) {

                        //lay giao dich goc theo tranId va so dien thoai
                        if (tranObj == null) {

                            log.add("Khong co giao dich goc voi tranid ", tranid_final);
                            log.writeLog();
                            return;
                        }

                        // cap nhat

                        log.add("tran Obj Old", tranObj.getJSON());
                        long curTime = System.currentTimeMillis();
                        tranObj.finishTime = curTime;
                        tranObj.tranId = curTime;
                        tranObj.status = TranObj.STATUS_FAIL;
                        tranObj.error = obj.errorCode;
                        tranObj.comment = obj.desciption;
                        tranObj.owner_number= phone_final;

                        //them cai gi do de app cap nhat thong tin len GUI.
                        //todo : anh linh hoi them app cap nhat vao truong nao
                        //

                        log.add("tran obj new", tranObj.getJSON());
                        transDb.upsertTran(phone_final, tranObj.getJSON(), new Handler<TransDb.TranObj>() {
                            @Override
                            public void handle(TransDb.TranObj tranObj) {
                                log.add("update new tran obj success", (tranObj == null ? "failed" : "ok"));
                            }
                        });

                        //gui lai giao dich
                        BroadcastHandler.sendOutSideTransSync(vertx, tranObj);

                        *//*log.add("begin send NOTIFICATION","");
                        log.add("phone number", "0" + mainObj.owner_number);
                        log.add("tranid", tranId);*//*

                        // ban noti

                        Notification noti = new Notification();
                        noti.receiverNumber = tranObj.owner_number;
                        noti.caption = obj.notiTitle;   //"Ngày MoMo";
                        noti.body = obj.notiBody;       //"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";
                        noti.sms = "";                  //"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.";
                        noti.priority = 1;
                        noti.time = curTime;
                        noti.tranId = curTime;
                        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

                        log.add("notification to send", noti.toJsonObject());
                        log.writeLog();

                        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                                , noti.toFullJsonObject(), new Handler<CoreMessage<JsonObject>>() {
                            @Override
                            public void handle(CoreMessage<JsonObject> message) {
                            }
                        });
                    }
                });
            }
        });
    }*/

    private void smartLink_tranFinish(final HttpServerRequest request) {
        request.expectMultiPart(true);
        System.err.println(request.method());

//        request.endHandler(new Handler<Void>() {
//            @Override
//            public void handle(Void aVoid) {
//
//                System.out.println(request.formAttributes().toString());
////                Iterator<Map.Entry<String, String>> i = request.formAttributes().iterator();
////                while(i.hasNext()) {
////                    System.err.println(i.next().toString());
////                }
//            }
//        });

        request.endHandler(new VoidHandler() {
            @Override
            protected void handle() {

                Iterator<Map.Entry<String, String>> i = request.formAttributes().iterator();
                while (i.hasNext()) {
                    container.logger().info("SmartLink callback: " + i.next().toString());
                }

                SmartLinkResponse response = new SmartLinkResponse();
                response.setValues(request.formAttributes());

                JsonObject cmd = new JsonObject();
                cmd.putString("cmd", SmartLinkVerticle.CMD_TRANSFER_COMPLETE);
                cmd.putObject("result", response.toJsonObject());
                vertx.eventBus().send(AppConstant.SmartLinkVerticle, cmd);

                request.response().end("Accepted!");
            }
        });
    }

    private void smartLink_tranCancel(HttpServerRequest request) {

    }

    private void phim123search(final HttpServerRequest request) {
        MultiMap params = request.params();

        long tranid = 0;
        if (params.get("tranid") != null && !"".equalsIgnoreCase(params.get("tranid")))
            tranid = DataUtil.stringToUNumber(params.get("tranid"));

        if (tranid == 0) {
            JsonArray results = new JsonArray();
            response(request, results.toString());
            return;
        }

        String invoiceNo = "";
        if (params.get("invoiceno") != null && !"".equalsIgnoreCase(params.get("invoiceno")))
            invoiceNo = params.get("invoiceno");

        String ticketCode = params.get("ticketcode");
        if (params.get("ticketcode") != null && !"".equalsIgnoreCase(params.get("ticketcode")))
            ticketCode = params.get("ticketcode");

        long fromDate = 0;
        if (params.get("fromdate") != null && !"".equalsIgnoreCase(params.get("fromdate")))
            fromDate = DataUtil.stringToUNumber(params.get("fromdate"));

        long toDate = 0;
        if (params.get("todate") != null && !"".equalsIgnoreCase(params.get("todate")))
            toDate = DataUtil.stringToUNumber(params.get("todate"));

        trackerDb.getTrackerInfo(tranid, invoiceNo, ticketCode, fromDate, toDate, 1, 100, new Handler<ArrayList<TrackerDb.Obj>>() {
            @Override
            public void handle(ArrayList<TrackerDb.Obj> objs) {
                JsonArray results = new JsonArray();
                if (objs != null) {
                    for (int i = 0; i < objs.size(); i++) {
                        results.add(objs.get(i).toJsonObj());
                    }
                }
                response(request, results.toString());
            }
        });
    }

    private void phim123action(final HttpServerRequest request) {
        tranContoller.transaction(new HttpRequestContext(request, null, null), new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject o) {
                JsonObject result = new JsonObject();
                if (o == null) {
                    result.putNumber("error", -1);
                    result.putString("desc", "SysError");
                } else {
                    int error = o.getInteger("error", -100);
                    result.putNumber("error", error);
                    if (error == 0) {
                        result.putString("desc", "Successful");
                    } else if (error == -100) {
                        result.putString("desc", "Input invalid");
                    } else if (error == -10) {
                        result.putString("desc", "Not support action");
                    } else if (error == -1) {
                        result.putString("desc", "SysError");
                    } else {
                        result.putString("desc", "Failed");
                    }
                }
                response(request, result);
            }
        });
    }

    private void setInvite(final HttpServerRequest request) {
        MultiMap params = request.params();
        String inviterParam = params.get("inviter");
        String inviteeParam = params.get("invitee");

        if (inviterParam == null || inviteeParam == null) {
            response(request,
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "Missing parameter")
            );
            return;
        }


        Integer inviterPhone = null;
        Integer inviteePhone = null;

        try {
            inviterPhone = Integer.parseInt(inviterParam);
            inviteePhone = Integer.parseInt(inviteeParam);
        } catch (NumberFormatException e) {
            container.logger().info("Phone number has invalid value");
        }

        if (inviterPhone == null || inviteePhone == null) {
            response(request,
                    new JsonObject()
                            .putNumber("error", 3)
                            .putString("desc", "Invalid value.")
            );
            return;
        }

        Long time = new Date().getTime();

        final FullInvitee fullInvitee = new FullInvitee();
        fullInvitee.invitee = inviteePhone;
        fullInvitee.inviter = inviterPhone;
        fullInvitee.time = time;

        fullInviTeeDb.save(fullInvitee, new Handler<String>() {
            @Override
            public void handle(String s) {
                response(request,
                        new JsonObject()
                                .putNumber("error", 0)
                                .putString("desc", "Successful.")
                );
            }
        });

        final Invitee invitee = new Invitee();
        invitee.inviter = inviterPhone;
        invitee.time = time;
        invitee.setModelId(String.valueOf(inviteePhone));


        Invitee inviteeFilter = new Invitee();
        inviteeFilter.setModelId(String.valueOf(inviteePhone));
        inviteeDb.findOne(inviteeFilter, new Handler<Invitee>() {
            @Override
            public void handle(Invitee eixstingInvitee) {
                if (eixstingInvitee != null) {
                    container.logger().error("Invitee is already exist! " + fullInvitee);
                    return;
                }
                Invitee inviteeFilter = new Invitee();
                inviteeFilter.inviter = invitee.inviter;
                inviteeDb.count(inviteeFilter, new Handler<Long>() {
                    @Override
                    public void handle(Long nInvitee) {
                        if (nInvitee >= 10) {
                            container.logger().error("Number has more than 10 inviter! " + fullInvitee);
                            return;
                        }
                        if (fullInvitee.inviter == fullInvitee.invitee) {
                            container.logger().error("Invitee == Inviter! " + fullInvitee);
                            return;
                        }
                        inviteeDb.save(invitee, new Handler<String>() {
                            @Override
                            public void handle(String s) {
                                container.logger().info("Saved new Invitee " + invitee);
                            }
                        });
                    }
                });
            }
        });
    }

    private void getMatchList(HttpServerRequest request) {

        StringBuilder out = new StringBuilder();

        out.append("<html><body>");
        out.append("<table border=1>");
        out.append(Match.getHtmlTableHeader());
        out.append("</table>");
        out.append("</body></html>");

        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "text/html; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(out.toString());

        request.response().end();
    }

    private void ketQua(final HttpServerRequest request) {
        MultiMap params = request.params();
        String tran = params.get("tran");
        if (tran == null) {
            response(request,
                    new JsonObject()
                            .putNumber("error", 1)
                            .putString("desc", "Tran is missing")
            );
            return;
        }
        Match filter = new Match();
        filter.setModelId(tran);
        matchDb.findOne(filter, new Handler<Match>() {
            @Override
            public void handle(final Match match) {
                if (match == null) {
                    response(request,
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("desc", "MatchId is not found.")
                    );
                    return;
                }
                final DuDoan duDoan = new DuDoan();
                duDoan.setMatchId(match.getModelId());
                duDoanDb.count(duDoan, new Handler<Long>() {
                    @Override
                    public void handle(final Long soNguoiChoi) {
                        DuDoan filter = new DuDoan();
                        filter.setMatchId(match.getModelId());
                        filter.setResult(match.getResult());
                        duDoanDb.count(filter, new Handler<Long>() {
                            @Override
                            public void handle(final Long soNguoiDungKetQua) {
                                DuDoan filter = new DuDoan();
                                filter.setMatchId(match.getModelId());
                                filter.setA(match.getA());
                                filter.setB(match.getB());
                                duDoanDb.count(filter, new Handler<Long>() {
                                    @Override
                                    public void handle(Long soNguoiDungTiSo) {
                                        response(request,
                                                new JsonObject()
                                                        .putObject("tran", match.getPersisFields())
                                                        .putNumber("soNguoiChoi", soNguoiChoi)
                                                        .putNumber("soNguoiDungKetQua", soNguoiDungKetQua)
                                                        .putNumber("soNguoiDungTiSo", soNguoiDungTiSo)
                                                        .putNumber("soTienKetQua", soNguoiDungKetQua != 0 ? soNguoiChoi * 500 / soNguoiDungKetQua : 0)
                                                        .putNumber("soTienTiso", soNguoiDungTiSo != 0 ? soNguoiChoi * 500 / soNguoiDungTiSo : 0)
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

    private void duDoan(final HttpServerRequest request) {
        container.logger().info("ZALO REQUEST: " + request.path());
        MultiMap params = request.params();

        String dt = params.get("dt");
        String tran = params.get("tran");
        String kq = params.get("kq");
        String ts = params.get("ts");
        String time = params.get("t");
        String zaloId = params.get("id");

        DuDoan duDoan = null;
        try {
            duDoan = DuDoan.buildFromWebServiceRequest(dt, tran, kq, ts, time, zaloId);
        } catch (IllegalArgumentException e) {
            response(request,
                    new JsonObject()
                            .putNumber("error", 1)
                            .putString("desc", e.getMessage())
            );
            container.logger().info("RESPONSE ZALO: " + 1);
            return;
        }

        FullDuDoan fullDuDoan = new FullDuDoan();
        fullDuDoan.setValues(duDoan);
        fullDuDoanDb.save(fullDuDoan, null);


        Match match = matches.get(duDoan.getMatchId());
        if (match == null) {
            response(request,
                    new JsonObject()
                            .putNumber("error", 2)
                            .putString("desc", "MatchId doesn't exist.")
            );
            container.logger().info("RESPONSE ZALO: " + 2);
            return;
        }

//        System.out.println("startTime: " + new Date(match.getStartTime()));
//        System.out.println("curTime: " + new Date(duDoan.getTime()));
//        System.out.println("endTime: " + new Date(match.getEndTime()));
//        if (duDoan.getTime() < match.getStartTime()) {
//            response(request,
//                    new JsonObject()
//                            .putNumber("error", 3)
//                            .putString("desc", "Out of time.")
//            );
//            container.logger().info("RESPONSE ZALO: " + 3);
//            return;
//        }
        if (duDoan.getTime() > match.getEndTime()) {
            response(request,
                    new JsonObject()
                            .putNumber("error", 4)
                            .putString("desc", "Out of time.")
            );
            container.logger().info("RESPONSE ZALO: " + 4);
            return;
        }

        duDoan.setTranError(-1);
        duDoan.setMoney(0L);

        final DuDoan fDuDoan = duDoan;

        duDoanDb.upsertWithZaloTimeChecking(fDuDoan, new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                // dù cho ZaloTime có nhỏ hơn thì cũng phải response successfull vì thống nhất bỏ qua lỗi = 6
                response(request,
                        new JsonObject()
                                .putNumber("error", 0)
                                .putString("desc", "Successfully.")
                                .putNumber("time", fDuDoan.getTime())
                );
                container.logger().info("Update DuDoan result: " + result);
                container.logger().info("RESPONSE ZALO: " + request.path() + "<<" + 0);
//                return;
//                if (result) {
//                }
//                response(request,
//                        new JsonObject()
//                                .putNumber("error", 6)
//                                .putString("desc", "Last request time is higher.")
//                );
//                container.logger().info("RESPONSE ZALO: " + 6);
            }
        });
//
//        duDoanDb.findOne(fDuDoan, new Handler<DuDoan>() {
//            @Override
//            public void handle(DuDoan lastDuDoan) {
//                if (lastDuDoan != null && lastDuDoan.getZaloTime() > fDuDoan.getZaloTime()) {
//                    response(request,
//                            new JsonObject()
//                                    .putNumber("error", 6)
//                                    .putString("desc", "Last request time is higher.")
//                    );
//                    container.logger().info("RESPONSE ZALO: " + 6);
//                    return;
//                }
//                duDoanDb.update(fDuDoan, true, new Handler<Boolean>() {
//                    @Override
//                    public void handle(Boolean event) {
//                        response(request,
//                                new JsonObject()
//                                        .putNumber("error", 0)
//                                        .putString("desc", "Successfully.")
//                                        .putNumber("time", fDuDoan.getTime())
//                        );
//                        container.logger().info("RESPONSE ZALO: " + request.path() + "<<" + 0);
//                    }
//                });
//            }
//        });
    }

    private void isMomoerProcess(Vertx vertx, Container container, final HttpServerRequest request) {
        ArrayList<Integer> phones = new ArrayList<Integer>();
        for (String key : request.params().names()) {
            key = key.trim();
            key.replace("+84", "");
            if (key.startsWith("84")) {
                key = key.substring(2, key.length());
            }
            try {
                phones.add(Integer.parseInt(key));
            } catch (NumberFormatException e) {
                //forget invalid numbers.
            }
        }
        phonesDb.checkWhoIsMomoer(phones, new Handler<List<Integer>>() {
            @Override
            public void handle(List<Integer> momoers) {
                request.response().putHeader("Content-Type", "application/json; charset=UTF-8");
                request.response().end(String.valueOf(momoers));
            }
        });
    }

    private void getOrderStatus(Vertx vertx, Container container, Partner123MuaOrderDb m123muaOrderDb, final HttpServerRequest request) {
        final JsonObject result = new JsonObject();
        String orderId = request.params().get("orderId");
        if (orderId != null) {
            orderId = orderId.trim();
        }

        if (orderId == null || orderId.length() == 0) {
            result.putNumber("code", CODE_PARAMS_ERROR);
            response(request, result);
            return;
        }

        m123muaOrderDb.getOrder(orderId, new Handler<Partner123MuaOrderDb.Order>() {
            @Override
            public void handle(Partner123MuaOrderDb.Order order) {
                if (order == null) {
                    result.putNumber("code", CODE_NO_SUCH_ORDER);
                    response(request, result);
                    return;
                }
                result.putNumber("code", CODE_SUCCESS);
                result.putNumber("status", order.status);
                if (order.status == 1) {
                    result.putNumber("time", order.time);
                }
                response(request, result);
            }
        });
    }

    private void getStoresList(Vertx vertx, Container container, AgentsDb mAgentDb, final HttpServerRequest request) {
        final JsonObject result = new JsonObject();
        String lo = request.params().get("lo");
        String la = request.params().get("la");

        if (lo != null && la != null) {
            try {
                double longitude = Double.parseDouble(lo);
                double latitude = Double.parseDouble(la);

                mAgentDb.getStores(longitude, latitude, 0, 0, 20, new org.vertx.java.core.Handler<List<AgentsDb.StoreInfo>>() {
                    @Override
                    public void handle(List<AgentsDb.StoreInfo> objects) {
                        JsonArray stores = new JsonArray();

                        if (objects != null) {
                            for (AgentsDb.StoreInfo item : objects) {
                                JsonObject storeInfo = new JsonObject();
                                storeInfo.putNumber("la", item.loc.Lat);
                                storeInfo.putNumber("lo", item.loc.Lng);
                                storeInfo.putString("name", item.storeName);
                                storeInfo.putString("aName", item.name);
                                storeInfo.putString("aPhone", item.phone);
                                storeInfo.putString("add", item.address);
                                storeInfo.putString("street", item.street);
                                storeInfo.putString("ward", item.ward);
                                storeInfo.putString("did", String.valueOf(item.districtId));
                                storeInfo.putString("cid", String.valueOf(item.cityId));
                                stores.add(storeInfo);
                            }
                        }
                        result.putNumber("code", CODE_SUCCESS);
                        result.putArray("stores", stores);
                        response(request, result);
                    }
                });

            } catch (NumberFormatException e) {
                container.logger().debug("Param parsing exception", e);
                result.putNumber("code", CODE_PARAMS_ERROR);
                response(request, result);
            }
            return;
        }

        result.putNumber("code", CODE_PARAMS_ERROR);
        response(request, result);
    }

    public static void response(HttpServerRequest request, JsonObject jsonObject) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(jsonObject.toString());

        request.response().end();
    }

    public static void response(HttpServerRequest request, String result) {
        request.response().setChunked(true);

        request.response().putHeader("Status", "200 OK");
        request.response().putHeader("Server", "localhost");
        request.response().putHeader("Content-Type", "application/json; charset=utf-8");
        request.response().putHeader("Access-Control-Allow-Origin", "*");

        request.response().write(result);

        request.response().end();
    }


    public static void main(String args[]) {
        try {
            System.out.println(new String(Hex.decodeHex("31323334353664616F74616F2E6D6F6D6F2E766E".toCharArray())));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
    }
}
