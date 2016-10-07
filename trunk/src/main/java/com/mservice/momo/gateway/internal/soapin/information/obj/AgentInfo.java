package com.mservice.momo.gateway.internal.soapin.information.obj;

import umarketscws.StandardBizResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

public class AgentInfo {
	private String agentReference = "";
	private String fullName = "";
	private String personalID = "";
	private String date_of_birth = "";
	private String address = "";
	private String email = "";
	private BigDecimal momoBalance = new BigDecimal(0);
	private BigDecimal mloadBalance = new BigDecimal(0);
	private BigDecimal momoPending = new BigDecimal(0);
	private BigDecimal mloadPending = new BigDecimal(0);
	private String mpin = "";
	private int status;
	private StandardBizResponse loginStatus;
	private String bank_name = "";
	private String bank_acc_no = "";
	private String bank_code = "";
	private String date_of_persionalid = "";
	private String place_of_persionalid = "";
	private String business_name = "";
	private String business_address = "";
	private String contact_no = "";
	private long agentid =0;
	private long refid;
	private boolean isMmtTopRetailer;
	private boolean isMmtRetailer;
	private boolean isMoMoEnduser;
	private boolean isMloadRetailer;
	private boolean isWorldNetAgent;
	private int worldNetLevel;
    private  boolean isNamed;

    private Date createdDate;
	
	public long getRefid() {
		return refid;
	}
	public void setRefid(long refid) {
		this.refid = refid;
	}
	private String smsAddress;
	protected String extra_desc;
	protected String province_code;
	protected String ward_code;
	protected String dist_code;
	ArrayList<String> group_name;
	private boolean sendOTP = false;
	
	public String getExtra_desc() {
		return extra_desc;
	}
	public void setExtra_desc(String extra_desc) {
		this.extra_desc = extra_desc;
	}
	public String getProvince_code() {
		return province_code == null ? "" : province_code ;
	}
	public void setProvince_code(String province_code) {
		this.province_code = province_code;
	}
	public String getWard_code() {
		return ward_code == null ? "" : ward_code;
	}
	public void setWard_code(String ward_code) {
		this.ward_code = ward_code;
	}
	public String getScore() {
		return score;
	}
	public void setScore(String score) {
		this.score = score==null?"0":score;
	}
	
	public String getDist_code() {
		return dist_code;
	}
	public void setDist_code(String dist_code) {
		this.dist_code = dist_code;
	}
	protected String score;
	
