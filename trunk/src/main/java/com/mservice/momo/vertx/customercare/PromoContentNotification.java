package com.mservice.momo.vertx.customercare;

import java.util.HashMap;

/**
 * Created by anhkhoa on 01/04/2015.
 */
public class PromoContentNotification {

//    public static String NOTIFY_GROUP_ONE = "Chúc mừng bạn nhận Thẻ quà tặng từ chương trình “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, " +
//            "nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi”. Sau đó bạn chọn xem và kích hoạt Thẻ quà tặng bạn vừa nhận! LH: (08) 399 171 99";
//
//
//    public static String NOTIFY_GROUP_TWO = "Tặng ngay voucher  50.000đ khi sử dụng 3 dịch vụ bất kỳ trên Ví MoMo từ 10/4-10/5. Ưu đãi dành cho KH nhận được tin nhắn. Chi tiết gọi 08.39917199";
//
//
//    public static String NOTIFY_GROUP_THREE = "Quý khách vừa được tặng voucher 50.000đ, Ví MoMo chân thành cảm ơn Quý khách đã sử dụng và mong tiếp tục được ủng hộ trong thời gian tới. ";
//
//
//    public static String NOTIFY_GROUP_FOUR = "Tặng ngay voucher 20.000đ khi nạp tiền điện thoại trên Ví MoMo từ 10/4-10/5. Ưu đãi dành cho KH nhận được tin nhắn. Chi tiết gọi 08.39917199";
//
//
//    public static String NOTIFY_GROUP_FIVE = "Liên kết tài khoản ngân hàng VCB với Ví MoMo để được tặng ngay voucher 100.000đ. Chi tiết gọi 08.39917199 hoặc tham khảo tại đây";
//

    public static final String BILL_PAY_PROMO_TITLE = "Nhận thẻ quà tặng";
    public static final String BILL_PAY_NOTI_CAPTION = "Trải nghiệm thanh toán - Tặng 200.000đ";
    public static String NOTIFY_CAPTION = "MoMo – Chuyển nhận tiền";
    public static String GIFT_MESSAGE_CUSTOMER_CARE = "Chúc mừng bạn vừa nhận được thẻ quà tặng từ chương trình ưu đãi dành cho khách hàng sử dụng Ví MoMo";
    public static String TRANS_COMMENT_CUSTOMER_CARE = "Chúc mừng bạn vừa nhận được thẻ quà tặng trị giá %sđ từ chương trình ưu đãi dành cho khách hàng sử dụng Ví MoMo";
    public static String NOTIFY_BODY_CUSTOMER_CARE = "Chúc mừng bạn vừa nhận được thẻ quà tặng trị giá %sđ từ chương trình ưu đãi dành cho khách hàng sử dụng Ví MoMo";
    public static String BILL_PAY_PROMO_DETAIL_NOTI_TWO = "Bạn đã nhận được thêm thẻ quà tặng giảm 50.000đ để thanh toán %s cho tháng tới.\n" +
            "Thời hạn sử dụng được bắt đầu từ ngày %s đến ngày %s.";

    public static String BILL_PAY_PROMO_TRAN_COMMENT_TWO = "Bạn đã nhận được thêm thẻ quà tặng giảm 50.000đ để thanh toán %s cho tháng tới.\n" +
            "Thời hạn sử dụng được bắt đầu từ ngày %s đến ngày %s.";

    public static String BILL_PAY_PROMO_GIFT_MESSAGE_TWO = "Bạn đã nhận được thêm thẻ quà tặng giảm 50.000đ để thanh toán %s cho tháng tới.\n" +
            "Thời hạn sử dụng được bắt đầu từ ngày %s đến ngày %s.";


    //    public static String BILL_PAY_PROMO_DETAIL_NOTI_FOUR = "Bạn đã nhận được thẻ quà tặng trị giá 50.000đ để thanh toán cho %s.";
//
//    public static String BILL_PAY_PROMO_TRAN_COMMENT_FOUR = "Bạn đã nhận được thẻ quà tặng trị giá 50.000đ để thanh toán cho %s.";
//
//    public static String BILL_PAY_PROMO_GIFT_MESSAGE_FOUR = "Bạn đã nhận được thẻ quà tặng 50.000đ để thanh toán %s.\n" +
//            "Thẻ hết hạn ngày: %s";
    public static String BILL_PAY_PROMO_DETAIL_NOTI_FOUR = "Bạn đã nhận được thêm thẻ quà tặng giảm 50.000đ để thanh toán %s cho tháng tới.\n" +
            "Thời hạn sử dụng được bắt đầu từ ngày %s đến ngày %s.";

    public static String BILL_PAY_PROMO_TRAN_COMMENT_FOUR = "Bạn đã nhận được thêm thẻ quà tặng giảm 50.000đ để thanh toán %s cho tháng tới.\n" +
            "Thời hạn sử dụng được bắt đầu từ ngày %s đến ngày %s.";

    public static String BILL_PAY_PROMO_GIFT_MESSAGE_FOUR = "Bạn đã nhận được thêm thẻ quà tặng giảm 50.000đ để thanh toán %s cho tháng tới.\n" +
            "Thời hạn sử dụng được bắt đầu từ ngày %s đến ngày %s.";


    public static String BILL_PAY_PROMO_DETAIL_NOTI_THREE = "Bạn đã nhận được thẻ quà tặng giảm 50.000đ khi thanh toán %s.  Hãy thanh toán ngay để tiếp tục nhận thêm 1 thẻ quà tặng trị giá 50,000đ\n" +
            "Hạn sử dụng đến: %s";

    public static String BILL_PAY_PROMO_TRAN_COMMENT_THREE = "Bạn đã nhận được thẻ quà tặng giảm 50.000đ khi thanh toán %s.  Hãy thanh toán ngay để tiếp tục nhận thêm 1 thẻ quà tặng trị giá 50,000đ (từ trang chủ chọn Ví của tôi --> Thanh toán --> Chọn dịch vụ cần thanh toán.\n" +
            "Hạn sử dụng đến: %s";

    public static String BILL_PAY_PROMO_GIFT_MESSAGE_THREE = "Bạn đã nhận được thẻ quà tặng giảm 50.000đ khi thanh toán %s.  Hãy thanh toán ngay để tiếp tục nhận ngay thêm 1 thẻ quà tặng trị giá 50,000đ\n" +
            "Hạn sử dụng đến: %s";


    public static String BILL_PAY_PROMO_BODY = "Bạn đã nhận được thẻ quà tặng trị giá 50.000đ để thanh toán cho %s. Thanh toán ngay để tiếp tục nhận 01 thẻ quà tặng trị giá 50.000đ.";

