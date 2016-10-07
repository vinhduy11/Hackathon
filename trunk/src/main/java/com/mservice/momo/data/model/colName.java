package com.mservice.momo.data.model;

/**
 * Created by User on 3/22/14.
 */
public class colName {


    public static class testDbCols {
        public static String TEST_ID = "_id";
        public static String TEST_NAME = "testName";
        public static String TEST_NGAY_SINH = "testNgaySinh";
        public static String TABLE = "test";
        public static String DELETED = "del";
        public static String NUMBER = "number";

    }

    public static class TranDBCols {
        public static String _ID = "_id";
        public static String COMMAND_INDEX = "cmdId";
        public static String TRAN_ID = "tranId";
        public static String CLIENT_TIME = "ctime";
        public static String ACK_TIME = "atime";
        public static String FINISH_TIME = "ftime";
        public static String TRAN_TYPE = "tranType";
        public static String IO = "io";
        public static String CATEGORY = "cat";
        public static String PARTNER_ID = "pid";
        public static String PARTNER_CODE = "pcode";
        public static String PARTNER_NAME = "pname";
        public static String PARTNER_REF = "pref";
        public static String BILL_ID = "billId";
        public static String AMOUNT = "amt";
        public static String COMMENT = "cmt";
        public static String STATUS = "status";
        public static String OWNER_NUMBER = "number";
        public static String OWNER_NAME = "name";
        public static String PARRENT_TRAN_TYPE = "parenttranType";
        public static String ERROR = "error";
        public static String BALANCE = "balance";
        public static String FROM_SOURCE = "fromsrc";
        public static String IS_M2NUMBER = "ism2number";
        //extra info
        public static String DELETED = "del";
        public static String TABLE_PREFIX = "tran_";

        public static String PARTNER_EXTRA_1 = "partnerExtra1";

        //doi soat voi ben thu 3
        public static String PARTNER_INVOICE_NO = "partnerInvNo";
        public static String PARTNER_TICKET_CODE = "partnerTicketCode";
        public static String PARTNER_ERROR = "partnerError";
        public static String PARTNER_DESCRIPTION = "partnerDesc";
        public static String PARTNER_ACTION = "partnerAction";

        public static String DESCRIPTION = "desc";
        public static String FORCE_COMMENT = "forceCmt";

        public static String KVP = "kvp";
        public static String SHARE = "share";
        public static String PHONE = "phone";
    }

    public static class ImeiDBCols {
        public static String IMEI = "imei";
        public static String IMEI_KEY = "imei_key";
        public static String NUMBER = "number";
        public static String CREATE_TIME = "ctime";
        public static String OPERATING_SYSTEM = "os";

        public static String TABLE = "imeis";

    }

    public static class AccessHistoryDBCols {
        public static String IP = "ip";
        public static String START_TIME_ACCESS = "tin";
        public static String END_TIME_ACCESS = "tout";
        public static String DEVICE_MODEL = "device";
    }

    public static class PhoneDBCols {

        public static String NUMBER = "number";
        public static String NAME = "name";
        public static String CARD_ID = "card_id";
        public static String MOMO = "momo";
        public static String MLOAD = "mload";
        public static String IS_REGED = "is_reged";
        public static String IS_NAMED = "is_named";
        public static String IS_ACTIVED = "is_actived";
        public static String IS_SETUP = "is_setup";
        public static String SESSION_KEY = "session_key";
        public static String LAST_IMEI = "last_imei";
        public static String LAST_TIME = "last_time";
        public static String PIN = "pin";
        public static String EMAIL = "email";
        public static String QUESTION = "question";
        public static String ANSWER = "answer";

        public static String MPOINT = "mpt";
        public static String DATE_OF_BIRTH = "dob";
        public static String ADDRESS = "addr";
        public static String OTP_TIME = "otp_time";
        public static String OTP = "otp";

        //for bank
        public static String BANK_NAME = "bnk_name";
        public static String BANK_ACCOUNT = "bnk_acc";
        public static String BANK_CODE = "bnk_code";

        //for trying login exceed
        public static String MAX_LOGIN_COUNT = "login_cnt";
        public static String LOCKED_UNTIL = "locked_until";

        public static String LAST_UPDATE_TIME = "last_update_time";

        //for push notification
        public static String IMEI = "imei";
        public static String IMEI_KEY = "imei_key";
        public static String PHONE_OS = "os";
        public static String PUSH_TOKEN = "push_token";

        public static String NONAME_TRAN_COUNT = "nncnt";
        public static String DELETED = "del";

        //FOR PROMOTION INVITER/INVITEE
        //so dien thoai duoc gioi thieu
        public static String REFERENCE_NUMBER = "refer_num";

        //so lan tu minh duoc nhan
        public static String INVITER_COUNT = "iter_cnt";

        //so lan duoc tang tu reference
        public static String INVITEE_COUNT = "itee_cnt";
        public static String CREATED_DATE = "create_date";
        public static String IS_INVITER = "isinviter";

        public static String APPCODE = "appCode";
        public static String APPVER = "appVer";

        public static String INVITER = "inviter";

        public static String LAST_CMD_IND = "lastCmdInd";
        public static String BANK_PERSONAL_ID = "bnk_cardid";
        public static String INVITE_TIME = "invite_time";

        public static String WAITING_REG = "waitingReg";
        public static String IS_AGENT = "isAgent";
        public static String DEVICE_INFO = "deviceInfo";
        public static String STATUS_ATMCARD = "statusAtmCard";
        public static String BALANCE_TOTAL = "balance_total";
        public static String IS_LOCKED_V1 = "is_locked_v1";
        public static String TABLE = "phones";

    }

    public static class BillDBCols {
        public static String TABLE_PREFIX = "bills_";
        public static String PROVIDER_ID = "pid";
        public static String BILL_ID = "bid";
        public static String BILL_DETAIL = "bdetail";
        public static String STATUS = "status";
        public static String TOTAL_AMOUNT = "amt";
        public static String CHECK = "chk";

        public static String OWNER_NAME = "oname";
        public static String OWNER_ADDRESS = "oaddr";
        public static String OWNER_PHONE = "ophone";
        public static String PAY_TYPE = "payType";
        public static String TRANSFER_TYPE = "tferType";
        public static String PAY_SCHEDULER = "paySchler";
        public static String PAY_CHANEL = "payCnel";
        public static String DUE_DATE = "dueDate";

        public static String START_DATE = "startDate";
        public static String END_DATE = "endDate";

        public static String PAY_DATE = "payDate";

        public static String DELETED = "deleted";
    }

    public static class AgentDBCols {
        public static String _ID = "_id";
        public static String LOCATION = "loc";
        public static String DISTRICT_ID = "did";
        public static String DISTRICT_NAME = "dname";
        public static String DISTRICT_NAME_NOR = "dname_nor";
        public static String CITY_ID = "cid";
        public static String CITY_NAME = "cname";
        public static String CITY_NAME_NOR = "cname_nor";
        public static String AREA_ID = "aid";
        public static String LAST_UPDATE_TIME = "lastUpTime";
        public static String NAME = "name";
        public static String NAME_NOR = "name_nor";
        public static String PHONE = "phone";
        public static String ADDRESS = "add";
        public static String ADDRESS_NOR = "add_nor";
        public static String STREET = "street";
        public static String STREET_NOR = "street_nor";
        public static String WARD = "ward";
        public static String WARD_NAME = "wname";
        public static String WARD_NAME_NOR = "wname_nor";
        public static String STORE_NAME = "s_name";
        public static String STORE_NAME_NOR = "s_name_nor";
        public static String LONGITUDE = "lng";
        public static String LATITUDE = "lat";
        public static String ROW_ID = "rid"; // quan ly ben local server
        public static String ROW_CORE_ID = "rcoreId";
        public static String DELETED = "del";
        public static String MOMO_PHONE = "mmphone";
        public static String STATUS = "status";
        public static String FULL_ADD_NOR = "full_add_nor";
        public static String TDL = "tdl";
        public static String ACTIVE_DATE = "active_date";
        public static String AGENT_TYPE = "agentType";
        public static String TABLE = "stores";

        /*p.ID rcoreid
        ,p.STORE_NAME s_name
        ,p.CITY_ID cid
        ,ci.NAME cityname
        ,p.AREA_ID aid
        ,p.DISTRICT_ID did
        ,d.NAME districtname
        ,p.WARD_ID ward
        ,w.NAME wardname
        ,p.FULL_NAME name
        ,p.STREET
        ,p.DATE_MODIFIED LastUpTime
        ,p.HOUSE_NUMBER "add"
                ,p.HOME_PHONE phone
        ,sq.IS_CLOCK del
        ,p.LATITUDE lat
        ,p.LONGITUDE lng
        ,ac.MOMO_PHONE momophone*/


    }

    public static class DepositInPlaceDBCols {
        public static String NUMBER = "number";
        public static String ADDRESS = "addr";
        public static String FULL_NAME = "name";
        public static String AMOUNT = "amount";
        public static String CREATE_TIME = "create_time";
        public static String APPROVE_TIME = "appr_time";
        public static String APPROVE_BY = "appr_by";
        public static String STATUS = "status";
        public static String ERROR = "error";
        public static String REMARK = "remark";
        public static String TABLE = "deposit";
        public static String IS_NAMED = "isNamed";
        public static String SERVICE_TYPE = "serviceType";
        public static String FROM_SOURCE = "fromsrc";
        public static String RESULT = "result";

        public static enum Status {
            NEW, APPROVED, DELETED;

            public static String getStatus(Status status) {
                switch (status) {
                    case NEW:
                        return "new";
                    case APPROVED:
                        return "approved";
                    case DELETED:
                        return "deleted";
                    default:
                        return "";
                }
            }
        }

    }

    public static class CardDBCols {
        public static String CARD_HOLDER_YEAR = "year";        // nam tren card
        public static String CARD_HOLDER_MONTH = "month";       // thang tren card
        public static String CARD_HOLDER_NUMBER = "bankacc";     // tai khoan ngan hang
        public static String CARD_HOLDER_NAME = "name";        // ten chu tai khoan
        public static String BANKID = "bankid";      // cybersource
        public static String DELETED = "del";         // xoa hay chua
        public static String BANK_TYPE = "bnktype";     // loai tai khoan
        public static String BANK_NAME = "bnkname";     // ten ngan hang
        public static String ROW_ID = "rowid";       //
        public static String LAST_SYNC_TIME = "lsynctime";   //
        public static String ICON_URL = "iconurl";
        //        public static String CARD_CHECK_SUM = "card_check_sum";
        public static String STATUS = "status";
        public static String CARD_ID = "cardId";
        public static String CARD_TYPE = "cardType";
        public static String CARD_CHECKSUM = "cardCheckSum";
        public static String TABLE_PREFIX = "card_";
    }

    public static class CoreLastTimeDBCols {
        public static String LAST_UPDATE_TIME = "lastUpTime";
        public static String ROW_ID = "rowid";
        public static String TABLE = "LastCoreUpTime";
    }

    public static class M2NumberCols {

        //cac ham core can co : lock, confirm, unlock
        //lock --> insert tran --> status = dang xu ly + tranid
        //unlock --> delete tran --> status = dang xu ly  + tranid

        public static String SOURCE_PHONE = "sphone";
        public static String DESTINATION_PHONE = "dphone";
        public static String NAME = "name";
        public static String CARD_ID = "cardid";
        public static String AMOUNT = "amt";
        public static String NOTICE = "notice";
        public static String CREATE_TIME = "createtime";
        public static String UPDATE_TIME = "updatetime";
        public static String UPDATE_BY = "updateby";
        public static String STATUS = "status";
        public static String ERROR = "error";
        public static String REMARK = "rmk";
        public static String TRAN_ID = "tranId";
        public static String SHORT_LINK = "link";
        public static String CODE = "_id";

        public static String COMMAND_INDEX = "cmdIndex";
        public static String MESSAGE_TYPE = "msgType";
        public static String SEED_NUMBER = "sdnumber";
        public static String SMS_SENDED = "smssent";

        public static String TABLE = "m2number";

        public static enum Status {
            NEW, APPROVED, DELETED, ROLLBACK, EXPIRED;

            public static String getStatus(Status status) {
                switch (status) {
                    case NEW:
                        return "new";
                    case APPROVED:
                        return "appr";
                    case DELETED:
                        return "del";
                    case ROLLBACK:
                        return "rollbck";
                    case EXPIRED:
                        return "exp";
                    default:
                        return "";
                }
            }
        }
    }

    public static class ReqMoneyCols {
        public static String NUMBER = "number";
        public static String COUNT = "cnt";
        public static String CURRENT_TIME = "curtime";
        public static String TABLE = "req_money";
    }

