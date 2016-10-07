package com.mservice.momo.vertx.models;

import com.mservice.momo.data.model.MongoModel;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nam on 5/17/14.
 */
public class Notification extends MongoModel{

    public static final int STATUS_DETAIL = 0;
    public static final int STATUS_DISPLAY = 1;
    public static final int STATUS_READ = 2;

    public static final int STATUS_OK = 3;
    public static final int STATUS_CANCEL = 4;
    public static final int STATUS_DELETED = 5;
    private static ArrayList<Integer> allStatusWithoutDeleted = null;
    public String id;
    public Integer priority;
    public Integer type;
    public String caption;
    public String body;
    public String sms;
    public Long tranId;
    public Long cmdId;
    public Long time;
    public Integer sender;
    public Integer status;
    public Integer receiverNumber = 0;
    public String bodyIOS = "";
    public String token = "";
    public String os = "";
    public String prefix = "";
    public String refId;
    public String btnTitle;
    public Integer btnStatus;
    public String htmlBody;
    public String extra;
    public Integer category;

    public Notification(Notification other) {
        id = other.id;
        priority =other.priority;
        type=other.type;
        caption=other.caption;
        body=other.body;
        sms=other.sms;
        tranId=other.tranId;
        cmdId=other.cmdId;
        time=other.time;
        sender=other.sender;
        status=other.status;
        receiverNumber =other.receiverNumber;
        bodyIOS =other.bodyIOS;
        token =other.token;
        os =other.os;
        prefix =other.prefix;
        refId=other.refId;
        btnTitle=other.btnTitle;
        btnStatus=other.btnStatus;
        htmlBody=other.htmlBody;
        extra=other.extra;
        category=other.category;
    }

    public Notification() {
        this.prefix = AppConstant.PREFIX;
    }

    public Notification(MomoProto.Notification momoNoti) {
        this.id = momoNoti.getId();
        this.priority = momoNoti.getPriority();
        this.type = momoNoti.getType();
        this.caption = momoNoti.getCaption();
        this.body = momoNoti.getBody();
        this.sms = "";
        this.tranId = momoNoti.getTranId();
        this.cmdId = momoNoti.getCmdId();
        this.time = momoNoti.getTime();
        this.status = momoNoti.getStatus();
        this.bodyIOS = "";
        this.token = "";
        this.os = "";
        this.prefix = AppConstant.PREFIX;
        this.refId = momoNoti.getRefId();
        this.btnTitle = momoNoti.getBtnTitle();
        this.btnStatus = momoNoti.getBtnStatus();
        this.htmlBody = momoNoti.getHtmlBody();
        this.extra = momoNoti.getExtra();
        this.category = momoNoti.getCategory();
    }

    public static Notification parse(JsonObject json) {
        Notification notification = new Notification();
        if (json == null) {
            return null;
        }
        notification.id = json.getString("_id", "");
        notification.priority = json.getInteger("priority", 0);
        notification.type = json.getInteger("type", 0);
        notification.caption = json.getString("caption", "");
        notification.body = json.getString("body", "");
        notification.sms = json.getString("sms", "");
        notification.tranId = json.getLong("tranId", 0L);
        notification.cmdId = json.getLong("cmdId", 0L);
        notification.time = json.getLong("time", 0L);
        notification.status = json.getInteger("status", 0);
        notification.sender = json.getInteger("sender", 0);
        notification.receiverNumber = json.getInteger("receiverNumber", 0);
        notification.bodyIOS = json.getString("bodyIOS", "");
        notification.token = json.getString("token","");
        notification.os = json.getString("os","");
        notification.prefix = json.getString("prefix","");
        notification.refId = json.getString("refId", "");
        notification.btnTitle = json.getString("btnTitle", "");
        notification.btnStatus = json.getInteger("btnStatus", 0);
        notification.htmlBody = json.getString("htmlBody", "");
        notification.extra = json.getString("extra", "");
        notification.category = json.getInteger("category", 0);

//        if (notification.id.isEmpty() && notification.sms.isEmpty() && notification.caption.isEmpty() && notification.body.isEmpty())
//            return null;
        return notification;
    }

