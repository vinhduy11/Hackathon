package com.mservice.momo.gateway.internal.soapin.information.obj;

public class AccountInfoRequest {
	private String sessionID;
	private int channel;
	private String dataSign;
	public String getSessionID() {
		return sessionID;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public int getChannel() {
		return channel;
	}
	public void setChannel(int channel) {
		this.channel = channel;
	}
	public String getDataSign() {
		return dataSign;
	}
	public void setDataSign(String dataSign) {
		this.dataSign = dataSign;
	}
	public String getTransID() {
		return transID;
	}
	public void setTransID(String transID) {
		this.transID = transID;
	}
	private String transID;
}
