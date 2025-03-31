package com.example.bread.utils;

import java.util.Date;

/**
 * TimestampUtils - Utils
 * <p>
 * Role / Purpose
 * Provides helper methods to transform `Date` objects into human-readable relative time strings such as "5h ago" or "2d ago".
 * Primarily used to display timestamps in a user-friendly format throughout the app (e.g., in mood events, comments).
 * <p>
 * Design Patterns
 * Utility Class Pattern: Implements static methods and prevents instantiation via a private constructor.
 * <p>
 * Outstanding Issues
 * - N/A
 */

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