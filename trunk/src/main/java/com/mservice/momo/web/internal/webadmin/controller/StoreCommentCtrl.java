package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.rate.StoreCommentDb;
import com.mservice.momo.data.rate.StoreWarningDb;
import com.mservice.momo.vertx.models.rate.StoreComment;
import com.mservice.momo.vertx.models.rate.StoreWarning;
import com.mservice.momo.vertx.models.rate.StoreWarningType;
import com.mservice.momo.vertx.rate.StoreRateManager;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 9/15/14.
 */
public class StoreCommentCtrl {
    private Vertx vertx;
    private Container container;

    private StoreCommentDb storeCommentDb;
    private StoreRateManager storeRateManager;
    private StoreWarningDb storeWarningDb;

    public StoreCommentCtrl(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
        storeCommentDb = new StoreCommentDb(vertx, container.logger());
        storeRateManager = new StoreRateManager(vertx, container);
        storeWarningDb = new StoreWarningDb(vertx, container.logger());
    }

    //params: lowestTime, pageSize, storeId, commenter, status
//    @Action(path = "/service/store/comment/getPage", roles = {Role.SUPER, Role.STORE_COMMENT})
    @Action(path = "/service/store/comment/getPage")
    public void getStoreCommentPage(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        long lowestTime;
        try {
            lowestTime = Long.parseLong(params.get("lowestTime"));
            if (lowestTime == 0) {
                lowestTime = Long.MAX_VALUE;
            }
        } catch (Exception e) {
            lowestTime = Long.MAX_VALUE;
        }

        int pageSize;
        try {
            pageSize = Integer.parseInt(params.get("pageSize"));
        } catch (Exception e) {
            pageSize = 10;
        }

        Integer storeId = null;
        try {
            storeId = Integer.parseInt(params.get("storeId"));
        } catch (Exception e) {
        }

        Integer commenter = null;
        try {
            commenter = Integer.parseInt(params.get("commenter"));
        } catch (Exception e) {
        }

        List<Integer> statuses = null;
        String status = params.get("status");
        if (status != null) {
            try {
                JsonArray arr = new JsonArray(status);
                statuses = new ArrayList<>();
                for (Object obj : arr) {
                    if (obj instanceof Integer)
                        statuses.add((Integer) obj);
                }
            } catch (DecodeException e) {

            }
        }

        storeCommentDb.getCommentPage(pageSize, storeId, commenter, lowestTime, statuses, new Handler<List<StoreComment>>() {
            @Override
            public void handle(List<StoreComment> storeComments) {
                JsonArray json = new JsonArray();
                for (StoreComment comment : storeComments) {
                    json.add(comment.toJsonObject());
                }

                callback.handle(json);
            }
        });

    }

    //params: commentId, status,
    @Action(path = "/service/store/comment/setStatus")
//    @Action(path = "/service/store/comment/setStatus", roles = {Role.SUPER, Role.STORE_COMMENT})
    public void setStatus(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String commentId = params.get("commentId");

        Integer status = null;
        try {
            status = Integer.parseInt(params.get("status"));
        } catch (Exception e) {
        }
        if (commentId == null || status == null) {
            callback.handle(
                    new JsonObject().putNumber("error", 2).putString("desc", "Missing params")
            );
            return;
        }

        StoreComment newValues = new StoreComment();
        newValues.status = status;

        if (!newValues.isValidStatus()) {
            callback.handle(
                    new JsonObject().putNumber("error", 1).putString("desc", "Invalid status values")
            );
            return;
        }

        storeRateManager.setCommentStatus(commentId, status, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                callback.handle(result);
            }
        });
    }

    //params: storeId, committer, status, warningType, time, pageSize
    @Action(path = "/service/store/warn/getAll")
//    @Action(path = "/service/store/warn/getAll", roles = {Role.SUPER, Role.STORE_WARNING})
    public void getStoreWarning(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        Integer storeId = null;
        try {
            storeId = Integer.parseInt(params.get("storeId"));
        } catch (Exception e) {
        }

        Integer committer = null;
        try {
            committer = Integer.parseInt(params.get("committer"));
        } catch (Exception e) {
        }

        Integer status = null;
        try {
            status = Integer.parseInt(params.get("status"));
        } catch (Exception e) {
        }

        Integer warningType = null;
        try {
            status = Integer.parseInt(params.get("warningType"));
        } catch (Exception e) {
        }

        Long time = null;
        try {
            time = Long.parseLong(params.get("time"));
        } catch (Exception e) {
        }
        if (time == null || time == 0)
            time = Long.MAX_VALUE;

        Integer pageSize = null;
        try {
            pageSize = Integer.parseInt(params.get("pageSize"));
        } catch (Exception e) {
            pageSize = 2;
        }

        StoreWarning filter = new StoreWarning();
        filter.storeId = storeId;
        filter.committer = committer;
        filter.status = status;
        filter.warningType = warningType;

        storeWarningDb.find(filter, time, pageSize, new Handler<List<StoreWarning>>() {

            @Override
            public void handle(List<StoreWarning> storeWarnings) {
                JsonArray array = new JsonArray();
                for (StoreWarning sw : storeWarnings) {
                    StoreWarningType type = storeRateManager.getStoreWarningType(sw.warningType);
                    String typeName;
                    if (type == null)
                        typeName = "<Not defined>";
                    else
                        typeName = type.name;
                    array.add(sw.toJsonObject().putString("typeName", typeName));
                }
                callback.handle(array);
            }
        });
    }

    //params: _id, status
    @Action(path = "/service/store/warn/setStatus")
//    @Action(path = "/service/store/warn/setStatus", roles = {Role.SUPER, Role.STORE_WARNING})
    public void setStoreWarning(HttpRequestContext context, final Handler<Object> callback) {
        MultiMap params = context.getRequest().params();
        String _id = params.get("_id");

        Integer status = null;
        try {
            status = Integer.parseInt(params.get("status"));
        } catch (Exception e) {
        }

        if (_id == null || status == null) {
            callback.handle(
                    new JsonObject().putNumber("error", 2).putString("desc", "Missing params")
            );
            return;
        }
        StoreWarning warning = new StoreWarning();
        warning.setModelId(_id);
        warning.status = status;
        storeWarningDb.update(warning, false, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                callback.handle(
                        new JsonObject().putNumber("error", 0).putString("desc", "Success")
                );
            }
        });
    }
}
