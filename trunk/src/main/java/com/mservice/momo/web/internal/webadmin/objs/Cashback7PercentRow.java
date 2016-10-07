package com.mservice.momo.web.internal.webadmin.objs;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 2/3/16.
 */
public class Cashback7PercentRow {

        public String phoneNumber = "";
        public String tranId = "";
        public String amount = "";
        public String serviceId = "";
        public String time = "";
        public String promotion_time = "";
        public String amount_bonus = "";
        public String tranIdBonus = "";
        public String error = "";
        public String errorDesc = "";

        public Cashback7PercentRow(String rawRow) {
            String[] fields = rawRow.split(";");
            phoneNumber = fields.length > 0 ? fields[0].trim() : "";
            tranId = fields.length > 1 ? fields[1].trim() : "";
            amount = fields.length > 2 ? fields[2].trim() : "";
            serviceId = fields.length > 3 ? fields[3].trim() : "";
            time = fields.length > 4 ? fields[4].trim() : "";
            promotion_time = fields.length > 5 ? fields[5].trim() : "";

        }

        public Cashback7PercentRow() {
        }

        public JsonObject toJson() {
            JsonObject jo = new JsonObject();
            jo.putString(colName.ZaloTetPromotionCol.PHONE_NUMBER, this.phoneNumber);
            jo.putString(StringConstUtil.ZaloPromo.TRAN_ID, tranId);
            jo.putString(StringConstUtil.ZaloPromo.AMOUNT, amount);
            jo.putString(StringConstUtil.ZaloPromo.SERVICE_ID, serviceId);
            jo.putString(colName.ZaloTetPromotionCol.TIME, time);
            jo.putString(StringConstUtil.ZaloPromo.PROMOTION_TIME, promotion_time);
            jo.putString("amount_bonus", amount_bonus);
            jo.putString("tranIdBonus", tranIdBonus);
            jo.putString(StringConstUtil.ERROR, error);
            jo.putString(StringConstUtil.DESCRIPTION, errorDesc);
            return jo;
        }


}