    public static String BILL_PAY_PROMO_DETAIL_NOTI_ONE = "Bạn đã nhận được thẻ quà tặng giảm 50.000đ khi thanh toán %s.  Hãy thanh toán ngay để tiếp tục nhận thêm 1 thẻ quà tặng trị giá 50,000đ.\n" +
            "Hạn sử dụng đến: %s";

    public static String BILL_PAY_PROMO_TRAN_COMMENT_ONE = "Bạn đã nhận được thẻ quà tặng giảm 50.000đ khi thanh toán %s.  Hãy thanh toán ngay để tiếp tục nhận thêm 1 thẻ quà tặng trị giá 50,000đ (từ trang chủ chọn Ví của tôi --> Thanh toán --> Chọn dịch vụ cần thanh toán.\n" +
            "Hạn sử dụng đến: %s";

    public static String BILL_PAY_PROMO_GIFT_MESSAGE_ONE = "Bạn đã nhận được thẻ quà tặng giảm 50.000đ khi thanh toán %s.  Hãy thanh toán ngay để tiếp tục nhận thêm 1 thẻ quà tặng trị giá 50,000đ\n" +
            "Hạn sử dụng đến: %s";


    public static String HTMLSTR = "<script type='text/javascript'>function whatos() { var userAgent = navigator.userAgent || navigator.vendor || window.opera; if( userAgent.match( /iPad/i ) || userAgent.match( /iPhone/i ) || userAgent.match( /iPod/i ) ) return 'ios'; else if( userAgent.match( /Android/i ) ) return 'android'; else return ''; }; function openFormView(ftype,redirectId,serviceId,billid,amount){  if (whatos()=='android') Android.openControl(ftype,redirectId,serviceId,billid,amount); else if (whatos()=='ios') window.location='com.mservice.com.vn.momotransfer://?action=webinapp&screenid='+redirectId+'&type='+ftype+'&providerId='+serviceId+'&billid='+billid+'&amount='+amount+'&redirect=0';}</script><a style='font-family: Verdana, Geneva, Tahoma, Arial, Helvetica, sans-serif;  display: inline-block;  color: #FFFFFF;  background-color: #C0075B;  font-weight: bold;  font-size: 13px;  text-align: center;  padding: 2% 0%;  text-decoration: none;  margin-left: 0;  margin-top: 0px;  margin-bottom: 8px;  border: 1px solid #B0006E;  border-radius: 5px;  white-space: nowrap; text-align: center;width:100%' href='javascript:void(form,0,promotion,0,0)' onclick=\"openFormView('form','','promotion','','')\">CHỌN THẺ NGAY</a>";

//    public static String HTMLSTR_GIFT = "<script type='text/javascript'>function whatos() { var userAgent = navigator.userAgent || navigator.vendor || window.opera; if( userAgent.match( /iPad/i ) || userAgent.match( /iPhone/i ) || userAgent.match( /iPod/i ) ) return 'ios'; else if( userAgent.match( /Android/i ) ) return 'android'; else return ''; }; function openFormView(ftype,redirectId,serviceId,billid,amount){  if (whatos()=='android') Android.openControl(ftype,redirectId,serviceId,billid,amount); else if (whatos()=='ios') window.location='com.mservice.com.vn.momotransfer://?action=webinapp&screenid='+redirectId+'&type='+ftype+'&providerId='+serviceId+'&billid='+billid+'&amount='+amount+'&redirect=0';}</script><a style='font-family: Verdana, Geneva, Tahoma, Arial, Helvetica, sans-serif;  display: inline-block;  color: #FFFFFF;  background-color: #C0075B;  font-weight: bold;  font-size: 13px;  text-align: center;  padding: 2% 0%;  text-decoration: none;  margin-left: 0;  margin-top: 0px;  margin-bottom: 8px;  border: 1px solid #B0006E;  border-radius: 5px;  white-space: nowrap; text-align: center;width:100%' " +
//            "href='javascript:void(form,0,serviceid,0,0)' onclick=\"openFormView('form','','serviceid','','')\" onTouchStart=\"openFormView('form','','serviceid','','')\">THANH TOÁN NGAY</a>";

    public static String HTMLSTR_GIFT = "<script type='text/javascript'>function whatos() { var userAgent = navigator.userAgent || navigator.vendor || window.opera; if( userAgent.match( /iPad/i ) || userAgent.match( /iPhone/i ) || userAgent.match( /iPod/i ) ) return 'ios'; else if( userAgent.match( /Android/i ) ) return 'android'; else return ''; }; function openFormView(ftype,redirectId,serviceId,billid,amount){  if (whatos()=='android') Android.openControl(ftype,redirectId,serviceId,billid,amount); else if (whatos()=='ios') window.location='com.mservice.com.vn.momotransfer://?action=webinapp&screenid='+redirectId+'&type='+ftype+'&providerId='+serviceId+'&billid='+billid+'&amount='+amount+'&redirect=0';}</script><a style='font-family: Verdana, Geneva, Tahoma, Arial, Helvetica, sans-serif;  display: inline-block;  color: #FFFFFF;  background-color: #C0075B;  font-weight: bold;  font-size: 13px;  text-align: center;  padding: 2% 0%;  text-decoration: none;  margin-left: 0;  margin-top: 0px;  margin-bottom: 8px;  border: 1px solid #B0006E;  border-radius: 5px;  white-space: nowrap; text-align: center;width:100%' " +
            "href='javascript:void(form,0,serviceid,0,0)' onclick=\"openFormView('form','','serviceid','','')\" onTouchStart=\"openFormView('form','','serviceid','','')\">THANH TOÁN NGAY</a>";

    public static String HTMLSTR_SCREEN_GIFT = "<script type='text/javascript'>function whatos() { var userAgent = navigator.userAgent || navigator.vendor || window.opera; if( userAgent.match( /iPad/i ) || userAgent.match( /iPhone/i ) || userAgent.match( /iPod/i ) ) return 'ios'; else if( userAgent.match( /Android/i ) ) return 'android'; else return ''; }; function openFormView(ftype,redirectId,serviceId,billid,amount){  if (whatos()=='android') Android.openControl(ftype,redirectId,serviceId,billid,amount); else if (whatos()=='ios') window.location='com.mservice.com.vn.momotransfer://?action=webinapp&screenid='+redirectId+'&type='+ftype+'&providerId='+serviceId+'&billid='+billid+'&amount='+amount+'&redirect=0';}</script><a style='font-family: Verdana, Geneva, Tahoma, Arial, Helvetica, sans-serif;  display: inline-block;  color: #FFFFFF;  background-color: #C0075B;  font-weight: bold;  font-size: 13px;  text-align: center;  padding: 2% 0%;  text-decoration: none;  margin-left: 0;  margin-top: 0px;  margin-bottom: 8px;  border: 1px solid #B0006E;  border-radius: 5px;  white-space: nowrap; text-align: center;width:100%' " +
            "href='javascript:void(form,7,0,0,0)' onclick=\"openFormView('form',7,'','','')\" onTouchStart=\"openFormView('form',7,'','','')\">THANH TOÁN NGAY</a>";


