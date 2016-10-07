package com.mservice.momo.gateway.internal.db.oracle;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStore;
import com.mservice.momo.gateway.internal.soapin.information.obj.MStoreNearestRequest;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.merchant.lotte.entity.MerTranInfo;
import com.mservice.momo.vertx.merchant.lotte.entity.MerTranhis;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import oracle.jdbc.OracleTypes;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

/**
 * Created by concu on 4/19/14.
 */
public class DBProcess {
    private ConnectDB connectDB = null;
    private Logger logger;

    public DBProcess(String driver
            , String url
            , String user
            , String pass
            , String cnnName
            , String poolName
            , Logger log) {
        this.logger = log;
        connectDB = new ConnectDB(driver, url, user, pass, cnnName, poolName, log);
    }

    public static long nullToDefault(Long val, long def) {
        return (val != null ? val : def);
    }

    public static int nullToDefault(Integer val, int def) {
        return (val != null ? val : def);
    }

    public static String nullToDefault(String val, String def) {
        return (val != null ? val : def);
    }

    public static void main(String[] args) {
        //System.out.printf(" ';".matches("('|\"|;|-{2,}|\\s)+") + "");
        JsonObject conf;
        try (Scanner scanner = new Scanner(new File("/Users/ios-001/momo.json.test")).useDelimiter("\\A")) {
            String sconf = scanner.next();
            try {
                conf = new JsonObject(sconf);
            } catch (DecodeException e) {
                System.out.printf("Configuration file does not contain a valid JSON object");
                return;
            }
        } catch (FileNotFoundException e) {
            //log.error("Config file " + configFile + " does not exist");
            return;
        }

    }

    private static boolean checkSQLInjection(String searchValue) {
        return searchValue.matches("('|\"|;|%|-{2,}|\\s)+");
    }

    private static String completedKeyword(String searchValue) {
        String searchReg = StringUtils.isNotEmpty(searchValue) ? searchValue.replaceAll("('|\"|;|%|-{2,}|:)+", "").replaceAll("\\s+", " ") : "";

        String tmp = new String(searchReg);
        if (tmp.toLowerCase().matches("quan\\d+")) {
            searchReg = tmp.toLowerCase().replaceAll("quan", " Quận ");
        }
        searchReg = searchReg.toLowerCase().replaceAll("tp(\\.|\\s)*hcm", "Thành Phố Hồ Chí Minh");
        searchReg = searchReg.toLowerCase().replaceAll("tp(\\.|\\s)*hn", "Hà Nội");

        searchReg = searchReg.replaceAll("(^|\\s)(TP|tp|Tp)\\s+", " Thành Phố ");
        searchReg = searchReg.replaceAll("(^|\\s)(TP|tp|Tp)\\.", " Thành Phố ");
        searchReg = searchReg.replaceAll("(^|\\s)(TX|tx|Tx)\\s+", " Thị Xã ");
        searchReg = searchReg.replaceAll("(^|\\s)(TX|tx|Tx)\\.", " Thị Xã ");
        searchReg = searchReg.replaceAll("(^|\\s)(p|P)\\s+", " Phường ");
        searchReg = searchReg.replaceAll("(^|\\s)(p|P)\\.", " Phường ");
        searchReg = searchReg.replaceAll("(^|\\s)(q|Q)\\s+", " Quận ");
        searchReg = searchReg.replaceAll("(^|\\s)(q|Q)\\.", " Quận ");

        return searchReg;
    }

    private static boolean checkSearchComplex(String searchReg) {
        searchReg = Misc.removeAccent(searchReg).toLowerCase();
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

    public ArrayList<JsonObject> getStoresData(long lastUpdateTime, Logger logger) {
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("syncloc");
        log.add("function", "getStoresData(long lastUpdateTime, Logger logger)");
        int index = 1;
        //lastUpdateTime = 0;
        ArrayList<JsonObject> arr = new ArrayList<>();
        try {

            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call PRO_MMTAGENT_LASTUPDATE(?,?)}");
            cs.setTimestamp(1, new Timestamp(lastUpdateTime));
            cs.registerOutParameter(2, OracleTypes.CURSOR);

            cs.execute();
            ResultSet res = (ResultSet) cs.getObject(2);
            while (res.next()) {
                JsonObject o = new JsonObject();
                o.putString(colName.AgentDBCols.NAME, res.getString(colName.AgentDBCols.NAME));
                o.putString(colName.AgentDBCols.PHONE, res.getString(colName.AgentDBCols.PHONE));
                o.putString(colName.AgentDBCols.STORE_NAME, res.getString(colName.AgentDBCols.STORE_NAME));
                o.putString(colName.AgentDBCols.LONGITUDE, res.getString(colName.AgentDBCols.LONGITUDE));
                o.putString(colName.AgentDBCols.LATITUDE, res.getString(colName.AgentDBCols.LATITUDE));

                String address = nullToDefault(res.getString(colName.AgentDBCols.ADDRESS), "");
                o.putString(colName.AgentDBCols.ADDRESS, address);
                o.putString(colName.AgentDBCols.STREET, res.getString(colName.AgentDBCols.STREET));
                o.putString(colName.AgentDBCols.WARD, res.getString(colName.AgentDBCols.WARD));
                o.putString(colName.AgentDBCols.WARD_NAME, res.getString(colName.AgentDBCols.WARD_NAME));
                o.putString(colName.AgentDBCols.DISTRICT_ID, res.getString(colName.AgentDBCols.DISTRICT_ID));
                o.putString(colName.AgentDBCols.DISTRICT_NAME, res.getString(colName.AgentDBCols.DISTRICT_NAME));
                o.putString(colName.AgentDBCols.CITY_ID, res.getString(colName.AgentDBCols.CITY_ID));
                o.putString(colName.AgentDBCols.CITY_NAME, res.getString(colName.AgentDBCols.CITY_NAME));
                o.putString(colName.AgentDBCols.AREA_ID, res.getString(colName.AgentDBCols.AREA_ID));

                //status : 0 active : 1 suspend 2: deleted
                int status = nullToDefault(res.getInt(colName.AgentDBCols.STATUS), 0);
                o.putNumber(colName.AgentDBCols.STATUS, status);

                o.putNumber(colName.AgentDBCols.LAST_UPDATE_TIME, res.getTimestamp(colName.AgentDBCols.LAST_UPDATE_TIME).getTime());
                String deleted = res.getString(colName.AgentDBCols.DELETED);
                //if(deleted == null || deleted.equalsIgnoreCase("null") || deleted.equalsIgnoreCase("0")){
                if (deleted != null && deleted.equalsIgnoreCase("0")) {
                    deleted = "0";
                } else {
                    deleted = "1";
                }

                //force deleted by status
                deleted = "1".equalsIgnoreCase(deleted) ? "1" : (status == 2 ? "1" : "0");

                o.putString(colName.AgentDBCols.DELETED, deleted);
                o.putString(colName.AgentDBCols.ROW_CORE_ID, res.getString(colName.AgentDBCols.ROW_CORE_ID));

                o.putString(colName.AgentDBCols.MOMO_PHONE, res.getString("momophone"));
                o.putNumber(colName.AgentDBCols.AGENT_TYPE, DataUtil.strToInt(res.getString("agent_type")));
                o.putString(colName.AgentDBCols.TDL, res.getString("tdl"));
                try {
                    o.putNumber(colName.AgentDBCols.ACTIVE_DATE, DataUtil.strToLong(res.getString(colName.AgentDBCols.ACTIVE_DATE)));
                    log.add("desc sync store, gia tri active date la ", DataUtil.strToLong(res.getString(colName.AgentDBCols.ACTIVE_DATE)));
                } catch (Exception ex) {
                    log.add("desc sync store, gia tri active date la ", "Khong co field nay, store chua tra ve.");
                    o.putNumber(colName.AgentDBCols.ACTIVE_DATE, 0);
                }

                log.add("agent_type sync", DataUtil.strToInt(res.getString("agent_type")));
                log.add("location " + index, "-------------------");
                log.add("json value", o.toString());
                index++;
                arr.add(formatStore(o));
            }

            conn.close();
        } catch (Exception ex) {
            String errorDesc = (ex.getMessage() == null ? "null" : ex.getMessage());

            logger.error("function getStoresData " + errorDesc, ex);

            log.add("exception", "PRO_MMTAGENT_LASTUPDATE " + errorDesc);
        }
        log.writeLog();

        return arr;
    }

    /*select acc.AVAIL_BALANCE sodumomo  from umarketadm.agent_ref ar left join  umarketadm.account acc on ar.BODYID=acc.AGENT and acc.type_=1
    where ar.reference = '0988329773';
    */

    private JsonObject formatStore(JsonObject o) {
        String name = o.getString(colName.AgentDBCols.NAME);
        String s_name = o.getString(colName.AgentDBCols.STORE_NAME);
        String add = o.getString(colName.AgentDBCols.ADDRESS);
        String street = o.getString(colName.AgentDBCols.STREET);
        String wname = o.getString(colName.AgentDBCols.WARD_NAME);
        String dname = o.getString(colName.AgentDBCols.DISTRICT_NAME);
        String cname = o.getString(colName.AgentDBCols.CITY_NAME);
        if (name != null)
            o.putString(colName.AgentDBCols.NAME, name.replaceAll("\\s{2,}", " "));
        if (s_name != null)
            o.putString(colName.AgentDBCols.STORE_NAME, s_name.replaceAll("\\s{2,}", " "));
        if (add != null)
            o.putString(colName.AgentDBCols.ADDRESS, add.replaceAll("\\s{2,}", " "));
        if (street != null)
            o.putString(colName.AgentDBCols.STREET, street.replaceAll("\\s{2,}", " ").replaceAll(":", ""));
        if (wname != null)
            o.putString(colName.AgentDBCols.WARD_NAME, wname.replaceAll("\\s{2,}", " ").replaceAll(":", ""));
        if (dname != null)
            o.putString(colName.AgentDBCols.DISTRICT_NAME, dname.replaceAll("\\s{2,}", " ").replaceAll(":", ""));
        if (cname != null)
            o.putString(colName.AgentDBCols.CITY_NAME, cname.replaceAll("\\s{2,}", " ").replaceAll(":", ""));
        return o;
    }

