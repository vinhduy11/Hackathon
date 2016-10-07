package com.mservice.momo.vertx.processor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.*;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.DBMsg;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.web.ServicePackageDb;
import com.mservice.momo.gateway.internal.connectorproxy.ViaConnectorObj;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.objects.Response;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.gateway.internal.soapin.SoapVerticle;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.msg.TranTypeExt;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.JacksonJSONUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.context.TransferWithGiftContext;
import com.mservice.momo.vertx.customercare.PromoContentNotification;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.form.Command;
import com.mservice.momo.vertx.form.FieldData;
import com.mservice.momo.vertx.form.FieldItem;
import com.mservice.momo.vertx.form.ReplyObj;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.retailer.FeeObj;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.mail.HtmlEmail;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by concu on 9/23/14.
 */
public class Misc {
    public static String BeginDateInMilliSec = "begindateinmillisec";
    public static String EndDateInMilliSec = "enddateinmillisec";
    //th lis of service that we have to refine billid before process
    private static ArrayList<String> checkedList = new ArrayList<>();
    private static byte[] secret;

    static {
        checkedList.add("dien");
        checkedList.add("evnhcm");
    }

    static {
        secret = new byte[]{118,
                96,
                -122,
                -122,
                -121,
                8,
                119,
                104,
                118,
                118,
                119,
                103,
                103,
                96,
                119,
                -121,
                118,
                96,
                -122,
                -122,
                -121,
                8,
                119,
                104
        };
    }

//    public static synchronized Cash calculateAmount(long amount
//            , long static_fee
//            , double dynamic_fee
//            , int lockedType
//            , int tranType, Common.BuildLog log){
//
//        log.add("function","calculateAmount");
//        log.add("amount", amount);
//        log.add("static fee", static_fee);
//        log.add("dynamic fee", dynamic_fee);
//        log.add("lockedType", lockedType);
//        log.add("tran type", MomoProto.TranHisV1.TranType.valueOf(tranType).name());
//
//
//        long bankNetAmountLocked;
//        long coreAmountAdjust;
//        long allFee =0;
//        if(tranType == MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE){
//
//            //Y = (X+1.100)/ (1-1.1%)
//
//            bankNetAmountLocked = (long) (Math.ceil( ((amount + static_fee)*100)
//                                                /
//                                               (100-dynamic_fee)));
//
//            coreAmountAdjust = amount;
//
//        }else {
//            allFee = static_fee + BigDecimal.valueOf(dynamic_fee).multiply(BigDecimal.valueOf(amount / 100)).longValue();
//
//            if(lockedType == MomoProto.CardItem.LockedType.FULL_VALUE){
//
////          bankNetAmountLocked = (long) (((amount + static_fee)*100)/(100-dynamic_fee));
//                bankNetAmountLocked = amount + allFee;
//                coreAmountAdjust = amount;
//            }else {https://www.youtube.com/wat
//                bankNetAmountLocked = amount;
////          coreAmountAdjust = (long) (amount * (100 - dynamic_fee) / 100) - static_fee;
//                coreAmountAdjust = amount - allFee;
//            }
//        }
//
//        log.add("bankNetAmountLocked", bankNetAmountLocked);
//        log.add("coreAmountAdjust", coreAmountAdjust);
//
//        log.add("end", "-----------");
//
//        return new Cash(bankNetAmountLocked,coreAmountAdjust);
//    }

    public static JsonObject getStartAndEndCurrentDateInMilliseconds() {
        Calendar c = Calendar.getInstance();

        Calendar beginDate = new GregorianCalendar(c.get(Calendar.YEAR)
                , c.get(Calendar.MONTH)
                , c.get(Calendar.DAY_OF_MONTH)
                , 0, 0, 0);
        Calendar endDate = new GregorianCalendar(c.get(Calendar.YEAR)
                , c.get(Calendar.MONTH)
                , c.get(Calendar.DAY_OF_MONTH)
                , 23, 59, 59);

        JsonObject jo = new JsonObject();
        jo.putNumber(BeginDateInMilliSec, beginDate.getTimeInMillis());
        jo.putNumber(EndDateInMilliSec, endDate.getTimeInMillis());
        return jo;
    }

    public static void sendStandardReply(NetSocket sock
            , MomoMessage msg
            , int momoMsgType
            , String desc
            , int rcode
            , Common mCommon) {

        boolean result = rcode == 0 ? true : false;

        Buffer buffer = MomoMessage.buildBuffer(momoMsgType
                , msg.cmdIndex
                , msg.cmdPhone
                , MomoProto.StandardReply.newBuilder()
                        .setDesc(desc)
                        .setRcode(rcode)
                        .setResult(result)
                        .build().toByteArray());

        mCommon.writeDataToSocket(sock, buffer);
    }

    public static synchronized Cash calculateAmount(long amount
            , long static_fee
            , double dynamic_fee
            , int lockedType
            , int tranType, Common.BuildLog log) {

        log.add("function", "calculateAmount");
        log.add("amount", amount);
        log.add("static fee", static_fee);
        log.add("dynamic fee", dynamic_fee);
        log.add("lockedType", lockedType);
        //log.add("tran type", MomoProto.TranHisV1.TranType.valueOf(tranType).name());


        long bankNetAmountLocked;
        long coreAmountAdjust;
        long allFee = 0;
        if (tranType == MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE ||
                tranType == MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE ||
                tranType == MomoProto.TranHisV1.TranType.BANK_IN_VALUE) {

            //Y = (X+1.100)/ (1-1.1%)

//            bankNetAmountLocked = (long) (Math.ceil( ((amount + static_fee)*100)
//                    /
//                    (100-dynamic_fee)));
//
//            coreAmountAdjust = amount;
            bankNetAmountLocked = (long) (amount + static_fee + Math.ceil(0.01 * dynamic_fee * amount));
            //bankNetAmountLocked = (long)Math.ceil(amount + Double.parseDouble(String.valueOf(static_fee + dynamic_fee *amount / 100)));
            coreAmountAdjust = amount;
        } else {
            //allFee = static_fee + BigDecimal.valueOf(dynamic_fee).multiply(BigDecimal.valueOf(amount / 100)).longValue();
            allFee = (long) (static_fee + Math.round(0.01 * dynamic_fee * amount));
            //allFee = (long) (0 + Math.ceil(0.01 * 0.3 * 2087958));
            //allFee = (long)Math.ceil(Double.parseDouble(String.valueOf(static_fee + dynamic_fee *amount / 100)));

            if (lockedType == MomoProto.CardItem.LockedType.FULL_VALUE) {

//          bankNetAmountLocked = (long) (((amount + static_fee)*100)/(100-dynamic_fee));
                bankNetAmountLocked = amount + allFee;
                coreAmountAdjust = amount;
            } else {
                https:
//www.youtube.com/wat
                bankNetAmountLocked = amount;
//          coreAmountAdjust = (long) (amount * (100 - dynamic_fee) / 100) - static_fee;
                coreAmountAdjust = amount - allFee;
            }
        }

        log.add("bankNetAmountLocked", bankNetAmountLocked);
        log.add("coreAmountAdjust", coreAmountAdjust);

        log.add("end", "-----------");

        return new Cash(bankNetAmountLocked, coreAmountAdjust);
    }

    public static void main(String[] args) {
        //(long) Math.ceil(Double.parseDouble(String.valueOf((staticFee + (dynamicFee / 100) * transAmount))));
//        long allFee = (long)Math.ceil(Double.parseDouble(String.valueOf(1100 + 1.1 *693000 / 100)));
        long allFee = (long) (0 + Math.round(0.01 * 0.3 * 3500080));
        Math.round(2.1);
        Math.round(2.6);
    }

    public static TranObj cloneTran(TranObj srcTranObj) {

        TranObj t = new TranObj();
        t.cmdId = srcTranObj.cmdId; // command index, cCID
        t.tranId = srcTranObj.tranId; // return from core doing transaction, cTID
        t.clientTime = srcTranObj.clientTime; // time at calling webservice
        t.ackTime = srcTranObj.ackTime; // ack time from server to client
        t.finishTime = srcTranObj.finishTime; // the time that server response result.
        t.tranType = srcTranObj.tranType; //ex : MomoProto.MsgType.BANK_IN_VALUE
        t.io = srcTranObj.io; // direction of transaction.
        t.category = srcTranObj.category; // type of transfer, chuyen tien ve que, cho vay, khac
        t.partnerId = srcTranObj.partnerId; // ban hang cua minh : providerId ..
        t.parterCode = srcTranObj.parterCode; // ma doi tac
        t.partnerName = srcTranObj.partnerName; // ten doi tac
        t.partnerRef = srcTranObj.partnerRef;
        t.billId = srcTranObj.billId; // for invoice, billID
        t.amount = srcTranObj.amount; // gia tri giao dich
        t.comment = srcTranObj.comment; // ghi chu
        t.status = srcTranObj.status; // trang thai giao dich
        t.error = srcTranObj.error; // ma loi giao dich
        t.owner_name = srcTranObj.owner_name; // giao dich duoc thuc hien tu ai
        t.owner_number = srcTranObj.owner_number; // giao dich duoc thuc hien tu so dien thoai nao
        t.parrent_tranType = srcTranObj.parrent_tranType; // giao dich tong ben ngoai
        t.source_from = srcTranObj.source_from;
        return t;
    }

    public static String getDate(long time) {
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm dd/MM/yyyy");
        return formater.format(new Date(time));
    }

    public static String getDateWithFormat(long time, String format) {
        SimpleDateFormat formater = new SimpleDateFormat(format);
        return formater.format(new Date(time));
    }

    public static String getNumberBus(int number) {
        return "momo.number." + String.valueOf(number);
    }

    public static Buffer buildTranHisReply(final MomoMessage requestMsg
            , final MomoProto.TranHisV1 request
            , final JsonObject transReply
            , final Common.BuildLog log
    ) {

        int tranType = request.getTranType();
        if (tranType == MomoProto.TranHisV1.TranType.M2C_VALUE) {
            tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
        }

        //neu la giao dich cho Escap thi map lai thanh Pay_one_bill
        if (tranType == TranTypeExt.Escape
                || tranType == TranTypeExt.CapDoiHoanHao
                || tranType == TranTypeExt.Fsc2014) {
            tranType = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
        }

        //final String vtelTarget = TopupMapping.getTargetTopup(customerPhone, "", log);
        log.add("function", "buildTranHisReply");
        log.add("tranid", transReply.getLong(colName.TranDBCols.TRAN_ID));
        log.add("client time", request.getClientTime());

        log.add("bill id", request.getBillId() == null ? "" : request.getBillId());
        log.add("amount", transReply.getLong(colName.TranDBCols.AMOUNT, 0));
        log.add("comment", transReply.getString(colName.TranDBCols.COMMENT, ""));
        log.add("cmd index", requestMsg.cmdIndex);
        log.add("status", transReply.getInteger(colName.TranDBCols.STATUS));
        log.add("desc", transReply.getString(colName.TranDBCols.DESCRIPTION, ""));

        //build shared field from kvps that client sent to server
        JsonArray shared = transReply.getArray(colName.TranDBCols.SHARE, new JsonArray());
        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(request.getKvpList());
        for (String s : hashMap.keySet()) {
            JsonObject o = new JsonObject();
            o.putString(s, hashMap.get(s));
            shared.add(o);
        }

        //refine shared value for all
        if (transReply.containsField(colName.TranDBCols.SHARE)) {
            transReply.removeField(colName.TranDBCols.SHARE);
        }
        transReply.putArray(colName.TranDBCols.SHARE, shared);


        log.add("shared", shared.toString());
        String forceComment = transReply.getString(colName.TranDBCols.FORCE_COMMENT, "");
        forceComment = ("".equalsIgnoreCase(forceComment) ? transReply.getString(colName.TranDBCols.COMMENT, "") : forceComment);
        String fcmt = ("".equalsIgnoreCase(forceComment) ? "0" : "1");
        int status = transReply.getInteger(colName.TranDBCols.STATUS) == null ? transReply.getInteger(colName.TranDBCols.ERROR, -1) == 0 ? 4 : 5 : transReply.getInteger(colName.TranDBCols.STATUS);
        log.add("status again ", status);
        log.writeLog();

        MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder()
                .setTranId(transReply.getLong(colName.TranDBCols.TRAN_ID, 0))
                .setClientTime(request.getClientTime())
                .setAckTime(transReply.getLong(colName.TranDBCols.ACK_TIME, 0))
                .setFinishTime(transReply.getLong(colName.TranDBCols.FINISH_TIME, 0))
                .setTranType(tranType)
                .setIo(transReply.getInteger(colName.TranDBCols.IO, 0))
                .setCategory(request.getCategory())
                .setPartnerId(transReply.getString(colName.TranDBCols.PARTNER_ID, ""))
                .setPartnerCode(transReply.getString(colName.TranDBCols.PARTNER_CODE, ""))
                .setPartnerName(transReply.getString(colName.TranDBCols.PARTNER_NAME, ""))
                .setPartnerRef(request.getPartnerRef() == null ? "" : request.getPartnerRef())
                .setBillId(transReply.getString(colName.TranDBCols.BILL_ID, ""))
                .setAmount(transReply.getLong(colName.TranDBCols.AMOUNT, 0))
                .setComment(forceComment)
                .setCommandInd(requestMsg.cmdIndex)
                .setStatus(status)
                .setError(transReply.getInteger(colName.TranDBCols.ERROR))
                .setSourceFrom(transReply.getInteger(colName.TranDBCols.FROM_SOURCE, request.getSourceFrom()))
                .setDesc(transReply.getString(colName.TranDBCols.DESCRIPTION, ""))
                .setShare(shared.toString());
        builder.addKvp(MomoProto.TextValue.newBuilder().setText("fcmt").setValue(fcmt));

        //Them builder
        if (transReply.getInteger(colName.TranDBCols.ERROR, 1) == 0) {
            builder.addKvp(MomoProto.TextValue.newBuilder().setText("htmlpopup").setValue(String.format(StringConstUtil.M2M_HTML_PASS)));
        } else {
            builder.addKvp(MomoProto.TextValue.newBuilder().setText("htmlpopup").setValue(String.format(StringConstUtil.M2M_HTML_FAIL)));
        }
        //add desc
        if (transReply.getInteger(colName.TranDBCols.ERROR, 0) == MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE) {
            builder.setDesc(transReply.getString(colName.TranDBCols.DESCRIPTION, ""));
        }

        long fee = transReply.containsField(Const.AppClient.Fee) ? transReply.getLong(Const.AppClient.Fee) : 0;
        builder.addKvp(MomoProto.TextValue.newBuilder().setText(Const.AppClient.Fee).setValue(String.valueOf(fee)));

        JsonObject kvp = transReply.getObject("kvp");
        if (kvp != null) {
            Map<String, Object> map = kvp.toMap();

            for (String fieldName : map.keySet()) {
                MomoProto.TextValue.Builder tv = MomoProto.TextValue.newBuilder();
                tv.setText(fieldName);
                tv.setValue(String.valueOf(map.get(fieldName)));
                builder.addKvp(tv);
            }
        }

        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.TRANS_REPLY_VALUE,
                requestMsg.cmdIndex,
                requestMsg.cmdPhone,
                builder.build().toByteArray()
        );