    public static String BILL_PAY_PROMO_DETAIL_NOTI_TIEP_CAN = "Bạn có cơ hội nhận thẻ quà tặng đến 200.000đ để thanh toán các loại hoá đơn/dịch vụ qua ứng dụng Ví MoMo từ 15/5-12/8/2015.";

    public static String BILL_PAY_PROMO_TRAN_COMMENT_TIEP_CAN = "Thực hiện 2 bước dưới đây để nhận thẻ quà tăng:\n" +
            "- Bước 1: Chọn hoá đơn/dịch vụ để nhận thẻ quà tặng\n" +
            "- Bước 2: Thực hiện thanh toán để tiếp tục nhận thêm thẻ quà tặng";

    public static String BILL_PAY_PROMO_GIFT_MESSAGE_TIEP_CAN = "Bạn có cơ hội nhận thẻ quà tặng đến 200.000đ để thanh toán các loại hoá đơn/dịch vụ qua ứng dụng Ví MoMo từ 15/5-12/8/2015.";


    public static String BILL_PAY_PROMO_ACTIVATED_GIFT_NOTI_BODY = "Thẻ quà tặng đã sẵn sàng để sử dụng. Bạn sẽ được giảm ngay 50.000đ khi thanh toán %s. Thời hạn sử dụng thẻ quà tặng đến hết %s";

    public static String BILL_PAY_PROMO_ACTIVATED_GIFT_NOTI_TITLE = "Thông báo sử dụng thẻ quà tặng";

    public static String BILL_PAY_PROMO_CAN_NOT_ACTIVE_GIFT = "Bạn không cần kích hoạt thẻ quà tặng do thẻ quà tặng sẽ được hệ thống tự động kích hoạt. Bạn có thể bắt đầu sử dụng thẻ quà tặng từ ngày %s. Chi tiết liên hệ: (08) 399 171 99";

    public static String OCB_PROMO_100_VOUCHER_GIFT = "Chúc mừng bạn đã nhận được thẻ quà tặng 100.000đ  sử dụng thanh toán cho 1 hóa đơn/dịch vụ bất kỳ trên Ứng dụng MoMo. Hãy tiếp tục thanh toán  (vd mua vé xem phim) bằng nguồn tiền OCB để nhận thêm quà tặng trị giá đến 100.000đ. \n";

    public static String OCB_PROMO_50_VOUCHER_GIFT_1 = "Chúc mừng bạn đã nhận được thẻ quà tặng trị giá 50.000đ dùng để thanh toán cho các dịch vụ trong chương trình khuyến mãi. Hãy thanh toán ngay để nhận thêm thẻ quà tặng trị giá 50.000đ.\n" +
            "Thời hạn sử dụng thẻ quà tặng: đến hết ngày %s.";

    public static String OCB_PROMO_50_VOUCHER_GIFT_2 = "Chúc mừng bạn đã nhận được thẻ quà tặng trị giá 50.000đ dùng để thanh toán cho các dịch vụ nằm trong chương trình khuyến mãi.\n" +
            "Thời hạn sử dụng thẻ quà tặng: từ %s đến hết ngày %s.";

    //MGD_2
    public static String MGD_2_PROMO_LOG_IN_NOTI_HEADER = "Thanh toán hoá đơn - Nhận đến 100.000đ";
    public static String MGD_2_PROMO_LOG_IN_NOTI_CONTENT = "Tặng ngay thẻ quà tặng lên đến 100.000đ để trải nghiệm thanh toán hoá đơn trên Ứng dụng MoMo \n" +
            "Chi tiết tại: momo.vn/thanhtoanhoadon hoặc gọi 08 399 171 99.";









    //IRON MAN NOTIFICATION

    public static String IRON_MAN_GROUP_VOUCHER_ONE_NOTIFICATION_CAPTION = "Nạp điện thoại miễn phí - Nhận thêm 500.000đ";

    public static String IRON_MAN_GROUP_VOUCHER_ONE_NOTIFICATION_BODY = "Thẻ nạp của Quý khách sắp hết hạn. Nạp ngay để nhận ưu đãi lên đến 500.000đ!!!";




    public static String IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_CAPTION_ONE_DAY = "Ngày cuối sử dụng thẻ quà tặng";
    public static String IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_BODY_ONE_DAY = "Ngày cuối sử dụng ưu đãi %s. Dùng ngay!!!";

    public static String IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_CAPTION_THREE_DAY = "Hết hạn thẻ quà tặng";
    public static String IRON_MAN_GROUP_VOUCHER_TWO_THREE_NOTIFICATION_BODY_THREE_DAY = "Còn 3 ngày nữa ưu đãi %s sẽ hết hạn. Thanh toán ngay!!!";

    public static String IRON_MAN_GROUP_MAPPING_CAPTION = "Nhận thêm 300.000đ từ MoMo";
    public static String IRON_MAN_GROUP_MAPPING_BODY = "Liên kết ngay tài khoản Vietcombank, OCB, VP Bank hoặc Visa/Master/JCB để nhận thêm ưu đãi 300.000đ. Thực hiện ngay!!!";



    public static String IRON_MAN_GIFT_RECEIVING_POP_UP_TITLE = "Nhận thẻ quà tặng";

    public static String IRON_MAN_GIFT_RECEIVING_POP_UP_CONTENT = "Chúc mừng Quý khách đã nhận được thẻ nạp trị giá 10.000đ. Nạp ngay để nhận ưu đãi lên đến 500.000đ. Hiệu lực trong vòng 24 giờ.";


    public static String PRE_PROMO_TOPUP = "Chúc mừng quí khách đã nhận được quà tặng nạp tiền điện thoại có giá trị 10.000đ từ chương trình \"Khách hàng may mắn\" của MoMo. Hiệu lực trong vòng 24 giờ. Hãy giới thiệu MoMo cho bạn bè để nhận nhiều quà tặng có giá trị";

    public static String IRON_MAN_GIFT_RECEIVING_POP_UP_200_CONTENT = "Chúc mừng Quý khách đã nhận được ưu đãi trị giá 200.000đ. Hiệu lực sử dụng đến %s. Liên kết ngay tài khoản Vietcombank, OCB, VP Bank hoặc Visa/Master/JCB để nhận thêm 300.000đ.";

    public static String IRON_MAN_GIFT_SUCCESSFUL_MAPPING_TITLE = "Nhận thưởng 300.000đ";

    public static String IRON_MAN_GIFT_SUCCESSFUL_MAPPING_CONTENT = "Chúc mừng Quý khách đã nhận được ưu đãi trị giá 300.000đ. Hiệu lực trong vòng 10 ngày!";


