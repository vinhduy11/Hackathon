package com.mservice.momo.msg;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.msg.CmdModels.CommandType;
import org.vertx.java.core.buffer.Buffer;

/**
 * Created by ntunam on 3/13/14.
 */
public class MomoCommand {

    private long id;
    private CommandType commandType;
    private int phone;
    private Object body;

    public MomoCommand() {
    }

    public int getPhone() {
        return phone;
    }

    public void setPhone(int phone) {
        this.phone = phone;
    }

    public MomoCommand(CommandType commandType) {
        this.commandType = commandType;
    }

    public MomoCommand(CommandType commandType, Object body) {
        this.commandType = commandType;
        this.body = body;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public MomoCommand setCommandType(CommandType commandType) {
        this.commandType = commandType;
        return this;
    }

    public Object getBody() {
        return body;
    }

    public MomoCommand setBody(Object body) {
        this.body = body;
        return this;
    }
    
    public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public static Buffer toBuffer(MomoCommand momoCommand) {
        if (momoCommand == null) {
            throw new IllegalArgumentException("Parsing MomoCommand can't be null!");
        }
        Buffer buffer = new Buffer(12);
        buffer.setLong(0, momoCommand.id);
        buffer.setInt(8, momoCommand.commandType.getNumber());
        buffer.setInt(12, momoCommand.phone);
        if (momoCommand.body != null) {
            switch (momoCommand.commandType) {
                case ERROR:
                    buffer.appendBytes(((CmdModels.Error) momoCommand.body).toByteArray());
                    break;
                case SEND_OTP:
                    buffer.appendBytes(((CmdModels.SendOtp) momoCommand.body).toByteArray());
                    break;
                case SEND_OTP_REPLY:
                    buffer.appendBytes(((CmdModels.SendOtpReply) momoCommand.body).toByteArray());
                    break;
                case VERIFY_OTP:
                    buffer.appendBytes(((CmdModels.VerifyOtp) momoCommand.body).toByteArray());
                    break;
                case VERIFY_OTP_REPLY:
                    buffer.appendBytes(((CmdModels.VerifyOtpReply) momoCommand.body).toByteArray());
                    break;
                case REGISTER:
                    buffer.appendBytes(((CmdModels.Register) momoCommand.body).toByteArray());
                    break;
                case REGISTER_REPLY:
                    buffer.appendBytes(((CmdModels.RegisterReply) momoCommand.body).toByteArray());
                    break;
                case GET_AGENT_INFO:
                    buffer.appendBytes(((CmdModels.GetAgentInfo) momoCommand.body).toByteArray());
                    break;
                case GET_AGENT_INFO_REPLY:
                    buffer.appendBytes(((CmdModels.GetAgentInfoReply) momoCommand.body).toByteArray());
                    break;
                case IS_PIN_CORRECT:
                    buffer.appendBytes(((CmdModels.IsPinCorrect) momoCommand.body).toByteArray());
                    break;
                case IS_PIN_CORRECT_REPLY:
                    buffer.appendBytes(((CmdModels.IsPinCorrectReply) momoCommand.body).toByteArray());
                    break;
                case CHANGE_PIN:
                    buffer.appendBytes(((CmdModels.ChangePin) momoCommand.body).toByteArray());
                    break;
                case CHANGE_PIN_REPLY:
                    buffer.appendBytes(((CmdModels.ChangePinReply) momoCommand.body).toByteArray());
                    break;
                case MODIFY_ARTICLE:
                    buffer.appendBytes(((CmdModels.ModifyArticle) momoCommand.body).toByteArray());
                    break;
                case MODIFY_ARTICLE_REPLY:
                    buffer.appendBytes(((CmdModels.ModifyArticleReply) momoCommand.body).toByteArray());
                    break;
                case GET_ARTICLE_PAGE:
                    buffer.appendBytes(((CmdModels.GetArticlePage) momoCommand.body).toByteArray());
                    break;
                case GET_ARTICLE_PAGE_REPLY:
                    buffer.appendBytes(((CmdModels.GetArticlePageReply) momoCommand.body).toByteArray());
                    break;
                case MODIFY_BANK_ACCOUNT:
                    buffer.appendBytes(((CmdModels.ModifyBankAccount) momoCommand.body).toByteArray());
                    break;
                case MODIFY_BANK_ACCOUNT_REPLY:
                    buffer.appendBytes(((CmdModels.ModifyBankAccountReply) momoCommand.body).toByteArray());
                    break;
                case GET_AGENT_BANK_ACCOUNTS:
                    buffer.appendBytes(((CmdModels.GetAgentBankAccounts) momoCommand.body).toByteArray());
                    break;
                case GET_AGENT_BANK_ACCOUNTS_REPLY:
                    buffer.appendBytes(((CmdModels.GetAgentBankAccountsReply) momoCommand.body).toByteArray());
                    break;
                case GET_TRANSACTION:
                    buffer.appendBytes(((CmdModels.GetTransaction) momoCommand.body).toByteArray());
                    break;
                case GET_TRANSACTION_REPLY:
                    buffer.appendBytes(((CmdModels.GetTransactionReply) momoCommand.body).toByteArray());
                    break;
                case GET_TRANSACTION_DETAIL:
                    buffer.appendBytes(((CmdModels.GetTransactionDetail) momoCommand.body).toByteArray());
                    break;
                case GET_TRANSACTION_DETAIL_REPLY:
                    buffer.appendBytes(((CmdModels.GetTransactionDetailReply) momoCommand.body).toByteArray());
                    break;
                case TOPUP:
                    buffer.appendBytes(((CmdModels.Topup) momoCommand.body).toByteArray());
                    break;
                case TOPUP_REPLY:
                    buffer.appendBytes(((CmdModels.TopupReply) momoCommand.body).toByteArray());
                    break;
                case TOPUP_GAME:
                    buffer.appendBytes(((CmdModels.TopupGame) momoCommand.body).toByteArray());
                    break;
                case TOPUP_GAME_REPLY:
                    buffer.appendBytes(((CmdModels.TopupGameReply) momoCommand.body).toByteArray());
                    break;
                case TRANSFER_M2M:
                    buffer.appendBytes(((CmdModels.TransferM2m) momoCommand.body).toByteArray());
                    break;
                case TRANSFER_M2M_REPLY:
                    buffer.appendBytes(((CmdModels.TransferM2mReply) momoCommand.body).toByteArray());
                    break;
                case TRANSFER_M2C:
                    buffer.appendBytes(((CmdModels.TransferM2c) momoCommand.body).toByteArray());
                    break;
                case TRANSFER_M2C_REPLY:
                    buffer.appendBytes(((CmdModels.TransferM2cReply) momoCommand.body).toByteArray());
                    break;
                case GET_BILL_INFO:
                    buffer.appendBytes(((CmdModels.GetBillInfo) momoCommand.body).toByteArray());
                    break;
                case GET_BILL_INFO_REPLY:
                    buffer.appendBytes(((CmdModels.GetBillInfoReply) momoCommand.body).toByteArray());
                    break;
                case PAY_BILL:
                    buffer.appendBytes(((CmdModels.PayBill) momoCommand.body).toByteArray());
                    break;
                case PAY_BILL_REPLY:
                    buffer.appendBytes(((CmdModels.PayBillReply) momoCommand.body).toByteArray());
                    break;
                case BANK_IN:
                    buffer.appendBytes(((CmdModels.BankIn) momoCommand.body).toByteArray());
                    break;
                case BANK_IN_REPLY:
                    buffer.appendBytes(((CmdModels.BankInReply) momoCommand.body).toByteArray());
                    break;
                case BANK_OUT:
                    buffer.appendBytes(((CmdModels.BankOut) momoCommand.body).toByteArray());
                    break;
                case BANK_OUT_REPLY:
                    buffer.appendBytes(((CmdModels.BankOutReply) momoCommand.body).toByteArray());
                    break;
                case BANK_NET_TO_MOMO:
                    buffer.appendBytes(((CmdModels.BanknetToMomo) momoCommand.body).toByteArray());
                    break;
                case BANK_NET_TO_MOMO_REPLY:
                    buffer.appendBytes(((CmdModels.BanknetToMomoReply) momoCommand.body).toByteArray());
                    break;
                case VERIFY_BANKNET_OTP:
                    buffer.appendBytes(((CmdModels.VerifyBanknetOtp) momoCommand.body).toByteArray());
                    break;
                case VERIFY_BANKNET_OTP_REPLY:
                    buffer.appendBytes(((CmdModels.VerifyBanknetOtpReply) momoCommand.body).toByteArray());
                    break;
                case GET_STORE_AROUND:
                    buffer.appendBytes(((CmdModels.GetStoreAround) momoCommand.body).toByteArray());
                    break;
                case GET_STORE_AROUND_REPLY:
                    buffer.appendBytes(((CmdModels.GetStoreAroundReply) momoCommand.body).toByteArray());
                    break;
                case KEY_VALUE_DATA:
                    buffer.appendBytes(((CmdModels.KeyValueData) momoCommand.body).toByteArray());
                    break;
                case KEY_VALUE_DATA_REPLY:
                    buffer.appendBytes(((CmdModels.KeyValueDataReply) momoCommand.body).toByteArray());
                    break;
                case GET_STORE_BY_CODE:
                    buffer.appendBytes(((CmdModels.GetStoreByCode) momoCommand.body).toByteArray());
                    break;
                case GET_STORE_BY_CODE_REPLY:
                    buffer.appendBytes(((CmdModels.GetStoreByCodeReply) momoCommand.body).toByteArray());
                    break;
                case PAY_123MUA_ORDER:
                    buffer.appendBytes(((CmdModels.Pay123MuaOrder) momoCommand.body).toByteArray());
                    break;
                case PAY_123MUA_ORDER_REPLY:
                    buffer.appendBytes(((CmdModels.Pay123MuaOrderReply) momoCommand.body).toByteArray());
                    break;
                case UPDATE_AGENT_INFO:
                    buffer.appendBytes(((CmdModels.UpdateAgentInfo) momoCommand.body).toByteArray());
                    break;
                case UPDATE_AGENT_INFO_REPLY:
                    buffer.appendBytes(((CmdModels.UpdateAgentInfoReply) momoCommand.body).toByteArray());
                    break;
                case DEPOSIT_WITHDRAW_AT_PLACE:AT_PLACE:
                    buffer.appendBytes(((CmdModels.DepositWithdrawAtPlace) momoCommand.body).toByteArray());
                    break;
                case DEPOSIT_WITHDRAW_AT_PLACE_REPLY:
                    buffer.appendBytes(((CmdModels.DepositWithdrawAtPlaceReply) momoCommand.body).toByteArray());
                    break;
                case GET_AVATAR_UPLOAD_TOKEN:
                    buffer.appendBytes(((CmdModels.GetAvatarUploadToken) momoCommand.body).toByteArray());
                    break;
                case GET_AVATAR_UPLOAD_TOKEN_REPLY:
                    buffer.appendBytes(((CmdModels.GetAvatarUploadTokenReply) momoCommand.body).toByteArray());
                    break;
                case SAVE_BILL:
                    buffer.appendBytes(((CmdModels.SaveBill) momoCommand.body).toByteArray());
                    break;
                case SAVE_BILL_REPLY:
                    buffer.appendBytes(((CmdModels.SaveBillReply) momoCommand.body).toByteArray());
                    break;
                case GET_SAVED_BILL:
                    buffer.appendBytes(((CmdModels.GetSavedBill) momoCommand.body).toByteArray());
                    break;
                case GET_SAVED_BILL_REPLY:
                    buffer.appendBytes(((CmdModels.GetSavedBillReply) momoCommand.body).toByteArray());
                    break;
                case REMOVE_SAVED_BILL:
                    buffer.appendBytes(((CmdModels.RemoveSavedBill) momoCommand.body).toByteArray());
                    break;
                case REMOVE_SAVED_BILL_REPLY:
                    buffer.appendBytes(((CmdModels.RemoveSavedBillReply) momoCommand.body).toByteArray());
                    break;
                case GET_TRANSACTION_FEE:
                    buffer.appendBytes(((CmdModels.GetTransactionFee) momoCommand.body).toByteArray());
                    break;
                case GET_TRANSACTION_FEE_REPLY:
                    buffer.appendBytes(((CmdModels.GetTransactionFeeReply) momoCommand.body).toByteArray());
                    break;
                case PAY_AIRLINE_TICKET:
                    buffer.appendBytes(((CmdModels.PayAirlineTicket) momoCommand.body).toByteArray());
                    break;
                case PAY_AIRLINE_TICKET_REPLY:
                    buffer.appendBytes(((CmdModels.PayAirlineTicketReply) momoCommand.body).toByteArray());
                    break;
                case SEND_SMS:
                    buffer.appendBytes(((CmdModels.SendSms) momoCommand.body).toByteArray());
                    break;
                case SEND_SMS_REPLY:
                    buffer.appendBytes(((CmdModels.SendSmsReply) momoCommand.body).toByteArray());
                    break;
                case WITHDRAW_BY_AGENT:
                    buffer.appendBytes(((CmdModels.WithdrawByAgent) momoCommand.body).toByteArray());
                    break;
                case WITHDRAW_BY_AGENT_REPLY:
                    buffer.appendBytes(((CmdModels.WithdrawByAgentReply) momoCommand.body).toByteArray());
                    break;
                case BANK_OUT_MANUAL:
                    buffer.appendBytes(((CmdModels.BankOutManual) momoCommand.body).toByteArray());
                    break;
                case BANK_OUT_MANUAL_REPLY:
                    buffer.appendBytes(((CmdModels.BankOutManualReply) momoCommand.body).toByteArray());
                    break;
                case IS_MOMOER:
                    buffer.appendBytes(((CmdModels.IsMomoer) momoCommand.body).toByteArray());
                    break;
                case IS_MOMOER_REPLY:
                    buffer.appendBytes(((CmdModels.IsMomoerReply) momoCommand.body).toByteArray());
                    break;
                case COUNT_AGENT_TRAN:
                    buffer.appendBytes(((CmdModels.CountAgentTran) momoCommand.body).toByteArray());
                    break;
                case COUNT_AGENT_TRAN_REPLY:
                    buffer.appendBytes(((CmdModels.CountAgentTranReply) momoCommand.body).toByteArray());
                    break;
                case MAKE_TRAN:
                    buffer.appendBytes(((MomoProto.TranHisV1) momoCommand.body).toByteArray());
                    break;
                case MAKE_TRAN_REPLY:
                    buffer.appendBytes(((CmdModels.MakeTranReply) momoCommand.body).toByteArray());
                    break;
                case GET_SERVICE_LAYOUT:
                    buffer.appendBytes(((MomoProto.GetServiceLayout) momoCommand.body).toByteArray());
                    break;
                case GET_SERVICE_LAYOUT_REPLY:
                    buffer.appendBytes(((MomoProto.GetServiceLayoutReply) momoCommand.body).toByteArray());
                    break;
                case COMPLETE_TRAN:
                    buffer.appendBytes(((CmdModels.CompeleteTran) momoCommand.body).toByteArray());
                    break;
                case COMPLETE_TRAN_REPLY:
                    buffer.appendBytes(((MomoProto.TranHisV1) momoCommand.body).toByteArray());
                    break;
                case DO_TRAN:
                    buffer.appendBytes(((MomoProto.TranHisV1) momoCommand.body).toByteArray());
                    break;
                case DO_TRAN_REPLY:
                    buffer.appendBytes(((MomoProto.TranHisV1) momoCommand.body).toByteArray());
                    break;
                case GET_SERVICE:
                    buffer.appendBytes(((MomoProto.Service) momoCommand.body).toByteArray());
                    break;
                case GET_SERVICE_REPLY:
                    buffer.appendBytes(((MomoProto.ServiceReply) momoCommand.body).toByteArray());
                    break;
                default:
                    throw new IllegalAccessError("Command Type is not defined.");
            }
        }
        return buffer;
    }

//    public static void print(byte[] arr) {
//        System.out.println();
//        for (byte i : arr) {
//            System.out.print(i + ",")
//        }
//        System.out.println();
//    }

    public static int CMD_BODY_INDEX =  16;
    public static MomoCommand fromBuffer(Buffer buffer) throws InvalidProtocolBufferException {
        MomoCommand result = new MomoCommand();
        result.id = buffer.getLong(0);
        result.commandType = CommandType.valueOf(buffer.getInt(8));
        result.phone = buffer.getInt(12);

        switch (result.commandType) {
            case ERROR:
                result.body = CmdModels.Error.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case SEND_OTP:
                result.body = CmdModels.SendOtp.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case SEND_OTP_REPLY:
                result.body = CmdModels.SendOtpReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case VERIFY_OTP:
                result.body = CmdModels.VerifyOtp.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case VERIFY_OTP_REPLY:
                result.body = CmdModels.VerifyOtpReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case REGISTER:
                result.body = CmdModels.Register.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case REGISTER_REPLY:
                result.body = CmdModels.RegisterReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_AGENT_INFO:
                result.body = CmdModels.GetAgentInfo.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_AGENT_INFO_REPLY:
                result.body = CmdModels.GetAgentInfoReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case IS_PIN_CORRECT:
                result.body = CmdModels.IsPinCorrect.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case IS_PIN_CORRECT_REPLY:
                result.body = CmdModels.IsPinCorrectReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case CHANGE_PIN:
                result.body = CmdModels.ChangePin.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case CHANGE_PIN_REPLY:
                result.body = CmdModels.ChangePinReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case MODIFY_ARTICLE:
                result.body = CmdModels.ModifyArticle.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case MODIFY_ARTICLE_REPLY:
                result.body = CmdModels.ModifyArticleReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_ARTICLE_PAGE:
                result.body = CmdModels.GetArticlePage.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_ARTICLE_PAGE_REPLY:
                result.body = CmdModels.GetArticlePageReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case MODIFY_BANK_ACCOUNT:
                result.body = CmdModels.ModifyBankAccount.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case MODIFY_BANK_ACCOUNT_REPLY:
                result.body = CmdModels.ModifyBankAccountReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_AGENT_BANK_ACCOUNTS:
                result.body = CmdModels.GetAgentBankAccounts.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_AGENT_BANK_ACCOUNTS_REPLY:
                result.body = CmdModels.GetAgentBankAccountsReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_TRANSACTION:
                result.body = CmdModels.GetTransaction.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_TRANSACTION_REPLY:
                result.body = CmdModels.GetTransactionReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_TRANSACTION_DETAIL:
                result.body = CmdModels.GetTransactionDetail.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_TRANSACTION_DETAIL_REPLY:
                result.body = CmdModels.GetTransactionDetailReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TOPUP:
                result.body = CmdModels.Topup.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TOPUP_REPLY:
                result.body = CmdModels.TopupReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TOPUP_GAME:
                result.body = CmdModels.TopupGame.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TOPUP_GAME_REPLY:
                result.body = CmdModels.TopupGameReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TRANSFER_M2M:
                result.body = CmdModels.TransferM2m.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TRANSFER_M2M_REPLY:
                result.body = CmdModels.TransferM2mReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TRANSFER_M2C:
                result.body = CmdModels.TransferM2c.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case TRANSFER_M2C_REPLY:
                result.body = CmdModels.TransferM2cReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_BILL_INFO:
                result.body = CmdModels.GetBillInfo.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_BILL_INFO_REPLY:
                result.body = CmdModels.GetBillInfoReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case PAY_BILL:
                result.body = CmdModels.PayBill.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case PAY_BILL_REPLY:
                result.body = CmdModels.PayBillReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_IN:
                result.body = CmdModels.BankIn.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_IN_REPLY:
                result.body = CmdModels.BankInReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_OUT:
                result.body = CmdModels.BankOut.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_OUT_REPLY:
                result.body = CmdModels.BankOutReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_NET_TO_MOMO:
                result.body = CmdModels.BanknetToMomo.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_NET_TO_MOMO_REPLY:
                result.body = CmdModels.BanknetToMomoReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case VERIFY_BANKNET_OTP:
                result.body = CmdModels.VerifyBanknetOtp.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case VERIFY_BANKNET_OTP_REPLY:
                result.body = CmdModels.VerifyBanknetOtpReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_STORE_AROUND:
                result.body = CmdModels.GetStoreAround.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_STORE_AROUND_REPLY:
                result.body = CmdModels.GetStoreAroundReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case KEY_VALUE_DATA:
                result.body = CmdModels.KeyValueData.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case KEY_VALUE_DATA_REPLY:
                result.body = CmdModels.KeyValueDataReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_STORE_BY_CODE:
                result.body = CmdModels.GetStoreByCode.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_STORE_BY_CODE_REPLY:
                result.body = CmdModels.GetStoreByCodeReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case PAY_123MUA_ORDER:
                result.body = CmdModels.Pay123MuaOrder.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case PAY_123MUA_ORDER_REPLY:
                result.body = CmdModels.Pay123MuaOrderReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case UPDATE_AGENT_INFO:
                result.body = CmdModels.UpdateAgentInfo.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case UPDATE_AGENT_INFO_REPLY:
                result.body = CmdModels.UpdateAgentInfoReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case DEPOSIT_WITHDRAW_AT_PLACE:
                result.body = CmdModels.DepositWithdrawAtPlace.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case DEPOSIT_WITHDRAW_AT_PLACE_REPLY:
                result.body = CmdModels.DepositWithdrawAtPlaceReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_AVATAR_UPLOAD_TOKEN:
                result.body = CmdModels.GetAvatarUploadToken.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_AVATAR_UPLOAD_TOKEN_REPLY:
                result.body = CmdModels.GetAvatarUploadTokenReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case SAVE_BILL:
                result.body = CmdModels.SaveBill.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case SAVE_BILL_REPLY:
                result.body = CmdModels.SaveBillReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_SAVED_BILL:
                result.body = CmdModels.GetSavedBill.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_SAVED_BILL_REPLY:
                result.body = CmdModels.GetSavedBillReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case REMOVE_SAVED_BILL:
                result.body = CmdModels.RemoveSavedBill.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case REMOVE_SAVED_BILL_REPLY:
                result.body = CmdModels.RemoveSavedBillReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_TRANSACTION_FEE:
                result.body = CmdModels.GetTransactionFee.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_TRANSACTION_FEE_REPLY:
                result.body = CmdModels.GetTransactionFeeReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case PAY_AIRLINE_TICKET:
                result.body = CmdModels.PayAirlineTicket.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case PAY_AIRLINE_TICKET_REPLY:
                result.body = CmdModels.PayAirlineTicketReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case SEND_SMS:
                result.body = CmdModels.SendSms.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case SEND_SMS_REPLY:
                result.body = CmdModels.SendSmsReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case WITHDRAW_BY_AGENT:
                result.body = CmdModels.WithdrawByAgent.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case WITHDRAW_BY_AGENT_REPLY:
                result.body = CmdModels.WithdrawByAgentReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_OUT_MANUAL:
                result.body = CmdModels.BankOutManual.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case BANK_OUT_MANUAL_REPLY:
                result.body = CmdModels.BankOutManualReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case IS_MOMOER:
                result.body = CmdModels.IsMomoer.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case IS_MOMOER_REPLY:
                result.body = CmdModels.IsMomoerReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case COUNT_AGENT_TRAN:
                result.body = CmdModels.CountAgentTran.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case COUNT_AGENT_TRAN_REPLY:
                result.body = CmdModels.CountAgentTranReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case MAKE_TRAN:
                result.body = MomoProto.TranHisV1.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case MAKE_TRAN_REPLY:
                result.body = CmdModels.MakeTranReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_SERVICE_LAYOUT:
                result.body = MomoProto.GetServiceLayout.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_SERVICE_LAYOUT_REPLY:
                result.body = MomoProto.GetServiceLayoutReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case COMPLETE_TRAN:
                result.body = CmdModels.CompeleteTran.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case COMPLETE_TRAN_REPLY:
                result.body = MomoProto.TranHisV1.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case DO_TRAN:
                result.body = MomoProto.TranHisV1.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case DO_TRAN_REPLY:
                result.body = MomoProto.TranHisV1.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_SERVICE:
                result.body = MomoProto.Service.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            case GET_SERVICE_REPLY:
                result.body = MomoProto.ServiceReply.parseFrom(buffer.getBytes(CMD_BODY_INDEX, buffer.length()));
                break;
            default:
                throw new IllegalAccessError("Command Type is not defined.");
        }
        return result;
    }

    @Override
    public String toString() {
        if (body == null) {
            return String.format("MomoCommand{%d, %s, NULL}", id, commandType);
        }
        return String.format("MomoCommand{%d, %d, %s, %s{%s}}", phone, id, commandType, body.getClass().getSimpleName(), body);
    }
    
    public static void main(String[] args) {
		MomoCommand cmd = new MomoCommand(CommandType.GET_STORE_BY_CODE, CmdModels.GetStoreByCode.newBuilder().setPageSize(1).setPageNum(3).setCid(3).setDid(3).build());
		cmd.setId(1234567890);
        cmd.setPhone(987568815);

        System.out.println(cmd.body);
		try {
			System.out.println(MomoCommand.fromBuffer(MomoCommand.toBuffer(cmd)));
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
