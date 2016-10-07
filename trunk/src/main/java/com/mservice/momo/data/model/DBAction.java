package com.mservice.momo.data.model;

/**
 * Created by duyhuynh on 14/07/2016.
 */
public class DBAction {

    public class Generic {
        public static final String UPDATE = "UPDATE"; // Insert/Update/Delete which only return true/false (both raw sql and prepared statement)
        public static final String QUERY = "QUERY"; // Query which return result set (both raw sql and prepared statement)
        public static final String STORE_PROCEDURE = "STORE_PROCEDURE"; // Store procedure
    }

    public class Noti {
        public static final String UPDATE_NOTI = "UPDATE_NOTI";
        public static final String SAVE_NOTI = "SAVE_NOTI";
        public static final String GET_NOTI_FOR_SYNC = "GET_NOTI_FOR_SYNC";
        public static final String GET_NOTI = "GET_NOTI";
        public static final String REMOVE_OLD_NOTI_BY_TIME = "REMOVE_OLD_NOTI_BY_TIME";
        public static final String REMOVE_OLD_NOTI_BY_USER = "REMOVE_OLD_NOTI_BY_USER";
    }

    public class Tran {

        public static final String DEL_ROWS_TRAN = "DEL_ROWS_TRAN";
        public static final String GET_STATISTIC_TRAN_PER_DAY = "GET_STATISTIC_TRAN_PER_DAY";
        public static final String UPSERT_TRAN_OUTSIDE = "UPSERT_TRAN_OUTSIDE";
        public static final String INSERT_TRAN = "INSERT_TRAN";
        public static final String FIND_TRAN = "FIND_TRAN";
        public static final String FIND_ASC_TRAN = "FIND_ASC_TRAN";
        public static final String FIND_DESC_TRAN = "FIND_DESC_TRAN";
        public static final String COUNT_WITH_FILTER = "COUNT_WITH_FILTER";
        public static final String GET_TRAN_PAGING = "GET_TRAN_PAGING";
        public static final String GET_TRANSACTION_DETAIL = "GET_TRANSACTION_DETAIL";
        public static final String UPDATE_TRAN_STATUS = "UPDATE_TRAN_STATUS";
        public static final String GET_TRAN_EXIST_ON_SERVER = "GET_TRAN_EXIST_ON_SERVER";
        public static final String COUNT_AGENT_TRAN = "COUNT_AGENT_TRAN";
        public static final String GET_TRAN_IN_LIST = "GET_TRAN_IN_LIST";
        public static final String SUM_TRAN_CURR_MONTH = "SUM_TRAN_CURR_MONTH";
        public static final String FIND_ONE_VCB_TRAN = "FIND_ONE_VCB_TRAN";
        public static final String FIND_ONE_FOR_TRANTYPE = "FIND_ONE_FOR_TRANTYPE";
        public static final String FIND_ONE_BILLPAY_AND_PHIM = "FIND_ONE_BILLPAY_AND_PHIM";
        public static final String GET_LAST_TRAN = "GET_LAST_TRAN";
        public static final String GET_TRAN_WITHOUT_TYPE = "GET_TRAN_WITHOUT_TYPE";
        public static final String GET_TRAN_BY_BILL = "GET_TRAN_BY_BILL";
    }
}
