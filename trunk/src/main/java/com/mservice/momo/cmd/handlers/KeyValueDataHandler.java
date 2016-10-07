package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.util.ValidationUtil;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 3/27/14.
 */
public class KeyValueDataHandler extends CommandHandler {
    public KeyValueDataHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject config) {
        super(mainDb, vertx, container, config);
    }

    public void getOrPut(final CommandContext context) {
        CmdModels.KeyValueData cmd = (CmdModels.KeyValueData) context.getCommand().getBody();

        if (ValidationUtil.isEmpty(cmd.getKey())) {
            context.replyError(-1, "Key can't be null");
            return;
        }

        switch (cmd.getCommandType()) {
            case GET:
                mainDb.keyValueDb.get(cmd.getKey(), new Handler<String>() {
                    @Override
                    public void handle(String value) {
                        CmdModels.KeyValueDataReply.Builder body = CmdModels.KeyValueDataReply.newBuilder()
                                .setResult(CmdModels.KeyValueDataReply.Result.SUCCESS);
                        if (value != null)
                            body.setValue(value);
                        MomoCommand replyComand = new MomoCommand(CmdModels.CommandType.KEY_VALUE_DATA_REPLY, body.build());
                        context.reply(replyComand);
                    }
                });
                break;
            case PUT:
                mainDb.keyValueDb.put(cmd.getKey(), cmd.getValue(), new Handler<String>() {
                    @Override
                    public void handle(String value) {
                        CmdModels.KeyValueDataReply body = CmdModels.KeyValueDataReply.newBuilder()
                                .setResult(CmdModels.KeyValueDataReply.Result.SUCCESS)
                                .build();
                        MomoCommand replyComand = new MomoCommand(CmdModels.CommandType.KEY_VALUE_DATA_REPLY, body);
                        context.reply(replyComand);
                    }
                });
                break;
            default:
                throw new IllegalAccessError("No such command! " + cmd.getCommandType());
        }
    }
}
