package com.mservice.momo.vertx.models.smartlink;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Created by nam on 7/21/14.
 */
public class SmartLinkRequest {

    public String url;
    public String secureHashSecret;

    public String vpc_Version = "1.1"; //required
    public String vpc_Locale = "vn"; //optional
    public String vpc_Command = "pay"; //required

    public String vpc_Merchant = "userID";
    public String vpc_AccessCode = "password";

    public String vpc_MerchTxnRef; // merchant's generated transaction id. [Use to check status]

    public String vpc_Amount;

    public String vpc_Currency = "VND";

    public String vpc_OrderInfo;  //[optional] response back to merchant.

    public String vpc_ReturnURL; // callback if success

    public String vpc_BackURL; // [optional] callback if cancel

    public String vpc_TicketNo;  //[optional] response back to merchant.

    public String buildUrl() {
        Map<String, Object> sortedMap = new TreeMap<>();

        if (vpc_Version != null)
            sortedMap.put("vpc_Version", vpc_Version);
        if (vpc_Locale != null)
            sortedMap.put("vpc_Locale", vpc_Locale);
        if (vpc_Command != null)
            sortedMap.put("vpc_Command", vpc_Command);
        if (vpc_Merchant != null)
            sortedMap.put("vpc_Merchant", vpc_Merchant);
        if (vpc_AccessCode != null)
            sortedMap.put("vpc_AccessCode", vpc_AccessCode);
        if (vpc_MerchTxnRef != null)
            sortedMap.put("vpc_MerchTxnRef", vpc_MerchTxnRef);
        if (vpc_Amount != null)
            sortedMap.put("vpc_Amount", vpc_Amount);
        if (vpc_Currency != null)
            sortedMap.put("vpc_Currency", vpc_Currency);
        if (vpc_OrderInfo != null)
            sortedMap.put("vpc_OrderInfo", vpc_OrderInfo);
        if (vpc_ReturnURL != null)
            sortedMap.put("vpc_ReturnURL", vpc_ReturnURL);
        if (vpc_BackURL != null)
            sortedMap.put("vpc_BackURL", vpc_BackURL);
        if (vpc_TicketNo != null)
            sortedMap.put("vpc_TicketNo", vpc_TicketNo);

        StringBuilder secureHashInput = new StringBuilder();
        for (String key : sortedMap.keySet()) {
            secureHashInput.append(sortedMap.get(key));
        }

        String secureHash = DigestUtils.md5Hex(secureHashSecret + secureHashInput.toString()).toUpperCase();

        sortedMap.put("vpc_SecureHash", secureHash);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(url).append("?");

        Iterator<Map.Entry<String, Object>> iterator = sortedMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            urlBuilder.append(entry.getKey()).append("=");
            try {
                urlBuilder.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
//                urlBuilder.append(String.valueOf(entry.getValue()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (iterator.hasNext())
                urlBuilder.append("&");
        }

        return urlBuilder.toString();
    }

    public static void main2(String args[]) {
        SmartLinkRequest x = new SmartLinkRequest();
        x.url = "https://payment.smartlink.com.vn/vpcpay.do";
        x.secureHashSecret = "6655B1C7201D9DB5A9D6A940FEAC34D6";
        x.vpc_Merchant = "MSERVICE";
        x.vpc_AccessCode = "E7E4B5D5";
        x.vpc_MerchTxnRef = "4824";
        x.vpc_Amount = "10000000";
        x.vpc_ReturnURL = "http://recharge.momo.vn:28080/MSRecharge/ms_return.jsp";
//        x.vpc_SecureHash = "198BE3F2E8C75A53F38C1C4A5B6DBA27";
        x.vpc_OrderInfo = "0917244824";
        x.vpc_TicketNo = "Nap tien MoMo su dung smartlink";
        System.out.println(x.buildUrl());
    }

    public static void main(String args[]) {
        SmartLinkRequest x = new SmartLinkRequest();
        x.url = "https://paymentcert.smartlink.com.vn:8181/vpcpay.do"; //https://paymentcert.smartlink.com.vn:8181/vpcpay.do
        x.secureHashSecret = "198BE3F2E8C75A53F38C1C4A5B6DBA27"; //198BE3F2E8C75A53F38C1C4A5B6DBA27
        x.vpc_Merchant = "SMLTEST"; //SMLTEST
        x.vpc_AccessCode = "ECAFAB"; //ECAFAB
        x.vpc_MerchTxnRef = "4828";
        x.vpc_Amount = "100000000";
        x.vpc_ReturnURL = "http://recharge.momo.vn:28080/MSRecharge/ms_return.jsp";
        x.vpc_OrderInfo = "123456789";
        x.vpc_TicketNo = "Nap tien MoMo su dung smartlink";
        x.vpc_BackURL = "http://vnexpress.net";
        System.out.println(x.buildUrl());
    }
}
