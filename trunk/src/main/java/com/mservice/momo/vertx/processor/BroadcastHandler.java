package com.mservice.momo.vertx.processor;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.entry.ServerVerticle;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.data.SockData;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

/**
 * Created by User on 3/17/14.
 */
public class BroadcastHandler implements Handler<Message> {
    public static boolean IN_PROMOTION;
    public static long NONAME_MAX_TRAN_VALUE_PER_DAY;

    NetSocket mSocket;
    Logger mLogger;
    PhonesDb mPhoneDb;
    int mNumber;
    SockData mData;
    Vertx mVertx;
    Boolean mIsClosed = false;
    private Common mCommon ;

    public int getmNumber() {
        return mNumber;
    }

    public BroadcastHandler(int number, NetSocket socket, PhonesDb phonesDb, Logger logger, SockData data, Vertx vertx, JsonObject glbConfig){
        mNumber = number;
        mSocket = socket;
        mLogger = logger;
        mPhoneDb = phonesDb;
        mData = data;
        mVertx = vertx;
        mIsClosed = false;
        mCommon = new Common(vertx,logger, glbConfig);
    }

    public static class LocalMsgHelper {
        JsonObject object;
        public LocalMsgHelper(JsonObject obj){
            object = obj;
        }

        public LocalMsgHelper(){
            object = new JsonObject();
        }

        public LocalMsgHelper setType(int type){
            object.putNumber("type", type);
            return this;
        }

        public LocalMsgHelper setSenderNumber(int sender){
            object.putNumber("sender_number", sender);
            return this;
        }

        public LocalMsgHelper setSenderName(String name){
            object.putString("sender_name", name);
            return this;
        }

        public LocalMsgHelper setReceivers(String receivers){
            object.putString("receivers", receivers);
            return this;
        }

        public LocalMsgHelper setAmount(long amount){
            object.putNumber("amount", amount);
            return this;
        }

        public LocalMsgHelper setNewPhone(int newPhone){
            object.putNumber("newphone", newPhone);
            return this;
        }

        public LocalMsgHelper setRequest(byte[] request){
            object.putBinary("request", request);
            return this;
        }

        public LocalMsgHelper setContent(String content){
            object.putString("content", content);
            return this;
        }


        public int getType(){
            return object.getInteger("type",0);
        }

        public int getSenderNumber(){
            return object.getInteger("sender_number", 0);
        }

        public String getSenderName(){
            return object.getString("sender_name", "");
        }

        public String getReceivers(){
            return object.getString("receivers", "");
        }

        public String getContent(){
            return object.getString("content", "");
        }

        public byte[] getRequest(){
            return object.getBinary("request", null);
        }

        public long getAmount(){
            return object.getLong("amount", 0);
        }

        public int getNewPhone(){
            return object.getInteger("newphone", 0);
        }

        public LocalMsgHelper setExtra(JsonObject extra){
            object.putObject("extra",extra);
            return this;
        }

        public JsonObject getExtra(){
            return  object.getObject("extra");
        }


        public JsonObject getJsonObject(){
            return object;
        }
    }

