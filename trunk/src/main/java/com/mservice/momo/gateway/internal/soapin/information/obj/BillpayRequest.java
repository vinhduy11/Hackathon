package com.mservice.momo.gateway.internal.soapin.information.obj;


public class BillpayRequest {
	private String sessionID;
	private int channel;
	private String transID;
	private String dataSign;
	private String billpayid;
	private String accountID;
	private String notifyAlt;
	private long amount;
	public void setBillpayid(String billpayid) {
		this.billpayid = billpayid;
	}
	public String getBillpayid() {
		return billpayid;
	}
	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}
	public String getAccountID() {
		return accountID;
	}
	public void setNotifyAlt(String notifyAlt) {
		this.notifyAlt = notifyAlt;
	}
	public String getNotifyAlt() {
		return notifyAlt;
	}
	public void setAmount(long amount) {
		this.amount = amount;
	}
	public long getAmount() {
		return amount;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getSessionID() {
		return sessionID;
	}
	public void setChannel(int channel) {
		this.channel = channel;
	}
	public int getChannel() {
		return channel;
	}
	public void setDataSign(String dataSign) {
		this.dataSign = dataSign;
	}
	public String getDataSign() {
		return dataSign;
	}
	public void setTransID(String transID) {
		this.transID = transID;
	}
	public String getTransID() {
		return transID;
	}
}