    public static class M2cOfflineSeedCols {
        public static String INDEX = "idx";
        public static String CURRENT_NUMBER = "cnumber";
        public static String TABLE = "seedtb";
    }

    public static class DeviceInfoDBCols {
        public static String NUMBER = "number";
        public static String DEVICE_NAME = "dname";
        public static String DEVICE_VERSION = "dver";
        public static String DEVICE_MODEL = "dmodel";
        public static String DEVICE_MANUFACTURER = "dmnfer";
        public static String APP_VERSION = "appver";
        public static String DEVICE_WIDTH = "dw";
        public static String DEVICE_HEIGTH = "dh";
        public static String DEVICE_PRIMARY_EMAIL = "dpriemail";
        public static String OPERATING_SYSTEM = "os";
        public static String TOKEN = "token";
        public static String CREATE_TIME = "ctime";

        public static String TABLE = "devices";
    }

    public static class CfgAtRuning {
        public static String TASK = "task";
        public static String TRAN_TYPE = "tranType";
        public static String VALUE = "value";
        public static String SUCCESS = "success";

    }

    public static class FeeDBCols {
        public static String ID = "_id";
        public static String BANKID = "bankid";
        public static String CHANNEL = "channel"; // banknet/smartlink/onepay
        public static String TRANTYPE = "trantype"; // bieu phi ap dung cho loai giao dich nao
        public static String DYNAMIC_FEE = "dynamicfee"; // phi dong
        public static String STATIC_FEE = "staticfee"; // phi tinh
        public static String INOUT_CITY = "iocity"; // trong ngoai thanh pho
        public static String TABLE = "fee"; // phi tinh
        public static String BANK_NAME = "bankname";
        public static String FEE_TYPE = "feetype";
        public static String MIN_VALUE = "minval";
        public static String MAX_VALUE = "maxval";
    }

    public static class BankDBCols {
        public static String BANKID = "bankid";   // ma ngan hang
        public static String CHANNEL = "bankname";// ten ngan hang
        public static String TABLE = "bank";      // phi tinh
    }

    public static class NotificationCols {
        public static final String NID = "nId";
        public static final String TYPE = "type";
        public static final String SENDER = "sender";
        public static final String DESC = "desc";
        public static final String BODY = "body";
        public static final String STATUS = "status";
        public static final String TIME = "time";
        public static final String TRAN_ID = "tranId";
    }

    public static class CoreBalanceCols {
        public static final String NUMBER = "number";
        public static final String BALANCE = "balance"; //momo
        //
//        public static final String MOMO = "momo";
        public static final String MLOAD = "mload";
        public static final String POINT = "point";
        public static final String VOUCHER = "voucher";
    }

    public static class BankManualCols {
        public static final String NUMBER = "number";
        public static final String BANK_NAME = "bankName";
        public static final String PENDING_TRAN_ID = "pendingTranId";
        public static final String CARD_NAME = "cardName";
        public static final String CARD_NUMBER = "cardNumber";
        public static final String AMOUNT = "amount";
        public static final String RECEIVED_AMOUNT = "receivedAmount";
        public static final String FEE_AMOUNT = "feeAmount";
        public static final String COMMENT = "comment";
        public static final String INOUT_CITY = "iocity";
        public static final String RESULT = "result";
    }

    public static class MinMaxForTranCols {
        public static String TRAN_TYPE = "trantype";
        public static String IS_NAMED = "isnamed";
        public static String MIN_VALUE = "minval";
        public static String MAX_VALUE = "maxval";
        public static String TRAN_NAME = "tranname";
        public static String ROW_ID = "_id";
        public static String LAST_MODIFY_BY = "create_by";
        public static String LAST_MODIFY_TIME = "create_time";
        public static String TABLE = "MinMaxTran";
    }

    public static class PromoCols {
        public static String ID = "_id";
        public static String NAME = "name"; // ten khuyen mai
        public static String DESCRIPTION = "desc"; // mo ta
        public static String DATE_FROM = "fromdate"; // khuyen mai tu ngay
        public static String DATE_TO = "todate"; // khuyen mai den ngay
        public static String TRAN_MIN_VALUE = "tranminval"; // giao tri toi thieu cua 1 giao dich de nhan khuyen mai : 20K
        public static String PER_TRAN_VALUE = "pertranval"; // gia tri khuyen mai duoc cong : 10k
        public static String TRAN_TYPE = "trantype";


        //thoi gian keo dai khuyen mai tu luc dang ky vi
        // 0: thuc hien khuyen mai the FromDate to ToDate
        //>1: thuc hien khuyen mai theo ngay tao moi vi
        public static String DURATION = "delaytime";

        public static String MAX_VALUE = "maxval"; // tong gia tri khuyen mai toi da 100K
        public static String INTRO_DATA = "introdata"; // noi dung gio thieu chung trinh khuyen mai bang data
        public static String INTRO_SMS = "introsms"; // noi dung ban sms gioi thieu chuong trinh khuyen mai

        // chuong trinh dang duoc kich hoat hay khong
        // false : khong ; true: kich hoat
        public static String ACTIVE = "active";
        // tinh theo % gia tri giao dich hoac gia tri co dinh
        // val --> gia tri khuyen mai = PROMOTION_VALUE; per gia tri khuyen mai = (PROMOTION_VALUE * Gia tri giao dich)/100
        public static String TYPE = "type";

        //so lan khuyen mai toi da co inviter,10 lan
        public static String MAX_TIMES = "maxtimes";

        //so lan khuyen mai toi thieu cho invitee, 1 lan
        public static String MIN_TIMES = "mintimes";

        //for noti
        public static String NOTI_CAPTION = "noticap";
        //for inviter
        public static String NOTI_BODY_INVITER = "notiboiter";
        public static String NOTI_SMS_INVITER = "notismsiter";

        //for invitee
        public static String NOTI_BODY_INVITEE = "notiboitee";
        public static String NOTI_SMS_INVITEE = "notismsitee";

        public static String NOTI_COMMENT = "noticmt"; // notification comment

        //tai khoan dung de chuyen tien khuyen mai cho khach hang
        public static String ADJUST_ACCOUNT = "adjustacc";

        public static String ADJUST_PIN = "pin";
        public static String DURATION_TRAN = "duratetran";

        public static String CREATE_TIME = "ctime";

        public static String OFF_TIME_FROM = "offtimefrom";
        public static String OFF_TIME_TO = "offtimeto";
        public static String STATUS = "status";
        public static String STATUS_IOS = "status_ios";
        public static String STATUS_ANDROID = "status_android";
        public static String ENABLE_PHASE2 = "enablePhase2";

        public static String TABLE = "promo";
        public static String ADDRESSNAME = "addressName";
        public static String ISVERTICLE = "isVerticle";
        public static String EXTRA = "extra";
    }

    public static class PartnerTrackCols {

        public static String NAME = "name"; // ten khach hang
        public static String EXECUTE_NUMBER = "exec_number"; // so dien thoai thuc hien giao dich

        public static String PHONE_NUMBER = "phone_number"; // so dien thoai nguoi di xem phim
        public static String EMAIL = "email"; // email khach hang
        public static String AMOUT = "amount"; // so tien
        public static String CREATE_DATE = "create_time"; // ngay tao
        public static String TRAN_ID = "tran_id";
        public static String PARTNER_ACC = "partner_acc";

        /*
            tao moi : new
            locked : dang lock tien
            commit : da chuyen tien
            rollback : da hoan tien
            adjusted : tra lai tien cho khach hang
            cancel : da huy
         */

        public static String STATUS = "status"; // trang thai lenh
        public static String INVOICE_NO = "invoice_no"; // so hop dong
        public static String TICKET_CODE = "ticket_code"; // ma ve
        public static String ERROR = "error"; // ma loi
        public static String DESCRIPTION = "description"; // mo ta

        public static String PRICE_BEFORE = "price_before"; // gia ban dau
        public static String PRICE_AFTER = "price_after"; // gia sau
        public static String LIST_PRICE = "list_price"; // chi tiet gia
        public static String DATE_COMFIRM = "date_confirm"; // ngay xac nhan
        public static String DATE_CANCEL = "date_cancel"; // ngay huy

        public static String TABLE = "tracker";

        /*Tên khách hàng
        - Số điện thoại
        - Email (nếu có)
        - Số tiền đã thanh toán
        - Thời gian thanh toán
        - Trạng thái (thanh toán thành công, lỗi...)*/

    }

    // locnguyen - com.mservice.momo.web.internal.webadmin
    public static class UserWebadmin {
        public static String USERNAME = "username";
        public static String PASSWORD = "password";
        public static String SESSIONID = "sessionid";
        public static String ROLL_TYPE = "roll_type";
//        public static String LASTACTIONTIME = "last_act_time";

        public static String TABLE = "userwebadmin";

        // {username: "admin", password: "admin", sessionid: 123456, roll_type: 1}
    }
    // locnguyen

    public static class TranErrorConfig {
        public static String ID = "_id";
        public static String ERROR_CODE = "error_code";
        public static String DESCRIPTION = "description";
        public static String TRAN_TYPE = "tran_type";
        public static String TRAN_NAME = "tran_name";
        public static String CONTENT_TRANHIS = "con_tranhis";
        public static String NOTI_TITLE = "noti_title";
        public static String NOTI_BODY = "noti_body";

        public static String TABLE = "tran_err_config";
    }

    //

    public static class PromotedCols {
        public static String INVITER_NUMBER = "iternum";
        public static String INVITEE_NUMBER = "iteenum";
        public static String INVITEE_NAME = "iteename";
        public static String PROMOTED_TIME = "protime";
        public static String TABLE = "promoted";
    }


    public static class ServiceCols {

        public static String ID = "_id";
        public static String SERVICE_TYPE = "ser_type"; //loai dich vu --> hoa don / dich vu / phim.... : invoice/service
        public static String PARTNER_ID = "part_id"; // ma doi tac vi du : viettel
        public static String SERVICE_ID = "ser_id"; // ma dich vu cua doi tac : vi du : iviettel, aviettel...
        public static String SERVICE_NAME = "ser_name"; // ten dich vu : ADSL Viettel, Dien...
        public static String PARTNER_SITE = "part_site"; // URL cua doi tac
        public static String ICON_URL = "icon_url"; // URL icon de hien thi tren client
        public static String TEXT_POPUP = "text_popup"; // dich vu on/off
        public static String STATUS = "status"; // dich vu on/off
        public static String HAS_CHECK_DEBIT = "has_ch_deb"; // dich vu nay co cho phep check cuoc hay khong : yes/no
        public static String BILL_TYPE = "bill_type"; // dich vu nay co cho phep check cuoc hay khong : yes/no
        public static String BILLERID = "billerid"; // conf link checkbill
        public static String BILLPAY = "billpay"; // conf link checkbill
        public static String IS_PROMO = "is_promo";

        public static String TITLE_DIALOG = "title_dlog";
        public static String LAST_UPDATE = "last_update";

        //new
        public static String ORDER = "ord"; // thu tu xuat hien tren app
        public static String STAR = "star"; //ngoi sao
        public static String TOTAL_FORM = "totfrm"; // chay qua form ke tiep khong
        public static String CAT_NAME = "catname";// thuoc nhom phan loai nao
        public static String CAT_ID = "catid"; // id cua nhom phan loai
        public static String UAT = "uat"; //tren khai tren UAT
        public static String IS_STORE = "isStore"; // phan biet app EU, DGD

        public static String WEB_PAYMENT_URL = "webpaymenturl";
        public static String STATUS_ANDROID = "statusAndroid";
        /*optional string title_dialog      =10;    // title cua dialog khi nhap ma
        optional uint64 last_update       =11;    //thoi gian update sau cung cau dich vu*/

        //sort theo : ngay tao moi nhat thi len dau

        //Them 1 column cho dich vu
        public static String CHECK_TIME_ON_ONE_BILL = "check_time_on_one_bill";
        public static String PAY_TIME_ON_ONE_BILL = "pay_time_on_one_bill";
        public static String NOT_LOAD_EU_SERVICE = "has_cached_bill";

        public static String SUB_CAT = "sub_cat";
        public static String ACTIVE = "ACTIVE";
        public static String TABLE = "service_partner";

    }

    public static class ServiceDetailCols {

        public static String ID = "_id";

