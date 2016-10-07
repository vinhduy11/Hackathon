package com.mservice.momo.gateway.external.banknet.obj;

/**
 * Created by User on 3/11/14.
 */
public class BanknetRequest {

    //SendGood_ext
    public String 	merchant_trans_id = "";
    public String 	 merchant_code = "";
    public String 	 country_code= "";
    public String 	good_code= "";
    public String 	 xml_description = "";
    public String 	 net_cost= "";
    public String 	ship_fee= "";
    public String 	 tax= "";
    public String 	 trans_datetime= "";
    public String 	selected_bank= "";
    public String 	 trans_secure_code= "";
    public String 	 customer_msisdn= "";
    public String 	merchant_trans_key= "";

    //CheckCardHolder
    public String 	trans_id = "";
    public String 	card_holder_number = "";
    public String 	card_holder_name= "";
    public String 	card_holder_month= "";
    public String 	card_holder_year= "";
    public String 	otpGetType = "";
    public String    otpCode="";
    public long      ms_transid = 0;

}
