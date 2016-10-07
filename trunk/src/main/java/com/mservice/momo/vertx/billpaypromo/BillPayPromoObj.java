package com.mservice.momo.vertx.billpaypromo;

import com.mservice.momo.data.ControlOnClickActivityDb;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 5/11/15.
 */
public class BillPayPromoObj {


    //public static final String BILL_PAY_BUSS_ADDRESS = "BILL_PAY_BUSS_ADDRESS";

    public int tranType = 0;
    public long tranId = 0;
    public String phoneNumber = "";
    public String serviceId = "";
    public String source = "";
    public String cardCheckSum = "";
    public String cmnd = "";

    public BillPayPromoObj() {
    }

    public BillPayPromoObj(JsonObject jo) {
        phoneNumber = jo.getString(BillPayPromoConst.NUMBER, "");
        tranType = jo.getInteger(BillPayPromoConst.TRAN_TYPE, 0);
        tranId = jo.getLong(BillPayPromoConst.TRAN_ID, 0);
        serviceId = jo.getString(BillPayPromoConst.SERVICE_ID, "");
        source = jo.getString(BillPayPromoConst.SOURCE, "");
        cardCheckSum = jo.getString(BillPayPromoConst.CARD_CHECK_SUM, "");
        cmnd = jo.getString(BillPayPromoConst.CMND, "");

    }

    public static void requestBillPayPromo(final Vertx vertx
            , String phoneNumber
            , int tranType
            , long tranId
            , String serviceId
            , String source
            , final Handler<JsonObject> callback) {

        final BillPayPromoObj o = new BillPayPromoObj();
        o.phoneNumber = phoneNumber;
        o.tranId = tranId;
        o.tranType = tranType;
        o.serviceId = serviceId;
        o.source = source;
        final JsonObject joReply = new JsonObject();
        final String providerNetworkMobile = DataUtil.phoneProviderName(phoneNumber);

        if ("".equalsIgnoreCase(serviceId) || serviceId == null || DataUtil.strToLong(phoneNumber) <= 0 || "".equalsIgnoreCase(phoneNumber)) {
            joReply.putNumber("error", 1000);
            joReply.putString("desc", "Hệ thống đang bảo trì. Chúng tôi đã ghi nhận bạn tham gia chương trình và sẽ phản hồi trong thời gian sớm nhất. Chi tiết liên hệ: (08)39917199");
            callback.handle(joReply);
            return;
        } else if ("vinahcm".equalsIgnoreCase(serviceId) && !providerNetworkMobile.equalsIgnoreCase("Vinaphone")) {
            joReply.putNumber("error", 1000);
            joReply.putString("desc", "Rất tiếc, thẻ quà tặng  này chỉ dùng được với thuê bao Vinaphone trả sau.");
            callback.handle(joReply);
            return;
        }
        ControlOnClickActivityDb controlDb = new ControlOnClickActivityDb(vertx);
        ControlOnClickActivityDb.Obj controlObj = new ControlOnClickActivityDb.Obj();
        controlObj.key = phoneNumber + tranId + tranType + serviceId + source;
        controlObj.number = phoneNumber;
        controlObj.program = source;
        controlObj.service = serviceId;

        controlDb.insert(controlObj, new Handler<Integer>() {
            @Override
            public void handle(Integer integer) {
                if (integer == 0) {
                    vertx.eventBus().send(AppConstant.BILL_PAY_BUSS_ADDRESS, o.toJson(), new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            callback.handle(message.body());
                            return;
                        }
                    });
                }
            }
        });
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString(BillPayPromoConst.NUMBER, phoneNumber);
        jo.putNumber(BillPayPromoConst.TRAN_TYPE, tranType);
        jo.putNumber(BillPayPromoConst.TRAN_ID, tranId);
        jo.putString(BillPayPromoConst.SERVICE_ID, serviceId);
        jo.putString(BillPayPromoConst.SOURCE, source);
        jo.putString(BillPayPromoConst.CARD_CHECK_SUM, cardCheckSum);
        jo.putString(BillPayPromoConst.CMND, cmnd);
        return jo;
    }
}