        //bat buoc phai co
        public static String SERVICE_ID = "ser_id";  // ma dich vu cua doi tac : vi du : iviettel, aviettel..
        public static String FIELD_LABEL = "fi_la";   // label nam ke input
        public static String FIELD_TYPE = "fi_ty";    // loai field : text/drop/ number
        public static String IS_AMOUNT = "is_am";     // quy dinh field de client boc so tien gui len server
        public static String IS_BILLID = "is_bi";    // quy dinh field de client boc ma hoa don gui len server
        public static String KEY = "key";            // quy dinh key cua field nay
        public static String REQUIRED = "requied";   // quy dinh field nay bat buoc nhap du lieu hay khong
        public static String LAST_TIME = "ltime";    // quy dinh field nay bat buoc nhap du lieu hay khong
        public static String ORDER = "ord";          // sap thu tu cho xuat hien tren app client
        public static String HAS_CHILD = "hchild";   // sap thu tu cho xuat hien tren app client
        public static String LINE = "line";          // so dong cua control nay

        public static String TABLE = "service_detail";

    }

    public static class WhoSystemPauseCols {

        public static String ID = "_id";
        public static String CAPTION = "caption";
        public static String BODY = "body";
        public static String ACTIVED = "actived";
        public static String LAST_CHANGED = "lastchange";
        public static String CHANGED_BY = "changeby";
        public static String TABLE = "systempaused";
    }

    public static class ServicePackage {

        public static String ID = "_id";
        public static String SERVICE_ID = "service_id";
        public static String SERVICE_NAME = "service_name";
        public static String PACKAGE_TYPE = "package_type";
        public static String PACKAGE_NAME = "package_name";
        public static String PACKAGE_VALUE = "package_value";
        public static String DESCRIPTION = "description";
        public static String LINKTODROPBOX = "link2drb";
        public static String LAST_TIME = "ltime";
        public static String PARENT_ID = "parid";
        public static String PARENT_NAME = "parname";
        public static String ORDER = "ord";
        public static String TABLE = "service_package";
    }

    public static class Track123PayNotify {
        public static String ID = "_id";
        public static String MTRANSACTIONID = "mTranId";
        public static String BANK_CODE = "bankCode";
        public static String TRAN_STATUS = "tranStat";
        public static String DESCRIPTION = "desc";
        public static String TIMESTAMP = "ts";
        public static String CHECK_SUM = "chksum";
        public static String PHONE_NUMBER = "phoneNumber";
        public static String AMOUNT = "amt";
        public static String CREATE_TIME = "ctime";
        public static String CREATE_TIME_VN = "ctimeVn";
        public static String ERROR_DESC = "errorDesc";
        public static String TABLE = "pay123notify";

    }

    public static class PromoTrackCols {
        public static String PROMO_CODE = "promocode";
        public static String NUMBER = "number";
        public static String TIME = "time";
        public static String TIME_VN = "timevn";
        public static String STATUS = "status";
        public static String PARTNER = "partner";
        public static String ERROR = "error";
        public static String DESCRIPTION = "description";
        public static String EXEC_NUMBER = "enumber";
        public static String EXEC_TIME = "etime";
        public static String EXEC_TIME_LAST = "l_etime";
        public static String TABLE = "promotrack_v2";
    }

    public static class Phim123PromoCols {
        public static String ID = "_id"; // so dien thaoi
        public static String INVOICE_NO = "invno"; // ma hoa don cua 123phim tra cho minh
        public static String TICKET_CODE = "tkcode"; // ma ve 123phim tra cho minh
        public static String FILM_NAME = "fname"; // ten phim
        public static String RAP = "rap"; // rap
        public static String DISPLAY_TIME = "dispaytime"; //gio chieu
        public static String BUY_TIME = "buytime"; // ngay mua
        public static String SEAT_LIST = "seatlst"; // danh sach ghe
        public static String NUMBER_OF_SEAT = "numofseat"; // so luong ghe
        public static String PROMO_CODE = "code"; // ma khuyen mai
        public static String TIME = "time"; // thoi gian tao record

        public static String PROMO_COUNT = "p_count"; // thoi gian tao record
        public static String PROMO_TIME = "p_time"; // thoi gian tao record

        public static String TABLE = "promo123phim"; // ten collection
    }

    public static class Phim123PromoGlxCols {
        public static String ID = "_id";                // so dien thoai
        public static String INVOICE_NO = "invno";        //ma hoa don cua 123phim tra cho minh
        public static String TICKET_CODE = "tkcode";      //ma ve 123phim tra cho minh
        public static String FILM_NAME = "fname";         //ten phim
        public static String RAP = "rap";                 // rap
        public static String DISPLAY_TIME = "dispaytime"; //gio chieu
        public static String BUY_TIME = "buytime";        // ngay mua
        public static String SEAT_LIST = "seatlst";       // danh sach ghe
        public static String NUMBER_OF_SEAT = "numofseat";// so luong ghe
        public static String PROMO_CODE = "code";         // ma khuyen mai
        public static String TIME = "time";               // thoi gian tao record
        public static String TIMEVN = "timevn";          // thoi gian vn
        public static String PROMO_COUNT = "pcnt";     // thoi gian tao record
        public static String PROMO_TIMEVN = "ptimevn";       // thoi gian tao record
        public static String AMOUNT = "amt";             // gia tri mua ve
        public static String UPDATE_BY = "uby";     // user tra combo
        public static String UPDATE_TIME = "utime"; // thoi gian tra combo
        public static String STATUS = "stat";           // trang thai combo
        public static String SUBRAP = "subrap";           // rap con
        public static String DESC = "desc";              // mo ta loi
        public static String TABLE = "glx"; // ten collection
    }

    public static class TrackGenCodeCols {

        public static String ITEE_NUMBER = "itee_num";
        public static String ITEE_CODE = "itee_code";
        public static String ITEE_DESC = "itee_desc";
        public static String ITEE_REGISTER_DATE = "itee_reg_date";
        public static String ITEE_PROCOUNT = "itee_procnt";

        public static String ITER_NUMBER = "iter_num";
        public static String ITER_CODE = "iter_code";
        public static String ITER_DESC = "iter_desc";
        public static String ITER_REGISTER_DATE = "iter_reg_date";
        public static String ITER_PROCOUNT = "iter_procnt";

        public static String TIME = "time";
        public static String AMOUNT = "amount";
        public static String TRAN_TYPE = "ttype";

        public static String TABLE = "trackgencode";
    }

    public static class RegImeiCols {
        public static String id = "_id";  //icon loai giao dich vu co su dung voucher
        public static String count = "cnt";     //ten cua voucher
        public static String time = "time";    //bat tat dich vu su dung vourcher
        public static String listPhone = "lstPhone"; // danh sach cac so phone khi bat OTP
        public static String table = "regimei";//ten bang chua danh sach cac dich vu co su dung vc
    }

    public static class ClaimPointCols {
        public static String CLAIM_CODE = "code";
        public static String NUMBER = "number";
        public static String TIME = "time";
        public static String TIME_VN = "timevn";
        public static String STATUS = "status";
        public static String PARTNER = "partner";
        public static String ERROR = "error";
        public static String DESCRIPTION = "desc";
        public static String EXEC_NUMBER = "enumber";
        public static String EXEC_TIME = "etime";
        public static String TABLE = "claimtrack";
    }

    public static class TopUpSumCols {
        public static String number = "_id";             //icon loai giao dich vu co su dung voucher
        public static String total_amount = "ttamt";    //ten cua voucher
        public static String time = "time";              //bat tat dich vu su dung vourcher
        public static String time_vn = "timevn";         //
        public static String table = "topupsum";         //ten bang chua danh sach cac dich vu co su dung vc
    }

    public static class CDHHCols {
        public static String id = "_id";
        public static String number = "number";
        public static String name = "name";
        public static String value = "value";
        public static String voteAmount = "vamount";
        public static String time_vn = "timevn";
        public static String day_vn = "dayvn";
        public static String time = "time";
        public static String code = "code";
        public static String tranid = "tid";
        public static String serviceid = "serviceid";
        public static String table = "cdhh";
    }

    public static class BNHVCols {
        public static String number = "number";
        public static String name = "name";
        public static String value = "value";
        public static String voteAmount = "vamount";
        public static String time_vn = "timevn";
        public static String day_vn = "dayvn";
        public static String time = "time";
        public static String code = "code";
        public static String tranid = "tid";
        public static String serviceid = "serviceid";
        public static String table = "bnhv1_15";
    }

    public static class CDHHSumCols {
        public static String id = "_id";
        public static String value = "value";
        public static String table = "cdhhTotal";
    }

    public static class CDHHErrCols {
        public static String number = "number";
        public static String voteAmount = "vamount";
        public static String time_vn = "timevn";
        public static String time = "time";
        public static String code = "code";
        public static String error = "error";
        public static String desc = "desc";
        public static String table = "cdhherr";
    }

    public static class CDHHPayBackCols {
        public static String number = "_id";
        public static String voteAmount = "vamount";
        public static String serviceId = "serviceid";
        public static String table = "cdhhpayback";
    }

    public static class CDHHPayBackSettingCols {
        public static String id = "_id";
        public static String status = "stat";
        public static String delaytime = "dtime";
        public static String paybackaccount = "pbacc";
        public static String paybbackmax = "pbmax";
        public static String serviceid = "serviceid";
        public static String table = "cdhhpaybacksetting";
    }

    public static class FSCSettingCols {
        public static String id = "_id";
        public static String maxcode = "mcode";
        public static String totalcode = "tcode";
        public static String usedcode = "ucode";
        public static String recieveaccount = "racc";
        public static String maxticket = "mticket";
        public static String fromdate = "fdate";
        public static String todate = "tdate";
        public static String table = "fscsetting";
    }

    public static class FSCRecCols {
        public static String code = "_id";
        public static String number = "num";
        public static String name = "name";
        public static String time = "time";
        public static String timevn = "timevn";
        public static String mssv = "mssv";
        public static String status = "stat";
        public static String table = "fscrec";
    }

    public static class ServiceCatCols {
        public static String id = "_id";
        public static String name = "name";
        public static String desc = "desc";
        public static String status = "stat";
        public static String lasttime = "ltime";
        public static String order = "ord";
        public static String iconurl = "iurl";
        public static String star = "star";

        public static String table = "srvcat";
    }

    public static class ServiceFormCols {
        public static String id = "_id";
        public static String formnumber = "frmnum";
        public static String name = "name";
        public static String serviceid = "sid";
        public static String servicename = "sname";
        public static String caption = "cap";
        public static String textbutton = "txtbtn";
        public static String desc = "desc";
        public static String lasttime = "lasttime";
        public static String guide = "guide";

        public static String table = "svfrm";
    }

    public static class VcbCols {
        public static String id = "_id"; // --> phone number
        public static String number = "number";
        public static String bankcode = "bnkcode";
        public static String time = "time";
        public static String timevn = "timevn";
        public static String voucherid = "vcid";
        public static String partner = "partner";
        public static String card_id = "cardid";
        public static String tranid = "tranid";
        public static String desc = "desc";
        public static String table = "vcbtb";
    }

    public static class VcbCmndCols {
        public static String cardid = "_id"; // --> phone number
        public static String bankcode = "bnkcode";
        public static String timevn = "timevn";
        public static String number = "number";
        public static String promocount = "procnt";
        public static String table = "vcbcmndtb";
    }

    //bao.start

    public static class InfoAlertTypeCols {
        public static String id = "_id";
        public static String desc = "desc";
        public static String status = "status";
        public static String last_time = "lastTime";

        public static String table = "InfoAlertType";
    }

    public static class InfoAlertCols {
        public static String id = "_id";
        public static String phone = "phone";
        public static String last_time = "lastTime";
        public static String alert_id = "alertId";
        public static String type = "type";
        public static String desc = "desc";
        public static String image = "image";
        public static String os = "os";
        public static String status = "status";
        public static String confirm_by = "confirmBy";
        public static String confirm_time = "confirmTime";

        public static String table = "InfoAlert";
    }

    //bao.end

    public static class PgGalaxyHisCols {
        public static String pgcode = "pgcode";
        public static String number = "number";
        public static String value = "val";
        public static String time = "time";
        public static String tranname = "tranname";
        public static String timevn = "timevn";
        public static String serviceid = "sid";
        public static String table = "pghis";
    }

    public static class MntCols {
        public static String code = "_id";
        public static String shared = "share";
        public static String timevn = "timevn";
        public static String agentnumber = "anum";
        public static String tranid = "tranid";
        public static String checkfone = "checkfone";
        public static String amount = "amt";
        public static String time = "time";
        public static String table = "mnt";

    }

    public static class RetailerOtpClient {
        public static String id = "_id";
        public static String retailer = "retailer";
        public static String customer_number = "cusnum";
        public static String opt = "otp";
        public static String timevn = "timevn";
        public static String time = "time";
        public static String table = "otpclient";
    }

    public static class BillInfo {
        public static String id = "_id"; // id cua dong nay
        public static String number = "number"; // vi check
        public static String billId = "bId"; // ma hoa dong
        public static String providerId = "pId"; // nha cung cap dich vu
        public static String amount = "amt"; // tong tin thanh toan

