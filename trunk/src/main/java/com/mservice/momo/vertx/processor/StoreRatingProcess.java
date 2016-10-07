package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.rate.StoreCommentDb;
import com.mservice.momo.data.rate.StoreStarDb;
import com.mservice.momo.data.rate.StoreWarningDb;
import com.mservice.momo.data.rate.StoreWarningTypeDb;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.rate.*;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import com.mservice.momo.vertx.rate.StoreRateManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Container;

import java.util.Collection;
import java.util.List;

/**
 * Created by nam on 9/13/14.
 */
public class StoreRatingProcess {
    public static final int DEFAULT_COMMENT_PAGE_SIZE = 10;

    private Vertx vertx;
    private Container container;

    private Logger logger;
    private Common mCom;
    private StoreRateManager storeRateManager;
    private StoreStarDb storeStarDb;
    private StoreCommentDb storeCommentDb;

    private StoreWarningDb storeWarningDb;

    public StoreRatingProcess(final Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
        this.logger = container.logger();

        storeRateManager = new StoreRateManager(vertx, container);
        storeStarDb = new StoreStarDb(vertx, container.logger());
        storeCommentDb = new StoreCommentDb(vertx, container.logger());
        storeWarningDb = new StoreWarningDb(vertx, container.logger());
        mCom = new Common(vertx,container.logger(), container.config());

        final StoreWarningTypeDb storeWarningTypeDb = new StoreWarningTypeDb(vertx, container.logger());
        storeWarningTypeDb.find(null, 0, new Handler<List<StoreWarningType>>() {
            @Override
            public void handle(List<StoreWarningType> storeWarningTypes) {
                if (storeWarningTypes.size() == 0) {
                    StoreWarningType swt;
                    swt = new StoreWarningType(1, "Điểm giao dịch này không tồn tại");
                    storeWarningTypeDb.save(swt, null);
                    swt = new StoreWarningType(2, "Địa chỉ không chính xác");
                    storeWarningTypeDb.save(swt, null);
                    swt = new StoreWarningType(3, "Không hỗ trợ tôi định danh");
                    storeWarningTypeDb.save(swt, null);
                    swt = new StoreWarningType(4, "Không hỗ trợ tôi chuyển tiền");
                    storeWarningTypeDb.save(swt, null);
                    swt = new StoreWarningType(5, "Không hỗ trợ tôi thanh toán hóa đơn");
                    storeWarningTypeDb.save(swt, null);
                    swt = new StoreWarningType(6, "Không hỗ trợ tôi thanh toán dịch vụ");
                    storeWarningTypeDb.save(swt, null);
                }
            }
        });
    }


