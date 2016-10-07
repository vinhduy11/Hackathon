package com.mservice.momo.vertx.processor.promotion;

import com.mservice.momo.data.Card;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.promotion.CashBackPromotionObj;
import com.mservice.momo.data.promotion.ErrorPromotionTrackingDb;
import com.mservice.momo.data.promotion.VisaMappingPromotionDb;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.vertx.processor.PromotionProcess;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by concu on 4/25/16.
 */
public class StandardCharterBankPromotionProcess extends PromotionProcess {

    VisaMappingPromotionDb visaMappingPromotionDb;
    ErrorPromotionTrackingDb errorPromotionTrackingDb;
    public StandardCharterBankPromotionProcess(Vertx vertx, Logger logger, JsonObject glbCfg) {
        super(vertx, logger, glbCfg);
        visaMappingPromotionDb = new VisaMappingPromotionDb(vertx, logger);
        errorPromotionTrackingDb = new ErrorPromotionTrackingDb(vertx, logger);
    }
//    public StandardCharterBankPromotionProcess() {
//        super();
//        visaMappingPromotionDb = new VisaMappingPromotionDb(vertx, logger);
//    }

    public void checkAndSaveSCBCardInfo(final String phoneNumber, final String cardInfo, final String bankAcc, final String imei, final JsonObject joExtra,final Common.BuildLog log)
    {
        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "executeScbPromotionProcess");
        phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj phoneObj) {
                if(phoneObj == null || phoneObj.isAgent)
                {
                    log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "So dien thoai tham gia la DGD " + phoneNumber);
                    log.writeLog();
                    return;
                }
                checkPromotionProgram(phoneNumber, cardInfo, bankAcc, imei, log, joExtra);
            }
        });