        public static String bills = "bills"; //chi tiet hoa don
        public static String customerName = "cusName"; // ten khach hang
        public static String customerAddress = "cusAddr"; // dia chi khach hang
        public static String customerPhone = "cusFone"; // dien thoai khach hang
        public static String servicePackage = "svcPkg"; // goi dich vu khach hang dang su dung

        public static String checkedTime = "chktime"; // thoi gian check cuoc thanh cong
        public static String checkedTimeVn = "chkTimeVn"; // thoi gian check cuoc thanh cong vn
        public static String checked = "chked"; // da check hoac chua check
        public static String forceCheck = "forceChk"; // force or not check
        public static String table = "billinf";
    }

    public static class CardType {
        public static String id = "_id";
        public static String partnerCode = "pcode";
        public static String cardType = "cardType";
        public static String desc = "desc";
        public static String lastTime = "ltime";
        public static String enable = "enable";
        public static String iconUrl = "iconUrl";
        public static String table = "cardtype";
    }

    public static class CustomCarePromo {

        //update data
        public static String number = "_id"; // wallet will be get voucher
        public static String enable = "enable"; // force cho phep tra thuong khong
        public static String group = "group";  // ma nhom khuyen mai, 1,2.....
        public static String groupDesc = "grDesc"; // mo ta nhom nhan khuyen mai
        public static String upTimeVn = "upTimeVn"; // thoi gian upload file len he thong

        //promoted
        public static String promoCount = "pcnt"; // so luong da khuyen mai
        public static String proTimeVn = "proTimeVn"; // thoi gian VN tra khuyen mai

        //promtion program

        //format yyyy-MM-dd hh:mm:ss
        public static String dateFrom = "dFrom"; // chuong trinh khuyen mai bat dau tu ngay
        public static String orgDateFrom = "orgDFrom";

        //format yyyy-MM-dd hh:mm:ss
        public static String dateTo = "dTo"; // chuong trinh khuyen mai keo dai den ngay
        public static String orgDateTo = "orgDto";

        public static String duration = "dur"; // thoi gian duoc cong them ke tu khi tao qua
        public static String promoValue = "proVal"; //gia tri khuyen mai
        public static String momoAgent = "mmAgent"; //tai khoan dung tra khuyen mai
        public static String giftTypeId = "gTypeId"; // loai voucher quy dinh cac dich vu duoc phep su dung
        public static String tranId = "tid";
        public static String nameCustomer = "nameCustomer";
        public static String transCount = "transCount";
        public static String table = "customCarePromo";


    }

    //BEGIN 0000000004
    public static class BillPayPromo {

        //update data
        public static String NUMBER = "_id"; // wallet will be get voucher

        public static String GROUP = "group";  // ma nhom khuyen mai, billpay, vcb, gio dong.....
        //promoted
        public static String PROMO_COUNT = "pcnt"; // so luong da khuyen mai

        //promoted time
        public static String PROMO_TIME_1 = "promoTime_1";
        public static String PROMO_TIME_2 = "promoTime_2";
        public static String PROMO_TIME_3 = "promoTime_3";
        public static String PROMO_TIME_4 = "promoTime_4";

        //promtion program
        public static String HAS_MPOINT = "hasMPoint";
        public static String MPOINT_VALUE = "mPointValue";
        public static String SERVICE_ID_POINT = "serviceIdPoint";

//
//        public static String momoAgent = "mmAgent"; //tai khoan dung tra khuyen mai

        public static String GIFT_TYPE_ID_1 = "gTypeId_1"; // loai voucher quy dinh cac dich vu duoc phep su dung
        public static String GIFT_TYPE_ID_2 = "gTypeId_2";
        public static String GIFT_TYPE_ID_3 = "gTypeId_3";
        public static String GIFT_TYPE_ID_4 = "gTypeId_4";     // tien khuyen mai da duoc nhan tu vcb.

        public static String SERVICE_ID_1 = "serviceid_1";
        public static String SERVICE_ID_2 = "serviceid_2";

        public static String TRAN_ID_1 = "tid_1";
        public static String TRAN_ID_2 = "tid_2";
        public static String TRAN_ID_3 = "tid_3";
        public static String TRAN_ID_4 = "tid_4";

        public static String TRAN_TYPE_1 = "tranType1";
        public static String TRAN_TYPE_2 = "tranType2";
        public static String TRAN_TYPE_3 = "tranType3";
        public static String TRAN_TYPE_4 = "tranType4";

        public static String GIFT_ID_1 = "giftId1";
        public static String GIFT_ID_2 = "giftId2";
        public static String GIFT_ID_3 = "giftId3";
        public static String GIFT_ID_4 = "giftId4";


        public static String ACTIVATED_TIME_2 = "activatedTime2";

        public static String ACTIVATED_TIME_4 = "activatedTime4";

        public static String CARD_CHECK_SUM_VISA = "cardCheckSumVisa";

        public static String CMND = "cmnd";

        public static String table = "billpayPromo";
    }
    //END 0000000004

    //BEGIN 0000000008 Lap table de quan ly nhung tai khoang nao khong duoc tra thuong

    public static class BillPayPromoError {

        public static String ID = "_id";
        public static String NUMBER = "number";
        public static String GROUP = "group";
        public static String SERVICE_ID = "servId";
        //        public static String SERVICE_ID_2 = "servId2";
        public static String ERROR_CODE = "errorcode";
        public static String ERROR_DESC = "errordesc";
        public static String TIME_ERROR = "time";
        //        public static String PROMO_COUNT = "procount"; //0: chua tra, 1: tra roi.
        public static String TABLE = "BillPayPromoError";


    }
    //END 0000000008 Lap table de quan ly nhung tai khoang nao khong duoc tra thuong

    //BEGIN 0000000015: visamasterMPointPromo
    public static class VisaMPointPromo {
        public static String ID = "_id";
        public static String NUMBER = "number";
        public static String CARD_NUMBER = "card_number";
        public static String MPOINT_1 = "mpoint_1";
        public static String MPOINT_2 = "mpoint_2";
        public static String PROMO_COUNT = "promo_count";
        public static String TIME_1 = "time_1";
        public static String TIME_2 = "time_2";
        public static String END_MONTH = "end_month";
        public static String TID_1 = "tid_1";
        public static String TID_2 = "tid_2";
        public static String TRANTYPE_1 = "trantype_1";
        public static String TRANTYPE_2 = "trantype_2";
        public static String TID_VISA_1 = "tid_visa_1";
        public static String TID_VISA_2 = "tid_visa_2";

        public static String SERVICE_ID_1 = "serviceid_1";
        public static String SERVICE_ID_2 = "serviceid_2";

        public static String TOTAL_AMOUNT_1 = "totalamount_1";
        public static String TOTAL_AMOUNT_2 = "totalamount_2";

        public static String CASH_IN_AMOUNT_1 = "cashin_amount_1";
        public static String CASH_IN_AMOUNT_2 = "cashin_amount_2";

        public static String CASH_IN_TIME_1 = "cashin_time_1";
        public static String CASH_IN_TIME_2 = "cashin_time_2";

        public static String TABLE = "VisaMPointPromo";


    }

    //END 0000000015: visamasterMPointPromo

    //BEGIN 0000000016
    public static class VMCardIdCardNumber {
        public static String CARDNUMBER = "_id";
        public static String PHONE = "phone";
        public static String CARDID = "cardid";
        public static String LASTTIME = "lasttime";
        public static String LASTTRANID = "lasttranid";
        public static String TABLE = "VMCardIdCardNumber";

    }

    public static class eventContent {
        public static String ID = "_id";
        public static String CAP = "cap";
        public static String BODY = "body";
        public static String TABLE = "eventContent";
    }

    public static class VisaMpointError {
        public static String ID = "_id";
        public static String NUMBER = "number";
        public static String ERROR = "error";
        public static String DESC_ERROR = "description";
        public static String TIME = "time";
        public static String TRANID = "tranid";
        public static String TRANTYPE = "trantype";
        public static String CARDNUMBER = "cardnumber";
        public static String COUNT = "count";
        public static String TABLE = "VisaMpointError";
    }

    public static class GroupManage {
        public static String NUMBER = "number";
        public static String GROUP_ID = "group_id";
        public static String TABLE = "GroupManageDb";
    }

    public static class HC_PRU_VoucherManage {
        public static String BILL_ID = "_id";
        public static String NUMBER = "number";
        public static String GIFT_TYPE_ID = "giftType";
        public static String TIME = "time";
        public static String SERVICE_ID = "serviceid";
        public static String GIFT_ID = "giftId";
        public static String TABLE = "HC_PRU_VoucherManage";

    }

    public static class BillRuleManage {
        public static String BILL_ID = "_id";

        public static String START_TIME = "start_time";

        public static String END_TIME = "end_time";

        public static String SERVICE_ID = "service_id";
        public static String AMOUNT = "amount";

        public static String PHONE_NUMBER = "phone_number";
        public static String TRAN_ID = "tid";
        public static String TRAN_TYPE = "tranType";
        public static String COUNT = "count";
        public static String TABLE = "BillRuleManageDB";
    }

    public static class CacheBillPay {
        public static String BILL_ID = "_id";
//        public static String PROXY_RESPONSE_CODE = "proxy_response_code";
//        public static String PROXY_RESPONSE_MESSAGE = "proxy_response_message";
//        public static String TOTAL_AMOUNT = "total_amount";
//        public static String PARENT_TRANS_ID = "parent_trans_id";
//        public static String CUSTOMER_ID = "customer_id";
//        public static String CUSTOMER_NAME = "customer_name";
//        public static String CUSTOMER_INFO = "customer_info";
//        public static String CUSTOMER_PHONE = "customer_phone";
//        public static String CUSTOMER_ADDRESS = "customer_address";
//        public static String CODE_ITEM = "code_item";
//        public static String HTML_CONTENT = "html_content";
//        public static String SERVICE_CODE = "service_code";
//        public static String BILL_LIST      = "bill_list";


        public static String TOTAL_AMOUNT = "total_amount";
        public static String CUSTOMER_INFO = "customer_info";
        public static String ARRAY_PRICE = "array_price";
        public static String EXTRA_INFO = "extra_info";


        public static String COUNT = "count";
        public static String CHECK_TIME = "check_time";
        public static String CHECK_TIME_AGAIN = "check_time_again";
        public static String SERVICE_ID = "service_id";
        public static String TABLE = "CacheBillPayDb";

    }

    public static class CacheBillInfoViaCore {
        public static String BILL_ID = "_id";
        public static String BILL_INFO = "billInfo";
        public static String RCODE = "rcode";
        public static String CHECKED_TIME = "checked_time";
        public static String END_CHECKED_TIME = "end_checked_time";
        public static String PROVIDER_ID = "provider_id";
        public static String NUMBER = "phone_number";
        public static String COUNT = "count";
        public static String JSON_RESULT = "json_result";
        public static String TABLE = "CacheBillInfoViaCoreDb";

    }

    public static class ControlOnClickActivity {
        public static String KEY = "_id";
        public static String NUMBER = "number";
        public static String SERVICE = "service";
        public static String PROGRAM = "program";
        public static String TABLE = "ControlOnClickActivityDb";
    }

    public static class RegisterInfoCol {
        public static String PHONE = "phone";
        public static String NAME = "name";
        public static String CARD_ID = "cardId";
        public static String ADDRESS = "address";
        public static String EMAIL = "email";
        public static String DATE_OF_BIRTH = "dateofbirth";
        public static String IS_NAME = "isNamed";
        public static String REGISTER_INFO = "registerInfo";
    }

    public static class MappingWalletBank {
        public static String ID = "_id";
        public static String NUMBER = "number";
        public static String BANK_CODE = "bank_code";
        public static String MAPPING_TIME = "mapping_time";
        public static String UNMAPPING_TIME = "unmapping_time";
        public static String BANK_NAME = "bank_name";
        public static String CUSTOMER_NAME = "customer_name";
        public static String CUSTOMER_ID = "cutomer_id";
        public static String NUMBER_OF_MAPPING = "number_of_mapping";
        public static String TABLE = "mapping_wallet_bank";
    }

    public static class MappingWalletOCBPromo {
        public static String CUSTOMER_ID = "_id";
        public static String NUMBER = "number";
        public static String BANK_CODE = "bank_code";
        public static String MAPPING_TIME = "mapping_time";
        public static String BANK_NAME = "bank_name";
        public static String CUSTOMER_NAME = "customer_name";
        public static String TABLE = "MappingWalletOCBPromoDb";
    }

