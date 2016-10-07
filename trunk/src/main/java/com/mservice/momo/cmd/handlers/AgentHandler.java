package com.mservice.momo.cmd.handlers;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.avatar.UserResourceVerticle;
import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle;
import com.mservice.momo.msg.*;
import com.mservice.momo.util.*;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.ConnectProcess;
import com.mservice.momo.vertx.processor.InfoProcess;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 3/18/14.
 */
public class AgentHandler extends CommandHandler {
    private final String arrGroup;
    private final String arrCapset;
    private final String upperLimit;

    private ConnectProcess connectProcess;
    private InfoProcess infoFactory;
    private PhonesDb phonesDb;
    private TransDb tranDb;

    private Vertx vertx;
    private Logger logger;

    private AgentsDb agentDb;

    public AgentHandler(MainDb mainDb, Vertx vertx, Container container, ConnectProcess connectProcess, InfoProcess infoFactory, JsonObject config) {
        super(mainDb, vertx, container, config);
        arrGroup = config.getString("arrGroup", "");
        arrCapset = config.getString("arrCapset", "");
        upperLimit = config.getString("upperLimit", "5000000");
        this.connectProcess = connectProcess;
        this.infoFactory = infoFactory;
        this.phonesDb = new PhonesDb(vertx.eventBus(), container.logger());
        tranDb = DBFactory.createTranDb(vertx, vertx.eventBus(), container.logger(), container.config());
        this.vertx = vertx;
        this.logger = container.logger();
        this.agentDb = new AgentsDb(vertx.eventBus(), container.logger());
        connectProcess = new ConnectProcess(vertx, logger, config);
    }

