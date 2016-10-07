package com.mservice.momo.vertx.promotion_server;

import com.mservice.momo.util.StringConstUtil;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 7/22/16.
 */
public class PromotionObj {

    public String billId = "";
    public int tranType = 0;
    public String customerNumber = "";
    public long tranId = 0;
    public long amount = 0;
    public String serviceId = "";
    public long fee = 0;
    public int srcFrom = 0;
    public JsonObject joExtra = new JsonObject();
    public JsonArray jarrGift = new JsonArray();
    public String phoneNumber = "";
    public JsonObject joPhone = new JsonObject();
    public boolean isStoreApp = false;

    public PromotionObj(){}

    public PromotionObj (JsonObject joPromotion)
    {
        billId = joPromotion.getString(StringConstUtil.PromotionField.BILL_ID, "");
        tranType = joPromotion.getInteger(StringConstUtil.PromotionField.TRAN_TYPE, 0);
        customerNumber = joPromotion.getString(StringConstUtil.PromotionField.CUSTOMER_NUMBER, "");
        tranId = joPromotion.getLong(StringConstUtil.PromotionField.TRAN_ID, 0);
        amount = joPromotion.getLong(StringConstUtil.PromotionField.AMOUNT, 0);
        serviceId = joPromotion.getString(StringConstUtil.PromotionField.SERVICE_ID, "");
        fee = joPromotion.getLong(StringConstUtil.PromotionField.FEE, 0);
        srcFrom = joPromotion.getInteger(StringConstUtil.PromotionField.SOURCE_FROM, 0);
        joExtra = joPromotion.getObject(StringConstUtil.PromotionField.JSON_EXTRA, new JsonObject());
        jarrGift = joPromotion.getArray(StringConstUtil.PromotionField.GIFT_ID_LIST, new JsonArray());
        phoneNumber = joPromotion.getString(StringConstUtil.PromotionField.PHONE_NUMBER, "");
        joPhone = joPromotion.getObject(StringConstUtil.PromotionField.PHONE_OBJ, new JsonObject());
        isStoreApp = joPromotion.getBoolean(StringConstUtil.PromotionField.IS_STORE_APP, false);
    }

    public PromotionObj toPromotionObject(JsonObject joPromotion)
    {
        PromotionObj promotionObj = new PromotionObj();
        promotionObj.billId = joPromotion.getString(StringConstUtil.PromotionField.BILL_ID, "");
        promotionObj.tranType = joPromotion.getInteger(StringConstUtil.PromotionField.TRAN_TYPE, 0);
        promotionObj.customerNumber = joPromotion.getString(StringConstUtil.PromotionField.CUSTOMER_NUMBER, "");
        promotionObj.tranId = joPromotion.getLong(StringConstUtil.PromotionField.TRAN_ID, 0);
        promotionObj.amount = joPromotion.getLong(StringConstUtil.PromotionField.AMOUNT, 0);
        promotionObj.serviceId = joPromotion.getString(StringConstUtil.PromotionField.SERVICE_ID, "");
        promotionObj.fee = joPromotion.getLong(StringConstUtil.PromotionField.FEE, 0);
        promotionObj.srcFrom = joPromotion.getInteger(StringConstUtil.PromotionField.SOURCE_FROM, 0);
        promotionObj.joExtra = joPromotion.getObject(StringConstUtil.PromotionField.JSON_EXTRA, new JsonObject());
        promotionObj.jarrGift = joPromotion.getArray(StringConstUtil.PromotionField.GIFT_ID_LIST, new JsonArray());
        promotionObj.phoneNumber = joPromotion.getString(StringConstUtil.PromotionField.PHONE_NUMBER, "");
        promotionObj.joPhone = joPromotion.getObject(StringConstUtil.PromotionField.PHONE_OBJ, new JsonObject());
        promotionObj.isStoreApp = joPromotion.getBoolean(StringConstUtil.PromotionField.IS_STORE_APP, false);
        return promotionObj;
    }

    public JsonObject toPromotionJsonObject()
    {
        JsonObject joPromotion = new JsonObject();
        joPromotion.putString(StringConstUtil.PromotionField.PHONE_NUMBER, phoneNumber)
                .putNumber(StringConstUtil.PromotionField.AMOUNT, amount).putNumber(StringConstUtil.PromotionField.TRAN_ID, tranId)
                .putString(StringConstUtil.PromotionField.BILL_ID, billId).putString(StringConstUtil.PromotionField.SERVICE_ID, serviceId)
                .putNumber(StringConstUtil.PromotionField.TRAN_TYPE, tranType).putString(StringConstUtil.PromotionField.CUSTOMER_NUMBER, customerNumber)
                .putNumber(StringConstUtil.PromotionField.FEE, fee).putNumber(StringConstUtil.PromotionField.SOURCE_FROM, srcFrom)
                .putObject(StringConstUtil.PromotionField.JSON_EXTRA, joExtra)
                .putObject(StringConstUtil.PromotionField.PHONE_OBJ, joPhone)
                .putArray(StringConstUtil.PromotionField.GIFT_ID_LIST, jarrGift).putBoolean(StringConstUtil.PromotionField.IS_STORE_APP, isStoreApp);
        return joPromotion;
    }

}
