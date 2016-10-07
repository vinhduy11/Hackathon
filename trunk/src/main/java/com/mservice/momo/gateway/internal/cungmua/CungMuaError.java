package com.mservice.momo.gateway.internal.cungmua;

import java.util.HashMap;

/**
 * Created by duy.huynh on 08/06/2015.
 */
public class CungMuaError {
    public static final int SUCCESS = 0;//	success
    public static final int CITY_LIST_EMPTY = 1;// City list is empty
    public static final int CATEGORY_LIST_EMPTY = 2;// City list is empty
    public static final int INVALID_PARAMETERS = 1000; // Dữ liệu vô không hợp lệ
    public static final int SYSTEM_ERROR = 1001; //	Lỗi hệ thống

    private static HashMap<Integer, String> CungMuaErrorMap = new HashMap<>();

    static {
        CungMuaErrorMap.put(SUCCESS, "Success");
        CungMuaErrorMap.put(CITY_LIST_EMPTY, "Danh sách thành phố rỗng");
        CungMuaErrorMap.put(CATEGORY_LIST_EMPTY, "Danh mục rỗng");
        CungMuaErrorMap.put(INVALID_PARAMETERS, "Invalid parameters");
        CungMuaErrorMap.put(SYSTEM_ERROR, "System error");
    }

    public static String getDesc(int errorCode){
        String s = CungMuaErrorMap.get(errorCode);
        if(s == null){
            return "not defined description for " + errorCode;
        }
        return s;
    }
}
