package com.mservice.momo.vertx;

import com.mservice.momo.data.*;
import com.mservice.momo.data.m2mpromotion.MerchantPromoTracksDb;
import com.mservice.momo.data.m2mpromotion.MerchantPromosDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.Promo.PromoReqObj;
import com.mservice.momo.data.model.Promo.PromoType;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.gateway.internal.db.mongo.MongoBase;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.SoapInProcess;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.CodeUtil;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.BroadcastHandler;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
import umarketscws.AdjustWalletResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by locnguyen on 18/08/2014.
 */
public class PromotionVerticle extends Verticle {
    private static ArrayList<Integer> TranTypeForChickenFeeding = new ArrayList<>();

    static {

        TranTypeForChickenFeeding.add(MomoProto.TranHisV1.TranType.TOP_UP_VALUE);
        TranTypeForChickenFeeding.add(MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE);
        TranTypeForChickenFeeding.add(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
    }

    private static ArrayList<Integer> AllowTran = new ArrayList<>();

    static {
        AllowTran.add(MomoProto.TranHisV1.TranType.TOP_UP_VALUE);
        AllowTran.add(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
        AllowTran.add(MomoProto.TranHisV1.TranType.PHIM123_VALUE);
        AllowTran.add(MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE);
    }

    private boolean allow_process_promotion = false;
    private EventBus eb;
    private Logger logger;
    private PromotionDb promotionDb;
    private PhonesDb phonesDb;
    private TransDb transDb;
    private MerchantPromosDb merchantPromosDb;
    private MerchantPromoTracksDb merchantPromoTracksDb;
    private CodeUtil codeUtilKNBB;
    private CodeUtil codeUtilM2M;
    private SoapInProcess mSoapProcessor;
    private SettingsDb settingsDb;
    private PromoTrackDb promoTrackDb;
    private ArrayList<PromotionDb.Obj> listPromos = new ArrayList<>();
    private HashMap<String, Integer> arrayChainPromo = new HashMap<>();

    @Override
    public void start() {
        container.logger().info("Start PromotionVerticle");
        JsonObject glbCfg = container.config();
        LoadCfg(glbCfg,container.logger());

        eb = vertx.eventBus();
        logger = container.logger();
        promotionDb = new PromotionDb(vertx.eventBus(),container.logger());
        promoTrackDb = new PromoTrackDb(vertx.eventBus(),logger);
        merchantPromosDb = new MerchantPromosDb(vertx.eventBus(),logger);
        merchantPromoTracksDb = new MerchantPromoTracksDb(vertx.eventBus(),logger);
        loadPromotion();

        phonesDb = new PhonesDb(vertx.eventBus(),container.logger());
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), container.logger(), container.config());
        mSoapProcessor = new SoapInProcess(container.logger(),glbCfg);
        settingsDb = new SettingsDb(vertx.eventBus(),logger);
        codeUtilKNBB = new CodeUtil(5,6);
        codeUtilM2M = new CodeUtil();

        allow_process_promotion = glbCfg.getBoolean("allow_process_promotion", false);

        JsonObject db_cfg =  glbCfg.getObject("lstandby_database");
        String Driver  = db_cfg.getString("driver");
        String Url = db_cfg.getString("url");
        String Username = db_cfg.getString("username");
        String Password = db_cfg.getString("password");

        loadProgramCfg(glbCfg);

        //dang ky event bus de chay khuyen mai
        Handler<Message> promoHandler = new Handler<Message>() {
            @Override
            public void handle(Message message) {
                JsonObject jo = (JsonObject)message.body();
                PromoReqObj reqObj = new PromoReqObj(jo);

                switch (reqObj.COMMAND){

                    case PromoType.CHICKEND_FEED:
                        doChickenFeed(message);
                        break;
                    case PromoType.INVITE_FRIEND_GEN_CODE:
                        doInviteFriendGenCode(message);
                        break;
                    case PromoType.INVITE_CREATE_CODE:
                        doCreateCodeForNumber(message);
                        break;
                    case PromoType.DO_PROMO_BY_CODE:
//                        doPromoFriend(message);
                        break;

                    //claim tien vao tai khoan khuyen mai tu code
                    case PromoType.CLAIM_POINT_BY_CODE:
                        //todo
//                        doPromoFriend(message);
                        break;

                    case PromoType.FORCE_PROMO_M2M_GEN_CODE:
//                        forcePromoM2MGenCode(message);
                        break;

                    case PromoType.PROMO_GET_LIST:
                        JsonArray jsonArray = null;
                        if(listPromos!=null && listPromos.size()>0){
                            jsonArray = new JsonArray();
                            for(PromotionDb.Obj o : listPromos){
                                jsonArray.add(o.toJsonObject());
                            }
                        }

                        JsonObject jsonObject = new JsonObject();
                        jsonObject.putArray("array",jsonArray);

                        message.reply(jsonObject);
                        break;

                    case PromoType.PROMO_GET_ACTIVE_LIST:
                        JsonArray jarrActivePromoList = null;
                        long currentTime = System.currentTimeMillis();
                        if(listPromos!=null && listPromos.size()>0){
                            jarrActivePromoList = new JsonArray();
                            for(PromotionDb.Obj o : listPromos){
                                if(o.DATE_FROM <= currentTime && o.DATE_TO >= currentTime)
                                {
                                    jarrActivePromoList.add(o.toJsonObject());
                                }
                            }
                        }
                        JsonObject jsonActivePromo = new JsonObject();
                        jsonActivePromo.putArray("array",jarrActivePromoList);

                        message.reply(jsonActivePromo);
                        break;

                    case PromoType.PROMO_DETAIL_BY_PROMO_ID:

                        JsonObject promoDetail  = null;
                        if(listPromos!=null && listPromos.size()>0){
                            for(PromotionDb.Obj o : listPromos){
                                if(reqObj.PROMO_ID.equalsIgnoreCase(o.ID)){

                                    if(System.currentTimeMillis() > o.DATE_TO){
                                        o.ACTIVE = false;
                                    }else{
                                        o.ACTIVE = true;
                                    }

                                    promoDetail = o.toJsonObject();
                                    break;
                                }
                            }
                        }
                        message.reply(promoDetail);
                        break;

                    case PromoType.PROMO_M2M_GEN_CODE:
//                        doPromoM2MGenCode(reqObj);
                        break;

                    case PromoType.PROMO_M2M_CLAIM_CODE:
//                        doM2MClaimCode(message);
                        break;

                    case PromoType.GET_PROMOTION_REC:
                        doGetPromoRec(message);
                        break;

                    default:
                        logger.info("PROMOTION not support for command: " + reqObj.COMMAND);
                        logger.info("json request: " + jo);
                        break;
                }
            }
        };

        //allow process promotion
        if (allow_process_promotion) {

            logger.info("Begin starting Promotion BUS");

            logger.info("BUS Name " + AppConstant.Promotion_ADDRESS);

            //eb.registerLocalHandler(AppConstant.Promotion_ADDRESS, promoHandler);

            logger.info("End starting Promotion BUS");
        }

        eb.registerLocalHandler(AppConstant.Promotion_ADDRESS, promoHandler);

        eb.registerHandler(AppConstant.Promotion_ADDRESS_UPDATE, new Handler<Message>() {
            @Override
            public void handle(Message message) {
                JsonObject jo = (JsonObject)message.body();
                PromoReqObj reqObj = new PromoReqObj(jo);
                logger.info("Request update json: " + reqObj.toJsonObject().encodePrettily());
                switch (reqObj.COMMAND){
                    //update cache

                    case PromoType.PROMO_UPDATE_DATA:
                        loadPromotion();
                        break;
                    default:
                        logger.info("PROMOTION not support for command: " + reqObj.COMMAND);
                        logger.info("json request: " + jo);
                }
                message.reply(message.body());
            }
        });
    }

    private void doGetPromoRec(Message message){

        final PromoReqObj reqObj = new PromoReqObj((JsonObject)message.body());
        String promoName = reqObj.PROMO_NAME.toLowerCase();
        PromotionDb.Obj obj = null;
        for (int i =0;i <listPromos.size();i++){
            obj = listPromos.get(i);
            if(promoName.equalsIgnoreCase(obj.NAME.toLowerCase().trim())){
                break;
            }
        }
        obj = (obj == null ? new PromotionDb.Obj() : obj);
        message.reply(obj.toJsonObject());
    }

    private int getMaxCodeByProgram(String program) {
        return (arrayChainPromo.containsKey(program) ? arrayChainPromo.get(program): 0);
    }

    private void loadProgramCfg(JsonObject glbCfg){
        JsonArray array = glbCfg.getArray("chain_promtion", null);
        arrayChainPromo.clear();
        if( array != null){
            for(int i =0;i< array.size();i++){
                JsonObject jo = array.get(i);
                arrayChainPromo.put(jo.getString("program",""), jo.getInteger("max_code",0));
            }
        }
    }

