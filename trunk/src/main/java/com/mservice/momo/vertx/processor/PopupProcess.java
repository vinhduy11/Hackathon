package com.mservice.momo.vertx.processor;

import com.mservice.common.Popup;
import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.popup.EmailPopupDb;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.Notification;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;

/**
 * Created by concu on 4/21/16.
 */
public class PopupProcess {

    private Vertx vertx;
    private Logger logger;
    private JsonObject joGlobalConfig;
    private EmailPopupDb emailPopupDb;
    boolean isStore;
    public PopupProcess(Vertx vertx, Logger logger, JsonObject glbConfig)
    {
        this.vertx = vertx;
        this.logger = logger;
        this.joGlobalConfig = glbConfig;
        this.emailPopupDb = new EmailPopupDb(vertx, logger);
        isStore = glbConfig.getBoolean(StringConstUtil.IS_STORE_APP, false);
    }

    public void checkPopupInfomation(final MomoMessage msg,final Common.BuildLog log, SockData sockData) {
        //Neu co qua tang thi ban popup khuyen mai.
        log.add("method", "checkPopupInfomation");
        log.add("desc", "Kiem tra popup email");

        Queue<String> popupQueued = new ArrayDeque<>();
        popupQueued.add(StringConstUtil.POPUP_EMAIL);

        getPopupFromQueue(popupQueued, msg, log, sockData);
    }

    private void getPopupFromQueue(Queue<String> popupQueue, final MomoMessage msg,final Common.BuildLog log, SockData sockData)
    {
        if(popupQueue.size() == 0)
        {
            log.add("desc getPopupFromQueue", "Khong co du lieu queue" );
            log.writeLog();
            return;
        }
        String popupType = popupQueue.poll();

        if(StringConstUtil.POPUP_EMAIL.equalsIgnoreCase(popupType) && !isStore)
        {
            if("IOS".equalsIgnoreCase(sockData.os) && sockData.appCode > 1922 && !isStore || "ANDROID".equalsIgnoreCase(sockData.os) && sockData.appCode > 70 && !isStore)
            executeEmailPopup(popupQueue, msg, log, sockData);
        }
    }

