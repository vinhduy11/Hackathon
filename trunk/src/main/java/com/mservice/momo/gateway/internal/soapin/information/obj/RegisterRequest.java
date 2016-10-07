package com.mservice.momo.gateway.internal.soapin.information.obj;

public class RegisterRequest {
	private String phoneNumber;
	private String sessionID;
	private String transID;
	private int channel;
	private String dataSign;
	private String fullName;
	private String personalID;
	private String bankAccountNo;
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getSessionID() {
		return sessionID;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getTransID() {
		return transID;
	}
	public void setTransID(String transID) {
		this.transID = transID;
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
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getPersonalID() {
		return personalID;
	}
	public void setPersonalID(String personalID) {
		this.personalID = personalID;
	}
	public void setBankAccountNo(String bankAccountNo) {
		this.bankAccountNo = bankAccountNo;
	}
	public String getBankAccountNo() {
		return bankAccountNo;
	}
}
