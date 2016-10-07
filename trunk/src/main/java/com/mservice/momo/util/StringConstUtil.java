package com.mservice.momo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anhkhoa on 20/03/2015.
 */
public class StringConstUtil {


    public static final int IO_DEFAULT_STATE = 0;

    public static final int IO_GET_MONEY_STATE = 1;

    public static final int IO_PAY_MONEY_STATE = -1;

    public static final String APP_CODE = "app_code";

    public static final String APP_VER = "app_ver";

    public static final String APP_OS = "app_os";
    //Category
    public static final int DEFAULT_CATEGORY = -1;

    public static final String JO_EXTRA = "jo_extra";

    public static final String DEVICE_IMEI = "device_imei";
    // Kind of Transfer
    //Top up
    public static final String TOP_UP = "topup";
    //123Phim
    public static final String ONE_TWO_THREE_FILM = "123phim";
    //Operation Smile
    public static final String OPERATION_SMILE = "operationsmile";
    //Epay
    public static final String EPAY = "epay";
    //Invoice
    public static final String INVOICE = "invoice";
    //Invoice_mobi
    public static final String INVOICE_MOBI = "invoice_mobi";
    //Vina dien thoai tra sau
    public static final String VINAHCM = "vinahcm";
    //Mobi dien thoai tra sau
    public static final String MOBI = "mobi";
    //Service Topup
    public static final String SERVICE_TOPUP = "servicetopup";
    //Event
    public static final String EVENT = "event";
    //Service
    public static final String SERVICE = "service";
    public static final String SERVICE_ID = "serviceId";

    //String in config file
    public static final String SERVER = "server";
    public static final String MOMO_GROUP = "momo_group";
    public static final String MOMO_CAPSET_ID = "momo_capset_id";
    public static final String MOMO_UPPER_LIMIT = "momo_upper_limit";
    public static final String SCAN_SYNC = "scan_sync";
    public static final String CHECK_STORE_APP = "storeApp";

    public static final String TQT_GROUP_ID = "tqt_group_id";
    //Noti Text.
    public static final String PAYMENT_STORE_TITLE = "Đăng ký Điểm chấp nhận thanh toán thành công";

    public static final String PAYMENT_STORE_BODY = "Chúc mừng quý khách đăng ký Điểm chấp nhận thanh toán thành công. Quý khách có thể thực hiện nạp tiền điện thoại/thẻ game, mua mã thẻ, thanh toán hóa đơn/dịch vụ nhanh chóng cho KH. LH: 0839917199.";

    public static final String GIFT_ALERT_VALUE = "Lưu ý: Bạn đang sử dụng thẻ quà tặng %sđ để thực hiện giao dịch trị giá %sđ. Số tiền chênh lệch %sđ sẽ không được hoàn lại.";
    public static final String GIFT_ALERT_KEY = "alert";
    //colName Gift Table

    public static final String GIFT_ID = "_id";
    public static final String GIFT_STATUS = "status";
    public static final String GIFT_TYPEID = "typeId";
    public static final String GIFT_AMOUNT = "amount";
    public static final String GIFT_STARTDATE = "startDate";
    public static final String GIFT_ENDDATE = "endDate";
    public static final String GIFT_OWNER = "owner";


    //
    public static final String START = "start";
    public static final String END = "end";


    // Kiem tra thong tin diem giao dich
    public static final String STORE_NAME = "storename";
    public static final String STORE_NUMBER = "storeNumber";
    public static final String NUMBER = "number";
    public static final String DATA = "data";
    public static final String PATH = "path";
    public static final String PIN = "pin";
    public static final String DELETE = "delete";
    public static final String WAITING_REG = "waitingreg";
    public static final String IS_SETUP = "issetup";
    public static final String IS_AGENT = "isagent";
    public static final String STATUS = "status";


    // Ten dich vu
    public static final String PRUDENTIAL = "prudential";
    public static final String HOME_CREDIT = "homecredit";
    public static final String PHIM123 = "123phim";
    public static final String CUNGMUA = "cungmua";
    public static final String VTHN = "vthn";
    public static final String HAYHAYTV = "hayhaytv";
    public static final String FSHARE = "fshare";
    public static final String FPTPLAY = "fptplay";
    public static final String SCJ = "scj";
    public static final String OPERATIONSMILE = "operationsmile";

    // Config trong momo.jon cho buildpaypromo
    public static final String MIN_TRAN_VALUE = "min_tran_value";
    public static final String CACHE_BILL_INFO = "cacheBillInfo";
    public static final String DURATION = "duration";
    public static final String COUNT = "count";

    // Config trong momo.json cho bank
    public static final String BANK = "bank";
    public static final String BANK_CODE = "bank_code"; // BANK CODE
    public static final String BANK_CONNECTOR_VERTICLE = "bank_connector_verticle";
    public static final String IS_STORE_APP = "storeApp";
    public static final String IS_UAT = "uat";
    public static final String ALLOW_BANK_VIA_CONNECTOR = "allow_bank_via_connector";
    public static final String IOS_CODE = "ios_code";
    public static final String ANDROID_CODE = "android_code";
    public static final String STORE_IOS_CODE = "store_ios_code";
    public static final String STORE_ANDROID_CODE = "store_android_code";
    public static final String TIMER = "timer";
    public static final String ANDROID_OS = "ANDROID";
    public static final String IOS_OS = "IOS";
    public static final String WINDOW_OS = "WINDOWS";
    public static final String SEND_REMIND_NOTI = "send_remind_noti";
    public static final String SEND_REMIND_PROMO = "send_remind_promo";


    // Noi dung show log
    public static final String WALLET_MAPPING_RESULT = "Thong tin map vi";
    public static final String WALLET_MAPPING_CONFIRMED = "confirmed";
    public static final String WALLET_MAPPING_PARTNER = "partner";
    public static final String LIQUID_POPUP = "liquid";
    public static final String INFO_POPUP = "info";
    public static final String BANK_POPUP = "bank";
    public static final String WALLET_MAPPING_NO = "0"; //HUY UNMAP
    public static final String WALLET_MAPPING_YES = "1";

