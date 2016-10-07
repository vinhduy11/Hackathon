package com.mservice.momo.gateway.internal.visamaster;

import java.util.HashMap;

/**
 * Created by khoanguyen on 18/03/2015.
 */
public class VMError {

    public static final int SUCCESS = 0;//	success
    public static final int INVALID_USER_PASSWORD = 1;//	Invalid User Password
    public static final int INVALID_DATA_SIGN = 2;//	Invalid data sign
    public static final int ACCOUNT_NOT_EXIST = 3;//	Account does not exist
    public static final int ACCOUNT_REGISTERED_OTHER_BANK = 4;//	Account is registered in other bank
    public static final int ACCOUNT_REGISTERED_THIS_BANK = 5;//	The account has been already registered with this bank.
    public static final int ACCOUNT_NOT_REGISTERED = 6;//	The account is not registered with any bank.
    public static final int PASSWORD_CANNOT_DECRYPTED = 7;//	The password can’t be decrypted.
    public static final int ACCOUNT_CANNOT_REGISTERED = 8;//	The account can’t be Registered/ Unregistered because MoMo > 0. Please contact to M_Service.
    public static final int ACCOUNT_LOCKED = 9;//	The account is locked.
    public static final int SESSION_ID_EXPIRED = 10;//	The SessionId is expired.
    public static final int TRANSACTION_TIMEOUT = 11;//	The Transaction is timeout.
    public static final int OVER_LIMITED_TRANSACTION = 12;//	Over limited transaction per days.
    public static final int AMOUNT_INVALID = 13;//	Amount is invalid.
    public static final int TRANS_ID_DUPLICATE = 14;//	Duplicate TransId.
    public static final int SERVICE_UPGRADING = 15;//	The service is upgrading.
    public static final int INVALID_PARAMETERS = 16;//	Invalid Parameters.
    public static final int INSUFFICIENT_FUNDS = 17;//	Insufficient funds
    public static final int WALLET_BALANCE_EXCEEDED = 18;//	Wallet Balance Exceeded
    public static final int CHANNEL_CODE_INVALID = 19;//	ChannelCode is invalid
    public static final int WEB_SERVICE_ERROR = 30;//	Webservice Error


    private static HashMap<Integer, String> VisaErrorMap = new HashMap<>();

    static {
        VisaErrorMap.put(SUCCESS, "success");
        VisaErrorMap.put(INVALID_USER_PASSWORD, "Invalid Username / Password.");
        VisaErrorMap.put(INVALID_DATA_SIGN, "Invalid  DataSign.");
        VisaErrorMap.put(ACCOUNT_NOT_EXIST, "The account doesn’t exist in M_Service system.");
        VisaErrorMap.put(ACCOUNT_REGISTERED_OTHER_BANK, "The account has been already registered with another bank.");
        VisaErrorMap.put(ACCOUNT_REGISTERED_THIS_BANK, "The account has been already registered with this bank.");
        VisaErrorMap.put(ACCOUNT_NOT_REGISTERED, "The account is not registered with any bank.");
        VisaErrorMap.put(PASSWORD_CANNOT_DECRYPTED, "The password can’t be decrypted.");
        VisaErrorMap.put(ACCOUNT_CANNOT_REGISTERED, "The account can’t be Registered/ Unregistered because MoMo > 0. Please contact to M_Service.");
        VisaErrorMap.put(ACCOUNT_LOCKED, "The account is locked.");
        VisaErrorMap.put(SESSION_ID_EXPIRED, "The SessionId is expired.");
        VisaErrorMap.put(TRANSACTION_TIMEOUT, "The Transaction is timeout.");
        VisaErrorMap.put(OVER_LIMITED_TRANSACTION, "Over limited transaction per days.");
        VisaErrorMap.put(AMOUNT_INVALID, "Amount is invalid.");
        VisaErrorMap.put(TRANS_ID_DUPLICATE, "Duplicate TransId.");
        VisaErrorMap.put(SERVICE_UPGRADING, "The service is upgrading.");
        VisaErrorMap.put(INVALID_PARAMETERS, "Invalid Parameters.");
        VisaErrorMap.put(INSUFFICIENT_FUNDS, "Insufficient funds");
        VisaErrorMap.put(WALLET_BALANCE_EXCEEDED, "Wallet Balance Exceeded");
        VisaErrorMap.put(CHANNEL_CODE_INVALID, "ChannelCode is invalid");
        VisaErrorMap.put(WEB_SERVICE_ERROR, "Webservice Error");
    }

    public static String getDesc(int errorCode){
        String s = VisaErrorMap.get(errorCode);
        if(s == null){
            return "not defined description for " + errorCode;
        }
        return s;
    }
}
