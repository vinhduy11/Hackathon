package com.mservice.momo.util;

/**
 * Created by ntunam on 3/20/14.
 */
public class ValidationUtil {
    public static boolean isValidPin(String pin) {
        int length = pin.trim().length();
        return pin != null && (length >= 6 && length <= 8);
    }

    public static boolean isEmpty(String string) {
        return string == null || string.trim().length() == 0;
    }
}
