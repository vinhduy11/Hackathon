package com.mservice.momo.web.internal.webadmin.verticle;

import com.mservice.momo.cloud.ControllerCloud;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.Cookie;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.controller.*;
import com.mservice.momo.web.internal.webadmin.handler.ControllerMapper;
import com.mservice.momo.web.internal.webadmin.handler.RenderHandler;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Administrator on 2/26/14.
 */
public class WebAdminVerticle extends Verticle {
//    private static String WEB_ADMIN_SESSION_MAP = "WebAdminVerticle.maps.session";


    public static String STATIC_FILE_DIRECTORY = "/tmp/";
    public static boolean isAvatarStorage;
    private static int PORT = 8080;
    private static String HOST_ADDRESS = "0.0.0.0";

    @Override
    public void start() {

        final Logger logger = container.logger();
        JsonObject globalConfig = container.config();
        JsonObject webAdminVerticleConfig = globalConfig.getObject("webAdminVerticle", new JsonObject());

        //because of forcing all setting per local machine we mapped code
        /*
        if(!webAdminVerticleConfig.getBoolean("enable", false)) {
            logger.info("WEB ADMIN IS DISABLE!!!");
            return;
        }*/

        this.PORT = webAdminVerticleConfig.getInteger("port", 9669);
        this.HOST_ADDRESS = webAdminVerticleConfig.getString("hostAddress", "0.0.0.0");


        JsonObject userResourceVerticle = container.config().getObject("userResourceVerticle", new JsonObject());
        STATIC_FILE_DIRECTORY = userResourceVerticle.getString("staticResourceDir","/tmp");
        if (!STATIC_FILE_DIRECTORY.endsWith("/"))
            STATIC_FILE_DIRECTORY += "/";
        isAvatarStorage = userResourceVerticle.getBoolean("isAvatarStorage", false);

        final Map<String, JsonObject> sessions = new HashMap<>();

        final ControllerMapper controllerMapper = new ControllerMapper(vertx, container);
        controllerMapper.addController(new Login(vertx, container));
        controllerMapper.addController(new User());
        controllerMapper.addController(new ConfigInfoCtrl(vertx));
        controllerMapper.addController(new Statistic(vertx));
        controllerMapper.addController(new WcController(vertx, container));
        controllerMapper.addController(new TranContoller(vertx, container));
        controllerMapper.addController(new WebAdminController(vertx, container, STATIC_FILE_DIRECTORY));
        controllerMapper.addController(new WebAdminControllerExtend(vertx, container, STATIC_FILE_DIRECTORY));
        controllerMapper.addController(new ControllerCloud(vertx,container,STATIC_FILE_DIRECTORY));
        controllerMapper.addController(new TranErrorController(vertx, container));
        controllerMapper.addController(new ErrorCodeController(vertx, container,globalConfig));
        controllerMapper.addController(new CheckCreateDateController(vertx, container,globalConfig));
        controllerMapper.addController(new PromotionReportController(vertx, container,globalConfig));
        controllerMapper.addController(new PromotionController(vertx, container));
        controllerMapper.addController(new ServicePartnerController(vertx, container));
        controllerMapper.addController(new HomeController(vertx, container));
        controllerMapper.addController(new ConnectorPathController(vertx, container));
        controllerMapper.addController(new TranConfigController(vertx, container));
        controllerMapper.addController(new MinMaxTranController(vertx, container));
        controllerMapper.addController(new FeeController(vertx, container));
        controllerMapper.addController(new ServiceFeeController(vertx, container));
        controllerMapper.addController(new StoreCommentCtrl(vertx, container));
        controllerMapper.addController(new GiftController(vertx, container, globalConfig));
        controllerMapper.addController(new PaybackSettingController(vertx, container, globalConfig));
        controllerMapper.addController(new ServiceCateController(vertx, container, globalConfig));
        controllerMapper.addController(new UpdateReferalController(vertx, container, globalConfig));
        controllerMapper.addController(new VcbController(vertx,container));

        //baohv
        controllerMapper.addController(new GalaxyPromoController(vertx, container));
        controllerMapper.addController(new UploadImagesController(vertx, container, ""));
        controllerMapper.addController(new PgController(vertx, container));

        //cong nguyen add new return gift tool
        controllerMapper.addController(new ReturnGiftController(vertx, container));

        final RenderHandler renderHandler = new RenderHandler(vertx, container);

        controllerMapper.setNextHandler(renderHandler);

        //processesChain.add(Socket)

        vertx.createHttpServer()
                .requestHandler(new Handler<HttpServerRequest>() {
                    @Override
                    public void handle(final HttpServerRequest request) {

                        if (request.path().startsWith("/uploadimage")) {

                            //todo process here

                            request.expectMultiPart(true);
                            request.uploadHandler(new Handler<HttpServerFileUpload>() {
                                @Override
                                public void handle(final HttpServerFileUpload file) {

                                    final String dirPath = STATIC_FILE_DIRECTORY;

                                    final String filePath = dirPath
                                            + file.filename();

                                    final Buffer dataBuf = new Buffer();

                                    file.dataHandler(new Handler<Buffer>() {
                                        @Override
                                        public void handle(final Buffer buffer) {

                                            dataBuf.appendBuffer(buffer);

                                            file.endHandler(new Handler<Void>() {
                                                @Override
                                                public void handle(Void aVoid) {
                                                    long size = file.size();
                                                    final String data = dataBuf.toString();

                                                    if (size == 0) {
                                                        logger.info("No data found when upload");
                                                        return;
                                                    }

                                                    try {

                                                        String newfilename = System.currentTimeMillis() + "_" + file.filename();

                                                        String fileName = "/home/java1711/Documents/imgs/" + newfilename;

                                                        OutputStream outputStream = new FileOutputStream(fileName);
                                                        outputStream.write(dataBuf.getBytes(), 0, dataBuf.getBytes().length);
                                                        outputStream.close();

                                                        request.params().add("hfile", newfilename);

                                                    } catch (Exception e) {
                                                        System.out.println("Exception: " + e.getMessage());
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            });

                            request.bodyHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer postData) {

                                    String cookieString = request.headers().get("Cookie");
                                    System.out.println("Cookie: " + cookieString);
                                    if (cookieString == null || cookieString.trim().length() == 0) {
                                        cookieString = "{}";
                                    }
                                    Cookie cookie;
                                    try {
                                        cookie = new Cookie(cookieString);
                                    } catch (DecodeException e) {
                                        cookie = new Cookie("{}");
                                        cookie.putNumber("time", System.currentTimeMillis());
                                    }

                                    JsonObject session = null;
                                    String sessionId = cookie.getString("sessionId");
                                    if (sessionId != null) {
                                        sessionId = sessionId.trim();
                                        if (sessionId.length() > 0) {
                                            session = sessions.get(sessionId);
                                        }
                                    }
                                    if (session == null) {
                                        sessionId = UUID.randomUUID().toString();
                                        session = new JsonObject()
                                                .putString("id", sessionId);
                                        sessions.put(sessionId, session);
                                    }

                                    HttpRequestContext context = new HttpRequestContext(request, cookie, session);
                                    controllerMapper.handle(context);
                                }
                            });

                            return;
                        }

                        //upload file to send noti by file
                        if (request.path().startsWith("/sendnoti/upload")
                                || request.path().startsWith("/uploadFile")
                                || request.path().startsWith("/uploadVtbOnline")
                                || request.path().startsWith("/uploadCustomerCareFile")
                                || request.path().startsWith("/uploadBillPayProFile")
                                || request.path().startsWith("/uploadGiftReturnCustomerFile")
                                || request.path().startsWith("/uploadVisaPromoFile")
                                || request.path().startsWith("/uploadRollbackPromoFile")
                                || request.path().startsWith("/uploadCustomerCareDollarHeartGroupFile")
                                || request.path().startsWith("/uploadCashback7PercentPromoFile")
                                || request.path().startsWith("/uploadLiXiFile")
                                || request.path().startsWith("/uploadEmailPopupFile")) {
                            doSaveFile(request, logger);

                        } else if (request.path().startsWith("/upload")) {
                            if (!isAvatarStorage) {
                                request.response().putHeader("Content-Type", "application/json; charset=UTF-8");
                                request.response().putHeader("access-control-allow-origin", "*");
                                request.response().end(new JsonObject()
                                        .putNumber("error", 1)
                                        .putString("desc", "This isn't static file storage server.")
                                        .toString());
                                return;
                            }

                            request.expectMultiPart(true);
                            request.uploadHandler(new Handler<HttpServerFileUpload>() {
                                @Override
                                public void handle(final HttpServerFileUpload httpServerFileUpload) {
                                    String path = request.path().replace("/upload/", "");
                                    if (!path.endsWith("/"))
                                        path += "/";
                                    final String dirPath = STATIC_FILE_DIRECTORY + path;

                                    httpServerFileUpload.streamToFileSystem(dirPath + httpServerFileUpload.filename());

                                }
                            });
                            request.endHandler(new Handler<Void>() {
                                @Override
                                public void handle(Void aVoid) {
                                    request.response().putHeader("Content-Type", "application/json; charset=UTF-8");
                                    request.response().putHeader("access-control-allow-origin", "*");
                                    request.response().end(new JsonObject().putNumber("error", 0).toString());
                                }
                            });
                        } else
                            request.expectMultiPart(true);
                        request.endHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                System.out.println("end");
                            }
                        });

                        request.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer postData) {

                                String cookieString = request.headers().get("Cookie");
                                System.out.println("Cookie: " + cookieString);
                                if (cookieString == null || cookieString.trim().length() == 0) {
                                    cookieString = "{}";
                                }
                                Cookie cookie;
                                try {
                                    cookie = new Cookie(cookieString);
                                } catch (DecodeException e) {
                                    cookie = new Cookie("{}");
                                    cookie.putNumber("time", System.currentTimeMillis());
                                }

                                JsonObject session = null;
                                String sessionId = cookie.getString("sessionId");
                                if (sessionId != null) {
                                    sessionId = sessionId.trim();
                                    if (sessionId.length() > 0) {
                                        session = sessions.get(sessionId);
                                    }
                                }
                                if (session == null) {
                                    sessionId = UUID.randomUUID().toString();
                                    session = new JsonObject()
                                            .putString("id", sessionId);
                                    sessions.put(sessionId, session);
                                }

                                HttpRequestContext context = new HttpRequestContext(request, cookie, session);

                                controllerMapper.handle(context);
                            }
                        });
                    }
                })