    // Title show popup map vi khi login
    public static final String TYPE = "type";
    public static final String BUTTON_TITLE_1 = "button_title_1";
    public static final String BUTTON_TITLE_2 = "button_title_2";
    public static final String BUTTON_TITLE_X = "button_title_x";
    public static final String CONFIRM_BUTTON_TITLE = "Xác Nhận";
    public static final String CANCEL_BUTTON_TITLE = "Hủy";
    public static final String CANCEL_HEADER = "Lỗi kết nối";
    public static final String CANCEL_CONTENT = "Bạn đã hủy thẻ không thành công";
    public static final String SCREEEN_ID = "screenId";

    //Ten ngan hang
    public static final String VCB_NAME = "Vietcombank";
    public static final String VCB_CODE = "12345";
    public static final String VIETIN_NAME = "Vietinbank";
    public static final String VIETIN_CODE = "102";
    public static final String SACOM_CODE = "109";
    public static final String SACOM_NAME = "Sacombank";
    public static final String EXIM_CODE = "107";
    public static final String EXIM_NAME = "EXIMBANK";
    public static final String TPB_CODE = "105";
    public static final String TPB_NAME = "TPBank";
    //Ten field share bang transaction
//    "cardlbl" : "Visa - 4915",
//            "cardtype" : "001",
//            "cardnumbervisa" : "Blrj5otpYzTYBGaGIzRT4Xk1b88LfDMJ6gRPP94fFeM=",
//            "tranType" : "7"
    public static final String TRANDB_CARD_LABEL = "cardlbl";
    public static final String TRANDB_CARD_TYPE = "cardtype";
    public static final String TRANDB_CARD_NUMBER_VISA = "cardnumbervisa";
    public static final String TRANDB_TRAN_TYPE = "tranType";
    public static final String TRANDB_TRAN_ID = "tranId";
    public static final String FINAL_BANKNET_ADJUST_ACCOUNT = "finalbanknetadjustaccount";
    //
    public static final String PHONES_BANKID_SBS = "sbs";
    //Error
    public static final String ERROR = "error";
    public static final String ResultCode = "resultCode";
    public static final String DESCRIPTION = "desc";
    public static final String AMOUNT = "amount";
    public static final String URL = "url";
    public static final String EXTRA = "extra";
    public static final String SEND_NOTI = "send_noti";
    public static final String PHONE = "phone";
    //Message name trong file tran-message.xml
    public static final String BONUS_DGD = "BONUS_DGD";
    //Noi dung popup
    public static final String SO_DIEN_THOAI = "Số điện thoại";
    public static final String MENH_GIA_THE = "Mệnh giá thẻ";
    public static final String SO_LUONG = "Số lượng";
    public static final String PHI_GIAO_DICH_FROM_CARD = "Phí giao dịch từ thẻ";
    public static final String PHI_GIAO_DICH = "Phí giao dịch";
    public static final String TONG_CHI_PHI = "Tổng chi phí";
    public static final String IS_NAMED_DGD = "Tài khoản KH là Điểm chấp nhận thanh toán không thực hiện định danh.";
    public static final String IS_NOT_DCNTT_FUNCTION = "Chức năng này không dành cho ĐCNTT. Xin cảm ơn.";
    public static final String IS_NAMED_USER = "Số điện thoại này đã định danh.";


    //Ten key value
    //public static final String IS_AGENT = "isAgent";


    // Noi dung thong bao
    //Thong bao day la diem giao dich chap nhan thanh toan
    public static final String IS_DGD_CNTT = "Tài khoản của bạn là Điểm chấp nhận thanh toán nên không thể thực hiện chức năng định danh";
    public static final String BONUS_300_CONTENT = "Bạn vừa nhận được 300đ vào tài khoản KM từ chương trình “Dùng nhiều – Tích lũy nhiều”. Bạn sẽ sử dụng được tiền trong tài khoản KM khi tích lũy được 50.000đ";
    public static final String BONUS_300_HEADER = "Nhận tiền khuyến mãi!";
    //Kiem tra service type
    // loai dich vu mua ma the
    public static final int CARD_EPAY_TYPE = 0;
    // loai thanh toan dich vu
    public static final int SERVICE_EPAY_TYPE = 1;
    public static final String M2M_HTML_FAIL = "<div align=\"center\"><img src=\"http://app.momo.vn:81/momo_app/logo/giaodichthatbai_ic.png\" style = \"width:100px; height:100px\"/><p><h3>Giao dịch không thành công</h3></p><p>Lỗi xảy ra. Chuyển tiền không thành công.</p><p>Bạn vui lòng thực hiện lại hoặc gọi  <a href=\"tel:0839917199\">0839917199</a> để được tư vấn.</div>";
    public static final String M2M_HTML_PASS = "<div align=\"center\"><img src=\"http://app.momo.vn:81/momo_app/logo/thanhcong_ic.png\" style = \"width:100px; height:100px\"/><p><h3>Giao dịch thành công</h3></p><p>Bạn vừa chuyển tiền thành công</p></div>";
    public static final String CLAIM_POINT_EXTRA = "<div align=\"center\"><img src=\"http://app.momo.vn:81/momo_app/logo/5plus.png\" style = \"width:100px; height:100px\"/><p><h3>Bạn có %sđ trong tài khoản khuyến mãi</h3></p><p>Bạn nhận được khuyến mãi từ %s (%s)</p></div>";
    public static final String CONNECTOR_NOTI_VERTICLE = "connector_noti_verticle";
    public static final String HTML = "html";
    // Core variables
    public static final String CORE_END_USER_VALUE = "0";
    //Ten Verticle
    public static final String CORE_STORE_VALUE = "1";
    public static final String KEY = "key";
    public static final String WAIT_REGISTER = "waitregister";
    //Constan Value for develop
    public static final String EMPTY = "";
    //Constan popup value for referral promotion
    public static final String VIEW_HELP_BUTTON_TITLE = "Hướng dẫn";
    public static final String CLOSE_BUTTON_TITLE = "Đóng";
    public static Map<String, String> bankLinkNames = new HashMap<String, String>();
    public static Map<String, Boolean> bankLinkVerify = new HashMap<String, Boolean>();
    public static String SHOPPING_WAITING_EXECUTE = "Đang chờ xử lí";
    public static String SHOPPING_FAILING_EXECUTE = "Giao dịch không thành công";
    public static String NOT_ENOUGH_MONEY = "Tài khoản không đủ tiền";
    public static String ACTIVE_SERVICE_FEE = "active_service_fee";
    public static String SCB_NO_NAMED_POP_UP = "Dịch vụ chỉ áp dụng với các ví MoMo đã được định danh. Quý khách vui lòng định danh trước khi thực hiện giao dịch. Có 2 cách thực hiện định danh:\n" +
            " - Đến điểm giao dịch MoMo gần nhất để yêu cầu được định danh. \n" +
            " - Liên kết với tài khoản ngân hàng.";
    //REACTIVE CUSTOMER
    public static String TITLE_NOTI = "title_noti";
    //OCB PROMO
    public static String NOTIFICATION_OBJ = "notification_obj";
    public static String CONTENT_NOTI = "content_noti";
    public static String TITLE_TRAN = "title_tran";
    public static String CONTENT_TRAN = "content_tran";
    public static String CONTENT_GIFT = "content_gift";
    public static String JSON_NUMBER_OF_REACTIVE_CUSTOMER_GIFT = "number_of_reactive_customer_gift";
    public static String AMOUNT_MOMO = "amount_momo";
    public static String AMOUNT_GIFT = "amount_gift";
    public static String AMOUNT_POINT = "amount_point";
    public static String RESULT = "result";
    public static String POPUP_EMAIL = "popup_email";
    public static String PROMOTION = "promotion";
    public static String GET_OTP = "GET_OTP";
    public static String SERVICE_OUTSIDE = "outside_service";

