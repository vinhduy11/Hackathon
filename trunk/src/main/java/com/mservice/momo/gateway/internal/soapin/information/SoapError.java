package com.mservice.momo.gateway.internal.soapin.information;

import java.util.HashMap;

/**
 * Created by admin on 2/16/14.
 */
public class SoapError {

    public static final int SUCCESS = 0;//	success
    public static final int BAD_FORMAT = 1;//	bad format
    public static final int AGENT_NOT_FOUND = 3;//	agent not found
    public static final int AGENT_NOT_REGISTERED = 4;//	agent not registered
    public static final int AGENT_INACTIVE = 5;//	agent inactive
    public static final int AGENT_SUSPENDED = 6;//	agent suspended
    public static final int ACCESS_DENIED = 7;//	access denied
    public static final int BAD_PASSWORD = 8;//	bad password
    public static final int PASSWORD_EXPIRED = 9;//	password expired
    public static final int PASSWORD_PREV_USED = 10;//	password prev used
    public static final int PASSWORD_RETRY_EXCEED = 11;//	password retry exceed
    public static final int PASSWORD_SAME = 12;//	password same
    public static final int TARGET_NOT_FOUND = 13;//	target not found
    public static final int TARGET_NOT_REGISTERED = 14;//	target not registered
    public static final int TARGET_INACTIVE = 15;//	target inactive
    public static final int TARGET_SUSPENDED = 16;//	target suspended
    public static final int INVALID_AMOUNT = 17;//	invalid amount
    public static final int ALREADY_REGISTERED = 18;//	already registered
    public static final int AGENT_BLACKLISTED = 19;//	agent blacklisted
    public static final int TARGET_BLACKLISTED = 20;//	target blacklisted
    public static final int TARGET_IS_SELF = 22;//	target is self
    public static final int AMOUNT_TOO_SMALL = 23;//	amount too small
    public static final int AMOUNT_TOO_BIG = 24;//	amount too big
    public static final int CONFIRM_WRONG_AMOUNT = 25;//	confirm wrong amount
    public static final int TARGET_REQUIRED = 26;//	target required
    public static final int AGENT_EXPIRED = 27;//	agent expired
    public static final int TARGET_EXPIRED = 28;//	target expired
    public static final int INVALID_PASSWORD = 29;//	invalid password
    public static final int AGENT_ACTIVE = 30;//	agent active
    public static final int AGENT_NOT_PARTY = 31;//	agent not party
    public static final int REQUEST_EXPIRED = 32;//	request expired
    public static final int TRANSACTION_REVERSED = 33;//	transaction reversed
    public static final int TRANSACTION_NOT_TWO_PARTY = 34;//	transaction not two party
    public static final int INVALID_STATUS_CHANGE = 35;//	invalid status change
    public static final int TRANSACTION_TOO_OLD = 36;//	transaction too old
    public static final int TRANSACTION_NOT_COMPLETED = 37;//	transaction not completed
    public static final int AGENT_STOPPED = 38;//	agent stopped
    public static final int TARGET_STOPPED = 39;//	target stopped
    public static final int AGENT_STARTED = 40;//	agent started
    public static final int IN_SYS_ERROR = 41;//	in sys error
    public static final int IN_BAD_MSISDN = 42;//	in bad msisdn
    public static final int IN_SYS_NOT_AVAILABLE = 43;//	in sys not available
    public static final int IN_BAD_VOUCHER = 44;//	in bad voucher
    public static final int IN_TIMEOUT = 45;//	in timeout
    public static final int AMOUNT_OUT_OF_RANGE = 46;//	amount out of range
    public static final int TARGET_IS_NOT_SPECIAL = 47;//	target is not special
    public static final int BILLPAY_ACCOUNT_INVALID = 48;//	billpay account invalid
    public static final int BANK_OFFLINE = 49;//	bank offline
    public static final int BANK_ERROR = 50;//	bank error
    public static final int SYNC_NOT_REQUIRED = 51;//	sync not required
    public static final int DEVICE_ALREADY_ACTIVE = 52;//	device already active
    public static final int CONFIG_UPDATE_NOT_REQUIRED = 53;//	config update not required
    public static final int NOT_DEVICE_OWNER = 54;//	not device owner
    public static final int ALIAS_ALREADY_EXISTS = 55;//	alias already exists
    public static final int ALIAS_NOT_FOUND = 56;//	alias not found
    public static final int AGENT_ALREADY_SUSPENDED = 57;//	agent already suspended
    public static final int AGENT_ALREADY_STOPPED = 58;//	agent already stopped
    public static final int CANNOT_USE_WALLET_TYPE = 59;//	cannot use wallet type
    public static final int SCHEDULE_DISALLOWED = 60;//	schedule disallowed
    public static final int INVALID_SCHEDULE = 61;//	invalid schedule
    public static final int SCHEDULE_NOT_FOUND = 62;//	schedule not found
    public static final int CONFIRM_MULTIPLE_TXNS = 63;//	confirm multiple txns
    public static final int AGENT_IS_NOT_SPECIAL = 64;//	agent is not special
    public static final int CONNECTION_OFFLINE = 66;//	connection offline
    public static final int CONNECTION_ERROR = 67;//	connection error
    public static final int OPERATION_NOT_SUPPORTED = 68;//	operation not supported
    public static final int AGENT_NOT_WELL_CONFIGURED = 69;//	agent not well configured
    public static final int AGENT_HAS_TRANSACTIONS = 70;//	agent has transactions
    public static final int AGENT_DELETED = 71;//	agent deleted
    public static final int TARGET_DELETED = 72;//	target deleted
    public static final int AGENT_HAS_BALANCE = 73;//	agent has balance
    public static final int INVALID_PASSWORD_MISMATCH = 74;//	invalid password mismatch
    public static final int REVERSAL_FAILED = 75;//	reversal failed
    public static final int GROUPNAME_SAME_OR_EXISTS = 76;//	groupname same or exists
    public static final int GROUP_NOT_EMPTY = 77;//	group not empty
    public static final int CONFIRM_SAME_INITIATOR = 78;//	confirm same initiator
    public static final int CONFIRM_REJECTED = 103;//	confirm rejected
    public static final int TRANSACTION_NOT_FOUND = 400;//	transaction not found
    public static final int TRACE_TRANSACTION_NOT_FOUND = 415;//	trace transaction not found
    public static final int TRANS_ALREADY_FINALIZED = 423;//	trans already finalized
    public static final int MESSAGE_TOO_LONG = 425;//	message too long
    public static final int CANNOT_LINK_TO_SUBAGENT = 501;//	cannot link to subagent
    public static final int CANNOT_LINK_TO_SELF = 502;//	cannot link to self
    public static final int LINK_AGENT_DELETED = 503;//	link agent deleted
    public static final int CANNOT_JOIN_TO_SELF = 504;//	cannot join to self
    public static final int AGENTS_ALREADY_JOINED = 505;//	agents already joined
    public static final int JOIN_AGENT_DELETED = 506;//	join agent deleted
    public static final int AGENTS_NOT_JOINED = 507;//	agents not joined
    public static final int PURCHASE_ORDER_NUMBER_NOT_UNIQUE = 508;//	purchase order number not unique
    public static final int INSUFFICIENT_FUNDS = 1001;//	insufficient funds
    public static final int TRANSACTION_RECOVERED = 1002;//	transaction recovered
    public static final int WALLET_BALANCE_EXCEEDED = 1003;//	wallet balance exceeded
    public static final int WALLET_CAP_EXCEEDED = 1004;//	wallet cap exceeded
    public static final int REQUEST_EXPIRED_1 = 1005;//	request expired
    public static final int SYSTEM_ERROR = 1006;//	system error
    public static final int DB_ERROR = 1007;//	Db error
    public static final int BAD_REQUEST = 1008;//	bad request
    public static final int DB_CONSTRAINT = 1009;//	Db constraint
    public static final int DB_NO_RECORD = 1010;//	Db no record
    public static final int AUTH_BAD_TOKEN = 1011;//	auth bad token
    public static final int AUTH_RETRY_EXCEED = 1012;//	auth retry exceed
    public static final int AUTH_EXPIRED = 1013;//	auth expired
    public static final int AUTH_BAD_PASSWORD = 1014;//	auth bad password
    public static final int AUTH_SAME_PASSWORD = 1015;//	auth same password
    public static final int AUTH_PASSWORD_PREV_USED = 1016;//	auth password prev used
    public static final int PARAM_INVALID_REQUIRED = 1020;//	param invalid required
    public static final int PARAM_INVALID_TOO_SHORT = 1021;//	param invalid too short
    public static final int PARAM_INVALID_TOO_LONG = 1022;//	param invalid too long
    public static final int PARAM_INVALID_REGXP = 1023;//	Tài khoản ngân hàng không đủ để thực hiện giao dịch
    public static final int LICENCE_EXPIRED = 1024;//	licence expired
    public static final int C2C_MTCN_IN_PROCESS = 541;
    public static final int C2C_MTCN_OR_AMOUNT_WRONG = 542;
    public static final int C2C_SOURCE_AND_DEST_AGENT_SAME = 543;
    // .....new.....


