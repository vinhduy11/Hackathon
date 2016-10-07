package com.mservice.momo.data.model;

import java.util.HashMap;

/**
 * Created by User on 4/2/14.
 */
public class AppSmsConst {

    public static class TranType{
        //top up
        public final static String TOP_UP ="0";
        public final static String TOP_UP_GAME ="1";
        //direct bank vcb/vtb
        public final static String BANK_IN ="2";
        public final static String BANK_OUT ="3";

        //transfer
        public final static String M2M ="4";
        public final static String M2C ="5";
        public final static String TRANSFER_MONEY_2_PLACE ="6"; // rut tien tai diem giao dich

        public final static String BILL_PAY_TELEPHONE       = "7"; // THANH TOAN CUOC DIEN THOAI
        public final static String BILL_PAY_TICKET_AIRLINE  = "8"; // THANH TOAN VE MAY BAY
        public final static String BILL_PAY_TICKET_TRAIN    = "9"; // THANH TOAN VE TAU LUA
        public final static String BILL_PAY_INSURANCE       = "10"; // THANH TOAN TIEN BAO HIEM
        public final static String BILL_PAY_INTERNET        = "11"; // THANH TOAN CUOC INTERNET
        public final static String BILL_PAY_OTHER           = "12"; // THANH TOAN CAC LOAI HOA DON KHAC
        public final static String BILL_PAY_ELECTRIC           = "18"; // THANH TOAN TIEN DIEN


        public final static String DEPOSIT_CASH_OTHER  = "13"; // NAP TIEN KHAC
        public final static String BUY_MOBILITY_CARD   = "14"; // MUA THE CAO
        public final static String BUY_GAME_CARD       = "15"; // MUA THE GAME
        public final static String BUY_OTHER           = "16"; // MUA KHAC


        public final static String GET_CURRENT_BAL           = "17"; // kiem tra so du

        public final static String MUA_123  = "19";

        //build pay
        public final static String BIULDPAY = "topup";
        //....
    }

    public static class Partner{

        private static HashMap<String,String> hashMap = new HashMap<String,String>();
        static {
            hashMap.put("ZXU","ZingXu");
            hashMap.put("VCO","	Vcoin");
            hashMap.put("BAC","	Bac");
            hashMap.put("ONC","	Oncash Đà Đẵng");


            //Chon nha cung cap ( VAS Name)
            hashMap.put("EVNP","VinaPhone");
            hashMap.put("EMOB","MobiFone");

            //Chon nha cung cap( VAS Name)
            hashMap.put("EZXU","ZingXu");
            hashMap.put("EVCO","Vcoin");
            hashMap.put("EBAC","Bac");
            hashMap.put("EONC","Oncash Đà Đẵng");

            //Chon nha mang( Biller Name)
            hashMap.put("VNPP","Vinaphone");
            hashMap.put("MOFP","MobiFone");
            hashMap.put("VPTP","Cố định VNPT");

            //Chon khu vuc( Area Name)
            hashMap.put("HCM","HCM");
            hashMap.put("HNO","Hà Nội");
            hashMap.put("DNA","	Đà Nẵng");


            //Chon nha cung cap( Biller Name)
            hashMap.put("VPTA","VNPT");
            hashMap.put("FPTA","FPT");

            //Chon hang bay( Biller Name)
            hashMap.put("VNAL","Vietnam Airline");
            hashMap.put("JSAL","JetStar");
            hashMap.put("MKAL","Mekong Air");
            hashMap.put("VJAL","Vietjet Air");

            //Chon nha cung cap ve( Biller Name)
            hashMap.put("MLTK","Mai Linh Express");
            hashMap.put("PTTK","Phuong Trang");
            hashMap.put("TBTK","Thành Bưởi");
            hashMap.put("MGTK","Megastar");
            hashMap.put("GXTK","Galaxy");
            hashMap.put("LTTK","Lotte Cinema");

            //Diem ban ve (Biller Name)
            hashMap.put("HCMT","HCM");
            hashMap.put("HNOT","Ha Noi");
            hashMap.put("DNAT","Đà Nẵng");

            //Chon don vi bao hiem(Biller Name)
            hashMap.put("PRUI","Prudential");
            hashMap.put("MANI","Manulife");
            hashMap.put("AAAI","AAA");
            hashMap.put("LIBI","Liberty Mutual");
            hashMap.put("BMII","Bao Minh");
            hashMap.put("AIAI","AIA");
        }
        public static String getProviderName(String billerId){
            String t = billerId.trim().toUpperCase();
            String result = hashMap.get(t) == null ? billerId : (String)hashMap.get(t);
            if(result.equalsIgnoreCase("")){
                System.out.println("Provider name not found with billerid :" + billerId);
            }
            return result;
        }

    }
}
