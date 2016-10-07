package com.mservice.momo.vertx.periodic;

import com.mservice.momo.data.AgentsDb;
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


public class TimerLocSync {
    private static int TIME_INTERVAL = 0;
    private static int BATCH_SIZE = 0;
    final private JsonObject joRound = new JsonObject();
    private long timerID;
    private Logger logger;
    private Vertx vertx;
    private ArrayList arrayList;
    private int curPos = 0;
    private MomoMessage msg;
    private NetSocket sock;
    private Common mCom;

    public TimerLocSync(Vertx vertx
            , Logger logger
            , NetSocket sock
            , MomoMessage msg
            , ArrayList arrayList, JsonObject glbConfig) {
        this.vertx = vertx;
        this.logger = logger;
        this.sock = sock;
        this.msg = msg;
        this.arrayList = arrayList;
        this.mCom = new Common(vertx, logger, glbConfig);
        joRound.putNumber("r", 1);
    }

    public static void loadCfg(int time_interval, int size) {
        TIME_INTERVAL = time_interval;
        BATCH_SIZE = size;
    }

    public void start() {

        timerID = vertx.setPeriodic(TIME_INTERVAL, new Handler<Long>() {

            public void handle(Long tId) {
                logger.debug("LocSync start at time " + System.currentTimeMillis());
                MomoProto.SyncStoreLocationReply.Builder builder = MomoProto.SyncStoreLocationReply.newBuilder();
                int k = 0;
                for (int i = curPos; i < arrayList.size(); i++) {
                    AgentsDb.StoreInfo si = (AgentsDb.StoreInfo) arrayList.get(curPos);

                    k++;
                    curPos++;
                    // Dong bo nhung diem giao dich co lat vs lng bang 0.
                    //khong lay diem giao dich sai tra ve cho client
//                    if(si.loc.Lng == 0 && si.loc.Lat ==0){
//                        continue;
//                    }

                    builder.addStores(MomoProto.StoreInfo.newBuilder()
                                    .setOwner(si.name)
                                    .setPhone(si.phone)
                                    .setName(si.storeName)
                                    .setLat(si.loc.Lat)
                                    .setLng(si.loc.Lng)
                                    .setAdd(si.address)
                                    .setStreet(si.street)
                                    .setWard(si.ward)
                                    .setDid(si.districtId)
                                    .setCid(si.cityId)
                                    .setAid(si.areaId)
                                    .setLastUpdateTime(si.last_update_time)
                                    .setDeleted(si.deleted)
                                    .setRowCoreId(si.rowCoreId)
                                    .setMomoNumber(si.momoNumber)
                    );

                    if ((k % BATCH_SIZE) == 0 || (curPos == arrayList.size())) {

                        int r = joRound.getInteger("r");

                        r++;
                        joRound.putNumber("r", r);
                        Buffer buff = MomoMessage.buildBuffer(MomoProto.MsgType.STORE_LOCATION_SYNC_REPLY_VALUE
                                , msg.cmdIndex
                                , msg.cmdPhone
                                , builder.setResult(true).build()
                                .toByteArray());

                        mCom.writeDataToSocket(sock, buff);

                        //exit for each timer called
                        break;
                    }
                }
                //stop timer
                if (curPos == arrayList.size()) {
                    vertx.cancelTimer(timerID);
                    arrayList.clear();
                }
            }
        });
    }
}
