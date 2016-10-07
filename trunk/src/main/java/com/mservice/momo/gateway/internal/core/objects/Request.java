package com.mservice.momo.gateway.internal.core.objects;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by concu on 9/26/14.
 */
public class Request {
    public  long TRAN_ID = 0; // ma giao dich
    public  String TYPE    = ""; // loai lenh khi chuyen qua core connector
    public  String SENDER_NUM = ""; // agent thuc hien giao dich
    public  String SENDER_PIN = ""; // pin of agent thuc hien giao dich
    public  String RECVER_NUM = ""; // agent nhan giao dich
    public  long TRAN_AMOUNT = 0; // gia tri giao dich
    public  String AGENT_NUMBER  = ""; // so dien thoai dang ky tao vi moi
    public  String AGENT_NAME  = ""; // ten
    public  String AGENT_ID_CARD  = ""; // cmnd
    public  String AGENT_EMAIL  = ""; // email
    public  String AGENT_PIN  = ""; // pin
    public  String ADJUST_TYP  = ""; // loai lenh adjust vi du wc
    public  String DESCRIPTION ="treo_tien";
    public  String TARGET =""; // tai khoan dac biet nhan tien khi topup/billpay
    public  int WALLET =0; // loai vi
    public  String RECIPIENT = ""; //tai khoan nhan trong kvp
    public  String BILLER_ID = ""; //tai khoan nhan trong kvp

    //for logger
    public  long TIME =0;
    public  String PHONE_NUMBER ="";

    //for key value pairs
    public ArrayList<KeyValue> KeyValueList = new ArrayList<>();

    public JsonObject toJsonObject(){
        JsonObject jo = new JsonObject();
        jo.putString(Structure.TYPE, TYPE);

        if(TRAN_ID>0){
            jo.putNumber(Structure.TRAN_ID,TRAN_ID);
        }

        if(!"".equalsIgnoreCase(SENDER_NUM)){
            jo.putString(Structure.SENDER_NUM, SENDER_NUM);
        }

        if(!"".equalsIgnoreCase(SENDER_PIN)){
            jo.putString(Structure.SENDER_PIN,SENDER_PIN);
        }

        if(!"".equalsIgnoreCase(RECVER_NUM)){
            jo.putString(Structure.RECVER_NUM, RECVER_NUM);
        }

        if(TRAN_AMOUNT >0){
            jo.putNumber(Structure.TRAN_AMOUNT,TRAN_AMOUNT);
        }

        if(!"".equalsIgnoreCase(AGENT_NUMBER)){
            jo.putString(Structure.AGENT_NUMBER,AGENT_NUMBER);
        }

        if(!"".equalsIgnoreCase(AGENT_NAME)){
            jo.putString(Structure.AGENT_NAME,AGENT_NAME);
        }

        if(!"".equalsIgnoreCase(AGENT_ID_CARD)){
            jo.putString(Structure.AGENT_ID_CARD,AGENT_ID_CARD);
        }

        if(!"".equalsIgnoreCase(AGENT_EMAIL)){
            jo.putString(Structure.AGENT_EMAIL,AGENT_EMAIL);
        }

        if(!"".equalsIgnoreCase(AGENT_PIN)){
            jo.putString(Structure.AGENT_PIN,AGENT_PIN);
        }

        if(!"".equalsIgnoreCase(ADJUST_TYP)){
            jo.putString(Structure.ADJUST_TYP,ADJUST_TYP);
        }

        if(!"".equalsIgnoreCase(DESCRIPTION)){
            jo.putString(Structure.DESCRIPTION,DESCRIPTION);
        }

        if(!"".equalsIgnoreCase(TARGET)){
            jo.putString(Structure.TARGET,TARGET);
        }

        jo.putNumber(Structure.WALLET,WALLET);

        if(!"".equalsIgnoreCase(RECIPIENT)){
            jo.putString(Structure.RECIPIENT,RECIPIENT);
        }

        if(!"".equalsIgnoreCase(TARGET)){
            jo.putString(Structure.TARGET,TARGET);
        }

        if(!"".equalsIgnoreCase(BILLER_ID)){
            jo.putString(Structure.BILLER_ID,BILLER_ID);
        }

        //for logger
        jo.putNumber(Structure.TIME,TIME);
        jo.putString(Structure.PHONE_NUMBER, PHONE_NUMBER);

        //for key value pairs
        //KVPs =;
        if(KeyValueList != null && KeyValueList.size() >0){
            JsonArray ar = new JsonArray();
            for (int i =0;i< KeyValueList.size() ; i ++){

                KeyValue kv = KeyValueList.get(i);
                JsonObject o = new JsonObject();
                o.putString(kv.Key, kv.Value);
                ar.add(o);
            }
            jo.putArray(Structure.KEY_VALUE_PAIR_ARR,ar);
        }
        return jo;
    }

    public Request(){}
    public Request(JsonObject jo){

        TRAN_ID = jo.getLong(Structure.TRAN_ID,0);
        TYPE    = jo.getString(Structure.TYPE,"");
        SENDER_NUM = jo.getString(Structure.SENDER_NUM, "");
        SENDER_PIN = jo.getString(Structure.SENDER_PIN,"");
        RECVER_NUM = jo.getString(Structure.RECVER_NUM, "");
        TRAN_AMOUNT = jo.getLong(Structure.TRAN_AMOUNT,0);
        AGENT_NUMBER  = jo.getString(Structure.AGENT_NUMBER,"");
        AGENT_NAME  = jo.getString(Structure.AGENT_NAME,"");
        AGENT_ID_CARD  = jo.getString(Structure.AGENT_ID_CARD,"");
        AGENT_EMAIL  = jo.getString(Structure.AGENT_EMAIL,"");
        AGENT_PIN  = jo.getString(Structure.AGENT_PIN,"");
        ADJUST_TYP  = jo.getString(Structure.ADJUST_TYP,"");
        DESCRIPTION = jo.getString(Structure.DESCRIPTION,"");
        TARGET = jo.getString(Structure.TARGET,"");
        WALLET = jo.getInteger(Structure.WALLET,0);
        RECIPIENT = jo.getString(Structure.RECIPIENT,"");
        BILLER_ID = jo.getString(Structure.BILLER_ID,"");

        //for logger
        TIME =jo.getLong(Structure.TIME,0);
        PHONE_NUMBER =jo.getString(Structure.PHONE_NUMBER,"");

        //for key value pairs
        //KVPs =;
        JsonArray ar = jo.getArray(Structure.KEY_VALUE_PAIR_ARR,null);

        if(ar!= null){
            KeyValueList = new ArrayList<>();
            for(int i=0;i<ar.size();i++){
                JsonObject o = ar.get(i);
                Iterator<Map.Entry<String,Object>> entries = o.toMap().entrySet().iterator();
                while (entries.hasNext()){

                    Map.Entry<String,Object> entry= entries.next();
                    KeyValue kv = new KeyValue();
                    kv.Key = entry.getKey();
                    kv.Value = entry.getValue().toString();
                    KeyValueList.add(kv);
                }
            }
        }
    }
}
