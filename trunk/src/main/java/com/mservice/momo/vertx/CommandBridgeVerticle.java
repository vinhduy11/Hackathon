package com.mservice.momo.vertx;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.msg.MomoCommand;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by nam on 4/30/14.
 */
public class CommandBridgeVerticle extends Verticle {
    @Override
    public void start() {
        NetServer server = vertx.createNetServer();

        server.setTCPKeepAlive(true);
        server.setReuseAddress(true);

        server.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket event) {
                try {
                    final PipedInputStream inputStream = new PipedInputStream(1000000);
                    final PipedOutputStream outputStream = new PipedOutputStream(inputStream);
                    final DataInputStream dataIn = new DataInputStream(inputStream);
                    final IncomePacketInfo packetInfo = new IncomePacketInfo();

                    container.logger().info(event.remoteAddress() + " connects to server.");

                    event.dataHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(Buffer income) {
                            try {
                                outputStream.write(income.getBytes());

                                while ((packetInfo.size < 0 && inputStream.available() > 4) || (packetInfo.size > 0 && inputStream.available() >= packetInfo.size)) {
                                    if (packetInfo.size == Integer.MIN_VALUE) { //read next packet
                                        if (dataIn.available() > 4) {
                                            packetInfo.size = dataIn.readInt();
                                        }
                                    }

                                    if (packetInfo.size > 0 && inputStream.available() >= packetInfo.size) {
                                        byte[] arr = new byte[packetInfo.size];
                                        dataIn.readFully(arr);
                                        Buffer momoMessageBuffer = new Buffer(arr);

                                        final MomoCommand requestMessage = MomoCommand.fromBuffer(momoMessageBuffer);

                                        container.logger().debug("[CommandBridgeVerticle] [IN] " + packetInfo.size + " bytes");
                                        vertx.eventBus().send(AppConstant.CommandVerticle_ADDRESS, momoMessageBuffer, new Handler<Message<byte[]>>() {
                                            @Override
                                            public void handle(Message<byte[]> result) {
                                                byte[] packetBody = result.body();
                                                try {
                                                    if (packetBody != null && packetBody.length > 0) {
                                                        MomoCommand cmdReply = MomoCommand.fromBuffer(new Buffer(packetBody));
                                                        cmdReply.setId(requestMessage.getId());

                                                        Buffer buffer = MomoCommand.toBuffer(cmdReply);
                                                        event.write(new Buffer().appendInt(buffer.length()));
                                                        event.write(buffer);

                                                        container.logger().debug("[CommandBridgeVerticle] [OUT] " + buffer.length() + " bytes");
                                                    }
                                                } catch (InvalidProtocolBufferException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        packetInfo.size = Integer.MIN_VALUE;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

//                    final long di = vertx.setPeriodic(1000, new Handler<Long>() {
//                        @Override
//                        public void handle(Long a) {
//                            event.write("abcdef");
//                            System.out.println("write...");
//                        }
//                    });
                    event.closeHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void v) {
                            container.logger().info(event.remoteAddress() + " has disconnected.");
//                            vertx.cancelTimer(di);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        server.listen(8445, "0.0.0.0", new Handler<AsyncResult<NetServer>>() {
            @Override
            public void handle(AsyncResult<NetServer> event) {
                if (event.failed()) {
                    container.logger().info("CommandBridgeVerticle started fail! ");
                    event.cause().printStackTrace();
                } else {
                    container.logger().info("CommandBridgeVerticle started successfully!");
                }
            }
        });
    }

    class IncomePacketInfo {
        public int size = Integer.MIN_VALUE;
    }
}
