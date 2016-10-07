package com.mservice.momo.vertx;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.cmd.handlers.*;
import com.mservice.momo.data.*;
import com.mservice.momo.data.web.ArticleDb;
import com.mservice.momo.data.web.BankAccountDb;
import com.mservice.momo.data.web.KeyValueDb;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.*;
import com.mservice.momo.vertx.processor.transferbranch.PayAirLineTicket;
import com.mservice.momo.vertx.processor.transferbranch.PayOneBill;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ntunam on 3/13/14.
 */
public class CommandVerticle extends Verticle {

//    public static final String ADDRESS = "momo.CommandVerticle";

    public static int MAX_LOGIN_ATTEMPT;
    //Banknet constants
    public static String MOMO_PHONE;
    private static boolean MODULE_ACTIVE_CONFIG = false;
    private double BANK_NET_DINAMIC_FEE;
    private int BANK_NET_STATIC_FEE;

    private Logger logger;

    private Handler<Message<Buffer>> registeredEventBusHandler;

    private Map<Integer, SockData> sockDatas;
    private Map<Integer, MomoProto.TranHisV1> tranMap;


    public void loadConfig(JsonObject config) {
        TransDb mTranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());
        PhonesDb mPhonesDb = new PhonesDb(vertx.eventBus(), container.logger());
        JsonObject bankNetConfig = config.getObject("bank_net");


        String PHONES_FOR_TEST = config.getString("phones_test_list","");
        boolean SEND_SMS = config.getBoolean("send_sms",true);
        String BANK_NET_BANK_CODE_BANK_NAME_SUPPORT = bankNetConfig.getString("bank_support");

