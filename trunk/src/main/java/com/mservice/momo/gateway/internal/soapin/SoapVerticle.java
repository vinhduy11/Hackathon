package com.mservice.momo.gateway.internal.soapin;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.data.*;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.gateway.internal.soapin.information.SoapInProcess;
import com.mservice.momo.gateway.internal.soapin.information.obj.AgentInfo;
import com.mservice.momo.gateway.internal.soapin.information.obj.RegisterInfo;
import com.mservice.momo.gateway.internal.soapin.information.obj.StandardMSResponse;
import com.mservice.momo.gateway.internal.soapin.information.session.SessionInfo;
import com.mservice.momo.gateway.internal.soapin.information.session.SessionManager;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.msg.StatisticModels;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StatisticUtils;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
import umarketscws.AdjustWalletResponse;
import umarketscws.AgentResponseType;
import umarketscws.CreatesessionResponseType;
import umarketscws.StandardBizResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by admin on 2/8/14.
 */
public class SoapVerticle extends Verticle {
    //    public static final String ADDRESS = "com.mservice.momo.soapin";
    private Logger logger;
    private SoapInProcess mSoapProcessor;
    private PhonesDb mPhoneDb;
    private M2cOffline m2cOffline;
    private TransDb transDb;
    //    private DBProcess dbProcess;
//    private DBProcess dbProcessPromotion;
    private PromotedDb promotedDb;
    private Common mCom;

    private int divider_bank_in; // used to calculate mpoint for agent, = 0-> not add mpoint
    private int divider_bank_out;
    private int divider_m2c;
    private int divider_m2m;
    private int divider_top_up;
    private int divider_top_up_game;
    private int divider_pay_one_bill;
    private int divider_quick_deposit;
    private int divider_quick_payment; // used to calculate mpoint for agent, = 0-> not add mpoint
    private int divider_pay_one_bill_other;
    private int divider_transfer_money_2_place;

    private int divider_bill_pay_telephone;
    private int divider_bill_pay_ticket_airline;
    private int divider_bill_pay_ticket_train;
    private int divider_bill_pay_insurance;
    private int divider_bill_pay_internet;
    private int divider_bill_pay_other;

    /*DEPOSIT_CASH_OTHER  = 5037; // NAP TIEN KHAC
    BUY_MOBILITY_CARD   = 5038; // MUA THE CAO
    BUY_GAME_CARD       = 5039; // MUA THE GAME
    BUY_OTHER           = 5040; // MUA KHAC*/
    private int divider_deposit_cash_other;
    private int divider_buy_mobility_card;
    private int divider_buy_game_card;
    private int divider_buy_other;

    private PromotionDb promotionDb;
    private PhonesDb phonesDb;
    private int noname_group_id = 0;
    private int tqt_group_id = 0;
    private GroupManageDb groupManageDb;
    private JsonObject glbCfg;
    //private CacheBillInfoViaCoreDb cacheBillInfoViaCoreDb;
    private JsonObject cacheBillCfg;

    public void start() {
        logger = getContainer().logger();
        glbCfg = container.config();
        LoadCfg(glbCfg, logger);

        mSoapProcessor = new SoapInProcess(logger, glbCfg);
        mPhoneDb = new PhonesDb(vertx.eventBus(), logger);
        m2cOffline = new M2cOffline(vertx.eventBus(), logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        promotedDb = new PromotedDb(vertx.eventBus(), logger);
        mCom = new Common(vertx, logger, container.config());
        EventBus eb = vertx.eventBus();
        promotionDb = new PromotionDb(vertx.eventBus(), logger);
        phonesDb = new PhonesDb(vertx.eventBus(), logger);

        groupManageDb = new GroupManageDb(vertx, logger);


//        cacheBillInfoViaCoreDb = new CacheBillInfoViaCoreDb(vertx, logger);

        cacheBillCfg = glbCfg.getObject(StringConstUtil.CACHE_BILL_INFO, new JsonObject());
        this.noname_group_id = DataUtil.strToInt(glbCfg.getObject("server").getString("noname_group_id", "34112"));
        this.tqt_group_id = DataUtil.strToInt(glbCfg.getObject("server").getString(StringConstUtil.TQT_GROUP_ID, "59123"));

        Handler<Message> myHandler = new Handler<Message>() {
            public void handle(Message message) {
                //we know this body must be a MomoMessage
                MomoMessage momoMessage = MomoMessage.fromBuffer((Buffer) message.body());

                switch (momoMessage.cmdType) {
                    case SoapProto.MsgType.GET_AGENT_INFO_VALUE:
                        getAgentInfo(message);
                        break;
                    case SoapProto.MsgType.REGISTER_VALUE:
                        register(message);
                        break;
                    case SoapProto.MsgType.LOG_IN_VALUE:
                        logIn(message);
                        break;
                    case SoapProto.MsgType.GET_BILL_INFO_VALUE:
                        getBillInfo(message);
                        break;

                    case SoapProto.MsgType.GET_BILL_INFO_BY_SERVICE_VALUE:
                        getBillInfoByService(message);
                        break;
                    case SoapProto.MsgType.TOP_UP_VALUE:
                        topUpAirTime(message);
                        break;

                    //topup voi source va recieve la string
                    case SoapProto.MsgType.TOP_UP_STR_VALUE:
                        topUpAirTimeString(message);
                        break;

                    case SoapProto.MsgType.BANK_OUT_VALUE:
                        doBankOut(message);
                        break;
                    case SoapProto.MsgType.BANK_IN_VALUE:
                        doBankIn(message);
                        break;
                    /*case SoapProto.MsgType.M2C_TRANSFER_VALUE:
                        doM2CTransfer(message);
                        break;*/
                    case SoapProto.MsgType.M2M_TRANSFER_VALUE:
                        doM2MTransfer(message);
                        break;
                    case SoapProto.MsgType.CHANGE_PIN_VALUE:
                        doChangePin(message);
                        break;

                    case SoapProto.MsgType.RECOVERY_NEW_PIN_VALUE:
                        doRecoveryPin(message);
                        break;
                    case SoapProto.MsgType.PAY_ONE_BILL_VALUE:
                        doPayOneBill(message);
                        break;

                    case SoapProto.MsgType.PAY_ONE_BILL_OTHER_VALUE:
                        //doPayOneBillOther(message);
                        break;

                    case SoapProto.MsgType.TOPUP_GAME_VALUE:
                        doTopUpGame(message);
                        break;

                    case SoapProto.MsgType.QUICK_DEPOSIT_VALUE:
                        doQuickDeposit(message);
                        break;

                    case SoapProto.MsgType.QUICK_PAYMENT_VALUE:
                        doQuickPayment(message);
                        break;

                    case SoapProto.MsgType.TRANSFER_MONEY_TO_PLACE_VALUE:
                        doTransferMoney2Place(message);
                        break;

                    case SoapProto.MsgType.BANK_NET_ADJUSTMENT_VALUE:
                        doBanknetAjustment(message);
                        break;
                    case SoapProto.MsgType.MUA_123_ADJUSTMENT_VALUE:
                        doMua123Adjustment(message);
                        break;

                    //for app sms
                    case SoapProto.MsgType.BILL_PAY_TELEPHONE_VALUE:
                        doBillPayTelephone(message);
                        break;
                    case SoapProto.MsgType.BILL_PAY_TICKET_AIRLINE_VALUE:
                        doBillPayTicketAirline(message);
                        break;
                    case SoapProto.MsgType.BILL_PAY_TICKET_TRAIN_VALUE:
                        doBillPayTicketTrain(message);
                        break;
                    case SoapProto.MsgType.BILL_PAY_INSURANCE_VALUE:
                        doBillPayInsurance(message);
                        break;
                    case SoapProto.MsgType.BILL_PAY_INTERNET_VALUE:
                        doBillPayInternet(message);
                        break;
                    case SoapProto.MsgType.BILL_PAY_OTHER_VALUE:
                        doBillPayOther(message);
                        break;
                    case SoapProto.MsgType.DEPOSIT_CASH_OTHER_VALUE:
                        doDepositCashOther(message);
                        break;
                    case SoapProto.MsgType.BUY_MOBILITY_CARD_VALUE:
                        doBuyMobilityCard(message);
                        break;
                    case SoapProto.MsgType.BUY_GAME_CARD_VALUE:
                        doBuyGameCard(message);
                        break;
                    case SoapProto.MsgType.BUY_OTHER_VALUE:
                        doBuyOther(message);
                        break;
                    case SoapProto.MsgType.AGENT_INFO_MODIFY_VALUE:
                        doAgentModify(message);
                        break;

                    case SoapProto.MsgType.MODIFY_AGENT_EXTRA_VALUE:
                        doAgentModifyExtra(message);
                        break;

                    case SoapProto.MsgType.REGISTER_INACTIVE_AGENT_VALUE:
                        registerM2N(message);
                        break;

                    case SoapProto.MsgType.CHECK_USER_STATUS_VALUE:
                        checkUserStatus(message);
                        break;
                    case SoapProto.MsgType.MAP_AGENT_TO_ZALO_GROUP_VALUE:
                        setZaloGroup(message);
                        break;
                    case SoapProto.MsgType.SET_AGENT_NAMED_VALUE:
                        doNamedAgent(message);
                        break;

                    case SoapProto.MsgType.TRA_THUONG_ZALO_VALUE:
                        traThuongZalo(message);
                        break;

                    case SoapProto.MsgType.ADJUSTMENT_VALUE:
                        doCommonAdjustment(message);
                        break;

                    case SoapProto.MsgType.MAP_AGENT_TO_VISA_GROUP_VALUE:
                        setVisaGroup(message);
                        break;
                    case SoapProto.MsgType.M2MERCHANT_TRANSFER_VALUE:
                        doM2MerchantTransfer(message);
                        break;
                    default:
                        logger.error("Call Soap in with invalid message type: ".concat(String.valueOf(momoMessage.cmdType)));
                        break;
                }
            }
        };

        eb.registerLocalHandler(AppConstant.SoapVerticle_ADDRESS, myHandler);

    }

    private void doNamedAgent(Message message) {
        MomoMessage msg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.ZaloGroup visaGroup;
        try {
            visaGroup = SoapProto.ZaloGroup.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            visaGroup = null;
        }

        if (visaGroup == null) {
            message.reply(false);
            return;
        }

        String phone = "0" + msg.cmdPhone;
        String action = visaGroup.getZaloGroup();
        final Common.BuildLog log = new Common.BuildLog(logger);
        final CreatesessionResponseType csrt = SessionManager.getCsrt(log.getTime());
        boolean result_noname = false;
        boolean result_tqt = false;
        if (csrt != null && !"".equalsIgnoreCase(csrt.getSessionid())) {

            if ("named".equalsIgnoreCase(action)) {
                //Unmap khoi group noname
                result_noname = mSoapProcessor.unMapNoNameGroup(csrt.getSessionid()
                        , phone
                        , noname_group_id
                        , log);
                log.add("**********", "result unmap noname: " + result_noname);
                //Unmap khoi group TQT
                result_tqt = mSoapProcessor.unMapNoNameGroup(csrt.getSessionid()
                        , phone
                        , tqt_group_id
                        , log);
                log.add("**********", "result unmap tqtgroup: " + result_tqt);
                if (result_noname) {
                    setGroupMoMo(phone);
                }
            } else if ("unnamed".equalsIgnoreCase(action)) {
                StandardBizResponse groupResponse;
                groupResponse = mSoapProcessor.mapAgentToGroup("77777777"
                        , phone
                        , noname_group_id);
                log.add("**********", "result map noname: " + result_noname);
                result_noname = groupResponse.getResult() == 0;
            }
        }
        log.writeLog();

        message.reply(result_noname);
    }

    private void doCommonAdjustment(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.commonAdjust commonAdjust;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            commonAdjust = SoapProto.commonAdjust.parseFrom(momoMsg.cmdBody);

        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            commonAdjust = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        StandardBizResponse response = null;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);
        log.setTime(commonAdjust.getTime());

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            AdjustWalletResponse adjustWalletResponse = mSoapProcessor.adjustment(commonAdjust.getSource()
                    , commonAdjust.getTarget()
                    , BigDecimal.valueOf(commonAdjust.getAmount())
                    , commonAdjust.getWalletType()
                    , commonAdjust.getDescription()
                    , commonAdjust.getExtraMapList()
                    , log);

            if (adjustWalletResponse == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                response = adjustWalletResponse.getAdjustWalletReturn();
                result = response.getResult();
                tranId = (response.getTransid() == null ? System.currentTimeMillis() : response.getTransid());
            }
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, commonAdjust.getAmount(), 1);
        message.reply(replyObj);

