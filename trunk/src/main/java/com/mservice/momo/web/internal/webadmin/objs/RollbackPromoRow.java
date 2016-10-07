package com.mservice.momo.web.internal.webadmin.objs;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.visampointpromo.VisaMpointPromoConst;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/4/15.
 */
public class RollbackPromoRow {


    public String phoneNumber = "";
    public String tranId = "";
    public String tranType = "";
    public String cardnumber = "";
    public String visatranId = "";
    public String visaAmount = "";
    public String serviceId = "";
    public String totalAmount = "";
    public String bankcode = "";
    public String error = "";
    public String errorDesc = "";
    public String count = "";

    public RollbackPromoRow(String rawRow) {
        String[] fields = rawRow.split(";");
        phoneNumber = fields.length > 0 ? fields[0].trim() : "";
        tranId = fields.length > 1 ? fields[1].trim() : "";
        tranType = fields.length > 2 ? fields[2].trim() : "";
        cardnumber = fields.length > 3 ? fields[3].trim() : "";
        totalAmount = fields.length > 4 ? fields[4].trim() : "";
        serviceId = fields.length > 5 ? fields[5].trim() : "";
        bankcode = fields.length > 6 ? fields[6].trim() : "";
        visaAmount = fields.length > 7 ? fields[7].trim() : "";
        visatranId = fields.length > 8 ? fields[8].trim() : "";

    }

    public RollbackPromoRow() {
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(VisaMpointPromoConst.NUMBER, this.phoneNumber);
        jo.putString(VisaMpointPromoConst.TID, tranId);

        jo.putString(VisaMpointPromoConst.TRANTYPE, tranType);
        jo.putString(VisaMpointPromoConst.CARD_NUMBER, cardnumber);
        jo.putString(VisaMpointPromoConst.VISA_TRAN_ID, visatranId);
        jo.putString(VisaMpointPromoConst.VISA_AMOUNT, visaAmount);

        jo.putString(VisaMpointPromoConst.SERVICE_ID, serviceId);
        jo.putString(VisaMpointPromoConst.TOTAL_AMOUNT, totalAmount);

        jo.putString(VisaMpointPromoConst.BANK_CODE, bankcode);

        jo.putString(StringConstUtil.ERROR, error);

        jo.putString(StringConstUtil.DESCRIPTION, errorDesc);
        jo.putString(StringConstUtil.COUNT, count);
        return jo;
    }




}