    private void isRegisted(int number, final Handler<Boolean> callback) {
        MomoMessage momoMessage = new MomoMessage(SoapProto.MsgType.CHECK_USER_STATUS_VALUE, 0, number, "".getBytes());
        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, momoMessage.toBuffer(), new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> response) {
                MomoMessage momo = MomoMessage.fromBuffer(response.body());
                try {
                    MomoProto.RegStatus status = MomoProto.RegStatus.parseFrom(momo.cmdBody);

                    callback.handle(status.getIsReged());
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                    callback.handle(false);
                }
            }
        });
    }


    public void register(final CommandContext context) {
        final CmdModels.Register cmd = (CmdModels.Register) context.getCommand().getBody();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber())) {
            context.replyError(-1, "Phone number is not valid.");
            return;
        }
        if (!ValidationUtil.isValidPin(cmd.getPin())) {
            context.replyError(-1, "Pin is not valid.");
            return;
        }

        isRegisted(cmd.getPhoneNumber(), new Handler<Boolean>() {
            @Override
            public void handle(Boolean registered) {
                if (registered) {
                    CmdModels.RegisterReply replyBody;
                    MomoCommand replyCommand;

                    replyBody = CmdModels.RegisterReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.RegisterReply.ResultCode.NUMBER_EXISTED)
                            .build();
                    replyCommand = new MomoCommand(CmdModels.CommandType.REGISTER_REPLY, replyBody);
                    context.reply(replyCommand);
                    return;
                }

                //doing business
                Buffer signUpBuf = MomoMessage.buildBuffer(
                        SoapProto.MsgType.REGISTER_VALUE,
                        0,
                        cmd.getPhoneNumber(),
                        SoapProto.Register.newBuilder()
                                .setName(cmd.getName())
                                .setIdCard(cmd.getCardId())
                                .setChannel(Const.CHANNEL_WEB)
                                .setPin(cmd.getPin())
                                .setEmail(cmd.getEmail())
                                .setArrGroup(arrGroup)
                                .setArrCapset(arrCapset)
                                .setUpperLimit(upperLimit)
                                .build()
                                .toByteArray()
                );

                vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, signUpBuf, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> result) {
                        CmdModels.RegisterReply.ResultCode resultCode;
                        CmdModels.RegisterReply replyBody;
                        MomoCommand replyCommand;

                        int rcode = result.body().getInteger("result", -1);
                        boolean isAddNew = result.body().getBoolean("isaddnew", false);

                        switch (rcode) {
                            case MomoProto.SystemError.ALL_OK_VALUE:

                                JsonObject jo = new JsonObject();
                                jo.putString(colName.PhoneDBCols.NAME, cmd.getName());
                                jo.putString(colName.PhoneDBCols.CARD_ID, cmd.getCardId());
//                                jo.putString(colName.PhoneDBCols.DATE_OF_BIRTH,dob);
//                                jo.putString(colName.PhoneDBCols.ADDRESS,cmd.get);
                                jo.putString(colName.PhoneDBCols.PIN, DataUtil.encode(cmd.getPin()));
                                jo.putNumber(colName.PhoneDBCols.NUMBER, cmd.getPhoneNumber());
                                jo.putBoolean(colName.PhoneDBCols.IS_REGED, true);  //da dang ky
                                jo.putBoolean(colName.PhoneDBCols.IS_NAMED, false); //da set group noname
                                jo.putBoolean(colName.PhoneDBCols.DELETED, false);  //dang hoat dong
                                jo.putBoolean(colName.PhoneDBCols.IS_SETUP, true);  //da setUp

                                phonesDb.updatePartialNoReturnObj(cmd.getPhoneNumber(), jo, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {
                                    }
                                });

                                /*phonesDb.updatePartial(cmd.getPhoneNumber(),jo,new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(PhonesDb.Obj obj) {
                                    }
                                });
*/

                                logger.info(cmd.getPhoneNumber() + " registered from web application.");
                                StatisticUtils.fireRegister(vertx.eventBus(), cmd.getPhoneNumber(), StatisticModels.Action.Channel.WEB);
                                resultCode = CmdModels.RegisterReply.ResultCode.SUCCESS;
                                //todo we should do m2n here
                                if (isAddNew) {
                                    MomoMessage msg = new MomoMessage(0, System.currentTimeMillis(), cmd.getPhoneNumber(), null);
                                    Common.BuildLog log = new Common.BuildLog(logger, cmd.getPhoneNumber());
                                    log.add("begin M2N", "OK");
                                    try {
                                        connectProcess.processM2N(msg, null, log);
                                    } catch (Exception e) {
                                        logger.error("connectProcess.processM2N", e);
                                    }
                                    log.add("end M2N", "OK");
                                    log.writeLog();
                                }
                                break;
                            case MomoProto.SystemError.NUMBER_EXISTED_VALUE:
                                resultCode = CmdModels.RegisterReply.ResultCode.NUMBER_EXISTED;
                                break;
                            default:
                                replyCommand = new MomoCommand(CmdModels.CommandType.ERROR
                                        , CmdModels.Error.newBuilder()
                                        .setCode(rcode)
                                        .build());
                                context.reply(replyCommand);
                                return;
                        }

                        replyBody = CmdModels.RegisterReply.newBuilder()
                                .setPhoneNumber(cmd.getPhoneNumber())
                                .setResult(resultCode)
                                .build();
                        replyCommand = new MomoCommand(CmdModels.CommandType.REGISTER_REPLY, replyBody);
                        context.reply(replyCommand);
                    }
                });

            }
        });

