package com.mservice.momo.tcp;

import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.msg.MomoMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by hung_thai on 6/10/14.
 */
public class TCPCoreClient {

    private AtomicLong indexGenerator = new AtomicLong(0L);

    private String host = "localhost";
    private int port = 80;
    private int connId =696969;
    private Socket socket;

    private LinkedBlockingQueue<TaskInfo> sendTasks = new LinkedBlockingQueue<TaskInfo>();

    private ConcurrentHashMap<Long, TaskInfo> receiveTasks = new ConcurrentHashMap<Long, TaskInfo>();

    private ExecutorService executors = Executors.newFixedThreadPool(2);

    private boolean stopFlag = false;

    public TCPCoreClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public void start() {
        stopFlag = false;
        doConnect();
    }

    public void stop() {
        stopFlag = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void submit(MomoMessage command) {
        command.cmdPhone = this.connId;
        TaskInfo task = new TaskInfo(command);

        sendTasks.add(task);


    }

    private void startReadingMessage() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = socket.getInputStream();
                    DataInputStream dataIn = new DataInputStream(in);
                    int i=0;
                    while (true) {
                        byte first = dataIn.readByte();
                        int msgLen = dataIn.readInt();
                        int cmdType = dataIn.readInt();
                        long cmdIndex = dataIn.readLong();
                        int cmdPhone = dataIn.readInt();
                        byte[] cmdBody = new byte[msgLen - MomoMessage.MIN_LEN];
                        // copy data to buffer
                        dataIn.read(cmdBody);
                        byte last = dataIn.readByte();
                        final MomoMessage result = new MomoMessage(cmdType, cmdIndex, cmdPhone, cmdBody);

                        if (first != MomoMessage.STX_BYTE || last != MomoMessage.ETX_BYTE) {
                            // network error here;
                            System.out.println(  " checksum error" + result.cmdBody.toString());
                            socket.close();
                        }
                        System.out.println("CoreMessage IN" +result.toString());
                        if (cmdType == Core.MsgType.HELLO_REPLY_VALUE){
                            connId = cmdPhone;
                        }else if(cmdType == Core.MsgType.ACK_VALUE){
                            System.out.println("ACK reqid " + cmdIndex);
                        }else {
                            System.out.println(new Date(System.currentTimeMillis())+"-CoreMessage In: "+result.toString());
                            Core.StandardReply reply = Core.StandardReply.parseFrom(result.cmdBody);
                            System.out.println(  " get msg " +i++ +"-"+ reply.getTid()+"-ResultCode: "+reply.getErrorCode()+"-Desc: "+reply.getDescription());

                            submit(new MomoMessage(Core.MsgType.ACK_VALUE, cmdIndex, cmdPhone, "".getBytes()));
                        }


                    }
                } catch (IOException e) {
                    System.out.println("Error");
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
//					logger.info("Disconnected to " + host + ":" + port);
//					doConnect();
                }
            }
        }).start();

    }

    private void startWritingMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                TaskInfo task = null;
                try {
                    OutputStream out = socket.getOutputStream();
                    DataOutputStream dataOut = new DataOutputStream(out);

                    MomoMessage momoMsg = new MomoMessage(0, 0, 0, "abc".getBytes());

                    ByteBuffer mWriteBuffer = ByteBuffer.allocate(1024);

                    int len = ((momoMsg.cmdBody != null) ? momoMsg.cmdBody.length : 0 )+ MomoMessage.MIN_LEN;

                    while (true) {
                        try {
                            task = sendTasks.take();

                            momoMsg = task.momoMessage;
                            System.out.println(new Date(System.currentTimeMillis())+"- CoreMessage OUT: "+momoMsg.toString());

                            len = ((momoMsg.cmdBody != null) ? momoMsg.cmdBody.length : 0 )+ MomoMessage.MIN_LEN;
                            if (len < 1024) {
                                mWriteBuffer.clear();
                                mWriteBuffer.put(MomoMessage.STX_BYTE);

                                mWriteBuffer.putInt(len);
                                mWriteBuffer.putInt(momoMsg.cmdType);
                                mWriteBuffer.putLong(momoMsg.cmdIndex);
                                mWriteBuffer.putInt(momoMsg.cmdPhone);
                                if(momoMsg.cmdBody != null){
                                    mWriteBuffer.put(momoMsg.cmdBody);
                                }
                                mWriteBuffer.put(MomoMessage.ETX_BYTE);
                                try {
                                    dataOut.write(mWriteBuffer.array(), 0, len);
                                    dataOut.flush();
                                } catch (Exception e) {
                                }
                            }
                            else{
                                System.out.println("Too big data");
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private synchronized void doConnect() {
        if(stopFlag)
            return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        System.out.println(String.format("Connecting to %s:%d", host, port));
                        if (socket != null)
                            socket.close();
                        socket = new Socket();
                        socket.setKeepAlive(true);
                        socket.setTcpNoDelay(true);
                        socket.connect(new InetSocketAddress(host, port), 5000);
                        startReadingMessage();
                        startWritingMessage();
                        System.out.println(String.format("Connected!", host, port));
                        return;
                    } catch (UnknownHostException e) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    } catch (IOException e) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    class TaskInfo {

        private MomoMessage momoMessage;

        public TaskInfo(MomoMessage momoMessage) {
            this.momoMessage = momoMessage;

        }

        public MomoMessage getMomoMessage() {
            return momoMessage;
        }


    }
}
