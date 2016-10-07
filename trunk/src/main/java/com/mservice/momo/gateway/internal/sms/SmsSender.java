package com.mservice.momo.gateway.internal.sms;



import org.vertx.java.core.logging.Logger;

import java.net.ConnectException;
import java.net.URL;
import java.rmi.RemoteException;
//todo rewrite it to async mode
public class SmsSender {
	private AlertServiceLocator mAlertServiceLocator;
	private Alert mAlert;
    Logger mLog;
    private String mEndpoint = "http://172.16.18.50:18080/ClientMark/services/Alert";

	public SmsSender(Logger log, String endpoint){
        mLog = log;
        mEndpoint = endpoint;

        mAlertServiceLocator = new AlertServiceLocator();
        try {
            mAlert = mAlertServiceLocator.getAlert(new URL(mEndpoint));
        } catch (Exception e) {
            e.printStackTrace();
            mLog.warn("can't create send sms service");
            mLog.error("", e);
        }

    }
	


	public void send(String phone, String sms) throws ConnectException, RemoteException{
        //todo check this is a vietnam phone
		if(phone.startsWith("0"))
        {
            phone = "84" + phone.substring(1);
        }
        else{
            phone = "84" + phone;
        }

        mLog.info("send sms: " + phone + " - " + sms);

        mAlert.send(phone, sms);
        mLog.info("send sms: success - " + phone);
	}
	

	


}