        return buf;
    }

    public static boolean checkNumber(int number) {
        return (800000000 <= number && number <= 1999999999);
    }

    public static String removeAccent(String str) {

        if (str == null || str.isEmpty()) return "";
        String[] signs = new String[]{
                "aAeEoOuUiIdDyY",
                "áàạảãâấầậẩẫăắằặẳẵ",
                "ÁÀẠẢÃÂẤẦẬẨẪĂẮẰẶẲẴ",
                "éèẹẻẽêếềệểễ",
                "ÉÈẸẺẼÊẾỀỆỂỄ",
                "óòọỏõôốồộổỗơớờợởỡ",
                "ÓÒỌỎÕÔỐỒỘỔỖƠỚỜỢỞỠ",
                "úùụủũưứừựửữ",
                "ÚÙỤỦŨƯỨỪỰỬỮ",
                "íìịỉĩ",
                "ÍÌỊỈĨ",
                "đ",
                "Đ",
                "ýỳỵỷỹ",
                "ÝỲỴỶỸ"
        };

        for (int i = 1; i < signs.length; i++) {
            for (int j = 0; j < signs[i].length(); j++) {
                str = str.replace(signs[i].charAt(j), signs[0].charAt(i - 1));
            }
        }
        return str.toUpperCase();
    }

    public static String removeAccentWithoutUpper(String str) {

        if (str == null || str.isEmpty()) return "";
        String[] signs = new String[]{
                "aAeEoOuUiIdDyY",
                "áàạảãâấầậẩẫăắằặẳẵ",
                "ÁÀẠẢÃÂẤẦẬẨẪĂẮẰẶẲẴ",
                "éèẹẻẽêếềệểễ",
                "ÉÈẸẺẼÊẾỀỆỂỄ",
                "óòọỏõôốồộổỗơớờợởỡ",
                "ÓÒỌỎÕÔỐỒỘỔỖƠỚỜỢỞỠ",
                "úùụủũưứừựửữ",
                "ÚÙỤỦŨƯỨỪỰỬỮ",
                "íìịỉĩ",
                "ÍÌỊỈĨ",
                "đ",
                "Đ",
                "ýỳỵỷỹ",
                "ÝỲỴỶỸ"
        };

        for (int i = 1; i < signs.length; i++) {
            for (int j = 0; j < signs[i].length(); j++) {
                str = str.replace(signs[i].charAt(j), signs[0].charAt(i - 1));
            }
        }
        return str;
    }

    public static JsonObject getJsonObjRpl(int err_code
            , long tranId
            , long amount
            , int in_out) {

        JsonObject newObj = new JsonObject();
        newObj.putNumber(colName.TranDBCols.STATUS, (err_code == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL));
        newObj.putNumber(colName.TranDBCols.ERROR, err_code);
        newObj.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        newObj.putNumber(colName.TranDBCols.AMOUNT, amount);
        newObj.putNumber(colName.TranDBCols.IO, in_out);
        newObj.putNumber(colName.TranDBCols.FINISH_TIME, System.currentTimeMillis());

        return newObj;
    }

    public static JsonObject getJsonObjRplWithFee(int err_code
            , long tranId
            , long amount
            , long fee
            , int in_out) {

        JsonObject newObj = new JsonObject();
        JsonObject jsonKvp = new JsonObject();
        newObj.putNumber(colName.TranDBCols.STATUS, (err_code == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL));
        newObj.putNumber(colName.TranDBCols.ERROR, err_code);
        newObj.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        newObj.putNumber(colName.TranDBCols.AMOUNT, amount);
        newObj.putNumber(colName.TranDBCols.IO, in_out);
        newObj.putNumber(colName.TranDBCols.FINISH_TIME, System.currentTimeMillis());
        jsonKvp.putNumber(Const.AppClient.Fee, fee);
        newObj.putObject(colName.TranDBCols.KVP, jsonKvp);

        return newObj;
    }

    public static void addErrDescAndComment(JsonObject joTranReply, String errDesc, String forceComment) {

        if (errDesc != null || !"".equalsIgnoreCase(errDesc)) {
            joTranReply.putString(colName.TranDBCols.DESCRIPTION, errDesc);
            joTranReply.putNumber(colName.TranDBCols.ERROR, MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE);
            joTranReply.putNumber(colName.TranDBCols.STATUS, TranObj.STATUS_FAIL);
        }
        if (forceComment != null && !"".equalsIgnoreCase(forceComment)) {
            joTranReply.putString(colName.TranDBCols.FORCE_COMMENT, forceComment);
        }
    }

    public static void addCustomNumber(JsonObject jsonObjRpl, String customNumber) {

        if (customNumber != null && !"".equalsIgnoreCase(customNumber) && !jsonObjRpl.containsField(Const.DGD.CusNumber)) {
            jsonObjRpl.putString(Const.DGD.CusNumber, customNumber);
        }
    }

    public static JsonObject getJsonObjRplProcessing(int err_code
            , long tranId
            , long amount
            , int in_out) {

        JsonObject newObj = getJsonObjRpl(err_code, tranId, amount, in_out);
        if (err_code == 0)
            newObj.putNumber(colName.TranDBCols.STATUS, TranObj.STATUS_PROCESS);

        return newObj;
    }

    public static long getDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance();
        //c.set(year,month-1,day,hour,minute,second);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime().getTime();
    }

    public static String formatAmount(long amount) {

        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount);
    }

    public static Buffer buildBufferInvalid(MomoProto.TranHisV1 request, MomoMessage msg, int whatError) {

        return MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REPLY_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , MomoProto.TranHisV1.newBuilder()
                        .setError(whatError)
                        .setTranType(request.getTranType())
                        .setStatus(TranObj.STATUS_FAIL)
                        .build().toByteArray()
        );
    }

    public static Buffer buildSessionExpired(MomoMessage msg) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException ex) {
            request = null;
        }

        return MomoMessage.buildBuffer(MomoProto.MsgType.TRANS_REPLY_VALUE
                , msg.cmdIndex
                , msg.cmdPhone
                , MomoProto.TranHisV1.newBuilder()
                        .setError(MomoProto.TranHisV1.ResultCode.SESSION_EXPIRED_VALUE)
                        .setTranType(request.getTranType())
                        .setStatus(TranObj.STATUS_FAIL)
                        .build()
                        .toByteArray()
        );
    }

    /**
     * Dinh dang ngay theo format dd/MM/yyyy
     *
     * @param date
     * @return string with format dd/MM/yyyy
     */
    public static String dateVNFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(date);
    }

    /**
     * dinh dang ngay theo format dd/MM/yyyy
     *
     * @param millisecs
     * @return string with format dd/MM/yyyy
     */
    public static String dateVNFormat(long millisecs) {
        Date date = new Date(millisecs);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(date);
    }

    public static String dateVNFormatWithDot(long millisecs) {
        Date date = new Date(millisecs);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = sdf.format(date);
        return strDate.replace("-", ".");
    }

    public static String dateMMYYYY(long millisecs) {
        Date date = new Date(millisecs);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
        return sdf.format(date);
    }

    public static String dateFormatWithParten(long millisecs, String parten) {
        Date date = new Date(millisecs);
        SimpleDateFormat sdf = new SimpleDateFormat(parten);
        return sdf.format(date);
    }

    public static String dateVNFormatWithTime(long millisecs) {
        Date date = new Date(millisecs);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * @param ddMMyyyyVN
     * @return dinh dang ngay thang theo dd/MM/yyyy
     */
    public static long str2Date(String ddMMyyyyVN) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date;
        try {
            date = formatter.parse(ddMMyyyyVN);
        } catch (ParseException e) {
            return 0;
        }

        return date.getTime();
    }

    /**
     * @param ddMMyyyyVN
     * @return lay thoi gian dau ngay
     */
    public static long str2BeginDate(String ddMMyyyyVN) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date;
        try {
            date = formatter.parse(ddMMyyyyVN);

        } catch (ParseException e) {
            return 0;
        }
        return date.getTime();
    }

    /**
     * @param ddMMyyyyVN
     * @return lay thoi gian cuoi ngay
     */
    public static long str2EndDate(String ddMMyyyyVN) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date;
        try {
            date = formatter.parse(ddMMyyyyVN);

        } catch (ParseException e) {
            return 0;
        }

        return (date.getTime() + (23 * 60 * 60 * 1000) + (59 * 60 * 1000) + (59 * 1000));
    }

    public static void sendNoti(Vertx _vertx, Notification noti) {

        _vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                , noti.toFullJsonObject()
                , new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                    }
                });
    }

    public static void sendNotiFromSDKServer(Vertx _vertx, Notification noti) {

        _vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_SDK_SERVER
                , noti.toFullJsonObject()
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
            }
        });
    }

    public static void sendNotiFromTool(Vertx _vertx, Notification noti) {

        _vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL
                , noti.toFullJsonObject()
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
            }
        });
    }

    public static void remindSendNoti(Vertx _vertx, Notification noti) {

        _vertx.eventBus().send(AppConstant.NotificationVerticle_REMIND_SEND_NOTI
                , noti.toFullJsonObject()
                , new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                    }
                });
    }

    public static void sendNotiFromToolRedis(Vertx _vertx, Notification noti) {

        _vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL_WITH_REDIS
                , noti.toFullJsonObject()
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
            }
        });
    }

    public static void sendNotiViaCloud(Vertx _vertx, Notification noti) {

        _vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION_VIA_CLOUD
                , noti.toFullJsonObject()
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
            }
        });
    }

    public static void sendPromotionPopupNoti(Vertx _vertx, Notification noti) {

        _vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION
                , noti.toFullJsonObject()
                , new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
            }
        });
    }

    public static ArrayList<ServicePackageDb.Obj> getPackageList(JsonArray joArr) {

        ArrayList<ServicePackageDb.Obj> arrayList = null;

        if (joArr == null || joArr.size() == 0) return arrayList;
        arrayList = new ArrayList<>();

        for (Object o : joArr) {
            JsonObject jo = (JsonObject) o;
            arrayList.add(new ServicePackageDb.Obj(jo));
        }
        return arrayList;
    }

    public static HashMap<String, ArrayList<FieldData>> getHasMapFieldData(ArrayList<FieldData> datas) {
        HashMap<String, ArrayList<FieldData>> hashMap = new HashMap<>();
        for (int i = 0; i < datas.size(); i++) {
            FieldData fd = datas.get(i);
            ArrayList<FieldData> array = hashMap.get(fd.linkto);
            if (array == null) {
                array = new ArrayList<>();
                array.add(fd);
                hashMap.put(fd.linkto, array);
            } else {
                array.add(fd);
                hashMap.put(fd.linkto, array);
            }
        }
        return hashMap;
    }

    public static String Date123Phim(String sdate) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {

            Date date = formatter.parse(sdate);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
            return sdf.format(date);

        } catch (ParseException e) {
            return sdate;
        }
    }

    public static long getDateAsLong(String sdate, String format, Logger logger, String ref) {

        if ("".equalsIgnoreCase(sdate)) return 0;

        long result = 0;
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        try {
            Date date = formatter.parse(sdate);
            result = date.getTime();

        } catch (ParseException e) {
            if (logger != null) {
                logger.info("getDateAsLong got loi " + e.getMessage());
                logger.info(ref);
            }
        }
        return result;
    }

    public static char getHash(String random, String checksum) {
        long total = 0;
        int hash = 0;
        for (int i = 0; i < random.length(); i++) {
            total += (int) random.charAt(i);
        }

        hash = (int) (total % 29);
        return checksum.charAt(hash);
    }

    public static HashMap<String, String> getKeyValuePairs(List<MomoProto.TextValue> keyValuePairs) {
        HashMap<String, String> extraData = new HashMap<>();
        if (keyValuePairs != null && keyValuePairs.size() > 0) {
            extraData = new HashMap<>();

            for (int i = 0; i < keyValuePairs.size(); i++) {

                extraData.put(keyValuePairs.get(i).getText()
                        , keyValuePairs.get(i).getValue());
            }
        }

        return extraData;
    }

    public static void getServiceInfo(Vertx _vertx
            , Common.ServiceReq serviceReq
            , final Handler<JsonArray> callback) {

        _vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                callback.handle(message.body());
            }
        });
    }

    public static long getRefundFee(long sDate, long eDate, long fee, int number, Logger logger) {

        long curTime = System.currentTimeMillis();
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + number);
        log.add("function", "getRefundFee");
        log.add("start date", dateVNFormatWithTime(sDate));
        log.add("current date", dateVNFormatWithTime(curTime));
        log.add("end date", dateVNFormatWithTime(eDate));

        if ((sDate <= curTime) && (curTime <= eDate)) {
            log.add("fee", fee);
            log.writeLog();
            return fee;

        }
        log.add("fee", 0);
        log.writeLog();
        return 0;
    }

    public static long get123PayFee(long amount, double dynamicFee, long staticFee) {
        double fee = (staticFee + ((amount * dynamicFee) / 100)) / (1 - (dynamicFee / 100));
        return (long) Math.ceil(fee);
    }

    //ham dung them keyvaluepair
    public static ArrayList<SoapProto.keyValuePair> addKeyValuePair(ArrayList<SoapProto.keyValuePair> keyValuePairs
            , String key, String value) {

        if (keyValuePairs == null) {
            keyValuePairs = new ArrayList<>();
        }
        keyValuePairs.add(SoapProto.keyValuePair.newBuilder()
                .setKey(key)
                .setValue(value).build());
        return keyValuePairs;
    }

    public static int getYearMonth(long millisecs) {
        Date date = new Date(millisecs);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        return DataUtil.strToInt(sdf.format(date));
    }

    public static long calBonusValue(long totalValueCurMonth
            , long tranAmount
            , long maxValuePerMonth
            , int percent
            , long staticValue
            , Common.BuildLog log) {

        log.add("sum tran value", totalValueCurMonth);
        log.add("tran amount", tranAmount);
        log.add("max value per month", maxValuePerMonth);
        log.add("percent", percent);
        log.add("static value", staticValue);

        long valAfter = totalValueCurMonth + tranAmount;
        long valWillDo = 0;

        log.add("value after", valAfter);

        if (totalValueCurMonth < maxValuePerMonth) {
            if (valAfter <= maxValuePerMonth) {
                valWillDo = tranAmount;
            } else {
                long valExceeded = valAfter - maxValuePerMonth;
                valWillDo = tranAmount - valExceeded;
            }
        }

        log.add("value will canculate", valWillDo);

        long value = 0;
        if (valWillDo > 0) {
            value = (long) Math.ceil((valWillDo * percent) / 100) + staticValue;
        }
        log.add("value will get", value);

        return value;
    }

    public static TimeOfMonth getBeginEndTimeOfMonth(long time) {
        //dd/MM/yyyy
        String strDate = dateVNFormat(time);
        String[] ar = strDate.split("/");
        int year = DataUtil.strToInt(ar[2]);
        int month = DataUtil.strToInt(ar[1]);
        int date = DataUtil.strToInt(ar[0]);

        int endDate = 0;

        Calendar c = Calendar.getInstance();

        //ngay dau tien trong thang
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, 1);

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        long beginTime = c.getTimeInMillis();

        //ngay dau thang sau
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.DAY_OF_MONTH, -1);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 56);
        c.set(Calendar.SECOND, 59);
        long endTime = c.getTimeInMillis();

        TimeOfMonth timeOfMonth = new TimeOfMonth(beginTime, endTime);
        return timeOfMonth;
    }

    public static void adjustment(final Vertx _vertx
            , final String fromNumber
            , final String toNumber
            , final long amount
            , final int walletType
            , ArrayList<KeyValue> listKeyValue
            , final Common.BuildLog log, final Handler<Common.SoapObjReply> callback) {


        SoapProto.commonAdjust.Builder builder = SoapProto.commonAdjust.newBuilder();
        builder.setSource(fromNumber)
                .setTarget(toNumber)
                .setWalletType(walletType)
                .setAmount(amount)
                .setPhoneNumber(fromNumber)
                .setTime(log.getTime())
                .setDescription("");

        if (listKeyValue != null && listKeyValue.size() > 0) {
            for (int i = 0; i < listKeyValue.size(); i++) {
                builder.addExtraMap(SoapProto.keyValuePair.newBuilder()
                        .setKey(listKeyValue.get(i).Key)
                        .setValue(listKeyValue.get(i).Value));
            }
        }

        //buffer adjust
        Buffer buffer = MomoMessage.buildBuffer(
                SoapProto.MsgType.ADJUSTMENT_VALUE
                , System.currentTimeMillis()
                , DataUtil.strToInt(fromNumber)
                , builder.build().toByteArray());

        //day qua soap verticle
        _vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> msgReply) {

                Common.SoapObjReply soapObjReply = new Common.SoapObjReply(msgReply.body());
                callback.handle(soapObjReply);
            }
        });

    }

    //lay thong tin trang thai cua Agent
    public static void getAgentStatus(final Vertx _vertx
            , final int phoneNumber
            , final Common.BuildLog log
            , final PhonesDb phonesDb
            , final Handler<SoapVerticle.ObjCoreStatus> callback) {

        phonesDb.getPhoneObjInfo(phoneNumber, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if (obj != null && obj.deleted == false) {

                    log.add("****", "da co tren bang phones");
                    SoapVerticle.ObjCoreStatus objCorStat = new SoapVerticle.ObjCoreStatus();
                    callback.handle(objCorStat);
                    return;
                }

                log.add("****", "chua co tren bang phones --> vao core check 1 phat");
                log.add("function", "getAgentStatus");
                MomoMessage mmMsg = new MomoMessage(SoapProto.MsgType.CHECK_USER_STATUS_VALUE
                        , 0
                        , phoneNumber
                        , "".getBytes());

                _vertx.eventBus().sendWithTimeout(AppConstant.SoapVerticle_ADDRESS
                        , mmMsg.toBuffer()
                        , 20000, new Handler<AsyncResult<Message<Buffer>>>() {
                            @Override
                            public void handle(AsyncResult<Message<Buffer>> msgAsResult) {
                                SoapVerticle.ObjCoreStatus objCorStat = new SoapVerticle.ObjCoreStatus();

                                if (msgAsResult.succeeded()) {
                                    MomoMessage msg = MomoMessage.fromBuffer(msgAsResult.result().body());
                                    MomoProto.RegStatus status = null;
                                    try {
                                        status = MomoProto.RegStatus.parseFrom(msg.cmdBody);

                                    } catch (InvalidProtocolBufferException e) {
                                        log.add("loi", "Khong the parse du lieu");
                                    }
                                    if (status != null) {
                                        objCorStat.isReged = status.getIsReged();
                                        objCorStat.isActivated = status.getIsActive();
                                        objCorStat.isSuspended = status.getIsSuppend();
                                        objCorStat.isStopped = status.getIsStopped();
                                        objCorStat.isFrozen = status.getIsFrozen();
                                    }

                                }
                                callback.handle(objCorStat);
                            }
                        });
            }
        });

        /*_vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS
                                        ,mmMsg.toBuffer()
                                        ,new Handler<CoreMessage<Buffer>>() {
            @Override
            public void handle(CoreMessage<Buffer> response) {
                MomoMessage msg = MomoMessage.fromBuffer(response.body());
                MomoProto.RegStatus status = null;
                try {
                    status =  MomoProto.RegStatus.parseFrom(msg.cmdBody);

                } catch (InvalidProtocolBufferException e) {
                    log.add("loi", "Khong the parse du lieu");
                }

                SoapVerticle.ObjCoreStatus objCorStat = new SoapVerticle.ObjCoreStatus();
                if(status != null){
                    objCorStat.isReged = status.getIsReged();
                    objCorStat.isActivated = status.getIsActive();
                    objCorStat.isSuspended = status.getIsSuppend();
                    objCorStat.isStopped = status.getIsStopped();
                    objCorStat.isFrozen = status.getIsFrozen();
                }
                callback.handle(objCorStat);
            }
        });*/
    }

    public static com.mservice.momo.gateway.internal.core.objects.KeyValue getIsSmsKeyValue() {

        return new com.mservice.momo.gateway.internal.core.objects.KeyValue("issms", "no");
    }

    public static com.mservice.momo.gateway.internal.core.objects.KeyValue getClientBackendKeyValue() {

        return new com.mservice.momo.gateway.internal.core.objects.KeyValue("client", "backend");
    }

    public static ArrayList<SoapProto.keyValuePair.Builder> buildKeyValuesForSoap(List<MomoProto.TextValue> list) {

        ArrayList<SoapProto.keyValuePair.Builder> arrayList = new ArrayList<>();

        if (list == null || list.size() == 0) return arrayList;
        for (int i = 0; i < list.size(); i++) {
            MomoProto.TextValue tv = list.get(i);

            arrayList.add(SoapProto.keyValuePair.newBuilder()
                    .setKey(tv.getText())
                    .setValue(tv.getValue())
            );
        }
        return arrayList;
    }

    public static void addKeyValueForSoap(ArrayList<SoapProto.keyValuePair.Builder> arrayKVs, String key, String value) {
        if (arrayKVs == null) {
            arrayKVs = new ArrayList<>();
        }
        arrayKVs.add(SoapProto.keyValuePair.newBuilder().setKey(key).setValue(value));
    }

    public static HashMap<String, String> buildKeyValueHashMap(List<MomoProto.TextValue> list) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (list == null || list.size() == 0) return hashMap;
        for (int i = 0; i < list.size(); i++) {
            MomoProto.TextValue tv = list.get(i);
            hashMap.put(tv.getText(), tv.getValue());
        }
        return hashMap;
    }

    public static HashMap<String, String> buildKeyValueHashMapInSoap(List<SoapProto.keyValuePair> list) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (list == null || list.size() == 0) return hashMap;
        for (int i = 0; i < list.size(); i++) {
            SoapProto.keyValuePair tv = list.get(i);
            hashMap.put(tv.getKey(), tv.getValue());
        }
        return hashMap;
    }

    public static void sendSms(Vertx _vertx
            , final int phoneNumber
            , String smsContent) {

        SoapProto.SendSms sms = SoapProto.SendSms.newBuilder()
                .setSmsId(0)
                .setToNumber(phoneNumber)
                .setContent(smsContent)
                .build();

        _vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sms.toByteArray());
    }

    public static void sendTranAsSyn(final MomoMessage msg
            , final NetSocket sock
            , final TransDb transDb
            , final TranObj tran
            , final Common mCom) {

        transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {

                MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                builder.addTranList(MomoProto.TranHisV1.newBuilder()
                        .setTranId(tran.tranId)
                        .setClientTime(tran.clientTime)
                        .setAckTime(tran.ackTime)
                        .setFinishTime(tran.finishTime)
                        .setTranType(tran.tranType)
                        .setIo(tran.io)
                        .setCategory(tran.category)
                        .setPartnerId(tran.partnerId)
                        .setPartnerCode(tran.parterCode)
                        .setPartnerName(tran.partnerName)
                        .setPartnerRef(tran.partnerRef)
                        .setBillId(tran.billId)
                        .setAmount(tran.amount)
                        .setComment(tran.comment)
                        .setStatus(tran.status)
                        .setError(tran.error)
                        .setCommandInd(tran.cmdId)
                );

                Buffer buff = MomoMessage.buildBuffer(
                        MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                        msg.cmdIndex,
                        msg.cmdPhone,
                        builder.setResult(true)
                                .build()
                                .toByteArray()
                );
                if (sock != null) {
                    mCom.writeDataToSocket(sock, buff);
                }
            }
        });
    }

    public static void sendTranAsSynWithOutSock(final TransDb transDb
            , final TranObj tran
            , final Common mCom) {

        transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {

                MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                builder.addTranList(MomoProto.TranHisV1.newBuilder()
                        .setTranId(tran.tranId)
                        .setClientTime(tran.clientTime)
                        .setAckTime(tran.ackTime)
                        .setFinishTime(tran.finishTime)
                        .setTranType(tran.tranType)
                        .setIo(tran.io)
                        .setCategory(tran.category)
                        .setPartnerId(tran.partnerId)
                        .setPartnerCode(tran.parterCode)
                        .setPartnerName(tran.partnerName)
                        .setPartnerRef(tran.partnerRef)
                        .setBillId(tran.billId)
                        .setAmount(tran.amount)
                        .setComment(tran.comment)
                        .setStatus(tran.status)
                        .setError(tran.error)
                        .setCommandInd(tran.cmdId)
                );
            }
        });
    }

    public static void transferViaSoap(final Vertx _vertx
            , final MomoMessage msg
            , final String fromNumber
            , final String pin
            , final String chanel
            , final long amount
            , final String toNumber
            , final List<SoapProto.keyValuePair> keyValuePairList
            , Common.BuildLog log
            , final Handler<Common.SoapObjReply> callback) {

        SoapProto.M2MTransfer.Builder builder = SoapProto.M2MTransfer.newBuilder();
        builder.setAgent("0" + fromNumber)
                .setMpin(pin)
                .setPhone("0" + toNumber)
                .setChannel(chanel)
                .setAmount(amount)
                .setNotice("");
        //
        if (keyValuePairList != null && keyValuePairList.size() > 0) {
            for (int i = 0; i < keyValuePairList.size(); i++) {
                builder.addKvps(SoapProto.keyValuePair.newBuilder()
                        .setKey(keyValuePairList.get(i).getKey())
                        .setValue(keyValuePairList.get(i).getValue())
                );

                log.add(keyValuePairList.get(i).getKey(), keyValuePairList.get(i).getValue());
            }
        }

        //build buffer --> soap verticle
        final Buffer m2mTransfer = MomoMessage.buildBuffer(
                SoapProto.MsgType.M2M_TRANSFER_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build().toByteArray()
        );

        _vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, m2mTransfer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {

                Common.SoapObjReply soapObjReply = new Common.SoapObjReply(result.body());
                callback.handle(soapObjReply);
            }
        });

    }

    public static void forceUpdateAgentInfo(final Vertx _vertx, PhonesDb.Obj phoneObj) {
        PhonesDb.Obj obj = new PhonesDb.Obj();
        obj.number = phoneObj.number;
        BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
        msg.setType(SoapProto.Broadcast.MsgType.FORCE_UPDATE_AGENT_INFO_VALUE);
        msg.setSenderNumber(phoneObj.number);
        msg.setExtra(obj.toJsonObject());
        _vertx.eventBus().publish(Misc.getNumberBus(phoneObj.number), msg.getJsonObject());
    }

    public static void getPayBackCDHHSetting(final Vertx _vertx, final String serviceId, final Handler<CDHHPayBackSetting.Obj> callback) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_PAYBACK_EVENT;
        serviceReq.ServiceId = serviceId;
        _vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject jo = message.body();
                CDHHPayBackSetting.Obj pbObj = new CDHHPayBackSetting.Obj(jo);
                callback.handle(pbObj);
            }
        });
    }

    public static void getCdhhWeekOrQuaterActive(final Vertx _vertx
            , String serviceId
            , final Handler<CdhhConfig> callback) {

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_CDHH_CONFIG_WEEK_OR_AQUATER_ACTIVE;
        serviceReq.ServiceId = serviceId;
        _vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject jo = message.body();
                CdhhConfig cdhhConfig = new CdhhConfig(jo);
                callback.handle(cdhhConfig);
            }
        });
    }

    public static MomoMessage setTranType(MomoMessage msg, int extTranType) {
        MomoProto.TranHisV1 request;
        try {
            request = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            request = null;
        }

        if (request != null) {
            MomoMessage msgB = new MomoMessage(msg.cmdType, msg.cmdIndex, msg.cmdPhone
                    , MomoProto.TranHisV1.newBuilder()
                    .setComment(request.getComment())
                    .setPartnerCode(request.getPartnerCode())
                    .setAmount(request.getAmount())
                    .setSourceFrom(request.getSourceFrom())
                    .setPartnerId(request.getPartnerId())
                    .setPartnerCode(request.getPartnerCode())
                    .setPartnerName(request.getPartnerName())
                    .setBillId(request.getBillId())
                    .setPartnerRef(request.getPartnerRef())
                    .setClientTime(request.getClientTime())
                    .setTranType(extTranType)
                    .setIo(request.getIo())
                    .setCategory(request.getCategory())
                    .setPartnerExtra1(request.getPartnerExtra1())
                    .build().toByteArray()
            );

            return msgB;
        }
        return msg;

    }

    public static void sendForceTranHis(final Vertx _vertx
            , final long tranId
            , final MomoMessage orgMsg
            , final MomoProto.TranHisV1 orgTranHisV1
            , final long amount
            , final SockData _data
            , final String content
            , final TransDb transDb
            , final Common mCom
            , final String partnerName
            , final int status
            , final NetSocket sock) {
        _vertx.setTimer(300, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {

                //todo send tranout side
                final TranObj tran = new TranObj();
                tran.owner_number = orgMsg.cmdPhone;
                tran.tranType = orgTranHisV1.getTranType();
                tran.status = status;
                tran.io = orgTranHisV1.getIo();
                tran.comment = content;
                tran.tranId = tranId;
                tran.cmdId = orgMsg.cmdIndex;
                tran.error = 100;
                tran.billId = orgTranHisV1.getPartnerId();
                tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                tran.clientTime = orgTranHisV1.getClientTime();
                tran.ackTime = System.currentTimeMillis();
                tran.finishTime = System.currentTimeMillis();
                tran.amount = amount;
                tran.owner_name = ((_data != null && _data.getPhoneObj() != null) ? _data.getPhoneObj().name : "");
                tran.category = 0;
                tran.deleted = false;
                tran.partnerId = orgTranHisV1.getPartnerId();
                tran.parterCode = orgTranHisV1.getPartnerCode();
                tran.partnerName = partnerName;
                tran.cmdId = orgMsg.cmdIndex;
                transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean result) {

                        MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                        builder.addTranList(MomoProto.TranHisV1.newBuilder()
                                .setTranId(tran.tranId)
                                .setClientTime(tran.clientTime)
                                .setAckTime(tran.ackTime)
                                .setFinishTime(tran.finishTime)
                                .setTranType(tran.tranType)
                                .setIo(tran.io)
                                .setCategory(tran.category)
                                .setPartnerId(tran.partnerId)
                                .setPartnerCode(tran.parterCode)
                                .setPartnerName(tran.partnerName)
                                .setPartnerRef(tran.partnerRef)
                                .setBillId(tran.billId)
                                .setAmount(tran.amount)
                                .setComment(tran.comment)
                                .setStatus(tran.status)
                                .setError(tran.error)
                                .setCommandInd(tran.cmdId)
                        );

                        Buffer buff = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                                orgMsg.cmdIndex,
                                orgMsg.cmdPhone,
                                builder.setResult(true)
                                        .build()
                                        .toByteArray()
                        );

                        mCom.writeDataToSocket(sock, buff);
                    }
                });
            }
        });
    }

    public static void requestPromoRecord(Vertx _vertx
            , Promo.PromoReqObj promoReq
            , final Logger logger
            , final Handler<JsonObject> callback) {

        _vertx.eventBus().send(AppConstant.Promotion_ADDRESS
                , promoReq.toJsonObject(), new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        if (message != null) {
                            callback.handle(message.body());
                        } else {
                            callback.handle(new JsonObject());
                        }
                    }
                });
    }

    public static JsonObject buildJsonForShare(String key, String value) {
        JsonObject jo = new JsonObject();
        jo.putString(key, value);
        return jo;
    }

    public static HashMap<String, ArrayList<ServicePackageDb.Obj>> buildHashMapSrvPkg(ArrayList<ServicePackageDb.Obj> arrayList) {
        HashMap<String, ArrayList<ServicePackageDb.Obj>> hashMap = new HashMap<>();
        if (arrayList != null && arrayList.size() > 0) {

            for (int i = 0; i < arrayList.size(); i++) {
                ServicePackageDb.Obj item = arrayList.get(i);
                ArrayList<ServicePackageDb.Obj> arr = hashMap.get(item.linktodropbox);

                if (arr != null) {
                    arr.add(item);
                    hashMap.put(item.linktodropbox, arr);
                } else {
                    arr = new ArrayList<>();
                    arr.add(item);
                    hashMap.put(item.linktodropbox, arr);
                }
            }
        }

        return hashMap;
    }

    public static MomoProto.TextValue.Builder getTextValueBuilder(String key, String value) {

        return MomoProto.TextValue.newBuilder().setText(key).setValue(value);

    }

    public static void requestSubmitForm(final Vertx _vertx
            , final HashMap<String, String> hashMapKVPs
            , int phoneNumber
            , boolean isStoreApp
            , final Handler<ArrayList<BillInfoService.TextValue>> callback) {
        com.mservice.momo.vertx.form.RequestObj requestObj = new com.mservice.momo.vertx.form.RequestObj();
        requestObj.command = Command.submit_form;
        requestObj.hashMap = hashMapKVPs;
        requestObj.phoneNumber = phoneNumber;
        if (isStoreApp) {
//            _vertx.eventBus().send(AppConstant.DGD_SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonArray>>() {
            _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                    Misc.makeHttpPostWrapperData(AppConstant.DGD_SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonArray>>() {
                @Override
                public void handle(Message<JsonArray> message) {
                    ArrayList<BillInfoService.TextValue> arrayList = getTextValueList(message.body());
                    callback.handle(arrayList);
                }
            });
            return;
        }
//        _vertx.eventBus().send(AppConstant.SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonArray>>() {
        _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                Misc.makeHttpPostWrapperData(AppConstant.SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                ArrayList<BillInfoService.TextValue> arrayList = getTextValueList(message.body());
                callback.handle(arrayList);
            }
        });
    }

    public static void requestFormFields(final Vertx _vertx
            , final int nextForm
            , final String serviceId
            , final String command
            , final boolean isStoreApp
            , final String parentId, int phoneNumber, final Handler<ReplyObj> callback) {

        com.mservice.momo.vertx.form.RequestObj requestObj = new com.mservice.momo.vertx.form.RequestObj();
        requestObj.command = command;
        requestObj.serviceid = serviceId;
        requestObj.nextform = nextForm;
        requestObj.parentid = parentId;
        requestObj.phoneNumber = phoneNumber;
        if (isStoreApp) {
//            _vertx.eventBus().send(AppConstant.DGD_SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonObject>>() {
            _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                    Misc.makeHttpPostWrapperData(AppConstant.DGD_SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject jo = message.body();
                    callback.handle(new ReplyObj(jo));
                }
            });
            return;
        }
