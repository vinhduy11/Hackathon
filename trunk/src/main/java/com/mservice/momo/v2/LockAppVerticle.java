package com.mservice.momo.v2;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.data.web.PortData;
import com.mservice.momo.gateway.internal.soapin.information.ErrorMapping;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Created by KhoaNguyen on 9/30/2016.
 */
public class LockAppVerticle extends Verticle {

    Logger logger;
    JsonObject glbConfig;
    PhonesDb phonesDb;
    @Override
    public void start() {

        logger = container.logger();
        glbConfig = container.config();
        phonesDb = new PhonesDb(vertx.eventBus(), logger);
        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest httpServerRequest) {
                if ("/lockphone".equalsIgnoreCase(httpServerRequest.path()) && "POST".equalsIgnoreCase(httpServerRequest.method()))
                httpServerRequest.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer receiveData) {
                        Common.BuildLog log = new Common.BuildLog(logger);
                        log.setPhoneNumber("lockphone v2");
                        HttpServerResponse httpResponse = httpServerRequest.response().putHeader("content-type", "application/json");
                        JsonObject joReply = new JsonObject();
                        log.add("receiveData", receiveData);
                        if (receiveData == null || "".equalsIgnoreCase(receiveData.toString()) || !Misc.isValidJsonObject(receiveData.toString())) {
                            joReply.putNumber(StringConstUtil.ERROR, 2);
                            joReply.putString(StringConstUtil.DESCRIPTION, ErrorMapping.getDescriptionError(2));
                            httpResponse.end(joReply.toString());
                            log.writeLog();
                            return;
                        }
                        JsonObject joReceivedData = new JsonObject(receiveData.toString());
                        String phoneNumber = joReceivedData.getString(StringConstUtil.NUMBER, "0");

                        JsonObject joUpdate = new JsonObject();
                        joUpdate.putBoolean(colName.PhoneDBCols.IS_LOCKED_V1, true);
                        phonesDb.updatePartial(DataUtil.strToInt(phoneNumber), joUpdate, new Handler<PhonesDb.Obj>() {
                            @Override
                            public void handle(PhonesDb.Obj phoneObj) {

                            }
                        });
                    }
                });
            }
        }).listen(PortData.PORT_LOCK_V1, "172.16.9.60");


    }




}
