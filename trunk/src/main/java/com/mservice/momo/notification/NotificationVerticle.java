package com.mservice.momo.notification;

import com.mservice.common.BankInfo;
import com.mservice.common.Popup;
import com.mservice.common.PushInfo;
import com.mservice.common.TransactionHistory;
import com.mservice.momo.data.*;
import com.mservice.momo.data.ironmanpromote.IronManPromoGiftDB;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.referral.ReferralV1CodeInputDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StatisticUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.ironmanpromo.IronManPromoObj;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import com.mservice.momo.vertx.redis.NotificationRedisVerticle;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.datagram.DatagramPacket;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.datagram.InternetProtocolFamily;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import javax.xml.bind.DatatypeConverter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by nam on 5/17/14.
 */
public class NotificationVerticle extends Verticle {
//    public static final String ADDRESS_SEND_NOTIFICATION = "notificationVerticle.sendNotification";
//    public static final String ADDRESS_SYNC_NOTIFICATION = "notificationVerticle.syncNotification";
//    public static final String ADDRESS_SEND_PACKET_FAIL = "notificationVerticle.failSendingNotification";
//    public static final String ADDRESS_SEND_PACKET_SUCCESS = "notificationVerticle.successSendingNotification";


    private NotificationDb notificationDb;

    private Set<String> sendingSet;

    private TransDb transDb;

    private PhonesDb phonesDb;

    private MappingWalletBankDb mappingWalletBankDb;
    private MappingWalletOCBPromoDb mappingWalletOCBPromoDb;
    private JsonObject jsonOcbPromo;
    private BillPayPromosDb billPayPromosDb;
    private VcbCmndRecs vcbCmndRecs;
    private IronManPromoGiftDB ironManPromoGiftDB;
    private JsonObject jsonIronManPromo;
    private boolean isActiveOCBPromo = false;
    private PushNotificationInfoManagerDb pushNotificationInfoManagerDb;
    private PromotionProcess promotionProcess;
    private ErrorPromotionTrackingDb errorPromotionTrackingDb;
    private ReferralV1CodeInputDb referralV1CodeInputDb;
    private ConnectorHTTPPostPathDb connectorHTTPPostPathDb;
    private HttpClient client;
    private NotificationToolDb notificationToolDb;
    private NotificationMySQLDb notificationMySQLDb;
    private JsonObject glbConfig;
    public static boolean isJsonArray(String s) {
        JsonArray resJson = null;
        try {
            resJson = new JsonArray(s);
        } catch (Exception e) {
        }
        return resJson != null;
    }

    public void findOld(final long end, final long start, final ArrayList<Long> list, final AtomicLong count) {
        System.out.println("Get noti old of <" + list.get((int) count.get()) + "> COUNT=" + count.get());
        final Long phone = list.get((int) count.get());
        final String colName = "noti_" + phone;
        notificationDb.findNotiOld(start, end, colName, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                count.getAndIncrement();
                if (jsonArray != null && jsonArray.size() > 0) {
                    notificationMySQLDb.saveNotifications(phone, jsonArray, new Handler<Integer>() {
                        @Override
                        public void handle(Integer integer) {
                            findOld(end, start, list, count);
                        }
                    });
                    /*final String fileName = colName + "_" + end + "_" + start + ".json";
                    notificationDb.writeJsonFile(fileName, jsonArray.toString(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            System.out.println("Write " + fileName + " is " + aBoolean);
                            if (aBoolean) {
                              notificationDb.deleteNotiOld(start, colName, new Handler<JsonArray>() {
                                    @Override
                                    public void handle(JsonArray jsonArray) {
                                        findOld(end, start, list, count);
                                    }
                                });
                            }
                        }
                    });*/
                } else {
                    findOld(end, start, list, count);
                }
            }
        });
    }

    /*public void importFileToMySQL(final List<File> files, final AtomicInteger count, final Handler<Boolean> callback) {
        if (count.get() >= files.size()) {
            callback.handle(true);
            return;
        }
        final File f = files.get(count.getAndIncrement());
        try {
            System.out.println("***file: " + f.getCanonicalPath());
            String[] fileNameParts = f.getName().split("_");
            if (fileNameParts == null || fileNameParts.length == 0 || StringUtils.isEmpty(fileNameParts[1])) {
                System.out.println("\tfile invalid " + f.getCanonicalPath());
                importFileToMySQL(files, count, callback);
                return;
            }
            FileInputStream fis = null;
            String raw = "";
            try {
                fis = new FileInputStream(f);
                byte[] data = new byte[(int) f.length()];
                fis.read(data);
                raw = new String(data, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            boolean isValid = true;
            if (!StringUtils.isEmpty(raw) && isJsonArray(raw)) {
                JsonArray jsonArray = new JsonArray(raw);
                for (Object o : jsonArray) {
                    JsonObject jsonObject = (JsonObject) o;
                    jsonObject.putNumber("sender", Integer.parseInt(fileNameParts[1]));
                    if (StringUtils.isEmpty(jsonObject.getString("_id"))) {
                        isValid = false;
                        break;
                    }
                    if (!StringUtils.isEmpty(jsonObject.getString("sms", ""))) {
                        System.out.println("sms: " + jsonObject.getString("sms", ""));
                    }
                }
                if (!isValid) {
                    System.out.println("\t_id invalid " + raw);
                    importFileToMySQL(files, count, callback);
                    return;
                } else {
                    System.out.println("\trecords size " + jsonArray.size());
                    notificationDb.saveNotifications(Integer.parseInt(fileNameParts[1]), jsonArray, new Handler<Integer>() {
                        @Override
                        public void handle(Integer integer) {
                            System.out.println("\tRESULT " + integer);
                            if (integer > 0) {
                                try {
                                    Path fromFile = Paths.get(f.toURI());
                                    Path toFile = Paths.get("/home/duyhuynh/backup_noti_done", f.getName());
                                    Files.move(
                                            fromFile,
                                            toFile,
                                            REPLACE_EXISTING);
                                    System.out.println("\tMove from " + fromFile + " to " + toFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            importFileToMySQL(files, count, callback);
                        }
                    });
                }
            } else {
                isValid = false;
                System.out.println("\tfile content is empty or invalid " + raw);
                importFileToMySQL(files, count, callback);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            importFileToMySQL(files, count, callback);
            return;
        } finally {
        }
    }*/

    private void init() {
        notificationDb = DBFactory.createNotiDb(vertx, container.logger(), container.config());
        notificationToolDb = new NotificationToolDb(vertx, container.logger());
        notificationMySQLDb = new NotificationMySQLDb(vertx, container.logger(), container.config());
        sendingSet = new HashSet<>();
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), container.logger(), container.config());
        phonesDb = new PhonesDb(vertx.eventBus(), container.logger());
        mappingWalletBankDb = new MappingWalletBankDb(vertx, container.logger());
        billPayPromosDb = new BillPayPromosDb(vertx, container.logger());
        vcbCmndRecs = new VcbCmndRecs(vertx.eventBus(), container.logger());
        ironManPromoGiftDB = new IronManPromoGiftDB(vertx, container.logger());
        pushNotificationInfoManagerDb = new PushNotificationInfoManagerDb(vertx, container.logger());
        promotionProcess = new PromotionProcess(vertx, container.logger(), container.config());
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, container.logger());
        referralV1CodeInputDb = new ReferralV1CodeInputDb(vertx, container.logger());
        connectorHTTPPostPathDb = new ConnectorHTTPPostPathDb(vertx);
        JsonObject config = container.config().getObject("promotion_server", new JsonObject());
        String host = config.getString("host", "0.0.0.0");
        int port = config.getInteger("port", 8449);
        client = vertx.createHttpClient().setHost(host).setPort(port);

        //Create Listen Local

        final DatagramSocket socket = vertx.createDatagramSocket(InternetProtocolFamily.IPv4);
        socket.listen(AppConstant.HOST_SERVER, AppConstant.PORT_SERVER, new AsyncResultHandler<DatagramSocket>() {
            public void handle(AsyncResult<DatagramSocket> asyncResult) {
                if (asyncResult.succeeded()) {
                    socket.dataHandler(new Handler<DatagramPacket>() {
                        public void handle(DatagramPacket packet) {
                            // Do something with the packet
                            container.logger().info("get packet " + packet.data());
                            if (Misc.isValidJsonObject(packet.data().toString())) {
                                JsonObject jsonObject = new JsonObject(packet.data().toString());
                                JsonObject joData = jsonObject.getObject(StringConstUtil.BroadcastField.DATA, new JsonObject());
                                String type = jsonObject.getString(StringConstUtil.BroadcastField.TYPE, "");
                                container.logger().info("noti content " + joData.toString());

                                if (type.equalsIgnoreCase(StringConstUtil.BroadcastField.UPDATE_LIST_CACHE)) {
                                    container.logger().info("UPDATE_LIST_CACHE " + AppConstant.hostServer);
                                    vertx.eventBus().send(AppConstant.CloundNotifyVerticleUpdate, joData);
                                    return;
                                } else if (type.equalsIgnoreCase(StringConstUtil.BroadcastField.NOTI_FROM_TOOL)) {
                                    final Notification notification = Notification.parse(joData);
                                    container.logger().info("number receiver noti broad " + notification.receiverNumber);
                                    BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                                    msg.setType(SoapProto.Broadcast.MsgType.NOTIFICATION_VALUE);
                                    msg.setNewPhone(notification.receiverNumber);
                                    msg.setExtra(notification.toJsonObject());
                                    vertx.eventBus().publish(Misc.getNumberBus(notification.receiverNumber), msg.getJsonObject());
                                    return;
                                } else if (type.equalsIgnoreCase(StringConstUtil.BroadcastField.NOTI_FROM_TOOL_WITH_REDIS)) {
                                    final Notification notification = Notification.parse(joData);
                                    container.logger().info("number receiver noti broad " + notification.receiverNumber);
                                    JsonObject joGetNumber = new JsonObject();
                                    joGetNumber.putString(StringConstUtil.BroadcastField.COMMAND, NotificationRedisVerticle.GET_NOTI_VIA_NUMBER);
                                    vertx.eventBus().sendWithTimeout(AppConstant.RedisVerticle_NOTIFICATION_FROM_TOOL, joGetNumber, 800000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                                        @Override
                                        public void handle(AsyncResult<Message<JsonObject>> msgResponseFromRedis) {
                                            container.logger().info(msgResponseFromRedis.cause());
                                            if (msgResponseFromRedis.failed()) {
                                                container.logger().info("SEND REDIS FROM TOOL FAIL");
                                                return;
                                            }
                                            JsonObject joResponseFromRedis = msgResponseFromRedis.result().body();
                                            final JsonArray jarrData = joResponseFromRedis.getArray(StringConstUtil.DATA, new JsonArray());
                                            final AtomicInteger countNoti = new AtomicInteger(jarrData.size());
                                            container.logger().info("countNoti " + countNoti);
                                            vertx.setPeriodic(100L, new Handler<Long>() {
                                                @Override
                                                public void handle(Long timerSendNoti) {
                                                    int positionNoti = countNoti.decrementAndGet();
                                                    if (positionNoti < 0) {
                                                        vertx.cancelTimer(timerSendNoti);
                                                        return;
                                                    }

                                                    JsonObject joDataNoti = jarrData.get(positionNoti);
                                                    int number = DataUtil.strToInt(joDataNoti.getString(StringConstUtil.NUMBER, "0"));
                                                    JsonObject joNoti = joDataNoti.getObject(StringConstUtil.NOTIFICATION_OBJ, null);
                                                    if (joNoti != null) {
                                                        container.logger().info("joNoti from noti tool + position " + positionNoti + " content " + joNoti);
                                                        BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                                                        msg.setType(SoapProto.Broadcast.MsgType.NOTIFICATION_VALUE);
                                                        msg.setNewPhone(number);
                                                        msg.setExtra(joNoti);
                                                        vertx.eventBus().publish(Misc.getNumberBus(number), msg.getJsonObject());
                                                    }
                                                    else {
                                                        container.logger().info("ERROR DATA NOTI FROM TOOL");
                                                    }
                                                }
                                            });
                                        }
                                    });
                                    return;
                                } else {
                                    final Notification notification = Notification.parse(joData);
                                    container.logger().info("number receiver noti broad " + notification.receiverNumber);
                                    BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                                    msg.setType(SoapProto.Broadcast.MsgType.NOTIFICATION_VALUE);
                                    msg.setNewPhone(notification.receiverNumber);
                                    msg.setExtra(notification.toJsonObject());
                                    vertx.eventBus().publish(Misc.getNumberBus(notification.receiverNumber), msg.getJsonObject());
                                    sendingSet.add(notification.id);
                                    vertx.setTimer(30000L, new Handler<Long>() {
                                        @Override
                                        public void handle(Long timerId) {
                                            if (sendingSet.contains(notification.id)) {
                                                //sent fail;
                                                container.logger().debug(notification.receiverNumber + " timer: sent notification via socket fail.");
                                                sendingNotificationViaSocketFail(notification.receiverNumber, notification);
                                                return;
                                            }
                                            // sent successfully;
                                            container.logger().debug(notification.receiverNumber + " timer: sent notification via socket successfully.");
                                            sendingNotificationViaSocketSuccess(notification.receiverNumber, notification);
                                        }
                                    });
                                    return;
                                }

                            } else {
                                container.logger().info("packet is not json");
                            }
                        }
                    });
                } else {
                    container.logger().warn("Listen failed" + asyncResult.cause());
                }
            }
        });
        final long beginTime = 1456938000000L; //3/3
        final long endTime = 1466787600000L; // 25/06
        /*phonesDb.getAllPhone(new Handler<ArrayList<Integer>>() {
            @Override
            public void handle(ArrayList<Integer> list) {
                findOld(endTime, beginTime, list, new AtomicInteger(0));
            }
        });*/

        /*ArrayList<Long> phones = new ArrayList<>();
        try {
            System.out.println("=============================================START");
            File f = new File("/home/duyhuynh/Desktop/phones.csv");
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    try {
                        phones.add((long)Double.parseDouble(line));
                    } catch (Exception e) {
                        System.out.println(line);
                    }
                }
            }
            findOld(endTime, beginTime, phones, new AtomicLong(811310));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }*/
        /*try {
            System.out.println("=============================================START");
            File dir = new File("/home/duyhuynh/backup_noti");

            System.out.println("Getting all files in " + dir.getCanonicalPath() + " including those in subdirectories");
            List<File> files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            AtomicInteger count = new AtomicInteger(0);
            importFileToMySQL(files, count, new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    System.out.println("=============================================END");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }*/


