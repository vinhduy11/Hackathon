package com.mservice.momo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by locnguyen on 11/08/2014.
 */
public class Constant123Pay {

    private static final Map<String, String> bankName;
    private static final Map<String, String> shortbankName;

    private static final Map<Integer, String> desc;

    static {
        bankName = new HashMap<>();

        bankName.put("VCB","Ngân hàng Ngoại Thương Việt Nam");
        bankName.put("DAB","Ngân hàng Đông Á");
        bankName.put("VTB","Ngân hàng Công Thương Việt Nam");
        bankName.put("AGB","Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam");
        bankName.put("TCB","Ngân hàng Kỹ Thương Việt Nam");
        bankName.put("EIB","Ngân hàng Xuất Nhập Khẩu Việt Nam");
        bankName.put("SCB","Ngân hàng Sài Gòn Thương Tín");
        bankName.put("VIB","Ngân hàng quốc tế");
        bankName.put("BIDV","Ngân hàng đầu tư và phát triển Việt Nam");
        bankName.put("MB","Ngân hàng quân đội");
        bankName.put("ACB","Ngân hàng Á Châu");
        bankName.put("MRTB","Ngân hàng TMCP Hàng Hải");
        bankName.put("GPB","Ngân Hàng Dầu Khí Toàn Cầu");
        bankName.put("HDB","Ngân Hàng TMCP Phát Triển TPHCM");
        bankName.put("NVB","Ngân hàng TMCP Nam Việt");
        bankName.put("VAB","Ngân hàng TMCP Việt Á");
        bankName.put("VPB","Ngân hàng Việt Nam Thịnh Vượng");
        bankName.put("BAB","Ngân hàng TMCP Bắc Á");
        bankName.put("OCEB","Ngân hàng TMCP Đại Dương");
        bankName.put("ABB","Ngân hàng TMCP An Bình");
        bankName.put("NAB","Ngân hàng TMCP Nam Á");
        bankName.put("SGB","Ngân hàng TMCP Sài Gòn Công Thương");
        bankName.put("PGB","Ngân hàng Dầu Khí");
        bankName.put("OCB","Ngân hàng TMCP Phương Đông");
        bankName.put("DAIAB","Ngân hàng TMCP Đại Á");
        bankName.put("TPB","Ngân hàng Tiền Phong");
        bankName.put("CC","Master Card, Visa Card, JCB");

    }

    static {
        shortbankName = new HashMap<>();

        shortbankName.put("VCB","Vietcombank");
        shortbankName.put("DAB","Dong A Bank");
        shortbankName.put("VTB","Vietinbank");
        shortbankName.put("AGB","Agribank");
        shortbankName.put("TCB","Techcombank");
        shortbankName.put("EIB","Eximbank");
        shortbankName.put("SCB","Sacombank");
        shortbankName.put("VIB","VIB");
        shortbankName.put("BIDV","BIDV");
        shortbankName.put("MB","MB Bank");
        shortbankName.put("ACB","ACB");
        shortbankName.put("MRTB","Maritime Bank");
        shortbankName.put("GPB","GPBank");
        shortbankName.put("HDB","HDBank");
        shortbankName.put("NVB","NaviBank");
        shortbankName.put("VAB","Viet A Bank");
        shortbankName.put("VPB","VPBank");
        shortbankName.put("BAB","Bac A Bank");
        shortbankName.put("OCEB","OceanBank");
        shortbankName.put("ABB","ABBank");
        shortbankName.put("NAB","Nam A Bank");
        shortbankName.put("SGB","Saigon Bank");
        shortbankName.put("PGB","PG Bank");
        shortbankName.put("OCB","OCB");
        shortbankName.put("DAIAB","DaiA Bank");
        shortbankName.put("TPB","TienPhong Bank");
        shortbankName.put("CC","Thẻ Master/Visa/JCB");

    }



    static {
        desc = new HashMap<>();

        desc.put(0,"Mới");
        desc.put(1, "Thành công");
        desc.put(-10, "Giao dịch không tồn tại. Vui lòng thực hiện giao dịch mới.");
        desc.put(-100, "Đơn hàng bị hủy");
        desc.put(10, "Đang kiểm tra thông tin tài khoản. Giao dịch chưa bị trừ tiền.");
        desc.put(20, "Không xác định trạng thái thanh toán từ ngân hàng");
        desc.put(5000, "Hệ thống bận");
        desc.put(6200, "Vi phạm quy định nghiệp vụ giữa đối tác & 123Pay");
        desc.put(6212, "Ngoài giới hạn thanh toán / giao dịch");
        desc.put(7200, "Thông tin thanh toán không hợp lệ");
        desc.put(7201, "Không đủ tiền trong tài khoản thanh toán");
        desc.put(7202, "Không đảm bảo số dư tối thiểu trong tài khoản thanh toán");
        desc.put(7203, "Giới hạn tại ngân hàng: Tổng số tiền / ngày");
        desc.put(7204, "Giới hạn tại ngân hàng: Tổng số giao dịch / ngày");
        desc.put(7205, "Giới hạn tại ngân hàng: Giá trị / giao dịch");
        desc.put(7210, "Khách hàng không nhập thông tin thanh toán");
        desc.put(7211, "Chưa đăng ký dịch vụ thanh toán trực tuyến");
        desc.put(7212, "Dịch vụ thanh toán trực tuyến của tài khoản đang tạm khóa");
        desc.put(7213, "Tài khoản thanh toán bị khóa");
        desc.put(7220, "Khách hàng không nhập OTP");
        desc.put(7221, "Nhập sai thông tin thẻ/tài khoản quá 3 lần");
        desc.put(7222, "Sai thông tin OTP");
        desc.put(7223, "OTP hết hạn");
        desc.put(7224, "Nhập sai thông tin OTP quá 3 lần");
        desc.put(7231, "Sai tên chủ thẻ");
        desc.put(7232, "Card không hợp lệ, không tìm thấy khách hàng / tài khoản");
        desc.put(7233, "Expired Card");
        desc.put(7234, "Lost Card");
        desc.put(7235, "Stolen Card");
        desc.put(7236, "Card is marked deleted");
        desc.put(7241, "Credit Card - Card Security Code verification failed");
        desc.put(7242, "Credit Card - Address Verification Failed");
        desc.put(7243, "Credit Card - Address Verification and Card Security Code Failed");
        desc.put(7244, "Credit Card - Card did not pass all risk checks");
        desc.put(7245, "Credit Card - Bank Declined Transaction");
        desc.put(7246, "Credit Card - Account has stop/hold(hold money,...)");
        desc.put(7247, "Credit Card - Account closed");
        desc.put(7248, "Credit Card - Frozen Account");
        desc.put(7300, "Lỗi giao tiếp hệ thống ngân hàng");
    }

    public static String getDesc(int errorCode) {
        String str = desc.get(errorCode);
        if(str == null){
            return errorCode + "";
        }
        return str;
    }
    public static String getBankName(String bankCode) {
        String str = bankName.get(bankCode);
        if(str == null){
            return bankCode;
        }
        return str;
    }

    public static String getShortBankName(String bankCode) {
        String str = shortbankName.get(bankCode);
        if(str == null){
            return bankCode;
        }
        return str;
    }
}