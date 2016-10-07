package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by concu on 3/7/16.
 */
public class WebAdminControllerExtend extends WebAdminController {

    private static ArrayList<Integer> arrayListNumber = null;
    private static int numberOfMonthsToDeleteNoti = 0;
    public WebAdminControllerExtend(Vertx vertx, Container container, String STATIC_FILE_DIRECTORY) {
        super(vertx, container, STATIC_FILE_DIRECTORY);
    }

    @Action(path = "/deleteData")
    public void deleteData(HttpRequestContext context, final Handler<String> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "deleteData");
        MultiMap params = request.params();
        final String numberOfMonth = params.get("numberOfMonth") == null ? "" : params.get("numberOfMonth");
        final String object = params.get("object") == null ? "" : params.get("object");
        numberOfMonthsToDeleteNoti = DataUtil.strToInt(numberOfMonth);
        //Get tat ca du lieu bang phone ra
        phonesDb.getAllPhoneDataFromTool(DataUtil.strToInt(object), new Handler<ArrayList<Integer>>() {
            @Override
            public void handle(ArrayList<Integer> listNumbers) {
//                executeDeleteData(listNumbers, callback, DataUtil.strToInt(numberOfMonth));
                arrayListNumber = listNumbers;
                callback.handle(listNumbers.size() + "");

            }
        });
    }

    @Action(path = "/sendDeleteData")
    public void sendDeleteData(HttpRequestContext context, final Handler<String> callback) {
        final HttpServerRequest request = context.getRequest();
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("func", "sendDeleteData");
        MultiMap params = request.params();
        final int begin = params.get("begin") == null ? 0 : DataUtil.strToInt(params.get("begin"));
        final int end = params.get("end") == null ? 0 : DataUtil.strToInt(params.get("end"));

        List<Integer> listNumberSub = arrayListNumber.subList(begin, end);

        executeDeleteData(listNumberSub, callback, numberOfMonthsToDeleteNoti, begin);
    }


    private void executeDeleteData(final List<Integer> listNumbers, final Handler<String> callback, final int numberOfMonth, final int end)
    {

        if(listNumbers.size() == 0)
        {
            callback.handle(end + "");
            return;
        }
        int number = listNumbers.get(0);
        listNumbers.remove(0);
        String collectName = "noti_" + number;
        long time = System.currentTimeMillis() - numberOfMonth * 30 * 24 * 60 * 60 * 1000L;
        logger.info("delete noti data " + number);
        notificationDb.removeOldRecs(collectName, time, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {
                executeDeleteData(listNumbers, callback, numberOfMonth, end);
            }
        });
    }

}