//        _vertx.eventBus().send(AppConstant.SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonObject>>() {
        _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                Misc.makeHttpPostWrapperData(AppConstant.SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject jo = message.body();
                callback.handle(new ReplyObj(jo));
            }
        });
    }

    public static void removeCacheFormData(final Vertx _vertx
            , String serviceId
            , int phoneNumber
            , boolean isStoreApp
            , final Handler<JsonObject> callback) {
        com.mservice.momo.vertx.form.RequestObj requestObj = new com.mservice.momo.vertx.form.RequestObj();
        requestObj.command = Command.remove_cache_bill_info;
        requestObj.serviceid = serviceId;
        requestObj.phoneNumber = phoneNumber;

        if (isStoreApp) {
//            _vertx.eventBus().send(AppConstant.DGD_SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonObject>>() {
            _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                    Misc.makeHttpPostWrapperData(AppConstant.DGD_SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    callback.handle(message.body());
                }
            });
            return;
        }
//        _vertx.eventBus().send(AppConstant.SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonObject>>() {
        _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                Misc.makeHttpPostWrapperData(AppConstant.SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }

    public static ArrayList<ServiceCategory.Obj> getCategoryList(JsonArray array) {
        ArrayList<ServiceCategory.Obj> arrayList = new ArrayList<>();
        if (array != null && array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                arrayList.add(new ServiceCategory.Obj((JsonObject) array.get(i)));
            }
        }
        return arrayList;
    }

    public static void requestTotalForm(final Vertx _vertx
            , final String serviceId
            , final boolean isStoreApp
            , final String command, final Handler<Integer> callback) {

        com.mservice.momo.vertx.form.RequestObj requestObj = new com.mservice.momo.vertx.form.RequestObj();
        requestObj.command = command;
        requestObj.serviceid = serviceId;
        if (isStoreApp) {
//            _vertx.eventBus().send(AppConstant.DGD_SubmitForm_Address, requestObj.toJson(), new Handler<Message<Integer>>() {
            _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                    Misc.makeHttpPostWrapperData(AppConstant.DGD_SubmitForm_Address, requestObj.toJson()), new Handler<Message<Integer>>() {
                @Override
                public void handle(Message<Integer> message) {
                    callback.handle(message.body());
                }
            });
            return;
        }
