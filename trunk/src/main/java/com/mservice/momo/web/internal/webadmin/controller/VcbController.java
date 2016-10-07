package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.PhonesDb;
import com.mservice.momo.data.VcbCmndRecs;
import com.mservice.momo.data.VcbRecords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.vcb.VcbCommon;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by locnguyen on 25/08/2014.
 */
public class VcbController {

    private Vertx vertx;
    private Logger logger;
    private VcbCmndRecs vcbCmndRecs;
    private VcbRecords vcbRecords;
    private PhonesDb phonesDb;

    public VcbController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();

        vcbCmndRecs = new VcbCmndRecs(vertx.eventBus(), logger);
        vcbRecords = new VcbRecords(vertx.eventBus(),logger);
        phonesDb = new PhonesDb(vertx.eventBus(),logger);
    }

    @Action(path = "/vcb/cmnd")
    public void findCmnd(HttpRequestContext context, final Handler<JsonObject> callback) {

        MultiMap params = context.getRequest().params();
        String phoneNumber = params.get("number")== null ? "" : params.get("number");
        String cardId = params.get("cardid")== null ? "" : params.get("cardid");

        vcbCmndRecs.findOne(cardId, new Handler<VcbCmndRecs.Obj>() {
            @Override
            public void handle(VcbCmndRecs.Obj obj) {
                obj = obj == null ? new VcbCmndRecs.Obj() : obj;
                callback.handle(obj.toJson());
            }
        });
    }

    @Action(path = "/vcb/vcb")
    public void findVcb(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String phoneNumber = params.get("number")== null ? "" : params.get("number");
        String cardId = params.get("cardid")== null ? "" : params.get("cardid");

        vcbRecords.getVCBByCardIdOrPhone(DataUtil.strToInt(phoneNumber), cardId, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray objects) {
                JsonObject jo = new JsonObject();
                jo.putArray("arr", objects);
                callback.handle(jo);
            }
        } );
    }

//    @Action(path = "/vcb/vcb1")
//    public void findCustomerInfoVcb(HttpRequestContext context, final Handler<JsonObject> callback) {
//        MultiMap params = context.getRequest().params();
//        String begintime = params.get("begintime")== null ? "" : params.get("begintime");
//        String endtime = params.get("endtime")== null ? "" : params.get("endtime");
//
//        long begin = DataUtil.strToLong(begintime);
//        long end = DataUtil.strToLong(endtime);
//
//        vcbRecords.getVCBAccountByTime(begin, end, new Handler<JsonArray>() {
//            @Override
//            public void handle(JsonArray objects) {
//                JsonObject jo = new JsonObject();
//                jo.putArray("arr", objects);
//                callback.handle(jo);
//            }
//        } );
//    }

    @Action(path = "/vcb/phone")
    public void findVcbPhone(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String phoneNumber = params.get("number")== null ? "" : params.get("number");
        String cardId = params.get("cardid")== null ? "" : params.get("cardid");

        phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneNumber), new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj fobj) {
                final PhonesDb.Obj obj = fobj == null ? new PhonesDb.Obj() : fobj;

                if(!"".equalsIgnoreCase(obj.inviter)){
                    phonesDb.getPhoneObjInfo(DataUtil.strToInt(obj.inviter), new Handler<PhonesDb.Obj>() {
                        @Override
                        public void handle(PhonesDb.Obj iter) {
                            iter = iter == null ? new PhonesDb.Obj(): iter;
                            JsonArray ar = new JsonArray();
                            ar.add(obj.toJsonObject());
                            ar.add(iter.toJsonObject());
                            JsonObject jo = new JsonObject();
                            jo.putArray("arr",ar);
                            callback.handle(jo);
                        }
                    });
                }else{
                    PhonesDb.Obj iter = new PhonesDb.Obj();
                    JsonArray ar = new JsonArray();
                    ar.add(obj.toJsonObject());
                    ar.add(iter.toJsonObject());
                    JsonObject jo = new JsonObject();
                    jo.putArray("arr",ar);
                    callback.handle(jo);

                }
            }
        });
    }

    @Action(path = "/vcb/updatephone")
    public void updatePhone(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String phoneNumber = params.get("number")== null ? "" : params.get("number");
        String cardId = params.get("cardid")== null ? "" : params.get("cardid");

        final JsonObject jo = new JsonObject();

        if("".equalsIgnoreCase(phoneNumber)){
            jo.putNumber("err", 1);
            jo.putString("desc", "Vui lòng nhập số điện thoại");
            callback.handle(jo);
            return;
        }

        if("".equalsIgnoreCase(cardId)){
            jo.putNumber("err", 1);
            jo.putString("desc", "Vui lòng nhập CMND");
            callback.handle(jo);
            return;
        }

        JsonObject joUpdate = new JsonObject();
        joUpdate.putNumber(colName.PhoneDBCols.CREATED_DATE, System.currentTimeMillis());
        joUpdate.putString(colName.PhoneDBCols.BANK_PERSONAL_ID,cardId);

        phonesDb.update(DataUtil.strToInt(phoneNumber), joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                String result = aBoolean ? "success" : "failed";
                int err = aBoolean ? 0 : 1;
                jo.putNumber("err", err);
                jo.putString("desc", "Cập nhật thông tin : " + result);
                callback.handle(jo);
            }
        });
    }

    @Action(path = "/vcb/givegift")
    public void giveGiftForA(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String phoneA = params.get("numbera")== null ? "" : params.get("numbera");
        String phoneB = params.get("numberb")== null ? "" : params.get("numberb");

        final JsonObject jo = new JsonObject();

        if("".equalsIgnoreCase(phoneA)){
            jo.putNumber("err", 1);
            jo.putString("desc", "Vui lòng nhập số điện thoại người thiệu");
            callback.handle(jo);
            return;
        }

        if("".equalsIgnoreCase(phoneB)){
            jo.putNumber("err", 1);
            jo.putString("desc", "Vui lòng nhập số điện thoại người được giới thiệu");
            callback.handle(jo);
            return;
        }

        VcbCommon.requestGiftForAAdmin(vertx
                                        ,DataUtil.strToInt(phoneA)
                                        ,0
                                        ,0
                                        ,0
                                        ,phoneB
                                        ,new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                callback.handle(jsonObject);
            }
        });
    }


    @Action(path = "/vcb/givegiftforB")
    public void giveGiftForB(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String phoneA = params.get("numbera")== null ? "" : params.get("numbera");
        String phoneB = params.get("numberb")== null ? "" : params.get("numberb");
        long tranId = params.get("tranid") == null ? 0 : DataUtil.stringToUNumber(params.get("tranid"));

        final JsonObject jo = new JsonObject();

        if("".equalsIgnoreCase(phoneA)){
            jo.putString("desc", "Vui lòng nhập số điện thoại người thiệu");
            callback.handle(jo);
            return;
        }

        if("".equalsIgnoreCase(phoneB)){
            jo.putString("desc", "Vui lòng nhập số điện thoại người được giới thiệu");
            callback.handle(jo);
            return;
        }

        if(DataUtil.strToInt(phoneA) == 0 || DataUtil.strToInt(phoneB) == 0){
            jo.putString("desc", "Số điện thoại không hợp lệ");
            callback.handle(jo);
            return;
        }
        //BEGIN 0000000003 GIVE GIFT FOR VCB WITHOUT TIME

        VcbCommon.requestGiftForBAdmin(vertx
                , DataUtil.strToInt(phoneB)
                , phoneA
                , 10000000
                , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
                , tranId, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject jsonObject) {
                callback.handle(jsonObject);
            }
        });