    public static String IRON_MAN_PLUS_TITLE = "500.000 cơ hội nạp điện thoại miễn phí.";
    public static String IRON_MAN_PLUS_BODY = "Chúc mừng bạn đã nhận được 10.000đ của chương trình “48h trải nghiệm cùng MoMo”. Bạn có thể nạp ngay tiền điện thoại cho chính mình hoặc người thân vì 10,000đ này chỉ có hiệu lực sử dụng trong 48 giờ từ khi nhận.";

    public static String IRON_MAN_PLUS_24H_REMIND_TITLE = "Còn 24h để nạp tiền điện thoại miễn phí";
    public static String IRON_MAN_PLUS_24H_REMIND_BODY = "Bạn còn 24h với chương trình “48h trải nghiệm cùng MoMo”. Hãy trải nghiệm ngay tính năng nạp tiền điện thoại siêu nhanh cùng MoMo.";

    public static String IRON_MAN_PLUS_1H_REMIND_TITLE = "Còn 1 giờ để nạp tiền điện thoại miễn phí";
    public static String IRON_MAN_PLUS_1H_REMIND_BODY = "Chỉ còn 1h nữa là 10,000đ trong chương trình “48h trải nghiệm cùng MoMo” sẽ hết hiệu lực sử dụng. Hãy tận dụng ngay cơ hội này khám phá những tính năng tuyệt vời của MoMo.";

    public static String IRON_MAN_PLUS_ROLLBACK_TITLE = "Khuyến mãi hết hiệu lực";
    public static String IRON_MAN_PLUS_ROLLBACK_BODY = "Chúng tôi rất tiếc phải thông báo 10,000đ trong “chương trình 48h trải nghiệm cùng MoMo” đã hết hiệu lực sử dụng. Cảm ơn bạn đã luôn đồng hành cùng MoMo.";

    public static String MONEY_POPUP = "{\"htmlBody\":\"%s\",\"button_title_1\":\"%s\",\"button_title_2\":\"%s\",\"button_title_x\":\"%s\",\"type\":%s,\"resultCode\":0,\"form\":{\"type\":\"popup\",\"header\":[],\"body\":[{\"type\":\"vbox\",\"action\":4,\"submitData\":{},\"sendBackend\":false,\"contents\":[{\"type\":\"html\",\"action\":4,\"value\":\"%s\"},{\"type\":\"cash_source\",\"action\":2,\"submitData\":{},\"sendBackend\":false,\"contents\":[]}]}],\"footer\":[],\"submitData\":{\"amount\":%s,\"phonenumber_id\":\"%s\",\"billid\":\"%s\",\"serviceid\":\"%s\"},\"backData\":{}}}";


    public static String OCTOBER_PROMO_UNLUCKY = "Quý khách đã nhận được thẻ quà tặng 50.000đ áp dụng giảm giá cho các hoá đơn từ 100.000đ trở lên. Hiệu lực sử dụng 15 ngày.";

    public static String OCTOBER_PROMO_LUCKY = "Quý khách đã nhận được ưu đãi 5 triệu đồng(100 thẻ quà tặng trong 10 tháng , mỗi tháng 10 thẻ). Hiệu lực sử dụng ưu đãi hàng tháng là 15 ngày kể từ ngày nhận được.";

    public static String OCTOBER_PROMO_LUCKY_N = "Chúc mừng Quý khách đã nhận được ưu đãi 500.000đ lần thứ %s từ ứng dụng MoMo. Hiệu lực sử dụng trong vòng 15 ngày.";

    public static String OCTOBER_PROMO_10DAYS_UNLUCKY_CAPTION = "Còn 10 ngày sử dụng ưu đãi 50.000đ";
    public static String OCTOBER_PROMO_10DAYS_UNLUCKY_BODY = "Thẻ quà tặng của Quý khách còn 10 ngày để sử dụng. Dùng ngay để được giảm 50.000đ cho mỗi lần thanh toán trên ứng dụng MoMo.";

    public static String OCTOBER_PROMO_5DAYS_UNLUCKY_CAPTION = "Còn 5 ngày sử dụng ưu đãi 50.000đ";
    public static String OCTOBER_PROMO_5DAYS_UNLUCKY_BODY = "Thẻ quà tặng của Quý khách còn 5 ngày để sử dụng. Dùng ngay để được giảm 50.000đ khi thanh toán trên ứng dụng MoMo.";

    public static String OCTOBER_PROMO_1DAYS_UNLUCKY_CAPTION = "Ngày cuối sử dụng ưu đãi 50.000đ";
    public static String OCTOBER_PROMO_1DAYS_UNLUCKY_BODY = "Thẻ quà tặng của Quý khách sắp hết hạn, thanh toán ngay hôm nay để được giảm giá 50.000đ.";

    public static String OCTOBER_PROMO_0DAYS_UNLUCKY_CAPTION = "Ưu đãi 50.000đ hết hiệu lực";
    public static String OCTOBER_PROMO_0DAYS_UNLUCKY_BODY = "Thẻ quà tặng 50.000đ của Quý khách đã hết hiệu lực sử dụng. Cảm ơn Quý khách đã đồng hành cùng MoMo.";


    public static String OCTOBER_PROMO_10DAYS_LUCKY_CAPTION = "Còn 10 ngày sử dụng ưu đãi 500.000đ";
    public static String OCTOBER_PROMO_10DAYS_LUCKY_BODY = "Quý khách còn %s thẻ quà tặng săp hết hạn sử dụng. Dùng ngay trong 10 ngày để được giảm 50.000đ cho mỗi lần thanh toán trên ứng dụng MoMo.";

    public static String OCTOBER_PROMO_5DAYS_LUCKY_CAPTION = "Còn 5 ngày sử dụng ưu đãi 500.000đ";
    public static String OCTOBER_PROMO_5DAYS_LUCKY_BODY = "Quý khách còn %s thẻ quà tặng sắp hết hạn sử dụng.  Dùng ngay trong 5 ngày để được giảm 50.000đ  khi thanh toán trên ứng dụng MoMo.";

    public static String OCTOBER_PROMO_1DAYS_LUCKY_CAPTION = "Ngày cuối sử dụng ưu đãi";
    public static String OCTOBER_PROMO_1DAYS_LUCKY_BODY = "Quý khách còn %s thẻ quà tặng sắp hết hạn sử dụng. Thanh toán ngay hôm nay để được giảm giá 50.000đ.";

    public static String OCTOBER_PROMO_0DAYS_LUCKY_CAPTION = "Ưu đãi 500.000đ hết hiệu lực";
    public static String OCTOBER_PROMO_0DAYS_LUCKY_BODY = "Các thẻ quà tặng trong ưu đãi 5 triệu của Quý khách đã hết hiệu lực sử dụng. Cảm ơn Quý khách đã đồng hành cùng MoMo.";

