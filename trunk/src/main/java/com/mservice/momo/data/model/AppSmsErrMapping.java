package com.mservice.momo.data.model;

import java.util.HashMap;

/**
 * Created by User on 4/7/14.
 */
public class AppSmsErrMapping {
    public static int ok = 0;
    public static int msg_format_not_valid = 5001;
    public static int number_not_valid = 5002;
    public static int pin_not_valid = 5003;
    public static int msg_len_not_valid = 5004;
    public static int system_error = 5005;

    private static HashMap<Integer,String> hashMap;

    static {
        hashMap = new HashMap<Integer,String>();
        hashMap.put(ok,"Thuc hien giao dich thanh cong");
        hashMap.put(msg_format_not_valid,"Lenh khong dung cu phap");
        hashMap.put(number_not_valid,"So dien thoai khong hop le");
        hashMap.put(pin_not_valid,"Ma PIN khong dung.");
        hashMap.put(msg_len_not_valid,"Chieu dai tin nhan khong hop le");
        hashMap.put(system_error,"He thong tam dung cung cap dich vu");

        //core
        hashMap.put(1,"bad format");
        hashMap.put(3,"agent not found");
        hashMap.put(4,"agent not registered");
        hashMap.put(5,"agent inactive");
        hashMap.put(6,"agent suspended");
        hashMap.put(7,"access denied");
        hashMap.put(8,"bad password");
        hashMap.put(9,"password expired");
        hashMap.put(10,"password prev used");
        hashMap.put(11,"password retry exceed");
        hashMap.put(12,"password same");
        hashMap.put(13,"target not found");
        hashMap.put(14,"target not registered");
        hashMap.put(15,"target inactive");
        hashMap.put(16,"target suspended");
        hashMap.put(17,"invalid amount");
        hashMap.put(18,"already registered");
        hashMap.put(19,"agent blacklisted");
        hashMap.put(20,"target blacklisted");
        hashMap.put(22,"target is self");
        hashMap.put(23,"amount too small");
        hashMap.put(24,"amount too big");
        hashMap.put(25,"confirm wrong amount");
        hashMap.put(26,"target required");
        hashMap.put(27,"agent expired");
        hashMap.put(28,"target expired");
        hashMap.put(29,"invalid password");
        hashMap.put(30,"agent active");
        hashMap.put(31,"agent not party");
        hashMap.put(32,"request expired");
        hashMap.put(33,"transaction reversed");
        hashMap.put(34,"transaction not two party");
        hashMap.put(35,"invalid status change");
        hashMap.put(36,"transaction too old");
        hashMap.put(37,"transaction not completed");
        hashMap.put(38,"agent stopped");
        hashMap.put(39,"target stopped");
        hashMap.put(40,"agent started");
        hashMap.put(41,"in sys error");
        hashMap.put(42,"in bad msisdn");
        hashMap.put(43,"in sys not available");
        hashMap.put(44,"in bad voucher");
        hashMap.put(45,"in timeout");
        hashMap.put(46,"amount out of range");
        hashMap.put(47,"target is not special");
        hashMap.put(48,"billpay account invalid");
        hashMap.put(49,"bank offline");
        hashMap.put(50,"bank error");
        hashMap.put(51,"sync not required");
        hashMap.put(52,"device already active");
        hashMap.put(53,"config update not required");
        hashMap.put(54,"not device owner");
        hashMap.put(55,"alias already exists");
        hashMap.put(56,"alias not found");
        hashMap.put(57,"agent already suspended");
        hashMap.put(58,"agent already stopped");
        hashMap.put(59,"cannot use wallet type");
        hashMap.put(60,"schedule disallowed");
        hashMap.put(61,"invalid schedule");
        hashMap.put(62,"schedule not found");
        hashMap.put(63,"confirm multiple txns");
        hashMap.put(64,"agent is not special");
        hashMap.put(66,"connection offline");
        hashMap.put(67,"connection error");
        hashMap.put(68,"operation not supported");
        hashMap.put(69,"agent not well configured");
        hashMap.put(70,"agent has transactions");
        hashMap.put(71,"agent deleted");
        hashMap.put(72,"target deleted");
        hashMap.put(73,"agent has balance");
        hashMap.put(74,"invalid password mismatch");
        hashMap.put(75,"reversal failed");
        hashMap.put(76,"groupname same or exists");
        hashMap.put(77,"group not empty");
        hashMap.put(78,"confirm same initiator");
        hashMap.put(103,"confirm rejected");
        hashMap.put(400,"transaction not found");
        hashMap.put(415,"trace transaction not found");
        hashMap.put(423,"trans already finalized");
        hashMap.put(425,"message too long");
        hashMap.put(501,"cannot link to subagent");
        hashMap.put(502,"cannot link to self");
        hashMap.put(503,"link agent deleted");
        hashMap.put(504,"cannot join to self");
        hashMap.put(505,"agents already joined");
        hashMap.put(506,"join agent deleted");
        hashMap.put(507,"agents not joined");
        hashMap.put(508,"purchase order number not unique");
        hashMap.put(1001,"insufficient funds");
        hashMap.put(1002,"transaction recovered");
        hashMap.put(1003,"wallet balance exceeded");
        hashMap.put(1004,"wallet cap exceeded");
        hashMap.put(1005,"request expired");
        hashMap.put(1006,"system error");
        hashMap.put(1007,"Db error");
        hashMap.put(1008,"bad request");
        hashMap.put(1009,"Db constraint");
        hashMap.put(1010,"Db no record");
        hashMap.put(1011,"auth bad token");
        hashMap.put(1012,"auth retry exceed");
        hashMap.put(1013,"auth expired");
        hashMap.put(1014,"auth bad password");
        hashMap.put(1015,"auth same password");
        hashMap.put(1016,"auth password prev used");
        hashMap.put(1020,"param invalid required");
        hashMap.put(1021,"param invalid too short");
        hashMap.put(1022,"param invalid too long");
        hashMap.put(1023,"param invalid regxp");
        hashMap.put(1024,"licence expired");

        // continue
    }
    public static String getMsg(int rcode){
        return hashMap.get(rcode) == null ? "SMS content is not defined " + rcode : hashMap.get(rcode);
    }
}