    private void executeEmailPopup(final Queue<String> popupQueue, final MomoMessage msg,final Common.BuildLog log, final SockData sockData)
    {
        log.add("desc", "executeEmailPopup");
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_LIST;
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject json) {
                JsonArray array = json.getArray("array", null);
                boolean enableGiftGiving = false;
                long promo_start_date = 0;
                long promo_end_date = 0;
                long currentTime = System.currentTimeMillis();
                String agent = "";
                long gift_amount = 0;
                if (array != null && array.size() > 0) {
                    PromotionDb.Obj promoObj = null;
                    JsonObject jsonTime = new JsonObject();
                    for (Object o : array) {
                        promoObj = new PromotionDb.Obj((JsonObject) o);
                        if (promoObj.NAME.equalsIgnoreCase(StringConstUtil.POPUP_EMAIL)) {
                            promo_start_date = promoObj.DATE_FROM;
                            promo_end_date = promoObj.DATE_TO;
                            agent = promoObj.ADJUST_ACCOUNT;
                            break;
                        }
                    }
                    final PromotionDb.Obj popupEmailProgram = promo_start_date > 0 || !"".equalsIgnoreCase(agent) ? promoObj : null;
                    if(popupEmailProgram == null)
                    {
                        log.add("desc", "DONT SEND EMAIL POPUP");
                        getPopupFromQueue(popupQueue, msg, log, sockData);
                        return;
                    }
                    else if(popupEmailProgram.ENABLE_PHASE2) {
                        log.add("desc", "send all user");
                        log.add("desc", "send popup");
                        emailPopupDb.findOne("0" + msg.cmdPhone, new Handler<EmailPopupDb.Obj>() {
                            @Override
                            public void handle(EmailPopupDb.Obj emailPopupObj) {
                                if(emailPopupObj != null && !emailPopupObj.enable)
                                {
                                    log.add("desc ban all popup ", "Da send popup cho so dien thoai nay roi " + msg.cmdPhone);
                                    getPopupFromQueue(popupQueue, msg, log, sockData);
                                    return;
                                }
                                log.add("desc", "send popup email cho so dien thoai " + msg.cmdPhone);
                                Popup emailPopup = createEmailPopup(popupEmailProgram);
                                sendPopUpInformation(emailPopup, msg, true, System.currentTimeMillis(), StringConstUtil.POPUP_EMAIL, DataUtil.POPUP_TYPE.EMAIL_POPUP);
                                return;
                            }
                        });
                        return;
                    }
                    else
                    {
                        log.add("desc", "kiem tra xem co send popup email cho user nay ko");
                        emailPopupDb.findOne("0" + msg.cmdPhone, new Handler<EmailPopupDb.Obj>() {
                            @Override
                            public void handle(EmailPopupDb.Obj emailPopupObj) {
                                if(emailPopupObj == null)
                                {
                                    log.add("desc", "Khong send popup email cho so dien thoai " + msg.cmdPhone);
                                    getPopupFromQueue(popupQueue, msg, log, sockData);
                                    return;
                                }
                                else if(!emailPopupObj.enable)
                                {
                                    log.add("desc", "sdt " + msg.cmdPhone + " da cap nhat email roi hoac so dien thoai nay khong can send popup nua");
                                    getPopupFromQueue(popupQueue, msg, log, sockData);
                                    return;
                                }
                                log.add("desc", "send popup email cho so dien thoai " + msg.cmdPhone);
                                Popup emailPopup = createEmailPopup(popupEmailProgram);
                                sendPopUpInformation(emailPopup, msg, true, System.currentTimeMillis(), StringConstUtil.POPUP_EMAIL, DataUtil.POPUP_TYPE.EMAIL_POPUP);
                                return;
                            }
                        });
                    }
                }
            }
        });
    }

    private Popup createEmailPopup(PromotionDb.Obj popupEmailProgram)
    {
        Popup emailPopup = new Popup(Popup.Type.CONFIRM);
        emailPopup.setHeader(popupEmailProgram.NOTI_CAPTION);
        emailPopup.setContent(popupEmailProgram.NOTI_COMMENT);
        emailPopup.setEnabledClose(false);
        emailPopup.setOkButtonLabel(popupEmailProgram.INTRO_DATA);
        emailPopup.setCancelButtonLabel(popupEmailProgram.INTRO_SMS);

        return emailPopup;
    }


    //END 0000000050
    private void sendPopUpInformation(Popup popup, MomoMessage msg, boolean isConfirm, long tid, String partner,
                                      DataUtil.POPUP_TYPE popup_type) {
        JsonObject jsonExtra = new JsonObject();
        int type = DataUtil.getType(popup_type);

        jsonExtra.putNumber(StringConstUtil.TYPE, type);
        String button_title_1 = popup != null ? popup.getOkButtonLabel() : StringConstUtil.CONFIRM_BUTTON_TITLE;
        jsonExtra.putString(StringConstUtil.BUTTON_TITLE_1, button_title_1);
        if (isConfirm) {
            String button_title_2 = popup != null ? popup.getCancelButtonLabel() : StringConstUtil.CANCEL_BUTTON_TITLE;
            jsonExtra.putString(StringConstUtil.BUTTON_TITLE_2, button_title_2);
        }
        boolean button_title_x = popup != null ? popup.isEnabledClose() : false;
        jsonExtra.putBoolean(StringConstUtil.BUTTON_TITLE_X, button_title_x);
        jsonExtra.putString(StringConstUtil.WALLET_MAPPING_PARTNER, partner);
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = 2;
        notification.type = MomoProto.NotificationType.POPUP_INFORMATION_VALUE;
        notification.caption = popup != null ? popup.getHeader() : "";
        notification.body = popup != null ? popup.getContent() : "";
        notification.tranId = 0L;
        notification.cmdId = msg.cmdIndex;
        notification.time = new Date().getTime();
        notification.receiverNumber = msg.cmdPhone;
        notification.extra = jsonExtra.toString();

        if(popup != null)
        {
            Misc.sendNoti(vertx, notification);
        }
        else{
            logger.info("popup bankinfo is null");
        }
    }




}
