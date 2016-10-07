package com.mservice.momo.gateway.internal.core;

/**
 * Created by concu on 5/13/14.
 */

import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.gateway.internal.core.msg.CoreMessage;
import com.mservice.momo.gateway.internal.core.objects.Command;
import com.mservice.momo.gateway.internal.core.objects.Request;
import com.mservice.momo.gateway.internal.core.objects.Structure;
import com.mservice.momo.gateway.internal.core.objects.WalletType;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//todo this one must be deploy as a worker
public class CoreConnectorCDHHVerticle extends Verticle {
//    public static final String ADDRESS = "momo.CoreConnector";

    private static byte[] EMPTY_BYTES = "".getBytes();
    private final Object RECV_LOCK = new Object();
    ConcurrentLinkedDeque mInComeQueque = new ConcurrentLinkedDeque();
    int totalSend = 0;
    int totalRecv = 0;
    private String SERVER_IP = "";
    //private CoreCommon common;
    private int SERVER_PORT = 0;
    private String INITIATOR_ACC = "";
    private String INITIATOR_PIN = "";
    private int CORE_CLIENT_ID = -1;
    private int CORE_CLIENT_PASS = -1;
    private Logger mLogger;
    private AtomicLong indexGenerator = new AtomicLong(0L);
    private ConcurrentHashMap<Long, org.vertx.java.core.eventbus.Message> waitTask = new ConcurrentHashMap<>();
    private ConcurrentLinkedDeque<org.vertx.java.core.eventbus.Message> sendTasks = new ConcurrentLinkedDeque<org.vertx.java.core.eventbus.Message>();
    private NetSocket mSocket;
    private Buffer mSendingData;
    private AtomicLong mLastTranAck = new AtomicLong(-1);
    private long mLastTranReq = -1;
    private long mLastTranTime = -1;
    private boolean mCanWrite = false;
    private long mLastSendTime = 0;
    private long mLastRecvTime = 0;
    private long mLastEchoTime = 0;
    private long mSendTimerId = -1;
    private AtomicInteger _debug_send = new AtomicInteger(0);
    private AtomicInteger _debug_ack = new AtomicInteger(0);
    private AtomicInteger _debug_reply = new AtomicInteger(0);
    private AtomicInteger _orphan_reply = new AtomicInteger(0);
    private long loadId = 0;
    private boolean allow_run = true;
    private AtomicInteger getBalanceReturn = new AtomicInteger(0);
    private AtomicLong pauseTillTime = new AtomicLong(0);
    private Object OBJ_SEND_LOCK = new Object();
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

        JsonObject cfg = container.config().getObject("vote_connector", connector_default);
//        JsonObject cfg = container.config();
        mLogger = getContainer().logger();
        SERVER_IP = cfg.getString("server_ip", "0.0.0.0");
        SERVER_PORT = cfg.getInteger("server_port", 6969);
        INITIATOR_ACC = cfg.getString("initiator_acc", "nothing");
        INITIATOR_PIN = cfg.getString("initiator_pin", "nopin");
        CORE_CLIENT_ID = cfg.getInteger("core_client_id", 0);
        CORE_CLIENT_PASS = cfg.getInteger("core_client_pass", 0);

        Handler<org.vertx.java.core.eventbus.Message> myHandler = new Handler<org.vertx.java.core.eventbus.Message>() {
            public void handle(org.vertx.java.core.eventbus.Message message) {
                sendTasks.add(message);
            }
        };

        vertx.eventBus().registerHandler(AppConstant.CoreConnectorCDHHVerticle_ADDRESS, myHandler);