    static {
        bankLinkNames.put(VCB_CODE, VCB_NAME);
        bankLinkNames.put(VIETIN_CODE, VIETIN_NAME);
        bankLinkNames.put(SACOM_CODE, SACOM_NAME);
        bankLinkNames.put(EXIM_CODE, EXIM_NAME);
        bankLinkNames.put(TPB_CODE, TPB_NAME);

        bankLinkVerify.put(SACOM_CODE, true);
    }

    //Verticle json
    public static class Verticle_json {
        public static final String VERTICLES = "verticles";
        public static final String NAME = "name";
        public static final String BLOCK = "block";
        public static final String ADDRESS = "address";
        public static final String MULTITHREAD = "thread";
        public static final String IS_MODULE = "isModule";
        public static final String POSITION_OF_MODULE = "posModule";
        public static final String INSTANCES = "instances";
        public static final String IS_DEPLOYED = "isDeployed";
    }

    //Cungmua
    public static class Shopping {
        public static final String ServiceType = "shopping";
        public static final String Verticle = "verticle";
        public static final String BillId = "billId";
    }

    public static class OBCPromo {
        public static final String JSON_OBJECT = "ocb_promo";
        public static final String BANK_CODE = "bank_code";
        public static final String OCB = "ocb";
        public static final String AGENT_POINT = "agent_point";
        public static final String POINT = "point";
        public static final String IS_ACTIVE = "isActive";
        public static final String GIFT_TYPE_1 = "gift_type_1";
        public static final String GIFT_TYPE_2 = "gift_type_2";
    }

    public static class IronManPromo {
        public static final String IRON_PROMO = "iron_promo";
        public static final String JSON_OBJECT = "iron_promo";
        public static final String IRON_PROMO_1 = "iron_promo_1";
        public static final String IRON_PROMO_2 = "iron_promo_2";
        public static final String IRON_PROMO_3 = "iron_promo_3";
        public static final String IS_ACTIVE = "isActive";
        public static final String GIFT_ID_ARRAY = "gift_id_array";
        public static final String TIME_SCAN_EXPIRED_GIFT = "time_scan_expired_gift";
        public static final String TIME_SCAN_MAPPING = "time_scan_mapping";
        public static final String IRON_PROMO_4 = "iron_promo_4";
        public static final String IRON_PROMO_LATER = "iron_promo_later";

    }

    public static class SyncStoreApp{
        public static final String JSON_OBJECT = "sync_store_app";
        public static final String TIME_SYNC = "time_sync";
        public static final String TIME_IOS_DEFAULT = "time_ios_default";
        public static final String TIME_ANDROID_DEFAULT = "time_android_default";

    }

    public static class PreIronManPromo{
//        "start_time" : 1442422800000,
//                "isActive" : true,
//        number_of_given_voucher:2000,
//        number_of_random_voucher:2000
        public static final String JSON_OBJECT = "pre_promo";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String IS_ACTIVE = "isActive";
        public static final String NUMBER_OF_GIVEN_VOUCHER = "number_of_given_voucher";
        public static final String NUMBER_OF_RANDOM_VOUCHER = "number_of_random_voucher";
        public static final String GROUP_1 = "group_1";
        public static final String GROUP_2 = "group_2";
    }

    public static class IronManPromoPlus{
        public static final String JSON_OBJECT = "iron_man_plus";
        public static final String IS_ACTIVE = "isActive";
        public static final String AGENT = "agent";
        public static final String TIME_FOR_GIFT = "timeforgift";
        public static final String VALUE_OF_GIFT = "valueofgift";
        public static final String TIME_SCAN_PERIOD = "time_scan_period";
    }

    public static class MerchantKeyManage{
        public static final String JSON_OBJECT = "merchant_key_manage";
        public static final String IS_TEST_ENVIRONMENT = "test";
        public static final String MERCHANT_CODE = "merchantcode";
        public static final String MERCHANT_NAME = "merchantname";
        public static final String DESCRIPTION = "description";
        public static final String AMOUNT = "amount";
        public static final String VOUCHER = "voucher";
        public static final String MOMO = "momo";
        public static final String SOURCE_FROM = "sourcefrom";
        public static final String PIN = "pin";
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String STATUS = "status";
        public static final String MESSAGE = "message";
        public static final String USER_NAME = "username";
        public static final String HASH = "hash";
        public static final String PHONENUMBER = "phonenumber";
        public static final String DATA = "data";
        public static final String IP_ADDRESS = "ipaddress";
        public static final String BILL_ID = "billid";
        public static final String TRANSID = "transid";
        public static final String MERCHANT_ID = "merchantId";
        public static final String TID = "TID";
        public static final String CUSTOMER_NUMBER = "customerNumber";
        public static final String TOKEN = "token";
        public static final String STANDARD_AMOUNT = "standard_amount";
        public static final String TIMEOUT = "timeOut";

