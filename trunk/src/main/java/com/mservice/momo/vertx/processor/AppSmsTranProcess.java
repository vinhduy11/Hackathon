package com.mservice.momo.vertx.processor;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.AppSmsVerticle;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Created by User on 4/2/14.
 */
public class AppSmsTranProcess {
    private Logger logger;
    private Vertx vertx;

    public AppSmsTranProcess(Vertx vertx, Logger logger){
        this.vertx =vertx;
        this.logger =logger;
    }

    public void processTopUp(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if (!tap.tranHis.hasPartnerCode() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processTopUp to_mumber,amount : " + tap.tranHis.getPartnerCode() + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        String channel = Const.CHANNEL_MOBI;
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.TOP_UP_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.TopUp.newBuilder()
                                .setFromNumber(momoMessage.cmdPhone)
                                .setMpin(tap.pin)
                                .setChannel(channel)
                                .setAmount(tap.tranHis.getAmount())
                                .setToNumber(Integer.valueOf(tap.tranHis.getPartnerCode().trim()))
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(),ackTime,momoMessage,tap.tranHis);
                }
        });
    }

    public void processTopUpGame(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        /*ProviderId	-->	partner_id
        game_account	-->	partner_code
        amount	-->	amount*/

        if ( !tap.tranHis.hasPartnerId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processTopUpGame ProviderId,game_account,amount : "
                    + tap.tranHis.getPartnerId() + "," + tap.tranHis.getPartnerCode() + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        String channel = Const.CHANNEL_MOBI;
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.TOPUP_GAME_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.TopUpGame.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setGameAccount(tap.tranHis.getPartnerCode())
                                .setMpin(tap.pin)
                                .setChannel(channel)
                                .setAmount(Integer.parseInt(String.valueOf(tap.tranHis.getAmount())))
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(),ackTime,momoMessage,tap.tranHis);
                    }
                });
    }

    public void processBankIn(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        /*from_bank	-->	partner_name(no to core)
        bank_code	-->	partner_code
        amount	-->	amount
        */

        if (!tap.tranHis.hasAmount() || !tap.tranHis.hasPartnerCode()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBankIn bank_code,amount : "
                            + tap.tranHis.getPartnerCode() + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        //send this to soap
        String channel = Const.CHANNEL_MOBI;
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BANK_IN_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BankIn.newBuilder()
                                .setBankCode(tap.tranHis.getPartnerCode())
                                .setChannel(channel)
                                .setMpin(tap.pin)
                                .setAmount(tap.tranHis.getAmount())
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processBankOut(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        /*from_bank	-->	partner_name(no to core)
        bank_code	-->	partner_code
        amount	-->	amount
        */

        if ( !tap.tranHis.hasAmount()|| !tap.tranHis.hasPartnerCode()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBankOut bank_code,amount : "
                + tap.tranHis.getPartnerCode() + "," + tap.tranHis.getAmount());

        final long ackTime = System.currentTimeMillis();
        //send this to soap
        String channel = Const.CHANNEL_MOBI;
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BANK_IN_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BankOut.newBuilder()
                                .setChannel(channel)
                                .setBankCode(tap.tranHis.getPartnerCode())
                                .setMpin(tap.pin)
                                .setAmount(Integer.parseInt(String.valueOf(tap.tranHis.getAmount())))
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processM2C(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        /*phone	-->	partner_id
        name	-->	partner_name
        cardId	-->	partner_code
        amount	-->	amount
        notice	-->	comment
        */

        if (!tap.tranHis.hasPartnerId() || !tap.tranHis.hasPartnerName()
                || !tap.tranHis.hasPartnerCode() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processM2C phone,name,cardId,amount,notice : "
                                            + tap.tranHis.getPartnerId()
                                            + "," + tap.tranHis.getPartnerName()
                                            + "," + tap.tranHis.getPartnerCode()
                                            + "," + tap.tranHis.getAmount()
                                            + "," + tap.tranHis.getComment());

        final  long ackTime = System.currentTimeMillis();
        String channel = Const.CHANNEL_MOBI;
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.M2C_TRANSFER_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.M2CTransfer.newBuilder()
                                .setPhone(tap.tranHis.getPartnerId())
                                .setName(tap.tranHis.getPartnerName())
                                .setCardId(tap.tranHis.getPartnerCode())
                                .setChannel(channel)
                                .setAmount(tap.tranHis.getAmount())
                                .setMpin(tap.pin)
                                .setNotice(tap.tranHis.getComment())
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processM2M(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        /*phone	-->	partner_id
        amount	-->	amount
        notice	-->	comment
        */
        if (!tap.tranHis.hasPartnerId()|| !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processM2M phone,amount,notice : "
                                        + tap.tranHis.getPartnerId()
                                        + "," + tap.tranHis.getAmount()
                                        + "," + tap.tranHis.getComment());

        final long ackTime = System.currentTimeMillis();

        String payfor = ""; // tam thoi de trong nhe

        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.M2M_TRANSFER_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.M2MTransfer.newBuilder()
                                .setPhone(tap.tranHis.getPartnerId())
                                .setChannel(Const.CHANNEL_MOBI)
                                .setAmount(tap.tranHis.getAmount())
                                .setMpin(tap.pin)
                                .setNotice(tap.tranHis.getComment())
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processTransferMoneyToPlace(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {
        /*phone	partner_id
        amount	amount
        notice	comment
        */
        if (!tap.tranHis.hasPartnerId()|| !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processTransferMoneyToPlace phone,amount,notice : "
                + tap.tranHis.getPartnerId()
                + "," + tap.tranHis.getAmount()
                + "," + tap.tranHis.getComment());

        final long ackTime = System.currentTimeMillis();
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.TRANSFER_MONEY_TO_PLACE_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.TransferMoney2Place.newBuilder()
                                .setPhone(tap.tranHis.getPartnerId())
                                .setAmount(tap.tranHis.getAmount())
                                .setMpin(tap.pin)
                                .setNotice(tap.tranHis.getComment())
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    /*//for app sms
    BILL_PAY_TELEPHONE = 5031; // THANH TOAN CUOC DIEN THOAI
    BILL_PAY_TICKET_AIRLINE=5032; // THANH TOAN VE MAY BAY
    BILL_PAY_TICKET_TRAIN=5033; // THANH TOAN VE TAU LUA
    BILL_PAY_INSURANCE=5034; // THANH TOAN TIEN BAO HIEM
    BILL_PAY_INTERNET=5035; // THANH TOAN CUOC INTERNET
    BILL_PAY_OTHER=5036; // THANH TOAN CAC LOAI HOA DON KHAC*/

    public void processBillPayTelephone(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if (!tap.tranHis.hasPartnerId() || !tap.tranHis.hasBillId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBillPayTelephone providerId,areaId,phone,amount : "
                                    + tap.tranHis.getPartnerId()
                                    + "," + tap.tranHis.getPartnerRef() == null ? "" : tap.tranHis.getPartnerRef()
                                    + "," + tap.tranHis.getBillId()
                                    + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        BillPayTelephone
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        partner_ref	-->	areaId
        billId	-->	phone
        amount	-->	amount
        */
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BILL_PAY_TELEPHONE_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BillPayTelephone.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setAreaId(tap.tranHis.getPartnerRef())
                                .setPhone(tap.tranHis.getBillId())
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processBillPayTicketAirline(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if (!tap.tranHis.hasPartnerId() || !tap.tranHis.hasBillId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBillPayTicketAirline providerId,ticketId,amount : "
                + tap.tranHis.getPartnerId()
                + "," + tap.tranHis.getBillId()
                + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        BillPayTicketAirline
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        billId	-->	ticketId
        amount	-->	amount
        */
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BILL_PAY_TICKET_AIRLINE_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BillPayTicketAirline.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setTicketId(tap.tranHis.getBillId())
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processBillPayInternet(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if (!tap.tranHis.hasPartnerId() || !tap.tranHis.hasBillId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBillPayInternet providerId,areaId,customerAcc,amount : "
                + tap.tranHis.getPartnerId()
                + "," + tap.tranHis.getPartnerRef()
                + "," + tap.tranHis.getBillId()
                + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        BillPayInternet
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        partner_ref	-->	areaId
        billId	-->	customerAcc
        amount	-->	amount
        */
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BILL_PAY_INTERNET_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BillPayInternet.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setCustomerAcc(tap.tranHis.getBillId())
                                .setAreaId(tap.tranHis.getPartnerRef())
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processBillPayTicketTrain(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if ( !tap.tranHis.hasPartnerId() || !tap.tranHis.hasBillId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBillPayTicketTrain providerId,areaId,customerAcc,amount : "
                                        + tap.tranHis.getPartnerId()
                                        + "," + tap.tranHis.getPartnerRef()
                                        + "," + tap.tranHis.getBillId()
                                        + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        BillPayTicketTrain
        partnerId	-->	providerId
        partnerName	-->	getProviderName(areaId)
        partner_ref	-->	areaId
        billId	-->	ticketId
        amount	-->	amount
        */
        //send this to soap
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BILL_PAY_TICKET_TRAIN_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BillPayTicketTrain.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setTicketId(tap.tranHis.getBillId())
                                .setPlaceId(tap.tranHis.getPartnerRef())
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processBillPayInsurance(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if ( !tap.tranHis.hasPartnerId() || !tap.tranHis.hasBillId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBillPayInsurance providerId,insuranceAcc,amount : "
                                    + tap.tranHis.getPartnerId()
                                    + "," + tap.tranHis.getBillId()
                                    + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        BillPayInsurance
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        billId	-->	insuranceAcc
        amount	-->	amount
        */
        //send this to soap
        String channel = Const.CHANNEL_MOBI;
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BILL_PAY_INSURANCE_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BillPayInsurance.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setInsuranceAcc(tap.tranHis.getBillId())
                                .setChannel(channel)
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(),ackTime,momoMessage,tap.tranHis);
                    }
                });
    }

    public void processBillPayOther(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if ( !tap.tranHis.hasPartnerId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBillPayOther providerId, billerId,areaId,amount : "
                + tap.tranHis.getPartnerId()
                + "," + tap.tranHis.getBillId() == null ? "" : tap.tranHis.getBillId()
                + "," + tap.tranHis.getPartnerRef()== null ? "" : tap.tranHis.getPartnerRef()
                + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        BillPayOther
        partnerId	-->	providerId
        partnerName	-->	getProviderName(providerId)
        partner_ref	-->	areaId
        billId	-->	billerId
        amount	-->	amount
        */
        //send this to soap\
        String channel = Const.CHANNEL_MOBI;
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.BILL_PAY_OTHER_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.BillPayOther.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setBillerId(tap.tranHis.getBillId())
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processDepositCashOther(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if (!tap.tranHis.hasPartnerId()|| !tap.tranHis.hasBillId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processDepositCashOther providerId, billerId,areaId,amount : "
                + tap.tranHis.getPartnerId()
                + "," + tap.tranHis.getBillId() == null ? "" : tap.tranHis.getBillId()
                + "," + tap.tranHis.getPartnerRef()== null ? "" : tap.tranHis.getPartnerRef()
                + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        DepositCashOther
        partnerId	-->	 providerId
        partnerName	-->	getProviderName(providerId)
        billId	-->	customerAcc
        amount	-->	amount
        */
        String channel = Const.CHANNEL_MOBI;
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.DEPOSIT_CASH_OTHER_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.DepositCashOther.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setCustomerAcc(tap.tranHis.getBillId())
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(),ackTime,momoMessage,tap.tranHis);
                    }
                });
    }

    public void processBillPayElectric(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {

        if (!tap.tranHis.hasPartnerId()|| !tap.tranHis.hasBillId() || !tap.tranHis.hasAmount()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }

        logger.debug("AppSms - processBillPayElectric providerId, billId,amount : "
                + tap.tranHis.getPartnerId()
                + "," + tap.tranHis.getBillId() == null ? "" : tap.tranHis.getBillId()
                + "," + tap.tranHis.getAmount());

        final  long ackTime = System.currentTimeMillis();
        /*
        PayOneBill
        ProviderId	-->	partner_id
	    billId	-->	billId
	    amount	-->	amount
        */
        String channel = Const.CHANNEL_MOBI;
        vertx.eventBus().send(
                AppConstant.SoapVerticle_ADDRESS,
                MomoMessage.buildBuffer(
                        SoapProto.MsgType.PAY_ONE_BILL_VALUE,
                        momoMessage.cmdIndex,
                        momoMessage.cmdPhone,
                        SoapProto.PayOneBill.newBuilder()
                                .setProviderId(tap.tranHis.getPartnerId())
                                .setBillId(tap.tranHis.getBillId())
                                .setChannel(channel)
                                .setAmount(tap.tranHis.getAmount())
                                .setPin(tap.pin)
                                .build()
                                .toByteArray()
                ),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        //CoreCommon.sendTransReplyAppSms(result.body(), ackTime, momoMessage, tap.tranHis);
                    }
                }
        );
    }

    public void processMua123(final MomoMessage momoMessage, final AppSmsVerticle.TranHisV1AndPin tap) {
        if(!tap.tranHis.hasBillId() || !tap.tranHis.hasPartnerId()) {
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid,"");
            return;
        }


    }

}
