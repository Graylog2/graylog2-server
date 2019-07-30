/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
