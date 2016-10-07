package com.mservice.momo.web.internal.webadmin.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.msg.StatisticModels;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created by nam on 4/29/14.
 */
public class Statistic {

    private Vertx vertx;

    public Statistic(Vertx vertx) {
        this.vertx = vertx;
    }

    public long getLong(Map<String, String> map, String fieldName) {
        String startDateRaw = map.get("fieldName");
        try {
            return Long.parseLong(startDateRaw);
        } catch (NumberFormatException e) {
        }
        return 0L;
    }


    @Action(path = "/service/statistic/online")
    public void online(HttpRequestContext context, final Handler<Object> callback) {
        final JsonObject obj = new JsonObject();

        long rawStartDate = getLong(context.getPostParams(), "startDate");
//        if (rawStartDate == 0L) {
//            rawStartDate = StatisticVerticle.getCurrentTime();
//        }
//        final long startDate = rawStartDate;
//        final long endDate = getLong(context.getPostParams(), "endDate");
//        getNumber(StatisticModels.ActionType.WEB_USER, startDate, endDate, new Handler<Long>() {
//            @Override
//            public void handle(Long webUser) {
//                obj.putNumber("WEB_USER", webUser);
//                getNumber(StatisticModels.ActionType.MOBILE_USER, startDate, endDate, new Handler<Long>() {
//                    @Override
//                    public void handle(Long mobileUser) {
//                        obj.putNumber("MOBILE_USER", mobileUser);
//                        callback.handle(obj);
//                    }
//                });
//            }
//        });
    }

    @Action(path = "/service/statistic/simple")
    public void logout(HttpRequestContext context, final Handler<Object> callback) {
        final JsonObject obj = new JsonObject();

        long rawStartDate = getLong(context.getPostParams(), "startDate");
//        if (rawStartDate == 0L) {
//            rawStartDate = StatisticVerticle.getCurrentTime();
//        }
//        final long startDate = rawStartDate;
//        final long endDate = getLong(context.getPostParams(), "endDate");
//        getNumber(StatisticModels.ActionType.TRANS, startDate, endDate, new Handler<Long>() {
//            @Override
//            public void handle(Long trans) {
//                obj.putNumber("TRANS", trans);
//                getNumber(StatisticModels.ActionType.TRANS_SUCCESS, startDate, endDate, new Handler<Long>() {
//                    @Override
//                    public void handle(Long trans) {
//                        obj.putNumber("TRANS_SUCCESS", trans);
//                        getNumber(StatisticModels.ActionType.TRANS_M2M, startDate, endDate, new Handler<Long>() {
//                            @Override
//                            public void handle(Long trans) {
//                                obj.putNumber("TRANS_M2M", trans);
//                                getNumber(StatisticModels.ActionType.TRANS_M2M_SUCCESS, startDate, endDate, new Handler<Long>() {
//                                    @Override
//                                    public void handle(Long trans) {
//                                        obj.putNumber("TRANS_M2M_SUCCESS", trans);
//                                        getNumber(StatisticModels.ActionType.TRANS_TOP_UP, startDate, endDate, new Handler<Long>() {
//                                            @Override
//                                            public void handle(Long trans) {
//                                                obj.putNumber("TRANS_TOP_UP", trans);
//                                                getNumber(StatisticModels.ActionType.TRANS_TOP_UP_SUCCESS, startDate, endDate, new Handler<Long>() {
//                                                    @Override
//                                                    public void handle(Long trans) {
//                                                        obj.putNumber("TRANS_TOP_UP_SUCCESS", trans);
//
//                                                        callback.handle(obj);
//                                                    }
//                                                });
//                                            }
//                                        });
//                                    }
//                                });
//                            }
//                        });
//                    }
//                });
//            }
//        });
    }

    public void getNumber(StatisticModels.ActionType type, long startDate, long endDate, final Handler<Long> handler) {
        StatisticModels.GetNumber query = StatisticModels.GetNumber.newBuilder()
                .setType(type)
                .setStartDate(startDate)
                .setEndDate(endDate)
                .build();
        vertx.eventBus().send(AppConstant.StatisticVerticle_ADDRESS_GET_NUMBER, query.toByteArray(), new Handler<Message<byte[]>>() {
            @Override
            public void handle(Message<byte[]> event) {
                long result = 0L;
                byte[] array = event.body();
                try {
                    StatisticModels.GetNumberReply reply = StatisticModels.GetNumberReply.parseFrom(array);
                    result = reply.getNumber();
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                } finally {
                    handler.handle(result);
                }
            }
        });
    }
}
