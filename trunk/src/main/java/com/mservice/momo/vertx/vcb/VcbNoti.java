package com.mservice.momo.vertx.vcb;

import com.mservice.momo.vertx.processor.Misc;

/**
 * Created by concu on 11/24/14.
 */
public class VcbNoti {

    //THÔNG BÁO MỜI THAM GIA - A nằm trong danh sách đã được chọn
    public static class Invite {
        public static class Body{
            public static String Android = "Bạn đã được mời tham gia chương trình \"Liên kết tài khoản VCB-Cùng nhận 100.000đ\". Hãy mời bạn bè cài đặt Ví MoMo và liên kết VCB để cả 2 nhận ngay 100.000đ. Xem thêm tại"+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc LH "+ Misc.getTelForClient("0839917199", "08.39917199");
            public static String Ios = "Bạn đã được mời tham gia chương trình \"Liên kết tài khoản VCB-Cùng nhận 100.000đ\". Hãy mời bạn bè cài đặt Ví MoMo và liên kết VCB để cả 2 nhận ngay 100.000đ. Xem thêm tại "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc LH "+Misc.getTelForClient("0839917199","08.39917199");
        }
        public static String Cap = "Mời tham gia Liên kết Vietcombank";
    }

    //CHÚC MỪNG THAM GIA CHƯƠNG TRÌNH - A ngẫu nhiên cài đặt và map ví VCB
    public static class InstallApp{
        public static class Body{
            public static String Android = "Ví điện tử MoMo đang có chương trình \"Liên kết tài khoản Vietcombank-Cùng nhận 100.000đ\". Hãy mời bạn bè cài đặt Ví MoMo và liên kết tài khoản Vietcombank. Cứ mỗi lần bạn giới thiệu thành công, người đó  và bạn, mỗi người nhận ngay 100.000đ. Xem thêm tại: "+Misc.getLinkForClient("http://momo.vn/lienketvcb","http://momo.vn/lienketvcb") +"  hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");
            public static String Ios ="Ví điện tử MoMo đang có chương trình \"Liên kết tài khoản Vietcombank-Cùng nhận 100.000đ\". Hãy mời bạn bè cài đặt Ví MoMo và liên kết tài khoản Vietcombank. Cứ mỗi lần bạn giới thiệu thành công, người đó  và bạn, mỗi người nhận ngay 100.000đ. Xem thêm tại: "+Misc.getLinkForClient("http://momo.vn/lienketvcb","http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");
        }
        public static String Cap = "Mời tham gia chương trình Khuyến mãi";
    }

    public static class InputA{
        public static class Body{
            public static String BNotMap = "Bạn chưa liên kết Ví MoMo với tài khoản Vietcombank. Để nhận được phần thưởng 100.000đ từ chương trình KM hãy thực hiện liên kết tài khoản ngay hôm nay. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199.");
            public static String BNotTranVcb = "Bạn chưa thực hiện giao dịch có nguồn tiền từ tài khoản Vietcombank. Để nhận được phần thưởng 100.000đ của chương trình KM, bạn hãy thực hiện một giao dịch thanh toán, hoặc một giao dịch nạp tiền vào ví MoMo lấy từ tài khoản Vietcombank. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");
            public static String AValid = "Bạn đã đăng ký người giới thiệu để bạn và người này cùng được nhận thưởng. Hãy liên kết Ví MoMo với tài khoản ngân hàng Vietcombank và thực hiện 1 giao dịch có nguồn tiền từ ngân hàng này, bạn sẽ nhận được 100.000đ của chương trình KM. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");

            //done
            public static String ANotMapWallet = "Số ĐT %s chưa liên kết Ví MoMo với tài khoản Vietcombank. Vui lòng đăng ký lại số điện thoại cùa người giới thiệu khác phù hợp. Hoặc hãy nhắc người %s sớm thực hiện liên kết với tài khoản Vietcombank và bạn đăng ký lại sau đó. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");
            //done
            public static String ANotHasWallet = "Số ĐT %s chưa đăng ký dùng Ví điện tử MoMo. Vui lòng đăng ký lại số điện thoại cùa một người giới thiệu khác có dùng Ví điện tử MoMo và đã liên kết tài khoản Vietcombank. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");
            public static String AIsPartnerOfB = "Bạn đã đăng ký số điện thoại của người giới thiệu này. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");

            public static String InvalidCode = "Mã số %s bạn gởi chưa hợp lệ. Để đăng ký tham dự lại, vui lòng liên hệ người giới thiệu, xem lại thông tin hướng dẫn chương trình khuyến mãi hoặc gọi: 0839917199";


        }
        public static class Cap{
            //B CHƯA map Ví MoMo
            public static String BNotMap = "Ví chưa liên kết Vietcombank";
            //B CHƯA thực hiện giao dịch từ nguồn tiền VCB
            public static String BNotTranVcb = "Ví chưa thực hiện giao dịch từ Vietcombank";
            //A hợp lệ (A đã map VCB)
            public static String AValid = "Đăng ký thành công";

            //A KHÔNG hợp lệ (chưa map VCB)
            public static String ANotMapWallet = "Người giới thiệu chưa liên kết Vietcombank";
            //A KHÔNG có Ví MoMo
            public static String ANotHasWallet = "Người giới thiệu chưa có Ví MoMo";
            //Nhập nhiều lần số của người A
            public static String AIsPartnerOfB = "Số điện thoại đã được đăng ký";
            public static String InvalidCode = "Mã số không hợp lệ";

        }
    }

    public static class UseVoucher{
        public static class Body{
            //Chúc mừng B sử dụng voucher thành công
            public static String BUseVcOk = "Cảm ơn bạn đã tham gia chương trình KM! Người bạn %s đã nhận phần thưởng 100.000đ. Hãy mời bạn bè cài đặt Ví MoMo và liên kết tài khoản Vietcombank. Cứ mỗi lần bạn giới thiệu thành công, người đó và bạn, mỗi người nhận ngay 100.000đ. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");
            //Chúc mừng A nhận được voucher
            public static String ARecieveVc = "Bạn đã giới thiệu thành công số điện thoại %s tham gia chương trình KM. Xin tặng bạn thẻ quà tặng trị giá 100.000đ. Hãy tiếp tục mời bạn bè cài đặt Ví MoMo và liên kết tài khoản Vietcombank. Cứ mỗi lần bạn giới thiệu thành công, người đó và bạn, mỗi người nhận ngay 100.000đ. Truy cập momo.vn/lienketvcb hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199");

            //Dịch vụ dưới 100.000đ
            public static String BUseLessThan100K = "Dịch vụ bạn sử dụng chưa đến 100.000đ, số tiền thừa của voucher sẽ không được hoàn lại.";

        }
        public static class Cap{
            //Chúc mừng B sử dụng voucher thành công
            public static String BUseVcOk = "Chúc mừng sử dụng thẻ thành công";
            //Chúc mừng A nhận được voucher
            public static String ARecieveVc = "Chúc mừng nhận được thẻ quà tặng";
            //Dịch vụ dưới 100.000đ
            public static String BUseLessThan100K = "Dịch vụ dưới 100.000đ";

        }
    }

    public static class BOkAndNotInputA{
        public static String Cap ="Chưa điền số người giới thiệu";
        public static String Body = "Hãy điền số điện thoại người đã giới thiệu cho bạn chương trình KM \"Liên kết tài khoản Vietcombank...\", bạn sẽ nhận ngay phần thưởng 100.000đ. Bạn cũng có thể là người đi giới thiệu bạn bè cài đặt Ví MoMo và liên kết tài khoản Vietcombank, mỗi lần bạn giới thiệu thành công, người đó và bạn, mỗi người nhận ngay 100.000đ. Truy cập "+ Misc.getLinkForClient("http://momo.vn/lienketvcb", "http://momo.vn/lienketvcb")+" hoặc Gọi "+Misc.getTelForClient("0839917199","0839917199.");
    }
}