//        vertx.setPeriodic(10000, new Handler<Long>() {
//            @Override
//            public void handle(Long aLong) {
//
//                BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
//                helper.setType(SoapProto.Broadcast.MsgType.NEW_USER_VALUE);
//                helper.setSenderNumber(987568815);
//                helper.setNewPhone(987568815);
//                vertx.eventBus().publish(ServerVerticle.MOMO_BROADCAST
//                        ,helper.getJsonObject());
//            }
//        });
        /*vertx.setTimer(4000, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                notificationDb.find(987568815, "8cb0bec1-d65f-49ef-9ca5-c484bda7c19e", new Handler<Notification>() {
                    @Override
                    public void handle(Notification notification) {
                        System.out.println("#$###################");
                        System.out.println(notification.toJsonObject());
                        System.out.println(notification);
                    }
                });
            }
        });*/

//        Notification notification = Notification.parse(new JsonObject("{\"priority\":1,\"type\":1,\"caption\":\"Nhận tiền thành công!\",\"body\":\"Quý khách đã nhận thành công số tiền 54.453đ từ NAM COI(0987568815). Nội dung: \\\"abc\\\".\",\"sms\":\"Chuc mung quy khach da nhan duoc 54.453d tu NAM COI(0987568815) luc 10:43 05/08/2014. TID: 33885065. Xin cam on.\",\"tranId\":33885065,\"time\":1407210207313,\"status\":0,\"sender\":0,\"receiverNumber\":1665530139}"));

//        cloudMessageQueue.push(notification, null);

//        vertx.setPeriodic(500, new Handler<Long>() {
//            @Override
//            public void handle(Long aLong) {
//                cloudMessageQueue.peek(new Handler<Notification>() {
//                    @Override
//                    public void handle(Notification notification) {
//                        if (notification == null) {
//                            container.logger().info("##QUEUE EMPTY ###");
//                        } else{
//                            container.logger().info(notification.toFullJsonObject());
//                            cloudMessageQueue.remove(notification, null);
//                        }
//
//                    }
//                });
//            }
//        });


//        sendingSet = vertx.sharedData().getSet("NotificationVerticle.sendingSet");

//        ArrayList<Notification> notis = new ArrayList<>();
//        Notification n = new Notification();
//        n.id = "224c364d-da40-495c-8a3d-a3675acd7d59";
//        n.status = 3;
//        notis.add(n);
//
//        notificationDb.updateNotifications(987568815, notis, new Handler<List<Notification>>() {
//            @Override
//            public void handle(List<Notification> event) {
//                    System.out.println(event);
//            }
//        });
//
//        notificationDb.count(987568815, 0L, Notification.getAllStatusWithoutDeleted(), new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                System.out.println(event);
//            }
//        });
//        notificationDb.getAllNotificationPage(1263081153, 5, 1, 0L, Notification.getAllStatusWithoutDeleted(), new Handler<List<Notification>>() {
//            @Override
//            public void handle(List<Notification> event) {
//                System.out.println(event.size());
//            }
//        });
//
//    -    notificationDb.count(979754034, 1400560589618L, Notification.getAllStatusWithoutDeleted(), new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                System.out.println(event);
//            }
//        });

    }

    @Override
    public void start() {
        init();
        JsonObject globalConfig = container.config();
        jsonOcbPromo = globalConfig.getObject(StringConstUtil.OBCPromo.JSON_OBJECT, new JsonObject());
        isActiveOCBPromo = jsonOcbPromo.getBoolean(StringConstUtil.OBCPromo.IS_ACTIVE, false);
        final JsonObject config = globalConfig.getObject("notificationVerticle", new JsonObject());
        final long defaultResponseTimeOut = config.getLong("defaultResponseTimeOut", 30000);
        jsonIronManPromo = globalConfig.getObject(StringConstUtil.IronManPromo.JSON_OBJECT, new JsonObject());
        mappingWalletOCBPromoDb = new MappingWalletOCBPromoDb(vertx, container.logger());
        container.logger().info("Start notification verticle on " + AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION);
//        vertx.setTimer(2000, new Handler<Long>() {
//            @Override
//            public void handle(Long event) {
//                Notification d = Notification.parse(NotificationUtils.createRequestMoneyNotification(3,987568815, 979754034,"Nhung", 10000L, "alo"));
//                d.priority= 1;
//                vertx.eventBus().send(NotificationVerticle.ADDRESS_SEND_NOTIFICATION, d.toFullJsonObject());
//            }
//        });

        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> notiGenRequest) {


                final Notification notification = Notification.parse(notiGenRequest.body());
//              container.logger().info("Got notification :" + notification);

                if (notification == null) {
//                  container.logger().error("Can't build notification from: " + notiGenRequest.body());
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("description", "Can't build notification object.")
                    );
                    return;
                }
                container.logger().info("Generated Notification:" + notification.toJsonObject());

                final int phoneNumber = notification.receiverNumber;
                if (phoneNumber == 0) {
                    container.logger().error("Request send notification doesn't contain phone field.");
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 2)
                                    .putString("description", "Missing parameter: phoneNumber")
                    );
                    return;
                }
                //BEGIN Ma hoa noti
                Notification encryptedNotification = new Notification(notification);
                encryptedNotification.body = DatatypeConverter.printBase64Binary(encryptedNotification.body.getBytes());

                //END ma hoa Noti
                notificationDb.saveNotification(phoneNumber, encryptedNotification.toJsonObject(), new Handler<String>() {
                    //                notificationDb.save(notification, new Handler<String>() {
                    @Override
                    public void handle(String savedId) {
                        if (savedId == null) {
                            //todo: Can't persist the Notification.
                            container.logger().error("Can't persist the Notification:" + notification.toJsonObject());
                            notiGenRequest.reply(
                                    new JsonObject()
                                            .putNumber("error", 3)
                                            .putString("description", "Can't persist the notification")
                            );
                            return;
                        }
                        notification.id = savedId;
                        //Todo: notification has persisted. Send it back to phone.

                        if (notification.priority < 10) {

                            BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                            msg.setType(SoapProto.Broadcast.MsgType.NOTIFICATION_VALUE);
                            msg.setNewPhone(phoneNumber);
                            msg.setExtra(notification.toJsonObject());

                            vertx.eventBus().publish(Misc.getNumberBus(phoneNumber), msg.getJsonObject());

                            sendingSet.add(notification.id);
                            vertx.setTimer(defaultResponseTimeOut, new Handler<Long>() {
                                @Override
                                public void handle(Long timerId) {
                                    if (sendingSet.contains(notification.id)) {
                                        //sent fail;
                                        container.logger().debug(phoneNumber + " timer: sent notification via socket fail.");
                                        sendingNotificationViaSocketFail(phoneNumber, notification);
                                        return;
                                    }
                                    // sent successfully;
                                    container.logger().debug(phoneNumber + " timer: sent notification via socket successfully.");
                                    sendingNotificationViaSocketSuccess(phoneNumber, notification);
                                }
                            });
                        } else {
                            sendingSet.add(notification.id);
                            sendingNotificationViaSocketFail(phoneNumber, notification);
                        }
                    }
                });
