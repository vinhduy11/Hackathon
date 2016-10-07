package com.mservice.momo.data;

import com.mservice.momo.data.model.MongoKeyWords;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Misc;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by User on 2/27/14.
 */
public class AgentsDb {
    EventBus eventBus;
    Logger logger;
    CoreLastTime coreLastTime;

    public AgentsDb(EventBus eb, Logger logger) {
        eventBus = eb;
        this.logger = logger;
        coreLastTime = new CoreLastTime(eb, logger);
    }

    private static String buildSearchRegex(String searchValueLow) {
        searchValueLow = searchValueLow.replaceAll(" ", "(:|\\\\.|\\\\s)*");
        return searchValueLow;
    }

    private static String completedKeyword(String searchValue) {
        String searchReg = StringUtils.isNotEmpty(searchValue) ? Misc.removeAccent(searchValue.trim()).replaceAll("[\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u0306|\u0306|\u0306\u0301|\u0306\u0301|\u0306\u0300|\u0306\u0300|\u0306\u0309|\u0306\u0309|\u0306\u0303|\u0306\u0303|\u0306\u0323|\u0306\u0323|\u0302|\u0302|\u0302\u0301|\u0302\u0301|\u0302\u0300|\u0302\u0300|\u0302\u0309|\u0302\u0309|\u0302\u0303|\u0302\u0303|\u0302\u0323|\u0302\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u0302|\u0302|\u0302\u0301|\u0302\u0301|\u0302\u0300|\u0302\u0300|\u0302\u0309|\u0302\u0309|\u0302\u0303|\u0302\u0303|\u0302\u0323|\u0302\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u031B|\u031B|\u031B\u0301|\u031B\u0301|\u031B\u0300|\u031B\u0300|\u031B\u0309|\u031B\u0309|\u031B\u0303|\u031B\u0303|\u031B\u0323|\u031B\u0323|\u0302|\u0302|\u0302\u0301|\u0302\u0301|\u0302\u0300|\u0302\u0300|\u0302\u0309|\u0302\u0309|\u0302\u0303|\u0302\u0303|\u0302\u0323|\u0302\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u031B|\u031B|\u031B\u0301|\u031B\u0301|\u031B\u0300|\u031B\u0300|\u031B\u0309|\u031B\u0309|\u031B\u0303|\u031B\u0303|\u031B\u0323|\u031B\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323]", "").toLowerCase().replaceAll("[^a-zA-Z0-9^/]", " ").replaceAll("\\s+", " ") : "";
        searchReg = searchReg.toLowerCase().replaceAll("(tp)*(\\.|:|\\s)*hcm", "thanh pho ho chi minh");
        searchReg = searchReg.toLowerCase().replaceAll("(tp)*(\\.|:|\\s)*hn", "ha noi");

        searchReg = searchReg.replaceAll("(TP|tp|Tp)(\\.|:)+", "(thanh pho|tp) ");
        searchReg = searchReg.replaceAll("(TX|tx|Tx)(\\.|:)+", "(thi xa|tx) ");
        searchReg = searchReg.replaceAll("(p|P)(\\.|:)+", "phuong ");
        searchReg = searchReg.replaceAll("(tt)(\\.|:)+", "(thi tran|tt) ");
        searchReg = searchReg.replaceAll("(q|Q)(\\.|:)+", "quan ");

        searchReg = searchReg.toLowerCase().replaceAll("(khu cong nghiep|kcn)", "(khu cong nghiep|kcn)");
        searchReg = searchReg.toLowerCase().replaceAll("(khu che xuat|kcx)", "(khu che xuat|kcx)");
        searchReg = searchReg.toLowerCase().replaceAll("(khu pho|kp)", "(khu pho|kp)");
        searchReg = searchReg.toLowerCase().replaceAll("(khu dan cu|kdc)", "(khu dan cu|kdc)");
        searchReg = searchReg.toLowerCase().replaceAll("(quoc lo|ql)", "(quoc lo|ql)");
        searchReg = searchReg.toLowerCase().replaceAll("(tinh lo|tl)", "(tinh lo|tl)");
        searchReg = searchReg.toLowerCase().replaceAll("(cua hang|ch\\s)", "(cua hang\\\\s\\*|ch\\\\s)");
        searchReg = searchReg.toLowerCase().replaceAll("(dien thoai|dt)", "(dien thoai|dt)\\\\s\\*");
        searchReg = searchReg.toLowerCase().replaceAll("(di dong|dd)", "\\\\s\\*(di dong|dd)");
        searchReg = searchReg.toLowerCase().replaceAll("(dgd|diem giao dich)", "(dgd|diem giao dich)");
        searchReg = searchReg.toLowerCase().replaceAll("(cty|cong ty)", "(cty|cong ty)");

        return searchReg;
    }

