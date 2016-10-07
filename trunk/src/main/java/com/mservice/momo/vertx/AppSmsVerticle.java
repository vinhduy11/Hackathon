package com.mservice.momo.vertx;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.AppSmsConst;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SmsProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.data.RSACipher;
import com.mservice.momo.vertx.models.result.Pay123MuaOrderResult;
import com.mservice.momo.vertx.processor.AppSmsBuildTranHis;
import com.mservice.momo.vertx.processor.AppSmsTranProcess;
import com.mservice.momo.vertx.processor.Common;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.math.BigInteger;

/*
 * Created by User on 4/2/14.
 */
public class AppSmsVerticle extends Verticle {
//    public static final String ADDRESS = "com.mservice.momo.vertx.app.sms";

    private BigInteger rsa_pri_e;
    private BigInteger rsa_pri_p;
    private BigInteger rsa_pri_q;
    private BigInteger rsa_pri_dP;
    private BigInteger rsa_pri_dQ;
    private BigInteger rsa_pri_qInv;
    private BigInteger rsa_pri_mod;
    private BigInteger rsa_pri_exp;

    //for public key
    private BigInteger rsa_pub_mod;
    private BigInteger rsa_pub_exp;

    private Logger logger;
    private AppSmsTranProcess appSms;
    private AppSmsBuildTranHis thBuilder;
    private String sep = " ";
    private  PhonesDb phonesDb;
    private Common mCom;

    public void loadConfig(JsonObject config) {

    }

    public void start() {
        JsonObject globalConfig = container.config();

        JsonObject sms_app_cfg = globalConfig.getObject("sms_app", new JsonObject());
        //load config
        logger = getContainer().logger();
        JsonObject glbCfg = container.config();
        EventBus eb = vertx.eventBus();
        loadConfig(glbCfg);
        LoadCfg(sms_app_cfg);

        this.appSms = new AppSmsTranProcess(vertx, logger);
        this.thBuilder = new AppSmsBuildTranHis(logger,glbCfg);
        this.phonesDb = new PhonesDb(vertx.eventBus(), logger);

        mCom = new Common(vertx,logger, container.config());

        Handler<Message> myHandler = new Handler<Message>() {
            public void handle(Message message) {

                MomoMessage momoMessage = MomoMessage.fromBuffer((Buffer) message.body());

                container.logger().debug("[AppSmsVerticle]" + "Comming message : " + momoMessage);

                /*//tra loi da nhan duoc yeu cau
                CoreCommon.replyAckAppSms(message, momoMessage.cmdPhone);

                if (momoMessage == null) {
                    CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid, "");
                    logger.info("App sms " + AppSmsErrMapping.getMsg(AppSmsErrMapping.msg_format_not_valid));
                    return;
                }
                if (!CoreCommon.checkNumber(momoMessage.cmdPhone)) {
                    CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.number_not_valid, "");
                    logger.info("App sms so dien thoai khong hop le");
                    return;
                }*/

                switch (momoMessage.cmdType) {
                    case SmsProto.MsgType.APP_SMS_REQUEST_VALUE:
                        parseRequest(momoMessage);
                        break;
                    case SmsProto.MsgType.MUA123_REQUEST_VALUE:
                        mua123Process(momoMessage);
                        break;
                    default:
                        logger.error("Received message type from sms request channel is not valid, phone, msgType:"
                                + momoMessage.cmdPhone + "," + momoMessage.cmdType);
                        break;
                }
            }
        };

        eb.registerLocalHandler(AppConstant.AppSmsVerticle_ADDRESS, myHandler);
    }