    public static String GIFT_EXPIRED_CAPTION = "Ngày cuối sử dụng thẻ quà tặng.";
    public static String GIFT_EXPIRED_BODY = "Thẻ quà tặng của Quý khách sắp hết hạn, chỉ còn 1 ngày sử dụng thẻ quà tặng để được giảm %sđ khi thực hiện thanh toán.";

    public static String GIFT_CLAIM_RECEIVE_TITLE = "Chương trình khuyến mãi";
    public static String GIFT_CLAIM_RECEIVE_BODY = "Bạn vừa nhận được %sđ từ việc giới thiệu bạn bè đăng ký tham gia MoMo thông qua chương trình Chia sẻ bạn bè";

    public static String GIFT_CLAIM_RECEIVED_TITLE = "Quà đã được nhận";
    public static String GIFT_CLAIM_RECEIVED_BODY = "";






    //Reactive

    public static String REACTIVE_DOLLAR_CUSTOMER_CARE_GIFT_NOTI_TITLE = "Tri Ân Khách Hàng";
    public static String REACTIVE_DOLLAR_CUSTOMER_CARE_GIFT_NOTI_BODY = "MoMo tặng bạn 50.000đ để trải nghiệm thanh toán dịch vụ tại Ví MoMo. Hạn sử dụng đến %s. Cảm ơn Bạn đã đồng hành cùng MoMo. Xem chi tiết quà tặng tại “Ví của tôi”";
    public static String REACTIVE_DOLLAR_CUSTOMER_CARE_GIFT_COMMENT_TRANHIS = "MoMo tặng bạn 50.000đ để trải nghiệm thanh toán dịch vụ tại Ví MoMo. Hạn sử dụng đến %s. Cảm ơn Bạn đã đồng hành cùng MoMo. Xem chi tiết quà tặng tại “Ví của tôi”";



    public static String REACTIVE_HEART_CUSTOMER_CARE_MONEY_NOTI_TITLE = "Nhận tiền khuyến mãi";
    public static String REACTIVE_HEART_CUSTOMER_CARE_MONEY_NOTI_BODY = "MoMo tặng bạn 10.000đ sử dụng được ngay cho các dịch vụ nạp tiền điện thoại, mua mã thẻ, mua sắm và thanh toán. Hạn sử dụng đến %s.";
    public static String REACTIVE_HEART_CUSTOMER_CARE_MONEY_COMMENT_TRANHIS = "MoMo tặng bạn 10.000đ sử dụng được ngay cho các dịch vụ nạp tiền điện thoại, mua mã thẻ, mua sắm và thanh toán. Hạn sử dụng đến %s.";



    public static String REACTIVE_HEART_CUSTOMER_CARE_GIFT_NOTI_TITLE = "Khuyến mãi đặc biệt";
    public static String REACTIVE_HEART_CUSTOMER_CARE_GIFT_NOTI_BODY = "MoMo tặng bạn 10.000đ và 2 thẻ quà tặng trị giá 100.000đ (áp dụng cho hóa đơn từ 60.000đ). Hoàn thêm 50% ";
    public static String REACTIVE_HEART_CUSTOMER_CARE_GIFT_NOTI_BODY_2 = "giá trị thanh toán (lên đến 200.000đ) với VCB/ OCB/ VPBank/ Visa/ MasterCard.\n" +
            "Vui lòng sử dụng trước %s. Xem chi tiết tại “Ví của tôi”.";
    public static String REACTIVE_HEART_CUSTOMER_CARE_GIFT_COMMENT_TRANHIS = "MoMo tặng bạn 10.000đ và 2 thẻ quà tặng trị giá 100.000đ (áp dụng cho hóa đơn từ 60.000đ). Hoàn thêm 50% ";
    public static String REACTIVE_HEART_CUSTOMER_CARE_GIFT_COMMENT_TRANHIS_2 = "giá trị thanh toán (lên đến 200.000đ) với VCB/ OCB/ VPBank/ Visa/ MasterCard.\n" +
            "Vui lòng sử dụng trước %s. Xem chi tiết tại “Ví của tôi”.";

    public static String PRU_CASTROL_GIFT_NOTI_TITLE = "30 ngày trải nghiệm MoMo";
    public static String PRU_CASTROL_GIFT_NOTI_BODY = "MoMo tặng bạn 150.000đ tương đương 3 thẻ quà tặng áp dụng thanh toán cho nhiều hóa đơn, dịch vụ. Hạn sử dụng đến %s.";
    public static String PRU_CASTROL_GIFT_NOTI_BODY_2 = "MoMo tặng bạn thẻ quà tặng trị giá 50.000đ áp dụng thanh toán cho nhiều hóa đơn, dịch vụ. Hạn sử dụng đến %s.";
    public static String PRU_CASTROL_GIFT_COMMENT_TRANHIS = "MoMo tặng bạn 150.000đ tương đương 3 thẻ quà tặng áp dụng thanh toán cho nhiều hóa đơn, dịch vụ. Hạn sử dụng đến %s.";
    public static String PRU_CASTROL_GIFT_COMMENT_TRANHIS_2 = "MoMo tặng bạn thẻ quà tặng trị giá 50.000đ áp dụng thanh toán cho nhiều hóa đơn, dịch vụ. Hạn sử dụng đến %s.";


    public static String PRE_ZALO_GIFT_NOTI_TITLE = "PHẦN QUÀ TỪ MOMO";
    public static String PRE_ZALO_GIFT_NOTI_BODY = "MoMo xin tặng bạn 10.000đ (hạn dùng trong 7 ngày). Cám ơn  bạn đã tham gia minigame cùng MoMo.";
    public static String PRE_ZALO_GIFT_TRANHIS = "MoMo xin tặng bạn 10.000đ (hạn dùng trong 7 ngày). Cám ơn  bạn đã tham gia minigame cùng MoMo.";

    public static String ZALO_GIFT_NOTI_TITLE = "Lì xì Lộc Táo Quân";
    public static String ZALO_GIFT_NOTI_BODY = "MoMo tặng bạn 10.000đ (hạn dùng trong 7 ngày) và 2 thẻ quà tặng trị giá 150.000đ để Xem phim và Thanh toán hóa đơn tiền Điện HCM. Trải nghiệm ngay để Vui Xuân Bất Tận !";
    public static String ZALO_GIFT_GIFT_TRANHIS = "MoMo tặng bạn 10.000đ (hạn dùng trong 7 ngày) và 2 thẻ quà tặng trị giá 150.000đ để Xem phim và Thanh toán hóa đơn tiền Điện HCM. Trải nghiệm ngay để Vui Xuân Bất Tận !";