//            }// else
            }
        });

        /*
        This method used to send noti from tool on webadmin
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> notiGenRequest) {


                final Notification notification = Notification.parse(notiGenRequest.body());
//              container.logger().info("Got notification :" + notification);

                if (notification == null) {
//                  container.logger().error("Can't build notification from: " + notiGenRequest.body());
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("description", "Can't build notification object.")
                    );
                    return;
                }
                container.logger().info("Generated Notification NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL:" + notification.toJsonObject());

                final int phoneNumber = notification.receiverNumber;
                if (phoneNumber == 0) {
                    container.logger().error("Request send notification doesn't contain phone field.");
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 2)
                                    .putString("description", "Missing parameter: phoneNumber")
                    );
                    return;
                }
                //BEGIN Ma hoa noti
//                Notification encryptedNotification = new Notification(notification);
//                encryptedNotification.body = DatatypeConverter.printBase64Binary(encryptedNotification.body.getBytes());
                String id = UUID.randomUUID().toString();
                notification.id = id;
                //END ma hoa Noti
                //Todo: notification has persisted. Send it back to phone.
                broadcashUdp(notification.toFullJsonObject(), 0, "0" + notification.receiverNumber, StringConstUtil.BroadcastField.NOTI_FROM_TOOL, container.logger(), false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
                    }
                });
            }
        });

        /*
        This method used to send noti from tool on webadmin
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL_WITH_REDIS, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> notiGenRequest) {


                final Notification notification = Notification.parse(notiGenRequest.body());
                    container.logger().info("Got NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL_WITH_REDIS :" + notification.toFullJsonObject());

                if (notification == null) {
//                  container.logger().error("Can't build notification from: " + notiGenRequest.body());
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("description", "Can't build notification object.")
                    );
                    return;
                }
                container.logger().info("Generated Notification NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL:" + notification.toJsonObject());

                final int phoneNumber = notification.receiverNumber;
                container.logger().info("phone number NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL: " + phoneNumber);
                if (phoneNumber == 0) {
                    container.logger().error("Request send notification doesn't contain phone field.");
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 2)
                                    .putString("description", "Missing parameter: phoneNumber")
                    );
                    return;
                }
                //BEGIN Ma hoa noti
                String id = UUID.randomUUID().toString();
                notification.id = id;
                //END ma hoa Noti
                //todo save noti into redis.
                JsonObject joData = new JsonObject().putString(StringConstUtil.BroadcastField.COMMAND, NotificationRedisVerticle.INSERT_NOTI).putNumber(StringConstUtil.NUMBER, phoneNumber).putObject(StringConstUtil.NOTIFICATION_OBJ, notification.toFullJsonObject());
                container.logger().info("data beforse send RedisVerticle_NOTIFICATION_FROM_TOOL " + joData.toString());
                vertx.eventBus().send(AppConstant.RedisVerticle_NOTIFICATION_FROM_TOOL, joData, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> messReply) {

                    }
                });

            }
        });

        /*
        This method used to send popup noti
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> notiGenRequest) {


                final Notification notification = Notification.parse(notiGenRequest.body());
//              container.logger().info("Got notification :" + notification);

                if (notification == null) {
//                  container.logger().error("Can't build notification from: " + notiGenRequest.body());
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 1)
                                    .putString("description", "Can't build notification object.")
                    );
                    return;
                }
                container.logger().info("Generated Notification NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION:" + notification.toJsonObject());

                final int phoneNumber = notification.receiverNumber;
                if (phoneNumber == 0) {
                    container.logger().error("Request send notification doesn't contain phone field. NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION");
                    notiGenRequest.reply(
                            new JsonObject()
                                    .putNumber("error", 2)
                                    .putString("description", "Missing parameter: phoneNumber")
                    );
                    return;
                }
                //BEGIN Ma hoa noti
                Notification encryptedNotification = new Notification(notification);
                encryptedNotification.body = DatatypeConverter.printBase64Binary(encryptedNotification.body.getBytes());

                notification.id = UUID.randomUUID().toString();
                //Todo: notification has persisted. Send it back to phone.

                if (notification.priority < 10) {

                    BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                    msg.setType(SoapProto.Broadcast.MsgType.NOTIFICATION_VALUE);
                    msg.setNewPhone(phoneNumber);
                    msg.setExtra(notification.toJsonObject());

                    vertx.eventBus().publish(Misc.getNumberBus(phoneNumber), msg.getJsonObject());

                    sendingSet.add(notification.id);
                    vertx.setTimer(defaultResponseTimeOut, new Handler<Long>() {
                        @Override
                        public void handle(Long timerId) {
                            if (sendingSet.contains(notification.id)) {
                                //sent fail;
                                container.logger().debug(phoneNumber + " timer: NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION sent notification via socket fail.");
                                sendingNotificationViaSocketFail(phoneNumber, notification);
                                return;
                            }
                            // sent successfully;
                            container.logger().debug(phoneNumber + " timer: NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION sent notification via socket successfully.");
                            sendingNotificationViaSocketSuccess(phoneNumber, notification);
                        }
                    });
                } else {
                    container.logger().debug(phoneNumber + " timer: NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION sent notification via socket fail. prio > 10");
                    sendingSet.add(notification.id);
                    sendingNotificationViaSocketFail(phoneNumber, notification);
                }
//                    }
//                });
//            }// else
            }
        });

        //PUBLISH
        vertx.eventBus().registerHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_PACKET_FAIL, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper(message.body());

                Notification notification = Notification.parse(helper.getExtra());
                if (notification == null) {
                    container.logger().error("Can't parse a Notification message received over ADDRESS_SEND_PACKET_FAIL: " + helper.getExtra());
                    message.reply(message.body());
                    return;
                }

                int phoneNumber = helper.getNewPhone();
                if (phoneNumber == 0) {
                    container.logger().error("ADDRESS_SEND_PACKET_FAIL: Missing phoneNumber" + helper);
                    message.reply(message.body());
                    return;
                }

                sendingNotificationViaSocketFail(phoneNumber, notification);
                message.reply(message.body());
            }
        });

        //PUBLISH
        vertx.eventBus().registerHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_PACKET_SUCCESS, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper(message.body());

                Notification notification = Notification.parse(helper.getExtra());
                if (notification == null) {
                    container.logger().error("Can't parse a Notification message received over ADDRESS_SEND_PACKET_SUCCESS: " + helper.getExtra());
                    message.reply(message.body());
                    return;
                }

                int phoneNumber = helper.getNewPhone();
                if (phoneNumber == 0) {
                    container.logger().error("ADDRESS_SEND_PACKET_SUCCESS: Missing phoneNumber" + helper);
                    message.reply(message.body());
                    return;
                }

                sendingNotificationViaSocketSuccess(phoneNumber, notification);
                message.reply(message.body());
            }
        });

        //Dung ban noti tu dong tu coreVerticle
        //BEGIN 0000000029
        vertx.eventBus().registerHandler(StringConstUtil.CONNECTOR_NOTI_VERTICLE, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final Logger logger = container.logger();
                logger.info("Ban noti tu dong tu core");
                sendConnectorNotification(message, logger, null, null);
            }
        });

        /*
        This method used to send noti visa broadcast to other server
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_VISA_ADDRESS_SEND_NOTIFICATION, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> mesNotiRequest) {


                final Notification notification = Notification.parse(mesNotiRequest.body());
                container.logger().info("Generated Notification:" + notification.toJsonObject());
                container.logger().info("Generated Notification: NotificationVerticle_VISA_ADDRESS_SEND_NOTIFICATION broadcashUdp " + notification.toJsonObject());
                broadcashUdp(notification.toFullJsonObject(), 0, "", "", container.logger(), false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {
                        mesNotiRequest.reply(result);
                    }
                });

            }
        });

         /*
        This method used to send broadcast to update list cache
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_CloudNotifyVerticleUpdate_UpdateListcache, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> msgRequest) {


                JsonObject joData = msgRequest.body();
                container.logger().info("Generated Notification: NotificationVerticle_CloudNotifyVerticleUpdate_UpdateListcache broadcashUdp " + joData);
                broadcashUdp(joData, 0, "", StringConstUtil.BroadcastField.UPDATE_LIST_CACHE, container.logger(), false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {

                    }
                });

            }
        });

        /*
        This method used to send noti via cloud
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_VIA_CLOUD, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> mesNotiRequest) {
                final Notification notification = Notification.parse(mesNotiRequest.body());
                container.logger().info("Generated Notification:" + notification.toJsonObject());
                container.logger().info("Generated Notification: NotificationVerticle_ADDRESS_SEND_NOTIFICATION_VIA_CLOUD broadcashUdp " + notification.toJsonObject());
                sendingNotificationViaCloud(notification.receiverNumber, notification);
            }
        });

        /*
        This Method used to get noti fron sync data and send broadcast to other server
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_SYNC_ADDRESS_SEND_NOTIFICATION, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> mesNotiRequest) {


                final Notification notification = Notification.parse(mesNotiRequest.body());
                container.logger().info("Generated Notification:" + notification.toJsonObject());
                container.logger().info("Generated Notification: NotificationVerticle_SYNC_ADDRESS_SEND_NOTIFICATION broadcashUdp " + notification.toJsonObject());
                broadcashUdp(notification.toFullJsonObject(), 0, "", "", container.logger(), false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {
                        mesNotiRequest.reply(result);
                    }
                });

            }
        });

        /*
        This method used to get Notification  from SDK server and send broadcast to other server
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_SDK_SERVER, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> mesNotiRequest) {


                final Notification notification = Notification.parse(mesNotiRequest.body());
                container.logger().info("Generated Notification:" + notification.toJsonObject());
                container.logger().info("Generated Notification: NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_SDK_SERVER broadcashUdp " + notification.toJsonObject());
                broadcashUdp(notification.toFullJsonObject(), 0, "", "", container.logger(), false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {
                        mesNotiRequest.reply(result);
                    }
                });
            }
        });

        /*
        This method used to get Notification  and send SMS
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_ADDRESS_SEND_SMS, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> mesNotiRequest) {

                final Notification notification = Notification.parse(mesNotiRequest.body());
                container.logger().info("Generated SMS:" + notification.toJsonObject());
                container.logger().info("Generated SMS: NotificationVerticle_ADDRESS_SEND_SMS " + notification.toJsonObject());
                sendSms(notification.receiverNumber, notification.sms);

            }
        });

                /*
        This method used to get Notification  and send SMS
         */
        vertx.eventBus().registerLocalHandler(AppConstant.NotificationVerticle_REMIND_SEND_NOTI, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> mesNotiRequest) {

                final Notification notification = Notification.parse(mesNotiRequest.body());
                container.logger().info("Generated Notification:" + notification.toJsonObject());
                container.logger().info("Generated Notification: NotificationVerticle_REMIND_SEND_NOTI broadcashUdp " + notification.toJsonObject());
                broadcashUdp(notification.toFullJsonObject(), 0, "", StringConstUtil.BroadcastField.NOTI_FROM_TOOL_WITH_REDIS, container.logger(), false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {
                        mesNotiRequest.reply(result);
                    }
                });

            }
        });

        //Dung HTTP POST SERVER ban NOTI
        JsonObject joConnectorNotification = globalConfig.getObject(StringConstUtil.ConnectorNotification.JSON_OBJECT, new JsonObject());
        boolean buildNotificationHttp = joConnectorNotification.getBoolean(StringConstUtil.ConnectorNotification.IS_BUILD, false);
        if (buildNotificationHttp) {
            String host = joConnectorNotification.getString(StringConstUtil.ConnectorNotification.HOST, "");
            int port = joConnectorNotification.getInteger(StringConstUtil.ConnectorNotification.PORT, 0);
            final Logger logger = container.logger();
            logger.info("Tao http post notification");
            vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
                @Override
                public void handle(final HttpServerRequest request) {
                    final JsonObject joReply = new JsonObject();
                    final String path = request.path();
                    logger.info("[WebService] [" + request.method() + "] " + " uri: " + request.uri() + " path: " + path);
                    if (request.method().equalsIgnoreCase("POST")) {
                        if (request != null && "/sendNotification".equalsIgnoreCase(path)) {
                            request.bodyHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer buffer) {
                                    logger.info(buffer.toString());
                                    if ("".equalsIgnoreCase(buffer.toString()) && !Misc.isValidJsonObject(buffer.toString())) {
                                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "data is not json");
                                        logger.info(joReply.toString());
                                        request.response().end(joReply.toString());
                                        return;
                                    }
                                    sendConnectorNotification(null, logger, new JsonObject(buffer.toString()), request);
                                }
                            });
                        } else if (request != null && "/sendVHNoti".equalsIgnoreCase(path)) {
                            request.bodyHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer buffer) {
                                    logger.info(buffer.toString());
                                    if ("".equalsIgnoreCase(buffer.toString()) && !Misc.isValidJsonObject(buffer.toString())) {
                                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                                        joReply.putString(StringConstUtil.DESCRIPTION, "data is not json");
                                        logger.info(joReply.toString());
                                        request.response().end(joReply.toString());
                                        return;
                                    }
                                    JsonObject joReceiveData = new JsonObject(buffer.toString());
                                    String title = joReceiveData.getString("title", "");
                                    String content = joReceiveData.getString("content", "");
                                    String phoneNumber = joReceiveData.getString("number", "");
                                    Notification vhNoti = new Notification();
                                    vhNoti.receiverNumber = DataUtil.strToInt(phoneNumber);
                                    vhNoti.body = content;
                                    vhNoti.bodyIOS = content;
                                    vhNoti.caption = title;
                                    vhNoti.priority = 2;
                                    vhNoti.sms = "";
                                    vhNoti.time = System.currentTimeMillis();
                                    vhNoti.tranId = 0L;
                                    vhNoti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                    broadcashUdp(vhNoti.toFullJsonObject(), 0L, phoneNumber, "", logger, false, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean event) {
                                            joReply.putNumber(StringConstUtil.ERROR, 0);
                                            joReply.putString(StringConstUtil.DESCRIPTION, "DONE");
                                            logger.info(joReply.toString());
                                            request.response().end(joReply.toString());
                                        }
                                    });
                                }
                            });
                        } else {
                            joReply.putNumber(StringConstUtil.ERROR, 1000);
                            joReply.putString(StringConstUtil.DESCRIPTION, "Wrong path");
                            logger.info(joReply.toString());
                            request.response().end(joReply.toString());
                        }
                    } else {
                        joReply.putNumber(StringConstUtil.ERROR, 1000);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Wrong method");
                        logger.info(joReply.toString());
                        request.response().end(joReply.toString());
                    }
                }
            }).listen(port, host);
        }
    }

    private void sendConnectorNotification(final Message<JsonObject> message, final Logger logger, JsonObject jsonInfo, final HttpServerRequest request) {
        final JsonObject jsonRep = new JsonObject();
        final Common common = new Common(vertx, container.logger(), container.config());
        PushInfo pushInfo;
        if (message != null && message.body() != null) {
            pushInfo = new PushInfo(message.body());
        } else {
            pushInfo = new PushInfo(jsonInfo);
        }

        if (pushInfo == null) {
            jsonRep.putNumber("error", 1000);
            jsonRep.putString("desc", "push info is null");
            logger.info("message is null");
            if (message != null) {
                message.reply(jsonRep);
            } else {
                request.response().end(jsonRep.toString());
            }
            return;
        }

        final TransactionHistory history = pushInfo.getHistory(); // luu tranhis
        final Popup popup = pushInfo.getPopup(); //show popup
        final com.mservice.common.Notification noti = pushInfo.getNotification(); // show noti
        final BankInfo bankInfo = pushInfo.getBankInfo();
        logger.info("bankinfo is " + bankInfo);

        String infoId = pushInfo.getId(); // Thong tin push info

        PushNotificationInfoManagerDb.Obj pushInfoObj = new PushNotificationInfoManagerDb.Obj();
        pushInfoObj.number = "";
        pushInfoObj.push_info_id = pushInfo.getId();
        pushInfoObj.time = System.currentTimeMillis();
        pushNotificationInfoManagerDb.insert(pushInfoObj, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {
                if (integer == 0) {
                    logger.info("thong tin moi");
                    logger.info("Da nhan duoc id nay roi");
                    jsonRep.putNumber("error", 0);
                    jsonRep.putString("desc", "Da nhan duoc id nay roi");
                    if (message != null) {
                        message.reply(jsonRep);
                    } else {
                        request.response().end(jsonRep.toString());
                    }
                    if (bankInfo != null) {
                        //Cap nhat thong tin bang phone khi nhan du lieu map vi tu Connector
                        final JsonObject joUpdate = new JsonObject();
                        logger.info("bank name is " + bankInfo.getName());
                        logger.info("bank code is " + bankInfo.getCoreBankCode());
                        logger.info("customer name is " + bankInfo.getCustomerName());
                        logger.info("customer phone is " + bankInfo.getPhoneNumber());

                        joUpdate.putString(colName.PhoneDBCols.BANK_NAME, bankInfo.getName());
                        joUpdate.putString(colName.PhoneDBCols.BANK_CODE, bankInfo.getCoreBankCode());
                        joUpdate.putString(colName.PhoneDBCols.NAME, bankInfo.getCustomerName());
                        joUpdate.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, bankInfo.getCustomerId());
                        if (!bankInfo.getCustomerName().equalsIgnoreCase("")) {
                            joUpdate.putBoolean(colName.PhoneDBCols.IS_NAMED, true);
                        }

                        final Popup popup_tmp = popup;
                        final com.mservice.common.Notification noti_tmp = noti;
                        final TransactionHistory tranHis_tmp = history;
                        final BankInfo bankInfo_tmp = bankInfo;
                        final String ocbBankCode = jsonOcbPromo.getString(StringConstUtil.OBCPromo.BANK_CODE, "104");
                        logger.info("ocb bank code is " + ocbBankCode);
                        phonesDb.getPhoneObjInfo(DataUtil.strToInt(bankInfo.getPhoneNumber()), new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(final PhonesDb.Obj phoneObj) {
                                if (phoneObj == null && bankInfo.getMapTimes() == 1) {
                                    logger.info("khong co thong tin trong bang phone " + bankInfo.getPhoneNumber());
                                    PhonesDb.Obj phoneObjNull = new PhonesDb.Obj();
                                    phoneObjNull.number = DataUtil.strToInt(bankInfo.getPhoneNumber());
                                    phoneObjNull.bankPersonalId = bankInfo.getCustomerId();
                                    phoneObjNull.bank_code = bankInfo.getCoreBankCode();
                                    phoneObjNull.bank_name = bankInfo.getName();
                                    phoneObjNull.name = bankInfo.getCustomerName();
                                    promotionProcess.getUserInfoToCheckPromoProgram("", bankInfo.getPhoneNumber(), phoneObjNull, 0, StringConstUtil.TranTypeExtra.FIRST_WALLET_MAPPING, 0, StringConstUtil.WomanNationalField.PROGRAM, null, new JsonObject());
                                } else if (phoneObj == null) {
                                    logger.info("khong co thong tin trong bang phone va map vi khong phai lan dau tien " + bankInfo.getPhoneNumber());
                                } else if (("0".equalsIgnoreCase(phoneObj.bank_code) || "".equalsIgnoreCase(phoneObj.bank_code)) && "".equalsIgnoreCase(phoneObj.bankPersonalId) && "".equalsIgnoreCase(phoneObj.bank_name) && bankInfo.getMapTimes() == 1) {
                                    //Insert vo bang Woman
                                    logger.info("Lan dau tien map vi ngan hang ne 0" + phoneObj.number);

                                    referralV1CodeInputDb.findAndIncCountUser("0" + phoneObj.number, new Handler<ReferralV1CodeInputDb.Obj>() {
                                        @Override
                                        public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                                            if (referralObj == null) {
                                                //Chua tham gia gioi thieu ban be => cho tham gia Lien ket tai khoan
                                                logger.info("Chua lien ket ban be, cho lien ket tai khoan nhe 0" + phoneObj.number);
                                                promotionProcess.getUserInfoToCheckPromoProgram("", "0" + phoneObj.number, phoneObj, 0, StringConstUtil.TranTypeExtra.FIRST_WALLET_MAPPING, 0, StringConstUtil.WomanNationalField.PROGRAM, null, new JsonObject());
                                            } else {
                                                Common.BuildLog log = new Common.BuildLog(container.logger());
                                                log.setPhoneNumber("0" + phoneObj.number);
                                                promotionProcess.executeReferralPromotion("0" + phoneObj.number, StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.FIRST_TIME_BANK_MAPPING, bankInfo, null, null, null, log, new JsonObject());
                                            }
                                        }
                                    });
                                } else {
                                    logger.info("so dien thoai nay da map vi ngan hang tu truoc 0" + phoneObj.number + " bankinfo " + phoneObj.bank_code + " " + bankInfo.getCoreBankCode() + " CMND " + phoneObj.bankPersonalId + " " + bankInfo.getCustomerId());
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, bankInfo.getPhoneNumber(), StringConstUtil.WomanNationalField.PROGRAM, 1000, "so dien thoai nay da map vi ngan hang tu truoc 0" + phoneObj.number + " bankinfo " + phoneObj.bank_code + " " + bankInfo.getCoreBankCode() + " CMND " + phoneObj.bankPersonalId + " " + bankInfo.getCustomerId());
                                }
                                phonesDb.updatePhoneWithOutUpsert(DataUtil.strToInt(bankInfo.getPhoneNumber()), joUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {
                                        logger.info("updatePhoneWithOutUpsert is " + aBoolean);
                                        if (bankInfo_tmp.getCoreBankCode().equalsIgnoreCase(ocbBankCode) && isActiveOCBPromo) {
                                            billPayPromosDb.searchWithPersonalData(bankInfo_tmp.getCustomerId(), bankInfo_tmp.getPhoneNumber(), new Handler<ArrayList<BillPayPromosDb.Obj>>() {
                                                @Override
                                                public void handle(ArrayList<BillPayPromosDb.Obj> arrayBillPayList) {
                                                    if (arrayBillPayList.size() == 0) {
                                                        logger.info("so dien thoai " + bankInfo_tmp.getPhoneNumber() + " " + "du dieu kien tham gia OCB nhe");
                                                        MappingWalletOCBPromoDb.Obj mappingObj = new MappingWalletOCBPromoDb.Obj();
                                                        mappingObj.id = bankInfo_tmp.getCustomerId();
                                                        mappingObj.customer_name = bankInfo_tmp.getCustomerName();
                                                        mappingObj.bank_code = bankInfo_tmp.getCoreBankCode();
                                                        mappingObj.bank_name = "OCB";
                                                        mappingObj.number = bankInfo_tmp.getPhoneNumber();
                                                        mappingObj.mapping_time = bankInfo_tmp.getMapTimes();
                                                        mappingWalletOCBPromoDb.insert(mappingObj, new Handler<Integer>() {
                                                            @Override
                                                            public void handle(Integer event) {
                                                                sendingOCBPopup(popup_tmp, common, logger);
                                                            }
                                                        });
                                                        return;
                                                    } else {
                                                        logger.info("Trung cmnd hoac so dien thoai roi " + bankInfo_tmp.getPhoneNumber());
                                                        sendingInfoToClient(popup_tmp, noti_tmp, tranHis_tmp, common, logger);
                                                        return;
                                                    }
                                                }
                                            });
                                        } else {
                                            sendingInfoToClient(popup_tmp, noti_tmp, tranHis_tmp, common, logger);
                                        }
                                    }
                                });

                                JsonObject joWalletUpdate = new JsonObject();
//                        String id = bankInfo.getCustomerId() + bankInfo.getCoreBankCode();
//                        if("".equalsIgnoreCase(bankInfo.getCustomerId()))
//                        {
//                            id = bankInfo.getPhoneNumber() + bankInfo.getCoreBankCode();
//                        }
//                        joWalletUpdate.putString(colName.MappingWalletBank.ID, id);
                                String id = bankInfo.getPhoneNumber() + bankInfo.getCoreBankCode() + bankInfo.getCustomerId();
//                                joWalletUpdate.putString(colName.MappingWalletBank.ID, id);
                                joWalletUpdate.putString(colName.MappingWalletBank.NUMBER, bankInfo.getPhoneNumber());
                                joWalletUpdate.putString(colName.MappingWalletBank.BANK_NAME, bankInfo.getName());
                                joWalletUpdate.putString(colName.MappingWalletBank.BANK_CODE, bankInfo.getCoreBankCode());
                                joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_NAME, bankInfo.getCustomerName());
                                joWalletUpdate.putNumber(colName.MappingWalletBank.MAPPING_TIME, System.currentTimeMillis());
                                joWalletUpdate.putString(colName.MappingWalletBank.CUSTOMER_ID, bankInfo.getCustomerId());
                                joWalletUpdate.putNumber(colName.MappingWalletBank.NUMBER_OF_MAPPING, bankInfo.getMapTimes());
                                MappingWalletBankDb.Obj mapObj = new MappingWalletBankDb.Obj(joWalletUpdate);
                                mappingWalletBankDb.upsertWalletBank(id, joWalletUpdate, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {
                                        logger.info("update mappingWalletBankDb for bankInfo.getPhoneNumber() is " + bankInfo_tmp.getPhoneNumber());
                                    }
                                });

                                //Kiem tra CMND nay co map vi lan dau tien khong
                                //BEGIN 0000000052 IRONMAN
                                //Kiem tra tiep gia tri anh Vu tra ve
//                                boolean isActiveIronPromo = jsonIronManPromo.getBoolean(StringConstUtil.IronManPromo.IS_ACTIVE, false);
//                                if (bankInfo.getMapTimes() == 1 && isActiveIronPromo) // Be nay map vi lan dau tien ne
//                                {
//                                    requestIronManPromo(bankInfo);
//                                } else if (isActiveIronPromo) {
//                                    ironManPromoGiftDB.findAndModify(bankInfo.getPhoneNumber(), new Handler<IronManPromoGiftDB.Obj>() {
//                                        @Override
//                                        public void handle(IronManPromoGiftDB.Obj event) {
//
//                                        }
//                                    });
//                                }
//                                else
//                                {
//                                    logger.info("Be nay map vi nhieu lan qua " + bankInfo.getMapTimes());
////                                    if(bankInfo.getMapTimes() > -1) //The nay dang map vi
////                                    {
////                                        promotionProcess.executeReferralPromotion(StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.FIRST_TIME_BANK_MAPPING, bankInfo, null, null, null, log, new JsonObject());
////                                    }
//                                    return;
//                                }
                                //END 0000000052 IRONMAN
                            }
                        });
                    } else {
                        logger.info("bank if null");
                        sendingInfoToClient(popup, noti, history, common, logger);
                    }
                } else {
                    logger.info("Da nhan duoc id nay roi");
                    jsonRep.putNumber("error", 0);
                    jsonRep.putString("desc", "Da nhan duoc id nay roi");
                    if (message != null) {
                        message.reply(jsonRep);
                    } else {
                        request.response().end(jsonRep.toString());
                    }
                }
            }
        });
    }

    //BEGIN 0000000029
    public void sendingInfoToClient(final Popup popup, final com.mservice.common.Notification noti, TransactionHistory history, Common common, Logger logger) {
        if (history != null) {
            logger.info("history is " + history);
            logger.info("history is " + history.getJsonObject());
            logger.info("history is " + history.getStatus() + " | " + history.getBankCode() + " | "
                    + history.getBillId() + " | " + history.getHtmlContent() + " | " + history.getNotificationContent() + " | "
                    + history.getNotificationHeader());
            sendingStandardTransHisFromCore(history, common);
        }
        if (noti != null) {
            //Show noti
            logger.info("noti is " + noti);
            logger.info("noti is " + noti.getJsonObject());
            logger.info("noti is " + noti.getContent() + " | " + noti.getHeader() + " | " + noti.getInitiator());
            boolean hasTransaction = history != null ? true : false;
            broadcashUdp(noti.getJsonObject(), noti.getTransId(), noti.getInitiator(), StringConstUtil.BroadcastField.NOTI, logger, hasTransaction, new Handler<Boolean>() {
                @Override
                public void handle(Boolean result) {
                    if (!result)
                        sendingStandardNotiFromCore(noti);
                }
            });
        }
        if (popup != null) {
            logger.info("bankinfo is not null oh yeah");
            //todo show popup
            logger.info("popup is " + popup);
            logger.info("popup is " + popup.getJsonObject());
            logger.info("content popup is " + popup.getContent());
            logger.info("html popup is " + popup.getHtmlContent());
            logger.info("popup is " + popup.getCancelButtonLabel() + " | " + popup.isEnabledClose() + " | " +
                    popup.getOkButtonLabel() + " | " + popup.getHeader() + " | " + popup.getContent());
            logger.info("show money popup " + popup.isShowMoney());
            broadcashUdp(popup.getJsonObject(), popup.getTransId(), popup.getInitiator(), StringConstUtil.BroadcastField.POPUP, logger, false, new Handler<Boolean>() {
                @Override
                public void handle(Boolean result) {
                    if (!result) {
                        Notification notification = null;
                        if (popup.isShowMoney()) {
                            notification = sendingMoneyPopupFromCore(popup);
                        } else if ("mis".equalsIgnoreCase(popup.getServiceCode())) {
                            //SEND POPUP/NOTI FROM MIS TEAM
                            notification = sendingStandardPopupFromMIS(popup);
                        } else {
                            notification = createNotiFromPopupConnector(popup);
                        }
                        Misc.sendNoti(vertx, notification);
                    }
                }
            });

        }
    }

    public Notification sendingStandardPopupFromCore(Popup popup) {
        JsonObject jsonExtra = new JsonObject();
        int type = 0;
        if (popup.getType() == -1) {
            type = (null == popup.getCancelButtonLabel() || "".equalsIgnoreCase(popup.getCancelButtonLabel())) ? 0 : 1;
        } else {
            type = popup.getType();
        }
        //int type = popup.getType();
        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, StringConstUtil.INFO_POPUP);
        if (popup.getCancelButtonLabel() != null && !popup.getCancelButtonLabel().equalsIgnoreCase("")) {
            String button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }
        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        jsonExtra.putString(StringConstUtil.SERVICE_ID, popup.getServiceCode());
        jsonExtra.putString("htmlBody", popup.getHtmlContent());
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = popup.getTransId();
        notification.time = new Date().getTime();
        notification.receiverNumber = DataUtil.strToInt(popup.getInitiator());
        notification.extra = jsonExtra.toString();
