package com.mservice.momo.util.claimcode;

import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.processor.Misc;

/**
 * Created by concu on 4/4/16.
 */
public class ClaimCodeUtils {

    public static String checkClaimCodeProgram(String code)
    {
        String program = "";
        //Kiem tra xem co phai chuong trinh referral khong
        //String[] code_tmp = code.split(StringConstUtil.ReferralVOnePromoField.SPLIT_SYMBOL);
        //if(code_tmp.length > 1 && DataUtil.strToInt(code) > 0 && Misc.checkNumber(DataUtil.strToInt(code))) StringConstUtil.ReferralVOnePromoField.REFERRAL_PREFIX.equalsIgnoreCase(code_tmp[0].toString().trim()))
        String firstChar = String.valueOf(code.charAt(0));
        if(code.length() > 9 && DataUtil.strToInt(code) > 0 && Misc.checkNumber(DataUtil.strToInt(code)) && firstChar.equalsIgnoreCase("0"))
        {
            //Code Referral
            program = StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM;
        }
        else {
            program = StringConstUtil.ClaimCodePromotion.CLAIM_PROGRAM;
        }
        return program;
    }


}
