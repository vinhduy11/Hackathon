package com.mservice.momo.vertx.periodic;

import com.mservice.momo.data.web.ServiceDb;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetSocket;

import java.util.ArrayList;

/**
 * Created by concu on 4/18/14.
 */
public class TimerService {
    private long timerID;
    private Logger logger;
    private Vertx vertx;
    private ArrayList arrayList;
    private int curPos = 0;
    private MomoMessage msg;
    private NetSocket sock;
    private int TIME_INTERVAL = 200;
    private int BATCH_SIZE = 4;
    private Common mCom;

    public TimerService(Vertx vertx, Logger logger
            , NetSocket sock
            , MomoMessage msg
            , ArrayList arrayList, JsonObject glbConfig) {
        this.vertx = vertx;
        this.logger = logger;
        this.sock = sock;
        this.msg = msg;
        this.arrayList = arrayList;
        mCom = new Common(vertx, logger, glbConfig);
    }

    public void start() {
        timerID = vertx.setPeriodic(TIME_INTERVAL, new Handler<Long>() {

            public void handle(Long tId) {

                MomoProto.ServiceReply.Builder builder = MomoProto.ServiceReply.newBuilder();

                int k = 0;
                for (int i = curPos; i < arrayList.size(); i++) {

                    ServiceDb.Obj o = (ServiceDb.Obj) arrayList.get(curPos);

                    builder.addServiceList(MomoProto.ServiceItem.newBuilder()
                                    .setServiceType(o.serviceType)
                                    .setPartnerCode(o.partnerCode)
                                    .setServiceId(o.serviceID)
                                    .setServiceName(o.serviceName)
                                    .setPartnerSite(o.partnerSite)
                                    .setIconUrl(o.iconUrl)
                                    .setStatus(o.status) // 1: on ; 0 off
                                    .setTextPopup(o.textPopup)
                                    .setHasCheckDebit(o.hasCheckDebit)
                                    .setTitleDialog(o.titleDialog)
                                    .setLastUpdate(o.lastUpdateTime)
                                    .setBillidType(o.billType)
                                    .setIsPromo(o.IsPromo)
                                    .setStar(o.star)
                                    .setTotalForm(o.totalForm)
                                    .setCategoryName("")
                                    .setCategoryId(o.cateId)
                                    .setSecretKey(o.billerID)
                                    .setWebPaymentUrl(o.webPaymentUrl)
                                    .setOrder(o.order)
                                    .setBillpayExtra(o.billPay)
                    );

                    k++;
                    curPos++;
                    if ((k % BATCH_SIZE) == 0 || (curPos == arrayList.size())) {

                        Buffer buff = MomoMessage.buildBuffer(MomoProto.MsgType.GET_SERVICE_REPLY_VALUE
                                , msg.cmdIndex
                                , msg.cmdPhone
                                , builder.build().toByteArray());
                        mCom.writeDataToSocket(sock, buff);

                        break;
                    }
                }
                //stop timer
                if (curPos == arrayList.size()) {
                    vertx.cancelTimer(timerID);

                    // no need to send finish
                    arrayList.clear();
                }
            }
        });
    }
}
