package com.mservice.momo.gateway.internal.core;

/**
 * Created by concu on 5/13/14.
 */

import com.mservice.momo.data.model.Const;
import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.msg.CoreMessage;
import com.mservice.momo.gateway.internal.core.objects.*;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//todo this one must be deploy as a worker
public class NewCoreConnectorVerticle extends Verticle {
//    public static final String ADDRESS = "momo.CoreConnector";

    private static byte[] EMPTY_BYTES = "".getBytes();
    private final Object RECV_LOCK = new Object();
    private final Object OBJ_SEND_LOCK = new Object();
    ConcurrentLinkedDeque mInComeQueque = new ConcurrentLinkedDeque();
    int totalSend = 0;
    int totalRecv = 0;
    //private CoreCommon common;
    private String SERVER_IP = "";
    private int SERVER_PORT = 0;
    private String INITIATOR_ACC = "";
    private String INITIATOR_PIN = "";
    private int CORE_CLIENT_ID = -1;
    private int CORE_CLIENT_PASS = -1;
    private Logger mLogger;
    private AtomicLong indexGenerator = new AtomicLong(0L);
    private ConcurrentHashMap<Long, org.vertx.java.core.eventbus.Message> waitTask = new ConcurrentHashMap<>();
    private ConcurrentLinkedDeque<org.vertx.java.core.eventbus.Message> sendTasks = new ConcurrentLinkedDeque<>();

    private Buffer mSendingData;
    private AtomicLong mLastTranAck = new AtomicLong(-1);
    private long mLastTranReq = -1;
    private long mLastTranTime = -1;

    private long mLastSendTime = 0;
    private long mLastRecvTime = 0;
    private long mLastEchoTime = 0;
    private long mSendTimerId = -1;
    private AtomicInteger _debug_send = new AtomicInteger(0);
    private AtomicInteger _debug_ack = new AtomicInteger(0);

    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private boolean mCanWrite = false;
    private boolean mCanRead = false;

    private ExecutorService executors = Executors.newFixedThreadPool(2);

    private boolean mIsConnected = false;

    /*
    Gửi tiền: VOUCHERIN pin recipientname recipient providername provider amount
        VOUCHERIN : ma lenh
        recipientname: ten nguoi nhan
        recipient : so dien thoai nguoi nhan
        providername: ten nguoi gui
        provider:  so dien thoai nguoi gui
        keyvaluepair tu add
        MTCN == couponid

    Kiểm tra thông tin giao dịch :  VOUCHEROUTINIT pin MTCN amount

    Nhận tiền : VOUCHEROUTCNFRM pin
    */
    private AtomicInteger _debug_reply = new AtomicInteger(0);
    private AtomicInteger _orphan_reply = new AtomicInteger(0);
    private long loadId = 0;
    private AtomicInteger getBalanceReturn = new AtomicInteger(0);
    private AtomicLong pauseTillTime = new AtomicLong(0);
    private long beginTime = 0;
    private long endTime = 0;
    private long minWaitTime = Long.MAX_VALUE;
    private long maxWaitTime = 0;

