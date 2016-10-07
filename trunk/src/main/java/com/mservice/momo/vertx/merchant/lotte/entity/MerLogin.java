package com.mservice.momo.vertx.merchant.lotte.entity;

import com.mservice.momo.util.MomoJackJsonPack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duyhuynh on 31/03/2016.
 */
public class MerLogin extends MerMsg {

    public String sessionKey = "";
    public List<MerAuthority> authorities = new ArrayList<>();
}
