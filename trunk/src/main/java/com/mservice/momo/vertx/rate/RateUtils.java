package com.mservice.momo.vertx.rate;

/**
 * Created by nam on 9/11/14.
 */
public class RateUtils {
    public static boolean isValidRatePoint(int point) {
        if (point < 1 || point > 5) {
            return false;
        }
        return true;
    }
}