    public boolean MT_NAP_RUT_TAN_NOI_UPSERT(String address
            , int service_type
            , int named
            , String full_name
            , String phone_number
            , long amount
            , int from_source
            , String comment
            , Logger log) {
        /*service_type : 0-deposit, 1-withdraw
        named : 1- named, 0 - not named
        p_from_source : 0-app_mobile, 1-web,3-other
        */
        boolean result = true;
        try {
            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call MMT_NAP_RUT_TAN_NOI_UPSERT(?,?,?,?,?,?,?,?)}");

            cs.setString(1, address);
            cs.setInt(2, service_type);
            cs.setInt(3, named);
            cs.setString(4, full_name);
            cs.setString(5, phone_number);
            cs.setLong(6, amount);
            cs.setInt(7, from_source);
            cs.setString(8, comment);
            //cs.registerOutParameter(2, OracleTypes.CURSOR);
            cs.execute();
            conn.close();
        } catch (Exception ex) {

            String errorDesc = (ex.getMessage() == null ? "null" : ex.getMessage());
            log.error("Co loi tai call MMT_NAP_RUT_TAN_NOI_UPSERT " + errorDesc);
            logger.error("function MT_NAP_RUT_TAN_NOI_UPSERT " + errorDesc, ex);
            result = false;
        }
        return result;
    }

    public boolean M_SERVICE_BANKNET_UPSERT(long p_start_time
            , long p_end_time
            , String p_customer_account
            , long p_amount
            , String p_trans_id
            , int p_partner_cfm_code
            , int p_internal_error
            , int p_external_error, int p_source
            , Logger log) {
        /*service_type : 0-deposit, 1-withdraw
        named : 1- named, 0 - not named
        p_from_source : 0-app_mobile, 1-web,3-other
        */
        boolean result = true;
        try {
            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call M_SERVICE_BANKNET_UPSERT(?,?,?,?,?,?,?,?,?)}");

            cs.setTimestamp(1, new Timestamp(p_start_time));
            cs.setTimestamp(2, new Timestamp(p_end_time));
            cs.setString(3, p_customer_account);
            cs.setLong(4, p_amount);
            cs.setString(5, p_trans_id);
            cs.setInt(6, p_partner_cfm_code);
            cs.setString(7, p_internal_error + "");
            cs.setString(8, p_external_error + "");
            cs.setInt(9, p_source);
            cs.execute();
            conn.close();
        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            log.error("co loi tai call M_SERVICE_BANKNET_UPSERT " + errorDesc);
            logger.error("function M_SERVICE_BANKNET_UPSERT " + errorDesc, e);
            result = false;
        }
        return result;
    }

    public boolean BANKPOUT_PENDING_UPSERT(String p_agent
            , String p_bankname
            , long p_trans_pending
            , String p_cardname
            , String p_cardnumber
            , long p_amount
            , long p_fee
            , long p_recieving_amount
            , String p_comment
            , int p_bank_location
            , String p_bank_branch
            , int p_source
            , Logger log
    ) {

        boolean result = true;
        try {
            Connection cnn = connectDB.getConnect();
            CallableStatement cs = cnn.prepareCall("{call BANKPOUT_PENDING_UPSERT(?,?,?,?,?,?,?,?,?,?,?,?)}");

            cs.setString(1, p_agent);
            cs.setString(2, p_bankname);
            cs.setLong(3, p_trans_pending);
            cs.setString(4, p_cardname);
            cs.setString(5, p_cardnumber);
            cs.setLong(6, p_amount);
            cs.setLong(7, p_fee);
            cs.setLong(8, p_recieving_amount);
            cs.setString(9, p_comment);
            cs.setInt(10, p_bank_location);
            cs.setString(11, p_bank_branch);
            cs.setInt(12, p_source);
            cs.execute();
            cnn.close();
        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            log.error("co loi tai call BANKPOUT_PENDING_UPSERT " + errorDesc);
            logger.error("function BANKPOUT_PENDING_UPSERT " + errorDesc, e);
            result = false;
        }
        return result;
    }

    public ArrayList<String> getAllMMTAgent(Logger log) {

        String query = "select REFERENCE from mv_mmt_agent";
        ArrayList<String> arrayList = new ArrayList<>();

        try {
            Connection cnn = connectDB.getConnect();
            Statement preparedStatement = cnn.createStatement();
            ResultSet res = preparedStatement.executeQuery(query);

            while (res.next()) {
                arrayList.add(res.getString("REFERENCE"));
            }
            cnn.close();
        } catch (Exception e) {
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            log.error("co loi tai select REFERENCE from mv_mmt_agent " + errorDesc);
            logger.error("function getAllMMTAgent " + errorDesc, e);
        }

        return arrayList;
    }

    public long GetAvailableBalance(int phoneNumber, Logger logger) {
        /*service_type : 0-deposit, 1-withdraw
        named : 1- named, 0 - not named
        p_from_source : 0-app_mobile, 1-web,3-other
        */
        long balance = 0;
        String query = "select acc.AVAIL_BALANCE sodumomo  from "
                + " umarketadm.agent_ref ar left join  umarketadm.account acc on ar.BODYID=acc.AGENT and acc.type_=1 "
                + " where ar.reference = '0" + phoneNumber + "'  AND ar.DELETED = 0";
        try {
            Connection cnn = connectDB.getConnect();
            Statement preparedStatement = cnn.createStatement();
            ResultSet res = preparedStatement.executeQuery(query);

            while (res.next()) {
                balance = res.getLong("sodumomo");
                break;
            }

            cnn.close();
        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("function GetAvailableBalance " + errorDesc, e);
            logger.error("co loi tai " + query + " " + errorDesc);
        }
        return balance;
    }

    public boolean login(String user, String pass) {
        /*service_type : 0-deposit, 1-withdraw
        named : 1- named, 0 - not named
        p_from_source : 0-app_mobile, 1-web,3-other
        */
        String agentId = "";
        String query = "SELECT *" +
                " FROM UMARKETADM.AGENT_AUTH_TOKEN" +
                " WHERE AGENTID = (SELECT BODYID FROM UMARKETADM.AGENT_REF WHERE REFERENCE = '" + user + "' AND DELETED = 0)" +
                " AND TOKEN = '" + pass + "'";
        try {
            Connection cnn = connectDB.getConnect();
            Statement preparedStatement = cnn.createStatement();
            ResultSet res = preparedStatement.executeQuery(query);

            while (res.next()) {
                agentId = res.getString("AGENTID");
                break;
            }

            cnn.close();
        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("function GetAvailableBalance " + errorDesc, e);
            logger.error("co loi tai " + query + " " + errorDesc);
        }

        boolean isSuccess = "".equalsIgnoreCase(agentId) ? false : true;

        return isSuccess;
    }

    public ArrayList<String> GetGHNPersonList(Common.BuildLog log) {

        return new ArrayList<>();

        /*log.add("function","GetGHNPersonList");

        ArrayList<String> result = new ArrayList<>();

        Connection conn = connectDB.getConnect();
        try {
        CallableStatement cs = conn.prepareCall("{call report_admin.PRO_GHN_PERSON_LIST(?)}");
        cs.registerOutParameter(1,OracleTypes.CURSOR);
        cs.execute();
        ResultSet res = (ResultSet)cs.getObject(1);
        while (res.next())
        {
            result.add(nullToDefault(res.getString("PHONE"),"").trim());
        }

        log.add("GHN Size", result.size());

        conn.close();
    }catch(Exception e){
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("function GetGHNPersonList " + errorDesc, e);

            logger.error("co loi tai call report_admin.PRO_GHN_PERSON_LIST(?) " + errorDesc);
            log.add("exception","report_admin.PRO_GHN_PERSON_LIST " + errorDesc);
    }
    return result;*/
    }

    public boolean PRO_M2M_CAPSET(String P_CUSTOMER
            , long P_AMOUNT, Common.BuildLog log) {
        /*
            result : 1 that bai, 0 thanh cong
        */

        long result = 0;
        try {
            Connection cnn = connectDB.getConnect();
            CallableStatement cs = cnn.prepareCall("{call PRO_M2M_CAPSET(?,?,?)}");

            cs.setString(1, P_CUSTOMER);
            cs.setLong(2, P_AMOUNT);
            cs.registerOutParameter(3, OracleTypes.NUMBER);

            cs.execute();
            result = cs.getLong(3);
            cnn.close();

        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("function PRO_M2M_CAPSET " + errorDesc, e);
            logger.error("co loi tai call PRO_M2M_CAPSET(?,?,?) number: " + P_CUSTOMER + "  amount: " + P_AMOUNT + " - " + errorDesc);
            log.add("exception", errorDesc);
            result = 0;
        }

        if (result == 0) {
            return true;
        } else {
            return false;
        }
    }

    public void PRO_UPDATE_NAPRUTTANNOI(String p_ghn_phone,
                                        String p_cus_phone,
                                        long p_last_modified,
                                        long p_tid,
                                        Common.BuildLog log

    ) {
        log.add("function", "PRO_UPDATE_NAPRUTTANNOI");
        log.add("p_ghn_phone", p_ghn_phone);
        log.add("p_cus_phone", p_cus_phone);
        log.add("p_last_modified", p_last_modified);
        log.add("p_tid", p_tid);
        try {
            Connection cnn = connectDB.getConnect();
            CallableStatement cs = cnn.prepareCall("{call PRO_UPDATE_NAPRUTTANNOI(?,?,?,?)}");

            cs.setString(1, p_ghn_phone);
            cs.setString(2, p_cus_phone);
            cs.setTimestamp(3, new Timestamp(p_last_modified));
            cs.setLong(4, p_tid);
            cs.execute();
            cnn.close();
            log.add("result", "success");

        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("co loi tai call PRO_UPDATE_NAPRUTTANNOI(?,?,?,?) " + errorDesc);
            logger.error("function PRO_UPDATE_NAPRUTTANNOI " + errorDesc, e);
            log.add("exception", errorDesc);
        }
    }

    /*
    dong bo du lieu DbOracle
     */
    public void PROC_APPS_UPDATE_AI_DATA(String p_momo,
                                         String p_fullname,
                                         String p_dob,
                                         String p_personalid,
                                         String p_personaltype,
                                         String p_email,
                                         String p_address,
                                         String p_ward,
                                         int p_districtid,
                                         String p_districtname,
                                         int p_cityid,
                                         String p_cityname,
                                         int p_status,
                                         Common.BuildLog log

    ) {
        log.add("function", "PROC_APPS_UPDATE_AI_DATA");
        log.add("p_momo", p_momo);
        log.add("p_fullname", p_fullname);
        log.add("p_dob", p_dob);
        log.add("p_personalid", p_personalid);
        log.add("p_personaltype", p_personaltype);
        log.add("p_email", p_email);
        log.add("p_address", p_address);
        log.add("p_ward", p_ward);
        log.add("p_districtid", p_districtid);
        log.add("p_districtname", p_districtname);
        log.add("p_cityid", p_cityid);
        log.add("p_cityname", p_cityname);
        log.add("p_status", p_status);

        try {
            Connection cnn = connectDB.getConnect();
            CallableStatement cs = cnn.prepareCall("{call PROC_APPS_UPDATE_AI_DATA(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");

            cs.setString(1, p_momo);
            cs.setString(2, p_fullname);
            cs.setString(3, p_dob);
            cs.setString(4, p_personalid);
            cs.setString(5, p_personaltype);
            cs.setString(6, p_email);
            cs.setString(7, p_address);
            cs.setString(8, p_ward);
            cs.setInt(9, p_districtid);
            cs.setString(10, p_districtname);
            cs.setInt(11, p_cityid);
            cs.setString(12, p_cityname);
            cs.setInt(13, p_status);
            cs.registerOutParameter(14, OracleTypes.VARCHAR);
            cs.execute();

            String result = cs.getString(14);
            log.add("core result", result);
            cnn.close();
            log.add("result", "success");

        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("co loi tai call htpp_admin.PROC_APPS_UPDATE_AI_DATA(?,?,?,?,?,?,?,?,?,?,?,?,?) " + errorDesc);
            logger.error("function PROC_APPS_UPDATE_AI_DATA " + errorDesc, e);
            log.add("exception", errorDesc);
        }
    }

    public String PRO_M2M_TYPE(String P_AGENT_FROM
            , String P_AGENT_TO, Common.BuildLog log) {

        String result = "";
        try {
            Connection cnn = connectDB.getConnect();
            CallableStatement cs = cnn.prepareCall("{call PRO_M2M_TYPE(?,?,?)}");

            cs.setString(1, P_AGENT_FROM);
            cs.setString(2, P_AGENT_TO);
            cs.registerOutParameter(3, OracleTypes.VARCHAR);

            cs.execute();
            result = cs.getString(3);
            cnn.close();

        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("function PRO_M2M_TYPE " + errorDesc, e);
            logger.error("co loi tai call PRO_M2M_TYPE(?,?,?) from number: " + P_AGENT_FROM + " to number " + P_AGENT_TO + " - " + errorDesc);
            log.add("exception", errorDesc);
//            result = "u2u"; Fix loi u2u khi khong goi store thanh cong.
        }
        return result;
    }

    public Integer PRO_AGENT_RATING(String P_AGENT, Integer P_RATING, String P_USER, String P_CONTENT, Integer P_TYPE, Common.BuildLog log) {

        int result = 0;
        try {
            Connection cnn = connectDB.getConnect();
            CallableStatement cs = cnn.prepareCall("{call PRO_AGENT_RATING(?,?,?,?,?)}");

            cs.setString(1, P_AGENT);
            cs.setInt(2, P_RATING);
            cs.setString(3, P_USER);
            cs.setString(4, P_CONTENT);
            cs.setInt(5, P_TYPE);
//            cs.registerOutParameter(3,OracleTypes.VARCHAR);

            cs.execute();
//            result = cs.getString(3);
            cnn.close();

        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("co loi tai PRO_AGENT_RATING(?,?,?,?,?) " + errorDesc);
            logger.error("function PRO_AGENT_RATING " + errorDesc, e);
            log.add("exception", errorDesc);
            result = 1;
        }
        return result;
    }

    /**
     * retailerNumber : so dien thoai cua diem giao dich
     */
    public JsonObject PRO_GET_CAPTION_SMS_LIQUIDITY(String retailerNumber, Common.BuildLog log) {
        JsonObject jo = new JsonObject();
        log.add("func", "PRO_GET_CAPTION_SMS_LIQUIDITY");

        try {

            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call PRO_GET_CAPTION_SMS_LIQUIDITY(?,?)}");

            cs.setString(1, retailerNumber);
            cs.registerOutParameter(2, OracleTypes.CURSOR);

            cs.execute();
            ResultSet res = (ResultSet) cs.getObject(2);
            while (res.next()) {
                String caption = nullToDefault(res.getString("caption"), "");
                String content = nullToDefault(res.getString("content"), "");

                log.add("caption", caption);
                log.add("content", content);

                jo.putString("caption", caption);
                jo.putString("content", content);
                break;
            }
            conn.close();
        } catch (Exception e) {
            jo.putNumber("error", 100);
            log.add("error", "co loi tai call PRO_GET_CAPTION_SMS_LIQUIDITY(?)" + e.getMessage());
            log.add("desc", e.getMessage());
        }
        return jo;
    }

    public ArrayList<JsonObject> getAgentsData(long lastUpdateTime, Logger logger) {
        ArrayList<JsonObject> arr = new ArrayList<>();
        try {

            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call PRO_GET_AGENT_INFO(?,?)}");

            cs.setTimestamp(1, new Timestamp(lastUpdateTime));
            cs.registerOutParameter(2, OracleTypes.CURSOR);

            cs.execute();
            ResultSet res = (ResultSet) cs.getObject(2);
            while (res.next()) {
                Common.BuildLog log = new Common.BuildLog(logger);

                JsonObject o = new JsonObject();
                String sNumber = nullToDefault(res.getString(colName.PhoneDBCols.NUMBER), "0");
                int number = DataUtil.strToInt(sNumber);
                log.setPhoneNumber(sNumber);

                //khong phai la so dien thoai --> bo qua
                if (!Misc.checkNumber(number)) {
                    log.add("Invalid number so we do next round, no update phones table", "");
                    log.writeLog();
                    continue;
                }
                /*"addr" : "",
                "bnk_acc" : "",
                "bnk_code" : "",
                "bnk_name" : "",
                "card_id" : "",
                "del" : false,
                "dob" : "",
                "email" : "",
                "is_actived" : false,
                "is_named" : true,
                "is_reged" : false,
                "name" : "",
                "number" : 943570109,
                "last_update_time":1475552214785L
                */

                /*SELECt MAX(address) address
                    ,MAX(bank_acc) bank_acc
                    ,MAX(bank_code) bank_code
                    ,MAX(bank_name) bank_name
                    ,MAX(card_id) card_id
                    ,MAX(del) del
                    ,MAX(DOB) DOB
                    ,MAX(Email) Email
                    ,MAX(is_active) is_active
                    ,MAX(is_named) is_named
                    ,MAX(is_reged) is_reged
                    ,MAX(name) "name"
                    ,phoneNumber AS "number"
                    ,MAX(last_modified)last_update_time*/


                String address = nullToDefault(res.getString(colName.PhoneDBCols.ADDRESS), "");
                String bank_acc = nullToDefault(res.getString(colName.PhoneDBCols.BANK_ACCOUNT), "");
                String bank_code = nullToDefault(res.getString(colName.PhoneDBCols.BANK_CODE), "");
                String bank_name = nullToDefault(res.getString(colName.PhoneDBCols.BANK_NAME), "");

                String card_id = nullToDefault(res.getString(colName.PhoneDBCols.CARD_ID), "");

                String waitRegister = nullToDefault(res.getString(colName.PhoneDBCols.WAITING_REG), "0");

                String strDeleted = nullToDefault(res.getString(colName.PhoneDBCols.DELETED), "false");
                boolean deleted = true;
                if (strDeleted.equalsIgnoreCase("false")) {
                    deleted = false;
                }

                String bank_personal_id = nullToDefault(res.getString(colName.PhoneDBCols.BANK_PERSONAL_ID), "");

                String dob = nullToDefault(res.getString(colName.PhoneDBCols.DATE_OF_BIRTH), "");
                String email = nullToDefault(res.getString(colName.PhoneDBCols.EMAIL), "");
                String name = nullToDefault(res.getString(colName.PhoneDBCols.NAME), "");

                //todo we dont use isFrozen any more
                String sIsActived = nullToDefault(res.getString(colName.PhoneDBCols.IS_ACTIVED), "false");
                boolean is_actived = true;
                if (sIsActived.equalsIgnoreCase("false")) {
                    is_actived = false;
                }

                String sIsReged = nullToDefault(res.getString(colName.PhoneDBCols.IS_REGED), "false");
                boolean is_reged = true;
                if (sIsReged.equalsIgnoreCase("false")) {
                    is_reged = false;
                }

                /*
                int status = nullToDefault(res.getInt(colName.PhoneDBCols.STATUS),0);
                ObjCoreStatus objCoreStatus = new ObjCoreStatus(status);
                is_reged = is_reged || objCoreStatus.isReged;
                is_actived = is_actived || objCoreStatus.isActivated;
                */

                String sIsNamed = nullToDefault(res.getString(colName.PhoneDBCols.IS_NAMED), "false");
                boolean is_named = "true".equalsIgnoreCase(sIsNamed);

                long last_update_time = nullToDefault(res.getTimestamp(colName.PhoneDBCols.LAST_UPDATE_TIME).getTime(), 0L);

                long createDate;
                if (res.getTimestamp(colName.PhoneDBCols.CREATED_DATE) == null) {
                    createDate = 0;
                } else {
                    createDate = nullToDefault(res.getTimestamp(colName.PhoneDBCols.CREATED_DATE).getTime(), 0L);
                }

                //set fields
                o.putString(colName.PhoneDBCols.ADDRESS, address);
                o.putString(colName.PhoneDBCols.BANK_ACCOUNT, bank_acc.equalsIgnoreCase("null") ? "" : bank_acc);
                o.putString(colName.PhoneDBCols.BANK_CODE, bank_code.equalsIgnoreCase("null") ? "" : bank_code);
                o.putString(colName.PhoneDBCols.BANK_NAME, bank_name.equalsIgnoreCase("null") ? "" : bank_name);
                o.putString(colName.PhoneDBCols.CARD_ID, card_id.equalsIgnoreCase("null") ? "" : card_id);
                o.putBoolean(colName.PhoneDBCols.DELETED, deleted);
                if (createDate > 0) {
                    o.putNumber(colName.PhoneDBCols.CREATED_DATE, createDate);
                }

                boolean isWaitRegister = "1".equalsIgnoreCase(waitRegister) ? true : false;

                o.putBoolean(colName.PhoneDBCols.WAITING_REG, isWaitRegister);

                if (!"".equalsIgnoreCase(bank_personal_id)) {
                    o.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, bank_personal_id);
                }

                logger.info("bankpersonalid: " + bank_personal_id);

                //neu xoa roi --> reset cac fields
                if (deleted) {
                    o.putBoolean(colName.PhoneDBCols.IS_SETUP, false);
                    o.putBoolean(colName.PhoneDBCols.IS_ACTIVED, false);
                    o.putBoolean(colName.PhoneDBCols.IS_REGED, false);
                    o.putBoolean(colName.PhoneDBCols.IS_NAMED, false);

                    o.putString(colName.PhoneDBCols.SESSION_KEY, "");
                    o.putString(colName.PhoneDBCols.IMEI_KEY, "");
                    o.putString(colName.PhoneDBCols.LAST_IMEI, "");
                } else {
                    o.putBoolean(colName.PhoneDBCols.IS_ACTIVED, is_actived);
                    o.putBoolean(colName.PhoneDBCols.IS_REGED, is_reged);
                    o.putBoolean(colName.PhoneDBCols.IS_NAMED, is_named);
                }

                o.putString(colName.PhoneDBCols.DATE_OF_BIRTH, dob.equalsIgnoreCase("null") ? "" : dob);
                o.putString(colName.PhoneDBCols.EMAIL, email.equalsIgnoreCase("null") ? "" : email);
                o.putString(colName.PhoneDBCols.NAME, name.equalsIgnoreCase("null") ? "" : name);
                o.putNumber(colName.PhoneDBCols.NUMBER, number);
                o.putNumber(colName.PhoneDBCols.LAST_UPDATE_TIME, last_update_time);

                log.add("json phones object will be updated", o.toString());
                log.writeLog();

                //add to JsonObject array
                arr.add(o);
            }

            conn.close();
        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("co loi tai call PRO_GET_AGENT_INFO(?,?) " + errorDesc);
            logger.error("function getAgentsData " + errorDesc, e);
            logger.info("call PRO_GET_AGENT_INFO " + errorDesc);
        }
        return refinedAgents(arr);
    }

    private long nullToDefault(Timestamp val, long def) {
        return (val != null ? val.getTime() : def);
    }

    //loai bo thang delete sau do qua lai dang ky --> tra len dong delete va dong dang ky moi
    private ArrayList<JsonObject> refinedAgents(ArrayList<JsonObject> srcJsonObjs) {
        if (srcJsonObjs.size() <= 1) return srcJsonObjs;

        ArrayList<JsonObject> cloneList = (ArrayList<JsonObject>) srcJsonObjs.clone();

        for (int i = 0; i < srcJsonObjs.size(); i++) {
            JsonObject item = srcJsonObjs.get(i);
            int itemNumber = item.getInteger(colName.PhoneDBCols.NUMBER);
            long itemUpTime = item.getLong(colName.PhoneDBCols.LAST_UPDATE_TIME);

            for (int j = i + 1; j < srcJsonObjs.size(); j++) {
                JsonObject replaceItem = srcJsonObjs.get(j);
                int replaceNumber = replaceItem.getInteger(colName.PhoneDBCols.NUMBER);
                long replaceUpTime = replaceItem.getLong(colName.PhoneDBCols.LAST_UPDATE_TIME);

                if (itemNumber == replaceNumber && (itemUpTime < replaceUpTime)) {
                    cloneList.remove(item);
                    item = replaceItem;
                }
            }
        }

        return cloneList;

    }

    public JsonObject getAgentBalanceExtend(String phone, Common.BuildLog log) {
        JsonObject json = new JsonObject();
        boolean result = true;
        try {
            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call PRO_GETAGENTBALANCE(?,?)}");
            cs.setString(1, phone);
            cs.registerOutParameter(2, OracleTypes.CURSOR);

            cs.execute();
            ResultSet res = (ResultSet) cs.getObject(2);
            //1 momo; 2 mload,3 point,4 voucher
            while (res.next()) {
                long balance = res.getLong("sodu");
                int type = res.getInt("acctype");
                log.add("acctype[" + type + "]", balance);
                switch (type) {
                    case 1:
                        json.putNumber(colName.CoreBalanceCols.BALANCE, balance); //momo
                        break;
                    case 2:
                        json.putNumber(colName.CoreBalanceCols.MLOAD, balance);
                        break;
                    case 3:
                        json.putNumber(colName.CoreBalanceCols.POINT, balance);
                        break;
                    case 4:
                        json.putNumber(colName.CoreBalanceCols.VOUCHER, balance);
                        break;
                }
            }
            conn.close();
        } catch (Exception e) {
            log.add("exception DB", e.toString());
            result = false;
            json.putNumber(colName.CoreBalanceCols.BALANCE, Long.MIN_VALUE);
            json.putNumber(StringConstUtil.ERROR, Integer.MIN_VALUE);
        }
        return json;
    }

    public JsonArray getC2CInfo(String mtcn, Common.BuildLog log) {

        String sndName = "";
        String sndPhone = "";
        String sndCardId = "";

        String rcvName = "";
        String rcvPhone = "";
        String rcvCardId = "";

        int status = 0;
        String reason = "";
        long amount = 0;
        String msg = "";
        log.add("call store", "PRO_GET_C2C_BY_MTCN");
        try {
            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call PRO_GET_C2C_BY_MTCN(?,?)}");
            cs.setString(1, mtcn);
            cs.registerOutParameter(2, OracleTypes.CURSOR);

            cs.execute();
            ResultSet res = (ResultSet) cs.getObject(2);

            while (res.next()) {

                sndName = nullToDefault(res.getString("PROVIDERNAME"), "");
                sndPhone = nullToDefault(res.getString("PROVIDER"), "");
                sndCardId = nullToDefault(res.getString("PROVIDERID"), "");

                rcvName = nullToDefault(res.getString("RECIPIENTNAME"), "");
                rcvPhone = nullToDefault(res.getString("RECIPIENT"), "");
                rcvCardId = nullToDefault(res.getString("RECIPIENTID"), "");

                amount = DataUtil.stringToUNumber(nullToDefault(res.getString("AMOUNT"), ""));

                msg = nullToDefault(res.getString("MESSAGE"), "");
                status = DataUtil.strToInt(nullToDefault(res.getString("STATUS"), ""));
                reason = nullToDefault(res.getString("REASON"), "");
                break;
            }
            log.add("status", status);
            log.add("reason", reason);
            conn.close();
        } catch (Exception e) {
            log.add("loi", "khong lay duoc thong tin cua giao dich c2c, mtcn " + mtcn);
        }

        JsonArray array = new JsonArray();

        array.add(new JsonObject().putString(Const.C2C.senderName, sndName));
        array.add(new JsonObject().putString(Const.C2C.senderPhone, sndPhone));
        array.add(new JsonObject().putString(Const.C2C.senderCardId, sndCardId));

        array.add(new JsonObject().putString(Const.C2C.receiverName, rcvName));
        array.add(new JsonObject().putString(Const.C2C.receiverPhone, rcvPhone));
        array.add(new JsonObject().putString(Const.C2C.receiverCardId, rcvCardId));
        array.add(new JsonObject().putNumber(Const.AppClient.Amount, amount));
        array.add(new JsonObject().putString("msg", msg));
        array.add(new JsonObject().putNumber(StringConstUtil.STATUS, status));
        array.add(new JsonObject().putString(StringConstUtil.DESCRIPTION, reason.trim()));
        log.add("result:", array.encodePrettily());
        return array;
    }

    public String getCouponId(long tranId, Common.BuildLog log) {

        String couponId = "";
        try {
            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call PRO_GET_LASTEST_MTCN(?,?)}");
            cs.setLong(1, tranId);
            cs.registerOutParameter(2, OracleTypes.CURSOR);

            cs.execute();
            ResultSet res = (ResultSet) cs.getObject(2);

            while (res.next()) {
                couponId = res.getString("mtcn");
                break;
            }

            conn.close();
        } catch (Exception e) {
            couponId = ""; //momo
            log.add("loi", "khong lay duoc coupon id cho giao dich c2c");
        }
        return couponId;
    }

    public boolean PRO_CHECK_GTBB(String phone, Common.BuildLog log) {
        boolean result = true;
        try {
            Connection conn = connectDB.getConnect();
            CallableStatement cs = conn.prepareCall("{call PRO_CHECK_GTBB(?,?)}");
            cs.setString(1, phone);
            cs.registerOutParameter(2, OracleTypes.CURSOR);

            cs.execute();
            ResultSet res = (ResultSet) cs.getObject(2);
            while (res.next()) {
                result = res.getBoolean("RESULT");
            }
            conn.close();
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    public JsonObject checkC2cRule(String pAgent, String customerId, String command,
                                   long amount, Common.BuildLog log) {
        JsonObject jsonResult = new JsonObject();
        try {
            Connection conn = connectDB.getConnect();
            if (conn == null) {
                conn = connectDB.getConnect();
            }
            log.add("desc", "call STORE ");
            CallableStatement cs = conn.prepareCall("{call PRO_C2C_RULE(?,?,?,?,?,?,?,?,?)}");

            cs.setString(1, pAgent);
            cs.setString(2, customerId);
            cs.setString(3, command);
            cs.setLong(4, amount);

            cs.registerOutParameter(5, OracleTypes.NUMBER);
            cs.registerOutParameter(6, OracleTypes.NUMBER);
            cs.registerOutParameter(7, OracleTypes.NUMBER);
            cs.registerOutParameter(8, OracleTypes.VARCHAR);
            cs.registerOutParameter(9, OracleTypes.NUMBER);


            cs.execute();
//            ResultSet res = (ResultSet) cs.getObject(5);
//            while (res.next()) {
//                result = res.getBoolean("RESULT");
//            }

            long p_result_amt = cs.getLong(5);
            long p_result_cnt = cs.getLong(6);
            long p_result_out_limit = cs.getLong(7);
            String p_result_type = cs.getString(8);
            long p_result_cnt_cus = cs.getLong(9);

            jsonResult.putNumber("p_result_amt", p_result_amt);
            jsonResult.putNumber("p_result_cnt", p_result_cnt);
            jsonResult.putNumber("p_result_out_limit", p_result_out_limit);
            jsonResult.putString("p_result_type", p_result_type);
            jsonResult.putNumber("p_result_cnt_cus", p_result_cnt_cus);
            log.add("jsonResult", jsonResult);
//            log.writeLog();
            conn.close();
        } catch (Exception e) {
            jsonResult = null;
        }
        return jsonResult;
    }

    public int checkMMTAgent(String number, Logger log) {

        String query = "select case when agm.AGID is not null and agm.AGID = 19112 then 0 " +
                "    else 1 end MMTFLAG " +
                "from UMARKETADM.AGENT_REF ar " +
                "left join UMARKETADM.AGENT_GROUP_MAP agm on agm.AGENTID = ar.BODYID and agm.AGID=19112 " +
                "where ar.REFERENCE = '" + number + "'";

        int result = 0;

        try {
            Connection cnn = connectDB.getConnect();
            Statement preparedStatement = cnn.createStatement();
            ResultSet res = preparedStatement.executeQuery(query);

            while (res.next()) {
                result = res.getInt("MMTFLAG");
            }
            cnn.close();
        } catch (Exception e) {
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            log.error("select case when agm.AGID is not null " + errorDesc);
            logger.error("function checkMMTAgent " + errorDesc, e);
        }

        return result;
    }

    /**
     * Query thông tin ĐGD từ table mis_profile
     *
     * @param number
     * @param log
     * @return
     */
    public MStore checkStoreStatus(String number, Logger log) {

        String query = "select ID, PREFERENCE, case when STATUS = 7 then 0  when STATUS = 6 then 1  else 2 end AS STATUS from MIS_PROFILE " +
                "WHERE PREFERENCE = '" + number + "'";

        Connection cnn = null;
        Statement preparedStatement = null;
        ResultSet res = null;
        MStore store = new MStore();
        store.ID = -1; // NOT FOUND

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.createStatement();
            res = preparedStatement.executeQuery(query);

            while (res.next()) {
                store = new MStore();
                store.ID = res.getInt("ID");
                store.MOMO_NUMBER = res.getString("PREFERENCE");
                store.STATUS = res.getInt("STATUS");
            }
        } catch (Exception e) {
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            log.error("select from MIS_PROFILE is null " + errorDesc);
            logger.error("function checkStoreInfo " + errorDesc, e);
        } finally {
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return store;
    }

    public boolean checkDCNTT(String number, Logger log, String dcnttGroups) {

        String query = "select count(*) SL " +
                "FROM umarketadm.agent_ref ar " +
                "    INNER JOIN umarketadm.agent_group_map agm ON ar.bodyid = agm.AGENTID " +
                "    left join umarketadm.agent_group ag on ag.ID = agm.agid " +
                "    left join umarketadm.AGENT_BODY ab on ab.ID = agm.AGENTID " +
                "WHERE " +
                "ab.status = 3 " + // dang hoat dong
                "AND agm.agid IN (" + dcnttGroups + ") " +
                "AND reference = '" + number + "'";

        Connection cnn = null;
        Statement preparedStatement = null;
        ResultSet res = null;
        boolean result = false; // NOT FOUND

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.createStatement();
            res = preparedStatement.executeQuery(query);

            while (res.next()) {
                result = res.getInt("SL") > 0;
            }
        } catch (Exception e) {
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            log.error("Check DCNTT is error " + errorDesc);
            logger.error("function checkDCNTT " + errorDesc, e);
        } finally {
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public Set<Integer> getAgentGroupId(String number, Logger log) {

        String query = "select agm.agid as groupId " +
                "FROM umarketadm.agent_ref ar " +
                "    INNER JOIN umarketadm.agent_group_map agm ON ar.bodyid = agm.AGENTID " +
                "    left join umarketadm.agent_group ag on ag.ID = agm.agid " +
                "    left join umarketadm.AGENT_BODY ab on ab.ID = agm.AGENTID " +
                "WHERE " +
                "ab.status = 3 " + // dang hoat dong
                "AND reference = '" + number + "' " +
                "GROUP BY agm.agid";

        Connection cnn = null;
        Statement preparedStatement = null;
        ResultSet res = null;
        Set<Integer> groups = new HashSet<>();

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.createStatement();
            res = preparedStatement.executeQuery(query);

            while (res.next()) {
                groups.add(res.getInt("groupId"));
            }
            logger.info("Agent " + number + " has group: " + StringUtils.join(groups, ","));
        } catch (Exception e) {
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            logger.error("function getAgentGroupId " + errorDesc, e);
        } finally {
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return groups;
    }

    public JsonArray getInfoBankUser(String number, String personalId, Logger log) {

        String query = "Select CORE_BANK_CODE, BANK_NAME, PHONE_NUMBER, PERSONAL_ID, CREATE_TIME, DELETE_TIME " +
                "from BANK_CUSTOMER " +
                "where BANK_CUSTOMER.PHONE_NUMBER = '" + number + "' " +
                            "OR BANK_CUSTOMER.PERSONAL_ID = '" + personalId + "'";

        Connection cnn = null;
        Statement preparedStatement = null;
        ResultSet res = null;
        JsonObject joData = null;
        JsonArray jArrBankUsers = new JsonArray();

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.createStatement();
            res = preparedStatement.executeQuery(query);

            while (res.next()) {
                joData = new JsonObject();
                joData.putString(colName.PhoneDBCols.BANK_CODE, nullToDefault(res.getString("CORE_BANK_CODE"), ""));
                joData.putString(colName.PhoneDBCols.BANK_NAME,nullToDefault(res.getString("BANK_NAME"), ""));
                joData.putString(colName.PhoneDBCols.NUMBER,nullToDefault(res.getString("PHONE_NUMBER"), ""));
                joData.putString(colName.PhoneDBCols.BANK_PERSONAL_ID,nullToDefault(res.getString("PERSONAL_ID"), ""));
                joData.putNumber(colName.MappingWalletBank.MAPPING_TIME, nullToDefault(res.getTimestamp("CREATE_TIME"), 0L));
                joData.putNumber(colName.MappingWalletBank.UNMAPPING_TIME, nullToDefault(res.getTimestamp("DELETE_TIME"), 0L));
                jArrBankUsers.add(joData);
            }
            logger.info("info BANK USER " + jArrBankUsers.toString());
        } catch (Exception e) {
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            logger.error("function getAgentGroupId " + errorDesc, e);
        } finally {
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return jArrBankUsers;
    }

    protected void selectStoreNearest_new(MStoreNearestRequest msg, JsonObject config, Logger log) {
        List<MStore> stores = new ArrayList<MStore>();
        msg.resultCode = -1; // not found
        msg.resultDesc = "Not found";
        msg.resultData = stores;
        // Check keyword
        if (checkSQLInjection(msg.searchValue)) {
            return;
        }
        if (msg.pageNum <= 0) { //if app not set page number, default is 1
            msg.pageNum = 1;
        }
        String searchReg = completedKeyword(msg.searchValue);
        int offest = ((msg.pageNum - 1) * msg.pageSize);

        // Condition
        String searchLC = Misc.removeAccent(searchReg).toLowerCase();
        List<String> condition = new ArrayList<String>();
        if (StringUtils.isNotEmpty(searchReg)) {
            condition.add(" ex.COMPANY_NAME_NOR LIKE ? ");
            condition.add("  c.NAME_NOR LIKE ? ");
            condition.add("  d.NAME_NOR LIKE ? ");
            condition.add("  w.NAME_NOR LIKE ? ");
            condition.add("  ex.HOUSE_NUM_NOR LIKE ? ");
            condition.add("  ex.STREET_NOR LIKE ? ");

            if (checkSearchComplex(searchReg)) {
                condition.add("  ex.COMPANY_NAME_NOR||' '||ex.HOUSE_NUM_NOR||' '||ex.STREET_NOR||' '||w.NAME_NOR_FULL||' '||d.NAME_NOR_FULL||' '||c.NAME_NOR_FULL LIKE ? ");
            }

            if (searchReg.matches("\\d+")) {
                condition.add("  z.HOME_PHONE = ? ");
                condition.add("  z.PREFERENCE = ? ");
            }
        }
        String sqlCondition = "";
        if (condition.size() > 0) {
            sqlCondition = " AND (" + StringUtils.join(condition, " OR ") + ")";
        }

        /* Build query statement */
        StringBuilder sql = new StringBuilder("SELECT * " +
                "FROM (" +
                "  SELECT ID, NAME, OWNER_ID, LATITUDE, LONGITUDE, MOMO_NUMBER, CONTACT_NUMBER, ADDRESS, STREET, WARD_ID, WARD, DISTRICT_ID, DISTRICT, CITY_ID, CITY, AREA_ID, AREA, STATUS, DISTANCE, ROWNUM AS R" +
                "  FROM (" +
                "    SELECT ID, NAME, OWNER_ID, LATITUDE, LONGITUDE, MOMO_NUMBER, CONTACT_NUMBER, ADDRESS, STREET, WARD_ID, WARD, DISTRICT_ID, DISTRICT, CITY_ID, CITY, AREA_ID, AREA, STATUS, DISTANCE" +
                "      FROM (" +
                "       SELECT z.ID, z.COMPANY_NAME as NAME, z.OWNER_ID, z.PREFERENCE AS MOMO_NUMBER, z.HOME_PHONE AS CONTACT_NUMBER, z.HOUSE_NUM||' '||z.STREET AS  ADDRESS, z.STREET, z.WARD_ID, w.NAME AS WARD, " +
                "              z.DISTRICT_ID, d.NAME AS DISTRICT, z.CITY_ID, c.NAME AS CITY, z.AREA_ID, a.NAME AS AREA," +
                "              z.STATUS, ex.LAT_FLOAT as LATITUDE, ex.LON_FLOAT AS LONGITUDE," +
                "              p.radius," +
                "              p.distance_unit" +
                "                 * haversine(ex.LAT_FLOAT,   ex.LON_FLOAT," +
                "                             latpoint,   longpoint) AS DISTANCE" +
                "        FROM " +
                "          MIS_PROFILE z" +
                "          INNER JOIN MIS_PROFILE_EXTRA ex ON z.ID = ex.ID" +
                "          JOIN (" +
                "                SELECT  ").append(msg.lat).append("  AS latpoint,   ").append(msg.lon).append("  AS longpoint," +
                "                        2000.0 AS radius,        111.045 AS distance_unit," +
                "                        0.0174532925 AS deg2rad" +
                "                FROM  MIS_DUAL" +
                "            ) p ON 1=1" +
                "          JOIN MIS_CITY c on c.ID = z.CITY_ID" +
                "          JOIN MIS_WARD w on w.ID = z.WARD_ID" +
                "          JOIN MIS_DISTRICT d on d.ID = z.DISTRICT_ID" +
                "          JOIN MIS_AREA a on a.ID = z.AREA_ID" +
                "        WHERE    " +
                "           z.STATUS = 7 AND z.MAP_FLAG = 1 " +
                "           AND z.PROPERTY_ID IN (0, 1, 4) AND profile_parent_id IS NULL " +
                "           AND ex.LAT_FLOAT >= " + config.getNumber("lat_min") +
                "           AND ex.LAT_FLOAT <= " + config.getNumber("lat_max") +
                "           AND ex.LON_FLOAT >= " + config.getNumber("lng_min") +
                "           AND ex.LON_FLOAT <= " + config.getNumber("lng_max") +
                "           AND ex.LAT_FLOAT" +
                "             BETWEEN p.latpoint  - (p.radius / p.distance_unit)" +
                "                 AND p.latpoint  + (p.radius / p.distance_unit)" +
                "           AND ex.LON_FLOAT" +
                "             BETWEEN p.longpoint - (p.radius / (p.distance_unit * COS(deg2rad * (p.latpoint))))" +
                "                 AND p.longpoint + (p.radius / (p.distance_unit * COS(deg2rad * (p.latpoint))))" +
                sqlCondition +
                "       )" +
                "     ORDER BY DISTANCE" +
                "    )" +
                "  WHERE ROWNUM <= " + (offest + msg.pageSize) +
                ") " +
                "WHERE R > ").append(offest);
        log.info("SQL=" + sql.toString());
        Connection cnn = null;
        PreparedStatement preparedStatement = null;
        ResultSet res = null;

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.prepareStatement(sql.toString());

            // Set params
            if (StringUtils.isNotEmpty(searchReg)) {
                preparedStatement.setString(1, "%" + searchLC + "%");
                preparedStatement.setString(2, "%" + searchLC + "%");
                preparedStatement.setString(3, "%" + searchLC + "%");
                preparedStatement.setString(4, "%" + searchLC + "%");
                preparedStatement.setString(5, "%" + searchLC + "%");
                preparedStatement.setString(6, "%" + searchLC + "%");
                if (checkSearchComplex(searchReg)) {
                    preparedStatement.setString(7, "%" + searchLC + "%");
                }
                if (searchReg.matches("\\d+")) {
                    preparedStatement.setString(8, searchReg);
                    preparedStatement.setString(9, searchReg);
                }
            }

            res = preparedStatement.executeQuery();

            while (res.next()) {
                MStore store = new MStore();
                store.ID = res.getInt("ID");
                store.NAME = res.getString("NAME");
                store.OWNER_ID = res.getLong("OWNER_ID");
                store.LATITUDE = res.getDouble("LATITUDE");
                store.LONGITUDE = res.getDouble("LONGITUDE");
                store.MOMO_NUMBER = res.getString("MOMO_NUMBER");
                store.CONTACT_NUMBER = res.getString("CONTACT_NUMBER");
                store.ADDRESS = res.getString("ADDRESS");
                store.STREET = res.getString("STREET");
                store.WARD_ID = res.getInt("WARD_ID");
                store.WARD = res.getString("WARD");
                store.DISTRICT_ID = res.getInt("DISTRICT_ID");
                store.DISTRICT = res.getString("DISTRICT");
                store.CITY_ID = res.getInt("CITY_ID");
                store.CITY = res.getString("CITY");
                store.AREA_ID = res.getInt("AREA_ID");
                store.AREA = res.getString("AREA");
                store.STATUS = res.getInt("STATUS");
                store.DISTANCE = res.getDouble("DISTANCE");
                stores.add(store);
            }
        } catch (Exception e) {
            msg.resultCode = -2; // system error
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            msg.resultDesc = errorDesc;
            log.error("select nearest from MIS_PROFILE is null " + errorDesc);
            logger.error("function selectStoreNearest " + errorDesc, e);
        } finally {
            if (stores.size() > 0) {
                msg.resultCode = 0; // ok
                msg.resultDesc = "";
            }
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void selectStoreNearest(MStoreNearestRequest msg, JsonObject config, Logger log) {
        List<MStore> stores = new ArrayList<MStore>();
        msg.resultCode = -1; // not found
        msg.resultDesc = "Not found";
        msg.resultData = stores;
        // Check keyword
        if (checkSQLInjection(msg.searchValue)) {
            return;
        }
        if (msg.pageNum <= 0) { //if app not set page number, default is 1
            msg.pageNum = 1;
        }
        String searchReg = completedKeyword(msg.searchValue);
        int offest = ((msg.pageNum - 1) * msg.pageSize);

        // Condition
        List<String> condition = new ArrayList<String>();
        if (StringUtils.isNotEmpty(searchReg)) {
            condition.add(" FN_CONVERT_TO_VN(z.COMPANY_NAME) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(REGEXP_REPLACE(c.NAME, '(Tỉnh|Thành Phố)(:)*', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(REGEXP_REPLACE(d.NAME, '(Huyện|Quận|Thành phố|Thị xã)(:)*', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(REGEXP_REPLACE(w.NAME, '(Phường|Xã|Thị trấn)(:)*', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(z.HOUSE_NUM) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(z.STREET) = '" + Misc.removeAccent(searchReg).toLowerCase() + "' ");

            if (checkSearchComplex(searchReg)) {
                condition.add("  FN_CONVERT_TO_VN(z.COMPANY_NAME||' '||z.HOUSE_NUM||' '||z.STREET||' '||REGEXP_REPLACE(w.NAME, ':', '')||' '||REGEXP_REPLACE(d.NAME, ':', '')||' '||REGEXP_REPLACE(c.NAME, ':', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            }

            condition.add("  z.HOME_PHONE = '" + searchReg + "' ");
            condition.add("  z.PREFERENCE = '" + searchReg + "' ");
        }
        String sqlCondition = "";
        if (condition.size() > 0) {
            sqlCondition = " AND (" + StringUtils.join(condition, " OR ") + ")";
        }

        /* Build query statement */
        StringBuilder sql = new StringBuilder("SELECT * " +
                "FROM (" +
                "  SELECT ID, NAME, OWNER_ID, LATITUDE, LONGITUDE, MOMO_NUMBER, CONTACT_NUMBER, ADDRESS, STREET, WARD_ID, WARD, DISTRICT_ID, DISTRICT, CITY_ID, CITY, AREA_ID, AREA, STATUS, DISTANCE, ROWNUM AS R" +
                "  FROM (" +
                "    SELECT ID, NAME, OWNER_ID, LATITUDE, LONGITUDE, MOMO_NUMBER, CONTACT_NUMBER, ADDRESS, STREET, WARD_ID, WARD, DISTRICT_ID, DISTRICT, CITY_ID, CITY, AREA_ID, AREA, STATUS, DISTANCE" +
                "      FROM (" +
                "       SELECT z.ID, z.COMPANY_NAME as NAME, z.OWNER_ID, z.PREFERENCE AS MOMO_NUMBER, z.HOME_PHONE AS CONTACT_NUMBER, z.HOUSE_NUM||' '||z.STREET AS  ADDRESS, z.STREET, z.WARD_ID, w.NAME AS WARD, " +
                "              z.DISTRICT_ID, d.NAME AS DISTRICT, z.CITY_ID, c.NAME AS CITY, z.AREA_ID, a.NAME AS AREA," +
                "              z.STATUS, geo_atof(z.LAT) as LATITUDE, geo_atof(z.LON) AS LONGITUDE," +
                "              p.radius," +
                "              p.distance_unit" +
                "                 * haversine(geo_atof(z.LAT),   geo_atof(z.LON)," +
                "                             latpoint,   longpoint) AS DISTANCE" +
                "        FROM " +
                "          MIS_PROFILE z" +
                "          JOIN (" +
                "                SELECT  ").append(msg.lat).append("  AS latpoint,   ").append(msg.lon).append("  AS longpoint," +
                "                        1800.0 AS radius,        111.045 AS distance_unit," +
                "                        0.0174532925 AS deg2rad" +
                "                FROM  MIS_DUAL" +
                "            ) p ON 1=1" +
                "          JOIN MIS_CITY c on c.ID = z.CITY_ID" +
                "          JOIN MIS_WARD w on w.ID = z.WARD_ID" +
                "          JOIN MIS_DISTRICT d on d.ID = z.DISTRICT_ID" +
                "          JOIN MIS_AREA a on a.ID = z.AREA_ID" +
                "        WHERE    " +
                "           z.STATUS = 7 AND z.MAP_FLAG = 1 " +
                "           AND z.PROPERTY_ID IN (0, 1, 4) AND profile_parent_id IS NULL " +
                "           AND geo_atof(z.LAT) >= " + config.getNumber("lat_min") +
                "           AND geo_atof(z.LAT) <= " + config.getNumber("lat_max") +
                "           AND geo_atof(z.LON) >= " + config.getNumber("lng_min") +
                "           AND geo_atof(z.LON) <= " + config.getNumber("lng_max") +
                "           AND geo_atof(z.LAT)" +
                "             BETWEEN p.latpoint  - (p.radius / p.distance_unit)" +
                "                 AND p.latpoint  + (p.radius / p.distance_unit)" +
                "           AND geo_atof(z.LON)" +
                "             BETWEEN p.longpoint - (p.radius / (p.distance_unit * COS(deg2rad * (p.latpoint))))" +
                "                 AND p.longpoint + (p.radius / (p.distance_unit * COS(deg2rad * (p.latpoint))))" +
                sqlCondition +
                "       )" +
                "     ORDER BY DISTANCE" +
                "    )" +
                "  WHERE ROWNUM <= " + (offest + msg.pageSize) +
                ") " +
                "WHERE R > ").append(offest);
        log.info("SQL=" + sql.toString());
        Connection cnn = null;
        Statement preparedStatement = null;
        ResultSet res = null;

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.createStatement();
            res = preparedStatement.executeQuery(sql.toString());

            while (res.next()) {
                MStore store = new MStore();
                store.ID = res.getInt("ID");
                store.NAME = res.getString("NAME");
                store.OWNER_ID = res.getLong("OWNER_ID");
                store.LATITUDE = res.getDouble("LATITUDE");
                store.LONGITUDE = res.getDouble("LONGITUDE");
                store.MOMO_NUMBER = res.getString("MOMO_NUMBER");
                store.CONTACT_NUMBER = res.getString("CONTACT_NUMBER");
                store.ADDRESS = res.getString("ADDRESS");
                store.STREET = res.getString("STREET");
                store.WARD_ID = res.getInt("WARD_ID");
                store.WARD = res.getString("WARD");
                store.DISTRICT_ID = res.getInt("DISTRICT_ID");
                store.DISTRICT = res.getString("DISTRICT");
                store.CITY_ID = res.getInt("CITY_ID");
                store.CITY = res.getString("CITY");
                store.AREA_ID = res.getInt("AREA_ID");
                store.AREA = res.getString("AREA");
                store.STATUS = res.getInt("STATUS");
                store.DISTANCE = res.getDouble("DISTANCE");
                stores.add(store);
            }
        } catch (Exception e) {
            msg.resultCode = -2; // system error
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            msg.resultDesc = errorDesc;
            log.error("select nearest from MIS_PROFILE is null " + errorDesc);
            logger.error("function selectStoreNearest " + errorDesc, e);
        } finally {
            if (stores.size() > 0) {
                msg.resultCode = 0; // ok
                msg.resultDesc = "";
            }
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void selectStore_new(MStoreNearestRequest msg, JsonObject config, Logger log) {
        List<MStore> stores = new ArrayList<MStore>();
        msg.resultCode = -1; // not found
        msg.resultDesc = "Not found";
        msg.resultData = stores;
        // Check keyword
        if (checkSQLInjection(msg.searchValue)) {
            return;
        }
        if (msg.pageNum <= 0) { //if app not set page number, default is 1
            msg.pageNum = 1;
        }
        String searchReg = completedKeyword(msg.searchValue);
        int offest = ((msg.pageNum - 1) * msg.pageSize);

        // Condition
        String searchLC = Misc.removeAccent(searchReg).toLowerCase();
        List<String> condition = new ArrayList<String>();
        if (StringUtils.isNotEmpty(searchReg)) {
            condition.add(" ex.COMPANY_NAME_NOR LIKE ? ");
            condition.add("  c.NAME_NOR LIKE ? ");
            condition.add("  d.NAME_NOR LIKE ? ");
            condition.add("  w.NAME_NOR LIKE ? ");
            condition.add("  ex.HOUSE_NUM_NOR LIKE ? ");
            condition.add("  ex.STREET_NOR LIKE ? ");

            if (checkSearchComplex(searchReg)) {
                condition.add("  ex.COMPANY_NAME_NOR||' '||ex.HOUSE_NUM_NOR||' '||ex.STREET_NOR||' '||w.NAME_NOR_FULL||' '||d.NAME_NOR_FULL||' '||c.NAME_NOR_FULL LIKE ? ");
            }

            if (searchReg.matches("\\d+")) {
                condition.add("  z.HOME_PHONE = ? ");
                condition.add("  z.PREFERENCE = ? ");
            }
        }
        String sqlCondition = "";
        if (condition.size() > 0) {
            sqlCondition = " AND (" + StringUtils.join(condition, " OR ") + ")";
        }

        /* Build query statement */
        StringBuilder sql = new StringBuilder("SELECT * " +
                "FROM (" +
                "  SELECT ID, NAME, OWNER_ID, LATITUDE, LONGITUDE, MOMO_NUMBER, CONTACT_NUMBER, ADDRESS, STREET, WARD_ID, WARD, DISTRICT_ID, DISTRICT, CITY_ID, CITY, AREA_ID, AREA, STATUS, ROWNUM AS R" +
                "  FROM (" +
                "       SELECT z.ID, z.COMPANY_NAME as NAME, z.OWNER_ID, z.PREFERENCE AS MOMO_NUMBER, z.HOME_PHONE AS CONTACT_NUMBER, z.HOUSE_NUM||' '||z.STREET AS ADDRESS, z.STREET, z.WARD_ID, w.NAME AS WARD, " +
                "              z.DISTRICT_ID, d.NAME AS DISTRICT, z.CITY_ID, c.NAME AS CITY, z.AREA_ID, a.NAME AS AREA," +
                "              z.STATUS, ex.LAT_FLOAT as LATITUDE, ex.LON_FLOAT AS LONGITUDE" +
                "        FROM " +
                "          MIS_PROFILE z" +
                "          INNER JOIN MIS_PROFILE_EXTRA ex ON z.ID = ex.ID" +
                "          JOIN MIS_CITY c on c.ID = z.CITY_ID" +
                "          JOIN MIS_WARD w on w.ID = z.WARD_ID" +
                "          JOIN MIS_DISTRICT d on d.ID = z.DISTRICT_ID" +
                "          JOIN MIS_AREA a on a.ID = z.AREA_ID" +
                "        WHERE    " +
                "           z.STATUS = 7 AND z.MAP_FLAG = 1 " +
                "           AND z.PROPERTY_ID IN (0, 1, 4) AND profile_parent_id IS NULL " +
                "           AND ex.LAT_FLOAT >= " + config.getNumber("lat_min") +
                "           AND ex.LAT_FLOAT <= " + config.getNumber("lat_max") +
                "           AND ex.LON_FLOAT >= " + config.getNumber("lng_min") +
                "           AND ex.LON_FLOAT <= " + config.getNumber("lng_max") +
                sqlCondition +
                "    )" +
                "  WHERE ROWNUM <= " + (offest + msg.pageSize) +
                ") " +
                "WHERE R > ").append(offest);
        log.info("SQL=" + sql.toString());
        Connection cnn = null;
        PreparedStatement preparedStatement = null;
        ResultSet res = null;

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.prepareStatement(sql.toString());

            // Set params
            if (StringUtils.isNotEmpty(searchReg)) {
                preparedStatement.setString(1, "%" + searchLC + "%");
                preparedStatement.setString(2, "%" + searchLC + "%");
                preparedStatement.setString(3, "%" + searchLC + "%");
                preparedStatement.setString(4, "%" + searchLC + "%");
                preparedStatement.setString(5, "%" + searchLC + "%");
                preparedStatement.setString(6, "%" + searchLC + "%");
                if (checkSearchComplex(searchReg)) {
                    preparedStatement.setString(7, "%" + searchLC + "%");
                }
                if (searchReg.matches("\\d+")) {
                    preparedStatement.setString(8, searchReg);
                    preparedStatement.setString(9, searchReg);
                }
            }

            res = preparedStatement.executeQuery();

            while (res.next()) {
                MStore store = new MStore();
                store.ID = res.getInt("ID");
                store.NAME = res.getString("NAME");
                store.OWNER_ID = res.getLong("OWNER_ID");
                store.LATITUDE = res.getDouble("LATITUDE");
                store.LONGITUDE = res.getDouble("LONGITUDE");
                store.MOMO_NUMBER = res.getString("MOMO_NUMBER");
                store.CONTACT_NUMBER = res.getString("CONTACT_NUMBER");
                store.ADDRESS = res.getString("ADDRESS");
                store.STREET = res.getString("STREET");
                store.WARD_ID = res.getInt("WARD_ID");
                store.WARD = res.getString("WARD");
                store.DISTRICT_ID = res.getInt("DISTRICT_ID");
                store.DISTRICT = res.getString("DISTRICT");
                store.CITY_ID = res.getInt("CITY_ID");
                store.CITY = res.getString("CITY");
                store.AREA_ID = res.getInt("AREA_ID");
                store.AREA = res.getString("AREA");
                store.STATUS = res.getInt("STATUS");
                store.DISTANCE = -1;
                stores.add(store);
            }
        } catch (Exception e) {
            msg.resultCode = -2; // system error
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            msg.resultDesc = errorDesc;
            log.error("select store from MIS_PROFILE is null " + errorDesc);
            logger.error("function selectStore " + errorDesc, e);
        } finally {
            if (stores.size() > 0) {
                msg.resultCode = 0; // ok
                msg.resultDesc = "";
            }
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void selectStore(MStoreNearestRequest msg, JsonObject config, Logger log) {
        List<MStore> stores = new ArrayList<MStore>();
        msg.resultCode = -1; // not found
        msg.resultDesc = "Not found";
        msg.resultData = stores;
        // Check keyword
        if (checkSQLInjection(msg.searchValue)) {
            return;
        }
        if (msg.pageNum <= 0) { //if app not set page number, default is 1
            msg.pageNum = 1;
        }
        String searchReg = completedKeyword(msg.searchValue);
        int offest = ((msg.pageNum - 1) * msg.pageSize);

        // Condition
        List<String> condition = new ArrayList<String>();
        if (StringUtils.isNotEmpty(searchReg)) {
            condition.add(" FN_CONVERT_TO_VN(z.COMPANY_NAME) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(REGEXP_REPLACE(c.NAME, '(Tỉnh|Thành Phố)(:)*', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(REGEXP_REPLACE(d.NAME, '(Huyện|Quận|Thành phố|Thị xã)(:)*', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(REGEXP_REPLACE(w.NAME, '(Phường|Xã|Thị trấn)(:)*', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(z.HOUSE_NUM) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            condition.add("  FN_CONVERT_TO_VN(z.STREET) = '" + Misc.removeAccent(searchReg).toLowerCase() + "' ");

            if (checkSearchComplex(searchReg)) {
                condition.add("  FN_CONVERT_TO_VN(z.COMPANY_NAME||' '||z.HOUSE_NUM||' '||z.STREET||' '||REGEXP_REPLACE(w.NAME, ':', '')||' '||REGEXP_REPLACE(d.NAME, ':', '')||' '||REGEXP_REPLACE(c.NAME, ':', '')) LIKE '%" + Misc.removeAccent(searchReg).toLowerCase() + "%' ");
            }

            condition.add("  z.HOME_PHONE = '" + searchReg + "' ");
            condition.add("  z.PREFERENCE = '" + searchReg + "' ");
        }
        String sqlCondition = "";
        if (condition.size() > 0) {
            sqlCondition = " AND (" + StringUtils.join(condition, " OR ") + ")";
        }

        /* Build query statement */
        StringBuilder sql = new StringBuilder("SELECT * " +
                "FROM (" +
                "  SELECT ID, NAME, OWNER_ID, LATITUDE, LONGITUDE, MOMO_NUMBER, CONTACT_NUMBER, ADDRESS, STREET, WARD_ID, WARD, DISTRICT_ID, DISTRICT, CITY_ID, CITY, AREA_ID, AREA, STATUS, ROWNUM AS R" +
                "  FROM (" +
                "       SELECT z.ID, z.COMPANY_NAME as NAME, z.OWNER_ID, z.PREFERENCE AS MOMO_NUMBER, z.HOME_PHONE AS CONTACT_NUMBER, z.HOUSE_NUM||' '||z.STREET AS ADDRESS, z.STREET, z.WARD_ID, w.NAME AS WARD, " +
                "              z.DISTRICT_ID, d.NAME AS DISTRICT, z.CITY_ID, c.NAME AS CITY, z.AREA_ID, a.NAME AS AREA," +
                "              z.STATUS, geo_atof(z.LAT) as LATITUDE, geo_atof(z.LON) AS LONGITUDE" +
                "        FROM " +
                "          MIS_PROFILE z" +
                "          JOIN MIS_CITY c on c.ID = z.CITY_ID" +
                "          JOIN MIS_WARD w on w.ID = z.WARD_ID" +
                "          JOIN MIS_DISTRICT d on d.ID = z.DISTRICT_ID" +
                "          JOIN MIS_AREA a on a.ID = z.AREA_ID" +
                "        WHERE    " +
                "           z.STATUS = 7 AND z.MAP_FLAG = 1 " +
                "           AND z.PROPERTY_ID IN (0, 1, 4) AND profile_parent_id IS NULL " +
                "           AND geo_atof(z.LAT) >= " + config.getNumber("lat_min") +
                "           AND geo_atof(z.LAT) <= " + config.getNumber("lat_max") +
                "           AND geo_atof(z.LON) >= " + config.getNumber("lng_min") +
                "           AND geo_atof(z.LON) <= " + config.getNumber("lng_max") +
                sqlCondition +
                "    )" +
                "  WHERE ROWNUM <= " + (offest + msg.pageSize) +
                ") " +
                "WHERE R > ").append(offest);
        log.info("SQL=" + sql.toString());
        Connection cnn = null;
        Statement preparedStatement = null;
        ResultSet res = null;

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.createStatement();
            res = preparedStatement.executeQuery(sql.toString());

            while (res.next()) {
                MStore store = new MStore();
                store.ID = res.getInt("ID");
                store.NAME = res.getString("NAME");
                store.OWNER_ID = res.getLong("OWNER_ID");
                store.LATITUDE = res.getDouble("LATITUDE");
                store.LONGITUDE = res.getDouble("LONGITUDE");
                store.MOMO_NUMBER = res.getString("MOMO_NUMBER");
                store.CONTACT_NUMBER = res.getString("CONTACT_NUMBER");
                store.ADDRESS = res.getString("ADDRESS");
                store.STREET = res.getString("STREET");
                store.WARD_ID = res.getInt("WARD_ID");
                store.WARD = res.getString("WARD");
                store.DISTRICT_ID = res.getInt("DISTRICT_ID");
                store.DISTRICT = res.getString("DISTRICT");
                store.CITY_ID = res.getInt("CITY_ID");
                store.CITY = res.getString("CITY");
                store.AREA_ID = res.getInt("AREA_ID");
                store.AREA = res.getString("AREA");
                store.STATUS = res.getInt("STATUS");
                store.DISTANCE = -1;
                stores.add(store);
            }
        } catch (Exception e) {
            msg.resultCode = -2; // system error
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            msg.resultDesc = errorDesc;
            log.error("select nearest from MIS_PROFILE is null " + errorDesc);
            logger.error("function selectStoreNearest " + errorDesc, e);
        } finally {
            if (stores.size() > 0) {
                msg.resultCode = 0; // ok
                msg.resultDesc = "";
            }
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int PRO_COUNT_ACCCOUNT(String P_CMND, String P_NUMBER) {

        int result = 0;
        Connection cnn = connectDB.getConnect();
        try {

            CallableStatement cs = cnn.prepareCall("{call PRO_COUNT_ACCCOUNT(?,?,?)}");

            cs.setString(1, P_CMND);
            cs.setString(2, P_NUMBER);
            cs.registerOutParameter(3, OracleTypes.NUMBER);

            cs.execute();
            result = cs.getInt(3);

        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("function PRO_COUNT_ACCCOUNT " + errorDesc, e);
            logger.error("co loi tai call PRO_COUNT_ACCCOUNT(?,?) of CMND: " + P_CMND + " - " + errorDesc);
            result = 100;
        } finally {
            try {
                cnn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public int PRO_CHECK_ACCCOUNT(String P_CMND, String P_NUMBER) {

        int result = 0;
        Connection cnn = connectDB.getConnect();
        try {

            CallableStatement cs = cnn.prepareCall("{call PRO_CHECK_ACCCOUNT(?,?)}");

            cs.setString(1, P_CMND);
            cs.setString(2, P_NUMBER);
            cs.registerOutParameter(3, OracleTypes.VARCHAR);

            cs.execute();
            result = cs.getInt(3);

        } catch (Exception e) {
            String errorDesc = (e.getMessage() == null ? "null" : e.getMessage());
            logger.error("function PRO_CHECK_ACCCOUNT " + errorDesc, e);
            logger.error("co loi tai call PRO_CHECK_ACCCOUNT(?,?) of CMND: " + P_CMND + " - " + errorDesc);

        } finally {
            try {
                cnn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean insertMerchantTran(MerTranInfo request, Logger log) {
        boolean result = false;

        StringBuilder sql = new StringBuilder("INSERT INTO LOT_TRANSHISTORY(ID, CREATED_DATE, TID, STORE_ID, STAFF_ID, PHONE_NUMBER, AMOUNT, FEE, RECEIVED_AMOUNT, STATUS, REFCODE, ERROR_CODE, DESCRIPTION, PAYMENTCODE, VOUCHERCODE) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        log.info("SQL=" + sql.toString());
        Connection cnn = null;
        PreparedStatement preparedStatement = null;

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.prepareStatement(sql.toString());

            preparedStatement.setString(1, request.ID);
            preparedStatement.setTimestamp(2, new Timestamp(request.CREATED_DATE));
            preparedStatement.setString(3, request.TID);
            preparedStatement.setInt(4, request.STORE_ID);
            preparedStatement.setInt(5, request.STAFF_ID);
            preparedStatement.setString(6, request.PHONE_NUMBER);
            preparedStatement.setLong(7, request.AMOUNT);
            preparedStatement.setInt(8, request.FEE);
            preparedStatement.setLong(9, request.RECEIVED_AMOUNT);
            preparedStatement.setInt(10, request.STATUS);
            preparedStatement.setString(11, request.REFCODE);
            preparedStatement.setString(12, request.ERROR_CODE);
            preparedStatement.setString(13, request.DESCRIPTION);
            preparedStatement.setString(14, request.PAYMENTCODE);
            preparedStatement.setString(15, request.VOUCHERCODE);

            result = preparedStatement.executeUpdate() > 0 ? true : false;
        } catch (Exception e) {
            result = false;
            log.error("insertMerchantTran error", e);
        } finally {
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public void getMerchantTran(MerTranhis request, Logger log) {

        if (request.pageNum <= 0) {
            request.pageNum = 1;
        }
        if (request.pageSize <= 0) {
            request.pageSize = 10;
        }
        int offest = ((request.pageNum - 1) * request.pageSize);

        StringBuilder sql = new StringBuilder("SELECT * FROM " +
                "(" +
                " SELECT CREATED_DATE, ID, TID, STORE_ID, STAFF_ID, PHONE_NUMBER, AMOUNT, FEE, RECEIVED_AMOUNT, STATUS, REFCODE, ERROR_CODE, DESCRIPTION, PAYMENTCODE, VOUCHERCODE, ROWNUM AS R " +
                " FROM LOT_TRANSHISTORY " +
                " WHERE STAFF_ID = (SELECT ID FROM LOT_ACCOUNT WHERE USERNAME = ? AND ROWNUM <= 1) AND CREATED_DATE >= ? AND CREATED_DATE < ? AND ROWNUM <= ? ORDER BY CREATED_DATE DESC" +
                ") WHERE R > ?");
        log.info("SQL=" + sql.toString());
        Connection cnn = null;
        PreparedStatement preparedStatement = null;
        ResultSet res = null;

        try {
            cnn = connectDB.getConnect();
            preparedStatement = cnn.prepareStatement(sql.toString());

            preparedStatement.setString(1, request.merchantId);
            preparedStatement.setTimestamp(2, new Timestamp(request.beginTime));
            preparedStatement.setTimestamp(3, new Timestamp(request.endTime));
            preparedStatement.setInt(4, offest + request.pageSize);
            preparedStatement.setInt(5, offest);

            res = preparedStatement.executeQuery();

            while (res.next()) {
                MerTranInfo store = new MerTranInfo();
                store.ID = res.getString("ID");
                store.CREATED_DATE = res.getTimestamp("CREATED_DATE").getTime();
                store.TID = res.getString("TID");
                store.STORE_ID = res.getInt("STORE_ID");
                store.STAFF_ID = res.getInt("STAFF_ID");
                store.PHONE_NUMBER = res.getString("PHONE_NUMBER");
                store.AMOUNT = res.getLong("AMOUNT");
                store.FEE = res.getInt("FEE");
                store.RECEIVED_AMOUNT = res.getLong("RECEIVED_AMOUNT");
                store.STATUS = res.getInt("STATUS");
                store.REFCODE = res.getString("REFCODE");
                store.ERROR_CODE = res.getString("ERROR_CODE");
                store.DESCRIPTION = res.getString("DESCRIPTION");
                store.PAYMENTCODE = res.getString("PAYMENTCODE");
                store.VOUCHERCODE = res.getString("VOUCHERCODE");
                request.tranList.add(store);
            }
        } catch (Exception e) {
            log.error("getMerchantTran error", e);
        } finally {
            if (cnn != null) {
                try {
                    cnn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class ObjCoreStatus {


        private static String Reged = "Reged";
        private static String Actived = "Actived";
        private static String Susppended = "Suppended";
        private static String Frozen = "Frozen";
        private static String Stopped = "Stopped";
        //Registered=1, Active=2, Stopped=128, Suspended=8, Frozen=32.
        public boolean isReged = true;
        public boolean isActivated = true;
        public boolean isStopped = false;
        public boolean isSuspended = false;
        public boolean isFrozen = false;
        private int registerMask = 1;
        ;
        private int activeMask = 2;
        private int suspendedMask = 8;
        private int frozenMask = 32;
        private int stoppedMask = 128;

        public ObjCoreStatus() {
        }

        public ObjCoreStatus(int status) {
            isReged = ((status & registerMask) == registerMask);
            isActivated = (status & activeMask) == activeMask;
            isSuspended = (status & suspendedMask) == suspendedMask;
            isFrozen = (status & frozenMask) == frozenMask;
            isStopped = (status & stoppedMask) == stoppedMask;
        }

        public ObjCoreStatus(JsonObject jo) {
            isReged = jo.getBoolean(Reged, false);
            isActivated = jo.getBoolean(Actived, false);
            isSuspended = jo.getBoolean(Susppended, false);
            isFrozen = jo.getBoolean(Frozen, false);
            isStopped = jo.getBoolean(Stopped, false);
        }

        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.putBoolean(Reged, isReged);
            jo.putBoolean(Actived, isActivated);
            jo.putBoolean(Susppended, isSuspended);
            jo.putBoolean(Frozen, isFrozen);
            jo.putBoolean(Stopped, isStopped);
            return jo;
        }
    }
}