        log.writeLog();
    }

    private void checkUserStatus(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        AgentResponseType agent = mSoapProcessor.getAgentStatus("0" + momoMsg.cmdPhone);

        MomoProto.RegStatus.Builder regStatus = MomoProto.RegStatus.newBuilder();

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);
        log.add("function", "checkUserStatus");

        if (agent == null || agent.getAgent() == null) {
            log.add("agent or agent.getAgent", "null");
            log.add("isreged", false);
            regStatus.setIsReged(false);
        } else {
            log.add("original agent status", agent.getAgent().getStatus());
            ObjCoreStatus objCoreStatus = new ObjCoreStatus(agent.getAgent().getStatus(), log);
            regStatus.setIsReged(objCoreStatus.isReged);
            regStatus.setIsActive(objCoreStatus.isActivated);
            regStatus.setIsStopped(objCoreStatus.isStopped);
            regStatus.setIsSuppend(objCoreStatus.isSuspended);
            regStatus.setIsFrozen(objCoreStatus.isFrozen);

        }

        momoMsg.cmdBody = regStatus.build().toByteArray();
        message.reply(momoMsg.toBuffer());
        log.writeLog();
    }

    public void doAgentModifyExtra(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.AgentInfoModify request;
        try {
            request = SoapProto.AgentInfoModify.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            request = null;
        }

        String phoneNumber = "0" + momoMsg.cmdPhone;
        List<SoapProto.keyValuePair> keyValuePairList = new ArrayList<>();

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMapInSoap(request.getKvpsList());
        if (hashMap != null && hashMap.containsKey(Const.REFERAL)) {
            keyValuePairList.add(SoapProto.keyValuePair.newBuilder()
                    .setKey(Const.REFERAL)
                    .setValue(hashMap.get(Const.REFERAL)).build());
        }

        if (request != null) {
            Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + momoMsg.cmdPhone);
            StandardBizResponse response = mSoapProcessor.modifyAgentInfoExtra(phoneNumber, keyValuePairList, log);

            boolean result = false;
            if (response != null && response.getResult() == 0) {
                result = true;
            }

            log.writeLog();
            message.reply(result);
        } else {
            message.reply(false);
        }
    }


    public void doAgentModify(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.AgentInfoModify request;
        try {
            request = SoapProto.AgentInfoModify.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            request = null;
        }

        String retailer = "";
        String isnamed = "";
        String cusnum = "";

        HashMap<String, String> hashMap = Misc.buildKeyValueHashMapInSoap(request.getKvpsList());
        if (hashMap != null && hashMap.containsKey(Const.DGD.Retailer)) {
            retailer = hashMap.get(Const.DGD.Retailer);
        }

        if (hashMap != null && hashMap.containsKey(Const.DGD.IsNamed)) {
            isnamed = hashMap.get(Const.DGD.IsNamed);
        }

        if (hashMap != null && hashMap.containsKey(Const.DGD.CusNumber)) {
            cusnum = hashMap.get(Const.DGD.CusNumber);
        }

        String phoneNumber = "0" + momoMsg.cmdPhone;

        List<SoapProto.keyValuePair> keyValuePairList = new ArrayList<>();

        boolean isRetailer = (Const.DGDValues.Retailer == DataUtil.strToInt(retailer)
                && Const.DGDValues.IsNamed == DataUtil.strToInt(isnamed)
                && DataUtil.strToInt(cusnum) > 0);


        if (isRetailer) {
            phoneNumber = "0" + DataUtil.strToInt(cusnum);
            keyValuePairList.add(SoapProto.keyValuePair.newBuilder()
                    .setKey("retailer")
                    .setValue("0" + momoMsg.cmdPhone).build());
        }

        if (hashMap != null && hashMap.containsKey(Const.REFERAL)) {
            keyValuePairList.add(SoapProto.keyValuePair.newBuilder()
                    .setKey(Const.REFERAL)
                    .setValue(hashMap.get(Const.REFERAL)).build());
        }
        // Them key value pair WaitReg = false (0)
        logger.info("key" + StringConstUtil.WAIT_REGISTER + "  " + "value --->" + "0");
        keyValuePairList.add(SoapProto.keyValuePair.newBuilder()
                .setKey(StringConstUtil.WAIT_REGISTER)
                .setValue("0").build());

        if (request != null) {
            AgentInfo agentInfo = mSoapProcessor.getAgentInfo(phoneNumber);
            if (agentInfo == null) {
                message.reply(false);
                return;
            }
            final Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber("0" + momoMsg.cmdPhone);

            ObjCoreStatus os = new ObjCoreStatus(agentInfo.getStatus(), log);
            StandardBizResponse response = mSoapProcessor.modifyAgentInfo(phoneNumber
                    , os.isReged
                    , os.isActivated
                    , request.getName()
                    , request.getDob()
                    , request.getAddress()
                    , request.getCardId()
                    , ((agentInfo.getFullName() == null ? "" : agentInfo.getFullName()))
                    , request.getEmail()
                    , Const.CHANNEL_MOBI
                    , keyValuePairList
                    , log);

            boolean result_noname = false;
            boolean result_tqt = false;
            if (response != null && response.getResult() == 0) {

                //phan lam cho diem giao dich
                if (isRetailer) {
                    final CreatesessionResponseType csrt = SessionManager.getCsrt(log.getTime());
                    if (csrt != null && !"".equalsIgnoreCase(csrt.getSessionid())) {

                        //Unmap khoi group noname
                        result_noname = mSoapProcessor.unMapNoNameGroup(csrt.getSessionid()
                                , phoneNumber
                                , noname_group_id
                                , log);
                        log.add("**********", "result unmap noname: " + result_noname);
                        //Unmap khoi group TQT
                        result_tqt = mSoapProcessor.unMapNoNameGroup(csrt.getSessionid()
                                , phoneNumber
                                , tqt_group_id
                                , log);
                        log.add("**********", "result unmap tqtgroup: " + result_tqt);
                        if(result_noname)
                        {
                            setGroupMoMo(phoneNumber);
                        }
//                         if(result_tqt || result_noname)
//                         {
//                             setGroupMoMo(phoneNumber);
//                         }
//                        JsonObject jsonSearch = new JsonObject();
//                        final String phone = phoneNumber;
//                        jsonSearch.putString(colName.GroupManage.NUMBER, phone);
//                        if (result) {
//                            log.add("unmap no name group", result);
//                            groupManageDb.searchWithFilter(jsonSearch, new Handler<ArrayList<GroupManageDb.Obj>>() {
//                                @Override
//                                public void handle(ArrayList<GroupManageDb.Obj> objs) {
//                                    if (objs != null && objs.size() > 0) {
//                                        boolean result = false;
//                                        for (GroupManageDb.Obj obj : objs) {
//                                            result = mSoapProcessor.unMapNoNameGroup(csrt.getSessionid()
//                                                    , phone
//                                                    , DataUtil.strToLong(obj.groupid)
//                                                    , log);
//                                            if (!result) {
//                                                log.add("**********", "unmap tu core, ket qua tra ve khong thanh cong");
//                                                log.writeLog();
//                                                break;
//                                            }
//                                        }
//                                        if (result) {
//                                            log.add("**********", "unmap thanh cong, set vao group momo");
//                                            setGroupMoMo(phone);
//                                            log.writeLog();
//                                        }
//                                    } else {
//                                        log.add("**********", "khong co trong group khac, dinh danh luon");
//                                        setGroupMoMo(phone);
//                                        log.writeLog();
//                                    }
//                                }
//                            });
//                        }
                    } else {
                        log.add("**********", "khong lay duoc session de bo vi dinh danh cho khach hang");
                    }
                } else {
                    result_noname = true;
                }
            }

            log.writeLog();
            message.reply(result_noname);
        } else {
            message.reply(false);
        }
    }

    private void setGroupMoMo(final String phoneNumber) {
        JsonObject jsonServerConf = new JsonObject();
        String MOMO_GROUP = "";
        String MOMO_CAPSET_ID = "";
        String MOMO_UPPER_LIMIT = "";
        if (glbCfg != null) {
            jsonServerConf = glbCfg.getObject(StringConstUtil.SERVER, null);
            MOMO_GROUP = jsonServerConf.getString(StringConstUtil.MOMO_GROUP, "");
            MOMO_CAPSET_ID = jsonServerConf.getString(StringConstUtil.MOMO_CAPSET_ID, "");
            MOMO_UPPER_LIMIT = jsonServerConf.getString(StringConstUtil.MOMO_UPPER_LIMIT, "");
        }

        if (jsonServerConf != null && !MOMO_GROUP.equalsIgnoreCase("") && !MOMO_CAPSET_ID.equalsIgnoreCase("") && !MOMO_UPPER_LIMIT.equalsIgnoreCase("")) {
            final Buffer buffer = MomoMessage.buildBuffer(SoapProto.MsgType.MAP_AGENT_TO_VISA_GROUP_VALUE
                    , 0
                    , DataUtil.strToInt(phoneNumber)
                    , SoapProto.ZaloGroup.newBuilder()
                    .setZaloGroup(MOMO_GROUP)
                    .setZaloCapsetId(MOMO_CAPSET_ID)
                    .setZaloUpperLimit(MOMO_UPPER_LIMIT)
                    .build().toByteArray());
            vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<Message<Boolean>>() {
                @Override
                public void handle(Message<Boolean> reply) {
                    logger.info(DataUtil.strToInt(phoneNumber) + ",set group momo " + reply.body());
                }
            });
        }

    }

    public void getAgentInfo(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.AgentInfo request;
        int result_code = MomoProto.SystemError.ALL_OK_VALUE;

        try {
            request = SoapProto.AgentInfo.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            result_code = MomoProto.SystemError.SYSTEM_ERROR_VALUE;
            request = null;
        }

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);
        log.add("function", "getAgentInfo");

        if (request != null && request.hasNumber()) {
            String number = String.valueOf(request.getNumber());

            AgentInfo agentInfo = mSoapProcessor.getAgentInfo(number);

            Buffer buffer = null;
            SoapProto.GetAgentInfoReply reply = null;
            if (agentInfo != null) {

                //Registered=1, Active=2, Stopped=128, Suspended=8, Frozen=32.
                log.add("agentInfo.getStatus()", agentInfo.getStatus());
                ObjCoreStatus objCoreStatus = new ObjCoreStatus(agentInfo.getStatus(), log);

                //set lai isReged theo isFrozen
                boolean isReged = (objCoreStatus.isFrozen ? false : objCoreStatus.isReged);

                reply = SoapProto.GetAgentInfoReply.newBuilder()
                        .setResult(true)
                        .setAgentId(agentInfo.getAgentid())
                        .setName(agentInfo.getFullName())
                        .setCardId(agentInfo.getPersonalID())
                        .setMomo(agentInfo.getMomoBalance().doubleValue())
                        .setMload(agentInfo.getMloadBalance().doubleValue())
                        .setEmail(agentInfo.getEmail())
                        .setCreatedDate(agentInfo.getCreatedDate().getTime())
                        .setRegStatus(SoapProto.RegStatus.newBuilder()
                                        .setIsSetup(false)
                                        .setIsReged(isReged)
                                        .setIsNamed(agentInfo.getIsNamed())
                                        .setIsActive(objCoreStatus.isActivated)
                                        .setIsFrozen(objCoreStatus.isFrozen)
                                        .setIsSuppend(objCoreStatus.isSuspended)
                                        .setIsStopped(objCoreStatus.isStopped)
                                        .build()
                        )

                        .setAddress(agentInfo.getAddress())
                        .setDateOfBirth(agentInfo.getDateOfBirth())
                        .setBankName(agentInfo.getBank_name())
                        .setBankAcc(agentInfo.getBank_acc_no())
                        .setBankCode(agentInfo.getBank_code())
                        .build();

                buffer = MomoMessage.buildBuffer(
                        MomoProto.MsgType.PHONE_EXIST_REPLY_VALUE
                        , momoMsg.cmdIndex
                        , momoMsg.cmdPhone
                        , reply.toByteArray());
            } else {

                buffer = MomoMessage.buildBuffer(
                        MomoProto.MsgType.PHONE_EXIST_REPLY_VALUE
                        , momoMsg.cmdIndex
                        , momoMsg.cmdPhone
                        , SoapProto.GetAgentInfoReply.newBuilder()
                                .setResult(false)
                                .setRcode(SoapProto.GetAgentInfoReply.ResultCode.AGENT_NOT_FOUND_VALUE)
                                .build().toByteArray()
                );
            }

            message.reply(buffer);
            log.writeLog();
        }
    }

    public void register(final Message message) {

        final MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.Register register;
        int resultToClient = 0;
        boolean isNewUser = false;
        JsonObject jsonRegisterInfo = new JsonObject();
        try {
            register = SoapProto.Register.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            resultToClient = MomoProto.SystemError.SYSTEM_ERROR_VALUE;
            register = null;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);

        if (resultToClient == MomoProto.SystemError.ALL_OK_VALUE) {

            String number = "0" + String.valueOf(momoMsg.cmdPhone);

            log.setPhoneNumber(number);

            log.add("begin call", "getAgentStatus");


            HashMap<String, String> hashMap = Misc.buildKeyValueHashMapInSoap(register.getKvpsList());

            String waitingReg = hashMap.containsKey("waitregister") ? hashMap.get("waitregister") : "0";

            log.add("waitregister", waitingReg);

            //is waiting register by named from mapping wallet via bank
            if ("1".equalsIgnoreCase(waitingReg)) {

                log.add("regDesc", "only reset password by client");
                StandardBizResponse resetPinResp = mSoapProcessor.resetPIN(number, register.getPin());
                resultToClient = resetPinResp == null ? SoapError.SYSTEM_ERROR : resetPinResp.getResult();

                JsonObject reply = new JsonObject();
                reply.putNumber("result", resultToClient);
                reply.putBoolean("isaddnew", false);
                message.reply(reply);

                return;
            }

            AgentResponseType agent = mSoapProcessor.getAgentStatus(number);

            ObjCoreStatus objCoreStatus = null;

            if (agent == null || agent.getAgent() == null) {
                isNewUser = true;
                log.add("agent or agent.getAgent()", "null");
            } else {
                log.add("original agent status", agent.getAgent().getStatus());
                objCoreStatus = new ObjCoreStatus(agent.getAgent().getStatus(), log);
                isNewUser = !objCoreStatus.isReged;
            }

            //kiem tra them neu tai khoan da dang ky va dang tam khoa thi tra ve luon
            if (objCoreStatus != null && objCoreStatus.isReged && (!objCoreStatus.isActivated)) {
                log.add("desc", "tai khoan da dang ky nhung dang bi tam khoa");
                JsonObject reply = new JsonObject();
                reply.putNumber("result", SoapError.AGENT_INACTIVE);
                reply.putBoolean("isaddnew", false);
                message.reply(reply);

                log.writeLog();
                return;
            }

            RegisterInfo obj = new RegisterInfo();
            obj.setPhone(number);
            obj.setRegisterNew(isNewUser);//dang ky moi/chinh sua
            obj.setNewPin(register.getPin());

            if (objCoreStatus != null) {

                AgentInfo agentInfo = mSoapProcessor.getAgentInfo(number);
                if (agentInfo != null) {

                    if (!agentInfo.getFullName().isEmpty()) {
                        obj.setName(agentInfo.getFullName());
                    }

                    if (!agentInfo.getPersonalID().isEmpty()) {
                        obj.setPersional_id(agentInfo.getPersonalID());
                    }

                    if (!agentInfo.getEmail().isEmpty()) {
                        obj.setEmail(agentInfo.getEmail());
                    }

                    if (!agentInfo.getAddress().isEmpty()) {
                        obj.setProvince(agentInfo.getAddress());
                    }

                    if (!agentInfo.getDateOfBirth().isEmpty()) {
                        obj.setDateOfBirth(agentInfo.getDateOfBirth());
                    }
                    obj.setIsNamed(agentInfo.getIsNamed());
                    List<SoapProto.keyValuePair> keyValuePairList = new ArrayList<>();
                    if(isNewUser)
                    {
                        log.add("desc", "co them bank code bang 0 ne");
                        keyValuePairList.add(SoapProto.keyValuePair.newBuilder()
                                .setKey(StringConstUtil.WAIT_REGISTER)
                                .setValue("0").setKey(StringConstUtil.BANK_CODE).setValue("0").build());
                    }
                    else{
                        keyValuePairList.add(SoapProto.keyValuePair.newBuilder()
                            .setKey(StringConstUtil.WAIT_REGISTER)
                            .setValue("0").build());
                    }


                    log.add("run via", "modifyAgentInfo");
                    log.writeLog();
                    mSoapProcessor.modifyAgentInfo(number, true, true, agentInfo.getFullName(), agentInfo.getDateOfBirth(), agentInfo.getAddress(),
                            agentInfo.getPersonalID(), agentInfo.getContact_no(), agentInfo.getEmail(), Const.CHANNEL_MOBI, keyValuePairList, log);
                    logger.info("run before end modifyAgentInfo");
                }

            } else {
//                AgentInfo agentInfo = mSoapProcessor.getAgentInfo(number);
//                String agentName = "";
//                String agentAddress = "";
//                String agentCardId = "";
//                String agentDOB = "";
//                if(agentInfo != null)
//                {
//                    agentName = (agentInfo.getFullName() != null && !agentInfo.getFullName().equalsIgnoreCase("")) ? agentInfo.getFullName() : "";
//                    agentAddress = (agentInfo.getAddress() != null && !agentInfo.getAddress().equalsIgnoreCase("")) ? agentInfo.getAddress() : "";
//                    agentCardId = (agentInfo.getBank_acc_no() != null && !agentInfo.getBank_acc_no().equalsIgnoreCase("")) ? agentInfo.getBank_acc_no() : "";
//                    agentDOB = (agentInfo.getDate_of_birth() != null && !agentInfo.getDate_of_birth().equalsIgnoreCase("")) ? agentInfo.getDate_of_birth() : "";
//                }
//                log.add("agentName --->", agentName);
//                log.add("agentAddress --->", agentAddress);
//                log.add("agentDOB --->", agentDOB);
//                log.add("agentCardId --->", agentCardId);
//
//                String name = !agentName.equalsIgnoreCase("") ? agentName : register.getName();
//                String add = !agentAddress.equalsIgnoreCase("") ? agentAddress : register.getAddress();
//                String cardId = !agentCardId.equalsIgnoreCase("") ? agentCardId : register.getIdCard();
//                String dob = !agentDOB.equalsIgnoreCase("") ? agentDOB : register.getDateOfBirth();
//
//                obj.setName(name);
//                obj.setPersional_id(cardId);
//                obj.setEmail(register.getEmail());
//                obj.setProvince(add);
//                obj.setDateOfBirth(dob);
                obj.setName(register.getName());
                obj.setPersional_id(register.getIdCard());
                obj.setEmail(register.getEmail());
                obj.setProvince(register.getAddress());
                obj.setDateOfBirth(register.getDateOfBirth());
                obj.setIsNamed(false);
            }
            logger.info("run before end getAgentStatus");
            jsonRegisterInfo = obj.toJsonObject();
            log.add("end call", "getAgentStatus");

            String[] arr_group = register.getArrGroup().split(",");
            String[] arr_capset = register.getArrCapset().split(",");
            String upperLimit = register.getUpperLimit();

            HashMap<String, String> kvps = new HashMap<>();
            ;
            log.add("kvps size", register.getKvpsCount());
            if (register.getKvpsCount() > 0) {
                for (int i = 0; i < register.getKvpsCount(); i++) {
                    kvps.put(register.getKvps(i).getKey()
                            , register.getKvps(i).getValue());
                }
            }


            //Them bank code bang 0 cho luong can bang tien.
            if (isNewUser || !objCoreStatus.isReged) {
                kvps.put(StringConstUtil.BANK_CODE, "0");
            }
            //End
            log.add("kvps ----->", kvps.size());
            log.add("kvps content ----->", kvps.toString());
            log.add("is New ----->", isNewUser);
            log.add("kvps bankcode ----->", kvps.get(StringConstUtil.BANK_CODE));

            if (mSoapProcessor.doRegisterMS(obj
                    , arr_group
                    , arr_capset
                    , upperLimit
                    , "MoMoApp register"
                    , register.getChannel(), kvps, log)) {

                resultToClient = MomoProto.SystemError.ALL_OK_VALUE;
            } else {
                resultToClient = MomoProto.SystemError.SYSTEM_ERROR_VALUE;
            }

            if (resultToClient == MomoProto.SystemError.ALL_OK_VALUE && isNewUser) {
                logger.info(momoMsg.cmdPhone + " registered from application.");
                StatisticModels.Action.Channel chanel = StatisticModels.Action.Channel.MOBILE;
                if (register.getChannel().equalsIgnoreCase(Const.CHANNEL_WEB)) {
                    chanel = StatisticModels.Action.Channel.WEB;
                }
                StatisticUtils.fireRegister(vertx.eventBus(), momoMsg.cmdPhone, chanel);
            }

        }


        //tra ket qua + thong tin dang ky moi/ hoac modify
        JsonObject reply = new JsonObject();
        reply.putNumber("result", resultToClient);
        reply.putBoolean("isaddnew", isNewUser);
        reply.putObject(colName.RegisterInfoCol.REGISTER_INFO, jsonRegisterInfo);

        message.reply(reply);


    }

    public void registerM2N(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.Register register;
        try {
            register = SoapProto.Register.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            register = null;
        }

        if (register == null) {
            message.reply(false);
            return;
        }

        String phone = "0" + momoMsg.cmdPhone;
        RegisterInfo obj = new RegisterInfo();

        obj.setPhone(phone);
        obj.setName("");
        obj.setPersional_id("");
        obj.setRegisterNew(true);
        obj.setNewPin("000000");
        obj.setEmail("");

        String[] arr_group = register.getArrGroup().split(",");
        String[] arr_capset = register.getArrCapset().split(",");
        String upperLimit = register.getUpperLimit();

        int rcode;

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(phone);
        log.add("function", " registerM2N");

        HashMap<String, String> kvps = null;
        if (register.getKvpsCount() > 0) {
            kvps = new HashMap<>();
            for (int i = 0; i < register.getKvpsCount(); i++) {
                kvps.put(register.getKvps(i).getKey(), register.getKvps(i).getValue());
            }
        }

        if (mSoapProcessor.doRegisterMS(obj
                , arr_group
                , arr_capset
                , upperLimit
                , "MoMoApp register"
                , register.getChannel(), kvps, log)) {
            rcode = MomoProto.SystemError.ALL_OK_VALUE;
        } else {
            rcode = MomoProto.SystemError.SYSTEM_ERROR_VALUE;
        }

        log.writeLog();

        message.reply(rcode);
    }


    public void setZaloGroup(Message message) {

        MomoMessage msg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.ZaloGroup zaloGroup;
        try {
            zaloGroup = SoapProto.ZaloGroup.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            zaloGroup = null;
        }

        if (zaloGroup == null) {
            message.reply(false);
            return;
        }

        String phone = "0" + msg.cmdPhone;
        long groupId = DataUtil.stringToUNumber(zaloGroup.getZaloGroup());
        long capsetId = DataUtil.stringToUNumber(zaloGroup.getZaloCapsetId());
        long upperLimit = DataUtil.stringToUNumber(zaloGroup.getZaloUpperLimit());

        StandardBizResponse groupResponse;

        groupResponse = mSoapProcessor.mapAgentToGroup("88888888"
                , phone
                , groupId);

        StandardBizResponse mapAgentResp = mSoapProcessor.mapAgentToCapset("88888888"
                , phone
                , capsetId
                , 1
                , upperLimit);

        int rcode = groupResponse.getResult();

        message.reply(rcode == 0 ? true : false);
    }

    public void setVisaGroup(Message message) {
        MomoMessage msg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.ZaloGroup visaGroup;
        try {
            visaGroup = SoapProto.ZaloGroup.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            visaGroup = null;
        }

        if (visaGroup == null) {
            message.reply(false);
            return;
        }

        String phone = "0" + msg.cmdPhone;
        String[] arr_group = visaGroup.getZaloGroup().split(",");
        long capsetId = DataUtil.stringToUNumber(visaGroup.getZaloCapsetId());
        long upperLimit = DataUtil.stringToUNumber(visaGroup.getZaloUpperLimit());
        StandardBizResponse groupResponse = new StandardBizResponse();
        for(String g : arr_group) {
            if (StringUtils.isEmpty(g))
                continue;
            long groupId = DataUtil.stringToUNumber(g);

            groupResponse = mSoapProcessor.mapAgentToGroup("77777777"
                    , phone
                    , groupId);
        }

        StandardBizResponse mapAgentResp = mSoapProcessor.mapAgentToCapset("77777777"
                , phone
                , capsetId
                , 1
                , upperLimit);
        final Common.BuildLog log = new Common.BuildLog(logger);
        int rcode = groupResponse.getResult();
//        if(rcode == 0)
//        {
//            final CreatesessionResponseType csrt = SessionManager.getCsrt(log.getTime());
//            if(csrt != null && !"".equalsIgnoreCase(csrt.getSessionid())) {
//                mSoapProcessor.unMapNoNameGroup(csrt.getSessionid()
//                        , phone
//                        , noname_group_id
//                        , log);
//            }
//        }
        message.reply(rcode == 0 ? true : false);
    }


    public void logIn(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.LogIn logIn;
        int result = MomoProto.LogInReply.ResultCode.ALL_OK_VALUE;

        try {
            logIn = SoapProto.LogIn.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            logIn = null;

            //khong parse duoc, xem nhu agent pin invalid
            result = MomoProto.LogInReply.ResultCode.PIN_INVALID_VALUE;
        }

        if (result == MomoProto.LogInReply.ResultCode.ALL_OK_VALUE) {
            String number = "0" + logIn.getNumber();

            long time = System.currentTimeMillis();
            SessionInfo sessionInfo = SessionManager.getCsrt(vertx, number, logIn.getMpin(), time);
            if (sessionInfo == null) {
                result = MomoProto.LogInReply.ResultCode.PIN_INVALID_VALUE; // agent pin invalid
            } else {
                result = sessionInfo.getLoginResult();
            }
        }

        message.reply(result);
    }

    public void getBillInfo(final Message message) {

        final MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());


        SoapProto.GetBillInfo getBillInfoReq;
        try {
            getBillInfoReq = SoapProto.GetBillInfo.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {

            getBillInfoReq = null;
        }

//        long amount = 0;
//        String billId = getBillInfoReq != null ? getBillInfoReq.getBillId() : "";
//        final int count = cacheBillCfg.getInteger(StringConstUtil.COUNT, 3);
//        final int duration = cacheBillCfg.getInteger(StringConstUtil.DURATION, 1);
        final SoapProto.GetBillInfo getBillInfoReq_tmp = getBillInfoReq;
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + String.valueOf(momoMsg.cmdPhone));
//        cacheBillInfoViaCoreDb.findOne("0" + momoMsg.cmdPhone, new Handler<CacheBillInfoViaCoreDb.Obj>() {
//            @Override
//            public void handle(CacheBillInfoViaCoreDb.Obj cacheBillObj) {

                int result_code;
                String billInfoStr = "";
                String number = "0" + String.valueOf(momoMsg.cmdPhone);
                String description  = "";