//        mainDb.mPhonesDb.getPhoneObjInfo(cmd.getPhoneNumber(),new Handler<PhonesDb.Obj>() {
//            @Override
//            public void handle(PhonesDb.Obj phoneObj) {
//                if (phoneObj != null
//                        && ("".equals(phoneObj.name)
//                        && "".equals(phoneObj.bank_account)
//                        && "".equals(phoneObj.cardId)
//                        && "".equals(phoneObj.email) && "".equals(phoneObj.address))) {
//
//                    //doing business
//                    Buffer signUpBuf = MomoMessage.buildBuffer(
//                            SoapProto.MsgType.REGISTER_VALUE,
//                            0,
//                            cmd.getPhoneNumber(),
//                            SoapProto.Register.newBuilder()
//                                    .setName(cmd.getName())
//                                    .setIdCard(cmd.getCardId())
//                                    .setChannel(Const.CHANNEL_WEB)
//                                    .setPin(cmd.getPin())
//                                    .setEmail(cmd.getEmail())
//                                    .setArrGroup(arrGroup)
//                                    .setArrCapset(arrCapset)
//                                    .setUpperLimit(upperLimit)
//                                    .build()
//                                    .toByteArray()
//                    );
//
//                    vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, signUpBuf, new Handler<CoreMessage<Integer>>() {
//                        @Override
//                        public void handle(CoreMessage<Integer> result) {
//                            CmdModels.RegisterReply.ResultCode resultCode;
//                            CmdModels.RegisterReply replyBody;
//                            MomoCommand replyCommand;
//                            switch (result.body()) {
//                                case MomoProto.SystemError.ALL_OK_VALUE:
//                                    logger.info(cmd.getPhoneNumber() + " registered from web application.");
//                                    StatisticUtils.fireRegister(vertx.eventBus(), cmd.getPhoneNumber(), StatisticModels.Action.Channel.WEB);
//                                    resultCode = CmdModels.RegisterReply.ResultCode.SUCCESS;
//                                    break;
//                                case MomoProto.SystemError.NUMBER_EXISTED_VALUE:
//                                    resultCode = CmdModels.RegisterReply.ResultCode.NUMBER_EXISTED;
//                                    break;
//                                default:
//                                    replyCommand = new MomoCommand(CmdModels.CommandType.ERROR, CmdModels.Error.newBuilder().setCode(result.body()).build());
//                                    context.reply(replyCommand);
//                                    return;
//                            }
//
//                            replyBody = CmdModels.RegisterReply.newBuilder()
//                                    .setPhoneNumber(cmd.getPhoneNumber())
//                                    .setResult(resultCode)
//                                    .build();
//                            replyCommand = new MomoCommand(CmdModels.CommandType.REGISTER_REPLY, replyBody);
//                            context.reply(replyCommand);
//                        }
//                    });
//                } else { // is already existed.
//
//                    CmdModels.RegisterReply replyBody;
//                    MomoCommand replyCommand;
//
//                    replyBody = CmdModels.RegisterReply.newBuilder()
//                            .setPhoneNumber(cmd.getPhoneNumber())
//                            .setResult(CmdModels.RegisterReply.ResultCode.NUMBER_EXISTED)
//                            .build();
//                    replyCommand = new MomoCommand(CmdModels.CommandType.REGISTER_REPLY, replyBody);
//                    context.reply(replyCommand);
//                }
//            }
//        });
    }

