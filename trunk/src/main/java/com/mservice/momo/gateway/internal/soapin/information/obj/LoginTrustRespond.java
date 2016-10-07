package com.mservice.momo.gateway.internal.soapin.information.obj;

import umarketscws.CreatesessionResponseType;
import umarketscws.StandardBizResponse;
import umarketscws.UMarketSC;

public class LoginTrustRespond {
	private StandardBizResponse bizResponse;
	private UMarketSC stub;
	private CreatesessionResponseType csrt;

	public void setBizResponse(StandardBizResponse bizResponse) {
		this.bizResponse = bizResponse;
	}

	public StandardBizResponse getBizResponse() {
		return bizResponse;
	}

	public void setStub(UMarketSC stub) {
		this.stub = stub;
	}

	public UMarketSC getStub() {
		return stub;
	}

	public void setCsrt(CreatesessionResponseType csrt) {
		this.csrt = csrt;
	}

	public CreatesessionResponseType getCsrt() {
		return csrt;
	} 
}