        //add more
        public static final String CUSTOMER_PHONE = "customer_phone";
        public static final String CUSTOMER_EMAIL = "customer_email";
        public static final String CUSTOMER_NAME = "customer_name";
        public static final String CUSTOMER_AMOUNT = "customer_amount";
        public static final String CUSTOMER_NUMBER_PARTNER = "customer_number";

    }

    public static class MerchantWeb {
        public static final String JSON_OBJECT = "merchant_web";
        public static final String IS_TEST_ENVIRONMENT = "test";
        public static final String MERCHANT_NUMBER = "merchantNumber";
        public static final String MERCHANT_NAME = "merchantName";
        public static final String DESCRIPTION = "description";
        public static final String AMOUNT = "amount";
        public static final String SOURCE_FROM = "sourcefrom";
        public static final String PIN = "pin";
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String STATUS = "status";
        public static final String MESSAGE = "message";
        public static final String USER_NAME = "username";
        public static final String HASH = "hash";
        public static final String AGENT_NAME = "agentName";
        public static final String DATA = "data";
        public static final String SERVICE_ID = "serviceId";
        public static final String BILL_ID = "billId";
        public static final String TRANSID = "transid";
        public static final String MERCHANT_ID = "merchantId";
        public static final String TID = "TID";
        public static final String CUSTOMER_NUMBER = "customerNumber";
        public static final String TOKEN = "token";
        public static final String TIMEOUT = "timeOut";
        public static final String KEY_SIZE = "keySize";
        public static final String PUBLIC_KEY = "publicKey";
        public static final String PRIVATE_KEY = "privateKey";
        public static final String FROM_NUMBER = "fromNumber";
        public static final String TO_NUMBER = "toNumber";
    }

    public static class HSBCKeyManage{
        public static final String HSBC_EXTRA_VALUE = "{type:2,\"resultCode\":0,\"form\":{\"type\":\"popup\",\"header\":[],\"body\":[{\"type\":\"vbox\",\"action\":4,\"submitData\":{},\"sendBackend\":false,\"contents\":[{\"type\":\"html\",\"action\":4,\"value\":%s},{\"type\":\"cash_source\",\"action\":2,\"submitData\":{},\"sendBackend\":false,\"contents\":[]}]}],\"footer\":[],\"submitData\":{\"amount\":%s,\"phonenumber_id\":%s,\"billid\":%s},\"backData\":{}}}";
    }

    public static class OctoberPromoProgram{
        public static final String OCTOBER_PROMO = "october_promo";
        public static final String OCTOBER_PROMO_STUDENT = "october_promo_student";
        public static final String JSON_OBJECT = "october_promo";
        public static final String IS_ACTIVE = "isActive";
        public static final String VALUE_OF_GIFT = "valueofgift";
        public static final String TIME_FOR_GIFT = "timeforgift";
        public static final String TIME_SCAN = "time_scan_expired_gift";
        public static final String AGENT = "agent";
        public static final String CREATE_DATE_LIMIT_START = "create_date_limit_start";
        public static final String CREATE_DATE_LIMIT_END = "create_date_limit_end";
        public static final String LUCKY_NUMBER = "lucky_number";
        public static final String MIN_LUCKY_AMOUNT = "min_lucky_amount";
        public static final String SCAN_EXPIRED_GIFT = "scan_expired_gift";
        public static final String NUMBER_OF_LUCKY_GIFT = "number_of_lucky_gift";
        public static final String TIME_SCAN_GIVING_GIFT = "time_scan_giving_gift";
        public static final String LIST_MAIN_NUMBER = "list_main_number";
        //OctoberPromoObj
        public static final String TRAN_ID = "tran_id";
        public static final String TRAN_TYPE = "tran_type";
        public static final String PHONE_NUMBER = "phone_number";
        public static final String SOURCE = "source";
        public static final String CARD_CHECK_SUM = "card_check_sum";
        public static final String CMND = "cmnd";

    }

    public static class ClaimPointFunction{
        public static final String JSON_OBJECT = "claim_point";
        public static final String AGENT =  "agent";
        public static final String CLAIM_AMOUNT = "claim_amount";
        public static final String IS_ACTIVE = "isActive";
    }

    //    "roll_back_50_percent":{
//        "isActive" : false,
//                "limit_amount" : 200000,
//                "agent" : "01000000000"
//    }
    public static class RollBack50Percent {
        public static final String ROLLBACK_PROMO = "rollback_promo";
        public static final String JSON_OBJECT = "roll_back_50_percent";
        public static final String IS_ACTIVE = "isActive";
        public static final String LIMIT_AMOUNT = "limit_amount";
        public static final String AGENT = "agent";
        public static final String BANK_CODES = "bank_codes";
        public static final String PERCENT = "percent";
        public static final String BONUS_BANK_CODES = "bonus_bank_codes";
        public static final String VCB_START_TIME = "vcb_start_time";
         //OctoberPromoObj
        public static final String TRAN_ID = "tran_id";
        public static final String TRAN_TYPE = "tran_type";
        public static final String PHONE_NUMBER = "phone_number";
        public static final String SOURCE = "source";
        public static final String CMND = "cmnd";
        public static final String BANK_TID = "bank_tid";
        public static final String BANK_AMOUNT = "bank_amount";
        public static final String BANK_CODE = "bank_code";
        public static final String TRAN_AMOUNT = "tran_amount";
        public static final String SERVICE_ID = "service_id";
        public static final String CARD_CHECK_SUM = "card_check_sum";
        public static final String VISA_PERCENT = "visa_percent";
        public static final String APP_CODE = "app_code";
    }

    public static class TranslateFromConst{
        //formId, caption, fieldItems, fieldlabel, button
        public static String TRANSLATE_FORM = "translateForm";
        public static String FORM_ID = "formId";
        public static String KEY = "key";
        public static String BUTTON_TITLE = "btn";
        public static String CAPTION = "cap";
        public static String FIELD_LABEL = "fieldlabel";
        public static String FIELD_ITEMS = "fields";

    }

