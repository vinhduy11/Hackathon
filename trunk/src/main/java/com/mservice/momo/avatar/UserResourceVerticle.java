package com.mservice.momo.avatar;

import com.mservice.momo.vertx.AppConstant;
import org.apache.commons.lang3.RandomStringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by nam on 5/9/14.
 */
public class UserResourceVerticle extends Verticle {
    public static final String CMD_GET_TOKEN = "UserResourceVerticle.CMD_GET_TOKEN";
    public static final long DEFAULT_TOKEN_TIMEOUT = 5 * (1000 * 60); // 5 minutes
    public static final long MAX_FILE_SIZE = 512 * 1024; // 512kBs
    public static final String DEFAULT_AVATAR_IMAGE = "avatar/avatar-default.png";
    public static String staticResourceDir = "/tmp/";
    private static final String PUSH_NEW_TOKEN_ADDRESS = "newTokenBus";
    private Logger logger;

    @Override
    public void start() {
        JsonObject globalConfig = container.config();
        JsonObject userResourceVerticleConfig = globalConfig.getObject("userResourceVerticle", new JsonObject());

        final String host = userResourceVerticleConfig.getString("address", "0.0.0.0");
        final int port = userResourceVerticleConfig.getInteger("port", 8446);
        String rawAvatarDir = userResourceVerticleConfig.getString("avatarDir", "/tmp");
        logger= container.logger();

        staticResourceDir = userResourceVerticleConfig.getString("staticResourceDir","/tmp/");
        if(!staticResourceDir.endsWith("/")) {
            staticResourceDir += "/";
        }

        final boolean isAvatarStorage = userResourceVerticleConfig.getBoolean("isAvatarStorage", false);

        if (!rawAvatarDir.endsWith("/")) {
            rawAvatarDir = rawAvatarDir + "/";
        }
        final String avatarDir = rawAvatarDir;

        HttpServer server = vertx.createHttpServer();

        server.setTCPNoDelay(false);
        server.setReuseAddress(true);
        server.setTCPKeepAlive(false);
//        server.setReceiveBufferSize(1024);
//        server.setSendBufferSize(1024);
//        server.set

//        final ConcurrentSharedMap<String, String> tokens = vertx.sharedData().getMap("userResourceToken");
//        final Set<String> avatarUpdatingPhones = vertx.sharedData().getSet("avatarUpdatingPhones");
        final HashMap<String, String> tokens = new HashMap<>();
        final HashSet<String> avatarUpdatingPhones = new HashSet<>();

        if(isAvatarStorage) {
            vertx.eventBus().registerHandler(PUSH_NEW_TOKEN_ADDRESS, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    final String phone = message.body().getString("phone");
                    final String token = message.body().getString("token");
                    tokens.put(token, phone);
                    avatarUpdatingPhones.add(phone);

                    vertx.setTimer(DEFAULT_TOKEN_TIMEOUT, new org.vertx.java.core.Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            tokens.remove(token);
                            avatarUpdatingPhones.remove(phone);
                        }
                    });
                }
            });
        }

        server.requestHandler(new org.vertx.java.core.Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest request) {
                if(logger != null){
                    logger.debug("[" + request.method() + "]" + request.path());
                }

                if (request.path().startsWith("/service/avatar/get")) {
                    String rawPhoneNumber = request.params().get("phoneNumber");
                    String rawDefaultAvatar = request.params().get("default");

                    if (rawPhoneNumber == null) {
                        responseAvatarRequest(request.response(), new JsonObject()
                                        .putNumber("result", 1)
                                        .putString("description", "Missing parameters: token.")
                                , false
                        );
                        return;
                    }

                    if (!rawPhoneNumber.startsWith("0")) {
                        rawPhoneNumber = "0" + rawPhoneNumber;
                    }

                    final boolean defaultAvatar = "true".equalsIgnoreCase(rawDefaultAvatar);
                    final String phoneNumber = rawPhoneNumber;

                    if (avatarUpdatingPhones.contains(phoneNumber)) { // This user's avatar is updating.
                        responseAvatarRequest(request.response(), new JsonObject()
                                        .putNumber("result", 6)
                                        .putString("description", "Avatar is updating.")
                                , defaultAvatar
                        );
                        return;
                    }

                    vertx.fileSystem().exists(avatarDir + phoneNumber
                                                , new org.vertx.java.core.Handler<AsyncResult<Boolean>>() {
                        @Override
                        public void handle(AsyncResult<Boolean> isFileExist) {
                            if (isFileExist.result()) {
                                request.response().putHeader("Content-Type", "image/png");

                                if (request == null || request.response() == null)
                                    return;
                                try {
                                    request.response().sendFile(avatarDir + phoneNumber
                                            , new Handler<AsyncResult<Void>>() {
                                        @Override
                                        public void handle(AsyncResult<Void> event) {
                                            if (event.failed()) {
//                                            request.response().end();
                                                if(logger != null){
                                                    logger.warn("Send avatar image fail.", event.cause());
                                                }
//                                            if (event.cause() instanceof FileNotFoundException) {
//                                                responseJson(request.response(), new JsonObject()
//                                                                .putNumber("result", 2)
//                                                                .putString("description", "Avatar empty")
//                                                );
//                                            }
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                }
                            } else {
                                responseAvatarRequest(request.response(), new JsonObject()
                                                .putNumber("result", 2)
                                                .putString("description", "Avatar empty")
                                        , defaultAvatar
                                );
                            }
                        }
                    });

                } else if (request.path().startsWith("/service/avatar/set")) {
                    request.expectMultiPart(true);

                    final String requestToken = request.params().get("token");
                    if (requestToken == null) {
                        responseJson(request.response(), new JsonObject()
                                        .putNumber("result", 1)
                                        .putString("description", "Missing parameters: token.")
                        );
                        return;
                    }

                    final String phoneNumber = tokens.get(requestToken);

                    if (phoneNumber == null) {
                        responseJson(request.response(), new JsonObject()
                                        .putNumber("result", 3)
                                        .putString("description", "Token doesn't exist!")
                        );
                        return;
                    }

                    // request EndHandler uses this to determine if it would download image by the given url in the case both file and URL was uploaded.
                    final SetAvatarTypeInfo setAvatarTypeInfo = new SetAvatarTypeInfo();

                    request.endHandler(new org.vertx.java.core.Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            if (setAvatarTypeInfo.setAvatarType == SetAvatarType.FROM_URL) {
                                String imageUrl = request.formAttributes().get("imageUrl");
                                if (imageUrl == null) {
                                    // no file & no imageUrl
                                    responseJson(request.response(), new JsonObject()
                                                    .putNumber("result", 8)
                                                    .putString("description", "Invalid value.")
                                    );
                                    tokens.remove(requestToken);
                                    return;
                                }
                                imageUrl = imageUrl.trim();

                                downloadImage(imageUrl, avatarDir + phoneNumber, new org.vertx.java.core.Handler<Integer>() {

                                    @Override
                                    public void handle(Integer error) {
                                        if (error == 0) {
                                            scaleAndConvertImage(avatarDir + phoneNumber, new org.vertx.java.core.Handler<Integer>() {
                                                @Override
                                                public void handle(Integer error) {
                                                    if (error == 0) {
                                                        responseJson(request.response(), new JsonObject()
                                                                        .putNumber("result", 0)
                                                                        .putString("description", "Avatar uploaded successfully!")
                                                        );
                                                    } else {
                                                        vertx.fileSystem().delete(avatarDir + phoneNumber, new org.vertx.java.core.Handler<AsyncResult<Void>>() {
                                                            @Override
                                                            public void handle(AsyncResult<Void> event) {
                                                                // can't convert file delete it
                                                            }
                                                        });
                                                        responseJson(request.response(), new JsonObject()
                                                                        .putNumber("result", 7)
                                                                        .putString("description", "File type is not supported!")
                                                        );
                                                    }
                                                    avatarUpdatingPhones.remove(phoneNumber);
                                                }
                                            });
                                            return;
                                        }
                                        responseJson(request.response(), new JsonObject()
                                                        .putNumber("result", 9)
                                                        .putString("description", "Can't download the file: ")
                                        );
                                        tokens.remove(requestToken);
                                    }
                                });
                            }
                        }
                    });

                    request.uploadHandler(new org.vertx.java.core.Handler<HttpServerFileUpload>() {
                        @Override
                        public void handle(final HttpServerFileUpload file) {
                            file.endHandler(new org.vertx.java.core.Handler<Void>() {
                                @Override
                                public void handle(Void voidEvent) {
                                    if (file.size() == 0 && file.filename().trim().length() == 0) {
                                        setAvatarTypeInfo.setAvatarType = SetAvatarType.FROM_URL;
                                        return;
                                    }
                                    setAvatarTypeInfo.setAvatarType = SetAvatarType.FILE_UPLOAD;
                                    if(logger!= null){
                                        logger.debug("Coming file size: " + file.size());
                                    }
                                    scaleAndConvertImage(avatarDir + phoneNumber, new org.vertx.java.core.Handler<Integer>() {
                                        @Override
                                        public void handle(Integer error) {
                                            if (error == 0) {
                                                responseJson(request.response(), new JsonObject()
                                                                .putNumber("result", 0)
                                                                .putString("description", "Avatar uploaded successfully!")
                                                );
                                            } else {
                                                vertx.fileSystem().delete(avatarDir + phoneNumber, new org.vertx.java.core.Handler<AsyncResult<Void>>() {
                                                    @Override
                                                    public void handle(AsyncResult<Void> event) {
                                                        // can't convert file delete it
                                                    }
                                                });
                                                responseJson(request.response(), new JsonObject()
                                                                .putNumber("result", 7)
                                                                .putString("description", "File type is not supported!")
                                                );
                                            }
                                            avatarUpdatingPhones.remove(phoneNumber);
                                        }
                                    });
                                }
                            });
                            file.exceptionHandler(new org.vertx.java.core.Handler<Throwable>() {
                                @Override
                                public void handle(Throwable e) {
                                    e.printStackTrace();
                                }
                            });
                            if (file.size() > MAX_FILE_SIZE) {
                                responseJson(request.response(), new JsonObject()
                                                .putNumber("result", 4)
                                                .putString("description", "File size is too big!")
                                );
                                return;
                            }
                            if(logger != null){
                                logger.debug("Comming file size: " + file.size());
                            }
                            file.streamToFileSystem(avatarDir + phoneNumber);
                        }
                    });
                } else  if (request.path().startsWith("/service/static/")) {
                    final String filePath = staticResourceDir + request.path().replace("/service/static/", "");

                    vertx.fileSystem().exists(filePath, new org.vertx.java.core.Handler<AsyncResult<Boolean>>() {
                        @Override
                        public void handle(AsyncResult<Boolean> isFileExist) {
                            if (isFileExist.result()) {

                                request.response().sendFile(filePath, new org.vertx.java.core.Handler<AsyncResult<Void>>() {
                                    @Override
                                    public void handle(AsyncResult<Void> event) {
                                        if (event.failed()) {
//                                            request.response().end();
//                                            container.logger().warn("Send avatar image fail.", event.cause());
//                                            if (event.cause() instanceof FileNotFoundException) {
//                                                responseJson(request.response(), new JsonObject()
//                                                                .putNumber("result", 2)
//                                                                .putString("description", "Avatar empty")
//                                                );
//                                            }
                                        }
                                    }
                                });
                            } else {
                                request.response().end("Resource not found!");
                            }
                        }
                    });

                } else {
                    responseJson(request.response(), new JsonObject()
                                    .putNumber("result", 5)
                                    .putString("description", "Resource not found!")
                    );
                }

            }
        });

        server.listen(port, host, new org.vertx.java.core.Handler<AsyncResult<HttpServer>>() {
            @Override
            public void handle(AsyncResult<HttpServer> event) {
                if (event.failed()) {
                    event.cause().printStackTrace();
                }
            }
        });

        vertx.eventBus().registerLocalHandler(CMD_GET_TOKEN, new org.vertx.java.core.Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject cmd = event.body();
                if (cmd == null)
                    return;

                final String phoneNumber = cmd.getString("phoneNumber");

                if (phoneNumber == null) {
                    event.reply(new JsonObject().putString("exception", "Missing parameter: phoneNumber"));
                    return;
                }

                final String uniqueId = RandomStringUtils.randomAlphanumeric(50);

                if(logger!= null){
                    logger.info(String.format("New upload avatar token: %s %s", phoneNumber, uniqueId));
                }

                if(isAvatarStorage) {
                    tokens.put(uniqueId, phoneNumber);
                    avatarUpdatingPhones.add(phoneNumber);

                    vertx.setTimer(DEFAULT_TOKEN_TIMEOUT, new org.vertx.java.core.Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            tokens.remove(uniqueId);
                            avatarUpdatingPhones.remove(phoneNumber);
                        }
                    });
                } else {
                    JsonObject json = new JsonObject();
                    String phone = phoneNumber;
                    if(!phone .startsWith("0"))
                        phone = "0" + phone;
                    json.putString("phone", phone);
                    json.putString("token", uniqueId);
                    vertx.eventBus().publish(PUSH_NEW_TOKEN_ADDRESS, json);
                }

                event.reply(new JsonObject().putString("token", uniqueId));
            }
        });
    }

    private void downloadImage(String imageUrl, String fileOut, final org.vertx.java.core.Handler<Integer> callback) {
        JsonObject requestBody = new JsonObject();
        requestBody.putString("cmd", HttpFileDownloadVerticle.CMD_DOWNLOAD_IMAGE);
        requestBody.putString("url", imageUrl);
        requestBody.putString("fileOut", fileOut);

        vertx.eventBus().send(AppConstant.HttpFileDownloadVerticle_ADDRESS, requestBody, new org.vertx.java.core.Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> msg) {
                int error = msg.body().getInteger("error", Integer.MIN_VALUE);

                switch (error) {
                    case 0:
                        callback.handle(0);
                        break;
                    case 1:
                        callback.handle(4);
                        break;
                    case 2:
                    case 3:
                        callback.handle(7);
                        break;
                    default:
                        callback.handle(7);
                        if(logger != null){
                            logger.error("Download file result:" + msg.body());
                        }
                        break;
                }
            }
        });
    }

    private void scaleAndConvertImage(String imageFile, final org.vertx.java.core.Handler<Integer> handler) {
        JsonObject cmdBody = new JsonObject();
        cmdBody.putString("cmd", "scale");
        cmdBody.putString("fileIn", imageFile);

        vertx.eventBus().send(AppConstant.ImageProcessVerticle_ADDRESS, cmdBody, new org.vertx.java.core.Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getInteger("error"));
            }
        });
    }


    public static void responseAvatarRequest(HttpServerResponse response, JsonObject obj, boolean defaultAvatar) {
        if (defaultAvatar) {
            response.sendFile(DEFAULT_AVATAR_IMAGE);
            return;
        }
        response.putHeader("Content-Type", "application/json");
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.end(obj.toString());
    }

    public static void responseJson(HttpServerResponse response, JsonObject obj) {
        response.putHeader("Content-Type", "application/json");
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.end(obj.toString());
    }

    enum SetAvatarType {
        UNDETERMINED,
        FROM_URL,
        FILE_UPLOAD
    }

    class SetAvatarTypeInfo {
        SetAvatarType setAvatarType = SetAvatarType.UNDETERMINED;

    }
}
