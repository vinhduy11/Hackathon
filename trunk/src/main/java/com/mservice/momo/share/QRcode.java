package com.mservice.momo.share;

import com.mservice.momo.msg.MomoMessage;

/**
 * Created by concu on 11/19/14.
 */
public class QRcode {

    public static class ClientScreen{
        public static final int TOPUP     = 3;
        public static final int M2M       = 6;
        public static final int SERVICE   = 7;
        public static final int INVOICE   = 19;
    }

    public static class StatusAction{
        public static final int VIA_SCREEN = 0;
        public static final int IMMEDIATE = 1;


        //7 + bell
        // + SERVICEID+ bell
        // + amount+ bell
        // + comment+ bell
        // + billId + bell
        // + "<status=0 hoặc 1>"

    }

    public static String getQrcode(int clientScreen, String serviceId, long amount, String comment, String billId, int statusAction){
        String tpm = "%s" + MomoMessage.BELL
                    +"%s" + MomoMessage.BELL
                    +"%s" + MomoMessage.BELL
                    +"%s" + MomoMessage.BELL
                    +"%s" + MomoMessage.BELL
                    +"%s" ;
        return String.format(tpm,clientScreen,serviceId,String.valueOf(amount),comment,billId,statusAction);
    }


}
