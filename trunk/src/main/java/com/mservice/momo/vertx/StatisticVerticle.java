package com.mservice.momo.vertx;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.MaxMobileOnlineDb;
import com.mservice.momo.data.StatisticDb;
import com.mservice.momo.data.UserInfoDb;
import com.mservice.momo.msg.StatisticModels;
import com.mservice.momo.vertx.models.MaxMobileOnline;
import com.mservice.momo.vertx.models.UserInfo;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by nam on 4/28/14.
 */
public class StatisticVerticle extends Verticle {

//    public static final String ADDRESS_ACTION = "StatisticVerticle.action";

//    public static final String ADDRESS_GET_NUMBER = "StatisticVerticle.getNumber";

    private StatisticDb statisticDb;
    private MaxMobileOnlineDb maxMobileOnlineDb;
    private UserInfoDb userInfoDb;

    private AtomicLong onlineUsers;

    private void init() {
        userInfoDb = new UserInfoDb(vertx, container);
        onlineUsers = new AtomicLong(0);
    }

    public void onlineNumber(StatisticModels.Action action) {
        final long currentNumber = onlineUsers.incrementAndGet();
        MaxMobileOnline maxMobileOnline = new MaxMobileOnline();
        maxMobileOnline.setModelId(getYYYY_MM_DD());
        maxMobileOnlineDb.findOne(maxMobileOnline, new Handler<MaxMobileOnline>() {
            @Override
            public void handle(MaxMobileOnline mobile) {
                if (mobile == null) {
                    mobile = new MaxMobileOnline();
                    mobile.setModelId(getYYYY_MM_DD());
                    mobile.setTime(new Date());
                    mobile.setUserNumber(currentNumber);
                    maxMobileOnlineDb.save(mobile, null);
                } else {
                    if (mobile != null && mobile.getUserNumber() < currentNumber) {
                        mobile.setTime(new Date());
                        mobile.setUserNumber(currentNumber);
                        maxMobileOnlineDb.update(mobile, false, null);
                    }
                }
            }
        });
    }

    public void offlineNumber(StatisticModels.Action action) {
        onlineUsers.decrementAndGet();
    }

    @Override
    public void start() {
        init();
        JsonObject globalConfig = container.config();
        JsonObject config = globalConfig.getObject("statisticVerticle", new JsonObject());

        statisticDb = new StatisticDb(vertx.eventBus());
        maxMobileOnlineDb = new MaxMobileOnlineDb(vertx, container);
        vertx.eventBus().registerLocalHandler(AppConstant.StatisticVerticle_ADDRESS_ACTION, new Handler<Message<byte[]>>() {
            @Override
            public void handle(Message<byte[]> request) {
                StatisticModels.Action action = null;
                try {
                    action = StatisticModels.Action.parseFrom(request.body());
                } catch (InvalidProtocolBufferException e) {
                    container.logger().error(e.getMessage(), e);
                    return;
                }

//                final int phoneNumber = action.getPhoneNumber();
//                if (phoneNumber == 0) {
//                    container.logger().error("Phone number can't be zero!");
//                    return;
//                }

                switch (action.getType()) {
                    case SEND_NOTI_LIST_VIA_CLOUD:
                        increase(StatisticModels.SubActionType.SEND_NOTI_LIST_VIA_CLOUD_IN_DAY);
                        break;
                    case SEND_NOTI_LIST_VIA_SOCKET:
                        increase(StatisticModels.SubActionType.SEND_NOTI_LIST_VIA_SOCKET_IN_DAY);
                        break;
                    case SEND_NOTI_VIA_CLOUD:
                        increase(StatisticModels.SubActionType.SEND_NOTI_VIA_CLOUD_IN_DAY);
                        break;
                    case SEND_NOTI_VIA_SOCKET:
                        increase(StatisticModels.SubActionType.SEND_NOTI_VIA_SOCKET_IN_DAY);
                        break;
                    case USER_REGISTER:
                        userRegisterProcess(action);
                        break;
                    case USER_LOGIN:
                        userLogInProcess(action);
                        break;
                    case USER_TRANS:
                        userMakeTransaction(action);
                        break;
                    case USER_ONLINE:
                        onlineNumber(action);
                        break;
                    case USER_OFFLINE:
                        offlineNumber(action);
                        break;
                    default:
                        throw new IllegalAccessError();
                }

            }
        });

    }

    private void userRegisterProcess(final StatisticModels.Action action) {
        switch (action.getChannel()) {
            case MOBILE:
                increase(StatisticModels.SubActionType.REGISTER_FROM_PHONE_IN_DAY);
                break;
            case WEB:
                increase(StatisticModels.SubActionType.REGISTER_FROM_WEB_IN_DAY);
                break;
        }
    }

