package com.mservice.momo.gateway.internal.walletmapping;

/**
 * Created by duyhuynh on 07/06/2016.
 */
public class WalletMappingConst {

    public final static String requestType = "requestType";
    public final static String bankCode = "bankCode";
    public final static String bankAccountNo = "bankAccountNo";
    public final static String bankAccountName = "bankAccountName";
    public final static String personalId = "personalId";
    public final static String otp = "otp";
    public final static String cardCreateAt = "cardCreateAt";
    public final static String cardId = "cardId";
    public final static String bankId = "bankId";
    public final static String bankName = "bankName";

    public static class ActionType {
        public final static String VERIRY = "verify";
        public final static String MAP = "confirm_map";
        public final static String UNMAP = "unmap";
    }
}
