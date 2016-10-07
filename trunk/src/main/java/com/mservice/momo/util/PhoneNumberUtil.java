package com.mservice.momo.util;

import java.util.HashMap;

/**
 * Created by admin on 2/9/14.
 */
public class PhoneNumberUtil {

    public static int PHONE_MIN = 900000000;
    public static int PHONE_MAX = 1999999999;

    public static HashMap<String, String> otpMap = new HashMap<String, String>();
    private static String[] arrChar = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    public static long stringToVnPhoneNumber(String number) {
        int length = number.length();

        //this must be a vietnam number
        if (number.equalsIgnoreCase("0")) {
            return 0;
        } else if (length < 9 || length > 11) {
            return -1;
        } else {
            long result = stringToUnSignNumber(number);
            //phone number in vn must like 0900 000 000 or 01000 000 000
            if (result < 900000000) {
                return -1;
            } else {
                return result;
            }
        }
    }


    public static final char ZERO = '0';
    public static final int[] pow = new int[]{
            1,
            10,
            100,
            1000,
            10000,
            100000,
            1000000,
            10000000,
            100000000,
            1000000000
    };

    public static int phoneToInt(String number) {

        int result = 0;
        int length = number.length();
        if (length < 9 || 11 < length) {
            return 0;
        }

        int c;
        length--;
        for (int i = 0; i <= length; i++) {
            c = number.charAt(i) - ZERO;
            if (c == 0) {
                continue;
            } else if (0 < c && c < 10) {
                result += c * pow[length - i];
            } else {
                result = -1;
                break;
            }
        }

        return result;
    }

    public static long stringToUnSignNumber(String number) {
        return stringToUnSignNumber(number);
    }

    public static boolean isValidPhoneNumber(int number) {
        return (PHONE_MIN <= number && number <= PHONE_MAX);
    }
}