//                if (cacheBillObj != null && System.currentTimeMillis() < cacheBillObj.againCheckTime) {
//                    log.add("value of cacheBillObj", cacheBillObj.toJson());
////                    log.add("count", cacheBillObj.count);
//                    log.add("again check time", cacheBillObj.againCheckTime);
//                    log.add("cacheBillInfoViaCoreDb", "Check lien tuc nen khong cho check");
////                    JsonObject jsonReply = new JsonObject();
////                    jsonReply.putString("billInfo", cacheBillObj.billInfo);
////                    jsonReply.putNumber("rcode", cacheBillObj.rcode);
////                    message.reply(jsonReply);
////
////                    cacheBillObj.count = cacheBillObj.count + 1;
////                    cacheBillObj.checkedTime = System.currentTimeMillis();
////                    cacheBillInfoViaCoreDb.updatePartial(cacheBillObj.billId, cacheBillObj.toJson(), new Handler<Boolean>() {
////                        @Override
////                        public void handle(Boolean aBoolean) {
////                            log.add("result", aBoolean);
////                            log.writeLog();
////                        }
////                    });
//                    log.writeLog();
//                    return;
//                }

                if (getBillInfoReq_tmp != null) {

                    StandardMSResponse response = mSoapProcessor.getBillInfo(number
                            , getBillInfoReq_tmp.getProviderId()
                            , getBillInfoReq_tmp.getBillId());

                    if (response == null) {
                        result_code = MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE;
                    } else {
                        //Pham Thanh Duc,,~HNG1798612,379500,28/02/2014,28/02/2014~
                        if (response.getResultCode() == 0) {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.ALL_OK_VALUE;
                            //logger.info("Bill infor result : " + response.getResultName());

                            billInfoStr = response.getResultName();
                            description = response.getDescription();
                            log.add("description checkinfo ", description);
//                            controlTimeToCheckBillViaCore(getBillInfoReq_tmp, number, billInfoStr, result_code, new JsonObject(), log, duration);
//                    amount = response.getDebitAmount();
                        } else if (response.getResultCode() == 13) {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.WRONG_AMOUNT_VALUE;
                        } else if (response.getResultCode() == 1023) {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.WRONG_ACCOUNT_ID_VALUE;
                        } else {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE;
                        }
                    }
                } else {
                    result_code = MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE;
                }

                JsonObject obj;
                if (result_code == MomoProto.GetBillInfoReply.ResultCode.ALL_OK_VALUE) {
                    obj = new JsonObject();
                    obj.putString("billInfo", billInfoStr);
                    obj.putNumber("rcode", result_code);
                    if(!Misc.isNullOrEmpty(description)){
                        obj.putString("description", description);
                    }

                } else {
                    obj = new JsonObject();
                    obj.putString("billInfo", "");
                    obj.putNumber("rcode", MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE);

                }
                log.writeLog();
                message.reply(obj);
