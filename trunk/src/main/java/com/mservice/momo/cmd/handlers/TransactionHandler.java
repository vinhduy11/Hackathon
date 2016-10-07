package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.FeeCollection;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.PhoneNumberUtil;
import com.mservice.momo.vertx.models.TranObj;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ntunam on 3/20/14.
 */
public class TransactionHandler extends CommandHandler {

    /**
     * MAPPING PROVIDER NAME FROM PROVIDER ID
     */
    private static Map<String, String> providerName;

    static {
        providerName = new HashMap<>();
        providerName.put("jetstar", "Jetstar");
        providerName.put("amr", "Mekong Airline");
        providerName.put("dien", "EVN");
        providerName.put("vco", "VTC");
        providerName.put("bac", "FPT");
        providerName.put("zxu", "VinaGame");
        providerName.put("onc", "VDC");
        providerName.put("vinahcm", "Vinaphone");
        providerName.put("cdhcm", "VNPT");
        providerName.put("ifpt", "FPT");
        providerName.put("ivettel", "Viettel");
        providerName.put("aviettel", "Viettel");
        providerName.put("nuochcm", "Nuoc Chợ Lớn");
        providerName.put("avg", "Truyền Hình An Viên");
    }

    private static String getProviderNameFromProviderId(String id) {
        String name = providerName.get(id);
        return name == null ? "" : name;
    }



    public TransactionHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject config) {
        super(mainDb, vertx, container, config);
    }

    private Integer getMomoProtoTransactionStatus(CmdModels.TransactionStatus status) {
        switch (status) {
            case FAILED:
                return TranObj.STATUS_FAIL;
            case SUCCESS:
                return TranObj.STATUS_OK;
            case PROCESS:
                return TranObj.STATUS_PROCESS;
            default:
                return null;
        }
    }

    private CmdModels.TransactionStatus toCmdModelsTransactionStatus(int momoProtoTransactionStatus) {
        switch (momoProtoTransactionStatus) {
            case TranObj.STATUS_OK:
                return CmdModels.TransactionStatus.SUCCESS;
            case TranObj.STATUS_FAIL:
                return CmdModels.TransactionStatus.FAILED;
            case TranObj.STATUS_PROCESS:
                return CmdModels.TransactionStatus.PROCESS;
            case TranObj.CANCELLED:
                return CmdModels.TransactionStatus.CANCELLED;
            default:
                return CmdModels.TransactionStatus.UNKNOWN_STATUS;
        }
    }

    private Integer toMomoProtoTransactionType(CmdModels.TransactionType type) {
        return type.getNumber();
    }

    private CmdModels.TransactionType toCmdModelsTransactionType(TranObj tran) {
        if (tran.tranType == MomoProto.TranHisV1.TranType.M2M_VALUE) {
            if("123pay".equals(tran.parterCode)) {
                return CmdModels.TransactionType.BANK_NET_TO_MOMO_TYPE; // để web show: "Nạp tiền từ ATM nội địa."
            }
        }
        if(tran.tranType == MomoProto.TranHisV1.TranType.BANK_IN_VALUE) {
            if("12345".equals(tran.parterCode) || "102".equals(tran.parterCode)) {
                // đúng là nạp tiền từ ngân hàng liên kết.
            } else {
                return CmdModels.TransactionType.BANK_NET_TO_MOMO_TYPE;
            }
        }
        CmdModels.TransactionType result = CmdModels.TransactionType.valueOf(tran.tranType);
        if (result == null)
            return CmdModels.TransactionType.UNKNOWN_TYPE;
        return result;
    }


    public void getTransaction(final CommandContext context) {
        final CmdModels.GetTransaction cmd = (CmdModels.GetTransaction) context.getCommand().getBody();
        final Integer status = getMomoProtoTransactionStatus(cmd.getStatus());
        final List<Integer> types = new ArrayList<>();

        List<CmdModels.TransactionType> listType = cmd.getTypeList();
        for(CmdModels.TransactionType type: listType) {
            types.add(toMomoProtoTransactionType(type));
        }

        final Long startTime = cmd.getStartTime() == 0 ? null : cmd.getStartTime();
        final Long endTime = cmd.getEndTime() == 0 ? null : cmd.getEndTime();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber())) {
            context.replyError(-1, "Phone number is not valid.");
            return;
        }

        mainDb.transDb.countWithFilter(cmd.getPhoneNumber(), startTime, endTime, status, types, new Handler<Long>() {
            @Override
            public void handle(final Long count) {
                int pageCount = count.intValue() / cmd.getPageSize();// result count never greater than Integer.MAX
                if (count.intValue() % cmd.getPageSize() > 0)
                    pageCount++;
                final int finalPageCount = pageCount;

                mainDb.transDb.getTransaction(cmd.getPhoneNumber(), cmd.getPageSize(), cmd.getPageNumber(), startTime, endTime, status, types, new Handler<ArrayList<TranObj>>() {
                    @Override
                    public void handle(ArrayList<TranObj> objs) {
                        CmdModels.GetTransactionReply.Builder builder = CmdModels.GetTransactionReply.newBuilder();
                        for (TranObj obj : objs) {
                            CmdModels.TransactionType type = toCmdModelsTransactionType(obj);
                            CmdModels.TransactionStatus status = toCmdModelsTransactionStatus(obj.status);

                            String providername = getPartnerNameFromTranHis(obj);
//                            if (providername == null || providername.isEmpty()) {
//                                providername = "M_Service";
//                            }

                            builder.addTransactions(
                                    CmdModels.Transaction.newBuilder()
                                            .setAmount(obj.amount)
                                            .setId(obj.tranId)
                                            .setTime(obj.finishTime)
                                            .setPartnerName(providername)
                                            .setType(type)
                                            .setStatus(status)
                                            .setPartnerId(getPartnerIdFromTranHis(obj))
                                            .build()
                            );
                        }

                        builder.setPhoneNumber(cmd.getPhoneNumber());
                        builder.setPageCount(finalPageCount);

                        MomoCommand cmd = new MomoCommand(CmdModels.CommandType.GET_TRANSACTION_REPLY, builder.build());
                        context.reply(cmd);
                    }
                });
            }
        });

