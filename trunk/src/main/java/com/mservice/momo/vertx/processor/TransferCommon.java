package com.mservice.momo.vertx.processor;

import com.mservice.bank.entity.BankRequest;
import com.mservice.bank.entity.BankRequestFactory;
import com.mservice.bank.entity.BankResponse;
import com.mservice.momo.data.AutoNotiCountDb;
import com.mservice.momo.data.FeeDb;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.FromSource;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.referral.ReferralV1CodeInputDb;
import com.mservice.momo.data.tracking.BanknetTransDb;
import com.mservice.momo.data.tracking.TrackingBanknetDb;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.BanknetErrors;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.BankHelperVerticle;
import com.mservice.momo.vertx.data.SockData;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by concu on 4/19/14.
 */
public class TransferCommon {
    private boolean BANK_NET_TEST_MODE;
    private String VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET;
    private String VIETTINBANK_AGENT_ADJUST_FOR_BANKNET;

    private String REFUND_BANKNET_AGENT = "";
    private long REFUND_DATE_FROM = 0;
    private long REFUND_DATE_TO = 0;

    private Vertx vertx;
    private Logger logger;
    private PhonesDb phonesDb;
    private FeeDb feeDb;
    private Common mCom;

    //code for amway promotion
    private int amwayCodeMin = 7000;
    private int amwayCodeMax = 7009;
    private JsonObject jsonBank = new JsonObject();
    private PromotionProcess promotionProcess;
    private TrackingBanknetDb trackingBanknetDb;
    private BanknetTransDb banknetTransDb;
    private ReferralV1CodeInputDb referralV1CodeInputDb;
    private TransProcess transProcess;
    private AutoNotiCountDb autoNotiCountDb;
    public TransferCommon(Vertx vertx, Logger logger, JsonObject glbCfg) {
        loadConfig(glbCfg);
        this.vertx = vertx;
        this.logger = logger;
        feeDb = new FeeDb(vertx, logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        this.mCom = new Common(vertx, logger, glbCfg);
        referralV1CodeInputDb = new ReferralV1CodeInputDb(vertx, logger);
        //amway config
        JsonObject amwayPromo = glbCfg.getObject("amwaypromo", null);
        if (amwayPromo != null) {
            amwayCodeMin = amwayPromo.getInteger("codemin", 7000);
            amwayCodeMax = amwayPromo.getInteger("codemax", 7009);
        }
        promotionProcess = new PromotionProcess(vertx, logger, glbCfg);
        trackingBanknetDb = new TrackingBanknetDb(vertx, logger);
        banknetTransDb = new BanknetTransDb(vertx, logger);
        transProcess = new TransProcess(vertx, logger, glbCfg);
        autoNotiCountDb = new AutoNotiCountDb(vertx, logger);
    }

    public void loadConfig(JsonObject config) {
        VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET = config.getObject("server").getString("vietcombank_adjust_for_banknet", "0"); // duoc su dung lam tai khoan adjustment khi chuyen tien tu banknet -->momo
        VIETTINBANK_AGENT_ADJUST_FOR_BANKNET = config.getObject("server").getString("viettinbank_adjust_for_banknet", "0"); // duoc su dung lam tai khoan adjustment khi chuyen tien tu banknet -->momo}
        REFUND_BANKNET_AGENT = config.getObject("refund").getString("banknet_agent", "");
        REFUND_DATE_FROM = Misc.getDateAsLong(config.getObject("refund").getString("date_from", ""), "yyyy-MM-dd HH:mm:ss", logger, "Ngày bắt đầu hoàn phí");
        REFUND_DATE_TO = Misc.getDateAsLong(config.getObject("refund").getString("date_to", ""), "yyyy-MM-dd HH:mm:ss", logger, "Ngày kết thúc hoàn phí");
        jsonBank = config.getObject(StringConstUtil.BANK, new JsonObject());
    }

    public void doVerifyBanknet(final NetSocket sock
            , final MomoMessage msg
            , final SockData data
            , final TransferCommon.BankNetObj bnObj
            , final boolean nextstep
            , final Handler<FromSource.Obj> callback
            , final Handler<JsonObject> webCallback) {
        final int inout_city = 1;//xem nhu trong thanh pho
        final int channel = MomoProto.CardItem.Channel.BANKNET_VALUE;
        final int trantype = MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE;
        int tsource = 0; // App
        if (sock == null) {
            tsource = 1; //web
        }
        final int source = tsource;

        int feetype = MomoProto.FeeType.MOMO_VALUE; // tinh phi qua bieu phi cua momo
        long amount = 0;
        final String KEY_LOG = "doVerifyBanknet " + msg.cmdPhone;
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + msg.cmdPhone);
        log.add("func" + KEY_LOG, "doVerifyOtpBanknet");
        log.add("bankid" + KEY_LOG, (data == null ? "data = null" : data.bank_net_bank_code));
        log.add("channel" + KEY_LOG, channel);
        log.add("trantype" + KEY_LOG, MomoProto.TranHisV1.TranType.valueOf(trantype).name());
        log.add("IO city " + KEY_LOG, inout_city);
        log.add("fee type " + KEY_LOG, MomoProto.FeeType.valueOf(feetype).name());
        log.add("amount" + KEY_LOG, amount);

        log.add("func " + KEY_LOG, "getFee");
        feeDb.getFee(data.bank_net_bank_code, channel, trantype, inout_city, feetype, amount, new Handler<FeeDb.Obj>() {
            @Override
            public void handle(FeeDb.Obj obj) {

                int static_fee;
                double dynamic_fee;
                if (obj == null) {
                    static_fee = 1100;
                    dynamic_fee = 1.2;
                } else {
                    static_fee = obj.STATIC_FEE;
                    dynamic_fee = obj.DYNAMIC_FEE;
                }

                log.add("static fee " + KEY_LOG, static_fee);
                log.add("dynamic fee " + KEY_LOG, dynamic_fee);

                Misc.Cash cash = Misc.calculateAmount(bnObj.amount
                        , static_fee
                        , dynamic_fee
                        , MomoProto.CardItem.LockedType.FULL_VALUE, trantype, log);

                final long bnLockedAmt = cash.bankNetAmountLocked;
                final long corAdjustAmt = cash.coreAmountAdjust;
                final long fee = Misc.getRefundFee(REFUND_DATE_FROM
                        , REFUND_DATE_TO
                        , (cash.bankNetAmountLocked - cash.coreAmountAdjust)
                        , msg.cmdPhone
                        , logger);

                log.add("banknet locked amount " + KEY_LOG, bnLockedAmt);
                log.add("core adjusted amount " + KEY_LOG, corAdjustAmt);

                //build buffer to verify OTP
                Buffer buffer = MomoMessage.buildBuffer(
                        SoapProto.MsgType.BANK_NET_VERIFY_OTP_VALUE
                        , msg.cmdIndex
                        , msg.cmdPhone
                        , SoapProto.BankNetVerifyOtp.newBuilder()
                                .setAmount(bnLockedAmt)
                                .setMerchantTransId(bnObj.merchant_trans_id)
                                .setTransId(bnObj.trans_id)
                                .setOtp(bnObj.otp)
                                .build()
                                .toByteArray()
                );

                final long ackTime = System.currentTimeMillis();

                log.add("call to " + KEY_LOG, "banket verticle");
                //gui lenh xac thuc OTP den bank-net
                vertx.eventBus().send(AppConstant.BanknetVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> json) {

                        final JsonObject bnTranRpl = json.body();
                        final boolean verifyBnResult = bnTranRpl.getBoolean("result");
                        int vError;
                        if (verifyBnResult) {
                            vError = 0;
                        } else {
                            vError = bnTranRpl.getInteger("error") - 5000;
                        }

                        final int verifyOtpBnError = vError;

                        log.add("banknet result code " + KEY_LOG, verifyOtpBnError);
                        log.add("desc " + KEY_LOG, BanknetErrors.getDesc(verifyOtpBnError));

                        //verify otp on bank-net side success
                        if (bnTranRpl != null && verifyBnResult) {

                            //chon tai khoan ke qua qua vietcombank hoac viettinbank
                            String adjustAcc = VIETCOMBANK_AGENT_ADJUST_FOR_BANKNET;
                            if (data != null &&
                                    data.getPhoneObj() != null &&
                                    data.getPhoneObj().bank_code != null &&
                                    data.getPhoneObj().bank_code.equalsIgnoreCase("102")) {

                                adjustAcc = VIETTINBANK_AGENT_ADJUST_FOR_BANKNET;
                            }

                            final String finalBankNetAdjustAccount = adjustAcc;

                            Buffer buffer = MomoMessage.buildBuffer(
                                    SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE
                                    , msg.cmdIndex
                                    , msg.cmdPhone
                                    , SoapProto.commonAdjust.newBuilder()
                                            .setSource(finalBankNetAdjustAccount)
                                            .setTarget("0" + msg.cmdPhone)
                                            .setAmount(bnObj.amount)
                                            .setPhoneNumber("0" + msg.cmdPhone)
                                            .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                                    .setKey(Const.REFUND_AMOUNT).setValue(String.valueOf(fee)))
                                            .addExtraMap(SoapProto.keyValuePair.newBuilder()
                                                    .setKey(Const.REFUND_AGENT).setValue(REFUND_BANKNET_AGENT))
                                            .build()
                                            .toByteArray()
                            );

                            log.add("call to " + KEY_LOG, "soapin verticle");
                            log.add("amount " + KEY_LOG, bnObj.amount);
                            vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> result) {

                                    final JsonObject adjTranRpl = result.body();
                                    final int adjustResultCore = adjTranRpl.getInteger(colName.TranDBCols.ERROR);

                                    //gui xac nhan qua banknet da do tien vao tai khoan khach
                                    Buffer buf = MomoMessage.buildBuffer(SoapProto.MsgType.BANK_NET_CONFIRM_VALUE
                                            , msg.cmdIndex
                                            , msg.cmdPhone,
                                            SoapProto.BankNetConfirm.newBuilder()
                                                    .setMerchantTransId(bnObj.merchant_trans_id)
                                                    .setTransId(bnObj.trans_id)
                                                    .setAdjustResult(adjustResultCore)
                                                    .build().toByteArray()
                                    );

                                    log.add("comfirm merchant_trans_id " + KEY_LOG, bnObj.merchant_trans_id);
                                    log.add("comfirm trans_id " + KEY_LOG, bnObj.trans_id);
                                    log.add("comfirm adjust result " + KEY_LOG, adjustResultCore);

                                    vertx.eventBus().send(AppConstant.BanknetVerticle_ADDRESS, buf, new Handler<Message<JsonObject>>() {
                                        @Override
                                        public void handle(Message<JsonObject> objRpl) {
                                            boolean cfmResult = objRpl.body().getBoolean("result");
                                            final int banknet_cfm_result = (cfmResult ? 0 : (objRpl.body().getInteger("error") - 5000));
                                            log.add("banknet confirm result " + KEY_LOG, banknet_cfm_result);

                                            JsonObject jo = new JsonObject();
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.TYPE, UMarketOracleVerticle.BANKNET);
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.START_TIME, ackTime);
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.END_TIME, System.currentTimeMillis());
                                            jo.putString(UMarketOracleVerticle.BANKNET_COLS.CUSTOMER_ACCOUNT, "0" + msg.cmdPhone);
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.AMOUNT, bnLockedAmt);
                                            jo.putString(UMarketOracleVerticle.BANKNET_COLS.PARTNER_TRANS_ID, bnObj.trans_id);
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.PARTNER_CODE_CONFIRM, banknet_cfm_result);
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.INTERNAL_ERROR, adjustResultCore);
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.EXTERNAL_ERROR, verifyOtpBnError);
                                            jo.putNumber(UMarketOracleVerticle.BANKNET_COLS.SOURCE, source);

                                            vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> trackResult) {
                                                    log.add("send to ke toan result " + KEY_LOG, trackResult.body().toString());
                                                    log.writeLog();
                                                }
                                            });

                                            //khong thuc hien buoc ke tiep
                                            if (!nextstep) {
                                                //gui giao dich voi so tien thuc lanh
                                                adjTranRpl.putNumber(colName.TranDBCols.AMOUNT, corAdjustAmt);
                                                mCom.sendTransReply(vertx, adjTranRpl, ackTime, msg, sock, data, webCallback);

                                                FromSource.Obj fromSrc = new FromSource.Obj();
                                                fromSrc.Result = false;
                                                callback.handle(fromSrc);
                                                //callback.handle(false);
                                                log.add("next step " + KEY_LOG, nextstep);
//                                                promotionProcess.getUserInfoToCheckPromoProgram("","0" + msg.cmdPhone, null, DataUtil.strToLong(bnObj.trans_id), MomoProto.TranHisV1.TranType.BANK_NET_VERIFY_OTP_VALUE, corAdjustAmt, StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, data, new JsonObject());
                                                JsonObject joUpdate = new JsonObject();
                                                joUpdate.putNumber(colName.BanknetTransCol.TIME_VERIFY, System.currentTimeMillis());
                                                joUpdate.putNumber(colName.BanknetTransCol.RESULT_VERIFY, banknet_cfm_result);
                                                banknetTransDb.updatePartial(bnObj.merchant_trans_id, joUpdate, new Handler<Boolean>() {
                                                    @Override
                                                    public void handle(Boolean event) {
                                                        if(banknet_cfm_result == 0)
                                                        {
                                                            JsonObject joExtra = new JsonObject();
                                                            joExtra.putNumber(StringConstUtil.AMOUNT, corAdjustAmt);
                                                            joExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, DataUtil.strToLong(bnObj.trans_id));
                                                            joExtra.putString(StringConstUtil.BANK_CODE, "banknet");
                                                            joExtra.putString(colName.BanknetTransCol.MERCHANT_TRAN_ID, bnObj.merchant_trans_id);
                                                            promotionProcess.excuteAcquireBinhTanUserPromotion("0" + msg.cmdPhone, log, null, null, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joExtra);
                                                        }

                                                    }
                                                });
                                                log.writeLog();
                                                return;
                                            }

                                            //la giao dich trung gian --> luu lai
                                            adjTranRpl.putNumber(colName.TranDBCols.CLIENT_TIME, System.currentTimeMillis() - 10);
                                            adjTranRpl.putNumber(colName.TranDBCols.ACK_TIME, ackTime);
                                            adjTranRpl.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.BANK_NET_VERIFY_OTP_VALUE);
                                            adjTranRpl.putNumber(colName.TranDBCols.IO, 1);
                                            adjTranRpl.putNumber(colName.TranDBCols.CATEGORY, -1);
                                            adjTranRpl.putString(colName.TranDBCols.PARTNER_ID, data.card_holder_number);
                                            adjTranRpl.putString(colName.TranDBCols.PARTNER_CODE, data.bank_net_bank_code);
                                            adjTranRpl.putString(colName.TranDBCols.PARTNER_NAME, data.bank_net_bank_name);
                                            adjTranRpl.putNumber(colName.TranDBCols.AMOUNT, corAdjustAmt);
                                            adjTranRpl.putString(colName.TranDBCols.PARTNER_REF, bnObj.merchant_trans_id);
                                            adjTranRpl.putNumber(colName.TranDBCols.FROM_SOURCE, MomoProto.TranHisV1.SourceFrom.BANK_NET_2_VERIFY_OTP_VALUE);

                                            //send tran sync ve cho client
                                            mCom.saveAndSendTempTran(vertx, msg, adjTranRpl, sock, data);
                                            JsonObject joUpdate = new JsonObject();
                                            joUpdate.putNumber(colName.BanknetTransCol.TIME_VERIFY, System.currentTimeMillis());
                                            joUpdate.putNumber(colName.BanknetTransCol.RESULT_VERIFY, adjTranRpl.getInteger(colName.TranDBCols.ERROR, -1));
                                            banknetTransDb.updatePartial(bnObj.merchant_trans_id, joUpdate, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean event) {
                                                    //ra ben ngoai thuc hien buoc ke tiep
                                                    if (adjTranRpl.getInteger(colName.TranDBCols.ERROR, -1) == 0) {
                                                        JsonObject jsonReply = new JsonObject();
                                                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, DataUtil.strToLong(bnObj.trans_id));
                                                        jsonReply.putString(StringConstUtil.FINAL_BANKNET_ADJUST_ACCOUNT, finalBankNetAdjustAccount);
                                                        FromSource.Obj fromSrc = new FromSource.Obj();
                                                        fromSrc.Result = true;
                                                        fromSrc.ServiceSource = finalBankNetAdjustAccount;
                                                        callback.handle(fromSrc);
                                                        promotionProcess.getUserInfoToCheckPromoProgram("","0" + msg.cmdPhone, null, DataUtil.strToLong(bnObj.trans_id), MomoProto.TranHisV1.TranType.BANK_NET_VERIFY_OTP_VALUE,
                                                                corAdjustAmt, StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, data, new JsonObject());
                                                        //callback.handle(true);
                                                        JsonObject joExtra = new JsonObject();
                                                        joExtra.putNumber(StringConstUtil.AMOUNT, corAdjustAmt);
                                                        joExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, DataUtil.strToLong(bnObj.trans_id));
                                                        joExtra.putString(StringConstUtil.BANK_CODE, "banknet");
                                                        joExtra.putString(colName.BanknetTransCol.MERCHANT_TRAN_ID, bnObj.merchant_trans_id);
                                                        promotionProcess.excuteAcquireBinhTanUserPromotion("0" + msg.cmdPhone, log, null, null, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joExtra);
                                                    } else {
                                                        //gui giao dich goc
                                                        adjTranRpl.putNumber(colName.TranDBCols.IO, -1);
                                                        mCom.sendTransReply(vertx, adjTranRpl, ackTime, msg, sock, data, webCallback);
                                                        FromSource.Obj fromSrc = new FromSource.Obj();
                                                        fromSrc.Result = false;
                                                        callback.handle(fromSrc);
                                                        //callback.handle(false);
                                                    }
                                                    log.writeLog();
                                                }
                                            });

                                            //ra ben ngoai thuc hien buoc ke tiep
                                            if (adjTranRpl.getInteger(colName.TranDBCols.ERROR, -1) == 0) {
                                                JsonObject jsonReply = new JsonObject();
                                                jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, DataUtil.strToLong(bnObj.trans_id));
                                                jsonReply.putString(StringConstUtil.FINAL_BANKNET_ADJUST_ACCOUNT, finalBankNetAdjustAccount);
                                                FromSource.Obj fromSrc = new FromSource.Obj();
                                                fromSrc.Result = true;
                                                fromSrc.ServiceSource = finalBankNetAdjustAccount;
                                                callback.handle(fromSrc);
                                                promotionProcess.getUserInfoToCheckPromoProgram("","0" + msg.cmdPhone, null, DataUtil.strToLong(bnObj.trans_id), MomoProto.TranHisV1.TranType.BANK_NET_VERIFY_OTP_VALUE,
                                                        corAdjustAmt, StringConstUtil.OctoberPromoProgram.OCTOBER_PROMO, data, new JsonObject());
                                                JsonObject joExtra = new JsonObject();
                                                joExtra.putNumber(StringConstUtil.AMOUNT, corAdjustAmt);
                                                joExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, DataUtil.strToLong(bnObj.trans_id));
                                                promotionProcess.excuteAcquireBinhTanUserPromotion("0" + msg.cmdPhone, log, null, null, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joExtra);

                                                //callback.handle(true);
                                            } else {
                                                //gui giao dich goc
                                                adjTranRpl.putNumber(colName.TranDBCols.IO, -1);
                                                mCom.sendTransReply(vertx, adjTranRpl, ackTime, msg, sock, data, webCallback);

                                                FromSource.Obj fromSrc = new FromSource.Obj();
                                                fromSrc.Result = false;
                                                callback.handle(fromSrc);
                                                //callback.handle(false);
                                            }

                                            log.writeLog();
                                        }
                                    });
                                }
                            });
                        } else {

                            //verify on bank-net side failed
                            JsonObject joUpdate = new JsonObject();
                            joUpdate.putNumber(colName.BanknetTransCol.TIME_VERIFY, System.currentTimeMillis());
                            joUpdate.putNumber(colName.BanknetTransCol.RESULT_VERIFY, -1000);
                            banknetTransDb.updatePartial(bnObj.merchant_trans_id, joUpdate, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean event) {
                                    JsonObject bnRpl = json.body();
                                    log.add("banknet verify OTP result " + KEY_LOG, bnRpl.toString());
                                    log.writeLog();
                                    JsonObject tranReply = Misc.getJsonObjRpl(bnRpl.getInteger(colName.TranDBCols.ERROR, -1)
                                            , -1
                                            , bnObj.amount - 1000
                                            , -1);
                                    mCom.sendTransReply(vertx, tranReply, ackTime, msg, sock, data, webCallback);
                                    FromSource.Obj fromSrc = new FromSource.Obj();
                                    fromSrc.Result = false;
                                    callback.handle(fromSrc);
                                }
                            });
                        }
                    }
                });

            }
        });
    }

    private void keepTrackInfoTran(String billId, String phoneNumber, String tranId, JsonObject joReply) {
        TrackingBanknetDb.Obj trackBanknetObj = new TrackingBanknetDb.Obj();
        trackBanknetObj.billId = billId;
        trackBanknetObj.phoneNumber = phoneNumber;
        trackBanknetObj.time = System.currentTimeMillis();
        trackBanknetObj.tranId = tranId;
        trackBanknetObj.joExtra = joReply;
        trackingBanknetDb.insert(trackBanknetObj, new Handler<Integer>() {
            @Override
            public void handle(Integer event) {

            }
        });
    }
    //chuyen tien tu bank vcb/vtb --> momo
    public void doBankIn(final MomoProto.TranHisV1 request, final NetSocket sock, final MomoMessage msg
            , final SockData data
            , final TransferCommon.BankLinkedObj blObj
            , final boolean nextstep
            , final Handler<FromSource.Obj> callback
            , final Handler<JsonObject> webCallback) {

        final long ackTime = System.currentTimeMillis();
        String allow_bank_via_connector = jsonBank.getString(StringConstUtil.ALLOW_BANK_VIA_CONNECTOR, "cluster");
        String channel = Const.CHANNEL_MOBI;
        if (sock == null) {
            channel = Const.CHANNEL_WEB;
        }
        String otp = "";
        String bankVerticle = jsonBank.getString(StringConstUtil.BANK_CONNECTOR_VERTICLE, "bankVerticle");
        Buffer bankin = MomoMessage.buildBuffer(
                SoapProto.MsgType.BANK_IN_VALUE,
                msg.cmdIndex,
                msg.cmdPhone,
                SoapProto.BankIn.newBuilder()
                        .setMpin(data.pin)
                        .setChannel(channel)
                        .setAmount(blObj.amount)
                        .setBankCode(blObj.bank_code)
                        .build()
                        .toByteArray()
        );

        logger.info("Transfer processBankIn,from_bank,bank_code,amount:" + blObj.from_bank
                + "," + blObj.bank_code
                + "," + blObj.amount);
//        JsonArray share = request.getShare().equalsIgnoreCase("") ? new JsonArray() : new JsonArray(request.getShare());

//        if (share.size() > 0) {
//            for (Object o : share) {
//                otp = ((JsonObject) o).getString("otp", "");
//                if (!otp.equalsIgnoreCase("")) break;
//            }
//        }
        try {
            JsonObject jsonShare = request.getShare().equalsIgnoreCase("") ? new JsonObject() : new JsonObject(request.getShare());
            otp = jsonShare.getString("otp", "");
        } catch (Exception ex) {
            JsonArray share = request.getShare().equalsIgnoreCase("") ? new JsonArray() : new JsonArray(request.getShare());
            if (share.size() > 0) {
                for (Object o : share) {
                    otp = ((JsonObject) o).getString("otp", "");
                    if (!otp.equalsIgnoreCase("")) break;
                }
            }
        }
        logger.info("otp " + otp);
        final Map<String, String> kvps = new HashMap<>();
        kvps.put("client", "backend");
        kvps.put("chanel", "mobi");
        kvps.put("issms", "no");
        final Common.BuildLog log = new Common.BuildLog(logger);
//        if ("cluster".equalsIgnoreCase(allow_bank_via_connector)) {
//            log.add("", "bankIn by cluster");
//            String pin = data.pin;
//            if (StringConstUtil.bankLinkVerify.containsKey(blObj.bank_code) && StringConstUtil.bankLinkVerify.get(blObj.bank_code)) {
//                pin = otp;
//            }
//            BankRequest bankRequest = BankRequestFactory.cashinRequest(blObj.bank_code, "0" + msg.cmdPhone, pin, System.currentTimeMillis(), blObj.amount, otp, kvps);
//            vertx.eventBus().sendWithTimeout(bankVerticle, bankRequest.getJsonObject(), 60 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
//                @Override
//                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
//                    final JsonObject jsonRep = new JsonObject();
//                    FromSource.Obj formSourceObj = new FromSource.Obj();
//                    if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
//                        final BankResponse bankResponse = new BankResponse(messageAsyncResult.result().body());
//                        log.add("result", "Co ket qua tra ve tu connector");
//                        log.add("bankResponse", bankResponse);
//
//                        if (bankResponse != null && bankResponse.getResultCode() == 0) {
//                            //Tra ket qua thanh cong ve cho client
//                            log.add("resultCode", bankResponse.getResultCode());
//                            log.add("transId", bankResponse.getRequest().getCoreTransId());
//                            formSourceObj.Result = true;
//                            formSourceObj.ServiceSource = jsonRep.toString();
//                            callback.handle(formSourceObj);
//                            vertx.setTimer(1000L * 10, new Handler<Long>() {
//                                @Override
//                                public void handle(Long waitingTimeToBonus) {
//                                    vertx.cancelTimer(waitingTimeToBonus);
//                                    doPromotionMethod(msg, bankResponse, blObj, log, data, jsonRep);
//                                }
//                            });
//                            log.writeLog();
//
//                        } else if (bankResponse != null && bankResponse.getResultCode() != 0) {
//                            int errorCode = bankResponse == null ? 1000 : bankResponse.getResultCode();
//                            String desc = bankResponse == null ? "Nạp tiền không thành công" : bankResponse.getDescription();
//                            log.add("errorCode", errorCode);
//                            log.add("desc", desc);
//                            jsonRep.putNumber(StringConstUtil.STATUS, 5);
//                            jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
//                            jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
//                            formSourceObj.Result = false;
//                            callback.handle(formSourceObj);
//                            log.writeLog();
//                        } else {
//                            log.add("errorCode", "bank response is null");
//                            jsonRep.putNumber(StringConstUtil.STATUS, 5);
//                            jsonRep.putNumber(StringConstUtil.ERROR, 1000);
//                            jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
//                            formSourceObj.Result = false;
//                            callback.handle(formSourceObj);
//                            log.writeLog();
//                        }
//
//                        return;
//                    } else {
//                        //Tra loi time out, khong ket noi duoc voi connector
//                        log.add("errorCode", "time out");
//                        jsonRep.putNumber(StringConstUtil.STATUS, 5);
//                        jsonRep.putNumber(StringConstUtil.ERROR, 1000);
//                        jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
//                        formSourceObj.Result = false;
//                        callback.handle(formSourceObj);
//                        log.writeLog();
//                        return;
//                    }
//                }
//            });
//            return;
//        } else
        if ("rest_ws".equalsIgnoreCase(allow_bank_via_connector)) {
            log.add("", "bankIn by rest_ws");
            final String finalOTP = otp;
            phonesDb.getPhoneObjInfo(msg.cmdPhone, new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj obj) {

                    if (StringUtils.isEmpty(blObj.bank_code)) {
                        blObj.bank_code = obj != null ? (StringUtils.isEmpty(obj.bank_code) ? "0" : obj.bank_code) : "0";
                    }
                    log.add("bank code again", blObj.bank_code);

                    String pin = data.pin;
                    if (StringConstUtil.bankLinkVerify.containsKey(blObj.bank_code) && StringConstUtil.bankLinkVerify.get(blObj.bank_code) && !"".equalsIgnoreCase(finalOTP)) {
                        pin = finalOTP;
                    }
                    final BankRequest bankRequest = BankRequestFactory.cashinRequest(blObj.bank_code, "0" + msg.cmdPhone, pin, System.currentTimeMillis(), blObj.amount, finalOTP, kvps);

                    JsonObject joBankReq = new JsonObject();
                    joBankReq.putNumber(BankHelperVerticle.COMMAND, BankHelperVerticle.BANK_IN_OUT);
                    joBankReq.putString(BankHelperVerticle.PHONE, "0" + msg.cmdPhone);
                    joBankReq.putObject(BankHelperVerticle.DATA, bankRequest.getJsonObject());

                    vertx.eventBus().sendWithTimeout(AppConstant.BankHelperVerticle_ADDRESS, joBankReq, 80 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                            final JsonObject jsonRep = new JsonObject();
                            FromSource.Obj formSourceObj = new FromSource.Obj();
                            if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {

                                JsonObject reponse = messageAsyncResult.result().body();
                                log.add("proxy_bank ws's response", reponse.toString());
                                if (reponse.getInteger(BankHelperVerticle.ERROR) != 0) {
                                    //Tra loi time out, khong ket noi duoc voi connector
                                    log.add("errorCode", "time out");
                                    jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                    jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                                    formSourceObj.Result = false;
                                    callback.handle(formSourceObj);
                                    log.writeLog();
                                    return;
                                }

                                final BankResponse bankResponse = new BankResponse(reponse.getObject(BankHelperVerticle.DATA));
                                log.add("result", "Co ket qua tra ve tu connector");
                                log.add("bankResponse", bankResponse);

                                if (bankResponse != null && bankResponse.getResultCode() == 0) {
                                    //Tra ket qua thanh cong ve cho client
                                    log.add("resultCode", bankResponse.getResultCode());
                                    log.add("transId", bankResponse.getRequest().getCoreTransId());
                                    formSourceObj.Result = true;
                                    formSourceObj.ServiceSource = jsonRep.toString();
                                    callback.handle(formSourceObj);
//                                    log.writeLog();

                                    vertx.setTimer(1000L * 10, new Handler<Long>() {
                                        @Override
                                        public void handle(Long waitingBonusTime) {
                                            vertx.cancelTimer(waitingBonusTime);
                                            doPromotionMethod(msg, bankResponse, blObj, log, data, jsonRep, bankRequest);
                                        }
                                    });
                                } else if (bankResponse != null && bankResponse.getResultCode() != 0) {
                                    int errorCode = bankResponse == null ? 1000 : bankResponse.getResultCode();
                                    String desc = bankResponse == null ? "Nạp tiền không thành công" : bankResponse.getDescription();
                                    log.add("errorCode", errorCode);
                                    log.add("desc", desc);
                                    jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                    jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
                                    jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
                                    formSourceObj.Result = false;
                                    callback.handle(formSourceObj);
                                    log.writeLog();
                                } else {
                                    log.add("errorCode", "bank response is null");
                                    jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                    jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                                    formSourceObj.Result = false;
                                    callback.handle(formSourceObj);
                                    log.writeLog();
                                }

                                return;
                            } else {
                                //Tra loi time out, khong ket noi duoc voi connector
                                log.add("errorCode", "time out");
                                jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                                jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                                formSourceObj.Result = false;
                                callback.handle(formSourceObj);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                    return;
                }
            });
        }
//        else {
//            vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, bankin, new Handler<Message<JsonObject>>() {
//                @Override
//                public void handle(Message<JsonObject> result) {
//
//                    JsonObject tranRpl = result.body();
//
//                    //vcb dev
////                tranRpl.putNumber(colName.TranDBCols.ERROR,0);
////                tranRpl.putNumber(colName.TranDBCols.STATUS,4);
//
//                    if (!nextstep) {
//                        mCom.sendTransReply(vertx, result.body(), ackTime, msg, sock, data, webCallback);
//
//                        FromSource.Obj frmSrcObj = new FromSource.Obj();
//                        frmSrcObj.Result = false;
//                        callback.handle(frmSrcObj);
//                        return;
//                    }
//
//                    //luu va tra thong tin ket qua giao dich trung gian ve
//                /*
//                obj.CID = cid;
//                obj.CTIME = System.currentTimeMillis();
//                obj.TYPE = MomoProto.TranHisV1.TranType.BANK_IN_VALUE;
//                obj.IO = 1;
//                obj.CAT = -1;
//                obj.PARTNER_ID = "Ngân hàng liên kết";
//                obj.PARTNER_CODE = bankCode;
//                obj.PARTNER_NAME = bankName;
//                obj.AMOUNT = amount;
//                //obj.COMMENT = "Nạp tiền vào tài khoản MoMo từ ngân hàng liên kết " + bankName;
//                obj.COMMENT = "";
//                obj.SOURCE_FORM = MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE;
//                */
//                    tranRpl.putNumber(colName.TranDBCols.CLIENT_TIME, System.currentTimeMillis());
//                    tranRpl.putNumber(colName.TranDBCols.ACK_TIME, ackTime);
//                    tranRpl.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
//                    tranRpl.putNumber(colName.TranDBCols.IO, 1);
//                    tranRpl.putNumber(colName.TranDBCols.CATEGORY, -1);
//                    tranRpl.putString(colName.TranDBCols.PARTNER_ID, "Ngân hàng liên kết");
//                    tranRpl.putString(colName.TranDBCols.PARTNER_CODE, blObj.bank_code);
//                    tranRpl.putString(colName.TranDBCols.PARTNER_NAME, blObj.from_bank);
//                    tranRpl.putNumber(colName.TranDBCols.AMOUNT, blObj.amount);
//                    tranRpl.putNumber(colName.TranDBCols.FROM_SOURCE, MomoProto.TranHisV1.SourceFrom.BANKLINKED_VALUE);
//
//                    mCom.saveAndSendTempTran(vertx, msg, tranRpl, sock, data);
//
//                    //set service name
//                    FromSource.Obj frmSrcObj = new FromSource.Obj();
//                    if (tranRpl.getInteger(colName.TranDBCols.ERROR, -1) == 0) {
//
//                        frmSrcObj.Result = true;
//                        frmSrcObj.ServiceSource = blObj.bank_code;
//                        callback.handle(frmSrcObj);
//
//                    } else {
//
//                        tranRpl.putNumber(colName.TranDBCols.IO, -1);
//                        mCom.sendTransReply(vertx, tranRpl, ackTime, msg, sock, data, webCallback);
//                        frmSrcObj.Result = false;
//                        callback.handle(frmSrcObj);
//                    }
//
//                    //thuc hien khuyen mai o day
//                    long tranId = tranRpl.getLong(colName.TranDBCols.TRAN_ID, 0);
//                    int error = tranRpl.getInteger(colName.TranDBCols.ERROR, SoapError.SYSTEM_ERROR);
//
//                    if (error == 0) {
//
//                        PhonesDb.Obj phoneObj = ((data == null || data.getPhoneObj() == null) ? null : data.getPhoneObj());
//                        String inviter = (phoneObj == null || phoneObj.inviter == null) ? "" : phoneObj.inviter;
//                        if (phoneObj == null) return;
//
//                        //chay chuong trinh uu dai map vi viettinbank vnpt-ha noi.
//                        if ("102".equalsIgnoreCase(blObj.bank_code)) {
//                            VcbCommon.requestGiftMomoViettinbank(vertx
//                                    , msg.cmdPhone
//                                    , "vtbmomo"
//                                    , blObj.amount
//                                    , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
//                                    , tranId
//                                    , phoneObj.bankPersonalId
//                                    , blObj.bank_code
//                                    , false
//                                    , 0
//                                    , "vnpthn"
//                                    , "vtbpromo"
//                                    , false
//                                    , ReqObj.online, inviter, new Handler<JsonObject>() {
//                                        @Override
//                                        public void handle(JsonObject jsonObject) {
//                                        }
//                                    });
//                            return;
//                        }
//
//                        //vcb theo ma momo
//                        if ("momo".equalsIgnoreCase(phoneObj.inviter.trim())) {
//                            VcbCommon.requestGiftMomoForB(vertx
//                                    , msg.cmdPhone
//                                    , phoneObj.inviter.trim()
//                                    , blObj.amount
//                                    , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
//                                    , tranId
//                                    , phoneObj.bankPersonalId
//                                    , phoneObj.bank_code, true);
//                            return;
//                        }
//
//                        //tra khuyen mai cho chuong trinh promotion girl va end-user
//                        if (DataUtil.strToInt(phoneObj.inviter) >= 6100 && DataUtil.strToInt(phoneObj.inviter) <= 6200) {
//
//                            //tra thuong cho end-user nhap ma gioi thieu la PG
//                            VcbCommon.requestGiftMomoForB(vertx
//                                    , msg.cmdPhone
//                                    , String.valueOf(DataUtil.strToInt(phoneObj.inviter))
//                                    , 0
//                                    , 0
//                                    , 0, phoneObj.bankPersonalId
//                                    , phoneObj.bank_code
//                                    , true);
//
//                            return;
//                        }
//
//                        // tra khuyen mai theo amway
//                        if (DataUtil.strToInt(phoneObj.inviter) >= amwayCodeMin && DataUtil.strToInt(phoneObj.inviter) <= amwayCodeMax) {
//
//                            //tra thuong cho end-user nhap ma gioi thieu la PG
//                            VcbCommon.requestGiftMomoForAmwayPromo(vertx
//                                    , msg.cmdPhone
//                                    , String.valueOf(DataUtil.strToInt(phoneObj.inviter))
//                                    , 0
//                                    , 0
//                                    , 0, phoneObj.bankPersonalId
//                                    , phoneObj.bank_code
//                                    , true);
//
//                            return;
//                        }
//
//                        //vcb theo so dien thoai nguoi gioi thieu
//                        if (DataUtil.strToInt(phoneObj.inviter) > 0) {
//                            VcbCommon.requestGiftForB(vertx
//                                    , msg.cmdPhone
//                                    , phoneObj.inviter
//                                    , blObj.amount
//                                    , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
//                                    , tranId);
//
//                            return;
//                        }
//                    }
//
//                }
//            });
//        }
    }

    private void doPromotionMethod(final MomoMessage msg, final BankResponse bankResponse, final BankLinkedObj blObj, final Common.BuildLog log, final SockData data, JsonObject jsonRep, BankRequest bankRequest) {
        JsonObject joInfoExtra = new JsonObject();
        joInfoExtra.putString(StringConstUtil.NUMBER, "0" + msg.cmdPhone);
        joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, bankResponse.getRequest().getCoreTransId());
        joInfoExtra.putNumber(StringConstUtil.AMOUNT, blObj.amount);
        joInfoExtra.putString(StringConstUtil.SERVICE_ID, "bankin");
        joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_TYPE, MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
        joInfoExtra.putString(StringConstUtil.BANK_CODE, "bankin");

        promotionProcess.excuteAcquireBinhTanUserPromotion("0" + msg.cmdPhone, log, msg, data, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joInfoExtra);
        jsonRep.putNumber(StringConstUtil.STATUS, 4);
        jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
        jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
        jsonRep.putNumber(StringConstUtil.TRANDB_TRAN_ID, bankResponse.getRequest().getCoreTransId());
