package com.mservice.momo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nam on 7/4/14.
 */
public class Phim123Errors {

    private static final Map<Integer, String> desc;

    static {
        desc = new HashMap<>();

        desc.put(0, "Thành công.");

        desc.put(-11,"Giá và loại của ghế mới phải bằng ghế cũ.");
        desc.put(-1,"Hệ thống đang bảo trì.");
        desc.put(10,"Dữ liệu không hợp lệ. Hãy kiểm tra và thử lại.");
        desc.put(11, "Email không hợp lệ.");
        desc.put(12,"Số điện thoại không hợp lệ hoặc đầu số không được hỗ trợ.");

        desc.put(20,"Mã OTP không đúng.");
        desc.put(21,"Xác nhận OTP vượt quá giới hạn trên ngày.");
        desc.put(22,"Số điện thoại không được hỗ trợ.");
        desc.put(1400,"Ghế đã có người đặt.");
        desc.put(1000,"Không tìm thấy giao dịch.");
        desc.put(1001,"Không thể tạo đơn hàng. Hãy thử lại.");
        desc.put(1003,"Không thể hủy đơn hàng. Hãy thử lại.");
        desc.put(1004,"Bạn có giao dịch đang chờ xử lý.");
        desc.put(1005,"Bạn có giao dịch đang xử lý. Vui lòng đợi trong tối đa 15 phút trước khi thực hiện thao tác khác nhằm tránh trùng lặp giao dịch.");
        desc.put(1500,"Không thể tạo đơn hàng. Hãy thử lại.");
        desc.put(1501,"Lỗi khi xử lý thanh toán. Hãy thử lại.");
        desc.put(1502,"Không tìm thấy mã thanh toán. Hãy thử lại.");
        desc.put(1503,"Hệ thống bận, không thể xác nhận thẻ. Hãy thử lại.");
        desc.put(1504,"Hệ thống bận, không thể xác nhận OTP. Hãy thử lại.");
        desc.put(1505,"Có lỗi khi giao tiếp hệ thống thanh toán.");
        desc.put(1100,"Không tìm thấy suất chiếu.");
        desc.put(1101,"Suất chiếu không tồn tại hoặc đã hết thời gian đặt vé.");
        desc.put(1200,"Rạp không tồn tại.");
        desc.put(1300,"Phim không tồn tại.");
        desc.put(1600,"Bình luận không tồn tại.");
        desc.put(1700,"Không thể lấy trailer của phim. Hãy thử lại.");
        desc.put(1800,"Không tìm thấy sơ đồ rạp.");
        desc.put(1900,"Có lỗi khi giao tiếp với hệ thống rạp BHD Star Cineplex.");
        desc.put(2000,"Không tìm thấy giá vé hoặc giá vé chưa được cập nhật.");
        desc.put(2100,"Ghế đã có người đặt, vui lòng chọn ghế khác.");
        desc.put(2200,"Không tìm thấy thông tin khách hàng. Hãy thử lại.");
        desc.put(2201,"Không tìm thấy thông tin khách hàng.");
        desc.put(9000,"Có lỗi khi giao tiếp với hệ thống rạp.");
        desc.put(9001,"Có lỗi khi giao tiếp với hệ thống rạp.");
        desc.put(9002,"Rạp chưa hỗ trợ mua vé online trên 123Phim.");
        desc.put(9100,"Có lỗi khi giao tiếp với hệ thống rạp Cinebox.");

        desc.put(401, "Không thể chứng thực từ 123Phim vào lúc này.");
        desc.put(411, "LENGTH HEADER chưa đúng.");
        desc.put(1002, "Transaction already confirmed");
        desc.put(-4992,"Request timeout");

    }

    public static String getDesc(int code) {

        if(code > 0){code = code -10000;}
        String str = desc.get(code);
        if(str == null){
            return "Not defined for " + code;
        }
        return str;
    }

}
