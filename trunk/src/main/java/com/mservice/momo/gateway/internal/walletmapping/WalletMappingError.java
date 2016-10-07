package com.mservice.momo.gateway.internal.walletmapping;


import java.util.HashMap;

/**
 * Created by duyhuynh on 07/06/2016.
 */
public class WalletMappingError {

    public static final int SUCCESS = 0;//	success
    public static final int INVALID_BANK_CODE = -1;
    public static final int INVALID_CARD_NUMBER = -2;
    public static final int INVALID_CARD_HOLDER_NAME = -3;
    public static final int INVALID_PERSIONAL_ID = -4;
    public static final int INVALID_CARD_CREATE_TIME = -5;
    public static final int TIMEOUT = -6;
    public static final int INVALID_PARAMETERS = -7;
    public static final int UPDATE_PHONE_ERROR = -8;
    public static final int MISSING_OTP = -9;
    public static final int ADD_CARD_ERROR = -10;
    public static final int DEL_CARD_ERROR = -11;

    private static HashMap<Integer, String> VisaErrorMap = new HashMap<>();

    static {
        VisaErrorMap.put(SUCCESS, "Thành công");
        VisaErrorMap.put(INVALID_BANK_CODE, "Mã ngân hàng không hợp lệ.");
        VisaErrorMap.put(INVALID_CARD_NUMBER, "Số thẻ không hợp lệ.");
        VisaErrorMap.put(INVALID_CARD_HOLDER_NAME, "Tên chủ thẻ không hợp lệ.");
        VisaErrorMap.put(INVALID_PERSIONAL_ID, "Số CMND chủ thẻ không hợp lệ.");
        VisaErrorMap.put(INVALID_CARD_CREATE_TIME, "Ngày mở thẻ không hợp lệ.");
        VisaErrorMap.put(TIMEOUT, "Hết hạn yêu cầu.");
        VisaErrorMap.put(INVALID_PARAMETERS, "Tham số không hợp lệ.");
        VisaErrorMap.put(UPDATE_PHONE_ERROR, "Cập nhật thông tin người dùng lỗi.");
        VisaErrorMap.put(MISSING_OTP, "Thiếu OTP.");
        VisaErrorMap.put(ADD_CARD_ERROR, "Thêm thẻ lỗi.");
        VisaErrorMap.put(DEL_CARD_ERROR, "Xóa thẻ lỗi.");
    }

    public static String getDesc(int errorCode){
        String s = VisaErrorMap.get(errorCode);
        if(s == null){
            return "";
        }
        return s;
    }
}