    public static class IronManNewRegisterTracking {
        public static String PHONE_NUMBER = "_id";
        public static String BANK_CODE = "bank_code";
        public static String CMND = "cmnd";
        public static String VISA_CARD_NUMBER = "visa_card_number";
        public static String TIME_REGISTRY = "time_registry";
        public static String HAS_BONUS = "has_bonus";
        public static String PROGRAM_NUMBER = "program_number";
        public static String TABLE = "IronManNewRegisterTracking";
    }

    public static class IronManBonusTrackingTable{
        public static String PROGRAM = "program";
        public static String PROGRAM_NUMBER = "_id";
        public static String NUMERATOR = "numerator"; // Tu so
        public static String DENOMINATOR = "denominator"; // Mau so
        public static String START_TIME = "start_time";
        public static String END_TIME = "end_time";
        public static String NUMBER_OF_BONUS_MAN = "number_of_bonus_man";
        public static String NUMBER_OF_NEW_COMER = "number_of_new_comer";
        public static String NOT_RATIO_FLAG = "not_ratio_flag";
        public static String MIN_RATIO = "min_ratio";
        public static String MAX_RATIO = "max_ratio";
        public static String NUMBER_OF_BONUS_GAVE_MAN = "number_of_bonus_gave_man";
        public static String TABLE = "IronManBonusTrackingTable";
    }

    public static class IronManPromoGift{
        public static String PHONE_NUMBER = "_id";
        public static String CMND = "cmnd";
        public static String VISA_CARD = "visa_card";

        public static String PROMO_COUNT = "promo_count";

        public static String HAS_VOUCHER_GROUP_1 = "has_voucher_group_1";
        public static String HAS_VOUCHER_GROUP_2 = "has_voucher_group_2";
        public static String HAS_VOUCHER_GROUP_3 = "has_voucher_group_3";
        public static String HAS_VOUCHER_GROUP_4 = "has_voucher_group_4";

        public static String START_TIME_GROUP_1 = "start_time_group_1";
        public static String START_TIME_GROUP_2 = "start_time_group_2";
        public static String START_TIME_GROUP_3 = "start_time_group_3";
        public static String START_TIME_GROUP_4 = "start_time_group_4";

        public static String END_TIME_GROUP_1 = "end_time_group_1";
        public static String END_TIME_GROUP_2 = "end_time_group_2";
        public static String END_TIME_GROUP_3 = "end_time_group_3";
        public static String END_TIME_GROUP_4 = "end_time_group_4";

        //GROUP 1
        public static String SERVICE_V1 = "service_v1";

        //GROUP 2
        public static String SERVICE_V2 = "service_v2";
        public static String SERVICE_V3 = "service_v3";
        public static String SERVICE_V4 = "service_v4";
        public static String SERVICE_V5 = "service_v5";

        //GROUP 3
        public static String SERVICE_V6 = "service_v6";
        public static String SERVICE_V7 = "service_v7";
        public static String SERVICE_V8 = "service_v8";
        public static String SERVICE_V9 = "service_v9";
        public static String SERVICE_V10 = "service_v10";
        public static String SERVICE_V11 = "service_v11";

        //GROUP 4
        public static String SERVICE_V12 = "service_v12";

        //USED_VOUCHER
        public static String USED_VOUCHER_1 = "used_voucher_1";

        public static String USED_VOUCHER_2 = "used_voucher_2";
        public static String USED_VOUCHER_3 = "used_voucher_3";
        public static String USED_VOUCHER_4 = "used_voucher_4";
        public static String USED_VOUCHER_5 = "used_voucher_5";

        public static String USED_VOUCHER_6 = "used_voucher_6";
        public static String USED_VOUCHER_7 = "used_voucher_7";
        public static String USED_VOUCHER_8 = "used_voucher_8";
        public static String USED_VOUCHER_9 = "used_voucher_9";
        public static String USED_VOUCHER_10 = "used_voucher_10";
        public static String USED_VOUCHER_11 = "used_voucher_11";
        //GROUP 4
        public static String USED_VOUCHER_12 = "used_voucher_12";

        public static String GIFT_ID_1 = "gift_id_1";
        public static String GIFT_ID_2 = "gift_id_2";
        public static String GIFT_ID_3 = "gift_id_3";
        public static String GIFT_ID_4 = "gift_id_4";
        public static String GIFT_ID_5 = "gift_id_5";
        public static String GIFT_ID_6 = "gift_id_6";
        public static String GIFT_ID_7 = "gift_id_7";
        public static String GIFT_ID_8 = "gift_id_8";
        public static String GIFT_ID_9 = "gift_id_9";
        public static String GIFT_ID_10 = "gift_id_10";
        public static String GIFT_ID_11 = "gift_id_11";

        public static String NUMBER_OF_NOTI_1 = "number_of_noti_1";
        public static String NUMBER_OF_NOTI_2 = "number_of_noti_2";
        public static String NUMBER_OF_NOTI_3 = "number_of_noti_3";
        public static String NUMBER_OF_NOTI_4 = "number_of_noti_4";

        public static String TABLE = "IronManPromoGiftDB";

    }

    public static class IronManRandomGiftManage{

        public static String GROUP = "_id";

        public static String FIXED_GIFT = "fixed_gift";

        public static String RANDOM_GIFT = "random_gift";

        public static String NUMBER_OF_GIFT = "number_of_gift";

        public static String TABLE = "IronManRandomGiftManageDb";
    }


    public static class PreIronManPromotionManage{
        public static String PHONE_NUMBER = "_id";
        public static String GIFT_ID = "gift_id";
        public static String START_TIME = "start_time";
        public static String END_TIME = "end_time";
        public static String IS_USED = "is_used";
        public static String IS_NEW_REGISTER = "is_new_register";
        public static String IS_LOGIN_USER = "is_login_user";
        public static String TABLE = "PreIronManPromotionManageDb";
    }

    public static class CountPreIronManManage{
        public static String GROUP = "group";
        public static String TOTAL = "total";
        public static String TABLE = "CountPreIronManManageDb";
    }


    public static class AndroidDataUser{
        public static String ID = "_id";
        public static String PHONE_NUMBER = "phone_number";
        public static String TABLE = "AndroidDataUserDb";
    }

    public static class DeviceDataUser{
        public static String ID = "_id";
        public static String PHONE_NUMBER = "phone_number";
        public static String TABLE = "DeviceDataUserDb";
    }

    public static class ServiceFeeDBCols {
        public static String ID = "_id";
        public static String SERVICE_ID = "serviceid";
        public static String CHANNEL = "channel"; // banknet/smartlink/onepay
        public static String TRANTYPE = "trantype"; // bieu phi ap dung cho loai giao dich nao
        public static String DYNAMIC_FEE = "dynamicfee"; // phi dong
        public static String STATIC_FEE = "staticfee"; // phi tinh
        public static String INOUT_CITY = "iocity"; // trong ngoai thanh pho
        public static String SERVICE_NAME = "servicename";
        public static String FEE_TYPE = "feetype";
        public static String MIN_VALUE = "minval";
        public static String MAX_VALUE = "maxval";
        public static String TABLE = "ServiceFeeDb"; // phi tinh
    }

    public static class MerchantKeyManageCols
    {
        public static String MERCHANT_ID = "_id";
        public static String MERCHANT_NAME = "merchant_name";
        public static String MERCHANT_NUMBER = "merchant_number";
        public static String DEV_PUBLIC_KEY = "dev_public_key";
        public static String DEV_PRIVATE_KEY = "dev_private_key";
        public static String PRODUCT_PUBLIC_KEY = "pro_public_key";
        public static String PRODUCT_PRIVATE_KEY = "pro_private_key";
        public static String MERCHANT_INFOS = "merchant_infos";
        public static String SERVICE_ID = "service_id";
        public static String AGENT_NAME = "agent_name";
        public static String BUSNAME = "busname";
        public static String SERVICE_TYPE = "service_type";
        public static String TABLE = "MerchantKeyManageDb";
    }

    public static class OctoberPromoUserManageCols{
        public static String PHONE_NUMBER = "_id";
        public static String TIME = "time";
        public static String IS_LUCKY_MAN = "is_lucky_man";
        public static String TABLE = "OctoberPromoUserManageDb";
    }

    public static class OctoberPromoManageCols
    {
        public static String ID = "_id";
        public static String PHONE_NUMBER = "phone_number";
        public static String CMND = "cmnd";
        public static String CARD_CHECK_SUM = "card_check_sum";
        public static String PROMO_COUNT = "promo_count";
        public static String TRAN_TYPE = "tran_type";
        public static String TRAN_ID = "tran_id";
        public static String IS_LUCKY_MAN = "is_lucky_man";
        public static String NUMBER_OF_NOTI = "number_of_noti";

        public static String GIFT_ID_1 = "gift_id_1";
        public static String GIFT_ID_2 = "gift_id_2";
        public static String GIFT_ID_3 = "gift_id_3";
        public static String GIFT_ID_4 = "gift_id_4";
        public static String GIFT_ID_5 = "gift_id_5";
        public static String GIFT_ID_6 = "gift_id_6";
        public static String GIFT_ID_7 = "gift_id_7";
        public static String GIFT_ID_8 = "gift_id_8";
        public static String GIFT_ID_9 = "gift_id_9";
        public static String GIFT_ID_10 = "gift_id_10";

        public static String START_TIME_GIFT_RECEIVED = "start_time_gift_received";
        public static String END_TIME_GIFT_RECEIVED = "end_time_gift_received";
        public static String NEXT_TIME_TO_RECEIVE_GIFT = "next_time_to_receive_gift";
        public static String AUTO_TIME_TO_RECEIVE_GIFT = "auto_time_to_receive_gift";

//        public static String SERVICE_ID_1 = "service_id_1";
//        public static String SERVICE_ID_2 = "service_id_2";
//        public static String SERVICE_ID_3 = "service_id_3";
//        public static String SERVICE_ID_4 = "service_id_4";
//        public static String SERVICE_ID_5 = "service_id_5";
//        public static String SERVICE_ID_6 = "service_id_6";
//        public static String SERVICE_ID_7 = "service_id_7";
//        public static String SERVICE_ID_8 = "service_id_8";
//        public static String SERVICE_ID_9 = "service_id_9";
//        public static String SERVICE_ID_10 = "service_id_10";

        public static String USED_GIFT_1 = "used_gift_1";
        public static String USED_GIFT_2 = "used_gift_2";
        public static String USED_GIFT_3 = "used_gift_3";
        public static String USED_GIFT_4 = "used_gift_4";
        public static String USED_GIFT_5 = "used_gift_5";
        public static String USED_GIFT_6 = "used_gift_6";
        public static String USED_GIFT_7 = "used_gift_7";
        public static String USED_GIFT_8 = "used_gift_8";
        public static String USED_GIFT_9 = "used_gift_9";
        public static String USED_GIFT_10 = "used_gift_10";

        public static String STATUS = "status"; //0: con han; 1//het han