//        mainDb.transDb.getTransaction(cmd.getPhoneNumber(), cmd.getPageSize(), cmd.getPageNumber()
//                , cmd.getStartTime(), cmd.getEndTime(), status, new Handler<ArrayList<TransDb.TranObj>>() {
//            @Override
//            public void handle(ArrayList<TransDb.TranObj> objs) {
//                CmdModels.GetTransactionReply.Builder builder = CmdModels.GetTransactionReply.newBuilder();
//                for (TransDb.TranObj obj : objs) {
//                    CmdModels.TransactionType type = toCmdModelsTransactionType(obj.cmdType);
//                    CmdModels.TransactionStatus status = toCmdModelsTransactionStatus(obj.status);
//
//                    builder.addTransactions(
//                            CmdModels.Transaction.newBuilder()
//                                    .setAmount(obj.amount)
//                                    .setId(obj.tranId)
//                                    .setTime(obj.finishTime)
//                                    .setType(type)
//                                    .setStatus(status)
//                                    .build()
//                    );
//                }
//
//                builder.setPhoneNumber(cmd.getPhoneNumber());
//                builder.setPageCount(1);
//
//                MomoCommand cmd = new MomoCommand(CmdModels.CommandType.GET_TRANSACTION_REPLY, builder.build());
//                context.reply(cmd);
//            }
//        });

    }

    private String getPartnerIdFromTranHis(TranObj obj) {
        switch (obj.tranType) {
            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
                return ""; // obj.parterCode;
            case MomoProto.TranHisV1.TranType.BANK_OUT_VALUE:
                return ""; //obj.parterCode;
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                return "";
            case MomoProto.TranHisV1.TranType.M2C_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                try {
                    int number = Integer.parseInt(obj.partnerId);
                    return "0" + number;
                } catch (NumberFormatException e) {
                }
                return ""; // Đối tượng giaoi dịch không phải là số điện thoại
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
                String providerName = getProviderNameFromProviderId(obj.partnerId);
                return providerName;
            case MomoProto.TranHisV1.TranType.QUICK_PAYMENT_VALUE:
                return obj.partnerId;
            case MomoProto.TranHisV1.TranType.QUICK_DEPOSIT_VALUE:
                return obj.partnerId;
            case MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BANK_NET_VERIFY_OTP_VALUE:
                return "";
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_OTHER_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TELEPHONE_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_TRAIN_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BILL_PAY_INSURANCE_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BILL_PAY_INTERNET_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BILL_PAY_OTHER_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.DEPOSIT_CASH_OTHER_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BUY_MOBILITY_CARD_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BUY_GAME_CARD_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BUY_OTHER_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.DEPOSIT_CASH_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.BILL_PAY_CINEMA_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE:
                return "";
            case MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE:
                return "";
            case MomoProto.TranHisV1.TranType.BONUS_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.FEE_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
                return "";
            case MomoProto.TranHisV1.TranType.PAY_NUOCCL_BILL_VALUE:
                return obj.parterCode;
            case MomoProto.TranHisV1.TranType.PAY_AVG_BILL_VALUE:
                return obj.parterCode;
        }
        return obj.parterCode;
    }

    private String getPartnerNameFromTranHis(TranObj obj) {
        switch (obj.tranType) {
            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BANK_OUT_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                return DataUtil.phoneProviderName(obj.parterCode);
            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                return getProviderNameFromProviderId(obj.partnerId);
            case MomoProto.TranHisV1.TranType.M2C_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.QUICK_PAYMENT_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.QUICK_DEPOSIT_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BANK_NET_VERIFY_OTP_VALUE:
                return "Nạp tiền từ thẻ ATM";
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_OTHER_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE:
                if (obj.io > 0)
                    return "Nạp tiền tại điểm giao dịch";
                return "Rút tiền tại điểm giao dịch";
            case MomoProto.TranHisV1.TranType.BILL_PAY_TELEPHONE_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                return "Jetstar";
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_TRAIN_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BILL_PAY_INSURANCE_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BILL_PAY_INTERNET_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BILL_PAY_OTHER_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.DEPOSIT_CASH_OTHER_VALUE:
            return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BUY_MOBILITY_CARD_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BUY_GAME_CARD_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BUY_OTHER_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.DEPOSIT_CASH_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.BILL_PAY_CINEMA_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE:
                return obj.partnerRef;
            case MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE:
                return "Nạp tiền tận nơi";
            case MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE:
                return "Rút tiền tận nơi";
            case MomoProto.TranHisV1.TranType.BONUS_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.FEE_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
                return "Thanh toán vé xem phim";
            case MomoProto.TranHisV1.TranType.PAY_NUOCCL_BILL_VALUE:
                return obj.partnerName;
            case MomoProto.TranHisV1.TranType.PAY_AVG_BILL_VALUE:
                return obj.partnerName;
        }
        return obj.partnerName;
    }

    public void getTransactionDetail(final CommandContext context) {
        final CmdModels.GetTransactionDetail cmd = (CmdModels.GetTransactionDetail) context.getCommand().getBody();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber())) {
            context.replyError(-1, "Phone number is not valid.");
            return;
        }

        mainDb.transDb.getTransactionDetail(cmd.getPhoneNumber(), cmd.getTransactionId(), new Handler<TranObj>() {

            @Override
            public void handle(TranObj obj) {
                if(obj==null) {
                    obj = new TranObj(); // không lấy lên đc
                }
                CmdModels.TransactionType type = toCmdModelsTransactionType(obj);
                CmdModels.TransactionStatus status = toCmdModelsTransactionStatus(obj.status);

                String partnerId = getPartnerIdFromTranHis(obj);
                String partnerName = getPartnerNameFromTranHis(obj);

                CmdModels.GetTransactionDetailReply replyBody = CmdModels.GetTransactionDetailReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setId(obj.tranId)
                        .setTime(obj.finishTime)
                        .setAmount(obj.amount)
                        .setType(type)
                        .setStatus(status)
                        .setPartnerId(partnerId)
                        .setPartnerCode(obj.parterCode)
                        .setPartnerName(partnerName)
                        .setBillId(partnerId)
                        .setComment(obj.comment)
                        .setPartnerPref(obj.partnerRef)
                        .setErrorCode(obj.error)
                        .build();

                MomoCommand cmd = new MomoCommand(CmdModels.CommandType.GET_TRANSACTION_DETAIL_REPLY, replyBody);
                context.reply(cmd);
            }
        });
    }

    public void getTransactionFee(final CommandContext context) {

        CmdModels.GetTransactionFeeReply.Builder builder = CmdModels.GetTransactionFeeReply.newBuilder() ;
        for (FeeDb.Obj o: FeeCollection.getInstance().getAll()){
            builder.addTransactionFees(CmdModels.TransactionFee.newBuilder()
                            .setBankId(o.BANKID)
                            .setChannel(o.CHANNEL)
                            .setDynamicFee(o.DYNAMIC_FEE)
                            .setIoCity(o.INOUT_CITY)
                            .setStaticFee(o.STATIC_FEE)
                            .setTransType(o.TRANTYPE).build()

            );
        }

        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_TRANSACTION_FEE_REPLY
                , builder.build());
        context.reply(replyCommand);


    }
}