    //Thu hoi tien 10k
    public static String ZALO_GET_BACK_PRE_MONEY_NOTI_TITLE = "Thu hồi 10.000đ Minigame";
    public static String ZALO_GET_BACK_PRE_MONEY_NOTI_BODY = "MoMo vừa thu hồi 10.000đ theo thể lệ từ chương trình Minigame cùng MoMo. Cám ơn bạn đã tham gia chương trình.";
    public static String ZALO_GET_BACK_PRE_MONEY_TRANHIS = "MoMo vừa thu hồi 10.000đ theo thể lệ từ chương trình Minigame cùng MoMo. Cám ơn bạn đã tham gia chương trình.";

    public static String ZALO_GET_BACK_MONEY_NOTI_TITLE = "Thu hồi Ưu đãi 10.000đ";
    public static String ZALO_GET_BACK_MONEY_NOTI_BODY = "MoMo vừa thu hồi 10.000đ theo thể lệ từ chương trình Lộc Táo Quân. Cám ơn bạn đã tham gia chương trình.";
    public static String ZALO_GET_BACK_MONEY_TRANHIS = "MoMo vừa thu hồi 10.000đ theo thể lệ từ chương trình Lộc Táo Quân. Cám ơn bạn đã tham gia chương trình.";

    public static String ZALO_BONUS_7_PERCENT_TRANHIS = "Bạn nhận thêm %sđ vào MoMo từ chương trình hoàn 10%% khi nạp tiền ĐT và mua mã thẻ.";
    public static String ZALO_BONUS_7_PERCENT_NOTI_TITLE = "HOÀN TIỀN 10%";
    public static String ZALO_BONUS_7_PERCENT_NOTI_BODY = "Bạn nhận thêm %sđ vào MoMo từ chương trình hoàn 10%% khi nạp tiền ĐT và mua mã thẻ.";

    public static String NOTI_GROUP_T_TITLE = "Nạp tiền dùng Lộc táo quân";
    public static String NOTI_GROUP_T_BODY = "Nạp tiền miễn phí từ Ngân Hàng, thanh toán Vé xem phim, Điện HCM sẽ được giảm đến 150.000đ. Xem hướng dẫn chi tiết!";

    public static String NOTI_GROUP_T_1_A_TITLE = "Tin vui cho các tín đồ Phim!";
    public static String NOTI_GROUP_T_1_A_BODY = "Dịp tết này bạn có thể Nạp tiền dễ dàng ở điểm Điểm Giao Dịch MoMo tại cụm rạp phim Galaxy. Xem chi tiết!";

    public static String NOTI_GROUP_T_1_B_TITLE = "Nhận ngay 10% giá trị giao dịch vào tài khoản";
    public static String NOTI_GROUP_T_1_B_BODY = "Khuyến mãi dành riêng cho bạn: Nhận ngay 10% giá trị giao dịch (3% vào TK Khuyến mãi và 7% vào TK MoMo) khi Nạp tiền điện thoại & mua mã thẻ trong vòng 3 tháng.";

    public static String NOTI_GROUP_T_2_TITLE = "Nạp tiền dùng Lộc táo quân";
    public static String NOTI_GROUP_T_2_BODY = "Nạp tiền miễn phí tại Điểm Giao Dịch MoMo để sử dụng Lộc Táo Quân trước ngày hết hạn %s nhé! Click để tìm Điểm Nạp/Rút tiền gần nhất!";

    public static String NOTI_GROUP_T_3_TITLE = "Nhận ngay 10% giá trị giao dịch vào tài khoản";
    public static String NOTI_GROUP_T_3_BODY = "Khuyến mãi dành riêng cho bạn: Nhận ngay 10% giá trị giao dịch (3% vào TK Khuyến mãi và 7% vào TK MoMo) khi Nạp tiền điện thoại & mua mã thẻ trong vòng 3 tháng.";

    public static String NOTI_GROUP_T_4_TITLE = "Nạp tiền dùng Lộc Táo Quân";
    public static String NOTI_GROUP_T_4_BODY = "Nạp tiền miễn phí tại Điểm Giao Dịch MoMo để sử dụng Lộc Táo Quân trước ngày hết hạn dd/mm/yyy nhé! Click để tìm Điểm nạp/rút tiền gần nhất!";

    public static String NOTI_GROUP_T_5_TITLE = "Tin vui cho các tín đồ Phim!";
    public static String NOTI_GROUP_T_5_BODY = "Dịp tết này bạn có thể Nạp tiền dễ dàng ở điểm Điểm Giao Dịch MoMo tại cụm rạp phim Galaxy. Xem chi tiết!";

    public static String NOTI_GROUP_T_6_TITLE = "Ngày cuối sử dụng Ưu đãi 10.000đ";
    public static String NOTI_GROUP_T_6_BODY = "Còn 1 ngày để sử dụng 10.000đ từ chương trình Lộc Táo Quân. Sử dụng ngay!!!";

//    public static String NOTI_GROUP_T_7_TITLE = "NOTI_GROUP_T_7";
//    public static String NOTI_GROUP_T_7_BODY = "NOTI_GROUP_T_7";

    public static String NOTI_GROUP_T_14_A_TITLE = "Cuối tuần này bạn xem phim gì?";
    public static String NOTI_GROUP_T_14_A_BODY = "Mua vé xem phim trên MoMo để được giảm ngay 50.000đ từ chương trình Lộc Táo Quân.";

    public static String NOTI_GROUP_T_14_B_TITLE = "Nhận ngay 10% giá trị giao dịch vào tài khoản";
    public static String NOTI_GROUP_T_14_B_BODY = "Khuyến mãi dành riêng cho bạn: Nhận ngay 10% giá trị giao dịch (3% vào TK Khuyến mãi và 7% vào TK MoMo) khi Nạp tiền điện thoại & mua mã thẻ trong vòng 3 tháng.";

    public static String NOTI_GROUP_T_21_TITLE = "Nạp tiền dùng Lộc táo quân";
    public static String NOTI_GROUP_T_21_BODY = "Nạp tiền miễn phí tại Điểm Giao Dịch MoMo để sử dụng Lộc Táo Quân trước ngày hết hạn %s nhé! Click để tìm Điểm Nạp/Rút tiền gần nhất!";

    public static String NOTI_GROUP_T_28_TITLE = "Bạn đã thanh toán tiền điện tháng này chưa?";
    public static String NOTI_GROUP_T_28_BODY = "Thanh toán hóa đơn Điện Hồ Chí Minh trên ứng dụng MoMo để được giảm ngay 100.000đ từ chương trình Lộc Táo Quân.";

    public static String NOTI_GROUP_T_35_TITLE = "Nạp tiền dùng Lộc táo quân";
    public static String NOTI_GROUP_T_35_BODY = "Nạp tiền miễn phí từ Ngân Hàng để sử dụng Lộc Táo Quân trước ngày hết hạn %s. Xem hướng dẫn chi tiết!";

