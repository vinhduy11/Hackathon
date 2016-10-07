package com.mservice.momo.data;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.DBAction;
import com.mservice.momo.data.model.DBMsg;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 3/6/14.
 */
public class TransMySQLDb implements TransDb {

    public static final String PREFIX_LOG = "MySQL_Tran ";
    private EventBus eventBus;
    private Vertx vertx;
    private Logger logger;
    private JsonObject config;

    public TransMySQLDb(Vertx vertx, EventBus eb, Logger log, JsonObject glbConfig) {
        this.vertx = vertx;
        this.eventBus = eb;
        this.logger = log;
        this.config = glbConfig;
    }

    public void delRows(final int number, final ArrayList<Long> tranIds, final Handler<Boolean> callback) {

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(StringUtils.join(tranIds, ","));

        DBMsg dbMsg = new DBMsg(number, DBAction.Tran.DEL_ROWS_TRAN);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "delTranRows is " + (dbResMsg.err == 0) + " number=" + number);
                callback.handle((dbResMsg.err == 0));
            }
        });
    }

    public void getStatisticTranPerDay(final int number, final Handler<JsonObject> callback) {

        JsonObject jo = Misc.getStartAndEndCurrentDateInMilliseconds();
        long beginDate = jo.getLong(Misc.BeginDateInMilliSec, 0);
        long endDate = jo.getLong(Misc.EndDateInMilliSec, 0);

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(beginDate);
        params.add(endDate);
        params.add(10000);

        DBMsg dbMsg = new DBMsg(number, DBAction.Tran.GET_STATISTIC_TRAN_PER_DAY);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                int tranCount = 0;
                long totalIn = 0;
                long totalOut = 0;
                logger.info(PREFIX_LOG + "getStatisticTranPerDay is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {

                        for (int i = 0; i < results.size(); i++) {
                            JsonArray jsonModel = results.get(i);
                            JsonObject jo = TranObj.fromArrayToJsonObj(jsonModel);
                            int io = jo.getInteger(colName.TranDBCols.IO, 0);
                            if (io == 1) {
                                totalIn += jo.getLong(colName.TranDBCols.AMOUNT, 0);
                            } else {
                                totalOut += jo.getLong(colName.TranDBCols.AMOUNT, 0);
                            }
                        }
                    }
                }
                JsonObject j = new JsonObject();
                j.putNumber("value", Math.max(totalIn, totalOut));
                j.putNumber("count", tranCount);
                callback.handle(j);
            }
        });
    }

    public void upsertTranOutSideNew(final int number, final JsonObject obj, final Handler<Boolean> callback) {

        final JsonObject tranObjJson = obj.copy();
        final Common.BuildLog log = new Common.BuildLog(logger);
        tranObjJson.removeField(colName.TranDBCols.OWNER_NUMBER);
        tranObjJson.removeField(colName.TranDBCols.OWNER_NAME);
        tranObjJson.removeField(colName.TranDBCols.PARRENT_TRAN_TYPE);
        tranObjJson.removeField(Const.AppClient.Html);
        tranObjJson.removeField(Const.AppClient.Qrcode);

        //neu co comment thi lay, khong thi remove luon
        if ("".equalsIgnoreCase(tranObjJson.getString(colName.TranDBCols.COMMENT, ""))) {
            tranObjJson.removeField(colName.TranDBCols.COMMENT);
        }

        tranObjJson.putBoolean(colName.TranDBCols.DELETED, false);

        tranObjJson.putNumber(colName.TranDBCols.PHONE, number);

        final long tranId = tranObjJson.getLong(colName.TranDBCols.TRAN_ID, System.currentTimeMillis());
        tranObjJson.putNumber(colName.TranDBCols.TRAN_ID, tranId);

        final String createdId = tranObjJson.getString(colName.TranDBCols._ID, UUID.randomUUID().toString());
        tranObjJson.putString(colName.TranDBCols._ID, createdId);

        final TranObj tranObj = new TranObj(tranObjJson);
        JsonArray params = tranObj.toJsonArray();

        DBMsg dbMsg = new DBMsg(number, tranId, DBAction.Tran.UPSERT_TRAN_OUTSIDE);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "upsertTranOutSideNew is " + (dbResMsg.err == 0) + " tranId=" + tranId +
                        " number=" + number);
                boolean isUpdate = false;
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    isUpdate = results.size() > 0 ? (boolean) results.get(0) : false;
                }
                callback.handle(isUpdate);
            }
        });
    }

    public void upsertTran(final int number, final JsonObject tranObjJson, final Handler<TranObj> callback) {
        final long tranId = tranObjJson.getLong(colName.TranDBCols.TRAN_ID, 0L);

        final Common.BuildLog log = new Common.BuildLog(logger);
        //remove some fields no need to store in DB
        tranObjJson.removeField(colName.TranDBCols.PARRENT_TRAN_TYPE);
        tranObjJson.removeField(colName.TranDBCols.OWNER_NUMBER);
        tranObjJson.removeField(colName.TranDBCols.OWNER_NAME);
        tranObjJson.removeField(colName.TranDBCols.IS_M2NUMBER);
        tranObjJson.removeField(colName.TranDBCols.FORCE_COMMENT);
        tranObjJson.removeField(colName.TranDBCols.DESCRIPTION);

        tranObjJson.putBoolean(colName.TranDBCols.DELETED, false);

        tranObjJson.putNumber(colName.TranDBCols.PHONE, number);

        final String createdId = UUID.randomUUID().toString();
        tranObjJson.putString(colName.TranDBCols._ID, createdId);

        final TranObj tranObj = new TranObj(tranObjJson);

        DBMsg dbMsg = new DBMsg(number, tranId, DBAction.Tran.INSERT_TRAN);
        dbMsg.params = tranObj.toJsonArray().toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "upsertTran is " + (dbResMsg.err == 0) + " number=" + number +
                        " tranId=" + tranId);
                if (dbResMsg.err == 0) {
                    logger.debug(String.format("Saved %d tran id: %s.", number, createdId));
                    callback.handle(tranObj);
                } else {
                    logger.debug(String.format("Save %d tran result: fail", number));
                    callback.handle(null);
                }
            }
        });
    }

    public void find(final int number, final long fromFinishTime, boolean isStore, final Handler<ArrayList<TranObj>> callback) {

        //du lieu khong phai tu app xuong roi, chac bi tan cong ha ?????
        if (fromFinishTime < 0) {
            logger.info("Sao lai co du lieu nay, fromFinishTime: " + fromFinishTime);
            callback.handle(null);
            return;
        }

        //quet them nhung giao dich ngoai backend
        long finishTime = fromFinishTime - TranObj.TRAN_SYNC_PREV_MINUTES * 60 * 1000;
        DBMsg dbMsg = new DBMsg(number, DBAction.Tran.FIND_TRAN);

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(finishTime);
        params.add(fromFinishTime);
        params.add(1000);
        if (isStore)
            params.add("ASC");
        else
            params.add("DESC");
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                ArrayList<TranObj> finalResult = null;
                logger.info(PREFIX_LOG + "find is " + (dbResMsg.err == 0) + " number=" + number + " fromFinishTime" + fromFinishTime);
                if ((dbResMsg.err == 0)) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        finalResult = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray jsonModel = results.get(i);
                            JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                            TranObj model = new TranObj(ob);
                            finalResult.add(model);
                        }
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    public void countWithFilter(final int phoneNumber, Long startTime,
                                Long endTime, Integer status, List<Integer> types, final Handler<Long> callback) {

        JsonArray params = new JsonArray();
        params.add(phoneNumber);
        params.add(status);
        params.add(startTime);
        params.add(endTime);
        params.add(StringUtils.join(types, ","));

        final DBMsg dbMsg = new DBMsg(phoneNumber, DBAction.Tran.COUNT_WITH_FILTER);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                Long count = 0L;
                logger.info(PREFIX_LOG + "countWithFilter is " + (dbResMsg.err == 0) + " phoneNumber=" + phoneNumber);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        count = results.get(0);
                    }
                }
                callback.handle(count);
            }
        });
    }

    public void getTransaction(final int phoneNumber, int pageSize, int pageNumber, Long startTime,
                               Long endTime, Integer status, List<Integer> types, final Handler<ArrayList<TranObj>> callback) {

        int skip = (pageNumber - 1) * pageSize;
        JsonArray params = new JsonArray();
        params.add(phoneNumber);
        params.add(status);
        params.add(startTime);
        params.add(endTime);
        params.add(StringUtils.join(types, ","));
        params.add(skip + 1);
        params.add(pageSize);

        final DBMsg dbMsg = new DBMsg(phoneNumber, DBAction.Tran.GET_TRAN_PAGING);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                ArrayList<TranObj> finalResult = null;
                logger.info(PREFIX_LOG + "getTransaction is " + (dbResMsg.err == 0) + " phoneNumber=" + phoneNumber);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        finalResult = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray jsonModel = results.get(i);
                            JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                            TranObj model = new TranObj(ob);
                            finalResult.add(model);
                        }
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    public void getTransactionDetail(final int phoneNumber, final long transactionId, final Handler<TranObj> callback) {

        JsonArray params = new JsonArray();
        params.add(phoneNumber);
        params.add(transactionId);

        final DBMsg dbMsg = new DBMsg(phoneNumber, transactionId, DBAction.Tran.GET_TRANSACTION_DETAIL);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                TranObj finalResult = null;
                logger.info(PREFIX_LOG + "getTransactionDetail is " + (dbResMsg.err == 0) + " phoneNumber=" + phoneNumber +
                        " transactionId=" + transactionId);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        JsonArray jsonModel = results.get(0);
                        JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                        finalResult = new TranObj(ob);
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    public void updateTranStatusNew(final int number
            , final long tranId
            , int status
            , final Handler<Boolean> callback) {

        JsonArray params = new JsonArray();
        params.add(status);
        params.add(System.currentTimeMillis());
        if (status != TranObj.STATUS_OK) {
            params.add(TranObj.STATUS_FAIL);
        } else {
            params.add(0);
        }
        params.add(number);
        params.add(tranId);

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.UPDATE_TRAN_STATUS);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "updateTranStatusNew is " + (dbResMsg.err == 0) + " number=" + number +
                        " tranId=" + tranId);
                callback.handle(dbResMsg.err == 0);
            }
        });
    }

    public void updateTranStatus(final int number
            , final long tranId
            , int status
            , final Handler<TranObj> callback) {

        JsonArray params = new JsonArray();
        params.add(status);
        params.add(System.currentTimeMillis());
        if (status != TranObj.STATUS_OK) {
            params.add(TranObj.STATUS_FAIL);
        } else {
            params.add(0);
        }
        params.add(number);
        params.add(tranId);

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.UPDATE_TRAN_STATUS);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                logger.info(PREFIX_LOG + "updateTranStatusNew is " + (dbResMsg.err == 0) + " number=" + number +
                        " tranId=" + tranId);
                if (dbResMsg.err == 0) {
                    getTransactionDetail(number, tranId, new Handler<TranObj>() {
                        @Override
                        public void handle(TranObj tranObj) {
                            callback.handle(tranObj);
                        }
                    });
                } else {
                    callback.handle(null);
                }
            }
        });
    }

    public void getTranExistOnServer(final int number, final ArrayList<Long> cIds, final Handler<ArrayList<Long>> callback) {

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(StringUtils.join(cIds, ","));

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.GET_TRAN_EXIST_ON_SERVER);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                ArrayList<Long> list = null;
                logger.info(PREFIX_LOG + "getTranExistOnServer is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        list = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray jsonModel = results.get(0);
                            list.add((Long) jsonModel.get(0));
                        }
                    }
                }
                callback.handle(list);
            }
        });
    }

    public void getTranById(final int phone, final long tranId, final Handler<TranObj> callback) {

        getTransactionDetail(phone, tranId, new Handler<TranObj>() {
            @Override
            public void handle(TranObj tranObj) {
                logger.info(PREFIX_LOG + "getTranById is " + (tranObj != null) + " phone=" + phone + " tranId=" + tranId);
                callback.handle(tranObj);
            }
        });
    }

    public void countAgentTran(final int phoneNumber, final Handler<Long> callback) {

        JsonArray params = new JsonArray();
        params.add(phoneNumber);

        final DBMsg dbMsg = new DBMsg(phoneNumber, DBAction.Tran.COUNT_AGENT_TRAN);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                Long count = 0L;
                logger.info(PREFIX_LOG + "countAgentTran is " + (dbResMsg.err == 0) + " phoneNumber=" + phoneNumber);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        count = results.get(0);
                    }
                }
                callback.handle(count);
            }
        });
    }

    public void getTransInList(final int number, ArrayList<Long> cIds, final Handler<ArrayList<TranObj>> callback) {

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(StringUtils.join(cIds, ","));
        params.add(1000);

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.GET_TRAN_IN_LIST);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                ArrayList<TranObj> list = null;
                logger.info(PREFIX_LOG + "getTransInList is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        list = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray jsonModel = results.get(i);
                            JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                            TranObj model = new TranObj(ob);
                            list.add(model);
                        }
                    }
                }
                callback.handle(list);
            }
        });
    }

    public void sumTranInCurrentMonth(final int number
            , ArrayList<Integer> listTranType
            , final Handler<Long> callback) {

        Misc.TimeOfMonth timeOfMonth = Misc.getBeginEndTimeOfMonth(System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L);

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(timeOfMonth.BeginTime);
        params.add(timeOfMonth.EndTime);
        params.add(StringUtils.join(listTranType, ","));
        params.add(10000);

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.SUM_TRAN_CURR_MONTH);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                Long totalValue = 0L;
                logger.info(PREFIX_LOG + "sumTranInCurrentMonth is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        totalValue = results.get(0);
                    }
                }
                callback.handle(totalValue);
            }
        });
    }

    public void findOneVcbTran(final int number, String bankCode, long promoFromDate, long promoToDate, final Handler<TranObj> callback) {

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(promoFromDate);
        params.add(promoToDate);
        params.add(MomoProto.TranHisV1.TranType.BANK_IN_VALUE);
        params.add(bankCode);

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.FIND_ONE_VCB_TRAN);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                TranObj finalResult = null;
                logger.info(PREFIX_LOG + "findOneVcbTran is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        JsonArray jsonModel = results.get(0);
                        JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                        finalResult = new TranObj(ob);
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    public void findOneByTranType(final int number, int tranType, long upperTime, final Handler<Integer> callback) {

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(upperTime);
        params.add(tranType);

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.FIND_ONE_FOR_TRANTYPE);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                int count = 0;
                logger.info(PREFIX_LOG + "findOneByTranType is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        count = 1;
                    }
                }
                callback.handle(count);
            }
        });
    }

    public void findOneInBillPayAndPhim(final int number, long promoFromDate, long tranFinishTime, final Handler<TranObj> callback) {

        List<Integer> listTranType = new ArrayList<>();
        listTranType.add(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE);
        listTranType.add(MomoProto.TranHisV1.TranType.PHIM123_VALUE);

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(promoFromDate);
        params.add(tranFinishTime);
        params.add(StringUtils.join(listTranType, ","));

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.FIND_ONE_BILLPAY_AND_PHIM);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                TranObj finalResult = null;
                logger.info(PREFIX_LOG + "findOneInBillPayAndPhim is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        JsonArray jsonModel = results.get(0);
                        JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                        finalResult = new TranObj(ob);
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    public void getTheThreeLastTranOfNumber(final int number, final Handler<JsonArray> callback) {

        JsonArray params = new JsonArray();
        params.add(number);
        params.add(3);

        final DBMsg dbMsg = new DBMsg(number, DBAction.Tran.GET_LAST_TRAN);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                JsonArray list = null;
                logger.info(PREFIX_LOG + "getTheThreeLastTranOfNumber is " + (dbResMsg.err == 0) + " number=" + number);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray();
                    if (results != null && results.size() > 0) {
                        list = new JsonArray();
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray jsonModel = results.get(i);
                            JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                            list.add(ob);
                        }
                    }
                }
                callback.handle(list);
            }
        });
    }

    public void getTranWithoutType(final int phoneNumber, List<Integer> excludeTranTypes, final Handler<ArrayList<TranObj>> callback) {

        JsonArray params = new JsonArray();
        params.add(phoneNumber);
        params.add(StringUtils.join(excludeTranTypes, ","));
        params.add(100000);

        final DBMsg dbMsg = new DBMsg(phoneNumber, DBAction.Tran.GET_TRAN_WITHOUT_TYPE);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                ArrayList<TranObj> list = null;
                logger.info(PREFIX_LOG + "getTranWithoutType is " + (dbResMsg.err == 0) + " phoneNumber=" + phoneNumber);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        list = new ArrayList<TranObj>();
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray jsonModel = results.get(i);
                            JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                            TranObj model = new TranObj(ob);
                            list.add(model);
                        }
                    }
                }
                callback.handle(list);
            }
        });
    }

    public void getTranByBillId(final int phone, final String billId, final Handler<TranObj> callback) {

        JsonArray params = new JsonArray();
        params.add(phone);
        params.add(billId);

        final DBMsg dbMsg = new DBMsg(phone, DBAction.Tran.GET_TRAN_BY_BILL);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                TranObj finalResult = null;
                logger.info(PREFIX_LOG + "getTranByBillId is " + (dbResMsg.err == 0) + " phone=" + phone);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        JsonArray jsonModel = results.get(0);
                        JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                        finalResult = new TranObj(ob);
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    public void getTranByPartnerInvNo(final int phone, final String partnerInvNo, final Handler<TranObj> callback) {

        JsonArray params = new JsonArray();
        params.add(phone);
        params.add(partnerInvNo);

        final DBMsg dbMsg = new DBMsg(phone, DBAction.Tran.GET_TRAN_BY_BILL);
        dbMsg.params = params.toList();

        Misc.makeDBRequest(vertx, logger, config, DBFactory.Source.TRAN_PATH, dbMsg, new Handler<DBMsg>() {
            @Override
            public void handle(DBMsg dbResMsg) {
                TranObj finalResult = null;
                logger.info(PREFIX_LOG + "getTranByPartnerInvNo is " + (dbResMsg.err == 0) + " phone=" + phone);
                if (dbResMsg.err == 0) {
                    JsonArray results = new JsonArray(dbResMsg.res);
                    if (results != null && results.size() > 0) {
                        JsonArray jsonModel = results.get(0);
                        JsonObject ob = TranObj.fromArrayToJsonObj(jsonModel);
                        finalResult = new TranObj(ob);
                    }
                }
                callback.handle(finalResult);
            }
        });
    }

    @Override
    public void getTheTwoLastTranOfNumber(int number, Handler<JsonArray> callback) {
        // not use
    }

    @Override
    public void countTop2WithFilter(int phoneNumber, Long startTime, Long endTime, Integer error, ArrayList<Integer> trantypes, int limit, Handler<Long> callback) {
        // not use
    }

    @Override
    public void searchWithFilter(int phoneNumber, JsonObject filter, Handler<ArrayList<TranObj>> callback) {
        // not use
    }
}
