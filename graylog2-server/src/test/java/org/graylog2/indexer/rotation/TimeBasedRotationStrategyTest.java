/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.rotation;

import org.graylog2.Graylog2BaseTest;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import static org.joda.time.Period.minutes;
import static org.joda.time.Period.seconds;
import static org.testng.Assert.*;

public class TimeBasedRotationStrategyTest extends Graylog2BaseTest {

    @AfterTest
    public void resetTimeProvider() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void determineAnchor() {
        final DateTime initialTime = new DateTime(2014, 3, 15, 14, 48, 35, 0, DateTimeZone.UTC);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);
        Period period;

        // should snap to 14:00:00
        period = Period.hours(1);
        final DateTime hourAnchor = TimeBasedRotationStrategy.determineRotationPeriodAnchor(period);
        assertEquals(hourAnchor.getHourOfDay(), 14);
        assertEquals(hourAnchor.getMinuteOfHour(), 0);
        assertEquals(hourAnchor.getSecondOfMinute(), 0);

        // should snap to 14:45:00
        period = Period.minutes(5);
        final DateTime fiveMins = TimeBasedRotationStrategy.determineRotationPeriodAnchor(period);
        assertEquals(fiveMins.getHourOfDay(), 14);
        assertEquals(fiveMins.getMinuteOfHour(), 45);
        assertEquals(fiveMins.getSecondOfMinute(), 0);

        // should snap to 2014-3-15 00:00:00
        period = Period.days(1).withHours(6);
        final DateTime dayAnd6Hours = TimeBasedRotationStrategy.determineRotationPeriodAnchor(period);
        assertEquals(dayAnd6Hours.getYear(), 2014);
        assertEquals(dayAnd6Hours.getMonthOfYear(), 3);
        assertEquals(dayAnd6Hours.getDayOfMonth(), 15);
        assertEquals(dayAnd6Hours.getHourOfDay(), 0);
        assertEquals(dayAnd6Hours.getMinuteOfHour(), 0);
        assertEquals(dayAnd6Hours.getSecondOfMinute(), 0);

    }

    @Test
    public void shouldRotateHourly() {
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        final Period period = Period.hours(1);
        final TimeBasedRotationStrategy hourlyRotation = new TimeBasedRotationStrategy(period);

        RotationStrategy.Result result;

        result = hourlyRotation.shouldRotate("ignored");
        assertTrue(result.shouldRotate(), "Should rotate the first index");

        clock.tick(seconds(2));

        result = hourlyRotation.shouldRotate("ignored");
        assertTrue(result.shouldRotate(), "Crossed rotation period");

        clock.tick(seconds(2));

        result = hourlyRotation.shouldRotate("ignored");
        assertFalse(result.shouldRotate(), "Did not cross rotation period");
    }

    @Test
    public void shouldRotateNonIntegralPeriod() {
        // start 5 minutes before full hour
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 55, 0, 0, DateTimeZone.UTC);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        final Period period = Period.minutes(10);
        final TimeBasedRotationStrategy tenMinRotation = new TimeBasedRotationStrategy(period);
        RotationStrategy.Result result;

        result = tenMinRotation.shouldRotate("ignored");
        assertTrue(result.shouldRotate(), "Should rotate the first index");

        // advance time to 01:55:01
        clock.tick(seconds(1));

        result = tenMinRotation.shouldRotate("ignored");
        assertFalse(result.shouldRotate(), "Did not cross rotation period");

        // advance time to 02:00:00
        clock.tick(minutes(4).withSeconds(59));

        result = tenMinRotation.shouldRotate("ignored");
        assertTrue(result.shouldRotate(), "Crossed rotation period");

        // advance time multiple rotation periods into the future
        // to time 02:51:00
        clock.tick(minutes(51));

        result = tenMinRotation.shouldRotate("ignored");
        assertTrue(result.shouldRotate(), "Crossed multiple rotation periods");

        // move time to 2:52:00
        // this should not cycle again, because next valid rotation time is 3:00:00
        clock.tick(minutes(1));
        result = tenMinRotation.shouldRotate("ignored");
        assertFalse(result.shouldRotate(), "Should not cycle when we missed multiple periods");
    }

}