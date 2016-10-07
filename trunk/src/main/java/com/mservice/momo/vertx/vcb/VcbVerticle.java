package com.mservice.momo.vertx.vcb;

import com.mservice.momo.data.*;
import com.mservice.momo.data.gift.GiftDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.gift.models.Gift;
import com.mservice.momo.vertx.gift.models.GiftType;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by admin on 2/13/14.
 */
public class VcbVerticle extends Verticle {

    private Logger logger;
    private JsonObject glbCfg;
    private EventBus eb;
    private VcbRecords vcbRecords;
    private GiftManager giftManager;
    private GiftDb giftDb;
    private TransDb tranDb;
    private PhonesDb phonesDb;
    private VcbCmndRecs vcbCmndRecs;
    private PgGlxDb glxDb;
    //thuc hien khuyen mai PG galaxy
    private Handler<Message<JsonObject>> doPromotionForPG = new Handler<Message<JsonObject>>() {
        @Override
        public void handle(final Message<JsonObject> message) {

            final ReqObj reqPromoForPG = new ReqObj(message.body());
            final Promo.PromoReqObj vcbReqPromoRec = new Promo.PromoReqObj();

            vcbReqPromoRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            vcbReqPromoRec.PROMO_NAME = "promogirlgalaxy";

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + reqPromoForPG.creator);
            log.add("amount", reqPromoForPG.amount);
            log.add("promotion name", vcbReqPromoRec.PROMO_NAME);
            log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(reqPromoForPG.tranType) == null ? "0" : MomoProto.TranHisV1.TranType.valueOf(reqPromoForPG.tranType));
            log.add("tranid", reqPromoForPG.tranId);

