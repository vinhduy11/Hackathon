package com.mservice.momo.util;

import com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.vertx.processor.Common;
import org.apache.mina.util.Base64;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import vn.com.ms.config.Config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by admin on 1/9/14.
 */
public class DataUtil {
    public static final char ZERO = '0';
    public static final char NINE = '9';
    public static String sHash = "KCLS6XY239EDM8GBT7NHVA5F4ZPIR";
    public static final String VIETTEL_PROVIDER = "Viettel";
    public static final String MOBIFONE_PROVIDER = "Mobifone";
    public static final String VINAPHONE_PROVIDER = "Vinaphone";
    public static final String VIETNAMMOBILE_PROVIDER = "Vietnamobile";
    public static final String GMOBILE_PROVIDER = "GMobile";
    public static final String SFONE_PROVIDER = "SFone";
    public static final String UNKNOW_PROVIDER = "Unknow";

    static long[] LONG_TEN_POW = new long[]{
            1,
            10,
            100,
            1000,
            10000,
            100000,
            1000000,
            10000000,
            100000000,
            1000000000,
            10000000000L,
            100000000000L,
            1000000000000L,
            10000000000000L,
            100000000000000L,
            1000000000000000L,
            10000000000000000L,
            100000000000000000L,
            1000000000000000000L // 10^18
    };
    private static String[] arrChar = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static char[] hexits = "0123456789ABCDEF".toCharArray();

    public static long stringToVnPhoneNumber(String number) {

        if (number == null) {
            return -1;
        }

        int length = number.length();

        //this must be a vietnam number
        if (number.equalsIgnoreCase("0")) {
            return 0;
        } else if (length < 9 || length > 11) {
            return -1;
        } else {
            long result = stringToUNumber(number, length);
            //phone number in vn must like 0900 000 000 or 01000 000 000
            if (result < 800000000) {
                return -1;
            } else {
                return result;
            }
        }
    }

    public static long stringToUNumber(String number, int length) {
        long result = -1;
        if (length < 20) {
            result = 0;
            int c;
            for (int i = length - 1; i >= 0; i--) {
                c = number.charAt(i) - ZERO;
                if (0 <= c && c <= 9) {
                    result += c * LONG_TEN_POW[length - (i + 1)];
                } else {
                    result = -1;
                    break;
                }
            }
        }
        return result;
    }

    public static long stringToUNumber(String number) {
        if (number == null || "".equalsIgnoreCase(number)) return 0;
        if (!number.equalsIgnoreCase("") && number.contains(".")) {
            number = number.split("\\.")[0];
        }
        return stringToUNumber(number, number.length());
    }

    public static String getSHA(String data) throws Exception {
        MessageDigest md = null;
        byte[] ba = null;
        md = MessageDigest.getInstance("SHA");
        md.update(data.getBytes("UTF8"));
        ba = md.digest();
        StringBuffer sb = new StringBuffer(ba.length * 2);

        for (int i = 0; i < ba.length; i++) {
            sb.append(hexits[(((int) ba[i] & 0xFF) / 16) & 0x0F]);
            sb.append(hexits[((int) ba[i] & 0xFF) % 16]);
        }
        return sb.toString();
    }

    public static String getOtp() {
        String str = "";
        for (int i = 0; i < 6; i++) {
            str += arrChar[new Random().nextInt(arrChar.length)];
        }
        return str;
    }

