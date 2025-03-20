package com.example.bread.utils;

import java.util.Date;

public final class TimestampUtils {

    // Private constructor to prevent instantiation.
    private TimestampUtils() {
    }

    /**
     * Transforms a given timestamp into a human-readable relative time string.
     *
     * @param timestamp the date to transform.
     * @return A string like "5 hours ago" or "2 days ago".
     */
    public static String transformTimestamp(Date timestamp) {
        if (timestamp == null) {
            return "";
        }
        long diff = new Date().getTime() - timestamp.getTime();
        long hours = diff / (60 * 60 * 1000);
        return (hours < 24) ? (hours + "h ago") : ((hours / 24) + "d ago");
    }
}