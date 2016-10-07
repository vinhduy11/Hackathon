package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.wc.DuDoanDb;
import com.mservice.momo.data.wc.InviteeDb;
import com.mservice.momo.data.wc.MatchDb;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.MongoExportVerticle;
import com.mservice.momo.vertx.WcTransfer;
import com.mservice.momo.vertx.WcVerticle;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.wc.DuDoan;
import com.mservice.momo.vertx.models.wc.Invitee;
import com.mservice.momo.vertx.models.wc.Match;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nam on 6/10/14.
 */
public class WcController {
    private Vertx vertx;

    private MatchDb matchDb;

    private Logger logger;

    private InviteeDb inviteeDb;
    private DuDoanDb duDoanDb;

    public WcController(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.matchDb = new MatchDb(vertx, container);
        logger = container.logger();
        inviteeDb = new InviteeDb(vertx, container);
        duDoanDb = new DuDoanDb(vertx, container);
    }

    @Action(path = "/wc/matchFinish")
    public void list(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");
        String result = params.get("result");
        String a = params.get("a");
        String b = params.get("b");

        if (matchId == null || result == null || a == null || b == null) {
            callback.handle("Missing params");
            return;
        }
        Integer mResult = Integer.parseInt(result);
        Integer mA = Integer.parseInt(a);
        Integer mB = Integer.parseInt(b);

        JsonObject request = new JsonObject()
                .putString("cmd", WcVerticle.CMD_MATCH_FINISH)
                .putString("matchId", matchId)
                .putNumber("result", mResult)
                .putNumber("a", mA)
                .putNumber("b", mB);

        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }


