package com.mservice.momo.util;

import com.mservice.momo.vertx.processor.Misc;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * Created by hung_thai on 9/26/14.
 */
public class CodeUtil {


    public static final String sHash = "5LCRP76NY2VID9AZBXEG8KH3FTM4S";
    public int ORG_LEN_OF_CODE = 6; // chieu dai code dau tien sinh ra
    public int TOTAL_LEN_OF_CODE = 7; // tong chieu dai cua code

    public CodeUtil(int orgLen, int totalLen){
        ORG_LEN_OF_CODE = orgLen;
        TOTAL_LEN_OF_CODE = totalLen;
    }

    public CodeUtil(){
        ORG_LEN_OF_CODE = 6; // chieu dai code dau tien sinh ra
        TOTAL_LEN_OF_CODE = 7; // tong chieu dai cua code
    }

    public boolean isCodeValid(String code, String begin){

        if (code == null) {
            return false;
        }
        code = code.toUpperCase();
        if (code.startsWith(begin)) {
            code = code.substring(begin.length());
        }
        if (code.length() != TOTAL_LEN_OF_CODE) {
            return false;
        }

        return code.charAt(ORG_LEN_OF_CODE) == Misc.getHash(code.substring(0,ORG_LEN_OF_CODE), sHash);
    }

    public  String getNextCode(){
        //todo something here to return the code

        String[] sFullSet = new String[]{
                "MCPG49XIR65AFTBLZ3K2DY7NVHE8S",
                "CA4BVRZDPEI9M2T7L6HG8YX5NFS3K",
                "H3ZMA7KDFG84I5ES6TV2CN9YRXLBP",
                "BAT5P9MKXH7YNDZFV2L48CSR6GE3I",
                "KZ5RAHGMYTX4P6792NFCLID3S8VEB",
                "8RKDMVSFB4PZ5E96XNIT7GH3C2LAY",
                "5LCRP76NY2VID9AZBXEG8KH3FTM4S",
                "S794CIDKFNBX2TVHYGELMZ5863RPA",
                "KCLS6XY239EDM8GBT7NHVA5F4ZPIR"
        };
        Random r = new Random();
        int ranNumber = r.nextInt(8);

        String sGen = sFullSet[ranNumber];//"MCPG49XIR65AFTBLZ3K2DY7NVHE8S";

        String random = RandomStringUtils.random(ORG_LEN_OF_CODE, sGen);
        String result="";
        result = random + Misc.getHash(random, sHash);
        return result;
    }
}
