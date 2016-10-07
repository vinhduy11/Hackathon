package com.mservice.momo.web.internal.webadmin.objs;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 2/4/16.
 */
public class LixiRow {

        public String phoneNumber = "";
        public String amount = "";
        public String tranId = "";
        public String name = "";
        public String time = "";
        public String title = "";
        public String body = "";
        public String error = "";
        public String errorDesc = "";

        public LixiRow(String rawRow) {
            String[] fields = rawRow.split(";");
            phoneNumber = fields.length > 0 ? fields[0].trim() : "";
            name = fields.length > 1 ? fields[1].trim() : "";
            amount = fields.length > 2 ? fields[2].trim() : "";
            title = fields.length > 3 ? fields[3].trim() : "";
            body = fields.length > 4 ? fields[4].trim() : "";
        }

        public LixiRow() {
        }

    public LixiRow(JsonObject jo) {
        this.phoneNumber = jo.getString(colName.ZaloTetPromotionCol.PHONE_NUMBER, "0");
        this.name = jo.getString("name", "");
        this.amount = jo.getString(StringConstUtil.ZaloPromo.AMOUNT, "0");
        this.title = jo.getString("title", "");
        this.body = jo.getString("body", "");
    }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, this.phoneNumber);
            jo.putString(StringConstUtil.ZaloPromo.AMOUNT, amount);
            jo.putString(StringConstUtil.TRANDB_TRAN_ID, tranId);
            jo.putString("name", name);
            jo.putString("title", title);
            jo.putString("body", body);
            jo.putString("time", time);
            jo.putString(StringConstUtil.ERROR, error);
            jo.putString(StringConstUtil.DESCRIPTION, errorDesc);
            return jo;
        }


}


