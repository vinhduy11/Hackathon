package com.mservice.momo.vertx.event.bnhv;

/**
 * Created by concu on 12/31/14.
 */
public class Noti {
    public static class Cap {
        public static String VoteOk = "Bình chọn thành công";
        public static String NotOpened = "Hệ thống chưa mở";
        public static String Closed = "Hệ thống đã đóng";
        public static String NotEnoughCash = "Không đủ tiền bình chọn";
        public static String OverAmount = "Quá số lượng bình chọn";
        public static String PayBack24 = "Hoàn tiền sau 24 giờ";
        public static String SystemRecieved = "Hệ thống đã ghi nhận";
        public static String PayBackComepleted = "Hoàn tiền bình chọn";
    }

    public static class Body {
        public static String VoteOk = "Bạn đã bình chọn thành công cho SBD%s với số lượng: %s tin nhắn. LH: (08) 399 171 99.";
        public static String NotOpened = "Hệ thống bình chọn chưa mở. Vui lòng chỉ bình chọn khi MC của chương trình Bước Nhảy Hoàn Vũ 2015 thông báo trong đêm liveshow. Xin cảm ơn!";
        public static String Closed = "Hệ thống đã đóng, bình chọn của bạn không hợp lệ. Bạn có thể tiếp tục bình chọn cho cặp thí sinh yêu thích vào lúc <THỜI GIAN> đêm nay - <THỜI GIAN> của đêm liveshow sắp tới. Xin cảm ơn!";
        public static String NotEnoughCash = "Ví MoMo của bạn không đủ tiền để bình chọn. Vui lòng nạp tiền để bình chọn. Nạp tiền Ví MoMo tại các điểm giao dịch của MoMo, từ 23 ngân hàng, thẻ Visa/Master có liên kết hoặc “mượn từ bạn bè” có tài khoản Ví MoMo. LH: (08) 399 171 99.";
        public static String OverAmount = "Bạn đã bình chọn quá 37 tin nhắn hôm nay. Vui lòng quay lại hôm sau để bình chọn cho thí sinh mình yêu thích. LH: (08) 399 171 99.";
        public static String PayBack24 = "Mỗi Ứng dụng MoMo sẽ được miễn phí 3 tin nhắn đầu tiên và sẽ được hoàn tiền trong vòng 24 giờ kể từ khi bình chọn. Cảm ơn bạn đã sử dụng Ứng dụng MoMo để bình chọn cho thí sinh yêu thích.";
        public static String SystemRecieved = "Hệ thống đã ghi nhận bình chọn & tiền bạn thanh toán. LH: (08) 399 171 99.";
        public static String PayBackComepleted = "Chúc mừng bạn đã nhận %s đồng từ chương trình miễn phí %s tin nhắn bình chọn của Bước Nhảy Hoàn Vũ 2015. Cảm ơn bạn đã dùng Ví MoMo để bình chọn.";
    }


    public static class Remix {
        public static class Cap {
            public static String VoteOk = "Bình chọn thành công";
            public static String NotOpened = "Hệ thống chưa mở";
            public static String Closed = "Hệ thống đã đóng";
            public static String NotEnoughCash = "Không đủ tiền bình chọn";
            public static String OverAmount = "Quá số lượng bình chọn";
            public static String PayBack24 = "Hoàn tiền sau 24 giờ";
            public static String SystemRecieved = "Hệ thống đã ghi nhận";
            public static String PayBackComepleted = "Hoàn tiền bình chọn";
        }