//        _vertx.eventBus().send(AppConstant.SubmitForm_Address, requestObj.toJson(), new Handler<Message<Integer>>() {
        _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                Misc.makeHttpPostWrapperData(AppConstant.SubmitForm_Address, requestObj.toJson()), new Handler<Message<Integer>>() {
            @Override
            public void handle(Message<Integer> message) {
                callback.handle(message.body());
            }
        });
    }

    public static BillInfoService.TextValue buildTextValue(String text, String value) {
        BillInfoService.TextValue tv = new BillInfoService.TextValue();
        tv.text = text;
        tv.value = value;
        return tv;
    }

    public static BillInfoService.TextValue getCaptionConfirm(String value) {
        BillInfoService.TextValue tv = new BillInfoService.TextValue();
        tv.text = "cap";
        tv.value = value;
        return tv;
    }

    public static BillInfoService.TextValue getButtonConfirm(String value) {
        BillInfoService.TextValue tv = new BillInfoService.TextValue();
        tv.text = "btn";
        tv.value = value;
        return tv;
    }

    public static void sendTranslateToClient(final MomoMessage msg
            , final NetSocket sock
            , ArrayList<BillInfoService.TextValue> arrayList
            , Common mCommon
            , Common.BuildLog log) {
        MomoProto.TextValueMsg.Builder builder = MomoProto.TextValueMsg.newBuilder();

        for (int i = 0; i < arrayList.size(); i++) {
            builder.addKeys(MomoProto.TextValue.newBuilder()
                    .setText(arrayList.get(i).text)
                    .setValue(arrayList.get(i).value)
            );

            log.add(arrayList.get(i).text, arrayList.get(i).value);

        }

        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.TRANSLATE_CONFIRM_INFO_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build()
                        .toByteArray()
        );

        mCommon.writeDataToSocket(sock, buf);
    }

    public static MomoMessage refineMomoMessage(MomoMessage msg, Common.BuildLog log) {
        MomoProto.TranHisV1 tranHisV1;
        try {
            tranHisV1 = MomoProto.TranHisV1.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            tranHisV1 = null;
        }
        if (tranHisV1 == null) {
            return msg;
        }

        /* final TranHisTable_V1.TranHisObject_V1 obj = new TranHisTable_V1.TranHisObject_V1();
        obj.CID = cid;
        obj.CTIME = System.currentTimeMillis();
        obj.TYPE = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
        obj.IO = -1;
        obj.CAT = -1;
        obj.PARTNER_ID = serviceId;
        obj.PARTNER_CODE = "";
        obj.PARTNER_NAME = serviceName;
        obj.BILL_ID = billId;
        obj.AMOUNT = amount;
        String comment = "Quý khách đã thanh toán thành công " + MoMoUtils.formatAmount(String.valueOf(amount))
                + " cho dịch vụ " + serviceName + ". Mã thanh toán: " + billId + ".";
        obj.COMMENT = comment;
        //obj.COMMENT = "Thanh toán dịch vụ của: " + serviceName + ". Mã hóa đơn: " + billId + ". Số tiền: " + MoMoUtils.formatAmount(String.valueOf(amount));
        obj.SOURCE_FORM = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        obj.TAG = "Ví MoMo";
        obj.KVP = kvp;
        obj.KVP_TRANSLATED = kvp_translated;
        return obj;
*/

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMap(tranHisV1.getKvpList());
        String billId = (hashMap.containsKey(Const.AppClient.BillId) ? hashMap.get(Const.AppClient.BillId) : "");
        String serviceId = (hashMap.containsKey(Const.AppClient.ServiceId) ? hashMap.get(Const.AppClient.ServiceId) : "");

        String strAmt = (hashMap.containsKey(Const.AppClient.Amount) ? hashMap.get(Const.AppClient.Amount) : "0");
        long amount = DataUtil.stringToUNumber(strAmt);

        amount = (amount == 0 ? (tranHisV1.getAmount() == 0 ? 0 : tranHisV1.getAmount()) : amount);
        String serviceName = tranHisV1.getPartnerName() == null ? "" : tranHisV1.getPartnerName();

        String comment = "Quý khách đã thanh toán thành công " + Misc.formatAmount(amount).replaceAll(",", ".") + "đ"
                + " cho dịch vụ " + serviceName + ". Mã thanh toán: " + billId + ".";

        billId = ("".equalsIgnoreCase(billId) ? (tranHisV1.getBillId() == null ? "" : tranHisV1.getBillId()) : billId);
        serviceId = ("".equalsIgnoreCase(serviceId) ? (tranHisV1.getPartnerId() == null ? "" : tranHisV1.getPartnerId()) : serviceId);

        String qty_tmp = (hashMap.containsKey(Const.AppClient.Quantity) ? hashMap.get(Const.AppClient.Quantity) : "1");
        int qty = DataUtil.strToInt(qty_tmp);

        long final_amount = amount * qty;

        log.add("function", "refineMomoMessage");
        log.add("service id", serviceId);
        log.add("service name", serviceName);
        log.add("billid", billId);
        log.add("amount", amount);
        log.add("qty", qty);
        log.add("final amount", final_amount);
        log.add("tranHisV1.getShare()", tranHisV1.getShare());
        log.add("source from", MomoProto.TranHisV1.SourceFrom.valueOf(tranHisV1.getSourceFrom()));

        /*optional uint64 tranId =1; // return from core doing transaction, cTID
        optional uint64 client_time = 2; // time at calling webservice
        optional uint64 ackTime = 3; // ack time from server to client
        optional uint64 finishTime=4; // the time that server response result.
        optional uint32 tranType = 5; //ex : MomoProto.MsgType.BANK_IN_VALUE
        optional uint32 io = 6; // direction of transaction.
        optional uint32 category=7; // type of transfer, chuyen tien ve que, cho vay, khac
        optional string partnerId = 8; // ban hang cua minh : providerId ..
        optional string partnerCode=9; // ma doi tac
        optional string partnerName=10; // ten doi tac
        optional string partner_ref =11;
        optional string billId=12; // for invoice, billID
        optional uint64 amount=13; // gia tri giao dich
        optional string comment=14; // ghi chu
        optional uint32 status=15; // trang thai giao dich
        optional uint32 error=16; // ma loi giao dich
        optional uint64 command_Ind = 17; // command index
        optional uint32 source_from=18; // tu nguon nao
        optional string partner_extra_1=19;
        repeated TextValue kvp = 20;
        optional string desc=21; // mo ta loi tra ve

        // text     key
        // "cusnum" 0974541201 -->so dien thoai cua khach hang
        // '"dgd"   "1"

        //tra thong tin ve client
        //key           value
        //qrcode        chuoi cac ky tu de app ve QRCODE
        //html          format html tra ve cho khach hang
        optional string share = 22;*/

        MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
        builder.setTranId(tranHisV1.getTranId())
                .setClientTime(tranHisV1.getClientTime())
                .setAckTime(tranHisV1.getAckTime())
                .setFinishTime(tranHisV1.getFinishTime())
                .setTranType(tranHisV1.getTranType())
                .setIo(-1)
                .setCategory(-1)
                .setPartnerId(serviceId)
                .setPartnerCode((tranHisV1.getPartnerCode() == null ? "" : tranHisV1.getPartnerCode()))
                .setPartnerName((tranHisV1.getPartnerName() == null ? "" : tranHisV1.getPartnerName()))
                .setPartnerRef((tranHisV1.getPartnerRef() == null ? "" : tranHisV1.getPartnerRef()))
                .setBillId(billId)
                .setAmount(final_amount)
                .setComment(comment)
                .setStatus(tranHisV1.getStatus())
                .setError(tranHisV1.getError())
                .setCommandInd(tranHisV1.getCommandInd())
                .setPartnerExtra1((tranHisV1.getPartnerExtra1() == null ? "" : tranHisV1.getPartnerExtra1()))
                .setSourceFrom(tranHisV1.getSourceFrom())
                .setDesc("")
                .setShare(tranHisV1.getShare())
                .addAllKvp(tranHisV1.getKvpList());

        Buffer buffer = MomoMessage.buildBuffer(msg.cmdType, msg.cmdIndex, msg.cmdPhone, builder.build().toByteArray());
        MomoMessage momoMessage = MomoMessage.fromBuffer(buffer);
        return momoMessage;

    }

    public static void getBillInfo(final Vertx vertx
            , final String billId, final String serviceId, final Handler<BillInfoService> callback) {

        Buffer getBillBuf = MomoMessage.buildBuffer(
                SoapProto.MsgType.GET_BILL_INFO_BY_SERVICE_VALUE,
                0,
                0,
                SoapProto.GetBillInfo.newBuilder()
                        .setMpin("")
                        .setBillId(billId)
                        .setProviderId(serviceId)
                        .build()
                        .toByteArray()
        );

        //send to soap
        vertx.eventBus().sendWithTimeout(AppConstant.SoapVerticle_ADDRESS, getBillBuf
                , 20000
                , new Handler<AsyncResult<Message<JsonObject>>>() {
                    @Override
                    public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                        BillInfoService bis;
                        if (messageAsyncResult.succeeded()) {

                            Message<JsonObject> result = messageAsyncResult.result();
                            int rcode = (result != null ? result.body().getInteger("rcode", 3) : 3); // loi he thong

                            //lay thong tin bill bi loi
                            if (rcode != 0) {
                                bis = new BillInfoService();
                                bis.total_amount = -100;// khong lay duoc thong tin bill
                            } else {
                                JsonObject jsonBill = result.body().getObject("json_result", null);
                                bis = new BillInfoService(jsonBill);
                            }
                        } else {
                            bis = new BillInfoService();
                            bis.total_amount = -100;// khong lay duoc thong tin bill
                        }
                        callback.handle(bis);

                    }
                });
    }

    public static HashMap<String, BillInfoService.TextValue> convertArrayTextValue(ArrayList<BillInfoService.TextValue> textValues) {
        HashMap<String, BillInfoService.TextValue> hashMap = new HashMap<>();
        if (textValues == null) return hashMap;
        for (int i = 0; i < textValues.size(); i++) {
            hashMap.put(textValues.get(i).text, textValues.get(i));
        }
        return hashMap;
    }

    public static boolean isValidJsonArray(String jarray) {
        return jarray.startsWith("[") && jarray.endsWith("]");
    }

    public static boolean isValidJsonObject(String text) {
        try {
            JsonObject jsonObject = new JsonObject(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static HashMap<String, String> convertArrayTextValueToHashMap(ArrayList<BillInfoService.TextValue> textValues) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (textValues == null) return hashMap;
        for (int i = 0; i < textValues.size(); i++) {
            hashMap.put(textValues.get(i).text, textValues.get(i).value);
        }
        return hashMap;
    }

    public static void refineLabelForFieldItem(String fieldKey, String data, ArrayList<FieldItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (FieldItem fi : items) {
            if (fieldKey.equalsIgnoreCase(fi.key)) {
                fi.fieldlabel = String.format(fi.fieldlabel, data);
                break;
            }
        }
    }

    public static void addValueForFieldItem(String fieldKey, String data, ArrayList<FieldItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (FieldItem fi : items) {
            if (fieldKey.equalsIgnoreCase(fi.key)) {
                fi.value = data;
                break;
            }
        }
    }

    public static void addDataForFieldItem(String fieldKey
            , ArrayList<BillInfoService.TextValue> listData
            , ArrayList<FieldData> fieldDataArrayList) {

        for (int i = 0; i < listData.size(); i++) {
            FieldData fd = new FieldData();
            fd.linkto = fieldKey;
            fd.text = listData.get(i).text;
            fd.value = listData.get(i).value;
            fd.parentid = "";
            fd.id = "";
            fieldDataArrayList.add(fd);
        }
    }

    public static void buildNotiAndSend(String caption
            , String content
            , long tranId
            , int phoneNumber
            , long cmdIndex
            , Vertx _vertx) {

        Notification noti = new Notification();
        noti.receiverNumber = phoneNumber;
        noti.caption = caption;
        noti.body = content;
        noti.sms = "";
        noti.priority = 2;
        noti.cmdId = cmdIndex;
        noti.time = System.currentTimeMillis();
        noti.tranId = tranId;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.status = Notification.STATUS_DISPLAY;
        noti.btnStatus = 0;
        noti.btnTitle = "";
        noti.category = MomoProto.NotiCategory.SYSTEM_VALUE;
        Misc.sendNoti(_vertx, noti);
    }

    public static void buildPayOneBillNotiAndSend(String caption
            , String content
            , long tranId
            , int phoneNumber
            , long cmdIndex
            , Vertx _vertx) {

        Notification noti = new Notification();
        noti.receiverNumber = phoneNumber;
        noti.caption = caption;
        noti.body = content;
        noti.sms = "";
        noti.priority = 2;
        noti.cmdId = cmdIndex;
        noti.time = System.currentTimeMillis();
        noti.tranId = tranId;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.status = Notification.STATUS_DETAIL;
        noti.btnStatus = 0;
        noti.btnTitle = "";
        noti.category = MomoProto.NotiCategory.SYSTEM_VALUE;
        Misc.sendNoti(_vertx, noti);
    }

    public static void buildTranHisAndSend(final MomoMessage msg
            , long amount
            , int rcode
            , long tranId
            , String comment
            , final MomoProto.TranHisV1 tranHisV1
            , TransDb transDb
            , final Common mComm
            , final JsonObject kvp
            , String desctiption
            , final NetSocket sock) {

        final TranObj tranObj = new TranObj();
        tranObj.tranId = tranId;
        tranObj.clientTime = tranHisV1.getClientTime();
        tranObj.ackTime = System.currentTimeMillis();
        tranObj.finishTime = System.currentTimeMillis();
        tranObj.amount = amount;
        tranObj.status = (rcode == 0 ? TranObj.STATUS_OK : TranObj.STATUS_FAIL);
        tranObj.error = rcode;
        tranObj.cmdId = msg.cmdIndex;
        tranObj.billId = tranHisV1.getBillId() == null ? "" : tranHisV1.getBillId();
        tranObj.tranType = tranHisV1.getTranType();
        tranObj.partnerName = tranHisV1.getPartnerName() == null ? "" : tranHisV1.getPartnerName();
        tranObj.parterCode = tranHisV1.getPartnerCode() == null ? "" : tranHisV1.getPartnerCode();
        tranObj.partnerRef = tranHisV1.getPartnerRef() == null ? "" : tranHisV1.getPartnerRef();
        tranObj.partnerId = tranHisV1.getPartnerId() == null ? "" : tranHisV1.getPartnerId();
        tranObj.io = -1;
        tranObj.comment = comment;
        tranObj.source_from = tranHisV1.getSourceFrom();
        tranObj.kvp = kvp;

        if (rcode == MomoProto.TranHisV1.ResultCode.CUSTOM_ERROR_VALUE) {
            tranObj.desc = desctiption;
        }

        JsonArray arrayShared = new JsonArray();

        String html = "";

        String qrcode = "";

        if (!"".equalsIgnoreCase(html)) {
            arrayShared.add(new JsonObject().putString(Const.AppClient.Html, html));
        }

        if (!"".equalsIgnoreCase(qrcode)) {
            arrayShared.add(new JsonObject().putString(Const.AppClient.Qrcode, qrcode));
        }

        tranObj.share = arrayShared;
        //save to database
        transDb.upsertTran(msg.cmdPhone, tranObj.getJSON(), new Handler<TranObj>() {
            @Override
            public void handle(TranObj tranObj) {
                //todo do nothing
            }
        });

        //send to client
        MomoProto.TranHisV1.Builder builder = MomoProto.TranHisV1.newBuilder();
        builder
                .setTranId(tranObj.tranId)
                .setClientTime(tranObj.clientTime)
                .setAckTime(tranObj.ackTime)
                .setFinishTime(tranObj.finishTime)
                .setTranType(tranObj.tranType)
                .setIo(tranObj.io)
                .setCategory(tranObj.category)
                .setPartnerId(tranObj.partnerId)
                .setPartnerCode(tranObj.parterCode)
                .setPartnerName(tranObj.partnerName)
                .setPartnerRef(tranObj.partnerRef)
                .setBillId(tranObj.billId)
                .setAmount(tranObj.amount)
                .setComment(tranObj.comment)
                .setCommandInd(tranObj.cmdId)
                .setStatus(tranObj.status)
                .setError(tranObj.error)
                .setSourceFrom(tranObj.source_from)
                .setDesc(DataUtil.nullToEmpty(tranObj.desc))
                .setShare(arrayShared.toString());

        builder.addKvp(MomoProto.TextValue.newBuilder().setText("fcmt").setValue("1"));

        if (kvp != null && kvp.getFieldNames().size() > 0) {
            for (String s : kvp.getFieldNames()) {
                builder.addKvp(MomoProto.TextValue.newBuilder()
                        .setText(s)
                        .setValue(kvp.getString(s)));
            }
        }

        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.TRANS_REPLY_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                builder.build().toByteArray()
        );

        mComm.writeDataToSocket(sock, buf);

    }

    public static void getTranslatedInfo(final Vertx _vertx
            , final HashMap<String, String> hashMap
            , final String serviceId
            , final int phoneNumber
            , final boolean isStoreApp
            , final Handler<ArrayList<BillInfoService.TextValue>> callback) {

        com.mservice.momo.vertx.form.RequestObj requestObj = new com.mservice.momo.vertx.form.RequestObj();
        requestObj.command = Command.translate_confirmation;
        requestObj.serviceid = serviceId;
        requestObj.hashMap = hashMap;
        requestObj.phoneNumber = phoneNumber;
        if (isStoreApp) {
//            _vertx.eventBus().send(AppConstant.DGD_SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonArray>>() {
            _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                    Misc.makeHttpPostWrapperData(AppConstant.DGD_SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonArray>>() {
                @Override
                public void handle(Message<JsonArray> message) {
                    ArrayList<BillInfoService.TextValue> arrayList = getTextValueList(message.body());
                    callback.handle(arrayList);
                }
            });
            return;
        }
//        _vertx.eventBus().send(AppConstant.SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonArray>>() {
        _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                Misc.makeHttpPostWrapperData(AppConstant.SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonArray>>() {
            @Override
            public void handle(Message<JsonArray> message) {
                ArrayList<BillInfoService.TextValue> arrayList = getTextValueList(message.body());
                callback.handle(arrayList);
            }
        });
    }

    public static ArrayList<BillInfoService.TextValue> getTextValueList(JsonArray array) {
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
        if (array == null || array.size() == 0) return arrayList;
        for (int i = 0; i < array.size(); i++) {
            BillInfoService.TextValue tv = new BillInfoService.TextValue();
            JsonObject jo = array.get(i);
            for (String s : jo.getFieldNames()) {
                tv.text = s;
                tv.value = jo.getString(s);
                arrayList.add(tv);
                break;
            }
        }
        return arrayList;
    }

    public static JsonArray getJsonArray(ArrayList<BillInfoService.TextValue> arrayList) {
        JsonArray joArr = new JsonArray();
        if (arrayList == null || arrayList.isEmpty()) return joArr;
        for (BillInfoService.TextValue tv : arrayList) {
            JsonObject jo = new JsonObject();
            jo.putString(tv.text, tv.value);
            joArr.add(jo);
        }
        return joArr;
    }

    public static void getViaCoreService(final Vertx _vertx
            , String serviceId
            , boolean isStoreApp
            , final Handler<ViaConnectorObj> callback) {

        com.mservice.momo.vertx.form.RequestObj requestObj = new com.mservice.momo.vertx.form.RequestObj();
        requestObj.command = Command.get_via_core_info;
        requestObj.serviceid = serviceId;

        if (isStoreApp) {
//            _vertx.eventBus().send(AppConstant.DGD_SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonObject>>() {
            _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                    Misc.makeHttpPostWrapperData(AppConstant.DGD_SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    ViaConnectorObj viaConnectorObj = new ViaConnectorObj(message.body());
                    callback.handle(viaConnectorObj);
                }
            });
            return;
        }