    private void mua123Process(final MomoMessage momoMessage) {
        // format: <Order Id>_<Amount>
        // Ex: ORDER00001_2000000
        try {
            SmsProto.SmsRequest smsRequest = SmsProto.SmsRequest.parseFrom(momoMessage.cmdBody);
            String rawRequest = smsRequest.getRsaContent(); // SMS Content: BILLPAY 000000 123MUA 123456789 1000
            logger.debug("[AppSmsVerticle] [mua123Process] income message:" + rawRequest);
            String[] params = rawRequest.split(" ");
            if (params.length < 5) {
                logger.error("[AppSmsVerticle]: receives invalid format 123mua request -> Reject request");
                return;
            }


            try {
                String rawOrderId = params[3];
                final long amount = Long.parseLong(params[4]);
                final String pin = params[1];
                final int phoneNumber = momoMessage.cmdPhone;


                if (rawOrderId != null && !rawOrderId.toUpperCase().startsWith("VNG")) {
                    rawOrderId = "VNG_" + rawOrderId;
                }

                String[] arr = rawOrderId.split("\\."); // VNG_12345678.0987568815
                rawOrderId = arr[0];

                int rawAdditionalPhone = 0;
                if (arr.length >= 2) {
                    try {
                        rawAdditionalPhone = Integer.parseInt(arr[1]);
                    } catch (NumberFormatException e) {
                        logger.debug("Can't parse additional phone number", e);
                    }
                }


                final int additionalPhone = rawAdditionalPhone;
                final String orderId = rawOrderId;


                JsonObject cmd = new JsonObject()
                        .putNumber("cmdId", Partner123muaRequestVerticle.CMD_PAY_ORDER)
                        .putNumber("phoneNumber", phoneNumber)
                        .putString("orderId", orderId)
                        .putNumber("amount", amount)
                        .putString("pin", pin);

                vertx.eventBus().send(Partner123muaRequestVerticle.VERTICLE_ADDRESS, cmd, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> response) {
                        JsonObject obj = response.body();

                        String responseMessage = "";
                        switch (Pay123MuaOrderResult.valueOf(obj.getString("result"))) {
                            case SUCCESS:
                                responseMessage = String.format("<MoMo>Xin chuc mung quy khach da thanh toan thanh cong so tien coc %dvnd cho Ma Don Hang %s", amount, orderId);

                                try {
                                    int buyerPhone = Integer.parseInt(response.body().getObject("orderInfo").getString("sender_phone", "0"));
                                    if (buyerPhone != 0) {
                                        sendSms(buyerPhone, String.format("<MoMo>Xin chuc mung quy khach da thanh toan thanh cong so tien coc %dvnd cho Ma Don Hang %s", amount, orderId));
                                    }

                                    if (additionalPhone != 0 && additionalPhone != buyerPhone && additionalPhone != phoneNumber) {
                                        sendSms(additionalPhone, String.format("<MoMo>Tien coc cua Ma Don Hang %s da duoc thanh toan boi so dien thoai %s.",orderId, phoneNumber));
                                    }

                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }

                                break;
                            case USER_NOT_REGISTERED:
                                responseMessage = "(Dich vu Momo) Ban can den diem giao dich MoMo de tao tai khoan truoc khi thuc hien chuc nang thanh toan don dat hang tu 123Mua.";
                                break;
                            case INVALID_PIN_NUMBER:
                                responseMessage = String.format("<MoMo>Quy khach da nhap sai Ma PIN. Vui long thuc hien lai hoac goi (08)39917199 de duoc ho tro.");
                                break;
                            case PAID_ORDER:
                                responseMessage = String.format("<MoMo>Tien coc cua Ma Don Hang %s da duoc thanh toan tu truoc.", orderId);
                                break;
                            case TRANSFER_ERROR:
                                responseMessage = String.format("(Dich vu Momo) Thanh toan don dat hang 123mua(ma don hang: %s) KHONG thanh cong vi qua trinh chuyen tien bi loi.", orderId);
                                break;
                            case TRANSFER_EXCEPTION:
                                responseMessage = String.format("<MoMo>Loi he thong. Quy khach vui long thuc hien lai hoac goi (08)39917199 de duoc ho tro. (Ma loi %d.)", obj.getInteger("errorCode", Integer.MAX_VALUE));
                                break;
                            case AMOUNT_NOT_MATCH:
                                long amount = obj.getLong("amount", 0);
                                if (amount == 0)
                                    responseMessage = String.format("<MoMo>So tien coc can thanh toan khong phu hop.");
                                else
                                    responseMessage = String.format("<MoMo>So tien coc can thanh toan khong phu hop. Quy khach can thanh toan %dvnd.", amount);
                                break;
                            case CANT_GET_ORDER_INFO:
                                responseMessage = String.format("<MoMo>Khong the truy van thong tin Don Hang. Quy khach vui long thuc hien lai hoac goi (08)39917199 de duoc ho tro.");
                                break;
                            case ORDER_INVALID_INFO_SHOP_PHONE:
                                responseMessage = String.format("<MoMo>So dien thoai cua cua hang 123mua duoc cung cap khong chinh xac, quy khach vui long goi (08)39917199 de duoc ho tro.");
                                break;
                            case TARGET_NOT_REGISTERED:
                                responseMessage = String.format("<MoMo>So dien thoai cua chu cua hang 123mua chua dang ky tai khoan MoMo, quy khach vui long goi (08)39917199 de duoc ho tro.", orderId);
                                break;
                            case INVALID_ORDER_CODE:
                                responseMessage = String.format("<MoMo>Ma don hang khong ton tai. Quy khach vui long nhap lai hoac goi (08)39917199 de duoc ho tro.", orderId);
                                break;
                            case ACCESS_DENIED:
                                responseMessage = String.format("(<MoMo>Thanh toan don dat hang 123mua(ma don hang: %s) KHONG thanh cong vi tai khoan chua duoc dinh danh.", orderId);
                                break;
                            case NOT_ENOUGH_MONEY:
                                responseMessage = String.format("<MoMo>Tai khoan cua quy khach khong du kha nang thanh toan. Vui long nap tien de thuc hien giao dich.");
                                break;
                            case SYSTEM_ERROR:
                            case INVALID_INPUTS:
                            default:
                                responseMessage = String.format("<MoMo>Loi he thong. Quy khach vui long thuc hien lai hoac goi (08)39917199 de duoc ho tro.", orderId);
                                break;
                        }
                        sendSms(phoneNumber, responseMessage);
                    }
                });

            } catch (NumberFormatException e) {
                logger.warn("[AppSmsVerticle]: Can't parse money amount -> Reject request");
                sendSms(momoMessage.cmdPhone, "(Dich vu Momo) Lenh thanh toan don dat hang 123mua khong the thuc hien vi so tien nhap vao khong hop le!");
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void sendSms(int phoneNumber, String content) {
        logger.info(String.format("sendSms(%d, \"%s\"", phoneNumber, content));

        SoapProto.SendSms sendSms = SoapProto.SendSms.newBuilder()
                .setSmsId(0)
                .setToNumber(phoneNumber)
                .setContent(content)
                .build();

        vertx.eventBus().send(AppConstant.SmsVerticle_ADDRESS, sendSms.toByteArray());
    }

    private void parseRequest(final MomoMessage momoMessage) {
        //build tran his here
        final TranHisV1AndPin tranHisAndPin = buildTranHisAndPin(momoMessage);

        if (tranHisAndPin.smsCmdType.equalsIgnoreCase(AppSmsConst.TranType.GET_CURRENT_BAL)) {
            getCurrentBal(momoMessage, tranHisAndPin);
            return;
        }

        if (tranHisAndPin.tranHis == null) {
            /*CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_format_not_valid, "");*/
            logger.info("App sms khong build duoc tranhis object");
            return;
        }

        //co 1 tran his
        switch (tranHisAndPin.tranHis.getTranType()) {
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                appSms.processTopUp(momoMessage, tranHisAndPin);
                break;

            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                appSms.processTopUpGame(momoMessage, tranHisAndPin);
                break;

            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
                appSms.processBankIn(momoMessage, tranHisAndPin);
                break;

            case MomoProto.TranHisV1.TranType.BANK_OUT_VALUE:
                appSms.processBankOut(momoMessage, tranHisAndPin);
                break;

            case MomoProto.TranHisV1.TranType.M2C_VALUE:
                appSms.processM2C(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                appSms.processM2M(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE:
                appSms.processTransferMoneyToPlace(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TELEPHONE_VALUE:
                appSms.processBillPayTelephone(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                appSms.processBillPayTicketAirline(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_TRAIN_VALUE:
                appSms.processBillPayTicketTrain(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_INSURANCE_VALUE:
                appSms.processBillPayInsurance(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_INTERNET_VALUE:
                appSms.processBillPayInternet(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_OTHER_VALUE:
                appSms.processBillPayOther(momoMessage, tranHisAndPin);
                break;
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
                appSms.processBillPayElectric(momoMessage, tranHisAndPin);
                break;

           /* public final static String DEPOSIT_CASH_OTHER  = "13"; // NAP TIEN KHAC
            public final static String BUY_MOBILITY_CARD   = "14"; // MUA THE CAO
            public final static String BUY_GAME_CARD       = "15"; // MUA THE GAME
            public final static String BUY_OTHER           = "16"; // MUA KHAC*/

            case MomoProto.TranHisV1.TranType.DEPOSIT_CASH_OTHER_VALUE:
                appSms.processDepositCashOther(momoMessage, tranHisAndPin);
                break;
            default:
                logger.info("AppSMS process with invalid tran type " + tranHisAndPin.tranHis.getTranType());
                break;
        }
    }

    //supported.start
    public void LoadCfg(JsonObject smsAppCfg) {

        //for private
        rsa_pri_p = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pri_p", 0)));
        rsa_pri_q = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pri_q", 0)));
        rsa_pri_dP = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pri_dP", 0)));
        rsa_pri_dQ = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pri_dQ", 0)));
        rsa_pri_qInv = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pri_qInv", 0)));
        rsa_pri_exp = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pri_exp", 0)));

        //shared public and private
        rsa_pub_mod = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pub_mod", 0)));
        rsa_pub_exp = new BigInteger(String.valueOf(smsAppCfg.getNumber("rsa_pub_exp", 0)));
        try {

            RSACipher.publicKey = new RSAKeyParameters(false, rsa_pub_mod, rsa_pub_exp);
            RSACipher.logger = logger;

            RSACipher.privateKey = new RSAPrivateCrtKeyParameters(rsa_pub_mod
                    , rsa_pub_exp
                    , rsa_pri_exp
                    , rsa_pri_p
                    , rsa_pri_q
                    , rsa_pri_dP
                    , rsa_pri_dQ
                    , rsa_pri_qInv);
                                        /*RSAPrivateCrtKeyParameters(Pub_Mod
                                                                    , Pub_Exp
                                                                    , Priv_Exp
                                                                    , Priv_p
                                                                    , Priv_q
                                                                    , Priv_dp
                                                                    , Priv_dq
                                                                    , Priv_Inv);*/

        } catch (Exception ex) {
            logger.info("Can not generate privateKey");
        }
    }

    private TranHisV1AndPin buildTranHisAndPin(MomoMessage momoMessage) {
        //1. byte[] -->String
        String rawContent = new String(momoMessage.cmdBody).trim();

        System.out.println(rawContent);

        //2. String-->RSA -->true string

        //3. parse true string to build TranHis message

        //<ICCID>_<LEN>BILLPAY_<MPIN>_MSR8_<Unique Customer ID>_<AMOUNT>/<COUNTER><BASEID>
        String orgContent = rawContent;
        String[] arr = orgContent.split(sep);
        String iccid = arr.length > 0 ? arr[0] : "";
        int len = arr.length > 1 ? Integer.valueOf(arr[1]) : 0;
        int strLen = orgContent.length();
        String smsCmdType = arr.length > 2 ? arr[2] : "";
        String pin = arr.length > 3 ? arr[3] : "";

        if (pin.equalsIgnoreCase("")) {
            logger.info("pin after parsing : " + pin);
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.pin_not_valid, "");
            return null;
        }

        logger.info("linhpvt : iccid,len,tranType,pin :" + iccid + "," + len + "," + smsCmdType + "," + pin);
        logger.info("linhpvt : " + orgContent);

        if (strLen != len) {
            logger.info("AppSms incorrect length,contentLen,fieldLen " + strLen + "," + len);
            //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.msg_len_not_valid, "");
            return null;
        }

        MomoProto.TranHisV1 tranHis;

        switch (smsCmdType) {
            case AppSmsConst.TranType.TOP_UP:
                tranHis = thBuilder.TopUpTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.TOP_UP_VALUE);
                break;
            case AppSmsConst.TranType.TOP_UP_GAME:
                tranHis = thBuilder.TopUpGameTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE);
                break;
            case AppSmsConst.TranType.BANK_IN:
                tranHis = thBuilder.BankInTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
                break;
            case AppSmsConst.TranType.BANK_OUT:
                tranHis = thBuilder.BankOutTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BANK_OUT_VALUE);
                break;
            case AppSmsConst.TranType.M2M:
                tranHis = thBuilder.M2MTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.M2M_VALUE);
                break;
            case AppSmsConst.TranType.M2C:
                tranHis = thBuilder.M2CTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.M2C_VALUE);
                break;
            case AppSmsConst.TranType.TRANSFER_MONEY_2_PLACE:
                tranHis = thBuilder.TransferMoney2PlaceTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE);
                break;
            case AppSmsConst.TranType.BILL_PAY_INSURANCE:
                tranHis = thBuilder.BillPayInsuranceTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BILL_PAY_INSURANCE_VALUE);
                break;
            case AppSmsConst.TranType.BILL_PAY_INTERNET:
                tranHis = thBuilder.BillPayInternetTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BILL_PAY_INTERNET_VALUE);
                break;
            case AppSmsConst.TranType.BILL_PAY_TELEPHONE:
                tranHis = thBuilder.BillPayTelephoneTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BILL_PAY_TELEPHONE_VALUE);
                break;
            case AppSmsConst.TranType.BILL_PAY_TICKET_AIRLINE:
                tranHis = thBuilder.BillPayTicketAirlineTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE);
                break;
            case AppSmsConst.TranType.BILL_PAY_TICKET_TRAIN:
                tranHis = thBuilder.BillPayTicketTrainTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_TRAIN_VALUE);
                break;
            case AppSmsConst.TranType.BILL_PAY_OTHER:
                tranHis = thBuilder.BillPayOtherTranHis(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BILL_PAY_OTHER_VALUE);
                break;

            case AppSmsConst.TranType.DEPOSIT_CASH_OTHER:
                tranHis = thBuilder.DepositCashOther(momoMessage, orgContent, MomoProto.TranHisV1.TranType.DEPOSIT_CASH_OTHER_VALUE);
                break;
            case AppSmsConst.TranType.BUY_MOBILITY_CARD:
                tranHis = thBuilder.BuyMobilityCard(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BUY_MOBILITY_CARD_VALUE);
                break;

            case AppSmsConst.TranType.BUY_GAME_CARD:
                tranHis = thBuilder.BuyGameCard(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BUY_GAME_CARD_VALUE);
                break;

            case AppSmsConst.TranType.BUY_OTHER:
                tranHis = thBuilder.BuyOther(momoMessage, orgContent, MomoProto.TranHisV1.TranType.BUY_OTHER_VALUE);
                break;
            case AppSmsConst.TranType.BILL_PAY_ELECTRIC:
                tranHis = thBuilder.BillPayElectric(momoMessage, orgContent, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
                break;
            case AppSmsConst.TranType.MUA_123:
                tranHis = thBuilder.mua123(momoMessage, orgContent, MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
                break;

            default:
                tranHis = null;
                logger.info("linhpvt : default tran type " + smsCmdType);
                break;
        }
        return new TranHisV1AndPin(tranHis, pin, smsCmdType);
    }

    private void getCurrentBal(final MomoMessage momoMessage, final TranHisV1AndPin tranHisAndPin) {

        phonesDb.getPhoneObjInfo(momoMessage.cmdPhone, new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if ((obj != null) && (obj.momo > 0) && DataUtil.decode(obj.pin).equalsIgnoreCase(tranHisAndPin.pin)) {
                    /*CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, 0, "So du cua tai khoan 0"
                            + momoMessage.cmdPhone + " : "
                            + String.format("%,d", obj.momo).replace(",", ".") + " vnd");*/
                } else if (!DataUtil.decode(obj.pin).equalsIgnoreCase(tranHisAndPin.pin)) {
                    //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.pin_not_valid, "");
                } else {
                    //CoreCommon.sendSmsAppSms(momoMessage.cmdPhone, AppSmsErrMapping.system_error, "");
                }
            }
        });
    }

    //thanh toan thuc hien bang billpay
    //extra info ma khu vuc
    public static class TranHisV1AndPin {
        public MomoProto.TranHisV1 tranHis;
        public String pin;
        public String smsCmdType;

        public TranHisV1AndPin(MomoProto.TranHisV1 tranHis, String pin, String smsCmdType) {
            this.tranHis = tranHis;
            this.pin = pin;
            this.smsCmdType = smsCmdType;
        }
    }
    //supported.end
}
