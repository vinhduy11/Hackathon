package com.mservice.momo.gateway.internal.soapin.information.obj;

import org.vertx.java.core.json.JsonObject;

public class StandardMSResponse {
    private String resultName = "";
    private int resultCode = -1;
    private String description = "";
    private String transID = "";
    private String dataSign = "";
    private long debitAmount = 0;

    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransID() {
        return transID;
    }

    public void setTransID(String transID) {
        this.transID = transID;
    }

    public String getDataSign() {
        return dataSign;
    }

    public void setDataSign(String dataSign) {
        this.dataSign = dataSign;
    }

    public long getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(long debitAmount) {
        this.debitAmount = debitAmount;
    }

    public JsonObject toJson() {
        JsonObject standardJson = new JsonObject();

        return standardJson;
    }


}
