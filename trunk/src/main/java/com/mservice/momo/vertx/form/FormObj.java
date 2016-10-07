package com.mservice.momo.vertx.form;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created by concu on 11/8/14.
 */
public class FormObj {
    /*"cap":"Kaspersky step 1",// caption cua form 1
    "btn":"Tiếp", // text cua button cua form 1
    "nexfrm":2,
    "guide":"Huong dan cho form 1",*/

    public String formId = ""; // ma form
    public String caption =""; //caption cua form
    public String button =""; //text cua button
    public int nexfrm = 1;  //form ke tiep cua form nay
    public String guide ="";  // huong dan su dung cho form

    public ArrayList<FieldItem> fieldItems = new ArrayList<>();
    public ArrayList<FieldData> fieldDatas = new ArrayList<>();

    public FormObj(){}

    public FormObj clone(){
        FormObj formObj = new FormObj();
        formObj.formId = this.formId;
        formObj.caption = this.caption;
        formObj.button = this.button;
        formObj.nexfrm = this.nexfrm;
        formObj.guide = this.guide;

        if(this.fieldItems != null && this.fieldItems.size() > 0){
            for (int i=0;i< this.fieldItems.size();i++){
                FieldItem fi = this.fieldItems.get(i).clone();
                formObj.fieldItems.add(fi);
            }
        }

        if(this.fieldDatas != null && this.fieldDatas.size() > 0){

            for (int i=0;i<this.fieldDatas.size();i++){
                FieldData fd = this.fieldDatas.get(i).clone();
                formObj.fieldDatas.add(fd);
            }
        }

        return formObj;
    }

    public FormObj(JsonObject jo, String formId){
        //get formid -->form1
        this.formId = formId;
        caption = jo.getString("cap","");
        button = jo.getString("btn","");
        nexfrm = jo.getInteger("nexfrm", 0);
        guide = jo.getString("guide","");

        //lay cac fields cua form nay
        JsonArray fieldArr = jo.getArray("fields", new JsonArray());

        for(int i =0; i<fieldArr.size(); i++){
            fieldItems.add(new FieldItem((JsonObject)fieldArr.get(i)));
        }

        //lay data cua form nay

        JsonArray dataArr = jo.getArray("data", new JsonArray());

        for(int i =0; i<dataArr.size(); i++){
            fieldDatas.add(new FieldData((JsonObject)dataArr.get(i)));
        }
    }

    public JsonObject toJson(){

        //form fields
        JsonArray arrayFields = new JsonArray();

        FieldItem capitem = new FieldItem();
        capitem.key = "";
        capitem.fieldlabel= caption;
        capitem.fieldtype = "caption";
        arrayFields.add(capitem.toJson());

        FieldItem btnitem = new FieldItem();
        btnitem.key = "";
        btnitem.fieldlabel= button;
        btnitem.fieldtype = "button";
        arrayFields.add(btnitem.toJson());

        FieldItem guiditem = new FieldItem();
        guiditem.key = "";
        guiditem.fieldlabel= guide;
        guiditem.fieldtype = "guide";
        arrayFields.add(guiditem.toJson());

        ArrayList<FieldItem> arrayListParentNotRoot = new ArrayList<>();

        if(fieldItems != null && fieldItems.size() > 0){

            for (int i =0;i< fieldItems.size(); i++){
                FieldItem fi =fieldItems.get(i);
                arrayFields.add(fi.toJson());

                if(fi.haschild == 1){
                    arrayListParentNotRoot.add(fi);
                }
            }
        }

        JsonArray arrayData = new JsonArray();

        //chi lay nhung thang nao co parentid = "";
        if(fieldDatas != null && fieldDatas.size() > 0){
            for (int i=0;i< fieldDatas.size(); i++){

                FieldData fd = fieldDatas.get(i);
                //get data for root parent
                if("".equalsIgnoreCase(fd.parentid)){
                    arrayData.add(fd.toJson());

                    for (int j = 0;j<arrayListParentNotRoot.size();j++){
                        FieldItem fi = arrayListParentNotRoot.get(j);
                        if(fi.key.equalsIgnoreCase(fd.linkto)){
                           arrayListParentNotRoot.remove(fi);
                            break;
                        }
                    }
                }

                //build dummy data for children are parent
                //fix bug for app
                for (int k=0;k<arrayListParentNotRoot.size();k++){
                    if(fd.linkto.equalsIgnoreCase(arrayListParentNotRoot.get(k).key)){
                        FieldData subFd = new FieldData();
                        subFd.linkto = arrayListParentNotRoot.get(k).key;
                        subFd.text= "";
                        subFd.value="";
                        subFd.id = fd.id;
                        arrayData.add(subFd.toJson());
                    }
                }
            }
        }

        JsonObject joResult = new JsonObject();
        joResult.putArray("field",arrayFields);
        joResult.putArray("data",arrayData);
        return joResult;
    }
}