    private void userMakeTransaction(final StatisticModels.Action action) {
        UserInfo filter = new UserInfo();
        filter.setPhoneNumber(action.getPhoneNumber());
        userInfoDb.findOne(filter, new Handler<UserInfo>() {
            @Override
            public void handle(UserInfo userInfo) {
                if (userInfo == null) {
                    // The time using app.
                    container.logger().info(action.getPhoneNumber() + " has a first time user MoMo web/mobile app.");
                    userInfo = new UserInfo();
                    userInfo.setPhoneNumber(action.getPhoneNumber());
                }
                switch (action.getChannel()) {
                    case MOBILE:
                        if (userInfo.getLastMobileTransTime() == null) {
                            increase(StatisticModels.SubActionType.MOBILE_USER_HAS_TRANS_IN_DAY);
                            increase(StatisticModels.SubActionType.MOBILE_USER_HAS_TRANS_IN_MONTH);
                        } else {
                            if (userInfo.getLastMobileTransTime().getTime() < getCurrentDay()) {
                                increase(StatisticModels.SubActionType.MOBILE_USER_HAS_TRANS_IN_DAY);
                            }
                            if (userInfo.getLastMobileTransTime().getTime() < getMonthTime()) {
                                increase(StatisticModels.SubActionType.MOBILE_USER_HAS_TRANS_IN_MONTH);
                            }
                        }
                        userInfo.setLastMobileTransTime(new Date());
                        break;
                    case WEB:
                        if (userInfo.getLastWebTransTime() == null) {
                            increase(StatisticModels.SubActionType.WEB_USER_HAS_TRANS_IN_DAY);
                            increase(StatisticModels.SubActionType.WEB_USER_HAS_TRANS_IN_MONTH);
                        } else {
                            if (userInfo.getLastWebTransTime().getTime() < getCurrentDay()) {
                                increase(StatisticModels.SubActionType.WEB_USER_HAS_TRANS_IN_DAY);
                            }
                            if (userInfo.getLastWebTransTime().getTime() < getMonthTime()) {
                                increase(StatisticModels.SubActionType.WEB_USER_HAS_TRANS_IN_MONTH);
                            }
                        }
                        userInfo.setLastWebTransTime(new Date());
                        break;
                }
                if (userInfo.getModelId() == null)
                    userInfoDb.save(userInfo, null);
                else
                    userInfoDb.update(userInfo, false, null);
            }
        });
    }

    private void userLogInProcess(final StatisticModels.Action action) {
        UserInfo filter = new UserInfo();
        filter.setPhoneNumber(action.getPhoneNumber());
        userInfoDb.findOne(filter, new Handler<UserInfo>() {
            @Override
            public void handle(UserInfo userInfo) {
                if (userInfo == null) {
                    // The time using app.
                    container.logger().info(action.getPhoneNumber() + " has a first time user MoMo web/mobile app.");
                    userInfo = new UserInfo();
                    userInfo.setPhoneNumber(action.getPhoneNumber());
                }
                switch (action.getChannel()) {
                    case MOBILE:
                        if (userInfo.getLastLoginFromMobile() == null) {
                            increase(StatisticModels.SubActionType.NEW_PHONE_USER_LOGIN_IN_DAY);
                            increase(StatisticModels.SubActionType.USER_LOGIN_PHONE_IN_DAY);
                        } else {
                            if (userInfo.getLastLoginFromMobile().getTime() < getCurrentDay()) {
                                increase(StatisticModels.SubActionType.USER_LOGIN_PHONE_IN_DAY);
                            }
                        }
                        userInfo.setLastLoginFromMobile(new Date());
                        break;
                    case WEB:
                        if (userInfo.getLastLoginFromWeb() == null) {
                            increase(StatisticModels.SubActionType.NEW_WEB_USER_LOGIN_IN_DAY);
                            increase(StatisticModels.SubActionType.USER_LOGIN_WEB_IN_DAY);
                        } else {
                            if (userInfo.getLastLoginFromWeb().getTime() < getCurrentDay()) {
                                increase(StatisticModels.SubActionType.USER_LOGIN_WEB_IN_DAY);
                            }
                        }
                        userInfo.setLastLoginFromWeb(new Date());
                        break;
                }
                if (userInfo.getModelId() == null)
                    userInfoDb.save(userInfo, null);
                else
                    userInfoDb.update(userInfo, false, null);
            }
        });
    }

    public void increase(StatisticModels.SubActionType subActionType) {
        switch (subActionType) {
            case REGISTER_FROM_WEB_IN_DAY:
            case REGISTER_FROM_PHONE_IN_DAY:
            case NEW_WEB_USER_LOGIN_IN_DAY:
            case NEW_PHONE_USER_LOGIN_IN_DAY:
            case USER_LOGIN_WEB_IN_DAY:
            case USER_LOGIN_PHONE_IN_DAY:
            case WEB_USER_HAS_TRANS_IN_DAY:
            case SEND_NOTI_VIA_CLOUD_IN_DAY:
            case SEND_NOTI_VIA_SOCKET_IN_DAY:
            case SEND_NOTI_LIST_VIA_CLOUD_IN_DAY:
            case SEND_NOTI_LIST_VIA_SOCKET_IN_DAY:
                statisticDb.increaseDayAction(getCurrentDay(), subActionType.name(), 1, null);
                break;
            case WEB_USER_HAS_TRANS_IN_MONTH:
                statisticDb.increaseMonthAction(getMonthTime(), subActionType.name(), 1, null);
                break;
            case MOBILE_USER_HAS_TRANS_IN_DAY:
                statisticDb.increaseDayAction(getCurrentDay(), subActionType.name(), 1, null);
                break;
            case MOBILE_USER_HAS_TRANS_IN_MONTH:
                statisticDb.increaseMonthAction(getMonthTime(), subActionType.name(), 1, null);
                break;

            case MAX_WEB_ONLINE_USER_IN_DAY:
            case MAX_PHONE_ONLINE_USER_IN_DAY:
                statisticDb.increaseDayAction(getCurrentDay(), subActionType.name(), 1, null);
                break;
            default:
                container.logger().error("Unknown subActionType: " + subActionType);
                break;
        }
    }

    public static long getCurrentDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    public static long getMonthTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    public String getYYYY_MM_DD() {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("yyy_MM_dd");

        return dateFormat.format(getCurrentDay());
    }
}
