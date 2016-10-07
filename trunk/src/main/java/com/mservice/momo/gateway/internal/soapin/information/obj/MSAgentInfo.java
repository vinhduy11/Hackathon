package com.mservice.momo.gateway.internal.soapin.information.obj;

public class MSAgentInfo {
	private String resultName;
	private int resultCode;
	private String description;
	private long transID;
	private int status;
	private long momo;
	private long mload;
	
	public long getMomo() {
		return momo;
	}
	public void setMomo(long momo) {
		this.momo = momo;
	}
	public long getMload() {
		return mload;
	}
	public void setMload(long mload) {
		this.mload = mload;
	}

	public void setResultName(String resultName) {
		this.resultName = resultName;
	}
	public String getResultName() {
		return resultName;
	}
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	public int getResultCode() {
		return resultCode;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setTransID(long transID) {
		this.transID = transID;
	}
	public long getTransID() {
		return transID;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
}
