package com.mservice.momo.gateway.internal.soapin.information;


import java.util.ArrayList;

public class UserLoginInfo {
	public UserLoginInfo(){
		lastUsed = System.currentTimeMillis();
		isValided = false;
	}
	private String username;
	private String password;
	private String passwordSalt;
	private String partner;
	private boolean isValided;
	private String sessionid;
	private long lastUsed;
	private String userAccount;
	private String mpin;
	private ArrayList<Integer> accessFunction;
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}
	public void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}
	public String getSessionid() {
		return sessionid;
	}
	public void setLastUsed(long lastUsed) {
		this.lastUsed = lastUsed;
	}
	public long getLastUsed() {
		return lastUsed;
	}
	public void setValided(boolean isValided) {
		this.isValided = isValided;
	}
	public boolean isValided() {
		return isValided;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}
	public void setPasswordSalt(String passwordSalt) {
		this.passwordSalt = passwordSalt;
	}
	public String getPasswordSalt() {
		return passwordSalt;
	}
	public void setPartner(String partner) {
		this.partner = partner;
	}
	public String getPartner() {
		return partner;
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
	public void setAccessFunction(ArrayList<Integer> accessFunction) {
		this.accessFunction = accessFunction;
	}
	public ArrayList<Integer> getAccessFunction() {
		return accessFunction;
	}
}