    public void commentCrud(final NetSocket sock, final MomoMessage msg, final SockData data, Handler<JsonObject> callback) {
        MomoProto.StoreCommentCrud request;
        try {
            request = MomoProto.StoreCommentCrud.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        MomoProto.StoreComment rComment = request.getComment();

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone);

        log.add("request.getCmd", request.getCmd());
        log.add("comment", rComment);

        switch (request.getCmd()) {
            case MomoProto.StoreCommentCrud.CrudCmd.CMD_CREATE_VALUE:
            case MomoProto.StoreCommentCrud.CrudCmd.CMD_UPDATE_VALUE:
                StoreComment c = new StoreComment(rComment, msg.cmdPhone, data.getPhoneObj().name);
                storeRateManager.upsertComment(c, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject r) {
                        log.add("result", r);
                        log.writeLog();
                        responseCrudCmd(sock, msg, r.getInteger("error", -100), r.getString("desc", ""), r.getString("commentId", ""));
                    }
                });
                break;
            case MomoProto.StoreCommentCrud.CrudCmd.CMD_DELETE_VALUE:
                storeRateManager.removeComment(rComment.getCommentId(), msg.cmdPhone, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject r) {
                        log.add("result", r);
                        log.writeLog();
                        responseCrudCmd(sock, msg, r.getInteger("error", -100), r.getString("desc", ""), r.getString("commentId", ""));
                    }
                });
                break;
            case MomoProto.StoreCommentCrud.CrudCmd.CMD_READ_VALUE:
                log.add("result", "Unsupported CommentID!");
                log.writeLog();
                responseCrudCmd(sock, msg, -1, "Unsupported CommentID!", "");
                break;
            default:
                log.add("result", "Unsupported CommentID!");
                log.writeLog();
                responseCrudCmd(sock, msg, -1, "Invalid crud command value!", "");
        }
    }

    public void responseCrudCmd(NetSocket sock, final MomoMessage msg, int error, String desc, String commentId) {
        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.STORE_COMMENT_CRUD_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                MomoProto.StoreCommentCrudReply.newBuilder()
                        .setError(error)
                        .setDesc(desc)
                        .setCommentId(commentId)
                        .build().toByteArray()
        );
        mCom.writeDataToSocket(sock, buf);
    }

    public void getStoreRate(final NetSocket sock, final MomoMessage msg, Object o) {
        MomoProto.GetStoreRate request;
        try {
            request = MomoProto.GetStoreRate.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        List<Integer> storeIds = request.getStoreIdsList();

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone);
        log.add("Command", "GetStoreRateInfo");
        log.add("storeIds", storeIds);

        storeStarDb.find(storeIds, new Handler<List<StoreStar>>() {
            @Override
            public void handle(List<StoreStar> storeStars) {

                log.add("result", storeStars);
                log.writeLog();

                MomoProto.GetStoreRateReply.Builder storeRateReply = MomoProto.GetStoreRateReply.newBuilder();

                for (StoreStar store : storeStars) {
                    storeRateReply.addRateInfos(store.toMomoProto());
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.GET_STORE_RATE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        storeRateReply.build().toByteArray()
                );
                mCom.writeDataToSocket(sock, buf);
            }
        });

    }

    public void getStoreCommentPage(final NetSocket sock, final MomoMessage msg, Object o) {
        MomoProto.GetStoreCommentPage request;
        try {
            request = MomoProto.GetStoreCommentPage.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        long lowestTime = request.getLowestTime();
        if (lowestTime == 0)
            lowestTime = Long.MAX_VALUE;

        int filterPhone = request.getCommenterPhone();

        final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone);
        log.add("Command", "getStoreCommentPage");
        log.add("storeId", request.getStoreId());
        log.add("filterPhone", filterPhone);
        log.add("lowestTime", lowestTime);

        List<Integer> status = null;
//        if (filterPhone == 0) {
//            status = new ArrayList<>();
//            status.add(StoreComment.STATUS_ACCEPTED);
//        }

        storeCommentDb.getCommentPage(DEFAULT_COMMENT_PAGE_SIZE, request.getStoreId(), filterPhone, lowestTime, status, new Handler<List<StoreComment>>() {
            @Override
            public void handle(List<StoreComment> storeComments) {
                log.add("Number comment return", storeComments.size());
                log.writeLog();

                MomoProto.GetStoreCommentPageReply.Builder builder = MomoProto.GetStoreCommentPageReply.newBuilder();

                for (StoreComment comment : storeComments) {
                    MomoProto.StoreComment.Builder c = comment.toMomoProtoBuilder();
                    builder.addComments(c.build());
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.GET_STORE_COMMENT_PAGE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build().toByteArray()
                );
                mCom.writeDataToSocket(sock, buf);
            }
        });
    }

    public void getStoreWarningTypes(final NetSocket sock, final MomoMessage msg, final Handler<JsonObject> callback) {
        MomoProto.GetStoreWarningType request;
        try {
            request = MomoProto.GetStoreWarningType.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        storeRateManager.getStoreWarningTypes(new Handler<Collection<StoreWarningType>>() {
            @Override
            public void handle(Collection<StoreWarningType> storeWarningTypes) {
                MomoProto.GetStoreWarningTypeReply.Builder builder = MomoProto.GetStoreWarningTypeReply.newBuilder();

                final Common.BuildLog log = new Common.BuildLog(logger, msg.cmdPhone);
                log.add("Method", "getStoreWarningTypes");
                log.add("warningType number", storeWarningTypes.size());
                log.writeLog();
                for (StoreWarningType type : storeWarningTypes) {
                    builder.addTypes(type.toMomoProtoBuilder().build());
                }

                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.GET_STORE_WARNING_TYPE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build().toByteArray()
                );
                mCom.writeDataToSocket(sock, buf);
            }
        });
    }

    public void warnStore(final NetSocket sock, final MomoMessage msg, final Handler<JsonObject> callback) {
        MomoProto.WarnStore request;
        try {
            request = MomoProto.WarnStore.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            request = null;
        }

        if (request == null) {
            mCom.writeErrorToSocket(sock);
            return;
        }

        StoreWarning storeWarning = new StoreWarning(request, msg.cmdPhone);

        StoreWarningType warningType = storeRateManager.getStoreWarningType(storeWarning.warningType);
        if (warningType == null) {
            MomoProto.WarnStoreReply.Builder builder = MomoProto.WarnStoreReply.newBuilder()
                    .setError(1)
                    .setDesc("invalid warning type");
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.WARN_STORE_REPLY_VALUE,
                    msg.cmdIndex,
                    msg.cmdPhone,
                    builder.build().toByteArray()
            );
            mCom.writeDataToSocket(sock, buf);
            return;
        }


        storeWarningDb.save(storeWarning, new Handler<String>() {
            @Override
            public void handle(String s) {
                MomoProto.WarnStoreReply.Builder builder = MomoProto.WarnStoreReply.newBuilder()
                        .setError(0)
                        .setDesc("success");
                Buffer buf = MomoMessage.buildBuffer(
                        MomoProto.MsgType.WARN_STORE_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.build().toByteArray()
                );
                mCom.writeDataToSocket(sock, buf);
            }
        });


        // insert to core;
        CoreAgentRating r = new CoreAgentRating(storeWarning, warningType.name);
        JsonObject coreRequest = new JsonObject();
        coreRequest.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.STORE_RATING);
        coreRequest.putObject("model", r.toJsonObject());
        vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS, coreRequest);
    }
}
