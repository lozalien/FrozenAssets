package com.frozenassets.app.utils;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static final long TWO_WEEKS_IN_MILLIS = 14L * 24 * 60 * 60 * 1000; // 14 days
    public static final long TWO_MONTHS_IN_MILLIS = 60L * 24 * 60 * 60 * 1000; // 60 days

    public static int getExpirationStatus(Date expirationDate) {
        if (expirationDate == null) return 0;

        long now = System.currentTimeMillis();
        long timeUntilExpiration = expirationDate.getTime() - now;

        if (timeUntilExpiration <= TWO_WEEKS_IN_MILLIS) {
            return 2; // Critical - Red
        } else if (timeUntilExpiration <= TWO_MONTHS_IN_MILLIS) {
            return 1; // Warning - Yellow
        } else {
            return 0; // Normal
        }
    }

    public static Date getExpirationThreshold() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 2);
        return calendar.getTime();
    }
}