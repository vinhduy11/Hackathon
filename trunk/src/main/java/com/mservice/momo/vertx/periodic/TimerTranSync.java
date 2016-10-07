package com.mservice.momo.vertx.periodic;

import com.mservice.momo.data.TransDb;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;

/**
 * Created by concu on 4/18/14.
 */
public class TimerTranSync {
    private long timerID;
    private Logger logger;
    private Vertx vertx;
    private ArrayList arrayList;
    private int curPos=0;
    private MomoMessage msg;
    private NetSocket sock;
    private static int TIME_INTERVAL = 0;
    private static int BATCH_SIZE = 0;
    private static int LAST_SYNC_TIME = 0;
    private Common mCom;

    public static void loadCfg(int time_interval, int size){
        TIME_INTERVAL= time_interval;
        BATCH_SIZE =size;
    }

    public TimerTranSync(Vertx vertx
                            ,Logger logger
                            ,NetSocket sock
                            ,MomoMessage msg
                            ,ArrayList arrayList, JsonObject glbConfig){
        this.vertx = vertx;
        this.logger= logger;
        this.sock = sock;
        this.msg  = msg;
        this.arrayList = arrayList;
        mCom = new Common(vertx,logger, glbConfig);
    }

//    public void start(){
//        timerID = vertx.setPeriodic(TIME_INTERVAL, new Handler<Long>() {
//
//            public void handle(Long tId) {
//
//                MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
//                int k = 0;
//                for(int i =curPos; i< arrayList.size(); i ++){
//                    TransDb.TranObj to = (TransDb.TranObj)arrayList.get(curPos);
//
//                    builder.addTranList(MomoProto.TranHisV1. newBuilder()
//                                    .setTranId(to.tranId)
//                                    .setClientTime(to.clientTime)
//                                    .setAckTime(to.ackTime)
//                                    .setFinishTime(to.finishTime)
//                                    .setTranType(to.tranType)
//                                    .setIo(to.io)
//                                    .setCategory(to.category)
//                                    .setPartnerId(to.partnerId)
//                                    .setPartnerCode(to.parterCode)
//                                    .setPartnerName(to.partnerName)
//                                    .setPartnerRef(to.partnerRef)
//                                    .setBillId(to.billId)
//                                    .setAmount(to.amount)
//                                    .setComment(to.comment)
//                                    .setStatus(to.status)
//                                    .setError(to.error)
//                                    .setCommandInd(to.cmdId)
//                                    .setShare(to.share.toString())
//                    );
//                    k++;
//                    curPos++;
//                    if((k % BATCH_SIZE) == 0  || (curPos == arrayList.size()) ){
//                        Buffer buff = MomoMessage.buildBuffer(
//                                MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
//                                msg.cmdIndex,
//                                msg.cmdPhone,
//                                builder.setResult(true)
//                                       .build()
//                                       .toByteArray()
//                        );
//                        mCom.writeDataToSocket(sock, buff);
//                        //exit loop for
//                        break;
//                    }
//                }
//                //stop timer
//                if(curPos==arrayList.size()){
//                    vertx.cancelTimer(timerID);
//
//                    // Send TRAN_SYNC_FINISH
//                    MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.TRAN_SYNC_FINISH_VALUE
//                                                                ,msg.cmdIndex
//                                                                ,msg.cmdPhone
//                                                                ,"f".getBytes());
//                    mCom.writeDataToSocket(sock, momoMessage.toBuffer());
//
//                    arrayList.clear();
//                }
//            }
//        });
//    }

