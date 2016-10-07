package com.mservice.momo.gateway.internal.core.objects;

import com.mservice.momo.gateway.internal.core.msg.Core;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.gateway.internal.soapin.information.SoapError;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.buffer.Buffer;

import java.util.ArrayList;

/**
 * Created by concu on 9/26/14.
 */
public class Response {
    public long Tid = -1;
    public int Error = -1;
    public String Description = "";
    public ArrayList<KeyValue> KeyValueList = new ArrayList<>();

    public static Response parse(Buffer buffer,final Common.BuildLog buildLog) {
        Response replyObj = new Response();

        final MomoMessage reply = MomoMessage.fromBuffer(buffer);
        Core.StandardReply rpl;
        try {
            rpl = Core.StandardReply.parseFrom(reply.cmdBody);
        } catch (Exception ex) {
            rpl = null;
        }

        //lock tien khong thanh cong
        if ((rpl == null)) {

            buildLog.add("error",-100);
            buildLog.add("desc","Loi he thong");

            replyObj.Error = -100;
            replyObj.Description = "Lỗi hệ thống";

        } else {
            replyObj.Error = rpl.getErrorCode();
            replyObj.Description = rpl.getDescription();
            replyObj.Tid = rpl.getTid();

            buildLog.add("tranid", rpl.getTid());
            buildLog.add("error", rpl.getErrorCode());
            buildLog.add("desc", rpl.getDescription());
            buildLog.add("soap desc", SoapError.getDesc(rpl.getErrorCode()));

            if(rpl.getParamsCount() > 0){
                replyObj.KeyValueList = new ArrayList<>();

                for(int i =0; i< rpl.getParamsCount(); i++){

                    KeyValue kv = new KeyValue();
                    kv.Key = rpl.getParams(i).getKey();
                    kv.Value = rpl.getParams(i).getValue();
                    replyObj.KeyValueList.add(kv);

                    buildLog.add(rpl.getParams(i).getKey(),rpl.getParams(i).getValue());
                }
            }
        }
        return replyObj;
    }
}
