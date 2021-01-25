/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events;

import org.graylog.scheduler.clock.JobSchedulerClock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.concurrent.TimeUnit;

/**
 * A mutable test clock.
 */
public class JobSchedulerTestClock implements JobSchedulerClock {
    private Instant instant;

    public JobSchedulerTestClock(DateTime now) {
        this.instant = now.toInstant();
    }

    /**
     * Advances the time by the given duration.
     *
     * @param duration
     * @param unit
     */
    public void plus(long duration, TimeUnit unit) {
        this.instant = instant.plus(unit.toMillis(duration));
    }

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
        return instant.toDateTime().withZone(zone);
    }

    /**
     * Advances the clock by the given duration and immediately returns after that.
     *
     * @param duration duration value
     * @param unit     duration unit
     * @throws InterruptedException
     */
    @Override
    public void sleep(long duration, TimeUnit unit) throws InterruptedException {
        plus(duration, unit);
    }

    /**
     * Advances the clock by the given duration and immediately returns after that.
     *
     * @param duration duration value
     * @param unit     duration unit
     * @throws InterruptedException
     */
    @Override
    public void sleepUninterruptibly(long duration, TimeUnit unit) {
        try {
            sleep(duration, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
