package com.mservice.momo.gateway.internal.soapin.information.session;

import com.mservice.momo.util.DataUtil;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import umarketscws.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

//import vn.com.ms.log4j.LOG;

public class SessionManager{
	public static CreatesessionResponseType csrt = null;

    public static String wsdl = null;
	public static String initiator=null;
	public static String password =null;
    public static int sessionExpired = 5;
    public static Logger logger;

    static UMarketSC stub = null;
	static long lastTime ;
	static long currentTime;
	public static Map<String, SessionInfo> currSession = new TreeMap<String, SessionInfo>();
	
	private static void callSoap() {
		URL url = null;
        try {
            logger.info("SessionManager.callSoap wsdl " + wsdl);
            /*
			if (wsdl == null) {
				wsdl = Config.getStrConfig("soap.url", "");
				initiator = Config.getStrConfig("soap.username", "");
				password = Config.getStrConfig("soap.password", "");
				sessionExpired = Config.getIntConfig("soap.session.expire", 6);
			} */

			url = new URL(wsdl);
		} catch (MalformedURLException e) {
			logger.error("SessionManager.callSoap",e);
		}

		UMarketSC_Service soap;
		soap = new UMarketSC_Service(url);
		stub = soap.getUMarketSC();
	}
	
	private static CreatesessionResponseType createSession(long time) {
		
		if(stub == null)
			callSoap();
		
		String MPin = "";
		csrt = stub.createsession();		
		try {
			MPin = DataUtil.getSHA(csrt.getSessionid() + DataUtil.shaHash(initiator + password).toLowerCase()).toUpperCase();
		} catch (Exception e) {
			logger.error("CreatesessionResponseType",e);
		}
		LoginRequestType login_req = new LoginRequestType();
		login_req.setSessionid(csrt.getSessionid());
		login_req.setInitiator(initiator);
		login_req.setPin(MPin);
		StandardBizResponse biz_response_login = stub.login(login_req);

        //
		if(biz_response_login.getResult() != 0){
			csrt = null;
			logger.info(time + " " + initiator + " result code: " + biz_response_login.getResult());
		}

        logger.info(time + " " + initiator + " result code: " + biz_response_login.getResult());
        logger.info(time + " " + initiator +  " password: " + password);
        //logger.info(time + " " + initiator +  " sessionid: " + csrt.getSessionid() == null ? "null" : csrt.getSessionid());
        logger.info(time + " " + initiator + " MPin: " + MPin);

		lastTime = System.currentTimeMillis();
		
		return csrt;
	}

	public SessionManager() {
		
		if (stub == null)
			callSoap();
	}
	
	public static CreatesessionResponseType getCsrt(long time)
	{
		currentTime = System.currentTimeMillis();
		if((csrt == null) || ((currentTime - lastTime)/1000 > sessionExpired)){
            createSession(time);
        }
		lastTime = currentTime;
		
		logger.info("soap sessionid >>>>>>>>>>>>: " + csrt.getSessionid());
		return csrt;
	}
	
	private static SessionInfo createSession(String userAccount, String mpin, String time) {
		
		if(stub == null)
			callSoap();
		
		String hashPin = "";
		CreatesessionResponseType ccsrt = stub.createsession();
		try {
            hashPin = DataUtil.getSHA(ccsrt.getSessionid() + DataUtil.getSHA(userAccount + mpin).toLowerCase()).toUpperCase();
		} catch (Exception e) {
            logger.info(time + " " + userAccount + " " + e.getMessage());
		}
		LoginRequestType login_req = new LoginRequestType();
		login_req.setSessionid(ccsrt.getSessionid());
		login_req.setInitiator(userAccount);
		login_req.setPin(hashPin);
		StandardBizResponse biz_response_login = stub.login(login_req);

        if(biz_response_login != null && biz_response_login.getResult() == 1014){
            logger.info(time + " " + userAccount
                             + " pin: " + mpin
                             + " hashPin: " + hashPin
                             + " sessionid: " + ccsrt.getSessionid());
        }

        logger.info(time + " " + userAccount + " login result " +biz_response_login.getResult());
        logger.info(time + " " + userAccount + " pinlen " +mpin.length());

        //login failed
		if(biz_response_login.getResult() != 0){
			ccsrt = null;
            logger.info(time + " " + userAccount + " login session manager failed, result code: " + biz_response_login.getResult());
		}
		
		long clastTime = System.currentTimeMillis();
		SessionInfo sessionInfo = new SessionInfo();
		
		if(ccsrt != null){
			sessionInfo.setCsrt(ccsrt);
			sessionInfo.setLastTime(clastTime);
			currSession.put(userAccount, sessionInfo);
		}
		sessionInfo.setLoginResult(biz_response_login.getResult());
		return sessionInfo;
	}

