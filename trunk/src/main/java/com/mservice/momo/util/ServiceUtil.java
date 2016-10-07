package com.mservice.momo.util;

import com.mservice.momo.data.model.colName;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;


/**
 * Created by concu on 2/16/16.
 */
public class ServiceUtil {

    public static void loadConnectorServiceBusName(int command, final Vertx vertx,final Logger logger, final Handler<JsonArray> jsonArrayHandler)
    {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = command;
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                logger.info("GET_CONNECTOR_SERVICE_BUS_NAME la " + jsonArray);
                if (jsonArray != null) {
                    logger.info("GET_CONNECTOR_SERVICE_BUS_NAME la" + jsonArray.size());
                    jsonArrayHandler.handle(jsonArray);
                }
                else
                {
                    jsonArrayHandler.handle(new JsonArray());
                }
            }
        });
    }

    public static void updateConnectorServiceBusName(int command, final Vertx vertx,final Logger logger)
    {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = command;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
    }

    public static void reloadConnectorServiceBusName(int command, final Vertx vertx,final Logger logger, final Handler<Boolean> callback)
    {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = command;
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> message) {
                Boolean isReload = message.body();
                callback.handle(isReload);
            }
        });
    }

    //SERVICE GIFT RULE
    public static void loadServiceGiftRules(int command, final Vertx vertx,final Logger logger, final Handler<JsonArray> jsonArrayHandler)
    {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = command;
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                logger.info("GET_SERVICE_GIFT_RULE la " + jsonArray);
                if (jsonArray != null) {
                    logger.info("GET_SERVICE_GIFT_RULE la" + jsonArray.size());
                    jsonArrayHandler.handle(jsonArray);
                }
                else
                {
                    jsonArrayHandler.handle(new JsonArray());
                }
            }
        });
    }

    //Update service gift rule variable global
    public static void updateServiceGiftRules(int command, final Vertx vertx,final Logger logger)
    {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = command;
        vertx.eventBus().publish(AppConstant.ConfigVerticleService_Update, serviceReq.toJSON());
    }

    public static void reloadServiceGiftRules(int command, final Vertx vertx,final Logger logger, final Handler<Boolean> callback)
    {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = command;
        vertx.eventBus().send(AppConstant.ConfigVerticleService, serviceReq.toJSON(), new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> message) {
                Boolean isReload = message.body();
                callback.handle(isReload);
            }
        });
    }

    public static void loadServiceGiftRuleByServiceId(final String serviceId, int command, final Vertx vertx,final Logger logger
            , final Handler<JsonObject> jsonObjGiftRuleHandler)
    {
        final Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.Command = command;
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {
                logger.info("GET_SERVICE_GIFT_RULE la " + jsonArray);
                JsonObject jsonService = new JsonObject();
                if (jsonArray != null) {
                    logger.info("GET_SERVICE_GIFT_RULE la" + jsonArray.size());
                    for(int i = 0; i < jsonArray.size(); i++)
                    {
                        jsonService = jsonArray.get(i);
                        if(serviceId.equalsIgnoreCase(jsonService.getString(colName.CheckServiceGiftRuleCol.SERVICE_ID, "")))
                        {
                            break;
                        }
                    }
                    jsonObjGiftRuleHandler.handle(jsonService);
                }
                else
                {
                    jsonObjGiftRuleHandler.handle(jsonService);
                }
            }
        });
    }
}