    @Override
    public void handle(Message message) {
        JsonObject obj = (JsonObject) message.body();
        final LocalMsgHelper helper = new LocalMsgHelper(obj);

        mLogger.debug("Broadcast handler <" + mNumber + "> recieved command type : " + helper.getType());
        switch (helper.getType()){
            case SoapProto.Broadcast.MsgType.MONEY_REQ_VALUE:
                mLogger.debug("BroadcastHandler process MONEY_REQ_VALUE " + mNumber);
                processMoneyReq(message,helper);
                break;
            case SoapProto.Broadcast.MsgType.KILL_PREV_VALUE:
                mLogger.debug("BroadcastHandler process KILL_PREV_VALUE " + mNumber);
                processKillPrev(helper);
                break;
            case SoapProto.Broadcast.MsgType.MONEY_RECV_VALUE:
                mLogger.debug("BroadcastHandler process UPDATE_TRAN_VALUE " + mNumber);
                message.reply(true);
                processMoneyReceive(helper);
                break;

            case SoapProto.Broadcast.MsgType.NEW_USER_VALUE:
                JsonObject jsonExtra = helper.getExtra();
                boolean isStore = jsonExtra.getBoolean(StringConstUtil.IS_STORE_APP, false);
                if(isStore) {
                    mLogger.debug("BroadcastHandler process NEW_USER_VALUE_FOR_STORE " + mNumber);
                    break;
                }
                else{
                    mLogger.debug("BroadcastHandler process NEW_USER_VALUE " + mNumber);
                    processUpdateNewUser(helper);
                    break;
                }
            case SoapProto.Broadcast.MsgType.TRANS_OUSIDE_VALUE:
                mLogger.debug("BroadcastHandler process TRANS_OUSIDE_VALUE " + mNumber);
                processOutSideTranSync(helper);
                break;

            case SoapProto.Broadcast.MsgType.NOTIFICATION_VALUE:
                mLogger.debug("BroadcastHandler process NOTIFICATION_VALUE " + mNumber);
                processNotification(message, helper);
                break;

            case SoapProto.Broadcast.MsgType.CHECK_PREV_VALUE:
                mLogger.debug("BroadcastHandler process CHECK_PREV_VALUE " + mNumber);
                processCheckPrev(helper);
                break;

            case SoapProto.Broadcast.MsgType.PREV_RETURN_VALUE:
                mLogger.debug("BroadcastHandler process PREV_RETURN_VALUE " + mNumber);
                processPrevReturn(helper);
                break;

            case SoapProto.Broadcast.MsgType.FORCE_UPDATE_AGENT_INFO_VALUE:
                processForceUpdateAgentInfo(helper);
                break;

            case SoapProto.Broadcast.MsgType.GET_TOKEN_VALUE:
                processGetToken(message);
                break;

            default:
                mLogger.info("Broadcast handler not support for the command SoapProto.Broadcast.MsgType, " + helper.getType());
                break;
        }
    }

    private void processCheckPrev(LocalMsgHelper helper) {
        //check if this come from new connection

        mLogger.debug("processCheckPrev socketId: " + mSocket.writeHandlerID() + " receivers:" + helper.getReceivers());
        if(mSocket != null &&  !mSocket.writeHandlerID().equalsIgnoreCase(helper.getReceivers())){
            mIsClosed = true;
            
            if(mData != null && mData.mPendingNotification != null && mData.mPendingNotification.size() > 0){
                JsonArray pendingList = new JsonArray();
                for(JsonObject notif : mData.mPendingNotification.values()){
                    pendingList.add(notif);
                }
                mData.mPendingNotification.clear();

                JsonObject result = new JsonObject();
                result.putArray("pendingNotifList",pendingList);
                //result.putString("otp", mData.otp);
                //result.putNumber("otpTime", mData.otpTime);
                helper.setExtra(result);
                helper.setType(SoapProto.Broadcast.MsgType.PREV_RETURN_VALUE);
                mVertx.eventBus().publish(Misc.getNumberBus(mNumber),helper.getJsonObject());
            }
            unregister();
            mLogger.info(mNumber + "We got a new connection for this number , close the prev"  );
            mSocket.close();
        }
    }


    private void processPrevReturn(LocalMsgHelper helper) {
        //check if this is our return
        if(mSocket != null &&  mSocket.writeHandlerID().equalsIgnoreCase(helper.getReceivers())){
            mLogger.error("Get data from previous connection" );
            JsonObject result = helper.getExtra();
            JsonArray pendingNotifications = result.getArray("pendingNotifList");
            if(pendingNotifications != null && pendingNotifications.size() > 0){
                for (int i = 0; i < pendingNotifications.size(); i++) {
                    Notification noti = Notification.parse((JsonObject)pendingNotifications.get(i));
                    sendNotification(noti);
                }
            }
        }

    }

    private void processNotification(Message request,final LocalMsgHelper helper) {

        if(!mIsClosed){
            mLogger.debug("<" + mNumber + "> GOT NEW NOTIFICATION, ECHO IT BACK AND SEND TO CLIENT");

            final JsonObject extraObj = helper.getExtra();
            final Notification notification = Notification.parse(extraObj);
            if (notification == null) {
                mLogger.error("Receives a broken notification message: " + helper.getJsonObject());
                return;
            }
            sendNotification(notification);
        }
        else {
            mLogger.debug("<" + mNumber + "> GOT NEW NOTIFICATION, WE IN CLOSING PHASE, IGNORE IT");
        }
    }