        public static class Body {
            public static String VoteOk = "Bạn đã bình chọn thành công cho %s với số lượng: %s tin nhắn. LH: (08) 399 171 99.";
            public static String NotOpened = "Hệ thống bình chọn chưa mở. Vui lòng chỉ bình chọn khi MC của chương trình The Remix 2015 thông báo trong đêm liveshow. Xin cảm ơn!";
            public static String Closed = "Hệ thống đã đóng, bình chọn của bạn không hợp lệ. Bạn có thể tiếp tục bình chọn cho cặp thí sinh yêu thích vào lúc <THỜI GIAN> đêm nay - <THỜI GIAN> của đêm liveshow sắp tới. Xin cảm ơn!";
            public static String NotEnoughCash = "Ví MoMo của bạn không đủ tiền để bình chọn. Vui lòng nạp tiền để bình chọn. Nạp tiền Ví MoMo tại các điểm giao dịch của MoMo, từ 23 ngân hàng, thẻ Visa/Master có liên kết hoặc “mượn từ bạn bè” có tài khoản Ví MoMo. LH: (08) 399 171 99.";
            public static String OverAmount = "Bạn đã bình chọn quá 35 tin nhắn hôm nay. Vui lòng quay lại hôm sau để bình chọn cho thí sinh mình yêu thích. LH: (08) 399 171 99.";
            public static String PayBack24 = "Mỗi Ví MoMo sẽ được miễn phí 3 tin nhắn đầu tiên và sẽ được hoàn tiền trong vòng 24 giờ kể từ khi bình chọn. Cảm ơn bạn đã sử dụng Ví MoMo để bình chọn cho thí sinh yêu thích.";
            public static String SystemRecieved = "Hệ thống đã ghi nhận bình chọn & tiền bạn thanh toán. LH: (08) 399 171 99.";
            public static String PayBackComepleted = "Chúc mừng bạn đã nhận %s đồng từ chương trình miễn phí %s tin nhắn bình chọn của Hòa Âm Ánh Sáng . Cảm ơn bạn đã dùng Ví MoMo để bình chọn.";
        }

    }

    public static class VnIdol {
        public static class Cap {
            public static String VoteOk = "Bình chọn thành công";
            public static String NotOpened = "Hệ thống chưa mở";
            public static String Closed = "Hệ thống đã đóng";
            public static String NotEnoughCash = "Không đủ tiền bình chọn";
            public static String OverAmount = "Quá số lượng bình chọn";
            public static String PayBack24 = "Hoàn tiền sau 24 giờ";
            public static String SystemRecieved = "Hệ thống đã ghi nhận";
            public static String PayBackComepleted = "Hoàn tiền bình chọn";
        }

        public static class Body {
            public static String VoteOk = "Bạn đã bình chọn thành công cho %s với số lượng %s tin nhắn. Bạn có cơ hội nhận được giải thưởng 20 triệu đồng từ CLEAR và CLOSE UP. Chúc bạn may mắn!";
            public static String NotOpened = "Thời gian bình chọn VietNam Idol tuần này chưa bắt đầu. Chi tiết tại www.vietnamidol.vtv.vn";
            public static String Closed = "Thời gian bình chọn VietNam Idol tuần này đã kết thúc. Chi tiết tại www.vietnamidol.vtv.vn";
            public static String NotEnoughCash = "Ứng dụng MoMo của bạn không đủ tiền để bình chọn. Vui lòng nạp tiền để bình chọn. Nạp tiền MoMo tại các điểm giao dịch của MoMo, từ 23 ngân hàng, thẻ Visa/Master có liên kết hoặc “mượn từ bạn bè” có tài khoản MoMo. LH: (08) 39917199.";
            public static String OverAmount = "Xin lỗi bạn, bạn đã bình chọn quá 3 tin nhắn dành cho một thí sinh tuần này. Chi tiết tại vietnamidol.vtv.vn";
            public static String PayBack24 = "Mỗi tài khoản MoMo sẽ được miễn phí 3 tin nhắn đầu tiên và sẽ được hoàn tiền trong vòng 24 giờ kể từ khi bình chọn. Cảm ơn bạn đã sử dụng Ứng dụng MoMo để bình chọn cho thí sinh yêu thích.";
            public static String SystemRecieved = "Hệ thống đã ghi nhận bình chọn & tiền bạn thanh toán. LH: (08) 399 171 99.";
            public static String PayBackComepleted = "Chúc mừng bạn đã nhận %s đồng từ chương trình miễn phí %s tin nhắn bình chọn của Vietnam Idol 2015 . Cảm ơn bạn đã dùng Ứng dụng MoMo để bình chọn.";
        }

    }

}
