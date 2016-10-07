package com.mservice.momo.vertx.vcb;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/21/14.
 */
public class ReqObj {
    public String command =""; // the command of the request
    public long amount = 0;     // the amount of transaction
    public String bankCode= ""; // bankocde at mservice side
    public int creator =0;      // the wallet, mservice will give gift
    public String inviter ="";  // the partner or of this wallet
    public long tranId = 0;     // the tranId that will generate the gift to this wallet
    public int tranType = 0;    // the transaction type
    public long finishTime = 0; // the completed tim of the tranId
    public String partner = ""; // the partner of the wallet
    public String cardId = "";  // the card id of the wallet
    public boolean hasBankTran = false; // true : has bank cash-in transaction
    public long promoValue = 0; // the value of the gift
    public String serviceId ="";    // the service that generates this gift

    public String promoName ="";    // the promotion name
    public String giftType = "";    // the gift type of thi gift
    public int promoCount = 0;      // the number of promotion count that we gave
    public boolean hasCardId = false; // true : has carid id in cmnd table, else not have in cmnd table
    public String mode = "";
    public static String offline ="offline";
    public static String online = "online";


    //thong tin lien lac

    public ReqObj(){};
    public ReqObj(JsonObject jo){
        command = jo.getString("cmd","");
        amount = jo.getLong("amt",0);
        bankCode = jo.getString("bnkcode","");
        creator =jo.getInteger("creator",0);
        inviter = jo.getString("inviter","");
        tranId = jo.getLong("tranid",0);
        tranType = jo.getInteger("trantype",-1);
        finishTime = jo.getLong("ftime", 0);
        partner = jo.getString("partner","");
        cardId = jo.getString("cardId","");
        hasBankTran = jo.getBoolean("hasBankTran",false);
        promoValue = jo.getLong("promoval",0);
        serviceId = jo.getString("sid","");
        promoName = jo.getString("promoName","");
        giftType = jo.getString("giftType","");
        promoCount = jo.getInteger("promoCount",0);
        hasCardId = jo.getBoolean("hasCardId",false);
        mode = jo.getString("mode","");


    }
    public JsonObject toJson(){
        JsonObject jo = new JsonObject();
        jo.putString("cmd",command);
        jo.putNumber("amt",amount);
        jo.putString("bnkcode",bankCode);
        jo.putNumber("creator",creator);
        jo.putString("inviter",inviter);
        jo.putNumber("tranid",tranId);
        jo.putNumber("trantype",tranType);
        jo.putNumber("ftime",finishTime);
        jo.putString("partner",partner);
        jo.putString("cardId",cardId);
        jo.putBoolean("hasBankTran", hasBankTran);
        jo.putNumber("promoval",promoValue);
        jo.putString("sid",serviceId);
        jo.putString("promoName",promoName);
        jo.putString("giftType",giftType);
        jo.putNumber("promoCount",promoCount);
        jo.putBoolean("hasCardId",hasCardId);
        jo.putString("mode",mode);
        return jo;
    }
}
