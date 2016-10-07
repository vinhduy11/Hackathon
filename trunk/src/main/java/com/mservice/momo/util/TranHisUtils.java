package com.mservice.momo.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.ConfigVerticle;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;


/**
 * Created by nam on 5/22/14.
 */
public class TranHisUtils {

    public static final int STATUS_ON = 1;
    public static void getTranStatusStatus(Vertx vertx, int tranType, final Handler<Integer> callback) {
        JsonObject query = new JsonObject();
        query.putString("cmd", ConfigVerticle.CMD_GET_TRANSFER_CONFIG);
        query.putNumber("tranType", tranType);
        vertx.eventBus().send(AppConstant.ConfigVerticle, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                int status = message.body().getInteger("status");
                callback.handle(status);
            }
        });
    }

    public static void isTranStatusOn(Vertx vertx, int tranType, final Handler<Boolean> callback) {
        getTranStatusStatus(vertx, tranType, new Handler<Integer>() {
            @Override
            public void handle(Integer status) {
                callback.handle(status == STATUS_ON);
            }
        });
    }

    public static String getChannel(NetSocket sock)
    {
        String channel = sock == null ? Const.CHANNEL_WEB : Const.CHANNEL_MOBI;
        return channel;
    }

    public static MomoProto.TranHisV1 getRequest(MomoMessage msg, Logger logger) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }
        return request;
    }

    public static void addMoreTransferInfoTranRpl(JsonObject tranRpl,
                                                  long ackTime,
                                                  int tranType,
                                                  int in_out,
                                                  int category,
                                                  String partnerId,
                                                  String partnerCode,
                                                  String partnerName,
                                                  long amount,
                                                  int fromSource) {
        tranRpl.putNumber(colName.TranDBCols.ACK_TIME, ackTime);
        tranRpl.putNumber(colName.TranDBCols.TRAN_TYPE, tranType);
        tranRpl.putNumber(colName.TranDBCols.IO, in_out);
        tranRpl.putNumber(colName.TranDBCols.CATEGORY, category);
        tranRpl.putString(colName.TranDBCols.PARTNER_ID, partnerId);
        tranRpl.putString(colName.TranDBCols.PARTNER_CODE, partnerCode);
        tranRpl.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
        tranRpl.putNumber(colName.TranDBCols.AMOUNT, amount);
        tranRpl.putNumber(colName.TranDBCols.FROM_SOURCE, fromSource);
    }
}
