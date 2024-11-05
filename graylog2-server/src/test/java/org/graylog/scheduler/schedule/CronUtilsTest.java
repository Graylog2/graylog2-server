package org.graylog.scheduler.schedule;

import org.graylog.events.JobSchedulerTestClock;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CronUtilsTest {

    @Test
    public void testMinimumFrequencyValidation() {
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.parse("2024-01-01T02:00:00.000Z"));
        // Every 5 minutes with max frequency of 5 minutes
        assertTrue(CronUtils.validateCronFrequency("0 0/5 0 ? * * *", "UTC", Duration.ofMinutes(5), clock));
        // Every 15 minutes with max frequency of 5 minutes
        assertTrue(CronUtils.validateCronFrequency("0 0/15 0 ? * * *", "UTC", Duration.ofMinutes(5), clock));
        // Every minute with max frequency of 5 minutes
        assertFalse(CronUtils.validateCronFrequency("0 0/1 0 ? * * *", "UTC", Duration.ofMinutes(5), clock));
        // Every 5 minutes MON-FRI with max frequency of 5 minutes (date above is a Monday)
        assertTrue(CronUtils.validateCronFrequency("0 0/5 0 ? * MON-FRI *", "UTC", Duration.ofMinutes(5), clock));
    }

    @Test
    public void testEdgeCases() {
        // 23:59:30 on Friday night
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.parse("2024-01-05T23:59:30.000Z"));
        // Every 5 minutes on MON-FRI with max frequency of 5 minutes
        assertTrue(CronUtils.validateCronFrequency("0 0/5 0 ? * MON-FRI *", "UTC", Duration.ofMinutes(5), clock));
        // Every minute on MON-FRI with max frequency of 5 minutes
        assertFalse(CronUtils.validateCronFrequency("0 0/1 0 ? * MON-FRI *", "UTC", Duration.ofMinutes(5), clock));

    }
}
