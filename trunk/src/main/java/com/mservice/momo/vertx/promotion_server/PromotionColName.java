package com.mservice.momo.vertx.promotion_server;

/**
 * Created by concu on 7/25/16.
 */
public class PromotionColName {

    public static class PromotionCountControlCol{
        public static String PHONE_NUMBER = "_id";
//        public static String PHONE_NUMBER = "phone_number";
        public static String PROGRAM = "program";
        public static String COUNT = "count";
        public static String TIME = "time";
        public static String TABLE = "promotion_count";
    }

    public static class PromotionErrorControlCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String PROGRAM      = "program";
        public static String TIME         = "time";
        public static String ERROR_CODE = "error_code";
        public static String DESC       = "desc";
        public static String DEVICE_INFO = "device_info";
        public static String TABLE = "promotion_error";
    }

    public static class PromotionDeviceControlCol{
        public static String ID = "_id";
        public static String PHONE_NUMBER = "phone_number";
        public static String TABLE = "promotion_device";
    }

}
