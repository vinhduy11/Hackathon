package com.mservice.momo.gateway.internal.soapin.information.session;

import umarketscws.CreatesessionResponseType;

public class SessionInfo {

	private CreatesessionResponseType csrt;
	long lastTime ;
	int sessionExpired;
	int loginResult;
	
	public int getLoginResult() {
		return loginResult;
	}
	public void setLoginResult(int loginResult) {
		this.loginResult = loginResult;
	}
	public void setCsrt(CreatesessionResponseType csrt) {
		this.csrt = csrt;
	}
	public CreatesessionResponseType getCsrt() {
		return csrt;
	}
	public long getLastTime() {
		return lastTime;
	}
	public void setLastTime(long lastTime) {
		this.lastTime = lastTime;
	}
	public int getSessionExpired() {
		return sessionExpired;
	}
	public void setSessionExpired(int sessionExpired) {
		this.sessionExpired = sessionExpired;
	}

}