    //Parse SYNC ENCRYPT Noti
    public static Notification parseEncryptNoti(JsonObject json) {
        Notification notification = new Notification();
        if (json == null) {
            return null;
        }
        notification.id = json.getString("_id", "");
        notification.priority = json.getInteger("priority", 0);
        notification.type = json.getInteger("type", 0);
        notification.caption = json.getString("caption", "");
        try{
            notification.body = new String(DatatypeConverter.parseBase64Binary(json.getString("body", "")));
        }
        catch (Exception ex)
        {
            notification.body = json.getString("body", "");
        }
        if("".equalsIgnoreCase(notification.body))
        {
            notification.body = json.getString("body", "");
        }
        notification.sms = json.getString("sms", "");
        notification.tranId = json.getLong("tranId", 0L);
        notification.cmdId = json.getLong("cmdId", 0L);
        notification.time = json.getLong("time", 0L);
        notification.status = json.getInteger("status", 0);
        notification.sender = json.getInteger("sender", 0);
        notification.receiverNumber = json.getInteger("receiverNumber", 0);
        notification.bodyIOS = json.getString("bodyIOS", "");
        notification.token = json.getString("token","");
        notification.os = json.getString("os","");
        notification.prefix = json.getString("prefix","");
        notification.refId = json.getString("refId", "");
        notification.btnTitle = json.getString("btnTitle", "");
        notification.btnStatus = json.getInteger("btnStatus", 0);
        notification.htmlBody = json.getString("htmlBody", "");
        notification.extra = json.getString("extra", "");
        notification.category = json.getInteger("category", 0);

        if (notification.id.isEmpty() && notification.sms.isEmpty() && notification.caption.isEmpty() && notification.body.isEmpty())
            return null;
        return notification;
    }

    public static List<Integer> getAllStatusWithoutDeleted() {
        if (allStatusWithoutDeleted == null) {
            ArrayList<Integer> status = new ArrayList<>();
            status.add(STATUS_DISPLAY);
            status.add(STATUS_READ);
            status.add(STATUS_OK);
            status.add(STATUS_CANCEL);
            allStatusWithoutDeleted = status;
        }

        return allStatusWithoutDeleted;
    }

