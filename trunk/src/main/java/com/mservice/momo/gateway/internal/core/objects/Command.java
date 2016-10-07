package com.mservice.momo.gateway.internal.core.objects;

/**
 * Created by concu on 9/26/14.
 */
public class Command {
    public static final String ADJUST = "adjust";       //adjust
    public static final String TRANSFER = "transfer";   //lock tien
    public static final String COMMIT ="commit";        //commit giao dich
    public static final String ROLLBACK ="rollback";    //rollback giao dich
    public static final String BALANCE ="balance";      //lay so du
    public static final String REGISTER ="register";    //dang ky
    public static final String TRAN_WITH_VOUCHER_AND_POINT ="tranWithVoucherAndPoint";      //thuc hien giao dich co su dung point va voucher : topup va billpay
    public static final String MOMO_MONEY_TO_VOUCHER ="momoMoneyToVoucher";
    public static final String VOUCHER_TOPOINT ="voucherToPoint";
    public static final String BILL_PAY = "billPay";
    public static final String TOPUP = "topup";
    public static final String TRANSFER_WITH_LOCK = "transferwithlock";
    public static final String VOTE = "vote";
    public static final String RETAILER_TRANSFER_CASH = "doC2C";
    public static final String RETAILER_TRANSFER_CASH_RECOMMIT = "retailerTransferCashRecommit";
    public static final String RETAILER_TRANSFER_CASH_COMMIT = "retailerTransferCashCommit";
}