    private static HashMap<Integer, String> CoreErrorMap = new HashMap<>();

    static {
        CoreErrorMap.put(-1, "error");
        CoreErrorMap.put(0, "success");
        CoreErrorMap.put(1, "bad format");
        CoreErrorMap.put(3, "agent not found");
        CoreErrorMap.put(4, "agent not registered");
        CoreErrorMap.put(5, "agent inactive");
        CoreErrorMap.put(6, "agent suspended");
        CoreErrorMap.put(7, "access denied");
        CoreErrorMap.put(8, "bad password");
        CoreErrorMap.put(9, "password expired");
        CoreErrorMap.put(10, "password prev used");
        CoreErrorMap.put(11, "password retry exceed");
        CoreErrorMap.put(12, "password same");
        CoreErrorMap.put(13, "target not found");
        CoreErrorMap.put(14, "target not registered");
        CoreErrorMap.put(15, "target inactive");
        CoreErrorMap.put(16, "target suspended");
        CoreErrorMap.put(17, "invalid amount");
        CoreErrorMap.put(18, "already registered");
        CoreErrorMap.put(19, "agent blacklisted");
        CoreErrorMap.put(20, "target blacklisted");
        CoreErrorMap.put(22, "target is self");
        CoreErrorMap.put(23, "amount too small");
        CoreErrorMap.put(24, "amount too big");
        CoreErrorMap.put(25, "confirm wrong amount");
        CoreErrorMap.put(26, "target required");
        CoreErrorMap.put(27, "agent expired");
        CoreErrorMap.put(28, "target expired");
        CoreErrorMap.put(29, "invalid password");
        CoreErrorMap.put(30, "agent active");
        CoreErrorMap.put(31, "agent not party");
        CoreErrorMap.put(32, "request expired");
        CoreErrorMap.put(33, "transaction reversed");
        CoreErrorMap.put(34, "transaction not two party");
        CoreErrorMap.put(35, "invalid status change");
        CoreErrorMap.put(36, "transaction too old");
        CoreErrorMap.put(37, "transaction not completed");
        CoreErrorMap.put(38, "agent stopped");
        CoreErrorMap.put(39, "target stopped");
        CoreErrorMap.put(40, "agent started");
        CoreErrorMap.put(41, "in sys error");
        CoreErrorMap.put(42, "in bad msisdn");
        CoreErrorMap.put(43, "in sys not available");
        CoreErrorMap.put(44, "in bad voucher");
        CoreErrorMap.put(45, "in timeout");
        CoreErrorMap.put(46, "amount out of range");
        CoreErrorMap.put(47, "target is not special");
        CoreErrorMap.put(48, "billpay account invalid");
        CoreErrorMap.put(49, "bank offline");
        CoreErrorMap.put(50, "bank error");
        CoreErrorMap.put(51, "sync not required");
        CoreErrorMap.put(52, "device already active");
        CoreErrorMap.put(53, "config update not required");
        CoreErrorMap.put(54, "not device owner");
        CoreErrorMap.put(55, "alias already exists");
        CoreErrorMap.put(56, "alias not found");
        CoreErrorMap.put(57, "agent already suspended");
        CoreErrorMap.put(58, "agent already stopped");
        CoreErrorMap.put(59, "cannot use wallet type");
        CoreErrorMap.put(60, "schedule disallowed");
        CoreErrorMap.put(61, "invalid schedule");
        CoreErrorMap.put(62, "schedule not found");
        CoreErrorMap.put(63, "confirm multiple txns");
        CoreErrorMap.put(64, "agent is not special");
        CoreErrorMap.put(66, "connection offline");
        CoreErrorMap.put(67, "connection error");
        CoreErrorMap.put(68, "operation not supported");
        CoreErrorMap.put(69, "agent not well configured");
        CoreErrorMap.put(70, "agent has transactions");
        CoreErrorMap.put(71, "agent deleted");
        CoreErrorMap.put(72, "target deleted");
        CoreErrorMap.put(73, "agent has balance");
        CoreErrorMap.put(74, "invalid password mismatch");
        CoreErrorMap.put(75, "reversal failed");
        CoreErrorMap.put(76, "groupname same or exists");
        CoreErrorMap.put(77, "group not empty");
        CoreErrorMap.put(78, "confirm same initiator");
        CoreErrorMap.put(103, "confirm rejected");
        CoreErrorMap.put(400, "transaction not found");
        CoreErrorMap.put(415, "trace transaction not found");
        CoreErrorMap.put(423, "trans already finalized");
        CoreErrorMap.put(425, "message too long");
        CoreErrorMap.put(501, "cannot link to subagent");
        CoreErrorMap.put(502, "cannot link to self");
        CoreErrorMap.put(503, "link agent deleted");
        CoreErrorMap.put(504, "cannot join to self");
        CoreErrorMap.put(505, "agents already joined");
        CoreErrorMap.put(506, "join agent deleted");
        CoreErrorMap.put(507, "agents not joined");
        CoreErrorMap.put(508, "purchase order number not unique");
        CoreErrorMap.put(1001, "insufficient funds");
        CoreErrorMap.put(1002, "transaction recovered");
        CoreErrorMap.put(1003, "wallet balance exceeded");
        CoreErrorMap.put(1004, "wallet cap exceeded");
        CoreErrorMap.put(1005, "request expired");
        CoreErrorMap.put(1006, "system error");
        CoreErrorMap.put(1007, "Db error");
        CoreErrorMap.put(1008, "bad request");
        CoreErrorMap.put(1009, "Db constraint");
        CoreErrorMap.put(1010, "Db no record");
        CoreErrorMap.put(1011, "auth bad token");
        CoreErrorMap.put(1012, "auth retry exceed");
        CoreErrorMap.put(1013, "auth expired");
        CoreErrorMap.put(1014, "auth bad password");
        CoreErrorMap.put(1015, "auth same password");
        CoreErrorMap.put(1016, "auth password prev used");
        CoreErrorMap.put(1020, "param invalid required");
        CoreErrorMap.put(1021, "param invalid too short");
        CoreErrorMap.put(1022, "param invalid too long");
        CoreErrorMap.put(1024, "licence expired");

        CoreErrorMap.put(1023, "Tài khoản ngân hàng không đủ để thực hiện giao dịch");//moi sua

        CoreErrorMap.put(541, "Mã nhận tiền đang được xử lý. Vui lòng thử lại sau 7 phút");
        CoreErrorMap.put(542, "Sai mã nhận tiền hoặc số tiền. Vui lòng kiểm tra lại");
        CoreErrorMap.put(543, "Điểm giao dịch gửi trùng với điểm giao dịch nhận");

    }

