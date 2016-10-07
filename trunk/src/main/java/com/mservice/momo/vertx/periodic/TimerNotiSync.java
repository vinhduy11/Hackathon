package com.mservice.momo.vertx.periodic;

import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.ArrayList;

/**
 * Created by concu on 8/28/14.
 */
public class TimerNotiSync {
    private long timerID;
    private Logger logger;
    private Vertx vertx;
    private ArrayList arrayList;
    private int curPos=0;
    private MomoMessage msg;
    private NetSocket sock;
    private int TIME_INTERVAL = 300;
    private int BATCH_SIZE = 5;
    private Common mCom;

    public TimerNotiSync(Vertx vertx
            ,Logger logger
            ,NetSocket sock
            ,MomoMessage msg
            ,ArrayList arrayList, JsonObject glbConfig){

        this.vertx = vertx;
        this.logger= logger;
        this.sock = sock;
        this.msg  = msg;
        this.arrayList = arrayList;
        this.mCom = new Common(vertx,logger, glbConfig);
    }

    public void start(){
        timerID = vertx.setPeriodic(TIME_INTERVAL, new Handler<Long>() {

            public void handle(Long tId) {


                MomoProto.NotificationSyncReply.Builder builder = MomoProto.NotificationSyncReply.newBuilder();

                int k = 0;
                for(int i =curPos; i< arrayList.size(); i ++){

                    Notification noti = (Notification)arrayList.get(curPos);
                    builder.addNotifications(noti.toMomoProto());

                    k++;
                    curPos++;

                    if((k % BATCH_SIZE) == 0  || (curPos == arrayList.size()) ){

                        //todo nam edit tren cai nay
                        Buffer buff = MomoMessage.buildBuffer(
                                MomoProto.MsgType.NOTIFICATION_SYNC_REPLY_VALUE,
                                msg.cmdIndex,
                                msg.cmdPhone,
                                builder.build()
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

                    // Send finish message
                    Buffer buf = MomoMessage.buildBuffer(
                            MomoProto.MsgType.NOTIFICATION_SYNC_FINISH_VALUE,
                            msg.cmdIndex,
                            msg.cmdPhone,
                            "f".getBytes()
                    );
                    mCom.writeDataToSocket(sock, buf);

                    arrayList.clear();
                }

            }
        });
    }
}
