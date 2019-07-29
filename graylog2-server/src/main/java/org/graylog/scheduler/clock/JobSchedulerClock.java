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
