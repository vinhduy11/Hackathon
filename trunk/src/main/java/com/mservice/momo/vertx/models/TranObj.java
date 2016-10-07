package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by duyhuynh on 12/07/2016.
 */
public class TranObj {

    public static final int STATUS_INIT = 0;
    public static final int STATUS_SEND = 1;
    public static final int STATUS_RECV = 2;
    public static final int STATUS_PROCESS = 3;
    public static final int STATUS_OK = 4;
    public static final int STATUS_FAIL = 5;
    public static final int CANCELLED = 6;
    public static int TRAN_SYNC_PREV_MINUTES = 0;
    public long cmdId = 0; // command index, cCID
    public long tranId = 0; // return from core doing transaction, cTID
    public long clientTime = 0; // time at calling webservice
    public long ackTime = 0; // ack time from server to client
    public long finishTime = 0; // the time that server response result.
    public int tranType = 0; //ex : MomoProto.MsgType.BANK_IN_VALUE
    public int io = 0; // direction of transaction.
    public int category = 0; // type of transfer, chuyen tien ve que, cho vay, khac
    public String partnerId = ""; // ban hang cua minh : providerId ..
    public String parterCode = ""; // ma doi tac
    public String partnerName = ""; // ten doi tac
    public String partnerRef = "";
    public String billId = ""; // for invoice, billID
    public long amount = 0; // gia tri giao dich
    public String comment = ""; // ghi chu
    public int status; // trang thai giao dich
    public int error; // ma loi giao dich
    public boolean deleted; // da xoa tren he thong
    public String owner_name; // giao dich duoc thuc hien tu ai
    public int owner_number; // giao dich duoc thuc hien tu so dien thoai nao
    public int parrent_tranType; // giao dich tong ben ngoai
    public int source_from = 1;

    //doi soat voi ben thu 3
    public String partner_invoice = "";
    public String partner_ticket = "";
    public int partner_error = 0;
    public String partner_description = "";
    public String partner_action = "";
    public int isRetailer = 0;
    public String desc = "";
    public boolean isStore = false;
    public JsonObject kvp;

    public JsonArray share = new JsonArray();

    public String _id = "";
    public Long balance = 0L;
    public Boolean ism2number = false;
    public String partnerExtra1 = "";
    public String forceCmt = "";
    public Long fee = 0L;
    public Long phone = 0L;

    public TranObj() {
    }

    public TranObj(JsonObject json) {
        _id = json.getString(colName.TranDBCols._ID, "");
        cmdId = json.getLong(colName.TranDBCols.COMMAND_INDEX, 0);
        tranId = json.getLong(colName.TranDBCols.TRAN_ID, 0);
        clientTime = json.getLong(colName.TranDBCols.CLIENT_TIME, 0);
        ackTime = json.getLong(colName.TranDBCols.ACK_TIME, 0);
        finishTime = json.getLong(colName.TranDBCols.FINISH_TIME, 0);
        tranType = json.getInteger(colName.TranDBCols.TRAN_TYPE, 0);
        io = json.getInteger(colName.TranDBCols.IO, 0);
        category = json.getInteger(colName.TranDBCols.CATEGORY, -1);
        partnerId = json.getString(colName.TranDBCols.PARTNER_ID, "");
        parterCode = json.getString(colName.TranDBCols.PARTNER_CODE, "");
        partnerName = json.getString(colName.TranDBCols.PARTNER_NAME, "");
        partnerRef = json.getString(colName.TranDBCols.PARTNER_REF, "");
        billId = json.getString(colName.TranDBCols.BILL_ID, "");
        amount = json.getLong(colName.TranDBCols.AMOUNT, 0);
        comment = json.getString(colName.TranDBCols.COMMENT, "");
        status = json.getInteger(colName.TranDBCols.STATUS, 0);
        error = json.getInteger(colName.TranDBCols.ERROR, 0);
        deleted = json.getBoolean(colName.TranDBCols.DELETED, false);
        owner_number = json.getInteger(colName.TranDBCols.OWNER_NUMBER, 0);
        owner_name = json.getString(colName.TranDBCols.OWNER_NAME, "");
        parrent_tranType = json.getInteger(colName.TranDBCols.PARRENT_TRAN_TYPE, 0);
        source_from = json.getInteger(colName.TranDBCols.FROM_SOURCE, MomoProto.TranHisV1.SourceFrom.MOMO_VALUE);
        partner_invoice = json.getString(colName.TranDBCols.PARTNER_INVOICE_NO, "");
        partner_error = json.getInteger(colName.TranDBCols.PARTNER_ERROR, 0);
        partner_description = json.getString(colName.TranDBCols.PARTNER_DESCRIPTION, "");
        partner_ticket = json.getString(colName.TranDBCols.PARTNER_TICKET_CODE, "");
        partner_action = json.getString(colName.TranDBCols.PARTNER_ACTION, "");
        desc = json.getString(colName.TranDBCols.DESCRIPTION, "");
        fee = 0L;
        try {
            fee = json.containsField(Const.AppClient.Fee) ? json.getLong(Const.AppClient.Fee, 0L) : 0L;
        } catch (Exception e) {
        }
        JsonObject kvpJson = json.getObject(colName.TranDBCols.KVP, new JsonObject());
        kvpJson.putNumber(Const.AppClient.Fee, fee);

        kvp = kvpJson;

        share = json.getArray(colName.TranDBCols.SHARE, new JsonArray());
        phone = json.getLong(colName.TranDBCols.PHONE, 0L);
    }

