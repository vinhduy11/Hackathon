package com.mservice.momo.web.internal.webadmin.objs;

import com.mservice.momo.vertx.billpaypromo.BillPayPromoConst;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 5/12/15.
 */
public class BillPayPromoRow {


    public String number = ""; // wallet will be get voucher

    public String group = "";  // ma nhom khuyen mai, 1,2.....

    public String serviceId = "";

    public String tranId = "";

    public String tranType = "";

    public String error = "";

    public String errorDesc = "";

    public String proTimeVn = "";

    public BillPayPromoRow(String rawRow) {
        String[] fields = rawRow.split(";");
        number = fields.length > 0 ? fields[0].trim() : "";
        group = fields.length > 1 ? fields[1].trim() : "";
        serviceId = fields.length > 2 ? fields[2].trim() : "";
        tranId = fields.length > 3 ? fields[3].trim() : "";
        tranType = fields.length > 4 ? fields[4].trim() : "";
    }

    public BillPayPromoRow() {
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(BillPayPromoConst.NUMBER, this.number);
        jo.putString(BillPayPromoConst.GROUP, group);
        jo.putString(BillPayPromoConst.SERVICE_ID, serviceId);
        jo.putString(BillPayPromoConst.TRAN_ID, tranId);
        jo.putString(BillPayPromoConst.TRAN_TYPE, tranType);
        jo.putString(BillPayPromoConst.PROTIMEVN, proTimeVn);

        jo.putString(BillPayPromoConst.ERROR, error);
        jo.putString(BillPayPromoConst.ERROR_DESC, errorDesc);

        return jo;
    }
}
