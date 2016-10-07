package com.mservice.momo.data.wc;

import com.mservice.momo.data.MongoModelController;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.models.wc.DuDoan;
import com.mservice.momo.vertx.models.wc.FullDuDoan;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 6/8/14.
 */
public class FullDuDoanDb extends MongoModelController<FullDuDoan>{

    public FullDuDoanDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(FullDuDoan model) {
        return "wc_fullDuDoan_" + model.matchId;
    }

    @Override
    public FullDuDoan newModelInstance() {
        return new FullDuDoan();
    }

}
