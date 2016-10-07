package com.mservice.momo.vertx.gift.models;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.json.JsonObject;
import java.util.Date;

/**
 * Created by nam on 9/26/14.
 */
public class Gift extends MongoModel {

    //gift was created new status
    public static final Integer STATUS_NEW = MomoProto.Gift.Status.NEW_VALUE; // 1

    //gift was viewed by client
    public static final Integer STATUS_VIEWED = MomoProto.Gift.Status.VIEWED_VALUE; //2

    //gift was queued already
    public static final Integer STATUS_USED = MomoProto.Gift.Status.USED_VALUE; //3

    //gift will be sent out to another wallet by setting time
    public static final Integer STATUS_TIMED = MomoProto.Gift.Status.TIMED_VALUE; // 4

    //gift was expired, only used by backend
    public static final Integer STATUS_EXPIRED = 5;

    //gift was used by client
    public static final Integer STATUS_COMPLETE= 6;

    //gift was sent to a wallet that has not registered yet
    public static final Integer STATUS_GIFT_TO_NUMBER = 7;

    /**
     *
     * @param status : the integer value of the status
     * @return return the vietnamese comment
     */
    public static String getStatusText(int status){
        String s ="not defined";
        switch (status){
            case  MomoProto.Gift.Status.NEW_VALUE :
                s = "Tạo mới";
                break;
            case  MomoProto.Gift.Status.VIEWED_VALUE :
                s = "Đã xem";
                break;
            case  MomoProto.Gift.Status.USED_VALUE :
                s = "Đã kích hoạt";
                break;
            case  MomoProto.Gift.Status.TIMED_VALUE :
                s = "Chờ tặng";
                break;
            case  5 :
                s = "Đã hết hạn";
                break;
            case  6 :
                s = "Đã sử dụng";
                break;
            case  7 :
                s = "Đã gửi G2N";
                break;

            default:
                break;
        }
        return s;
    }

    public static String getDesc(int status){
        String s ="not defined";
        switch (status){
            case  MomoProto.Gift.Status.NEW_VALUE :
                s = "Tao moi";
                break;
            case  MomoProto.Gift.Status.VIEWED_VALUE :
                s = "Da xem";
                break;
            case  MomoProto.Gift.Status.USED_VALUE :
                s = "Da kich hoat";
                break;
            case  MomoProto.Gift.Status.TIMED_VALUE :
                s = "Cho tang";
                break;
            case  5 :
                s = "Da het han";
                break;
            case  6 :
                s = "Da su dung";
                break;
            case  7 :
                s = "Da gui G2N";
                break;

            default:
                break;
        }
        return s;
    }

    public String code;
    public String typeId;
    public Long amount;
    public Date startDate;
    public Date endDate;
    public Date modifyDate;
    public String owner;
    public Integer status;

    public Long tranId;
    public Boolean lock;
    public JsonObject extra;
    public Long tranDate;
    public String note;
    public String oldOwner;

    //0000000011 Insert them cho ke toan loc danh sach
    public String giftInfoDetail;

    public Gift() {

    }

    public Gift(String giftId) {
        setModelId(giftId);
    }

    public Gift(String owner, String giftId) {
        setModelId(giftId);
        this.owner = owner;
    }

    public Gift(JsonObject gift) {
        if (gift == null)
            return;
        fromJson(gift);
    }

    public JsonObject getExtra() {
        if (extra == null)
            extra = new JsonObject();
        return extra;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (code != null)
            json.putString("code", code);
        if (typeId != null)
            json.putString("typeId", typeId);
        if (amount != null)
            json.putNumber("amount", amount);
        if (startDate != null)
            json.putNumber("startDate", startDate.getTime());
        if (endDate != null)
            json.putNumber("endDate", endDate.getTime());
        if (modifyDate != null)
            json.putNumber("modifyDate", modifyDate.getTime());
        if (owner != null)
            json.putString("owner", owner);
        if (status != null)
            json.putNumber("status", status);
        if (lock != null)
            json.putBoolean("lock", lock);
        if (tranId != null)
            json.putNumber("tranId", tranId);
        if (extra != null)
            json.putObject("extra", extra);
        if (tranDate != null)
            json.putNumber("tranDate", tranDate);

        if (note != null)
            json.putString("note", note);

        if(oldOwner != null){
            json.putString("oldOwner",oldOwner);
        }

        if(giftInfoDetail != null)
        {
            json.putString("giftInfoDetail", giftInfoDetail);
        }

        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        Long temp;
        this.code = savedObject.getString("code", "");
        this.typeId = savedObject.getString("typeId", "");
        this.amount = savedObject.getLong("amount", 0);

        temp = savedObject.getLong("startDate", System.currentTimeMillis());
        if (temp != null)
            this.startDate = new Date(temp);

        temp = savedObject.getLong("endDate", System.currentTimeMillis());
        if (temp != null)
            this.endDate = new Date(temp);

        temp = savedObject.getLong("modifyDate", System.currentTimeMillis());
        if (temp != null) {
            this.modifyDate = new Date(temp);
        }

        this.owner = savedObject.getString("owner", "");
        this.status = savedObject.getInteger("status", 0);
        this.lock = savedObject.getBoolean("lock", false);
        this.tranId = savedObject.getLong("tranId", 0);
        this.extra = savedObject.getObject("extra", new JsonObject());
        this.tranDate = savedObject.getLong("tranDate", System.currentTimeMillis());
        this.note = savedObject.getString("note","");
        this.oldOwner = savedObject.getString("oldOwner","");

        this.giftInfoDetail = savedObject.getString("giftInfoDetail","");
    }

    public MomoProto.Gift toMomoProto() {
        MomoProto.Gift.Builder builder = MomoProto.Gift.newBuilder();
        builder.setId(getModelId());
        if (code != null)
            builder.setCode(code);
        builder.setTypeId(typeId);
        builder.setAmount(amount);
        if (startDate != null)
            builder.setStartDate(startDate.getTime());
        if (endDate != null)
            builder.setEndDate(endDate.getTime());
        builder.setModifyDate(modifyDate.getTime());
        builder.setStatus(status);
//        builder.setPhone(phone);
        if (extra != null)
            builder.setExtra(extra.toString());
        return builder.build();
    }

    public void fromJson(JsonObject json) {
        setValues(json);
        this.setModelId(json.getString("_id", ""));
    }
}