/*    public void register(final CommandContext context) {
        final CmdModels.Register cmd = (CmdModels.Register) context.getCommand().getBody();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber())) {
            context.replyError(-1, "Phone number is not valid.");
            return;
        }
        if (!ValidationUtil.isValidPin(cmd.getPin())) {
            context.replyError(-1, "Pin is not valid.");
            return;
        }

        //doing business
        Buffer signUpBuf = MomoMessage.buildBuffer(
                SoapProto.MsgType.REGISTER_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.Register.newBuilder()
                        .setName(cmd.getName())
                        .setIdCard(cmd.getCardId())
                        .setPin(cmd.getPin())
                        .setEmail(cmd.getEmail())
                        .setArrGroup(arrGroup)
                        .setArrCapset(arrCapset)
                        .setUpperLimit(upperLimit)
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(SoapVerticle.ADDRESS, signUpBuf, new Handler<CoreMessage<Integer>>() {
            @Override
            public void handle(CoreMessage<Integer> result) {
                CmdModels.RegisterReply.ResultCode resultCode;
                CmdModels.RegisterReply replyBody;
                MomoCommand replyCommand;
                switch (result.body()) {
                    case MomoProto.RegisterReply.ResultCode.ALL_OK_VALUE:
                        mainDb.mPhonesDb.syncWithCoreRegister(cmd.getPhoneNumber(),
                                cmd.getEmail(),
                                cmd.getQuestion(),
                                cmd.getAnswer(),
                                new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(PhonesDb.Obj obj) {
                                        if (obj == null) {
                                            logger.error("Register synchronizing failed! : phoneNumber " + cmd.getPhoneNumber());
                                        }
                                    }
                                });
                        resultCode = CmdModels.RegisterReply.ResultCode.SUCCESS;
                        break;
                    case MomoProto.RegisterReply.ResultCode.NUMBER_EXISTED_VALUE:
                        resultCode = CmdModels.RegisterReply.ResultCode.NUMBER_EXISTED;
                        break;
                    default:
                        replyCommand = new MomoCommand(CmdModels.CommandType.ERROR, CmdModels.Error.newBuilder().setCode(result.body()).build());
                        context.reply(replyCommand);
                        return;
                }

                replyBody = CmdModels.RegisterReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(resultCode)
                        .build();
                replyCommand = new MomoCommand(CmdModels.CommandType.REGISTER_REPLY, replyBody);
                context.reply(replyCommand);
            }
        });
    }*/

    public void getAgentInfo(final CommandContext context) {
        final CmdModels.GetAgentInfo cmd = (CmdModels.GetAgentInfo) context.getCommand().getBody();

        //validation
        if (!PhoneNumberUtil.isValidPhoneNumber(cmd.getPhoneNumber())) {
            context.replyError(-1, "Phone number is not valid.");
            return;
        }

        JsonObject jo = new JsonObject();
        jo.putNumber(UMarketOracleVerticle.fieldNames.TYPE, UMarketOracleVerticle.GET_BALANCE);
        jo.putNumber(UMarketOracleVerticle.fieldNames.NUMBER, cmd.getPhoneNumber());

        vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, jo, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonRpl) {
                final int error = jsonRpl.body().getInteger(StringConstUtil.ERROR, 0);
                if (error != 0) {
                    logger.info("EXCEPTION BALANCE");
                    return;
                }
                final long balance = jsonRpl.body().getLong(UMarketOracleVerticle.fieldNames.BALANCE, 0);

                agentDb.getOneAgent("0" + cmd.getPhoneNumber(), "getAgentInfo", new Handler<AgentsDb.StoreInfo>() {
                    @Override
                    public void handle(final AgentsDb.StoreInfo storeInfo) {
                        mainDb.mPhonesDb.getPhoneObjInfoLocal(cmd.getPhoneNumber(), new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj phoneObj) {

                                PhonesDb.Obj coreResult = phoneObj;

                                CmdModels.GetAgentInfoReply replyBody;
                                boolean isAgent = storeInfo != null ? true : false;
                                if (coreResult != null) {
                                    if (phoneObj.isAgent) {
                                        replyBody = CmdModels.GetAgentInfoReply.newBuilder()
                                                .setPhoneNumber(cmd.getPhoneNumber())
                                                .setName(phoneObj.name)
                                                .setEmail(phoneObj.email)
                                                .setCardId(phoneObj.cardId)
                                                .setQuestion(isAgent + "")
                                                .setAnswer(phoneObj.answer)
                                                .setBankAccount(phoneObj.bank_account)
                                                .setBankCode(phoneObj.bank_code)
                                                .setBankName(phoneObj.bank_name)
                                                .setMomo(balance)
                                                .setPoint(phoneObj.mpoint)
                                                .setIsNamedAccount(phoneObj.isNamed)
                                                .setMload(phoneObj.mload)
                                                .setBirthday(phoneObj.dateOfBirth)
                                                .setIsActive(phoneObj.isActived)
                                                .setAddress(phoneObj.address)
                                                .setCreatedDate(phoneObj.createdDate)
                                                .build();
                                    } else {
                                        replyBody = CmdModels.GetAgentInfoReply.newBuilder()
                                                .setPhoneNumber(cmd.getPhoneNumber())
                                                .setName(phoneObj.name)
                                                .setEmail(phoneObj.email)
                                                .setCardId(phoneObj.cardId)
                                                .setQuestion(isAgent + "")
                                                .setAnswer(phoneObj.answer)
                                                .setBankAccount(phoneObj.bank_account)
                                                .setBankCode(phoneObj.bank_code)
                                                .setBankName(phoneObj.bank_name)
                                                .setMomo(balance)
                                                .setPoint(phoneObj.mpoint)
                                                .setIsNamedAccount(phoneObj.isNamed)
                                                .setMload(phoneObj.mload)
                                                .setBirthday(phoneObj.dateOfBirth)
                                                .setIsActive(phoneObj.isActived)
                                                .setAddress(phoneObj.address)
                                                .setCreatedDate(phoneObj.createdDate)
                                                .build();
                                    }
                                } else
                                    replyBody = CmdModels.GetAgentInfoReply.newBuilder()
                                            .setPhoneNumber(cmd.getPhoneNumber())
                                            .build();
                                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_AGENT_INFO_REPLY, replyBody);
                                context.reply(replyCommand);
                            }
                        });
                    }
                });
            }
        });