    public static void main(String[] args) {
        System.out.println(",,,,".matches("[^a-zA-Z0-9]"));
    }

    private static String normalize(String s) {
        if (s == null)
            return "";
        return Misc.removeAccent(s).replaceAll("\\s{2,}", " ").replaceAll(":", "").replaceAll("[\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u0306|\u0306|\u0306\u0301|\u0306\u0301|\u0306\u0300|\u0306\u0300|\u0306\u0309|\u0306\u0309|\u0306\u0303|\u0306\u0303|\u0306\u0323|\u0306\u0323|\u0302|\u0302|\u0302\u0301|\u0302\u0301|\u0302\u0300|\u0302\u0300|\u0302\u0309|\u0302\u0309|\u0302\u0303|\u0302\u0303|\u0302\u0323|\u0302\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u0302|\u0302|\u0302\u0301|\u0302\u0301|\u0302\u0300|\u0302\u0300|\u0302\u0309|\u0302\u0309|\u0302\u0303|\u0302\u0303|\u0302\u0323|\u0302\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u031B|\u031B|\u031B\u0301|\u031B\u0301|\u031B\u0300|\u031B\u0300|\u031B\u0309|\u031B\u0309|\u031B\u0303|\u031B\u0303|\u031B\u0323|\u031B\u0323|\u0302|\u0302|\u0302\u0301|\u0302\u0301|\u0302\u0300|\u0302\u0300|\u0302\u0309|\u0302\u0309|\u0302\u0303|\u0302\u0303|\u0302\u0323|\u0302\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323|\u031B|\u031B|\u031B\u0301|\u031B\u0301|\u031B\u0300|\u031B\u0300|\u031B\u0309|\u031B\u0309|\u031B\u0303|\u031B\u0303|\u031B\u0323|\u031B\u0323|\u0301|\u0301|\u0300|\u0300|\u0309|\u0309|\u0303|\u0303|\u0323|\u0323]", "").toLowerCase();
    }

    private static boolean checkSearchComplex(String searchReg, List<String> arr) {
        searchReg = Misc.removeAccent(searchReg).toLowerCase();
        List<String> tmp = Arrays.asList(searchReg.split("\\s+"));
        for (int i = 0; i < tmp.size(); i = i + 3) {
            try {
                arr.add(tmp.get(i) + " " + tmp.get(i + 1) + " " + tmp.get(i + 2));
            } catch (Exception e) {
            }
        }
        try {
            if (searchReg.matches("(.*)(duong|pho|hem|kcn|kcx|khu do thi):*(\\s.*)")) {
                return true;
            }
            if (searchReg.matches("(.*)(phuong|p(\\.)*|xa|thi tran|thon|ap):*(\\s.*)")) {
                return true;
            }
            if (searchReg.matches("(.*)(quan|q(\\.)*|huyen|thi xa|tx(\\.)*):*(\\s.*)")) {
                return true;
            }
            if (searchReg.matches("(.*)(thanh pho|tp(\\.)*):*(\\s.*)")) {
                return true;
            }
            if (searchReg.matches("(.*)(p|kcn|kcx|q|tx|tp)\\.(.*)")) {
                return true;
            }
            if (searchReg.matches("\\d+.*")) {
                return true;
            }
            if (searchReg.split("\\s").length >= 3) {
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static void doUpdateNormalize(JsonObject ob) {
        String s_name = normalize(ob.getString(colName.AgentDBCols.STORE_NAME));
        String cname = normalize(ob.getString(colName.AgentDBCols.CITY_NAME));
        String dname = normalize(ob.getString(colName.AgentDBCols.DISTRICT_NAME));
        String wname = normalize(ob.getString(colName.AgentDBCols.WARD_NAME));
        String add = normalize(ob.getString(colName.AgentDBCols.ADDRESS));
        String street = normalize(ob.getString(colName.AgentDBCols.STREET));
        ob.putString(colName.AgentDBCols.STORE_NAME_NOR, s_name);
        ob.putString(colName.AgentDBCols.CITY_NAME_NOR, cname.replaceAll("thanh pho|tinh", ""));
        ob.putString(colName.AgentDBCols.DISTRICT_NAME_NOR, dname.replaceAll("quan|huyen|thi xa|thanh pho", ""));
        ob.putString(colName.AgentDBCols.WARD_NAME_NOR, wname.replaceAll("phuong|xa|thi tran", ""));
        ob.putString(colName.AgentDBCols.ADDRESS_NOR, add);
        ob.putString(colName.AgentDBCols.STREET_NOR, street);
        ob.putString(colName.AgentDBCols.FULL_ADD_NOR, s_name + " " + add + " " + street + " " + wname + " " + dname + " " + cname);
    }

    public void countStoreByCode(int cid, Integer did, int pageSize, int pageNumber, final Handler<Long> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, "findWithFilter");
        query.putString("collection", "stores");

        int skip = (pageNumber - 1) * pageSize;
        int records = pageSize;
        query.putNumber("skip", skip);
        query.putNumber("limit", records);

        JsonObject matcher = new JsonObject();
        matcher.putNumber("cid", cid);
        if (did != null && did != 0)
            matcher.putNumber("did", did);


        matcher.putBoolean(colName.AgentDBCols.DELETED, false);
        matcher.putNumber(colName.AgentDBCols.STATUS, 0);


        query.putObject("matcher", matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Long count = event.body().getLong("count");
                callback.handle(count);
            }
        });
    }