        vertx.setPeriodic(15, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                heartBeat();
            }
        });

        /*vertx.setPeriodic(5000,new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                mLogger.info("Total time " + (endTime - beginTime)/1000 + " seconds. Max wait : " + maxWaitTime + ". Min wait : " + minWaitTime);
            }
        });*/

        doConnect(SERVER_PORT, SERVER_IP);
        Thread thread = new Thread(new Runnable() {

            private Buffer fullBuffer = new Buffer(2048);

            @Override
            public void run() {
                while (true) {

                    final Buffer buffer = (Buffer) mInComeQueque.poll();
                    if (buffer != null) {

                        if (fullBuffer == null || fullBuffer.length() == 0) {
                            fullBuffer = buffer;
                        } else {
                            fullBuffer = fullBuffer.appendBuffer(buffer);
                        }

                        //mLogger.info("BEGIN PROCESS" + fullBuffer.toString());

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
                                        fullBuffer = null;
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
                    } else {
                        mLogger.info("coming queue is empty");
                        synchronized (RECV_LOCK) {
                            try {
                                RECV_LOCK.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            }
        });

        thread.start();

    }

    private void doConnect(final int port, final String server) {
        mLogger.info("do connect ....");

        mCanWrite = false;
        NetClient netClient = vertx.createNetClient();
        netClient.setReconnectAttempts(-1); //unlimited
        netClient.setReconnectInterval(1000);
        netClient.setTCPKeepAlive(true);
        netClient.setTCPNoDelay(true);
        netClient.setReuseAddress(true);
        netClient.setReceiveBufferSize(2048);

        netClient.connect(port, server, new Handler<AsyncResult<NetSocket>>() {
            @Override
            public void handle(AsyncResult<NetSocket> netSocketAsyncResult) {
                if (netSocketAsyncResult.succeeded()) {
                    mLogger.info("new connected ....");
                    mSocket = netSocketAsyncResult.result();
                    mSocket.exceptionHandler(new Handler<Throwable>() {
                        @Override
                        public void handle(Throwable event) {
                            mLogger.info("socket exception  ....", event);
                            mSocket.close();

                        }
                    });

                    mSocket.closeHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void aVoid) {
                            mLogger.info("socket closed closeHandler ....");
                            vertx.cancelTimer(mSendTimerId);
                            mSendTimerId = -1;
                            mCanWrite = false;
                            vertx.setTimer(1000, new Handler<Long>() {
                                @Override
                                public void handle(Long event) {
                                    doConnect(port, server);
                                }
                            });

                        }
                    });

                    mSocket.dataHandler(new Handler<Buffer>() {

                        //private Buffer leftBuffer;
                        public void handle(final Buffer buffer) { // <-- Note 'final' here
                            mSocket.pause();
                            mInComeQueque.add(buffer);
                            mSocket.resume();

                            mLastRecvTime = System.currentTimeMillis();
                            synchronized (RECV_LOCK) {
                                RECV_LOCK.notify();
                            }
                        }
                    });
                    mLogger.info("send Hello <" + CORE_CLIENT_ID + "><" + CORE_CLIENT_PASS + ">");
                    CoreMessage hello = new CoreMessage(Core.MsgType.HELLO_VALUE
                            , CORE_CLIENT_PASS
                            , CORE_CLIENT_ID
                            , EMPTY_BYTES);
                    writeData(hello.toBuffer());

                }
            }
        });

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
                    mLogger.info("CAN NOT PARSE MESSAGE, LOST DATA HERE, RECONNECT");
                    mSocket.close();
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

                /*if (_type.equalsIgnoreCase(Command.TRANSFER_WITH_LOCK)) {
                    result = (srpl.getErrorCode() == 0);
                }else if (_type.equalsIgnoreCase(Command.TRANSFER)) {
                    result = srpl.getErrorCode() == 0;
                }
                else if (_type.equalsIgnoreCase(Command.COMMIT)) {
                    result = srpl.getErrorCode() == 0;
                } else if (_type.equalsIgnoreCase(Command.ROLLBACK)) {
                    result = srpl.getErrorCode() == 103;
                } else if (_type.equalsIgnoreCase(Command.TRAN_WITH_VOUCHER_AND_POINT)) {
                    result = srpl.getErrorCode() == 0;
                } else if (_type.equalsIgnoreCase(Command.TOPUP_WITH_VOUHER)) {
                    result = srpl.getErrorCode() == 0;
                } else if (_type.equalsIgnoreCase(Command.MOMO_MONEY_TO_VOUCHER)) {
                    result = srpl.getErrorCode() == 0;
                } else if (_type.equalsIgnoreCase(Command.VOUCHER_TOPOINT)) {
                    result = srpl.getErrorCode() == 0;
                }*/
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

            /*for(int i = 0 ; i < srpl.getParamsCount(); i++){
                builder.addParams(Core.KeyValuePair.newBuilder()
                        .setKey(srpl.getParams(i).getKey())
                        .setValue( srpl.getParams(i).getKey()));
            }*/

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

    public void heartBeat() {
        if (mCanWrite && mSocket != null) {

            synchronized (OBJ_SEND_LOCK) {

                //mLogger.info("BEGIN SEND <" + System.currentTimeMillis() + ">");
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
                        e.printStackTrace();
                        joMessage = null;
                    }

                    if (joMessage != null && joMessage.body() != null) {

                        JsonObject jo = joMessage.body();
                        Request reqObj = new Request(jo);

                        byte[] msgBody = null;
                        int msgType = -1;

                        Common.BuildLog log = new Common.BuildLog(mLogger);
                        log.setPhoneNumber(reqObj.SENDER_NUM);
                        log.setTime(reqObj.TIME);
                        log.add("cmdType", reqObj.TYPE);
                        log.add("json request", jo);

                        String _type = reqObj.TYPE;

                        if (_type.equalsIgnoreCase(Command.VOTE)) {
                            String sender_num = reqObj.SENDER_NUM;
                            String sender_pin = reqObj.SENDER_PIN;
                            String recver_num = reqObj.RECVER_NUM;//so dien thoai hoac ma hoa don
                            long tran_amount = reqObj.TRAN_AMOUNT;//gia tri giao dich
                            String sms = "MOMOADJUST|" + INITIATOR_PIN + "|" + sender_num + "|" + recver_num + "|" + tran_amount + "|cdhh|voting";
                            log.add("sender", sender_num);
                            log.add("receiver", recver_num);
                            log.add("amount", tran_amount);
                            log.add("sms", sms);

                            msgType = Core.MsgType.GENERIC_REQUEST_VALUE;
                            msgBody = Core.GenericRquest.newBuilder()
                                    .setInitiator(INITIATOR_ACC)
                                    .setSms(sms)
                                    .build().toByteArray();

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
                        log.writeLog();
                        return;
                    } else {
                        mLogger.info("Khong lay duoc du lieu tu queue dang doi, sendTasks size " + sendTasks.size());
                    }
                } else if (System.currentTimeMillis() - mLastEchoTime >= 10 * 1000) {

                    mLastEchoTime = System.currentTimeMillis();
                    mLogger.info("send ECHO message at " + mLastEchoTime);
                    sendEcho();
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
        mSocket.write(buf);
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

    /*private void sendLoad(){
        for(int i = 0;i<100;i++){
            totalSend++;
            if(totalSend == 1){
                beginTime = System.currentTimeMillis();
            }
            else if(totalSend > 100){
                vertx.cancelTimer(loadId);
                mLogger.info("Total time " + (endTime - beginTime)/1000 + " seconds. Max wait : " + maxWaitTime + ". Min wait : " + minWaitTime);
                break;

            }

            Request reqObj = new Request();
            reqObj.TYPE = Command.VOTE;
            reqObj.SENDER_NUM = "" + (int)(910000001+(i %9));
            reqObj.RECVER_NUM = "" + (int)(910000011+(i %9));
            reqObj.TRAN_AMOUNT = 10;
            reqObj.SENDER_PIN = "000000";

            vertx.eventBus().send(AppConstant.CoreConnectorVerticle_ADDRESS, reqObj.toJsonObject(), new Handler<CoreMessage<Buffer>>() {


                @Override
                public void handle(CoreMessage<Buffer> result) {
                    endTime = System.currentTimeMillis();

                    mLogger.info("Total time " + (endTime - beginTime)/1000 + " seconds. Max wait : " + maxWaitTime + ". Min wait : " + minWaitTime);

                    totalRecv++;
                    MomoMessage momoMessage = MomoMessage.fromBuffer(result.body());
                    Core.StandardReply reply;
                    try {
                        reply = Core.StandardReply.parseFrom(momoMessage.cmdBody);
                    } catch (Exception ex) {
                        reply = null;
                    }

                    JsonObject joResult = new JsonObject();
                    if (reply == null) {
                        mLogger.info("vote : can not parse reveived buffer from core");
                    } else {
                        mLogger.info("vote done, result  : " + reply.getErrorCode());
                    }

                    //final long rplTranId = reply.getTid();
                    mLogger.info("total recv" + totalRecv + " time " + System.currentTimeMillis());
                }
            });

        }


    }
*/
}