	public void setAgentReference(String agentReference) {
		this.agentReference = agentReference;
	}
	public String getAgentReference() {
		return agentReference;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getFullName() {

		return fullName == null ? "" : fullName;
	}
	public void setPersonalID(String personalID) {
		this.personalID = personalID;
	}
	public String getPersonalID() {
		return personalID == null ? "" : personalID;
	}
	public void setDateOfBirth(String dateOfBirth) {
		this.date_of_birth = dateOfBirth;
	}
	public String getDateOfBirth() {
		return date_of_birth == null ? "" : date_of_birth;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getAddress() {
		return address == null ? "" : address;
	}
	public void setMomoBalance(BigDecimal momoBalance) {
		this.momoBalance = momoBalance;
	}
	public BigDecimal getMomoBalance() {
		return momoBalance;
	}
	public void setMloadBalance(BigDecimal mloadBalance) {
		this.mloadBalance = mloadBalance;
	}
	public BigDecimal getMloadBalance() {
		return mloadBalance;
	}
	public void setMomoPending(BigDecimal momoPending) {
		this.momoPending = momoPending;
	}
	public BigDecimal getMomoPending() {
		return momoPending;
	}
	public void setMloadPending(BigDecimal mloadPending) {
		this.mloadPending = mloadPending;
	}
	public BigDecimal getMloadPending() {
		return mloadPending;
	}
	public void setMpin(String mpin) {
		this.mpin = mpin;
	}
	public String getMpin() {
		return mpin == null ? "" : mpin;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setLoginStatus(StandardBizResponse loginStatus) {
		this.loginStatus = loginStatus;
	}
	public StandardBizResponse getLoginStatus() {
		return loginStatus;
	}
	public void setBank_name(String bank_name) {
		this.bank_name = bank_name;
	}
	public String getBank_name() {
		return bank_name == null ? "" : bank_name;
	}
	public void setBank_acc_no(String bank_acc_no) {
		this.bank_acc_no = bank_acc_no;
	}
	public String getBank_acc_no() {
		return bank_acc_no == null ? "" : bank_acc_no;
	}
	public void setDate_of_persionalid(String date_of_persionalid) {
		this.date_of_persionalid = date_of_persionalid;
	}
	public String getDate_of_persionalid() {
		return date_of_persionalid == null ? "" : date_of_persionalid;
	}
	public void setPlace_of_persionalid(String place_of_persionalid) {
		this.place_of_persionalid = place_of_persionalid;
	}
	public String getPlace_of_persionalid() {
		return place_of_persionalid== null ? "" : place_of_persionalid;
	}
	public void setBusiness_name(String business_name) {
		this.business_name = business_name;
	}
	public String getBusiness_name() {
		return business_name== null ? "" : business_name;
	}
	public void setBusiness_address(String business_address) {
		this.business_address = business_address;
	}
	public String getBusiness_address() {
		return business_address== null ? "" : business_address;
	}
	public void setContact_no(String contact_no) {
		this.contact_no = contact_no;
	}
	public String getContact_no() {
		return contact_no == null ? "" : contact_no;
	}

	public void setAgentid(long agentid) {
		this.agentid = agentid;
	}
	public long getAgentid() {
		return agentid;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getEmail() {
		return email == null ? "" : email;
	}
	public void setBank_code(String bank_agent) {
		this.bank_code = bank_agent;
	}
	public String getBank_code() {
		return bank_code == null ? "" : bank_code;
	}
	public void setSmsAddress(String smsAddress) {
		this.smsAddress = smsAddress;
	}
	public String getSmsAddress() {
		return smsAddress == null ? "" : smsAddress;
	}
	public String getDate_of_birth() {
		return date_of_birth== null ? "" : date_of_birth;
	}
	public void setDate_of_birth(String date_of_birth) {
		this.date_of_birth = date_of_birth;
	}
	public ArrayList<String> getGroup_name() {
		return group_name;
	}
	public void setGroup_name(ArrayList<String> group_name) {
		this.group_name = group_name;
	}
	public boolean isSendOTP() {
		return sendOTP;
	}
	public void setSendOTP(boolean sendOTP) {
		this.sendOTP = sendOTP;
	}
	
	public boolean isMoMoEnduser() {
		return isMoMoEnduser;
	}
	public void setMoMoEnduser(boolean isMoMoEnduser) {
		this.isMoMoEnduser = isMoMoEnduser;
	}
	public boolean isMmtTopRetailer() {
		return isMmtTopRetailer;
	}
	public void setMmtTopRetailer(boolean isMmtTopRetailer) {
		this.isMmtTopRetailer = isMmtTopRetailer;
	}
	public boolean isMmtRetailer() {
		return isMmtRetailer;
	}
	public void setMmtRetailer(boolean isMmtRetailer) {
		this.isMmtRetailer = isMmtRetailer;
	}
	public boolean isMloadRetailer() {
		return isMloadRetailer;
	}
	public void setMloadRetailer(boolean isMloadRetailer) {
		this.isMloadRetailer = isMloadRetailer;
	}
	public boolean isWorldNetAgent() {
		return isWorldNetAgent;
	}
	public void setWorldNetAgent(boolean isWorldNetAgent) {
		this.isWorldNetAgent = isWorldNetAgent;
	}
	public int getWorldNetLevel() {
		return worldNetLevel;
	}
	public void setWorldNetLevel(int worldNetLevel) {
		this.worldNetLevel = worldNetLevel;
	}

    public boolean getIsNamed() {
        return isNamed;
    }

    public void setIsNamed(boolean isNamed) {
        this.isNamed = isNamed;
    }
}
