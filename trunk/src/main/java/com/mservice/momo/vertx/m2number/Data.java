package com.mservice.momo.vertx.m2number;

/**
 * Created by concu on 12/9/14.
 */

public class Data{

    public static class Cap{
        public static String remain24h = "Còn 24 giờ để nhận tiền";
        public static String remain12h = "Còn 12 giờ để nhận tiền";
        public static String expired = "Gửi tiền không thành công";
    }

    public static class Body{
        //Số điện thoại <số ĐT người nhận> mà bạn gửi <số tiền> đ vẫn chưa cài ứng dụng MoMo để nhận tiền. Số tiền đó sẽ được hoàn trả lại bạn sau 24 giờ tới. Vui lòng nhắc <số ĐT người nhận> cài MoMo để nhận tiền.
        public static String remain24h = "Số điện thoại %s mà bạn gửi %s đ vẫn chưa cài ứng dụng MoMo để nhận tiền. Số tiền đó sẽ được hoàn trả lại bạn sau 24 giờ tới. Vui lòng nhắc %s cài MoMo để nhận tiền.";
        //Số điện thoại <số ĐT người nhận> mà bạn gửi <số tiền> đ vẫn chưa cài ứng dụng MoMo để nhận tiền. Số tiền đó sẽ được hoàn trả lại bạn sau 12 giờ tới. Vui lòng nhắc <số ĐT người nhận> cài MoMo để nhận tiền.
        public static String remain12h = "Số điện thoại %s mà bạn gửi %s đ vẫn chưa cài ứng dụng MoMo để nhận tiền. Số tiền đó sẽ được hoàn trả lại bạn sau 12 giờ tới. Vui lòng nhắc %s cài MoMo để nhận tiền.";
        //Giao dịch chuyển tiền cho <số ĐT người nhận> bị hủy vì người nhận vẫn chưa cài ứng dụng MoMo để nhận tiền. Số tiền <Số tiền> đ sẽ được hoàn trả về tài khoản của bạn. Xin gọi CSKH (0839917199) nếu cần hỗ trợ.
        public static String expired = "Giao dịch chuyển tiền cho %s bị hủy vì người nhận vẫn chưa cài ứng dụng MoMo để nhận tiền. Số tiền %s đ sẽ được hoàn trả về tài khoản của bạn. Xin gọi CSKH (0839917199) nếu cần hỗ trợ.";
    }

    public static String link = "http://app.momo.vn/";
    public static String share = "Minh vua chuyen cho ban %sd. Truy cap http://app.momo.vn:8081/%s de nhan tien trong 48 gio hoac goi 0839917199 de duoc ho tro.";
}
