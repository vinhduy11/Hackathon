package com.mservice.momo.vertx.event;

import com.mservice.momo.data.CDHHPayBack;
import com.mservice.momo.data.DBFactory;
import com.mservice.momo.data.TransDb;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

/**
 * Created by concu on 5/31/15.
 */
public class TestEventVerticle extends Verticle {

    /*TODO ADD TO MOMO JSON : "TestEventVerticle" : "true"*/
    CDHHPayBack cdhhPayBack = null;
    private Logger logger;
    private TransDb transDb;
    private JsonObject glbCfg;

    final String SERVICE_ID_STR     = "SERVICE_ID_STR";
    final String ROOT_NUMBER_INT    = "ROOT_NUMBER_INT";
    final String ROOT_PASS_STR      = "ROOT_PASS_STR";
    final String MOMO_NUMBER_INT    = "MOMO_NUMBER_INT";
    final String MOMO_PASS_STR      = "MOMO_PASS_STR";
    final String EXPT_NUMBER_INT    = "EXPT_NUMBER_INT";
    final String EXPT_NAME_STR      = "EXPT_NAME_STR";
    final String VOTE_CODE_INT      = "VOTE_CODE_INT";
    final String SMS_VOL_INT        = "SMS_VOL_INT";
    final String SMS_VAL_INT        = "SMS_VAL_INT";


    public void start() {
        glbCfg = getContainer().config();
        logger = getContainer().logger();
        EventBus eb = vertx.eventBus();
        cdhhPayBack = new CDHHPayBack(vertx.eventBus(),logger);
        transDb = DBFactory.createTranDb(vertx, vertx.eventBus(), logger, container.config());

        final TestProcessEvent processEvent = new TestProcessEvent(glbCfg,vertx,logger);

        //get some config
        NetServer server = vertx.createNetServer();
        //setup the connection properties
        server.setTCPNoDelay(true); //If true then Nagle's Algorithm is disabled. If false then it is enabled.

        server.setSendBufferSize(1024); // Sets the TCP send buffer size in bytes.

        server.setReceiveBufferSize(1024); //Sets the TCP receive buffer size in bytes.

        server.setTCPKeepAlive(false); // if keepAlive is true then TCP keep alive is enabled, if false it is disabled.

        server.setReuseAddress(true);  // if reuse is true then addresses in TIME_WAIT state can be reused after they have been closed.

        server.setAcceptBacklog(25000);

        server.setClientAuthRequired(false);
        server.setUsePooledBuffers(true);
        server.setSoLinger(0);

        server.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket sock) {
                logger.info("Test get new connection");

                sock.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {

                        JsonObject json = null;
                        try{
                            json = new JsonObject(buffer.toString());
                        }
                        catch (Exception e){
                            logger.info("Ignore " + buffer.toString());
                            json = null;
                        }

                        if(json == null){
                            sock.write("UNKNOW MESSAGE\n");
                            return;
                        }


                        String cmd = json.getString("CMD");

                        if("ECHO".equalsIgnoreCase(cmd)){
                            sock.write("OHCE\n");
                            return;
                        }
                        else if("VOTE".equalsIgnoreCase(cmd)){
                            //this will be a json object
                            String serviceId    = json.getString(SERVICE_ID_STR,"");
                            int rootNumber      = json.getInteger(ROOT_NUMBER_INT,0);
                            String rootPass     = json.getString(ROOT_PASS_STR,"000000");
                            int momoNumber      = json.getInteger(MOMO_NUMBER_INT,0);
                            //String momoPass     = json.getString(MOMO_PASS_STR,"000000");
                            int exptNumber      = json.getInteger(EXPT_NUMBER_INT,0);
                            String exptName     = json.getString(EXPT_NAME_STR,"");
                            int voteCode        = json.getInteger(VOTE_CODE_INT,-1);
                            int    voteAmount   = json.getInteger(SMS_VOL_INT,0);
                            long   voteMoney     = json.getInteger(SMS_VAL_INT,0);

                            processEvent.testEvent(serviceId,rootNumber, rootPass, momoNumber,exptNumber,exptName,voteCode,voteAmount,voteMoney, new Handler<String>() {
                                @Override
                                public void handle(String s) {
                                    logger.info(s);
                                    sock.write(s + "\n");
                                }
                            });

                        }


                    }
                });

                sock.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable throwable) {
                        logger.info("Connect exception ", throwable);
                    }
                });



            }
        });

        server.listen(5073, new Handler<AsyncResult<NetServer>>() {
            @Override
            public void handle(AsyncResult<NetServer> netServerAsyncResult) {
                if(netServerAsyncResult.succeeded()){
                    logger.info("TEST START OK");
                }
                else {
                    logger.info("TEST START FAIL");
                }
            }
        });

    }

}
