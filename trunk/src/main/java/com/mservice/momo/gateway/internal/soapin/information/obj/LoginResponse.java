package com.mservice.momo.gateway.internal.soapin.information.obj;

public class LoginResponse {
	private int resultCode;
	private String sessionID;
	private String resultName;
	private String description;
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	public int getResultCode() {
		return resultCode;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getSessionID() {
		return sessionID;
	}
	public void setResultName(String resultName) {
		this.resultName = resultName;
	}
	public String getResultName() {
		return resultName;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
}
