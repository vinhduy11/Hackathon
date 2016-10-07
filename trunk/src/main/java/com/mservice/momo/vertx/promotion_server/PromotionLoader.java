package com.mservice.momo.vertx.promotion_server;

import com.mservice.momo.data.PromotionDb;
import com.mservice.momo.data.model.Promo;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by concu on 7/22/16.
 */
public class PromotionLoader {

    Vertx vertx;
    Logger logger;
    JsonObject glbConfig;
    public PromotionLoader(Vertx vertx, Logger logger, JsonObject globalConfig)
    {
        this.vertx = vertx;
        this.logger = logger;
        this.glbConfig = globalConfig;

    }

    /*
        This method used to get all active promotions.
     */
    public void executePromotionVerticle(final JsonObject joData, final Handler<JsonArray> callback)
    {
        final JsonArray jArrNoti = new JsonArray();
        Promo.PromoReqObj promoReqObj = new Promo.PromoReqObj();
        promoReqObj.COMMAND = Promo.PromoType.PROMO_GET_ACTIVE_LIST; //This command used to get all promotions that is running.
        Misc.requestPromoRecord(vertx, promoReqObj, logger, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jPromotionResponse) {
                JsonArray jArrListPromotions = jPromotionResponse.getArray("array", null);
                Queue<JsonObject> activePromotionQueued = new ArrayDeque<JsonObject>();
//                JsonArray jArrActivePromotions = new JsonArray();
                if (jArrListPromotions != null && jArrListPromotions.size() > 0) {
                    PromotionDb.Obj promoProgramObj = null;
                    for (Object o : jArrListPromotions) { //Loop all active promotion
                        promoProgramObj = new PromotionDb.Obj((JsonObject) o);
                        if(Misc.isValidJsonObject(promoProgramObj.EXTRA.toString().trim())) //Extra String is Json, is not is???
                        {
                            JsonObject joPromotionExtra = new JsonObject(promoProgramObj.EXTRA.toString().trim()); //Parse Extra String to JsonObject
                            if(joPromotionExtra.containsField(StringConstUtil.PromotionField.ADDRESS) && !"".equalsIgnoreCase(joPromotionExtra.getString(StringConstUtil.PromotionField.ADDRESS, "")))
                            {
                                activePromotionQueued.add(promoProgramObj.toJsonObject()); //GET ALL PROMOTIONS THAT HAVE VERTICLE ADDRESS IN WEB ADMIN.
                            }
                        }
                    }
                    //After get all suitable promotions, do it.
                    //Check suitable promotions array
                    if(activePromotionQueued.size() == 0)
                    {
                        callback.handle(jArrNoti);
                        return;
                    }
                    //If having some promotions in array
                    executePromotion(joData, activePromotionQueued, jArrNoti, callback);

                }
                else {
                    logger.info("DONT HAVE ANY ACTIVE PROMOTION IS RUNNING");
                    callback.handle(jArrNoti);
                }
            }
        });
    }


    public void executePromotion(final JsonObject joData,final Queue<JsonObject> activedPromotionQueued, final JsonArray jArrNoti, final Handler<JsonArray> callback)
    {
        if(activedPromotionQueued.size() == 0)
        {
            callback.handle(jArrNoti);
            return;
        }
        JsonObject joProgramPromotion = activedPromotionQueued.poll();
        String verticleAddress = joProgramPromotion.getObject(colName.PromoCols.EXTRA, new JsonObject()).getString(StringConstUtil.PromotionField.ADDRESS, "");
        if("".equalsIgnoreCase(verticleAddress))
        {
            executePromotion(joData, activedPromotionQueued, jArrNoti, callback);
        }
        else {
            vertx.eventBus().sendWithTimeout(verticleAddress, joData, 60000, new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> joMessage) {
                    if(joMessage.succeeded() && joMessage != null && joMessage.result() != null && joMessage.result().body() != null)
                    {
                        JsonObject joRespond = joMessage.result().body();
                        if(joRespond.containsField(StringConstUtil.PromotionField.NOTIFICATION))
                        {
                            jArrNoti.add(joRespond);
                        }
                    }
                    executePromotion(joData, activedPromotionQueued, jArrNoti, callback);
                }
            });
        }

    }
}