/*        mainDb.mPhonesDb.forceSyncWithCore(cmd.getPhoneNumber(),new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj phoneObj) {
                if (phoneObj != null) {
                    CmdModels.GetAgentInfoReply replyBody = CmdModels.GetAgentInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setName(phoneObj.name)
                            .setEmail(phoneObj.email)
                            .setCardId(phoneObj.cardId)
                            .setQuestion(phoneObj.question)
                            .setAnswer(phoneObj.answer)
                            .setBankAccount(phoneObj.bank_account)
                            .setBankCode(phoneObj.bank_code)
                            .setBankName(phoneObj.bank_name)
                            .setMomo(phoneObj.momo)
                            .setPoint(phoneObj.mpoint)
                            .setIsNamedAccount(phoneObj.isNamed)
                            .setMload(phoneObj.mload)
                            .setBirthday(phoneObj.dateOfBirth)
                            .setIsActive(phoneObj.isActived)
                            .setAddress(phoneObj.address)
                            .setCreatedDate(phoneObj.createdDate)
                            .build();
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_AGENT_INFO_REPLY, replyBody);
                    context.reply(replyCommand);

                } else {
                    CmdModels.Error replyBody = CmdModels.Error.newBuilder()
                            .setDescription("Can't get agent info.")
                            .build();
                    logger.info("SYNC WITH CORE AFTER DOING TRANSACTION  FAILED");
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_AGENT_INFO_REPLY, replyBody);
                    context.reply(replyCommand);
                }
            }
        });*/
    }

    public void isPinCorrect(final CommandContext context) {

        final CmdModels.IsPinCorrect cmd = (CmdModels.IsPinCorrect) context.getCommand().getBody();

        //validation
        if (!ValidationUtil.isValidPin(cmd.getPin())) {
            context.replyError(-1, "Pin is not valid.");
            return;
        }

        mainDb.mPhonesDb.getPhoneObjInfo(cmd.getPhoneNumber(), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(final PhonesDb.Obj phoneObj) {

                if (phoneObj == null) {
                    context.replyError(100, "Agent not found");
                    return;
                }

                final SockData sockData = new SockData(vertx, logger, config);
                sockData.setPhoneObj(phoneObj, logger, "Web set phone object at begin request login");
                sockData.isSetup = true;

                MomoMessage msg = new MomoMessage(MomoProto.MsgType.LOGIN_VALUE
                        , System.currentTimeMillis()
                        , cmd.getPhoneNumber(),
                        MomoProto.LogIn.newBuilder()
                                .setMpin(cmd.getPin())
                                .setDeviceModel("")
                                .build().toByteArray()
                );

                connectProcess.processLogIn(null, msg, sockData, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject loginResult) {
                        int error = loginResult.getInteger("error", 100);
                        long lockTill = loginResult.getLong("lockTill", 0);

                        CmdModels.IsPinCorrectReply.ResultCode resultCode = CmdModels.IsPinCorrectReply.ResultCode.CORRECT;

                        //builder
                        CmdModels.IsPinCorrectReply.Builder builder = CmdModels.IsPinCorrectReply.newBuilder();
                        builder.setPhoneNumber(cmd.getPhoneNumber());

                        MomoCommand replyCommand;

                        switch (error) {
                            case MomoProto.LogInReply.ResultCode.ALL_OK_VALUE:

                                logger.info(cmd.getPhoneNumber() + " logged in by web.");
                                StatisticUtils.fireLogin(vertx.eventBus(), cmd.getPhoneNumber(), StatisticModels.Action.Channel.WEB);
                                sockData.pin = cmd.getPin();
                                context.getSockDatas().put(cmd.getPhoneNumber(), sockData);
                                break;

                            case MomoProto.LogInReply.ResultCode.AUTH_EXPIRED_VALUE:
                                resultCode = CmdModels.IsPinCorrectReply.ResultCode.AUTH_EXPIRED;
                                break;

                            case MomoProto.LogInReply.ResultCode.AUTH_RETRY_EXCEED_VALUE:
                                resultCode = CmdModels.IsPinCorrectReply.ResultCode.AUTH_RETRY_EXCEED;
                                break;

                            case MomoProto.LogInReply.ResultCode.PIN_INVALID_VALUE:
                                resultCode = CmdModels.IsPinCorrectReply.ResultCode.INCORRECT;
                                break;

                            default:

                                replyCommand = new MomoCommand(CmdModels.CommandType.ERROR
                                        , CmdModels.Error.newBuilder()
                                        .setCode(error)
                                        .build());
                                context.reply(replyCommand);
                                return;
                        }

                        if (resultCode != null) {

                            builder.setResult(resultCode)
                                    .setLocktill(lockTill);

                            replyCommand = new MomoCommand(CmdModels.CommandType.IS_PIN_CORRECT_REPLY
                                    , builder.build());
                            context.reply(replyCommand);
                        }
                    }
                });
            }
        });
    }

    public void changePin(final CommandContext context) {
        final CmdModels.ChangePin cmd = (CmdModels.ChangePin) context.getCommand().getBody();

        //validation
        if (!ValidationUtil.isValidPin(cmd.getOldPin())) {
            context.replyError(-1, "Old Pin is not valid.");
            return;
        }
        if (!ValidationUtil.isValidPin(cmd.getNewPin())) {
            context.replyError(-1, "New Pin is not valid.");
            return;
        }

        String fullPhoneNhumber = "0" + cmd.getPhoneNumber();

        //doing business
        Buffer changePin = MomoMessage.buildBuffer(
                SoapProto.MsgType.CHANGE_PIN_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.ChangePin.newBuilder()
                        .setNumber(fullPhoneNhumber)
                        .setOldPin(cmd.getOldPin())
                        .setNewPin(cmd.getNewPin())
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, changePin, new Handler<Message<Integer>>() {
            @Override
            public void handle(Message<Integer> result) {
                CmdModels.ChangePinReply.ResultCode resultCode;
                CmdModels.ChangePinReply replyBody;
                MomoCommand replyCommand;

                switch (result.body()) {
                    case 0:
                        resultCode = CmdModels.ChangePinReply.ResultCode.SUCCESS;
                        break;
                    case 1014:
                        resultCode = CmdModels.ChangePinReply.ResultCode.PIN_INCORRECT;
                        break;
                    case 1015:
                        resultCode = CmdModels.ChangePinReply.ResultCode.SAME_PASSWORD;
                        break;
                    case 1016:
                        resultCode = CmdModels.ChangePinReply.ResultCode.PASSWORD_PREVIOUS_USED;
                        break;
                    default:
                        replyCommand = new MomoCommand(CmdModels.CommandType.ERROR, CmdModels.Error.newBuilder().setCode(result.body()).build());
                        context.reply(replyCommand);
                        return;
                }
                replyBody = CmdModels.ChangePinReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(resultCode)
                        .build();
                replyCommand = new MomoCommand(CmdModels.CommandType.CHANGE_PIN_REPLY, replyBody);
                context.reply(replyCommand);
            }
        });
    }

    public void updateAgentInfo(final CommandContext context) {
        final CmdModels.UpdateAgentInfo cmd = (CmdModels.UpdateAgentInfo) context.getCommand().getBody();

        SockData sockData = context.getSockData(cmd.getPhoneNumber());
        if (sockData == null) {
            context.replyError(-1, "Login required!");
            return;
        }
        MomoMessage msg = new MomoMessage(MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE, 0, cmd.getPhoneNumber(),
                MomoProto.Register.newBuilder()
                        .setAddress(cmd.getAddress())
                        .setCardId(cmd.getCardId())
                        .setEmail(cmd.getEmail())
                        .setName(cmd.getName())
                        .build().toByteArray()
        );

        infoFactory.processAgentModify(null, msg, sockData, new Handler<Boolean>() {
            @Override
            public void handle(Boolean isOk) {

                if (!isOk) {
                    CmdModels.UpdateAgentInfoReply replyBody = CmdModels.UpdateAgentInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.UpdateAgentInfoReply.ResultCode.FAIL)
                            .build();
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.UPDATE_AGENT_INFO_REPLY, replyBody);
                    context.reply(replyCommand);
                }

                CmdModels.UpdateAgentInfoReply replyBody = CmdModels.UpdateAgentInfoReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(CmdModels.UpdateAgentInfoReply.ResultCode.SUCCESS)
                        .build();
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.UPDATE_AGENT_INFO_REPLY, replyBody);
                context.reply(replyCommand);
            }
        });


        /*optional string name = 1;
        optional string card_id = 2;
        optional string address = 3;
        optional string email = 4;*/

