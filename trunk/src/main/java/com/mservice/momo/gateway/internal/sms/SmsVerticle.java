package com.mservice.momo.gateway.internal.sms;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.net.ConnectException;
import java.rmi.RemoteException;


/**
 * Created by admin on 2/13/14.
 */
public class SmsVerticle extends Verticle {
//    public static final String ADDRESS = "com.mservice.momo.gateway.internal.sms";
    public static String endPoint = null;

    private SmsSender sender;
    private Logger logger;

    //todo this must be a worker verticle

    public static void LoadCfg(JsonObject sms_cfg){
        endPoint = sms_cfg.getString("sys.sms.api", "http://172.16.18.50:18080/ClientMark/services/Alert");
    }

    public void start() {
        logger = getContainer().logger();
        JsonObject sms_config = new JsonObject();
        sms_config.putString("sys.sms.api", "http://172.16.18.50:18080/ClientMark/services/Alert");
        JsonObject sms_cfg = container.config().getObject("sms", sms_config);

//        JsonObject sms_cfg = container.config();
        LoadCfg(sms_cfg);

        logger.info("SMS SERVICE ENDPOINT IS " + endPoint);

        //todo init the sms sender here
        sender = new SmsSender(logger, endPoint);

        //todo we listen on the bus here
        EventBus eb = vertx.eventBus();

        Handler<Message> myHandler = new Handler<Message>() {

            public void handle(Message message) {
                //we know this body must be a MomoMessage
                byte[] buf = (byte[]) message.body();
                SoapProto.SendSms req;
                try {
                    req = SoapProto.SendSms.parseFrom(buf);
                } catch (InvalidProtocolBufferException e) {
                    req = null;
                }

                if (req != null) {

                    try {
                        sender.send(String.valueOf(req.getToNumber()), req.getContent());
                    } catch (ConnectException ce) {
                        logger.info("SEND SMS ConnectException " + ce.getMessage());
                    } catch (RemoteException re) {
                        logger.info("SEND SMS RemoteException " + re.getMessage());
                    } catch (Exception ex) {
                        logger.info("SEND SMS Exception " + ex.getMessage());
                    }
                }
            }
        };

        eb.registerLocalHandler(AppConstant.SmsVerticle_ADDRESS, myHandler);
    }
}