//        card.findAllActivedCardVisa(DataUtil.strToInt(phoneNumber), new Handler<ArrayList<Card.Obj>>() {
//            @Override
//            public void handle(ArrayList<Card.Obj> listCardVisas) {
//                if (listCardVisas.size() == 0) {
//                    log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co the visa nao");
//                    log.writeLog();
//                    return;
//                }
//                log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Co thong tin the visa " + phoneNumber);
//                checkPromotionProgram(phoneNumber, cardInfo, bankAcc, imei, log, joExtra, listCardVisas);
//            }
//        });
        return;
    }
    public void executeScbPromotionCashBackProcess(final String phoneNumber, final Common.BuildLog log, final JsonObject joExtra) {
        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "executeScbPromotionCashBackProcess");

        card.findAllActivedCardVisa(DataUtil.strToInt(phoneNumber), new Handler<ArrayList<Card.Obj>>() {
            @Override
            public void handle(ArrayList<Card.Obj> listCardVisa) {
                  if(listCardVisa.size() == 0)
                  {
                      log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co the visa dang lien ket");
                      log.writeLog();
                      return;
                  }
                  log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "executeCashBackScbPromotion");
                  executeCashBackScbPromotion(phoneNumber, log, joExtra, listCardVisa);
                  return;
            }
        });


        return;
    }

    private void executeCashBackScbPromotion(final String phoneNumber, final Common.BuildLog log, final JsonObject joExtra, final ArrayList<Card.Obj> listCardVisa) {
        visaMappingPromotionDb.findOne(StringConstUtil.StandardCharterBankPromotion.PROGRAM, phoneNumber, new Handler<VisaMappingPromotionDb.Obj>() {
            @Override
            public void handle(final VisaMappingPromotionDb.Obj visaObj) {
                if(visaObj == null)
                {
                    log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong Co thong tin the visa " + phoneNumber);
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.StandardCharterBankPromotion.PROGRAM, 1000, "Khong ghi nhan so dien thoai nay tham gia chuong trinh");
                    return;
                }
                else if(isStoreApp)
                {
                    log.add("func " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "DGD khong duoc tham gia chuong trinh khuyen mai nay");
                    log.writeLog();
                    return;
                }
                boolean isMapping = false;
                for(Card.Obj cardObj : listCardVisa)
                {
                    if(cardObj.cardCheckSum.equalsIgnoreCase(visaObj.cardInfo))
                    {
                        isMapping = true;
                        break;
                    }
                }
                if(!isMapping)
                {
                    log.add("func " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co thong tin the duoc tham gia " + visaObj.cardInfo);
                    log.writeLog();
                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.StandardCharterBankPromotion.PROGRAM, 1000, "Hien tai user khong map the scb nen khong duoc tra thuong");
                    return;
                }
                Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
                promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
                Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject json) {
                        JsonArray array = json.getArray("array", null);
                        long promo_start_date = 0;

                        if (array != null && array.size() > 0) {
                            PromotionDb.Obj promoObj = null;
                            for (Object o : array) {
                                promoObj = new PromotionDb.Obj((JsonObject) o);
                                if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.StandardCharterBankPromotion.PROGRAM)) {
                                    promo_start_date = promoObj.DATE_FROM;
                                    break;
                                }
                            }
                            final PromotionDb.Obj scbPromotionProgram = promo_start_date > 0 ? promoObj : null;
                            if (scbPromotionProgram == null) {
                                log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co thong tin chuong trinh scb");
                                log.writeLog();
                                return;
                            } else if ("".equalsIgnoreCase(scbPromotionProgram.ADJUST_ACCOUNT)) {
                                log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co tai khoan tra thuong khuyen mai scb promotion");
                                log.writeLog();
                                return;
                            } else if (visaObj.time + 1000L * 60 * 60 * 24 * scbPromotionProgram.DURATION < System.currentTimeMillis()) {
                                log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Het thoi gian nhan cash back scb promotion");
                                log.writeLog();
                                return;
                            }
                            final long tranId = joExtra.getLong(StringConstUtil.TRANDB_TRAN_ID, 0);
                            final long amount = joExtra.getLong(StringConstUtil.AMOUNT, 0);
                            final String serviceId = joExtra.getString(StringConstUtil.SERVICE_ID, "");
                            final int tranType = joExtra.getInteger(StringConstUtil.TRANDB_TRAN_TYPE, 0);
                            joExtra.putObject(StringConstUtil.PROMOTION, scbPromotionProgram.toJsonObject());
                            //Kiem tra the da duoc su dung chua
                            JsonObject joFilter = new JsonObject();
                            joFilter.putString(colName.VisaMappingPromotionCol.CARD_INFO, visaObj.cardInfo);
                            visaMappingPromotionDb.searchWithFilter(StringConstUtil.StandardCharterBankPromotion.PROGRAM, joFilter, new Handler<ArrayList<VisaMappingPromotionDb.Obj>>() {
                                @Override
                                public void handle(ArrayList<VisaMappingPromotionDb.Obj> listVisaCard) {
                                    boolean isMap = false;
                                    for(VisaMappingPromotionDb.Obj visaMappingObj : listVisaCard)
                                    {
                                        if(visaMappingObj.joExtra.containsField(StringConstUtil.ERROR) && !visaObj.phone_number.equalsIgnoreCase(visaMappingObj.phone_number))
                                        {
                                            isMap = true;
                                            break;
                                        }
                                    }
                                    if(isMap)
                                    {
                                        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Tai khoan visa nay da duoc tra thuong " + visaObj.cardInfo);
                                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.StandardCharterBankPromotion.PROGRAM, 1000, "Tai khoang visa nay da duoc tra thuong");
                                        log.writeLog();
                                        return;
                                    }
                                    CashBackPromotionObj.requestCashBackPromotion(vertx, StringConstUtil.StandardCharterBankPromotion.PROGRAM, phoneNumber
                                            , tranId, scbPromotionProgram.PER_TRAN_VALUE, serviceId, amount,
                                            visaObj.bankAcc, visaObj.device_imei, visaObj.cardInfo,
                                            tranType, joExtra, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject joResponse) {
                                                    final int err = joResponse.getInteger(StringConstUtil.ERROR, -1);
                                                    JsonObject joUpdate = new JsonObject();
                                                    joUpdate.putObject(colName.VisaMappingPromotionCol.EXTRA, joResponse);
                                                    visaMappingPromotionDb.updatePartial(StringConstUtil.StandardCharterBankPromotion.PROGRAM, phoneNumber, joUpdate, new Handler<Boolean>() {
                                                        @Override
                                                        public void handle(Boolean event) {
                                                            if(err != 0)
                                                            {
                                                                Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.StandardCharterBankPromotion.PROGRAM, err, "Loi Core");
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                    );
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void checkPromotionProgram(final String phoneNumber, final String cardInfo, final String bankAcc, final String imei, final Common.BuildLog log,final JsonObject joExtra) {

        log.add("func " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "checkPromotionProgram");
        if(isStoreApp)
        {
            log.add("func " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "DGD khong duoc tham gia chuong trinh khuyen mai nay");
            log.writeLog();
            return;
        }
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                long promo_start_date = 0;
                long promo_end_date = 0;
                long currentTime = System.currentTimeMillis();
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.StandardCharterBankPromotion.PROGRAM)) {
                            promo_start_date = promoObj.DATE_FROM;
                            promo_end_date = promoObj.DATE_TO;
                            break;
                        }
                    }
                    final PromotionDb.Obj scbPromotionProgram = promo_start_date > 0 ? promoObj : null;
                    if (scbPromotionProgram == null) {
                        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co thong tin chuong trinh scb");
                        log.writeLog();
                        return;
                    } else if (currentTime < promo_start_date || currentTime > promo_end_date) {
                        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Ngoai thoi gian tham gia khuyen mai scb promotion");
                        log.writeLog();
                        return;
                    } else if ("".equalsIgnoreCase(scbPromotionProgram.ADJUST_ACCOUNT)) {
                        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co tai khoan tra thuong khuyen mai scb promotion");
                        log.writeLog();
                        return;
                    } else if ("".equalsIgnoreCase(scbPromotionProgram.ADJUST_PIN)) {
                        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Khong co thong tin so the tham gia tra thuong khuyen mai sbc promotion");
                        log.writeLog();
                        return;
                    }
                    List<String> scbListPin = getScbPin(scbPromotionProgram.ADJUST_PIN);
                    log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM +phoneNumber, "Check thong tin the visa");
                    log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, scbListPin.toString());

                    String pinHeadCard = "";
                    boolean isOk = false;
                    if (bankAcc.length() > 6) {
                        pinHeadCard = bankAcc.substring(0, 6);
                    }
                    if (!"".equalsIgnoreCase(pinHeadCard)) {
                        for (String pin : scbListPin) {
                            if (pinHeadCard.equalsIgnoreCase(pin.toString().trim())) {
                                isOk = true;
                                break;
                            }
                        }
                    }
                    if("".equalsIgnoreCase(pinHeadCard) || !isOk)
                    {
                        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Dau pin khong thuoc LIST, khong duoc tham gia chuong trinh " + isOk + " " + pinHeadCard);
                        Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.StandardCharterBankPromotion.PROGRAM, 1000, "The visa nay khong duoc tham gia chuong trinh khuyen mai");
                        log.writeLog();
                        return;
                    }
                    log.add("co thong tin scb bank " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, bankAcc);
                    log.add("Kiem tra vi voi so dien thoai co giong nhau ko " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, bankAcc);
                    JsonObject joFilter = new JsonObject();
                    JsonObject joPhoneNumber = new JsonObject().putString(colName.VisaMappingPromotionCol.PHONE_NUMBER, phoneNumber);
                    JsonObject joCardInfo = new JsonObject().putString(colName.VisaMappingPromotionCol.CARD_INFO, cardInfo);
                    JsonArray joOr = new JsonArray();
                    joOr.add(joPhoneNumber);
                    joOr.add(joCardInfo);

                    joFilter.putArray(MongoKeyWords.OR, joOr);
                    visaMappingPromotionDb.searchWithFilter(StringConstUtil.StandardCharterBankPromotion.PROGRAM, joFilter, new Handler<ArrayList<VisaMappingPromotionDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<VisaMappingPromotionDb.Obj> listItems) {
                            boolean isMapped = false;
                            if(listItems.size() > 0)
                            {
                                for(VisaMappingPromotionDb.Obj obj : listItems)
                                {
                                    if(obj.joExtra.containsField(StringConstUtil.ERROR))
                                    {
                                        isMapped = true;
                                        break;
                                    }
                                }
                                if(isMapped)
                                {
                                    log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Vi/the da tham gia chuong trinh, khong cho tham gia nua");
                                    Misc.saveErrorPromotionInfo(errorPromotionTrackingDb, phoneNumber, StringConstUtil.StandardCharterBankPromotion.PROGRAM, 1000, "Vi/The visa da tham gia chuong trinh, khong duoc tham gia tiep");
                                    log.writeLog();
                                    return;
                                }
                                updateCustomerInfo(log, phoneNumber, bankAcc, cardInfo, imei);
                                return;
                            }
                            updateCustomerInfo(log, phoneNumber, bankAcc, cardInfo, imei);
                        }
                    } );
                }
            }
        });
    }

    private void updateCustomerInfo(final Common.BuildLog log, final String phoneNumber, String bankAcc, String cardInfo, String imei) {
        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "co thong tin the / vi");
        VisaMappingPromotionDb.Obj visaObj = new VisaMappingPromotionDb.Obj();
        visaObj.bankAcc = bankAcc;
        visaObj.cardInfo = cardInfo;
        visaObj.device_imei = imei;
        visaObj.phone_number = phoneNumber;
        visaObj.program = StringConstUtil.StandardCharterBankPromotion.PROGRAM;
        visaObj.time = System.currentTimeMillis();
        visaMappingPromotionDb.updateWithInsert(StringConstUtil.StandardCharterBankPromotion.PROGRAM, visaObj.phone_number, visaObj.toJson(), new Handler<Boolean>() {
                    @Override
                    public void handle(Boolean event) {
                   log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Da cap nhat thong tin SCB PROMO thanh cong");
                   log.writeLog();
                    }
                });
//                visaMappingPromotionDb.updateWithInsert(StringConstUtil.StandardCharterBankPromotion.PROGRAM, visaObj.phone_number, visaObj.toJson(), new Handler<Integer>() {
//                    @Override
//                    public void handle(Integer event) {
//                        log.add("desc " + StringConstUtil.StandardCharterBankPromotion.PROGRAM + phoneNumber, "Da cap nhat thong tin SCB PROMO thanh cong");
//                        log.writeLog();
//                    }
//                });
    }

    private List<String> getScbPin(String pin) {
        String[] pinList = pin.split(";");
        List<String> scbPinList = new ArrayList<>();
        for (String p : pinList) {
            scbPinList.add(p.toString().trim());
        }

        return scbPinList;
    }
}