    public void start(){
        timerID = vertx.setPeriodic(TIME_INTERVAL, new Handler<Long>() {

            public void handle(Long tId) {

                MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
                int k = 0;
                JsonArray array = new JsonArray();
                JsonArray newArrayShare;
                String html_tmp = "";
                int flag = 0;

                for(int i =curPos; i< arrayList.size(); i ++){
                    newArrayShare = new JsonArray();
                    TranObj to = (TranObj)arrayList.get(curPos);

                    try{
                        if(Misc.isValidJsonArray(to.share.toString()) && to.share.toString().contains(Const.AppClient.Html))
                        {
                            array = new JsonArray(to.share.toString());
                            if(array.size() > 0)
                            {
                                for(Object o : array)
                                {
                                    String html = ((JsonObject)o) == null ? "" : ((JsonObject)o).getString(Const.AppClient.Html, "");
                                    if(!"".equalsIgnoreCase(html))
                                    {
                                        try
                                        {
                                            html_tmp = new String(DatatypeConverter.parseBase64Binary(((JsonObject) o).getString(Const.AppClient.Html, "")));
                                        }
                                        catch (Exception ex)
                                        {
                                            html_tmp = ((JsonObject) o).getString(Const.AppClient.Html, "");
                                        }
                                        newArrayShare.add(new JsonObject().putString(Const.AppClient.Html, html_tmp));
                                        flag = 1;
                                    }
                                    else
                                    {
                                        newArrayShare.add(o);
                                    }
                                }
                            }
                        }
                    }catch (Exception ex)
                    {
                        logger.info("flag when sync tran with app " + flag);
                        logger.info(ex.toString());
                        flag = 0;
                    }
                    logger.info("flag when sync tran with app " + flag);
                    if(flag == 0)
                    {
                        builder.addTranList(MomoProto.TranHisV1. newBuilder()
                                        .setTranId(to.tranId)
                                        .setClientTime(to.clientTime)
                                        .setAckTime(to.ackTime)
                                        .setFinishTime(to.finishTime)
                                        .setTranType(to.tranType)
                                        .setIo(to.io)
                                        .setCategory(to.category)
                                        .setPartnerId(to.partnerId)
                                        .setPartnerCode(to.parterCode)
                                        .setPartnerName(to.partnerName)
                                        .setPartnerRef(to.partnerRef)
                                        .setBillId(to.billId)
                                        .setAmount(to.amount)
                                        .setComment(to.comment)
                                        .setStatus(to.status)
                                        .setError(to.error)
                                        .setCommandInd(to.cmdId)
                                        .setShare(to.share.toString())
                        );
                    }
                    else{
                        builder.addTranList(MomoProto.TranHisV1. newBuilder()
                                        .setTranId(to.tranId)
                                        .setClientTime(to.clientTime)
                                        .setAckTime(to.ackTime)
                                        .setFinishTime(to.finishTime)
                                        .setTranType(to.tranType)
                                        .setIo(to.io)
                                        .setCategory(to.category)
                                        .setPartnerId(to.partnerId)
                                        .setPartnerCode(to.parterCode)
                                        .setPartnerName(to.partnerName)
                                        .setPartnerRef(to.partnerRef)
                                        .setBillId(to.billId)
                                        .setAmount(to.amount)
                                        .setComment(to.comment)
                                        .setStatus(to.status)
                                        .setError(to.error)
                                        .setCommandInd(to.cmdId)
                                        .setShare(newArrayShare.toString())
                        );
                    }
                    k++;
                    curPos++;
                    if((k % BATCH_SIZE) == 0  || (curPos == arrayList.size()) ){
                        Buffer buff = MomoMessage.buildBuffer(
                                MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.setResult(true)
                                        .build()
                                        .toByteArray()
                        );
                        mCom.writeDataToSocket(sock, buff);
                        //exit loop for
                        break;
                    }
                }
                //stop timer
                if(curPos==arrayList.size()){
                    vertx.cancelTimer(timerID);

                    // Send TRAN_SYNC_FINISH
                    MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.TRAN_SYNC_FINISH_VALUE
                            ,msg.cmdIndex
                            ,msg.cmdPhone
                            ,"f".getBytes());
                    mCom.writeDataToSocket(sock, momoMessage.toBuffer());

                    arrayList.clear();
                }
            }
        });
    }