    private void sendNotification(final Notification notification){
        mData.addPendingPacket(notification.id, notification.toJsonObject());
        MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.NOTIFICATION_NEW_VALUE
                , 0
                , mData.getNumber()
                ,MomoProto.NotificationNew.newBuilder()
                .setNotification(notification.toMomoProto()).build().toByteArray()
        );
        mCommon.writeDataToSocket(mSocket, momoMessage.toBuffer());

        mVertx.setTimer(30000, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                if (mData.mPendingNotification.get(notification.id) != null) {
                    if(mSocket != null){
                        mLogger.info("Can't send the notification - this is a dead socket - close this!");
                        mSocket.close();
                    }
                    else {
                        mLogger.info("Can't send the notification - socket is null");
                    }
                    return;
                }
                else {
                    mLogger.info("Notification has been sent");
                }
            }
        });
    }

    private void processMoneyReq(final Message message
                                ,final LocalMsgHelper helper){

        mLogger.debug("send MONEY_REQUEST to " + mNumber);
        Buffer buf = MomoMessage.buildBuffer(
                MomoProto.MsgType.MONEY_REQUEST_VALUE,
                System.currentTimeMillis(),
                0,
                helper.getRequest()
        );
        mCommon.writeDataToSocket(mSocket, buf);
        message.reply("OK");


        //todo thuc hien ban cloud message cho thang nay ; Loc lam
//        BroadcastHandler.LocalMsgHelper extra = new BroadcastHandler.LocalMsgHelper();
//        extra.setAmount(helper.getAmount());
//        extra.setSenderNumber(helper.getSenderNumber());
//        extra.setSenderName(helper.getSenderName());
//        extra.setContent(helper.getContent());
//
//        boolean isCut = false;
//        while (extra.getJsonObject().toString().getBytes().length > 154) {
//            isCut = true;
//            extra.setContent(extra.getContent().substring(0, extra.getContent().length() - 1));
//        }
//        if (isCut) {
//            extra.setContent(extra.getContent().substring(0, extra.getContent().length() - 3) + "...");
//        }
//
//        Notification noti = new Notification();
//        noti.receiverNumber = mNumber;
//        noti.caption = "Bạn nhận được 1 yêu cầu vây tiền từ bạn bè";
//        noti.body = "Bạn nhận được 1 yêu cầu vây tiền từ số 0" + extra.getSenderNumber() + ".";
//        noti.bodyIOS = "Ban nhan duoc 1 yeu cau vay tien tu so 0" + extra.getSenderNumber() + ".";
//        noti.type = MomoProto.NotificationType.NOTI_MONEY_REQUEST_VALUE;
//        noti.priority = 2;
//        noti.extra = extra.getJsonObject().toString();
//        noti.time = System.currentTimeMillis();
//
//        cloudMessageQueue.push(noti,null);

    }

    private void processUpdateNewUser(final LocalMsgHelper helper){

        int newphone = helper.getNewPhone();
        if(newphone != mNumber){
            mLogger.debug("send NEW_USER to " + mNumber);
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.NEW_USER_VALUE,
                    System.currentTimeMillis(),
                    0,
                    MomoProto.NewUser.newBuilder()
                        .setNewNumber(newphone)
                        .setTime(System.currentTimeMillis())
                        .build().toByteArray()
            );
            mCommon.writeDataToSocket(mSocket, buf);
        }
    }

    public void processMoneyReceive(final LocalMsgHelper helper){

        if(mSocket != null && mData != null){

            Common.BuildLog log = null;
            if(mLogger != null){
                log = new Common.BuildLog(mLogger);
                log.setPhoneNumber("0" + mNumber);
                log.add("function","processMoneyReceive");
                log.add("send tran for reciever", mNumber);
                log.add("function","sendCurrentAgentInfo");
            }
            
            //nhan tien tu mot nguoi khac gui den
            //cap nhat so du
            final JsonObject rcv = helper.getExtra();

            mCommon.sendCurrentAgentInfo(mVertx,mSocket,0,mNumber,mData);

            byte[] bytes = MomoProto.TranHisV1.newBuilder()
                    .setCommandInd(rcv.getLong(colName.TranDBCols.COMMAND_INDEX,0))
                    .setTranId(rcv.getLong(colName.TranDBCols.TRAN_ID,-1))
                    .setClientTime(rcv.getLong(colName.TranDBCols.CLIENT_TIME,0))
                    .setAckTime(rcv.getLong(colName.TranDBCols.ACK_TIME,0))
                    .setFinishTime(rcv.getLong(colName.TranDBCols.FINISH_TIME,0))
                    .setTranType(rcv.getInteger(colName.TranDBCols.TRAN_TYPE,0))
                    .setIo(rcv.getInteger(colName.TranDBCols.IO,0))
                    .setCategory(rcv.getInteger(colName.TranDBCols.CATEGORY,-1))
                    .setPartnerId(rcv.getString(colName.TranDBCols.PARTNER_ID,""))
                    .setPartnerCode(rcv.getString(colName.TranDBCols.PARTNER_CODE,""))
                    .setPartnerName(rcv.getString(colName.TranDBCols.PARTNER_NAME,""))
                    .setPartnerRef(rcv.getString(colName.TranDBCols.PARTNER_REF,""))
                    .setBillId(rcv.getString(colName.TranDBCols.BILL_ID,""))
                    .setAmount(rcv.getLong(colName.TranDBCols.AMOUNT,0))
                    .setComment(rcv.getString(colName.TranDBCols.COMMENT, ""))
                    .setStatus(rcv.getInteger(colName.TranDBCols.STATUS,4))
                    .setError(rcv.getInteger(colName.TranDBCols.ERROR,0))
                    .setSourceFrom(rcv.getInteger(colName.TranDBCols.ERROR
                            ,MomoProto.TranHisV1.SourceFrom.MOMO_VALUE))
                    .setShare(rcv.getArray(colName.TranDBCols.SHARE, new JsonArray()).toString())
                    .build().toByteArray();

            Buffer buffer =  MomoMessage.buildBuffer(
                    MomoProto.MsgType.TRAN_RECEIVER_VALUE,
                    System.currentTimeMillis(),
                    Integer.valueOf(helper.getReceivers()),
                    bytes
            );

            mCommon.writeDataToSocket(mSocket, buffer);
            
            if(log != null){

                log.add("cmdIndex", rcv.getLong(colName.TranDBCols.COMMAND_INDEX,0));
                log.add("tranid",rcv.getLong(colName.TranDBCols.TRAN_ID,-1));
                log.add("clientitme",rcv.getLong(colName.TranDBCols.CLIENT_TIME,0));
                log.add("acktime",rcv.getLong(colName.TranDBCols.ACK_TIME,0));
                log.add("finishtime",rcv.getLong(colName.TranDBCols.FINISH_TIME,0));
                log.add("trantype",rcv.getInteger(colName.TranDBCols.TRAN_TYPE, 0));
                log.add("io",rcv.getInteger(colName.TranDBCols.IO,0));
                log.add("partnerid",rcv.getString(colName.TranDBCols.PARTNER_ID, ""));
                log.add("partnercode",rcv.getString(colName.TranDBCols.PARTNER_CODE,""));
                log.add("partnername",rcv.getString(colName.TranDBCols.PARTNER_NAME,""));
                log.add("partnerref",rcv.getString(colName.TranDBCols.PARTNER_REF,""));
                log.add("billid",rcv.getString(colName.TranDBCols.BILL_ID,""));
                log.add("amount",rcv.getLong(colName.TranDBCols.AMOUNT, 0));
                log.add("comment",rcv.getString(colName.TranDBCols.COMMENT, ""));
                log.add("status",rcv.getInteger(colName.TranDBCols.STATUS, 4));
                log.add("error",rcv.getInteger(colName.TranDBCols.ERROR,0));
                log.add("source from",rcv.getInteger(colName.TranDBCols.ERROR,MomoProto.TranHisV1.SourceFrom.MOMO_VALUE));
                log.add("share", rcv.getArray(colName.TranDBCols.SHARE, new JsonArray()).toString());
                log.add("function","writeDataToSocket");
                log.add("buffer", new String(buffer.getBytes()));

                log.writeLog();
            }
        }
    }

    private void processKillPrev(final LocalMsgHelper helper ){

        if(!mSocket.writeHandlerID().equalsIgnoreCase(helper.getReceivers())){
            Buffer buf = MomoMessage.buildBuffer(
                    MomoProto.MsgType.LOG_OUT_REPLY_VALUE,
                    0,
                    mNumber,
                    MomoProto.StandardReply.newBuilder()
                            .setResult(true)
                            .setRcode(0)
                            .build().toByteArray()
            );

            mCommon.writeDataToSocketAndClose(mSocket, buf);
        }
    }

    private void processOutSideTranSync(LocalMsgHelper helper){

        mCommon.sendCurrentAgentInfo(mVertx, mSocket,0,mNumber,mData);

        TranObj tranObj = new TranObj(helper.getExtra());
        MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();

        builder.addTranList(MomoProto.TranHisV1.newBuilder()
                        .setTranId(tranObj.tranId)
                        .setClientTime(tranObj.clientTime)
                        .setAckTime(tranObj.ackTime)
                        .setFinishTime(tranObj.finishTime)
                        .setTranType(tranObj.tranType)
                        .setIo(tranObj.io)
                        .setCategory(tranObj.category)
                        .setPartnerId(tranObj.partnerId)
                        .setPartnerCode(tranObj.parterCode)
                        .setPartnerName(tranObj.partnerName)
                        .setPartnerRef(tranObj.partnerRef)
                        .setBillId(tranObj.billId)
                        .setAmount(tranObj.amount)
                        .setComment(tranObj.comment)
                        .setStatus(tranObj.status)
                        .setError(tranObj.error)
                        .setCommandInd(tranObj.cmdId)
                        .setShare(tranObj.share.toString())
        );

        Buffer buff = MomoMessage.buildBuffer(
                MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                System.currentTimeMillis(),
                tranObj.owner_number,
                builder.setResult(true)
                        .build()
                        .toByteArray()
        );

        mCommon.writeDataToSocket(mSocket, buff);
    }

    private void processForceUpdateAgentInfo(LocalMsgHelper helper){
        PhonesDb.Obj phoneObj = null;
        if(helper != null)
        {
            phoneObj = new PhonesDb.Obj(helper.getJsonObject());
            if(phoneObj.number == 0)
            {
                phoneObj.number = helper.getSenderNumber();
            }
        }
        else {
            phoneObj = new PhonesDb.Obj();
        }

        if(mNumber != phoneObj.number) return;

        if(mData != null && phoneObj.number > 0 && !"".equalsIgnoreCase(phoneObj.name)){
            mData.setPhoneObj(phoneObj,mLogger,"");
        }
        if(mSocket != null){
            mCommon.sendCurrentAgentInfo(mVertx, mSocket,0,mNumber,mData);
        }
    }

    private void processGetToken(Message message){

        JsonObject jo = null;
        JsonObject json = (JsonObject)message.body();

        if(mData!=null){

            jo = new JsonObject();
            jo.putString("token",mData.pushToken);
            jo.putString("os",mData.os);
            message.reply(jo);
        }

        message.reply(jo);
    }

    public static void sendOutSideTransSync(Vertx _vertx, TranObj tranObj){
        LocalMsgHelper helper = new LocalMsgHelper();
        helper.setType(SoapProto.Broadcast.MsgType.TRANS_OUSIDE_VALUE);
        helper.setReceivers("" + tranObj.owner_number);
        helper.setSenderNumber(tranObj.owner_number);
        helper.setSenderName(tranObj.owner_name);
        helper.setExtra(tranObj.getJSON());

        _vertx.eventBus().publish(Misc.getNumberBus(tranObj.owner_number), helper.getJsonObject());
    }

    public void unregister(){
        //mLogger.debug("unregister broadcast channel for number " + mNumber + " - id " + mSocket.writeHandlerID());
        mVertx.eventBus().unregisterHandler(ServerVerticle.MOMO_BROADCAST, this);
        mVertx.eventBus().unregisterHandler(Misc.getNumberBus(mNumber), this);
    }
}
