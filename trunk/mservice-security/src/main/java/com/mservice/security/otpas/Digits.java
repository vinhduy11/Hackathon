
package com.mservice.security.otpas;

public enum Digits {
    FIVE(100000), SIX(1000000), SEVEN(10000000), EIGHT(100000000);
    private int digits;
    Digits(int digits) {
        this.digits = digits;
    }
    public int getValue() {
        return digits;
    }
}
