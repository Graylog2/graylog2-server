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
