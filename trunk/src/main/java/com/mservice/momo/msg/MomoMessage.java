package com.mservice.momo.msg;


import org.vertx.java.core.buffer.Buffer;

/**
 * Created by admin on 2/8/14.
 */
public class MomoMessage {

    public int cmdType  = 0;
    public long cmdIndex = 0;
    public int cmdPhone = 0;
    public byte[] cmdBody;

    public static final char BELL_CHAR = 7;
    public static final String BELL = "" + BELL_CHAR;

    public static final char LF_CHAR = 10;
    public static final String LF = "" + LF_CHAR;

    public static  final  MomoMessage fromBuffer(Buffer buffer){
        int dataLen = buffer.length();
        if (dataLen >= MomoMessage.MIN_LEN) {
            byte first = buffer.getByte(0);
            if (first == STX_BYTE) {
                int len = getLen(buffer);
                int typ = getType(buffer);
                long idx = getIndex(buffer);
                int phn = getPhone(buffer);
                if(len <= dataLen){
                    byte[] bdy = buffer.getBytes(MomoMessage.HEAD_LEN, len - 1);
                    return new MomoMessage(typ,idx,phn,bdy);
                }
                else{
                    System.out.println("can not parse MomoMessage len : " + len + " - type : " + typ + " - index : " + idx + " - phone : " + phn + " full data " + buffer.toString());
                }
            }
            else {
                System.out.println("can not parse MomoMessage first byte : <" + first + "><" + buffer.toString());
            }
        }
        else {
            System.out.println("received partial message <" + dataLen + ">" + buffer.toString());
        }

        return null;
    }

    public static final Buffer buildBuffer(int type, long index, int phone, byte[] body){

        //len must smaller than 1024
        if(body == null){
            return new Buffer(MomoMessage.MIN_LEN)
                    .appendByte(STX_BYTE)
                    .appendInt(MIN_LEN)
                    .appendInt(type)
                    .appendLong(index)
                    .appendInt(phone)
                    .appendByte(ETX_BYTE);
        }
        else {
            int len = body.length + MomoMessage.MIN_LEN;

            return new Buffer(1024)
                    .appendByte(STX_BYTE)
                    .appendInt(len)
                    .appendInt(type)
                    .appendLong(index)
                    .appendInt(phone)
                    .appendBytes(body)
                    .appendByte(ETX_BYTE);
        }
    }

    public static final Buffer buildAck(int index){
        return new Buffer(MIN_LEN)
                .appendByte(STX_BYTE)
                .appendInt(MIN_LEN)
                .appendInt(MomoProto.MsgType.ACK_VALUE)
                .appendInt(index)
                .appendInt(0)
                .appendByte(ETX_BYTE);
    }


    public Buffer toBuffer(){

        int len = ((cmdBody != null) ? cmdBody.length : 0) + MomoMessage.MIN_LEN;
        //len must smaller than 1024
        return new Buffer(1024)
                .appendByte(STX_BYTE)
                .appendInt(len)
                .appendInt(cmdType)
                .appendLong(cmdIndex)
                .appendInt(cmdPhone)
                .appendBytes(cmdBody)
                .appendByte(ETX_BYTE);
    }


    public MomoMessage(int type, long index, int phone, byte[] body){
        cmdType = type;
        cmdIndex = index;
        cmdPhone = phone;
        cmdBody = body;
    }


    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[").append(cmdPhone).append(", ");
        s.append(cmdIndex).append("]");
        return s.toString();
    }

    /**
     * append the EXT then reset index & len
      * @param buffer
     * @return
     */
    public Buffer fillBuffer(Buffer buffer){

        buffer.appendByte(ETX_BYTE);
        setIndex(buffer,cmdIndex);
        setLen(buffer,buffer.length());
        return buffer;
    }

    public static final byte getFirstByte(Buffer buffer){
        return buffer.getByte(0);
    }

    public static final byte getLastByte(Buffer buffer){
        return buffer.getByte(buffer.length() - 1);
    }

    public static final int getLen(Buffer buffer){
        return buffer.getInt(1);
    }



    public static final Buffer setLen(Buffer buffer, int len){
        return buffer.setInt(1,len);
    }

    public static final int getType(Buffer buffer){
        return buffer.getInt(5);
    }

    public static final Buffer setType(Buffer buffer, int typ){
        return buffer.setInt(5, typ);
    }

    public static final long getIndex(Buffer buffer){
        return buffer.getLong(9);
    }
    
    public static final Buffer setIndex(Buffer buffer, long idx){
        return buffer.setLong(9, idx);
    }

    public static final int getPhone(Buffer buffer){
        return buffer.getInt(17);
    }

    public static final Buffer setPhone(Buffer buffer, int phone){
        return buffer.setInt(17, phone);
    }

    public static final int HEAD_LEN = 21;// [STX(1)][LEN(4)][TYPE(4)][INDEX(8)][PHONE[4]]
    public static final int TAIL_LEN = 01;// [ETX(1)]
    public static final int MIN_LEN = HEAD_LEN + TAIL_LEN;
    public static final byte STX_BYTE = 2;
    public static final byte ETX_BYTE = 3;

}
