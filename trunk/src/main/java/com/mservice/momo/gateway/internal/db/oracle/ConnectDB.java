package com.mservice.momo.gateway.internal.db.oracle;

import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.vertx.java.core.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Created by concu on 4/19/14.
 */
public class ConnectDB {

    private UniversalConnectionPoolManager mgr = null;
    PoolDataSourceImpl pds = null;

    private Logger logger;

    private String driver ="";
    private String url = "";
    private String user = "";
    private String pass = "";
    private String ucpName="Pool";
    private String poolname = "";

    public ConnectDB(String driver
                        ,String url
                        , String user
                        , String pass
                        , String cnnName
                        , String poolName
                        , Logger log){
        this.driver =driver;
        this.url =url;
        this.user=user;
        this.pass =pass;
        this.ucpName = cnnName;
        this.poolname = poolName;
        logger = log;
        if(!createPool()){

            logger.error("CAN NOT OPEN THE POOL " + cnnName + " - " + poolName);
        }
    }

    private boolean createPool() //String _ucpName)
    {
        //huy ca pool cu
        destroyCon();
        pds = new PoolDataSourceImpl();
        logger.info("create pool: " + poolname);
        logger.info("pool name: " + poolname);
        logger.info(driver);
        logger.info(url);
        logger.info(user);
        logger.info(pass);
        try {


            mgr = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();

            pds.setConnectionPoolName(poolname);
            pds.setConnectionFactoryClassName(driver);
            pds.setURL(url);
            pds.setUser(user);
            pds.setPassword(pass);

            pds.setMinPoolSize(20);
            pds.setMaxPoolSize(30);

            pds.setMaxConnectionReuseTime(600);
            pds.setMaxConnectionReuseCount(100);

            pds.setAbandonedConnectionTimeout(10);

            pds.setConnectionWaitTimeout(100);
            pds.setInactiveConnectionTimeout(120);

            pds.setTimeToLiveConnectionTimeout(500);
            pds.setTimeoutCheckInterval(60);
            pds.setMaxStatements(30);

            int error = 0;
            try {
                if (mgr.getConnectionPool(poolname) != null) {
                    logger.warn("PoolName: " + poolname + " exists");
                }
            } catch (Exception e1) {
                error = 1;
            }
            if (error == 1) {
                mgr.createConnectionPool(pds);
                mgr.startConnectionPool(poolname);
                logger.info("Create pool: " + poolname);
            }


        } catch (Exception e) {
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();

            logger.error("function createPool " + errorDesc, e);
            return false;
        }
        logger.info("return createConnectDb true");
        return true;
    }

    public Connection getConnect(){

        Connection con = null;
        try{
//            mgr.getConnectionPool(poolname).get
            if(pds==null){
                reConnect();
            }

            con = pds.getConnection();
        }catch(SQLException e){
            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            logger.error("function getConnect " + errorDesc, e);
        }
        return con;
    }

    private Connection reConnect(){
        Connection con = null;
        destroyCon();
        createPool();
        try {
            Locale.setDefault(Locale.getDefault());
            con = pds.getConnection();
        } catch (SQLException e1) {
            String erroDesc = e1.getMessage() == null ? "null" : e1.getMessage();
            logger.error("function reConnect " + erroDesc, e1);
        }
        return con;
    }

//    public synchronized void createPool() {
//        if(pds == null)
//        {
//            if(createPool(poolname))
//            {
//                logger.info("Create connection " + poolname + " successfully");
//                logger.info("Create connection pool successfully");
//            }
//        }
//    }

    public void destroyCon(){
        try {
            if(mgr!= null){
                mgr.destroyConnectionPool(poolname);
            }
            pds = null;

        } catch (Exception e) {

            String errorDesc = e.getMessage() == null ? "null" : e.getMessage();
            logger.error("function destroyCon " + errorDesc , e);
        }
    }
}
