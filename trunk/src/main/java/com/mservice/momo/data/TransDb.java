package com.mservice.momo.data;

import com.mservice.momo.vertx.models.TranObj;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 3/6/14.
 */
public interface TransDb {

    void delRows(final int number, final ArrayList<Long> tranIds, final Handler<Boolean> callback);

    void getStatisticTranPerDay(final int number, final Handler<JsonObject> callback);

    void upsertTranOutSideNew(final int number, JsonObject obj, final Handler<Boolean> callback);

    void upsertTran(final int number, final JsonObject tranObjJson, final Handler<TranObj> callback);

    void find(int number, final long fromFinishTime, boolean isStore, final Handler<ArrayList<TranObj>> callback);

    void countWithFilter(int phoneNumber, Long startTime,
                         Long endTime, Integer status, List<Integer> types, final Handler<Long> callback);

    void getTransaction(int phoneNumber, int pageSize, int pageNumber, Long startTime,
                        Long endTime, Integer status, List<Integer> types, final Handler<ArrayList<TranObj>> callback);

    void getTransactionDetail(int phoneNumber, long transactionId, final Handler<TranObj> callback);

    void updateTranStatusNew(final int number
            , final long tranId
            , int status
            , final Handler<Boolean> callback);

    void updateTranStatus(final int number
            , final long tranId
            , int status
            , final Handler<TranObj> callback);

    void getTranExistOnServer(final int number, final ArrayList<Long> cIds, final Handler<ArrayList<Long>> callback);

    void getTranById(final int phone, final long tranId, final Handler<TranObj> callback);

    void countAgentTran(final int phoneNumber, final Handler<Long> callback);

    void getTransInList(final int number, ArrayList<Long> cIds, final Handler<ArrayList<TranObj>> callback);

    void sumTranInCurrentMonth(final int number
            , ArrayList<Integer> listTranType
            , final Handler<Long> callback);

    //vcb.start
    void findOneVcbTran(int number, String bankCode, long promoFromDate, long promoToDate, final Handler<TranObj> callback);

    void findOneByTranType(int number, int tranType, long upperTime, final Handler<Integer> callback);

    void findOneInBillPayAndPhim(int number, long promoFromDate, long tranFinishTime, final Handler<TranObj> callback);

    void getTheTwoLastTranOfNumber(int number, final Handler<JsonArray> callback);

    void getTheThreeLastTranOfNumber(int number, final Handler<JsonArray> callback);

    void countTop2WithFilter(int phoneNumber, Long startTime,
                             final Long endTime, Integer error, ArrayList<Integer> trantypes, int limit, final Handler<Long> callback);

    void searchWithFilter(int phoneNumber, JsonObject filter, final Handler<ArrayList<TranObj>> callback);

    void getTranByBillId(final int phone, final String billId, final Handler<TranObj> callback);

    void getTranByPartnerInvNo(final int phone, final String partnerInvNo, final Handler<TranObj> callback);
}
