package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 8/1/14.
 */
public class ModelMapper {

    /**
     *
     public static String COMMAND_INDEX ="cmdId";
     public static String TRAN_ID ="tranId";
     public static String CLIENT_TIME ="ctime";
     public static String ACK_TIME ="atime";
     public static String FINISH_TIME ="ftime";
     public static String TRAN_TYPE ="tranType";
     public static String IO ="io";
     public static String CATEGORY ="cat";
     public static String PARTNER_ID ="pid";
     public static String PARTNER_CODE ="pcode";
     public static String PARTNER_NAME ="pname";
     public static String PARTNER_REF ="pref";
     public static String BILL_ID ="billId";
     public static String AMOUNT ="amt";
     public static String COMMENT ="cmt";
     public static String STATUS ="status";
     public static String OWNER_NUMBER ="number";
     public static String OWNER_NAME ="name";
     public static String PARRENT_TRAN_TYPE ="parenttranType";
     public static String ERROR ="error";
     public static String BALANCE = "balance";
     public static String FROM_SOURCE ="fromsrc";
     public static String IS_M2NUMBER ="ism2number";
     //extra info
     public static String DELETED ="del";
     public static String TABLE_PREFIX ="tran_";

     //doi soat voi ben thu 3
     public static String PARTNER_INVOICE_NO ="partnerInvNo";
     public static String PARTNER_TICKET_CODE ="partnerTicketCode";
     public static String PARTNER_ERROR ="partnerError";
     public static String PARTNER_DESCRIPTION ="partnerDesc";
     public static String PARTNER_ACTION ="partnerAction";
     */

    public static MomoProto.TranHisV1 toTranHisV1(JsonObject json) {
        Long tranId  = json.getLong(colName.TranDBCols.TRAN_ID);
        Long client_time   = json.getLong(colName.TranDBCols.CLIENT_TIME);
        Long ackTime   = json.getLong(colName.TranDBCols.ACK_TIME);
        Long finishTime   = json.getLong(colName.TranDBCols.FINISH_TIME);
        Integer tranType = json.getInteger(colName.TranDBCols.TRAN_TYPE);
        Integer io = json.getInteger(colName.TranDBCols.IO);
        Integer category = json.getInteger(colName.TranDBCols.CATEGORY);
        String partnerId = json.getString(colName.TranDBCols.PARTNER_ID);
        String partnerCode  = json.getString(colName.TranDBCols.PARTNER_CODE);
        String partnerName  = json.getString(colName.TranDBCols.PARTNER_NAME);
        String partner_ref  = json.getString(colName.TranDBCols.PARTNER_REF);
        String billId  = json.getString(colName.TranDBCols.BILL_ID);
        Long amount = json.getLong(colName.TranDBCols.AMOUNT);
        String comment  = json.getString(colName.TranDBCols.COMMENT);
        Integer status = json.getInteger(colName.TranDBCols.STATUS);
        Integer error = json.getInteger(colName.TranDBCols.ERROR);
        Long command_Ind = json.getLong(colName.TranDBCols.COMMAND_INDEX);
        Integer source_from = json.getInteger(colName.TranDBCols.FROM_SOURCE);
        String partner_extra_1   = json.getString(colName.TranDBCols.PARTNER_EXTRA_1);

        MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();

        if (tranId != null) builder.setTranId(tranId);
        if (client_time != null) builder.setClientTime(client_time);
        if (ackTime != null) builder.setAckTime(ackTime);
        if (finishTime != null) builder.setFinishTime(finishTime);
        if (tranType != null) builder.setTranType(tranType);
        if (io != null) builder.setIo(io);
        if (category != null) builder.setCategory(category);
        if (partnerId != null) builder.setPartnerId(partnerId);
        if (partnerCode != null) builder.setPartnerCode(partnerCode);
        if (partnerName != null) builder.setPartnerName(partnerName);
        if (partner_ref != null) builder.setPartnerRef(partner_ref);
        if (billId != null) builder.setBillId(billId);
        if (amount != null) builder.setAmount(amount);
        if (status != null) builder.setStatus(status);
        if (error != null) builder.setError(error);
        if (command_Ind != null) builder.setCommandInd(command_Ind);
        if (source_from != null) builder.setSourceFrom(source_from);
        if (partner_extra_1 != null) builder.setSourceFrom(source_from);

        return builder.build();
    }
}
