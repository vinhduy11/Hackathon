package com.mservice.momo.data;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created by concu on 8/4/14.
 */
public class BillInfoService {
    
    
    public static class ExtraInfo{

        public String bill_detail_id  =""; //loai dich vu --> hoa don / dich vu / phim....
        public String amount          =""; // ma doi tac vi du : viettel
        public String from_date       =""; // ma dich vu cua doi tac : vi du : iviettel, aviettel...
        public String to_date         =""; // ten dich vu : ADSL Viettel, Dien...

        public ExtraInfo(){}
    }

    public static class TextValue{
        public String text="";
        public String value = "";
        public String parentid ="";
        public TextValue(){}
        public TextValue(String text, String value){
            this.text = text;
            this.value = value;
        }
    }
    
    //static fields
    /*message ExtraInfo{

        optional string bill_detail_id  =1; //loai dich vu --> hoa don / dich vu / phim....
        optional string amount          =2; // ma doi tac vi du : viettel
        optional string from_date       =3; // ma dich vu cua doi tac : vi du : iviettel, aviettel...
        optional string to_date         =4; // ten dich vu : ADSL Viettel, Dien...
    }*/
    //optional uint64 total_amount   =1;

    /*message TextValue{
        optional string text=1;
        optional string value = 2;
    }*/

    /*repeated TextValue array_price      =3; // danh sach cac menh gia co the tra
    repeated TextValue customer_info    =4; // chua thong tin khach hang
    repeated ExtraInfo extra_info       =5; // thong tin ho tro nguoi dung, vi du : chi tiet tung bill con cua dien*/

    // for TextValue
    private static String text = "text";
    private static String value = "value";

    //for ExtraInfo
    private static String bill_detail_id  ="bill_detail_id"; //loai dich vu --> hoa don / dich vu / phim....
    private static String amount          ="amount"; // ma doi tac vi du : viettel
    private static String from_date       ="from_date"; // ma dich vu cua doi tac : vi du : iviettel, aviettel...
    private static String to_date         ="to_date"; // ten dich vu : ADSL Viettel, Dien...

    public long total_amount=0; // tong so dien can thanh toan, neu co co list price --> khong can set field nay

    //vi du :ho ten : nguyen van A
    // dia chi : Phan dinh phung
    // so dien thoai : 0974540385
    public ArrayList<TextValue> customer_info= new ArrayList<>();

    //vi du : gia 1 : 10000
    // gia 2 : 20000
    public ArrayList<TextValue> array_price = new ArrayList<>();

    // vi du : billdetailid : 123456
    //  amount : 500000
    // from date : 12/07/2014
    // to date : 22/07/2014
    public ArrayList<ExtraInfo> extra_info = new ArrayList<>();

    public void addPrice(TextValue priceItem){
        array_price.add(priceItem);
    }

    public void addCustomInfo(TextValue customItem){
        customer_info.add(customItem);
    }

    public void addExtraInfo(ExtraInfo extraItem){
        extra_info.add(extraItem);
    }

    public JsonObject toJsonObject(){
        JsonObject jo = new JsonObject();

        //total amount
        jo.putNumber("total_amount",total_amount);

        //thong tin khach hang
        if(customer_info !=null && customer_info.size() >0){
            JsonArray cusInfo = new JsonArray();
            for (TextValue cusItem : customer_info){
                cusInfo.add(new JsonObject().putString(text,cusItem.text)
                                            .putString(value,cusItem.value)
                );    
            }
            jo.putArray("customer_info",cusInfo);
        }
        
        //danh sach bang gia co the tra
        if(array_price !=null && array_price.size() >0){
            JsonArray arrPrice = new JsonArray();
            for (TextValue priceItem : array_price){
                arrPrice.add(new JsonObject().putString(text,priceItem.text)
                                             .putString(value,priceItem.value)
                );
            }
            jo.putArray("array_price",arrPrice);
        }

        /*message ExtraInfo{
        optional string bill_detail_id  =1; //loai dich vu --> hoa don / dich vu / phim....
        optional string amount          =2; // ma doi tac vi du : viettel
        optional string from_date       =3; // ma dich vu cua doi tac : vi du : iviettel, aviettel...
        optional string to_date         =4; // ten dich vu : ADSL Viettel, Dien...
        }*/

        //thong tin phu them
        if(extra_info !=null && extra_info.size() >0){
            JsonArray arrExtra = new JsonArray();
            for (ExtraInfo extraItem : extra_info){
                arrExtra.add(new JsonObject().putString(bill_detail_id,extraItem.bill_detail_id)
                                .putString(amount,extraItem.amount)
                                .putString(from_date,extraItem.from_date)
                                .putString(to_date,extraItem.to_date)
                );
            }
            jo.putArray("extra_info",arrExtra);
        }

        return jo;
    }

    public BillInfoService(JsonObject jo) {

        //khong co json
        if(jo ==  null){
            new BillInfoService();
            return;
        }

        total_amount = jo.getLong("total_amount", total_amount);

        /*if(jsonListPrice!=null){
            obj.list_price = new ArrayList<>();
            Iterator<Map.Entry<String,Object>> entries = jsonListPrice.toMap().entrySet().iterator();

            while (entries.hasNext()){
                Map.Entry<String,Object> entry = entries.next();
                JsonObject item = new JsonObject();
                obj.list_price.add(new Price(String.valueOf(entry.getKey()), DataUtil.strToInt(entry.getValue().toString())));
            }
        }*/

        //{"rcode":0
        // ,"json_result":{"total_amount":837100
        //,"customer_info":[{"text":"ht","value":"Tran Long Sai"}]
        //,"extra_info":[
        // {"bill_detail_id":"SGS0177440","amount":"440000","from_date":"31/12/2008","to_date":"31/12/2008"}
        //,{"bill_detail_id":"SGS0205895","amount":"397100","from_date":"31/01/2009","to_date":"31/01/2009"}]}}

        //for customer info
        JsonArray arrCusInfo = jo.getArray("customer_info", null);

        if (arrCusInfo != null) {

            if (customer_info == null) {
                customer_info = new ArrayList<>();
            }

            for (Object o : arrCusInfo) {
                TextValue item = new TextValue();
                item.text = ((JsonObject) o).getString(text);
                item.value = ((JsonObject) o).getString(value);
                customer_info.add(item);
            }
        }

        //for array price
        JsonArray arrPrice = jo.getArray("array_price", null);
        if (arrPrice != null) {

            if (array_price == null) {
                array_price = new ArrayList<>();
            }

            for (Object o : arrPrice) {
                TextValue item = new TextValue();
                item.text = ((JsonObject) o).getString(text);
                item.value = ((JsonObject) o).getString(value);
                array_price.add(item);
            }
        }

        //for extra information
        JsonArray arrExtra = jo.getArray("extra_info", null);
        if (arrExtra != null) {

            if (extra_info == null) {
                extra_info = new ArrayList<>();
            }

            for (Object o : arrExtra) {

                ExtraInfo extraInfo = new ExtraInfo();
                extraInfo.bill_detail_id = ((JsonObject) o).getString(bill_detail_id, "");
                extraInfo.amount = ((JsonObject) o).getString(amount, "0");
                extraInfo.from_date = ((JsonObject) o).getString(from_date, "");
                extraInfo.to_date= ((JsonObject) o).getString(to_date, "");

                extra_info.add(extraInfo);
            }
        }
    }

    public BillInfoService(){}

}
