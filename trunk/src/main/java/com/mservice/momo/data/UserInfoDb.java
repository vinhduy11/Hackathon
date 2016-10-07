package com.mservice.momo.data;

import com.mservice.momo.vertx.models.UserInfo;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by nam on 5/23/14.
 */
public class UserInfoDb extends MongoModelController<UserInfo> {

    public UserInfoDb(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public String getCollectionName(UserInfo model) {
        return "statistic_userInfo";
    }

    @Override
    public UserInfo newModelInstance() {
        return new UserInfo();
    }
}
