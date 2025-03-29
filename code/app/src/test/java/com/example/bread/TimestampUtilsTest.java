package com.example.bread;

import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

import com.example.bread.utils.TimestampUtils;

public class TimestampUtilsTest {

    @Test
    public void testTransformTimestamp_NullInput_ReturnsEmptyString() {
        assertEquals("", TimestampUtils.transformTimestamp(null));
    }

    @Test
    public void testTransformTimestamp_LessThan24Hours() {
        Date twoHoursAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2));
        String result = TimestampUtils.transformTimestamp(twoHoursAgo);
        assertEquals("2h ago", result);
    }

    @Test
    public void testTransformTimestamp_MoreThan24Hours() {
        Date threeDaysAgo = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));
        String result = TimestampUtils.transformTimestamp(threeDaysAgo);
        assertEquals("3d ago", result);
    }

    @Test
    public void testTransformTimestamp_Exactly24Hours() {
        Date exactly24HoursAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));
        String result = TimestampUtils.transformTimestamp(exactly24HoursAgo);
        assertEquals("1d ago", result);
    }

    @Test
    public void testTransformTimestamp_ZeroDifference() {
        Date now = new Date();
        String result = TimestampUtils.transformTimestamp(now);
        assertEquals("0h ago", result);
    }
}