//            }
//        });


    }

    public void controlTimeToCheckBillViaCore(final SoapProto.GetBillInfo getBillInfoReq, final String number, final String billInfoStr, final int result_code, final JsonObject jsonResult, final Common.BuildLog log, final int duration) {

        CacheBillInfoViaCoreDb.Obj cacheObj = new CacheBillInfoViaCoreDb.Obj();
        cacheObj.billId = number;
//        cacheObj.providerId = getBillInfoReq.getProviderId();
        cacheObj.number = number;
        cacheObj.checkedTime = System.currentTimeMillis();
//        cacheObj.billInfo = billInfoStr;
//        cacheObj.rcode = result_code;
        cacheObj.againCheckTime = System.currentTimeMillis() + 1000L * 60 * duration;
//        cacheObj.count = 0;
//        cacheObj.jsonResult = jsonResult;
        log.add("cachObj", cacheObj.toJson());
//        cacheBillInfoViaCoreDb.updatePartial(cacheObj.billId, cacheObj.toJson(), new Handler<Boolean>() {
//            @Override
//            public void handle(Boolean aBoolean) {
//
//            }
//        });
    }

//    public void cachedBillViaCore(final SoapProto.GetBillInfo getBillInfoReq, final String number, final String billInfoStr, final int result_code, final JsonObject jsonResult, final Common.BuildLog log, final int duration) {
//        Common.ServiceReq serviceReq = new Common.ServiceReq();
//        serviceReq.ServiceId = getBillInfoReq.getProviderId();
//        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_BY_SERVICE_ID;
//
//        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
//            @Override
//            public void handle(final JsonArray objects) {
//
//                if (objects != null && objects.size() > 0) {
//                    log.add("size", objects.size());
//                    boolean isCachedBill = ((JsonObject) objects.get(0)).getBoolean(colName.ServiceCols.HAS_CACHED_BILL, false);
//                    if (isCachedBill) {
//                        CacheBillInfoViaCoreDb.Obj cacheObj = new CacheBillInfoViaCoreDb.Obj();
//                        cacheObj.billId = getBillInfoReq.getBillId();
//                        cacheObj.providerId = getBillInfoReq.getProviderId();
//                        cacheObj.number = number;
//                        cacheObj.checkedTime = System.currentTimeMillis();
//                        cacheObj.billInfo = billInfoStr;
//                        cacheObj.rcode = result_code;
//                        cacheObj.againCheckTime = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * duration;
//                        cacheObj.count = 0;
//                        cacheObj.jsonResult = jsonResult;
//                        log.add("cachObj", cacheObj.toJson());
////                        cacheBillInfoViaCoreDb.updatePartial(cacheObj.billId, cacheObj.toJson(), new Handler<Boolean>() {
////                            @Override
////                            public void handle(Boolean aBoolean) {
////
////                            }
////                        });
//                    }
//
//                }
//            }
//        });
//    }

    public void getBillInfoByService(final Message message) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "getBillInfoByService");

        final MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        int result_code;

        SoapProto.GetBillInfo getBillInfoReq;
        String number = "0" + String.valueOf(momoMsg.cmdPhone);

        log.setPhoneNumber(number);
        try {
            getBillInfoReq = SoapProto.GetBillInfo.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {

            getBillInfoReq = null;
        }

//        String billId = getBillInfoReq != null ? getBillInfoReq.getBillId() : "";
//        final int count = cacheBillCfg.getInteger(StringConstUtil.COUNT, 3);
//        final int duration = cacheBillCfg.getInteger(StringConstUtil.DURATION, 1);
        final SoapProto.GetBillInfo getBillInfoReq_tmp = getBillInfoReq;
        log.setPhoneNumber("0" + String.valueOf(momoMsg.cmdPhone));
//        cacheBillInfoViaCoreDb.findOne(number, new Handler<CacheBillInfoViaCoreDb.Obj>() {
//            @Override
//            public void handle(CacheBillInfoViaCoreDb.Obj cacheBillObj) {
                JsonObject jsonResult = null;
//                String billInfoStr = "";
//                if (cacheBillObj != null && System.currentTimeMillis() < cacheBillObj.againCheckTime) {
//                    log.add("value of cacheBillObj", cacheBillObj.toJson());
//                    log.add("count", cacheBillObj.count);
//                    log.add("again check time", cacheBillObj.againCheckTime);
//                    log.add("cacheBillObj.jsonResult", cacheBillObj.jsonResult);
//
////                    message.reply(cacheBillObj.jsonResult);
////
////                    cacheBillObj.count = cacheBillObj.count + 1;
////                    cacheBillObj.checkedTime = System.currentTimeMillis();
////                    cacheBillInfoViaCoreDb.updatePartial(cacheBillObj.billId, cacheBillObj.toJson(), new Handler<Boolean>() {
////                        @Override
////                        public void handle(Boolean aBoolean) {
////                            log.add("result", aBoolean);
////                            log.writeLog();
////                        }
////                    });
//                    log.writeLog();
//                    return;
//                }

                if (getBillInfoReq_tmp != null) {

                    try {
                        jsonResult = mSoapProcessor.getBillInfoByService(number
                                , getBillInfoReq_tmp.getProviderId()
                                , getBillInfoReq_tmp.getBillId());
                    } catch (Exception ex) {
                        jsonResult = null;
                        log.add("getBillInfoByService timeout", "");
                    }

                    log.add("json response from SoapInProcess.getBillInfoByService", (jsonResult == null ? "null" : jsonResult));
            /*jo.putNumber("rcode", response.getResultCode());
            jo.putObject("json_result",billInfoService.toJsonObject());*/

                    if (jsonResult == null) {
                        result_code = MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE;
                    } else {
                        int rcode = jsonResult.getInteger("rcode", MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE);

                        //Pham Thanh Duc,,~HNG1798612,379500,28/02/2014,28/02/2014~
                        if (rcode == 0) {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.ALL_OK_VALUE;
//                            controlTimeToCheckBillViaCore(getBillInfoReq_tmp, "0" + number, billInfoStr, result_code, jsonResult, log, duration);
                        } else if (rcode == 13) {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.WRONG_AMOUNT_VALUE;
                        } else if (rcode == 1023) {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.WRONG_ACCOUNT_ID_VALUE;
                        } else {
                            result_code = MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE;
                        }
                    }
                } else {
                    result_code = MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE;
                }

                if (jsonResult == null) {
                    jsonResult = new JsonObject();
                }

                if (result_code == MomoProto.GetBillInfoReply.ResultCode.ALL_OK_VALUE) {
                    jsonResult.putNumber("rcode", result_code);

                } else {
                    jsonResult.putNumber("rcode", MomoProto.GetBillInfoReply.ResultCode.SYSTEM_ERROR_VALUE);
                }

                log.add("json reply to Server Verticle", jsonResult);
                log.writeLog();
                message.reply(jsonResult);
//            }
//        });
    }

    public void getBillInfoByServiceTEST(final Message message)
    {
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "getBillInfoByService");
        String avg_demo = "{\"rcode\":0,\"json_result\":{\"total_amount\":0,\"customer_info\":[{\"text\":\"ht\",\"value\":\"Nguyễn Văn  Cường ( Đlý Toàn Thủy )\"},{\"text\":\"sdt\",\"value\":\"01287761898\"},{\"text\":\"dc\",\"value\":\"132 Lý Thái Tổ , Phường An Hòa, Thành phố Huế, Thừa Thiên - Huế\"},{\"text\":\"sn\",\"value\":\"DTH VN Cao cấp\"}],\"array_price\":[{\"text\":\"3 tháng - trị giá 264.000đ\",\"value\":\"264000\"},{\"text\":\"6 tháng - trị giá 528.000đ\",\"value\":\"528000\"},{\"text\":\"12 tháng - trị giá 1.056.000đ\",\"value\":\"1056000\"},{\"text\":\"18 tháng - trị giá 1.584.000đ\",\"value\":\"1584000\"},{\"text\":\"24 tháng - trị giá 2.112.000đ\",\"value\":\"2112000\"}]}}\n" +
                "2015-11-10 18:28:03| INFO|omo.gateway.internal.soapin.SoapVerticle-921394719|1447154877900|00|json reply to Server Verticle -> {\"rcode\":0,\"json_result\":{\"total_amount\":0,\"customer_info\":[{\"text\":\"ht\",\"value\":\"Nguyễn Văn  Cường ( Đlý Toàn Thủy )\"},{\"text\":\"sdt\",\"value\":\"01287761898\"},{\"text\":\"dc\",\"value\":\"132 Lý Thái Tổ , Phường An Hòa, Thành phố Huế, Thừa Thiên - Huế\"},{\"text\":\"sn\",\"value\":\"DTH VN Cao cấp\"}],\"array_price\":[{\"text\":\"3 tháng - trị giá 264.000đ\",\"value\":\"264000\"},{\"text\":\"6 tháng - trị giá 528.000đ\",\"value\":\"528000\"},{\"text\":\"12 tháng - trị giá 1.056.000đ\",\"value\":\"1056000\"},{\"text\":\"18 tháng - trị giá 1.584.000đ\",\"value\":\"1584000\"},{\"text\":\"24 tháng - trị giá 2.112.000đ\",\"value\":\"2112000\"}]}}";
        JsonObject jsonResult_tmp = new JsonObject(avg_demo);
        log.add("json reply to Server Verticle", jsonResult_tmp);
        //log.add("json reply to Server Verticle", jsonResult);
        log.writeLog();
        message.reply(jsonResult_tmp);
    }

    //chuyen tien tu MM --> vcb/vtb
    public void doBankOut(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.BankOut bankOut;
        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE;
        long tranId = 0;
        boolean isOk = false;
        try {
            bankOut = SoapProto.BankOut.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            bankOut = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        HashMap<String, String> hm = Misc.getHashMapInSoap(bankOut.getKvpsList());
        long time = hm.containsKey("time") ? DataUtil.stringToUNumber(hm.get("time")) : System.currentTimeMillis();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);
        log.setTime(time);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardBizResponse response = mSoapProcessor.bankout(vertx, "0" + momoMsg.cmdPhone
                    , bankOut.getBankCode()
                    , bankOut.getMpin()
                    , BigDecimal.valueOf(bankOut.getAmount())
                    , bankOut.getChannel()
                    , log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResult();
                tranId = (response.getTransid() == null ? System.currentTimeMillis() : response.getTransid());
            }
        }

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {
            isOk = true;
        }

        if (isOk && (divider_bank_out > 0)) {

            incPoint(momoMsg.cmdPhone, bankOut.getAmount(), divider_bank_out, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, bankOut.getAmount(), -1);
        message.reply(replyObj);

        log.writeLog();

    }

    //chuyen tien tu vcb/vtb->MM
    public void doBankIn(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());
        SoapProto.BankIn bankIn;
        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;
        boolean isOk = false;

        try {
            bankIn = SoapProto.BankIn.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException ", e);
            bankIn = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        HashMap<String, String> hm = Misc.getHashMapInSoap(bankIn.getKvpsList());
        long time = hm.containsKey("time") ? DataUtil.stringToUNumber(hm.get("time")) : System.currentTimeMillis();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);
        log.setTime(time);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {
            StandardBizResponse response = mSoapProcessor.bankin(vertx, "0" + momoMsg.cmdPhone
                    , bankIn.getBankCode()
                    , bankIn.getMpin()
                    , BigDecimal.valueOf(bankIn.getAmount())
                    , bankIn.getChannel()
                    , log);
            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResult();
                tranId = (response.getTransid() == null ? System.currentTimeMillis() : response.getTransid());
            }
        }

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {
            isOk = true;
        }

        if (isOk && (divider_bank_in > 0)) {
            incPoint(momoMsg.cmdPhone, bankIn.getAmount(), divider_bank_in, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, bankIn.getAmount(), 1);
        message.reply(replyObj);


        log.writeLog();
    }

    /*public void doM2CTransfer(CoreMessage message){

        MomoMessage msg = MomoMessage.fromBuffer( (Buffer)message.body());

        SoapProto.M2CTransfer  m2CTransfer;

        int result = MomoProto.TranHisV1. ResultCode.ALL_OK_VALUE ; // default, success
        long tranId = 0;

        try {
            m2CTransfer = SoapProto.M2CTransfer.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException",e);
            m2CTransfer = null;
            result = MomoProto.TranHisV1. ResultCode.SYSTEM_ERROR_VALUE;
        }

        if(result == MomoProto.TranHisV1. ResultCode.ALL_OK_VALUE){

            MsTransfer transfer = new MsTransfer();
            transfer.setAmount(BigDecimal.valueOf(m2CTransfer.getAmount()));
            transfer.setMessage(m2CTransfer.getNotice());
            transfer.setTarget(m2CTransfer.getPhone());
            transfer.setTargetid(m2CTransfer.getCardId());
            transfer.setTargetname(m2CTransfer.getName());

            StandardBizResponse response = mSoapProcessor.transferM2c(m2CTransfer.getAgent()
                                                                , m2CTransfer.getMpin()
                                                                , transfer,m2CTransfer.getChannel());

            if(response == null){
                result = MomoProto.TranHisV1. ResultCode.SYSTEM_ERROR_VALUE;
            }
            else {
                result = response.getResult();
                tranId =  response.getTransid();
            }

        }

        if( (result == MomoProto.TranHisV1. ResultCode.ALL_OK_VALUE) && (divider_m2c >0)){
            incPoint(msg.cmdPhone,m2CTransfer.getAmount(),divider_m2c);
        }

        JsonObject replyObj = CoreCommon.getJsonObjRpl(result
                                            ,tranId
                                            ,m2CTransfer.getAmount()
                                            ,-1);
        message.reply(replyObj);

    }*/
    public void doTransferMoney2Place(Message message) {
        MomoMessage msg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.TransferMoney2Place transferMoney2Place;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success

        try {
            transferMoney2Place = SoapProto.TransferMoney2Place.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            transferMoney2Place = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        if (result == MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE) {
            JsonObject replyObj = Misc.getJsonObjRpl(result, 0, 0, -1);
            message.reply(replyObj);
            return;
        }

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            String ownerNumber = transferMoney2Place.getAgent();
            String ownerPin = transferMoney2Place.getMpin();
            String toNumber = transferMoney2Place.getPhone();
            long amount = transferMoney2Place.getAmount();
            String channel = transferMoney2Place.getChannel();

            excuteTransfer(message, ownerPin, ownerNumber, toNumber, amount, channel, transferMoney2Place.getKvpsList());
        }
    }

    public void doM2MTransfer(final Message message) {
        final MomoMessage msg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.M2MTransfer m2MTransfer;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success

        try {
            m2MTransfer = SoapProto.M2MTransfer.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            m2MTransfer = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        //khong parse duoc message --> tra ve loi he thong
        if (m2MTransfer == null) {
            JsonObject replyObj = Misc.getJsonObjRpl(result, 0, 0, -1);
            message.reply(replyObj);
            return;
        }

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            //excuteTransfer(message, msg, m2MTransfer, fm2MTransfer);
            String ownerPin = m2MTransfer.getMpin();
            String ownerNumber = m2MTransfer.getAgent(); //"0" + DataUtil.strToInt(m2MTransfer.getAgent());
            String toNumber = m2MTransfer.getPhone(); //"0" + DataUtil.strToInt(m2MTransfer.getPhone());
            long amount = m2MTransfer.getAmount();
            String channel = m2MTransfer.getChannel();

            excuteTransfer(message, ownerPin, ownerNumber, toNumber, amount, channel, m2MTransfer.getKvpsList());
        }
    }

    public void doM2MerchantTransfer(final Message message) {
        final MomoMessage msg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.M2MTransfer m2MTransfer;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success

        try {
            m2MTransfer = SoapProto.M2MTransfer.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(msg + "InvalidProtocolBufferException", e);
            m2MTransfer = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        //khong parse duoc message --> tra ve loi he thong
        if (m2MTransfer == null) {
            JsonObject replyObj = Misc.getJsonObjRpl(result, 0, 0, -1);
            message.reply(replyObj);
            return;
        }

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            //excuteTransfer(message, msg, m2MTransfer, fm2MTransfer);
            String ownerPin = m2MTransfer.getMpin();
            String ownerNumber = m2MTransfer.getAgent(); //"0" + DataUtil.strToInt(m2MTransfer.getAgent());
            String toNumber = m2MTransfer.getPhone(); //"0" + DataUtil.strToInt(m2MTransfer.getPhone());
            long amount = m2MTransfer.getAmount();
            String channel = m2MTransfer.getChannel();

            excuteMerchantTransfer(message, ownerPin, ownerNumber, toNumber, amount, channel, m2MTransfer.getKvpsList());
        }
    }

    public void topUpAirTime(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        long tranId = 0;
        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE;

        SoapProto.TopUp topUpReq;
        try {
            topUpReq = SoapProto.TopUp.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            topUpReq = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        String from_number = "0" + topUpReq.getFromNumber();
        String to_number = "0" + topUpReq.getToNumber();
        String pin = (topUpReq.getMpin() == null ? "" : topUpReq.getMpin());
        long amount = topUpReq.getAmount();
        String channel = (topUpReq.getChannel() == null ? Const.CHANNEL_MOBI : topUpReq.getChannel());

        HashMap<String, String> hm = Misc.getHashMapInSoap(topUpReq.getKeyValuePairsList());
        long time = hm.containsKey("time") ? DataUtil.stringToUNumber(hm.get("time")) : System.currentTimeMillis();
        int wallettype = hm.containsKey("wallettype") ? Integer.valueOf(hm.get("wallettype")) : 1;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(from_number);
        log.setTime(time);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.topupAirtime(vertx
                    , from_number
                    , pin
                    , to_number
                    , amount
                    , wallettype
                    , channel
                    , topUpReq.getKeyValuePairsList()
                    , log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                if (response.getTransID() == null) {
                    tranId = System.currentTimeMillis();
                } else {
                    tranId = DataUtil.stringToUNumber(response.getTransID());
                }
            }

        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_top_up > 0)) {
            incPoint(momoMsg.cmdPhone, topUpReq.getAmount(), divider_top_up, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , topUpReq.getAmount()
                , -1);

        message.reply(replyObj);

        //thanh cong, khuyen mai gioi thieu ban be
        if (result == 0) {
            Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
            promoReqObj.COMMAND = Promo.PromoType.INVITE_FRIEND_GEN_CODE;
            promoReqObj.CREATOR = "0" + momoMsg.cmdPhone;
            promoReqObj.TRAN_TYPE = MomoProto.TranHisV1.TranType.TOP_UP_VALUE;
            promoReqObj.TRAN_AMOUNT = topUpReq.getAmount();
            log.add("function", "requestPromo");
            log.add("request promo invitefriend", promoReqObj.toJsonObject());

            Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {

                }
            });
        }
        log.writeLog();
    }


    public void topUpAirTimeString(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        long tranId = 0;
        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE;

        SoapProto.TopUpString topUpString;
        try {
            topUpString = SoapProto.TopUpString.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            topUpString = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(topUpString.getFromNumber());

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.topupAirtime(vertx
                    , topUpString.getFromNumber()
                    , topUpString.getMpin()
                    , topUpString.getToNumber()
                    , topUpString.getAmount()
                    , 1
                    , topUpString.getChannel()
                    , topUpString.getKeyValuePairsList()
                    , log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                if (response.getTransID() == null) {
                    tranId = System.currentTimeMillis();
                } else {
                    tranId = DataUtil.stringToUNumber(response.getTransID());
                }
            }

        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , topUpString.getAmount()
                , -1);

        message.reply(replyObj);

        log.writeLog();
    }


    public void doChangePin(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.ChangePin changePin;

        int result = MomoProto.SystemError.ALL_OK_VALUE; // default, success

        try {
            changePin = SoapProto.ChangePin.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            changePin = null;
            result = MomoProto.SystemError.SYSTEM_ERROR_VALUE;
        }

        if (result != MomoProto.SystemError.SYSTEM_ERROR_VALUE) {

            StandardBizResponse res = mSoapProcessor.changePIN(changePin.getNumber()
                    , changePin.getOldPin()
                    , changePin.getNewPin());

            result = res.getResult();
        }
        message.reply(result);
    }

    public void doRecoveryPin(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.RecoveryNewPin recoveryNewPin;

        int result = MomoProto.SystemError.ALL_OK_VALUE; // default, success

        try {
            recoveryNewPin = SoapProto.RecoveryNewPin.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            recoveryNewPin = null;
            result = MomoProto.SystemError.SYSTEM_ERROR_VALUE;
        }

        if (result != MomoProto.SystemError.SYSTEM_ERROR_VALUE) {

            StandardBizResponse res = mSoapProcessor.resetPINWithSms(recoveryNewPin.getNumber()
                    , recoveryNewPin.getNewPin());

            result = res.getResult();

        }
        message.reply(result);
    }

    //thanh toan hoa don dien
    public void doPayOneBill(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.PayOneBill payOneBill;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            payOneBill = SoapProto.PayOneBill.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            payOneBill = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        HashMap<String, String> hm = Misc.getHashMapInSoap(payOneBill.getKeyValuePairsList());
        long time = hm.containsKey("time") ? DataUtil.stringToUNumber(hm.get("time")) : System.currentTimeMillis();
        //wallet type for merchant web 1 momo, 2 mload
        int wallettype = hm.containsKey("wallettype") ? Integer.valueOf(hm.get("wallettype")) : 1;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);
        log.setTime(time);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardBizResponse response = mSoapProcessor.billPayChien("0" + momoMsg.cmdPhone
                    , payOneBill.getPin()
                    , payOneBill.getProviderId()
                    , payOneBill.getBillId()
                    , payOneBill.getAmount()
                    , wallettype, payOneBill.getChannel()
                    , payOneBill.getKeyValuePairsList()
                    , log
            );

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResult();
                tranId = (response.getTransid() == null ? System.currentTimeMillis() : response.getTransid());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_pay_one_bill > 0)) {
            incPoint(momoMsg.cmdPhone, payOneBill.getAmount(), divider_pay_one_bill, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , payOneBill.getAmount()
                , -1);

        message.reply(replyObj);

        //khuyen mai gioi thieu ban be
        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
            promoReqObj.COMMAND = Promo.PromoType.INVITE_FRIEND_GEN_CODE;
            promoReqObj.CREATOR = "0" + momoMsg.cmdPhone;
            promoReqObj.TRAN_TYPE = MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE;
            promoReqObj.TRAN_AMOUNT = payOneBill.getAmount();

            log.add("function", "requestPromo");
            log.add("request promo invitefriend", promoReqObj.toJsonObject());

            Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {

                }
            });
        }
        log.writeLog();
    }

    //thanh toan hoa don khac, -->lenh chuyen tien M2M den tai khoan nhan
    /*public void doPayOneBillOther(CoreMessage message){

        MomoMessage msg = MomoMessage.fromBuffer( (Buffer)message.body());

        SoapProto.PayOneBillOther  payOneBillOther;

        int result = MomoProto.TranHisV1. ResultCode.ALL_OK_VALUE ; // default, success
        long tranId = 0;

        try {
            payOneBillOther = SoapProto.PayOneBillOther.parseFrom(msg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException",e);
            payOneBillOther = null;
            result = MomoProto.TranHisV1. ResultCode.SYSTEM_ERROR_VALUE;
        }

        if(result == MomoProto.TranHisV1. ResultCode.ALL_OK_VALUE){
            StandardMSResponse response= mSoapProcessor.transfer(payOneBillOther.getAgent()
                    , payOneBillOther.getMpin()
                    , payOneBillOther.getPhone()
                    , payOneBillOther.getAmount()
                    , payOneBillOther.getNotice(),payOneBillOther.getChannel());

            if(response == null){
                result = MomoProto.TranHisV1. ResultCode.SYSTEM_ERROR_VALUE;
            }
            else {
                result = response.getResultCode();
                tranId = Long.valueOf(response.getTransID());
            }

        }

        if( (result == MomoProto.TranHisV1. ResultCode.ALL_OK_VALUE) && (divider_pay_one_bill_other >0)){
            incPoint(msg.cmdPhone,payOneBillOther.getAmount(),divider_pay_one_bill_other);
        }

        JsonObject replyObj = CoreCommon.getJsonObjRpl(result
                                        ,tranId
                                        ,payOneBillOther.getAmount()
                                        ,-1);
        message.reply(replyObj);
    }*/

    //nap tien game
    public void doTopUpGame(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.TopUpGame topUpGame;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            topUpGame = SoapProto.TopUpGame.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            topUpGame = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        //todo do some check
        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , topUpGame.getMpin()
                    , topUpGame.getProviderId()
                    , topUpGame.getGameAccount()
                    , topUpGame.getAmount()
                    , 1, topUpGame.getChannel()
                    , topUpGame.getKeyValuePairsList()
                    , log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }

        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_top_up_game > 0)) {
            incPoint(momoMsg.cmdPhone, topUpGame.getAmount(), divider_top_up_game, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, topUpGame.getAmount(), -1);
        message.reply(replyObj);

        //khuyen mai gioi thieu ban be
        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
            promoReqObj.COMMAND = Promo.PromoType.INVITE_FRIEND_GEN_CODE;
            promoReqObj.CREATOR = "0" + momoMsg.cmdPhone;
            promoReqObj.TRAN_TYPE = MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE;
            promoReqObj.TRAN_AMOUNT = topUpGame.getAmount();
            log.add("function", "requestPromo");
            log.add("request promo invitefrien", promoReqObj.toJsonObject());

            Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject jsonObject) {

                }
            });
        }
        log.writeLog();

    }

    public void doQuickDeposit(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.QuickDeposit quickDeposit;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;
        boolean isOk = false;
        try {
            quickDeposit = SoapProto.QuickDeposit.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            quickDeposit = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);
        //todo do some check
        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , quickDeposit.getPin()
                    , quickDeposit.getProviderId()
                    , quickDeposit.getBillId()
                    , quickDeposit.getAmount()
                    , 1, quickDeposit.getChannel(), null, log);
            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {
            isOk = true;
        }

        if (isOk && (divider_quick_deposit > 0)) {
            incPoint(momoMsg.cmdPhone, quickDeposit.getAmount(), divider_quick_deposit, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , quickDeposit.getAmount()
                , 1);
        message.reply(replyObj);

        log.writeLog();

    }


    //thanh toan nhanh --> chuyen tien M2M den tai khoan nguoi nhan
    public void doQuickPayment(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.QuickPayment quickPayment;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            quickPayment = SoapProto.QuickPayment.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error("InvalidProtocolBufferException", e);
            quickPayment = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        //todo do some check
        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , quickPayment.getPin()
                    , quickPayment.getProviderId()
                    , quickPayment.getBillId()
                    , quickPayment.getAmount()
                    , 1, quickPayment.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_quick_payment > 0)) {
            incPoint(momoMsg.cmdPhone, quickPayment.getAmount(), divider_quick_payment, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , quickPayment.getAmount()
                , -1);

        message.reply(replyObj);

        log.writeLog();
    }

    public void doBanknetAjustment(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.commonAdjust adjustMent;
        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            adjustMent = SoapProto.commonAdjust.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            adjustMent = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        StandardBizResponse response;
        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg);
        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            AdjustWalletResponse adjustWalletResponse = mSoapProcessor.adjustment(adjustMent.getSource()
                    , adjustMent.getTarget()
                    , BigDecimal.valueOf(adjustMent.getAmount())
                    , 1
                    , ""
                    , adjustMent.getExtraMapList()
                    , log);
            if (adjustWalletResponse == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                response = adjustWalletResponse.getAdjustWalletReturn();
                result = response.getResult();
                tranId = response.getTransid();
            }
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, adjustMent.getAmount(), 1);
        message.reply(replyObj);

        log.writeLog();
    }

    //chi danh rieng cho tra thuong truong trinh khuyen mai

    public void doMua123Adjustment(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.commonAdjust adjustMent;
        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            adjustMent = SoapProto.commonAdjust.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            adjustMent = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        StandardBizResponse response = null;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            AdjustWalletResponse adjustWalletResponse = mSoapProcessor.adjustment(adjustMent.getSource()
                    , adjustMent.getTarget()
                    , BigDecimal.valueOf(adjustMent.getAmount())
                    , 1
                    , ""
                    , adjustMent.getExtraMapList()
                    , log);

            if (adjustWalletResponse == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                response = adjustWalletResponse.getAdjustWalletReturn();
                result = response.getResult();
                tranId = response.getTransid();
            }
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, adjustMent.getAmount(), 1);
        message.reply(replyObj);

        log.writeLog();
    }


    public void traThuongZalo(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.commonAdjust adjustMent;
        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            adjustMent = SoapProto.commonAdjust.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            adjustMent = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        StandardBizResponse response = null;

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            AdjustWalletResponse adjustWalletResponse = mSoapProcessor.adjustment(adjustMent.getSource()
                    , adjustMent.getTarget()
                    , BigDecimal.valueOf(adjustMent.getAmount())
                    , 1
                    , ""
                    , adjustMent.getExtraMapList()
                    , log);

            if (adjustWalletResponse == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                response = adjustWalletResponse.getAdjustWalletReturn();
                result = response.getResult();
                tranId = response.getTransid();
            }
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, adjustMent.getAmount(), 1);
        message.reply(replyObj);

        log.writeLog();

    }

    //todo -> broadcast mpoint for data memory
    private void incPoint(final int number, final long amount, final int divider, Common.BuildLog log) {

        final long incPoint = (long) amount / divider;
        log.add("tang diem", incPoint);
        mPhoneDb.incMPoint(number, incPoint, new Handler<Boolean>() {
            @Override
            public void handle(Boolean o) {
            }
        });
    }

    private void LoadCfg(JsonObject config, Logger logger) {

        JsonObject tran_has_point_cfg = config.getObject("tran_has_point");

        divider_bank_in = tran_has_point_cfg.getInteger("bank_in", 0);
        divider_bank_out = tran_has_point_cfg.getInteger("bank_out", 0);
        divider_m2c = tran_has_point_cfg.getInteger("m2c", 0);
        divider_m2m = tran_has_point_cfg.getInteger("m2m", 0);
        divider_top_up = tran_has_point_cfg.getInteger("top_up", 0);
        divider_top_up_game = tran_has_point_cfg.getInteger("top_up_game", 0);
        divider_pay_one_bill = tran_has_point_cfg.getInteger("pay_one_bill", 0);
        divider_quick_deposit = tran_has_point_cfg.getInteger("quick_deposit", 0);
        divider_quick_payment = tran_has_point_cfg.getInteger("quick_payment", 0);
        divider_pay_one_bill_other = tran_has_point_cfg.getInteger("pay_one_bill_other", 0);
        divider_transfer_money_2_place = tran_has_point_cfg.getInteger("transfer_money_2_place", 0);

        divider_bill_pay_telephone = tran_has_point_cfg.getInteger("bill_pay_telephone", 0);
        divider_bill_pay_ticket_airline = tran_has_point_cfg.getInteger("bill_pay_ticket_airline", 0);
        divider_bill_pay_ticket_train = tran_has_point_cfg.getInteger("bill_pay_ticket_train", 0);
        divider_bill_pay_insurance = tran_has_point_cfg.getInteger("bill_pay_insurance", 0);
        divider_bill_pay_internet = tran_has_point_cfg.getInteger("bill_pay_internet", 0);
        divider_bill_pay_other = tran_has_point_cfg.getInteger("bill_pay_other", 0);

        divider_deposit_cash_other = tran_has_point_cfg.getInteger("deposit_cash_other", 0);
        divider_buy_mobility_card = tran_has_point_cfg.getInteger("buy_mobility_card", 0);
        divider_buy_game_card = tran_has_point_cfg.getInteger("buy_game_card", 0);
        divider_buy_other = tran_has_point_cfg.getInteger("buy_other", 0);

        //for session
        SessionManager.wsdl = config.getObject("soap").getString("soap_url", "http://172.16.18.50:8280/services/umarketsc?wsdl");
        SessionManager.initiator = config.getObject("soap").getString("soap_user_name", "admin_soapin");
        SessionManager.password = config.getObject("soap").getString("soap_password", "soap#access");
        SessionManager.sessionExpired = config.getObject("soap").getInteger("soap_session_expire", 6);
        SessionManager.logger = logger;

        //for m2m type
        JsonObject db_cfg = config.getObject("lstandby_database");
        String Driver = db_cfg.getString("driver");
        String Url = db_cfg.getString("url");
        String Username = db_cfg.getString("username");
        String Password = db_cfg.getString("password");

        JsonObject cfg = config.getObject("umarket_database");
        String driverPromo = cfg.getString("driver");
        String urlPromo = cfg.getString("url");
        String usernamePromo = cfg.getString("username");
        String passwordPromo = cfg.getString("password");

        String tpl = "DRIVER: %s URL: %s USERNAME: %s PASSWORD: %s";
        logger.info("PostCommitVerticle " + String.format(tpl, driverPromo, urlPromo, usernamePromo, passwordPromo));


//        dbProcessPromotion = new DBProcess(driverPromo
//                                            ,urlPromo
//                                            ,usernamePromo
//                                            ,passwordPromo
//                                            ,AppConstant.Promotion_ADDRESS
//                                            ,AppConstant.Promotion_ADDRESS
//                                            ,logger);
//
//        dbProcess = new DBProcess(Driver
//                                        ,Url
//                                        ,Username
//                                        ,Password
//                                        ,AppConstant.LStandbyOracleVerticle_ADDRESS
//                                        ,AppConstant.LStandbyOracleVerticle_ADDRESS
//                                        ,logger);
    }

    //app sms
    //.start
    //thanh toan cuoc dien thoai
    public void doBillPayTelephone(Message message) {

        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.BillPayTelephone billPayTelephone;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            billPayTelephone = SoapProto.BillPayTelephone.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            billPayTelephone = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , billPayTelephone.getPin()
                    , billPayTelephone.getProviderId()
                    , billPayTelephone.getPhone()
                    , billPayTelephone.getAmount()
                    , 1, billPayTelephone.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_bill_pay_telephone > 0)) {
            incPoint(momoMsg.cmdPhone, billPayTelephone.getAmount(), divider_bill_pay_telephone, log);
        }

        log.writeLog();

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , billPayTelephone.getAmount()
                , -1);
        message.reply(replyObj);
    }

    //thanh toan cuoc internet
    public void doBillPayInternet(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.BillPayInternet billPayInternet;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            billPayInternet = SoapProto.BillPayInternet.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            billPayInternet = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , billPayInternet.getPin()
                    , billPayInternet.getProviderId()
                    , billPayInternet.getCustomerAcc()
                    , billPayInternet.getAmount()
                    , 1, billPayInternet.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_bill_pay_internet > 0)) {
            incPoint(momoMsg.cmdPhone, billPayInternet.getAmount(), divider_bill_pay_internet, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , billPayInternet.getAmount()
                , -1);
        message.reply(replyObj);

        log.writeLog();
    }

    //thanh toan ve may bay
    public void doBillPayTicketAirline(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.BillPayTicketAirline billPayTicketAirline;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            billPayTicketAirline = SoapProto.BillPayTicketAirline.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            billPayTicketAirline = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , billPayTicketAirline.getPin()
                    , billPayTicketAirline.getProviderId()
                    , billPayTicketAirline.getTicketId()
                    , billPayTicketAirline.getAmount()
                    , 1, billPayTicketAirline.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_bill_pay_ticket_airline > 0)) {
            incPoint(momoMsg.cmdPhone, billPayTicketAirline.getAmount(), divider_bill_pay_ticket_airline, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , billPayTicketAirline.getAmount()
                , -1);
        message.reply(replyObj);

        log.writeLog();
    }

    //thanh toan ve tau
    public void doBillPayTicketTrain(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.BillPayTicketTrain billPayTicketTrain;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            billPayTicketTrain = SoapProto.BillPayTicketTrain.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            billPayTicketTrain = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , billPayTicketTrain.getPin()
                    , billPayTicketTrain.getProviderId()
                    , billPayTicketTrain.getTicketId()
                    , billPayTicketTrain.getAmount()
                    , 1, billPayTicketTrain.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_bill_pay_ticket_train > 0)) {
            incPoint(momoMsg.cmdPhone, billPayTicketTrain.getAmount(), divider_bill_pay_ticket_train, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, billPayTicketTrain.getAmount(), -1);
        message.reply(replyObj);

        log.writeLog();

    }

    //thanh toan tien bao hiem
    public void doBillPayInsurance(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.BillPayInsurance billPayInsurance;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            billPayInsurance = SoapProto.BillPayInsurance.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            billPayInsurance = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , billPayInsurance.getPin()
                    , billPayInsurance.getProviderId()
                    , billPayInsurance.getInsuranceAcc()
                    , billPayInsurance.getAmount()
                    , 1, billPayInsurance.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_bill_pay_insurance > 0)) {
            incPoint(momoMsg.cmdPhone, billPayInsurance.getAmount(), divider_bill_pay_insurance, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , billPayInsurance.getAmount()
                , -1);
        message.reply(replyObj);

        log.writeLog();
    }

    //thanh toan cac loai hoa don khac
    public void doBillPayOther(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.BillPayOther billPayOther;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            billPayOther = SoapProto.BillPayOther.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            billPayOther = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , billPayOther.getPin()
                    , billPayOther.getProviderId()
                    , billPayOther.getBillerId()
                    , billPayOther.getAmount()
                    , 1, billPayOther.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }

        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_bill_pay_other > 0)) {
            incPoint(momoMsg.cmdPhone, billPayOther.getAmount(), divider_bill_pay_other, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , billPayOther.getAmount()
                , -1);
        message.reply(replyObj);

        log.writeLog();

    }