    public static class StandardNoti{
        public static String CAPTION = "caption";
        public static String BODY = "body";
        public static String RECEIVER_NUMBER = "receiver_number";
        public static String TRAN_ID = "tran_id";
    }
    public static class lotteNoti{
        public static String CAPTIONNOTI = "Giảm ngay 100.000 đồng khi thanh toán tại LotteMart";
        public static String CAPTION = "Giảm ngay 100.000đ";
        public static String BODY = "Chúc mừng bạn! Bạn sẽ được giảm ngay 100.000đ cho lần thanh toán tiếp theo bằng ví MoMo với hóa đơn từ 300.000đ tại siêu thị LotteMart Nam Sài Gòn. Thẻ quà tặng có thời hạn đến hết ngày %s";
        public static String CAPTION22  = "Thẻ quà tặng LotteMart sắp hết hạn!";
        public static String BODY22 = "Chỉ còn 1 tuần để được giảm 100.000đ khi thanh toán bằng ví MoMo với hóa đơn từ 300.000đ tại LotteMart Nam Sài Gòn! Thời hạn %s. Bạn cần có tiền trong ví MoMo đủ để thanh toán. Kiểm tra số tiền trong ví của bạn nhé!";
        public static String CAPTION14 = "Bạn đang có thẻ quà tặng LotteMart 100.000đ!";
        public static String BODY14= "Bạn sẽ được giảm ngay 100.000đ khi thanh toán hóa đơn từ 300.000đ bằng ví MoMo tại LotteMart Nam Sài Gòn trước ngày %s ! Bạn cần có tiền trong ví MoMo đủ để thanh toán. Kiểm tra số tiền trong ví của bạn nhé!";
    }
    public static class vicPromoNoti{
        public static String CAPTIONNOTIGIFT1 ="Bạn được tặng 01 chuyến VIC Taxi miễn phí!";
        public static String BODYGIFT1 = "MoMo gửi tặng bạn chuyến đi VIC miễn phí, trị giá 50.000đ. Nạp từ 10.000đ vào ví để sử dụng quà tặng trước %s. Liên hệ: 043 8230 230. Hướng dẫn: https://momo.vn/thanhtoanvic";

        public static String CAPTIONNOTI="Bạn chưa sử dụng thẻ quà tặng VIC Taxi!";
        public static String BODYNOTI15 = "Bạn đã nhận thẻ quà tặng 50.000đ miễn phí khi đi VIC Taxi thanh toán bằng MoMo. Hãy nạp từ 10.000đ vào Ví để kích hoạt thẻ quà tặng và sử dụng trước ngày: %s.";



        public static String CAPTIONNOTIGIFT3 ="Bạn chưa sử dụng thẻ quà tặng VIC Taxi!";
        public static String BODYGIFT3 = "Bạn đã nhận thẻ quà tặng 50.000đ miễn phí khi đi VIC Taxi thanh toán bằng MoMo. . Hãy nạp từ 10.000đ vào ví để kích hoạt và sử dụng quà tặng trước %s";


        public static String REMIND_NOTI_BODY_GIFT = "Bạn đã nhận thẻ quà tặng 50.000đ miễn phí khi đi VIC Taxi thanh toán bằng MoMo. Thực hiện các bước theo hướng dẫn đểkích hoạt và sử dụng quà tặng trước %s. Xem hướng dẫn.";


        ///Sau 15 ngày kể từ ngày nhận thẻ quà tặng, Hệ thống kiểm tra KH đã cashin chưa. gift1
        public static String REMIND15_NOTI_CAPTION_GIFT2 = "Miễn phí 50.000đ đi VIC ngay hôm nay!";
        public static String REMIND15_NOTI_BODY_GIFT2 = " Bạn chỉ cần nạp tối thiểu 10.000đ vào Ví để nhận được chuyến xe VIC Taxi miễn phí 50.000đ thanh toán qua Ví MoMo. Hãy nạp tiền ngay hôm nay! Thời hạn: %s. Cách nạp tiền: http://goo.gle/12345";

        //Sau khi KH thực hiện cash in: (chỉ tính lần cash in đầu tiên kể từ thời điểm nhận voucher) dung dung nhac nho lan 2
        public static String CAPTIONNOTI_CASHIN1 ="Chuyến đi VIC đang sẵn sàng chờ bạn!";
        public static String BODY_CACSHIN1 = "Bạn đã kích hoạt thành công thẻ quà tặng 50.000đ áp dụng khi đi VIC Taxi thanh toán bằng MoMo. Thời hạn: %s  Hướng dẫn thanh toán VIC: https://momo.vn/thanhtoanvic/";

        //Hệ thống gửi voucher 2 và notification thông báo đến KH sau 7 ngày kể từ ngày sử dụng voucher
        public static String CAPTIONNOTI_GIVEVOUCHER2 ="Bạn được tặng thêm 01 chuyến VIC Taxi miễn phí!";
        public static String BODY_GIVEVOUCHER2 = "MoMo gửi tặng bạn chuyến đi VIC miễn phí, trị giá 50.000đ. Nạp từ 10.000đ vào ví để sử dụng quà tặng trước %s. Liên hệ: 043 8230 230. Hướng dẫn: https://momo.vn/thanhtoanvic/";

        //Sau 15 ngày kể từ ngày nhận thẻ quà tặng, Hệ thống kiểm tra KH đã cashin chưa. gift2
        public static String CAPTIONNOTI15DAYS_NOTUSEVOUCHER2 ="Miễn phí 50.000đ đi VIC ngay hôm nay!";
        public static String BODY15DAYS_NOTUSEEVOUCHER2 = "Bạn chỉ cần nạp tối thiểu 10.000đ vào Ví để nhận được chuyến xe VIC Taxi miễn phí 50.000đ thanh toán qua Ví MoMo. Hãy nạp tiền ngay hôm nay! Thời hạn: %s. Cách nạp tiền: http://goo.gle/12345";