        //setting for feeDB
        FeeCollection.setFeeDb(new FeeDb(vertx,logger));
        FeeCollection.getInstance().initData();

//        sockDatas = vertx.sharedData().getMap("CommandVerticle.sockDatas");
        sockDatas = new HashMap<>();
        tranMap = new HashMap<>();
    }

    @Override
    public void start() {
        logger = container.logger();

        JsonObject globalConfig = container.config();
        JsonObject config = globalConfig.getObject("commandVerticle", new JsonObject());
        JsonObject serverConfig = globalConfig.getObject("server", new JsonObject());
        JsonObject banknetConfig = globalConfig.getObject("bank_net", new JsonObject());

        loadConfig(globalConfig);

        MAX_LOGIN_ATTEMPT = serverConfig.getInteger("login_max_try", 3);
        MOMO_PHONE = serverConfig.getString("momo_phone", "0");

//        BANK_NET_DINAMIC_FEE = banknetConfig.getNumber("fee.dynamic").doubleValue();
//        BANK_NET_STATIC_FEE = banknetConfig.getInteger("fee.static");

        MODULE_ACTIVE_CONFIG = config.getBoolean("active", MODULE_ACTIVE_CONFIG);

        if (!MODULE_ACTIVE_CONFIG) {
            logger.info("CommandVerticle is INACTIVATED!.");
            return;
        }

        MainDb mainDb = new MainDb();
        mainDb.mPhonesDb = new PhonesDb(vertx.eventBus(), container.logger());
        mainDb.mImeisDb = new ImeisDb(vertx.eventBus());
        mainDb.mBillsDb = new BillsDb(vertx.eventBus());
        mainDb.mAccessHistoryDb = new AccessHistoryDb(vertx.eventBus());
        mainDb.mDeviceInfoDb = new DeviceInfoDb(vertx.eventBus());
        mainDb.mAgentDb = new AgentsDb(vertx.eventBus(),logger);
        mainDb.transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), container.logger(), container.config());

        mainDb.feeDb = new FeeDb(vertx, container.logger());

        mainDb.articleDb = new ArticleDb(vertx.eventBus());
        mainDb.bankAccountDb = new BankAccountDb(vertx.eventBus());
        mainDb.keyValueDb = new KeyValueDb(vertx.eventBus());

        ConnectProcess connectProcess = new ConnectProcess(vertx, container.logger(),globalConfig);
        InfoProcess infoFactory = new InfoProcess(vertx,logger,globalConfig);
        PayOneBill payBillFactory = new PayOneBill(vertx,logger,globalConfig);
        TransProcess transFactory = new TransProcess(vertx,logger,globalConfig);
        BanknetProcess banknetFactory = new BanknetProcess(vertx,logger, globalConfig);
        PayAirLineTicket airLineTicketFactory = new PayAirLineTicket(vertx, container.logger(),globalConfig);

        //Building Transfer common.
        TransferCommon transferCommon = new TransferCommon(vertx,logger,globalConfig);

        final OtpHandler otpHandler = new OtpHandler(mainDb, vertx, container, MAX_LOGIN_ATTEMPT, config.getObject("otpHandler", new JsonObject()));
        final AgentHandler agentHandler = new AgentHandler(mainDb, vertx, container, connectProcess, infoFactory, config.getObject("registerHandler", new JsonObject()));
        final ArticleHandler articleHandler = new ArticleHandler(mainDb, vertx, container, config.getObject("articleHandler", new JsonObject()));
        final BankAccountHandler bankAccountHandler = new BankAccountHandler(mainDb, vertx, container, transFactory, config.getObject("bankAccountHandler", globalConfig));
        final TransactionHandler transactionHandler = new TransactionHandler(mainDb, vertx, container, config.getObject("transactionHandler", new JsonObject()));
        final TopupHandler topupHandler = new TopupHandler(mainDb, vertx, container, transFactory, config.getObject("topupHandler", new JsonObject()));
        final TransferHandler transferHandler = new TransferHandler(mainDb, vertx, container, globalConfig, config.getObject("transferHandler", new JsonObject()), transFactory);
        final BillHandler billHandler = new BillHandler(mainDb, vertx, container, config.getObject("billHandler", new JsonObject()), infoFactory, airLineTicketFactory, payBillFactory);
        final BanknetHandler banknetHandler = new BanknetHandler(mainDb, vertx, container, BANK_NET_DINAMIC_FEE, BANK_NET_STATIC_FEE, MOMO_PHONE, banknetFactory, globalConfig, infoFactory);
        final StoresHandler storesHandler = new StoresHandler(mainDb, vertx, container, config.getObject("storesHandler", new JsonObject()));
        final KeyValueDataHandler keyValueDataHandler = new KeyValueDataHandler(mainDb, vertx, container, config.getObject("keyValueDataHandler", new JsonObject()));
        final Mua123Handler mua123Handler = new Mua123Handler(mainDb, vertx, container, config.getObject("mua123Handler", new JsonObject()));

        final MakeTranHandler makeTranHandler = new MakeTranHandler(mainDb, vertx, container, globalConfig);