/*        Buffer agentInfo = MomoMessage.buildBuffer(
                SoapProto.MsgType.AGENT_INFO_MODIFY_VALUE,
                0,
                cmd.getPhoneNumber(),
                SoapProto.AgentInfoModify.newBuilder()
                        .setName(cmd.getName() == null ? "" : cmd.getName())
                        .setCardId(cmd.getCardId() == null ? "" : cmd.getCardId())
                        .setAddress(cmd.getAddress() == null ? "" : cmd.getAddress())
                        .setEmail(cmd.getEmail() == null ? "" : cmd.getEmail())
                        .setDob(cmd.getDob() == null ? "" : cmd.getDob())
                        .build()
                        .toByteArray()
        );

        vertx.eventBus().send(SoapVerticle.ADDRESS, agentInfo, new Handler<CoreMessage<Boolean>>() {
            @Override
            public void handle(CoreMessage<Boolean> result) {
                boolean isOk = result.body();

                if (!isOk) {
                    CmdModels.UpdateAgentInfoReply replyBody = CmdModels.UpdateAgentInfoReply.newBuilder()
                            .setPhoneNumber(cmd.getPhoneNumber())
                            .setResult(CmdModels.UpdateAgentInfoReply.ResultCode.FAIL)
                            .build();
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.UPDATE_AGENT_INFO_REPLY, replyBody);
                    context.reply(replyCommand);
                }

                CmdModels.UpdateAgentInfoReply replyBody = CmdModels.UpdateAgentInfoReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setResult(CmdModels.UpdateAgentInfoReply.ResultCode.SUCCESS)
                        .build();
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.UPDATE_AGENT_INFO_REPLY, replyBody);
                context.reply(replyCommand);
            }
        });*/
    }

    public void getAvatarUploadToken(final CommandContext context) {
        final CmdModels.GetAvatarUploadToken cmd = (CmdModels.GetAvatarUploadToken) context.getCommand().getBody();
        JsonObject queryCmd = new JsonObject()
                .putString("phoneNumber", "0" + cmd.getPhoneNumber());
        vertx.eventBus().send(UserResourceVerticle.CMD_GET_TOKEN, queryCmd, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                String token = event.body().getString("token", "");

                CmdModels.GetAvatarUploadTokenReply replyBody = CmdModels.GetAvatarUploadTokenReply.newBuilder()
                        .setPhoneNumber(cmd.getPhoneNumber())
                        .setToken(token)
                        .build();
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_AVATAR_UPLOAD_TOKEN_REPLY, replyBody);
                context.reply(replyCommand);
            }
        });
    }

    public void isMomoer(final CommandContext context) {
        final CmdModels.IsMomoer cmd = (CmdModels.IsMomoer) context.getCommand().getBody();

        MomoMessage momoMessage = new MomoMessage(SoapProto.MsgType.CHECK_USER_STATUS_VALUE
                , 0
                , cmd.getPhoneNumber()
                , "".getBytes());
        vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, momoMessage.toBuffer(), new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> response) {
                MomoMessage momo = MomoMessage.fromBuffer(response.body());
                boolean isRegistered = false;
                try {
                    MomoProto.RegStatus status = MomoProto.RegStatus.parseFrom(momo.cmdBody);

                    isRegistered = status.getIsReged();
                } catch (InvalidProtocolBufferException e) {
                    logger.error(e.getMessage(), e);
                }

                CmdModels.IsMomoerReply replyBody = CmdModels.IsMomoerReply.newBuilder()
                        .setIsMomoer(isRegistered)
                        .build();
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.IS_MOMOER_REPLY, replyBody);
                context.reply(replyCommand);
            }
        });

    }

    public void countAgentTran(final CommandContext context) {
        final CmdModels.CountAgentTran cmd = (CmdModels.CountAgentTran) context.getCommand().getBody();

        tranDb.countAgentTran(cmd.getPhone(), new Handler<Long>() {
            @Override
            public void handle(Long agentTranNumber) {
                CmdModels.CountAgentTranReply replyBody = CmdModels.CountAgentTranReply.newBuilder()
                        .setCounter(agentTranNumber)
                        .build();
                MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.COUNT_AGENT_TRAN_REPLY, replyBody);
                context.reply(replyCommand);
            }
        });
    }
}