    public static JsonObject toMySQLJsonObj(JsonObject jsonObject) {
        jsonObject.putNumber("ism2number", jsonObject.getBoolean("ism2number", false) ? 1 : 0);
        jsonObject.putNumber("del", jsonObject.getBoolean("del", false) ? 1 : 0);
        jsonObject.putString("kvp", jsonObject.getObject("kvp", new JsonObject()).encode());
        jsonObject.putString("share", jsonObject.getArray("share", new JsonArray()).encode());
        return jsonObject;
    }

    public static JsonObject fromArrayToJsonObj(JsonArray jsonArray) {
        JsonObject res = new JsonObject();
        res.putString("_id", (String) jsonArray.get(0));
        res.putNumber("cmdId", (Number) jsonArray.get(1));
        res.putNumber("tranId", (Number) jsonArray.get(2));
        res.putNumber("ctime", (Number) jsonArray.get(3));
        res.putNumber("atime", (Number) jsonArray.get(4));
        res.putNumber("ftime", (Number) jsonArray.get(5));
        res.putNumber("tranType", (Number) jsonArray.get(6));
        res.putNumber("io", (Number) jsonArray.get(7));
        res.putNumber("cat", (Number) jsonArray.get(8));
        res.putString("pid", (String) jsonArray.get(9));
        res.putString("pcode", (String) jsonArray.get(10));
        res.putString("pname", (String) jsonArray.get(11));
        res.putString("pref", (String) jsonArray.get(12));
        res.putString("billId", (String) jsonArray.get(13));
        res.putNumber("amt", (Number) jsonArray.get(14));
        res.putString("cmt", (String) jsonArray.get(15));
        res.putNumber("status", (Number) jsonArray.get(16));
        res.putNumber("number", (Number) jsonArray.get(17));
        res.putString("name", (String) jsonArray.get(18));
        res.putNumber("parenttranType", (Number) jsonArray.get(19));
        res.putNumber("error", (Number) jsonArray.get(20));
        res.putNumber("balance", (Number) jsonArray.get(21));
        res.putNumber("fromsrc", (Number) jsonArray.get(22));
        res.putBoolean("ism2number", (Boolean) jsonArray.get(23));
        res.putBoolean("del", (Boolean) jsonArray.get(24));
        res.putString("partnerExtra1", (String) jsonArray.get(25));
        res.putString("partnerInvNo", (String) jsonArray.get(26));
        res.putString("partnerTicketCode", (String) jsonArray.get(27));
        res.putNumber("partnerError", (Number) jsonArray.get(28));
        res.putString("partnerDesc", (String) jsonArray.get(29));
        res.putString("partnerAction", (String) jsonArray.get(30));
        res.putString("desc", (String) jsonArray.get(31));
        res.putString("forceCmt", (String) jsonArray.get(32));
        if (jsonArray.get(33) != null && DataUtil.isJsonObject((String) jsonArray.get(33))) {
            res.putObject("kvp", new JsonObject((String) jsonArray.get(33)));
        } else {
            res.putObject("kvp", new JsonObject());
        }
        if (jsonArray.get(34) != null && DataUtil.isJsonArray((String) jsonArray.get(34))) {
            res.putArray("share", new JsonArray((String) jsonArray.get(34)));
        } else {
            res.putArray("share", new JsonArray());
        }
        res.putNumber("fee", (Number) jsonArray.get(35));
        res.putNumber("phone", (Number) jsonArray.get(36));
        return res;
    }