//        VcbCommon.requestGiftForBAdmin_withoutTime(vertx
//                , DataUtil.strToInt(phoneB)
//                , phoneA
//                , 10000000
//                , MomoProto.TranHisV1.TranType.BANK_IN_VALUE
//                , tranId, new Handler<JsonObject>() {
//            @Override
//            public void handle(JsonObject jsonObject) {
//                callback.handle(jsonObject);
//            }
//        });
        //END 0000000003 GIVE GIFT FOR VCB WITHOUT TIME

    }


    @Action(path = "/vcb/givegiftforBByPG")
    public void giveGiftForBByPG(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        final String pgCode = params.get("pgcode")== null ? "" : params.get("pgcode");
        final String phoneB = params.get("numberb")== null ? "" : params.get("numberb");
        final long tranId = params.get("tranid") == null ? 0 : DataUtil.stringToUNumber(params.get("tranid"));

        final JsonObject jo = new JsonObject();

        if("".equalsIgnoreCase(pgCode)){
            jo.putString("desc", "Vui lòng nhập mã PG");
            callback.handle(jo);
            return;
        }

        if("".equalsIgnoreCase(phoneB)){
            jo.putString("desc", "Vui lòng nhập số điện thoại người được giới thiệu");
            callback.handle(jo);
            return;
        }

        if(DataUtil.strToInt(pgCode) == 0 || DataUtil.strToInt(phoneB) == 0){
            jo.putString("desc", "Số điện thoại không hợp lệ");
            callback.handle(jo);
            return;
        }

        phonesDb.getPhoneObjInfo(DataUtil.strToInt(phoneB),new Handler<PhonesDb.Obj>() {
            @Override
            public void handle(PhonesDb.Obj obj) {
                if(obj == null
                        || "".equalsIgnoreCase(obj.bankPersonalId)
                        || DataUtil.strToInt(pgCode) <6100
                        || 6200 < DataUtil.strToInt(pgCode)){
                    jo.putString("desc", "Không tìm thấy số người được giới thiệu trên bảng phones");
                    callback.handle(jo);
                    return;
                }
                VcbCommon.requestGiftMomoForBByPG(vertx
                                                    ,DataUtil.strToInt(phoneB)
                                                    ,pgCode
                                                    ,1000000
                                                    ,1
                                                    ,tranId
                                                    ,obj.bankPersonalId
                                                    ,obj.bank_code
                                                    ,true,new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject jsonObject) {
                        callback.handle(jsonObject);
                    }
                });
            }
        });
    }
}