    public static HashMap<Integer, String> CoreErrorMap_VN = new HashMap<>();
    static {
        CoreErrorMap_VN.put(-1, "Lỗi");
        CoreErrorMap_VN.put(0, "Thành công");
        CoreErrorMap_VN.put(1, "Mật khẩu không đúng");
        CoreErrorMap_VN.put(3, "Thông tin tài khoản không tồn tại ");
        CoreErrorMap_VN.put(4, "Bạn chưa đăng ký sử dụng dịch vụ");
        CoreErrorMap_VN.put(5, "Tài khoản của bạn chưa kích hoạt");
        CoreErrorMap_VN.put(6, "Tài khoản bị tạm dừng hoạt động");
        CoreErrorMap_VN.put(7, "Không có quyền truy cập");
        CoreErrorMap_VN.put(8, "Sai mật khẩu");
        CoreErrorMap_VN.put(9, "Mật khẩu của bạn đã hết hạn sử dụng");
        CoreErrorMap_VN.put(10, "Mật khẩu đã được dùng trước đây");
        CoreErrorMap_VN.put(11, "Mật khẩu vượt quá số lần nhập");
        CoreErrorMap_VN.put(12, "Mật khẩu đã được dùng");
        CoreErrorMap_VN.put(13, "Mã đặt chỗ không tồn tại");
        CoreErrorMap_VN.put(14, "Tài khoản người nhận chưa đăng ký");
        CoreErrorMap_VN.put(15, "Tài khoản người nhận chưa kích hoạt");
        CoreErrorMap_VN.put(16, "Tài khoản người nhận tạm ngưng hoạt động");
        CoreErrorMap_VN.put(17, "Số tiền không đủ để thực hiện giao dịch");
        CoreErrorMap_VN.put(18, "Tài khoản đã được đăng ký");
        CoreErrorMap_VN.put(19, "Tài khoản người dùng đã bị cấm giao dịch");
        CoreErrorMap_VN.put(20, "Tài khoản người nhận đã bị cấm giao dịch");
        CoreErrorMap_VN.put(22, "Tài khoản người nhận trùng với tài khoản người gửi");
        CoreErrorMap_VN.put(23, "Số tiền nhập quá nhỏ. Vui lòng thử lại");
        CoreErrorMap_VN.put(24, "Số tiền nhập quá lớn. Vui lòng thử lại");
        CoreErrorMap_VN.put(25, "Xác nhận số tiền không hợp lệ. Vui lòng thử lại");
        CoreErrorMap_VN.put(26, "Vui lòng nhập tài khoản người nhận");
        CoreErrorMap_VN.put(27, "Tài khoản người dùng đã hết hạn sử dụng");
        CoreErrorMap_VN.put(28, "Tài khoản người nhận đã hết hạn sử dụng");
        CoreErrorMap_VN.put(29, "Định dạng mật khẩu không đúng. Vui lòng thử lại");
        CoreErrorMap_VN.put(30, "Tài khoản của bạn đã được đăng ký");
        CoreErrorMap_VN.put(31, "Tài khoản của bạn chưa có trong hệ thống");
        CoreErrorMap_VN.put(32, "Hết thời gian giao dịch. Vui lòng thử lại sau ít phút");
        CoreErrorMap_VN.put(33, "Giao dịch không thành công. Vui lòng thử lại sau");
        CoreErrorMap_VN.put(34, "Giao dịch bị lỗi. Vui lòng thử lại");
        CoreErrorMap_VN.put(35, "Thay đổi không hợp lệ. Vui lòng thử lại");
        CoreErrorMap_VN.put(36, "Giao dịch quá lâu");
        CoreErrorMap_VN.put(37, "Giao dịch chưa được hoàn tất");
        CoreErrorMap_VN.put(38, "Tài khoản của bạn đã ngừng giao dịch");
        CoreErrorMap_VN.put(39, "Tài khoản người nhận đã ngừng giao dịch");
        CoreErrorMap_VN.put(40, "Tài khoản người dùng đã bắt đầu giao dịch");
        CoreErrorMap_VN.put(41, "Hệ thống báo lỗi. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(42, "Hệ thống báo lỗi. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(43, "Hệ thống báo lỗi. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(44, "Hệ thống báo lỗi. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(45, "Hệ thống báo lỗi. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(46, "Số tiền vượt quá phạm vi. Vui lòng thử lại");
        CoreErrorMap_VN.put(47, "Nhà cung cấp không tồn tại. Vui lòng thử lại sau ít phút");
        CoreErrorMap_VN.put(48, "Tài khoản hóa đơn không hợp lệ. Vui lòng thử lại");
        CoreErrorMap_VN.put(49, "Ngân hàng tạm ngừng giao dịch. Vui lòng thử lại sau ít phút");
        CoreErrorMap_VN.put(50, "Lỗi từ ngân hàng. Vui lòng thử lại sau ít phút");
        CoreErrorMap_VN.put(51, "Không yêu cầu đồng bộ");
        CoreErrorMap_VN.put(52, "Thiết bị của bạn đã được kích hoạt MoMoS");
        CoreErrorMap_VN.put(53, "config update not required");
        CoreErrorMap_VN.put(54, "Không phải chủ sở hữu thiết bị");
        CoreErrorMap_VN.put(55, "Bí danh đã tồn tại");
        CoreErrorMap_VN.put(56, "Không tìm thấy bí danh");
        CoreErrorMap_VN.put(57, "Tài khoản người dùng đã bị tạm ngưng hoạt động");
        CoreErrorMap_VN.put(58, "Tài khoản người dùng đã dừng hoạt động");
        CoreErrorMap_VN.put(59, "Không thể sử dụng loại ví");
        CoreErrorMap_VN.put(60, "Lịch trình không được chấp nhận");
        CoreErrorMap_VN.put(61, "Lịch trình không hợp lệ");
        CoreErrorMap_VN.put(62, "Lịch trình không tồn tại");
        CoreErrorMap_VN.put(63, "Bạn hãy hoàn tất giao dịch đang chờ trước khi thực hiện giao dịch mới");
        CoreErrorMap_VN.put(64, "agent is not special");
        CoreErrorMap_VN.put(66, "Vui lòng kiểm tra kết nối wifi hoặc 2G/3G");
        CoreErrorMap_VN.put(67, "Lỗi kết nối. Vui lòng thử lại sau ít phút");
        CoreErrorMap_VN.put(68, "Bạn không được hỗ trợ thực hiện thao tác này. Vui lòng chọn cách khác");
        CoreErrorMap_VN.put(69, "Tài khoản người dùng không được cấu hình tốt");
        CoreErrorMap_VN.put(70, "Tài khoản người dùng có phiên giao dịch");
        CoreErrorMap_VN.put(71, "Tài khoản người dùng đã bị xóa");
        CoreErrorMap_VN.put(72, "Tài khoản người nhận đã bị xóa");
        CoreErrorMap_VN.put(73, "Số dư tài khoản người dùng");
        CoreErrorMap_VN.put(74, "Mật khẩu không hợp lệ. Vui lòng thử lại");
        CoreErrorMap_VN.put(75, "reversal failed");
        CoreErrorMap_VN.put(76, "Tên nhóm bị trùng hoặc đã được sử dụng");
        CoreErrorMap_VN.put(77, "Nhóm không được trống");
        CoreErrorMap_VN.put(78, "confirm_same_initiator");
        CoreErrorMap_VN.put(103, "Hủy giao dịch treo tiền");
        CoreErrorMap_VN.put(400, "Phiên giao dịch không tồn tại");
        CoreErrorMap_VN.put(415, "Giao dịch không tồn tại. Vui lòng thử lại sau ít phút");
        CoreErrorMap_VN.put(423, "Hoàn tất phiên giao dịch");
        CoreErrorMap_VN.put(425, "Nội dung tin nhắn quá dài. Vui lòng thử lại");
        CoreErrorMap_VN.put(501, "Không thể kết nối đến người dùng");
        CoreErrorMap_VN.put(502, "Không thể tự kết nối");
        CoreErrorMap_VN.put(503, "Kết nối người dùng đã bị xóa");
        CoreErrorMap_VN.put(504, "Không thể tự tham gia");
        CoreErrorMap_VN.put(505, "Người dùng đã tham gia");
        CoreErrorMap_VN.put(506, "Người dùng tham gia đã bị xóa");
        CoreErrorMap_VN.put(507, "Người dùng không tham gia ");
        CoreErrorMap_VN.put(508, "Hóa đơn thanh toán không duy nhất");
        CoreErrorMap_VN.put(1001, "Tài khoản không đủ tiền");
        CoreErrorMap_VN.put(1002, "Giao dịch đã phục hồi");
        CoreErrorMap_VN.put(1003, "Số dư vượt quá hạn mức cho phép");
        CoreErrorMap_VN.put(1004, "Tài khoản người nhận hết hạn mức giao dịch");
        CoreErrorMap_VN.put(1005, "request expired");
        CoreErrorMap_VN.put(1006, "Lỗi hệ thống. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(1007, "Lỗi cơ sở dữ liệu. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(1008, "Yêu cầu không hợp lệ");
        CoreErrorMap_VN.put(1009, "Lỗi cơ sở dữ liệu. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(1010, "Lỗi cơ sở dữ liệu. Chúng tôi ghi nhận và cố gắng khắc phục sớm");
        CoreErrorMap_VN.put(1011, "auth bad token");
        CoreErrorMap_VN.put(1012, "Tài khoản của bạn đã bị khóa ");
        CoreErrorMap_VN.put(1013, "Xác thực hết hạn");
        CoreErrorMap_VN.put(1014, "Mật khẩu bạn nhập không chính xác");
        CoreErrorMap_VN.put(1015, "Mật khẩu không được trùng mật khẩu cũ");
        CoreErrorMap_VN.put(1016, "Mật khẩu mới trùng với mật khẩu trước đây");
        CoreErrorMap_VN.put(1020, "Tham số không hợp lệ. Vui lòng nhập lại");
        CoreErrorMap_VN.put(1021, "Tham số quá ngắn. Vui lòng nhập lại");
        CoreErrorMap_VN.put(1022, "Tham số quá dài. Vui lòng nhập lại");
        CoreErrorMap_VN.put(1024, "Giấy phép hết hạn");

        CoreErrorMap_VN.put(1023, "Tài khoản ngân hàng không đủ để thực hiện giao dịch");//moi sua

        CoreErrorMap_VN.put(541, "Mã nhận tiền đang được xử lý. Vui lòng thử lại sau 7 phút");
        CoreErrorMap_VN.put(542, "Sai mã nhận tiền hoặc số tiền. Vui lòng kiểm tra lại");
        CoreErrorMap_VN.put(543, "Điểm giao dịch gửi trùng với điểm giao dịch nhận");
        CoreErrorMap_VN.put(9003, "Hệ thống đang tạm ngưng, xin vui lòng thực hiện lại sau");

    }

    public static String getDesc(int errorCode) {
        String s = CoreErrorMap.get(errorCode);
        if (s == null) {
            return "not defined description for " + errorCode;
        }
        return s;
    }
}
