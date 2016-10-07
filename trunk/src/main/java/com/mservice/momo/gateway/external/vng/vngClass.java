package com.mservice.momo.gateway.external.vng;

/**
 * Created by concu on 6/17/14.
 */
public class vngClass {

    public static String PHONE_NUMBER ="phonenumber";
    public static String TIME ="time";
    public static class Create{
        /*Method:POST
        Request:
        Param(session_id
        ,customer_name
        ,customer_email
        ,customer_phone
        ,list_seat
        ,client_id
        ,device_i
        )*/
        public static String session_id = "session_id";
        public static String customer_name = "customer_name";
        public static String customer_email = "customer_email";
        public static String customer_phone = "customer_phone";
        public static String list_seat = "list_seat";
        public static String client_ip = "client_ip";
        public static String device_id = "device_id";
        public static String phone_number="phonenumber";
        public static String time = "time";
        public static String session_time = "session_time";
        public static String film_name = "film_name";
        public static String galaxy_info ="galaxy_info";
    }

    public static class ConfirmOrCancel{
        public static String invoice_no = "invoice_no";
        public static String payment_code = "payment_code";
        public static String phone_number="phonenumber";
        public static String time = "time";
    }

    public static class Res{

        public static String error="error";
        public static String desc="desc";
        public static String status ="status";
        public static String message ="message";
        //create
        public static String invoice_no = "invoice_no";
        public static String ticket_code ="ticket_code";
        public static String price_before ="price_before";
        public static String price_after ="price_after";
        public static String list_price ="list_price";
        public static String session_id = "session_id";
        public static String session_time ="session_time";
        public static String film_name ="film_name";
        public static String status_id = "status_id";
        public static String status_desc = "status_desc";


        //confirm
        public static String date_add = "date_add";
        public static String date_confirm = "date_confirm";
        //cancel
        public static String date_cancel = "date_cancel";

        //create
        /*"invoice_no": "20140616130734741451",
                "ticket_code": "10114061664329",
                "price_before": "100000",
                "price_after": "100000",
                "list_price": [{"A01":50000}, {"A02":50000}]*/

        //confirm
        /*"ticket_code":10113060354467,
        "date_add":"2013-06-03 15:53:35",
        "date_confirm":"2014-06-16 11:41:00",*/

        //cancel
        /*"invoice_no":20130603155335979315,
        "date_cancel":"2013-06-03 15:53:35",*/

    }

    public static class ResError{
        //{"error_code":2100,"error_description":"Could not get ticket code"}
        public static String error_code = "error_code";
        public static String error_description = "error_description";
    }


}