//        Misc.sendNoti(vertx, notification);
//        broadcashUdp();
        return notification;
    }

    /**
     * Popup send from MIS is one notification
     *
     * @param popup
     */
    public Notification sendingStandardPopupFromMIS(Popup popup) {
        JsonObject jsonExtra = new JsonObject();

//        if(popup.getType() == -1)
//        {
//            type = (null == popup.getCancelButtonLabel() || "".equalsIgnoreCase(popup.getCancelButtonLabel())) ? 0 : 1;
//        }
//        else {
//            type = popup.getType();
//        }
        //int type = popup.getType();
//        jsonExtra.putNumber(StringConstUtil.TYPE, type);
//        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
//        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
//        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, StringConstUtil.INFO_POPUP);
//        if (popup.getCancelButtonLabel() != null && !popup.getCancelButtonLabel().equalsIgnoreCase("")) {
//            String button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
//            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
//        }
//        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
//        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
//        jsonExtra.putString(StringConstUtil.SERVICE_ID, popup.getServiceCode());
//        jsonExtra.putString("htmlBody", popup.getHtmlContent());
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = popup.getTransId();
        notification.time = new Date().getTime();
        notification.refId = popup.getBillId();
        notification.receiverNumber = DataUtil.strToInt(popup.getInitiator());
        notification.extra = jsonExtra.putNumber("balanceLimitAlert", 2000000).toString();
//        Misc.sendNoti(vertx, notification);
//        broadcashUdp();
        return notification;
    }

    public Notification sendingMoneyPopupFromCore(Popup popup) {
        JsonObject jsonExtra = new JsonObject();
        int type = 0;
        if (popup.getType() == -1) {
            type = (null == popup.getCancelButtonLabel() || "".equalsIgnoreCase(popup.getCancelButtonLabel())) ? 0 : 1;
        } else {
            type = popup.getType();
        }

        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        String button_title_2 = "";
        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, StringConstUtil.INFO_POPUP);
        if (popup.getCancelButtonLabel() != null && !popup.getCancelButtonLabel().equalsIgnoreCase("")) {
            button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }

        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = popup.getTransId();
        notification.time = new Date().getTime();
        notification.receiverNumber = DataUtil.strToInt(popup.getInitiator());
        notification.extra = String.format(PromoContentNotification.MONEY_POPUP, "", button_title_1, button_title_2, button_title_x, popup.getContent(), popup.getAmount(), popup.getInitiator(), popup.getBillId(), popup.getServiceCode());
        notification.btnTitle = "Đồng Ý";
        notification.status = 0;
        notification.btnStatus = 1;