    public static String NOTI_GROUP_T_42_TITLE = "Hết hạn ưu đãi Lộc Táo Quân";
    public static String NOTI_GROUP_T_42_BODY = "Còn 3 ngày nữa ưu đãi giảm %sđ cho %s sẽ hết hạn. Dùng ngay!!!";

    public static String NOTI_GROUP_T_45_TITLE = "Ngày cuối của ưu đãi Lộc Táo Quân";
    public static String NOTI_GROUP_T_45_BODY = "Ngày cuối sử dụng ưu đãi giảm %sđ cho %s. Dùng ngay!!!";

    public static String NOTI_GROUP_T_50_TITLE = "Nhận ngay 10% giá trị giao dịch vào tài khoản";
    public static String NOTI_GROUP_T_50_BODY = "Khuyến mãi dành riêng cho bạn: Nhận ngay 10% giá trị giao dịch (3% vào TK Khuyến mãi và 7% vào TK MoMo) khi Nạp tiền điện thoại & mua mã thẻ trong vòng 3 tháng.";

    public static String NOTI_GROUP_T_70_TITLE = "Nhận ngay 10% giá trị giao dịch vào tài khoản";
    public static String NOTI_GROUP_T_70_BODY = "Khuyến mãi dành riêng cho bạn: Nhận ngay 10% giá trị giao dịch (3% vào TK Khuyến mãi và 7% vào TK MoMo) khi Nạp tiền điện thoại & mua mã thẻ trong vòng 3 tháng.";

    public static String NOTI_GROUP_T_80_TITLE = "Nhận ngay 10% giá trị giao dịch vào tài khoản";
    public static String NOTI_GROUP_T_80_BODY = "Khuyến mãi dành riêng cho bạn: Nhận ngay 10% giá trị giao dịch (3% vào TK Khuyến mãi và 7% vào TK MoMo) khi Nạp tiền điện thoại & mua mã thẻ trong vòng 3 tháng.";

    public static String NOTI_GROUP_T_90_TITLE = "Ưu đãi hoàn 10% kết thúc";
    public static String NOTI_GROUP_T_90_BODY = "Chương trình ưu đãi hoàn 10% giá trị giao dịch cho các dịch vụ Nạp tiền điện thoại và mua mã thẻ đã kết thúc. Cám ơn bạn đã tham gia chương trình.";


    //===================================================================================
    // BINH TAN PROMO
    public static String NOTI_BINH_TAN_DEVICE_REMIND = "Thiết bị này đã được tham gia chương trình khuyến mãi Bình Tân. Vui lòng kiểm tra lại hoặc gọi 1900 545 441 để được hỗ trợ.";

    public static String NOTI_BINH_TAN_DEVICE_REMIND_SDT = "Thiết bị này đã được tham gia chương trình khuyến mãi Bình Tân trên ví MoMo %s. Vui lòng kiểm tra lại hoặc gọi 1900 545 441 để được hỗ trợ.";

    public static String NOTI_BINH_TAN_OVER_TIME = "Khách hàng thực hiện giao dịch ngoài khung giờ khuyến mãi 7h00-21h00 sẽ không nhận được khuyến mãi từ chương trình.";

    public static String NOTI_BINH_TAN_OVER_NUMBER_OF_TRAN_PER_HOUR = "Chỉ có %s khách hàng được nhận khuyến mãi trong 1h. Vui lòng đợi sau 1h nữa để có thể nạp tiền tham gia khuyến mãi, hotline 1900545441.";
    public static String NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_HOUR = "Khách hàng đã thực hiện hết số lượng giao dịch tham gia khuyến mãi %s lần/giờ. Vui lòng đợi sau 1h nữa để có thể nạp tiền tham gia khuyến mãi";
    public static String NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_HOUR_TITLE = "Giao dịch thứ %s trong 1h";

    public static String NOTI_BINH_TAN_OVER_NUMBER_OF_TRAN_PER_DAY = "Chỉ có %s khách hàng được nhận khuyến mãi trong ngày, vui lòng liên hệ hotline 1900545441 để biết thêm chi tiết.";
    public static String NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_DAY = "Khách hàng đã thực hiện hết số lượng giao dịch tham gia khuyến mãi %s lần/ngày, Vui lòng nạp tiền và tham gia khuyến mãi ngày kế tiếp.";
    public static String NOTI_BINH_TAN_EQUAL_NUMBER_OF_TRAN_PER_DAY_TITLE = "Giao dịch thứ %s trong ngày";

    public static String NOTI_BINH_TAN_NOT_NEW_NUMBER= "Khách hàng không nằm trong đối tượng nhận được khuyến mãi từ chương trình. Vui lòng kiểm tra lại hoặc gọi 1900 545 441 để được hỗ trợ.";

    public static String NOTI_BINH_TAN_OVER_TIME_BETWEEN_TWO_TRANS = "Điểm giao dịch chỉ được thực hiện 1 giao dịch trong %s phút. Vui lòng kiểm tra lại hoặc gọi 1900 545 441 để được hỗ trợ.";

    public static String NOTI_BINH_TAN_TITLE = "Giao dịch không nhận khuyến mãi";

    public static String NOTI_BINH_TAN_HAS_RECEIVE_BONUS_TITLE = "Giao dịch không nhận khuyến mãi";

    public static String NOTI_BINH_TAN_OUT_OF_TIME_TITLE = "Lưu ý ngoài khung giờ khuyến mãi";

    public static String NOTI_BINH_TAN_ERROR_DEFAULT = "Giao dịch trả thưởng không thành công. Vui lòng liên hệ hotline 1900545441 để biết thêm chi tiết.";

    public static String NOTI_BINH_TAN_REGISTER = "Bạn hãy đến các điểm giao dịch MoMo tại Quận Bình Tân, TP.HCM để tham gia chương trình “Nạp 10.000đ tặng 50.000đ” cùng MoMo. Chạm để xem chi tiết. Liên hệ: 1900 545 441";
    public static String NOTI_BINH_TAN_PROMO_TITLE = "Khuyến mãi “Nạp 10.000đ tặng 50.000đ”!";
    public static String NOTI_BINH_TAN_PROMO_TITLE_30 = "Khuyến mãi “Nạp 10.000đ tặng 30.000đ”!";

    public static String NOTI_BINH_TAN_CASHIN_T_DAY_TITLE = "Nạp tiền điện thoại từ 10.000đ để nhận ngay 50.000đ";
    public static String NOTI_BINH_TAN_CASHIN_T_DAY = "Bạn sẽ được nhận ngay 50.000đ từ MoMo khi thực hiện giao dịch nạp tiền điện thoại chỉ với 10.000đ trước ngày %date%";

