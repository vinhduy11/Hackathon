package com.mservice.momo.vertx;

import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.DeviceInfoDb;
import com.mservice.momo.data.NotificationDb;
import com.mservice.momo.data.PhonesDb;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by locnguyen on 18/08/2014.
 */
public class ToolsVerticle extends Verticle {
    private EventBus eventBus;
    private Logger logger;
    private ArrayList<Integer> listPhones = new ArrayList<>();
    PhonesDb phonesDb;
    DeviceInfoDb deviceInfoDb;


    ArrayList<String> notiDocList = new ArrayList<>();
    private int curPosNoti = 0;
    NotificationDb notificationDb;

    @Override
    public void start() {

        eventBus = vertx.eventBus();
        logger = container.logger();
        notificationDb = DBFactory.createNotiDb(vertx, logger, container.config());

        /*phonesDb = new PhonesDb(vertx.eventBus(),logger);

        deviceInfoDb = new DeviceInfoDb(vertx.eventBus());

        phonesDb.getAllPhone(new Handler<ArrayList<Integer>>() {
            @Override
            public void handle(ArrayList<Integer> integers) {

                if(integers!=null && integers.size() >0){
                    listPhones = integers;
                    update(0);
                }
            }
        });*/

        //for notification
        /*readData("/home/concu/Desktop/noti_20140904_raw");

        vertx.setPeriodic(350,new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                if(curPosNoti == notiDocList.size()){
                    logger.info("COMPLETE TASK AT " + CoreCommon.dateVNFormatWithTime(System.currentTimeMillis()));
                    vertx.cancelTimer(aLong);
                    return;
                }

                final String collectName = notiDocList.get(curPosNoti);
                final CoreCommon.BuildLog log = new CoreCommon.BuildLog(logger);
                log.setPhoneNumber(collectName);
                log.add("noti at", curPosNoti + "/" + notiDocList.size());

                final long start = System.currentTimeMillis();
                notificationDb.getTime(collectName,100,1,new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {

                        log.add("data will be removed from", aLong);

                        if(aLong >0){
                            notificationDb.removeOldRecs(collectName,aLong,new Handler<Integer>() {
                                @Override
                                public void handle(Integer integer) {
                                    long end = System.currentTimeMillis();
                                    long diff = end -start;
                                    log.add("records removed", integer);
                                    log.add("excute total millisec", diff);
                                    log.writeLog();
                                }
                            });
                        }
                        long end = System.currentTimeMillis();
                        long diff = end - start;
                        log.add("excute total millisec", diff);
                        log.writeLog();
                        curPosNoti ++;
                    }
                });
            }
        });*/
    }

    private void readData(String fullPath){
    try{
        BufferedReader reader = new BufferedReader(new FileReader(fullPath));
        String line = null;
        while ((line = reader.readLine()) != null) {
            notiDocList.add(line);
        }

        reader.close();

        }catch (IOException ioe){
                logger.info("readData got exception " + ioe.getMessage());
        }
    }

    public void update(final int position){

        vertx.setTimer(200, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                if (position < listPhones.size()){
                    // Todo: update token phoneDB
//                    logger.info(position + "");
                    deviceInfoDb.getOneLastTime(listPhones.get(position), new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject jsonObject) {
                            logger.info(listPhones.get(position) + " : " + position + "/" + listPhones.size());

                            if(jsonObject != null){

                                logger.info(jsonObject);

                                phonesDb.updatePartialNoReturnObj(listPhones.get(position),jsonObject,new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean obj) {

                                        update(position + 1);

                                    }
                                });
                            }else {

                                update(position + 1);
                            }
                        }
                    });
                }
            }
        });
    }
}
