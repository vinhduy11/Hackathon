package com.mservice.momo.vertx.vcb;

import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by concu on 11/25/14.
 */
public class VcbCommon {
    public static void requestGiftForA(Vertx _vertx
            , int creator
            , int tranType
            , long tranId
            , long amount
            , String partner){

        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_for_A;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.partner = partner;

        _vertx.eventBus().send(AppConstant.VietCombak_Address, reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                //todo nothing
            }
        });
    }

    public static void requestGiftForAAdmin(Vertx _vertx
            , int creator
            , int tranType
            , long tranId
            , long amount
            , String partner, final Handler<JsonObject> callback){

        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_for_A;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.partner = partner;

        _vertx.eventBus().send(AppConstant.VietCombak_Address, reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }

    public static void requestGiftForB(Vertx _vertx
                                        ,int creator
                                        ,String partner
                                        ,long amount
                                        ,int tranType
                                        ,long tranId){
        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_for_B;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.partner = partner;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                //todo nothing
            }
        });
    }

    public static void requestGiftForBAdmin(Vertx _vertx
            ,int creator
            ,String partner
            ,long amount
            ,int tranType
            ,long tranId, final Handler<JsonObject> callback){
        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_for_B;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.partner = partner;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }

    //BEGIN 0000000003 GIVE GIFT FOR VCB WITHOUT TIME
    public static void requestGiftForBAdmin_withoutTime(Vertx _vertx
            ,int creator
            ,String partner
            ,long amount
            ,int tranType
            ,long tranId, final Handler<JsonObject> callback){
        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_for_B_without_time;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.partner = partner;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }
    //END 0000000003 GIVE GIFT FOR VCB WITHOUT TIME

    public static void requestGiftMomoForB(Vertx _vertx
                                            ,int creator
                                            ,String partner
                                            ,long amount
                                            ,int tranType
                                            ,long tranId
                                            ,String carId
                                            ,String bankcode
                                            ,boolean hasVcbTran){
        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_by_momo;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.partner = partner;
        reqObj.cardId = carId;
        reqObj.bankCode = bankcode;
        reqObj.hasBankTran = hasVcbTran;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                //todo nothing
            }
        });
    }

    public static void requestGiftMomoForAmwayPromo(Vertx _vertx
            ,int creator
            ,String partner
            ,long amount
            ,int tranType
            ,long tranId
            ,String carId
            ,String bankcode
            ,boolean hasVcbTran){
        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_by_amway_promo;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.partner = partner;
        reqObj.cardId = carId;
        reqObj.bankCode = bankcode;
        reqObj.hasBankTran = hasVcbTran;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                //todo nothing
            }
        });
    }


    public static void requestGiftMomoForBByPG(Vertx _vertx
            ,int creator
            ,String partner
            ,long amount
            ,int tranType
            ,long tranId
            ,String carId
            ,String bankcode
            ,boolean hasVcbTran, final Handler<JsonObject> callback){
        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_by_momo;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.partner = partner;
        reqObj.cardId = carId;
        reqObj.bankCode = bankcode;
        reqObj.hasBankTran = hasVcbTran;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }


    public static void requestPromoForPG(Vertx _vertx
                                        ,int creator
                                        ,int partner
                                        ,long amount
                                        ,int tranType
                                        ,long tranId
                                        ,long promoValue, String serviceId){

        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_promo_by_pg;
        reqObj.creator = creator;
        reqObj.amount = amount;
        reqObj.tranType = tranType;
        reqObj.tranId = tranId;
        reqObj.partner = String.valueOf(partner);
        reqObj.promoValue = promoValue;
        reqObj.serviceId = serviceId;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                //todo nothing
            }
        });
    }


    public static void requestGiftMomoViettinbank(Vertx _vertx
            ,int creator
            ,String partner
            ,long amount
            ,int tranType
            ,long tranId
            ,String carId
            ,String bankcode
            ,boolean hasBankTran
            ,int promoCount
            ,String promoName
            ,String giftType
            ,boolean hasCardId
            ,String mode
            ,String inviter
            ,final Handler<JsonObject> callback){
        ReqObj reqObj = new ReqObj();
        reqObj.command = Command.req_create_gift_by_viettinbank_promo;
        reqObj.creator = creator; // create and give voucher for this wallet
        reqObj.amount = amount;  //
        reqObj.tranType = tranType; //
        reqObj.tranId = tranId; //
        reqObj.partner = partner;   // partner according to this wallet
        reqObj.cardId = carId;      // the card id of this wallet mapped with viettinbank
        reqObj.bankCode = bankcode; // bankcode at mservice side
        reqObj.hasBankTran = hasBankTran; // has existed banktran or not
        reqObj.promoCount = promoCount;  // the count of number that we gave the gift this wallet
        reqObj.promoName = promoName;    // id of promotion
        reqObj.giftType = giftType;      // gift type of this gift
        reqObj.hasCardId = hasCardId;    // has existed the cardid or not on cmnd table
        reqObj.mode = mode;
        reqObj.inviter = inviter;

        _vertx.eventBus().send(AppConstant.VietCombak_Address,reqObj.toJson(),new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                callback.handle(message.body());
            }
        });
    }


}
