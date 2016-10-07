package com.mservice.momo.gateway.internal.soapin.information.obj;


public class ObjectResponse extends ObjectRequest{
	private long stopTime ;
	private String transid;
	private Thread currThread;
	private String username;

	
	
	public String getTransid() {
		return transid;
	}

	public void setTransid(String transid) {
		this.transid = transid;
	}

	public long getStopTime() {
		return stopTime;
	}

	public void setStopTime(long stopTime) {
		this.stopTime = stopTime;
	}

	public void setCurrThread(Thread currThread) {
		this.currThread = currThread;
	}

	public Thread getCurrThread() {
		return currThread;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
}