    @Action(path = "/wc/tranReset")
    public void tranReset(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_TRANS_RESET);
        request.putString("matchId", matchId);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/tranInit")
    public void tranInit(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_TRANS_INIT);
        request.putString("matchId", matchId);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }


    @Action(path = "/wc/confirmMakeTran")
    public void confirmMakeTran(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }
        Match filter = new Match();
        filter.setModelId(matchId.trim());
        matchDb.findOne(filter, new Handler<Match>() {
            @Override
            public void handle(Match match) {
                Map<String, Object> model = new HashMap<String, Object>();
                model.putAll(match.getPersisFields().toMap());
                model.put("matchId", match.getModelId());
                model.put("$template", "confirmMakeTran.html");
                callback.handle(model);
            }
        });
    }

    @Action(path = "/wc/makeTran")
    public void makeTran(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_MAKE_TRANS);
        request.putString("matchId", matchId);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/makeQuickTran")
    public void makeQuickTran(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcTransfer.CMD_MAKE_TRANS);
        request.putString("matchId", matchId);

        logger.info("send Eventbus: AppConstant.WcTransfer");
        vertx.eventBus().send(AppConstant.WcTransfer, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }


    @Action(path = "/wc/confirmSendZaloMessage")
    public void confirmSendZaloMessage(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }
        Match filter = new Match();
        filter.setModelId(matchId.trim());
        matchDb.findOne(filter, new Handler<Match>() {
            @Override
            public void handle(Match match) {
                Map<String, Object> model = new HashMap<String, Object>();
                model.putAll(match.getPersisFields().toMap());
                model.put("matchId", match.getModelId());
                model.put("$template", "confirmSendZaloMessage.html");
                callback.handle(model);
            }
        });
    }

    @Action(path = "/wc/sendZaloMessage")
    public void sendZaloMessage(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_SEND_ZALO_MESSAGE);
        request.putString("matchId", matchId);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }


    @Action(path = "/wc/cleanThuongThem")
    public void cleanThuongThem(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_CLEAR_THUONG_THEM);
        request.putString("matchId", matchId);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/initThuongThem")
    public void initThuongThem(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_INIT_THUONG_THEM);
        request.putString("matchId", matchId);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/makeThuongThem")
    public void makeThuongThem(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String matchId = params.get("matchId");

        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_MAKE_THUONG_THEM);
        request.putString("matchId", matchId);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }


    @Action(path = "/wc/makeTransferTask")
    public void makeTransferTask(HttpRequestContext context, final Handler<Object> callback) {

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_MAKE_TRANSFER_TASK);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });

    }

    @Action(path = "/wc/testTran")
    public void testTran(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();

        String number = params.get("number");

        if (number == null) {
            callback.handle("Missing matchid params");
            return;
        }

        String amount = params.get("amount");

        if (amount == null) {
            callback.handle("Missing amount params");
            return;
        }

        Integer mNumber = Integer.parseInt(number);
        Long mAmount = Long.parseLong(amount);

        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_TEST_TRAN);
        request.putNumber("number", mNumber);
        request.putNumber("amount", mAmount);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/thuong")
    public void thuong(HttpRequestContext context, final Handler<Object> callback) {
        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_TANG_THUONG);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/test")
    public void test(HttpRequestContext context, final Handler<Object> callback) {
        Notification noti = new Notification();
        noti.receiverNumber = 1263081153;
        noti.caption = "test";   //"Ngày MoMo";
        noti.body = "testtset" ; //"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";
        noti.sms =  "";         //"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.";
        noti.priority = 1;
        noti.time = System.currentTimeMillis();
        noti.tranId = 9999L;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

        vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                logger.info(message.body());
            }
        });
    }

    @Action(path = "/wc/setMessages")
    public void setMessages(HttpRequestContext context, final Handler<Object> callback) {
        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_SET_MESSAGES);

        MultiMap params = context.getRequest().params();


        String giaiTiso = params.get("MESSAGE_GIAI_TI_SO");
        String giaiKetQua = params.get("MESSAGE_GIAI_KET_QUA");
        String giaiTiSoVaKetQua = params.get("MESSAGE_GIAI_TI_SO_VA_KET_QUA");

        if (giaiTiso != null)
            request.putString("MESSAGE_GIAI_TI_SO", giaiTiso);
        if (giaiKetQua != null)
            request.putString("MESSAGE_GIAI_KET_QUA", giaiKetQua);
        if (giaiTiSoVaKetQua != null)
            request.putString("MESSAGE_GIAI_TI_SO_VA_KET_QUA", giaiTiSoVaKetQua);

        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/getMessages")
    public void getMessages(HttpRequestContext context, final Handler<Object> callback) {
        JsonObject request = new JsonObject();
        request.putString("cmd", WcVerticle.CMD_GET_MESSAGES);
        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body());
            }
        });
    }

    @Action(path = "/wc/export")
    public void getExport(final HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        final String matchId = params.get("matchId");
        if (matchId == null) {
            callback.handle("Missing matchid params");
            return;
        }

        final JsonObject request = new JsonObject();
        request.putString("cmd", MongoExportVerticle.CMD_EXPORT_DUDOAN);
        request.putString("matchId", matchId);

        vertx.eventBus().send(AppConstant.MongoExportVerticle, request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                int error = result.body().getInteger("error");
                if (error != 0) {
                    callback.handle(result.body());
                    return;
                }
                //response.setHeader( "Content-Disposition", "filename=" + filename );
                context.getRequest().response().putHeader("Content-Disposition", "filename=" + matchId + ".csv");
                context.getRequest().response().sendFile("/tmp/" + matchId + ".csv");
            }
        });

    }

    @Action(path = "/wc/inviteInfo")
    public void inviteInfo(final HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String phoneNumber = params.get("phoneNumber");

        if (phoneNumber == null) {
            callback.handle("Missing matchid params");
            return;
        }
        final String matchId = params.get("matchId");

        phoneNumber = phoneNumber.trim();
        final Integer fPhoneNumber;
        try {
            fPhoneNumber = Integer.parseInt(phoneNumber);
        } catch (NumberFormatException e) {
            callback.handle("Invalid phone number!");
            return;
        }

        Invitee inviterFilter = new Invitee();
        inviterFilter.setModelId(phoneNumber);
        inviteeDb.findOne(inviterFilter, new Handler<Invitee>() {
            @Override
            public void handle(final Invitee inviter) {
                Invitee inviteeFilter = new Invitee();
                inviteeFilter.inviter = fPhoneNumber;
                inviteeDb.find(inviteeFilter, 0, new Handler<List<Invitee>>() {
                    @Override
                    public void handle(final List<Invitee> invitees) {

                        final StringBuilder builder = new StringBuilder();
                        builder.append("<b>INVITED BY :</b> <br/>");
                        if (inviter == null)
                            builder.append("<empty>");
                        else
                            builder.append(inviter.inviter);
                        builder.append("<br/><br/><b>INVITED :</b> <br/>");

                        final AtomicInteger counter = new AtomicInteger(0);

                        final DuDoan[] duDoans = new DuDoan[invitees.size()];
                        for (int i = 0; i < invitees.size(); i++) {
                            Invitee invitee = invitees.get(i);
                            DuDoan duDoanFilter = new DuDoan();
                            duDoanFilter.setMatchId(matchId);
                            duDoanFilter.setModelId(invitee.getModelId());

                            final int fI = i;
                            duDoanDb.findOne(duDoanFilter, new Handler<DuDoan>() {
                                @Override
                                public void handle(DuDoan duDoan) {
                                    counter.incrementAndGet();
//                                    logger.info("###" + fI  + "|" + duDoan );
                                    duDoans[fI] = duDoan;
                                    if (counter.get() == invitees.size()) {
                                        for (int i = 0; i < invitees.size(); i++) {
                                            builder.append((i + 1) + ". " + invitees.get(i).getModelId());
                                            DuDoan dd = duDoans[i];
                                            if (dd != null) {
                                                builder.append(" : Dự đoán:" + (dd.getResult() == 1 ? "T" : dd.getResult() == 0 ? "H" : "B") + " " + dd.getA() + " " + dd.getB());
                                                if (dd.getsMoney() != null && dd.getsMoney() != 0L)
                                                    builder.append("  . Số tiền nhận: " + dd.getsMoney());
                                            }
                                            builder.append("<br/>");
                                        }
                                        callback.handle(builder.toString());
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });

    }

    @Action(path = "/wc/setZaloStatus")
    public void setZaloStatus(final HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        Integer phone = null;
        try {
            phone = Integer.parseInt(params.get("phone"));
        } catch (NumberFormatException e) {
            phone = null;
        }
        final Integer fPhone = phone;
        if (phone == null) {
            callback.handle("Số điện thoại không hợp lệ");
            return;
        }

        JsonObject query = new JsonObject();
        query.putString("cmd", WcVerticle.CMD_GET_USER_STATUS);
        query.putNumber("number", phone);

        vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                Integer error = result.body().getInteger("error");
                if (error == null || error != 0) {
                    callback.handle(result.body());
                    return;
                }
                Integer status = result.body().getInteger("status");

                if (status == 1) {
                    JsonObject query = new JsonObject();
                    query.putString("cmd", WcVerticle.CMD_SET_USER_STATUS);
                    query.putNumber("number", fPhone);
                    query.putNumber("status", 2);
                    vertx.eventBus().send(AppConstant.WcVerticle_ADDRESS_REQUEST_CMD, query, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            callback.handle("Số điện thoại đã đc thêm thành công!");
//                            callback.handle(event.body());
                        }
                    });
                    return;
                }
                responseStatusDescription(status, callback);
            }
        });
    }

    private void responseStatusDescription(int status, final Handler<Object> callback) {
        switch (status) {
            case -1000:
                callback.handle("-1000: Số điện thoại chưa đăng ký ví điện tử MoMo.");
                break;
            case -1:
                callback.handle("-1: Số điện thoại chưa theo dõi Momo fanpage trên zalo.");
                break;
            case 0:
                callback.handle("0: Số điện thoại chưa đăng ký bên Zalo.");
                break;
            case 1:
                callback.handle("1: Số điện thoại đã theo dõi Momo fanpage.");
                break;
            case 2:
                callback.handle("2: Số điện thoại đã tham gia chương trình Momo Tỷ Phú World Cup 2014.");
                break;
            case 3:
                callback.handle("3: Số điện thoại đã bị khóa.");
                break;
            case 4:
                callback.handle("4: Số điện thoại nằm trong trạng thái .");
                break;
            default:
                callback.handle("error");
        }
    }
}
