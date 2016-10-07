package com.mservice.momo.gateway.internal.soapin.information.session;

//import vn.com.ms.crypto.Cryption;
//import vn.com.ms.log4j.LOG;

public class SessionUtils {
	/*
	 * This function is used to create new session, the session must be validate before use
	 * @return new session id
	 * *//*
	public static String createSession() {
		String result = "";
		try {
			result = Cryption.MD5(String.valueOf(System.currentTimeMillis()));
		} catch (NoSuchAlgorithmException e) {
			LOG.sysmessage.error("", e);
		}
		LOG.sysmessage.info("CREATE SESSION SUCCESS - " + result);
		return result;
	}*/
}