    public static JsonObject fromArrayToJsonObj(JsonArray jsonArray) {
        JsonObject res = new JsonObject();
        res.putString("_id", (String) jsonArray.get(0));
        res.putNumber("priority", (Number) jsonArray.get(1));
        res.putNumber("type", (Number) jsonArray.get(2));
        res.putString("caption", (String) jsonArray.get(3));
        res.putString("body", (String) jsonArray.get(4));
        res.putString("sms", (String) jsonArray.get(5));
        res.putNumber("tranId", (Number) jsonArray.get(6));
        res.putNumber("cmdId", (Number) jsonArray.get(7));
        res.putNumber("time", (Number) jsonArray.get(8));
        res.putNumber("sender", (Number) jsonArray.get(9));
        res.putNumber("status", (Number) jsonArray.get(10));
        res.putNumber("receiverNumber", (Integer) jsonArray.get(11));
        res.putString("bodyIOS", (String) jsonArray.get(12));
        res.putString("token", (String) jsonArray.get(13));
        res.putString("os", (String) jsonArray.get(14));
        res.putString("prefix", (String) jsonArray.get(15));
        res.putString("refId", (String) jsonArray.get(16));
        res.putString("btnTitle", (String) jsonArray.get(17));
        res.putNumber("btnStatus", (Number) jsonArray.get(18));
        res.putString("htmlBody", (String) jsonArray.get(19));
        res.putString("extra", (String) jsonArray.get(20));
        res.putNumber("category", (Number) jsonArray.get(21));
        return res;
    }

// utils;

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        if (id != null && !id.isEmpty())
            json.putString("_id", id);
        if (priority != null)
            json.putNumber("priority", priority);
        if (type != null)
            json.putNumber("type", type);
        if (caption != null && !caption.isEmpty())
            json.putString("caption", caption);
        if (body != null && !body.isEmpty())
            json.putString("body", body);
        if (sms != null && !sms.isEmpty())
            json.putString("sms", sms);
        if (tranId != null)
            json.putNumber("tranId", tranId);
        if (cmdId != null)
            json.putNumber("cmdId", cmdId);
        if (time != null)
            json.putNumber("time", time);
        json.putNumber("status", status); // alway has status
        if (sender != null)
            json.putNumber("sender", sender);
        if (bodyIOS != null && !bodyIOS.isEmpty())
            json.putString("bodyIOS", bodyIOS);
        if (token != null && !token.isEmpty())
            json.putString("token", token);
        if (os != null && !os.isEmpty())
            json.putString("os", os);
        if (prefix != null && !prefix.isEmpty())
            json.putString("prefix", prefix);
        if (refId != null)
            json.putString("refId", refId);
        if (btnTitle != null)
            json.putString("btnTitle", btnTitle);
        if (btnStatus != null)
            json.putNumber("btnStatus", btnStatus);
        if (htmlBody != null)
            json.putString("htmlBody", htmlBody);
        if (extra != null)
            json.putString("extra", extra);
        if (category != null)
            json.putNumber("category", category);
        return json;
    }

    public JsonObject toFullJsonObject() {
        return toJsonObject().putNumber("receiverNumber", receiverNumber);
    }

    public MomoProto.Notification toMomoProto() {
        MomoProto.Notification.Builder builder = MomoProto.Notification.newBuilder();
        if (id != null)
            builder.setId(id);
        if (priority != null)
            builder.setPriority(priority);
        if (type != null)
            builder.setType(type);
        if (caption != null)
            builder.setCaption(caption);
        if (body != null)
            builder.setBody(body);
        // No sms here: MomoPro uses to send message between Phone and Server. It don't need sms content at all.
        if (tranId != null)
            builder.setTranId(tranId);
        if (cmdId != null)
            builder.setCmdId(cmdId);
        if (time != null)
            builder.setTime(time);
        if (status != null)
            builder.setStatus(status);
        if (sender != null)
            builder.setSender(sender);
        if (refId != null)
            builder.setRefId(refId);
        if (btnTitle != null)
            builder.setBtnTitle(btnTitle);
        if (btnStatus != null)
            builder.setBtnStatus(btnStatus);
        if (htmlBody != null)
            builder.setHtmlBody(htmlBody);
        if (extra != null)
            builder.setExtra(extra);
        if (category != null)
            builder.setCategory(category);
        return builder.build();
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (id != null)
            json.putString("id", id);
        if (priority!= null)
            json.putNumber("priority", priority);
        if (type != null)
            json.putNumber("type", type);
        if (caption != null)
            json.putString("caption", caption);
        if (body != null)
            json.putString("body", body);
//        if (sms != null)
//            json.putString("sms", sms);
        if (tranId != null)
            json.putNumber("tranId", tranId);
        if (cmdId != null)
            json.putNumber("cmdId", cmdId);
        if (time != null)
            json.putNumber("time", time);
        if (sender != null)
            json.putNumber("sender", sender);
        if (status != null)
            json.putNumber("status", status);
        if (receiverNumber != null && receiverNumber != 0)
            json.putNumber("receiverNumber", receiverNumber);
        if (refId != null)
            json.putString("refId", refId);
        if (btnTitle != null)
            json.putString("btnTitle", btnTitle);
        if (btnStatus != null)
            json.putNumber("btnStatus", btnStatus);
        if (htmlBody != null)
            json.putString("htmlBody", htmlBody);
        if (extra != null)
            json.putString("extra", extra);
        if (category != null)
            json.putNumber("category", category);

//        if (bodyIOS != null && !bodyIOS.isEmpty())
//            json.putString("bodyIOS", bodyIOS);
//        if (token != null && !token.isEmpty())
//            json.putString("token", token);
//        if (os != null && !os.isEmpty())
//            json.putString("os", os);
//        if (prefix != null && !prefix.isEmpty())
//            json.putString("prefix", prefix);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        this.id = savedObject.getString("id");
        this.priority = savedObject.getInteger("priority");
        this.type = savedObject.getInteger("type");

        this.caption = savedObject.getString("caption");
        this.body = savedObject.getString("body");
        this.sms = savedObject.getString("sms");

        this.tranId = savedObject.getLong("tranId");
        this.cmdId = savedObject.getLong("cmdId");
        this.time = savedObject.getLong("time");

        this.sender = savedObject.getInteger("sender");
        this.status = savedObject.getInteger("status");
        this.receiverNumber = savedObject.getInteger("receiverNumber");

        this.bodyIOS = savedObject.getString("bodyIOS","");
        this.token = savedObject.getString("token","");
        this.os = savedObject.getString("os","");
        this.prefix = savedObject.getString("prefix","");
        this.refId = savedObject.getString("refId", "");
        this.btnTitle = savedObject.getString("btnTitle");
        this.btnStatus = savedObject.getInteger("btnStatus");
        this.htmlBody = savedObject.getString("htmlBody");
        this.extra = savedObject.getString("extra");
        this.category = savedObject.getInteger("category");
    }

    public JsonObject getMessageForAndroid(){

        Notification noti = new Notification();

        noti.id = id;
        noti.caption = caption;
        noti.body = body;
        noti.type = type;
        noti.cmdId = cmdId;
        noti.tranId = tranId;
        noti.time = time;
        noti.refId = refId;
        noti.btnTitle = btnTitle;
        noti.btnStatus = btnStatus;
        noti.htmlBody = htmlBody;
        noti.sender = sender;
        noti.extra = extra;
        noti.category = category;
        noti.status = status;

        return noti.toJsonObject();
    }

    public String getBodyForIOS(int length){
        String temp = bodyIOS;
        if (temp == null || temp.isEmpty()) {
            temp = body;
        }
        if (temp != null
                && !temp.isEmpty()
                && temp.getBytes().length > length
                && length > -1) {
            while (temp.getBytes().length > length) {
                temp = temp.substring(0, temp.length() - 1);
            }
            if (temp.length()>2){
                temp.substring(0, temp.length()-3);
                temp += "...";
            }
        }
        return temp;
    }

    public JsonArray toJsonArray() {
        JsonArray res = new JsonArray();
        res.add(id);
        res.add(priority);
        res.add(type);
        res.add(caption);
        res.add(body);
        res.add(sms);
        res.add(tranId);
        res.add(cmdId);
        res.add(time);
        res.add(sender);
        res.add(status);
        res.add(receiverNumber);
        res.add(bodyIOS);
        res.add(token);
        res.add(os);
        res.add(prefix);
        res.add(refId);
        res.add(btnTitle);
        res.add(btnStatus);
        res.add(htmlBody);
        res.add(extra);
        res.add(category);
        return res;
    }
}
