package com.mservice.momo.vertx.merchant.lotte.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duyhuynh on 05/04/2016.
 */
public class MerTranhis extends MerMsg {

    public int pageNum;
    public int pageSize;
    public long beginTime;
    public long endTime;
    public List<MerTranInfo> tranList = new ArrayList<>();
}