//        Misc.sendNoti(vertx, notification);
        return notification;
    }

    public Notification createNotiFromPopupConnector(Popup popup) {
        JsonObject jsonExtra = new JsonObject();
        int type = 0;
        if (popup.getType() == -1) {
            type = (null == popup.getCancelButtonLabel() || "".equalsIgnoreCase(popup.getCancelButtonLabel())) ? 0 : 1;
        } else {
            type = popup.getType();
        }

        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : "";
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        String button_title_2 = "";
        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, StringConstUtil.INFO_POPUP);
        if (popup.getCancelButtonLabel() != null && !popup.getCancelButtonLabel().equalsIgnoreCase("")) {
            button_title_2 = popup != null ? popup.getCancelButtonLabel() : "";
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }

        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = popup.getTransId();
        notification.time = new Date().getTime();
        notification.receiverNumber = DataUtil.strToInt(popup.getInitiator());

        String htmlContent = popup.getHtmlContent().replaceAll("\"", "'");
        jsonExtra.putString("htmlBody", htmlContent);
        jsonExtra.putString(StringConstUtil.SERVICE_ID, popup.getServiceCode());
        notification.extra = jsonExtra.toString();
        notification.btnTitle = popup.getOkNotiButtonLabel();
        notification.status = 0;
        notification.btnStatus = "".equalsIgnoreCase(popup.getOkNotiButtonLabel()) ? 0 : 1;

        return notification;
    }

    public void sendingStandardNotiFromCore(com.mservice.common.Notification notiConnector) {
        String notiCaption = notiConnector.getHeader() != null ? notiConnector.getHeader() : "";
        String notiBody = notiConnector.getContent() != null ? notiConnector.getContent() : "";
        final Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
        noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = notiConnector.getTransId();
        noti.time = new Date().getTime();
//                noti.extra = new JsonObject()
//                        .putString("giftId", gift.getModelId())
//                        .putString("giftTypeId", gift.typeId)
//                        .putString("amount", String.valueOf(tranAmount))
//                        .putString("sender", "Trải nghiệm thanh toán")
//                        .putString("senderName", "MoMo")
//                        .putString("msg",giftMessage)
//                        .putNumber("status", gift.status)
//                        .putString("serviceid", gift.typeId)
//                        .toString();

        noti.receiverNumber = DataUtil.strToInt(notiConnector.getInitiator());

        Misc.sendNoti(vertx, noti);
    }

    public Notification createBroadcashNotiByConnectorNoti(com.mservice.common.Notification notiConnector, boolean hasTransaction) {
        String notiCaption = notiConnector.getHeader() != null ? notiConnector.getHeader() : "";
        String notiBody = notiConnector.getContent() != null ? notiConnector.getContent() : "";
        final Notification noti = new Notification();
        noti.priority = 2;
        noti.type = hasTransaction ? MomoProto.NotificationType.NOTI_TRANSACTION_VALUE : MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
        noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = notiConnector.getTransId();
        noti.time = new Date().getTime();
//                noti.extra = new JsonObject()
//                        .putString("giftId", gift.getModelId())
//                        .putString("giftTypeId", gift.typeId)
//                        .putString("amount", String.valueOf(tranAmount))
//                        .putString("sender", "Trải nghiệm thanh toán")
//                        .putString("senderName", "MoMo")
//                        .putString("msg",giftMessage)
//                        .putNumber("status", gift.status)
//                        .putString("serviceid", gift.typeId)
//                        .toString();

        noti.receiverNumber = DataUtil.strToInt(notiConnector.getInitiator());

        return noti;
    }

    public void broadcashUdp(final JsonObject joData, final long tranId, final String phoneNumber, final String type, final Logger logger, final boolean hasTransaction, final Handler<Boolean> callback) {
        final DatagramSocket socket = vertx.createDatagramSocket(InternetProtocolFamily.IPv4);
        // Send a Buffer
        connectorHTTPPostPathDb.findContain("broadcast", new Handler<ArrayList<ConnectorHTTPPostPathDb.Obj>>() {
            @Override
            public void handle(final ArrayList<ConnectorHTTPPostPathDb.Obj> listConnectorPaths) {
                if (listConnectorPaths.size() < 1) {
                    callback.handle(false);
                } else {
                    Notification notification = new Notification();
                    if (StringConstUtil.BroadcastField.POPUP.equalsIgnoreCase(type)) {
                        Popup popup = new Popup(joData);

                        if (popup.isShowMoney()) {
                            notification = sendingMoneyPopupFromCore(popup);
                        } else if ("mis".equalsIgnoreCase(popup.getServiceCode())) {
                            //SEND POPUP/NOTI FROM MIS TEAM
                            notification = sendingStandardPopupFromMIS(popup);
                        } else {
//                           notification = sendingStandardPopupFromCore(popup);
                            notification = createNotiFromPopupConnector(popup);
                        }
                    } else if (StringConstUtil.BroadcastField.NOTI.equalsIgnoreCase(type)) {
                        notification = createBroadcashNotiByConnectorNoti(new com.mservice.common.Notification(joData), hasTransaction);
                    } else if (StringConstUtil.BroadcastField.UPDATE_LIST_CACHE.equalsIgnoreCase(type)) {

                    } else {
                        notification = Notification.parse(joData);
                    }

                    if (StringConstUtil.BroadcastField.UPDATE_LIST_CACHE.equalsIgnoreCase(type)) {
                        container.logger().info("UPDATE_LIST_CACHE");
                        final AtomicInteger listConnectors = new AtomicInteger(listConnectorPaths.size());
                        vertx.setPeriodic(500L, new Handler<Long>() {
                            @Override
                            public void handle(Long timer) {
                                int position = listConnectors.decrementAndGet();
                                if (position < 0) {
                                    vertx.cancelTimer(timer);
                                    callback.handle(true);
                                    return;
                                }
                                logger.info("host: " + listConnectorPaths.get(position).host);
                                logger.info("port: " + listConnectorPaths.get(position).port);
                                JsonObject joBroadCash = new JsonObject().putObject(StringConstUtil.BroadcastField.DATA, joData).putString(StringConstUtil.BroadcastField.TYPE, type);
                                final Buffer buffer = new Buffer(joBroadCash.toString());
                                socket.send(buffer, listConnectorPaths.get(position).host, listConnectorPaths.get(position).port, new Handler<AsyncResult<DatagramSocket>>() {
                                    @Override
                                    public void handle(AsyncResult<DatagramSocket> asyncResult) {
                                        if (asyncResult != null && asyncResult.succeeded()) {
                                            logger.info("RESULT BROATCASH UPDATE_LIST_CACHE SUCCESS " + asyncResult.succeeded());
                                        } else if (asyncResult != null) {
                                            logger.info("RESULT BROATCASH UPDATE_LIST_CACHE FAIL " + asyncResult.result() + "|" + asyncResult.cause());
                                        } else {
                                            logger.info("RESULT BROATCASH UPDATE_LIST_CACHE FAIL => ASYNC IS NULL");
                                        }
                                    }
                                });
                            }
                        });
                        return;
                    } else if (StringConstUtil.BroadcastField.NOTI_FROM_TOOL.equalsIgnoreCase(type)) {
                        container.logger().info("saveNotificationGenId");
                        final Notification notiFromTool = notification;
                        notificationToolDb.upsertNotification(DataUtil.strToInt(phoneNumber), notification.toFullJsonObject(), new Handler<String>() {
                            @Override
                            public void handle(String savedId) {
                                final AtomicInteger listConnectors = new AtomicInteger(listConnectorPaths.size());
                                vertx.setPeriodic(500L, new Handler<Long>() {
                                    @Override
                                    public void handle(Long timer) {
                                        int position = listConnectors.decrementAndGet();
                                        if (position < 0) {
                                            vertx.cancelTimer(timer);
                                            callback.handle(true);
                                            return;
                                        }
                                        logger.info("host: " + listConnectorPaths.get(position).host);
                                        logger.info("port: " + listConnectorPaths.get(position).port);
                                        JsonObject joBroadCash = new JsonObject().putObject(StringConstUtil.BroadcastField.DATA, notiFromTool.toFullJsonObject()).putString(StringConstUtil.BroadcastField.TYPE, type);
                                        final Buffer buffer = new Buffer(joBroadCash.toString());
                                        socket.send(buffer, listConnectorPaths.get(position).host, listConnectorPaths.get(position).port, new Handler<AsyncResult<DatagramSocket>>() {
                                            @Override
                                            public void handle(AsyncResult<DatagramSocket> asyncResult) {
                                                if (asyncResult != null && asyncResult.succeeded()) {
                                                    logger.info("RESULT BROATCASH SUCCESS " + asyncResult.succeeded());
                                                } else if (asyncResult != null) {
                                                    logger.info("RESULT BROATCASH FAIL " + asyncResult.result() + "|" + asyncResult.cause());
                                                } else {
                                                    logger.info("RESULT BROATCASH FAIL => ASYNC IS NULL");
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        return;
                    } else if (StringConstUtil.BroadcastField.NOTI_FROM_TOOL_WITH_REDIS.equalsIgnoreCase(type)) {
                        container.logger().info("NOTI_FROM_TOOL_WITH_REDIS");
                        final Notification notiRemind = notification;
                        final AtomicInteger listConnectors = new AtomicInteger(listConnectorPaths.size());
                        vertx.setPeriodic(500L, new Handler<Long>() {
                            @Override
                            public void handle(Long timer) {
                                int position = listConnectors.decrementAndGet();
                                if (position < 0) {
                                    vertx.cancelTimer(timer);
                                    callback.handle(true);
                                    return;
                                }
                                logger.info("host: " + listConnectorPaths.get(position).host);
                                logger.info("port: " + listConnectorPaths.get(position).port);
                                JsonObject joBroadCash = new JsonObject().putObject(StringConstUtil.BroadcastField.DATA, notiRemind.toFullJsonObject()).putString(StringConstUtil.BroadcastField.TYPE, type);
                                final Buffer buffer = new Buffer(joBroadCash.toString());
                                socket.send(buffer, listConnectorPaths.get(position).host, listConnectorPaths.get(position).port, new Handler<AsyncResult<DatagramSocket>>() {
                                    @Override
                                    public void handle(AsyncResult<DatagramSocket> asyncResult) {
                                        if (asyncResult != null && asyncResult.succeeded()) {
                                            logger.info("RESULT BROATCASH SUCCESS " + asyncResult.succeeded());
                                        } else if (asyncResult != null) {
                                            logger.info("RESULT BROATCASH FAIL " + asyncResult.result() + "|" + asyncResult.cause());
                                        } else {
                                            logger.info("RESULT BROATCASH FAIL => ASYNC IS NULL");
                                        }
                                    }
                                });
                            }
                        });
                        return;
                    }
                    saveNotiBeforeBroadCast(notification, new Handler<Notification>() {
                        @Override
                        public void handle(final Notification noti) {
                            final AtomicInteger listConnectors = new AtomicInteger(listConnectorPaths.size());
                            vertx.setPeriodic(500L, new Handler<Long>() {
                                @Override
                                public void handle(Long timer) {
                                    int position = listConnectors.decrementAndGet();
                                    if (position < 0) {
                                        vertx.cancelTimer(timer);
                                        callback.handle(true);
                                        return;
                                    }
                                    logger.info("host: " + listConnectorPaths.get(position).host);
                                    logger.info("port: " + listConnectorPaths.get(position).port);
                                    JsonObject joBroadCash = new JsonObject().putObject(StringConstUtil.BroadcastField.DATA, noti.toFullJsonObject()).putString(StringConstUtil.BroadcastField.TYPE, type);
                                    final Buffer buffer = new Buffer(joBroadCash.toString());
                                    socket.send(buffer, listConnectorPaths.get(position).host, listConnectorPaths.get(position).port, new Handler<AsyncResult<DatagramSocket>>() {
                                        @Override
                                        public void handle(AsyncResult<DatagramSocket> asyncResult) {
                                            if (asyncResult != null && asyncResult.succeeded()) {
                                                logger.info("RESULT BROATCASH SUCCESS " + asyncResult.succeeded());
                                            } else if (asyncResult != null) {
                                                logger.info("RESULT BROATCASH FAIL " + asyncResult.result() + "|" + asyncResult.cause());
                                            } else {
                                                logger.info("RESULT BROATCASH FAIL => ASYNC IS NULL");
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }
        });

//    // Send a String
//        socket.send("A string used as content", 1234, "10.0.0.1", asyncResult -> {
//            System.out.println("Send succeeded? " + asyncResult.succeeded());
//        });
    }


    public void sendingStandardTransHisFromCore(TransactionHistory history, Common common) {
        //Send tranhis
        final TranObj mainObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        TransactionHistory.Type type = (history != null && history.getType() != null) ? history.getType() : TransactionHistory.Type.CASH_IN;
        int tranType = getTranType(type);
        mainObj.tranType = tranType;
        mainObj.comment = history.getTransactionContent(); // Sua ngay 27/06/2016 => Anh Vu (connector leader) bao ke
        mainObj.tranId = history.getTransId();
        mainObj.clientTime = currentTime;
        mainObj.ackTime = currentTime;
        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = history.getAmount();
        mainObj.status = (TransactionHistory.Status.OK == history.getStatus()) ? TranObj.STATUS_OK : TranObj.STATUS_FAIL;
        mainObj.error = 0;
        mainObj.io = tranType == MomoProto.TranHisV1.TranType.BANK_OUT_VALUE ? -1 : 1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.owner_number = DataUtil.strToInt(history.getInitiator());
        mainObj.partnerName = history.getPName() == null ? "M_Service" : history.getPName();
        mainObj.partnerId = history.getPId() == null ? history.getServiceCode() == null ? "" : history.getServiceCode() : history.getPId();
        mainObj.billId = history.getBillId() != null ? history.getBillId() : "";
        mainObj.partnerRef = history.getCmt() == null || "".equalsIgnoreCase(history.getCmt()) ? mainObj.comment : history.getCmt();
        mainObj.share = new JsonArray().add(new JsonObject().putString("html", history.getHtmlContent())).add(new JsonObject().putString("coreGroupId", history.getGroupId()));
        mainObj.parterCode = history.getPCode() == null ? "" : history.getPCode();
//        Misc.sendTranAsSynWithOutSock(transDb, mainObj, common);
        transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                }
            }
        });
    }

    public int getTranType(TransactionHistory.Type type) {
        int tranType = 0;
        if (type == null) {
            tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
        } else {
            switch (type) {
                case CASH_IN:
                    tranType = MomoProto.TranHisV1.TranType.BANK_IN_VALUE;
                    break;
                case CASH_OUT:
                    tranType = MomoProto.TranHisV1.TranType.BANK_OUT_VALUE;
                    break;
                default:
                    tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
                    break;
            }
        }
        return tranType;
    }
    //END 0000000029


    public void sendingNotificationViaSocketSuccess(int phoneNumber, Notification notification) {
        boolean containedId = sendingSet.remove(notification.id);
        if (!containedId)
            return;

        //todo ban theo danh sach
        if (notification.tranId == -10001L) {
            StatisticUtils.fireSendNotificationListViaCloud(vertx.eventBus());
        } else if (notification.tranId == -9999L) {
            StatisticUtils.fireSendNotificationViaSocket(vertx.eventBus());
        }

        container.logger().debug(String.format("%d successfully received notification %s.", phoneNumber, notification.toJsonObject()));
    }

    public void sendingNotificationViaSocketFail(final int phoneNumber, final Notification notification) {
        boolean containedId = sendingSet.remove(notification.id);
        if (!containedId)
            return;
        container.logger().debug(String.format("A notification sent to %d has failed. The phone would be offline.", phoneNumber));

        //todo: The phone is offline. Send notification via Cloud Service or SMS.

        if (notification.priority <= 1) {
            // high priority, send Notification via sms.
            sendSms(phoneNumber, notification.sms);
        }

        final Common.BuildLog log = new Common.BuildLog(container.logger());
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "sendingNotificationViaSocketFail");

        //da OS va token roi
        if (!"".equalsIgnoreCase(notification.os) && !"".equalsIgnoreCase(notification.token)) {

            log.add("token", notification.token);
            log.add("tranid", notification.tranId);
            log.add("type", notification.type);


            log.add("had token and os in noti object", "");

            //todo ban theo danh sach
            if (notification.tranId == -10001L) {
                StatisticUtils.fireSendNotificationListViaCloud(vertx.eventBus());
            } else if (notification.tranId == -9999L) {
                StatisticUtils.fireSendNotificationViaCloud(vertx.eventBus());
            }
            boolean allowSendNotiLocal = container.config().getBoolean("allow_send_noti_cloud_local", false);
            if (allowSendNotiLocal) {
                vertx.eventBus().send(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject());
            } else {
                //            vertx.eventBus().publish(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject());
                vertx.eventBus().publish(AppConstant.HTTP_POST_BUS_ADDRESS,
                        Misc.makeHttpPostWrapperData(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject()));
            }
            log.writeLog();
            return;
        }

        //lay token tu socket data
        notification.receiverNumber = phoneNumber;
        BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
        helper.setType(SoapProto.Broadcast.MsgType.GET_TOKEN_VALUE);
        log.add("noti", notification.toFullJsonObject());
        vertx.eventBus().sendWithTimeout(Misc.getNumberBus(phoneNumber)
                , helper.getJsonObject()
                , 1500
                , new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> result) {
                        Message<JsonObject> message = result.result();

                        if (result.succeeded()) {
                            if (message.body() != null) {
                                notification.token = message.body().getString("token", "");
                                notification.os = message.body().getString("os", "");
                            }

                            log.add("Get token from Sockdata", "Success");
                        }

                        log.add("Get token from Sockdata", "Failed");

                        //todo ban theo danh sach
                        if (notification.tranId == -10001L) {
                            StatisticUtils.fireSendNotificationListViaCloud(vertx.eventBus());
                        } else if (notification.tranId == -9999L) {
                            StatisticUtils.fireSendNotificationViaCloud(vertx.eventBus());
                        }

//                vertx.eventBus().publish(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject());
                        vertx.eventBus().publish(AppConstant.HTTP_POST_BUS_ADDRESS,
                                Misc.makeHttpPostWrapperData(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject()));
                        log.writeLog();
                    }
                });
    }

    public void sendingNotificationViaCloud(final int phoneNumber, final Notification notification) {
        container.logger().debug(String.format("A notification sent to %d has failed. The phone would be offline.", phoneNumber));

        //todo: The phone is offline. Send notification via Cloud Service or SMS.

        if (notification.priority <= 1) {
            // high priority, send Notification via sms.
            sendSms(phoneNumber, notification.sms);
        }

        final Common.BuildLog log = new Common.BuildLog(container.logger());
        log.setPhoneNumber("0" + phoneNumber);
        log.add("function", "sendingNotificationViaSocketFail");

        //da OS va token roi
        if (!"".equalsIgnoreCase(notification.os) && !"".equalsIgnoreCase(notification.token)) {

            log.add("token", notification.token);
            log.add("tranid", notification.tranId);
            log.add("type", notification.type);


            log.add("had token and os in noti object", "");

            //todo ban theo danh sach
            if (notification.tranId == -10001L) {
                StatisticUtils.fireSendNotificationListViaCloud(vertx.eventBus());
            } else if (notification.tranId == -9999L) {
                StatisticUtils.fireSendNotificationViaCloud(vertx.eventBus());
            }
            boolean allowSendNotiLocal = container.config().getBoolean("allow_send_noti_cloud_local", false);
            if (allowSendNotiLocal) {
                vertx.eventBus().send(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject());
            } else {
                //            vertx.eventBus().publish(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject());
                vertx.eventBus().publish(AppConstant.HTTP_POST_BUS_ADDRESS,
                        Misc.makeHttpPostWrapperData(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject()));
            }
            log.writeLog();
            return;
        }

        //lay token tu socket data
        notification.receiverNumber = phoneNumber;
        BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
        helper.setType(SoapProto.Broadcast.MsgType.GET_TOKEN_VALUE);
        log.add("noti", notification.toFullJsonObject());
        vertx.eventBus().sendWithTimeout(Misc.getNumberBus(phoneNumber)
                , helper.getJsonObject()
                , 1500
                , new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> result) {
                Message<JsonObject> message = result.result();

                if (result.succeeded()) {
                    if (message.body() != null) {
                        notification.token = message.body().getString("token", "");
                        notification.os = message.body().getString("os", "");
                    }

                    log.add("Get token from Sockdata", "Success");
                }

                log.add("Get token from Sockdata", "Failed");

                //todo ban theo danh sach
                if (notification.tranId == -10001L) {
                    StatisticUtils.fireSendNotificationListViaCloud(vertx.eventBus());
                } else if (notification.tranId == -9999L) {
                    StatisticUtils.fireSendNotificationViaCloud(vertx.eventBus());
                }

//                vertx.eventBus().publish(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject());
                vertx.eventBus().publish(AppConstant.HTTP_POST_BUS_ADDRESS,
                        Misc.makeHttpPostWrapperData(AppConstant.CloundNotifyVerticle, notification.toFullJsonObject()));
                log.writeLog();
            }
        });
    }

    private void sendSms(int phoneNumber, String content) {
        if (content == null || content.isEmpty())
            return;
        container.logger().info("Send Sms to " + phoneNumber);

        SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                .setSmsId(0)
                .setToNumber(phoneNumber)
                .setContent(content)
                .build();

        vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
    }

    //BEGIN 000000052 IRON MAN
    private void requestIronManPromo(final BankInfo bankInfo) {
        vcbCmndRecs.findOne(bankInfo.getCustomerId(), new Handler<VcbCmndRecs.Obj>() {
            @Override
            public void handle(VcbCmndRecs.Obj vcbCmndObj) {
                if (vcbCmndObj == null) {
                    //Chua map lan nao ne ... ngon lanh
                    // Kiem tra xem co v1 chua ne
                    ironManPromoGiftDB.findOne(bankInfo.getPhoneNumber(), new Handler<IronManPromoGiftDB.Obj>() {
                        @Override
                        public void handle(IronManPromoGiftDB.Obj ironManPromoObj) {
                            if (ironManPromoObj != null && ironManPromoObj.has_voucher_group_1 && !ironManPromoObj.has_voucher_group_3 && ironManPromoObj.promo_count < 6 && ironManPromoObj.gift_id_6.equalsIgnoreCase("")) {
                                //Tra thuong group 3 cho em no

                                //So dien thoai nay dang online
                                IronManPromoObj.requestIronManPromo(vertx, bankInfo.getPhoneNumber(), 0, 0, "", StringConstUtil.IronManPromo.IRON_PROMO_3, bankInfo.getCustomerId(), "",
                                        new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject jsonObject) {

                                            }
                                        }
                                );
//                                                    BroadcastHandler.LocalMsgHelper helper = new BroadcastHandler.LocalMsgHelper();
//                                                    helper.setType(SoapProto.Broadcast.MsgType.GET_TOKEN_VALUE);
//                                                    vertx.eventBus().sendWithTimeout(Misc.getNumberBus(DataUtil.strToInt(bankInfo_tmp.getPhoneNumber()))
//                                                            , helper.getJsonObject()
//                                                            , 1500
//                                                            , new Handler<AsyncResult<Message<JsonObject>>>() {
//                                                                @Override
//                                                                public void handle(AsyncResult<Message<JsonObject>> result) {
//                                                                    Message<JsonObject> message = result.result();
//                                                                    if (result.succeeded()) {
//                                                                        if (message.body() != null) {
//
//                                                                        }
//                                                                        else{
//                                                                            //
//                                                                            JsonObject joUpdate = new JsonObject();
//                                                                            joUpdate.putString(colName.IronManPromoGift.CMND, bankInfo_tmp.getCustomerId());
//                                                                            ironManPromoGiftDB.updatePartial(bankInfo_tmp.getPhoneNumber(), joUpdate, new Handler<Boolean>() {
//                                                                                @Override
//                                                                                public void handle(Boolean aBoolean) {
//
//                                                                                }
//                                                                            });
//
//                                                                        }
//                                                                    }
//
//                                                                }
//                                                            });
                            }
                        }
                    });
                }
            }
        });
    }
    //END 00000000052 IRON MAN

    //OCB PROMO
//    private void requestOCBPromo(final BankInfo bankInfo)
//    {
//
//        BillPayPromoObj.requestBillPayPromo(vertx, bankInfo.getPhoneNumber(), 0, 0, "", StringConstUtil.OBCPromo.OCB, new Handler<JsonObject>() {
//            @Override
//            public void handle(JsonObject event) {
//
//            }
//        });
//
//    }

    public void sendingOCBPopup(Popup popup, Common common, Logger logger) {
        if (popup != null) {
            logger.info("bankinfo is not null oh yeah");
            //todo show popup
            logger.info("popup is " + popup);
            logger.info("popup is " + popup.getJsonObject());
            logger.info("popup is " + popup.getCancelButtonLabel() + popup.isEnabledClose() +
                    popup.getOkButtonLabel() + popup.getHeader() + popup.getContent());
            popup.setHeader(BillPayPromoConst.POPUP_HEADER_OCB_PROMO);
            popup.setContent(BillPayPromoConst.POPUP_CONTENT_OCB_PROMO);
            long tid = System.currentTimeMillis();
            sendingOCBPopupFromCore(popup, tid);
            JsonObject jsonTranHis = new JsonObject();
            jsonTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, 7);
            jsonTranHis.putString(colName.TranDBCols.COMMENT, BillPayPromoConst.POPUP_CONTENT_OCB_PROMO);
            jsonTranHis.putNumber(colName.TranDBCols.TRAN_ID, tid);
            jsonTranHis.putNumber(colName.TranDBCols.AMOUNT, 100000);
            jsonTranHis.putNumber(colName.TranDBCols.STATUS, 0);
            jsonTranHis.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(popup.getInitiator()));
            jsonTranHis.putString(colName.TranDBCols.BILL_ID, "");
            String html = BillPayPromoConst.HTMLSTR_OCB_PROMO.replace("serviceid", "topup");
            jsonTranHis.putString(StringConstUtil.HTML, html);
            Misc.sendingStandardTransHisFromJson(vertx, transDb, jsonTranHis, new JsonObject());
        }
    }

    public void sendingOCBPopupFromCore(Popup popup, long tid) {
        JsonObject jsonExtra = new JsonObject();
        int type = (null == popup.getCancelButtonLabel() || "".equalsIgnoreCase(popup.getCancelButtonLabel())) ? 0 : 1;
        //int type = popup.getType();
        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        if (popup.getCancelButtonLabel() != null && !popup.getCancelButtonLabel().equalsIgnoreCase("")) {
            String button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }
        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = tid;
        notification.time = new Date().getTime();
        notification.receiverNumber = DataUtil.strToInt(popup.getInitiator());
        notification.extra = jsonExtra.toString();
        Misc.sendNoti(vertx, notification);

    }

    //OCB PROMO

    public void buildParams(HttpClientRequest req, JsonObject jo, Common.BuildLog log) {

        Buffer buffer = null;
        buffer = new Buffer(jo.toString());
        req.putHeader("Content-Length", buffer.length() + "");
        req.end(buffer);

    }

    private void saveNotiBeforeBroadCast(final Notification notification, final Handler<Notification> callback) {
        if (notification == null) {
            container.logger().info("Generated Notification notification: notification == null");
            return;
        }
        container.logger().info("Generated Notification sendNotiBroashCast:" + notification.toJsonObject());

        final int phoneNumber = notification.receiverNumber;
        if (phoneNumber == 0) {
            container.logger().info("Generated Notification sendNotiBroashCast: phoneNumber == 0");
            return;
        }
        notificationDb.saveNotification(phoneNumber, notification.toJsonObject(), new Handler<String>() {
            @Override
            public void handle(String savedId) {
                if (savedId == null) {
                    //todo: Can't persist the Notification.
                    container.logger().error("Can't persist the Notification:" + notification.toJsonObject());
                    container.logger().error("savedId == null:");
                    return;
                }
                notification.id = savedId;
                //Todo: notification has persisted. Send it back to phone.
                callback.handle(notification);
            }
        });
    }
}
