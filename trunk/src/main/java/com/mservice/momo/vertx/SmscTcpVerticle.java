package com.mservice.momo.vertx;

import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by nam on 4/27/14.
 */
public class SmscTcpVerticle extends Verticle {
    @Override
    public void start() {
        JsonObject globalConfig = container.config();

        JsonObject config = globalConfig.getObject("smscTcpVerticle");
        String hostAddress = config.getString("host", "0.0.0.0");
        int port = config.getInteger("port", 8444);

        NetServer server = vertx.createNetServer();

        server.setReuseAddress(true);
        server.setTCPNoDelay(true);
        server.setTCPKeepAlive(true);

        server.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket event) {

                container.logger().info("New client: " + event.remoteAddress());
                event.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer buffer) {

                        container.logger().info("Income " + buffer.length() + "bytes");

                        final MomoMessage msg = MomoMessage.fromBuffer(buffer);
                        if (msg == null) {
                            container.logger().warn("Error message");
                            return;
                        }
                        if (msg.cmdType > 0) {
                            vertx.eventBus().send(AppConstant.AppSmsVerticle_ADDRESS, msg.toBuffer());
                            writeDataToSocket(event, msg.toBuffer(), container.logger());
//                            event.write(buffer);
                        }
                    }
                });
            }
        });

        server.listen(port, hostAddress, new Handler<AsyncResult<NetServer>>() {
            @Override
            public void handle(AsyncResult<NetServer> event) {
                if (event.failed()) {
                    container.logger().info("SmscTcpVerticle started fail! ");
                    event.cause().printStackTrace();
                } else {
                    container.logger().info("SmscTcpVerticle started successfully!");
                }
            }
        });
        }

    class IncomePacketInfo {
        public int size = Integer.MIN_VALUE;
    }

    public static void writeDataToSocket(final NetSocket sock, Buffer buf, Logger logger) {
        if (!sock.writeQueueFull()) {
            sock.write(buf);
            //for debug only
            MomoProto.MsgType type = MomoProto.MsgType.valueOf(MomoMessage.getType(buf));
            if (type != null) {
                logger.info(sock.remoteAddress().toString() + " >> send msg type " + type.name() + " " + buf.length() + " bytes of data");
            } else {
                logger.info(sock.remoteAddress().toString() + " >> send unknown msg type " + buf.length() + " bytes of data");
            }

        } else {
            sock.pause();
            sock.drainHandler(new VoidHandler() {
                public void handle() {
                    sock.resume();
                }
            });
        }
    }
}
