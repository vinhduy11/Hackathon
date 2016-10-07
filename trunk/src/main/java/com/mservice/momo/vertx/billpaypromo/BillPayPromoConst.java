package com.mservice.momo.vertx.billpaypromo;

/**
 * Created by concu on 5/11/15.
 */
public class BillPayPromoConst {

    public static String NUMBER = "number";
    public static String TRAN_TYPE = "tranType";
    public static String TRAN_ID = "tranId";
    public static String SERVICE_ID = "serviceId";
    public static String SOURCE = "source";
    public static String PROTIMEVN = "proTimeVn";
    public static String ERROR = "error";
    public static String ERROR_DESC = "errorDesc";
    public static String GROUP = "group";
    public static String VCB_PROMO = "vcb";
    public static String OCB_PROMO = "ocb";

    public static String OCB_PROMO_PROGRAM = "ocbpromo";
    public static String MUON_GIO_DONG_PROMO = "mgd";

    public static String START_DATE_VCB = "startdatevcb";

    public static String END_DATE_VCB = "enddatevcb";

    public static String START_DATE_MGD = "startdatemgd";

    public static String END_DATE_MGD = "enddatemgd";

    public static String ACTIVE_TIME = "activetime";

    public static String CARD_CHECK_SUM = "cardCheckSum";

    public static String CMND = "cmnd";

    //Noi dung popup tra loi
//    public static String VTHN_VCB_ERROR_CONTENT = "Rất tiếc, Ví của bạn cần chứa thông tin thẻ quốc tế hoặc đang kết nối với ngân hàng (Vietinbank hoặc Vietcombank) trước khi nhận thẻ quà tặng này. Xin cảm ơn!";
//    public static String VTHN_VIETIN_ERROR_CONTENT = "Rất tiếc, Ví của bạn cần chứa thông tin thẻ quốc tế hoặc đang kết nối với ngân hàng (Vietinbank hoặc Vietcombank) trước khi nhận thẻ quà tặng này. Xin cảm ơn!!";
    public static String VTHN_NOT_MAPPING_CONTENT = "Rất tiếc, Ví của bạn cần chứa thông tin thẻ quốc tế hoặc đang kết nối với ngân hàng (Vietinbank hoặc Vietcombank) trước khi nhận thẻ quà tặng này. Xin cảm ơn!";
    public static String VTHN_CMND_ERROR_CONTENT = "Rất tiếc, bạn chưa đủ điều kiện nhận thẻ quà tặng cho dịch vụ này. Vui lòng chọn dịch vụ khác, xin cảm ơn!";
    public static String VTHN_CARDNUMBER_ERROR_CONTENT = "Rất tiếc, bạn chưa đủ điều kiện nhận thẻ quà tặng cho dịch vụ này. Vui lòng chọn dịch vụ khác, xin cảm ơn!";

    public static String POPUP_HEADER_OCB_PROMO = "Nhận khuyến mãi";
    public static String POPUP_CONTENT_OCB_PROMO = "Xin chúc mừng!\n" +
            "Bạn đã liên kết thành công với tài khoản ngân hàng Phương Đông (OCB). Bạn sẽ nhận ngay 100.000đ (tương đương 2 thẻ" +
            "quà tặng) khi thực hiện giao dịch bất kỳ trên Ứng dụng" +
            "MoMo với nguồn tiền OCB\n";
    public static String TRANHIS_CONTENT_OCB_PROMO = "Bạn đã liên kết ví MoMo với tài khoản Ngân hàng OCB. Bạn hãy hoàn tất việc nhận tiền 100.000đ bằng cách thực hiện 1 giao dịch bất kỳ (vd: nạp tiền điện thoại hoặc thanh toán dịch vụ) từ nguồn tiền OCB";