//    public void start(){
////        timerID = vertx.setPeriodic(TIME_INTERVAL, new Handler<Long>() {
////
////            public void handle(Long tId) {
//
//               final MomoProto.TranHisSyncReply.Builder builder = MomoProto.TranHisSyncReply.newBuilder();
//               final AtomicInteger numberOfTran = new AtomicInteger(arrayList.size());
//                vertx.setPeriodic(TIME_INTERVAL, new Handler<Long>() {
//                    @Override
//                    public void handle(Long aLong) {
//                        //neu so luong het thi tra ve cho app luon
//                        JsonArray array = new JsonArray();
//                        JsonArray newArrayShare;
//                        String html_tmp = "";
//                        int flag = 0;
//                        if (numberOfTran.intValue() < 1) {
//                            vertx.cancelTimer(aLong);
//                            // Send TRAN_SYNC_FINISH
//                            MomoMessage momoMessage = new MomoMessage(MomoProto.MsgType.TRAN_SYNC_FINISH_VALUE
//                                    ,msg.cmdIndex
//                                    ,msg.cmdPhone
//                                    ,"f".getBytes());
//                            arrayList.clear();
//                            mCom.writeDataToSocket(sock, momoMessage.toBuffer());
//                            return;
//                        }
//                        else{
//                            int number = Math.min(numberOfTran.intValue(), BATCH_SIZE);
//                            int position = 0;
//                            for(int i = 0; i < number; i++)
//                            {
//                                position = numberOfTran.decrementAndGet();
//                                newArrayShare = new JsonArray();
//                                TransDb.TranObj to = (TransDb.TranObj)arrayList.get(position);
//
//                                if(Misc.isValidJsonArray(to.share.toString()) && to.share.toString().contains(Const.AppClient.Html))
//                                {
//                                    array = new JsonArray(to.share.toString());
//                                    if(array.size() > 0)
//                                    {
//                                        for(Object o : array)
//                                        {
//                                            String html = ((JsonObject)o).getString(Const.AppClient.Html, "");
//                                            if(!"".equalsIgnoreCase(html))
//                                            {
//                                                try
//                                                {
//                                                    html_tmp = new String(DatatypeConverter.parseBase64Binary(((JsonObject) o).getString(Const.AppClient.Html, "")));
//                                                }
//                                                catch (Exception ex)
//                                                {
//                                                    html_tmp = ((JsonObject) o).getString(Const.AppClient.Html, "");
//                                                }
//                                                newArrayShare.add(new JsonObject().putString(Const.AppClient.Html, html_tmp));
//                                                flag = 1;
//                                            }
//                                            else
//                                            {
//                                                newArrayShare.add(o);
//                                            }
//                                        }
//                                    }
//
//                                }
//                                if(flag == 0)
//                                {
//                                    builder.addTranList(MomoProto.TranHisV1. newBuilder()
//                                                    .setTranId(to.tranId)
//                                                    .setClientTime(to.clientTime)
//                                                    .setAckTime(to.ackTime)
//                                                    .setFinishTime(to.finishTime)
//                                                    .setTranType(to.tranType)
//                                                    .setIo(to.io)
//                                                    .setCategory(to.category)
//                                                    .setPartnerId(to.partnerId)
//                                                    .setPartnerCode(to.parterCode)
//                                                    .setPartnerName(to.partnerName)
//                                                    .setPartnerRef(to.partnerRef)
//                                                    .setBillId(to.billId)
//                                                    .setAmount(to.amount)
//                                                    .setComment(to.comment)
//                                                    .setStatus(to.status)
//                                                    .setError(to.error)
//                                                    .setCommandInd(to.cmdId)
//                                                    .setShare(to.share.toString())
//                                    );
//                                }
//                                else{
//                                    builder.addTranList(MomoProto.TranHisV1. newBuilder()
//                                                    .setTranId(to.tranId)
//                                                    .setClientTime(to.clientTime)
//                                                    .setAckTime(to.ackTime)
//                                                    .setFinishTime(to.finishTime)
//                                                    .setTranType(to.tranType)
//                                                    .setIo(to.io)
//                                                    .setCategory(to.category)
//                                                    .setPartnerId(to.partnerId)
//                                                    .setPartnerCode(to.parterCode)
//                                                    .setPartnerName(to.partnerName)
//                                                    .setPartnerRef(to.partnerRef)
//                                                    .setBillId(to.billId)
//                                                    .setAmount(to.amount)
//                                                    .setComment(to.comment)
//                                                    .setStatus(to.status)
//                                                    .setError(to.error)
//                                                    .setCommandInd(to.cmdId)
//                                                    .setShare(newArrayShare.toString())
//                                    );
//                                }
//                            }
//                            Buffer buff = MomoMessage.buildBuffer(
//                                    MomoProto.MsgType.TRAN_SYNC_REPLY_VALUE,
//                                    msg.cmdIndex,
//                                    msg.cmdPhone,
//                                    builder.setResult(true)
//                                            .build()
//                                            .toByteArray());
//                            mCom.writeDataToSocket(sock, buff);
//                        }
//                    }
//                });
////            }
////        });
//    }
}
