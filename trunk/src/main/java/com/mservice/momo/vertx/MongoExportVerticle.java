package com.mservice.momo.vertx;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.platform.Verticle;

import java.io.IOException;

/**
 * Created by nam on 6/15/14.
 */
public class MongoExportVerticle extends Verticle{
    public static final String CMD_EXPORT_DUDOAN = "exportDuDoan";

    @Override
    public void start() {
        vertx.eventBus().registerLocalHandler(AppConstant.MongoExportVerticle, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                String cmd = message.body().getString("cmd");
                switch (cmd){
                    case CMD_EXPORT_DUDOAN:
                        exportDuDoan(message);
                        break;
                    default:
                        message.reply(new JsonObject().putNumber("error", 1).putString("desc", "Unknown command."));
                }
            }
        });
    }

    private void exportDuDoan(Message<JsonObject> message) {
        String matchId = message.body().getString("matchId");
        if (matchId == null) {
            message.reply(new JsonObject().putNumber("error", 2).putString("desc", "Missing matchId param"));
            return;
        }
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("mongoexport --host 172.16.14.35 --db newmomovn_db --collection wc_duDoan_" + matchId + " --csv --out /tmp/" + matchId + ".csv -f _id,result,a,b,time");

            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        message.reply(new JsonObject().putNumber("error", 0));
    }
}