        //Hệ thống gửi voucher 2 và notification thông báo đến KH sau 7 ngày kể từ ngày sử dụng voucher
        public static String CAPTIONNOTI_GIVEVOUCHER3 ="Bạn được tặng thêm 01 chuyến VIC Taxi miễn phí!";
        public static String BODY_GIVEVOUCHER3 = "MoMo gửi tặng bạn chuyến đi VIC miễn phí, trị giá 50.000đ.Thực hiện các bước theo hướng dẫn để sử dụng quà tặng trước %s. Liên hệ: 043 8230 230. Hướng dẫn:https://momo.vn/thanhtoanvic/";

        //Notification nhắc Định Danh và cashin gift3
        public static String CAPTIONNOTI_ISNAMECASHIN = "Miễn phí 50.000đ đi VIC ngay hôm nay!";
        public static String ISNAMECASHIN = "Cảm ơn bạn! MoMo gởi tặng ban 50.000đ miễn phí cho chuyến đi tiếp theo. Nạp tiền vào Ví tối thiểu 10.000đ để sử dụng chuyến đi VIC miễn phí nhé! Thời hạn: %s. Cách nạp tiền: http://goo.gle/12345";

        //Notification nhắc Định Danh và cashin gift3
        public static String CAPTION_GIVEVOUCHER_ISNAME = "Quà tặng 3 - Tặng thêm 50.000đ miễn phí cho chuyến đi vui vẻ cùng VIC!";
        public static String BODY_GIVEVOUCHER_ISNAME = "Cảm ơn bạn! MoMo gởi tặng ban 50.000đ miễn phí cho chuyến đi tiếp theo. Nạp tiền vào Ví tối thiểu 10.000đ để sử dụng chuyến đi VIC miễn phí nhé! Thời hạn: %s. Cách nạp tiền: http://goo.gle/12345";


    }
    public static class RedirectNoti{
        public static String CAPTION = "caption";
        public static String BODY = "body";
        public static String RECEIVER_NUMBER = "receiver_number";
        public static String TRAN_ID = "tran_id";
        public static String URL = "url";
        public static String TYPE = "type";
    }

    public static class MerchantContent{
        public static String CAPTION_MERCHANT_NOTI = "Nhận tiền thành công";
        public static String BODY_MERCHANT_NOTI = "Bạn đã nhận thành công số tiền %sđ từ %s.";
        public static String BODY_MERCHANT_TRANHIS = "Bạn đã nhận thành công số tiền %sđ từ %s.";

        public static String FAIL_CAPTION_CUSTOMER_NOTI = "Thanh toán không thành công";
        public static String FAIL_BODY_CUSTOMER_NOTI_FPT_PLAY = "Bạn đã thanh toán không thành công số tiền %sđ cho nhà cung cấp %s. Vui lòng liên hệ FPT Play (08)73008888 – (04)73008888 hoặc MoMo 08.39917199 để được hỗ trợ.";
        public static String FAIL_BODY_CUSTOMER_TRANHIS_FPT_PLAY = "Bạn đã thanh toán không thành công số tiền %sđ cho nhà cung cấp %s. Vui lòng liên hệ FPT Play (08)73008888 – (04)73008888 hoặc MoMo 08.39917199 để được hỗ trợ.";
        public static String FAIL_BODY_CUSTOMER_NOTI = "Qúy khách đã thanh toán không thành công số tiền %sđ cho nhà cung cấp %s. Vui lòng liên hệ %s %s hoặc MoMo 1900545441 để được hỗ trợ.";
        public static String FAIL_BODY_CUSTOMER_TRANHIS = "Qúy khách đã thanh toán không thành công số tiền %sđ cho nhà cung cấp %s. Vui lòng liên hệ %s %s hoặc MoMo 1900545441 để được hỗ trợ.";


        public static String SUCCESS_CAPTION_CUSTOMER_NOTI = "Thanh toán thành công";
        public static String SUCCESS_BODY_CUSTOMER_NOTI = "Qúy khách đã thanh toán thành công số tiền %sđ cho nhà cung cấp %s. Cám ơn Quý Khách đã sử dụng dịch vụ của MoMo.";
        public static String SUCCESS_BODY_CUSTOMER_TRANHIS = "Qúy khách đã thanh toán thành công số tiền %sđ cho nhà cung cấp %s. Cám ơn Quý Khách đã sử dụng dịch vụ của MoMo.";
    }

//    "turn_off_noti":{
//        "list_number" : ["0123456789", "0123456789"],
//        "list_agent" : ["vcb.bank"],
//        "isActive":true
//    }
    public static class TurningOffNotification{
        public static String JSON_OBJECT = "turn_off_noti";
        public static String LIST_NUMBER = "list_number";
        public static String LIST_AGENT = "list_agent";
        public static String IS_ACTIVE = "isActive";

    }

    public static class DollarHeartCustomerCareGiftGroupString
    {
        public static String PHONE_NUMBER = "phonenumber";
        public static String AGENT = "agent";
        public static String GIFT_TYPE_ID = "giftTypeId";
        public static String GIFT_VALUE = "giftValue";
        public static String DURATION = "duration";
        public static String PROGRAM = "program";
        public static String GROUP = "group";

        public static String JSON_OBJECT = "reactive_customer";
        public static String GROUP_DOLLAR = "group_dollar";
        public static String GROUP_HEART = "group_heart";
        public static String GROUP_DOLLAR_VALUE = "gift_dollar_value";
        public static String GROUP_HEART_VALUE = "gift_heart_value";
    }

    public static class BillRuleManagement{
        public static String JSON_OBJECT = "bill_rule_management";
        public static String PAY_TIMES = "pay_times";
//        "bill_rule_management":{
//            "pay_times" : 2
//        }
    }

    public static class ConnectorNotification{
        public static String JSON_OBJECT = "connector_notification";
        public static String IS_BUILD = "is_build";
        public static String HOST = "host";
        public static String PORT = "port";
        public static String PATH = "path";
        public static String JSON_INFO = "jsonInfo";
    }

    public static class ZaloSMS {
        public static String JSON_OBJECT = "zalo_sms";
        public static String SEND_BY_ZALO = "sendByZalo";
        public static String ZALO_OTP_SMS_MESSAGE_TEMPLATE = "zaloOtpSmsMessageTemplate";
        public static String PAGE_ID = "pageId";
        public static String SECRET_KEY = "secretKey";
        public static String TIMEOUT = "timeOut";
    }