            Misc.requestPromoRecord(vertx, vcbReqPromoRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    final PromotionDb.Obj promoRecord = new PromotionDb.Obj(jsonObject);

                    //todo khong co chuong trinh khuyen mai vcbpromo
                    if (promoRecord == null || promoRecord.DATE_TO == 0 || promoRecord.DATE_FROM == 0) {
                        log.add("*******", "pggalaxy khong co chuong trinh khuyen mai");
                        log.writeLog();
                        return;
                    }

                    //todo kiem tra con thoi gian khuyen mai khong
                    long curTime = System.currentTimeMillis();
                    if ((curTime < promoRecord.DATE_FROM) || (curTime > promoRecord.DATE_TO)) {
                        log.add("from date", Misc.dateVNFormatWithTime(promoRecord.DATE_FROM));
                        log.add("to date", Misc.dateVNFormatWithTime(promoRecord.DATE_TO));
                        log.add("********", "pggalaxy het thoi gian khuyen mai");
                        log.writeLog();
                        return;
                    }

                    glxDb.find(reqPromoForPG.partner
                            , String.valueOf(reqPromoForPG.creator)
                            , new Handler<ArrayList<PgGlxDb.Obj>>() {
                        @Override
                        public void handle(ArrayList<PgGlxDb.Obj> list) {

                            //het so lan khuyen mai
                            if (list.size() >= promoRecord.MAX_TIMES) {
                                log.add("pggalaxy", "Het so lan khuyen mai " + list.size() + ">=" + promoRecord.MAX_TIMES);
                                log.writeLog();
                                return;
                            }

                            boolean paied = false;
                            for (int i = 0; i < list.size(); i++) {
                                if (list.get(i).value == reqPromoForPG.promoValue) {
                                    paied = true;
                                    break;
                                }
                            }

                            if (paied) {
                                log.add("pggalaxy", "da tra thuong cho giao dich loai nay roi tran name " + MomoProto.TranHisV1.TranType.valueOf(reqPromoForPG.tranType).name());
                                return;
                            }

                            PgGlxDb.Obj obj = new PgGlxDb.Obj();
                            obj.pgcode = reqPromoForPG.partner;
                            obj.number = String.valueOf(reqPromoForPG.creator);
                            obj.value = reqPromoForPG.promoValue;
                            obj.tranname = MomoProto.TranHisV1.TranType.valueOf(reqPromoForPG.tranType).name();
                            obj.serviceid = reqPromoForPG.serviceId;

                            glxDb.save(obj, new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {

                                }
                            });
                        }
                    });
                }
            });
        }
    };
    private AgentsDb agentsDb;

    //give gift for viettinbank promotion.end
    //give gift for viettinbank promotion.start
    private Handler<Message<JsonObject>> doPromotionViettinbank = new Handler<Message<JsonObject>>() {
        @Override
        public void handle(final Message<JsonObject> message) {

            final ReqObj reqGiftByObj = new ReqObj(message.body());

            final Promo.PromoReqObj reqPromoRec = new Promo.PromoReqObj();
            reqPromoRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            reqPromoRec.PROMO_NAME = "vnpthn";

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + reqGiftByObj.creator);
            log.add("amount", reqGiftByObj.amount);
            log.add("promotion name", reqPromoRec.PROMO_NAME);

            Misc.requestPromoRecord(vertx, reqPromoRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    final PromotionDb.Obj resPromoRec = new PromotionDb.Obj(jsonObject);

                    final JsonObject joReply = new JsonObject();

                    //khong co chuong trinh khuyen mai amway cho vietcombank
                    if (resPromoRec == null || resPromoRec.DATE_TO == 0 || resPromoRec.DATE_FROM == 0) {
                        log.add("*******", "viettinbank khong co chuong trinh khuyen mai");
                        log.writeLog();
                        joReply.putNumber("error",201);
                        joReply.putString("desc", "Không có chương trình khuyến mãi nào");
                        message.reply(joReply);
                        return;
                    }

                    //het thoi gian khuyen mai
                    long curTime = System.currentTimeMillis();
                    if ((curTime < resPromoRec.DATE_FROM) || (resPromoRec.DATE_TO < curTime)) {
                        log.add("from date", Misc.dateVNFormatWithTime(resPromoRec.DATE_FROM));
                        log.add("to date", Misc.dateVNFormatWithTime(resPromoRec.DATE_TO));
                        log.add("********", "viettinbank het thoi gian khuyen mai");
                        log.writeLog();

                        joReply.putNumber("error",202);
                        joReply.putString("desc", "Hết thời gian khuyến mãi");
                        message.reply(joReply);

                        return;
                    }

                    final Notification noti = new Notification();
                    noti.receiverNumber = reqGiftByObj.creator;
                    noti.sms = "";
                    noti.tranId= System.currentTimeMillis(); // ban tren toan he thong
                    noti.priority = 2;
                    noti.time =  System.currentTimeMillis();
                    noti.category = 0;

                    //chua map vi hoac map vi voi ngan hang khac
                    if(("".equalsIgnoreCase(reqGiftByObj.bankCode) || !"102".equalsIgnoreCase(reqGiftByObj.bankCode))
                            &&  ReqObj.online.equalsIgnoreCase(reqGiftByObj.mode)
                            ){
                        //send noti thong bao
                        //noti.caption = "Đăng ký với VCB để nhận thưởng khuyến mãi";
                        noti.caption="Đăng ký với Vietinbank để nhận ưu đãi";
                        //noti.body = "Cảm ơn quý khách đã quan tâm chương trình KM “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”. Quý khách cần đăng ký với Vietcombank để được kết nối với MoMo. Quà tặng trị giá 100 ngàn đồng sẽ chuyển ngay xuống ví MoMo sau khi quý khách thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ (để chứng thực tài khoản đang hoạt động). TT chi tiết: 0839917199";
                        noti.body = "Cảm ơn quý khách quan tâm chương trình ưu đãi tặng 100.000 đ. Quý khách cần đăng ký với Vietinbank để liên kết với Ví MoMo. Quý khách sẽ nhận ngay Thẻ quà tặng trị giá 100.000đ sau khi thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ từ nguồn tiền Vietinbank (để chứng thực tài khoản đang hoạt động). Chi tiết xin liên hệ: 0839917199";

                        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                        noti.htmlBody = noti.body;
                        noti.status = Notification.STATUS_DETAIL;
                        Misc.sendNoti(vertx,noti);

                        joReply.putNumber("error",203);
                        joReply.putString("desc","Chưa map vi Viettinbank");
                        message.reply(joReply);

                        return;
                    }

                    if(!"vnpthn".equalsIgnoreCase(reqGiftByObj.inviter) && ReqObj.online.equalsIgnoreCase(reqGiftByObj.mode)){

                        log.add("viettinbank", "Chua nhap ma gioi thieu");
                        log.writeLog();
                        return;
                    }

                    //kiem tra tra thuong theo card id
                    hasCardIdRecord(reqGiftByObj,new Handler<VcbCmndRecs.Obj>() {
                        @Override
                        public void handle(final VcbCmndRecs.Obj cmndObj) {

                            //khong co thong tin trong bang cmnd
                            if (cmndObj == null) {
                                log.add("*********", "viettinbank khong tim thay vi 0" + reqGiftByObj.creator + " trong ban cmnd, co the map lai vi");
                                log.writeLog();

                                joReply.putNumber("error",204);
                                joReply.putString("desc", "Không tìm thấy CMND trong bảng CMND");
                                message.reply(joReply);

                                return;
                            }

                            //da tra khuyen mai roi
                            if (cmndObj != null && cmndObj.promocount > 0) {
                                log.add("*********", "viettinbank tai khoan 0" + reqGiftByObj.creator + " da tra gift cho cmnd " + reqGiftByObj.cardId);
                                log.writeLog();

                                joReply.putNumber("error",205);
                                joReply.putString("desc", "Đã trả khuyến mãi rồi, vui lòng kiểm tra lại");
                                message.reply(joReply);
                                return;
                            }

                            //tao va tang gift
                            log.add("viettinbank", "tang gift cho 0" + reqGiftByObj.creator);
                            log.add("existsMMPhone", "VCBVerticle");
                            //check la diem giao dich
                            agentsDb.existsMMPhone("0" + reqGiftByObj.creator, "VCBVerticle", new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean exists) {
                                    if (exists) {

                                        joReply.putNumber("error",206);
                                        joReply.putString("desc","Điểm giao dịch");
                                        message.reply(joReply);

                                        log.add("khong tra qua", "0" + reqGiftByObj.creator + " la diem giao dich");
                                        log.writeLog();
                                        return;
                                    }

                                    //kiem tra co giao dich tu ngan hang ve chua
                                    hasBankTran(reqGiftByObj, resPromoRec, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean hasBankTran) {

                                            //chua co giao dich tu vcb ve
                                            if (!hasBankTran && ReqObj.online.equalsIgnoreCase(reqGiftByObj.mode)) {

                                                noti.caption = "Nạp tiền vào Ví để nhận ưu đãi";
                                                //noti.body = "Cảm ơn quý khách quan tâm chương trình KM “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn” và đã đăng ký với Vietcombank để kết nối tài khoản MoMo. Quà tặng trị giá 100 ngàn đồng sẽ chuyển ngay xuống ví MoMo sau khi quý khách thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ (để chứng thực tài khoản đang hoạt động). TT chi tiết: 0839917199";
                                                noti.body = "Cảm ơn quý khách quan tâm chương trình ưu đãi tặng 100.000 đ và đã đăng ký với Vietinbank để liên kết tài khoản MoMo. Quý khách sẽ nhận ngay Thẻ quà tặng trị giá 100.000đ sau khi thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ từ nguồn tiền Vietinbank (để chứng thực tài khoản đang hoạt động). Chi tiết xin liên hệ: 0839917199";

                                                noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                                noti.htmlBody = noti.body;
                                                noti.status = Notification.STATUS_DETAIL;
                                                Misc.sendNoti(vertx, noti);

                                                log.add("viettinbank", "chua co giao dich tu viettinbank ve");
                                                log.writeLog();

                                                joReply.putNumber("error",208);
                                                joReply.putString("desc", "Chưa có giao dịch Viettinbank");
                                                message.reply(joReply);
                                                return;
                                            }

                                            //kiem tra da tra thuong cho create by partner
                                            vcbRecords.countBy(reqGiftByObj.creator,reqGiftByObj.partner, new Handler<Integer>() {
                                                @Override
                                                public void handle(Integer count) {

                                                    //da tra thuong cho tai khoan nay roi
                                                    if(count >= 1){
                                                        joReply.putNumber("error",207);
                                                        joReply.putString("desc","Đã trả thưởng cho 0" + reqGiftByObj.creator +" bởi "+ reqGiftByObj.partner);
                                                        log.add("viettinbank","da tra thuong cho 0" + reqGiftByObj.creator +" boi "+ reqGiftByObj.partner);
                                                        message.reply(joReply);
                                                        log.writeLog();
                                                        return;
                                                    }

                                                    //tra thuong viettinbank
                                                    ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                                    keyValues.add(new Misc.KeyValue("promo", reqGiftByObj.partner));
                                                    keyValues.add(new Misc.KeyValue("mode", reqGiftByObj.mode));

                                                    giftManager.adjustGiftValue(resPromoRec.ADJUST_ACCOUNT
                                                            , "0" + reqGiftByObj.creator
                                                            , resPromoRec.PER_TRAN_VALUE
                                                            , keyValues, new Handler<JsonObject>() {
                                                        @Override
                                                        public void handle(JsonObject jsonObject) {

                                                            final int error = jsonObject.getInteger("error", -1);
                                                            long tranId = jsonObject.getLong("tranId", -1);
                                                            log.add("error", error);
                                                            log.add("error", SoapError.getDesc(error));
                                                            joReply.putNumber("error",error);
                                                            joReply.putString("desc", SoapError.getDesc(error));

                                                            if (error == 0) {

                                                                final String giftTypeId = reqGiftByObj.giftType;
                                                                GiftType giftType = new GiftType();
                                                                giftType.setModelId(giftTypeId);

                                                                giftManager.createLocalGift("0" + reqGiftByObj.creator
                                                                        , resPromoRec.PER_TRAN_VALUE
                                                                        , giftType
                                                                        , tranId
                                                                        , resPromoRec.ADJUST_ACCOUNT
                                                                        , resPromoRec.DURATION
                                                                        , null, new Handler<JsonObject>() {
                                                                    @Override
                                                                    public void handle(JsonObject jsonObject) {
                                                                        int err = jsonObject.getInteger("error", -1);
                                                                        long tranId = jsonObject.getInteger("tranId", -1);

                                                                        Gift gift = new Gift(jsonObject.getObject("gift"));

                                                                        log.add("viettinbank", "tra thuong chuong trinh viettinbank bang gift");
                                                                        log.add("err", err);

                                                                        if (err == 0) {
                                                                            vcbCmndRecs.incProCount(cmndObj.cardid, 1, new Handler<Boolean>() {
                                                                                @Override
                                                                                public void handle(Boolean aBoolean) {
                                                                                    message.reply(joReply);
                                                                                }
                                                                            });

                                                                            VcbRecords.Obj vcbObj = new VcbRecords.Obj();
                                                                            vcbObj.vourcherid = gift.getModelId();
                                                                            vcbObj.number = reqGiftByObj.creator;
                                                                            vcbObj.bankcode = reqGiftByObj.bankCode;
                                                                            vcbObj.partner = reqGiftByObj.partner;
                                                                            vcbObj.tranid = tranId;
                                                                            vcbObj.bankcode = reqGiftByObj.bankCode;
                                                                            vcbObj.card_id = cmndObj.cardid;
                                                                            vcbObj.tranType = reqGiftByObj.tranType;

                                                                            vcbRecords.save(vcbObj, new Handler<VcbRecords.Obj>() {
                                                                                @Override
                                                                                public void handle(VcbRecords.Obj obj) {
                                                                                }
                                                                            });

                                                                            //String tranComment = "Bạn nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình “Liên kết tài khoản VCB - Nhận ngay quà tặng 100 ngàn.”";
                                                                            String tranComment = "Bạn nhận được Thẻ quà tặng trị giá 100.000 đ từ chương trình ưu đãi cho khách hàng thuộc công ty VNPT – Hà Nội. Trước khi dùng Thẻ để thanh toán, bấm “Sử dụng” để kích hoạt Thẻ. Lưu ý thời hạn hiệu lực của Thẻ.";
                                                                            String notiCaption = "Nhận thẻ quà tặng!";
                                                                            //  String notiBody = "Chúc mừng bạn nhận Thẻ quà tặng từ chương trình “Liên kết tài khoản VCB - Nhận ngay quà tặng 100 ngàn”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi”. Sau đó bạn chọn xem và kích hoạt Thẻ quà tặng bạn vừa nhận! LH: (08) 399 171 99";
                                                                            String notiBody = "Chúc mừng bạn nhận Thẻ quà tặng từ chương trình ưu đãi cho nhóm khách hàng thuộc công ty VNPT- Hà Nội. Vui lòng về màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi”. Bạn bấm chọn vào Thẻ quà tặng để xem chi tiết. Trước khi dùng Thẻ quà tặng nào để thanh toán, bạn cần bấm “Sử dụng” để kích hoạt Thẻ đó! Chi tiết xin LH: 0839917199";
                                                                            //String giftMessage = "Bạn vừa nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình khuyến mãi “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”.”";
                                                                            String giftMessage = "Bạn vừa nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình ưu đãi cho nhóm khách hàng thuộc công ty VNPT – Hà Nội";

                                                                            Misc.sendTranHisAndNotiGift(vertx
                                                                                    , reqGiftByObj.creator
                                                                                    , tranComment
                                                                                    , tranId
                                                                                    , resPromoRec.PER_TRAN_VALUE
                                                                                    , gift
                                                                                    , notiCaption
                                                                                    , notiBody
                                                                                    , giftMessage
                                                                                    , tranDb);
                                                                            log.writeLog();

                                                                        } else {

                                                                            joReply.putNumber("error",err);
                                                                            joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                                                            message.reply(joReply);
                                                                            log.writeLog();
                                                                        }
                                                                    }
                                                                });
                                                            } else {
                                                                message.reply(joReply);
                                                                log.add("viettinbank", "Tra qua chuong trinh viettinbank loi");
                                                                log.writeLog();
                                                            }
                                                        }
                                                    });


                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    };
    //thuc hien khuyen mai chuong trinh nhap ma momo nhan 100K lien ket vietcombank
    private Handler<Message<JsonObject>> doPromotionGiftMoMo = new Handler<Message<JsonObject>>() {
        @Override
        public void handle(final Message<JsonObject> message) {

            final ReqObj reqGiftByObj = new ReqObj(message.body());

            final Promo.PromoReqObj reqPromotionMomoRec = new Promo.PromoReqObj();
            reqPromotionMomoRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            reqPromotionMomoRec.PROMO_NAME = "momo";

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + reqGiftByObj.creator);
            log.add("amount", reqGiftByObj.amount);
            log.add("promotion name", reqPromotionMomoRec.PROMO_NAME);
            log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(reqGiftByObj.tranType) == null ? "0" : MomoProto.TranHisV1.TranType.valueOf(reqGiftByObj.tranType));
            log.add("tranid", reqGiftByObj.tranId);

            Misc.requestPromoRecord(vertx, reqPromotionMomoRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    final PromotionDb.Obj resPromotionMomoRec = new PromotionDb.Obj(jsonObject);

                    final JsonObject joReply = new JsonObject();

                    //khong co chuong trinh khuyen mai momo cho vietcombank
                    if (resPromotionMomoRec == null || resPromotionMomoRec.DATE_TO == 0 || resPromotionMomoRec.DATE_FROM == 0) {
                        log.add("*******", "momo khong co chuong trinh khuyen mai");
                        log.writeLog();
                        joReply.putString("desc", "Không có chương trình khuyến mãi nào");
                        message.reply(joReply);
                        return;
                    }

                    //het thoi gian khuyen mai
                    long curTime = System.currentTimeMillis();
                    if ((curTime < resPromotionMomoRec.DATE_FROM) || (resPromotionMomoRec.DATE_TO < curTime)) {
                        log.add("from date", Misc.dateVNFormatWithTime(resPromotionMomoRec.DATE_FROM));
                        log.add("to date", Misc.dateVNFormatWithTime(resPromotionMomoRec.DATE_TO));
                        log.add("********", "momo het thoi gian khuyen mai");
                        log.writeLog();

                        joReply.putString("desc", "Hết thời gian khuyến mãi");
                        message.reply(joReply);

                        return;
                    }

                    final Notification noti = new Notification();
                    noti.receiverNumber = reqGiftByObj.creator;
                    noti.sms = "";
                    noti.tranId = System.currentTimeMillis(); // ban tren toan he thong
                    noti.priority = 2;
                    noti.time = System.currentTimeMillis();
                    noti.category = 0;

                    //chua map vi hoac map vi voi ngan hang khac
                    if ("".equalsIgnoreCase(reqGiftByObj.bankCode) || !"12345".equalsIgnoreCase(reqGiftByObj.bankCode)) {
                        //send noti thong bao
                        noti.caption = "Đăng ký với VCB để nhận thưởng khuyến mãi";
                        noti.body = "Cảm ơn quý khách đã quan tâm chương trình KM “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”. Quý khách cần đăng ký với Vietcombank để được kết nối với MoMo. Quà tặng trị giá 100 ngàn đồng sẽ chuyển ngay xuống ví MoMo sau khi quý khách thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ (để chứng thực tài khoản đang hoạt động). TT chi tiết: 0839917199";
                        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                        noti.htmlBody = noti.body;
                        noti.status = Notification.STATUS_DETAIL;
                        Misc.sendNoti(vertx, noti);

                        joReply.putString("desc", "Chưa map vi vietcomback");
                        message.reply(joReply);

                        return;
                    }

                    hasBankTran(reqGiftByObj, resPromotionMomoRec, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean hasVcbTran) {

                            //chua co giao dich tu vcb ve
                            if (!hasVcbTran) {

                                noti.caption = "Nạp tiền hoặc thanh toán lần đầu để nhận KM";
                                noti.body = "Cảm ơn quý khách quan tâm chương trình KM “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn” và đã đăng ký với Vietcombank để kết nối tài khoản MoMo. Quà tặng trị giá 100 ngàn đồng sẽ chuyển ngay xuống ví MoMo sau khi quý khách thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ (để chứng thực tài khoản đang hoạt động). TT chi tiết: 0839917199";
                                noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                noti.htmlBody = noti.body;
                                noti.status = Notification.STATUS_DETAIL;
                                Misc.sendNoti(vertx, noti);

                                log.add("momodesc", "chua co giao dich tu vietcombank ve");
                                log.writeLog();

                                joReply.putString("desc", "Chưa có giao dịch bank-in");
                                message.reply(joReply);

                                return;
                            }

                            //tra thuong cho ban PG gioi thieu nguoi nay
                            if (DataUtil.strToInt(reqGiftByObj.partner) >= 6100
                                    && DataUtil.strToInt(reqGiftByObj.partner) <= 6200) {

                                glxDb.find(String.valueOf(DataUtil.strToInt(reqGiftByObj.partner))
                                        , String.valueOf(reqGiftByObj.creator)
                                        , new Handler<ArrayList<PgGlxDb.Obj>>() {
                                    @Override
                                    public void handle(ArrayList<PgGlxDb.Obj> list) {

                                        //het so lan khuyen mai
                                        if (list.size() >= 3) {
                                            log.add("pggalaxy", "Het so lan khuyen mai " + list.size() + ">=" + 3);
                                            log.writeLog();
                                            return;
                                        }

                                        boolean paied = false;
                                        for (int i = 0; i < list.size(); i++) {
                                            if (list.get(i).value == 20000) {
                                                paied = true;
                                                break;
                                            }
                                        }

                                        if (paied) {
                                            log.add("pggalaxy", "da tra thuong cho giao dich loai nay roi tran name " + MomoProto.TranHisV1.TranType.valueOf(1).name());
                                            return;
                                        }

                                        PgGlxDb.Obj obj = new PgGlxDb.Obj();
                                        obj.pgcode = String.valueOf(DataUtil.strToInt(reqGiftByObj.partner));
                                        obj.number = String.valueOf(reqGiftByObj.creator);
                                        obj.value = 20000;
                                        obj.tranname = MomoProto.TranHisV1.TranType.valueOf(1).name();
                                        obj.serviceid = "";

                                        glxDb.save(obj, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {
                                            }
                                        });
                                    }
                                });
                            }

                            //kiem tra trong bang cmnd
                            vcbCmndRecs.findOne(reqGiftByObj.cardId, new Handler<VcbCmndRecs.Obj>() {
                                @Override
                                public void handle(final VcbCmndRecs.Obj cmndObj) {

                                    //khong co thong tin trong bang cmnd
                                    if (cmndObj == null) {
                                        log.add("*********", "momo khong tim thay vi 0" + reqGiftByObj.creator + " trong ban cmnd, co the map lai vi");
                                        log.writeLog();

                                        joReply.putString("desc", "Không tìm thấy CMND trong bảng CMND");
                                        message.reply(joReply);

                                        return;
                                    }

                                    //da tra khuyen mai roi
                                    if (cmndObj != null && cmndObj.promocount > 0) {
                                        log.add("*********", "momo tai khoan 0" + reqGiftByObj.creator + " da tra gift cho cmnd");
                                        log.writeLog();

                                        joReply.putString("desc", "Đã trả khuyến mãi rồi, vui lòng kiểm tra lại");
                                        message.reply(joReply);
                                        return;
                                    }

                                    //tao va tang gift cho B
                                    log.add("*********", "tang gift cho 0" + reqGiftByObj.creator);
                                    log.add("existsMMPhone", "VCBVerticle");
                                    //check la diem giao dich
                                    agentsDb.existsMMPhone("0" + reqGiftByObj.creator,"VCBVerticle", new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean exists) {
                                            if (exists) {
                                                log.add("khong tra qua", "0" + reqGiftByObj.creator + " la diem giao dich");
                                                log.writeLog();
                                                return;
                                            }

                                            ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                            keyValues.add(new Misc.KeyValue("promo", reqGiftByObj.partner));

                                            giftManager.adjustGiftValue(resPromotionMomoRec.ADJUST_ACCOUNT
                                                    , "0" + reqGiftByObj.creator
                                                    , resPromotionMomoRec.PER_TRAN_VALUE
                                                    , keyValues, new Handler<JsonObject>() {
                                                @Override
                                                public void handle(JsonObject jsonObject) {

                                                    final int error = jsonObject.getInteger("error", -1);
                                                    long tranId = jsonObject.getLong("tranId", -1);
                                                    log.add("error", error);
                                                    log.add("momodesc", SoapError.getDesc(error));

                                                    if (error == 0) {

                                                        final String giftTypeId = "momo";
                                                        GiftType giftType = new GiftType();
                                                        giftType.setModelId(giftTypeId);

                                                        ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                                        Misc.KeyValue kv = new Misc.KeyValue();
                                                        kv.Key = "inviter";
                                                        kv.Value = reqGiftByObj.partner;
                                                        keyValues.add(kv);

                                                        giftManager.createLocalGift("0" + reqGiftByObj.creator
                                                                , resPromotionMomoRec.PER_TRAN_VALUE
                                                                , giftType
                                                                , tranId
                                                                , resPromotionMomoRec.ADJUST_ACCOUNT
                                                                , resPromotionMomoRec.DURATION
                                                                , keyValues, new Handler<JsonObject>() {
                                                            @Override
                                                            public void handle(JsonObject jsonObject) {
                                                                int err = jsonObject.getInteger("error", -1);
                                                                long tranId = jsonObject.getInteger("tranId", -1);

                                                                Gift gift = new Gift(jsonObject.getObject("gift"));

                                                                log.add("momodesc", "tra thuong chuong trinh momo bang gift");
                                                                log.add("err", err);

                                                                if (err == 0) {
                                                                    vcbCmndRecs.incProCount(cmndObj.cardid, 1, new Handler<Boolean>() {
                                                                        @Override
                                                                        public void handle(Boolean aBoolean) {
                                                                        }
                                                                    });

                                                                    joReply.putString("desc", "Thành công");
                                                                    message.reply(joReply);

                                                                    VcbRecords.Obj vcbObj = new VcbRecords.Obj();
                                                                    vcbObj.vourcherid = gift.getModelId();
                                                                    vcbObj.number = reqGiftByObj.creator;
                                                                    vcbObj.bankcode = reqGiftByObj.bankCode;
                                                                    vcbObj.partner = reqGiftByObj.partner;
                                                                    vcbObj.tranid = tranId;
                                                                    vcbObj.bankcode = reqGiftByObj.bankCode;
                                                                    vcbObj.card_id = cmndObj.cardid;
                                                                    vcbObj.tranType = reqGiftByObj.tranType;

                                                                    vcbRecords.save(vcbObj, new Handler<VcbRecords.Obj>() {
                                                                        @Override
                                                                        public void handle(VcbRecords.Obj obj) {
                                                                        }
                                                                    });

                                                                    String tranComment = "Bạn nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình “Liên kết tài khoản VCB - Nhận ngay quà tặng 100 ngàn.”";
                                                                    String notiCaption = "Nhận thẻ quà tặng!";
                                                                    String notiBody = "Chúc mừng bạn nhận Thẻ quà tặng từ chương trình “Liên kết tài khoản VCB - Nhận ngay quà tặng 100 ngàn”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi”. Sau đó bạn chọn xem và kích hoạt Thẻ quà tặng bạn vừa nhận! LH: (08) 399 171 99";
                                                                    String giftMessage = "Bạn vừa nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình “Liên kết tài khoản VCB - Nhận ngay quà tặng 100 ngàn.”";

                                                                    Misc.sendTranHisAndNotiGift(vertx
                                                                            , reqGiftByObj.creator
                                                                            , tranComment
                                                                            , tranId
                                                                            , resPromotionMomoRec.PER_TRAN_VALUE
                                                                            , gift
                                                                            , notiCaption
                                                                            , notiBody
                                                                            , giftMessage
                                                                            , tranDb);
                                                                    log.writeLog();

                                                                } else {

                                                                    joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                                                    message.reply(joReply);

                                                                    log.writeLog();
                                                                }
                                                            }
                                                        });
                                                    } else {
                                                        log.add("momodesc", "Tra qua chuong trinh momo loi");
                                                        log.writeLog();

                                                        joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                                        message.reply(joReply);

                                                    }
                                                }
                                            });

                                        }
                                    });

                                }
                            });
                        }
                    });
                }
            });
        }
    };
    //tra khuyen mai cho nguoi gio thieu chuong trinh VCB
    private Handler<Message<JsonObject>> doPromotionGiftFor_A = new Handler<Message<JsonObject>>() {
        @Override
        public void handle(final Message<JsonObject> message) {

            final ReqObj reqGiftForAObj = new ReqObj(message.body());

            Promo.PromoReqObj reqPromotionVCBRec = new Promo.PromoReqObj();
            reqPromotionVCBRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            reqPromotionVCBRec.PROMO_NAME = "vcbpromo";

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + reqGiftForAObj.creator);
            log.add("amount", reqGiftForAObj.amount);
            log.add("promotion name", reqPromotionVCBRec.PROMO_NAME);

            final JsonObject joReply = new JsonObject();
            joReply.putNumber("err", 0);
            joReply.putString("desc", "Thành công");

            Misc.requestPromoRecord(vertx, reqPromotionVCBRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {

                    final PromotionDb.Obj resPromotionVCBRec = new PromotionDb.Obj(jsonObject);

                    //khong co khuyen mai VCB
                    if (resPromotionVCBRec == null || resPromotionVCBRec.DATE_FROM == 0 || resPromotionVCBRec.DATE_TO == 0) {

                        joReply.putNumber("err", -1);
                        joReply.putString("desc", "Không có chương trình khuyến mãi VCB");
                        message.reply(joReply);

                        log.add("*******", "vcb khong co chuong trinh khuyen mai");
                        log.writeLog();
                        return;
                    }

                    vcbRecords.countBy(reqGiftForAObj.creator, reqGiftForAObj.partner, new Handler<Integer>() {
                        @Override
                        public void handle(Integer count) {

                            //da tra khuyen mai cho A khi B su dung qua chuong trinh VCB
                            if (count > 0) {

                                joReply.putNumber("err", -1);
                                joReply.putString("desc", "Đã trả khuyến mãi cho A: 0" + reqGiftForAObj.creator + ", được nhận do B: " + reqGiftForAObj.partner + " sử dụng quà VCB thành công. Ngày nhận quà ");
                                message.reply(joReply);

                                log.add("*********", "vcb da tra khuyen mai cho A = 0" + reqGiftForAObj.creator + " , duoc nhan do B = " + reqGiftForAObj.partner + " su dung gift");
                                log.writeLog();
                                return;
                            }

                            //kiem tra ben A
                            phonesDb.getPhoneObjInfo(reqGiftForAObj.creator, new Handler<PhonesDb.Obj>() {
                                @Override
                                public void handle(final PhonesDb.Obj phoneObjA) {
                                    if (phoneObjA == null) {

                                        joReply.putNumber("err", -1);
                                        joReply.putString("desc", "Không tồn tại số người giới thiệu trên hệ thống: 0" + reqGiftForAObj.creator);
                                        message.reply(joReply);

                                        log.add("*********", "vcb khong ton tai 0" + reqGiftForAObj.creator + " tren he thong");
                                        log.writeLog();
                                        return;
                                    }

                                    if (!"12345".equalsIgnoreCase(phoneObjA.bank_code)) {

                                        joReply.putNumber("err", -1);
                                        joReply.putString("desc", "Số người giới thiệu : 0" + reqGiftForAObj.creator + " chưa map ví VCB");
                                        message.reply(joReply);

                                        log.add("*********", "vcb " + reqGiftForAObj.creator + " chua map vi");
                                        log.writeLog();
                                        return;
                                    }
                                    log.add("existsMMPhone", "VCBVerticle");
                                    //kiem tra diem giao dich --> khong tra qua
                                    agentsDb.existsMMPhone("0" + reqGiftForAObj.creator,"VCBVerticle", new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean exist) {
                                            if (exist) {
                                                log.add("khong tra qua", "vi 0" + reqGiftForAObj.creator + " la diem giao dich");
                                                log.writeLog();
                                                return;
                                            }

                                            //tra qua
                                            String giftTypeA = "VCBPROMO_A"; // not transferable
                                            log.add("giftTypeA", giftTypeA);
                                            log.add("*********", "vcb tao va tang gift cho A 0" + reqGiftForAObj);
                                            giveGift(resPromotionVCBRec
                                                    , giftTypeA
                                                    , reqGiftForAObj
                                                    , phoneObjA
                                                    , log, new Handler<Integer>() {
                                                @Override
                                                public void handle(Integer error) {
                                                    //create and send gift to A success

                                                    if (error == 0) {
                                                        //send noti to A
                                                        final Notification noti = new Notification();
                                                        noti.receiverNumber = reqGiftForAObj.creator;
                                                        noti.caption = VcbNoti.UseVoucher.Cap.ARecieveVc;
                                                        noti.body = String.format(VcbNoti.UseVoucher.Body.ARecieveVc, reqGiftForAObj.partner);

                                                        noti.htmlBody = String.format(VcbNoti.UseVoucher.Body.ARecieveVc, reqGiftForAObj.partner);
                                                        noti.sms = "";
                                                        noti.tranId = System.currentTimeMillis(); // ban tren toan he thong
                                                        noti.status = Notification.STATUS_DETAIL;
                                                        noti.type = MomoProto.NotificationType.NOTI_STUDENT_VALUE;
                                                        noti.priority = 2;
                                                        noti.time = System.currentTimeMillis();
                                                        noti.category = 0;

                                                        Misc.sendNoti(vertx, noti);

                                                        joReply.putNumber("err", 0);
                                                        joReply.putString("desc", "Tặng quà cho : 0" + reqGiftForAObj.creator + " thành công");
                                                        message.reply(joReply);
                                                    } else {

                                                        joReply.putNumber("err", -1);
                                                        joReply.putString("desc", "Tặng quà cho : 0" + reqGiftForAObj.creator + " không thành công");
                                                        message.reply(joReply);

                                                    }
                                                }
                                            });

                                        }
                                    });

                                }
                            });

                        }
                    });
                }
            });
        }
    };
    private int amwayCodeMin = 7000;
    private int amwayCodeMax = 7009;
    private Handler<Message<JsonObject>> doPromotionGiftFor_B = new Handler<Message<JsonObject>>() {
        @Override
        public void handle(final Message<JsonObject> message) {

            final ReqObj reqGiftForBObj = new ReqObj(message.body());

            Promo.PromoReqObj reqPromotionVCBRec = new Promo.PromoReqObj();
            reqPromotionVCBRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            reqPromotionVCBRec.PROMO_NAME = "vcbpromo";

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + reqGiftForBObj.creator);
            log.add("amount", reqGiftForBObj.amount);
            log.add("promotion name", reqPromotionVCBRec.PROMO_NAME);
            log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(reqGiftForBObj.tranType) == null ? "0" : MomoProto.TranHisV1.TranType.valueOf(reqGiftForBObj.tranType));
            log.add("tranid", reqGiftForBObj.tranId);

            final JsonObject joReply = new JsonObject();

            Misc.requestPromoRecord(vertx, reqPromotionVCBRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    final PromotionDb.Obj resPromotionVCBRec = new PromotionDb.Obj(jsonObject);

                    //khong co chuong trinh khuyen mai vcbpromo
                    if (resPromotionVCBRec == null || resPromotionVCBRec.DATE_FROM == 0 || resPromotionVCBRec.DATE_TO == 0) {
                        joReply.putString("desc", "Không có chương trình khuyến mãi");
                        message.reply(joReply);
                        log.add("*******", "vcb khong co chuong trinh khuyen mai");
                        log.writeLog();
                        return;
                    }

                    //kiem tra con thoi gian khuyen mai khong
                    long curTime = System.currentTimeMillis();
                    if ((curTime < resPromotionVCBRec.DATE_FROM) || (curTime > resPromotionVCBRec.DATE_TO)) {

                        joReply.putString("desc", "Hết chương trình khuyến mãi VCB");
                        message.reply(joReply);

                        log.add("from date", Misc.dateVNFormatWithTime(resPromotionVCBRec.DATE_FROM));
                        log.add("to date", Misc.dateVNFormatWithTime(resPromotionVCBRec.DATE_TO));
                        log.add("********", "vcb het thoi gian khuyen mai");
                        log.writeLog();
                        return;
                    }

                    final Notification noti = new Notification();
                    noti.receiverNumber = reqGiftForBObj.creator;
                    noti.sms = "";
                    noti.tranId = System.currentTimeMillis(); // ban tren toan he thong
                    noti.priority = 2;
                    noti.time = System.currentTimeMillis();
                    noti.category = 0;

                    //check B side
                    phonesDb.getPhoneObjInfo(reqGiftForBObj.creator, new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(final PhonesDb.Obj phoneObjB) {
                            if (phoneObjB == null) {

                                joReply.putString("desc", "Không tìm thấy số điện thoại người được giới thiệu");
                                message.reply(joReply);

                                log.add("*********", "vcb khong ton tai 0" + reqGiftForBObj.creator + " tren he thong");
                                log.writeLog();
                                return;
                            }

                            if ("".equalsIgnoreCase(phoneObjB.inviter)
                                    || "momo".equalsIgnoreCase(phoneObjB.inviter)
                                    || (6100 <= DataUtil.strToInt(phoneObjB.inviter) && DataUtil.strToInt(phoneObjB.inviter) <= 6200)
                                    || (amwayCodeMin <= DataUtil.strToInt(phoneObjB.inviter) && DataUtil.strToInt(phoneObjB.inviter) <= amwayCodeMax)
                                    ) {

                                joReply.putString("desc", "Người giới thiệu " + phoneObjB.inviter + " không hợp lệ");
                                message.reply(joReply);
                                log.add("*********", "vcb nguoi gioi thieu" + phoneObjB.inviter + " khong hop le");
                                log.writeLog();
                                return;
                            }

                            if ("".equalsIgnoreCase(phoneObjB.bankPersonalId)) {
                                joReply.putString("desc", "Chưa có bank personal id từ ngân hàng");
                                message.reply(joReply);
                                log.add("*********", "vcb chua co bank personal id tu ngan hang");
                                log.writeLog();
                                return;

                            }

                            //kiem tra trong bang cmnd
                            vcbCmndRecs.findOne(phoneObjB.bankPersonalId, new Handler<VcbCmndRecs.Obj>() {
                                @Override
                                public void handle(final VcbCmndRecs.Obj cmndObj) {
                                    //da tra khuyen mai roi
                                    if (cmndObj != null && cmndObj.promocount > 0) {

                                        joReply.putString("desc", "Đã trả thưởng cho chứng minh nhân dân " + cmndObj.cardid);
                                        message.reply(joReply);

                                        log.add("*********", "vcb tai khoan 0" + phoneObjB.number + " da  tao gift cho cmnd");
                                        log.writeLog();
                                        return;
                                    } else if (cmndObj == null) {

                                        joReply.putString("desc", "Không có thông tin chứng minh nhân dân của 0" + phoneObjB.number);
                                        message.reply(joReply);
                                        //khong co thong tin trong bang cmnd
                                        log.add("*********", "vcb khong tim thay vi 0" + phoneObjB.number + " trong ban cmnd, co the map lai vi");
                                        log.writeLog();
                                        return;
                                    }
                                    log.add("existsMMPhone", "VCBVerticle");
                                    //check diem giao dich
                                    agentsDb.existsMMPhone("0" + reqGiftForBObj.creator,"VCBVerticle", new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean exists) {
                                            if (exists) {
                                                log.add("khong tra qua", "vi 0" + reqGiftForBObj.creator + " la diem giao dich");
                                                log.writeLog();
                                                return;
                                            }

                                            //tao va tang gift cho B
                                            String giftTypeB = "VCBPROMO_B"; // not transferable
                                            log.add("*********", "vcb tao va tang gift cho 0" + reqGiftForBObj.creator);
                                            giveGift(resPromotionVCBRec, giftTypeB, reqGiftForBObj, phoneObjB, log, new Handler<Integer>() {
                                                @Override
                                                public void handle(Integer ok) {

                                                    if (ok == 0) {
                                                        joReply.putString("desc", "Trả voucher cho 0" + reqGiftForBObj.creator + " thành công");
                                                        vcbCmndRecs.incProCount(cmndObj.cardid, 1, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {

                                                                //------------give gift for A.start-----------
                                                                final ReqObj reqGiftForAObj = new ReqObj();
                                                                reqGiftForAObj.creator = Integer.parseInt(DataUtil.stringToVnPhoneNumber(phoneObjB.inviter) + "");
                                                                reqGiftForAObj.partner = "0" + phoneObjB.number;

                                                                if (reqGiftForAObj.creator <= 0) {
                                                                    log.add("0_0", "so dien thoai khong hop le.");
                                                                    log.writeLog();
                                                                    return;
                                                                }

                                                                vcbRecords.countBy(reqGiftForAObj.creator, reqGiftForAObj.partner, new Handler<Integer>() {
                                                                    @Override
                                                                    public void handle(Integer count) {

                                                                        //da tra khuyen mai cho A khi B su dung qua chuong trinh VCB
                                                                        if (count > 0) {

                                                                            joReply.putNumber("err", -1);
                                                                            joReply.putString("desc", "Đã trả khuyến mãi cho A: 0" + reqGiftForAObj.creator + ", được nhận do B: " + reqGiftForAObj.partner + " sử dụng quà VCB thành công. Ngày nhận quà ");
                                                                            log.add("*********", "vcb da tra khuyen mai cho A = 0" + reqGiftForAObj.creator + " , duoc nhan do B = " + reqGiftForAObj.partner + " su dung gift");
                                                                            message.reply(joReply);
                                                                            log.writeLog();
                                                                            return;
                                                                        }

                                                                        //kiem tra ben A
                                                                        phonesDb.getPhoneObjInfo(reqGiftForAObj.creator, new Handler<PhonesDb.Obj>() {
                                                                            @Override
                                                                            public void handle(final PhonesDb.Obj phoneObjA) {
                                                                                if (phoneObjA == null) {

                                                                                    joReply.putNumber("err", -1);
                                                                                    joReply.putString("desc", "Không tồn tại số người giới thiệu trên hệ thống: 0" + reqGiftForAObj.creator);
                                                                                    message.reply(joReply);

                                                                                    log.add("*********", "vcb khong ton tai 0" + reqGiftForAObj.creator + " tren he thong");
                                                                                    log.writeLog();
                                                                                    return;
                                                                                }

                                                                                if (!"12345".equalsIgnoreCase(phoneObjA.bank_code)) {

                                                                                    joReply.putNumber("err", -1);
                                                                                    joReply.putString("desc", "Số người giới thiệu : 0" + reqGiftForAObj.creator + " chưa map ví VCB");
                                                                                    message.reply(joReply);

                                                                                    log.add("*********", "vcb " + reqGiftForAObj.creator + " chua map vi");
                                                                                    log.writeLog();
                                                                                    return;
                                                                                }
                                                                                log.add("existsMMPhone", "VCBVerticle");
                                                                                //kiem tra diem giao dich --> khong tra qua
                                                                                agentsDb.existsMMPhone("0" + reqGiftForAObj.creator, "VCBVerticle",new Handler<Boolean>() {
                                                                                    @Override
                                                                                    public void handle(Boolean exist) {
                                                                                        if (exist) {
                                                                                            log.add("khong tra qua", "vi 0" + reqGiftForAObj.creator + " la diem giao dich");
                                                                                            log.writeLog();
                                                                                            return;
                                                                                        }

                                                                                        //tra qua cho nguoi gioi thieu (ben A)
                                                                                        String giftTypeA = "VCBPROMO_A"; // not transferable
                                                                                        log.add("giftTypeA", giftTypeA);
                                                                                        log.add("*********", "vcb tao va tang gift cho A 0" + reqGiftForAObj);
                                                                                        giveGift(resPromotionVCBRec
                                                                                                , giftTypeA
                                                                                                , reqGiftForAObj
                                                                                                , phoneObjA
                                                                                                , log, new Handler<Integer>() {
                                                                                            @Override
                                                                                            public void handle(Integer error) {

                                                                                                //create and send gift to A success
                                                                                                if (error == 0) {
                                                                                                    //send noti to A
                                                                                                    final Notification noti = new Notification();
                                                                                                    noti.receiverNumber = reqGiftForAObj.creator;
                                                                                                    noti.caption = VcbNoti.UseVoucher.Cap.ARecieveVc;
                                                                                                    noti.body = String.format(VcbNoti.UseVoucher.Body.ARecieveVc, reqGiftForAObj.partner);

                                                                                                    noti.htmlBody = String.format(VcbNoti.UseVoucher.Body.ARecieveVc, reqGiftForAObj.partner);
                                                                                                    noti.sms = "";
                                                                                                    noti.tranId = System.currentTimeMillis(); // ban tren toan he thong
                                                                                                    noti.status = Notification.STATUS_DETAIL;
                                                                                                    noti.type = MomoProto.NotificationType.NOTI_STUDENT_VALUE;
                                                                                                    noti.priority = 2;
                                                                                                    noti.time = System.currentTimeMillis();
                                                                                                    noti.category = 0;

                                                                                                    Misc.sendNoti(vertx, noti);

                                                                                                    joReply.putNumber("err", 0);
                                                                                                    joReply.putString("desc", "Tặng quà cho : 0" + reqGiftForAObj.creator + " thành công");
                                                                                                    message.reply(joReply);
                                                                                                } else {

                                                                                                    joReply.putNumber("err", -1);
                                                                                                    joReply.putString("desc", "Tặng quà cho : 0" + reqGiftForAObj.creator + " không thành công");
                                                                                                    message.reply(joReply);

                                                                                                }
                                                                                            }
                                                                                        });

                                                                                    }
                                                                                });

                                                                            }
                                                                        });

                                                                    }
                                                                });

                                                                //give gift for A.end
                                                            }
                                                        });
                                                    } else {
                                                        joReply.putString("desc", "Trả voucher cho 0" + reqGiftForBObj.creator + " lỗi : " + SoapError.getDesc(ok));
                                                    }
                                                    message.reply(joReply);
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    };
    private long amwayBonusValue = 20000;
    //thuc hien khuyen mai chuong trinh nhap ma amway nhan 100K lien ket vietcombank
    private Handler<Message<JsonObject>> doPromotionGiftMoMoForAmway = new Handler<Message<JsonObject>>() {
        @Override
        public void handle(final Message<JsonObject> message) {

            final ReqObj reqGiftByObj = new ReqObj(message.body());


            final Promo.PromoReqObj reqPromoRec = new Promo.PromoReqObj();
            reqPromoRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            reqPromoRec.PROMO_NAME = "amwaypromo";

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + reqGiftByObj.creator);
            log.add("amount", reqGiftByObj.amount);
            log.add("promotion name", reqPromoRec.PROMO_NAME);
            log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(reqGiftByObj.tranType) == null ? "0" : MomoProto.TranHisV1.TranType.valueOf(reqGiftByObj.tranType));
            log.add("tranid", reqGiftByObj.tranId);

            Misc.requestPromoRecord(vertx, reqPromoRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    final PromotionDb.Obj resPromoRec = new PromotionDb.Obj(jsonObject);

                    final JsonObject joReply = new JsonObject();

                    //khong co chuong trinh khuyen mai amway cho vietcombank
                    if (resPromoRec == null || resPromoRec.DATE_TO == 0 || resPromoRec.DATE_FROM == 0) {
                        log.add("*******", "amway khong co chuong trinh khuyen mai");
                        log.writeLog();
                        joReply.putString("desc", "Không có chương trình khuyến mãi nào");
                        message.reply(joReply);
                        return;
                    }

                    //het thoi gian khuyen mai
                    long curTime = System.currentTimeMillis();
                    if ((curTime < resPromoRec.DATE_FROM) || (resPromoRec.DATE_TO < curTime)) {
                        log.add("from date", Misc.dateVNFormatWithTime(resPromoRec.DATE_FROM));
                        log.add("to date", Misc.dateVNFormatWithTime(resPromoRec.DATE_TO));
                        log.add("********", "amway het thoi gian khuyen mai");
                        log.writeLog();

                        joReply.putString("desc", "Hết thời gian khuyến mãi");
                        message.reply(joReply);

                        return;
                    }

                    final Notification noti = new Notification();
                    noti.receiverNumber = reqGiftByObj.creator;
                    noti.sms = "";
                    noti.tranId= System.currentTimeMillis(); // ban tren toan he thong
                    noti.priority = 2;
                    noti.time =  System.currentTimeMillis();
                    noti.category = 0;

                    //chua map vi hoac map vi voi ngan hang khac
                    if("".equalsIgnoreCase(reqGiftByObj.bankCode) || !"12345".equalsIgnoreCase(reqGiftByObj.bankCode)){
                        //send noti thong bao
                        //noti.caption = "Đăng ký với VCB để nhận thưởng khuyến mãi";
                        noti.caption="Đăng ký với Vietcombank để nhận KM";
                        //noti.body = "Cảm ơn quý khách đã quan tâm chương trình KM “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”. Quý khách cần đăng ký với Vietcombank để được kết nối với MoMo. Quà tặng trị giá 100 ngàn đồng sẽ chuyển ngay xuống ví MoMo sau khi quý khách thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ (để chứng thực tài khoản đang hoạt động). TT chi tiết: 0839917199";
                        noti.body = "Cảm ơn quý khách quan tâm chương trình khuyến mãi tặng 100.000 đ. Quý khách cần đăng ký với Vietcombank để liên kết với Ví MoMo. Quý khách sẽ nhận ngay Thẻ quà tặng trị giá 100 ngàn đồng sau khi thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ từ nguồn tiền Vietcombank (để chứng thực tài khoản đang hoạt động). Chi tiết xin liên hệ: 0839917199";

                        noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;


                        noti.htmlBody = noti.body;
                        noti.status = Notification.STATUS_DETAIL;
                        Misc.sendNoti(vertx,noti);

                        joReply.putString("desc","Chưa map vi vietcomback");
                        message.reply(joReply);

                        return;
                    }

                    vcbCmndRecs.findOne(reqGiftByObj.cardId, new Handler<VcbCmndRecs.Obj>() {
                        @Override
                        public void handle(final VcbCmndRecs.Obj cmndObj) {

                            //khong co thong tin trong bang cmnd
                            if (cmndObj == null) {
                                log.add("*********", "amway khong tim thay vi 0" + reqGiftByObj.creator + " trong ban cmnd, co the map lai vi");
                                log.writeLog();

                                joReply.putString("desc", "Không tìm thấy CMND trong bảng CMND");
                                message.reply(joReply);

                                return;
                            }

                            //da tra khuyen mai roi
                            if (cmndObj != null && cmndObj.promocount > 0) {
                                log.add("*********", "amway tai khoan 0" + reqGiftByObj.creator + " da tra gift cho cmnd " + reqGiftByObj.cardId);
                                log.writeLog();

                                joReply.putString("desc", "Đã trả khuyến mãi rồi, vui lòng kiểm tra lại");
                                message.reply(joReply);
                                return;
                            }

                            //tao va tang gift cho B
                            log.add("amway", "tang gift cho 0" + reqGiftByObj.creator);
                            log.add("existsMMPhone", "VCBVerticle");
                            //check la diem giao dich
                            agentsDb.existsMMPhone("0" + reqGiftByObj.creator, "VCBVerticle",new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean exists) {
                                    if (exists) {
                                        log.add("khong tra qua", "0" + reqGiftByObj.creator + " la diem giao dich");
                                        log.writeLog();
                                        return;
                                    }

                                    hasBankTran(reqGiftByObj, resPromoRec, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean hasVcbTran) {

                                            //chua co giao dich tu vcb ve
                                            if (!hasVcbTran) {

                                                noti.caption = "Nạp tiền vào MoMo để nhận KM";
                                                //noti.body = "Cảm ơn quý khách quan tâm chương trình KM “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn” và đã đăng ký với Vietcombank để kết nối tài khoản MoMo. Quà tặng trị giá 100 ngàn đồng sẽ chuyển ngay xuống ví MoMo sau khi quý khách thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ (để chứng thực tài khoản đang hoạt động). TT chi tiết: 0839917199";
                                                noti.body = "Cảm ơn quý khách quan tâm chương trình khuyến mãi tặng 100.000đ và đã đăng ký với Vietcombank để liên kết tài khoản MoMo. Quý khách sẽ nhận ngay Thẻ quà tặng trị giá 100 ngàn đồng sau khi thực hiện một giao dịch nạp tiền vào MoMo hoặc thanh toán với giá trị bất kỳ từ nguồn tiền Vietcombank (để chứng thực tài khoản đang hoạt động). Chi tiết xin liên hệ: 0839917199";

                                                noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                                noti.htmlBody = noti.body;
                                                noti.status = Notification.STATUS_DETAIL;
                                                Misc.sendNoti(vertx, noti);

                                                log.add("amwaydesc", "chua co giao dich tu vietcombank ve");
                                                log.writeLog();

                                                joReply.putString("desc", "Chưa có giao dịch bank-in");
                                                message.reply(joReply);

                                                return;
                                            }

                                            //tra thuong cho ban amway gioi thieu nguoi nay
                                            if (DataUtil.strToInt(reqGiftByObj.partner) >= amwayCodeMin
                                                    && DataUtil.strToInt(reqGiftByObj.partner) <= amwayCodeMax) {

                                                glxDb.find(String.valueOf(DataUtil.strToInt(reqGiftByObj.partner))
                                                        , String.valueOf(reqGiftByObj.creator)
                                                        , new Handler<ArrayList<PgGlxDb.Obj>>() {
                                                    @Override
                                                    public void handle(ArrayList<PgGlxDb.Obj> list) {

                                                        //het so lan khuyen mai
                                                        if (list.size() >= 1) {
                                                            log.add("amway", "Het so lan khuyen mai " + list.size() + ">=" + 1);
                                                            log.writeLog();
                                                            return;
                                                        }

                                                        PgGlxDb.Obj obj = new PgGlxDb.Obj();
                                                        obj.pgcode = String.valueOf(DataUtil.strToInt(reqGiftByObj.partner));
                                                        obj.number = String.valueOf(reqGiftByObj.creator);
                                                        obj.value = amwayBonusValue;
                                                        obj.tranname = MomoProto.TranHisV1.TranType.valueOf(1).name();
                                                        obj.serviceid = "amwaypromo";

                                                        glxDb.save(obj, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {
                                                            }
                                                        });
                                                    }
                                                });

                                                // Trả khuyến mãi
                                                ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                                keyValues.add(new Misc.KeyValue("promo", reqGiftByObj.partner));

                                                giftManager.adjustGiftValue(resPromoRec.ADJUST_ACCOUNT
                                                        , "0" + reqGiftByObj.creator
                                                        , resPromoRec.PER_TRAN_VALUE
                                                        , keyValues, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject jsonObject) {

                                                        final int error = jsonObject.getInteger("error", -1);
                                                        long tranId = jsonObject.getLong("tranId", -1);
                                                        log.add("error", error);
                                                        log.add("amwaydesc", SoapError.getDesc(error));

                                                        if (error == 0) {

                                                            final String giftTypeId = "amwaypromo";
                                                            GiftType giftType = new GiftType();
                                                            giftType.setModelId(giftTypeId);

                                                            ArrayList<Misc.KeyValue> keyValues = new ArrayList<Misc.KeyValue>();
                                                            Misc.KeyValue kv = new Misc.KeyValue();
                                                            kv.Key = "inviter";
                                                            kv.Value = reqGiftByObj.partner;
                                                            keyValues.add(kv);

                                                            giftManager.createLocalGift("0" + reqGiftByObj.creator
                                                                    , resPromoRec.PER_TRAN_VALUE
                                                                    , giftType
                                                                    , tranId
                                                                    , resPromoRec.ADJUST_ACCOUNT
                                                                    , resPromoRec.DURATION
                                                                    , keyValues, new Handler<JsonObject>() {
                                                                @Override
                                                                public void handle(JsonObject jsonObject) {
                                                                    int err = jsonObject.getInteger("error", -1);
                                                                    long tranId = jsonObject.getInteger("tranId", -1);

                                                                    Gift gift = new Gift(jsonObject.getObject("gift"));

                                                                    log.add("amwaydesc", "tra thuong chuong trinh amway bang gift");
                                                                    log.add("err", err);

                                                                    if (err == 0) {
                                                                        vcbCmndRecs.incProCount(cmndObj.cardid, 1, new Handler<Boolean>() {
                                                                            @Override
                                                                            public void handle(Boolean aBoolean) {
                                                                            }
                                                                        });

                                                                        joReply.putString("desc", "Thành công");
                                                                        message.reply(joReply);

                                                                        VcbRecords.Obj vcbObj = new VcbRecords.Obj();
                                                                        vcbObj.vourcherid = gift.getModelId();
                                                                        vcbObj.number = reqGiftByObj.creator;
                                                                        vcbObj.bankcode = reqGiftByObj.bankCode;
                                                                        vcbObj.partner = reqGiftByObj.partner;
                                                                        vcbObj.tranid = tranId;
                                                                        vcbObj.bankcode = reqGiftByObj.bankCode;
                                                                        vcbObj.card_id = cmndObj.cardid;
                                                                        vcbObj.tranType = reqGiftByObj.tranType;

                                                                        vcbRecords.save(vcbObj, new Handler<VcbRecords.Obj>() {
                                                                            @Override
                                                                            public void handle(VcbRecords.Obj obj) {
                                                                            }
                                                                        });

                                                                        //String tranComment = "Bạn nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình “Liên kết tài khoản VCB - Nhận ngay quà tặng 100 ngàn.”";
                                                                        String tranComment = "Bạn nhận được Thẻ quà tặng trị giá 100.000 đ từ chương trình KM “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”. Trước khi dùng Thẻ để thanh toán, bấm “Sử dụng” để kích hoạt Thẻ. Lưu ý thời hạn hiệu lực của Thẻ.";
                                                                        String notiCaption = "Nhận thẻ quà tặng!";
                                                                        //  String notiBody = "Chúc mừng bạn nhận Thẻ quà tặng từ chương trình “Liên kết tài khoản VCB - Nhận ngay quà tặng 100 ngàn”. Vui lòng quay lại màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi”. Sau đó bạn chọn xem và kích hoạt Thẻ quà tặng bạn vừa nhận! LH: (08) 399 171 99";
                                                                        String notiBody = "Chúc mừng bạn nhận Thẻ quà tặng từ chương trình khuyến mãi đăng ký liên kết tài khoản với Vietcombank. Vui lòng về màn hình chính của ứng dụng Ví MoMo, nhấn chọn “Số tiền trong ví” để vào “Tài khoản của tôi”. Bạn bấm chọn vào Thẻ quà tặng để xem chi tiết. Trước khi dùng Thẻ quà tặng nào để thanh toán, bạn cần bấm “Sử dụng” để kích hoạt Thẻ đó! Chi tiết xin LH: 0839917199";
                                                                        //String giftMessage = "Bạn vừa nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình khuyến mãi “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”.”";
                                                                        String giftMessage = "Bạn vừa nhận được Thẻ quà tặng trị giá 100.000 đồng từ chương trình khuyến mãi “Liên kết tài khoản VCB- Nhận ngay quà tặng 100 ngàn”";


                                                                        Misc.sendTranHisAndNotiGift(vertx
                                                                                , reqGiftByObj.creator
                                                                                , tranComment
                                                                                , tranId
                                                                                , resPromoRec.PER_TRAN_VALUE
                                                                                , gift
                                                                                , notiCaption
                                                                                , notiBody
                                                                                , giftMessage
                                                                                , tranDb);
                                                                        log.writeLog();

                                                                    } else {

                                                                        joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                                                        message.reply(joReply);

                                                                        log.writeLog();
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            log.add("amwaydesc", "Tra qua chuong trinh amway loi");
                                                            log.writeLog();

                                                            joReply.putString("desc", "Lỗi " + SoapError.getDesc(error));
                                                            message.reply(joReply);

                                                        }
                                                    }
                                                });
                                            }

                                        }
                                    });

                                }
                            });
                        }
                    });
                }
            });
        }
    };

    private void hasCardIdRecord(ReqObj reqObj, final Handler<VcbCmndRecs.Obj> callback) {
        if (reqObj.hasCardId) {
            VcbCmndRecs.Obj obj = new VcbCmndRecs.Obj();
            obj.bankcode = reqObj.bankCode;
            obj.cardid = reqObj.cardId;
            obj.number = "0" + reqObj.creator;
            obj.promocount = reqObj.promoCount;
            callback.handle(obj);
            return;
        }
        vcbCmndRecs.findOne(reqObj.cardId, new Handler<VcbCmndRecs.Obj>() {
            @Override
            public void handle(VcbCmndRecs.Obj obj) {
                callback.handle(obj);
            }
        });
    }

    private void hasBankTran(ReqObj reqObj
            , PromotionDb.Obj promoRec
            , final Handler<Boolean> callback) {
        if (reqObj.hasBankTran) {
            callback.handle(true);
        } else {
            String bankCode = ("".equalsIgnoreCase(reqObj.bankCode) || reqObj.bankCode == null) ? "12345" : reqObj.bankCode;
            tranDb.findOneVcbTran(reqObj.creator
                    , bankCode
                    , promoRec.DATE_FROM
                    , promoRec.DATE_TO
                    , new Handler<TranObj>() {
                @Override
                public void handle(TranObj tranObj) {
                    boolean hasVcbTran = (tranObj == null ? false : true);
                    callback.handle(hasVcbTran);
                }
            });
        }
    }

    private void giveGift(final PromotionDb.Obj promotionRecord
            , final String giftTypeId
            , final ReqObj reqVcbObj
            , final PhonesDb.Obj phoneObj
            , final Common.BuildLog log, final Handler<Integer> cbGaveGiftOk) {

        //tao gift bang tien momo trong core
        giftManager.createGift(promotionRecord.ADJUST_ACCOUNT
                , promotionRecord.ADJUST_PIN
                , promotionRecord.PER_TRAN_VALUE
                , giftTypeId, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject result) {
                int error = result.getInteger("error");
                long tranId = result.getLong("tranId", 0);
                log.add("create Gift result", result.encodePrettily());

                //ket qua tao gift
                /*.putNumber("error", 0)
                .putNumber("tranId", coreReply.Tid)
                .putObject("gift", gift.toJsonObject())*/

                log.add("error", error);
                log.add("error desc", SoapError.getDesc(error));

                final ArrayList<Misc.KeyValue> keyValues = new ArrayList<>();
                keyValues.add(new Misc.KeyValue("tidref", String.valueOf(tranId)));

                final Gift gift = new Gift(result.getObject("gift"));

                //final GiftType giftType = new GiftType(result.getObject("giftType"));
                if (error == 0) {
                    log.add("*******", "vcb tao gift thanh cong");

                    gift.getExtra().putString("creator", "");

                    long partnerNumber = DataUtil.stringToVnPhoneNumber(phoneObj.inviter + "");

                    //invalid wallet
                    if(partnerNumber <=0){
                        gift.getExtra().putString("inviter",phoneObj.inviter);
                    }else{
                        gift.getExtra().putString("inviter","");
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, promotionRecord.DURATION);
                    gift.endDate = calendar.getTime();

                    giftDb.update(gift, false, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean event) {
                            log.add("update gift", event);

                            //chuyen gift tu tai khoan nay qua tai khoan khac
                            giftManager.adjustGift(promotionRecord.ADJUST_ACCOUNT
                                    , "0" + reqVcbObj.creator
                                    , gift.getModelId(), keyValues, new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject result) {
                                    int error = result.getInteger("error");
                                    final long tranId = System.currentTimeMillis(); //result.getLong("tranId", 0);

                                    if (error == 0) {
                                        log.add("******", "vcb chuyen gift thanh cong cho 0" + reqVcbObj.creator);
                                        final TranObj tran = new TranObj();
                                        long currentTime = System.currentTimeMillis();

                                        //TODO: Build mainObj
                                        tran.tranType = MomoProto.TranHisV1.TranType.GIFT_RECEIVE_VALUE;
                                        /*tran.comment = DataUtil.stringFormat("Bạn nhận được 01 món quà từ MoMo trị giá " + Misc.formatAmount(promoVcbRec.PER_TRAN_VALUE).replace(",",".") + "đ")
                                                .put("giftName", giftType.name)
                                                .toString();*/
                                        tran.comment = "Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank - Cùng nhận thưởng 100.000đ”";
                                        tran.tranId = tranId;
                                        tran.clientTime = currentTime;
                                        tran.ackTime = currentTime;
                                        tran.finishTime = currentTime;//=> this must be the time we sync, or user will not sync this to device
                                        tran.partnerName = "MoMo"; //
                                        tran.partnerId = "Chuyển tiền nhanh";
                                        tran.partnerRef = ""; // for avatar
                                        tran.amount = promotionRecord.PER_TRAN_VALUE;
                                        tran.status = TranObj.STATUS_OK;
                                        tran.error = 0;
                                        tran.cmdId = System.currentTimeMillis();
                                        //tran.parterCode = gift.typeId;
                                        tran.parterCode = tran.comment;
                                        tran.billId = gift.getModelId();
                                        tran.owner_number = reqVcbObj.creator;
                                        //mainObj.billId = gift.getModelId();
                                        tran.io = 1;
                                        tranDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean result) {
                                                if (!result) {
                                                    BroadcastHandler.sendOutSideTransSync(vertx, tran);

                                                    //TODO: Build noti
                                                    String str = "Bạn vừa nhận được thẻ quà tặng từ chương trình khuyến mãi. “Liên kết tài khoản Vietcombank...”";
                                                    final Notification noti = new Notification();
                                                    noti.priority = 2;
                                                    noti.type = MomoProto.NotificationType.NOTI_GIFT_RECEIVE_VALUE;
                                                    noti.caption = "Nhận thưởng quà khuyến mãi";
                                                    noti.body = "Bạn vừa nhận được thẻ quà tặng trị giá 100.000đ từ chương trình khuyến mãi “Liên kết tài khoản Vietcombank - Cùng nhận thưởng 100.000đ”. Vui lòng về màn hình chính của ứng dụng ví MoMo, nhấn vào “Số tiền trong ví”, bạn sẽ vào “Tài khoản của tôi” và thấy thẻ quà tặng bạn vừa nhận.";
                                                    noti.tranId = tranId;

                                                    noti.time = new Date().getTime();
                                                    noti.extra = new JsonObject()
                                                            .putString("giftId", gift.getModelId())
                                                            .putString("giftTypeId", "VCBPROMO_A")
                                                            .putString("amount", String.valueOf(promotionRecord.PER_TRAN_VALUE))
                                                            .putString("sender", "Chuyển tiền nhanh")
                                                            .putString("senderName", "MoMo")
                                                            .putString("msg", str)
                                                            .toString();

                                                    noti.receiverNumber = reqVcbObj.creator;
                                                    Misc.sendNoti(vertx, noti);

                                                }
                                            }
                                        });

                                        //todo save data for tracking
                                        VcbRecords.Obj vcbObj = new VcbRecords.Obj();
                                        vcbObj.vourcherid = gift.getModelId();
                                        vcbObj.number = reqVcbObj.creator;
                                        vcbObj.bankcode = reqVcbObj.bankCode;
                                        vcbObj.partner = reqVcbObj.partner;
                                        vcbObj.tranid = tranId;

                                        //save tracking info
                                        vcbRecords.save(vcbObj, new Handler<VcbRecords.Obj>() {
                                            @Override
                                            public void handle(VcbRecords.Obj obj) {
                                            }
                                        });

                                    } else {
                                        log.add("******", "vcb chuyen gift khong thanh cong");
                                        log.add("error", error);
                                        log.add("desc", SoapError.getDesc(error));
                                    }

                                    cbGaveGiftOk.handle(error);
                                    log.writeLog();
                                }
                            });
                        }
                    });
                } else {

                    cbGaveGiftOk.handle(error);
                    log.add("*******", "vcb Tao gift khong thanh cong thanh cong");
                    log.add("error", error);
                    log.add("desc", SoapError.getDesc(error));
                    log.writeLog();
                }
            }
        });
    }

    //BEGIN 0000000003 GIVE GIFT FOR VCB WITHOUT TIME
    private Handler<Message<JsonObject>> doPromotionGiftFor_B_withoutTime = new Handler<Message<JsonObject>>() {
        @Override
        public void handle(final Message<JsonObject> message) {

            final ReqObj reqGiftForBObj = new ReqObj(message.body());

            Promo.PromoReqObj reqPromotionVCBRec = new Promo.PromoReqObj();
            reqPromotionVCBRec.COMMAND = Promo.PromoType.GET_PROMOTION_REC;
            reqPromotionVCBRec.PROMO_NAME = "vcbpromo";

            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + reqGiftForBObj.creator);
            log.add("amount", reqGiftForBObj.amount);
            log.add("promotion name", reqPromotionVCBRec.PROMO_NAME);
            log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(reqGiftForBObj.tranType) == null ? "0" : MomoProto.TranHisV1.TranType.valueOf(reqGiftForBObj.tranType));
            log.add("tranid", reqGiftForBObj.tranId);

            final JsonObject joReply = new JsonObject();

            Misc.requestPromoRecord(vertx, reqPromotionVCBRec, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {
                    final PromotionDb.Obj resPromotionVCBRec = new PromotionDb.Obj(jsonObject);

                    //khong co chuong trinh khuyen mai vcbpromo
                    if (resPromotionVCBRec == null || resPromotionVCBRec.DATE_FROM == 0 || resPromotionVCBRec.DATE_TO == 0) {
                        joReply.putString("desc", "Không có chương trình khuyến mãi");
                        message.reply(joReply);
                        log.add("*******", "vcb khong co chuong trinh khuyen mai");
                        log.writeLog();
                        return;
                    }

                    //kiem tra con thoi gian khuyen mai khong
//                    long curTime = System.currentTimeMillis();
//                    if ((curTime < resPromotionVCBRec.DATE_FROM) || (curTime > resPromotionVCBRec.DATE_TO)) {
//
//                        joReply.putString("desc", "Hết chương trình khuyến mãi VCB");
//                        message.reply(joReply);
//
//                        log.add("from date", Misc.dateVNFormatWithTime(resPromotionVCBRec.DATE_FROM));
//                        log.add("to date", Misc.dateVNFormatWithTime(resPromotionVCBRec.DATE_TO));
//                        log.add("********", "vcb het thoi gian khuyen mai");
//                        log.writeLog();
//                        return;
//                    }

                    final Notification noti = new Notification();
                    noti.receiverNumber = reqGiftForBObj.creator;
                    noti.sms = "";
                    noti.tranId = System.currentTimeMillis(); // ban tren toan he thong
                    noti.priority = 2;
                    noti.time = System.currentTimeMillis();
                    noti.category = 0;

                    //check B side
                    phonesDb.getPhoneObjInfo(reqGiftForBObj.creator, new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(final PhonesDb.Obj phoneObjB) {
                            if (phoneObjB == null) {

                                joReply.putString("desc", "Không tìm thấy số điện thoại người được giới thiệu");
                                message.reply(joReply);

                                log.add("*********", "vcb khong ton tai 0" + reqGiftForBObj.creator + " tren he thong");
                                log.writeLog();
                                return;
                            }

                            if ("".equalsIgnoreCase(phoneObjB.inviter)
                                    || "momo".equalsIgnoreCase(phoneObjB.inviter)
                                    || (6100 <= DataUtil.strToInt(phoneObjB.inviter) && DataUtil.strToInt(phoneObjB.inviter) <= 6200)
                                    || (amwayCodeMin <= DataUtil.strToInt(phoneObjB.inviter) && DataUtil.strToInt(phoneObjB.inviter) <= amwayCodeMax)
                                    ) {

                                joReply.putString("desc", "Người giới thiệu " + phoneObjB.inviter + " không hợp lệ");
                                message.reply(joReply);
                                log.add("*********", "vcb nguoi gioi thieu" + phoneObjB.inviter + " khong hop le");
                                log.writeLog();
                                return;
                            }

                            if ("".equalsIgnoreCase(phoneObjB.bankPersonalId)) {
                                joReply.putString("desc", "Chưa có bank personal id từ ngân hàng");
                                message.reply(joReply);
                                log.add("*********", "vcb chua co bank personal id tu ngan hang");
                                log.writeLog();
                                return;

                            }

                            //kiem tra trong bang cmnd
                            vcbCmndRecs.findOne(phoneObjB.bankPersonalId, new Handler<VcbCmndRecs.Obj>() {
                                @Override
                                public void handle(final VcbCmndRecs.Obj cmndObj) {
                                    //da tra khuyen mai roi
                                    if (cmndObj != null && cmndObj.promocount > 0) {

                                        joReply.putString("desc", "Đã trả thưởng cho chứng minh nhân dân " + cmndObj.cardid);
                                        message.reply(joReply);

                                        log.add("*********", "vcb tai khoan 0" + phoneObjB.number + " da  tao gift cho cmnd");
                                        log.writeLog();
                                        return;
                                    } else if (cmndObj == null) {

                                        joReply.putString("desc", "Không có thông tin chứng minh nhân dân của 0" + phoneObjB.number);
                                        message.reply(joReply);
                                        //khong co thong tin trong bang cmnd
                                        log.add("*********", "vcb khong tim thay vi 0" + phoneObjB.number + " trong ban cmnd, co the map lai vi");
                                        log.writeLog();
                                        return;
                                    }
                                    log.add("existsMMPhone", "VCBVerticle");
                                    //check diem giao dich
                                    agentsDb.existsMMPhone("0" + reqGiftForBObj.creator,"VCBVerticle", new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean exists) {
                                            if (exists) {
                                                log.add("khong tra qua", "vi 0" + reqGiftForBObj.creator + " la diem giao dich");
                                                log.writeLog();
                                                return;
                                            }

                                            //tao va tang gift cho B
                                            String giftTypeB = "VCBPROMO_B"; // not transferable
                                            log.add("*********", "vcb tao va tang gift cho 0" + reqGiftForBObj.creator);
                                            giveGift(resPromotionVCBRec, giftTypeB, reqGiftForBObj, phoneObjB, log, new Handler<Integer>() {
                                                @Override
                                                public void handle(Integer ok) {

                                                    if (ok == 0) {
                                                        joReply.putString("desc", "Trả voucher cho 0" + reqGiftForBObj.creator + " thành công");
                                                        vcbCmndRecs.incProCount(cmndObj.cardid, 1, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {

                                                                //------------give gift for A.start-----------
                                                                final ReqObj reqGiftForAObj = new ReqObj();
                                                                reqGiftForAObj.creator = Integer.parseInt(DataUtil.stringToVnPhoneNumber(phoneObjB.inviter) + "");
                                                                reqGiftForAObj.partner = "0" + phoneObjB.number;

                                                                if (reqGiftForAObj.creator <= 0) {
                                                                    log.add("0_0", "so dien thoai khong hop le.");
                                                                    log.writeLog();
                                                                    return;
                                                                }

                                                                vcbRecords.countBy(reqGiftForAObj.creator, reqGiftForAObj.partner, new Handler<Integer>() {
                                                                    @Override
                                                                    public void handle(Integer count) {

                                                                        //da tra khuyen mai cho A khi B su dung qua chuong trinh VCB
                                                                        if (count > 0) {

                                                                            joReply.putNumber("err", -1);
                                                                            joReply.putString("desc", "Đã trả khuyến mãi cho A: 0" + reqGiftForAObj.creator + ", được nhận do B: " + reqGiftForAObj.partner + " sử dụng quà VCB thành công. Ngày nhận quà ");
                                                                            log.add("*********", "vcb da tra khuyen mai cho A = 0" + reqGiftForAObj.creator + " , duoc nhan do B = " + reqGiftForAObj.partner + " su dung gift");
                                                                            message.reply(joReply);
                                                                            log.writeLog();
                                                                            return;
                                                                        }

                                                                        //kiem tra ben A
                                                                        phonesDb.getPhoneObjInfo(reqGiftForAObj.creator, new Handler<PhonesDb.Obj>() {
                                                                            @Override
                                                                            public void handle(final PhonesDb.Obj phoneObjA) {
                                                                                if (phoneObjA == null) {

                                                                                    joReply.putNumber("err", -1);
                                                                                    joReply.putString("desc", "Không tồn tại số người giới thiệu trên hệ thống: 0" + reqGiftForAObj.creator);
                                                                                    message.reply(joReply);

                                                                                    log.add("*********", "vcb khong ton tai 0" + reqGiftForAObj.creator + " tren he thong");
                                                                                    log.writeLog();
                                                                                    return;
                                                                                }

                                                                                if (!"12345".equalsIgnoreCase(phoneObjA.bank_code)) {

                                                                                    joReply.putNumber("err", -1);
                                                                                    joReply.putString("desc", "Số người giới thiệu : 0" + reqGiftForAObj.creator + " chưa map ví VCB");
                                                                                    message.reply(joReply);

                                                                                    log.add("*********", "vcb " + reqGiftForAObj.creator + " chua map vi");
                                                                                    log.writeLog();
                                                                                    return;
                                                                                }
                                                                                log.add("existsMMPhone", "VCBVerticle");
                                                                                //kiem tra diem giao dich --> khong tra qua
                                                                                agentsDb.existsMMPhone("0" + reqGiftForAObj.creator,"VCBVerticle", new Handler<Boolean>() {
                                                                                    @Override
                                                                                    public void handle(Boolean exist) {
                                                                                        if (exist) {
                                                                                            log.add("khong tra qua", "vi 0" + reqGiftForAObj.creator + " la diem giao dich");
                                                                                            log.writeLog();
                                                                                            return;
                                                                                        }

                                                                                        //tra qua cho nguoi gioi thieu (ben A)
                                                                                        String giftTypeA = "VCBPROMO_A"; // not transferable
                                                                                        log.add("giftTypeA", giftTypeA);
                                                                                        log.add("*********", "vcb tao va tang gift cho A 0" + reqGiftForAObj);
                                                                                        giveGift(resPromotionVCBRec
                                                                                                , giftTypeA
                                                                                                , reqGiftForAObj
                                                                                                , phoneObjA
                                                                                                , log, new Handler<Integer>() {
                                                                                            @Override
                                                                                            public void handle(Integer error) {

                                                                                                //create and send gift to A success
                                                                                                if (error == 0) {
                                                                                                    //send noti to A
                                                                                                    final Notification noti = new Notification();
                                                                                                    noti.receiverNumber = reqGiftForAObj.creator;
                                                                                                    noti.caption = VcbNoti.UseVoucher.Cap.ARecieveVc;
                                                                                                    noti.body = String.format(VcbNoti.UseVoucher.Body.ARecieveVc, reqGiftForAObj.partner);

                                                                                                    noti.htmlBody = String.format(VcbNoti.UseVoucher.Body.ARecieveVc, reqGiftForAObj.partner);
                                                                                                    noti.sms = "";
                                                                                                    noti.tranId = System.currentTimeMillis(); // ban tren toan he thong
                                                                                                    noti.status = Notification.STATUS_DETAIL;
                                                                                                    noti.type = MomoProto.NotificationType.NOTI_STUDENT_VALUE;
                                                                                                    noti.priority = 2;
                                                                                                    noti.time = System.currentTimeMillis();
                                                                                                    noti.category = 0;

                                                                                                    Misc.sendNoti(vertx, noti);

                                                                                                    joReply.putNumber("err", 0);
                                                                                                    joReply.putString("desc", "Tặng quà cho : 0" + reqGiftForAObj.creator + " thành công");
                                                                                                    message.reply(joReply);
                                                                                                } else {

                                                                                                    joReply.putNumber("err", -1);
                                                                                                    joReply.putString("desc", "Tặng quà cho : 0" + reqGiftForAObj.creator + " không thành công");
                                                                                                    message.reply(joReply);

                                                                                                }
                                                                                            }
                                                                                        });

                                                                                    }
                                                                                });

                                                                            }
                                                                        });

                                                                    }
                                                                });

                                                                //give gift for A.end
                                                            }
                                                        });
                                                    } else {
                                                        joReply.putString("desc", "Trả voucher cho 0" + reqGiftForBObj.creator + " lỗi : " + SoapError.getDesc(ok));
                                                    }
                                                    message.reply(joReply);
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    };
    //END 0000000003 GIVE GIFT FOR VCB WITHOUT TIME




    public void start() {
        this.logger = getContainer().logger();
        this.glbCfg = container.config();
        this.eb = vertx.eventBus();
        this.vcbRecords = new VcbRecords(vertx.eventBus(),logger);
        this.phonesDb = new PhonesDb(vertx.eventBus(),logger);
        this.tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        this.giftDb = new GiftDb(vertx,logger);
        this.giftManager = new GiftManager(vertx,logger,glbCfg);
        this.vcbCmndRecs = new VcbCmndRecs(vertx.eventBus(),logger);
        this.glxDb = new PgGlxDb(vertx.eventBus(),logger);
        this.agentsDb = new AgentsDb(vertx.eventBus(),logger);

        JsonObject amwayPromo = glbCfg.getObject("amwaypromo",null);
        if(amwayPromo != null){
            amwayCodeMin = amwayPromo.getInteger("codemin",7000);
            amwayCodeMax = amwayPromo.getInteger("codemax",7009);
            amwayBonusValue = amwayPromo.getLong("bonusvalue", 20000);
        }

        Handler<Message<JsonObject>> vbcHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                ReqObj reqObj = new ReqObj(message.body());

                switch (reqObj.command){
                    case Command.req_create_gift_by_viettinbank_promo:
                        doPromotionViettinbank.handle(message);
                        break;
                    case Command.req_create_gift_for_B:
                        doPromotionGiftFor_B.handle(message);
                        break;
                    //BEGIN 0000000003 GIVE GIFT FOR VCB WITHOUT TIME
                    case Command.req_create_gift_for_B_without_time:
                        doPromotionGiftFor_B_withoutTime.handle(message);
                        break;
                    //END 0000000003 GIVE GIFT FOR VCB WITHOUT TIME
                    case Command.req_create_gift_for_A:
                        doPromotionGiftFor_A.handle(message);
                        break;
                    case Command.req_create_gift_by_momo:
                        doPromotionGiftMoMo.handle(message);
                        break;

                    case  Command.req_create_promo_by_pg:
                        doPromotionForPG.handle(message);
                        break;

                    //amway promotion
                    case Command.req_create_gift_by_amway_promo:
                        doPromotionGiftMoMoForAmway.handle(message);
                        break;

                    default:
                        logger.info("VcbVerticle not support for command " + reqObj.command);
                }
            }
        };
        eb.registerLocalHandler(AppConstant.VietCombak_Address, vbcHandler);
    }
}