//        _vertx.eventBus().send(AppConstant.SubmitForm_Address, requestObj.toJson(), new Handler<Message<JsonObject>>() {
        _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                Misc.makeHttpPostWrapperData(AppConstant.SubmitForm_Address, requestObj.toJson()), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                ViaConnectorObj viaConnectorObj = new ViaConnectorObj(message.body());
                callback.handle(viaConnectorObj);
            }
        });
    }

    public static void getRetailerFee(final Vertx _vertx
            , int tranType
            , long amount
            , int phoneNumber
            , final Handler<FeeObj> callback) {
        FeeObj feeReqObj = new FeeObj();
        feeReqObj.command = FeeObj.get_fee;
        feeReqObj.tranType = tranType;
        feeReqObj.tranAmount = amount;
        feeReqObj.phoneNumber = phoneNumber;
//        _vertx.eventBus().send(AppConstant.Retailer_Fee_Address, feeReqObj.toJson(), new Handler<Message<JsonObject>>() {
        _vertx.eventBus().send(AppConstant.HTTP_POST_BUS_ADDRESS,
                Misc.makeHttpPostWrapperData(AppConstant.Retailer_Fee_Address, feeReqObj.toJson()), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(new FeeObj(message.body()));
            }
        });
    }

    public static void getM2MFee(final Vertx _vertx
            , int trantype
            , long amount
            , String senderNumber
            , String receiverNumber
            , final Handler<FeeDb.Obj> callback) {

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_FEE;
        serviceReq.bankId = "m2m";
        serviceReq.channel = 0;
        serviceReq.tranType = trantype;
        serviceReq.inoutCity = 0;
        serviceReq.amount = amount;
        serviceReq.PackageId = receiverNumber; // Dung ke data cho nguoi nhan
        serviceReq.PackageType = senderNumber; // Dung ke data cho nguoi gui
        _vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override

            public void handle(Message<JsonObject> message) {
                FeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new FeeDb.Obj(message.body());
                }
                callback.handle(obj);
            }
        });
    }

    public static void getM2MerchantFee(final Vertx _vertx
            , int trantype
            , long amount
            , String senderNumber
            , String receiverNumber
            , final Handler<FeeDb.Obj> callback) {

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_FEE;
        serviceReq.bankId = "m2merchant";
        serviceReq.channel = 0;
        serviceReq.tranType = trantype;
        serviceReq.inoutCity = 0;
        serviceReq.amount = amount;
        serviceReq.PackageId = receiverNumber; // Dung ke data cho nguoi nhan
        serviceReq.PackageType = senderNumber; // Dung ke data cho nguoi gui
        _vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override

            public void handle(Message<JsonObject> message) {
                FeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new FeeDb.Obj(message.body());
                }
                callback.handle(obj);
            }
        });
    }

    // giao dich c2c
    public static void getC2CFee(final Vertx vertx
            , int tranType
            , long amount
            , final Handler<FeeDb.Obj> callback) {
        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_FEE;
        serviceReq.bankId = "c2c";
        serviceReq.channel = 0;
        serviceReq.tranType = tranType;
        serviceReq.inoutCity = 0;
        serviceReq.amount = amount;

        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<JsonObject>>() {
            @Override

            public void handle(Message<JsonObject> message) {
                FeeDb.Obj obj = null;
                if (message.body() != null) {
                    obj = new FeeDb.Obj(message.body());
                }
                callback.handle(obj);
            }
        });
    }

    public static int getVoucherPointType(TransferWithGiftContext context) {
        int voucherPointType = 0;

        if (context.point != 0 && context.voucher == 0) {
            voucherPointType = Const.VoucherPointType.OnlyPoint;
        } else if (context.point == 0 && context.voucher != 0) {
            voucherPointType = Const.VoucherPointType.OnlyVoucher;
        } else if (context.point != 0 && context.voucher != 0) {
            voucherPointType = Const.VoucherPointType.PointAndVoucher;
        }
        return voucherPointType;
    }

    public static void modifyAgent(final Vertx _vertx
            , final PhonesDb.Obj phoneObj
            , final Logger logger, final Handler<Boolean> callback) {

        ArrayList<SoapProto.keyValuePair.Builder> arrayKVs = new ArrayList<>();
        Misc.addKeyValueForSoap(arrayKVs, Const.REFERAL, String.valueOf(phoneObj.inviter));

        SoapProto.AgentInfoModify.Builder builder = SoapProto.AgentInfoModify.newBuilder();

        for (int i = 0; i < arrayKVs.size(); i++) {
            builder.addKvps(arrayKVs.get(i));
        }

        Buffer agentInfo = MomoMessage.buildBuffer(
                SoapProto.MsgType.MODIFY_AGENT_EXTRA_VALUE,
                System.currentTimeMillis(),
                phoneObj.number,
                builder.build().toByteArray()
        );

        _vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, agentInfo, new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> result) {
                callback.handle(result.body());
            }
        });
    }

    public static String getLinkForClient(String url, String text) {
        String tmp = "<a href=\"%s\">%s</a>";
        return String.format(tmp, url, text);
        //<a href="http://momo.vn/lienketvcb">momo</a>, <a href="tel:0839911188">(08) 399 111 88</a>,<a href="mailto:lanh.luu@mservice.com.vn">mymail</a>
    }

    public static String getTelForClient(String phone, String text) {
        String tmp = "<a href=\"tel:%s\">%s</a>";
        return String.format(tmp, phone, text);
        //<a href="http://momo.vn/lienketvcb">momo</a>, <a href="tel:0839911188">(08) 399 111 88</a>,<a href="mailto:lanh.luu@mservice.com.vn">mymail</a>
    }

    public static String getEmailForClient(String phone, String text) {
        String tmp = "<a href=\"mailto:%s\">%s</a>";
        return String.format(tmp, phone, text);
        //<a href="http://momo.vn/lienketvcb">momo</a>, <a href="tel:0839911188">(08) 399 111 88</a>,<a href="mailto:lanh.luu@mservice.com.vn">mymail</a>
    }

    public static Response getCoreReplyObj(Core.StandardReply stdReply) {

        Response replyObj = new Response();
        //lock tien khong thanh cong
        if ((stdReply == null)) {
            replyObj.Error = -100;
            replyObj.Description = "System error";

        } else {

            replyObj.Error = stdReply.getErrorCode();
            replyObj.Description = stdReply.getDescription();
            replyObj.Tid = stdReply.getTid();
            if (stdReply.getParamsCount() > 0) {
                replyObj.KeyValueList = new ArrayList<>();
                for (int i = 0; i < stdReply.getParamsCount(); i++) {
                    com.mservice.momo.gateway.internal.core.objects.KeyValue kv = new com.mservice.momo.gateway.internal.core.objects.KeyValue();
                    kv.Key = stdReply.getParams(i).getKey();
                    kv.Value = stdReply.getParams(i).getValue();
                    replyObj.KeyValueList.add(kv);
                }
            }
        }
        return replyObj;
    }

    public static String getSpecialAgent(String serviceId, JsonObject billpay_cfg) {
        String key = "billerid." + serviceId + ".target";
        String specialAgent = billpay_cfg.getString(key, "");
        return specialAgent;
    }

    public static ArrayList<Object> readFile(String fileName, Common.BuildLog log) {
        BufferedReader br = null;
        ArrayList<Object> arrayList = new ArrayList<>();

        try {
            try {
                br = new BufferedReader(new FileReader(fileName));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String line = null;
            try {
                line = br.readLine().trim();
                while (line != null && !"".equalsIgnoreCase(line)) {
                    arrayList.add(line);
                    line = br.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
                log.add("error", e.getMessage());
            }

        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                log.add("error", e.getMessage());
            }
        }
        return arrayList;
    }

    public static boolean writeFile(String data, String filePath) {
        boolean result = true;
        BufferedWriter writer = null;
        try {

            //create a temporary file
            java.io.File logFile = new java.io.File(filePath);
            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(data);
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
                result = false;
            }
        }
        return result;
    }

    public static SoapProto.keyValuePair.Builder buildKeyValuePairForSoap(String key, String value) {
        return SoapProto.keyValuePair.newBuilder().setKey(key)
                .setValue(value);
    }

    public static HashMap<String, String> getHashMapInSoap(List<SoapProto.keyValuePair> valuePairList) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (valuePairList == null) return hashMap;
        for (int i = 0; i < valuePairList.size(); i++) {
            hashMap.put(valuePairList.get(i).getKey(), valuePairList.get(i).getValue());
        }
        return hashMap;
    }

    public static void sendTranHisAndNotiGift(final Vertx _vertx
            , final int phoneNumber
            , String tranComment
            , final long tranId
            , final long tranAmount
            , final Gift gift
            , final String notiCaption
            , final String notiBody
            , final String giftMessage
            , TransDb tranDb) {

        //them phan huong dan su dung qua vao transaction nhan qua tang
        String sufixTranComment = "\n Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";

        String fullTranComment = tranComment + sufixTranComment;

        final TranObj tran = new TranObj();
        long currentTime = System.currentTimeMillis();
        tran.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        tran.comment = fullTranComment;
        tran.tranId = tranId;
        tran.clientTime = currentTime;
        tran.ackTime = currentTime;
        tran.finishTime = currentTime;          //=> this must be the time we sync, or user will not sync this to device
        tran.partnerName = "MoMo";              //
        tran.partnerId = "";                    // "Chuyển tiền nhanh";
        tran.partnerRef = tranComment;           // for avatar neu la qua cua end-user, qua he thong creator = sys | ""
        tran.amount = tranAmount;
        tran.status = TranObj.STATUS_OK;
        tran.error = 0;
        tran.cmdId = System.currentTimeMillis();
        tran.parterCode = tran.comment;
        tran.billId = gift.getModelId();
        tran.owner_number = phoneNumber;

        tran.io = 1;
        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(_vertx, tran);

                    final Notification noti = new Notification();
                    noti.priority = 2;
                    noti.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
                    noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
                    noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                    noti.tranId = tranId;
                    noti.time = new Date().getTime();
                    noti.extra = new JsonObject()
                            .putString("giftId", gift.getModelId())
                            .putString("giftTypeId", gift.typeId)
                            .putString("amount", String.valueOf(tranAmount))
                            .putString("sender", "Chuyển tiền nhanh")
                            .putString("senderName", "MoMo")
                            .putString("msg", giftMessage)
                            .toString();

                    noti.receiverNumber = phoneNumber;
                    Misc.sendNoti(_vertx, noti);

                }
            }
        });
    }

    public static String getInfoByKey(String key, JsonArray senderInfo) {
        String data = "";
        for (int i = 0; i < senderInfo.size(); i++) {
            if (((JsonObject) senderInfo.get(i)).getFieldNames().contains(key)) {
                data = ((JsonObject) senderInfo.get(i)).getString(key);
                break;
            }
        }
        return data;
    }


    //retailer.start

    public static String getCoreValueByKey(String key, ArrayList<com.mservice.momo.gateway.internal.core.objects.KeyValue> list) {
        if (list == null || list.size() == 0) {
            return "";
        }
        for (int i = 0; i < list.size(); i++) {
            if (key.equalsIgnoreCase(list.get(i).Key)) {
                return list.get(i).Value;
            }
        }
        return "";
    }

    ///////todo so many try ??????????
    public static void readCfg(String filePath, HashMap<Integer, JsonObject> hashMap, Logger logger) {
        BufferedReader br = null;
        try {

            try {
                br = new BufferedReader(new FileReader(filePath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                String fullContent = sb.toString();
                JsonArray jsonArray = new JsonArray(fullContent);

                for (int i = 0; i < jsonArray.size(); i++) {

                    JsonObject jo = jsonArray.get(i);
                    for (String s : jo.getFieldNames()) {
                        hashMap.put(DataUtil.strToInt(s), jo.getObject(s));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("error", e);
            }

        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("error", e);
            }
        }
    }

    //retailer.end

    public static InformObj getContentFromCfg(HashMap<Integer, JsonObject> hm, int tranType, int error, Logger logger) {
        String failOrSuc = error == 0 ? "succ" : "fail";

        JsonObject jo = hm.get(tranType);
        if (jo == null) {
            logger.info("func getContentFromCfg KHONG LAY DUOC THONG "
                    + " tranType: " + MomoProto.TranHisV1.TranType.valueOf(tranType).name()
                    + " error: " + error
            );
            return new InformObj();
        }
        return new InformObj(jo.getArray(failOrSuc));
    }

    /**
     * @param serviceId the service id
     * @param billId    the bill id
     * @param strStart  the value start of string bill id
     * @return the billId after refined
     */
    public static String refineBillId(String serviceId, String billId, String strStart) {

        billId = billId == null ? "" : billId;
        //no need to refind billId
        if (!checkedList.contains(serviceId)) {
            return billId;
        }

        //begin refine
        String tmpStart = strStart.toLowerCase().trim();
        String tmpBillId = billId.toLowerCase().trim();

        //billid start with strStart String
        if (tmpBillId.startsWith(tmpStart)) {
            return billId;
        }
        //bill not start with strStart String
        return (tmpStart + tmpBillId);
    }

    public static JsonObject getStartAndEndDate(long time) {
        JsonObject jo = new JsonObject();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int endDate = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, endDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);

        long endTime = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 00);
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 0);
        long startTime = cal.getTimeInMillis();

        jo.putNumber("start", startTime);
        jo.putNumber("end", endTime);
        return jo;
    }

    public static void getCapsetAndM2mType(Vertx vertx
            , String numberFrom
            , String numberTo
            , long amount, Common.BuildLog log, final Handler<CapsetAndM2MType> callback) {

        numberFrom = "0" + DataUtil.strToInt(numberFrom);
        numberTo = "0" + DataUtil.strToInt(numberTo);
        log.add("begin", "PRO_M2M_CAPSET");
        log.add("phone", numberFrom);
        log.add("amount", amount);

        JsonObject json = new JsonObject();
//        json.putNumber(LStandbyOracleVerticle.COMMAND, LStandbyOracleVerticle.GET_M2M_TYPE_AND_CAPSET);
        json.putNumber("type", UMarketOracleVerticle.GET_M2M_TYPE_AND_CAPSET);
        json.putString("fromNumber", "0" + DataUtil.strToInt(numberFrom));
        json.putString("toNumber", "0" + DataUtil.strToInt(numberTo));
        json.putNumber("amount", amount);

//        vertx.eventBus().send(AppConstant.LStandbyOracleVerticle_ADDRESS, json, new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> result) {
        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, json, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> result) {
                JsonObject json = result.body();
                boolean isCapsetOk = json.getBoolean("isCapsetOk", true);
                String m2mtype = json.getString("m2mtype", "u2u");
                callback.handle(new CapsetAndM2MType(isCapsetOk, m2mtype));
            }
        });
    }

    public static JsonArray convertListToJsonArray(List<JsonObject> arrayList) {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = null;
        for (int i = 0; i < arrayList.size(); i++) {
            jsonObject = arrayList.get(i);
            jsonArray.addObject(jsonObject);
        }

        return jsonArray;
    }

    public static JsonArray sortJsonByTime(JsonArray objArray, String keySort) {
        List jsonObjectList = objArray.toList();

        jsonObjectList = sortListByTime(jsonObjectList, keySort);

        JsonArray jsonArray = convertListToJsonArray(jsonObjectList);

        return jsonArray;
    }

    public static List<JsonObject> sortListByTime(List<JsonObject> array, String keySort) {
        JsonObject jsonObject = new JsonObject();
        JsonObject lastJsonObject = new JsonObject();
        boolean hasChanged = false;
        long time = 0;
        long lastTime = 0;
        for (int i = 0; i < array.size(); i++) {
            jsonObject = array.get(i);
            time = jsonObject.getLong(keySort, 0);
            if (i > 0) {
                lastJsonObject = array.get(i - 1);
                lastTime = lastJsonObject.getLong(keySort, 0);
            }
            if (i == 0 && array.size() > 1) {
                array.add(i, jsonObject);
                hasChanged = true;
            } else if (time < lastTime) {
                array.add(i - 1, jsonObject);
                hasChanged = true;
            } else {
                array.add(i, jsonObject);
            }
        }
        if (hasChanged) {
            sortListByTime(array, keySort);
        }

        return array;
    }

    public static void sendTranHisAndNotiGiftForBillPay(final Vertx _vertx
            , final int phoneNumber
            , String tranComment
            , final long tranId
            , final long tranAmount
            , final Gift gift
            , final String notiCaption
            , final String notiBody
            , final String giftMessage
            , TransDb tranDb) {

        //them phan huong dan su dung qua vao transaction nhan qua tang
//        String sufixTranComment = "\n Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";

        String fullTranComment = tranComment; //+ sufixTranComment;
        JsonObject jsonShare = new JsonObject();
        JsonObject jsonHideLink = new JsonObject();
        final TranObj tran = new TranObj();
        long currentTime = System.currentTimeMillis();
        tran.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        tran.comment = fullTranComment;
        tran.tranId = tranId;
        tran.clientTime = currentTime;
        tran.ackTime = currentTime;
        tran.finishTime = currentTime;          //=> this must be the time we sync, or user will not sync this to device
        tran.partnerName = "Trải nghiệm thanh toán \n" + "Cơ hội nhận đến";              //
        tran.partnerId = "";                    // "Chuyển tiền nhanh";
        tran.partnerRef = tranComment;           // for avatar neu la qua cua end-user, qua he thong creator = sys | ""
        tran.amount = tranAmount;
        tran.status = TranObj.STATUS_OK;
        tran.error = 0;
        tran.cmdId = System.currentTimeMillis();
        tran.parterCode = tran.comment;
        tran.billId = gift.getModelId();
        tran.owner_number = phoneNumber;
        tran.io = 1;
//        jsonShare.putString("html", PromoContentNotification.HTMLSTR);
        jsonHideLink.putString("giftdetaillink", "false");
//        tran.share.addObject(jsonShare);
        tran.share.addObject(jsonHideLink);
        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (result) {
                    BroadcastHandler.sendOutSideTransSync(_vertx, tran);

                    final Notification noti = new Notification();
                    noti.priority = 2;
                    noti.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
                    noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
                    noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                    noti.tranId = tranId;
                    noti.time = new Date().getTime();
                    noti.extra = new JsonObject()
                            .putString("giftId", gift.getModelId())
                            .putString("giftTypeId", gift.typeId)
                            .putString("amount", String.valueOf(tranAmount))
                            .putString("sender", "Trải nghiệm thanh toán - Tặng 200.000")
                            .putString("senderName", "MoMo")
                            .putString("msg", giftMessage)
                            .toString();

                    noti.receiverNumber = phoneNumber;
                    Misc.sendNoti(_vertx, noti);

                }
            }
        });
    }

    public static void sendTranHisAndNotiBillPayGift(final Vertx _vertx
            , final int phoneNumber
            , String tranComment
            , final long tranId
            , final long tranAmount
            , final Gift gift
            , final String notiCaption
            , final String notiBody
            , final String giftMessage
            , TransDb tranDb) {

        //them phan huong dan su dung qua vao transaction nhan qua tang
//        String sufixTranComment = "\n Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";

        String fullTranComment = tranComment;
        JsonObject jsonShare = new JsonObject();
        JsonObject jsonHideLink = new JsonObject();
        final TranObj tran = new TranObj();
        long currentTime = System.currentTimeMillis();
        tran.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        tran.comment = fullTranComment;
        tran.tranId = tranId;
        tran.clientTime = currentTime;
        tran.ackTime = currentTime;
        tran.finishTime = currentTime;          //=> this must be the time we sync, or user will not sync this to device
        tran.partnerName = "Tri Ân Khách Hàng";              //
        tran.partnerId = "";                    // "Chuyển tiền nhanh";
        tran.partnerRef = tranComment;           // for avatar neu la qua cua end-user, qua he thong creator = sys | ""
        tran.amount = tranAmount;
        tran.status = TranObj.STATUS_OK;
        tran.error = 0;
        tran.cmdId = System.currentTimeMillis();
        tran.parterCode = tran.comment;
        tran.billId = gift.getModelId();
        tran.owner_number = phoneNumber;
        String html = "";
        String giftdetaillink = "true";
//        if (gift.status == 3) {
//            giftdetaillink = "false";
//            html = PromoContentNotification.HTMLSTR_SCREEN_GIFT;
//        }
        jsonHideLink.putString("giftdetaillink", giftdetaillink);
//        jsonShare.putString("html", html);
        tran.share.addObject(jsonShare);
        tran.share.addObject(jsonHideLink);
        tran.io = 1;
        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(_vertx, tran);
                    final Notification noti = new Notification();
                    noti.priority = 2;
                    noti.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
                    noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
                    noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                    noti.tranId = tranId;
                    noti.time = new Date().getTime();
                    noti.extra = new JsonObject()
                            .putString("giftId", gift.getModelId())
                            .putString("giftTypeId", gift.typeId)
                            .putString("amount", String.valueOf(tranAmount))
                            .putString("sender", "Tri Ân Khách Hàng")
                            .putString("senderName", "MoMo")
                            .putString("msg", giftMessage)
                            .putNumber("status", gift.status)
                            .putString("serviceid", gift.typeId)
                            .toString();

                    noti.receiverNumber = phoneNumber;
                    Misc.sendNoti(_vertx, noti);

                }
            }
        });
    }

    public static void sendTranHisAndNotiZaloMoney(final Vertx _vertx
            , final int phoneNumber
            , String tranComment
            , final long tranId
            , final long tranAmount
            , final Gift gift
            , final String notiCaption
            , final String notiBody
            , final String giftMessage
            , final String partnerName
            , final String serviceId
            , TransDb tranDb) {

        //them phan huong dan su dung qua vao transaction nhan qua tang
//        String sufixTranComment = "\n Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";

        String fullTranComment = tranComment;
        JsonObject jsonShare = new JsonObject();
        JsonObject jsonHideLink = new JsonObject();
        final TranObj tran = new TranObj();
        long currentTime = System.currentTimeMillis();
        tran.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        tran.comment = fullTranComment;
        tran.tranId = tranId;
        tran.clientTime = currentTime;
        tran.ackTime = currentTime;
        tran.finishTime = currentTime;          //=> this must be the time we sync, or user will not sync this to device
        tran.partnerName = partnerName;              //
        tran.partnerId = "MoMo";                    // "Chuyển tiền nhanh";
        tran.partnerRef = tranComment;           // for avatar neu la qua cua end-user, qua he thong creator = sys | ""
        tran.amount = tranAmount;
        tran.status = TranObj.STATUS_OK;
        tran.error = 0;
        tran.cmdId = System.currentTimeMillis();
        tran.parterCode = tran.comment;
        tran.billId = gift.getModelId();
        tran.owner_number = phoneNumber;
        String html = "";
        String giftdetaillink = "false";
//        if (gift.status == 3) {
//            giftdetaillink = "false";
//            html = PromoContentNotification.HTMLSTR_SCREEN_GIFT;
//        }
        jsonHideLink.putString("giftdetaillink", giftdetaillink);
//        jsonShare.putString("html", html);
        tran.share.addObject(jsonShare);
        tran.share.addObject(jsonHideLink);
        tran.io = 1;
        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(_vertx, tran);
                    final Notification noti = new Notification();
                    noti.priority = 2;
                    noti.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
                    noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
                    noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                    noti.tranId = tranId;
                    noti.time = new Date().getTime();
                    noti.extra = new JsonObject()
                            .putString("giftId", gift.getModelId())
                            .putString("giftTypeId", gift.typeId)
                            .putString("amount", String.valueOf(tranAmount))
                            .putString("sender", partnerName)
                            .putString("senderName", "MoMo")
                            .putString("msg", giftMessage)
                            .putNumber("status", gift.status)
                            .putString("serviceid", serviceId)
                            .putString("imageUrl", "")
                            .toString();

                    noti.receiverNumber = phoneNumber;
                    Misc.sendNoti(_vertx, noti);

                }
            }
        });
    }

    public static void sendTranHisAndNotiIronManGift(final Vertx _vertx
            , final int phoneNumber
            , String tranComment
            , final long tranId
            , final long tranAmount
            , final Gift gift
            , final String notiCaption
            , final String notiBody
            , final String giftMessage
            , TransDb tranDb, final String source) {

        //them phan huong dan su dung qua vao transaction nhan qua tang
//        String sufixTranComment = "\n Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";

        String fullTranComment = tranComment;
        JsonObject jsonShare = new JsonObject();
        JsonObject jsonHideLink = new JsonObject();
        final TranObj tran = new TranObj();
        long currentTime = System.currentTimeMillis();
        tran.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
        tran.comment = fullTranComment;
        tran.tranId = tranId;
        tran.clientTime = currentTime;
        tran.ackTime = currentTime;
        tran.finishTime = currentTime;          //=> this must be the time we sync, or user will not sync this to device
        tran.partnerName = "Trải nghiệm thanh toán";              //
        tran.partnerId = "";                    // "Chuyển tiền nhanh";
        tran.partnerRef = tranComment;           // for avatar neu la qua cua end-user, qua he thong creator = sys | ""
        tran.amount = tranAmount;
        tran.status = TranObj.STATUS_OK;
        tran.error = 0;
        tran.cmdId = System.currentTimeMillis();
        tran.parterCode = tran.comment;
        tran.billId = gift.getModelId();
        tran.owner_number = phoneNumber;
        String html = "";
        String giftdetaillink = "true";
        if (gift.status == 3) {
            giftdetaillink = "false";
            //html = PromoContentNotification.HTMLSTR_GIFT.replaceAll("serviceid", gift.typeId);
            html = PromoContentNotification.HTMLSTR_SCREEN_GIFT;
        }
        jsonHideLink.putString("giftdetaillink", giftdetaillink);
//        jsonShare.putString("html", html);
//        tran.share.addObject(jsonShare);
        tran.share.addObject(jsonHideLink);
        tran.io = 1;
        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(_vertx, tran);

                    final Notification noti = new Notification();
                    noti.priority = 2;
                    int type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
                    if (source.equalsIgnoreCase(StringConstUtil.IronManPromo.IRON_PROMO_1)) {
                        type = MomoProto.NotificationType.NOTI_TOPUP_VALUE;
                        ;
                    } else {
                        type = MomoProto.NotificationType.NOTI_VOUCHER_VIEW_VALUE;
                    }
                    noti.type = type;
                    noti.caption = notiCaption;// "Nhận thưởng quà khuyến mãi";
                    noti.body = notiBody;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
//                    noti.tranId = tranId;
                    noti.tranId = System.currentTimeMillis();
                    noti.time = new Date().getTime();
                    noti.extra = new JsonObject()
                            .putString("giftId", gift.getModelId())
                            .putString("giftTypeId", gift.typeId)
                            .putString("amount", String.valueOf(tranAmount))
                            .putString("sender", "Trải nghiệm thanh toán")
                            .putString("senderName", "MoMo")
                            .putString("msg", giftMessage)
                            .putNumber("status", gift.status)
                            .putString("serviceid", gift.typeId)
                            .toString();

                    noti.receiverNumber = phoneNumber;
                    Misc.sendNoti(_vertx, noti);

                }
            }
        });
    }


    //BEGIN 0000000004

    public static JSONArray readJsonFile(String filename) {
        JSONParser parser = new JSONParser();
        JsonArray jsonArray = new JsonArray();
        JSONArray a = new JSONArray();
        try {
            a = (JSONArray) parser.parse(new FileReader(filename));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }

        return a;
    }

    public static JsonObject readJsonObjectFile(String filename) {
        BufferedReader br = null;
        JsonObject jsonObject = new JsonObject();
        try {

            try {
                br = new BufferedReader(new FileReader(filename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                String fullContent = sb.toString();
                jsonObject = new JsonObject(fullContent);

            } catch (IOException e) {
                e.printStackTrace();
//                log.add("error", e.getMessage());
            }

        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
//                log.add("error", e.getMessage());
            }
        }
        return jsonObject;
    }

    public static void sendingStandardTransHisFromJson(final Vertx vertx, TransDb transDb, JsonObject jsonObject, JsonObject jsonExtra) {
        int tranType = jsonObject.getInteger(colName.TranDBCols.TRAN_TYPE, 0);
        String comment = jsonObject.getString(colName.TranDBCols.COMMENT, "");
        long tranId = jsonObject.getLong(colName.TranDBCols.TRAN_ID, 0);
        long amount = jsonObject.getLong(colName.TranDBCols.AMOUNT, 0);
        int status = jsonObject.getInteger(colName.TranDBCols.STATUS, 0);
        int number = jsonObject.getInteger(colName.TranDBCols.OWNER_NUMBER, 0);
        String billId = jsonObject.getString(colName.TranDBCols.BILL_ID, "");
        String partnerName = jsonObject.getString(colName.TranDBCols.PARTNER_NAME, "");
        String partnerId = jsonObject.getString(colName.TranDBCols.PARTNER_ID, "");
        String partnerCode = jsonObject.getString(colName.TranDBCols.PARTNER_CODE, "");
        String desc = jsonObject.getString(colName.TranDBCols.DESCRIPTION, "");

        String partnerNameFinal = "".equalsIgnoreCase(partnerName) ? "M_Service" : partnerName;
        String partnerIdFinal = "".equalsIgnoreCase(partnerId) ? "MoMo" : partnerId;

        String html = jsonObject.getString(StringConstUtil.HTML, "");
        int io = jsonObject.getInteger(colName.TranDBCols.IO, -1);
        //Send tranhis

        final TranObj mainObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        mainObj.tranType = tranType;
        mainObj.comment = comment;
        mainObj.tranId = tranId;
        mainObj.clientTime = currentTime;
        mainObj.ackTime = currentTime;
        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = amount;
        mainObj.status = status;
        mainObj.error = 0;
        mainObj.io = io;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.owner_number = number;
        mainObj.partnerName = partnerNameFinal;
        mainObj.partnerId = partnerIdFinal;
        mainObj.parterCode = partnerCode;
        mainObj.billId = billId;
        mainObj.partnerRef = mainObj.comment;
        if(!Misc.isNullOrEmpty(desc)){
            mainObj.desc = desc;
        }
        JsonObject jsonHtml = new JsonObject();
        JsonArray jsonArrayShare = new JsonArray();
        jsonHtml.putString(StringConstUtil.HTML, html);
        jsonArrayShare.addObject(jsonHtml);
        jsonArrayShare.addObject(jsonExtra);
        mainObj.share = jsonArrayShare;
//        Misc.sendTranAsSynWithOutSock(transDb, mainObj, common);
        transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                }
            }
        });
    }

    public static void sendingStandardTransHisFromJsonWithCallback(final Vertx vertx, TransDb transDb, JsonObject jsonObject, JsonObject jsonExtra, final Handler<JsonObject> callback) {
        int tranType = jsonObject.getInteger(colName.TranDBCols.TRAN_TYPE, 0);
        String comment = jsonObject.getString(colName.TranDBCols.COMMENT, "");
        long tranId = jsonObject.getLong(colName.TranDBCols.TRAN_ID, 0);
        long amount = jsonObject.getLong(colName.TranDBCols.AMOUNT, 0);
        int status = jsonObject.getInteger(colName.TranDBCols.STATUS, 0);
        int number = jsonObject.getInteger(colName.TranDBCols.OWNER_NUMBER, 0);
        String billId = jsonObject.getString(colName.TranDBCols.BILL_ID, "");
        String partnerName = jsonObject.getString(colName.TranDBCols.PARTNER_NAME, "");
        String partnerId = jsonObject.getString(colName.TranDBCols.PARTNER_ID, "");

        String partnerNameFinal = "".equalsIgnoreCase(partnerName) ? "M_Service" : partnerName;
        String partnerIdFinal = "".equalsIgnoreCase(partnerId) ? "MoMo" : partnerId;

        String html = jsonObject.getString(StringConstUtil.HTML, "");
        int io = jsonObject.getInteger(colName.TranDBCols.IO, -1);
        //Send tranhis

        final TranObj mainObj = new TranObj();
        long currentTime = System.currentTimeMillis();
        mainObj.tranType = tranType;
        mainObj.comment = comment;
        mainObj.tranId = tranId;
        mainObj.clientTime = currentTime;
        mainObj.ackTime = currentTime;
        mainObj.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount = amount;
        mainObj.status = status;
        mainObj.error = 0;
        mainObj.io = io;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.owner_number = number;
        mainObj.partnerName = partnerNameFinal;
        mainObj.partnerId = partnerIdFinal;
        mainObj.billId = billId;
        mainObj.partnerRef = mainObj.comment;
        JsonObject jsonHtml = new JsonObject();
        JsonArray jsonArrayShare = new JsonArray();
        jsonHtml.putString(StringConstUtil.HTML, html);
        jsonArrayShare.addObject(jsonHtml);
        jsonArrayShare.addObject(jsonExtra);
        mainObj.share = jsonArrayShare;
//        Misc.sendTranAsSynWithOutSock(transDb, mainObj, common);
        transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {
                if (!result) {
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);
                    callback.handle(new JsonObject().putNumber(StringConstUtil.ERROR, 0));
                }
            }
        });
    }

    public static void sendStandardNoti(Vertx vertx, JsonObject joNoti) {

        String caption = joNoti.getString(StringConstUtil.StandardNoti.CAPTION, "");
        String body = joNoti.getString(StringConstUtil.StandardNoti.BODY, "");
        String receiver = joNoti.getString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, "");
        long tid = joNoti.getLong(StringConstUtil.StandardNoti.TRAN_ID, 0);

        final Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        noti.caption = caption;// "Nhận thưởng quà khuyến mãi";
        noti.body = body;//"Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank- Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
        noti.tranId = tid;
        noti.time = new Date().getTime();

        noti.receiverNumber = DataUtil.strToInt(receiver);
        Misc.sendNoti(vertx, noti);
    }

    /**
     * Cong Nguyen 13/07/2016
     * @param vertx
     * @param joNoti
     */
    public static void sendRedirectNoti(Vertx vertx, JsonObject joNoti) {

        String caption = joNoti.getString(StringConstUtil.StandardNoti.CAPTION, "");
        String body = joNoti.getString(StringConstUtil.StandardNoti.BODY, "");
        String receiver = joNoti.getString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, "");
        long tid = joNoti.getLong(StringConstUtil.StandardNoti.TRAN_ID, 0);
        String url = joNoti.getString(StringConstUtil.RedirectNoti.URL,"");

        final Notification noti = new Notification();
        noti.priority = 2;
        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
        noti.caption = caption;
        noti.body = body;
        noti.tranId = tid;
        noti.time = new Date().getTime();
        noti.extra = new JsonObject().putString(StringConstUtil.RedirectNoti.URL, url).toString();
        noti.receiverNumber = DataUtil.strToInt(receiver);
        Misc.sendNoti(vertx, noti);
        return;
    }

    public static void getDataFromConnector(final String number, final String host, final int port, final String path, final Vertx vertx, final Common.BuildLog log, final String serviceId, final JsonObject jsonInfo, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();

        // open connect to connector
        HttpClient connectorHttpClient = vertx.createHttpClient()
                .setHost(host)
                .setPort(port)
                .setConnectTimeout(120000) // 2 phut
                .setKeepAlive(false).setMaxPoolSize(20);
        String servicePath = "http://" + host + ":" + port + path;
        log.add("service path", servicePath);
        log.add("service id", serviceId);
        log.writeLog();
        //todo check replace servicePath to path
        AtomicInteger atomicCount = new AtomicInteger(0);
//        requestHttpPostToConnector(vertx, path, log, serviceId, jsonInfo, callback, joReply, connectorHttpClient, atomicCount);
        requestHttpPostVerticleToConnector(vertx, path, log, serviceId, jsonInfo, callback, port, host, number);
    }

    public static void getDataForPaymentFromConnector(final String phone_number, final String host, final int port, final JsonObject globalConfig, final String path, final Vertx vertx, final Common.BuildLog log, final String serviceId, final JsonObject jsonInfo, final Handler<JsonObject> callback) {
        final JsonObject joReply = new JsonObject();

        // open connect to connector
        final HttpClient connectorHttpClient = vertx.createHttpClient()
                .setHost(host)
                .setPort(port)
                .setConnectTimeout(120000) // 2 phut
                .setKeepAlive(false);
        String servicePath = "http://" + host + ":" + port + path;
        log.add("service path", servicePath);
        log.add("service id", serviceId);
        log.writeLog();
        //todo check replace servicePath to path
        AtomicInteger atomicCount = new AtomicInteger(0);
        requestHttpPostVerticleToConnector(vertx, path, log, serviceId, jsonInfo, callback, port, host, phone_number);
//        requestPaymentHttpPostToConnector(path, log, serviceId, jsonInfo, callback, joReply, connectorHttpClient, atomicCount);
    }

