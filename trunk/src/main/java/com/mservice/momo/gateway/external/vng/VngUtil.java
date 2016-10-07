package com.mservice.momo.gateway.external.vng;

import com.mservice.momo.util.DataUtil;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by concu on 7/5/14.
 */
public class VngUtil{
    public static class ObjCreate{
        //for create order
        public String session_id = "";
        public String customer_name = "";
        public String customer_email = "";
        public String customer_phone = "";
        public List<String> list_seat = null;
        public String client_ip = "118.69.210.244"; //fixed default value
        public String device_id = "";
        public String phone_number="";
        public long time =0;
        public String session_time ="";
        public String film_name ="";
        public String galaxy_info = "";

        public ObjCreate(){}
    }

    public static class ObjConfirmOrCancel{
        //for confirm or cancel order
        public String invoice_no = "";
        public long payment_code = 0;
        public String phone_number="";
        public long time=0;

        public ObjConfirmOrCancel(){}
    }

    public static class Price{
        public String key ="";
        public int value =0;
        public Price(String key, int value){
            this.key =key;
            this.value =value;
        }
        public Price(){}
    }

    public static class ObjReply{

        public int error=9999;
        public String desc="";

        //create
        public String invoice_no = "";
        public String ticket_code ="";
        public long price_before =0;
        public long price_after =0;
        public ArrayList<Price> list_price = null;

        //create order
        public String session_id="";
        public String session_time="";
        public String film_name="";
        //confirm
        public String date_add = "";
        public String date_confirm = "";

        //cancel
        public String date_cancel = "";

        public ObjReply(){}

        public ObjReply(JsonObject jo){

            error = jo.getInteger(vngClass.Res.error,9999);
            desc = jo.getString(vngClass.Res.desc,"");
            invoice_no = jo.getString(vngClass.Res.invoice_no,"");
            ticket_code = jo.getString(vngClass.Res.ticket_code,"");
            price_before = DataUtil.stringToUNumber(jo.getString(vngClass.Res.price_before,"0")) ;
            price_after = DataUtil.stringToUNumber(jo.getString(vngClass.Res.price_after,"0")) ;
            date_add = jo.getString(vngClass.Res.date_add,"");
            date_confirm = jo.getString(vngClass.Res.date_confirm,"");
            date_cancel = jo.getString(vngClass.Res.date_cancel,"");

            session_id = jo.getString(vngClass.Res.session_id,"");
            session_time = jo.getString(vngClass.Res.session_time,"");
            film_name = jo.getString(vngClass.Res.film_name,"");

            JsonObject jsonListPrice = jo.getObject(vngClass.Res.list_price,null);

            if(jsonListPrice!=null){
                list_price = new ArrayList<>();
                Iterator<Map.Entry<String,Object>> entries = jsonListPrice.toMap().entrySet().iterator();

                while (entries.hasNext()){
                    Map.Entry<String,Object> entry = entries.next();
                    JsonObject item = new JsonObject();
                    list_price.add(new Price(String.valueOf(entry.getKey()), DataUtil.strToInt(entry.getValue().toString())));
                }
            }
        }
    }

    public static JsonObject getJsonCreateOrder(ObjCreate obj){

        JsonArray list_seat = new JsonArray();
        for(String s : obj.list_seat){
            list_seat.add(s);
        }

        //fix bug cho vi moi khong co ten -- > khong dat duoc ve xem phim
        if("".equalsIgnoreCase(obj.customer_name)){
            obj.customer_name = "noname";
        }

        JsonObject jo = new JsonObject();
        jo.putString(CinemaVerticle.COMMAND, CinemaVerticle.CmdType.CREATE_ORDER);
        jo.putString(vngClass.Create.session_id, obj.session_id);
        jo.putString(vngClass.Create.customer_phone,obj.customer_phone);
        jo.putString(vngClass.Create.customer_email,obj.customer_email);
        jo.putString(vngClass.Create.customer_name, obj.customer_name);
        jo.putArray(vngClass.Create.list_seat,list_seat);
        jo.putString(vngClass.Create.client_ip,obj.client_ip);
        jo.putString(vngClass.Create.device_id,obj.device_id);
        jo.putString(vngClass.Create.phone_number,obj.phone_number);
        jo.putNumber(vngClass.Create.time,obj.time);
        jo.putString(vngClass.Create.session_time,obj.session_time);
        jo.putString(vngClass.Create.film_name,obj.film_name);
        jo.putString(vngClass.Create.galaxy_info, obj.galaxy_info);

        return jo;
    }

    public static JsonObject getJsonConfirm(ObjConfirmOrCancel obj){
        JsonObject jo = new JsonObject();
        jo.putString(CinemaVerticle.COMMAND,CinemaVerticle.CmdType.CONFIRM_ORDER);
        jo.putString(vngClass.ConfirmOrCancel.invoice_no,obj.invoice_no);
        jo.putNumber(vngClass.ConfirmOrCancel.payment_code,obj.payment_code);
        jo.putString(vngClass.ConfirmOrCancel.phone_number,obj.phone_number);
        jo.putNumber(vngClass.ConfirmOrCancel.time,obj.time);

        return jo;
    }

    public static JsonObject getJsonCancel(ObjConfirmOrCancel obj){
        JsonObject jo = new JsonObject();
        jo.putString(CinemaVerticle.COMMAND,CinemaVerticle.CmdType.CANCEL_ORDER);
        jo.putString(vngClass.ConfirmOrCancel.invoice_no,obj.invoice_no);
        jo.putString(vngClass.ConfirmOrCancel.phone_number,obj.phone_number);
        jo.putNumber(vngClass.ConfirmOrCancel.time,obj.time);
        return jo;
    }

    public static ObjReply getObjReply(JsonObject jo){

        ObjReply obj = new ObjReply(null);

        obj.error = jo.getInteger(vngClass.Res.error,9999);
        obj.desc = jo.getString(vngClass.Res.desc,"");
        obj.invoice_no = jo.getString(vngClass.Res.invoice_no,"");
        obj.ticket_code = jo.getString(vngClass.Res.ticket_code,"");
        obj.price_before = DataUtil.stringToUNumber(jo.getString(vngClass.Res.price_before,"0"));
        obj.price_after = DataUtil.stringToUNumber(jo.getString(vngClass.Res.price_after,"0")) ;
        obj.date_add = jo.getString(vngClass.Res.date_add,"");
        obj.date_confirm = jo.getString(vngClass.Res.date_confirm,"");
        obj.date_cancel = jo.getString(vngClass.Res.date_cancel,"");

        JsonObject jsonListPrice = jo.getObject(vngClass.Res.list_price,null);

        if(jsonListPrice!=null){
            obj.list_price = new ArrayList<>();
            Iterator<Map.Entry<String,Object>> entries = jsonListPrice.toMap().entrySet().iterator();

            while (entries.hasNext()){
                Map.Entry<String,Object> entry = entries.next();
                JsonObject item = new JsonObject();
                obj.list_price.add(new Price(String.valueOf(entry.getKey()), DataUtil.strToInt(entry.getValue().toString())));
            }
        }

        return obj;
    }

}