    public static String NOTI_BINH_TAN_UNTOPUP_T7_DAY_TITLE = "Chỉ còn 1 tuần để nhận 50.000đ!";
    public static String NOTI_BINH_TAN_UNTOPUP_T7_DAY = "Nhận ngay 50.000đ từ MoMo khi thực hiện giao dịch nạp tiền điện thoại chỉ với 10.000đ, lại hưởng thêm chiết khấu 3-5%, nạp mọi lúc mọi nơi, nạp cho cả làng! Thời hạn: %todate%.";

    public static String NOTI_BINH_TAN_UNCASHIN_T2135_DAY_TITLE = "Chỉ còn 1 tuần để nhận 30.000đ!";
    public static String NOTI_BINH_TAN_UNCASHIN_T2135_DAY = "Bạn hãy đến các điểm giao dịch MoMo tại Quận Bình Tân để tham gia chương trình “Nạp 10.000đ tặng 30.000đ” cùng MoMo. Thời hạn: %todate%. Chạm để xem chi tiết. Liên hệ: 1900 545 441";



    //==========================================================================================================================================================================================================================================================================================================================================================
    //Referral Promotion
    public static String NOTI_AUTO_REFFERAL_BODY = "Bạn có hài lòng với giao dịch vừa thực hiện? Tham gia chương trình Chia Sẻ MoMo để bạn bè của bạn cũng được trải nghiệm tính năng tuyệt vời của MoMo nhé! Đồng thời, bạn và bạn bè sẽ được nhận 100.000đ mỗi người! Nếu có thắc mắc vui lòng gửi về: hotro@momo.vn để được hỗ trợ trong vòng 24h.";
    public static String NOTI_AUTO_REFFERAL_TITLE = "Hướng dẫn Chia Sẻ MoMo – Nhận 100.000đ mỗi người";
    public static String CHIA_SE_MOMO_TITLE = "Chia sẻ MoMo";
    public static String THEM_BAN_THEM_QUA_TITLE = "Thêm bạn thêm quà";
    public static String NOTI_AUTO_REFFERAL_UA_BODY = "SĐT %contact% đã nhập mã của bạn. Hãy hướng dẫn bạn ấy liên kết ví với tài khoản ngân hàng để cả 2 cùng nhận được 100.000đ  nhé! Xem hướng dẫn tại đây.";
    public static String NOTI_AUTO_MAPPING_SUCCESS = "Chúc mừng bạn! Hãy thực hiện nạp tiền/thanh toán từ nguồn tài khoản để nhận thẻ quà tặng 100.000đ từ CT Chia Sẻ MoMo. Chạm để xem hướng dẫn. Liên hệ hotro@momo.vn để được hỗ trợ trong vòng 24h.";
    //public static String NOTI_AUTO_REFFERAL_UA_SUCCESS_BODY = "MoMo gửi tặng bạn 100.000đ khi giới thiệu thành công %contact%. Tiếp tục giới thiệu thêm bạn bè nhé! Ưu đãi dành cho bạn là không giới hạn, với mỗi lượt giới thiệu thành công bạn nhận thêm 100.000đ!";
    public static String NOTI_AUTO_REFFERAL_UA_1D_LINK_BODY = "Bạn sắp nhận được 100.000đ từ MoMo! Chỉ cần liên kết ví với tài khoản ngân hàng và nạp tiền/ thanh toán. Xem hướng dẫn tại đây. Liên hệ: hotro@momo.vn để được hỗ trợ trong vòng 24h\n";
    public static String NOTI_AUTO_REFFERAL_UA_1D_CASHIN_BODY = "Bạn sắp nhận được 100.000đ từ MoMo! Chỉ cần nạp tiền/thanh toán từ 10.000đ bằng tài khoản liên kết. Xem hướng dẫn tại đây. Liên hệ: hotro@momo.vn để được hỗ trợ trong vòng 24h";
    public static String NOTI_AUTO_REFFERAL_UA_3D_BODY = "Đã 3 ngày rồi nhưng SĐT %contact% vẫn chưa nhận thưởng từ CT Chia Sẻ MoMo. Có vẻ bạn ấy chưa biết cách liên kết ví với tài khoản ngân hàng. Gửi lại bạn ấy hướng dẫn tại đây nhé!";
    public static String NOTI_AUTO_REFFERAL_UA_5D_LINK_BODY = "Nhan 100.000d tu MoMo khi lien ket tai khoan ngan hang! Xem huong dan: https://momo.vn/chiasemomo/lienket. Lien he hotro@momo.vn hoac 1900545441";
    public static String NOTI_AUTO_REFFERAL_UA_5D_CASHIN_BODY = "Nhan 100.000d tu MoMo khi nap tien/thanh toan tu tai khoan lien ket! Xem huong dan: https://momo.vn/chiasemomo/naptien. Lien he hotro@momo.vn hoac 1900545441";

    //==========================================================================================================================================================================================================================================================================================================================================================
    // PromotionGrab
    public static class GrabPromoContent {
        public static String partner = "Khuyến mãi cho tài xế Grab";
        public static String Title = "Khuyến mãi cho tài xế Grab!";
        public static String GiftMessageContent = "MoMo tặng bạn món quà: Giảm ngay %amount%đ khi nạp tiền điện thoại hoặc mua mã thẻ điện thoại trên MoMo. Nạp được mọi lúc mọi nơi, luôn có chiết khấu 3-5%!";
        public static String AutoNotiContent = "Tặng ngay %amount%đ nạp tiền điện thoại khi nạp ID tài xế bằng ví MoMo! Thời hạn: %time%. Xem chi tiết tại: %link%";
    }


    public static HashMap<Integer, String> PromotionErrorMap = new HashMap<>();

    static {
        PromotionErrorMap.put(5000, "Tham số không hợp lệ.");
        PromotionErrorMap.put(5001, "Sai đường dẫn.");
        PromotionErrorMap.put(5002, "Thiết bị này là máy ảo nên không được tham gia chương trình.");
        PromotionErrorMap.put(5003, "Sai đường dẫn.");
        PromotionErrorMap.put(5004, "Thiết bị IOS của số điện thoại phone1 đã được trả thưởng cho số điện thoại phone2");
        PromotionErrorMap.put(5005, "Không tồn tại thông tin thiết bị.");
        PromotionErrorMap.put(5006, "Vui lòng download app ANDROID mới nhất.");
        PromotionErrorMap.put(5007, "Thiếu thông tin thiết bị ANDROID.");
        PromotionErrorMap.put(5008, "Thiết bị ANDROID của số điện thoại phone1 đã được trả thưởng cho số điện thoại phone2");
        PromotionErrorMap.put(5009, "Sai đường dẫn.");
        PromotionErrorMap.put(5010, "Sai đường dẫn.");
    }





}
