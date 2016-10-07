package com.mservice.momo.web.internal.webadmin.objs;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by anhkhoa on 30/03/2015.
 */
public class CustomerCareRow {

    //update data
    public String number = ""; // wallet will be get voucher
    public String enable = ""; // force cho phep tra thuong khong
    public String group = "";  // ma nhom khuyen mai, 1,2.....
    public String groupDesc = ""; // mo ta nhom nhan khuyen mai
    public String upTimeVn = ""; // thoi gian upload file len he thong

    //promoted
    public String promoCount = ""; // so luong da khuyen mai
    public String proTimeVn = ""; // thoi gian VN tra khuyen mai

    //promtion program

    //format yyyy-MM-dd hh:mm:ss
    public String dateFrom = ""; // chuong trinh khuyen mai bat dau tu ngay
    public String orgDateFrom = "";

    //format yyyy-MM-dd hh:mm:ss
    public String dateTo = ""; // chuong trinh khuyen mai keo dai den ngay
    public String orgDateTo = "";

    public String duration = ""; // thoi gian duoc cong them ke tu khi tao qua
    public String promoValue = ""; //gia tri khuyen mai
    public String momoAgent = ""; //tai khoan dung tra khuyen mai
    public String giftTypeId = ""; // loai voucher quy dinh cac dich vu duoc phep su dung

    public String transId = "";

    public String nameCustomer = "";

    public String error = "";

    public String errorDesc = "";

    public CustomerCareRow(String rawRow) {
        String[] fields = rawRow.split(";");
        number = fields.length > 0 ? fields[0].trim() : "";
        nameCustomer = fields.length > 1 ? fields[1].trim() : "";
        enable = fields.length > 2 ? fields[2].trim() : "";
        group = fields.length > 3 ? fields[3].trim() : "";
        groupDesc = fields.length > 4 ? fields[4].trim() : "";

        orgDateFrom = fields.length > 5 ? fields[5].trim() : "";
        String[] dfArr = orgDateFrom.split(" ");
        orgDateFrom = dfArr[0].trim() + " " + dfArr[dfArr.length -1].trim();

        orgDateTo = fields.length > 6 ? fields[6].trim() : "";
        String[] dtArr = orgDateTo.split(" ");
        orgDateTo = dtArr[0].trim() + " " + dtArr[dtArr.length -1].trim();


        duration = fields.length > 7 ? fields[7].trim() : "";
        momoAgent = fields.length > 8 ? fields[8].trim() : "";
        promoValue = fields.length > 9 ? fields[9].trim() : "";
        giftTypeId = fields.length > 10 ? fields[10].trim() : "";

//        error = fields.length > 11 ? fields[11].trim() : "";
//        errorDesc = fields.length > 12 ? fields[12].trim() : "";
        /*
        o.number
        o.nameCustomer
        o.enable
        o.group
        o.groupDesc
        o.orgDateFrom
        o.orgDateTo
        o.duration
        o.momoAgent
        o.promoValue
        o.giftTypeId
        */
    }

    public CustomerCareRow() {
    }

    public JsonObject toJson() {
        JsonObject jo = new JsonObject();
        jo.putString("number", this.number);
        jo.putString("enable", this.enable);
        jo.putString("group", group);
        jo.putString("groupDesc", groupDesc);
        jo.putString("upTimeVn", upTimeVn);
        jo.putString("promoCount", promoCount);
        jo.putString("proTimeVn", proTimeVn);

        jo.putString("dateFrom", dateFrom);
        jo.putString("orgDateFrom", orgDateFrom);
        jo.putString("dateTo", dateTo);
        jo.putString("orgDateTo", orgDateTo);
        jo.putString("duration", duration);

        jo.putString("promoValue", promoValue);
        jo.putString("momoAgent", momoAgent);
        jo.putString("giftTypeId", giftTypeId);

        jo.putString("transId", transId);
        jo.putString("nameCustomer", nameCustomer);

        jo.putString("error", error);
        jo.putString("errorDesc", errorDesc);

        return jo;
    }
}