/*
    DEPOSIT_CASH_OTHER  = 5037; // NAP TIEN KHAC
    BUY_MOBILITY_CARD   = 5038; // MUA THE CAO
    BUY_GAME_CARD       = 5039; // MUA THE GAME
    BUY_OTHER           = 5040; // MUA KHAC*/

    public void doDepositCashOther(Message message) {
        MomoMessage momoMsg = MomoMessage.fromBuffer((Buffer) message.body());

        SoapProto.DepositCashOther depositCashOther;

        int result = MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE; // default, success
        long tranId = 0;

        try {
            depositCashOther = SoapProto.DepositCashOther.parseFrom(momoMsg.cmdBody);
        } catch (InvalidProtocolBufferException e) {
            logger.error(momoMsg + "InvalidProtocolBufferException", e);
            depositCashOther = null;
            result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + momoMsg.cmdPhone);

        if (result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) {

            StandardMSResponse response = mSoapProcessor.billpay(vertx, "0" + momoMsg.cmdPhone
                    , depositCashOther.getPin()
                    , depositCashOther.getProviderId()
                    , depositCashOther.getCustomerAcc()
                    , depositCashOther.getAmount()
                    , 1, depositCashOther.getChannel(), null, log);

            if (response == null) {
                result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
            } else {
                result = response.getResultCode();
                tranId = DataUtil.stringToUNumber(response.getTransID());
            }
        }

        if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_deposit_cash_other > 0)) {
            incPoint(momoMsg.cmdPhone, depositCashOther.getAmount(), divider_deposit_cash_other, log);
        }

        JsonObject replyObj = Misc.getJsonObjRpl(result
                , tranId
                , depositCashOther.getAmount()
                , -1);
        message.reply(replyObj);

        log.writeLog();
    }


    public void doBuyMobilityCard(Message message) {
        //todo will implement later
    }

    public void doBuyGameCard(Message message) {
        //todo will implement later
    }

    public void doBuyOther(Message message) {
        //todo will implement later
    }

    //app sms.end

    //priv

    private void excuteTransfer(final Message message
            , final String ownerPin
            , final String ownerNumber
            , final String toNumber
            , final long amount
            , final String channel
            , final List<SoapProto.keyValuePair> kvps
    ) {

        HashMap<String, String> hm = Misc.getHashMapInSoap(kvps);
        long time = hm.containsKey("time") ? DataUtil.stringToUNumber(hm.get("time")) : System.currentTimeMillis();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(ownerNumber);
        log.add("func", "excuteTransfer");
        log.setTime(time);

        Misc.getCapsetAndM2mType(vertx, ownerNumber, toNumber, amount, log, new Handler<Misc.CapsetAndM2MType>() {
            @Override
            public void handle(Misc.CapsetAndM2MType capsetAndM2MType) {

                if (capsetAndM2MType.Capset == false) {
                    JsonObject replyObj = Misc.getJsonObjRpl(SoapError.WALLET_CAP_EXCEEDED, 0, amount, -1);
                    message.reply(replyObj);
                    log.writeLog();
                    return;
                }

                int hasSms = 0; // default khong SMS quy dinh co gui sms hay khong
                String m2mType = capsetAndM2MType.M2mTranferType; // quy dinh loai giao dich gi

                //neu giao dich la
                if (!m2mType.equalsIgnoreCase("u2u")) {
                    hasSms = 1; //co sms
                }

                long tranId = 0;
                StandardMSResponse response = mSoapProcessor.transfer(vertx, ownerNumber
                        , ownerPin
                        , toNumber
                        , amount
                        , ""
                        , channel
                        , m2mType
                        , hasSms
                        , kvps
                        , log);


                int result;
                if (response == null) {
                    result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
                } else {
                    log.add("result", response.getResultCode());
                    log.add("status", response.getDescription());
                    result = response.getResultCode();
                    tranId = (response.getTransID() == null ? -1 : DataUtil.stringToUNumber(response.getTransID()));
                }

                if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_m2m > 0)) {
                    incPoint(DataUtil.strToInt(ownerNumber), amount, divider_m2m, log);
                }

                JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, amount, -1);
                message.reply(replyObj);
                log.writeLog();

            }
        });
    }

    private void excuteMerchantTransfer(final Message message
            , final String ownerPin
            , final String ownerNumber
            , final String toNumber
            , final long amount
            , final String channel
            , final List<SoapProto.keyValuePair> kvps
    ) {

        HashMap<String, String> hm = Misc.getHashMapInSoap(kvps);
        long time = hm.containsKey("time") ? DataUtil.stringToUNumber(hm.get("time")) : System.currentTimeMillis();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(ownerNumber);
        log.add("func", "excuteTransfer");
        log.setTime(time);

        Misc.getCapsetAndM2mType(vertx, ownerNumber, toNumber, amount, log, new Handler<Misc.CapsetAndM2MType>() {
            @Override
            public void handle(Misc.CapsetAndM2MType capsetAndM2MType) {

                if (capsetAndM2MType.Capset == false) {
                    JsonObject replyObj = Misc.getJsonObjRpl(SoapError.WALLET_CAP_EXCEEDED, 0, amount, -1);
                    message.reply(replyObj);
                    log.writeLog();
                    return;
                }

                int hasSms = 0; // default khong SMS quy dinh co gui sms hay khong
                String m2mType = capsetAndM2MType.M2mTranferType; // quy dinh loai giao dich gi

                //neu giao dich la
                if (!m2mType.equalsIgnoreCase("u2u")) {
                    hasSms = 1; //co sms
                }

                long tranId = 0;
                StandardMSResponse response = mSoapProcessor.transferMerchant(vertx, ownerNumber
                        , ownerPin
                        , toNumber
                        , amount
                        , ""
                        , channel
                        , m2mType
                        , hasSms
                        , kvps
                        , log);


                int result;
                if (response == null) {
                    result = MomoProto.TranHisV1.ResultCode.SYSTEM_ERROR_VALUE;
                } else {
                    log.add("result", response.getResultCode());
                    log.add("status", response.getDescription());
                    result = response.getResultCode();
                    tranId = (response.getTransID() == null ? -1 : DataUtil.stringToUNumber(response.getTransID()));
                }

                if ((result == MomoProto.TranHisV1.ResultCode.ALL_OK_VALUE) && (divider_m2m > 0)) {
                    incPoint(DataUtil.strToInt(ownerNumber), amount, divider_m2m, log);
                }

                JsonObject replyObj = Misc.getJsonObjRpl(result, tranId, amount, -1);
                message.reply(replyObj);
                log.writeLog();

            }
        });
    }

    public static class ObjCoreStatus {


        private static String Reged = "Reged";
        private static String Actived = "Actived";
        private static String Susppended = "Suppended";
        private static String Frozen = "Frozen";
        private static String Stopped = "Stopped";
        //Registered=1, Active=2, Stopped=128, Suspended=8, Frozen=32.
        public boolean isReged = true;
        public boolean isActivated = true;
        public boolean isStopped = false;
        public boolean isSuspended = false;
        public boolean isFrozen = false;
        private int registerMask = 1;
        ;
        private int activeMask = 2;
        private int suspendedMask = 8;
        private int frozenMask = 32;
        private int stoppedMask = 128;

        public ObjCoreStatus() {
        }

        public ObjCoreStatus(int status, Common.BuildLog log) {
            isReged = ((status & registerMask) == registerMask);
            isActivated = (status & activeMask) == activeMask;
            isSuspended = (status & suspendedMask) == suspendedMask;
            isFrozen = (status & frozenMask) == frozenMask;
            isStopped = (status & stoppedMask) == stoppedMask;

            log.add("isReged", isReged);
            log.add("isActivated", isActivated);
            log.add("isSuspended", isSuspended);
            log.add("isFrozen", isFrozen);
            log.add("isStopped", isStopped);
        }

        public ObjCoreStatus(JsonObject jo) {
            isReged = jo.getBoolean(Reged, false);
            isActivated = jo.getBoolean(Actived, false);
            isSuspended = jo.getBoolean(Susppended, false);
            isFrozen = jo.getBoolean(Frozen, false);
            isStopped = jo.getBoolean(Stopped, false);
        }

        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.putBoolean(Reged, isReged);
            jo.putBoolean(Actived, isActivated);
            jo.putBoolean(Susppended, isSuspended);
            jo.putBoolean(Frozen, isFrozen);
            jo.putBoolean(Stopped, isStopped);
            return jo;
        }
    }

}