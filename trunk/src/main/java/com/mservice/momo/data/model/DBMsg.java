package com.mservice.momo.data.model;

import com.mservice.momo.vertx.AppConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duyhuynh on 14/07/2016.
 */
public class DBMsg {

    public String source = "";
    public long time;
    public int phone;
    public long tranId;

    public String action = "";
    public String query = "";
    public List params = new ArrayList<>();

    public int err;
    public String des = "";
    public List res = new ArrayList<>();

    public DBMsg() {
        source = AppConstant.PREFIX;
        time = System.currentTimeMillis();
    }

    public DBMsg(int phone, String action) {
        this();
        this.phone = phone;
        this.action = action;
    }

    public DBMsg(int phone, long tranId, String action) {
        this();
        this.phone = phone;
        this.tranId = tranId;
        this.action = action;
    }

    public DBMsg(String source, int phone, long tranId, String action) {
        this();
        this.source = source;
        this.phone = phone;
        this.tranId = tranId;
        this.action = action;
    }
}