    public static class ZaloPromo {
        public static String ZALO_CASHBACK_PROGRAM = "zalo_cashback";
        public static String ZALO_PROGRAM = "Zalo_Promo";
        public static String JSON_OBJECT = "zalo_promo";
        public static String SOURCE = "source";
        public static String AMOUNT = "amount";
        public static String TRAN_ID = "tranId";
        public static String SERVICE_ID = "serviceId";
        public static String AMOUNT_BONUS_MAXIMUM = "amount_bonus_max";
        public static String IS_ACTIVE = "isActive";
        public static String AGENT = "agent";
        public static String AGENT_CASHBACK = "agent_cashback";
        public static String TIME_FOR_MONEY = "timeformoney";
        public static String TIME_FOR_GIFT = "timeforgift";
        public static String VALUE_OF_MONEY = "valueofmoney";
        public static String VALUE_OF_GIFT = "valueofgift";
        public static String SCAN_EXPIRED_GIFT = "scan_expired_money";
        public static String URL = "url";
        public static String PROMOTION_TIME = "promotion_time";
        public static String CASH_BACK_TIME = "cashback_time";
        public static String NOTI_GROUP_T = "NOTI_GROUP_T";
        public static String NOTI_GROUP_T_1_A = "NOTI_GROUP_T_1_A";
        public static String NOTI_GROUP_T_1_B = "NOTI_GROUP_T_1_B";

        public static String NOTI_GROUP_T_2 = "NOTI_GROUP_T_2";
        public static String NOTI_GROUP_T_3 = "NOTI_GROUP_T_3";
        public static String NOTI_GROUP_T_4 = "NOTI_GROUP_T_4";
        public static String NOTI_GROUP_T_5 = "NOTI_GROUP_T_5";
        public static String NOTI_GROUP_T_6 = "NOTI_GROUP_T_6";
        public static String NOTI_GROUP_T_7 = "NOTI_GROUP_T_7";
        public static String NOTI_GROUP_T_14_A = "NOTI_GROUP_T_14_A";
        public static String NOTI_GROUP_T_14_B = "NOTI_GROUP_T_14_B";
        public static String NOTI_GROUP_T_21 = "NOTI_GROUP_T_21";
        public static String NOTI_GROUP_T_28 = "NOTI_GROUP_T_28";
        public static String NOTI_GROUP_T_35 = "NOTI_GROUP_T_35";
        public static String NOTI_GROUP_T_42 = "NOTI_GROUP_T_42";
        public static String NOTI_GROUP_T_45 = "NOTI_GROUP_T_45";
        public static String NOTI_GROUP_T_50 = "NOTI_GROUP_T_50";
        public static String NOTI_GROUP_T_70 = "NOTI_GROUP_T_70";
        public static String NOTI_GROUP_T_80 = "NOTI_GROUP_T_80";
        public static String NOTI_GROUP_T_90 = "NOTI_GROUP_T_90";

    }

    public static class ConnectorServiceBusName{
        public static String ID = "_id";
        public static String SERVICE_ID = "service_id";
        public static String BUS_NAME = "busname";
        public static String BILL_PAY = "billpay";
        public static String SERVICE_TYPE = "service_type";
        public static String SERVICE_TYPE_NAME = "service_type_name";
    }

    public static class ClaimCodePromotion{
        public static String CLAIM_PROGRAM = "claimProgram";
        public static String CODE = "code";
        public static String DEVICE_IMEI = "deviceImei";
        public static String PHONE_NUMBER = "phoneNumber";
        public static String AMOUNT = "amount";
        public static String TRAN_ID = "tran_id";
        public static String SERVICE_ID = "serviceId";
        public static String PROMOTION_TIME = "promotionTime";
        public static String COMMAND = "command";
        public static String EXTRA = "extra";
        public static String POST_SALE_JUPVIEC_SERVICE = "post_sale_jupviec_service";
    }

    public static class WomanNationalField{
        public static String PROGRAM = "woman_international_2016";
        public static String EXTRA = "extra";
        public static String PHONE_OBJ = "phoneObj";
    }

    public static class DGD2MillionsPromoField{
        public static String GET_FEE_PROGRAM = "dgdpromo_getfee";
        public static String BONUS_PROGRAM = "dgdpromo_bonus";
        public static String AGENT = "agent";
        public static String START_DATE = "start_date";
        public static String END_DATE = "end_date";
        public static String AMOUNT = "amount";
    }

    public static class ReferralVOnePromoField{
        public static String CODE = "code";
        public static String DEVICE_IMEI = "deviceImei";
        public static String PHONE_NUMBER = "phoneNumber";
        public static String AMOUNT = "amount";
        public static String TRAN_ID = "tran_id";
        public static String SERVICE_ID = "serviceId";
        public static String PROMOTION_TIME = "promotionTime";
        public static String COMMAND = "command";
        public static String EXTRA = "extra";
        public static String REFERRAL_PROGRAM = "referralv1_promo";
        public static String REFERRAL_CASHBACK_PROGRAM = "referralv1_cashback_promo";
        public static String REFERRAL_PREFIX  = "R1";
        public static String SPLIT_SYMBOL     = "_";
        public static String INVITER_PHONE_OBJ = "inviter_phoneObj";
        public static String INVITEE_PHONE_OBJ = "invitee_phoneObj";
        public static String REFERRAL_OBJ = "referral_obj";
        public static String BANK_ACC = "bankacc";
        public static String CARD_INFO = "card_info";
        //MSG_TYPE
        public enum MSG_TYPE_REFERRAL{
            FIRST_TIME_BANK_MAPPING, BANK_MAPPING, CASH_BACK, BACKEND_BANK_MAPPING, CASH_IN;

            public static MSG_TYPE_REFERRAL getType(String s) {
                if(s == FIRST_TIME_BANK_MAPPING.toString()) {
                    return FIRST_TIME_BANK_MAPPING;
                } else if(s == BANK_MAPPING.toString()) {
                    return BANK_MAPPING;
                } else if(s == CASH_BACK.toString()) {
                    return CASH_BACK;
                } else if(s == BACKEND_BANK_MAPPING.toString()) {
                    return BACKEND_BANK_MAPPING;
                } else {
                    return null;
                }
            }
        }
    }