//       todo: write code to notifi referal promotion event

        //Nap tien thanh cong, kiem tra tra khuyen mai
        referralV1CodeInputDb.findOne("0" + msg.cmdPhone, new Handler<ReferralV1CodeInputDb.Obj>() {
            @Override
            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
                if(referralObj == null)
                {
                    promotionProcess.getUserInfoToCheckPromoProgram("","0" + msg.cmdPhone, null, bankResponse.getRequest().getCoreTransId(), MomoProto.TranHisV1.TranType.BANK_IN_VALUE, blObj.amount, StringConstUtil.WomanNationalField.PROGRAM, data, new JsonObject());
                }
                else {
                    promotionProcess.executeReferralPromotion("0" + msg.cmdPhone, StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.CASH_IN, null, null, data, bankResponse, log, new JsonObject());
                }
            }
        });
        promotionProcess.executePromotion("", MomoProto.TranHisV1.TranType.BANK_IN_VALUE, "", msg.cmdPhone,
                bankResponse.getRequest().getCoreTransId(), bankRequest.getAmount(), data, "bankin", null, 0, 1, log, new JsonObject(), new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject event) {

                    }
                });
        promotionProcess.executeRefferalNotify("0" + msg.cmdPhone, blObj.bank_code, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,log, joInfoExtra);
    }

