package com.mservice.momo.gateway.internal.soapin.information.obj;

public class TopupAirtimeRequest {
	private String phoneNumber;
	private String sessionID;
	private String transID;
	private int channel;
	private String dataSign;
	private long amount;
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getSessionID() {
		return sessionID;
	}
	public void setTransID(String transID) {
		this.transID = transID;
	}
	public String getTransID() {
		return transID;
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
	public void setAmount(long amount) {
		this.amount = amount;
	}
	public long getAmount() {
		return amount;
	}
}
