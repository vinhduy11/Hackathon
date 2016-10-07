package com.mservice.momo.gateway.internal.visamaster;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by khoanguyen on 18/03/2015.
 */
public class VMFeeType {

    public static final int DEPOSIT_FEE_TYPE = 0; // phi nap tien.

    public static final int TRANSFER_FEE_TYPE = 1; //phi chuyen khoang

    public static final String SBS_DEPOSIT = "sbsdeposit";

    public static final String SBS_TRANSFER = "sbstransfer";

    public static String getBankId(int feeType)
    {

        String bankId = "";
        switch (feeType)
        {
            case DEPOSIT_FEE_TYPE:
                bankId = SBS_DEPOSIT;
                break;
            case TRANSFER_FEE_TYPE:
                bankId = SBS_TRANSFER;
                break;
            default:
                break;
        }
                return bankId;
    }

    public static long calculateFeeMethod(int staticFee, double dynamicFee, long transAmount)
    {
        long fee = 0;
//        fee = Math(((long) Math. (staticFee + (dynamicFee / 100) * transAmount)));
        try
        {
            NumberFormat formatter = new DecimalFormat("#0.00");
            String amount = formatter.format(staticFee + (dynamicFee / 100) * transAmount);
            fee = (long) Math.ceil(Double.parseDouble(amount));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fee = (long) Math.ceil(Double.parseDouble(String.valueOf((staticFee + (dynamicFee / 100) * transAmount))));
        }
        return fee;
    }
}