    public static class ResetPasswordFields{
        public static String APP_CODE = "";
        public static String APP_OS = "";
    }

    public static class StandardCharterBankPromotion
    {
        public static String PROGRAM = "SCBPromotion";

        public enum MSG_TYPE_SCB_PROMOTION{
            BANK_MAPPING, CASH_BACK;

            public static MSG_TYPE_SCB_PROMOTION getType(String s) {
                if(s == BANK_MAPPING.toString()) {
                    return BANK_MAPPING;
                } else if(s == CASH_BACK.toString()) {
                    return CASH_BACK;
                } else {
                    return null;
                }
            }
        }
    }

    public static class BinhTanPromotion{
        public static String ECHO = "binhtan_promotion_echo";
        public static String PROGRAM = "binhtan_promotion";
        public static String PROGRAM_OUT = "binhtan_promotion_out";
        public static String PROGRAM_GROUP2 = "binhtan_promotion_2";
        public static String PROGRAM_GROUP3 = "binhtan_promotion_3";
        public static String PROGRAM_GROUP4 = "binhtan_promotion_4";
        public static String PROGRAM_GROUP5 = "binhtan_promotion_5";
        public static String CASHIN_SOURCE = "cashin_source";
        public static String EXTRA_KEY = "extra_key";
        public static String PROGRAM_KEY = "program";
        public static String SOURCE = "source";
        public static String ACQUIRE_USER_OBJ = "acquire_user_obj";
        public static String PHONE_NUMBER = "phone_number";
        public static String BANK_ID = "bankId";
        public static String MSG_TYPE = "msg_type";
        public enum MSG_TYPE_BINHTAN_PROMOTION{

            REGISTER, LOGIN, CASH_IN, BILL_PAY, UPDATE_BILL_PAY, ECHO, GET_OTP, NONE;

            public static MSG_TYPE_BINHTAN_PROMOTION getType(String s) {
                if(s.equalsIgnoreCase(REGISTER.toString())) {
                    return REGISTER;
                } else if(s.equalsIgnoreCase(LOGIN.toString())) {
                    return LOGIN;
                } else if(s.equalsIgnoreCase(CASH_IN.toString())) {
                    return CASH_IN;
                } else if(s.equalsIgnoreCase(BILL_PAY.toString())) {
                    return BILL_PAY;
                } else if(s.equalsIgnoreCase(UPDATE_BILL_PAY.toString())) {
                    return UPDATE_BILL_PAY;
                } else {
                    return null;
                }
            }
        }
    }

    public static class TranTypeExtra{
        public static int FIRST_WALLET_MAPPING = 3001;
    }

    public static class BroadcastField{
        public static String DATA = "data";
        public static String TYPE = "type";
        public static String NOTI = "noti";
        public static String NOTI_FROM_TOOL = "noti_from_tool";
        public static String NOTI_FROM_TOOL_WITH_REDIS = "noti_from_tool__with_redis";
        public static String UPDATE_LIST_CACHE = "update_list_cache";
        public static String POPUP = "popup";
        public static String COMMAND = "command";
    }

    public static class PromotionField{
        public static String PHONE_NUMBER = "phoneNumber";
        public static String BILL_ID = "billId";
        public static String TRAN_TYPE = "tranType";
        public static String CUSTOMER_NUMBER = "customerNumber";
        public static String TRAN_ID = "tranId";
        public static String AMOUNT = "amount";
        public static String SERVICE_ID = "serviceId";
        public static String FEE = "fee";
        public static String GIFT_ID_LIST = "giftIdList";
        public static String SOURCE_FROM = "srcFrom";
        public static String PHONE_OBJ = "phoneObj";
        public static String IS_STORE_APP = "isStoreApp";
        public static String JSON_EXTRA = "joExtra";
        public static String ERROR = "error";
        public static String RESULT = "result";
        public static String ADDRESS = "address";
        public static String NOTIFICATION = "noti";
        public static String DESCRIPTION = "description";
        public static String GIFT_TID = "gift_tid";
        public static String GIFT_ID = "gift_id";
        public static String GIFT = "gift";
        public static String CODE = "code";
        public static String DATA = "data";
        public static String GIFT_POSITION = "gift_position";
        //Cong Nguyen 10/08/2016 add new field to get bank id was accepted join promotion
        public static String BANK_LIST = "bank_list";
        public static String DATA_POSITION = "data_position";
        public static String TOTAL_DATA = "total_data";
    }

    public static class MOMO_TOOL
    {
        public static String JSON_MOMO_TOOL = "momo_tool";
        public static String HOST = "host";
        public static String PORT = "port";
        public static String PHONE_NUMBER = "number";
        public static String PHONE_LIST = "phone_list";
    }

    public static class GRAB_PROMO {
        public static String NAME = "grab_promotion";

    }

    public static class RETAIN_BINHTAN_PROMO {
        public static String NAME = "retain_binhtan_promotion";

    }
//    "email_data":{
//        "host":"",
//                "port":"",
//                "tls":true,
//                "sender_email":"",
//                "sender_pass" :"",
//                    "receiver_ToEmail":[],
//    "receiver_CCEmail":[],
//            "from":""
//    }
    public static class EMAIL {
        public static String JSON_OBJECT = "email_data";
        public static String HOST = "host";
        public static String PORT = "port";
        public static String SENDER_EMAIL = "sender_email";
        public static String SENDER_PASS = "sender_pass";
        public static String RECEIVER_TO_EMAIL = "receiver_ToEmail";
        public static String RECEIVER_CC_EMAIL = "receiver_CCEmail";
        public static String FROM = "from";
        public static String TLS = "tls";

        public static String SUBJECT = "subject";
        public static String BODY = "body";
        public static String REFERENCE = "reference";
        public static String APP_CODE = "appcode";
        public static String APP_VER = "appver";
        public static String EMAIL_TYPE = "emailType";
    }

    public static class BALANCE_TOTAL {
        public static String MOMO = "momo";
        public static String GIFT = "gift";
        public static String POINT = "point";
    }


}