    public static void getBalance(Vertx _vertx
            , final String number
            , final String pin
            , final Logger mLogger
            , final Handler<Integer> callback) {

        Request loginObj = new Request();
        loginObj.TYPE = Command.BALANCE;
        loginObj.SENDER_NUM = number;
        loginObj.SENDER_PIN = pin;

        /*jo.putString(Structure.TYPE, BALANCE);
        jo.putNumber(Structure.SENDER_NUM, number);
        jo.putString(Structure.SENDER_PIN, pin);*/

        JsonObject loginJo = loginObj.toJsonObject();

        _vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, loginJo, new Handler<Message<Buffer>>() {
            @Override
            public void handle(Message<Buffer> message) {
                CoreMessage coreMessage = CoreMessage.fromBuffer(message.body());
                Core.StandardReply reply;
                try {
                    reply = Core.StandardReply.parseFrom(coreMessage.cmdBody);
                } catch (Exception ex) {
                    reply = null;
                }

                if (reply == null) {
                    mLogger.info("getBalance : can not parse reveived buffer from core , number : " + number);
                    callback.handle(-1);
                    return;
                }

                //final long rplTranId = reply.getTid();
                mLogger.info("getBalance, ecode: " + reply.getTid() + "," + reply.getErrorCode());
                callback.handle(reply.getErrorCode());
            }
        });
    }

    public void start() {

        JsonObject connector_default = new JsonObject();
        connector_default.putString("server_ip", "0.0.0.0");
        connector_default.putNumber("server_port", 6969);
        connector_default.putString("initiator_acc", "confirm");
        connector_default.putString("initiator_pin", "000000");
        connector_default.putString("core_client_id", "backend");
        connector_default.putString("core_client_pass", "696969");

        JsonObject cfg = container.config().getObject("core_connector", connector_default);
//        JsonObject cfg = container.config(); //backup
        mLogger = getContainer().logger();


        SERVER_IP = cfg.getString("server_ip", "0.0.0.0");
        SERVER_PORT = cfg.getInteger("server_port", 6969);
        INITIATOR_ACC = cfg.getString("initiator_acc", "nothing");
        INITIATOR_PIN = cfg.getString("initiator_pin", "nopin");
        CORE_CLIENT_ID = cfg.getInteger("core_client_id", 0);
        CORE_CLIENT_PASS = cfg.getInteger("core_client_pass", 0);

        //common = new CoreCommon(vertx,mLogger);

        Handler<org.vertx.java.core.eventbus.Message> myHandler = new Handler<org.vertx.java.core.eventbus.Message>() {
            public void handle(org.vertx.java.core.eventbus.Message message) {
                sendTasks.add(message);
            }
        };

        vertx.eventBus().registerHandler(AppConstant.CoreConnectorVerticle_ADDRESS, myHandler);


        Runnable connectAndSend = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //if socket
                    mIsConnected = false;
                    mCanRead = false;
                    mCanWrite = false;

                    if (mSocket != null) {
                        //1 force close
                        try {
                            mSocket.close();
                        } catch (Exception e) {

                        }
                    }

                    try {
                        mLogger.info(String.format("Connecting to %s:%d", SERVER_IP, SERVER_PORT));
                        mSocket = new Socket();
                        mSocket.setKeepAlive(true);
                        mSocket.setTcpNoDelay(true);
                        mSocket.setReceiveBufferSize(1024 * 100);
                        mSocket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 5000);
                        mLogger.info(String.format("Connected to %s:%d", SERVER_IP, SERVER_PORT));
                        mInputStream = mSocket.getInputStream();
                        mOutputStream = mSocket.getOutputStream();
                        mCanRead = true;
                        mIsConnected = true;
                        mLogger.info("send Hello <" + CORE_CLIENT_ID + "><" + CORE_CLIENT_PASS + ">");
                        CoreMessage hello = new CoreMessage(Core.MsgType.HELLO_VALUE
                                , CORE_CLIENT_PASS
                                , CORE_CLIENT_ID
                                , EMPTY_BYTES);
                        writeData(hello.toBuffer());
                    } catch (Exception e) {
                        mLogger.info("Can not connect to core proto - retry in next 3 seconds", e);
                        e.printStackTrace();

                        mIsConnected = false;
                    }

                    if (!mIsConnected) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        continue;
                    }
                    //
                    while (mIsConnected) {
                        bloodBeat();
                    }

                }
            }
        };


        Runnable read = new Runnable() {
            private Buffer fullBuffer = new Buffer(102400);

            @Override
            public void run() {
                while (true) {
                    if (mIsConnected && mCanRead && mInputStream != null) {
                        try {
                            byte[] buf = new byte[1024];
                            int readlen = mInputStream.read(buf, 0, 1024);
                            if (readlen > 0) {
                                fullBuffer.appendBytes(buf, 0, readlen);

                                int fullLen = fullBuffer.length();
                                while (fullLen >= CoreMessage.MIN_LEN) {
                                    byte first = fullBuffer.getByte(0);
                                    if (first == CoreMessage.STX_BYTE) {
                                        int len = CoreMessage.getLen(fullBuffer);
                                        if (len > fullLen) {
                                            //ko du du lieu, doi vong tiep theo
                                            break;
                                        } else {
                                            int typ = CoreMessage.getType(fullBuffer);
                                            long idx = CoreMessage.getIndex(fullBuffer);
                                            int phn = CoreMessage.getPhone(fullBuffer);
                                            byte[] bdy = fullBuffer.getBytes(CoreMessage.HEAD_LEN, len - 1);
                                            byte lst = fullBuffer.getByte(len - 1);
                                            if (lst != CoreMessage.ETX_BYTE) {
                                                mLogger.info(" WRONG DATA RECEIVED ");
                                            }

                                            CoreMessage newMsg = new CoreMessage(typ, idx, phn, bdy);

                                            onMessageComing(newMsg);

                                            if (len == fullLen) {
                                                fullBuffer = fullBuffer.getBuffer(0, 0);
                                            } else {
                                                fullBuffer = fullBuffer.getBuffer(len, fullBuffer.length());
                                            }

                                            fullLen = fullLen - len;

                                            mLogger.info("process done <" + len + "><" + fullLen + ">");

                                        }
                                    } else {
                                        mLogger.info("first is not CoreMessage.STX_BYTE ");
                                        fullBuffer = fullBuffer.getBuffer(1, fullLen);
                                        fullLen--;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            disConnect("on read " + e.getMessage());
                        }
                    } else {
                        //wait for a while
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {

                        }
                    }


                }
            }
        };

        executors.submit(connectAndSend);
        executors.submit(read);

    }


    private void onMessageComing(CoreMessage msg) {
        Core.MsgType type = Core.MsgType.valueOf(msg.cmdType);
        mLogger.info("Recevied msg type " + type.name());

        switch (msg.cmdType) {
            case Core.MsgType.ACK_VALUE:
                mLogger.info("ack " + _debug_ack.incrementAndGet() + " ACK " + msg.cmdIndex);
                mLastTranAck.set(msg.cmdIndex);
                break;
            case Core.MsgType.HELLO_REPLY_VALUE:
                long mReconnectTranAck = msg.cmdIndex;
                if (mReconnectTranAck < mLastTranReq) {
                    //bi mat du lieu -> thuc hien gui lai
                    Set<Long> resendKey = new HashSet<Long>();

                    for (long idx : waitTask.keySet()) {
                        if (idx > mReconnectTranAck) {
                            //move this from wait to un send
                            resendKey.add(idx);
                        }
                    }

                    for (long ridx : resendKey) {
                        sendTasks.add(waitTask.remove(ridx));
                    }
                }

                indexGenerator.set(mReconnectTranAck);
                mLastTranAck.set(mReconnectTranAck);
                mSendingData = null;
                pauseTillTime.set(System.currentTimeMillis() + 5000);
                mLogger.info("Will write data in 5 seconds...");
                mCanWrite = true;

                break;

            case Core.MsgType.STANDARD_REPLY_VALUE:
                Core.StandardReply srpl;
                try {
                    srpl = Core.StandardReply.parseFrom(msg.cmdBody);
                } catch (Exception e) {
                    srpl = null;
                }

                if (srpl != null) {
                    if (srpl.getErrorCode() == 9999) {
                        mLogger.info("CORE OVER LIMIT, PAUSE 1 SECONDS");
                        pauseTillTime.set(System.currentTimeMillis() + 1000);
                    }

                    sendAck(msg);
                    onCoreReply(msg, srpl);
                    mLogger.info("STANDARD_REPLY_VALUE " + _debug_reply.incrementAndGet());
                } else {
                    disConnect("CAN NOT PARSE MESSAGE, LOST DATA HERE, RECONNECT");
                    return;
                }

                break;
            case Core.MsgType.ECHO_REPLY_VALUE:
                mLogger.info("msg reply from core : ECHO_REPLY_VALUE");
                break;

            default:
                mLogger.info("CoreConnectorVerticle not support command " + type.name());
                break;
        }
    }

    private void disConnect(String reason) {
        mLogger.info("Disconnect with core, reason " + reason);
        try {
            mIsConnected = false;
            mCanWrite = false;
            mCanRead = false;
            mSocket.close();
        } catch (Exception e) {
            mLogger.info("disconnect exception ", e);
        }
    }


    private void onCoreReply(CoreMessage msgRcv, Core.StandardReply srpl) {
        //lay msg ra
        Message<JsonObject> orgMessage = waitTask.get(msgRcv.cmdIndex);

        if (orgMessage != null) {
            boolean result = false;
            JsonObject waitObj = orgMessage.body();

            if (waitObj != null) {
                String _type = waitObj.getString(Structure.TYPE, "");

                if (_type.equalsIgnoreCase(Command.ROLLBACK)) {
                    result = srpl.getErrorCode() == 103;
                } else {
                    result = srpl.getErrorCode() == 0;
                }
            }

            if (!result) {
                mLogger.info("Core tra ve loi " + srpl == null ? "srpl is null " : srpl.getErrorCode());
            } else {
                mLogger.info("Core tra ve thanh cong " + srpl == null ? "srpl is null " : srpl.getErrorCode());
            }

            org.vertx.java.core.eventbus.Message doneMessage = waitTask.remove(msgRcv.cmdIndex);

            long waitTime = System.currentTimeMillis() - orgMessage.body().getLong("sendTime");
            minWaitTime = Math.min(minWaitTime, waitTime);
            maxWaitTime = Math.max(maxWaitTime, waitTime);

            Core.StandardReply.Builder builder = Core.StandardReply.newBuilder()
                    .setErrorCode(result ? 0 : srpl.getErrorCode())
                    .setTid(srpl.getTid())
                    .setDescription(srpl.getDescription());

            for (int i = 0; i < srpl.getParamsCount(); i++) {
                builder.addParams(Core.KeyValuePair.newBuilder()
                        .setKey(srpl.getParams(i).getKey())
                        .setValue(srpl.getParams(i).getValue()));
            }

            Buffer bufferRpl = CoreMessage.buildBuffer(
                    0
                    , 0
                    , 0
                    , builder.build().toByteArray()
            );

            orgMessage.reply(bufferRpl);
        } else {
            mLogger.info("No one wait for " + msgRcv.cmdIndex + " totalSend miss " + _orphan_reply.incrementAndGet() + " queue " + waitTask.size());
        }
    }


    public void bloodBeat() {
        if (mIsConnected && mCanWrite) {

            if (mSendingData != null && mLastTranReq > mLastTranAck.get()) {
                if (System.currentTimeMillis() - mLastTranTime > 5000) {
                    mLogger.info("resend message <" + mLastTranReq + "><" + mLastTranAck + "> after 5 seconds from " + mLastTranTime);
                    writeData(mSendingData);
                    return;
                }
            } else if (sendTasks.size() > 0 && System.currentTimeMillis() >= pauseTillTime.get()) {
                mLogger.info("new mLastTranReq<" + mLastTranReq + "> - mLastTranAck<" + mLastTranAck.get() + "> - sendTasks.size<" + sendTasks.size() + ">");

                Message<JsonObject> joMessage = null;
                try {
                    joMessage = sendTasks.poll();
                } catch (Exception e) {
                    mLogger.info("Can not get task", e);
                    joMessage = null;
                }


                if (joMessage != null && joMessage.body() != null) {

                    JsonObject jo = joMessage.body();
                    Request reqObj = new Request(jo);

                    byte[] msgBody = null;
                    int msgType = -1;

                    Common.BuildLog log = new Common.BuildLog(mLogger);
                    log.setPhoneNumber(reqObj.PHONE_NUMBER);
                    log.setTime(reqObj.TIME);
                    log.add("cmdType", reqObj.TYPE);
                    log.add("json request", jo);

                    String _type = reqObj.TYPE;

                    if (_type.equalsIgnoreCase(Command.ADJUST)) {
                        String sender_num = reqObj.SENDER_NUM;
                        String recver_num = reqObj.RECVER_NUM;
                        String adjust_typ = reqObj.ADJUST_TYP;
                        long tran_amount = reqObj.TRAN_AMOUNT;

                        log.add("type", "ADJUST");
                        log.add("Initiator", INITIATOR_ACC);
                        log.add("InitiatorPinlen", INITIATOR_PIN.length());
                        log.add("sender_num", sender_num);
                        log.add("recver_num", recver_num);
                        log.add("amount", tran_amount);
                        log.add("adjust_type", adjust_typ);

                        log.writeLog();

                        msgType = Core.MsgType.MOMO_ADJUST_VALUE;

                        msgBody = Core.MomoAdjust.newBuilder()
                                .setInitiator(INITIATOR_ACC)
                                .setMpin(INITIATOR_PIN)
                                .setSource(sender_num)
                                .setAmount(tran_amount)
                                .setTarget(recver_num)
                                .setAdjustType(adjust_typ)
                                .setDescription("adjust")
                                .build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.TRANSFER_WITH_LOCK)) {

                        String sender_num = reqObj.SENDER_NUM;
                        String sender_pin = reqObj.SENDER_PIN;  //jo.getString(Structure.SENDER_PIN, "");
                        String target = reqObj.TARGET;          //jo.getString(Structure.RECVER_NUM, "");
                        long tran_amount = reqObj.TRAN_AMOUNT;  //jo.getLong(Structure.TRAN_AMOUNT, 0);
                        int waType = reqObj.WALLET;
                        Core.WalletType walletType = getWalletType(waType);

                        log.add("**************", "begin process transfer with lock");
                        log.add("sender_num", sender_num);
                        log.add("SendPinlen", sender_pin.length());
                        log.add("target", target); // treo tien den tai khoan nay
                        log.add("amount", tran_amount);
                        log.add("wallet type", walletType.name());
                        log.add("descption", reqObj.DESCRIPTION);

                        msgType = Core.MsgType.INIT_ADJUST_VALUE;

                        Core.InitAdjust.Builder builder = Core.InitAdjust.newBuilder();
                        builder.setInitiator(sender_num)
                                .setMpin(sender_pin)
                                .setAmount(tran_amount)
                                .setTarget(target)
                                .setWallettype(walletType)
                                .setDescription(reqObj.DESCRIPTION.replace(" ", "_"))
                                .setType(Core.TransType.TRANSFER);

                        if (reqObj.KeyValueList != null && reqObj.KeyValueList.size() > 0) {
                            for (int i = 0; i < reqObj.KeyValueList.size(); i++) {
                                KeyValue kv = reqObj.KeyValueList.get(i);
                                builder.addParams(Core.KeyValuePair.newBuilder()
                                        .setKey(kv.Key)
                                        .setValue(kv.Value));
                                log.add(kv.Key, kv.Value);
                            }
                        }

                        log.writeLog();

                        msgBody = builder.build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.COMMIT)) {

                        long tranId = reqObj.TRAN_ID;// jo.getLong(Structure.TRAN_ID, 0);
                        log.add("**************", "begin process commit tran");
                        log.add("Initiator", INITIATOR_ACC);
                        log.add("InitiatorPinlen", INITIATOR_PIN.length());
                        log.add("tranid", tranId);

                        log.writeLog();

                        msgType = Core.MsgType.CONFIRM_ADJUST_VALUE;
                        msgBody = Core.ConfirmAdjust.newBuilder()
                                .setInitiator(INITIATOR_ACC)
                                .setMpin(INITIATOR_PIN)
                                .setTid(tranId)
                                .setAction("commit")
                                .setDescription("")
                                .build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.ROLLBACK)) {
                        long tranId = reqObj.TRAN_ID;// jo.getLong(Structure.TRAN_ID, 0);
                        log.add("**************", "begin process rollback tran");
                        log.add("Initiator", INITIATOR_ACC);
                        log.add("InitiatorPinlen", INITIATOR_PIN.length());
                        log.add("tranid", tranId);

                        log.writeLog();

                        msgType = Core.MsgType.CONFIRM_ADJUST_VALUE;
                        msgBody = Core.ConfirmAdjust.newBuilder()
                                .setInitiator(INITIATOR_ACC)
                                .setMpin(INITIATOR_PIN)
                                .setTid(tranId)
                                .setAction("rollback")
                                .setDescription("")
                                .build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.BALANCE)) {
                        String sender_Num = reqObj.SENDER_NUM;     //jo.getInteger(Structure.SENDER_NUM);
                        String pin = reqObj.SENDER_PIN;             //o.getString(Structure.SENDER_PIN);
                        msgType = Core.MsgType.MOMO_BALANCE_VALUE;
                        msgBody = Core.MomoBalance.newBuilder()
                                .setInitiator(sender_Num)
                                .setMpin(pin)
                                .build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.BILL_PAY)) {
                        String sender_num = reqObj.SENDER_NUM;
                        String sender_pin = reqObj.SENDER_PIN;
                        String recver_num = reqObj.RECVER_NUM;//so dien thoai hoac ma hoa don
                        long tran_amount = reqObj.TRAN_AMOUNT;//gia tri giao dich
                        String target = reqObj.TARGET; // tai khoan dac biet de nhan tien

                        int wallet = reqObj.WALLET;

                        log.add("**************", "begin process billpay");
                        log.add("sender_number", sender_num);
                        log.add("sender_pin len", sender_pin.length());
                        log.add("biller Id", recver_num);
                        log.add("tran_amount", tran_amount);
                        log.add("target", target);

                        log.writeLog();

                        msgType = Core.MsgType.INIT_ADJUST_VALUE;
                        Core.InitAdjust.Builder builder = Core.InitAdjust.newBuilder();
                        builder.setInitiator(sender_num)
                                .setMpin(sender_pin)
                                .setAmount(tran_amount)
                                .setTarget(target)
                                .setWallettype(getWalletType(wallet))
                                .setDescription(reqObj.DESCRIPTION)
                                .setType(Core.TransType.BILLPAY);

                        if (reqObj.KeyValueList != null && reqObj.KeyValueList.size() > 0) {
                            for (int i = 0; i < reqObj.KeyValueList.size(); i++) {
                                KeyValue kv = reqObj.KeyValueList.get(i);
                                builder.addParams(Core.KeyValuePair.newBuilder()
                                        .setKey(kv.Key)
                                        .setValue(kv.Value));
                                log.add(kv.Key, kv.Value);
                            }
                        }

                        //luon add them kvp issms
                        builder.addParams(Core.KeyValuePair.newBuilder().setKey(Const.CoreVC.IsSms).setValue("no"));

                        msgBody = builder.build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.TOPUP)) {
                        String sender_num = reqObj.SENDER_NUM;
                        String sender_pin = reqObj.SENDER_PIN;// jo.getString(Structure.SENDER_PIN, "");
                        String recver_num = reqObj.RECVER_NUM;// so dien thoai nhan tien topup
                        long tran_amount = reqObj.TRAN_AMOUNT;// jo.getLong(Structure.TRAN_AMOUNT, 0);
                        int wallet = reqObj.WALLET;

                        log.add("**************", "begin process topup");
                        log.add("sender_number", sender_num);
                        log.add("sender_pin len", sender_pin.length());
                        log.add("recver_num", recver_num);
                        log.add("tran_amount", tran_amount);
                        log.add("recipient", recver_num);

                        Core.InitAdjust.Builder builder = Core.InitAdjust.newBuilder();
                        builder.setInitiator(sender_num)
                                .setMpin(sender_pin)
                                .setAmount(tran_amount)
                                .setTarget("airtime")

                                .setDescription(reqObj.DESCRIPTION)
                                .setType(Core.TransType.BUY)
                                .setWallettype(getWalletType(wallet));

                        if (reqObj.KeyValueList != null && reqObj.KeyValueList.size() > 0) {
                            for (int i = 0; i < reqObj.KeyValueList.size(); i++) {
                                KeyValue kv = reqObj.KeyValueList.get(i);
                                builder.addParams(Core.KeyValuePair.newBuilder()
                                        .setKey(kv.Key)
                                        .setValue(kv.Value));
                                log.add(kv.Key, kv.Value);
                            }
                        }

                        //luon add them kvp issms
                        builder.addParams(Core.KeyValuePair.newBuilder().setKey(Const.CoreVC.IsSms).setValue("no"));

                        log.writeLog();

                        msgType = Core.MsgType.INIT_ADJUST_VALUE;
                        msgBody = builder.build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.REGISTER)) {

                        String number = jo.getString(Structure.AGENT_NUMBER);
                        String name = jo.getString(Structure.AGENT_NAME);
                        String idcard = jo.getString(Structure.AGENT_ID_CARD);
                        String email = jo.getString(Structure.AGENT_EMAIL);
                        String pin = jo.getString(Structure.AGENT_PIN);

                        log.add("agent number", number);
                        log.add("agent name", name);
                        log.add("agent idcard", idcard);
                        log.add("agent email", email);
                        log.add("agent pin len", pin.length());
                        log.writeLog();

                        msgType = Core.MsgType.MOMO_REGISTER_VALUE;
                        msgBody = Core.MomoRegister.newBuilder()
                                .setInitiator(INITIATOR_ACC)
                                .setMpin(INITIATOR_PIN)
                                .setMomoPhone(number)
                                .setMomoPin(pin)
                                .setMomoName(name)
                                .setPersonalId(idcard)
                                .setMomoEmail(email)
                                .setMomoGroup("NONAME")
                                .build().toByteArray();
                    } else if (_type.equalsIgnoreCase(Command.TRAN_WITH_VOUCHER_AND_POINT)) {
                        String intiator = jo.getString(Structure.INITIATOR);
                        String sms = jo.getString(Structure.SMS);
                        log.add("intiator", intiator);
                        log.add("sms", sms);
                        log.writeLog();
                        msgType = Core.MsgType.GENERIC_REQUEST_VALUE;
                        msgBody = Core.GenericRquest.newBuilder()
                                .setInitiator(intiator)
                                .setSms(sms)
                                .build()
                                .toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.MOMO_MONEY_TO_VOUCHER)) {
                        String intiator = jo.getString(Structure.INITIATOR);
                        String pin = jo.getString(Structure.AGENT_PIN);
                        long amount = jo.getLong(Structure.TRAN_AMOUNT);
                        String sms = "MMONEYTOVOUCHER " + pin + " " + amount;

                        log.add("intiator", intiator);
                        log.add("sms", "MMONEYTOVOUCHER ****** " + amount);
                        log.writeLog();

                        mLogger.info(intiator + "| " + sms);

                        msgType = Core.MsgType.GENERIC_REQUEST_VALUE;
                        msgBody = Core.GenericRquest.newBuilder()
                                .setInitiator(intiator)
                                .setSms(sms)
                                .build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.VOUCHER_TOPOINT)) {
                        String intiator = jo.getString(Structure.INITIATOR);
                        String pin = jo.getString(Structure.AGENT_PIN);
                        long amount = jo.getLong(Structure.TRAN_AMOUNT);
                        String sms = "VOUCHERTOPOINT " + pin + " " + amount;

                        log.add("intiator", intiator);
                        log.add("sms command", "VOUCHERTOPOINT ****** " + amount);

                        mLogger.info(intiator + "| " + sms);
                        log.writeLog();

                        msgType = Core.MsgType.GENERIC_REQUEST_VALUE;
                        msgBody = Core.GenericRquest.newBuilder()
                                .setInitiator(intiator)
                                .setSms(sms)
                                .build().toByteArray();

                        //c2c.start
                    } else if (_type.equalsIgnoreCase(Command.RETAILER_TRANSFER_CASH)) {
                        String intiator = jo.getString(Structure.INITIATOR);
                        String sms = jo.getString(Structure.SMS);
                        JsonArray array = jo.getArray(Structure.KEY_VALUE_PAIR_ARR, new JsonArray());

                        Core.GenericRquest.Builder builder = Core.GenericRquest.newBuilder();
                        builder.setInitiator(intiator)
                                .setSms(sms);

                        for (int i = 0; i < array.size(); i++) {
                            JsonObject j = array.get(i);
                            for (String s : j.getFieldNames()) {
                                builder.addParams(Core.KeyValuePair.newBuilder()
                                        .setKey(s)
                                        .setValue(j.getString(s)));
                                break;
                            }
                        }

                        log.add("intiator", intiator);
                        log.add("sms", sms);
                        log.writeLog();
                        msgType = Core.MsgType.GENERIC_REQUEST_VALUE;
                        msgBody = builder
                                .build()
                                .toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.RETAILER_TRANSFER_CASH_RECOMMIT)) {

                        String sender_num = reqObj.SENDER_NUM;
                        String sender_pin = reqObj.SENDER_PIN;  //jo.getString(Structure.SENDER_PIN, "");
                        String target = reqObj.TARGET;          //jo.getString(Structure.RECVER_NUM, "");
                        long tran_amount = reqObj.TRAN_AMOUNT;  //jo.getLong(Structure.TRAN_AMOUNT, 0);
                        int waType = reqObj.WALLET;
                        Core.WalletType walletType = getWalletType(waType);

                        log.add("**************", "begin process c2c get information");
                        log.add("sender_num", sender_num);
                        log.add("SendPinlen", sender_pin.length());
                        log.add("target", target); // treo tien den tai khoan nay
                        log.add("amount", tran_amount);
                        log.add("wallet type", walletType.name());
                        log.add("descption", reqObj.DESCRIPTION);

                        msgType = Core.MsgType.INIT_ADJUST_VALUE;

                        Core.InitAdjust.Builder builder = Core.InitAdjust.newBuilder();
                        builder.setInitiator(sender_num)
                                .setMpin(sender_pin)
                                .setAmount(tran_amount)
                                .setTarget(target)
                                .setWallettype(walletType)
                                .setDescription(reqObj.DESCRIPTION.replace(" ", "_"))
                                .setType(Core.TransType.CASHOUT);

                        if (reqObj.KeyValueList != null && reqObj.KeyValueList.size() > 0) {
                            for (int i = 0; i < reqObj.KeyValueList.size(); i++) {
                                KeyValue kv = reqObj.KeyValueList.get(i);
                                builder.addParams(Core.KeyValuePair.newBuilder()
                                        .setKey(kv.Key)
                                        .setValue(kv.Value));
                                log.add(kv.Key, kv.Value);
                            }
                        }

                        log.writeLog();

                        msgBody = builder.build().toByteArray();

                    } else if (_type.equalsIgnoreCase(Command.RETAILER_TRANSFER_CASH_COMMIT)) {
                        String intiator = jo.getString(Structure.INITIATOR);
                        String sms = jo.getString(Structure.SMS);
                        JsonArray array = jo.getArray(Structure.KEY_VALUE_PAIR_ARR, new JsonArray());

                        Core.GenericRquest.Builder builder = Core.GenericRquest.newBuilder();
                        builder.setInitiator(intiator)
                                .setSms(sms);

                        for (int i = 0; i < array.size(); i++) {
                            JsonObject j = array.get(i);
                            for (String s : j.getFieldNames()) {
                                builder.addParams(Core.KeyValuePair.newBuilder()
                                        .setKey(s)
                                        .setValue(j.getString(s)));
                                break;
                            }
                        }
                        log.add("intiator", intiator);
                        log.add("sms", sms);
                        log.writeLog();
                        msgType = Core.MsgType.GENERIC_REQUEST_VALUE;
                        msgBody = builder
                                .build()
                                .toByteArray();

                    }

                    //c2c.end


                    else if (_type.equalsIgnoreCase(Command.TRANSFER)) {

                        String sender_num = reqObj.SENDER_NUM;
                        String sender_pin = reqObj.SENDER_PIN;
                        String recver_num = reqObj.RECVER_NUM;//so dien thoai hoac ma hoa don
                        long tran_amount = reqObj.TRAN_AMOUNT;//gia tri giao dich
                        String target = reqObj.TARGET; // tai khoan dac biet de nhan tien
                        int wallet = reqObj.WALLET;


                        log.add("**************", "begin process TRANSFER");
                        log.add("sender_number", sender_num);
                        log.add("sender_pin len", sender_pin.length());
                        log.add("biller Id", recver_num);
                        log.add("tran_amount", tran_amount);
                        log.add("target", target);
                        log.add("wallet", wallet);

                        log.writeLog();

                        String serviceType = "";
                        for (int i = 0; i < reqObj.KeyValueList.size(); i++) {
                            KeyValue kv = reqObj.KeyValueList.get(i);
                            if (Const.CoreVC.ServiceType.equalsIgnoreCase(kv.Key)) {
                                serviceType = kv.Value;
                                break;
                            }
                        }

                        msgType = Core.MsgType.MOMO_TRANSFER_VALUE;
                        msgBody = Core.MomoTransfer.newBuilder()
                                .setInitiator(sender_num)
                                .setMpin(sender_pin)
                                .setAmount(tran_amount)
                                .setTarget(target)
                                .setM2MType(serviceType)
                                .setDescription(reqObj.DESCRIPTION)
                                .build().toByteArray();
                    } else {
                        mLogger.info("DATA NOT CORRECT " + _type);
                        return;
                    }

                    if (msgType != -1) {

                        mLastTranReq = indexGenerator.incrementAndGet();
                        CoreMessage msg = new CoreMessage(
                                msgType
                                , mLastTranReq
                                , CORE_CLIENT_ID
                                , msgBody);
                        mSendingData = msg.toBuffer();
                        mLastTranTime = System.currentTimeMillis();

                        Buffer buffer = msg.toBuffer();
                        joMessage.body().putNumber("sendTime", System.currentTimeMillis());
                        waitTask.put(msg.cmdIndex, joMessage);
                        mLogger.info("send new message <" + _debug_send.incrementAndGet() + "><" + mLastTranReq + "><" + mLastTranAck.get() + ">");
                        writeData(mSendingData);
                    }
                    //mLogger.info("END SEND <" + System.currentTimeMillis() + ">");
                    return;
                } else {
                    mLogger.info("Khong lay duoc du lieu tu queue dang doi, sendTasks size " + sendTasks.size());
                }
            } else if (System.currentTimeMillis() - mLastEchoTime >= 10 * 1000) {

                mLastEchoTime = System.currentTimeMillis();
                mLogger.info("send ECHO message at " + mLastEchoTime);
                sendEcho();
            } else {
                //nothing to send, sleep a little bit
                try {
                    Thread.sleep(50);
                } catch (Exception e) {

                }
            }
        }

    }

    private void sendEcho() {
        if (mSocket != null && mCanWrite) {
            CoreMessage echo = new CoreMessage(Core.MsgType.ECHO_VALUE
                    , mLastSendTime
                    , CORE_CLIENT_ID
                    , EMPTY_BYTES);
            writeData(echo.toBuffer());
        }
    }

    private void writeData(Buffer buf) {
        try {
            mOutputStream.write(buf.getBytes());
            mOutputStream.flush();
        } catch (Exception e) {
            mLogger.info("Can not send data", e);
            disConnect("Can not send data from writeDate <" + buf.toString() + ">");
        }
    }

    private void sendAck(CoreMessage msg) {
        mLogger.info("SEND ACK " + msg.cmdIndex);
        CoreMessage ack = new CoreMessage(Core.MsgType.ACK_VALUE
                , msg.cmdIndex
                , CORE_CLIENT_ID
                , EMPTY_BYTES);
        writeData(ack.toBuffer());
    }

    private Core.WalletType getWalletType(int walletType) {
        switch (walletType) {
            case WalletType.MOMO:
                return Core.WalletType.MOMO;
            case WalletType.MLOAD:
                return Core.WalletType.MLOAD;
            case WalletType.POINT:
                return Core.WalletType.POINT;
            case WalletType.VOUCHER:
                return Core.WalletType.VOUCHER;
            default:
                return Core.WalletType.MOMO;
        }
    }


}