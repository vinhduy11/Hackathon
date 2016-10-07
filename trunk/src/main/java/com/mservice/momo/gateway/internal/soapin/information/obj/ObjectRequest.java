package com.mservice.momo.gateway.internal.soapin.information.obj;

public class ObjectRequest {

	private int transactionType;
	private Object object ;
	private Thread currThread;
	private String username;
	private String userAccount;
	private String mpin;
	private boolean isWakeup = false;
	private String partnerTransid = "";
	
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	long startTime;
	
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public Integer getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(int transactionType) {
		this.transactionType = transactionType;
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
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}
	public String getUserAccount() {
		return userAccount;
	}
	public void setMpin(String mpin) {
		this.mpin = mpin;
	}
	public String getMpin() {
		return mpin;
	}
	public boolean isWakeup() {
		return isWakeup;
	}
	public void setWakeup(boolean isWakeup) {
		this.isWakeup = isWakeup;
	}
	public String getPartnerTransid() {
		return partnerTransid;
	}
	public void setPartnerTransid(String partnerTransid) {
		this.partnerTransid = partnerTransid;
	}
	
	
}
