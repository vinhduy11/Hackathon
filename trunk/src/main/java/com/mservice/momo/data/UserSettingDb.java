package com.mservice.momo.data;

import com.mservice.momo.vertx.models.UserSetting;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

/**
 * Created by nam on 10/3/14.
 */
public class UserSettingDb extends MongoModelController<UserSetting> {

    public UserSettingDb(Vertx vertx, Logger logger) {
        super(vertx, logger);
    }

    @Override
    public String getCollectionName(UserSetting model) {
        return "userSetting";
    }

    @Override
    public UserSetting newModelInstance() {
        return new UserSetting();
    }
}