    public void getStores(int cid, Integer did, int pageSize, int pageNumber, final Handler<List<StoreInfo>> callback) {
        List<StoreInfo> stores = new ArrayList<StoreInfo>();

        JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", "stores");

        int skip = (pageNumber - 1) * pageSize;
        int records = pageSize;
        query.putNumber("skip", skip);
        query.putNumber("limit", records);

        JsonObject matcher = new JsonObject();

        matcher.putBoolean(colName.AgentDBCols.DELETED, false);
        matcher.putNumber(colName.AgentDBCols.STATUS, 0);
        JsonArray orTDL = new JsonArray();
        matcher.putArray("$and", new JsonArray().add(new JsonObject().putArray("$or", orTDL)));
        orTDL.add(new JsonObject().putObject(colName.AgentDBCols.TDL, new JsonObject().putBoolean("$exists", false)));
        orTDL.add(new JsonObject().putObject(colName.AgentDBCols.TDL, new JsonObject().putBoolean("$exists", true))
                .putBoolean(colName.AgentDBCols.TDL, false));

        matcher.putNumber("cid", cid);
        if (did != null && did != 0)
            matcher.putNumber("did", did);

        // Chi show len DGD
        JsonObject agentType = new JsonObject();
        //0: ĐGD Thường: 3 lần /tháng
        //1: ĐGD tự thanh khoản : sửa thành 5 lần/tháng
        //2: MOMO Shop: 3 lần /tháng
        //4: ĐGD Sale tự thanh khoản : 5 lần / tháng
        //3 : ĐGD CNTT nhé
        agentType.putArray("$in", new JsonArray().add(0).add(1).add(4));
        matcher.putObject(colName.AgentDBCols.AGENT_TYPE, agentType);

        query.putObject("matcher", matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray results = null;
                results = message.body().getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray());
                ArrayList<StoreInfo> storeInfos = new ArrayList<StoreInfo>();
                for (int i = 0; i < results.size(); i++) {
                    storeInfos.add(new StoreInfo((JsonObject) results.get(i)));
                }
                callback.handle(storeInfos);
            }
        });
    }

    public void getStores(double lng, double lat, int districtId, int cityId, int limit, final Handler<List<StoreInfo>> callback) {
        //boolean isSearchByLoc = false;
        JsonObject mchrObj = new JsonObject();
        mchrObj.putBoolean(colName.AgentDBCols.DELETED, false);

        if (lng > 0 && lat > 0) {
            //isSearchByLoc = true;
            //{ type: "Point", coordinates: [ 10.801133, 106.660507 ] }
            JsonObject locObj = new JsonObject();
            JsonObject nearObj = new JsonObject();
            JsonObject geomObj = new JsonObject();
            JsonArray coorObj = new JsonArray();

            coorObj.addNumber(lng).addNumber(lat);
            geomObj.putString("type", "Point");
            geomObj.putArray("coordinates", coorObj);

            nearObj.putObject("$geometry", geomObj);
            locObj.putObject("$near", nearObj);
            mchrObj.putObject(colName.AgentDBCols.LOCATION, locObj);
        } else {
            //isSearchByLoc = false;
            if (districtId > 0) {
                mchrObj.putNumber("did", districtId);
            }

            if (cityId > 0) {
                mchrObj.putNumber("cid", cityId);
            }
        }

        JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", "stores");
        query.putObject("matcher", mchrObj);
        if (limit > 20) {
            limit = 20;
        }
        query.putNumber("limit", limit);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray results = null;
                results = message.body().getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray());
                ArrayList<StoreInfo> storeInfos = new ArrayList<StoreInfo>();
                for (int i = 0; i < results.size(); i++) {
                    storeInfos.add(new StoreInfo((JsonObject) results.get(i)));
                }
                callback.handle(storeInfos);
            }
        });
    }

    public void updateNormalize(final Handler<JsonArray> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        JsonObject matcher = new JsonObject();

        query.putObject(MongoKeyWords.MATCHER, matcher);
        query.putNumber(MongoKeyWords.BATCH_SIZE, 3000000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                JsonArray newArr = new JsonArray();
                for (Object o : results) {
                    JsonObject ob = (JsonObject) o;
                    if (ob.getString(colName.AgentDBCols.STORE_NAME_NOR) == null || StringUtils.isEmpty(ob.getString(colName.AgentDBCols.STORE_NAME_NOR))) {
                        ob.removeField(colName.AgentDBCols._ID);
                        doUpdateNormalize(ob);
                        newArr.add(ob);
                    }
                }
                callback.handle(newArr);
            }
        });
    }

    public void getStoresNew(
            double lat,
            double lng,
            String searchValue,
            int pageNumber,
            int pageSize,
            final Handler<List<StoreInfo>> callback) {

        JsonObject mchrObj = new JsonObject();
        mchrObj.putBoolean(colName.AgentDBCols.DELETED, false);
        mchrObj.putNumber(colName.AgentDBCols.STATUS, 0);

        JsonArray orTDL = new JsonArray();
        mchrObj.putArray("$and", new JsonArray().add(new JsonObject().putArray("$or", orTDL)));
        orTDL.add(new JsonObject().putObject(colName.AgentDBCols.TDL, new JsonObject().putBoolean("$exists", false)));
        orTDL.add(new JsonObject().putObject(colName.AgentDBCols.TDL, new JsonObject().putBoolean("$exists", true))
                .putBoolean(colName.AgentDBCols.TDL, false));

        JsonObject agentType = new JsonObject();
        //0: ĐGD Thường: 3 lần /tháng
        //1: ĐGD tự thanh khoản : sửa thành 5 lần/tháng
        //2: MOMO Shop: 3 lần /tháng
        //4: ĐGD Sale tự thanh khoản : 5 lần / tháng
        //3 : ĐGD CNTT nhé
        agentType.putArray("$in", new JsonArray().add(0).add(1).add(4));
        mchrObj.putObject(colName.AgentDBCols.AGENT_TYPE, agentType);

        if (lng > 0 && lat > 0) {
            JsonObject locObj = new JsonObject();
            JsonObject nearObj = new JsonObject();
            JsonObject geomObj = new JsonObject();
            JsonArray coorObj = new JsonArray();

            coorObj.addNumber(lng).addNumber(lat);
            geomObj.putString("type", "Point");
            geomObj.putArray("coordinates", coorObj);

            nearObj.putObject("$geometry", geomObj);
            locObj.putObject("$near", nearObj);
            mchrObj.putObject(colName.AgentDBCols.LOCATION, locObj);
        }

        // match
        String searchReg = completedKeyword(searchValue);
        if (StringUtils.isNotEmpty(searchReg)) {

            JsonArray or = new JsonArray();
            mchrObj.putArray("$or", or);

            String searchLCRegex = buildSearchRegex(searchReg);
            logger.info("searchLCRegex: " + searchLCRegex);
            or.add(new JsonObject().putObject(colName.AgentDBCols.STORE_NAME_NOR, new JsonObject().putString("$regex", ".*((?i)" + searchLCRegex + ").*")));
            or.add(new JsonObject().putObject(colName.AgentDBCols.CITY_NAME_NOR, new JsonObject().putString("$regex", ".*((?i)" + searchLCRegex + ").*")));
            or.add(new JsonObject().putObject(colName.AgentDBCols.DISTRICT_NAME_NOR, new JsonObject().putString("$regex", ".*((?i)" + searchLCRegex + ").*")));
            or.add(new JsonObject().putObject(colName.AgentDBCols.WARD_NAME_NOR, new JsonObject().putString("$regex", ".*((?i)" + searchLCRegex + ").*")));
            or.add(new JsonObject().putObject(colName.AgentDBCols.ADDRESS_NOR, new JsonObject().putString("$regex", ".*((?i)" + searchLCRegex + ").*")));
            or.add(new JsonObject().putObject(colName.AgentDBCols.STREET_NOR, new JsonObject().putString("$regex", ".*((?i)" + searchLCRegex + ").*")));

            List<String> arr = new ArrayList<>();
            if (checkSearchComplex(searchReg, arr)) {
                or.add(new JsonObject().putObject(colName.AgentDBCols.FULL_ADD_NOR, new JsonObject().putString("$regex", ".*((?i)" + searchLCRegex + ").*")));
                or.add(new JsonObject().putObject(colName.AgentDBCols.FULL_ADD_NOR, new JsonObject().putString("$regex", ".*((?i)" + Misc.removeAccent(searchValue).toLowerCase() + ").*")));
//                for (String e : arr) {
//                    or.add(new JsonObject().putObject(colName.AgentDBCols.FULL_ADD_NOR, new JsonObject().putString("$regex", ".*((?i)" + e + ").*")));
//                }
            }
            if (searchReg.matches("\\d+")) {
                if (searchReg.length() == 11 || searchReg.length() == 10) {
                    //or.add(new JsonObject().putString("mmphone", searchReg));
                    or.add(new JsonObject().putString("phone", searchReg));
                } else {
                    //or.add(new JsonObject().putObject("mmphone", new JsonObject().putString("$regex", ".*((?i)" + searchReg + ").*")));
                    or.add(new JsonObject().putObject("phone", new JsonObject().putString("$regex", ".*((?i)" + searchReg + ").*")));
                }
            }
        }

        JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", "stores");
        query.putObject("matcher", mchrObj);

        int skip = (pageNumber - 1) * pageSize;
        int records = pageSize;
        query.putNumber("limit", records);
        query.putNumber("skip", skip);
        logger.info("getStoreNew " + query.toString());

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonArray results = null;
                results = message.body().getArray(MongoKeyWords.RESULT_ARRAY, new JsonArray());
                ArrayList<StoreInfo> storeInfos = new ArrayList<StoreInfo>();
                for (int i = 0; i < results.size(); i++) {
                    storeInfos.add(new StoreInfo((JsonObject) results.get(i)));
                }
                callback.handle(storeInfos);
            }
        });
    }

    public void upsertLocation(final JsonObject jo, final Handler<Boolean> callback) {

        upsertLoc(jo, new Handler<Boolean>() {
            @Override
            public void handle(Boolean aBoolean) {
                callback.handle(aBoolean);
            }
        });
    }

    public void getOneAgent(String agentPhone, String callby, final Handler<StoreInfo> callback) {

        logger.info("getOneAgent - callby " + callby + "for number " + agentPhone);
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.AgentDBCols.MOMO_PHONE, agentPhone);
        matcher.putNumber(colName.AgentDBCols.STATUS, 0);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonObject jsonObject = message.body().getObject(MongoKeyWords.RESULT, null);
                logger.debug("getOneAgent result: " + jsonObject);
                StoreInfo storeInfo = null;
                if (jsonObject != null) {
                    storeInfo = new StoreInfo(jsonObject);
                }

                callback.handle(storeInfo);
            }
        });
    }


    public void getStores(long lastUpdateTime, final Handler<ArrayList<StoreInfo>> callback) {

        if (lastUpdateTime < 0) {
            logger.info("Sao lai truyen xuong lastUpdateTime am: " + lastUpdateTime);
            callback.handle(null);
            return;
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        //greater
        JsonObject greater = new JsonObject();
        greater.putNumber(MongoKeyWords.GREATER, lastUpdateTime);

        JsonObject matcher = new JsonObject();
        matcher.putObject(colName.AgentDBCols.LAST_UPDATE_TIME, greater);

        query.putObject(MongoKeyWords.MATCHER, matcher);
        //sort
        JsonObject fieldSort = new JsonObject();
        fieldSort.putNumber(colName.AgentDBCols.LAST_UPDATE_TIME, 1);
        query.putObject(MongoKeyWords.SORT, fieldSort);

        //batch size
        query.putNumber(MongoKeyWords.BATCH_SIZE, 30000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<StoreInfo> arrayList = new ArrayList<>();
                for (Object o : results) {
                    arrayList.add(new StoreInfo((JsonObject) o));
                }
                callback.handle(arrayList);
            }
        });
    }

    public void getActivesStores(long lastUpdateTime, final Handler<ArrayList<StoreInfo>> callback) {

        if (lastUpdateTime < 0) {
            logger.info("Sao lai truyen xuong lastUpdateTime am: " + lastUpdateTime);
            callback.handle(null);
            return;
        }

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        //greater
        JsonObject greater = new JsonObject();
        greater.putNumber(MongoKeyWords.GREATER, lastUpdateTime);

        JsonObject jsonLastUpdateTime = new JsonObject();
        jsonLastUpdateTime.putObject(colName.AgentDBCols.LAST_UPDATE_TIME, greater);

        JsonObject agentStatus = new JsonObject();
        agentStatus.putNumber(colName.AgentDBCols.STATUS, 0);

        JsonObject agentDel = new JsonObject();
        agentDel.putBoolean(colName.AgentDBCols.DELETED, false);

        JsonArray jsonAnd = new JsonArray();
        jsonAnd.addObject(jsonLastUpdateTime);
        jsonAnd.addObject(agentStatus);
        jsonAnd.addObject(agentDel);

        JsonObject matcher = new JsonObject();
        matcher.putArray(MongoKeyWords.AND_$, jsonAnd);

        query.putObject(MongoKeyWords.MATCHER, matcher);
        //sort
        JsonObject fieldSort = new JsonObject();
        fieldSort.putNumber(colName.AgentDBCols.LAST_UPDATE_TIME, 1);
        query.putObject(MongoKeyWords.SORT, fieldSort);

        //batch size
        query.putNumber(MongoKeyWords.BATCH_SIZE, 30000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<StoreInfo> arrayList = new ArrayList<>();
                for (Object o : results) {
                    arrayList.add(new StoreInfo((JsonObject) o));
                }
                callback.handle(arrayList);
            }
        });
    }

    private void upsertLoc(JsonObject si, final Handler<Boolean> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        //matcher
//        JsonArray jOr = new JsonArray();
//
//        JsonArray jAnd = new JsonArray();
//
//        JsonObject jsonMMphone = new JsonObject();
//        jsonMMphone.putString(colName.AgentDBCols.MOMO_PHONE, si.getString(colName.AgentDBCols.MOMO_PHONE));
//        JsonObject jsonPhone = new JsonObject();
//        jsonPhone.putString(colName.AgentDBCols.PHONE, si.getString(colName.AgentDBCols.PHONE));
//
//        jAnd.add(jsonMMphone);
//        jAnd.add(jsonPhone);
//
//        JsonObject jsonAnd = new JsonObject();
//        jsonAnd.putArray(MongoKeyWords.AND_$, jAnd);
//
//        JsonObject criteria = new JsonObject();
//        criteria.putNumber(colName.AgentDBCols.ROW_CORE_ID, si.getInteger(colName.AgentDBCols.ROW_CORE_ID));
//
//        jOr.add(jAnd);
//        jOr.add(criteria);
//
//        JsonObject jsonOr = new JsonObject();
//        jsonOr.putArray(MongoKeyWords.OR, jOr);
//
////        query.putObject(MongoKeyWords.CRITERIA, criteria); backup
//        query.putObject(MongoKeyWords.CRITERIA, jsonOr);
        JsonObject criteria = new JsonObject();
        try {
            criteria.putNumber(colName.AgentDBCols.ROW_CORE_ID, si.getInteger(colName.AgentDBCols.ROW_CORE_ID));
        } catch (Exception ex) {
            callback.handle(false);
            return;
        }
        query.putObject(MongoKeyWords.CRITERIA, criteria);

        JsonObject update = new JsonObject();
        update.putObject(MongoKeyWords.SET_$, si);

        query.putObject(MongoKeyWords.OBJ_NEW, update);
        query.putBoolean(MongoKeyWords.UPSERT, true);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                boolean result = message.body().getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    public void getMomoPhone(final Handler<ArrayList<Integer>> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        //keys
        JsonObject keys = new JsonObject();
        keys.putNumber("_id", 0);
        keys.putNumber(colName.AgentDBCols.MOMO_PHONE, 1);
        query.putObject(MongoKeyWords.KEYS, keys);

        //batch size
        query.putNumber(MongoKeyWords.BATCH_SIZE, 50000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                ArrayList<Integer> arrayList = new ArrayList<>();

                if (results != null && results.size() > 0) {
                    for (Object o : results) {
                        int momoNumber = DataUtil.strToInt(((JsonObject) o).getString(colName.AgentDBCols.MOMO_PHONE, "0"));
                        if (momoNumber > 0) {
                            arrayList.add(momoNumber);
                        }
                    }
                }

                callback.handle(arrayList);
            }
        });
    }

    public void getActivedLocs(final Handler<ArrayList<StoreInfo>> callback) {
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putBoolean(colName.AgentDBCols.DELETED, false);

        query.putObject(MongoKeyWords.MATCHER, matcher);

        //batch size
        query.putNumber(MongoKeyWords.BATCH_SIZE, 30000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonArray results = message.body().getArray(MongoKeyWords.RESULT_ARRAY);

                ArrayList<StoreInfo> arrayList = new ArrayList<>();
                for (Object o : results) {
                    arrayList.add(new StoreInfo((JsonObject) o));
                }
                callback.handle(arrayList);
            }
        });
    }

    public void existsMMPhone(final String mmphone, final String callBy, final Handler<Boolean> callback) {

        logger.info("existsMMPhone - callby " + callBy + "for number " + mmphone);
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putBoolean(colName.AgentDBCols.DELETED, false);
        matcher.putString(colName.AgentDBCols.MOMO_PHONE, mmphone);
        matcher.putNumber(colName.AgentDBCols.STATUS, 0);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        JsonObject fields = new JsonObject();
        fields.putNumber("_id", 0);
        fields.putNumber(colName.AgentDBCols.MOMO_PHONE, 1);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonObject jsonObject = message.body().getObject(MongoKeyWords.RESULT, null);
                callback.handle(jsonObject == null ? false : true);
            }
        });
    }

    public void searchWithFilter(JsonObject filter, final Handler<ArrayList<StoreInfo>> callback) {

        //query
        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        if (filter != null && filter.getFieldNames().size() > 0) {
            query.putObject(MongoKeyWords.MATCHER, filter);
        }

        query.putNumber(MongoKeyWords.BATCH_SIZE, 100000);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {

                ArrayList<StoreInfo> arrayList = new ArrayList<StoreInfo>();

                JsonArray joArr = event.body().getArray(MongoKeyWords.RESULT_ARRAY, null);
                if (joArr != null && joArr.size() > 0) {
                    for (int i = 0; i < joArr.size(); i++) {
                        StoreInfo obj = new StoreInfo((JsonObject) joArr.get(i));
                        arrayList.add(obj);
                    }
                }

                callback.handle(arrayList);
            }
        });
    }


    public void existsMMPhoneWithoutDelColumn(final String mmphone, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.FIND_ONE);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);

        JsonObject matcher = new JsonObject();
        matcher.putString(colName.AgentDBCols.MOMO_PHONE, mmphone);
        query.putObject(MongoKeyWords.MATCHER, matcher);

        JsonObject fields = new JsonObject();
        fields.putNumber(colName.AgentDBCols.MOMO_PHONE, 0);
        query.putObject(MongoKeyWords.KEYS, fields);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {

                JsonObject jsonObject = message.body().getObject(MongoKeyWords.RESULT, null);
                callback.handle(jsonObject == null ? false : true);
            }
        });
    }

    public void updatePartial(String phoneNumber
            , JsonObject joUpdate, final Handler<Boolean> callback) {

        JsonObject query = new JsonObject();
        query.putString(MongoKeyWords.ACTION, MongoKeyWords.UPDATE);
        query.putString(MongoKeyWords.COLLECTION, colName.AgentDBCols.TABLE);
        JsonObject match = new JsonObject();

        //matcher
        match.putString(colName.AgentDBCols.MOMO_PHONE, phoneNumber);
        query.putObject(MongoKeyWords.CRITERIA, match);


        JsonObject fieldsSet = new JsonObject();
        fieldsSet.putObject(MongoKeyWords.SET_$, joUpdate);

        query.putObject(MongoKeyWords.OBJ_NEW, fieldsSet);
        query.putBoolean(MongoKeyWords.UPSERT, false);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                JsonObject obj = jsonObjectMessage.body();
                boolean result = obj.getBoolean(MongoKeyWords.IS_UPDATED, false);
                callback.handle(result);
            }
        });
    }

    public static class Location {
        public double Lng;
        public double Lat;

        public Location(double lng, double lat) {
            this.Lng = lng;
            this.Lat = lat;
        }
    }

    public static class StoreInfo {
        public String _id = "";
        public String name = "";
        public String phone = "";
        public String storeName = "";
        public Location loc;
        public String address = "";
        public String street = "";
        public String ward = "";
        public int districtId;
        public int cityId;
        public int areaId;
        public int rowId; // used for client to update/insert information
        public int rowCoreId; // used to get location from core
        public long last_update_time;
        public boolean deleted; // client used this field to delete on local client DB
        public String momoNumber = "";
        public String wardname = "";
        public String districtname = "";
        public String cityname = "";
        public int status = 0; // used to get location from core
        public int agent_type = 0; // used to compare between DGD and DGDCNTT
        public long activeDate = 0;


        public StoreInfo(JsonObject o) {
            _id = o.getString(colName.AgentDBCols._ID, "");
            name = o.getString(colName.AgentDBCols.NAME, "");
            phone = o.getString(colName.AgentDBCols.PHONE, "");
            storeName = o.getString(colName.AgentDBCols.STORE_NAME, "");

            JsonObject l = o.getObject(colName.AgentDBCols.LOCATION, null);
            if (l != null) {
                double lng = DataUtil.getDouble(String.valueOf(l.getArray("coordinates").get(0)));
                double lat = DataUtil.getDouble(String.valueOf(l.getArray("coordinates").get(1)));
                loc = new Location(lng, lat);
            } else {
                loc = new Location(0, 0);
            }

            address = o.getString(colName.AgentDBCols.ADDRESS, "");
            street = o.getString(colName.AgentDBCols.STREET, "");
            ward = o.getString(colName.AgentDBCols.WARD, "");
            districtId = o.getInteger(colName.AgentDBCols.DISTRICT_ID, 0);
            cityId = o.getInteger(colName.AgentDBCols.CITY_ID, 0);
            areaId = o.getInteger(colName.AgentDBCols.AREA_ID, 0);
            rowId = o.getInteger(colName.AgentDBCols.ROW_ID, 0);
            last_update_time = o.getLong(colName.AgentDBCols.LAST_UPDATE_TIME, 0);
            deleted = o.getBoolean(colName.AgentDBCols.DELETED, false);
            rowCoreId = o.getInteger(colName.AgentDBCols.ROW_CORE_ID, 0);
            momoNumber = o.getString(colName.AgentDBCols.MOMO_PHONE, "");
            wardname = o.getString(colName.AgentDBCols.WARD_NAME, "");
            districtname = o.getString(colName.AgentDBCols.DISTRICT_NAME, "");
            cityname = o.getString(colName.AgentDBCols.CITY_NAME, "");
            status = o.getInteger(colName.AgentDBCols.STATUS, 0);
            agent_type = o.getInteger(colName.AgentDBCols.AGENT_TYPE, 0);
            activeDate = o.getLong(colName.AgentDBCols.ACTIVE_DATE, 0);
        }

        public StoreInfo(String name
                , String phone
                , String storeName
                , double lng
                , double lat
                , String address
                , String street
                , String ward
                , int districtId
                , int cityId
                , int areaId
                , long last_update_time
                , boolean deleted
                , int rowCoreId
                , String momo_phone, int status, int agent_type, long activeDate) {
            this.name = name;
            this.phone = phone;
            this.storeName = storeName;
            loc = new Location(lng, lat);
            this.address = address;
            this.street = street;
            this.ward = ward;
            this.districtId = districtId;
            this.cityId = cityId;
            this.areaId = areaId;
            this.last_update_time = last_update_time;
            this.deleted = deleted;
            this.rowCoreId = rowCoreId;
            this.momoNumber = momo_phone;
            this.status = status;
            this.agent_type = agent_type;
            this.activeDate = activeDate;
        }
    }
}