//    private void doPromotionMethod(final MomoMessage msg, final BankResponse bankResponse, final BankLinkedObj blObj, final Common.BuildLog log, final SockData data, JsonObject jsonRep) {
//        JsonObject joInfoExtra = new JsonObject();
//        joInfoExtra.putString(StringConstUtil.NUMBER, "0" + msg.cmdPhone);
//        joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_ID, bankResponse.getRequest().getCoreTransId());
//        joInfoExtra.putNumber(StringConstUtil.AMOUNT, blObj.amount);
//        joInfoExtra.putString(StringConstUtil.SERVICE_ID, "bankin");
//        joInfoExtra.putNumber(StringConstUtil.TRANDB_TRAN_TYPE, MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
//        joInfoExtra.putString(StringConstUtil.BANK_CODE, "bankin");
//        promotionProcess.excuteAcquireBinhTanUserPromotion("0" + msg.cmdPhone, log, msg, data, StringConstUtil.BinhTanPromotion.MSG_TYPE_BINHTAN_PROMOTION.CASH_IN, joInfoExtra);
//
//        jsonRep.putNumber(StringConstUtil.STATUS, 4);
//        jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
//        jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
//        jsonRep.putNumber(StringConstUtil.TRANDB_TRAN_ID, bankResponse.getRequest().getCoreTransId());
//
//        //Nap tien thanh cong, kiem tra tra khuyen mai
//        referralV1CodeInputDb.findOne("0" + msg.cmdPhone, new Handler<ReferralV1CodeInputDb.Obj>() {
//            @Override
//            public void handle(ReferralV1CodeInputDb.Obj referralObj) {
//                if(referralObj == null)
//                {
//                    promotionProcess.getUserInfoToCheckPromoProgram("","0" + msg.cmdPhone, null, bankResponse.getRequest().getCoreTransId(), MomoProto.TranHisV1.TranType.BANK_IN_VALUE, blObj.amount, StringConstUtil.WomanNationalField.PROGRAM, data, new JsonObject());
//                }
//                else {
//                    promotionProcess.executeReferralPromotion("0" + msg.cmdPhone, StringConstUtil.ReferralVOnePromoField.MSG_TYPE_REFERRAL.CASH_IN, null, null, data, bankResponse, log, new JsonObject());
//                }
//            }
//        });
//
//
//        //todo: write code to notifi referal promotion event
//        promotionProcess.executeRefferalNotify("0" + msg.cmdPhone, blObj.bank_code, StringConstUtil.ReferralVOnePromoField.REFERRAL_PROGRAM,log, joInfoExtra);
//    }

    //chuyen tien tu bank vcb/vtb --> momo
    public void doBankInViaSDK(
            final String phoneNumber
            , final SockData data
            , final TransferCommon.BankLinkedObj blObj
            , final Handler<FromSource.Obj> callback) {

        final long ackTime = System.currentTimeMillis();
        String allow_bank_via_connector = jsonBank.getString(StringConstUtil.ALLOW_BANK_VIA_CONNECTOR, "cluster");
        String channel = Const.CHANNEL_MOBI;

        String bankVerticle = jsonBank.getString(StringConstUtil.BANK_CONNECTOR_VERTICLE, "bankVerticle");

        logger.debug("Transfer processBankInViaSDK,from_bank,bank_code,amount:" + blObj.from_bank
                + "," + blObj.bank_code
                + "," + blObj.amount);

        final Map<String, String> kvps = new HashMap<>();
        kvps.put("client", "backend");
        kvps.put("chanel", "mobi");
        kvps.put("issms", "no");
        final Common.BuildLog log = new Common.BuildLog(logger);
        if ("cluster".equalsIgnoreCase(allow_bank_via_connector)) {
            log.add("", "bankIn by cluster");
            BankRequest bankRequest = BankRequestFactory.cashinRequest(blObj.bank_code, phoneNumber, data.pin, System.currentTimeMillis(), blObj.amount, "", kvps);
            vertx.eventBus().sendWithTimeout(bankVerticle, bankRequest.getJsonObject(), 60 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                    JsonObject jsonRep = new JsonObject();
                    FromSource.Obj formSourceObj = new FromSource.Obj();
                    if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {
                        BankResponse bankResponse = new BankResponse(messageAsyncResult.result().body());
                        log.add("result", "Co ket qua tra ve tu connector");
                        log.add("bankResponse", bankResponse);

                        if (bankResponse != null && bankResponse.getResultCode() == 0) {
                            //Tra ket qua thanh cong ve cho client
                            log.add("resultCode", bankResponse.getResultCode());
                            log.add("transId", bankResponse.getRequest().getCoreTransId());
                            jsonRep.putNumber(StringConstUtil.STATUS, 4);
                            jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
                            jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
                            jsonRep.putNumber(StringConstUtil.TRANDB_TRAN_ID, bankResponse.getRequest().getCoreTransId());
                            formSourceObj.Result = true;
                            formSourceObj.ServiceSource = jsonRep.toString();
                            //Nap tien thanh cong, kiem tra tra khuyen mai
                            promotionProcess.getUserInfoToCheckPromoProgram("",phoneNumber, null, bankResponse.getRequest().getCoreTransId(), MomoProto.TranHisV1.TranType.BANK_IN_VALUE, blObj.amount, StringConstUtil.WomanNationalField.PROGRAM, data, new JsonObject());
                        } else if (bankResponse != null && bankResponse.getResultCode() != 0) {
                            int errorCode = bankResponse == null ? 1000 : bankResponse.getResultCode();
                            String desc = bankResponse == null ? "Nạp tiền không thành công" : bankResponse.getDescription();
                            log.add("errorCode", errorCode);
                            log.add("desc", desc);
                            jsonRep.putNumber(StringConstUtil.STATUS, 5);
                            jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
                            jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
                            formSourceObj.Result = false;
                        } else {
                            log.add("errorCode", "bank response is null");
                            jsonRep.putNumber(StringConstUtil.STATUS, 5);
                            jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                            jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                            formSourceObj.Result = false;
                        }
                        callback.handle(formSourceObj);
                        log.writeLog();
                        return;
                    } else {
                        //Tra loi time out, khong ket noi duoc voi connector
                        log.add("errorCode", "time out");
                        jsonRep.putNumber(StringConstUtil.STATUS, 5);
                        jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                        jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                        formSourceObj.Result = false;
                        callback.handle(formSourceObj);
                        log.writeLog();
                        return;
                    }
                }
            });
            return;
        }
        else if ("rest_ws".equalsIgnoreCase(allow_bank_via_connector)) {
            log.add("", "bankIn by rest_ws");

            phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
                @Override
                public void handle(PhonesDb.Obj obj) {

                    if (StringUtils.isEmpty(blObj.bank_code)) {
                        blObj.bank_code = obj != null ? (StringUtils.isEmpty(obj.bank_code) ? "0" : obj.bank_code) : "0";
                    }
                    log.add("bank code again", blObj.bank_code);

                    BankRequest bankRequest = BankRequestFactory.cashinRequest(blObj.bank_code, phoneNumber, data.pin, System.currentTimeMillis(), blObj.amount, "", kvps);

                    JsonObject joBankReq = new JsonObject();
                    joBankReq.putNumber(BankHelperVerticle.COMMAND, BankHelperVerticle.BANK_IN_OUT);
                    joBankReq.putString(BankHelperVerticle.PHONE, phoneNumber);
                    joBankReq.putObject(BankHelperVerticle.DATA, bankRequest.getJsonObject());

                    vertx.eventBus().sendWithTimeout(AppConstant.BankHelperVerticle_ADDRESS, joBankReq, 80 * 1000L, new Handler<AsyncResult<Message<JsonObject>>>() {
                        @Override
                        public void handle(AsyncResult<Message<JsonObject>> messageAsyncResult) {
                            JsonObject jsonRep = new JsonObject();
                            FromSource.Obj formSourceObj = new FromSource.Obj();
                            if (messageAsyncResult.succeeded() && messageAsyncResult.result() != null) {

                                JsonObject reponse = messageAsyncResult.result().body();
                                log.add("proxy_bank ws's response", reponse.toString());
                                if (reponse.getInteger(BankHelperVerticle.ERROR) != 0) {
                                    //Tra loi time out, khong ket noi duoc voi connector
                                    log.add("errorCode", "time out");
                                    jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                    jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                                    formSourceObj.Result = false;
                                    callback.handle(formSourceObj);
                                    log.writeLog();
                                    return;
                                }

                                BankResponse bankResponse = new BankResponse(reponse.getObject(BankHelperVerticle.DATA));
                                log.add("result", "Co ket qua tra ve tu connector");
                                log.add("bankResponse", bankResponse);

                                if (bankResponse != null && bankResponse.getResultCode() == 0) {
                                    //Tra ket qua thanh cong ve cho client
                                    log.add("resultCode", bankResponse.getResultCode());
                                    log.add("transId", bankResponse.getRequest().getCoreTransId());
                                    jsonRep.putNumber(StringConstUtil.STATUS, 4);
                                    jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
                                    jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
                                    jsonRep.putNumber(StringConstUtil.TRANDB_TRAN_ID, bankResponse.getRequest().getCoreTransId());
                                    formSourceObj.Result = true;
                                    formSourceObj.ServiceSource = jsonRep.toString();
                                    //Nap tien thanh cong, kiem tra tra khuyen mai
                                    promotionProcess.getUserInfoToCheckPromoProgram("",phoneNumber, null, bankResponse.getRequest().getCoreTransId(), MomoProto.TranHisV1.TranType.BANK_IN_VALUE, blObj.amount, StringConstUtil.WomanNationalField.PROGRAM, data, new JsonObject());
                                } else if (bankResponse != null && bankResponse.getResultCode() != 0) {
                                    int errorCode = bankResponse == null ? 1000 : bankResponse.getResultCode();
                                    String desc = bankResponse == null ? "Nạp tiền không thành công" : bankResponse.getDescription();
                                    log.add("errorCode", errorCode);
                                    log.add("desc", desc);
                                    jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                    jsonRep.putNumber(StringConstUtil.ERROR, bankResponse.getResultCode());
                                    jsonRep.putString(StringConstUtil.DESCRIPTION, bankResponse.getDescription());
                                    formSourceObj.Result = false;
                                } else {
                                    log.add("errorCode", "bank response is null");
                                    jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                    jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                                    jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                                    formSourceObj.Result = false;
                                }
                                callback.handle(formSourceObj);
                                log.writeLog();
                                return;
                            } else {
                                //Tra loi time out, khong ket noi duoc voi connector
                                log.add("errorCode", "time out");
                                jsonRep.putNumber(StringConstUtil.STATUS, 5);
                                jsonRep.putNumber(StringConstUtil.ERROR, 1000);
                                jsonRep.putString(StringConstUtil.DESCRIPTION, "Nạp tiền không thành công");
                                formSourceObj.Result = false;
                                callback.handle(formSourceObj);
                                log.writeLog();
                                return;
                            }
                        }
                    });
                    return;
                }
            });
        }
    }





    public static class BankNetObj {

        public String card_holder_year;
        public String card_holder_month;
        public String card_holder_number;
        public String card_holder_name;
        public String bankId;
        public long amount;
        public String merchant_trans_id;
        public String trans_id;
        public String otp;
        public int tranType;

    }

    public static class BankLinkedObj {
        public String from_bank;
        public String bank_code;
        public long amount;
    }
}