        public static String TABLE = "OctoberPromoManageDb";

    }

//    public static class RollBack50PerPromo{
//        public static String PHONE_NUMBER = "_id";
//        public static String PROMO_COUNT = "promo_count";
//
//        //So tien thanh toan 10 lan
//        public static String AMOUNT_1 = "";
//        public static String AMOUNT_2 = "";
//        public static String AMOUNT_3 = "";
//        public static String AMOUNT_4 = "";
//        public static String AMOUNT_5 = "";
//        public static String AMOUNT_6 = "";
//        public static String AMOUNT_7 = "";
//        public static String AMOUNT_8 = "";
//        public static String AMOUNT_9 = "";
//        public static String AMOUNT_10 = "";
//
//        //So tien rut tu bank ve 10 lan
//        public static String BANK_AMOUNT_1 = "";
//        public static String BANK_AMOUNT_2 = "";
//        public static String BANK_AMOUNT_3 = "";
//        public static String BANK_AMOUNT_4 = "";
//        public static String BANK_AMOUNT_5 = "";
//        public static String BANK_AMOUNT_6 = "";
//        public static String BANK_AMOUNT_7 = "";
//        public static String BANK_AMOUNT_8 = "";
//        public static String BANK_AMOUNT_9 = "";
//        public static String BANK_AMOUNT_10 = "";
//
//        //So tien khuyen mai 10 lan
//        public static String BONUS_AMOUNT_1 = "";
//        public static String BONUS_AMOUNT_2 = "";
//        public static String BONUS_AMOUNT_3 = "";
//        public static String BONUS_AMOUNT_4 = "";
//        public static String BONUS_AMOUNT_5 = "";
//        public static String BONUS_AMOUNT_6 = "";
//        public static String BONUS_AMOUNT_7 = "";
//        public static String BONUS_AMOUNT_8 = "";
//        public static String BONUS_AMOUNT_9 = "";
//        public static String BONUS_AMOUNT_10 = "";
//
//        //Thong tin ca nhan
//        public static String CARD_CHECK_SUM = "";
//        public static String CMND = "";
//
//        //Thong tin giao dich
//        public static String TRAN_ID_1 = "";
//        public static String TRAN_ID_2 = "";
//        public static String TRAN_ID_3 = "";
//        public static String TRAN_ID_4 = "";
//        public static String TRAN_ID_5 = "";
//        public static String TRAN_ID_6 = "";
//        public static String TRAN_ID_7 = "";
//        public static String TRAN_ID_8 = "";
//        public static String TRAN_ID_9 = "";
//        public static String TRAN_ID_10 = "";
//
//        //Thong tin giao dich ngan hang
//        public static String BANK_TRAN_ID_1 = "";
//        public static String BANK_TRAN_ID_2 = "";
//        public static String BANK_TRAN_ID_3 = "";
//        public static String BANK_TRAN_ID_4 = "";
//        public static String BANK_TRAN_ID_5 = "";
//        public static String BANK_TRAN_ID_6 = "";
//        public static String BANK_TRAN_ID_7 = "";
//        public static String BANK_TRAN_ID_8 = "";
//        public static String BANK_TRAN_ID_9 = "";
//        public static String BANK_TRAN_ID_10 = "";
//
//        //Thoi gian thuc hien
//        public static String TIME_1 = "";
//        public static String TIME_2 = "";
//        public static String TIME_3 = "";
//        public static String TIME_4 = "";
//        public static String TIME_5 = "";
//        public static String TIME_6 = "";
//        public static String TIME_7 = "";
//        public static String TIME_8 = "";
//        public static String TIME_9 = "";
//        public static String TIME_10 = "";
//
//        //Thong tin dich vu
//        public static String SERVICE_ID_1 = "";
//        public static String SERVICE_ID_2 = "";
//        public static String SERVICE_ID_3 = "";
//        public static String SERVICE_ID_4 = "";
//        public static String SERVICE_ID_5 = "";
//        public static String SERVICE_ID_6 = "";
//        public static String SERVICE_ID_7 = "";
//        public static String SERVICE_ID_8 = "";
//        public static String SERVICE_ID_9 = "";
//        public static String SERVICE_ID_10 = "";
//
//        public static String TABLE = "RollBack50PerPromoDb";
//    }

    public static class RollBack50PerPromo{
        public static String PHONE_NUMBER = "phone_number";
        public static String PROMO_COUNT = "promo_count";

        //So tien thanh toan 10 lan
        public static String AMOUNT = "amount";

        //So tien rut tu bank ve 10 lan
        public static String BANK_AMOUNT = "bank_amount";

        //So tien khuyen mai 10 lan
        public static String BONUS_AMOUNT = "bonus_amount";

        //Thong tin ca nhan
        public static String BANK_CODE = "bank_code";
        public static String CMND = "cmnd";

        //Thong tin giao dich
        public static String TRAN_ID = "tran_id";


        //Thong tin giao dich ngan hang
        public static String BANK_TRAN_ID = "bank_tran_id";

        public static String PROMO_TRAN_ID = "promo_tran_id";
        //Thoi gian thuc hien
        public static String TIME = "time";

        //Thong tin dich vu
        public static String SERVICE_ID = "service_id";


        public static String TABLE = "RollBack50PerPromoDb";
    }

    public static class CashInManage{

        public static String PHONE_NUMBER = "phone_number";

        public static String TRAN_ID = "tran_id";

        public static String TRAN_TYPE = "tran_type";

        public static String BANK_CODE = "bank_code";

        public static String CASH_IN_TIME = "cash_in_time";

        public static String BILL_ID = "bill_id";

        public static String CASH_IN_AMOUNT = "cash_in_amount";

        public static String BILL_AMOUNT = "bill_amount";

        public static String SERVICE_ID = "service_id";
    }

    public static class PushNotificationInfoManager
    {
        public static String PUSH_INFO_ID = "_id";
        public static String NUMBER = "number";
        public static String TIME = "time";
        public static String TABLE = "PushNotificationInfoManagerDb";
    }

    public static class DollarHeartCustomerCareGiftGroup
    {
        public static String NUMBER = "number";
        public static String TIME = "time";
        public static String MONEY_VALUE = "money_value";
        public static String GIFT_ID = "gift_id";
        public static String GIFT_AMOUNT = "gift_amount";
        public static String IS_RECEIVED_NOTIFICATION = "is_received_notification";
        public static String PROGRAM = "program";
        public static String TRAN_ID = "tran_id";
        public static String GROUP = "group";
        public static String DURATION = "duration";
        public static String TABLE = "DollarHeartCustomerCareGiftGroup";
    }

    public static class MisPopup
    {
//        "_id" : ObjectId("5667b40d6bdf49a5fc77fb0d"),
//        "caption" : "Liquidity Forecast",
//                "body" : "test form MIS",
//                "tranId" : NumberLong(1449636877733),
//        "phone" : "01684171757"
        public static String CAPTION = "caption";
        public static String BODY = "body";
        public static String TRAN_ID = "tranId";
        public static String PHONE = "phone";
        public static String TABLE = "MIS_POPUP";

    }

    public static class ConnectorHTTPPostPath{
        public static String SERVICE_ID = "_id";
        public static String PATH = "path";
        public static String HOST = "host";
        public static String PORT = "port";
        public static String VERSION = "version";
        public static String TABLE = "ConnectorHTTPPostPathDb";
    }

    public static class HomeCols {

        public static String ID = "_id";
        public static String SERVICE_TYPE = "ser_type"; //1:tinh nang, 2:dich vu, 3:popup, 4:html, 5:danh muc
        public static String FUNC = "func";//1: tinh nang
        public static String SERVICE_ID = "ser_id";//2:dich vu
        public static String TEXT_POPUP = "text_popup"; //3:popup
        public static String WEB_URL = "web_url";//4:html
        public static String CAT_ID = "catid"; //5:danh muc
        public static String SERVICE_NAME = "ser_name"; // ten dich vu : ADSL Viettel, Dien...
        public static String ICON_URL = "icon_url"; // URL icon de hien thi tren client
        public static String STATUS = "status"; // dich vu on/off
        public static String LAST_UPDATE = "last_update";
        public static String POSITION = "position";//hien tai co 2 vitri: 0:top, 1:bottom
        public static String ORDER = "ord"; // thu tu xuat hien tren group
        public static String IS_STORE = "isStore"; // phan biet app EU, DGD
        public static String BUTTON_TITLE = "button_title"; //cho html
        public static String TABLE = "home_page";

    }

    public static class ReactiveDbCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String GIFT_ID = "gift_id";
        public static String GIFT_TYPE = "gift_type";
        public static String TIME = "time";
        public static String AMOUNT = "amount";
        public static String TRAN_ID = "tranId";
        public static String DURATION = "duration";
    }

    public static class ZaloTetPromotionCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String ZALO_CODE = "zalo_code";
        public static String TIME = "time";
        public static String GIFT_ID_1 = "gift_id_1";
        public static String GIFT_TYPE_1 = "gift_type_1";
        public static String USED_VOUCHER_1 = "used_voucher_1";
        public static String GIFT_ID_2 = "gift_id_2";
        public static String GIFT_TYPE_2 = "gift_type_2";
        public static String USED_VOUCHER_2 = "used_voucher_2";
        public static String HAD_MONEY = "had_money";
        public static String MONEY_STATUS = "money_status";
        public static String END_TIME_MONEY = "end_time_money";
        public static String END_TIME_GIFT = "end_time_gift";
        public static String DEVICE_IMEI = "device_imei";
        public static String HAD_BONUS_7PERCENT = "had_bonus_7percent";
        public static String END_BONUS_7PERCENT_TIME = "end_bonus_7percent_time";
        public static String CASH_IN_TYPE = "cashin_type";
        public static String CASH_IN_TID = "cashin_tid";
        public static String TABLE_ALL = "zalo_group_all";
    }

    public static class ZaloTetCashBackPromotionCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String TRAN_ID = "tranId";
        public static String TRAN_ID_BONUS = "tranIdBonus";
        public static String AMOUNT = "amount";
        public static String AMOUNT_BONUS = "amount_bonus";
        public static String SERVICE_ID = "service_id";
        public static String TIME = "time";
        public static String TABLE = "ZaloTetCashBackPromotionDb";
    }

    public static class ZaloNotiGroupCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String GROUP = "group";
        public static String TABLE=  "ZaloNotiGroupDb";
    }

    public static class LixiManageCol{
        public static String PHONE_NUMBER = "_id";
        public static String AMOUNT = "amount";
        public static String TIME = "time";
        public static String TABLE = "LixiManageDb";
    }

    public static class ConnectorProxyBusNameCol{
        public static String SERVICE_ID = "_id";
        public static String BUS_NAME = "busname";
        public static String BILL_PAY = "billpay";
        public static String SERVICE_TYPE = "service_type";
        public static String SERVICE_TYPE_NAME = "service_type_name";
        public static String CHECK_IS_NAMED = "isNamedChecked";
        public static String GET_INFO_TYPE = "type";
        public static String TABLE = "ConnectorProxyBusNameDb";
    }

    public static class CheckServiceGiftRuleCol{
        public static String SERVICE_ID = "_id";
        public static String MIN_AMOUNT = "min_amount";
        public static String HAS_CHECKED = "has_checked";
        public static String HAS_CHECKED_ALL = "has_checked_all";
        public static String TABLE = "CheckServiceGiftRuleDb";
    }

    public static class ClaimCodePromotionCols{
        public static String PROMOTION_ID = "_id";
        public static String PROMOTION_NAME = "promotion_name";
        public static String PREFIX = "prefix";
        public static String NUMBER_OF_GIFT = "number_of_gift";
        public static String GIFT_LIST = "gift_list"; // format: aaa:10000;bbb:20000
        public static String GIFT_TIME = "gift_time";
        public static String MOMO_MONEY = "momo_money";
        public static String MONEY_TIME = "money_time";
        public static String CHECK_PHONE = "check_phone";
        public static String NOTI_TITLE = "notiTitle";
        public static String NOTI_BODY = "notiBody";
        public static String TRANS_BODY = "transBody";
        public static String PARTNER_NAME = "partnerName";
        public static String SERVICE_ID = "serviceId";
        public static String AGENT = "agent";
        public static String GET_BACK_MONEY = "get_back_money";
        public static String NOTI_ROLLBACK_TITLE = "notiRollbackTitle";
        public static String NOTI_ROLLBACK_BODY = "notiRollbackBody";
        public static String TRANS_ROLLBACK_BODY = "transRollbackBody";
        public static String ACTIVE_PROMO = "active";
        public static String UNCHECK_DEVICE = "uncheckDevice";
        public static String IS_MOMO_PROMOTION = "isMoMoPromotion";
        public static String GROUP = "group";
        public static String TABLE = "ClaimCodePromotionDb";
    }

    public static class ClaimCode_CodeCheckCols{
        public static String CODE = "_id";
        public static String PROGRAM = "program";
        public static String ENABLED = "enabled";
    }

    public static class ClaimCode_PhoneCheckCols{
        public static String PHONE_NUMBER = "_id";
        public static String PROGRAM = "program";
    }

    public static class ClaimCode_AllCheckCols{
        public static String CODE = "_id";
        public static String PHONE = "phone";
        public static String TIME = "time";
        public static String GIFT_ID = "gift_id";
        public static String GIFT_TIME = "gift_time";
        public static String GIFT_TID = "gift_tid";
        public static String GIFT_AMOUNT = "gift_amount";
        public static String MOMO_MONEY_AMOUNT = "money_amount";
        public static String MONEY_STATUS = "money_status";
        public static String MONEY_TIME = "money_time";
        public static String MONEY_TID = "money_tid";
        public static String MONEY_ROLLBACK_TID = "money_rollback_tid";
        public static String TRANS_PAY_BILL_TID = "transPayBillTid";
        public static String DEVICE_IMEI = "device_imei";
        public static String JSON_EXTRA = "extra";
    }

    public static class WomanNationalCols{
        public static String PHONE_NUMBER = "_id";
        public static String GIFT_TID = "gift_tid";
        public static String GIFT_ID = "gift_id";
        public static String CASHIN_TID = "cashin_tid";
        public static String CASHIN_TIME = "cashin_time";
        public static String BANK_CODE = "bank_code";
        public static String CARD_ID = "card_id";
        public static String TABLE = "WomanNationalDb";
    }

    public static class PhoneChecking{
        public static String PHONE_NUMBER = "_id";
        public static String ENABLE = "enable";
        public static String PROGRAM = "program";
        public static String TRANS_PER_DAY = "trans_per_day";
        public static String TRANS_PER_HOUR = "trans_per_hour";
        public static String MIN_BETWEEN_TRANS = "min_between_trans";
        public static String TOTAL_IOS = "total_ios";
        public static String TOTAL_ANDROID = "total_android";
        public static String OFF_TIME_FROM = "offtimefrom";
        public static String OFF_TIME_TO = "offtimeto";
        public static String LOCAL = "local";
    }

    public static class DGD2MillionsPromotionMembersCol{
        public static String STORE_ID = "_id";
        public static String MOMO_PHONE = "momo_phone";
        public static String STORE_NAME = "store_name";
        public static String REGISTER_TIME = "register_time";
        public static String IS_ACTIVED = "is_actived";
        public static String ACTIVED_TIME = "actived_time";
        public static String TID_FEE = "tid_fee";
        public static String END_BONUS_3MONTHS = "end_bonus_3months";
        public static String END_BONUS_2MIL = "end_bonus_2mil";
        public static String ERROR_CODE = "error_code";
        public static String REGISTER_END_TIME = "register_end_time";
        public static String BONUS_END_TIME = "bonus_end_time";
        public static String TABLE = "DGD2MillionsPromotionMembersDb";
    }

    public static class DGD2MillionsPromotionTrackingCol{
        public static String STORE_ID = "store_id";
        public static String MOMO_PHONE = "momo_phone";
        public static String TID_BILLPAY = "tran_id";
        public static String TRAN_TYPE = "tran_type";
        public static String AMOUNT = "amount";
        public static String TID_CASHBACK = "tid_cashback";
        public static String ERROR_CODE = "error_code";
        public static String GROUP = "group";
        public static String SERVICE_ID = "service_id";
        public static String BILL_ID = "bill_id";
        public static String TIME = "time";
        public static String TABLE = "DGD2MillionsPromotionTrackingDb";

    }

    public static class NoTransactionsPhonesCol{
        public static String PHONE_NUMBER = "_id";
        public static String NUMBER_OF_TRANSACTION = "number_of_transaction";
        public static String TABLE = "NoTransactionsPhonesTable";
    }

    public static class ReferralV1CodeInputCol{
        public static String INVITEE_NUMBER = "_id";
        public static String IMEI_CODE = "imei_code";
        public static String INVITER_NUMBER = "inviter_number";
        public static String INPUT_TIME = "input_time";
        public static String MAPPING_TIME = "mapping_time";
        public static String INVITER_CARD_INFO = "inviter_card_info";
        public static String INVITEE_CARD_INFO = "invitee_card_info";
        public static String INVITEE_BANK_CODE = "invitee_bank_code";
        public static String INVITER_BANK_CODE = "inviter_bank_code";

        //Thong tin khi tra thuong
        public static String BONUS_TIME = "bonus_time";
        public static String INVITER_BONUS_TID = "inviter_bonus_tid";
        public static String INVITER_BONUS_AMOUNT = "inviter_bonus_amount";
        public static String INVITEE_BONUS_TID = "invitee_bonus_tid";
        public static String INVITEE_BONUS_AMOUNT = "invitee_bonus_amount";
        public static String IS_MAPPED         = "is_mapped";
        public static String IS_BLOCK          = "is_block";
        public static String COUNT = "count";
        public static String SMS = "sms";
        public static String SMS_MAPPED = "sms_mapped";
        public static String NOTI = "noti_phone";
        public static String NOTI3DAY = "noti_3_day";
        public static String NOTI1DAY = "noti_1_day";
        public static String INVITEE_EXTRA_BONUS = "invitee_extra_bonus";
        public static String INVITER_EXTRA_BONUS = "inviter_extra_bonus";
        public static String LOCK = "lock";
        public static String TABLE = "ReferralV1CodeInputDb";
    }
    public static class ReferralV1TransactionsTrackingCol{
        public static String INVITER_NUMBER = "inviter_number";
        public static String INVITEE_NUMBER = "invitee_number";
        public static String TRAN_AMOUNT = "tran_amount";
        public static String TRAN_ID     = "tran_id";
        public static String BONUS_TIME = "bonus_time";
        public static String BONUS_TID = "bonus_tid";
        public static String BONUS_AMOUNT = "bonus_amount";
        public static String CARD_INFO = "card_info";
        public static String BANK_CODE = "bank_code";
        public static String TABLE = "ReferralV1TransactionsTrackingDb";
    }

    public static class IPosCashLimit{
        public static String PHONE_NUMBER = "_id";
        public static String AMOUNT = "amount";
        public static String TIME = "time";
        public static String TABLE = "IPosCashLimitDb";
    }

    public static class Tracking123PhimCol{
        public static String BILL_ID = "_id";
        public static String PHONE_NUMBER = "phone_number";
        public static String TIME = "time";
        public static String TRAN_ID = "tran_id";
        public static String EXTRA = "extra";
        public static String TABLE = "Tracking123PhimDb";
    }
    public static class TrackingBanknetCol{
        public static String BILL_ID = "billId";
        public static String PHONE_NUMBER = "phone_number";
        public static String TIME = "time";
        public static String TRAN_ID = "tran_id";
        public static String EXTRA = "extra";
        public static String TABLE = "TrackingBanknetDb";
    }

    public static class BanknetTransCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String MERCHANT_TRAN_ID = "merchantTranId";
        public static String TRAN_ID = "tranId";
        public static String BANK_ACC = "bankAcc";
        public static String BANK_NAME = "bankName";
        public static String BANK_ID = "bankId";
        public static String TIME_GET_OTP = "time_get_otp";
        public static String RESULT_GET_OTP = "result_get_otp";
        public static String TIME_VERIFY = "time_verify";
        public static String RESULT_VERIFY = "result_verify";
        public static String EXTRA = "extra";
        public static String TABLE = "BankNetTransDb";
    }