    public JsonObject getJSON() {

        JsonObject _json = new JsonObject();
        _json.putNumber(colName.TranDBCols.COMMAND_INDEX, cmdId);
        _json.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        _json.putNumber(colName.TranDBCols.CLIENT_TIME, clientTime);
        _json.putNumber(colName.TranDBCols.ACK_TIME, ackTime);
        _json.putNumber(colName.TranDBCols.FINISH_TIME, finishTime);
        _json.putNumber(colName.TranDBCols.TRAN_TYPE, tranType);
        _json.putNumber(colName.TranDBCols.IO, io);
        _json.putNumber(colName.TranDBCols.CATEGORY, category);
        _json.putString(colName.TranDBCols.PARTNER_ID, partnerId);
        _json.putString(colName.TranDBCols.PARTNER_CODE, parterCode);
        _json.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
        _json.putString(colName.TranDBCols.PARTNER_REF, partnerRef);
        _json.putString(colName.TranDBCols.BILL_ID, billId);
        _json.putNumber(colName.TranDBCols.AMOUNT, amount);
        _json.putString(colName.TranDBCols.COMMENT, comment);
        _json.putNumber(colName.TranDBCols.STATUS, status);
        _json.putNumber(colName.TranDBCols.ERROR, error);
        _json.putBoolean(colName.TranDBCols.DELETED, deleted);
        _json.putNumber(colName.TranDBCols.OWNER_NUMBER, owner_number);
        _json.putString(colName.TranDBCols.OWNER_NAME, owner_name);
        _json.putNumber(colName.TranDBCols.PARRENT_TRAN_TYPE, parrent_tranType);
        _json.putNumber(colName.TranDBCols.FROM_SOURCE, source_from);
        _json.putString(colName.TranDBCols.DESCRIPTION, desc);
        _json.putArray(colName.TranDBCols.SHARE, share);
        if (kvp != null)
            _json.putObject(colName.TranDBCols.KVP, kvp);
        _json.putNumber(colName.TranDBCols.PHONE, phone);
        return _json;
    }

    public JsonArray toJsonArray() {
        JsonArray res = new JsonArray();
        res.add(_id);
        res.add(cmdId);
        res.add(tranId);
        res.add(clientTime);
        res.add(ackTime);
        res.add(finishTime);
        res.add(tranType);
        res.add(io);
        res.add(category);
        res.add(partnerId);
        res.add(parterCode);
        res.add(partnerName);
        res.add(partnerRef);
        res.add(billId);
        res.add(amount);
        res.add(comment);
        res.add(status);
        res.add(owner_number);
        res.add(owner_name);
        res.add(parrent_tranType);
        res.add(error);
        res.add(balance);
        res.add(source_from);
        res.add(ism2number ? 1 : 0);
        res.add(deleted ? 1 : 0);
        res.add(partnerExtra1);
        res.add(partner_invoice);
        res.add(partner_ticket);
        res.add(partner_error);
        res.add(partner_description);
        res.add(partner_action);
        res.add(desc);
        res.add(forceCmt);
        res.add(kvp.encode());
        res.add(share.encode());
        res.add(fee);
        res.add(phone);
        return res;
    }
}
