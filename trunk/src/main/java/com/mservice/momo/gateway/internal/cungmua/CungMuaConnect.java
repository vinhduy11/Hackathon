package com.mservice.momo.gateway.internal.cungmua;

import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duy.huynh on 08/06/2015.
 */
public class CungMuaConnect {

    public static void getCity(final Vertx vertx,
                               final SockData data,
                               final String busAddress,
                               final String phoneNumber,
                               final String channel,
                               final Common.BuildLog log,
                               final Handler<CungMuaResponse> callback) {

        log.add("func", "getCityList");
        log.add("number", phoneNumber);
        log.add("channel", channel);

        // Create CungMua request
        final CungMuaRequest cungMuaRequest = new CungMuaRequest();
        // TODO: init cungMuaRequest

        // Send to connector
        vertx.eventBus().sendWithTimeout(busAddress, cungMuaRequest.getJsonObject(), 450000,
                new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> asyncResult) {

                        CungMuaResponse cungMuaResponse = null;

                        if (asyncResult.succeeded()) {
                            log.add("desc", "success");
                            cungMuaResponse = new CungMuaResponse();
                            // TODO: parse cungMuaResponse
                            List<City> cityList = new ArrayList<City>();
                            cityList.add(new City(1, "Hà Nội"));
                            cityList.add(new City(2, "TP HCM"));
                            cungMuaResponse.cities = cityList;
                        } else {
                            log.add("desc", "failed");
                        }

                        callback.handle(cungMuaResponse);
                    }
                });
    }


    public static void getCategoryByCity(final Vertx vertx,
                                         final SockData data,
                                         final String busAddress,
                                         final String phoneNumber,
                                         final String channel,
                                         final int cityId,
                                         final Common.BuildLog log,
                                         final Handler<CungMuaResponse> callback) {

        log.add("func", "getCategoryByCity");
        log.add("number", phoneNumber);
        log.add("channel", channel);
        log.add("cityId", cityId);

        // Create CungMua request
        final CungMuaRequest cungMuaRequest = new CungMuaRequest();
        cungMuaRequest.cityId = cityId;
        // TODO: init cungMuaRequest

        vertx.eventBus().sendWithTimeout(busAddress, cungMuaRequest, 450000,
                new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> asyncResult) {

                        CungMuaResponse cungMuaResponse = null;

                        if (asyncResult.succeeded()) {
                            log.add("desc", "success");
                            cungMuaResponse = new CungMuaResponse();
                            // TODO: parse cungMuaResponse
                            List<Category> categories = null;
                            categories = new ArrayList<Category>();
                            categories.add(new Category(1, "Ẩm thực", cityId));
                            categories.add(new Category(2, "Làm đẹp", cityId));
                            categories.add(new Category(2, "Thời trang", cityId));
                            cungMuaResponse.categories = categories;
                        } else {
                            log.add("desc", "failed " + asyncResult.cause().getMessage());
                        }
                        callback.handle(cungMuaResponse);
                    }
                });
    }

    public static void createOrder(final Vertx vertx,
                                   final SockData data,
                                   final String busAddress,
                                   final String phoneNumber,
                                   final String channel,
                                   final Common.BuildLog log,
                                   final Handler<CungMuaResponse> callback) {

        log.add("func", "createOrder");
        log.add("number", phoneNumber);
        log.add("channel", channel);

        // Create CungMua request
        final CungMuaRequest cungMuaRequest = new CungMuaRequest();
        // TODO: init cungMuaRequest

        vertx.eventBus().sendWithTimeout(busAddress, cungMuaRequest, 450000,
                new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> asyncResult) {

                        CungMuaResponse cungMuaResponse = null;

                        if (asyncResult.succeeded()) {
                            log.add("desc", "success");
                            cungMuaResponse = new CungMuaResponse();
                            // TODO
                            cungMuaResponse.transId = "11111";
                        } else {
                            log.add("desc", "failed " + asyncResult.cause().getMessage());
                        }
                        callback.handle(cungMuaResponse);
                    }
                });
    }

    public static void payBill(final Vertx vertx,
                               final SockData data,
                               final String busAddress,
                               final String phoneNumber,
                               final String channel,
                               final String accountId,
                               final long amount,
                               final String transId,
                               final String paymentDate,
                               final Common.BuildLog log,
                               final Handler<CungMuaResponse> callback) {

        log.add("func", "payBill");
        log.add("number", phoneNumber);
        log.add("channel", channel);

        // Create CungMua request
        final CungMuaRequest cungMuaRequest = new CungMuaRequest();
        cungMuaRequest.accountId = accountId;
        cungMuaRequest.amount = amount;
        cungMuaRequest.transId = transId;
        cungMuaRequest.paymentDate = paymentDate;
        // TODO: init cungMuaRequest

        vertx.eventBus().sendWithTimeout(busAddress, cungMuaRequest, 450000,
                new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> asyncResult) {

                        CungMuaResponse cungMuaResponse = null;

                        if (asyncResult.succeeded()) {
                            log.add("desc", "success");
                            cungMuaResponse = new CungMuaResponse();
                            // TODO
                            cungMuaResponse.transId = "11111";
                            cungMuaResponse.partnerTransId = "11111";
                        } else {
                            log.add("desc", "failed " + asyncResult.cause().getMessage());
                        }
                        callback.handle(cungMuaResponse);
                    }
                });
    }

    public static void takeEvoucherDeal(final Vertx vertx,
                                        final SockData data,
                                        final String busAddress,
                                        final String phoneNumber,
                                        final String channel,
                                        final int categoryId,
                                        final int cityId,
                                        final int count,
                                        final Common.BuildLog log,
                                        final Handler<CungMuaResponse> callback) {

        log.add("func", "takeEvoucherDeal");
        log.add("number", phoneNumber);
        log.add("channel", channel);

        // Create CungMua request
        final CungMuaRequest cungMuaRequest = new CungMuaRequest();
        cungMuaRequest.categoryId = categoryId;
        cungMuaRequest.cityId = cityId;
        cungMuaRequest.count = count;
        // TODO: init cungMuaRequest

        vertx.eventBus().sendWithTimeout(busAddress, cungMuaRequest, 450000,
                new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> asyncResult) {

                        CungMuaResponse cungMuaResponse = null;

                        if (asyncResult.succeeded()) {
                            log.add("desc", "success");
                            cungMuaResponse = new CungMuaResponse();
                            // TODO
                            cungMuaResponse.data = new JsonObject();
                            cungMuaResponse.pager = new JsonObject();
                        } else {
                            log.add("desc", "failed " + asyncResult.cause().getMessage());
                        }
                        callback.handle(cungMuaResponse);
                    }
                });
    }

    public static void takeDealDetail(final Vertx vertx,
                                      final SockData data,
                                      final String busAddress,
                                      final String phoneNumber,
                                      final String channel,
                                      final String dealId,
                                      final Common.BuildLog log,
                                      final Handler<CungMuaResponse> callback) {

        log.add("func", "takeDealDetail");
        log.add("number", phoneNumber);
        log.add("channel", channel);

        // Create CungMua request
        final CungMuaRequest cungMuaRequest = new CungMuaRequest();
        cungMuaRequest.dealId = dealId;
        // TODO: init cungMuaRequest

        vertx.eventBus().sendWithTimeout(busAddress, cungMuaRequest, 450000,
                new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> asyncResult) {

                        CungMuaResponse cungMuaResponse = null;

                        if (asyncResult.succeeded()) {
                            log.add("desc", "success");
                            cungMuaResponse = new CungMuaResponse();
                            // TODO
                            cungMuaResponse.data = new JsonObject();
                        } else {
                            log.add("desc", "failed " + asyncResult.cause().getMessage());
                        }
                        callback.handle(cungMuaResponse);
                    }
                });
    }

}