                .listen(PORT, HOST_ADDRESS, new Handler<AsyncResult<HttpServer>>() {
                    @Override
                    public void handle(AsyncResult<HttpServer> event) {
                        if (event.failed()) {
                            logger.error("WebAdminVerticle can't listen on " + HOST_ADDRESS + ":" + PORT);
                            event.cause().printStackTrace();
                            return;
                        }
                        logger.info("WebAdmin is listening on " + HOST_ADDRESS + ":" + PORT);
                    }
                });
    }

    private void doSaveFile(final HttpServerRequest request, final Logger logger) {
        request.expectMultiPart(true);
        request.uploadHandler(new Handler<HttpServerFileUpload>() {
            @Override
            public void handle(final HttpServerFileUpload file) {
                final String dirPath = STATIC_FILE_DIRECTORY;

                final String filePath = dirPath + file.filename();

                final Buffer dataBuf = new Buffer();

                file.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(final Buffer buffer) {

                        dataBuf.appendBuffer(buffer);

                        file.endHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void aVoid) {
                                long size = file.size();
                                final String data = dataBuf.toString();
                                //logger.info(data);

                                if (size == 0) {
                                    logger.info("No data found when upload");
                                    return;
                                }
                                vertx.fileSystem().exists(dirPath, new Handler<AsyncResult<Boolean>>() {
                                    @Override
                                    public void handle(AsyncResult<Boolean> isDirectoryExisted) {
                                        if (!isDirectoryExisted.result()) {
                                            vertx.fileSystem().mkdir(dirPath, true, new Handler<AsyncResult<Void>>() {
                                                @Override
                                                public void handle(AsyncResult<Void> voidAsyncResult) {

                                                    vertx.fileSystem().exists(filePath, new Handler<AsyncResult<Boolean>>() {
                                                        @Override
                                                        public void handle(AsyncResult<Boolean> fileExist) {
                                                            if (fileExist.result()) {
                                                                vertx.fileSystem().delete(filePath, new Handler<AsyncResult<Void>>() {
                                                                    @Override
                                                                    public void handle(AsyncResult<Void> voidAsyncResult) {
                                                                        boolean result = Misc.writeFile(data, filePath);
                                                                        logger.info("Save file to " + filePath + " result " + result);
                                                                    }
                                                                });
                                                            } else {
                                                                boolean result = Misc.writeFile(data, filePath);
                                                                logger.info("Save file to " + filePath + " result " + result);
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        } else {
                                            vertx.fileSystem().exists(filePath, new Handler<AsyncResult<Boolean>>() {
                                                @Override
                                                public void handle(AsyncResult<Boolean> fileExist) {
                                                    if (fileExist.result()) {
                                                        vertx.fileSystem().delete(filePath, new Handler<AsyncResult<Void>>() {
                                                            @Override
                                                            public void handle(AsyncResult<Void> voidAsyncResult) {
                                                                boolean result = Misc.writeFile(data, filePath);
                                                                logger.info("Save file to " + filePath + " result " + result);
                                                            }
                                                        });
                                                    } else {
                                                        boolean result = Misc.writeFile(data, filePath);
                                                        logger.info("Save file to " + filePath + " result " + result);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    /*private void loadData(Logger logger, String filePath) {
        CoreCommon.BuildLog log = new CoreCommon.BuildLog(logger);
        log.setPhoneNumber("readfile");
        log.add("filepath", filePath);
        ArrayList<Object> arrayList = Misc.readFile(filePath, log);

        while ((QPhones !=null)  && (QPhones.size() > 0) ){
            QPhones.poll();
        }

        for (int i = 0; i < arrayList.size(); i++) {
            int phone = DataUtil.strToInt(arrayList.get(i).toString());
            if (phone > 0) {
                QPhones.add(phone);
            }
        }
    }*/
}
