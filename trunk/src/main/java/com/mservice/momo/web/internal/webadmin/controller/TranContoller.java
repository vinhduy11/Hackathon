package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.gateway.internal.core.objects.Command;
import com.mservice.momo.gateway.internal.core.objects.Request;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by locnguyen on 18/07/2014.
 */
public class TranContoller {
    private Vertx vertx;

    private Logger logger;


    public TranContoller(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
    }

    @Action(path = "/tran/do")
    public void transaction(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();

        long id = DataUtil.stringToUNumber(params.get("id"));

        if (id < 1 || !WebAdminController.listUser.containsKey(id)
                || WebAdminController.listUser.get(id).lastActTime + 60000 < System.currentTimeMillis()){
            callback.handle(
                    new JsonObject()
                            .putString("error", "-1")
                            .putString("desc", "Must login")
            );
            return;
        }

        String username = "";
        if (params.get("username") == null || "".equalsIgnoreCase(params.get("username")))
        {
            callback.handle(
                    new JsonObject()
                            .putString("error", "-1")
                            .putString("desc", "Must login")
            );
            return;
        }
        username = params.get("username");

        final Common.BuildLog log = new Common.BuildLog(logger);

        log.add("username",username);

        String action = "";

        if (params.get("action") != null) {
            action = params.get("action");
        }

        log.add("action", action);

        if ("commit".equalsIgnoreCase(action)) {
            rollback_commit(Command.COMMIT, context, callback, log);
        } else if ("rollback".equalsIgnoreCase(action)) {
            rollback_commit(Command.ROLLBACK, context, callback, log);
        } else if ("adjust".equalsIgnoreCase(action)) {
            adjust(context, callback, log);
        } else {
            log.add("error", -100);
            log.add("decs", "Not support action " + action);

            JsonObject response = new JsonObject();
            response.putNumber("error", -10);
            response.putString("desc", "Not support action " + action);

            log.writeLog();
            callback.handle(response);
            return;
        }

    }



    @Action(path = "/tran/doForConnector")
    public void transDoForConnector(HttpRequestContext context, final Handler<JsonObject> callback) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        String data = context.postData;
        log.add("data", data);

        if("".equalsIgnoreCase(data)){
            return;
        }

        JsonObject joRequest = new JsonObject(context.postData);
        String action = joRequest.getString("action","");
        long tranid = joRequest.getLong("tranid",0);
        int wallet = joRequest.getInteger("wallet",1);

        log.setPhoneNumber("" + tranid);
        log.add("action", action);

        final Request reqObj = new Request();

        if ("commit".equalsIgnoreCase(action)) {
            reqObj.TYPE = Command.COMMIT;
            reqObj.TRAN_ID = tranid;
            reqObj.WALLET = wallet;

        } else if ("rollback".equalsIgnoreCase(action)) {
            reqObj.TYPE = Command.ROLLBACK;
            reqObj.TRAN_ID = tranid;
            reqObj.WALLET = wallet;

            //rollback_commit(Command.ROLLBACK, context, callback, log);

        } else {
            log.add("error", -100);
            log.add("decs", "Not support action " + action);

            JsonObject response = new JsonObject();
            response.putNumber("error", -10);
            response.putString("desc", "Not support action " + action);

            log.writeLog();
            callback.handle(response);
            return;
        }

    }

    private void rollback_commit(final String action, final HttpRequestContext request, final Handler<JsonObject> callback, final Common.BuildLog log) {
        MultiMap params = request.getRequest().params();
        final JsonObject response = new JsonObject();

        String phone = "";
        if (params.get("phone") != null) {
            phone = params.get("phone");
            log.setPhoneNumber(phone);
        }

        long tranId = 0;
        if (params.get("tranid") == null || "".equalsIgnoreCase(params.get("tranid"))
                || (tranId = DataUtil.stringToUNumber(params.get("tranid"))) <= 0) {
            log.add("error", -100);
            log.add("decs", "Input invalid");

            log.writeLog();

            response.putNumber("error", -100);
            response.putString("desc", "Input invalid");
            callback.handle(response);

            return;
        }

        log.add("tranID", tranId + "");

        Request reqObj = new Request();
        //CoreConnectorVerticle.Obj reqObj = new CoreConnectorVerticle.Obj();
        reqObj.TYPE = action;
        reqObj.TRAN_ID = tranId;
        reqObj.WALLET = WalletType.MOMO;

        doAction(reqObj, callback, log);

    }

    public void adjust(final HttpRequestContext request, final Handler<JsonObject> callback, final Common.BuildLog log) {
        MultiMap params = request.getRequest().params();

        String sid = "";
        String did = "";
        long amount = 0;
        if (params.get("sid") != null) {
            sid = params.get("sid");
            log.setPhoneNumber(sid);
            log.add("source phone", sid);
        }
        if (params.get("did") != null) {
            did = params.get("did");
            log.add("destination phone", did);
        }
        if (params.get("amount") != null) {
            amount = DataUtil.stringToUNumber(params.get("amount"));
            log.add("amount", amount);
        }

        JsonObject response = new JsonObject();

        if ("".equalsIgnoreCase(sid) || "".equalsIgnoreCase(did) || amount <= 0) {
            log.add("error", -100);
            log.add("description", "Input invalid");
            log.writeLog();

            response.putNumber("error", -100);
            response.putString("desc", "Input invalid");
            callback.handle(response);
            return;
        }

        Request reqObj = new Request();
        reqObj.TYPE = Command.ADJUST;
        reqObj.ADJUST_TYP = "wc";
        reqObj.SENDER_NUM = sid;
        reqObj.RECVER_NUM = did;
        reqObj.TRAN_AMOUNT = amount;

        doAction(reqObj, callback, log);

    }

    private void doAction(final Request reqObj, final Handler<JsonObject> callback, final Common.BuildLog log) {

        log.add("begin request action ", reqObj.TYPE);
        log.add("requested json", reqObj.toJsonObject());

        vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, reqObj.toJsonObject(), new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {

                final MomoMessage reply = MomoMessage.fromBuffer(message.body());
                Core.StandardReply rpl;
                int result = -100;

                JsonObject response = new JsonObject();

                try {
                    rpl = Core.StandardReply.parseFrom(reply.cmdBody);
                } catch (Exception ex) {
                    rpl = null;
                }

                //lock tien khong thanh cong
                if ((rpl == null)) {
                    log.add("Core.StandardReply", "null");

                    response.putNumber("error", -1);
                    response.putString("desc", "Core.StandardReply is null");

                    callback.handle(response);

                    log.writeLog();
                    return;
                }
                result = rpl.getErrorCode();

                log.add("error", result);
                log.add("tid", rpl.getTid());
                log.add("description", (rpl.getDescription() == null ? "" : rpl.getDescription()));

                if (Command.ROLLBACK.equalsIgnoreCase(reqObj.TYPE) && result == 103) {
                    result = 0;
                }

                response.putNumber("error", result);
                response.putNumber("tid", rpl.getTid());
                response.putString("desc", (rpl.getDescription() == null ? "" : "".equalsIgnoreCase(rpl.getDescription()) ? "Successful" : rpl.getDescription()));

                log.writeLog();

                callback.handle(response);
            }
        });
    }


}
