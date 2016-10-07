package com.mservice.momo.gateway.internal.soapin.information.obj;

import java.math.BigDecimal;

public class MsTransfer {
	private String target;
	private String provider="";
	private String providername="";
	private String targetname="";
	private BigDecimal amount;
	private int walletType;
	private boolean sendOTP = false;
	private String transferType="";
	
	private String providerid = "";
	private String targetid = "";
	private String message = "";
	
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getTargetname() {
		return targetname;
	}
	public void setTargetname(String targetname) {
		this.targetname = targetname;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getTarget() {
		return target;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setWalletType(int walletType) {
		this.walletType = walletType;
	}
	public int getWalletType() {
		return walletType;
	}
	public void setSendOTP(boolean sendOTP) {
		this.sendOTP = sendOTP;
	}
	public boolean isSendOTP() {
		return sendOTP;
	}
	public void setTransferType(String transferType) {
		this.transferType = transferType;
	}
	public String getProvidername() {
		return providername;
	}
	public void setProvidername(String providername) {
		this.providername = providername;
	}
	public String getTransferType() {
		return transferType;
	}
	public String getProviderid() {
		return providerid;
	}
	public void setProviderid(String providerid) {
		this.providerid = providerid;
	}
	public String getTargetid() {
		return targetid;
	}
	public void setTargetid(String targetid) {
		this.targetid = targetid;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