//    public static class Tracking123phimCol{
//        public static String BILL_ID = "_id";
//        public static String PHONE_NUMBER = "phone_number";
//        public static String TIME = "time";
//        public static String TRAN_ID = "tran_id";
//        public static String EXTRA = "extra";
//        public static String TABLE = "Tracking123phimDb";
//    }
    public static class EmailPopupCol{
        public static String PHONE_NUMBER = "_id";
        public static String EMAIL = "email";
        public static String ENABLE = "enable";
        public static String TABLE = "EmailPopupDb";
    }

    public static class CashBackCol{
        public static String PHONE_NUMBER  = "phone_number";
        public static String PROGRAM = "program";
        public static String AMOUNT = "amount";
        public static String TRAN_ID = "tran_id";
        public static String BONUS_AMOUNT = "bonus_amount";
        public static String BONUS_TRAN_ID = "bonus_tran_id";
        public static String RATE = "rate";
        public static String TRAN_TYPE = "tran_type";
        public static String SERVICE_ID = "service_id";
        public static String TIME = "time";
        public static String DEVICE_IMEI = "imei";
        public static String BANK_ACC = "bank_acc";
        public static String CARD_INFO = "card_info";
        public static String ERROR_CODE = "error_code";
    }

    public static class VisaMappingPromotionCol{
        public static String PHONE_NUMBER = "_id";
        public static String PROGRAM = "program";
        public static String CARD_INFO = "card_info";
        public static String BANK_ACC = "bank_acc";
        public static String DEVICE_IMEI = "imei";
        public static String TIME_MAPPING = "time_mapping";
        public static String EXTRA = "extra";
        public static String TABLE = "VisaMappingPromotion";
    }

    public static class ErrorPromotionTrackingCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String PROGRAM      = "program";
        public static String TIME         = "time";
        public static String ERROR_CODE = "error_code";
        public static String DESC       = "desc";
        public static String DEVICE_INFO = "device_info";
    }

    public static class AcquireBinhTanUserPromotionCol{
        public static String PHONE_NUMBER = "_id";
        public static String IMEI = "imei";
        public static String EXTRA_KEY = "extra_key";
        public static String TIME = "time";
        public static String GROUP = "group";
        public static String EXTRA = "extra";
        public static String TID_CASHIN = "tid_cashin";
        public static String AMOUNT_CASHIN = "amount_cashin";
        public static String TIME_CASHIN   = "time_cashin";
        public static String TIME_GROUP_3 = "time_group_3";
        public static String END_GROUP_2 = "end_group_2";
        public static String NEXT_TIME_BONUS = "next_time_bonus";
        public static String NEXT_TIME_ROLLBACK = "next_time_rollback";
        public static String END_GROUP_3 = "end_group_3";
        public static String LOCK_STATUS = "lock_status";
        public static String NUMBER_OF_ECHO = "number_of_echo";
        public static String NUMBER_OF_OTP = "number_of_otp";
        public static String BANK_CARD_ID = "bank_card_id";
        public static String IS_LOCKED = "is_locked";
        public static String HAS_BONUS = "has_bonus";
        public static String PROGRAM_NUMBER = "program_number";
        public static String NOTI_TIMES = "noti_times";
        public static String LOCAL = "local";
        public static String TIME_OF_NOTI_FIRE = "time_of_noti_fire";
        public static String IS_TOPUP = "is_topup";

        public static String NOTI_7_DAY = "noti_7_day";
        public static String NOTI_14_DAY = "noti_14_day";
        public static String NOTI_21_DAY = "noti_21_day";
        public static String NOTI_28_DAY = "noti_28_day";
        public static String NOTI_35_DAY = "noti_35_day";

        public static String TABLE = "AcquireBinhTanUserPromotionDb";
    }

    public static class AcquireBinhTanGroup2PromotionCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String TID_BONUS = "tid_bonus";
        public static String AMOUNT_BONUS = "amount_bonus";
        public static String TIME_BONUS = "time_bonus";
        public static String TIMES = "times";
        public static String IS_USED = "is_used";
        public static String TABLE = "AcquireBinhTanGroup2PromotionDb";
    }

    public static class AcquireBinhTanGroup3PromotionCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String TID_BILLPAY = "tid_billpay";
        public static String AMOUNT_BILLPAY = "amount_billpay";
        public static String TID_CASHIN = "tid_cashin";
        public static String TIME_CASHIN = "time_cashin";
        public static String AMOUNT_CASHIN = "amount_cashin";
        public static String TID_BONUS = "tid_bonus";
        public static String TIME_BONUS = "time_bonus";
        public static String AMOUNT_BONUS = "amount_bonus";
        public static String IS_USED = "is_used";
        public static String GIFT_ID = "gift_id";
        public static String EXTRA_BONUS = "extra_bonus";
        public static String TABLE = "AcquireBinhTanGroup3PromotionDb";
    }

    public static class LottePromoCodeCol{
        public static String ID = "_id";
        public static String PROMO_CODE = "promotionCode";
        public static String OWNER = "owner";
        public static String EVENT_ID = "eventId";
        public static String AMOUNT = "amount";
        public static String START_DATE = "startDate";
        public static String END_DATE = "endDate";
        public static String MODIFY_DATE = "modifyDate";
        public static String STATUS = "status";
        public static String CREATOR = "creator";
        public static String PROMO_TYPE = "promoType";
        public static String TABLE = "LottePromoCodeDb";
    }
