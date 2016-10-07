package com.mservice.momo.web;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ntunam on 4/7/14.
 */
public class HttpRequestContext {
    public static final String RESPONSE_CONTEXT_MODEL = "model";

    private HttpServerRequest request;
    private JsonObject session;
    private Cookie cookie;

    private Map<String, String> postParams;
    public String postData ="";

    private Map<String, Object> responseContext;

    public HttpRequestContext(HttpServerRequest request, Cookie cookie, JsonObject session) {
        this.request = request;
        this.session = session;
        this.cookie = cookie;
        responseContext = new ConcurrentHashMap<String, Object>();
        postParams = new HashMap<String, String>();
    }

    public HttpServerRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServerRequest request) {
        this.request = request;
    }

    public JsonObject getSession() {
        return session;
    }

    public void setSession(JsonObject session) {
        this.session = session;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public Map<String, Object> getResponseContext() {
        return responseContext;
    }

    public void setResponseContext(Map<String, Object> responseContext) {
        this.responseContext = responseContext;
    }

    public Map<String, String> getPostParams() {
        return postParams;
    }

    public void setPostParams(Map<String, String> postParams) {
        this.postParams = postParams;
    }

    public void setPostParams(String stringData) {

        try {
            //postData = new String(stringData.getBytes("UTF8"));
            postData = URLDecoder.decode(stringData, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] strings = stringData.split("&");
        for (String str : strings) {
            String[] param = str.split("=");
            String value = "";
            if (param.length == 0)
                continue;
            if (param.length > 1) {
                value = param[1];
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            postParams.put(param[0], value);
        }
    }
}
