package com.mservice.momo.util.pos.otpas;

import java.util.Calendar;
import java.util.TimeZone;

public class Clock {
    private int interval;
    private Calendar calendar;
    private int markedLock;
    public Clock() {
        interval = 180;
    }
    public Clock(int interval) {
        this.interval = interval;
    }
    public long getCurrentInterval() {
        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTimeSeconds =  calendar.getTimeInMillis() / (interval *1000);
        markedLock = (int) (currentTimeSeconds%10);
        return currentTimeSeconds;
    }
    public int getMarkedLock()
    {
    	return markedLock;
    }
}
