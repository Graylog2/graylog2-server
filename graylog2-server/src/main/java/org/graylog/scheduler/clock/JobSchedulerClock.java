package org.graylog.scheduler.clock;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;

/**
 * A clock that provides access to the current {@link DateTime}.
 */
public interface JobSchedulerClock {
    /**
     * Returns the current UTC time.
     *
     * @return current time
     */
    DateTime nowUTC();

    /**
     * Returns the current time for the give time zone.
     *
     * @return current time
     */
    DateTime now(DateTimeZone zone);

    /**
     * Causes the current execution thread to sleep for the given duration.
     *
     * @param duration duration value
     * @param unit     duration unit
     * @throws InterruptedException
     */
    void sleep(long duration, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current execution thread to sleep uninterruptibly for the given duration.
     *
     * @param duration duration value
     * @param unit     duration unit
     */
    void sleepUninterruptibly(long duration, TimeUnit unit);
}