//        mainDb.bankAccountDb.getUserBankAccounts(1699204000, new Handler<ArrayList<CmdModels.BankAccount>>() {
//            @Override
//            public void handle(ArrayList<CmdModels.BankAccount> event) {
//                for (CmdModels.BankAccount a: event) {
//                    System.out.println(a);
//                }
//            }
//        });

        registeredEventBusHandler = new Handler<Message<Buffer>>() {

            @Override
            public void handle(Message<Buffer> message) {
                try {
                    MomoCommand cmd = MomoCommand.fromBuffer(message.body());
                    CommandContext context = new CommandContext(container, message, cmd, sockDatas, tranMap);
                    switch (cmd.getCommandType()) {
                        case SEND_OTP:
                            otpHandler.sendOtp(context);
                            break;
                        case VERIFY_OTP:
                            otpHandler.verifyOtp(context);
                            break;
                        case REGISTER:
                            agentHandler.register(context);
                            break;
                        case GET_AGENT_INFO:
                            agentHandler.getAgentInfo(context);
                            break;
                        case IS_PIN_CORRECT:
                            agentHandler.isPinCorrect(context);
                            break;
                        case CHANGE_PIN:
                            agentHandler.changePin(context);
                            break;
                        case MODIFY_ARTICLE:
                            articleHandler.modifyArticle(context);
                            break;
                        case GET_ARTICLE_PAGE:
                            articleHandler.getArticlePage(context);
                            break;
                        case MODIFY_BANK_ACCOUNT:
                            bankAccountHandler.modifyBankAccount(context);
                            break;
                        case GET_AGENT_BANK_ACCOUNTS:
                            bankAccountHandler.getAgentBankAccounts(context);
                            break;
                        case GET_TRANSACTION:
                            transactionHandler.getTransaction(context);
                            break;
                        case GET_TRANSACTION_DETAIL:
                            transactionHandler.getTransactionDetail(context);
                            break;
                        case TOPUP:
                            topupHandler.topup(context);
                            break;
                        case TOPUP_GAME:
                            topupHandler.topupGame(context);
                            break;
                        case TRANSFER_M2M:
                            transferHandler.m2m(context);
                            break;
                        case TRANSFER_M2C:
                            transferHandler.m2c(context);
                            break;
                        case GET_BILL_INFO:
                            billHandler.getBillInfo(context);
                            break;
                        case PAY_BILL:
                            billHandler.payBill(context);
                            break;
                        case BANK_IN:
                            bankAccountHandler.bankIn(context);
                            break;
                        case BANK_OUT:
                            bankAccountHandler.bankOut(context);
                            break;
                        case BANK_NET_TO_MOMO:
                            banknetHandler.banknetToMomo(context);
                            break;
                        case VERIFY_BANKNET_OTP:
                            banknetHandler.verifyBankNetOtp(context);
                            break;
                        case GET_STORE_AROUND:
                            storesHandler.getStoreAround(context);
                            break;
                        case KEY_VALUE_DATA:
                            keyValueDataHandler.getOrPut(context);
                            break;
                        case GET_STORE_BY_CODE:
                            storesHandler.getStoreByCode(context);
                            break;
                        case PAY_123MUA_ORDER:
                            mua123Handler.payOrder(context);
                            break;
                        case UPDATE_AGENT_INFO:
                            agentHandler.updateAgentInfo(context);
                            break;
                        case DEPOSIT_WITHDRAW_AT_PLACE:
                            transferHandler.depositWithdrawAtPlace(context);
                            break;
                        case GET_AVATAR_UPLOAD_TOKEN:
                            agentHandler.getAvatarUploadToken(context);
                            break;
                        case SAVE_BILL:
                            billHandler.saveBill(context);
                            break;
                        case GET_SAVED_BILL:
                            billHandler.getSavedBill(context);
                            break;
                        case REMOVE_SAVED_BILL:
                            billHandler.removeSavedBill(context);
                            break;
                        case GET_TRANSACTION_FEE:
                            transactionHandler.getTransactionFee(context);
                            break;
                        case PAY_AIRLINE_TICKET:
                            billHandler.payAirlineTicket(context);
                            break;
                        case SEND_SMS:
                            otpHandler.sendSms(context);
                            break;
                        case WITHDRAW_BY_AGENT:
                            transferHandler.withdrawByAgent(context);
                            break;
                        case BANK_OUT_MANUAL:
                            bankAccountHandler.bankOutManual(context);
                            break;
                        case COUNT_AGENT_TRAN:
                            agentHandler.countAgentTran(context);
                            break;
                        case IS_MOMOER:
                            agentHandler.isMomoer(context);
                            break;
                        case MAKE_TRAN:
                            makeTranHandler.makeTran(context);
                            break;
                        case COMPLETE_TRAN:
                            makeTranHandler.completeTran(context);
                            break;
                        case DO_TRAN:
                            makeTranHandler.doTran(context);
                            break;
                        case GET_SERVICE_LAYOUT:
                            billHandler.getServiceLayout(context);
                            break;
                        case GET_SERVICE:
                            billHandler.getService(context);
                            break;

                        default:
                            logger.warn(message.address() + " request an unsupported MomoCommand: " + cmd.getCommandType());
                    }
                } catch (InvalidProtocolBufferException e) {
                    logger.warn("Receive a broken MomoCommand packet from " + message.address());
                }
            }
        };


        vertx.eventBus().registerLocalHandler(AppConstant.CommandVerticle_ADDRESS, registeredEventBusHandler);
    }

    @Override
    public void stop() {
//        vertx.eventBus().unregisterHandler(ADDRESS, registeredEventBusHandler);
    }

}