    public static String HEADER_NOTI_100_MOMO_OCB_PROMO = "Nhận tiền khuyến mãi";
    public static String CONTENT_NOTI_100_MOMO_OCB_PROMO = "Bạn vừa nhận đươc 100.000đ  tiền khuyến mại vào tài khoản Ví MoMo, có thể sử dụng cho tất cả các dịch vụ. Hãy tiếp tục thanh toán  (ví dụ nạp tiền điện thoại) bằng nguồn tiền OCB để nhận thêm quà tặng trị giá đến 100.000đ";
    public static String HTMLSTR_OCB_PROMO = "<script type='text/javascript'>function whatos() { var userAgent = navigator.userAgent || navigator.vendor || window.opera; if( userAgent.match( /iPad/i ) || userAgent.match( /iPhone/i ) || userAgent.match( /iPod/i ) ) return 'ios'; else if( userAgent.match( /Android/i ) ) return 'android'; else return ''; }; function openFormView(ftype,redirectId,serviceId,billid,amount){  if (whatos()=='android') Android.openControl(ftype,redirectId,serviceId,billid,amount); else if (whatos()=='ios') window.location='com.mservice.com.vn.momotransfer://?action=webinapp&screenid='+redirectId+'&type='+ftype+'&providerId='+serviceId+'&billid='+billid+'&amount='+amount+'&redirect=0';}</script><a style='font-family: Verdana, Geneva, Tahoma, Arial, Helvetica, sans-serif;  display: inline-block;  color: #FFFFFF;  background-color: #C0075B;  font-weight: bold;  font-size: 13px;  text-align: center;  padding: 2% 0%;  text-decoration: none;  margin-left: 0;  margin-top: 0px;  margin-bottom: 8px;  border: 1px solid #B0006E;  border-radius: 5px;  white-space: nowrap; text-align: center;width:100%' " +
            "href='javascript:void(form,0,serviceid,0,0)' onclick=\\\"openFormView('form',7,'','','')\\\" onTouchStart=\\\"openFormView('form',7,'','','')\\\">THANH TOÁN NGAY</a>";

    public static String HTML_OCB_PROMO = "<script type='text/javascript'>function whatos() { var userAgent = navigator.userAgent || navigator.vendor || window.opera; if( userAgent.match( /iPad/i ) || userAgent.match( /iPhone/i ) || userAgent.match( /iPod/i ) ) return 'ios'; else if( userAgent.match( /Android/i ) ) return 'android'; else return ''; }; function openFormView(ftype,redirectId,serviceId,billid,amount){  if (whatos()=='android') Android.openControl(ftype,redirectId,serviceId,billid,amount); else if (whatos()=='ios') window.location='com.mservice.com.vn.momotransfer://?action=webinapp&screenid='+redirectId+'&type='+ftype+'&providerId='+serviceId+'&billid='+billid+'&amount='+amount+'&redirect=0';}</script><a style='font-family: Verdana, Geneva, Tahoma, Arial, Helvetica, sans-serif;  display: inline-block;  color: #C0075B;  background-color: #FFFFFF;  font-weight: bold;  font-size: 13px;  text-align: center;  padding: 2% 0%;  text-decoration: none;  margin-left: 0;  margin-top: 0px;  margin-bottom: 8px;  border-radius: 5px;  white-space: nowrap; text-align: center;width:100%' " +
            " href='javascript:void(form,0,serviceid,0,0)' onclick=\"openFormView('form',7,'','','')\" onTouchStart=\"openFormView('form',7,'','','')\"><u>Nạp tiền điện thoại ngay</u></a>";

    public static String CONTENT_TRANHIS_100_MOMO_OCB_PROMO = "“Bạn đã nhận được 100.000đ vào ví MoMo dùng để sử dụng cho tất cả dịch vụ. Bạn sẽ nhận thêm 2 thẻ quà tặng dùng để giảm giá cho lần thanh toán kế tiếp (mỗi thẻ trị giá 50.000đ) khi thanh toán cho 2 dịch vụ bất kỳ nằm trong danh sách dịch vụ của chương trình “Liên kết ví MoMo với NH OCB”\n"
            + BillPayPromoConst.HTMLSTR_OCB_PROMO;
}
