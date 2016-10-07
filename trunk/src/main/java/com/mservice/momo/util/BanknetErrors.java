package com.mservice.momo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nam on 7/4/14.
 */
public class BanknetErrors {

    private static final Map<Integer, String> desc;

    static {
        desc = new HashMap<>();

        desc.put(-1, "Lỗi hệ thống");
        desc.put(0, "Thành công");
        desc.put(1, "merchant code không hợp lệ");
        desc.put(2, "chuỗi mã hóa không hợp lệ (trans_secure_code không hợp lệ)");
        desc.put(3, "merchant trans id không hợp lệ");
        desc.put(5, "Số tiền chủ thẻ không đủ thực hiện giao dịch");
        desc.put(6, "Lỗi xác nhận giao dịch: giao dịch đã được xác nhận (thành công / không thành công trước đó và không thể xác nhận lại)");
        desc.put(8, "Lỗi timeout trong quá trình xử lý");
        desc.put(10, "Ghi nhận giao dịch đơn hàng hợp lệ");
        desc.put(11, "net cost không hợp lệ");
        desc.put(12, "ship_fee không hợp lệ");
        desc.put(12, "Khách hàng hủy giao dịch");
        desc.put(13, "tax không hợp lệ");
        desc.put(14, "Merchant code chưa được cấu hình cho phép thanh toán");
        desc.put(15, "Sai mã NH");
        desc.put(16, "Số tiền thanh toán của đại lý không nằm trong khoảng cho phép");
        desc.put(17, "Tài khoản không đủ tiền");
        desc.put(18, "Ghi nhận đơn hàng, thông tin Key giao dịch không đúng");
        desc.put(19, "trans_datetime không hợp lệ");
        desc.put(19, "Mã Đại lý truyền vào không đúng");
        desc.put(20, "Giá trị OtpGetType không hợp lệ");
        desc.put(21, "Mật khẩu OTP không đúng");
        desc.put(24, "Không tìm thấy giao dịch trong hệ thống");
        desc.put(25, "Nhập sai thông tin chủ thẻ - Còn 2 lần nhập");
        desc.put(26, "Nhập sai thông tin chủ thẻ - Còn 1 lần nhập");
        desc.put(27, "Nhập sai thông tin chủ thẻ - Thanh toán không thành công");
        desc.put(41, "Thẻ nghi vấn (thẻ đánh mất, hot card)");
        desc.put(54, "Thẻ hết hạn");
        desc.put(57, "Chưa đăng ký dịch vụ thanh toán trực tuyến");
        desc.put(61, "Qúa hạn mức giao dịch trong ngày");
        desc.put(62, "Thẻ bị khóa");
        desc.put(65, "Qúa hạn mức 1 lần giao dịch");
        desc.put(97, "Ngân hàng chưa sẵn sàng");
        desc.put(98, "Giao dịch không hợp lệ");
        desc.put(99, "Lỗi không xác định");
        desc.put(100, "Chấp nhận yêu cầu Kiểm tra thông tin chủ Tài khoản");
        desc.put(101, "Chưa đăng ký dịch vụ thanh toán trực tuyến");
        desc.put(102, "Qúa hạn mức giao dịch trong ngày");
        desc.put(103, "Qúa hạn mức của 1 lần giao dịch");
        desc.put(104, "Giao dịch bị lặp");
        desc.put(105, "Thẻ bị khóa");
        desc.put(106, "Thẻ hết hạn");
        desc.put(107, "Lỗi khi thực hiện hạch toán");
        desc.put(108, "Thẻ nghi vấn (thẻ đánh mất, hot card)");
        desc.put(110, "Truy vấn thông tin chủ TK thành công");
        desc.put(111, "Truy vấn TK sai lần 1");
        desc.put(112, "Truy vấn TK sai lần 2");
        desc.put(113, "Truy vấn TK sai lần 3");
        desc.put(115, "Số dư chủ TK không đủ");
        desc.put(116, "Không nhận được phản hồi của Bank (Timeout)");
        desc.put(119, "Giao dịch kiểm tra thông tin chủ Tài khoản không hợp lệ");
        desc.put(200, "Yêu cầu xác thực thông tin OTP");
        desc.put(210, "Xác thực OTP thành công");
        desc.put(211, "Sai thông tin OTP");
        desc.put(217, "Không nhận đc phản hồi từ bank (Timeout)");
        desc.put(219, "Giao dịch không hợp lệ");
        desc.put(300, "Đại lý gửi yêu cầu xác thực kết quả giao dịch đơn hàng");
        desc.put(310, "Đại lý xác nhận giao dịch thành công, giao hàng thành công");
        desc.put(315, "Đại lý xác nhận giao dịch không thành công");
        desc.put(317, "Lỗi trong quá trình xác nhận giao hàng không thành công nhưng Bndebit vẫn đồng ý cho phép xác nhận giao hàng không thành công");
        desc.put(318, "Lỗi trong quá trình xác nhận giao hàng thành công nhưng Bndebit vẫn đồng ý cho phép xác nhận giao hàng thành công");
        desc.put(319, "Giao dịch xác thực không hợp lệ");
        desc.put(516, "Kết quả giao dịch thành công, nhưng KEY(F64) của Ngân hàng trả về không đúng");
        desc.put(519, "Trạng thái giao dịch không xác định – lỗi");

    }

    public static String getDesc(int errorCode) {
        String str = desc.get(errorCode);
        if(str == null){
            return "Not defined for " + errorCode;
        }
        return str;
    }
}
