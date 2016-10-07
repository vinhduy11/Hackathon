package com.mservice.momo.object;

/**
 * Created by concu on 8/15/15.
 */
public class FeeObject {

    private String id = "";
    private int feeType = 0;
    private long minAmount = 0;
    private long maxAmount = 0;

    public FeeObject() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFeeType() {
        return feeType;
    }

    public void setFeeType(int feeType) {
        this.feeType = feeType;
    }

    public long getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(long minAmount) {
        this.minAmount = minAmount;
    }

    public long getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(long maxAmount) {
        this.maxAmount = maxAmount;
    }
}