//    {
//        "_id" : ObjectId("574e5005cfa44beca61ee60a"),
//            "phone_number" : "01225242888",
//            "group_id" : "65123",
//            "group_name" : "VIC",
//            "discount" : "0.2",
//            "receiver_number" : "0973499621"
//    }
    public static class LottePromoCol{
        public static String PHONE_NUMBER = "_id";
        public static String REGISTER_TIME = "registerTime";
        public static String TRAN_TYPE = "tranType";
        public static String TRAN_ID = "tranId";
        public static String BONUS_TIME = "bonusTime";
        public static String TRAN_AMOUNT = "tranAmount";
        public static String BONUS_AMOUNT = "bonusAmount";
        public static String ERROR = "error";
        public static String USED_GIFT = "usedGift";
        public static String UPDATE_TIME = "updateTime";
        public static String TABLE = "LottePromoDb";
    }
    public static class Vic1PromoCol{

        public static String PHONE_NUMBER = "_id";
        public static String IS_NAMED = "isNamed";
        public static String TRAN_TYPE = "tranType";

        public static String TRAN_ID= "tranId";
        public static String TRAN_TIME= "tranTime";
        public static String BONUS_TIME = "bonusTime";
        public static String BONUS_AMOUNT = "bonusAmount";
        public static String TRAN_AMOUNT = "tranAmount";
        public static String IS_CASHIN = "isCashin";
        public static String ERROR = "error";
        public static String USED_GIFT = "usedGift";
        public static String TABLE = "Vic1PromoDb";
    }
    public static class Vic2PromoCol{

        public static String PHONE_NUMBER = "_id";
        public static String TRAN_TYPE = "tranType";
        public static String IS_NAMED = "isNamed";

        public static String TRAN_ID1= "tranId1";
        public static String BONUS_TIME1= "bonusTime1";
        public static String BONUS_AMOUNT1 = "bonusAmount1";
        public static String TRAN_AMOUNT1 = "tranAmount1";
        public static String IS_CASHIN1 = "isCashin1";
        public static String ERROR1 = "error1";
        public static String USED_GIFT1 = "usedGift1";

        public static String TRAN_ID2 = "tranId2";
        public static String BONUS_TIME2 = "bonusTime2";
        public static String TRAN_AMOUNT2 = "tranAmount2";
        public static String BONUS_AMOUNT2 = "bonusAmount2";
        public static String ERROR2 = "error2";
        public static String USED_GIFT2 = "usedGift2";

        public static String TRAN_ID3 = "tranId3";
        public static String BONUS_TIME3 = "bonusTime3";
        public static String TRAN_AMOUNT3 = "tranAmount3";
        public static String BONUS_AMOUNT3 = "bonusAmount3";
        public static String ERROR3 = "error3";
        public static String USED_GIFT3 = "usedGift3";

        public static String TABLE = "Vic2PromoDb";
    }

    public static class Vic3PromoCol{

        public static String PHONE_NUMBER = "_id";
        public static String TRAN_TYPE = "tranType";
        public static String REGISTER_TIME = "registerTime";
        public static String PROMOTION_CODE = "promotionCode";
        public static String IS_NAMED = "isNamed";

        public static String TRAN_ID1= "tranId1";
        public static String TRAN_TIME1= "tranTime1";
        public static String BONUS_TIME1= "bonusTime1";
        public static String IS_PAY_VOUCHER1 = "isPayVoucher1";
        public static String BONUS_AMOUNT1 = "bonusAmount1";
        public static String IS_CASHIN1 = "isCashin1";
        public static String TRAN_AMOUNT1 = "tranAmount1";
        public static String ERROR1 = "error1";
        public static String USED_GIFT1 = "usedGift1";
        public static String GIFT_TIME1 = "giftTime1";

        public static String IS_PAY_VOUCHER2 = "isPayVoucher2";
        public static String TRAN_ID2 = "tranId2";
        public static String BONUS_TIME2 = "bonusTime2";
        public static String TRAN_AMOUNT2 = "tranAmount2";
        public static String BONUS_AMOUNT2 = "bonusAmount2";
        public static String IS_CASHIN2 = "isCashin2";
        public static String ERROR2 = "error2";
        public static String USED_GIFT2 = "usedGift2";
        public static String GIFT_TIME2 = "giftTime2";

        public static String TRAN_ID3 = "tranId3";
        public static String BONUS_TIME3 = "bonusTime3";
        public static String TRAN_AMOUNT3 = "tranAmount3";
        public static String BONUS_AMOUNT3 = "bonusAmount3";
        public static String IS_PAY_VOUCHER3 = "isPayVoucher3";
        public static String IS_CASHIN3 = "isCashin3";
        public static String ERROR3 = "error3";
        public static String USED_GIFT3 = "usedGift3";
        public static String GIFT_TIME3 = "giftTime3";

        public static String TABLE = "Vic3PromoDb";
    }
    public static class VicPromoCol{

        public static String PHONE_NUMBER = "_id";
        public static String TRAN_TYPE = "tranType";
        public static String REGISTER_TIME = "registerTime";
        public static String PROMOTION_CODE = "promotionCode";
        public static String IS_NAMED = "isNamed";

        public static String SEND_CASHIN_NOTI1 = "sendCashinNoti1";
        public static String SEND_CASHIN_NOTI2 = "sendCashinNoti2";
        public static String SEND_CASHIN_NOTI3 = "sendCashinNoti3";

        public static String GROUP = "group";
        public static String TRAN_ID1= "tranId1";
        public static String TRAN_TIME1= "tranTime1";
        public static String BONUS_TIME1= "bonusTime1";
        public static String IS_PAY_VOUCHER1 = "isPayVoucher1";
        public static String BONUS_AMOUNT1 = "bonusAmount1";
        public static String GIFTID1 = "giftId1";
        public static String IS_CASHIN1 = "isCashin1";
        public static String TRAN_AMOUNT1 = "tranAmount1";
        public static String ERROR1 = "error1";
        public static String USED_GIFT1 = "usedGift1";
        public static String GIFT_TIME1 = "giftTime1";

        public static String IS_PAY_VOUCHER2 = "isPayVoucher2";
        public static String TRAN_ID2 = "tranId2";
        public static String BONUS_TIME2 = "bonusTime2";
        public static String TRAN_AMOUNT2 = "tranAmount2";
        public static String BONUS_AMOUNT2 = "bonusAmount2";
        public static String GIFTID2 = "giftId2";
        public static String IS_CASHIN2 = "isCashin2";
        public static String ERROR2 = "error2";
        public static String USED_GIFT2 = "usedGift2";
        public static String GIFT_TIME2 = "giftTime2";

        public static String TRAN_ID3 = "tranId3";
        public static String BONUS_TIME3 = "bonusTime3";
        public static String TRAN_AMOUNT3 = "tranAmount3";
        public static String BONUS_AMOUNT3 = "bonusAmount3";
        public static String IS_PAY_VOUCHER3 = "isPayVoucher3";
        public static String GIFTID3 = "giftId3";
        public static String IS_CASHIN3 = "isCashin3";
        public static String ERROR3 = "error3";
        public static String USED_GIFT3 = "usedGift3";
        public static String GIFT_TIME3 = "giftTime3";

        public static String TABLE = "VicPromoDb";
    }

    public static class SpecialGroupCol{
        public static String PHONE_NUMBER = "phone_number";
        public static String GROUP_ID = "group_id";
        public static String GROUP_NAME = "group_name";
        public static String DISCOUNT = "discount";
        public static String RECEIVER_NUMBER = "receiver_number";
        public static String TABLE = "SpecialGroup";
    }

    public static class BinhTanDgdTransTracking{
        public static String PHONE_NUMBER = "phone_number";
        public static String TRAN_ID = "tran_id";
        public static String TIME = "time";
        public static String AMOUNT = "amount";
        public static String CUSTOMER_PHONE = "customer_phone";
        public static String CUSTOMER_OS = "customer_os";
        public static String TABLE = "BinhTanDgdTransTrackingDb";
    }

    public static class PromotionCountTracking{
        public static String ID = "_id";
        public static String PHONE_NUMBER = "phone_number";
        public static String PROGRAM = "program";
        public static String COUNT = "count";
        public static String TIME = "time";
        public static String TABLE = "PromotionCountTrackingDb";
    }

    public static class AutoNotiCount {
        public static String PHONE_NUMBER = "phone_number";
        public static String TYPE = "type";
        public static String COUNT = "count";
        public static String CASHIN_TIME = "cashin_time";
        public static String TABLE = "AutoNotiCountDb";
    }

    /**
     * Database luu tru thong tin chuong trinh khuyen mai cua VicTaxi(Hanoi)
     */
    public static class VicTaxiPromo {
        public static String NUMBER = "number";
        public static String VOUCHER_QUANTITY = "voucher_quantity";
        public static String GROUP = "group";
        public static String TABLE = "VicTaxiPromoDb";
    }

    /**
     * Database lưu trữ thông tin khách hàng được nhận voucher của VicTaxi
     */
    public static class VicTaxiCustomerList {
        public static String NUMBER = "number";
        public static String IN_PREPARE_SEND_LIST = "in_prepare_send_list";
        public static String SEND_TIME = "send_time";
        public static String IS_RECEIVED = "is_received";
        public static String RECEIVED_TIME = "received_time";
        public static String TABLE = "VicTaxiCustomerListDb";
    }

    public static  class FeCreditPromotionCols {
        public static String PROMOTION_NAME = "promotion_name";
        public static String PHONE_NUMBER = "phone_number";
        public static String DIV_NO = "div_no";
        public static String PREFIX = "prefix";
        public static String NUMBER_OF_GIFT = "number_of_gift";
        public static String GIFT_LIST = "gift_list"; // format: aaa:10000;bbb:20000
        public static String GIFT_TIME = "gift_time";
        public static String MOMO_MONEY = "momo_money";
        public static String MONEY_TIME = "money_time";
        public static String NOTI_TITLE = "notiTitle";
        public static String NOTI_BODY = "notiBody";
        public static String TRANS_BODY = "transBody";
        public static String PARTNER_NAME = "partnerName";
        public static String SERVICE_ID = "serviceId";
        public static String AGENT = "agent";
        public static String GET_BACK_MONEY = "get_back_money";
        public static String NOTI_ROLLBACK_TITLE = "notiRollbackTitle";
        public static String NOTI_ROLLBACK_BODY = "notiRollbackBody";
        public static String TRANS_ROLLBACK_BODY = "transRollbackBody";
        public static String ACTIVE_PROMO = "active";
        public static String GROUP = "group";
        public static String TABLE = "FeCreditPromotionDb";
    }
    public static class ErrorCodeMgtCols{
        public static String ERROR_CODE = "error_code";
        public static String DESCRIPTION = "description";
        public static String TABLE = "ErrorCodeMgtDb";

    }

    public static class ReturnGiftDetailDbCols {
        public static String RID = "RID";
        public static String RUSER = "RUSER";
        public static String PHONE_NUMBER = "phone_number";
        public static String PROMOTION_NAME = "promotion_name";
        public static String TIME = "return_time";
        public static String RESULT = "result";
        public static String TABLE = "ReturnGiftDetailDb";
    }

    public static class ReturnGiftDbCols {
        public static String RID = "RID";
        public static String PROMOTION_NAME = "promotion_name";
        public static String TIME = "time";
        public static String TABLE = "ReturnGiftDb";
    }

    public static class GrabPromoDbCols {
        public static String NUMBER = "_id";
        public static String CODE = "code";
        public static String CARDID = "cardid";
        public static String TIME_INPUT = "time_input";
        public static String TID = "tran_id";
        public static String TIME_OF_BONUS = "time_of_bonus";
        public static String IS_LOCK = "is_lock";
        public static String END_PROMO = "end_promo";
        public static String TIME_REGISTER = "time_register";
        public static String TIME_UPDATE = "time_update";
        public static String TABLE = "GrabPromoDb";
    }

    public static class GrabCodePromoDbCols {
        public static String CODE = "_id";
        public static String NUMBER = "number";
        public static String TABLE = "GrabCodePromoDb";

    }

    public static class GrabVoucherDbCols {
        public static String NUMBER = "number";
        public static String CODE = "code";
        public static String TIME_CASHIN = "time_cashin";
        public static String CASHIN_AMOUNT = "cashin_amount";
        public static String VOUCHER_TIME = "voucher_time";
        public static String VOUCHER_AMOUNT = "voucher_amount";
        public static String TABLE = "GrabVoucherDb";
        public static String GIFT_ID = "gift_id";
        public static String IS_USED = "is_used";
        public static String TIME_OF_VOUCHER = "time_of_voucher";
        public static String BONUS_EXTRA = "bonus_extra";
    }

    public static class CheatInfoDbCols {
        public static String PHONE_NUMBER = "phone_number";
        public static String TIME = "time";
        public static String INFO = "info";
        public static String TABLE = "CheatInfoDb";
    }

    public static class IPOSTransactionInfoCols {
        public static String PAYMENT_CODE = "_id";
        public static String TIME = "time";
        public static String MERCHANT_ID = "merchant_id";
        public static String DECRYPTED_PAYMENT_CODE = "decrypted_payment_code";
        public static String TABLE = "IPOSTransactionInfoDb";
    }

    public static class RetainBinhTanDbCols {
        public static String NUMBER = "_id";
        public static String LAST_TID= "last_tid";
        public static String LAST_TRANS_TIME = "last_trans_time";
        public static String TIME_OF_VOUCHER = "time_of_voucher";
        public static String START_TIME = "start_time";
        public static String END_TIME = "end_time";
        public static String UPDATE_TIME = "update_time";
        public static String TABLE = "RetainBinhTanDb";
    }

    public static class RetainBinhTanVoucherDbCols {
        public static String NUMBER = "number";
        public static String TIME_CASHIN = "time_cashin";
        public static String CASHIN_AMOUNT = "cashin_amount";
        public static String VOUCHER_TIME = "voucher_time";
        public static String VOUCHER_AMOUNT = "voucher_amount";
        public static String GIFT_ID = "gift_id";
        public static String IS_USED = "is_used";
        public static String TIME_OF_VOUCHER = "time_of_voucher";
        public static String BONUS_EXTRA = "bonus_extra";
        public static String TABLE = "RetainBinhTanVoucherDb";
    }

}

