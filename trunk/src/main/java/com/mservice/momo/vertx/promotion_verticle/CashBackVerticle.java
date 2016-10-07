package com.mservice.momo.vertx.promotion_verticle;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.CashBackDb;
import com.mservice.momo.data.promotion.CashBackPromotionObj;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;

/**
 * Created by concu on 4/23/16.
 */
public class CashBackVerticle extends Verticle {

    private Logger logger;
    private JsonObject glbCfg;
    private GiftManager giftManager;
    private TransDb tranDb;

    private boolean isStoreApp;

    CashBackDb cashBackDb;
    ErrorPromotionTrackingDb errorPromotionTrackingDb;
    @Override
    public void start() {

        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());

        this.giftManager = new GiftManager(vertx, logger, glbCfg);

        isStoreApp = glbCfg.getBoolean(StringConstUtil.IS_STORE_APP, false);
        // jsonZaloPromo = glbCfg.getObject(StringConstUtil.ZaloPromo.JSON_OBJECT, new JsonObject());

        cashBackDb = new CashBackDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject reqJson = message.body();
                final CashBackPromotionObj cashBackPromotionObj = new CashBackPromotionObj(reqJson);
                final Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber(cashBackPromotionObj.phoneNumber);
                final String cashBackProgramName = cashBackPromotionObj.program;
                log.add("phoneNumber "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.phoneNumber);
                log.add("amount "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.amount);
                log.add("bankAcc "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.bankAcc);
                log.add("cardInfo "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.cardInfo);
                log.add("deviceImei "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.deviceImei);
                log.add("joExtra "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.joExtra.toString());
                log.add("program "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.program);
                log.add("rate "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.rate);
                log.add("serviceId "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.serviceId);
                log.add("tranId "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.tranId);
                log.add("tranType "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, cashBackPromotionObj.tranType);
                final PromotionDb.Obj promotionCashBack = new PromotionDb.Obj(cashBackPromotionObj.joExtra.getObject(StringConstUtil.PROMOTION, new JsonObject()));
                log.add("promotionCashBack "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, promotionCashBack.toJsonObject());
                JsonObject joFilter = new JsonObject();
                joFilter.putString(colName.CashBackCol.PHONE_NUMBER, cashBackPromotionObj.phoneNumber);
                cashBackDb.searchWithFilter(cashBackProgramName, joFilter, new Handler<ArrayList<CashBackDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<CashBackDb.Obj> listCashBack) {
                        final long cashbackAmount = getBonusAmountForUser(listCashBack, cashBackPromotionObj, promotionCashBack);
                        if(cashbackAmount <= 0)
                        {
                            log.add("cashbackAmount "  + cashBackProgramName + cashBackPromotionObj.phoneNumber, "Da cashback day du, khong tra nua.");
                            Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, cashBackPromotionObj.phoneNumber, StringConstUtil.StandardCharterBankPromotion.PROGRAM, 1000, "Da cashback lai du tien roi, khong cashback nua");
                            log.writeLog();
                            return;
                        }
                        cashBackForUser(promotionCashBack.ADJUST_ACCOUNT, cashbackAmount, cashBackPromotionObj.phoneNumber, log, cashBackProgramName, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject joResponse) {
                                final int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                                final long tranId = joResponse.getLong(StringConstUtil.TRANDB_TRAN_ID, System.currentTimeMillis());
                                final CashBackDb.Obj cashBackObj = new CashBackDb.Obj();
                                cashBackObj.amountBonus = cashbackAmount;
                                cashBackObj.amount = cashBackPromotionObj.amount;
                                cashBackObj.error_code = err;
                                cashBackObj.cardInfo = cashBackPromotionObj.cardInfo;
                                cashBackObj.bankAcc = cashBackPromotionObj.bankAcc;
                                cashBackObj.device_imei = cashBackPromotionObj.deviceImei;
                                cashBackObj.phone_number = cashBackPromotionObj.phoneNumber;
                                cashBackObj.program = cashBackProgramName;
                                cashBackObj.rate = cashBackPromotionObj.rate;
                                cashBackObj.serviceId = cashBackPromotionObj.serviceId;
                                cashBackObj.tranId = cashBackPromotionObj.tranId;
                                cashBackObj.tranIdBonus = tranId;
                                cashBackObj.tranType = cashBackPromotionObj.tranType;
                                cashBackObj.time = System.currentTimeMillis();
                                cashBackDb.insert(cashBackProgramName, cashBackObj, new Handler<Integer>() {
                                    @Override
                                    public void handle(Integer event) {
                                        JsonObject joReply = new JsonObject();
                                        if(err == 0)
                                        {
                                            String body = promotionCashBack.NOTI_COMMENT.replace("%money", Misc.formatAmount(cashbackAmount)+ "");
                                            //Send noti
                                            fireMoneyNotiAndSendTranHist(cashBackPromotionObj.phoneNumber, cashbackAmount, tranId, log, promotionCashBack.NOTI_CAPTION, body
                                            ,body, promotionCashBack.INTRO_DATA, 1);

                                        }
                                        joReply.putNumber(StringConstUtil.ERROR, err);
                                        message.reply(joReply);

                                    }
                                } );
                            }
                        });
                    }
                });

            }
        };

        vertx.eventBus().registerLocalHandler(AppConstant.CASH_BACK_PROMOTION_BUS_ADDRESS, myHandler);
    }

    private long getBonusAmountForUser(ArrayList<CashBackDb.Obj> listCashBacks, CashBackPromotionObj cashBackPromotionObj, PromotionDb.Obj promotionCashBack)
    {
        long bonusAmount = 0;

        long totalBonus = 0;
        for(CashBackDb.Obj cashbackObj : listCashBacks)
        {
            totalBonus = totalBonus + cashbackObj.amountBonus;
        }
        long bonusPerTran = cashBackPromotionObj.rate * cashBackPromotionObj.amount / 100;
        bonusAmount = promotionCashBack.TRAN_MIN_VALUE - totalBonus < bonusPerTran ? promotionCashBack.TRAN_MIN_VALUE - totalBonus : bonusPerTran;

        return bonusAmount;
    }

    private void cashBackForUser(final String agent, final long value_of_money, final String phoneNumber, final Common.BuildLog log, final String program,  final Handler<JsonObject> callback)
    {
        final JsonObject jsonReply = new JsonObject();
        log.setPhoneNumber(phoneNumber);
        log.add("desc" + program + phoneNumber, "getFeeFromStore");
        log.add("phone " + program + phoneNumber, phoneNumber);
        log.add("agent " + program + phoneNumber, agent);
        ArrayList<Misc.KeyValue> keyValueArrayList = new ArrayList<Misc.KeyValue>();
        Misc.KeyValue keyValue = new Misc.KeyValue();
        keyValue.Key = "program";
        keyValue.Value = program;
        Misc.adjustment(vertx, agent, phoneNumber, value_of_money,
                Core.WalletType.MOMO_VALUE, keyValueArrayList, log, new Handler<Common.SoapObjReply>() {
                    @Override
                    public void handle(Common.SoapObjReply soapObjReply) {
                        log.add("desc" + program + phoneNumber, "core tra ket qua");
                        if (soapObjReply != null && soapObjReply.error != 0) {
                            log.add("core tra loi " + program + phoneNumber, soapObjReply.error);
                            log.add("status " + program + phoneNumber, soapObjReply.status);
                            log.add("tid " + program + phoneNumber, soapObjReply.tranId);
                            log.add("desc " + program + phoneNumber, "core tra loi");
                            log.writeLog();
                            JsonObject jsonReply = new JsonObject();
                            jsonReply.putNumber(StringConstUtil.ERROR, soapObjReply.error);
                            jsonReply.putString(StringConstUtil.DESCRIPTION,  "Khong tang duoc tien khach hang, core tra loi");
                            jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                            callback.handle(jsonReply);
                            return;
                        }
                        //Yeu cau xong tien mat
                        //Luu qua trong Db va ban noti thong bao
//                        giveMoney(message, zaloTetPromotionObj, log, soapObjReply, jsonReply);
                        jsonReply.putNumber(StringConstUtil.ERROR, 0);
                        jsonReply.putNumber(StringConstUtil.TRANDB_TRAN_ID, soapObjReply.tranId);
                        callback.handle(jsonReply);
                    }
                });
    }

    private void fireMoneyNotiAndSendTranHist(final String phoneNumber, final long amount
            , final long tranId
            , final Common.BuildLog log
            , final String titleNoti
            , final String bodyNoti
            , final String bodyTrans
            , final String partnerName
            , final int io)
    {
        log.add("desc", "fireMoneyNotiAndSendTranHist");

        JsonObject joTranHis = new JsonObject();
        joTranHis.putNumber(colName.TranDBCols.TRAN_TYPE, MomoProto.TranHisV1.TranType.FEE_VALUE);
        joTranHis.putString(colName.TranDBCols.COMMENT, bodyTrans);
        joTranHis.putNumber(colName.TranDBCols.TRAN_ID, tranId);
        joTranHis.putNumber(colName.TranDBCols.AMOUNT, amount);
        joTranHis.putNumber(colName.TranDBCols.STATUS, 4);
        joTranHis.putNumber(colName.TranDBCols.OWNER_NUMBER, DataUtil.strToInt(phoneNumber));
        joTranHis.putString(colName.TranDBCols.PARTNER_NAME, partnerName);
        joTranHis.putString(colName.TranDBCols.BILL_ID, "");
        joTranHis.putString(StringConstUtil.HTML, "");
        joTranHis.putNumber(colName.TranDBCols.IO, io);
        Misc.sendingStandardTransHisFromJson(vertx, tranDb, joTranHis, new JsonObject());

        JsonObject joNoti = new JsonObject();
        joNoti.putString(StringConstUtil.StandardNoti.CAPTION, titleNoti);
        joNoti.putString(StringConstUtil.StandardNoti.BODY, bodyNoti);
        joNoti.putString(StringConstUtil.StandardNoti.RECEIVER_NUMBER, phoneNumber);
        joNoti.putNumber(StringConstUtil.StandardNoti.TRAN_ID, tranId);
        Misc.sendStandardNoti(vertx, joNoti);

    }

}