    private static SessionInfo createSessionVertx(Vertx _vertx, final String userAccount, String mpin, long time) {

        if(stub == null)
            callSoap();

        String hashPin = "";
        CreatesessionResponseType ccsrt = stub.createsession();
        try {
            hashPin = DataUtil.getSHA(ccsrt.getSessionid() + DataUtil.getSHA(userAccount + mpin).toLowerCase()).toUpperCase();
        } catch (Exception e) {
            logger.info(time + " " + userAccount + " " + e.getMessage());
        }
        LoginRequestType login_req = new LoginRequestType();
        login_req.setSessionid(ccsrt.getSessionid());
        login_req.setInitiator(userAccount);
        login_req.setPin(hashPin);
        StandardBizResponse biz_response_login = stub.login(login_req);

        if(biz_response_login != null && biz_response_login.getResult() == 1014){
            logger.info(time + " " + userAccount
                    + " pin: " + mpin
                    + " hashPin: " + hashPin
                    + " sessionid: " + ccsrt.getSessionid());
        }

        logger.info(time + " " + userAccount + " login result " +biz_response_login.getResult());
        logger.info(time + " " + userAccount + " pinlen " +mpin.length());

        //login failed
        if(biz_response_login.getResult() != 0){
            ccsrt = null;
            logger.info(time + " " + userAccount + " login session manager failed, result code: " + biz_response_login.getResult());
        }

        long clastTime = System.currentTimeMillis();
        SessionInfo sessionInfo = new SessionInfo();

        if(ccsrt != null){
            sessionInfo.setCsrt(ccsrt);
            sessionInfo.setLastTime(clastTime);
            currSession.put(userAccount, sessionInfo);
            _vertx.setTimer(6*60*1000,new Handler<Long>() {
                @Override
                public void handle(Long timerId) {
                    SessionInfo si = currSession.get(userAccount);
                    if(si!=null){
                        if(System.currentTimeMillis() - si.getLastTime() >5*60*1000){
                            currSession.remove(userAccount);
                            logger.info("remove sessionmanager for " + userAccount + " currSession size " + currSession.size());
                        }
                    }
                }
            });
        }
        sessionInfo.setLoginResult(biz_response_login.getResult());
        return sessionInfo;
    }

	public static SessionInfo getCsrt(Vertx _vertx
                                    ,String userAccount
                                    ,String mpin
                                    ,long time)
	{
		logger.info(time +" "+ userAccount + " SessionInfo getCsrt");
        currentTime = System.currentTimeMillis();
		SessionInfo sessionInfo = currSession.get(userAccount);
		if(sessionInfo == null){
            //sessionInfo = createSession( userAccount, mpin, time);
            sessionInfo = createSessionVertx(_vertx, userAccount, mpin, time);
        }else{
			if((currentTime - sessionInfo.getLastTime()) > (sessionExpired*60*1000)){
                //sessionInfo = createSession(userAccount, mpin,time);
                sessionInfo = createSessionVertx(_vertx, userAccount, mpin, time);
            }
		}

		if(sessionInfo != null){
            sessionInfo.setLastTime(currentTime);
			currSession.put(userAccount, sessionInfo);
		}
        if(sessionInfo != null && sessionInfo.getCsrt() != null && sessionInfo.getCsrt().getSessionid() != null){
            logger.info(time +" "+ userAccount + " sessionid: " + sessionInfo.getCsrt().getSessionid());
        }else{
            logger.info(time +" "+ userAccount + " sessionid: null");
        }

		return sessionInfo;
	}

    public static SessionInfo getCsrtForMerchantUser(Vertx _vertx
            ,String userAccount
            ,String mpin
            ,long time)
    {
        logger.info(time +" "+ userAccount + " SessionInfo getCsrt");
        currentTime = System.currentTimeMillis();
        SessionInfo sessionInfo = currSession.get(userAccount);
        if(sessionInfo == null){
            //sessionInfo = createSession( userAccount, mpin, time);
            sessionInfo = createSessionVertx(_vertx, userAccount, mpin, time);
        }else{
                //sessionInfo = createSession(userAccount, mpin,time);
                sessionInfo = createSessionVertx(_vertx, userAccount, mpin, time);
        }

        if(sessionInfo != null){
            sessionInfo.setLastTime(currentTime);
            currSession.put(userAccount, sessionInfo);
        }
        if(sessionInfo != null && sessionInfo.getCsrt() != null && sessionInfo.getCsrt().getSessionid() != null){
            logger.info(time +" "+ userAccount + " sessionid: " + sessionInfo.getCsrt().getSessionid());
        }else{
            logger.info(time +" "+ userAccount + " sessionid: null");
        }

        return sessionInfo;
    }
}
