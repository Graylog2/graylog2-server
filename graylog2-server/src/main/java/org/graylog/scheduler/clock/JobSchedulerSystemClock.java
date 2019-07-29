package org.graylog.scheduler.clock;

import com.google.common.util.concurrent.Uninterruptibles;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;

public class JobSchedulerSystemClock implements JobSchedulerClock {
    public static final JobSchedulerSystemClock INSTANCE = new JobSchedulerSystemClock();

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime nowUTC() {
        return now(DateTimeZone.UTC);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime now(DateTimeZone zone) {
        return DateTime.now(zone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sleep(long duration, TimeUnit unit) throws InterruptedException {
        Thread.sleep(unit.toMillis(duration));
    }

    @Override
    public void sleepUninterruptibly(long duration, TimeUnit unit) {
        Uninterruptibles.sleepUninterruptibly(duration, unit);
    }
}