    ///
    //khuyen mai cho tat ca cac chuong trinh gui M2M neu co
    private void doPromoM2MGenCode(final PromoReqObj reqObj) {
        //kiem tra xem nguoi nhan co nam trong chuong trinh khuyen mai nao khong

        /*we only care about
        public String DEBITOR ="";//debitor  ==> nguoi chuyen tien
        public String CREDITOR=""; //creditor ==> nguoi nhan tien
        public long TIME = 0;
        public int TRAN_TYPE = -1; => must be M2M
        public long TRAN_ID = 0;
        public long TRAN_AMOUNT = 0;
        * */

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("" + reqObj.TRAN_ID);

        log.add("Kiem tra chuong trinh khuyen mai M2M cho TranID", reqObj.TRAN_ID);

        //lay thong tin ve nguoi nhan, phai nam trong list
        merchantPromosDb.findOneByNumberInList("0" + DataUtil.strToInt(reqObj.CREDITOR), new Handler<MerchantPromosDb.Obj>() {
            @Override
            public void handle(final MerchantPromosDb.Obj merchantProInfo) {
                if (merchantProInfo == null) {
                    log.add("Khong tim thay thong tin nguoi nhan trong bang khuyen mai", reqObj.CREDITOR);
                    log.writeLog();
                    return;
                } else {
                    //
                    log.add("So cua nguoi nhan nam trong chuong trinh khuyen mai", reqObj.CREDITOR);

                    log.add("Kiem tra so tien thanh toan phai lon hon", reqObj.TRAN_AMOUNT + ">=" + merchantProInfo.threshold + "vnd");

                    if (reqObj.TRAN_AMOUNT < merchantProInfo.threshold) {
                        log.add("Khong du dieu kien khuyen mai", reqObj.TRAN_AMOUNT + "<" + merchantProInfo.threshold + " vnd");
                        log.writeLog();
                        return;
                    }

                    log.add("Kiem tra thoi han thuc hien chuong trinh", "tu ngay " + merchantProInfo.fromDate.toString() + " den ngay " + merchantProInfo.toDate.toString());
                    long current = System.currentTimeMillis();
                    if (current < merchantProInfo.fromDate.getTime() || current > merchantProInfo.toDate.getTime()) {
                        log.add("Khong nam trong thoi gian khuyen mai", new Date(current).toString());
                        log.writeLog();
                        return;
                    }

                    log.add("Kiem tra tong so luong code duoc phat ra", "tong gia tri " + merchantProInfo.totalCode);
                    if (merchantProInfo.totalCode >= merchantProInfo.maxCode) {
                        log.add("Gioi han so luong code duoc khuyen mai la", merchantProInfo.maxCode);
                        log.writeLog();
                        return;
                    }

                    log.add("Kiem tra han muc thuc hien khuyen mai", "tong gia tri " + merchantProInfo.totalVal + " - gioi han " + merchantProInfo.maxVal);
                    if (merchantProInfo.totalVal >= merchantProInfo.maxVal) {
                        log.add("Da vuot qua gioi han tien khuyen mai", merchantProInfo.maxVal);
                        log.writeLog();
                        return;
                    }

                    log.add("Kiem tra tong so lan da nhan ma cua nguoi gui", reqObj.DEBITOR);
                    merchantPromoTracksDb.getListByClientProgram(reqObj.DEBITOR, merchantProInfo.program, new Handler<List<MerchantPromoTracksDb.Obj>>() {
                        @Override
                        public void handle(List<MerchantPromoTracksDb.Obj> codeList) {

                            int maxCode = getMaxCodeByProgram(merchantProInfo.program);

                            //dem tong so code theo quan nay
                            //todo config max code by program
                            if ((codeList.size() >= maxCode) || (maxCode == 0)) {
                                log.add("Da vuot qua gioi han so lan khuyen mai cho chuong trinh", "" + codeList.size() + " >= " + maxCode);
                                log.writeLog();
                                return;
                            } else {

                                JsonObject jo = Misc.getStartAndEndCurrentDateInMilliseconds();
                                long beginTime = jo.getLong(Misc.BeginDateInMilliSec, 0);

                                int countInDate = 0;
                                int drunkCount = 0;

                                for (int i = 0; i < codeList.size(); i++) {
                                    if (merchantProInfo.numList.contains(codeList.get(i).merchantNumber)) {
                                        drunkCount++;
                                        if (beginTime <= codeList.get(i).createTime.getTime()) {
                                            countInDate++;
                                        }
                                    }
                                }

                                if (drunkCount >= merchantProInfo.valList.size()) {
                                    log.add("Da vuot qua gioi han so lan khuyen mai cho quan nay", merchantProInfo.name +  " " + drunkCount + " >= " + merchantProInfo.valList.size());
                                    log.writeLog();
                                    return;
                                }
                                //kiem tra so luong code trong ngay khong vuot qua so lan quy dinh
                                if (merchantProInfo.maxTranPerDay <= countInDate) {
                                    log.add("Da vuot qua gioi han so lan khuyen mai trong 1 ngay", countInDate + ">=" + merchantProInfo.maxTranPerDay);
                                    log.writeLog();
                                    return;
                                }

                                //nam trong nhom cua quan thi khong khong nhan code
                                if(merchantProInfo.numList.contains("0" + DataUtil.strToInt(reqObj.DEBITOR))){
                                    String str = "[";
                                    for (int i =0; i < merchantProInfo.numList.size();i++){
                                        str += merchantProInfo.numList.get(i) + ",";
                                    }
                                    str = "0" + DataUtil.strToInt(reqObj.DEBITOR) + " in " + str + "]";
                                    log.add("So dien thoai nhan nam trong nhom so cua quan",str);
                                    log.writeLog();
                                    return;
                                }


                                final long val = merchantProInfo.valList.get(drunkCount);

                                if (merchantProInfo.totalVal + val > merchantProInfo.maxVal) {
                                    log.add("Da vuot qua gioi han tien khuyen mai", "" + merchantProInfo.totalVal + " + " + val + ">" + merchantProInfo.maxVal);
                                    log.writeLog();
                                    return;
                                }

                                //build the code
                                final String orgCode = codeUtilM2M.getNextCode();
                                final String code = "MOMO" + orgCode;

                                final String num_list = arrayList2String(merchantProInfo.numList);

                                merchantPromoTracksDb.insert(code, merchantProInfo.number, num_list, merchantProInfo.program, reqObj.DEBITOR, val, merchantProInfo.expireDay, reqObj.TRAN_ID, new Handler<MerchantPromoTracksDb.Obj>() {
                                    @Override
                                    public void handle(MerchantPromoTracksDb.Obj obj) {
                                        //its ok, no thing wrong
                                        if (MerchantPromoTracksDb.SUCCESS_CODE.equalsIgnoreCase(obj.claimError)) {
                                            Notification noti = new Notification();
                                            noti.receiverNumber = DataUtil.strToInt(reqObj.DEBITOR);
                                            noti.caption = "Cafe miễn phí cùng ví MoMo";

                                            //Mã MK: MoMo... (trị giá: ...đ) để sử dụng tại quán … từ ngày … - …/2014. Cảm ơn bạn đã sử dụng Ví MoMo.

                                            String tpl = "Mã MK: MoMo%s (trị giá: %sđ) để sử dụng tại quán %s từ ngày %s - %s/2014. Cảm ơn bạn đã sử dụng Ví MoMo.";
                                            String sotien = Misc.formatAmount(val).replace(",", ".");
                                            String tungay = Misc.dateFormatWithParten(System.currentTimeMillis(), "dd/MM");
                                            String denngay = Misc.dateFormatWithParten(System.currentTimeMillis() + merchantProInfo.expireDay * 24 * 60 * 60 * 1000L, "dd/MM");

                                            String content = String.format(tpl, orgCode, sotien, merchantProInfo.name, tungay, denngay);

                                            noti.body = content;
                                            noti.bodyIOS = content;
                                            noti.sms = Misc.removeAccent(content);
                                            noti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                                            noti.type = MomoProto.NotificationType.NOTI_SERVICE_ONE_BILL_VALUE;
                                            noti.priority = 2;
                                            noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                            noti.time = System.currentTimeMillis();
                                            noti.extra = new JsonObject().putString("serviceId","coffee_shop").toString();

                                            Misc.sendNoti(vertx, noti);
                                            log.add("Luu thanh cong - ban noti cho khach hang", noti.tranId);

                                            //cap nhat total code
                                            merchantPromosDb.increase(MerchantPromosDb.ColNames.TOTAL_CODE, merchantProInfo.number, 1, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean aBoolean) {
                                                    log.add("increase total code result", aBoolean);
                                                    log.writeLog();
                                                }
                                            });

                                        } else {
                                            //todo if error is related to code (can not save a duplicate code...) then we should do this again
                                            if (MongoBase.DUPLICATE_CODE_ERROR == DataUtil.strToInt(obj.claimError)) {
                                                vertx.setTimer(60 * 1000, new Handler<Long>() {
                                                    @Override
                                                    public void handle(Long aLong) {
//                                                        doPromoM2MGenCode(reqObj);
                                                    }
                                                });

                                                log.add("Bi trung code", code);
                                                log.writeLog();
                                                return;

                                            } else {
                                                log.add("Khong luu duoc code nay, bo qua", code);
                                                log.writeLog();
                                                return;
                                            }
                                        }
                                    }
                                });
                            }
                        }

                    });
                    //kiem tra xem nguoi gui da nhan duoc bao nhieu code tai quan nay
                }
            }
        });
    }

    private void forcePromoM2MGenCode(final Message message) {

        //kiem tra xem nguoi nhan co nam trong chuong trinh khuyen mai nao khong
        /*we only care about
        public String DEBITOR ="";//debitor  ==> nguoi chuyen tien
        public String CREDITOR=""; //creditor ==> nguoi nhan tien
        public long TIME = 0;
        public int TRAN_TYPE = -1; => must be M2M
        public long TRAN_ID = 0;
        public long TRAN_AMOUNT = 0;
        */

        final PromoReqObj reqObj = new PromoReqObj((JsonObject)message.body());

        final JsonObject jo = new JsonObject();
        jo.putNumber("error",0);
        jo.putString("desc","Thành công");

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("" + reqObj.TRAN_ID);

        log.add("Kiem tra chuong trinh khuyen mai M2M cho TranID", reqObj.TRAN_ID);

        //lay thong tin ve nguoi nhan, phai nam trong list
        merchantPromosDb.findOneByNumberInList("0" + DataUtil.strToInt(reqObj.CREDITOR), new Handler<MerchantPromosDb.Obj>() {
            @Override
            public void handle(final MerchantPromosDb.Obj merchantProInfo) {
                if (merchantProInfo == null) {
                    log.add("Khong tim thay thong tin nguoi nhan trong bang khuyen mai", reqObj.CREDITOR);
                    log.writeLog();

                    jo.putNumber("error",-1);
                    jo.putString("desc","Không tìm thấy thông tin chủ quán. Vui lòng kiểm tra lại");
                    message.reply(jo);
                    return;

                } else {
                    //
                    log.add("So cua nguoi nhan nam trong chuong trinh khuyen mai", reqObj.CREDITOR);

                    log.add("Kiem tra so tien thanh toan phai lon hon", reqObj.TRAN_AMOUNT + ">=" + merchantProInfo.threshold + "vnd");

                    if (reqObj.TRAN_AMOUNT < merchantProInfo.threshold) {
                        log.add("Khong du dieu kien khuyen mai", reqObj.TRAN_AMOUNT + "<" + merchantProInfo.threshold + " vnd");
                        log.writeLog();
                        return;
                    }

                    log.add("Kiem tra thoi han thuc hien chuong trinh", "tu ngay " + merchantProInfo.fromDate.toString() + " den ngay " + merchantProInfo.toDate.toString());
                    long current = System.currentTimeMillis();
                    if (current < merchantProInfo.fromDate.getTime() || current > merchantProInfo.toDate.getTime()) {
                        log.add("Khong nam trong thoi gian khuyen mai", new Date(current).toString());
                        log.writeLog();

                        jo.putNumber("error",-1);
                        jo.putString("desc","Không nằm trong thời gian khuyến mãi");
                        message.reply(jo);

                        return;
                    }

                    log.add("Kiem tra tong so luong code duoc phat ra", "tong gia tri " + merchantProInfo.totalCode);
                    if (merchantProInfo.totalCode >= merchantProInfo.maxCode) {
                        log.add("Gioi han so luong code duoc khuyen mai la", merchantProInfo.maxCode);
                        log.writeLog();

                        jo.putNumber("error",-1);
                        jo.putString("desc","Vượt số lượng code phát ra");
                        message.reply(jo);
                        return;
                    }

                    log.add("Kiem tra han muc thuc hien khuyen mai", "tong gia tri " + merchantProInfo.totalVal + " - gioi han " + merchantProInfo.maxVal);
                    if (merchantProInfo.totalVal >= merchantProInfo.maxVal) {
                        log.add("Da vuot qua gioi han tien khuyen mai", merchantProInfo.maxVal);
                        log.writeLog();

                        jo.putNumber("error",-1);
                        jo.putString("desc","Vượt giới hạn số tiền khuyến mãi - " + merchantProInfo.maxVal);
                        message.reply(jo);

                        return;
                    }

                    if (merchantProInfo.numList.contains("0" + DataUtil.strToInt(reqObj.DEBITOR))) {

                        String str = "[";
                        for(int i =0;i<merchantProInfo.numList.size(); i++){
                            str+= merchantProInfo.numList.get(i) + ",";
                        }
                        str += "]";

                        log.add("Nằm trong danh sách của quán cafe", reqObj.CREDITOR + " in "  + str);
                        log.writeLog();

                        jo.putNumber("error",-1);
                        jo.putString("desc","Nằm trong danh sách của quán cafe: " + reqObj.CREDITOR + " in "  + str);
                        message.reply(jo);
                        return;
                    }

                    log.add("Kiem tra tong so lan da nhan ma cua nguoi gui", reqObj.DEBITOR);
                    merchantPromoTracksDb.getListByClientProgram(reqObj.DEBITOR, merchantProInfo.program, new Handler<List<MerchantPromoTracksDb.Obj>>() {
                        @Override
                        public void handle(List<MerchantPromoTracksDb.Obj> codeList) {

                            int maxCode = getMaxCodeByProgram(merchantProInfo.program);

                            //dem tong so code theo quan nay
                            //todo config max code by program
                            if ((codeList.size() >= maxCode) || (maxCode == 0)) {
                                log.add("Da vuot qua gioi han so lan khuyen mai cho chuong trinh", "" + codeList.size() + " >= " + maxCode);
                                log.writeLog();

                                jo.putNumber("error",-1);
                                jo.putString("desc","Vượt giới hạn Khuyến mãi của chương trình: " + codeList.size() + " >= " + maxCode);
                                message.reply(jo);
                                return;

                            } else {

                                //todo tinh so lan uong cua quan may
                                int drunkCnt = 0;
                                for(int i=0;i<codeList.size();i++){
                                    if(merchantProInfo.numList.contains( "0" + DataUtil.strToInt(codeList.get(i).merchantNumber))){
                                        drunkCnt++;
                                    }
                                }

                                if( merchantProInfo.valList.size() <= drunkCnt){

                                    log.add("Vuot so lan khuyen mai cho quan", merchantProInfo.valList.size() + " <= " + codeList.size());
                                    log.writeLog();

                                    jo.putNumber("error",-1);
                                    jo.putString("desc","Vượt số lần khuyến mãi tối đa của quán " + merchantProInfo.valList.size() + " <= " + codeList.size());
                                    message.reply(jo);
                                    return;

                                }

                                //get value for this time
                                final long val = merchantProInfo.valList.get(drunkCnt);
                                if (merchantProInfo.totalVal + val > merchantProInfo.maxVal) {
                                    log.add("Da vuot qua gioi han tien khuyen mai", "" + merchantProInfo.totalVal + " + " + val + ">" + merchantProInfo.maxVal);
                                    log.writeLog();

                                    jo.putNumber("error",-1);
                                    jo.putString("desc","Vượt giới hạn tiền khuyến mãi của chương trình - " + merchantProInfo.maxVal);
                                    message.reply(jo);
                                    return;
                                }

                                //build the code
                                final String orgCode = codeUtilM2M.getNextCode();
                                final String code = "MOMO" + orgCode;

                                final String num_list = arrayList2String(merchantProInfo.numList);

                                merchantPromoTracksDb.insert(code, merchantProInfo.number, num_list, merchantProInfo.program, reqObj.DEBITOR, val, merchantProInfo.expireDay, reqObj.TRAN_ID, new Handler<MerchantPromoTracksDb.Obj>() {
                                    @Override
                                    public void handle(MerchantPromoTracksDb.Obj obj) {
                                        //its ok, no thing wrong
                                        if (MerchantPromoTracksDb.SUCCESS_CODE.equalsIgnoreCase(obj.claimError)) {
                                            Notification noti = new Notification();
                                            noti.receiverNumber = DataUtil.strToInt(reqObj.DEBITOR);
                                            noti.caption = "Cafe miễn phí cùng ví MoMo";

                                            //Mã MK: MoMo... (trị giá: ...đ) để sử dụng tại quán … từ ngày … - …/2014. Cảm ơn bạn đã sử dụng Ví MoMo.

                                            String tpl = "Mã MK: MoMo%s (trị giá: %sđ) để sử dụng tại quán %s từ ngày %s - %s/2014. Cảm ơn bạn đã sử dụng Ví MoMo.";
                                            String sotien = Misc.formatAmount(val).replace(",", ".");
                                            String tungay = Misc.dateFormatWithParten(System.currentTimeMillis(), "dd/MM");
                                            String denngay = Misc.dateFormatWithParten(System.currentTimeMillis() + merchantProInfo.expireDay * 24 * 60 * 60 * 1000L, "dd/MM");

                                            String content = String.format(tpl, orgCode, sotien, merchantProInfo.name, tungay, denngay);

                                            noti.body = content;
                                            noti.bodyIOS = content;
                                            noti.sms = Misc.removeAccent(content);
                                            noti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                                            noti.type = MomoProto.NotificationType.NOTI_SERVICE_ONE_BILL_VALUE;
                                            noti.priority = 2;
                                            noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                            noti.time = System.currentTimeMillis();
                                            noti.extra = new JsonObject().putString("serviceId","coffee_shop").toString();

                                            Misc.sendNoti(vertx, noti);
                                            log.add("Luu thanh cong - ban noti cho khach hang", noti.tranId);

                                            //cap nhat total code
                                            merchantPromosDb.increase(MerchantPromosDb.ColNames.TOTAL_CODE, merchantProInfo.number, 1, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean aBoolean) {
                                                    log.add("increase total code result", aBoolean);
                                                    log.writeLog();
                                                }
                                            });

                                            jo.putNumber("error",0);
                                            jo.putString("desc","Thành công");
                                            message.reply(jo);

                                        } else {
                                            //todo if error is related to code (can not save a duplicate code...) then we should do this again
                                            if (MongoBase.DUPLICATE_CODE_ERROR == DataUtil.strToInt(obj.claimError)) {
                                                vertx.setTimer(60 * 1000, new Handler<Long>() {
                                                    @Override
                                                    public void handle(Long aLong) {
//                                                        doPromoM2MGenCode(reqObj);
                                                    }
                                                });

                                                log.add("Bi trung code", code);
                                                log.writeLog();
                                                jo.putNumber("error",-1);
                                                jo.putString("desc","Bị trùng code");
                                                message.reply(jo);
                                                return;

                                            } else {
                                                log.add("Khong luu duoc code nay, bo qua", code);
                                                log.writeLog();

                                                jo.putNumber("error",-1);
                                                jo.putString("desc","Không lưu được code");
                                                message.reply(jo);
                                                return;
                                            }
                                        }
                                    }
                                });
                            }
                        }

                    });
                    //kiem tra xem nguoi gui da nhan duoc bao nhieu code tai quan nay
                }
            }
        });
    }

    private String arrayList2String(ArrayList<String> arrayList){
        String str = "";
        for(int i =0;i<arrayList.size();i++){
            str += arrayList.get(i) + ",";
        }
        if("".equalsIgnoreCase(str)){
            str = str.substring(0,str.length() - 1);
        }
        return str;
    }

    private void loadPromotion(){
        promotionDb.getPromotions(new Handler<ArrayList<PromotionDb.Obj>>() {
            @Override
            public void handle(ArrayList<PromotionDb.Obj> objs) {
                ArrayList<PromotionDb.Obj> tmpPromos = new ArrayList<>();
                if (objs != null && objs.size() > 0) {
                    for (int i = 0; i < objs.size(); i++) {
                        tmpPromos.add(objs.get(i));
                    }
                }

                if(tmpPromos.size() > 0){
                    ArrayList<PromotionDb.Obj> tmp = listPromos;
                    listPromos = tmpPromos;
                    if(tmp.size() >0){
                        tmp.clear();
                    }
                }
            }
        });
    }

    private void doClaimTKKM(final Message msg){
        //da pre-check o tren roi, neu code pass duoc checksum thi moi vao den day

        final PromoReqObj reqObj = new PromoReqObj((JsonObject)msg.body());
        reqObj.PROMO_CODE = reqObj.PROMO_CODE.toUpperCase();
        final String promoName = reqObj.PROMO_NAME.toLowerCase(); // tuong ung voi service id cua dich vu

        //tinh toan thuc hien gen code

        /*case 30001:"Bạn đã nhập sai mã khuyến mãi. Vui lòng nhập lại.";
        case 30002: "Mã khuyến mãi của bạn đã được sử dụng. Vui lòng sử dụng mã khuyến mãi khác.";
        break;
        case 30003: "Mã khuyến mãi của bạn đã hết hạn sử dụng. Vui lòng nhập mã khác.";
        30004 : khong co chuong trinh khuyen mai nao
        30005 : thuc hien top up khong thanh cong

        */

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(reqObj.CREATOR);
        log.add("function", "doClaimTKKM");
        log.add("promo code", reqObj.PROMO_CODE);
        log.add("promo name", reqObj.PROMO_NAME);

        //todo here : lay noi dung moi nhat trong khuyen mai tu memory
        PromotionDb.Obj promoObj = getPromoObj(promoName);

        final Promo.PromoResObj proRes = new Promo.PromoResObj();

        //khong co chuong trinh khuyen mai
        if(promoObj == null){
            log.add("promoObj", "null");
            proRes.RESULT =false;
            proRes.ERROR = -100;
            proRes.DESCRIPTION = "Không tìm thấy chương trình khuyến mãi trên hệ thống";
            msg.reply(proRes);
            return;
        }

        //todo claim tien tai day va tra ve ket qua

        //thanh cong thong bao ket qua va affect lai so du


    }

    private void doChickenFeed(Message message){

        return;

        /*JsonObject jo =(JsonObject)message.body();

        PromoReqObj reqObj = new PromoReqObj(jo);

        CoreCommon.BuildLog log = new CoreCommon.BuildLog(logger);

        log.setPhoneNumber(reqObj.PHONE_NUMBER);
        log.setTime(reqObj.TIME == 0 ? System.currentTimeMillis() : reqObj.TIME);
        log.add("function","doChickenFeed");
        log.add("chicken request",jo);
        log.add("request trantype", MomoProto.TranHisV1.TranType.valueOf(reqObj.TRAN_TYPE).name());
        log.add("tran id", reqObj.TRAN_ID);

        //co giao dich
        if(reqObj.TRAN_ID <=0){
            log.add("invalid tran_id","");
            log.writeLog();
            return;
        }

        //giao dich duoc khuyen mai
        if(!TranTypeForChickenFeeding.contains(reqObj.TRAN_TYPE)){
            log.add("loai giao dich khong duoc khuyen mai","");
            log.add("TranTypeForChickenFeeding size",TranTypeForChickenFeeding.size());
            log.writeLog();
            return;
        }

        doAdjustChickenFeed(reqObj.TRAN_ID, log);

        log.writeLog();*/
    }

    private void doCreateCodeForNumber(final Message msg){
        final PromoReqObj reqObj = new PromoReqObj((JsonObject)msg.body());
        final Common.BuildLog log = new Common.BuildLog(logger);
        final JsonObject jo = new JsonObject();

        //lay thong tin inviter
        phonesDb.getPhoneObjInfo(DataUtil.strToInt(reqObj.CREATOR), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(final PhonesDb.Obj phoneObj) {
                if (phoneObj == null) {
                    log.add("Khong tim thay thong tin inviter trong phone DB", "");
                    log.writeLog();

                    jo.putString("result", "Không tìm thấy tài khoản trên hệ thống");
                    msg.reply(jo);
                    return;
                }

                //tao code cho ca 2 nguoi va gui notification
                settingsDb.incAndGetLong("INVITE_FRIEND", +1L, new Handler<Long>() {
                    @Override
                    public void handle(final Long val) {

                        log.add("seed", val);

                        int orgSeed = DataUtil.strToInt(String.valueOf(val));

                        //tao code cho invitee
                        final String strCode = codeUtilKNBB.getNextCode();

                        log.add("seed for number", reqObj.CREATOR);
                        log.add("code for number", strCode);

                        Notification noti = new Notification();
                        noti.receiverNumber = DataUtil.strToInt(reqObj.CREATOR);
                        noti.caption = Itee.caption;
                        noti.body = String.format(Itee.body, strCode);
                        noti.bodyIOS = String.format(Itee.body, strCode);
                        noti.sms = String.format(Itee.sms, strCode);
                        noti.tranId = System.currentTimeMillis(); // tran id khi ban theo danh sach
                        noti.type = MomoProto.NotificationType.NOTI_SERVICE_ONE_BILL_VALUE;
                        noti.priority = 2;
                        noti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                        noti.time = System.currentTimeMillis();
                        noti.extra = new JsonObject().putString("serviceId","invitefriend").toString();


                        log.add("body for number", noti.body);
                        log.add("bodyIos for number", noti.bodyIOS);
                        log.add("sms for number", noti.sms);
                        //ban notification
                        Misc.sendNoti(vertx, noti);

                        //save lai code
                        PromoTrackDb.Obj trackObj = new PromoTrackDb.Obj();
                        trackObj.DESCRIPTION = "";
                        trackObj.NUMBER = DataUtil.strToInt(reqObj.CREATOR);
                        trackObj.PROMO_CODE = strCode;
                        trackObj.STATUS = PromoTrackDb.STATUS_NEW;
                        trackObj.ERROR = 0;
                        trackObj.TIME_VN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
                        trackObj.TIME = System.currentTimeMillis();
                        promoTrackDb.save(trackObj, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                log.add("save code for number", aBoolean);

                                //cap nhat so lan khuyen mai cua invitee len 1
                                if (aBoolean) {
                                    JsonObject joUpdate = new JsonObject();
                                    joUpdate.putNumber(colName.PhoneDBCols.NUMBER, DataUtil.strToInt(reqObj.CREATOR));
                                    joUpdate.putNumber(colName.PhoneDBCols.INVITEE_COUNT, 1);
                                    log.add("json update info", joUpdate.toString());

                                    jo.putString("result", "Mã khuyến mãi được tạo: " + strCode);

                                    phonesDb.updatePartialNoReturnObj(DataUtil.strToInt(reqObj.CREATOR)
                                            , joUpdate, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {
                                            log.writeLog();
                                        }
                                    });
                                } else {
                                    jo.putString("result", "Tạo mã khuyến mãi cho " + reqObj.CREATOR + " không thành công, Vui lòng thử lại");
                                    log.add("save promotrack", "không thành công");
                                    log.writeLog();
                                }

                                msg.reply(jo);
                            }
                        });
                    }
                });
            }
        });
    }

    private void doInviteFriendGenCode(final Message msg){

        final PromoReqObj reqObj = new PromoReqObj((JsonObject)msg.body());

        final JsonObject joReply = new JsonObject();

        //tinh toan thuc hien gen code
        final Common.BuildLog prelog = new Common.BuildLog(logger);
        prelog.setPhoneNumber(reqObj.CREATOR);
        prelog.add("begin process gen code with creator", reqObj.CREATOR);
        prelog.add("function", "doInviteFriendGenCode");
        prelog.add("tran type", MomoProto.TranHisV1.TranType.valueOf(reqObj.TRAN_TYPE));
        prelog.add("amount", reqObj.TRAN_AMOUNT);
        prelog.writeLog();

        /*phonesDb.getPhoneObjInfo(DataUtil.strToInt(reqObj.CREATOR),new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if(obj == null || (obj.isInviter ==false)){
                    prelog.add("desc", reqObj.CREATOR + " khong nam trong danh sach khuyen mai");
                    prelog.writeLog();
                    return;
                }
            }
        });*/

//        executeInviteFriendGenCode(msg, reqObj, joReply, prelog);
    }

    private void executeInviteFriendGenCode(final Message msg, final PromoReqObj reqObj, final JsonObject joReply, Common.BuildLog prelog) {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(reqObj.CREATOR);
        log.setTime(prelog.getTime());

        if(!AllowTran.contains(reqObj.TRAN_TYPE)){

            //reply for caller
            joReply.putString("desc","Loại giao dịch không được khuyến mãi");
            msg.reply(joReply);

            log.add("loai giao dich khong duoc khuyen mai","");
            log.add("request trantype", MomoProto.TranHisV1.TranType.valueOf(reqObj.TRAN_TYPE).name());
            log.add("list trantype to get promotion","----------------");
            for(int i : AllowTran){
                log.add("trantype", MomoProto.TranHisV1.TranType.valueOf(i).name());
            }
            log.writeLog();
            return;
        }

        //todo here : lay noi dung moi nhat trong khuyen mai tu memory
        PromotionDb.Obj promoObj = null;
        if(listPromos.size()>0){
            promoObj= listPromos.get(0);
        }

        //khong co chuong trinh khuyen mai nao tren he thong
        if(promoObj == null){

            //reply for caller
            joReply.putString("desc","Không có chương trình khuyến mãi nào trên hệ thống");
            msg.reply(joReply);

            log.add("Không có đợt khuyến mãi nào","");
            log.writeLog();
            return;
        }

        //gia tri giao dich khong thoa
        if (reqObj.TRAN_AMOUNT < promoObj.TRAN_MIN_VALUE){

            //reply for caller
            joReply.putString("desc","giá trị giao dịch <= giá trị tối thiểu " + reqObj.TRAN_AMOUNT + "<=" + promoObj.TRAN_MIN_VALUE);
            msg.reply(joReply);
            log.add("giá trị giao dịch <= giá trị tối thiểu",reqObj.TRAN_AMOUNT + "<=" + promoObj.TRAN_MIN_VALUE);
            log.writeLog();
            return;
        }

        final PromotionDb.Obj fPromoObj =promoObj;

        //lay thong tin invitee
        phonesDb.getPhoneObjInfo(DataUtil.strToInt(reqObj.CREATOR), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(final PhonesDb.Obj recverObj) {

                //kiem tra xem, nguoi gioi thieu co vi hay khong
                if(recverObj == null){

                    //reply for caller
                    joReply.putString("desc","Người tạo giao dịch không có trên hệ thống");
                    msg.reply(joReply);

                    log.add("desc","Khong tim thay vi cua nguoi duoc gioi thieu");
                    log.writeLog();
                    return;
                }

                //todo khong co token thi khong gen code
                /*log.add("os", recverObj.phoneOs);
                log.add("token", recverObj.pushToken);

                if("".equalsIgnoreCase(recverObj.pushToken)){

                    joReply.putString("desc","Người tạo giao dịch không có trên hệ thống, Device không có token");
                    msg.reply(joReply);
                    log.add("errDesc", "device khong co token");
                    log.writeLog();
                    return;
                }*/

                /*if(recverObj.createdDate < fPromoObj.DATE_FROM  || recverObj.createdDate > fPromoObj.DATE_TO){

                    //reply for caller
                    joReply.putString("desc","Ngày đăng ký mới không nằm trong ngày khuyến mãi");
                    msg.reply(joReply);

                    log.add("registed date", Misc.dateVNFormatWithTime(recverObj.createdDate) );
                    log.add("from date", Misc.dateVNFormatWithTime(fPromoObj.DATE_FROM));
                    log.add("to date", Misc.dateVNFormatWithTime(fPromoObj.DATE_TO));
                    log.add("expired promotion","");
                    log.writeLog();
                    return;
                }*/

                //neu thang nay khong co nguoi gioi thieu, khong duoc nhan khuyen mai
                if(recverObj.referenceNumber == 0){

                    //reply for caller
                    joReply.putString("desc","Không có người giới thiệu");
                    msg.reply(joReply);

                    log.add("Khong co nguoi gioi thieu", "khong co tien khuyen mai");
                    log.writeLog();
                    return;
                }

                //tu gioi thieu
                if(recverObj.referenceNumber == DataUtil.strToInt(reqObj.CREATOR)){

                    //reply for caller
                    joReply.putString("desc","Tự giới thiệu cho chính mình");
                    msg.reply(joReply);

                    log.add("reference number", "0" + recverObj.referenceNumber);
                    log.add("number", reqObj.CREATOR);
                    log.add("sao chu lai cheat anh vay","");
                    log.writeLog();
                    return;
                }

                //invitee da su dung het quyen khuyen mai
                if( (fPromoObj.MIN_TIMES > 0) && (recverObj.inviteeCount >= fPromoObj.MIN_TIMES)){

                    //gui lai code neu chua duoc khuyen mai
                    if(reqObj.RESEND){

                        promoTrackDb.getByNumber(DataUtil.strToInt(reqObj.CREATOR), new Handler<ArrayList<PromoTrackDb.Obj>>() {
                            @Override
                            public void handle(ArrayList<PromoTrackDb.Obj> objs) {

                                String strDesc = "";

                                if(objs != null && objs.size() > 0){
                                    for(int i = 0; i < objs.size(); i++){

                                        PromoTrackDb.Obj o = objs.get(i);

                                        if(o.STATUS == PromoTrackDb.STATUS_NEW){

                                            Notification resendNoti = new Notification();
                                            resendNoti.receiverNumber = objs.get(i).NUMBER;
                                            resendNoti.caption = Itee.caption;
                                            resendNoti.body = String.format(Itee.body,objs.get(i).PROMO_CODE);
                                            resendNoti.bodyIOS = String.format(Itee.body,objs.get(i).PROMO_CODE);
                                            resendNoti.sms = String.format(Itee.sms, objs.get(i).PROMO_CODE);
                                            resendNoti.tranId= System.currentTimeMillis(); // tran id khi ban theo danh sach
                                            resendNoti.type = MomoProto.NotificationType.NOTI_SERVICE_ONE_BILL_VALUE;
                                            resendNoti.priority = 2;
                                            resendNoti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                            resendNoti.time = System.currentTimeMillis();
                                            resendNoti.extra = new JsonObject().putString("serviceId","invitefriend").toString();

                                            //ban notification
                                            Misc.sendNoti(vertx, resendNoti);
                                            strDesc += "Gửi lại code cho " + objs.get(i).NUMBER + "[" + objs.get(i).PROMO_CODE + "]<br/>";
                                        }else{
                                            if(o.STATUS == PromoTrackDb.STATUS_PROMOTED){
                                                strDesc+= "Đã nhận khuyến mãi, Mã " + o.PROMO_CODE +  " Người tạo 0" + o.NUMBER + ", người nhận 0" + o.EXEC_NUMBER  +" lúc " + o.EXEC_TIME + "</br>";
                                            }
                                            if(o.STATUS == PromoTrackDb.STATUS_LOCKED){
                                                strDesc+= "Đang tạm khóa, Mã " + o.PROMO_CODE +  " Người tạo 0" + o.NUMBER + ", người nhận 0" + o.EXEC_NUMBER  +" lúc " + o.EXEC_TIME + "</br>";
                                            }

                                            if(o.STATUS == PromoTrackDb.STATUS_EXPIRED){
                                                strDesc+= "Đã hết hạn, Mã " + o.PROMO_CODE +  " Người tạo 0" + o.NUMBER + ", người nhận 0" + o.EXEC_NUMBER  +" lúc " + o.EXEC_TIME + "</br>";
                                            }
                                        }
                                    }

                                    joReply.putString("desc", strDesc);
                                    msg.reply(joReply);

                                }
                                else {

                                    promoTrackDb.getAllByNumber(DataUtil.strToInt(reqObj.CREATOR),new Handler<ArrayList<PromoTrackDb.Obj>>() {
                                        @Override
                                        public void handle(ArrayList<PromoTrackDb.Obj> objs) {

                                            String strInfo = "";

                                            //co tao ma
                                            if(objs != null && objs.size() >0){
                                                for(int i=0;i<objs.size();i++){
                                                    PromoTrackDb.Obj o = objs.get(i);
                                                    if(o.STATUS == PromoTrackDb.STATUS_NEW){
                                                        Notification resendNoti = new Notification();
                                                        resendNoti.receiverNumber = objs.get(i).NUMBER;
                                                        resendNoti.caption = Itee.caption;
                                                        resendNoti.body = String.format(Itee.body,objs.get(i).PROMO_CODE);
                                                        resendNoti.bodyIOS = String.format(Itee.body,objs.get(i).PROMO_CODE);
                                                        resendNoti.sms = String.format(Itee.sms, objs.get(i).PROMO_CODE);
                                                        resendNoti.tranId= System.currentTimeMillis(); // tran id khi ban theo danh sach
                                                        resendNoti.type = MomoProto.NotificationType.NOTI_SERVICE_ONE_BILL_VALUE;
                                                        resendNoti.priority = 2;
                                                        resendNoti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                                                        resendNoti.time = System.currentTimeMillis();
                                                        resendNoti.extra = new JsonObject().putString("serviceId","invitefriend").toString();
                                                        //ban notification
                                                        Misc.sendNoti(vertx, resendNoti);
                                                        strInfo += "Gửi lại code cho " + objs.get(i).NUMBER + "[" + objs.get(i).PROMO_CODE + "]<br/>";
                                                    }else{
                                                        if(o.STATUS == PromoTrackDb.STATUS_PROMOTED){
                                                            strInfo+= "Đã nhận khuyến mãi, Mã " + o.PROMO_CODE +  " Người tạo 0" + o.NUMBER + ", người nhận 0" + o.EXEC_NUMBER  +" lúc " + o.EXEC_TIME + "</br>";
                                                        }
                                                        if(o.STATUS == PromoTrackDb.STATUS_LOCKED){
                                                            strInfo+= "Đang tạm khóa, Mã " + o.PROMO_CODE +  " Người tạo 0" + o.NUMBER + ", người nhận 0" + o.EXEC_NUMBER  +" lúc " + o.EXEC_TIME + "</br>";
                                                        }

                                                        if(o.STATUS == PromoTrackDb.STATUS_EXPIRED){
                                                            strInfo+= "Đã hết hạn, Mã " + o.PROMO_CODE +  " Người tạo 0" + o.NUMBER + ", người nhận 0" + o.EXEC_NUMBER  +" lúc " + o.EXEC_TIME + "</br>";
                                                        }
                                                    }
                                                }
                                            }else{
                                                strInfo = "Không tìm thấy code để gửi lại";
                                            }

                                            joReply.putString("desc", strInfo);
                                            msg.reply(joReply);
                                        }
                                    });
                                }
                            }
                        });
                    }else {
                        //reply for caller
                        joReply.putString("desc","Người được giới thiệu đã sử dụng hết số lần khuyến mãi");
                        msg.reply(joReply);
                    }

                    log.add("NDGThieu su dung het so lan khuyen mai", recverObj.inviteeCount + " >= " + fPromoObj.MIN_TIMES);
                    log.writeLog();
                    return;
                }

                //qua 7 ngay ke tu ngay dang ky
                if(System.currentTimeMillis() > (recverObj.createdDate + fPromoObj.DURATION_TRAN*24*60*60*1000L)){

                    //reply for caller
                    joReply.putString("desc","Quá " + fPromoObj.DURATION_TRAN + " " + "kể từ ngày đăng ký");
                    msg.reply(joReply);
                    log.add("qua thoi han khuyen mai",Misc.dateVNFormatWithTime(recverObj.createdDate) + " " + fPromoObj.DURATION_TRAN +"<"+ Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                    log.writeLog();
                    return;
                }

                //lay thong tin inviter
                phonesDb.getPhoneObjInfo(recverObj.referenceNumber,new Handler<PhonesDb.Obj>() {
                    @Override
                    public void handle(final PhonesDb.Obj senderObj) {
                        if(senderObj == null){

                            joReply.putString("desc","Người giới thiệu không có trên hệ thống");
                            msg.reply(joReply);

                            log.add("Khong tim thay thong tin NGThieu trong phone DB","");
                            log.writeLog();
                            return;
                        }

                        if(senderObj.isInviter == false){
                            joReply.putString("desc","Người giới thiệu không nằm trong chương trình khuyến mãi");
                            msg.reply(joReply);

                            log.add("NGThieu khong nam trong chuong trinh khuyen mai","Tranh chit --> khong sinh code cho nguoi NDGThieu");

                            JsonObject jo = new JsonObject();
                            jo.putBoolean(colName.PhoneDBCols.IS_INVITER,false);
                            log.add("cap nhat PhoneDb","khong cho nguoi DGThieu gioi thieu nguoi khach");

                            phonesDb.updatePartialNoReturnObj(DataUtil.strToInt(reqObj.CREATOR),jo,new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {}
                            });

                            log.writeLog();
                            return;
                        }

                        //inviter duoc tao sau invitee
                        /*if((recverObj.createdDate < senderObj.createdDate) && senderObj.createdDate > 0){

                            joReply.putString("desc","NDGthieu tao truoc NGThieu");
                            msg.reply(joReply);

                            log.add("Inviter duoc tao sau Invitee","");
                            log.writeLog();
                            return;
                        }*/

                        final long time = System.currentTimeMillis();

                        //tao code cho invitee
                        final String iteeCode = codeUtilKNBB.getNextCode();

                        log.add("code for invitee",iteeCode);

                        Notification ndgtNoti = new Notification();
                        ndgtNoti.receiverNumber = DataUtil.strToInt(reqObj.CREATOR);
                        ndgtNoti.caption = Itee.caption;
                        ndgtNoti.body = String.format(Itee.body,iteeCode);
                        ndgtNoti.bodyIOS = String.format(Itee.body,iteeCode);
                        ndgtNoti.sms = String.format(Itee.sms, iteeCode);
                        ndgtNoti.tranId= System.currentTimeMillis(); // tran id khi ban theo danh sach
                        ndgtNoti.type = MomoProto.NotificationType.NOTI_SERVICE_ONE_BILL_VALUE;
                        ndgtNoti.priority = 2;
                        ndgtNoti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                        ndgtNoti.time = System.currentTimeMillis();
                        ndgtNoti.extra = new JsonObject().putString("serviceId","invitefriend").toString();

                        log.add("body for invitee",ndgtNoti.body);
                        log.add("bodyIos for invitee",ndgtNoti.bodyIOS);
                        log.add("sms for invitee",ndgtNoti.sms);
                        //ban notification
                        Misc.sendNoti(vertx, ndgtNoti);

                        //save lai code
                        PromoTrackDb.Obj ndgtObj = new PromoTrackDb.Obj();
                        ndgtObj.DESCRIPTION="";
                        ndgtObj.NUMBER = DataUtil.strToInt(reqObj.CREATOR);
                        ndgtObj.PROMO_CODE = iteeCode;
                        ndgtObj.STATUS = PromoTrackDb.STATUS_NEW;
                        ndgtObj.ERROR = 0;
                        ndgtObj.TIME_VN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
                        ndgtObj.TIME =  time;
                        ndgtObj.PARTNER = DataUtil.strToInt(reqObj.CREATOR);

                        //tao code moi cho nguoi duoc gioi thieu
                        joReply.putString("desc", "Tạo code cho được giới thiệu "+reqObj.CREATOR+" OK ");

                        promoTrackDb.save(ndgtObj, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                log.add("save code for invitee", aBoolean);

                                //cap nhat so lan khuyen mai cua invitee len 1
                                if (aBoolean) {
                                    JsonObject recverJo = new JsonObject();
                                    recverJo.putNumber(colName.PhoneDBCols.NUMBER, DataUtil.strToInt(reqObj.CREATOR));
                                    recverJo.putNumber(colName.PhoneDBCols.INVITEE_COUNT, recverObj.inviteeCount + 1);
                                    log.add("json update initee info", recverJo.toString());

                                    phonesDb.updatePartialNoReturnObj(DataUtil.strToInt(reqObj.CREATOR)
                                            , recverJo, new Handler<Boolean>() {
                                        @Override
                                        public void handle(Boolean aBoolean) {

                                        }
                                    });
                                }
                            }
                        });

                        log.add("max times", fPromoObj.MAX_TIMES);
                        log.add("senderObj.inviterCount",senderObj.inviterCount);

                        final String iterCode = codeUtilKNBB.getNextCode();

                        //gioi han so lan nhan thuong cua inviter
                        if(fPromoObj.MAX_TIMES < 0 || senderObj.inviterCount < fPromoObj.MAX_TIMES){
                            //tao code cho inviter
                            //for inviter

                            log.add("code for inviteer", iterCode);

                            //tao noti cho inviter
                            Notification nguoiGioiThieuNoti = new Notification();
                            nguoiGioiThieuNoti.receiverNumber = senderObj.number;
                            nguoiGioiThieuNoti.caption = Iter.caption;
                            nguoiGioiThieuNoti.body = String.format(Iter.body,iterCode,reqObj.CREATOR);
                            nguoiGioiThieuNoti.bodyIOS = String.format(Iter.body,iterCode,reqObj.CREATOR);
                            nguoiGioiThieuNoti.sms = String.format(Iter.sms, reqObj.CREATOR,iterCode);
                            nguoiGioiThieuNoti.tranId= System.currentTimeMillis(); // tran id khi ban theo danh sach
                            nguoiGioiThieuNoti.type = MomoProto.NotificationType.NOTI_SERVICE_ONE_BILL_VALUE;
                            nguoiGioiThieuNoti.priority = 2;
                            nguoiGioiThieuNoti.status = Notification.STATUS_DETAIL; // cho phep hien thi khi sync du lieu
                            nguoiGioiThieuNoti.time = System.currentTimeMillis();
                            nguoiGioiThieuNoti.extra = new JsonObject().putString("serviceId","invitefriend").toString();
                            log.add("body for inviter", nguoiGioiThieuNoti.body);
                            log.add("body for inviter", nguoiGioiThieuNoti.bodyIOS);
                            log.add("sms for inviter", nguoiGioiThieuNoti.sms);

                            //ban notification
                            Misc.sendNoti(vertx, nguoiGioiThieuNoti);

                            joReply.putString("descsender", "Tạo code cho giới thiệu 0"+senderObj.number+" OK ");

                            //todo: luu lai code
                            PromoTrackDb.Obj nguoiGioiThieuObj = new PromoTrackDb.Obj();
                            nguoiGioiThieuObj.DESCRIPTION="";
                            nguoiGioiThieuObj.NUMBER = senderObj.number;
                            nguoiGioiThieuObj.PROMO_CODE = iterCode;
                            nguoiGioiThieuObj.STATUS = PromoTrackDb.STATUS_NEW;
                            nguoiGioiThieuObj.ERROR = 0;
                            nguoiGioiThieuObj.TIME_VN = Misc.dateVNFormatWithTime(System.currentTimeMillis());
                            nguoiGioiThieuObj.TIME = time;
                            nguoiGioiThieuObj.PARTNER = DataUtil.strToInt(reqObj.CREATOR);
                            promoTrackDb.save(nguoiGioiThieuObj,new Handler<Boolean>() {
                                @Override
                                public void handle(Boolean aBoolean) {
                                    log.add("save code for inviter", aBoolean);

                                    //cap nhat so lan khuyen mai cua inviter len 1
                                    if(aBoolean){
                                        JsonObject inviterJo = new JsonObject();
                                        inviterJo.putNumber(colName.PhoneDBCols.NUMBER,senderObj.number);
                                        inviterJo.putNumber(colName.PhoneDBCols.INVITER_COUNT,senderObj.inviterCount +1);
                                        log.add("json update inviter info", inviterJo.toString());

                                        phonesDb.updatePartialNoReturnObj(senderObj.number
                                                ,inviterJo,new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {

                                            }
                                        });
                                    }
                                }
                            });
                        }else{
                            joReply.putString("descsender", "Không tạo mã khuyến mãi cho người giới thiệu 0"+senderObj.number+" OK ");

                            log.add("Khong khuyen mai cho inviter","0" +senderObj.number);

                        }
                    }
                });
            }
        });
    }

    private void doPromoFriend(final Message msg){
        final PromoReqObj reqObj = new PromoReqObj((JsonObject)msg.body());
        reqObj.PROMO_CODE = reqObj.PROMO_CODE.toUpperCase();
        //tinh toan thuc hien gen code
        /*case 30001:"Bạn đã nhập sai mã khuyến mãi. Vui lòng nhập lại.";
        case 30002: "Mã khuyến mãi của bạn đã được sử dụng. Vui lòng sử dụng mã khuyến mãi khác.";
        break;
        case 30003: "Mã khuyến mãi của bạn đã hết hạn sử dụng. Vui lòng nhập mã khác.";
        30004 : khong co chuong trinh khuyen mai nao
        30005 : thuc hien top up khong thanh cong

        */

        logger.info("begin process claim code for " + reqObj.CREATOR);

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(reqObj.CREATOR);
        log.add("function", "doPromoFriend");
        log.add("promo code", reqObj.PROMO_CODE);
        String promoName = reqObj.PROMO_NAME.toLowerCase();

        //todo here : lay noi dung moi nhat trong khuyen mai tu memory
        //PromotionDb.Obj promoObj = getPromoObj(promoName);

        PromotionDb.Obj promoObj = null;
        if(listPromos != null && listPromos.size() > 0){
            promoObj = listPromos.get(0);
        }

        final Promo.PromoResObj proRes = new Promo.PromoResObj();

        //khong co chuong trinh khuyen mai nao
        if(promoObj == null){
            proRes.DESCRIPTION = "Hệ thống chúng tôi hiện không có chương trình khuyến mãi nào.";
            proRes.RESULT = false;
            proRes.ERROR = 30004;
            msg.reply(proRes.toJsonObject());
            log.add("execute promo result", proRes.toJsonObject());
            log.writeLog();
            return;
        }

        if("".equalsIgnoreCase(reqObj.PROMO_CODE)){
            proRes.DESCRIPTION = "Hệ thống chúng tôi hiện không có chương trình khuyến mãi nào.";
            proRes.RESULT = false;
            proRes.ERROR = 30004;
            msg.reply(proRes.toJsonObject());
            log.add("execute promo result", proRes.toJsonObject());
            log.writeLog();
            return;
        }

        //tien xu ly ma code nhan khuyen mai
        if(!reqObj.PROMO_CODE.startsWith("MOMO")){
            proRes.DESCRIPTION = "Bạn đã nhập sai mã khuyến mãi. Vui lòng nhập lại.";
            proRes.RESULT = false;
            proRes.ERROR = 30001;
            msg.reply(proRes.toJsonObject());
            log.add("execute promo result", proRes.toJsonObject());
            log.writeLog();
            return;
        }

        final String actualCode = (reqObj.PROMO_CODE.length() >=6 ? reqObj.PROMO_CODE.substring(reqObj.PROMO_CODE.length() - 6) : "");
        log.add("actual code", actualCode);

        final  PromotionDb.Obj fPromoObj = promoObj;

        log.add("promoTrackDb.getOne with code", actualCode);

        String strhh =  Misc.dateFormatWithParten(System.currentTimeMillis(), "HHmmss");
        int hh =  DataUtil.strToInt(strhh);

        //khong cho lay thong tin tu 0h den 6h sang
        if(0 <= hh &&  hh <= 55959){

            proRes.DESCRIPTION = "Hệ thống đang nâng cấp, bạn vui lòng nhập mã KM từ 6 giờ đến 24 giờ/ngày. Cảm ơn bạn đã tham gia chương trình “Giới thiệu bạn bè”.";
            proRes.RESULT = false;
            proRes.ERROR = 30001;
            msg.reply(proRes.toJsonObject());
            log.add("alarm", "Quy khach vui long thuc hien tu 06h - 24h hang ngay");
            log.add("execute promo result", proRes.toJsonObject().encodePrettily());
            log.writeLog();
            return;
        }

        phonesDb.getPhoneObjInfo(DataUtil.strToInt(reqObj.CREATOR), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj phoneObj) {

                //todo khong co tai khoan tren he thong
                if(phoneObj == null){
                    proRes.DESCRIPTION = "Ví quý khách không có trên hệ thống của chúng toi. Vui lòng kiểm tra lại. Xin cảm ơn";
                    proRes.RESULT = false;
                    proRes.ERROR = 30001;
                    msg.reply(proRes.toJsonObject());
                    log.add("alarm", "Vi qua khach khong co tren he thong");
                    log.add("execute promo result", proRes.toJsonObject().encodePrettily());
                    log.writeLog();
                    return;
                }

                //todo chi tra khuyen mai cho cac device co token
                if("".equalsIgnoreCase(phoneObj.pushToken)){

                    proRes.DESCRIPTION = "Máy quý khách sử dụng không hợp lệ, vui lòng thử lại sau";
                    proRes.RESULT = false;
                    proRes.ERROR = 30001;
                    msg.reply(proRes.toJsonObject());
                    log.add("os", phoneObj.phoneOs);
                    log.add("token",phoneObj.pushToken);
                    log.add("alarm", "device khong co token");
                    log.add("execute promo result", proRes.toJsonObject().encodePrettily());
                    log.writeLog();
                    return;
                }

                //todo lam theo luong hien tai
                promoTrackDb.getLatestByLastExecTime(DataUtil.strToInt(reqObj.CREATOR),4, new Handler<ArrayList<PromoTrackDb.Obj>>() {
                    @Override
                    public void handle(ArrayList<PromoTrackDb.Obj> objs) {

                        //todo su dung toi da 3 code/ 1ngay
                        if (objs != null && objs.size() >= 3) {

                            String ngay1= Misc.dateFormatWithParten(objs.get(0).LAST_EXEC_TIME, "yyyyMMdd");
                            String ngay2 = Misc.dateFormatWithParten(objs.get(1).LAST_EXEC_TIME, "yyyyMMdd");
                            String ngay3 = Misc.dateFormatWithParten(objs.get(2).LAST_EXEC_TIME, "yyyyMMdd");
                            String ngayhientai = Misc.dateFormatWithParten(System.currentTimeMillis(), "yyyyMMdd");

                            if(ngay1.equalsIgnoreCase(ngayhientai)
                                    && ngay2.equalsIgnoreCase(ngayhientai)
                                    && ngay3.equalsIgnoreCase(ngayhientai)){

                                proRes.DESCRIPTION = "Bạn đã sử dụng quá số lượng mã KM trong hôm nay. Vui lòng sử dụng tiếp mã KM vào ngày mai trong thời gian từ 6 giờ – 24 giờ.";
                                proRes.RESULT = false;
                                proRes.ERROR = 30001;
                                msg.reply(proRes.toJsonObject());
                                log.add("alarm", "Bi chan, toi da 3 lan/ 1ngay");
                                log.add("execute promo result", proRes.toJsonObject().encodePrettily());
                                log.writeLog();
                                return;

                            }
                        }

                        //normal.start
                        //kiem tra xem no da nhan bao nhieu lan roi
                        promoTrackDb.getClaimedCount(DataUtil.strToInt(reqObj.CREATOR), new Handler<Integer>() {
                            @Override
                            public void handle(Integer integer) {
                                if (integer >= fPromoObj.MAX_TIMES) {

                                    proRes.DESCRIPTION = "Bạn đã sử dụng quá số lượng mã KM trong chương trình. Cảm ơn bạn đã tham gia chương trình. MoMo sẽ tiếp tục mang đến cho bạn nhiều chương trình hấp dẫn trong thời gian tới.";
                                    proRes.RESULT = false;
                                    proRes.ERROR = 30001;
                                    msg.reply(proRes.toJsonObject());
                                    log.add("max time", fPromoObj.MAX_TIMES);
                                    log.add("exceed to get promo count", proRes.toJsonObject());
                                    log.writeLog();
                                    return;
                                }

                                promoTrackDb.findOne(actualCode, DataUtil.strToInt(reqObj.CREATOR), new Handler<PromoTrackDb.Obj>() {
                                    @Override
                                    public void handle(final PromoTrackDb.Obj proTrackObj) {
                                        if (proTrackObj == null) {
                                            proRes.DESCRIPTION = "Bạn đã nhập mã KM không hợp lệ. Vui lòng kiểm tra lại mã. Mã KM chỉ được sử dụng trên chính số ví MoMo của bạn và không sử dụng được cho các thuê bao trả sau chưa đăng ký thanh toán bằng thẻ trả trước hoặc Top-Up.";
                                            proRes.RESULT = false;
                                            proRes.ERROR = 30001;
                                            msg.reply(proRes.toJsonObject());
                                            log.add("execute promo result", proRes.toJsonObject());
                                            log.writeLog();
                                            return;
                                        }

                                        //30003: "Mã khuyến mãi của bạn đã hết hạn sử dụng. Vui lòng nhập mã khác.";
                                        long curTime = System.currentTimeMillis();
                                        if (curTime > (proTrackObj.TIME + fPromoObj.DURATION * 24 * 60 * 60 * 1000L)) {

                                            proRes.DESCRIPTION = "Mã khuyến mãi của bạn đã hết hạn sử dụng. Vui lòng nhập mã khác.";
                                            proRes.RESULT = false;
                                            proRes.ERROR = 30003;
                                            msg.reply(proRes.toJsonObject());

                                            log.add("current time", Misc.dateVNFormatWithTime(curTime));
                                            log.add("expired time", Misc.dateVNFormatWithTime((proTrackObj.TIME + fPromoObj.DURATION * 24 * 60 * 60 * 1000L)));
                                            log.add("Mã khuyến mãi của bạn đã hết hạn sử dụng. Vui lòng nhập mã khác.", "");
                                            log.add("execute promo result", proRes.toJsonObject());

                                            //cap nhat lai trang thai het han
                                            JsonObject joExpired = new JsonObject();
                                            joExpired.putString(colName.PromoTrackCols.PROMO_CODE, proTrackObj.PROMO_CODE);
                                            joExpired.putNumber(colName.PromoTrackCols.STATUS, PromoTrackDb.STATUS_EXPIRED);
                                            joExpired.putNumber(colName.PromoTrackCols.EXEC_NUMBER, DataUtil.strToInt(reqObj.CREATOR));
                                            joExpired.putString(colName.PromoTrackCols.EXEC_TIME, Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                                            joExpired.putNumber(colName.PromoTrackCols.EXEC_TIME_LAST, System.currentTimeMillis());

                                            promoTrackDb.update(joExpired, new Handler<Boolean>() {
                                                @Override
                                                public void handle(Boolean aBoolean) {
                                                    log.add("update result expired", aBoolean);
                                                    log.add("update expired for", proTrackObj.PROMO_CODE);
                                                    log.writeLog();
                                                }
                                            });

                                            return;
                                        }

                                        // 30002: "Mã khuyến mãi của bạn đã được sử dụng. Vui lòng sử dụng mã khuyến mãi khác.";
                                        if (proTrackObj.STATUS == PromoTrackDb.STATUS_PROMOTED) {
                                            proRes.DESCRIPTION = "Mã khuyến mãi của bạn đã được sử dụng. Vui lòng sử dụng mã khuyến mãi khác.";
                                            proRes.RESULT = false;
                                            proRes.ERROR = 30002;
                                            msg.reply(proRes.toJsonObject());
                                            log.add("Mã khuyến mãi của bạn đã được sử dụng. Vui lòng sử dụng mã khuyến mãi khác.", "");
                                            log.add("execute promo result", proRes.toJsonObject());
                                            log.writeLog();
                                            return;
                                        }

                                        if (proTrackObj.STATUS == PromoTrackDb.STATUS_LOCKED) {
                                            proRes.DESCRIPTION = "Mã khuyến mãi của bạn đang tạm khóa. Vui lòng thực hiện lại sau";
                                            proRes.RESULT = false;
                                            proRes.ERROR = 30002;
                                            msg.reply(proRes.toJsonObject());
                                            log.add("Mã khuyến mãi của bạn đang tạm khóa. Vui lòng thực hiện lại sau", "");
                                            log.add("execute promo result", proRes.toJsonObject());
                                            log.writeLog();
                                            return;
                                        }

                                        if (proTrackObj.STATUS == PromoTrackDb.STATUS_EXPIRED) {
                                            proRes.DESCRIPTION = "Mã khuyến mãi của bạn đã hết hạn sử dụng.";
                                            proRes.RESULT = false;
                                            proRes.ERROR = 30002;
                                            msg.reply(proRes.toJsonObject());
                                            log.add("Mã khuyến mãi của bạn đã hết hạn sử dụng.", "");
                                            log.add("execute promo result", proRes.toJsonObject());
                                            log.writeLog();
                                            return;
                                        }

                                        //cap nhat len trang thai locked
                                        JsonObject joLocked = new JsonObject();
                                        joLocked.putString(colName.PromoTrackCols.PROMO_CODE, proTrackObj.PROMO_CODE);
                                        joLocked.putNumber(colName.PromoTrackCols.STATUS, PromoTrackDb.STATUS_LOCKED);
                                        joLocked.putNumber(colName.PromoTrackCols.EXEC_NUMBER, DataUtil.strToInt(reqObj.CREATOR));
                                        joLocked.putString(colName.PromoTrackCols.EXEC_TIME, Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                                        joLocked.putNumber(colName.PromoTrackCols.EXEC_TIME_LAST, System.currentTimeMillis());

                                        promoTrackDb.update(joLocked, new Handler<Boolean>() {
                                            @Override
                                            public void handle(Boolean aBoolean) {
                                                log.add("update result locked", aBoolean);

                                                //neu lock thanh cong
                                                if (aBoolean) {
                                                    //send this to soap
                                                    vertx.eventBus().send(
                                                            AppConstant.SoapVerticle_ADDRESS,
                                                            MomoMessage.buildBuffer(
                                                                    SoapProto.MsgType.TOP_UP_STR_VALUE,
                                                                    0,
                                                                    0,
                                                                    SoapProto.TopUpString.newBuilder()
                                                                            .setFromNumber(fPromoObj.ADJUST_ACCOUNT)
                                                                            .setMpin(fPromoObj.ADJUST_PIN)
                                                                            .setChannel(Const.CHANNEL_MOBI)
                                                                            .setAmount(fPromoObj.PER_TRAN_VALUE)
                                                                            .setToNumber(reqObj.CREATOR)
                                                                            .addKeyValuePairs(SoapProto.keyValuePair.newBuilder()
                                                                                    .setKey(Const.INVITE_FRIEND)
                                                                                    .setValue(proTrackObj.PROMO_CODE))
                                                                            .build()
                                                                            .toByteArray()
                                                            ),
                                                            new Handler<Message<JsonObject>>() {
                                                                @Override
                                                                public void handle(Message<JsonObject> result) {

                                                                    final JsonObject jResult = result.body();

                                                                    //{"status":5,"error":43,"tranId":1343529,"amt":20000,"io":-1,"ftime":1410486383467}
                                                                    int error = jResult.getInteger("error", -1);
                                                                    log.add("error", error);
                                                                    log.add("desc", SoapError.getDesc(error));

                                                                    //khuyen mai thanh cong
                                                                    if (error == 0) {
                                                                        //khuyen mai da thanh cong
                                                                        log.add("cap nhat trang thai da khuyen mai", "promoted");

                                                                        JsonObject joSuccess = new JsonObject();
                                                                        joSuccess.putString(colName.PromoTrackCols.PROMO_CODE, proTrackObj.PROMO_CODE);
                                                                        joSuccess.putNumber(colName.PromoTrackCols.STATUS, PromoTrackDb.STATUS_PROMOTED);
                                                                        joSuccess.putNumber(colName.PromoTrackCols.EXEC_NUMBER, DataUtil.strToInt(reqObj.CREATOR));
                                                                        joSuccess.putString(colName.PromoTrackCols.EXEC_TIME, Misc.dateVNFormatWithTime(System.currentTimeMillis()));
                                                                        joSuccess.putNumber(colName.PromoTrackCols.EXEC_TIME_LAST, System.currentTimeMillis());

                                                                        promoTrackDb.update(joSuccess, new Handler<Boolean>() {
                                                                            @Override
                                                                            public void handle(Boolean aBoolean) {
                                                                                log.add("update result promoted", aBoolean);
                                                                                log.add("update with code", proTrackObj.PROMO_CODE);

                                                                                //tra ket qua thong bao thanh cong
                                                                                proRes.DESCRIPTION = "Khuyen mai thanh cong";
                                                                                proRes.RESULT = true;
                                                                                proRes.PROMO_AMOUNT = fPromoObj.PER_TRAN_VALUE;
                                                                                proRes.ERROR = 0;
                                                                                msg.reply(proRes.toJsonObject());
                                                                                log.add("desc", "khuyen mai thanh cong");
                                                                                log.add("execute promo result", proRes.toJsonObject());
                                                                                log.writeLog();
                                                                            }
                                                                        });

                                                                    } else {

                                                                        //30005 : thuc hien top up khong thanh cong
                                                                        //topup khong thanh cong
                                                                        proRes.DESCRIPTION = "Khuyến mãi không thành công. Thuê bao quý khách có thể là trả sau. Quý khách có thể tặng mã khuyến mãi cho người thân, bạn bè hoặc thử lại. Xin cảm ơn";
                                                                        proRes.RESULT = false;
                                                                        proRes.ERROR = 30005;
                                                                        msg.reply(proRes.toJsonObject());

                                                                        log.add("thuc hien khuyen mai khong thanh cong", "");
                                                                        log.add("error", jResult.getInteger(colName.TranDBCols.ERROR, -1));
                                                                        log.add("desc", SoapError.getDesc(jResult.getInteger(colName.TranDBCols.ERROR, -1)));
                                                                        log.add("execute promo result", proRes.toJsonObject());

                                                                        log.writeLog();
                                                                    }
                                                                }
                                                            }
                                                    );
                                                } else {
                                                    log.add("update with code", proTrackObj.PROMO_CODE);
                                                    log.writeLog();
                                                }
                                            }
                                        });
                                    }
                                });

                            }
                        });
                        //normal.end
                    }
                });


            }
        });
    }

    private void doM2MClaimCode(final Message msg){
        final PromoReqObj reqObj = new PromoReqObj((JsonObject)msg.body());
        reqObj.PROMO_CODE = reqObj.PROMO_CODE.toUpperCase();
        //tinh toan thuc hien gen code

        /*case 30001:"Bạn đã nhập sai mã khuyến mãi. Vui lòng nhập lại.";
        case 30002: "Mã khuyến mãi của bạn đã được sử dụng. Vui lòng sử dụng mã khuyến mãi khác.";
        break;
        case 30003: "Mã khuyến mãi của bạn đã hết hạn sử dụng. Vui lòng nhập mã khác.";
        30004 : khong co chuong trinh khuyen mai nao
        30005 : thuc hien top up khong thanh cong

        */

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(reqObj.CREATOR);
        log.add("function", "doM2MClaimCode");
        log.add("promo code", reqObj.PROMO_CODE);

        final Promo.PromoResObj proRes = new Promo.PromoResObj();
        proRes.ERROR= 2;
        proRes.RESULT= false;
        proRes.DESCRIPTION= "Mã code không hợp lệ. Vui lòng kiểm tra lại";

        boolean isValid =  codeUtilM2M.isCodeValid(reqObj.PROMO_CODE, "MOMO");

        if(!isValid){
            msg.reply(proRes.toJsonObject());

            log.add("error", "Hash khong hop le :" + reqObj.PROMO_CODE);
            log.writeLog();
            return;
        }
        merchantPromosDb.findOneByNumberInList("0" + DataUtil.strToInt(reqObj.CREATOR), new Handler<MerchantPromosDb.Obj>() {
            @Override
            public void handle(final MerchantPromosDb.Obj merchantInfo) {
                if (merchantInfo == null) {
                    proRes.DESCRIPTION = "Số điện thoại của quý khách không nằm trong chương trình này";
                    msg.reply(proRes.toJsonObject());
                    log.add("so dien thoai khong nam trong danh sach chuong trinh", reqObj.CREATOR);
//                    log.writeLog();

                } else {

                    log.add("Kiem tra co nam trong danh sach khuyen mai nhan khuyen mai khong", "0" + reqObj.CREATOR);
                    if (!merchantInfo.numList.contains("0" + DataUtil.strToInt(reqObj.CREATOR))) {
                        String str = "";
                        for (int i = 0; i < merchantInfo.numList.size(); i++) {
                            str += merchantInfo.numList.get(i) + "  ";
                        }
                        log.add("numList", str);
                        log.add("Khong nam trong danh sach nhan khuyen mai", "----------");
                        log.writeLog();

                        proRes.DESCRIPTION = "Số điện thoại của quý khách không nằm trong chương trình này";
                        msg.reply(proRes.toJsonObject());
                        return;
                    }

                    merchantPromoTracksDb.findByCode(reqObj.PROMO_CODE, new Handler<MerchantPromoTracksDb.Obj>() {
                        @Override
                        public void handle(final MerchantPromoTracksDb.Obj trackInfo) {
                            if (trackInfo == null) {

                                proRes.DESCRIPTION = "Mã code không hợp lệ. Vui lòng kiểm tra lại";
                                msg.reply(proRes.toJsonObject());
                                log.add("error", "Khong tim thay code :" + reqObj.PROMO_CODE + " trong DB");
                                log.writeLog();
                                return;
                            } else {

                                //todo kiem tra value
                                if (merchantInfo.totalVal + trackInfo.value > merchantInfo.maxVal) {
                                    proRes.DESCRIPTION = "Số tiền dùng để khuyến mãi đã vượt quá giới hạn " + Misc.formatAmount(merchantInfo.maxVal).replace(",", ".") + "đ.";
                                    msg.reply(proRes.toJsonObject());
                                    log.add("error", "Số tiền dùng để khuyến mãi đã vượt quá giới hạn " + merchantInfo.maxVal);
                                    log.writeLog();
                                    return;
                                }

                                //todo kiem tra expired code
                                long curTime = System.currentTimeMillis();
                                if (trackInfo.expiredTime.getTime() < curTime) {

                                    proRes.DESCRIPTION = "Mã code đã hết hạn sử dụng";
                                    msg.reply(proRes.toJsonObject());
                                    log.add("error", "Mã code đã hết hạn sử dụng");
                                    log.writeLog();
                                    return;
                                }

                                //todo kiem tra trang thai ma code
                                if (trackInfo.status != MerchantPromoTracksDb.STAT_NEW) {
                                    proRes.DESCRIPTION = "Mã code đã được sử dụng";
                                    msg.reply(proRes.toJsonObject());
                                    log.add("error", "Mã code đã được sử dụng");
                                    log.writeLog();
                                    return;
                                }

                                //todo include old values
                                if (!"".equalsIgnoreCase(trackInfo.mNumberList)
                                        && !trackInfo.mNumberList.contains(reqObj.CREATOR)) {
                                    proRes.DESCRIPTION = "Mã khuyến mãi không hợp lệ";
                                    msg.reply(proRes.toJsonObject());
                                    log.add("error", "So dien thoai " + reqObj.CREATOR + " khong nam trong danh sach claim tien cua code " + reqObj.PROMO_CODE);
                                    log.writeLog();
                                    return;
                                }

                                merchantPromoTracksDb.setStatus(reqObj.PROMO_CODE
                                        , MerchantPromoTracksDb.STAT_NEW, MerchantPromoTracksDb.STAT_BLOCK, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {

                                        log.add("cap nhat trang thai ma code", "new -->locked");
                                        log.add("result", aBoolean);

                                        if (aBoolean) {

                                            log.add("thuc hien adjustment", "with info");
                                            log.add("from number", merchantInfo.sourceAcc);
                                            log.add("to number", merchantInfo.number);
                                            log.add("value", trackInfo.value);
                                            log.add("wallet", WalletType.MOMO);

                                            ArrayList<Misc.KeyValue> list = new ArrayList<>();
                                            list.add(new Misc.KeyValue(Const.PROMO_CODE, reqObj.PROMO_CODE));

                                            Misc.adjustment(vertx
                                                    , merchantInfo.sourceAcc
                                                    , reqObj.CREATOR
                                                    , trackInfo.value
                                                    , WalletType.MOMO
                                                    , list, log, new Handler<Common.SoapObjReply>() {
                                                @Override
                                                public void handle(Common.SoapObjReply soapObjReply) {

                                                    log.add("error", soapObjReply.error);
                                                    log.add("errorDesc", SoapError.getDesc(soapObjReply.error));
//                                                    log.writeLog();
                                                    //khong thanh cong
                                                    proRes.ERROR = soapObjReply.error;

                                                    if (soapObjReply.error != 0) {
                                                        proRes.RESULT = false;
                                                        proRes.DESCRIPTION = "Quý khách vui lòng thử lại. Xin cảm ơn";

                                                        //ca
                                                        merchantPromoTracksDb.setStatus(reqObj.PROMO_CODE, MerchantPromoTracksDb.STAT_BLOCK, MerchantPromoTracksDb.STAT_NEW, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {

                                                            }
                                                        });
                                                    } else {

                                                        proRes.RESULT = true;
                                                        proRes.DESCRIPTION = "Chúc mừng quý khách đã nhận thành công " + Misc.formatAmount(trackInfo.value) + " đ";
                                                        proRes.PROMO_AMOUNT = trackInfo.value;

                                                        //todo : cap nhat tong so luong code da su dung
                                                        log.add("tang so code da su dung", "1");
                                                        merchantPromosDb.increase(MerchantPromosDb.ColNames.USED_CODE, merchantInfo.number, 1, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {
                                                            }
                                                        });

                                                        log.add("cap nhat lai total trong bang CodeUtil", "");
                                                        //todo : cap nhat lai total val
                                                        merchantPromosDb.increase(MerchantPromosDb.ColNames.TOTAL_VAL, merchantInfo.number, trackInfo.value, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {

                                                            }
                                                        });

                                                        log.add("Cap nhat trang thai trong bang MerchantPromoTracksDb", "blocked --> used");

                                                        //todo cap nhat lai trang thai code
                                                        merchantPromoTracksDb.setStatus(reqObj.PROMO_CODE, MerchantPromoTracksDb.STAT_BLOCK, MerchantPromoTracksDb.STAT_USED, new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {

                                                            }
                                                        });

                                                        //todo ghi nhan thang claim tien trong danh sach chu quan
                                                        merchantPromoTracksDb.setClaimer(reqObj.PROMO_CODE, "0" + DataUtil.strToInt(reqObj.CREATOR), new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean aBoolean) {

                                                            }
                                                        });

                                                        PhonesDb.Obj obj = new PhonesDb.Obj();
                                                        obj.number = DataUtil.strToInt(reqObj.CREATOR);

                                                        //todo force update current balance
                                                        BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                                                        msg.setType(SoapProto.Broadcast.MsgType.FORCE_UPDATE_AGENT_INFO_VALUE);
                                                        msg.setSenderNumber(DataUtil.strToInt(reqObj.CREATOR));
                                                        msg.setExtra(obj.toJsonObject());

                                                        vertx.eventBus().publish(Misc.getNumberBus(DataUtil.strToInt(reqObj.CREATOR)), msg.getJsonObject());

                                                        //todo tao tran  + noti cho giao dich nay
                                                        final TranObj tran = new TranObj();
                                                        tran.owner_number = DataUtil.strToInt(reqObj.CREATOR);
                                                        tran.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
                                                        tran.tranId = soapObjReply.tranId;
                                                        tran.clientTime = soapObjReply.finishTime;
                                                        tran.ackTime = soapObjReply.finishTime;
                                                        tran.finishTime = soapObjReply.finishTime;//=> this must be the time we sync, or user will not sync this to device
                                                        tran.amount = soapObjReply.amount;
                                                        tran.status = soapObjReply.status;
                                                        tran.error = soapObjReply.error;// lay ma loi cua core tra ve cho client o day
                                                        tran.cmdId = soapObjReply.finishTime;
                                                        tran.billId = "-1";
                                                        tran.category = 0;
                                                        tran.io = soapObjReply.io;
                                                        tran.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
                                                        tran.parterCode = "M_SERVICE";

                                                        tran.comment = "Bạn đã nhận được " + Misc.formatAmount(trackInfo.value).replace(",", ".") + "đ từ " + merchantInfo.sourceAcc + ", M_SERVICE. Mã nhận tiền " + reqObj.PROMO_CODE;

                                                        tran.partnerId = merchantInfo.sourceAcc;
                                                        tran.partnerName = "M_SERVICE";

                                                        log.add("create new tran for 123pay", "");
                                                        log.add("tran 123pay json", tran.getJSON());

                                                        transDb.upsertTranOutSideNew(tran.owner_number, tran.getJSON(), new Handler<Boolean>() {
                                                            @Override
                                                            public void handle(Boolean result) {

                                                                log.add("isUpdated", result);

                                                                BroadcastHandler.sendOutSideTransSync(vertx, tran);

                                                                Notification noti = new Notification();
                                                                noti.receiverNumber = tran.owner_number;
                                                                noti.caption = "Nhận tiền thành công";   //"Ngày MoMo";
                                                                noti.body = tran.comment; //"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";
                                                                noti.sms = "";          //"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.";
                                                                noti.priority = 2;
                                                                noti.time = System.currentTimeMillis();
                                                                noti.tranId = tran.tranId;
                                                                noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

                                                                Misc.sendNoti(vertx, noti);
                                                            }
                                                        });
                                                    }

                                                    msg.reply(proRes.toJsonObject());
                                                    log.writeLog();
                                                }
                                            });
                                        } else {

                                            proRes.ERROR = 1;
                                            proRes.RESULT = false;
                                            proRes.DESCRIPTION = "Hệ thống đang tạm bảo trì, Quý khách vui lòng thực hiện lại sau";
                                            msg.reply(proRes.toJsonObject());
                                            log.writeLog();
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private PromotionDb.Obj getPromoObj(String promoName) {
        PromotionDb.Obj promoObj = null;
        if(listPromos.size()>0){

            for(int i = 0; i< listPromos.size(); i ++){
                if(promoName.equalsIgnoreCase(listPromos.get(i).NAME)){
                    promoObj= listPromos.get(i);
                    break;
                }
            }
        }
        return promoObj;
    }

    private void doAdjustChickenFeed(final long tranId, final Common.BuildLog log) {

//        log.add("function","doAdjustChickenFeed");
//
//        ArrayList<CoreObj> arrayList = dbProcessPromotion.PRO_POSTCOMMIT_TRANS(tranId, log);
//
//        log.add("call store result size",arrayList.size());
//
//        if(arrayList != null && arrayList.size() >0){
//            for(CoreObj o : arrayList){
//                switch (o.child_type){
//                    case "adjustment":
//                        final HashMap map = new HashMap();
//                        final CoreObj fo = o;
//                        map.put("promo_name",o.promo_name);
//
//                        SoapProto.keyValuePair.Builder builder = SoapProto.keyValuePair.newBuilder();
//                        builder.setKey("promo_name");
//                        builder.setValue(o.promo_name);
//
//                        final List<SoapProto.keyValuePair> listKeyValuePairs = new ArrayList<>();
//                        listKeyValuePairs.add(builder.build());
//
//                        phonesDb.getPhoneObjInfo(DataUtil.strToInt(o.child_creditor), new Handler<PhonesDb.Obj>() {
//                            @Override
//                            public void handle(PhonesDb.Obj obj) {
//                                int rcode = execPromotion(fo.child_debitor
//                                        ,fo.child_creditor
//                                        ,fo.child_amount
//                                        ,listKeyValuePairs
//                                        ,log
//                                        ,"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo."
//                                        ,"Ngày MoMo"
//                                        ,"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo."
//                                        ,"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach."
//                                        ,obj);
//
//                                if(rcode != 0){
//                                    log.add("khong co khuyen mai",rcode);
//                                }
//                            }
//                        });
//                        break;
//                    default:
//                        log.add("not support for promotion type",o.child_type);
//                        break;
//                }
//            }
//        }
    }

    private int execPromotion(String debitor
                                            ,String creditor
                                            ,long amount
                                            ,List<SoapProto.keyValuePair> map
                                            ,final Common.BuildLog log
                                            ,String notiComment
                                            ,String notiCaption
                                            ,String notiBody
                                            ,String notiSms
                                            ,PhonesDb.Obj obj){

        AdjustWalletResponse res = mSoapProcessor.adjustment(debitor
                ,creditor
                ,new BigDecimal(amount)
                ,1
                ,""
                ,map
                ,log);

        int result = -99;

        if(res!= null && res.getAdjustWalletReturn() != null){

            result = res.getAdjustWalletReturn().getResult();

            long failTranId = System.currentTimeMillis();

            long tranId = (res.getAdjustWalletReturn().getTransid() == null ? failTranId : res.getAdjustWalletReturn().getTransid());

            //thuc hien khuyen mai thanh cong
            if(result == 0){

                //1. send tranoutside, noti + sms
                sendTranAndSms(amount
                        ,creditor
                        ,notiComment
                        ,notiCaption
                        ,notiBody
                        ,notiSms,tranId);

                //2. update current balance
                if(obj != null && obj.number > 0){

                    BroadcastHandler.LocalMsgHelper msg = new BroadcastHandler.LocalMsgHelper();
                    msg.setType(SoapProto.Broadcast.MsgType.FORCE_UPDATE_AGENT_INFO_VALUE);
                    msg.setSenderNumber(obj.number);
                    msg.setExtra(obj.toJsonObject());

                    vertx.eventBus().publish(Misc.getNumberBus(obj.number), msg.getJsonObject());
                }
            }
        }
        return result;
    }

    //save tran, send tranout side, send noti + sms ra
    private void sendTranAndSms(final long amount
            ,final String creditor
            ,final String comment
            ,final String caption
            ,final String body
            ,final String sms
            ,final long tranId) {
        final TranObj mainObj = new TranObj();

        mainObj.tranType = MomoProto.TranHisV1.TranType.M2M_VALUE;
        mainObj.tranId = tranId;
        mainObj.clientTime = System.currentTimeMillis();
        mainObj.ackTime = System.currentTimeMillis();
        mainObj.finishTime = System.currentTimeMillis();//=> this must be the time we sync, or user will not sync this to device
        mainObj.amount =amount ;//o.child_amount
        mainObj.status = TranObj.STATUS_OK;
        mainObj.error = 0;
        mainObj.cmdId = -1;
        mainObj.billId = "-1";
        mainObj.io = +1;
        mainObj.source_from = MomoProto.TranHisV1.SourceFrom.MOMO_VALUE;
        mainObj.category = -1;
        mainObj.partnerName = "MoMo";

        //[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.

        mainObj.comment = comment;//"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";

        mainObj.owner_number = DataUtil.strToInt(creditor); //o.child_creditor
        mainObj.owner_name = "MoMo";

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + mainObj.owner_number);
        log.add("function","sendTranAndSms");
        log.add("creditor",creditor);
        log.add("comment", comment);
        log.add("body", body);
        log.add("caption",caption);
        log.add("sms",sms);
        log.add("tranid", mainObj.tranId);
        log.add("tran JSON", mainObj.getJSON());
        log.add("function","upsertTranOutSideNew");

        transDb.upsertTranOutSideNew(mainObj.owner_number, mainObj.getJSON(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean result) {

                log.add("isUpdated", result);

                //khong phai la cap nhat -->tao moi
                if (!result) {
                    //log.add("sendOutSideTransSync","");
                    BroadcastHandler.sendOutSideTransSync(vertx, mainObj);

                    Notification noti = new Notification();
                    noti.receiverNumber = mainObj.owner_number;
                    noti.caption = caption;   //"Ngày MoMo";
                    noti.body = body;       //"Chúc mừng Quý Khách đã được tặng 20.000đ nhân ngày MoMo.";
                    noti.sms = sms;         //"[Ngay MoMo] Chuc mung Quy khach da duoc tang 20.000d vao vi MoMo. Vui long dang nhap de kiem tra so du. Chan thanh cam on quy khach.";
                    noti.priority = 1;
                    noti.time = System.currentTimeMillis();
                    noti.tranId = tranId;
                    noti.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;

                    vertx.eventBus().send(AppConstant.NotificationVerticle_ADDRESS_SEND_NOTIFICATION
                            , noti.toFullJsonObject(), new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                        }
                    });
                }
                log.writeLog();
            }
        });
    }

    private void LoadCfg(JsonObject config, Logger logger){

        JsonObject cfg  = config.getObject("umarket_database");
        String driverPromo = cfg.getString("driver");
        String urlPromo = cfg.getString("url");
        String usernamePromo = cfg.getString("username");
        String passwordPromo = cfg.getString("password");

        String tpl = "DRIVER: %s URL: %s USERNAME: %s PASSWORD: %s";
        logger.info("PostCommitVerticle " + String.format(tpl,driverPromo,urlPromo,usernamePromo,passwordPromo));


//        dbProcessPromotion = new DBProcess(driverPromo
//                ,urlPromo
//                ,usernamePromo
//                ,passwordPromo
//                ,AppConstant.Promotion_ADDRESS
//                ,AppConstant.Promotion_ADDRESS
//                ,logger);
    }

    private static class Iter {
        protected static String caption = "Giới thiệu bạn bè thành công";
        protected static String body = "Mã KM: MoMo%s Chúc mừng bạn đã giới thiệu thành công cho số %s Mã khuyến mãi có giá trị trong 07 ngày (mã không sử dụng được cho các thuê bao trả sau chưa đăng ký thanh toán bằng thẻ trả trước hoặc  Top-Up).";
        protected static String sms = "Ban da gioi thieu thanh cong cho so %s Su dung ma KM: MoMo%s de nap tien dt cho chinh minh hoac ban be/nguoi than. Ma KM co gia tri trong 30 ngay.";
    }

    private static class Itee {
        protected static String caption = "Mã khuyến mãi";
        //Mã KM: MoMo123456 . Chúc mừng bạn đã giới thiệu thành công cho số 01234567890.
        protected static String body = "Mã KM: MoMo%s Dùng mã khuyến mãi này để nạp tiền điện thoại cho chính mình. Mã khuyến mãi có giá trị trong 07 ngày (mã không sử dụng được cho các thuê bao trả sau chưa đăng ký thanh toán bằng thẻ trả trước hoặc Top-Up).";
        protected static String sms = "Ma KM cua ban la MoMo%s Dung ma KM nay de nao tien dt cho chinh minh hoac ban be/nguoi than. Ma KM co gia tri trong 30 ngay.";
    }



}
