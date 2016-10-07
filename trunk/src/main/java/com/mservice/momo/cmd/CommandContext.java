package com.mservice.momo.cmd;

import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.data.SockData;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Container;

import java.util.Map;

/**
 * Created by ntunam on 3/14/14.
 */
public class CommandContext {

    private Container container;
    private Message<Buffer> request;
    private MomoCommand momoCommand;
    private Map<Integer, SockData> sockDatas;
    private Map<Integer, MomoProto.TranHisV1> tranMap;

    public CommandContext(Container container, Message<Buffer> request, MomoCommand momoCommand, Map<Integer, SockData> sockDatas, Map<Integer, MomoProto.TranHisV1> tranMap) {
        this.container = container;
        this.request = request;
        this.momoCommand = momoCommand;
        this.sockDatas = sockDatas;
        this.tranMap = tranMap;
        container.logger().debug("[IN ] " + momoCommand);
    }

    public Message<Buffer> getRequest() {
        return request;
    }

    public MomoCommand getCommand() {
        return momoCommand;
    }

    public Map<Integer, SockData> getSockDatas() {
        return sockDatas;
    }

    public SockData getSockData(int phoneNumber) {
        return sockDatas.get(phoneNumber);
    }

    public Map<Integer, MomoProto.TranHisV1> getTranMap() {
        return tranMap;
    }

    public void reply(MomoCommand cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("MomoCommand can't be null.");
        }
        container.logger().debug("[OUT] " + cmd);
        this.request.reply(MomoCommand.toBuffer(cmd).getBytes());
    }

    public void replyError(int errorCode, String description) {
        CmdModels.Error replyBody = CmdModels.Error.newBuilder()
                .setCode(errorCode)
                .setDescription(description)
                .build();
        MomoCommand command = new MomoCommand(CmdModels.CommandType.ERROR, replyBody);
        reply(command);
    }
}
