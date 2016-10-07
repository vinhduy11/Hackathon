package com.mservice.momo.vertx.gift;

import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.customercare.PromoContentNotification;

/**
 * Created by nam on 10/10/14.
 */
public class GiftError {
    public static final int SYSTEM_ERROR = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
    public static final int NO_SUCH_GIFT = MomoProto.TranHisV1.ResultCode.NO_SUCH_GIFT_VALUE;
    public static final int GIFT_TYPE_NOT_ACTIVE = MomoProto.TranHisV1.ResultCode.GIFT_TYPE_NOT_ACTIVE_VALUE;
    public static final int WRONG_PRICE_AMOUNT = MomoProto.TranHisV1.ResultCode.WRONG_PRICE_AMOUNT_VALUE;
    public static final int ALREADY_OWNED = MomoProto.TranHisV1.ResultCode.ALREADY_OWNED_VALUE;
    public static final int NOT_OWNED = MomoProto.TranHisV1.ResultCode.NOT_OWNED_VALUE;
    public static final int NOT_TRANSFERABLE = MomoProto.TranHisV1.ResultCode.NOT_TRANSFERABLE_VALUE;

    public static final int QUEUED_GIFT_SERVICE = -2000;
    public static final int GIFT_NOT_QUEUED = -2001;
    public static final int GIFT_NOT_STABLE = -2002;
    public static final int NO_SUCH_GIFTYPE = -2003;
    public static final int OVERTIME_TO_USE = -2004;

    public static final int ACTIVATED_TIME_ERROR = -2005;


    public static String getDesc(int error) {
        switch (error) {
            case SYSTEM_ERROR:
                return "Lỗi hệ thống!";
            case NO_SUCH_GIFT:
                return "Thẻ quà tặng không tồn tại!";
            case GIFT_TYPE_NOT_ACTIVE:
                return "Thẻ quà tặng đang bị khóa!";
            case WRONG_PRICE_AMOUNT:
                return "Giá trị thẻ quà tặng không đúng!";
            case ALREADY_OWNED:
                return "Thẻ quà tặng này đang là của bạn!";
            case NOT_OWNED:
                return "Thẻ quà tặng này không phải của bạn!";
            case NOT_TRANSFERABLE:
                return "Thẻ quà tặng này không thể! ";
            case QUEUED_GIFT_SERVICE:
                return "Có một thẻ quà tặng khác đang chờ thanh toán!";
            case GIFT_NOT_QUEUED:
                return "Thẻ quà tặng chưa sẵn sàng để dùng!";  // internal backend only
            case OVERTIME_TO_USE:
                return "Thẻ quà tặng chỉ được kích hoạt trong %s ngày kể từ ngày tặng";
            case ACTIVATED_TIME_ERROR:
                return PromoContentNotification.BILL_PAY_PROMO_CAN_NOT_ACTIVE_GIFT;
        }
        return "Lỗi hệ thống!";
    }
}
