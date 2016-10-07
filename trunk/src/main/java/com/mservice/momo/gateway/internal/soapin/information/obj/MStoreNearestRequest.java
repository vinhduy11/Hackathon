package com.mservice.momo.gateway.internal.soapin.information.obj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by duyhv on 12/7/2015.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MStoreNearestRequest {

    public double lat;
    public double lon;
    public String searchValue;
    public int pageNum;
    public int pageSize;
    public long time;
    public String checkSum;

    /**
     * 0: ok
     * -1: not found
     * -2: system error
     * 10000: invalid params
     */
    public int resultCode;
    public String resultDesc;
    public List<MStore> resultData;
}
