package com.mservice.momo.vertx.merchant.lotte.entity;

/**
 * Created by duyhuynh on 31/03/2016.
 */
public enum MerErrorCode {
    Success(0, "Thanh toán thành công"),
    PhoneNumberNotFound(3, "Tài khoản Khách hàng không tồn tại. Vui lòng kiểm tra thông tin hoặc liên hệ (08) 399 171 99 để được hỗ trợ."),
    WrongAmount(25, "Số tiền không đúng"),
    NotExistsMerchant(47, "Không tồn tại đối tác"),
    WrongSourceMoney(73, "Nguồn tiền không đúng"),
    TokenIsExist(508, "Token đã được sử dụng"),
    GetRequestSuccessButError(1006, "Nhận được yêu cầu nhưng thực thi lỗi"),
    HashIsNull(1008, "Hash không có dữ liệu"),
    HashWrongSessionKey(1011, "Giải mã hash sai sessionKey"),
    WrongRequest(1020, "Nhận được yêu cầu sai"),
    WrongPublicKey(1024, "Public key không đúng"),
    InternalError(1025, "Internal error"),
    TransferError(1026, "Transfer error"),
    WrongUserPass(1027, "ID đăng nhập hoặc mật khẩu không chính xác."),
    AlreadyPay(1028, "Mã thanh toán đã được sử dụng. Vui lòng hướng dẫn KH cập nhật lại mã thanh toán và thực hiện lại giao dịch."),
    PromoCodeNotExit(1029, "Mã khuyến mãi không chính xác."),
    PromoCodeUsed(1030, "Mã khuyến mãi đã sử dụng."),
    PromoCodeExpired(1031, "Mã khuyến mãi đã hết hạn sử dụng."),
    HashNull(1032, "Hash sau khi giải mã không có dữ liệu"),
    CompareHashWrong(1033, "Hash sau khi giải mã có thông tin không đúng"),
    DecryptPhoneFail(1034, "Giải mã số điện thoại thất bại"),
    DecryptPaymentCodeException(1035, "Lỗi xảy ra khi giải mã mã thanh toán"),
    SessionKeyNotExits(1042, "SessionKey không tồn tại"),
    PaymentCodeWrong(1036, "Mã thanh toán không chính xác. Vui lòng hướng dẫn KH cập nhật lại mã thanh toán và thực hiện lại giao dịch."),
    SessionKeyExpire(1037, "Phiên làm việc hết hiệu lực. Vui lòng đăng nhập lại"),
    WrongStoreId(1038, "ID không thể đăng nhập tại chi nhánh này."),
    AmountOverquota(1039, "Vượt quá hạn mức thanh toán Khách hàng đã chọn. Vui lòng hướng dẫn KH cài đặt hạn mức lớn hơn để tiếp tục thanh toán."),
    LoginAnotherDevice(1040, "Tài khoản đã được đăng nhập ở thiết bị khác"),
    PaymentCodeExpire(1041, "Mã thanh toán hết hạn sử dụng. Vui lòng hướng dẫn KH cập nhật lại mã thanh toán để thực hiện giao dịch."),
    PinHashError(1043, "Giả mã password bị lỗi");

    private int c;
    private String d;
    MerErrorCode(int code, String desc) {
        c = code;
        d = desc;
    }

    public int getCode() {
        return c;
    }

    public String getDesc() {
        return d ;
    }
}