    static public boolean isValidPin(String pin) {
        if (pin.length() != 6) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            char c = pin.charAt(i);
            if (c < ZERO || c > NINE) {
                return false;
            }
        }
        return true;
    }

    static public String shaHash(String input) throws NoSuchAlgorithmException {
        String result = "";
        MessageDigest sha = MessageDigest.getInstance("SHA");
        sha.update(input.getBytes());
        BigInteger dis = new BigInteger(1, sha.digest());
        result = dis.toString(16);
        return result.toUpperCase();
    }

    public static int strToInt(String input) {
        if (input == null) return 0;
        input = input.trim();
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long strToLong(String input) {
        if (input == null) return 0;
        input = input.trim();
        try {
            return Long.parseLong(input);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isExistFromConfigList(String ele, String configKey) {
        List<String> list = Config.getListConfig(configKey);
        for (int i = 0; i < list.size(); i++) {
            if (ele.equalsIgnoreCase(list.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static String xmlDateToString(XMLGregorianCalendar xmlDate) {
        String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(xmlDate.toGregorianCalendar().getTime());
    }

    public static String encode(String orgStr) {
        //encoding  byte array into base 64
        byte[] encoded = Base64.encodeBase64(orgStr.getBytes());
        return new String(encoded);
    }

    public static String decode(String encodedStr) {

        if (encodedStr == null || encodedStr.isEmpty()) {
            return "";
        }
        //decoding byte array into base64
        byte[] decoded = Base64.decodeBase64(encodedStr.getBytes());
        return new String(decoded);

    }

    public static double getDouble(String src) {
        double result = 0;
        try {
            result = Double.parseDouble(src);
            return result;
        } catch (Exception ex) {
            return 0;
        }
    }


    /*private static char[] hexits="0123456789ABCDEF".toCharArray();
    public static String getSHA(String data) throws Exception
    {
        MessageDigest md = null;
        byte [] ba = null;
        md = MessageDigest.getInstance("SHA");
        md.update(data.getBytes("UTF-8"));
        ba = md.digest();
        StringBuffer sb = new StringBuffer(ba.length*2);
        for (int i = 0; i < ba.length; i++) {
            sb.append(hexits[(((int)ba[i] & 0xFF) / 16) & 0x0F]);
            sb.append(hexits[((int)ba[i] & 0xFF) % 16]);
        }
        return sb.toString();
    }*/

    public static HashMap<String, String> convertKVPArrToMap(KeyValuePair[] input) {
        HashMap<String, String> output = new HashMap<String, String>();

        for (KeyValuePair element : input) {
            if (element.getKey() != null
                    && element.getValue() != null)
                output.put(element.getKey(), element.getValue());
        }

        return output;
    }

    public static String getDeviceInfo(String deviceModel)
    {
        String deviceInfo = "";

        String[] device_datas = deviceModel.split(MomoMessage.BELL, 2);
        if(device_datas.length > 1)
        {
            //Bo gia tri dau tien.
            deviceInfo = device_datas[1].toString().trim();
        }
        else {
            deviceInfo = deviceModel;
        }

        return deviceInfo;

    }
    //for HMAC SAH1 algorithrm
    /*
        mTransactionID + returnCode + ts +
        secretKey(123Pay cung cấp)
    */


    public static String phoneProviderName(String number) {
        String char3 = number.substring(0, 3);
        String char4 = number.substring(0, 4);
        switch (char3) {
            case "091":
            case "094":
            case "088":
                return "Vinaphone";
            case "090":
            case "093":
            case "089":
                return "Mobiphone";
            case "098":
            case "097":
            case "086":
                return "Viettel";
            case "092":
                return "Vietnammobile";
            case "095":
                return "Sfone";
            case "096":
                return "EVN";
        }
        switch (char4) {
            case "0123":
            case "0124":
            case "0125":
            case "0127":
            case "0129":
                return "Vinaphone";
            case "0122":
            case "0126":
            case "0128":
            case "0121":
            case "0120":
                return "Mobiphone";
            case "0169":
            case "0168":
            case "0167":
            case "0166":
            case "0165":
            case "0164":
            case "0163":
                return "Viettel";
            case "0188":
                return "Vietnammobile";
        }
        return "";
    }

    public static String getHMACSHA1(String data, String key, Common.BuildLog log) {
        String result = "";
        String HMAC_SHA1_ALGORITHM = "HmacSHA1";
        try {
            //SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

            SecretKeySpec signingKey = new SecretKeySpec(null, HMAC_SHA1_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            result = new String(mac.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException ne) {
            log.add("exception", "NoSuchAlgorithmException " + ne.getMessage());

        } catch (InvalidKeyException ie) {
            log.add("exception", "InvalidKeyException " + ie.getMessage());
        } catch (Exception ex) {
            log.add("exception", "InvalidKeyException " + ex.getMessage());
        }

        return result;
    }

    private static String getSAH1(String strSrc, Common.BuildLog log) {

        String result = "";
        log.add("function", "getSAH1");
        log.add("strSrc", strSrc);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");

            md.update(strSrc.getBytes());
            byte[] output = md.digest();

            String sah = new String(output);

            log.add("SAH1 result", sah);

        } catch (Exception e) {
            log.add("exception", e.getMessage());
        }

        return result;
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    public static String getHMACSHA1WithoutKey(String text) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(text.getBytes());
            return toHexString(result);

        } catch (NoSuchAlgorithmException nse) {
            return "";
        } catch (Exception ex) {
            return "";
        }

    }

    public static boolean isValidCode(String code, String begin) {
        if (code == null) {
            return false;
        }
        code = code.toUpperCase();
        if (code.startsWith(begin)) {
            code = code.substring(begin.length());
        }
        if (code.length() != 6) {
            return false;
        }

        return code.charAt(5) == getHash(code.substring(0, 5), sHash);
    }

    public static char getHash(String random, String checksum) {
        long total = 0;
        int hash = 0;
        for (int i = 0; i < random.length(); i++) {
            total += (int) random.charAt(i);
        }

        hash = (int) (total % 29);
        return checksum.charAt(hash);
    }

    public static Integer toInteger(String str, int defaultResult) {
        if (str == null) return defaultResult;
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultResult;
        }
    }

//    public static ArrayList<Object> convertJsonArrayToArrayList(JsonArray jsonArray) {
//        ArrayList<Object> obj = new ArrayList<>();
//
//        if (jsonArray.size() > 0) {
//            for (Object o : jsonArray) {
//
//            }
//        }
//
//        return obj;
//    }

    public static String timeDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        return dateFormat.format(date);
    }

    public static long parseTime(String format, String source) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(source).getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    public static StringFormater stringFormat(String template) {
        return new StringFormater(template);
    }

    public static String nullToEmpty(Object object) {
        return object == null ? "" : object.toString();
    }


    //    public static int checkServiceType(String serviceId)
//    {
//        int serviceType = 0;
//        //
//        switch (serviceId)
//        {
//            case "":
//                break;
//        }
//
//
//
//
//        return serviceType;
//    }
    public static HashMap<String, String> convertJsonToHashMap(JsonObject joData) {

        HashMap<String, String> map = new HashMap<String, String>();
//        JsonObject jObject = new JsonObject(t);
        Set<String> set = joData.getFieldNames();
        String value = "";
        for (String fieldName : set) {
            value = String.valueOf(joData.getValue(fieldName));
            map.put(fieldName, value);
        }
        return map;
    }

    public static int countNumberOfDayInMonth(int month, int year)
    {
        int numberOfDay = 0;
        switch (month)
        {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                numberOfDay = 31;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                numberOfDay = 30;
                break;
            case 2:
                numberOfDay = 0 == year % 4 ? 29 : 28;
                break;
            default:
                numberOfDay = 30;
                break;
        }

        return numberOfDay;

    }

    public static enum POPUP_TYPE{
        CONFIRM_POPUP, CANCEL_POPUP, HSBC_POPUP, REDIRECT_POPUP, EMAIL_POPUP
    }

    public static int getType(POPUP_TYPE popup_type)
    {
        int type = 0;
        switch (popup_type)
        {
            case CONFIRM_POPUP:
                type = 0;
                break;
            case CANCEL_POPUP:
                type = 1;
                break;
            case HSBC_POPUP:
                type = 2;
                break;
            case REDIRECT_POPUP:
                type = 3;
                break;
            case EMAIL_POPUP:
                type = 4;
                break;
            default:
                type = 0;
                break;
        }
        return type;
    }

    public static boolean isJsonArray(String s) {
        JsonArray resJson = null;
        try {
            resJson = new JsonArray(s);
        } catch (Exception e) {
        }
        return resJson != null;
    }

    public static boolean isJsonObject(String s) {
        JsonObject resJson = null;
        try {
            resJson = new JsonObject(s);
        } catch (Exception e) {
        }
        return resJson != null;
    }

    // Kiểm tra tên nhà mạng tại VN
    public static String getPhoneNumberProviderInVN(String phoneNumber) {
        String phones[] = {"096", "097", "098", "0163", "0164", "0165",
                "0166", "0167", "0168", "0169","086", // VIETTEL
                "090", "093", "0120", "0121", "0122", "0126", "0128","089", // MOBIFONE
                "091", "094", "0123", "0124", "0125", "0127", "0129", "088",// VINAPHONE
                "092", "0188", // VIETNAM MOBILE
                "0996", "0199", // GMOBILE
                "095" // SFONE
        };
        for (int i = 0; i < phones.length; i++) {
            if (phoneNumber.startsWith(phones[i])) {
                if (i >= 0 && i <= 9) {
                    return VIETTEL_PROVIDER;
                } else if (i >= 10 && i <= 16) {
                    return MOBIFONE_PROVIDER;
                } else if (i >= 17 && i <= 23) {
                    return VINAPHONE_PROVIDER;
                } else if (i >= 24 && i <= 25) {
                    return VIETNAMMOBILE_PROVIDER;
                } else if (i >= 26 && i <= 27) {
                    return GMOBILE_PROVIDER;
                } else {
                    return SFONE_PROVIDER;
                }
            }
        }
        return UNKNOW_PROVIDER;
    }

    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("dd/MM/yyyy");
        return format.format(date);
    }
}
