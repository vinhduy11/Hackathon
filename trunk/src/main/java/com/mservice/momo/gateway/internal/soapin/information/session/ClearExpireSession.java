package com.mservice.momo.gateway.internal.soapin.information.session;

import vn.com.ms.config.Config;
import vn.com.ms.log4j.LOG;


public class ClearExpireSession extends Thread {
	public ClearExpireSession(){
		start();
		LOG.sysmessage.debug("THREAD ClearExpireSession has been started!");
	}
	public void run(){
		while (true) {
			try {
		        //Get time expire of the session from configuration file
		        String sTimeExpire = Config.getStrConfig("session.timeexpire", "");
		        int timeExpire = 5;
		        try{
		        	timeExpire = Integer.parseInt(sTimeExpire);
		        }catch (NumberFormatException e) {
		        	LOG.sysmessage.error("CLEAR EXPIRED SESSION - Cannot parse session.timeexpire: " + sTimeExpire, e);
				}
				//Call function to clear expired session
				clearSession(timeExpire);
				//Get free time for this Thread from configuration file
				int timeSleep = 10*60*1000;
				String sTimeSleep = Config.getStrConfig("thread.timesleep", "");
				try{
					timeSleep = Integer.parseInt(sTimeSleep);
				}
				catch (NumberFormatException e) {
					LOG.sysmessage.error("CLEAR EXPIRED SESSION - Cannot parse thread.timesleep: " + sTimeSleep, e);
				}
				sleep(timeSleep);
			} catch (InterruptedException e) {
				LOG.sysmessage.error("CLEAR EXPIRED SESSION - InterruptedException", e);
			}
		}
	}
	
	
	public void clearSession(int timeExpire) {
//		Map<String, UserLoginInfo> map = MSFunction.mapUserInfo;
//		//Get Map in Set interface to get key and value
//        Set<Map.Entry<String, UserLoginInfo>> s = map.entrySet();
//        //Move next key and value of Map by iterator
//        Iterator<Map.Entry<String, UserLoginInfo>> it = s.iterator();
//        while(it.hasNext())
//        {
//            Map.Entry<String, UserLoginInfo> m =(Map.Entry<String, UserLoginInfo>)it.next();
//            String sessionID = m.getKey();
//            UserLoginInfo bankInfo = m.getValue();
//            long currTime = System.currentTimeMillis();
//            int currTimeExpire = (int)(currTime - bankInfo.getLastUsed())/60*1000;
//            if(currTimeExpire >= timeExpire){
//            	MSFunction.mapUserInfo.remove(sessionID);
//            	LOG.sysmessage.debug("Removed expired sessionid '" + sessionID + " '");
//            }
//        }
	}
}
