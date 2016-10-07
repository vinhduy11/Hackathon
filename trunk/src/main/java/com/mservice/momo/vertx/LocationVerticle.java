package com.mservice.momo.vertx;

import com.mservice.momo.data.AgentsDb;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by User on 3/29/14.
 */
public class LocationVerticle extends Verticle {
//    public static final String ADDRESS = "com.mservice.momo.location";
    private Logger logger;
    private AgentsDb agentsDb;

    public void start() {
        logger = getContainer().logger();
        EventBus eb = vertx.eventBus();
        agentsDb = new AgentsDb(eb,logger);

        Handler<Message> myHandler = new Handler<Message>() {
            public void handle(Message message) {
            MomoMessage momoMessage = MomoMessage.fromBuffer((Buffer)message.body()) ;

            switch (momoMessage.cmdType){
                case MomoProto.MsgType.STORE_LOCATION_VALUE:
                    break;
                default:
                    logger.error("Call Location verticle with invalid message type: ".concat(String.valueOf(momoMessage.cmdType)) );
                    break;
            }
            }
        };

        eb.registerLocalHandler(AppConstant.LocationVerticle_ADDRESS, myHandler);
    }


}
