package com.mservice.security.otpas;

import java.util.Calendar;
import java.util.TimeZone;

public class Clock {
    private int interval;
    private Calendar calendar;
    private int markedLock;
    public Clock() {
        interval = 60;
    }
    public Clock(int interval) {
        this.interval = interval;
    }
    public long getCurrentInterval() {
        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTimeSeconds = Math.round((calendar.getTimeInMillis()*1.0) /(interval *1000));
        markedLock = (int) (currentTimeSeconds%10);
        System.out.println("TIME BASE: " + currentTimeSeconds);
        return currentTimeSeconds;
    }
    public int getMarkedLock()
    {
    	return markedLock;
    }
}
