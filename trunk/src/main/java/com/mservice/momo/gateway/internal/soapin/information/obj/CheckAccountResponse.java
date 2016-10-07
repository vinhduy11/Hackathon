package com.mservice.momo.gateway.internal.soapin.information.obj;

public class CheckAccountResponse {
	private String resultName;
	private int resultCode;
	private String description;
	private String dataSign;
	private String bankName;
	private String bankCode;
	private int status;
	private long momoBalance;
	private long mloadBalance;
	
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
	public long getMomoBalance() {
		return momoBalance;
	}
	public void setMomoBalance(long momoBalance) {
		this.momoBalance = momoBalance;
	}
	public long getMloadBalance() {
		return mloadBalance;
	}
	public void setMloadBalance(long mloadBalance) {
		this.mloadBalance = mloadBalance;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setDataSign(String dataSign) {
		this.dataSign = dataSign;
	}
	public String getDataSign() {
		return dataSign;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	public String getBankName() {
		return bankName;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
	public void setBankCode(String bankCode) {
		this.bankCode = bankCode;
	}
	public String getBankCode() {
		return bankCode;
	}
}
