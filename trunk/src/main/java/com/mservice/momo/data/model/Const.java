package com.mservice.momo.data.model;

/**
 * Created by concu on 5/5/14.
 */
public class Const {
    public static final String SET_TRAN_SERVICE_STATUS = "settranservicestatus";
    public static final String TASK_CONFIG = "config";
    public static final int ALL_TRANSACTION = 0;
    public static final String GET_ALL_TRAN_SERVICE_STATUS = "getalltranservicestatus";
    //channel, to seperate request from mobi app or web app
    public static final String CHANNEL_WEB = "web";
    public static final String CHANNEL_MOBI = "mobi";
    public static String REFUND_AMOUNT = "refundamount";
    public static String REFUND_AGENT = "refundagent";
    public static String PROMO_CODE = "promocode";
    public static String INVITE_FRIEND = "invitefriend";
    public static String SERVICE_NAME = "service_name"; // phan biet chuyen tien muon tien, chuyen tien qua vi MOMO, qua banknet, qua bank linked
    public static String PARTNER_ID = "partner_id";
    public static String SERCICE_MODE = "service_mode"; // phan biet chuyen tien thanh toan cho dich vu nao, vidu QRCode
    public static String SERVICE_ID = "serviceid";
    public static String REFERAL = "referal"; // tai khoan nguoi gioi thieu
    public static String BONUSFORREFERAL = "bonusforreferal"; // so tien bonus cho tai khoan nay
    public static String OLD_NUMBER = "oldnumber";

    public static enum ServiceType {
        DEPOSIT, WITHDRAW
    }

    public static enum FromSource {
        APP_MOBI, WEB, OTHER
    }

        /*
        misc:key>refundamount</misc:key>
        <misc:value>1000</misc:value>
        </misc:keyValuePairs>
        <misc:keyValuePairs>
        <misc:key>refundagent</misc:key>
        <misc:value>vtb2vcb</misc:value>
        */

    //dinh nghia cac keyvaluepair danh rieng cho DGD
    public static class DGD {
        public static final String IsNamed = "isnamed"; // "0": chua dinh danh, "1": dinh danh
        public static final String CusNumber = "cusnum"; // moi giao dich cua DGD deu co thong tin nay
        public static final String Retailer = "dgd";
        public static final String Img = "image";
        public static final String District = "district";
        public static final String City = "city";
        public static final String CusName = "cusname";
        public static final String CusCardId = "cuscardid";
        public static final String CusEmail = "cusemail";
        public static final String GroupId = "coreGroupId";
    }

    //dinh nghia cac key cho keyvaluepair de giao tiep voi core connector
    public static class CoreVC {
        public static final String Recipient = "recipient";
        public static final String IsSms = "issms";
        public static final String NoSms = "no";
        public static final String Client = "client";
        public static final String Backend = "backend";
        public static final String ServiceType = "servicetype";
        public static final String Account = "account";
        public static final String M2mTpype = "m2mtype";
    }

    public static class M2MTypeVals {
        public static final String M2n = "m2n";
        public static final String Pay123 = "123pay";
        public static final String Banknet = "banknet";
    }


    public static class DGDValues {
        public static final int IsNamed = 1;
        public static final int Retailer = 1;
        //"isnamed" : "1" --> DGD dinh danh vi cho khach hang
        //"cusnum"   : "0974540385" -->vi DGD muon dinh danh
        //"dgd"      : "0" use thuong , "1" DGD
    }

    public static class SmsKey {
        public static final String Topup = "topup";
        public static final String M2m = "topup";
        public static final String Invoice = "invoice";
        public static final String Service = "service";
    }

    public static class SmsField {
        public static final String Amount = "amount";
        public static final String Retailer = "retailer";
        public static final String VnTime = "vntime";
        public static final String TranId = "tranId";
        public static final String BillId = "billId";
    }

    public static class CoreTranType {
        public static final String Topup = "buy";
        public static final String Billpay = "billpay";
    }

    public static class VoucherPointType {
        public static final int PointAndVoucher = 1;
        public static final int OnlyVoucher = 2;
        public static final int OnlyPoint = 3;
    }

    public static class CoreProcessType {
        public static final int OneStep = 1; // core thuc hien lenh 1 buoc
        public static final int TwoStep = 2; // core thuc hien 2 buoc  : 1-lock tien 2-confirm
    }

    public static class ValidBill {
        public static final String ServiceId = "sid";
        public static final String BillId = "billid";
    }

    public static class AppClient {
        public static final String Qrcode = "qrcode";
        public static final String Html = "html";
        public static final String ServiceId = "sid";
        public static final String Id = "id";
        public static final String Referal = "referal";
        public static final String NextForm = "nexfrm";
        public static final String Caption = "cap";
        public static final String Button = "btn";
        public static final String Error = "err";
        public static final String BillId = "billid";
        public static final String Amount = "amt";
        public static final String Phone = "sdt";
        public static final String Address = "dc";
        public static final String ServiceName = "sn";
        public static final String FullName = "ht";
        public static final String Success = "ok";
        public static final String Quantity = "qty";
        public static final String Account = "acc";
        public static final String GroupId = "gid";
        public static final String TotalMomo = "totalmomo";
        public static final String Fee = "fee";
        public static final String Desciption = "desc";
        public static final String NewComment = "newcomment";
        public static final String NewAmount = "newamt";

        public static final String OldComment = "oldcomment";
        public static final String OldAmount = "oldamt";
        public static final String SourceFrom = "srcfrom";
        public static final String Source = "source";

        public static final String Email = "email";

        public static final String Stores = "dgd";
        public static final String ServiceType = "servicetype";
        public static final String Share = "share";

    }

    public static class C2C {

        public static String senderName = "sndName";
        public static String senderPhone = "sndPhone";
        public static String senderCardId = "sndCardId";

        public static String receiverName = "rcvName";
        public static String receiverPhone = "rcvPhone";
        public static String receiverCardId = "rcvCardId";

        public static String retailerAddress = "retailerAddr";
        public static String retailerPhone = "retailerPhone";

    }

    public static class INTERNAL_HTTP_POST_CFG {
        public static String CONFIG_NAME = "HttpPostConnectorVerticle";
        public static String IS_SERVER = "isServer";
        public static String HOST = "host";
        public static String PORT = "port";
        public static String FORM_EU_HOST = "formEUHost";
        public static String FORM_EU_PORT = "formEUPort";
        public static String FORM_DGD_HOST = "formDGDHost";
        public static String FORM_DGD_PORT = "formDGDPort";
        public static String BUS = "BUS";
        public static String DATA = "DATA";
    }

    public static class MYSQL {
        public static final int TIMEOUT = 60000;
    }

}