//    private static void requestHttpPostToConnector(final Vertx vertx, final String path, final Common.BuildLog log, final String serviceId,final JsonObject jsonInfo, final Handler<JsonObject> callback, final JsonObject joReply, final HttpClient connectorHttpClient,final AtomicInteger atomicCount) {
//        final Buffer bufferData = new Buffer();
//
//        final HttpClientRequest httpClientRequest = connectorHttpClient.post(path, new Handler<HttpClientResponse>() {
//            @Override
//            public void handle(final HttpClientResponse httpClientResponse) {
//
//
//                int statusCode = httpClientResponse != null ? httpClientResponse.statusCode() : 1000;
//
//                if (statusCode != 200) {
//
//                    log.add(StringConstUtil.DESCRIPTION, "Proxy " + serviceId + " is offline or internal error. Http status code = " + statusCode);
//                    log.writeLog();
//                    callback.handle(joReply.putNumber(StringConstUtil.ERROR, (statusCode + 10000)));
//                    log.add(StringConstUtil.ERROR, statusCode);
//                    log.writeLog();
//                    return;
//                }
//
//                httpClientResponse.bodyHandler(new Handler<Buffer>() {
//                    @Override
//                    public void handle(final Buffer bodyBuffer) {
//                        try {
//                            vertx.setTimer(500L, new Handler<Long>() {
//                                @Override
//                                public void handle(Long timer) {
//                                    String data = "".equalsIgnoreCase(bodyBuffer.toString()) ? bufferData.toString() : bodyBuffer.toString();
//                                    log.add("Rcv buffer", data);
//                                    int count = atomicCount.decrementAndGet();
//                                    if ("".equalsIgnoreCase(data) && count < 0) {
//                                        joReply.putNumber(StringConstUtil.ERROR, 8888);
//                                        joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
//                                        callback.handle(joReply);
//                                        log.add("json res", joReply);
//                                        vertx.cancelTimer(timer);
//                                        log.writeLog();
//                                        return;
//                                    } else if ("".equalsIgnoreCase(data) && count >= 0) {
//                                        log.add("desc", "call again HTTP");
//                                        requestHttpPostToConnector(vertx, path, log, serviceId, jsonInfo, callback, joReply, connectorHttpClient, atomicCount);
//                                        return;
//                                    }
//                                    JsonObject result = new JsonObject(data);
//                                    log.add("Rcv json", result.toString());
//                                    joReply.putNumber(StringConstUtil.ERROR, 0);
//                                    joReply.putObject(BankHelperVerticle.DATA, result);
//                                    vertx.cancelTimer(timer);
//                                    callback.handle(joReply);
//                                }
//                            });
//                        } catch (DecodeException e) {
//                            log.add("ex", e.fillInStackTrace());
//                            joReply.putNumber(StringConstUtil.ERROR, 9999);
//                            joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
//                            callback.handle(joReply);
//                        }
//                        log.add("json res", joReply);
//                        log.writeLog();
//                        //todo check
//                        connectorHttpClient.close();
//                        return;
//                    }
//                });
//
//                httpClientResponse.dataHandler(new Handler<Buffer>() {
//                    @Override
//                    public void handle(Buffer dataBuffer) {
//                        log.add("data", dataBuffer);
//                        bufferData.appendBuffer(dataBuffer);
//
//                        //=======================
////                        endDataHandler(httpClientResponse, log, bufferData, atomicCount, joReply, serviceId, callback, path, jsonInfo, connectorHttpClient);
//                    }
//                });
//
////                httpClientResponse.endHandler(new Handler<Void>() {
////                    @Override
////                    public void handle(final Void voidEndHandler) {
////
////                        vertx.setTimer(1000L, new Handler<Long>() {
////                            @Override
////                            public void handle(Long timer) {
////                                try {
////                                    log.add("Rcv buffer", bufferData.toString());
////                                    int count = atomicCount.decrementAndGet();
////                                    if("".equalsIgnoreCase(bufferData.toString()) && count < 0)
////                                    {
////                                        joReply.putNumber(StringConstUtil.ERROR, 8888);
////                                        joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
////                                        callback.handle(joReply);
////                                        log.add("json res", joReply);
////                                        log.writeLog();
////                                        return;
////                                    }
////                                    else if("".equalsIgnoreCase(bufferData.toString()) && count >= 0)
////                                    {
////                                        log.add("desc", "call again HTTP");
////                                        requestHttpPostToConnector(vertx, path, log, serviceId, jsonInfo, callback, joReply, connectorHttpClient, atomicCount);
////                                        return;
////                                    }
////                                    JsonObject result = new JsonObject(bufferData.toString());
////                                    log.add("Rcv json", result.toString());
////                                    joReply.putNumber(StringConstUtil.ERROR, 0);
////                                    joReply.putObject(BankHelperVerticle.DATA, result);
////
////                                    callback.handle(joReply);
////                                } catch (DecodeException e) {
////                                    log.add("ex", e.fillInStackTrace());
////                                    joReply.putNumber(StringConstUtil.ERROR, 9999);
////                                    joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
////                                    callback.handle(joReply);
////                                }
////                                log.add("json res", joReply);
////                                log.writeLog();
////                                //todo check
////                                connectorHttpClient.close();
////                                vertx.cancelTimer(timer);
////                                return;
////                            }
////                        });
////                    }
////                });
//
//                httpClientResponse.exceptionHandler(new Handler<Throwable>() {
//                    @Override
//                    public void handle(Throwable throwable) {
//                        joReply.putNumber(StringConstUtil.ERROR, 9999);
//                        joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
//                        callback.handle(joReply);
//                        log.add("json res", joReply);
//                        log.writeLog();
//                        //todo check
//                        connectorHttpClient.close();
//                        return;
//                    }
//                });
//            }
//        });
//        Buffer buffer = new Buffer(jsonInfo.toString());
//        httpClientRequest.putHeader("Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
//        log.add("Add Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
//        httpClientRequest.setTimeout(120000L);
//        httpClientRequest.putHeader("Content-Length", buffer.length() + "");
//        httpClientRequest.write(buffer).end();
//        log.add("Add Json data", jsonInfo);
//    }

    private static void requestPaymentHttpPostToConnector(final String path, final Common.BuildLog log, final String serviceId, final JsonObject jsonInfo, final Handler<JsonObject> callback, final JsonObject joReply, final HttpClient connectorHttpClient, final AtomicInteger atomicCount) {
        HttpClientRequest httpClientRequest = connectorHttpClient.post(path, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse httpClientResponse) {
                int statusCode = httpClientResponse.statusCode();

                if (statusCode != 200) {

                    log.add(StringConstUtil.DESCRIPTION, "Proxy " + serviceId + " is offline or internal error. Http status code = " + statusCode);
                    log.writeLog();
                    callback.handle(joReply.putNumber(StringConstUtil.ERROR, (statusCode + 10000)));
                    log.add(StringConstUtil.ERROR, statusCode);
                    return;
                }

                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            log.add("Rcv buffer", buffer.toString());
                            int count = atomicCount.decrementAndGet();
                            if ("".equalsIgnoreCase(buffer.toString()) && count < 0) {
                                joReply.putNumber(StringConstUtil.ERROR, 8888);
                                joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
                                callback.handle(joReply);
                                log.add("json res", joReply);
                                log.writeLog();
                                return;
                            } else if ("".equalsIgnoreCase(buffer.toString()) && count >= 0) {
                                log.add("desc", "call again HTTP");
                                jsonInfo.putNumber("requestType", 23);
                                requestPaymentHttpPostToConnector(path, log, serviceId, jsonInfo, callback, joReply, connectorHttpClient, atomicCount);
                                return;
                            }
                            JsonObject result = new JsonObject(buffer.toString());
                            log.add("Rcv json", result.toString());
                            joReply.putNumber(StringConstUtil.ERROR, 0);
                            joReply.putObject(BankHelperVerticle.DATA, result);
                            callback.handle(joReply);
                        } catch (DecodeException e) {
                            log.add("ex", e.fillInStackTrace());
                            joReply.putNumber(StringConstUtil.ERROR, 9999);
                            joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
                            callback.handle(joReply);
                        }
                        log.add("json res", joReply);
                        log.writeLog();
                        //todo check
//                        connectorHttpClient.close();
                        return;
                    }
                });

                httpClientResponse.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        joReply.putNumber(StringConstUtil.ERROR, 9999);
                        joReply.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
                        callback.handle(joReply);
                        log.add("json res", joReply);
                        log.writeLog();
                        //todo check
                        connectorHttpClient.close();
                        return;
                    }
                });
            }
        });
        Buffer buffer = new Buffer(jsonInfo.toString());
        httpClientRequest.putHeader("Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);
        log.add("Add Content-Type", BankHelperVerticle.CONTENT_TYPE_DEFAULT);

        httpClientRequest.putHeader("Content-Length", buffer.length() + "");
        httpClientRequest.write(buffer).end();
        log.add("Add Json data", jsonInfo);
    }

    private static void requestHttpPostVerticleToConnector(final Vertx vertx, final String path, final Common.BuildLog log, final String serviceId, final JsonObject jsonInfo, final Handler<JsonObject> callback, final int port, final String host, final String phoneNumber) {

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.putString(StringConstUtil.NUMBER, phoneNumber);
        jsonRequest.putString(StringConstUtil.ConnectorNotification.HOST, host);
        jsonRequest.putNumber(StringConstUtil.ConnectorNotification.PORT, port);
        jsonRequest.putString(StringConstUtil.ConnectorNotification.PATH, path);
        jsonRequest.putObject(StringConstUtil.ConnectorNotification.JSON_INFO, jsonInfo);
        jsonRequest.putString(StringConstUtil.SERVICE_ID, serviceId);


        vertx.eventBus().sendWithTimeout(AppConstant.HTTP_POST_CONNECTOR_BUS_ADDRESS, jsonRequest, 120000L, new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> responseData) {
                JsonObject jsonRespond = new JsonObject();
                if (responseData.succeeded()) {
                    jsonRespond = responseData.result().body();
                    callback.handle(jsonRespond);
                } else {
                    jsonRespond.putNumber(StringConstUtil.ERROR, 9999);
                    jsonRespond.putString(StringConstUtil.DESCRIPTION, "Unexpected result. " + serviceId);
                    callback.handle(jsonRespond);
                }
            }
        });
    }

    //END 0000000004

    public static boolean writeAppendFile(String data, String filePath) {
        boolean result = true;
        BufferedWriter writer = null;
        try {

            //create a temporary file
            java.io.File logFile = new java.io.File(filePath);
            writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.append(data);
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
                result = false;
            }
        }
        return result;
    }

    //END 0000000056 IronMan_PLUS
    public static void setGroupVisa(String group, String capsetId, String upperLimit, GroupManageDb groupManageDb, final Vertx vertx, final Logger logger, final String phoneNumber) {
        final Buffer buffer = MomoMessage.buildBuffer(SoapProto.MsgType.MAP_AGENT_TO_VISA_GROUP_VALUE
                , 0
                , DataUtil.strToInt(phoneNumber)
                , SoapProto.ZaloGroup.newBuilder()
                        .setZaloGroup(group)
                        .setZaloCapsetId(capsetId)
                        .setZaloUpperLimit(upperLimit)
                        .build().toByteArray());
        GroupManageDb.Obj groupObj = new GroupManageDb.Obj();
        groupObj.number = phoneNumber;
        groupObj.groupid = group;
        groupManageDb.insert(groupObj, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {
                vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<Boolean>>() {
                    @Override
                    public void handle(Message<Boolean> reply) {
                        logger.info(DataUtil.strToInt(phoneNumber) + ",set group visa " + reply.body());
                    }
                });
            }
        });

    }

    public static void saveErrorPromotionInfo(ErrorPromotionTrackingDb errorPromotionTrackingDb, String phoneNumber, String program, int error, String desc)
    {
        String deviceInfo = "";
        String description = "";
        if(Misc.isValidJsonObject(desc))
        {
            JsonObject joDesc = new JsonObject(desc);
            deviceInfo = joDesc.getString(StringConstUtil.DEVICE_IMEI, "");
            description = joDesc.getString(StringConstUtil.DESCRIPTION, "");
        }

        desc = "".equalsIgnoreCase(description) ? desc : description;

        ErrorPromotionTrackingDb.Obj errorObj = new ErrorPromotionTrackingDb.Obj();
        errorObj.phone = phoneNumber;
        errorObj.program = program;
        errorObj.time = System.currentTimeMillis();
        errorObj.error_code = error;
        errorObj.desc = desc;
        errorObj.deviceInfo = deviceInfo;
        errorPromotionTrackingDb.insert(program, errorObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {

            }
        });
    }

    public static boolean isNullOrEmpty(String s) {
        if (s == null) {
            return true;
        } else {
            String s1 = s.trim();
            return "".equals(s1);
        }
    }

    public static JsonObject makeHttpPostWrapperData(String bus, JsonObject data) {
        return new JsonObject()
                .putString(Const.INTERNAL_HTTP_POST_CFG.BUS, bus)
                .putObject(Const.INTERNAL_HTTP_POST_CFG.DATA, data);
    }

    public static String encrypt(String type, String salt, String key, String value) {
        if (value == null) {
            return null;
        } else {
            byte[][] ciphertext = new byte[2][];

            try {
                SecretKeyFactory ct = SecretKeyFactory.getInstance("DESede");
                if (ct == null) {
                    throw new GeneralSecurityException("No DESede factory");
                }

                byte[] result = secret;
                SecretKey enc = ct.generateSecret(new DESedeKeySpec(result));
                if (enc == null) {
                    throw new GeneralSecurityException("No secret key");
                }

                Cipher plaintext = Cipher.getInstance("DESede/CBC/PKCS5Padding");
                byte[] iv = generateIV(type, salt, key);
                plaintext.init(1, enc, new IvParameterSpec(iv));
                ciphertext[0] = plaintext.update(stringToByteArray(value));
                ciphertext[1] = plaintext.doFinal(generateMAC(type, salt, key, value));
            } catch (GeneralSecurityException var11) {
                return null;
            }

            if (ciphertext[0] == null) {
                ciphertext[0] = new byte[0];
            }

            if (ciphertext[1] == null) {
                ciphertext[1] = new byte[0];
            }

            byte[] ct1 = new byte[ciphertext[0].length + ciphertext[1].length];
            System.arraycopy(ciphertext[0], 0, ct1, 0, ciphertext[0].length);
            System.arraycopy(ciphertext[1], 0, ct1, ciphertext[0].length, ciphertext[1].length);
            StringBuffer result1 = new StringBuffer(2 * ct1.length + 1);
            result1.append("b");
            result1.append(new Base64().encode(ct1));
            String plaintext1 = getSafePlaintext(type, key, value);
            if (plaintext1 != null) {
                result1.append(':').append(plaintext1);
            }

            return result1.toString();
        }
    }

    public static String getSafePlaintext(String type, String key, String value) {
        return null;
    }

    private static byte[] generateIV(String type, String salt, String key) throws NoSuchAlgorithmException {
        try {
            MessageDigest ex = MessageDigest.getInstance("SHA-1");
            if (type != null) {
                ex.update(stringToByteArray(type));
            }

            if (salt != null) {
                ex.update(stringToByteArray(salt));
            }

            if (key != null) {
                ex.update(stringToByteArray(key));
            }

            byte[] sha = ex.digest();
            byte[] iv = new byte[8];

            for (int scan = 0; scan < sha.length; ++scan) {
                iv[scan % 8] ^= sha[scan];
            }

            return iv;
        } catch (NoSuchAlgorithmException var8) {
            throw new IllegalStateException(var8.getMessage());
        }
    }

    private static byte[] generateMAC(String type, String salt, String key, String value) {
        try {
            MessageDigest ex = MessageDigest.getInstance("SHA-1");
            if (type != null) {
                ex.update(stringToByteArray(type));
            }

            if (salt != null) {
                ex.update(stringToByteArray(salt));
            }

            if (key != null) {
                ex.update(stringToByteArray(key));
            }

            if (value != null) {
                ex.update(stringToByteArray(value));
            }

            byte[] sha = ex.digest();
            byte[] mac = new byte[4];

            for (int scan = 0; scan < sha.length; ++scan) {
                mac[scan % 4] ^= sha[scan];
            }

            return mac;
        } catch (NoSuchAlgorithmException var9) {
            throw new IllegalStateException(var9.getMessage());
        }
    }

    private static byte[] stringToByteArray(String aString) {
        try {
            return aString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public static void writeLogWithFormat(Logger logger, MomoMessage msg)
    {

    }

    public static void makeDBRequest(Vertx vertx, Logger log, final JsonObject glbConfig, final String path,
                                     final DBMsg dbMsg, final Handler<DBMsg> callback) {

        String bus = AppConstant.NOTI_DB_BUS;
        if (DBFactory.Source.TRAN_PATH.equalsIgnoreCase(path)) {
            bus = AppConstant.TRAN_DB_BUS;
        }
        vertx.eventBus().sendWithTimeout(bus, JacksonJSONUtils.objToJsonObj(dbMsg), Const.MYSQL.TIMEOUT, new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                if (messageAsyncResult.succeeded()) {
                    JsonObject res = messageAsyncResult.result().body();
                    callback.handle(JacksonJSONUtils.jsonToObj(res, DBMsg.class));
                } else {
                    dbMsg.err = 400;
                    dbMsg.des = messageAsyncResult.cause().getMessage();
                    callback.handle(dbMsg);
                }
            }
        });
    }

    /**
     * Cong Nguyen 11/07/2016
     * send popup to EU
     *
     * @param phoneNumber
     * @param body
     * @param title
     */
    public static void sendPopupReferral(Vertx vertx, String phoneNumber, String body, String title, String url) {
        JsonObject jsonExtra = new JsonObject();
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, StringConstUtil.VIEW_HELP_BUTTON_TITLE);
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, StringConstUtil.CLOSE_BUTTON_TITLE);
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, false);
        jsonExtra.putString(StringConstUtil.RedirectNoti.URL, url);
        jsonExtra.putNumber(StringConstUtil.TYPE, 3);
        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, StringConstUtil.INFO_POPUP);
        Notification notification = new Notification();
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = title;
        notification.body = body;
        notification.cmdId = 0L;
        notification.time = System.currentTimeMillis();
        notification.receiverNumber = Integer.parseInt(phoneNumber);
        notification.extra = jsonExtra.toString();
        sendNoti(vertx, notification);
    }

    public static void sendEmail(JsonObject joConfig, String title, String content, Common.BuildLog log) {
        log.add("func", "sendEmail");
        JsonObject joMail = joConfig.getObject(StringConstUtil.EMAIL.JSON_OBJECT, new JsonObject());
        String host = joMail.getString(StringConstUtil.EMAIL.HOST, "");
        int port = joMail.getInteger(StringConstUtil.EMAIL.PORT, 0);
        String senderEmail = joMail.getString(StringConstUtil.EMAIL.SENDER_EMAIL, "");
        String senderPass = joMail.getString(StringConstUtil.EMAIL.SENDER_PASS, "");
        boolean tls = joMail.getBoolean(StringConstUtil.EMAIL.TLS, false);
        JsonArray jArrToEmail = joMail.getArray(StringConstUtil.EMAIL.RECEIVER_TO_EMAIL, new JsonArray());
        JsonArray jArrCCEmail = joMail.getArray(StringConstUtil.EMAIL.RECEIVER_CC_EMAIL, new JsonArray());
        String from = joMail.getString(StringConstUtil.EMAIL.FROM, "");

        log.add("content", content);
        HtmlEmail email = new HtmlEmail();
        try {
            email.setHostName(host); //HostName
            email.setAuthentication(senderEmail, senderPass); //User pass
            email.setSmtpPort(port); //Port
            email.setTLS(tls); //TLS True or false
            for (int i = 0; i < jArrToEmail.size(); i++) {
                email.addTo(jArrToEmail.get(i) + "");
            }
            for (int i = 0; i < jArrCCEmail.size(); i++) {
                email.addBcc(jArrCCEmail.get(i) + "");
            }
            email.setFrom(senderEmail, from); // From
            email.setCharset("utf-8");
            email.setSubject(title); //Title
            email.setTextMsg(content);

//            email.setHtmlMsg(content); //COntent

            email.send();
        } catch (Exception ex) {
            log.add("exception sendEmail", ex.getMessage());
        }
    }

    public static class Cash {
        public long bankNetAmountLocked = 0;
        public long coreAmountAdjust = 0;

        public Cash(long bankNetAmountLocked, long coreAmountAdjust) {
            this.bankNetAmountLocked = bankNetAmountLocked;
            this.coreAmountAdjust = coreAmountAdjust;
        }
    }

    public static class TimeOfMonth {
        public long BeginTime = 0;
        public long EndTime = 0;

        public TimeOfMonth(long beginTime, long endTime) {
            this.BeginTime = beginTime;
            this.EndTime = endTime;
        }
    }

    public static class KeyValue {
        public String Key = "";
        public String Value = "";

        public KeyValue(String key, String value) {
            this.Key = key;
            this.Value = value;
        }

        public KeyValue() {
        }
    }

    public static class CapsetAndM2MType {
        public boolean Capset = true;
        public String M2mTranferType = "";

        public CapsetAndM2MType(boolean capset, String m2mTranferType) {
            this.Capset = capset;
            this.M2mTranferType = m2mTranferType;
        }
    }

}
