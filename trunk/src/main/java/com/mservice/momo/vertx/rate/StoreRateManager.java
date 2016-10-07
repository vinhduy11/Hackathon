package com.mservice.momo.vertx.rate;

import com.mservice.momo.data.rate.StoreCommentDb;
import com.mservice.momo.data.rate.StoreStarDb;
import com.mservice.momo.data.rate.StoreWarningTypeDb;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.rate.*;
import com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nam on 9/11/14.
 */
public class StoreRateManager {
    public static int STORE_PAGE_SIZE = 10;

    private Vertx vertx;
    private Container container;

    private Logger logger;

    private StoreStarDb storeStarDb;
    private StoreCommentDb storeCommentDb;
    private StoreWarningTypeDb storeWarningTypeDb;

    private Map<Integer, StoreWarningType> warningTypeMap;

    public StoreRateManager(final Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;

        this.logger = container.logger();

        storeStarDb = new StoreStarDb(vertx, container.logger());
        storeCommentDb = new StoreCommentDb(vertx, container.logger());
        storeWarningTypeDb = new StoreWarningTypeDb(vertx, container.logger());

        warningTypeMap = new HashMap<>();
        loadWarningType();
    }

    private void loadWarningType () {
        storeWarningTypeDb.find(null, 0, new Handler<List<StoreWarningType>>() {
            @Override
            public void handle(List<StoreWarningType> storeWarningTypes) {
                for (StoreWarningType type : storeWarningTypes) {
                    warningTypeMap.put(type.typeId, type);
                }
            }
        });
    }

    /**
     * Nếu noti tòn tại và accepted thì bỏ sao và update lại
     * Nếu noti chưa tồn tại thì save.
     *
     * @param newComment
     * @param callback
     */
    public void upsertComment(final StoreComment newComment, final Handler<JsonObject> callback) {
        if (newComment.isValid()) {
            callback.handle(new JsonObject()
                    .putNumber("error", -1)
                    .putString("desc", "Comment's params is invalid."));
        }

        StoreComment filter = new StoreComment();
        filter.storeId = newComment.storeId;
        filter.commenter = newComment.commenter;
        storeCommentDb.findOne(filter, new Handler<StoreComment>() {
            @Override
            public void handle(StoreComment oldComment) {
                if (oldComment != null) { // existed;
                    if(oldComment.status == StoreComment.STATUS_ACCEPTED) {
                        StoreStar storeStarfilter = new StoreStar();
                        storeStarfilter.storeId = oldComment.storeId;

                        StoreStar increaseValues = new StoreStar();
                        increaseValues.increaseStar(oldComment.star, -1);
                        storeStarDb.increase(storeStarfilter, increaseValues, true, new Handler<Long>() {
                            @Override
                            public void handle(Long count) {
                            }
                        });

                    }

                    newComment.setModelId(oldComment.getModelId());
                    newComment.status = StoreComment.STATUS_NEW;  // make sure this comment must be re-reviewed.
                    storeCommentDb.update(newComment, false, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            callback.handle(new JsonObject()
                                    .putNumber("error", 0)
                                    .putString("action", "update"));
                        }
                    });
                    return;
                }
                // doesn't exist
                //TODO:
                storeCommentDb.save(newComment, new Handler<String>() {
                    @Override
                    public void handle(String s) {
                        callback.handle(
                                new JsonObject()
                                        .putNumber("error", 0)
                                        .putString("commentId", s)
                        );
                    }
                });
            }
        });

        // insert to core;
        CoreAgentRating r = new CoreAgentRating(newComment);
        JsonObject request = new JsonObject();
        request.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.STORE_RATING);
        request.putObject("model", r.toJsonObject());
        vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS, request);
    }

    public void setCommentStatus(String commentId, final int newStatus, final Handler<JsonObject> callback) {
        StoreComment filter = new StoreComment();
        filter.setModelId(commentId);
        storeCommentDb.findOne(filter, new Handler<StoreComment>() {
            @Override
            public void handle(StoreComment c) {
                if(newStatus == c.status) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", 0)
                                    .putString("desc", "Success")
                    );
                }

                int increaseStar = 0;
                if(newStatus == StoreComment.STATUS_ACCEPTED && c.status != StoreComment.STATUS_ACCEPTED) {
                    increaseStar = 1;
                }
                if(newStatus != StoreComment.STATUS_ACCEPTED && c.status == StoreComment.STATUS_ACCEPTED) {
                    increaseStar = -1;
                }

                c.status = newStatus;
                storeCommentDb.update(c, false, new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean aBoolean) {
                        callback.handle(
                                new JsonObject()
                                        .putNumber("error", 0)
                                        .putString("desc", "Success")
                        );
                    }
                });

                if(increaseStar !=0) {
                    StoreStar storeStarfilter = new StoreStar();
                    storeStarfilter.storeId = c.storeId;

                    StoreStar increaseValues = new StoreStar();
                    increaseValues.increaseStar(c.star, increaseStar);

                    storeStarDb.increase(storeStarfilter, increaseValues, true, null);
                }

            }
        });
    }

    public void removeComment(final String commentId, int phoneNumber, final Handler<JsonObject> callback) {
        StoreComment filter = new StoreComment();
        filter.setModelId(commentId);
        filter.commenter = phoneNumber;
        storeCommentDb.findOne(filter, new Handler<StoreComment>() {
            @Override
            public void handle(final StoreComment oldComment) {
                if (oldComment == null) {
                    callback.handle(
                            new JsonObject()
                                    .putNumber("error", 0)
                                    .putBoolean("deleted", false)
                    );
                }

                StoreStar storeStarfilter = new StoreStar();
                storeStarfilter.storeId = oldComment.storeId;

                StoreStar increaseValues = new StoreStar();
                increaseValues.increaseStar(oldComment.star, -1);
                storeStarDb.increase(storeStarfilter, increaseValues, new Handler<Long>() {
                    @Override
                    public void handle(Long count) {
                        storeCommentDb.remove(oldComment, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean deleted) {
                                callback.handle(
                                        new JsonObject()
                                                .putNumber("error", 0)
                                                .putBoolean("deleted", deleted)
                                );
                            }
                        });
                    }
                });
            }
        });
    }

//    private void increaseStoreStarFields(StoreComment oldComment, StoreComment newComment, final Handler<Long> callback) {
//        //TODO: re-rates store;
//        StoreStar storeStarfilter = new StoreStar();
//        storeStarfilter.storeId = oldComment.storeId;
//
//        StoreStar increaseValues = new StoreStar();
//        if (newComment != null)
//            increaseValues.increaseStar(newComment.star, 1);
//        if (oldComment != null)
//            increaseValues.increaseStar(oldComment.star, -1);
//        storeStarDb.increase(storeStarfilter, increaseValues, true, new Handler<Long>() {
//            @Override
//            public void handle(Long count) {
//                if (callback != null)
//                    callback.handle(count);
//            }
//        });
//    }

    public void getStoreWarningTypes(Handler<Collection<StoreWarningType>> callback) {
        callback.handle(warningTypeMap.values());
    }

    public StoreWarningType getStoreWarningType(int id) {
        return warningTypeMap.get(id);
    }
}
